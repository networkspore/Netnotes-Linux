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

import javafx.stage.FileChooser;
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



    public Scene createMnemonicScene(String id, String name,  NetworkType networkType, Stage stage) {        //String oldStageName = mnemonicStage.getTitle();

        String titleStr = "Mnemonic phrase - " + m_ergoWallet.getName();

        stage.setTitle(titleStr);

        Button closeBtn = new Button();
        Button maximizeBtn = new Button();

        HBox titleBox = App.createTopBar(m_ergoWallet.getSmallAppIcon(), maximizeBtn, closeBtn, stage);

        //Region spacer = new Region();
        //HBox.setHgrow(spacer, Priority.ALWAYS);
     /*
        HBox menuBar = new HBox();
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(1, 0, 1, 5));

        VBox menuPaddingBox = new VBox(menuBar);
        menuPaddingBox.setPadding(new Insets(0, 2, 5, 2));
        */
        Text headingText = new Text("Mnemonic phrase");
        headingText.setFill(App.txtColor);
        headingText.setFont(App.txtFont);

        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(40);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 15, 10, 15));
        headingBox.setId("headingBox");

        VBox headerBox = new VBox(headingBox);
        headerBox.setPadding(new Insets(0, 5, 2, 5));

        TextArea mnemonicField = new TextArea(Mnemonic.generateEnglishMnemonic());
        mnemonicField.setFont(App.txtFont);
        mnemonicField.setId("textFieldCenter");
        mnemonicField.setEditable(false);
        mnemonicField.setWrapText(true);
        mnemonicField.setPrefRowCount(2);
        HBox.setHgrow(mnemonicField, Priority.ALWAYS);

        Platform.runLater(() -> mnemonicField.requestFocus());

        HBox mnemonicFieldBox = new HBox(mnemonicField);

        VBox mnemonicBox = new VBox(mnemonicFieldBox);
        mnemonicBox.setAlignment(Pos.CENTER);
        mnemonicBox.setPadding(new Insets(20, 30, 0, 30));

        Region hBar = new Region();
        hBar.setPrefWidth(400);
        hBar.setPrefHeight(2);
        hBar.setId("hGradient");

        HBox gBox = new HBox(hBar);
        gBox.setAlignment(Pos.CENTER);
        gBox.setPadding(new Insets(15, 0, 0, 0));

        Button nextBtn = new Button("Next");

        nextBtn.setFont(App.txtFont);

        HBox nextBox = new HBox(nextBtn);
        nextBox.setAlignment(Pos.CENTER);
        nextBox.setPadding(new Insets(25, 0, 0, 0));

        VBox bodyBox = new VBox(mnemonicBox, gBox, nextBox);
        VBox.setMargin(bodyBox, new Insets(5, 10, 0, 20));
        VBox.setVgrow(bodyBox, Priority.ALWAYS);

        VBox layoutVBox = new VBox(titleBox, headerBox, bodyBox);

        mnemonicFieldBox.setMaxWidth(900);
        HBox.setHgrow(mnemonicFieldBox, Priority.ALWAYS);

        Scene mnemonicScene = new Scene(layoutVBox, 600, 425);
        mnemonicScene.setFill(null);
        mnemonicScene.getStylesheets().add("/css/startWindow.css");

        mnemonicBox.prefHeightProperty().bind(mnemonicScene.heightProperty().subtract(titleBox.heightProperty()).subtract(headerBox.heightProperty()).subtract(130));

        closeBtn.setOnAction(e -> {
            mnemonicField.setText("");
            stage.close();

        });

 

        nextBtn.setOnAction(nxtEvent -> {
            Alert nextAlert = new Alert(AlertType.NONE, "This mnemonic phrase may be used to generate backup or to recover this wallet if it is lost. It is strongly recommended to always maintain a paper copy of this phrase in a secure location. \n\nWarning: Loss of your mnemonic phrase could lead to the loss of your ability to recover this wallet.", ButtonType.CANCEL, ButtonType.OK);
            nextAlert.setHeaderText("Notice");
            nextAlert.initOwner(stage);
            nextAlert.setTitle("Notice - Mnemonic phrase - Add wallet");
            Optional<ButtonType> result = nextAlert.showAndWait();

            if(result != null && result.isPresent() && result.get() == ButtonType.OK){

                 Button closePassBtn = new Button();
                Stage passwordStage = App.createPassword("Wallet password - " + ErgoWallets.NAME, m_ergoWallet.getSmallAppIcon(), m_ergoWallet.getAppIcon(), closePassBtn,m_ergoWallet.getNetworksData().getExecService(), onSuccess -> {
                    Object sourceObject = onSuccess.getSource().getValue();

                    if (sourceObject != null && sourceObject instanceof String) {

                        String password = (String) sourceObject;
                        if (!password.equals("")) {

                            FileChooser saveFileChooser = new FileChooser();
                          //  saveFileChooser.setInitialDirectory(getWalletsDirectory());
                            saveFileChooser.setTitle("Save: Wallet file");
                            saveFileChooser.getExtensionFilters().add(ErgoWallets.ergExt);
                            saveFileChooser.setSelectedExtensionFilter(ErgoWallets.ergExt);

                            File walletFile = saveFileChooser.showSaveDialog(stage);
                            int indexOfDecimal = walletFile != null ? walletFile.getName().lastIndexOf(".") : -1;
                            walletFile = walletFile != null ? (indexOfDecimal != -1 && walletFile.getName().substring(indexOfDecimal).equals("erg") ? walletFile : new File(walletFile.getAbsolutePath() + ".erg")) : null;
                            if (walletFile != null) {
                                

                                Wallet.create(walletFile.toPath(), Mnemonic.create(SecretString.create(mnemonicField.getText()), SecretString.create(password)), walletFile.getName(), password.toCharArray());
                                mnemonicField.setText("-");
                                String configId = FriendlyId.createFriendlyId();
                                ErgoWalletData walletData = new ErgoWalletData(id,configId, name, walletFile, networkType, m_ergoWallet);
                                add(walletData, true);
                                
                            }
                  
                            
                            closePassBtn.fire();
                            stage.close();
                        }else{
                            Alert a = new Alert(AlertType.NONE, "Enter a password for the wallet file.", ButtonType.OK);
                            a.setTitle("Password Required");
                            a.setHeaderText("Password Required");
                            a.show();
                        }

                    }else{
                        closePassBtn.fire();
                    }
                    
                });
                closePassBtn.setOnAction(e->{
                    passwordStage.close();
             
                });
                passwordStage.show();

                Platform.runLater(()->{
                    passwordStage.toBack();
                    Platform.runLater(()->{
                        passwordStage.toFront();
                        Platform.runLater(()->passwordStage.requestFocus());
                    });
                });
               
                
                
            }
        });

        return mnemonicScene;
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

            File file = new File(path);
            
            NoteInterface existingWalletData = getWalletByPath(file.getAbsolutePath());

            if(existingWalletData != null){
                return existingWalletData;
            }

            m_tmpStr = "Wallet #" + FriendlyId.createFriendlyId();

            while(containsName(m_tmpStr)){
                m_tmpStr = "Wallet #" + FriendlyId.createFriendlyId();
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

        return null;
    }


    public void sendNoteToNetworkId(JsonObject note, String networkId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        m_walletDataList.forEach(walletData -> {
            if (walletData.getNetworkId().equals(networkId)) {

                walletData.sendNote(note, onSucceeded, onFailed);
            }
        });
    }

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
