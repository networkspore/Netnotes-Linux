package com.netnotes;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import com.utils.Utils;

import javafx.beans.property.SimpleObjectProperty;

public class PriceQuote {

    private SimpleObjectProperty<BigDecimal> m_amount = new SimpleObjectProperty<>( BigDecimal.ZERO);
    private String m_baseId = "";
    private String m_baseSymbol;
    private String m_quoteSymbol;
    private long m_timestamp = 0;
    private String m_quoteId = "";
    private String m_exchangeName = "";
    private boolean m_defaultInvert = false;
    private String m_id = "";


    public PriceQuote(){
        m_timestamp = System.currentTimeMillis();
    }

    public PriceQuote(JsonObject json){
        JsonElement idElement = json.get("id");
        JsonElement baseIdElement =json.get("baseId");
        JsonElement baseSymbolElement = json.get("baseSymbol");
        JsonElement quoteIdElement = json.get("quoteId");
        JsonElement quoteSymbolElement = json.get("quoteSymbol");
        JsonElement amountElement = json.get("amount");
        JsonElement timeStampElement = json.get("timeStamp");

        m_timestamp = timeStampElement != null && !timeStampElement.isJsonNull() ? timeStampElement.getAsLong() : System.currentTimeMillis();

        m_baseSymbol = baseSymbolElement != null && !baseSymbolElement.isJsonNull() ? baseSymbolElement.getAsString() : "null";
        m_quoteSymbol = quoteSymbolElement != null && !quoteSymbolElement.isJsonNull() ? quoteSymbolElement.getAsString() : "null";
        m_baseId = baseIdElement != null && !baseIdElement.isJsonNull() ? baseIdElement.getAsString() : m_baseSymbol;
        m_quoteId = quoteIdElement != null && !quoteIdElement.isJsonNull() ? quoteIdElement.getAsString() : m_quoteSymbol;
        m_id = idElement != null && !idElement.isJsonNull() ? idElement.getAsString() : m_baseId + "_" + m_quoteId;  
        BigDecimal amount = amountElement != null && !amountElement.isJsonNull() ? amountElement.getAsBigDecimal() : BigDecimal.ZERO;

        m_amount.set(amount);
    }



    public PriceQuote(String amountString, String transactionCurrency, String quoteCurrency) {

        setPrices(amountString, transactionCurrency, quoteCurrency);
    }

    public PriceQuote(String amountString, String transactionCurrency, String quoteCurrency, long timestamp) {
        m_timestamp = timestamp;
        
        setPrices(amountString, transactionCurrency, quoteCurrency);
    }

    public PriceQuote(BigDecimal amount, String transactionCurrency, String quoteCurrency,String baseId, String quoteId, long timestamp) {
        m_timestamp = timestamp;
        
        setPrices(amount, transactionCurrency, quoteCurrency, baseId, quoteId);
    }
    public PriceQuote(String id, BigDecimal amount, String transactionCurrency, String quoteCurrency,String baseId, String quoteId, long timestamp) {
        m_timestamp = timestamp;
        m_id = id;
        setPrices(amount, transactionCurrency, quoteCurrency, baseId, quoteId);
    }

    private void setPrices(String amountString, String transactionCurrency, String quoteCurrency){
        setStringAmount(amountString);
        m_timestamp = System.currentTimeMillis();
        setAmountString(amountString);
        m_baseSymbol = transactionCurrency;
        m_quoteSymbol = quoteCurrency;
        m_baseId = m_baseSymbol;
        m_quoteId = m_quoteSymbol;
    }
    
    public void setPrices(BigDecimal amount, String transactionCurrency, String quoteCurrency, String baseId, String quoteId){
       
        m_timestamp = System.currentTimeMillis();
        BigDecimal amt = amount != null ? amount : BigDecimal.ZERO;
        setAmount(amt);
        m_baseSymbol = transactionCurrency;
        m_quoteSymbol = quoteCurrency;
        m_baseId = baseId;
        m_quoteId = quoteId;

    }

    public String getSymbol(){
        return getBaseSymbol() + "-" + getQuoteSymbol();
    }

    public void setExchangeName(String name){
        m_exchangeName = name;
    }

    public String getExchangeName(){
        return m_exchangeName;
    }

    public void setStringAmount(String amountString) {
        setAmount(new BigDecimal(amountString));

    }

