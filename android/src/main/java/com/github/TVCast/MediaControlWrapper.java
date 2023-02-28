package com.github.TVCast;

import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.service.capability.MediaControl;

public class MediaControlWrapper extends JSObjectWrapper {
    MediaControl mediaControl;

    public MediaControlWrapper(TVConnectModule module, MediaControl control) {
        super(module);
        this.mediaControl = control;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("objectId", objectId);

        return obj;
    }

    @Override
    public void cleanup() {
        mediaControl = null;

        super.cleanup();
    }
}
