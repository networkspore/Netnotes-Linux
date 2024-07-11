package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import javafx.scene.image.Image;

public class ErgoNodes extends Network implements NoteInterface {

  //  private File logFile = new File("netnotes-log.txt");

    public final static String NAME = "Ergo Nodes";
    public final static String DESCRIPTION = "Ergo Nodes allows you to configure your access to the Ergo blockchain";
    public final static String NETWORK_ID = "ERGO_NODES";
    public final static String SUMMARY = "";

    public final static int MAINNET_PORT = 9053;
    public final static int TESTNET_PORT = 9052;
    public final static int EXTERNAL_PORT = 9030;

    private File m_appDir = null;

    private ErgoNodesList m_ergoNodesList = null;
    private ErgoNetworkData m_ergNetData;

    public ErgoNodes(ErgoNetworkData ergoNetworkData, ErgoNetwork ergoNetwork) {
        super(new Image(getAppIconString()), NAME, NETWORK_ID, ergoNetwork);
        m_appDir = new File(ergoNetwork.getAppDir() + "/nodes");
        m_ergNetData = ergoNetworkData;
       
   
        setStageWidth(750);
        if (!m_appDir.isDirectory()) {

            try {
                Files.createDirectories(m_appDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }

            
        
        }
      
    }

    public String getDescription(){
        return DESCRIPTION;
    }
    
   
    public static String getAppIconString(){
        return "/assets/ergoNodes-100.png";
    }

    public static String getSmallAppIconString(){
        return "/assets/ergoNodes-30.png";
    }

    private Image m_smallAppIcon = new Image(getSmallAppIconString());

    public Image getSmallAppIcon() {
        return m_smallAppIcon;
    }



    public File getAppDir() {
        return m_appDir;
    }

    public String getType(){
        return App.NODE_TYPE;
    }

    @Override
    public Object sendNote(JsonObject note){
        if(note != null && m_ergoNodesList != null){
            JsonElement subjecElement = note.get("subject");
            JsonElement networkIdElement = note.get("networkId");

            if( subjecElement != null && subjecElement.isJsonPrimitive() && networkIdElement != null && networkIdElement.isJsonPrimitive()){
                String networkId = networkIdElement.getAsString();
            
                if(networkId.equals(m_ergNetData.getId())){
                    switch (subjecElement.getAsString()) {
                        case "getNodes":
                            
                        case "getNodeById":
                                
                        case "openNode":
                            
                    }
                }
                
            }
        }
        return null;
    }
  

    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

       /* JsonElement subjecElement = note.get("subject");
        JsonElement networkTypeElement = note.get("networkType");
        JsonElement nodeIdElement = note.get("nodeId"); */

        return false;
    }

  


