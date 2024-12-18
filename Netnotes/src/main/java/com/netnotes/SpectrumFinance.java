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

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class SpectrumFinance extends Network implements NoteInterface {

    public final static String DESCRIPTION = "Spectrum Finance is an open source cross-chain decentralized exchange (DEX) for fast, trustless cross-chain swaps, liquidity provision & liquidity mining.";

    public final static String NAME = "Spectrum Finance";
    public final static String WEB_URL = "https://spectrum.fi/";
    public final static String NETWORK_ID = "SPECTRUM_FINANCE";
    public final static String API_URL = "https://api.spectrum.fi";

    private final static NetworkInformation[]  SUPPORTED_NETWORKS = new NetworkInformation[]{
        ErgoNetwork.getNetworkInformation()
    };

    private String m_currentNetworkId = ErgoNetwork.NETWORK_ID;
    
    public static java.awt.Color POSITIVE_HIGHLIGHT_COLOR = new java.awt.Color(0xff3dd9a4, true);
    public static java.awt.Color POSITIVE_COLOR = new java.awt.Color(0xff028A0F, true);

    public static java.awt.Color NEGATIVE_COLOR = new java.awt.Color(0xff9A2A2A, true);
    public static java.awt.Color NEGATIVE_HIGHLIGHT_COLOR = new java.awt.Color(0xffe96d71, true);
    public static java.awt.Color NEUTRAL_COLOR = new java.awt.Color(0x111111);

    public final static String ERG_ID = "0000000000000000000000000000000000000000000000000000000000000000";
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

    public static final String MARKETS_LIST = "MARKETS_LIST";

    private ArrayList<SpectrumMarketData> m_marketsList = new ArrayList<>();



    private ScheduledExecutorService m_schedualedExecutor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> m_scheduledFuture = null;


    private String m_locationId;

    public SpectrumFinance(NetworksData networksData, String locationId) {
        super(new Image(getAppIconString()), NAME, NETWORK_ID, networksData);
        setKeyWords(new String[]{"ergo", "exchange", "usd", "ergo tokens", "dApp", "SigUSD"});
        m_locationId = locationId;
    }

 



    public static String getAppIconString(){
        return "/assets/spectrum-150.png";
    }

 
    public ArrayList<SpectrumMarketData> marketsList(){
        return m_marketsList;
    }
    
   
    public  Image getSmallAppIcon() {
        return new Image(getSmallAppIconString());
    }

    public static String getSmallAppIconString(){
        return "/assets/spectrumFinance.png";
    }




    public Stage getAppStage() {
        return m_appStage;
    }

    public SpectrumMarketData[] getMarketDataArray(){
        SpectrumMarketData[] dataArray = new SpectrumMarketData[m_marketsList.size()];
        return dataArray = m_marketsList.toArray(dataArray);
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
                return SpectrumFinance.this.getAppIcon();
            }


            public SimpleObjectProperty<LocalDateTime> getLastUpdated(){
                return getLastUpdated();
            }

            public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
                return SpectrumFinance.this.sendNote(note, onSucceeded, onFailed);
            }

            public Object sendNote(JsonObject note){
                return SpectrumFinance.this.sendNote(note);
            }

            public JsonObject getJsonObject(){
                JsonObject spectrumObject = SpectrumFinance.this.getJsonObject();
                spectrumObject.addProperty("apiUrl", API_URL);
                spectrumObject.addProperty("website", WEB_URL);
                spectrumObject.addProperty("description", DESCRIPTION);
                return spectrumObject;
            }

            public TabInterface getTab(Stage appStage, SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject,  Button networkBtn){
                return SpectrumFinance.this.getTab(appStage, heightObject, widthObject, networkBtn);
            }

            public NetworksData getNetworksData(){
                return SpectrumFinance.this.getNetworksData();
            }

            public NoteInterface getParentInterface(){
                return getParentInterface();
            }

            public void addUpdateListener(ChangeListener<LocalDateTime> changeListener){
                SpectrumFinance.this.addUpdateListener(changeListener);
            }

            public void removeUpdateListener(){
                SpectrumFinance.this.removeUpdateListener();
            }

            public void shutdown(){}

            public SimpleObjectProperty<LocalDateTime> shutdownNowProperty(){
                return null;
            }

            public void addMsgListener(NoteMsgInterface listener){
                if(listener != null && listener.getId() != null){
                    SpectrumFinance.this.addMsgListener(listener);
                }
            }
            public boolean removeMsgListener(NoteMsgInterface listener){
                
                return SpectrumFinance.this.removeMsgListener(listener);
            }

            public int getConnectionStatus(){
                return SpectrumFinance.this.getConnectionStatus();
            }

            public void setConnectionStatus(int status){

            }


            public String getDescription(){
                return SpectrumFinance.this.getDescription();
            }
        };
    }


    

    private SpectrumFinanceTab m_spectrumFinanceTab = null;;

    @Override
    public TabInterface getTab(Stage appStage,  SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject, Button menuBtn){
        if(m_spectrumFinanceTab != null){
            return m_spectrumFinanceTab;
        }else{
            m_spectrumFinanceTab = new SpectrumFinanceTab(appStage,  heightObject, widthObject, menuBtn);
            return m_spectrumFinanceTab;
        }
    }

    private class SpectrumFinanceTab extends AppBox implements TabInterface{
        private Button m_menuBtn;
        private SpectrumDataList m_spectrumData = null;

        private SimpleObjectProperty<NoteInterface> m_networkInterface = new SimpleObjectProperty<>(null);
        private NoteMsgInterface m_networkMsgInterface;
        private SimpleObjectProperty<TimeSpan> m_itemTimeSpan = new SimpleObjectProperty<TimeSpan>(new TimeSpan("1day"));
        private SimpleStringProperty m_status = new SimpleStringProperty(App.STATUS_STOPPED);
        private HBox m_menuBar;
        private VBox m_bodyPaddingBox;

        public SpectrumFinanceTab(Stage appStage,  SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject, Button menuBtn){
            super(getNetworkId());
            addListeners();
            getData();
            m_appStage = appStage;
            m_menuBtn = menuBtn;
            setPrefWidth(App.DEFAULT_STATIC_WIDTH);
            setMaxWidth(App.DEFAULT_STATIC_WIDTH);

 
            double defaultGridWidth = App.DEFAULT_STATIC_WIDTH;
            double defaultGridHeight = heightObject.get() - 100;      

            prefHeightProperty().bind(heightObject);

            
            SimpleDoubleProperty gridWidth = new SimpleDoubleProperty(defaultGridWidth);
            SimpleDoubleProperty gridHeight = new SimpleDoubleProperty(defaultGridHeight);
            SimpleObjectProperty<HBox> currentBox = new SimpleObjectProperty<>(null);


            m_spectrumData = new SpectrumDataList(FriendlyId.createFriendlyId(), appStage, SpectrumFinance.this, gridWidth,gridHeight, currentBox, m_itemTimeSpan, m_networkInterface);


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

                switch(m_spectrumData.getSortMethod().getType()){
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

                m_spectrumData.sort();
                m_spectrumData.updateGridBox();
                m_spectrumData.getLastUpdated().set(LocalDateTime.now());
            };

           // updateSortTypeSelected.run();

            sortLiquidityItem.setOnAction(e->{
                SpectrumSort sortMethod = m_spectrumData.getSortMethod();
                sortMethod.setType(sortLiquidityItem.getText());
                updateSortTypeSelected.run();
            });

            sortBaseVolItem.setOnAction(e->{
                SpectrumSort sortMethod = m_spectrumData.getSortMethod();
                sortMethod.setType(sortBaseVolItem.getText());
                updateSortTypeSelected.run();
            });

            sortQuoteVolItem.setOnAction(e->{
                SpectrumSort sortMethod = m_spectrumData.getSortMethod();
                sortMethod.setType(sortQuoteVolItem.getText());
                updateSortTypeSelected.run();
            });

            sortLastPriceItem.setOnAction(e->{
                SpectrumSort sortMethod = m_spectrumData.getSortMethod();
                sortMethod.setType(sortLastPriceItem.getText());
                updateSortTypeSelected.run();
            });


            BufferedButton sortDirectionButton = new BufferedButton(m_spectrumData.getSortMethod().isAsc() ? "/assets/sortAsc.png" : "/assets/sortDsc.png", App.MENU_BAR_IMAGE_WIDTH);
            sortDirectionButton.setOnAction(e->{
                SpectrumSort sortMethod = m_spectrumData.getSortMethod();
                sortMethod.setDirection(sortMethod.isAsc() ? SpectrumSort.SortDirection.DSC : SpectrumSort.SortDirection.ASC);
                sortDirectionButton.setImage(new Image(sortMethod.isAsc() ? "/assets/sortAsc.png" : "/assets/sortDsc.png"));
                m_spectrumData.sort();
                m_spectrumData.updateGridBox();
                m_spectrumData.getLastUpdated().set(LocalDateTime.now());
            });

            BufferedButton swapTargetButton = new BufferedButton(m_spectrumData.isInvertProperty().get() ? "/assets/targetSwapped.png" : "/assets/targetStandard.png", App.MENU_BAR_IMAGE_WIDTH);
            swapTargetButton.setOnAction(e->{
                m_spectrumData.isInvertProperty().set(!m_spectrumData.isInvertProperty().get());
            });

            m_spectrumData.isInvertProperty().addListener((obs,oldval,newval)->{
                swapTargetButton.setImage(new Image(newval ? "/assets/targetSwapped.png" : "/assets/targetStandard.png"));
        
            });
            



            TextField searchField = new TextField();
            searchField.setPromptText("Search");
            searchField.setId("urlField");
            searchField.setPrefWidth(200);
            searchField.setPadding(new Insets(2, 10, 3, 10));
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                m_spectrumData.setSearchText(searchField.getText());
            });

            Region menuBarRegion = new Region();
            HBox.setHgrow(menuBarRegion, Priority.ALWAYS);

            
            Tooltip currentNetworkTip = new Tooltip("Install: Ergo Network");
            currentNetworkTip.setShowDelay(new javafx.util.Duration(100));
            currentNetworkTip.setFont(App.txtFont);

            


            ImageView networkMenuBtnImageView = new ImageView();
            networkMenuBtnImageView.setPreserveRatio(true);
            networkMenuBtnImageView.setFitWidth(30);

            MenuButton networkMenuBtn = new MenuButton();
            networkMenuBtn.setGraphic(networkMenuBtnImageView);
            networkMenuBtn.setPadding(new Insets(0, 3, 0, 0));
            
            Tooltip networkTip = new Tooltip("Network: (select)");
            networkTip.setShowDelay(new javafx.util.Duration(50));
            networkTip.setFont(App.txtFont);

            networkMenuBtn.setTooltip(networkTip);
            
            networkMenuBtn.addEventFilter(MouseEvent.MOUSE_PRESSED, (e)->{
                getNetworkMenu(networkMenuBtn);
            });

            ChangeListener<NoteInterface> networkChangeListener = (obs,oldval,newval)->{
                if(newval != null){
                    networkMenuBtnImageView.setImage(newval.getAppIcon());
                    networkTip.setText("Network: " + newval.getName());
                }
            };

            m_networkInterface.addListener(networkChangeListener);

            if(m_networkInterface.get() != null){
                NoteInterface networkInterface = m_networkInterface.get();
                networkMenuBtnImageView.setImage(networkInterface.getAppIcon());
                networkTip.setText("Network: " + networkInterface.getName());
            }

            VBox networkMenuBtnBox = new VBox( networkMenuBtn);

            getNetworkMenu(networkMenuBtn);
    
            VBox.setVgrow(networkMenuBtnBox,Priority.ALWAYS);


            HBox rightSideMenu = new HBox(networkMenuBtnBox);
           
            rightSideMenu.setId("rightSideMenuBar");
            rightSideMenu.setPadding(new Insets(0, 3, 0, 10));
            rightSideMenu.setAlignment(Pos.CENTER_RIGHT);

            Region menuBarRegion1 = new Region();
            menuBarRegion1.setMinWidth(10);

         
            MenuButton timeSpanBtn = new MenuButton();
            timeSpanBtn.setId("urlMenuButton");
            timeSpanBtn.setMinWidth(100);
            timeSpanBtn.setPrefWidth(100);
            timeSpanBtn.textProperty().bind( Bindings.concat( m_itemTimeSpan.asString(), " 🞃"));
            timeSpanBtn.setAlignment(Pos.CENTER_RIGHT);
            timeSpanBtn.setPadding(Insets.EMPTY);
    
            
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

            m_menuBar = new HBox(sortTypeButton,sortDirectionButton,swapTargetButton, menuBarRegion1, searchField, menuBarRegion,timeSpanBtn, rightSideMenu);
            HBox.setHgrow(m_menuBar, Priority.ALWAYS);
            m_menuBar.setAlignment(Pos.CENTER_LEFT);
            m_menuBar.setId("menuBar");
            m_menuBar.setPadding(new Insets(1, 2, 1, 5));
            m_menuBar.setMinHeight(25);


      

            VBox chartList = m_spectrumData.getGridBox();
  
            ScrollPane scrollPane = new ScrollPane(chartList);
            scrollPane.setPadding(new Insets(2));
        //    HBox headingsBox = new HBox();

            HBox menuBarBox = new HBox(m_menuBar);
            HBox.setHgrow(menuBarBox,Priority.ALWAYS);
            menuBarBox.setPadding(new Insets(0,0,10,0));
            
            m_bodyPaddingBox = new VBox(menuBarBox, scrollPane);
            m_bodyPaddingBox.setPadding(new Insets(0,5,0,5));

     

            Text lastUpdatedTxt = new Text("Updated ");
            lastUpdatedTxt.setFill(App.formFieldColor);
            lastUpdatedTxt.setFont(App.titleFont);

            TextField lastUpdatedField = new TextField();
            lastUpdatedField.setEditable(false);
            lastUpdatedField.setId("formFieldSmall");
            lastUpdatedField.setPrefWidth(230);

            Binding<String> errorTxtBinding = Bindings.createObjectBinding(()->(m_spectrumData.statusMsgProperty().get().startsWith("Error") ? m_spectrumData.statusMsgProperty().get() : "") ,m_spectrumData.statusMsgProperty());

            Text errorText = new Text("");
            errorText.setFont(App.titleFont);
            errorText.setFill(App.altColor);
            errorText.textProperty().bind(errorTxtBinding);
            
            Region lastUpdatedRegion = new Region();
            lastUpdatedRegion.setMinWidth(10);
            HBox.setHgrow(lastUpdatedRegion, Priority.ALWAYS);

            HBox lastUpdatedBox = new HBox(errorText, lastUpdatedRegion, lastUpdatedTxt, lastUpdatedField);
            lastUpdatedBox.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(lastUpdatedBox, Priority.ALWAYS);

            VBox footerVBox = new VBox(lastUpdatedBox);
            HBox.setHgrow(footerVBox, Priority.ALWAYS);
            footerVBox.setPadding(new Insets(0,5,2,5));



            getChildren().addAll( m_bodyPaddingBox, footerVBox);


            

            m_bodyPaddingBox.prefWidthProperty().bind(widthProperty().subtract(1));
            scrollPane.prefViewportWidthProperty().bind(widthObject);

           // Binding<Double> scrollWidth = Bindings.createObjectBinding(()->scrollPane.viewportBoundsProperty().get() != null ? (scrollPane.viewportBoundsProperty().get().getWidth() < 300 ? 300 : scrollPane.viewportBoundsProperty().get().getWidth() ) : 300, scrollPane.viewportBoundsProperty());

           scrollPane.viewportBoundsProperty().addListener((obs,oldval,newval)->{
              
                double width = newval.getWidth();
           
                gridWidth.set( width < 300 ? 300 : width );
       
            
                
            });

            Binding<Double> scrollHeight = Bindings.createObjectBinding(()->scrollPane.viewportBoundsProperty().get() != null ? scrollPane.viewportBoundsProperty().get().getHeight() : defaultGridHeight, scrollPane.viewportBoundsProperty());
            gridHeight.bind(scrollHeight);

        
            scrollPane.prefViewportHeightProperty().bind(heightObject.subtract(footerVBox.heightProperty()));

            chartList.prefWidthProperty().bind(scrollPane.prefViewportWidthProperty().subtract(40));
       
            m_spectrumData.addUpdateListener((obs,oldval,newval)->{
                if(newval != null){
         
                    lastUpdatedField.setText(Utils.formatDateTimeString(newval));
                    
                }else{
                    lastUpdatedField.setText(m_spectrumData.statusMsgProperty().get());
                }
            });
        
        }




        

        @Override
        public String getName() {  
            return SpectrumFinance.this.getName();
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
                    m_spectrumFinanceTab = null;
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
            m_spectrumData.shutdown();
            m_spectrumData.removeUpdateListener();

            m_networkInterface.set(null);

            m_appStage = null;

        }
 

        private ChangeListener<NoteInterface> m_networkChanged;

        private void addListeners(){
            m_networkChanged = (obs,oldval,newval)->{
                if(oldval != null && m_networkMsgInterface != null){
                   
                    oldval.removeMsgListener(m_networkMsgInterface);
                    m_networkMsgInterface = null;
                }

                if(newval != null){
                
                    String networkInterfaceId = FriendlyId.createFriendlyId();
                    
                    m_networkMsgInterface = new NoteMsgInterface(){
        
                        public String getId() {
                            return networkInterfaceId;
                        }
                        public void sendMessage(int code, long timeStamp, String networkId, Number num){
                            switch(code){
                                case App.STARTED:
                                   
                                break;
                                case App.STOPPED:
                                
                                break;
                                case App.LIST_CHANGED:
                                case App.LIST_UPDATED:
                                
                                break;
                            }
                        }
                    
                        public void sendMessage(int code, long timestamp, String networkId, String msg){
                                
                        }
                    
                        
                    };
                    
                    newval.addMsgListener(m_networkMsgInterface);
                }

              
            };
           
            m_networkInterface.addListener(m_networkChanged);
           
        }

        public void getData(){
            openJson(getNetworksData().getData("data", ".", "tab", SpectrumFinance.NETWORK_ID));
        }

        public void openJson(JsonObject json){
            JsonElement currentNetworkIdElement = json != null ? json.get("currentNetworkId") : null;
            JsonElement itemTimeSpanElement = json != null ? json.get("itemTimeSpan") : null;

            String currentNetworkId = currentNetworkIdElement == null ? ErgoNetwork.NETWORK_ID : (currentNetworkIdElement.isJsonNull() ? null : currentNetworkIdElement.getAsString());
            
            setCurrentNetworkId(currentNetworkId, false);
            TimeSpan timeSpan = itemTimeSpanElement != null && itemTimeSpanElement.isJsonObject() ? new TimeSpan(itemTimeSpanElement.getAsJsonObject()) : new TimeSpan("1day");
            
            m_itemTimeSpan.set(timeSpan);
        }

        public JsonObject getJsonObject(){
            TimeSpan itemTimeSpan = m_itemTimeSpan == null ? new TimeSpan("1day") : m_itemTimeSpan.get();

            JsonObject networkObj = new JsonObject();
            networkObj.addProperty("currentNetworkId", m_currentNetworkId);
            networkObj.add("itemTimeSpan", itemTimeSpan.getJsonObject());
            return networkObj;
        }

        public void save(){
            getNetworksData().save("data", ".", "tab", SpectrumFinance.NETWORK_ID, getJsonObject());
        }

        @Override
        public SimpleStringProperty titleProperty() {

            return null;
        }
    
   

        public void setCurrentNetworkId(String networkId, boolean update){
            m_currentNetworkId = networkId;

            NoteInterface networkInterface = m_currentNetworkId != null ? getNetworksData().getNetwork(m_currentNetworkId) : null;
    
            m_networkInterface.set(networkInterface);
     
            if(update){
                save();
            }    
        }
    
        
        public void getNetworkMenu(MenuButton networkMenuBtn){
            networkMenuBtn.getItems().clear();

            String selectedMarketId = m_currentNetworkId;
        
            SimpleBooleanProperty found = new SimpleBooleanProperty(false);
            
            for(int i = 0; i < SUPPORTED_NETWORKS.length ; i++ ){
                NetworkInformation networkInfo = SUPPORTED_NETWORKS[i];
               

                NoteInterface noteInterface = getNetworksData().getNetwork(networkInfo.getNetworkId());
                ImageView imgView = new ImageView(new Image(networkInfo.iconString()));
                imgView.setPreserveRatio(true);
                imgView.setFitHeight(30);
                
                boolean selected = selectedMarketId.equals(networkInfo.getNetworkId());
                MenuItem menuItem = new MenuItem(networkInfo.getNetworkName() + (noteInterface == null ? ": (not installed)" : (selectedMarketId != null && selected ? " (selected)" :  "") ), imgView);
        

                if(selected){
                    ImageView menuBtnGraphic = new ImageView(new Image(networkInfo.iconString()));
                    menuBtnGraphic.setPreserveRatio(true);
                    menuBtnGraphic.setFitWidth(30);
                    networkMenuBtn.setGraphic(menuBtnGraphic);

                    menuItem.setId("selectedMenuItem");
                    found.set(true);
                }
                
                menuItem.setUserData(networkInfo);
            
                menuItem.setOnAction(e->{
                    NetworkInformation mInfo = (NetworkInformation) menuItem.getUserData();
                
                    setCurrentNetworkId(mInfo.getNetworkId(), true);

                    ImageView menuBtnGraphic = new ImageView(new Image(mInfo.getSmallIconString()));
                    menuBtnGraphic.setPreserveRatio(true);
                    menuBtnGraphic.setFitWidth(30);

                    networkMenuBtn.setGraphic(menuBtnGraphic);


                    getNetworkMenu(networkMenuBtn);
                });

                networkMenuBtn.getItems().add(menuItem);
            }

            MenuItem noneItem = new MenuItem("(none)");
            if(selectedMarketId == null){
                noneItem.setId("selectedMenuItem");
            }
            noneItem.setOnAction(e->{
                if(selectedMarketId != null){
                    setCurrentNetworkId(null, true);
                    ImageView menuBtnGraphic = new ImageView(new Image("/assets/globe-outline-white-30.png"));
                    menuBtnGraphic.setPreserveRatio(true);
                    menuBtnGraphic.setFitWidth(30);
                    networkMenuBtn.setGraphic(menuBtnGraphic);
                    getNetworkMenu(networkMenuBtn);
                }
            });

            if(!found.get()){
                ImageView menuBtnGraphic = new ImageView(new Image("/assets/globe-outline-white-120.png"));
                menuBtnGraphic.setPreserveRatio(true);
                menuBtnGraphic.setFitWidth(30);

                networkMenuBtn.setGraphic(menuBtnGraphic);
            }
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
                        Files.writeString(App.logFile.toPath(), "SpectrumFinance(updateMarkets): " + e.toString() + " " + marketDataJson.toString() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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
                    Files.writeString(App.logFile.toPath(), "SpectrumFinance (onTickersFailed): " + onTickersFailed.getSource().getException().toString() +"\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                   
                }
             
            });
        }
    }




    private void updateMarketList(ArrayList<SpectrumMarketData> data){
        int size = data.size();
        long timeStamp = System.currentTimeMillis();

        if(m_marketsList.size() == 0){
                for(int i = 0; i < size; i++){
                    SpectrumMarketData marketData = data.get(i);
                    if(marketData != null){
                        m_marketsList.add(marketData);    
                    }
                }
                data.clear();
                
            sendMessage(App.LIST_CHANGED, timeStamp,NETWORK_ID, m_marketsList.size());
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
                            Files.writeString( new File(getNetworksData().getDataDir().getAbsolutePath() + "/markets.json").toPath(), marketJsonArray.toString());
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


    private List<SpectrumMarketData> m_searchList ;
    private Optional<SpectrumMarketData> m_quoteOptional;


    private SpectrumMarketData findMarketDataById(String baseId, String quoteId){
        if(m_marketsList != null && baseId != null && quoteId != null){
            int size = m_marketsList.size();
            for(int i = 0; i < size ; i++){
                SpectrumMarketData data = m_marketsList.get(i);
                if(data.getBaseId().equals(baseId) && data.getQuoteId().equals(quoteId)){
                    data.setExchangeId(NETWORK_ID);
                    return data;
                }
            }
        }
        return null;
    }

    private SpectrumMarketData findMarketDataBySymbol(String baseSymbol, String quoteSymbol){
        if(m_marketsList != null && baseSymbol != null && quoteSymbol != null){
            int size = m_marketsList.size();
            for(int i = 0; i < size ; i++){
                SpectrumMarketData data = m_marketsList.get(i);
                if(data.getBaseSymbol().equals(baseSymbol) && data.getQuoteSymbol().equals(quoteSymbol)){
                    data.setExchangeId(NETWORK_ID);
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
        String base = baseElement != null && baseElement.isJsonPrimitive() ? baseElement.getAsString() : null;
        String quote = quoteElement != null && quoteElement.isJsonPrimitive() ? quoteElement.getAsString() : null;

        m_searchList = null;
        if(baseType != null && base != null){
            switch(baseType){
         
                case "symbol":
                    m_searchList = m_marketsList.stream().filter(item -> item.getBaseSymbol().toLowerCase().equals(base.toLowerCase())).collect(Collectors.toList());
                break;
                case "id":
                    m_searchList = m_marketsList.stream().filter(item -> item.getBaseId().equals(base)).collect(Collectors.toList());
                 
                    
                    break;
            }
        }


        m_searchList = m_searchList != null ? m_searchList : m_marketsList;

        if(quoteType != null && quote != null){
            switch(quoteType){
                case "firstSymbolContains":
                    m_quoteOptional = m_searchList.stream().filter(item -> item.getQuoteSymbol().contains(quote)).findFirst();
                    if(m_quoteOptional.isPresent()){
                        return m_quoteOptional.get();
                    }
                break;
                case "firstId":
                    m_quoteOptional = m_searchList.stream().filter(item -> item.getQuoteId().equals(quote)).findFirst();
                  
                    
                    if(m_quoteOptional.isPresent()){
                     
                        SpectrumMarketData data = m_quoteOptional.get();
                        data.setExchangeId(NETWORK_ID);
                        return data;
                    }
                break;
            }
        }
        m_searchList = null;
        return null;
    }

    private Object getErgoUSDQuote(){
        return findMarketDataById(ErgoCurrency.TOKEN_ID, SIGUSD_ID);
    }

    private Object getTokenQuoteInErg(JsonObject note){
       
        
        JsonElement idElement = note != null ? note.get("tokenId") : null;
        String tokenId = idElement != null && !idElement.isJsonNull() && idElement.isJsonPrimitive() ? idElement.getAsString() : null;
        if(tokenId != null){
            SpectrumMarketData data = findMarketDataById(ERG_ID, tokenId);
            if(data != null){
                SpectrumMarketData invertedQuote = data.clone(true);
                invertedQuote.setExchangeId(NETWORK_ID);
                return invertedQuote;
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
            return findMarketDataById(baseId, quoteId);
        }
        return null;
    }

    private Object getQuoteBySymbol(JsonObject note){
        JsonElement baseSymbolElement = note != null ? note.get("baseSymbol") : null;
        JsonElement quoteSymbolElement = note != null ? note.get("quoteSymbol") : null;

        String baseSymbol = baseSymbolElement != null && !baseSymbolElement.isJsonNull() && baseSymbolElement.isJsonPrimitive() ? baseSymbolElement.getAsString() : null;
        String quoteSymbol = quoteSymbolElement != null && !quoteSymbolElement.isJsonNull() && quoteSymbolElement.isJsonPrimitive() ? quoteSymbolElement.getAsString() : null;

        if(baseSymbol != null && quoteSymbol != null){
            return findMarketDataBySymbol(baseSymbol, quoteSymbol);
        }
        return null;
    }
  

    @Override
    public void shutdown(){
        super.shutdown();
    }
 

   
    
}
