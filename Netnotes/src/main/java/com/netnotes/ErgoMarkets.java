package com.netnotes;


import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import com.google.gson.JsonObject;
import io.netnotes.engine.NetworksData;
import io.netnotes.engine.NoteConstants;
import io.netnotes.engine.NoteInterface;
import com.google.gson.JsonElement;

import java.util.List;
import java.util.concurrent.Future;

import com.google.gson.JsonArray;

public class ErgoMarkets {
    public static final String NAME = "Ergo Markets";
    public static final String DESCRIPTION = "A collection of markets for Ergo";
  
    private ErgoNetworkData m_ergNetData = null;

    private String m_defaultMarketId = ErgoDex.NETWORK_ID;
    private String m_defaultTokenMarketId = ErgoDex.NETWORK_ID;

    

    public ErgoMarkets(ErgoNetworkData ergNetData, ErgoNetwork ergoNetwork){
     
        m_ergNetData = ergNetData;
        getData();
    }

    public Image getSmallAppIcon(){
        return new Image(getSmallAppIconString());
    }

    public static String getAppIconString(){
        return "/assets/bar-chart-150.png";
    }
    public Image getAppIcon(){
        return new Image(getAppIconString());   
    }

    public static String getSmallAppIconString(){
        return "/assets/bar-chart-30.png";
    }


    public String getDescription(){
        return DESCRIPTION;
    }

   
    public Object sendNote(JsonObject note){
        
        if(note != null){
            JsonElement cmdElement = note.get(NoteConstants.CMD);

            switch (cmdElement.getAsString()) {
                case "getMarketById":
                    return getMarketById(note);
                case "getMarketByName":
                    return getMarketByName(note);
                //Market
                case "getMarkets":
                    return getMarkets();
                case "getDefaultMarketInterface":
                    return getDefaultMarketInterface(note);
                case "setDefaultMarket":
                    return setDefaultMarket(note);
                case "clearDefaultMarket":
                    return clearDefaultMarket();
                //Tokens
                case "getTokenMarkets":
                    return getTokenMarkets();
                case "setDefaultTokenMarket":
                    return setDefaultTokenMarket(note);
                case "getDefaultTokenInterface":
                    return getDefaultTokenInterface(note);
                case "clearDefaultTokenMarket":
                    return clearDefaultTokenMarket();
            }
        
            
        }

        return null;
    }

    public void sendMessage(int code, long timeStamp, String networkId, String msg) {
        if(networkId != null && networkId.equals(NetworksData.APPS)){
            switch(code){
                case NoteConstants.LIST_ITEM_ADDED:
                case NoteConstants.LIST_ITEM_REMOVED:
                    getErgoNetwork().sendMessage(NoteConstants.LIST_DEFAULT_CHANGED, timeStamp, NoteConstants.MARKET_NETWORK, m_defaultMarketId);
                    getErgoNetwork().sendMessage(NoteConstants.LIST_DEFAULT_CHANGED, timeStamp, NoteConstants.TOKEN_MARKET_NETWORK, m_defaultTokenMarketId);
                break;
            }
        }
    }

    public boolean clearDefaultMarket(){

        m_defaultMarketId = null;
        long timeStamp = System.currentTimeMillis();
        
        getErgoNetwork().sendMessage(NoteConstants.LIST_DEFAULT_CHANGED, timeStamp, NoteConstants.MARKET_NETWORK, (String) null);
        save();

        return true;
    }

    public Boolean clearDefaultTokenMarket(){

        m_defaultTokenMarketId = null;
        long timeStamp = System.currentTimeMillis();
        
        getErgoNetwork().sendMessage(NoteConstants.LIST_DEFAULT_CHANGED, timeStamp, NoteConstants.TOKEN_MARKET_NETWORK, (String) null);
        save();

        return true;
    }


    public ErgoNetwork getErgoNetwork(){
        return m_ergNetData.getErgoNetwork();
    }


    public String getDefaultMarketId() {
        return m_defaultMarketId;
    }



    public void setDefaultMarketId(String defaultMarketId, boolean isSave) {
        this.m_defaultMarketId = defaultMarketId;
        
        if(isSave){
          
            save();
            long timeStamp = System.currentTimeMillis();
            
            getErgoNetwork().sendMessage(NoteConstants.LIST_DEFAULT_CHANGED, timeStamp, NoteConstants.MARKET_NETWORK, defaultMarketId);
        }
    }

    public void setDefaulTokenMarketId(String defaultTokenMarketId, boolean isSave) {
        this.m_defaultTokenMarketId = defaultTokenMarketId;
        
        if(isSave){
          
            save();
            long timeStamp = System.currentTimeMillis();
        
            getErgoNetwork().sendMessage(NoteConstants.LIST_DEFAULT_CHANGED, timeStamp, NoteConstants.TOKEN_MARKET_NETWORK, defaultTokenMarketId);
        }
    }

    public boolean setDefaultTokenMarket(JsonObject note){
        JsonElement idElement = note != null ? note.get("id") : null;
  
        if(idElement != null){
            String defaultId = !idElement.isJsonNull() ? idElement.getAsString() : null;
            if(defaultId != null){
                NoteInterface noteInterface = getMarket(defaultId);

                if(noteInterface != null){
                    setDefaulTokenMarketId(defaultId, true);
                    return true;
                }
            }else{
                clearDefaultTokenMarket();
            }
        }
        return false;
    }
    
