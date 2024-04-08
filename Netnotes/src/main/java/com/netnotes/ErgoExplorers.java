package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.beans.property.SimpleDoubleProperty;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class ErgoExplorers extends Network implements NoteInterface {

    public final static String DESCRIPTION = "Ergo Explorer allows you to explore and search the Ergo blockchain.";
    public final static String SUMMARY = "Installing the Ergo Explorer allows balance and transaction information to be looked up for wallet addresses.";
    public final static String NAME = "Ergo Explorer";
    public final static String NETWORK_ID = "ERGO_EXPLORER";

    public final static String MAINNET_EXPLORER_URL = "explorer.ergoplatform.com/";
    public final static String TESTNET_EXPLORER_URL = "testnet.ergoplatform.com/";

    private static File logFile = new File("netnotes-log.txt");
    private Stage m_stage = null;
    
  //  private final static long EXECUTION_TIME = 500;

  //  private ScheduledFuture<?> m_lastExecution = null;

    private File m_appDir = null;
    private File m_dataFile = null;


    public ErgoExplorers(ErgoNetwork ergoNetwork) {
        super(getAppIcon(), NAME, NETWORK_ID, ergoNetwork);

        m_appDir = new File(ergoNetwork.getAppDir().getAbsolutePath() + "/" + NAME);
        setup(null);
        
    }

    public ErgoExplorers(JsonObject jsonObject, ErgoNetwork ergoNetwork) {
        super(getAppIcon(), NAME, NETWORK_ID, ergoNetwork);

        m_appDir = new File(ergoNetwork.getAppDir().getAbsolutePath() + "/" + NAME);
        setDataDir(new File(m_appDir.getAbsolutePath() + "/data"));
        setup(jsonObject);
    }

    @Override
    public void open(){
        showStage();
    }
    
    public void getIdJson(String id, String urlString, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        File dataFile = getIdDataFile(id);
        if(dataFile != null && dataFile.isFile()){
            try {
                JsonObject json = Utils.readJsonFile(getAppKey(), dataFile);

                Utils.returnObject(json, getNetworksData().getExecService(), onSucceeded, onFailed);
            } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                    | IOException e) {
                try {
                    Files.writeString(logFile.toPath(), "ErgoExplorers (getIdJson): " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
                    
                }

            }
        }else{
            Utils.getUrlJson(urlString, getNetworksData().getExecService(), (urlJson)->{
                Object sourceObject = urlJson.getSource().getValue();
                if(sourceObject != null && sourceObject instanceof JsonObject){
                    try {
                        JsonObject json = (JsonObject) sourceObject;
                        Utils.saveJson(getAppKey(), json, dataFile );
                        Utils.returnObject(sourceObject,getNetworksData().getExecService(), onSucceeded, onFailed);
                    } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                            | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                            | IOException e) {
                        try {
                            Files.writeString(logFile.toPath(),"ErgoExplorers (getidjson urljson): " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e1) {
         
                        }
                    }
                }else{
                    Utils.returnObject(null,getNetworksData().getExecService(), onSucceeded, onFailed);
                }
            }, onFailed, null);
        }
        
    }

     private void setup(JsonObject json) {
        if (!m_appDir.isDirectory()) {

            try {
                Files.createDirectories(m_appDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.ERROR, e.toString(), ButtonType.CLOSE);
                a.show();
            }
        }  
        m_dataFile = new File(m_appDir.getAbsolutePath() + "/" + NAME + ".dat");
        setDataDir(new File(m_appDir.getAbsolutePath() + "/data"));
        
        
        new ErgoExplorerList(this);
     
        
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

    public File getDataFile(){
        return m_dataFile;
    }
    public File getAppDir(){
        return m_appDir;
    }

    public static JsonObject getBalanceNote(String address, NetworkType... networkType) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("subject", "GET_BALANCE");
        jsonObject.addProperty("address", address);
        if (networkType != null && networkType.length > 0) {
            jsonObject.addProperty("networkType", networkType[0].toString());
        } else {
            jsonObject.addProperty("networkType", NetworkType.MAINNET.toString());
        }
        return jsonObject;
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/ergo-explorer-30.png");
    }

    public static Image getAppIcon() {
        return App.ergoExplorerImg;
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
    

    public void showStage() {
        
        if(m_stage == null){
            String title = getName();

            ErgoExplorerList explorersList = new ErgoExplorerList(this);

            double buttonHeight = 100;

            m_stage = new Stage();
            m_stage.getIcons().add(getIcon());
            m_stage.setResizable(false);
            m_stage.initStyle(StageStyle.UNDECORATED);
            m_stage.setTitle(title);

            Button closeBtn = new Button();

            Button maximizeButton = new Button();

            HBox titleBox = App.createTopBar(getSmallAppIcon(), maximizeButton, closeBtn, m_stage);
            Region menuSpacer = new Region();
            HBox.setHgrow(menuSpacer, Priority.ALWAYS);

            HBox menuBar = new HBox(menuSpacer);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 5));

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setId("bodyBox");

            HBox menuBarPadding = new HBox(menuBar);
            menuBarPadding.setPadding(new Insets(0, 2, 5, 2));
            menuBarPadding.setId("bodyBox");

            Button addButton = new Button("Add");
            addButton.setId("menuBarBtn");
            addButton.setPadding(new Insets(2, 6, 2, 6));
            addButton.setPrefWidth(getStageWidth() / 2);
            addButton.setPrefHeight(buttonHeight);

            Button removeButton = new Button("Remove");
            removeButton.setId("menuBarBtnDisabled");
            removeButton.setPadding(new Insets(2, 6, 2, 6));

            removeButton.setDisable(true);
            removeButton.setPrefWidth(getStageWidth() / 2);
            removeButton.setPrefHeight(buttonHeight);

            HBox menuBox = new HBox(addButton, removeButton);
            menuBox.setId("blackMenu");
            menuBox.setAlignment(Pos.CENTER_LEFT);
            menuBox.setPadding(new Insets(5, 5, 5, 5));
            menuBox.setPrefHeight(buttonHeight);

            VBox layoutBox = new VBox(titleBox, menuBar, scrollPane, menuBox);

            Scene mainScene = new Scene(layoutBox, getStageWidth(), getStageHeight());
            mainScene.getStylesheets().add("/css/startWindow.css");
            m_stage.setScene(mainScene);


            scrollPane.prefViewportWidthProperty().bind(mainScene.widthProperty());
            scrollPane.prefViewportHeightProperty().bind(mainScene.heightProperty().subtract(140));
            scrollPane.setPadding(new Insets(5, 5, 5, 5));
    
            SimpleDoubleProperty gridWidth = new SimpleDoubleProperty(m_stage.getWidth());
            SimpleDoubleProperty scrollWidth = new SimpleDoubleProperty(0);
            gridWidth.bind(m_stage.widthProperty().subtract(15));

            VBox gridBox = explorersList.getGridBox(gridWidth, scrollWidth);

            scrollPane.setContent(gridBox);

            ResizeHelper.addResizeListener(m_stage, 300, 300, Double.MAX_VALUE, Double.MAX_VALUE);

            m_stage.setOnCloseRequest(e -> {
              
                m_stage = null;
            });

            closeBtn.setOnAction(closeEvent -> {
            
                m_stage.close();
                m_stage = null;
            });
            shutdownNowProperty().addListener((obs, oldVal, newVal) -> {
             
                m_stage.close();
                m_stage = null;
            });

            gridBox.heightProperty().addListener((obs, oldVal, newVal) -> {
                double val = newVal.doubleValue();
                if (val > scrollPane.prefViewportHeightProperty().doubleValue()) {
                    scrollWidth.set(40);
                } else {
                    scrollWidth.set(0);
                }
            });

            addButton.prefWidthProperty().bind(m_stage.widthProperty().divide(2));
            removeButton.prefWidthProperty().bind(m_stage.widthProperty().divide(2));
            m_stage.show();
        } else {
            if (m_stage.isIconified()) {
                m_stage.setIconified(false);
            }
            m_stage.show();
   
        }

    }

    public ErgoExplorerList getErgoExplorersList(){
        return new ErgoExplorerList(this);
    }
}
