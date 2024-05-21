package com.netnotes;

public interface SpectrumMarketInterface  {
    String getId();
    void sendMessage(int code, long timestamp);
    void sendMessage(int code, long timestamp, String msg);
}
