package com.netnotes;

import java.util.List;

import org.ergoplatform.sdk.ErgoToken;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;


public class ErgoInputData{

    public static final String CHANGE_INPUT = "ChangeInput";
    public static final String ASSETS_INPUT = "AssetsInput";
    public static final String FEE_INPUT = "FeeInput";
    

    private String m_name = null;
    private AddressInformation m_addressInformation = null;
    private List<String> m_inputRoles = null;
    private String m_walletType = null;
    private long m_nanoErgs = -1;
    private long m_feeNanoErgs = -1;
    private ErgoToken[] m_tokens = null;

    public ErgoInputData(JsonObject inputDataObject) throws Exception{

        String walletType = ErgoTransactionData.getWalletTypeFromObject(inputDataObject);
        if(walletType == null){
            throw new Exception("Type not provided for input");
        }

        m_walletType = walletType;
        String addressString = ErgoTransactionData.getAddressFromObject(inputDataObject);
        if(addressString == null){
            throw new Exception("Address string not provided for input");
        }
        m_addressInformation = new AddressInformation(addressString);
        String name = ErgoTransactionData.getNameFromObject(inputDataObject);
        if(name == null){
            throw new Exception("Name not provided for input");
        }

        getRolesFromObject(inputDataObject);
        
        m_nanoErgs = ErgoTransactionData.getErgAmountFromDataObject(inputDataObject);

        if(m_nanoErgs < ErgoNetwork.MIN_NANO_ERGS){
            throw new Exception("Input must be greater than " + ErgoNetwork.MIN_NETWORK_FEE + " ERG");
        }
    
        m_tokens = ErgoTransactionData.getTokensFromDataObject(inputDataObject);

        m_feeNanoErgs = ErgoTransactionData.getFeeAmountFromDataObject(inputDataObject);

        if(m_feeNanoErgs != -1 && m_feeNanoErgs < ErgoNetwork.MIN_NANO_ERGS){
            throw new Exception("Fee must be greater than " + ErgoNetwork.MIN_NETWORK_FEE + " ERG\n(Babblefees not supported yet)");
        }

    }

    public ErgoInputData(String name, String walletType, String addressString, long nanoErgs, ErgoToken[] tokens, long feeNanoErgs, String... roles){
        m_name = name;
        m_walletType = walletType;
        m_addressInformation = new AddressInformation(addressString);
        m_nanoErgs = nanoErgs;
        m_feeNanoErgs = feeNanoErgs;
        m_tokens = tokens;
        m_inputRoles = List.of(roles);
    }

    public static ErgoToken[] convertPriceAmountsToErgoTokens(PriceAmount[] priceAmounts){
        ErgoToken[] tokens = new ErgoToken[priceAmounts.length];
        for(int i =0 ; i< priceAmounts.length ; i++){
            tokens[i] = priceAmounts[i].getErgoToken();
        }
        return tokens;
    }
    
    public long getTotalInputNanoErgs(){
        return m_nanoErgs + (isFeeInput() ? m_feeNanoErgs : 0);
    }

    private void getRolesFromObject(JsonObject inputObject) throws Exception{
        JsonElement rolesElement = inputObject.get("roles");
        if(rolesElement == null){
            throw new Exception("Roles element not added to input");
        }
        if( rolesElement.isJsonNull() || !rolesElement.isJsonArray()){
            throw new Exception("Roles element is not a JsonArray");
        }
        JsonArray jsonArray = rolesElement.getAsJsonArray();
        String[] rolesArray = new String[jsonArray.size()];
        for(int i = 0; i < jsonArray.size(); i++){
            JsonElement element = jsonArray.get(i);
            if(element.isJsonNull()){
                throw new Exception("Role should not be JsonNull");
            }
            if(!element.isJsonPrimitive()){
                throw new Exception("Role should be String primitive");
            }
            rolesArray[i] = element.getAsString();
        }
        m_inputRoles = List.of(rolesArray);
    }

    private void addRolesToObject(JsonObject jsonObject){
        JsonArray jsonArray = new JsonArray();
        for(String role : m_inputRoles){
            jsonArray.add(new JsonPrimitive(role));
        }
        jsonObject.add("roles", jsonArray);
    }

    public boolean isChangeAddress(){
        return m_inputRoles.contains(CHANGE_INPUT);
    }

    public boolean isAssetsInput(){
        return m_inputRoles.contains(CHANGE_INPUT);
    }

    public boolean isFeeInput(){
        return m_inputRoles.contains(FEE_INPUT);
    }

    public String getName(){
        return m_name;
    }

    public String getAddressString(){
        return m_addressInformation.getAddressString();
    }

    public AddressInformation getAddressInformation(){
        return m_addressInformation;
    }

    public String getWalletType(){
        return m_walletType;
    }

    public long getNanoErgs(){
        return m_nanoErgs;
    }

    public long getFeeNanoErgs(){
        return m_feeNanoErgs;
    }

    public ErgoToken[] getTokens(){
        return m_tokens;
    }

    public JsonObject getJsonObject(){
        JsonObject json = ErgoTransactionData.createWalletAddressObject(m_addressInformation.getAddressString(), m_name, m_walletType);
        ErgoTransactionData.addNanoErgAmountToDataObject(m_nanoErgs, json);
        ErgoTransactionData.addTokensToDataObject(m_tokens, json);
        ErgoTransactionData.addFeeAmountToDataObject(m_feeNanoErgs, json);
        addRolesToObject(json);
        return json;
    }
}


