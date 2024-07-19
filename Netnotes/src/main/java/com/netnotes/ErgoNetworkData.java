package com.netnotes;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;

import org.ergoplatform.appkit.NetworkType;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.utils.Utils;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

public class ErgoNetworkData {
    
    private SimpleLongProperty m_updated = new SimpleLongProperty();

    private double m_stageWidth = 750;
    private double m_stageHeight = 500;

    private ErgoNetwork m_ergoNetwork;

    private ErgoWallets m_ergoWallets = null ;
    private ErgoNodes m_ergoNodes = null;
    private ErgoTokens m_ergoTokens = null;
    private ErgoExplorers m_ergoExplorers = null;

           
    private String m_nodesId = null;
    private String m_explorerId = ErgoPlatformExplorerData.ERGO_PLATFORM_EXPLORER;
    private String m_marketsId = null;
    private String m_tokenMarketId = null;

    private String m_walletId = null;

    private SimpleObjectProperty<NoteInterface> m_selectedWallet = new SimpleObjectProperty<NoteInterface>(null);
    private SimpleObjectProperty<NoteInterface> m_selectedErgoMarket = new SimpleObjectProperty<NoteInterface>(null);
    private SimpleObjectProperty<NoteInterface> m_selectedTokensMarket = new SimpleObjectProperty<NoteInterface>(null);
    private SimpleObjectProperty<NoteInterface> m_selectedNodeData = new SimpleObjectProperty<NoteInterface>(null);
    private SimpleObjectProperty<NoteInterface> m_selectedExplorerData = new SimpleObjectProperty<NoteInterface>(null);



    private ChangeListener<NoteInterface> m_selectedNodeListener = null;
    private ChangeListener<NoteInterface> m_ergoMarketsChangeListener = null;
    private ChangeListener<NoteInterface> m_tokenMarketsChangeListener = null;
    private ChangeListener<NoteInterface> m_selectedWalletListener = null;
    private ChangeListener<NoteInterface> m_explorerChangeListener = null;



    
    private NoteMsgInterface m_ergoMarketMsgInterface = null;

    private NoteMsgInterface m_tokenMarketMsgInterface = null;
    private NoteMsgInterface m_walletMsgInterface = null;

    private NoteMsgInterface m_nodeMsgInterface = null;
    private NoteMsgInterface m_explorerMsgInterface = null;
    private NoteMsgInterface m_tokensMsgInterface = null;
    private NoteMsgInterface m_ergoWalletsMsgInterface = null;
    private String m_id;

    public ErgoNetworkData(ErgoNetwork ergoNetwork) {
        m_id = FriendlyId.createFriendlyId();
        m_ergoNetwork = ergoNetwork;

        File appDir = ergoNetwork.getAppDir();

        if (!appDir.isDirectory()) {
            try {
                Files.createDirectory(appDir.toPath());
            } catch (IOException e) {

            }
        }
       
        installNetworks();
        
        openJson();
        
        addChangeListeners();

        setDefaults();
        
        


    }

    public NetworkType getNetworkType(){
        return m_ergoNetwork.getNetworkType();
    }

    private void setDefaultExplorer(){
        if(m_explorerId != null){
            JsonObject note = Utils.getCmdObject("getExplorerById");
            note.addProperty("id", m_explorerId);
            note.addProperty("networkId", getId());
            Object obj = getErgoExplorers().sendNote(note);
        
            m_selectedExplorerData.set(obj != null && obj instanceof NoteInterface ? (NoteInterface) obj : null);
        }else{
            m_selectedExplorerData.set(null);
        }
    }
    private void setDefaultWallet(){
        if(m_walletId != null){
            JsonObject note = Utils.getCmdObject("getWalletById");
            note.addProperty("id", m_walletId);
            note.addProperty("networkId", getId());
            Object obj = getErgoWallets().sendNote(note);
        
            m_selectedWallet.set(obj != null && obj instanceof NoteInterface ? (NoteInterface) obj : null);
        }else{
            m_selectedWallet.set(null);
        }
    }

