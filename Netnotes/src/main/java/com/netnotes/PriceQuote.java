package com.netnotes;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.beans.property.SimpleObjectProperty;

public class PriceQuote {

    private SimpleObjectProperty<BigDecimal> m_amount = new SimpleObjectProperty<>( BigDecimal.ZERO);
    private String m_baseId = "";
    private String m_transactionCurrency;
    private String m_quoteCurrency;
    private long m_timestamp = 0;
    private String m_quoteId = "";
    private String m_exchangeId = "";
    private boolean m_defaultInvert = false;

    public PriceQuote(){
        m_timestamp = System.currentTimeMillis();

    }

    public PriceQuote(String amountString, String transactionCurrency, String quoteCurrency) {

        setPrices(amountString, transactionCurrency, quoteCurrency);
    }

    public PriceQuote(String amountString, String transactionCurrency, String quoteCurrency, long timestamp) {
        m_timestamp = timestamp;
        
        setPrices(amountString, transactionCurrency, quoteCurrency);
    }

    private void setPrices(String amountString, String transactionCurrency, String quoteCurrency){
        setStringAmount(amountString);
        m_timestamp = System.currentTimeMillis();
        setAmountString(amountString);
        m_transactionCurrency = transactionCurrency;
        m_quoteCurrency = quoteCurrency;
        m_baseId = null;
        m_quoteId = null;
    }
    
    public void setPrices(BigDecimal amount, String transactionCurrency, String quoteCurrency, String baseId, String quoteId){
       
        m_timestamp = System.currentTimeMillis();
        BigDecimal amt = amount != null ? amount : BigDecimal.ZERO;
        setAmount(amt);
        m_transactionCurrency = transactionCurrency;
        m_quoteCurrency = quoteCurrency;
        m_baseId = baseId;
        m_quoteId = quoteId;

    }

    public void setExchangeId(String id){
        m_exchangeId = id;
    }

    public String getExchangeId(){
        return m_exchangeId;
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
        return m_baseId;
    }
    public String getQuoteCurrencyId(){
        return m_quoteId;
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
        BigDecimal amount = getBigDecimalAmount();
        
        if(amount.equals(BigDecimal.ZERO)){
            return amount;
        }

        try{
        
            return BigDecimal.ONE.divide(amount, amount.precision(), RoundingMode.HALF_UP);
        
        }catch(ArithmeticException ex){
            String msg = ex.toString();
            
        }
        
        return BigDecimal.ZERO;
    }

    public int getFractionalPrecision(){
        return getAmount().scale();
    }


    public String getAmountString() {
        return m_amount + "";
    }

    public void setAmountString(String amountString){
        setStringAmount(amountString);
    }

    public String getTransactionCurrency() {
        return m_transactionCurrency;
    }

    public void setTransactionCurrency(String transactionCurrency){
        m_transactionCurrency = transactionCurrency;
    }

    public String getQuoteCurrency() {
        return m_quoteCurrency;
    }

    public void setQuoteCurrency(String quoteCurrency){
        m_quoteCurrency = quoteCurrency;
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

    public JsonObject getJsonObject() {
        JsonObject priceQuoteObject = new JsonObject();
        priceQuoteObject.addProperty("transactionCurrency", m_transactionCurrency);
        priceQuoteObject.addProperty("quoteCurrency", m_quoteCurrency);
        priceQuoteObject.addProperty("amount", m_amount.get());
        priceQuoteObject.addProperty("timeStamp", m_timestamp);

        return priceQuoteObject;
    }
 
    public String toString(boolean invert) {
        return invert ? (getInvertedAmount() + " " +  m_quoteCurrency + "/" +m_transactionCurrency ) : toString();
    }


    @Override
    public String toString() {
        return m_amount + " " + m_transactionCurrency + "/" + m_quoteCurrency;
    }

    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>(LocalDateTime.now());
    
    public SimpleObjectProperty<LocalDateTime> getLastUpdated(){
        return m_lastUpdated;
    }
}
