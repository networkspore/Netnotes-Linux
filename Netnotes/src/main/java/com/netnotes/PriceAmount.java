package com.netnotes;


import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.ErgoToken;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;

public class PriceAmount  {
    private long m_timeout = AddressesData.QUOTE_TIMEOUT;


    private final SimpleObjectProperty<BigDecimal> m_amount = new SimpleObjectProperty<>();
    private final PriceCurrency m_currency;
    
    private final long m_created;
    private SimpleLongProperty m_timeStampProperty = new SimpleLongProperty(System.currentTimeMillis());

    private boolean m_readonly = false;

    public PriceAmount(BigDecimal amount, PriceCurrency priceCurrency){
        m_currency = priceCurrency;
        m_amount.set(amount);
        m_created = System.currentTimeMillis();
    }

    public PriceAmount(long amount, PriceCurrency currency) {
        m_currency = currency;

        BigDecimal bigDecimal = calculateLongToBigDecimal(amount);
        m_amount.set(bigDecimal);
        m_created = System.currentTimeMillis();
    }

    public PriceAmount(long amount, PriceCurrency currency, boolean readonly) {
        m_currency = currency;

        BigDecimal bigDecimal = calculateLongToBigDecimal(amount);
        m_amount.set(bigDecimal);
        m_created = System.currentTimeMillis();
        m_readonly = readonly;
    }



    public PriceAmount(ErgoBoxAsset asset, String networkType, boolean readonly){
        m_currency = new PriceCurrency(asset.getTokenId(), asset.getName(), asset.getDecimals(), asset.getType(), networkType);
        BigDecimal bigDecimal = calculateLongToBigDecimal(asset.getAmount());
        m_amount.set(bigDecimal);
        m_created = System.currentTimeMillis();
        m_readonly = readonly;
    }


    public PriceAmount(double amount, PriceCurrency currency) {
        m_currency = currency;
        setDoubleAmount(amount);
        m_created = System.currentTimeMillis();
    }

    public PriceAmount(long amount, PriceCurrency currency, long timeStamp){
        m_currency = currency;
        BigDecimal bigDecimal = calculateLongToBigDecimal(amount);
        m_amount.set(bigDecimal);
        m_timeStampProperty.set(timeStamp);
        m_created = timeStamp;
    }

    public PriceAmount(JsonObject json) throws Exception{
        
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
        JsonElement tokenIdElement = currencyObject.get("tokenId");
        JsonElement nameElement = currencyObject.get("name");
        JsonElement decimalsElement = currencyObject.get("decimals");
        JsonElement tokenTypeElement = currencyObject.get("tokenType");
        JsonElement networkTypeElement = currencyObject.get("networkType");

        String tokenId = tokenIdElement != null && tokenIdElement.isJsonPrimitive() ? tokenIdElement.getAsString() : null;
        int decimals = decimalsElement != null && decimalsElement.isJsonPrimitive() ? decimalsElement.getAsInt() : -1;

        if(tokenId == null || decimals == -1){
            throw new Exception("currency null");
        }
        String name = nameElement != null && nameElement.isJsonPrimitive() ? nameElement.getAsString() : "null"; 
        String tokenType = tokenTypeElement != null && tokenTypeElement.isJsonPrimitive() ? tokenTypeElement.getAsString() : null;
        String networkType = networkTypeElement != null && networkTypeElement.isJsonPrimitive() ? networkTypeElement.getAsString() : NetworkType.MAINNET.toString();
        PriceCurrency importCurrency = new PriceCurrency(tokenId,name,decimals,tokenType, networkType);


        m_currency = importCurrency;
        BigDecimal bigDecimal = calculateLongToBigDecimal(amountElement.getAsLong());
        m_amount.set(bigDecimal);
        m_created = timeStampElement.getAsLong();
       
        
    }
    

    public JsonObject getAmountObject(){
        JsonObject json = new JsonObject();
        json.addProperty("decimalAmount", getBigDecimalAmount());
        json.addProperty("longAmount", getLongAmount());
        json.addProperty("name", getCurrency().getName());
        json.addProperty("tokenId", getCurrency().getTokenId());
        json.addProperty("decimals", getCurrency().getDecimals());

        return json;
    }

    public ErgoToken getErgoToken(){
        return new ErgoToken(getTokenId(), getLongAmount());
    }

