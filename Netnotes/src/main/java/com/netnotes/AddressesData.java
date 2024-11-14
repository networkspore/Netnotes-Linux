package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.BlockHeader;
import org.ergoplatform.appkit.BlockchainDataSource;
import org.ergoplatform.appkit.BlockchainParameters;
import org.ergoplatform.appkit.ErgoClient;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.RestApiErgoClient;
import org.ergoplatform.appkit.UnsignedTransaction;

import com.satergo.Wallet;
import com.satergo.WalletKey;
import com.satergo.WalletKey.Failure;
import com.satergo.ergo.ErgoInterface;

import org.ergoplatform.appkit.ErgoToken;

import com.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
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

public class AddressesData {
    

    
    public final static NetworkInformation NOMARKET = new NetworkInformation("null", "(disabled)","/assets/bar-chart-150.png", "/assets/bar-chart-30.png", "No market selected" );
    public final static NetworkInformation[] ERGO_MARKETS = new NetworkInformation[]{
        NOMARKET,
        SpectrumFinance.getNetworkInformation(), 
        KucoinExchange.getNetworkInformation()
    };
    public final static NetworkInformation[] ERGO_TOKEN_MARKETS= new NetworkInformation[]{ 
        NOMARKET,
        SpectrumFinance.getNetworkInformation() 
    }; 

   
    private final NetworkType m_networkType;

  //  private final Wallet m_wallet;
    private ErgoWalletData m_walletData;

    private SimpleObjectProperty<AddressData> m_selectedAddressData = new SimpleObjectProperty<AddressData>(null);




    private ArrayList<AddressData> m_addressDataList;


    private ArrayList<PriceAmount> m_tokenAmounts = new ArrayList<>();
    




   // private SimpleObjectProperty<PriceQuote> m_currentQuote = new SimpleObjectProperty<>(null);
    public final static long POLLING_TIME = 3000;
    public final static int ADDRESS_IMG_HEIGHT = 40;
    public final static int ADDRESS_IMG_WIDTH = 350;
    public final static long QUOTE_TIMEOUT = POLLING_TIME*2;


    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>(LocalDateTime.now());

    

    private Stage m_promptStage = null;

    

    private ScheduledExecutorService m_schedualedExecutor = null;
    private ScheduledFuture<?> m_scheduledFuture = null;


    private long m_balanceTimestamp = 0;

    public AddressesData(String id,  ArrayList<AddressData> addressDataList, ErgoWalletData walletData, NetworkType networkType) {
        
      //  m_wallet = wallet;
        m_walletData = walletData;
        m_networkType = networkType;
        m_addressDataList = addressDataList;
        selectedAddressDataProperty().set(m_addressDataList.get(0));
        
        start();
    }

    private static ArrayList<PriceAmount>  getBalanceList(JsonObject json, boolean confirmed, NetworkType networkType){

        ArrayList<PriceAmount> assetsList = new ArrayList<>();

        JsonElement timeStampElement = json != null ? json.get("timeStamp") : null;
        JsonElement objElement = json != null ? json.get( confirmed ? "confirmed" : "unconfirmed") : null;

        long timeStamp = timeStampElement != null ? timeStampElement.getAsLong() : -1;
        if (objElement != null && timeStamp != -1) {

            JsonObject objObject = objElement.getAsJsonObject();
            JsonElement nanoErgElement = objObject.get("nanoErgs");
            
            long nanoErg = nanoErgElement != null && nanoErgElement.isJsonPrimitive() ? nanoErgElement.getAsLong() : 0;

            ErgoAmount ergoAmount = new ErgoAmount(nanoErg, networkType);
        
            assetsList.add(ergoAmount);
            
            JsonElement confirmedArrayElement = objObject.get("tokens");

            if(confirmedArrayElement != null && confirmedArrayElement.isJsonArray()){
                JsonArray confirmedTokenArray = confirmedArrayElement.getAsJsonArray();

                for (JsonElement tokenElement : confirmedTokenArray) {
                    JsonObject tokenObject = tokenElement.getAsJsonObject();

                    JsonElement tokenIdElement = tokenObject.get("tokenId");
                    JsonElement amountElement = tokenObject.get("amount");
                    JsonElement decimalsElement = tokenObject.get("decimals");
                    JsonElement nameElement = tokenObject.get("name");
                    JsonElement tokenTypeElement = tokenObject.get("tokenType");
                    
                    String tokenId = tokenIdElement.getAsString();
                    long amount = amountElement.getAsLong();
                    int decimals = decimalsElement.getAsInt();
                    String name = nameElement.getAsString();
                    String tokenType = tokenTypeElement.getAsString();
                    
                    PriceAmount tokenAmount = new PriceAmount(amount, new PriceCurrency(tokenId, name, decimals, tokenType, networkType.toString()));    
                    
                    assetsList.add(tokenAmount);
                    
                }
            }
     
             
        } 

        return assetsList;
    
    }

