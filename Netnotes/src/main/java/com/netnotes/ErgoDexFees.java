package com.netnotes;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

public class ErgoDexFees {
    public static final BigDecimal DEFAULT_NETWORK_FEE = BigDecimal.valueOf(0.002);
    public static final BigDecimal MIN_NITRO = BigDecimal.valueOf(1.2);
    public static final BigDecimal DEFAULT_ERG_MIN_EX_FEE = DEFAULT_NETWORK_FEE.multiply(BigDecimal.valueOf(3));
    public static final BigDecimal DEFAULT_ERG_MAX_EX_FEE = DEFAULT_ERG_MIN_EX_FEE.multiply(MIN_NITRO).setScale(ErgoDex.POOL_FEE_MAX_DECIMALS, RoundingMode.FLOOR);

    private PriceQuote m_spfPriceQuote = null;

    private final SimpleBooleanProperty m_isSPF = new SimpleBooleanProperty(false);
    private final SimpleObjectProperty<BigDecimal> m_nitro = new SimpleObjectProperty<>(ErgoDex.DEFAULT_NITRO);
    private final SimpleObjectProperty<BigDecimal> m_ergoMaxExFee;
    private final SimpleObjectProperty<BigDecimal> m_spfMinExFee = new SimpleObjectProperty<>(null);
    private final SimpleObjectProperty<BigDecimal> m_spfMaxExFee = new SimpleObjectProperty<>(null);
    private final SimpleObjectProperty<BigDecimal> m_minExFeeProperty = new SimpleObjectProperty<>(null);
    private final SimpleObjectProperty<BigDecimal> m_maxExFeeProperty = new SimpleObjectProperty<>(null);
    private final SimpleObjectProperty<BigDecimal> m_networkFee = new SimpleObjectProperty<>(DEFAULT_NETWORK_FEE);

    public ErgoDexFees(){
        m_ergoMaxExFee = new SimpleObjectProperty<>(DEFAULT_ERG_MIN_EX_FEE);
        setNitro(MIN_NITRO);
        createBindings();
    }

    public ErgoDexFees(BigDecimal networkFee){
        m_networkFee.set(networkFee);
        m_ergoMaxExFee = new SimpleObjectProperty<>(DEFAULT_ERG_MAX_EX_FEE);
        
        setNitro(MIN_NITRO);
        createBindings();
    }

    public ErgoDexFees(JsonObject json){
        JsonElement networkMinFeeElement = json != null ? json.get("networkFee") : null;
        JsonElement nitroElement = json != null ? json.get("nitro") : null;
        JsonElement isSPFElement = json != null ? json.get("isSPF") : null;
        BigDecimal networkFee = networkMinFeeElement != null && !networkMinFeeElement.isJsonNull() ? networkMinFeeElement.getAsBigDecimal() : DEFAULT_NETWORK_FEE;
        BigDecimal nitro = nitroElement != null ? nitroElement.getAsBigDecimal() : ErgoDex.DEFAULT_NITRO;
        boolean isSPF = isSPFElement != null ? isSPFElement.getAsBoolean() : false;

        m_networkFee.set(networkFee);
        m_ergoMaxExFee = new SimpleObjectProperty<>(DEFAULT_ERG_MIN_EX_FEE.multiply(nitro).setScale(ErgoDex.POOL_FEE_MAX_DECIMALS, RoundingMode.FLOOR));
        
        setIsSPF(isSPF);
        setNitro(nitro);
        createBindings();
    }

    public void reset(){
        m_networkFee.set(DEFAULT_NETWORK_FEE);
        setNitro(MIN_NITRO);
        m_isSPF.set(false);
    }
    
    private void createBindings(){
        m_maxExFeeProperty.bind(Bindings.createObjectBinding(()->getMaxExFee(), m_isSPF, m_ergoMaxExFee, m_spfMaxExFee));
        m_minExFeeProperty.bind(Bindings.createObjectBinding(()->getMinExFee(), m_isSPF, m_spfMinExFee));
    }

    public BigDecimal calculateSpfMinExFee(){ 
        BigDecimal quoteAmount = m_spfPriceQuote != null ? m_spfPriceQuote.getQuote() : null;
        return quoteAmount != null ? DEFAULT_ERG_MIN_EX_FEE.multiply(quoteAmount) : null;
    }

    public static BigDecimal calculateSpfMaxExFee(BigDecimal minExFee, BigDecimal nitro){      
        nitro = nitro == null || nitro.compareTo(MIN_NITRO) == -1 ? MIN_NITRO : nitro;
        return minExFee != null ? minExFee.multiply(nitro) : null;
    }


    public PriceCurrency getFeeCurrency(){
        return isSPF() ? ErgoDex.SPF_CURRENCY : ErgoDex.ERGO_CURRENCY;
    }

    public boolean isSPF(){
        return m_isSPF.get();
    }

    public void setIsSPF(boolean value){
        if(value != isSPF()){
            m_isSPF.set(value);
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
        BigDecimal maxExFee = DEFAULT_ERG_MIN_EX_FEE.multiply(nitro);
        maxExFee = maxExFee.scale() > ErgoDex.POOL_FEE_MAX_DECIMALS ? maxExFee.setScale(ErgoDex.POOL_FEE_MAX_DECIMALS, RoundingMode.FLOOR) : maxExFee;
        m_ergoMaxExFee.set(maxExFee);
        m_nitro.set(nitro);
        updateSpfFees();
    }

    public void updateSpfFees(){
        BigDecimal minExFee = calculateSpfMinExFee();
        m_spfMinExFee.set(minExFee);
        BigDecimal maxExFee = calculateSpfMaxExFee(minExFee, getNitro());
        maxExFee = maxExFee != null && maxExFee.scale() > ErgoDex.POOL_FEE_MAX_DECIMALS ? maxExFee.setScale(ErgoDex.POOL_FEE_MAX_DECIMALS, RoundingMode.FLOOR) : maxExFee;
        m_spfMaxExFee.set(maxExFee);

    }
    public void setSpfQuote(PriceQuote quote){
        m_spfPriceQuote = quote;
        updateSpfFees();
    }

    public BigDecimal getNitro(){
        return m_nitro.get();
    }


    public ReadOnlyObjectProperty<BigDecimal> spfMinExFeeProperty(){
        return m_spfMinExFee;
    }

    public ReadOnlyObjectProperty<BigDecimal> spfMaxExFeeProperty(){
        return m_spfMaxExFee;
    }

    public BigDecimal getMinExFee(){
        return isSPF() ? m_spfMinExFee.get() : DEFAULT_ERG_MIN_EX_FEE;
    }

    public BigDecimal getMaxExFee(){
        return isSPF() ? m_spfMaxExFee.get() : m_ergoMaxExFee.get();
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

    public void setMaxExFee(BigDecimal maxExFee){
        if(isSPF()){
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
                BigDecimal newNitro = maxExFee.divide(DEFAULT_ERG_MIN_EX_FEE, ErgoDex.POOL_FEE_MAX_DECIMALS, RoundingMode.HALF_UP);
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
                    BigDecimal newNitro = maxExFee.divide(minSpfFee, ErgoDex.POOL_FEE_MAX_DECIMALS, RoundingMode.HALF_UP);
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
        json.addProperty("isSPF", isSPF());
        json.addProperty("networkFee", m_networkFee.get());
        json.addProperty("nitro", m_nitro.get());
        return json;
    }
}
