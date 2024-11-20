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
import com.google.gson.JsonParser;
import com.netnotes.IconButton.IconStyle;
import com.utils.Utils;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
import javafx.scene.control.Alert.AlertType;
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

public class NetworksData {

    public final static long WATCH_INTERVAL = 50;
    public final static String INPUT_EXT = ".in";
    public final static String OUT_EXT = ".out";
    public final static long DEFAULT_CYCLE_PERIOD = 7;
    public final static String NETWORK_ID = "NetworksData";

    public final static String APPS = "APPS";
    public final static String NETWORKS = "NETWORKS";

    public final static int BTN_IMG_SIZE = 30;

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
    private ScrollPane m_subMenuScroll;
    private VBox m_subMenuBox = new VBox();
    private HBox m_topBarBox;
    private HBox m_menuContentBox;
    private SimpleObjectProperty<TabInterface> m_currentMenuTab = new SimpleObjectProperty<TabInterface>();
    private BufferedMenuButton m_settingsBtn;
    private BufferedButton m_appsBtn;
    private Tooltip m_appsToolTip;
    private BufferedButton m_networkBtn;

    private AppsMenu m_appsMenu = null;
    /*private SettingsTab m_settingsTab = null;
    private NetworkTab m_networkTab = null;
    private AppsTab m_appsTab = null;*/

    private Label m_tabLabel = new Label("");
    

    private SimpleDoubleProperty m_widthObject = new SimpleDoubleProperty(App.DEFAULT_STATIC_WIDTH);
    private SimpleDoubleProperty m_heightObject = new SimpleDoubleProperty(200);

    private String m_localId;

