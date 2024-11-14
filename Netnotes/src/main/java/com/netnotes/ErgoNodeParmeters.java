package com.netnotes;

import com.google.gson.JsonObject;

import org.ergoplatform.appkit.BlockchainParameters;
import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonElement;

public class ErgoNodeParmeters{
    
    private long m_timeStamp;
    private byte m_blockVersion;
    private int m_dataInputCost; 
    private int m_inputCost;
    private int m_maxBlockCost;
    private int m_maxBlockSize;
    private int m_minValuePerByte;
    private NetworkType m_networkType;
    private int m_outputCost;
    private int m_storageFeeFactor;
    private int m_tokenAccessCost;
    private int m_blockHeaderHeight;
    private int m_networkHeight;

    public byte getBlockVersion() {
        return m_blockVersion;
    }

    public int getNetworkHeight(){
        return m_networkHeight;
    }

    public void setNetworkHeight(int height){
        m_networkHeight = height;
    }


    public long getTimeStamp() {
        return m_timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.m_timeStamp = timeStamp;
    }

    public void setBlockVersion(byte blockVersion) {
        this.m_blockVersion = blockVersion;
    }

    public int getDataInputCost() {
        return m_dataInputCost;
    }

    public void setDataInputCost(int dataInputCost) {
        this.m_dataInputCost = dataInputCost;
    }

    public int getInputCost() {
        return m_inputCost;
    }

    public void setInputCost(int inputCost) {
        this.m_inputCost = inputCost;
    }

    public int getMaxBlockCost() {
        return m_maxBlockCost;
    }

    public void setMaxBlockCost(int maxBlockCost) {
        this.m_maxBlockCost = maxBlockCost;
    }

    public int getMaxBlockSize() {
        return m_maxBlockSize;
    }

    public void setMaxBlockSize(int maxBlockSize) {
        this.m_maxBlockSize = maxBlockSize;
    }

    public int getMinValuePerByte() {
        return m_minValuePerByte;
    }

    public void setMinValuePerByte(int minValuePerByte) {
        this.m_minValuePerByte = minValuePerByte;
    }

    public NetworkType getNetworkType() {
        return m_networkType;
    }

    public void setNetworkType(NetworkType networkType) {
        this.m_networkType = networkType;
    }

    public int getOutputCost() {
        return m_outputCost;
    }

    public void setOutputCost(int outputCost) {
        this.m_outputCost = outputCost;
    }

    public int getStorageFeeFactor() {
        return m_storageFeeFactor;
    }

    public void setStorageFeeFactor(int storageFeeFactor) {
        this.m_storageFeeFactor = storageFeeFactor;
    }

    public int getTokenAccessCost() {
        return m_tokenAccessCost;
    }

    public void setTokenAccessCost(int tokenAccessCost) {
        this.m_tokenAccessCost = tokenAccessCost;
    }
    public int getBlockHeaderHeight(){
        return m_blockHeaderHeight;
    }

    public void setBlockHeaderHeight(int height){
        m_blockHeaderHeight = height;
    }

    public ErgoNodeParmeters(BlockchainParameters parameters, int blockHeight, long timeStamp, int networkHeight){
        m_networkHeight = networkHeight;
        m_timeStamp = timeStamp;
        m_blockHeaderHeight = blockHeight;

        m_blockVersion = parameters.getBlockVersion();
        m_dataInputCost= parameters.getDataInputCost();
        m_inputCost = parameters.getInputCost();
        m_maxBlockCost = parameters.getMaxBlockCost();
        m_maxBlockSize = parameters.getMaxBlockSize();
        m_minValuePerByte = parameters.getMinValuePerByte();
        m_networkType = parameters.getNetworkType();
        m_outputCost = parameters.getOutputCost();
        m_storageFeeFactor = parameters.getStorageFeeFactor();
        m_tokenAccessCost = parameters.getTokenAccessCost();
    }

