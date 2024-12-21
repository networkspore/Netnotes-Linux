package com.netnotes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

import com.google.gson.JsonElement;

public class ErgoDexMarketData extends PriceQuote {

 
    private BigDecimal m_baseVolume = BigDecimal.ZERO;
    private BigDecimal m_quoteVolume = BigDecimal.ZERO;

    private int m_baseDecimals;
    private int m_quoteDecimals;

    private long m_lastPoolData = 0;

    private String m_poolId = null;

    private BigDecimal m_liquidityUSD = BigDecimal.ZERO;
    private String m_tickerId = null;

    private SimpleObjectProperty<PoolStats> m_poolStats = new SimpleObjectProperty<>(null);
    private SimpleObjectProperty<BigDecimal> m_poolSlippage = new SimpleObjectProperty<>(null);


    private SimpleObjectProperty<ErgoDexChartView> m_chartViewProperty = new SimpleObjectProperty<>(null);

    public ErgoDexMarketData(BigDecimal amount, String baseSymbol, String quoteSymbol, String baseId, String quoteId, String id, String poolId, BigDecimal baseVolume, BigDecimal quoteVolume, int baseDecimals, int quoteDecimals, PoolStats poolStats, BigDecimal poolSlippage, long timeStamp){
        setAmount(amount);
        setTransactionCurrency(baseSymbol);
        setQuoteCurrency(quoteSymbol);
        setBaseId(baseId);
        setQuoteId(quoteId);
        setTimeStamp(timeStamp);
        setId(id);
        setBaseVolume(baseVolume);
        setQuoteVolume(quoteVolume);
        m_poolId = poolId;
        m_poolStats.set(poolStats);
        m_poolSlippage.set(poolSlippage);
    }

    public ErgoDexMarketData(JsonObject json, long timeStamp) throws Exception{
        super();
        JsonElement idElement = json.get("id");
        JsonElement lastPriceElement = json.get("lastPrice");
        JsonElement baseSymbolElement = json.get("baseSymbol"); 
        JsonElement quoteSymbolElement =  json.get("quoteSymbol");
        JsonElement baseIdElement = json.get("baseId");
        JsonElement quoteIdElement = json.get("quoteId");
        JsonElement quoteVolumeElement = json.get("baseVolume");
        JsonElement baseVolumeElement = json.get("quoteVolume");

        
        if(
            idElement != null &&
            lastPriceElement != null &&
            baseSymbolElement != null &&
            quoteSymbolElement != null &&
            baseIdElement != null &&
            quoteIdElement != null &&
            baseVolumeElement != null &&  baseVolumeElement.isJsonObject() &&
            quoteVolumeElement != null && quoteVolumeElement.isJsonObject()
        ){
            
            setId(idElement.getAsString());
            setLastPrice(lastPriceElement.getAsBigDecimal());
            setBaseSymbol(baseSymbolElement.getAsString());
            setQuoteSymbol(quoteSymbolElement.getAsString());
            setBaseId(baseIdElement.getAsString());
            setQuoteId(quoteIdElement.getAsString());
            setTimeStamp(timeStamp);
   
            JsonObject quoteVolumeObject = quoteVolumeElement.getAsJsonObject();
            long quoteVolumeValue = quoteVolumeObject.get("value").getAsLong();
            int quoteVolumeDecimals = quoteVolumeObject.get("units").getAsJsonObject().get("asset").getAsJsonObject().get("decimals").getAsInt();
            BigDecimal quoteVolumeBigDecimal = calculateLongToBigDecimal(quoteVolumeValue, quoteVolumeDecimals);
            

            JsonObject baseVolumeObject = baseVolumeElement.getAsJsonObject();
            long baseVolumeValue = baseVolumeObject.get("value").getAsLong();
            int baseVolumeDecimals = baseVolumeObject.get("units").getAsJsonObject().get("asset").getAsJsonObject().get("decimals").getAsInt();
            
            BigDecimal baseVolumeBigDecimal = calculateLongToBigDecimal(baseVolumeValue, baseVolumeDecimals);
          
            setDefaultInvert(!getQuoteId().equals(ErgoDex.SIGUSD_ID));

            setQuoteVolume( quoteVolumeBigDecimal); 
            setBaseVolume(baseVolumeBigDecimal);

            m_baseDecimals = baseVolumeDecimals;
            m_quoteDecimals = quoteVolumeDecimals;

            m_tickerId = getBaseId().equals(ErgoCurrency.TOKEN_ID) ? getQuoteId() + "_" + getBaseId() : getId();
            
        }else{
            throw new Exception("Missing expected arguments");
        }
        
    }

