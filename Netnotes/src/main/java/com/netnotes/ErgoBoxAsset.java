package com.netnotes;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

public class ErgoBoxAsset {
    private String m_tokenId;
    private long m_amount;
    private int m_index;
    private String m_name = null;
    private int m_decimals = 0;
    private String m_type = null;

    public boolean isType(){
        return m_type != null;
    }

    public boolean isName(){
        return m_name != null;
    }

    public String getTokenId() {
        return m_tokenId;
    }

    public long getAmount() {
        return m_amount;
    }

    public int getIndex() {
        return m_index;
    }

    public String getName() {
        return m_name;
    }

    public int getDecimals() {
        return m_decimals;
    }

    public String getType() {
        return m_type;
    }

    public ErgoBoxAsset(long amount, String tokenId, String name, int decimals, String tokenType){
        m_amount = amount;
        m_tokenId = tokenId;
        m_name = name;
        m_type = tokenType;
        m_decimals = decimals;
    }

    public ErgoBoxAsset(JsonObject json) throws NullPointerException{

        JsonElement tokenIdElement = json.get("tokenId");
        if(tokenIdElement == null){
            throw new NullPointerException("tokenId is null");
        }
        JsonElement amountElement = json.get("amount");
        JsonElement indexElement = json.get("index");
        JsonElement nameElement = json.get("name");
        JsonElement decimalsElement = json.get("decimals");
        JsonElement typeElement = json.get("type");

        m_tokenId = tokenIdElement.getAsString();
        m_amount = amountElement != null ? amountElement.getAsLong() : 0;
        m_index = indexElement != null ? indexElement.getAsInt() : 0;
        m_name = nameElement != null && !nameElement.isJsonNull() ? nameElement.getAsString() : "";
        m_decimals = decimalsElement != null && !decimalsElement.isJsonNull() ? decimalsElement.getAsInt() : 0;
        m_type = typeElement != null && !typeElement.isJsonNull() ? typeElement.getAsString() : "";

    }
}
