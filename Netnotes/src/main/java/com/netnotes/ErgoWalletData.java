package com.netnotes;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.ergoplatform.appkit.NetworkType;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.satergo.Wallet;

import com.utils.Utils;


import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ErgoWalletData extends Network implements NoteInterface {

    public final static double MIN_WIDTH = 400;
    public final static double MIN_HEIGHT = 275;
  //  private long m_explorerTimePeriod = 15;


    private File logFile = new File("netnotes-log.txt");
    private File m_walletFile = null;
      
    

    private NetworkType m_networkType = NetworkType.MAINNET;

    // private String m_name;
    private String m_nodesId;
    private String m_explorerId;
    private String m_marketsId;
    private boolean m_isErgoTokens = true;


   // private String m_quoteTransactionCurrency = "USD";

    private ErgoWallets m_ergoWallet;



    // private ErgoWallet m_ergoWallet;
    public ErgoWalletData(String id, String name, File walletFile, String nodesId, String explorerId, String marketsId, boolean ergoTokensEnabled, NetworkType networkType, ErgoWallets ergoWallet) {
        super(null, name, id, ergoWallet);

        m_walletFile = walletFile;
        m_networkType = networkType;
        m_isErgoTokens = ergoTokensEnabled;
        m_nodesId = nodesId;
        m_explorerId = explorerId;
        m_marketsId = marketsId;
        m_ergoWallet = ergoWallet;

        setIconStyle(IconStyle.ROW);
        getLastUpdated().set(LocalDateTime.now());
    }   

    public ErgoWalletData(String id, String name, JsonObject jsonObject, ErgoWallets ergoWallet) {
        super(null, name, id, ergoWallet);

        m_ergoWallet = ergoWallet;

       

        JsonElement fileLocationElement = jsonObject.get("file");
        JsonElement stageElement = jsonObject.get("stage");
        JsonElement networkTypeElement = jsonObject.get("networkType");
        JsonElement nodeIdElement = jsonObject.get("nodeId");
        JsonElement explorerElement = jsonObject.get("explorerId");
        JsonElement marketElement = jsonObject.get("marketId");
        JsonElement ergoTokensElement = jsonObject.get("isErgoTokens");

        m_isErgoTokens = ergoTokensElement != null && ergoTokensElement.isJsonPrimitive() ? ergoTokensElement.getAsBoolean() : false;
        m_walletFile = fileLocationElement == null ? null : new File(fileLocationElement.getAsString());
        m_networkType = networkTypeElement == null ? NetworkType.MAINNET : networkTypeElement.getAsString().equals(NetworkType.TESTNET.toString()) ? NetworkType.TESTNET : NetworkType.MAINNET;

        JsonObject stageJson = stageElement != null && stageElement.isJsonObject() ? stageElement.getAsJsonObject() : null;
        JsonElement stageWidthElement = stageJson != null ? stageJson.get("width") : null;
        JsonElement stageHeightElement = stageJson != null ? stageJson.get("height") : null;

        double stageWidth = stageWidthElement != null && stageWidthElement.isJsonPrimitive() ? stageWidthElement.getAsDouble() : 700;
        double stageHeight = stageHeightElement != null && stageHeightElement.isJsonPrimitive() ? stageHeightElement.getAsDouble() : 350;

        setIconStyle(IconStyle.ROW);
        setStageWidth(stageWidth);
        setStageHeight(stageHeight);

        m_nodesId = nodeIdElement != null && nodeIdElement.isJsonPrimitive() ? nodeIdElement.getAsString() : null;
        m_marketsId = marketElement != null && marketElement.isJsonPrimitive() ? marketElement.getAsString() : null;
        m_explorerId =  explorerElement != null && explorerElement.isJsonPrimitive() ? explorerElement.getAsString() : null;
    }
    /*
    public void setMarketObject(JsonObject json) {
        if (json == null) {
            m_marketsId = null;
     
        } else {
            JsonElement marketIdElement = json.get("networkId");
            m_marketsId = marketIdElement != null && marketIdElement.isJsonPrimitive() ? marketIdElement.getAsString() : null;

        }

    }

    public void setExplorerObject(JsonObject json) {
        if (json == null) {
            m_explorerId = null;
     
        } else {
            JsonElement explorerIdElement = json.get("networkId");
            m_explorerId = explorerIdElement != null && explorerIdElement.isJsonPrimitive() ? explorerIdElement.getAsString() : null;
           

        }

    }*/


  

    @Override
    public JsonObject getJsonObject() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("name", getName());
        jsonObject.addProperty("id", getNetworkId());
        jsonObject.addProperty("file", m_walletFile.getAbsolutePath());
        jsonObject.addProperty("networkType", m_networkType.toString());

        jsonObject.add("stage", getStageJson());

        if (m_nodesId != null) {
            jsonObject.addProperty("nodeId", m_nodesId);
        }

        if (m_explorerId != null) {
            jsonObject.addProperty("explorerId", m_explorerId);
        }

        if (m_marketsId != null) {
            jsonObject.addProperty("marketId", m_marketsId);
        }
        jsonObject.addProperty("isErgoTokens", m_isErgoTokens);

        return jsonObject;
    }

    @Override
    public void open() {

        openWallet();

    }
    private Stage m_walletStage = null;

    public void openWallet() {
        if (m_walletStage == null) {
       
            double sceneWidth = 600;
            double sceneHeight = 305;

            m_walletStage = new Stage();
            m_walletStage.getIcons().add(ErgoWallets.getSmallAppIcon());
            m_walletStage.initStyle(StageStyle.UNDECORATED);
            m_walletStage.setTitle("Wallet file: Enter password");

            Button closeBtn = new Button();

            HBox titleBox = App.createTopBar(ErgoWallets.getSmallAppIcon(), getName() + " - Enter password", closeBtn, m_walletStage);
            closeBtn.setOnAction(event -> {
                m_walletStage.close();
                m_walletStage = null;
            });

            Button imageButton = App.createImageButton(ErgoWallets.getAppIcon(), "Wallet");
            imageButton.setGraphicTextGap(10);
            HBox imageBox = new HBox(imageButton);
            imageBox.setAlignment(Pos.CENTER);
            imageBox.setPadding(new Insets(0, 0, 15, 0));

            Text passwordTxt = new Text("> Enter password:");
            passwordTxt.setFill(App.txtColor);
            passwordTxt.setFont(App.txtFont);

            PasswordField passwordField = new PasswordField();
            passwordField.setFont(App.txtFont);
            passwordField.setId("passField");
            HBox.setHgrow(passwordField, Priority.ALWAYS);

            Platform.runLater(() -> passwordField.requestFocus());

            HBox passwordBox = new HBox(passwordTxt, passwordField);
            passwordBox.setAlignment(Pos.CENTER_LEFT);

            Button clickRegion = new Button();
            clickRegion.setPrefWidth(Double.MAX_VALUE);
            clickRegion.setId("transparentColor");
            clickRegion.setPrefHeight(40);

            clickRegion.setOnAction(e -> {
                passwordField.requestFocus();

            });

            VBox.setMargin(passwordBox, new Insets(5, 10, 0, 20));

            VBox layoutVBox = new VBox(titleBox, imageBox, passwordBox, clickRegion);
            VBox.setVgrow(layoutVBox, Priority.ALWAYS);

            Scene passwordScene = new Scene(layoutVBox, sceneWidth, sceneHeight);
            passwordScene.setFill(null);
            passwordScene.getStylesheets().add("/css/startWindow.css");
            m_walletStage.setScene(passwordScene);
            Rectangle rect = getNetworksData().getMaximumWindowBounds();

            passwordField.setOnKeyPressed(e -> {

                KeyCode keyCode = e.getCode();

                if (keyCode == KeyCode.ENTER) {

                    try {

                        Wallet wallet = Wallet.load(m_walletFile.toPath(), passwordField.getText());
                        passwordField.setText("");

                        m_walletStage.setScene(getWalletScene(wallet, m_walletStage));
                        ResizeHelper.addResizeListener(m_walletStage, MIN_WIDTH, MIN_HEIGHT, rect.getWidth(), rect.getHeight());

                    } catch (Exception e1) {

                        passwordField.setText("");
                        try {
                            Files.writeString(logFile.toPath(), "Password error: " + getName() + " \n" +e1.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e2) {
                  
                        }
                        
                    }

                }
            });

            ResizeHelper.addResizeListener(m_walletStage, MIN_WIDTH, MIN_HEIGHT, rect.getWidth(), rect.getHeight());
            m_walletStage.show();
            m_walletStage.setOnCloseRequest(e -> {
                closeBtn.fire();
            });
        }else{
            if(m_walletStage.isIconified()){
                m_walletStage.setIconified(false);
                m_walletStage.show();
                Platform.runLater(()->m_walletStage.requestFocus());
            }
        }
    }


    public NoteInterface getMarketInterface() {
        if (m_marketsId != null) {
            return m_ergoWallet.getErgoNetworkData().getNetwork(m_marketsId);
        }
        return null;
    }

    public ErgoWallets getErgoWallets(){
        return m_ergoWallet;
    }


    
    private Scene getWalletScene(Wallet wallet, Stage walletStage) {
        
       
        AddressesData addressesData = new AddressesData(FriendlyId.createFriendlyId(), wallet, this, m_networkType);
           

        String title = getName() + " - (" + m_networkType.toString() + ")";

        double imageWidth = App.MENU_BAR_IMAGE_WIDTH;
        //double alertImageWidth = 75;

        //  PriceChart priceChart = null;
        walletStage.setTitle(title);

        Button closeBtn = new Button();
        Button maximizeBtn = new Button();
        Button shrinkBtn = new Button();

        SimpleBooleanProperty isShrunk = new SimpleBooleanProperty(false);

        HBox titleBox = App.createShrinkTopBar(ErgoWallets.getSmallAppIcon(), title, maximizeBtn, shrinkBtn, closeBtn, walletStage, isShrunk, getNetworksData().getAppData());

     

        Tooltip sendTip = new Tooltip("Send");
        sendTip.setShowDelay(new javafx.util.Duration(100));
    

        BufferedButton sendBtn = new BufferedButton("/assets/arrow-send-white-30.png", imageWidth);
        sendBtn.setTooltip(sendTip);
        sendBtn.setId("menuBtn");
        sendBtn.setUserData("sendButton");
   



        //   addressesData.currentAddressProperty
        Tooltip addTip = new Tooltip("Add address");
        addTip.setShowDelay(new javafx.util.Duration(100));
        addTip.setFont(App.txtFont);

        Button addButton = new Button();
        addButton.setGraphic(IconButton.getIconView(new Image("/assets/git-branch-outline-white-30.png"), imageWidth));
        addButton.setId("menuBtn");
        addButton.setTooltip(addTip);

        HBox rightSideMenu = new HBox();

        Tooltip nodesTip = new Tooltip("Select node");
        nodesTip.setShowDelay(new javafx.util.Duration(50));
        nodesTip.setFont(App.txtFont);


        BufferedMenuButton nodesMenuBtn = new BufferedMenuButton("/assets/ergoNodes-30.png", imageWidth);
        nodesMenuBtn.setPadding(new Insets(2, 0, 0, 0));
        nodesMenuBtn.setTooltip(nodesTip);



        Runnable updateNodeBtn = () ->{
            ErgoNodes ergoNodes = (ErgoNodes) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID);
            ErgoNodeData nodeData = addressesData.selectedNodeData().get();
        
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
            ErgoNodes ergoNodes = (ErgoNodes) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID);
            if(ergoNodes != null){
                ergoNodes.getErgoNodesList().getMenu(nodesMenuBtn, addressesData.selectedNodeData());
                nodesMenuBtn.setId("menuBtn");
                
            }else{
                nodesMenuBtn.getItems().clear();
                nodesMenuBtn.setId("menuBtnDisabled");
             
            }
            updateNodeBtn.run();
        };

        addressesData.selectedNodeData().addListener((obs, oldval, newval)->{
                updateNodeBtn.run();
    
            setNodesId(newval == null ? null : newval.getId());
           
        });

        
        Tooltip explorerTip = new Tooltip("Select explorer");
        explorerTip.setShowDelay(new javafx.util.Duration(50));
        explorerTip.setFont(App.txtFont);



        BufferedMenuButton explorerBtn = new BufferedMenuButton("/assets/ergo-explorer-30.png", imageWidth);
        explorerBtn.setPadding(new Insets(2, 0, 0, 2));
        explorerBtn.setTooltip(explorerTip);

        Runnable updateExplorerBtn = () ->{
            ErgoExplorers ergoExplorers = (ErgoExplorers) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);

            ErgoExplorerData explorerData = addressesData.selectedExplorerData().get();
           
           
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
        
            ErgoExplorers ergoExplorers = (ErgoExplorers) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);
            if(ergoExplorers != null){
                explorerBtn.setId("menuBtn");
                ergoExplorers.getErgoExplorersList().getMenu(explorerBtn, addressesData.selectedExplorerData());
            }else{
                 explorerBtn.getItems().clear();
                 explorerBtn.setId("menuBtnDisabled");
               
            }
            updateExplorerBtn.run();
        };    

        addressesData.selectedExplorerData().addListener((obs, oldval, newval)->{
            setExplorer(newval == null ? null : newval.getId());
            updateExplorerBtn.run();
        });

        Tooltip marketsTip = new Tooltip("Select market");
        marketsTip.setShowDelay(new javafx.util.Duration(50));
        marketsTip.setFont(App.txtFont);

        BufferedMenuButton marketsBtn = new BufferedMenuButton("/assets/ergoChart-30.png", imageWidth);
        marketsBtn.setPadding(new Insets(2, 0, 0, 0));
        marketsBtn.setTooltip(marketsTip);






        Runnable getAvailableMarketsMenu = () -> {
            ErgoMarkets ergoMarkets = (ErgoMarkets) getErgoWallets().getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID);
            if (ergoMarkets != null) {
                marketsBtn.setId("menuBtn");

                ergoMarkets.getErgoMarketsList().getMenu(marketsBtn, addressesData.selectedMarketData());
            } else {
                marketsBtn.getItems().clear();
                marketsBtn.setId("menuBtnDisabled");
            }
           
        };
    
        /*Runnable setSelectedMarket = ()->{
            ErgoMarkets marketInterface = getMarketsId() != null ? (ErgoMarkets) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID) : null;
            ErgoMarketsList marketsList = marketInterface != null ? marketInterface.getErgoMarketsList() : null;
            ErgoMarketsData marketsData = marketsList != null ? marketsList.getMarketsData(getMarketsId()) : null;
      
            getAvailableMarketsMenu.run();
        };*/


        
       

        Tooltip tokensTip = new Tooltip("Ergo Tokens");
        tokensTip.setShowDelay(new javafx.util.Duration(50));
        tokensTip.setFont(App.txtFont);


        BufferedMenuButton tokensMenuBtn = new BufferedMenuButton("/assets/diamond-30.png", imageWidth);
        tokensMenuBtn.setPadding(new Insets(2, 0, 0, 0));
        tokensMenuBtn.setTooltip(tokensTip);

        

        Runnable updateTokensMenu = ()->{
            tokensMenuBtn.getItems().clear();
            ErgoTokens ergoTokens = (ErgoTokens) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoTokens.NETWORK_ID);  
            boolean isEnabled = addressesData.isErgoTokensProperty().get();

            if(ergoTokens != null){
                tokensMenuBtn.setId("menuBtn");
                MenuItem tokensEnabledItem = new MenuItem("Enabled" + (isEnabled ? " (selected)" : ""));
                tokensEnabledItem.setOnAction(e->{
                    addressesData.isErgoTokensProperty().set(true);
                });
                

                MenuItem tokensDisabledItem = new MenuItem("Disabled" + (isEnabled ? "" : " (selected)"));
                tokensDisabledItem.setOnAction(e->{
                    addressesData.isErgoTokensProperty().set(false);
                });

                if(isEnabled){
                    tokensTip.setText("Ergo Tokens: Enabled");
                    tokensEnabledItem.setId("selectedMenuItem");
                }else{
                    tokensTip.setText("Ergo Tokens: Disabled");
                    tokensDisabledItem.setId("selectedMenuItem");
                }
                tokensMenuBtn.getItems().addAll(tokensEnabledItem, tokensDisabledItem);
            }else{
                tokensMenuBtn.setId("menuBtnDisabled");
                MenuItem tokensInstallItem = new MenuItem("(install 'Ergo Tokens')");
                tokensInstallItem.setOnAction(e->{
                    m_ergoWallet.getErgoNetworkData().showwManageStage();
                });
                tokensTip.setText("(install 'Ergo Tokens')");
            }
           
        };
   
        addressesData.isErgoTokensProperty().addListener((obs,oldval,newval)->{
            setIsErgoTokens(newval);
            updateTokensMenu.run();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

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

        rightSideMenu.getChildren().addAll(nodesMenuBtn, seperator1, explorerBtn, seperator2, marketsBtn, seperator3, tokensMenuBtn);
        rightSideMenu.setId("rightSideMenuBar");
        rightSideMenu.setPadding(new Insets(0, 0, 0, 0));
        rightSideMenu.setAlignment(Pos.CENTER_RIGHT);

   


        HBox menuBar = new HBox(sendBtn, addButton, spacer, rightSideMenu);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(1, 0, 1, 5));

        HBox paddingBox = new HBox(menuBar);
        paddingBox.setPadding(new Insets(2, 5, 1, 5));

        VBox menuVBox = new VBox(paddingBox);

      
        //layoutBox.setPadding(SMALL_INSETS);

        Font smallerFont = Font.font("OCR A Extended", 10);

        Text updatedTxt = new Text("Updated:");
        updatedTxt.setFill(App.altColor);
        updatedTxt.setFont(smallerFont);

        TextField lastUpdatedField = new TextField();
        lastUpdatedField.setPrefWidth(190);
  
        lastUpdatedField.setId("smallPrimaryColor");

        HBox updateBox = new HBox(updatedTxt, lastUpdatedField);
        updateBox.setPadding(new Insets(0,2,2,0));
        updateBox.setAlignment(Pos.CENTER_RIGHT);

        Region spacerRegion = new Region();
        VBox.setVgrow(spacerRegion, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setId("bodyBox");
        
        TextField totalField = new TextField();
        totalField.setId("priceField");
        totalField.setEditable(false);
        HBox.setHgrow(totalField, Priority.ALWAYS);

        TextField totalTokensField = new TextField();
        totalTokensField.setId("formFieldSmall");
        totalTokensField.setEditable(false);
        HBox.setHgrow(totalField, Priority.ALWAYS);

      //  HBox totalTokensBox = new HBox(totalTokensField);
      //  HBox.setHgrow(totalTokensBox, Priority.ALWAYS);

        VBox summaryBox = new VBox(totalField, totalTokensField);
        HBox.setHgrow(summaryBox, Priority.ALWAYS);
        summaryBox.setPadding(new Insets(5, 0, 0, 0));
        summaryBox.setAlignment(Pos.CENTER_LEFT);


        HBox scrollBox = new HBox(scrollPane);
        HBox.setHgrow(scrollBox, Priority.ALWAYS);
        scrollBox.setPadding(new Insets(5, 5, 0, 5));
        VBox bodyVBox = new VBox(titleBox, menuVBox, scrollBox, summaryBox, updateBox);

      

        SimpleDoubleProperty normalHeight = new SimpleDoubleProperty(MIN_HEIGHT);

        isShrunk.addListener((obs, oldval, newval)->{
            Rectangle rect = getNetworksData().getMaximumWindowBounds();
            if(newval){
                
                normalHeight.set(walletStage.getHeight());
                double smallHeight = titleBox.heightProperty().get() + summaryBox.heightProperty().get() + updateBox.heightProperty().get();
                bodyVBox.getChildren().removeAll(menuVBox, scrollBox);
                walletStage.setHeight(smallHeight);
                walletStage.setAlwaysOnTop(true);
                ResizeHelper.addResizeListener(walletStage, MIN_WIDTH, smallHeight, rect.getWidth(), smallHeight);
            }else{

                walletStage.setAlwaysOnTop(false);
                ResizeHelper.addResizeListener(walletStage, MIN_WIDTH, MIN_HEIGHT, rect.getWidth(), rect.getHeight());
                walletStage.setHeight(normalHeight.get());

                bodyVBox.getChildren().clear();
                bodyVBox.getChildren().addAll(titleBox, menuVBox, scrollBox, summaryBox, updateBox);
            }
        });
        addButton.setOnAction(e -> {
            addressesData.addAddress();
        });



        m_ergoWallet.getErgoNetworkData().addNetworkListener((ListChangeListener.Change<? extends NoteInterface> c) -> {
            getAvailableNodeMenu.run();
            getAvailableExplorerMenu.run();
            getAvailableMarketsMenu.run();
            updateTokensMenu.run();
        });

        getAvailableExplorerMenu.run();
        getAvailableNodeMenu.run();
        getAvailableMarketsMenu.run();
        updateTokensMenu.run();
      

        sendBtn.setOnAction((actionEvent) -> {
           
            AddressData selectedAdr = addressesData.selectedAddressDataProperty().get();

            if(selectedAdr != null){
                JsonObject json = new JsonObject();
                json.addProperty("cmd", AddressData.AddressNotes.SEND_CMD);
                selectedAdr.open();
                selectedAdr.sendNote(json, null,null);
            }

        });

        Scene openWalletScene = new Scene(bodyVBox, getStageWidth(), getStageHeight());
        openWalletScene.getStylesheets().add("/css/startWindow.css");
        openWalletScene.setFill(null);
        
        scrollPane.setContent(addressesData.getAddressesBox(openWalletScene));
      

        scrollPane.prefViewportWidthProperty().bind(openWalletScene.widthProperty().subtract(10));
        scrollPane.prefViewportHeightProperty().bind(openWalletScene.heightProperty().subtract(titleBox.heightProperty()).subtract(menuBar.heightProperty()).subtract(updateBox.heightProperty()).subtract(summaryBox.heightProperty()).subtract(5));



        openWalletScene.focusOwnerProperty().addListener((e) -> {
            if (openWalletScene.focusOwnerProperty().get() instanceof AddressData) {

                AddressData addressData = (AddressData) openWalletScene.focusOwnerProperty().get();

                addressesData.selectedAddressDataProperty().set(addressData);
                
            } /* else {
                

                if (openWalletScene.focusOwnerProperty().get() instanceof Button) {
                    Button focusedBtn = (Button) openWalletScene.focusOwnerProperty().get();
                    Object userData = focusedBtn.getUserData();
                    if(userData != null && userData instanceof String){
                        String userDataString = (String) userData;
                        if(userDataString.equals("sendButton")){

                        }else{
                            addressesData.selectedAddressDataProperty().set(null);
                        }
                    }else{
                        addressesData.selectedAddressDataProperty().set(null);
                    }
           
                } else {
                    addressesData.selectedAddressDataProperty().set(null);
                }
            }*/
        });

        Runnable calculateTotal = () ->{
            
        
            ErgoAmount totalErgoAmount = addressesData.totalErgoAmountProperty().get();

            ErgoAmount totalTokenErgs = addressesData.getTotalTokenErgs();
            
            ErgoMarketsData marketsData = addressesData.selectedMarketData().get();

            PriceQuote priceQuote = marketsData != null ? marketsData.priceQuoteProperty().get() : null;

            

            
            String totalString = totalErgoAmount == null ? "Σ-" : totalErgoAmount.toString();

            
    
            double ergoAmountDouble = (totalErgoAmount != null ? totalErgoAmount.getDoubleAmount() : 0);


            BigDecimal tokenErgsPrice = priceQuote != null ? priceQuote.getBigDecimalAmount().multiply(totalTokenErgs.getBigDecimalAmount()) : null;

            double totalPrice = priceQuote != null ? priceQuote.getDoubleAmount() * ergoAmountDouble : 0;
        
            double totalTokenPrice = tokenErgsPrice != null ? tokenErgsPrice.doubleValue() : 0;
            
            String quoteString = (priceQuote != null ? ": " + Utils.formatCryptoString( totalPrice , priceQuote.getQuoteCurrency(),priceQuote.getFractionalPrecision(),  totalErgoAmount != null) +" (" + priceQuote.toString() + ")" : "" );

            
            String text = totalString  + quoteString;

            String tokenQuoteString = (priceQuote != null ? Utils.formatCryptoString( totalTokenPrice , priceQuote.getQuoteCurrency(),priceQuote.getFractionalPrecision(),  totalTokenErgs != null)  : "" );

            String tokenText = totalTokenPrice > 0 ? "Tokens: " + totalTokenErgs.toString() + " (" +tokenQuoteString + ")"  : "";

            Platform.runLater(() ->totalField.setText(text));
        
            Platform.runLater(()-> totalTokensField.setText(tokenText));

            Platform.runLater(() -> lastUpdatedField.setText(Utils.formatDateTimeString(LocalDateTime.now())));
        };



        addressesData.totalErgoAmountProperty().addListener((obs, oldval, newval)->calculateTotal.run());
       
        ChangeListener<PriceQuote> priceQuoteListener = (obs,oldval,newval)->{
            calculateTotal.run();
        };
        
     
        addressesData.selectedMarketData().addListener((obs, oldval, newVal) -> {
            setMarketsId(newVal != null ? newVal.getMarketId() : null);
            
            if(oldval != null){
                oldval.priceQuoteProperty().removeListener(priceQuoteListener);
                oldval.shutdown();
            }
            if (newVal != null) {
                newVal.priceQuoteProperty().addListener(priceQuoteListener);
                marketsTip.setText("Ergo Markets: " + newVal.getName());
                
                newVal.start();

            
            } else {
                NoteInterface ergoMarkets = getErgoWallets().getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID);

                if (ergoMarkets == null) {
                    marketsTip.setText("(install 'Ergo Markets')");
                } else {
                    marketsTip.setText("Select market...");
                }
            }
        });
        
        
        calculateTotal.run();

        if(addressesData.selectedMarketData().get() != null){
            addressesData.selectedMarketData().get().priceQuoteProperty().addListener(priceQuoteListener);
       
        }
        


        closeBtn.setOnAction(closeEvent -> {
           
         
            addressesData.shutdown();
            walletStage.close();
            m_walletStage = null;
        });
        walletStage.setOnCloseRequest(event -> {
            closeBtn.fire();    
        });

        maximizeBtn.setOnAction(e->{
            boolean maximized = walletStage.isMaximized();
            setStageMaximized(!maximized);

            if (!maximized) {
                setStagePrevWidth(walletStage.getWidth());
                setStagePrevHeight(walletStage.getHeight());
            }

            walletStage.setMaximized(!maximized);
        });

        m_ergoWallet.shutdownNowProperty().addListener((obs, oldVal, newVal) -> {
            closeBtn.fire();
        });

        return openWalletScene;

    }

    public JsonObject getMarketData() {
        JsonObject json = new JsonObject();
        json.addProperty("subject", App.GET_DATA);
        return json;
    }

    public String getMarketsId() {
        return m_marketsId;
    }

  

    public String getNodesId() {
        return m_nodesId;
    }


    public NoteInterface getNodeInterface() {
        return m_nodesId == null ? null : m_ergoWallet.getErgoNetworkData().getNetwork(m_nodesId);
    }

    public String getExplorerId() {
        return m_explorerId;
    }


    public void setNodesId(String nodesId) {
        m_nodesId = nodesId;
        getLastUpdated().set(LocalDateTime.now());
    }

    public void setExplorer(String explorerId) {
        m_explorerId = explorerId;

       // m_explorerTimePeriod = 15;
        getLastUpdated().set(LocalDateTime.now());
    }

    public void setMarketsId(String marketsId) {
        m_marketsId = marketsId;
    
        getLastUpdated().set(LocalDateTime.now());
    }

    public void setIsErgoTokens(boolean value){
        m_isErgoTokens = value;
        getLastUpdated().set(LocalDateTime.now());
    }

    public boolean isErgoTokens(){
        return m_isErgoTokens;
    }

    @Override
    public IconButton getButton(String iconStyle) {

        IconButton iconButton = new IconButton(iconStyle == IconStyle.ROW ? ErgoWallets.getSmallAppIcon() : ErgoWallets.getAppIcon(), getName(), iconStyle) {
            @Override
            public void open() {
                getOpen();
            }
        };

        iconButton.setButtonId(getNetworkId());
        return iconButton;
    }
}
