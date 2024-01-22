package com.netnotes;

import com.google.gson.JsonObject;

public interface MessageInterface {

    String getSubject();

    String getTopic();

    String getTunnelId();

    String getId();

    void onMsgChanged(JsonObject jsonObject);

    void onReady();
}
