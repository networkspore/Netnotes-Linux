package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.utils.Utils;

import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;

import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class ErgoNetwork extends Network implements NoteInterface {

    public final static String NAME = "Ergo Network";
    public final static String DESCRIPTION = "A layer 0, smart contract enabled P2P blockchain network.";
    public final static String SUMMARY = "";
    public final static String NETWORK_ID = "ERGO_NETWORK";


  
    @Override
    public String getType(){
        return App.NETWORK_TYPE;
    }

    private File m_appDir = null;

    private NetworkType m_networkType = NetworkType.MAINNET;

   // private File logFile = new File("netnotes-log.txt");
    private ErgoNetworkData m_ergNetData = null;

    
  //  private Image m_balanceImage = new Image("/assets/balance-list-30.png");
 //   private Image m_txImage = new Image("/assets/transaction-list-30.png");
//    private Image m_sendImage = new Image("/assets/arrow-send-white-30.png");

    
    private File m_networksDir;

    //private SimpleBooleanProperty m_shuttingdown = new SimpleBooleanProperty(false);
    public ErgoNetwork(NetworksData networksData) {
        super(new Image("/assets/ergo-network-30.png"), NAME, NETWORK_ID, networksData);
        m_networksDir = new File (getNetworksData().getAppDir().getAbsolutePath() +"/networks");
        if(!m_networksDir.isDirectory()){
            try {
                Files.createDirectory(m_networksDir.toPath());
            } catch (IOException e) {
                try {
                    Files.writeString(App.logFile.toPath(), "Cannot create network directory: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
                    
                }
            }
        }
        m_appDir = new File( m_networksDir.getAbsolutePath() + "/Ergo");

        if(!m_appDir.isDirectory()){
            try {
                Files.createDirectory(m_appDir.toPath());
            } catch (IOException e) {
                try {
                    Files.writeString(App.logFile.toPath(), "Cannot create ergo directory: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
                    
                }
            }
        }

        setKeyWords(new String[]{"blockchain","smart contracts", "programmable", "dApp", "wallet"});

        m_ergNetData = new ErgoNetworkData(this);

    }
  

    @Override
    public String getDescription(){
        return DESCRIPTION;
    }

    public File getAppDir(){
        if(!m_appDir.isDirectory()){
            
            try {
                m_appDir.mkdirs();
                
            } catch (SecurityException e) {
                try {
                    Files.writeString(App.logFile.toPath(), "Cannot create ergo directory: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
                    
                }
            }
            
        }

        return m_appDir;
    }

    public JsonObject getAppDir(JsonObject note){
        JsonObject json = new JsonObject();
        json.addProperty("appDir", getAppDir().getAbsolutePath());
        return json;    
    }

    private Image m_smallAppIcon = new Image(getSmallAppIconString());

    public Image getSmallAppIcon() {
        return m_smallAppIcon;
    }

    public static String getAppIconString(){
        return "/assets/ergo-network.png";
    }

    public static String getSmallAppIconString(){
        return "/assets/ergo-network-30.png";
    }
    public JsonArray getKeyWordsArray(){
        JsonArray keywordsArray = new JsonArray();
        String[] keywords =getKeyWords();
        for(String word : keywords){
            keywordsArray.add(new JsonPrimitive(word));
        }
        return keywordsArray;
    }

    @Override
    public JsonObject getJsonObject() {

        JsonObject networkObj = super.getJsonObject();
        networkObj.addProperty("networkType", m_networkType.toString());
        networkObj.add("keyWords", getKeyWordsArray());
        

        return networkObj;

    }

    public NetworkType getNetworkType(){
        return m_networkType;
    }


    @Override
    protected void start(){
        if(getConnectionStatus() == App.STOPPED){
            super.start();
            
        }
        sendStatus();
    }

    @Override
    protected void stop(){
        super.stop();


        sendStatus();        
    }

    private void sendStatus(){
        long timeStamp = System.currentTimeMillis();

        JsonObject json = Utils.getJsonObject("code", App.STATUS);
        json.addProperty("networkId", ErgoNetwork.NETWORK_ID);
        json.addProperty("timeStamp", timeStamp);
        json.addProperty("statusCode", getConnectionStatus());

        sendMessage(App.STATUS, timeStamp, ErgoNetwork.NETWORK_ID, json.toString());
    }

   

    private ArrayList<String> m_authorizedLocations = new ArrayList<>();

    private boolean isLocationAuthorized(String locationString){
        
        return locationString.equals(App.LOCAL) || m_authorizedLocations.contains(locationString);
    }




    public void shutdown() {
        m_ergNetData.shutdown();
    }

    @Override
    public Object sendNote(JsonObject note){
        JsonElement cmdElement = note.get(App.CMD);
        JsonElement networkIdElement = note.get("networkId");
        JsonElement locationIdElement = note.get("locationId");

    
        if (cmdElement != null  && networkIdElement != null && networkIdElement != null && networkIdElement.isJsonPrimitive() && locationIdElement != null && locationIdElement.isJsonPrimitive()) {
            String locationId = locationIdElement.getAsString();
            String locationString = getNetworksData().getLocationString(locationId);
            if(isLocationAuthorized(locationString)){
                
                note.remove("locationString");
                note.addProperty("locationString", locationString);

                String networkId = networkIdElement.getAsString();

                switch(networkId){
                    case App.WALLET_NETWORK:
                        return m_ergNetData.getErgoWallets().sendNote(note);
                    case App.EXPLORER_NETWORK:
                        return m_ergNetData.getErgoExplorers().sendNote(note);
                    case App.NODE_NETWORK:
                        return m_ergNetData.getErgoNodes().sendNote(note);
                    case App.MARKET_NETWORK:
                        return m_ergNetData.getErgoMarkets().sendNote(note);
                    case App.TOKEN_NETWORK:
                        return m_ergNetData.getErgoTokens().sendNote(note);
               
                }

            }
            
        }
       
        return null;
    }

    
    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        JsonElement cmdElement = note.get(App.CMD);
        JsonElement networkIdElement = note.get("networkId");
        JsonElement locationIdElement = note.get("locationId");

    
        if (cmdElement != null  && networkIdElement != null && networkIdElement != null && networkIdElement.isJsonPrimitive() && locationIdElement != null && locationIdElement.isJsonPrimitive()) {
            String locationId = locationIdElement.getAsString();
            String locationString = getNetworksData().getLocationString(locationId);
            if(isLocationAuthorized(locationString)){
                
                note.remove("locationString");
                note.addProperty("locationString", locationString);

                String networkId = networkIdElement.getAsString();

                switch(networkId){
                    case App.WALLET_NETWORK:
                        return m_ergNetData.getErgoWallets().sendNote(note, onSucceeded, onFailed);
                    case App.EXPLORER_NETWORK:
                        return m_ergNetData.getErgoExplorers().sendNote(note, onSucceeded, onFailed);
                    case App.NODE_NETWORK:
                        return m_ergNetData.getErgoNodes().sendNote(note, onSucceeded, onFailed);
                    case App.MARKET_NETWORK:
                        return m_ergNetData.getErgoMarkets().sendNote(note, onSucceeded, onFailed);
                    case App.TOKEN_NETWORK:
                        return m_ergNetData.getErgoTokens().sendNote(note, onSucceeded, onFailed);
                }

            }
            
        }
       

        return false;
    }


    public static NetworkInformation getNetworkInformation(){
        return new NetworkInformation(NETWORK_ID, NAME, getAppIconString(), getSmallAppIconString(), DESCRIPTION);
    }


    public final String ADDRESS_LOCKED =  "[ Locked ]";

    private TabInterface m_ergoNetworkTab = null;

    @Override
    public TabInterface getTab(Stage appStage, String locationId,  SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject, Button networkBtn){
        m_ergoNetworkTab = new ErgoNetworkTab(appStage, locationId, heightObject, widthObject, networkBtn);
        return m_ergoNetworkTab;
    }

    private class ErgoNetworkTab extends AppBox implements TabInterface{
        

        private ScrollPane m_walletScroll;
        private ChangeListener<Bounds> m_boundsChange;

        private ErgoWalletsAppBox m_ergoWalletsAppBox = null;
        private ErgoExplorersAppBox m_ergoExplorerAppBox = null;
        private ErgoNodesAppBox m_ergoNodesAppBox = null;

        private NoteMsgInterface m_ergoNetworkMsgInterface = null;
        
        private SimpleBooleanProperty m_current = new SimpleBooleanProperty(false);
        private Button m_menuBtn;

        private SimpleStringProperty m_titleProperty = new SimpleStringProperty(getName());
        public String getName(){
            return ErgoNetwork.this.getName();
        }
        public SimpleStringProperty titleProperty(){
            return m_titleProperty;
        }

        

        public ErgoNetworkTab(Stage appStage, String locationId, SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject, Button networkBtn){
            super(NETWORK_ID);
            m_menuBtn = networkBtn;
     
            setPrefWidth(App.DEFAULT_STATIC_WIDTH);
            setMaxWidth(App.DEFAULT_STATIC_WIDTH);
          
            prefHeightProperty().bind(heightObject);


            m_ergoWalletsAppBox = new ErgoWalletsAppBox(appStage, locationId, getNoteInterface());

            m_ergoExplorerAppBox = new ErgoExplorersAppBox(appStage, locationId, getNoteInterface());

            m_ergoNodesAppBox = new ErgoNodesAppBox(appStage,  locationId, getNoteInterface());
              
            m_ergoNetworkMsgInterface = new NoteMsgInterface() {
                public String getId(){
                    return m_ergNetData.getId();
                }
                public void sendMessage(int code, long timestamp, String networkId, Number num){
                }

                public void sendMessage(int code, long timestamp, String networkId, String msg){
                   
                    m_ergoWalletsAppBox.sendMessage(code, timestamp,networkId, msg);

                    m_ergoNodesAppBox.sendMessage(code, timestamp, networkId, msg);
                    
                    m_ergoExplorerAppBox.sendMessage(code, timestamp, networkId, msg);
                    
                }
            };
            

       
            addMsgListener(m_ergoNetworkMsgInterface);

            Region hBar = new Region();
            hBar.setPrefWidth(400);
            hBar.setPrefHeight(2);
            hBar.setMinHeight(2);
            hBar.setId("hGradient");

            HBox gBox = new HBox(hBar);
            gBox.setAlignment(Pos.CENTER);
            gBox.setPadding(new Insets(5, 0, 5, 0));

            Region hBar1 = new Region();
            hBar1.setPrefWidth(400);
            hBar1.setPrefHeight(2);
            hBar1.setMinHeight(2);
            hBar1.setId("hGradient");

            HBox gBox1 = new HBox(hBar1);
            gBox1.setAlignment(Pos.CENTER);
            gBox1.setPadding(new Insets(5, 0, 5, 0));

            Region hBar2 = new Region();
            hBar2.setPrefWidth(400);
            hBar2.setPrefHeight(2);
            hBar2.setMinHeight(2);
            hBar2.setId("hGradient");

            HBox gBox2 = new HBox(hBar2);
            gBox2.setAlignment(Pos.CENTER);
            gBox2.setPadding(new Insets(5, 0, 5, 0));

            Region vBar = new Region();
            vBar.setPrefWidth(1);
            vBar.setMinWidth(1);
            vBar.setId("vGradient");
            VBox.setVgrow(vBar,Priority.ALWAYS);


            Region appBoxSpacer = new Region();
            VBox.setVgrow(appBoxSpacer, Priority.ALWAYS);
        

            getChildren().addAll(m_ergoWalletsAppBox, appBoxSpacer,gBox, m_ergoNodesAppBox, gBox1,  m_ergoExplorerAppBox);
      
         
        }
        
        @Override
        public void shutdown(){
            
            if(m_ergoNetworkMsgInterface != null){
                removeMsgListener(m_ergoNetworkMsgInterface);
                m_ergoNetworkMsgInterface = null;
            }

            if(m_ergoWalletsAppBox != null){
                getChildren().remove(m_ergoWalletsAppBox);
                m_ergoWalletsAppBox.shutdown();
                m_ergoWalletsAppBox = null;
                
            }
            
            /*
                shutdownMenu.fire();
                AddressesData addressesData = m_addressesDataObject.get();
                if(addressesData != null){
                    addressesData.shutdown();
                }
            
                m_addressesDataObject.removeListener(addressesDataObjChangeListener);
             */
            if(m_boundsChange != null && m_walletScroll != null){
                m_walletScroll.layoutBoundsProperty().removeListener(m_boundsChange);
                m_boundsChange = null;
            }
            m_ergoNetworkTab = null;
        }

    
        public void setCurrent(boolean value){
            m_menuBtn.setId(value ? "activeMenuBtn" : "menuTabBtn");
            m_current.set(value);
        }
    
        
        public boolean getCurrent(){
            return m_current.get();
        } 
    

    }


}
