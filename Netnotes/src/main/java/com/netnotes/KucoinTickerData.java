package com.netnotes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class KucoinTickerData {

    private String m_symbol = null;
    private String m_symbolName = null;
    private double m_buy = Double.NaN;
    private double m_sell = Double.NaN;
    private double m_changeRate = Double.NaN;
    private double m_changePrice = Double.NaN;
    private double m_high = Double.NaN;
    private double m_low = Double.NaN;
    private double m_vol = Double.NaN;
    private double m_volValue = Double.NaN;
    private double m_last = Double.NaN;
    private double m_averagePrice = Double.NaN;
    private double m_takerFeeRate = Double.NaN;
    private double m_makerFeeRate = Double.NaN;
    private double m_takerCoefficient = Double.NaN;
    private double m_makerCoefficient = Double.NaN;

    private String m_buyString = "-.--";
    private String m_sellString = "-.--";
    private String m_changeRateString = "-.--";
    private String m_changePriceString = "-.--";
    private String m_highString = "-.--";
    private String m_lowString = "-.--";
    private String m_volString = "-.--";
    private String m_volValueString = "-.--";
    private String m_lastString = "-.--";
    private String m_averagePriceString = "-.--";

    public KucoinTickerData(String symbol, JsonObject tickerDataObject) {

        JsonElement symbolNameElement = tickerDataObject.get("symbolName");
        JsonElement buyElement = tickerDataObject.get("buy");
        JsonElement sellElement = tickerDataObject.get("sell");
        JsonElement changeRateElement = tickerDataObject.get("changeRate");
        JsonElement changePriceElement = tickerDataObject.get("changePrice");
        JsonElement highElement = tickerDataObject.get("high");
        JsonElement lowElement = tickerDataObject.get("low");
        JsonElement volElement = tickerDataObject.get("vol");
        JsonElement volValueElement = tickerDataObject.get("volValue");
        JsonElement lastElement = tickerDataObject.get("last");
        JsonElement averagePriceElement = tickerDataObject.get("averagePrice");
        JsonElement takerFeeRateElement = tickerDataObject.get("takerFeeRate");
        JsonElement makerFeeRateElement = tickerDataObject.get("makerFeeRate");
        JsonElement takerCoefficientElement = tickerDataObject.get("takerCoefficient");
        JsonElement makerCoefficientElement = tickerDataObject.get("makerCoefficient");

        m_symbol = symbol;
        m_symbolName = symbolNameElement != null && symbolNameElement.isJsonPrimitive() ? symbolNameElement.getAsString() : null;
        m_buy = buyElement != null && buyElement.isJsonPrimitive() ? buyElement.getAsDouble() : Double.NaN;
        m_sell = sellElement != null && sellElement.isJsonPrimitive() ? sellElement.getAsDouble() : Double.NaN;
        m_changeRate = changeRateElement != null && changeRateElement.isJsonPrimitive() ? changeRateElement.getAsDouble() : Double.NaN;
        m_changePrice = changePriceElement != null && changePriceElement.isJsonPrimitive() ? changePriceElement.getAsDouble() : Double.NaN;
        m_high = highElement != null && highElement.isJsonPrimitive() ? highElement.getAsDouble() : Double.NaN;
        m_low = lowElement != null && lowElement.isJsonPrimitive() ? lowElement.getAsDouble() : Double.NaN;
        m_vol = volElement != null && volElement.isJsonPrimitive() ? volElement.getAsDouble() : Double.NaN;
        m_volValue = volValueElement != null && volValueElement.isJsonPrimitive() ? volValueElement.getAsDouble() : Double.NaN;
        m_last = lastElement != null && lastElement.isJsonPrimitive() ? lastElement.getAsDouble() : Double.NaN;

        m_averagePrice = averagePriceElement != null && averagePriceElement.isJsonPrimitive() ? averagePriceElement.getAsDouble() : Double.NaN;
        m_takerFeeRate = takerFeeRateElement != null && takerFeeRateElement.isJsonPrimitive() ? takerFeeRateElement.getAsDouble() : Double.NaN;
        m_makerFeeRate = makerFeeRateElement != null && makerFeeRateElement.isJsonPrimitive() ? makerFeeRateElement.getAsDouble() : Double.NaN;
        m_takerCoefficient = takerCoefficientElement != null && takerCoefficientElement.isJsonPrimitive() ? takerCoefficientElement.getAsDouble() : Double.NaN;
        m_makerCoefficient = makerCoefficientElement != null && makerCoefficientElement.isJsonPrimitive() ? makerCoefficientElement.getAsDouble() : Double.NaN;

        m_buyString = buyElement != null && buyElement.isJsonPrimitive() ? buyElement.getAsString() : "-.--";
        m_sellString = sellElement != null && sellElement.isJsonPrimitive() ? sellElement.getAsString() : "-.--";
        m_changeRateString = changeRateElement != null && changeRateElement.isJsonPrimitive() ? changeRateElement.getAsString() : "-.--";
        m_changePriceString = changePriceElement != null && changePriceElement.isJsonPrimitive() ? changePriceElement.getAsString() : "-.--";
        m_highString = highElement != null && highElement.isJsonPrimitive() ? highElement.getAsString() : "-.--";
        m_lowString = lowElement != null && lowElement.isJsonPrimitive() ? lowElement.getAsString() : "-.--";
        m_volString = volElement != null && volElement.isJsonPrimitive() ? volElement.getAsString() : "-.--";
        m_volValueString = volValueElement != null && volValueElement.isJsonPrimitive() ? volValueElement.getAsString() : "-.--";
        m_lastString = lastElement != null && lastElement.isJsonPrimitive() ? lastElement.getAsString() : "-.--";
        m_averagePriceString = averagePriceElement != null && averagePriceElement.isJsonPrimitive() ? averagePriceElement.getAsString() : "-.--";

    }

    public String getSymbol() {
        return m_symbol;
    }

    public String getSymbolName() {
        return m_symbolName;
    }

    public double getBuy() {
        return m_buy;
    }

    public double getSell() {
        return m_sell;
    }

    public double getChangeRate() {
        return m_changeRate;
    }

    public double getChangePrice() {
        return m_changePrice;
    }

    public double getHigh() {
        return m_high;
    }

    public double getLow() {
        return m_low;
    }

    public double getVol() {
        return m_vol;
    }

    public double getVolValue() {
        return m_volValue;
    }

    public double getLast() {
        return m_last;
    }

    public double getAveragePrice() {
        return m_averagePrice;
    }

    public double getTakerFeeRate() {
        return m_takerFeeRate;
    }

    public double getMakerFeeRate() {
        return m_makerFeeRate;
    }

    public double getTakerCoefficient() {
        return m_takerCoefficient;
    }

    public double getMakerCoefficient() {
        return m_makerCoefficient;
    }

    public String getBuyString() {
        return m_buyString;
    }

    public String getSellString() {
        return m_sellString;
    }

    public String getChangeRateString() {
        return m_changeRateString;
    }

    public String getChangePriceString() {
        return m_changePriceString;
    }

    public String getHighString() {
        return m_highString;
    }

    public String getLowString() {
        return m_lowString;
    }

    public String getVolString() {
        return m_volString;
    }

    public String getVolValueString() {
        return m_volValueString;
    }

    public String getLastString() {
        return m_lastString;
    }

    public String getAveragePriceString() {
        return m_averagePriceString;
    }

    @Override
    public String toString() {
        return getSymbol() + " - " + getLastString();
    }
}