    public String getTickerId(){
        return m_tickerId;
    }

    @Override
    public JsonObject getJsonObject(){
        JsonObject json = super.getJsonObject();
        json.addProperty("baseVolume", getBaseVolume());
        json.addProperty("quoteVolume", getQuoteVolume());
        if(getPoolId() != null){
            json.addProperty("poolId", getPoolId());
        }
        return json;
    }

    @Override
    public JsonObject getInvertedJsonObject(){
        JsonObject json = super.getInvertedJsonObject();
        json.addProperty("baseVolume", getQuoteVolume());
        json.addProperty("quoteVolume", getBaseVolume());
        if(getPoolId() != null){
            json.addProperty("poolId", getPoolId());
        }
        return json;
    }

    public BigDecimal getBaseVolume(){
        return m_baseVolume;
    }
    public BigDecimal getQuoteVolume(){
        return m_quoteVolume;
    }


    public void setQuoteVolume(BigDecimal quoteVolume){
        m_quoteVolume = quoteVolume;
    }

    public void setBaseVolume(BigDecimal baseVolume){
        m_baseVolume = baseVolume;
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

    public SimpleObjectProperty<ErgoDexChartView> getSpectrumChartView(){
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



    public PriceQuote getPriceQuote(){
        return getPriceQuote(false);
    }

    public PriceQuote getPriceQuote(boolean invert){
        BigDecimal amount = invert ? getInvertedAmount(): getAmount();
        
        String quoteSymbol = invert ?  getBaseSymbol() : getQuoteSymbol();
        String baseSymbol  = invert ? getQuoteSymbol() : getBaseSymbol();
        String quoteId = invert ? getBaseId() : getQuoteId();
        String baseId  = invert ? getQuoteId() : getBaseId();

        PriceQuote priceQuote = new PriceQuote(getId(), amount, baseSymbol, quoteSymbol, baseId, quoteId, getTimeStamp());

        return priceQuote;
    }

    public ErgoDexMarketData clone(){
        return clone(false);
    }

    public ErgoDexMarketData clone(boolean invert){

        BigDecimal price = invert ? getInvertedAmount(): getAmount();
        
        String quoteSymbol = invert ?  getBaseSymbol() : getQuoteSymbol();
        String baseSymbol  = invert ? getQuoteSymbol() : getBaseSymbol();
        String quoteId = invert ? getBaseId() : getQuoteId();
        String baseId  = invert ? getQuoteId() : getBaseId();
        BigDecimal baseVolume =  invert ? getQuoteVolume() : getBaseVolume();
        BigDecimal quoteVolume = invert ? getBaseVolume() : getQuoteVolume();

        return new ErgoDexMarketData(price,baseSymbol,quoteSymbol,baseId,quoteId, getId(), m_poolId, baseVolume, quoteVolume,m_baseDecimals, m_quoteDecimals,m_poolStats.get(), m_poolSlippage.get(), getTimeStamp());

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


   

    public String lastPriceString(){
        return getAmount() + "";
    }



    public BigDecimal getInvertedLastPrice(){
        return getInvertedAmount();
    }



     public void updatePoolStats(ExecutorService execService){
        ErgoDex.getPoolStats(getPoolId(), execService, (onSucceeded)->{
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
        ErgoDex.getPoolSlippage(getPoolId(), execService, (onSucceeded)->{
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

    public void update(ErgoDexMarketData updateData){
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
        private BigDecimal m_volumeValue;
        private BigDecimal m_fees;

        private long m_feesFrom;
        private long m_feesTo;
        private long m_volumeFrom;
        private long m_volumeTo;
        private BigDecimal m_yearlyFeesPercent;
        private JsonObject m_poolStatsJson = null;
        
        public PoolStats(JsonObject json) throws Exception{
            updatePoolStats(json);
        }

        public JsonObject getJsonObject(){
            return m_poolStatsJson;
        }

        public void updatePoolStats(JsonObject json) throws Exception{
            m_poolStatsJson = json;
            if(json != null){ 
                JsonElement lockedXElement = json.get("lockedX");
                JsonElement lockedYElement = json.get("lockedY");
                JsonElement tvlElement = json.get("tvl");
                JsonElement volumeElement = json.get("volume");
                JsonElement feesElement = json.get("fees");
                JsonElement yearlyFeesPercentElement = json.get("yearlyFeesPercent");

                JsonObject lockedXObject = lockedXElement != null && lockedXElement.isJsonObject() ? lockedXElement.getAsJsonObject() : null;
                JsonElement lockedXTokenIdElement = lockedXObject != null ? lockedXObject.get("id") : null;
                JsonElement lockedXSymbolElement = lockedXObject != null ? lockedXObject.get("ticker") : null;
                JsonElement lockedXAmountElement = lockedXObject != null ? lockedXObject.get("amount") : null;
                JsonElement lockedDecimalsElement = lockedXObject != null ? lockedXObject.get("decimals") : null;

                JsonObject lockedYObject = lockedYElement != null && lockedYElement.isJsonObject() ? lockedYElement.getAsJsonObject() : null;
                JsonElement lockedYTokenIdElement = lockedYObject != null ? lockedYObject.get("id") : null;
                JsonElement lockedYSymbolElement = lockedYObject != null ? lockedYObject.get("ticker") : null;
                JsonElement lockedYAmountElement = lockedYObject != null ? lockedYObject.get("amount") : null;
                JsonElement lockedYDecimalsElement = lockedYObject != null ? lockedYObject.get("decimals") : null;

                JsonObject volumeObject =  volumeElement != null && volumeElement.isJsonObject() ? volumeElement.getAsJsonObject() : null;
                JsonElement volumeValueElement = volumeObject != null ? volumeObject.get("value") : null;

                JsonObject feesObject = feesElement != null && feesElement.isJsonObject() ? feesElement.getAsJsonObject() : null;
                JsonElement feesValueElement = feesObject != null ? feesObject.get("value") : null;
                JsonElement feesObjectWindowElement =  feesObject.get("window");

                JsonObject feesWindowObject = feesObjectWindowElement != null && feesObjectWindowElement.isJsonObject() ? feesObjectWindowElement.getAsJsonObject() : null;
                JsonElement feesFromElement = feesWindowObject != null ? feesWindowObject.get("from") : null;
                JsonElement feesToElement = feesWindowObject != null ? feesWindowObject.get("to") : null;


                JsonElement volumeWindowElement =  volumeObject != null ? volumeObject.get("window") : null;
                JsonObject volumeWindowObject = volumeWindowElement != null && volumeWindowElement.isJsonObject() ? volumeWindowElement.getAsJsonObject() : null;
                JsonElement volumeWindowFromElement = volumeWindowObject != null ? volumeWindowObject.get("from") : null;
                JsonElement volumeWindowToElement =volumeWindowObject != null ? volumeWindowObject.get("to") : null;
                JsonElement volumValueElement = volumeObject != null ? volumeObject.get("value") : null;


                if( 
                    tvlElement != null && 
                    lockedXTokenIdElement != null &&
                    lockedXSymbolElement != null &&
                    lockedXAmountElement != null &&
                    lockedDecimalsElement != null &&
                    lockedYTokenIdElement != null &&
                    lockedYSymbolElement != null &&
                    lockedYAmountElement != null &&
                    lockedYDecimalsElement != null &&
                    volumeValueElement != null &&
                    volumeWindowFromElement != null &&
                    volumeWindowToElement != null &&
                    feesValueElement != null
                ){
                    
                    m_lockedXTokenId = lockedXTokenIdElement.getAsString();
                    m_lockedXSymbol = lockedXSymbolElement.getAsString();
                    m_lockedXAmount = lockedXAmountElement.getAsLong();
                    m_lockedDecimals = lockedDecimalsElement.getAsInt();

                    m_lockedYTokenId = lockedYTokenIdElement.getAsString();
                    m_lockedYSymbol = lockedYSymbolElement.getAsString();
                    m_lockedYAmount = lockedYAmountElement.getAsLong();
                    m_lockedYDecimals = lockedYDecimalsElement.getAsInt();
  
                    m_volumeValue = volumValueElement.getAsBigDecimal();
                    
                    m_volumeFrom = volumeWindowFromElement.getAsLong();
                    m_volumeTo = volumeWindowToElement.getAsLong();
                    
                    m_fees = volumeValueElement.getAsBigDecimal();
                    
                    m_feesFrom = feesFromElement.getAsLong();
                    m_feesTo = feesToElement.getAsLong();
                    
                    m_yearlyFeesPercent = yearlyFeesPercentElement.getAsBigDecimal();
                }
    
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
            return m_volumeValue;
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
