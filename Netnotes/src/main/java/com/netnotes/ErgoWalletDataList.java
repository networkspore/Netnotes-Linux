package com.netnotes;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.ergoplatform.appkit.Mnemonic;
import org.ergoplatform.appkit.MnemonicValidationException;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.SecretString;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.satergo.Wallet;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ErgoWalletDataList {

    private ArrayList<ErgoWalletData> m_walletDataList = new ArrayList<>();
    private ErgoWallets m_ergoWallet;
    private SimpleDoubleProperty m_gridWidth;
    private SimpleStringProperty m_iconStyle;


    public ErgoWalletDataList(ErgoWallets ergoWallet) {

        m_ergoWallet = ergoWallet;
        getData();
    }


    public SimpleDoubleProperty gridWidthProperty() {
        return m_gridWidth;
    }

    public SimpleStringProperty iconStyleProperty() {
        return m_iconStyle;
    }

    private void getData(){
        
        openJson(m_ergoWallet.getNetworksData().getData("data", ".list", ErgoWallets.NETWORK_ID, ErgoNetwork.NETWORK_ID));
    }

    public void openJson(JsonObject json) {
        if (json != null) {
            
            m_walletDataList.clear();
            JsonElement walletsElement = json.get("wallets");

    
            if (walletsElement != null && walletsElement.isJsonArray()) {
                JsonArray jsonArray = walletsElement.getAsJsonArray();

                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonElement jsonElement = jsonArray.get(i);

                    if (jsonElement != null && jsonElement.isJsonObject()) {
                        JsonObject jsonObject = jsonElement.getAsJsonObject();

                        if (jsonObject != null) {
                            JsonElement fileElement = jsonObject.get("file");
                            JsonElement nameElement = jsonObject.get("name");
                            JsonElement idElement = jsonObject.get("id");
                            JsonElement networkTypeElement = jsonObject.get("networkType");
                        
                            String id = idElement == null ? FriendlyId.createFriendlyId() : idElement.getAsString();
                            String name = nameElement == null ? "Wallet " + id : nameElement.getAsString();
                            String fileString = fileElement != null && fileElement.isJsonPrimitive() ? fileElement.getAsString() : null;
                            NetworkType networkType = networkTypeElement == null ? NetworkType.MAINNET : networkTypeElement.getAsString().equals(NetworkType.TESTNET.toString()) ? NetworkType.TESTNET : NetworkType.MAINNET;
                            
                            File file = fileString != null ? new File(fileString) : null;


                            ErgoWalletData walletData = new ErgoWalletData(id,FriendlyId.createFriendlyId(), name, file,  networkType, m_ergoWallet);
                            
                            add(walletData, false);

                            
                        }
                    }
                }

            }
           
        }
    }

    public void save(){
        m_ergoWallet.getNetworksData().save("data", ".list", ErgoWallets.NETWORK_ID, ErgoNetwork.NETWORK_ID, getJsonObject());
    }

    public void add(ErgoWalletData walletData, boolean isSave) {
        m_walletDataList.add(walletData);
        walletData.addUpdateListener((obs,oldval,newval)->{
            m_ergoWallet.sendMessage(App.UPDATED, System.currentTimeMillis(), walletData.getNetworkId());
            save();
        }); 

        

        if(isSave){
            save();
            m_ergoWallet.sendMessage(App.LIST_ITEM_ADDED, System.currentTimeMillis(), walletData.getNetworkId());
        }
    }

    public void remove(String id, boolean isSave) {
        for (int i =0; i< m_walletDataList.size(); i++) {
            ErgoWalletData walletData = m_walletDataList.get(i);
            if (walletData.getNetworkId().equals(id)) {
                walletData.removeUpdateListener();
                walletData.shutdown();
                m_walletDataList.remove(i);
               
                if(isSave){
                    save();
                    m_ergoWallet.sendMessage(App.LIST_ITEM_REMOVED, System.currentTimeMillis(), walletData.getNetworkId());
                }
                break;
            }
        }
    }

    public NoteInterface getWalletByName(JsonObject note) {
        JsonElement nameElement = note.get("name");

        if(nameElement != null && nameElement.isJsonPrimitive()){
            return getWalletByName(nameElement.getAsString());
        }
        
        return null;
    }
    
    public NoteInterface getWalletByName(String name) {
        
        for (ErgoWalletData walletData : m_walletDataList) {
            if(name.equals(walletData.getName())){
                return walletData.getNoteInterface();
            }
        }
        return null;
    }

    public NoteInterface getWalletByPath(String path) {
        
        for (ErgoWalletData walletData : m_walletDataList) {
            if(path.equals(walletData.getWalletPath())){
                return walletData.getNoteInterface();
            }
        }
        return null;
    }

    public boolean containsName(String name) {
        for (ErgoWalletData walletData : m_walletDataList) {
          
            if(walletData.getName().equals(name)){
                return true;
            }
        }
        return false;
    }



 


    public String restoreMnemonicStage() {
        String titleStr = m_ergoWallet.getName() + " - Restore wallet: Mnemonic phrase";

        Stage mnemonicStage = new Stage();

        mnemonicStage.setTitle(titleStr);

        mnemonicStage.getIcons().add(m_ergoWallet.getIcon());

        mnemonicStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(m_ergoWallet.getIcon(), titleStr, closeBtn, mnemonicStage);

        Button imageButton = App.createImageButton(m_ergoWallet.getIcon(), "Restore wallet");

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

        Text subTitleTxt = new Text("> Mnemonic phrase:");
        subTitleTxt.setFill(App.txtColor);
        subTitleTxt.setFont(App.txtFont);

        HBox subTitleBox = new HBox(subTitleTxt);
        subTitleBox.setAlignment(Pos.CENTER_LEFT);

        TextArea mnemonicField = new TextArea();
        mnemonicField.setFont(App.txtFont);
        mnemonicField.setId("formField");

        mnemonicField.setWrapText(true);
        mnemonicField.setPrefRowCount(2);
        HBox.setHgrow(mnemonicField, Priority.ALWAYS);

        Platform.runLater(() -> mnemonicField.requestFocus());

        HBox mnemonicBox = new HBox(mnemonicField);
        mnemonicBox.setPadding(new Insets(20, 30, 0, 30));

        Region hBar = new Region();
        hBar.setPrefWidth(400);
        hBar.setPrefHeight(2);
        hBar.setId("hGradient");

        HBox gBox = new HBox(hBar);
        gBox.setAlignment(Pos.CENTER);
        gBox.setPadding(new Insets(15, 0, 0, 0));

        Button nextBtn = new Button("Words left: 15");
        nextBtn.setId("toolBtn");
        nextBtn.setFont(App.txtFont);
        nextBtn.setDisable(true);
        nextBtn.setOnAction(nxtEvent -> {
            String mnemonicString = mnemonicField.getText();;

            String[] words = mnemonicString.split("\\s+");

            List<String> mnemonicList = Arrays.asList(words);
            try {
                Mnemonic.checkEnglishMnemonic(mnemonicList);
                mnemonicStage.close();
            } catch (MnemonicValidationException e) {
                Alert a = new Alert(AlertType.NONE, "Error: Mnemonic invalid\n\nPlease correct the mnemonic phrase and try again.", ButtonType.CLOSE);
                a.initOwner(mnemonicStage);
                a.setTitle("Error: Mnemonic invalid.");
            }

        });

        mnemonicField.setOnKeyPressed(e1 -> {
            String mnemonicString = mnemonicField.getText();;

            String[] words = mnemonicString.split("\\s+");
            int numWords = words.length;
            if (numWords == 15) {
                nextBtn.setText("Ok");

                List<String> mnemonicList = Arrays.asList(words);
                try {
                    Mnemonic.checkEnglishMnemonic(mnemonicList);
                    nextBtn.setDisable(false);

                } catch (MnemonicValidationException e) {
                    nextBtn.setText("Invalid");
                    nextBtn.setId("toolBtn");
                    nextBtn.setDisable(true);
                }

            } else {
                if (nextBtn.getText().equals("")) {
                    nextBtn.setText("Words left: 15");
                } else {
                    nextBtn.setText("Words left: " + (15 - numWords));
                }

                nextBtn.setId("toolBtn");
                nextBtn.setDisable(true);
            }

        });

        HBox nextBox = new HBox(nextBtn);
        nextBox.setAlignment(Pos.CENTER);
        nextBox.setPadding(new Insets(25, 0, 0, 0));

        VBox bodyBox = new VBox(subTitleBox, mnemonicBox, gBox, nextBox);
        VBox.setMargin(bodyBox, new Insets(5, 10, 0, 20));
        VBox.setVgrow(bodyBox, Priority.ALWAYS);

        VBox layoutVBox = new VBox(titleBox, imageBox, bodyBox);

        Scene mnemonicScene = new Scene(layoutVBox, 600, 425);
        mnemonicScene.setFill(null);
        mnemonicScene.getStylesheets().add("/css/startWindow.css");
        mnemonicStage.setScene(mnemonicScene);

        closeBtn.setOnAction(e -> {

            mnemonicStage.close();
            mnemonicField.setText("");

        });

        mnemonicStage.showAndWait();

        return mnemonicField.getText();

    }

    public Object getWalletById(JsonObject json){
        JsonElement idElement = json.get("id");

        if(idElement != null && idElement.isJsonPrimitive()){
            ErgoWalletData walletData = getWallet(idElement.getAsString());
            return walletData.getNoteInterface();
        }

        return null;
    }

    private ErgoWalletData getWallet(String id){
 
        for (ErgoWalletData walletData : m_walletDataList) {
            if (walletData.getNetworkId().equals(id)) {
                return walletData;
            }
        }
        return null;
    }
    private String m_tmpStr = "";

    public Object openWallet(JsonObject note){

        JsonElement pathElement = note.get("path");
        JsonElement networkTypeElement = note.get("networkType");

        if( pathElement != null && pathElement.isJsonPrimitive()){
            
            String path = pathElement.getAsString();

            if(Utils.findPathPrefixInRoots(path)){

                File file = new File(path);
                

                NoteInterface existingWalletData = getWalletByPath(file.getAbsolutePath());

                if(existingWalletData != null){
                    return existingWalletData.getJsonObject();
                }

                

                m_tmpStr = file.getName();

                if(!m_tmpStr.equals("")){

                    m_tmpStr = m_tmpStr.endsWith(".erg") ? m_tmpStr.substring(0, m_tmpStr.length()-4) : m_tmpStr;
                    
                    int i = 1;
                    while(containsName(m_tmpStr)){
                        m_tmpStr =  m_tmpStr + " #" + i;
                        i++;
                    }
                    String name = m_tmpStr;
                    

                    m_tmpStr = FriendlyId.createFriendlyId();

                    while(getWallet(m_tmpStr) != null){
                        m_tmpStr = FriendlyId.createFriendlyId(); 
                    }

                    String id = m_tmpStr;
                    m_tmpStr = null;
                    
                    String configId = FriendlyId.createFriendlyId();
                    NetworkType networkType = networkTypeElement != null && networkTypeElement.isJsonPrimitive() && networkTypeElement.getAsString().equals(NetworkType.TESTNET.toString()) ? NetworkType.TESTNET : NetworkType.MAINNET;
                    ErgoWalletData walletData = new ErgoWalletData(id, configId, name, file, networkType, m_ergoWallet);
                    add(walletData, true);

                    JsonObject json = walletData.getJsonObject();
                    json.addProperty("configId", configId);
                    return json;
                }
            }
            
        }

        return null;
    }


    /*public void sendNoteToNetworkId(JsonObject note, String networkId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        m_walletDataList.forEach(walletData -> {
            if (walletData.getNetworkId().equals(networkId)) {

                walletData.sendNote(note, onSucceeded, onFailed);
            }
        });
    }*/

    public int size() {
        return m_walletDataList.size();
    }



    public void getMenu(MenuButton menuBtn, SimpleObjectProperty<ErgoWalletData> selected){

        menuBtn.getItems().clear();
        ErgoWalletData newSelectedWallet =  selected.get();

        MenuItem noneMenuItem = new MenuItem("(disabled)");
        if(selected.get() == null){
            noneMenuItem.setId("selectedMenuItem");
        }
        noneMenuItem.setOnAction(e->{
            selected.set(null);
        });
        menuBtn.getItems().add(noneMenuItem);

        int numCells = m_walletDataList.size();

        for (int i = 0; i < numCells; i++) {
            
            ErgoWalletData walletData = (ErgoWalletData) m_walletDataList.get(i);

                MenuItem menuItem = new MenuItem(walletData.getName());
            if(newSelectedWallet != null && newSelectedWallet.getNetworkId().equals(walletData.getNetworkId())){
                menuItem.setId("selectedMenuItem");
                //  menuItem.setText(menuItem.getText());
            }
            menuItem.setOnAction(e->{
                
                selected.set(walletData);
            });

            menuBtn.getItems().add(menuItem);
        }


    

    }


    public void shutdown(){
        
        m_walletDataList.forEach(item->shutdown());
    }

    public JsonArray getWallets(){

        JsonArray jsonArray = new JsonArray();

        for (ErgoWalletData walletData : m_walletDataList) {
            JsonObject result =  walletData.getWallet();
            jsonArray.add(result);

        }
        return jsonArray;
    }

    private JsonArray getWalletsJsonArray() {
        JsonArray jsonArray = new JsonArray();

        for (NoteInterface walletData : m_walletDataList) {

            JsonObject jsonObj = walletData.getJsonObject();
            jsonArray.add(jsonObj);

        }
        return jsonArray;
    }


    private JsonObject getJsonObject(){
        JsonObject fileObject = new JsonObject();
        fileObject.add("wallets", getWalletsJsonArray());
        return fileObject;
    }
    
}
