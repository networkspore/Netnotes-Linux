package com.netnotes;

import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.reactfx.util.FxTimer;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.netnotes.IconButton.IconStyle;
import com.utils.Utils;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class NetworksData extends Network implements NoteInterface {

    public final static long WATCH_INTERVAL = 50;
    public final static String INPUT_EXT = ".in";
    public final static String OUT_EXT = ".out";
    public final static long DEFAULT_CYCLE_PERIOD = 7;
    public final static String NETWORK_ID = "NetworksData";



    public final static NetworkInformation[] SUPPORTED_APPS = new NetworkInformation[]{
   
       // KucoinExchange.getNetworkInformation(),
        SpectrumFinance.getNetworkInformation()
        
    };

    
    public final static NetworkInformation NO_NETWORK = new NetworkInformation("NO_NETWORK", "(none)","/assets/globe-outline-white-120.png", "/assets/globe-outline-white-30.png", "No network selected" );
    
    public static NetworkInformation[] SUPPORTED_NETWORKS = new NetworkInformation[]{ 
        ErgoNetwork.getNetworkInformation()
    };
    
    private SimpleStringProperty m_currentNetworkId = new SimpleStringProperty(null);
    
    private Tooltip m_networkToolTip = new Tooltip("Network");

    private HashMap<String, String>  m_locationsIds = new HashMap<>();

    private ExecutorService m_execService = Executors.newFixedThreadPool(6);
    
    private HashMap<String, NoteInterface> m_apps = new HashMap<>();
    private HashMap<String, NoteInterface> m_networks = new HashMap<>();

    private Stage m_addNetworkStage = null;

    private File m_notesDir;
    private File m_outDir;
    private File m_inDir;

    private NoteWatcher m_noteWatcher = null;

    private SimpleStringProperty m_stageIconStyle = new SimpleStringProperty(IconStyle.ICON);

    private double m_stageWidth = 700;
    private double m_stageHeight = 600;
    private double m_stagePrevWidth = 310;
    private double m_stagePrevHeight = 500;
    private boolean m_stageMaximized = false;
    private AppData m_appData;

    private Stage m_appStage = null;
    private ChangeListener<Bounds> m_boundsListener = null;
    private ScrollPane m_menuScroll;
    private ScrollPane m_subMenuScroll;
    private VBox m_subMenuBox = new VBox();

    private SimpleObjectProperty<TabInterface> m_currentMenuTab = new SimpleObjectProperty<TabInterface>();
    private BufferedButton m_settingsBtn;
    private Button m_appsBtn = new Button();
    
    private SettingsTab m_settingsTab = null;
    private NetworkTab m_networkTab = null;
    private AppsTab m_appsTab = null;

    private Label m_tabLabel = new Label("");
    

    private SimpleDoubleProperty m_widthObject = new SimpleDoubleProperty(App.DEFAULT_STATIC_WIDTH);
    private SimpleDoubleProperty m_heightObject = new SimpleDoubleProperty(200);

    private String m_configId;

    private String m_localId;

    public NetworksData(AppData appData) {
        super(App.globeImage30,"Networks",NETWORK_ID,(NetworksData) null);
        m_appData = appData;
        m_configId = FriendlyId.createFriendlyId();
        m_localId = FriendlyId.createFriendlyId();

        m_locationsIds.put(m_localId, App.LOCAL);

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        try( 
            InputStream stream = App.class.getResource("/assets/OCRAEXT.TTF").openStream(); 
        ) {
            
           
            java.awt.Font ocrFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, stream).deriveFont(48f);
            ge.registerFont(ocrFont);
           

        } catch (FontFormatException | IOException e) {
           
            try {
                Files.writeString(App.logFile.toPath(), "\nError registering font: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {
            
            }
            
        } 

        
        m_notesDir = new File(m_appData.getAppDir().getAbsolutePath() + "/notes");
        m_outDir = new File(m_notesDir + "/out");
        m_inDir = new File(m_notesDir + "/in");

        if (!m_notesDir.isDirectory()) {
            try {
                Files.createDirectory(m_notesDir.toPath());
            } catch (IOException e) {

            }
        }
        if (!m_outDir.isDirectory()) {
            try {
                Files.createDirectory(m_outDir.toPath());
            } catch (IOException e) {

            }
        }
        if(!m_inDir.isDirectory()){
            try {
                Files.createDirectory(m_inDir.toPath());
            } catch (IOException e) {

            }
        }

        /* try {
            m_noteWatcher = new NoteWatcher(m_notesDir, new NoteListener() {
                public void onNoteChange(String fileString) {
                    checkFile(new File(fileString));
                }
            });
        } catch (IOException e) {

        }*/

       
        openJson(getData("data",  ".", "main","root"));
       
        m_appData.appKeyProperty().addListener((obs,oldval,newval)->updateIdDataFile(oldval, newval));
        


    }

    private boolean isConfigId(String configId){
        if(configId != null && configId.equals(m_configId)){
            return true;
        }
        return false;
    }

    public static boolean isAppSupported(String networkId){
        if(networkId != null){
            for(int i =0; i < SUPPORTED_APPS.length ; i++){
                if(SUPPORTED_APPS[i].getNetworkId().equals(networkId)){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isNetworkSupported(String networkId){
        if(networkId != null){
            for(int i =0; i < SUPPORTED_NETWORKS.length ; i++){
                if(SUPPORTED_NETWORKS[i].getNetworkId().equals(networkId)){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isValidLocation(String locationString){
        return (locationString != null && locationString.equals((App.LOCAL)));
    }
    

    @Override
    public Object sendNote(JsonObject note){
        if(note != null){

            JsonElement cmdElement = note.get(App.CMD);
            JsonElement configIdElement = note.get("configId");
            JsonElement locationIdElement = note.get("locationId");

            String configId = configIdElement != null && configIdElement.isJsonPrimitive() ? configIdElement.getAsString() : null;
            String locationId = locationIdElement != null && locationIdElement.isJsonPrimitive() ? locationIdElement.getAsString() : null;
            String locationString =  getLocationString(locationId);

            if(cmdElement != null && cmdElement.isJsonPrimitive() && configId != null && isConfigId(configId) && isValidLocation(locationString)){
                String cmd = cmdElement.getAsString();

                switch(cmd){
                    case "removeNetwork":
                        return removeNetwork(note);
                        
                }
            }
        }

        return false;
    }



    public ExecutorService getExecService(){
        return m_execService;
    }

    
    public Image getCharacterImage(String characterString){
        return null;
    }







    private AppData getAppData() {
        return m_appData;
    }

    public boolean verifyAppPassword(String password){
        BCrypt.Result result = BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).verify(password.toCharArray(), getAppData().getAppKeyBytes());
        return result.verified;
    }

    private void openJson(JsonObject networksObject) {
        if (networksObject != null) {

            JsonElement jsonArrayElement = networksObject == null ? null : networksObject.get("apps");

            if(jsonArrayElement == null){
                addAllApps(true);
            }

            JsonElement jsonNetArrayElement = networksObject == null ? null : networksObject.get("netArray");
            
            JsonElement stageElement = networksObject.get("stage");
            JsonElement currentNetworkIdElement = networksObject.get("currentNetworkId");

          
            JsonArray jsonArray = jsonNetArrayElement != null && jsonNetArrayElement.isJsonArray() ? jsonNetArrayElement.getAsJsonArray() : new JsonArray();
            
        
            for (JsonElement element : jsonArray) {
                JsonObject jsonObject = element.getAsJsonObject();
                JsonElement networkIdElement = jsonObject.get("networkId");

                if (networkIdElement != null) {
                    String networkId = networkIdElement.getAsString();

                    NoteInterface noteInterface = createNetwork(networkId);
                    if(noteInterface != null){
                        addNetwork(noteInterface, false);
                    }
                }

            }
            String currentNetworkString = currentNetworkIdElement != null && currentNetworkIdElement.isJsonPrimitive() ? currentNetworkIdElement.getAsString() : null; 
            if(currentNetworkString != null && getNetwork(currentNetworkString) != null){
                
                m_currentNetworkId.set(currentNetworkString); 
            }else{
                m_currentNetworkId.set(null);
            }
            
        


            jsonArray = jsonArrayElement != null && jsonArrayElement.isJsonArray() ? jsonArrayElement.getAsJsonArray() : new JsonArray();

          
            for (JsonElement element : jsonArray) {
                JsonObject jsonObject = element.getAsJsonObject();
                JsonElement networkIdElement = jsonObject.get("networkId");

                if (networkIdElement != null) {
                    String networkId = networkIdElement.getAsString();

                    NoteInterface noteInterface = createApp(networkId);
                    if(noteInterface != null){
                        addApp(noteInterface, false);
                    }

                }

            }
            
            if (stageElement != null && stageElement.isJsonObject()) {

                JsonObject stageObject = stageElement.getAsJsonObject();
                JsonElement stagePrevXElement = stageObject.get("prevX");
                JsonElement stagePrevYElement = stageObject.get("prevY");

                JsonElement stageWidthElement = stageObject.get("width");
                JsonElement stageHeightElement = stageObject.get("height");
                JsonElement stagePrevWidthElement = stageObject.get("prevWidth");
                JsonElement stagePrevHeightElement = stageObject.get("prevHeight");

                JsonElement iconStyleElement = stageObject.get("iconStyle");
                JsonElement stageMaximizedElement = stageObject.get("maximized");

                boolean maximized = stageMaximizedElement == null ? false : stageMaximizedElement.getAsBoolean();
                String iconStyle = iconStyleElement != null ? iconStyleElement.getAsString() : IconStyle.ICON;
                m_prevX = stagePrevXElement != null && stagePrevXElement.isJsonPrimitive() ? stagePrevXElement.getAsDouble() : -1;
                m_prevY = stagePrevYElement != null && stagePrevYElement.isJsonPrimitive() ? stagePrevYElement.getAsDouble() : -1;

                m_stageIconStyle.set(iconStyle);
                setStagePrevWidth(Network.DEFAULT_STAGE_WIDTH);
                setStagePrevHeight(Network.DEFAULT_STAGE_HEIGHT);
                if (!maximized) {

                    setStageWidth(stageWidthElement.getAsDouble());
                    setStageHeight(stageHeightElement.getAsDouble());
                } else {
                    double prevWidth = stagePrevWidthElement != null && stagePrevWidthElement.isJsonPrimitive() ? stagePrevWidthElement.getAsDouble() : Network.DEFAULT_STAGE_WIDTH;
                    double prevHeight = stagePrevHeightElement != null && stagePrevHeightElement.isJsonPrimitive() ? stagePrevHeightElement.getAsDouble() : Network.DEFAULT_STAGE_HEIGHT;
                    setStageWidth(prevWidth);
                    setStageHeight(prevHeight);
                    setStagePrevWidth(prevWidth);
                    setStagePrevHeight(prevHeight);
                }
                setStageMaximized(maximized);
            }
          

        }else{
            
            addNetwork(createNetwork(ErgoNetwork.NETWORK_ID), false);
            m_currentNetworkId.set(ErgoNetwork.NETWORK_ID);

            addApp(createApp(SpectrumFinance.NETWORK_ID), true);
           
        }
    }

    private NoteInterface createApp(String networkId){
        if(getApp(networkId) == null){
            switch (networkId) {
                        
                /*case KucoinExchange.NETWORK_ID:
                    return new KucoinExchange(this), false);
                 */
                case SpectrumFinance.NETWORK_ID:
                    return new SpectrumFinance(this);

                
            }
        }
        return null;
    }

    private NoteInterface createNetwork(String networkId){
        
        if(getNetwork(networkId) == null){
            switch (networkId) {
                        
                case ErgoNetwork.NETWORK_ID:
                    return new ErgoNetwork(this);                         

            }
        }
        return null;
    }


    public double getStageWidth() {
        return m_stageWidth;
    }

    public void setStageWidth(double width) {
        m_stageWidth = width;

    }

    public void setStageHeight(double height) {
        m_stageHeight = height;
    }

    public double getStageHeight() {
        return m_stageHeight;
    }

    public SimpleStringProperty iconStyleProperty() {
        return m_stageIconStyle;
    }

    public boolean getStageMaximized() {
        return m_stageMaximized;
    }

    public void setStageMaximized(boolean value) {
        m_stageMaximized = value;
    }

    public double getStagePrevWidth() {
        return m_stagePrevWidth;
    }

    public void setStagePrevWidth(double width) {
        m_stagePrevWidth = width;

    }

    public void setStagePrevHeight(double height) {
        m_stagePrevHeight = height;
    }

    public double getStagePrevHeight() {
        return m_stagePrevHeight;
    }

    public JsonObject getStageJson() {
        JsonObject json = new JsonObject();
        json.addProperty("prevX", m_prevX);
        json.addProperty("prevY", m_prevY);
        json.addProperty("maximized", getStageMaximized());
        json.addProperty("width", getStageWidth());
        json.addProperty("height", getStageHeight());
        json.addProperty("prevWidth", getStagePrevWidth());
        json.addProperty("prevHeight", getStagePrevHeight());
        json.addProperty("iconStyle", m_stageIconStyle.get());
        return json;
    }


    public File getAppDir() {
        return m_appData.getAppDir();
    }

    public void openHostUrl(String url) {
        
    }



    private boolean addApp(NoteInterface noteInterface, boolean isSave) {
        // int i = 0;

        String networkId = noteInterface.getNetworkId();

        if (getApp(networkId) == null) {
            m_apps.put(networkId, noteInterface);
            
            if(isSave){

                long timestamp = System.currentTimeMillis();
                JsonObject resultJson = new JsonObject();
                resultJson.addProperty("code", App.LIST_ITEM_ADDED);
                resultJson.addProperty("type", App.APP_TYPE);
                resultJson.addProperty("networkId", getNetworkId());
                resultJson.addProperty("timeStamp", timestamp);
                resultJson.addProperty("id", networkId);
                
            
                sendMessage( App.LIST_ITEM_ADDED, timestamp, App.APP_TYPE, resultJson.toString());

            }
            return true;
        }
        return false;
    }

    private boolean addNetwork(NoteInterface noteInterface, boolean isSave) {
        // int i = 0;

        String networkId = noteInterface.getNetworkId();

        if (getNetwork(networkId) == null) {
               
        
            m_networks.put(noteInterface.getNetworkId(), noteInterface);
           
            if(isSave){
                long timestamp = System.currentTimeMillis();
                JsonObject resultJson = new JsonObject();
                resultJson.addProperty("code", App.LIST_ITEM_ADDED);
                resultJson.addProperty("type", App.NETWORK_TYPE);
                resultJson.addProperty("networkId", getNetworkId());
                resultJson.addProperty("timeStamp", timestamp);
                resultJson.addProperty("id", networkId);
                
            
                sendMessage( App.LIST_ITEM_ADDED, timestamp, App.NETWORK_TYPE, resultJson.toString());


                save();
            }
         

            return true;
        }
        return false;
    }

   
    
    public String getLocationString(String locationId){
        String locationString = m_locationsIds.get(locationId);
        return  locationString != null ?  locationString : "Unknown";
    }


    private boolean removeNetwork(String networkId, boolean isSave){       
       
        if(networkId != null) {
            
            NoteInterface noteInterface = m_networks.remove(networkId);
            
            if (noteInterface != null) {
               
                if(m_currentNetworkId.get() != null && m_currentNetworkId.get().equals(networkId)){
                    m_currentNetworkId.set(null);
                }
                

                noteInterface.shutdown();

                if(m_currentMenuTab.get() != null && m_currentMenuTab.get().getTabId().equals(networkId)){
                    open(NetworkTab.NAME, App.STATIC_TYPE, m_localId);
                }
                
                if(isSave){
                    long timestamp = System.currentTimeMillis();
                    JsonObject resultJson = new JsonObject();
                    resultJson.addProperty("code", App.LIST_ITEM_REMOVED);
                    resultJson.addProperty("type", App.NETWORK_TYPE);
                    resultJson.addProperty("timeStamp", timestamp);
                    resultJson.addProperty("id", networkId);
                    
                
                    sendMessage( App.LIST_ITEM_REMOVED, timestamp,getNetworkId(),  resultJson.toString());

                    save();
                }
                return true;
            }
        }
     
        return false;
        
    }

    private boolean removeNetwork(JsonObject note){
        JsonElement networksArrayElement = note.get("ids");
        long timeStamp = System.currentTimeMillis();
        

        if(networksArrayElement != null && networksArrayElement.isJsonArray()){
            JsonArray networksArray = networksArrayElement.getAsJsonArray();

             JsonArray namesArray = new JsonArray();

            for(JsonElement element : networksArray){
                JsonObject idObj = element.getAsJsonObject();
                JsonElement idElement = idObj.get("id");
                String id = idElement.getAsString();
                NoteInterface noteInterface = getNetwork(id);
                if(noteInterface != null){
                    String name = noteInterface.getName();
                    JsonObject networkJson = new JsonObject();
                    networkJson.addProperty("name", name);
                    namesArray.add(networkJson);
                }
            }

            JsonArray jsonArray = new JsonArray();

            for(JsonElement element : namesArray){
                JsonObject idObj = element.getAsJsonObject();
                JsonElement idElement = idObj.get("id");
                
                if(removeNetwork(idElement.getAsString(), false)){
                    jsonArray.add(idObj);
                }
            }
            save();

            JsonObject resultJson = new JsonObject();
            resultJson.addProperty("code", App.LIST_ITEM_REMOVED);
            resultJson.addProperty("networkID", App.NETWORK_TYPE);
            resultJson.addProperty("timeStamp", timeStamp);
            resultJson.add("ids", namesArray);
            
        
            sendMessage( App.LIST_ITEM_REMOVED, timeStamp,getNetworkId(), resultJson.toString());
     
            return true;
         
        }
        return false;
    }


  
    private SimpleObjectProperty<LocalDateTime> m_shutdownNow = new SimpleObjectProperty<>(null);

    public SimpleObjectProperty<LocalDateTime> shutdownNowProperty() {
        return m_shutdownNow;
    }

    public void shutdown() {
        m_shutdownNow.set(LocalDateTime.now());

        removeAllApps(false);

        removeAllNetworks(false);

        if (m_noteWatcher != null) {
            m_noteWatcher.shutdown();
        }
        closeNetworksStage();
    }


    /* 
    public void showManageAppsStage() {

        if (m_addNetworkStage == null) {
            
            m_installedVBox = new VBox();
            m_installedVBox.prefWidth(m_leftColumnWidth);
            m_installedVBox.setId("bodyBox");
            VBox.setVgrow(m_installedVBox, Priority.ALWAYS);

            m_notInstalledVBox = new VBox();
            m_notInstalledVBox.setId("bodyRight");
            VBox.setVgrow(m_notInstalledVBox, Priority.ALWAYS);
            HBox.setHgrow(m_notInstalledVBox, Priority.ALWAYS);


            String topTitle = "Netnotes - Manage Apps";
            m_addNetworkStage = new Stage();
            m_addNetworkStage.setTitle(topTitle);
            m_addNetworkStage.getIcons().add(App.logo);
            m_addNetworkStage.setResizable(false);
            m_addNetworkStage.initStyle(StageStyle.UNDECORATED);

            Button closeBtn = new Button();
            closeBtn.setOnAction(e -> {
                closeNetworksStage();
            });

            HBox titleBox = App.createTopBar(App.icon, topTitle, closeBtn, m_addNetworkStage);

       
            Text headingText = new Text("Manage");
            headingText.setFont(App.txtFont);
            headingText.setFill(Color.WHITE);

            HBox headingBox = new HBox(headingText);
            headingBox.prefHeight(35);
            headingBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headingBox, Priority.ALWAYS);
            headingBox.setPadding(new Insets(10, 10, 10, 10));
            headingBox.setId("headingBox");

            HBox headingPaddingBox = new HBox(headingBox);

            headingPaddingBox.setPadding(new Insets(5, 0, 2, 0));

            VBox headerBox = new VBox(headingPaddingBox);

            headerBox.setPadding(new Insets(0, 5, 0, 5));



            Button installBtn = new Button("Install");
            installBtn.setFont(App.txtFont);

            Button installAllBtn = new Button("All");
            installAllBtn.setMinWidth(60);
            installAllBtn.setFont(App.txtFont);

            Button removeBtn = new Button("Remove");
            removeBtn.setPrefWidth(m_leftColumnWidth);
            removeBtn.setId("menuBarBtn");
            
            Button removeAllBtn = new Button("All");
            removeAllBtn.setId("menuBarBtn");
            removeAllBtn.setMinWidth(60);

            Region vSpacerOne = new Region();
            VBox.setVgrow(vSpacerOne, Priority.ALWAYS);
          


            Region leftSpacer = new Region();
            HBox.setHgrow(leftSpacer, Priority.ALWAYS);

            Region topSpacer = new Region();
            VBox.setVgrow(topSpacer, Priority.ALWAYS);

            Region installAllSpacer = new Region();
            installAllSpacer.setMinWidth(5);

            HBox addBox = new HBox(leftSpacer, installBtn, installAllSpacer, installAllBtn);
            addBox.setPadding(new Insets(15,2, 15,15));
            HBox.setHgrow(m_notInstalledVBox, Priority.ALWAYS);
        
         
            


            HBox rmvBtnBox = new HBox(removeBtn, removeAllBtn);
           

            VBox leftSide = new VBox(m_installedVBox, rmvBtnBox);
            leftSide.setPadding(new Insets(5,5,5,5));
            leftSide.prefWidth(m_leftColumnWidth);
            VBox.setVgrow(leftSide, Priority.ALWAYS);


           VBox rightSide = new VBox(m_notInstalledVBox, addBox);
            HBox.setHgrow(rightSide, Priority.ALWAYS);
           // rightSide.setId("bodyRight");
            rightSide.setPadding(new Insets(5,5,0,5));


            HBox columnsHBox = new HBox(leftSide, rightSide);
            VBox.setVgrow(columnsHBox, Priority.ALWAYS);
            columnsHBox.setId("bodyBox");
            columnsHBox.setPadding(new Insets(10, 10, 10, 10));

            //rightSidePaddingBox.prefHeightProperty().bind(columnsHBox.heightProperty().subtract(addBox.heightProperty()).subtract(10));

            VBox bodyBox = new VBox(columnsHBox);
            bodyBox.setPadding(new Insets(0,2,2,2));
            VBox.setVgrow(bodyBox, Priority.ALWAYS);

            Region menuSpacer = new Region();
            HBox.setHgrow(menuSpacer, Priority.ALWAYS);

             Button okBtn = new Button("Ok");
            okBtn.setPadding(new Insets(5,25, 5, 25));

            HBox footerBar = new HBox(menuSpacer, okBtn);
            footerBar.setAlignment(Pos.CENTER_LEFT);
            footerBar.setPadding(new Insets(1, 0, 1, 0));
            HBox.setHgrow(footerBar, Priority.ALWAYS);

            VBox footerBox = new VBox(footerBar);
            footerBox.setPadding(new Insets(15));

            VBox fullBodyBox = new VBox(columnsHBox, footerBox);
            fullBodyBox.setPadding(new Insets(5));
            fullBodyBox.setId("bodyBox");
            HBox.setHgrow(fullBodyBox, Priority.ALWAYS);
            VBox.setVgrow(fullBodyBox,Priority.ALWAYS);

            VBox paddingBodyBox = new VBox(fullBodyBox);
            paddingBodyBox.setPadding(new Insets(0, 2,2,2));
            HBox.setHgrow(paddingBodyBox, Priority.ALWAYS);
            VBox.setVgrow(paddingBodyBox,Priority.ALWAYS);

            Region spacer = new Region();
            spacer.setMinHeight(2);
            
            VBox layoutVBox = new VBox(titleBox, headerBox, paddingBodyBox, spacer);

            Scene addNetworkScene = new Scene(layoutVBox, 600, 500);
            addNetworkScene.setFill(null);
            addNetworkScene.getStylesheets().add("/css/startWindow.css");
            m_addNetworkStage.setScene(addNetworkScene);

            addNetworkScene.focusOwnerProperty().addListener((e) -> {
                if (addNetworkScene.focusOwnerProperty().get() instanceof InstallableIcon) {
                    InstallableIcon installable = (InstallableIcon) addNetworkScene.focusOwnerProperty().get();

                    m_focusedInstallable = installable;
                } else {
                    if (addNetworkScene.focusOwnerProperty().get() instanceof Button) {
                        Button focusedButton = (Button) addNetworkScene.focusOwnerProperty().get();
                        String buttonString = focusedButton.getText();
                        if (!(buttonString.equals(installBtn.getText()) || buttonString.equals(removeBtn.getText()))) {

                            m_focusedInstallable = null;

                        }
                    }
                }

            });

            okBtn.setOnAction(e -> {
                closeBtn.fire();
            });

            installBtn.setOnAction(e -> {

                if (m_focusedInstallable != null && (!m_focusedInstallable.getInstalled())) {
                    installApp(m_focusedInstallable.getNetworkId(),true);
                }
                m_focusedInstallable = null;
            });

            removeBtn.setOnAction(e -> {
                if (m_focusedInstallable != null && (m_focusedInstallable.getInstalled())) {
                    removeApp(m_focusedInstallable.getNetworkId());
                }
                m_focusedInstallable = null;
            });

             removeAllBtn.setOnAction(e -> {
                removeAllApps(true);
            });

            installAllBtn.setOnAction(e -> {
                addAllApps(true);
            });

            m_addNetworkStage.show();

            FxTimer.runLater(Duration.ofMillis(20), ()->{
                    if(m_addNetworkStage != null){

                        Platform.runLater(()->m_addNetworkStage.toBack());
                        Platform.runLater(()->m_addNetworkStage.toFront());
                        Platform.runLater(()->m_addNetworkStage.requestFocus());

                    }
                    
                
            });
       
        } else {
             m_addNetworkStage.show();
        }
    }*/

    public void closeNetworksStage() {
        if (m_addNetworkStage != null) {
            m_addNetworkStage.close();
        }
        m_addNetworkStage = null;

        //m_focusedInstallable = null;
    }

    private double m_prevX = -1;
    private double m_prevY = -1;
  

    public double getPrevX(){
        return m_prevX;
    }

    public double getPrevY(){
        return m_prevY;
    }

    public void setPrevX(double value){
        m_prevX = value;
    }

    public void setPrevY(double value){
        m_prevY = value;
    }


    /*
    public void updateAvailableLists() {
        if (m_installables != null && m_installedVBox != null && m_notInstalledVBox != null) {
            m_installedVBox.getChildren().clear();

            m_notInstalledVBox.getChildren().clear();

            //  double listImageWidth = 30;
            //    double listImagePadding = 5;
  

        
            double cellWidth =  IconButton.NORMAL_IMAGE_WIDTH + (IconButton.NORMAL_PADDING * 2);
        //    double numCells = INTALLABLE_NETWORK_IDS.length - m_noteInterfaceList.size();
            double boxWidth = m_addNetworkStage.getWidth() - 150;

            int floor = (int) Math.floor(boxWidth / (cellWidth + 20));
            int numCol = floor == 0 ? 1 : floor;
        //    int numRows = numCells > 0 && numCol != 0 ? (int) Math.ceil(numCells / (double) numCol) : 1;

            //int numCol = floor == 0 ? 1 : floor;
            // currentNumCols.set(numCol);
         //   int numRows = numCells > 0 && numCol != 0 ? (int) Math.ceil(numCells / (double) numCol) : 1;

            ArrayList<HBox> rowsBoxes = new ArrayList<HBox>();

            ItemIterator grid = new ItemIterator();
            //j = row
            //i = col

            for (InstallableIcon installable : m_installables) {
                if(installable.getInstalled()){
                    installable.prefWidthProperty().bind(m_installedVBox.widthProperty());
                    m_installedVBox.getChildren().add(installable);
                }else{
                    if(rowsBoxes.size() < (grid.getJ() + 1)){
                        HBox newHBox = new HBox();
                        rowsBoxes.add(newHBox);
                        m_notInstalledVBox.getChildren().add(newHBox);
                      
                    }
    
                    HBox rowBox = rowsBoxes.get(grid.getJ());

                    installable.setPrefWidth(IconButton.NORMAL_IMAGE_WIDTH);

                    rowBox.getChildren().add(installable);
    
                    if (grid.getI() < numCol) {
                        grid.setI(grid.getI() + 1);
                    } else {
                        grid.setI(0);
                        grid.setJ(grid.getJ() + 1);
                    }
                }

            }
        }
    } */

    /*private void updateInstallables() {
        m_installables = new ArrayList<>();
        for (String networkId : INTALLABLE_NETWORK_IDS) {
            NoteInterface noteInterface = getApp(networkId);
            boolean installed = !(noteInterface == null);
            InstallableIcon installableIcon = new InstallableIcon(this, networkId, installed);

            m_installables.add(installableIcon);
        }
    }*/

   // private SimpleDoubleProperty m_gridWidth = new SimpleDoubleProperty(200);
    private NoteInterface getAppInterface(String networkId) {
        if (networkId != null) {
            return m_apps.get(networkId);
        }
        return null;
    }

public NoteInterface getApp(String networkId) {
    if (networkId != null) {

        NoteInterface noteInterface = getAppInterface(networkId);

        if (noteInterface != null && noteInterface instanceof Network) {
            Network network = (Network) noteInterface;
            return network.getNoteInterface();
        }
        
    }
    return null;
}


    private void installNetwork(String networkId){
        if(getNetwork(networkId) == null && isNetworkSupported(networkId)){
           
            addNetwork(createNetwork(networkId), true);
           
        }
    }

    private void installApp(String networkId, boolean save) {

        if(getApp(networkId) == null && isAppSupported(networkId)){
           
            addApp(createNetwork(networkId), true);
           
        }

    }


    private void addAllApps(boolean save) {
        for (NetworkInformation networkInfo : SUPPORTED_APPS) {
            if (getApp(networkInfo.getNetworkId()) == null) {
                installApp(getNetworkId(), false);
            }
        }
       // updateInstallables();
        if(save){
            save();
        }
    }

    private void removeAllApps(boolean isSave) {
        JsonArray result = new JsonArray();
        for (Map.Entry<String, NoteInterface> entry : m_apps.entrySet()) {
            
            NoteInterface noteInterface = entry.getValue();
            
            noteInterface.shutdown();
            if(isSave){
                result.add(noteInterface.getJsonObject());
            }
        }

        m_apps.clear();

        if(isSave){
            long timestamp = System.currentTimeMillis();
            JsonObject resultJson = new JsonObject();
            resultJson.addProperty("code", App.LIST_ITEM_REMOVED);
            resultJson.addProperty("type", App.APP_TYPE);
            resultJson.addProperty("networkId", NETWORK_ID);
            resultJson.addProperty("timeStamp", timestamp);
            resultJson.add("ids", result);

            sendMessage( App.LIST_ITEM_REMOVED, timestamp,NETWORK_ID, resultJson.toString());

            save();
        }
        
    }

    private void removeAllNetworks(boolean isSave) {
        JsonArray result = new JsonArray();
        for (Map.Entry<String, NoteInterface> entry : m_networks.entrySet()) {
            
            NoteInterface noteInterface = entry.getValue();
            
            noteInterface.shutdown();
            if(isSave){
                result.add(noteInterface.getJsonObject());
            }
        }

        m_networks.clear();

        if(isSave){
            long timestamp = System.currentTimeMillis();
            JsonObject resultJson = new JsonObject();
            resultJson.addProperty("code", App.LIST_ITEM_REMOVED);
            resultJson.addProperty("type", App.APP_TYPE);
            resultJson.addProperty("networkId", NETWORK_ID);
            resultJson.addProperty("timeStamp", timestamp);
            resultJson.add("ids", result);

            sendMessage( App.LIST_ITEM_REMOVED, timestamp,NETWORK_ID, resultJson.toString());

            save();
        }
        
    }


  
    private boolean removeApp(String networkId) {
        return removeApp(networkId, true);
    }

    private boolean removeApp(String networkId, boolean isSave) {
        boolean success = false;

        NoteInterface noteInterface = m_apps.remove(networkId);
        
        if(noteInterface != null){
            noteInterface.shutdown();

            if(isSave){
                long timestamp = System.currentTimeMillis();
                JsonObject resultJson = new JsonObject();
                resultJson.addProperty("code", App.LIST_ITEM_REMOVED);
                resultJson.addProperty("type", App.APP_TYPE);
                resultJson.addProperty("networkId", NETWORK_ID);
                resultJson.addProperty("timeStamp", timestamp);
                resultJson.addProperty("id", networkId);

                sendMessage( App.LIST_ITEM_ADDED, timestamp,NETWORK_ID, resultJson.toString());

                save();
            }
        }
                    
    
        return success;
    }

    /*public void broadcastNote(JsonObject note) {

        m_noteInterfaceList.forEach(noteInterface -> {

            noteInterface.sendNote(note, null, null);

        });

    }

    public void broadcastNoteToNetworkIds(JsonObject note, ArrayList<String> networkIds) {

        networkIds.forEach(id -> {
            m_noteInterfaceList.forEach(noteInterface -> {

                int index = id.indexOf(":");
                String networkId = index == -1 ? id : id.substring(0, index);
                if (noteInterface.getNetworkId().equals(networkId)) {

                    note.addProperty("uuid", id);
                    noteInterface.sendNote(note, null, null);
                }
            });
        });
    }*/

    private NoteInterface getNetworkInterface(String networkId) {
        if (networkId != null) {
            return m_networks.get(networkId);
        }
        return null;
    }

    public NoteInterface getNetwork(String networkId) {
        if (networkId != null) {
   
            NoteInterface noteInterface = getNetworkInterface(networkId);

            if (noteInterface != null && noteInterface instanceof Network) {
                Network network = (Network) noteInterface;
                return network.getNoteInterface();
            }
            
        }
        return null;
    }

    public TabInterface getNetworkTab(String networkId, String locationId){
       

       
        
        NoteInterface noteInterface = getNetwork(networkId);
      
        if(noteInterface != null){
            return noteInterface.getTab(m_appStage, locationId, m_heightObject, m_widthObject, m_networkBtn);
        }else{

        }
        return null;
    }

    

    /*public void sendNoteToNetworkId(JsonObject note, String networkId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        m_noteInterfaceList.forEach(noteInterface -> {
            if (noteInterface.getNetworkId().equals(networkId)) {

                noteInterface.sendNote(note, onSucceeded, onFailed);
            }
        });
    }*/

    public JsonArray getAppsArray(){
        JsonArray jsonArray = new JsonArray();

        for (Map.Entry<String, NoteInterface> entry : m_apps.entrySet()) {
            
            NoteInterface noteInterface = entry.getValue();

            jsonArray.add(noteInterface.getJsonObject());
        }


        return jsonArray;
    }
    
    public JsonArray getNetworksArray(){
        JsonArray jsonArray = new JsonArray();

        for (Map.Entry<String, NoteInterface> entry : m_networks.entrySet()) {
            
            NoteInterface noteInterface = entry.getValue();

            jsonArray.add(noteInterface.getJsonObject());
        }


        return jsonArray;
    }

    @Override
    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        JsonArray appsArray = getAppsArray();
        JsonArray networksArray = getNetworksArray();

        json.add("apps", appsArray);
        json.add("networks", networksArray);

        return json;
    }

    private JsonObject getSaveJson(){
        JsonObject fileObject = new JsonObject();
        JsonArray appsArray = getAppsArray();
        JsonArray networksArray = getNetworksArray();

        if(m_currentNetworkId.get() != null){
            fileObject.addProperty("currentNetworkId", m_currentNetworkId.get());
        }
        fileObject.add("netArray", networksArray);
        fileObject.add("apps", appsArray);
        fileObject.add("stage", getStageJson());
        return fileObject;
    }
    
    public void save() {
       
        save("data", ".", "main","root", getSaveJson());

    }
   


    public void open(String networkId, String type, String locationId){

        String currentTabId = m_currentMenuTab.get() != null ? m_currentMenuTab.get().getTabId() : null;

        if(type == null || networkId == null || (currentTabId != null &&  currentTabId.equals(networkId))){
            return;
        }
      
        TabInterface tab = null;
        switch(type){
            case App.STATIC_TYPE:
                tab = getStaticTab(networkId);
            break;
            case App.NETWORK_TYPE:
                tab = getNetworkTab(networkId, locationId);
            break;
        }

    
 
        m_currentMenuTab.set(tab != null ? tab : null);

        switch(type){
            case App.APP_TYPE:
               
            break;

            case App.NETWORK_TYPE:
            
                if(m_currentNetworkId.get() == null || (m_currentNetworkId.get() != null && !m_currentNetworkId.get().equals(networkId))){
                    m_currentNetworkId.set(networkId);
                    save();
                }
               
            break;
        }
        
    }

    public void closeMenuTab(){
        m_currentMenuTab.set(null);
    }

    public List<NoteInterface> getAppsContainsAllKeyWords(String... keyWords){
        //m_searchList = m_marketsList.stream().filter(item -> item.getBaseSymbol().equals(base)).collect(Collectors.toList());
        ArrayList<NoteInterface> list = new ArrayList<>();
        
        
        for (Map.Entry<String, NoteInterface> entry : m_apps.entrySet()) {
            
            NoteInterface appInterface = entry.getValue();

            if(appInterface != null && appInterface instanceof Network){
                Network appNetwork = (Network) appInterface;

                if(containsAllKeyWords(appNetwork, keyWords)){
                    list.add(appNetwork.getNoteInterface());
                }

            }
        }


        return list;
    }


    public static boolean containsAllKeyWords(Network item, String... keywords){
       
            
        Network app = (Network) item;
        String[] appKeyWords = app.getKeyWords();

        SimpleBooleanProperty found = new SimpleBooleanProperty(false);
        
        int appKeyWordsLength = appKeyWords.length;
        int keyWordsLength = keywords.length;

        for(int i = 0; i < keyWordsLength; i++){
            String keyWord = keywords[i];
            found.set(false);
            for(int j = 0; j < appKeyWordsLength ; j++){
                if(appKeyWords[j].equals(keyWord)){
                    found.set(true);
                    break;
                }
            }
            if(found.get() != true){
                return false;
            }
    
        }

        return true;
        
    }


    private TabInterface getStaticTab(String networkId){

        switch(networkId){
            case AppsTab.NAME:
                m_appsTab = m_appsTab == null ?  new AppsTab(m_appStage,this, m_widthObject , m_settingsBtn) : m_appsTab;
            return m_appsTab;

            case SettingsTab.NAME:
                
                m_settingsTab = m_settingsTab == null ?  new SettingsTab(m_appStage,this, getAppData(), m_widthObject , m_settingsBtn) : m_settingsTab;
              
            return m_settingsTab;
            
            case NetworkTab.NAME:
                m_networkTab = m_networkTab == null ? new NetworkTab() : m_networkTab;
            return m_networkTab;
            
        }

        return null;
    }

    private void shutdownStaticTab(String id){
        switch(id){
            case AppsTab.NAME:
                m_appsTab = null;
            break;
            case SettingsTab.NAME:
                
                m_settingsTab = null;
                
            break;
            case NetworkTab.NAME:
                m_networkTab = null;
            break;
            
        }

    }

  
    public SimpleObjectProperty<TabInterface> menuTabProperty() {
        return m_currentMenuTab;
    };


    private VBox m_menuBox = null;

    private VBox m_appTabsBox;
    private VBox m_menuContentBox;

 
    private void initMenu(int imgSize){
        m_tabLabel.setPadding(new Insets(2,0,2,5));
        m_tabLabel.setFont(App.titleFont);
        m_networkToolTip.setShowDelay(new javafx.util.Duration(100));


        Tooltip appsToolTip = new Tooltip("Apps");
        appsToolTip.setShowDelay(new javafx.util.Duration(100));


        ImageView appsImgView = new ImageView();
        appsImgView.setImage(new Image("/assets/apps-outline-35.png"));
        appsImgView.setFitHeight(imgSize);
        appsImgView.setPreserveRatio(true);

        m_appsBtn.setGraphic(appsImgView);
        m_appsBtn.setId("menuTabBtn");
        m_appsBtn.setTooltip(appsToolTip);

   

        Tooltip settingsTooltip = new Tooltip("Settings");
        settingsTooltip.setShowDelay(new javafx.util.Duration(100));
        
        m_settingsBtn = new BufferedButton("/assets/settings-outline-white-30.png", imgSize);
        m_settingsBtn.setTooltip(settingsTooltip);
        m_settingsBtn.setId("menuTabBtn");
        
        m_appTabsBox = new VBox();
        m_menuContentBox = new VBox(m_appTabsBox);

        m_networkBtn =new BufferedButton("/assets/globe-outline-white-30.png", imgSize);
        m_networkBtn.disablePressedEffects();
        
    }

    private BufferedButton m_networkBtn;


    public void createMenu(Stage appStage,VBox menuBox,ScrollPane menuScroll,ScrollPane subMenuScroll, VBox contentBox){
         int imgSize = 30;
         

        m_menuBox = menuBox;
        m_appStage = appStage;
        m_subMenuScroll = subMenuScroll;
        m_boundsListener = (obs,oldval,newval)->{
            m_widthObject.set(newval.getWidth()-2);
            m_heightObject.set(newval.getHeight()-40);
        };    
        m_subMenuScroll.viewportBoundsProperty().addListener(m_boundsListener);

        m_subMenuBox.setAlignment(Pos.TOP_LEFT);
        m_subMenuBox.setId("darkBox");

        HBox vBar = new HBox();
        vBar.setAlignment(Pos.CENTER);
        vBar.setId("vGradient");
        vBar.setMinWidth(1);
        VBox.setVgrow(vBar, Priority.ALWAYS);
       
        initMenu(imgSize);

        
        m_menuScroll = menuScroll;

        
        ContextMenu networkContextMenu = new ContextMenu();



        BufferedButton networkMenuBtn = new BufferedButton("/assets/caret-down-15.png", 10);
        networkMenuBtn.setId("iconBtnDark");
       

        HBox networkMenuBtnBox = new HBox(networkMenuBtn);

        VBox.setVgrow(networkMenuBtnBox, Priority.ALWAYS);
        HBox.setHgrow(networkMenuBtnBox, Priority.ALWAYS);
        networkMenuBtnBox.setAlignment(Pos.TOP_RIGHT);
        networkMenuBtnBox.setOnMouseClicked(e->m_networkBtn.fire());


        HBox socketBox = new HBox();
        socketBox.setId("socketBox");

        socketBox.setMouseTransparent(true);
        socketBox.setMaxWidth(App.MENU_BAR_IMAGE_WIDTH);
        socketBox.setMaxHeight(App.MENU_BAR_IMAGE_WIDTH);

        
        

        StackPane currentNetworkBox = new StackPane(m_networkBtn, socketBox, networkMenuBtnBox);
        HBox.setHgrow(currentNetworkBox, Priority.ALWAYS);
        currentNetworkBox.setAlignment(Pos.CENTER);

        

        Runnable showNetworkMenu = () ->{
  
            networkContextMenu.getItems().clear();
            for (Map.Entry<String, NoteInterface> entry : m_networks.entrySet()) {
            
                NoteInterface noteInterface = entry.getValue();

                if(noteInterface instanceof Network){
                    Network network = (Network) noteInterface;
                    
                    ImageView menuItemImg = new ImageView();
                    menuItemImg.setPreserveRatio(true);
                    menuItemImg.setFitWidth(App.MENU_BAR_IMAGE_WIDTH);
                    menuItemImg.setImage(network.getAppIcon());

                    MenuItem menuItem = new MenuItem(network.getName(), menuItemImg);
                    menuItem.setOnAction(e->{
                       
                        open(network.getNetworkId(), App.NETWORK_TYPE, m_localId);
                    });
                    networkContextMenu.getItems().add(menuItem);
                }
            }

            MenuItem manageMenuItem = new MenuItem("Manage networks...");
            manageMenuItem.setOnAction(e->{

                open(NetworkTab.NAME, App.STATIC_TYPE, m_localId);
            });

            SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();

            networkContextMenu.getItems().addAll(separatorMenuItem, manageMenuItem);

            Point2D p = networkMenuBtn.localToScene(0.0, 0.0);

            networkContextMenu.show(networkMenuBtn,
                    p.getX() + networkMenuBtn.getScene().getX() + networkMenuBtn.getScene().getWindow().getX() + networkMenuBtn.getLayoutBounds().getWidth(),
                    (p.getY() + networkMenuBtn.getScene().getY() + networkMenuBtn.getScene().getWindow().getY()));
        };

        



        networkMenuBtn.setOnAction(e->showNetworkMenu.run());


        Tooltip currentNetworkTooltip = new Tooltip();
        currentNetworkTooltip.setShowDelay(javafx.util.Duration.millis(150));

        m_networkBtn.setTooltip(currentNetworkTooltip);        


        VBox bottomBox = new VBox();


     
        m_menuBox.getChildren().addAll(m_appsBtn, m_menuScroll, m_settingsBtn, currentNetworkBox, bottomBox);
        
     
        m_appsBtn.setOnAction(e -> {
            open(AppsTab.NAME, App.STATIC_TYPE, m_localId);
    
        });

        m_settingsBtn.setOnAction(e->{
            open(SettingsTab.NAME, App.STATIC_TYPE, m_localId);
        });
    
        m_networkBtn.setOnAction(e->{
            NoteInterface noteInterface = getNetworkInterface(m_currentNetworkId.get());

            if(noteInterface != null && noteInterface instanceof Network){
                Network currentNetwork = (Network) noteInterface;
                open(currentNetwork.getNetworkId(), App.NETWORK_TYPE, m_localId);
            }else{
                open(NetworkTab.NAME, App.STATIC_TYPE, m_localId);
            }

        });

        Runnable updateCurrentNetwork = ()->{
            NoteInterface noteInterface = getNetworkInterface(m_currentNetworkId.get());
      

            if(noteInterface != null && noteInterface instanceof Network){
                
                Network currentNetwork = (Network) noteInterface;
                
                m_networkBtn.setImage(currentNetwork.getAppIcon());
                currentNetworkTooltip.setText(currentNetwork.getName());
               
     
            }else{
                m_networkBtn.setImage( new Image("/assets/globe-outline-white-30.png"));
                currentNetworkTooltip.setText("Select network");
                
            }
        };

        updateCurrentNetwork.run();

        m_currentNetworkId.addListener((obs,oldval,newval)->updateCurrentNetwork.run());

 
        Region logoGrowRegion = new Region();
        HBox.setHgrow(logoGrowRegion, Priority.ALWAYS);

        Label closeTabBtn = new Label("🢐");
        closeTabBtn.setId("caretBtn");

        closeTabBtn.setOnMouseClicked(e->{
            closeMenuTab();
        });

        HBox topBarBox = new HBox(m_tabLabel, logoGrowRegion, closeTabBtn);
        HBox.setHgrow(topBarBox, Priority.ALWAYS);
        topBarBox.setAlignment(Pos.CENTER_LEFT);
        topBarBox.setId("networkTopBar");

        
   

        HBox gradBox = new HBox();

        Region hBar = new Region();
        hBar.setPrefWidth(400);
        hBar.setPrefHeight(2);
        hBar.setMinHeight(2);
        hBar.setId("hGradient");

        HBox gBox = new HBox(hBar);
        gBox.setAlignment(Pos.CENTER);
        gBox.setPadding(new Insets(0, 0, 10, 0));

        
        m_currentMenuTab.addListener((obs,oldval,newval)->{

            if(oldval != null){
              
                oldval.setCurrent(false);
                oldval.shutdown();
                switch(oldval.getType()){
                    case App.STATIC_TYPE:
                        shutdownStaticTab(oldval.getTabId());
                    break;
                    case App.NETWORK_TYPE:
                    break;
                }
            }

          
            m_subMenuBox.getChildren().clear();
            gradBox.getChildren().clear();

            if(newval != null && newval instanceof Pane){
                m_tabLabel.setText(newval.getName());
       
                m_subMenuBox.getChildren().addAll(topBarBox,gBox, (Pane) newval);
                gradBox.getChildren().addAll(m_subMenuBox, vBar);
                m_subMenuScroll.setContent( gradBox );

                newval.setCurrent(true);
               
            }else{
                  m_subMenuScroll.setContent(null);
            }
          

        }); 
       
    }

    

    
    public File getDataDir(){
        File dataDir = new File(getAppDir().getAbsolutePath() + "/data");
        if(!dataDir.isDirectory()){
            try{
                Files.createDirectory(dataDir.toPath());
            }catch(IOException e){
                try {
                    Files.writeString(App.logFile.toPath(),"\ncannot create data directory: " + e.toString()  , StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
                
            }
        }
        return dataDir;
    }

    public File getIdDataFile(){
        File dataDir = getDataDir();

        File idDataFile = new File(dataDir.getAbsolutePath() + "/data.dat");
        return idDataFile;
    }

    public File createNewDataFile(File dataDir, JsonObject dataFileJson) {
        
     
        String friendlyId = FriendlyId.createFriendlyId();

        while(dataFileJson != null && isFriendlyId(friendlyId, dataFileJson)){
            friendlyId = FriendlyId.createFriendlyId();
        }
        File dataFile = new File(dataDir.getAbsolutePath() + "/" + friendlyId + ".dat");
        return dataFile;
    }
    
    
    private boolean isFriendlyId(String friendlyId, JsonObject dataFileJson) {
        if(dataFileJson != null){
            
            friendlyId = "/" + friendlyId + ".dat";
            JsonElement idsArrayElement = dataFileJson.get("ids");
            if(idsArrayElement != null && idsArrayElement.isJsonArray()){
                JsonArray idsArray = idsArrayElement.getAsJsonArray();

                for(int i = 0; i < idsArray.size() ; i++){
                    JsonElement idFileObjectElement = idsArray.get(i);

                    if(idFileObjectElement != null && idFileObjectElement.isJsonObject()){
                        JsonObject idFileObject = idFileObjectElement.getAsJsonObject();
                        JsonElement dataElement = idFileObject.get("data");

                        if(dataElement != null && dataElement.isJsonArray()){
                            JsonArray dataArray = dataElement.getAsJsonArray();

                            for(int j = 0; j< dataArray.size(); j++){
                                JsonElement dataFileObjectElement = dataArray.get(j);

                                if(dataFileObjectElement != null && dataFileObjectElement.isJsonObject()){
                                    JsonObject dataFileObject = dataFileObjectElement.getAsJsonObject();

                                    JsonElement fileElement = dataFileObject.get("file");
                                    if(fileElement != null && fileElement.isJsonPrimitive()){
                                        if(fileElement.getAsString().endsWith(friendlyId)){
                                            return true;
                                        }
                                        
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private void updateIdDataFile(SecretKey oldval, SecretKey newval){
        try {
              File idDataFile = getIdDataFile();
              if(idDataFile.isFile()){
                  try {
                      JsonObject dataFileJson = Utils.readJsonFile(oldval, idDataFile);
                      if(dataFileJson != null){
                          Utils.saveJson(newval, dataFileJson, idDataFile);

                          JsonElement idsArrayElement = dataFileJson.get("ids");
                          if(idsArrayElement != null && idsArrayElement.isJsonArray()){
                              JsonArray idsArray = idsArrayElement.getAsJsonArray();

                              for(int i = 0; i < idsArray.size() ; i++){
                                  JsonElement idFileObjectElement = idsArray.get(i);

                                  if(idFileObjectElement != null && idFileObjectElement.isJsonObject()){
                                      JsonObject idFileObject = idFileObjectElement.getAsJsonObject();
                                      JsonElement dataElement = idFileObject.get("data");

                                      if(dataElement != null && dataElement.isJsonArray()){
                                          JsonArray dataArray = dataElement.getAsJsonArray();

                                          for(int j = 0; j< dataArray.size(); j++){
                                              JsonElement dataFileObjectElement = dataArray.get(j);

                                              if(dataFileObjectElement != null && dataFileObjectElement.isJsonObject()){
                                                  JsonObject dataFileObject = dataFileObjectElement.getAsJsonObject();

                                                  JsonElement fileElement = dataFileObject.get("file");
                                                  if(fileElement != null && fileElement.isJsonPrimitive()){
                                                      File file = new File(fileElement.getAsString());
                                                      if(file.isFile()){
                                                          String fileString = Utils.readStringFile(oldval, file);
                                                          if(fileString != null){
                                                              Utils.writeEncryptedString(newval, file, fileString);
                                                          }
                                                      }
                                                  }
                                              }
                                          }
                                      }
                                  }
                              }
                          }
                      }
                  } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                          | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                      try {
                          Files.writeString(App.logFile.toPath(),"Error updating wallets idDataFile key: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                      } catch (IOException e1) {
                      
                      }
                   
                  }

              }
          } catch (IOException e) {
           
          }
  }


    public void save(String subId, String id, String subParent, String parent, JsonObject json) {
        if(id != null && parent != null){
           
            try {
                File idDataFile = getIdDataFile(subId, id, subParent, parent);
                
                if(idDataFile != null && idDataFile.isFile() && json == null){
                    idDataFile.delete();
                }else{
                    Utils.saveJson(getAppData().appKeyProperty().get(), json, idDataFile);
                }

               
            } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                    | IOException e) {
                try {
                    Files.writeString(App.logFile.toPath(),"Error saving networks data" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
                
                }
            }
        }
    }

    public void removeData(  String subParent, String parent){
        String id2 =subParent +  parent;
        removeData(id2);
    }
    
    public void removeData(String id2){
         
        try {
            File idDataFile =  getIdDataFile();
            if(idDataFile.isFile()){
              
                JsonObject json = Utils.readJsonFile(getAppData().appKeyProperty().get(), idDataFile);
                JsonElement idsElement = json.get("ids");
        
                if(idsElement != null && idsElement.isJsonArray()){
                    JsonArray idsArray = idsElement.getAsJsonArray();
                    SimpleIntegerProperty indexProperty = new SimpleIntegerProperty(-1);
                    for(int i = 0; i < idsArray.size(); i ++){
                        JsonElement dataFileElement = idsArray.get(i);
                        if(dataFileElement != null && dataFileElement.isJsonObject()){
                            JsonObject fileObject = dataFileElement.getAsJsonObject();
                            JsonElement dataIdElement = fileObject.get("id");

                            if(dataIdElement != null && dataIdElement.isJsonPrimitive()){
                                String fileId2String = dataIdElement.getAsString();
                                if(fileId2String.equals(id2)){
                                    indexProperty.set(i);
                                    JsonElement dataArrayElement = fileObject.get("data");
                                    if(dataArrayElement != null && dataArrayElement.isJsonArray()){
                                        JsonArray dataArray = dataArrayElement.getAsJsonArray();
                                        for(int j = 0; j< dataArray.size();j++){
                                            JsonElement fileDataObjectElement = dataArray.get(j);
                                            if(fileDataObjectElement != null && fileDataObjectElement.isJsonObject()){
                                                JsonObject fileDataObject = fileDataObjectElement.getAsJsonObject();
                                                JsonElement fileElement = fileDataObject.get("file");
                                                if(fileElement != null && fileElement.isJsonPrimitive()){
                                                    File file = new File(fileElement.getAsString());
                                                    if(file.isFile()){
                                                        Files.delete(file.toPath());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    int index = indexProperty.get();
                    if(index > -1){
                        idsArray.remove(index);
                        json.remove("ids");
                        json.add("ids",idsArray);
                        Utils.saveJson(getAppData().appKeyProperty().get(), json, idDataFile);
                    }
                }
            }
        }catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException | IOException e) {
            try {
                Files.writeString(App.logFile.toPath(),"Error reading Wallets data Array(getAddressInfo): " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {
            
            }
        }
    }

    public JsonObject getData(String subId, String id, String subParent, String parent){
        
        try {
            File idDataFile = getIdDataFile(subId,id, subParent, parent);
            return idDataFile != null && idDataFile.isFile() ? Utils.readJsonFile(getAppData().appKeyProperty().get(), idDataFile) : null;
        } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException | IOException e) {
            try {
                Files.writeString(App.logFile.toPath(),"Error reading Wallets data Array(getAddressInfo): " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {
            
            }
        }
        
        return null;
    }


    public File getIdDataFile(String subId, String id, String subParent, String parent) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException{
        id = id +":" + subId;
        String id2 = parent + ":" + subParent;
        File idDataFile = getIdDataFile();
    
        File dataDir = idDataFile.getParentFile();
           
        if(idDataFile.isFile()){
            
            JsonObject json = Utils.readJsonFile(getAppData().appKeyProperty().get(), idDataFile);
            JsonElement idsElement = json.get("ids");
            json.remove("ids");
            if(idsElement != null && idsElement.isJsonArray()){
                JsonArray idsArray = idsElement.getAsJsonArray();
        
                for(int i = 0; i < idsArray.size(); i ++){
                    JsonElement dataFileElement = idsArray.get(i);
                    if(dataFileElement != null && dataFileElement.isJsonObject()){
                        JsonObject fileObject = dataFileElement.getAsJsonObject();
                        JsonElement dataIdElement = fileObject.get("id");

                        if(dataIdElement != null && dataIdElement.isJsonPrimitive()){
                            String fileId2String = dataIdElement.getAsString();
                            if(fileId2String.equals(id2)){
                                JsonElement dataElement = fileObject.get("data");

                                if(dataElement != null && dataElement.isJsonArray()){
                                    JsonArray dataIdArray = dataElement.getAsJsonArray();
                                    fileObject.remove("data");
                                    for(int j =0; j< dataIdArray.size() ; j++){
                                        JsonElement dataIdArrayElement = dataIdArray.get(j);
                                        if(dataIdArrayElement != null && dataIdArrayElement.isJsonObject()){
                                            JsonObject fileIdObject = dataIdArrayElement.getAsJsonObject();
                                            JsonElement idElement = fileIdObject.get("id");
                                            if(idElement != null && idElement.isJsonPrimitive()){
                                                String fileIdString = idElement.getAsString();
                                                if(fileIdString.equals(id)){
                                                    
                                                    JsonElement fileElement = fileIdObject.get("file");

                                                    if(fileElement != null && fileElement.isJsonPrimitive()){
                                                        return new File(fileElement.getAsString());
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    File newFile = createNewDataFile(dataDir, json);
                                    JsonObject fileJson = new JsonObject();
                                    fileJson.addProperty("id", id);
                                    fileJson.addProperty("file", newFile.getCanonicalPath());

                                    dataIdArray.add( fileJson);
                                    
                                    fileObject.add("data", dataIdArray);

                                    idsArray.set(i, fileObject);

                                    json.add("ids", idsArray);

                                    
                                    Utils.saveJson(getAppData().appKeyProperty().get(), json, idDataFile);
                                    
                                
                                    return newFile;

                                }

                            }
                        }
                    }
                }

                File newFile = createNewDataFile(dataDir, json);

                JsonObject fileJson = new JsonObject();
                fileJson.addProperty("id", id);
                fileJson.addProperty("file", newFile.getCanonicalPath());

                JsonArray dataIdArray = new JsonArray();
                dataIdArray.add(fileJson);

                JsonObject fileObject = new JsonObject();
                fileObject.addProperty("id", id2);
                fileObject.add("data", dataIdArray);

                idsArray.add(fileObject);
                
                json.add("ids", idsArray);
                
                Utils.saveJson(getAppData().appKeyProperty().get(), json, idDataFile);
                    
                return newFile;
            }
        }
      
        
   
        File newFile = createNewDataFile(dataDir, null);

        JsonObject fileJson = new JsonObject();
        fileJson.addProperty("id", id);
        fileJson.addProperty("file", newFile.getCanonicalPath());

        JsonArray dataIdArray = new JsonArray();
        dataIdArray.add(fileJson);

        JsonObject fileObject = new JsonObject();
        fileObject.addProperty("id", id2);
        fileObject.add("data", dataIdArray);
        
        JsonArray idsArray = new JsonArray();
        idsArray.add(fileObject);

        JsonObject json = new JsonObject();
        json.add("ids", idsArray);

       
        Utils.saveJson(getAppData().appKeyProperty().get(), json, idDataFile);
        return newFile;
        
    }

    public void verifyAppKey( String networkName, String cmd, String details, String location, long timeStamp, Runnable runnable){
        int lblCol = 80;
        int rowHeight = 22;
        //String title = "Remove wallet - Ergo Network";
        String timeStampString = Utils.formatDateTimeString(Utils.milliToLocalTime(timeStamp));
        
        String title = "Netnotes - Authorization - " + networkName + " - " + cmd;

        Label networkLbl = new Label("Network:");
        networkLbl.setMinWidth(lblCol);
        networkLbl.setFont(App.txtFont);

        TextField networkField = new TextField(networkName);
        networkField.setEditable(false);
        networkField.setFont(App.txtFont);
        HBox.setHgrow(networkField, Priority.ALWAYS);
        

        HBox networkBox = new HBox(networkLbl, networkField);
        HBox.setHgrow(networkBox, Priority.ALWAYS);
        networkBox.setAlignment(Pos.CENTER_LEFT);
        networkBox.setMinHeight(rowHeight);

        Label cmdLbl = new Label("Command:");
        cmdLbl.setMinWidth(lblCol);
        cmdLbl.setFont(App.txtFont);

        TextField cmdField = new TextField(cmd);
        cmdField.setEditable(false);
        HBox.setHgrow(cmdField, Priority.ALWAYS);
        cmdField.setFont(App.txtFont);

        HBox cmdBox = new HBox(cmdLbl, cmdField);
        HBox.setHgrow(cmdBox,Priority.ALWAYS);
        cmdBox.setAlignment(Pos.CENTER_LEFT);
        cmdBox.setMinHeight(rowHeight);

        
        TextArea textArea = new TextArea(details);
        textArea.setFont(App.txtFont);
        HBox.setHgrow(textArea, Priority.ALWAYS);
        textArea.setPrefRowCount(4);
        textArea.setEditable(false);
        textArea.setWrapText(false);

        HBox infoBox = new HBox(textArea);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        VBox.setVgrow(infoBox,Priority.ALWAYS);
        infoBox.setPadding(new Insets(5,0,0,lblCol));


        Label locationLbl = new Label("Location:");
        locationLbl.setMinWidth(lblCol);
        locationLbl.setFont(App.txtFont);

        TextField locationField = new TextField(location);
        locationField.setEditable(false);
        HBox.setHgrow(locationField, Priority.ALWAYS);
        locationField.setFont(App.txtFont);

        HBox locationBox = new HBox(locationLbl, locationField);
        HBox.setHgrow(locationBox,Priority.ALWAYS);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        locationBox.setMinHeight(rowHeight);

        Label timeLbl = new Label("Time");
        timeLbl.setMinWidth(lblCol);
        timeLbl.setFont(App.txtFont);

        TextField timeField = new TextField(timeStampString);
        timeField.setEditable(false);
        HBox.setHgrow(timeField, Priority.ALWAYS);
        timeField.setFont(App.txtFont);

        HBox timeBox = new HBox(timeLbl, timeField);
        HBox.setHgrow(timeBox,Priority.ALWAYS);
        timeBox.setAlignment(Pos.CENTER_LEFT);
        timeBox.setMinHeight(rowHeight);

        Stage passwordStage = new Stage();
        passwordStage.getIcons().add(App.logo);
        passwordStage.setResizable(false);
        passwordStage.initStyle(StageStyle.UNDECORATED);
        passwordStage.setTitle(title);

        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(App.icon, title, closeBtn, passwordStage);

        ImageView btnImageView = new ImageView(App.logo);
        btnImageView.setPreserveRatio(true);
        btnImageView.setFitHeight(75);
        

        Label textField = new Label("Authorization Required");
        textField.setFont(App.mainFont);
        textField.setPadding(new Insets(20,0,20,15));
        

        VBox imageBox = new VBox(btnImageView, textField);
        imageBox.setAlignment(Pos.CENTER);
        imageBox.setPadding(new Insets(10,0,10,0));

        Text passwordTxt = new Text("Enter password:");
        passwordTxt.setFill(App.txtColor);
        passwordTxt.setFont(App.txtFont);

        PasswordField passwordField = new PasswordField();
        passwordField.setFont(App.txtFont);
        passwordField.setId("passField");

        HBox.setHgrow(passwordField, Priority.ALWAYS);

        HBox passwordBox = new HBox(passwordTxt, passwordField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        passwordBox.setPadding(new Insets(10, 0, 0, 0));


        VBox.setMargin(passwordBox, new Insets(5, 10, 15, 20));



        VBox bodyBox = new VBox(networkBox,cmdBox, infoBox, locationBox, timeBox);
        VBox.setVgrow(bodyBox,Priority.ALWAYS);
        bodyBox.setPadding(new Insets(0,20, 0, 20));

        VBox layoutVBox = new VBox(titleBox, imageBox,bodyBox, passwordBox);
        VBox.setVgrow(layoutVBox, Priority.ALWAYS);

        Scene passwordScene = new Scene(layoutVBox, 800, 600);
        passwordScene.setFill(null);
        passwordScene.getStylesheets().add("/css/startWindow.css");
        passwordStage.setScene(passwordScene);

        Stage statusStage = App.getStatusStage("Verifying", "Verifying...");

        passwordField.setOnAction(e -> {

            String pass = passwordField.getText();
            if (pass.length() < 6) {
                passwordField.setText("");
            } else {
                statusStage.show();
                FxTimer.runLater(Duration.ofMillis(100), ()->{

                    boolean verified = verifyAppPassword(pass);
                        
                    Platform.runLater(() -> passwordField.setText(""));
                    statusStage.close();
                    if (verified) {
                        passwordStage.close();

                        runnable.run();

                    }
                });
            }
        
        });

        closeBtn.setOnAction(e -> {
            passwordStage.close();

        });

        passwordScene.focusOwnerProperty().addListener((obs, oldval, newVal) -> {
            if (newVal != null && !(newVal instanceof PasswordField)) {
                Platform.runLater(() -> passwordField.requestFocus());
            }
        });
        passwordStage.show();
 
        Platform.runLater(() ->{
       
        
            passwordField.requestFocus();}
        );
    }

    
    public static void verifyInfo( String title, String details, long timeStamp, Runnable yes, Runnable no){
        int lblCol = 80;
        int rowHeight = 22;
        //String title = "Remove wallet - Ergo Network";
        String timeStampString = Utils.formatDateTimeString(Utils.milliToLocalTime(timeStamp));
        
        title = "Netnotes - Security - " + title;

 

        TextField cmdField = new TextField(title);
        cmdField.setEditable(false);
        HBox.setHgrow(cmdField, Priority.ALWAYS);
        cmdField.setFont(App.txtFont);

        HBox cmdBox = new HBox(cmdField);
        HBox.setHgrow(cmdBox,Priority.ALWAYS);
        cmdBox.setAlignment(Pos.CENTER_LEFT);
        cmdBox.setMinHeight(rowHeight);

        
        TextArea textArea = new TextArea(details);
        textArea.setFont(App.txtFont);
        HBox.setHgrow(textArea, Priority.ALWAYS);
        textArea.setPrefRowCount(4);
        textArea.setEditable(false);
        textArea.setWrapText(false);

        HBox infoBox = new HBox(textArea);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        VBox.setVgrow(infoBox,Priority.ALWAYS);
        infoBox.setPadding(new Insets(5,0,0,lblCol));


   
        Label timeLbl = new Label("Time");
        timeLbl.setMinWidth(lblCol);
        timeLbl.setFont(App.txtFont);

        TextField timeField = new TextField(timeStampString);
        timeField.setEditable(false);
        HBox.setHgrow(timeField, Priority.ALWAYS);
        timeField.setFont(App.txtFont);

        HBox timeBox = new HBox(timeLbl, timeField);
        HBox.setHgrow(timeBox,Priority.ALWAYS);
        timeBox.setAlignment(Pos.CENTER_LEFT);
        timeBox.setMinHeight(rowHeight);

        Stage passwordStage = new Stage();
        passwordStage.getIcons().add(App.logo);
        passwordStage.setResizable(false);
        passwordStage.initStyle(StageStyle.UNDECORATED);
        passwordStage.setTitle(title);

        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(App.icon, title, closeBtn, passwordStage);

        ImageView btnImageView = new ImageView(App.logo);
        btnImageView.setPreserveRatio(true);
        btnImageView.setFitHeight(75);
        

        Label textField = new Label("Accept");
        textField.setFont(App.mainFont);
        textField.setPadding(new Insets(20,0,20,15));
        

        VBox imageBox = new VBox(btnImageView, textField);
        imageBox.setAlignment(Pos.CENTER);
        imageBox.setPadding(new Insets(10,0,10,0));

        Button yesBtn = new Button("Yes");
        yesBtn.setOnAction(e->yes.run());

        Region spacerRegion = new Region();
        spacerRegion.setMinWidth(20);

        Button noBtn = new Button("No");
        noBtn.setOnAction(e->no.run());


        HBox acceptBtnBox = new HBox(yesBtn, noBtn);
        acceptBtnBox.setAlignment(Pos.CENTER_LEFT);
        acceptBtnBox.setPadding(new Insets(10, 0, 10, 0));

        VBox bodyBox = new VBox(cmdBox, infoBox, timeBox);
        VBox.setVgrow(bodyBox,Priority.ALWAYS);
        bodyBox.setPadding(new Insets(0,20, 0, 20));

        VBox layoutVBox = new VBox(titleBox, imageBox,bodyBox, acceptBtnBox);
        VBox.setVgrow(layoutVBox, Priority.ALWAYS);

        Scene passwordScene = new Scene(layoutVBox, 800, 600);
        passwordScene.setFill(null);
        passwordScene.getStylesheets().add("/css/startWindow.css");
        passwordStage.setScene(passwordScene);



        closeBtn.setOnAction(e -> {
            passwordStage.close();

        });


        passwordStage.show();
 
   
    }

   
     public void verifyAppKey(Runnable runnable) {

        String title = "Netnotes - Enter Password";

        Stage passwordStage = new Stage();
        passwordStage.getIcons().add(App.logo);
        passwordStage.setResizable(false);
        passwordStage.initStyle(StageStyle.UNDECORATED);
        passwordStage.setTitle(title);

        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(App.icon, title, closeBtn, passwordStage);

        Button imageButton = App.createImageButton(App.logo, "Netnotes");

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

        Text passwordTxt = new Text("Enter password:");
        passwordTxt.setFill(App.txtColor);
        passwordTxt.setFont(App.txtFont);

        PasswordField passwordField = new PasswordField();
        passwordField.setFont(App.txtFont);
        passwordField.setId("passField");
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        HBox passwordBox = new HBox(passwordTxt, passwordField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        passwordBox.setPadding(new Insets(20, 0, 0, 0));

        Button clickRegion = new Button();
        clickRegion.setPrefWidth(Double.MAX_VALUE);
        clickRegion.setId("transparentColor");
        clickRegion.setPrefHeight(500);

        clickRegion.setOnAction(e -> {
            passwordField.requestFocus();

        });

        VBox.setMargin(passwordBox, new Insets(5, 10, 0, 20));

        VBox layoutVBox = new VBox(titleBox, imageBox, passwordBox, clickRegion);
        VBox.setVgrow(layoutVBox, Priority.ALWAYS);

        Scene passwordScene = new Scene(layoutVBox, App.STAGE_WIDTH, App.STAGE_HEIGHT);
        passwordScene.setFill(null);
        passwordScene.getStylesheets().add("/css/startWindow.css");
        passwordStage.setScene(passwordScene);

        Stage statusStage = App.getStatusStage("Verifying - Netnotes", "Verifying...");

        passwordField.setOnKeyPressed(e -> {

            KeyCode keyCode = e.getCode();

            if (keyCode == KeyCode.ENTER) {

                if (passwordField.getText().length() < 6) {
                    passwordField.setText("");
                } else {
          
                    statusStage.show();
                    String txt = passwordField.getText();
                    
                    FxTimer.runLater(Duration.ofMillis(100), () -> {
                        boolean verified = verifyAppPassword(txt);
                       
                        Platform.runLater(() -> passwordField.setText(""));
                        statusStage.close();
                        if (verified) {
                            passwordStage.close();

                            runnable.run();

                        }

                    });
                }
            }
        });

        closeBtn.setOnAction(e -> {
            passwordStage.close();

        });

        passwordScene.focusOwnerProperty().addListener((obs, oldval, newVal) -> {
            if (newVal != null && !(newVal instanceof PasswordField)) {
                Platform.runLater(() -> passwordField.requestFocus());
            }
        });
             passwordStage.show();
            
        Platform.runLater(() ->{

            passwordStage.toBack();
            passwordStage.toFront();
            
        }
        );
    }

   /* private void showNetworks(Scene appScene, VBox header, VBox bodyVBox) {
        bodyVBox.setPadding(new Insets(0,2,2,0));
        bodyVBox.setId("darkBox");
        bodyVBox.getChildren().clear();

        Tooltip addTip = new Tooltip("Networks");
        addTip.setShowDelay(new javafx.util.Duration(100));
        addTip.setFont(App.txtFont);

        BufferedButton manageButton = new BufferedButton("assets/filter.png", App.MENU_BAR_IMAGE_WIDTH);
        manageButton.setTooltip(addTip);
        manageButton.setOnAction(e -> m_networksData.showManageNetworkStage());

        Region menuSpacer = new Region();
        HBox.setHgrow(menuSpacer, Priority.ALWAYS);

        Tooltip gridTypeToolTip = new Tooltip("Toggle: List view");
        gridTypeToolTip.setShowDelay(new javafx.util.Duration(50));
        gridTypeToolTip.setHideDelay(new javafx.util.Duration(200));

        BufferedButton toggleGridTypeButton = new BufferedButton("/assets/list-outline-white-25.png", App.MENU_BAR_IMAGE_WIDTH);
        toggleGridTypeButton.setTooltip(gridTypeToolTip);
  

        HBox menuBar = new HBox(manageButton, menuSpacer, toggleGridTypeButton);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(1, 5, 1, 5));

        HBox menuBarPadding = new HBox(menuBar);
        menuBarPadding.setId("darkBox");
        HBox.setHgrow(menuBarPadding, Priority.ALWAYS);
        menuBarPadding.setPadding(new Insets(0,0,4,2));

        header.getChildren().clear();
        header.setPadding(new Insets(0,1,0,1));
        header.setId("darkBox");
        header.getChildren().add(menuBarPadding);


        VBox gridBox = m_networksData.getNetworksBox();


        ScrollPane scrollPane = new ScrollPane(gridBox);
        scrollPane.setId("bodyBox");
        scrollPane.setPadding(new Insets(5));
        scrollPane.prefViewportWidthProperty().bind(appScene.widthProperty());
        scrollPane.prefViewportHeightProperty().bind(appScene.heightProperty());

        Binding<Double> viewportWidth = Bindings.createObjectBinding(()->scrollPane.viewportBoundsProperty().get().getWidth(), scrollPane.viewportBoundsProperty());

        gridBox.prefWidthProperty().bind(viewportWidth);

        VBox scrollPanePadding = new VBox(scrollPane);
        VBox.setVgrow(scrollPanePadding,Priority.ALWAYS);
        HBox.setHgrow(scrollPanePadding, Priority.ALWAYS);
        scrollPanePadding.setPadding(new Insets(0,0,0,3));
        bodyVBox.getChildren().addAll(scrollPanePadding);

        toggleGridTypeButton.setOnAction(e -> {

            m_networksData.iconStyleProperty().set(m_networksData.iconStyleProperty().get().equals(IconStyle.ROW) ? IconStyle.ICON : IconStyle.ROW);

        });

       
    }*/



    /*public String getAckString(String id, long timeStamp) {
        return "{\"id\": \"" + id + "\", \"type\": \"ack\", \"timeStamp\": " + timeStamp + "}";
    }

    private void noteIn(String id, String body, File inFile) {
        switch (body) {
            case App.CMD_SHOW_APPSTAGE:
                show();

                break;
            case App.CMD_SHUTDOWN:
                shutdown();
                break;
            default:
                break;
        }
    }

    private void sendAck(String senderId) {

        File ackFile = new File(m_outDir.getAbsolutePath() + "/" + senderId + OUT_EXT);
        try {
            Files.writeString(ackFile.toPath(), getAckString(senderId, System.currentTimeMillis()));
        } catch (IOException e) {

        }

    }

    private void checkFile(File file) {

        String fileName = file.getName();
        int fileNameLength = fileName.length();
        int extIndex = fileNameLength - INPUT_EXT.length();

        if (fileName.substring(extIndex, fileNameLength).equals(INPUT_EXT)) {
            int hashIndex = fileName.indexOf("#");
            String senderId = hashIndex == -1 ? fileName.substring(0, extIndex) : fileName.substring(0, hashIndex);
            String bodyString = hashIndex == -1 ? null : fileName.substring(hashIndex + 1, extIndex);

            if (bodyString != null) {
                sendAck(senderId);
                //  Platform.runLater(() -> );
                noteIn(senderId, bodyString, file);
            }
        }

    }*/

    private class NetworkTab extends VBox  implements TabInterface{
        public static final String NAME = "Networks";
        private boolean m_current = false;
        private NoteMsgInterface m_networksDataMsgInterface = null;

        public String getTabId(){
            return NAME;
        }
        public String getName(){
            return NAME;
        }
        public void shutdown(){
            if(m_networksDataMsgInterface != null){
                removeMsgListener(m_networksDataMsgInterface);
                m_networksDataMsgInterface = null;
            }
        }
        public void setCurrent(boolean value){
            m_current = value;
        }
        public boolean getCurrent(){
            return m_current;
        }
        public String getType(){
            return App.STATIC_TYPE;
        }
        public boolean isStatic(){
            return true;
        }
        public SimpleStringProperty titleProperty(){
            return null;
        }


        public NetworkTab(){

           
            
            prefWidthProperty().bind(m_widthObject);
            prefHeightProperty().bind(m_heightObject);
            setAlignment(Pos.CENTER);


            Label headingText = new Label("Manage networks");
            headingText.setFont(App.txtFont);
            headingText.setPadding(new Insets(0,0,0,15));

            HBox headingBox = new HBox(headingText);
            headingBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headingBox, Priority.ALWAYS);
            headingBox.setPadding(new Insets(0, 15, 10, 15));
     
            VBox headerBox = new VBox(headingBox);
            headerBox.setPadding(new Insets(0, 5, 0, 0));
            

            Region hBar = new Region();
            hBar.setPrefWidth(400);
            hBar.setPrefHeight(2);
            hBar.setMinHeight(2);
            hBar.setId("hGradient");
    
            HBox gBox = new HBox(hBar);
            gBox.setAlignment(Pos.CENTER);
            gBox.setPadding(new Insets(0, 30, 20, 30));
    
            VBox listBox = new VBox();
            listBox.setPadding(new Insets(10));
            listBox.setId("bodyBox");

            ScrollPane listScroll = new ScrollPane(listBox);
            listScroll.setPrefViewportHeight(100);

            HBox networkListBox = new HBox(listScroll);
            networkListBox.setPadding(new Insets(0,40,0, 40));
         
            HBox.setHgrow(networkListBox, Priority.ALWAYS);
            VBox.setVgrow(networkListBox, Priority.ALWAYS);

            listScroll.prefViewportWidthProperty().bind(networkListBox.widthProperty().subtract(1));
            listScroll.prefViewportHeightProperty().bind(networkListBox.heightProperty().subtract(1));

            listScroll.viewportBoundsProperty().addListener((obs,oldval,newval)->{
                listBox.setMinWidth(newval.getWidth());
                listBox.setMinHeight(newval.getHeight());
            });

            HBox networkOptionsBox = new HBox();
            networkOptionsBox.setAlignment(Pos.CENTER);
            HBox.setHgrow(networkOptionsBox, Priority.ALWAYS);
            networkOptionsBox.setPadding(new Insets(0,0,0,0));

    
            VBox bodyBox = new VBox(networkListBox, networkOptionsBox);
            HBox.setHgrow(bodyBox, Priority.ALWAYS);
            VBox.setVgrow(bodyBox,Priority.ALWAYS);

  
            Runnable updateNetworkList = ()->{

                listBox.getChildren().clear();
        
                for (Map.Entry<String, NoteInterface> entry : m_networks.entrySet()) {
            

                    NoteInterface item = entry.getValue();
                    Network network = item instanceof Network ? (Network) item : null; 
                              
                    if(network != null){
                        ImageView networkImgView = new ImageView();
                        networkImgView.setPreserveRatio(true);
                        networkImgView.setFitWidth(App.MENU_BAR_IMAGE_WIDTH);
                        networkImgView.setImage(network.getAppIcon());

                        Label nameText = new Label(network.getName());
                        nameText.setFont(App.txtFont);
                        nameText.setPadding(new Insets(0,0,0,10));



                        Tooltip selectedTooltip = new Tooltip();
                        selectedTooltip.setShowDelay(javafx.util.Duration.millis(100));

                        Label selectedBtn = new Label();
                        selectedBtn.setTooltip(selectedTooltip);
                        selectedBtn.setId("lblBtn");
                        
                        selectedBtn.setOnMouseClicked(e->{
                            String currentNetworkId = m_currentNetworkId.get();
                            boolean selectedNetwork = currentNetworkId != null && currentNetworkId.equals(network.getNetworkId());         
                        
                            if(selectedNetwork){
                                m_currentNetworkId.set(null);
                            }else{
                                m_currentNetworkId.set(network.getNetworkId());
                                save();
                            }
                        });

                    
                

                        Runnable updateSelectedSwitch = () ->{
                            String currentNetworkId = m_currentNetworkId.get();
                            boolean selectedNetwork = currentNetworkId != null && currentNetworkId.equals(network.getNetworkId());         
                            
                            selectedBtn.setText(selectedNetwork ? "🟊" : "⚝");
                
                            selectedTooltip.setText(selectedNetwork ? "Selected" : "Select network");
                        

                        };
        
                        updateSelectedSwitch.run();
                
                
                        int topMargin = 15;

                        Region marginRegion = new Region();
                        marginRegion.setMinWidth(topMargin);


                        Region growRegion = new Region();
                        HBox.setHgrow(growRegion, Priority.ALWAYS);

                      

                        MenuButton menuBtn = new MenuButton("⋮");
                  
                    

                        MenuItem openItem = new MenuItem("⇲   Open…");
                        openItem.setOnAction(e->{
                            menuBtn.hide();
                            open(network.getNetworkId(), App.NETWORK_TYPE, m_localId);
                        });

                        MenuItem removeItem = new MenuItem("🗑   Uninstall");
                        removeItem.setOnAction(e->{
                            
                            JsonObject note = Utils.getCmdObject("removeNetwork");
                            note.addProperty("id",m_configId);
                            
                            sendNote(note);
                            
                        });

                        menuBtn.getItems().addAll(openItem, removeItem);

                   

                        HBox networkItemTopRow = new HBox(selectedBtn,marginRegion, networkImgView, nameText, growRegion, menuBtn);
                        HBox.setHgrow(networkItemTopRow, Priority.ALWAYS);
                        networkItemTopRow.setAlignment(Pos.CENTER_LEFT);
                        networkItemTopRow.setPadding(new Insets(2,0,2,0));


          

                        VBox networkItem = new VBox(networkItemTopRow);
                        networkItem.setFocusTraversable(true);
                        networkItem.setAlignment(Pos.CENTER_LEFT);
                        HBox.setHgrow(networkItem, Priority.ALWAYS);
                        networkItem.setId("rowBtn");
                        networkItem.setPadding(new Insets(2,5,2,5));

                        networkItemTopRow.setOnMouseClicked(e->{
                            if(e.getClickCount() == 2){
                                openItem.fire();
                            }
                        });

                

           
            
                        listBox.getChildren().add(networkItem);

 
                    }
        
                }
        
    
            };

          
            updateNetworkList.run();

            SimpleObjectProperty<NetworkInformation> installItemInformation = new SimpleObjectProperty<>(null);


            Label installText = new Label("Install: ");
            installText.setMinWidth(58);

            TextField installField = new TextField();
            installField.setPromptText("(Select network to install)");
            installField.setEditable(false);
            HBox.setHgrow(installField, Priority.ALWAYS);

            Label installMenuBtn = new Label("⏷");
            installMenuBtn.setId("lblBtn");

            ImageView installFieldImgView = new ImageView();
            installFieldImgView.setPreserveRatio(true);
            installFieldImgView.setFitWidth(App.MENU_BAR_IMAGE_WIDTH);
            


            HBox installFieldBox = new HBox(installFieldImgView, installField, installMenuBtn);
            HBox.setHgrow(installFieldBox, Priority.ALWAYS);
            installFieldBox.setId("bodyBox");
            installFieldBox.setPadding(new Insets(2, 5, 2, 2));
            installFieldBox.setMaxHeight(18);
            installFieldBox.setAlignment(Pos.CENTER_LEFT);

            Button installBtn = new Button("Install");
      
 
            HBox installBox = new HBox(installText, installFieldBox);
            HBox.setHgrow(installBox,Priority.ALWAYS);
            installBox.setAlignment(Pos.CENTER);
            installBox.setPadding(new Insets(10,20,10,20));

            installItemInformation.addListener((obs,oldval,newval)->{
                if(newval != null){
                    installField.setText(newval.getNetworkName());
                    installFieldImgView.setImage(new Image(newval.getSmallIconString()));
                    if(!installFieldBox.getChildren().contains(installBtn)){
                        installFieldBox.getChildren().add(installBtn);
                    }
                }else{
                    installField.setText("");
                    installFieldImgView.setImage(null);
                    if(installFieldBox.getChildren().contains(installBtn)){
                        installFieldBox.getChildren().remove(installBtn);
                    }
                }
            });


            ContextMenu installContextMenu = new ContextMenu();

            Runnable showInstallContextMenu = ()->{
                installContextMenu.getItems().clear();
                for(int i = 0; i < SUPPORTED_NETWORKS.length; i++){
                    NetworkInformation networkInformation = SUPPORTED_NETWORKS[i];
                    if(getNetwork(networkInformation.getNetworkId()) == null){
                        ImageView intallItemImgView = new ImageView();
                        intallItemImgView.setPreserveRatio(true);
                        intallItemImgView.setFitWidth(App.MENU_BAR_IMAGE_WIDTH);
                        intallItemImgView.setImage(new Image(networkInformation.getSmallIconString()));
                        MenuItem installItem = new MenuItem(String.format("%-33s",networkInformation.getNetworkName()), intallItemImgView);
                    
                        installItem.setOnAction(e->{
                            installItemInformation.set(networkInformation);
                        });

                        installContextMenu.getItems().add(installItem);
                    }
                }
                if(installContextMenu.getItems().size() == 0){
                    MenuItem installItem = new MenuItem("(none available)                 ");
                    installContextMenu.getItems().add(installItem);
                }

                Point2D p = installFieldBox.localToScene(0.0, 0.0);

                installContextMenu.show(installFieldBox,
                    p.getX() + installFieldBox.getScene().getX() + installFieldBox.getScene().getWindow().getX(),
                    (p.getY() + installFieldBox.getScene().getY() + installFieldBox.getScene().getWindow().getY()
                            + installFieldBox.getLayoutBounds().getHeight()));

            };

            Region topRegion = new Region();
            

            installBox.addEventFilter(MouseEvent.MOUSE_CLICKED,e->{
                if(!e.getSource().equals(installBtn)){
                    showInstallContextMenu.run();
                }
            });

    
            VBox.setVgrow(topRegion, Priority.ALWAYS);


                                    
            Region hBar1 = new Region();
            hBar1.setPrefWidth(400);
            hBar1.setMinHeight(2);
            hBar1.setId("hGradient");
    
            HBox gBox1 = new HBox(hBar1);
            gBox1.setAlignment(Pos.CENTER);
            gBox1.setPadding(new Insets(30, 30, 20, 30));

                                        
            HBox botRegionBox = new HBox();
            botRegionBox.setMinHeight(40);
            getChildren().addAll(headerBox, gBox, bodyBox, gBox1, installBox,botRegionBox);
    
        
            installBtn.setOnAction(e->{
                NetworkInformation info = installItemInformation.get();
                String networkId = info != null ? info.getNetworkId() : null;
                
                if (networkId != null){
                    installItemInformation.set(null);
                    installNetwork(networkId);
                    if(m_currentNetworkId.get() == null){
                        m_currentNetworkId.set(networkId);
                    }
                }
            });
            

            m_currentNetworkId.addListener((obs,oldval,newval)->{
                updateNetworkList.run();
            });


            m_networksDataMsgInterface = new NoteMsgInterface() {
                String msgId = FriendlyId.createFriendlyId();
                @Override
                public String getId() {
                    return msgId;
                }
                @Override
                public void sendMessage(int code, long timestamp, String networkId, Number num) {
                }

                @Override
                public void sendMessage(int code, long timestamp, String networkId, String msg) {
                    updateNetworkList.run();
                }
                
            };
           
            addMsgListener(m_networksDataMsgInterface);

        
        }


    }

}
