package com.netnotes;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.zip.ZipEntry;

import java.util.zip.ZipInputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;


import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.utils.Utils;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import ove.crypto.digest.Blake2b;

public class ErgoTokens extends Network implements NoteInterface {

    public final static String DESCRIPTION = "Ergo Tokens allows you to manage your interactions with the tokens on the Ergo Network.";
    public final static String SUMMARY = "Mange your tokens with Ergo Tokens.";
    public final static String NAME = "Ergo Tokens";
    public final static String NETWORK_ID = "ERGO_TOKENS";

    public final static String[] TOKEN_MARKETS = new String[] { SpectrumFinance.NETWORK_ID };


    private File logFile = new File("netnotes-log.txt");
    private File m_testnetDataFile = null;
    private File m_appDir = null;
    private Stage m_tokensStage = null;

    private NetworkType m_networkType = NetworkType.MAINNET;
    private String m_explorerId = null;

    private ErgoNetworkData m_ergNetData;

   // private boolean m_firstOpen = false;
    

    private String m_marketId = SpectrumFinance.NETWORK_ID;

    public ErgoTokens(ErgoNetworkData ergNetData, ErgoNetwork ergoNetwork) {
        super(getAppIcon(), NAME, NETWORK_ID, ergoNetwork);
        m_ergNetData = ergNetData;
        m_appDir = new File(ergoNetwork.getAppDir().getAbsolutePath() + "/" + NAME);
     
        getLastUpdated().set(LocalDateTime.now());

    }

    public ErgoTokens(ErgoNetworkData ergNetData, JsonObject jsonObject, ErgoNetwork ergoNetwork) {
        super(getAppIcon(), NAME, NETWORK_ID, ergoNetwork);
        m_ergNetData = ergNetData;
        JsonElement networkTypeElement = jsonObject.get("networkType");
        JsonElement explorerIdElement = jsonObject.get("explorerId");
        JsonElement marketIdElement = jsonObject.get("marketId");
        
        m_marketId = marketIdElement != null && marketIdElement.isJsonPrimitive() ? marketIdElement.getAsString() : SpectrumFinance.NETWORK_ID;

        m_explorerId = explorerIdElement != null ? explorerIdElement.getAsString() : null;
        
        if (networkTypeElement != null) {
            if (networkTypeElement.getAsString().equals(NetworkType.TESTNET.toString())) {
                m_networkType = NetworkType.TESTNET;
            } else {
                m_networkType = NetworkType.MAINNET;
            }
        }
        addListeners();
        setup();

        
    }

    public void setMarketId(String marketId){
        m_marketId = marketId;
        getLastUpdated().set(LocalDateTime.now());
    }

    public String getMarketId(){
        return m_marketId;
    }

