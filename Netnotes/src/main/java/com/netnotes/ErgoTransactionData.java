package com.netnotes;

import com.google.gson.JsonObject;
import com.satergo.Wallet;
import com.satergo.WalletKey.Failure;
import com.utils.Base58;

import javafx.beans.property.SimpleObjectProperty;
import scorex.util.encode.Base16;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.ergoplatform.appkit.*;
import org.ergoplatform.appkit.impl.ErgoTreeContract;
import org.ergoplatform.sdk.ErgoId;
import org.ergoplatform.sdk.ErgoToken;
import org.ergoplatform.sdk.JavaHelpers;

import sigmastate.SType;
import sigmastate.Values;
import sigmastate.crypto.DLogProtocol;
import sigmastate.crypto.DLogProtocol.DLogProverInput;
import sigmastate.crypto.DLogProtocol.ProveDlog;
import sigmastate.serialization.ErgoTreeSerializer;
import sigma.Coll;
import scala.collection.IndexedSeq;
import sigmastate.Values.Constant;
import sigmastate.Values.ErgoTree;
import org.ergoplatform.appkit.impl.ErgoScriptContract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ErgoTransactionData {

    public final static String LONG_TYPE = "long";
    public final static String BIG_INT_TYPE = "bigInt";
    public final static String SHORT_TYPE = "short";
    public final static String HEX_BYTE_TYPE = "hexByte";
    public final static String ERGO_HEX = "ergoHex";
    public final static String INT_TYPE = "integer";
    public final static String BOOLEAN_TYPE = "boolean";
    public final static String BLANK_DLOG_PROV_INPUT = "BlankDlogProvInput";
    public final static String ADDRESS_PUBLIC_KEY = "addressPK";
    public final static String WALLET_ADDRESS_P2PK_PROVE_DLOG = "walletAddressP2PK";
    public final static String WALLET_ADDRESS_P2PK_PROP_BYTES = "walletAddressP2PKBytes";
    public final static String ADDRESS_PROP_BYTES = "addressPropBytes";
    public final static String HEX_BYTES = "hexToBytes";
    public final static String TOKEN_ID_BYTES = "tokenIdBytes";

    public final static String CURRENT_WALLET_FILE = "wallet file";

    private final JsonObject m_txDataObject;
    private final NetworkType m_networkType;

    private List<ErgoInputData> m_inputList = null;
    private NamedNodeUrl m_namedNodeUrl = null;
    private ErgoNetworkUrl m_ergoExplorerUrl = null;

    private List<OutputData> m_outputList;
/*

    private boolean m_isBabbleFees = false;
     */  

    public List<ErgoInputData> getErgoInputData(){
        return m_inputList;
    }

    public ErgoInputData getChangeInputData(){
        for(int i =0 ; i< m_inputList.size(); i++){
            ErgoInputData inputData = m_inputList.get(i);
            if(inputData.isChangeAddress()){
                return inputData;
            }
        }
        return null;
    }

    public ErgoInputData[] getAssetsInputData(){
        ArrayList<ErgoInputData> inputs = new ArrayList<>();
        for(int i =0 ; i< m_inputList.size(); i++){
            ErgoInputData inputData = m_inputList.get(i);
            if(inputData.isAssetsInput()){
                inputs.add(inputData);
            }
        }
        return inputs.toArray(new ErgoInputData[inputs.size()]);
    }

    public ErgoInputData getFeeInputData(){
        for(int i =0 ; i< m_inputList.size(); i++){
            ErgoInputData inputData = m_inputList.get(i);
            if(inputData.isFeeInput()){
                return inputData;
            }
        }
        return null;
    }

    public NamedNodeUrl getNamedNodeUrl(){
        return m_namedNodeUrl;
    }
    public ErgoNetworkUrl getErgoExplorerUrl(){
        return m_ergoExplorerUrl;
    }

    public List<OutputData> getOutputList(){
        return m_outputList;
    }
    /*
    public long getTotalNanoErgs(){
        return m_totalNanoErgs;
    }
    public long getTotalFeeNanoErgs(){
        return m_totalFeeNanoErgs;
    }
    
    public boolean isBabbleFees(){
        return m_isBabbleFees;
    }
    public ErgoToken[] getTotalTokens(){
        return m_totalTokens;
    }
 */
    public NetworkType getNetworkType(){
        return m_networkType;
    }

    public JsonObject getTxDataObject(){
        return m_txDataObject;
    }

 
    public ErgoTransactionData(JsonObject ergoTxDataObject, NetworkType networkType, JsonObject defaultNode, JsonObject defaultExplorer) throws Exception{
   
        if(!checkNetworkType(ergoTxDataObject, networkType)){
            throw new Exception("Network type must be " + networkType);
        }
        m_txDataObject = ergoTxDataObject;
        m_networkType = networkType; 

        m_inputList = getInputsFromDataObject(ergoTxDataObject);

        ErgoInputData changeAddressInput = getChangeInputData();

        if(changeAddressInput == null){
            throw new Exception("Change input not provided");
        }

        AddressInformation changeAddressInformation = changeAddressInput.getAddressInformation();

        switch(changeAddressInput.getWalletType()){
            case CURRENT_WALLET_FILE:
                if(!changeAddressInformation.getAddress().isP2PK()){
                    throw new Exception("Change address is not valid for " + CURRENT_WALLET_FILE);
                }
            break;
            default:
                throw new Exception("Change address type is not currently supported, " + CURRENT_WALLET_FILE + " required.");
        }

        ErgoInputData feeInputData = getFeeInputData();

        if(feeInputData == null){
            throw new Exception("Fee input not provided");
        }
       
        long feeAmountNanoErgs = feeInputData.getFeeNanoErgs();
        
        if(feeAmountNanoErgs == -1){
            throw new Exception("No fee provided");
        }

        if(feeAmountNanoErgs < 1000000){
            throw new Exception("Minimum fee of 0.001 Erg required (1000000 nanoErg)");
        }

     
        NamedNodeUrl namedNodeUrl = getNamedNodeUrlFromDataObject(ergoTxDataObject, defaultNode);

        if(namedNodeUrl == null){
            throw new Exception("Node unavailable");
        }

        if(namedNodeUrl.getUrlString() == null){
            throw new Exception("No node URL provided");
        }

        m_namedNodeUrl = namedNodeUrl;


        ErgoNetworkUrl explorerNetworkUrl = getExplorerUrl(ergoTxDataObject, defaultExplorer);

        if(explorerNetworkUrl == null){
            throw new Exception("Explorer url not provided");
        }

        m_ergoExplorerUrl = explorerNetworkUrl;
       

        JsonElement boxesElement = ergoTxDataObject.get("outputs");
        
        if(boxesElement == null){
            throw new Exception("No outputs array element provided");
        }

        if(boxesElement.isJsonNull() || !boxesElement.isJsonArray()){
            throw new Exception("Outputs element is not a JsonArray");
        }

        JsonArray boxesJsonArray = boxesElement.getAsJsonArray();
        int size = boxesJsonArray.size();

        if(size == 0){
            throw new Exception("Zero outputs provided");
        }
        OutputData[] outputs = new OutputData[size];

        for(int i = 0; i < size ; i++){
            JsonElement boxElement = boxesJsonArray.get(i);
            try{
                checkElementNull(boxElement);
            }catch(NullPointerException e){
                throw new Exception("Box: " + e.toString() + " (index: " + i+ ")" );
            }
            if(!boxElement.isJsonObject()){
                throw new Exception("Box: Is not a json object (index: " + i+ ")" );
            }
            OutputData output = new OutputData(boxElement.getAsJsonObject());
            outputs[i] = output;
        }

        m_outputList = List.of(outputs);

    }

    
    public static void addNodeUrlToDataObject(JsonObject dataObject, NoteInterface nodeInterface){
        JsonObject nodeInterfaceObject = nodeInterface.getJsonObject();
        dataObject.add("node", nodeInterfaceObject);
    }

    public static NamedNodeUrl getNamedNodeUrlFromDataObject(JsonObject dataObject, JsonObject defaultNode){

        JsonElement nodeElement = dataObject.get("node");

        JsonObject nodeObject = nodeElement != null && nodeElement.isJsonObject() ? nodeElement.getAsJsonObject() : defaultNode;
        
        if(nodeObject != null){
            JsonElement namedNodeElement = nodeObject.get("namedNode");
            if( namedNodeElement != null && namedNodeElement.isJsonObject()){
                JsonObject namedNodeJson = namedNodeElement.getAsJsonObject();
                
                NamedNodeUrl namedNode = null;
                try {
                    namedNode = new NamedNodeUrl(namedNodeJson);
                    return namedNode;
                }catch(Exception e1){
                       
                }
            }
        }
      
        return null;
    }

    
    public static ErgoNetworkUrl getExplorerUrl(JsonObject dataObject, JsonObject defaultExplorer){
        JsonElement explorerElement = dataObject.get("explorer");

        JsonObject explorerObject = explorerElement != null && explorerElement.isJsonObject() ? explorerElement.getAsJsonObject() : defaultExplorer;
        
        if(explorerObject != null){
       
            JsonElement namedExplorerElement = explorerObject.get("ergoNetworkUrl");

            JsonObject namedExplorerJson = namedExplorerElement.getAsJsonObject();
            
            try {
                
                ErgoNetworkUrl explorerUrl = new ErgoNetworkUrl(namedExplorerJson);

                return explorerUrl;

            } catch (Exception e1) {
              
            }

            
        }
     
        return null;
    }
   
    public static List<ErgoInputData> getInputsFromDataObject(JsonObject dataObject) throws Exception{
        JsonElement inputsElement = dataObject.get("inputs");
        if(inputsElement == null){
            throw new Exception("Inputs element not found in transaction");
        }
        if(inputsElement.isJsonNull()){
            throw new Exception("Inputs element is JsonNull");
        }
        if(!inputsElement.isJsonArray()){
            throw new Exception("Inputs element is not a JsonArray");
        }
        JsonArray inputJsonArray = inputsElement.getAsJsonArray();
        ErgoInputData[] inputData = new ErgoInputData[inputJsonArray.size()];
        for(int i = 0; i < inputJsonArray.size(); i++){
            JsonElement element = inputJsonArray.get(i);
            if(element.isJsonNull() || !element.isJsonObject()){
                throw new Exception("InputData element is not a JsonObject");
            }
            inputData[i] = new ErgoInputData(element.getAsJsonObject());
        }

        return List.of(inputData);
    }


    public static JsonObject getChangeAddressObjectFromDataObject(JsonObject dataObject){
        return getAddressObjectFromDataObject("changeAddress", dataObject);
    }
    
    public static JsonObject getAddressObjectFromDataObject(String property, JsonObject dataObject){
        JsonElement walletElement = dataObject != null ? dataObject.get(property) : null;
        JsonObject walletJson = walletElement != null && walletElement.isJsonObject() ? walletElement.getAsJsonObject() : null;
        return walletJson;
    }

    

    public static String getStringPropertyFromObject(String property, JsonObject json){
        JsonElement element = json.get(property);
        if(element != null && !element.isJsonNull()){
            return element.getAsString();
        }
        return null;
    }

    public static String getNameFromObject(JsonObject json){
        return getStringPropertyFromObject("name", json);
    }

    public static String getAddressFromObject(JsonObject json){
        return getStringPropertyFromObject("address", json);
    }

    public static String getWalletTypeFromObject(JsonObject json){
        return getStringPropertyFromObject("walletType", json);
    }

    public static JsonObject createWalletAddressObject(String address, String name, String walletType){
        JsonObject ergoObject = new JsonObject();
        ergoObject.addProperty("address", address);
        ergoObject.addProperty("name", name);
        ergoObject.addProperty("walletType", walletType);
        return ergoObject;
    }

    public static void addWalletChangeAddressToDataObject(String address, String walletName, String walletType, JsonObject dataObject){
        addWalletAddressToDataObject("changeAddress", address, walletName, walletType, dataObject);
    }

    public static void addWalletAddressToDataObject(String property, String address, String walletName, String walletType, JsonObject dataObject){
        JsonObject addressObject = createWalletAddressObject(address, walletName, walletType);
        dataObject.add(property, addressObject);
    }


    public static void addWalletAddressToAddressInput(String address, String walletName, String walletType, long nanoErgs, PriceAmount[] tokens, JsonArray addressInputs){
        JsonObject addressObject = createWalletAddressObject(address, walletName, walletType);
        addNanoErgAmountToDataObject(nanoErgs, addressObject);
        addTokensToDataObject(tokens, addressObject);
        addressInputs.add(addressObject);
    }

    public static void addSingleInputToDataObject(ErgoInputData inputData, JsonObject dataObject){

        JsonObject inputDataObject = inputData.getJsonObject();

        JsonArray inputsArray = new JsonArray();
        inputsArray.add(inputDataObject);

        addInputsToDataObject(inputsArray, dataObject);
    }

    public static void addInputsToDataObject(JsonArray inputsArray, JsonObject dataObject){
        dataObject.add("inputs", inputsArray);
    }


  
    public static boolean checkNetworkType(JsonObject dataObject, NetworkType networkType){
        JsonElement networkElement = dataObject != null ? dataObject.get("network") : null;
        JsonObject networkObject = networkElement != null && networkElement.isJsonObject() ? networkElement.getAsJsonObject() : null;
        JsonElement networkTypeElement = networkObject != null ? networkObject.get("networkType") : null;
        String networkTypeString = networkTypeElement != null ? networkTypeElement.getAsString() : null;
        return networkTypeString != null &&  networkType.toString().toLowerCase().equals(networkTypeString.toLowerCase());
    }

    public static JsonObject getNetworkTypeObject(NetworkType networkType){
        JsonObject ergoObject = new JsonObject();
        ergoObject.addProperty("networkType", networkType.toString());
        return ergoObject;
    }

    public static void addNetworkTypeToDataObject(NetworkType networkType, JsonObject dataObject){
        JsonObject networkTypeObject = getNetworkTypeObject(networkType);
        dataObject.add("network", networkTypeObject);
    }


    public static long getFeeAmountFromDataObject(JsonObject dataObject){
        JsonElement feeElement = dataObject != null ? dataObject.get("feeAmount") : null;
        JsonObject feeObject = feeElement != null && feeElement.isJsonObject() ? feeElement.getAsJsonObject() : null;
        JsonElement nanoErgsElement = feeObject != null ? feeObject.get("nanoErgs") : null;
        return nanoErgsElement != null ? nanoErgsElement.getAsLong() : -1;
    }

    public static long getErgAmountFromDataObject(JsonObject dataObject){
        JsonElement ergAmountElement = dataObject != null ? dataObject.get("ergAmount") : null;
        JsonObject nanoErgObject = ergAmountElement != null && ergAmountElement.isJsonObject() ? ergAmountElement.getAsJsonObject() : null;
        JsonElement nanoErgsElement = nanoErgObject != null ? nanoErgObject.get("nanoErgs") : null;
        return nanoErgsElement != null ? nanoErgsElement.getAsLong() : -1;
    }

    public static ErgoToken[] getTokensFromDataObject(JsonObject dataObject) throws NullPointerException{
        JsonElement assetsElement = dataObject != null ? dataObject.get("tokens") : null;
        JsonArray assetsArray = assetsElement != null && assetsElement.isJsonArray() ? assetsElement.getAsJsonArray() : null;

        if(assetsArray != null){
            ErgoToken[] tokenArray = new ErgoToken[assetsArray.size()];
            
            for(int i = 0; i < assetsArray.size() ; i++ ){
                JsonElement element = assetsArray.get(i);
                if(element != null && !element.isJsonNull() && element.isJsonObject()){
                    JsonObject assetJson = element.getAsJsonObject();
                    ErgoToken ergoToken = PriceAmount.getErgoToken(assetJson);
                    if(ergoToken == null){
                        throw new NullPointerException("Provided asset is missing token information. \n(index: "+i+")");
                    }
                    tokenArray[i] = ergoToken;
                }else{
                    throw new NullPointerException("Provided asset is not a valid json object. (Index: "+i+")");
                }
            
            }

            return tokenArray;
        }
        return new ErgoToken[0];
    }
    

  

    public static void addTokensToDataObject(PriceAmount[] priceAmounts, JsonObject dataObject){
        priceAmounts = priceAmounts == null ? new PriceAmount[0] : priceAmounts;
        JsonArray tokenArray = new JsonArray();
        for(int i = 0 ; i < priceAmounts.length ; i++){
            PriceAmount token = priceAmounts[i];
            tokenArray.add(token.getAmountObject());
        }
        dataObject.add("tokens", tokenArray);
    }

    public static void addTokensToDataObject(ErgoToken[] tokens, JsonObject dataObject){
        tokens = tokens == null ? new ErgoToken[0] : tokens;
        JsonArray tokenArray = new JsonArray();
        for(int i = 0 ; i < tokens.length ; i++){
            ErgoToken token = tokens[i];
            JsonObject tokenJson = new JsonObject();
            tokenJson.addProperty("id", token.getId().toString());
            tokenJson.addProperty("value", token.getValue());
            tokenArray.add(tokenJson);
        }
        dataObject.add("tokens", tokenArray);
    }



    public static JsonObject createNanoErgsObject(long nanoErg){
        JsonObject ergoObject = new JsonObject();
        ergoObject.addProperty("nanoErgs", nanoErg);
        return ergoObject;
    }

    public static boolean addFeeAmountToDataObject(PriceAmount ergoAmount, JsonObject dataObject){
        if(!ergoAmount.getTokenId().equals(ErgoCurrency.TOKEN_ID)){
            return false;
        }
        long nanoErgs = ergoAmount.getLongAmount();
        JsonObject nanoErgsObject = createNanoErgsObject(nanoErgs);
        nanoErgsObject.addProperty("ergs", ergoAmount.getBigDecimalAmount());
        dataObject.add("feeAmount", nanoErgsObject);
        return nanoErgs >= ErgoNetwork.MIN_NANO_ERGS;
    }

    public static boolean addFeeAmountToDataObject(long nanoErgs, JsonObject dataObject){
        
        JsonObject nanoErgsObject = createNanoErgsObject(nanoErgs);
        nanoErgsObject.addProperty("ergs", PriceAmount.calculateLongToBigDecimal(nanoErgs, ErgoCurrency.DECIMALS));
        dataObject.add("feeAmount", nanoErgsObject);
        return nanoErgs >= ErgoNetwork.MIN_NANO_ERGS;
    }


    public static void addNanoErgAmountToDataObject(long nanoErgs, JsonObject dataObject){
        JsonObject nanoErgsObject = createNanoErgsObject(nanoErgs);
        nanoErgsObject.addProperty("ergs", PriceAmount.calculateLongToBigDecimal(nanoErgs, ErgoCurrency.DECIMALS));
        dataObject.add("ergAmount", nanoErgsObject);
    }



    public List<OutputData> getOutputData(){
        return m_outputList;
    }


    
    public void checkConstants(JsonArray constantsArray, NetworkType networkType, boolean indexes) throws Exception{
        
        String type = null;
        String name = null;
        
        int p2PKIntType = networkType == NetworkType.MAINNET ? AddressInformation.P2PK_INT_TYPE : AddressInformation.TEST_P2PK_INT_TYPE; 

        for(int i = 0; i < constantsArray.size() ; i++){
            JsonElement element = constantsArray.get(i);
            if(element == null || !element.isJsonObject() || element.isJsonNull()){
                throw new Exception("Contract constants error: array contains invalid objects\n(index: " + i + ")");
            }
            JsonObject json = element.getAsJsonObject();
            JsonElement nameElement = json.get("name");
            JsonElement valueElement = json.get("value");
            JsonElement typeElement = json.get("type");
            
            if(nameElement == null || typeElement == null || nameElement.isJsonNull() || typeElement.isJsonNull()){
                throw new Exception("Contract constants error: json object contains invalid elements\n(index: " + i+ ")");
            }
            if(indexes){
                JsonElement indexElement = json.get("index");
                if(indexElement == null){
                    throw new Exception("Parameters error: json object does not contain index element\n(index: " + i+ ")");
                }
                if(indexElement.isJsonNull()){
                    throw new Exception("Parameters error: index elements is JsonNull\n(index: " + i+ ")");
                }
                try{
                    indexElement.getAsInt();
                }catch(Exception e){
                    throw new Exception("Parameters error: index element is not a valid integerl\n(index: " + i+ ")");
                }
            }

            type = typeElement.getAsString();
            name = nameElement.getAsString();

           

            switch(type){
                case LONG_TYPE:
                    testValueAsLong(i, name, valueElement);
                break;
                case BIG_INT_TYPE:
                    testValueAsBigInteger(i, name, valueElement);
                break;
                case SHORT_TYPE: 
                    testValueAsShort(i, name, valueElement);
                break;
                case HEX_BYTE_TYPE:
                    testValueAsByte(i, name, valueElement);
                break;
                case INT_TYPE:
                    testValueAsInteger(i, name, valueElement);
                break;
                case BOOLEAN_TYPE:
                    testValueAsBoolean(i, name, valueElement);
                break;
                case ADDRESS_PUBLIC_KEY:
                case WALLET_ADDRESS_P2PK_PROVE_DLOG:
                case WALLET_ADDRESS_P2PK_PROP_BYTES:
                    
                    testAddressInformation(testValueAsAddressInformation(i, name, valueElement, networkType), p2PKIntType, name, i);
                    
                break;
                case HEX_BYTES:
                    testValueAsHex(i, name, valueElement);
                break;
                case BLANK_DLOG_PROV_INPUT:
                    logBlankDlogProvInput(i, name);
                break;
                case ADDRESS_PROP_BYTES:
                    testValueAsAddressInformation(i, name, valueElement, networkType);
                break;
                case TOKEN_ID_BYTES:
                    testValueAsTokenId(i, name, valueElement);
                break;
                default:
                    throw new Exception("Contract constants error: array contains invalid type.\n(property: " + name + " index: " + i+ ")");
            }
        }
    }

    public static void testAddressInformation(AddressInformation addressInformation, int addressIntType, String name, int index)throws Exception{
        int adrIntType = addressInformation.getAddressIntType();

        if(adrIntType != addressIntType){
            throw new Exception("Address is incorrect type: " + AddressInformation.getAddressTypeString(adrIntType) + ", expected: " + AddressInformation.getAddressTypeString(addressIntType) + "\n(property: " + name + " index: " + index + ")");
        } 
    }


    
    public static void checkElementNull(JsonElement element ) throws NullPointerException{
        if(element == null){
            throw new NullPointerException("Element is null");
        }
        if(element.isJsonNull()){
            throw new NullPointerException("JsonNull not supported");
        }
    }

    public static BigInteger testValueAsBigInteger(int i, String name, JsonElement valueElement) throws Exception{
        try{
            checkElementNull(valueElement);
        }catch(NullPointerException e){
            throw new Exception("Contract constants error: Value " + e.toString() + "\n(property: " + name + " index: " + i+ ")" );
        }
        try{  
            BigInteger bigIntegerValue = valueElement.getAsBigInteger();
           
            return bigIntegerValue;
        }catch(Exception e){
            throw new Exception("Contract constants error: array contains invalid BigInt. \n(property: " + name + " index: " + i+ ")" );
        }
    }

    public static short testValueAsShort(int i, String name, JsonElement valueElement) throws Exception{
        try{
            checkElementNull(valueElement);
        }catch(NullPointerException e){
            throw new Exception("Contract constants error: Value " + e.toString() + " \n(property: " + name + " index: " + i+ ")" );
        }
        try{
            short sh = valueElement.getAsShort();
          
            return sh;
        }catch(Exception e){
            throw new Exception("Contract constants error: array contains invalid short. \n(property: " + name + " index: " + i+ ")" );
        }
    }

    public static byte testValueAsByte(int i, String name, JsonElement valueElement) throws Exception{
        try{
            checkElementNull(valueElement);
        }catch(NullPointerException e){
            throw new Exception("Contract constants error: Value " + e.toString() + " \n(property: " + name + " index: " + i+ ")" );
        }
        try{
                    
            byte[] bytes = Hex.decodeHex(valueElement.getAsString());
            if(bytes.length != 1){
                throw new Exception("Contract constants error: array hex byte is incorrect length. \n(property: " + name + " index: " + i+ ")" );
            }
          
            return bytes[0];
        }catch(Exception e){
            throw new Exception("Contract constants error: array contains invalid hex value. \n(property: " + name + " index: " + i+ ")" );
        }
    }
    
    public static int testValueAsInteger(int i, String name, JsonElement valueElement) throws Exception{
        try{
            checkElementNull(valueElement);
        }catch(NullPointerException e){
            throw new Exception("Contract constants error: Value " + e.toString() + " \n(property: " + name + " index: " + i+ ")" );
        }
        try{
            int integerValue = valueElement.getAsInt();
            int parseValue = Integer.parseInt(valueElement.getAsString());
            if(integerValue != parseValue){
                throw new Exception("Contract constants error: Value returns inconsitent results \n(property: " + name + " index: " + i+ ")" );
            }
           
            return integerValue;
        }catch(Exception e){
            throw new Exception("Contract constants error: array contains invalid integer. \n(property: " + name + " index: " + i+ ")" );
        }
    }

    public static boolean testValueAsBoolean(int i, String name, JsonElement valueElement) throws Exception{
        try{
            checkElementNull(valueElement);
        }catch(NullPointerException e){
            throw new Exception("Contract constants error: Value " + e.toString() + " \n(property: " + name + " index: " + i+ ")" );
        }
        try{
            boolean boolValue = valueElement.getAsBoolean();
           
            return boolValue;
        }catch(Exception e){
            throw new Exception("Contract constants error: array contains invalid boolean. \n(property: " + name + " index: " + i+ ")" );
        }
    }

    public AddressInformation testValueAsAddressInformation(int i, String name, JsonElement valueElement, NetworkType networkType) throws Exception{
        try{
            checkElementNull(valueElement);
        }catch(NullPointerException e){
            throw new Exception("Contract constants error: Value " + e.toString() + " \n(property: " + name + " index: " + i+ ")" );
        }
        
        try{
            String str = valueElement.getAsString();

            AddressInformation adrInfo = new AddressInformation(str);
            Address address = adrInfo.getAddress();
            if( address == null){
                throw new Exception("Contract constants error: Constant is not a valid address. \n(property: " + name + " index: " + i+ ")" );
            }

            if(networkType != address.getNetworkType()){
                throw new Exception("Contract constants error: Address is incorrect network type. \n(property: " + name + " index: " + i+ ")" );
            }
        
            return adrInfo;
        }catch(Exception e){
            throw new Exception("Contract constants error: array contains invalid address value. \n(property: " + name + " index: " + i+ ")" );
        }
    }

    public static boolean testValueAsHex(int i, String name, JsonElement valueElement) throws Exception{
        try{
            checkElementNull(valueElement);
        }catch(NullPointerException e){
            throw new Exception("Contract constants error: Value " + e.toString() + "\n(property: " + name + " index: " + i+ ")" );
        }
        
        try{
            String str = valueElement.getAsString();
            String b16Test = str.replaceAll("[^A-Fa-f0-9]", "");
            if(!b16Test.equals(str)){
                throw new Exception("Contract constants error: value contains invalid hex characters\n(property: " + name + " index: " + i+ ")" );
            }
            
            return true;
        }catch(Exception e){
            throw new Exception("Contract constants error: array contains invalid hex value. \n(property: " + name + " index: " + i+ ")" );
        }
    }

    public static long testValueAsLong(int i, String name, JsonElement valueElement) throws Exception{
        try{
            checkElementNull(valueElement);
        }catch(NullPointerException e){
            throw new Exception("Contract constants error: Value " + e.toString() + "\n(property: " + name + " index: " + i+ ")" );
        }
        try{
            long longValue = valueElement.getAsLong();
           
            return longValue;
        }catch(Exception e){
            throw new Exception("Contract constants error: array contains invalid long.\n(property: " + name + " index: "+ i+ ")" );
        }
    }

    public static Address testValueAsAddress(int i, JsonElement valueElement, String name, int networkIntType) throws Exception{
        try{
            checkElementNull(valueElement);
        }catch(NullPointerException e){
            throw new Exception("Contract constants error: Value " + e.toString() + "\n(property: " + name + " index: " + i+ ")" );
        }
        try{
            String str = valueElement.getAsString();
            byte[] bytes = AddressInformation.convertAddressToBytes(str);
            
            if(bytes == null){
                throw new Exception("Contract constants error: Address could not be converted to bytes. \n(property: " + name + " index: " + i+ ")" );
            }
            int[] ints = AddressInformation.getAddressIntType(bytes[0]);
            if(ints == null){
                throw new Exception("Contract constants error: Address is invalid type.\n(property: " + name + " index: " + i+ ")" );
            }
            if(ints[1] != networkIntType){
                throw new Exception("Contract constants error: Address is not " +AddressInformation.getAddressTypeNetworkType(networkIntType) + ". \n(property: " + name + " index: " + i+ ")" );
            } 
            
            Address address = Address.create(str);
          
            return address;
        }catch(Exception e){
            throw new Exception("Contract constants error: array contains invalid address value. \n(property: " + name + " index: " + i+ ")" );
        }
    }

    public static ErgoId testValueAsTokenId(int i, String name, JsonElement valueElement) throws Exception{
        try{
            checkElementNull(valueElement);
        }catch(NullPointerException e){
            throw new Exception("Contract constants error: Value " + e.toString() + "\n(property: " + name + " index: " + i+ ")" );
        }
        try{
            String str = valueElement.getAsString();
      
            String b58Test = str.replaceAll("[^A-HJ-NP-Za-km-z0-9]", "");
            if(!b58Test.equals(str)){
                throw new Exception("Contract constants error: Value contains invalid base58 characters\n(property: " + name + " index: " + i+ ")" );
            }
            
            return getErgoIdFromString(str);
        }catch(Exception e){
            throw new Exception("Contract constants error: array contains invalid address value. \n(property: " + name + " index: " + i+ ")" );
        }
    }

    public static Object getConstantObject(String type, JsonElement valueElement, Wallet wallet, NetworkType networkType){
        switch(type){
            case LONG_TYPE:
                return valueElement.getAsLong();
            case INT_TYPE:
                return valueElement.getAsInt();
            case BOOLEAN_TYPE:
                return valueElement.getAsBoolean();
            case WALLET_ADDRESS_P2PK_PROVE_DLOG:
                return getWalletAddressPK(wallet, valueElement.getAsString(), networkType); 
            case WALLET_ADDRESS_P2PK_PROP_BYTES:
                return getWalletAddressPKPropBytes(wallet, valueElement.getAsString(), networkType);
            case ADDRESS_PROP_BYTES:
                return getAddressPropBytes(valueElement.getAsString());
            case HEX_BYTES:
                return Base16.decode(valueElement.getAsString()).get();
            case BIG_INT_TYPE:
                return valueElement.getAsBigInteger();
            case SHORT_TYPE: 
                return valueElement.getAsShort();
            case HEX_BYTE_TYPE:
                return Base16.decode(valueElement.getAsString()).get()[0];
            case BLANK_DLOG_PROV_INPUT:
                return getMaxLongPublicImage();
            case TOKEN_ID_BYTES:
                return getTokenIdBytes(valueElement.getAsString());
        }
        return null;
    }


    public static ErgoId getErgoIdFromString(String tokenId){
        ErgoToken token = new ErgoToken(tokenId, 0L);
        return token.getId();
    }

    public static byte[] getTokenIdBytes(String tokenId){
        ErgoId ergoId = getErgoIdFromString(tokenId);
        return ergoId.getBytes();
    }

    public static ProveDlog getMaxLongPublicImage(){

        return new DLogProverInput(BigInteger.valueOf(Long.MAX_VALUE)).publicImage();
    }


    public static void logBlankDlogProvInput(int i, String name) throws Exception{
       
    }



 
    

    public static ProveDlog getWalletAddressPK(Wallet wallet, String addressString, NetworkType networkType){
        Address address = getWalletAddress(wallet, addressString, networkType);
        if(address != null){
            ProveDlog key = address.getPublicKey();
            return key;
        }else{
            return null;
        }
    }

    public static byte[] getWalletAddressPKPropBytes(Wallet wallet, String addressString, NetworkType networkType){
        Address address = getWalletAddress(wallet, addressString, networkType);
        if(address != null){
            byte[] propBytes = address.toPropositionBytes();
            return propBytes;
        }
        return null;
    }

    public static byte[] getAddressPropBytes(String addressString){
        
        return Address.create(addressString).toPropositionBytes();
        
    }

    public static Address getWalletAddress(Wallet wallet, String addressString, NetworkType networkType){
        SimpleObjectProperty<Address> resultAddress = new SimpleObjectProperty<>(null);
        wallet.myAddresses.forEach((index, name) -> {                    
            try {
                Address address = wallet.publicAddress(networkType, index);
                if(address.toString().equals(addressString)){
                   resultAddress.set(address);
                }
            } catch (Failure e) {
       
            }
        });

        return resultAddress.get();
    }

    public Address getChangeAddress(Wallet wallet){
        ErgoInputData changeInputdata = getChangeInputData();
        switch(changeInputdata.getWalletType()){
            case CURRENT_WALLET_FILE:
                return getWalletAddress(wallet, changeInputdata.getAddressString(), m_networkType);
        }
        return null;
    }
    

    public static ErgoContract substituteConstants(ErgoContract ergoContract, JsonArray constantsArray, Wallet wallet, NetworkType networkType){
        for(int i = 0; i < constantsArray.size() ; i++){
            JsonElement element = constantsArray.get(i);
            JsonObject json = element.getAsJsonObject();
            JsonElement nameElement = json.get("name");
            JsonElement valueElement = json.get("value");
            JsonElement typeElement = json.get("type");
    
    
            String type = typeElement.getAsString();
            String name = nameElement.getAsString();

            ergoContract = ergoContract.substConstant(name, getConstantObject(type, valueElement, wallet, networkType));
        }

        return ergoContract;
    }

    
    public static JsonObject getLongContractProperty(String propertyName, long value){
        JsonObject json = new JsonObject();
        json.addProperty("name", propertyName);
        json.addProperty("value", value);
        json.addProperty("type", LONG_TYPE);
        return json;
    }

    public static JsonObject getIntContractProperty(String propertyName, int value){
        JsonObject json = new JsonObject();
        json.addProperty("name", propertyName);
        json.addProperty("value", value);
        json.addProperty("type", INT_TYPE);
        return json;
    }

    public static JsonObject getBigIntegerContractProperty(String propertyName, BigInteger value){
        JsonObject json = new JsonObject();
        json.addProperty("name", propertyName);
        json.addProperty("value", value);
        json.addProperty("type", BIG_INT_TYPE);
        return json;
    }

    public static JsonObject getBooleanContractProperty(String propertyName, boolean value){
        JsonObject json = new JsonObject();
        json.addProperty("name", propertyName);
        json.addProperty("value", value);
        json.addProperty("type", BOOLEAN_TYPE);
        return json;
    }


    public static JsonObject getPKContractProperty(String propertyName, String address){
        String b58Test = address.replaceAll("[^A-HJ-NP-Za-km-z0-9]", "");
        if(!b58Test.equals(address)){
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("name", propertyName);
        json.addProperty("value", address);
        json.addProperty("type", WALLET_ADDRESS_P2PK_PROVE_DLOG);
        return json;
    }

    public static JsonObject getDlogProvInputProperty(String propertyName){
        JsonObject json = new JsonObject();
        json.addProperty("name", propertyName);
        json.addProperty("value", 0);
        json.addProperty("type", BLANK_DLOG_PROV_INPUT);
        return json;
    }

    public static JsonObject getWalletP2PKPropBytesContractProperty(String propertyName, String address){
        String b58Test = address.replaceAll("[^A-HJ-NP-Za-km-z0-9]", "");
        if(!b58Test.equals(address)){
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("name", propertyName);
        json.addProperty("value", address);
        json.addProperty("type", WALLET_ADDRESS_P2PK_PROP_BYTES);
        return json;
    }

    public static JsonObject getAddressPropBytesContractProperty(String propertyName, String address){
        String b58Test = address.replaceAll("[^A-HJ-NP-Za-km-z0-9]", "");
        if(!b58Test.equals(address)){
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("name", propertyName);
        json.addProperty("value", address);
        json.addProperty("type", ADDRESS_PROP_BYTES);
        return json;
    }

    public static JsonObject getAddressPublicKeyContractProperty(String propertyName, String address){
        String b58Test = address.replaceAll("[^A-HJ-NP-Za-km-z0-9]", "");
        if(!b58Test.equals(address)){
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("name", propertyName);
        json.addProperty("value", address);
        json.addProperty("type", ADDRESS_PUBLIC_KEY);
        return json;
    }

    public static JsonObject getHexBytesContractProperty(String propertyName, String base16){
        String b16Test = base16.replaceAll("[^A-Fa-f0-9]", "");
        if(!b16Test.equals(base16)){
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("name", propertyName);
        json.addProperty("value", base16);
        json.addProperty("type", HEX_BYTES);
        return json;
    }

    public static JsonObject getTokenIdBytesContractProperty(String propertyName, String tokenId){
        String b58Test = tokenId.replaceAll("[^A-HJ-NP-Za-km-z0-9]", "");
        if(!b58Test.equals(tokenId)){
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("name", propertyName);
        json.addProperty("value", tokenId);
        json.addProperty("type", TOKEN_ID_BYTES);
        return json;
    }
    


    //Parameters
    
    public static JsonObject getLongContractProperty(String propertyName, long value, int index){
        JsonObject json = getLongContractProperty(propertyName, value);
        json.addProperty("index", index);
        return json;
    }

    public static JsonObject getIntContractProperty(String propertyName, int value, int index){
        JsonObject json = getIntContractProperty(propertyName, value);
        json.addProperty("index", index);
        return json;
    }
    public static JsonObject getBigIntegerContractProperty(String propertyName, BigInteger value, int index){
        JsonObject json = getBigIntegerContractProperty(propertyName, value);
        json.addProperty("index", index);
        return json;
    }

    public static JsonObject getBooleanContractProperty(String propertyName, boolean value, int index){
        JsonObject json = getBooleanContractProperty(propertyName, value);
        json.addProperty("index", index);
        return json;
    }


    public static JsonObject getErgoHexErgoValueProperty(String propertyName, String base16, int index){
        String b16Test = base16.replaceAll("[^A-Fa-f0-9]", "");
        if(!b16Test.equals(base16)){
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("name", propertyName);
        json.addProperty("value", base16);
        json.addProperty("type", ERGO_HEX);
        json.addProperty("index", index);

        return json;
    }

    public static JsonObject getPKContractProperty(String propertyName, String address, int index){
        JsonObject json = getPKContractProperty(propertyName, address);
        json.addProperty("index", index);
        return json;
    }

    public static JsonObject getDlogProvInputProperty(String propertyName, int index){
        JsonObject json = getDlogProvInputProperty(propertyName, index);
        json.addProperty("index", index);
        return json;
    }

    public static JsonObject getWalletP2PKPropBytesContractProperty(String propertyName, String address, int index){
        JsonObject json = getWalletP2PKPropBytesContractProperty(propertyName, address);
        json.addProperty("index", index);
        return json;
    }

    public static JsonObject getAddressPropBytesContractProperty(String propertyName, String address, int index){
        JsonObject json = getAddressPropBytesContractProperty(propertyName, address);
        json.addProperty("index", index);
        return json;
    }

    public static JsonObject getAddressPublicKeyContractProperty(String propertyName, String address, int index){
        JsonObject json = getAddressPublicKeyContractProperty(propertyName, address);
        json.addProperty("index", index);
        return json;
    }

    public static JsonObject getHexBytesContractProperty(String propertyName, String base16, int index){
        JsonObject json = getHexBytesContractProperty(propertyName, base16);
        json.addProperty("index", index);
        return json;
    }

    public static JsonObject getTokenIdBytesContractProperty(String propertyName, String base58, int index){
        JsonObject json = getTokenIdBytesContractProperty(propertyName, base58);
        json.addProperty("index", index);
        return json;
    }


    public class OutputData{
        private String m_ergoTreeHex = null;
        private String m_ergoScript = null;
        private JsonArray m_compileConstantsArray = null;
        private JsonArray m_substituteConstantsArray = null;
        private JsonArray m_parametersArray = null;
        private long m_nanoErgs = 0;
        private ErgoToken[] m_ergoTokens = null;

        public boolean isSubstituteConstants(){
            return m_substituteConstantsArray != null;
        }

   
        public OutputData(JsonObject outputDataObject) throws Exception{
            JsonElement ergoTreeElement = outputDataObject.get("ergoTree");
            JsonElement contractElement = outputDataObject.get("description");
            JsonElement compileConstantsElement = outputDataObject.get("compileConstants");
            JsonElement substituteConstantsElement = outputDataObject.get("substituteConstants");

    

            if(ergoTreeElement != null && ergoTreeElement.isJsonNull()){
                throw new Exception("Contract ergoTree is invalid type");
            }

            m_ergoTreeHex = ergoTreeElement != null ? ergoTreeElement.getAsString() : null;


            if(contractElement != null && contractElement.isJsonNull()){
                throw new Exception("Contract description is invalid type");
            }

             m_ergoScript = contractElement != null ? contractElement.getAsString() : null;
            

            if(substituteConstantsElement != null && (substituteConstantsElement.isJsonNull() || !substituteConstantsElement.isJsonArray())){
                throw new Exception("Contract substitute constants invalid type");
            }

            JsonArray substituteConstantsArray = substituteConstantsElement != null ? substituteConstantsElement.getAsJsonArray() : null;
            if(substituteConstantsArray != null){
                checkConstants(substituteConstantsArray , m_networkType,false);
            }
            m_substituteConstantsArray = substituteConstantsArray;

            if(compileConstantsElement != null && (compileConstantsElement.isJsonNull() || !compileConstantsElement.isJsonArray())){
                throw new Exception("Contract compile constants invalid");
            }

            JsonArray compileConstantArray = compileConstantsElement != null ? compileConstantsElement.getAsJsonArray() : null;
            if(compileConstantArray != null){
            
                checkConstants(compileConstantArray, m_networkType, false);
            
            }
            m_compileConstantsArray = compileConstantArray;

            JsonElement parametersElement = outputDataObject.get("parameters");

            if(parametersElement != null && (parametersElement.isJsonNull() || !parametersElement.isJsonArray())){
                throw new Exception("Contract parameters invalid");
            }

            JsonArray parametersArray = parametersElement != null ? parametersElement.getAsJsonArray() : null;
            if(parametersArray != null){
            
                checkConstants(parametersArray, m_networkType, true);
                
            }


            m_parametersArray = parametersArray;

            m_ergoTokens = getTokensFromDataObject(outputDataObject);


            
            m_nanoErgs = getErgAmountFromDataObject(outputDataObject);

            if(!(isErgoTreeContract() || isCompileContract())){
                throw new Exception("No contract provided for output");
            }
        }

  
        public String getContractString(){
            return m_ergoScript;
        }
        public JsonArray getCompileConstantsArray(){
            return m_compileConstantsArray;
        }
        public JsonArray getSubstituteConstantsArray(){
            return m_substituteConstantsArray;
        }
        public JsonArray getParametersArray(){
            return m_parametersArray;
        }

        public static Constants getContractContants(JsonArray constantsArray, Wallet wallet, NetworkType networkType){
        
            ConstantsBuilder builder = ConstantsBuilder.create();
    
            for(int i = 0; i < constantsArray.size() ; i++){
                JsonElement element = constantsArray.get(i);
    
                JsonObject json = element.getAsJsonObject();
                JsonElement nameElement = json.get("name");
                JsonElement valueElement = json.get("value");
                JsonElement typeElement = json.get("type");
        
                String type = typeElement.getAsString();
                String name = nameElement.getAsString();
                builder.item(name, getConstantObject(type, valueElement, wallet, networkType));
            }
    
            return builder.build();
        }

        public boolean isErgoTreeContract(){
            return m_ergoTreeHex != null && m_parametersArray != null;
        }
      
        public boolean isCompileContract(){
            return m_compileConstantsArray != null && m_ergoScript != null;
        }
    
        public String getErgoTreeHex(){
            return m_ergoTreeHex;
        }

        public boolean isErgoTreeHex(){
            return m_ergoTreeHex != null;
        }


        public Values.ErgoTree getErgoTree(){
            return ErgoTreeSerializer.DefaultSerializer().deserializeErgoTree(Base16.decode( getErgoTreeHex()).get());
        }
        private byte[] m_tmpBytes;
        private ErgoValue<Coll<Byte>> m_tmpByteArrayErgoValue;

        public void decodeErgoTreeValues(int[] indexes, ErgoValue<?>[] ergoValues, Wallet wallet, NetworkType networkType){
            int length = indexes.length;
            ConstantsBuilder builder = ConstantsBuilder.create();
            
            for(int i = 0; i < length ; i++){
                JsonElement element = m_parametersArray.get(i);

                JsonObject json = element.getAsJsonObject();
                JsonElement nameElement = json.get("name");
                JsonElement valueElement = json.get("value");
                JsonElement typeElement = json.get("type");
                JsonElement indexElement = json.get("index");
                
                String type = typeElement.getAsString();
                indexes[i] = indexElement.getAsInt();
                String value = valueElement.getAsString();
                Address address = null;
                
                switch(type){
                    case LONG_TYPE:
           
                        ergoValues[i] = ErgoValue.of(valueElement.getAsLong());
                        break;
                    case INT_TYPE:
                        ergoValues[i] = ErgoValue.of(valueElement.getAsInt());
                        break;
                    case BOOLEAN_TYPE:
                        ergoValues[i] = ErgoValue.of(valueElement.getAsBoolean());
                        break;
                    case WALLET_ADDRESS_P2PK_PROVE_DLOG:
                        address = getWalletAddress(wallet, value, networkType);
                        ergoValues[i] = ErgoValue.of(address.getPublicKey());
                        break; 
                    case WALLET_ADDRESS_P2PK_PROP_BYTES:
                        address = getWalletAddress(wallet, value, networkType);
                        ergoValues[i] = ErgoValue.of(address.toPropositionBytes());
                        break;
                    case ADDRESS_PROP_BYTES:
                        address = Address.create(value);
                        ergoValues[i] = ErgoValue.of(address.toPropositionBytes());
                        break;
                    case BIG_INT_TYPE:
                        ergoValues[i] = ErgoValue.of(valueElement.getAsBigInteger());
                        break;
                    case SHORT_TYPE: 
                        ergoValues[i] = ErgoValue.of(valueElement.getAsShort());
                        break;
                    case HEX_BYTE_TYPE:
                        m_tmpBytes = Base16.decode(value).get();
                        m_tmpByteArrayErgoValue = ErgoValue.of(m_tmpBytes);
                        ergoValues[i] = m_tmpByteArrayErgoValue;
                        break;
                    case TOKEN_ID_BYTES:
                        m_tmpBytes = getTokenIdBytes(value);
                        m_tmpByteArrayErgoValue = ErgoValue.of(m_tmpBytes);
                        ergoValues[i] = m_tmpByteArrayErgoValue;
                    break;
                    case HEX_BYTES:
                    case ERGO_HEX:
                        ergoValues[i] = ErgoValue.fromHex(value);
                    break;
                }
                try {
                    Files.writeString(App.logFile.toPath(), "ergoValues[" +i+"]:" + ergoValues[i].toHex() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }
                m_tmpBytes = null;
                m_tmpByteArrayErgoValue = null;
            }
        }

        public Values.ErgoTree getErgoTreeFromHex(){
            Values.ErgoTree ergoTree = JavaHelpers.decodeStringToErgoTree(m_ergoTreeHex);
            logErgoTreeDetails(ergoTree);
            return ergoTree;
        }

        public ErgoContract createErgoTreeContract(Values.ErgoTree inputTree, BlockchainContext ctx, Wallet wallet){
            int size = m_parametersArray.size();
            ErgoValue<?>[] ergoValues = new ErgoValue[size];
            int[] indexes = new int[size];


            decodeErgoTreeValues(indexes, ergoValues, wallet, ctx.getNetworkType());

         
            Values.ErgoTree outputTree = ErgoTreeTemplate.fromErgoTree(inputTree)
            .withParameterPositions(indexes)
            .applyParameters(ergoValues);

            ErgoTreeContract ergoContract = new ErgoTreeContract(outputTree, ctx.getNetworkType());
            
            logContractDetails(ergoContract);

            return ergoContract;
        }

        

        public ErgoContract compileContract(BlockchainContext ctx, Wallet wallet){
            Constants compileConstants = m_compileConstantsArray != null ? getContractContants(m_compileConstantsArray, wallet, getNetworkType()) : null;
            
            ErgoScriptContract ergoScriptContract = ErgoScriptContract.create(compileConstants, m_ergoScript, m_networkType);

            if(isSubstituteConstants()){
                return substituteConstants(ergoScriptContract, m_substituteConstantsArray, wallet, getNetworkType());
            }
            return ergoScriptContract;
            
            
        }

        public ErgoContract compileWithParameters(BlockchainContext ctx, Wallet wallet){
        
            ErgoContract contract = compileContract(ctx, wallet);
            
            return createErgoTreeContract(contract.getErgoTree(), ctx, wallet);
        }

        public boolean isCompileWidthParameters(){
            return m_compileConstantsArray != null && m_ergoScript != null && m_parametersArray != null;
        }

        public OutBox getOutBox(BlockchainContext ctx, UnsignedTransactionBuilder txBuilder, Wallet wallet){
            ErgoContract contract = isCompileWidthParameters() ? compileWithParameters(ctx, wallet) : isCompileContract() ? compileContract(ctx, wallet) : isErgoTreeHex() ? createErgoTreeContract(getErgoTreeFromHex(), ctx, wallet) : null;
            logContractDetails(contract);
            if(contract == null){
                return null;
            }
            OutBoxBuilder newBoxBuilder = txBuilder.outBoxBuilder()
            .value(m_nanoErgs)
            .contract(contract);

            if (m_ergoTokens != null && m_ergoTokens.length > 0) {
                newBoxBuilder.tokens(m_ergoTokens);
            }

            try {
                Files.writeString(App.logFile.toPath(), "Creating outBox\n" , StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
            
            }

            return newBoxBuilder.build();
        }
   
        public static String getTemplateHash(byte[] templateBytes){
            return Base16.encode(scorex.crypto.hash.Sha256.hash(templateBytes));
        }

        public static JsonObject createErgoTreeDetailsObject(Values.ErgoTree ergoTree){
            byte[] templateBytes = ergoTree.template();

            JsonObject json = new JsonObject();
            json.addProperty("hex", ergoTree.bytesHex());
            json.addProperty("templateHex", Base16.encode(templateBytes));
            json.addProperty("templateHash", getTemplateHash(templateBytes));
            json.addProperty("hasDeserialize", ergoTree.hasDeserialize());
            json.addProperty("isConstantSegregation", ergoTree.isConstantSegregation());
            json.addProperty("version", ergoTree.version());
            json.add("constants",getErgoTreeConstantsObject(ergoTree));

        
            return json;
        }


        private static int m_tmpConstantIndex = 0;
        public static JsonArray getErgoTreeConstantsObject(ErgoTree inputTree){
            
            JsonArray jsonArray = new JsonArray();

            List<Constant<SType>> list = scala.collection.JavaConverters.asJava(inputTree.constants());
            
            m_tmpConstantIndex = 0;
           

            for(Constant<SType> constant : list){

                ErgoValue<?> value  = AppkitIso.isoErgoValueToSValue().from(constant);
                JsonObject json = new JsonObject();
                json.addProperty("ergoValue<?>", value + "");
                json.addProperty("type", value.getType() + "");
                json.addProperty("valueObject",value.getValue() + "");
                json.addProperty("hex", value.toHex());
                json.addProperty("index", m_tmpConstantIndex);
                jsonArray.add(json);
            }

            return jsonArray;
        }

        public static void logContractDetails(ErgoContract contract){
            logErgoTreeDetails(contract.getErgoTree());
        }

        public static void logErgoTreeDetails(Values.ErgoTree ergoTree){
            JsonObject ergoTreeDetailsObject = createErgoTreeDetailsObject(ergoTree);
              
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Files.writeString(App.logFile.toPath(), "**ErgoTree**/n" + gson.toJson(ergoTreeDetailsObject) +"\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {

            }
        }
    }
}
