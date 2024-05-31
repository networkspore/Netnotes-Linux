package com.netnotes;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.ergoplatform.appkit.NetworkType;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;


public class SpectrumDataList extends Network implements NoteInterface {

    //private ErgoCurrencyToken m_ergoCurrency = new ErgoCurrencyToken(NetworkType.MAINNET);
    public final static String LOADING = "Loading...";

    private File logFile = new File("netnotes-log.txt");
    private SpectrumFinance m_spectrumFinance;

    private VBox m_favoriteGridBox = new VBox();
    private VBox m_gridBox = new VBox();
    
    private ArrayList<SpectrumMarketItem> m_marketsList = new ArrayList<SpectrumMarketItem>();

    private ArrayList<String> m_favoriteIds = new ArrayList<>();

    private SimpleStringProperty m_statusMsg = new SimpleStringProperty(LOADING);

    private SpectrumSort m_sortMethod = new SpectrumSort();
    private String m_searchText = null;

    private BufferedImage m_symbolImage = null;
    private Graphics2D m_symbolGraphics = null;
    private BufferedImage m_emojiLayer; 

    private BufferedImage m_imgCache = null;
    private Graphics2D m_g2d = null;
    
    private int MIN_BTN_IMG_HEIGHT = 30;
    public static final int MIN_BTN_IMG_WIDTH = 350;
    private final String m_exchangeId;

    SpectrumMarketInterface m_msgListener;

    private  ExecutorService m_singleThreadService = Executors.newSingleThreadExecutor();
    
    public ExecutorService singleThreadService(){ return m_singleThreadService; }
    
    private ImageText m_imageText = new ImageText();
    
    private SimpleObjectProperty<Network> m_tokensList = new SimpleObjectProperty<>(null);

    private SimpleObjectProperty<NoteInterface> m_currentNetwork = new SimpleObjectProperty<>();
    
    private SimpleLongProperty m_doGridUpdate = new SimpleLongProperty(0);

    public SpectrumDataList(String id, SpectrumFinance spectrumFinance) {
        
        super(null, "spectrumDataList", id+"SDLIST", spectrumFinance);
        m_spectrumFinance = spectrumFinance;
        m_exchangeId= FriendlyId.createFriendlyId();
        setup(m_spectrumFinance.getNetworksData().getAppData().appKeyProperty().get());
        
        updateTokensList();

        m_isInvert.addListener((obs,oldval,newval)->{
            SpectrumSort sortMethod = getSortMethod();
            sortMethod.setSwapTarget(newval ?  SpectrumSort.SwapMarket.SWAPPED : SpectrumSort.SwapMarket.STANDARD);
            sort();
            updateGridBox();
        }); 
    }

    public SpectrumDataList(String id, SpectrumFinance spectrumFinance, SecretKey oldval, SecretKey newval ) {
        super(null, "spectrumDataList", id+"SDLIST", spectrumFinance);
        m_exchangeId= FriendlyId.createFriendlyId();
        m_spectrumFinance = spectrumFinance;
        
        updateFile(oldval, newval);
    }

    private void setup(SecretKey secretKey) {
        updateFont();
        getFile(secretKey);
    }

