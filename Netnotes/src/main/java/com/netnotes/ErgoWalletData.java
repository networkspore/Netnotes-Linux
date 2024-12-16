package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ErgoWalletData extends Network implements NoteInterface {

 
    private File m_walletFile = null;
      
    private ArrayList<String>  m_authorizedIds = new ArrayList<>();

    private AddressesData m_addressesData = null;

    private NetworkType m_networkType = NetworkType.MAINNET;

    private ErgoWalletDataList m_ergoWalletsDataList;
    private String m_configId;

  
    // private ErgoWallet m_ergoWallet;
    public ErgoWalletData(String id, String configId, String name, File walletFile, NetworkType networkType, ErgoWalletDataList ergoWalletDataList) {
        super(null, name, id, ergoWalletDataList.getErgoNetwork().getNetworksData());

        m_walletFile = walletFile;
        m_networkType = networkType;
        m_ergoWalletsDataList = ergoWalletDataList;
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

    private void getAccessId(JsonObject note, String locationString, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        
        int lblCol = 180;
        String authorizeString = "Authorize Wallet Access";
        String title = getName() + " - " + authorizeString;

        Stage passwordStage = new Stage();
        passwordStage.getIcons().add(App.logo);
        passwordStage.initStyle(StageStyle.UNDECORATED);
        passwordStage.setTitle(title);

        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(App.icon, title, closeBtn, passwordStage);

        ImageView btnImageView = new ImageView(App.logo);
        btnImageView.setPreserveRatio(true);
        btnImageView.setFitHeight(75);

        Label textField = new Label(authorizeString);
        textField.setFont(App.mainFont);
        textField.setPadding(new Insets(20,0,20,15));

        VBox imageBox = new VBox(btnImageView, textField);
        imageBox.setAlignment(Pos.CENTER);
        imageBox.setPadding(new Insets(10,0,10,0));

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("location", locationString);
        paramsObject.addProperty("wallet", getName());
        paramsObject.addProperty("timeStamp", System.currentTimeMillis());
        
        JsonParametersBox walletInformationBox = new JsonParametersBox(paramsObject, lblCol);
        HBox.setHgrow(walletInformationBox, Priority.ALWAYS);

        Text passwordTxt = new Text("Enter password:");
        passwordTxt.setFill(App.txtColor);
        passwordTxt.setFont(App.txtFont);

        PasswordField passwordField = new PasswordField();
        passwordField.setFont(App.txtFont);
        passwordField.setId("passField");
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        HBox passwordBox = new HBox(passwordTxt, passwordField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        passwordBox.setPadding(new Insets(10, 10, 10, 10));

        VBox bodyBox = new VBox(walletInformationBox);
        HBox.setHgrow(bodyBox,Priority.ALWAYS);
        bodyBox.setPadding(new Insets(0,15, 0,0));

        VBox layoutVBox = new VBox(titleBox, imageBox, bodyBox, passwordBox);
        VBox.setVgrow(layoutVBox, Priority.ALWAYS);
    

        double defaultHeight = App.STAGE_HEIGHT + 60;
        double defaultWidth = App.STAGE_WIDTH + 100;

        Scene passwordScene = new Scene(layoutVBox, defaultWidth , defaultHeight);
        passwordScene.setFill(null);
        passwordScene.getStylesheets().add("/css/startWindow.css");
        passwordStage.setScene(passwordScene);
        
   

        passwordField.setOnAction(action -> {

            String pass = passwordField.getText();    
            passwordField.setText("");

            try {

                Wallet wallet = Wallet.load(m_walletFile.toPath(), pass);

                if(m_addressesData == null){
                    ArrayList<AddressData> addressDataList = new ArrayList<>();
                    wallet.myAddresses.forEach((index, name) -> {

                        try {

                            Address address = wallet.publicAddress(m_networkType, index);
                            AddressData addressData = new AddressData(name, index, address.toString(), m_networkType, ErgoWalletData.this);
                            addressDataList.add(addressData);
                        } catch (Failure e) {
                            try {
                                Files.writeString(App.logFile.toPath(), "AddressesData - address failure: " + e.toString() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e1) {

                            }
                        }
                        

                    });
                 
                    m_addressesData = new AddressesData(getNetworkId(), addressDataList, ErgoWalletData.this, m_networkType);
                }

                String id = FriendlyId.createFriendlyId();
                m_authorizedIds.add(id);
                
                JsonObject json = new JsonObject();
                json.addProperty("accessId", id);
                json.addProperty("networkId", getNetworkId());
            
                
                Utils.returnObject(json, getExecService(), onSucceeded, onFailed);
                passwordStage.close();

            } catch (Exception e) {
                
                if(e instanceof NoSuchFileException){

                    Alert noFileAlert = new Alert(AlertType.ERROR, "Wallet file does not exist, or has been moved.", ButtonType.OK);
                    noFileAlert.setHeaderText("Error");
                    noFileAlert.setTitle("Error: File not found");
                    noFileAlert.show();

                    Utils.returnObject(Utils.getMsgObject(App.ERROR, "Authorization cancelled"), getExecService(), onSucceeded, onFailed);
                
                    passwordStage.close();
                 
                }

            }
        
        });

        
        closeBtn.setOnAction(e -> {
            
            Utils.returnObject(Utils.getMsgObject(App.ERROR, "Authorization cancelled"), getExecService(), onSucceeded, onFailed);
                
            
            passwordStage.close();

        });

        passwordStage.setOnCloseRequest(e->{
        
            Utils.returnObject(Utils.getMsgObject(App.ERROR, "Authorization cancelled"), getExecService(), onSucceeded, onFailed);

            passwordStage.close();
        });

        passwordScene.focusOwnerProperty().addListener((obs, oldval, newVal) -> {
            if (newVal != null && !(newVal instanceof PasswordField)) {
                Platform.runLater(() -> passwordField.requestFocus());
            }
        });
        passwordStage.show();

        Platform.runLater(()->passwordField.requestFocus());
        
        ResizeHelper.addResizeListener(passwordStage, defaultWidth, defaultHeight, Double.MAX_VALUE, defaultHeight);
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

    
  


    public boolean isLocationAuthorized(String locationString){
        return m_ergoWalletsDataList.getErgoWallets().getErgoNetworkData().isLocationAuthorized(locationString);
    }

    
    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        // getAddresses
        JsonElement cmdElement = note.get(App.CMD);
        JsonElement locationIdElement = note.get("locationId");

       String locationId = locationIdElement != null && !locationIdElement.isJsonNull() ? locationIdElement.getAsString() : null;
        String cmd = cmdElement != null ? cmdElement.getAsString() : null;
        String locationString = getNetworksData().getLocationString(locationId);


        if(cmd != null && isLocationAuthorized(locationString)){
                
            switch(cmd){
                case "getAccessId":
                    getAccessId(note, locationString, onSucceeded, onFailed);
                    return true;
               
                case "getConfigId":
                    return getConfigId(note, locationString, onSucceeded, onFailed);
                
                default:
                    JsonElement accessIdElement = note.get("accessId");
                    String accessId = accessIdElement != null && accessIdElement.isJsonPrimitive() ? accessIdElement.getAsString() : null;
       
                    if(accessId != null &&  m_authorizedIds.contains(accessId) && m_addressesData != null){
                        switch(cmd){
                            case "sendAssets":
                                //m_addressesData.sendAssets(note,locationString, onSucceeded, onFailed);
                                m_addressesData.executeContract(note, locationString, onSucceeded, onFailed);
                                return true;
                        }       
                    }
            }

        }
        
        return false;
    }


    public ErgoNetworkData getErgoNetworkData(){
        return m_ergoWalletsDataList.getErgoWallets().getErgoNetworkData();
    }

    @Override
    public Object sendNote(JsonObject note){

        JsonElement cmdElement = note.get(App.CMD);
        JsonElement accessIdElement = note.get("accessId");
        JsonElement locationIdElement = note.get("locationId");
        String cmd = cmdElement != null && !cmdElement.isJsonNull() && cmdElement.isJsonPrimitive() ? cmdElement.getAsString() : null;
        String locationId = locationIdElement != null && !locationIdElement.isJsonNull() && locationIdElement.isJsonPrimitive() ? locationIdElement.getAsString() : null;
     

        if (cmd != null && locationId != null) {
            String locationString = getNetworksData().getLocationString(locationId);

            if(isLocationAuthorized(locationString)){
                String accessId = accessIdElement != null && accessIdElement.isJsonPrimitive() ? accessIdElement.getAsString() : null;

                if(accessId != null &&  m_authorizedIds.contains(accessId) && m_addressesData != null){
                    switch(cmd){
                        case "getAddresses":
                            return m_addressesData.getAddressesJson();
                        case "getBalance":
                            return getBalance(note);
        
                    }
                }else{
                    switch (cmd) {
                        case "isOpen":
                            return isOpen();
                        case "getWallet":
                            return getWallet();
            
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

            public TabInterface getTab(Stage appStage, SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject,  Button networkBtn){
                return ErgoWalletData.this.getTab(appStage, heightObject, widthObject, networkBtn);
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
