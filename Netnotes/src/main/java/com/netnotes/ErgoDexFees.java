package com.netnotes;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import com.google.gson.JsonObject;
import com.utils.Utils;
import com.google.gson.JsonElement;

public class ErgoDexFees {
    public static final int MAX_DECIMALS = 6;
    public static final BigDecimal DEFAULT_NETWORK_FEE = BigDecimal.valueOf(0.002);
    public static final BigDecimal MIN_NITRO = BigDecimal.valueOf(1.2);
    public static final BigDecimal DEFAULT_ERG_MIN_EX_FEE = DEFAULT_NETWORK_FEE.multiply(BigDecimal.valueOf(3));
    public static final BigDecimal DEFAULT_ERG_MAX_EX_FEE = DEFAULT_ERG_MIN_EX_FEE.multiply(MIN_NITRO);

    private PriceQuote m_spfPriceQuote = null;

    private final SimpleBooleanProperty m_isSPF = new SimpleBooleanProperty(false);
    private final SimpleObjectProperty<BigDecimal> m_nitro = new SimpleObjectProperty<>(ErgoDex.DEFAULT_NITRO);
    private final SimpleObjectProperty<BigDecimal> m_minExFeeProperty = new SimpleObjectProperty<>(null);
    private final SimpleObjectProperty<BigDecimal> m_maxExFeeProperty = new SimpleObjectProperty<>(null);
    private final SimpleObjectProperty<BigDecimal> m_networkFee = new SimpleObjectProperty<>(DEFAULT_NETWORK_FEE);

    public ErgoDexFees(){
        setNitro(MIN_NITRO);
    }

    public ErgoDexFees(BigDecimal networkFee){
        m_networkFee.set(networkFee);
        setNitro(MIN_NITRO);
    }

    public ErgoDexFees(JsonObject json){
        JsonElement networkMinFeeElement = json != null ? json.get("networkFee") : null;
        JsonElement nitroElement = json != null ? json.get("nitro") : null;
        JsonElement isSPFElement = json != null ? json.get("isSPF") : null;
        BigDecimal networkFee = networkMinFeeElement != null && !networkMinFeeElement.isJsonNull() ? networkMinFeeElement.getAsBigDecimal() : DEFAULT_NETWORK_FEE;
        BigDecimal nitro = nitroElement != null ? nitroElement.getAsBigDecimal() : ErgoDex.DEFAULT_NITRO;
        boolean isSPF = isSPFElement != null ? isSPFElement.getAsBoolean() : false;

        m_networkFee.set(networkFee);
        
        setIsSPF(isSPF);
        setNitro(nitro);
    }

    public void reset(){
        m_networkFee.set(DEFAULT_NETWORK_FEE);
        m_isSPF.set(false);
        setNitro(MIN_NITRO);
        
    }
    

    public BigDecimal calculateSpfMinExFee(){ 
        BigDecimal quoteAmount = m_spfPriceQuote != null ? m_spfPriceQuote.getQuote() : null;
        return quoteAmount != null ? DEFAULT_ERG_MIN_EX_FEE.multiply(quoteAmount) : null;
    }


    public PriceCurrency getFeeCurrency(){
        return isFeeSPF() ? ErgoDex.SPF_CURRENCY : ErgoDex.ERGO_CURRENCY;
    }

    public boolean isFeeSPF(){
        return m_isSPF.get();
    }

    public void setIsSPF(boolean value){
        if(value != isFeeSPF()){
            m_isSPF.set(value);
            updateFees();
        }
    }

    public ReadOnlyBooleanProperty isSPFProperty(){
        return m_isSPF;
    }


    public BigDecimal ergoMaxExFee(){
        return getNitro().multiply(DEFAULT_ERG_MIN_EX_FEE);
    }
    
    public ReadOnlyObjectProperty<BigDecimal> nitroProperty(){
        return m_nitro;
    }

    public void setNitro(BigDecimal nitro){
        nitro = nitro.compareTo(MIN_NITRO) == -1 ? MIN_NITRO : nitro;
        m_nitro.set(nitro);
        updateFees();
    }

    public void setSpfQuote(PriceQuote quote){
        m_spfPriceQuote = quote;
        if(isFeeSPF()){
            updateFees();
        }
    }

    public BigDecimal getNitro(){
        return m_nitro.get();
    }


