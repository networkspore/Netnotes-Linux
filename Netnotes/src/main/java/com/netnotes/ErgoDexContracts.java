package com.netnotes;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netnotes.engine.networks.ergo.ErgoCurrency;
import io.netnotes.engine.NoteConstants;
import io.netnotes.engine.PriceAmount;
import io.netnotes.engine.PriceCurrency;
import io.netnotes.engine.Utils;

public class ErgoDexContracts {

    public ErgoDexContracts(){

    }


    private static String getDexBuyTokenContract(){
        try{
            return Utils.getStringFromResource("/contracts/dex/buyTokenOrder.sc");
        }catch(IOException e){
            return null;
        }
    }

    private static String getDexSellTokenContract(){
        try{
            return Utils.getStringFromResource("/contracts/dex/sellTokenOrder.sc");
        }catch(IOException e){
            return null;
        }
    }

    public static JsonObject getSellTokenOutputData(String walletAddress, String tokenId, long pricePerToken, long dexFeePerToken, PriceAmount[] tokens, long nanoErgs) throws Exception{
        if(dexFeePerToken <= 1){
            throw new Exception( "dexFeePerToken should be > 1");
        }
        if(pricePerToken <= 1){
            throw new Exception( "tokenPricePerToken should be > 1");
        }

        String contractScript = getDexSellTokenContract();
        JsonObject contractData = new JsonObject();
        NoteConstants.addNanoErgAmountToDataObject(nanoErgs, contractData);
        NoteConstants.addTokensToDataObject(tokens, contractData);
        NoteConstants.addRegistersToDataObject(
            contractData,
            NoteConstants.getTokenIdBytesConstant("R4", tokenId),
            NoteConstants.getLongConstant("R5", pricePerToken),
            NoteConstants.getLongConstant("R6", dexFeePerToken)

        );

        contractData.addProperty("description", contractScript);
        contractData.add("compileConstants", getSellTokenConstants(walletAddress, tokenId, pricePerToken, dexFeePerToken));
        return contractData;

    }

    /** Parameters for DEX limit order seller contract
  * @param sellerPk seller's PK (used for canceling the contract (spending the box).
  * @param tokenId token id seller wants to sell
  * @param tokenPrice price per token in nanoERGs
  * @param dexFeePerToken DEX matcher's reward (per token, nanoERGs)*/
  private static JsonArray getSellTokenConstants(String walletAddress, String tokenId, long tokenPrice, long dexFeePerToken) throws Exception{
    JsonArray compileConstantsArray = new JsonArray();
    compileConstantsArray.add(NoteConstants.getWalletPKConstant("sellerPk", walletAddress));
    compileConstantsArray.add(NoteConstants.getTokenIdBytesConstant("tokenId", tokenId));
    compileConstantsArray.add(NoteConstants.getLongConstant("tokenPrice", tokenPrice));
    compileConstantsArray.add(NoteConstants.getLongConstant("dexFeePerToken", dexFeePerToken));
    return compileConstantsArray;
}


    public static JsonObject getBuyTokenOutputData(String walletAddress, String tokenId, long pricePerToken, long dexFeePerToken, long nanoErgs) throws Exception{
        if(dexFeePerToken <= 1){
            throw new Exception( "dexFeePerToken should be > 1");
        }
        if(pricePerToken <= 1){
            throw new Exception( "tokenPricePerToken should be > 1");
        }

        String contractScript = getDexBuyTokenContract();
        JsonObject contractData = new JsonObject();
        NoteConstants.addNanoErgAmountToDataObject(nanoErgs, contractData);
        NoteConstants.addRegistersToDataObject(
            contractData,
            NoteConstants.getTokenIdBytesConstant("R4", tokenId),
            NoteConstants.getLongConstant("R5", pricePerToken)
        );

        contractData.addProperty("description", contractScript);
        contractData.add("compileConstants", getBuyTokenConstants(walletAddress, tokenId, pricePerToken, dexFeePerToken));
        return contractData;

    }

    
    /** Parameters for DEX limit order buyer contract
  * @param buyerPk buyer's PK (used for canceling the contract (spending the box).
  * @param tokenId token id buyer wants to buy
  * @param tokenPrice price per token in nanoERGs
  * @param dexFeePerToken DEX matcher's reward (per token, nanoERGs)*/

