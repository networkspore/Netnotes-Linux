package com.netnotes;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.netnotes.ErgoTransaction.TransactionType;
import com.satergo.ergo.ErgoInterface;
import com.utils.Utils;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener.Change;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;


public class AddressData extends Network {

    public static class AddressNotes{
         public final static String SEND_CMD = "SEND";
    }
   

    public static class AddressTabs {
        public final static String TRANSACTIONS = "Transactions";
        public final static String BALANCE = "Balance";
    }
    
    public final static int UPDATE_LIMIT = 10;
    public final static long QUOTE_TIMEOUT = 1000*60;
  
    private int m_index;
    private Address m_address;

    private final SimpleObjectProperty<ErgoAmount> m_ergoAmountProperty ;
    private final ArrayList<PriceAmount> m_confirmedTokensList = new ArrayList<>();

    private final ObservableList<ErgoTransaction> m_watchedTransactions = FXCollections.observableArrayList();
    private final SimpleObjectProperty<ErgoTransaction> m_selectedTransaction = new SimpleObjectProperty<>(null);
    private long m_quoteTimeout = QUOTE_TIMEOUT;
    private long m_unconfirmedNanoErgs = 0;
    private String m_priceBaseCurrency = "ERG";
    private String m_priceTargetCurrency = "USDT";
    private ArrayList<PriceAmount> m_unconfirmedTokensList = new ArrayList<>();
    private Stage m_addressStage = null;
    private File logFile = new File("netnotes-log.txt");
    private AddressesData m_addressesData;
    private int m_minImgWidth = 250;
    private int m_apiIndex = 0;
    private SimpleStringProperty m_selectedTab = new SimpleStringProperty("Balances");
    private SimpleObjectProperty<LocalDateTime> m_fieldsUpdated = new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> m_imgBuffer = new SimpleObjectProperty<Image>(null);
    private final String m_addressString;
    private ScheduledFuture<?> m_lastExecution = null;
    
    public AddressData(String name, int index, Address address, NetworkType networktype, AddressesData addressesData) {
        super(null, name, address.toString(), addressesData.getWalletData());
        m_ergoAmountProperty = new SimpleObjectProperty<ErgoAmount>(new ErgoAmount(0, networktype));
        
        m_addressesData = addressesData;
        m_index = index;
        m_address = address;
        m_addressString = address.toString();
        Tooltip addressTip = new Tooltip(getName());
        addressTip.setShowDelay(new javafx.util.Duration(100));
        addressTip.setFont(App.txtFont);


        setTooltip(addressTip);
        setPadding(new Insets(0, 10, 0, 10));
        setId("rowBtn");
        setText(getButtonText());

        setContentDisplay(ContentDisplay.LEFT);
        setAlignment(Pos.CENTER_LEFT);
        setTextAlignment(TextAlignment.LEFT);
       
        ChangeListener<PriceQuote> quoteChangeListener = (obs,oldval,newval)->updateBufferedImage();

        m_addressesData.selectedMarketData().addListener((obs, oldval, newVal) -> {
            if (oldval != null) {
                oldval.priceQuoteProperty().removeListener(quoteChangeListener);
               // oldval.shutdown();
            }
            if (newVal != null) {
                newVal.priceQuoteProperty().addListener(quoteChangeListener);
                updateBufferedImage();
            }
        });

        if(m_addressesData.selectedMarketData().get() != null){
            m_addressesData.selectedMarketData().get().priceQuoteProperty().addListener(quoteChangeListener);
            updateBufferedImage();
        }
        
        getNetworksData().timeCycleProperty().addListener((obs, oldval, newval)->{
            update();
        });

        getAddressInfo();
       
        m_ergoAmountProperty.addListener((obs,oldval,newval)->updateBufferedImage());
     

        update();
        updateBufferedImage();
    }


