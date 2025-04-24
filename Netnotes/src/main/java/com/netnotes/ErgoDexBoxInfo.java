package com.netnotes;

import io.netnotes.engine.networks.ergo.ErgoBox;
import io.netnotes.engine.networks.ergo.ErgoBoxAsset;

public class ErgoDexBoxInfo {
    private String m_poolId;
    private ErgoBoxAsset m_lp;
    private ErgoBoxAsset m_x;
    private ErgoBoxAsset m_y;
    private long m_feeNum;

    public ErgoDexBoxInfo(ErgoBox box){

    }

    public String getPoolId(){
        return m_poolId;
    }
}
