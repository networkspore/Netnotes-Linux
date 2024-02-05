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

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ErgoMarketsList {

    private File logFile = new File("netnotes-log.txt");
    private String m_id = FriendlyId.createFriendlyId();
    private ArrayList<ErgoMarketsData> m_dataList = new ArrayList<>();
    private ErgoMarkets m_ergoMarkets;
    private SimpleObjectProperty<LocalDateTime> m_doGridUpdate = new SimpleObjectProperty<LocalDateTime>(null);

    private SimpleStringProperty m_defaultId = new SimpleStringProperty(null);

    private Stage m_stage = null;
    private double m_stageWidth = 600;
    private double m_stageHeight = 500;

     public ErgoMarketsList(SecretKey secretKey, ErgoMarkets ergoMarkets) {
        m_ergoMarkets = ergoMarkets;
        readFile(secretKey);
    }

    public ErgoMarketsList(ErgoMarkets ergoMarkets) {
        m_ergoMarkets = ergoMarkets;
        SecretKey secretKey = m_ergoMarkets.getNetworksData().getAppData().appKeyProperty().get();
        readFile(secretKey);
      
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
                            KucoinErgoMarketsData kEMData = new KucoinErgoMarketsData(this, marketsDataJson);
                            m_dataList.add(kEMData);
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
    }

    public void showAddStage() {
        if (m_stage == null) {
            String heading = "Add market";
            String friendlyId = FriendlyId.createFriendlyId();

            m_stage = new Stage();
            m_stage.getIcons().add(ErgoMarkets.getSmallAppIcon());
            m_stage.setResizable(false);
            m_stage.initStyle(StageStyle.UNDECORATED);
            m_stage.setTitle(heading + " - " + ErgoMarkets.NAME);

            Button closeBtn = new Button();

            Button maximizeButton = new Button();

            HBox titleBox = App.createTopBar(ErgoMarkets.getSmallAppIcon(), maximizeButton, closeBtn, m_stage);
            Region menuSpacer = new Region();
            HBox.setHgrow(menuSpacer, Priority.ALWAYS);

            Text headingText = new Text(heading);
            headingText.setFont(App.txtFont);
            headingText.setFill(Color.WHITE);

            HBox headingBox = new HBox(headingText);
            headingBox.prefHeight(40);
            headingBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headingBox, Priority.ALWAYS);
            headingBox.setPadding(new Insets(10, 15, 10, 15));
            headingBox.setId("headingBox");

            HBox headingPaddingBox = new HBox(headingBox);
            headingPaddingBox.setPadding(new Insets(5, 2, 2, 2));

            Text nameText = new Text(String.format("%-15s", "Name"));
            nameText.setFill(App.txtColor);
            nameText.setFont(App.txtFont);

            TextField nameField = new TextField("Market #" + friendlyId);
            nameField.setFont(App.txtFont);
            nameField.setId("formField");
            HBox.setHgrow(nameField, Priority.ALWAYS);

            
            HBox nameBox = new HBox(nameText, nameField);
            nameBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(nameBox, Priority.ALWAYS);


            Text marketText = new Text(String.format("%-15s", "Market"));
            marketText.setFill(App.txtColor);
            marketText.setFont(App.txtFont);

          //  SimpleStringProperty selectedMarketId = new SimpleStringProperty(KucoinExchange.NETWORK_ID);
            SimpleStringProperty selectedMarketType = new SimpleStringProperty(ErgoMarketsData.REALTIME);
            SimpleStringProperty selectedMarketValue = new SimpleStringProperty(ErgoMarketsData.TICKER);

            MenuButton marketsBtn = new MenuButton(KucoinExchange.NAME);
            marketsBtn.setFont(App.txtFont);
            marketsBtn.setTextFill(App.altColor);
            marketsBtn.setPrefWidth(200);

            MenuItem marketKucoinItem = new MenuItem(KucoinExchange.NAME);
            marketKucoinItem.setGraphic(IconButton.getIconView(KucoinExchange.getSmallAppIcon(), 30));
            marketKucoinItem.setOnAction((e) -> {

            });

            //no other options avaialable
            marketsBtn.getItems().add(marketKucoinItem);

            HBox marketBox = new HBox(marketText, marketsBtn);
            marketBox.setAlignment(Pos.CENTER_LEFT);

            Text typeText = new Text(String.format("%-15s", "Type"));
            typeText.setFill(App.txtColor);
            typeText.setFont(App.txtFont);

            MenuButton typeBtn = new MenuButton("Real-time: Ticker");
            typeBtn.setPadding(new Insets(4, 5, 0, 5));
            typeBtn.setFont(Font.font("OCR A Extended", 12));

            MenuItem updatesRealTimeItem = new MenuItem("Real-time: Ticker");
            updatesRealTimeItem.setOnAction(e -> {
                typeBtn.setText("Real-time");
                selectedMarketType.set(ErgoMarketsData.REALTIME);
                selectedMarketValue.set(ErgoMarketsData.TICKER);
            });

            MenuItem updates5secItem = new MenuItem("5s");
            updates5secItem.setOnAction(e -> {
                typeBtn.setText(updates5secItem.getText());
                selectedMarketType.set(ErgoMarketsData.POLLED);
                selectedMarketValue.set("5");
            });

            MenuItem updates15secItem = new MenuItem("15s");
            updates15secItem.setOnAction(e -> {
                typeBtn.setText(updates15secItem.getText());
                selectedMarketType.set(ErgoMarketsData.POLLED);
                selectedMarketValue.set("15");
            });

            MenuItem updates30secItem = new MenuItem("30s");
            updates30secItem.setOnAction(e -> {
                typeBtn.setText(updates30secItem.getText());
                selectedMarketType.set(ErgoMarketsData.POLLED);
                selectedMarketValue.set("30");
            });

            typeBtn.getItems().addAll(updates5secItem, updates15secItem, updates30secItem);

            HBox updatesBox = new HBox(typeText, typeBtn);

           // SimpleDoubleProperty rowHeight = new SimpleDoubleProperty(40);

            closeBtn.setOnAction(closeEvent -> {
                m_stage.close();
                m_stage = null;
            });
            m_ergoMarkets.shutdownNowProperty().addListener((obs, oldVal, newVal) -> {
                closeBtn.fire();
            });
            m_stage.setOnCloseRequest(e -> {
                m_stage = null;
            });

            VBox layoutBox = new VBox(titleBox, headingPaddingBox,nameBox, marketBox, updatesBox);
            Scene addScene = new Scene(layoutBox, m_stageWidth, m_stageHeight);
            addScene.setFill(null);
            m_stage.setScene(addScene);

            m_stage.show();
        } else {
            m_stage.show();
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
        JsonObject fileJson = getDataObject();
        fileJson.add("stage", getStageJson());
        String jsonString = fileJson.toString();

        //  byte[] bytes = jsonString.getBytes(StandardCharsets.UTF_8);
        // String fileHexString = Hex.encodeHexString(bytes);
        try {
            Utils.writeEncryptedString(m_ergoMarkets.getNetworksData().getAppData().appKeyProperty().get(), m_ergoMarkets.getDataFile(), jsonString);
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
