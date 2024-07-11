package com.netnotes;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;

public class ErgoTokens extends Network implements NoteInterface {

    public final static String DESCRIPTION = "Ergo Tokens allows you to manage your interactions with the tokens on the Ergo Network.";
    public final static String SUMMARY = "Mange your tokens with Ergo Tokens.";
    public final static String NAME = "Ergo Tokens";
    public final static String NETWORK_ID = "ERGO_TOKENS";

    public final static String[] TOKEN_MARKETS = new String[] { SpectrumFinance.NETWORK_ID };


    private NetworkType m_networkType = NetworkType.MAINNET;

    private ErgoNetworkData m_ergNetData;

   // private boolean m_firstOpen = false;
   

    private ErgoTokensList m_ergoTokensList = null;


    public ErgoTokens(ErgoNetworkData ergNetData, ErgoNetwork ergoNetwork) {
        super(new Image(getAppIconString()), NAME, NETWORK_ID, ergoNetwork);
        m_ergNetData = ergNetData;
        m_networkType = NetworkType.MAINNET;
        
    
        setupTokens();
        
    }


    public String getType(){
        return App.TOKEN_TYPE;
    }
    public String getDescription(){
        return DESCRIPTION;
    }




    public ErgoNetworkData getErgoNetworkData(){
        return m_ergNetData;
    }


    public NetworkType getNetworkType() {
        return m_networkType;
    }
    




   

    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        Object obj = sendNote(note);        
        
        Utils.returnObject(obj,getNetworksData().getExecService(), onSucceeded, onFailed);

        return obj != null;

    }

    @Override
    public Object sendNote(JsonObject note){
        JsonElement subjectElement = note.get("subject");
        JsonElement networkIdElement = note.get("networkId");


        if (m_ergoTokensList != null && subjectElement != null && networkIdElement != null && networkIdElement.isJsonPrimitive() && networkIdElement.getAsString().equals(m_ergNetData.getId())) {
            String subject = subjectElement.getAsString();
    
            switch (subject) {
                case "getToken":
                    return getToken(note);
                case "getAddToken":
                    return getAddToken(note);

                default:
            }
          
        }
        return null;
    }

    private JsonObject getToken(JsonObject note){

        JsonElement tokenIdElement = note.get("tokenId");
        
        if(tokenIdElement != null && tokenIdElement.isJsonPrimitive() && m_ergoTokensList != null){
            ErgoTokenData tokenData = m_ergoTokensList.getErgoToken(tokenIdElement.getAsString());
            
            if(tokenData != null){
                return tokenData.getJsonObject();
            }
        }
        
        return null;
    }
    
    private JsonObject getAddToken(JsonObject note){
        note.get("tokenId");
        note.get("name");
        note.get("decimals");

        JsonElement tokenIdElement = note.get("tokenId");
        JsonElement nameElement = note.get("name");
        JsonElement decimalsElement = note.get("decimals");
        
        if(tokenIdElement != null && tokenIdElement.isJsonPrimitive() && m_ergoTokensList != null){
            ErgoTokenData tokenData = m_ergoTokensList.getAddErgoToken(tokenIdElement.getAsString(), nameElement.getAsString(), decimalsElement.getAsInt());
            
            if(tokenData != null){
                return tokenData.getJsonObject();
            }
        }

        return null;
    }

    @Override
    public void start(){
        if(getConnectionStatus() == App.STOPPED){
            m_ergoTokensList = new ErgoTokensList(m_networkType, this);
           
            super.start();
        }
    }

    @Override
    public void stop(){
        if(getConnectionStatus() != App.STOPPED){
            super.stop();
            
            if(m_ergoTokensList != null){
                m_ergoTokensList.shutdown();
                m_ergoTokensList = null;
            }
            
        }
    }
    public static String getAppIconString(){
        return "/assets/ergoTokens-150.png";
    }

    private Image m_smallAppIcon = new Image(getSmallAppIconString());

    public Image getSmallAppIcon() {
        return m_smallAppIcon;
    }

    public static String getSmallAppIconString(){
        return "/assets/ergoTokens-30.png";
    }

    private NoteInterface getExplorer(){
        return m_ergNetData.selectedExplorerData().get();
    }





    public void setupTokens( ) {
       
        m_ergoTokensList = new ErgoTokensList(m_networkType, this);
        m_ergoTokensList.shutdown();
        m_ergoTokensList = null;
    }
   



   

    @Override
    public void shutdown(){
        super.shutdown();
        msgListeners().clear();
        stop();
    }

}


