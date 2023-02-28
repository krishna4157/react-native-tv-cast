package com.github.TVCast;

import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.ConnectableDeviceListener;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.DeviceService.PairingType;
import com.connectsdk.service.command.ServiceCommandError;
import com.facebook.react.bridge.Callback;
import com.facebook.react.modules.core.DeviceEventManagerModule;

class ConnectableDeviceWrapper implements ConnectableDeviceListener {
    TVConnectModule module;
    String deviceId;
    ConnectableDevice device;
    Callback successCallback;
    Callback errorCallback;
    HashMap<String, JSCommand> commands = new HashMap<>();
    JSCommandDispatcher dispatcher;
    boolean active = false;

    public ConnectableDeviceWrapper(TVConnectModule module, ConnectableDevice device) {
        this.module = module;
        this.device = device;
        this.deviceId = device.getId();
        this.dispatcher = new JSCommandDispatcher(this);
    }

    public void setCallbackContext(Callback successCallback, Callback errorCallback) {
        this.successCallback = successCallback;
        this.errorCallback = errorCallback;

        setActive(successCallback != null);
    }

    // Active means that the wrapper is actively listening for events
    public void setActive(boolean activate) {
        if (!active && activate) {
            this.device.addListener(this);
            active = true;
        } else if (active && !activate) {
            this.device.removeListener(this);
            active = false;
        }
    }

    public JSONObject toJSONObject() {
        JSONObject obj = device.toJSONObject();

        try {
            obj.put("deviceId", deviceId);
            obj.put("capabilities", new JSONArray(device.getCapabilities()));
            obj.put("ready", device.isConnected()); // FIXME need actual ready state

            JSONArray services = new JSONArray();

            for (DeviceService service : device.getServices()) {
                String serviceName = service.getServiceName();

                if (serviceName == null) {
                    Log.e("ConnectSDK", "service is missing id: " + service.getClass().getName());
                    continue;
                }

                JSONObject serviceObj = new JSONObject();
                serviceObj.put("name", serviceName);
                services.put(serviceObj);
            }

            obj.put("services", services);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }

    public void connect() {
        this.device.connect();
    }

    public void disconnect() {
        this.device.disconnect();
    }

    public void sendCommand(String commandId, String ifaceName, String methodName, JSONObject args, boolean subscribe, Callback successCallback, Callback errorCallback) {
        JSCommand command = new JSCommand(this, commandId, methodName, subscribe, successCallback, errorCallback);
        commands.put(commandId, command);

        this.successCallback = successCallback;
        this.errorCallback = errorCallback;
        Log.d("ConnectSDK", "dispatching command " + ifaceName + "." + methodName);
        this.dispatcher.dispatchCommand(ifaceName, methodName, command, args);
    }

    public void cancelCommand(String commandId) {
        JSCommand command = commands.remove(commandId);
        if (command != null && command.methodName != null)
            module.getContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(command.commandId, "cancelCommand");

        if (command != null) {
            command.cancel();
        }
    }

    void sendEvent(String event, Object ...objs) {
        if (successCallback != null) {
            module.sendEvent(successCallback, event, objs);
        }
    }

    @Override
    public void onConnectionFailed(ConnectableDevice device, ServiceCommandError error) {
        module.error(errorCallback, error);
    }

    @Override
    public void onDeviceReady(ConnectableDevice device) {
        sendEvent("ready");
    }

    @Override
    public void onDeviceDisconnected(ConnectableDevice device) {
        sendEvent("disconnect");
    }

    @Override
    public void onCapabilityUpdated(ConnectableDevice device, List<String> added, List<String> removed) {
        JSONObject obj = new JSONObject();

        try {
            obj.put("added", removed != null ? new JSONArray(added) : new JSONArray());
            obj.put("removed", removed != null ? new JSONArray(removed) : new JSONArray());
            obj.put("reset", false);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendEvent("capabilitieschanged", obj);
    }

    @Override
    public void onPairingRequired(ConnectableDevice device,
                                  DeviceService service, PairingType pairingType) {
        sendEvent("servicepairingrequired");
    }

    public void setPairingType(PairingType pairingType) {
        device.setPairingType(pairingType);
    }
}
