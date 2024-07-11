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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.reactfx.util.FxTimer;

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
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SpectrumFinance extends Network implements NoteInterface {

    public static String DESCRIPTION = "Spectrum Finance is a cross-chain decentralized exchange (DEX).";
    public static String SUMMARY = "";
    public static String NAME = "Spectrum Finance";
    public final static String NETWORK_ID = "SPECTRUM_FINANCE";

    private static NetworkInformation[]  SUPPORTED_NETWORKS = new NetworkInformation[]{ErgoNetwork.getNetworkInformation()};

    private String m_currentNetworkId = ErgoNetwork.NETWORK_ID;
    private String m_tokensId = null;
  

    public static String API_URL = "https://api.spectrum.fi";

    private ListChangeListener<NoteInterface> m_networkListener = null;

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
    private TimeSpan m_itemTimeSpan= new TimeSpan("1day");

    private boolean m_isMax = false;
    private double m_prevHeight = -1;
    private double m_prevWidth = -1;
    private double m_prevX = -1;
    private double m_prevY = -1;

    private ChangeListener<NoteInterface> m_tokensChangeListener = null;
    private NoteMsgInterface m_tokenMsgInterface = null;
    private SimpleObjectProperty<NoteInterface> m_tokensInterface = new SimpleObjectProperty<>(null);

    private SimpleObjectProperty<NoteInterface> m_networkInterface = new SimpleObjectProperty<>(null);
    private ChangeListener<NoteInterface> m_networkChangeListener = null;
    private NoteMsgInterface m_networkMsgInterface;

    public String getType(){
        return App.APP_TYPE;
    }

  

    public SpectrumFinance(NetworksData networksData) {
        this(null, networksData);
        setup(null);

    }

    public SpectrumFinance(JsonObject jsonObject, NetworksData networksData) {
        super(new Image(getAppIconString()), NAME, NETWORK_ID, networksData);
      
        setup(jsonObject);
    }
    


    

    public ArrayList<SpectrumMarketData> marketsList(){
        return m_marketsList;
    }



    /*public SimpleObjectProperty<JsonObject> cmdObjectProperty() {

        return m_cmdObjectProperty;
    }*/

    public SimpleObjectProperty<NoteInterface> networkInterface() {

        return m_networkInterface;
    }

    public SimpleObjectProperty<NoteInterface> tokensInterface() {

        return m_tokensInterface;
    }
  

    public File getAppDir() {
        return m_appDir;
    }
    public static String getAppIconString(){
        return "/assets/spectrum-150.png";
    }


    public  Image getSmallAppIcon() {
        return new Image(getSmallAppIconString());
    }

    public static String getSmallAppIconString(){
        return "/assets/spectrumFinance.png";
    }


    private void setup(JsonObject jsonObject) {
       

        JsonElement currentNetworkElement = jsonObject != null ? jsonObject.get("currentNetworkId") : null;
        JsonElement tokensIdElement = jsonObject != null ? jsonObject.get("tokensId") : null;
        JsonElement stageElement = jsonObject != null ? jsonObject.get("stage") : null; 
        m_currentNetworkId = currentNetworkElement != null && currentNetworkElement.isJsonPrimitive() ? currentNetworkElement.getAsString() : ErgoNetwork.NETWORK_ID;
        
        m_networkChangeListener = (obs,oldval,newval)->{
            if(oldval != null && m_networkMsgInterface != null){
               
                oldval.removeMsgListener(m_networkMsgInterface);
                m_networkMsgInterface = null;
            }
            if(newval != null){
                addNetworkInterface(newval);
            
            }
            
        };
        
        m_networkInterface.addListener(m_networkChangeListener);

        

        m_tokensId = tokensIdElement != null && tokensIdElement.isJsonPrimitive() ? tokensIdElement.getAsString() : ErgoTokens.NETWORK_ID;

        
        m_tokensChangeListener = (obs,oldval,newval)->{
            if(oldval != null && m_tokenMsgInterface != null){
               
                oldval.removeMsgListener(m_tokenMsgInterface);
                m_tokenMsgInterface = null;
            }
            if(newval != null){
                addTokenInterface(newval);
            
            }
            
        };
        
        m_tokensInterface.addListener(m_tokensChangeListener);

      
        
        FxTimer.runLater(Duration.ofMillis(200), ()->{
            updateNetworks();
            
        });

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
 
    protected void updateNetworks(){
    
        NoteInterface network = getNetworksData().getNoteInterface(m_currentNetworkId);
        if(network != null){
            if(networkInterface().get() != null && networkInterface().get().getNetworkId().equals(network.getNetworkId())){
                if(m_tokensInterface.get() == null){
                    JsonObject networkCmd = Utils.getCmdObject("getNetwork");
                    networkCmd.addProperty("networkType", "Tokens");

                    Object obj = network != null ? network.sendNote(networkCmd) : null;
                    m_tokensInterface.set(obj != null && obj instanceof NoteInterface ? (NoteInterface) obj : null );

                }
            }else{
               
                m_networkInterface.set(network);
                
                JsonObject networkCmd = Utils.getCmdObject("getNetwork");
                networkCmd.addProperty("networkType", "Tokens");

                Object obj = network != null ? network.sendNote(networkCmd) : null;
                m_tokensInterface.set(obj != null && obj instanceof NoteInterface ? (NoteInterface) obj : null );
                
            }

        }else{
            if(m_networkInterface != null){
                m_networkInterface.set(null);
            }
            if(m_tokensInterface != null){
                m_tokensInterface.set(null);
            }
        }
    }

    @Override
    public void addMsgListener(NoteMsgInterface item){
        try {
            Files.writeString(App.logFile.toPath(), "\nadd item: " + item.getId() + " " + getConnectionStatus(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
 
        }
        super.addMsgListener(item);
    }

    @Override
    public boolean removeMsgListener(NoteMsgInterface item){
        

        boolean removed = super.removeMsgListener(item);
        try {
            Files.writeString(App.logFile.toPath(), "\nremove item: " + removed + " " + item.getId() + " " + getConnectionStatus(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
 
        }
        return removed;
    }
    

    private void addTokenInterface(NoteInterface tokens){
       
        String tokensInterfaceId = FriendlyId.createFriendlyId();
        
        m_tokenMsgInterface = new NoteMsgInterface(){

           public String getId() {
               return tokensInterfaceId;
           }
           
           public void sendMessage(String networkId, int code, long timestamp){

           }

           public void sendMessage(String networkId, int code, long timestamp, JsonObject json){
           }

           public void sendMessage(int msg, long timestamp){
               switch(msg){
                   case App.STARTED:
                   
                   break;
                   case App.STOPPED:
                   
                   break;
                   case App.LIST_CHANGED:
                   case App.LIST_UPDATED:
                   
                   break;
               }
              

           }
           public void sendMessage(String networkId, int code, long timestamp, String msg){
               
           }
           public void sendMessage(int code, long timestamp, String msg){
               
           }
       };
       tokens.addMsgListener(m_tokenMsgInterface);
   }

   private void addNetworkInterface(NoteInterface network){
       
    String networkInterfaceId = FriendlyId.createFriendlyId();
    
    m_tokenMsgInterface = new NoteMsgInterface(){

       public String getId() {
           return networkInterfaceId;
       }
       
       public void sendMessage(String networkId, int code, long timestamp){

       }
       public void sendMessage(String networkId, int code, long timestamp, JsonObject json){
       }
       public void sendMessage(int msg, long timestamp){
           switch(msg){
               case App.STARTED:
               
               break;
               case App.STOPPED:
               
               break;
               case App.LIST_CHANGED:
               case App.LIST_UPDATED:
               
               break;
           }
          

       }
       public void sendMessage(String networkId, int code, long timestamp, String msg){
           
       }
       public void sendMessage(int code, long timestamp, String msg){
           
       }
   };
   
   network.addMsgListener(m_tokenMsgInterface);
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
        return json;
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


    public void setCurrentNetworkId(String networkId,boolean update){
        m_currentNetworkId = networkId;
        if(update){
        getLastUpdated().set(LocalDateTime.now());
        }    
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

    public void getNetworkMenu(MenuButton networkMenuBtn){
        networkMenuBtn.getItems().clear();

        String selectedMarketId = m_currentNetworkId;
      
        SimpleBooleanProperty found = new SimpleBooleanProperty(false);

        for(NetworkInformation networkInfo : SUPPORTED_NETWORKS ){
            NoteInterface noteInterface = getNetworksData().getNoteInterface(networkInfo.getNetworkId());
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
                updateNetworks();

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


    private void showAppStage() {
        if (m_appStage == null) {
            double appStageWidth = 670;
            double appStageHeight = 600;

            double defaultGridWidth = appStageWidth - 30;
            double defaultGridHeight = appStageHeight - 100;      

            SimpleObjectProperty<TimeSpan> itemTimeSpanObject = new SimpleObjectProperty<TimeSpan>(m_itemTimeSpan);
            SimpleDoubleProperty gridWidth = new SimpleDoubleProperty(defaultGridWidth);
            SimpleDoubleProperty gridHeight = new SimpleDoubleProperty(defaultGridHeight);
            SimpleObjectProperty<HBox> currentBox = new SimpleObjectProperty<>(null);
            m_networkListener = (ListChangeListener.Change<? extends NoteInterface> c)->{
                updateNetworks();
            };
            getNetworksData().addNetworkListener(m_networkListener);
            updateNetworks();

            SpectrumDataList spectrumData = new SpectrumDataList(FriendlyId.createFriendlyId(), this, gridWidth,gridHeight, itemTimeSpanObject, currentBox);

            
        
            


            m_appStage = new Stage();
            m_appStage.getIcons().add(getSmallAppIcon());
            m_appStage.initStyle(StageStyle.UNDECORATED);
            m_appStage.setTitle(NAME);

            Button closeBtn = new Button();


            Button maxBtn = new Button();
            Button fillLeftBtn = new Button();

            HBox titleBox = App.createTopBar(getSmallAppIcon(),fillLeftBtn, maxBtn, closeBtn, m_appStage);
            ImageView fillLeftImgView = new ImageView(new Image("/assets/fillLeft.png"));
            fillLeftImgView.setPreserveRatio(true);
            fillLeftImgView.setFitHeight(19);
            fillLeftBtn.setGraphic(fillLeftImgView);
            

            titleBox.setPadding(new Insets(7, 8, 5, 10));

            m_appStage.titleProperty().bind(Bindings.concat(NAME, " - ", spectrumData.statusMsgProperty()));

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
                    networkMenuBtnImageView.setImage(newval.getSmallAppIcon());
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



      

            VBox chartList = spectrumData.getGridBox();
  
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

            Binding<String> errorTxtBinding = Bindings.createObjectBinding(()->(spectrumData.statusMsgProperty().get().startsWith("Error") ? spectrumData.statusMsgProperty().get() : "") ,spectrumData.statusMsgProperty());

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

            VBox headerVBox = new VBox(titleBox);
            headerVBox.setPadding(new Insets(0, 5, 0, 5));
            headerVBox.setAlignment(Pos.TOP_CENTER);

            VBox layoutBox = new VBox(headerVBox, bodyPaddingBox, footerVBox);

            HBox menuPaddingBox = new HBox(menuBar);
            menuPaddingBox.setPadding(new Insets(0, 0, 0, 0));

            spectrumData.statusMsgProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && (newVal.startsWith("Top"))) {
                    if (!headerVBox.getChildren().contains(menuPaddingBox)) {
                        headerVBox.getChildren().add(1, menuPaddingBox);

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

           // Binding<Double> scrollWidth = Bindings.createObjectBinding(()->scrollPane.viewportBoundsProperty().get() != null ? (scrollPane.viewportBoundsProperty().get().getWidth() < 300 ? 300 : scrollPane.viewportBoundsProperty().get().getWidth() ) : 300, scrollPane.viewportBoundsProperty());
           
         
           
           scrollPane.viewportBoundsProperty().addListener((obs,oldval,newval)->{
              
            double width = newval.getWidth();
           
                gridWidth.set( width < 300 ? 300 : width );
       
            
                
            });

            Binding<Double> scrollHeight = Bindings.createObjectBinding(()->scrollPane.viewportBoundsProperty().get() != null ? scrollPane.viewportBoundsProperty().get().getHeight() : defaultGridHeight, scrollPane.viewportBoundsProperty());
            gridHeight.bind(scrollHeight);

        
            scrollPane.prefViewportHeightProperty().bind(m_appStage.heightProperty().subtract(headerVBox.heightProperty()).subtract(footerVBox.heightProperty()));

            chartList.prefWidthProperty().bind(scrollPane.prefViewportWidthProperty().subtract(40));
       
            spectrumData.addUpdateListener((obs,oldval,newval)->{
                if(newval != null){
         
                    lastUpdatedField.setText(Utils.formatDateTimeString(newval));
                    
                }else{
                    lastUpdatedField.setText(spectrumData.statusMsgProperty().get());
                }
            });

            ResizeHelper.addResizeListener(m_appStage, 250, 300,Double.MAX_VALUE, Double.MAX_VALUE);
   
            Runnable runClose = () -> {

                spectrumData.shutdown();
               
                m_networkInterface.removeListener(networkChangeListener);

                spectrumData.removeUpdateListener();
                if(m_appStage != null){
                    m_appStage.close();
                    m_appStage = null;
                }

            };
            
            closeBtn.setOnAction(closeEvent -> {
                
                runClose.run();
            });

            maxBtn.setOnAction(e->{
                if(m_isMax){
                    
                    m_appStage.setWidth(m_prevWidth);
                    m_appStage.setHeight(m_prevHeight);
                    FxTimer.runLater(Duration.ofMillis(100), ()->{
                        m_appStage.setY(m_prevY);
                        m_appStage.setX(m_prevX);

                        m_prevX = -1;
                        m_prevY = -1;
                    });
                    m_prevHeight = -1;
                    m_prevWidth = -1;  
                    m_isMax = false;
                }else{
                    
                    m_isMax = true;
                    m_prevY = m_appStage.getY();
                    m_prevX = m_appStage.getX();
                    m_prevHeight = m_appStage.getScene().getHeight();
                    m_prevWidth = m_appStage.getScene().getWidth();
                    m_appStage.setMaximized(true);

                    FxTimer.runLater(Duration.ofMillis(100), ()->{
                        double height = m_appStage.getScene().getHeight();
                        double width = m_appStage.getScene().getWidth();
                        double x = m_appStage.getX();
                        double y = m_appStage.getY();
                        m_appStage.setMaximized(false);
                        FxTimer.runLater(Duration.ofMillis(100), ()->{
                            m_appStage.setX(x);
                            m_appStage.setY(y);
                            m_appStage.setHeight(height);
                            m_appStage.setWidth(width);
                        });
                    });
                
                }
                
                    
               
            });

           fillLeftBtn.setOnAction(e -> {
                if(m_isMax){
                    
                    
                    m_appStage.setHeight(m_prevHeight);
                    m_appStage.setWidth(m_prevWidth);
                    
                    FxTimer.runLater(Duration.ofMillis(100), ()->{
                        m_appStage.setY(m_prevY);
                        m_appStage.setX(m_prevX);

                        m_prevX = -1;
                        m_prevY = -1;
                    });
                    m_prevHeight = -1;  
                    m_prevWidth = -1;
                    m_isMax = false;
                }else{
                    m_isMax = true;
                    m_prevY = m_appStage.getY();
                    m_prevX = m_appStage.getX();
                    m_prevHeight = m_appStage.getScene().getHeight();
                    m_prevWidth = m_appStage.getScene().getWidth();
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
            sendMessage(App.LIST_CHANGED);
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
                sendMessage(App.LIST_CHANGED);
            }else{
                sendMessage(App.LIST_UPDATED);
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
                        getMarketUpdate(marketJsonArray);
                    } 
                }, (onfailed)->{
                    
                    setConnectionStatus(App.ERROR);
                    Throwable throwable = onfailed.getSource().getException();
                    String msg= throwable instanceof java.net.SocketException ? "Connection unavailable" : (throwable instanceof java.net.UnknownHostException ? "Unknown host: Spectrum Finance unreachable" : throwable.toString());
                  
                    sendMessage(App.ERROR, System.currentTimeMillis(), msg);
                });
                

                
            };

            Runnable submitExec = ()->executor.submit(exec);

            m_scheduledFuture = m_schedualedExecutor.scheduleAtFixedRate(submitExec, 0, 7000, TimeUnit.MILLISECONDS);


       
           
        }
    }

    
  
    @Override
    public Object sendNote(JsonObject json){
    
        JsonElement subjectElement = json.get("subject");

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
        if(m_networkListener != null){
            getNetworksData().removeNetworkListener(m_networkListener);
            m_networkListener = null;
        }
        
        m_tokensInterface.set(null);
        m_networkInterface.set(null);
        if(m_tokensInterface != null){
            m_tokensInterface.removeListener(m_tokensChangeListener);
            m_tokensInterface = null;
        }        
        
        if(m_networkInterface != null){
            m_networkInterface.removeListener(m_networkChangeListener);
            m_networkInterface = null;
        }
        while(msgListeners().size() > 0){
            sendMessage(App.SHUTDOWN);
            removeMsgListener(msgListeners().get(0));
        }     
    }

    @Override
    public void shutdown(){
        uninstall();
        super.shutdown();
    }
 

   
    
}
