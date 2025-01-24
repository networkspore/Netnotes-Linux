package com.netnotes;

public interface AmountBoxInterface {
    String getTokenId();
    long getTimeStamp();
    void setTimeStamp(long timeStamp);
    void shutdown();
    PriceAmount getPriceAmount();


}
