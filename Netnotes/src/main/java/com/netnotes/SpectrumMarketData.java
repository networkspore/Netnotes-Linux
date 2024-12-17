package com.netnotes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.utils.Utils;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

import com.google.gson.JsonElement;

public class SpectrumMarketData extends PriceQuote {

    private String m_id;

  

    private BigDecimal m_baseVolume = BigDecimal.ZERO;
    private BigDecimal m_quoteVolume = BigDecimal.ZERO;
    private int m_baseDecimals;
    private int m_quoteDecimals;

    private long m_lastPoolData = 0;

    private String m_poolId = null;

    private BigDecimal m_liquidityUSD = BigDecimal.ZERO;


    private SimpleObjectProperty<PoolStats> m_poolStats = new SimpleObjectProperty<>(null);
    private SimpleObjectProperty<BigDecimal> m_poolSlippage = new SimpleObjectProperty<>(null);


    private SimpleObjectProperty<SpectrumChartView> m_chartViewProperty = new SimpleObjectProperty<>(null);

    public SpectrumMarketData(BigDecimal amount, String baseSymbol, String quoteSymbol, String baseId, String quoteId, String id, String poolId, long timeStamp){
        setAmount(amount);
        setTransactionCurrency(baseSymbol);
        setQuoteCurrency(quoteSymbol);
        setBaseId(baseId);
        setQuoteId(quoteId);
        setTimeStamp(timeStamp);
        m_id = id;
        m_poolId = poolId;
    }

    public SpectrumMarketData(JsonObject json, long timeStamp) throws Exception{
        super(json.get("lastPrice").getAsString(), json.get("baseSymbol").getAsString(), json.get("quoteSymbol").getAsString(), timeStamp);
       
        JsonElement baseIdElement =  json.get("baseId");
        JsonElement baseSymbolElement = json.get("baseSymbol");

        JsonElement quoteIdElement =json.get("quoteId");
        JsonElement quoteSymbolElement =  json.get("quoteSymbol");

        JsonElement lastPriceElement = json.get("lastPrice");

        JsonElement quoteVolumeElement = json.get("baseVolume");
        JsonElement baseVolumeElement = json.get("quoteVolume");

   
        if(
           
            baseIdElement != null && baseIdElement.isJsonPrimitive() &&
            baseSymbolElement != null && baseSymbolElement.isJsonPrimitive() &&
            quoteIdElement != null && quoteIdElement.isJsonPrimitive() &&
            quoteSymbolElement != null && quoteSymbolElement.isJsonPrimitive() &&
            lastPriceElement != null && lastPriceElement.isJsonPrimitive() &&
            baseVolumeElement != null &&  baseVolumeElement.isJsonObject() &&
            quoteVolumeElement != null && quoteVolumeElement.isJsonObject()
        ){
            
            JsonObject quoteVolumeObject = quoteVolumeElement.getAsJsonObject();
            long quoteVolumeValue = quoteVolumeObject.get("value").getAsLong();
            int quoteVolumeDecimals = quoteVolumeObject.get("units").getAsJsonObject().get("asset").getAsJsonObject().get("decimals").getAsInt();
            BigDecimal quoteVolumeBigDecimal = calculateLongToBigDecimal(quoteVolumeValue, quoteVolumeDecimals);
            

            JsonObject baseVolumeObject = baseVolumeElement.getAsJsonObject();
            long baseVolumeValue = baseVolumeObject.get("value").getAsLong();
            int baseVolumeDecimals = baseVolumeObject.get("units").getAsJsonObject().get("asset").getAsJsonObject().get("decimals").getAsInt();
            
            BigDecimal baseVolumeBigDecimal = calculateLongToBigDecimal(baseVolumeValue, baseVolumeDecimals);
           
           
            String baseId = baseIdElement.getAsString();
            String quoteId = quoteIdElement.getAsString();
            m_id = quoteId + "_" +baseId ;
            String quoteSymbol = quoteSymbolElement.getAsString();
            String baseSymbol = baseSymbolElement.getAsString();
            
            setDefaultInvert(!quoteSymbol.equals("SigUSD"));

            m_quoteVolume = quoteVolumeBigDecimal; 
            m_baseVolume = baseVolumeBigDecimal;

            m_baseDecimals = baseVolumeDecimals;
            m_quoteDecimals = quoteVolumeDecimals;

            

            setPrices(lastPriceElement.getAsBigDecimal(), baseSymbol,quoteSymbol, baseId, quoteId);
            
            

          
        }else{
            throw new Exception("Missing expected arguments");
        }
        
    }
    public boolean isPool(){
        return m_poolId != null;
    }
    
