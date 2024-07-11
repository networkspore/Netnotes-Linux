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
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
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
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class NetworksData implements InstallerInterface {

    public final static long WATCH_INTERVAL = 50;
    public final static String INPUT_EXT = ".in";
    public final static String OUT_EXT = ".out";
    public final static long DEFAULT_CYCLE_PERIOD = 7;

    public final static String[] INTALLABLE_NETWORK_IDS = new String[]{
   
        KucoinExchange.NETWORK_ID,
        SpectrumFinance.NETWORK_ID
        
    };
    
    
    private Tooltip m_networkToolTip = new Tooltip("Network");

    
    private ExecutorService m_execService = Executors.newFixedThreadPool(6);
    
    private ObservableList<NoteInterface> m_noteInterfaceList = FXCollections.observableArrayList();
    private ObservableList<NoteInterface> m_networkList = FXCollections.observableArrayList();

    private double m_leftColumnWidth = 175;

    private VBox m_installedVBox = null;
    private VBox m_notInstalledVBox = null;
    private ArrayList<InstallableIcon> m_installables = new ArrayList<>();

    private Stage m_addNetworkStage = null;

    private InstallableIcon m_focusedInstallable = null;

    private HostServices m_hostServices;
  

    private File m_notesDir;
    private File m_outDir;
    private File m_inDir;

    private NoteWatcher m_noteWatcher = null;

    private SimpleStringProperty m_stageIconStyle = new SimpleStringProperty(IconStyle.ICON);

    private double m_stageWidth = 700;
    private double m_stageHeight = 500;
    private double m_stagePrevWidth = 310;
    private double m_stagePrevHeight = 500;
    private boolean m_stageMaximized = false;
    private AppData m_appData;
    private String m_networkId;

    private StringExpression m_titleExpression = null;
    private Stage m_appStage = null;
    private ChangeListener<Bounds> m_boundsListener = null;
    private SimpleObjectProperty<NoteInterface> m_currentNetwork = new SimpleObjectProperty<>(null);
    private String m_currentNetworkId = null;
    private ScrollPane m_menuScroll;
    private ScrollPane m_subMenuScroll;
    private HBox m_subMenuBox = new HBox();

    private SimpleObjectProperty<TabInterface> m_currentTab = new SimpleObjectProperty<TabInterface>();
    private ImageView m_networkImgView = new ImageView();
    private Button m_settingsBtn = new Button();
    private Button m_appsBtn = new Button();
    private Button m_networkBtn = new Button();

    private SettingsTab m_settingsTab = null;
    private NetworkTab m_networkTab = null;
    private AppsTab m_appsTab = null;

    private SimpleDoubleProperty m_widthObject = new SimpleDoubleProperty(300);
    private SimpleDoubleProperty m_heightObject = new SimpleDoubleProperty(200);

    public NetworksData(AppData appData,  HostServices hostServices) {
        m_appData = appData;
        m_networkId = FriendlyId.createFriendlyId();
     
        m_hostServices = hostServices;
       
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        try {
            
            InputStream stream = App.class.getResource("/assets/OCRAEXT.TTF").openStream();
            java.awt.Font ocrFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, stream).deriveFont(48f);
            ge.registerFont(ocrFont);
            stream.close();
           

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
    public final static NetworkInformation NO_NETWORK = new NetworkInformation("NO_NETWORK", "No network","/assets/globe-outline-white-120.png", "/assets/globe-outline-white-30.png", "No network selected" );
    
    public static NetworkInformation[] SUPPORTED_NETWORKS = new NetworkInformation[]{ ErgoNetwork.getNetworkInformation()};

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

    public void setCurrentNetwork(String networkId){

        
        if(networkId == null || (m_currentNetworkId != null && m_currentNetworkId.equals(networkId))){
            return;
        }
            
        m_currentNetworkId = networkId;
       

        updateNetworks();
        
        save();
    }

    public String getNetworkId(){
        return m_networkId;
    }

    public ExecutorService getExecService(){
        return m_execService;
    }

    
    public Image getCharacterImage(String characterString){
        return null;
    }




     protected void updateNetworks(){
        
        NoteInterface network = getNetwork(m_currentNetworkId);
        if(network != null){
            if(m_currentNetwork.get() != null && m_currentNetwork.get().getNetworkId().equals(network.getNetworkId())){
                
            }else{
               
                m_currentNetwork.set(network);   
            }

        }else{
            if(m_currentNetwork.get() != null){
                m_currentNetwork.set(null);
            }
        }
    }

    public SimpleObjectProperty<NoteInterface> currentNetworkProperty(){
        return m_currentNetwork;
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

            JsonElement jsonArrayElement = networksObject == null ? null : networksObject.get("networks");
            JsonElement jsonNetArrayElement = networksObject == null ? null : networksObject.get("netArray");
            JsonElement stageElement = networksObject.get("stage");
            JsonElement currentNetworkIdElement = networksObject.get("currentNetworkId");

          
            JsonArray jsonArray = jsonNetArrayElement != null && jsonNetArrayElement.isJsonArray() ? jsonNetArrayElement.getAsJsonArray() : new JsonArray();
            
        
            for (JsonElement element : jsonArray) {
                JsonObject jsonObject = element.getAsJsonObject();
                JsonElement networkIdElement = jsonObject.get("networkId");

                if (networkIdElement != null) {
                    String networkId = networkIdElement.getAsString();

                    switch (networkId) {
                    
                        case ErgoNetwork.NETWORK_ID:
                            addNetwork(new ErgoNetwork(this));
                            break;                          

                    }

                }

            }

            m_currentNetworkId = currentNetworkIdElement != null && currentNetworkIdElement.isJsonPrimitive() ? currentNetworkIdElement.getAsString() : null; 
            
        


            jsonArray = jsonArrayElement != null && jsonArrayElement.isJsonArray() ? jsonArrayElement.getAsJsonArray() : new JsonArray();

          
            for (JsonElement element : jsonArray) {
                JsonObject jsonObject = element.getAsJsonObject();
                JsonElement networkIdElement = jsonObject.get("networkId");

                if (networkIdElement != null) {
                    String networkId = networkIdElement.getAsString();

                    switch (networkId) {
                    
                        case KucoinExchange.NETWORK_ID:
                            addNoteInterface(new KucoinExchange(jsonObject, this), false);
                            break;
                        case SpectrumFinance.NETWORK_ID:
                            addNoteInterface(new SpectrumFinance(jsonObject, this), false);
                            break;
                        

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
          

        }
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

    public HostServices getHostServices() {
        return m_hostServices;
    }

  

    public void clear() {
        for (NoteInterface noteInterface : m_noteInterfaceList) {
            m_noteInterfaceList.remove(noteInterface);
        }

        try {
            save();
        } catch (Exception e) {

        }


    }

    public boolean addNoteInterface(NoteInterface noteInterface) {
        return addNoteInterface(noteInterface, true);
    }

    public boolean addNoteInterface(NoteInterface noteInterface, boolean update) {
        // int i = 0;

        String networkId = noteInterface.getNetworkId();

        if (getNoteInterface(networkId) == null) {
            m_noteInterfaceList.add(noteInterface);
           
          
            return true;
        }
        return false;
    }

    public boolean addNetwork(NoteInterface noteInterface) {
        // int i = 0;

        String networkId = noteInterface.getNetworkId();

        if (getNetwork(networkId) == null) {
            m_networkList.add(noteInterface);
            
     
           
            return true;
        }
        return false;
    }

    public boolean removeNet(String networkId){
        
       
        for (int i = 0; i < m_networkList.size(); i++) {
            NoteInterface noteInterface = m_networkList.get(i);
            if (networkId.equals(noteInterface.getNetworkId())) {
                m_networkList.remove(i);
                noteInterface.remove();

                return true;
            }
        }
     
        return false;
        
    }

    public void addNetworkListListener(ListChangeListener<? super NoteInterface> listener){
        m_networkList.addListener(listener);
    }

    public void removeNetworkListListener(ListChangeListener<? super NoteInterface> listener){
        m_networkList.removeListener(listener);
    }




    public void addNetworkListener(ListChangeListener<? super NoteInterface> listener){
        m_noteInterfaceList.addListener(listener);
    }

    public void removeNetworkListener(ListChangeListener<? super NoteInterface> listener){
        m_noteInterfaceList.removeListener(listener);
    }

  
    private SimpleObjectProperty<LocalDateTime> m_shutdownNow = new SimpleObjectProperty<>(null);

    public SimpleObjectProperty<LocalDateTime> shutdownNowProperty() {
        return m_shutdownNow;
    }

    public void shutdown() {
        m_shutdownNow.set(LocalDateTime.now());

        for (int i = 0; i < m_noteInterfaceList.size(); i++) {
            NoteInterface noteInterface = m_noteInterfaceList.get(i);
            noteInterface.shutdown();

            m_noteInterfaceList.remove(i);
        }

        if (m_noteWatcher != null) {
            m_noteWatcher.shutdown();
        }
        closeNetworksStage();
    }



    public void showManageNetworkStage() {

        if (m_addNetworkStage == null) {
            
            m_installedVBox = new VBox();
            m_installedVBox.prefWidth(m_leftColumnWidth);
            m_installedVBox.setId("bodyBox");
            VBox.setVgrow(m_installedVBox, Priority.ALWAYS);

            m_notInstalledVBox = new VBox();
            m_notInstalledVBox.setId("bodyRight");
            VBox.setVgrow(m_notInstalledVBox, Priority.ALWAYS);
            HBox.setHgrow(m_notInstalledVBox, Priority.ALWAYS);
            updateInstallables();

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
          

           /* VBox installedVBox = new VBox(m_installedVBox, vSpacerOne);
            installedVBox.setId("bodyBox");
            VBox.setVgrow(installedVBox, Priority.ALWAYS);
            HBox.setHgrow(installedVBox,Priority.ALWAYS);*/

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

          /*   VBox rightSidePaddingBox = new VBox(rightSide);
            rightSidePaddingBox.setPadding(new Insets(0, 2, 0, 5));
            HBox.setHgrow(rightSidePaddingBox, Priority.ALWAYS);*/

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
                    installApp(m_focusedInstallable.getNetworkId());
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
                removeAll();
            });

            installAllBtn.setOnAction(e -> {
                addAll();
            });

            m_addNetworkStage.show();
            updateAvailableLists();
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
    }

    public void closeNetworksStage() {
        if (m_addNetworkStage != null) {
            m_addNetworkStage.close();
        }
        m_addNetworkStage = null;
        m_notInstalledVBox = null;
        m_installedVBox = null;
        m_installables = null;
        m_focusedInstallable = null;
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
    }

    public void updateInstallables() {
        m_installables = new ArrayList<>();
        for (String networkId : INTALLABLE_NETWORK_IDS) {
            NoteInterface noteInterface = getNoteInterface(networkId);
            boolean installed = !(noteInterface == null);
            InstallableIcon installableIcon = new InstallableIcon(this, networkId, installed);

            m_installables.add(installableIcon);
        }
    }

   // private SimpleDoubleProperty m_gridWidth = new SimpleDoubleProperty(200);
    


    public void installNetwork(String networkId){
        if(getNetwork(networkId) == null && isNetworkSupported(networkId)){
            switch (networkId) {

            
                case ErgoNetwork.NETWORK_ID:
                try {
                    Files.writeString(App.logFile.toPath(), "\nNetworksData: adding ergo", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
                    addNetwork(new ErgoNetwork(this));
                    break;
            
            }
        }
        save();
    }

    public void installApp(String networkId) {

        switch (networkId) {

           
            case KucoinExchange.NETWORK_ID:
                addNoteInterface(new KucoinExchange(this));
                break;
            case SpectrumFinance.NETWORK_ID:
                addNoteInterface(new SpectrumFinance(this));
                break;
        }
        m_installedVBox.getChildren().clear();
        m_notInstalledVBox.getChildren().clear();
        updateInstallables();
        updateAvailableLists();
        save();
    }

    public void removeApp(String networkId) {
        removeNoteInterface(networkId);

        m_installedVBox.getChildren().clear();
        m_notInstalledVBox.getChildren().clear();
        updateInstallables();
        updateAvailableLists();

        save();
    }

    public void addAll() {
        for (String networkId : INTALLABLE_NETWORK_IDS) {
            if (getNoteInterface(networkId) == null) {
                installApp(networkId);
            }
        }
        updateInstallables();
        updateAvailableLists();
 
        save();
    }

    public void removeAll() {

        while (m_noteInterfaceList.size() > 0) {
            NoteInterface noteInterface = m_noteInterfaceList.get(0);
            m_noteInterfaceList.remove(noteInterface);
            noteInterface.remove();
        }

        updateAvailableLists();
        updateInstallables();

        save();
    }



  
    public boolean removeNoteInterface(String networkId) {
        return removeNoteInterface(networkId, true);
    }

    public boolean removeNoteInterface(String networkId, boolean update) {
        boolean success = false;
        for (int i = 0; i < m_noteInterfaceList.size(); i++) {
            NoteInterface noteInterface = m_noteInterfaceList.get(i);
            if (networkId.equals(noteInterface.getNetworkId())) {
                m_noteInterfaceList.remove(i);
                noteInterface.remove();

                success = true;
                break;
            }
        }
    
        return success;
    }

    public void broadcastNote(JsonObject note) {

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
    }

    public NoteInterface getNetwork(String networkId) {
        if (networkId != null) {
            for (int i = 0; i < m_networkList.size(); i++) {
                NoteInterface noteInterface = m_networkList.get(i);

                if (noteInterface.getNetworkId().equals(networkId)) {
                    return noteInterface;
                }
            }
        }
        return null;
    }

    public TabInterface getNetworkTab(String networkId){
        NoteInterface noteInterface = getNetwork(networkId);
        if(noteInterface != null){
            return noteInterface.getTab(m_appStage, m_heightObject, m_widthObject, m_networkBtn);
        }
        return null;
    }

    public NoteInterface getNoteInterface(String networkId) {
        if (networkId != null) {
            for (int i = 0; i < m_noteInterfaceList.size(); i++) {
                NoteInterface noteInterface = m_noteInterfaceList.get(i);

                if (noteInterface.getNetworkId().equals(networkId)) {
                    return noteInterface;
                }
            }
        }
        return null;
    }

    public void sendNoteToNetworkId(JsonObject note, String networkId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        m_noteInterfaceList.forEach(noteInterface -> {
            if (noteInterface.getNetworkId().equals(networkId)) {

                noteInterface.sendNote(note, onSucceeded, onFailed);
            }
        });
    }

    private JsonObject getJsonObject(){
        JsonObject fileObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        JsonArray netArray = new JsonArray();
        for (NoteInterface noteInterface : m_noteInterfaceList) {

            JsonObject jsonObj = noteInterface.getJsonObject();
            jsonArray.add(jsonObj);

        }
        for (NoteInterface noteInterface : m_networkList){
            JsonObject jsonObj = noteInterface.getJsonObject();
            netArray.add(jsonObj);
        }
        fileObject.addProperty("currentNetworkId", m_currentNetworkId);
        fileObject.add("netArray", netArray);
        fileObject.add("networks", jsonArray);
        fileObject.add("stage", getStageJson());
        return fileObject;
    }
  

    public void save() {
       
        save("data", ".", "main","root", getJsonObject());

    }
   


    public void open(String networkId, String type){
       // NoteInterface noteInterface = getNoteInterface(networkId);

      //  noteInterface.getPane();
        String currentTabId = m_currentTab.get() != null ? m_currentTab.get().getTabId() : null;

        if(type == null || networkId == null || (currentTabId != null &&  currentTabId.equals(networkId))){
            return;
        }
      
        TabInterface tab = null;
        switch(type){
            case App.STATIC_TYPE:
                tab = getStaticTab(networkId);
            break;
            case App.NETWORK_TYPE:
                tab = getNetworkTab(networkId);
            break;
        }
 
        m_currentTab.set(tab != null ? tab : null);
        
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
                m_networkTab = m_networkTab == null ? new NetworkTab(m_appStage, this,m_heightObject, m_widthObject, m_networkBtn) : m_networkTab;
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
        return m_currentTab;
    };


    private VBox m_menuBox = null;
    private ObservableList<TabInterface> m_appTabs = FXCollections.observableArrayList();
    private VBox m_appTabsBox;
    private VBox m_menuContentBox;

    private NoteMsgInterface m_networkMsgInterface = null;
 
 
    private void initMenu(){
        double menuSize = 30;

        m_networkToolTip.setShowDelay(new javafx.util.Duration(100));


        Tooltip appsToolTip = new Tooltip("Apps");
        appsToolTip.setShowDelay(new javafx.util.Duration(100));


        ImageView appsImgView = new ImageView();
        appsImgView.setImage(new Image("/assets/apps-outline-35.png"));
        appsImgView.setFitHeight(menuSize);
        appsImgView.setPreserveRatio(true);

        m_appsBtn.setGraphic(appsImgView);
        m_appsBtn.setId("menuTabBtn");
        m_appsBtn.setTooltip(appsToolTip);

        ImageView settingsImageView = new ImageView();
        settingsImageView.setImage(App.settingsImg);
        settingsImageView.setFitHeight(menuSize);
        settingsImageView.setPreserveRatio(true);

        Tooltip settingsTooltip = new Tooltip("Settings");
        settingsTooltip.setShowDelay(new javafx.util.Duration(100));

        m_settingsBtn.setGraphic(settingsImageView);
        m_settingsBtn.setId("menuTabBtn");
        m_settingsBtn.setTooltip(settingsTooltip);

        
        m_networkImgView.setImage(App.globeImage30);
        m_networkImgView.setPreserveRatio(true);
        m_networkImgView.setFitWidth(menuSize);

        m_networkBtn.setGraphic(m_networkImgView);
        m_networkBtn.setId("menuTabBtn");
        m_networkBtn.setTooltip(m_networkToolTip);
  
       

        m_appTabsBox = new VBox();
        m_menuContentBox = new VBox(m_appTabsBox);


    }
    private ImageView m_selectNewtorkImgView;
    private HBox m_selectNetworkBox;
    private StackPane m_prevPositionBox;

    public void createMenu(Stage appStage,VBox menuBox,ScrollPane menuScroll,ScrollPane subMenuScroll, VBox contentBox){
        m_appStage = appStage;
        m_subMenuScroll = subMenuScroll;
        m_boundsListener = (obs,oldval,newval)->{
            m_widthObject.set(newval.getWidth());
            m_heightObject.set(newval.getHeight());
        };    
        m_subMenuScroll.viewportBoundsProperty().addListener(m_boundsListener);

        m_subMenuBox.setAlignment(Pos.TOP_LEFT);

        HBox vBar = new HBox();
        vBar.setAlignment(Pos.CENTER);
        vBar.setId("vGradient");
        vBar.setMinWidth(1);
        VBox.setVgrow(vBar, Priority.ALWAYS);
        
        initMenu();

        
        m_menuScroll = menuScroll;
        
        int imgSize = 17;
        m_selectNewtorkImgView = new ImageView();
        m_selectNewtorkImgView.setPreserveRatio(true);
        m_selectNewtorkImgView.setFitWidth(imgSize);

        
     
        m_selectNetworkBox = new HBox();
        m_selectNetworkBox.setMinWidth(imgSize);
        m_selectNetworkBox.setMinHeight(imgSize);
        m_selectNetworkBox.setMaxWidth(imgSize);
        m_selectNetworkBox.setMaxHeight(imgSize);
        m_selectNetworkBox.setMouseTransparent(true);


      

        HBox socketBox = new HBox();
        // fx-background-color:radial-gradient(radius 100%, #33333350 2%, #00000000);
        socketBox.setId("socketBox");
        VBox.setVgrow(socketBox, Priority.ALWAYS);
        HBox.setHgrow(socketBox, Priority.ALWAYS);
        socketBox.setMouseTransparent(true);
        socketBox.setMaxWidth(32);
        socketBox.setMaxHeight(30);
        //socketBox.addEventFilter(null, null);
   

        StackPane btnPos = new StackPane(m_networkBtn,socketBox);
        HBox.setHgrow(btnPos, Priority.ALWAYS);
        btnPos.setAlignment(Pos.CENTER);

        m_prevPositionBox = new StackPane(btnPos, m_selectNewtorkImgView, m_selectNetworkBox);

        m_prevPositionBox.setAlignment(Pos.BOTTOM_RIGHT);

     
        menuBox.getChildren().addAll(m_appsBtn, m_menuScroll, m_settingsBtn, m_prevPositionBox);
        
     
        m_appsBtn.setOnAction(e -> {
    
        });
    
        m_settingsBtn.setOnAction(e -> {
            open(SettingsTab.NAME, App.STATIC_TYPE);
        });

        m_networkBtn.setOnAction(e->{
            if(currentNetworkProperty().get() == null){
                open(NetworkTab.NAME, App.STATIC_TYPE);
            }else{
                open(currentNetworkProperty().get().getNetworkId(), App.NETWORK_TYPE);
            }
        });

        EventHandler<MouseEvent> selectNetworkBoxOnClick = (e)->{
            open(NetworkTab.NAME, App.STATIC_TYPE);
        };



        m_currentNetwork.addListener((obs,oldval,newval)->{
            if(oldval != null){
                if(m_networkMsgInterface != null){
                    oldval.removeMsgListener(m_networkMsgInterface);
                    m_networkMsgInterface = null;
                }
            }
            
            if(newval != null){

                m_networkMsgInterface = new NoteMsgInterface() {
                    private String m_msgId = FriendlyId.createFriendlyId();
                    public String getId(){
                        return m_msgId;
                    }
                    public void sendMessage(String networkId, int code, long timestamp){

                    }
                    public void sendMessage(int code, long timestamp){

                    }
                    public void sendMessage(int code, long timestamp, String msg){

                    }
                    public void sendMessage(String networkId, int code, long timestamp, String msg){

                    }
                    public void sendMessage(String networkId, int code, long timestamp, JsonObject json){
                    }
                };

                newval.addMsgListener(m_networkMsgInterface);
                m_networkImgView.setImage(newval.getSmallAppIcon());
                m_networkToolTip.setText("Network: " + newval.getName());

                m_selectNetworkBox.setId("socketBox");
                m_selectNewtorkImgView.setImage(App.globeImage30);

                m_selectNetworkBox.setMouseTransparent(false);
                m_selectNetworkBox.addEventFilter(MouseEvent.MOUSE_CLICKED, selectNetworkBoxOnClick);

            }else{
                m_selectNetworkBox.setMouseTransparent(true);
                m_selectNetworkBox.setId(null);
                m_selectNewtorkImgView.setImage(null);

                m_networkImgView.setImage(App.globeImage30);
                m_networkToolTip.setText("Network: Select");
                m_selectNetworkBox.removeEventFilter(MouseEvent.MOUSE_CLICKED, selectNetworkBoxOnClick);

            }
        });


        
        m_currentTab.addListener((obs,oldval,newval)->{

            if(oldval != null){
                appStage.titleProperty().unbind();
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

            if(newval != null && newval instanceof Pane){
                m_subMenuBox.getChildren().addAll((Pane) newval, vBar);
                m_subMenuScroll.setContent( m_subMenuBox );

                newval.setCurrent(true);
                appStage.titleProperty().unbind();
                m_titleExpression = Bindings.concat("Netnotes - ", newval.titleProperty());
                appStage.titleProperty().bind(m_titleExpression);
            }else{
                  m_subMenuScroll.setContent(null);
                appStage.setTitle("Netnotes");
            }
          

        }); 
        
        updateNetworks();   
       
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
                    
                }

                Utils.saveJson(getAppData().appKeyProperty().get(), json, idDataFile);
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

        Text passwordTxt = new Text("〉 Enter password:");
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

}