    public void setup(){
  


        m_appDir = new File(m_ergNetData.getErgoNetwork().getAppDir().getAbsolutePath() + "/" + NAME);
        
        boolean isAppDir = m_appDir.isDirectory();
        if (!isAppDir) {

            try {
                Files.createDirectories(m_appDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }
           
        }
        
        setDataDir(new File(m_appDir.getAbsolutePath()));

        m_testnetDataFile = new File(m_appDir.getAbsolutePath() + "/testnet" + ErgoTokens.NAME + ".dat");
  
        File dataFile = getIdDataFile(NAME);
        if(!isAppDir || !dataFile.isFile()){
           
            File tokensDir = new File(m_appDir.getAbsolutePath() + "/tokens");
            setupTokens(getNetworksData().getAppData().appKeyProperty().get(), tokensDir);
        }
    }
    public void addListeners(){

        
    

        
        getNetworksData().getAppData().appKeyProperty().addListener((obs, oldVal, newVal) -> {
            JsonArray indexArray = getIndexFileArray(oldVal);
            if(indexArray != null){
                saveIndexFile(indexArray);
                for(int i = 0; i< indexArray.size() ; i++){
                    JsonElement fileElement = indexArray.get(i);
                    JsonObject idFileObject = fileElement.getAsJsonObject();
                    JsonElement fileLocationElement = idFileObject.get("file");
                    String fileLocationString = fileLocationElement.getAsString();

                    File file = new File(fileLocationString);
                    
                    if(file.isFile()){
                        try {
                            String fileString = Utils.readStringFile(oldVal, file);
                            Utils.writeEncryptedString(newVal, file, fileString);
                        } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                                | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                                | IOException e) {
                            try {
                                Files.writeString(logFile.toPath(), "ErgoTokens: could not update file: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e1) {
                        
                            }
                        }
                    }
                }
            }
            
            
            
        });
    }

    public String getExplorerId(){
        return m_explorerId;
    }

    public void setExplorerId(String explorerId){
        m_explorerId = explorerId;
        getLastUpdated().set(LocalDateTime.now());
    }

    public ErgoNetworkData getErgoNetworkData(){
        return m_ergNetData;
    }

    @Override
    public void open() {
        showTokensStage();
    }

    private void setNetworkType(NetworkType networkType){
        m_networkType = networkType;
        getLastUpdated().set(LocalDateTime.now());
    }


    public NetworkType getNetworkType() {
        return m_networkType;
    }

    public void showTokensStage() {
        if (m_tokensStage == null) {
            ErgoTokensList tokensList = new ErgoTokensList(getAppKey(), m_networkType, this, m_marketId, m_explorerId);
            

            tokensList.selectedExplorerData().addListener((obs,oldval,newval)->{
                setExplorerId(newval != null ? newval.getId() : null);
            });



            double tokensStageWidth = 375;
            double tokensStageHeight = 600;
            double buttonHeight = 100;



            m_tokensStage = new Stage();
            m_tokensStage.getIcons().add(getIcon());
            m_tokensStage.initStyle(StageStyle.UNDECORATED);
            m_tokensStage.setTitle(getName() + ": Tokens " + (m_networkType == NetworkType.MAINNET ? "(MAINNET)" : "(TESTNET)"));

            Button closeBtn = new Button();
            closeBtn.setOnAction(closeEvent -> {
                m_tokensStage.close();
                m_tokensStage = null;
               
            });

            Button maxBtn = new Button();

            HBox titleBox = App.createTopBar(getIcon(), maxBtn, closeBtn, m_tokensStage);

            BufferedMenuButton menuBtn = new BufferedMenuButton("/assets/menu-outline-30.png", App.MENU_BAR_IMAGE_WIDTH);
  

            MenuItem importBtn = new MenuItem(" Import JSON...");
            importBtn.setId("menuBtn");
            importBtn.setOnAction(action -> {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Import JSON File...");
                chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("text/json", "*.json"));
                File openFile = chooser.showOpenDialog(m_tokensStage);
                if (openFile != null) {
                    tokensList.importJson(m_tokensStage, openFile);
                }
            });

            MenuItem exportBtn = new MenuItem(" Export JSON...");
            exportBtn.setId("menuBtn");
            exportBtn.setOnAction(action -> {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Export JSON file...");
                chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("text/json", "*.json"));
                chooser.setInitialFileName("ergoTokens-" + m_networkType);
                File saveFile = chooser.showSaveDialog(m_tokensStage);

                if (saveFile != null) {
                    try {
                        Files.writeString(saveFile.toPath(), tokensList.getJsonObject().toString());
                    } catch (IOException e) {
                        Alert writeAlert = new Alert(AlertType.NONE, e.toString(), ButtonType.OK);
                        writeAlert.initOwner(m_tokensStage);
                        writeAlert.setGraphic(IconButton.getIconView(getIcon(), 75));

                    }
                }
            });

            menuBtn.getItems().addAll(importBtn, exportBtn);

            Tooltip toggleTip = new Tooltip((m_networkType == NetworkType.MAINNET ? "MAINNET" : "TESTNET"));
            toggleTip.setShowDelay(new javafx.util.Duration(100));
            toggleTip.setFont(App.txtFont);

            BufferedButton toggleNetworkTypeBtn = new BufferedButton(m_networkType == NetworkType.MAINNET ? "/assets/toggle-on.png" : "/assets/toggle-off.png", App.MENU_BAR_IMAGE_WIDTH);
            toggleNetworkTypeBtn.setId("menuBtn");
            toggleNetworkTypeBtn.setTooltip(toggleTip);
            toggleNetworkTypeBtn.setOnAction(e -> {
                setNetworkType(m_networkType == NetworkType.MAINNET ? NetworkType.TESTNET : NetworkType.MAINNET);
             
                tokensList.setNetworkType(getAppKey(), m_networkType);

                toggleTip.setText((m_networkType == NetworkType.MAINNET ? "MAINNET" : "TESTNET"));
                toggleNetworkTypeBtn.setImage(m_networkType == NetworkType.MAINNET ?new Image("/assets/toggle-on.png") : new Image("/assets/toggle-off.png"));
                m_tokensStage.setTitle(getName() + ": Tokens " + (m_networkType == NetworkType.MAINNET ? "(MAINNET)" : "(TESTNET)"));

               
            });

            Tooltip marketsTip = new Tooltip("Token Market: (set default)");
            marketsTip.setShowDelay(new javafx.util.Duration(50));
            marketsTip.setFont(App.txtFont);
     
            BufferedMenuButton marketBtn = new BufferedMenuButton("/assets/ergo-charts-30.png", App.MENU_BAR_IMAGE_WIDTH);
            marketBtn.setPadding(new Insets(2, 0, 0, 2));
            marketBtn.setTooltip(marketsTip);

            

            Tooltip explorerTip = new Tooltip("Select explorer");
            explorerTip.setShowDelay(new javafx.util.Duration(50));
            explorerTip.setFont(App.txtFont);
     
            BufferedMenuButton explorerBtn = new BufferedMenuButton("/assets/ergo-explorer-30.png", App.MENU_BAR_IMAGE_WIDTH);
            explorerBtn.setPadding(new Insets(2, 0, 0, 2));
            explorerBtn.setTooltip(explorerTip);

            

           

            Runnable updateExplorerBtn = () ->{
                ErgoExplorers ergoExplorers = (ErgoExplorers) getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);
            
                ErgoExplorerData explorerData = tokensList.selectedExplorerData().get();
                
            
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
            
                ErgoExplorerList explorerList = tokensList.ergoExplorerListProperty().get();
                if(explorerList != null){
                    explorerBtn.setId("menuBtn");
                    explorerList.getMenu(explorerBtn, tokensList.selectedExplorerData());
                }else{
                    explorerBtn.getItems().clear();
                    explorerBtn.setId("menuBtnDisabled");
                
                }
                updateExplorerBtn.run();
            };

