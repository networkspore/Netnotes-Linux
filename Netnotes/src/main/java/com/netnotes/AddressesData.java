package com.netnotes;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.satergo.Wallet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netnotes.ErgoTransactionData.OutputData;
import io.netnotes.engine.networks.ergo.AddressInformation;
import io.netnotes.engine.networks.ergo.ErgoInputData;
import io.netnotes.engine.networks.ergo.ErgoNetworkUrl;
import io.netnotes.engine.JsonParametersBox;
import io.netnotes.engine.NamedNodeUrl;
import io.netnotes.engine.NetworkInformation;
import io.netnotes.engine.NoteConstants;
import io.netnotes.engine.NoteInterface;
import io.netnotes.engine.NoteMsgInterface;
import io.netnotes.engine.PriceAmount;
import io.netnotes.engine.PriceQuote;
import io.netnotes.engine.ResizeHelper;
import io.netnotes.engine.Stages;
import io.netnotes.engine.Utils;
import io.netnotes.friendly_id.FriendlyId;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.ergoplatform.appkit.*;
import org.ergoplatform.sdk.ErgoToken;

public class AddressesData {

   
    
    public final static NetworkInformation NOMARKET = new NetworkInformation("null", "(disabled)","/assets/bar-chart-150.png", "/assets/bar-chart-30.png", "No market selected" );
    public final static NetworkInformation[] ERGO_MARKETS = new NetworkInformation[]{
        NOMARKET,
        ErgoDex.getNetworkInformation(), 
        KucoinExchange.getNetworkInformation()
    };
    public final static NetworkInformation[] ERGO_TOKEN_MARKETS= new NetworkInformation[]{ 
        NOMARKET,
        ErgoDex.getNetworkInformation() 
    }; 

    private final NetworkType m_networkType;

    private ErgoWalletData m_walletData;


    private ArrayList<AddressData> m_addressDataList;


    private SimpleObjectProperty<NoteInterface> m_selectedMarket = new SimpleObjectProperty<>(null);
    private SimpleObjectProperty<NoteInterface> m_selectedTokenMarket = new SimpleObjectProperty<>(null);

    private ChangeListener<NoteInterface> m_selectedMarketChanged = null;
    private ChangeListener<NoteInterface> m_selectedTokenMarketChanged = null;
    
    //private SimpleIntegerProperty m_ergoExchangeStatus = new SimpleIntegerProperty(NoteConstants.STOPPED);
    /*private String m_statusMsg = "";
    private String m_tokenStatusMsg = "";

    private String m_ergoBaseQuery = "ERG";
    private String m_ergoBaseType = "symbol";

    private String m_ergoQuoteType = "firstSymbolContains"; 
    private String m_ergoQuoteQuery = "USD";*/
   // private SimpleObjectProperty<PriceQuote> m_currentQuote = new SimpleObjectProperty<>(null);

    public final static int ADDRESS_IMG_HEIGHT = 40;
    public final static int ADDRESS_IMG_WIDTH = 350;
 
    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>(LocalDateTime.now());

    private Stage m_promptStage = null;

    private ScheduledExecutorService m_schedualedExecutor = null;
    private ScheduledFuture<?> m_scheduledFuture = null;

    private NoteMsgInterface m_ergoNetworkMsgInterface;

    private NoteMsgInterface m_marketMsgInterface = null;
    private NoteMsgInterface m_tokenMarketMsgInterface = null;

    //private ChangeListener<BigDecimal> m_priceQuoteAmountChanged = null;
    private long m_balanceTimestamp = 0;
    private String m_walletId;

    public AddressesData(String id,  ArrayList<AddressData> addressDataList, ErgoWalletData walletData, NetworkType networkType) {

        m_walletId = id;
        m_walletData = walletData;
        m_networkType = networkType;
       
        m_addressDataList = addressDataList;
        
     
        for(AddressData addressData : m_addressDataList){
            addressData.setAddressesData(AddressesData.this);
        }

      //  m_addressDataList.get(0).updateBalance();
        
        start();
    }

    public NetworkType getNetworkType(){
        return m_networkType;
    }


    public static ArrayList<PriceAmount> getSendAssetsList(JsonObject jsonObject, NetworkType networkType){
        ArrayList<PriceAmount> assetsList = new ArrayList<>();
        for(Map.Entry<String, JsonElement> entry : jsonObject.entrySet()){
            JsonElement element = entry.getValue();
            if(!element.isJsonNull() && element.isJsonObject()){
                JsonObject assetJson = element.getAsJsonObject();
                assetsList.add(PriceAmount.getByAmountObject(assetJson, networkType));
            }
        }
        return assetsList;
    }

    public JsonObject getBalance(JsonObject note){

        JsonElement addressElement = note.get("address");
        String address = addressElement != null && addressElement.isJsonPrimitive() ? addressElement.getAsString() : null;
        
        AddressData addressData = address!= null ? getAddressData(address) : null;
            
        if(addressData != null){
           
            return addressData.getBalance();
        }

        return null;
    
        
    }


    public Future<?> getTransactionViews(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        JsonElement addressElement = note.get("address");
        String address = addressElement != null && addressElement.isJsonPrimitive() ? addressElement.getAsString() : null;
        
        AddressData addressData = address!= null ? getAddressData(address) : null;
            
        if(addressData != null){
           
            return addressData.getTransactionViews(note, onSucceeded, onFailed);
        }

        return null;
    }

    public Future<?> getTransactions(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        JsonElement addressElement = note.get("address");
        String address = addressElement != null && addressElement.isJsonPrimitive() ? addressElement.getAsString() : null;
        
        AddressData addressData = address!= null ? getAddressData(address) : null;
            
        if(addressData != null){
           
            return addressData.getTransactions(note, onSucceeded, onFailed);
        }

        return null;
    }

    public Future<?> updateAddressBoxInfo(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        JsonElement dataElement = note.get("data");
        if(dataElement != null && dataElement.isJsonObject()){
            JsonObject dataObject = dataElement.getAsJsonObject();
            JsonElement addressElement = dataObject.get("address");
            String address = addressElement != null && addressElement.isJsonPrimitive() ? addressElement.getAsString() : null;
            
            AddressData addressData = address!= null ? getAddressData(address) : null;
                
            if(addressData != null){
            
                return addressData.updateBoxInfo(dataObject, onSucceeded, onFailed);
            }
        }
        return null;
    }


    public ExecutorService getExecService(){
        return m_walletData.getNetworksData().getExecService();
    }

