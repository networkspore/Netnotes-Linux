package com.netnotes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javafx.beans.value.ChangeListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

public class ErgoExplorerList {
    private ErgoExplorers m_ergoExplorer = null;
    private HashMap<String,ErgoExplorerData> m_dataList = new HashMap<>();
    

    private ChangeListener<LocalDateTime> m_updateListener = null;
    private ErgoNetworkData m_ergoNetworkData;
    private String m_defaultExplorerId = ErgoPlatformExplorerData.ERGO_PLATFORM_EXPLORER;

    public ErgoExplorerList(ErgoExplorers ergoExplorer, ErgoNetworkData ergoNetworkData) {
        m_ergoExplorer = ergoExplorer;
        m_updateListener = (obs, oldval, newVal) -> save();
        m_ergoNetworkData = ergoNetworkData;
        getData();

        
    }

    public String getDefaultExplorerId(){
        return m_defaultExplorerId;
    }

    public void setDefaultExplorerId(String id, boolean isSave){
        m_defaultExplorerId = id;

        
       
        

        if(isSave){
            
             long timeStamp = System.currentTimeMillis();
            save();
           
            JsonObject note = Utils.getJsonObject("networkId", ErgoNetwork.EXPLORER_NETWORK);
            if(id != null){
                ErgoExplorerData explorerData = getErgoExplorerData(id);
                note.addProperty("id",  id);
                note.addProperty("name", explorerData.getName());
            }
            note.addProperty("code", App.LIST_DEFAULT_CHANGED);
            note.addProperty("timeStamp", timeStamp);
            
            getErgoNetwork().sendMessage(App.LIST_DEFAULT_CHANGED, timeStamp, ErgoNetwork.EXPLORER_NETWORK, note.toString());
        }
        
    }

    public Boolean clearDefault(JsonObject note){

        m_defaultExplorerId = null;
        long timeStamp = System.currentTimeMillis();
        
        JsonObject msg = Utils.getJsonObject("networkId", ErgoNetwork.EXPLORER_NETWORK);
        msg.addProperty("code", App.LIST_DEFAULT_CHANGED);
        msg.addProperty("timeStamp", timeStamp);
        getErgoNetwork().sendMessage(App.LIST_DEFAULT_CHANGED, timeStamp, ErgoNetwork.EXPLORER_NETWORK, msg.toString());
        

        return true;
    }

    public JsonObject setDefault(JsonObject note){
        JsonElement idElement = note != null ? note.get("id") : null;
        if(idElement != null){
            String defaultId = idElement.getAsString();
            ErgoExplorerData explorerData = getErgoExplorerData(defaultId);

          
            if(explorerData != null){
                setDefaultExplorerId(defaultId, true);
                return explorerData.getJsonObject();
            }
        }
        return null;
    }

    public JsonObject getDefault(JsonObject note){
        ErgoExplorerData explorerData = getErgoExplorerData(getDefaultExplorerId());

        return explorerData != null ? explorerData.getJsonObject() : null;
    }

    public NoteInterface getDefaultInterface(){
        ErgoExplorerData explorerData = getErgoExplorerData(getDefaultExplorerId());
        
        if(explorerData != null){
            return explorerData.getNoteInterface();
        }

        return null;
    }

    public ErgoExplorers getErgoExplorer(){
        return m_ergoExplorer;
    }

    public ErgoNetworkData getErgoNetworkData(){
        return m_ergoNetworkData;
    }



    public JsonObject getExplorerById(JsonObject note){
        JsonElement idElement = note.get("id");

        if(idElement != null && idElement.isJsonPrimitive()){
            String id = idElement.getAsString() ;
        
            ErgoExplorerData explorerData = getErgoExplorerData(id);
            return explorerData.getJsonObject();
        }
        return null;
    }


    private void getData(){
        JsonObject json = m_ergoExplorer.getNetworksData().getData("data", ".", ErgoNetwork.EXPLORER_NETWORK, ErgoNetwork.NETWORK_ID);
      
        if(json != null){
            
            openJson(json);
        }else{
            addDefault();
        }
    }

    public void save() {
       
        m_ergoExplorer.getNetworksData().save("data", ".", ErgoNetwork.EXPLORER_NETWORK, ErgoNetwork.NETWORK_ID, getJsonObject());
        
    }

    private void addDefault(){
        
        ErgoPlatformExplorerData ergoExplorerData = new ErgoPlatformExplorerData(this);
        
        m_dataList.put(ergoExplorerData.getId(), ergoExplorerData);
        save();
    }

    public int size(){
        return m_dataList.size();
    }


