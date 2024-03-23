package com.netnotes;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.ergoplatform.appkit.NetworkType;

import com.utils.GitHubAPI;
import com.utils.Utils;
import com.utils.Version;
import com.utils.GitHubAPI.GitHubAsset;

import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import com.google.gson.JsonObject;
import com.netnotes.ErgoNodeConfig.BlockchainMode;
import com.netnotes.ErgoNodeConfig.DigestAccess;
import com.netnotes.ErgoNodeConfig.ConfigMode;
import com.google.gson.JsonElement;

public class ErgoNodeLocalData extends ErgoNodeData {

    public final static String DESCRIPTION = "Install and manage an Ergo Node locally.";

    public final static String DEFAULT_NODE_NAME = "Local Node";
    public final static String DEFAULT_CONFIG_NAME = "ergo.conf";
    public final static int MAX_CONSOLE_ROWS = 200;
    public final static int MAX_INPUT_BUFFER_SIZE = 30;
    public final static int SYNC_TIMEOUT_CYCLE = 15;

    public final static int DEFAULT_MEM_GB_REQUIRED = 6;

    public final static String STARTUP_STRING = "Starting up...";


    final private List<ErgoNodeMsg> m_nodeMsgBuffer = Collections.synchronizedList(new ArrayList<ErgoNodeMsg>());

    private static File logFile = new File("netnotes-log.txt");

 
    private String m_setupImgUrl = "/assets/open-outline-white-20.png";

    private boolean m_runOnStart = false;

    private File m_appDir = null;
    private String m_appFileName = null;
    private HashData m_appFileHashData = null;
    private String m_execParams = "";

    private long m_spaceRequired = 50L * (1024L * 1024L * 1024L);

    private int m_memGBRequired = DEFAULT_MEM_GB_REQUIRED;

    private Stage m_stage = null;
    private Stage m_configStage = null;
    public final static long EXECUTION_TIME = 500;

    public double SETUP_STAGE_WIDTH = 700;
    public double SETUP_STAGE_HEIGHT = 520;

    public double CORE_SETUP_STAGE_WIDTH = 700;
    public double CORE_SETUP_STAGE_HEIGHT = 460;

    public double SETTINGS_STAGE_WIDTH = 1024;
    public double SETTINGS_STAGE_HEIGHT = 800;

    private Stage m_settingsStage = null;

    public long INPUT_CYCLE_PERIOD = 100;

    private ErgoNodeConfig m_nodeConfigData = null;

    private ExecutorService m_executor = null;
   
    private Future<?> m_future = null;
    private ScheduledExecutorService m_schedualedExecutor = null;
    private ScheduledFuture<?> m_scheduledFuture = null;
    private SimpleStringProperty m_consoleOutputProperty = new SimpleStringProperty("");

    private final SimpleBooleanProperty m_isSetupProperty = new SimpleBooleanProperty(false);

    private final SimpleLongProperty m_nodeHeadersHeightProperty = new SimpleLongProperty(-1);
    private final SimpleLongProperty m_nodeBlockHeightProperty = new SimpleLongProperty(-1);
    private final SimpleLongProperty m_networkBlockHeightProperty = new SimpleLongProperty(-1);
    private final SimpleIntegerProperty m_peerCountProperty = new SimpleIntegerProperty(-1);

    private final SimpleStringProperty m_warningsProperty = new SimpleStringProperty("");

    private SimpleObjectProperty<Version> m_appVersion = new SimpleObjectProperty<Version>(null);

    private AtomicReference<String> m_lastNodeMsgId = new AtomicReference<>(null);
    private AtomicInteger m_inputCycleIndex = new AtomicInteger(0);
    private AtomicInteger m_isSyncStuck = new AtomicInteger(-1);
    private final AtomicBoolean isGettingInfo = new AtomicBoolean(false);
    private boolean m_update = false;
    private boolean m_autoUpdate = false;
    private boolean m_deleteOldFiles = true;
    private final AtomicBoolean m_checking =  new AtomicBoolean(false);


    private GitHubAPI m_gitHubAPI = new GitHubAPI("ergoplatform", "ergo");


    /*private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);

            return t;
        }
    });*/

    public ErgoNodeLocalData(String id, ErgoNodesList ergoNodesList) {
        super(ergoNodesList, LOCAL_NODE, new NamedNodeUrl(id, "Not Installed", "127.0.0.1", ErgoNodes.MAINNET_PORT, "", NetworkType.MAINNET));
        m_update = ergoNodesList.getErgoNodes().getNetworksData().getAppData().getUpdates();
        setListeners();
    
    }

    public ErgoNodeLocalData(NamedNodeUrl namedNode, JsonObject json, ErgoNodesList ergoNodesList) {
        super(ergoNodesList, LOCAL_NODE, namedNode);
        m_update = ergoNodesList.getErgoNodes().getNetworksData().getAppData().getUpdates();
        openJson(json);
        setListeners();
        if (m_runOnStart) {
            start();
        }
    }



    private void setListeners() {
        
        getNetworksData().shutdownNowProperty().addListener((obs, oldVal, newVal) -> stop());
        // m_syncedProperty.bind(Bindings.equal(m_nodeBlockHeightProperty, m_networkBlockHeightProperty).and(m_networkBlockHeightProperty.isNotEqualTo(-1L).and(statusProperty.isNotEqualTo(MarketsData.STOPPED))));



        Runnable updateSynced = () -> {
            String status = statusProperty().get() == null ? ErgoMarketsData.STOPPED : statusProperty().get();
            long networkBlockHeight = m_networkBlockHeightProperty.get();
            long nodeBlockHeight = m_nodeBlockHeightProperty.get();
            boolean running = !status.equals(ErgoMarketsData.STOPPED);
            boolean synced = nodeBlockHeight != -1 && networkBlockHeight != -1 && nodeBlockHeight >= networkBlockHeight && running;
            
            isAvailableProperty().set(synced);

            if (running) {

                long headersHeight = m_nodeHeadersHeightProperty.get();

                if (!synced) {

                    //    botMidText.setFill(getSecondaryColor());
      
                    if (networkBlockHeight == -1) {
                        statusString().set(STARTUP_STRING);
                    } else {

                        if (nodeBlockHeight == -1) {
                            if(headersHeight > -1){
                                String headersHeightString = headersHeight == -1 ? "Getting headers..." : headersHeight + "";
                                String networkBlockHeightString = networkBlockHeight == -1 ? "Getting Network Height..." : networkBlockHeight + "";
                                statusString().set("Syncing headers: " + headersHeightString + "/" + networkBlockHeightString);
                                
                            }else{
                                //int seconds = (SYNC_TIMEOUT_CYCLE - m_isSyncStuck.get());
                                statusString().set("Finding next block... " + (m_isSyncStuck.get() > 2 ? (" (Timing out...") : ""));
                            } 
                        } else {
                            String nodeBlockHeightString = nodeBlockHeight == -1 ? "Getting Node Height..." : nodeBlockHeight + "";
                            String networkBlockHeightString = networkBlockHeight == -1 ? "Getting Network Height..." : networkBlockHeight + "";

                            statusString().set("Syncing: " + nodeBlockHeightString + "/" + networkBlockHeightString);
                        }

                    }

                    //+ " (" + String.format("%.1f", p * 100) + ")");
                    // }
                } else {

                    statusString().set("Ready");
                  
                }
                
            } else {

                statusString().set(getIsSetup() ? "Offline" : "(Not Installed)");

            }

        };

        m_nodeHeadersHeightProperty.addListener((obs,oldval,newval)->updateSynced.run());
        m_nodeBlockHeightProperty.addListener((obs, oldVal, newVal) -> updateSynced.run());
        m_networkBlockHeightProperty.addListener((obs, oldval, newVal) -> updateSynced.run());
        
        updateSynced.run();


       m_msgInput.addListener((obs,oldval,newval)->{
            ErgoNodeMsg[] newMsgs = newval;
            long blockHeight = m_nodeBlockHeightProperty.get();
            long headersHeight = m_nodeHeadersHeightProperty.get();
            int size = newMsgs.length;
           
            long newNodeHeight = blockHeight;
            long newheadersHeight = headersHeight;

            for (int i = 0; i < size; i++) {
                ErgoNodeMsg msg = newMsgs[i];
    
                if(msg.getLocalTime() != null){
                    String body = msg.getBody();
                    String type = msg.getType();
                    //  Platform.runLater(() -> {
                      //  m_nodeMsg.set(msg.getBody());
                        cmdProperty().set(type);
        
                        if (type.equals(ErgoNodeMsg.MsgTypes.NEW_HEIGHT)) {
                            
                            long msgBlockHeight = msg.getHeight();
                            if(msgBlockHeight > newNodeHeight){
                               // m_nodeBlockHeightProperty.set(msg.getHeight());
                                newNodeHeight = msgBlockHeight;
                            }
                        }
                        if (type.equals(ErgoNodeMsg.MsgTypes.NEW_HEADER_HEIGHT)) {
                            long msgheadersHeight = msg.getHeight();
                            
                            if(msgheadersHeight > newheadersHeight){
                                newheadersHeight = msgheadersHeight;
                            }
                            //m_nodeHeadersHeightProperty.set(msg.getHeight());
                        }
                        
                        
                //    });
                }
            }
            ErgoNodeMsg lastMsg = newMsgs[newMsgs.length-1];
            m_nodeBlockHeightProperty.set(newNodeHeight);
            m_nodeHeadersHeightProperty.set(newheadersHeight);
            cmdStatusUpdated().set(String.format("%29s", lastMsg.getDateTimeString()));
        });
    }

    public int getMemGBRequired(){
        return m_memGBRequired;
    }

    public void setMemGBRequired(int memGBrequired){
        m_memGBRequired = memGBrequired;
    }

    public void coreFileError() {
        Alert a = new Alert(AlertType.WARNING, "Local node core file has been altered.", ButtonType.OK);
        a.setHeaderText("Error: Core Mismatch");
        a.setTitle("Error: Core File Mistmatch - Local Node - Ergo Nodes");
        a.show();

    }

    public void configFileError(boolean updated) {
        Alert a = new Alert(AlertType.WARNING, "Local node config file has been altered and " + (updated ? "has been reverted to the last saved file." : " cannot be recoverd. Node setup will be required."), ButtonType.OK);
        a.setHeaderText("Config Mismatch");
        a.setTitle("Error: Config Mistmatch - Local Node - Ergo Nodes");
        a.show();

    }

    public SimpleStringProperty warningsProperty(){
        return m_warningsProperty;
    }

