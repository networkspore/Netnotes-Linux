package com.netnotes;

import com.google.gson.JsonObject;

import java.math.BigDecimal;

import com.google.gson.JsonElement;

public class RangeStyle {

    public static final String USER = "User";
    public static final String AUTO = "Auto";
    public static final String NONE = "None";

    private BigDecimal m_topValue = null;
    private BigDecimal m_botValue = null;

    private String m_name;
    private String m_id;

    public static String[] AVAILABLE_RANGE_STYLES = new String[]{
        USER, AUTO, NONE
    };


    public RangeStyle(String id) {
        m_name = id;
        m_id = id;
    }

    public RangeStyle(JsonObject json){
        openJson(json);
    }

    public RangeStyle(String name, String id) {
        m_id = id;
        m_name = name;
    }

    public RangeStyle(String name, String id, BigDecimal topValue, BigDecimal botValue){
        m_name = name;
        m_id = id;
        m_topValue = topValue;
        m_botValue = botValue;
    }

    public void setStyle(String id){
        m_name = id;
        m_id = id;
    }

    public String getName() {
        return m_name;
    }

    public String getId() {
        return m_id;
    }

    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("id", m_id);
        json.addProperty("name", m_name);
        json.addProperty("topValue", m_topValue);
        json.addProperty("botValue", m_botValue);
        return json;
    }

    public void openJson(JsonObject json) throws NullPointerException{
        if(json != null){
            JsonElement idElement = json.get("id");
            JsonElement nameElement = json.get("name");
            JsonElement topValueElement = json.get("topValue");
            JsonElement botValueElement = json.get("botValue");

            if(idElement != null && idElement.isJsonPrimitive() && nameElement != null && nameElement.isJsonPrimitive()){
                m_id = idElement.getAsString();
                m_name = nameElement.getAsString();

                m_topValue = topValueElement != null && topValueElement.isJsonPrimitive() ? topValueElement.getAsBigDecimal() : null;
                m_botValue = botValueElement != null && botValueElement.isJsonPrimitive() ? botValueElement.getAsBigDecimal() : null;

            }else{
                throw new NullPointerException("RangeStyle elements are null");
            }

        }else{
            throw new NullPointerException("RangeStyle json is null");
        }
    }

    public void setTopValue(BigDecimal topRange){
        m_topValue = topRange;
    }

    public void setBottomValue(BigDecimal botRange){
        m_botValue = botRange;
    }

    public BigDecimal getTopValue(){
        return m_topValue;
    }

    public BigDecimal getBotValue(){
        return m_botValue;
    }
}
