package com.netnotes;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.utils.Utils;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;


public class ErgoWalletControl {

    private String m_locationId;
    private String m_accessId = null;
    private Future<?> m_accessIdFuture = null;

    private NoteInterface m_ergoNetworkInterface;
    private NoteInterface m_walletInterface = null;

    private SimpleStringProperty m_currentAddress = new SimpleStringProperty(null);
    private SimpleObjectProperty<JsonObject> m_balanceObject = new SimpleObjectProperty<>(null);
    private SimpleStringProperty m_walletName = new SimpleStringProperty(null);

    private NoteMsgInterface m_walletMsgInterface = null;

   
    public ErgoWalletControl(String locationId, NoteInterface ergoNetworkInterface){
        m_locationId = locationId;
        m_ergoNetworkInterface = ergoNetworkInterface;
    }

    private String getLocationId(){
        return m_locationId;
    }

    public SimpleStringProperty walletNameProperty(){
        return m_walletName;
    }

    public SimpleStringProperty currentAddressProperty(){
        return m_currentAddress;
    }

    public SimpleObjectProperty<JsonObject> balanceProperty(){
        return m_balanceObject;
    }

    public String getCurrentAddress(){
        return m_currentAddress.get();
    }

    public String getWalletName(){
        return m_walletName.get();
    }

    public Future<?> executeSimpleTransaction(long amountToSpendNanoErgs, long feeNanoErgs, PriceAmount[] tokens, JsonObject outputData, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        JsonObject txData = new JsonObject();
        String currentAddressString = getCurrentAddress();
        String walletName = getWalletName();

        if(currentAddressString == null || walletName == null){
            return Utils.returnException("Wallet is locked", execService, onFailed);
        }

           JsonObject balanceObject = m_balanceObject.get();
        
        if(balanceObject == null){
            return Utils.returnException("Balance is unvailable", execService, onFailed);
        }

        ArrayList<PriceAmount> balanceList = AddressesData.getBalanceList(balanceObject, true, getNetworkType());

        PriceAmount ergoBalance = AddressesData.getPriceAmountFromList(balanceList, ErgoCurrency.TOKEN_ID);

        if(ergoBalance == null){
            return Utils.returnException("Ergo balance unavailable", execService, onFailed);
        }

        if(ergoBalance.getLongAmount() < (amountToSpendNanoErgs + feeNanoErgs + ErgoDexFees.getNanoErgsFromErgs(ErgoNetwork.MIN_NETWORK_FEE))){
            return Utils.returnException("Insufficient ergo with network fee ("+ErgoNetwork.MIN_NETWORK_FEE+") and token housing ("+ErgoNetwork.MIN_NETWORK_FEE+")", execService, onFailed);
        }


        if(tokens.length > 0){
            for(PriceAmount tokenAmount : tokens){
                PriceAmount tokenBalance = AddressesData.getPriceAmountFromList(balanceList, ErgoCurrency.TOKEN_ID);
                if(tokenBalance == null){
                    return Utils.returnException(tokenAmount.getSymbol() + " not available in wallet.", execService, onFailed);
                }
                if(tokenAmount.getLongAmount() > tokenBalance.getLongAmount()){
                    return Utils.returnException("Insufficient " + tokenAmount.getSymbol() + " in wallet (Balance: "+ tokenBalance.getBigDecimalAmount() +")", execService, onFailed);
                }
            }
        }

        ErgoInputData inputData = new ErgoInputData(walletName, ErgoTransactionData.CURRENT_WALLET_FILE, currentAddressString, amountToSpendNanoErgs, ErgoInputData.convertPriceAmountsToErgoTokens(tokens), feeNanoErgs, ErgoInputData.ASSETS_INPUT, ErgoInputData.FEE_INPUT, ErgoInputData.CHANGE_INPUT);

        ErgoTransactionData.addSingleInputToDataObject(inputData, txData);

      
        if(outputData == null){
            return Utils.returnException("No ouptut data provided", execService, onFailed);
        }
        JsonArray outputs = new JsonArray();
        outputs.add(outputData);
 
        ErgoTransactionData.addNetworkTypeToDataObject(getNetworkType(), txData);

        txData.add("outputs", outputs);

        return sendNoteData("executeTransaction", txData, execService, onSucceeded, onFailed);
    }

