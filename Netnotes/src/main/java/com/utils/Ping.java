package com.utils;

public class Ping{
    private boolean m_available;
    private long m_timeStamp;
    private String m_error;
    private int m_ping;

    public Ping(boolean available, String error, int ping){
        m_available = available;
        m_error = error;
        m_ping = ping;
        m_timeStamp = System.currentTimeMillis();
    }

    public boolean getAvailable(){
        return m_available;
    }

    public void setAvailable(boolean available){
        m_available = available;
    }

    public void setTimestamp(long timestamp){
        m_timeStamp = timestamp;
    }

    public long getTimeStamp(){
        return m_timeStamp;
    }

    public int getPing(){
        return m_ping;
    }

    public void setPing(int ping){
        m_ping = ping;
    }

    public String getError(){
        return m_error;
    }

    public void setError(String error){
        m_error = error;
    }

}