    public void getAddressInfo(){
        
        try {
            JsonObject json = m_addressesData.getWalletData().getErgoWallets().getAddressInfo(m_addressString, m_addressesData.getWalletData().getId());
          
            if(json != null){
              
                openAddressJson(json);
              
            }
        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
            try {
                Files.writeString(logFile.toPath(), "\nAddress cannot open Tx file: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {
                
            }
        }
    }

    public void update(){
        ErgoExplorerData explorerData = m_addressesData.selectedExplorerData().get();
        if( explorerData != null){
            updateBalance(explorerData);
            
        }
        
    }

 
  

    public ErgoTransaction[] getTxArray(JsonObject json){
        
        if(json != null){
            
            JsonElement itemsElement = json.get("items");

            if(itemsElement != null && itemsElement.isJsonArray()){
                JsonArray itemsArray = itemsElement.getAsJsonArray();
                int size = itemsArray.size();
                ErgoTransaction[] ergTxs = new ErgoTransaction[size];

                for(int i = 0; i < size ; i ++){
                    JsonElement txElement = itemsArray.get(i);
                    if(txElement != null && txElement.isJsonObject()){
                        JsonObject txObject = txElement.getAsJsonObject();

                        JsonElement txIdElement = txObject.get("id");
                        if(txIdElement != null && txIdElement.isJsonPrimitive()){
                            String txId = txIdElement.getAsString();
            
                            ergTxs[i] = new ErgoTransaction(txId, this, txObject);
                        }
                    }
                }
                return ergTxs;
            }

        }

        return new ErgoTransaction[0];
    }

    public SecretKey getSecretKey(){
        return getNetworksData().getAppData().appKeyProperty().get();
    }


  
    public void openAddressJson(JsonObject json){
        if(json != null){
         
            JsonElement txsElement = json.get("txs");
            JsonElement stageElement = json.get("stage");
            
            if(txsElement != null && txsElement.isJsonArray()){
                openWatchedTxs(txsElement.getAsJsonArray());
            }
           
            if (stageElement != null && stageElement.isJsonObject()) {

                JsonObject stageObject = stageElement.getAsJsonObject();
                JsonElement stageWidthElement = stageObject.get("width");
                JsonElement stageHeightElement = stageObject.get("height");
                JsonElement stagePrevWidthElement = stageObject.get("prevWidth");
                JsonElement stagePrevHeightElement = stageObject.get("prevHeight");

                JsonElement iconStyleElement = stageObject.get("iconStyle");
                JsonElement stageMaximizedElement = stageObject.get("maximized");

                boolean maximized = stageMaximizedElement == null ? false : stageMaximizedElement.getAsBoolean();

                setStageIconStyle(iconStyleElement.getAsString());
                setStagePrevWidth(DEFAULT_STAGE_WIDTH);
                setStagePrevHeight(DEFAULT_STAGE_HEIGHT);
                if (!maximized) {

                    setStageWidth(stageWidthElement.getAsDouble());
                    setStageHeight(stageHeightElement.getAsDouble());
                } else {
                    double prevWidth = stagePrevWidthElement != null && stagePrevWidthElement.isJsonPrimitive() ? stagePrevWidthElement.getAsDouble() : DEFAULT_STAGE_WIDTH;
                    double prevHeight = stagePrevHeightElement != null && stagePrevHeightElement.isJsonPrimitive() ? stagePrevHeightElement.getAsDouble() : DEFAULT_STAGE_HEIGHT;
                    setStageWidth(prevWidth);
                    setStageHeight(prevHeight);
                    setStagePrevWidth(prevWidth);
                    setStagePrevHeight(prevHeight);
                }
                setStageMaximized(maximized);
            }
        }
    }




    public void openWatchedTxs(JsonArray txsJsonArray){
        if(txsJsonArray != null){
       
            int size = txsJsonArray.size();

            for(int i = 0; i<size ; i ++){
                JsonElement txElement = txsJsonArray.get(i);

                if(txElement != null && txElement.isJsonObject()){
                    JsonObject txJson = txElement.getAsJsonObject();
                                                
                        JsonElement txIdElement = txJson.get("txId");
                        JsonElement parentAdrElement = txJson.get("parentAddress");
                        JsonElement timeStampElement = txJson.get("timeStamp");
                        JsonElement txTypeElement = txJson.get("txType");
                       // JsonElement nodeUrlElement = txJson.get("nodeUrl");

            
                        
                        if(txIdElement != null && txIdElement.isJsonPrimitive() 
                            && parentAdrElement != null && parentAdrElement.isJsonPrimitive() 
                            && timeStampElement != null && timeStampElement.isJsonPrimitive() 
                            && txTypeElement != null && txTypeElement.isJsonPrimitive()){

                            String txId = txIdElement.getAsString();
                            String txType = txTypeElement.getAsString();
                            String parentAdr = parentAdrElement.getAsString();

                            

                            if(parentAdr.equals(getAddressString())){
                                switch(txType){
                                    case TransactionType.SEND:
                                        
                                        try {
                                            ErgoSimpleSendTx simpleSendTx = new ErgoSimpleSendTx(txId, this, txJson);
                                            addWatchedTransaction(simpleSendTx, false);
                                        } catch (Exception e) {
                                            try {
                                                Files.writeString(logFile.toPath(), "\nCould not read tx json: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                            } catch (IOException e1) {
                                                
                                            }
                                        }
                                      
                                        break;
                                    default:
                                        ErgoTransaction ergTx = new ErgoTransaction(txId, this, txType);
                                        
                                        addWatchedTransaction(ergTx, false);
                                }
                            }
                        }
                    }
                }
            
        }
    }

    

    public JsonObject getAddressJson(){
        JsonObject json = new JsonObject();
        json.add("txs", getWatchedTxJsonArray());
        json.add("stage", getStageJson());
        return json;
    }




    public void addWatchedTransaction(ErgoTransaction transaction){
        addWatchedTransaction(transaction, true);
    }

    public void addWatchedTransaction(ErgoTransaction transaction, boolean save){
        if(getWatchedTx(transaction.getTxId()) == null){
            m_watchedTransactions.add(transaction);
            transaction.addUpdateListener((obs,oldval,newval)->{
            
                saveAddresInfo();
            });
            if(save){
                saveAddresInfo();
            }
        }
    }

   

    public JsonArray getWatchedTxJsonArray(){
        ErgoTransaction[] ergoTransactions =  getWatchedTxArray();

        JsonArray jsonArray = new JsonArray();
   
        for(int i = 0; i < ergoTransactions.length; i++){
            JsonObject tokenJson = ergoTransactions[i].getJsonObject();
            jsonArray.add(tokenJson);
        }
 
        return jsonArray; 
    }

    public ErgoTransaction getWatchedTx(String txId){
        if(txId != null){
            ErgoTransaction[] txs =  getWatchedTxArray();
            
            for(int i = 0; i < txs.length; i++){
                ErgoTransaction tx = txs[i];
                if(txId.equals(tx.getTxId())){
                    return tx;
                }
            }
        }
        return null;
    }

    public SimpleStringProperty selectedTabProperty(){
        return m_selectedTab;
    }

    public ObservableList<ErgoTransaction> watchedTxList(){
        return m_watchedTransactions;
    }

    public SimpleObjectProperty<ErgoTransaction> selectedTransaction(){
        return m_selectedTransaction;
    }

    public String getButtonText() {
        return "  " + getName() + "\n   " + getAddressString();
    }

    /*public boolean donate(){
        BigDecimal amountFullErg = dialog.showForResult().orElse(null);
		if (amountFullErg == null) return;
		try {
			Wallet wallet = Main.get().getWallet();
			UnsignedTransaction unsignedTx = ErgoInterface.createUnsignedTransaction(Utils.createErgoClient(),
					wallet.addressStream().toList(),
					DONATION_ADDRESS, ErgoInterface.toNanoErg(amountFullErg), Parameters.MinFee, Main.get().getWallet().publicAddress(0));
			String txId = wallet.transact(Utils.createErgoClient().execute(ctx -> {
				try {
					return wallet.key().sign(ctx, unsignedTx, wallet.myAddresses.keySet());
				} catch (WalletKey.Failure ex) {
					return null;
				}
			}));
			if (txId != null) Utils.textDialogWithCopy(Main.lang("transactionId"), txId);
		} catch (WalletKey.Failure ignored) {
			// user already informed
		}
    }*/
 /* return ;
        }); */
    public String getNodesId() {
        return "";
    }

 

 

    @Override
    public void open() {
        
        boolean open = showAddressStage();
        
        setOpen(open);
    }

    

    public VBox getBalanceBox(Scene scene){
        ErgoAmountBox ergoAmountBox = new ErgoAmountBox(ergoAmountProperty().get(), scene, getNetworksData().getHostServices());
        HBox.setHgrow(ergoAmountBox,Priority.ALWAYS);
      
        ergoAmountBox.priceAmountProperty().bind(ergoAmountProperty());
        
        

        HBox amountBoxPadding = new HBox(ergoAmountBox);
        amountBoxPadding.setPadding(new Insets(10,10,0,10));
        HBox.setHgrow(amountBoxPadding, Priority.ALWAYS);

        AmountBoxes amountBoxes = new AmountBoxes();
     
        amountBoxes.setPadding(new Insets(10,10,10,0));
        amountBoxes.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(amountBoxes, Priority.ALWAYS);
    
        
       
        Runnable updateAmountBoxes = ()->{
            long timestamp = System.currentTimeMillis();
            for(int i = 0; i < m_confirmedTokensList.size() ; i ++){
                PriceAmount tokenAmount = m_confirmedTokensList.get(i);
                AmountBox amountBox = amountBoxes.getAmountBox(tokenAmount.getCurrency().getTokenId());
                
                if(amountBox == null){
                    AmountBox newAmountBox = new AmountBox(tokenAmount, scene, m_addressesData);
                    newAmountBox.setTimeStamp(timestamp);
        
                    amountBoxes.add(newAmountBox);

                }else{
                    
                    amountBox.priceAmountProperty().set(tokenAmount);
                    amountBox.setTimeStamp(timestamp);
                }
            }

            amountBoxes.removeOld(timestamp);

            m_fieldsUpdated.set(LocalDateTime.now());
        };

        updateAmountBoxes.run();

        m_ergoAmountProperty.addListener((obs,oldval,newval)->updateAmountBoxes.run());

        ChangeListener<PriceQuote> priceQuoteListener = (obs,oldval,newval)->{
            ergoAmountBox.priceQuoteProperty().set(newval);
            m_fieldsUpdated.set(LocalDateTime.now());

        };   

        m_addressesData.selectedMarketData().addListener((obs, oldval, newVal) -> {
           
            if(oldval != null){
                oldval.priceQuoteProperty().removeListener(priceQuoteListener);
           
            }
            if (newVal != null) {
                newVal.priceQuoteProperty().addListener(priceQuoteListener);   
                ergoAmountBox.priceQuoteProperty().set(newVal.priceQuoteProperty().get());
            } 
        });
        if(m_addressesData.selectedMarketData().get() != null){
            PriceQuote priceQuote = m_addressesData.selectedMarketData().get().priceQuoteProperty().get();

            ergoAmountBox.priceQuoteProperty().set(priceQuote);
            
        }

        VBox balanceVBox = new VBox(amountBoxPadding, amountBoxes);
      
        return balanceVBox;
    }

  
    public ErgoTransaction[] getReverseTxArray(){
        ArrayList<ErgoTransaction> list = new ArrayList<>(m_watchedTransactions);
        Collections.reverse(list);

        int size = list.size();
        ErgoTransaction[] txArray = new ErgoTransaction[size];
        txArray = list.toArray(txArray);
        return txArray;
    }

    public ErgoTransaction[] getWatchedTxArray(){
        int size = m_watchedTransactions.size();
        ErgoTransaction[] txArray = new ErgoTransaction[size];
        txArray = m_watchedTransactions.toArray(txArray);
        return txArray;
    }

    public void removeTransaction(String txId){
        removeTransaction(txId, true);
    }

     public void removeTransaction(String txId, boolean save){
        
        ErgoTransaction ergTx = getWatchedTx(txId);
        if(ergTx != null){
            m_watchedTransactions.remove(ergTx);
        }
        if(save){
            saveAddresInfo();
        }
    }

    

    public VBox getTransactionsContent(Scene scene){
    
        ImageView watchedIcon = IconButton.getIconView(new Image("/assets/star-30.png"), 30);
        
        Text watchedText = new Text("Watched");
        watchedText.setFont(App.txtFont);
        watchedText.setFill(App.txtColor);

        TextField newTxIdField = new TextField();
        newTxIdField.setPromptText("Add Transaction Id");
        newTxIdField.setPrefWidth(200);
        newTxIdField.setId("numField");
       
        Button addTxId = new Button("Add");
        addTxId.setOnAction(e->{
            String newTxId = newTxIdField.getText();
            String hexString =  newTxId.replaceAll("[^0-9a-fA-F]", "");

            if(newTxId.length() == 64 && hexString.equals(newTxId)){
                ErgoExplorerData explorerData = m_addressesData.selectedExplorerData().get();

                ErgoTransaction userTx = new ErgoTransaction(newTxIdField.getText(),this,TransactionType.USER);
                if(explorerData != null){
                    userTx.doUpdate(explorerData, true);
                }
                addWatchedTransaction(userTx);
                
            }else{
                Alert a = new Alert(AlertType.NONE, "Notice: Transaction amounts for transactions which do not involve this address may not be displayed.", ButtonType.OK);
                a.initOwner(m_addressStage);
                a.setHeaderText("Invalid Transaction Id");
                a.setTitle("Invalid Transaction Id");
                a.show();
            }
            newTxIdField.setText("");
        });
        newTxIdField.setOnAction(e->addTxId.fire());
      

        Region watchedSpacerRegion = new Region();
        HBox.setHgrow(watchedSpacerRegion, Priority.ALWAYS);

        HBox watchedHeadingBox = new HBox(watchedIcon, watchedText,watchedSpacerRegion, newTxIdField, addTxId);
        HBox.setHgrow(watchedHeadingBox, Priority.ALWAYS);
        watchedHeadingBox.setId("headingBox");
        watchedHeadingBox.setMinHeight(40);
        watchedHeadingBox.setAlignment(Pos.CENTER_LEFT);
        watchedHeadingBox.setPadding(new Insets(0,15,0,5));

        VBox watchedTxsBox = new VBox();
        HBox.setHgrow(watchedTxsBox, Priority.ALWAYS);

        Text allText = new Text("All");
        allText.setFont(App.txtFont);
        allText.setFill(App.txtColor);

        Region spacerRegion = new Region();
        HBox.setHgrow(spacerRegion, Priority.ALWAYS);

        Text offsetText = new Text("Start at: ");
        offsetText.setFont(App.titleFont);
        offsetText.setFill(App.altColor);
        
        TextField offsetField = new TextField("0");
        offsetField.setPromptText("Offset");
        offsetField.setPrefWidth(60);
        offsetField.setId("numField");
        offsetField.textProperty().addListener((obs,oldval,newval)->{
            String numStr = newval.replaceAll("[^0-9]", "");
            numStr = numStr.equals("") ? "0" : numStr;
            long longNum = Long.parseLong(numStr);
            int num = longNum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) longNum;
            offsetField.setText(num + "");

        });

        Text limitText = new Text("Max: ");
        limitText.setFont(App.titleFont);
        limitText.setFill(App.altColor);

        TextField limitField = new TextField("50");
        limitField.setPrefWidth(60);
        limitField.setPromptText("Limit");
        limitField.setId("numField");
        limitField.textProperty().addListener((obs,oldval,newval)->{
            String numStr = newval.replaceAll("[^0-9]", "");
            numStr = numStr.equals("") ? "0" : numStr;
            int maxInt = Integer.parseInt(numStr);
            maxInt = maxInt > 500 ? 500 : maxInt;
            limitField.setText(maxInt + "");
        });

        Button getTxsBtn = new Button("Get");

        Region fieldSpacer = new Region();
        fieldSpacer.setMinWidth(5);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);

        Text progressText = new Text("Getting transactions...");
        progressText.setFill(App.txtColor);
        progressText.setFont(App.titleFont);

        HBox progressTextBox = new HBox(progressText);
        HBox.setHgrow(progressTextBox, Priority.ALWAYS);
        progressTextBox.setMinHeight(30);
        progressTextBox.setAlignment(Pos.CENTER);


        progressBar.progressProperty().addListener((obs,oldval,newval)->{
            double value = newval.doubleValue();
            if(value > 0){
                progressText.setText("(" + String.format("%.1f", value * 100) + "%)");
            }else{
                progressText.setText("Getting Transactions...");
            }
        });
    
        VBox progressBarBox = new VBox(progressBar, progressTextBox);
        HBox.setHgrow(progressBarBox, Priority.ALWAYS);
        progressBarBox.setAlignment(Pos.CENTER);
        progressBarBox.setMinHeight(150);

        HBox allHeadingBox = new HBox(allText, spacerRegion,offsetText, offsetField,limitText, limitField,fieldSpacer, getTxsBtn);
        HBox.setHgrow(allHeadingBox, Priority.ALWAYS);
        allHeadingBox.setId("headingBox");
        allHeadingBox.setMinHeight(40);
        allHeadingBox.setAlignment(Pos.CENTER_LEFT);
        allHeadingBox.setPadding(new Insets(0,15,0,15));

        VBox allTxsBox = new VBox();
        HBox.setHgrow(allTxsBox, Priority.ALWAYS);

        Runnable updateWatchedTxs = ()->{
            
            ErgoExplorerData explorerData = m_addressesData.selectedExplorerData().get();
            if(explorerData != null){
                ErgoTransaction[] watchedTxs = getWatchedTxArray();
                for(int i = 0; i < watchedTxs.length ; i++){
                    watchedTxs[i].doUpdate(explorerData, false);
                }
            }
        
        };

        getNetworksData().timeCycleProperty().addListener((obs,oldval,newval)->updateWatchedTxs.run());

        updateWatchedTxs.run();

        Runnable updateTxList = () ->{
            
            ErgoTransaction[] txArray = getReverseTxArray();
        
            watchedTxsBox.getChildren().clear();
       
            for(int i = 0; i < txArray.length ; i++){
                ErgoTransaction ergTx = txArray[i];

                watchedTxsBox.getChildren().add(ergTx.getTxBox());
                
                
            }
            



            if(watchedTxsBox.getChildren().size() == 0){
                Text noSavedTxs = new Text("No saved transactions");
                noSavedTxs.setFill(App.altColor);
                noSavedTxs.setFont(App.txtFont);

                HBox emptywatchedBox = new HBox(noSavedTxs);
                HBox.setHgrow(emptywatchedBox, Priority.ALWAYS);
                emptywatchedBox.setMinHeight(40);
                emptywatchedBox.setAlignment(Pos.CENTER);

                watchedTxsBox.getChildren().add(emptywatchedBox);
            }

            
        };

        updateTxList.run();     
        /*getNetworksData().timeCycleProperty().addListener((obs,oldval,newVal)->{
            int watchedTxSize = m_watchedTransactions.size();
            if(watchedTxSize > 0){
                ErgoExplorerData explorerData = m_addressesData.selectedExplorerData().get();
                for(int i = 0; i < watchedTxSize ; i++){

                }
            }
        }); */
        

        m_watchedTransactions.addListener((ListChangeListener.Change<? extends ErgoTransaction> c)->updateTxList.run());
        SimpleObjectProperty<ErgoTransaction[]> txsProperty = new SimpleObjectProperty<>(new ErgoTransaction[0]);

        Runnable updateAllTxList = ()->{
            ErgoTransaction[] txArray = txsProperty.get();
            
            allTxsBox.getChildren().clear();

            for(int i = 0; i < txArray.length ; i++){
               
                allTxsBox.getChildren().add(txArray[i].getTxBox());
                
            }
            
            if(allTxsBox.getChildren().size() == 0){
                Text noSavedTxs = new Text("No transactions available");
                noSavedTxs.setFill(App.altColor);
                noSavedTxs.setFont(App.txtFont);

                HBox emptywatchedBox = new HBox(noSavedTxs);
                HBox.setHgrow(emptywatchedBox, Priority.ALWAYS);
                emptywatchedBox.setMinHeight(40);
                emptywatchedBox.setAlignment(Pos.CENTER);

                allTxsBox.getChildren().add(emptywatchedBox);
            }
            if(allTxsBox.getChildren().contains(progressBarBox)){
                allTxsBox.getChildren().remove(progressBarBox);
            }
        };

       
       

    
     
        Region allSpacer = new Region();
        allSpacer.setMinHeight(10);

        VBox contentBox = new VBox(watchedHeadingBox, watchedTxsBox, allSpacer, allHeadingBox, allTxsBox);
        contentBox.setPadding(new Insets(10));
       

        getTxsBtn.setOnAction(e->{
            ErgoExplorerData explorerData = m_addressesData.selectedExplorerData().get();
            if(explorerData != null){
                int offset = Integer.parseInt(offsetField.getText());
                int limit = Integer.parseInt(limitField.getText());
                allTxsBox.getChildren().clear();
                
                allTxsBox.getChildren().add(progressBarBox);
                
                explorerData.getAddressTransactions(getAddressString(), offset, limit ,(onSucceeded)->{
                    Object sourceObject = onSucceeded.getSource().getValue();
                    if(sourceObject != null && sourceObject instanceof JsonObject){
                        JsonObject txsJson = (JsonObject) sourceObject;

                        ErgoTransaction[] txArray = getTxArray(txsJson);
                        Platform.runLater(()-> {
                            txsProperty.set(txArray);
                            updateAllTxList.run();
                        });
                    }else{
                        Platform.runLater(()->{
                            txsProperty.set(new ErgoTransaction[0]);
                            updateAllTxList.run();
                        });
                    }
                }, (onFailed)->{
                    Platform.runLater(()->{
                        txsProperty.set(new ErgoTransaction[0]);
                        updateAllTxList.run();
                    });
                }, progressBar);
            }else{
                Alert a = new Alert(AlertType.NONE, "Select an Ergo explorer", ButtonType.OK);
                a.initOwner(m_addressStage);
                a.setTitle("Required: Ergo Explorer");
                a.setHeaderText("Required: Ergo Explorer");
                a.show();
            }
        });
        
        getTxsBtn.fire();
        
        return contentBox;
    }
    public AddressesData getAddressesData(){
        return m_addressesData;
    }