    public static PriceAmount getByAmountObject(JsonObject json, NetworkType networkType){

        JsonElement decimalElement = json.get("decimalAmount");
        JsonElement longElement = json.get("longAmount");
        JsonElement nameElement = json.get("name");
        JsonElement tokenIdElement = json.get("tokenId");
        JsonElement decimalsElement = json.get("decimals");

        BigDecimal decimalAmount = decimalElement != null ? decimalElement.getAsBigDecimal() : null;
        long longAmount = decimalAmount == null && longElement != null ? longElement.getAsLong() : -1;

        if(decimalAmount == null && longAmount == -1){
            return null;
        }

        String name = nameElement != null ? nameElement.getAsString() : null;
        String tokenId = tokenIdElement != null ? tokenIdElement.getAsString() : null;
        int decimals = decimalsElement != null ? decimalsElement.getAsInt() : -1;

        if(name == null || tokenId == null || decimals == -1){
            return null;
        }
        PriceCurrency currency = new PriceCurrency(tokenId, name, decimals, "simple", networkType.toString());

        return decimalAmount != null ? new PriceAmount(decimalAmount, currency) : new PriceAmount(longAmount, currency);
        
    }
   


    public long getTimeStamp(){
        return m_timeStampProperty.get();
    }

    public void setTimeStamp(long timestamp){
        m_timeStampProperty.set(timestamp);
        
    }

    public SimpleLongProperty timeStampProperty(){
        return m_timeStampProperty;
    }
    public boolean getAmountValid(){
        return getAmountValid(System.currentTimeMillis());
    }
    
    public boolean getAmountValid(long timestamp){
        return  ((timestamp - getTimeStamp())) < m_timeout;
    }


    public BigDecimal getBigDecimalAmount(){
        return m_amount.get();
    }

    /*public void setBigDecimalAmount(BigDecimal amount) {

        m_amount.set(amount);
        m_timeStampProperty.set(System.currentTimeMillis());
    } */

    public boolean isReadonly(){
        return m_readonly;
    }

    public void setBigDecimalAmount(BigDecimal amount, long timestamp) {
        if(!m_readonly){
            m_amount.set(amount);
            m_timeStampProperty.set(timestamp);
        }
    }

    public void addBigDecimalAmount(BigDecimal amount){
        if(!isReadonly()){
            BigDecimal a = m_amount.get() != null ? m_amount.get() : BigDecimal.ZERO;
            BigDecimal newVal = a.add(amount);

            m_amount.set(newVal);
        }
    }


    public PriceCurrency getCurrency(){
        return m_currency;
    }

    

    public String getTokenId(){
        return m_currency.getTokenId();
    }
    
    public ReadOnlyObjectProperty<BigDecimal> amountProperty(){
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
        PriceCurrency c = getCurrency() != null ? getCurrency() : null;
       
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
        if(!isReadonly()){
        setLongAmount(amount,  System.currentTimeMillis());
        }
    }

    public void setLongAmount(long longAmount, long timeStamp) {
        if(!isReadonly()){
            BigDecimal amount = calculateLongToBigDecimal(longAmount);


            m_amount.set(amount);
            m_timeStampProperty.set(timeStamp);
        }
    }

    public void addLongAmount(long longAmount){
        if(!isReadonly()){
            BigDecimal bigAmount = calculateLongToBigDecimal(longAmount);
            addBigDecimalAmount(bigAmount);
        }
    }

    public long getLongAmount() {
        if(m_amount.get() == null || m_currency == null){
            return 0;
        }
        int decimals = getCurrency().getFractionalPrecision();
        BigDecimal pow = BigDecimal.valueOf(10).pow(decimals);

        return m_amount.get().multiply(pow).longValue();
    }

    public void setDoubleAmount(double doubleAmount) {
        if(!isReadonly()){
            BigDecimal amount = BigDecimal.valueOf(doubleAmount);
            m_amount.set(amount);
        }
    }



    public LocalDateTime getCreatedTime() {
        return Utils.milliToLocalTime(m_created);
    }

    public String getAmountString(){
        BigDecimal amount = m_amount.get();
        return amount == null ? "0" : amount.toPlainString();
    }

    @Override
    public String toString() {
        

        return getAmountString() + (m_currency != null ? " " + getCurrency().getSymbol() : "");
    }

    public JsonObject getJsonObject(){
        long amount = getLongAmount();

        JsonObject json = new JsonObject();
        json.addProperty("amount", amount);
        json.addProperty("timeStamp", m_created);
        json.add("currency", m_currency == null ? null : getCurrency().getJsonObject());
        return json;
    }


}
