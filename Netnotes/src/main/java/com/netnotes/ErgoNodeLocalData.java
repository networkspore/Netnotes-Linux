package com.netnotes;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;

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

import org.apache.commons.io.FileUtils;
import org.ergoplatform.appkit.NetworkType;
import org.reactfx.util.FxTimer;

import com.utils.Utils;
import com.utils.Version;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Alert.AlertType;

import com.google.gson.JsonObject;
import com.GitHubAPI.GitHubAPI;
import com.GitHubAPI.GitHubAPI.GitHubAsset;
import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonElement;

public class ErgoNodeLocalData extends ErgoNodeData {

    public final static String DESCRIPTION = "Install and manage an Ergo Node locally.";

    public final static String DEFAULT_NODE_NAME = "Local Node";
    public final static String DEFAULT_CONFIG_NAME = "ergo.conf";
    public final static int MAX_CONSOLE_ROWS = 200;
    public final static int MAX_INPUT_BUFFER_SIZE = 30;
    public final static int SYNC_TIMEOUT_CYCLE = 15;

    public final static int DEFAULT_MEM_GB_REQUIRED = 6;

    public static final int INSTALL_PROGRESS = 100;

    public final static String STARTUP_STRING = "Starting up...";

    private String m_setupImgUrl = "/assets/open-outline-white-20.png";

    private boolean m_isRunOnStart = false;

    private File m_appDir = null;
    private String m_appFileName = null;
    private HashData m_appFileHashData = null;
    private String m_execParams = "";

    private long m_spaceRequired = 50L * (1024L * 1024L * 1024L);

    private int m_memGBRequired = DEFAULT_MEM_GB_REQUIRED;

    public final static long EXECUTION_TIME = 500;


    public long INPUT_CYCLE_PERIOD = 100;

    private ErgoNodeConfig m_nodeConfigData = null;

    private ExecutorService m_executor = null;
   
    private Future<?> m_future = null;
    private ScheduledExecutorService m_schedualedExecutor = null;
    private ScheduledFuture<?> m_scheduledFuture = null;
    private SimpleStringProperty m_consoleOutputProperty = new SimpleStringProperty("");

    private int m_statusCode = App.STOPPED;
    private String m_warnings = "";

    private Version m_appVersion = new Version();
    private Version m_latestVersion = null;

    private AtomicBoolean m_cancelUpdate = new AtomicBoolean(false); 

 

    private AtomicInteger m_isSyncStuck = new AtomicInteger(-1);
    private final AtomicBoolean isGettingInfo = new AtomicBoolean(false);
    private boolean m_deleteOldFiles = true;

    private GitHubAPI m_gitHubAPI = new GitHubAPI("ergoplatform", "ergo");

    /*private boolean m_synced = false;
    private long m_networkBlockHeight;
    private long m_nodeBlockHeight;
    private long m_headersBlockHeight;
    private int m_peerCount;*/
    private JsonObject m_statusJson;

