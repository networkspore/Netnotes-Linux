package com.netnotes;

import org.ergoplatform.appkit.NetworkType;

public class ErgoToken extends ErgoNetworkToken {
    public ErgoToken(String networkId, ErgoTokensList tokensList){
        super(tokensList,97739925000000000L,"Layer 0 native currency.", ErgoCurrency.TOKEN_ID, ErgoCurrency.NAME, ErgoCurrency.SYMBOL, ErgoCurrency.FRACTIONAL_PRECISION,networkId, NetworkType.MAINNET.toString(), ErgoCurrency.IMAGE_STRING, ErgoCurrency.TOKEN_TYPE, ErgoCurrency.FONT_SYMBOL);
        explorerVerifiedProperty().set(true);
        
    }

    @Override
    public void updateTokenInfo(){
         
    }

    @Override
    public PriceQuote getPriceQuote(){
        return getErgoTokensList().findPriceQuoteById(SpectrumFinance.SIGUSD_ID, SpectrumFinance.ERG_ID);
    }


}
