package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

import com.utils.Utils;

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

public class SpectrumDataList extends Network implements NoteInterface {

    private File logFile = new File("netnotes-log.txt");
    private SpectrumFinance m_spectrumFinance;

    private VBox m_favoriteGridBox = new VBox();
    private VBox m_gridBox = new VBox();
    
    private List<SpectrumMarketItem> m_marketsList = Collections.synchronizedList(new ArrayList<SpectrumMarketItem>());

    private ArrayList<String> m_favoriteIds = new ArrayList<>();

    

    private boolean m_notConnected = false;
    private SimpleStringProperty m_statusMsg = new SimpleStringProperty("Loading...");

    private SpectrumSort m_sortMethod = new SpectrumSort();
    private String m_searchText = null;

    public SpectrumDataList(String id, SpectrumFinance spectrumFinance) {
        super(null, "spectrumDataList", id+"SDLIST", spectrumFinance);
        m_spectrumFinance = spectrumFinance;

        setup(m_spectrumFinance.getNetworksData().getAppData().appKeyProperty().get());
        


    }
    public SpectrumDataList(String id, SpectrumFinance spectrumFinance, SecretKey oldval, SecretKey newval ) {
        super(null, "spectrumDataList", id+"SDLIST", spectrumFinance);
        m_spectrumFinance = spectrumFinance;

        updateFile(oldval, newval);
    }

    private void setup(SecretKey secretKey) {
        getFile(secretKey);
        updateMarkets();
    }

    public void closeAll() {

    }

    public boolean getIsFavorite(String id){
        return m_favoriteIds.contains(id);
    }


    public void addFavorite(String id, boolean doSave) {
 
        m_favoriteIds.add(id);
        if (doSave) {
            updateGridBox();
            save();
        }   
    
        
    }

    public void removeFavorite(String symbol, boolean doSave) {
        m_favoriteIds.remove(symbol);

        if (doSave) {
            updateGridBox();
            save();
        }
        
    }

    public SimpleStringProperty statusProperty() {
        return m_statusMsg;
    }



    public SpectrumMarketItem getMarketItem(String id) {
        if (id != null) {
            synchronized(m_marketsList){
                for (SpectrumMarketItem item : m_marketsList) {
                    if (item.getId().equals(id)) {
                        return item;
                    }
                }
            }
        }
        return null;
    }