    public SimpleObjectProperty<NoteInterface> selectedWallet(){
        return m_selectedWallet;
    }

    private void setDefaults(){
       
        setDefaultExplorer();
        setDefaultWallet();
        
    }

    private void addChangeListeners(){
        m_explorerChangeListener = (obs,oldval,newval)->{
            setExplorer(newval != null ? newval.getNetworkId() : null);
        };

        m_selectedExplorerData.addListener(m_explorerChangeListener);

        m_selectedNodeListener = (obs,oldval,newval)->{
            setNodesId(newval != null ? newval.getNetworkId() : null);
        };

        m_selectedNodeData.addListener(m_selectedNodeListener);
      
        m_ergoMarketsChangeListener = (obs,oldval,newval) ->{
            if(oldval != null && m_ergoMarketMsgInterface != null){
                 oldval.removeMsgListener(m_ergoMarketMsgInterface);
                 m_ergoMarketMsgInterface = null;
            }
            if(newval != null){
                addErgoMarketInterface(newval);
            }
            setMarketsId(newval != null ? newval.getNetworkId() : null);
        };

        m_selectedErgoMarket.addListener(m_ergoMarketsChangeListener);

        m_selectedWalletListener = (obs,oldval,newval)->{
            setWalletId(newval != null ? newval.getNetworkId() : null);
        };
        m_selectedWallet.addListener(m_selectedWalletListener);

    }

    private void openJson(){
        JsonObject json = getErgoNetwork().getNetworksData().getData("getJsonObject", "networkData", ".", ErgoNetwork.NETWORK_ID);

        JsonElement walletIdElement = json != null ? json.get("walledId") : null;
        JsonElement marketIdElement = json != null ? json.get("marketId") : null;
        JsonElement explorerIdElement = json != null ? json.get("explorerId") : null;
        JsonElement nodeIdElement = json != null ? json.get("nodeId") : null;
        JsonElement tokenMarketIdElement = json != null ? json.get("tokenMarketId") : null;

        m_marketsId = marketIdElement != null && marketIdElement.isJsonPrimitive() ? marketIdElement.getAsString() : KucoinExchange.NETWORK_ID;
        m_walletId = walletIdElement != null && walletIdElement.isJsonPrimitive() ? walletIdElement.getAsString() : null;
        m_explorerId = explorerIdElement != null && explorerIdElement.isJsonPrimitive() ? explorerIdElement.getAsString() : ErgoPlatformExplorerData.ERGO_PLATFORM_EXPLORER;
        m_nodesId = nodeIdElement != null && nodeIdElement.isJsonPrimitive() ? nodeIdElement.getAsString() : ErgoNodesList.DEFAULT;
        m_tokenMarketId = tokenMarketIdElement != null && tokenMarketIdElement.isJsonPrimitive() ? tokenMarketIdElement.getAsString() : KucoinExchange.NETWORK_ID;
        
    }

    public SimpleLongProperty updatedProperty(){
        return m_updated;
    }


    public String getId(){
        return m_id;
    }
    
    private void addErgoMarketInterface(NoteInterface market){
        

        m_ergoMarketMsgInterface = new NoteMsgInterface(){
            String interfaceId = FriendlyId.createFriendlyId();

            public String getId() {
                return interfaceId;
            }
            
            public void sendMessage(String networkId, int code, long timestamp){

            }

            public void sendMessage(int msg, long timestamp){
                switch(msg){
                    case App.STARTED:
                    case App.LIST_CHANGED:
                    case App.LIST_UPDATED:
                        getErgoQuote();
                    break;
                }
                
            }
            public void sendMessage(String networkId, int code, long timestamp, String msg){
                
            }
            public void sendMessage(int code, long timestamp, String msg){
                
            }
            public void sendMessage(String networkId, int code, long timestamp, JsonObject json){
            }
        };

        if(market.getConnectionStatus() == App.STARTED){
            getErgoQuote();
        }

        market.addMsgListener(m_ergoMarketMsgInterface);

        

    }
 
