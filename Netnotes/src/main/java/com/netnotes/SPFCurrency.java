package com.netnotes;

import org.ergoplatform.appkit.NetworkType;

public class SPFCurrency extends PriceCurrency {
    public final static String TOKEN_ID = "9a06d9e545a41fd51eeffc5e20d818073bf820c635e2a9d922269913e0de369d";
    public final static String NAME = "SPF";
    public final static String SYMBOL = "SPF";
    public final static String IMAGE_STRING = "/assets/spf.svg";
    public final static int DECIMALS = 6;
    public final static String FONT_SYMBOL  = "SPF";
    public final static String TOKEN_TYPE = "EIP-004";
    public final static String URL_STRING = "https://spectrum.fi/";
    public final static String BOX_ID = "2ec6d0fe8a8f20fedeb5a8279ead47ce6b89f4d43d1ce494a05165a22f7426de";
    

    public SPFCurrency(){
        super(TOKEN_ID, NAME, SYMBOL, DECIMALS, ErgoDex.NETWORK_TYPE.toString(), IMAGE_STRING, TOKEN_TYPE, FONT_SYMBOL);
        setEmissionAmount(1000000000000000L);
        setDescription("Official utility and governance token of the Spectrum Finance protocol");
        setUrl(URL_STRING);
    }

    public String getBoxId(){
        return BOX_ID;
    }

    public NetworkType getNetworkType(){
        return ErgoDex.NETWORK_TYPE;
    }
}