    public void updateMarkets() {
        
        m_spectrumFinance.getCMCMarkets(success -> {
            boolean init = m_marketsList.size() == 0; 

            Object sourceObject = success.getSource().getValue();
            if (sourceObject != null && sourceObject instanceof JsonArray) {
             
              
                    JsonArray jsonArray = (JsonArray) sourceObject;
                    synchronized(m_marketsList){
                        for (int i = 0; i < jsonArray.size(); i++) {
                    
                            JsonElement marketObjectElement = jsonArray.get(i);
                            if (marketObjectElement != null && marketObjectElement.isJsonObject()) {

                                JsonObject marketDataJson = marketObjectElement.getAsJsonObject();
                                
                                try{
                                    
                                    SpectrumMarketData marketData = new SpectrumMarketData(marketDataJson);
                                    boolean isFavorite = this.getIsFavorite(marketData.getId());

                                    if (init) {
                                        BigDecimal invertedPrice = marketData.getInvertedLastPrice();
                                        
                                        try{
                                            BigDecimal lastPrice = BigDecimal.ONE.divide(invertedPrice, invertedPrice.precision(), RoundingMode.CEILING);
                                            marketData.setLastPrice(lastPrice);
                                        }catch(ArithmeticException ae){

                                        }
                                        SpectrumMarketItem newMarketItem = new SpectrumMarketItem( isFavorite, marketData, getSpectrumDataList());
                                        m_marketsList.add(newMarketItem);
                                        
                                        
                                    } else {
                                        SpectrumMarketItem item = getMarketItem(marketData.getId());
                                        if(item != null){
                                            marketData.setLastPrice(item.getLastPrice());
                                            marketData.setPoolId(item.getPoolId());
                                            marketData.setLiquidityUSD(item.getLiquidityUSD());
                                            item.marketDataProperty().set(marketData);
                                        }else{
                                            BigDecimal invertedPrice = marketData.getInvertedLastPrice();

                                            try {
                                                BigDecimal lastPrice = BigDecimal.ONE.divide(invertedPrice,
                                                    invertedPrice.precision(), RoundingMode.CEILING);
                                                    marketData.setLastPrice(lastPrice);
                                            } catch (ArithmeticException ae) {

                                            }
                                            SpectrumMarketItem newItem = new SpectrumMarketItem(isFavorite, marketData, getSpectrumDataList());
                                            m_marketsList.add(newItem);

                                        }
                                    }

                                    
                                }catch(Exception e){
                                    try {
                                        Files.writeString(logFile.toPath(), "\nSpectrumFinance(updateMarkets): " + e.toString() + " " + marketDataJson.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                    } catch (IOException e1) {
                                  
                                    }
                                }
                                
                            }

                        }
                  
                        
                    }
                    Runnable finish = () ->{
                        sort(false);
                        Platform.runLater(()->m_notConnected = false);
                        Platform.runLater(()->updateGridBox());
                        Platform.runLater(()->getLastUpdated().set(LocalDateTime.now()));
                    };
                    
                    if(m_marketsList.size() != 0){
                        m_spectrumFinance.getTickers((onTickerArray)->{
                            Object tickerSourceObject = onTickerArray.getSource().getValue();
                            if (tickerSourceObject != null && tickerSourceObject instanceof JsonArray) {
                                JsonArray tickerArray = (JsonArray) tickerSourceObject;
                             
                                synchronized(m_marketsList){

                                    for (int j = 0; j < tickerArray.size(); j++) {
                                
                                        JsonElement tickerObjectElement = tickerArray.get(j);
                                        if (tickerObjectElement != null && tickerObjectElement.isJsonObject()) {

                                            JsonObject tickerDataJson = tickerObjectElement.getAsJsonObject();

                                            JsonElement tickerIdElement = tickerDataJson.get("ticker_id");
                                            String tickerId = tickerIdElement != null && tickerIdElement.isJsonPrimitive() ? tickerIdElement.getAsString() : null;

                                            if(tickerId != null){
                                        
                                                SpectrumMarketItem spectrumItem = getMarketItem(tickerId);
                                            
                                                if(spectrumItem != null){
                                                  
                                                    JsonElement lastPriceElement = tickerDataJson.get("last_price");
                                                    JsonElement liquidityUsdElement = tickerDataJson.get("liquidity_in_usd");
                                                    JsonElement poolIdElement = tickerDataJson.get("pool_id");
                                                    if(
                                                        lastPriceElement != null && lastPriceElement.isJsonPrimitive() &&
                                                        liquidityUsdElement != null && liquidityUsdElement.isJsonPrimitive() &&
                                                        poolIdElement != null && poolIdElement.isJsonPrimitive()
                                                    ){
                                                        SpectrumMarketData spectrumData = spectrumItem.marketDataProperty().get();
                                                        spectrumData.setLastPrice(lastPriceElement.getAsBigDecimal());
                                                        spectrumData.setLiquidityUSD(liquidityUsdElement.getAsBigDecimal());
                                                        spectrumData.setPoolId(poolIdElement.getAsString());
                                                        
                                                    }
                                                }

                                            }
                                       
                                            
                                        }

                                    }
                            
                                    
                                }
                                finish.run();
                            }else{
                                finish.run();
                            }
                        }, (onTickersFailed)->{
                            finish.run();
                        });
                    }else{
                        Platform.runLater(()->m_notConnected = true);
                        Platform.runLater(()-> updateGridBox());
                        Platform.runLater(()->m_statusMsg.set("Not connected"));
                        Platform.runLater(()->getLastUpdated().set(LocalDateTime.now()));
                    }
            
               
           
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

    public SpectrumDataList getSpectrumDataList(){
        return this;
    }

    public File getDataFile(){
        return new File(m_spectrumFinance.getDataDir().getAbsolutePath() + "/" + getNetworkId() + ".dat");
    }

    private void updateFile(SecretKey oldKey, SecretKey newKey){
        

        File dataFile = getDataFile();
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
                    Files.writeString(logFile.toPath(), "\nSpectrum getfile error: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }

            }

        }
    }

    private void getFile(SecretKey secretKey) {

        File dataFile = getDataFile();
        if (dataFile != null && dataFile.isFile()) {
            try {
                JsonObject json = Utils.readJsonFile(secretKey, dataFile);
           
                if(json!= null){
           
                    openJson(json);
                }
            } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                    | IOException e) {

                try {
                    Files.writeString(logFile.toPath(), "\nSpectrum Finance getfile error: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }

            }

        }

    }


    public VBox getGridBox() {

        return m_gridBox;
    }

    public VBox getFavoriteGridBox() {
        return m_favoriteGridBox;
    }

    public void sort(){
        sort(true);
    }

    public void sort(boolean doSave) {
        synchronized(m_marketsList){
            String type = m_sortMethod.getType();
            boolean isAsc = m_sortMethod.isAsc();
            boolean swapped = m_sortMethod.isTargetSwapped();
            switch (type) {
                case SpectrumSort.SortType.LIQUIDITY_VOL:
                    if (isAsc) {
                        Collections.sort(m_marketsList, Comparator.comparing(SpectrumMarketItem::getLiquidityUSD));

                    } else {
                        Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(SpectrumMarketItem::getLiquidityUSD)));
                    }
                    break;
                case SpectrumSort.SortType.LAST_PRICE:
                    
                    if (isAsc) {
                        Collections.sort(m_marketsList, Comparator.comparing(SpectrumMarketItem::getLastPrice));

                    } else {
                        Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(SpectrumMarketItem::getLastPrice)));
                    }
                    break;
                case SpectrumSort.SortType.BASE_VOL:
                    if(!swapped){
                        if (isAsc) {
                            Collections.sort(m_marketsList, Comparator.comparing(SpectrumMarketItem::getBaseVolume));
                        } else {
                            Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(SpectrumMarketItem::getBaseVolume)));
                        }
                    }else{
                        if (isAsc) {
                            Collections.sort(m_marketsList, Comparator.comparing(SpectrumMarketItem::getQuoteVolume));
                        } else {
                            Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(SpectrumMarketItem::getQuoteVolume)));
                        }
                    }
                    break;
                case SpectrumSort.SortType.QUOTE_VOL:
                    if(!swapped){
                        if (isAsc) {
                            Collections.sort(m_marketsList, Comparator.comparing(SpectrumMarketItem::getQuoteVolume));
                        } else {
                            Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(SpectrumMarketItem::getQuoteVolume)));
                        }
                    }else{
                        if (isAsc) {
                            Collections.sort(m_marketsList, Comparator.comparing(SpectrumMarketItem::getBaseVolume));
                        } else {
                            Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(SpectrumMarketItem::getBaseVolume)));
                        }
                    }
                    break;
            

            }
        }
        int maxItems = m_marketsList.size() > 100 ? 100 : m_marketsList.size();
        Platform.runLater(()->m_statusMsg.set("Top "+maxItems+" - " + m_sortMethod.getType() + " " + (m_sortMethod.isAsc() ? "(Low to High)" : "(High to Low)")));
        if(doSave){
            save();
        }
    }

    public SpectrumSort getSortMethod(){
        return m_sortMethod;
    }

    public void setSortMethod(SpectrumSort sortMethod){
        m_sortMethod = sortMethod;
        sort();
      
    }

    public void setSearchText(String text) {
        m_searchText = text.equals("") ? null : text;

        updateGridBox();

    }

    public String getSearchText() {
        return m_searchText;
    }

    private void doSearch( EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        Task<SpectrumMarketItem[]> task = new Task<SpectrumMarketItem[]>() {
            @Override
            public SpectrumMarketItem[] call() {
                
                synchronized(m_marketsList){
                    List<SpectrumMarketItem> searchResultsList = m_marketsList.stream().filter(marketItem -> marketItem.getSymbol().contains(m_searchText.toUpperCase())).collect(Collectors.toList());

                    SpectrumMarketItem[] results = new SpectrumMarketItem[searchResultsList.size()];

                    searchResultsList.toArray(results);

                    return results;
                }
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
      
            int numFavorites = m_favoriteIds.size();

            for (int i = 0; i < numFavorites; i++) {
                String favId = m_favoriteIds.get(i);
                SpectrumMarketItem favMarketItem = getMarketItem(favId);
                m_favoriteGridBox.getChildren().add(favMarketItem.getRowBox());
            }

            if (m_searchText == null) {
                synchronized(m_marketsList){
                    int numCells = m_marketsList.size() > 100 ? 100 : m_marketsList.size();
                    for (int i = 0; i < numCells; i++) {
                        SpectrumMarketItem marketItem = m_marketsList.get(i);

                        HBox rowBox = marketItem.getRowBox();

                        m_gridBox.getChildren().add(rowBox);

                    }
                 
                }
               
            } else {

                // List<SpectrumMarketItem> searchResultsList = m_marketsList.stream().filter(marketItem -> marketItem.getSymbol().contains(m_searchText.toUpperCase())).collect(Collectors.toList());
                doSearch( onSuccess -> {
                    WorkerStateEvent event = onSuccess;
                    Object sourceObject = event.getSource().getValue();

                    if (sourceObject instanceof SpectrumMarketItem[]) {
                        SpectrumMarketItem[] searchResults = (SpectrumMarketItem[]) sourceObject;
                        int numResults = searchResults.length > 100 ? 100 : searchResults.length;

                        for (int i = 0; i < numResults; i++) {
                            SpectrumMarketItem marketItem = searchResults[i];

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
                notConnectedBtn.setFont(App.txtFont);
                notConnectedBtn.setTextFill(App.txtColor);
                notConnectedBtn.setId("menuBtn");
                notConnectedBtn.setGraphicTextGap(15);
                notConnectedBtn.setGraphic(IconButton.getIconView(new Image("/assets/cloud-offline-150.png"), 150));
                notConnectedBtn.setContentDisplay(ContentDisplay.TOP);
                notConnectedBtn.setOnAction(e -> {
                    updateMarkets();
                });

            } else {
                Button loadingBtn = new Button("Loading...");
                loadingBtn.setFont(App.txtFont);
                loadingBtn.setTextFill(Color.WHITE);
                loadingBtn.setId("transparentColor");
                loadingBtn.setGraphicTextGap(15);
                loadingBtn.setGraphic(IconButton.getIconView(new Image("/assets/spectrum-150.png"), 150));
                loadingBtn.setContentDisplay(ContentDisplay.TOP);

                imageBox.getChildren().add(loadingBtn);

            }
            m_gridBox.getChildren().add(imageBox);
        }
        
    }

    public JsonArray getFavoritesJsonArray() {
        JsonArray jsonArray = new JsonArray();
        for (String favId : m_favoriteIds) {

            jsonArray.add(favId);
            
        }
        return jsonArray;
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject jsonObject = super.getJsonObject();

        jsonObject.add("favorites", getFavoritesJsonArray());
        jsonObject.add("sortMethod", m_sortMethod.getJsonObject());
 

        return jsonObject;
    }

    private void openJson(JsonObject json) {
        if (json != null) {
            JsonElement favoritesElement = json.get("favorites");
            JsonElement sortMethodElement = json.get("sortMethod");
  

            if (favoritesElement != null && favoritesElement.isJsonArray()) {
                JsonArray favoriteJsonArray = favoritesElement.getAsJsonArray();

                for (int i = 0; i < favoriteJsonArray.size(); i++) {
                    JsonElement favoriteElement = favoriteJsonArray.get(i);
                    if(favoriteElement != null && favoriteElement.isJsonPrimitive()){
                        String id = favoriteElement.getAsString();
                        addFavorite(id, false);
                    }
                }
            }

            try{
                m_sortMethod = sortMethodElement != null && sortMethodElement.isJsonObject()
                        ? new SpectrumSort(sortMethodElement.getAsJsonObject())
                        : m_sortMethod;
            }catch(Exception e){
                m_sortMethod = new SpectrumSort();
            }
           
    

        }

    }

    public SpectrumFinance getSpectrumFinance() {
        return m_spectrumFinance;
    }

    public void save(){
        save(getNetworksData().getAppData().appKeyProperty().get());
    }

    public void save(SecretKey secretKey) {
  
        try {
           
            Utils.saveJson(secretKey, getJsonObject(), getDataFile());
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                | IOException e) {
            try {
                Files.writeString(logFile.toPath(), "\nSpectrumFinance save failed: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
        }

    }

}