            Runnable updateMarketsBtn = () -> {
                
                ErgoMarketsData marketsData = tokensList.selectedMarketData().get();
                ErgoMarkets ergoMarkets = (ErgoMarkets) getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID);
    
                if (marketsData != null && ergoMarkets != null) {
    
                    marketsTip.setText("Ergo Markets: " + marketsData.getName());
                    tokensList.updateSelectedMarket(marketsData);
                    
                } else {
    
                    if (ergoMarkets == null) {
                        marketsTip.setText("(install 'Ergo Markets')");
                    } else {
                        marketsTip.setText("Select market...");
                    }
                }
    
            };
            tokensList.selectedMarketData().addListener((obs, oldval, newVal) -> {
                setMarketId(newVal != null ? newVal.getMarketId() : null);
                updateMarketsBtn.run();
            });
    
            Runnable getAvailableMarketsMenu = () -> {
                ErgoMarkets ergoMarkets = (ErgoMarkets) getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID);
         
                if (ergoMarkets != null) {
                    marketBtn.setId("menuBtn");
                    
                    ergoMarkets.getErgoMarketsList().getMenu(marketBtn, tokensList.selectedMarketData());
                } else {
                    marketBtn.getItems().clear();
                    marketBtn.setId("menuBtnDisabled");
                }
                updateMarketsBtn.run();
            };
          


            HBox rightSideMenu = new HBox(marketBtn, explorerBtn);
            rightSideMenu.setId("rightSideMenuBar");
            rightSideMenu.setPadding(new Insets(0, 10, 0, 20));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox menuBar = new HBox(menuBtn, toggleNetworkTypeBtn, spacer, rightSideMenu);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 5));

            HBox menuBarPadding = new HBox(menuBar);
            menuBarPadding.setId("darkBox");
            HBox.setHgrow(menuBarPadding, Priority.ALWAYS);
            menuBarPadding.setPadding(new Insets(0,0,4,0));

            ImageView addImage = new ImageView(App.addImg);
            addImage.setFitHeight(10);
            addImage.setPreserveRatio(true);

            Tooltip addTip = new Tooltip("New");
            addTip.setShowDelay(new javafx.util.Duration(100));
            addTip.setFont(App.txtFont);

            VBox layoutVBox = new VBox(titleBox);
            layoutVBox.setPadding(new Insets(0, 5, 0, 5));
            VBox.setVgrow(layoutVBox, Priority.ALWAYS);

            VBox tokensBox = tokensList.getButtonGrid();

            Region growRegion = new Region();

            VBox.setVgrow(growRegion, Priority.ALWAYS);

            VBox bodyBox = new VBox(tokensBox, growRegion);

            ScrollPane scrollPane = new ScrollPane(bodyBox);

            scrollPane.setId("bodyBox");

            Button addButton = new Button("New");
            // addButton.setGraphic(addImage);
            addButton.setId("menuBarBtn");
            addButton.setPadding(new Insets(2, 6, 2, 6));
            addButton.setTooltip(addTip);
            addButton.setPrefWidth(tokensStageWidth / 2);
            addButton.setPrefHeight(buttonHeight);

            Tooltip removeTip = new Tooltip("Remove");
            removeTip.setShowDelay(new javafx.util.Duration(100));
            removeTip.setFont(App.txtFont);

            Button removeButton = new Button("Remove");

            removeButton.setId("menuBarBtnDisabled");
            removeButton.setPadding(new Insets(2, 6, 2, 6));
            removeButton.setTooltip(removeTip);
            removeButton.setDisable(true);
            removeButton.setPrefWidth(tokensStageWidth / 2);
            removeButton.setPrefHeight(buttonHeight);
            removeButton.setUserData(null);

            removeButton.setOnAction(action -> {

                
                ErgoNetworkToken selectedToken = (ErgoNetworkToken) tokensList.selectedTokenProperty().get();
                if(selectedToken != null){
                    Alert a = new Alert(AlertType.NONE, "Would you like to remove '" + selectedToken.getName() + "' from Ergo Tokens?", ButtonType.NO, ButtonType.YES);
                    a.initOwner(m_tokensStage);
                    a.setTitle("Remove Token - " + selectedToken.getName());
                    a.setGraphic(IconButton.getIconView(selectedToken.getIcon(), 40));
                    a.setHeaderText("Remove Token");
                    Optional<ButtonType> result = a.showAndWait();

                    if (result.isPresent() && result.get() == ButtonType.YES) {

                        tokensList.removeToken(selectedToken.getNetworkId());
                    }
                
                }

                removeButton.setDisable(true);
                tokensList.selectedTokenProperty().set(null);
                removeButton.setId("menuBarBtnDisabled");
            });

            HBox menuBox = new HBox(addButton, removeButton);
            HBox.setHgrow(menuBox, Priority.ALWAYS);
            menuBox.setId("blackMenu");
            menuBox.setAlignment(Pos.CENTER_LEFT);
            menuBox.setPadding(new Insets(5, 5, 5, 5));
            menuBox.setPrefHeight(buttonHeight);

            layoutVBox.getChildren().addAll(menuBarPadding, scrollPane, menuBox);

            Scene tokensScene = new Scene(layoutVBox, tokensStageWidth, tokensStageHeight);
            tokensScene.setFill(null);
            getErgoNetworkData().addNetworkListener((ListChangeListener.Change<? extends NoteInterface> c) -> {
    
                getAvailableExplorerMenu.run();
                getAvailableMarketsMenu.run();
            });

            getAvailableExplorerMenu.run();
            getAvailableMarketsMenu.run();

            tokensScene.focusOwnerProperty().addListener((e) -> {
                Object focusOwnerObject = tokensScene.focusOwnerProperty().get();
                if (focusOwnerObject != null && focusOwnerObject instanceof IconButton &&  ((IconButton) focusOwnerObject).getUserData() != null &&  ((IconButton) focusOwnerObject).getUserData() instanceof ErgoNetworkToken) {
                  
                    ErgoNetworkToken selectedToken = (ErgoNetworkToken) ((IconButton) focusOwnerObject).getUserData();    
                    tokensList.selectedTokenProperty().set(selectedToken);
                    removeButton.setDisable(false);
                    removeButton.setId("menuBarBtn");
                } else {

                    if (focusOwnerObject != null && focusOwnerObject instanceof Button && ((Button) focusOwnerObject).getText().equals(removeButton.getText())) {

                    } else {
                        removeButton.setDisable(true);
                        tokensList.selectedTokenProperty().set(null);
                        removeButton.setId("menuBarBtnDisabled");
                    }
                }
            });

            scrollPane.prefViewportWidthProperty().bind(tokensScene.widthProperty());
            scrollPane.prefViewportHeightProperty().bind(tokensScene.heightProperty().subtract(menuBar.heightProperty()).subtract(menuBox.heightProperty()));

            addButton.prefWidthProperty().bind(menuBox.widthProperty().divide(2));
            removeButton.prefWidthProperty().bind(menuBox.widthProperty().divide(2));

            tokensBox.prefWidthProperty().bind(scrollPane.prefViewportWidthProperty().subtract(40));
            //  bodyBox.prefHeightProperty().bind(tokensScene.heightProperty() - 40 - 100);
            tokensScene.getStylesheets().add("/css/startWindow.css");
            m_tokensStage.setScene(tokensScene);

            Rectangle rect = getNetworksData().getMaximumWindowBounds();

            addButton.setOnAction(actionEvent -> {
                Stage addEditTokenStage =  new Stage();
                addEditTokenStage.getIcons().add(getIcon());
                addEditTokenStage.initStyle(StageStyle.UNDECORATED);
                Button stageCloseBtn = new Button();

                Scene addTokenScene = tokensList.getEditTokenScene(null, m_networkType, addEditTokenStage, stageCloseBtn);

                addEditTokenStage.setScene(addTokenScene);
                addEditTokenStage.show();
                stageCloseBtn.setOnAction(e->{
                    addEditTokenStage.close();
                });
            });

            ResizeHelper.addResizeListener(m_tokensStage, 300, 400, rect.width, rect.height);
            m_tokensStage.setOnCloseRequest(windowEvent -> {
               closeBtn.fire();
            });
            m_tokensStage.show();
        } else {
            if (m_tokensStage.isIconified()) {
                m_tokensStage.setIconified(false);
            }
            m_tokensStage.show();
        }

    }




    public File getTestnetDataFile() {
        return m_testnetDataFile;
    }

    public ErgoTokensList getTokensList(NetworkType networkType){
      
        return new ErgoTokensList(getAppKey(), networkType, this, m_marketId, m_explorerId);

    }

    public ErgoTokensList getTokensList(NetworkType networkType, String marketId, String explorerId){
      
        return new ErgoTokensList(getAppKey(), networkType, this, marketId, explorerId);

    }

    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
      
        return false;

    }


    public static Image getAppIcon() {
        return new Image("/assets/diamond-150.png");
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/diamond-30.png");
    }

    public String getFile(NetworkType networkType) {
  
        return networkType == NetworkType.MAINNET ? getIdDataFile(NAME).getAbsolutePath() : m_testnetDataFile.getAbsolutePath();

    }

    public File getAppDir() {
        return m_appDir;
    }

    public void setAppDir(String string) {
        m_appDir = new File(string);

        getLastUpdated().set(LocalDateTime.now());
    }



    @Override
    public JsonObject getJsonObject() {
        JsonObject json = super.getJsonObject();
        json.addProperty("appDir", m_appDir.getAbsolutePath());
        json.addProperty("networkType", m_networkType.toString());
   
        if(m_explorerId != null){
            json.addProperty("explorerId", m_explorerId);
        }
        json.addProperty("marketId",m_marketId == null ? "NULL" : m_marketId);
        return json;
    }

    public void setupTokens(SecretKey appKey, File tokensDir) {
        if (!m_appDir.isDirectory()) {

            try {
                Files.createDirectories(m_appDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }
        }
        boolean createdtokensDirectory = false;
        if (!tokensDir.isDirectory()) {
            try {
                Files.createDirectories(tokensDir.toPath());
                createdtokensDirectory = true;

            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }
        }
        
        ErgoTokensList tokensList = new ErgoTokensList(appKey, m_networkType, this, m_marketId, m_explorerId);
     
        if (tokensDir.isDirectory() && createdtokensDirectory) {
            //ArrayList<ErgoNetworkToken> ergoTokenList = new ArrayList<ErgoNetworkToken>();

            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("assets/ergoTokenIcons.zip");

            ZipInputStream zipStream = null;
         
            try {

                zipStream = new ZipInputStream(is);
                final Blake2b digest = Blake2b.Digest.newInstance(32);
            
                String tokensPathString = tokensDir.getAbsolutePath();

                if (zipStream != null) {
                    // Enumeration<? extends ZipEntry> entries = zipFile.entries();
                  
                    ZipEntry entry;
                    while ((entry = zipStream.getNextEntry()) != null) {

                        String entryName = entry.getName();

                        int indexOfDir = entryName.lastIndexOf("/");

                        if (indexOfDir != entryName.length() - 1) {

                            int indexOfExt = entryName.lastIndexOf(".");

                            String fileName = entryName.substring(0, indexOfExt);

                            File newDirFile = new File(tokensPathString + "/" + fileName);
                            if (!newDirFile.isDirectory()) {
                                Files.createDirectory(newDirFile.toPath());
                            }
                            String fileString = tokensPathString + "/" + fileName + "/" + entryName;
                            File entryFile = new File(fileString);
                            OutputStream outStream = null;
                            try {
                                
                              
                                outStream = new FileOutputStream(entryFile);
                                //outStream.write(buffer);

                                byte[] buffer = new byte[8 * 1024];
                                int bytesRead;
                                while ((bytesRead = zipStream.read(buffer)) != -1) {

                                    outStream.write(buffer, 0, bytesRead);
                                    digest.update(buffer, 0, bytesRead);
                                }
                                byte[] hashbytes = digest.digest();

                                HashData hashData = new HashData(hashbytes);
                        
                                
                          //      ErgoNetworkToken token = createToken();
                            
                             //   if (token != null) {

                                    tokensList.addToken(fileName, fileString, hashData);
                               // }

                            } catch (IOException ex) {
                                try {
                                    Files.writeString(logFile.toPath(), "\nErgoTokens:" + ex.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                } catch (IOException e1) {

                                }
                            } finally {
                                if (outStream != null) {
                                    outStream.close();
                                }
                            }
                        }

                    }
                }
            } catch (IOException e) {
                try {
                    Files.writeString(logFile.toPath(), "\nErgoTokens:" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
            } finally {
                if (zipStream != null) {

                    try {
                        zipStream.close();
                    } catch (IOException e2) {
                        try {
                            Files.writeString(logFile.toPath(), "\nErgoTokens: " + e2.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e1) {

                        }
                    }

                }
            }


            save(appKey, tokensList.getJsonObject(), m_networkType);
        }

    }
   

    public void save(SecretKey appKey, JsonObject listJson, NetworkType networkType) {

        

        String tokenString = listJson.toString();

        if (networkType == NetworkType.TESTNET) {
            if (m_testnetDataFile != null) {
                try {
                    Files.writeString(m_testnetDataFile.toPath(), tokenString);
                } catch (IOException e) {
                    try {
                        Files.writeString(logFile.toPath(), "\nErgoTokens:" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e1) {

                    }
                }
            }
        } else {
            try {

                SecureRandom secureRandom = SecureRandom.getInstanceStrong();
                byte[] iV = new byte[12];
                secureRandom.nextBytes(iV);

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);

                cipher.init(Cipher.ENCRYPT_MODE, appKey, parameterSpec);

                byte[] encryptedData = cipher.doFinal(tokenString.getBytes());

                try {
                    File tokenListFile = getIdDataFile(NAME);

                    if (tokenListFile.exists()) {
                        Files.delete(tokenListFile.toPath());
                    }

                    FileOutputStream outputStream = new FileOutputStream(tokenListFile);
                    FileChannel fc = outputStream.getChannel();

                    ByteBuffer byteBuffer = ByteBuffer.wrap(iV);

                    fc.write(byteBuffer);

                    int written = 0;
                    int bufferLength = 1024 * 8;

                    while (written < encryptedData.length) {

                        if (written + bufferLength > encryptedData.length) {
                            byteBuffer = ByteBuffer.wrap(encryptedData, written, encryptedData.length - written);
                        } else {
                            byteBuffer = ByteBuffer.wrap(encryptedData, written, bufferLength);
                        }

                        written += fc.write(byteBuffer);
                    }

                    outputStream.close();

                } catch (IOException e) {
                    try {
                        Files.writeString(logFile.toPath(), "\nErgoTokens: IO exception:" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e1) {

                    }
                }

            } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                try {
                    Files.writeString(logFile.toPath(), "\nErgoTokens: Key error: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
            }
        }
    }
    
    

    @Override
    public IconButton getButton(String iconStyle) {

        IconButton iconButton = new IconButton(iconStyle.equals(IconStyle.ROW) ? getSmallAppIcon() : getAppIcon(), iconStyle.equals(IconStyle.ROW) ? getName() : getText(), iconStyle) {
            @Override
            public void open() {
                getOpen();
            }
        };

        if (iconStyle.equals(IconStyle.ROW)) {
            iconButton.setContentDisplay(ContentDisplay.LEFT);
            iconButton.setImageWidth(30);
        } else {
            iconButton.setContentDisplay(ContentDisplay.TOP);
            iconButton.setTextAlignment(TextAlignment.CENTER);
        }

        return iconButton;
    }

}
