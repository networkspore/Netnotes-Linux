package com.netnotes;

import org.ergoplatform.appkit.NetworkType;

public class ErgoAmount extends PriceAmount {
    private NetworkType m_networkType;
    public ErgoAmount(double amount, NetworkType networkType) {
        super(amount, new ErgoCurrency(networkType));
        m_networkType = networkType;
    }

    public ErgoAmount(long nanoErg, NetworkType networkType) {
        super(nanoErg, new ErgoCurrency(networkType));
        m_networkType = networkType;
    }

    public NetworkType getNetworkType(){
        return m_networkType;
    }
}