    public ReadOnlyObjectProperty<BigDecimal> minExFeeProperty(){
        return m_minExFeeProperty;
    }

    public ReadOnlyObjectProperty<BigDecimal> maxExFeeProperty(){
        return m_maxExFeeProperty;
    }

    public ReadOnlyObjectProperty<BigDecimal> networkFeeProperty(){
        return m_networkFee;
    }

    public BigDecimal getNetworkFee(){
        return m_networkFee.get();
    }

    public static long getNanoErgsFromErgs(BigDecimal ergs){
        return PriceAmount.calculateBigDecimalToLong(ergs, ErgoCurrency.DECIMALS);
    }

    public void setNetworkFee(BigDecimal fee){
        if(fee != null && fee.compareTo(ErgoNetwork.MIN_NETWORK_FEE) > -1){
            m_networkFee.set(fee);
        }else{
            m_networkFee.set(DEFAULT_NETWORK_FEE);
        }
    }

    public static final BigInteger i64Max = BigInteger.valueOf(Long.MAX_VALUE);
    private BigDecimal m_exFeePerToken = null;
    private BigDecimal m_tmpExFeePerToken = null;
    private long m_exFeePerTokenNum = -1;
    private long m_exFeePerTokenDenom = -1;
    private SimpleObjectProperty<PriceAmount> m_minQuoteVolume = new SimpleObjectProperty<>(null);
   // private SimpleObjectProperty<PriceAmount> m_maxQuoteVolume = new SimpleObjectProperty<>(null);

    public ReadOnlyObjectProperty<PriceAmount> minVolumeProperty(){
        return m_minQuoteVolume;
    }

    /*public ReadOnlyObjectProperty<PriceAmount> maxQuoteVolumeProperty(){
        return m_maxQuoteVolume;
    }*/

    public BigDecimal getExFeePerToken(){
        return m_exFeePerToken;
    }

    public long getExFeePerTokenNum(){
        return m_exFeePerTokenNum;
    }

    public long getExFeePerTokenDenom(){
        return m_exFeePerTokenDenom;
    }

    public void setMinQuoteVolume(PriceAmount minvolume){
        m_minQuoteVolume.set(minvolume);
        updateFees();
    }


    public static BigInteger[] decimalToFractional(BigDecimal decimal){

        if(decimal == null || decimal.equals(BigDecimal.ZERO)){
            return new BigInteger[]{BigInteger.ZERO, BigInteger.ONE};
        }
        BigInteger leftSide = decimal.toBigInteger();
        int scale = decimal.scale();
        if(scale == 0){
            return new BigInteger[]{ leftSide, BigInteger.ONE};
        }
        
        BigInteger rightSide = decimal.remainder(BigDecimal.ONE).movePointRight(scale).abs().toBigInteger();

        //String rightSide = number.substring(number.indexOf(".") + 1);
       // int numDecimals = rightSide.length();

        //  new BigInteger(rightSide)
        BigInteger denominator = BigInteger.valueOf(10).pow(scale);
        BigInteger numerator =  leftSide.multiply(denominator).add(rightSide);
        return new BigInteger[]{numerator, denominator};
    }


