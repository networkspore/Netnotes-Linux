package com.netnotes;


import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import java.util.List;

import com.google.gson.JsonArray;

public class ErgoMarkets extends Network implements NoteInterface {
    public static final String NAME = "Ergo Markets";
    public static final String DESCRIPTION = "A collection of markets for Ergo";
    public static final int MARKET_LIST_DEFAULT_CHANGED = 25;
    public static final int TOKEN_LIST_DEFAULT_CHANGED = 26;
  
    private ErgoNetworkData m_ergNetData = null;

    private String m_defaultMarketId = SpectrumFinance.NETWORK_ID;
    private String m_defaultTokenMarketId = SpectrumFinance.NETWORK_ID;

    

    public ErgoMarkets(ErgoNetworkData ergNetData, ErgoNetwork ergoNetwork){
        super(new Image(getAppIconString()), NAME, ErgoNetwork.MARKET_NETWORK, ergoNetwork);
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

    @Override
    public String getDescription(){
        return DESCRIPTION;
    }

    @Override
    public Object sendNote(JsonObject note){
        if(note != null){
            JsonElement cmdElement = note.get(App.CMD);

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

    public boolean clearDefaultMarket(){

        m_defaultMarketId = null;
        long timeStamp = System.currentTimeMillis();
        
        getErgoNetwork().sendMessage(MARKET_LIST_DEFAULT_CHANGED, timeStamp, ErgoNetwork.MARKET_NETWORK, (String) null);
        save();

        return true;
    }

    public Boolean clearDefaultTokenMarket(){

        m_defaultMarketId = null;
        long timeStamp = System.currentTimeMillis();
        
        getErgoNetwork().sendMessage(TOKEN_LIST_DEFAULT_CHANGED, timeStamp, ErgoNetwork.MARKET_NETWORK, (String) null);
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
            
            getErgoNetwork().sendMessage(MARKET_LIST_DEFAULT_CHANGED, timeStamp, ErgoNetwork.MARKET_NETWORK, defaultMarketId);
        }
    }

    public void setDefaulTokenMarketId(String defaultTokenMarketId, boolean isSave) {
        this.m_defaultTokenMarketId = defaultTokenMarketId;
        
        if(isSave){
          
            save();
            long timeStamp = System.currentTimeMillis();
            
            getErgoNetwork().sendMessage(TOKEN_LIST_DEFAULT_CHANGED, timeStamp, ErgoNetwork.MARKET_NETWORK, defaultTokenMarketId);
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


      @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        JsonElement cmdElement = note.get(App.CMD);
        if (cmdElement != null) {
            String cmd  = cmdElement.getAsString();
            switch(cmd){
                default:
            }
        }
        return false;
    }




    public JsonObject getMarketById(JsonObject json){
        JsonElement idElement = json.get("id");

        if(idElement != null && idElement.isJsonPrimitive()){
            NoteInterface marketInterface = getMarket(idElement.getAsString());
            return marketInterface != null ? marketInterface.getJsonObject() : null;
        }

        return null;
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
        JsonObject json = getNetworksData().getData("data", ".", ErgoNetwork.MARKET_NETWORK, ErgoNetwork.NETWORK_ID);

        if(json != null){
            
            openJson(json);
        }
    }


    public void save() {
       
        getNetworksData().save("data", ".", ErgoNetwork.MARKET_NETWORK, ErgoNetwork.NETWORK_ID, getJsonObject());
        
    }

 
    @Override
    public JsonObject getJsonObject() {
        JsonObject json = super.getJsonObject();
        json.addProperty("defaultMarketId", m_defaultMarketId);
        json.addProperty("defaultTokenMarketId", m_defaultTokenMarketId);
        return json;
    }

    private void openJson(JsonObject json){

        JsonElement defaultMarketIdElement = json != null ? json.get("defaultMarketId") : null;
        JsonElement defaultTokenMarketIdElement = json != null ? json.get("defaultTokenMarketId") : null;
        m_defaultMarketId = defaultMarketIdElement != null && !defaultMarketIdElement.isJsonNull() ? defaultMarketIdElement.getAsString() : null;
        m_defaultTokenMarketId = defaultTokenMarketIdElement != null && !defaultTokenMarketIdElement.isJsonNull() ? defaultTokenMarketIdElement.getAsString() : null;
        
    }
}
