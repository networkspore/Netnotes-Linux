package com.netnotes;

import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import com.utils.Utils;

import io.circe.Json;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.concurrent.Task;
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
import javafx.scene.paint.Color;
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

    private File logFile = new File("netnotes-log.txt");

    public static java.awt.Color POSITIVE_HIGHLIGHT_COLOR = new java.awt.Color(0xff3dd9a4, true);
    public static java.awt.Color POSITIVE_COLOR = new java.awt.Color(0xff028A0F, true);

    public static java.awt.Color NEGATIVE_COLOR = new java.awt.Color(0xff9A2A2A, true);
    public static java.awt.Color NEGATIVE_HIGHLIGHT_COLOR = new java.awt.Color(0xffe96d71, true);
    public static java.awt.Color NEUTRAL_COLOR = new java.awt.Color(0x111111);

    public static long DATA_TIMEOUT_SPAN = (30*1000)-100;
    private File m_appDir = null;
   // private File m_dataFile = null;
    private File m_dataDir = null;

    private Stage m_appStage = null;

    public static final String CMC_DATA_ID = "cmcMarketData";
    public static final String TICKER_DATA_ID = "tickerData";

    private SimpleObjectProperty<JsonObject> m_cmdObjectProperty = new SimpleObjectProperty<>(null);

    private ArrayList<String> m_favoriteIds = new ArrayList<>();
    private List<SpectrumMarketItem> m_marketsList = Collections.synchronizedList(new ArrayList<SpectrumMarketItem>());
    private AtomicLong m_cmcLastChecked = new AtomicLong(0);
    private AtomicLong m_tickersLastChecked = new AtomicLong(0);

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
                                    Files.writeString(logFile.toPath(), "SpectrumFinance (addListenersr): " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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


        String fileString = null;
        String appDirFileString = null;
        if (jsonObject != null) {
            JsonElement appDirElement = jsonObject.get("appDir");
      
            JsonElement dataFileElement = jsonObject.get("dataFile");

            fileString = dataFileElement == null ? null : dataFileElement.toString();

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

   

        m_dataDir = new File(m_appDir.getAbsolutePath() + "/data");
        if(!m_dataDir.isDirectory()){
            try {
                Files.createDirectories(m_dataDir.toPath());
            } catch (IOException e) {
          
            }
        }

    }

    public void open() {
        super.open();
      
        showAppStage();
    }

    public Stage getAppStage() {
        return m_appStage;
    }

    public File getDataDir(){
        
        return m_dataDir;
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


    public void updateMarkets() {
        
       
    }


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

        
                spectrumData.closeAll();
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


            BufferedButton refreshBtn = new BufferedButton("/assets/refresh-white-30.png", App.MENU_BAR_IMAGE_WIDTH);
            refreshBtn.setId("menuBtn");
            refreshBtn.setOnAction(e -> {
                refreshBtn.setDisable(true);
                refreshBtn.setImage(new Image("/assets/sync-30.png"));
                spectrumData.updateMarkets();
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

            HBox menuBar = new HBox(sortTypeButton,sortDirectionButton,swapTargetButton, menuBarRegion, searchField, refreshBtn);
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

            Rectangle rect = getNetworksData().getMaximumWindowBounds();
           
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
                refreshBtn.setDisable(false);
                refreshBtn.setImage(new Image("/assets/refresh-white-30.png"));
                String dateString = Utils.formatDateTimeString(newVal);

                lastUpdatedField.setText(dateString);
            });

            double maxWidth = rect.getWidth() / 2;

            ResizeHelper.addResizeListener(m_appStage, 250, 300, maxWidth, rect.getHeight());

            maxBtn.setOnAction(e -> {
                if (m_appStage.getX() != 0 || m_appStage.getY() != 0 || m_appStage.getHeight() != rect.getHeight()) {

                    m_appStage.setX(0);
                    m_appStage.setY(0);
                    m_appStage.setHeight(rect.getHeight());
                } else {
                    m_appStage.setWidth(appStageWidth);
                    m_appStage.setHeight(appStageHeight);

                    m_appStage.setX((rect.getWidth() / 2) - (appStageWidth / 2));
                    m_appStage.setY((rect.getHeight() / 2) - (appStageHeight / 2));
                }

            });

            m_cmdObjectProperty.addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    JsonElement subjectElement = newVal.get("subject");
                    if (subjectElement != null && subjectElement.isJsonPrimitive()) {
                        switch (subjectElement.getAsString()) {
                            case "MAXIMIZE_STAGE_LEFT":
                                m_appStage.setX(0);
                                m_appStage.setY(0);
                                m_appStage.setHeight(rect.getHeight());
                                m_appStage.show();
                                break;
                        }
                    }
                }
            });

            getNetworksData().timeCycleProperty().addListener((obs,oldval,newval)->refreshBtn.fire());

            m_appStage.setOnCloseRequest(e -> runClose.run());

        } else {
            m_appStage.show();
        }
    }


    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

    
        return true;
    }

  



    private void getCMCMarketsLocal(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        String urlString = API_URL + "/v1/price-tracking/cmc/markets";
        /*try {
            Files.writeString(logFile.toPath(), "\ngetting url: " + urlString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/
        
        Task<JsonArray> task = new Task<JsonArray>() {
            @Override
            public JsonArray call() throws JsonParseException, MalformedURLException, IOException {
                InputStream inputStream = null;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                String outputString = null;

                URL url = new URL(urlString);

                URLConnection con = url.openConnection();

                con.setRequestProperty("User-Agent", Utils.USER_AGENT);

                inputStream = con.getInputStream();

                byte[] buffer = new byte[2048];

                int length;

                while ((length = inputStream.read(buffer)) != -1) {

                    outputStream.write(buffer, 0, length);

            
                }

                outputStream.close();
                outputString = outputStream.toString();

                JsonElement jsonElement = new JsonParser().parse(outputString);

                JsonArray jsonArray = jsonElement != null && jsonElement.isJsonArray() ? jsonElement.getAsJsonArray() : null;

                if(jsonArray != null){
                    Platform.runLater(()->{
                        File cmcFile = getIdDataFile(CMC_DATA_ID);
                        try {
                            Utils.writeEncryptedString(getAppKey(), cmcFile, jsonArray.toString());
                        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
                                | IOException e) {
                            try {
                                Files.writeString(logFile.toPath(), "SpectrumFinance (CMCmarketsLocal): " + e.toString(),StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e1) {
                  
                            }
                        }
                  
                    });
                    
                }

                return jsonArray == null ? null : jsonArray;

            }

        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
                
              

    }

    public void getCMCMarkets(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        
        long currentTime = System.currentTimeMillis();
        long lastChecked = m_cmcLastChecked.getAndSet(currentTime);
        File cmcFile = getIdDataFile(CMC_DATA_ID);

        if(lastChecked != 0 && (currentTime -lastChecked) < (1000*30) && cmcFile.isFile() && cmcFile.length() > 50 ){
            
            try{
                
                String fileString = Utils.readStringFile(getAppKey(), cmcFile);
           
                Utils.returnObject(new JsonParser().parse(fileString).getAsJsonArray(), onSucceeded, onSucceeded);
            }catch(IOException | InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e){
                try {
                    Files.writeString(logFile.toPath(), "\nSpectrumFinance: getCMCMarkets: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
                getCMCMarketsLocal(onSucceeded, onFailed);
            }

        }else{
            getCMCMarketsLocal(onSucceeded, onFailed);
        }
        
    }
    
    public void getTickersLocal(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/v1/price-tracking/cg/tickers";

        Task<JsonArray> task = new Task<JsonArray>() {
            @Override
            public JsonArray call() throws JsonParseException, MalformedURLException, IOException {
                InputStream inputStream = null;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                String outputString = null;

                URL url = new URL(urlString);

                URLConnection con = url.openConnection();

                con.setRequestProperty("User-Agent", Utils.USER_AGENT);

                inputStream = con.getInputStream();

                byte[] buffer = new byte[2048];

                int length;

                while ((length = inputStream.read(buffer)) != -1) {

                    outputStream.write(buffer, 0, length);

            
                }

                outputStream.close();
                outputString = outputStream.toString();

                JsonElement jsonElement = new JsonParser().parse(outputString);

                JsonArray jsonArray = jsonElement != null && jsonElement.isJsonArray() ? jsonElement.getAsJsonArray() : null;

                if(jsonArray != null){
                    Platform.runLater(()->{
                        File tickerFile = getIdDataFile(TICKER_DATA_ID);
                        try {
                            Utils.writeEncryptedString(getAppKey(), tickerFile, jsonArray.toString());
                        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
                                | IOException e) {
                            try {
                                Files.writeString(logFile.toPath(), "SpectrumFinance (CMCmarketsLocal): " + e.toString(),StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e1) {
                  
                            }
                        }
                  
                    });
                    
                }

                return jsonArray == null ? null : jsonArray;

            }

        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
                

    }

    public void getTickers(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        
        long currentTime = System.currentTimeMillis();
        long lastChecked = m_tickersLastChecked.getAndSet(currentTime);
        File tickersFile = getIdDataFile(TICKER_DATA_ID);

        if(lastChecked != 0 && (currentTime -lastChecked) < DATA_TIMEOUT_SPAN && tickersFile.isFile() && tickersFile.length() > 50 ){
            
            try{
                String fileString = Utils.readStringFile(getAppKey(), tickersFile);
        
                Utils.returnObject(new JsonParser().parse(fileString).getAsJsonArray(), onSucceeded, onSucceeded);

            }catch(IOException | InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e){
                try {
                    Files.writeString(logFile.toPath(), "\nSpectrumFinance: getTickersMarkets: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
                getTickersLocal(onSucceeded, onFailed);
            }

        }else{
             getTickersLocal(onSucceeded, onFailed);
        }
        
    }
    //
    public boolean getPoolChart(String poolId,long from, long to, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/v1/amm/pool/" + poolId + "/chart?from="+from+"&to=" + to;
        /*try {
            Files.writeString(logFile.toPath(), "\ngetting url: " + urlString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/
        Utils.getUrlJsonArray(urlString, onSucceeded, onFailed, null);

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
    public File getIdIndexFile(){
        return new File(m_dataDir.getAbsolutePath() + "/index.dat");
    }
    
    public File addNewIdFile(String id, JsonArray jsonArray){
        String filePath = m_dataDir.getAbsolutePath() + "/" + id + ".dat";
        File newFile = new File(filePath);
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("file", filePath);

        jsonArray.add(json);
        return newFile;
    }
    public SecretKey getAppKey(){
        return getNetworksData().getAppData().appKeyProperty().get();
    }
    public void saveIndexFile(JsonArray jsonArray){
        JsonObject json = new JsonObject();
        json.add("fileArray", jsonArray);
        
        try {
            Utils.saveJson(getAppKey(), json, getIdIndexFile());
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                | IOException e) {
            try {
                Files.writeString(logFile.toPath(), "SpectrumFinance (saveIndexFile): " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
        }
    }

    private JsonArray getIndexFileArray(SecretKey key, File indexFile){
        try {
            JsonObject indexFileJson = indexFile.isFile() ? Utils.readJsonFile(key, indexFile) : null;
            if(indexFileJson != null){
                JsonElement fileArrayElement = indexFileJson.get("fileArray");
                if(fileArrayElement != null && fileArrayElement.isJsonArray()){
                    return fileArrayElement.getAsJsonArray();
                }
            }
        } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                | IOException e) {
            try {
                Files.writeString(logFile.toPath(), "SpectrumFinance (getIndexFileArray): " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
            
        }
        return null;
    }

    public File getIdDataFile(String id){
        File indexFile = getIdIndexFile();
        JsonArray indexFileArray = indexFile.isFile() ? getIndexFileArray(getAppKey(), indexFile) : null;
        
        if(indexFileArray != null){
            File existingFile = getFileById(id, indexFileArray);
            if(existingFile != null){
                return existingFile;
            }else{
                File newFile = addNewIdFile(id, indexFileArray);
                saveIndexFile(indexFileArray);
                return newFile;
            }
        }else{
            JsonArray newIndexFileArray = new JsonArray();
            File newFile = addNewIdFile(id, newIndexFileArray);
            
            saveIndexFile(newIndexFileArray);

            return newFile;
        }

    }

    private File getFileById(String id, JsonArray jsonArray){
        int size = jsonArray.size();
        for(int i = 0; i < size; i++){
            JsonElement jsonElement = jsonArray.get(i);
            
            JsonObject obj = jsonElement.getAsJsonObject();

            String idString = obj.get("id").getAsString();
            if(idString.equals(id)){
                return new File(obj.get("file").getAsString());
            }
        }
        return null;
    }
}
