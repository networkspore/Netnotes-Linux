package com.netnotes;

import java.time.LocalDateTime;
import java.util.ArrayList;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

public class ErgoExplorerList {
    private ErgoExplorers m_ergoExplorer = null;
    private final SimpleStringProperty m_defaultIdProperty = new SimpleStringProperty(null);
    private ArrayList<ErgoExplorerData> m_dataList = new ArrayList<>();
    
    private double m_stageWidth = 600;
    private double m_stageHeight = 500;
    private ChangeListener<LocalDateTime> m_updateListener = null;

    public ErgoExplorerList(ErgoExplorers ergoExplorer) {
        m_ergoExplorer = ergoExplorer;
        m_updateListener = (obs, oldval, newVal) -> save();
        getData();
       
    }

    public ErgoExplorers getErgoExplorer(){
        return m_ergoExplorer;
    }

    public ArrayList<ErgoExplorerData> getDataList(){
        return m_dataList;
    }

    public NoteInterface getExplorerById(JsonObject note){
        JsonElement idElement = note.get("id");

        if(idElement != null && idElement.isJsonPrimitive()){
            ErgoExplorerData explorerData = getErgoExplorerData(idElement.getAsString());
            return explorerData.getNoteInterface();
        }
        return null;
    }

    private void getData(){
        JsonObject json = m_ergoExplorer.getNetworksData().getData("data", ".", ErgoExplorers.NETWORK_ID, ErgoNetwork.NETWORK_ID);
        if(json != null){
            openJson(json);
        }else{
            setDefault();
        }
    }

    public void save() {
       
        m_ergoExplorer.getNetworksData().save("data", ".", ErgoExplorers.NETWORK_ID, ErgoNetwork.NETWORK_ID, getJsonObject());
        
    }

    private void setDefault(){

    }

    public int size(){
        return m_dataList.size();
    }


    public void openJson(JsonObject json){
        
        JsonElement dataElement = json.get("data");
        JsonElement stageElement = json.get("stage");
        JsonElement defaultIdElement = json.get("defaultId");
        String defaultId = defaultIdElement != null ? defaultIdElement.getAsString() : null;
        m_defaultIdProperty.set(defaultId);

        if (dataElement != null && dataElement.isJsonArray()) {
            com.google.gson.JsonArray dataArray = dataElement.getAsJsonArray();

            for (int i = 0; i < dataArray.size(); i++) {
                JsonElement dataItem = dataArray.get(i);
                JsonObject dataJson = dataItem != null && dataItem.isJsonObject() ? dataItem.getAsJsonObject() : null;
                
                if(dataJson != null){
                    JsonElement dataIdElement = dataJson.get("id");
                    if(dataIdElement != null && dataIdElement.isJsonPrimitive()){
                        String dataId = dataIdElement.getAsString();
                        if(getErgoExplorerData(dataId) == null){
                            switch(dataId){
                                case ErgoPlatformExplorerData.ERGO_PLATFORM_EXPLORER:
                                    
                                    ErgoPlatformExplorerData ergoExplorerData = new ErgoPlatformExplorerData(this);
                                    m_dataList.add(ergoExplorerData);
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (stageElement != null && stageElement.isJsonObject()) {
            JsonObject stageObject = stageElement.getAsJsonObject();
            JsonElement widthElement = stageObject.get("width");
            JsonElement heightElement = stageObject.get("height");

            m_stageWidth = widthElement != null && widthElement.isJsonPrimitive() ? widthElement.getAsDouble() : m_stageWidth;

            m_stageHeight = heightElement != null && heightElement.isJsonPrimitive() ? heightElement.getAsDouble() : m_stageHeight;
        }
    }

    public SimpleStringProperty defaultIdProperty(){
        return m_defaultIdProperty;
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
        if (id != null) {
        
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

    public JsonObject getStageJson() {
        JsonObject json = new JsonObject();
        json.addProperty("width", m_stageWidth);
        json.addProperty("height", m_stageHeight);
        return json;
    }

    public JsonObject getJsonObject() {
        JsonObject json = new JsonObject();

        json.add("data", getDataJsonArray());
        if(m_defaultIdProperty.get() != null){
            json.addProperty("defaultId", m_defaultIdProperty.get());
        }
        json.add("addStage", getStageJson());

       

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
