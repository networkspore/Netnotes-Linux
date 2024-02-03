package com.netnotes;
import java.math.BigDecimal;
import java.math.RoundingMode;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SpectrumTickerData {
    private String m_tickerId;
    private BigDecimal m_lastPrice;
    private BigDecimal m_liquidityUSD;
    private String m_poolId;
    private String m_baseId;
    private String m_targetId;
   
    
    public SpectrumTickerData(JsonObject tickerDataJson){
        JsonElement tickerIdElement = tickerDataJson.get("ticker_id");
        JsonElement lastPriceElement = tickerDataJson.get("last_price");
        JsonElement liquidityUsdElement = tickerDataJson.get("liquidity_in_usd");
        JsonElement poolIdElement = tickerDataJson.get("pool_id");
        JsonElement baseIdElement = tickerDataJson.get("base_currency");
        JsonElement targetIdElement = tickerDataJson.get("target_currency");
        
        //"03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04"
        BigDecimal lastPrice = lastPriceElement.getAsBigDecimal();
        String baseId = baseIdElement.getAsString();
        String targetId = targetIdElement.getAsString();
        String poolId = poolIdElement.getAsString();
        String tickerId = tickerIdElement.getAsString();
        BigDecimal liquidityUSD = liquidityUsdElement.getAsBigDecimal();
       
      
        m_lastPrice = lastPrice;
        m_baseId = baseId;
        m_targetId = targetId;
        m_poolId = poolId;
        m_tickerId = tickerId;
        m_liquidityUSD = liquidityUSD;
    }
    public String getTickerId(){
        return m_tickerId;
    }
    public BigDecimal getLastPrice(){
        return m_lastPrice;
    }
    public BigDecimal getLiquidityUSD(){
        return m_liquidityUSD;
    }
    public String getPoolId(){
        return m_poolId;
    }
    public String getBaseId(){
        return m_baseId;
    }
    public String getTargetId(){
        return m_targetId;
    }

    public BigDecimal getInvertedPrice(){
        try{
            return m_lastPrice.equals(BigDecimal.ZERO) ? BigDecimal.ZERO : m_lastPrice.divide(BigDecimal.ONE, m_lastPrice.precision(), RoundingMode.HALF_UP);
        }catch(ArithmeticException e){
            return BigDecimal.ZERO;
        }

    }

    public void setTickerId (String tickerId){
        m_tickerId = tickerId;
    }
    public void setLastPrice(BigDecimal lastPrice){
        m_lastPrice = lastPrice;
    }
    public void setLiquidityUSD(BigDecimal liquidityUSD){
        m_liquidityUSD = liquidityUSD;
    }
    public void setPoolId(String poolId){
        m_poolId = poolId;
    }

    public void setBaseId(String baseId){
        m_baseId = baseId;
    }

    public void setTargetId(String targetId){
        m_targetId = targetId;
    }
 
}