    public ErgoNodeParmeters(JsonObject json){
    
        JsonElement networkTypeElement = json != null ? json.get("networkType"): null;
        JsonElement timeStampElement = json != null ? json.get("timeStamp") : null;
        JsonElement blockHeaderHeightElement = json != null ? json.get("blockHeaderHeight") : null;
        JsonElement networkHeightElement = json != null ? json.get("networkHeight") :null;
        JsonElement parametersElement = json != null ? json.get("parameters") : null;

        JsonObject paramJson = parametersElement != null && parametersElement.isJsonObject() ? parametersElement.getAsJsonObject() : null;

        JsonElement blockVersionElement = paramJson != null ? paramJson.get("blockVersion") : null;
        JsonElement dataInputCostElement = paramJson != null ? paramJson.get("dataInputCost") : null;
        JsonElement inputCostElement = paramJson != null ? paramJson.get("inputCost"): null;
        JsonElement maxBlockCostElement = paramJson != null ? paramJson.get("maxBlockCost"): null;
        JsonElement maxBlockSizeElement =paramJson != null ? paramJson.get("maxBlockSize"): null;
        JsonElement minValuePerByteElement = paramJson != null ? paramJson.get("minValuePerByte"): null;
        JsonElement outputCostElement =paramJson != null ? paramJson.get("outputCost") : null;
        JsonElement storageFeeFactorElement = paramJson != null ?paramJson.get("storageFeeFactor"): null; 
        JsonElement tokenAccessCostElement = paramJson != null ? paramJson.get("tokenAccessCost"): null;

        setNetworkHeight(networkHeightElement != null ? networkHeightElement.getAsInt() : -1);
        setBlockHeaderHeight(blockHeaderHeightElement != null ? blockHeaderHeightElement.getAsInt() : -1);
        setTimeStamp(timeStampElement != null ? timeStampElement.getAsLong() : -1);
        setBlockVersion(blockVersionElement != null ? blockVersionElement.getAsNumber().byteValue() : null);
        setDataInputCost(dataInputCostElement != null ? dataInputCostElement.getAsInt() : -1);
        setInputCost(inputCostElement != null ? inputCostElement.getAsInt() : -1);
        setMaxBlockCost(maxBlockCostElement != null ? maxBlockCostElement.getAsInt() : -1);
        setMaxBlockSize(maxBlockSizeElement != null ? maxBlockSizeElement.getAsInt() : -1);
        setMinValuePerByte(minValuePerByteElement != null ? minValuePerByteElement.getAsInt() : -1);
        setNetworkType(networkTypeElement != null ? NetworkType.fromValue(networkTypeElement.getAsString()) : null);
        setOutputCost(outputCostElement != null ? outputCostElement.getAsInt() : -1);
        setStorageFeeFactor(storageFeeFactorElement != null ? storageFeeFactorElement.getAsInt() : -1);
        setTokenAccessCost(tokenAccessCostElement != null ? tokenAccessCostElement.getAsInt() : -1);
    
    }

    public boolean getSynced(){
        return getBlockHeaderHeight() >= getNetworkHeight() && getNetworkHeight() != -1;
    }
  
    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("networkType", getNetworkType().verboseName);
        json.addProperty("blockHeaderHeight", getBlockHeaderHeight());
        json.addProperty("networkHeight", getNetworkHeight());
        json.addProperty("synced", getSynced());
        json.addProperty("timeStamp", getTimeStamp());

        JsonObject parametersJson = new JsonObject();
        parametersJson.addProperty("blockVersion", getBlockVersion());
        parametersJson.addProperty("dataInputCost", getDataInputCost());
        parametersJson.addProperty("inputCost", getInputCost());
        parametersJson.addProperty("maxBlockCost", getMaxBlockCost());
        parametersJson.addProperty("maxBlockSize", getMaxBlockSize());
        parametersJson.addProperty("minValuePerByte", getMinValuePerByte());
        parametersJson.addProperty("outputCost", getOutputCost());
        parametersJson.addProperty("storageFeeFactor", getStorageFeeFactor());
        parametersJson.addProperty("tokenAccessCost", getTokenAccessCost());

        json.add("parameters", parametersJson);
        
        return json;

    }

  
}


