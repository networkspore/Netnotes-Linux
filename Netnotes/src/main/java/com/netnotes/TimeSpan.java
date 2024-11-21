package com.netnotes;

import com.google.gson.JsonObject;

import java.math.BigDecimal;

import com.google.gson.JsonElement;

public class TimeSpan {

    private BigDecimal m_millis;
    private String m_name;
    private String m_id;

    public static String[] AVAILABLE_TIMESPANS = new String[]{
        "1min", "3min", "15min", "30min", "1hour", "2hour", "4hour", "6hour", "8hour", "12hour", "1day", "1week"
    };


    public TimeSpan(String id) {
        setup(id);
    }

    public TimeSpan(JsonObject json){
        openJson(json);
    }

    public TimeSpan(String name, String id, long seconds) {
        m_millis = new BigDecimal(seconds).movePointRight(3);
        m_id = id;
        m_name = name;
    }

    public TimeSpan(long millis, String name, String id) {
        m_millis = new BigDecimal(millis);
        m_id = id;
        m_name = name;
    }

    public String getName() {
        return m_name;
    }

    public long getSeconds() {
        return m_millis.movePointLeft(3).longValue();
    }
    public long getMillis(){
        return m_millis.longValue();
    }

    public String getId() {
        return m_id;
    }

    @Override
    public String toString(){
        return m_name;
    }

    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("id", m_id);
        json.addProperty("name", m_name);
        json.addProperty("millis", m_millis);
        return json;
    }

    public void openJson(JsonObject json) throws NullPointerException{
        if(json != null){
            JsonElement idElement = json.get("id");
            JsonElement secondsElement = json.get("seconds");
            JsonElement nameElement = json.get("name");
            JsonElement millisElement = json.get("millis");
            
            if(idElement != null && idElement.isJsonPrimitive() && nameElement != null && nameElement.isJsonPrimitive()){
                m_id = idElement.getAsString();
                
                m_millis = secondsElement != null && secondsElement.isJsonPrimitive() ? new BigDecimal(secondsElement.getAsLong()).movePointRight(3) : (millisElement != null && millisElement.isJsonPrimitive() ? millisElement.getAsBigDecimal() : BigDecimal.ZERO);
               
                m_name = nameElement.getAsString();
            }else{
                throw new NullPointerException("Timespan elements are null");
            }

        }else{
            throw new NullPointerException("Timespan json is null");
        }
    }

    private void setup(String id) {
        m_id = id;

        switch (id) {
            case "1min":
                m_name = "1 min";
                m_millis = new BigDecimal(60000);
                break;
            case "3min":
                m_name = "3 min";
                m_millis = new BigDecimal(60000 * 3);
                break;
            case "15min":
                m_name = "15 min";
                m_millis = new BigDecimal(60000 * 15);
                break;
            case "30min":
                m_name = "30 min";
                m_millis = new BigDecimal(60000 * 30);
                break;
            case "1hour":
                m_name = "1 hour";
                m_millis = new BigDecimal(60000 * 60);
                break;
            case "2hour":
                m_name = "2 hour";
                m_millis = new BigDecimal(60000 * 60 * 2);
                break;
            case "4hour":
                m_name = "4 hour";
                m_millis = new BigDecimal(60000 * 60 * 4);
                break;
            case "6hour":
                m_name = "6 hour";
                m_millis = new BigDecimal(60000 * 60 * 6);
                break;
            case "8hour":
                m_name = "8 hour";
                m_millis = new BigDecimal(60000 * 60 * 8);
                break;
            case "12hour":
                m_name = "12 hour";
                m_millis = new BigDecimal(60000 * 60 * 12);
                break;
            case "1day":
                m_name = "1 day";
                m_millis = new BigDecimal(60000 * 60 * 24);
                break;
            case "7day":
            case "1week":
                m_name = "1 week";
                m_millis = new BigDecimal(60000L * 60L * 24L * 7L);
                break;
            case "30day":
            case "1month":
                m_name = "1 month";
                m_millis = new BigDecimal(60000L * 60L * 24L * 30L);
                break;
            case "6month":
                m_name = "6 month";
                m_millis = new BigDecimal(60000L * 60L * 24L * 30L * 6L);
                break;
            case "1year":
                m_name = "1 year";
                m_millis = new BigDecimal(60000L * 60L * 24L * 365L);
                break;

        }
    }
}
