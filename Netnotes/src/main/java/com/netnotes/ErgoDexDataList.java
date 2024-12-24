package com.netnotes;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


public class ErgoDexDataList  {

    //private ErgoCurrencyToken m_ergoCurrency = new ErgoCurrencyToken(NetworkType.MAINNET);
    public final static String LOADING = "Loading...";
    public final static String ID = "spectrumDataList";
    private ErgoDex m_ergodex;
    private String m_locationId;

    private VBox m_gridBox = new VBox();
    
    private ArrayList<ErgoDexMarketItem> m_marketsList = new ArrayList<ErgoDexMarketItem>();

    private ArrayList<String> m_favoriteIds = new ArrayList<>();

    private SimpleStringProperty m_statusMsg = new SimpleStringProperty(LOADING);

    private ErgpDexSort m_sortMethod = new ErgpDexSort();
    private String m_searchText = null;
   

    private int m_connectionStatus = App.STOPPED;
    
    public static final int MIN_BTN_IMG_WIDTH = 350;
    

    private SimpleDoubleProperty m_gridWidth;
    private SimpleDoubleProperty m_gridHeight;
    private SimpleObjectProperty<TimeSpan> m_timeSpanObject;
    private SimpleObjectProperty<NoteInterface> m_networkInterface;
    private SimpleIntegerProperty m_currentIndex;

        
    private java.awt.Font m_headingFont = new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 18);
    private java.awt.Font m_labelFont = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 12);
    private int m_labelAscent;
    private int m_labelLeading;
    private int m_labelHeight;
    private FontMetrics m_labelMetrics;
    private BufferedImage m_labelImg = null;
    private Graphics2D m_labelG2d = null;

    private NoteMsgInterface m_spectrumMsgInterface = null;
    private Stage m_appStage;
    private SimpleObjectProperty<NoteInterface> m_ergoNetworkInterfaceProperty = new SimpleObjectProperty<>(null);
    private ScrollPane m_scrollPane;
    private TextField m_updatedField;

    
    public ErgoDexDataList(String locationId,Stage appStage, ErgoDex ergoDex, SimpleDoubleProperty gridWidth, SimpleDoubleProperty gridHeight, TextField updatedField,  SimpleObjectProperty<TimeSpan> timeSpanObject, SimpleObjectProperty<NoteInterface> networkInterface, ScrollPane scrollPane) {
        m_currentIndex = new SimpleIntegerProperty(0);

        m_locationId = locationId;
        m_ergodex = ergoDex;
        m_appStage = appStage;
        setup();
        m_scrollPane = scrollPane;
        m_updatedField = updatedField;
        m_networkInterface = networkInterface;
        m_gridWidth = gridWidth;
        m_gridHeight = gridHeight;
        m_timeSpanObject = timeSpanObject;

        

        m_isInvert.addListener((obs,oldval,newval)->{
            ErgpDexSort sortMethod = getSortMethod();
            sortMethod.setSwapTarget(newval ?  ErgpDexSort.SwapMarket.SWAPPED : ErgpDexSort.SwapMarket.STANDARD);
            sort();
            updateGrid();
        });


        addDexistener();

    }


    public SimpleIntegerProperty currentIndex(){
        return m_currentIndex;
    }

    public ScrollPane getScrollPane(){
        return m_scrollPane;
    }

    public SimpleObjectProperty<NoteInterface> ergoNetworkInterfaceProperty(){
        return m_ergoNetworkInterfaceProperty;
    }
    
    public Stage appStage(){
        return m_appStage;
    }

    public SimpleObjectProperty<NoteInterface> ergoNetworkProperty(){
        return m_networkInterface;
    }

    public String getLocationId(){
        return m_locationId;
    }


    public void addDexistener(){

        m_spectrumMsgInterface = new NoteMsgInterface() {
            private String m_id = FriendlyId.createFriendlyId();
           
            public String getId(){
                return m_id;
            }

            public void sendMessage(int code, long timeStamp, String poolId, Number num){
  
                    switch(code){
                        case App.LIST_CHANGED:
                        case App.LIST_UPDATED:
                        
                            updateMarkets(m_ergodex.marketsList());
                            m_connectionStatus = App.STARTED;
                        

                        case App.STOPPED:

                        break;
                        case App.STARTED:
                            m_connectionStatus = App.STARTED;
                        case App.ERROR:
                         //   JsonElement msgElement = json != null ? json.get("msg") : null;
                            m_connectionStatus = App.ERROR;
                            
                          
                        //  getLastUpdated().set(LocalDateTime.now());
                        break;
                    } 
                
            }
        
            public void sendMessage(int code, long timestamp, String networkId, String msg){
                if(code == App.ERROR){
                    m_connectionStatus = App.ERROR;
                    m_statusMsg.set("Error: " + msg);
                }
            }

    

        };
        
        if(m_ergodex.getConnectionStatus() == App.STARTED && m_ergodex.marketsList().size() > 0){
            updateMarkets(m_ergodex.marketsList());
            m_connectionStatus = App.STARTED;
        }

        m_ergodex.addMsgListener(m_spectrumMsgInterface);
  
        
    }

    public Image getSmallAppIcon(){ 
        return null;
    }
   


    private void setup() {
        updateFont();
        getData();
    }


    
    public SimpleDoubleProperty gridWidthProperty(){
        return m_gridWidth;
    };
    public SimpleDoubleProperty gridHeightProperty(){
        return m_gridHeight;
    }
    public SimpleObjectProperty<TimeSpan> timeSpanObjectProperty(){
        return m_timeSpanObject;
    } 




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

    
    private SimpleBooleanProperty m_isInvert = new SimpleBooleanProperty(false);

    public SimpleBooleanProperty isInvertProperty(){
        return m_isInvert;
    }


  
    public TabInterface getTab(){
        return null;
    }

    public void updateMarkets(ArrayList<ErgoDexMarketData> marketsArray) {
        
            int updateSize = marketsArray.size() ;
         
            SimpleBooleanProperty update = new SimpleBooleanProperty(false);
 
            for(int i = 0; i< updateSize ; i++){
                
                ErgoDexMarketData marketData = marketsArray.get(i);
          

            
                ErgoDexMarketItem item = getMarketItem(marketData.getId());
                if(item == null){
                    ErgoDexMarketItem newItem = new ErgoDexMarketItem( marketData, getSpectrumDataList());
                    m_marketsList.add(newItem);
                    newItem.init();
                    update.set(true);
                }
                
            }
          
            if(update.get() || m_connectionStatus == App.ERROR){
                sort();
               updateGrid();
            }
                   

    }





    public void addFavorite(String id, boolean doSave) {
 
        m_favoriteIds.add(id);
        if (doSave) {
            updateGrid();
            save();
        }   
    
        
    }

    public void removeFavorite(String symbol, boolean doSave) {
        m_favoriteIds.remove(symbol);

        if (doSave) {
            updateGrid();
            save();
        }
        
    }

    public SimpleStringProperty statusMsgProperty() {
        return m_statusMsg;
    }



    public ErgoDexMarketItem getMarketItem(String id) {
        if (id != null) {
            for (ErgoDexMarketItem item : m_marketsList) {
                if (item.getId().equals(id)) {
                    return item;
                }
            }
        }
        return null;
    }

    public ErgoDexDataList getSpectrumDataList(){
        return this;
    }

    public NetworksData getNetworksData(){
        return m_ergodex.getNetworksData();
    }

 

    private void getData() {
        JsonObject json = getNetworksData().getData("data", ".", ID, ErgoDex.NETWORK_ID);
        openJson(json);
        
    }

    public VBox getGridBox() {
        VBox gridBox = m_gridBox;
        
        gridBox.setId("darkBox");
        
        updateGrid();

        return gridBox;
    }


    public void updateGrid(){
        m_gridBox.getChildren().clear();
         
        if (m_marketsList.size() > 0) {
        
            if (m_searchText == null) {
            
                int numCells = m_marketsList.size() > 100 ? 100 : m_marketsList.size();
                for (int i = 0; i < numCells; i++) {
                    ErgoDexMarketItem marketItem = m_marketsList.get(i);

                    HBox rowBox = marketItem.getRowBox();
                    marketItem.setItemIndex(i);
                    m_gridBox.getChildren().add(rowBox);

                }
                    
                
                
            } else {

                // List<SpectrumMarketItem> searchResultsList = m_marketsList.stream().filter(marketItem -> marketItem.getSymbol().contains(m_searchText.toUpperCase())).collect(Collectors.toList());
                doSearch( onSuccess -> {
                    WorkerStateEvent event = onSuccess;
                    Object sourceObject = event.getSource().getValue();

                    if (sourceObject instanceof ErgoDexMarketItem[]) {
                        ErgoDexMarketItem[] searchResults = (ErgoDexMarketItem[]) sourceObject;
                        int numResults = searchResults.length > 100 ? 100 : searchResults.length;
                        m_gridBox.getChildren().clear();
                        for (int i = 0; i < numResults; i++) {
                            ErgoDexMarketItem marketItem = searchResults[i];

                            HBox rowBox = marketItem.getRowBox();
                            marketItem.setItemIndex(i);
                            m_gridBox.getChildren().add(rowBox);
                        }
                    }
                }, onFailed -> {
                });

            }
        } else {

            ImageView imgView = new ImageView(m_ergodex.getAppIcon());
            imgView.setPreserveRatio(true);
            imgView.setFitWidth(150);

            Button loadingBtn = new Button(ErgoDex.NAME);
            loadingBtn.setTextFill(Color.WHITE);
            loadingBtn.setGraphicTextGap(20);
            loadingBtn.setId("startImageBtn");
            loadingBtn.setGraphicTextGap(15);
            loadingBtn.setGraphic(imgView);
            loadingBtn.setContentDisplay(ContentDisplay.TOP);

            VBox imageBox = new VBox(loadingBtn);
            imageBox.setId("transparentColor");
            imageBox.setAlignment(Pos.CENTER);
            HBox.setHgrow(imageBox, Priority.ALWAYS);
            imageBox.prefHeightProperty().bind(m_gridHeight.subtract(30));

            Label statusLabel = new Label();
            statusLabel.setId("italicFont");
            statusLabel.textProperty().bind(statusMsgProperty());
            

            HBox statusLabelBox = new HBox(statusLabel);
            HBox.setHgrow(statusLabelBox, Priority.ALWAYS);
            statusLabelBox.setAlignment(Pos.CENTER);


            

            //imageBox.setPadding(new Insets(0,0,40,0));


            
            m_gridBox.getChildren().addAll(imageBox,statusLabelBox);
        }

        m_updatedField.setText(Utils.formatDateTimeString( LocalDateTime.now()));
    }

    public void sort(){
        sort(true);
    }

    public void sort(boolean doSave) {
        String type = m_sortMethod.getType();
        boolean isAsc = m_sortMethod.isAsc();
        boolean swapped = m_sortMethod.isTargetSwapped();
        switch (type) {
            case ErgpDexSort.SortType.LIQUIDITY_VOL:
                if (isAsc) {
                    Collections.sort(m_marketsList, Comparator.comparing(ErgoDexMarketItem::getLiquidityUSD));

                } else {
                    Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(ErgoDexMarketItem::getLiquidityUSD)));
                }
                break;
            case ErgpDexSort.SortType.LAST_PRICE:
                
                if (isAsc) {
                    Collections.sort(m_marketsList, Comparator.comparing(ErgoDexMarketItem::getLastPrice));

                } else {
                    Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(ErgoDexMarketItem::getLastPrice)));
                }
                break;
            case ErgpDexSort.SortType.BASE_VOL:
                if(!swapped){
                    if (isAsc) {
                        Collections.sort(m_marketsList, Comparator.comparing(ErgoDexMarketItem::getBaseVolume));
                    } else {
                        Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(ErgoDexMarketItem::getBaseVolume)));
                    }
                }else{
                    if (isAsc) {
                        Collections.sort(m_marketsList, Comparator.comparing(ErgoDexMarketItem::getQuoteVolume));
                    } else {
                        Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(ErgoDexMarketItem::getQuoteVolume)));
                    }
                }
                break;
            case ErgpDexSort.SortType.QUOTE_VOL:
                if(!swapped){
                    if (isAsc) {
                        Collections.sort(m_marketsList, Comparator.comparing(ErgoDexMarketItem::getQuoteVolume));
                    } else {
                        Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(ErgoDexMarketItem::getQuoteVolume)));
                    }
                }else{
                    if (isAsc) {
                        Collections.sort(m_marketsList, Comparator.comparing(ErgoDexMarketItem::getBaseVolume));
                    } else {
                        Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(ErgoDexMarketItem::getBaseVolume)));
                    }
                }
                break;
        

        }
    
        int maxItems = m_marketsList.size() > 100 ? 100 : m_marketsList.size();
        m_statusMsg.set("Top "+maxItems+" - " + m_sortMethod.getType() + " " + (m_sortMethod.isAsc() ? "(Low to High)" : "(High to Low)"));
        if(doSave){
            save();
        }
    }

    public ErgpDexSort getSortMethod(){
        return m_sortMethod;
    }

    public void setSortMethod(ErgpDexSort sortMethod){
        m_sortMethod = sortMethod;
        sort();
      
    }

    public void setSearchText(String text) {
        m_searchText = text.equals("") ? null : text;

        updateGrid();

    }

    public String getSearchText() {
        return m_searchText;
    }

    private void doSearch( EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        Task<ErgoDexMarketItem[]> task = new Task<ErgoDexMarketItem[]>() {
            @Override
            public ErgoDexMarketItem[] call() {
                List<ErgoDexMarketItem> searchResultsList = m_marketsList.stream().filter(marketItem -> marketItem.getSymbol().toUpperCase().contains(m_searchText.toUpperCase())).collect(Collectors.toList());

                ErgoDexMarketItem[] results = new ErgoDexMarketItem[searchResultsList.size()];

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

    public JsonObject getJsonObject() {
        JsonObject jsonObject =  new JsonObject();

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

    public ErgoDex getErgoDex() {
        return m_ergodex;
    }

    public void save(){
        getNetworksData().save("data", ".", ID, ErgoDex.NETWORK_ID, getJsonObject());
    }

   
 

    public void shutdown(){
 
        m_connectionStatus = App.STOPPED;

        m_marketsList.forEach((item)->{
            item.shutdown();
        });
       
        statusMsgProperty().set(App.STATUS_STOPPED);

        m_ergodex.removeMsgListener(m_spectrumMsgInterface);
        m_spectrumMsgInterface = null;

    }



  
    
}
