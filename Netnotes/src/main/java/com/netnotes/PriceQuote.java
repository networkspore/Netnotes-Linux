package com.netnotes;


import java.math.BigDecimal;
import com.google.gson.JsonObject;
import com.utils.Utils;

public class PriceQuote {

    private String m_amountString;
    private String m_transactionCurrencyId = "";
    private String m_transactionCurrency;
    private String m_quoteCurrency;
    private long m_timestamp = 0;
    private long m_precisionLong;
    private String m_quoteCurrencyId = "";

    private int m_fractionalPrecision = 0;



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
        m_amountString = amountString;
        m_transactionCurrency = transactionCurrency;
        m_quoteCurrency = quoteCurrency;
        m_transactionCurrencyId = null;
        m_quoteCurrencyId = null;
    }
    
    public void setPrices(String amountString, String transactionCurrency, String quoteCurrency, String txId, String quoteId){
        setStringAmount(amountString);
        m_timestamp = System.currentTimeMillis();
        m_amountString = amountString;
        m_transactionCurrency = transactionCurrency;
        m_quoteCurrency = quoteCurrency;
        m_transactionCurrencyId = txId;
        m_quoteCurrencyId = quoteId;
    }

    public void setStringAmount(String amountString) {
        m_amountString = amountString;
        int indexOfDecimal = amountString.indexOf(".");
        m_fractionalPrecision = indexOfDecimal == -1 ? 0 : amountString.substring(indexOfDecimal + 1).length();

        double amountDouble = Double.parseDouble(amountString);

        double precision = Math.pow(10, m_fractionalPrecision);
        m_precisionLong = (long) (precision * amountDouble);
    }

    public void setDoubleAmount(double amount) {
        m_amountString = String.format("%." + m_fractionalPrecision + "f", amount);
        double precision = Math.pow(10, m_fractionalPrecision);
        m_precisionLong = (long) (precision * amount);
    }

    public String getTransactionCurrencyId(){
        return m_transactionCurrencyId;
    }

    public String getQuoteCurrencyId(){
        return m_quoteCurrencyId;
    }

    public double getDoubleAmount() {
        return java.lang.Double.parseDouble(m_amountString);
    }

    public BigDecimal getBigDecimalAmount(){
        return new BigDecimal(m_amountString);
    }

    public int getFractionalPrecision(){
        return m_fractionalPrecision;
    }


    public String getAmountString() {
        return m_amountString;
    }

    public void setAmountString(String amountString){
        m_amountString = amountString;
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
        priceQuoteObject.addProperty("precisionLong", m_precisionLong);
        priceQuoteObject.addProperty("fractionalPrecision", m_fractionalPrecision);
        priceQuoteObject.addProperty("amount", m_amountString);
        priceQuoteObject.addProperty("timeStamp", m_timestamp);

        return priceQuoteObject;
    }

    @Override
    public String toString() {
        return m_amountString + " " + m_transactionCurrency + "/" + m_quoteCurrency;
    }
}
