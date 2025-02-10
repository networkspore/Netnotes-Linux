package com.netnotes;

import java.io.IOException;
import java.util.Arrays;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.utils.Utils;

import scorex.util.encode.Base16;

public class ErgoDexContracts {

    
    public final static String T2T_ADDRESS = "3gb1RZucekcRdda82TSNS4FZSREhGLoi1FxGDmMZdVeLtYYixPRviEdYireoM9RqC6Jf4kx85Y1jmUg5XzGgqdjpkhHm7kJZdgUR3VBwuLZuyHVqdSNv3eanqpknYsXtUwvUA16HFwNa3HgVRAnGC8zj8U7kksrfjycAM1yb19BB4TYR2BKWN7mpvoeoTuAKcAFH26cM46CEYsDRDn832wVNTLAmzz4Q6FqE29H9euwYzKiebgxQbWUxtupvfSbKaHpQcZAo5Dhyc6PFPyGVFZVRGZZ4Kftgi1NMRnGwKG7NTtXsFMsJP6A7yvLy8UZaMPe69BUAkpbSJdcWem3WpPUE7UpXv4itDkS5KVVaFtVyfx8PQxzi2eotP2uXtfairHuKinbpSFTSFKW3GxmXaw7vQs1JuVd8NhNShX6hxSqCP6sxojrqBxA48T2KcxNrmE3uFk7Pt4vPPdMAS4PW6UU82UD9rfhe3SMytK6DkjCocuRwuNqFoy4k25TXbGauTNgKuPKY3CxgkTpw9WfWsmtei178tLefhUEGJueueXSZo7negPYtmcYpoMhCuv4G1JZc283Q7f3mNXS";
    public final static String N2T_ADDRESS = "5vSUZRZbdVbnk4sJWjg2uhL94VZWRg4iatK9VgMChufzUgdihgvhR8yWSUEJKszzV7Vmi6K8hCyKTNhUaiP8p5ko6YEU9yfHpjVuXdQ4i5p4cRCzch6ZiqWrNukYjv7Vs5jvBwqg5hcEJ8u1eerr537YLWUoxxi1M4vQxuaCihzPKMt8NDXP4WcbN6mfNxxLZeGBvsHVvVmina5THaECosCWozKJFBnscjhpr3AJsdaL8evXAvPfEjGhVMoTKXAb2ZGGRmR8g1eZshaHmgTg2imSiaoXU5eiF3HvBnDuawaCtt674ikZ3oZdekqswcVPGMwqqUKVsGY4QuFeQoGwRkMqEYTdV2UDMMsfrjrBYQYKUBFMwsQGMNBL1VoY78aotXzdeqJCBVKbQdD3ZZWvukhSe4xrz8tcF3PoxpysDLt89boMqZJtGEHTV9UBTBEac6sDyQP693qT3nKaErN8TCXrJBUmHPqKozAg9bwxTqMYkpmb9iVKLSoJxG7MjAj72SRbcqQfNCVTztSwN3cRxSrVtz4p87jNFbVtFzhPg7UqDwNFTaasySCqM";
    //public final static String n2T_SWAP_SELL_TEMPLATE_ERG = "d803d6017300d602b2a4730100d6037302eb027201d195ed92b1a4730393b1db630872027304d804d604db63087202d605b2a5730500d606b2db63087205730600d6077e8c72060206edededededed938cb2720473070001730893c27205d07201938c72060173099272077e730a06927ec172050699997ec1a7069d9c72077e730b067e730c067e720306909c9c7e8cb27204730d0002067e7203067e730e069c9a7207730f9a9c7ec17202067e7310067e9c73117e7312050690b0ada5d90108639593c272087313c1720873147315d90108599a8c7208018c72080273167317";
    //public final static String N2T_SWAP_SELL_TEMPLATE_SPF = "d804d601b2a4730000d6027301d6037302d6049c73037e730405eb027305d195ed92b1a4730693b1db630872017307d806d605db63087201d606b2a5730800d607db63087206d608b27207730900d6098c720802d60a95730a9d9c7e997209730b067e7202067e7203067e720906edededededed938cb27205730c0001730d93c27206730e938c720801730f92720a7e7310069573117312d801d60b997e7313069d9c720a7e7203067e72020695ed91720b731492b172077315d801d60cb27207731600ed938c720c017317927e8c720c0206720b7318909c7e8cb2720573190002067e7204069c9a720a731a9a9c7ec17201067e731b067e72040690b0ada5d9010b639593c2720b731cc1720b731d731ed9010b599a8c720b018c720b02731f7320";
    public final static String MINER_PROP_HEX = "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304";