    public static ArrayList<PriceAmount> getAssetsList(JsonObject jsonObject, NetworkType networkType){
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

    public static ArrayList<PriceAmount> getAssetsList(JsonArray jsonArray, NetworkType networkType){
        ArrayList<PriceAmount> assetsList = new ArrayList<>();
        for(JsonElement element : jsonArray){

            if(!element.isJsonNull() && element.isJsonObject()){
                JsonObject assetJson = element.getAsJsonObject();
                assetsList.add(PriceAmount.getByAmountObject(assetJson, networkType));
            }
        }
        return assetsList;
    }

    public static PriceAmount getPriceAmountFromList(ArrayList<PriceAmount> priceList, String tokenId){
        if(tokenId != null && priceList != null){
            int size = priceList.size();
            for(int i = 0; i < size ; i++){
                PriceAmount amount = priceList.get(i);
                if(amount.getTokenId().equals(tokenId)){
                    return amount;
                }
            }
        }
        
        return null;
    }

    public ExecutorService getExecService(){
        return m_walletData.getErgoWallets().getNetworksData().getExecService();
    }


    public void sendAssets(JsonObject note,String locationString, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        
        JsonElement dataElement = note.get("data");
        int lblCol = 120;
        int rowHeight = 22;

        if(dataElement != null && dataElement.isJsonObject())
        {
            JsonObject dataObject = dataElement.getAsJsonObject();

            JsonElement networkElement = dataObject.get("network");
            JsonElement recipientElement = dataObject.get("recipient");
            JsonElement walletElement = dataObject.get("wallet");
            JsonElement feeElement = dataObject.get("fee");
            JsonElement assetsElement = dataObject.get("assets");
            JsonElement nodeElement = dataObject.get("node");
            JsonElement explorerElement = dataObject.get("explorer");

            if(networkElement == null || (networkElement != null && !networkElement.isJsonObject())){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "No network element provided"), getExecService(), onSucceeded, onFailed);
                return;
            }

            JsonObject networkObject = networkElement.getAsJsonObject();


            JsonElement networkHeightElement = networkObject.get("networkHeight");
            JsonElement networkTypeElement = networkObject.get("networkType");

            if(networkTypeElement == null || (networkTypeElement != null && networkTypeElement.isJsonNull())){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Network type not provided"), getExecService(), onSucceeded, onFailed);
                return;
            }

            int networkHeight = networkHeightElement.getAsInt();
            String networkTypeString = networkTypeElement.getAsString();

      
            if(!m_networkType.toString().toLowerCase().equals(networkTypeString.toLowerCase())){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Network type must be " + m_networkType.toString()), getExecService(), onSucceeded, onFailed);
                return;
            }

            JsonObject walletJson = walletElement != null && walletElement.isJsonObject() ? walletElement.getAsJsonObject() : null;
            
            if(walletJson == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "No wallet provided"), getExecService(), onSucceeded, onFailed);
                return;
            }

            JsonElement walletAddressElement = walletJson.get("address");

            if(walletAddressElement == null || (walletAddressElement != null && walletAddressElement.isJsonNull())){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "No wallet address provided"), getExecService(), onSucceeded, onFailed);
                return;
            }

            String walletAddress = walletAddressElement.getAsString();

            AddressData addressData = getAddress(walletAddress);

            if(addressData == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Address not found in this wallet"), getExecService(), onSucceeded, onFailed);
                return;
            }

            ArrayList<PriceAmount> balanceList = getBalanceList(addressData.getBalance(),true, m_networkType);

            JsonObject feeObject = feeElement != null && feeElement.isJsonObject() ? feeElement.getAsJsonObject() : null;
            

            if(feeObject == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "No fee provided"), getExecService(), onSucceeded, onFailed);
                return;
            }

            PriceAmount feeAmount = PriceAmount.getByAmountObject(feeObject, m_networkType);

            PriceAmount balanceFeeAvailable = getPriceAmountFromList(balanceList, feeAmount.getTokenId());
            
            if(balanceFeeAvailable == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Fee currency not available in address"), getExecService(), onSucceeded, onFailed);
                return;
            }

            if(assetsElement == null || assetsElement != null && !assetsElement.isJsonArray()){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Asset element required"), getExecService(), onSucceeded, onFailed);
                return;
            }

            ArrayList<PriceAmount> assetsList = getAssetsList(assetsElement.getAsJsonArray(), m_networkType);

            PriceAmount feeCurrencyAssetsAmount = getPriceAmountFromList(assetsList, feeAmount.getTokenId());
            if(assetsList != null && assetsList.size() > 0){
                BigDecimal totalFee = feeAmount.getBigDecimalAmount().add(feeCurrencyAssetsAmount != null ? feeCurrencyAssetsAmount.getBigDecimalAmount() : BigDecimal.ZERO);

                if(totalFee.compareTo(balanceFeeAvailable.getBigDecimalAmount()) == 1){
            
                    Utils.returnObject(Utils.getMsgObject(App.ERROR,  "Insufficent " + feeAmount.getCurrency().getName() + " - Required: " + totalFee ), getExecService(), onSucceeded, onFailed);           
                    return;
                }
            }else{
                Utils.returnObject(Utils.getMsgObject(App.ERROR,  "No assets transmitted"), getExecService(), onSucceeded, onFailed);           
                return;
            }

            for(PriceAmount sendAmount : assetsList){
                PriceAmount balanceAmount = getPriceAmountFromList(balanceList, sendAmount.getTokenId());
                if(sendAmount.getLongAmount() > balanceAmount.getLongAmount()){
                    Utils.returnObject(Utils.getMsgObject(App.ERROR,  "Insufficent " + balanceAmount.getCurrency().getName() + " - Required: " + sendAmount.getBigDecimalAmount()), getExecService(), onSucceeded, onFailed);           
                    return;
                }
            }
            
            JsonObject recipientObject = recipientElement != null && recipientElement.isJsonObject() ? recipientElement.getAsJsonObject() : null;

            if(recipientObject == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "No recipient provided"), getExecService(), onSucceeded, onFailed);
                return;
            }

            JsonElement recipientAddressElement = recipientObject.get("address");

            AddressInformation recipientAddressInfo = recipientAddressElement != null && !recipientAddressElement.isJsonNull() ? new AddressInformation(recipientAddressElement.getAsString().replaceAll("[^A-HJ-NP-Za-km-z1-9]", "")) : null;
            
            if(recipientAddressInfo == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "No recipient address provided"), getExecService(), onSucceeded, onFailed);
                return;
            }

            if(recipientAddressInfo.getAddress() == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Invalid recipient address"), getExecService(), onSucceeded, onFailed);
                return;
            }

            JsonObject nodeObject = nodeElement != null && nodeElement.isJsonObject() ? nodeElement.getAsJsonObject() : null;

            if(nodeObject == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "No node provided"), getExecService(), onSucceeded, onFailed);
                return;
            }

            JsonElement nodeUrlElement = nodeObject.get("url");
            JsonElement nodeApiKeyElement = nodeObject.get("apiKey");
            
            if(nodeUrlElement == null || (nodeUrlElement != null & nodeUrlElement.isJsonNull())){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "No node URL provided"), getExecService(), onSucceeded, onFailed);
                return;
            }

            String nodeUrl = nodeUrlElement.getAsString();
            String nodeApiKey = nodeApiKeyElement != null && !nodeApiKeyElement.isJsonNull() ? nodeApiKeyElement.getAsString() : "";

            JsonObject explorerObject = explorerElement != null && explorerElement.isJsonObject() ? explorerElement.getAsJsonObject() : null;

            if(explorerObject == null){
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "No explorer provided"), getExecService(), onSucceeded, onFailed);
                return;
            }

            JsonElement explorerUrlElement = explorerObject.get("url");

            String explorerUrl = explorerUrlElement != null && !explorerUrlElement.isJsonNull() ? explorerUrlElement.getAsString() : null;


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
            bodyScroll.viewportBoundsProperty().addListener((obs,oldval,newval)->{
                double width = newval.getWidth();
                parametersBox.setPrefWidth(width-1);
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
                        
                        
                        BlockchainDataSource dataSource = ergoClient != null ? ergoClient.getDataSource() : null;
                        BlockchainParameters parameters = dataSource != null ? dataSource.getParameters() : null;

                        List<BlockHeader> blockHeaderList =  dataSource != null ? dataSource.getLastBlockHeaders(1, true) : null;
                        BlockHeader blockHeader = blockHeaderList != null && blockHeaderList.size() > 0 ? blockHeaderList.get(0)  : null;
                        
                        int blockHeight = blockHeader != null ? blockHeader.getHeight() : -1;
                        long timeStamp = blockHeader != null ? blockHeader.getTimestamp() : -1;

                        ErgoNodeParmeters ergoNodeParameters =   new ErgoNodeParmeters(parameters, blockHeight, timeStamp, networkHeight) ;
            
                        JsonObject ergoNodeParametersObject = ergoNodeParameters.getJsonObject();

                        dataObject.remove("network");

                        dataObject.add("network", ergoNodeParametersObject);

                        PriceAmount ergoAmount = getPriceAmountFromList(assetsList, ErgoCurrency.TOKEN_ID);
                    
                        int tokenArraySize = ergoAmount == null ? assetsList.size() : assetsList.size()-1;

                        ErgoToken[] tokenArray =  new ErgoToken[tokenArraySize > 0 ? 0 :tokenArraySize];
                        int i = 0;

                        for(PriceAmount tokenAmount : assetsList){
                            String tokenId = tokenAmount.getTokenId();
                            if(!tokenId.equals(ErgoCurrency.TOKEN_ID)){
                                tokenArray[i] = new ErgoToken(tokenId, tokenAmount.getLongAmount());
                                i++;
                            }
                        }

                        UnsignedTransaction unsignedTx = ErgoInterface.createUnsignedTransaction(
                            ergoClient,
                            wallet.addressStream(m_networkType).toList(),
                            recipientAddressInfo.getAddress(),
                            ergoAmount.getLongAmount(),
                            feeAmount.getLongAmount(),
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

                            JsonObject resultObject = new JsonObject();
                            resultObject.addProperty("code", App.SUCCESS);
                            resultObject.addProperty("timeStamp", System.currentTimeMillis());
                            resultObject.addProperty("txId", txId);
                            resultObject.add("data", dataObject);
                        
                            return resultObject;
                        }else{
                            throw new Exception("Transaction signing failed");
                        }
                        
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
                  
               
                passwordStage.close();

            });



            passwordStage.setOnCloseRequest(e->{
            
                Utils.returnObject(Utils.getMsgObject(App.ERROR, "Transaction cancelled"), getExecService(), onSucceeded, onFailed);
             
                
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
        }

            


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

    public ErgoNetworkData getErgoNetworkdata(){
        return m_walletData.getErgoWallets().getErgoNetworkData();
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
            
            

            m_scheduledFuture = m_schedualedExecutor.scheduleAtFixedRate(()->update(),0, POLLING_TIME, TimeUnit.MILLISECONDS);
       
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
    }

    public SimpleObjectProperty<LocalDateTime> balanceUpdatedProperty(){
        return m_lastUpdated;
    }

    public ErgoWalletData getWalletData() {
        return m_walletData;
    }



 

    public SimpleObjectProperty<AddressData> selectedAddressDataProperty() {
        return m_selectedAddressData;
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
    public BigDecimal getPrice() {

        return BigDecimal.ZERO;//getValid() ? m_addressesData.selectedMarketData().get().priceQuoteProperty().get().getDoubleAmount()  : 0.0;
    }


    /*public VBox getAddressesBox(Button shutdown, Button itemShutdown) {

        VBox addressBox = new VBox();
        
        Runnable updateAdressBox = () ->{
            addressBox.getChildren().clear();
            for (int i = 0; i < m_addressDataList.size(); i++) {
                AddressData addressData = m_addressDataList.get(i);

                //addressData.prefWidthProperty().bind(m_addressBox.widthProperty());
                
                addressBox.getChildren().add(addressData.getAddressBox(itemShutdown));
            }
        };
        updateAdressBox.run();
        ListChangeListener<AddressData> addressDataListListener = (ListChangeListener.Change<? extends AddressData> c) ->updateAdressBox.run();
        m_addressDataList.addListener(addressDataListListener);

        shutdown.setOnAction(e->{
            m_addressDataList.removeListener(addressDataListListener);
        });
 
        return addressBox;
    }*/

  
    public PriceAmount getTokenAmount(String tokenId){
        for(int i =0 ; i< m_tokenAmounts.size(); i++){
            PriceAmount priceAmount = m_tokenAmounts.get(i);
            if(priceAmount.getTokenId().equals(tokenId)){
                return priceAmount;
            }
        }
        return null;
    }


    public PriceAmount getTokenAmount(PriceCurrency currency){
        PriceAmount priceAmount = getTokenAmount(currency.getTokenId());
        
        if(priceAmount != null){
            
            return priceAmount;
        }else{
            PriceAmount newTokenAmount = new PriceAmount(BigDecimal.ZERO, currency);
            m_tokenAmounts.add(newTokenAmount);
            return newTokenAmount;
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
      
        m_addressDataList.forEach(item->{
            item.shutdown();
        });
       

        stop();
         
     
    }

}
