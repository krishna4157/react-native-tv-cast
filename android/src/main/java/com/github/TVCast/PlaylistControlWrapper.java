package com.github.TVCast;


import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.PlaylistControl;

public class PlaylistControlWrapper extends JSObjectWrapper {
    PlaylistControl playlistControl;

    public PlaylistControlWrapper(ConnectSDKModule module, PlaylistControl control) {
        super(module);
        this.playlistControl = control;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("objectId", objectId);

        return obj;
    }

    @Override
    public void cleanup() {
        playlistControl = null;

        super.cleanup();
    }
}