    public void viewWalletMnemonic(String locationString, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){

        JsonObject authObj = new JsonObject();

        authObj.addProperty("timeStamp", System.currentTimeMillis());

        String title = "Wallet - View Mnemonic - Signature Required";
    
        Stage txStage = new Stage();
        txStage.getIcons().add(Stages.logo);
        txStage.initStyle(StageStyle.UNDECORATED);
        txStage.setTitle(title);

        PasswordField passwordField = new PasswordField();
        Button closeBtn = new Button();

        Scene passwordScene = getAuthorizationScene(txStage,title,closeBtn, passwordField, authObj, locationString, rowHeight, lblCol);

        txStage.setScene(passwordScene);

        passwordField.setOnAction(e -> {
            String pass = passwordField.getText();
            passwordField.setText("");
            
            Button cancelBtn = new Button("Cancel");
            cancelBtn.setId("iconBtnSelected");
            Label progressLabel = new Label(m_walletData.isTxAvailable() ? "Opening wallet..." : "Waiting for wallet access...");
            
            Scene waitingScene = Stages.getWaitngScene(progressLabel, cancelBtn, txStage);
            txStage.setScene(waitingScene);
            txStage.centerOnScreen();

            Future<?> walletFuture = m_walletData.startTx(pass, m_walletData.getWalletFile(), getExecService(), onWalletLoaded->{
                
                cancelBtn.setDisable(true);
                cancelBtn.setId("iconBtn");
                Object walletObject = onWalletLoaded.getSource().getValue();
                if(walletObject != null && walletObject instanceof Wallet){
                    
                    Wallet wallet = (Wallet) walletObject;
                    try{
                        wallet.key().viewWalletMnemonic(pass);
                        Utils.returnObject(NoteConstants.getJsonObject("result", "success"), getExecService(), onSucceeded);

                    }catch(Exception passFailed){
                        Utils.returnException(passFailed, getExecService(), onFailed);
                    }
                
                      
                    m_walletData.endTx();      
                    txStage.close();
                }else{
                    m_walletData.endTx();
                    txStage.setScene(passwordScene);
                    txStage.centerOnScreen();
                }
            }, onLoadFailed->{
                Throwable throwable = onLoadFailed.getSource().getException();

                if(throwable != null){
                    if(!(throwable instanceof InterruptedException)){
                        m_walletData.endTx();
                    }
                    if(throwable instanceof NoSuchFileException){

                        Alert noFileAlert = new Alert(AlertType.ERROR, "Wallet file does not exist, or has been moved.", ButtonType.OK);
                        noFileAlert.setHeaderText("Error");
                        noFileAlert.setTitle("Error: File not found");
                        noFileAlert.show();
                        Utils.returnException((Exception) throwable, getExecService(), onFailed);
                        txStage.close();
                    }else{
                        txStage.setScene(passwordScene);
                        txStage.centerOnScreen();
                    }
                }else{
                    m_walletData.endTx();

                    Alert unavailableAlert = new Alert(AlertType.ERROR, "Wallet Unavailable", ButtonType.OK);
                    unavailableAlert.setHeaderText("Error");
                    unavailableAlert.setTitle("Error: Wallet Unavailable");
                    unavailableAlert.show();

                    Utils.returnException("Wallet Unavailable", getExecService(), onFailed);
                    txStage.close();
                }

            });

            cancelBtn.setOnAction(onCancel->{
                walletFuture.cancel(true);
            });
        });

        
        Runnable sendCanceledJson =()->{
      
            passwordField.setOnAction(null);
            Utils.returnException("Transaction Canceled", getExecService(), onFailed);
            txStage.close();
        };

        closeBtn.setOnAction(e -> sendCanceledJson.run());


        txStage.setOnCloseRequest(e->sendCanceledJson.run());

        passwordScene.focusOwnerProperty().addListener((obs, oldval, newVal) -> {
            if (newVal != null && !(newVal instanceof PasswordField)) {
                Platform.runLater(() -> passwordField.requestFocus());
            }
        });
        
        txStage.show();

        Platform.runLater(()->passwordField.requestFocus());
        
        ResizeHelper.addResizeListener(txStage, 400, 600, Double.MAX_VALUE, Double.MAX_VALUE);

    }