    public long getLastPoolData(){
        return m_lastPoolData;
    }

    public void setLastPoolData(long value){
        m_lastPoolData = value;
    }

    public SimpleObjectProperty<SpectrumChartView> getSpectrumChartView(){
        return m_chartViewProperty;
    }

    private SimpleLongProperty m_poolLastWritten = new SimpleLongProperty(0);

    public long getPoolLastWritten(){
        return m_poolLastWritten.get();
    }


    public void setPoolLastWritten(long value){
        m_poolLastWritten.set( value);
    }

    public SimpleLongProperty poolLastWrittenProperty(){
        return m_poolLastWritten;
    }

    public int getBaseDecimals(){
        return m_baseDecimals;
    }

    public int getQuoteDecimals(){
        return m_quoteDecimals;
    }

    public BigDecimal calculateLongToBigDecimal(long amount, int decimals){

        BigDecimal bigAmount = BigDecimal.valueOf(amount);

        if(decimals != 0){
            BigDecimal pow = BigDecimal.valueOf(10).pow(decimals);
            return bigAmount.divide(pow);
        }else{
            return bigAmount;
        }
    }

    public SpectrumMarketData clone(boolean invert){

        BigDecimal price = invert ? getInvertedAmount(): getAmount();
        
        String quoteSymbol = invert ?  getBaseSymbol() : getQuoteSymbol();
        String baseSymbol  = invert ? getQuoteSymbol() : getBaseSymbol();
        String quoteId = invert ? getBaseId() : getQuoteId();
        String baseId  = invert ? getQuoteId() : getBaseId();
        return new SpectrumMarketData(price,baseSymbol,quoteSymbol,baseId,quoteId, m_id, m_poolId, getTimeStamp());

    }
    

    public String getPoolId(){
        return m_poolId;
    }

    public void setPoolId(String poolId){
        m_poolId = poolId;
    }

    public BigDecimal getLiquidityUSD(){
        return m_liquidityUSD;
    }

    public void setLiquidityUSD(BigDecimal liqudityUSD){
        m_liquidityUSD = liqudityUSD;
    }

    public BigDecimal getLastPrice(){
        
        return getAmount();
    }

    public void setLastPrice(BigDecimal lastPrice){
        setAmount(lastPrice);
    }


    public String getSymbol(){
        return getBaseSymbol() + "-" + getQuoteSymbol();
    }

    public String lastPriceString(){
        return getAmount() + "";
    }


    public String getId(){
        return m_id;
    }

    public String getBaseId(){
        return getTransactionCurrencyId();
    }
    public String getBaseSymbol(){
        return getTransactionCurrency();
    }

    public String getQuoteId(){
        return getQuoteCurrencyId();
    }
    public String getQuoteSymbol(){
        return getQuoteCurrency();
    }

