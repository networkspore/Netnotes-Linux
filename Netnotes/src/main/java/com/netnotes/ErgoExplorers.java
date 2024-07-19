package com.netnotes;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;

public class ErgoExplorers extends Network implements NoteInterface {

    public final static String DESCRIPTION = "Ergo Explorer allows you to explore and search the Ergo blockchain.";
    public final static String SUMMARY = "Installing the Ergo Explorer allows balance and transaction information to be looked up for wallet addresses.";
    public final static String NAME = "Ergo Explorer";
    public final static String NETWORK_ID = "ERGO_EXPLORER";

    public final static String MAINNET_EXPLORER_URL = "explorer.ergoplatform.com/";
    public final static String TESTNET_EXPLORER_URL = "testnet.ergoplatform.com/";

    public final static String DEFAULT_EXPLORER_ID = ErgoPlatformExplorerData.ERGO_PLATFORM_EXPLORER;

    private ErgoNetworkData m_ergNetData = null;
    private ErgoExplorerList m_explorerList = null;

    public String getType(){
        return App.EXPLORER_TYPE;
    }
    private ErgoNetwork m_ergoNetwork;

    public ErgoExplorers(ErgoNetworkData ergoNetworkData, ErgoNetwork ergoNetwork) {
        super(new Image(getAppIconString()), NAME, NETWORK_ID, ergoNetwork);
        m_ergoNetwork = ergoNetwork;
        m_ergNetData = ergoNetworkData;
        
        
    }

    

    public String getDescription(){
        return DESCRIPTION;
    }



    public static String getAppIconString(){
        return "/assets/ergo-explorer.png";
    }




    public static String getSmallAppIconString(){
        return "/assets/ergo-explorer-30.png";
    }

    private Image m_smallAppIcon = new Image(getSmallAppIconString());
    public Image getSmallAppIcon() {
        return m_smallAppIcon;
    }

    @Override
    public Object sendNote(JsonObject note){

        JsonElement subjectElement = note.get("subject");
        JsonElement networkIdElement = note.get("networkId");
        if (m_explorerList != null && subjectElement != null && subjectElement.isJsonPrimitive() && networkIdElement != null && networkIdElement.isJsonPrimitive()) {
            String networkId = networkIdElement.getAsString();
        
            if(networkId.equals(m_ergNetData.getId())){
               
                String subject = subjectElement.getAsString();
                switch(subject){
                    case "getExplorerById":
                        return m_explorerList.getExplorerById(note);
                        
                }
            }
        }

        return null;
    }




    /*public ErgoExplorerList getErgoExplorersList(){
        return m_explorerList;
    }*/

    @Override
    public void start(){
        if(getConnectionStatus() != App.STARTED){
            m_explorerList = new ErgoExplorerList(this, m_ergNetData);
        }
        super.start();
    }


    public void getData(String subId, String id, String urlString, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        JsonObject existingData = m_ergoNetwork.getNetworksData().getData(subId, id, ErgoExplorers.NETWORK_ID, ErgoExplorers.NETWORK_ID);

        if(existingData != null){

            Utils.returnObject(existingData, getNetworksData().getExecService(), onSucceeded, onFailed);

        }else{
            Utils.getUrlJson(urlString, getNetworksData().getExecService(), (urlJson)->{
                Object sourceObject = urlJson.getSource().getValue();
                if(sourceObject != null && sourceObject instanceof JsonObject){
                    
                        JsonObject json = (JsonObject) sourceObject;
                        getNetworksData().save(subId, id, ErgoExplorers.NETWORK_ID, ErgoExplorers.NETWORK_ID, json);
                        Utils.returnObject(sourceObject,getNetworksData().getExecService(), onSucceeded, onFailed);
                 
                }else{
                    Utils.returnObject(null,getNetworksData().getExecService(), onSucceeded, onFailed);
                }
            }, onFailed, null);
        }
        
    }


    @Override
    public void stop(){
        if(m_explorerList != null){
            m_explorerList = null;
        }
        super.stop();
    }

    public NetworkInformation getNetworkInformation(){
        return new NetworkInformation(NETWORK_ID, NAME,getAppIconString(), getSmallAppIconString(),DESCRIPTION );
    }
}


    /*

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

            VBox gridBox = new VBox();

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

    }*/