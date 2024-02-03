package com.netnotes;

import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.reactfx.util.FxTimer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.netnotes.IconButton.IconStyle;
import com.satergo.extra.AESEncryption;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class NetworksData implements InstallerInterface {

    public final static long WATCH_INTERVAL = 50;
    public final String INPUT_EXT = ".in";
    public final String OUT_EXT = ".out";

    public final static String[] INTALLABLE_NETWORK_IDS = new String[]{
        ErgoNetwork.NETWORK_ID,
        KucoinExchange.NETWORK_ID,
        SpectrumFinance.NETWORK_ID
        
    };

    private ArrayList<NoteInterface> m_noteInterfaceList = new ArrayList<>();
    private File m_networksFile;
    private String m_selectedId;
    private VBox m_networksBox;

    

    private double m_leftColumnWidth = 175;

    private VBox m_installedVBox = null;
    private VBox m_notInstalledVBox = null;
    private ArrayList<InstallableIcon> m_installables = new ArrayList<>();

    private Stage m_addNetworkStage = null;

    private InstallableIcon m_focusedInstallable = null;

    private Rectangle m_rect;
    private HostServices m_hostServices;
  

    private File m_notesDir;
    private File m_outDir;
    private SimpleObjectProperty<com.grack.nanojson.JsonObject> m_cmdSwitch = new SimpleObjectProperty<com.grack.nanojson.JsonObject>(null);

    private NoteWatcher m_noteWatcher = null;

    private File logFile = new File("netnotes-log.txt");
    private SimpleStringProperty m_stageIconStyle = new SimpleStringProperty(IconStyle.ICON);

    private double m_stageWidth = 700;
    private double m_stageHeight = 500;
    private double m_stagePrevWidth = 310;
    private double m_stagePrevHeight = 500;
    private boolean m_stageMaximized = false;
    private AppData m_appData;
    
    private ScheduledExecutorService m_timeExecutor = null;
    private ScheduledFuture<?> m_lastExecution = null;
    private final SimpleObjectProperty<LocalDateTime> m_timeCycle = new SimpleObjectProperty<>(LocalDateTime.now());
    private long m_cyclePeriod = 30;
    private TimeUnit m_cycleTimeUnit = TimeUnit.SECONDS;

    public NetworksData(AppData appData,  HostServices hostServices, File networksFile, boolean isFile) {
        m_appData = appData;
       
        m_networksFile = networksFile;
        m_networksBox = new VBox();
        m_hostServices = hostServices;

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        m_rect = ge.getMaximumWindowBounds();
 
        

        try {
            
            InputStream stream = App.class.getResource("/assets/OCRAEXT.TTF").openStream();
            java.awt.Font ocrFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, stream).deriveFont(48f);
            ge.registerFont(ocrFont);
            stream.close();
            
 
            
        } catch (FontFormatException | IOException e) {

            try {
                Files.writeString(logFile.toPath(), "\nError registering font: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {
            
            }

        } 

        if (isFile) {
            readFile(m_appData.appKeyProperty().get(), networksFile.toPath());
        }

        m_notesDir = new File(m_appData.getAppDir().getAbsolutePath() + "/notes");
        m_outDir = new File(m_appData.getAppDir().getAbsolutePath() + "/out");
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

        try {
            m_noteWatcher = new NoteWatcher(m_notesDir, new NoteListener() {
                public void onNoteChange(String fileString) {
                    checkFile(new File(fileString));
                }
            });
        } catch (IOException e) {

        }

        setupTimer();
        m_appData.appKeyProperty().addListener((obs,oldval,newval)->save());
    }

    private void readFile(SecretKey appKey, Path filePath) {

        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(filePath);

            byte[] iv = new byte[]{
                fileBytes[0], fileBytes[1], fileBytes[2], fileBytes[3],
                fileBytes[4], fileBytes[5], fileBytes[6], fileBytes[7],
                fileBytes[8], fileBytes[9], fileBytes[10], fileBytes[11]
            };

            ByteBuffer encryptedData = ByteBuffer.wrap(fileBytes, 12, fileBytes.length - 12);

            try {
                JsonElement jsonElement = new JsonParser().parse(new String(AESEncryption.decryptData(iv, appKey, encryptedData)));
                if (jsonElement != null && jsonElement.isJsonObject()) {
                    openJson(jsonElement.getAsJsonObject());
                }
            } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                    try {
                        Files.writeString(logFile.toPath(), "\nNetworksData: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e1) {

                    }
            }

        } catch (IOException e) {
            try {
                Files.writeString(logFile.toPath(), "\nNetworks Data:" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
        }

    }

     public void setupTimer() {

        if (m_lastExecution != null) {
            m_lastExecution.cancel(false);
        }

        if (m_timeExecutor != null) {
            m_timeExecutor.shutdownNow();
            m_timeExecutor = null;
        }

        if (getCyclePeriod() > 0) {
            m_timeExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });

            Runnable doUpdate = () -> {
                 updateTimeCycle();
            };

            m_lastExecution = m_timeExecutor.scheduleAtFixedRate(doUpdate, 0, getCyclePeriod(), getCycleTimeUnit());
        }
    }

    public void updateTimeCycle() {
        Platform.runLater(() ->m_timeCycle.set(LocalDateTime.now()));
    }

    public SimpleObjectProperty<LocalDateTime> timeCycleProperty() {
        return m_timeCycle;
    }

    public long getCyclePeriod(){
        return m_cyclePeriod;
    }

    public TimeUnit getCycleTimeUnit(){
        return m_cycleTimeUnit;
    }

  

    public AppData getAppData() {
        return m_appData;
    }

    private void openJson(JsonObject networksObject) {
        if (networksObject != null) {

            JsonElement jsonArrayElement = networksObject == null ? null : networksObject.get("networks");
            JsonElement stageElement = networksObject.get("stage");
            JsonArray jsonArray = jsonArrayElement.getAsJsonArray();

            for (JsonElement element : jsonArray) {
                JsonObject jsonObject = element.getAsJsonObject();
                JsonElement networkIdElement = jsonObject.get("networkId");

                if (networkIdElement != null) {
                    String networkId = networkIdElement.getAsString();

                    switch (networkId) {
                        case ErgoNetwork.NETWORK_ID:
                            addNoteInterface(new ErgoNetwork(jsonObject, this), false);
                            break;

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
                JsonElement stageWidthElement = stageObject.get("width");
                JsonElement stageHeightElement = stageObject.get("height");
                JsonElement stagePrevWidthElement = stageObject.get("prevWidth");
                JsonElement stagePrevHeightElement = stageObject.get("prevHeight");

                JsonElement iconStyleElement = stageObject.get("iconStyle");
                JsonElement stageMaximizedElement = stageObject.get("maximized");

                boolean maximized = stageMaximizedElement == null ? false : stageMaximizedElement.getAsBoolean();
                String iconStyle = iconStyleElement != null ? iconStyleElement.getAsString() : IconStyle.ICON;

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
            updateNetworksGrid();

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
        json.addProperty("maximized", getStageMaximized());
        json.addProperty("width", getStageWidth());
        json.addProperty("height", getStageHeight());
        json.addProperty("prevWidth", getStagePrevWidth());
        json.addProperty("prevHeight", getStagePrevHeight());
        json.addProperty("iconStyle", m_stageIconStyle.get());
        return json;
    }

    public SimpleObjectProperty<com.grack.nanojson.JsonObject> cmdSwitchProperty() {
        return m_cmdSwitch;
    }

    public File getAppDir() {
        return m_appData.getAppDir();
    }

    public HostServices getHostServices() {
        return m_hostServices;
    }

    public Rectangle getMaximumWindowBounds() {
        return m_rect;
    }

    public void clear() {
        for (NoteInterface noteInterface : m_noteInterfaceList) {
            m_noteInterfaceList.remove(noteInterface);
        }

        try {
            save();
        } catch (Exception e) {

        }
        updateNetworksGrid();

    }

    public boolean addNoteInterface(NoteInterface noteInterface) {
        return addNoteInterface(noteInterface, true);
    }

    public boolean addNoteInterface(NoteInterface noteInterface, boolean update) {
        // int i = 0;

        String networkId = noteInterface.getNetworkId();

        if (getNoteInterface(networkId) == null) {
            m_noteInterfaceList.add(noteInterface);
            noteInterface.addUpdateListener((obs, oldValue, newValue) -> save());

            if (update) {
                updateNetworksGrid();
            }
            return true;
        }
        return false;
    }

    public VBox getNetworksBox() {

        updateNetworksGrid();
        m_stageIconStyle.addListener((obs, oldVal, newVal) -> {
            updateNetworksGrid();
            save();
        });

        return m_networksBox;
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

    public void show() {
  
        
        com.grack.nanojson.JsonObject showJson = new com.grack.nanojson.JsonObject();

        showJson.put("type", "CMD");
        showJson.put("cmd", App.CMD_SHOW_APPSTAGE);
        showJson.put("timeStamp", System.currentTimeMillis());


        m_cmdSwitch.set(showJson);
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

            String topTitle = "Manage - Netnotes: Networks";
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
                    installNetwork(m_focusedInstallable.getNetworkId());
                }
                m_focusedInstallable = null;
            });

            removeBtn.setOnAction(e -> {
                if (m_focusedInstallable != null && (m_focusedInstallable.getInstalled())) {
                    removeNetwork(m_focusedInstallable.getNetworkId());
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
            FxTimer.runLater(Duration.ofMillis(100), ()->{
                Platform.runLater(()->m_addNetworkStage.requestFocus());
                Platform.runLater(()->m_addNetworkStage.toFront());
            });
       
        } else {
             Platform.runLater(()->{
                m_addNetworkStage.show();
                m_addNetworkStage.toFront();
                m_addNetworkStage.requestFocus();
            });
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

    private SimpleDoubleProperty m_gridWidth = new SimpleDoubleProperty(200);
    

    public void updateNetworksGrid() {
        m_networksBox.getChildren().clear();
        double minSize = m_stageWidth - 110;

        int numCells = m_noteInterfaceList.size();
        double width = m_networksBox.widthProperty().get();
        width = width < minSize ? minSize : width;
        
        String currentIconStyle = m_stageIconStyle.get();

        if (numCells != 0) {

            m_networksBox.setAlignment(Pos.TOP_LEFT);
            if (currentIconStyle.equals(IconStyle.ROW)) {
                for (int i = 0; i < numCells; i++) {
                    NoteInterface network = m_noteInterfaceList.get(i);
                    IconButton iconButton = network.getButton(currentIconStyle);
                    iconButton.setPrefWidth(width);
                    m_networksBox.getChildren().add(iconButton);
                }
            } else {

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
  
              for (NoteInterface noteInterface : m_noteInterfaceList) {
                  if(rowsBoxes.size() < (grid.getJ() + 1)){
                      HBox newHBox = new HBox();
                      rowsBoxes.add(newHBox);
                      m_networksBox.getChildren().add(newHBox);
                  }
                  HBox rowBox = rowsBoxes.get(grid.getJ());
                  rowBox.getChildren().add(noteInterface.getButton(currentIconStyle));
  
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

    public void installNetwork(String networkId) {

        switch (networkId) {

            case ErgoNetwork.NETWORK_ID:
                addNoteInterface(new ErgoNetwork(this));
                break;
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

    public void removeNetwork(String networkId) {
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
                installNetwork(networkId);
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



    public void setSelected(String networkId) {
        if (m_selectedId != null) {
            NoteInterface prevInterface = getNoteInterface(m_selectedId);
            if (prevInterface != null) {
                prevInterface.getButton().setCurrent(false);
            }
        }
        m_selectedId = networkId;
        if (networkId != null) {
            NoteInterface currentInterface = getNoteInterface(networkId);
            if (currentInterface != null) {
                currentInterface.getButton().setCurrent(true);
            }
        }
    }

    public String getSelected() {
        return m_selectedId;
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
        if (success && update) {
            updateNetworksGrid();
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

  

    public void save() {
        JsonObject fileObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();

        for (NoteInterface noteInterface : m_noteInterfaceList) {

            JsonObject jsonObj = noteInterface.getJsonObject();
            jsonArray.add(jsonObj);

        }

        fileObject.add("networks", jsonArray);
        fileObject.add("stage", getStageJson());

        String jsonString = fileObject.toString();

   

        try {

            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            byte[] iV = new byte[12];
            secureRandom.nextBytes(iV);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);

            cipher.init(Cipher.ENCRYPT_MODE, getAppData().appKeyProperty().get(), parameterSpec);

            byte[] encryptedData = cipher.doFinal(jsonString.getBytes());

            try {

                if (m_networksFile.isFile()) {
                    Files.delete(m_networksFile.toPath());
                }

                FileOutputStream outputStream = new FileOutputStream(m_networksFile);
                FileChannel fc = outputStream.getChannel();

                ByteBuffer byteBuffer = ByteBuffer.wrap(iV);

                fc.write(byteBuffer);

                int written = 0;
                int bufferLength = 1024 * 8;

                while (written < encryptedData.length) {

                    if (written + bufferLength > encryptedData.length) {
                        byteBuffer = ByteBuffer.wrap(encryptedData, written, encryptedData.length - written);
                    } else {
                        byteBuffer = ByteBuffer.wrap(encryptedData, written, bufferLength);
                    }

                    written += fc.write(byteBuffer);
                }

                outputStream.close();

            } catch (IOException e) {
                try {
                    Files.writeString(logFile.toPath(), "\nIO exception:" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
            }

        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
            try {
                Files.writeString(logFile.toPath(), "\nKey error: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
        }

    }

    public String getAckString(String id, long timeStamp) {
        return "{\"id\": \"" + id + "\", \"type\": \"ack\", \"timeStamp\": " + timeStamp + "}";
    }

    private void noteIn(String id, String body, File inFile) {
        switch (body) {
            case App.CMD_SHOW_APPSTAGE:
                show();

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

    }

}
