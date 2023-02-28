package com.github.TVCast;

import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.service.sessions.WebAppSession;

public class WebAppSessionWrapper extends JSObjectWrapper {
    WebAppSession session;

    public WebAppSessionWrapper(TVConnectModule module, WebAppSession session) {
        super(module);
        this.session = session;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject obj = new JSONObject(); // FIXME serialize session
        obj.put("objectId", objectId);

        if (session.launchSession != null) {
            obj.put("launchSession", session.launchSession.toJSONObject());
        }

        return obj;
    }

    @Override
    public void cleanup() {
        session.setWebAppSessionListener(null);
        session = null;

        super.cleanup();
    }
}

