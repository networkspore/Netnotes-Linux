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

  
    private ErgoNetworkData m_ergNetData = null;

    private String m_defaultMarketId = SpectrumFinance.NETWORK_ID;
 


    public ErgoMarkets(ErgoNetworkData ergNetData, ErgoNetwork ergoNetwork){
        super(new Image(getAppIconString()), NAME, App.MARKET_NETWORK, ergoNetwork);
        m_ergNetData = ergNetData;
        getData();
    }

    public Image getSmallAppIcon(){
        return new Image("/assets/bar-chart-30.png");
    }

    public static String getAppIconString(){
        return "/assets/bar-chart-150.png";
    }
    public Image getAppIcon(){
        return new Image("/assets/bar-chart-150.png");   
    }

    @Override
    public String getDescription(){
        return DESCRIPTION;
    }

    @Override
    public Object sendNote(JsonObject note){
        if(note != null){
            JsonElement subjecElement = note.get(App.CMD);
            JsonElement networkIdElement = note.get("networkId");

            if( subjecElement != null && subjecElement.isJsonPrimitive() && networkIdElement != null && networkIdElement.isJsonPrimitive()){
                String networkId = networkIdElement.getAsString();
            
                if(networkId.equals(m_ergNetData.getId())){
                    switch (subjecElement.getAsString()) {
                        case "getMarkets":
                            return getMarkets();
                        case "getMarketById":
                            return getMarketById(note);
                        case "getMarketByName":
                            return getMarketByName(note);
                    }
                }
            }
        }

        return null;
    }


    public String getDefaultMarketId() {
        return m_defaultMarketId;
    }

    public void setDefaultMarketId(String defaultMarketId) {
        this.m_defaultMarketId = defaultMarketId;
        save();
    }


      @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        JsonElement cmdElement = note != null ? note.get(App.CMD) : null;
        JsonElement idElement = note != null ? note.get("id") : null;
        

        if(cmdElement != null){
            String id = idElement != null ? idElement.getAsString() : getDefaultMarketId();
         
            
            NoteInterface marketData = getMarket(id);
        
            if(marketData != null){
               
                return marketData.sendNote(note, onSucceeded, onFailed);
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

    private JsonArray getTokenMarkets(){
    
        List<NoteInterface> list = getNetworksData().getAppsContainsAllKeyWords("ergo", "exchange", "usd", "ergo tokens");
            
        JsonArray jsonArray = new JsonArray();

         for ( NoteInterface data : list) {

            JsonObject result =  data.getJsonObject();
            jsonArray.add(result);

        }
    
        return jsonArray;
    }

    private JsonArray getMarkets(){
    
        List<NoteInterface> list = getNetworksData().getAppsContainsAllKeyWords("ergo", "exchange", "usd");
            
        JsonArray jsonArray = new JsonArray();

         for ( NoteInterface data : list) {

            JsonObject result =  data.getJsonObject();
            jsonArray.add(result);

        }
    
        return jsonArray;
    }

    private void getData(){
        JsonObject json = getNetworksData().getData("data", ".", App.MARKET_NETWORK, ErgoNetwork.NETWORK_ID);

        if(json != null){
            
            openJson(json);
        }
    }


    public void save() {
       
        getNetworksData().save("data", ".", App.MARKET_NETWORK, ErgoNetwork.NETWORK_ID, getJsonObject());
        
    }

 
    @Override
    public JsonObject getJsonObject() {
        JsonObject json = super.getJsonObject();
        json.addProperty("defaultMarketId", m_defaultMarketId);
        return json;
    }

    private void openJson(JsonObject json){

        JsonElement defaultMarketIdElement = json != null ? json.get("defaultMarketId") : null;

        m_defaultMarketId = defaultMarketIdElement != null ? defaultMarketIdElement.getAsString() : m_defaultMarketId;
      
        
    }
}
