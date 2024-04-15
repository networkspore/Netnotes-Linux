package com.netnotes;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

public class SpectrumPrice{
        private BigDecimal m_price;
        private long m_timeStamp;
        public SpectrumPrice(JsonObject json) throws Exception{
            JsonElement timestampElement = json != null && json.isJsonObject() ? json.get("timestamp") : null;
            JsonElement priceElement = json != null && json.isJsonObject() ? json.get("price") : null;

            m_price = priceElement != null && priceElement.isJsonPrimitive() ? priceElement.getAsBigDecimal() : null;
            m_timeStamp = timestampElement != null && timestampElement.isJsonPrimitive() ? timestampElement.getAsLong() : -1;

            if(m_price == null || m_timeStamp == -1){
                throw new Exception("Invalid Spectrum Price");
            }
        }

        public SpectrumPrice(BigDecimal price, long timeStamp){
            m_price = price;
            m_timeStamp = timeStamp;
        }

        public BigDecimal getPrice(){
            
            return m_price;
        }

        public long getTimeStamp(){
            return m_timeStamp;
        }

        public BigDecimal invertBigDecimal() throws ArithmeticException{
            return BigDecimal.ONE.divide(m_price, m_price.scale(), RoundingMode.CEILING);
        }

        public JsonObject getInvertedJson() throws ArithmeticException{
            JsonObject json = new JsonObject();
            json.addProperty("price", invertBigDecimal());
            json.addProperty("timestamp", m_timeStamp);
            return json;
        }
    }