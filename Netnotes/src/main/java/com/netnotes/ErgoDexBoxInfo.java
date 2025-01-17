package com.netnotes;

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