    public static Scene getAuthorizationScene(Stage txStage,  String title, Button closeBtn, PasswordField passwordField, JsonObject dataObject, String locationString, double rowHeight, int lblCol){

        JsonParametersBox parametersBox = new JsonParametersBox(dataObject, lblCol);
        parametersBox.openAll();


        Label locationLbl = new Label("Location:");
        locationLbl.setMinWidth(lblCol);
        locationLbl.setFont(Stages.txtFont);

        TextField locationField = new TextField(locationString);
        locationField.setEditable(false);
        HBox.setHgrow(locationField, Priority.ALWAYS);
        locationField.setFont(Stages.txtFont);

        HBox locationBox = new HBox(locationLbl, locationField);
        HBox.setHgrow(locationBox,Priority.ALWAYS);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        locationBox.setMinHeight(rowHeight);


        HBox titleBox = Stages.createTopBar(Stages.icon, title, closeBtn, txStage);

        ImageView btnImageView = new ImageView(Stages.logo);
        btnImageView.setPreserveRatio(true);
        btnImageView.setFitHeight(75);
        

        Label textField = new Label("Authorization Required");
        textField.setFont(Stages.mainFont);
        textField.setPadding(new Insets(20,0,20,15));
        

        VBox imageBox = new VBox(btnImageView, textField);
        imageBox.setAlignment(Pos.CENTER);
        imageBox.setPadding(new Insets(10,0,10,0));

        Text passwordTxt = new Text("Enter password:");
        passwordTxt.setFill(Stages.txtColor);
        passwordTxt.setFont(Stages.txtFont);

        passwordField.setFont(Stages.txtFont);
        passwordField.setId("passField");

        HBox.setHgrow(passwordField, Priority.ALWAYS);

        HBox passwordBox = new HBox(passwordTxt, passwordField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        passwordBox.setPadding( new Insets(5, 10, 15, 20));



        ScrollPane bodyScroll = new ScrollPane(parametersBox);


        VBox bodyBox = new VBox(locationBox, bodyScroll);
        HBox.setHgrow(bodyBox,Priority.ALWAYS);
        VBox.setVgrow(bodyBox,Priority.ALWAYS);
        bodyBox.setPadding(new Insets(0,20, 0, 20));

        Button exportBtn = new Button("🖫 Export JSON…");
        exportBtn.setOnAction(onSave->{
            ExtensionFilter txtFilter = new FileChooser.ExtensionFilter("JSON (application/json)", "*.json");
            FileChooser saveChooser = new FileChooser();
            saveChooser.setTitle("🖫 Export JSON…");
            saveChooser.getExtensionFilters().addAll(txtFilter);
            saveChooser.setSelectedExtensionFilter(txtFilter);
            File saveFile = saveChooser.showSaveDialog(txStage);
            if(saveFile != null){
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                
                try {
                    Files.writeString(saveFile.toPath(), gson.toJson(dataObject));
                } catch (IOException e1) {
                    Alert alert = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                    alert.setTitle("Error");
                    alert.setHeaderText("Error");
                    alert.initOwner(txStage);
                    alert.show();
                }
            }
        });

        HBox exportBtnBox = new HBox(exportBtn);
        exportBtnBox.setAlignment(Pos.CENTER_RIGHT);
        exportBtnBox.setPadding(new Insets(15,15,15,0));

        VBox layoutVBox = new VBox(titleBox, imageBox,bodyBox, exportBtnBox, passwordBox);
        VBox.setVgrow(layoutVBox, Priority.ALWAYS);

        bodyScroll.prefViewportWidthProperty().bind(bodyBox.widthProperty().subtract(1));
        bodyScroll.prefViewportHeightProperty().bind(bodyBox.heightProperty().subtract(10));

        parametersBox.setPrefWidth(bodyBox.widthProperty().get() -1);
        bodyScroll.prefViewportWidthProperty().addListener((obs,oldval,newval)->{
            parametersBox.setPrefWidth(newval.doubleValue()-50);
        });



        Scene passwordScene = new Scene(layoutVBox, 830, 600);
        passwordScene.setFill(null);
        passwordScene.getStylesheets().add("/css/startWindow.css");
  
        return passwordScene;
    }
    private int lblCol = 170;
    private int rowHeight = 22;

    public Future<?> sendAssets(JsonObject note, String locationString, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) throws Exception{
        
        JsonElement dataElement = note.get("data");


        if(dataElement != null && dataElement.isJsonObject())
        {
            JsonObject dataObject = dataElement.getAsJsonObject();

            JsonElement recipientElement = dataObject.get("recipient");

            JsonObject recipientObject = recipientElement != null && recipientElement.isJsonObject() ? recipientElement.getAsJsonObject() : null;

            if(recipientObject == null){
                throw new Exception("No recipient provided");
            }

            JsonElement recipientAddressElement = recipientObject.get("address");

            AddressInformation recipientAddressInfo = recipientAddressElement != null && !recipientAddressElement.isJsonNull() ? new AddressInformation(recipientAddressElement.getAsString()) : null;
            
            if(recipientAddressInfo == null){
                throw new Exception("No recipient address provided");
            }

            if(recipientAddressInfo.getAddress() == null){
                throw new Exception("Invalid recipient address");
            }

            if(!NoteConstants.checkNetworkType(dataObject, m_networkType)){
                throw new Exception("Network type must be " + m_networkType.toString());
            }

            JsonObject walletAddressObject = NoteConstants.getAddressObjectFromDataObject("senderAddress", dataObject);

            String walletAddress = NoteConstants.getAddressFromObject(walletAddressObject);

            if(walletAddress == null){
                throw new Exception("No wallet address provided");
            }

            AddressData addressData = getAddressData(walletAddress);

            if(addressData == null){
                throw new Exception("Address not found in this wallet");
            }
            ArrayList<PriceAmount> balanceList = NoteConstants.getBalanceList(addressData.getBalance(),true, m_networkType);


            long feeAmountNanoErgs = NoteConstants.getFeeAmountFromDataObject(dataObject);
            
            if(feeAmountNanoErgs == -1){
                throw new Exception("No fee provided");
            }

            if(feeAmountNanoErgs < NoteConstants.MIN_NANO_ERGS){
                throw new Exception("Minimum fee of 0.001 Erg required (1000000 nanoErg)");
            }

            long amountToSpendNanoErgs = NoteConstants.getErgAmountFromDataObject(dataObject);
          
            

            PriceAmount[] amountsArray = NoteConstants.getAmountsFromDataObject(dataObject);
            
            for(PriceAmount sendAmount : amountsArray){
                PriceAmount balanceAmount = NoteConstants.getPriceAmountFromList(balanceList, sendAmount.getTokenId().toString());
                if(sendAmount.getLongAmount() > balanceAmount.getLongAmount()){
                    throw new Exception("Insufficent " + balanceAmount.getCurrency().getName());
                }
            }
            
            
            

            NamedNodeUrl namedNodeUrl = NoteConstants.getNamedNodeUrlFromDataObject(dataObject, getDefaultNode());

            if(namedNodeUrl == null){
                throw new Exception("Node unavailable");
            }

            if(namedNodeUrl.getUrlString() == null){
                throw new Exception("No node URL provided");
            }

            String nodeUrl = namedNodeUrl.getUrlString();
            String nodeApiKey = namedNodeUrl.getApiKey();

            ErgoNetworkUrl explorerNetworkUrl = NoteConstants.getExplorerUrl(dataObject, getDefaultExplorer());

            if(explorerNetworkUrl == null){
                throw new Exception("Explorer url not provided");
            }

            String explorerUrl = explorerNetworkUrl.getUrlString();

            String title = "Wallet - Send Assets - Signature Required";
    
            Stage txStage = new Stage();
            txStage.getIcons().add(Stages.logo);
            txStage.initStyle(StageStyle.UNDECORATED);
            txStage.setTitle(title);

            PasswordField passwordField = new PasswordField();
            Button closeBtn = new Button();

            Scene passwordScene = getAuthorizationScene(txStage,title,closeBtn, passwordField, dataObject, locationString, rowHeight, lblCol);

            txStage.setScene(passwordScene);

            passwordField.setOnAction(e -> {
                String pass = passwordField.getText();
                passwordField.setText("");
                
                Button cancelBtn = new Button("Cancel");
                cancelBtn.setId("iconBtnSelected");
                Label progressLabel = new Label(m_walletData.isTxAvailable() ? "Opening wallet..." : "Waiting for wallet access...");
                
                Scene waitingScene = Stages.getWaitngScene(progressLabel, cancelBtn, txStage);
                txStage.setScene(waitingScene);
                txStage.centerOnScreen();

                Future<?> walletFuture = m_walletData.startTx(pass, m_walletData.getWalletFile(),getExecService(), onWalletLoaded->{
                    
                    cancelBtn.setDisable(true);
                    cancelBtn.setId("iconBtn");
                    Object walletObject = onWalletLoaded.getSource().getValue();
                    if(walletObject != null && walletObject instanceof Wallet){
                        progressLabel.setText("Executing transaction...");
                        addressData.sendAssets(
                            getExecService(), 
                            (Wallet) walletObject, 
                            nodeUrl, 
                            nodeApiKey, 
                            explorerUrl, 
                            m_networkType, 
                            recipientAddressInfo, 
                            amountToSpendNanoErgs, 
                            feeAmountNanoErgs, 
                            amountsArray, 
                            pass,
                            (onSent)->{
                               
                                Utils.returnObject(onSent.getSource().getValue(), getExecService(), onSucceeded);
                                txStage.close();
                                m_walletData.endTx();
                            }, 
                            (onSendFailed)->{                     
                            
                                Object sourceException = onSendFailed.getSource().getException();
                                Exception exception = sourceException instanceof Exception ? (Exception) sourceException : null;
                                if(exception != null){
                                    Utils.returnException(exception , getExecService(), onFailed);
                                }else{
                                    String msg = sourceException == null ? "Transaction terminated unexpectedly" : onSendFailed.getSource().getException().toString(); 
                                    Utils.returnException(msg, getExecService(), onFailed);
                                }
                                txStage.close();
                                m_walletData.endTx();
                            }
                        );
                    }else{
                        m_walletData.endTx();
                        txStage.setScene(passwordScene);
                        txStage.centerOnScreen();
                    }
                }, onLoadFailed->{
                    Throwable throwable = onLoadFailed.getSource().getException();

                    if(throwable != null){
                        if(!(throwable instanceof InterruptedException)){
                            m_walletData.endTx();
                        }
                        if(throwable instanceof NoSuchFileException){

                            Alert noFileAlert = new Alert(AlertType.ERROR, "Wallet file does not exist, or has been moved.", ButtonType.OK);
                            noFileAlert.setHeaderText("Error");
                            noFileAlert.setTitle("Error: File not found");
                            noFileAlert.show();
                            Utils.returnException((Exception) throwable, getExecService(), onFailed);
                            txStage.close();
                        }else{
                            txStage.setScene(passwordScene);
                            txStage.centerOnScreen();
                        }
                    }else{
                        m_walletData.endTx();

                        Alert unavailableAlert = new Alert(AlertType.ERROR, "Transaction Unavailable", ButtonType.OK);
                        unavailableAlert.setHeaderText("Error");
                        unavailableAlert.setTitle("Error: Transaction Unavailable");
                        unavailableAlert.show();

                        Utils.returnException("Transaction Unavailable", getExecService(), onFailed);
                        txStage.close();
                    }

                });

                cancelBtn.setOnAction(onCancel->{
                    walletFuture.cancel(true);
                });
            });

            
            Runnable sendCanceledJson =()->{
          
                passwordField.setOnAction(null);
                Utils.returnException("Transaction Canceled", getExecService(), onFailed);
                txStage.close();
            };

            closeBtn.setOnAction(e -> sendCanceledJson.run());


            txStage.setOnCloseRequest(e->sendCanceledJson.run());

            passwordScene.focusOwnerProperty().addListener((obs, oldval, newVal) -> {
                if (newVal != null && !(newVal instanceof PasswordField)) {
                    Platform.runLater(() -> passwordField.requestFocus());
                }
            });
            
            txStage.show();
    
            Platform.runLater(()->passwordField.requestFocus());
            
            ResizeHelper.addResizeListener(txStage, 400, 600, Double.MAX_VALUE, Double.MAX_VALUE);

            
        }
        return null;

    }



  

    public Future<?> reclaimBox(JsonObject note,String locationId, String locationString, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        JsonElement dataElement = note.get("data");

        if(dataElement != null && dataElement.isJsonObject())
        {
            JsonObject dataObject = dataElement.getAsJsonObject();

            JsonElement addressElement = dataObject.get("address");
            JsonElement parentTxIdElement = dataObject.get("txId");
            JsonElement boxIdElement = dataObject.get("boxId");

            String addressString = addressElement != null && !addressElement.isJsonNull() ? addressElement.getAsString() : null;

            if(addressString == null){
                return Utils.returnException("address required", getExecService(), onFailed);
            }

            String boxId = boxIdElement != null && !boxIdElement.isJsonNull() ? boxIdElement.getAsString() : null;
            
            if(boxId == null){
                return Utils.returnException("boxId required", getExecService(), onFailed);
            }

            String parentTxId = parentTxIdElement != null && !parentTxIdElement.isJsonNull() ? parentTxIdElement.getAsString() : null;
            if(parentTxId == null){
                return Utils.returnException("txId required", getExecService(), onFailed);
            }

            if(locationId == null){
                return Utils.returnException("locationId required", getExecService(), onFailed);
            }

            AddressData addressData = getAddressData(addressString);

            if(addressData == null){
                 return Utils.returnException("Address not found in this wallet", getExecService(), onFailed);
            }
          
            NamedNodeUrl namedNodeUrl = NoteConstants.getNamedNodeUrlFromDataObject(dataObject, getDefaultNode());

            if(namedNodeUrl == null){
                return Utils.returnException("Node unavailable", getExecService(), onFailed);
            }

            if(namedNodeUrl.getUrlString() == null){
                return Utils.returnException("No node URL provided", getExecService(), onFailed);
            }

            String nodeUrl = namedNodeUrl.getUrlString();
            String nodeApiKey = namedNodeUrl.getApiKey();

            ErgoNetworkUrl explorerNetworkUrl = NoteConstants.getExplorerUrl(dataObject, getDefaultExplorer());

            if(explorerNetworkUrl == null){
                return Utils.returnException("Explorer url not provided", getExecService(), onFailed);
            }

            String explorerUrl = explorerNetworkUrl.getUrlString();

            String title = "Wallet - Reclaim Box - Signature Required";
    
            Stage txStage = new Stage();
            txStage.getIcons().add(Stages.logo);
            txStage.initStyle(StageStyle.UNDECORATED);
            txStage.setTitle(title);

            PasswordField passwordField = new PasswordField();
            Button closeBtn = new Button();

            Scene passwordScene = getAuthorizationScene(txStage,title,closeBtn, passwordField, dataObject, locationString, rowHeight, lblCol);

            txStage.setScene(passwordScene);

            passwordField.setOnAction(e -> {
                String pass = passwordField.getText();
                passwordField.setText("");
                
                Button cancelBtn = new Button("Cancel");
                cancelBtn.setId("iconBtnSelected");
                Label progressLabel = new Label(m_walletData.isTxAvailable() ? "Opening wallet..." : "Waiting for wallet access...");
                
                Scene waitingScene = Stages.getWaitngScene(progressLabel, cancelBtn, txStage);
                txStage.setScene(waitingScene);
                txStage.centerOnScreen();

                Future<?> walletFuture = m_walletData.startTx(pass, m_walletData.getWalletFile(),getExecService(), onWalletLoaded->{
                    
                    cancelBtn.setDisable(true);
                    cancelBtn.setId("iconBtn");
                    Object walletObject = onWalletLoaded.getSource().getValue();
                    if(walletObject != null && walletObject instanceof Wallet){
                        progressLabel.setText("Executing transaction...");
                        addressData.reclaimBox(
                            (Wallet) walletObject, 
                            nodeUrl, 
                            nodeApiKey, 
                            explorerUrl, 
                            m_networkType,
                            parentTxId,
                            boxId,
                            pass,
                            locationId,
                            (onSent)->{
                                Utils.returnObject(onSent.getSource().getValue(), getExecService(), onSucceeded);
                                txStage.close();
                                m_walletData.endTx();
                            }, 
                            (onSendFailed)->{                     
                            
                                Object sourceException = onSendFailed.getSource().getException();
                                Exception exception = sourceException instanceof Exception ? (Exception) sourceException : null;
                                if(exception != null){
                                    Utils.returnException(exception , getExecService(), onFailed);
                                }else{
                                    String msg = sourceException == null ? "Transaction terminated unexpectedly" : onSendFailed.getSource().getException().toString(); 
                                    Utils.returnException(msg, getExecService(), onFailed);
                                }
                                txStage.close();
                                m_walletData.endTx();
                            }
                        );
                    }else{
                        m_walletData.endTx();
                        txStage.setScene(passwordScene);
                        txStage.centerOnScreen();
                    }
                }, onLoadFailed->{
                    Throwable throwable = onLoadFailed.getSource().getException();

                    if(throwable != null){
                        if(!(throwable instanceof InterruptedException)){
                            m_walletData.endTx();
                        }
                        if(throwable instanceof NoSuchFileException){

                            Alert noFileAlert = new Alert(AlertType.ERROR, "Wallet file does not exist, or has been moved.", ButtonType.OK);
                            noFileAlert.setHeaderText("Error");
                            noFileAlert.setTitle("Error: File not found");
                            noFileAlert.show();
                            Utils.returnException((Exception) throwable, getExecService(), onFailed);
                            txStage.close();
                        }else{
                            txStage.setScene(passwordScene);
                            txStage.centerOnScreen();
                        }
                    }else{
                        m_walletData.endTx();

                        Alert unavailableAlert = new Alert(AlertType.ERROR, "Transaction Unavailable", ButtonType.OK);
                        unavailableAlert.setHeaderText("Error");
                        unavailableAlert.setTitle("Error: Transaction Unavailable");
                        unavailableAlert.show();

                        Utils.returnException("Transaction Unavailable", getExecService(), onFailed);
                        txStage.close();
                    }

                });

                cancelBtn.setOnAction(onCancel->{
                    walletFuture.cancel(true);
                });
            });

            
            Runnable sendCanceledJson =()->{
          
                passwordField.setOnAction(null);
                Utils.returnException("Transaction Canceled", getExecService(), onFailed);
                txStage.close();
            };

            closeBtn.setOnAction(e -> sendCanceledJson.run());


            txStage.setOnCloseRequest(e->sendCanceledJson.run());

            passwordScene.focusOwnerProperty().addListener((obs, oldval, newVal) -> {
                if (newVal != null && !(newVal instanceof PasswordField)) {
                    Platform.runLater(() -> passwordField.requestFocus());
                }
            });
            
            txStage.show();
    
            Platform.runLater(()->passwordField.requestFocus());
            
            ResizeHelper.addResizeListener(txStage, 400, 600, Double.MAX_VALUE, Double.MAX_VALUE);

            
        }
        return null;
    }

   


    
    public Future<?> executeTransaction(JsonObject note, String locationString, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) throws Exception{
        
        JsonElement dataElement = note.get("data");
        int lblCol = 170;
        int rowHeight = 22;

        if(dataElement != null && dataElement.isJsonObject())
        {
            JsonObject txDataObject = dataElement.getAsJsonObject();

            if(txDataObject == null){
                throw new Exception("No Ergo output data provided");
            }


            ErgoTransactionData txData = new ErgoTransactionData(txDataObject, getNetworkType(), getDefaultNode(), getDefaultExplorer() );
            if(txData.getErgoInputData().size() != 1){
                throw new Exception("Limited to currenly requiring one asset input");
            }
            
            if(txData.getFeeInputData() == null){
                throw new Exception("No fee input provided");
            }

            ErgoInputData[] assetInputs = txData.getAssetsInputData();
            
            for(ErgoInputData inputData : assetInputs){
                if(inputData.getWalletType().equals(NoteConstants.CURRENT_WALLET_FILE)){
                    AddressData addressData = getAddressData(inputData.getAddressString());

                    if(addressData == null){
                        throw new Exception("Input address not found in this wallet");
                    }

                    ArrayList<PriceAmount> balanceList = NoteConstants.getBalanceList(addressData.getBalance(), true, getNetworkType());
                    for(ErgoToken sendAmount : inputData.getTokens()){
                        PriceAmount balanceAmount = NoteConstants.getPriceAmountFromList(balanceList, sendAmount.getId().toString());
                        if(sendAmount.getValue() > balanceAmount.getLongAmount()){
                            throw new Exception("Insufficent " + balanceAmount.getCurrency().getName());
                        }
                    }
                }
            }
            
            String title = "Wallet - Send Assets - Signature Required";
    
            Stage txStage = new Stage();
            txStage.getIcons().add(Stages.logo);
            txStage.initStyle(StageStyle.UNDECORATED);
            txStage.setTitle(title);

            PasswordField passwordField = new PasswordField();
            Button closeBtn = new Button();

            Scene passwordScene = getAuthorizationScene(txStage,title,closeBtn, passwordField, txDataObject, locationString, rowHeight, lblCol);

            txStage.setScene(passwordScene);

            passwordField.setOnAction(e -> {
                String pass = passwordField.getText();
                passwordField.setText("");
                
                Button cancelBtn = new Button("Cancel");
                cancelBtn.setId("iconBtnSelected");
                Label progressLabel = new Label(m_walletData.isTxAvailable() ? "Opening wallet..." : "Waiting for wallet access...");
                
                Scene waitingScene = Stages.getWaitngScene(progressLabel, cancelBtn, txStage);
                txStage.setScene(waitingScene);
                txStage.centerOnScreen();

                Future<?> walletFuture = m_walletData.startTx(pass, m_walletData.getWalletFile(),getExecService(), onWalletLoaded->{
                    
                    cancelBtn.setDisable(true);
                    cancelBtn.setId("iconBtn");
                    Object walletObject = onWalletLoaded.getSource().getValue();
                    if(walletObject != null && walletObject instanceof Wallet){
                        progressLabel.setText("Executing transaction...");
                        executeTransaction(
                            getExecService(),
                            pass,
                            (Wallet) walletObject, 
                            txData,
                            (onSent)->{
                               
                                Utils.returnObject(onSent.getSource().getValue(), getExecService(), onSucceeded);
                                txStage.close();
                                m_walletData.endTx();
                            }, 
                            (onSendFailed)->{                     
                            
                                Object sourceException = onSendFailed.getSource().getException();
                                Exception exception = sourceException instanceof Exception ? (Exception) sourceException : null;
                                if(exception != null){
                                    Utils.returnException(exception , getExecService(), onFailed);
                                }else{
                                    String msg = sourceException == null ? "Transaction terminated unexpectedly" : onSendFailed.getSource().getException().toString(); 
                                    Utils.returnException(msg, getExecService(), onFailed);
                                }
                                txStage.close();
                                m_walletData.endTx();
                            }
                        );
                    }else{
                        m_walletData.endTx();
                        txStage.setScene(passwordScene);
                        txStage.centerOnScreen();
                    }
                }, onLoadFailed->{
                    Throwable throwable = onLoadFailed.getSource().getException();

                    if(throwable != null){
                        if(!(throwable instanceof InterruptedException)){
                            m_walletData.endTx();
                        }
                        if(throwable instanceof NoSuchFileException){

                            Alert noFileAlert = new Alert(AlertType.ERROR, "Wallet file does not exist, or has been moved.", ButtonType.OK);
                            noFileAlert.setHeaderText("Error");
                            noFileAlert.setTitle("Error: File not found");
                            noFileAlert.show();
                            Utils.returnException((Exception) throwable, getExecService(), onFailed);
                            txStage.close();
                        }else{
                            txStage.setScene(passwordScene);
                            txStage.centerOnScreen();
                        }
                    }else{
                        m_walletData.endTx();

                        Alert unavailableAlert = new Alert(AlertType.ERROR, "Transaction Unavailable", ButtonType.OK);
                        unavailableAlert.setHeaderText("Error");
                        unavailableAlert.setTitle("Error: Transaction Unavailable");
                        unavailableAlert.show();

                        Utils.returnException("Transaction Unavailable", getExecService(), onFailed);
                        txStage.close();
                    }

                });

                cancelBtn.setOnAction(onCancel->{
                    walletFuture.cancel(true);
                });
            });

            
            Runnable sendCanceledJson =()->{
          
                passwordField.setOnAction(null);
                Utils.returnException("Transaction Canceled", getExecService(), onFailed);
                txStage.close();
            };

            closeBtn.setOnAction(e -> sendCanceledJson.run());


            txStage.setOnCloseRequest(e->sendCanceledJson.run());

            passwordScene.focusOwnerProperty().addListener((obs, oldval, newVal) -> {
                if (newVal != null && !(newVal instanceof PasswordField)) {
                    Platform.runLater(() -> passwordField.requestFocus());
                }
            });
            
            txStage.show();
    
            Platform.runLater(()->passwordField.requestFocus());
            
            ResizeHelper.addResizeListener(txStage, 400, 600, Double.MAX_VALUE, Double.MAX_VALUE);

            
        }
        return null;

    }

    public Future<?> executeTransaction(
        ExecutorService execService, 
        String pass,    
        Wallet wallet,  
        ErgoTransactionData txData,
        EventHandler<WorkerStateEvent> onSucceeded,
        EventHandler<WorkerStateEvent> onFailed)
    {
        try{
            txData.prepareContract(wallet);
        }catch(Exception e){
            return Utils.returnException(e, execService, onFailed);
        }

        Task<JsonObject> task = new Task<JsonObject>() {
            @Override
            public JsonObject call() throws Exception {

    
                
                NetworkType networkType = txData.getNetworkType();
                Address changeAddress = txData.getChangeAddress(wallet);
                String nodeUrl = txData.getNamedNodeUrl().getUrlString();
                String nodeApiKey = txData.getNamedNodeUrl().getApiKey();
                String explorerUrl = txData.getErgoExplorerUrl().getUrlString();
   
                ErgoInputData inputData = txData.getAssetsInputData()[0];
                
                ErgoClient ergoClient = RestApiErgoClient.create(nodeUrl, networkType, nodeApiKey, explorerUrl);

                List<Address> addresses = wallet.addressStream(networkType).toList();
          
                UnsignedTransaction unsignedTx = ergoClient.execute(ctx -> {
                    
                    List<InputBox> boxesToSpend =  BoxOperations.createForSenders(addresses, ctx)
                    .withAmountToSpend(inputData.getNanoErgs())
                    .withFeeAmount(inputData.getFeeNanoErgs())
                    .withTokensToSpend(List.of(inputData.getTokens()))
                    .loadTop();

                    UnsignedTransactionBuilder txBuilder = ctx.newTxBuilder();
                    int outBoxSize = txData.getOutputData().size();

                    OutBox[] outBoxes = new OutBox[outBoxSize];

                    for(int i = 0; i < outBoxSize ; i++){
                        OutputData outputData = txData.getOutputData().get(i);
                        outBoxes[i] = outputData.getOutBox(ctx, txBuilder.outBoxBuilder());
                        
                    }
                    
                    return txBuilder
                    .addInputs(boxesToSpend.toArray(new InputBox[0])).addOutputs(outBoxes)
                    .fee(inputData.getFeeNanoErgs())
                    .sendChangeTo(changeAddress)
                    .build();
                
                });

        
                String txId = null; /*unsignedTx != null ?  wallet.transact(ergoClient, ergoClient.execute(ctx -> {
                    try {
                        return wallet.key().signWithPassword(pass, ctx, unsignedTx, wallet.myAddresses.keySet());
                    } catch (WalletKey.Failure ex) {

                        return null;
                    }
                })) : null;*/


                if(txId != null){

                    JsonObject resultObject = new JsonObject();
                    resultObject.addProperty("result","Executed");
                    resultObject.addProperty("txId", txId);
                    resultObject.addProperty("timeStamp", System.currentTimeMillis());

                    try{
                        
                        BlockchainDataSource dataSource = ergoClient != null ? ergoClient.getDataSource() : null;
                        List<BlockHeader> blockHeaderList =  dataSource != null ? dataSource.getLastBlockHeaders(1, true) : null;
                        BlockHeader blockHeader = blockHeaderList != null && blockHeaderList.size() > 0 ? blockHeaderList.get(0)  : null;
                        
                        int blockHeight = blockHeader != null ? blockHeader.getHeight() : -1;
                        long timeStamp = blockHeader != null ? blockHeader.getTimestamp() : -1;

                        JsonObject networkInfoObject = new JsonObject();
                        networkInfoObject.addProperty("networkHeight", blockHeight);
                        networkInfoObject.addProperty("timeStamp", timeStamp);
                        resultObject.add("networkInfo", networkInfoObject);
                    }catch(Exception dataSourcException){
        
                    }
                    
                    return resultObject;
                }else{
                    throw new Exception("Transaction signing failed");
                }
                
            }
        };

        task.setOnSucceeded(onSucceeded);
        task.setOnFailed(onFailed);

        return execService.submit(task);
    }


   


    public JsonObject getDefaultExplorer(){
        JsonObject getDefaultExplorerNote = NoteConstants.getCmdObject("getDefault");

        Object explorerObj = m_walletData.getErgoNetworkData().getErgoExplorers().sendNote(getDefaultExplorerNote);
        NoteInterface explorerInterface = explorerObj != null && explorerObj instanceof NoteInterface ?  (NoteInterface) explorerObj : null;
        JsonObject defaultObject =explorerInterface != null ?explorerInterface.getJsonObject(): null;

        return defaultObject;
    }

    public JsonObject getDefaultNode(){
        JsonObject getDefaultNodeNote = NoteConstants.getCmdObject("getDefaultInterface");
        Object nodeObj =  m_walletData.getErgoNetworkData().getErgoNodes().sendNote(getDefaultNodeNote);
        NoteInterface nodeInterface = nodeObj != null && nodeObj instanceof NoteInterface ? (NoteInterface) nodeObj : null;
        return nodeInterface != null ? nodeInterface.getJsonObject() : null;
    }



    public JsonArray getAddressesJson(){
        JsonArray json = new JsonArray();
        
        for(AddressData addressData : m_addressDataList){
            json.add(addressData.getAddressJson());
        }
        return json;
    }


    
    private void start(){

        if(m_schedualedExecutor == null){
            m_schedualedExecutor = Executors.newSingleThreadScheduledExecutor();
            
            //Ergo
            m_ergoNetworkMsgInterface = new NoteMsgInterface() {
                private String m_msgId = FriendlyId.createFriendlyId();
                @Override
                public String getId() {
                    return m_msgId;
                }

                @Override
                public void sendMessage(int code, long timestamp, String networkId, String msg) {
                    
                    if(networkId != null){
                        switch(networkId){
                            case NoteConstants.MARKET_NETWORK:
                                switch(code){
                                
                                    case NoteConstants.LIST_DEFAULT_CHANGED:
                                        getDefaultMarket(); 
                                    break;
                                
                                }
                            break;
                            case NoteConstants.TOKEN_MARKET_NETWORK:
                                switch(code){
                                    case NoteConstants.LIST_DEFAULT_CHANGED:
                                        getDefaultTokenMarket();
                                    break;
                                }
                            break;
                        }
                       
                    }
                   
                }

                @Override
                public void sendMessage(int code, long timestamp, String networkId, Number number) {
   
                }
                
            };
          
            m_selectedMarketChanged = (obs,oldval,newval)->{
                if(oldval != null){
                    connectToErgoExchange(false, oldval);
                }
                
                if(newval != null){
                    connectToErgoExchange(true, newval);
                }
            };

            m_selectedMarket.addListener(m_selectedMarketChanged);
            
            


            m_selectedTokenMarketChanged = (obs,oldval,newval)->{
                if(oldval != null){
                    connectToTokenExchange(false, oldval);
                }
                
                if(newval != null){
                    connectToTokenExchange(true, newval);
                }
            };

            m_selectedTokenMarket.addListener(m_selectedTokenMarketChanged);
            
            getDefaultMarket();
            getDefaultTokenMarket();


            getErgoNetwork().addMsgListener(m_ergoNetworkMsgInterface);

            m_scheduledFuture = m_schedualedExecutor.scheduleAtFixedRate(()->{
                update();
            },0, NoteConstants.POLLING_TIME, TimeUnit.MILLISECONDS);
       
        }
    }



    public void update(){


        for(int i = 0; i < m_addressDataList.size(); i++){
            AddressData addressData = m_addressDataList.get(i);

            addressData.updateBalance();
        }
       

    }

    public void stop(){
       
        if(m_scheduledFuture != null){
            m_scheduledFuture.cancel(false);
            m_scheduledFuture = null;
        }
        if(m_schedualedExecutor != null){
            m_schedualedExecutor.shutdownNow();
            m_schedualedExecutor = null;
        }

        if(m_ergoNetworkMsgInterface != null){
            getErgoNetwork().removeMsgListener(m_ergoNetworkMsgInterface);
            m_ergoNetworkMsgInterface = null;
        }
  
        if(m_selectedMarketChanged != null){
            m_selectedMarket.set(null);
            m_selectedMarket.removeListener(m_selectedMarketChanged);
            m_selectedMarketChanged = null;
        }


        if(m_selectedTokenMarketChanged != null){
            m_selectedTokenMarket.set(null);
   
            m_selectedTokenMarket.removeListener(m_selectedTokenMarketChanged);
            m_selectedTokenMarketChanged = null;
        }
    }
    

    public SimpleObjectProperty<LocalDateTime> balanceUpdatedProperty(){
        return m_lastUpdated;
    }

    public ErgoWalletData getWalletData() {
        return m_walletData;
    }


    public void addAddress() {

        if (m_promptStage == null) {

            m_promptStage = new Stage();
            m_promptStage.initStyle(StageStyle.UNDECORATED);
            m_promptStage.getIcons().add(new Image("/assets/git-branch-outline-white-30.png"));
            m_promptStage.setTitle("Add Address - " + m_walletData.getName() + " - Ergo Wallets");

            TextField textField = new TextField();
            Button closeBtn = new Button();

            Stages.showGetTextInput("Address name", "Add Address", new Image("/assets/git-branch-outline-white-240.png"), textField, closeBtn, m_promptStage);
            closeBtn.setOnAction(e -> {
                m_promptStage.close();
                m_promptStage = null;
            });
            m_promptStage.setOnCloseRequest(e -> {
                closeBtn.fire();
            });
            textField.setOnKeyPressed(e -> {

              //  KeyCode keyCode = e.getCode();

                /* if (keyCode == KeyCode.ENTER) {
                    String addressName = textField.getText();
                    if (!addressName.equals("")) {
                        int nextAddressIndex = m_wallet.nextAddressIndex();
                        m_wallet.myAddresses.put(nextAddressIndex, addressName);

                        try {

                            Address address = m_wallet.publicAddress(m_networkType, nextAddressIndex);
                            AddressData addressData = new AddressData(addressName, nextAddressIndex, address,m_wallet, m_networkType, this);
                            addAddressData(addressData);
                          
                        } catch (Failure e1) {

                            Alert a = new Alert(AlertType.ERROR, e1.toString(), ButtonType.OK);
                            a.showAndWait();
                        }

                    }
                    closeBtn.fire();
                }*/
            });
        } else {
            if (m_promptStage.isIconified()) {
                m_promptStage.setIconified(false);
            } else {
                if(!m_promptStage.isShowing()){
                    m_promptStage.show();
                }else{
                    Platform.runLater(() -> m_promptStage.toBack());
                    Platform.runLater(() -> m_promptStage.toFront());
                }
                
            }
        }

    }
   






    


    public AddressData getAddressData(String address){
        if(address != null){
            for(AddressData addressData :  m_addressDataList){
            
                if(addressData.getAddressString().equals(address)){
                    return addressData;
                }
            }
        }
        return null;
    }



    public long getBalanceTimeStamp(){
        return m_balanceTimestamp;
    }



    public void shutdown() {
      
       

        stop();
         
     
    }



    public void connectToErgoExchange(boolean connect, NoteInterface exchangeInterface ){
     
        if(connect && exchangeInterface != null){

          
            m_marketMsgInterface = new NoteMsgInterface() {
                private String m_msgId = FriendlyId.createFriendlyId();

                public String getId(){
                    return m_msgId;
                }

                public void sendMessage(int code, long timeStamp, String poolId, Number num){
                   m_walletData.sendMessage(code, timeStamp,  NoteConstants.MARKET_NETWORK +":" +poolId, num);

                  
                   /* switch(code){
                        case NoteConstants.LIST_CHANGED:
                        case NoteConstants.LIST_UPDATED:
                            //updated
                            
                            m_ergoExchangeStatus.set(code);
                            break;
                        case NoteConstants.STOPPED:
                            m_ergoExchangeStatus.set(code);
                        break;
                        case NoteConstants.STARTED:
                            m_ergoExchangeStatus.set(code);
                        break;
                        case NoteConstants.STARTING:
                            m_ergoExchangeStatus.set(code);
                        break;
                        case NoteConstants.ERROR:
                        
                        break;
                    } */
                    
                }
            
                public void sendMessage(int code, long timestamp, String networkId, String msg){
                    m_walletData.sendMessage(code, timestamp, NoteConstants.MARKET_NETWORK +":"+ networkId, msg);
                    /*if(code == NoteConstants.ERROR){
                
                      //  m_statusMsg = msg;
                      //  m_ergoExchangeStatus.set(code);
                    }*/
                }

            };
    
            exchangeInterface.addMsgListener(m_marketMsgInterface);
            
        }else{
            
            if(m_marketMsgInterface != null && exchangeInterface != null){
              
                exchangeInterface.removeMsgListener(m_marketMsgInterface);  
            }
            m_marketMsgInterface = null;
        }
    }

    public void connectToTokenExchange(boolean connect, NoteInterface tokenExchangeInterface ){
     
        if(connect && tokenExchangeInterface != null){

          
            m_tokenMarketMsgInterface = new NoteMsgInterface() {
                private String m_msgId = FriendlyId.createFriendlyId();

                public String getId(){
                    return m_msgId;
                }

                public void sendMessage(int code, long timeStamp, String id, Number num){
                    m_walletData.sendMessage(code, timeStamp, NoteConstants.TOKEN_MARKET_NETWORK+":" +id, num);
            
                   /* switch(code){
                        case NoteConstants.LIST_CHANGED:
                        case NoteConstants.LIST_UPDATED:
                            //updated
                        
                            break;
                        case NoteConstants.STOPPED:

                        break;
                        case NoteConstants.STARTED:
                        break;
                        case NoteConstants.STARTING:
                        break;
                        case NoteConstants.ERROR:
                        
                        break;
                    } */
                    
                }
            
                public void sendMessage(int code, long timeStamp, String networkId, String msg){
                    m_walletData.sendMessage(code, timeStamp, NoteConstants.TOKEN_MARKET_NETWORK+":" +networkId, msg);
                    /*if(code == NoteConstants.ERROR){
                
               
                    }*/
                }

        

            };
    
            tokenExchangeInterface.addMsgListener(m_tokenMarketMsgInterface);
            
            

        }else{

            if(m_tokenMarketMsgInterface != null && tokenExchangeInterface != null){
                
                tokenExchangeInterface.removeMsgListener(m_tokenMarketMsgInterface);  
            }
            m_tokenMarketMsgInterface = null;
        }
    }



    public ErgoNetworkData getErgoNetworkData(){
        return  m_walletData.getErgoNetworkData();
    }

    public ErgoNetwork getErgoNetwork(){
        return getErgoNetworkData().getErgoNetwork();
    }

    public String getLocationId(){
    
        return getErgoNetworkData().getLocationId();
    }

    public void getDefaultMarket(){
        
        JsonObject note = NoteConstants.getCmdObject("getDefaultMarketInterface");
        note.addProperty("networkId", NoteConstants.MARKET_NETWORK);
        note.addProperty("locationId", getLocationId());

        Object obj = getErgoNetwork().sendNote(note);
        NoteInterface noteInterface =obj != null && obj instanceof NoteInterface ? (NoteInterface) obj : null;

      
        m_selectedMarket.set(noteInterface);
        
    }

    public void getDefaultTokenMarket(){
        
        JsonObject note = NoteConstants.getCmdObject("getDefaultTokenInterface");
        note.addProperty("networkId", NoteConstants.MARKET_NETWORK);
        note.addProperty("locationId", getLocationId());
        Object obj = getErgoNetwork().sendNote(note);;
        NoteInterface noteInterface =obj != null && obj instanceof NoteInterface ? (NoteInterface) obj : null;
        m_selectedTokenMarket.set(noteInterface);
        
    }


    public boolean isTokenMarket(){
        NoteInterface noteInterface = m_selectedTokenMarket.get();
        return noteInterface != null && noteInterface.getConnectionStatus() == NoteConstants.STARTED;
    }

    public boolean isErgoMarket(){
        NoteInterface noteInterface = m_selectedMarket.get();
        return noteInterface != null && noteInterface.getConnectionStatus() == NoteConstants.STARTED;
    }

    public PriceQuote getErgoQuote(){
        NoteInterface exchangeInterface = m_selectedMarket.get();
        if(exchangeInterface != null && exchangeInterface.getConnectionStatus() == NoteConstants.STARTED){
          
            JsonObject note = NoteConstants.getCmdObject("getErgoUSDQuote");
            note.addProperty("locationId", getLocationId());

            Object result =  exchangeInterface.sendNote(note);
            if(result != null && result instanceof PriceQuote){
                PriceQuote ergoQuote = (PriceQuote) result;
                return ergoQuote;
            }
        }
        return null;
    }

    public PriceQuote getTokenQuoteInErg(String tokenId){
        NoteInterface exchangeTokenInterface = m_selectedTokenMarket.get();

        if(exchangeTokenInterface != null && exchangeTokenInterface.getConnectionStatus() == NoteConstants.STARTED){

            JsonObject note = NoteConstants.getCmdObject("getTokenQuoteInErg");

            note.addProperty("locationId", getLocationId());
            note.addProperty("tokenId",tokenId);
            Object result =  exchangeTokenInterface.sendNote(note);
            
            if(result != null && result instanceof PriceQuote){
                PriceQuote priceQuote = (PriceQuote) result;
                return priceQuote;
            }
        }
        return null;
    }

}