    public void updateFees(){
        boolean isSPF =  isFeeSPF(); 

        BigDecimal minExFeeBigDecimal = isSPF ? calculateSpfMinExFee() : DEFAULT_ERG_MIN_EX_FEE;
        PriceAmount minQuotePriceAmount = m_minQuoteVolume.get();
        long minQuoteAmount = minQuotePriceAmount != null ? minQuotePriceAmount.getLongAmount() : 0;
        int decimals  = isSPF ? SPFCurrency.DECIMALS : ErgoCurrency.DECIMALS;
        int maxFeeDecimal = isSPF ? SPFCurrency.DECIMALS : 4;

        if (((isSPF && minQuotePriceAmount != null&& minExFeeBigDecimal != null) || !isSPF) && minQuoteAmount > 0 ) {
           // PriceCurrency volumeCurrency = minQuotePriceAmount.getCurrency();
            
            BigDecimal minOutput = BigDecimal.valueOf(minQuoteAmount);
            
            long minExFee = PriceAmount.calculateBigDecimalToLong(minExFeeBigDecimal, decimals);
            m_tmpExFeePerToken = BigDecimal.valueOf(minExFee).divide(minOutput,  20, RoundingMode.FLOOR).stripTrailingZeros();
            BigInteger[] fees = decimalToFractional(m_exFeePerToken);
            
            while (fees[0].compareTo(i64Max) == 1 || fees[1].compareTo(i64Max) == 1) {
                m_tmpExFeePerToken = m_tmpExFeePerToken.setScale(m_tmpExFeePerToken.scale() - 1, RoundingMode.FLOOR);
                fees = decimalToFractional(m_tmpExFeePerToken);
            }
            m_exFeePerToken = m_tmpExFeePerToken;
            m_exFeePerTokenNum = fees[0].longValue();
            m_exFeePerTokenDenom = fees[1].longValue();
           
            long adjustedMinExFee = m_exFeePerToken.multiply(minOutput).longValue();
            
      

            BigDecimal minExFeeDecimal = PriceAmount.calculateLongToBigDecimal(adjustedMinExFee, decimals);
            
            m_minExFeeProperty.set(minExFeeDecimal.setScale(maxFeeDecimal, RoundingMode.FLOOR).stripTrailingZeros());
            
            BigDecimal maxExFee = minExFeeBigDecimal.multiply(m_nitro.get()).setScale(maxFeeDecimal, RoundingMode.FLOOR).stripTrailingZeros();
  
            m_maxExFeeProperty.set(maxExFee);
           
         //   long maxOutput = BigDecimal.valueOf(PriceAmount.calculateBigDecimalToLong(maxExFee, decimals)).divide(m_exFeePerToken, 1, RoundingMode.FLOOR).longValue();
           // m_maxQuoteVolume.set(new PriceAmount(maxOutput, volumeCurrency));

        } else {
            m_exFeePerToken = null;
            m_exFeePerTokenNum = -1;
            m_exFeePerTokenDenom = -1;
            m_minExFeeProperty.set(minExFeeBigDecimal == null ? null : minExFeeBigDecimal);

            BigDecimal maxExFee = minExFeeBigDecimal != null ? minExFeeBigDecimal.multiply(m_nitro.get()) : null;
  
            maxExFee = maxExFee != null ? maxExFee.setScale(maxFeeDecimal, RoundingMode.FLOOR).stripTrailingZeros() : null;

            m_maxExFeeProperty.set(maxExFee);

           // m_maxQuoteVolume.set(minQuotePriceAmount != null ? new PriceAmount(0, minQuotePriceAmount.getCurrency())  : null);
        }

    }

    public void setMaxExFee(BigDecimal maxExFee){
        if(isFeeSPF()){
            setSpfMaxExFee(maxExFee);
        }else{
            setErgoMaxExFee(maxExFee);
        }
    }

    public void setErgoMaxExFee(BigDecimal maxExFee){
        if(maxExFee == null){
            setNitro(MIN_NITRO);
        }else{
         
            if(maxExFee.compareTo(DEFAULT_ERG_MAX_EX_FEE) == 1){
                BigDecimal newNitro = maxExFee.divide(DEFAULT_ERG_MIN_EX_FEE, 3, RoundingMode.HALF_UP).stripTrailingZeros();
                setNitro(newNitro);
            }else{
                setNitro(MIN_NITRO);
            }
        }
    }

    public void setSpfMaxExFee(BigDecimal maxExFee){
        if(maxExFee == null){
            setNitro(MIN_NITRO);
        }else{
            if(m_spfPriceQuote != null){
                BigDecimal minSpfFee = getMinSpfFee();
                BigDecimal defaultMaxSpfFee = minSpfFee.multiply(MIN_NITRO);

                if(minSpfFee != null && maxExFee.compareTo(defaultMaxSpfFee) == 1){
                    BigDecimal newNitro = maxExFee.divide(minSpfFee, MAX_DECIMALS, RoundingMode.HALF_UP);
                    setNitro(newNitro);
                }else{
                    setNitro(MIN_NITRO);
                }
            }else{
                setNitro(MIN_NITRO);
            }
        }
    }

    public BigDecimal getMinSpfFee(){
        return calculateSpfMinExFee();
    }

    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("isSPF", isFeeSPF());
        json.addProperty("networkFee", m_networkFee.get());
        json.addProperty("nitro", m_nitro.get());
        return json;
    }
}