    public final static String N2T_PROP_HEX = Base16.encode(new byte[]{16,5,4,0,4,0,14,54,16,2,4,-96,11,8,-51,2,121,-66,102,126,-7,-36,-69,-84,85,-96,98,-107,-50,-121,11,7,2,-101,-4,-37,45,-50,40,-39,89,-14,-127,91,22,-8,23,-104,-22,2,-47,-110,-93,-102,-116,-57,-89,1,115,0,115,1,16,1,2,4,2,-47,-106,-125,3,1,-109,-93,-116,-57,-78,-91,115,0,0,1,-109,-62,-78,-91,115,1,0,116,115,2,115,3,-125,1,8,-51,-18,-84});
    
    public static JsonObject getN2tErgSellOutputData(String walletAddress, String poolId, long baseAmount, String quoteId, long minQuoteAmount, long feePerTokenNum, long feePerTokenDenom, long maxMinerFee, int feeDenom, int feeNum, long nanoErgs){
        
            String ergoTreeHex = getN2tErgSellErgoTree();
            String contractScript = getN2tErgSellContract();
            JsonObject contractData = new JsonObject();
            ErgoTransactionData.addNanoErgAmountToDataObject(nanoErgs, contractData);
            contractData.addProperty("ergoTree", ergoTreeHex);
            //contractData.add("compileConstants", getN2tErgSellCompileConstants());     
            //contractData.add("substituteConstants", getN2tErgSellSubstituteConstants(walletAddress, poolId, quoteId, maxMinerFee, N2T_ADDRESS));
            contractData.addProperty("description", contractScript);
            contractData.add("parameters", getN2tErgSellIndexedParameters(walletAddress, poolId, quoteId, maxMinerFee, baseAmount, minQuoteAmount,feePerTokenNum, feePerTokenDenom, feeDenom, feeNum));
            return contractData;

    }

    public static JsonObject getN2tSpfSellOutputData(boolean spectrumIsQuote, long baseAmount, long delta, long exFeePerTokenDenom, long maxExFee, String poolId, String quoteId, long minQuoteAmount, int feeNum, long maxMinerFee, String walletAddress, long nanoErgs, PriceAmount[] tokens){
        String ergoTree =  getN2tSpfSellErgoTree();
        if(ergoTree != null){
            String contractScript = getN2tSpfSellContract();
            JsonObject contractData = new JsonObject();
            ErgoTransactionData.addNanoErgAmountToDataObject(nanoErgs, contractData);
            ErgoTransactionData.addTokensToDataObject(tokens, contractData);
            contractData.add("parameters", getN2tSpfSellParameters(spectrumIsQuote, baseAmount, delta, exFeePerTokenDenom, maxExFee, poolId, quoteId, minQuoteAmount, feeNum, maxMinerFee, walletAddress));
            contractData.addProperty("ergoTree", ergoTree);
            contractData.addProperty("description", contractScript);
            return contractData;
        }
        return null;
    }

    
    private static String getN2tErgSellErgoTree(){
        try{
            return Utils.getStringFromResource("/ergoDex/contracts/n2t/erg/SwapSellERG.ergoTree");
        }catch(IOException e){
            return null;
        }
    }

    private static String getN2tSpfSellErgoTree(){
        try{
            return Utils.getStringFromResource("/ergoDex/contracts/n2t/spf/SwapSellSPF.ergoTree");
        }catch(IOException e){
            return null;
        }
    }

    private static String getN2tSpfBuyContract(){
        try{
            return Utils.getStringFromResource("/ergoDex/contracts/n2t/spf/SwapBuySPF.sc");
        }catch(IOException e){
            return null;
        }
    }

    private static String getN2tSpfSellContract(){
        try{
            return Utils.getStringFromResource("/ergoDex/contracts/n2t/spf/SwapSellSPF.sc");
        }catch(IOException e){
            return null;
        }
    }

    private static String getT2tSpfContract(){
        try{
            return Utils.getStringFromResource("/ergoDex/contracts/t2t/spf/SwapSPF.sc");
        }catch(IOException e){
            return null;
        }
    }

    private static String getN2tErgSellContract(){
        try{
            return Utils.getStringFromResource("/ergoDex/contracts/n2t/erg/SwapSellERG.sc");
        }catch(IOException e){
            return null;
        }
    }