    private boolean showAddressStage() {
        if (m_addressStage == null) {
            String titleString = getName() + " - " + m_address.toString() + " - (" + getNetworkType().toString() + ")";
            m_addressStage = new Stage();
            m_addressStage.getIcons().add(ErgoWallets.getAppIcon());
            m_addressStage.setResizable(false);
            m_addressStage.initStyle(StageStyle.UNDECORATED);
            

    
            Button closeBtn = new Button();

            addShutdownListener((obs, oldVal, newVal) -> {
                Platform.runLater(()->closeBtn.fire());
           
            });

            Button maximizeBtn = new Button();

            HBox titleBox = App.createTopBar(ErgoWallets.getSmallAppIcon(), maximizeBtn,  closeBtn, m_addressStage);
 
            m_addressStage.setTitle(titleString);

            double imageWidth = App.MENU_BAR_IMAGE_WIDTH;

            
            Tooltip nodesTip = new Tooltip("Select node");
            nodesTip.setShowDelay(new javafx.util.Duration(50));
            nodesTip.setFont(App.txtFont);


            BufferedMenuButton nodesBtn = new BufferedMenuButton("/assets/ergoNodes-30.png", imageWidth);
            nodesBtn.setPadding(new Insets(2, 0, 0, 0));
            nodesBtn.setTooltip(nodesTip);
            


            Runnable updateNodeBtn = () ->{
            
                ErgoNodes ergoNodes = (ErgoNodes)m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID);
                ErgoNodeData nodeData = m_addressesData.selectedNodeData().get();
            
                if(nodeData != null && ergoNodes != null){

                nodesTip.setText(nodeData.getName());
                
                }else{
                    
                    if(ergoNodes == null){
                        nodesTip.setText("(install 'Ergo Nodes')");
                    }else{
                        nodesTip.setText("Select node...");
                    }
                }
            
                
            };
            
            Runnable getAvailableNodeMenu = () ->{
                ErgoNodes ergoNodes = (ErgoNodes)m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID);
                if(ergoNodes != null){
                    ergoNodes.getErgoNodesList().getMenu(nodesBtn, m_addressesData.selectedNodeData());
                    nodesBtn.setId("menuBtn");
                }else{
                    nodesBtn.getItems().clear();
                    nodesBtn.setId("menuBtnDisabled");
                
                }
                updateNodeBtn.run();
            };

