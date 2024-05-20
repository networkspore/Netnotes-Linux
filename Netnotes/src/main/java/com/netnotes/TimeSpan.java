package com.netnotes;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

public class TimeSpan {

    private long m_seconds;
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
        m_seconds = seconds;
        m_id = id;
        m_name = name;
    }

    public String getName() {
        return m_name;
    }

    public long getSeconds() {
        return m_seconds;
    }
    public long getMillis(){
        return m_seconds * 1000;
    }

    public String getId() {
        return m_id;
    }

    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("id", m_id);
        json.addProperty("seconds", m_seconds);
        json.addProperty("name", m_name);
        return json;
    }

    public void openJson(JsonObject json) throws NullPointerException{
        if(json != null){
            JsonElement idElement = json.get("id");
            JsonElement secondsElement = json.get("seconds");
            JsonElement nameElement = json.get("name");

            if(idElement != null && idElement.isJsonPrimitive() && secondsElement != null && secondsElement.isJsonPrimitive() && nameElement != null && nameElement.isJsonPrimitive()){
                m_id = idElement.getAsString();
                m_seconds = secondsElement.getAsLong();
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
                m_seconds = 60;
                break;
            case "3min":
                m_name = "3 min";
                m_seconds = 60 * 3;
                break;
            case "15min":
                m_name = "15 min";
                m_seconds = 60 * 15;
                break;
            case "30min":
                m_name = "30 min";
                m_seconds = 60 * 30;
                break;
            case "1hour":
                m_name = "1 hour";
                m_seconds = 60 * 60;
                break;
            case "2hour":
                m_name = "2 hour";
                m_seconds = 60 * 60 * 2;
                break;
            case "4hour":
                m_name = "4 hour";
                m_seconds = 60 * 60 * 4;
                break;
            case "6hour":
                m_name = "6 hour";
                m_seconds = 60 * 60 * 6;
                break;
            case "8hour":
                m_name = "8 hour";
                m_seconds = 60 * 60 * 8;
                break;
            case "12hour":
                m_name = "12 hour";
                m_seconds = 60 * 60 * 12;
                break;
            case "1day":
                m_name = "1 day";
                m_seconds = 60 * 60 * 24;
                break;
            case "7day":
            case "1week":
                m_name = "1 week";
                m_seconds = 60 * 60 * 24 * 7;
                break;
            case "30day":
            case "1month":
                m_name = "1 month";
                m_seconds = 60 * 60 * 24 * 30;
                break;
            case "6month":
                m_name = "6 month";
                m_seconds = 60 * 60 * 24 * 30 * 6;
                break;
            case "1year":
                m_name = "1 year";
                m_seconds = 60 * 60 * 24 * 365;
                break;

        }
    }
}
