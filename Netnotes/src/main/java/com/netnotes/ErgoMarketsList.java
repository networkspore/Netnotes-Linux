package com.netnotes;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ErgoMarketsList {

    
    String[] MARKET_OPTIONS_AVAILABLE = new String[] { 
        KucoinExchange.NETWORK_ID, 
        SpectrumFinance.NETWORK_ID
    };


    private File logFile = new File("netnotes-log.txt");
    private String m_id = FriendlyId.createFriendlyId();
    private ArrayList<ErgoMarketsData> m_dataList = new ArrayList<>();
    private ErgoMarkets m_ergoMarkets;
    private SimpleObjectProperty<LocalDateTime> m_doGridUpdate = new SimpleObjectProperty<LocalDateTime>(null);

    private SimpleStringProperty m_defaultId = new SimpleStringProperty(null);
    private SimpleStringProperty m_selectedId = new SimpleStringProperty(null);

    private double DEFAULT_ADD_STAGE_WIDTH = 600;
    private double DEFAULT_ADD_STAGE_HEIGHT = 485;

    private Stage m_stage = null;
    private double m_addStageWidth = DEFAULT_ADD_STAGE_WIDTH;
    private double m_addStageHeight = DEFAULT_ADD_STAGE_HEIGHT;

    private double m_stageWidth = 600;
    private double m_stageHeight = 500;

    private long m_lastSave = 0;

    public ErgoMarketsList(SecretKey secretKey, ErgoMarkets ergoMarkets) {
        m_ergoMarkets = ergoMarkets;
        readFile(secretKey);
    }

    public ErgoMarketsList(ErgoMarkets ergoMarkets) {
        m_ergoMarkets = ergoMarkets;
        SecretKey secretKey = m_ergoMarkets.getNetworksData().getAppData().appKeyProperty().get();
        readFile(secretKey);
        m_ergoMarkets.timeStampProperty().addListener((obs,oldval,newval)->{
            long lastSave = newval.longValue();
            if(lastSave != m_lastSave){
                m_lastSave = lastSave;
                readFile(m_ergoMarkets.getNetworksData().getAppData().appKeyProperty().get());
                m_doGridUpdate.set(LocalDateTime.now());
            }
        });
    }



    public ErgoMarketsData getMarketsData(String id) {
    
        if (id != null) {
            for (int i = 0; i < m_dataList.size(); i++) {
                ErgoMarketsData marketsData = m_dataList.get(i);

                if (marketsData.getId().equals(id)) {
                  
                    return marketsData;
                }
            }
        }
        return null;
    }

    public void remove(String id) {
    
        if (id != null) {
            for (int i = 0; i < m_dataList.size(); i++) {
                ErgoMarketsData marketsData = m_dataList.get(i);

                if (marketsData.getId().equals(id)) {
                    marketsData.shutdown();
                    m_dataList.remove(i);
                    save();
                    m_doGridUpdate.set(LocalDateTime.now());
                    break;
                }
                
            }
        }
    }

    public VBox getGridBox(SimpleDoubleProperty width, SimpleDoubleProperty scrollWidth) {
        VBox gridBox = new VBox();

        Runnable updateGrid = () -> {
            gridBox.getChildren().clear();

            int numCells = m_dataList.size();

            for (int i = 0; i < numCells; i++) {
                ErgoMarketsData marketsData = m_dataList.get(i);
                HBox rowItem = marketsData.getRowItem();
                rowItem.prefWidthProperty().bind(width.subtract(scrollWidth));
                gridBox.getChildren().add(rowItem);
            }

        };

        updateGrid.run();

        m_doGridUpdate.addListener((obs, oldval, newval) -> updateGrid.run());
        return gridBox;
    }

    private void readFile(SecretKey secretKey) {

        
        File dataFile = m_ergoMarkets.getDataFile();

        if (dataFile != null && dataFile.isFile()) {
            try {
     
                openJson(Utils.readJsonFile(secretKey, dataFile));
            } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                    | IOException e) {

            }

        }
    }

    public SimpleStringProperty selectedIdProperty(){
        return m_selectedId;
    }

    public ArrayList<ErgoMarketsData> getMarketsDataList() {
        return m_dataList;
    }

    public String getID() {
        return m_id;
    }

    public SimpleStringProperty defaultIdProperty() {
        return m_defaultId;
    }

    public ErgoMarkets getErgoMarkets() {
        return m_ergoMarkets;
    }

    private void openJson(JsonObject json) {
        m_dataList.clear();

        JsonElement marketsElement = json.get("data");
        JsonElement stageElement = json.get("stage");
        JsonElement defaultIdElement = json.get("defaultId");
        String defaultId = defaultIdElement != null ? defaultIdElement.getAsString() : null;
        m_defaultId.set(defaultId);

        if (marketsElement != null && marketsElement.isJsonArray()) {
            
            JsonArray marketsArray = marketsElement.getAsJsonArray();

            for (int i = 0; i < marketsArray.size(); i++) {
                JsonElement marketsDataItem = marketsArray.get(i);
                JsonObject marketsDataJson = marketsDataItem.getAsJsonObject();
                String marketId = marketsDataJson.get("id").getAsString();
                switch(marketId){
                    case KucoinExchange.NETWORK_ID:
                        try{
                            KucoinErgoMarketsData kuCoinData = new KucoinErgoMarketsData(this, marketsDataJson);
                            add(kuCoinData);
                            if(defaultIdProperty().get() == null){
                                defaultIdProperty().set(marketId);    
                            }
                            
                        }catch(NullPointerException e){
                            try {
                                Files.writeString(logFile.toPath(), "ErgoMarketsList (openJson):" +e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e1) {

                            }
                        }
                        break;
                    case SpectrumFinance.NETWORK_ID:
                        try{
                            m_dataList.add(new SpectrumErgoMarketsData(this, marketsDataJson));
                            if(defaultIdProperty().get() == null){
                                defaultIdProperty().set(marketId);    
                            }
                           
                        }catch(NullPointerException e){
                            try {
                                Files.writeString(logFile.toPath(), "ErgoMarketsList (openJson):" +e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e1) {

                            }
                        }
                    break;
                }
               // m_dataList.add(new ErgoMarketsData(this, ));
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

    public void add(ErgoMarketsData data) {
        m_dataList.add(data);
        m_doGridUpdate.set(LocalDateTime.now());
    }

    public void showAddStage() {
        if (m_stage == null) {
            String heading = "Add Market";
            //String friendlyId = FriendlyId.createFriendlyId();

            SimpleStringProperty typeOption = new SimpleStringProperty(null);

            Image icon = ErgoMarkets.getSmallAppIcon();
            String name = m_ergoMarkets.getName();
            VBox layoutBox = new VBox();

            m_stage = new Stage();
            m_stage.getIcons().add(icon);
            m_stage.setResizable(false);
            m_stage.initStyle(StageStyle.UNDECORATED);

            //double minWidth = 600;
            //double minHeight = 500;

            Scene addScene = new Scene(layoutBox, m_addStageWidth, m_addStageHeight);
            addScene.setFill(null);
            addScene.getStylesheets().add("/css/startWindow.css");
            
            Button closeBtn = new Button();

            Button maximizeButton = new Button();

            HBox titleBox = App.createTopBar(icon, maximizeButton, closeBtn, m_stage);
            Region menuSpacer = new Region();
            HBox.setHgrow(menuSpacer, Priority.ALWAYS);


            String titleString = heading + " - " + name;
            m_stage.setTitle(titleString);

            Text headingText = new Text(heading);
            headingText.setFont(App.txtFont);
            headingText.setFill(Color.WHITE);

            HBox headingBox = new HBox(headingText);
            headingBox.prefHeight(40);
            headingBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headingBox, Priority.ALWAYS);
            headingBox.setPadding(new Insets(10, 10, 10, 10));
            headingBox.setId("headingBox");

            HBox headingPaddingBox = new HBox(headingBox);
            headingPaddingBox.setPadding(new Insets(5, 0, 2, 0));

            VBox headerBox = new VBox(titleBox, headingPaddingBox);
            headerBox.setPadding(new Insets(0, 5, 0, 5));

            SimpleDoubleProperty rowHeight = new SimpleDoubleProperty(40);

            Text marketTypeText = new Text("Market ");
            marketTypeText.setFill(App.txtColor);
            marketTypeText.setFont(App.txtFont);

            MenuButton typeBtn = new MenuButton();
            typeBtn.setId("bodyRowBox");
            typeBtn.setMinWidth(300);
            typeBtn.setAlignment(Pos.CENTER_LEFT);


            Runnable typeBtnUpdate = () ->{
                typeBtn.getItems().clear();
                for(String marketId : MARKET_OPTIONS_AVAILABLE){
                    if(getMarketsData(marketId) == null){
                        final InstallableIcon newInstallableItem = new InstallableIcon(m_ergoMarkets.getNetworksData(), marketId,true);
                        MenuItem marketItem = new MenuItem(newInstallableItem.getName());
                        marketItem.setGraphic(IconButton.getIconView(newInstallableItem.getIcon(), App.MENU_BAR_IMAGE_WIDTH));
                        marketItem.setUserData(marketId);
                        marketItem.setOnAction(e->{
                            typeOption.set(marketId);
                            typeBtn.setText(newInstallableItem.getName());
                            typeBtn.setGraphic(IconButton.getIconView(newInstallableItem.getIcon(), App.MENU_BAR_IMAGE_WIDTH));
                        });
                        
                        typeBtn.getItems().add(marketItem);
                    }
                }
                if(typeOption.get() == null){
                    if(typeBtn.getItems().size() > 0){
                        typeBtn.getItems().get(0).fire();
                    }else{
                        typeOption.set(null);
                        typeBtn.setText("(all markets added)");
                        typeBtn.setGraphic(null);
                    }
                }else{
                    if(getMarketsData(typeOption.get()) != null){
                        if(typeBtn.getItems().size() > 0){
                            typeBtn.getItems().get(0).fire();
                        }else{
                            typeOption.set(null);
                            typeBtn.setText("(none available)");
                        }
                    }
                }
            };

            typeBtnUpdate.run();

            m_doGridUpdate.addListener((obs,oldval,newval)->typeBtnUpdate.run());

            HBox marketTypeBox = new HBox(marketTypeText, typeBtn);
            // Binding<Double> viewportWidth = Bindings.createObjectBinding(()->settingsScroll.viewportBoundsProperty().get().getWidth(), settingsScroll.viewportBoundsProperty());


            typeBtn.minWidthProperty().bind(marketTypeBox.widthProperty().subtract(marketTypeText.layoutBoundsProperty().get().getWidth()).subtract(5));
            marketTypeBox.setAlignment(Pos.CENTER_LEFT);
            marketTypeBox.setPadding(new Insets(0));
            marketTypeBox.minHeightProperty().bind(rowHeight);
            HBox.setHgrow(marketTypeBox, Priority.ALWAYS);

            Button marketImg = App.createImageButton(ErgoMarkets.getAppIcon(), "Ergo Markets");
         
            HBox marketImgBtnBox = new HBox(marketImg);
            HBox.setHgrow(marketImgBtnBox,Priority.ALWAYS);
            marketImgBtnBox.setAlignment(Pos.CENTER);


            TextArea marketTextArea = new TextArea();
            marketTextArea.setWrapText(true);
            marketTextArea.setEditable(false);
            VBox.setVgrow(marketTextArea, Priority.ALWAYS);
        
            VBox marketTextAreaBox = new VBox(marketTextArea);
            marketTextAreaBox.setPadding(new Insets(10));
            HBox.setHgrow(marketTextAreaBox, Priority.ALWAYS);
            VBox.setVgrow(marketTextAreaBox,Priority.ALWAYS);


            VBox marketOptionsBox = new VBox(marketImgBtnBox, marketTextAreaBox);
            VBox.setVgrow(marketOptionsBox,Priority.ALWAYS);

            VBox bodyOptionsBox = new VBox();
            bodyOptionsBox.setPadding(new Insets(15,0,0,15));
            VBox.setVgrow(bodyOptionsBox, Priority.ALWAYS);

            Runnable updateBodyOptions = ()->{
     
                if(typeOption.get() != null){
                    final String optionString = typeOption.get();
                    final InstallableIcon newInstallableItem = new InstallableIcon(m_ergoMarkets.getNetworksData(), optionString,false);
                     
                    marketImg.setGraphic(IconButton.getIconView(newInstallableItem.getIcon(), 135));
                    marketImg.setText(newInstallableItem.getName());
                    marketTextArea.setText(newInstallableItem.getDescription());

                    if(!bodyOptionsBox.getChildren().contains(marketOptionsBox)){
                        bodyOptionsBox.getChildren().add(marketOptionsBox);
                    }
                }else{
                    bodyOptionsBox.getChildren().clear();
                }
            
            };


            typeOption.addListener((obs,oldval,newval)->updateBodyOptions.run());

            updateBodyOptions.run();

            Button nextBtn = new Button("Add");
            nextBtn.setPadding(new Insets(5, 15, 5, 15));

            HBox nextBox = new HBox(nextBtn);
            nextBox.setPadding(new Insets(0, 0, 0, 0));
            nextBox.setMinHeight(50);
            nextBox.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(nextBox, Priority.ALWAYS);

            
            VBox bodyBox = new VBox(marketTypeBox, bodyOptionsBox, nextBox);
            bodyBox.setId("bodyBox");
            bodyBox.setPadding(new Insets(0, 10, 0, 10));
            VBox.setVgrow(bodyBox, Priority.ALWAYS);
            VBox bodyPaddingBox = new VBox(bodyBox);
            bodyPaddingBox.setPadding(new Insets(0, 5, 0, 5));
            VBox.setVgrow(bodyPaddingBox, Priority.ALWAYS);
            
            layoutBox.getChildren().addAll(headerBox, bodyPaddingBox);

            nextBtn.setOnAction(e->{
                if(typeOption.get() != null){
                    final String optionString = typeOption.get();
                    switch(optionString){
                        case KucoinExchange.NETWORK_ID:
                            typeOption.set(null);
                            KucoinErgoMarketsData kucoinMarketData = new KucoinErgoMarketsData(this);    
                            add(kucoinMarketData);
                            
                            save();
                       
                        break;
                        case SpectrumFinance.NETWORK_ID:
                            typeOption.set(null);
                            SpectrumErgoMarketsData spectrumMarketData = new SpectrumErgoMarketsData(this);
                            add(spectrumMarketData);
                         

                            save();
                         
                        break;
                    }
                }
            });

            closeBtn.setOnAction(e->{
                if(m_stage != null){
                    m_stage.close();
                    m_stage = null;
                }
            });

            m_stage.setOnCloseRequest(e->closeBtn.fire());

            m_stage.setScene(addScene);
            m_stage.show();
        } else {
            
            if(m_stage.isIconified()){
                m_stage.setIconified(false);    
            }
            if(!m_stage.isShowing()){
                m_stage.show();
            }else{
                Platform.runLater(()->m_stage.toBack());
                
                Platform.runLater(()->m_stage.toFront());
            }
            
        }
    }

    public JsonArray getMarketsJsonArray() {
        JsonArray jsonArray = new JsonArray();

        for (ErgoMarketsData mData : m_dataList) {

            JsonObject jsonObj = mData.getJsonObject();
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

    public void shutdown() {
        for (ErgoMarketsData mData : m_dataList) {
            mData.shutdown();
        }
    }

    public JsonObject getDataObject() {
        JsonObject fileObject = new JsonObject();
        String defaultId = defaultIdProperty().get();
        if (defaultId != null) {
            fileObject.addProperty("defaultId", defaultId);
        }
        fileObject.add("data", getMarketsJsonArray());
        return fileObject;
    }

    public void save() {
        m_lastSave = System.currentTimeMillis();
        JsonObject fileJson = getDataObject();
        fileJson.add("stage", getStageJson());
        String jsonString = fileJson.toString();

        //  byte[] bytes = jsonString.getBytes(StandardCharsets.UTF_8);
        // String fileHexString = Hex.encodeHexString(bytes);
        try {
            Utils.writeEncryptedString(m_ergoMarkets.getNetworksData().getAppData().appKeyProperty().get(), m_ergoMarkets.getDataFile(), jsonString);
            m_ergoMarkets.timeStampProperty().set(m_lastSave);
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
                | IOException e) {
            try {
                Files.writeString(logFile.toPath(), "\nMarketsList: " + e.toString());
            } catch (IOException e1) {

            }
        }

        
    }

      public void getMenu(MenuButton menuBtn, SimpleObjectProperty<ErgoMarketsData> selectedMarketsData){

        Runnable updateMenu = () -> {
            ErgoMarketsData selectedMarketData = selectedMarketsData.get();

            menuBtn.getItems().clear();
            MenuItem noneMenuItem = new MenuItem("(disable)");
            if(selectedMarketData == null){
                noneMenuItem.setId("selectedMenuItem");
            }
            noneMenuItem.setOnAction(e->{
                selectedMarketsData.set(null);
            });
            menuBtn.getItems().add(noneMenuItem);
     

            int numCells = m_dataList.size();
            
            for (int i = 0; i < numCells; i++) {
            
                ErgoMarketsData marketsData = m_dataList.get(i);
                MenuItem menuItem = new MenuItem(marketsData.getName() + (selectedMarketData != null && selectedMarketData.getId().equals(marketsData.getId()) ? " (selected)" : ""));
                if(selectedMarketData != null && selectedMarketData.getId().equals(marketsData.getId())){
                    menuItem.setId("selectedMenuItem");
                }
                menuItem.setOnAction(e->{
                    selectedMarketsData.set(marketsData);
                });

                menuBtn.getItems().add(menuItem);
            }


        };

        updateMenu.run();
        selectedMarketsData.addListener((obs,oldval, newval)->updateMenu.run());

        m_doGridUpdate.addListener((obs, oldval, newval) -> updateMenu.run());
 
    }


}