    public SimpleObjectProperty<Network> tokensListNetwork(){
        return m_tokensList;
    }

    
    private java.awt.Font m_headingFont = new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 18);
    private java.awt.Font m_labelFont = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 12);
    private int m_labelAscent;
    private int m_labelLeading;
    private int m_labelHeight;
    private FontMetrics m_labelMetrics;
    private BufferedImage m_labelImg = null;
    private Graphics2D m_labelG2d = null;
    
    public void updateFont(){
    
        m_labelImg = new BufferedImage(1,1, BufferedImage.TYPE_INT_ARGB);
        m_labelG2d = m_labelImg.createGraphics();
        m_labelG2d.setFont(m_labelFont);
        m_labelMetrics = m_labelG2d.getFontMetrics();
        m_labelLeading = m_labelMetrics.getLeading();
        m_labelAscent = m_labelMetrics.getAscent();
        m_labelHeight = m_labelMetrics.getHeight();
        
 
        m_labelG2d.dispose();
        m_labelG2d = null;
        m_labelImg = null;
    }

    public FontMetrics getLabelMetrics(){
        return m_labelMetrics;
    }
    public int getLabelAscent(){
        return m_labelAscent;
    }
    public int getLabelLeading(){
        return m_labelLeading;
    }
    public int getLabelHeight(){
        return m_labelHeight;
    }

    public java.awt.Font getLabelFont(){
        return m_labelFont;
    }

    public java.awt.Font getHeadingFont(){
        return m_headingFont;
    }

    public void updateTokensList(){
        m_currentNetwork.set( getNetworksData().getNoteInterface(m_spectrumFinance.getCurrentNetworkId()));
        if(m_currentNetwork != null){
            if(m_currentNetwork.get() instanceof ErgoNetwork){
                ErgoNetwork ergoNetwork = (ErgoNetwork) m_currentNetwork.get();
                NoteInterface tokensNetwork = ergoNetwork.getNetwork(m_spectrumFinance.getTokensID());
                if(tokensNetwork != null && tokensNetwork instanceof ErgoTokens){
                    ErgoTokens ergoTokens = (ErgoTokens) tokensNetwork;
                    m_tokensList.set(ergoTokens.getTokensList(NetworkType.MAINNET));
                }else{
                    m_tokensList.set(null);
                }
            }else{
                m_tokensList.set(null);
            }
        }else{
            m_tokensList.set(null);
        }
    }
    private SimpleBooleanProperty m_isInvert = new SimpleBooleanProperty(false);

    public SimpleBooleanProperty isInvertProperty(){
        return m_isInvert;
    }
  

    public SimpleObjectProperty<NoteInterface> currentNetwork(){
        return m_currentNetwork;
    }
    
    public void updateMarkets(ArrayList<SpectrumMarketData> marketsArray) {
        
            int updateSize = marketsArray.size() ;
         
            SimpleBooleanProperty update = new SimpleBooleanProperty(false);
 
            for(int i = 0; i< updateSize ; i++){
                
                SpectrumMarketData marketData = marketsArray.get(i);
                boolean isFavorite = getIsFavorite(marketData.getId());

            
                SpectrumMarketItem item = getMarketItem(marketData.getId());
                if(item == null){
                    SpectrumMarketItem newItem = new SpectrumMarketItem(isFavorite, marketData, getSpectrumDataList());
                    m_marketsList.add(newItem);
                    newItem.init(getNetworkId());
                    update.set(true);
                }
                
            }
            if(update.get()){
                sort();
                m_doGridUpdate.set(System.currentTimeMillis());
            }
                   

    }
    
    
    public void connectToExchange(SpectrumFinance spectrum){
      


        /*ChangeListener<LocalDateTime> changeListener = (obs, oldval, newval)->{

        }; */

        
    

        m_msgListener = new SpectrumMarketInterface() {
            
            public String getId() {
                return m_exchangeId;
            }

            public void sendMessage(int msg, long timestamp){
                 if(spectrum.marketsList().size() > 0){
                    switch(msg){
                        case SpectrumFinance.LIST_CHANGED:
                        case SpectrumFinance.LIST_UPDATED:
                        
                                updateMarkets(spectrum.marketsList());
                        
                        case SpectrumFinance.STOPPED:

                        break;
                    }   
                }else{
                    statusProperty().set(ErgoMarketsData.ERROR);
                    m_doGridUpdate.set(System.currentTimeMillis());
                }
            }
            public void sendMessage(int code, long timestamp, String msg){
                switch(code){
                    case SpectrumFinance.ERROR:
                        
                        statusProperty().set("Error: " + msg);
                        m_doGridUpdate.set(System.currentTimeMillis());
                    break;
                }
            }
        };

        spectrum.addMsgListener(m_msgListener);

  
        
        shutdownNowProperty().addListener((obs, oldval, newVal) -> {
            m_marketsList.forEach((item)->{
                item.shutdown();
            });
            spectrum.removeMsgListener(m_msgListener);

            statusProperty().set(ErgoMarketsData.STOPPED);
        });
        
    }


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

    public VBox getGridBox(SimpleDoubleProperty gridWidth, SimpleDoubleProperty gridHeight, SimpleObjectProperty<TimeSpan> timeSpanObject, SimpleObjectProperty<HBox> currentBox) {
        VBox gridBox = m_gridBox;
        
        gridBox.setId("darkBox");
        Runnable updateGrid = ()->{
            m_favoriteGridBox.getChildren().clear();
            m_gridBox.getChildren().clear();
          
    
            if (m_marketsList.size() > 0) {
          
                int numFavorites = m_favoriteIds.size();
    
                for (int i = 0; i < numFavorites; i++) {
                    String favId = m_favoriteIds.get(i);
                    SpectrumMarketItem favMarketItem = getMarketItem(favId);
                    m_favoriteGridBox.getChildren().add(favMarketItem.getRowBox(gridWidth, timeSpanObject, currentBox));
                }
    
                if (m_searchText == null) {
                
                    int numCells = m_marketsList.size() > 100 ? 100 : m_marketsList.size();
                    for (int i = 0; i < numCells; i++) {
                        SpectrumMarketItem marketItem = m_marketsList.get(i);
    
                        HBox rowBox = marketItem.getRowBox(gridWidth, timeSpanObject, currentBox);
    
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
    
                                HBox rowBox = marketItem.getRowBox(gridWidth, timeSpanObject, currentBox);
    
                                m_gridBox.getChildren().add(rowBox);
                            }
                        }
                    }, onFailed -> {
                    });
    
                }
            } else {
                HBox imageBox = new HBox();
                imageBox.prefHeightProperty().bind(gridHeight);
                imageBox.setId("transparentColor");
                imageBox.setAlignment(Pos.TOP_CENTER);
                HBox.setHgrow(imageBox, Priority.ALWAYS);
                VBox.setVgrow(imageBox, Priority.ALWAYS);
                
          
                Button loadingBtn = new Button(statusProperty().get());
                loadingBtn.setFont(App.txtFont);
                loadingBtn.setTextFill(Color.WHITE);
                loadingBtn.setId("transparentColor");
                loadingBtn.setGraphicTextGap(15);
                loadingBtn.setGraphic(IconButton.getIconView(new Image("/assets/spectrum-150.png"), 150));
                loadingBtn.setContentDisplay(ContentDisplay.TOP);

                imageBox.getChildren().add(loadingBtn);

                
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

        if(m_g2d != null){
            m_g2d.dispose();
           
        }
        if(m_symbolGraphics != null){
            m_symbolGraphics.dispose();
        }
        
        m_symbolImage = null;
        m_imgCache = null;
        m_g2d = null;
        m_symbolGraphics = null;
        super.shutdown();
    }



    public void getButtonImage(SpectrumMarketData data, WritableImage wImg,int width, boolean isInvert) {
        
        int height = MIN_BTN_IMG_HEIGHT;
    
        String symbolString = String.format("%-24s", data.getCurrentSymbol(isInvert) );

        width = width < MIN_BTN_IMG_WIDTH ? MIN_BTN_IMG_WIDTH : width - 70;

       if(m_symbolImage == null ){ 
            m_symbolImage = new BufferedImage(MIN_BTN_IMG_WIDTH, height, BufferedImage.TYPE_INT_ARGB);
   
        }else{
            Drawing.fillArea(m_symbolImage, App.DEFAULT_RGBA,0,0,m_symbolImage.getWidth(), m_symbolImage.getHeight(),false );
        }
      
        int fontHeight = m_imageText.getStandarFontMetrics().getHeight();

        m_emojiLayer = m_imageText.updateLineImage(0, (height /2) - (fontHeight/2), m_symbolImage, symbolString);
        
        if(m_emojiLayer != null){
            Drawing.drawImageExact(m_symbolImage, m_emojiLayer, 0, 0, true);
        }
        m_emojiLayer = null;

        if(m_imgCache == null){
            m_imgCache = new BufferedImage(MIN_BTN_IMG_WIDTH, height, BufferedImage.TYPE_INT_ARGB);
        }else{    
            Drawing.clearImage(m_imgCache);
        }

    
        Drawing.drawImageExact(m_imgCache, m_symbolImage, 0, m_imgCache.getHeight() - m_symbolImage.getHeight(), true);


        PixelReader pR = wImg.getPixelReader();
        PixelWriter pW = wImg.getPixelWriter();
        int w = (int) m_imgCache.getWidth();
        int h = (int) m_imgCache.getHeight();
        Drawing.clearImage(wImg);

        if(m_tokensList.get()!= null && m_tokensList.get() instanceof ErgoTokensList){

            
            ErgoTokensList tokensList = (ErgoTokensList) m_tokensList.get();

            String baseTokenId =  isInvert ? data.getQuoteId() : data.getBaseId();
            String baseTokenName = isInvert ? data.getQuoteSymbol() : data.getBaseSymbol();
            int baseDecimals = isInvert ? data.getQuoteDecimals() : data.getBaseDecimals();
            ErgoNetworkToken baseToken = tokensList.getAddErgoToken(baseTokenId, baseTokenName , baseDecimals);

            String quoteTokenId =  isInvert ? data.getBaseId() : data.getQuoteId();
            String quoteTokenName =  isInvert ? data.getBaseSymbol() : data.getQuoteSymbol();
            int quoteDecimals = isInvert ? data.getBaseDecimals() : data.getQuoteDecimals();
            ErgoNetworkToken quoteToken = tokensList.getAddErgoToken(quoteTokenId, quoteTokenName, quoteDecimals);


            Image baseImage = baseToken != null ? baseToken.getIcon() : null;
            Image quoteImage = quoteToken != null ? quoteToken.getIcon() : null;
            
            if(baseImage != null){
     
                Drawing.drawImageLimit(wImg,pR,pW, baseImage,(width/2), (int)((m_imgCache.getHeight() /2) - (baseImage.getHeight() /2)), 0x30);
               
             }
             if(quoteImage != null){
                 
                 Drawing.drawImageLimit(wImg, pR, pW, quoteImage, (width/2)+50, (int)((m_imgCache.getHeight() /2) - (quoteImage.getHeight() /2)), 0x30);
                
                 
             }
        }



        Drawing.drawImageExact(wImg, pR, pW, m_imgCache, 0, 0, w, h, true);

    
    }

  
    
}