    public Future<?> sendNoteData(String cmd, JsonObject noteData, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        if(m_accessId == null){
            return Utils.returnException("No access to wallet", execService, onFailed);
        }

        if(m_locationId == null){
            return Utils.returnException("Location is not initialized", execService, onFailed);
        }

        JsonObject note = Utils.getCmdObject(cmd);
        note.addProperty("accessId", m_accessId);
        note.addProperty("locationId", m_locationId);
        note.add("data", noteData);

        NoteInterface walletInterface = m_walletInterface;
        if(walletInterface != null){
            return walletInterface.sendNote(note, onSucceeded, onFailed);
        }else{
            return Utils.returnException("Wallet unavilable", execService, onFailed);
        }
   }



   

    

    public void connectToWallet(){
        NoteInterface walletInterface = m_walletInterface;
        if(walletInterface != null && m_accessIdFuture == null || (m_accessIdFuture != null && m_accessIdFuture.isDone())){
            JsonObject getWalletObject = Utils.getCmdObject("getAccessId");
            getWalletObject.addProperty("locationId", getLocationId());

            m_accessIdFuture = walletInterface.sendNote(getWalletObject, onSucceeded->{
                Object successObject = onSucceeded.getSource().getValue();

                if (successObject != null) {
                    JsonObject json = (JsonObject) successObject;
                    // addressesDataObject.set(json);
                    JsonElement codeElement = json.get("code");
                    JsonElement accessIdElement = json.get("accessId");


                    if(accessIdElement != null && codeElement == null){
                    
                        m_accessId = accessIdElement.getAsString();
                        

                        m_walletMsgInterface = new NoteMsgInterface() {

                            public String getId() {
                                return m_accessId;
                            }
                            @Override
                            public void sendMessage(int code, long timestamp,String networkId, Number num) {
                            }

                            public void sendMessage(int code, long timestamp,String networkId, String msg) {
                            
                                String currentAddress = m_currentAddress.get();
                                    
                                switch (code) {
                                    case App.UPDATED:
                                        if (networkId != null && currentAddress != null && networkId.equals(currentAddress)) {
                                            updateBalance();
                                        }
                                        break;
                            
                                }
                            }
                        };
                       
                        walletInterface.addMsgListener(m_walletMsgInterface);
                        openWallet();
                    }
                } 
            }, onFailed->{});
        }
    }



    private void openWallet(){
    
        JsonArray addressesArray = getAddresses();
        JsonElement addressJsonElement = addressesArray != null ? addressesArray.get(0) : null;
        JsonObject adr0 = addressJsonElement != null && !addressJsonElement.isJsonNull() && addressJsonElement.isJsonObject() ? addressJsonElement.getAsJsonObject() : null;
        JsonElement addressElement = adr0 != null ? adr0.get("address") : null;
        String address = addressElement != null ? addressElement.getAsString() : null;

        if(address != null){
            m_currentAddress.set(address);
            updateBalance();
        }else{
            disconnectWallet();
        }
    
    }

    public JsonArray getWallets(){
        NoteInterface ergoNetworkInterface = m_ergoNetworkInterface;
        if(ergoNetworkInterface != null){
            JsonObject note = Utils.getCmdObject("getWallets");
            note.addProperty("networkId", ErgoNetwork.WALLET_NETWORK);
            note.addProperty("locationId", getLocationId());

            Object obj = ergoNetworkInterface.sendNote(note);
            if(obj != null && obj instanceof JsonArray){
                return (JsonArray) obj;
            }
        }
        return null;
    }

    public JsonArray getAddresses(){
        NoteInterface walletInterface = m_walletInterface;
        String accessId = m_accessId;
        if(accessId != null &&  walletInterface != null){
            JsonObject json = Utils.getCmdObject("getAddresses");
            json.addProperty("accessId", accessId);
            json.addProperty("locationId", getLocationId());

            Object successObject = walletInterface.sendNote(json);

            if (successObject != null && successObject instanceof JsonArray) {
                JsonArray jsonArray = (JsonArray) successObject;
                if (jsonArray.size() > 0) {
                    return jsonArray;
                }
            }
        }
        return null;
    }
    
    public void updateBalance(){
        String accessId = m_accessId;
        String address = m_currentAddress.get();
        NoteInterface walletInterface = m_walletInterface;
        if(accessId != null && address != null && walletInterface != null){
            
            JsonObject note = Utils.getCmdObject("getBalance");
            note.addProperty("locationId", getLocationId());
            note.addProperty("accessId", accessId);
            note.addProperty("address", address);
            Object obj = walletInterface.sendNote(note);
            
            JsonObject balanceObject = obj != null && obj instanceof JsonObject ? (JsonObject) obj : null;
            
            m_balanceObject.set(balanceObject);
        }else{
            m_balanceObject.set(null);
        }
    }

