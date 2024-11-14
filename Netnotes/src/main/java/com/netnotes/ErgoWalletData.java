package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.satergo.Wallet;
import com.satergo.WalletKey.Failure;
import com.utils.Utils;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ErgoWalletData extends Network implements NoteInterface {

 
    private File m_walletFile = null;
      
    private ArrayList<String>  m_authorizedIds = new ArrayList<>();
    private ArrayList<String> m_authorizedLocations = new ArrayList<>();

    private AddressesData m_addressesData = null;

    private NetworkType m_networkType = NetworkType.MAINNET;

    private ErgoWalletDataList m_ergoWalletsDataList;
    private String m_configId;
    @Override
    public String getType(){
        return "DATA";
    }

  
    // private ErgoWallet m_ergoWallet;
    public ErgoWalletData(String id, String configId, String name, File walletFile, NetworkType networkType, ErgoWalletDataList ergoWalletDataList) {
        super(null, name, id, ergoWalletDataList.getErgoNetwork().getNetworksData());

        m_walletFile = walletFile;
        m_networkType = networkType;
        m_ergoWalletsDataList = ergoWalletDataList;
        m_authorizedLocations.add(App.LOCAL);
        m_configId = configId;
    }   

    public ErgoWalletDataList getErgoWalletsDataList(){
        return m_ergoWalletsDataList;
    }

    @Override
    public void stop(){
        
        if(m_addressesData != null){
            m_addressesData.shutdown();
            m_addressesData = null;
        }
    }
    

    public ErgoWallets getErgoWallets(){
        return m_ergoWalletsDataList.getErgoWallets();
    }


    public Image getSmallAppIcon(){
        return getErgoWallets().getSmallAppIcon();
    }

    public File getWalletFile(){
        return m_walletFile;
    }


    @Override
    public void addMsgListener(NoteMsgInterface listener){
        if(listener != null && m_authorizedIds.contains(listener.getId())){
            super.addMsgListener(listener);
        }
    }

    @Override
    public boolean removeMsgListener(NoteMsgInterface listener){
        if(listener != null){
            String listenerId = listener.getId();
            boolean removed = super.removeMsgListener(listener);;
            if(removed && listenerId != null){
                m_authorizedIds.remove(listenerId);
            }
            return removed;
        }else{
            return false;
        }
        
    }

    private JsonObject updateFile(JsonObject note){

        JsonElement fileElement = note.get("file");
        JsonElement fileConfigIdElement = note.get("configId");
        String configId = fileConfigIdElement != null && fileConfigIdElement.isJsonPrimitive() ? fileConfigIdElement.getAsString() : null;
   
        if(configId != null && configId.equals(m_configId) ){
            String fileString = fileElement != null && fileElement.isJsonPrimitive() ? fileElement.getAsString() : null;
            File walletFile = fileString != null && fileString.length() > 1 ? new File(fileString) : null;
    
            if(walletFile != null && walletFile.isFile()){
                
                if(!walletFile.getAbsolutePath().equals(m_walletFile.getAbsolutePath())){
                    if( getConnectionStatus() == App.STOPPED){
                        setWalletFile(walletFile);
                
                        updated();
                        return Utils.getMsgObject(App.UPDATED, "Saved");
                    }else{
                        return Utils.getMsgObject(App.ERROR, "Error: In use");
                    }
                }else{
                    return Utils.getMsgObject(App.WARNING, "No change");
                }   
                
            }else{
                return Utils.getMsgObject(App.ERROR, "Error: Invalid");
            }
            
        }else{
            return Utils.getMsgObject(App.ERROR, "Error: Not authorized");
        }
    }

    private void setWalletFile(File walletFile){
  
        m_walletFile = walletFile;
        updated();
    }

    public File getWalleFile(){
        return m_walletFile;
      
    }

    private void updated(){
        getLastUpdated().set(LocalDateTime.now());
    }

    

    private JsonObject updateName(JsonObject note){
        JsonElement namElement = note.get("name");
        JsonElement configIdElement = note.get("configId");
  
        String name = namElement != null && namElement.isJsonPrimitive() ? namElement.getAsString() : null;
        String configId = configIdElement != null && configIdElement.isJsonPrimitive() ? configIdElement.getAsString() : null;

        

        if(configId != null && configId.equals(m_configId) ){
            if(name != null && name.length() > 1){
                if(!name.equals(getName())){
                    

                    if(!getErgoWalletsDataList().containsName(name)){
                        setName(name);
                        updated();
                        return Utils.getMsgObject(App.UPDATED, "Saved");
                    }else{
                        return Utils.getMsgObject(App.ERROR, "Name in use");
                    }
                }else{
                    return Utils.getMsgObject(App.WARNING, "No change");
                }   
                
            }else{
                return Utils.getMsgObject(App.ERROR, "Error: Invalid");
            }
            
        }else{
            return Utils.getMsgObject(App.ERROR, "Error: Not authorized");
        }
     
    }

    private JsonObject getConfigObject(){
        JsonObject json = new JsonObject();
       
        json.addProperty("configId", m_configId);
        
        return json;
    }

    private ExecutorService getExecService(){
        return getErgoWallets().getNetworksData().getExecService();
    }
    
  


    private boolean getConfigId(JsonObject note, String locationString, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){

    
        long timestamp = System.currentTimeMillis();
       

        getNetworksData().verifyAppKey("Ergo","Wallet Config", "", locationString, timestamp, ()->{
            
            Utils.returnObject(getConfigObject(), getExecService(), onSucceeded, onFailed);
        
        });
        return true;
    
        
    }

    private JsonObject getAccessId(JsonObject note){
        JsonElement passwordElement = note.get("password");
     

        String pass = passwordElement != null && passwordElement.isJsonPrimitive() ? passwordElement.getAsString() : null;

        if(passwordElement != null && passwordElement.isJsonPrimitive() && pass != null){
              
          
            try {
                Wallet wallet = Wallet.load(m_walletFile.toPath(), pass);
                
                    
                if(m_addressesData == null){
                    ArrayList<AddressData> addressDataList = new ArrayList<>();
                    wallet.myAddresses.forEach((index, name) -> {

                        try {

                            Address address = wallet.publicAddress(m_networkType, index);
                            AddressData addressData = new AddressData(name, index, address.toString(), m_networkType, this);
                            addressDataList.add(addressData);
                        } catch (Failure e) {
                            try {
                                Files.writeString(App.logFile.toPath(), "\nAddressesData - address failure: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e1) {

                            }
                        }
                       

                    });
                    addressDataList.get(0).updateBalance();
                    m_addressesData = new AddressesData(getNetworkId(), addressDataList, this, m_networkType);
                
                    String id = FriendlyId.createFriendlyId();

                    m_authorizedIds.add(id);
                    
                    JsonObject json = new JsonObject();
                    json.addProperty("accessId", id);

                    return json;
                }  
            } catch (Exception e) {
                
                try {
                    Files.writeString(App.logFile.toPath(), "\nerr" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
            }

        }

        return null;
    }



    private JsonObject getBalance(JsonObject note){

        JsonElement addressElement = note.get("address");
        String address = addressElement != null && addressElement.isJsonPrimitive() ? addressElement.getAsString() : null;
        
        AddressData addressData = address!= null ? m_addressesData.getAddress(address) : null;
            
        if(addressData != null){
           
            return addressData.getBalance();
        }

        return null;
    
        
    }
   
    public JsonObject isOpen(){
        JsonObject json = new JsonObject();
        json.addProperty("isOpen",getConnectionStatus() != 0);
        return json;
    }

    
  




    
    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        // getAddresses
        JsonElement cmdElement = note.get(App.CMD);
        JsonElement accessIdElement = note.get("accessId");
        JsonElement locationIdElement = note.get("locationId");

        String accessId = accessIdElement != null && accessIdElement.isJsonPrimitive() ? accessIdElement.getAsString() : null;
        String locationId = locationIdElement != null && !locationIdElement.isJsonNull() ? locationIdElement.getAsString() : null;
        String cmd = cmdElement != null ? cmdElement.getAsString() : null;
        String locationString = getNetworksData().getLocationString(locationId);


        if(cmd != null && locationString != null && m_authorizedLocations.contains(locationString) && accessId != null &&  m_authorizedIds.contains(accessId) && m_addressesData != null){
                
            switch(cmd){
                case "sendAssets":
                    m_addressesData.sendAssets(note,locationString, onSucceeded, onFailed);
                    return true;
                    case "getConfigId":
                    return getConfigId(note, locationString, onSucceeded, onFailed);
                
                default:

                    Object obj = sendNote(note);
                    
                    Utils.returnObject(obj, getExecService(), onSucceeded, onFailed);
                    
                    return obj != null;
            }

        }
        
        return false;
    }


    @Override
    public Object sendNote(JsonObject note){

        JsonElement subjectElement = note.get(App.CMD);
        JsonElement accessIdElement = note.get("accessId");

        if (subjectElement != null && subjectElement.isJsonPrimitive()) {

            String accessId = accessIdElement != null && accessIdElement.isJsonPrimitive() ? accessIdElement.getAsString() : null;
            String subject = subjectElement.getAsString();

            if(accessId != null &&  m_authorizedIds.contains(accessId) && m_addressesData != null){
                switch(subject){
                    case "getAddresses":
                        return m_addressesData.getAddressesJson();
                    case "getBalance":
                        return getBalance(note);
     
                }
            }else{
                switch (subject) {
                    case "isOpen":
                        return isOpen();
                    case "getWallet":
                        return getWallet();
                    case "getAccessId":
                        return getAccessId(note);
                    case "updateFile":
                        return updateFile(note);  
                    case "updateName":
                        return updateName(note);
                    case "getFileData":
                        return getFileData(note);
                    
                }
            }
        }
        return null;
    }

    public boolean isFile(){
        return  m_walletFile != null && m_walletFile.isFile();
    }
    @Override 
    public NoteInterface getNoteInterface(){
      
        return new NoteInterface() {
            
            public String getName(){
                return ErgoWalletData.this.getName();
            }

            public String getNetworkId(){
                return ErgoWalletData.this.getNetworkId();
            }

            public Image getAppIcon(){
                return ErgoWalletData.this.getAppIcon();
            }

   
            public SimpleObjectProperty<LocalDateTime> getLastUpdated(){
                return null;
            }

            public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
                return ErgoWalletData.this.sendNote(note, onSucceeded, onFailed);
            }

            public Object sendNote(JsonObject note){
                return ErgoWalletData.this.sendNote(note);
            }

            public JsonObject getJsonObject(){
                return getWallet();
            }

            public TabInterface getTab(Stage appStage, String locationId, SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject,  Button networkBtn){
                return ErgoWalletData.this.getTab(appStage, locationId, heightObject, widthObject, networkBtn);
            }

            public String getType(){
                return ErgoWalletData.this.getType();
            }


            public NetworksData getNetworksData(){
                return null;
            }

            public NoteInterface getParentInterface(){
                return null;
            }

            public void addUpdateListener(ChangeListener<LocalDateTime> changeListener){}

            public void removeUpdateListener(){}


            public void shutdown(){}

            public SimpleObjectProperty<LocalDateTime> shutdownNowProperty(){
                return null;
            }

            public void addMsgListener(NoteMsgInterface listener){
                if(listener != null && listener.getId() != null){
                    ErgoWalletData.this.addMsgListener(listener);
                }
            }
            public boolean removeMsgListener(NoteMsgInterface listener){
                
                return ErgoWalletData.this.removeMsgListener(listener);
            }

            public int getConnectionStatus(){
                return ErgoWalletData.this.getConnectionStatus();
            }

            public void setConnectionStatus(int status){}


            public String getDescription(){
                return ErgoWalletData.this.getDescription();
            }
        };
    }

    




    private HashData m_tmpHashData = null;
    private String m_tmpPath = null;

    private JsonObject getFileData(JsonObject note){
        JsonElement configIdElement = note.get("configId");
        String configId = configIdElement != null && configIdElement.isJsonPrimitive() ? configIdElement.getAsString() : null;

        if(configId != null && configId.equals(m_configId)){
            return getFileData();
        }
        return null;
    }

    private JsonObject getFileData(){

        JsonObject json = new JsonObject();
        m_tmpPath = null;

        try{
            m_tmpPath = m_walletFile != null ?  m_walletFile.getCanonicalPath() : null;
         
        }catch(IOException ioE){
           json.addProperty("error", ioE.toString());
        }

        boolean isDrive = m_tmpPath != null ? Utils.findPathPrefixInRoots(m_tmpPath) : false;  
        boolean isFile = m_tmpPath != null && isDrive &&  m_walletFile.isFile();
        json.addProperty("isFile", isFile);
        json.addProperty("isDrive", isDrive);
        json.addProperty("path", m_tmpPath != null ? m_tmpPath :( m_walletFile != null ? m_walletFile.getAbsolutePath() : null));
        json.addProperty("name", m_walletFile != null ? m_walletFile.getName() : null);
      
        m_tmpHashData = null;

        try{
            m_tmpHashData = isFile ? new HashData(m_walletFile) : null;
        }catch(IOException e){

        }

        json.add("hashData", m_tmpHashData != null ? m_tmpHashData.getJsonObject() : null);

        m_tmpPath = null;
        m_tmpHashData = null;
        return json;
    }
  
  
    @Override
    public JsonObject getJsonObject() {
        JsonObject jsonObject = getWallet();
        if(m_walletFile != null){
            jsonObject.addProperty("file", m_walletFile.getAbsolutePath());
        }
        jsonObject.add("fileData", getFileData());
        return jsonObject;
    }        

    public JsonObject getWallet() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", getName());
        jsonObject.addProperty("id", getNetworkId());
        jsonObject.addProperty("networkType", m_networkType.toString());
     
       return jsonObject;
   }   
   @Override
    public String getDescription(){
        return "data";
    }



}
