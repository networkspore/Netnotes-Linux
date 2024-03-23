package com.netnotes;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
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

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
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
    
    private ArrayList<SpectrumMarketItem> m_marketsList = new ArrayList<SpectrumMarketItem>();

    private ArrayList<String> m_favoriteIds = new ArrayList<>();


    private boolean m_notConnected = false;
    private SimpleStringProperty m_statusMsg = new SimpleStringProperty("Loading...");

    private SpectrumSort m_sortMethod = new SpectrumSort();
    private String m_searchText = null;

    //* */

    private BufferedImage m_symbolImage = null;
    private Graphics2D m_symbolGraphics = null;

    private BufferedImage m_imgCache = null;
    private Graphics2D m_g2d = null;
    
    private final static int MAX_ROW_IMG_WIDTH = 330;

    private int m_height = 30;
    private int m_symbolColWidth = 180;
    private int m_colPadding = 5;
    private int m_stringY = -1;

    
    private java.awt.Font m_font = new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 15);
    private java.awt.Font m_txtFont = new java.awt.Font("Deja Vu Sans", java.awt.Font.PLAIN, 15);


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
    public void updateMarkets(ArrayList<SpectrumMarketData> marketsArray) {
        
        if(marketsArray != null){
            int updateSize = marketsArray.size() ;
         
            boolean init = m_marketsList.size() == 0;

            for(int i = 0; i< updateSize ; i++){
                
                SpectrumMarketData marketData = marketsArray.get(i);
                boolean isFavorite = this.getIsFavorite(marketData.getId());

                if (init) {
                    
                    SpectrumMarketItem newMarketItem = new SpectrumMarketItem( isFavorite, marketData, getSpectrumDataList());
                    m_marketsList.add(newMarketItem);
                    
                } else {
                    SpectrumMarketItem item = getMarketItem(marketData.getId());
                    if(item != null){
                        
                        /*marketData.setLastPrice(item.getLastPrice());
                        marketData.setPoolId(item.getPoolId());
                        marketData.setLiquidityUSD(item.getLiquidityUSD());
                        //item.marketDataProperty().set(marketData);
                        marketData.getLastUpdated().set(LocalDateTime.now());
                        */
                    }else{
                        
                        SpectrumMarketItem newItem = new SpectrumMarketItem(isFavorite, marketData, getSpectrumDataList());
                        m_marketsList.add(newItem);
                        init = true;
                    }
                }
            }
            
            sort();
            m_doGridUpdate.set(System.currentTimeMillis());
            
           // sort();
         
           //m_doGridUpdate.set(System.currentTimeMillis());
            
            statusProperty().set(ErgoMarketsData.TICKER);
            getLastUpdated().set(LocalDateTime.now());
        }else{
     
        }
                   

    }
    
    
    
    public void connectToExchange(SpectrumFinance spectrum){
      

        ChangeListener<LocalDateTime> listUpdateListener = (obs,oldval,newval)->{
            updateMarkets(spectrum.marketsList());
        };

        /*ChangeListener<LocalDateTime> changeListener = (obs, oldval, newval)->{

        }; */

        m_msgListener = new SpectrumMarketInterface() {
            
            public String getId() {
                return m_exchangeId;
            }

        };

        spectrum.addMsgListener(m_msgListener);
        

        //updateMarkets(spectrum);

        spectrum.listUpdated().addListener(listUpdateListener);

        if(spectrum.marketsList().size() > 0){
            updateMarkets(spectrum.marketsList());
        }
        
        shutdownNowProperty().addListener((obs, oldval, newVal) -> {
     
            spectrum.listUpdated().removeListener(listUpdateListener);
            spectrum.removeMsgListener(m_msgListener);
            statusProperty().set(ErgoMarketsData.STOPPED);

        });
        
    }

    private static volatile boolean m_shuttingDown = false;

    public boolean getIsFavorite(String id){
        return m_favoriteIds.contains(id);
    }

    public void updateGridBox(){
        m_doGridUpdate.set(System.currentTimeMillis());
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
            for (SpectrumMarketItem item : m_marketsList) {
                if (item.getId().equals(id)) {
                    return item;
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

    private SimpleLongProperty m_doGridUpdate = new SimpleLongProperty(0);

    public VBox getGridBox() {
        VBox gridBox = m_gridBox;
        Runnable updateGrid = ()->{
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
                
                    int numCells = m_marketsList.size() > 100 ? 100 : m_marketsList.size();
                    for (int i = 0; i < numCells; i++) {
                        SpectrumMarketItem marketItem = m_marketsList.get(i);
    
                        HBox rowBox = marketItem.getRowBox();
    
                        m_gridBox.getChildren().add(rowBox);
    
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
             
        };
        updateGrid.run();

        m_doGridUpdate.addListener((obs,oldval,newval)->{
            updateGrid.run();
        });
        return gridBox;
    }

    public VBox getFavoriteGridBox() {
        return m_favoriteGridBox;
    }

    public void sort(){
        sort(true);
    }

    public void sort(boolean doSave) {
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
            public SpectrumMarketItem[] call() {List<SpectrumMarketItem> searchResultsList = m_marketsList.stream().filter(marketItem -> marketItem.getSymbol().contains(m_searchText.toUpperCase())).collect(Collectors.toList());

                SpectrumMarketItem[] results = new SpectrumMarketItem[searchResultsList.size()];

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

    @Override
    public void shutdown(){
        m_shuttingDown = true;
        m_g2d.dispose();
        m_symbolGraphics.dispose();
        m_symbolImage = null;
        m_imgCache = null;
        m_g2d = null;
        m_symbolGraphics = null;
        super.shutdown();
    }


    public Image getButtonImage(SpectrumMarketData data) {
        if(m_shuttingDown){
            return null;
        }
        
        boolean isInvert = data.getDefaultInvert() ? ! getSortMethod().isTargetSwapped() : getSortMethod().isTargetSwapped();
        
        String symbolString = String.format("%-18s", data.getCurrentSymbol(isInvert) );
        String priceString = isInvert ? data.getInvertedLastPrice().toString() : data.getLastPrice().toString();


       if(m_symbolImage == null){
        
            m_symbolImage = new BufferedImage(m_symbolColWidth, m_height, BufferedImage.TYPE_INT_ARGB);
            m_symbolGraphics = m_symbolImage.createGraphics();
            m_symbolGraphics.setFont(m_txtFont);
            FontMetrics fm = m_symbolGraphics.getFontMetrics();        
            int fontAscent = fm.getAscent();
            int fontHeight = fm.getHeight();
            m_stringY = ((m_height - fontHeight) / 2) + fontAscent;

            m_symbolGraphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            m_symbolGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            m_symbolGraphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            m_symbolGraphics.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
            m_symbolGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            m_symbolGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            m_symbolGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            m_symbolGraphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            m_symbolGraphics.setColor(SpectrumMarketItem.WHITE_COLOR);
            
        }else{
            Drawing.fillArea(m_symbolImage, App.DEFAULT_RGBA,0,0,m_symbolImage.getWidth(), m_symbolImage.getHeight(),false );
        }
         
        m_symbolGraphics.drawString(symbolString, 0, m_stringY);

        if(m_imgCache == null){
            m_imgCache = new BufferedImage(MAX_ROW_IMG_WIDTH, m_height, BufferedImage.TYPE_INT_ARGB);

            m_g2d = m_imgCache.createGraphics();
            m_g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            m_g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            m_g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            m_g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
            m_g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            m_g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            m_g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            m_g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            m_g2d.setFont(m_font);

        }else{    
            Drawing.fillArea(m_imgCache, App.DEFAULT_RGBA,0,0,m_imgCache.getWidth(), m_imgCache.getHeight(),false);
        }

        Drawing.drawImageExact(m_imgCache, m_symbolImage, 0, 0, false);

        m_g2d.drawString(priceString, m_symbolColWidth + m_colPadding, m_stringY);

       /* } else {

            g2d.drawString(lastString, symbolWidth + m_colPadding, m_stringY);

            int x1 = symbolWidth + m_colPadding;
            int y1 = (height / 2) - (fontHeight / 2);
            int x2 = x1 + lastWidth;
            int y2 = y1 + fontHeight;
            java.awt.Color color1 = positive ? KucoinExchange.POSITIVE_COLOR : KucoinExchange.NEGATIVE_HIGHLIGHT_COLOR;
            java.awt.Color color2 = positive ? KucoinExchange.POSITIVE_HIGHLIGHT_COLOR : KucoinExchange.NEGATIVE_COLOR;

            Drawing.drawBarFillColor(positive ? 0 : 1, false, FILL_COLOR, color1.getRGB(), color2.getRGB(), img, x1, y1, x2, y2);

        }*/

       // g2d.dispose();

        return SwingFXUtils.toFXImage(m_imgCache, null);
    }

}
