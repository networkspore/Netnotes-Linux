package com.netnotes;

import com.google.gson.JsonObject;
import io.netnotes.engine.networks.ergo.AddressInformation;
import io.netnotes.engine.networks.ergo.ErgoInputData;
import io.netnotes.engine.networks.ergo.ErgoNetworkUrl;
import io.netnotes.engine.NamedNodeUrl;
import io.netnotes.engine.NoteConstants;
import io.netnotes.engine.Utils;
import com.satergo.Wallet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.ergoplatform.appkit.*;
import org.ergoplatform.sdk.ErgoToken;
import sigmastate.serialization.ErgoTreeSerializer;

import sigmastate.Values;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.netnotes.ergo.TreeHelper;
import io.netnotes.notes.ParamTypes;
import scorex.util.encode.Base16;


public class ErgoTransactionData {
    
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
   
        if(!NoteConstants.checkNetworkType(ergoTxDataObject, networkType)){
            throw new Exception("Network type must be " + networkType);
        }
        m_txDataObject = ergoTxDataObject;
        m_networkType = networkType; 

        m_inputList = NoteConstants.getInputsFromDataObject(ergoTxDataObject);

        ErgoInputData changeAddressInput = getChangeInputData();

        if(changeAddressInput == null){
            throw new Exception("Change input not provided");
        }

        AddressInformation changeAddressInformation = changeAddressInput.getAddressInformation();