    private SimpleObjectProperty<PriceQuote> m_ergoPriceQuoteProperty = new SimpleObjectProperty<>();
    private String m_currentQuoteMarketId = null;
    private String m_stableSymbol = "USD";

    public void getErgoQuote(){
        NoteInterface ergoMarketInterface = m_selectedErgoMarket.get();
        
        if(ergoMarketInterface != null){
            
            if(m_ergoPriceQuoteProperty.get() == null || m_currentQuoteMarketId == null || (m_currentQuoteMarketId != null && !m_currentQuoteMarketId.equals(ergoMarketInterface.getNetworkId()))){
                
                Object succededObject = ergoMarketInterface.sendNote(getErgoQuoteWithSymbol(m_stableSymbol));

                if(succededObject != null && succededObject instanceof PriceQuote){
                    PriceQuote newQuote = (PriceQuote) succededObject;
                    m_currentQuoteMarketId = ergoMarketInterface.getNetworkId();

                    m_ergoPriceQuoteProperty.set(newQuote);
                }
                
            }
        }


        
    }

    public SimpleObjectProperty<PriceQuote> ergoPriceQuoteProperty(){
        return m_ergoPriceQuoteProperty;
    }



      public static JsonObject getTokenQuoteJson(String tokenId){
        JsonObject json = Utils.getCmdObject("getQuote");
        json.addProperty("baseType", "id");
        json.addProperty("base", ErgoCurrency.TOKEN_ID);
        json.addProperty("quoteType","firstId");
        json.addProperty("quote", tokenId);

        return json;
    }

    public static JsonObject getErgoQuoteWithSymbol(String firstQuoteSymbolContains){
        JsonObject json = Utils.getCmdObject("getQuote");
        json.addProperty("baseType", "symbol");
        json.addProperty("base", ErgoCurrency.SYMBOL);
        json.addProperty("quoteType", "firstSymbolContains");
        json.addProperty("quote", firstQuoteSymbolContains);

        return json;
    }

    
    public void updateTokenQuote(PriceAmount tokenAmount, String marketInterfaceId){
        

        NoteInterface tokenMarketInterface = m_selectedTokensMarket.get();
        
        if(tokenMarketInterface != null){
            
            String currentQuoteMarketId = tokenAmount.getMarketId();
     

            if(tokenAmount.priceQuoteProperty().get() == null || currentQuoteMarketId == null || (currentQuoteMarketId != null && !currentQuoteMarketId.equals(marketInterfaceId))){
                String tokenId = tokenAmount.getTokenId();
              
                
                Object succededObject = tokenMarketInterface.sendNote(getTokenQuoteJson(tokenId));
                  
                if(succededObject != null && succededObject instanceof PriceQuote){
                    PriceQuote newQuote = (PriceQuote) succededObject;
                    tokenAmount.setMarketId(marketInterfaceId);

                    tokenAmount.priceQuoteProperty().set(newQuote);
                    
                }
                
            }
        }else{

        }
    }

    
    public SimpleObjectProperty<NoteInterface> selectedNodeData() {
        return m_selectedNodeData;
    }

    public SimpleObjectProperty<NoteInterface> selectedErgoMarket() {
        return m_selectedErgoMarket;
    }

    public SimpleObjectProperty<NoteInterface> selectedTokenMarket() {
        return m_selectedTokensMarket;
    }
    
    public SimpleObjectProperty<NoteInterface> selectedExplorerData() {
        return m_selectedExplorerData;
    }



    public ErgoWallets getErgoWallets(){
        return m_ergoWallets;
    }

    public ErgoNodes getErgoNodes(){
        return m_ergoNodes;
    }

    public ErgoTokens getErgoTokens(){
        return m_ergoTokens;
    }

    public ErgoExplorers getErgoExplorers(){
        return m_ergoExplorers;
    }




    public void installNetworks() {
     
        
        m_ergoTokens = new ErgoTokens(this, m_ergoNetwork);
        m_ergoWallets = new ErgoWallets(this, m_ergoNetwork);
        m_ergoExplorers = new ErgoExplorers(this, m_ergoNetwork); 
        m_ergoNodes = new ErgoNodes(this, m_ergoNetwork);
            
        
        addListeners();
    }



