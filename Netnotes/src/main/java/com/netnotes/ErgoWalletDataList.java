package com.netnotes;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.ergoplatform.appkit.Mnemonic;
import org.ergoplatform.appkit.MnemonicValidationException;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.SecretString;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netnotes.IconButton.IconStyle;
import com.satergo.Wallet;

import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ErgoWalletDataList {

    private File logFile = new File("netnotes-log.txt");
    private ArrayList<NoteInterface> m_noteInterfaceList = new ArrayList<>();
    private String m_selectedId;
    private VBox m_gridBox;
    //  private double m_width = 400;
    // private String m_direction = "column";

    private ErgoWallets m_ergoWallet;
    private File m_dataFile;
    private SimpleDoubleProperty m_gridWidth;
    private SimpleStringProperty m_iconStyle;
    private double m_stageWidth = 600;
    private double m_stageHeight = 450;

    public ErgoWalletDataList(double width, String iconStyle, File dataFile, File walletsDirectory, ErgoWallets ergoWallet) {
        m_gridWidth = new SimpleDoubleProperty(width);
        m_iconStyle = new SimpleStringProperty(iconStyle);
        m_gridBox = new VBox();
        


        m_ergoWallet = ergoWallet;
        m_dataFile = dataFile;
        readFile(m_ergoWallet.getNetworksData().getAppData().appKeyProperty().get());

        m_iconStyle.addListener((obs, oldval, newval) -> updateGrid());
    }

     public ErgoWalletDataList(SecretKey secretKey, File dataFile, ErgoWallets ergoWallet) {
      
    
        m_ergoWallet = ergoWallet;
        m_dataFile = dataFile;
        readFile(secretKey);
        save();
    }

    public SimpleDoubleProperty gridWidthProperty() {
        return m_gridWidth;
    }

    public SimpleStringProperty iconStyleProperty() {
        return m_iconStyle;
    }

    public void readFile(SecretKey secretKey) {
        if (m_dataFile != null && m_dataFile.isFile()) {
            try {

                openJson(Utils.readJsonFile(secretKey, m_dataFile));

            } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                    | IOException e) {

            }
        }
    }

    public void openJson(JsonObject json) {
        if (json != null) {
            JsonElement stageElement = json.get("stage");
            JsonElement walletsElement = json.get("wallets");

            if (stageElement != null && stageElement.isJsonObject()) {
                JsonObject stageObject = stageElement.getAsJsonObject();
                JsonElement widthElement = stageObject.get("width");
                JsonElement heightElement = stageObject.get("height");

                if (widthElement != null && widthElement.isJsonPrimitive()) {
                    m_stageWidth = widthElement.getAsDouble();
                }
                m_stageHeight = heightElement != null && heightElement.isJsonPrimitive() ? heightElement.getAsDouble() : m_stageHeight;
            }

            if (walletsElement != null && walletsElement.isJsonArray()) {
                JsonArray jsonArray = walletsElement.getAsJsonArray();

                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonElement jsonElement = jsonArray.get(i);

                    if (jsonElement != null && jsonElement.isJsonObject()) {
                        JsonObject jsonObject = jsonElement.getAsJsonObject();

                        if (jsonObject != null) {
                            JsonElement nameElement = jsonObject.get("name");
                            JsonElement idElement = jsonObject.get("id");
                            JsonElement fileLocationElement = jsonObject.get("file");

                            String id = idElement == null ? FriendlyId.createFriendlyId() : idElement.getAsString();
                            String name = nameElement == null ? "Wallet " + id : nameElement.getAsString();
                            File walletFile = fileLocationElement != null && fileLocationElement.isJsonPrimitive() ? new File(fileLocationElement.getAsString()) : null;

                            if(walletFile != null && walletFile.isFile()){
                                ErgoWalletData walletData = new ErgoWalletData(id, name, jsonObject, m_ergoWallet);
                                
                                m_noteInterfaceList.add(walletData);

                                walletData.addUpdateListener((obs, oldValue, newValue) -> save());
                            }
                        }
                    }
                }

            }
        }
    }

    public File getWalletsDirectory() {
        return m_ergoWallet.getWalletsDirectory();
    }

    public void add(ErgoWalletData walletData) {
        m_noteInterfaceList.add(walletData);
        walletData.addUpdateListener((obs, oldval, newVal) -> save());
        updateGrid();

    }

    public void remove(String id) {
        for (NoteInterface noteInterface : m_noteInterfaceList) {
            if (noteInterface.getNetworkId().equals(id)) {
                noteInterface.removeUpdateListener();
                m_noteInterfaceList.remove(noteInterface);
                break;
            }
        }

        updateGrid();

    }

    public final static String INSTALL_NODES = "(install 'Ergo Nodes')";
    public final static String INSTALL_EXPLORERS = "(install 'Ergo Explorer')";
    public final static String INSTALL_MARKETS = "(install 'Ergo Markets')";

    public void showAddWalletStage() {
        String friendlyId = FriendlyId.createFriendlyId();


        
        SimpleObjectProperty<ErgoNodeData> selectedNodeData = new SimpleObjectProperty<>(null);
        SimpleObjectProperty<ErgoExplorerData> selectedExplorerData = new SimpleObjectProperty<ErgoExplorerData>(null);
        SimpleObjectProperty<ErgoMarketsData> selectedMarketsData = new SimpleObjectProperty<>(null);
        SimpleBooleanProperty ergoTokensEnabledProperty = new SimpleBooleanProperty(m_ergoWallet.getErgoNetworkData().getNetwork(ErgoTokens.NETWORK_ID) != null ? true : false);
        SimpleObjectProperty<NetworkType> selectedNetworkType = new SimpleObjectProperty<>(NetworkType.MAINNET);

      

        Image icon = m_ergoWallet.getIcon();
        String name = m_ergoWallet.getName();

        VBox layoutBox = new VBox();

        Stage stage = new Stage();
        stage.getIcons().add(ErgoWallets.getSmallAppIcon());
        stage.setResizable(false);
        stage.initStyle(StageStyle.UNDECORATED);

        Scene walletScene = new Scene(layoutBox, m_stageWidth, m_stageHeight);
        walletScene.setFill(null);
        Button maximizeBtn = new Button();

        String heading = "New";
        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(icon, maximizeBtn, closeBtn, stage);
        String titleString = heading + " - " + name;
        stage.setTitle(titleString);

        Text headingText = new Text(heading);
        headingText.setFont(App.txtFont);
        headingText.setFill(Color.WHITE);

        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(40);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 15, 10, 15));
        headingBox.setId("headingBox");


        HBox headingPaddingBox = new HBox(headingBox);

        headingPaddingBox.setPadding(new Insets(5, 0, 2, 0));

        VBox headerBox = new VBox(headingPaddingBox);

        headerBox.setPadding(new Insets(2, 5, 0, 5));

        Text walletName = new Text(String.format("%-15s", "Name"));
        walletName.setFill(App.txtColor);
        walletName.setFont(App.txtFont);

        TextField walletNameField = new TextField("Wallet #" + friendlyId);
        walletNameField.setFont(App.txtFont);
        walletNameField.setId("formField");
        HBox.setHgrow(walletNameField, Priority.ALWAYS);

        HBox walletNameBox = new HBox(walletName, walletNameField);
        walletNameBox.setAlignment(Pos.CENTER_LEFT);

        Text networkTxt = new Text(String.format("%-15s", "Node"));
        networkTxt.setFill(App.txtColor);
        networkTxt.setFont(App.txtFont);

    
        MenuButton nodesMenuBtn = new MenuButton();
        nodesMenuBtn.setFont(App.txtFont);
        nodesMenuBtn.setTextFill(App.altColor);
        nodesMenuBtn.setOnAction(e->{
            if(nodesMenuBtn.getText().equals(INSTALL_NODES)){
                m_ergoWallet.getErgoNetworkData().showwManageStage();
            }
        });
    
        Runnable updateNodeBtn = () ->{
            NoteInterface noteInterface = m_ergoWallet.getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID);

            if(selectedNodeData.get() != null && noteInterface != null){
                
                ErgoNodeData selectedNode = selectedNodeData.get();

                nodesMenuBtn.setText(selectedNode.getName());
            
            }else{
                
                if(noteInterface == null){
                    nodesMenuBtn.setText(INSTALL_NODES);
                }else{
                    nodesMenuBtn.setText("Select node...");
                }
            }
        
        };
       
        Runnable getAvailableNodes = () ->{
            ErgoNodes ergoNodes = (ErgoNodes) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID);
            if(ergoNodes != null){
                ergoNodes.getErgoNodesList().getMenu(nodesMenuBtn, selectedNodeData);
              
            }else{
                nodesMenuBtn.getItems().clear();
                selectedNodeData.set(null);
            }
            updateNodeBtn.run();
        };
        
        
      
        Text networkTypeTxt = new Text(String.format("%-15s", "Network type"));
        networkTypeTxt.setFill(App.txtColor);
        networkTypeTxt.setFont(App.txtFont);

        MenuButton walletTypeMenuBtn = new MenuButton();
        walletTypeMenuBtn.setFont(App.txtFont);
        walletTypeMenuBtn.setTextFill(App.altColor);

        walletTypeMenuBtn.textProperty().bind(selectedNetworkType.asString());
     

        MenuItem testnetItem = new MenuItem(NetworkType.TESTNET.toString());
        testnetItem.setOnAction(e -> {
            selectedNetworkType.set(NetworkType.TESTNET);
        });
        MenuItem mainnetItem = new MenuItem(NetworkType.MAINNET.toString());
        mainnetItem.setOnAction(e -> {
           selectedNetworkType.set(NetworkType.MAINNET);
        });
        walletTypeMenuBtn.getItems().add(mainnetItem);
        walletTypeMenuBtn.getItems().add(testnetItem);

        HBox networkSelectBox = new HBox(networkTxt, nodesMenuBtn);
        networkSelectBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(networkSelectBox, Priority.ALWAYS);
        nodesMenuBtn.prefWidthProperty().bind(networkSelectBox.widthProperty().subtract(networkTxt.getLayoutBounds().getWidth()).subtract(10));

        HBox networkTypeBox = new HBox(networkTypeTxt, walletTypeMenuBtn);
        networkTypeBox.setAlignment(Pos.CENTER_LEFT);

        Text explorerText = new Text(String.format("%-15s", "Explorer"));
        explorerText.setFill(App.txtColor);
        explorerText.setFont(App.txtFont);

       

        MenuButton explorersBtn = new MenuButton(ErgoExplorers.NAME);
        explorersBtn.setFont(App.txtFont);
        explorersBtn.setTextFill(App.altColor);


        HBox explorerBox = new HBox(explorerText, explorersBtn);
        explorerBox.setAlignment(Pos.CENTER_LEFT);

        Runnable updateExplorerBtn = () ->{
            NoteInterface noteInterface = m_ergoWallet.getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);
            ErgoExplorerData explorerData = selectedExplorerData.get();
            if(explorerData != null && noteInterface != null){
            
                explorersBtn.setText(explorerData.getName());
            
            }else{
                
                if(noteInterface == null){
                    explorersBtn.setText(INSTALL_EXPLORERS);
                }else{
                    explorersBtn.setText("Select Explorer...");
                }
            }
        
        };

        Runnable getAvailableExplorers = () ->{
            ErgoExplorers ergoExplorers = (ErgoExplorers) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);
            if(ergoExplorers != null){
                ergoExplorers.getErgoExplorersList().getMenu(explorersBtn, selectedExplorerData);
            }else{
                explorersBtn.getItems().clear();
                selectedExplorerData.set(null);
            }
            updateExplorerBtn.run();
        };

        

        Text marketTxt = new Text(String.format("%-15s", "Market"));
        marketTxt.setFill(App.txtColor);
        marketTxt.setFont(App.txtFont);

        MenuButton marketBtn = new MenuButton();
        marketBtn.setFont(App.txtFont);

        HBox marketBox = new HBox(marketTxt, marketBtn);
        marketBox.setAlignment(Pos.CENTER_LEFT);
        
        Runnable updateMarketsBtn = () ->{
            ErgoMarketsData marketsData = selectedMarketsData.get();
            NoteInterface noteInterface = m_ergoWallet.getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID);

            if(marketsData != null && noteInterface != null){
                marketBtn.setText(marketsData.getName());
            }else{
               
                if(noteInterface == null){
                    marketBtn.setText(INSTALL_MARKETS);
                }else{
                    marketBtn.setText("Select market...");
                }
            }
        
        };

        Runnable getAvailableMarkets = () ->{
            ErgoMarkets ergoMarkets = (ErgoMarkets) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID);
            if(ergoMarkets != null){
                ergoMarkets.getErgoMarketsList().getMenu(marketBtn, selectedMarketsData);
             
            }else{
                marketBtn.getItems().clear();
                selectedMarketsData.set(null);
            }
            updateMarketsBtn.run();
        };

       

        
        Text tokensTxt = new Text(String.format("%-15s", "Ergo Tokens"));
        tokensTxt.setFill(App.txtColor);
        tokensTxt.setFont(App.txtFont);

        

        

        MenuButton tokensBtn = new MenuButton();
        tokensBtn.setFont(App.txtFont);
    
        tokensBtn.setPrefWidth(200);
        tokensBtn.setOnAction(e->{
            if(tokensBtn.getText().equals("(install 'Ergo Tokens')")){
                m_ergoWallet.getErgoNetworkData().showwManageStage();
            }
        });

        Runnable updateTokensBtn = ()->{
            boolean ergoTokensAvailable = m_ergoWallet.getErgoNetworkData().getNetwork(ErgoTokens.NETWORK_ID) != null;

            if(!ergoTokensAvailable)
            {
                tokensBtn.setText("(install 'Ergo Tokens')");
            }else{
                tokensBtn.setText( ergoTokensEnabledProperty.get() ? "Enabled" : "Disabled");
            }
        };
        
        ergoTokensEnabledProperty.addListener((obs,oldval,newval)->updateTokensBtn.run());

        HBox tokensBox = new HBox(tokensTxt, tokensBtn);
        tokensBox.setAlignment(Pos.CENTER_LEFT);

      
        MenuItem tokensEnabledItem = new MenuItem("Enabled");
        tokensEnabledItem.setOnAction(e->{
            ergoTokensEnabledProperty.set(true);
        });

        MenuItem tokensDisabledItem = new MenuItem("Disabled");
        tokensDisabledItem.setOnAction(e->{
            ergoTokensEnabledProperty.set(false);
        });

        Runnable getAvailabledErgoTokens = ()->{
            tokensBtn.getItems().clear();
            ErgoTokens ergoTokens = (ErgoTokens) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoTokens.NETWORK_ID);  
            if(ergoTokens != null){
                tokensBtn.getItems().addAll(tokensEnabledItem, tokensDisabledItem);
            }
            updateTokensBtn.run();
        };

        Text walletTxt = new Text(String.format("%-15s", ""));
        walletTxt.setFont(App.txtFont);
        walletTxt.setFill(App.txtColor);

        Button newWalletBtn = new Button("Create");
        newWalletBtn.setId("menuBarBtn");
        newWalletBtn.setPadding(new Insets(2, 10, 2, 10));
        newWalletBtn.setFont(App.txtFont);

        getAvailableNodes.run();
        getAvailableMarkets.run();
        getAvailableExplorers.run();
        getAvailabledErgoTokens.run();

      
        
        
        


        m_ergoWallet.getErgoNetworkData().addNetworkListener((ListChangeListener.Change<? extends NoteInterface> c) -> {
            getAvailableNodes.run();
            getAvailableExplorers.run();
            getAvailableMarkets.run();
            getAvailabledErgoTokens.run();
        });

        selectedExplorerData.addListener((obs, oldval,newval) -> updateExplorerBtn.run());
        selectedNodeData.addListener((obs, oldval,newval) -> updateNodeBtn.run());
        selectedMarketsData.addListener((obs, oldVal, newVal)-> updateMarketsBtn.run());
        ergoTokensEnabledProperty.addListener((obs,oldval,newVal)->updateTokensBtn.run());

           

        if(m_ergoWallet.getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID) != null){
            ErgoNodes ergoNodes = (ErgoNodes) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID);
            String defaultId = ergoNodes.getErgoNodesList().defaultNodeIdProperty().get();
         
            if(defaultId != null){
                ErgoNodeData ergoNodeData = ergoNodes.getErgoNodesList().getErgoNodeData(defaultId);
                selectedNodeData.set(ergoNodeData);
             
            }
        }

        if(m_ergoWallet.getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID) != null){
            ErgoExplorers ergoExplorers = (ErgoExplorers) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);
            String defaultId = ergoExplorers.getErgoExplorersList().defaultIdProperty().get();
            if(defaultId != null){
                ErgoExplorerData ergoExplorerData = ergoExplorers.getErgoExplorersList().getErgoExplorerData(defaultId);
                selectedExplorerData.set(ergoExplorerData);
            }
        }

        if(m_ergoWallet.getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID ) != null){
            ErgoMarkets ergoMarkets = (ErgoMarkets) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID);
            String defaultId = ergoMarkets.getErgoMarketsList().defaultIdProperty().get();
            if(defaultId != null){
                ErgoMarketsData ergoMarketsData = ergoMarkets.getErgoMarketsList().getMarketsData(defaultId);
                selectedMarketsData.set(ergoMarketsData);
            }
        }

        Rectangle windowBounds = m_ergoWallet.getNetworksData().getMaximumWindowBounds();

        newWalletBtn.setOnAction(newWalletEvent -> {
            NetworkType networkType = selectedNetworkType.get();
            String nodeId = selectedNodeData.get() == null ? null : selectedNodeData.get().getId();
            String explorerId = selectedExplorerData.get() == null ? null : selectedExplorerData.get().getId();
            String marketsId = selectedMarketsData.get() == null ? null : selectedMarketsData.get().getId();
            boolean tokensEnabled = ergoTokensEnabledProperty.get();
            
            Scene mnemonicScene = createMnemonicScene(friendlyId, walletNameField.getText(), nodeId, explorerId, marketsId, tokensEnabled, networkType, stage);
            stage.setScene(mnemonicScene);
            ResizeHelper.addResizeListener(stage, 500, 425, windowBounds.getWidth(), windowBounds.getHeight());
        });

        Button existingWalletBtn = new Button("Open");
        existingWalletBtn.setId("menuBarBtn");
        existingWalletBtn.setPadding(new Insets(2, 10, 2, 10));

        existingWalletBtn.setFont(App.txtFont);

        existingWalletBtn.setOnAction(clickEvent -> {
            FileChooser openFileChooser = new FileChooser();
            openFileChooser.setInitialDirectory(getWalletsDirectory());
            openFileChooser.setTitle("Open: Wallet file");
            openFileChooser.getExtensionFilters().add(ErgoWallets.ergExt);
            openFileChooser.setSelectedExtensionFilter(ErgoWallets.ergExt);

            File walletFile = openFileChooser.showOpenDialog(stage);

            if (walletFile != null) {

                NetworkType networkType = selectedNetworkType.get();
                String nodeId = selectedNodeData.get() != null ? selectedNodeData.get().getId() : null;
                String explorerId = selectedExplorerData.get() == null ? null : selectedExplorerData.get().getId();
                String marketId = selectedMarketsData.get() == null ? null : selectedMarketsData.get().getId();
                boolean tokensEnabled = ergoTokensEnabledProperty.get();

                ErgoWalletData walletData = new ErgoWalletData(friendlyId, walletNameField.getText(), walletFile, nodeId, explorerId, marketId, tokensEnabled, networkType, m_ergoWallet);

                add(walletData);
                save();
                stage.close();

            }
        });

        Button restoreWalletBtn = new Button("Restore");
        restoreWalletBtn.setId("menuBarBtn");
        restoreWalletBtn.setPadding(new Insets(2, 5, 2, 5));
        restoreWalletBtn.setFont(App.txtFont);

        restoreWalletBtn.setOnAction(clickEvent -> {
            String seedPhrase = restoreMnemonicStage();
            if (!seedPhrase.equals("")) {
                Button passBtn = new Button();
                Stage passwordStage = App.createPassword(m_ergoWallet.getName() + " - Restore wallet: Password", m_ergoWallet.getIcon(), ErgoWallets.getAppIcon(),passBtn, onSuccess -> {
                    Object sourceObject = onSuccess.getSource().getValue();

                    if (sourceObject != null && sourceObject instanceof String) {

                        String passwordString = (String) sourceObject;
                        if (!passwordString.equals("")) {
                            Mnemonic mnemonic = Mnemonic.create(SecretString.create(seedPhrase), SecretString.create(passwordString));

                            FileChooser saveFileChooser = new FileChooser();
                            saveFileChooser.setInitialDirectory(getWalletsDirectory());
                            saveFileChooser.setTitle("Save: Wallet file");
                            saveFileChooser.getExtensionFilters().add(ErgoWallets.ergExt);
                            saveFileChooser.setSelectedExtensionFilter(ErgoWallets.ergExt);

                            File walletFile = saveFileChooser.showSaveDialog(stage);

                            if (walletFile != null) {

                                try {
                                    

                                    Wallet.create(walletFile.toPath(), mnemonic, seedPhrase, passwordString.toCharArray());

                                    NetworkType networkType = selectedNetworkType.get();
                                    String nodeId = selectedNodeData.get() == null ? null : selectedNodeData.get().getId();
                                    String explorerId =  selectedExplorerData.get() == null ? null : selectedExplorerData.get().getId();
                                    String marketsId = selectedMarketsData.get() == null ? null : selectedMarketsData.get().getId();
                                    boolean tokensEnabled =ergoTokensEnabledProperty.get();

                                    ErgoWalletData walletData = new ErgoWalletData(friendlyId, walletNameField.getText(), walletFile, nodeId, explorerId, marketsId, tokensEnabled, networkType, m_ergoWallet);
                                    add(walletData);
                                    save();

                                   
                                } catch (Exception e1) {
                                    Alert a = new Alert(AlertType.NONE, "Wallet creation: Cannot be saved.\n\n" + e1.toString(), ButtonType.OK);
                                    a.initOwner(stage);
                                    a.show();
                                }
                            }

                        }
                        stage.setScene(walletScene);
                        passBtn.fire();
                    }
                });
                passwordStage.show();
            }

        });

        HBox newWalletBox = new HBox(newWalletBtn, existingWalletBtn, restoreWalletBtn);
        newWalletBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(newWalletBox, Priority.ALWAYS);

        HBox walletBox = new HBox(walletTxt);
        walletBox.setAlignment(Pos.CENTER_LEFT);

        SimpleDoubleProperty rowHeight = new SimpleDoubleProperty(40);

        walletNameBox.minHeightProperty().bind(rowHeight);
        networkSelectBox.minHeightProperty().bind(rowHeight);
        networkTypeBox.minHeightProperty().bind(rowHeight);
        explorerBox.minHeightProperty().bind(rowHeight);
        marketBox.minHeightProperty().bind(rowHeight);
        walletBox.minHeightProperty().bind(rowHeight);
        newWalletBox.minHeightProperty().bind(rowHeight);
        tokensBox.minHeightProperty().bind(rowHeight);

        newWalletBtn.prefHeightProperty().bind(rowHeight);
        restoreWalletBtn.prefHeightProperty().bind(rowHeight);
        existingWalletBtn.prefHeightProperty().bind(rowHeight);

        newWalletBtn.prefWidthProperty().bind(walletScene.widthProperty().subtract(20).divide(3));

        existingWalletBtn.prefWidthProperty().bind(walletScene.widthProperty().subtract(20).divide(3));

        restoreWalletBtn.prefWidthProperty().bind(walletScene.widthProperty().subtract(30).divide(3));

        VBox mainBodyBox = new VBox(walletNameBox, networkTypeBox, networkSelectBox, explorerBox, marketBox,tokensBox, walletBox);
        HBox.setHgrow(mainBodyBox, Priority.ALWAYS);
        Region leftP = new Region();
        leftP.setPrefWidth(40);

        HBox bodyBox = new HBox(leftP, mainBodyBox);
        bodyBox.setId("bodyBox");
        bodyBox.setPadding(new Insets(0, 5, 0, 20));

        VBox bodyPaddBox = new VBox(bodyBox);
        bodyPaddBox.setPadding(new Insets(0, 5, 10, 5));

        VBox footerBox = new VBox(newWalletBox);
        footerBox.setAlignment(Pos.CENTER_RIGHT);
        footerBox.setPadding(new Insets(5, 20, 0, 20));

        layoutBox.getChildren().addAll(titleBox, headerBox, bodyPaddBox, footerBox);
        walletScene.getStylesheets().add("/css/startWindow.css");
        stage.setScene(walletScene);
        Rectangle maxSize = m_ergoWallet.getNetworksData().getMaximumWindowBounds();

        ResizeHelper.addResizeListener(stage, 380, 460, maxSize.getWidth(), maxSize.getHeight());

        ChangeListener<Number> walletWidthListener = (obs, oldval, newVal) -> {
            m_stageWidth = newVal.doubleValue();
        };

        ChangeListener<Number> walletHeightListener = (obs, oldval, newVal) -> {
            m_stageHeight = newVal.doubleValue();
            // double height = m_stageHeight - titleBox.heightProperty().get() - headerBox.heightProperty().get();
            // rowHeight.set((height-5) / 6);
        };

        rowHeight.bind(stage.heightProperty().subtract(titleBox.heightProperty()).subtract(headerBox.heightProperty()).subtract(35).divide(8));
        walletScene.widthProperty().addListener(walletWidthListener);

        walletScene.heightProperty().addListener(walletHeightListener);
        closeBtn.setOnAction(e -> {
            stage.close();
        });
        stage.show();

    }

    public Scene createMnemonicScene(String id, String name, String nodeId,  String explorerId, String marketsId,boolean ergoTokensEnabled, NetworkType networkType, Stage stage) {        //String oldStageName = mnemonicStage.getTitle();

        String titleStr = "Mnemonic phrase - " + m_ergoWallet.getName();

        stage.setTitle(titleStr);

        Button closeBtn = new Button();
        Button maximizeBtn = new Button();

        HBox titleBox = App.createTopBar(ErgoWallets.getSmallAppIcon(), maximizeBtn, closeBtn, stage);

        //Region spacer = new Region();
        //HBox.setHgrow(spacer, Priority.ALWAYS);
     /*
        HBox menuBar = new HBox();
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(1, 0, 1, 5));

        VBox menuPaddingBox = new VBox(menuBar);
        menuPaddingBox.setPadding(new Insets(0, 2, 5, 2));
        */
        Text headingText = new Text("Mnemonic phrase");
        headingText.setFill(App.txtColor);
        headingText.setFont(App.txtFont);

        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(40);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 15, 10, 15));
        headingBox.setId("headingBox");

        VBox headerBox = new VBox(headingBox);
        headerBox.setPadding(new Insets(0, 5, 2, 5));

        TextArea mnemonicField = new TextArea(Mnemonic.generateEnglishMnemonic());
        mnemonicField.setFont(App.txtFont);
        mnemonicField.setId("textFieldCenter");
        mnemonicField.setEditable(false);
        mnemonicField.setWrapText(true);
        mnemonicField.setPrefRowCount(2);
        HBox.setHgrow(mnemonicField, Priority.ALWAYS);

        Platform.runLater(() -> mnemonicField.requestFocus());

        HBox mnemonicFieldBox = new HBox(mnemonicField);

        VBox mnemonicBox = new VBox(mnemonicFieldBox);
        mnemonicBox.setAlignment(Pos.CENTER);
        mnemonicBox.setPadding(new Insets(20, 30, 0, 30));

        Region hBar = new Region();
        hBar.setPrefWidth(400);
        hBar.setPrefHeight(2);
        hBar.setId("hGradient");

        HBox gBox = new HBox(hBar);
        gBox.setAlignment(Pos.CENTER);
        gBox.setPadding(new Insets(15, 0, 0, 0));

        Button nextBtn = new Button("Next");

        nextBtn.setFont(App.txtFont);

        HBox nextBox = new HBox(nextBtn);
        nextBox.setAlignment(Pos.CENTER);
        nextBox.setPadding(new Insets(25, 0, 0, 0));

        VBox bodyBox = new VBox(mnemonicBox, gBox, nextBox);
        VBox.setMargin(bodyBox, new Insets(5, 10, 0, 20));
        VBox.setVgrow(bodyBox, Priority.ALWAYS);

        VBox layoutVBox = new VBox(titleBox, headerBox, bodyBox);

        mnemonicFieldBox.setMaxWidth(900);
        HBox.setHgrow(mnemonicFieldBox, Priority.ALWAYS);

        Scene mnemonicScene = new Scene(layoutVBox, 600, 425);
        mnemonicScene.setFill(null);
        mnemonicScene.getStylesheets().add("/css/startWindow.css");

        mnemonicBox.prefHeightProperty().bind(mnemonicScene.heightProperty().subtract(titleBox.heightProperty()).subtract(headerBox.heightProperty()).subtract(130));

        closeBtn.setOnAction(e -> {
            mnemonicField.setText("");
            stage.close();

        });

 

        nextBtn.setOnAction(nxtEvent -> {
            Alert nextAlert = new Alert(AlertType.NONE, "This mnemonic phrase may be used to generate backup or to recover this wallet if it is lost. It is strongly recommended to always maintain a paper copy of this phrase in a secure location. \n\nWarning: Loss of your mnemonic phrase could lead to the loss of your ability to recover this wallet.", ButtonType.CANCEL, ButtonType.OK);
            nextAlert.setHeaderText("Notice");
            nextAlert.initOwner(stage);
            nextAlert.setTitle("Notice - Mnemonic phrase - Add wallet");
            Optional<ButtonType> result = nextAlert.showAndWait();

            if(result != null && result.isPresent() && result.get() == ButtonType.OK){

                 Button closePassBtn = new Button();
                Stage passwordStage = App.createPassword("Wallet password - " + ErgoWallets.NAME, ErgoWallets.getSmallAppIcon(), ErgoWallets.getAppIcon(), closePassBtn, onSuccess -> {
                    Object sourceObject = onSuccess.getSource().getValue();

                    if (sourceObject != null && sourceObject instanceof String) {

                        String password = (String) sourceObject;
                        if (!password.equals("")) {

                            FileChooser saveFileChooser = new FileChooser();
                            saveFileChooser.setInitialDirectory(getWalletsDirectory());
                            saveFileChooser.setTitle("Save: Wallet file");
                            saveFileChooser.getExtensionFilters().add(ErgoWallets.ergExt);
                            saveFileChooser.setSelectedExtensionFilter(ErgoWallets.ergExt);

                            File walletFile = saveFileChooser.showSaveDialog(stage);
                            int indexOfDecimal = walletFile != null ? walletFile.getName().lastIndexOf(".") : -1;
                            walletFile = walletFile != null ? (indexOfDecimal != -1 && walletFile.getName().substring(indexOfDecimal).equals("erg") ? walletFile : new File(walletFile.getAbsolutePath() + ".erg")) : null;
                            if (walletFile != null) {
                                

                                Wallet.create(walletFile.toPath(), Mnemonic.create(SecretString.create(mnemonicField.getText()), SecretString.create(password)), walletFile.getName(), password.toCharArray());
                                mnemonicField.setText("-");

                                ErgoWalletData walletData = new ErgoWalletData(id, name, walletFile, nodeId, explorerId, marketsId, ergoTokensEnabled, networkType, m_ergoWallet);
                                add(walletData);
                                save();
                              
                            }
                  
                            
                            closePassBtn.fire();
                            stage.close();
                        }else{
                            Alert a = new Alert(AlertType.NONE, "Enter a password for the wallet file.", ButtonType.OK);
                            a.setTitle("Password Required");
                            a.setHeaderText("Password Required");
                            a.show();
                        }

                    }else{
                        closePassBtn.fire();
                    }
                    
                });
                closePassBtn.setOnAction(e->{
                    passwordStage.close();
             
                });
                passwordStage.show();
                Platform.runLater(()->passwordStage.requestFocus());
                Platform.runLater(()->passwordStage.toFront());
            }
        });

        return mnemonicScene;
    }

    public String restoreMnemonicStage() {
        String titleStr = m_ergoWallet.getName() + " - Restore wallet: Mnemonic phrase";

        Stage mnemonicStage = new Stage();

        mnemonicStage.setTitle(titleStr);

        mnemonicStage.getIcons().add(m_ergoWallet.getIcon());

        mnemonicStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(m_ergoWallet.getIcon(), titleStr, closeBtn, mnemonicStage);

        Button imageButton = App.createImageButton(m_ergoWallet.getIcon(), "Restore wallet");

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

        Text subTitleTxt = new Text("> Mnemonic phrase:");
        subTitleTxt.setFill(App.txtColor);
        subTitleTxt.setFont(App.txtFont);

        HBox subTitleBox = new HBox(subTitleTxt);
        subTitleBox.setAlignment(Pos.CENTER_LEFT);

        TextArea mnemonicField = new TextArea();
        mnemonicField.setFont(App.txtFont);
        mnemonicField.setId("formField");

        mnemonicField.setWrapText(true);
        mnemonicField.setPrefRowCount(2);
        HBox.setHgrow(mnemonicField, Priority.ALWAYS);

        Platform.runLater(() -> mnemonicField.requestFocus());

        HBox mnemonicBox = new HBox(mnemonicField);
        mnemonicBox.setPadding(new Insets(20, 30, 0, 30));

        Region hBar = new Region();
        hBar.setPrefWidth(400);
        hBar.setPrefHeight(2);
        hBar.setId("hGradient");

        HBox gBox = new HBox(hBar);
        gBox.setAlignment(Pos.CENTER);
        gBox.setPadding(new Insets(15, 0, 0, 0));

        Button nextBtn = new Button("Words left: 15");
        nextBtn.setId("toolBtn");
        nextBtn.setFont(App.txtFont);
        nextBtn.setDisable(true);
        nextBtn.setOnAction(nxtEvent -> {
            String mnemonicString = mnemonicField.getText();;

            String[] words = mnemonicString.split("\\s+");

            List<String> mnemonicList = Arrays.asList(words);
            try {
                Mnemonic.checkEnglishMnemonic(mnemonicList);
                mnemonicStage.close();
            } catch (MnemonicValidationException e) {
                Alert a = new Alert(AlertType.NONE, "Error: Mnemonic invalid\n\nPlease correct the mnemonic phrase and try again.", ButtonType.CLOSE);
                a.initOwner(mnemonicStage);
                a.setTitle("Error: Mnemonic invalid.");
            }

        });

        mnemonicField.setOnKeyPressed(e1 -> {
            String mnemonicString = mnemonicField.getText();;

            String[] words = mnemonicString.split("\\s+");
            int numWords = words.length;
            if (numWords == 15) {
                nextBtn.setText("Ok");

                List<String> mnemonicList = Arrays.asList(words);
                try {
                    Mnemonic.checkEnglishMnemonic(mnemonicList);
                    nextBtn.setDisable(false);

                } catch (MnemonicValidationException e) {
                    nextBtn.setText("Invalid");
                    nextBtn.setId("toolBtn");
                    nextBtn.setDisable(true);
                }

            } else {
                if (nextBtn.getText().equals("")) {
                    nextBtn.setText("Words left: 15");
                } else {
                    nextBtn.setText("Words left: " + (15 - numWords));
                }

                nextBtn.setId("toolBtn");
                nextBtn.setDisable(true);
            }

        });

        HBox nextBox = new HBox(nextBtn);
        nextBox.setAlignment(Pos.CENTER);
        nextBox.setPadding(new Insets(25, 0, 0, 0));

        VBox bodyBox = new VBox(subTitleBox, mnemonicBox, gBox, nextBox);
        VBox.setMargin(bodyBox, new Insets(5, 10, 0, 20));
        VBox.setVgrow(bodyBox, Priority.ALWAYS);

        VBox layoutVBox = new VBox(titleBox, imageBox, bodyBox);

        Scene mnemonicScene = new Scene(layoutVBox, 600, 425);
        mnemonicScene.setFill(null);
        mnemonicScene.getStylesheets().add("/css/startWindow.css");
        mnemonicStage.setScene(mnemonicScene);

        closeBtn.setOnAction(e -> {

            mnemonicStage.close();
            mnemonicField.setText("");

        });

        mnemonicStage.showAndWait();

        return mnemonicField.getText();

    }

    public void setSelected(String networkId) {

        if (m_selectedId != null) {
            NoteInterface prevInterface = getNoteInterface(m_selectedId);
            if (prevInterface != null) {
                prevInterface.getButton().setCurrent(false);
            }
        }
        if (networkId != null) {
            NoteInterface currentInterface = getNoteInterface(networkId);
            if (currentInterface != null) {
                currentInterface.getButton().setCurrent(true);
            }
        }
    }

    public String getSelected() {
        return m_selectedId;
    }

    public NoteInterface getNoteInterface(String networkId) {

        for (NoteInterface noteInterface : m_noteInterfaceList) {
            if (noteInterface.getNetworkId().equals(networkId)) {
                return noteInterface;
            }
        }
        return null;
    }

    public void sendNoteToNetworkId(JsonObject note, String networkId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        m_noteInterfaceList.forEach(noteInterface -> {
            if (noteInterface.getNetworkId().equals(networkId)) {

                noteInterface.sendNote(note, onSucceeded, onFailed);
            }
        });
    }

    public int size() {
        return m_noteInterfaceList.size();
    }

    public JsonArray getJsonArray() {
        JsonArray jsonArray = new JsonArray();

        for (NoteInterface noteInterface : m_noteInterfaceList) {

            JsonObject jsonObj = noteInterface.getJsonObject();
            jsonArray.add(jsonObj);

        }

        return jsonArray;
    }

    public VBox getButtonGrid() {
        updateGrid();
        return m_gridBox;
    }

    public void updateGrid() {

        int numCells = m_noteInterfaceList.size();
        String currentIconStyle = m_iconStyle.get();
        m_gridBox.getChildren().clear();

        if (currentIconStyle.equals(IconStyle.ROW)) {
            for (int i = 0; i < numCells; i++) {
                NoteInterface network = m_noteInterfaceList.get(i);
                IconButton iconButton = network.getButton(currentIconStyle);
                iconButton.prefWidthProperty().bind(m_gridWidth);
                m_gridBox.getChildren().add(iconButton);
            }
        } else {

            double width = m_gridWidth.get();
            double imageWidth = 75;
            double cellPadding = 15;
            double cellWidth = imageWidth + (cellPadding * 2);

            int floor = (int) Math.floor(width / cellWidth);
            int numCol = floor == 0 ? 1 : floor;
            // currentNumCols.set(numCol);
            //int numRows = numCells > 0 && numCol != 0 ? (int) Math.ceil(numCells / (double) numCol) : 1;

            ArrayList<HBox> rowsBoxes = new ArrayList<HBox>();

            ItemIterator grid = new ItemIterator();
            //j = row
            //i = col

            for (NoteInterface noteInterface : m_noteInterfaceList) {
                if(rowsBoxes.size() < (grid.getJ() + 1)){
                    HBox newHBox = new HBox();
                    rowsBoxes.add(newHBox);
                    m_gridBox.getChildren().add(newHBox);
                }
                HBox rowBox = rowsBoxes.get(grid.getJ());
                rowBox.getChildren().add(noteInterface.getButton(currentIconStyle));

                if (grid.getI() < numCol) {
                    grid.setI(grid.getI() + 1);
                } else {
                    grid.setI(0);
                    grid.setJ(grid.getJ() + 1);
                }
            }

        }

    }

    public JsonArray getWalletsJsonArray() {
        JsonArray jsonArray = new JsonArray();

        for (NoteInterface noteInterface : m_noteInterfaceList) {

            JsonObject jsonObj = noteInterface.getJsonObject();
            jsonArray.add(jsonObj);

        }
        return jsonArray;
    }

    public JsonObject getStageJson() {
        JsonObject json = new JsonObject();
        json.addProperty("width", m_stageWidth);
        json.addProperty("height", m_stageHeight);
        return json;
    }

    public void save() {
        JsonObject fileObject = new JsonObject();
        fileObject.add("stage", getStageJson());
        fileObject.add("wallets", getWalletsJsonArray());

        try{
            Utils.saveJson(m_ergoWallet.getNetworksData().getAppData().appKeyProperty().get(), fileObject, m_dataFile);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException | IOException e) {
            try {
                Files.writeString(logFile.toPath(), "\nKey error: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
        }
    }

}