 /*

    private Stage m_stage = null;

   
    public void showStage() {
        if (m_stage == null) {
            String title = getName();

            double buttonHeight = 70;

      

            m_stage = new Stage();
            m_stage.getIcons().add(getIcon());
            m_stage.setResizable(false);
            m_stage.initStyle(StageStyle.UNDECORATED);
            m_stage.setTitle(title);

            Button closeBtn = new Button();

            Button maximizeBtn = new Button();

            HBox titleBox = App.createTopBar(getSmallAppIcon(), maximizeBtn, closeBtn, m_stage);

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setId("bodyBox");

        
            
        
            Button addBtn = new Button("Add");
            addBtn.setId("menuBarBtn");
            addBtn.setPadding(new Insets(2, 6, 2, 6));
            addBtn.setPrefWidth(getStageWidth() / 2);
            addBtn.setPrefHeight(buttonHeight);
            addBtn.setOnAction(e->{
                m_ergoNodesList.showAddNodeStage();
            });


            Button removeBtn = new Button("Remove");
            removeBtn.setId("menuBarBtnDisabled");
            removeBtn.setPadding(new Insets(2, 6, 2, 6));
            removeBtn.setDisable(true);
            removeBtn.setPrefWidth(getStageWidth() / 2);
            removeBtn.setPrefHeight(buttonHeight);

            removeBtn.setOnAction(e->{
                String selectedId = m_ergoNodesList.selectedIdProperty().get();
                ErgoNodeData ergoNodeData = m_ergoNodesList.getErgoNodeData(selectedId);
                ergoNodeData.remove();
            });

            HBox menuBox = new HBox(addBtn, removeBtn);
            menuBox.setId("blackMenu");
            menuBox.setAlignment(Pos.CENTER_LEFT);
            menuBox.setPadding(new Insets(5, 5, 5, 5));
            menuBox.setPrefHeight(buttonHeight);
            menuBox.setMinHeight(buttonHeight);

            m_ergoNodesList.selectedIdProperty().addListener((obs,oldval,newval)->{
                if(newval != null){
                    removeBtn.setDisable(false);
                    
                    removeBtn.setId("menuBarBtn");

                }else{
                    removeBtn.setDisable(true);
                    removeBtn.setId("menuBarBtnDisabled");
                }
            });


            VBox bodyBox = new VBox(scrollPane,menuBox);
            bodyBox.setPadding(new Insets(0,3,2,3));
            VBox layoutBox = new VBox(titleBox, bodyBox);
  
          
            Scene mainScene = new Scene(layoutBox, getStageWidth(), getStageHeight());
            mainScene.setFill(null);
            mainScene.getStylesheets().add("/css/startWindow.css");
            m_stage.setScene(mainScene);

 

            scrollPane.prefViewportWidthProperty().bind(mainScene.widthProperty().subtract(4));
            scrollPane.prefViewportHeightProperty().bind(mainScene.heightProperty().subtract(titleBox.heightProperty()).subtract(menuBox.heightProperty()));
            scrollPane.setPadding(new Insets(5, 5, 5, 5));
            scrollPane.setOnMouseClicked(e -> {
       
            });

            SimpleDoubleProperty gridWidth = new SimpleDoubleProperty(m_stage.getWidth());
            SimpleDoubleProperty scrollWidth = new SimpleDoubleProperty(0);
            gridWidth.bind(mainScene.widthProperty().subtract(20));
            m_stage.show();

            VBox gridBox = m_ergoNodesList.getGridBox(gridWidth, scrollWidth);

    

          

            ResizeHelper.addResizeListener(m_stage, 300, 300, Double.MAX_VALUE, Double.MAX_VALUE);

            scrollPane.setContent(gridBox);

            addBtn.prefWidthProperty().bind(mainScene.widthProperty().divide(2));
            removeBtn.prefWidthProperty().bind(mainScene.widthProperty().divide(2));


            m_stage.setOnCloseRequest(e -> {
                shutdownNowProperty().set(LocalDateTime.now());
            });

            closeBtn.setOnAction(closeEvent -> {
                shutdownNowProperty().set(LocalDateTime.now());
            });
            shutdownNowProperty().addListener((obs, oldVal, newVal) -> {
                m_ergoNodesList.shutdown();
                
                if(m_stage != null){
                    m_stage.close();
                    m_stage = null;
                }
            });

            Runnable updateScrollWidth = () -> {
                double val = gridBox.heightProperty().doubleValue();
                if (val > scrollPane.prefViewportHeightProperty().doubleValue()) {
                    scrollWidth.set(40);
                } else {
                    scrollWidth.set(0);
                }
            };

            gridBox.heightProperty().addListener((obs, oldVal, newVal) -> updateScrollWidth.run());

            

            updateScrollWidth.run();

            if (getStageMaximized()) {
                m_stage.setMaximized(true);
            }

        } else {
            if (m_stage.isIconified()) {
                m_stage.setIconified(false);
            }
            if(!m_stage.isShowing()){
                m_stage.show();
            }else{
                Platform.runLater(()->m_stage.toBack());
                Platform.runLater(()->m_stage.toFront());
            }
         
        }

    } */

    @Override
    public JsonObject getJsonObject() {
        JsonObject json = super.getJsonObject();
       
        return json;
    }



    @Override
    public void remove() {
        super.remove();
        if (m_ergoNodesList != null && m_ergoNodesList.getErgoLocalNode() != null) {
            m_ergoNodesList.getErgoLocalNode().stop();
        }
    }

    @Override
    protected void start(){
        if(getConnectionStatus() == App.STOPPED){
            super.start();
            if(m_ergoNodesList == null){
                m_ergoNodesList = new ErgoNodesList(this);
            }
        }
    }

    @Override
    protected void stop(){
        super.stop();
        if(m_ergoNodesList != null){
            m_ergoNodesList.shutdown();
            m_ergoNodesList = null;
        }
    }

    public NetworkInformation getNetworkInformation(){
        return new NetworkInformation(NETWORK_ID, NAME, getAppIconString(), getSmallAppIconString(), DESCRIPTION);
    }
}
