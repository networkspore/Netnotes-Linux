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

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
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

    private String m_currentNetworkId = null;
    private String m_tokensId = null;


    public static String API_URL = "https://api.spectrum.fi";

    public final static int STOPPED = 0;
    public final static int STARTING = 1;
    public final static int STARTED = 2;
    public final static int ERROR = 3;


    public static final int LIST_CHANGED = 10;
    public static final int LIST_UPDATED = 11;



    public static java.awt.Color POSITIVE_HIGHLIGHT_COLOR = new java.awt.Color(0xff3dd9a4, true);
    public static java.awt.Color POSITIVE_COLOR = new java.awt.Color(0xff028A0F, true);

    public static java.awt.Color NEGATIVE_COLOR = new java.awt.Color(0xff9A2A2A, true);
    public static java.awt.Color NEGATIVE_HIGHLIGHT_COLOR = new java.awt.Color(0xffe96d71, true);
    public static java.awt.Color NEUTRAL_COLOR = new java.awt.Color(0x111111);

    public final static String ERG_ID = "0000000000000000000000000000000000000000000000000000000000000000";
    public final static String SPF_ID = "9a06d9e545a41fd51eeffc5e20d818073bf820c635e2a9d922269913e0de369d";
    public final static String SIGUSD_ID = "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04";

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

    private ObservableList<SpectrumMarketInterface> m_msgListeners = FXCollections.observableArrayList();
    private int m_connectionStatus = 0;

    private ScheduledExecutorService m_schedualedExecutor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> m_scheduledFuture = null;
    private TimeSpan m_itemTimeSpan= new TimeSpan("1day");


    private boolean m_isMax = false;
    private double m_prevHeight = -1;
    private double m_prevX = -1;
    private double m_prevY = -1;
    private int m_listenerSize = 0;


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

    public ArrayList<SpectrumMarketData> marketsList(){
        return m_marketsList;
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

        JsonElement currentNetworkElement = jsonObject != null ? jsonObject.get("currentNetworkId") : null;
        JsonElement tokensIdElement = jsonObject != null ? jsonObject.get("tokensId") : null;
        JsonElement stageElement = jsonObject != null ? jsonObject.get("stage") : null; 
        String currentNetworkId = currentNetworkElement != null && currentNetworkElement.isJsonPrimitive() ? currentNetworkElement.getAsString() : ErgoNetwork.NETWORK_ID;
        String tokensId = tokensIdElement != null && tokensIdElement.isJsonPrimitive() ? tokensIdElement.getAsString() : m_currentNetworkId != null ? ((m_currentNetworkId.equals(ErgoNetwork.NETWORK_ID) ? ErgoTokens.NETWORK_ID : null)) : null;

        m_currentNetworkId =currentNetworkId;
        m_tokensId = tokensId;

        String appDirFileString = null;
        if (jsonObject != null) {
            JsonElement appDirElement = jsonObject.get("appDir");
      

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

        if(stageElement != null && stageElement.isJsonObject()){
            JsonObject stageObject =  stageElement.getAsJsonObject();

            JsonElement itemTimeSpanElement = stageObject.get("itemTimeSpan");

            try{
                m_itemTimeSpan = itemTimeSpanElement != null && itemTimeSpanElement.isJsonObject()  ? new TimeSpan(itemTimeSpanElement.getAsJsonObject()) :  new TimeSpan("1day");

            }catch(NullPointerException nPE){
                m_itemTimeSpan = new TimeSpan("1day");
            }
        }

    }

    public void setItemTimeSpan(TimeSpan value){
        m_itemTimeSpan = value;
        getLastUpdated().set(LocalDateTime.now());
    }

    public TimeSpan getItemTimeSpan(){
        return m_itemTimeSpan;
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject networkObj = new JsonObject();
        networkObj.addProperty("name", getName());
        networkObj.addProperty("networkId", getNetworkId());
        networkObj.addProperty("currentNetworkId", m_currentNetworkId);
        networkObj.addProperty("tokensId", m_tokensId);
        networkObj.add("stage", getStageJson());
        return networkObj;

    }

    @Override
    public JsonObject getStageJson() {
        JsonObject json = new JsonObject();
        json.add("itemTimeSpan", m_itemTimeSpan.getJsonObject());
        json.addProperty("maximized", getStageMaximized());
        json.addProperty("width", getStageWidth());
        json.addProperty("height", getStageHeight());
        json.addProperty("prevWidth", getStagePrevWidth());
        json.addProperty("prevHeight", getStagePrevHeight());
        json.addProperty("iconStyle", getStageIconStyle());
        return json;
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

    public void setCurrentNetworkId(String networkId){
        m_currentNetworkId = networkId;
        getLastUpdated().set(LocalDateTime.now());
    }

    public String getCurrentNetworkId(){
        return m_currentNetworkId;
    }

    public String getTokensID(){
        return m_tokensId;
    }

    public void setTokensId(String id){
        m_tokensId = id;
        getLastUpdated().set(LocalDateTime.now());
    }


    private void showAppStage() {
        if (m_appStage == null) {
            

            SpectrumDataList spectrumData = new SpectrumDataList(getNetworkId(), this);

            SimpleObjectProperty<TimeSpan> itemTimeSpanObject = new SimpleObjectProperty<TimeSpan>(m_itemTimeSpan);


            double appStageWidth = 670;
            double appStageHeight = 600;

            m_appStage = new Stage();
            m_appStage.getIcons().add(SpectrumFinance.getSmallAppIcon());
            m_appStage.initStyle(StageStyle.UNDECORATED);
            m_appStage.setTitle(NAME);

            Button closeBtn = new Button();


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

            BufferedButton swapTargetButton = new BufferedButton(spectrumData.isInvertProperty().get() ? "/assets/targetSwapped.png" : "/assets/targetStandard.png", App.MENU_BAR_IMAGE_WIDTH);
            swapTargetButton.setOnAction(e->{
                spectrumData.isInvertProperty().set(!spectrumData.isInvertProperty().get());
            });

            spectrumData.isInvertProperty().addListener((obs,oldval,newval)->{
                swapTargetButton.setImage(new Image(newval ? "/assets/targetSwapped.png" : "/assets/targetStandard.png"));
        
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

            
            Tooltip currentNetworkTip = new Tooltip("Install: Ergo Network");
            currentNetworkTip.setShowDelay(new javafx.util.Duration(100));
            currentNetworkTip.setFont(App.txtFont);

            String tokensDefaultImgString = "/assets/remove-30.png";
            String networkDefaultImgString = "/assets/globe-outline-white-120.png";
     
            /*ImageView currentNetworkImageView = new ImageView(new Image());
            currentNetworkImageView.setFitWidth(App.MENU_BAR_IMAGE_WIDTH);
            currentNetworkImageView.setPreserveRatio(true);*/

            BufferedMenuButton currentNetworkBtn = new BufferedMenuButton(networkDefaultImgString, 25);
           
            currentNetworkBtn.setTooltip(currentNetworkTip);


            
            BufferedMenuButton tokensMenuBtn = new BufferedMenuButton(tokensDefaultImgString, 25);
            tokensMenuBtn.setPadding(new Insets(2, 0, 0, 0));
            
            Tooltip tokensTip = new Tooltip("Tokens: Disabled");
            tokensTip.setShowDelay(new javafx.util.Duration(50));
            tokensTip.setFont(App.txtFont);

            tokensMenuBtn.setTooltip(tokensTip);

            SimpleObjectProperty< ListChangeListener<NoteInterface>> tokensChangeListenerObject = new SimpleObjectProperty<>(null);

            Runnable updateTokensMenu = ()->{
                tokensMenuBtn.getItems().clear();
                NoteInterface currentNetworkInterface = spectrumData.currentNetwork().get();
                
                if(currentNetworkInterface != null && currentNetworkInterface instanceof ErgoNetwork){
                    
                    ErgoNetwork ergoNetwork = (ErgoNetwork) currentNetworkInterface;
                    if(tokensChangeListenerObject.get() != null){
                        ergoNetwork.removeNetworkListener(tokensChangeListenerObject.get());
                        ergoNetwork.addNetworkListener(tokensChangeListenerObject.get());
                    }
                    currentNetworkBtn.setImage(ErgoNetwork.getSmallAppIcon());
                    currentNetworkBtn.setId("menuBtn");
                    currentNetworkTip.setText("Ergo Network");

                    MenuItem ergoNetworksItem = new MenuItem("Ergo Network (selected)");
                    ergoNetworksItem.setId("selectedMenuItem");
                    ergoNetworksItem.setOnAction(e->{
                        setCurrentNetworkId(ErgoNetwork.NETWORK_ID);
                        setTokensId(ErgoTokens.NETWORK_ID);
                        spectrumData.updateTokensList();
                    });
                    
                    MenuItem disableNetworkItem = new MenuItem("Disabled");
                    disableNetworkItem.setOnAction(e->{
                        setTokensId(null);
                        setCurrentNetworkId(null);
                        spectrumData.updateTokensList();
                    });

                    
                    currentNetworkBtn.getItems().clear();
                    currentNetworkBtn.getItems().add(ergoNetworksItem);

                    boolean isEnabled = m_tokensId != null && m_tokensId.equals(ErgoTokens.NETWORK_ID) && ergoNetwork.getNetwork(ErgoTokens.NETWORK_ID) != null;
                
                    MenuItem tokensEnabledItem = new MenuItem("Ergo Tokens" + (isEnabled ? " (selected)" : ""));
                    tokensEnabledItem.setOnAction(e->{
                        setTokensId(ErgoTokens.NETWORK_ID);
                        spectrumData.updateTokensList();
                    });

                    MenuItem tokensDisabledItem = new MenuItem("Tokens Disabled" + (isEnabled ? "" : " (selected)"));
                    tokensDisabledItem.setOnAction(e->{
                        setTokensId(null);
                        spectrumData.updateTokensList();
                    });

                    if(isEnabled){
                        tokensTip.setText("Ergo Tokens: Enabled");
                        tokensEnabledItem.setId("selectedMenuItem");
                        tokensMenuBtn.setImage(ErgoTokens.getSmallAppIcon());
                        tokensMenuBtn.setId("menuBtn");
                    }else{
                        tokensTip.setText("Tokens: Disabled");
                        tokensDisabledItem.setId("selectedMenuItem");
                        tokensMenuBtn.setImage(new Image(tokensDefaultImgString));
                        tokensMenuBtn.setId("menuBtnDisabled");
                    }

                    tokensMenuBtn.getItems().addAll(tokensEnabledItem, tokensDisabledItem);

                  
                }else{
                    if(getNetworksData().getNoteInterface(ErgoNetwork.NETWORK_ID) == null){

                        tokensMenuBtn.setId("menuBtnDisabled");
                        tokensMenuBtn.setImage(new Image(tokensDefaultImgString));
                        
                        currentNetworkBtn.setId("menuBtnDisabled");
                        currentNetworkBtn.setImage(new Image(networkDefaultImgString));

                        MenuItem ergoNetworksInstallItem = new MenuItem("(install 'Ergo Network')");
                        ergoNetworksInstallItem.setOnAction(e->{
                            getNetworksData().showManageNetworkStage();
                        });
                        tokensTip.setText("No network available");
                        
                        
                        currentNetworkBtn.getItems().add(ergoNetworksInstallItem);

                    }else{

                        tokensMenuBtn.setId("menuBtnDisabled");
                        tokensMenuBtn.setImage(new Image(tokensDefaultImgString));

                        MenuItem unavailableItem = new MenuItem("(unavailable)");
                        
                        tokensMenuBtn.getItems().add(unavailableItem);

                        currentNetworkBtn.setId("menuBtnDisabled");
                        currentNetworkBtn.setImage(new Image(networkDefaultImgString));

                        MenuItem ergoNetworksInstallItem = new MenuItem("Ergo Network");
                        ergoNetworksInstallItem.setOnAction(e->{
                            m_tokensId = ErgoTokens.NETWORK_ID;
                            setCurrentNetworkId(ErgoNetwork.NETWORK_ID);
                            spectrumData.updateTokensList();
                        });
                        tokensTip.setText("Select network");
               
                        currentNetworkBtn.getItems().add(ergoNetworksInstallItem);

                    }
                   
                }
            
            };

            spectrumData.currentNetwork().addListener((obs,oldval,newval)->updateTokensMenu.run());

            ListChangeListener<NoteInterface> tokenChangeListener = (ListChangeListener.Change<? extends NoteInterface> c) -> spectrumData.updateTokensList();
          
            tokensChangeListenerObject.set(tokenChangeListener);

            getNetworksData().addNetworkListener((ListChangeListener.Change<? extends NoteInterface> c) -> spectrumData.updateTokensList());

            updateTokensMenu.run();
            spectrumData.updateTokensList();

            HBox rightSideMenu = new HBox(currentNetworkBtn, tokensMenuBtn);
           
            rightSideMenu.setId("rightSideMenuBar");
            rightSideMenu.setPadding(new Insets(0, 0, 0, 0));
            rightSideMenu.setAlignment(Pos.CENTER_RIGHT);

            Region menuBarRegion1 = new Region();
            menuBarRegion1.setMinWidth(10);

            
            MenuButton timeSpanBtn = new MenuButton(m_itemTimeSpan.getName());
            timeSpanBtn.setFont(App.txtFont);
            timeSpanBtn.setContentDisplay(ContentDisplay.LEFT);
            timeSpanBtn.setAlignment(Pos.CENTER_LEFT);

            
            String[] spans = {"1hour", "8hour", "12hour", "1day", "1week", "1month", "6month", "1year"};

            for (int i = 0; i < spans.length; i++) {

                String span = spans[i];
                TimeSpan timeSpan = new TimeSpan(span);
                MenuItem menuItm = new MenuItem(timeSpan.getName());
                menuItm.setId("urlMenuItem");
                menuItm.setUserData(timeSpan);

                menuItm.setOnAction(action -> {
    
                    Object itemObject = menuItm.getUserData();

                    if (itemObject != null && itemObject instanceof TimeSpan) {
                        
                        TimeSpan timeSpanItem = (TimeSpan) itemObject;
                        itemTimeSpanObject.set(timeSpanItem);
                        
                    }

                });

                timeSpanBtn.getItems().add(menuItm);

            }

            itemTimeSpanObject.addListener((obs,oldval,newval)->{
                timeSpanBtn.setText(newval.getName());
                setItemTimeSpan(newval);
            });

            HBox menuBar = new HBox(sortTypeButton,sortDirectionButton,swapTargetButton, menuBarRegion1, searchField, menuBarRegion,timeSpanBtn, rightSideMenu);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 2, 1, 5));

            VBox favoritesVBox = spectrumData.getFavoriteGridBox();

            SimpleObjectProperty<ScrollPane> selectedScroll = new SimpleObjectProperty<>(null);

            ScrollPane favoriteScroll = new ScrollPane(favoritesVBox);
            favoriteScroll.setPadding(new Insets(5, 0, 5, 5));
            favoriteScroll.setId("darkBox");
            favoriteScroll.addEventFilter(MouseEvent.MOUSE_CLICKED,e->{
                selectedScroll.set(favoriteScroll);
            });
            double defaultGridWidth = appStageWidth -30;
            double defaultGridHeight = appStageHeight - 100;            

            SimpleDoubleProperty gridWidth = new SimpleDoubleProperty(defaultGridWidth);
            SimpleDoubleProperty gridHeight = new SimpleDoubleProperty(defaultGridHeight);
            SimpleObjectProperty<HBox> currentBox = new SimpleObjectProperty<>(null);

            VBox chartList = spectrumData.getGridBox(gridWidth, gridHeight, itemTimeSpanObject, currentBox);
  
            ScrollPane scrollPane = new ScrollPane(chartList);
            scrollPane.setPadding(new Insets(2));
            HBox headingsBox = new HBox();

            scrollPane.addEventFilter(MouseEvent.MOUSE_CLICKED,(e)->{
                selectedScroll.set(scrollPane);
            });

            VBox bodyPaddingBox = new VBox(headingsBox,
                scrollPane);
            bodyPaddingBox.setPadding(new Insets(5,5,0,5));

            Font smallerFont = Font.font("OCR A Extended", 10);

            Text lastUpdatedTxt = new Text("Updated ");
            lastUpdatedTxt.setFill(App.formFieldColor);
            lastUpdatedTxt.setFont(smallerFont);

            TextField lastUpdatedField = new TextField();
            lastUpdatedField.setEditable(false);
            lastUpdatedField.setId("formField");
            lastUpdatedField.setFont(smallerFont);
            lastUpdatedField.setPrefWidth(245);

            HBox lastUpdatedBox = new HBox(lastUpdatedTxt, lastUpdatedField);
            lastUpdatedBox.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(lastUpdatedBox, Priority.ALWAYS);

            VBox footerVBox = new VBox(lastUpdatedBox);
            HBox.setHgrow(footerVBox, Priority.ALWAYS);
            footerVBox.setPadding(new Insets(0,5,2,5));

            VBox headerVBox = new VBox(titleBox);
            headerVBox.setPadding(new Insets(0, 5, 0, 5));
            headerVBox.setAlignment(Pos.TOP_CENTER);

            VBox layoutBox = new VBox(headerVBox, bodyPaddingBox, footerVBox);

            HBox menuPaddingBox = new HBox(menuBar);
            menuPaddingBox.setPadding(new Insets(0, 0, 0, 0));

            spectrumData.statusProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && (newVal.startsWith("Top"))) {
                    if (!headerVBox.getChildren().contains(menuPaddingBox)) {
                        headerVBox.getChildren().add(1, menuPaddingBox);

                    }

                } else {
                    if (headerVBox.getChildren().contains(menuPaddingBox)) {
                        headerVBox.getChildren().remove(menuPaddingBox);

                    }

                }
            });
            Runnable updateFavorites = ()->{
                    int numFavorites = favoritesVBox.getChildren().size();
                    if (numFavorites > 0) {
                        if (!headerVBox.getChildren().contains(favoriteScroll)) {
    
                            headerVBox.getChildren().add(favoriteScroll);
                            menuPaddingBox.setPadding(new Insets(0, 0, 5, 0));
                        }
                        int favoritesHeight = (numFavorites * 32) + 37 + (currentBox.get() != null && selectedScroll.get() != null && selectedScroll.get().equals(favoriteScroll) ? 128 : 0);
                        favoriteScroll.setPrefViewportHeight(favoritesHeight > 175 ? 175 : favoritesHeight);
                    } else {
                        if (headerVBox.getChildren().contains(favoriteScroll)) {
    
                            headerVBox.getChildren().remove(favoriteScroll);
                            menuPaddingBox.setPadding(new Insets(0, 0, 0, 0));
                        }
                    }
                
            };
            favoritesVBox.getChildren().addListener((Change<? extends Node> changeEvent) -> updateFavorites.run());

            selectedScroll.addListener((obs,oldval,newval)->{
                updateFavorites.run();
            });

            currentBox.addListener((obs,oldval,newval)->updateFavorites.run());

            Scene appScene = new Scene(layoutBox, appStageWidth, appStageHeight);
            appScene.setFill(null);
            appScene.getStylesheets().add("/css/startWindow.css");
            m_appStage.setScene(appScene);
            m_appStage.show();

            bodyPaddingBox.prefWidthProperty().bind(m_appStage.widthProperty());
            scrollPane.prefViewportWidthProperty().bind(m_appStage.widthProperty());

            Binding<Double> scrollWidth = Bindings.createObjectBinding(()->scrollPane.viewportBoundsProperty().get() != null ? (scrollPane.viewportBoundsProperty().get().getWidth() < 300 ? 300 : scrollPane.viewportBoundsProperty().get().getWidth() ) : 300, scrollPane.viewportBoundsProperty());
            gridWidth.bind(scrollWidth);

            Binding<Double> scrollHeight = Bindings.createObjectBinding(()->scrollPane.viewportBoundsProperty().get() != null ? scrollPane.viewportBoundsProperty().get().getHeight() : defaultGridHeight, scrollPane.viewportBoundsProperty());
            gridHeight.bind(scrollHeight);

            favoriteScroll.prefViewportWidthProperty().bind(m_appStage.widthProperty());

            scrollPane.prefViewportHeightProperty().bind(m_appStage.heightProperty().subtract(headerVBox.heightProperty()).subtract(footerVBox.heightProperty()));

            chartList.prefWidthProperty().bind(scrollPane.prefViewportWidthProperty().subtract(40));
            favoritesVBox.prefWidthProperty().bind(favoriteScroll.prefViewportWidthProperty().subtract(40));

          
            spectrumData.addUpdateListener((obs,oldval,newval)->{
                if(newval != null){
                    lastUpdatedField.setText(Utils.formatDateTimeString(newval));
                }else{
                    lastUpdatedField.setText("");
                }
            });

            ResizeHelper.addResizeListener(m_appStage, 250, 300,Double.MAX_VALUE, Double.MAX_VALUE);
   
            Runnable runClose = () -> {


                spectrumData.shutdown();
                spectrumData.removeUpdateListener();
                if(m_appStage != null){
                    m_appStage.close();
                    m_appStage = null;
                }

            };
            
            closeBtn.setOnAction(closeEvent -> {
                
                runClose.run();
            });

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

            
            FxTimer.runLater(Duration.ofMillis(50), ()->spectrumData.connectToExchange(this));
            

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

    private void getMarkets(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/v1/price-tracking/markets";

        Utils.getUrlJsonArray(urlString, getExecService(), onSucceeded, onFailed, null);
                        
    }
  
      
    public void getTickers(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/v1/price-tracking/cg/tickers";

        Utils.getUrlJsonArray(urlString, getNetworksData().getExecService(), onSucceeded, onFailed, null);                

    }

    public static void getPoolSlippage(String poolId, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        String urlString = API_URL + "/v1/amm/pool/" + poolId + "/slippage";

        Utils.getUrlJson(urlString, execService, onSucceeded, onFailed, null);
    }

    public static void getPoolStats(String poolId, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        String urlString = API_URL + "/v1/amm/pool/" + poolId + "/stats";

        Utils.getUrlJson(urlString, execService, onSucceeded, onFailed, null);
    }

    private void getMarketUpdate(JsonArray jsonArray){
        
        ArrayList<SpectrumMarketData> tmpMarketsList = new ArrayList<>();
        long timeStamp = System.currentTimeMillis();

        SimpleBooleanProperty isChanged = new SimpleBooleanProperty(false);
        for (int i = 0; i < jsonArray.size(); i++) {
    
            JsonElement marketObjectElement = jsonArray.get(i);
            if (marketObjectElement != null && marketObjectElement.isJsonObject()) {

                JsonObject marketDataJson = marketObjectElement.getAsJsonObject();
                
                try{
                    
                    SpectrumMarketData marketData = new SpectrumMarketData(marketDataJson, timeStamp);
               
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
                                    if(liquidityUsdElement != null && liquidityUsdElement.isJsonPrimitive() ){

                                        marketData.setLiquidityUSD(liquidityUsdElement.getAsBigDecimal());
       
                                    }
                                    if( poolIdElement != null && poolIdElement.isJsonPrimitive()){
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


    public void sendMessage(int msg){
        long timestamp = System.currentTimeMillis();
        for(int i = 0; i < m_msgListeners.size() ; i++){
            m_msgListeners.get(i).sendMessage(msg, timestamp);
        }
    }

    public void sendMessage(int msg, long timestamp){

        for(int i = 0; i < m_msgListeners.size() ; i++){
            m_msgListeners.get(i).sendMessage(msg, timestamp);
        }
    }
    public void sendMessage(int code, long timestamp, String msg){

        for(int i = 0; i < m_msgListeners.size() ; i++){
            m_msgListeners.get(i).sendMessage(code, timestamp, msg);
        }
    }

    private void updateMarketList(ArrayList<SpectrumMarketData> data){
        int size = data.size();

        if(m_marketsList.size() == 0){
            for(int i = 0; i < size; i++){
                SpectrumMarketData marketData = data.get(i);
               if(data != null){
                    m_marketsList.add(marketData);
               }
            }
            data.clear();
            sendMessage(LIST_CHANGED);
        }else{
            SimpleBooleanProperty changed = new SimpleBooleanProperty(false);
            for(int i = 0; i < size; i++){
                SpectrumMarketData newMarketData = data.get(i);
                
                SpectrumMarketData marketData = getMarketDataById(m_marketsList, newMarketData.getId());
                if(marketData != null){
                    marketData.update(newMarketData);
                }else{
                    changed.set(true);
                    m_marketsList.add(newMarketData);
                   
                }
            }
            data.clear();
            if(changed.get()){
                sendMessage(LIST_CHANGED);
            }else{
                sendMessage(LIST_UPDATED);
            }
            
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
    

    public File getMarketFile(SpectrumMarketData data){
        
        return data.getPoolId() != null ? getIdDataFile( data.getPoolId()) : null;    
    }

    public void getPoolChart(String poolId, long currentTime, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {


        String urlString = API_URL + "/v1/amm/pool/" + poolId + "/chart?from=0&to=" + currentTime;

        Utils.getUrlJsonArray(urlString, getExecService(), onSucceeded, onFailed, null);
    }
    

    public void addMsgListener(SpectrumMarketInterface item) {
        if (!m_msgListeners.contains(item)) {

            
            start();
            
            m_msgListeners.add(item);
        }else{
          
        }

    }

    private void setConnectionStatus(int status){
        m_connectionStatus = status;
        sendMessage(status);
    }

    public void stop(){
        setConnectionStatus(STOPPED);
        
        if (m_scheduledFuture != null && !m_scheduledFuture.isDone()) {
            m_scheduledFuture.cancel(false);
         
        }

    }
 
    //private static volatile int m_counter = 0;

    public void start(){
        if(m_connectionStatus == STOPPED){

            m_connectionStatus = STARTING;
                    

            ExecutorService executor = getNetworksData().getExecService();
            
            Runnable exec = ()->{
                //FreeMemory freeMem = Utils.getFreeMemory();
                
                getMarkets(success -> {


                    Object sourceObject = success.getSource().getValue();
                    if (sourceObject != null && sourceObject instanceof JsonArray) {
                        JsonArray marketJsonArray = (JsonArray) sourceObject;
                        try {
                            Files.writeString( new File(getDataDir().getAbsolutePath() + "/markets.json").toPath(), marketJsonArray.toString());
                        } catch (IOException e) {
                 
                        }
                        m_connectionStatus = STARTED;
                        getMarketUpdate(marketJsonArray);
                    } 
                }, (onfailed)->{
                    //onfailed.getSource().getException();
                    m_connectionStatus = ERROR;
                    sendMessage(ERROR, System.currentTimeMillis(), onfailed.getSource().getException().toString());
                });
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

        
            m_msgListeners.addListener((ListChangeListener.Change<? extends SpectrumMarketInterface> c) ->{
                int newSize = m_msgListeners.size();
                boolean added = newSize > m_listenerSize;
                
                if(added){
      
                    
                }else{
                    if(newSize == 0){
                    
                        stop();
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

    public int getConnectionStatus(){
        return m_connectionStatus;
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