    public boolean setDefaultMarket(JsonObject note){
        JsonElement idElement = note != null ? note.get("id") : null;
  
        if(idElement != null){
            String defaultId = !idElement.isJsonNull() ? idElement.getAsString() : null;
            if(defaultId != null){
                NoteInterface noteInterface = getMarket(defaultId);

                if(noteInterface != null){
                    setDefaultMarketId(defaultId, true);
                    return true;
                }
            }else{
                clearDefaultMarket();
            }
        }
        return false;
    }


  
    public Future<?> sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        JsonElement cmdElement = note != null ? note.get(NoteConstants.CMD) : null;
        JsonElement idElement = note != null ? note.get("id") : null;
        
        if(cmdElement != null){

            switch(cmdElement.getAsString()){
                
                default: 
                    String id = idElement != null ? idElement.getAsString() : getDefaultMarketId();
                
                    NoteInterface market = id != null ? getMarket(id) : null;
                
                    if(market != null){
                    
                        return market.sendNote(note, onSucceeded, onFailed);
                    }
            }
        }

        return null;
    }




    public JsonObject getMarketById(JsonObject json){
        JsonElement idElement = json.get("id");

        if(idElement != null && idElement.isJsonPrimitive()){
            NoteInterface marketInterface = getMarket(idElement.getAsString());
            return marketInterface != null ? marketInterface.getJsonObject() : null;
        }

        return null;
    }
    
    public NetworksData getNetworksData(){
        return m_ergNetData.getNetworksData();
    }


    public NoteInterface getMarket(String id){
  
        if(id != null){

            return  getNetworksData().getApp(id);
         
        }
        return null;
    }

    public NoteInterface getDefaultMarketInterface(JsonObject note){
        return getMarket(m_defaultMarketId);
    }

    public NoteInterface getDefaultTokenInterface(JsonObject note){
        return getMarket(m_defaultTokenMarketId);
    }

   
    public JsonObject getMarketByName(JsonObject note){
        JsonElement nameElement = note.get("name");

        
        if(nameElement != null && nameElement.isJsonPrimitive()){
            
            String name = nameElement.getAsString();

            List<NoteInterface> list = getNetworksData().getAppsContainsAllKeyWords("ergo", "exchange");
            
            for ( NoteInterface noteInterface :list) {

                if(noteInterface.getName().equals(name)){
                    return noteInterface.getJsonObject();
                }
            }

        }

        return null;
    } 

    /*private JsonArray getTokenMarkets(){
    
        List<NoteInterface> list = getNetworksData().getAppsContainsAllKeyWords("ergo", "exchange", "usd", "ergo tokens");
            
        JsonArray jsonArray = new JsonArray();

         for ( NoteInterface data : list) {

            JsonObject result =  data.getJsonObject();
            jsonArray.add(result);

        }
    
        return jsonArray;
    }*/

    private JsonArray getMarkets(){
    
        List<NoteInterface> list = getNetworksData().getAppsContainsAllKeyWords("ergo", "exchange", "usd");
            
        JsonArray jsonArray = new JsonArray();

         for ( NoteInterface data : list) {

            JsonObject result =  data.getJsonObject();
            jsonArray.add(result);

        }
    
        return jsonArray;
    }

    private JsonArray getTokenMarkets(){
    
        List<NoteInterface> list = getNetworksData().getAppsContainsAllKeyWords("ergo tokens", "exchange", "usd");
            
        JsonArray jsonArray = new JsonArray();

         for ( NoteInterface data : list) {

            JsonObject result =  data.getJsonObject();
            jsonArray.add(result);

        }
    
        return jsonArray;
    }

    private void getData(){
        getNetworksData().getData("data", ".", NoteConstants.MARKET_NETWORK, NoteConstants.ERGO_NETWORK_ID, onSucceded ->{
            Object obj = onSucceded.getSource().getValue();
        
            JsonObject json = obj != null && obj instanceof JsonObject ? (JsonObject) obj : null;
            if(json != null){
            
                openJson(json);
            }
        });

       
    }


    public void save() {
       
        getNetworksData().save("data", ".", NoteConstants.MARKET_NETWORK, NoteConstants.ERGO_NETWORK_ID, getJsonObject());
        
    }

 
  
    public JsonObject getJsonObject() {
        JsonObject json = new JsonObject();
        if(m_defaultMarketId != null){
            json.addProperty("defaultMarketId", m_defaultMarketId);
        }
        if(m_defaultTokenMarketId != null){
            json.addProperty("defaultTokenMarketId", m_defaultTokenMarketId);
        }
        return json;
    }

    private void openJson(JsonObject json){

        JsonElement defaultMarketIdElement = json != null ? json.get("defaultMarketId") : null;
        JsonElement defaultTokenMarketIdElement = json != null ? json.get("defaultTokenMarketId") : null;
        m_defaultMarketId = defaultMarketIdElement != null && !defaultMarketIdElement.isJsonNull() ? defaultMarketIdElement.getAsString() : null;
        m_defaultTokenMarketId = defaultTokenMarketIdElement != null && !defaultTokenMarketIdElement.isJsonNull() ? defaultTokenMarketIdElement.getAsString() : null;
        
    }
}