    private static JsonArray getBuyTokenConstants(String walletAddress, String tokenId, long tokenPrice, long dexFeePerToken) throws Exception{
        JsonArray compileConstantsArray = new JsonArray();
        compileConstantsArray.add(NoteConstants.getWalletPKConstant("buyerPk", walletAddress));
        compileConstantsArray.add(NoteConstants.getTokenIdBytesConstant("tokenId", tokenId));
        compileConstantsArray.add(NoteConstants.getLongConstant("tokenPrice", tokenPrice));
        compileConstantsArray.add(NoteConstants.getLongConstant("dexFeePerToken", dexFeePerToken));

        return compileConstantsArray;
    }
    /*
    private static String m_buyTemplateHash = null;
    private static String m_sellTemplateHash = null;

    static{
        try{
            TreeHelper buyTree = getBuyTree(NetworkType.MAINNET);
            if(buyTree != null){
                Utils.logJson("**buyTree**", buyTree.getErgoTreeDetailsJson());
                m_buyTemplateHash = buyTree.getTemplateHashHex();
            }
        }catch(Exception e){

        }
        try{
            TreeHelper sellTree = getBuyTree(NetworkType.MAINNET);
            if(sellTree != null){
                Utils.logJson("**sellTree**", sellTree.getErgoTreeDetailsJson());
                m_sellTemplateHash = sellTree.getTemplateHashHex();
            }
        }catch(Exception e){

        }
    }

    public static String getBuyTemplateHash(){
        return m_buyTemplateHash;
    }
    public static String getSellTemplateHash(){
        return m_sellTemplateHash;
    }


    public static TreeHelper getSellTree(NetworkType networkType) throws Exception{

        long dexFeePerToken = 2222L;
        long tokenPrice = 1111L;

        String contractScript = getDexSellTokenContract();

        JsonArray compileConstantsArray = new JsonArray();

        

        compileConstantsArray.add(NoteConstants.getDummyPKConstant("sellerPk"));
        compileConstantsArray.add(NoteConstants.getHexBytesContractProperty("tokenId", NoteConstants.getBlankTokenIdHex()));
        compileConstantsArray.add(NoteConstants.getLongConstant("tokenPrice", tokenPrice));
        compileConstantsArray.add(NoteConstants.getLongConstant("dexFeePerToken", dexFeePerToken));
        
        return new TreeHelper(compileConstantsArray, contractScript, networkType);

    }

    public static TreeHelper getBuyTree(NetworkType networkType) throws Exception{

        long dexFeePerToken = 2222L;
        long tokenPrice = 1111L;

        String contractScript = getDexBuyTokenContract();
        
        JsonArray compileConstantsArray = new JsonArray();
        compileConstantsArray.add(NoteConstants.getDummyPKConstant("buyerPk"));
        compileConstantsArray.add(NoteConstants.getHexBytesContractProperty("tokenId", NoteConstants.getBlankTokenIdHex()));
        compileConstantsArray.add(NoteConstants.getLongConstant("tokenPrice", tokenPrice));
        compileConstantsArray.add(NoteConstants.getLongConstant("dexFeePerToken", dexFeePerToken));

        return new TreeHelper(compileConstantsArray, contractScript, networkType);

    }*/

 

    public static long getFeePerToken(BigDecimal dexFeeBigDecimal, BigDecimal tokenDecimal, PriceCurrency tokenCurrency){
        int decimals = tokenCurrency.getDecimals();

        long dexFee  = ErgoCurrency.getNanoErgsFromErgs(dexFeeBigDecimal);
        long volume = PriceAmount.calculateBigDecimalToLong(tokenDecimal, decimals);

        return dexFee == 0 || volume == 0 ? 0 : BigDecimal.valueOf(dexFee).divide(BigDecimal.valueOf(volume), 0, RoundingMode.HALF_UP).longValue();
    }

    public static long getPricePerToken(BigDecimal ergs, BigDecimal tokenBigDecimal, PriceCurrency tokenCurrency){
        int decimals = tokenCurrency.getDecimals();

        long nanoErgs = ErgoCurrency.getNanoErgsFromErgs(  ergs);
        long tokens = PriceAmount.calculateBigDecimalToLong(tokenBigDecimal, decimals);

        if(nanoErgs < 1 || tokens < 1){
            return 0;
        }

        return BigDecimal.valueOf(nanoErgs).divide(BigDecimal.valueOf(tokens), 0, RoundingMode.HALF_UP).longValue();
    }
}
