package com.netnotes;

import org.ergoplatform.appkit.NetworkType;

public class ErgoCurrency extends PriceCurrency {

    public final static String TOKEN_ID = SpectrumFinance.ERG_ID;
    public final static String NAME = "Ergo";
    public final static String SYMBOL = "ERG";
    public final static String IMAGE_STRING = "/assets/unitErgo.png";
    public final static int FRACTIONAL_PRECISION = 9;
    public final static String NETWORK_ID = ErgoNetwork.NETWORK_ID;
    public final static String FONT_SYMBOL  = "Σ";
    public final static String TOKEN_TYPE = "LAYER_0";
    
    private NetworkType m_networkType; 

    public ErgoCurrency(NetworkType networkType) { 
        super(TOKEN_ID, NAME, SYMBOL, FRACTIONAL_PRECISION, NETWORK_ID,networkType.toString(), IMAGE_STRING, TOKEN_TYPE, FONT_SYMBOL);
        setEmissionAmount(97739925000000000L);
        setDescription("Layer 0 native currency.");
        m_networkType = networkType;
    }

    public NetworkType getErgoNetworkType(){
        return m_networkType;
    }

}