    public BigDecimal getInvertedLastPrice(){
        return getInvertedAmount();
    }
    public BigDecimal getBaseVolume(){
        return m_baseVolume;
    }
    public BigDecimal getQuoteVolume(){
        return m_quoteVolume;
    }
  
    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("id", m_id);
        json.addProperty("base_id", getBaseId());
        json.addProperty("base_symbol", getBaseSymbol());
        json.addProperty("quote_id", getQuoteId());
        json.addProperty("quote_symbol", getQuoteSymbol());
        json.addProperty("last_price", getAmount());
        json.addProperty("base_volume", getBaseVolume());
        json.addProperty("quote_volume", getQuoteVolume());
        return json;
    }

    public JsonObject getInvertedJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("id", m_id);
        json.addProperty("base_id",  getQuoteId());
        json.addProperty("base_symbol", getQuoteSymbol() );
        json.addProperty("quote_id",getBaseId());
        json.addProperty("quote_symbol",getBaseSymbol());
        json.addProperty("last_price", getInvertedAmount());
        json.addProperty("base_volume",  getQuoteVolume());
        json.addProperty("quote_volume",getBaseVolume());
        return json;
    }

     public void updatePoolStats(ExecutorService execService){
        SpectrumFinance.getPoolStats(getPoolId(), execService, (onSucceeded)->{
            Object sourceValue = onSucceeded.getSource().getValue();
            
            if(sourceValue != null && sourceValue instanceof JsonObject){
                
                try {
                    m_poolStats.set( new PoolStats((JsonObject) sourceValue));
                } catch (Exception e) {
                    m_poolStats.set(null);
                }

            }

        }, (onFailed)->{
            m_poolStats.set(null);
        });
    }

    public void updatePoolSlipage(ExecutorService execService){
        SpectrumFinance.getPoolSlippage(getPoolId(), execService, (onSucceeded)->{
        Object sourceValue = onSucceeded.getSource().getValue();
        
        if(sourceValue != null && sourceValue instanceof JsonObject){
            JsonObject slippageJson = (JsonObject) sourceValue;
            JsonElement slippageElement = slippageJson.get("slippagePercent");
            if(slippageElement != null && slippageElement.isJsonPrimitive()){
                
                m_poolSlippage.set(slippageElement.getAsBigDecimal());
            } else{
                m_poolSlippage.set(null);
            }

        }
        }, (onFailed)->{
            m_poolSlippage.set(null);
        });
    }

    public SimpleObjectProperty<BigDecimal> poolSlippageProperty(){
        return m_poolSlippage;
    }

    public SimpleObjectProperty<PoolStats> poolStatsProperty(){
        return m_poolStats;
    }

     public class SpectrumAsset{

        private String m_ticker;
        private String m_tokenId;
        private int m_decimals;

        public SpectrumAsset(JsonObject json) throws JsonParseException
        {
            JsonElement tickerElement = json.get("ticker");
            JsonElement tokenIdElement = json.get("tokenId");
            JsonElement decimalsElement = json.get("decimals");

            if(
                tickerElement != null && tickerElement.isJsonPrimitive() && 
                tokenIdElement != null && tokenIdElement.isJsonPrimitive() && 
                decimalsElement != null && decimalsElement.isJsonPrimitive()
            ){
                m_ticker = tickerElement.getAsString();
                m_tokenId = tokenIdElement.getAsString();
                m_decimals = decimalsElement.getAsInt();
            }else{
                throw new JsonParseException("Invalid asset arguments");
            }
        }

        public String getTicker(){
            return m_ticker;
        }    
        public String getTokenId(){
            return m_tokenId;
        }
        public int getDecimals(){
            return m_decimals;
        }

        public JsonObject getJsonObject(){
            JsonObject json = new JsonObject();
            json.addProperty("ticker", m_ticker);
            json.addProperty("tokenId", m_tokenId);
            json.addProperty("decimals", m_decimals);
            return json;
        }
    }

    public class SpectrumWindow{
        private String m_from = null;
        private String m_to = null;

        public SpectrumWindow(JsonObject json){
            JsonElement fromElement = json.get("from");
            JsonElement toElement = json.get("to");
            
            m_from = fromElement != null && fromElement.isJsonPrimitive() ? fromElement.getAsString() : null;
            m_to = toElement != null && toElement.isJsonPrimitive() ? toElement.getAsString() : null;
        }

        public String getFrom(){
            return m_from;
        }

        public String getTo(){
            return m_to;
        }

        public JsonObject getJsonObject(){
            JsonObject json = new JsonObject();
            if(m_from != null){
                json.addProperty("from", m_from);
            }
            if(m_to != null){
                json.addProperty("to", m_to);
            }
            return json;
        }
    }

    public String getCurrentSymbol(boolean swapped){
        return swapped ? (getQuoteSymbol() + "-" + getBaseSymbol()) : getSymbol();
    }

    public void update(SpectrumMarketData updateData){
        setAmount(updateData.getAmount());
       
        m_baseVolume = updateData.getBaseVolume();
        m_quoteVolume = updateData.getQuoteVolume();
        m_liquidityUSD = updateData.getLiquidityUSD();
        setTimeStamp(updateData.getTimeStamp());
        
        if(m_chartViewProperty.get() != null && m_chartViewProperty.get().getConnectionStatus() != App.STOPPED){
            m_chartViewProperty.get().update();
        }

        LocalDateTime now = LocalDateTime.now();
        getLastUpdated().set(now);
 
    }



    
    public class PoolStats{
        
        private String m_lockedYTokenId;
        private String m_lockedYSymbol;
        private long m_lockedYAmount;
        private int m_lockedYDecimals;

        private String m_lockedXTokenId;
        private String m_lockedXSymbol;
        private long m_lockedXAmount;
        private int m_lockedDecimals;

        private BigDecimal m_tvl;
        private BigDecimal m_volume;
        private BigDecimal m_fees;

        private long m_feesFrom;
        private long m_feesTo;
        private long m_volumeFrom;
        private long m_volumeTo;
        private BigDecimal m_yearlyFeesPercent;
        
        public PoolStats(JsonObject json) throws Exception{
            updatePoolStats(json);
        }

        public void updatePoolStats(JsonObject json) throws Exception{
            
            if(json == null){
                throw new Exception("No pool stats");
            }
            
            JsonElement lockedXElement = json.get("lockedX");
            JsonElement lockedYElement = json.get("lockedY");
            JsonElement tvlElement = json.get("tvl");
            JsonElement volumeElement = json.get("volume");
            JsonElement feesElement = json.get("fees");
            JsonElement yearlyFeesPercent = json.get("yearlyFeesPercent");

            if(lockedXElement != null && lockedXElement.isJsonObject() && lockedYElement != null && lockedYElement.isJsonObject() && tvlElement != null && volumeElement != null && feesElement != null){
                
                JsonObject lockedXObject = lockedXElement.getAsJsonObject();
                
                m_lockedXTokenId = lockedXObject.get("id").getAsString();
                m_lockedXSymbol = lockedXObject.get("ticker").getAsString();
                m_lockedXAmount = lockedXObject.get("amount").getAsLong();
                m_lockedDecimals = lockedXObject.get("decimals").getAsInt();
             
                JsonObject lockedYObject = lockedYElement.getAsJsonObject();

                m_lockedYTokenId = lockedYObject.get("id").getAsString();
                m_lockedYSymbol = lockedYObject.get("ticker").getAsString();
                m_lockedYAmount = lockedYObject.get("amount").getAsLong();
                m_lockedYDecimals = lockedYObject.get("decimals").getAsInt();

                JsonObject volumeObject = volumeElement.getAsJsonObject();
                m_volume = volumeObject.get("value").getAsBigDecimal();
                
                JsonObject volumeWindowObject = volumeObject.get("window").getAsJsonObject();
                m_volumeFrom = volumeWindowObject.get("from").getAsLong();
                m_volumeTo = volumeWindowObject.get("to").getAsLong();
                
                JsonObject feesObject = feesElement.getAsJsonObject();
                m_fees = feesObject.get("value").getAsBigDecimal();
                
                JsonObject feesWindowObject = feesObject.get("window").getAsJsonObject();
                m_feesFrom = feesWindowObject.get("from").getAsLong();
                m_feesTo = feesWindowObject.get("to").getAsLong();
                
                m_yearlyFeesPercent = yearlyFeesPercent.getAsBigDecimal();
               

            }else{
                throw new Exception("Invalid data");
            }
    
            
        }

     
        public String getLockedYTokenId(){
            return m_lockedYTokenId;
        }
        public String getLockedYSymbol(){
            return m_lockedYSymbol;
        }
        public long getLockedYAmount(){
            return m_lockedYAmount;
        }
        public int getLockedYDecimals(){
            return m_lockedYDecimals;
        }

        public String getLockedXTokenId(){
            return m_lockedXTokenId;
        }
        public String getLockedXSymbol(){
            return m_lockedXSymbol;
        }
        public long getLockedXAmount(){
            return m_lockedXAmount;
        }
        public int getLockedDecimals(){
            return m_lockedDecimals;
        }

        public BigDecimal getTvl(){
            return m_tvl;
        }
        
        public BigDecimal getVolume(){
            return m_volume;
        }
        
        public BigDecimal getFees(){
            return m_fees;
        }
        
        public long getFeesFrom(){
            return m_feesFrom;
        }
        
        public long getFeesTo(){
            return m_feesTo;
        }
        
        public long getVolumeFrom(){
            return m_volumeFrom;
        }
        
        public long getVolumeTo(){
            return m_volumeTo;
        }
        
        public BigDecimal getYearlyFeesPercent(){
            return m_yearlyFeesPercent;
        }

    }
    
    public class SpectrumVolumeData{
        private long m_value;
        private SpectrumAsset m_asset;
        private SpectrumWindow m_window;
 
        public SpectrumVolumeData(JsonObject json) throws JsonParseException{
            JsonElement valueElement = json.get("value");
            JsonElement unitsElement = json.get("units");
            JsonElement windowElement = json.get("window");
            
            if(unitsElement != null && unitsElement.isJsonObject()){
                m_value = valueElement != null && valueElement.isJsonPrimitive() ? valueElement.getAsLong() : -1;
                JsonObject unitsObject = unitsElement.getAsJsonObject();
                JsonElement assetElement = unitsObject.get("asset");
                if(assetElement != null && assetElement.isJsonObject()){
                    m_asset = new SpectrumAsset(assetElement.getAsJsonObject());
                }else{
                    throw new JsonParseException("Invalid volumeData asset");
                }
                if(windowElement != null && windowElement.isJsonObject()){
                    m_window = new SpectrumWindow(windowElement.getAsJsonObject());
                }
            }else{
                throw new JsonParseException("Invalid volumeData unit.");
            }
        }
        public long getValue(){
            return m_value;
        }
        public SpectrumAsset getAsset(){
            return m_asset;
        }
        public SpectrumWindow getWindow(){
            return m_window;
        }
    }
    
}