    @Override
    public void openJson(JsonObject jsonObj) {

        if (jsonObj != null) {
            
 

        
            JsonElement runOnStartElement = jsonObj.get("runOnStart");
            JsonElement updateElement = jsonObj.get("update");
            JsonElement autoUpdateElement = jsonObj.get("autoUpdate");
            JsonElement appDirElement = jsonObj.get("appDir");
            JsonElement appFileNameElement = jsonObj.get("appFileName");
            JsonElement appFileHashDataElement = jsonObj.get("appFileHashData");
            JsonElement appExecParamsElement = jsonObj.get("appExecParams");
            JsonElement appVersionElement = jsonObj.get("appVersion");
            JsonElement configElement = jsonObj.get("config");

            String appDirString = appDirElement != null && appDirElement.isJsonPrimitive()  ? appDirElement.getAsString() : null;
           

            if(Utils.findPathPrefixInRoots(appDirString)){
                 m_appDir = appDirString != null ? new File(appDirString) : null;
                
                m_runOnStart = runOnStartElement != null && runOnStartElement.isJsonPrimitive() ? runOnStartElement.getAsBoolean() : false;
                m_appFileHashData = null;
                m_nodeConfigData = null;
            
                m_autoUpdate = autoUpdateElement != null && autoUpdateElement.isJsonPrimitive() ? autoUpdateElement.getAsBoolean() : false;

                JsonObject configJson = configElement != null && configElement.isJsonObject() ? configElement.getAsJsonObject() : null;

                String appFileName = m_appDir != null && appFileNameElement != null && appFileNameElement.isJsonPrimitive() ? appFileNameElement.getAsString() : null;
                File appFile = appFileName != null ? new File(m_appDir.getAbsolutePath() + "/" + appFileName) : null;

                if (appFile != null && appFile.canRead() && appFile.isFile() && appFileHashDataElement != null && appFileHashDataElement.isJsonObject() && configElement != null && configElement.isJsonObject()) {
                    m_appFileName = appFileName;
                    boolean isCorrectHash = false;

                    try {
                        m_appFileHashData = new HashData(appFileHashDataElement.getAsJsonObject());
                        m_appVersion.set(appVersionElement != null && appVersionElement.isJsonPrimitive() ? new Version(appVersionElement.getAsString()) : null);

                        HashData appFileHashData = new HashData(appFile);
                        /*
                        Files.writeString(logFile.toPath(), "\nAppfiledata: " +appFileHashData.getJsonObject().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        */
                        isCorrectHash = m_appFileHashData.getHashStringHex().equals(appFileHashData.getHashStringHex());

                    } catch (Exception e) {
                        try {
                            Files.writeString(logFile.toPath(), "\n" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e1) {

                        }
                    }

                    if (isCorrectHash) {

                        try {
                            m_nodeConfigData = new ErgoNodeConfig(configJson, m_appDir);

                            if (m_nodeConfigData != null && m_nodeConfigData.getConfigFile() != null && m_nodeConfigData.getConfigFile().isFile() && m_nodeConfigData.getConfigFileHashData() != null) {

                                HashData configFileHashData = new HashData(m_nodeConfigData.getConfigFile());

                                String nodeConfigHash = m_nodeConfigData.getConfigFileHashData().getHashStringHex();
                                if (nodeConfigHash.equals(configFileHashData.getHashStringHex())) {

                                    if (m_runOnStart) {
                                        runNode(m_nodeConfigData.getConfigFile(), appFile);
                                    }
                                } else {

                                    m_runOnStart = false;

                                    configFileError(true);
                                    m_nodeConfigData.updateConfigFile();
                                }

                            } else {

                                m_runOnStart = false;

                                configFileError(true);
                                m_nodeConfigData.updateConfigFile();
                            }
                        } catch (Exception e) {
                            m_runOnStart = false;
                            m_isSetupProperty.set(false);
                            configFileError(false);

                        }
                        m_isSetupProperty.set(true);
                    } else {
                        m_runOnStart = false;
                        m_appFileHashData = null;
                        m_nodeConfigData = null;

                        if (m_isSetupProperty.get()) {
                            m_isSetupProperty.set(false);
                            coreFileError();
                        }
                    }

                }

                if (appExecParamsElement != null && appExecParamsElement.isJsonPrimitive()) {
                    m_execParams = appExecParamsElement.getAsString();
                }

            }else{
                isSetupProperty().set(false);
            }
        }

    }

    public SimpleObjectProperty<Version> appVersionProperty() {
        return m_appVersion;
    }

    public SimpleBooleanProperty isSetupProperty() {
        return m_isSetupProperty;
    }

    public SimpleBooleanProperty syncedProperty() {
        return isAvailableProperty();    
    }

    public SimpleLongProperty nodeBlockHeightProperty() {
        return m_nodeBlockHeightProperty;
    }

    public SimpleLongProperty networkBlockHeightProperty() {
        return m_networkBlockHeightProperty;
    }


    public long getSpaceRequired() {
        return m_spaceRequired;
    }

    public boolean getRunOnStart() {
        return m_runOnStart;

    }

    public String getInstallImgUrl() {
        return m_setupImgUrl;
    }

    public File getAppDir() {
        return m_appDir;
    }

    public File getAppFile() {
        return m_appDir != null && m_appDir.exists() && m_appDir.isDirectory() && m_appFileName != null ? new File(m_appDir.getAbsolutePath() + "/" + m_appFileName) : null;
    }

    

    public void cleanSync() throws IOException {
        Files.writeString(logFile.toPath(), "\nErgoLocalNode: Clean Sync Required - Re-syncing node", StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        File stateDir = new File(m_appDir.getCanonicalPath() + "/.ergo/state");
        File historyDir = new File(m_appDir.getCanonicalPath() + "/.ergo/history");

        if (stateDir.isDirectory()) {
            FileUtils.deleteDirectory(stateDir);
        }
        if (historyDir.isDirectory()) {
            FileUtils.deleteDirectory(historyDir);
        }

    }

    private final void updateCycle() {
        //get Network info

        //long timeoutSeconds = (getNetworksData().getCyclePeriod() * SYNC_TIMEOUT_CYCLE) / 60;
 
        final String localApiString = namedNodeUrlProperty().get().getUrlString() + "/info";
        final String prevVersionString = m_appVersion.get() != null ? m_appVersion.get().get() : "0.0.0";
        boolean foundPID = Utils.findPIDs(m_appFileName) != null;
        if(foundPID){
            isGettingInfo.set(true);
            Utils.getUrlJson(localApiString, getNetworksData().getExecService(), (onSucceeded) -> {
                Object sourceObject = onSucceeded.getSource().getValue();
                if (sourceObject != null && sourceObject instanceof JsonObject) {
                    JsonObject json = (JsonObject) sourceObject;

                    JsonElement fullHeightElement = json.get("fullHeight");
                    JsonElement maxPeerHeightElement = json.get("maxPeerHeight");
                    JsonElement peerCountElement = json.get("peersCount");
                    JsonElement headersHeightElement = json.get("headersHeight");
                    JsonElement appVersionElement = json.get("appVersion");

                    try {
                        Files.writeString(new File(m_appDir.getAbsolutePath() + "/info.json").toPath(), json.toString());
                    } catch (IOException e) {

                    }

                    long fullHeight = fullHeightElement != null && fullHeightElement.isJsonPrimitive() ? fullHeightElement.getAsLong() : -1;
                    long networkHeight = maxPeerHeightElement != null && maxPeerHeightElement.isJsonPrimitive() ? maxPeerHeightElement.getAsLong() : -1;
                    int peerCount = peerCountElement != null && peerCountElement.isJsonPrimitive() ? peerCountElement.getAsInt() : -1;
                    String appVersionString = appVersionElement != null && appVersionElement.isJsonPrimitive() ? appVersionElement.getAsString() : null;
                    long headersHeight = headersHeightElement != null && headersHeightElement.isJsonPrimitive() ? headersHeightElement.getAsLong() : -1;

                    long prevheadersHeight = m_nodeHeadersHeightProperty.get();
                    long prevBlockHeight = m_nodeBlockHeightProperty.get();
                    long prevNetworkHeight = m_networkBlockHeightProperty.get();

                  
                    cmdStatusUpdated().set(String.format("%29s", Utils.formatDateTimeString(LocalDateTime.now())));
                    m_peerCountProperty.set(peerCount);
                    if(fullHeight > prevBlockHeight){
                        m_nodeBlockHeightProperty.set(fullHeight);
                    }
                    if(networkHeight > prevNetworkHeight){
                        m_networkBlockHeightProperty.set(networkHeight);
                    }
                    if(headersHeight > prevheadersHeight){
                        m_nodeHeadersHeightProperty.set(headersHeight);
                    }
                    if (appVersionString != null && !prevVersionString.equals(appVersionString)) {
                        try {

                            m_appVersion.set(new Version(appVersionString));
                            lastUpdated().set(LocalDateTime.now());

                        } catch (IllegalArgumentException e) {

                        }
                    }

                    isGettingInfo.set(false);
                    if (m_isSyncStuck.get() > -1) {
                        if (m_isSyncStuck.get() > SYNC_TIMEOUT_CYCLE) {
                            stop();
                            NamedNodeUrl namedNode = namedNodeUrlProperty().get();
                            
                            Alert a = new Alert(AlertType.WARNING, namedNode.getName() +  " has been unable to sync. Would you like to restart the sync process?\n\nNotice: Improper shut down is the primary cuase of a node being unable to sync. It is recommended to use the graphical interface in order to ensure that the node is shut down gracefully.", ButtonType.YES, ButtonType.NO);
                            a.setTitle("Attention - Node Sync: Timed out");
                            a.setHeaderText("Attention: Node Sync: Timed out");
                            
                            Optional<ButtonType> result = a.showAndWait();
                            if(result != null && result.isPresent() && result.get() == ButtonType.YES){
        
                                try {
                                    cleanSync();
                                } catch (IOException e) {
        
                                }
                                start();
                
                            }
                            m_isSyncStuck.set(-1);
                        }
                        m_isSyncStuck.set(m_isSyncStuck.get() + 1);
                    }
            
                }
            }, (onFailed) -> {
                //String errMsg = onFailed.getSource().getException().toString();
                stop();
                isGettingInfo.set(false);
            }, null);
        }else{
            stop();
        }

    }

    private SimpleObjectProperty<ErgoNodeMsg[]> m_msgInput = new SimpleObjectProperty<>();

    private Runnable m_readNodeInput = () -> {
        
        if ( m_inputCycleIndex.incrementAndGet() == 300) {
         

            updateCycle();
            m_inputCycleIndex.set(0);
        }
        if (m_nodeMsgBuffer.size() > 0) {
            ArrayList<ErgoNodeMsg> newMsgs = new ArrayList<ErgoNodeMsg>();
            if (m_nodeMsgBuffer.size() > 0) {
                synchronized (m_nodeMsgBuffer) {

                    int j = m_nodeMsgBuffer.size() - 1;
                    if (j > -1) {
                        ErgoNodeMsg ergoNodeMsg = m_nodeMsgBuffer.get(j);

                        String lastInputId = m_lastNodeMsgId.get();
                        if (!(ergoNodeMsg.getId().equals(lastInputId))) {
                            m_lastNodeMsgId.set(lastInputId);
                            newMsgs.add(ergoNodeMsg);

                            j--;
                            while (j > 0 && !(ergoNodeMsg.getId().equals(lastInputId))) {
                                newMsgs.add(0, ergoNodeMsg);
                                ergoNodeMsg = m_nodeMsgBuffer.get(j);
                                j--;
                            }
                        }
                    }

                }
            }
            if(newMsgs.size() > 0){
                ErgoNodeMsg[] nodeMsgArray = new ErgoNodeMsg[newMsgs.size()];
                nodeMsgArray = newMsgs.toArray(nodeMsgArray);

                Utils.returnObject(nodeMsgArray,getNetworksData().getExecService(), (onSucceeded)->{
                    ErgoNodeMsg[] msgs = (ErgoNodeMsg[]) onSucceeded.getSource().getValue();
                    m_msgInput.set(msgs);
                }, (onFailed)->{

                });
            }
        }
    };
    //private Simpl<String> m_nodeMsg = new AtomicReference<>("");

   

    public String getExecCmd(File appFile, File configFile) {

        String networkTypeString = namedNodeUrlProperty().get().getNetworkType() == null ? NetworkType.MAINNET.toString() : namedNodeUrlProperty().get().getNetworkType().toString();

        String networkTypeFlag = networkTypeString.equals(NetworkType.MAINNET.toString()) ? "--mainnet" : "--testnet";
        
        if (m_execParams == null) {
            m_execParams = "";
        }

        String params = m_execParams.trim().length() > 0 ? " " + m_execParams.trim() : "";

        String cmdPrefix = "java" + params + " -jar " + appFile.getName() + " " + networkTypeFlag;

        String cmdPostfix = " -c " + configFile.getName();
        
        
        return cmdPrefix + cmdPostfix;
    }

    public void reset(){
           
        if (m_scheduledFuture != null) {
            m_scheduledFuture.cancel(false);
        }
        if (m_future != null) {
            m_future.cancel(false);
        }
        statusProperty().set(ErgoMarketsData.STOPPED);
        isAvailableProperty().set(false);
        m_nodeBlockHeightProperty.set(-1);
        m_networkBlockHeightProperty.set(-1);
        m_nodeHeadersHeightProperty.set(-1);
        m_peerCountProperty.set(-1);
        m_warningsProperty.set("");
        
        try {
            Utils.removeAppResource(m_appFileName);
        } catch (IOException e) {
            try {
                Files.writeString(App.logFile.toPath(), "\nLocalNode reset removeAppResource: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {
            }
        
        }
    }

    private void runNode(File appFile, File configFile) {
        if (m_executor == null) {
            m_executor = Executors.newSingleThreadExecutor();

        }
        if(m_schedualedExecutor == null){
            m_schedualedExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });
        }
  
        
        if ((m_future == null || m_future != null && m_future.isDone()) ) {

            String[] cmd = Utils.getShellCmd(getExecCmd(appFile, configFile));
        
           
            Thread mainThread = Thread.currentThread();
            //ScheduledFuture<?> future = 
            
            m_scheduledFuture = m_schedualedExecutor.scheduleAtFixedRate(()->{
         
                getNetworksData().getExecService().submit(()-> updateCycle());


            },0, 2000, TimeUnit.MILLISECONDS);
          
     
           
            m_future = m_executor.submit(new Runnable() {
                
                Process proc;

                /*private final ExecutorService executor = Executors.newCachedThreadPool(
                        new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        Thread t = Executors.defaultThreadFactory().newThread(r);
                        t.setDaemon(true);
                        return t;
                    }
                });
                private Future<?> lastExecution = null;*/
                @Override
                public void run() {

                    try {
                        proc = Runtime.getRuntime().exec(cmd, null, appFile.getParentFile());
                        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                        BufferedReader stderr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

                        if(proc.isAlive()){
                            Utils.returnObject(null,getNetworksData().getExecService(), (onSucceeded)->{
                                Utils.addAppResource(m_appFileName);
                                statusProperty().set(ErgoMarketsData.STARTED);
                                statusString().set(STARTUP_STRING);
                            }, (onFailed)->{});
                        }

                        String s = null;
                        try {
                            s = stdInput.readLine();
                        } catch (IOException e) {
                            try {
                                Files.writeString(logFile.toPath(), "\nstarting: No input", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e1) {
                              
                            }
                        }

                        while (s != null) {
                            s = stdInput.readLine();
                         /*   try {

                                s = stdInput.readLine();
                                String str = s;
                                if (s != null) {
                                 synchronized (m_nodeMsgBuffer) {
                                        if (m_nodeMsgBuffer.size() > MAX_INPUT_BUFFER_SIZE) {
                                            m_nodeMsgBuffer.remove(0);
                                        }
                                       
                                        m_nodeMsgBuffer.add(new ErgoNodeMsg(str));
                                        
                                    }
                                }
                            } catch (IOException e) {

                                proc.waitFor(2, TimeUnit.SECONDS);
                                try {
                                    s = stdInput.readLine();
                                    String str = s;
                                    synchronized (m_nodeMsgBuffer) {
                                        if (m_nodeMsgBuffer.size() > MAX_INPUT_BUFFER_SIZE) {
                                            m_nodeMsgBuffer.remove(0);
                                        }
                                        m_nodeMsgBuffer.add(new ErgoNodeMsg(str));
                                    }
                                } catch (IOException e1) {

                                }

                            }*/

                        }
                        String errLine = null;
                        while ((errLine = stderr.readLine()) != null) {

                            Files.writeString(logFile.toPath(), "\nLocal node execution error: " + errLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                             
                        }

                        proc.waitFor();
                        mainThread.join();
                        reset();

                    } catch (Exception e) {
                        try{

                            Files.writeString(logFile.toPath(), "\nLocal node error: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                             
                        }catch(IOException e1){

                        }
                        try {
                            mainThread.join();
                        } catch (InterruptedException e1) {
              
                        }
                        
                        reset();
                    }

                    //   m_pid = -1;
                    //   statusProperty().set(MarketsData.STOPPED);
                }

            });

        }

        //t.start();
    }


    @Override
    public void start() {
        String currentStatus = statusProperty().get();
        /*int memRecommended = m_memGBRequired;
        switch(namedNodeUrlProperty().get().getName()){
            case "FULL NODE": 
            default:
                memRecommended = 5;
        }

        FreeMemory freeMemory = null;
        try{
            freeMemory = Utils.getFreeMemory();
           
            double memAvailable = freeMemory.getMemAvailableGB();

            if(memAvailable < memRecommended){
                m_warningsProperty.set("LOW MEMORY AVAILABLE");
            }

        }catch(Exception e){
            
            try {
                Files.writeString(logFile.toPath(), "\nergoNodelocalData: freeMemory: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {
              
            }
              
        }*/
        if (m_isSetupProperty.get() && currentStatus.equals(ErgoMarketsData.STOPPED)) {
      
            
            Runnable runError = () -> {
                m_isSetupProperty.set(false);
                stop();
            };

            statusProperty().set(ErgoMarketsData.STARTING);

            checkDrive(m_appDir, (onSuccess)->{
                
                Object sourceValue = onSuccess.getSource().getValue();

                if(sourceValue != null && sourceValue instanceof Boolean && (Boolean) sourceValue){
                
                    File appFile = getAppFile();

                    File configFile = m_nodeConfigData != null ? m_nodeConfigData.getConfigFile() : null;

                    if (appFile != null && appFile.isFile() && configFile != null && configFile.isFile() && m_appFileHashData != null && m_nodeConfigData != null && m_nodeConfigData.getConfigFileHashData() != null) {
                     
                        try {
                            HashData appFileHashData = new HashData(Utils.digestFile(appFile));
                            HashData configFileHashData = new HashData(Utils.digestFile(configFile));

                            if (m_appFileHashData.getHashStringHex().equals(appFileHashData.getHashStringHex())) {
                                if (m_nodeConfigData.getConfigFileHashData().getHashStringHex().equals(configFileHashData.getHashStringHex())) {
                                   
                                   
                                    runNode(appFile, configFile);
                                    
                                } else {
                                    runError.run();

                                    configFileError(false);

                                }

                            } else {

                                runError.run();
                                coreFileError();
                            }

                        } catch (Exception e) {
                            runError.run();
                        }
                    } else {
                        runError.run();
                    }

                }else{
                    runError.run();
                }

            }, onFailed->{
                runError.run();
            });
            
        } else {
            if (!m_isSetupProperty.get()) {
                setup();
            }
        }

    }

    private Scene initialSetupScene(Button nextBtn, MenuButton configModeBtn, TextField apiKeyField, SimpleObjectProperty<File> configFileOption, SimpleObjectProperty<File> directoryRoot, TextField directoryNameField, Stage stage) {
        String titleString = "Setup - Local Node - " + ErgoNodes.NAME;
        stage.setTitle(titleString);

        Image icon = ErgoNodes.getSmallAppIcon();
        double defaultRowHeight = 30;
        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(icon, titleString, closeBtn, stage);
        Text headingText = new Text("Setup");
        headingText.setFont(App.txtFont);
        headingText.setFill(Color.WHITE);

        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(defaultRowHeight);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 10, 10, 10));
        headingBox.setId("headingBox");

        HBox headingPaddingBox = new HBox(headingBox);

        headingPaddingBox.setPadding(new Insets(5, 0, 2, 0));

        VBox headerBox = new VBox(headingPaddingBox);

        headerBox.setPadding(new Insets(0, 5, 0, 5));

        SimpleDoubleProperty rowHeight = new SimpleDoubleProperty(defaultRowHeight);

        Text configText = new Text("Setup");
        configText.setFill(App.txtColor);
        configText.setFont(App.txtFont);

        MenuItem simpleItem = new MenuItem(m_nodeConfigData != null ? m_nodeConfigData.getConfigMode() : ConfigMode.BASIC);
        simpleItem.setOnAction(e -> {
            configModeBtn.setText(simpleItem.getText());
        });

        MenuItem advancedItem = new MenuItem(ConfigMode.ADVANCED);
        advancedItem.setOnAction(e -> {
            configModeBtn.setText(advancedItem.getText());
        });

        configModeBtn.getItems().addAll(simpleItem, advancedItem);
        configModeBtn.setFont(App.txtFont);

        HBox configBox = new HBox(configText);
        configBox.setAlignment(Pos.CENTER_LEFT);
        configBox.setMinHeight(40);
        configBox.setId("headingBox");
        configBox.setPadding(new Insets(0, 0, 0, 15));

        Text apiKeyText = new Text(String.format("%-15s", " API Key"));
        apiKeyText.setFill(getPrimaryColor());
        apiKeyText.setFont((App.txtFont));

        apiKeyField.setPromptText("Enter key");
        apiKeyField.setId("formField");
        HBox.setHgrow(apiKeyField, Priority.ALWAYS);

        PasswordField apiKeyHidden = new PasswordField();
        apiKeyHidden.setPromptText("Enter key");
        apiKeyHidden.setId("formField");
        HBox.setHgrow(apiKeyHidden, Priority.ALWAYS);

        apiKeyHidden.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                apiKeyField.setText(apiKeyHidden.getText());
            }
        });

        BufferedButton showKeyBtn = new BufferedButton("/assets/eye-30.png", App.MENU_BAR_IMAGE_WIDTH);

        Tooltip randomApiKeyTip = new Tooltip("Random API Key");

