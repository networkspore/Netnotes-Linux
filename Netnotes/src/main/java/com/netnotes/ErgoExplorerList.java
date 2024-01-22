package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import com.utils.Utils;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ErgoExplorerList {
    private File logFile = new File("netnotes-log.txt");
    private ErgoExplorers m_ergoExplorer = null;
    private final SimpleStringProperty m_defaultIdProperty = new SimpleStringProperty(null);
    private ArrayList<ErgoExplorerData> m_dataList = new ArrayList<>();
    private SimpleObjectProperty<LocalDateTime> m_doGridUpdate = new SimpleObjectProperty<LocalDateTime>(null);
    private final SimpleStringProperty m_selectedIdProperty = new SimpleStringProperty(null);
    
    private double m_stageWidth = 600;
    private double m_stageHeight = 500;
    private ChangeListener<LocalDateTime> m_nodeUpdateListener = (obs, oldval, newVal) -> save();

    public ErgoExplorerList(ErgoExplorers ergoExplorer) {
        m_ergoExplorer = ergoExplorer;
       
        readFile();
        m_ergoExplorer.getNetworksData().getAppData().appKeyProperty().addListener((obs, oldVal, newVal) -> save());
    }

    private void readFile(){
        SecretKey secretKey = m_ergoExplorer.getNetworksData().getAppData().appKeyProperty().get();
        File dataFile = m_ergoExplorer.getDataFile();
   
        if (dataFile != null && dataFile.isFile()) {
            try {
                openJson(Utils.readJsonFile(secretKey, dataFile.toPath()));
            } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                    | IOException e) {

            }

        }else{
            String friendlyId = FriendlyId.createFriendlyId();
            m_defaultIdProperty.set(friendlyId);
            m_dataList.add(new ErgoExplorerData(friendlyId, this));
            save();
        }
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
                try{
                    ErgoExplorerData ergoExplorerData = new ErgoExplorerData(dataItem.getAsJsonObject(), this);
                    m_dataList.add(ergoExplorerData);
                }catch(Exception e){
                    try {
                        Files.writeString(logFile.toPath(), "\nErgoExplorerList: " + e.toString());
                    } catch (IOException e1) {
                        
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

    public SimpleStringProperty selectedIdProperty(){
        return m_selectedIdProperty;
    }

    public void add(ErgoExplorerData explorerData){
        add(explorerData, true);
    }

    public void add(ErgoExplorerData ergoExplorerData, boolean doSave) {
        if (ergoExplorerData != null) {
            m_dataList.add(ergoExplorerData);
       
            ergoExplorerData.addUpdateListener(m_nodeUpdateListener);
            if (doSave) {
                save();
            }
        }
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


   public void save() {
        
        File appDir = m_ergoExplorer.getAppDir();
        File dataFile = m_ergoExplorer.getDataFile();
        try {

            JsonObject fileJson = getJsonObject();
            String jsonString = fileJson.toString();

            if (!appDir.isDirectory()) {
                Files.createDirectory(appDir.toPath());
            }

            Utils.writeEncryptedString(m_ergoExplorer.getNetworksData().getAppData().appKeyProperty().get(), dataFile, jsonString);
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
                | IOException e) {
            try {
                Files.writeString(logFile.toPath(), "\nErgoExplorerList: " + e.toString());
            } catch (IOException e1) {

            }
        }
    }

    public VBox getGridBox(SimpleDoubleProperty width, SimpleDoubleProperty scrollWidth){
        VBox gridBox = new VBox();

        Runnable updateGrid = () -> {
            gridBox.getChildren().clear();

            int numCells = m_dataList.size();

            for (int i = 0; i < numCells; i++) {
                ErgoExplorerData explorerData = m_dataList.get(i);
                HBox rowItem = explorerData.getRowItem();
                rowItem.prefWidthProperty().bind(width.subtract(scrollWidth));
                gridBox.getChildren().add(rowItem);
            }

        };

        updateGrid.run();

        m_doGridUpdate.addListener((obs, oldval, newval) -> updateGrid.run());

        return gridBox;
    }

    public void getMenu(MenuButton menuBtn, SimpleObjectProperty<ErgoExplorerData> selectedExplorer){

        Runnable updateMenu = () -> {
            menuBtn.getItems().clear();
            ErgoExplorerData selectedExplorerData = selectedExplorer.get();
            MenuItem noneMenuItem = new MenuItem("(disabled)");
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
                
                MenuItem menuItem = new MenuItem( explorerData.getName() + (selectedExplorerData != null && selectedExplorerData.getId().equals(explorerData.getId()) ? " (selected)" : ""));
                if(selectedExplorerData != null && selectedExplorerData.getId().equals(explorerData.getId())){
                    menuItem.setId("selectedMenuItem");
                }
                menuItem.setOnAction(e->{
                    selectedExplorer.set(explorerData);
                });

                menuBtn.getItems().add(menuItem);
            }


        };

        updateMenu.run();

        selectedExplorer.addListener((obs,oldval, newval)->updateMenu.run());

        m_doGridUpdate.addListener((obs, oldval, newval) -> updateMenu.run());
     
    }

}
