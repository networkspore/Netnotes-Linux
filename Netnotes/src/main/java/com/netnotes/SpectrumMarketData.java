package com.netnotes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import com.google.gson.JsonElement;

public class SpectrumMarketData {

    private File logFile = new File("netnotes-log.txt");

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


    public SpectrumMarketData(JsonObject json) throws Exception{
     
        JsonElement baseIdElement = json.get("base_id");
        JsonElement baseSymbolElement = json.get("base_symbol");
        JsonElement quoteIdElement = json.get("quote_id");
        JsonElement quoteSymbolElement = json.get("quote_symbol");
        JsonElement lastPriceElement = json.get("last_price");
        JsonElement quoteVolumeElement = json.get("base_volume");
        JsonElement baseVolumeElement = json.get("quote_volume");

        if(
      
            baseIdElement != null && baseIdElement.isJsonPrimitive() &&
            baseSymbolElement != null && baseSymbolElement.isJsonPrimitive() &&
            quoteIdElement != null && quoteIdElement.isJsonPrimitive() &&
            quoteSymbolElement != null && quoteSymbolElement.isJsonPrimitive() &&
            lastPriceElement != null && lastPriceElement.isJsonPrimitive() &&
            baseVolumeElement != null &&  baseVolumeElement.isJsonPrimitive() &&
            quoteVolumeElement != null && quoteVolumeElement.isJsonPrimitive()
        ){
            
            m_baseId = baseIdElement.getAsString();
            m_baseSymbol = baseSymbolElement.getAsString();
            m_quoteId = quoteIdElement.getAsString();
            m_quoteSymbol = quoteSymbolElement.getAsString();

            m_id = m_quoteId + "_" + m_baseId;

            m_quoteVolume = quoteVolumeElement.getAsBigDecimal();
            m_baseVolume = baseVolumeElement.getAsBigDecimal();

            m_invertedPrice = lastPriceElement.getAsBigDecimal();
            
        }else{
            throw new Exception("Missing expected arguments");
        }
        
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
}