    public void setDoubleAmount(double amount) {
        setAmount( new BigDecimal(amount));

    }

    public void setAmount(BigDecimal amount){
        m_amount.set(amount);
    }

    protected void setDefaultInvert(boolean invert){
        m_defaultInvert = invert;
    }

    public boolean getDefaultInvert(){
        return m_defaultInvert;
    }

    public String getTransactionCurrencyId(){
        return m_baseId;
    }

    public void setBaseId(String baseId){
        m_baseId = baseId;
    }

    public String getBaseId(){

        return m_baseId != null && m_baseId != "" ?  m_baseId : getBaseSymbol();
    }
    public String getQuoteCurrencyId(){
        return m_quoteId;
    }

    public String getQuoteId(){
        return m_quoteId != null && m_quoteId != "" ?  m_quoteId : getQuoteSymbol();
    }

    public void setQuoteId(String quoteId){
        m_quoteId = quoteId;
    }

    public double getDoubleAmount() {
        return getAmount().doubleValue();
    }

    public BigDecimal getBigDecimalAmount(){
        return getAmount();
    }

    public BigDecimal getAmount(){
        return m_amount.get();
    }

    public SimpleObjectProperty<BigDecimal> amountProperty(){
        return m_amount;
    }

    public BigDecimal getInvertedAmount(){
        BigDecimal amount = m_amount.get();
        
        if(amount.equals(BigDecimal.ZERO)){
            return amount;
        }

        try{
        
            return BigDecimal.ONE.divide(amount, amount.precision(), RoundingMode.HALF_UP);
        
        }catch(ArithmeticException ex){
            //String msg = ex.toString();
            
        }
        
        return BigDecimal.ZERO;
    }

    public int getFractionalPrecision(){
        return getAmount().scale();
    }


    public String getAmountString() {
        return m_amount.get() + "";
    }

    public void setAmountString(String amountString){
        setStringAmount(amountString);
    }

    public String getTransactionCurrency() {
        return m_baseSymbol;
    }

    public String getBaseCurrency(){
        return m_baseSymbol;
    }

    public void setTransactionCurrency(String transactionCurrency){
        m_baseSymbol = transactionCurrency;
    }

    public String getQuoteCurrency() {
        return m_quoteSymbol;
    }

    public void setQuoteCurrency(String quoteCurrency){
        m_quoteSymbol = quoteCurrency;
    }

    public long howOldMillis() {
        return (Utils.getNowEpochMillis() - m_timestamp);
    }

    public void setTimeStamp(long timeStamp){
        m_timestamp = timeStamp;
    }
    public long getTimeStamp() {
        return m_timestamp;
    }
    
    public String getId(){
        return m_id;
    }

    public void setId(String id){
        m_id = id;
    }

    public String getBaseSymbol(){
        return m_baseSymbol;
    }

    public String getQuoteSymbol(){
        return m_quoteSymbol;
    }

 
    public void setBaseSymbol(String symbol){
        m_baseSymbol = symbol;
    }

    public void setQuoteSymbol(String symbol){
        m_quoteSymbol = symbol;
    }

    
    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("id", getId());
        json.addProperty("baseId", getBaseId());
        json.addProperty("baseSymbol", getBaseSymbol());
        json.addProperty("quoteId", getQuoteId());
        json.addProperty("quoteSymbol", getQuoteSymbol());
        json.addProperty("amount", getAmount());
        json.addProperty("timeStamp", getTimeStamp());
        return json;
    }

    public JsonObject getInvertedJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("id", getId());
        json.addProperty("baseId",  getQuoteId());
        json.addProperty("baseSymbol", getQuoteSymbol() );
        json.addProperty("quoteId",getBaseId());
        json.addProperty("quoteSymbol",getBaseSymbol());
        json.addProperty("amount", getInvertedAmount());
        json.addProperty("timeStamp", getTimeStamp());
        return json;
    }

 
    public String toString(boolean invert) {
        return invert ? (getInvertedAmount() + " " +  m_quoteSymbol + "/" +m_baseSymbol ) : toString();
    }


    @Override
    public String toString() {
        return m_amount + " " + m_baseSymbol + "/" + m_quoteSymbol;
    }

    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>(LocalDateTime.now());
    
    public SimpleObjectProperty<LocalDateTime> getLastUpdated(){
        return m_lastUpdated;
    }
}
