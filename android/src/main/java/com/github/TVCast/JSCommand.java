package com.github.TVCast;

import com.connectsdk.core.AppInfo;
import com.connectsdk.core.ChannelInfo;
import com.connectsdk.core.JSONSerializable;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.MediaControl.PlayStateListener;
import com.connectsdk.service.capability.MediaControl.PlayStateStatus;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.MediaPlayer.MediaLaunchObject;
import com.connectsdk.service.capability.TVControl;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.service.sessions.WebAppSession;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class JSCommand {
    ConnectableDeviceWrapper deviceWrapper;
    String commandId;
    String methodName;
    boolean subscription;
    Callback successCallback;
    Callback errorCallback;
    ServiceSubscription<?> serviceSubscription;

    JSCommand(ConnectableDeviceWrapper deviceWrapper, String commandId, String methodName, boolean subscription, Callback successCallback, Callback errorCallback) {
        this.deviceWrapper = deviceWrapper;
        this.commandId = commandId;
        this.methodName = methodName;
        this.subscription = subscription;
        this.successCallback = successCallback;
        this.errorCallback = errorCallback;
    }

    void destroy() {
        deviceWrapper.cancelCommand(commandId);
    }

    public void cancel() {
        successCallback = null;
        errorCallback = null;

        if (serviceSubscription != null) {
            serviceSubscription.unsubscribe();
        }
    }

    void sendSuccessEvent(Object ... objs) {
        if (successCallback == null) return;

        try {
            if (objs.length > 0) {
                WritableNativeArray array = new WritableNativeArray();
                array.pushString("success");
                for (Object value : objs) {
                    if (value instanceof JSONObject) {
                        array.pushMap(TVConnectModule.convertJsonToMap((JSONObject) value));
                    } else if (value instanceof JSONArray) {
                        array.pushArray(TVConnectModule.convertJsonToArray((JSONArray) value));
                    } else if (value instanceof Boolean) {
                        array.pushBoolean((Boolean) value);
                    } else if (value instanceof Integer) {
                        array.pushInt((Integer) value);
                    } else if (value instanceof Double) {
                        array.pushDouble((Double) value);
                    } else if (value instanceof String) {
                        array.pushString((String) value);
                    }
                }
                try {
                    successCallback.invoke(array);
                }
                catch (RuntimeException e) {
                    deviceWrapper.module.getContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                            .emit(commandId, array);
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public void success() {
        sendSuccessEvent();
    }

    public void success(JSONObject obj) {
        sendSuccessEvent(obj);
    }

    public void success(JSONArray arr) {
        sendSuccessEvent(arr);
    }

    public void success(JSONSerializable obj) {
        JSONObject response = null;

        try {
            response = obj.toJSONObject();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (response != null) {
            success(response);
        }
    }

    public <T extends JSONSerializable> void success(List<T> list) {
        JSONArray response = listToJSON(list);

        success(response);
    }

    public void success(Number obj) {
        sendSuccessEvent(obj);
    }

    public void success(Boolean obj) {
        sendSuccessEvent(obj);
    }

    public void error(String errorMessage) {
        if (errorCallback == null) return;

        JSONObject errorObj = new JSONObject();

        try {
            errorObj.put("message", errorMessage);
            errorCallback.invoke(TVConnectModule.convertJsonToMap(errorObj));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        destroy();
    }

    public void error(Exception ex) {
        if (errorCallback == null) return;

        JSONObject errorObj = new JSONObject();

        try {
            errorObj.put("message", ex.getMessage());
            errorObj.put("detail", ex.toString());
            errorCallback.invoke(TVConnectModule.convertJsonToMap(errorObj));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        destroy();
    }

    public void error(ServiceCommandError error) {
        if (errorCallback == null) return;

        JSONObject errorObj = new JSONObject();

        try {
            errorObj.put("code", error.getCode());
            errorObj.put("message", error.getMessage());
            errorObj.put("detail", error.getPayload());
            errorCallback.invoke(TVConnectModule.convertJsonToMap(errorObj));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        destroy();
    }

    public ResponseListener<Object> getResponseListener() {
        return new ResponseListener<Object>() {

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }

            @Override
            public void onSuccess(Object object) {
                success((JSONObject) object);
            }
        };
    }

    JSONArray listToJSON(Iterable<? extends JSONSerializable> list) {
        JSONArray arr = new JSONArray();

        try {
            for (JSONSerializable item : list) {
                arr.put(item.toJSONObject());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return arr;
    }

    public Launcher.AppLaunchListener getAppLaunchListener() {
        return new Launcher.AppLaunchListener() {
            @Override
            public void onSuccess(LaunchSession object) {
                success(object);
            }

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }
        };
    }

    public Launcher.AppListListener getAppListListener() {
        return new Launcher.AppListListener() {
            @Override
            public void onSuccess(List<AppInfo> object) {
                success(object);
            }

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }
        };
    }

    public TVControl.ChannelListListener getChannelListListener() {
        return new TVControl.ChannelListListener() {
            @Override
            public void onSuccess(List<ChannelInfo> object) {
                success(object);
            }

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }
        };
    }

    public TVControl.ChannelListener getChannelListener() {
        return new TVControl.ChannelListener() {
            @Override
            public void onSuccess(ChannelInfo object) {
                success(object);
            }

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }
        };
    }

    public VolumeControl.VolumeListener getVolumeListener() {
        return new VolumeControl.VolumeListener() {
            @Override
            public void onSuccess(Float object) {
                success((int)(object * 100));
            }

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }
        };
    }

    public VolumeControl.MuteListener getMuteListener() {
        return new VolumeControl.MuteListener() {
            @Override
            public void onSuccess(Boolean object) {
                success(object);
            }

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }
        };
    }

    public MediaPlayer.LaunchListener getMediaLaunchListener() {
        return new MediaPlayer.LaunchListener() {
            @Override
            public void onSuccess(MediaLaunchObject object) {
                // FIXME include media control
                JSONObject launchSessionObj = null;
                JSONObject mediaControlObj = null;
                JSONObject playlistControlObj = null;

                try {
                    launchSessionObj = object.launchSession.toJSONObject();
                    launchSessionObj.put("serviceName", object.launchSession.getService().getServiceName());

                    if (object.mediaControl != null) {
                        MediaControlWrapper mediaControlWrapper = new MediaControlWrapper(deviceWrapper.module, object.mediaControl);
                        deviceWrapper.module.addObjectWrapper(mediaControlWrapper);
                        mediaControlObj = mediaControlWrapper.toJSONObject();
                    }

                    if (object.playlistControl != null) {
                        PlaylistControlWrapper playlistControlWrapper = new PlaylistControlWrapper(deviceWrapper.module, object.playlistControl);
                        deviceWrapper.module.addObjectWrapper(playlistControlWrapper);
                        playlistControlObj = playlistControlWrapper.toJSONObject();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                sendSuccessEvent(launchSessionObj, mediaControlObj, playlistControlObj);
            }

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }
        };
    }

    public WebAppSession.LaunchListener getWebAppLaunchListener() {
        return new WebAppSession.LaunchListener() {
            @Override
            public void onSuccess(WebAppSession session) {
                TVConnectModule module = deviceWrapper.module;
                WebAppSessionWrapper wrapper = new WebAppSessionWrapper(module, session);
                deviceWrapper.module.addObjectWrapper(wrapper);

                success(wrapper);
            }

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }
        };
    }

    public PlayStateListener getPlayStateListener() {
        return new PlayStateListener() {
            @Override
            public void onSuccess(PlayStateStatus state) {
                String name = "";

                switch (state) {
                    case Buffering:
                        name = "buffering"; break;
                    case Finished:
                        name = "finished"; break;
                    case Idle:
                        name = "idle"; break;
                    case Paused:
                        name = "pause"; break;
                    case Playing:
                        name = "playing"; break;
                    case Unknown:
                        name = "unknown"; break;
                    default:
                        break;
                }

                sendSuccessEvent(name);
            }

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }
        };
    }
}