    private void addListeners(){
        
        m_explorerMsgInterface = new NoteMsgInterface() {
                
            private String m_interfaceId = FriendlyId.createFriendlyId();

            public String getId(){
                return m_interfaceId;
            }

            public void sendMessage(String networkId, int code, long timestamp){

            }

            public void sendMessage(int code, long timestamp){
                m_ergoNetwork.sendMessage(ErgoExplorers.NETWORK_ID, code, timestamp, "Updated");
            }

            public void sendMessage(int code, long timestamp, String msg){

            }

            public void sendMessage(String networkId, int code, long timestamp, String msg){

            }
            public void sendMessage(String networkId, int code, long timestamp, JsonObject json){
            }

        };

        getErgoExplorers().addMsgListener(m_explorerMsgInterface);

           
            
       
         
        m_nodeMsgInterface = new NoteMsgInterface() {
                
            private String m_interfaceId = FriendlyId.createFriendlyId();

            public String getId(){
                return m_interfaceId;
            }

            public void sendMessage(String networkId, int code, long timestamp){

            }

            public void sendMessage(int code, long timestamp){
                m_ergoNetwork.sendMessage(ErgoNodes.NETWORK_ID, code, timestamp, "Updated");
            }

            public void sendMessage(int code, long timestamp, String msg){

            }

            public void sendMessage(String networkId, int code, long timestamp, String msg){

            }

            public void sendMessage(String networkId, int code, long timestamp, JsonObject json){
            }

        };

        getErgoNodes().addMsgListener(m_nodeMsgInterface);

        m_tokensMsgInterface = new NoteMsgInterface() {
            
            private String m_interfaceId = FriendlyId.createFriendlyId();

            public String getId(){
                return m_interfaceId;
            }

            public void sendMessage(String networkId, int code, long timestamp){

            }

            public void sendMessage(int code, long timestamp){
                m_ergoNetwork.sendMessage(ErgoTokens.NETWORK_ID, code, timestamp, "Updated");
            }

            public void sendMessage(int code, long timestamp, String msg){

            }

            public void sendMessage(String networkId, int code, long timestamp, String msg){

            }
            public void sendMessage(String networkId, int code, long timestamp, JsonObject json){
            }

        };

        m_ergoTokens.addMsgListener(m_tokensMsgInterface);
        

        m_ergoWalletsMsgInterface = new NoteMsgInterface() {
            
            private String m_interfaceId = FriendlyId.createFriendlyId();

            public String getId(){
                return m_interfaceId;
            }

            public void sendMessage(String networkId, int code, long timestamp){

            }

            public void sendMessage(int code, long timestamp){
                
            }

            public void sendMessage(int code, long timestamp, String msg){
                m_ergoNetwork.sendMessage(ErgoWallets.NETWORK_ID, code, timestamp, msg);
            }

            public void sendMessage(String networkId, int code, long timestamp, String msg){
                
            }
            
            public void sendMessage(String networkId, int code, long timestamp, JsonObject json){
                
            } 
            
        };

        m_ergoWallets.addMsgListener(m_ergoWalletsMsgInterface);
    }



    public void shutdown() {
        if(m_explorerMsgInterface != null && m_ergoExplorers != null){
            m_ergoExplorers.removeMsgListener(m_explorerMsgInterface);
        }
        if(m_ergoTokens != null && m_tokensMsgInterface != null){
            m_ergoTokens.removeMsgListener(m_tokensMsgInterface);
        }
        if(m_ergoWallets != null && m_ergoWalletsMsgInterface != null){
            m_ergoWallets.removeMsgListener(m_ergoWalletsMsgInterface);
        }
        if(m_ergoNodes != null && m_nodeMsgInterface != null){
            m_ergoNodes.removeMsgListener(m_nodeMsgInterface);
        }

    }




