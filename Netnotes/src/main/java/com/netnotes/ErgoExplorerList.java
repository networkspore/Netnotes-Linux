package com.netnotes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ErgoExplorerList {
    private ErgoExplorers m_ergoExplorer = null;
    private ArrayList<ErgoExplorerData> m_dataList = new ArrayList<>();
    

    private ChangeListener<LocalDateTime> m_updateListener = null;
    private ErgoNetworkData m_ergoNetworkData;

    public ErgoExplorerList(ErgoExplorers ergoExplorer, ErgoNetworkData ergoNetworkData) {
        m_ergoExplorer = ergoExplorer;
        m_updateListener = (obs, oldval, newVal) -> save();
        m_ergoNetworkData = ergoNetworkData;
        getData();

       
    }

    public ErgoExplorers getErgoExplorer(){
        return m_ergoExplorer;
    }

    public ErgoNetworkData getErgoNetworkData(){
        return m_ergoNetworkData;
    }

    public ArrayList<ErgoExplorerData> getDataList(){
        return m_dataList;
    }

    public NoteInterface getExplorerById(JsonObject note){
        JsonElement idElement = note.get("id");

        if(idElement != null && idElement.isJsonPrimitive()){
            String id = idElement.getAsString() ;
        
            ErgoExplorerData explorerData = getErgoExplorerData(id);
            return explorerData.getNoteInterface();
        }
        return null;
    }

    private void getData(){
        JsonObject json = m_ergoExplorer.getNetworksData().getData("data", ".", ErgoExplorers.NETWORK_ID, ErgoNetwork.NETWORK_ID);
        JsonElement dataElement = json != null ? json.get("data") : null;
        if(dataElement != null){
            
            openJson(json);
        }else{
            setDefault();
        }
    }

    public void save() {
       
        m_ergoExplorer.getNetworksData().save("data", ".", ErgoExplorers.NETWORK_ID, ErgoNetwork.NETWORK_ID, getJsonObject());
        
    }

    private void setDefault(){
        
        ErgoPlatformExplorerData ergoExplorerData = new ErgoPlatformExplorerData(this);
        
        m_dataList.add(ergoExplorerData);
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
                            ErgoExplorerData explorerData;
                            switch(explorerId){
                                case ErgoPlatformExplorerData.ERGO_PLATFORM_EXPLORER:
                                    explorerData = new ErgoPlatformExplorerData(this);
                                    break;
                                default:
                                    try{
                                        explorerData = new ErgoExplorerData(explorerId, explorerJson, this);
                                    }catch(Exception e){
                                        explorerData = new ErgoPlatformExplorerData(this);
                                    }
                            }
                          
                            m_dataList.add(explorerData);
                            
                            
                        }
                        
                    }
                }
            }
        }

        
    }

 
 
    public void add(ErgoExplorerData explorerData){
        add(explorerData, true);
    }

    public void add(ErgoExplorerData ergoExplorerData, boolean doSave) {
        if (ergoExplorerData != null) {
            m_dataList.add(ergoExplorerData);
       
            ergoExplorerData.addUpdateListener(m_updateListener);
            if (doSave) {
                save();
            }
        }
    }

    public boolean remove(String id, boolean doSave){
        if (id != null) {
        
            for (int i = 0; i < m_dataList.size(); i++) {
                
                if (m_dataList.get(i).getId().equals(id)) {
                    m_dataList.remove(i);
                    if(doSave){
                        save();
                    }
                    return true;
                }
            }
            
        }
        return false;
    }

    public ErgoExplorerData getErgoExplorerData(String id) {
        if (id != null && m_dataList != null) {
       
           
            for (int i = 0; i < m_dataList.size(); i++) {
                ErgoExplorerData ergoExplorerData = m_dataList.get(i);
                     
                if (ergoExplorerData.getId().equals(id)) {
                    return ergoExplorerData;
                }
            }
            
        }
        return null;
    }

     public JsonArray getDataJsonArray() {
        JsonArray jsonArray = new JsonArray();

        for (ErgoExplorerData data : m_dataList) {

            JsonObject jsonObj = data.getJsonObject();
            jsonArray.add(jsonObj);

        }
        return jsonArray;
    }



    public JsonObject getJsonObject() {
        JsonObject json = new JsonObject();
        if(m_dataList.size() > 0){
            json.add("data", getDataJsonArray());
        }

        return json;
    }


   

   

    public void getMenu(MenuButton menuBtn, SimpleObjectProperty<ErgoExplorerData> selectedExplorer){

    
        menuBtn.getItems().clear();

        
        MenuItem openItem = new MenuItem("(open)");
        openItem.setOnAction(e->{
          
        });
        menuBtn.getItems().add(openItem);
        
        ImageView noneImgView = new ImageView();
        noneImgView.setImage(new Image("/assets/cloud-offline-30.png"));
        noneImgView.setPreserveRatio(true);
        
        ErgoExplorerData selectedExplorerData = selectedExplorer.get();
        MenuItem noneMenuItem = new MenuItem("(disabled)");
        noneMenuItem.setGraphic(noneImgView);
        if(selectedExplorerData == null){
            noneMenuItem.setId("selectedMenuItem");
        }

        noneMenuItem.setOnAction(e->{
            selectedExplorer.set(null);
        });
        menuBtn.getItems().add(noneMenuItem);
    

        int numCells = m_dataList.size();

        for (int i = 0; i < numCells; i++) {
            
            ErgoExplorerData explorerData = m_dataList.get(i);
            ImageView itemImageView = new ImageView();
            itemImageView.setImage(new Image(explorerData.getImgUrl()));
            itemImageView.setPreserveRatio(true);
            itemImageView.setFitWidth(25);

            MenuItem menuItem = new MenuItem( explorerData.getName() );
            menuItem.setGraphic(itemImageView);
            if(selectedExplorerData != null && selectedExplorerData.getId().equals(explorerData.getId())){
                menuItem.setId("selectedMenuItem");
            }
            menuItem.setOnAction(e->{
                selectedExplorer.set(explorerData);
            });

            menuBtn.getItems().add(menuItem);
        }




     
    }

}
