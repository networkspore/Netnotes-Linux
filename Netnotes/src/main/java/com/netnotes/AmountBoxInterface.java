package com.netnotes;

import java.math.BigDecimal;

public interface AmountBoxInterface {
    String getTokenId();
    long getTimeStamp();
    void setTimeStamp(long timeStamp);
    void shutdown();
    PriceAmount getPriceAmount();


}
