package com.netnotes;


import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.reactfx.util.FxTimer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener.Change;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SpectrumFinance extends Network implements NoteInterface {

    public static String DESCRIPTION = "Spectrum Finance is a cross-chain decentralized exchange (DEX).";
    public static String SUMMARY = "";
    public static String NAME = "Spectrum Finance";
    public final static String NETWORK_ID = "SPECTRUM_FINANCE";

    public static String API_URL = "https://api.spectrum.fi";


    public static java.awt.Color POSITIVE_HIGHLIGHT_COLOR = new java.awt.Color(0xff3dd9a4, true);
    public static java.awt.Color POSITIVE_COLOR = new java.awt.Color(0xff028A0F, true);

    public static java.awt.Color NEGATIVE_COLOR = new java.awt.Color(0xff9A2A2A, true);
    public static java.awt.Color NEGATIVE_HIGHLIGHT_COLOR = new java.awt.Color(0xffe96d71, true);
    public static java.awt.Color NEUTRAL_COLOR = new java.awt.Color(0x111111);

    

    public static long DATA_TIMEOUT_SPAN = (15*1000)-100;
    public static long TICKER_DATA_TIMEOUT_SPAN = 1000*60;
    private File m_appDir = null;
   // private File m_dataFile = null;


    private Stage m_appStage = null;

    public static final String MARKET_DATA_ID = "marketData";
    public static final String TICKER_DATA_ID = "tickerData";

    private SimpleObjectProperty<JsonObject> m_cmdObjectProperty = new SimpleObjectProperty<>(null);

    private ArrayList<String> m_favoriteIds = new ArrayList<>();
    private ArrayList<SpectrumMarketData> m_marketsList = new ArrayList<>();

    private SimpleObjectProperty<LocalDateTime> m_listUpdated = new SimpleObjectProperty<>(LocalDateTime.now());
    private SimpleObjectProperty<LocalDateTime> m_listChanged = new SimpleObjectProperty<>(null);
//    private AtomicLong m_marketsLastChecked = new AtomicLong(0);
//    private AtomicLong m_tickersLastChecked = new AtomicLong(0);

    
    private ScheduledExecutorService m_schedualedExecutor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> m_scheduledFuture = null;

    private boolean m_isMax = false;
    private double m_prevHeight = -1;
    private double m_prevX = -1;
    private double m_prevY = -1;
    private int m_listenerSize = 0;

    //private SimpleObjectProperty<JsonArray> m_marketJson = new SimpleObjectProperty<>(null);


    public ArrayList<SpectrumMarketData> marketsList(){
        return m_marketsList;
    }

    public SimpleObjectProperty<LocalDateTime> listUpdated(){
        return m_listUpdated;
    }

    public SimpleObjectProperty<LocalDateTime> listChanged(){
        return m_listChanged;
    }
    

    public SpectrumFinance(NetworksData networksData) {
        this(null, networksData);
        setup(null);
        addListeners();
    }

    public SpectrumFinance(JsonObject jsonObject, NetworksData networksData) {
        super(getAppIcon(), NAME, NETWORK_ID, networksData);

        setup(jsonObject);
        addListeners();
    }

    public void addListeners(){
        getNetworksData().getAppData().appKeyProperty().addListener((obs,oldval,newval)->{
            File indexFile = getIdIndexFile();
            if(indexFile != null && indexFile.isFile()){
                JsonArray jsonArray = getIndexFileArray(oldval, indexFile);
                saveIndexFile(jsonArray);
                if(jsonArray != null){
                int size = jsonArray.size();
                    for(int i = 0; i< size ; i++){
                        JsonElement jsonElement = jsonArray.get(i);
                
                        JsonObject obj = jsonElement.getAsJsonObject();

                        File file = new File(obj.get("file").getAsString());
                        if(file.isFile()){
                            try {
                                String oldString = Utils.readStringFile(oldval, file);
                                Utils.writeEncryptedString(newval, file, oldString);
                            } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                                    | IOException e) {
                                try {
                                    Files.writeString(App.logFile.toPath(), "SpectrumFinance (addListenersr): " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                } catch (IOException e1) {
                                
                                }
                            }
                        }
                    }
                }
            }     
        });
     
    }

    public SimpleObjectProperty<JsonObject> cmdObjectProperty() {

        return m_cmdObjectProperty;
    }



  

    public File getAppDir() {
        return m_appDir;
    }

    public static Image getAppIcon() {
        return new Image("/assets/spectrum-150.png");
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/spectrumFinance.png");
    }



    private void setup(JsonObject jsonObject) {


        String appDirFileString = null;
        if (jsonObject != null) {
            JsonElement appDirElement = jsonObject.get("appDir");
      
          //  JsonElement dataFileElement = jsonObject.get("dataFile");

        //    fileString = dataFileElement == null ? null : dataFileElement.toString();

            appDirFileString = appDirElement == null ? null : appDirElement.getAsString();

        }

        m_appDir = appDirFileString == null ? new File(getNetworksData().getAppDir().getAbsolutePath() + "/" + NAME) : new File(appDirFileString);

        if (!m_appDir.isDirectory()) {

            try {
                Files.createDirectories(m_appDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }

        }

   

        setDataDir(new File(m_appDir.getAbsolutePath() + "/data"));
        getDataDir();

    }

    public void open() {
        super.open();
      
        showAppStage();
    }

    public Stage getAppStage() {
        return m_appStage;
    }

    public SpectrumMarketData[] getMarketDataArray(){
        SpectrumMarketData[] dataArray = new SpectrumMarketData[m_marketsList.size()];
        return dataArray = m_marketsList.toArray(dataArray);
    }


    public SpectrumMarketData getMarketDataBySymbols(String baseSymbol, String quoteSymbol) {
        
        if (m_marketsList.size() > 0 && baseSymbol != null && quoteSymbol != null) {
            SpectrumMarketData[] dataArray = getMarketDataArray();

            for (SpectrumMarketData data : dataArray) {
           
                if (data.getBaseSymbol().equals(baseSymbol) && data.getQuoteSymbol().equals(quoteSymbol) ) {
                    return data;
                }
            }
            
        }
        return null;
    }

    public PriceQuote getPriceQuoteById(String baseId, String quoteId){
        if (m_marketsList.size() > 0 && baseId != null && quoteId != null) {
            SpectrumMarketData[] dataArray = getMarketDataArray();
            
            for (int i = 0; i < dataArray.length ; i++) {
                SpectrumMarketData data = m_marketsList.get(i);

                if (data.getBaseId().equals(baseId) && data.getQuoteId().equals(quoteId)) {
                    
                    return data;
                }
            }
            
        }
        return null;
    }
    public static SpectrumMarketData getMarketDataById(ArrayList<SpectrumMarketData> dataList, String id) {
        if (id != null) {
            for (SpectrumMarketData data : dataList) {
                if (data.getId().equals(id) ) {
                    return data;
                }
            }
            
        }
        return null;
    }

    public static int getMarketDataIndexById(ArrayList<SpectrumMarketData> dataList, String id) {
        if (id != null) {
            int size = dataList.size();
            for (int i = 0; i < size; i++) {
                SpectrumMarketData data = dataList.get(i);
                if (data.getId().equals(id) ) {
                    return i;
                }
            }
            
        }
        return -1;
    }

 
    public boolean getIsFavorite(String id){
        return m_favoriteIds.contains(id);
    }


    public void addFavorite(String id, boolean doSave) {
 
        m_favoriteIds.add(id);
        if (doSave) {
            getLastUpdated().set(LocalDateTime.now());
        }   
    }

    public void removeFavorite(String symbol, boolean doSave) {
        m_favoriteIds.remove(symbol);

        if (doSave) {
            getLastUpdated().set(LocalDateTime.now());
        }
        
    }


    
    /*public SpectrumMarketItem getMarketItem(String id) {
        if (id != null) {
            
            for (SpectrumMarketItem item : m_marketsList) {
                if (item.getId().equals(id)) {
                    return item;
                }
            }
            
        }
        return null;
    }*/




    private void showAppStage() {
        if (m_appStage == null) {
            

            SpectrumDataList spectrumData = new SpectrumDataList(getNetworkId(), this);

            double appStageWidth = 450;
            double appStageHeight = 600;

            m_appStage = new Stage();
            m_appStage.getIcons().add(SpectrumFinance.getSmallAppIcon());
            m_appStage.initStyle(StageStyle.UNDECORATED);
            m_appStage.setTitle(NAME);

            Button closeBtn = new Button();

            Runnable runClose = () -> {
                
        
                spectrumData.shutdown();
                spectrumData.removeUpdateListener();

                m_appStage = null;

            };

            closeBtn.setOnAction(closeEvent -> {

                m_appStage.close();
                runClose.run();
            });

            Button maxBtn = new Button();

            HBox titleBox = App.createTopBar(getSmallAppIcon(), maxBtn, closeBtn, m_appStage);
            titleBox.setPadding(new Insets(7, 8, 5, 10));

            m_appStage.titleProperty().bind(Bindings.concat(NAME, " - ", spectrumData.statusProperty()));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Tooltip refreshTip = new Tooltip("Refresh");
            refreshTip.setShowDelay(new javafx.util.Duration(100));
            refreshTip.setFont(App.txtFont);

         

            BufferedMenuButton sortTypeButton = new BufferedMenuButton("/assets/filter.png", App.MENU_BAR_IMAGE_WIDTH);

            MenuItem sortLiquidityItem = new MenuItem(SpectrumSort.SortType.LIQUIDITY_VOL);
            MenuItem sortBaseVolItem = new MenuItem(SpectrumSort.SortType.BASE_VOL);
            MenuItem sortQuoteVolItem = new MenuItem(SpectrumSort.SortType.QUOTE_VOL);
            MenuItem sortLastPriceItem = new MenuItem(SpectrumSort.SortType.LAST_PRICE);
          
            sortTypeButton.getItems().addAll(sortLiquidityItem, sortBaseVolItem, sortQuoteVolItem, sortLastPriceItem);

            Runnable updateSortTypeSelected = () ->{
                sortLiquidityItem.setId(null);
                sortBaseVolItem.setId(null);
                sortQuoteVolItem.setId(null);
                sortLastPriceItem.setId(null);

                switch(spectrumData.getSortMethod().getType()){
                    case SpectrumSort.SortType.LIQUIDITY_VOL:
                        sortLiquidityItem.setId("selectedMenuItem");
                    break;
                    case SpectrumSort.SortType.BASE_VOL:
                        sortBaseVolItem.setId("selectedMenuItem");
                    break;
                    case SpectrumSort.SortType.QUOTE_VOL:
                        sortQuoteVolItem.setId("selectedMenuItem");
                    break;
                    case SpectrumSort.SortType.LAST_PRICE:
                        sortLastPriceItem.setId("selectedMenuItem");
                    break;
                }

                spectrumData.sort();
                spectrumData.updateGridBox();
                spectrumData.getLastUpdated().set(LocalDateTime.now());
            };

           // updateSortTypeSelected.run();

            sortLiquidityItem.setOnAction(e->{
                SpectrumSort sortMethod = spectrumData.getSortMethod();
                sortMethod.setType(sortLiquidityItem.getText());
                updateSortTypeSelected.run();
            });

            sortBaseVolItem.setOnAction(e->{
                SpectrumSort sortMethod = spectrumData.getSortMethod();
                sortMethod.setType(sortBaseVolItem.getText());
                updateSortTypeSelected.run();
            });

            sortQuoteVolItem.setOnAction(e->{
                SpectrumSort sortMethod = spectrumData.getSortMethod();
                sortMethod.setType(sortQuoteVolItem.getText());
                updateSortTypeSelected.run();
            });

            sortLastPriceItem.setOnAction(e->{
                SpectrumSort sortMethod = spectrumData.getSortMethod();
                sortMethod.setType(sortLastPriceItem.getText());
                updateSortTypeSelected.run();
            });


            BufferedButton sortDirectionButton = new BufferedButton(spectrumData.getSortMethod().isAsc() ? "/assets/sortAsc.png" : "/assets/sortDsc.png", App.MENU_BAR_IMAGE_WIDTH);
            sortDirectionButton.setOnAction(e->{
                SpectrumSort sortMethod = spectrumData.getSortMethod();
                sortMethod.setDirection(sortMethod.isAsc() ? SpectrumSort.SortDirection.DSC : SpectrumSort.SortDirection.ASC);
                sortDirectionButton.setImage(new Image(sortMethod.isAsc() ? "/assets/sortAsc.png" : "/assets/sortDsc.png"));
                spectrumData.sort();
                spectrumData.updateGridBox();
                spectrumData.getLastUpdated().set(LocalDateTime.now());
            });

            BufferedButton swapTargetButton = new BufferedButton(spectrumData.getSortMethod().isTargetSwapped()? "/assets/targetSwapped.png" : "/assets/targetStandard.png", App.MENU_BAR_IMAGE_WIDTH);
            swapTargetButton.setOnAction(e->{
                SpectrumSort sortMethod = spectrumData.getSortMethod();
                sortMethod.setSwapTarget(sortMethod.isTargetSwapped() ? SpectrumSort.SwapMarket.STANDARD : SpectrumSort.SwapMarket.SWAPPED);
                swapTargetButton.setImage(new Image(spectrumData.getSortMethod().isTargetSwapped()? "/assets/targetSwapped.png" : "/assets/targetStandard.png"));
                spectrumData.sort();
                spectrumData.updateGridBox();
                spectrumData.getLastUpdated().set(LocalDateTime.now());

            });



            TextField searchField = new TextField();
            searchField.setPromptText("Search");
            searchField.setId("urlField");
            searchField.setPrefWidth(200);
            searchField.setPadding(new Insets(2, 10, 3, 10));
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                spectrumData.setSearchText(searchField.getText());
            });

            Region menuBarRegion = new Region();
            HBox.setHgrow(menuBarRegion, Priority.ALWAYS);

            HBox menuBar = new HBox(sortTypeButton,sortDirectionButton,swapTargetButton, menuBarRegion, searchField);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 10, 1, 5));

            VBox favoritesVBox = spectrumData.getFavoriteGridBox();

            ScrollPane favoriteScroll = new ScrollPane(favoritesVBox);
            favoriteScroll.setPadding(new Insets(5, 0, 5, 5));
            favoriteScroll.setId("bodyBox");

            VBox chartList = spectrumData.getGridBox();

            ScrollPane scrollPane = new ScrollPane(chartList);
            scrollPane.setPadding(SMALL_INSETS);
            scrollPane.setId("bodyBox");

            VBox bodyPaddingBox = new VBox(scrollPane);
            bodyPaddingBox.setPadding(SMALL_INSETS);

            Font smallerFont = Font.font("OCR A Extended", 10);

            Text lastUpdatedTxt = new Text("Updated ");
            lastUpdatedTxt.setFill(App.formFieldColor);
            lastUpdatedTxt.setFont(smallerFont);

            TextField lastUpdatedField = new TextField();
            lastUpdatedField.setEditable(false);
            lastUpdatedField.setId("formField");
            lastUpdatedField.setFont(smallerFont);
            lastUpdatedField.setPrefWidth(165);

            HBox lastUpdatedBox = new HBox(lastUpdatedTxt, lastUpdatedField);
            lastUpdatedBox.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(lastUpdatedBox, Priority.ALWAYS);

            VBox footerVBox = new VBox(lastUpdatedBox);
            HBox.setHgrow(footerVBox, Priority.ALWAYS);
            footerVBox.setPadding(SMALL_INSETS);

            VBox headerVBox = new VBox(titleBox);
            headerVBox.setPadding(new Insets(0, 5, 0, 5));
            headerVBox.setAlignment(Pos.TOP_CENTER);

            VBox layoutBox = new VBox(headerVBox, bodyPaddingBox, footerVBox);

            HBox menuPaddingBox = new HBox(menuBar);
            menuPaddingBox.setPadding(new Insets(0, 0, 0, 0));

            spectrumData.statusProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !(newVal.equals("Loading..."))) {
                    if (!headerVBox.getChildren().contains(menuPaddingBox)) {
                        headerVBox.getChildren().add(1, menuPaddingBox);

                    }

                } else {
                    if (headerVBox.getChildren().contains(menuPaddingBox)) {
                        headerVBox.getChildren().remove(menuPaddingBox);

                    }

                }
            });

            favoritesVBox.getChildren().addListener((Change<? extends Node> changeEvent) -> {
                int numFavorites = favoritesVBox.getChildren().size();
                if (numFavorites > 0) {
                    if (!headerVBox.getChildren().contains(favoriteScroll)) {

                        headerVBox.getChildren().add(favoriteScroll);
                        menuPaddingBox.setPadding(new Insets(0, 0, 5, 0));
                    }
                    int favoritesHeight = numFavorites * 40 + 21;
                    favoriteScroll.setPrefViewportHeight(favoritesHeight > 175 ? 175 : favoritesHeight);
                } else {
                    if (headerVBox.getChildren().contains(favoriteScroll)) {

                        headerVBox.getChildren().remove(favoriteScroll);
                        menuPaddingBox.setPadding(new Insets(0, 0, 0, 0));
                    }
                }
            });

            Scene appScene = new Scene(layoutBox, appStageWidth, appStageHeight);
            appScene.setFill(null);
            appScene.getStylesheets().add("/css/startWindow.css");
            m_appStage.setScene(appScene);
            m_appStage.show();

            bodyPaddingBox.prefWidthProperty().bind(m_appStage.widthProperty());
            scrollPane.prefViewportWidthProperty().bind(m_appStage.widthProperty());

            favoriteScroll.prefViewportWidthProperty().bind(m_appStage.widthProperty());

            scrollPane.prefViewportHeightProperty().bind(m_appStage.heightProperty().subtract(headerVBox.heightProperty()).subtract(footerVBox.heightProperty()));

            chartList.prefWidthProperty().bind(scrollPane.prefViewportWidthProperty().subtract(40));
            favoritesVBox.prefWidthProperty().bind(favoriteScroll.prefViewportWidthProperty().subtract(40));

            spectrumData.getLastUpdated().addListener((obs, oldVal, newVal) -> {
               // refreshBtn.setDisable(false);
             //   refreshBtn.setImage(new Image("/assets/refresh-white-30.png"));
                String dateString = Utils.formatDateTimeString(newVal);

                lastUpdatedField.setText(dateString);
            });

        

            ResizeHelper.addResizeListener(m_appStage, 250, 300, 400, Double.MAX_VALUE);
   
            maxBtn.setOnAction(e -> {
                if(m_isMax){
                    
                    m_appStage.setX(m_prevX);
                    m_appStage.setHeight(m_prevHeight);
                    m_appStage.setY(m_prevY);
                    m_prevX = -1;
                    m_prevY = -1;
                    m_prevHeight = -1;
                    m_isMax = false;
                }else{
                    m_isMax = true;
                    m_prevY = m_appStage.getY();
                    m_prevX = m_appStage.getX();
                    m_prevHeight = m_appStage.getScene().getHeight();
                    m_appStage.setMaximized(true);
                    FxTimer.runLater(Duration.ofMillis(100), ()->{
                        double height = m_appStage.getScene().getHeight();
                       // double width = m_appStage.getScene().getWidth();
                        double x = m_appStage.getX();
                        double y = m_appStage.getY();
                        m_appStage.setMaximized(false);
                        FxTimer.runLater(Duration.ofMillis(100), ()->{
                            m_appStage.setX(x);
                            m_appStage.setY(y);
                            m_appStage.setHeight(height);
                        });
                    });
                }
            });

            
            FxTimer.runLater(Duration.ofMillis(100), ()->Platform.runLater(()->spectrumData.connectToExchange(this)));
            

            m_appStage.setOnCloseRequest(e -> runClose.run());

        } else {
            m_appStage.show();
        }
    }


    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

    
        return true;
    }

  
    public ExecutorService getExecService(){
        return getNetworksData().getExecService();
    }

    private void getMarkets() {
        String urlString = API_URL + "/v1/price-tracking/markets";

        Utils.getUrlJsonArray(urlString, getExecService(), success -> {


            Object sourceObject = success.getSource().getValue();
            if (sourceObject != null && sourceObject instanceof JsonArray) {
                JsonArray marketJsonArray = (JsonArray) sourceObject;
             
                getMarketUpdate(marketJsonArray);
            } 
        }, (onfailed)->{
            try {
                Files.writeString(App.logFile.toPath(), "getMarketsDataArray failed:" + onfailed.getSource().getException().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                
            }
        }, null);
                        
    }
  
      
    public void getTickers(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/v1/price-tracking/cg/tickers";

        Utils.getUrlJsonArray(urlString, getNetworksData().getExecService(), onSucceeded, onFailed, null);                

    }

    

    private void getMarketUpdate(JsonArray jsonArray){
        
        ArrayList<SpectrumMarketData> tmpMarketsList = new ArrayList<>();

        SimpleBooleanProperty isChanged = new SimpleBooleanProperty(false);
        for (int i = 0; i < jsonArray.size(); i++) {
    
            JsonElement marketObjectElement = jsonArray.get(i);
            if (marketObjectElement != null && marketObjectElement.isJsonObject()) {

                JsonObject marketDataJson = marketObjectElement.getAsJsonObject();
                
                try{
                    
                    SpectrumMarketData marketData = new SpectrumMarketData(marketDataJson);
                    int marketIndex = getMarketDataIndexById(tmpMarketsList, marketData.getId());
                    

                    if(marketIndex != -1){
                        SpectrumMarketData lastData = tmpMarketsList.get(marketIndex);
                        BigDecimal quoteVolume = lastData.getQuoteVolume();
                        if(marketData.getQuoteVolume().doubleValue() > quoteVolume.doubleValue()){
                            tmpMarketsList.set(marketIndex, marketData);
                        }
                    }else{
                        isChanged.set(true);
                        tmpMarketsList.add(marketData);
                    }

                    
                    
                }catch(Exception e){
                    try {
                        Files.writeString(App.logFile.toPath(), "\nSpectrumFinance(updateMarkets): " + e.toString() + " " + marketDataJson.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e1) {
                    
                    }
                }
                
            }

        }
      
        
        if(tmpMarketsList.size() != 0){
            getTickers((onTickerArray)->{
                Object tickerSourceObject = onTickerArray.getSource().getValue();
                if (tickerSourceObject != null && tickerSourceObject instanceof JsonArray) {
                    JsonArray tickerArray = (JsonArray) tickerSourceObject;

         
                    
                    for (int j = 0; j < tickerArray.size(); j++) {
                
                        JsonElement tickerObjectElement = tickerArray.get(j);
                        if (tickerObjectElement != null && tickerObjectElement.isJsonObject()) {

                            JsonObject tickerDataJson = tickerObjectElement.getAsJsonObject();

                            JsonElement tickerIdElement = tickerDataJson.get("ticker_id");
                            String tickerId = tickerIdElement != null && tickerIdElement.isJsonPrimitive() ? tickerIdElement.getAsString() : null;

                            if(tickerId != null){
                        
                                SpectrumMarketData marketData = getMarketDataById(tmpMarketsList, tickerId);
                            
                                if(marketData != null){
                                    
                                
                                    JsonElement liquidityUsdElement = tickerDataJson.get("liquidity_in_usd");
                                    JsonElement poolIdElement = tickerDataJson.get("pool_id");
                                    if(
                                    
                                        liquidityUsdElement != null && liquidityUsdElement.isJsonPrimitive() &&
                                        poolIdElement != null && poolIdElement.isJsonPrimitive()
                                    ){
                                        
                                    
                                        marketData.setLiquidityUSD(liquidityUsdElement.getAsBigDecimal());
                                        marketData.setPoolId(poolIdElement.getAsString());
                                        
                                    }
                                }

                            }
                        
                            
                        }

                    }
                    updateMarketList(tmpMarketsList);
                   // Platform.runLater(()->updateMarketArray(completeDataArray));

                }
            }, (onTickersFailed)->{
                try {
                    Files.writeString(App.logFile.toPath(), "\nSpectrumFinance (onTickersFailed): " + onTickersFailed.getSource().getException().toString() , StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                   
                }
             
            });
        }
    }
    
    private void updateMarketList(ArrayList<SpectrumMarketData> data){
        int size = data.size();

        if(m_marketsList.size() == 0){
            for(int i = 0; i < size; i++){
                SpectrumMarketData marketData = data.get(i);
                m_marketsList.add(marketData);
            }
            data.clear();
            listUpdated().set(LocalDateTime.now());
        }else{
            for(int i = 0; i < size; i++){
                SpectrumMarketData newMarketData = data.get(i);
                SpectrumMarketData marketData = getMarketDataById(m_marketsList, newMarketData.getId());
                marketData.update(newMarketData);
            }
            data.clear();
        }
    }
    
    /*
    public void getTickers(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        
        long currentTime = System.currentTimeMillis();
        long lastChecked = m_tickersLastChecked.getAndSet(currentTime);
        File tickersFile = getIdDataFile(TICKER_DATA_ID);

        if(lastChecked != 0 && (currentTime -lastChecked) < TICKER_DATA_TIMEOUT_SPAN && tickersFile.isFile() && tickersFile.length() > 50 ){
            
            try{
                String fileString = Utils.readStringFile(getAppKey(), tickersFile);
        
                Utils.returnObject(new JsonParser().parse(fileString).getAsJsonArray(), onSucceeded, onSucceeded);

            }catch(IOException | InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e){
                try {
                    Files.writeString(App.logFile.toPath(), "\nSpectrumFinance: getTickersMarkets: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
                getTickersLocal(onSucceeded, onFailed);
            }

        }else{
             getTickersLocal(onSucceeded, onFailed);
        }
        
    }*/
    //

    //https://api.spectrum.fi/v1/history/mempool
    /*
     * POST JsonArray [address]
     */
    public void getMemPoolHistory(String address, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/v1/history/mempool";
        
        Utils.getUrlJsonArray(urlString, getExecService(), onSucceeded, onFailed, null);

    }

    //https://api.spectrum.fi/v1/history/order
    /* POST
        "addresses": 
        [

            "9gEwsJePmqhCXwdtCWVhvoRUgNsnpgWkFQ2kFhLwYhRwW7tMc61"

        ],
        "orderType": "Swap",
        "orderStatus": "Evaluated",
        "txId": "00000111ba9e273590f73830aaeb9ccbb7e75fb57d9d2d3fb1b6482013b2c38f",
        "tokenIds": 
        [

            "0000000000000000000000000000000000000000000000000000000000000000"

        ],
        "tokenPair": 
        {

            "x": "0000000000000000000000000000000000000000000000000000000000000000",
            "y": "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04"

        }
     */
    public void getOrderHistory(String address, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/v1/history/order";
        
        Utils.getUrlJsonArray(urlString, getExecService(), onSucceeded, onFailed, null);

    }


    // https://api.spectrum.fi/v1/history/addresses?offset=0&limit=100
    /*
        offset	
        integer <int32> >= 0
        limit	
        integer <int32> [ 1 .. 100 ] 
    
    */
    public void getAddressesHistory(int offset, int limit, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/v1/history/addresses?offset="+offset + "&limit=" + limit;
        
        Utils.getUrlJsonArray(urlString, getExecService(), onSucceeded, onFailed, null);

    }


    /*https://api.spectrum.fi/v1/lm/pools/stats
        "poolId": "9916d75132593c8b07fe18bd8d583bda1652eed7565cf41a4738ddd90fc992ec",
        "compoundedReward": 75,
        "yearProfit": 3700
    */
    public void getPoolStats(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
            String urlString = API_URL + "/v1/lm/pools/stats";
       
            
            Utils.getUrlJsonArray(urlString, getExecService(), onSucceeded, onFailed, null);
    
    }

    public void getPoolChart(String poolId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/v1/amm/pool/" + poolId + "/chart";
   
        
        Utils.getUrlJsonArray(urlString, getExecService(), onSucceeded, onFailed, null);

    }
    
    private ObservableList<SpectrumMarketInterface> m_msgListeners = FXCollections.observableArrayList();
    private SimpleIntegerProperty m_connectionStatus = new SimpleIntegerProperty(0);

    public void addMsgListener(SpectrumMarketInterface item) {
        if (!m_msgListeners.contains(item)) {

            if (m_connectionStatus.get() == 0) {
                start();
            }
            m_msgListeners.add(item);
        }else{
          
        }

    }

    public void stop(){
        m_connectionStatus.set(0);
        
        if (m_scheduledFuture != null && !m_scheduledFuture.isDone()) {
            m_scheduledFuture.cancel(false);
            try {
                Files.writeString(App.logFile.toPath(), "Update spf stopped", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                
            }
        }

    }
 
    //private static volatile int m_counter = 0;

    public void start(){
        if(m_connectionStatus.get() == 0){

            m_connectionStatus.set(1);
                    

            ExecutorService executor = getNetworksData().getExecService();
            
            Runnable exec = ()->{
                //FreeMemory freeMem = Utils.getFreeMemory();
                
                getMarkets();
                /*try {
                    Files.writeString(App.logFile.toPath(), "\nfreeMem: " + freeMem.getMemFreeGB() + " GB", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    
                }

               
                int counter = m_counter;
 
                if(counter >= 10){
                    System.gc();
                    try {
                        Files.writeString(App.logFile.toPath(), "\nGarbage Collected", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        
                    }
                    m_counter = 0;
                }else{
                    m_counter++;
                }*/
                
            };

            Runnable submitExec = ()->executor.submit(exec);

            m_scheduledFuture = m_schedualedExecutor.scheduleAtFixedRate(submitExec, 0, 7000, TimeUnit.MILLISECONDS);

                
            /*ChangeListener<SpectrumMarketData[]> dataChangeListener = (obs,oldval,newval)->{
        
                for(SpectrumMarketInterface msgListener : m_msgListeners){
                    msgListener.marketArrayChange(newval);
                }
                
            };*/
        // dataArrayObject.addListener(dataChangeListener);
         //   getMarkets();
        
            m_msgListeners.addListener((ListChangeListener.Change<? extends SpectrumMarketInterface> c) ->{
                int newSize = m_msgListeners.size();
                boolean added = newSize > m_listenerSize;
                
                if(added){
                    
                //   SpectrumMarketInterface lastInterface = m_msgListeners.get(newSize-1);
                // lastInterface.marketArrayChange(dataArrayObject.get());
                    
                }else{
                    if(newSize == 0){
                    
                        shutdown();
                    }
                }

                m_listenerSize = newSize;
            });
       
            addShutdownListener((obs,oldval,newval)->{
            
                stop();
               
            // dataArrayObject.removeListener(dataChangeListener);
                removeShutdownListener();
          
            });
        }
    }

    public SpectrumMarketInterface getListener(String id) {
        for (int i = 0; i < m_msgListeners.size(); i++) {
            SpectrumMarketInterface listener = m_msgListeners.get(i);
            if (listener.getId().equals(id)) {
                return listener;
            }
        }
        return null;
    }

  

    public boolean removeMsgListener(SpectrumMarketInterface item) {
   

        SpectrumMarketInterface listener = getListener(item.getId());
        
        if (listener != null) {
            boolean removed = m_msgListeners.remove(listener);
            
            
            if (m_msgListeners.size() == 0) {
                shutdown();
            }
            return removed;
        }
        return false;
    }

 

    @Override
    public IconButton getButton(String iconStyle) {

        IconButton iconButton = new IconButton(iconStyle.equals(IconStyle.ROW) ? getSmallAppIcon() : getAppIcon(), getName(), iconStyle) {
            @Override
            public void open() {
                getOpen();
            }
        };

        return iconButton;
    }
    
}