    public NetworkType getNetworkType(){
        String accessId = m_accessId;
        NoteInterface walletInterface = m_walletInterface;

        JsonObject note = Utils.getCmdObject("getNetworkType");
        note.addProperty("locationId", getLocationId());
        note.addProperty("accessId", accessId);
        Object obj = walletInterface.sendNote(note);

        if(obj != null && obj instanceof NetworkType){
            return (NetworkType) obj;
        }
        return null;
    }

   
    
    public void getDefaultWallet(){
        NoteInterface ergoNetworkInterface = m_ergoNetworkInterface;
         
        if(ergoNetworkInterface != null){
            JsonObject note = Utils.getCmdObject("getDefaultInterface");
            note.addProperty("networkId", ErgoNetwork.WALLET_NETWORK);
            note.addProperty("locationId", getLocationId());
           
          
            Object obj = ergoNetworkInterface.sendNote(note);
            NoteInterface walletInterface = obj != null && obj instanceof NoteInterface ? (NoteInterface) obj : null;
            setWalletInterface(walletInterface);
        }else{
            setWalletInterface((NoteInterface) null);
        }
        
    }

    private void setWalletInterface(NoteInterface walletInterface){
        if(m_walletInterface != null){
            disconnectWallet();
        }
        m_walletInterface = walletInterface;
        m_walletName.set(m_walletInterface != null ? m_walletInterface.getName() : null);


    }


    public void setWalletInterface(String id){
        NoteInterface ergoNetworkInterface = m_ergoNetworkInterface;

        if(ergoNetworkInterface != null){
            JsonObject note = Utils.getCmdObject("getWalletInterface");
            note.addProperty("networkId", ErgoNetwork.WALLET_NETWORK);
            note.addProperty("locationId", getLocationId());
            note.addProperty("id", id);
            Object obj = ergoNetworkInterface.sendNote(note);
            NoteInterface walletInterface = obj != null && obj instanceof NoteInterface ? (NoteInterface) obj : null;

            setWalletInterface(walletInterface);
        }else{
            setWalletInterface((NoteInterface) null);
        }
        
    }

 

    public void walletRemoved(String msg){
        String walletId = m_walletInterface != null ? m_walletInterface.getNetworkId() : null;
        if(walletId != null){
            JsonParser jsonParser = msg != null ? new JsonParser() : null;
            JsonElement jsonElement = jsonParser != null ? jsonParser.parse(msg) : null;
            JsonObject json = jsonElement != null && !jsonElement.isJsonNull() && jsonElement.isJsonObject() ? jsonElement.getAsJsonObject() : null;
            JsonElement idsElement = json != null ? json.get("ids") : null;
            JsonArray idsArray = idsElement != null && !idsElement.isJsonNull() && idsElement.isJsonArray() ? idsElement.getAsJsonArray() : null;
            
            if(idsArray != null){
                int idsArraySize = idsArray.size();
                
                for(int i = 0; i < idsArraySize ; i++){
                    JsonElement idElement = idsArray.get(i);

                    String id = idElement != null && !idElement.isJsonNull() && idElement.isJsonPrimitive() ? idElement.getAsString() : null;

                    if(id != null && id.equals(walletId)){
                        clearWallet();
                        return;
                    }
                }

            }
        }
    }

    public void setErgoNetworkInterface(NoteInterface ergoNetworkInterface){
        clearWallet();
        m_ergoNetworkInterface = ergoNetworkInterface;
        if(ergoNetworkInterface != null){
            getDefaultWallet();
        }
    }

    public void clearWallet(){
        setWalletInterface((NoteInterface) null);
    }
    

    public void disconnectWallet(){
        if(m_accessIdFuture != null && !m_accessIdFuture.isDone() || !m_accessIdFuture.isCancelled()){
            m_accessIdFuture.cancel(true);
        }
        if(m_walletInterface != null && m_walletMsgInterface != null){
            m_walletInterface.removeMsgListener(m_walletMsgInterface);
        }      

        m_currentAddress.set(null);
        m_balanceObject.set(null);
        m_accessId = null;
    }

    public boolean isUnlocked(){
        return m_accessId != null;
    }

    public void shutdown(){
        clearWallet();
    }

   
}
