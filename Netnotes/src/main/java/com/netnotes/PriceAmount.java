package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;

import org.apache.commons.math3.exception.NullArgumentException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

public class PriceAmount {

    private BigDecimal m_amount;
    private PriceCurrency m_currency;
    private long m_created = System.currentTimeMillis();
    private boolean m_valid = true;

    public PriceAmount(long amount, PriceCurrency currency) {
        m_currency = currency;
        setLongAmount(amount);
   
    }

    public PriceAmount(BigDecimal amount, PriceCurrency currency){
        m_currency = currency;
        setBigDecimalAmount(amount);
       

    }

 

    public PriceAmount(double amount, PriceCurrency currency) {
        
        m_currency = currency;
        setDoubleAmount(amount);
      
    }

    public PriceAmount(long amount, PriceCurrency currency, boolean amountValid){
        
        m_currency = currency;
        setLongAmount(amount);
        m_valid = amountValid;
  
    }

    public PriceAmount(JsonObject json) throws Exception{
        JsonElement amountElement = json.get("amount");
        JsonElement timeStampElement = json.get("timeStamp");
        JsonElement validElement = json.get("valid");
        JsonElement currencyObjectElement = json.get("currency");

        if(amountElement == null || timeStampElement == null || validElement == null || currencyObjectElement == null){
            throw new Exception("Null argument");
        }

        JsonObject currencyObject = currencyObjectElement.getAsJsonObject();
        m_currency = new PriceCurrency(currencyObject);
        setLongAmount(amountElement.getAsLong());
        m_created = timeStampElement.getAsLong() ;
        m_valid = validElement.getAsBoolean();
        

       
    }
  
    
  
      
    

    public void setBigDecimalAmount(BigDecimal amount) {
        m_amount = amount;
    }

    public void addBigDecimalAmount(BigDecimal amount){
        m_amount = m_amount.add(amount);
    }

    public String getTokenId(){
        return m_currency.getTokenId();
    }
    
    public BigDecimal getBigDecimalAmount(){
        return m_amount;
    }

    public double getDoubleAmount() {
        return m_amount.doubleValue();
    }


    public boolean getAmountValid(){
        return m_valid;
    }

    public void setAmoutValid(boolean valid){
        m_valid = valid;
    }

    public BigDecimal calculateLongToBigDecimal(long amount){
        int decimals = m_currency.getFractionalPrecision();
        BigDecimal bigAmount = BigDecimal.valueOf(amount);

        if(decimals != 0){
            BigDecimal pow = BigDecimal.valueOf(10).pow(decimals);
            return bigAmount.divide(pow);
        }else{
            return bigAmount;
        }
    }

    public void setLongAmount(long amount) {
        m_amount = calculateLongToBigDecimal(amount);
    }

    public void addLongAmount(long amount){
        BigDecimal bigAmount = calculateLongToBigDecimal(amount);
        m_amount = m_amount.add(bigAmount);
    }

    public long getLongAmount() {
        int decimals = m_currency.getFractionalPrecision();
        BigDecimal pow = BigDecimal.valueOf(10).pow(decimals);

        return m_amount.multiply(pow).longValue();
    }

    public void setDoubleAmount(double amount) {
        m_amount = BigDecimal.valueOf(amount);
    }

    public PriceCurrency getCurrency() {
        return m_currency;
    }

    public LocalDateTime getCreatedTime() {
        return Utils.milliToLocalTime(m_created);
    }

    public String getAmountString(){
        int precision = getCurrency().getFractionalPrecision();
        DecimalFormat df = new DecimalFormat("0");
        df.setMaximumFractionDigits(precision);
        
        
        String formatedDecimals = df.format(m_amount);

        return formatedDecimals;
    }

    @Override
    public String toString() {
        String formatedDecimals = getAmountString();
        
        String amount = m_valid ? formatedDecimals : "-";

        switch (getCurrency().getSymbol()) {
      
            case "USD":
                amount = "$" + amount;
                break;
            case "EUR":
                amount = "€‎" + amount;
                break;
            default:
                amount = amount + " " + getCurrency().getSymbol();
        }

        return amount;
    }

    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("amount", getLongAmount());
        json.addProperty("timeStamp", m_created);
        json.addProperty("valid", m_valid);
        json.add("currency", m_currency.getJsonObject());
        return json;
    }
}
