package com.netnotes;

import io.netnotes.engine.networks.ergo.ErgoTransactionPartner;
import io.netnotes.engine.networks.ergo.ErgoTransactionView;
import io.netnotes.engine.PriceAmount;
import io.netnotes.engine.networks.ergo.ErgoTransactionPartner.PartnerType;

public class ErgoSimpleSendTx extends ErgoTransactionView  {

    public ErgoSimpleSendTx(String txId, String parentAddress, String receipientAddress,  long sentNanoErgs, PriceAmount[] tokens, long feeNanoErgs, long created ){
        super(txId, parentAddress);
        setPartnerType(PartnerType.SENDER);
        tokens = tokens == null ? new PriceAmount[0] : tokens;
        setStatus(TransactionStatus.CREATED);
        setTimeStamp(created);
        setTxFlag(TransactionFlag.REQURES_UPDATE);

        setTxPartnerArray(new ErgoTransactionPartner[] {new ErgoTransactionPartner(getParentAddress(), PartnerType.SENDER, sentNanoErgs + feeNanoErgs, tokens), new ErgoTransactionPartner(receipientAddress, PartnerType.RECEIVER, sentNanoErgs, tokens), new ErgoTransactionPartner("",PartnerType.MINER, feeNanoErgs)});
     
    }

}
