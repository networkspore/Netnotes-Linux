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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.netnotes.engine.IconButton;
import io.netnotes.engine.Network;
import io.netnotes.engine.NoteInterface;
import io.netnotes.engine.Stages;
import io.netnotes.engine.Utils;
import io.netnotes.friendly_id.FriendlyId;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class KuCoinDataList extends Network implements NoteInterface {

    private File logFile = new File("netnotes-log.txt");
    private KucoinExchange m_kucoinExchange;

    private VBox m_favoriteGridBox = new VBox();
    private VBox m_gridBox = new VBox();
    private List<KucoinMarketItem> m_marketsList = Collections.synchronizedList(new ArrayList<KucoinMarketItem>());
    private ArrayList<String> m_favoriteIds = new ArrayList<String>();

    private boolean m_notConnected = false;
    private SimpleStringProperty m_statusMsg = new SimpleStringProperty("Loading...");

    private int m_sortMethod = 0;
    private boolean m_sortDirection = false;
    private String m_searchText = null;

    public KuCoinDataList(KucoinExchange kuCoinExchange) {
        super(null, "Ergo Charts List", "KUCOIN_CHARTS_LIST", kuCoinExchange);
        m_kucoinExchange = kuCoinExchange;

        setup();

    }
    public KuCoinDataList(KucoinExchange kuCoinExchange, SecretKey oldval, SecretKey newval ) {
        super(null, "Ergo Charts List", "KUCOIN_CHARTS_LIST", kuCoinExchange);
        m_kucoinExchange = kuCoinExchange;

        updateFile(oldval, newval);
    }

    public Image getSmallAppIcon(){
        return null;
    }

    private void setup() {
        getData(onSucceeded->{

    
            updateGridBox();

            m_kucoinExchange.getAllTickers(success -> {
                Object sourceObject = success.getSource().getValue();
                if (sourceObject != null && sourceObject instanceof JsonObject) {

                    readTickers(getDataJson((JsonObject) sourceObject), onSuccess -> {
                    
                        sortByChangeRate(false);
                        sort();
                        updateGridBox();
                        getLastUpdated().set(LocalDateTime.now());
                    }, failed -> {
                        try {
                            Files.writeString(logFile.toPath(), "setup failed 1 " + failed.getSource().getException().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e) {

                        }
                        m_notConnected = true;
                        m_statusMsg.set("Not connected");
                        updateGridBox();

                    });

                } else {
                    try {
                        Files.writeString(logFile.toPath(), "setup failed 2  instance of object is not json", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {

                    }
                    m_notConnected = true;
                    m_statusMsg.set("Not connected");
                    updateGridBox();
                }
            }, failed -> {
                try {
                    Files.writeString(logFile.toPath(), "setup failed 3 " + failed.getSource().getException().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }
                m_notConnected = true;
                m_statusMsg.set("Not connected");
                updateGridBox();
            });

        });
      
    }

    public void closeAll() {

    }



    public void addFavorite(String symbol, boolean doSave) {
        if (!getIsFavorite(symbol)) {
            
            synchronized(m_favoriteIds){
                m_favoriteIds.add(symbol);
            }
           

            if (doSave) {
                updateGridBox();
                save();
            }
        }
    }

    public void removeFavorite(String symbol, boolean doSave) {
        if(symbol != null && getIsFavorite(symbol)){

            synchronized(m_favoriteIds){
                m_favoriteIds.remove(symbol);
            }
            updateGridBox();

            if (doSave) {
                save();
            }
        
        }
    }

    public SimpleStringProperty statusProperty() {
        return m_statusMsg;
    }

    private JsonObject getDataJson(JsonObject urlJson) {
        if (urlJson != null) {
            JsonElement dataElement = urlJson.get("data");
            if (dataElement != null && dataElement.isJsonObject()) {
                return dataElement.getAsJsonObject();
            }
        }
        return null;
    }

    public KucoinMarketItem getMarketItem(String symbol) {
        if (symbol != null) {
            KucoinMarketItem[] itemArray = null;
            
            synchronized(m_marketsList){
                itemArray = new KucoinMarketItem[ m_marketsList.size()];
                itemArray = m_marketsList.toArray(itemArray);
            }

            if(itemArray != null){
                for (KucoinMarketItem item : itemArray) {
                    if (item.getSymbol().equals(symbol)) {
                        return item;
                    }
                }
            }
        }
        return null;
    }

    public void updateTickers() {
        m_kucoinExchange.getAllTickers(success -> {
            Object sourceObject = success.getSource().getValue();
            if (sourceObject != null && sourceObject instanceof JsonObject) {
                readTickers(getDataJson((JsonObject) sourceObject), succeeded -> {
                    m_notConnected = false;
                    updateGridBox();
                    getLastUpdated().set(LocalDateTime.now());
                }, secondfail -> {

                    m_notConnected = true;
                    m_statusMsg.set("Not connected");
                    updateGridBox();
                    getLastUpdated().set(LocalDateTime.now());
                }
                );

            } else {
                m_notConnected = true;
                m_statusMsg.set("Not connected");
                updateGridBox();
                getLastUpdated().set(LocalDateTime.now());
            }
        }, failed -> {
            m_notConnected = true;
            m_statusMsg.set("Not connected");
            updateGridBox();
            getLastUpdated().set(LocalDateTime.now());
        });
    }

    public KuCoinDataList getKuCoinDataList() {
        return this;
    }

    private void readTickers(JsonObject tickersJson, EventHandler<WorkerStateEvent> onSuccess, EventHandler<WorkerStateEvent> onFailed) {

        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() {

                JsonElement tickerElement = tickersJson.get("ticker");

                if (tickerElement != null && tickerElement.isJsonArray()) {

                    JsonArray jsonArray = tickerElement.getAsJsonArray();

                    int i = 0;
                    boolean init = false;
                    if (m_marketsList.size() == 0) {
                        init = true;
                    }
                    synchronized(m_marketsList){
                        for (i = 0; i < jsonArray.size(); i++) {
                            try {
                                JsonElement tickerObjectElement = jsonArray.get(i);
                                if (tickerObjectElement != null && tickerObjectElement.isJsonObject()) {

                                    JsonObject tickerJson = tickerObjectElement.getAsJsonObject();
                                    JsonElement symbolElement = tickerJson.get("symbol");
                                    if (symbolElement != null && symbolElement.isJsonPrimitive()) {

                                        String symbolString = symbolElement.getAsString();

                                        KucoinTickerData tickerData = new KucoinTickerData(symbolString, tickerJson);

                                       

                                        if (init) {
                                            boolean isFavorite = getIsFavorite(symbolString);

                                            Platform.runLater(()->{
                                                String id = FriendlyId.createFriendlyId();
                                                m_marketsList.add(new KucoinMarketItem(m_kucoinExchange, id, symbolString, symbolString, isFavorite, tickerData, getKuCoinDataList()));
                                            });
                                        }else {
                                            Platform.runLater(()->{
                                                KucoinMarketItem item = getMarketItem(symbolString);
                                                if(item != null){
                                                    item.tickerDataProperty().set(tickerData);
                                                }else{

                                                    String id = FriendlyId.createFriendlyId();
                                                    m_marketsList.add(new KucoinMarketItem(m_kucoinExchange, id, symbolString, symbolString, getIsFavorite(id), tickerData, getKuCoinDataList()));    
                                                }
                                            });
                                            
                                        }
                                    }
                                }

                            } catch (Exception jsonException) {
                                try {
                                    Files.writeString(logFile.toPath(), "\njson error" + jsonException.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                } catch (IOException e2) {

                                }
                            }

                        }
                    }

                } else {
                    try {
                        Files.writeString(logFile.toPath(), "\nticker json bad format: \n" + tickersJson.toString());
                    } catch (IOException e) {

                    }
                }

                return null;

            }
        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSuccess);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();

    }

    public boolean getIsFavorite(String id){

        synchronized(m_favoriteIds){
            return m_favoriteIds.contains(id);
            
        }

    }


    private void updateFile(SecretKey oldKey, SecretKey newKey){
        File dataFile = m_kucoinExchange.getDataFile();
        if (dataFile != null && dataFile.isFile()) {
            try {
                JsonObject json = Utils.readJsonFile(oldKey, dataFile);
               
                if(json!= null){
                    Utils.saveJson(newKey, json, dataFile);
                }
            } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                    | IOException e) {

                try {
                    Files.writeString(logFile.toPath(), "\nKuCoin getfile error: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }

            }

        }
    }

    private void getData(EventHandler<WorkerStateEvent> onSucceeded) {

        m_kucoinExchange.getNetworksData().getData("data", ".", getNetworkId(), KucoinExchange.NETWORK_ID, onSucceeded);
        

    }

    /*
    private void openJson(JsonObject json) {
        JsonElement dataElement = json.get("data");

        if (dataElement != null && dataElement.isJsonArray()) {

            JsonArray dataArray = dataElement.getAsJsonArray();

            //  if (m_ergoTokens.getNetworkType().toString().equals(networkType)) {
            for (JsonElement objElement : dataArray) {
                if (objElement.isJsonObject()) {
                    JsonObject objJson = objElement.getAsJsonObject();
                    JsonElement nameElement = objJson.get("name");
                    JsonElement idElement = objJson.get("id");

                    if (nameElement != null && nameElement.isJsonPrimitive() && idElement != null && idElement.isJsonPrimitive()) {

                        try {
                            Files.writeString(logFile.toPath(), "\ndataElement: " + objJson.toString());
                        } catch (IOException e) {

                        }
                    }

                }
            }
            //   }
        }

        updateGridBox();
    } */
    public VBox getGridBox() {

        return m_gridBox;
    }

    public VBox getFavoriteGridBox() {
        return m_favoriteGridBox;
    }

    private void sort() {

        synchronized(m_marketsList){
        switch (m_sortMethod) {
            case 0:
                if (m_sortDirection) {
                    Collections.sort(m_marketsList, Comparator.comparing(KucoinMarketItem::getChangeRate));

                } else {
                    Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(KucoinMarketItem::getChangeRate)));

                }
                break;
            case 1:
                if (m_sortDirection) {
                    Collections.sort(m_marketsList, Comparator.comparing(KucoinMarketItem::getChangePrice));
                } else {
                    Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(KucoinMarketItem::getChangePrice)));
                }
                break;
            case 2:
                if (m_sortDirection) {
                    Collections.sort(m_marketsList, Comparator.comparing(KucoinMarketItem::getHigh));
                } else {
                    Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(KucoinMarketItem::getHigh)));
                }
                break;
            case 3:
                if (m_sortDirection) {
                    Collections.sort(m_marketsList, Comparator.comparing(KucoinMarketItem::getLow));
                } else {
                    Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(KucoinMarketItem::getLow)));
                }
                break;
            case 4:
                if (m_sortDirection) {
                    Collections.sort(m_marketsList, Comparator.comparing(KucoinMarketItem::getVolValue));
                } else {
                    Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(KucoinMarketItem::getVolValue)));
                }
                break;

        }
        }

    }

    public void sortByChangeRate(boolean direction) {
        m_sortMethod = 0;
        m_sortDirection = direction;
        String msg = "Top 100 - 24h Change Rate ";
        m_statusMsg.set(msg + (direction ? "(Low to High)" : "(High to Low)"));
    }

    public void sortByChangePrice(boolean direction) {
        m_sortMethod = 1;
        m_sortDirection = direction;

        String msg = "Top 100 - 24h Price Change ";
        m_statusMsg.set(msg + (direction ? "(Low to High)" : ("High to Low")));
    }

    public void sortByHigh(boolean direction) {
        m_sortMethod = 2;
        m_sortDirection = direction;
        String msg = "Top 100 - 24h High Price ";
        m_statusMsg.set(msg + (direction ? "(Low to High)" : ("High to Low")));
    }

    public void sortByLow(boolean direction) {
        m_sortMethod = 3;
        m_sortDirection = direction;
        String msg = "Top 100 - 24h Low Price ";
        m_statusMsg.set(msg + (direction ? "(Low to High)" : ("High to Low")));
    }

    public void sortByVolValue(boolean direction) {
        m_sortMethod = 4;
        m_sortDirection = direction;
        String msg = "Top 100 - 24h Volume ";
        m_statusMsg.set(msg + (direction ? "(Low to High)" : ("High to Low")));
    }

    public void setSearchText(String text) {
        m_searchText = text.equals("") ? null : text;

        updateGridBox();

    }

    public String getSearchText() {
        return m_searchText;
    }
    public String getType(){
        return "DATA";
    }
    public String getDescription(){
        return "data";
    }
    private void doSearch(List<KucoinMarketItem> marketItems, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        Task<KucoinMarketItem[]> task = new Task<KucoinMarketItem[]>() {
            @Override
            public KucoinMarketItem[] call() {
            
                List<KucoinMarketItem> searchResultsList = marketItems.stream().filter(marketItem -> marketItem.getSymbol().contains(m_searchText.toUpperCase())).collect(Collectors.toList());

                KucoinMarketItem[] results = new KucoinMarketItem[searchResultsList.size()];

                searchResultsList.toArray(results);

                return results;
                
            }
        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public void updateGridBox() {
        m_favoriteGridBox.getChildren().clear();
        m_gridBox.getChildren().clear();
      

        if (m_marketsList.size() > 0) {

            synchronized(m_favoriteIds){
                int numFavorites = m_favoriteIds.size();
                String[] favIds = new String[numFavorites];
                favIds = m_favoriteIds.toArray(favIds);
            
                for (int i = 0; i < numFavorites; i++) {
                    String favId = favIds[i];

                    KucoinMarketItem favMarketItem = getMarketItem(favId);
                    if(favMarketItem != null){
                        m_favoriteGridBox.getChildren().add(favMarketItem.getRowBox());
                    }
                }
            }
            if (m_searchText == null) {
                int numCells = m_marketsList.size() > 100 ? 100 : m_marketsList.size();
                  
                for (int i = 0; i < numCells; i++) {
                    KucoinMarketItem marketItem = m_marketsList.get(i);

                    HBox rowBox = marketItem.getRowBox();

                    m_gridBox.getChildren().add(rowBox);

                }
            } else {

                // List<KucoinMarketItem> searchResultsList = m_marketsList.stream().filter(marketItem -> marketItem.getSymbol().contains(m_searchText.toUpperCase())).collect(Collectors.toList());
                doSearch(m_marketsList, onSuccess -> {
                    WorkerStateEvent event = onSuccess;
                    Object sourceObject = event.getSource().getValue();

                    if (sourceObject instanceof KucoinMarketItem[]) {
                        KucoinMarketItem[] searchResults = (KucoinMarketItem[]) sourceObject;
                        int numResults = searchResults.length > 100 ? 100 : searchResults.length;

                        for (int i = 0; i < numResults; i++) {
                            KucoinMarketItem marketItem = searchResults[i];

                            HBox rowBox = marketItem.getRowBox();

                            m_gridBox.getChildren().add(rowBox);
                        }
                    }
                }, onFailed -> {
                });

            }
        } else {
            HBox imageBox = new HBox();
            imageBox.setAlignment(Pos.CENTER);
            HBox.setHgrow(imageBox, Priority.ALWAYS);
            
            if (m_notConnected) {
                Button notConnectedBtn = new Button("No Connection");
                notConnectedBtn.setFont(Stages.txtFont);
                notConnectedBtn.setTextFill(Stages.txtColor);
                notConnectedBtn.setId("menuBtn");
                notConnectedBtn.setGraphicTextGap(15);
                notConnectedBtn.setGraphic(IconButton.getIconView(new Image("/assets/cloud-offline-150.png"), 150));
                notConnectedBtn.setContentDisplay(ContentDisplay.TOP);
                notConnectedBtn.setOnAction(e -> {
                    updateTickers();
                });

            } else {
                Button loadingBtn = new Button("Loading...");
                loadingBtn.setFont(Stages.txtFont);
                loadingBtn.setTextFill(Color.WHITE);
                loadingBtn.setId("transparentColor");
                loadingBtn.setGraphicTextGap(15);
                loadingBtn.setGraphic(IconButton.getIconView(new Image("/assets/kucoin-100.png"), 150));
                loadingBtn.setContentDisplay(ContentDisplay.TOP);

                imageBox.getChildren().add(loadingBtn);

            }
            m_gridBox.getChildren().add(imageBox);
        }
    }

    public JsonArray getFavoritesJsonArray() {
        String[] favids = null;
        JsonArray jsonArray = new JsonArray();
        
        synchronized(m_favoriteIds){
            favids = new String[m_favoriteIds.size()];
            favids = m_favoriteIds.toArray(favids);
        }

        if(favids != null){
            for (String favId : favids) {
        
                jsonArray.add(favId);

            }
           
        }
         return jsonArray;
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject jsonObject = super.getJsonObject();

        jsonObject.add("favorites", getFavoritesJsonArray());
        jsonObject.addProperty("sortMethod", m_sortMethod);
        jsonObject.addProperty("sortDirection", m_sortDirection);

        return jsonObject;
    }

    private void openJson(JsonObject json) {
        if (json != null) {
            JsonElement favoritesElement = json.get("favorites");
            JsonElement sortMethodElement = json.get("sortMethod");
            JsonElement sortDirectionElement = json.get("sortDirection");

            if (favoritesElement != null && favoritesElement.isJsonArray()) {
                JsonArray favoriteJsonArray = favoritesElement.getAsJsonArray();

                for (int i = 0; i < favoriteJsonArray.size(); i++) {
                    JsonElement favoriteElement = favoriteJsonArray.get(i);
                    String symbol = favoriteElement.getAsString();
                    addFavorite(symbol, false);
                    KucoinMarketItem item = getMarketItem(symbol);
                    if(item != null){
                        item.isFavoriteProperty().set(true);
                    }
                }
            }

            m_sortDirection = sortDirectionElement != null && sortDirectionElement.isJsonPrimitive() ? sortDirectionElement.getAsBoolean() : m_sortDirection;
            m_sortMethod = sortMethodElement != null && sortMethodElement.isJsonPrimitive() ? sortMethodElement.getAsInt() : m_sortMethod;

        }
    }

    public KucoinExchange getKucoinExchange() {
        return m_kucoinExchange;
    }

    public void save(){
        m_kucoinExchange.getNetworksData().save("data", ".", getNetworkId(), KucoinExchange.NETWORK_ID, getJsonObject());
    }

    public void save(SecretKey secretKey) {
  
        try {
           
            Utils.saveJson(secretKey, getJsonObject(), m_kucoinExchange.getDataFile());
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                | IOException e) {
            try {
                Files.writeString(logFile.toPath(), "\nsave failed: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
        }

    }

}
