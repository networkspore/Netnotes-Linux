package com.netnotes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.google.gson.JsonObject;
import io.netnotes.engine.Utils;


public class ErgoDexPriceData {

    private long m_startTimestamp = 0;
    private BigDecimal m_open = BigDecimal.ZERO;
    private BigDecimal m_close = BigDecimal.ZERO;
    private BigDecimal m_high = BigDecimal.ZERO;
    private BigDecimal m_low = BigDecimal.ZERO;

    private long m_epochEnd = 0;
    private int m_count = 0;
    private boolean m_lastCloseDirection = false;
    private long m_lastTimeStamp = 0;
    
    public ErgoDexPriceData(long epochStart, long epochEnd, BigDecimal price){
        m_startTimestamp = epochStart;
        m_epochEnd = epochEnd;
        m_open = price;
        m_close = price;
        m_high = price;
        m_low = price;
        m_lastTimeStamp = epochStart;
    }

    public ErgoDexPriceData(long timestamp, long epochEnd){
        m_startTimestamp = timestamp;
        m_epochEnd = epochEnd;
        m_lastTimeStamp = timestamp;
    }


    public ErgoDexPriceData(ErgoDexPrice spectrumPrice, long epochStart, long epochEnd){
        m_startTimestamp = epochStart;
        m_lastTimeStamp = epochStart;

        BigDecimal price = spectrumPrice.getPrice();

        m_open = price;
        m_low = price;
        m_high = price;
        m_close = price;
        m_epochEnd = epochEnd;
    }

    public long getEpochEnd(){
        return m_epochEnd;
    }


    public ErgoDexPriceData(long timestamp, BigDecimal open, BigDecimal close, BigDecimal high, BigDecimal low, long epochStart, long epochEnd) {
        m_startTimestamp = epochStart;
        m_open = open;
        m_close = close;
        m_high = high;
        m_low = low;
        m_epochEnd = epochEnd;
        m_lastTimeStamp = epochStart;
    }
/*
    public long getPriceAreaTimeStamp(){
        return m_priceAreaTimeStamp;
    }

    public int getDirection(){
        return m_direction;
    }
    public int getX(){
        return m_x;
    }
    public int getHighY(){
        return m_highY;
    }
    public int getLowY(){
        return m_lowY;
    }
    public int getOpenY(){
        return m_openY;
    }
    public int getCloseY(){
        return m_closeY;
    }

    public void setPriceArea(long timeStamp, int direction, int x, int highY, int lowY, int openY, int closeY){
        m_priceAreaTimeStamp = timeStamp;
        m_direction = direction;
        m_x = x;
        m_highY = highY;
        m_lowY = lowY;
        m_openY = openY;
        m_closeY = closeY;
    }*/


    public String getCloseString() {
        return m_close.toString();
    }

    public long getEpochStart() {
        return m_startTimestamp;
    }

    public long getLastTimeStamp(){
        return m_lastTimeStamp;
    }

    public void setLastTimeStamp(long timeStamp){
        m_lastTimeStamp = timeStamp;
    }

    public void addPrice(long timestamp, BigDecimal price){
        if(timestamp >= m_startTimestamp && timestamp < m_epochEnd){
           
       
            m_low = m_low.equals(BigDecimal.ZERO) ? price :  m_low.min(price);
            m_high = m_high.max(price);

            int compareTo = m_close.compareTo(price);
            m_close = price;

            if(compareTo != 0){
                m_lastCloseDirection = compareTo == -1;
            }
            m_lastTimeStamp = timestamp;
            m_count++;
        }
        

     
    }

    public boolean getLastCloseDirection(){
        return m_lastCloseDirection;
    }
    public void setLastCloseDirection(boolean value){
        m_lastCloseDirection = value;
    }

    public LocalDateTime getLocalDateTime() {
       
        return Utils.milliToLocalTime(m_epochEnd);
        
    }

    public void setCount(int count){
        m_count = count;
    }

    public int getCount(){
        return m_count;
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
        jsonObject.addProperty("epochStartTimeStamp", getEpochStart());
        jsonObject.addProperty("epchEndTimeStamp", getEpochEnd());
        return jsonObject;
    }
}
