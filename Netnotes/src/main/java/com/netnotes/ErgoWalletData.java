package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import org.ergoplatform.appkit.NetworkType;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.satergo.Wallet;
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
    private AddressesData m_addressesData = null;

    private NetworkType m_networkType = NetworkType.MAINNET;

    private ErgoWallets m_ergoWallet;
    private String m_configId;

    public String getType(){
        return "DATA";
    }

  
    // private ErgoWallet m_ergoWallet;
    public ErgoWalletData(String id, String configId, String name, File walletFile, NetworkType networkType, ErgoWallets ergoWallet) {
        super(null, name, id, ergoWallet);

        m_walletFile = walletFile;
        m_networkType = networkType;
        m_ergoWallet = ergoWallet;
        
        m_configId = configId;
    }   


    @Override
    public void stop(){
        m_addressesData.shutdown();
        m_addressesData = null;
    }
    

    public ErgoWallets getErgoWallets(){
        return m_ergoWallet;
    }


    public Image getSmallAppIcon(){
        return m_ergoWallet.getSmallAppIcon();
    }



    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
       // getAddresses
       JsonElement subjectElement = note.get("subject");
       JsonElement networkElement = note.get("networkId");

       if (subjectElement != null && networkElement != null) {
           
            switch(subjectElement.getAsString()){
                case "getConfigId":
                    getConfigId(onSucceeded, onFailed);
                    return true;
                default:
                    Object obj = sendNote(note);

                    Utils.returnObject(obj, m_ergoWallet.getNetworksData().getExecService(), onSucceeded, onFailed);
                    return obj != null;
            }
           
       }
       return false;
    }



    @Override
    public void addMsgListener(NoteMsgInterface note){
        if(m_authorizedIds.contains(note.getId())){
            super.addMsgListener(note);
        }
    }

    @Override
    public boolean removeMsgListener(NoteMsgInterface note){
        if(note != null){
            m_authorizedIds.remove(note.getId());
        }
        return super.removeMsgListener(note);
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
    };

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
                    setName(name);
                    updated();
                    return Utils.getMsgObject(App.UPDATED, "Saved");
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
        return m_ergoWallet.getNetworksData().getExecService();
    }


    private void getConfigId(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
    
        getNetworksData().verifyAppKey(()->{
            Utils.returnObject(getConfigObject(),getExecService(), onSucceeded, onFailed);
        });
        
    }

    private JsonObject getAccessId(JsonObject note){
        JsonElement passwordElement = note.get("password");
     

        String pass = passwordElement != null && passwordElement.isJsonPrimitive() ? passwordElement.getAsString() : null;

        if(passwordElement != null && passwordElement.isJsonPrimitive() && pass != null){

           
            try {
                Wallet wallet = Wallet.load(m_walletFile.toPath(), pass);
                m_addressesData = m_addressesData == null ? new AddressesData(getNetworkId(), wallet, this, m_networkType) : m_addressesData;
                String id = FriendlyId.createFriendlyId();

                m_authorizedIds.add(id);
                
                JsonObject json = new JsonObject();
                json.addProperty("accessId", id);

                return json;
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
    public Object sendNote(JsonObject note){

        JsonElement subjectElement = note.get("subject");
        JsonElement networkIdElement = note.get("networkId");
        JsonElement accessIdElement = note.get("accessId");

        if (subjectElement != null && subjectElement.isJsonPrimitive() && networkIdElement != null && networkIdElement.isJsonPrimitive()) {
            String networkId = networkIdElement.getAsString();
        
            if(networkId.equals(m_ergoWallet.getErgoNetworkData().getId())){
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
        }
        return null;
    }

    public boolean isFile(){
        return  m_walletFile != null && m_walletFile.isFile();
    }

    public NoteInterface getNoteInterface(){
        ErgoWalletData thisData = this;
        return new NoteInterface() {
            
            public String getName(){
                return thisData.getName();
            }

            public String getNetworkId(){
                return thisData.getNetworkId();
            }

            public Image getAppIcon(){
                return thisData.getAppIcon();
            }

            public Image getSmallAppIcon(){
                return thisData.getSmallAppIcon();
            }

            public SimpleObjectProperty<LocalDateTime> getLastUpdated(){
                return null;
            }

            public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
                return thisData.sendNote(note, onSucceeded, onFailed);
            }

            public Object sendNote(JsonObject note){
                return thisData.sendNote(note);
            }

            public JsonObject getJsonObject(){
                return getWallet();
            }

            public TabInterface getTab(Stage appStage, SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject,  Button networkBtn){
                return thisData.getTab(appStage, heightObject, widthObject, networkBtn);
            }

            public String getType(){
                return thisData.getType();
            }


            public NetworksData getNetworksData(){
                return null;
            }

            public NoteInterface getParentInterface(){
                return null;
            }

            public void addUpdateListener(ChangeListener<LocalDateTime> changeListener){}

            public void removeUpdateListener(){}

            public void remove(){}
            public void shutdown(){}

            public SimpleObjectProperty<LocalDateTime> shutdownNowProperty(){
                return null;
            }

            public void addMsgListener(NoteMsgInterface listener){}
            public boolean removeMsgListener(NoteMsgInterface listener){
                return false;
            }

            public int getConnectionStatus(){
                return thisData.getConnectionStatus();
            }

            public void setConnectionStatus(int status){}


            public String getDescription(){
                return thisData.getDescription();
            }
        };
    }

    

    public String getWalletPath(){
        return m_walletFile.getAbsolutePath();
      
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

    public String getDescription(){
        return "data";
    }

    @Override
    protected void sendMessage(int msg){
        sendMessage(msg, System.currentTimeMillis());
    }
    @Override
    protected void sendMessage(int msg, long timestamp){

        for(int i = 0; i < msgListeners().size() ; i++){
            NoteMsgInterface msgInterface = msgListeners().get(i);
            if(msgInterface.getId() != null && m_authorizedIds.contains(msgInterface.getId())){
                msgInterface.sendMessage( msg, timestamp);
            }
        }
    }
    @Override
    protected void sendMessage(int code, long timestamp, String msg){

        for(int i = 0; i < msgListeners().size() ; i++){
            NoteMsgInterface msgInterface = msgListeners().get(i);
            if(msgInterface.getId() != null && m_authorizedIds.contains(msgInterface.getId())){
                msgInterface.sendMessage( code, timestamp, msg);
            }
        }
    }
    @Override
    protected void sendMessage(String networkId, int code, long timestamp, String msg){

        for(int i = 0; i < msgListeners().size() ; i++){
            NoteMsgInterface msgInterface = msgListeners().get(i);
            if(msgInterface.getId() != null && m_authorizedIds.contains(msgInterface.getId())){
                msgInterface.sendMessage(networkId, code, timestamp, msg);
            }
        }
    }
    

}
