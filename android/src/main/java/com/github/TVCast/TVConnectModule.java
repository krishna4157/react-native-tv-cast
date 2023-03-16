package com.github.TVCast;
// Import android modules
import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

import android.app.AlertDialog;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.whisperplay.fling.media.controller.DiscoveryController;
import com.amazon.whisperplay.fling.media.controller.RemoteMediaPlayer;
import com.connectsdk.core.ImageInfo;
import com.connectsdk.core.JSONSerializable;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.SubtitleInfo;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.DevicePicker;
import com.connectsdk.device.SimpleDevicePicker;
import com.connectsdk.device.SimpleDevicePickerListener;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.sessions.LaunchSession;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

// Import React Native dependencies

public class TVConnectModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    private LaunchSession launchSession;
    private MediaControl mediaControl;
    private BaseFragment baseFragment;
    private Long totalTimeDuration;

    private static final String LOG_TAG = "TVConnectModule";
    private final ReactApplicationContext mCtx;
    DevicePicker dp;
    ConnectableDevice mTV;
    AlertDialog dialog;
    ConnectableDevice connectableDevice;
    private RemoteMediaPlayer mCurrentDevice;
    private static final String META_TITLE = "title";
    private static final String META_DESCRIPTION = "description";
    private static final String META_MIME_TYPE = "type";
    private static final String META_ICON_IMAGE = "poster";
    private static final String META_NOREPLAY = "noreplay";
    private static final String META_TRACKS = "tracks";
    private static final String META_SRC = "src";
    private static final String META_KIND = "kind";
    private static final String META_SRCLANG = "srclang";
    private static final String META_LABEL = "label";
    private DiscoveryController mController;
    final String TAG = "TVConnectModule";
    private List<RemoteMediaPlayer> mDeviceList = new LinkedList<>();

    @ReactMethod
    public TVConnectModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addLifecycleEventListener(this);
        this.mCtx = reactContext;
        initDiscoveryManagerWrapper();
        mController = new DiscoveryController(reactContext);

    }

    private void sendEvent(String eventName, WritableMap params) {
        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    public void updateDeviceList(RemoteMediaPlayer device) {
        Log.v(TAG, "updateDeviceList");
        if (mDeviceList.contains(device)) {
            mDeviceList.remove(device);
        }
        mDeviceList.add(device);
        JSONArray arrOfDevices = new JSONArray();
        for (RemoteMediaPlayer dev : mDeviceList) {
            JSONObject json = new JSONObject();
            try {
                json.put("name", dev.getName());
                json.put("uuid", dev.getUniqueIdentifier());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            arrOfDevices.put(json);
        }


        WritableMap params = Arguments.createMap();
        params.putString("devices", arrOfDevices.toString());
        Log.v(TAG, arrOfDevices.toString());
        sendEvent("device_list", params);
    }


    DiscoveryController.IDiscoveryListener mDiscovery = new DiscoveryController.IDiscoveryListener() {
        @Override
        public void playerDiscovered(RemoteMediaPlayer player) {
            Log.v(TAG, "playerDiscovered" + player.toString());
            //add media player to the application’s player list.
            updateDeviceList(player);
        }

        @Override
        public void playerLost(RemoteMediaPlayer player) {
            Log.v(TAG, "jed2");
            //remove media player from the application’s player list.
        }

        @Override
        public void discoveryFailure() {
            Log.v(TAG, "jed3");
        }
    };


    public DiscoveryManagerWrapper getDiscoveryManagerWrapper () {
        return discoveryManagerWrapper;
    }

    @NonNull
    @Override
    public String getName() {
        return "TVCast";
    }
    public static final String JS_PAIRING_TYPE_FIRST_SCREEN = "FIRST_SCREEN";
    public static final String JS_PAIRING_TYPE_PIN = "PIN";
    public static final String JS_PAIRING_TYPE_MIXED = "MIXED";

    DiscoveryManager discoveryManager;
    DiscoveryManagerWrapper discoveryManagerWrapper;
    LinkedHashMap<String, ConnectableDeviceWrapper> deviceWrapperById = new LinkedHashMap<>();
    LinkedHashMap<ConnectableDevice, ConnectableDeviceWrapper> deviceWrapperByDevice = new LinkedHashMap<>();

    HashMap<String, JSObjectWrapper> objectWrappers = new HashMap<>();
    private SimpleDevicePicker picker;

    @Override
    public void onHostResume() {
    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {
        if (picker != null) {
            picker.hidePairingDialog();
            picker.hidePicker();
            picker = null;
        }
    }

    @ReactMethod
    public void CastToTv(String uri1,Callback callback) throws URISyntaxException, MalformedURLException {
        String imagePath = uri1;
        String mimeType = "image/png";
        String title = "Connect SDK";
        String description = "One SDK Eight Media Platforms";
        String icon = "http://TVCast.com/TVCast_Logo.jpg";

        MediaInfo mediaInfo = new MediaInfo.Builder(imagePath, mimeType)
                .setTitle(title)
                .setDescription(description)
                .setIcon(icon)
                .build();
        mTV.getMediaPlayer().displayImage(mediaInfo, new MediaPlayer.LaunchListener() {

            @Override
            public void onError(ServiceCommandError error) {
                Log.e("Error", "Error displaying Image", error);
//                stopMediaSession();
            }

            @Override
            public void onSuccess(MediaPlayer.MediaLaunchObject object) {
                launchSession = object.launchSession;
            }
        });

    }

    private void fling() {
        mCurrentDevice = (RemoteMediaPlayer) mTV;
        Log.d("FLINGED","FLINGED");
        mCurrentDevice.setMediaSource("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "Buck", true, false).getAsync(new ErrorResultHandler("Error attempting to Play","ERROR"));
    }




    private class ErrorResultHandler implements RemoteMediaPlayer.FutureListener<Void> {
        private String mCommand;
        private String mMsg;
        private boolean mExtend;

        ErrorResultHandler(String command, String msg) {
            this(command, msg, false);
        }

        ErrorResultHandler(String command, String msg, boolean extend) {
            mCommand = command;
            mMsg = msg;
            mExtend = extend;
        }

        @Override
        public void futureIsNow(Future<Void> result) {
            try {
                result.get();
            } catch (ExecutionException e) {
            } catch (Exception e) {
            }
        }
    }


    private String getMetadata(MediaInfo mediaInfo)
            throws JSONException {
        JSONObject json = new JSONObject();
        if (mediaInfo.getTitle() != null && !mediaInfo.getTitle().isEmpty()) {
            json.put(META_TITLE, mediaInfo.getTitle());
        }
        if (mediaInfo.getDescription() != null && !mediaInfo.getDescription().isEmpty()) {
            json.put(META_DESCRIPTION, mediaInfo.getDescription());
        }
        json.put(META_MIME_TYPE, mediaInfo.getMimeType());
        if (mediaInfo.getImages() != null && mediaInfo.getImages().size() > 0) {
            ImageInfo image = mediaInfo.getImages().get(0);
            if (image != null) {
                if (image.getUrl() != null && !image.getUrl().isEmpty()) {
                    json.put(META_ICON_IMAGE, image.getUrl());
                }
            }
        }
        json.put(META_NOREPLAY, true);
        if (mediaInfo.getSubtitleInfo() != null) {
            JSONArray tracksArray = new JSONArray();
            JSONObject trackObj = new JSONObject();
            trackObj.put(META_KIND, "subtitles");
            trackObj.put(META_SRC, mediaInfo.getSubtitleInfo().getUrl());
            String label = mediaInfo.getSubtitleInfo().getLabel();
            trackObj.put(META_LABEL, label == null ? "" : label);
            String language = mediaInfo.getSubtitleInfo().getLanguage();
            trackObj.put(META_SRCLANG, language == null ? "" : language);
            tracksArray.put(trackObj);
            json.put(META_TRACKS, tracksArray);
        }
        return json.toString();
    }



    @ReactMethod
    public void CastToTvVideo(String url){
        Log.d("CHECK AMAZON DEVICE LIST ",mDeviceList.toString());
        boolean shouldLoop = true;

        SubtitleInfo.Builder subtitleBuilder = null;
        MediaInfo mediaInfo = new MediaInfo.Builder(url, "video/mp4")
                .setTitle("Connect SDK")
                .setDescription("One SDK Eight Media Platforms")
                .setIcon("https://picsum.photos/200/300")
                .setSubtitleInfo(subtitleBuilder == null ? null : subtitleBuilder.build())
                .build();
        mTV.connect();

        mTV.getCapability(MediaPlayer.class).getMediaPlayer().playMedia(mediaInfo, shouldLoop, new MediaPlayer.LaunchListener() {

            @Override
            public void onError(ServiceCommandError error) {
                Log.e("Error", "Error playing video", error);
                Log.d("CHECK DETAILS : ", mTV.toString());

            }

            public void onSuccess(MediaPlayer.MediaLaunchObject object) {
                launchSession = object.launchSession;
            }
        });

    }

    @ReactMethod
    public void CastToTvVideo(ReadableMap movieData, Callback callback){
        Log.d("CHECK AMAZON DEVICE LIST ",mDeviceList.toString());
        boolean shouldLoop = true;

        SubtitleInfo.Builder subtitleBuilder = null;
        String url = movieData.getString("url");
        String title = movieData.getString("title");
        String description = movieData.getString("description");
        String icon = movieData.getString("icon");

        MediaInfo mediaInfo = new MediaInfo.Builder(
                url
//                "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                , "video/mp4")
                .setTitle(title)
                .setDescription(description)
                .setIcon(icon)
                .setSubtitleInfo(subtitleBuilder == null ? null : subtitleBuilder.build())
                .build();
    if(!mTV.isConnected()){
        mTV.connect();
    }
        Log.d("HELLO WORLD888 : ", String.valueOf(mTV.getFriendlyName()));

        mTV.getCapability(MediaPlayer.class).getMediaPlayer().playMedia(mediaInfo, shouldLoop, new MediaPlayer.LaunchListener() {

            @Override
            public void onError(ServiceCommandError error) {
                getContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("playbackerror", error.toString());
                Log.e("Error", "Error playing video", error);
                Log.d("CHECK DETAILS : ", mTV.toString());
            }

            @Override
            public void onSuccess(MediaPlayer.MediaLaunchObject object) {
                Log.d("PLAY_SUCCESS : ","PLAYSUCCESS");
                launchSession = object.launchSession;
                Handler handler = new Handler();

                handler.postDelayed(new Runnable() {
                    public void run() {
                        if(mTV.isConnected() ) {

                            new Timer().scheduleAtFixedRate(new TimerTask() {
                                @Override
                                public void run() {
                                    //your method
                                    Log.d("DURATION_STARTED",String.valueOf(mTV.getCapability(MediaControl.class)));
                                    if(mTV.getCapability(MediaControl.class) != null) {
                                        mTV.getCapability(MediaControl.class).
                                                getDuration(durationListener);
                                    }

                                }
                            }, 0, 2000);
                        }

                        // Actions to do after 5 seconds
                    }
                }, 10000);
                                callback.invoke("PLAY_SUCCESS");

            }
        });

    }

    @ReactMethod
    public void addListener(String eventName) {

    }

    @ReactMethod
    public void removeListeners(Integer count) {

    }

    public ReactApplicationContext getContext() {
        return mCtx;
    }

    static class NoSuchDeviceException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    synchronized ConnectableDeviceWrapper getDeviceWrapper(String deviceId) throws NoSuchDeviceException {
        ConnectableDeviceWrapper wrapper = deviceWrapperById.get(deviceId);

        if (wrapper == null) {
            throw new NoSuchDeviceException();
        }

        return wrapper;
    }

    synchronized ConnectableDeviceWrapper getDeviceWrapper(ConnectableDevice device) {
        ConnectableDeviceWrapper wrapper = deviceWrapperByDevice.get(device);

        if (wrapper == null) {
            wrapper = new ConnectableDeviceWrapper(this, device);
            deviceWrapperByDevice.put(device, wrapper);
            deviceWrapperById.put(wrapper.deviceId, wrapper);
        }

        return wrapper;
    }

    synchronized void removeDeviceWrapper(ConnectableDevice device) {
        ConnectableDeviceWrapper wrapper = deviceWrapperByDevice.get(device);

        if (wrapper != null) {
            deviceWrapperByDevice.remove(device);
            deviceWrapperById.remove(wrapper.deviceId);
        }
    }

    @ReactMethod
    public boolean execute(String action, String arrArgs, Callback successCallback, Callback errorCallback) throws JSONException {
        try {
            JSONArray args = new JSONArray(arrArgs);
            Log.w(LOG_TAG, "execute" + arrArgs);

            if ("sendCommand".equals(action)) {
                ConnectableDeviceWrapper deviceWrapper = getDeviceWrapper(args.getString(0));

                String commandId = args.getString(1);
                String ifaceName = args.getString(2);
                String methodName = args.getString(3);
                JSONObject methodArgs = args.getJSONObject(4);
                boolean subscribe = args.getBoolean(5);
                deviceWrapper.sendCommand(commandId, ifaceName, methodName, methodArgs, subscribe, successCallback, errorCallback);
                return true;
            } else if ("cancelCommand".equals(action)) {
                ConnectableDeviceWrapper deviceWrapper = getDeviceWrapper(args.getString(0));
                String commandId = args.getString(1);

                deviceWrapper.cancelCommand(commandId);
                success(successCallback);

                return true;
            } else if ("startDiscovery".equals(action)) {
                discoveryManagerWrapper = null;
                initDiscoveryManagerWrapper();
                discoveryManager = DiscoveryManager.getInstance();
                discoveryManager.registerDefaultDeviceTypes();
                discoveryManager.start();
                startDiscovery(arrArgs, successCallback);
                return true;
            } else if ("stopDiscovery".equals(action)) {

                stopDiscovery(successCallback);
                return true;
            }else if ("resetDiscovery".equals(action)) {
                stopDiscovery(successCallback);                
                discoveryManagerWrapper = null;
                initDiscoveryManagerWrapper();
                startDiscovery(arrArgs, successCallback);
                Log.d("DISCOVERRESET","TRIGGERED");
                return true;
            }
            else if ("setDiscoveryConfig".equals(action)) {
                setDiscoveryConfig(args, successCallback);
                return true;
            } else if ("pickDevice".equals(action)) {
                discoveryManager = DiscoveryManager.getInstance();
                discoveryManager.registerDefaultDeviceTypes();
                discoveryManager.start();

                Log.d("HELLO WORLD ","TRIGGERED pick device");
                pickDevice(args, successCallback);
                return true;
            } else if ("setDeviceListener".equals(action)) {
                ConnectableDeviceWrapper deviceWrapper = getDeviceWrapper(args.getString(0));
                deviceWrapper.setCallbackContext(successCallback, errorCallback);
                return true;
            } else if ("connectDevice".equals(action)) {
                ConnectableDeviceWrapper deviceWrapper = getDeviceWrapper(args.getString(0));
                deviceWrapper.setCallbackContext(successCallback, errorCallback);
                deviceWrapper.connect();
                Log.d("CHECK",action.toString());
                return true;
            } else if ("setPairingType".equals(action)) {
                ConnectableDeviceWrapper deviceWrapper = getDeviceWrapper(args.getString(0));
                deviceWrapper.setCallbackContext(successCallback, errorCallback);
                deviceWrapper.setPairingType(getPairingTypeFromString(args.getString(1)));
                return true;
            } else if ("disconnectDevice".equals(action)) {
                ConnectableDeviceWrapper deviceWrapper = getDeviceWrapper(args.getString(0));
                deviceWrapper.disconnect();
                return true;
            } else if ("acquireWrappedObject".equals(action)) {
                String objectId = args.getString(0);
                JSObjectWrapper wrapper = objectWrappers.get(objectId);

                return true;
            } else if ("releaseWrappedObject".equals(action)) {
                String objectId = args.getString(0);
                JSObjectWrapper wrapper = objectWrappers.get(objectId);

                if (wrapper != null) {
                    removeObjectWrapper(wrapper);
                }

                return true;
            }
        } catch (NoSuchDeviceException e) {
            error(errorCallback, "no such device");
            return true;
        } catch (JSONException e) {
            Log.d(LOG_TAG, "exception while handling " + action, e);
            error(errorCallback, e.toString());
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        Log.w(LOG_TAG, "no handler for exec action " + action);
        return false;
    }

    void initDiscoveryManagerWrapper() {
        if (discoveryManagerWrapper == null) {
            DiscoveryManager.init(mCtx.getApplicationContext());
            discoveryManager = DiscoveryManager.getInstance();
           
            
            discoveryManager.registerDefaultDeviceTypes();
            try {
                discoveryManager.registerDeviceService((Class<DeviceService>) Class.forName("com.connectsdk.service.CastService"), (Class<DiscoveryProvider>)Class.forName("com.connectsdk.discovery.provider.CastDiscoveryProvider"));
              } catch (ClassNotFoundException e) {
                Log.d("CHROME","CHROME CAST NOT REGISTERED");
                e.printStackTrace();
              }
            discoveryManagerWrapper = new DiscoveryManagerWrapper(this, discoveryManager);
            Log.d("CALLED 9998","CALLED 9988");

        }
    }

    @ReactMethod
    void stopCasting(){
        try {
            if(mTV.getCapability(MediaControl.class) != null) {
                mTV.getCapability(MediaControl.class).stop((new ResponseListener<Object>() {

                    @Override
                    public void onSuccess(Object response) {
                        mTV.disconnect();
                        connectableDevice.disconnect();
                    }

                    @Override
                    public void onError(ServiceCommandError error) {
                    }
                }));
//            mTV.getMediaControl().stop();
            }
        } catch(Exception e) {
            Log.d("ERROR 123", e.toString());
        }

    }

    private MediaControl.PositionListener positionListener = new MediaControl.PositionListener() {
        @Override
        public void onSuccess(Long position) {
            WritableMap params = Arguments.createMap();
            params.putString("totalDuration", String.valueOf(totalTimeDuration.intValue()));
            params.putString("currentPosition", String.valueOf(position.intValue()));
            getContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("duration", params);
        }

        @Override
        public void onError(ServiceCommandError error) {

        }
    };

    private MediaControl.DurationListener durationListener = new MediaControl.DurationListener() {

        @Override public void onError(ServiceCommandError error) { }

        @Override
        public void onSuccess(Long duration) {
            totalTimeDuration = duration;
            if(mTV.isConnected() && mTV.getCapability(MediaControl.class) != null ) {
                mTV.getCapability(MediaControl.class).
                        getPosition(positionListener);
            }
        }
    };

    @ReactMethod
    void setSelectedDevice(String deviceId, ReadableMap movieData,Callback callback){
        Map<String, ConnectableDevice> list = discoveryManager.getAllDevices();
        list.forEach((k, v) -> {
            Log.d("DEVICE ID ",deviceId);

            Log.d("LIST ITERATOR ",v.toString()+" K VALUE : "+k.toString());
            if(v.getId().equals(deviceId)){
                Log.d("TV Connected",movieData.toString());
                ConnectableDeviceWrapper wrapper = getDeviceWrapper(v);
                wrapper.connect();
                v.connect();
                mTV = v;
                mTV.connect();
                connectableDevice = v;
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        if(mTV.isConnected()){
                            CastToTvVideo(movieData, callback);
                        }
                        // Actions to do after 5 seconds
                    }
                }, 5000);



            }
        });


    }

    @ReactMethod
    void startDiscovery(String mapArgs, final Callback Callback) throws JSONException, ClassNotFoundException {
        JSONArray args = new JSONArray(mapArgs);
        initDiscoveryManagerWrapper();

        if (mapArgs != null && args.length() > 0) {
            for (int i=0; i< args.length(); i++) {
                JSONObject arg = args.getJSONObject(i);
                discoveryManagerWrapper.configure(arg);
            }
        }

        discoveryManagerWrapper.setCallbackContext(Callback);
        discoveryManagerWrapper.start();
    }


    void stopDiscovery(final Callback Callback) throws JSONException {
        if (discoveryManagerWrapper != null) {
            discoveryManagerWrapper.stop();
        }
        discoveryManager = null;
        discoveryManagerWrapper = null;
        success(Callback);
    }

    void setDiscoveryConfig(JSONArray args, final Callback Callback) throws JSONException {
        initDiscoveryManagerWrapper();
        discoveryManagerWrapper.configure(args.getJSONObject(0));

        success(Callback);
    }

    private void setupPicker() {
        Log.d("HELLO WORLD" ,"TRIGGERED 4");
        dp = new DevicePicker(getCurrentActivity());
        dialog = dp.getPickerDialog("Cast to", new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                mTV = (ConnectableDevice)arg0.getItemAtPosition(arg2);
                mTV.setPairingType(null);
                mTV.connect();
                Log.d("HELLO WORLD : ",mTV.getFriendlyName().toString());

                dp.pickDevice(mTV);
            }
        });

    }

    private void showImage() {
        String imagePath = "https://picsum.photos/200/300";
        String mimeType = "image/jpeg";
        String title = "Connect SDK";
        String description = "One SDK Eight Media Platforms";
        String icon = "http://TVCast.com/TVCast_Logo.jpg";

        MediaInfo mediaInfo = new MediaInfo.Builder(imagePath, mimeType)
                .setTitle(title)
                .setDescription(description)
                .setIcon(icon)
                .build();
        baseFragment.getMediaPlayer().displayImage(mediaInfo, new MediaPlayer.LaunchListener() {

            @Override
            public void onError(ServiceCommandError error) {
                Log.e("Error", "Error displaying Image", error);
            }

            @Override
            public void onSuccess(MediaPlayer.MediaLaunchObject object) {
                launchSession = object.launchSession;
            }
        });
    }


    void pickDevice(JSONArray args, final Callback Callback) throws JSONException {
        Log.d("HELLO WORLD ","TRIGGERED pick device 2");
        // try {
        //     discoveryManager.registerDeviceService((Class<DeviceService>) Class.forName("com.TVCast.service.CastService"), (Class<DiscoveryProvider>)Class.forName("com.TVCast.discovery.provider.CastDiscoveryProvider"));
        // } catch (ClassNotFoundException e) {
        //     Log.d("CHROME : ","CHROME CAST NOT ACTIVATED");
        //     e.printStackTrace();
        // }

        JSONObject options = args.optJSONObject(0);
        String pairingTypeString = null;
        if (options != null) {
            Log.d("OPTIONS : ",options.toString());

            pairingTypeString = options.optString("pairingType");
        }

        if (discoveryManager != null) {
            final DeviceService.PairingType pairingType = getPairingTypeFromString(pairingTypeString);

            runOnUiThread(() -> {
                if (picker == null) {
                    picker = new SimpleDevicePicker(getCurrentActivity());
                }
                Log.d("HELLO WORLD : ",pairingType.toString());

                picker.setPairingType(pairingType);
                picker.setListener(new SimpleDevicePickerListener() {
                    @Override
                    public void onPrepareDevice(ConnectableDevice device) {
                    }

                    @Override
                    public void onPickDevice(ConnectableDevice device) {
                        ConnectableDeviceWrapper wrapper = getDeviceWrapper(device);
                        wrapper.connect();
                        mTV =  device;
                        connectableDevice = device;
                        device.connect();

                        Log.d("Device PROPS : ",device.toString());
                        Log.d("HELLO WORLD ","TRIGGERED pick device 3");
                        sendEvent(Callback, "device", wrapper.toJSONObject());

                    }

                    @Override
                    public void onPickDeviceFailed(boolean canceled) {
                    }
                });
                picker.showPicker();
            });
        } else {
            error(Callback, "discovery not started");
        }
    }

    public static WritableArray convertJsonToArray(JSONArray jsonArray) throws JSONException {
        WritableArray array = Arguments.createArray();

        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONObject) {
                array.pushMap(convertJsonToMap((JSONObject) value));
            } else if (value instanceof  JSONArray) {
                array.pushArray(convertJsonToArray((JSONArray) value));
            } else if (value instanceof  Boolean) {
                array.pushBoolean((Boolean) value);
            } else if (value instanceof  Integer) {
                array.pushInt((Integer) value);
            } else if (value instanceof  Double) {
                array.pushDouble((Double) value);
            } else if (value instanceof String)  {
                array.pushString((String) value);
            }
        }
        return array;
    }

    public static WritableMap convertJsonToMap(JSONObject jsonObject) throws JSONException {
        WritableMap map = new WritableNativeMap();

        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.putMap(key, convertJsonToMap((JSONObject) value));
            } else if (value instanceof  JSONArray) {
                map.putArray(key, convertJsonToArray((JSONArray) value));
            } else if (value instanceof  Boolean) {
                map.putBoolean(key, (Boolean) value);
            } else if (value instanceof  Integer) {
                map.putInt(key, (Integer) value);
            } else if (value instanceof  Double) {
                map.putDouble(key, (Double) value);
            } else if (value instanceof String)  {
                map.putString(key, (String) value);
            } else {
                map.putString(key, value.toString());
            }
        }
        return map;
    }

    @Nullable
    public static JSONObject convertMapToJson(ReadableMap readableMap) {
        JSONObject jsonObject = new JSONObject();
        if (readableMap == null) {
            return null;
        }
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        if (!iterator.hasNextKey()) {
            return null;
        }
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            ReadableType readableType = readableMap.getType(key);
            try {
                switch (readableType) {
                    case Null:
                        jsonObject.put(key, null);
                        break;
                    case Boolean:
                        jsonObject.put(key, readableMap.getBoolean(key));
                        break;
                    case Number:
                        // Can be int or double.
                        jsonObject.put(key, readableMap.getInt(key));
                        break;
                    case String:
                        jsonObject.put(key, readableMap.getString(key));
                        break;
                    case Map:
                        jsonObject.put(key, convertMapToJson(readableMap.getMap(key)));
                        break;
                    case Array:
                        jsonObject.put(key, convertArrayToJson(Objects.requireNonNull(readableMap.getArray(key))));
                    default:
                        // Do nothing and fail silently
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }
        return jsonObject;
    }

    @Nullable
    public static JSONArray convertMapToArray(ReadableMap readableMap) {
        JSONArray jsonObject = new JSONArray();
        if (readableMap == null) {
            return null;
        }
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        if (!iterator.hasNextKey()) {
            return null;
        }
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            ReadableType readableType = readableMap.getType(key);
            try {
                switch (readableType) {
                    case Null:
                        jsonObject.put(null);
                        break;
                    case Boolean:
                        jsonObject.put(readableMap.getBoolean(key));
                        break;
                    case Number:
                        // Can be int or double.
                        jsonObject.put(readableMap.getInt(key));
                        break;
                    case String:
                        jsonObject.put(readableMap.getString(key));
                        break;
                    case Map:
                        jsonObject.put(convertMapToJson(readableMap.getMap(key)));
                        break;
                    case Array:
                        jsonObject.put(convertArrayToJson(Objects.requireNonNull(readableMap.getArray(key))));
                    default:
                        // Do nothing and fail silently
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }
        return jsonObject;
    }

    public static JSONArray convertArrayToJson(ReadableArray readableArray) throws JSONException {
        JSONArray array = new JSONArray();
        for (int i = 0; i < readableArray.size(); i++) {
            switch (readableArray.getType(i)) {
                case Null:
                    break;
                case Boolean:
                    array.put(readableArray.getBoolean(i));
                    break;
                case Number:
                    array.put(readableArray.getDouble(i));
                    break;
                case String:
                    array.put(readableArray.getString(i));
                    break;
                case Map:
                    array.put(convertMapToJson(readableArray.getMap(i)));
                    break;
                case Array:
                    array.put(convertArrayToJson(readableArray.getArray(i)));
                    break;
            }
        }
        return array;
    }

    public static JSONArray listToJSON(Iterable<? extends JSONSerializable> list) {
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

    public void sendEvent(Callback callbackContext, String event, Object ... objs) {
        if (event == null) return;
        JSONObject json = new JSONObject();
        WritableNativeMap map = new WritableNativeMap();
        WritableNativeArray arr = new WritableNativeArray();
        Log.d("HELLO WORLD : ",event.toString());
        try {
            Log.d(LOG_TAG, "sendEvent 123" + objs[0].toString());
            String jsonInString = objs[0].toString();
            JSONObject mJSONObject = new JSONObject(jsonInString);
            for (Iterator<String> it = mJSONObject.keys(); it.hasNext(); ) {
                if(it.hasNext()) {
                    String key = it.next();
                    json.put(key, mJSONObject.get(key));
                }
            }

            map = (WritableNativeMap) convertJsonToMap(json);
            if (objs.length > 1)
            {
                for (Object obj : objs) {
                    arr.pushString(obj.toString());
                }

                map.putArray(event, arr);
            }
            else
            {
                map = (WritableNativeMap) convertJsonToMap(mJSONObject);
            }
            if (callbackContext == null)
                getContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit(event, map);
            else
                callbackContext.invoke(map);
        }
        catch (RuntimeException e) {
            Log.d("CHECK DATA 1",e.toString());
            getContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(event, map);
        }
        catch (Exception ex) {
            Log.d("CHECK DATA 2",ex.toString());

            ex.printStackTrace();
        }
    }

    void sendSuccessEvent(Callback callback, Object ... objs) {
        if (callback == null) return;
        WritableNativeArray arr = new WritableNativeArray();

        try {
            JSONArray arrObj = new JSONArray(objs.toString());
            for (int i=0; i < arrObj.length(); i++) {
                JSONObject json = arrObj.getJSONObject(i);
                arr.pushMap(convertJsonToMap(json));
            }

            callback.invoke(arr);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void success(Callback callback) {
        sendSuccessEvent(callback);
    }

    public void success(Callback callback,JSONObject obj) {
        sendSuccessEvent(callback, obj);
    }

    public void success(Callback callback, JSONArray arr) {
        sendSuccessEvent(callback, arr);
    }

    public void success(Callback callback,JSONSerializable obj) {
        JSONObject response = null;

        try {
            response = obj.toJSONObject();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (response != null) {
            success(callback, response);
        }
    }

    public <T extends JSONSerializable> void success(Callback callback, List<T> list) {
        JSONArray response = listToJSON(list);

        success(callback, response);
    }

    public void success(Callback callback, Number obj) {
        sendSuccessEvent(callback, obj);
    }

    public void success(Callback callback,Boolean obj) {
        sendSuccessEvent(callback, obj);
    }

    public void error(Callback callback, String errorMessage) {
        if (callback == null) return;

        JSONObject errorObj = new JSONObject();

        try {
            errorObj.put("message", errorMessage);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        callback.invoke(errorObj);
    }

    public void error(Callback callback, Exception ex) {
        if (callback == null) return;

        JSONObject errorObj = new JSONObject();

        try {
            errorObj.put("message", ex.getMessage());
            errorObj.put("detail", ex.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        callback.invoke(errorObj);
    }

    public void error(Callback callback, ServiceCommandError error) {
        if (callback == null) return;

        WritableMap errorObj = Arguments.createMap();

        errorObj.putString("code", String.valueOf(error.getCode()));
        errorObj.putString("message", error.getMessage());
        errorObj.putString("detail", String.valueOf(error.getPayload()));
        callback.invoke(errorObj);
    }

    public void addObjectWrapper(JSObjectWrapper wrapper) {
        objectWrappers.put(wrapper.objectId, wrapper);
    }

    public JSObjectWrapper getObjectWrapper(String objectId) {
        return objectWrappers.get(objectId);
    }

    public void removeObjectWrapper(JSObjectWrapper wrapper) {
        objectWrappers.remove(wrapper.objectId);
        wrapper.cleanup();
    }

    private DeviceService.PairingType getPairingTypeFromString(String pairingTypeString) {
        if (JS_PAIRING_TYPE_FIRST_SCREEN.equalsIgnoreCase(pairingTypeString)) {
            return DeviceService.PairingType.FIRST_SCREEN;
        } else if (JS_PAIRING_TYPE_PIN.equalsIgnoreCase(pairingTypeString)) {
            return DeviceService.PairingType.PIN_CODE;
        } else if (JS_PAIRING_TYPE_MIXED.equalsIgnoreCase(pairingTypeString)) {
            return DeviceService.PairingType.MIXED;
        }
        return DeviceService.PairingType.NONE;
    }
}