        switch(changeAddressInput.getWalletType()){
            case NoteConstants.CURRENT_WALLET_FILE:
                if(!changeAddressInformation.getAddress().isP2PK()){
                    throw new Exception("Change address is not valid for " + NoteConstants.CURRENT_WALLET_FILE);
                }
            break;
            default:
                throw new Exception("Change address type is not currently supported, " + NoteConstants.CURRENT_WALLET_FILE + " required.");
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

     
        NamedNodeUrl namedNodeUrl = NoteConstants.getNamedNodeUrlFromDataObject(ergoTxDataObject, defaultNode);

        if(namedNodeUrl == null){
            throw new Exception("Node unavailable");
        }

        if(namedNodeUrl.getUrlString() == null){
            throw new Exception("No node URL provided");
        }

        m_namedNodeUrl = namedNodeUrl;


        ErgoNetworkUrl explorerNetworkUrl = NoteConstants.getExplorerUrl(ergoTxDataObject, defaultExplorer);

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
                NoteConstants.checkElementNull(boxElement);
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

    public void prepareContract(Wallet wallet) throws Exception{
        for(OutputData outputData :m_outputList){
            outputData.prepareContract(wallet);
        }
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
                case ParamTypes.LONG_TYPE:
                    NoteConstants.testValueAsLong(i, name, valueElement);
                break;
                case ParamTypes.BIG_INT_TYPE:
                    NoteConstants.testValueAsBigInteger(i, name, valueElement);
                break;
                case ParamTypes.SHORT_TYPE: 
                    NoteConstants.testValueAsShort(i, name, valueElement);
                break;
                case ParamTypes.BYTE_TYPE:
                    NoteConstants.testValueAsByte(i, name, valueElement);
                break;
                case ParamTypes.INT_TYPE:
                    NoteConstants.testValueAsInteger(i, name, valueElement);
                break;
                case ParamTypes.BOOLEAN_TYPE:
                    NoteConstants.testValueAsBoolean(i, name, valueElement);
                break;
                case ParamTypes.ADDRESS_PUBLIC_KEY:
                case NoteConstants.WALLET_ADDRESS_PK:
                    NoteConstants.testAddressInformation(testValueAsAddressInformation(i, name, valueElement, networkType), p2PKIntType, name, i);
                    
                break;
                case ParamTypes.HEX_BYTES:
                    NoteConstants.testValueAsHex(i, name, valueElement);
                break;
                case ParamTypes.DUMMY_PK:
                   // NoteConstants.logBlankDlogProvInput(i, name);
                break;
                case ParamTypes.BLANK_PK:
                   // NoteConstants.logBlankDlogProvInput(i, name);
                break;
                case NoteConstants.WALLET_ADDRESS_PROP_BYTES:
                case ParamTypes.ADDRESS_PROP_BYTES:
                    testValueAsAddressInformation(i, name, valueElement, networkType);
                break;
                case ParamTypes.TOKEN_ID_BYTES:
                    NoteConstants.testValueAsTokenId(i, name, valueElement);
                break;
                default:
                    throw new Exception("Contract constants error: array contains invalid type.\n(property: " + name + " index: " + i+ ")");
            }
        }
    }

    public AddressInformation testValueAsAddressInformation(int i, String name, JsonElement valueElement, NetworkType networkType) throws Exception{
        try{
            NoteConstants.checkElementNull(valueElement);
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
            address.asP2PK();
            return adrInfo;
        }catch(Exception e){
            throw new Exception("Contract constants error: array contains invalid address value. \n(property: " + name + " index: " + i+ ")" );
        }
    }


    public Address getChangeAddress(Wallet wallet){
        ErgoInputData changeInputdata = getChangeInputData();
        switch(changeInputdata.getWalletType()){
            case NoteConstants.CURRENT_WALLET_FILE:
                return AddressData.getWalletAddress(wallet, changeInputdata.getAddressString(), m_networkType);
        }
        return null;
    }
    





    //Parameters
    
    /*public static JsonObject getErgoHexErgoValueProperty(String propertyName, String base16, int index){
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
    }*/


    public class OutputData{
        private String m_ergoTreeHex = null;
        private String m_ergoScript = null;
        private JsonArray m_compileConstantsArray = null;
        private JsonArray m_parametersArray = null;
        private JsonArray m_registersArray = null;

        private long m_nanoErgs = 0;
        private ErgoToken[] m_ergoTokens = null;
        private ErgoValue<?>[] m_registers = null;
        private boolean m_isLog = true;

        private Constants m_compileConstants = null;



        private TreeHelper m_treeHelper = null;


   
        public OutputData(JsonObject outputDataObject) throws Exception{
            JsonElement ergoTreeElement = outputDataObject.get("ergoTree");
            JsonElement contractElement = outputDataObject.get("description");
            JsonElement compileConstantsElement = outputDataObject.get("compileConstants");
            JsonElement registersElement = outputDataObject.get("registers");
    

            if(ergoTreeElement != null && ergoTreeElement.isJsonNull()){
                throw new Exception("Contract ergoTree is invalid type");
            }

            m_ergoTreeHex = ergoTreeElement != null ? ergoTreeElement.getAsString() : null;


            if(contractElement != null && contractElement.isJsonNull()){
                throw new Exception("Contract description is invalid type");
            }

             m_ergoScript = contractElement != null ? contractElement.getAsString() : null;
            


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

            if(registersElement != null && (registersElement.isJsonNull() || !registersElement.isJsonArray())){
                throw new Exception("Registers element is not a valid JsonArray");
            }

            JsonArray registersArray = registersElement != null ? registersElement.getAsJsonArray() : null;
            if(registersArray != null){
                checkConstants(registersArray, m_networkType, false);
            }

            m_registersArray = registersArray;

            m_ergoTokens = NoteConstants.getTokensFromDataObject(outputDataObject);

            
            m_nanoErgs = NoteConstants.getErgAmountFromDataObject(outputDataObject);

            if(m_ergoTreeHex == null && m_ergoScript == null){
                throw new Exception("No contract details provided for output");
            }
        }

  
        public String getErgoScript(){
            return m_ergoScript;
        }
        public JsonArray getCompileConstantsArray(){
            return m_compileConstantsArray;
        }
  
        public JsonArray getParametersArray(){
            return m_parametersArray;
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

        public JsonArray executeNoteTypes(JsonArray constantsArray, Wallet wallet)throws Exception{
            int size = constantsArray.size();
            JsonArray checkedParamsArray = new JsonArray();

            for(int i = 0; i < size ; i++){
                JsonElement element = constantsArray.get(i);

                JsonObject json = element.getAsJsonObject();
                JsonElement valueElement = json.get("value");
                JsonElement typeElement = json.get("type");
                JsonElement indexElement = json.get("index");
                JsonElement nameElement = json.get("name");
                String type = typeElement.getAsString();


                switch(type){
                    case NoteConstants.WALLET_ADDRESS_PK:
                        if(AddressData.getWalletAddress(wallet, valueElement.getAsString(), m_networkType) == null){
                            throw new Exception("Address: " +valueElement.getAsString() + " is not in wallet");
                        }
                        checkedParamsArray.add(NoteConstants.replaceConstantObject(indexElement, nameElement, valueElement, ParamTypes.ADDRESS_PUBLIC_KEY));
                    break;
                    case NoteConstants.WALLET_ADDRESS_PROP_BYTES:
                        if(AddressData.getWalletAddress(wallet, valueElement.getAsString(), m_networkType) == null){
                            throw new Exception("Address: " +valueElement.getAsString() + " is not in wallet");
                        }
                        checkedParamsArray.add(NoteConstants.replaceConstantObject(indexElement, nameElement, valueElement, ParamTypes.ADDRESS_PROP_BYTES));
                    break;
                    default:
                        checkedParamsArray.add(json);
                }
              
            }
            return checkedParamsArray;
        }



        public void prepareContract(Wallet wallet) throws Exception{
         
            if(m_registersArray != null){
                m_registers = NoteConstants.parseConstantsToErgoValue(executeNoteTypes(m_registersArray, wallet));
            }
        
       
            if(isCompileContract()){

                m_compileConstants = NoteConstants.parseConstants(executeNoteTypes(m_compileConstantsArray, wallet));
                
            }else if(m_parametersArray != null && isErgoTreeHex()){

                try {
                    Files.writeString(NoteConstants.logFile.toPath(), "creating helper\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
    
                }
                m_parametersArray = executeNoteTypes(m_parametersArray, wallet);

                m_treeHelper = m_parametersArray != null ? new TreeHelper(m_ergoTreeHex, m_parametersArray, m_networkType ) : null;
                if(m_treeHelper != null && isLog()){
        
                    Utils.logJson("OutBox ErgoTree", m_treeHelper.getErgoTreeDetailsJson());
                }
        
            }      
            

    
        }

        public Values.ErgoTree getErgoTreeFromHex(){
            return ErgoTreeSerializer.DefaultSerializer().deserializeErgoTree(Base16.decode(m_ergoTreeHex).get());
        }

        public boolean isLog(){
            return m_isLog;
        }

        public OutBox getOutBox(BlockchainContext ctx, OutBoxBuilder newBoxBuilder){
            try {
                Files.writeString(NoteConstants.logFile.toPath(), "creating contract\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
            ErgoContract contract = m_treeHelper != null ? ctx.newContract((m_treeHelper.getErgoTree())) : null;

            try {
                Files.writeString(NoteConstants.logFile.toPath(), "compiling contract\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
            contract = contract == null && isCompileContract() ? ctx.compileContract(m_compileConstants, m_ergoScript) : ( isErgoTreeHex() ? ctx.newContract(getErgoTreeFromHex()) : null); 

            if(contract != null){
                try {
                    try {
                        Files.writeString(NoteConstants.logFile.toPath(), "got contract\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e1) {
        
                    }
        
                    TreeHelper treeLogger = new TreeHelper(contract.getErgoTree());
                    Utils.logJson("outBoxTree", treeLogger.getErgoTreeDetailsJson());

                } catch (Exception e) {
                    try {
                        Files.writeString(NoteConstants.logFile.toPath(), e.toString() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e1) {

                    }
        
                }

                newBoxBuilder = newBoxBuilder
                .value(m_nanoErgs)      
                .contract(contract);

                newBoxBuilder =  m_ergoTokens != null && m_ergoTokens.length > 0 ? newBoxBuilder.tokens(m_ergoTokens) : newBoxBuilder;
                newBoxBuilder = m_registers != null && m_registers.length > 0 ?  newBoxBuilder.registers(m_registers) : newBoxBuilder;

                try {
                    Files.writeString(NoteConstants.logFile.toPath(), "built outbox\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }


                try {
                    Files.writeString(NoteConstants.logFile.toPath(), "Creating outBox\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                
        
                }

            

                return newBoxBuilder.build();
            }

            return null;
        }
   

      

    
    }
}
