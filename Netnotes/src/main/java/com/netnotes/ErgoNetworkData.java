package com.netnotes;

import java.util.ArrayList;

import org.ergoplatform.appkit.NetworkType;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonObject;

import javafx.beans.property.SimpleLongProperty;

public class ErgoNetworkData {
    
    private SimpleLongProperty m_updated = new SimpleLongProperty();

    private double m_stageWidth = 750;
    private double m_stageHeight = 500;

    private ErgoNetwork m_ergoNetwork;

    private ErgoWallets m_ergoWallets = null ;
    private ErgoNodes m_ergoNodes = null;
    private ErgoTokens m_ergoTokens = null;
    private ErgoExplorers m_ergoExplorers = null;
    private ErgoMarkets m_ergoMarkets = null;
           
    private ArrayList<String> m_authorizedLocations = new ArrayList<>();

    private String m_locationId;
    private String m_id;

    public ErgoNetworkData(ErgoNetwork ergoNetwork, String locationId) {
        m_id = FriendlyId.createFriendlyId();
        m_ergoNetwork = ergoNetwork;
        m_locationId = locationId;

        installNetworks();
        
        
    }

    public NetworkType getNetworkType(){
        return m_ergoNetwork.getNetworkType();
    }


    public String getLocationId(){
        return m_locationId;
    }

    public boolean isLocationAuthorized(String locationString){
        if(locationString != null){
            return locationString.equals(ErgoNetwork.NAME) || m_authorizedLocations.contains(locationString);
        }else{
            return false;
        }
    }

    public SimpleLongProperty updatedProperty(){
        return m_updated;
    }


    public String getId(){
        return m_id;
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

    public ErgoMarkets getErgoMarkets(){
        return m_ergoMarkets;
    }


    public void installNetworks() {
     
        
        m_ergoTokens = new ErgoTokens(this, m_ergoNetwork);
        m_ergoWallets = new ErgoWallets(this, m_ergoNetwork);
        m_ergoExplorers = new ErgoExplorers(this, m_ergoNetwork); 
        m_ergoNodes = new ErgoNodes(this, m_ergoNetwork);
        m_ergoMarkets = new ErgoMarkets(this, m_ergoNetwork);
        
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




    public void shutdown(){

    }



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


