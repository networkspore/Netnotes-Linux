package com.netnotes;

import java.math.BigDecimal;

import org.ergoplatform.appkit.NetworkType;

public class ErgoAmount extends PriceAmount {
    private NetworkType m_networkType;

    public ErgoAmount(String amount){
        this(new BigDecimal(amount));
    }

    public ErgoAmount(BigDecimal ergs){
        this(ergs, NetworkType.MAINNET);
    }
    public ErgoAmount(long nanoErgs){
        this(nanoErgs, NetworkType.MAINNET);
    }

    public ErgoAmount(long nanoErgs, boolean readonly){
        this(nanoErgs, NetworkType.MAINNET, readonly);
    }

    public ErgoAmount(double amount, NetworkType networkType) {
        super(amount, new ErgoCurrency(networkType));
        m_networkType = networkType;
    }

    public ErgoAmount(long nanoErg, NetworkType networkType) {
        super(nanoErg, new ErgoCurrency(networkType));
        m_networkType = networkType;
    }

    public ErgoAmount(long nanoErg, NetworkType networkType, boolean readonly) {
        super(nanoErg, new ErgoCurrency(networkType), readonly);
        m_networkType = networkType;
    }

    public ErgoAmount(String amount, NetworkType networkType){
        this(new BigDecimal(amount), networkType);
    }

    public ErgoAmount(BigDecimal ergs, NetworkType networkType){
        super(ergs, new ErgoCurrency(networkType));

        m_networkType = networkType;

    }

    public NetworkType getNetworkType(){
        return m_networkType;
    }
}