    public ErgoNodeLocalData(String id,  File appDir, boolean isAppFile, File appFile, String configName, String configText, NamedNodeUrl namedNode, ErgoNodesList ergoNodesList) throws Exception {
        super(id,namedNode.getName(), LOCAL_NODE,  ergoNodesList, namedNode);
        m_appDir = appDir;

        if(!appDir.isDirectory()){
            appDir.mkdirs();
        }

        if(isAppFile){
            if(appFile != null && appFile.isFile()){
                File appFileParent = appFile.getParentFile();
                m_appFileName = appFile.getName();
                if(!appFileParent.getAbsolutePath().equals(m_appDir.getAbsolutePath())){
                    m_appFileName = appFile.getName();
                    File appFileCopy = new File(m_appDir.getAbsolutePath() + "/" + m_appFileName);
                    
                    Files.copy(appFile.toPath(), appFileCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    
                    m_appFileHashData = new HashData(appFileCopy);
                  
                }
                checkLatest((onLatest)->{
                    if(m_isRunOnStart){
                        run();
                    }
                });
            }else{
 
                throw new NullPointerException("Application file missing");
            }
        }else{
            FxTimer.runLater(Duration.ofMillis(200), ()->{
                updateApp();
            });
            
        }

        m_nodeConfigData = new ErgoNodeConfig(configText, configName, this);
        init();
    }





    public ErgoNodeLocalData(String imgString, String id, String name, String clientType, JsonObject json, ErgoNodesList ergoNodesList) throws Exception {

        super(id,name, LOCAL_NODE,  ergoNodesList,(NamedNodeUrl) null);
        openJson(json);
        
        init();
        checkLatest((onLatest)->{
            if(m_isRunOnStart){
                run();
            }
        });
    }

 
    public void init(){
   


        m_statusCode = App.STOPPED;

        JsonObject statusJson = new JsonObject();
        statusJson.addProperty("status", "Stopped");
        statusJson.addProperty("appVersion", m_appVersion.get());
        statusJson.addProperty("statusCode", m_statusCode);
        
        m_statusJson = statusJson;
        
        
    }

    @Override
    public void shutdown(){
        terminate();
    }
   

    public int getMemGBRequired(){
        return m_memGBRequired;
    }

    public void setMemGBRequired(int memGBrequired){
        m_memGBRequired = memGBrequired;
    }

    @Override
    public Object sendNote(JsonObject note) {
        
        JsonElement cmdElement = note.get(App.CMD);

        if(cmdElement != null){
            switch (cmdElement.getAsString()) {
                case "run":
                    run();
                    return true;
                case "terminate":
                    terminate();
                    return true;
                case "updateApp":
                    updateApp();
                    return true;
                case "getStatus":
                    return getStatus();
            }
        }
        return null;
    }




    public void hashError(String appFileName, String configFileName, boolean isAppHash, boolean isConfigHash, HashData appFileHashData, HashData configFileHashData, long timeStamp) {
        int code = App.ERROR; 
        
        HashData appHashData = m_appFileHashData;
        HashData configHashData = m_nodeConfigData.getConfigFileHashData();


        JsonObject json = new JsonObject();
        if(m_appDir != null){
   
            json.addProperty("fileDir",  m_appDir.getAbsolutePath()  );
            json.addProperty("appFileName", appFileName);
            json.addProperty("configFileName", configFileName);
            
            json.addProperty("isAppHash", isAppHash);
            json.addProperty("isConfigHash", isConfigHash);

            json.add("appHashData", appHashData.getJsonObject());
            json.add("appFileHashData", appFileHashData.getJsonObject());
            json.add("configHashData", configHashData.getJsonObject());
            json.add("configFileHashData", configFileHashData.getJsonObject());


        }else{

            json.addProperty("status", "Directory not found");
            json.addProperty("code", App.ERROR);
            
        }
        
        JsonObject msgObject = Utils.getMsgObject(code, timeStamp, ErgoNetwork.NODE_NETWORK);
  
        msgObject.add("data", json);

        sendMessage(code, timeStamp, getNetworkId(), msgObject.toString());
        terminate();
    }

    public void configFileError(boolean updated) {
        Alert a = new Alert(AlertType.WARNING, "Local node config file has been altered and " + (updated ? "has been reverted to the last saved file." : " cannot be recoverd. Node setup will be required."), ButtonType.OK);
        a.setHeaderText("Config Mismatch");
        a.setTitle("Error: Config Mistmatch - Local Node - Ergo Nodes");
        a.show();

    }

    public String getWarnings(){
        return m_warnings;
    }

    @Override
    public void openJson(JsonObject jsonObj)  {

        if (jsonObj != null) {
           
          try{
            JsonElement runOnStartElement = jsonObj.get("runOnStart");
          
            JsonElement appDirElement = jsonObj.get("appDir");
            JsonElement appFileNameElement = jsonObj.get("appFileName");
            JsonElement appFileHashDataElement = jsonObj.get("appFileHashData");
            JsonElement appExecParamsElement = jsonObj.get("appExecParams");
            JsonElement appVersionElement = jsonObj.get("appVersion");
            JsonElement configElement = jsonObj.get("config");


            JsonElement namedNodeElement = jsonObj == null ? null : jsonObj.get("namedNode");
            NamedNodeUrl namedNodeUrl = namedNodeElement != null && namedNodeElement.isJsonObject() ? new NamedNodeUrl(namedNodeElement.getAsJsonObject()) : new NamedNodeUrl(getId(), getName(), "127.0.0.1", DEFAULT_MAINNET_PORT, "", NetworkType.MAINNET);
    

            namedNodeUrlProperty().set( namedNodeUrl);
          

            m_appDir =  new File( appDirElement.getAsString()) ;
         
            m_isRunOnStart = runOnStartElement != null && runOnStartElement.isJsonPrimitive() ? runOnStartElement.getAsBoolean() : false;
            m_appFileHashData = null;
            m_nodeConfigData = null;
        
       

            JsonObject configJson = configElement != null && configElement.isJsonObject() ? configElement.getAsJsonObject() : null;

            m_appFileName = appFileNameElement != null && appFileNameElement.isJsonPrimitive() ? appFileNameElement.getAsString() : null;
                
    
            m_appFileHashData = appFileHashDataElement != null ? new HashData(appFileHashDataElement.getAsJsonObject()) : null;
            m_appVersion = appVersionElement != null && appVersionElement.isJsonPrimitive() ? new Version(appVersionElement.getAsString()) : null;
        

            if(configJson != null){
                
                m_nodeConfigData = new ErgoNodeConfig(configJson, this);
                
                if (m_nodeConfigData != null && m_nodeConfigData.getConfigFile() != null && m_nodeConfigData.getConfigFile().isFile() && m_nodeConfigData.getConfigFileHashData() != null) {
                    
                    if (m_isRunOnStart) {
                        run();
                    }

                } else {

                    // m_nodeConfigData.updateConfigFile();
                }
            }

            if (appExecParamsElement != null && appExecParamsElement.isJsonPrimitive()) {
                m_execParams = appExecParamsElement.getAsString();
            }

            }catch(Exception e ){
                try {
                    Files.writeString(App.logFile.toPath(),"\nLocalNode: "+getName()+" Error: " + e.toString(), StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                } catch (IOException e1) {
  
                }

            }
        
        }
 

    }

    public Version getAppVersion() {
        return m_appVersion;
    }

    public boolean isSetup() {
        return m_appDir != null;
    }

    @Override
    public void start(){
        if (m_isRunOnStart) {
            run();
        }
    }

    @Override
    public void stop(){
        terminate();
    }


    public long getSpaceRequired() {
        return m_spaceRequired;
    }

    public boolean isRunOnStart() {
        return m_isRunOnStart;

    }

    public String getInstallImgUrl() {
        return m_setupImgUrl;
    }


    public File getAppFile() {
        return m_appDir != null && m_appDir.isDirectory() && m_appFileName != null ? new File(m_appDir.getAbsolutePath() + "/" + m_appFileName) : null;
    }

    

    public void cleanSync() throws IOException {
        Files.writeString(App.logFile.toPath(), "\nErgoLocalNode: Clean Sync Required - Re-syncing node", StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        File stateDir = new File(m_appDir.getCanonicalPath() + "/.ergo/state");
        File historyDir = new File(m_appDir.getCanonicalPath() + "/.ergo/history");

        if (stateDir.isDirectory()) {
            FileUtils.deleteDirectory(stateDir);
        }
        if (historyDir.isDirectory()) {
            FileUtils.deleteDirectory(historyDir);
        }

    }

    protected void setStatusStopped(){
        updateStatus(App.STOPPED, "Stopped");
    }

    public JsonObject getStatus(){   
        return m_statusJson;
    }

    protected void updateStatus(int statusCode, JsonObject statusJson){
        m_statusCode = statusCode;
        

        long timeStamp = System.currentTimeMillis();
      
        
        JsonObject json = new JsonObject();
        JsonElement appVersionElement = statusJson.get("appVersion");
        String appVersionString = appVersionElement != null && appVersionElement.isJsonPrimitive() ? appVersionElement.getAsString() : null;
           
        if(statusCode != App.STOPPED){
            JsonElement fullHeightElement = statusJson.get("fullHeight");
            JsonElement maxPeerHeightElement = statusJson.get("maxPeerHeight");
            JsonElement peerCountElement = statusJson.get("peersCount");
            JsonElement headersHeightElement = statusJson.get("headersHeight");
           
            String prevVersionString =  m_appVersion.get();
    
            if (appVersionString != null && !prevVersionString.equals(appVersionString)) {
                try {

                    setVersion(new Version(appVersionString), true);
                    

                } catch (IllegalArgumentException e) {

                }
            }


            long nodeBlockHeight = fullHeightElement != null && fullHeightElement.isJsonPrimitive() ? fullHeightElement.getAsLong() : -1;
            long networkBlockHeight = maxPeerHeightElement != null && maxPeerHeightElement.isJsonPrimitive() ? maxPeerHeightElement.getAsLong() : -1;
            long headersHeight = headersHeightElement != null && headersHeightElement.isJsonPrimitive() ? headersHeightElement.getAsLong() : -1;

            int peerCount = peerCountElement != null && peerCountElement.isJsonPrimitive() ? peerCountElement.getAsInt() : -1;
            
            


            boolean synced = nodeBlockHeight != -1 && networkBlockHeight != -1 && headersHeight != -1 && headersHeight >= networkBlockHeight && nodeBlockHeight >= networkBlockHeight;
          

            if(statusCode == App.STARTING){
                json.addProperty("status","Starting");
            }else{
                json.addProperty("synced", synced);

                if (nodeBlockHeight == -1) {
                    if(headersHeight > -1 && peerCount > 0){
                        String headersHeightString = headersHeight == -1 ? "Getting headers..." : headersHeight + "";
                        String networkBlockHeightString = networkBlockHeight == -1 ? "Getting Network Height..." : networkBlockHeight + "";
                     
                        json.addProperty("status", "Syncing: " + headersHeightString + "/" + networkBlockHeightString);
                        
                    }else{
                        json.addProperty("status",  peerCount == 0 ? "Searching for peers" : "Connecting");
                    } 
                } else {
                    String nodeBlockHeightString = nodeBlockHeight == -1 ? "Getting Node Height..." : nodeBlockHeight + "";
                    String networkBlockHeightString = networkBlockHeight == -1 ? "Getting Network Height..." : networkBlockHeight + "";
    
                    json.addProperty("status", synced ? "Online" : "Syncing: " + nodeBlockHeightString + "/" + networkBlockHeightString);
                }
            }

            
           
        
        }else{
            json.addProperty("status", "Stopped");
           
        }
        if(appVersionElement != null){
            json.addProperty("appVersion", m_appVersion.get());
        }
        if(m_latestVersion != null){
            json.addProperty("latestVersion", m_latestVersion.get());
        }
        json.addProperty("statusCode", m_statusCode);

        json.add("information", statusJson);

        m_statusJson = json;

 

        sendMessage(App.STATUS, timeStamp,  getNetworkId(), json.toString());

    }

    public void updateStatus(int statusCode, String msg){
        m_statusCode = statusCode;
        long timeStamp = System.currentTimeMillis();
   
        
        JsonObject json = new JsonObject();
        json.addProperty("status", msg);
        json.addProperty("appVersion", m_appVersion.get());
        if(m_latestVersion != null){
            json.addProperty("latestVersion", m_latestVersion.get());
        }
        json.addProperty("statusCode", m_statusCode);
     


        m_statusJson = json;
        sendMessage(App.STATUS, timeStamp,getNetworkId(), json.toString());

      
    }

    private void setVersion(Version version, boolean isSave){
        
        m_appVersion = version;
        
        if(isSave){
            getLastUpdated().set(LocalDateTime.now());
        }

    }

    public String getUrlString(){
        return namedNodeUrlProperty().get().getUrlString();
    }

    
    protected void updateCycle() {
        //get Network info

        //long timeoutSeconds = (getNetworksData().getCyclePeriod() * SYNC_TIMEOUT_CYCLE) / 60;
 
        String localApiString = getUrlString() + "/info";
     

        boolean foundPID = Utils.findPIDs(m_appFileName) != null;
        if(foundPID){
            isGettingInfo.set(true);
            Utils.getUrlJson(localApiString, getNetworksData().getExecService(), (onSucceeded) -> {
                Object sourceObject = onSucceeded.getSource().getValue();
                if (sourceObject != null && sourceObject instanceof JsonObject) {
                    JsonObject json = (JsonObject) sourceObject;

             
      
                    
                    updateStatus(App.STARTED,  json);
                  

                    isGettingInfo.set(false);
                    if (m_isSyncStuck.get() > -1) {
                        if (m_isSyncStuck.get() > SYNC_TIMEOUT_CYCLE) {
                            terminate();
                            NamedNodeUrl namedNode = namedNodeUrlProperty().get();
                            
                            Alert a = new Alert(AlertType.WARNING, namedNode.getName() +  " has been unable to sync. Would you like to restart the sync process?", ButtonType.YES, ButtonType.NO);
                            a.setTitle("Attention - Node Sync: Timed out");
                            a.setHeaderText("Attention: Node Sync: Timed out");
                            
                            Optional<ButtonType> result = a.showAndWait();
                            if(result != null && result.isPresent() && result.get() == ButtonType.YES){
        
                                try {
                                    cleanSync();
                                } catch (IOException e) {
        
                                }
                                run();
                
                            }
                            m_isSyncStuck.set(-1);
                        }
                        m_isSyncStuck.set(m_isSyncStuck.get() + 1);
                    }
            
                }
            }, (onFailed) -> {
                //String errMsg = onFailed.getSource().getException().toString();
                terminate();
                isGettingInfo.set(false);
            });
        }else{
            terminate();
        }

    }

   

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
        
        setStatusStopped();
        
        if (m_scheduledFuture != null) {
            m_scheduledFuture.cancel(false);
        }
        if (m_future != null) {
            m_future.cancel(false);
        }

        m_warnings = "";
  
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
        
           
            //Thread mainThread = Thread.currentThread();
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
                                
                                updateStatus(App.STARTED, "Started");

                            },null);
                        }

