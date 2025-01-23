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

    private final BigDecimal m_defaultNetworkFee;
    private final BigDecimal m_defaultErgMinExFee;
    private final BigDecimal m_deafultErgMaxExFee;

    private final SimpleObjectProperty<BigDecimal> m_networkFee = new SimpleObjectProperty<>();
        
    private final SimpleBooleanProperty m_isSPF = new SimpleBooleanProperty(false);
    private final SimpleObjectProperty<BigDecimal> m_nitro = new SimpleObjectProperty<>(ErgoDex.DEFAULT_NITRO);

    private final SimpleObjectProperty<BigDecimal> m_ergoMaxExFee;

    private final SimpleObjectProperty<BigDecimal> m_spfMinExFee = new SimpleObjectProperty<>(null);
    private final SimpleObjectProperty<BigDecimal> m_spfMaxExFee = new SimpleObjectProperty<>(null);
    private final SimpleObjectProperty<BigDecimal> m_minExFeeProperty = new SimpleObjectProperty<>(null);
    private final SimpleObjectProperty<BigDecimal> m_maxExFeeProperty = new SimpleObjectProperty<>(null);

    public ErgoDexFees(){
        
        m_defaultNetworkFee = DEFAULT_NETWORK_FEE;
        m_defaultErgMinExFee = m_defaultNetworkFee.multiply(BigDecimal.valueOf(3));
        m_deafultErgMaxExFee = m_defaultErgMinExFee.multiply(ErgoDex.DEFAULT_NITRO);
        m_ergoMaxExFee = new SimpleObjectProperty<>(m_deafultErgMaxExFee);
       
        createBindings();
    }

    public ErgoDexFees(BigDecimal networkFee){
        m_defaultNetworkFee = networkFee;
        m_defaultErgMinExFee = m_defaultNetworkFee.multiply(BigDecimal.valueOf(3));
        m_deafultErgMaxExFee = m_defaultErgMinExFee.multiply(ErgoDex.DEFAULT_NITRO);
        m_ergoMaxExFee = new SimpleObjectProperty<>(m_deafultErgMaxExFee);

        createBindings();
    }

    public ErgoDexFees(JsonObject json){
        JsonElement networkMinFeeElement = json != null ? json.get("networkFee") : null;
        JsonElement nitroElement = json != null ? json.get("nitro") : null;
        JsonElement isSPFElement = json != null ? json.get("isSPF") : null;

        BigDecimal nitro = nitroElement != null ? nitroElement.getAsBigDecimal() : ErgoDex.DEFAULT_NITRO;
        boolean isSPF = isSPFElement != null ? isSPFElement.getAsBoolean() : false;

        m_defaultNetworkFee = networkMinFeeElement != null ? networkMinFeeElement.getAsBigDecimal() : DEFAULT_NETWORK_FEE;
        m_defaultErgMinExFee = m_defaultNetworkFee.multiply(BigDecimal.valueOf(3));
        m_deafultErgMaxExFee = m_defaultErgMinExFee.multiply(ErgoDex.DEFAULT_NITRO);
        m_ergoMaxExFee = new SimpleObjectProperty<>(m_defaultErgMinExFee.multiply(nitro));
        setIsSPF(isSPF);
        createBindings();
    }

    public void reset(){
        m_networkFee.set(m_defaultNetworkFee);
        m_ergoMaxExFee.set(m_deafultErgMaxExFee);
        m_isSPF.set(false);
    }
    
    private void createBindings(){
        
        m_maxExFeeProperty.bind(Bindings.createObjectBinding(()->getMaxExFee(), m_isSPF, m_spfMaxExFee, m_ergoMaxExFee));
        m_minExFeeProperty.bind(Bindings.createObjectBinding(()->getMinExFee(), m_isSPF, m_spfMinExFee));
    }

    public static BigDecimal calculateSpfMinExFee(PriceQuote quote, BigDecimal defaultErgMinExFee){ 
        BigDecimal quoteAmount = quote != null ? quote.getQuote() : null;
        return quoteAmount != null ? defaultErgMinExFee.multiply(quoteAmount) : null;
    }

    public static BigDecimal calculateSpfMaxExFee(BigDecimal minExFee, BigDecimal nitro){        
        BigDecimal newMaxFee = minExFee != null && nitro != null && nitro.compareTo(ErgoDex.DEFAULT_NITRO) > -1 ? minExFee.multiply(nitro) : null;
        return newMaxFee != null ? newMaxFee : null;
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

    public BigDecimal getDefaultNetworkFee(){
        return m_defaultNetworkFee;
    }

    public BigDecimal getErgoMinExFee(){
        return m_defaultErgMinExFee;
    }

    public BigDecimal getDefaultErgoMaxExFee(){
        return m_deafultErgMaxExFee;
    }

    public BigDecimal ergoMaxExFee(){
        return m_nitro.get().multiply(m_defaultErgMinExFee);
    }
    
    public ReadOnlyObjectProperty<BigDecimal> nitroProperty(){
        return m_nitro;
    }

    public boolean setNitro(BigDecimal nitro){
        if(nitro.compareTo(MIN_NITRO) > -1){
            if(m_priceQuote != null && isSPF()){
                m_ergoMaxExFee.set(m_defaultErgMinExFee.multiply(nitro));
                m_nitro.set(nitro);
                return updateSpfFees();
            }else{
                m_ergoMaxExFee.set(m_defaultErgMinExFee.multiply(nitro));
                m_nitro.set(nitro);
                return true;
            }

        }
        return false;
    }

    private PriceQuote m_priceQuote = null;

    public boolean updateSpfFees(){
        if(m_priceQuote == null){
            return false;
        }
        updateSpfFees(m_priceQuote);
        return true;
    }
    public void updateSpfFees(PriceQuote quote){
        m_priceQuote = quote;

        BigDecimal minExFee = calculateSpfMinExFee(quote, m_defaultErgMinExFee);
        m_spfMinExFee.set(minExFee);
        m_spfMaxExFee.set(calculateSpfMaxExFee(minExFee, getNitro()));
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
        return isSPF() ? m_spfMinExFee.get() : m_defaultErgMinExFee;
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

    public void setNetworkFee(BigDecimal fee){
        if(fee != null && fee.compareTo(ErgoNetwork.MIN_NETWORK_FEE) > -1){
            m_networkFee.set(fee);
        }else{
            m_networkFee.set(DEFAULT_NETWORK_FEE);
        }
    }

    public void setMaxExFee(BigDecimal maxExFee){
        if(maxExFee == null){
            setNitro(MIN_NITRO);
        }else{
            BigDecimal minMaxFee = getMinExFee().multiply(MIN_NITRO);
            if(maxExFee.compareTo(minMaxFee) > -1){
                BigDecimal newNitro = minMaxFee.divide(maxExFee, SPFCurrency.FRACTIONAL_PRECISION, RoundingMode.FLOOR);
                setNitro(newNitro);
            }else{
                setNitro(MIN_NITRO);
            }
            
        }
    }

    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("isSPF", isSPF());
        json.addProperty("networkFee", m_networkFee.get());
        json.addProperty("nitro", m_nitro.get());
        return json;
    }
}