    private String getN2tErgBuyContract(){
        try{
            return Utils.getStringFromResource("/ergoDex/contracts/n2t/erg/SwapBuyERG.sc");
        }catch(IOException e){
            return null;
        }
    }

    private String getT2tErgContract(){
        try{
            return Utils.getStringFromResource("/ergoDex/contracts/t2t/erg/SwapERG.sc");
        }catch(IOException e){
            return null;
        }
    }

    private static String getBlankPoolIdHex(){
        byte[] poolNFT = new byte[32];
        Arrays.fill(poolNFT, (byte) 2);
        return Base16.encode(poolNFT);
    }

    private static String getBlankQuoteIdHex(){
        byte[] quoteId = new byte[32];
        Arrays.fill(quoteId, (byte) 4);
        return Base16.encode(quoteId);
    }

    private static String getBlankSpectrumIdHex(){
        byte[] spectrumId = new byte[32];
        Arrays.fill(spectrumId, (byte) 3);
        
        return Base16.encode(spectrumId);
    }

    private static String getBlankP2PKPropBytesHex(){
        byte[] redeemerPropBytes = new byte[32];
        Arrays.fill(redeemerPropBytes, (byte) 1);
        
        return Base16.encode(redeemerPropBytes);
    }

   
    private static JsonArray getN2tErgSellIndexedParameters(String walletAddress, String poolId, String quoteId, long maxMinerFee, long baseAmount, long minQuoteAmount, long feePerTokenNum, long feePerTokenDenom, int feeDenom, int feeNum){
        JsonArray indexedParmetersArray = new JsonArray();
        indexedParmetersArray.add(ErgoTransactionData.getPKContractProperty("Pk", walletAddress, 0));
        indexedParmetersArray.add(ErgoTransactionData.getLongContractProperty("BaseAmount", baseAmount, 2));
        indexedParmetersArray.add(ErgoTransactionData.getTokenIdBytesContractProperty("PoolNFT", poolId, 8));
        indexedParmetersArray.add(ErgoTransactionData.getTokenIdBytesContractProperty("QuoteId", quoteId, 9));
        indexedParmetersArray.add(ErgoTransactionData.getLongContractProperty("MinQuoteAmount", minQuoteAmount, 10));
        indexedParmetersArray.add(ErgoTransactionData.getLongContractProperty("DexFeePerTokenNum", feePerTokenNum, 11));
        indexedParmetersArray.add(ErgoTransactionData.getLongContractProperty("DexFeePerTokenDenom", feePerTokenDenom, 12));
        indexedParmetersArray.add(ErgoTransactionData.getIntContractProperty("FeeNum", feeNum, 14));
        indexedParmetersArray.add(ErgoTransactionData.getIntContractProperty("FeeDenom", feeDenom, 16));
        indexedParmetersArray.add(ErgoTransactionData.getLongContractProperty("BaseAmount", baseAmount, 17));
        indexedParmetersArray.add(ErgoTransactionData.getIntContractProperty("FeeNum", feeNum, 18));
        indexedParmetersArray.add(ErgoTransactionData.getLongContractProperty("MaxMinerFee", maxMinerFee, 22));
        return indexedParmetersArray;
    }

    private static JsonArray getN2tErgSellCompileConstants(){
        JsonArray indexedParmetersArray = new JsonArray();
        indexedParmetersArray.add(ErgoTransactionData.getDlogProvInputProperty("Pk"));
        indexedParmetersArray.add(ErgoTransactionData.getHexBytesContractProperty("PoolNFT", getBlankPoolIdHex()));
        indexedParmetersArray.add(ErgoTransactionData.getHexBytesContractProperty("QuoteId", getBlankQuoteIdHex()));
        indexedParmetersArray.add(ErgoTransactionData.getHexBytesContractProperty("MinerPropBytes", MINER_PROP_HEX));
        indexedParmetersArray.add(ErgoTransactionData.getLongContractProperty("MaxMinerFee", 10000L));

        return indexedParmetersArray;
    }

    private static JsonArray getN2tErgSellSubstituteConstants(String walletAddress, String poolId, String quoteId, long maxMinerFee, String minerAddress){
        JsonArray indexedParmetersArray = new JsonArray();
        indexedParmetersArray.add(ErgoTransactionData.getPKContractProperty("Pk", walletAddress));
        indexedParmetersArray.add(ErgoTransactionData.getTokenIdBytesContractProperty("PoolNFT", poolId));
        indexedParmetersArray.add(ErgoTransactionData.getTokenIdBytesContractProperty("QuoteId", quoteId));
        indexedParmetersArray.add(ErgoTransactionData.getAddressPropBytesContractProperty("MinerPropBytes", minerAddress));
        indexedParmetersArray.add(ErgoTransactionData.getLongContractProperty("MaxMinerFee", maxMinerFee));
        return indexedParmetersArray;
    }



