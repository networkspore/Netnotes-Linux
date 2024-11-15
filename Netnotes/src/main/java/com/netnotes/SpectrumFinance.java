package com.netnotes;

import java.io.File;
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
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class SpectrumFinance extends Network implements NoteInterface {

    public static String DESCRIPTION = "Spectrum Finance is a cross-chain decentralized exchange (DEX).";
    public static String SUMMARY = "";
    public static String NAME = "Spectrum Finance";
    public final static String NETWORK_ID = "SPECTRUM_FINANCE";

    private static NetworkInformation[]  SUPPORTED_NETWORKS = new NetworkInformation[]{
        ErgoNetwork.getNetworkInformation(),
        NetworksData.NO_NETWORK
    };

    private String m_currentNetworkId = ErgoNetwork.NETWORK_ID;
    

    public static String API_URL = "https://api.spectrum.fi";

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

    private ArrayList<SpectrumMarketData> m_marketsList = new ArrayList<>();



    private ScheduledExecutorService m_schedualedExecutor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> m_scheduledFuture = null;


  

    public SpectrumFinance(NetworksData networksData) {
        super(new Image(getAppIconString()), NAME, NETWORK_ID, networksData);
        setKeyWords(new String[]{"ergo", "exchange", "usd", "ergo tokens", "dApp", "SigUSD"});
        setAppDir();
    }

 
    public void setAppDir() {
     
        m_appDir = new File(getNetworksData().getAppDir().getAbsolutePath() + "/" + NAME);
       
        if (!m_appDir.isDirectory()) {

            try {
                Files.createDirectories(m_appDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }

        }
        
    }

    public File getAppDir() {
        return m_appDir;
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


    @Override
    public void addMsgListener(NoteMsgInterface item){
        try {
            Files.writeString(App.logFile.toPath(), "\nspectrum add msgListener: " + item.getId() + " " + getConnectionStatus(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
 
        }
        super.addMsgListener(item);
    }

    @Override
    public boolean removeMsgListener(NoteMsgInterface item){
        

        boolean removed = super.removeMsgListener(item);
        try {
            Files.writeString(App.logFile.toPath(), "\nspectrum remove msg listneer: " + removed + " " + item.getId() + " " + getConnectionStatus(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
 
        }
        return removed;
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

   



    
    public String getType(){
        return App.APP_TYPE;
    }
    private SpectrumFinanceTab m_spectrumFinanceTab = null;;

    @Override
    public TabInterface getTab(Stage appStage, String locationId,  SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject, Button menuBtn){
        m_spectrumFinanceTab = new SpectrumFinanceTab(appStage, locationId, heightObject, widthObject, menuBtn);
        return m_spectrumFinanceTab;
    }

    private class SpectrumFinanceTab extends AppBox implements TabInterface{
        private Button m_menuBtn;
        private SpectrumDataList m_spectrumData = null;

        private SimpleObjectProperty<NoteInterface> m_networkInterface = new SimpleObjectProperty<>(null);
        private NoteMsgInterface m_networkMsgInterface;
        private SimpleObjectProperty<TimeSpan> m_itemTimeSpan = new SimpleObjectProperty<TimeSpan>();
        private SimpleBooleanProperty m_current = new SimpleBooleanProperty(false);

        public SpectrumFinanceTab(Stage appStage, String locationId,  SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject, Button menuBtn){
            super(getNetworkId());
            addListeners();
            getData();

            m_menuBtn = menuBtn;
            setPrefWidth(App.DEFAULT_STATIC_WIDTH);
            setMaxWidth(App.DEFAULT_STATIC_WIDTH);

            double defaultGridWidth = App.DEFAULT_STATIC_WIDTH - 30;
            double defaultGridHeight = heightObject.get() - 100;      

            prefHeightProperty().bind(heightObject);

            
            SimpleDoubleProperty gridWidth = new SimpleDoubleProperty(defaultGridWidth);
            SimpleDoubleProperty gridHeight = new SimpleDoubleProperty(defaultGridHeight);
            SimpleObjectProperty<HBox> currentBox = new SimpleObjectProperty<>(null);


            m_spectrumData = new SpectrumDataList(FriendlyId.createFriendlyId(), SpectrumFinance.this, gridWidth,gridHeight, currentBox, m_itemTimeSpan, m_networkInterface);


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

            
            MenuButton timeSpanBtn = new MenuButton(m_itemTimeSpan.getName());
            timeSpanBtn.setFont(App.txtFont);
            timeSpanBtn.setContentDisplay(ContentDisplay.LEFT);
            timeSpanBtn.setAlignment(Pos.CENTER_LEFT);

            
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
                timeSpanBtn.setText(newval.getName());
                save();
            });

            HBox menuBar = new HBox(sortTypeButton,sortDirectionButton,swapTargetButton, menuBarRegion1, searchField, menuBarRegion,timeSpanBtn, rightSideMenu);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 2, 1, 5));



      

            VBox chartList = m_spectrumData.getGridBox();
  
            ScrollPane scrollPane = new ScrollPane(chartList);
            scrollPane.setPadding(new Insets(2));
            HBox headingsBox = new HBox();

 
            VBox bodyPaddingBox = new VBox(headingsBox, scrollPane);
            bodyPaddingBox.setPadding(new Insets(5,5,0,5));

     

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



            getChildren().addAll( bodyPaddingBox, footerVBox);

            HBox menuPaddingBox = new HBox(menuBar);
            menuPaddingBox.setPadding(new Insets(0, 0, 0, 0));

            bodyPaddingBox.prefWidthProperty().bind(widthProperty().subtract(1));
            scrollPane.prefViewportWidthProperty().bind(m_appStage.widthProperty());

           // Binding<Double> scrollWidth = Bindings.createObjectBinding(()->scrollPane.viewportBoundsProperty().get() != null ? (scrollPane.viewportBoundsProperty().get().getWidth() < 300 ? 300 : scrollPane.viewportBoundsProperty().get().getWidth() ) : 300, scrollPane.viewportBoundsProperty());
           
         
           
           scrollPane.viewportBoundsProperty().addListener((obs,oldval,newval)->{
              
            double width = newval.getWidth();
           
                gridWidth.set( width < 300 ? 300 : width );
       
            
                
            });

            Binding<Double> scrollHeight = Bindings.createObjectBinding(()->scrollPane.viewportBoundsProperty().get() != null ? scrollPane.viewportBoundsProperty().get().getHeight() : defaultGridHeight, scrollPane.viewportBoundsProperty());
            gridHeight.bind(scrollHeight);

        
            scrollPane.prefViewportHeightProperty().bind(m_appStage.heightProperty().subtract(footerVBox.heightProperty()));

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
        public boolean getCurrent() {
            return m_current.get();
        }

        @Override
        public String getName() {  
            return SpectrumFinance.this.getName();
        }

    



        @Override
        public void setCurrent(boolean value) {
            m_menuBtn.setId(value ? "activeMenuBtn" : "menuTabBtn");
            m_current.set(value);
            
        }

        @Override
        public void shutdown() {
            m_spectrumData.shutdown();
            m_spectrumData.removeUpdateListener();

            m_networkInterface.set(null);

            if(m_appStage != null){
                m_appStage.close();
                m_appStage = null;
            }

            
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
                                
                        }
                    
                        public void sendMessage(int code, long timestamp, String networkId, String msg){
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

            NoteInterface networkInterface = m_currentNetworkId != null && !m_currentNetworkId.equals(NetworksData.NO_NETWORK.getNetworkId())? getNetworksData().getNetwork(m_currentNetworkId) : null;
    
            m_networkInterface.set(networkInterface);
     
            if(update){
                save();
            }    
        }
    
        
        public void getNetworkMenu(MenuButton networkMenuBtn){
            networkMenuBtn.getItems().clear();

            String selectedMarketId = m_currentNetworkId;
        
            SimpleBooleanProperty found = new SimpleBooleanProperty(false);

            for(NetworkInformation networkInfo : SUPPORTED_NETWORKS ){
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




    private void updateMarketList(ArrayList<SpectrumMarketData> data){
        int size = data.size();
        long timeStamp = System.currentTimeMillis();

        if(m_marketsList.size() == 0){
            for(int i = 0; i < size; i++){
                SpectrumMarketData marketData = data.get(i);
               if(data != null){
                    m_marketsList.add(marketData);
                    data.clear();

                    sendMessage(App.STATUS, timeStamp,marketData.getPoolId(), App.LIST_CHANGED);
                    
               }
            }
           
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
                sendMessage(App.STATUS, timeStamp,NETWORK_ID, App.LIST_CHANGED);
            }else{
                sendMessage(App.STATUS, timeStamp,NETWORK_ID, App.LIST_UPDATED);
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
    public void getPoolStats(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
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
        try {
            Files.writeString(App.logFile.toPath(), "\nStopped", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e1) {

        }
        
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
                    
            ExecutorService executor = getNetworksData().getExecService();
            
            Runnable exec = ()->{
                //FreeMemory freeMem = Utils.getFreeMemory();
                
                getMarkets(success -> {


                    Object sourceObject = success.getSource().getValue();
                    if (sourceObject != null && sourceObject instanceof JsonArray) {
                        JsonArray marketJsonArray = (JsonArray) sourceObject;
                        /*try {
                            Files.writeString( new File(getDataDir().getAbsolutePath() + "/markets.json").toPath(), marketJsonArray.toString());
                        } catch (IOException e) {
                 
                        }*/
                        setConnectionStatus(App.STARTED);
                        sendMessage(App.STATUS, System.currentTimeMillis(), NETWORK_ID, App.STARTED);
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

    
  
    @Override
    public Object sendNote(JsonObject json){
    
        JsonElement subjectElement = json.get(App.CMD);

        JsonElement timeStampElement = json.get("timeStamp");
        

        String subject = subjectElement != null && subjectElement.isJsonPrimitive() ? subjectElement.getAsString() : null;
        long timeStamp = timeStampElement != null && timeStampElement.isJsonPrimitive() ? timeStampElement.getAsLong() : 0;
        
        switch(subject){
            case "getQuote":
            
                return getQuote(json, timeStamp);
            
                
        }
        return null;
    }


    private List<SpectrumMarketData> m_searchList ;
    private Optional<SpectrumMarketData> m_quoteOptional;

    private Object getQuote(JsonObject json, long timestamp){
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
                    m_searchList = m_marketsList.stream().filter(item -> item.getBaseSymbol().equals(base)).collect(Collectors.toList());
                break;
                case "id":
                    m_searchList = m_marketsList.stream().filter(item -> item.getBaseId().equals(base)).collect(Collectors.toList());
                    try {
                        Files.writeString(App.logFile.toPath(),"\nfound: " + m_searchList.size() , StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        
                    }
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
                    try {
                        Files.writeString(App.logFile.toPath(),"\n m_marketsList: " + m_marketsList.size() + " found: " + json.toString() + " "  , StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        
                    }
                    if(m_quoteOptional.isPresent()){
                        try {
                            Files.writeString(App.logFile.toPath(),"\n found: " + m_quoteOptional.get()  , StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e) {
                            
                        }
                        return m_quoteOptional.get();
                    }
                break;
            }
        }
        m_searchList = null;
        return null;
    }
    
  
    public void uninstall(){
        
        
         
    }

    @Override
    public void shutdown(){
        uninstall();
        super.shutdown();
    }
 

   
    
}
