package com.netnotes;


import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;
import javafx.beans.property.SimpleObjectProperty;

public class PriceAmount  {
    private long m_timeout = AddressesData.QUOTE_TIMEOUT;

    private final SimpleObjectProperty<PriceQuote> m_priceQuoteProperty;
    private final SimpleObjectProperty<BigDecimal> m_amount = new SimpleObjectProperty<>();
    private final PriceCurrency m_currency;
    
    private long m_created = System.currentTimeMillis();
    private long m_timeStamp = 0;
    private String m_marketId = null;

    public PriceAmount(BigDecimal amount, PriceCurrency priceCurrency, SimpleObjectProperty<PriceQuote> priceQuoteProperty){
        m_currency = priceCurrency;
        m_priceQuoteProperty = priceQuoteProperty;
        m_amount.set(amount);
    }



    public PriceAmount(long amount, PriceCurrency currency) {
        this(BigDecimal.ZERO, currency, new SimpleObjectProperty<>());

        setLongAmount(amount);
    }

    public PriceAmount(BigDecimal amount, PriceCurrency currency){
        m_currency = currency;
        m_priceQuoteProperty = new SimpleObjectProperty<>(null);
        setBigDecimalAmount(amount);
       
    }

    public String getMarketId(){
        return m_marketId;
    }

    public void setMarketId(String networkId){
        m_marketId = networkId;
    }

    public PriceAmount(double amount, PriceCurrency currency) {
        m_currency = currency;
        m_priceQuoteProperty = new SimpleObjectProperty<>(null);
        setDoubleAmount(amount);
      
    }

    public PriceAmount(long amount, PriceCurrency currency, long timeStamp){
        m_currency = currency;
        m_priceQuoteProperty = new SimpleObjectProperty<>(null);
        setLongAmount(amount);
        m_timeStamp = timeStamp;
 
    }

    public PriceAmount(JsonObject json) throws Exception{
        
        m_priceQuoteProperty = new SimpleObjectProperty<>(null);
        
        JsonElement amountElement = json.get("amount");
        JsonElement timeStampElement = json.get("timeStamp");
        JsonElement currencyObjectElement = json.get("currency");

        if(amountElement == null || timeStampElement == null  || currencyObjectElement == null || (currencyObjectElement != null && !currencyObjectElement.isJsonObject())){
            try {
                Files.writeString(App.logFile.toPath(), "\npriceAmount: " + json.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {
                
            }
            throw new Exception("Null argument");
        }

        JsonObject currencyObject = currencyObjectElement.getAsJsonObject();
        PriceCurrency importCurrency = new PriceCurrency(currencyObject);


        m_currency = importCurrency;
        setLongAmount(amountElement.getAsLong());
        m_created = timeStampElement.getAsLong();
       
        
    }
    
   


    public long getTimeStamp(){
        return m_timeStamp;
    }

    public void setTimeStamp(long timestamp){
        m_timeStamp = timestamp;
        
    }
    public boolean getAmountValid(){
        return getAmountValid(System.currentTimeMillis());
    }
    
    public boolean getAmountValid(long timestamp){
        return  ((timestamp - m_timeStamp)) < m_timeout;
    }
    
    public SimpleObjectProperty<PriceQuote> priceQuoteProperty(){
        return m_priceQuoteProperty;
    }
      

    public BigDecimal getBigDecimalAmount(){
        return m_amount.get();
    }
    

    public void setBigDecimalAmount(BigDecimal amount) {
        m_amount.set(amount);
        m_timeStamp = System.currentTimeMillis();
    }

    public void setBigDecimalAmount(BigDecimal amount, long timestamp) {
        m_amount.set(amount);
        m_timeStamp = timestamp;
    }

    public void addBigDecimalAmount(BigDecimal amount){
        BigDecimal a = m_amount.get() != null ? m_amount.get() : BigDecimal.ZERO;
        m_amount.set(a.add(amount));
    }

    public String getTokenId(){
        return m_currency.getTokenId();
    }
    
    public SimpleObjectProperty<BigDecimal> amountProperty(){
        return m_amount;
    }

    public double getDoubleAmount() {
        if(m_amount.get() == null || m_currency == null){
            return 0;
        }
        return m_amount.get() != null ? m_amount.get().doubleValue() : 0;
    }


    public BigDecimal calculateLongToBigDecimal(long amount){
        BigDecimal a = m_amount.get() != null ? m_amount.get() : BigDecimal.ZERO;
        PriceCurrency c = m_currency != null ? m_currency : null;
       
        if(a == null || c == null){
            try {
                Files.writeString(App.logFile.toPath(), "calculatelong to big decimal error", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {

            }
            return BigDecimal.ZERO;
        }
       
        int decimals = c.getFractionalPrecision();
        BigDecimal bigAmount = BigDecimal.valueOf(amount);

        if(decimals != 0){
            BigDecimal pow = BigDecimal.valueOf(10).pow(decimals);
            return bigAmount.divide(pow);
        }else{
            return bigAmount;
        }
    }

    public void setLongAmount(long amount) {
        m_amount.set(calculateLongToBigDecimal(amount));
        m_timeStamp = System.currentTimeMillis();
    }

    public void addLongAmount(long amount){
        BigDecimal bigAmount = calculateLongToBigDecimal(amount);
        addBigDecimalAmount(bigAmount);
    }

    public long getLongAmount() {
        if(m_amount.get() == null || m_currency == null){
            return 0;
        }
        int decimals = m_currency.getFractionalPrecision();
        BigDecimal pow = BigDecimal.valueOf(10).pow(decimals);

        return m_amount.get().multiply(pow).longValue();
    }

    public void setDoubleAmount(double amount) {
        m_amount.set(BigDecimal.valueOf(amount));
    }

    public PriceCurrency getCurrency() {
        return m_currency;
    }




    public LocalDateTime getCreatedTime() {
        return Utils.milliToLocalTime(m_created);
    }

    public String getAmountString(){
        
        return m_amount.get() + "";
    }

    @Override
    public String toString() {
        

        return getAmountString() + m_currency != null ? " " + m_currency.getSymbol() : "";
    }

    public JsonObject getJsonObject(){
        long amount = getLongAmount();

        JsonObject json = new JsonObject();
        json.addProperty("amount", amount);
        json.addProperty("timeStamp", m_created);
        json.add("currency", m_currency == null ? null : m_currency.getJsonObject());
        return json;
    }


}
