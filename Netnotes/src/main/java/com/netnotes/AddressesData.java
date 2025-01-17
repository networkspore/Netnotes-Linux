package com.netnotes;


import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.Bidi;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.satergo.Wallet;
import com.satergo.WalletKey;
import com.satergo.WalletKey.Failure;
import com.satergo.ergo.ErgoInterface;

import com.utils.Utils;
import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
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

import scorex.util.encode.Base16;

import org.apache.commons.io.IOUtils;
import org.ergoplatform.appkit.*;

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
    
    private SimpleIntegerProperty m_ergoExchangeStatus = new SimpleIntegerProperty(App.STOPPED);
    private SimpleIntegerProperty m_tokenExchangeStatus = new SimpleIntegerProperty(App.STOPPED);
    private String m_statusMsg = "";
    private String m_tokenStatusMsg = "";

    private String m_ergoBaseQuery = "ERG";
    private String m_ergoBaseType = "symbol";

    private String m_ergoQuoteType = "firstSymbolContains"; 
    private String m_ergoQuoteQuery = "USD";






   // private SimpleObjectProperty<PriceQuote> m_currentQuote = new SimpleObjectProperty<>(null);
    public final static long POLLING_TIME = 3000;

    public final static int ADDRESS_IMG_HEIGHT = 40;
    public final static int ADDRESS_IMG_WIDTH = 350;
    public final static long QUOTE_TIMEOUT = POLLING_TIME*2;

    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>(LocalDateTime.now());

    private Stage m_promptStage = null;

    private ScheduledExecutorService m_schedualedExecutor = null;
    private ScheduledFuture<?> m_scheduledFuture = null;

    private NoteMsgInterface m_ergoNetworkMsgInterface;

    private NoteMsgInterface m_marketMsgInterface = null;
    private NoteMsgInterface m_tokenMarketMsgInterface = null;

    private ChangeListener<BigDecimal> m_priceQuoteAmountChanged = null;
    private long m_balanceTimestamp = 0;

    public AddressesData(String id,  ArrayList<AddressData> addressDataList, ErgoWalletData walletData, NetworkType networkType) {

      //  m_wallet = wallet;
        m_walletData = walletData;
        m_networkType = networkType;
        m_addressDataList = addressDataList;
        
     
        for(AddressData addressData : m_addressDataList){
            addressData.setAddressesData(AddressesData.this);
        }

      //  m_addressDataList.get(0).updateBalance();
        
        start();
    }

    public static ArrayList<PriceAmount>  getBalanceList(JsonObject json, boolean confirmed, NetworkType networkType){

        ArrayList<PriceAmount> ballanceList = new ArrayList<>();

        JsonElement timeStampElement = json != null ? json.get("timeStamp") : null;
        JsonElement objElement = json != null ? json.get( confirmed ? "confirmed" : "unconfirmed") : null;

        long timeStamp = timeStampElement != null ? timeStampElement.getAsLong() : -1;
        if (objElement != null && timeStamp != -1) {

            JsonObject objObject = objElement.getAsJsonObject();
            JsonElement nanoErgElement = objObject.get("nanoErgs");
            
            long nanoErg = nanoErgElement != null && nanoErgElement.isJsonPrimitive() ? nanoErgElement.getAsLong() : 0;

            ErgoAmount ergoAmount = new ErgoAmount(nanoErg, networkType);
        
            ballanceList.add(ergoAmount);
            
            JsonElement confirmedArrayElement = objObject.get("tokens");

            if(confirmedArrayElement != null && confirmedArrayElement.isJsonArray()){
                JsonArray confirmedTokenArray = confirmedArrayElement.getAsJsonArray();

                for (JsonElement tokenElement : confirmedTokenArray) {
                    JsonObject tokenObject = tokenElement.getAsJsonObject();

                    ErgoBoxAsset asset = new ErgoBoxAsset(tokenObject);


                    PriceAmount tokenAmount = new PriceAmount(asset, networkType.toString(), true);    
                    
                    ballanceList.add(tokenAmount);
                    
                }
            }
     
             
        } 

        return ballanceList;
    
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

    public static ArrayList<PriceAmount> getSendAssetsListFromArray(JsonArray jsonArray, NetworkType networkType){
        ArrayList<PriceAmount> assetsList = new ArrayList<>();
        for(JsonElement element : jsonArray){

            if(!element.isJsonNull() && element.isJsonObject()){
                JsonObject assetJson = element.getAsJsonObject();
                assetsList.add(PriceAmount.getByAmountObject(assetJson, networkType));
            }
        }
        return assetsList;
    }

    public static ArrayList<PriceAmount> getTokenFromList(ArrayList<PriceAmount> assetsList){
        ArrayList<PriceAmount> tokensList = new ArrayList<>();
        for(PriceAmount priceAmount : assetsList){
            if(!priceAmount.getTokenId().equals(ErgoCurrency.TOKEN_ID)){
                tokensList.add(priceAmount);
            }
        
        }
        return tokensList;
    }

    public static PriceAmount getPriceAmountFromList(ArrayList<PriceAmount> priceList, String tokenId){
        if(tokenId != null && priceList != null){
            int size = priceList.size();
            for(int i = 0; i < size ; i++){
                PriceAmount amount = priceList.get(i);
                if(amount != null && amount.getTokenId().equals(tokenId)){
                    return amount;
                }
            }
        }
        
        return null;
    }

    


    public ExecutorService getExecService(){
        return m_walletData.getNetworksData().getExecService();
    }


    public Future<?> sendAssets(JsonObject note, String locationString, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        
        JsonElement dataElement = note.get("data");
        int lblCol = 170;
        int rowHeight = 22;

        if(dataElement != null && dataElement.isJsonObject())
        {
            JsonObject dataObject = dataElement.getAsJsonObject();

            JsonElement recipientElement = dataObject.get("recipient");
            JsonElement assetsElement = dataObject.get("assets");

            
            if(!checkNetworkType(dataObject, m_networkType)){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Network type must be " + m_networkType.toString()), getExecService(), onSucceeded, onFailed);
                return null;
            }


            String walletAddress = getAddressStringFromDataObject(dataObject);

            
            if(walletAddress == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "No wallet address provided"), getExecService(), onSucceeded, onFailed);
                return null;
            }


            AddressData addressData = getAddress(walletAddress);

            if(addressData == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Address not found in this wallet"), getExecService(), onSucceeded, onFailed);
                return null;
            }

            ArrayList<PriceAmount> balanceList = getBalanceList(addressData.getBalance(),true, m_networkType);


            long feeAmountNanoErgs = getFeeAmountNanoErgs(dataObject);
            

            if(feeAmountNanoErgs == -1){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "No fee provided"), getExecService(), onSucceeded, onFailed);
                return null;
            }

            if(feeAmountNanoErgs < 1000000){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Minimum fee of 1000000 nanoErg (0.001 Erg) required"), getExecService(), onSucceeded, onFailed);
                return null;
            }

            long amountToSpendNanoErgs = getAmountToSpendNanoErgs(dataObject);
          

            if(assetsElement == null || assetsElement != null && !assetsElement.isJsonArray()){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Asset element required"), getExecService(), onSucceeded, onFailed);
                return null;
            }

            
            ArrayList<PriceAmount> assetsList = getSendAssetsListFromArray(assetsElement.getAsJsonArray(), m_networkType);
          
            ArrayList<PriceAmount> tokensList = getTokenFromList(assetsList);

            ErgoToken[] tokenArray = new ErgoToken[tokensList.size()];
            for(int i = 0; i < tokensList.size() ; i++){
                PriceAmount tokenAmount = tokensList.get(i);
                tokenArray[i] = tokenAmount.getErgoToken();
            }

           
            if(assetsList != null && assetsList.size() > 0){
                Utils.returnObject(Utils.getMsgObject(App.ERROR,  "No assets transmitted"), getExecService(), onSucceeded, onFailed);           
                return null;
            }

            for(PriceAmount sendAmount : assetsList){
                PriceAmount balanceAmount = getPriceAmountFromList(balanceList, sendAmount.getTokenId());
                if(sendAmount.getLongAmount() > balanceAmount.getLongAmount()){
                    Utils.returnObject(Utils.getMsgObject(App.ERROR,  "Insufficent " + balanceAmount.getCurrency().getName() + " - Required: " + sendAmount.getBigDecimalAmount()), getExecService(), onSucceeded, onFailed);           
                    return null;
                }
            }
            
            JsonObject recipientObject = recipientElement != null && recipientElement.isJsonObject() ? recipientElement.getAsJsonObject() : null;

            if(recipientObject == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "No recipient provided"), getExecService(), onSucceeded, onFailed);
                return null;
            }

            JsonElement recipientAddressElement = recipientObject.get("address");

            AddressInformation recipientAddressInfo = recipientAddressElement != null && !recipientAddressElement.isJsonNull() ? new AddressInformation(recipientAddressElement.getAsString().replaceAll("[^A-HJ-NP-Za-km-z1-9]", "")) : null;
            
            if(recipientAddressInfo == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "No recipient address provided"), getExecService(), onSucceeded, onFailed);
                return null;
            }

            if(recipientAddressInfo.getAddress() == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Invalid recipient address"), getExecService(), onSucceeded, onFailed);
                return null;
            }

            NamedNodeUrl namedNodeUrl = getNamedNodeUrl(dataObject);

            if(namedNodeUrl == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Node unavailable"), getExecService(), onSucceeded, onFailed);
                return null;
            }

            if(namedNodeUrl.getUrlString() == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "No node URL provided"), getExecService(), onSucceeded, onFailed);
                return null;
            }

            String nodeUrl = namedNodeUrl.getUrlString();
            String nodeApiKey = namedNodeUrl.getApiKey();

            ErgoNetworkUrl explorerNetworkUrl = getExplorerUrl(dataObject);

            if(explorerNetworkUrl == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Explorer unavailable"), getExecService(), onSucceeded, onFailed);
                return null;
            }

           

            String explorerUrl = explorerNetworkUrl.getUrlString();


            String title = "Wallet - Send Assets - Signature Required";

     

            JsonParametersBox parametersBox = new JsonParametersBox(dataObject, lblCol);
            parametersBox.openAll();


            Label locationLbl = new Label("Location:");
            locationLbl.setMinWidth(lblCol);
            locationLbl.setFont(App.txtFont);

            TextField locationField = new TextField(locationString);
            locationField.setEditable(false);
            HBox.setHgrow(locationField, Priority.ALWAYS);
            locationField.setFont(App.txtFont);

            HBox locationBox = new HBox(locationLbl, locationField);
            HBox.setHgrow(locationBox,Priority.ALWAYS);
            locationBox.setAlignment(Pos.CENTER_LEFT);
            locationBox.setMinHeight(rowHeight);


            Stage passwordStage = new Stage();
            passwordStage.getIcons().add(App.logo);
            passwordStage.initStyle(StageStyle.UNDECORATED);
            passwordStage.setTitle(title);

            Button closeBtn = new Button();

            HBox titleBox = App.createTopBar(App.icon, title, closeBtn, passwordStage);

            ImageView btnImageView = new ImageView(App.logo);
            btnImageView.setPreserveRatio(true);
            btnImageView.setFitHeight(75);
            

            Label textField = new Label("Authorization Required");
            textField.setFont(App.mainFont);
            textField.setPadding(new Insets(20,0,20,15));
            

            VBox imageBox = new VBox(btnImageView, textField);
            imageBox.setAlignment(Pos.CENTER);
            imageBox.setPadding(new Insets(10,0,10,0));

            Text passwordTxt = new Text("Enter password:");
            passwordTxt.setFill(App.txtColor);
            passwordTxt.setFont(App.txtFont);

            PasswordField passwordField = new PasswordField();
            passwordField.setFont(App.txtFont);
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
                File saveFile = saveChooser.showSaveDialog(passwordStage);
                if(saveFile != null){
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    
                    try {
                        Files.writeString(saveFile.toPath(), gson.toJson(dataObject));
                    } catch (IOException e1) {
                        Alert alert = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                        alert.setTitle("Error");
                        alert.setHeaderText("Error");
                        alert.initOwner(passwordStage);
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
                parametersBox.setPrefWidth(newval.doubleValue()-1);
            });

  

            Scene passwordScene = new Scene(layoutVBox, 800, 600);
            passwordScene.setFill(null);
            passwordScene.getStylesheets().add("/css/startWindow.css");
            passwordStage.setScene(passwordScene);

         

            passwordField.setOnAction(e -> {
                String pass = passwordField.getText();
             
                Task<JsonObject> task = new Task<JsonObject>() {
                    @Override
                    public JsonObject call() throws Exception {
                        Platform.runLater(()-> passwordStage.close());
                        Wallet wallet = Wallet.load(m_walletData.getWalleFile().toPath(), pass);
                       
                        Address address = getWalletAddress(wallet, walletAddress, m_networkType);
                
                        ErgoClient ergoClient = RestApiErgoClient.create(nodeUrl, m_networkType, nodeApiKey, explorerUrl);

                        UnsignedTransaction unsignedTx = ErgoInterface.createUnsignedTransaction(
                            ergoClient,
                            wallet.addressStream(m_networkType).toList(),
                            recipientAddressInfo.getAddress(),
                            amountToSpendNanoErgs,
                            feeAmountNanoErgs,
                            address,
                            tokenArray
                        );

                    
                  
                        String txId = wallet.transact(ergoClient, ergoClient.execute(ctx -> {
                            try {
                                return wallet.key().signWithPassword(pass, ctx, unsignedTx, wallet.myAddresses.keySet());
                            } catch (WalletKey.Failure ex) {
        
                                return null;
                            }
                        }));


                        if(txId != null){

                            JsonElement networkElement = dataObject.remove("network");
                            
                            JsonObject networkObject = networkElement != null && networkElement.isJsonObject() ? networkElement.getAsJsonObject() : new JsonObject();
                            try{
                                
                                BlockchainDataSource dataSource = ergoClient != null ? ergoClient.getDataSource() : null;
                                List<BlockHeader> blockHeaderList =  dataSource != null ? dataSource.getLastBlockHeaders(1, true) : null;
                                BlockHeader blockHeader = blockHeaderList != null && blockHeaderList.size() > 0 ? blockHeaderList.get(0)  : null;
                                
                                int blockHeight = blockHeader != null ? blockHeader.getHeight() : -1;
                                long timeStamp = blockHeader != null ? blockHeader.getTimestamp() : -1;
                                networkObject.remove("networkHeight");
                                networkObject.remove("timeStamp");
                                networkObject.addProperty("networkHeight", blockHeight);
                                networkObject.addProperty("timeStamp", timeStamp);
    
                            }catch(Exception dataSourcException){
                                networkObject.remove("infoException");
                                networkObject.addProperty("infoException", dataSourcException.toString());
                            }
                            
                            dataObject.add("network", networkObject);

                            
                            JsonObject resultObject = new JsonObject();
                            resultObject.addProperty("code", App.SUCCESS);
                            resultObject.addProperty("timeStamp", System.currentTimeMillis());
                            resultObject.addProperty("txId", txId);
                            resultObject.addProperty("result","Success");
                            resultObject.add("data", dataObject);
                        
                            return resultObject;
                        }else{
                            throw new Exception("Transaction signing failed");
                        }
                        
                    }
                };
        
                task.setOnFailed((failed->{
                    JsonObject errorJson = Utils.getMsgObject(App.ERROR, failed.getSource().getException().toString());
                    errorJson.addProperty("errTxId", "error_"+ FriendlyId.createFriendlyId());
                    errorJson.addProperty("result","Error");
                    errorJson.add("data", dataObject);
                    Utils.returnObject(errorJson, getExecService(), onSucceeded, onFailed);
                   
                }));
        
                task.setOnSucceeded((succeeded)->{
                    Utils.returnObject(succeeded.getSource().getValue(), getExecService(), onSucceeded, onFailed);
            
                });
        
                Thread t = new Thread(task);
                t.start();
            
            
            });

            Runnable sendCanceledJson =()->{
                JsonObject errorJson = Utils.getMsgObject(App.CANCEL, "Transaction cancelled");
                errorJson.addProperty("errTxId", "cancel_" + FriendlyId.createFriendlyId());
                errorJson.addProperty("result","Canceled");
                errorJson.add("data", dataObject);
                Utils.returnObject(errorJson, getExecService(), onSucceeded, onFailed);
            };

            Semaphore activeThreadSemaphore = new Semaphore(1); 
            try {
                activeThreadSemaphore.acquire();
            } catch (InterruptedException e1) {
        
            }

            closeBtn.setOnAction(e -> {
                sendCanceledJson.run();
                activeThreadSemaphore.release();
                passwordStage.close();
                
            });



            passwordStage.setOnCloseRequest(e->{
                sendCanceledJson.run();
                activeThreadSemaphore.release();
                passwordStage.close();
            });

            passwordScene.focusOwnerProperty().addListener((obs, oldval, newVal) -> {
                if (newVal != null && !(newVal instanceof PasswordField)) {
                    Platform.runLater(() -> passwordField.requestFocus());
                }
            });
            
            passwordStage.show();
    
            Platform.runLater(()->passwordField.requestFocus());
            
            ResizeHelper.addResizeListener(passwordStage, 400, 600, Double.MAX_VALUE, Double.MAX_VALUE);

            
            Task<Object> task = new Task<Object>() {
                @Override
                public Object call() throws InterruptedException {
                    activeThreadSemaphore.acquire();
                    activeThreadSemaphore.release();
                    return null;
                }
            };
            return getExecService().submit(task);   
        }

        return null;

    }

    public JsonObject getDefaultExplorer(){
        JsonObject getDefaultExplorerNote = Utils.getCmdObject("getDefault");

        Object explorerObj = m_walletData.getErgoNetworkData().getErgoExplorers() .sendNote(getDefaultExplorerNote);
        NoteInterface explorerInterface = explorerObj != null && explorerObj instanceof NoteInterface ?  (NoteInterface) explorerObj : null;
        
        return explorerInterface != null ? explorerInterface.getJsonObject() : null;
    }

    public ErgoNetworkUrl getExplorerUrl(JsonObject dataObject){
        JsonElement explorerElement = dataObject.get("explorer");

        JsonObject explorerObject = explorerElement != null && explorerElement.isJsonObject() ? explorerElement.getAsJsonObject() : getDefaultExplorer();
        
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

    public JsonObject getDefaultNode(){
        JsonObject getDefaultNodeNote = Utils.getCmdObject("getDefaultInterface");
        Object nodeObj =  m_walletData.getErgoNetworkData().getErgoNodes().sendNote(getDefaultNodeNote);
        NoteInterface nodeInterface = nodeObj != null && nodeObj instanceof NoteInterface ? (NoteInterface) nodeObj : null;
        return nodeInterface != null ? nodeInterface.getJsonObject() : null;
    }

    public static void addNodeUrlToDataObject(JsonObject dataObject, NoteInterface nodeInterface){
        JsonObject nodeInterfaceObject = nodeInterface.getJsonObject();
        dataObject.add("node", nodeInterfaceObject);
    }

    public NamedNodeUrl getNamedNodeUrl(JsonObject dataObject){

        JsonElement nodeElement = dataObject.get("node");

        JsonObject nodeObject = nodeElement != null && nodeElement.isJsonObject() ? nodeElement.getAsJsonObject() : getDefaultNode();
        
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
   
    public static String getAddressStringFromDataObject(JsonObject dataObject){
        JsonElement walletElement = dataObject != null ? dataObject.get("wallet") : null;
        JsonObject walletJson = walletElement != null && walletElement.isJsonObject() ? walletElement.getAsJsonObject() : null;
        JsonElement walletAddressElement = walletJson != null ? walletJson.get("address") : null;
        return walletAddressElement != null && !walletAddressElement.isJsonNull() ? walletAddressElement.getAsString() : null;
    }

    public static JsonObject getAddressObject(String address, String name){
        JsonObject ergoObject = new JsonObject();
        ergoObject.addProperty("address", address);
        ergoObject.addProperty("name", name);
        return ergoObject;
    }

    public static void addWalletAddressToDataObject(JsonObject dataObject, String address, String walletName){
        JsonObject networkTypeObject = getAddressObject(address, walletName);
        dataObject.add("wallet", networkTypeObject);
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

    public static void addNetworkTypeToDataObject(JsonObject dataObject, NetworkType networkType){
        JsonObject networkTypeObject = getNetworkTypeObject(networkType);
        dataObject.add("network", networkTypeObject);
    }


    public static long getFeeAmountNanoErgs(JsonObject dataObject){
        JsonElement feeElement = dataObject != null ? dataObject.get("feeAmount") : null;
        JsonObject feeObject = feeElement != null && feeElement.isJsonObject() ? feeElement.getAsJsonObject() : null;
        JsonElement nanoErgsElement = feeObject != null ? feeObject.get("nanoErgs") : null;
        return nanoErgsElement != null ? nanoErgsElement.getAsLong() : -1;
    }

    public static long getAmountToSpendNanoErgs(JsonObject dataObject){
        JsonElement feeElement = dataObject != null ? dataObject.get("amountToSpend") : null;
        JsonObject feeObject = feeElement != null && feeElement.isJsonObject() ? feeElement.getAsJsonObject() : null;
        JsonElement nanoErgsElement = feeObject != null ? feeObject.get("nanoErgs") : null;
        return nanoErgsElement != null ? nanoErgsElement.getAsLong() : -1;
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
        JsonObject nanoErgsObject = createNanoErgsObject(ergoAmount.getLongAmount());
        nanoErgsObject.addProperty("ergs", ergoAmount.getBigDecimalAmount());
        dataObject.add("feeAmount", nanoErgsObject);
        return true;
    }

    public static boolean addAmountToSpendToDataObject(PriceAmount ergoAmount, JsonObject dataObject){
        if(ergoAmount == null || !ergoAmount.getTokenId().equals(ErgoCurrency.TOKEN_ID)){
            return false;
        }
        JsonObject nanoErgsObject = createNanoErgsObject(ergoAmount.getLongAmount());
        nanoErgsObject.addProperty("ergs", ergoAmount.getBigDecimalAmount());
        dataObject.add("amountToSpend", nanoErgsObject);
        return true;
    }

    public String getContractString(JsonObject datObject){
        return null;
    }
    
    public Future<?> executeContract(JsonObject note,String locationString, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        
        JsonElement dataElement = note.get("data");
        int lblCol = 120;
        int rowHeight = 22;

        if(dataElement != null && dataElement.isJsonObject())
        {
            try {
                Files.writeString(App.logFile.toPath(), note + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {

            }

            JsonObject dataObject = dataElement.getAsJsonObject();
            if(dataObject == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "'data' element missing"), getExecService(), onSucceeded, null);
                return null;
            }
            
            NamedNodeUrl namedNodeUrl = getNamedNodeUrl(dataObject);
            if(namedNodeUrl == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Node unavailable"), getExecService(), onSucceeded, null);
                return null;
            }

            ErgoNetworkUrl explorerUrl = getExplorerUrl(dataObject);
            if(explorerUrl == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Explorer unavailable"), getExecService(), onSucceeded, null);
                return null;
            }

            String walletAddress = getAddressStringFromDataObject(dataObject);
            if(walletAddress == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "No wallet address provided"), getExecService(), onSucceeded, onFailed);
                return null;
            }


            if(!checkNetworkType(dataObject, m_networkType)){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Network type mismatch"), getExecService(), onSucceeded, onFailed);
                return null;
            }

            long nanoErgFee = getFeeAmountNanoErgs(dataObject);
            if(nanoErgFee < 2000000){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, nanoErgFee == -1 ? "Network fee unavailable" : "Insufcient network fee: "+nanoErgFee+" (Minimum: 2000000 nanoErg or 0.002 Erg)"), getExecService(), onSucceeded, onFailed);
                return null;
            }

            Semaphore activeThreadSemaphore = new Semaphore(1);
            try{
                activeThreadSemaphore.acquire();
            }catch(InterruptedException e){
                
            }


            String smartContract = getContractString(dataObject);


            BigDecimal priceQuoteDecimal = new BigDecimal(1.75);

            ErgoAmount ergoAmount = new ErgoAmount(BigDecimal.valueOf(1), m_networkType);
            
            long ergoAmountLong = ergoAmount.getLongAmount();
            long minerFeeAmount = 0;//feePriceAmount.getLongAmount(); 

            //SwapPossible outputAmount()
            
            BigDecimal slippage = BigDecimal.valueOf(minerFeeAmount);

            
            BigDecimal minExFee = BigDecimal.valueOf(1000);
            BigDecimal nitro = BigDecimal.valueOf(1.0);
            BigDecimal minOutput = null;

            //BigDecimal exFeePerToken = 

            BigDecimal quoteDecimal = ergoAmount.getBigDecimalAmount().multiply(priceQuoteDecimal);




            String title = "Wallet - Execute Contract - Signature Required";

     

            JsonParametersBox parametersBox = new JsonParametersBox(dataObject, lblCol);
            parametersBox.openAll();


            Label locationLbl = new Label("Location:");
            locationLbl.setMinWidth(lblCol);
            locationLbl.setFont(App.txtFont);

            TextField locationField = new TextField(locationString);
            locationField.setEditable(false);
            HBox.setHgrow(locationField, Priority.ALWAYS);
            locationField.setFont(App.txtFont);

            HBox locationBox = new HBox(locationLbl, locationField);
            HBox.setHgrow(locationBox,Priority.ALWAYS);
            locationBox.setAlignment(Pos.CENTER_LEFT);
            locationBox.setMinHeight(rowHeight);


            Stage passwordStage = new Stage();
            passwordStage.getIcons().add(App.logo);
            passwordStage.initStyle(StageStyle.UNDECORATED);
            passwordStage.setTitle(title);

            Button closeBtn = new Button();

            HBox titleBox = App.createTopBar(App.icon, title, closeBtn, passwordStage);

            ImageView btnImageView = new ImageView(App.logo);
            btnImageView.setPreserveRatio(true);
            btnImageView.setFitHeight(75);
            

            Label textField = new Label("Authorization Required");
            textField.setFont(App.mainFont);
            textField.setPadding(new Insets(20,0,20,15));
            

            VBox imageBox = new VBox(btnImageView, textField);
            imageBox.setAlignment(Pos.CENTER);
            imageBox.setPadding(new Insets(10,0,10,0));

            Text passwordTxt = new Text("Enter password:");
            passwordTxt.setFill(App.txtColor);
            passwordTxt.setFont(App.txtFont);

            PasswordField passwordField = new PasswordField();
            passwordField.setFont(App.txtFont);
            passwordField.setId("passField");

            HBox.setHgrow(passwordField, Priority.ALWAYS);

            HBox passwordBox = new HBox(passwordTxt, passwordField);
            passwordBox.setAlignment(Pos.CENTER_LEFT);
            passwordBox.setPadding(new Insets(10, 0, 0, 0));


            VBox.setMargin(passwordBox, new Insets(5, 10, 15, 20));

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
                File saveFile = saveChooser.showSaveDialog(passwordStage);
                if(saveFile != null){
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    
                    try {
                        Files.writeString(saveFile.toPath(), gson.toJson(dataObject));
                    } catch (IOException e1) {
                        Alert alert = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                        alert.setTitle("Error");
                        alert.setHeaderText("Error");
                        alert.initOwner(passwordStage);
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
                double width = newval.doubleValue();
                parametersBox.setPrefWidth(width-1);
            });

            
            
            PriceCurrency quoteCurrency = new PriceCurrency(ErgoDex.SIGUSD_ID, "SigUSD", 2, "EIP-004", m_networkType.toString());
            String quoteIdString = quoteCurrency.getTokenId(); //SpectrumFinance.ERG_ID;
            PriceAmount quotePriceAmount = new PriceAmount(quoteDecimal, quoteCurrency);
            boolean spectrumIsQuote = quoteIdString.equals(ErgoDex.SPF_ID);

            boolean feeIsSPF = false;
            byte[] spectrumId = Base16.decode(feeIsSPF ? ErgoDex.SPF_ID : ErgoCurrency.TOKEN_ID).get();
            //minValueForOrder = minerFee + uiFee + exFee + MinBoxValue

            long quoteAmountLong = quotePriceAmount.getLongAmount();
            
           // ErgoToken[] tokenArray = new ErgoToken[] {new ErgoToken(SpectrumFinance.SIGUSD_ID, quoteAmountLong)};

           




            
            Scene passwordScene = new Scene(layoutVBox, 800, 600);
            passwordScene.setFill(null);
            passwordScene.getStylesheets().add("/css/startWindow.css");
            passwordStage.setScene(passwordScene);


            passwordField.setOnAction(e -> {
                String pass = passwordField.getText();
             
                Task<JsonObject> task = new Task<JsonObject>() {
                    @Override
                    public JsonObject call() throws Exception {
                        Platform.runLater(()-> passwordStage.close());
                        Wallet wallet = Wallet.load(m_walletData.getWalleFile().toPath(), pass);
                       
                        Address address = getWalletAddress(wallet, walletAddress, m_networkType);
                
                        ErgoClient ergoClient = RestApiErgoClient.create(namedNodeUrl.getUrlString(), m_networkType, namedNodeUrl.getApiKey(), explorerUrl.getUrlString());
                        
                        
                        BlockchainDataSource dataSource = ergoClient != null ? ergoClient.getDataSource() : null;
                        BlockchainParameters parameters = dataSource != null ? dataSource.getParameters() : null;

                        List<BlockHeader> blockHeaderList =  dataSource != null ? dataSource.getLastBlockHeaders(1, true) : null;
                        BlockHeader blockHeader = blockHeaderList != null && blockHeaderList.size() > 0 ? blockHeaderList.get(0)  : null;
                        
                        int blockHeight = blockHeader != null ? blockHeader.getHeight() : -1;
                        long timeStamp = blockHeader != null ? blockHeader.getTimestamp() : -1;

                        ErgoNodeParmeters ergoNodeParameters = blockHeader != null ? new ErgoNodeParmeters(parameters, blockHeight, timeStamp, blockHeight) : null;
            
                        JsonObject ergoNodeParametersObject = ergoNodeParameters != null ? ergoNodeParameters.getJsonObject() : null;
                        if(ergoNodeParametersObject != null){
                            dataObject.remove("network");
                    
                            dataObject.add("network", ergoNodeParametersObject);
                        }
                        List<Address> addresses = wallet.addressStream(m_networkType).toList();

                        //swapBuy
                        /*BigInteger maxInteger = BigInteger.valueOf(Long.MAX_VALUE);
                        
                        ProveDlog refundProp = new DLogProverInput(maxInteger).publicImage();
                        long maxExFee = 1400L;
                        long exFeePerTokenNum = 22L;
                        long exFeePerTokenDenom = 100L;
                        long baseAmount = 1200L;
                        int feeNum = 996;
                        byte[] poolNFT = new byte[32];
                        Arrays.fill(poolNFT, (byte) 2);
                        byte[] redeemerPropBytes = new byte[32];
                        Arrays.fill(redeemerPropBytes, (byte) 1);
                        long minQuoteAmount = 800L;
                        byte[] spectrumId = new byte[32];
                        Arrays.fill(spectrumId, (byte) 3);
                        int feeDenom = 1000;
                        byte[] minerPropBytes = Base16.decode("1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304").get();
                        long maxMinerFee = 10000L;

                        String swapBuyV3SC = Files.readString(new File("/txt/SwapBuyV3.sc").toPath());*/
                        
                        /*export const uiFeeParams$ = new BehaviorSubject<UiFeeParams>({
                            address: '9fdmUutc4DhcqXAAyQeBTsw49PjEM4vuW9riQCHtXAoGEw3R11d',
                            minUiFee: 0.3,
                            uiFeePercent: 3,
                            uiFeeThreshold: 30,
                        });*/
                         
                        //swapSell
                        byte[] redeemerPropBytes = address.toPropositionBytes();

                        //ake2b256 -> (Coll[SByte$]) => Coll[SByte$])
                       
                        AddressInformation minerAddressInfo = new AddressInformation(ErgoDex.MINER_ADDRESS);
                        Address minerAddress = minerAddressInfo.getAddress();
                        //MinerPropBytes = Base16.decode("1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304").get()
                        byte[] minerBytes = minerAddress.toPropositionBytes();

                        Files.writeString(App.logFile.toPath(),"redeemerBytes: " + Base16.encode(redeemerPropBytes) +   "\nminerBytes:" + Base16.encode(minerBytes) + "\n" + "minerBytes:" + "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304" + "\n" ,StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                        UnsignedTransaction unsignedTx = ergoClient.execute(ctx -> {
                        
                            List<InputBox> boxesToSpend = BoxOperations.createForSenders(addresses, ctx)
                                .withAmountToSpend(ergoAmountLong)
                                .withFeeAmount(minerFeeAmount)
                                .loadTop();
                            //boxesToSpend.get(0).getTokens().
                            UnsignedTransactionBuilder txBuilder = ctx.newTxBuilder();
                
                            OutBoxBuilder newBoxBuilder = txBuilder.outBoxBuilder();
                            newBoxBuilder.value( ergoAmountLong );

                            /*if (tokensToSend.length > 0) {
                                newBoxBuilder.tokens(tokensToSend);
                            }*/
                           //recipient.getPublicKey()

                           byte[] quoteIdBytes = Base16.decode(quoteIdString).get();

                          
                           
                            ErgoContract ergoContract = ctx.compileContract(ConstantsBuilder.create()
                                .item("ExFeePerTokenDenom", 22222L)
                                .item("Delta",11111L)
                                .item("BaseAmount", ergoAmountLong)
                                .item("FeeNum",996)
                                .item("RefundProp", address.getPublicKey())
                                .item("SpectrumIsQuote", spectrumIsQuote)
                                .item("MaxExFee", 1400L)
                                .item("PoolNFT", quoteIdBytes)
                                .item("RedeemerPropBytes", redeemerPropBytes)
                                .item("QuoteId", quoteIdBytes)
                                .item("MinQuoteAmount", quoteAmountLong)
                                .item("SpectrumId", spectrumId)
                                .item("FeeDenom", 1000)
                                .item("MinerPropBytes", minerBytes)
                                
                                .build(),
                                smartContract);

                            try {
                                Files.writeString(App.logFile.toPath(), "ErgoTree:" + ergoContract.getErgoTree().bytesHex() + "\n" + "ErgoTree:" + "19fe04210400059cdb0205cead0105e01204c80f08cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d0404040604020400010105f01504000e2002020202020202020202020202020202020202020202020202020202020202020e2001010101010101010101010101010101010101010101010101010101010101010e20040404040404040404040404040404040404040404040404040404040404040405c00c0101010105f015060100040404020e2003030303030303030303030303030303030303030303030303030303030303030101040406010104d00f0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d804d601b2a4730000d6027301d6037302d6049c73037e730405eb027305d195ed92b1a4730693b1db630872017307d806d605db63087201d606b2a5730800d607db63087206d608b27207730900d6098c720802d60a95730a9d9c7e997209730b067e7202067e7203067e720906edededededed938cb27205730c0001730d93c27206730e938c720801730f92720a7e7310069573117312d801d60b997e7313069d9c720a7e7203067e72020695ed91720b731492b172077315d801d60cb27207731600ed938c720c017317927e8c720c0206720b7318909c7e8cb2720573190002067e7204069c9a720a731a9a9c7ec17201067e731b067e72040690b0ada5d9010b639593c2720b731cc1720b731d731ed9010b599a8c720b018c720b02731f7320", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e) {
                
                            }

                            try {
                                Files.writeString(App.logFile.toPath(), "ErgoTreeTemplate:" + Base16.encode(ergoContract.getErgoTree().template()) + "\n" + "ErgoTreeTemplate:" + "d804d601b2a4730000d6027301d6037302d6049c73037e730405eb027305d195ed92b1a4730693b1db630872017307d806d605db63087201d606b2a5730800d607db63087206d608b27207730900d6098c720802d60a95730a9d9c7e997209730b067e7202067e7203067e720906edededededed938cb27205730c0001730d93c27206730e938c720801730f92720a7e7310069573117312d801d60b997e7313069d9c720a7e7203067e72020695ed91720b731492b172077315d801d60cb27207731600ed938c720c017317927e8c720c0206720b7318909c7e8cb2720573190002067e7204069c9a720a731a9a9c7ec17201067e731b067e72040690b0ada5d9010b639593c2720b731cc1720b731d731ed9010b599a8c720b018c720b02731f7320", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e) {
                
                            }
                            
                            
                            newBoxBuilder.contract(ergoContract).build();
                                /*val rewardBox = OUTPUTS(1)
                                
                                */
                                OutBox newBox = newBoxBuilder.build();
                                

                             return txBuilder
                                    .addInputs(boxesToSpend.toArray(new InputBox[0]))
                                    .addOutputs(newBox)
                                    .fee(minerFeeAmount)
                                    .sendChangeTo(address)
                                    .build();
                       
                        });

                  
                        /*String txId = wallet.transact(ergoClient, ergoClient.execute(ctx -> {
                            try {
                                return wallet.key().signWithPassword(pass, ctx, unsignedTx, wallet.myAddresses.keySet());
                            } catch (WalletKey.Failure ex) {
        
                                return null;
                            }
                        }));
                        if(txId != null){

                            JsonObject resultObject = new JsonObject();
                            resultObject.addProperty("code", App.SUCCESS);
                            resultObject.addProperty("timeStamp", System.currentTimeMillis());
                            resultObject.addProperty("txId", txId);
                            resultObject.add("data", dataObject);
                        
                            return resultObject;
                        }else{
                            throw new Exception("Transaction signing failed");
                        }*/
                        
                        JsonObject resultObject = new JsonObject();
                        resultObject.addProperty("code", App.SUCCESS);
                        resultObject.addProperty("timeStamp", System.currentTimeMillis());
                        resultObject.addProperty("txId", "none");
                        resultObject.add("data", dataObject);
                    
                        return resultObject;
                    }
                };
        
                task.setOnFailed((failed->{
                    Utils.returnObject(Utils.getMsgObject(App.ERROR, failed.getSource().getException().toString()), getExecService(), onSucceeded, onFailed);
                   
                }));
        
                task.setOnSucceeded((succeeded)->{
                    Utils.returnObject(succeeded.getSource().getValue(), getExecService(), onSucceeded, onFailed);
            
                });
        
                Thread t = new Thread(task);
                t.setDaemon(true);
                t.start();
            
            
            });

       

            closeBtn.setOnAction(e -> {
             
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Transaction cancelled"), getExecService(), onSucceeded, onFailed);
                  
                activeThreadSemaphore.release();
                passwordStage.close();

            });



            passwordStage.setOnCloseRequest(e->{
            
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Transaction cancelled"), getExecService(), onSucceeded, onFailed);
             
                activeThreadSemaphore.release();
                passwordStage.close();
            });

            passwordScene.focusOwnerProperty().addListener((obs, oldval, newVal) -> {
                if (newVal != null && !(newVal instanceof PasswordField)) {
                    Platform.runLater(() -> passwordField.requestFocus());
                }
            });
            passwordStage.show();
    
            Platform.runLater(()->passwordField.requestFocus());
            
            ResizeHelper.addResizeListener(passwordStage, 400, 600, Double.MAX_VALUE, Double.MAX_VALUE);
        
            Task<Object> task = new Task<Object>() {
                @Override
                public Object call() throws InterruptedException {
                    activeThreadSemaphore.acquire();
                    activeThreadSemaphore.release();
                    return null;
                }
            };
            return getExecService().submit(task); 
        }

            
        return null;

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
                            case ErgoNetwork.MARKET_NETWORK:
                                switch(code){
                                
                                    case App.LIST_DEFAULT_CHANGED:
                                        getDefaultMarket(); 
                                    break;
                                
                                }
                            break;
                            case ErgoNetwork.TOKEN_MARKET_NETWORK:
                                switch(code){
                                    case App.LIST_DEFAULT_CHANGED:
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
            },0, POLLING_TIME, TimeUnit.MILLISECONDS);
       
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

            App.showGetTextInput("Address name", "Add Address", new Image("/assets/git-branch-outline-white-240.png"), textField, closeBtn, m_promptStage);
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
   






    


    public AddressData getAddress(String address){
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
                   
                    switch(code){
                        case App.LIST_CHANGED:
                        case App.LIST_UPDATED:
                            //updated
                            
                            m_ergoExchangeStatus.set(code);
                            break;
                        case App.STOPPED:
                            m_ergoExchangeStatus.set(code);
                        break;
                        case App.STARTED:
                            m_ergoExchangeStatus.set(code);
                        break;
                        case App.STARTING:
                            m_ergoExchangeStatus.set(code);
                        break;
                        case App.ERROR:
                        
                        break;
                    } 
                    
                }
            
                public void sendMessage(int code, long timestamp, String networkId, String msg){
                    if(code == App.ERROR){
                
                        m_statusMsg = msg;
                        m_ergoExchangeStatus.set(code);
                    }
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

                public void sendMessage(int code, long timeStamp, String poolId, Number num){
                   
                    switch(code){
                        case App.LIST_CHANGED:
                        case App.LIST_UPDATED:
                            //updated
                        
                            m_tokenExchangeStatus.set(App.LIST_UPDATED);
                            break;
                        case App.STOPPED:
                        m_tokenExchangeStatus.set(code);
                        break;
                        case App.STARTED:
                        m_tokenExchangeStatus.set(code);
                        break;
                        case App.STARTING:
                        m_tokenExchangeStatus.set(code);
                        break;
                        case App.ERROR:
                        
                        break;
                    } 
                    
                }
            
                public void sendMessage(int code, long timestamp, String networkId, String msg){
                    if(code == App.ERROR){
                
                        m_tokenStatusMsg = msg;
                        m_tokenExchangeStatus.set(code);
                    }
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
        
        JsonObject note = Utils.getCmdObject("getDefaultMarketInterface");
        note.addProperty("networkId", ErgoNetwork.MARKET_NETWORK);
        note.addProperty("locationId", getLocationId());

        Object obj = getErgoNetwork().sendNote(note);
        NoteInterface noteInterface =obj != null && obj instanceof NoteInterface ? (NoteInterface) obj : null;

      
        m_selectedMarket.set(noteInterface);
        
    }

    public void getDefaultTokenMarket(){
        
        JsonObject note = Utils.getCmdObject("getDefaultTokenInterface");
        note.addProperty("networkId", ErgoNetwork.MARKET_NETWORK);
        note.addProperty("locationId", getLocationId());
        Object obj = getErgoNetwork().sendNote(note);;
        NoteInterface noteInterface =obj != null && obj instanceof NoteInterface ? (NoteInterface) obj : null;
        m_selectedTokenMarket.set(noteInterface);
        
    }


    public boolean isTokenMarket(){
        NoteInterface noteInterface = m_selectedTokenMarket.get();
        return noteInterface != null && noteInterface.getConnectionStatus() == App.STARTED;
    }

    public boolean isErgoMarket(){
        NoteInterface noteInterface = m_selectedMarket.get();
        return noteInterface != null && noteInterface.getConnectionStatus() == App.STARTED;
    }

    public PriceQuote getErgoQuote(){
        NoteInterface exchangeInterface = m_selectedMarket.get();
        if(exchangeInterface != null && exchangeInterface.getConnectionStatus() == App.STARTED){
          
            JsonObject note = Utils.getCmdObject("getErgoUSDQuote");
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

        if(exchangeTokenInterface != null && exchangeTokenInterface.getConnectionStatus() == App.STARTED){

            JsonObject note = Utils.getCmdObject("getTokenQuoteInErg");

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
