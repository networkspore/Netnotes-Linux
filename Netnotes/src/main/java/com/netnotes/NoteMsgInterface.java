package com.netnotes;

import com.google.gson.JsonObject;

public interface NoteMsgInterface  {
    String getId();
    void sendMessage(String networkId, int code, long timestamp);
    void sendMessage(int code, long timestamp);
    void sendMessage(int code, long timestamp, String msg);
    void sendMessage(String networkId, int code, long timestamp, String msg);
    void sendMessage(String networkId, int code, long timestamp, JsonObject json);
}
