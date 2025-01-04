package com.netnotes;


import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netnotes.NetworksData.ManageNetworksTab;
import com.utils.Utils;

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class ErgoDex extends Network implements NoteInterface {

    public final static String DESCRIPTION = "ErgoDex is an open source decentralized exchange (DEX) for fast, trustless swaps, liquidity provision & liquidity mining on the Ergo Blockchain.";

    public final static String NAME = "ErgoDex";
    public final static String WEB_URL = "https://www.ergodex.io";
    public final static String NETWORK_ID = "ERGO_DEX";
    public final static String API_URL = "https://api.spectrum.fi";

    public final static String IMAGE_LINK = "https://raw.githubusercontent.com/spectrum-finance/token-logos/master/logos/ergo";


    public static java.awt.Color POSITIVE_HIGHLIGHT_COLOR = new java.awt.Color(0xff3dd9a4, true);
    public static java.awt.Color POSITIVE_COLOR = new java.awt.Color(0xff028A0F, true);

    public static java.awt.Color NEGATIVE_COLOR = new java.awt.Color(0xff9A2A2A, true);
    public static java.awt.Color NEGATIVE_HIGHLIGHT_COLOR = new java.awt.Color(0xffe96d71, true);
    public static java.awt.Color NEUTRAL_COLOR = new java.awt.Color(0x111111);

    public final static String SPF_ID = "9a06d9e545a41fd51eeffc5e20d818073bf820c635e2a9d922269913e0de369d";
    public final static String SIGUSD_ID = "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04";

    public final static String ERG_SIGUSD_POOL_ID = "9916d75132593c8b07fe18bd8d583bda1652eed7565cf41a4738ddd90fc992ec";
    public final static String ERG_SPF_POOL_ID = "f40afb6f877c40a30c8637dd5362227285738174151ce66d6684bc1b727ab6cf";

    public final static String MINER_ADDRESS = "2iHkR7CWvD1R4j1yZg5bkeDRQavjAaVPeTDFGGLZduHyfWMuYpmhHocX8GJoaieTx78FntzJbCBVL6rf96ocJoZdmWBL2fci7NqWgAirppPQmZ7fN9V6z13Ay6brPriBKYqLp1bT2Fk4FkFLCfdPpe";

    public final static long BLOCK_TIME_MILLIS = 2L * 60L * 1000L;
    public final static long DATA_TIMEOUT_SPAN = (15*1000)-100;
    public final static long TICKER_DATA_TIMEOUT_SPAN = 1000*60;
    public final static long MIN_BOX_VALUE = 60000;

   // private File m_dataFile = null;


    private Stage m_appStage = null;

    public static final String MARKET_DATA_ID = "marketData";
    public static final String TICKER_DATA_ID = "tickerData";
    public static final NetworkType NETWORK_TYPE = NetworkType.MAINNET;
    public static final String MARKETS_LIST = "MARKETS_LIST";

    private ArrayList<ErgoDexMarketData> m_marketsList = new ArrayList<>();



    private ScheduledExecutorService m_schedualedExecutor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> m_scheduledFuture = null;


    private String m_locationId;

    public ErgoDex(NetworksData networksData, String locationId) {
        super(new Image(getAppIconString()), NAME, NETWORK_ID, networksData);
        setKeyWords(new String[]{"ergo", "exchange", "usd", "ergo tokens", "dApp", "SigUSD"});
        m_locationId = locationId;
    }

 



    public static String getAppIconString(){
        return "/assets/ErgoDex-150.png";
    }

 
    public ArrayList<ErgoDexMarketData> marketsList(){
        return m_marketsList;
    }
    
   
    public  Image getSmallAppIcon() {
        return new Image(getSmallAppIconString());
    }

    public static String getSmallAppIconString(){
        return "/assets/ErgoDex-32.png";
    }




    public Stage getAppStage() {
        return m_appStage;
    }

    public ErgoDexMarketData[] getMarketDataArray(){
        ErgoDexMarketData[] dataArray = new ErgoDexMarketData[m_marketsList.size()];
        return dataArray = m_marketsList.toArray(dataArray);
    }


    public static ErgoDexMarketData getMarketDataById(ArrayList<ErgoDexMarketData> dataList, String id) {
        if (id != null) {
            for (ErgoDexMarketData data : dataList) {
                if (data.getId().equals(id) ) {
                    return data;
                }
            }
            
        }
        return null;
    }


    public static ErgoDexMarketData getMarketDataByTickerId(ArrayList<ErgoDexMarketData> dataList, String tickerId) {
        if (tickerId != null) {
            for (ErgoDexMarketData data : dataList) {
                if (data.getTickerId().equals(tickerId) ) {
                    return data;
                }
            }
            
        }
        return null;
    }

    public static int getMarketDataIndexById(ArrayList<ErgoDexMarketData> dataList, String id) {
        if (id != null) {
            int size = dataList.size();
            for (int i = 0; i < size; i++) {
                ErgoDexMarketData data = dataList.get(i);
                if (data.getId().equals(id) ) {
                    return i;
                }
            }
            
        }
        return -1;
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

   

    
    public NoteInterface getNoteInterface(){
       
        return new NoteInterface() {
            
            public String getName(){
                return NAME;
            }

            public String getNetworkId(){
                return NETWORK_ID;
            }

            public Image getAppIcon(){
                return ErgoDex.this.getAppIcon();
            }



            public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
                return ErgoDex.this.sendNote(note, onSucceeded, onFailed);
            }

            public Object sendNote(JsonObject note){
                return ErgoDex.this.sendNote(note);
            }

            public JsonObject getJsonObject(){
                JsonObject spectrumObject = ErgoDex.this.getJsonObject();
                spectrumObject.addProperty("apiUrl", API_URL);
                spectrumObject.addProperty("website", WEB_URL);
                spectrumObject.addProperty("description", DESCRIPTION);
                return spectrumObject;
            }

            public TabInterface getTab(Stage appStage, SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject,  Button networkBtn){
                return ErgoDex.this.getTab(appStage, heightObject, widthObject, networkBtn);
            }

            public NetworksData getNetworksData(){
                return ErgoDex.this.getNetworksData();
            }

            public NoteInterface getParentInterface(){
                return getParentInterface();
            }

    
            public void shutdown(){}

            public SimpleObjectProperty<LocalDateTime> shutdownNowProperty(){
                return null;
            }

            public void addMsgListener(NoteMsgInterface listener){
                if(listener != null && listener.getId() != null){
                    ErgoDex.this.addMsgListener(listener);
                }
            }
            public boolean removeMsgListener(NoteMsgInterface listener){
                
                return ErgoDex.this.removeMsgListener(listener);
            }

            public int getConnectionStatus(){
                return ErgoDex.this.getConnectionStatus();
            }

            public void setConnectionStatus(int status){

            }


            public String getDescription(){
                return ErgoDex.this.getDescription();
            }
        };
    }


    

    private ErgoDexTab m_ergoDexTab = null;

    @Override
    public TabInterface getTab(Stage appStage,  SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject, Button menuBtn){
        if(m_ergoDexTab != null){
            return m_ergoDexTab;
        }else{
            m_ergoDexTab = new ErgoDexTab(appStage,  heightObject, widthObject, menuBtn);
            return m_ergoDexTab;
        }
    }

    private class ErgoDexTab extends AppBox implements TabInterface{
        private Button m_menuBtn;
        private ErgoDexDataList m_dexDataList = null;
        private String noNetworkImgString = "/assets/globe-outline-white-30.png";

        private boolean m_isErgoNetwork = true;
        private SimpleObjectProperty<NoteInterface> m_ergoNetworkInterface = new SimpleObjectProperty<>(null);
        
        private NoteMsgInterface m_networksDataMsgInterface;

        private SimpleObjectProperty<TimeSpan> m_itemTimeSpan = new SimpleObjectProperty<TimeSpan>(new TimeSpan("1day"));
        private SimpleStringProperty m_status = new SimpleStringProperty(App.STATUS_STOPPED);
        private HBox m_menuBar;
        private VBox m_bodyPaddingBox;

        private TextField m_lastUpdatedField;

        private SimpleDoubleProperty m_gridWidth;
        private SimpleDoubleProperty m_gridHeight;

        private SimpleDoubleProperty m_widthObject;
        private SimpleDoubleProperty m_heightObject;
        private ScrollPane scrollPane;

        public ErgoDexTab(Stage appStage,  SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject, Button menuBtn){
            super(getNetworkId());
            
            m_appStage = appStage;
            m_menuBtn = menuBtn;
            
            m_lastUpdatedField = new TextField(); 
            
            m_widthObject = widthObject;
            m_heightObject = heightObject;

            m_gridWidth = new SimpleDoubleProperty(App.DEFAULT_STATIC_WIDTH);
            m_gridHeight = new SimpleDoubleProperty(heightObject.get() - 100);
            setPrefWidth(App.DEFAULT_STATIC_WIDTH);
            setMaxWidth(App.DEFAULT_STATIC_WIDTH);

            scrollPane = new ScrollPane();
            scrollPane.setPadding(new Insets(2));
            prefHeightProperty().bind(heightObject);
            
            getData((onSucceeded)->{
                Object obj = onSucceeded.getSource().getValue();
                JsonObject json = obj != null && obj instanceof JsonObject ? (JsonObject) obj : null;
                openJson(json); 
                layoutTab();
            });

        }

        public void layoutTab(){
           
            m_dexDataList = new ErgoDexDataList(m_locationId, m_appStage, ErgoDex.this, m_gridWidth,m_gridHeight,m_lastUpdatedField,  m_itemTimeSpan, m_ergoNetworkInterface,  scrollPane);


            ImageView networkMenuBtnImageView = new ImageView(new Image(noNetworkImgString));
            networkMenuBtnImageView.setPreserveRatio(true);
            networkMenuBtnImageView.setFitWidth(30);

            MenuButton networkMenuBtn = new MenuButton();
            networkMenuBtn.setGraphic(networkMenuBtnImageView);
            networkMenuBtn.setPadding(new Insets(0, 3, 0, 0));

            networkMenuBtn.showingProperty().addListener((obs,oldval,newval)->{
                networkMenuBtn.getItems().clear();
                if(newval){
                    if(getErgoNetworkInterface() != null){
                        MenuItem openNetworkItem = new MenuItem("Open…");
                        openNetworkItem.setOnAction(e->{
                            networkMenuBtn.hide();
                            getNetworksData().openNetwork(ErgoNetwork.NETWORK_ID);
                        });
                        networkMenuBtn.getItems().add(openNetworkItem);
                    }else{
                        MenuItem manageNetworkItem = new MenuItem("Manage networks…");
                        manageNetworkItem.setOnAction(e->{
                            networkMenuBtn.hide();
                            getNetworksData().openStatic(ManageNetworksTab.NAME);
                        });
                        networkMenuBtn.getItems().add(manageNetworkItem);
                    }

                    SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();
                    
                    networkMenuBtn.getItems().add(separatorMenuItem);

                    if(isErgoNetwork()){
                        MenuItem disableNetworkItem = new MenuItem("[disable access]");
                        disableNetworkItem.setOnAction(e->{
                            setErgoNetworkEnabled(false);
                        });
                        networkMenuBtn.getItems().add(disableNetworkItem);
                    }else{
                        MenuItem enableNetworkItem = new MenuItem("[enable access]");
                        enableNetworkItem.setOnAction(e->{
                            setErgoNetworkEnabled(true);
                            if(getErgoNetworkInterface() == null){
                                networkMenuBtn.hide();
                                getNetworksData().openStatic(ManageNetworksTab.NAME);
                            }
                        });

                        networkMenuBtn.getItems().add(enableNetworkItem);
                    }
                
    
                    
                
                }
            });
            
            Tooltip networkTip = new Tooltip("Network: (select)");
            networkTip.setShowDelay(new javafx.util.Duration(50));
            networkTip.setFont(App.txtFont);

            networkMenuBtn.setTooltip(networkTip);

            m_ergoNetworkInterface.addListener((obs,oldval,newval)->{
                if(newval != null){
                    networkMenuBtnImageView.setImage(newval.getAppIcon());
                    networkTip.setText(newval.getName());
                }else{
                    networkMenuBtnImageView.setImage(new Image(noNetworkImgString));
                    networkTip.setText("Ergo Network: Unavailable");
                }
            });


            addNetworksDataListener();




         



        

         

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Tooltip refreshTip = new Tooltip("Refresh");
            refreshTip.setShowDelay(new javafx.util.Duration(100));
            refreshTip.setFont(App.txtFont);

        

            BufferedMenuButton sortTypeButton = new BufferedMenuButton("/assets/filter.png", App.MENU_BAR_IMAGE_WIDTH);

            MenuItem sortLiquidityItem = new MenuItem(ErgpDexSort.SortType.LIQUIDITY_VOL);
            MenuItem sortBaseVolItem = new MenuItem(ErgpDexSort.SortType.BASE_VOL);
            MenuItem sortQuoteVolItem = new MenuItem(ErgpDexSort.SortType.QUOTE_VOL);
            MenuItem sortLastPriceItem = new MenuItem(ErgpDexSort.SortType.LAST_PRICE);
        
            sortTypeButton.getItems().addAll(sortLiquidityItem, sortBaseVolItem, sortQuoteVolItem, sortLastPriceItem);

            Runnable updateSortTypeSelected = () ->{
                sortLiquidityItem.setId(null);
                sortBaseVolItem.setId(null);
                sortQuoteVolItem.setId(null);
                sortLastPriceItem.setId(null);

                switch(m_dexDataList.getSortMethod().getType()){
                    case ErgpDexSort.SortType.LIQUIDITY_VOL:
                        sortLiquidityItem.setId("selectedMenuItem");
                    break;
                    case ErgpDexSort.SortType.BASE_VOL:
                        sortBaseVolItem.setId("selectedMenuItem");
                    break;
                    case ErgpDexSort.SortType.QUOTE_VOL:
                        sortQuoteVolItem.setId("selectedMenuItem");
                    break;
                    case ErgpDexSort.SortType.LAST_PRICE:
                        sortLastPriceItem.setId("selectedMenuItem");
                    break;
                }

                m_dexDataList.sort();
                m_dexDataList.updateGrid();
            };

        // updateSortTypeSelected.run();

            sortLiquidityItem.setOnAction(e->{
                ErgpDexSort sortMethod = m_dexDataList.getSortMethod();
                sortMethod.setType(sortLiquidityItem.getText());
                updateSortTypeSelected.run();
            });

            sortBaseVolItem.setOnAction(e->{
                ErgpDexSort sortMethod = m_dexDataList.getSortMethod();
                sortMethod.setType(sortBaseVolItem.getText());
                updateSortTypeSelected.run();
            });

            sortQuoteVolItem.setOnAction(e->{
                ErgpDexSort sortMethod = m_dexDataList.getSortMethod();
                sortMethod.setType(sortQuoteVolItem.getText());
                updateSortTypeSelected.run();
            });

            sortLastPriceItem.setOnAction(e->{
                ErgpDexSort sortMethod = m_dexDataList.getSortMethod();
                sortMethod.setType(sortLastPriceItem.getText());
                updateSortTypeSelected.run();
            });


            BufferedButton sortDirectionButton = new BufferedButton(m_dexDataList.getSortMethod().isAsc() ? "/assets/sortAsc.png" : "/assets/sortDsc.png", App.MENU_BAR_IMAGE_WIDTH);
            sortDirectionButton.setOnAction(e->{
                ErgpDexSort sortMethod = m_dexDataList.getSortMethod();
                sortMethod.setDirection(sortMethod.isAsc() ? ErgpDexSort.SortDirection.DSC : ErgpDexSort.SortDirection.ASC);
                sortDirectionButton.setImage(new Image(sortMethod.isAsc() ? "/assets/sortAsc.png" : "/assets/sortDsc.png"));
                m_dexDataList.sort();
                m_dexDataList.updateGrid();
            });

            BufferedButton swapTargetButton = new BufferedButton(m_dexDataList.isInvertProperty().get() ? "/assets/targetSwapped.png" : "/assets/targetStandard.png", App.MENU_BAR_IMAGE_WIDTH);
            swapTargetButton.setOnAction(e->{
                m_dexDataList.isInvertProperty().set(!m_dexDataList.isInvertProperty().get());
            });

            m_dexDataList.isInvertProperty().addListener((obs,oldval,newval)->{
                swapTargetButton.setImage(new Image(newval ? "/assets/targetSwapped.png" : "/assets/targetStandard.png"));
        
            });
            



            TextField searchField = new TextField();
            searchField.setPromptText("Search");
            searchField.setId("urlField");
            searchField.setPrefWidth(200);
            searchField.setPadding(new Insets(2, 10, 3, 10));
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                m_dexDataList.setSearchText(searchField.getText());
            });

            Region menuBarRegion = new Region();
            HBox.setHgrow(menuBarRegion, Priority.ALWAYS);

            
        


            VBox networkMenuBtnBox = new VBox( networkMenuBtn);


    
            VBox.setVgrow(networkMenuBtnBox,Priority.ALWAYS);


            HBox rightSideMenu = new HBox(networkMenuBtnBox);
        
            rightSideMenu.setId("rightSideMenuBar");
            rightSideMenu.setPadding(new Insets(0, 3, 0, 10));
            rightSideMenu.setAlignment(Pos.CENTER_RIGHT);

            Region menuBarRegion1 = new Region();
            menuBarRegion1.setMinWidth(10);

        
            MenuButton timeSpanBtn = new MenuButton();
            timeSpanBtn.setId("arrowMenuButton");
            timeSpanBtn.setMinWidth(100);
            timeSpanBtn.setPrefWidth(100);
            timeSpanBtn.textProperty().bind( m_itemTimeSpan.asString());
            timeSpanBtn.setAlignment(Pos.CENTER_RIGHT);
    
            
            String[] spans = { "1hour", "8hour", "12hour", "1day", "1week", "1month", "6month", "1year" };

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
                        m_itemTimeSpan.set(timeSpanItem);
                        
                    }

                });

                timeSpanBtn.getItems().add(menuItm);

            }

            m_itemTimeSpan.addListener((obs,oldval,newval)->{
        //     timeSpanLabel.setText(newval.getName() + " ▾");
                save();
            });

            HBox timeSpanBtnBox = new HBox(timeSpanBtn);
            timeSpanBtnBox.setId("urlMenuButton");
            timeSpanBtnBox.setAlignment(Pos.CENTER_LEFT);

            m_menuBar = new HBox(sortTypeButton,sortDirectionButton,swapTargetButton, menuBarRegion1, searchField, menuBarRegion,timeSpanBtnBox, rightSideMenu);
            HBox.setHgrow(m_menuBar, Priority.ALWAYS);
            m_menuBar.setAlignment(Pos.CENTER_LEFT);
            m_menuBar.setId("menuBar");
            m_menuBar.setPadding(new Insets(1, 2, 1, 5));
            m_menuBar.setMinHeight(25);


    

            VBox chartList = m_dexDataList.getLayoutBox();

            scrollPane.setContent(chartList);
            
        //    HBox headingsBox = new HBox();

            HBox menuBarBox = new HBox(m_menuBar);
            HBox.setHgrow(menuBarBox,Priority.ALWAYS);
            menuBarBox.setPadding(new Insets(0,0,10,0));
            
            m_bodyPaddingBox = new VBox(menuBarBox, scrollPane);
            m_bodyPaddingBox.setPadding(new Insets(0,5,0,5));

    

    
            m_lastUpdatedField.setEditable(false);
            m_lastUpdatedField.setId("formFieldSmall");
            m_lastUpdatedField.setPrefWidth(230);

            Binding<String> errorTxtBinding = Bindings.createObjectBinding(()->(m_dexDataList.statusMsgProperty().get().startsWith("Error") ? m_dexDataList.statusMsgProperty().get() : "") ,m_dexDataList.statusMsgProperty());

            Text errorText = new Text("");
            errorText.setFont(App.titleFont);
            errorText.setFill(App.altColor);
            errorText.textProperty().bind(errorTxtBinding);
            
            Region lastUpdatedRegion = new Region();
            lastUpdatedRegion.setMinWidth(10);
            HBox.setHgrow(lastUpdatedRegion, Priority.ALWAYS);

            HBox lastUpdatedBox = new HBox(errorText, lastUpdatedRegion, m_lastUpdatedField);
            lastUpdatedBox.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(lastUpdatedBox, Priority.ALWAYS);

            VBox footerVBox = new VBox(lastUpdatedBox);
            HBox.setHgrow(footerVBox, Priority.ALWAYS);
            footerVBox.setPadding(new Insets(0,5,2,5));



            getChildren().addAll( m_bodyPaddingBox, footerVBox);


            

            m_bodyPaddingBox.prefWidthProperty().bind(widthProperty().subtract(1));
            scrollPane.prefViewportWidthProperty().bind(m_widthObject);

        // Binding<Double> scrollWidth = Bindings.createObjectBinding(()->scrollPane.viewportBoundsProperty().get() != null ? (scrollPane.viewportBoundsProperty().get().getWidth() < 300 ? 300 : scrollPane.viewportBoundsProperty().get().getWidth() ) : 300, scrollPane.viewportBoundsProperty());

            scrollPane.viewportBoundsProperty().addListener((obs,oldval,newval)->{
            
                double width = newval.getWidth();
        
                m_gridWidth.set( width < 300 ? 300 : width );
    
            
                
            });

            Binding<Double> scrollHeight = Bindings.createObjectBinding(()->{
                Bounds bounds = scrollPane.viewportBoundsProperty().get();
                return bounds != null ? bounds.getHeight() : m_gridHeight.get();
            }, scrollPane.viewportBoundsProperty());

            m_gridHeight.bind(scrollHeight);

        
            scrollPane.prefViewportHeightProperty().bind(m_heightObject.subtract(footerVBox.heightProperty()));

            scrollPane.viewportBoundsProperty().addListener((obs,oldval,newval)->{
                chartList.setPrefWidth(newval.getWidth() - 40);
                chartList.setMaxWidth(newval.getWidth() - 40);
            });
        }
        private boolean isErgoNetwork(){
            return m_isErgoNetwork;
        }
        private void setErgoNetworkEnabled(boolean isEnabled){
            m_isErgoNetwork = isEnabled;
            save();
            
            updateErgoNetworkInterface();
        }

        private NoteInterface getErgoNetworkInterface(){
            return getNetworksData().getNetwork(ErgoNetwork.NETWORK_ID);
        }

        public void updateErgoNetworkInterface(){
            if(isErgoNetwork()){
                m_ergoNetworkInterface.set(getErgoNetworkInterface());
            }else{
                m_ergoNetworkInterface.set(null);
            }
        }

        public void addNetworksDataListener(){
            m_networksDataMsgInterface = new NoteMsgInterface() {

                @Override
                public String getId() {
                    
                    return m_locationId;
                }

                @Override
                public void sendMessage(int code, long timestamp, String networkId, String msg) {
                    switch(networkId){
                        case NetworksData.NETWORKS:
                            updateErgoNetworkInterface();
                        break;
                    }
                }

                @Override
                public void sendMessage(int code, long timestamp, String networkId, Number number) {
                    
                }
                
            };

            updateErgoNetworkInterface();

            getNetworksData().addMsgListener(m_networksDataMsgInterface);
        }
        

        @Override
        public String getName() {  
            return ErgoDex.this.getName();
        }

    
        @Override
        public String getStatus() {
            return m_status.get();
        }

        @Override
        public void setStatus(String value) {
            
            switch(value){
                case App.STATUS_STOPPED:
                    m_menuBtn.setId("menuTabBtn");
                    shutdown();
                    
                break;
                case App.STATUS_MINIMIZED:
                    m_menuBtn.setId("minimizedMenuBtn"); 
                break;
                case App.STATUS_STARTED:
                    m_menuBtn.setId("activeMenuBtn");
                break;
                
            }

            m_status.set(value);
            
        }

        @Override
        public void shutdown() {
            
            if(m_networksDataMsgInterface != null){
                getNetworksData().removeMsgListener(m_networksDataMsgInterface);
                m_networksDataMsgInterface = null;
            }

            m_ergoNetworkInterface.set(null);

            m_dexDataList.shutdown();

            m_ergoDexTab = null;

            m_appStage = null;

        

        }
 


        public void getData( EventHandler<WorkerStateEvent> onSucceeded){
            getNetworksData().getData("data", ".", "tab", ErgoDex.NETWORK_ID, onSucceeded);
        }

        public void openJson(JsonObject json){

            JsonElement itemTimeSpanElement = json != null ? json.get("itemTimeSpan") : null;
            JsonElement isErgoNetworkElement = json != null ? json.get("isErgoNetwork") : null;
            TimeSpan timeSpan = itemTimeSpanElement != null && itemTimeSpanElement.isJsonObject() ? new TimeSpan(itemTimeSpanElement.getAsJsonObject()) : new TimeSpan("1day");
            
            boolean isErgoNetwork = isErgoNetworkElement != null ? isErgoNetworkElement.getAsBoolean() : true;

            m_itemTimeSpan.set(timeSpan);
            m_isErgoNetwork = isErgoNetwork;
        }

        public JsonObject getJsonObject(){
            TimeSpan itemTimeSpan = m_itemTimeSpan == null ? new TimeSpan("1day") : m_itemTimeSpan.get();

            JsonObject networkObj = new JsonObject();
            networkObj.add("itemTimeSpan", itemTimeSpan.getJsonObject());
            networkObj.addProperty("isErgoNetworkEnabled", isErgoNetwork());
            return networkObj;
        }

        public void save(){
            getNetworksData().save("data", ".", "tab", ErgoDex.NETWORK_ID, getJsonObject());
        }

        @Override
        public SimpleStringProperty titleProperty() {

            return null;
        }
    
   

    
        
      
      
    }


    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        JsonElement subjectElement = note.get(App.CMD);
        JsonElement locationIdElement = note.get("locationId");
      
        String cmd = subjectElement != null && subjectElement.isJsonPrimitive() ? subjectElement.getAsString() : null;
        
        if(cmd != null && locationIdElement != null && locationIdElement.isJsonPrimitive()){
            String locationId = locationIdElement.getAsString();
            String locationString = getNetworksData().getLocationString(locationId);
            if(isLocationAuthorized(locationString)){
              
                note.remove("locationString");
                note.addProperty("locationString", locationString);
               
                switch(cmd){
                    case "getPoolSlippage":
                        return getPoolSlippage(note, onSucceeded, onFailed);
                    
                    case "getPoolStats":
                        return getPoolStats(note, onSucceeded, onFailed);
                    
                    case "getPlatformStats":
                        getPlatformStats(onSucceeded, onFailed);
                    return true;
                    case "getLiquidityPoolStats":
                        getLiquidityPoolStats(onSucceeded, onFailed);
                    return true;
                    case "getMarkets":
                        getMarkets(onSucceeded, onFailed);
                    return true;
                    case "getTickers":
                        getTickers(onSucceeded, onFailed);
                    case "getPoolsSummary":
                        getPoolsSummary(onSucceeded, onFailed);
                }
            }
        }
        return false;

    }

  
    public boolean getPoolSlippage(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        JsonElement poolIdElement = note != null ? note.get("poolId") : null;
        String poolId = poolIdElement != null && !poolIdElement.isJsonNull() ? poolIdElement.getAsString() : null;
        if(poolId != null){
            getPoolSlippage(poolId, getExecService(), onSucceeded, onFailed);

            return true;
        }

        return false;
    }

    public boolean getPoolStats(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        JsonElement poolIdElement = note != null ? note.get("poolId") : null;
        String poolId = poolIdElement != null && !poolIdElement.isJsonNull() ? poolIdElement.getAsString() : null;
        
        if(poolId != null){
            getPoolStats(poolId, getExecService(), onSucceeded, onFailed);

            return true;
        }

        return false;
    }   

   

  
    public ExecutorService getExecService(){
        return getNetworksData().getExecService();
    }
    // /v1/amm/pools/summary/all

    private void getPoolsSummary(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/v1/amm/pools/summary/all";

        Utils.getUrlJsonArray(urlString, getExecService(), onSucceeded, onFailed, null);
                        
    }

    private void getMarkets(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/v1/price-tracking/markets";

        Utils.getUrlJsonArray(urlString, getExecService(), onSucceeded, onFailed, null);
                        
    }
  
      
    public void getTickers(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/v1/price-tracking/cg/tickers";

        Utils.getUrlJsonArray(urlString, getNetworksData().getExecService(), onSucceeded, onFailed, null);                

    }

    public void getPlatformStats(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/v1/amm/platform/stats";

        Utils.getUrlJson(urlString, getNetworksData().getExecService(), onSucceeded, onFailed);                

    }

    public static void getPoolSlippage(String poolId, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        String urlString = API_URL + "/v1/amm/pool/" + poolId + "/slippage";

        Utils.getUrlJson(urlString, execService, onSucceeded, onFailed);
    }

    public static void getPoolStats(String poolId, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        String urlString = API_URL + "/v1/amm/pool/" + poolId + "/stats";

        Utils.getUrlJson(urlString, execService, onSucceeded, onFailed);
    }

    private void getMarketUpdate(JsonArray jsonArray){

        ArrayList<ErgoDexMarketData> tmpMarketsList = new ArrayList<>();
        long timeStamp = System.currentTimeMillis();

        SimpleBooleanProperty isChanged = new SimpleBooleanProperty(false);
        for (int i = 0; i < jsonArray.size(); i++) {
    
            JsonElement marketObjectElement = jsonArray.get(i);
            if (marketObjectElement != null && marketObjectElement.isJsonObject()) {

                JsonObject marketDataJson = marketObjectElement.getAsJsonObject();
                
                try{
                    
                    ErgoDexMarketData marketData = new ErgoDexMarketData(marketDataJson, timeStamp);
               
                    int marketIndex = getMarketDataIndexById(tmpMarketsList, marketData.getId());
                    

                    if(marketIndex != -1){
                        ErgoDexMarketData lastData = tmpMarketsList.get(marketIndex);
                        BigDecimal quoteVolume = lastData.getQuoteVolume();

                        if(marketData.getQuoteVolume().compareTo(quoteVolume) > 0){
                            tmpMarketsList.set(marketIndex, marketData);
                        }
                    }else{
                        isChanged.set(true);
                        tmpMarketsList.add(marketData);
                    }

                    
                    
                }catch(Exception e){
                    try {
                        Files.writeString(App.logFile.toPath(), "egoDex(updateMarkets): " + e.toString() + " " + marketDataJson.toString() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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
                            JsonElement poolIdElement = tickerDataJson.get("pool_id");
                            String poolId = poolIdElement != null && !poolIdElement.isJsonNull() && poolIdElement.isJsonPrimitive() ? poolIdElement.getAsString() : null;

                            if(tickerId != null){
                        
                               
                                ErgoDexMarketData marketData = getMarketDataByTickerId(tmpMarketsList, tickerId);
                            
                             
                                
                                if(marketData != null){
                                    
                                    JsonElement liquidityUsdElement = tickerDataJson.get("liquidity_in_usd");
                                  
                                    if(liquidityUsdElement != null && liquidityUsdElement.isJsonPrimitive() ){

                                        marketData.setLiquidityUSD(liquidityUsdElement.getAsBigDecimal());
       
                                    }
                                    if( poolId != null ){
                                        marketData.setPoolId(poolId);
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
                    Files.writeString(App.logFile.toPath(), "egoDex (onTickersFailed): " + onTickersFailed.getSource().getException().toString() +"\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                   
                }
             
            });
        }
    }




    private void updateMarketList(ArrayList<ErgoDexMarketData> data){
        int size = data.size();
        long timeStamp = System.currentTimeMillis();

        if(m_marketsList.size() == 0){
                for(int i = 0; i < size; i++){
                    ErgoDexMarketData marketData = data.get(i);
                    if(marketData != null){
                        m_marketsList.add(marketData);    
                    }
                }
                data.clear();
                
            sendMessage(App.LIST_CHANGED, timeStamp,NETWORK_ID, m_marketsList.size());
        }else{
            SimpleBooleanProperty changed = new SimpleBooleanProperty(false);
            for(int i = 0; i < size; i++){
                ErgoDexMarketData newMarketData = data.get(i);
                
                ErgoDexMarketData marketData = getMarketDataById(m_marketsList, newMarketData.getId());
                if(marketData != null){
                    marketData.update(newMarketData);
                }else{
                    changed.set(true);
                    m_marketsList.add(newMarketData);
                   
                }
            }
            data.clear();
            if(changed.get()){
                sendMessage(App.LIST_CHANGED, timeStamp,NETWORK_ID,  m_marketsList.size());
            }else{
                sendMessage(App.LIST_UPDATED, timeStamp,NETWORK_ID,  m_marketsList.size());
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
                    Files.writeString(App.logFile.toPath(), "\negoDex: getTickersMarkets: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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

    public static NetworkInformation getNetworkInformation(){
        return new NetworkInformation(NETWORK_ID, NAME, getAppIconString(), getSmallAppIconString(), DESCRIPTION);
    }

    public void getOrderHistory(String address, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/v1/history/order";
        
        Utils.getUrlJsonArray(urlString, getExecService(), onSucceeded, onFailed, null);

    }

    @Override
    public String getDescription(){
        return DESCRIPTION;
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
    public void getLiquidityPoolStats(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
            String urlString = API_URL + "/v1/lm/pools/stats";
       
            
            Utils.getUrlJsonArray(urlString, getExecService(), onSucceeded, onFailed, null);
    
    }
    

   

    public void getPoolChart(String poolId, long currentTime, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {


        String urlString = API_URL + "/v1/amm/pool/" + poolId + "/chart?from=0&to=" + currentTime;

        Utils.getUrlJsonArray(urlString, getExecService(), onSucceeded, onFailed, null);
    }
    
    public void getPoolChart(String poolId,long fromTime, long currentTime, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {


        String urlString = API_URL + "/v1/amm/pool/" + poolId + "/chart?from=" + fromTime + "&to=" + currentTime;

        Utils.getUrlJsonArray(urlString, getExecService(), onSucceeded, onFailed, null);
    }

 


    @Override
    public void stop(){
        setConnectionStatus(App.STOPPED);

        m_marketsList.clear();
        if (m_scheduledFuture != null && !m_scheduledFuture.isDone()) {
            m_scheduledFuture.cancel(false);
            
        }

    }
 
    //private static volatile int m_counter = 0;
    @Override
    public void start(){
      
        if(getConnectionStatus() == App.STOPPED){

            setConnectionStatus(App.STARTING);
            sendMessage(App.STARTING, System.currentTimeMillis(), NETWORK_ID, App.STARTING);
            ExecutorService executor = getNetworksData().getExecService();
            
            Runnable exec = ()->{
                //FreeMemory freeMem = Utils.getFreeMemory();
            
                getMarkets(success -> {

                    Object sourceObject = success.getSource().getValue();
                    if (sourceObject != null && sourceObject instanceof JsonArray) {
                        JsonArray marketJsonArray = (JsonArray) sourceObject;

                        /*try {
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            Files.writeString(App.logFile.toPath(), gson.toJson(marketJsonArray) +"\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e) {

                        }*/
        
                        if(getConnectionStatus() != App.STARTED){
                            setConnectionStatus(App.STARTED);
                            sendMessage(App.STARTED, System.currentTimeMillis(), NETWORK_ID, App.STARTED);
                        }
                        getMarketUpdate(marketJsonArray);
                    } 
                 
                }, (onfailed)->{
                    
                    setConnectionStatus(App.ERROR);
                    Throwable throwable = onfailed.getSource().getException();
                    String msg= throwable instanceof java.net.SocketException ? "Connection unavailable" : (throwable instanceof java.net.UnknownHostException ? "Unknown host: Spectrum Finance unreachable" : throwable.toString());
                  

                    sendMessage(App.ERROR, System.currentTimeMillis(),NETWORK_ID, msg);
                });
                

                
            };

            Runnable submitExec = ()->executor.submit(exec);

            m_scheduledFuture = m_schedualedExecutor.scheduleAtFixedRate(submitExec, 0, 7000, TimeUnit.MILLISECONDS);


       
           
        }
    }

    private ArrayList<String> m_authorizedLocations = new ArrayList<>();

    private boolean isLocationAuthorized(String locationString){
        
        return locationString.equals(ErgoNetwork.NAME) || m_authorizedLocations.contains(locationString);
    }
    
  
    @Override
    public Object sendNote(JsonObject note){
    
        JsonElement subjectElement = note.get(App.CMD);
        JsonElement locationIdElement = note.get("locationId");

        String cmd = subjectElement != null && subjectElement.isJsonPrimitive() ? subjectElement.getAsString() : null;
        String locationId = locationIdElement != null && !locationIdElement.isJsonNull() && locationIdElement.isJsonPrimitive() ? locationIdElement.getAsString() : null;

        if(cmd != null && locationId != null){
            
            String locationString = getNetworksData().getLocationString(locationId);
            
            if(isLocationAuthorized(locationString)){
            
                note.remove("locationString");
                note.addProperty("locationString", locationString);
                switch(cmd){
                    case "getQuote":
                        return getQuote(note);
                    case "getTokenQuoteInErg":
                        return getTokenQuoteInErg(note);
                    case "getErgoUSDQuote":
                        return getErgoUSDQuote();
                    case "getQuoteById":
                        return getQuoteById(note);
                    case "getQuoteBySymbol":
                        return getQuoteBySymbol(note);
                }
            }
        }
        
    
        return null;
    }


    private List<ErgoDexMarketData> m_searchList ;
    private Optional<ErgoDexMarketData> m_quoteOptional;


    private ErgoDexMarketData findMarketDataById(String baseId, String quoteId){
        if(m_marketsList != null && baseId != null && quoteId != null){
            int size = m_marketsList.size();
            for(int i = 0; i < size ; i++){
                ErgoDexMarketData data = m_marketsList.get(i);
                if(data.getBaseId().equals(baseId) && data.getQuoteId().equals(quoteId)){
                    data.setExchangeName(NETWORK_ID);
                    return data;
                }
            }
        }
        return null;
    }

    private ErgoDexMarketData findMarketDataBySymbol(String baseSymbol, String quoteSymbol){
        if(m_marketsList != null && baseSymbol != null && quoteSymbol != null){
            int size = m_marketsList.size();
            for(int i = 0; i < size ; i++){
                ErgoDexMarketData data = m_marketsList.get(i);
                if(data.getBaseSymbol().equals(baseSymbol) && data.getQuoteSymbol().equals(quoteSymbol)){
                    data.setExchangeName(NETWORK_ID);
                    return data;
                }
            }
        }
        return null;
    }

    private Object getQuote(JsonObject json){
        JsonElement baseTypeElement = json.get("baseType");
        JsonElement quoteTypeElement = json.get("quoteType");
        JsonElement baseElement = json.get("base");
        JsonElement quoteElement = json.get("quote");

        String baseType = baseTypeElement != null && baseTypeElement.isJsonPrimitive() ? baseTypeElement.getAsString() : null;
        String quoteType = quoteTypeElement != null && quoteTypeElement.isJsonPrimitive() ? quoteTypeElement.getAsString() : null;
        String baseString = baseElement != null && baseElement.isJsonPrimitive() ? baseElement.getAsString() : null;
        String quoteString = quoteElement != null && quoteElement.isJsonPrimitive() ? quoteElement.getAsString() : null;

        m_searchList = null;
        if(baseType != null && baseString != null){
            switch(baseType){
         
                case "symbol":
                    m_searchList = m_marketsList.stream().filter(item -> item.getBaseSymbol().toLowerCase().equals(baseString.toLowerCase())).collect(Collectors.toList());
                break;
                case "id":
                    m_searchList = m_marketsList.stream().filter(item -> item.getBaseId().equals(baseString)).collect(Collectors.toList());
                 
                    
                    break;
            }
        }


        m_searchList = m_searchList != null ? m_searchList : m_marketsList;

        if(quoteType != null && quoteString != null){
            switch(quoteType){
                case "firstSymbolContains":
                    m_quoteOptional = m_searchList.stream().filter(item -> item.getQuoteSymbol().contains(quoteString)).findFirst();
                    if(m_quoteOptional.isPresent()){
                      
                        return m_quoteOptional.get().getPriceQuote();
                    }
                break;
                case "firstId":
                    m_quoteOptional = m_searchList.stream().filter(item -> item.getQuoteId().equals(quoteString)).findFirst();
                  
                    
                    if(m_quoteOptional.isPresent()){
                     
                        return m_quoteOptional.get().getPriceQuote();
                    }
                break;
            }
        }
        m_searchList = null;
        return null;
    }

    private Object getErgoUSDQuote(){
        return findMarketDataById(ErgoCurrency.TOKEN_ID, SIGUSD_ID).getPriceQuote();
    }

    private Object getTokenQuoteInErg(JsonObject note){
       
        
        JsonElement idElement = note != null ? note.get("tokenId") : null;
        String tokenId = idElement != null && !idElement.isJsonNull() && idElement.isJsonPrimitive() ? idElement.getAsString() : null;
        if(tokenId != null){
            ErgoDexMarketData data = findMarketDataById(ErgoCurrency.TOKEN_ID, tokenId);
            if(data != null){
                return data.getPriceQuote(true);
            }
        }
        return null;
    }
    
    private Object getQuoteById(JsonObject note){
        JsonElement baseIdElement = note != null ? note.get("baseId") : null;
        JsonElement quoteIdElement = note != null ? note.get("quoteId") : null;

        String baseId = baseIdElement != null && !baseIdElement.isJsonNull() && baseIdElement.isJsonPrimitive() ? baseIdElement.getAsString() : null;
        String quoteId = quoteIdElement != null && !quoteIdElement.isJsonNull() && quoteIdElement.isJsonPrimitive() ? quoteIdElement.getAsString() : null;

        if(baseId != null && quoteId != null){
            return findMarketDataById(baseId, quoteId).getPriceQuote();
        }
        return null;
    }

    private Object getQuoteBySymbol(JsonObject note){
        JsonElement baseSymbolElement = note != null ? note.get("baseSymbol") : null;
        JsonElement quoteSymbolElement = note != null ? note.get("quoteSymbol") : null;

        String baseSymbol = baseSymbolElement != null && !baseSymbolElement.isJsonNull() && baseSymbolElement.isJsonPrimitive() ? baseSymbolElement.getAsString() : null;
        String quoteSymbol = quoteSymbolElement != null && !quoteSymbolElement.isJsonNull() && quoteSymbolElement.isJsonPrimitive() ? quoteSymbolElement.getAsString() : null;

        if(baseSymbol != null && quoteSymbol != null){
            return findMarketDataBySymbol(baseSymbol, quoteSymbol).getPriceQuote();
        }
        return null;
    }
  

    @Override
    public void shutdown(){
        super.shutdown();

    
    }
 

   
    
}
