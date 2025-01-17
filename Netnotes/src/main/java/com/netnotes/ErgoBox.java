package com.netnotes;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;


public class ErgoBox {
    private String m_boxId;
    private String m_transactionId;
    private String m_blockId;
    private long m_value;
    private int m_index;
    private long m_globalIndex;
    private int m_creationHeight;
    private int m_settlementHeight;
    private String m_ergoTree;
    private String m_ergoTreeConstants;
    private String m_ergoTreeScript;
    private String m_address;
    private ErgoBoxAsset[] m_assets;
    private HashMap<String, ErgoBoxRegister> m_additionalRegisters = new HashMap<>();;
    private boolean m_mainChain = true;

    public ErgoBox(JsonObject json) throws NullPointerException{
        JsonElement boxIdElement = json.get("boxId");
        if(boxIdElement == null || boxIdElement.isJsonNull()){
            throw new NullPointerException("boxId is null");
        }
        JsonElement transactionIdElement = json.get("transactionId");
        JsonElement blockIdElement = json.get("blockId");
        JsonElement valueElement = json.get("value");
        JsonElement indexElement = json.get("index");
        JsonElement globalIndexElement = json.get("globalIndex");
        JsonElement creationHeightElement = json.get("creationHeight");
        JsonElement settlementHeightElement = json.get("settlementHeight");
        JsonElement ergoTreeElement = json.get("ergoTree");
        JsonElement ergoTreeConstantsElement = json.get("ergoTreeConstants");
        JsonElement ergoTreeScriptElement = json.get("ergoTreeScript");
        JsonElement addressElement = json.get("address");
        JsonElement assetsElement = json.get("assets");
        JsonElement additionalRegistersElement = json.get("additionalRegisters");
        JsonElement mainChainElement = json.get("mainChain");
    


        m_boxId =  boxIdElement.getAsString();
        m_transactionId = transactionIdElement != null ? transactionIdElement.getAsString() : null;
        m_blockId = blockIdElement != null ? blockIdElement.getAsString() : null;
        m_value = valueElement != null ? valueElement.getAsLong() : -1;
        m_index = indexElement != null ? indexElement.getAsInt() : -1;
        m_globalIndex = globalIndexElement != null ? globalIndexElement.getAsLong() : -1;
        m_creationHeight = creationHeightElement != null ? creationHeightElement.getAsInt(): -1;
        m_settlementHeight = settlementHeightElement != null ? settlementHeightElement.getAsInt() : -1;
        m_ergoTree = ergoTreeElement != null ? ergoTreeElement.getAsString() : null;
        m_ergoTreeConstants = ergoTreeConstantsElement != null ? ergoTreeConstantsElement.getAsString() : null;
        m_ergoTreeScript = ergoTreeScriptElement != null ? ergoTreeScriptElement.getAsString() : null;
        m_address = addressElement != null ? addressElement.getAsString() : null;
        if(assetsElement != null && assetsElement.isJsonArray()){
            setAssets(assetsElement.getAsJsonArray());
        }

        if(additionalRegistersElement.isJsonObject()){
            setRegisters(additionalRegistersElement.getAsJsonObject());
        }
        m_mainChain = mainChainElement != null ? mainChainElement.getAsBoolean() : true;
    }

    public String getBoxId() {
        return m_boxId;
    }

    public String getTransactionId() {
        return m_transactionId;
    }

    public String getBlockId() {
        return m_blockId;
    }

    public long getValue() {
        return m_value;
    }

    public int getIndex() {
        return m_index;
    }

    public long getGlobalIndex() {
        return m_globalIndex;
    }

    public int getCreationHeight() {
        return m_creationHeight;
    }

    public int getSettlementHeight() {
        return m_settlementHeight;
    }

    public String getErgoTree() {
        return m_ergoTree;
    }

    public String getErgoTreeConstants() {
        return m_ergoTreeConstants;
    }

    public String getErgoTreeScript() {
        return m_ergoTreeScript;
    }

    public String getAddress() {
        return m_address;
    }

    public ErgoBoxAsset[] getAssets() {
        return m_assets;
    }

    public HashMap<String, ErgoBoxRegister> getAdditionalRegisters() {
        return m_additionalRegisters;
    }

    public boolean getMainChain(){
        return m_mainChain;
    }
  

    private void setRegisters(JsonObject json){
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String registerName = entry.getKey();
            JsonElement registerElement = entry.getValue();

            m_additionalRegisters.put(registerName, new ErgoBoxRegister(registerElement.getAsJsonObject()));
        }
    }

    private void setAssets(JsonArray jsonArray){
        int size = jsonArray.size();
        m_assets = new ErgoBoxAsset[size];

        for(int i = 0; i < size ; i++){

            JsonElement jsonElement = jsonArray.get(i);
            
            JsonObject json = !jsonElement.isJsonNull() && jsonElement.isJsonObject() ? jsonElement.getAsJsonObject() : null;

            m_assets[i] = json != null ? new ErgoBoxAsset(json) : null;
        }
    }
}