/*
 
    public void showTokensStage() {
        if (m_tokensStage == null) {

            ErgoExplorers ergoExplorers =  getErgoNetworkData().getErgoExplorers();

            NoteMsgInterface msgInterface = new NoteMsgInterface() {

                public String getId(){
                    return getNetworkId();
                }
                
                public void sendMessage(String networkId, int code, long timestamp){

                }

                public void sendMessage(int code, long timestamp){

                }

                public void sendMessage(int code, long timestamp, String msg){

                }

                public void sendMessage(String networkId, int code, long timestamp, String msg){

                }
                public void sendMessage(String networkId, int code, long timestamp, JsonObject json){
                }

            };   
            addMsgListener(msgInterface);


            double tokensStageWidth = 375;
            double tokensStageHeight = 600;
            double buttonHeight = 70;



            m_tokensStage = new Stage();
            m_tokensStage.getIcons().add(getIcon());
            m_tokensStage.initStyle(StageStyle.UNDECORATED);
            m_tokensStage.setTitle(getName() + ": Tokens " + (m_networkType == NetworkType.MAINNET ? "(MAINNET)" : "(TESTNET)"));

            Button closeBtn = new Button();
            
            Button maxBtn = new Button();

            HBox titleBox = App.createTopBar(getIcon(), maxBtn, closeBtn, m_tokensStage);

            BufferedMenuButton menuBtn = new BufferedMenuButton("/assets/menu-outline-30.png", App.MENU_BAR_IMAGE_WIDTH);
  

            MenuItem importBtn = new MenuItem(" Import JSON...");
            importBtn.setId("menuBtn");

            importBtn.setOnAction(action -> {
                if(m_ergoTokensList != null){
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle("Import JSON File...");
                    chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("text/json", "*.json"));
                    File openFile = chooser.showOpenDialog(m_tokensStage);
                    if (openFile != null) {
                        
                        m_ergoTokensList.importJson(m_tokensStage, openFile);
                    }
                }
            });

            MenuItem exportBtn = new MenuItem(" Export JSON...");
            exportBtn.setId("menuBtn");
            exportBtn.setOnAction(action -> {
                if(m_ergoTokensList != null){
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle("Export JSON file...");
                    chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("text/json", "*.json"));
                    chooser.setInitialFileName("ergoTokens-" + m_networkType);
                    File saveFile = chooser.showSaveDialog(m_tokensStage);

                    if (saveFile != null) {
                        try {
                            Files.writeString(saveFile.toPath(), m_ergoTokensList.getJsonObject().toString());
                        } catch (IOException e) {
                            Alert writeAlert = new Alert(AlertType.NONE, e.toString(), ButtonType.OK);
                            writeAlert.initOwner(m_tokensStage);
                            writeAlert.setGraphic(IconButton.getIconView(getIcon(), 75));

                        }
                    }
                }
            });

            menuBtn.getItems().addAll(importBtn, exportBtn);



            Tooltip marketsTip = new Tooltip("Token Market: (set default)");
            marketsTip.setShowDelay(new javafx.util.Duration(50));
            marketsTip.setFont(App.txtFont);
     
            BufferedMenuButton marketBtn = new BufferedMenuButton("/assets/ergo-charts-30.png", App.MENU_BAR_IMAGE_WIDTH);
            marketBtn.setPadding(new Insets(2, 0, 0, 2));
            marketBtn.setTooltip(marketsTip);

            

            Tooltip explorerTip = new Tooltip("Select explorer");
            explorerTip.setShowDelay(new javafx.util.Duration(50));
            explorerTip.setFont(App.txtFont);
     


           
         
      
    
 


            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox menuBar = new HBox(menuBtn, spacer);
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

            HBox tokensBox = new HBox();
            HBox.setHgrow(tokensBox, Priority.ALWAYS);

            
            Runnable addLoadingBox = ()->{
                VBox loadingBox = new VBox(App.createImageButton(getAppIcon(), NAME));
                HBox.setHgrow(loadingBox, Priority.ALWAYS);
                VBox.setVgrow(loadingBox, Priority.ALWAYS);
                loadingBox.setAlignment(Pos.CENTER);
                tokensBox.getChildren().clear();
                tokensBox.getChildren().add(loadingBox);
            };

            addLoadingBox.run();

            Runnable setTokensList = () ->{
               
                ErgoTokensList tList = new ErgoTokensList( m_networkType, this, m_explorerId);
                
                Utils.returnObject(tList, getNetworksData().getExecService(), (onSuceeded)->{
                    Object listObject = onSuceeded.getSource().getValue();
                    if(listObject != null && listObject instanceof ErgoTokensList){
                        ErgoTokensList tokensList = (ErgoTokensList) listObject;

                  
                    
                        tokensBox.getChildren().clear();
                        VBox vBox = tokensList.getButtonGrid();
                        HBox.setHgrow(vBox, Priority.ALWAYS);

                        tokensBox.getChildren().add(vBox);
                    }
                }, null);
            };
          
            Region growRegion = new Region();

            VBox.setVgrow(growRegion, Priority.ALWAYS);

            VBox bodyBox = new VBox(tokensBox, growRegion);

            ScrollPane scrollPane = new ScrollPane(bodyBox);

            bodyBox.prefWidthProperty().bind(Bindings.createObjectBinding(()->scrollPane.layoutBoundsProperty().get().getWidth() < 300 ? 300 :scrollPane.layoutBoundsProperty().get().getWidth() , scrollPane.layoutBoundsProperty()));
            
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
                ErgoTokensList tokensList = m_ergoTokensList;
                if(tokensList != null){
                    ErgoTokenData selectedToken = (ErgoTokenData) tokensList.selectedTokenProperty().get();
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
                }
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
         

            tokensScene.focusOwnerProperty().addListener((e) -> {
                ErgoTokensList tokensList = m_ergoTokensList;
                if(tokensList != null){
                   
                    Object focusOwnerObject = tokensScene.focusOwnerProperty().get();
                    if (focusOwnerObject != null && focusOwnerObject instanceof IconButton &&  ((IconButton) focusOwnerObject).getUserData() != null &&  ((IconButton) focusOwnerObject).getUserData() instanceof ErgoTokenData) {
                    
                        ErgoTokenData selectedToken = (ErgoTokenData) ((IconButton) focusOwnerObject).getUserData();    
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


            addButton.setOnAction(actionEvent -> {
                if(m_ergoTokensList != null){
                    ErgoTokensList tokensList = m_ergoTokensList;

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
                }
            });

            ResizeHelper.addResizeListener(m_tokensStage, 300, 400, Double.MAX_VALUE, Double.MAX_VALUE);
           
            m_tokensStage.show();

            closeBtn.setOnAction(closeEvent -> {

                if(m_tokensStage != null){
                    m_tokensStage.close();
                    m_tokensStage = null;
                }
            });

            m_tokensStage.setOnCloseRequest(windowEvent -> {
                closeBtn.fire();
             });

             
         
        } else {
            if (m_tokensStage.isIconified()) {
                m_tokensStage.setIconified(false);
            }
            m_tokensStage.show();
        }

    }
 */