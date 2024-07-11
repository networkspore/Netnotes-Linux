package com.netnotes;

import org.ergoplatform.appkit.NetworkType;

public class ErgoToken extends ErgoTokenData {

    public ErgoToken(ErgoTokensList tokensList){
        super(tokensList,97739925000000000L,"Layer 0 native currency.", ErgoCurrency.TOKEN_ID, ErgoCurrency.NAME, ErgoCurrency.SYMBOL, ErgoCurrency.FRACTIONAL_PRECISION,ErgoNetwork.NETWORK_ID, NetworkType.MAINNET.toString(), ErgoCurrency.IMAGE_STRING, ErgoCurrency.TOKEN_TYPE, ErgoCurrency.FONT_SYMBOL);
        explorerVerifiedProperty().set(true);
        
    }

    @Override
    public void updateTokenInfo(){
         
    }

    @Override
    public String getUrlString(){
        return ErgoCurrency.URL_STRING;
    }

}
