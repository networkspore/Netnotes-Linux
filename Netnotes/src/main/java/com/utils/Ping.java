package com.utils;

import java.util.ArrayList;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;



public class Ping{
    private boolean m_available = false;
    private long m_timeStamp = 0;
    private String m_error = "";
    private double m_avgPing = 0;

    public Ping(boolean available, String error, double avgPing){
        m_available = available;
        m_error = error;
        m_avgPing = avgPing;
        m_timeStamp = System.currentTimeMillis();
    }

    public Ping(JsonObject json){
        JsonElement availableElement = json != null ? json.get("available") : null;
        JsonElement timeStampElement = json != null ? json.get("timeStamp") : null;
        JsonElement avgPingElement = json != null ? json.get("ping") : null;
        JsonElement errorElement = json != null ? json.get("error") : null;

        m_available = availableElement != null ? availableElement.getAsBoolean() : false;
        m_timeStamp = timeStampElement != null ? timeStampElement.getAsLong() : 0;
        m_avgPing = avgPingElement != null ? avgPingElement.getAsDouble() : 0;
        m_error = errorElement != null ? errorElement.getAsString() : "not initialized";
  
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

    public double getAvgPing(){
        return m_avgPing;
    }

    public void setAvgPing(double ping){
        m_avgPing = ping;
    }

    public String getError(){
        return m_error;
    }

    public void setError(String error){
        m_error = error;
    }

    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("available", m_available);
        json.addProperty("timeStamp", m_timeStamp);
        json.addProperty("ping", m_avgPing);
        json.addProperty("error", m_error);
        return json;
    }
}