                        String s = null;
                        try {
                            s = stdInput.readLine();
                        } catch (IOException e) {
                            try {
                                Files.writeString(App.logFile.toPath(), "\nNode starting: No input: " + e.toString() , StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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

                            Files.writeString(App.logFile.toPath(), "\nLocal node execution error: " + errLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                             
                        }

                        proc.waitFor();
                        
                        
                       
                       
                    } catch (Exception e) {
                        try{

                            Files.writeString(App.logFile.toPath(), "\nLocal node error: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                             
                        }catch(IOException e1){

                        }
              
                        
                      
                    }
                    reset();
                    //   m_pid = -1;
                    //   statusProperty().set(MarketsData.STOPPED);
                }

            });

        }

        //t.start();
    }

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
  
    public void run() {
      
        
        long timeStamp = System.currentTimeMillis();

        if (m_appDir != null && m_statusCode == App.STOPPED) {

    
            updateStatus(App.STARTING, "Starting");

            Utils.checkDrive(m_appDir, getNetworksData().getExecService(), (onSuccess)->{
                
                Object sourceValue = onSuccess.getSource().getValue();

                if(sourceValue != null && sourceValue instanceof Boolean && (Boolean) sourceValue){
                
                    File appFile = getAppFile();

                    File configFile = m_nodeConfigData != null ? m_nodeConfigData.getConfigFile() : null;

                    if (appFile != null && appFile.isFile() && configFile != null && configFile.isFile() && m_appFileHashData != null && m_nodeConfigData != null && m_nodeConfigData.getConfigFileHashData() != null) {
                     
                        try {
                            HashData appHashData = m_appFileHashData;
                            HashData appFileHashData = new HashData(appFile);
                            
                            HashData configFileHashData = new HashData(configFile);
                            HashData configHashData = m_nodeConfigData.getConfigFileHashData();


                            boolean isAppHash = appHashData.getHashStringHex().equals(appFileHashData.getHashStringHex());
                            boolean isConfigHash = configHashData.getHashStringHex().equals(configFileHashData.getHashStringHex());


                            if (isAppHash && isConfigHash ) {

                                runNode(appFile, configFile);
                                
                            } else {
                                
                                
                                hashError(m_appFileName, configFile.getName(),isAppHash, isConfigHash, appFileHashData, configFileHashData, timeStamp);
                             
                            }

                     

                        } catch (IOException er1) {
                            sendError(App.STOPPED, er1.toString(), "IOException");
                    
                        }
                    } else {
                        sendError(App.STOPPED, "File data unavailable", "Application");
           
                    }

                }else{
                    sendError(App.STOPPED,"File system did not respond", "Application");
                }

            }, onFailed->{
                
            });
            
        } else {
            if (m_appDir == null) {
                sendError(App.STOPPED,"Setup required", "Application");
                terminate();
            }
        }

    }



    private void sendError(int statusCode, String msg, String type){
        long timeStamp = System.currentTimeMillis();
        JsonObject errJson = new JsonObject();
        errJson.addProperty("state", getStatusCodeString(m_statusCode));
        m_statusCode = statusCode;

        errJson.addProperty("status", msg);
        errJson.addProperty("error", type);
        errJson.addProperty("statusCode", statusCode);


        
        sendMessage(App.STATUS, timeStamp, getNetworkId(), errJson.toString());

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
        return 5L * 1024L * 1024L * 1024L;
    }



    public boolean checkValidSetup() {

        
        File configFile = m_nodeConfigData != null ? m_nodeConfigData.getConfigFile() : null;
        File appFile = getAppFile();

        return (m_appDir != null && m_appDir.isDirectory() && configFile != null && configFile.isFile() && appFile != null && appFile.isFile());
    }


    

    public SimpleStringProperty consoleOutputProperty() {
        return m_consoleOutputProperty;
    }


    public void terminate() {
        if ( m_statusCode != App.STOPPED) {   
            terminateProcess();
        }
    }

    public void terminateProcess(){
        if (m_statusCode != App.STOPPED) {

            if(!Utils.sendTermSig(m_appFileName)){
                reset();
            }else{
                
            }
        
        }
    }

    
    public void kill() {

        Alert a = new Alert(AlertType.NONE, "Sync-Error detected. Would you like to force shutdown and re-sync?", ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = a.showAndWait();
        
        if(result.isPresent() && result.get() == ButtonType.YES){
            Utils.sendKillSig(m_appFileName);
            
            try {
                cleanSync();
            } catch (IOException e) {

            }
        }

    }

    public boolean getIsSetup() {
        return m_appDir != null;
    }

  



    

    public void setExecParams(String params) {
        m_execParams = params;
    }

    public String getExecParams() {

        return m_execParams;
    }

    public File getAppDir(){
        return m_appDir;
    }
   

    @Override
    public JsonObject getJsonObject() {
 
        JsonObject json = super.getJsonObject();


        json.addProperty("runOnStart", m_isRunOnStart);

        if (m_nodeConfigData != null) {
            json.add("config", m_nodeConfigData.getJsonObject());
        }

        if (m_appDir != null) {
            json.addProperty("appDir", m_appDir.getAbsolutePath());
        }
        if (m_appFileName != null ) {
            json.addProperty("appFileName", m_appFileName);
        }
        if (m_appFileHashData != null) {
            json.add("appFileHashData", m_appFileHashData.getJsonObject());
        }
        if(m_appVersion != null){
            json.addProperty("appVersion", m_appVersion.get());
        }
        json.addProperty("appExecParams", m_execParams);
        
    

        return json;

    }

    public static String getStatusCodeString(int statusCode){
        switch(statusCode){
            case App.STARTED:
                return "Running";
            case App.STARTING:
                return  "Starting";
            case App.UPDATING:
                return "Updating";
            case App.STOPPED:
                return "Stopped";
            default:
                return "Unavailable";
        }

    }

    public void checkLatest(EventHandler<WorkerStateEvent> complete){
        
        m_gitHubAPI.getAssetsLatestRelease(getNetworksData().getExecService(), (onFinished)->{
            Object finishedObject = onFinished.getSource().getValue();
            if(finishedObject != null && finishedObject instanceof GitHubAsset[] && ((GitHubAsset[]) finishedObject).length > 0){
            
                

                GitHubAsset[] assets = (GitHubAsset[]) finishedObject;

                GitHubAsset fileAsset = assets[0];

                String name = fileAsset.getName();

                m_latestVersion = Utils.getFileNameVersion(name);
                
           
                if(m_latestVersion.compareTo(m_appVersion) > 0 ){
                    updateStatus(m_statusCode, "Update available: " + m_latestVersion.get());
                }else{
                    updateStatus(m_statusCode, "Node is latest: " + m_latestVersion.get());
                }
             
            }
            if(complete != null){
                Utils.returnObject(null, getExecutorService(), complete, null);
            }
        },(onErr)->{
            if(complete != null){
                Utils.returnObject(null, getExecutorService(), complete, null);
            }
        });
    }



    public void updateApp(){

            updateApp(false);

      
    }

    private ProgressIndicator m_updateProgressIndicator;

    public void updateApp(boolean force){
        if(m_statusCode != App.UPDATING){

            m_cancelUpdate.set(false);  
          
            String appDirString = m_appDir.getAbsolutePath();  
            Version prevVersion = m_appVersion;
            File prevAppFile = getAppFile();

            terminate();
            updateStatus(App.UPDATING, "Checking for updates...");

            

            m_gitHubAPI.getAssetsLatestRelease(getNetworksData().getExecService(), (onFinished)->{
                Object finishedObject = onFinished.getSource().getValue();
                if(finishedObject != null && finishedObject instanceof GitHubAsset[] && ((GitHubAsset[]) finishedObject).length > 0){
                
                    

                    GitHubAsset[] assets = (GitHubAsset[]) finishedObject;

                    GitHubAsset fileAsset = assets[0];

                    String name = fileAsset.getName();
                    String url = fileAsset.getUrl();

                    m_latestVersion = Utils.getFileNameVersion(name);

                    if (m_latestVersion.compareTo(prevVersion) > 0  || prevAppFile == null || (prevAppFile != null && !prevAppFile.isFile()) || force) { 

                        File appFile =  new File(appDirString + "/" + name);
                        m_updateProgressIndicator = new ProgressIndicator();

                        m_updateProgressIndicator.progressProperty().addListener((obs,oldval,newval)->{
                            sendMessage(INSTALL_PROGRESS, System.currentTimeMillis(), ErgoNetwork.NODE_NETWORK, newval);
                        });
            
        
                    
                        Utils.getUrlFileHash(url, appFile, getNetworksData().getExecService(), (onDlSucceeded) -> {
                            m_updateProgressIndicator = null;
                            Object dlObject = onDlSucceeded.getSource().getValue();
                            if (dlObject != null && dlObject instanceof HashData) {
                                m_appFileHashData = (HashData) dlObject;
                                m_appFileName = name;
                                m_appVersion = m_latestVersion;

                

                                if (m_deleteOldFiles) {
                                    if (prevAppFile != null && !prevAppFile.getName().equals(name) && prevAppFile.isFile()) {
                                        prevAppFile.delete();
                                    }
                                }

                                updateStatus(App.STOPPED, m_appFileName +": " +  m_appVersion.get());
                                
                                getLastUpdated().set(LocalDateTime.now());
                                if(m_isRunOnStart){
                                    run();
                                }
                            } else {
                
                                
                                sendError(App.STOPPED, "Download incomplete", "Network");
                            
                            }
                        }, (onError)->{
                            sendError(App.STOPPED, onError.getSource().getException().toString(), "Network");
                        }, m_updateProgressIndicator, m_cancelUpdate);

                    }else{

                        updateStatus(App.STOPPED, "Node is latest: " + m_latestVersion.get());
                        if(m_isRunOnStart){
                            run();
                        }
                        
                    }
                
                }else{

                    sendError(App.STOPPED,"Network unavailable", "Network");

                }
            }, onError->{
                sendError(App.STOPPED, onError.getSource().getException().toString(), "Network");
            });
   
        }
    }



    
    /*
    private Scene getFinalSetupScene(Button nextBtn, Button backBtn, SimpleObjectProperty<File> jarFile, SimpleBooleanProperty getLatestBoolean, SimpleBooleanProperty updateBoolean, SimpleBooleanProperty autoUpdateBoolean, SimpleStringProperty downloadUrlProperty, SimpleStringProperty downloadFileName, Stage stage) {

        String titleString = "Core File - Setup - Local Node - " + ErgoNodes.NAME;
        stage.setTitle(titleString);

        Image icon = new Image(ErgoNodes.getSmallAppIconString());
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
    } */

    /*
    @Override
    public HBox getStatusBox() {

        //   statusString.set(getIsSetup() ? "Offline" : "(Not Installed)");
        Text middleTopRightText = new Text();
        middleTopRightText.setFont(getFont());
        middleTopRightText.setFill(getSecondaryColor());

      //  middleTopRightText.textProperty().bind(cmdProperty());

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
        run();

        HBox contentsBox = new HBox(bodyBox);
        HBox.setHgrow(contentsBox, Priority.ALWAYS);
        contentsBox.setId("bodyRowBox");
        contentsBox.setPadding(new Insets(0, 10, 0, 10));
        return contentsBox;
    }
 */
  
    
    /* 
    @Override
    public HBox getRowItem() {




        BufferedMenuButton itemMenuBtn = new BufferedMenuButton();
        itemMenuBtn.setPadding(new Insets(0));

        MenuItem settingsItem = new MenuItem("Settings");
        settingsItem.setOnAction(e->{
            openSettings();
        });

        MenuItem openInBrowserItem = new MenuItem("Show in browser");
        openInBrowserItem.setOnAction((e)->{
            //TODO: showInBrowser
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

       // middleTopRightText.textProperty().bind(cmdProperty());

        Text middleBottomRightText = new Text(getNetworkTypeString());
        middleBottomRightText.setFont(getFont());
        middleBottomRightText.setFill(getPrimaryColor());

        VBox centerRightBox = new VBox(middleTopRightText, middleBottomRightText);
        centerRightBox.setAlignment(Pos.CENTER_RIGHT);

        VBox.setVgrow(centerRightBox, Priority.ALWAYS);

        Tooltip statusBtnTip = new Tooltip("");
        statusBtnTip.setShowDelay(new Duration(100));
        //m_startImgUrl : m_stopImgUrl
        String statusBtnImg = statusProperty().get().equals(App.STATUS_STOPPED) ? (getIsSetup() && getAppFile() != null ? getStartImgUrl() : getInstallImgUrl()) : getStopImgUrl();
        BufferedButton statusBtn = new BufferedButton(statusBtnImg, 15);
        statusBtn.setId("statusBtn");
        statusBtn.setPadding(new Insets(0, 10, 0, 10));
        statusBtn.setTooltip(statusBtnTip);
        statusBtn.setOnAction(action -> {
            if (statusProperty().get().equals(App.STATUS_STOPPED)) {

                run();

            } else {
                terminate();

            }
        });

        HBox leftBox = new HBox();
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
            String value = statusProperty().get() == null ? App.STATUS_STOPPED : statusProperty().get();

            if (value.equals(App.STATUS_STOPPED)) {
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
          
                open();
             
            
        });
 

   
        //double width = bottomBox.layoutBoundsProperty().get().getWidth() - ipText.layoutBoundsProperty().get().getWidth() - botTimeText.layoutBoundsProperty().get().getWidth();
        // syncField.minWidthProperty().bind(rowBox.widthProperty().subtract(botTimeText.layoutBoundsProperty().get().getWidth()).subtract(200));


        return rowBox;
    }
    @Override
    public void open(){
        if(getIsSetup()){
            String statusString = statusProperty().get();

            if(statusString.equals( App.STATUS_STOPPED)){
                openSettings();
            }else{
                //TODO: open in browser
            }
        }else{
            openSettings();
        }
    }
    */

    
/*
    public void openAppFile(Stage stage, Runnable onSuccess, Runnable onFailed) {
        terminate();
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
                        getLastUpdated().set(LocalDateTime.now());
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
   */ 




    /*
     
    private String m_downArrowUrlString = "/assets/caret-down-15.png";
    private String m_upArrowUrlString = "/assets/caret-up-15.png";
    @Override
    public void openSettings() {
        if (checkValidSetup()) {
            if (m_settingsStage == null) {
                Utils.checkDrive(m_appDir,getErgoNodesList().getErgoNodes().getNetworksData().getExecService(), (onSuccess)->{
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

    }*/

    /*
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
    */
   
}


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