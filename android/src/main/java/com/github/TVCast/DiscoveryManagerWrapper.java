package com.github.TVCast;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.CapabilityFilter;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManager.PairingLevel;
import com.connectsdk.discovery.DiscoveryManagerListener;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.command.ServiceCommandError;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import okhttp3.internal.platform.Platform;

public class DiscoveryManagerWrapper extends ReactContextBaseJavaModule implements DiscoveryManagerListener {
    TVConnectModule module;
    DiscoveryManager discoveryManager;
    Callback callbackContext;
    List<ConnectableDevice> mDeviceList = new LinkedList<>();
    @NonNull
    @NotNull
    @Override
    public String getName() {
        return "DiscoveryManager";
    }

    DiscoveryManagerWrapper(TVConnectModule module, DiscoveryManager discoveryManager) {
        this.module = module;

        discoveryManager.registerDefaultDeviceTypes();
        this.discoveryManager = discoveryManager;

        discoveryManager.addListener(this);
    }

    public void setCallbackContext(Callback callbackContext) {
        this.callbackContext = callbackContext;
    }

    public void start() throws ClassNotFoundException {

        discoveryManager.registerDefaultDeviceTypes();
//        devicesList.put();
//        devicesList.put("com.connectsdk.service.CastService", "");

        try {

            discoveryManager.registerDeviceService((Class<DeviceService>) Class.forName("com.connectsdk.service.CastService"), (Class<DiscoveryProvider>)Class.forName("com.connectsdk.discovery.provider.CastDiscoveryProvider"));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
//        discoveryManager.registerDeviceService(CastService, "com.connectsdk.discovery.provider.CastDiscoveryProvider");
        discoveryManager.start();
    }

    public void stop() {
        discoveryManager.stop();
    }

    public void configure(JSONObject config) throws JSONException {
        if (config.has("pairingLevel")) {
            String pairingLevel = config.getString("pairingLevel");

            if ("off".equals(pairingLevel)) {
                discoveryManager.setPairingLevel(PairingLevel.OFF);
            } else if ("on".equals(pairingLevel)) {
                discoveryManager.setPairingLevel(PairingLevel.ON);
            }
        }

        if (config.has("capabilityFilters")) {
            JSONArray filters = config.getJSONArray("capabilityFilters");
            ArrayList<CapabilityFilter> capabilityFilters = new ArrayList<>();

            for (int i = 0; i < filters.length(); i++) {
                JSONArray filter = filters.getJSONArray(i);
                CapabilityFilter capabilityFilter = new CapabilityFilter();

                for (int j = 0; j < filter.length(); j++) {
                    capabilityFilter.addCapability(filter.getString(j));
                }

                capabilityFilters.add(capabilityFilter);
            }

            discoveryManager.setCapabilityFilters(capabilityFilters);
        }
    }

    @Override
    public void onDeviceAdded(DiscoveryManager manager, ConnectableDevice device) {
        mDeviceList.add(device);
        Map<String,ConnectableDevice> listOfDevices =discoveryManager.getAllDevices();
        List<ConnectableDevice> writableList = new LinkedList<>();
        listOfDevices.
                forEach((k, v) -> {
                            writableList.add(v);
                            Log.d("LIST ITERATOR ",v.toString()+" K VALUE : "+k.toString());

                        }
                );

        mDeviceList.add(device);
        module. getContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("deviceList", writableList.toString());
        sendDeviceEvent("devicefound", device);
    }

    @Override
    public void onDeviceUpdated(DiscoveryManager manager, ConnectableDevice device) {
        try {
            List<ConnectableDevice> writableList = new LinkedList<>();
            discoveryManager.registerDeviceService((Class<DeviceService>) Class.forName("com.connectsdk.service.CastService"), (Class<DiscoveryProvider>)Class.forName("com.connectsdk.discovery.provider.CastDiscoveryProvider"));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
         Map<String,ConnectableDevice> listOfDevices =discoveryManager.getAllDevices();
        List<ConnectableDevice> writableList = new LinkedList<>();

        listOfDevices.
                forEach((k, v) -> {
                    writableList.add(v);

                    Log.d("LIST ITERATOR ",v.toString()+" K VALUE : "+k.toString());

                    }
                );

        mDeviceList.add(device);
        module. getContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("deviceList", writableList.toString());
        sendDeviceEvent("devicefound", device);
    }

    @Override
    public void onDeviceRemoved(DiscoveryManager manager, ConnectableDevice device) {
        sendDeviceEvent("devicelost", device);
        mDeviceList.remove(device);
        Map<String,ConnectableDevice> listOfDevices =discoveryManager.getAllDevices();
        List<ConnectableDevice> writableList = new LinkedList<>();

        listOfDevices.
                forEach((k, v) -> {
                            writableList.add(v);
                            Log.d("LIST ITERATOR ",v.toString()+" K VALUE : "+k.toString());

                        }
                );


        module. getContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("deviceList", writableList.toString());
        module.removeDeviceWrapper(device);
    }

    @Override
    public void onDiscoveryFailed(DiscoveryManager manager, ServiceCommandError error) {
        if (callbackContext != null) {
//            module.error(callbackContext, error);
            WritableMap params = Arguments.createMap();
            params.putString("error", String.valueOf(error));
            module.getContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("error", params);
        }
    }

    public JSONObject getDeviceJSON(ConnectableDevice device) {
        ConnectableDeviceWrapper wrapper = module.getDeviceWrapper(device);
        return wrapper.toJSONObject();
    }

    public void sendDeviceEvent(String event, ConnectableDevice device) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("device", getDeviceJSON(device));
        } catch (JSONException e) {
        }

        sendEvent(event, obj);
    }

    public void sendEvent(String event, JSONObject obj) {
        if (callbackContext != null) {
            module.sendEvent(callbackContext, event, obj);
        }
    }

    @ReactMethod
    void startDiscovery(ReadableMap mapArgs, final Callback Callback) throws JSONException, ClassNotFoundException {
        JSONObject args = TVConnectModule.convertMapToJson(mapArgs);

        if (args != null && args.length() > 0) {
            configure(args);
        }

        setCallbackContext(Callback);
        start();
    }

    @ReactMethod
    void stopDiscovery(JSONArray args, final Callback Callback) throws JSONException {
        stop();
        module.success(Callback);
    }
}