    public NetworksData(AppData appData) {
       
        m_appData = appData;
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




    public void openHostUrl(String url) {
        
    }



    private void sendMessage(int code, long timestamp, String type, String msg){
        if(m_appsMenu != null){
            m_appsMenu.sendMessage(code, timestamp, type, msg);
        }
        TabInterface tabInterface = m_currentMenuTab.get();
        if(tabInterface != null && (tabInterface instanceof ManageAppsTab || tabInterface instanceof ManageNetworksTab) ){
            tabInterface.sendMessage(code, timestamp, type, msg);
        }

        for (Map.Entry<String, NoteInterface> entry : m_apps.entrySet()) {
            NoteInterface noteInterface = entry.getValue();
            if(noteInterface instanceof Network){
                Network network = (Network) noteInterface;
                network.sendMessage(code, timestamp, type, msg);
            }
        }

        for (Map.Entry<String, NoteInterface> entry : m_networks.entrySet()) {
            NoteInterface noteInterface = entry.getValue();
            if(noteInterface instanceof Network){
                Network network = (Network) noteInterface;
                network.sendMessage(code, timestamp, type, msg);
            }
        }
        
    }

    private boolean addApp(NoteInterface noteInterface, boolean isSave) {
        // int i = 0;

        String networkId = noteInterface.getNetworkId();

        if (getApp(networkId) == null) {
            m_apps.put(networkId, noteInterface);
            
            if(isSave){
                save();
                long timestamp = System.currentTimeMillis();
                JsonObject resultJson = new JsonObject();
                resultJson.addProperty("code", App.LIST_ITEM_ADDED);
                resultJson.addProperty("neworkId", APPS);
                resultJson.addProperty("timeStamp", timestamp);
                resultJson.addProperty("id", networkId);
                
            
                sendMessage( App.LIST_ITEM_ADDED, timestamp, APPS, resultJson.toString());

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
                resultJson.addProperty("type", NETWORKS);
                resultJson.addProperty("timeStamp", timestamp);
                resultJson.addProperty("id", networkId);
                
            
                sendMessage( App.LIST_ITEM_ADDED, timestamp, NETWORKS, resultJson.toString());


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

                if(m_currentMenuTab.get() != null && m_currentMenuTab.get().getAppId().equals(networkId)){
                    m_currentMenuTab.set(null);
                }
                
                if(isSave){
                    long timestamp = System.currentTimeMillis();
                    JsonObject resultJson = new JsonObject();
                    resultJson.addProperty("code", App.LIST_ITEM_REMOVED);
                    resultJson.addProperty("type", NETWORKS);
                    resultJson.addProperty("timeStamp", timestamp);
                    resultJson.addProperty("id", networkId);
                    
                
                    sendMessage( App.LIST_ITEM_REMOVED, timestamp, NETWORKS,  resultJson.toString());

                    save();
                }
                return true;
            }
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


    private NoteInterface getAppInterface(String networkId) {
        if (networkId != null) {
            return m_apps.get(networkId);
        }
        return null;
    }

    private Network getAppNetwork(String networkId){
        if (networkId != null) {

            NoteInterface noteInterface = getAppInterface(networkId);

            if (noteInterface != null && noteInterface instanceof Network) {
                Network network = (Network) noteInterface;
                return network;
            }
            
        }
        return null;
    }

    public NoteInterface getApp(String networkId) {
        Network network = getAppNetwork(networkId);
        return network != null ? network.getNoteInterface() : null;
    }


    private void installNetwork(String networkId){
        if(getNetwork(networkId) == null && isNetworkSupported(networkId)){
           
            addNetwork(createNetwork(networkId), true);
           
        }
    }

    private void installApp(String networkId){
        installApp(networkId, true);
    }

    private void installApp(String networkId, boolean save) {

        if(getApp(networkId) == null && isAppSupported(networkId)){
           
            addApp(createApp(networkId), true);
           
        }

    }


    private void addAllApps(boolean save) {
        for (NetworkInformation networkInfo : SUPPORTED_APPS) {
            if (getApp(networkInfo.getNetworkId()) == null) {
                installApp(networkInfo.getNetworkId(), false);
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
            resultJson.addProperty("networkId", APPS);
            resultJson.addProperty("timeStamp", timestamp);
            resultJson.add("ids", result);

            sendMessage( App.LIST_ITEM_REMOVED, timestamp, APPS, resultJson.toString());

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
            resultJson.addProperty("networkId", NETWORKS);
            resultJson.addProperty("timeStamp", timestamp);
            resultJson.add("ids", result);

            sendMessage( App.LIST_ITEM_REMOVED, timestamp,NETWORKS, resultJson.toString());

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
                resultJson.addProperty("networkId", APPS);
                resultJson.addProperty("timeStamp", timestamp);
                resultJson.addProperty("id", networkId);

                sendMessage( App.LIST_ITEM_REMOVED, timestamp, APPS, resultJson.toString());

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

    private void openStatic(String networkId){
        String currentTabId = m_currentMenuTab.get() != null ? m_currentMenuTab.get().getAppId() : null;

        if(networkId == null || (currentTabId != null &&  currentTabId.equals(networkId))){
            return;
        }

        TabInterface tab = getStaticTab(networkId);
        if(tab != null){
            m_currentMenuTab.set(tab);
        }
   
    }

   
    private void openNetwork(String networkId){
        
        
        String currentTabId = m_currentMenuTab.get() != null ? m_currentMenuTab.get().getAppId() : null;

        if(networkId == null || (currentTabId != null &&  currentTabId.equals(networkId))){
            return;
        }
      
        NoteInterface noteInterface = getNetwork(networkId);
        
        if(noteInterface != null){

            TabInterface tab = noteInterface != null ? noteInterface.getTab(m_appStage, m_localId, m_heightObject, m_widthObject, m_networkBtn) : null;
    
            
            m_currentMenuTab.set(tab);

            if(m_currentNetworkId.get() == null || (m_currentNetworkId.get() != null && !m_currentNetworkId.get().equals(networkId))){
                m_currentNetworkId.set(networkId);
                save();
            }
        }
    }

    private void openApp(String networkId){

        String currentTabId = m_currentMenuTab.get() != null ? m_currentMenuTab.get().getAppId() : null;

        if(networkId == null || (currentTabId != null &&  currentTabId.equals(networkId))){
            return;
        }
      
        Network appNetwork = getAppNetwork(networkId);


    
        if(appNetwork != null){
       

            TabInterface tab = appNetwork.getTab(m_appStage, m_localId, m_heightObject, m_widthObject, appNetwork.getButton(BTN_IMG_SIZE));
    
            m_currentMenuTab.set( tab);

        
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

        if(m_currentMenuTab.get() != null && m_currentMenuTab.get().getAppId().equals(networkId)){
            return m_currentMenuTab.get();
        }
        switch(networkId){
            case ManageAppsTab.NAME:
                return new ManageAppsTab();

            case SettingsTab.NAME:                
                return  new SettingsTab() ;

            case ManageNetworksTab.NAME:
                return new ManageNetworksTab();

        }

        return null;
    }


  
    public SimpleObjectProperty<TabInterface> menuTabProperty() {
        return m_currentMenuTab;
    };






 

    private SimpleDoubleProperty m_menuWidth;
    private VBox m_contentBox;
    private ContentTabs m_contentTabs;

  

    public void createMenu(Stage appStage,SimpleDoubleProperty menuWidth, HBox menuBox, ScrollPane subMenuScroll, VBox contentBox){
         
        m_menuWidth = menuWidth;
        m_contentBox = contentBox;
        m_appStage = appStage;
        m_subMenuScroll = subMenuScroll;
      
        m_subMenuBox.setAlignment(Pos.TOP_LEFT);
       
        m_heightObject.bind(contentBox.heightProperty().subtract(45));
  
        m_tabLabel.setPadding(new Insets(2,0,2,5));
        m_tabLabel.setFont(App.titleFont);

        m_networkToolTip.setShowDelay(new javafx.util.Duration(100));

        HBox vBar = new HBox();
        vBar.setAlignment(Pos.CENTER);
        vBar.setId("vGradient");
        vBar.setMinWidth(1);
        VBox.setVgrow(vBar, Priority.ALWAYS);
       
        Region menuVBar = new Region();
        VBox.setVgrow(menuVBar, Priority.ALWAYS);
        menuVBar.setPrefWidth(2);
        menuVBar.setMinWidth(2);
        menuVBar.setId("vGradient");
        
        HBox menuVBarBox = new HBox(menuVBar);
        VBox.setVgrow(menuVBarBox, Priority.ALWAYS);
        menuVBarBox.setMinWidth(5);
        menuVBarBox.setAlignment(Pos.CENTER_LEFT);
        menuVBarBox.setId("darkBox");

        m_appsMenu = new AppsMenu();
        m_contentTabs = new ContentTabs();
     
        menuBox.getChildren().addAll( m_appsMenu, menuVBar);
        
        m_contentBox.getChildren().add(m_contentTabs);
             
        Region logoGrowRegion = new Region();
        HBox.setHgrow(logoGrowRegion, Priority.ALWAYS);

        Label closeTabBtn = new Label("🢐");
        closeTabBtn.setId("caretBtn");

        closeTabBtn.setOnMouseClicked(e->{
            closeMenuTab();
        });

        m_topBarBox = new HBox(m_tabLabel, logoGrowRegion, closeTabBtn);
        HBox.setHgrow(m_topBarBox, Priority.ALWAYS);
        m_topBarBox.setAlignment(Pos.CENTER_LEFT);
        m_topBarBox.setId("networkTopBar");

        m_menuContentBox = new HBox();
        m_menuContentBox.setId("darkBox");
        
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
            }

            m_subMenuBox.getChildren().clear();
            m_menuContentBox.getChildren().clear();

            if(newval != null){
                m_tabLabel.setText(newval.getName());
       
                m_subMenuBox.getChildren().addAll(m_topBarBox, gBox, (Pane) newval);
                m_menuContentBox.getChildren().addAll(m_subMenuBox, vBar);
                m_subMenuScroll.setContent( m_menuContentBox );

                newval.setCurrent(true);
               
            }else{
                  m_subMenuScroll.setContent(null);
            }
          

        }); 
       
    }

    
    private File getAppDir(){
        return m_appData.getAppDir();
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

    public File getAssetsDir() throws IOException{
        File assetsDir = new File(getDataDir().getAbsolutePath() + "/assets");
        if(!assetsDir.isDirectory()){
          
            Files.createDirectory(assetsDir.toPath());
          
        }
        return assetsDir;
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


    private class ManageNetworksTab extends AppBox  implements TabInterface{
        public static final String NAME = "Networks";
        private boolean m_current = false;
        private VBox m_listBox = new VBox();
        private ContextMenu m_installContextMenu = new ContextMenu();
        private SimpleObjectProperty<NetworkInformation> m_installItemInformation = new SimpleObjectProperty<>(null);
        private HBox m_installFieldBox;

        public String getName(){
            return NAME;
        }
        public void shutdown(){
            
        }
        public void setCurrent(boolean value){
            m_settingsBtn.setId(value ? "activeMenuBtn" : "menuTabBtn");
            m_current = value;
        }
        public boolean getCurrent(){
            return m_current;
        }
     
        public SimpleStringProperty titleProperty(){
            return null;
        }


        public ManageNetworksTab(){
            super(NAME);
           
            
            prefWidthProperty().bind(m_widthObject);
            prefHeightProperty().bind(m_heightObject);
            setAlignment(Pos.CENTER);
            minHeightProperty().bind(m_heightObject);
    
           
            m_listBox.setPadding(new Insets(10));
         

            ScrollPane listScroll = new ScrollPane(m_listBox);
           
            listScroll.setId("bodyBox");

            HBox networkListBox = new HBox(listScroll);
            networkListBox.setPadding(new Insets(20,40,0, 40));
        

            HBox.setHgrow(networkListBox, Priority.ALWAYS);
            VBox.setVgrow(networkListBox, Priority.ALWAYS);

            listScroll.prefViewportWidthProperty().bind(networkListBox.widthProperty().subtract(1));
            listScroll.prefViewportHeightProperty().bind(m_appStage.getScene().heightProperty().subtract(250));

            listScroll.viewportBoundsProperty().addListener((obs,oldval,newval)->{
                m_listBox.setMinWidth(newval.getWidth());
                m_listBox.setMinHeight(newval.getHeight());
            });

            HBox networkOptionsBox = new HBox();
            networkOptionsBox.setAlignment(Pos.CENTER);
            HBox.setHgrow(networkOptionsBox, Priority.ALWAYS);
            networkOptionsBox.setPadding(new Insets(0,0,0,0));

    
           

            VBox bodyBox = new VBox(networkListBox, networkOptionsBox);
            HBox.setHgrow(bodyBox, Priority.ALWAYS);
            VBox.setVgrow(bodyBox,Priority.ALWAYS);

            updateNetworkList();

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

            m_installFieldBox = new HBox(installFieldImgView, installField, installMenuBtn);
            HBox.setHgrow(m_installFieldBox, Priority.ALWAYS);
            m_installFieldBox.setId("bodyBox");
            m_installFieldBox.setPadding(new Insets(2, 5, 2, 2));
            m_installFieldBox.setMaxHeight(18);
            m_installFieldBox.setAlignment(Pos.CENTER_LEFT);

            Button installBtn = new Button("Install");
      
            HBox installBox = new HBox(installText, m_installFieldBox);
            HBox.setHgrow(installBox,Priority.ALWAYS);
            installBox.setAlignment(Pos.CENTER);
            installBox.setPadding(new Insets(10,20,10,20));

            m_installItemInformation.addListener((obs,oldval,newval)->{
                if(newval != null){
                    installField.setText(newval.getNetworkName());
                    installFieldImgView.setImage(new Image(newval.getSmallIconString()));
                    if(!m_installFieldBox.getChildren().contains(installBtn)){
                        m_installFieldBox.getChildren().add(installBtn);
                    }
                }else{
                    installField.setText("");
                    installFieldImgView.setImage(null);
                    if(m_installFieldBox.getChildren().contains(installBtn)){
                        m_installFieldBox.getChildren().remove(installBtn);
                    }
                }
            });

            Region topRegion = new Region();
            
            installBox.addEventFilter(MouseEvent.MOUSE_CLICKED,e->{
                if(!e.getSource().equals(installBtn)){
                    showInstallContextMenu();
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
            getChildren().addAll( bodyBox, gBox1, installBox,botRegionBox);
    
        
            installBtn.setOnAction(e->{
                NetworkInformation info = m_installItemInformation.get();
                String networkId = info != null ? info.getNetworkId() : null;
                
                if (networkId != null){
                    m_installItemInformation.set(null);
                    installNetwork(networkId);
                    if(m_currentNetworkId.get() == null){
                        m_currentNetworkId.set(networkId);
                    }
                }
            });
            

            m_currentNetworkId.addListener((obs,oldval,newval)->{
                updateNetworkList();
            });

        }

        @Override
        public void sendMessage(int code, long timestamp, String type, String msg) {
            switch(type){
                case NETWORKS:
                    updateNetworkList();
                break;
            }
        }

        public void showInstallContextMenu(){
            m_installContextMenu.getItems().clear();
            for(int i = 0; i < SUPPORTED_NETWORKS.length; i++){
                NetworkInformation networkInformation = SUPPORTED_NETWORKS[i];
                if(getNetwork(networkInformation.getNetworkId()) == null){
                    ImageView intallItemImgView = new ImageView();
                    intallItemImgView.setPreserveRatio(true);
                    intallItemImgView.setFitWidth(App.MENU_BAR_IMAGE_WIDTH);
                    intallItemImgView.setImage(new Image(networkInformation.getSmallIconString()));
                    MenuItem installItem = new MenuItem(String.format("%-40s",networkInformation.getNetworkName()), intallItemImgView);
                
                    installItem.setOnAction(e->{
                        m_installItemInformation.set(networkInformation);
                    });

                    m_installContextMenu.getItems().add(installItem);
                }
            }
            if(m_installContextMenu.getItems().size() == 0){
                MenuItem installItem = new MenuItem(String.format("%-40s","(none available)"));
                m_installContextMenu.getItems().add(installItem);
            }

            Point2D p = m_installFieldBox.localToScene(0.0, 0.0);

            m_installContextMenu.show(m_installFieldBox,
                p.getX() + m_installFieldBox.getScene().getX() + m_installFieldBox.getScene().getWindow().getX(),
                (p.getY() + m_installFieldBox.getScene().getY() + m_installFieldBox.getScene().getWindow().getY()
                        + m_installFieldBox.getLayoutBounds().getHeight()));

        }

        public void updateNetworkList(){

            m_listBox.getChildren().clear();
    
            if(m_networks.size() > 0){
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

                    
                        if(m_networks.size() > 0){
                            MenuButton menuBtn = new MenuButton("⋮");
                    
                        

                            MenuItem openItem = new MenuItem("⇲   Open…");
                            openItem.setOnAction(e->{
                                menuBtn.hide();
                                openNetwork(network.getNetworkId());
                            });

                            MenuItem removeItem = new MenuItem("🗑   Uninstall");
                            removeItem.setOnAction(e->{
                                menuBtn.hide();
                                removeNetwork(network.getNetworkId(), true);
                                
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

                            m_listBox.getChildren().add(networkItem);

                        }
            
                    }
    
                }
            }else{
                
                IconButton emptyAddAppBtn = new IconButton(new Image("/assets/settings-outline-white-30.png"), "Install Network", IconStyle.ICON);
                emptyAddAppBtn.disableActions();
                emptyAddAppBtn.setOnAction(e->{
                    showInstallContextMenu();
                });
                HBox addBtnBox = new HBox(emptyAddAppBtn);
                HBox.setHgrow(addBtnBox, Priority.ALWAYS);
                addBtnBox.setAlignment(Pos.CENTER);
                addBtnBox.setPrefHeight(300);
                m_listBox.getChildren().add(addBtnBox);
            
            }

        }

    }

    public class ManageAppsTab extends AppBox implements TabInterface  {
        public static final int PADDING = 10;
        public static final String NAME = "Manage Apps";


        private SimpleBooleanProperty m_current = new SimpleBooleanProperty(true);
        private SimpleStringProperty m_selectedAppId = new SimpleStringProperty(null);
        private ContextMenu m_installContextMenu = new ContextMenu();
        private VBox m_listBox = new VBox();
        private SimpleObjectProperty<NetworkInformation> m_installItemInformation = new SimpleObjectProperty<>(null);
        private HBox m_installFieldBox;

        public ManageAppsTab(){
            super(NAME);
      
            minHeightProperty().bind(m_heightObject);
            minWidthProperty().bind(m_widthObject);
            maxWidthProperty().bind(m_widthObject);
            setAlignment(Pos.TOP_CENTER);

        
            


            
            m_listBox.setPadding(new Insets(10));
        

            ScrollPane listScroll = new ScrollPane(m_listBox);
            
            listScroll.setId("bodyBox");

            HBox appsListBox = new HBox(listScroll);
            appsListBox.setPadding(new Insets(20,40,0, 40));
           

            HBox.setHgrow(appsListBox, Priority.ALWAYS);
            VBox.setVgrow(appsListBox, Priority.ALWAYS);

            listScroll.prefViewportWidthProperty().bind(appsListBox.widthProperty().subtract(1));
            listScroll.prefViewportHeightProperty().bind(m_appStage.getScene().heightProperty().subtract(250));
            listScroll.viewportBoundsProperty().addListener((obs,oldval,newval)->{
                m_listBox.setMinWidth(newval.getWidth());
                m_listBox.setMinHeight(newval.getHeight());
            });

            HBox appsOptionsBox = new HBox();
            appsOptionsBox.setAlignment(Pos.CENTER);
            HBox.setHgrow(appsOptionsBox, Priority.ALWAYS);
            appsOptionsBox.setPadding(new Insets(0,0,0,0));


   


            VBox bodyBox = new VBox( appsListBox, appsOptionsBox);
            HBox.setHgrow(bodyBox, Priority.ALWAYS);
            VBox.setVgrow(bodyBox,Priority.ALWAYS);
            
            updateAppList();

            


            Label installText = new Label("Install: ");
            installText.setMinWidth(58);

            TextField installField = new TextField();
            installField.setPromptText("(Select app to install)");
            installField.setEditable(false);
            HBox.setHgrow(installField, Priority.ALWAYS);

        

            Label installMenuBtn = new Label("⏷");
            installMenuBtn.setId("lblBtn");

            ImageView installFieldImgView = new ImageView();
            installFieldImgView.setPreserveRatio(true);
            installFieldImgView.setFitWidth(App.MENU_BAR_IMAGE_WIDTH);
            


            m_installFieldBox = new HBox(installFieldImgView, installField, installMenuBtn);
            HBox.setHgrow(m_installFieldBox, Priority.ALWAYS);
            m_installFieldBox.setId("bodyBox");
            m_installFieldBox.setPadding(new Insets(2, 5, 2, 2));
            m_installFieldBox.setMaxHeight(18);
            m_installFieldBox.setAlignment(Pos.CENTER_LEFT);

            Button installBtn = new Button("Install");
        

            HBox installBox = new HBox(installText, m_installFieldBox);
            HBox.setHgrow(installBox,Priority.ALWAYS);
            installBox.setAlignment(Pos.CENTER);
            installBox.setPadding(new Insets(10,20,10,20));

            m_installItemInformation.addListener((obs,oldval,newval)->{
                if(newval != null){
                    installField.setText(newval.getNetworkName());
                    installFieldImgView.setImage(new Image(newval.getSmallIconString()));
                    if(!m_installFieldBox.getChildren().contains(installBtn)){
                        m_installFieldBox.getChildren().add(installBtn);
                    }
                }else{
                    installField.setText("");
                    installFieldImgView.setImage(null);
                    if(m_installFieldBox.getChildren().contains(installBtn)){
                        m_installFieldBox.getChildren().remove(installBtn);
                    }
                }
            });


            
            

           

            Region topRegion = new Region();
            

            installBox.addEventFilter(MouseEvent.MOUSE_CLICKED,e->{
                if(!e.getSource().equals(installBtn)){
                    showInstallContextMenu();
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
            getChildren().addAll(  bodyBox, gBox1, installBox,botRegionBox);

        
            installBtn.setOnAction(e->{
                NetworkInformation info = m_installItemInformation.get();
                String networkId = info != null ? info.getNetworkId() : null;
                
                if (networkId != null){
                    m_installItemInformation.set(null);
                    installApp(networkId);
                    if(m_selectedAppId.get() == null){
                        m_selectedAppId.set(networkId);
                    }
                }
            });
            

            m_selectedAppId.addListener((obs,oldval,newval)->{
                updateAppList();
            });


           

        }

        public void showInstallContextMenu(){
            m_installContextMenu.getItems().clear();
            for(int i = 0; i < SUPPORTED_APPS.length; i++){
                NetworkInformation networkInformation = SUPPORTED_APPS[i];
                if(getNetwork(networkInformation.getNetworkId()) == null){
                    ImageView intallItemImgView = new ImageView();
                    intallItemImgView.setPreserveRatio(true);
                    intallItemImgView.setFitWidth(App.MENU_BAR_IMAGE_WIDTH);
                    intallItemImgView.setImage(new Image(networkInformation.getSmallIconString()));
                    
                    MenuItem installItem = new MenuItem(String.format("%-40s",networkInformation.getNetworkName()), intallItemImgView);
                    installItem.setOnAction(e->{
                        m_installItemInformation.set(networkInformation);
                    });

                    m_installContextMenu.getItems().add(installItem);
                }
            }
            if(m_installContextMenu.getItems().size() == 0){
                MenuItem installItem = new MenuItem(String.format("%-40s", "(none available)"));
                m_installContextMenu.getItems().add(installItem);
            }

            Point2D p = m_installFieldBox.localToScene(0.0, 0.0);
         

            m_installContextMenu.show(m_installFieldBox,
                p.getX() + m_installFieldBox.getScene().getX() + m_installFieldBox.getScene().getWindow().getX(),
                (p.getY() + m_installFieldBox.getScene().getY() + m_installFieldBox.getScene().getWindow().getY()
                        + m_installFieldBox.getLayoutBounds().getHeight()));

        };

        public void updateAppList(){
            m_listBox.getChildren().clear();
        
            if(m_apps.size() > 0){
                for (Map.Entry<String, NoteInterface> entry : m_apps.entrySet()) {
            
                    NoteInterface appInterface = entry.getValue();
                                
                    ImageView appImgView = new ImageView();
                    appImgView.setPreserveRatio(true);
                    appImgView.setFitWidth(App.MENU_BAR_IMAGE_WIDTH);
                    appImgView.setImage(appInterface.getAppIcon());

                    Label nameText = new Label(appInterface.getName());
                    nameText.setFont(App.txtFont);
                    nameText.setPadding(new Insets(0,0,0,10));

                    int topMargin = 15;

                    Region marginRegion = new Region();
                    marginRegion.setMinWidth(topMargin);

                    Region growRegion = new Region();
                    HBox.setHgrow(growRegion, Priority.ALWAYS);

                    MenuButton appListmenuBtn = new MenuButton("⋮");

                    MenuItem openItem = new MenuItem("⇲   Open…");
                    openItem.setOnAction(e->{
                        appListmenuBtn.hide();
                        openApp(appInterface.getNetworkId());
                    });

                    MenuItem removeItem = new MenuItem("🗑   Uninstall");
                    removeItem.setOnAction(e->{
                        appListmenuBtn.hide();
                        removeApp(appInterface.getNetworkId());
                    });

                    appListmenuBtn.getItems().addAll(openItem, removeItem);

                    HBox networkItemTopRow = new HBox( appImgView, nameText, growRegion, appListmenuBtn);
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


                    m_listBox.getChildren().add(networkItem);

                }
            }else{
                IconButton emptyAddAppBtn = new IconButton(new Image("/assets/settings-outline-white-120.png"), "Install App", IconStyle.ICON);
                emptyAddAppBtn.disableActions();
                emptyAddAppBtn.setOnAction(e->{
                    showInstallContextMenu();
                });
                HBox addBtnBox = new HBox(emptyAddAppBtn);
                HBox.setHgrow(addBtnBox, Priority.ALWAYS);
                VBox.setVgrow(addBtnBox, Priority.ALWAYS);
                addBtnBox.setAlignment(Pos.CENTER);
                m_listBox.getChildren().add(addBtnBox);
            }
        }

        @Override
        public void sendMessage(int code, long timestamp,String networkId, String msg){
            switch(networkId){
                case APPS:
                    updateAppList();
                break;
            }
        }

        public void update(){
            
            double minSize = m_listBox.widthProperty().get() - 110;
            minSize = minSize < 110 ? 100 : minSize;

            int numCells = m_apps.size();
            double width = widthProperty().get();
            width = width < minSize ? width : minSize;
            
            
            m_listBox.getChildren().clear();

            if (numCells != 0) {

                for (Map.Entry<String, NoteInterface> entry : m_apps.entrySet()) {
                    NoteInterface noteInterface = entry.getValue();
            
                    IconButton iconButton = new IconButton(noteInterface.getAppIcon(), noteInterface.getName(), IconStyle.ROW);
                    iconButton.setPrefWidth(width);

                    m_listBox.getChildren().add(iconButton);
                }
                
            }else{

            }
        }

    
        public String getName(){
            return NAME;
        }
    
        public void setCurrent(boolean value){
            m_settingsBtn.setId(value ? "activeMenuBtn" : "menuTabBtn");
            m_current.set(value);
          
        }

        
        public boolean getCurrent(){
            return m_current.get();
        } 


        private SimpleStringProperty m_titleProperty = new SimpleStringProperty(NAME);

        public SimpleStringProperty titleProperty(){
            return m_titleProperty;
        }

        public void shutdown(){
            this.prefWidthProperty().unbind();
        }

        @Override
        public void sendMessage(int code, long timestamp, String networkId, Number number) {
            switch(networkId){
                case APPS:
                    update();
                break;
            }
        }
    }

    public class AppsMenu extends VBox {
        //public static final int PADDING = 10;
        public static final String NAME = "Apps";
  
        private VBox m_listBox;
    
        public AppsMenu(){
            super();
            
            
            minWidthProperty().bind(m_menuWidth.add(5));
            maxWidthProperty().bind(m_menuWidth.add(5));
            
            

            setAlignment(Pos.TOP_CENTER);

               
            Tooltip settingsTooltip = new Tooltip("Settings");
            settingsTooltip.setShowDelay(new javafx.util.Duration(100));

         
            
            MenuItem settingsManageAppsItem = new MenuItem("Manage Apps…");
            settingsManageAppsItem.setOnAction(e->{
                openStatic(ManageAppsTab.NAME);
            });
            MenuItem settingsManageNetworksItem = new MenuItem("Manage Networks…");
            settingsManageNetworksItem.setOnAction(e->{
                openStatic(ManageNetworksTab.NAME);
            });
            MenuItem settingsAppItem = new MenuItem("Settings");
            settingsAppItem.setOnAction(e->{
                openStatic(SettingsTab.NAME);
            });

            SeparatorMenuItem seperatorItem = new SeparatorMenuItem();

            
            m_settingsBtn = new BufferedMenuButton("/assets/settings-outline-white-30.png", BTN_IMG_SIZE);
            m_settingsBtn.setTooltip(settingsTooltip);
            m_settingsBtn.setId("menuTabBtn");

            m_settingsBtn.getItems().addAll(settingsManageAppsItem, settingsManageNetworksItem,seperatorItem, settingsAppItem);
        //  m_appTabsBox = new VBox();
        // m_menuContentBox = new VBox(m_appTabsBox);

            m_networkBtn =new BufferedButton("/assets/globe-outline-white-30.png", BTN_IMG_SIZE);
            m_networkBtn.disablePressedEffects();
            m_networkBtn.setId("menuTabBtn");

            m_listBox = new VBox();
            HBox.setHgrow(m_listBox, Priority.ALWAYS);
            m_listBox.setPadding(new Insets(0,5,2,5));
            m_listBox.setAlignment(Pos.TOP_CENTER);
          

            
                
            
            ContextMenu networkContextMenu = new ContextMenu();



            BufferedButton networkMenuBtn = new BufferedButton("/assets/caret-down-15.png", 10);
            networkMenuBtn.setId("iconBtnDark");
        

            HBox networkMenuBtnBox = new HBox(networkMenuBtn);
            networkMenuBtnBox.setId("hand");
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
                        
                            openNetwork(network.getNetworkId());
                        });
                        networkContextMenu.getItems().add(menuItem);
                    }
                }

                MenuItem manageMenuItem = new MenuItem("Manage networks...");
                manageMenuItem.setOnAction(e->{
                    openStatic(ManageNetworksTab.NAME);
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


                
          
        
            m_networkBtn.setOnAction(e->{
                NoteInterface noteInterface = getNetworkInterface(m_currentNetworkId.get());

                if(noteInterface != null && noteInterface instanceof Network){
                    Network currentNetwork = (Network) noteInterface;
                    openNetwork(currentNetwork.getNetworkId());
                }else{
                    openStatic(ManageNetworksTab.NAME);
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



            
            HBox listBoxPadding = new HBox(m_listBox);
            listBoxPadding.minHeightProperty().bind(m_appStage.getScene().heightProperty().subtract(50).subtract(m_settingsBtn.heightProperty()).subtract(currentNetworkBox.heightProperty()));
            listBoxPadding.setId("appMenuBox");
            VBox scrollContentBox = new VBox(listBoxPadding);

            ScrollPane listScroll = new ScrollPane(scrollContentBox);
            listScroll.prefViewportWidthProperty().bind(m_menuWidth.add(5));
            listScroll.prefViewportHeightProperty().bind(m_appStage.getScene().heightProperty().subtract(50).subtract(m_settingsBtn.heightProperty()).subtract(currentNetworkBox.heightProperty()));
        

            /*Region hBar = new Region();
            hBar.prefWidthProperty().bind(m_widthObject.subtract(40));
            hBar.setPrefHeight(2);
            hBar.setMinHeight(2);
            hBar.setId("hGradient");*/

            Button manageBtn = new Button("Manage Apps");
            manageBtn.prefWidthProperty().bind(m_widthObject);
            manageBtn.setId("rowBtn");
            manageBtn.setOnAction(e->{
                openStatic(ManageAppsTab.NAME);
            });

            HBox manageBtnBox = new HBox(manageBtn);
            HBox.setHgrow(manageBtnBox, Priority.ALWAYS);
            manageBtnBox.setAlignment(Pos.CENTER);
            manageBtnBox.setPadding(new Insets(5,0,0,0));


    
            getChildren().addAll(listScroll, m_settingsBtn, currentNetworkBox);
            setId("appMenuBox");

            update();

            
        }
    
 
        public void sendMessage(int code, long timestamp,String networkId, String msg){
            switch(networkId){
                case APPS:
                    update();
                    break;
            }
        }

        public void update(){
       

            m_listBox.getChildren().clear();

            if (m_apps.size() != 0) {
                if(m_appsBtn != null){
                    m_appsBtn = null;
                    m_appsToolTip = null;
                }
    
                for (Map.Entry<String, NoteInterface> entry : m_apps.entrySet()) {
                    NoteInterface noteInterface = entry.getValue();
                    Button appBtn = ((Network) noteInterface).getButton(BTN_IMG_SIZE);
                    appBtn.setOnAction(e->{
                        openApp(noteInterface.getNetworkId());
                    });
                    m_listBox.getChildren().add(appBtn);
                    
                }
            
            }else{
                
                if(m_appsToolTip == null){
                    m_appsToolTip = new Tooltip("Manage Apps");
                    m_appsToolTip.setShowDelay(new javafx.util.Duration(100));

                    
                    m_appsBtn = new BufferedButton("/assets/apps-outline-35.png", BTN_IMG_SIZE);
                    m_appsBtn.setId("menuTabBtn");
                    m_appsBtn.setTooltip(m_appsToolTip);
                    m_appsBtn.setOnAction(e -> {
                
                        openStatic(ManageAppsTab.NAME);
                        
                    });
                }

                if(!m_listBox.getChildren().contains(m_appsBtn)){
                    m_listBox.getChildren().add(m_appsBtn);
                }
            }
        }
    
      
    }
    /*
                    double imageWidth = 75;
                    double cellPadding = 15;
                    double cellWidth = imageWidth + (cellPadding * 2);
    
                    int floor = (int) Math.floor(width / cellWidth);
                    int numCol = floor == 0 ? 1 : floor;
                    // currentNumCols.set(numCol);
                  //  int numRows = numCells > 0 && numCol != 0 ? (int) Math.ceil(numCells / (double) numCol) : 1;
    
                  ArrayList<HBox> rowsBoxes = new ArrayList<HBox>();
    
                  ItemIterator grid = new ItemIterator();
                  //j = row
                  //i = col
      
                  for (Map.Entry<String, NoteInterface> entry : m_apps.entrySet()) {
                        NoteInterface noteInterface = entry.getValue();
    
                        if(rowsBoxes.size() < (grid.getJ() + 1)){
                            HBox newHBox = new HBox();
                            rowsBoxes.add(newHBox);
                            m_listBox.getChildren().add(newHBox);
                        }
    
                        HBox rowBox = rowsBoxes.get(grid.getJ());
    
                        IconButton iconButton = new IconButton(noteInterface.getAppIcon(), noteInterface.getName(), IconStyle.ICON);
    
                        rowBox.getChildren().add(iconButton);
        
                        if (grid.getI() < numCol) {
                            grid.setI(grid.getI() + 1);
                        } else {
                            grid.setI(0);
                            grid.setJ(grid.getJ() + 1);
                        }
                  } */

    public class SettingsTab extends AppBox implements TabInterface  {
        public final static String NAME = "Settings";
    
    
        private SimpleBooleanProperty m_current = new SimpleBooleanProperty(true);
        
        public boolean getCurrent(){
            return m_current.get();
        } 
    
        public void setCurrent(boolean value){
            m_current.set(value);
            m_settingsBtn.setId(value ? "activeMenuBtn" : "menuTabBtn");
        }
    
    
    
        public SettingsTab(){
            super(NAME);
            minHeightProperty().bind(m_heightObject);
    
            Button settingsButton = App.createImageButton(App.logo, "Settings");
    
            HBox settingsBtnBox = new HBox(settingsButton);
            settingsBtnBox.setAlignment(Pos.CENTER);
    
            Text passwordTxt = new Text(String.format("%-18s", "  Password:"));
            passwordTxt.setFill(App.txtColor);
            passwordTxt.setFont(App.txtFont);
    
    
    
            Button passwordBtn = new Button("(click to update)");
         
            passwordBtn.setAlignment(Pos.CENTER_LEFT);
            passwordBtn.setId("toolBtn");
            passwordBtn.setOnAction(e -> {
                Button closeBtn = new Button();
                verifyAppKey(()->{
                    Stage passwordStage = App.createPassword("Netnotes - Password", App.logo, App.logo, closeBtn, getExecService(), (onSuccess) -> {
                        Object sourceObject = onSuccess.getSource().getValue();
    
                        if (sourceObject != null && sourceObject instanceof String) {
                            String newPassword = (String) sourceObject;
    
                            if (!newPassword.equals("")) {
    
                                Stage statusStage = App.getStatusStage("Netnotes - Saving...", "Saving...");
                                statusStage.show();
                                FxTimer.runLater(Duration.ofMillis(100), () -> {
                                    final String hash = Utils.getBcryptHashString(newPassword);
                                    Platform.runLater(()->{
                                        try {
    
                                            getAppData().setAppKey(hash);
                                            getAppData().createKey(newPassword);
                                        } catch ( Exception e1) {
                                            try {
                                                Files.writeString(App.logFile.toPath(), "App createPassword: " +  e1.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                            } catch (IOException e2) {
                                            
                                            }
                                            Alert a = new Alert(AlertType.NONE, "Error: Password not changed.", ButtonType.CLOSE);
                                            a.setTitle("Error: Password not changed.");
                                            a.initOwner(m_appStage);
                                            a.show();
                                        }
                                    });
                                    statusStage.close();
    
                                });
                            } else {
                                Alert a = new Alert(AlertType.NONE, "Netnotes: Passwod not change.\n\nCanceled by user.", ButtonType.CLOSE);
                                a.setTitle("Netnotes: Password not changed");
                                a.initOwner(m_appStage);
                                a.show();
                            }
                        }
                        closeBtn.fire();
                    });
                    passwordStage.show();
                });
            });
    
            HBox passwordBox = new HBox(passwordTxt, passwordBtn);
            passwordBox.setAlignment(Pos.CENTER_LEFT);
            passwordBox.setPadding(new Insets(10, 0, 10, 10));
            passwordBox.setMinHeight(30);
    
            Tooltip checkForUpdatesTip = new Tooltip();
            checkForUpdatesTip.setShowDelay(new javafx.util.Duration(100));
    
            String checkImageUrlString = "/assets/checkmark-25.png";
            
    
            BufferedButton checkForUpdatesToggle = new BufferedButton(getAppData().getUpdates() ? checkImageUrlString : null, App.MENU_BAR_IMAGE_WIDTH);
            checkForUpdatesToggle.setTooltip(checkForUpdatesTip);
    
            checkForUpdatesToggle.setOnAction(e->{
                boolean wasUpdates = getAppData().getUpdates();
                
                wasUpdates = !wasUpdates;
    
                checkForUpdatesToggle.setImage(wasUpdates ? new Image(checkImageUrlString) : null);
        
                try {
                    getAppData().setUpdates(wasUpdates);
                } catch (IOException e1) {
                    Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.CANCEL);
                    a.setTitle("Error: File IO");
                    a.setHeaderText("Error");
                    a.initOwner(m_appStage);
                    a.show();
                }
            });
    
            Text versionTxt = new Text(String.format("%-18s", "  Version:"));
            versionTxt.setFill(App.txtColor);
            versionTxt.setFont(App.txtFont);
            //LATEST_RELEASE_URL
    
            TextField versionField = new TextField(App.CURRENT_VERSION);
            versionField.setFont(App.txtFont);
            versionField.setId("formField");
            versionField.setEditable(false);
            HBox.setHgrow(versionField, Priority.ALWAYS);
       
            HBox versionBox = new HBox(versionTxt, versionField);
            versionBox.setPadding(new Insets(10,10,5,10));
            versionBox.setAlignment(Pos.CENTER_LEFT);
    
            Text fileTxt = new Text(String.format("%-18s", "  File:"));
            fileTxt.setFill(App.txtColor);
            fileTxt.setFont(App.txtFont);
            //LATEST_RELEASE_URL
    
            TextField fileField = new TextField(m_appData.getAppFile().getName());
            fileField.setFont(App.txtFont);
            fileField.setEditable(false);
            fileField.setId("formField");
            HBox.setHgrow(fileField, Priority.ALWAYS);
       
            HBox fileBox = new HBox(fileTxt, fileField);
            HBox.setHgrow(fileBox, Priority.ALWAYS);
            fileBox.setAlignment(Pos.CENTER_LEFT);
            fileBox.setPadding(new Insets(5,10,5,10));
        
            Text hashTxt = new Text(String.format("%-18s", "  Hash (Blake-2b):"));
            hashTxt.setFill(App.txtColor);
            hashTxt.setFont(App.txtFont);
            //LATEST_RELEASE_URL
    
            TextField hashField = new TextField(m_appData.appHashData().getHashStringHex());
            hashField.setFont(App.txtFont);
            hashField.setEditable(false);
            hashField.setId("formField");
            HBox.setHgrow(hashField, Priority.ALWAYS);
       
            HBox hashBox = new HBox(hashTxt, hashField);
            HBox.setHgrow(hashBox, Priority.ALWAYS);
            hashBox.setPadding(new Insets(5,10,5,10));
            hashBox.setAlignment(Pos.CENTER_LEFT);
    
            Text passwordHeading = new Text("Password");
            passwordHeading.setFont(App.txtFont);
            passwordHeading.setFill(App.txtColor);
    
            HBox passHeadingBox = new HBox(passwordHeading);
            HBox.setHgrow(passHeadingBox,Priority.ALWAYS);
            passHeadingBox.setId("headingBox");
            passHeadingBox.setPadding(new Insets(5));
    
            VBox passwordSettingsBox = new VBox(passHeadingBox, passwordBox);
            passwordSettingsBox.setId("bodyBox");
    
            Text appHeading = new Text("App");
            appHeading.setFont(App.txtFont);
            appHeading.setFill(App.txtColor);
    
            HBox appHeadingBox = new HBox(appHeading);
            HBox.setHgrow(appHeadingBox,Priority.ALWAYS);
            appHeadingBox.setId("headingBox");
            appHeadingBox.setPadding(new Insets(5));
    
            VBox appSettingsBox = new VBox(appHeadingBox, versionBox, fileBox, hashBox);
            appSettingsBox.setId("bodyBox");
            
    
            Text latestVersionTxt = new Text(String.format("%-18s", "  Version:"));
            latestVersionTxt.setFill(App.txtColor);
            latestVersionTxt.setFont(App.txtFont);
            //LATEST_RELEASE_URL
    
            Button latestVersionField = new Button("(Click to get latest info.)");
            latestVersionField.setFont(App.txtFont);
            latestVersionField.setId("formField");
            HBox.setHgrow(latestVersionField, Priority.ALWAYS);
       
            HBox latestVersionBox = new HBox(latestVersionTxt, latestVersionField);
            latestVersionBox.setPadding(new Insets(10,10,5,10));
            latestVersionBox.setAlignment(Pos.CENTER_LEFT);
    
            Text latestURLTxt = new Text(String.format("%-18s", "  Url:"));
            latestURLTxt.setFill(App.txtColor);
            latestURLTxt.setFont(App.txtFont);
            //LATEST_RELEASE_URL
    
            TextField latestURLField = new TextField();
            latestURLField.setFont(App.txtFont);
            latestURLField.setEditable(false);
            latestURLField.setId("formField");
            HBox.setHgrow(latestURLField, Priority.ALWAYS);
       
            HBox latestURLBox = new HBox(latestURLTxt, latestURLField);
            HBox.setHgrow(latestURLBox, Priority.ALWAYS);
            latestURLBox.setAlignment(Pos.CENTER_LEFT);
            latestURLBox.setPadding(new Insets(5,10,5,10));
    
            Text latestNameTxt = new Text(String.format("%-18s", "  File name:"));
            latestNameTxt.setFill(App.txtColor);
            latestNameTxt.setFont(App.txtFont);
            //LATEST_RELEASE_URL
    
            TextField latestNameField = new TextField();
            latestNameField.setFont(App.txtFont);
            latestNameField.setEditable(false);
            latestNameField.setId("formField");
            HBox.setHgrow(latestNameField, Priority.ALWAYS);
       
            HBox latestNameBox = new HBox(latestNameTxt, latestNameField);
            HBox.setHgrow(latestNameBox, Priority.ALWAYS);
            latestNameBox.setAlignment(Pos.CENTER_LEFT);
            latestNameBox.setPadding(new Insets(5,10,5,10));
        
            Text latestHashTxt = new Text(String.format("%-18s", "  Hash (Blake-2b):"));
            latestHashTxt.setFill(App.txtColor);
            latestHashTxt.setFont(App.txtFont);
            //LATEST_RELEASE_URL
    
            TextField latestHashField = new TextField();
            latestHashField.setFont(App.txtFont);
            latestHashField.setEditable(false);
            latestHashField.setId("formField");
            HBox.setHgrow(latestHashField, Priority.ALWAYS);
       
            HBox latestHashBox = new HBox(latestHashTxt, latestHashField);
            HBox.setHgrow(latestHashBox, Priority.ALWAYS);
            latestHashBox.setPadding(new Insets(5,10,5,10));
            latestHashBox.setAlignment(Pos.CENTER_LEFT);
    
            
            Text latestHeading = new Text("Latest");
            latestHeading.setFont(App.txtFont);
            latestHeading.setFill(App.txtColor);
    
            Region latestHeadingSpacer = new Region();
            HBox.setHgrow(latestHeadingSpacer, Priority.ALWAYS);
    
            Button downloadLatestBtn = new Button("Download");
            
            SimpleObjectProperty<UpdateInformation> updateInfoProperty = new SimpleObjectProperty<>();
    
            updateInfoProperty.addListener((obs,oldval,newval)->{
            
                latestHashField.setText(newval.getJarHashData().getHashStringHex());
                latestVersionField.setText(newval.getTagName());
                latestNameField.setText(newval.getJarName());
                latestURLField.setText(newval.getJarUrl());
            
            });
            
            
    
            Button getInfoBtn = new Button("Update");
            getInfoBtn.setId("checkBtn");
            getInfoBtn.setOnAction(e->{
                getAppData().checkForUpdates(getExecService(), updateInfoProperty);         
            });
            downloadLatestBtn.setOnAction(e->{
                SimpleObjectProperty<UpdateInformation> downloadInformation = new SimpleObjectProperty<>();
                UpdateInformation updateInfo = updateInfoProperty.get();
                File appDir = getAppData().getAppDir();
                if(updateInfo != null && updateInfo.getJarHashData() != null){
                
                    HashData appHashData = updateInfo.getJarHashData();
                    String appName = updateInfo.getJarName();
                    String urlString = updateInfo.getJarUrl();
                 
                    HashDataDownloader dlder = new HashDataDownloader(App.logo, urlString, appName, appDir, appHashData, HashDataDownloader.Extensions.getJarFilter());
                    dlder.start(getExecService());
    
                }else{
                    downloadInformation.addListener((obs,oldval,newval)->{
                        if(newval != null){
                            updateInfoProperty.set(newval);
    
                            String urlString = newval.getJarUrl();
                            if(urlString.startsWith("http")){  
                                HashData latestHashData = newval.getJarHashData();
                                HashDataDownloader dlder = new HashDataDownloader(App.logo, urlString, latestNameField.getText(),appDir, latestHashData, HashDataDownloader.Extensions.getJarFilter());
                                dlder.start(getExecService());
                            }
                        }
                    });
                    getAppData().checkForUpdates(getExecService(), downloadInformation);
                }
            });
    
            latestVersionField.setOnAction(e->{
                getInfoBtn.fire();
            });
    
            HBox latestHeadingBox = new HBox(latestHeading, latestHeadingSpacer, getInfoBtn);
            HBox.setHgrow(latestHeadingBox,Priority.ALWAYS);
            latestHeadingBox.setId("headingBox");
            latestHeadingBox.setPadding(new Insets(5,10,5,10));
            latestHeadingBox.setAlignment(Pos.CENTER_LEFT);
         
            
           
            HBox downloadLatestBox = new HBox(downloadLatestBtn);
            downloadLatestBox.setAlignment(Pos.CENTER_RIGHT);
            downloadLatestBox.setPadding(new Insets(5,15,10,10));
    
            VBox latestSettingsBox = new VBox(latestHeadingBox, latestVersionBox, latestNameBox, latestURLBox, latestHashBox, downloadLatestBox);
            latestSettingsBox.setId("bodyBox");
            
            
            Region settingsSpacer1 = new Region();
            settingsSpacer1.setMinHeight(15);
    
            Region settingsSpacer2 = new Region();
            settingsSpacer2.setMinHeight(15);
    
            VBox settingsVBox = new VBox(settingsBtnBox, passwordSettingsBox, settingsSpacer1, appSettingsBox, settingsSpacer2, latestSettingsBox);
            HBox.setHgrow(settingsVBox, Priority.ALWAYS);
    
            settingsVBox.setAlignment(Pos.CENTER_LEFT);
            settingsVBox.setPadding(new Insets(5,10,5,5));
    
    
    
            HBox.setHgrow(settingsVBox, Priority.ALWAYS);
    
            getChildren().add(settingsVBox);
    
         
            prefWidthProperty().bind(m_widthObject);
    
        }
    
        private SimpleStringProperty m_titleProperty = new SimpleStringProperty(NAME);
    
        public SimpleStringProperty titleProperty(){
            return m_titleProperty;
        }
         
        public String getName(){
            return NAME;
        }
    
    }

    public ContentTabs getContentTabs(){
        return m_contentTabs;
    }




    public class ContentTabs extends VBox{
        private HBox m_tabsContent;
        private ScrollPane m_tabsScroll;
        private ScrollPane m_bodyScroll;
        private SimpleDoubleProperty m_tabsHeight = new SimpleDoubleProperty(40);

       
        private HashMap<String, ContentTab> m_itemTabs = new HashMap<>();

        private SimpleStringProperty m_currentId = new SimpleStringProperty(null);

        public ContentTabs(){
            m_tabsContent = new HBox();
            m_tabsContent.setAlignment(Pos.BOTTOM_LEFT);
            m_tabsScroll = new ScrollPane(m_tabsContent);

            m_tabsScroll.prefViewportWidthProperty().bind(m_contentBox.widthProperty().subtract(1));
            m_tabsScroll.prefViewportHeightProperty().bind(m_tabsHeight);
            m_tabsScroll.setId("tabsBox");

            m_tabsScroll.viewportBoundsProperty().addListener((obs,oldval,newval)->{
                m_tabsContent.prefHeight(newval.getHeight()-1);
                m_tabsContent.prefWidth(newval.getWidth()-1);
            });

            m_bodyScroll = new ScrollPane();
            m_bodyScroll.prefViewportWidthProperty().bind(m_contentBox.widthProperty().subtract(1));
            m_bodyScroll.prefViewportHeightProperty().bind(m_contentBox.heightProperty().subtract(m_tabsScroll.heightProperty()).subtract(1));

            getChildren().addAll(m_tabsScroll, m_bodyScroll);

            m_currentId.addListener((obs,oldval,newval)->{
                if(newval != null){
                    ContentTab tab = m_itemTabs.get(newval);
                    if(tab != null){
                        m_bodyScroll.setContent(tab.getPane());
                    }else{
                        m_currentId.set(null);
                    }
                }else{
                    m_bodyScroll.setContent(null);
                }
            });
        }

        public ContentTab getTab(String id){
            return m_itemTabs.get(id);
        }

        public boolean contains(String id){
            return m_itemTabs.get(id) != null;
        }

        public void addContentTab(ContentTab tab){
            Pane tabPane = tab.getPane();
            String id = tab.getId();
            if(tabPane != null && id != null){
                m_itemTabs.put(id, tab);
                tab.currentIdProperty().bind(m_currentId);
                m_contentTabs.getChildren().add(tab.getTabBox());
                tab.onCloseBtn(e->{
                    removeContentTab(id);
                });
                tab.onTabClicked(e->{
                    m_currentId.set(tab.getId());
                });
                /*Binding<Double> prefWidth = Bindings.createObjectBinding(()->m_bodyScroll.viewportBoundsProperty().get() != null ? (m_bodyScroll.viewportBoundsProperty().get().getWidth() < 400 ? 400 : m_bodyScroll.viewportBoundsProperty().get().getWidth()) : 400, m_bodyScroll.viewportBoundsProperty());
                Binding<Double> prefHeight = Bindings.createObjectBinding(()->m_bodyScroll.viewportBoundsProperty().get() != null ? (m_bodyScroll.viewportBoundsProperty().get().getHeight() < 400 ? 400 : m_bodyScroll.viewportBoundsProperty().get().getHeight()) : 400, m_bodyScroll.viewportBoundsProperty());
                tabPane.prefWidthProperty().bind(prefWidth);
                tabPane.prefHeightProperty().bind(prefHeight);*/
                m_currentId.set(id);
            }
        }


        public ContentTab removeContentTab(String id){
            if(m_currentId.get() != null && m_currentId.get().equals(id)){
                m_currentId.set(null);
            }
           
            ContentTab tab = m_itemTabs.remove(id);
            if(tab != null){
                tab.currentIdProperty().unbind();
                tab.onCloseBtn(null);
                tab.onTabClicked(null);
                Pane tabPane = tab.getPane();
                tabPane.prefWidthProperty().unbind();
                tabPane.prefHeightProperty().unbind();
            }

            return tab;
        }

    
        public ArrayList<ContentTab> getContentTabByParentId(String parentId){
            ArrayList<ContentTab> result = new ArrayList<>();
            
            for (Map.Entry<String, ContentTab> entry : m_itemTabs.entrySet()) {
                ContentTab contentTab = entry.getValue();
                if(parentId != null){   
                    if(contentTab.getParentId() != null && contentTab.getParentId().equals(parentId)){
                        result.add(contentTab);
                    }
                }else{
                    if(contentTab.getParentId() == null){
                        result.add(contentTab);
                    }
                }
            }
            
            return result;
        }

        public void removeByParentId(String id){
            for (Map.Entry<String, ContentTab> entry : m_itemTabs.entrySet()) {
                ContentTab contentTab = entry.getValue();
                
                if(contentTab.getParentId().equals(id)){
                    removeContentTab(contentTab.getId());
                }
            }        
        }

        public void sendMessage(int code, long timestamp, String type, String msg){
            switch(type){
                case APPS:
                    removeByParentId(Utils.parseMsgForJsonId(msg));
                break;
            }
        }

    } 
    
}
