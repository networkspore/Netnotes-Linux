package com.netnotes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.Future;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public class ErgoPoolBoxData{
    private String m_poolId = null;
    private ErgoBoxAsset m_lp = null;
    private PriceAmount m_assetX = null;
    private PriceAmount m_assetY = null;
    private int m_feeNum = -1;
    private BigDecimal m_price;
    private long m_timeStamp = 0;
    private int m_scale = 0;

    public ErgoPoolBoxData(JsonObject json, boolean isNative2Token, int scale, long timeStamp) throws NullPointerException{
        this(new ErgoBox(json), isNative2Token, scale, timeStamp);
    }
    
    public ErgoPoolBoxData(ErgoBox box, boolean isNative, int scale, long timeStamp) throws NullPointerException{
        m_scale = scale;

        ErgoBoxRegister r4Register = box.getAdditionalRegisters().get("R4");
        if(r4Register == null){
            throw new NullPointerException("R4 box is null");
        }

        
        if(box.getAssets() != null && box.getAssets().length > 2){
            m_poolId = box.getAssets()[0].getTokenId();
        }else{
            throw new NullPointerException("Box assets < 3");
        }

        m_timeStamp = timeStamp;

        if(isNative){
            if(box.getAssets().length == 3 ){
                m_lp = box.getAssets()[1];
                long amount = box.getValue();
                m_assetX = new ErgoAmount(amount, ErgoDex.NETWORK_TYPE);
                m_assetY = new PriceAmount(box.getAssets()[2], ErgoDex.NETWORK_TYPE.toString(), true);
            }else{
                throw new NullPointerException("Box asset length invalid for N2T");
            }
        }else{
            if(box.getAssets().length == 4){
                m_lp = box.getAssets()[1];
                m_assetX = new PriceAmount(box.getAssets()[2], ErgoDex.NETWORK_TYPE.toString(), true);
                m_assetY = new PriceAmount(box.getAssets()[3], ErgoDex.NETWORK_TYPE.toString(), true);
                
            }else{
                throw new NullPointerException("Box asset length invalid for T2T");
            }
        }

        JsonElement r4Element = r4Register.getRenderedValue();
        if(r4Element != null){
            m_feeNum = r4Element.getAsInt();
        }else{
            throw new NullPointerException("R4 register value is null");
        }
    

        BigDecimal bigX = m_assetX.getBigDecimalAmount();
        BigDecimal bigY = m_assetY.getBigDecimalAmount();



        m_price =  bigY.divide(bigX, scale, RoundingMode.HALF_UP);
    
    }

    public int getScale(){
        return m_scale;
    }

    public long getTimestamp(){
        return m_timeStamp;
    }

    public BigDecimal getPrice(){
        return m_price;
    }

    public ErgoDexPrice getErgoDexPrice(){
        return new ErgoDexPrice(m_price, m_timeStamp);
    }

    public String getPoolId() {
        return m_poolId;
    }

    public ErgoBoxAsset getLp() {
        return m_lp;
    }

    public PriceAmount getAssetX() {
        return m_assetX;
    }

    public PriceAmount getAssetY() {
        return m_assetY;
    }

    public int getFeeNum() {
        return m_feeNum;
    }

    public static Future<?> getPoolBoxDataByPoolId(NoteInterface ergoInterface, String poolId, String locationId, EventHandler<WorkerStateEvent> onComplete, EventHandler<WorkerStateEvent> onError){
        if(ergoInterface != null){
            JsonObject cmdObject = Utils.getCmdObject("getUnspentByTokenId");
            cmdObject.addProperty("tokenId", poolId);
            cmdObject.addProperty("locationId", locationId);
            cmdObject.addProperty("networkId", ErgoNetwork.EXPLORER_NETWORK);
            cmdObject.addProperty("offset", 0);
            cmdObject.addProperty("limit", 1);

            return ergoInterface.sendNote(cmdObject, onComplete, onError);
        }
        return null;
    }
}