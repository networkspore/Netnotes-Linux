package com.netnotes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import javafx.beans.property.SimpleObjectProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import com.google.gson.JsonElement;

public class SpectrumMarketData extends PriceQuote {

    private String m_id;
    private String m_baseId; 
    private String m_baseSymbol;
    private String m_quoteId;
    private String m_quoteSymbol;
    private BigDecimal m_lastPrice = BigDecimal.ZERO;
    private BigDecimal m_invertedPrice = BigDecimal.ZERO;
    private BigDecimal m_baseVolume = BigDecimal.ZERO;
    private BigDecimal m_quoteVolume = BigDecimal.ZERO;

    private String m_poolId = null;
    private BigDecimal m_liquidityUSD = BigDecimal.ZERO;

    private boolean m_defaultInvert = false;

    

    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>(LocalDateTime.now());
    
    public SpectrumMarketData(JsonObject json) throws Exception{
        super(json.get("lastPrice").getAsString(), json.get("baseSymbol").getAsString(), json.get("quoteSymbol").getAsString(), System.currentTimeMillis());
        JsonElement baseIdElement = json.get("baseId");
        JsonElement baseSymbolElement = json.get("baseSymbol");
        JsonElement quoteIdElement = json.get("quoteId");
        JsonElement quoteSymbolElement = json.get("quoteSymbol");
        JsonElement lastPriceElement = json.get("lastPrice");
        JsonElement quoteVolumeElement = json.get("baseVolume");
        JsonElement baseVolumeElement = json.get("quoteVolume");
// idElement != null && idElement.isJsonPrimitive() &&
        if(
           
            baseIdElement != null && baseIdElement.isJsonPrimitive() &&
            baseSymbolElement != null && baseSymbolElement.isJsonPrimitive() &&
            quoteIdElement != null && quoteIdElement.isJsonPrimitive() &&
            quoteSymbolElement != null && quoteSymbolElement.isJsonPrimitive() &&
            lastPriceElement != null && lastPriceElement.isJsonPrimitive() &&
            baseVolumeElement != null &&  baseVolumeElement.isJsonObject() &&
            quoteVolumeElement != null && quoteVolumeElement.isJsonObject()
        ){
            m_lastPrice = lastPriceElement.getAsBigDecimal();
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
            m_id = quoteId + "_" + baseId;
            String quoteSymbol = quoteSymbolElement.getAsString();
            
            m_defaultInvert = quoteSymbol.equals("SigUSD");
            m_quoteId = baseId;
            m_quoteSymbol = baseSymbolElement.getAsString();
            m_baseId = quoteIdElement.getAsString();
            m_baseSymbol = quoteSymbol;
        
            m_quoteVolume = baseVolumeBigDecimal;
            m_baseVolume = quoteVolumeBigDecimal;

            try{
            
                
                m_invertedPrice = lastPriceElement.getAsBigDecimal();
                m_lastPrice = BigDecimal.ONE.divide(m_invertedPrice, m_invertedPrice.precision(), RoundingMode.CEILING);

                setPrices(m_lastPrice.toString(), m_baseSymbol,m_quoteSymbol, m_baseId, m_quoteId);
            }catch(ArithmeticException ae){
                m_invertedPrice = BigDecimal.ZERO;
                setPrices("0", m_baseSymbol, m_quoteSymbol, m_baseId, m_quoteId);
                // Files.writeString(logFile.toPath(), "\nspectrumMarketData (Arithmetic exception): " + ae.toString() + "|n" + json.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            //   throw new Exception("Divide by zero");
            }
            
            
          
        }else{
            throw new Exception("Missing expected arguments");
        }
        
    }

    public boolean getDefaultInvert(){
        return m_defaultInvert;
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
        boolean invert = getDefaultInvert();
        String price = invert ? getInvertedLastPrice().toString() : lastPriceString();
        return new PriceQuote(price, invert ? getQuoteSymbol() : getBaseSymbol(), invert ? getBaseSymbol() : getQuoteSymbol());
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
        
        return m_lastPrice;
    }

    public void setLastPrice(BigDecimal lastPrice){
        m_lastPrice = lastPrice;
    }


    public String getSymbol(){
        return m_baseSymbol + "-" + m_quoteSymbol;
    }

    public String lastPriceString(){
        return m_lastPrice.toString();
    }


    public String getId(){
        return m_id;
    }
    public String getBaseId(){
        return m_baseId;
    }
    public String getBaseSymbol(){
        return m_baseSymbol;
    }
    public String getQuoteId(){
        return m_quoteId;
    }
    public String getQuoteSymbol(){
        return m_quoteSymbol;
    }

    public BigDecimal getInvertedLastPrice(){
        return m_invertedPrice;
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
        json.addProperty("base_id", m_baseId);
        json.addProperty("base_symbol", m_baseSymbol);
        json.addProperty("quote_id", m_quoteId);
        json.addProperty("quote_symbol", m_quoteSymbol);
        json.addProperty("last_price", m_lastPrice);
        json.addProperty("base_volume", m_baseVolume);
        json.addProperty("quote_volume", m_quoteVolume);
        return json;
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
    public String getCurrentSymbol(boolean swapped){
        return swapped ? (getQuoteSymbol() + "-" + getBaseSymbol()) : getSymbol();
    }

    public void update(SpectrumMarketData updateData){
        m_lastPrice = updateData.getLastPrice();
        m_invertedPrice = updateData.getInvertedLastPrice();
        m_baseVolume = updateData.getBaseVolume();
        m_quoteVolume = updateData.getQuoteVolume();
        m_liquidityUSD = updateData.getLiquidityUSD();
        updateTimeStamp();
        m_lastUpdated.set(LocalDateTime.now());
    }

    public SimpleObjectProperty<LocalDateTime> getLastUpdated(){
        return m_lastUpdated;
    }
}