    private static JsonArray getN2tSpfSellParameters(boolean spectrumIsQuote, long baseAmount, long delta, long exFeePerTokenDenom, long maxExFee, String poolId, String quoteId, long minQuoteAmount, int feeNum, long maxMinerFee, String walletAddress){
        JsonArray contractConstants = new JsonArray();
        contractConstants.add(ErgoTransactionData.getLongContractProperty("ExFeePerTokenDenom", exFeePerTokenDenom, 1)); 
        contractConstants.add(ErgoTransactionData.getLongContractProperty("Delta", delta, 2)); 
        contractConstants.add(ErgoTransactionData.getLongContractProperty("BaseAmount", baseAmount, 3)); 
        contractConstants.add(ErgoTransactionData.getIntContractProperty("FeeNum", feeNum,4));
        contractConstants.add(ErgoTransactionData.getPKContractProperty("RefundProp", walletAddress, 5));
        contractConstants.add(ErgoTransactionData.getBooleanContractProperty("SpectrumIsQuote", spectrumIsQuote, 10));
        contractConstants.add(ErgoTransactionData.getLongContractProperty("MaxExFee", maxExFee, 11));
        contractConstants.add(ErgoTransactionData.getTokenIdBytesContractProperty("PoolNFT", poolId, 13));
        contractConstants.add(ErgoTransactionData.getWalletP2PKPropBytesContractProperty("RedeemerPropBytes", walletAddress,14));
        contractConstants.add(ErgoTransactionData.getTokenIdBytesContractProperty("QuoteId", quoteId, 15));
        contractConstants.add(ErgoTransactionData.getLongContractProperty("MinQuoteAmount", minQuoteAmount, 16)); 
        contractConstants.add(ErgoTransactionData.getTokenIdBytesContractProperty("SpectrumId", ErgoDex.SPF_ID, 23));
        contractConstants.add(ErgoTransactionData.getIntContractProperty("FeeDenom", 1000, 27));
        contractConstants.add(ErgoTransactionData.getAddressPropBytesContractProperty("MinerPropBytes", N2T_ADDRESS, 28));
        contractConstants.add(ErgoTransactionData.getLongContractProperty("MaxMinerFee", maxMinerFee, 31));
        return contractConstants;
    }

    private static JsonArray getN2tSpfSellCompileConstants(){
        
        JsonArray compileData = new JsonArray();
        compileData.add(ErgoTransactionData.getLongContractProperty("ExFeePerTokenDenom", 22222L));
        compileData.add(ErgoTransactionData.getLongContractProperty("Delta", 11111L));
        compileData.add(ErgoTransactionData.getLongContractProperty("BaseAmount", 1200L)); //3
        compileData.add(ErgoTransactionData.getIntContractProperty("FeeNum", 999)); //4
        compileData.add(ErgoTransactionData.getDlogProvInputProperty("RefundProp")); //5
        compileData.add(ErgoTransactionData.getBooleanContractProperty("SpectrumIsQuote", false)); //10
        compileData.add(ErgoTransactionData.getLongContractProperty("MaxExFee", 1400L)); //11
        compileData.add(ErgoTransactionData.getHexBytesContractProperty("PoolNFT", getBlankPoolIdHex())); //13
        compileData.add(ErgoTransactionData.getHexBytesContractProperty("RedeemerPropBytes", getBlankP2PKPropBytesHex())); //14
        compileData.add(ErgoTransactionData.getHexBytesContractProperty("QuoteId", getBlankQuoteIdHex())); //15
        compileData.add(ErgoTransactionData.getLongContractProperty("MinQuoteAmount", 800L)); //16
        compileData.add(ErgoTransactionData.getHexBytesContractProperty("SpectrumId", getBlankSpectrumIdHex())); //23
        compileData.add(ErgoTransactionData.getIntContractProperty("FeeDenom", 1000)); //27
        compileData.add(ErgoTransactionData.getAddressPropBytesContractProperty("MinerPropBytes", MINER_PROP_HEX)); //28
        compileData.add(ErgoTransactionData.getLongContractProperty("MaxMinerFee", 10000L)); //31

        return compileData;
    }

}
