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
    private BigDecimal m_open = BigDecimal.ZERO;
    private BigDecimal m_close = BigDecimal.ZERO;
    private boolean m_lastCloseDirection = false;
    private SpectrumPriceData[] m_priceData = null;
    
    private long m_lastTimeStamp;
    private int m_lastIndex = 0;

    public SpectrumNumbers(){}

    public int getLastIndex(){
        return m_lastIndex;
    }

    public void setLastIndex(int index){
        m_lastIndex = index;
    }

    public SpectrumNumbers invert(){
        SpectrumNumbers inverted = new SpectrumNumbers();
        
        inverted.setLongValue(m_longValue);
        inverted.setHigh(getHigh(true));
        inverted.setLow(getLow(true));
        inverted.setSum(getSum(true));
        inverted.setCount(m_count);
        inverted.setDecimals(m_decimals);
        inverted.setOpen(getOpen(true));
        inverted.setClose(getClose(true));

        return inverted;
    }

        
    public void updateNumbers(BigDecimal price){
        m_decimals = Math.max(price.scale(), m_decimals);

        setClose(price);
        if(m_open.equals(BigDecimal.ZERO)){
            setOpen(price);
        }

        if (m_low.equals(BigDecimal.ZERO)) {
            setLow(price);
        }

        setSum(m_sum.add(price));
    
        setHigh(price.max(m_high));

        setLow( price.min(m_low));
        
        setCount(getCount() + 1);
    }
    public int dataLength(){
        return m_priceData == null ? -1 : m_priceData.length;
    }
    
    public boolean getLastCloseDirection (){
        return m_lastCloseDirection;
    }

    public void setLastCloseDirection(boolean closeDirection){
        m_lastCloseDirection = closeDirection;
    }

    public void setLastTimeStamp(long timestamp){
        m_lastTimeStamp = timestamp;
    }

    public long getLastTimeStamp(){
        return m_lastTimeStamp;
    }

 


    public void updateData(SpectrumPriceData data){

        m_decimals = Math.max(m_decimals, data.getClose().scale());

        m_close = data.getClose();
        if(m_open.equals(BigDecimal.ZERO)){
            setOpen(data.getOpen());
        }

        if (m_low.equals(BigDecimal.ZERO)) {
            m_low = data.getLow();
        }

        setSum(m_sum.add(data.getClose()));
    
        setHigh(data.getHigh().max(m_high));

        setLow(data.getLow().min(m_low));
        
        setCount(m_count + data.getCount());


        m_lastTimeStamp = data.getLastTimeStamp();
    }

    public SpectrumPriceData[] getSpectrumPriceData(){
        return m_priceData;
    }

    public void setSpectrumPriceData(SpectrumPriceData[] data){
        m_priceData = data;
    }

    public SpectrumNumbers(long value) {
        m_longValue = value;
    }

    public void setLongValue(long value) {
        m_longValue = value;
    }

    public long getLongValue() {
        return m_longValue;
    }

    public BigDecimal getOpen(boolean invert){
        if(invert){
            try{
                return m_open.equals(BigDecimal.ZERO) ? BigDecimal.ZERO : BigDecimal.ONE.divide(m_open, m_open.precision(), RoundingMode.HALF_UP);
            }catch(ArithmeticException e){
                return BigDecimal.ZERO;
            }
        }else{
            return m_open;
        }
     
    }

    public BigDecimal getOpen(){
       
        return m_open;

    }

    public void setOpen(BigDecimal open){
       
        m_open = open;

    }

    public BigDecimal getClose(boolean invert){
        if(invert){
            try{
                return m_close.equals(BigDecimal.ZERO) ? BigDecimal.ZERO : BigDecimal.ONE.divide(m_close, m_close.precision(), RoundingMode.HALF_UP);
            }catch(ArithmeticException e){
                return BigDecimal.ZERO;
            }
        }else{
            return m_close;
        }

    }
    public BigDecimal getClose(){

        return m_close;
    }




    public void setClose(BigDecimal close){
        m_close = close;
    
    }


    public BigDecimal getHigh(boolean invert){
        if(invert){
            try{
                return m_low.equals(BigDecimal.ZERO) ? BigDecimal.ZERO : BigDecimal.ONE.divide(m_low, m_low.precision(), RoundingMode.HALF_UP);
            }catch(ArithmeticException e){
                return BigDecimal.ZERO;
            }
        }else{
            return m_high;
        }
        
    }
    public BigDecimal getHigh(){
     
        return m_high;
        
        
    }

    public void setHigh(BigDecimal high){
        m_high = high;
    }

    public BigDecimal getLow(boolean invert){
        if(invert){
            try{
                return m_high.equals(BigDecimal.ZERO) ? BigDecimal.ZERO : BigDecimal.ONE.divide(m_high, m_high.precision(), RoundingMode.HALF_UP);
            }catch(ArithmeticException e){
                return BigDecimal.ZERO;
            }
        }else{
            return m_low;
        }
    }

    public BigDecimal getLow(){
   
        return m_low;
        
    }

    public void setLow(BigDecimal low){
        m_low = low;
    }

    public BigDecimal getSum(boolean invert){
            
        if(invert){
            try{
                return m_sum.equals(BigDecimal.ZERO) ? BigDecimal.ZERO : BigDecimal.ONE.divide(m_sum, m_sum.precision(), RoundingMode.HALF_UP);
            }catch(ArithmeticException e){
                return BigDecimal.ZERO;
            }
        }else{ 
            return m_sum;
        }
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

    public BigDecimal getAverage(boolean invert) {
        if(invert){
            try{               
                return m_sum.equals(BigDecimal.ZERO) ? BigDecimal.ZERO : m_count == 0 ? BigDecimal.ZERO : getSum(invert).divide(new BigDecimal(m_count), m_sum.precision(), RoundingMode.HALF_UP);
            }catch(ArithmeticException e){
                return BigDecimal.ZERO;
            }
        }else{
            try{
                return m_count == 0 ? BigDecimal.ZERO : m_sum.divide(new BigDecimal(m_count), SpectrumChartView.DECIMAL_PRECISION, RoundingMode.HALF_UP);
            }catch(ArithmeticException e){
                return BigDecimal.ZERO;
            }
        }
        
    }

    public BigDecimal getAverage() {
        
        try{
            return m_count == 0 ? BigDecimal.ZERO : m_sum.divide(new BigDecimal(m_count), SpectrumChartView.DECIMAL_PRECISION, RoundingMode.HALF_UP);
        }catch(ArithmeticException e){
            return BigDecimal.ZERO;
        }
        
    }

    public BigDecimal getPercentIncrease(boolean invert){
        
        try{
            BigDecimal increase = getClose(invert).subtract(getOpen(invert));
        
            return increase.equals(BigDecimal.ZERO) ? BigDecimal.ZERO : increase.divide(getOpen(invert), m_open.scale(), RoundingMode.HALF_UP);
        }catch(ArithmeticException e){
            return BigDecimal.ZERO;
        }
        
    }
    public BigDecimal getPercentIncrease(BigDecimal open){
        
        try{
            BigDecimal increase = m_close.subtract(open);
            return  increase.equals(BigDecimal.ZERO) ? BigDecimal.ZERO :  increase.divide(m_open, m_open.precision(), RoundingMode.HALF_UP);
        }catch(ArithmeticException e){
            return BigDecimal.ZERO;
        }
        
    }

    public BigDecimal getPercentIncrease(){
        
        try{
            BigDecimal increase = m_close.subtract(m_open);
            return  increase.equals(BigDecimal.ZERO) ? BigDecimal.ZERO :  increase.divide(m_open, m_open.precision(), RoundingMode.HALF_UP);
        }catch(ArithmeticException e){
            return BigDecimal.ZERO;
        }
        
    }

    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("avg", getAverage(false));
        json.addProperty("count", getCount());
        json.addProperty("hight", getHigh(false));
        json.addProperty("low", getLow(false));
        json.addProperty("decimals", getDecimals());

        return json;

    }
}

