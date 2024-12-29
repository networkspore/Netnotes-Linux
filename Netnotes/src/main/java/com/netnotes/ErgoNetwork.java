package com.netnotes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

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

    public static final String WALLET_NETWORK = "WALLET_NETWORK";
    public static final String NODE_NETWORK = "NODE_NETWORK";
    public static final String EXPLORER_NETWORK = "EXPLORER_NETWORK";
    public static final String MARKET_NETWORK = "MARKET_NETWORK";
    public static final String TOKEN_MARKET_NETWORK = "TOKEN_MARKET_NETWORK";

    private NetworkType m_networkType = NetworkType.MAINNET;

   // private File logFile = new File("netnotes-log.txt");
    private ErgoNetworkData m_ergNetData = null;

  //  private Image m_balanceImage = new Image("/assets/balance-list-30.png");
 //   private Image m_txImage = new Image("/assets/transaction-list-30.png");
//    private Image m_sendImage = new Image("/assets/arrow-send-white-30.png");



    //private SimpleBooleanProperty m_shuttingdown = new SimpleBooleanProperty(false);
    public ErgoNetwork(NetworksData networksData, String locationId) {
        super(new Image("/assets/ergo-network-30.png"), NAME, NETWORK_ID, networksData);
        

        setKeyWords(new String[]{"blockchain","smart contracts", "programmable", "dApp", "wallet"});

        m_ergNetData = new ErgoNetworkData(this, locationId);

    }
  

    @Override
    public String getDescription(){
        return DESCRIPTION;
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

     
            if(m_ergNetData.isLocationAuthorized(locationString)){
                
                note.remove("locationString");
                note.addProperty("locationString", locationString);

                String networkId = networkIdElement.getAsString();

                switch(networkId){
                    case WALLET_NETWORK:
                        return m_ergNetData.getErgoWallets().sendNote(note);
                    case EXPLORER_NETWORK:
                        return m_ergNetData.getErgoExplorers().sendNote(note);
                    case NODE_NETWORK:
                        return m_ergNetData.getErgoNodes().sendNote(note);
                    case MARKET_NETWORK:
                        return m_ergNetData.getErgoMarkets().sendNote(note);

               
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
            if(m_ergNetData.isLocationAuthorized(locationString)){
                
                note.remove("locationString");
                note.addProperty("locationString", locationString);

                String networkId = networkIdElement.getAsString();

                switch(networkId){
                    case WALLET_NETWORK:
                        return m_ergNetData.getErgoWallets().sendNote(note, onSucceeded, onFailed);
                    case EXPLORER_NETWORK:
                        return m_ergNetData.getErgoExplorers().sendNote(note, onSucceeded, onFailed);
                    case NODE_NETWORK:
                        return m_ergNetData.getErgoNodes().sendNote(note, onSucceeded, onFailed);
                    case MARKET_NETWORK:
                      return m_ergNetData.getErgoMarkets().sendNote(note, onSucceeded, onFailed);
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
    public TabInterface getTab(Stage appStage,  SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject, Button networkBtn){
        if(m_ergoNetworkTab != null){
            return m_ergoNetworkTab;
        }else{
            m_ergoNetworkTab = new ErgoNetworkTab(appStage, heightObject, widthObject, networkBtn);
            return m_ergoNetworkTab;
        }
    }

    private class ErgoNetworkTab extends AppBox implements TabInterface{
        

        private ScrollPane m_walletScroll;
        private ChangeListener<Bounds> m_boundsChange;

        private ErgoWalletsAppBox m_ergoWalletsAppBox = null;
        private ErgoMarketAppBox m_ergoMarketsAppBox = null;
        private ErgoTokenMarketAppBox m_ergoTokenMarketAppBox = null;
        private ErgoExplorersAppBox m_ergoExplorerAppBox = null;
        private ErgoNodesAppBox m_ergoNodesAppBox = null;
        
        private NoteMsgInterface m_ergoNetworkMsgInterface = null;
        
        private SimpleStringProperty m_status = new SimpleStringProperty(App.STATUS_STOPPED);
        private Button m_menuBtn;

        private SimpleStringProperty m_titleProperty = new SimpleStringProperty(getName());
        public String getName(){
            return ErgoNetwork.this.getName();
        }
        public SimpleStringProperty titleProperty(){
            return m_titleProperty;
        }

        

        public ErgoNetworkTab(Stage appStage, SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject, Button networkBtn){
            super(NETWORK_ID);
            m_menuBtn = networkBtn;
     
            setPrefWidth(App.DEFAULT_STATIC_WIDTH);
            setMaxWidth(App.DEFAULT_STATIC_WIDTH);
          
            prefHeightProperty().bind(heightObject);


            m_ergoWalletsAppBox = new ErgoWalletsAppBox(appStage, m_ergNetData.getLocationId(), getNoteInterface());
            m_ergoExplorerAppBox = new ErgoExplorersAppBox(appStage, m_ergNetData.getLocationId(), getNoteInterface());
            m_ergoNodesAppBox = new ErgoNodesAppBox(appStage,  m_ergNetData.getLocationId(), getNoteInterface());
            m_ergoMarketsAppBox = new ErgoMarketAppBox(appStage, m_ergNetData.getLocationId(), getNoteInterface());
            m_ergoTokenMarketAppBox = new ErgoTokenMarketAppBox(appStage, m_ergNetData.getLocationId(), getNoteInterface());
              
            m_ergoNetworkMsgInterface = new NoteMsgInterface() {
                public String getId(){
                    return m_ergNetData.getId();
                }
                public void sendMessage(int code, long timestamp, String networkId, Number num){
                    switch(networkId){
                        case WALLET_NETWORK:
                            m_ergoWalletsAppBox.sendMessage(code, timestamp,networkId, num);
                        break;
                        case NODE_NETWORK:
                            m_ergoNodesAppBox.sendMessage(code, timestamp, networkId, num);
                        break;
                        case EXPLORER_NETWORK:
                            m_ergoExplorerAppBox.sendMessage(code, timestamp, networkId, num);
                        break;
                        case MARKET_NETWORK:
                            m_ergoMarketsAppBox.sendMessage(code, timestamp, networkId, num);
                        break;
                        case TOKEN_MARKET_NETWORK:
                            m_ergoTokenMarketAppBox.sendMessage(code, timestamp, networkId, num);
                        break;
                    }
                }

                public void sendMessage(int code, long timestamp, String networkId, String msg){
                    switch(networkId){
                        case WALLET_NETWORK:
                            m_ergoWalletsAppBox.sendMessage(code, timestamp,networkId, msg);
                        break;
                        case NODE_NETWORK:
                            m_ergoNodesAppBox.sendMessage(code, timestamp, networkId, msg);
                        break;
                        case EXPLORER_NETWORK:
                            m_ergoExplorerAppBox.sendMessage(code, timestamp, networkId, msg);
                        break;
                        case MARKET_NETWORK:
                            m_ergoMarketsAppBox.sendMessage(code, timestamp, networkId, msg);
                        break;
                        case TOKEN_MARKET_NETWORK:
                            m_ergoTokenMarketAppBox.sendMessage(code, timestamp, networkId, msg);
                        break;
                    }
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

            Region hBar3 = new Region();
            hBar3.setPrefWidth(400);
            hBar3.setPrefHeight(2);
            hBar3.setMinHeight(2);
            hBar3.setId("hGradient");

            HBox gBox3 = new HBox(hBar3);
            gBox3.setAlignment(Pos.CENTER);
            gBox3.setPadding(new Insets(5, 0, 5, 0));


            Region appBoxSpacer = new Region();
            VBox.setVgrow(appBoxSpacer, Priority.ALWAYS);
        

            getChildren().addAll(m_ergoWalletsAppBox, appBoxSpacer,gBox, m_ergoMarketsAppBox, gBox1, m_ergoTokenMarketAppBox, gBox2, m_ergoNodesAppBox, gBox3, m_ergoExplorerAppBox);
      
         
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

    
        public void setStatus(String value){
            switch(value){
                case App.STATUS_STOPPED:
                    m_menuBtn.setId("menuTabBtn");
                    shutdown();
                    m_ergoNetworkTab = null;
                break;
                case App.STATUS_MINIMIZED:
                    m_menuBtn.setId("minimizedMenuBtn"); 
                break;
                case App.STATUS_STARTED:
                    m_menuBtn.setId("activeMenuBtn");
                break;
                
            }
            
            m_status.set(value);
        }
    
        
        public String getStatus(){
            return m_status.get();
        } 
    

    }


}