    public void openJson(JsonObject json){
        
        JsonElement dataElement = json.get("data");

        if (dataElement != null && dataElement.isJsonArray()) {
            com.google.gson.JsonArray dataArray = dataElement.getAsJsonArray();

            for (int i = 0; i < dataArray.size(); i++) {
                JsonElement dataItem = dataArray.get(i);
                JsonObject explorerJson = dataItem != null && dataItem.isJsonObject() ? dataItem.getAsJsonObject() : null;
                
                if(explorerJson != null){
                    JsonElement explorerIdElement = explorerJson.get("explorerId");
                    if(explorerIdElement != null && explorerIdElement.isJsonPrimitive()){
                        String explorerId = explorerIdElement.getAsString();
                        if(getErgoExplorerData(explorerId) == null){
                            ErgoExplorerData explorerData = null;
                            switch(explorerId){
                                case ErgoPlatformExplorerData.ERGO_PLATFORM_EXPLORER:
                                    explorerData = new ErgoPlatformExplorerData(this);
                                    break;
                                default:
                                    try{
                                        explorerData = new ErgoExplorerData(explorerId, explorerJson, this);
                                    }catch(Exception e){
                                       try {
                                            Files.writeString(App.logFile.toPath(), "\nErgoExplorerList cannot open data: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                        } catch (IOException e1) {

                                        }
                                    }
                            }
                            
                            if(explorerData != null){
                                m_dataList.put(explorerData.getId(), explorerData);
                            }
                            
                        }
                        
                    }
                }
            }
        }
        

        JsonElement explorerIdElement = json != null ? json.get("defaultId") : null;
        setDefaultExplorerId( explorerIdElement != null && explorerIdElement.isJsonPrimitive() ? explorerIdElement.getAsString() : null, false);
      

        
    }

 
 
    public void add(ErgoExplorerData explorerData){
        add(explorerData, true);
    }

    public void add(ErgoExplorerData ergoExplorerData, boolean doSave) {
        if (ergoExplorerData != null) {
            m_dataList.put(ergoExplorerData.getId(), ergoExplorerData);
       
            ergoExplorerData.addUpdateListener(m_updateListener);
            if (doSave) {
                long timeStamp = System.currentTimeMillis();
                save();
                JsonObject note = Utils.getJsonObject("networkId", ErgoNetwork.EXPLORER_NETWORK);
                note.addProperty("id",  ergoExplorerData.getId());
                note.addProperty("name", ergoExplorerData.getName());
                note.addProperty("code", App.LIST_ITEM_ADDED);
                note.addProperty("timeStamp", timeStamp);
                
                getErgoNetwork().sendMessage(App.LIST_ITEM_ADDED, timeStamp, ErgoNetwork.EXPLORER_NETWORK, note.toString());
            }
        }
    }

    public ErgoNetwork getErgoNetwork(){
        return m_ergoNetworkData.getErgoNetwork();
    }

    public boolean remove(String id, boolean doSave){
        if (id != null) {
            ErgoExplorerData explorerData = m_dataList.remove(id);
            if (explorerData != null) {
                
                if(doSave){
                    explorerData.removeUpdateListener();
                    save();

                    JsonObject note = Utils.getJsonObject("networkId", ErgoNetwork.EXPLORER_NETWORK);
                    note.addProperty("id",  explorerData.getId());
                    note.addProperty("code", App.LIST_ITEM_REMOVED);
                            
                    long timeStamp = System.currentTimeMillis();
                    note.addProperty("timeStamp", timeStamp);
                    
                    getErgoNetwork().sendMessage(App.LIST_ITEM_REMOVED, timeStamp, ErgoNetwork.EXPLORER_NETWORK, note.toString());
                }
                return true;
            }
            
        }
        return false;
    }

    public ErgoExplorerData getErgoExplorerData(String id) {
        if (id != null && m_dataList != null) {
       
            ErgoExplorerData ergoExplorerData = m_dataList.get(id);
            
            return ergoExplorerData;     
        }
        return null;
    }

     private JsonArray getDataJsonArray() {
        JsonArray jsonArray = new JsonArray();
     
        for (Map.Entry<String, ErgoExplorerData> entry : m_dataList.entrySet()) {
            ErgoExplorerData data = entry.getValue();
            JsonObject jsonObj = data.getJsonObject();
            jsonArray.add(jsonObj);

        }
        return jsonArray;
    }



    public JsonObject getJsonObject() {
        JsonObject json = new JsonObject();
        if(m_dataList.size() > 0){
            json.addProperty("defaultId", getDefaultExplorerId());
            json.add("data", getDataJsonArray());
        }

        return json;
    }


    public JsonArray getExplorers(){
        JsonArray jsonArray = new JsonArray();

        for (Map.Entry<String, ErgoExplorerData> entry : m_dataList.entrySet()) {
            
            ErgoExplorerData data = entry.getValue();
            JsonObject jsonObj = Utils.getJsonObject("name", data.getName());
            jsonObj.addProperty("id", data.getId());
            jsonObj.addProperty("isDefault", getDefaultExplorerId() != null ? data.getId().equals(getDefaultExplorerId()) : false);
            jsonArray.add(jsonObj);

        }
        return jsonArray;
    }


}
