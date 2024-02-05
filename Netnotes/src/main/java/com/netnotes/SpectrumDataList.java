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

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
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
        
    }

    SpectrumMarketInterface m_msgListener;
    private final String m_exchangeId = FriendlyId.createFriendlyId();
    public void updateMarkets(SpectrumMarketData[] marketsArray) {
    
        if(marketsArray != null){
            int updateSize = marketsArray.length ;
         
            boolean init = m_marketsList.size() == 0;

            for(int i = 0; i< updateSize ; i++){
                
                SpectrumMarketData marketData = marketsArray[i];
                boolean isFavorite = this.getIsFavorite(marketData.getId());

                if (init) {
                    
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
                        
                        SpectrumMarketItem newItem = new SpectrumMarketItem(isFavorite, marketData, getSpectrumDataList());
                        m_marketsList.add(newItem);

                    }
                }
            }
            sort();
         
          updateGridBox();
          statusProperty().set(ErgoMarketsData.TICKER);
           getLastUpdated().set(LocalDateTime.now());
        }else{
            try {
                Files.writeString(logFile.toPath(), "\nSpectrumDataList (null Array)", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
 
            }
        }
                             

    }
                  
                
    public void connectToExchange(SpectrumFinance spectrum){
        statusProperty().set(ErgoMarketsData.STARTED);
      

        m_msgListener = new SpectrumMarketInterface() {
            
            
          
            public String getId() {
                return m_exchangeId;
            }
        
            public void marketArrayChange(SpectrumMarketData[] dataArray) {
                
                updateMarkets(dataArray);
            }


        };

        spectrum.addMsgListener(m_msgListener);
        
        shutdownNowProperty().addListener((obs, oldval, newVal) -> {
         
            spectrum.removeMsgListener(m_msgListener);
            statusProperty().set(ErgoMarketsData.STOPPED);
        });
       // spectrum.
        
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

    public SpectrumDataList getSpectrumDataList(){
        return this;
    }

    public File getDataFile(){
        return m_spectrumFinance.getIdDataFile(getNetworkId());
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
        VBox gridBox = m_gridBox;
        updateGridBox();
        return gridBox;
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
