package com.netnotes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.google.gson.JsonObject;
import com.utils.Utils;


public class SpectrumPriceData {

    private long m_latestTimestamp = 0;
    private BigDecimal m_open = BigDecimal.ZERO;
    private BigDecimal m_close = BigDecimal.ZERO;
    private BigDecimal m_high = BigDecimal.ZERO;
    private BigDecimal m_low = BigDecimal.ZERO;

    private long m_epochEnd = 0;
    
    public SpectrumPriceData(long timestamp, long epochEnd, BigDecimal price){
       m_latestTimestamp = timestamp;
        m_epochEnd = epochEnd;
        m_open = price;
        m_close = price;
        m_high = price;
        m_low = price;
    }


    public SpectrumPriceData(SpectrumPrice spectrumPrice, long epochEnd){

        BigDecimal price = spectrumPrice.getPrice();
        m_latestTimestamp = spectrumPrice.getTimeStamp();
        m_open = price;
        m_low = price;
        m_high = price;
        m_close = price;
        m_epochEnd = epochEnd;
    }

    public long getEpochEnd(){
        return m_epochEnd;
    }


    public SpectrumPriceData(long timestamp, BigDecimal open, BigDecimal close, BigDecimal high, BigDecimal low, long epochEnd) {
        m_latestTimestamp = timestamp;
        m_open = open;
        m_close = close;
        m_high = high;
        m_low = low;
        m_epochEnd = epochEnd;
    }



    public String getCloseString() {
        return m_close.toString();
    }

    public long getLatestTimestamp() {
        return m_latestTimestamp;
    }

    public void addPrice(long timestamp, BigDecimal price){
  
       m_latestTimestamp = timestamp;
        m_close = price;
       
        m_low = m_low.min(price);
        m_high = m_high.max(price);
        
    }

    public LocalDateTime getLocalDateTime() {
       
        return Utils.milliToLocalTime(m_latestTimestamp);
        
    }

    public void setLatestTimestamp(long timeStamp) {
        m_latestTimestamp = timeStamp;
    }

    public BigDecimal getOpen() {
        return m_open;
    }

    public void setOpen(BigDecimal open) {
        m_open = open;
    }

    public BigDecimal getClose() {
        return m_close;
    }

    public void setClose(BigDecimal close) {
        m_close = close;
    }

    public BigDecimal getHigh() {
        return m_high;
    }

    public void setHigh(BigDecimal high) {
        m_high = high;
    }

    public BigDecimal getLow() {
        return m_low;
    }

    public void setLow(BigDecimal low) {
        m_low = low;
    }


    public JsonObject getJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("open", getOpen());
        jsonObject.addProperty("close",getClose());
        jsonObject.addProperty("high", getHigh());
        jsonObject.addProperty("low", getLow());
        jsonObject.addProperty("latestTimeStamp", getLatestTimestamp());
        jsonObject.addProperty("epchEndTimeStamp", getEpochEnd());
        return jsonObject;
    }
}