        BufferedButton randomApiKeyBtn = new BufferedButton("/assets/d6-30.png", App.MENU_BAR_IMAGE_WIDTH);
        randomApiKeyBtn.setTooltip(randomApiKeyTip);
        randomApiKeyBtn.setOnAction(e -> {
            try {
                int length = Utils.getRandomInt(12, 20);
                char key[] = new char[length];
                for (int i = 0; i < length; i++) {
                    key[i] = (char) Utils.getRandomInt(33, 126);
                }
                String keyString = new String(key);
                apiKeyField.setText(keyString);
                apiKeyHidden.setText(keyString);
            } catch (NoSuchAlgorithmException e1) {
                Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.CANCEL);
                a.initOwner(stage);
                a.setHeaderText("Error");
                a.setTitle("Error - Setup - Local Node - " + ErgoNodes.NAME);
                a.show();
            }
        });

        HBox apiKeyBox = new HBox(apiKeyText, apiKeyHidden, showKeyBtn, randomApiKeyBtn);
        apiKeyBox.setPadding(new Insets(0, 0, 0, 15));;
        apiKeyBox.setAlignment(Pos.CENTER_LEFT);

        apiKeyBox.minHeightProperty().bind(rowHeight);
        HBox.setHgrow(apiKeyBox, Priority.ALWAYS);

        showKeyBtn.setOnAction(e -> {
            if (apiKeyBox.getChildren().contains(apiKeyHidden)) {
                apiKeyField.setText(apiKeyHidden.getText());
                apiKeyBox.getChildren().remove(apiKeyHidden);
                apiKeyBox.getChildren().add(1, apiKeyField);
                showKeyBtn.setImage(new Image("/assets/eye-off-30.png"));
            } else {
                apiKeyHidden.setText(apiKeyField.getText());
                apiKeyBox.getChildren().remove(apiKeyField);
                apiKeyBox.getChildren().add(1, apiKeyHidden);
                showKeyBtn.setImage(new Image("/assets/eye-30.png"));
            }
        });
        /*
        Text digestModeText = new Text(String.format("%-15s", " Transactions"));
        digestModeText.setFill(getPrimaryColor());
        digestModeText.setFont(App.txtFont);

        MenuItem localItem = new MenuItem(DigestAccess.LOCAL);

        localItem.setOnAction(e -> {
            digestAccessBtn.setText(localItem.getText());
        });
        MenuItem allItem = new MenuItem(DigestAccess.ALL);

        allItem.setOnAction(e -> {
            digestAccessBtn.setText(allItem.getText());
        });
        digestAccessBtn.getItems().addAll(localItem, allItem);
        digestAccessBtn.setId("formField");
        digestAccessBtn.setFont(App.txtFont);

        HBox digestModeBox = new HBox(digestModeText, digestAccessBtn);
        digestModeBox.setAlignment(Pos.CENTER_LEFT);
        digestModeBox.setPadding(new Insets(0, 0, 0, 15));
        digestModeBox.minHeightProperty().bind(rowHeight);
        HBox.setHgrow(digestModeBox, Priority.ALWAYS);

        Text blockchainModeText = new Text(String.format("%-15s", " Blockchain"));
        blockchainModeText.setFill(getPrimaryColor());
        blockchainModeText.setFont(App.txtFont);

        MenuItem bootstrapItem = new MenuItem(BlockchainMode.PRUNED);

        bootstrapItem.setOnAction(e -> {
            blockchainModeBtn.setText(bootstrapItem.getText());
        });
        MenuItem latestItem = new MenuItem(BlockchainMode.RECENT_ONLY);

        latestItem.setOnAction(e -> {
            blockchainModeBtn.setText(latestItem.getText());
        });
        MenuItem fullItem = new MenuItem(BlockchainMode.FULL);

        fullItem.setOnAction(e -> {
            blockchainModeBtn.setText(fullItem.getText());
        });

        blockchainModeBtn.getItems().addAll(bootstrapItem, latestItem, fullItem);
        blockchainModeBtn.setFont(App.txtFont);

        HBox blockchainModeBox = new HBox(blockchainModeText, blockchainModeBtn);
        blockchainModeBox.setAlignment(Pos.CENTER_LEFT);
        blockchainModeBox.setPadding(new Insets(0, 0, 0, 15));
        blockchainModeBox.minHeightProperty().bind(rowHeight);

        HBox.setHgrow(blockchainModeBox, Priority.ALWAYS);

         Text noticeText = new Text(String.format("%-15s", "   Notice"));
        noticeText.setFill(getPrimaryColor());
        noticeText.setFont(App.txtFont);

        TextField noticeField = new TextField("An old wallet requires the full blockchain.");
        noticeField.setId("formField");
        noticeField.setEditable(false);
        HBox.setHgrow(noticeField, Priority.ALWAYS);

        HBox noticeBox = new HBox();
        noticeBox.setAlignment(Pos.CENTER_LEFT);
        noticeBox.setPadding(new Insets(0, 0, 0, 15));
        noticeBox.minHeightProperty().bind(rowHeight);*/

        Text modeText = new Text(String.format("%-9s", "Mode"));
        modeText.setFill(App.txtColor);
        modeText.setFont((App.txtFont));

        HBox configModeBox = new HBox(modeText, configModeBtn);
        configModeBox.setAlignment(Pos.CENTER_LEFT);
        configModeBox.setPadding(new Insets(0, 0, 0, 15));
        configModeBox.setMinHeight(40);

        Text advFileModeText = new Text(String.format("%-15s", " File"));
        advFileModeText.setFill(getPrimaryColor());
        advFileModeText.setFont(App.txtFont);

        Button advFileModeBtn = new Button("Browse...");
        advFileModeBtn.setFont(App.txtFont);
        advFileModeBtn.setId("rowBtn");
        HBox.setHgrow(advFileModeBtn, Priority.ALWAYS);

        advFileModeBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select location");
            chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Config (text)", "*.conf", "*.config", "*.cfg"));
            File configFile = chooser.showOpenDialog(stage);
            if (configFile != null && configFile.isFile()) {
                configFileOption.set(configFile);
                advFileModeBtn.setText(configFile.getAbsolutePath());
            }
        });

        HBox advFileModeBox = new HBox(advFileModeText, advFileModeBtn);
        advFileModeBox.setAlignment(Pos.CENTER_LEFT);
        advFileModeBox.setPadding(new Insets(0, 0, 0, 15));
        advFileModeBox.minHeightProperty().bind(rowHeight);




        VBox modeOptionsBodyBox = new VBox(apiKeyBox);
        modeOptionsBodyBox.setPadding(new Insets(0, 0, 0, 15));

     
  


        VBox modeBodyBox = new VBox(configModeBox, modeOptionsBodyBox);
        modeBodyBox.setPadding(new Insets(15));
        modeBodyBox.setId("bodyBox");
        HBox.setHgrow(modeBodyBox, Priority.ALWAYS);

        VBox modeBox = new VBox(configBox, modeBodyBox);
        modeBox.setPadding(new Insets(0, 0, 15, 0));

        //configModeBtn.prefWidthProperty().bind(configModeBox.widthProperty().subtract(configModeText.layoutBoundsProperty().get().getWidth()));
        Text directoryText = new Text("Directory");
        directoryText.setFill(App.txtColor);
        directoryText.setFont(App.txtFont);

        HBox directoryBox = new HBox(directoryText);
        directoryBox.setAlignment(Pos.CENTER_LEFT);
        directoryBox.setMinHeight(40);;
        directoryBox.setId("headingBox");
        directoryBox.setPadding(new Insets(0, 0, 0, 15));

        Text directoryRootText = new Text(String.format("%-9s", "Location"));
        directoryRootText.setFill(App.txtColor);
        directoryRootText.setFont(App.txtFont);

        Text directoryNameText = new Text(String.format("%-9s", "Folder"));
        directoryNameText.setFill(App.txtColor);
        directoryNameText.setFont(App.txtFont);

        directoryNameField.setFont(App.txtFont);
        directoryNameField.setId("formField");
        HBox.setHgrow(directoryNameField, Priority.ALWAYS);

        HBox directoryNameBox = new HBox(directoryNameText, directoryNameField);
        directoryNameBox.setAlignment(Pos.CENTER_LEFT);
        directoryNameBox.setPadding(new Insets(0, 0, 0, 15));
        directoryNameBox.minHeightProperty().bind(rowHeight);

        Button directoryRootBtn = new Button();
        directoryRootBtn.setFont(App.txtFont);
        directoryRootBtn.setId("rowBtn");
        HBox.setHgrow(directoryRootBtn, Priority.ALWAYS);
        directoryRootBtn.textProperty().bind(directoryRoot.asString());
        directoryRootBtn.setOnAction(e -> {

            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select location");
            chooser.setInitialDirectory(directoryRoot.get());

            File locationDir = chooser.showDialog(stage);
            if (locationDir != null && locationDir.isDirectory()) {
                directoryRoot.set(locationDir);
            }
        });

        HBox directoryRootBox = new HBox(directoryRootText, directoryRootBtn);
        directoryRootBox.setPadding(new Insets(0, 0, 0, 15));
        directoryRootBox.setAlignment(Pos.CENTER_LEFT);
        directoryRootBox.minHeightProperty().bind(rowHeight);

        Text useableText = new Text(" Available Space  ");
        useableText.setFill(getPrimaryColor());
        useableText.setFont(App.txtFont);

        TextField useableField = new TextField(Utils.formatedBytes(directoryRoot.get().getUsableSpace(), 2));
        useableField.setFont(App.txtFont);
        useableField.setId("formField");
        useableField.setEditable(false);
        HBox.setHgrow(useableField, Priority.ALWAYS);

        directoryRoot.addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                useableField.setText(Utils.formatedBytes(directoryRoot.get().getUsableSpace(), 2));
            } else {
                useableField.setText("-");
            }
        });

        HBox useableBox = new HBox(useableText, useableField);
        useableBox.minHeightProperty().bind(rowHeight);
        HBox.setHgrow(useableBox, Priority.ALWAYS);
        useableBox.setPadding(new Insets(0, 0, 0, 15));
        useableBox.setAlignment(Pos.CENTER_LEFT);

        Text requiredText = new Text(" Required Space   ");
        requiredText.setFill(getPrimaryColor());
        requiredText.setFont(App.txtFont);

        //  final String advSpaceRequiredString = "~100 Mb - >50 Gb";
        TextField requiredField = new TextField("~20 GB");
        requiredField.setFont(App.txtFont);
        requiredField.setId("formField");
        requiredField.setEditable(false);
        HBox.setHgrow(requiredField, Priority.ALWAYS);

        /* Runnable estimateSpaceRequired = () -> {
            switch (blockchainModeBtn.getText()) {
                case BlockchainMode.RECENT_ONLY:
                    requiredField.setText("~100 Mb");
                    break;
                case BlockchainMode.PRUNED:
                    requiredField.setText("~500 MB");
                    break;
                case BlockchainMode.FULL:
                    requiredField.setText(">50 GB");
                    break;
            }
        };
        blockchainModeBtn.textProperty().addListener((obs, oldval, newval) -> {
            estimateSpaceRequired.run();
        });*/

        
 /*configModeBtn.textProperty().addListener((obs, oldVal, newVal) -> {
            switch (newVal) {
                case ConfigMode.ADVANCED:
                    requiredField.setText(advSpaceRequiredString);
                    if (modeBodyBox.getChildren().contains(modeOptionsBodyBox)) {
                        modeBodyBox.getChildren().remove(modeOptionsBodyBox);
                    }

                    if (!modeBodyBox.getChildren().contains(advFileModeBox)) {
                        modeBodyBox.getChildren().add(advFileModeBox);
                    }

                    break;
                default:
                    estimateSpaceRequired.run();
                    if (modeBodyBox.getChildren().contains(advFileModeBox)) {
                        modeBodyBox.getChildren().remove(advFileModeBox);
                    }
                    if (!modeBodyBox.getChildren().contains(modeOptionsBodyBox)) {
                        modeBodyBox.getChildren().add(modeOptionsBodyBox);
                    }

                    break;
            }
        });*/
        HBox requiredBox = new HBox(requiredText, requiredField);
        requiredBox.minHeightProperty().bind(rowHeight);
        HBox.setHgrow(requiredBox, Priority.ALWAYS);
        requiredBox.setPadding(new Insets(0, 0, 0, 15));
        requiredBox.setAlignment(Pos.CENTER_LEFT);

        VBox directorySpaceBox = new VBox(useableBox, requiredBox);
        directorySpaceBox.setPadding(new Insets(0, 0, 0, 105));

        VBox directoryBodyBox = new VBox(directoryNameBox, directoryRootBox, directorySpaceBox);
        directoryBodyBox.setPadding(new Insets(15));
        directoryBodyBox.setId("bodyBox");
        HBox.setHgrow(directoryBodyBox, Priority.ALWAYS);

        HBox padBox = new HBox(directoryBodyBox);
        padBox.setPadding(new Insets(2, 0, 15, 0));
        HBox.setHgrow(padBox, Priority.ALWAYS);

        VBox directoryPaddingBox = new VBox(directoryBox, padBox);
        HBox.setHgrow(directoryPaddingBox, Priority.ALWAYS);

        nextBtn.setPadding(new Insets(5, 15, 5, 15));
        HBox nextBox = new HBox(nextBtn);

        nextBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(nextBox, Priority.ALWAYS);

        VBox bodyBox = new VBox(modeBox, directoryPaddingBox, nextBox);
        bodyBox.setId("bodyBox");
        bodyBox.setPadding(new Insets(15));

        VBox bodyPaddingBox = new VBox(bodyBox);
        bodyPaddingBox.setPadding(new Insets(5, 5, 5, 5));

        Region footerSpacer = new Region();
        footerSpacer.setMinHeight(5);

        VBox footerBox = new VBox(footerSpacer);

        VBox layoutBox = new VBox(titleBox, headerBox, bodyPaddingBox, footerBox);
        Scene setupNodeScene = new Scene(layoutBox, SETUP_STAGE_WIDTH, SETUP_STAGE_HEIGHT);
        setupNodeScene.setFill(null);
        setupNodeScene.getStylesheets().add("/css/startWindow.css");

        Runnable closeStage = () -> {
            stage.close();
            m_stage = null;
        };
        
        configModeBtn.textProperty().addListener((obs,oldval,newval)->{
            switch(newval){
                case ConfigMode.ADVANCED:
                    if(!modeOptionsBodyBox.getChildren().contains(advFileModeBox)){
                        modeOptionsBodyBox.getChildren().add(0, advFileModeBox);
                    }

                    directoryNameField.setText("");

                    if(directoryBodyBox.getChildren().contains(directoryNameBox)){
                        directoryBodyBox.getChildren().remove(directoryNameBox);
                    }
                  
                break;
                case ConfigMode.BASIC:
                default:
                    if(modeOptionsBodyBox.getChildren().contains(advFileModeBox)){
                        modeOptionsBodyBox.getChildren().remove(advFileModeBox);
                    }
                    directoryNameField.setText(DEFAULT_NODE_NAME);

                    if(!directoryBodyBox.getChildren().contains(directoryNameBox)){
                        directoryBodyBox.getChildren().add(0, directoryNameBox);
                    }
            }
         });

        closeBtn.setOnAction(e -> closeStage.run());
        m_stage.setOnCloseRequest(e -> closeStage.run());
        return setupNodeScene;
    }

    private Scene getFinalSetupScene(Button nextBtn, Button backBtn, SimpleObjectProperty<File> jarFile, SimpleBooleanProperty getLatestBoolean, SimpleBooleanProperty updateBoolean, SimpleBooleanProperty autoUpdateBoolean, SimpleStringProperty downloadUrlProperty, SimpleStringProperty downloadFileName, Stage stage) {

        String titleString = "Core File - Setup - Local Node - " + ErgoNodes.NAME;
        stage.setTitle(titleString);

        Image icon = ErgoNodes.getSmallAppIcon();
        double defaultRowHeight = 30;
        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(icon, titleString, closeBtn, stage);
        Text headingText = new Text("Setup");
        headingText.setFont(App.txtFont);
        headingText.setFill(Color.WHITE);

        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(defaultRowHeight);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 10, 10, 10));
        headingBox.setId("headingBox");

        HBox headingPaddingBox = new HBox(headingBox);

        headingPaddingBox.setPadding(new Insets(5, 0, 2, 0));

        VBox headerBox = new VBox(headingPaddingBox);

        headerBox.setPadding(new Insets(0, 5, 0, 5));

        SimpleDoubleProperty rowHeight = new SimpleDoubleProperty(defaultRowHeight);

        Text appFileText = new Text("Core File");
        appFileText.setFill(App.txtColor);
        appFileText.setFont(App.txtFont);

        HBox appFileBox = new HBox(appFileText);
        appFileBox.setPadding(new Insets(0, 0, 0, 15));
        appFileBox.setAlignment(Pos.CENTER_LEFT);
        appFileBox.setId("headingBox");
        appFileBox.setMinHeight(defaultRowHeight);

        BufferedButton latestJarRadio = new BufferedButton("/assets/radio-button-on-30.png", 15);

        Text latestJarText = new Text(String.format("%-10s", " Download"));
        latestJarText.setFill(App.txtColor);
        latestJarText.setFont((App.txtFont));
        latestJarText.setOnMouseClicked(e -> {
            getLatestBoolean.set(true);
        });

        TextField latestJarNameField = new TextField("ergo-5.0.13.jar");
        latestJarNameField.setFont(App.txtFont);
        latestJarNameField.setId("formField");
        latestJarNameField.setEditable(false);
        HBox.setHgrow(latestJarNameField, Priority.ALWAYS);
        latestJarNameField.textProperty().bind(downloadFileName);
        TextField latestJarUrlField = new TextField();
        latestJarUrlField.setFont(App.txtFont);
        latestJarUrlField.setId("formField");
        latestJarUrlField.setEditable(false);
        HBox.setHgrow(latestJarUrlField, Priority.ALWAYS);

        latestJarUrlField.textProperty().bind(downloadUrlProperty);

        Runnable getLatestUrl = () -> {

            m_gitHubAPI.getAssetsLatest(getNetworksData().getExecService(), (onSucceded)->{
                Object assetsObject = onSucceded.getSource().getValue();
                if(assetsObject != null && assetsObject instanceof GitHubAsset[] && ((GitHubAsset[]) assetsObject).length > 0){
                    GitHubAsset[] assets = (GitHubAsset[]) assetsObject;
                    GitHubAsset latestAsset = assets[0];

                    downloadFileName.set(latestAsset.getName());
                    downloadUrlProperty.set(latestAsset.getUrl());
                    getLatestBoolean.set(true);
                }else{
                    latestJarNameField.setText("Unable to connect to GitHub (try again ->)");
                }

            }, onFailed -> {
                latestJarNameField.setText("Unable to connect to GitHub (try again ->)");
            });
        };

        Tooltip downloadBtnTip = new Tooltip("Get GitHub Info");
        downloadBtnTip.setShowDelay(new Duration(200));

        BufferedButton downloadBtn = new BufferedButton("/assets/sync-30.png", 30);
        downloadBtn.setTooltip(downloadBtnTip);
        downloadBtn.setOnAction(e -> getLatestUrl.run());
   
        getLatestUrl.run();

        latestJarRadio.setOnAction(e -> {
            getLatestBoolean.set(true);
        });

        Region btnSpacer = new Region();
        HBox.setHgrow(btnSpacer, Priority.ALWAYS);

        HBox latestJarBox = new HBox(latestJarRadio, latestJarText, btnSpacer, downloadBtn);
        latestJarBox.setPadding(new Insets(0, 0, 0, 5));
        latestJarBox.setAlignment(Pos.CENTER_LEFT);
        latestJarBox.setMinHeight(defaultRowHeight);

        Text latestJarNameText = new Text(String.format("%-10s", "Name"));
        latestJarNameText.setFill(App.txtColor);
        latestJarNameText.setFont((App.txtFont));

        HBox latestJarNameBox = new HBox(latestJarNameText, latestJarNameField);
        latestJarNameBox.setPadding(new Insets(0, 0, 0, 55));
        latestJarNameBox.setAlignment(Pos.CENTER_LEFT);
        latestJarNameBox.minHeightProperty().bind(rowHeight);

        Text latestJarUrlText = new Text(String.format("%-10s", "Url"));
        latestJarUrlText.setFill(App.txtColor);
        latestJarUrlText.setFont((App.txtFont));

        HBox latestJarUrlBox = new HBox(latestJarUrlText, latestJarUrlField);
        latestJarUrlBox.setPadding(new Insets(0, 0, 0, 55));
        latestJarUrlBox.setAlignment(Pos.CENTER_RIGHT);
        latestJarUrlBox.minHeightProperty().bind(rowHeight);

        BufferedButton selectJarRadio = new BufferedButton("/assets/radio-button-off-30.png", 15);
        selectJarRadio.setOnAction(e -> {
            getLatestBoolean.set(false);
        });

        Text customText = new Text(" Existing");
        customText.setFill(App.txtColor);
        customText.setFont((App.txtFont));
        customText.setOnMouseClicked(e -> {
            getLatestBoolean.set(false);
        });

        HBox customBox = new HBox(selectJarRadio, customText);
        customBox.setPadding(new Insets(0, 0, 0, 5));
        customBox.setAlignment(Pos.CENTER_LEFT);

        Text jarFileText = new Text(String.format("%-10s", "File"));
        jarFileText.setFill(App.txtColor);
        jarFileText.setFont((App.txtFont));

        Button jarFileBtn = new Button("Browse...");
        jarFileBtn.setId("rowBtn");
        jarFileBtn.setOnAction(e -> {

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Core File (*.jar)");
            chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Ergo Node Jar", "*.jar"));

            File appFile = chooser.showOpenDialog(m_stage);
            if (appFile != null) {
                if (Utils.checkJar(appFile)) {
                    jarFileBtn.setText(appFile.getAbsolutePath());
                    getLatestBoolean.set(false);
                    jarFile.set(appFile);
                } else {
                    Alert a = new Alert(AlertType.NONE, "Unable to open file.", ButtonType.CANCEL);
                    a.initOwner(stage);
                    a.setHeaderText("Invalid file");
                    a.setTitle("Invalid File - Setup - " + ErgoNodes.NAME);
                    a.show();
                }
            }
        });

        HBox jarFileBox = new HBox(jarFileText, jarFileBtn);
        jarFileBox.setPadding(new Insets(0, 0, 0, 55));
        jarFileBox.setAlignment(Pos.CENTER_LEFT);
        jarFileBox.minHeightProperty().bind(rowHeight);

        getLatestBoolean.addListener((obs, oldVal, newVal) -> {
            if (newVal.booleanValue()) {
                latestJarRadio.setImage(new Image("/assets/radio-button-on-30.png"));
                selectJarRadio.setImage(new Image("/assets/"));
            } else {
                latestJarRadio.setImage(new Image("/assets/radio-button-off-30.png"));
                selectJarRadio.setImage(new Image("/assets/radio-button-on-30.png"));
            }
        });

        Text autoUpdateText = new Text(String.format("%-10s", "Auto"));
        autoUpdateText.setFill(App.txtColor);
        autoUpdateText.setFont((App.txtFont));
        autoUpdateText.setOnMouseClicked(e -> {
            getLatestBoolean.set(false);
        });
   

        MenuButton autoUpdateBtn = new MenuButton(autoUpdateBoolean.get() ? "Enabled" : "Disabled");
        autoUpdateBtn.setId("amountMenuBtn");
        MenuItem autoUpdateEnabledItem = new MenuItem("Enabled");
      

        MenuItem autoUpdateDiabledItem = new MenuItem("Disabled");
 
        autoUpdateBtn.getItems().addAll(autoUpdateEnabledItem, autoUpdateDiabledItem);

        HBox autoUpdateBox = new HBox(autoUpdateText, autoUpdateBtn);
        autoUpdateBox.setPadding(new Insets(0, 0, 0, 55));
        autoUpdateBox.setAlignment(Pos.CENTER_LEFT);
    


        Text updatesText = new Text(String.format("%-11s", " Updates"));
        updatesText.setFill(App.txtColor);
        updatesText.setFont((App.txtFont));

        BufferedButton updatesBtn = new BufferedButton("/assets/checkmark-25.png", App.MENU_BAR_IMAGE_WIDTH);
        updatesBtn.setId("checkBtn");

        updateBoolean.addListener((obs,oldval,newval)->{
            if(newval){
                updatesBtn.setImage(new Image("/assets/checkmark-25.png"));
            }else{
                updatesBtn.setImage(null);
            }
        });

        HBox updatesBox = new HBox(updatesBtn, updatesText);
        updatesBox.setPadding(new Insets(0, 0, 0, 5));
        updatesBox.setAlignment(Pos.CENTER_LEFT);
        updatesBox.setMinHeight(40);


        VBox jarBodyBox = new VBox(latestJarBox, latestJarNameBox, latestJarUrlBox, customBox, jarFileBox, updatesBox, autoUpdateBox);
        jarBodyBox.setPadding(new Insets(15));
        jarBodyBox.setId("bodyBox");
        HBox.setHgrow(jarBodyBox, Priority.ALWAYS);

    

      
        autoUpdateDiabledItem.setOnAction(e->{
            autoUpdateBoolean.set(false);
            autoUpdateBtn.setText("Disabled");
        });

        autoUpdateEnabledItem.setOnAction(e->{
            autoUpdateBoolean.set(true);
            autoUpdateBtn.setText(autoUpdateEnabledItem.getText());
        });

        updatesBtn.setOnAction(e->{
            updateBoolean.set(!updateBoolean.get());
  

            if(updateBoolean.get()){
                if(!jarBodyBox.getChildren().contains(autoUpdateBox)){
                    jarBodyBox.getChildren().add(autoUpdateBox);
                }
            }else{
                autoUpdateDiabledItem.fire();
                if(jarBodyBox.getChildren().contains(autoUpdateBox)){
                    jarBodyBox.getChildren().remove(autoUpdateBox);
                }
            }
        });




        HBox jarbodyPadBox = new HBox(jarBodyBox);
        jarbodyPadBox.setPadding(new Insets(2, 0, 15, 0));
        HBox.setHgrow(jarbodyPadBox, Priority.ALWAYS);

        VBox jarBox = new VBox(appFileBox, jarbodyPadBox);
        HBox.setHgrow(jarBox, Priority.ALWAYS);
        Region smallRegion = new Region();
        smallRegion.setMinWidth(15);
        backBtn.setPadding(new Insets(5, 15, 5, 15));
        nextBtn.setPadding(new Insets(5, 15, 5, 15));
        HBox nextBox = new HBox(backBtn, smallRegion, nextBtn);
        nextBox.setMinHeight(35);
        nextBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(nextBox, Priority.ALWAYS);

        VBox bodyBox = new VBox(jarBox, nextBox);
        bodyBox.setId("bodyBox");
        bodyBox.setPadding(new Insets(15));

        VBox bodyPaddingBox = new VBox(bodyBox);
        bodyPaddingBox.setPadding(new Insets(5, 5, 5, 5));

        Region footerSpacer = new Region();
        footerSpacer.setMinHeight(5);

        VBox footerBox = new VBox(footerSpacer);

        VBox layoutBox = new VBox(titleBox, headerBox, bodyPaddingBox, footerBox);
        Scene setupNodeScene = new Scene(layoutBox, CORE_SETUP_STAGE_WIDTH, CORE_SETUP_STAGE_HEIGHT);
        setupNodeScene.setFill(null);
        setupNodeScene.getStylesheets().add("/css/startWindow.css");

        SimpleDoubleProperty heightProperty = new SimpleDoubleProperty(CORE_SETUP_STAGE_HEIGHT);
        heightProperty.bind(titleBox.heightProperty().add(headerBox.heightProperty()).add(bodyPaddingBox.heightProperty()).add(footerBox.heightProperty()));

        stage.setHeight(heightProperty.get());
        
        ChangeListener<Number> heightChangeListener = (obs,oldval,newval)->{
            stage.setHeight(newval.doubleValue());
        };
    

        heightProperty.addListener(heightChangeListener);     
        Runnable closeStage = () -> {
            stage.close();
            m_stage = null;
        };

        closeBtn.setOnAction(e -> closeStage.run());
        m_stage.setOnCloseRequest(e -> closeStage.run());
        return setupNodeScene;
    }

    public static long getRequiredSpace() {
        /*switch (blockchainMode) {
            case BlockchainMode.RECENT_ONLY:
                return 150L * 1024L * 1024L;

            case BlockchainMode.PRUNED:
                return 500L * 1024L * 1024L;

            case BlockchainMode.FULL:
            default:
                
        }*/
        return 20L * 1024L * 1024L * 1024L;
    }

    public static void checkDrive(File appDir, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        Task<Boolean> task = new Task<Boolean>() {
            @Override
            public Boolean call() throws IOException {
                String path = appDir.getCanonicalPath();
                return Utils.findPathPrefixInRoots(path);
             
            }
        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();

    }



    public boolean checkValidSetup() {

        File appDir = m_appDir;
        File configFile = m_nodeConfigData.getConfigFile();
        File appFile = getAppFile();

        return (appDir != null && appDir.exists() && appDir.isDirectory() && configFile != null && configFile.exists() && configFile.isFile() && appFile != null && appFile.exists() && appFile.isFile());
    }

    public void setup() {

        if (m_stage == null) {

            File appFile = getAppFile();

            File parentDir = appFile != null ? appFile.getParentFile() : null;


            SimpleObjectProperty<File> directory = new SimpleObjectProperty<File>(parentDir != null && parentDir.isDirectory() ? parentDir.getParentFile() : getErgoNodesList().getErgoNodes().getAppDir());

            TextField folderNameField = new TextField(parentDir != null && parentDir.isDirectory() ? parentDir.getName() : DEFAULT_NODE_NAME);

            TextField configApiKey = new TextField();
            MenuButton configModeBtn = new MenuButton(ConfigMode.BASIC);
            // MenuButton configDigestAccess = new MenuButton(DigestAccess.LOCAL);
            // MenuButton configBlockchainMode = new MenuButton(BlockchainMode.PRUNED);

            SimpleObjectProperty<File> configFileOption = new SimpleObjectProperty<>(null);

            Button nextBtn = new Button("Next");

            m_stage = new Stage();
            m_stage.getIcons().add(getIcon());
            m_stage.setResizable(false);
            m_stage.initStyle(StageStyle.UNDECORATED);

            Scene initialScene = initialSetupScene(nextBtn, configModeBtn, configApiKey, configFileOption, directory, folderNameField, m_stage);
            m_stage.setScene(initialScene);

            m_stage.show();
            Platform.runLater(()->m_stage.requestFocus());

            nextBtn.setOnAction(e -> {
                final String configMode = configModeBtn.getText();
                // final String digestMode = configDigestAccess.getText();
                //  final String blockchainMode = configBlockchainMode.getText();
                final String apiKey = configApiKey.getText();
                final File dir = directory.get();
                String directoryString = dir.getAbsolutePath();

                final File installDir = new File(dir.getAbsolutePath() + "/" + folderNameField.getText());

                if (!installDir.isDirectory()) {

                    Alert a = new Alert(AlertType.NONE, "This will create the '" + folderNameField.getText() + "' folder.\n(" + dir.getAbsolutePath() +"/"+ folderNameField.getText() + ")", ButtonType.OK, ButtonType.CANCEL);
                    a.initOwner(m_stage);
                    a.setTitle("Create Folder");
                    a.setHeaderText("Create Folder");
                    Optional<ButtonType> result = a.showAndWait();
                    if (result != null && result.isPresent() && result.get() == ButtonType.CANCEL) {
                        return;
                    } else {
                        if (result == null) {
                            return;
                        }
                    }
                }

                long useableSpace = directory.get().getUsableSpace();
                long requiredSpace = getRequiredSpace();
                String errorMsg = "";

                if (!installDir.isDirectory()) {
                    try {
                        Files.createDirectory(installDir.toPath());

                    } catch (IOException e1) {
                        errorMsg = e1.toString();
                    }
                }
                if (installDir.isDirectory()) {
                    if (requiredSpace > useableSpace) {
                        Alert a = new Alert(AlertType.NONE, "The selected directory does not meet the space requirements.\n\nUseable space: " + Utils.formatedBytes(useableSpace, 2) + "\nRequired space: " + Utils.formatedBytes(requiredSpace, 2), ButtonType.OK);
                        a.initOwner(m_stage);
                        a.setHeaderText("Required Space");
                        a.setTitle("Required Space - Setup - Local Node - Ergo Nodes");
                        a.show();
                    } else {
                        if (configMode.equals(ConfigMode.BASIC)) {

                            install(configMode, DEFAULT_CONFIG_NAME, apiKey, installDir, initialScene);
                        } else {
                            File configFile = configFileOption.get();
                            if (configFile != null && configFile.isFile()) {
                                final String configFileName = configFile.getName();
                                File configParent = configFile.getParentFile();
                                File newConfig = new File(directoryString + "/" + configFileName);

                                String configFileErr = "Cannot find config file.";
                                if (!configParent.getAbsolutePath().equals(directoryString)) {

                                    try {
                                        Files.copy(configFile.toPath(), newConfig.toPath());
                                    } catch (IOException e1) {
                                        configFileErr = e1.toString();
                                    }
                                }
                                if (newConfig.isFile()) {
                                    install(configMode, configFileName, apiKey, installDir, initialScene);
                                } else {
                                    Alert a = new Alert(AlertType.NONE, configFileErr, ButtonType.OK);
                                    a.initOwner(m_stage);
                                    a.setHeaderText("Config File Error");
                                    a.setTitle("Config File Error - Setup - Local Node - Ergo Nodes");
                                    a.show();
                                }
                            } else {
                                Alert a = new Alert(AlertType.NONE, "Select an existing config text file, or select 'Basic' mode.", ButtonType.OK);
                                a.initOwner(m_stage);
                                a.setHeaderText("Config File");
                                a.setTitle("Config File - Setup - Local Node - Ergo Nodes");
                                a.show();
                            }
                        }
                    }
                } else {
                    Alert a = new Alert(AlertType.NONE, errorMsg, ButtonType.OK);
                    a.initOwner(m_stage);
                    a.setHeaderText("Directory Creation Error");
                    a.setTitle("Directory Creation Error - Setup - Local Node - Ergo Nodes");
                    a.show();
                }

            });

        } else {
            if (m_stage.isIconified()) {
                m_stage.setIconified(false);
            }
            m_stage.show();
            m_stage.toFront();
        }

    }

    public SimpleStringProperty consoleOutputProperty() {
        return m_consoleOutputProperty;
    }

    public void install(String configMode, String configFileName, String apiKeyString, File installDir, Scene initialScene) {
        if (m_stage == null) {
            setup();
        } else {
            Button installBtn = new Button("Install");
            Button backBtn = new Button("Back");

            // 
            SimpleBooleanProperty getLatestBoolean = new SimpleBooleanProperty(true);
            SimpleStringProperty downloadUrl = new SimpleStringProperty("https://github.com/ergoplatform/ergo/releases/download/v5.0.20/ergo-5.0.20.jar");
            SimpleStringProperty downloadFileName = new SimpleStringProperty("ergo-5.0.20.jar");
            SimpleObjectProperty<File> jarFile = new SimpleObjectProperty<File>(null);
            SimpleBooleanProperty updateBoolean = new SimpleBooleanProperty(true);
            SimpleBooleanProperty autoUpdateBoolean = new SimpleBooleanProperty(false);

            Scene finalSetupScene = getFinalSetupScene(installBtn, backBtn, jarFile, getLatestBoolean,updateBoolean, autoUpdateBoolean, downloadUrl, downloadFileName, m_stage);

            m_stage.setScene(finalSetupScene);
            Platform.runLater(()->m_stage.requestFocus());
            backBtn.setOnAction(e -> {
                m_stage.setScene(initialScene);
                m_stage.setHeight(SETUP_STAGE_HEIGHT);
            });

            installBtn.setOnAction(e -> {

                if (!getLatestBoolean.get() && jarFile.get() == null) {

                    Alert a = new Alert(AlertType.NONE, "Select an existing node file.", ButtonType.OK);
                    a.initOwner(m_configStage);
                    a.setHeaderText("Custom File");
                    a.setTitle("Custom File - Setup - Ergo Nodes");
                    a.show();

                } else {
                    ProgressBar progressBar = new ProgressBar();

                    boolean isDownload = getLatestBoolean.get();

                    Runnable installComplete = () -> {
                        m_update = updateBoolean.get();
                        m_autoUpdate = autoUpdateBoolean.get();
                        m_stage.close();
                        m_stage = null;

                        try {
                            if(m_nodeConfigData == null){
                                m_nodeConfigData = new ErgoNodeConfig(apiKeyString, configMode, DigestAccess.ALL, BlockchainMode.FULL, configFileName, m_appDir);
                            }else{
                                m_nodeConfigData.setConfigMode(configMode);
                                m_nodeConfigData.setConfigFileName(configFileName, false);
                                m_nodeConfigData.setAppDir(m_appDir);
                                //m_nodeConfigData.seStateMode(DigestAccess.ALL);
                                //m_nodeConfigData.blockchainMode(BlockchainMode.FULL);
                                m_nodeConfigData.setApiKey(apiKeyString);
                            }
                            m_isSetupProperty.set(true);
                            setNamedNodeUrl(new NamedNodeUrl(getId(), BlockchainMode.FULL, "127.0.0.1", ErgoNodes.MAINNET_PORT, apiKeyString, NetworkType.MAINNET));
                            start();

                        } catch (Exception e1) {
                            Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                            a.initOwner(m_stage);
                            a.setTitle("Config Creation Error - Setup - Ergo Nodes");
                            a.setHeaderText("Config Creation Error");
                            a.show();
                            m_stage.setScene(initialScene);

                        }

                    };

                    if (installDir.isDirectory()) {

                        if (isDownload) {
                            File appFile = new File(installDir + "/" + downloadFileName.get());
                            Button closeBtn = new Button();
                            Scene progressScene = App.getProgressScene(ErgoNodes.getSmallAppIcon(), "Downloading", "Setup - " + ErgoNodes.NAME, downloadFileName.get(), progressBar, m_stage, closeBtn);
                            m_stage.setScene(progressScene);
                            m_stage.setHeight(260);
                            SimpleBooleanProperty cancelDl = new SimpleBooleanProperty(false);

                            Utils.getUrlFileHash(downloadUrl.get(), appFile, getNetworksData().getExecService(), (onSucceeded) -> {
                                Object sourceObject = onSucceeded.getSource().getValue();
                                if (sourceObject != null && sourceObject instanceof HashData) {
                                    m_appDir = installDir;
                                    m_appFileName = downloadFileName.get();
                                    m_appFileHashData = (HashData) sourceObject;
                                    /*
                                    try {
                                        Files.writeString(logFile.toPath(), "\n" +m_appFileName +": " + m_appFileHashData.getJsonObject(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                    } catch (IOException e1) {

                                    }*/
                                    installComplete.run();
                                } else {
                                    Alert a = new Alert(AlertType.NONE, "Check the download URL and destination path and then try again.", ButtonType.OK);
                                    a.initOwner(m_stage);
                                    a.setTitle("Download Failed - Setup - Ergo Nodes");
                                    a.setHeaderText("Download Failed");
                                    a.show();
                                    m_stage.setScene(initialScene);
                                }
                            }, (onFailed) -> {
                                String errorString = onFailed.getSource().getException().toString();

                                Alert a = new Alert(AlertType.NONE, errorString, ButtonType.OK);
                                a.initOwner(m_stage);
                                a.setTitle("Error - Setup - Ergo Nodes");
                                a.setHeaderText("Error");
                                a.show();
                                m_stage.setScene(initialScene);
                            }, progressBar, cancelDl);
                            closeBtn.setOnAction(e1->{
                                cancelDl.set(true);
                                
                            });
                        } else {
                            File customFile = jarFile.get();

                            if (Utils.checkJar(customFile)) {

                                SimpleStringProperty fileNameProperty = new SimpleStringProperty(customFile.getName());
                                File appFile = new File(installDir.getAbsolutePath() + "/" + fileNameProperty.get());
                                Button closeBtn = new Button();
                                closeBtn.setDisable(true);
                                Scene progressScene = App.getProgressScene(ErgoNodes.getSmallAppIcon(), "Downloading", "Setup - " + ErgoNodes.NAME, fileNameProperty.get(), progressBar, m_stage, closeBtn);
                                m_stage.setScene(progressScene);

                                if (customFile.getAbsolutePath().equals(appFile.getAbsolutePath())) {

                                    try {

                                        m_appDir = installDir;
                                        m_appFileName = fileNameProperty.get();
                                        m_appFileHashData = new HashData(appFile);

                                        installComplete.run();
                                    } catch (Exception er) {

                                        Alert a = new Alert(AlertType.NONE, "Error: " + er.toString(), ButtonType.OK);
                                        a.initOwner(m_stage);
                                        a.setTitle("Error - Setup - Ergo Nodes");
                                        a.setHeaderText("Error");
                                        a.show();

                                        m_stage.setScene(initialScene);
                                    }
                                } else {

                                    Utils.moveFileAndHash(customFile, appFile, onSucceeded -> {

                                        Object sourceObject = onSucceeded.getSource().getValue();
                                        if (sourceObject != null && sourceObject instanceof HashData) {
                                            m_appDir = installDir;
                                            m_appFileName = fileNameProperty.get();
                                            m_appFileHashData = (HashData) sourceObject;

                                            installComplete.run();
                                        } else {
                                            Alert a = new Alert(AlertType.NONE, "Check the selected file and destination path and then try again.", ButtonType.OK);
                                            a.initOwner(m_stage);
                                            a.setTitle("Download Failed - Setup - Ergo Nodes");
                                            a.setHeaderText("Download Failed");
                                            a.show();
                                            m_stage.setScene(initialScene);
                                        }

                                    }, onFailed -> {

                                        String errorString = onFailed.getSource().getException().toString();

                                        Alert a = new Alert(AlertType.NONE, errorString, ButtonType.OK);
                                        a.initOwner(m_stage);
                                        a.setTitle("Error - Setup - Ergo Nodes");
                                        a.setHeaderText("Error");
                                        a.show();
                                        m_stage.setScene(initialScene);

                                    }, progressBar);

                                }
                             

                            } else {
                                Alert a = new Alert(AlertType.NONE, "Select a valid Ergo core file. (ergo-<Version>.jar)", ButtonType.OK);
                                a.initOwner(m_stage);
                                a.setTitle("Invalid Core File - Setup - Ergo Nodes");
                                a.setHeaderText("Invalid Core File");
                                a.show();

                                m_stage.setScene(initialScene);
                            }

                        }

                    } else {
                        Alert a = new Alert(AlertType.NONE, "File system cannote be accessed.", ButtonType.OK);
                        a.initOwner(m_stage);
                        a.setTitle("File System Error - Setup - Ergo Nodes");
                        a.setHeaderText("File System Error");
                        a.show();

                    }

                }

            });
        }

    }

    @Override
    public void stop() {

        if (!statusProperty().get().equals(ErgoMarketsData.STOPPED)) {

            if(!Utils.sendTermSig(m_appFileName)){
                reset();
            }else{
                
            }
      
        }
    }

    public void kill() {
        if (!statusProperty().get().equals(ErgoMarketsData.STOPPED)) {
        
            Alert a = new Alert(AlertType.NONE, "This mode may be corrupted. Would you like to force shutdown and re-sync?", ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = a.showAndWait();
            
            if(result.isPresent() && result.get() == ButtonType.YES){
                Utils.sendKillSig(m_appFileName);
                
                try {
                    cleanSync();
                } catch (IOException e) {

                }
            }

        }

    }

    public boolean getIsSetup() {
        return m_isSetupProperty.get();
    }

    public void resetToDefault() {
    
        m_appFileName = null;
        isAvailableProperty().set(false);
        m_nodeBlockHeightProperty.set(-1);
        m_networkBlockHeightProperty.set(-1);
        m_nodeHeadersHeightProperty.set(-1);
        m_peerCountProperty.set(-1);
        namedNodeUrlProperty().set(new NamedNodeUrl(namedNodeUrlProperty().get().getId(), "Not Installed", "127.0.0.1", ErgoNodes.MAINNET_PORT, "", NetworkType.MAINNET));
        m_isSetupProperty.set(false);
        lastUpdated().set(LocalDateTime.now());
    }
    public String getCurrentStatus(){
        String currentStatus = "Offline";
        if (!statusProperty().get().equals(ErgoMarketsData.STOPPED)) {

            boolean synced = isAvailableProperty().get();
          //  int peerCount = m_peerCountProperty.get();
            long networkBlockHeight = m_networkBlockHeightProperty.get();
            long nodeBlockHeight = m_nodeBlockHeightProperty.get();
            long headersHeight = m_nodeHeadersHeightProperty.get();

            if (!synced) {

                //    botMidText.setFill(getSecondaryColor());
              
                if (networkBlockHeight == -1) {
                    currentStatus = ("Getting status...");
                } else {

                    if (nodeBlockHeight == -1) {
                        if(headersHeight > -1){
                            String headersHeightString = headersHeight == -1 ? "Getting headers..." : headersHeight + "";
                            String networkBlockHeightString = networkBlockHeight == -1 ? "Getting Network Height..." : networkBlockHeight + "";
                            currentStatus = ("Syncing headers: " + headersHeightString + "/" + networkBlockHeightString);
                            
                        }else{
                            //int seconds = (SYNC_TIMEOUT_CYCLE - m_isSyncStuck.get());
                            currentStatus = ("Finding next block... " + (m_isSyncStuck.get() > 2 ? (" (Timing out...") : ""));
                        } 
                    } else {
                        String nodeBlockHeightString = nodeBlockHeight == -1 ? "Getting Node Height..." : nodeBlockHeight + "";
                        String networkBlockHeightString = networkBlockHeight == -1 ? "Getting Network Height..." : networkBlockHeight + "";

                        currentStatus = ("Syncing: " + nodeBlockHeightString + "/" + networkBlockHeightString);
                    }

                }

                //+ " (" + String.format("%.1f", p * 100) + ")");
                // }
            } else {

                currentStatus = ("Ready");
             
            }
        } else {

            currentStatus = (getIsSetup() ? "Offline" : "(Not Installed)");

        }

        return currentStatus;
    }

    @Override
    public HBox getStatusBox() {

        //   statusString.set(getIsSetup() ? "Offline" : "(Not Installed)");
        Text middleTopRightText = new Text();
        middleTopRightText.setFont(getFont());
        middleTopRightText.setFill(getSecondaryColor());

        middleTopRightText.textProperty().bind(cmdProperty());

        Text middleBottomRightText = new Text(getNetworkTypeString());
        middleBottomRightText.setFont(getFont());
        middleBottomRightText.setFill(getPrimaryColor());

        VBox centerRightBox = new VBox(middleTopRightText, middleBottomRightText);
        centerRightBox.setAlignment(Pos.CENTER_RIGHT);

        Text topInfoStringText = new Text();
        topInfoStringText.setFont(getFont());
        topInfoStringText.setFill(getPrimaryColor());
        topInfoStringText.textProperty().bind(namedNodeUrlProperty().asString());

        Text topRightText = new Text();
        topRightText.setFont(getSmallFont());
        topRightText.setFill(getSecondaryColor());

        Text botTimeText = new Text();
        botTimeText.setFont(getSmallFont());
        botTimeText.setFill(getSecondaryColor());
        botTimeText.textProperty().bind(cmdStatusUpdated());

        Text centerText = new Text();
        centerText.setFont(App.txtFont);
        centerText.setFill(getPrimaryColor());

        Region topMiddleRegion = new Region();
        HBox.setHgrow(topMiddleRegion, Priority.ALWAYS);

        HBox topBox = new HBox(topInfoStringText, topMiddleRegion, topRightText);
        //    topBox.setId("darkBox");

        Text ipText = new Text(namedNodeUrlProperty().get().getUrlString());
        ipText.setFill(getPrimaryColor());
        ipText.setFont(getSmallFont());

        Text syncText = new Text();
        syncText.setFill(isAvailableProperty().get() ? getPrimaryColor() : getSecondaryColor());
        syncText.setFont(getSmallFont());

        Region lbotRegion = new Region();
        lbotRegion.setMinWidth(5);
        HBox.setHgrow(lbotRegion, Priority.ALWAYS);

        Region rbotRegion = new Region();
        rbotRegion.setMinWidth(5);
        HBox.setHgrow(rbotRegion, Priority.ALWAYS);

        HBox bottomBox = new HBox(ipText, lbotRegion, syncText, rbotRegion, botTimeText);
        // bottomBox.setId("darkBox");
        bottomBox.setAlignment(Pos.CENTER_LEFT);

        //syncText.prefWidthProperty().bind(bottomBox.widthProperty().subtract(ipText.layoutBoundsProperty().get().getWidth()).subtract(botTimeText.layoutBoundsProperty().get().getWidth()));
        HBox.setHgrow(bottomBox, Priority.ALWAYS);

        HBox centerTextBox = new HBox(centerText);
        HBox.setHgrow(centerTextBox, Priority.ALWAYS);
        centerTextBox.setAlignment(Pos.CENTER);

        HBox centerBox = new HBox(centerTextBox, centerRightBox);
        centerBox.setPadding(new Insets(0, 5, 0, 5));
        centerBox.setAlignment(Pos.CENTER_RIGHT);
        // centerBox.setId("darkBox");

        VBox bodyBox = new VBox(topBox, centerBox, bottomBox);
        HBox.setHgrow(bodyBox, Priority.ALWAYS);

        statusString().addListener((obs,oldval,newval)->{
            centerText.setText(newval);
        });

        namedNodeUrlProperty().addListener((obs, oldval, newval) -> {
            ipText.setText(newval != null ? newval.getUrlString() : "Address unknown");
        });

        //double width = bottomBox.layoutBoundsProperty().get().getWidth() - ipText.layoutBoundsProperty().get().getWidth() - botTimeText.layoutBoundsProperty().get().getWidth();
        // syncField.minWidthProperty().bind(rowBox.widthProperty().subtract(botTimeText.layoutBoundsProperty().get().getWidth()).subtract(200));
        start();

        HBox contentsBox = new HBox(bodyBox);
        HBox.setHgrow(contentsBox, Priority.ALWAYS);
        contentsBox.setId("bodyRowBox");
        contentsBox.setPadding(new Insets(0, 10, 0, 10));
        return contentsBox;
    }

    @Override
    public void remove(){
        NamedNodeUrl namedNode = getNamedNodeUrl();
        if(namedNode != null){
            Alert a = new Alert(AlertType.NONE, "Would you like to remove:\n\n" + namedNode.getName() + "\nhttp://" + namedNode.getUrlString(), ButtonType.NO, ButtonType.YES);
            Optional<ButtonType> btnType = a.showAndWait();
            if(btnType.isPresent() && btnType.get() == ButtonType.YES){
                getErgoNodesList().remove(getId());
                delete(null,null);
            }
        }else{
            super.remove();
        }
        
    }
    

    @Override
    public HBox getRowItem() {

        Tooltip defaultIdTip = new Tooltip(getErgoNodesList().defaultNodeIdProperty().get() != null && getErgoNodesList().defaultNodeIdProperty().get().equals(getId()) ? "Default Node" : "Set default");
        defaultIdTip.setShowDelay(new Duration(100));
        BufferedButton defaultIdBtn = new BufferedButton(getErgoNodesList().defaultNodeIdProperty().get() != null && getErgoNodesList().defaultNodeIdProperty().get().equals(getId()) ? getRadioOnUrl() : getRadioOffUrl(), 15);
        defaultIdBtn.setTooltip(defaultIdTip);
        defaultIdBtn.setOnAction(e -> {
            String currentDefaultId = getErgoNodesList().defaultNodeIdProperty().get();
            if (currentDefaultId != null && currentDefaultId.equals(getId())) {
                getErgoNodesList().defaultNodeIdProperty().set(null);
            } else {
                getErgoNodesList().defaultNodeIdProperty().set(getId());
            }
        });

        getErgoNodesList().defaultNodeIdProperty().addListener((obs, oldval, newval) -> {
            defaultIdBtn.setImage(new Image(newval != null && newval.equals(getId()) ? getRadioOnUrl() : getRadioOffUrl()));
            defaultIdTip.setText(newval != null && newval.equals(getId()) ? "Default Node" : "Set default");
        });



        BufferedMenuButton itemMenuBtn = new BufferedMenuButton();
        itemMenuBtn.setPadding(new Insets(0));

        MenuItem settingsItem = new MenuItem("Settings");
        settingsItem.setOnAction(e->{
            openSettings();
        });

        MenuItem openInBrowserItem = new MenuItem("Show in browser");
        openInBrowserItem.setOnAction((e)->{
            openInBrowser();
        });
 
        MenuItem removeItem = new MenuItem("Remove");
        removeItem.setOnAction(e->{
            remove();
        });
        

        Text topInfoStringText = new Text();
        topInfoStringText.setFont(getFont());
        topInfoStringText.setFill(getPrimaryColor());
        topInfoStringText.textProperty().bind(namedNodeUrlProperty().asString());

        Text topRightText = new Text();
        topRightText.setFont(getSmallFont());
        topRightText.setFill(getSecondaryColor());

        TextField topStatusField = new TextField();
        topStatusField.setId("formFieldSmall");
        topStatusField.setEditable(false);
        topStatusField.setAlignment(Pos.CENTER);
        topStatusField.setPadding(new Insets(0, 10, 0, 10));
        HBox.setHgrow(topStatusField, Priority.ALWAYS);
        topStatusField.textProperty().bind(m_warningsProperty);

        Text botTimeText = new Text();
        botTimeText.setFont(getSmallFont());
        botTimeText.setFill(getSecondaryColor());
        botTimeText.textProperty().bind(cmdStatusUpdated());

        TextField centerField = new TextField(statusString().get());
        centerField.setFont(getLargeFont());
        centerField.setId("formField");
        centerField.setEditable(false);
        centerField.setAlignment(Pos.CENTER);
        centerField.setPadding(new Insets(0, 10, 0, 0));


       

    

        Text middleTopRightText = new Text();
        middleTopRightText.setFont(getFont());
        middleTopRightText.setFill(getSecondaryColor());

        middleTopRightText.textProperty().bind(cmdProperty());

        Text middleBottomRightText = new Text(getNetworkTypeString());
        middleBottomRightText.setFont(getFont());
        middleBottomRightText.setFill(getPrimaryColor());

        VBox centerRightBox = new VBox(middleTopRightText, middleBottomRightText);
        centerRightBox.setAlignment(Pos.CENTER_RIGHT);

        VBox.setVgrow(centerRightBox, Priority.ALWAYS);

        Tooltip statusBtnTip = new Tooltip("");
        statusBtnTip.setShowDelay(new Duration(100));
        //m_startImgUrl : m_stopImgUrl
        String statusBtnImg = statusProperty().get().equals(ErgoMarketsData.STOPPED) ? (getIsSetup() && getAppFile() != null ? getStartImgUrl() : getInstallImgUrl()) : getStopImgUrl();
        BufferedButton statusBtn = new BufferedButton(statusBtnImg, 15);
        statusBtn.setId("statusBtn");
        statusBtn.setPadding(new Insets(0, 10, 0, 10));
        statusBtn.setTooltip(statusBtnTip);
        statusBtn.setOnAction(action -> {
            if (statusProperty().get().equals(ErgoMarketsData.STOPPED)) {

                start();

            } else {
                stop();

            }
        });

        HBox leftBox = new HBox(defaultIdBtn);
        leftBox.setPadding(new Insets(5));
        HBox rightBox = new HBox(statusBtn);

        leftBox.setAlignment(Pos.CENTER_LEFT);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        leftBox.setId("bodyBox");
        rightBox.setId("bodyBox");

        Region currencySpacer = new Region();
        currencySpacer.setMinWidth(10);

        HBox centerBox = new HBox(itemMenuBtn, centerField, centerRightBox);
        centerBox.setPadding(new Insets(0, 5, 0, 5));
        centerBox.setAlignment(Pos.CENTER_RIGHT);
        centerBox.setId("darkBox");


        centerField.prefWidthProperty().bind(centerBox.widthProperty().subtract(centerRightBox.widthProperty()).subtract(20));

        MenuItem startItem = new MenuItem("Install");
        startItem.setOnAction(e->{
            statusBtn.fire();          
        });


        HBox topSpacer = new HBox();
        HBox bottomSpacer = new HBox();

        topSpacer.setMinHeight(2);
        bottomSpacer.setMinHeight(2);

        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
        topSpacer.setId("bodyBox");
        bottomSpacer.setId("bodyBox");


        

        HBox topBox = new HBox(topInfoStringText, topStatusField, topRightText);
        topBox.setId("darkBox");

        Text ipText = new Text(namedNodeUrlProperty().get().getUrlString());
        ipText.setFill(getPrimaryColor());
        ipText.setFont(getSmallFont());

        TextField bottomMiddleText = new TextField();
        bottomMiddleText.setId("formFieldSmall");
        bottomMiddleText.setPadding(new Insets(0,0,0,0));
        bottomMiddleText.setAlignment(Pos.CENTER);
        HBox.setHgrow(bottomMiddleText, Priority.ALWAYS);

        Binding<String> peerBinding = Bindings.createObjectBinding(()->m_peerCountProperty.get() > -1 ? " Peers: " + m_peerCountProperty.get() + " " : "", m_peerCountProperty);

        Binding<String> networkHeightBinding = Bindings.createObjectBinding(()->m_nodeBlockHeightProperty.get() > -1 ? " Height: " + m_nodeBlockHeightProperty.get() + " " : "", m_nodeBlockHeightProperty);

        
        Binding<String> syncPercent = Bindings.createObjectBinding(()->m_nodeBlockHeightProperty.get() != -1 && m_networkBlockHeightProperty.get() != -1 ? " Sync: " + String.format("%.2f",(new BigDecimal(m_nodeBlockHeightProperty.get()).divide(new BigDecimal(m_networkBlockHeightProperty.get()), 4, RoundingMode.HALF_UP)).multiply(new BigDecimal(100))) + "% " : "", m_nodeBlockHeightProperty, m_networkBlockHeightProperty);

       
        bottomMiddleText.textProperty().bind(Bindings.concat(networkHeightBinding, syncPercent, peerBinding));

        HBox bottomBox = new HBox(ipText, bottomMiddleText,  botTimeText);
        bottomBox.setId("darkBox");
        bottomBox.setAlignment(Pos.CENTER_LEFT);

        //bottomMiddleText.prefWidthProperty().bind(bottomBox.widthProperty().subtract(ipText.layoutBoundsProperty().get().getWidth()).subtract(botTimeText.layoutBoundsProperty().get().getWidth()));
        HBox.setHgrow(bottomBox, Priority.ALWAYS);

        VBox bodyBox = new VBox(topSpacer, topBox, centerBox, bottomBox, bottomSpacer);
        HBox.setHgrow(bodyBox, Priority.ALWAYS);


        HBox contentsBox = new HBox(leftBox, bodyBox, rightBox);
        // contentsBox.setId("rowBox");
        HBox.setHgrow(contentsBox, Priority.ALWAYS);

        HBox rowBox = new HBox(contentsBox);
        rowBox.setPadding(new Insets(0, 0, 5, 0));
        rowBox.setAlignment(Pos.CENTER_RIGHT);
        //  rowBox.setId("unselected");
        HBox.setHgrow(rowBox, Priority.ALWAYS);
        // rowBox.setId("rowBox");

        statusString().addListener((obs,oldval,newval)->{
            centerField.setText(newval);
        });
        
        Runnable checkStatus = () -> {
            itemMenuBtn.getItems().clear();
            String value = statusProperty().get() == null ? ErgoMarketsData.STOPPED : statusProperty().get();

            if (value.equals(ErgoMarketsData.STOPPED)) {
                String stoppedString = getIsSetup() ? "Start" : "Setup";
                startItem.setText(stoppedString);
                statusBtnTip.setText(stoppedString);
                statusBtn.setImage(new Image(getIsSetup() && getAppFile() != null ? getStartImgUrl() : getInstallImgUrl()));
                
            } else {
                String stoppedString = getIsSetup() ? "Stop" : "Setup";
                startItem.setText(stoppedString);
                statusBtnTip.setText(stoppedString);
                statusBtn.setImage(new Image(getStopImgUrl()));
            }
            if(getIsSetup()){
                
                itemMenuBtn.getItems().addAll( settingsItem, openInBrowserItem, removeItem,new SeparatorMenuItem(), startItem);
            }else{
                
                itemMenuBtn.getItems().add(startItem);
            }
            topRightText.setText(m_appFileName != null ? m_appFileName : "");
        };

        checkStatus.run();
        isSetupProperty().addListener((obs,oldval,newval)->{
            checkStatus.run();
        });
        statusProperty().addListener((obs, oldval, newval) -> {
            checkStatus.run();
        });
        

        rowBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if(e.getClickCount() == 2){
                open();
                e.consume();
            }else{
        
                getErgoNodesList().selectedIdProperty().set(getId());
                e.consume();
                
            }
            
        });
 

        Runnable updateSelected = () -> {
            String selectedId = getErgoNodesList().selectedIdProperty().get();
            boolean isSelected = selectedId != null && getId().equals(selectedId);

            centerField.setId(isSelected ? "selectedField" : "formField");
            rowBox.setId(isSelected ? "selected" : "unSelected");
        };

        //double width = bottomBox.layoutBoundsProperty().get().getWidth() - ipText.layoutBoundsProperty().get().getWidth() - botTimeText.layoutBoundsProperty().get().getWidth();
        // syncField.minWidthProperty().bind(rowBox.widthProperty().subtract(botTimeText.layoutBoundsProperty().get().getWidth()).subtract(200));
        getErgoNodesList().selectedIdProperty().addListener((obs, oldval, newVal) -> updateSelected.run());
        updateSelected.run();

        return rowBox;
    }
    @Override
    public void open(){
        if(getIsSetup()){
            String statusString = statusProperty().get();

            if(statusString.equals( ErgoMarketsData.STOPPED)){
                openSettings();
            }else{
                openInBrowser();
            }
        }else{
            openSettings();
        }
    }

    @Override
    public String getName() {
        return "Local Node (" + super.getName() + ")";
    }

    public void setExecParams(String params) {
        m_execParams = params;
    }

    public String getExecParams() {

        return m_execParams;
    }

    @Override
    public JsonObject getJsonObject() {
        NamedNodeUrl namedNodeUrl = namedNodeUrlProperty().get();

        JsonObject json = new JsonObject();

        if (namedNodeUrl != null) {
            json.add("namedNode", namedNodeUrl.getJsonObject());
        }

        json.addProperty("runOnStart", m_runOnStart);
        json.addProperty("update", m_update);
        json.addProperty("autoUpdate", m_autoUpdate);

        if (m_nodeConfigData != null) {
            json.add("config", m_nodeConfigData.getJsonObject());
        }

        if (m_appDir != null && m_appDir.isDirectory()) {
            json.addProperty("appDir", getAppDir().getAbsolutePath());
        }
        if (m_appFileName != null && getAppFile() != null) {
            json.addProperty("appFileName", m_appFileName);
        }
        if (m_appFileHashData != null) {
            json.add("appFileHashData", m_appFileHashData.getJsonObject());
        }
        if(m_appVersion.get() != null){
            json.addProperty("appVersion", m_appVersion.get().get());
        }
        json.addProperty("appExecParams", m_execParams);
        
    

        return json;

    }

    

    public void openAppFile(Stage stage, Runnable onSuccess, Runnable onFailed) {
        stop();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select 'Ergo Core (.jar)'");
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Ergo Core (.jar)", "*.jar"));
        File coreFile = chooser.showOpenDialog(stage);
        String errorString = "File cannot be opened.";
        if (coreFile != null) {
            if (Utils.checkJar(coreFile)) {

                try {
                    final String coreFileName = coreFile.getName();
                    final HashData hashData = new HashData(coreFile);
                    if (!coreFile.getAbsolutePath().equals(m_appDir.getAbsolutePath())) {
                        Files.move(coreFile.toPath(), new File(m_appDir.getAbsolutePath() + "/" + coreFileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    //final Version fileNameVersion = Utils.getFileNameVersion(coreFileName);

                  //  Platform.runLater(() -> {
                        m_appFileHashData = hashData;
                        m_appFileName = coreFileName;
                        lastUpdated().set(LocalDateTime.now());
                        onSuccess.run();
                  //  });

                } catch (Exception e1) {
                    Alert a = new Alert(AlertType.NONE, errorString + ":\n\n" + e1, ButtonType.OK);
                    a.setHeaderText("Error Opening");
                    a.setTitle("Error Opening - Settings - Local Node - Ergo Nodes");
                    a.initOwner(m_stage);
                    a.show();
                    onFailed.run();
                    //Platform.runLater(() -> onFailed.run());

                }

            } else {
                Alert a = new Alert(AlertType.NONE, errorString, ButtonType.OK);
                a.setHeaderText("Error Opening");
                a.setTitle("Error Opening - Settings - Local Node - Ergo Nodes");
                a.initOwner(m_stage);
                a.show();
                //Platform.runLater(() -> onFailed.run());
                onFailed.run();
            }
        }
    }
   
    public void checkForupdate(Runnable next){
        if(!m_checking.get()){
            m_checking.set( true);
            m_gitHubAPI.getAssetsLatest(getNetworksData().getExecService(), (onFinished)->{
                Object finishedObject = onFinished.getSource().getValue();
                if(finishedObject != null && finishedObject instanceof GitHubAsset[] && ((GitHubAsset[]) finishedObject).length > 0){
            
                    GitHubAsset[] assets = (GitHubAsset[]) finishedObject;

                    GitHubAsset fileAsset = assets[0];

                    String name = fileAsset.getName();
                    //String url = fileAsset.getUrl();

                    

                    Version newVersion = Utils.getFileNameVersion(name);
                    Version prevVersion =  m_appVersion.get() != null && !m_appVersion.get().get().equals("0.0.0") ? m_appVersion.get() : null;
                   
                    prevVersion = prevVersion == null && m_appFileName != null ? Utils.getFileNameVersion(m_appFileName) : null; 
                    

                    if (prevVersion != null && newVersion.compareTo(prevVersion) > 0) {
                        TextField updateConfirmField = new TextField();
                        Button closeBtn = new Button();
                        if(m_stage != null){
                            m_stage.close();
                            m_stage = null;
                            
                        }
                        m_stage = new Stage();
                        m_stage.getIcons().add(getIcon());
                        m_stage.initStyle(StageStyle.UNDECORATED);
                        if(!m_autoUpdate){
                            App.showGetTextInput("Update? ("+newVersion.get()+") (Y/n)", "Update", ErgoNodes.getAppIcon(), updateConfirmField, closeBtn, m_stage);
                            
                            closeBtn.setOnAction(e->{
                                if(m_stage != null){
                                    m_stage.close();
                                    m_stage = null;
                                }
                                m_checking.set( false);
                                if(next != null){
                                    next.run();
                                }
                            });
                            m_stage.setOnCloseRequest(e->closeBtn.fire());

                            updateConfirmField.setOnKeyPressed(e1 -> {
                                KeyCode keyCode = e1.getCode();

                                Runnable updateFailed = ()->{
                                    Alert a = new Alert(AlertType.NONE, "Unable to update at this time.", ButtonType.OK);
                                    a.initOwner(m_stage);
                                    a.setHeaderText("Update Failed");
                                    a.setTitle("Update Failed");
                                    a.show();
                                   
                                    closeBtn.fire();
                                };

                                if(keyCode == KeyCode.ENTER || keyCode == KeyCode.Y){
                                    if(!statusProperty().get().equals(ErgoMarketsData.STOPPED)){
                                        statusProperty().addListener((obs,oldval,newval)->{
                                            updateAppFile(assets, m_stage, null, ()->{
                                                
                                                closeBtn.fire();
                                            }, ()->updateFailed.run(), ()->updateFailed.run());
                                        });
                                        stop();
                             
                                    }else{
                                        updateAppFile(assets, m_stage, null, ()->closeBtn.fire(), ()->updateFailed.run(), ()->updateFailed.run());
                                    }
                                }else{
                                    if(keyCode == KeyCode.N){
                                        closeBtn.fire();
                                    }else{
                                        updateConfirmField.setText("");
                                    }
                                }
                            });
                        }else{
                             if(!statusProperty().get().equals(ErgoMarketsData.STOPPED)){
                                statusProperty().addListener((obs,oldval,newval)->{
                                    updateAppFile(assets, m_stage, null, ()->{
                                         m_checking.set( false);
                                        next.run();
                                    }, ()->{
                                         m_checking.set( false);
                                        next.run();
                                    }, ()->{
                                     m_checking.set( false);
                                        next.run();
                                    });
                                });
                                stop();

                            }else{
                                updateAppFile(assets, null, null, ()->{
                                    m_checking.set( false);
                                    next.run();
                                },()->{
                                    m_checking.set( false);
                                    next.run();
                                },()->{
                                    m_checking.set( false);
                                    next.run();
                                });
                            }          
                        }
                    }else{
                        next.run();
                    }
                
                }else{
                    next.run();
                }
            },onfailed->{
                if(next != null){
                    m_checking.set(false);
                    next.run();
                }
            });
        }
    }

    public void updateAppFile(GitHubAsset[] assets, Stage stage, Scene previousScene, Runnable onComplete, Runnable noUpdate, Runnable failed) {

        Rectangle rect = getErgoNodesList().getErgoNodes().getNetworksData().getMaximumWindowBounds();

        GitHubAsset fileAsset = assets[0];

        String name = fileAsset.getName();
        String url = fileAsset.getUrl();

        Version newVersion = Utils.getFileNameVersion(name);
        Version prevVersion = m_appVersion.get() != null ? m_appVersion.get() : new Version();
        File prevAppFile = getAppFile();

        if (newVersion.compareTo(prevVersion) > 0 || prevAppFile == null) {

            

            File appFile = new File(m_appDir.getAbsolutePath() + "/" + name);
            ProgressBar progressBar = new ProgressBar();
            Button closeBtn = new Button();
            if (stage != null) {
                Scene progressScene = App.getProgressScene(ErgoNodes.getSmallAppIcon(), "Downloading", "Settings - Ergo Local Node - " + ErgoNodes.NAME, name, progressBar, stage, closeBtn);
                stage.setScene(progressScene);

                stage.setX((rect.getWidth() / 2) - (stage.getWidth() / 2));
                stage.setY((rect.getHeight() / 2) - (stage.getHeight() / 2));
            }
            SimpleBooleanProperty cancel = new SimpleBooleanProperty(false);
            Utils.getUrlFileHash(url, appFile, getNetworksData().getExecService(), (onDlSucceeded) -> {
                Object dlObject = onDlSucceeded.getSource().getValue();
                if (dlObject != null && dlObject instanceof HashData) {
                    m_appFileHashData = (HashData) dlObject;
                    m_appFileName = name;
                    m_appVersion.set(newVersion);

                    if (m_deleteOldFiles) {
                        if (prevAppFile != null && !prevAppFile.getName().equals(name) && prevAppFile.isFile()) {
                            prevAppFile.delete();
                        }
                    }
                    lastUpdated().set(LocalDateTime.now());

                    if (onComplete != null) {
                        onComplete.run();
                    }
                    if (stage != null) {

                        stage.setScene(previousScene);
                        stage.setX((rect.getWidth() / 2) - (stage.getWidth() / 2));
                        stage.setY((rect.getHeight() / 2) - (stage.getHeight() / 2));
                    }
                } else {
                    Alert a = new Alert(AlertType.NONE, "The download returned an invalid file.", ButtonType.OK);
                    a.initOwner(stage);
                    a.setTitle("Download Failed - Setup - Ergo Nodes");
                    a.setHeaderText("Download Failed");
                    a.show();
                    if(failed != null){
                        failed.run();
                    }
                    if (stage != null) {
                        stage.setScene(previousScene);
                        stage.setScene(previousScene);
                        stage.setX((rect.getWidth() / 2) - (stage.getWidth() / 2));
                        stage.setY((rect.getHeight() / 2) - (stage.getHeight() / 2));
                    }
                }
            }, (onDlFailed) -> {
                String errorString = onDlFailed.getSource().getException().toString();

                Alert a = new Alert(AlertType.NONE, errorString, ButtonType.OK);
                a.initOwner(stage);
                a.setTitle("Error - Setup - Ergo Nodes");
                a.setHeaderText("Error");
                a.show();
                if(failed != null){
                    failed.run();
                }
                if (stage != null) {
                    stage.setScene(previousScene);
                    stage.setScene(previousScene);
                    stage.setX(rect.getWidth() - (stage.getWidth() / 2));
                    stage.setY(rect.getHeight() - (stage.getHeight() / 2));
                }
            }, stage != null ? progressBar : null, cancel);

            closeBtn.setOnAction(e->cancel.set(true));

        } else {
            if (noUpdate != null) {
                noUpdate.run();
            }
        }
       
            
              
      
    }

    public void delete(Stage stage, Button closeBtn){
        stop();
        if(m_appDir != null){
            Alert a = new Alert(AlertType.NONE, "Delete all local node files?", ButtonType.YES, ButtonType.NO);
            a.setTitle("Remove Node - " + getNamedNodeUrl().getName() + " - Ergo Nodes");
            a.setHeaderText("Delete Node");
            if(stage != null){
                a.initOwner(stage);
            }
            Optional<ButtonType> result = a.showAndWait();

            if (result != null && result.isPresent()) {
                if(closeBtn != null){
                    closeBtn.fire();
                }

                if (result.get() == ButtonType.YES) {

                    try {
                        FileUtils.deleteDirectory(m_appDir);
                    } catch (IOException e1) {
                        Alert a1 = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                        a1.setTitle("Error");
                        if(stage != null){
                            a1.initOwner(stage);
                        }
                        a1.setHeaderText("Error");
                        a1.show();
                    }
                     
                    resetToDefault();

                }
            }
        }else{
            resetToDefault();
        }
    }

 
    private String m_downArrowUrlString = "/assets/caret-down-15.png";
    private String m_upArrowUrlString = "/assets/caret-up-15.png";

    private Scene settingsScene(Stage stage, Runnable closeStage) {
        if(!checkValidSetup())
        {
          
            return null;
        }

        String titleString = "Settings - Local Node - " + ErgoNodes.NAME;

        stage.setTitle(titleString);
        
        VBox layoutBox = new VBox();
        Scene settingsScene = new Scene(layoutBox, SETTINGS_STAGE_WIDTH, SETTINGS_STAGE_HEIGHT);
        settingsScene.setFill(null);
        settingsScene.getStylesheets().add("/css/startWindow.css");


        Image icon = ErgoNodes.getSmallAppIcon();
        double defaultRowHeight = 40;
        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(icon, titleString, closeBtn, stage);

        Text headingText = new Text("Settings");
        headingText.setFont(App.txtFont);
        headingText.setFill(Color.WHITE);

        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(defaultRowHeight);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 10, 10, 10));
        headingBox.setId("headingBox");

        HBox headingPaddingBox = new HBox(headingBox);

        headingPaddingBox.setPadding(new Insets(5, 0, 2, 0));

        VBox headerBox = new VBox(headingPaddingBox);

        headerBox.setPadding(new Insets(0, 5, 0, 5));

        Tooltip setupTooltip = new Tooltip("Setup");
        setupTooltip.setShowDelay(new Duration(100));

        BufferedButton setupBtn = new BufferedButton(getInstallImgUrl(), App.MENU_BAR_IMAGE_WIDTH);
        setupBtn.setTooltip(setupTooltip);
        setupBtn.setOnAction(e -> {
            closeBtn.fire();
            setup();
        });
        Tooltip deleteTip = new Tooltip("Remove Node");
        deleteTip.setShowDelay(new Duration(100));

        BufferedButton deleteBtn = new BufferedButton("/assets/trash-outline-white-30.png", App.MENU_BAR_IMAGE_WIDTH);
        deleteBtn.setTooltip(deleteTip);

        deleteBtn.setOnAction(e -> {
            delete(stage, closeBtn);
        });

        TextField appDirField = new TextField(m_appDir.getAbsolutePath());
        appDirField.setId("urlField");
        appDirField.setEditable(false);

        HBox.setHgrow(appDirField, Priority.ALWAYS);

        Tooltip navTooltip = new Tooltip("Open in File Explorer");
        navTooltip.setShowDelay(new Duration(100));

        BufferedButton navBtn = new BufferedButton("/assets/navigate-outline-white-30.png", App.MENU_BAR_IMAGE_WIDTH);
        navBtn.setText("Location");
        navBtn.setGraphicTextGap(15);
        navBtn.setId("titleBtn");
        navBtn.setContentDisplay(ContentDisplay.RIGHT);
        navBtn.setTooltip(navTooltip);
        navBtn.setOnAction(e -> {
            try {
                Utils.openDir(m_appDir);
            } catch (Exception e1) {
                Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                a.setTitle("Error");
                a.initOwner(stage);
                a.setHeaderText("Error");
                a.show();
            }
        });

        HBox menuBar = new HBox(navBtn, appDirField, setupBtn, deleteBtn);

        menuBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setId("menuBar");

        HBox menubarPaddingBox = new HBox(menuBar);

        menubarPaddingBox.setPadding(new Insets(5, 0, 2, 0));

        VBox menuBarBox = new VBox(menubarPaddingBox);

        menuBarBox.setPadding(new Insets(0, 5, 0, 5));

        SimpleDoubleProperty rowHeight = new SimpleDoubleProperty(defaultRowHeight);

        Text configText = new Text("Config");
        configText.setFill(App.txtColor);
        configText.setFont(App.txtFont);

        HBox configBox = new HBox(configText);
        configBox.setAlignment(Pos.CENTER_LEFT);
        configBox.setMinHeight(40);
        configBox.setId("headingBox");
        configBox.setPadding(new Insets(0, 0, 0, 15));

       

        Text apiKeyText = new Text(String.format("%-14s", "API Key"));
        apiKeyText.setFill(getPrimaryColor());
        apiKeyText.setFont((App.txtFont));

        TextField apiKeyField = new TextField();
        apiKeyField.setPromptText("Enter key");
        apiKeyField.setId("formField");
        HBox.setHgrow(apiKeyField, Priority.ALWAYS);

        Button showKeyBtn = new Button("(Click to view)");
        showKeyBtn.setId("rowBtn");
        showKeyBtn.setPrefWidth(250);
        showKeyBtn.setPrefHeight(30);
        showKeyBtn.setAlignment(Pos.CENTER_LEFT);


        Runnable updateKey = ()->{
            String keyString = apiKeyField.getText();

                try {
                    m_nodeConfigData.setApiKey(keyString);
                    NamedNodeUrl newNamedNodeUrl = getNamedNodeUrl();
                    newNamedNodeUrl.setApiKey(keyString);
                    setNamedNodeUrl(newNamedNodeUrl);
                    
                } catch (Exception e1) {
              
                }
                
               
   

        };

        Tooltip randomApiKeyTip = new Tooltip("Random API Key");

        BufferedButton hideKeyBtn = new BufferedButton("/assets/eye-off-30.png", App.MENU_BAR_IMAGE_WIDTH);
        BufferedButton saveKeyBtn = new BufferedButton("/assets/save-30.png", App.MENU_BAR_IMAGE_WIDTH);
        BufferedButton randomApiKeyBtn = new BufferedButton("/assets/d6-30.png", App.MENU_BAR_IMAGE_WIDTH);

     

        randomApiKeyBtn.setTooltip(randomApiKeyTip);
        randomApiKeyBtn.setOnAction(e -> {
            try {
                int length = Utils.getRandomInt(12, 20);
                char key[] = new char[length];
                for (int i = 0; i < length; i++) {
                    key[i] = (char) Utils.getRandomInt(33, 126);
                }
                String keyString = new String(key);
                apiKeyField.setText(keyString);
               
            } catch (NoSuchAlgorithmException e1) {
                Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.CANCEL);
                a.initOwner(stage);
                a.setHeaderText("Error");
                a.setTitle("Error");
                a.show();
            }
        });

        HBox apiKeyBox = new HBox(apiKeyText, showKeyBtn);
        apiKeyBox.setPadding(new Insets(0));;
        apiKeyBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(apiKeyBox, Priority.ALWAYS);

       
        Runnable hideKey = ()->{
           
            apiKeyBox.getChildren().removeAll(apiKeyField, hideKeyBtn, randomApiKeyBtn, saveKeyBtn);
       
            apiKeyBox.getChildren().add(showKeyBtn);
        
        };

        Runnable showKey = ()->{
            apiKeyField.setText(namedNodeUrlProperty().get().getApiKey());
            apiKeyBox.getChildren().remove(showKeyBtn);
            apiKeyBox.getChildren().addAll(apiKeyField, hideKeyBtn, randomApiKeyBtn, saveKeyBtn);
        };

        hideKeyBtn.setOnAction(e->{
            hideKey.run();
        });
       
        saveKeyBtn.setOnAction(e->{
            updateKey.run();
            hideKey.run();
        });

        showKeyBtn.setOnAction(e ->{
            App.verifyAppKey(()->{
                showKey.run();
            }, getErgoNodesList().getErgoNodes().getNetworksData().getAppData().getAppKeyBytes());
            
        });

        SimpleBooleanProperty showConfigFile = new SimpleBooleanProperty(false);

        BufferedButton configFileHeadingBtn = new BufferedButton(m_downArrowUrlString, App.MENU_BAR_IMAGE_WIDTH);
        configFileHeadingBtn.setText("File");
        configFileHeadingBtn.setId("titleBtn");
        configFileHeadingBtn.setGraphicTextGap(15);
        configFileHeadingBtn.setPadding(new Insets(0, 0, 0, 0));
        configFileHeadingBtn.setContentDisplay(ContentDisplay.RIGHT);
        configFileHeadingBtn.setOnAction(e -> {
            showConfigFile.set(!showConfigFile.get());
        });

        HBox configFileHeadingBox = new HBox(configFileHeadingBtn);
        configFileHeadingBox.setAlignment(Pos.CENTER_LEFT);
        configFileHeadingBox.minHeightProperty().bind(rowHeight);

        Text settingsFileNameText = new Text(String.format("%-14s", "Name"));
        settingsFileNameText.setFill(getPrimaryColor());
        settingsFileNameText.setFont(App.txtFont);

        File configFile = m_nodeConfigData.getConfigFile();

        TextField settingsFileNameField = new TextField(configFile != null ? configFile.getName() : "Unknown");
        settingsFileNameField.setId("formField");
        settingsFileNameField.setEditable(false);
        HBox.setHgrow(settingsFileNameField, Priority.ALWAYS);

        HBox settingsFileNameBox = new HBox(settingsFileNameText, settingsFileNameField);
        settingsFileNameBox.setAlignment(Pos.CENTER_LEFT);
        settingsFileNameBox.minHeightProperty().bind(rowHeight);

        Text settingsFileText = new Text(String.format("%-14s", "Location"));
        settingsFileText.setFill(getPrimaryColor());
        settingsFileText.setFont(App.txtFont);

        TextField settingsFileField = new TextField(configFile != null ? configFile.getAbsolutePath() : "");
        settingsFileField.setId("formField");
        settingsFileField.setEditable(false);
        HBox.setHgrow(settingsFileField, Priority.ALWAYS);

        HBox settingsFileBox = new HBox(settingsFileText, settingsFileField);
        settingsFileBox.setAlignment(Pos.CENTER_LEFT);
        settingsFileBox.minHeightProperty().bind(rowHeight);

        Text settingsFileHashText = new Text(String.format("%-14s", "Hash"));
        settingsFileHashText.setFill(getPrimaryColor());
        settingsFileHashText.setFont(App.txtFont);

        HashData configFileHashData = m_nodeConfigData.getConfigFileHashData();

        TextField settingsFileHashField = new TextField(configFileHashData != null ? configFileHashData.getHashStringHex() : "Unknown");
        settingsFileHashField.setId("formField");
        settingsFileHashField.setEditable(false);

        HBox.setHgrow(settingsFileHashField, Priority.ALWAYS);

        Text configHashName = new Text(configFileHashData != null ? "(" + configFileHashData.getHashName() + ")" : "");
        configHashName.setFill(getSecondaryColor());
        configHashName.setFont(App.txtFont);

        Region configHashSpacer = new Region();
        configHashSpacer.setMinWidth(5);

        HBox settingsFileHashBox = new HBox(settingsFileHashText, configHashSpacer, configHashName, settingsFileHashField);
        settingsFileHashBox.setAlignment(Pos.CENTER_LEFT);
        settingsFileHashBox.minHeightProperty().bind(rowHeight);

        VBox configFileBodyBox = new VBox(settingsFileNameBox, settingsFileHashBox, settingsFileBox);
        configFileBodyBox.setId("bodyBox");
        configFileBodyBox.setPadding(new Insets(0, 0, 0, 30));
        HBox.setHgrow(configFileBodyBox, Priority.ALWAYS);

        VBox configBodyBox = new VBox(apiKeyBox, configFileHeadingBox, configFileBodyBox);
        configBodyBox.setPadding(new Insets(10, 20, 10, 30));
        configBodyBox.setId("bodyBox");
        HBox.setHgrow(configBodyBox, Priority.ALWAYS);

        Runnable updateConfigFileInfo = () -> {
            boolean isShowing = showConfigFile.get();
            if (isShowing) {
                if (!configBodyBox.getChildren().contains(configFileBodyBox)) {
                    int index = configBodyBox.getChildren().indexOf(configFileHeadingBox) + 1;

                    configBodyBox.getChildren().add(index, configFileBodyBox);
                }
                configFileHeadingBtn.setImage(new Image(m_upArrowUrlString));
            } else {
                if (configBodyBox.getChildren().contains(configFileBodyBox)) {
                    configBodyBox.getChildren().remove(configFileBodyBox);
                }
                configFileHeadingBtn.setImage(new Image(m_downArrowUrlString));
            }
        };
        updateConfigFileInfo.run();
        showConfigFile.addListener((obs, oldVal, newVal) -> updateConfigFileInfo.run());

        VBox configVBox = new VBox(configBox, configBodyBox);
        configVBox.setPadding(new Insets(0, 0, 15, 0));

        Text appHeadingText = new Text("Application");
        appHeadingText.setFill(App.txtColor);
        appHeadingText.setFont(App.txtFont);

        HBox appHeadingBox = new HBox(appHeadingText);
        appHeadingBox.setAlignment(Pos.CENTER_LEFT);
        appHeadingBox.setMinHeight(40);
        appHeadingBox.setId("headingBox");
        appHeadingBox.setPadding(new Insets(0, 0, 0, 15));

        Text appNetworkTypeText = new Text(String.format("%-14s", "Network Type"));
        appNetworkTypeText.setFill(getPrimaryColor());
        appNetworkTypeText.setFont(App.txtFont);

        TextField appNetworkTypeField = new TextField(getNamedNodeUrl().getNetworkType().toString());
        appNetworkTypeField.setId("formField");
        appNetworkTypeField.setEditable(false);
        HBox.setHgrow(appNetworkTypeField, Priority.ALWAYS);

        HBox appNetworkTypeBox = new HBox(appNetworkTypeText, appNetworkTypeField);
        appNetworkTypeBox.setAlignment(Pos.CENTER_LEFT);
        appNetworkTypeBox.minHeightProperty().bind(rowHeight);

        Text appRunOnStartText = new Text(String.format("%-14s", "Autorun"));
        appRunOnStartText.setFill(getPrimaryColor());
        appRunOnStartText.setFont(App.txtFont);

        BufferedButton appRunOnStartBtn = new BufferedButton("/assets/checkmark-25.png", App.MENU_BAR_IMAGE_WIDTH);
        appRunOnStartBtn.setPrefWidth(30);
        appRunOnStartBtn.setPrefHeight(30);
        appRunOnStartBtn.setId("checkBtn");

        Runnable updateRunOnStart = () -> {
            boolean runOnStart = m_runOnStart;
            appRunOnStartBtn.setImage(runOnStart ? new Image("/assets/checkmark-25.png") : null);
        };
        updateRunOnStart.run();

        appRunOnStartBtn.setOnAction(e -> {
            m_runOnStart = !m_runOnStart;
            lastUpdated().set(LocalDateTime.now());
            updateRunOnStart.run();
        });

        Region appRunOnStartRegion = new Region();
        appRunOnStartRegion.setMinWidth(9);

        HBox appRunOnStartBox = new HBox(appRunOnStartText, appRunOnStartRegion, appRunOnStartBtn);
        appRunOnStartBox.setAlignment(Pos.CENTER_LEFT);
        appRunOnStartBox.minHeightProperty().bind(rowHeight);

        Text appNameText = new Text(String.format("%-14s", "Name"));
        appNameText.setFill(getPrimaryColor());
        appNameText.setFont(App.txtFont);

        TextField appNameField = new TextField();
        appNameField.setId("formField");
        appNameField.setEditable(false);
        HBox.setHgrow(appNameField, Priority.ALWAYS);

        HBox appNameBox = new HBox(appNameText, appNameField);
        appNameBox.setAlignment(Pos.CENTER_LEFT);
        appNameBox.minHeightProperty().bind(rowHeight);

        Text appFileText = new Text(String.format("%-14s", "Location"));
        appFileText.setFill(getPrimaryColor());
        appFileText.setFont(App.txtFont);

        TextField appFileField = new TextField();
        appFileField.setId("formField");
        appFileField.setEditable(false);
        HBox.setHgrow(appFileField, Priority.ALWAYS);

        HBox appFileBox = new HBox(appFileText, appFileField);
        appFileBox.setAlignment(Pos.CENTER_LEFT);
        appFileBox.minHeightProperty().bind(rowHeight);

        Text appHashText = new Text(String.format("%-14s", "Hash"));
        appHashText.setFill(getPrimaryColor());
        appHashText.setFont(App.txtFont);

        TextField appHashField = new TextField();
        appHashField.setId("formField");
        appHashField.setEditable(false);
        HBox.setHgrow(appHashField, Priority.ALWAYS);

        Text appHashName = new Text();
        appHashName.setFill(getSecondaryColor());
        appHashName.setFont(App.txtFont);

        Region appHashSpacer = new Region();
        appHashSpacer.setMinWidth(5);

        HBox appHashBox = new HBox(appHashText, appHashSpacer, appHashName, appHashField);
        appHashBox.setAlignment(Pos.CENTER_LEFT);
        appHashBox.minHeightProperty().bind(rowHeight);
        HBox.setHgrow(appHashBox, Priority.ALWAYS);

        Text appExecText = new Text(String.format("%-14s", "Parameters"));
        appExecText.setFill(getPrimaryColor());
        appExecText.setFont(App.txtFont);

        MenuButton viewExecutionStringBtn = new MenuButton("(View)");
        viewExecutionStringBtn.setId("menuBtn");
        viewExecutionStringBtn.setPopupSide(Side.TOP);

        TextField appParamsField = new TextField(m_execParams);
        appParamsField.setId("formField");
        appParamsField.setPromptText("Enter additional parameters (Advanced)");
        HBox.setHgrow(appParamsField, Priority.ALWAYS);

        Runnable updateCmdString = () -> {
            File appFile = getAppFile();
            File cfgFile = m_nodeConfigData.getConfigFile();

            String cmdString = getExecCmd(appFile, cfgFile);
            MenuItem executionStringItem = new MenuItem("\"" + cmdString + "\"  (Click to copy)");
            executionStringItem.setOnAction(e -> {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(cmdString);
                clipboard.setContent(content);

            });
            viewExecutionStringBtn.getItems().clear();
            viewExecutionStringBtn.getItems().add(executionStringItem);
        };

        appParamsField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String prevParams = m_execParams;
                String newParams = appParamsField.getText();

                if (!prevParams.equals(newParams)) {
                    m_execParams = appParamsField.getText();
                    lastUpdated().set(LocalDateTime.now());
                    updateCmdString.run();
                }

            }
        });
        updateCmdString.run();

        HBox appExecBox = new HBox(appExecText, viewExecutionStringBtn, appParamsField);
        appExecBox.setAlignment(Pos.CENTER_LEFT);
        appExecBox.minHeightProperty().bind(rowHeight);

        SimpleBooleanProperty showAppFile = new SimpleBooleanProperty(false);

        BufferedButton appFileHeadingBtn = new BufferedButton(m_downArrowUrlString, App.MENU_BAR_IMAGE_WIDTH);
        appFileHeadingBtn.setText("File");
        appFileHeadingBtn.setId("titleBtn");
        appFileHeadingBtn.setPadding(new Insets(0, 0, 0, 0));
        appFileHeadingBtn.setGraphicTextGap(15);
        appFileHeadingBtn.setContentDisplay(ContentDisplay.RIGHT);
        appFileHeadingBtn.setOnAction(e -> {
            showAppFile.set(!showAppFile.get());
        });
        Tooltip openAppBtnTip = new Tooltip("Select new file");
        openAppBtnTip.setShowDelay(new Duration(100));

        BufferedButton openAppFileBtn = new BufferedButton(getInstallImgUrl(), App.MENU_BAR_IMAGE_WIDTH);
        openAppFileBtn.setTooltip(openAppBtnTip);

        Tooltip downloadAppBtnTip = new Tooltip("Get latest version");
        downloadAppBtnTip.setShowDelay(new Duration(100));

        Text isLatestText = new Text("");
        isLatestText.setFont(App.titleFont);
        isLatestText.setFill(getSecondaryColor());

        BufferedButton downloadAppBtn = new BufferedButton("/assets/cloud-download-30.png", App.MENU_BAR_IMAGE_WIDTH);
        downloadAppBtn.setTooltip(downloadAppBtnTip);

        Region appFileRegion = new Region();
        HBox.setHgrow(appFileRegion, Priority.ALWAYS);

        HBox appFileHeadingBox = new HBox(appFileHeadingBtn, appFileRegion, isLatestText, openAppFileBtn, downloadAppBtn);

        appFileHeadingBox.setMinHeight(40);
        appFileHeadingBox.setAlignment(Pos.CENTER_LEFT);

        Text appFileVersionText = new Text(String.format("%-14s", "Version"));
        appFileVersionText.setFill(getPrimaryColor());
        appFileVersionText.setFont(App.txtFont);

        TextField appFileVersionField = new TextField(m_appVersion.get() != null ? m_appVersion.get().get() : "");
        appFileVersionField.setId("formField");
        appFileVersionField.setEditable(false);
        HBox.setHgrow(appFileVersionField, Priority.ALWAYS);

        HBox appFileVersionBox = new HBox(appFileVersionText, appFileVersionField);
        appFileVersionBox.setAlignment(Pos.CENTER_LEFT);
        appFileVersionBox.minHeightProperty().bind(rowHeight);

        Runnable updateAppFileText = () -> {
            // String fileVersion = m_appVersion.get().get();

            appNameField.setText(m_appFileName);
            appFileField.setText(getAppFile().getAbsolutePath());
            appHashName.setText("(" + m_appFileHashData.getHashName() + ")");
            appHashField.setText(m_appFileHashData.getHashStringHex());

        };


        Runnable updateFileVersion = ()->{
            if(m_appVersion.get() != null){
                appFileVersionField.setText(m_appVersion.get().get());
            }else{
                appFileVersionField.setText("-");
            }
        };

        m_appVersion.addListener((obs,oldval,newval)->updateFileVersion.run());

        updateAppFileText.run();

        openAppFileBtn.setOnAction(e -> {
            openAppFile(stage, updateAppFileText, () -> {
            });
        });

        downloadAppBtn.setOnAction(e -> {
            m_gitHubAPI.getAssetsLatest(getNetworksData().getExecService(), (onFinished)->{
                Object finishedObject = onFinished.getSource().getValue();
                    if(finishedObject != null && finishedObject instanceof GitHubAsset[] && ((GitHubAsset[]) finishedObject).length > 0){
                
                        GitHubAsset[] assets = (GitHubAsset[]) finishedObject;
        
                        updateAppFile(assets, stage, settingsScene, () -> {
                            isLatestText.setText("Updated: " + m_appVersion.get().get());
                            updateAppFileText.run();
                        }, () -> {
                            isLatestText.setText("Already latest: " + m_appVersion.get().get());
                        },()->{
                            isLatestText.setText("Update failed");
                        });
                    }else{
                        isLatestText.setText("Update failed");
                    }
                }, (onFailed)->{
                    Alert a = new Alert(AlertType.NONE, onFailed.getSource().getMessage(), ButtonType.OK);
                    a.setHeaderText("Download Error");
                    a.setTitle("Download Error - Local Node - Ergo Nodes");
                    if (stage != null) {
                        a.initOwner(stage);
                    }
                    a.show();
                    isLatestText.setText("Update failed");
            });
        });

        VBox appFileBodyVBox = new VBox(appNameBox, appFileVersionBox, appHashBox, appFileBox);
        appFileBodyVBox.setId("bodyBox");
        appFileBodyVBox.setPadding(new Insets(0, 0, 0, 30));
        HBox.setHgrow(appFileBodyVBox, Priority.ALWAYS);

        VBox appBodyBox = new VBox(appNetworkTypeBox, appRunOnStartBox, appExecBox, appFileHeadingBox);
        appBodyBox.setPadding(new Insets(10, 20, 10, 30));
        appBodyBox.setId("bodyBox");
        HBox.setHgrow(appBodyBox, Priority.ALWAYS);

        Runnable updateAppFileInfo = () -> {
            boolean isShowing = showAppFile.get();
            if (isShowing) {
                if (!appBodyBox.getChildren().contains(appFileBodyVBox)) {
                    int index = appBodyBox.getChildren().indexOf(appFileHeadingBox) + 1;
                    appBodyBox.getChildren().add(index, appFileBodyVBox);
                }
                appFileHeadingBtn.setImage(new Image(m_upArrowUrlString));
            } else {
                if (appBodyBox.getChildren().contains(appFileBodyVBox)) {
                    appBodyBox.getChildren().remove(appFileBodyVBox);
                }
                appFileHeadingBtn.setImage(new Image(m_downArrowUrlString));
            }
        };
        updateAppFileInfo.run();
        showAppFile.addListener((obs, oldVal, newVal) -> updateAppFileInfo.run());

        VBox appVBox = new VBox(appHeadingBox, appBodyBox);
        appVBox.setPadding(new Insets(0, 0, 15, 0));

        VBox bodyBox = new VBox(appVBox, configVBox);
        bodyBox.setId("bodyBox");
        bodyBox.setPadding(new Insets(15));

        VBox bodyPaddingBox = new VBox(bodyBox);
        bodyPaddingBox.setPadding(new Insets(5, 5, 5, 5));

        Region footerSpacer = new Region();
        footerSpacer.setMinHeight(5);

        VBox footerBox = new VBox();

        ScrollPane bodyScroll = new ScrollPane(bodyPaddingBox);
        bodyScroll.prefViewportWidthProperty().bind(settingsScene.widthProperty());
        bodyScroll.setPadding(new Insets(0));
        layoutBox.getChildren().addAll(titleBox, headerBox, menuBarBox, bodyScroll, footerBox);
        settingsScene.getStylesheets().add("/css/startWindow.css");

        SimpleDoubleProperty scrollBarWidth = new SimpleDoubleProperty(0);

        bodyPaddingBox.prefWidthProperty().bind(settingsScene.widthProperty().subtract(scrollBarWidth).subtract(5));

        Rectangle screenRectangle = getErgoNodesList().getErgoNodes().getNetworksData().getMaximumWindowBounds();

        Runnable updateBodySize = () -> {
            //
            double screenHeight = screenRectangle.getHeight();
            double bodyHeight = bodyPaddingBox.heightProperty().get() + 5;
            double restOfStageHeight = titleBox.heightProperty().doubleValue() + headerBox.heightProperty().get() + menuBarBox.heightProperty().get() + footerBox.heightProperty().get();
            double totalHeight = bodyHeight + restOfStageHeight;

            if (totalHeight <= screenHeight) {
                stage.setHeight(totalHeight);
                bodyScroll.setPrefViewportHeight(bodyHeight);
                if (stage.getY() + totalHeight > screenHeight) {
                    stage.setY(screenHeight - totalHeight);
                }
            } else {
                scrollBarWidth.set(20);
                stage.setHeight(screenHeight);
                bodyScroll.setPrefViewportHeight(screenHeight - restOfStageHeight);
                stage.setY(0);
            }
        };

        bodyPaddingBox.heightProperty().addListener((obs, oldVal, newVal) -> updateBodySize.run());

        stage.sceneProperty().addListener((obs, oldval, newval) -> updateBodySize.run());

        closeBtn.setOnAction(e -> closeStage.run());

        return settingsScene;
    }

    @Override
    public void openSettings() {
        if (checkValidSetup()) {
            if (m_settingsStage == null) {

                checkDrive(m_appDir, (onSuccess)->{
                    Object sourceValue = onSuccess.getSource().getValue();

                    if(sourceValue != null && sourceValue instanceof Boolean && (Boolean) sourceValue){
    
                        m_settingsStage = new Stage();
                        Runnable close = () -> {
                            m_settingsStage.close();
                            m_settingsStage = null;
                        };
                        m_settingsStage.getIcons().add(getIcon());
                        m_settingsStage.setResizable(false);
                        m_settingsStage.initStyle(StageStyle.UNDECORATED);
                        Scene scene = settingsScene(m_settingsStage, close);

                        if(scene != null){
                            m_settingsStage.setScene(scene);
                            m_settingsStage.setOnCloseRequest(e -> close.run());
                            m_settingsStage.show();
                        }else{
                            m_settingsStage = null;
                        }
                        
                    }else{
                        Alert a = new Alert(AlertType.WARNING, "Drive may be removed. Would you like to reset the node?", ButtonType.YES, ButtonType.NO);
                        a.setTitle("Not Found");
                        a.setHeaderText("Not Found");
                        Optional<ButtonType> result = a.showAndWait();
                        if(result != null && result.isPresent() && result.get() == ButtonType.YES){
                            isSetupProperty().set(false);
                            setup();
                        }
                    }

                }, (onFailed)->{
                    isSetupProperty().set(false);
                });

           
            } else {
                if (m_settingsStage.isIconified()) {
                    m_settingsStage.setIconified(false);
                }
                m_settingsStage.show();
                m_settingsStage.toFront();
            }
        } else {

            setup();
        }

    }

    @Override
    public HBox getMenuBar() {
        Tooltip settingsTip = new Tooltip("Settings");
        settingsTip.setShowDelay(new Duration(100));
        BufferedButton settingsBtn = new BufferedButton("/assets/settings-outline-white-120.png", App.MENU_BAR_IMAGE_WIDTH);
        settingsBtn.setTooltip(settingsTip);

        Tooltip installTooltip = new Tooltip("Setup");
        installTooltip.setShowDelay(new Duration(100));

        BufferedButton installBtn = new BufferedButton(getInstallImgUrl(), App.MENU_BAR_IMAGE_WIDTH);
        installBtn.setTooltip(installTooltip);
        installBtn.setOnAction(e -> {
            setup();
        });

        Region menuSpacer = new Region();
        HBox.setHgrow(menuSpacer, Priority.ALWAYS);

        HBox menuBar = new HBox(menuSpacer);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);

        Runnable checkInstalled = () -> {
            boolean isSetup = m_isSetupProperty.get();

            if (isSetup) {
                if (!menuBar.getChildren().contains(settingsBtn)) {
                    menuBar.getChildren().add(settingsBtn);
                }
                if (menuBar.getChildren().contains(installBtn)) {
                    menuBar.getChildren().remove(installBtn);
                }
            } else {
                if (menuBar.getChildren().contains(settingsBtn)) {
                    menuBar.getChildren().remove(settingsBtn);
                }
                if (!menuBar.getChildren().contains(installBtn)) {
                    menuBar.getChildren().add(installBtn);
                }
            }
        };

        m_isSetupProperty.addListener((obs, oldval, newval) -> checkInstalled.run());
        checkInstalled.run();
        settingsBtn.setOnAction(e -> openSettings());
        return menuBar;
    }

    public void openInBrowser(){
        NamedNodeUrl namedNode = getNamedNodeUrl();
        getErgoNodesList().getErgoNodes().getNetworksData().getHostServices().showDocument(namedNode.getUrlString() + "/panel" );
    }
}
