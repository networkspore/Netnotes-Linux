package com.netnotes;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.google.gson.JsonObject;

public class SpectrumNumbers {

    private long m_longValue = Long.MIN_VALUE;
    private BigDecimal m_high = BigDecimal.ZERO;
    private BigDecimal m_low = BigDecimal.ZERO;
    private BigDecimal m_sum = BigDecimal.ZERO;
    private int m_count = 0;
    private int m_decimals = 0;

    public SpectrumNumbers(){}


    public SpectrumNumbers(long value) {
        m_longValue = value;
    }

    public void setLongValue(long value) {
        m_longValue = value;
    }

    public long getLongValue() {
        return m_longValue;
    }

    public BigDecimal getHigh(){
        return m_high;
    }

    public void setHigh(BigDecimal high){
        m_high = high;
    }

    public BigDecimal getLow(){
        return m_low;
    }

    public void setLow(BigDecimal low){
        m_low = low;
    }

    public BigDecimal getSum(){
        return m_sum;
    }

    public void setSum(BigDecimal sum){
        m_sum = sum;
    }

    public int getCount(){
        return m_count;
    }

    public void setCount(int count){
        m_count = count;
    }

    public int getDecimals(){
        return m_decimals;
    }

    public void setDecimals(int decimals){
        m_decimals = decimals;
    }

    public BigDecimal getAverage() {
        try{
            return m_count == 0 ? BigDecimal.ZERO : m_sum.divide(new BigDecimal(m_count), SpectrumChartView.DECIMAL_PRECISION, RoundingMode.HALF_UP);
        }catch(ArithmeticException e){
            return BigDecimal.ZERO;
        }
    }

    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("avg", getAverage());
        json.addProperty("count", getCount());
        json.addProperty("hight", getHigh());
        json.addProperty("low", getLow());
        json.addProperty("decimals", getDecimals());

        return json;

    }
}