    public JsonObject getStageObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("width", m_stageWidth);
        jsonObject.addProperty("height", m_stageHeight);
        return jsonObject;
    }

    public ErgoNetwork getErgoNetwork(){
        return m_ergoNetwork;
    }




    protected String getWalletId(){
        return m_walletId;
    }




    public void setTokenMarketId(String marketId){
        m_tokenMarketId = marketId;
        m_ergoNetwork.sendMessage("tokenMarketId", App.UPDATED, System.currentTimeMillis(), marketId);
    }

    
    public String getTokenMarketId(){
        return m_tokenMarketId;
    }


    public void setNodesId(String nodesId) {
        
        m_nodesId = nodesId;
        
        m_ergoNetwork.sendMessage("nodeId", App.UPDATED, System.currentTimeMillis(), nodesId);
    }
        
    public String getMarketsId() {
        return m_marketsId;
    }

  

    public String getNodesId() {
        return m_nodesId;
    }


 

    public String getExplorerId() {
        return m_explorerId;
    }

    private void setWalletId(String walletId){
        m_walletId = walletId;
        save();
        m_ergoNetwork.sendMessage("walletId", App.UPDATED, System.currentTimeMillis(), walletId);
    }
    
    private void setExplorer(String explorerId) {
        m_explorerId = explorerId;
        save();
        m_ergoNetwork.sendMessage("explorerId", App.UPDATED, System.currentTimeMillis(), explorerId);
    }

    private void setMarketsId(String marketsId) {
        m_marketsId = marketsId;
        save();
        m_ergoNetwork.sendMessage("marketId", App.UPDATED, System.currentTimeMillis(), marketsId);
    }

    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        if(m_walletId != null){
            json.addProperty("walledId", m_walletId);
        }
        if(m_marketsId != null){
            json.addProperty("marketId", m_marketsId);
        }
        if(m_explorerId != null){
            json.addProperty("explorerId", m_explorerId);
        }
        if(m_nodesId != null){
            json.addProperty("nodeId", m_nodesId);
        }
        if(m_tokenMarketId != null){
            json.addProperty("tokenMarketId", m_tokenMarketId);
        }
        return json;
    }

    public void save(){
        getErgoNetwork().getNetworksData().save("getJsonObject", "networkData", ".", ErgoNetwork.NETWORK_ID, getJsonObject());
    }

    
    /*  private Image m_offlineImage = new Image("/assets/cloud-offline-30.png");
     private Image m_explorerImage = new Image("/assets/ergo-explorer-30.png");

         private SimpleObjectProperty<ErgoExplorerList> m_ergoExplorerList = new SimpleObjectProperty<>(null);


    
    public void updateMarketMenu(Menu menuBtn){
        menuBtn.getItems().clear();

        String selectedMarketId = m_walletData.getMarketsId();
       
            
        for(NetworkInformation marketInfo : ERGO_MARKETS){
            NoteInterface noteInterface = m_walletData.getNetworksData().getNoteInterface(marketInfo.getNetworkId());
            ImageView imgView = new ImageView(new Image(marketInfo.iconString()));
            imgView.setPreserveRatio(true);
            imgView.setFitHeight(30);
     
            boolean selected = selectedMarketId != null ? selectedMarketId.equals(marketInfo.getNetworkId()) : false;
            MenuItem menuItem = new MenuItem(marketInfo.getNetworkName() + (noteInterface == null && !marketInfo.getNetworkId().equals(NOMARKET.getNetworkId()) ? ": (not installed)" : (selectedMarketId != null && selected ? " (selected)" :  "") ), imgView);
      

            if(selected){
                menuItem.setId("selectedMenuItem");
               
            }
            
            menuItem.setUserData(marketInfo);
        
            menuItem.setOnAction(e->{
                NetworkInformation mInfo = (NetworkInformation) menuItem.getUserData();
                m_walletData.setMarketsId(mInfo.getNetworkId());
                NoteInterface marketInterface = m_walletData.getNetworksData().getNoteInterface(mInfo.getNetworkId());
     
                m_selectedErgoMarket.set(marketInterface != null ? marketInterface : null);
               
            });

            menuBtn.getItems().add(menuItem);

        }

        
    }

    public void updateTokenMarketMenu(Menu menuBtn){
        menuBtn.getItems().clear();

        String selectedMarketId = m_walletData.getMarketsId();
 
            
        for(NetworkInformation marketInfo : ERGO_TOKEN_MARKETS){
            NoteInterface noteInterface = m_walletData.getNetworksData().getNoteInterface(marketInfo.getNetworkId());
            ImageView imgView = new ImageView(new Image(marketInfo.iconString()));
            imgView.setPreserveRatio(true);
            imgView.setFitHeight(30);
     
            boolean selected = selectedMarketId != null ? selectedMarketId.equals(marketInfo.getNetworkId()) : false;
            MenuItem menuItem = new MenuItem(marketInfo.getNetworkName() + (noteInterface == null && !marketInfo.getNetworkId().equals(NOMARKET.getNetworkId()) ? ": (not installed)" : (selectedMarketId != null && selected ? " (selected)" :  "") ), imgView);
      

            if(selected){
                menuItem.setId("selectedMenuItem");
         
            }
            
            menuItem.setUserData(marketInfo);
        
            menuItem.setOnAction(e->{
                NetworkInformation mInfo = (NetworkInformation) menuItem.getUserData();
                m_walletData.setMarketsId(mInfo.getNetworkId());
                NoteInterface marketInterface = m_walletData.getNetworksData().getNoteInterface(mInfo.getNetworkId());
     
                m_selectedErgoMarket.set(marketInterface != null ? marketInterface : null);
               
            });

            menuBtn.getItems().add(menuItem);

        }
        
        
    }


    public void updateMarketMenu(MenuButton menuBtn){
        menuBtn.getItems().clear();

        String selectedMarketId = m_walletData.getMarketsId();
            
        for(NetworkInformation marketInfo : ERGO_MARKETS){
            NoteInterface noteInterface = m_walletData.getNetworksData().getNoteInterface(marketInfo.getNetworkId());
            ImageView imgView = new ImageView(new Image(marketInfo.iconString()));
            imgView.setPreserveRatio(true);
            imgView.setFitHeight(30);
     
            boolean selected = selectedMarketId != null ? selectedMarketId.equals(marketInfo.getNetworkId()) : false;
            MenuItem menuItem = new MenuItem(marketInfo.getNetworkName() + (noteInterface == null && !marketInfo.getNetworkId().equals(NOMARKET.getNetworkId()) ? ": (not installed)" : (selectedMarketId != null && selected ? " (selected)" :  "") ), imgView);
      

            if(selected){
            
                menuItem.setId("selectedMenuItem");
  
            }
            
            menuItem.setUserData(marketInfo);
        
            menuItem.setOnAction(e->{
                NetworkInformation mInfo = (NetworkInformation) menuItem.getUserData();
                m_walletData.setMarketsId(mInfo.getNetworkId());
                NoteInterface marketInterface = m_walletData.getNetworksData().getNoteInterface(mInfo.getNetworkId());
     
   
                m_selectedErgoMarket.set(marketInterface != null ? marketInterface : null);
               
           
            });

            menuBtn.getItems().add(menuItem);
            
        }

       
    }

    
    public void updateTokenMarketMenu(BufferedMenuButton menuBtn, Tooltip tooltip){
        menuBtn.getItems().clear();

        String selectedMarketId = m_walletData.getTokenMarketId();
        SimpleBooleanProperty found = new SimpleBooleanProperty(false);
            
        for(NetworkInformation marketInfo : ERGO_TOKEN_MARKETS){
            NoteInterface noteInterface = m_walletData.getNetworksData().getNoteInterface(marketInfo.getNetworkId());
            ImageView imgView = new ImageView(new Image(marketInfo.iconString()));
            imgView.setPreserveRatio(true);
            imgView.setFitHeight(30);
     
            boolean selected = selectedMarketId != null ? selectedMarketId.equals(marketInfo.getNetworkId()) : false;
            MenuItem menuItem = new MenuItem(marketInfo.getNetworkName() + (noteInterface == null && !marketInfo.getNetworkId().equals(NOMARKET.getNetworkId()) ? ": (not installed)" : (selectedMarketId != null && selected ? " (selected)" :  "") ), imgView);
      

            if(selected){
            
                menuBtn.setImage(new Image(marketInfo.iconString()));
                found.set(true);
                menuItem.setId("selectedMenuItem");
                tooltip.setText("Token Market: " + marketInfo.getNetworkName());
            }
            
            menuItem.setUserData(marketInfo);
        
            menuItem.setOnAction(e->{
                NetworkInformation mInfo = (NetworkInformation) menuItem.getUserData();
                m_walletData.setMarketsId(mInfo.getNetworkId());
                NoteInterface marketInterface = m_walletData.getNetworksData().getNoteInterface(mInfo.getNetworkId());
     
                menuBtn.setImage(new Image(mInfo.iconString()));
                m_selectedErgoMarket.set(marketInterface != null ? marketInterface : null);
               
                updateTokenMarketMenu(menuBtn, tooltip);
            });

            menuBtn.getItems().add(menuItem);

        }

        if(!found.get()){
            tooltip.setText("Token Market: unavailable");
   
            menuBtn.setImage(new Image("/assets/bar-chart-30.png"));
        }
    }

   private void addTokenInterface(ErgoTokens ergoTokens){
       
         String tokensInterfaceId = FriendlyId.createFriendlyId();
        m_tokenMsgInterface = new NoteMsgInterface(){

            public String getId() {
                return tokensInterfaceId;
            }
            
            public void sendMessage(String networkId, int code, long timestamp){

            }

            public void sendMessage(int msg, long timestamp){
                switch(msg){
                    case App.STARTED:
                    
                    break;
                    case App.STOPPED:
                    
                    break;
                    case App.LIST_CHANGED:
                    case App.LIST_UPDATED:
                    
                    break;
                }
               

            }
            public void sendMessage(String networkId, int code, long timestamp, String msg){
                
            }
            public void sendMessage(int code, long timestamp, String msg){
                
            }
        };
        ergoTokens.addMsgListener(m_tokenMsgInterface);
    }

    

    
    private void addTokenMarketInterface(NoteInterface market){
        String marketInterfaceId = FriendlyId.createFriendlyId();

        m_ergoMarketMsgInterface = new NoteMsgInterface(){
            
            public String getId() {
                return marketInterfaceId;
            }
            
            public void sendMessage(String networkId, int code, long timestamp){

            }

            public void sendMessage(int msg, long timestamp){
                switch(msg){
                    case App.STARTED:
                    case App.LIST_CHANGED:
                    case App.LIST_UPDATED:
                        updateTokenQuotes(marketInterfaceId);
                    break;
                }
                
            }
            public void sendMessage(String networkId, int code, long timestamp, String msg){
                
            }
            public void sendMessage(int code, long timestamp, String msg){
                
            }
        };

        if(market.getConnectionStatus() == App.STARTED){
            updateTokenQuotes(marketInterfaceId);
        }

        market.addMsgListener(m_ergoMarketMsgInterface);

        

    }

        private NoteMsgInterface m_tokenMarketMsgInterface = null;
    private NoteMsgInterface m_ergoMarketMsgInterface = null;
    
    private ChangeListener<ErgoTokens> m_ergoTokensChangeListener = null;
    private NoteMsgInterface m_tokenMsgInterface = null;
    

    private ChangeListener<NoteInterface> m_ergoMarketsChangeListener = null;
    private ChangeListener<NoteInterface> m_ergoTokenMarketChangeListener = null;


    //shutdown 

        if(m_ergoTokensChangeListener != null){
            m_ergoTokens.removeListener(m_ergoTokensChangeListener);
            m_ergoTokensChangeListener = null;
        }

        m_selectedErgoMarket.set(null);

        if(m_ergoMarketsChangeListener != null){
            m_selectedErgoMarket.removeListener(m_ergoMarketsChangeListener);
        }
        
        m_selectedTokensMarket.set(null);
     */

}
