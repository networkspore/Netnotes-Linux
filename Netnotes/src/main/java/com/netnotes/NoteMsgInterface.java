package com.netnotes;


import java.lang.Number;

public interface NoteMsgInterface  {

    String getId();
    void sendMessage(int code, long timestamp, String networkId, String msg);
    void sendMessage(int code, long timestamp, String networkId, Number number);

}