            m_addressesData.selectedNodeData().addListener((obs, oldval, newval)->{
                    updateNodeBtn.run();
        
            m_addressesData.getWalletData().setNodesId(newval == null ? null : newval.getId());
            
            });

            
            Tooltip explorerTip = new Tooltip("Select explorer");
            explorerTip.setShowDelay(new javafx.util.Duration(50));
            explorerTip.setFont(App.txtFont);



            BufferedMenuButton explorerBtn = new BufferedMenuButton("/assets/ergo-explorer-30.png", imageWidth);
            explorerBtn.setPadding(new Insets(2, 0, 0, 2));
            explorerBtn.setTooltip(explorerTip);

            Runnable updateExplorerBtn = () ->{
                ErgoExplorers ergoExplorers = (ErgoExplorers) m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);

                ErgoExplorerData explorerData = m_addressesData.selectedExplorerData().get();
            
            
                if(explorerData != null && ergoExplorers != null){
                
                    explorerTip.setText("Ergo Explorer: " + explorerData.getName());
                    

                }else{
                    
                    if(ergoExplorers == null){
                        explorerTip.setText("(install 'Ergo Explorer')");
                    }else{
                        explorerTip.setText("Select Explorer...");
                    }
                }
                
            };
            Runnable getAvailableExplorerMenu = () ->{
            
                ErgoExplorers ergoExplorers = (ErgoExplorers) m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);
                if(ergoExplorers != null){
                    explorerBtn.setId("menuBtn");
                    ergoExplorers.getErgoExplorersList().getMenu(explorerBtn, m_addressesData.selectedExplorerData());
                }else{
                    explorerBtn.getItems().clear();
                    explorerBtn.setId("menuBtnDisabled");
                
                }
                updateExplorerBtn.run();
            };    

            m_addressesData.selectedExplorerData().addListener((obs, oldval, newval)->{
                m_addressesData.getWalletData().setExplorer(newval == null ? null : newval.getId());
                updateExplorerBtn.run();
            });

            Tooltip marketsTip = new Tooltip("Select market");
            marketsTip.setShowDelay(new javafx.util.Duration(50));
            marketsTip.setFont(App.txtFont);

            BufferedMenuButton marketsBtn = new BufferedMenuButton("/assets/ergoChart-30.png", imageWidth);
            marketsBtn.setPadding(new Insets(2, 0, 0, 0));
            marketsBtn.setTooltip(marketsTip);

         

            m_addressesData.selectedMarketData().addListener((obs, oldval, newVal) -> {
               
           
                if (newVal != null) {
                    marketsTip.setText("Ergo Markets: " + newVal.getName());
                    
                } else {
                    NoteInterface ergoMarkets = m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID);
    
                    if (ergoMarkets == null) {
                        marketsTip.setText("(install 'Ergo Markets')");
                    } else {
                        marketsTip.setText("Select market...");
                    }
                }
            });
        

            Runnable getAvailableMarketsMenu = () -> {
                ErgoMarkets ergoMarkets = (ErgoMarkets) m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID);
                
                if (ergoMarkets != null) {
                    marketsBtn.setId("menuBtn");
    
                    ergoMarkets.getErgoMarketsList().getMenu(marketsBtn, m_addressesData.selectedMarketData());
                } else {
                    marketsBtn.getItems().clear();
                    marketsBtn.setId("menuBtnDisabled");
                }
               
            };
        
            

            Tooltip tokensTip = new Tooltip("Ergo Tokens");
            tokensTip.setShowDelay(new javafx.util.Duration(50));
            tokensTip.setFont(App.mainFont);


            BufferedMenuButton tokensBtn = new BufferedMenuButton("/assets/diamond-30.png", imageWidth);
            tokensBtn.setPadding(new Insets(2, 0, 0, 0));
        
            

           
            Region seperator1 = new Region();
            seperator1.setMinWidth(1);
            seperator1.setId("vSeperatorGradient");
            VBox.setVgrow(seperator1, Priority.ALWAYS);

            Region seperator2 = new Region();
            seperator2.setMinWidth(1);
            seperator2.setId("vSeperatorGradient");
            VBox.setVgrow(seperator2, Priority.ALWAYS);

            Region seperator3 = new Region();
            seperator3.setMinWidth(1);
            seperator3.setId("vSeperatorGradient");
            VBox.setVgrow(seperator3, Priority.ALWAYS);

            HBox rightSideMenu = new HBox(nodesBtn, seperator1, explorerBtn, seperator2, marketsBtn, seperator3, tokensBtn);
            rightSideMenu.setId("rightSideMenuBar");
            rightSideMenu.setPadding(new Insets(0, 0, 0, 0));
            rightSideMenu.setAlignment(Pos.CENTER_RIGHT);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Tooltip sendTip = new Tooltip("Send");
            sendTip.setShowDelay(new javafx.util.Duration(100));

            BufferedButton sendBtn = new BufferedButton("/assets/arrow-send-white-30.png", imageWidth);
            sendBtn.setTooltip(sendTip);
            sendBtn.setId("menuBtn");
            sendBtn.setUserData("sendButton");

    
            HBox menuBar = new HBox(sendBtn, spacer, rightSideMenu);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 5));

            VBox layoutVBox = new VBox();

            double stageWidth = getStageMaximized() ? getStagePrevWidth() : getStageWidth();
            double stageHeight = getStageMaximized() ? getStagePrevHeight() : getStageHeight();
            
            Scene addressScene = new Scene(layoutVBox, stageWidth, stageHeight);
            addressScene.setFill(null);
            Text addressText = new Text(getName() + ": ");
            addressText.setFont(App.txtFont);
            addressText.setFill(App.txtColor);
        

            final String addressString = m_address.toString();
   

            TextField addressField = new TextField(addressString);
            addressField.setId("addressField");
            addressField.setEditable(false);
            addressField.setPrefWidth(Utils.measureString(addressString, new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 14)) + 30);

            Tooltip copiedTooltip = new Tooltip("copied");

            BufferedButton copyAddressBtn = new BufferedButton("/assets/copy-30.png", App.MENU_BAR_IMAGE_WIDTH);
            copyAddressBtn.setOnAction(e->{
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(m_address.toString());
                clipboard.setContent(content);

                Point2D p = copyAddressBtn.localToScene(0.0, 0.0);

                copiedTooltip.show(
                    copyAddressBtn,  
                    p.getX() + copyAddressBtn.getScene().getX() + copyAddressBtn.getScene().getWindow().getX(), 
                    (p.getY()+ copyAddressBtn.getScene().getY() + copyAddressBtn.getScene().getWindow().getY())-copyAddressBtn.getLayoutBounds().getHeight()
                    );
                PauseTransition pt = new PauseTransition(Duration.millis(1600));
                pt.setOnFinished(ptE->{
                    copiedTooltip.hide();
                });
                pt.play();
            });

            HBox addressBox = new HBox(addressText, addressField, copyAddressBtn);
            HBox.setHgrow(addressBox, Priority.ALWAYS);
            addressBox.setAlignment(Pos.CENTER_LEFT);
            addressBox.setPadding(new Insets(0,15,0,5));
            addressBox.setMinHeight(40);

            Button balanceBtn = new Button(AddressTabs.BALANCE);
            balanceBtn.setId("tabBtnSelected");
            balanceBtn.setOnAction(e->{
                m_selectedTab.set(AddressTabs.BALANCE);
            });

            Button transactionsBtn = new Button(AddressTabs.TRANSACTIONS);
            transactionsBtn.setId("tabBtn");
            transactionsBtn.setOnAction(e->{
                m_selectedTab.set(AddressTabs.TRANSACTIONS);
            });

            Region txBtnSpacer = new Region();
            txBtnSpacer.setMinWidth(5);

            HBox addressTabsBox = new HBox(balanceBtn, txBtnSpacer, transactionsBtn);
            addressTabsBox.setPadding(new Insets(0, 0, 0, 0));
            addressTabsBox.setId("tabsBox");


        

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setId("bodyBox");
            scrollPane.setPadding(new Insets(5,0,5, 0));
      
           

            Runnable updateTab = ()->{
                String selectedTab = m_selectedTab.get() == null ? AddressTabs.BALANCE : m_selectedTab.get();
                switch(selectedTab){
                    case AddressTabs.TRANSACTIONS:
                        balanceBtn.setId("tabBtn");
                        transactionsBtn.setId("tabBtnSelected");

                        VBox transactionsContent = getTransactionsContent(addressScene);
                        transactionsContent.prefWidthProperty().bind(scrollPane.widthProperty());

                        scrollPane.setContent(transactionsContent);
                    break;
                    case AddressTabs.BALANCE:
                    default:
                        balanceBtn.setId("tabBtnSelected");
                        transactionsBtn.setId("tabBtn");
                        VBox balanceContent = getBalanceBox(addressScene);
                        balanceContent.prefWidthProperty().bind(scrollPane.widthProperty());
                        scrollPane.setContent(balanceContent);
                    break;
                         
                }
                
            };
        
            updateTab.run();

            m_selectedTab.addListener((obs, oldval, newval)->updateTab.run());

            

            Text updatedTxt = new Text("Updated:");
            updatedTxt.setFill(App.altColor);
            updatedTxt.setFont(Font.font("OCR A Extended", 10));

            TextField lastUpdatedField = new TextField();
            lastUpdatedField.setPrefWidth(190);
            lastUpdatedField.setId("smallPrimaryColor");

            m_fieldsUpdated.addListener((obs,oldval,newval)->{
                LocalDateTime time = newval == null ? LocalDateTime.now() : newval;
                lastUpdatedField.setText(Utils.formatDateTimeString(time));
            });

            HBox updateBox = new HBox(updatedTxt, lastUpdatedField);
            updateBox.setPadding(new Insets(2,2,2,0));
            updateBox.setAlignment(Pos.CENTER_RIGHT);


            Region botSpacer = new Region();
            botSpacer.setMinHeight(5);
            
            VBox bodyBox = new VBox(addressBox, addressTabsBox, scrollPane, botSpacer);
            bodyBox.setId("bodyBox");
            bodyBox.setPadding(new Insets(0,15,5,15));
            HBox.setHgrow(bodyBox,Priority.ALWAYS);

            VBox menuPaddingBox = new VBox(menuBar);
            menuPaddingBox.setPadding(new Insets(0,0,4,0));


            VBox bodyPaddingBox = new VBox(menuPaddingBox, bodyBox);
            bodyPaddingBox.setPadding(new Insets(0,4, 0, 4));
            HBox.setHgrow(bodyPaddingBox,Priority.ALWAYS);

     
            layoutVBox.getChildren().addAll(titleBox, bodyPaddingBox,  updateBox);
            VBox.setVgrow(layoutVBox, Priority.ALWAYS);
   

            addressScene.getStylesheets().add("/css/startWindow.css");

            m_addressStage.setScene(addressScene);
            m_addressStage.show();

            scrollPane.prefViewportHeightProperty().bind(addressScene.heightProperty().subtract(titleBox.heightProperty()).subtract(addressTabsBox.heightProperty()).subtract(updateBox.heightProperty()).subtract(addressBox.heightProperty()));
         
   
            Rectangle rect = getNetworksData().getMaximumWindowBounds();

            ResizeHelper.addResizeListener(m_addressStage, 200, 250, rect.getWidth(), rect.getHeight());

            maximizeBtn.setOnAction(maxEvent -> {
                boolean maximized = m_addressStage.isMaximized();
                if (!maximized) {
                    setStagePrevWidth(m_addressStage.getWidth());
                    setStagePrevHeight(m_addressStage.getHeight());
                }
                setStageMaximized(!maximized);
                m_addressStage.setMaximized(!maximized);
            
            });

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });

             Runnable setUpdated = () -> {
                saveAddresInfo();
            };

            addressScene.widthProperty().addListener((obs, oldVal, newVal) -> {
                setStageWidth(newVal.doubleValue());

                if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                    m_lastExecution.cancel(false);
                }

                m_lastExecution = executor.schedule(setUpdated, EXECUTION_TIME, TimeUnit.MILLISECONDS);
            });

            addressScene.heightProperty().addListener((obs, oldVal, newVal) -> {
                setStageHeight(newVal.doubleValue());

                if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                    m_lastExecution.cancel(false);
                }

                m_lastExecution = executor.schedule(setUpdated, EXECUTION_TIME, TimeUnit.MILLISECONDS);
            });

            sendBtn.setOnAction((actionEvent) -> {
                Button closeStageBtn = new Button();
                Scene sendScene = m_addressesData.getSendScene(addressScene, m_addressStage, closeStageBtn);
                if (sendScene != null) {
                     
                    m_addressStage.setScene(sendScene);
                    Rectangle currentRect = getNetworksData().getMaximumWindowBounds();
                    ResizeHelper.addResizeListener(m_addressStage, ErgoWalletData.MIN_WIDTH, 500, currentRect.getWidth(), currentRect.getHeight());
                    closeStageBtn.setOnAction(e->{
                        m_addressStage.close();
                        m_addressStage = null;
                    });

                    m_addressStage.setOnCloseRequest(e->{
                        closeStageBtn.fire();
                    });
                }else{
                    Alert b = new Alert(AlertType.ERROR, "Unable open 'Send' window. Please try again.", ButtonType.OK);
                    b.show();
                }
            });


             ChangeListener<LocalDateTime> changeListener = (obs, oldval, newval)->{
                ErgoExplorerData explorerData = m_addressesData.selectedExplorerData().get();
                if(explorerData != null){
                    updateBalance(explorerData);
                }
            };

            SimpleBooleanProperty isListeningToTokenList = new SimpleBooleanProperty(false);

             Runnable updateTokensMenu = ()->{
                tokensBtn.getItems().clear();
                ErgoTokens ergoTokens = (ErgoTokens) m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoTokens.NETWORK_ID);  
                ErgoTokensList ergoTokensList = m_addressesData.tokensListProperty().get();
                boolean isEnabled = ergoTokensList != null;

                if(ergoTokens != null){
                    tokensBtn.setId("menuBtn");
                    MenuItem tokensEnabledItem = new MenuItem("Enabled" + (isEnabled ? " (selected)" : ""));
                    tokensEnabledItem.setOnAction(e->{
                        m_addressesData.isErgoTokensProperty().set(true);
                    });
                    

                    MenuItem tokensDisabledItem = new MenuItem("Disabled" + (isEnabled ? "" : " (selected)"));
                    tokensDisabledItem.setOnAction(e->{
                        m_addressesData.isErgoTokensProperty().set(false);
                    });

                    if(isEnabled){
                        tokensTip.setText("Ergo Tokens: Enabled");
                        tokensEnabledItem.setId("selectedMenuItem");
                        if(!isListeningToTokenList.get()){
                            ergoTokensList.getLastUpdated().addListener(changeListener);
                            isListeningToTokenList.set(true);
                        }
                    }else{
                        tokensTip.setText("Ergo Tokens: Disabled");
                        tokensDisabledItem.setId("selectedMenuItem");
                        if(isListeningToTokenList.get()){
                      
                            isListeningToTokenList.set(false);
                        }
                    }
                    tokensBtn.getItems().addAll(tokensEnabledItem, tokensDisabledItem);
                }else{
                    tokensBtn.setId("menuBtnDisabled");
                    MenuItem tokensInstallItem = new MenuItem("(install 'Ergo Tokens')");
                    tokensInstallItem.setOnAction(e->{
                        m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().showwManageStage();
                    });
                    tokensTip.setText("(install 'Ergo Tokens')");
                }
            
            };
    
            m_addressesData.isErgoTokensProperty().addListener((obs,oldval,newval)->{
                m_addressesData.getWalletData().setIsErgoTokens(newval);
                updateTokensMenu.run();
            });

            
            m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().addNetworkListener((ListChangeListener.Change<? extends NoteInterface> c) -> {
                getAvailableNodeMenu.run();
                getAvailableExplorerMenu.run();
                getAvailableMarketsMenu.run();
                updateTokensMenu.run();
            });

            getAvailableExplorerMenu.run();
            getAvailableNodeMenu.run();
            getAvailableMarketsMenu.run();
            updateTokensMenu.run();


            closeBtn.setOnAction(closeEvent -> {
                removeShutdownListener();

                m_addressStage.close();
                m_addressStage = null;
                setOpen(false);
            });

   

            noteHookProperty().addListener((obs,oldval,newval)->{
                if(newval != null && newval.getNote() != null){
                    JsonObject note = newval.getNote();
                    JsonElement cmdElement = note.get("cmd");

                    if (cmdElement != null) {
                        String cmd = cmdElement.getAsString();
                        switch (cmd) {
                            case AddressNotes.SEND_CMD:
                                sendBtn.fire();
                            break;
                        }
                    }
                }
            });

            if (getStageMaximized()) {

                m_addressStage.setMaximized(true);
            }

            m_addressStage.setOnCloseRequest((closeRequest) -> {

                closeBtn.fire();
            });
         
        } else {
            if(m_addressStage.isIconified()){
                m_addressStage.setIconified(false);
                m_addressStage.show();
                m_addressStage.setAlwaysOnTop(true);
            }
        }

        return true;
    }


   
    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        JsonElement cmdElement = note.get("cmd");
        NoteHook noteHook = null;
        if (cmdElement != null) {
            String cmd = cmdElement.getAsString();
            switch (cmd) {
                case AddressNotes.SEND_CMD:
                    noteHook = new NoteHook(note, onSucceeded, onFailed);
                    noteHookProperty().set(noteHook);
                return true;
                

            }
        }
       
        return false;
    }

    public int getIndex() {
        return m_index;
    }

    public boolean getValid() {
        return m_addressesData.selectedMarketData().get() != null && m_addressesData.selectedMarketData().get().priceQuoteProperty().get() != null;
    }

    public Address getAddress() {
        return m_address;
    }

    public String getAddressString() {
        return m_addressString;
    }

    public String getAddressMinimal(int show) {
        String adr = m_address.toString();
        int len = adr.length();

        return (show * 2) > len ? adr : adr.substring(0, show) + "..." + adr.substring(len - show, len);
    }

    public BigDecimal getConfirmedAmount() {
        return ErgoInterface.toFullErg(getConfirmedNanoErgs());
    }

    public NetworkType getNetworkType() {
        return m_address.getNetworkType();
    }

    public SimpleObjectProperty<ErgoAmount> ergoAmountProperty(){
        return m_ergoAmountProperty;
    }

    public long getConfirmedNanoErgs() {
        ErgoAmount ergoAmount = m_ergoAmountProperty.get();
        return ergoAmount == null ? 0 : ergoAmount.getLongAmount();
    }

    public long getUnconfirmedNanoErgs() {
        return m_unconfirmedNanoErgs;
    }

    public ArrayList<PriceAmount> getConfirmedTokenList() {
        return m_confirmedTokensList;
    }
    
    public BigDecimal getTotalTokenErgBigDecimal(){
        int tokenListSize = m_confirmedTokensList.size();
        BigDecimal totalErgoAmount = BigDecimal.ZERO;

        PriceAmount[] tokenAmounts = new PriceAmount[tokenListSize];
        tokenAmounts = m_confirmedTokensList.toArray(tokenAmounts);

        for(int i = 0; i < tokenListSize ; i++){
            PriceAmount priceAmount = tokenAmounts[i];
            PriceCurrency priceCurrency = priceAmount.getCurrency();
          
            PriceQuote priceQuote = (priceCurrency != null && priceCurrency instanceof ErgoNetworkToken) && ((ErgoNetworkToken) priceCurrency).getPriceQuote() != null ? ((ErgoNetworkToken) priceCurrency).getPriceQuote() : null;
            
            BigDecimal priceBigDecimal = priceQuote != null ? priceQuote.getBigDecimalAmount() : null;
            BigDecimal amountBigDecimal = priceQuote != null ? priceAmount.getBigDecimalAmount() : null;
            BigDecimal tokenErgs = priceBigDecimal != null && amountBigDecimal != null ? priceBigDecimal.multiply(amountBigDecimal) : BigDecimal.ZERO;
        
        
            totalErgoAmount = totalErgoAmount.add(tokenErgs);
            
        }
 
        return totalErgoAmount;
    }

    public ArrayList<PriceAmount> getUnconfirmedTokenList() {
        return m_unconfirmedTokensList;
    }

    public double getFullAmountDouble() {
        return (double) getConfirmedNanoErgs() / 1000000000;
    }

    public double getFullAmountUnconfirmed() {
        return (double) getUnconfirmedNanoErgs() / 1000000000;
    }

    public double getPrice() {

        return getValid() ? m_addressesData.selectedMarketData().get().priceQuoteProperty().get().getDoubleAmount()  : 0.0;
    }

    public double getTotalAmountPrice() {
        return getFullAmountDouble() * getPrice();
    }



   

    public int getAmountInt() {
        return (int) getFullAmountDouble();
    }

    public double getAmountDecimalPosition() {
        return getFullAmountDouble() - getAmountInt();
    }

    public Image getUnitImage() {
        ErgoAmount ergoAmount = m_ergoAmountProperty.get();
        if (ergoAmount == null) {
            return new Image("/assets/unknown-unit.png");
        } else {
            return ergoAmount.getCurrency().getIcon();
        }
    }

    public HBox getAddressBox(){

        Button addressBtn = new Button();
        addressBtn.setId("transparentColor");
        addressBtn.setContentDisplay(ContentDisplay.LEFT);
        addressBtn.setAlignment(Pos.CENTER_LEFT);
        addressBtn.setPadding(new Insets(0));
        addressBtn.setMouseTransparent(true);
        if(m_imgBuffer.get() != null){
            Image icon = m_imgBuffer.get();
            addressBtn.setGraphic(IconButton.getIconView(icon, icon.getWidth()));
        }
        m_imgBuffer.addListener((obs, oldval, newval)->{
            if(newval != null){
                addressBtn.setGraphic(IconButton.getIconView(newval, newval.getWidth()));
            }
        });

        
        Text addressNameText = new Text(getName());
        addressNameText.setFill(App.txtColor);
        addressNameText.setFont(App.txtFont);

        Region topMidSpacer = new Region();
        HBox.setHgrow(topMidSpacer,Priority.ALWAYS);

        Tooltip copiedTooltip = new Tooltip("copied");
       
        BufferedButton openBtn = new BufferedButton("/assets/open-outline-white-20.png", 15);
        openBtn.setOnAction(e->{
            open();
        });

        BufferedButton copyBtn = new BufferedButton("/assets/copy-30.png", 15);
        copyBtn.setOnAction(e->{
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(m_address.toString());
            clipboard.setContent(content);
            copyBtn.setTooltip(copiedTooltip);
            
            Point2D p = copyBtn.localToScene(0.0, 0.0);

            copiedTooltip.show(
                copyBtn,  
                p.getX() + copyBtn.getScene().getX() + copyBtn.getScene().getWindow().getX(), 
                (p.getY()+ copyBtn.getScene().getY() + copyBtn.getScene().getWindow().getY())-copyBtn.getLayoutBounds().getHeight()
                );
            PauseTransition pt = new PauseTransition(Duration.millis(1600));
            pt.setOnFinished(ptE->{
                copiedTooltip.hide();
            });
            pt.play();
        });



        //copiedTooltip.setOnHidden(e->{
       //     copyBtn.setTooltip(null);
       // });

        HBox topInfoBox = new HBox(addressNameText, topMidSpacer, copyBtn, openBtn);
        HBox.setHgrow(topInfoBox,Priority.ALWAYS);
        topInfoBox.setAlignment(Pos.CENTER_LEFT);

        TextField addressField = new TextField(m_address.toString());
        addressField.setEditable(false);
        addressField.setId("amountField");
        addressField.setAlignment(Pos.CENTER_LEFT);
        addressField.setPadding(new Insets(0, 10, 0, 10));

        VBox addressInformationBox = new VBox(topInfoBox, addressField);
        addressInformationBox.setPadding(new Insets(0,5,0,5));
        addressInformationBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(addressInformationBox, Priority.ALWAYS);

        HBox addressBox = new HBox(addressBtn, addressInformationBox);
        addressBox.setId("rowBox");
        addressBox.setFocusTraversable(true);
        addressBox.addEventFilter(MouseEvent.MOUSE_CLICKED,e->{
            m_addressesData.selectedAddressDataProperty().set(this);
            if(e.getClickCount() == 2){
                open();
            }
        }); 
        HBox.setHgrow(addressBox, Priority.ALWAYS);

        m_addressesData.selectedAddressDataProperty().addListener((obs,oldval,newval)->{
            if(newval == null){
                addressBox.setId("rowBox");
            }else{
                String id =  newval.getId();
                if(id.equals(getId())){
                    addressBox.setId("bodyRowBox");
                }else{
                    addressBox.setId("rowBox");
                }
            }
        });

        return addressBox;
    }

    public void updateBufferedImage() {
        ErgoAmount priceAmount = m_ergoAmountProperty.get();
        boolean quantityValid = priceAmount != null && priceAmount.getAmountValid();
        double priceAmountDouble = priceAmount != null && quantityValid ? priceAmount.getDoubleAmount() : 0;

        PriceQuote priceQuote = getValid() ? m_addressesData.selectedMarketData().get().priceQuoteProperty().get() : null;
        boolean priceValid = priceQuote != null && priceQuote.getTimeStamp() != 0 && priceQuote.howOldMillis() < m_quoteTimeout;
        double priceQuoteDouble = priceValid  && priceQuote != null ? priceQuote.getDoubleAmount() : 0;
        
        String totalPrice = priceValid && priceQuote != null ? Utils.formatCryptoString( priceQuoteDouble * priceAmountDouble, priceQuote.getQuoteCurrency(), priceQuote.getFractionalPrecision(),  quantityValid && priceValid) : " -.--";
        int integers = priceAmount != null ? (int) priceAmount.getDoubleAmount() : 0;
        double decimals = priceAmount != null ? priceAmount.getDoubleAmount() - integers : 0;
        int decimalPlaces = priceAmount != null ? priceAmount.getCurrency().getFractionalPrecision() : 0;
        String cryptoName = priceAmount != null ? priceAmount.getCurrency().getSymbol() : "UKNOWN";
        int space = cryptoName.indexOf(" ");
        cryptoName = space != -1 ? cryptoName.substring(0, space) : cryptoName;

        String currencyPrice = priceValid && priceQuote != null ? priceQuote.toString() : "-.--";

        java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 30);
        java.awt.Font smallFont = new java.awt.Font("SANS-SERIF", java.awt.Font.PLAIN, 12);

        //   Image ergoBlack25 = new Image("/assets/ergo-black-25.png");
        //   SwingFXUtils.fromFXImage(ergoBlack25, null);
        
        String amountString = quantityValid ? String.format("%d", integers) : " -";
        String decs = String.format("%." + decimalPlaces + "f", decimals);

        decs = quantityValid ? decs.substring(1, decs.length()) : "";
        totalPrice = totalPrice + "   ";
        currencyPrice = "(" + currencyPrice + ")   ";
    
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int padding = 5;
        int stringWidth = fm.stringWidth(amountString);
       
        int height = fm.getHeight() + 10;

        g2d.setFont(smallFont);

        fm = g2d.getFontMetrics();
        
        int priceWidth = fm.stringWidth(totalPrice);
        int currencyWidth = fm.stringWidth(currencyPrice);
        int decsWidth = fm.stringWidth(decs);


        int priceLength = (priceWidth > currencyWidth ? priceWidth : currencyWidth);

        //  int priceAscent = fm.getAscent();
        int integersX = priceLength + 10;
        integersX = integersX < 130 ? 130 : integersX;
        int decimalsX = integersX + stringWidth + 1;

       // int cryptoNameStringWidth = fm.stringWidth(cryptoName);
       

        int width = decimalsX + decsWidth + (padding * 2);
   
        width = width < m_minImgWidth ? m_minImgWidth : width;

        int cryptoNameStringX = decimalsX + 2;

        g2d.dispose();
        
        BufferedImage unitImage = SwingFXUtils.fromFXImage(priceAmount != null ? priceAmount.getCurrency().getIcon() : new Image("/assets/unknown-unit.png"), null);
        Drawing.setImageAlpha(unitImage, 0x40);
        //  adrBuchImg.getScaledInstance(width, height, java.awt.Image.SCALE_AREA_AVERAGING);
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        //   g2d.setComposite(AlphaComposite.Clear);

        /* for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = new Color(adrBuchImg.getRGB(x, y), true);

                Color c2 = new Color(c.getRed(), c.getGreen(), c.getBlue(), 35);

                img.setRGB(x, y, c2.getRGB());
            }
        }
         */
        g2d.drawImage(unitImage,75, (height / 2) - (unitImage.getHeight() / 2), unitImage.getWidth(), unitImage.getHeight(), null);

       



        g2d.setFont(font);
        fm = g2d.getFontMetrics();
        g2d.setColor(java.awt.Color.WHITE);

        

        g2d.drawString(amountString, integersX, fm.getAscent() + 5);

        g2d.setFont(smallFont);
        fm = g2d.getFontMetrics();
        g2d.setColor(new java.awt.Color(.9f, .9f, .9f, .9f));

       
        if(decimalPlaces > 0){
            //decimalsX = widthIncrease > 0 ? decimalsX + widthIncrease : decimalsX;
            g2d.drawString(decs, decimalsX , fm.getHeight() + 2);
        }

        
        g2d.drawString(cryptoName, cryptoNameStringX, height - 10);

        g2d.setFont(smallFont);
        g2d.setColor(java.awt.Color.WHITE);
        fm = g2d.getFontMetrics();
        g2d.drawString(totalPrice, padding, fm.getHeight() + 2);

        g2d.setColor(new java.awt.Color(.6f, .6f, .6f, .9f));
        g2d.drawString(currencyPrice, padding, height - 10);

        /*try {
            Files.writeString(logFile.toPath(), amountString + decs);
        } catch (IOException e) {

        }*/
        g2d.dispose();

        m_imgBuffer.set(SwingFXUtils.toFXImage(img, null));

  
    }

    public SimpleObjectProperty<Image> getImageProperty() {
        return m_imgBuffer;
    }



    public PriceAmount getConfirmedTokenAmount(String tokenId){
        if(tokenId != null){
            for(int i = 0; i < m_confirmedTokensList.size(); i++){
                PriceAmount tokenAmount = m_confirmedTokensList.get(i);
                if(tokenAmount.getCurrency().getTokenId().equals(tokenId)){
                    return tokenAmount;
                }
            }
        }
        return null;
    }

    public void updateBalance(ErgoExplorerData explorerData) {

   
        explorerData.getBalance(m_address.toString(),
        success -> {
            Object sourceObject = success.getSource().getValue();

            if (sourceObject != null) {
                JsonObject jsonObject = (JsonObject) sourceObject;
                
                Platform.runLater(() ->setBalance(jsonObject));  
            }},
            failed -> {
                    try {
                        Files.writeString(logFile.toPath(), "\nAddressData, Explorer failed update: " + failed.getSource().getException().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        
                        
                    }
            
                //update();
            }
        );
      
        
    }
    /*
    public void updateTransactions(ErgoExplorerData explorerData){
        
        try {
            Files.writeString(logFile.toPath(), "updateTxOffset: " + m_updateTxOffset, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
    
        }

        explorerData.getAddressTransactions(m_addressString, (m_updateTxOffset * UPDATE_LIMIT), UPDATE_LIMIT,
            success -> {
                Object sourceObject = success.getSource().getValue();

                if (sourceObject != null && sourceObject instanceof JsonObject) {
                    JsonObject jsonObject = (JsonObject) sourceObject;
                    
                    Platform.runLater(() ->{
                        updateWatchedTxs(explorerData, jsonObject);
                        //  saveAllTxJson(jsonObject);
                    });  
                    
                    
                }
            },
            failed -> {
                    try {
                        Files.writeString(logFile.toPath(), "\nAddressData, Explorer failed transaction update: " + failed.getSource().getException().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        
                        
                        }
                
                    //update();
                }
        );
        
        
    }*/
    
    public void setBalance(JsonObject jsonObject){
        if (jsonObject != null) {


            JsonElement confirmedElement = jsonObject != null ? jsonObject.get("confirmed") : null;
            JsonElement unconfirmedElement = jsonObject.get("unconfirmed");
            if (confirmedElement != null && unconfirmedElement != null) {

                JsonObject confirmedObject = confirmedElement.getAsJsonObject();
                JsonObject unconfirmedObject = unconfirmedElement.getAsJsonObject();

                JsonElement nanoErgElement = confirmedObject.get("nanoErgs");

               

                m_unconfirmedNanoErgs = unconfirmedObject.get("nanoErgs").getAsLong();

                JsonElement confirmedArrayElement = confirmedObject.get("tokens");
                //JsonArray unconfirmedTokenArray = unconfirmedObject.get("tokens").getAsJsonArray();


              //  int confirmedSize = confirmedTokenArray.size();
                
                
                ErgoTokens ergoTokens = m_addressesData.isErgoTokensProperty().get() ? (ErgoTokens) m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoTokens.NETWORK_ID) : null;
            
                ErgoTokensList tokensList = ergoTokens != null ?  m_addressesData.tokensListProperty().get() : null;

               
                
                if(confirmedArrayElement != null && confirmedArrayElement.isJsonArray()){
                    JsonArray confirmedTokenArray = confirmedArrayElement.getAsJsonArray();
                   
                    m_confirmedTokensList.clear();

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
                  

                        ErgoNetworkToken networkToken = tokensList != null ? tokensList.getErgoToken(tokenId) : null;
    

                        
                        if(networkToken != null){
                            networkToken.setDecimals(decimals);
                           // networkToken.setName(name);
                            networkToken.setTokenType(tokenType);
                        }

                        PriceAmount tokenAmount = networkToken != null ? new PriceAmount(amount, networkToken) : new PriceAmount(amount, new PriceCurrency(tokenId, name, name, decimals, ErgoNetwork.NETWORK_ID,m_address.getNetworkType().toString(), "/assets/unknown-unit.png",tokenType, ""));    
                        
                        m_confirmedTokensList.add(tokenAmount);
                   
                    }
                }else{
                    try {
                        Files.writeString(new File("networkToken.txt").toPath(), "\ntoken json array " + (confirmedArrayElement != null ? confirmedArrayElement.toString() : "null"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) { 
                    
                    }
                }
           
                 if (nanoErgElement != null && nanoErgElement.isJsonPrimitive()) {
                    ErgoAmount ergoAmount = new ErgoAmount(nanoErgElement.getAsLong(), getNetworkType());
                    m_ergoAmountProperty.set(ergoAmount);
                }
            } 
        } 
    }


    public void updateWatchedTxs(ErgoExplorerData explorerData, JsonObject json){
        
        if(json != null){
            JsonElement itemsElement = json.get("items");

            if(itemsElement != null && itemsElement.isJsonArray()){
                JsonArray itemsArray = itemsElement.getAsJsonArray();
                int size = itemsArray.size();

                SimpleBooleanProperty found = new SimpleBooleanProperty(false);

                for(int i = 0; i < size ; i ++){
                    JsonElement txElement = itemsArray.get(i);
                    if(txElement != null && txElement.isJsonObject()){
                        JsonObject txObject = txElement.getAsJsonObject();

                        JsonElement txIdElement = txObject.get("id");
                        if(txIdElement != null && txIdElement.isJsonPrimitive()){
                           
                            String txId = txIdElement.getAsString();
                            ErgoTransaction ergTx = getWatchedTx(txId);
                            if(ergTx != null){
                                if(!found.get()){
                                    found.set(true);
                                }
                                ergTx.update(txObject);
                            }
                        }
                    }
                }

                /*if(size >= UPDATE_LIMIT && !found.get()){
                    m_updateTxOffset++;
                    
                    updateTransactions(explorerData);
                }*/
               
            }
        
        }
    }

    /* Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String prettyString = gson.toJson(json); */

    public void saveAddresInfo(){
       

        JsonObject json = getAddressJson();    
        
        try {
            m_addressesData.getWalletData().getErgoWallets().saveAddressInfo(m_addressString,m_addressesData.getWalletData().getId(), json);
        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
            try {
                Files.writeString(logFile.toPath(), "saveAddressInfo failed: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {
             
            }
        }
        
    }
 
   /* public JsonArray getConfirmedTokenJsonArray() {
        JsonArray confirmedTokenArray = new JsonArray();
        m_confirmedTokensList.forEach(token -> {
            confirmedTokenArray.add(token.getJsonObject());
        });
        return confirmedTokenArray;
    }*/

    /*public JsonArray getUnconfirmedTokenJsonArray() {
        JsonArray unconfirmedTokenArray = new JsonArray();
        m_unconfirmedTokensList.forEach(token -> {
            unconfirmedTokenArray.add(token.getJsonObject());
        });
        return unconfirmedTokenArray;
    }*/




    /*public JsonObject getJsonObject() {
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("id", m_address.toString());
        jsonObj.addProperty("tickerName", m_priceBaseCurrency);
        jsonObj.addProperty("name", getName());
        jsonObj.addProperty("address", m_address.toString());
        jsonObj.addProperty("networkType", m_address.getNetworkType().toString());
        jsonObj.addProperty("marketValidated", getValid());
       
        return jsonObj;

    }*/



    

    public int getApiIndex() {
        return m_apiIndex;
    }

    public String getPriceBaseCurrency() {
        return m_priceBaseCurrency;
    }

    public String getPriceTargetCurrency() {
        return m_priceTargetCurrency;
    }

    @Override
    public String toString() {
  
        return getText();
    }
}
