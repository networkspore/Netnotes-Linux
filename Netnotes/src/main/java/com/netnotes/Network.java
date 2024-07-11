package com.netnotes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import com.google.gson.JsonObject;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Network  {

    private int m_connectionStatus = 0;
    private String m_networkId;
    private NetworksData m_networksData;
   // private ArrayList<NoteInterface> m_tunnelInterfaceList = new ArrayList<>();
    private NoteInterface m_parentInterface = null;
    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<LocalDateTime>(LocalDateTime.now());
    private ChangeListener<LocalDateTime> m_changeListener = null;
    private SimpleObjectProperty<LocalDateTime> m_shutdownNow = new SimpleObjectProperty<>(null);
    private SimpleObjectProperty<NoteHook> m_noteHookProperty = new SimpleObjectProperty<>(null);

    public final static long EXECUTION_TIME = 500;

    public final static double SMALL_STAGE_WIDTH = 500;
    public final static double DEFAULT_STAGE_WIDTH = 700;
    public final static double DEFAULT_STAGE_HEIGHT = 500;

    private double m_stagePrevWidth = DEFAULT_STAGE_WIDTH;
    private double m_stagePrevHeight = DEFAULT_STAGE_HEIGHT;
    private boolean m_stageMaximized = false;
    private double m_stageWidth = DEFAULT_STAGE_WIDTH;
    private double m_stageHeight = DEFAULT_STAGE_HEIGHT;

    private ArrayList<NoteMsgInterface> m_msgListeners = new ArrayList<>();

    private String m_name;
    private Image m_icon;

    public Network(Image icon, String name, String id, NetworksData networksData) {
        m_icon = icon;
        m_name = name;
        m_networkId = id;
        m_networksData = networksData;
        m_parentInterface = null;
    }

    public Network(Image icon, String name, String id, NoteInterface parentInterface) {
        this(icon, name, id, parentInterface.getNetworksData());
        m_parentInterface = parentInterface;

    }

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        return false;
    }

    public Object sendNote(JsonObject note){
        return null;
    }

    
    public void addMsgListener(NoteMsgInterface item) {
        if (!m_msgListeners.contains(item)) {
            if(m_connectionStatus != App.STARTED){
                start();
            }
            m_msgListeners.add(item);
        }else{
          
        }

    }

    protected void setName(String name){
        m_name = name;
        
    }

    public Image getAppIcon(){
        return m_icon;
    }


    public TabInterface getTab(Stage appStage,  SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject, Button networkBtn){
        return null;
    }

    public boolean removeMsgListener(NoteMsgInterface item){
        if(item != null){
            boolean removed = m_msgListeners.remove(item);
            
            if(m_msgListeners.size() == 0){
                stop();
            }
            
            return removed;
        }

        return false;
    }

    public Pane getPane(){
        return null;
    }

    public void setConnectionStatus(int status){
        m_connectionStatus = status;
    }

    public int getConnectionStatus(){
        return m_connectionStatus;
    }

    protected void stop(){
        
        setConnectionStatus(App.STOPPED);
        
    }
 
    public ArrayList<NoteMsgInterface> msgListeners(){
        return m_msgListeners;
    }

    protected void start(){
        setConnectionStatus(App.STARTED);
    }

    protected void sendMessage(int msg){
        long timestamp = System.currentTimeMillis();
        sendMessage(msg, timestamp);
    }

    protected void sendMessage(int msg, long timestamp){

        for(int i = 0; i < m_msgListeners.size() ; i++){
            m_msgListeners.get(i).sendMessage(msg, timestamp);
        }
    }
    protected void sendMessage(int code, long timestamp, String msg){

        for(int i = 0; i < m_msgListeners.size() ; i++){
            m_msgListeners.get(i).sendMessage(code, timestamp, msg);
        }
    }

    protected void sendMessage(String networkId, int code, long timestamp, String msg){

        for(int i = 0; i < m_msgListeners.size() ; i++){
            m_msgListeners.get(i).sendMessage(networkId, code, timestamp, msg);
        }
    }

    protected void sendMessage(String networkId, int code, long timeStamp, JsonObject json){
        for(int i = 0; i < m_msgListeners.size() ; i++){
            m_msgListeners.get(i).sendMessage(networkId, code, timeStamp, json);
        }
    }

    protected NoteMsgInterface getListener(String id) {
        for (int i = 0; i < m_msgListeners.size(); i++) {
            NoteMsgInterface listener = m_msgListeners.get(i);
            if (listener.getId().equals(id)) {
                return listener;
            }
        }
        return null;
    }

    public SimpleObjectProperty<NoteHook> noteHookProperty(){
        return m_noteHookProperty;
    }

    public NoteInterface getParentInterface() {
        return m_parentInterface;
    }

    public void setNetworkId(String id) {
        m_networkId = id;
    }

    public String getNetworkId() {
        return m_networkId;
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
        return json;
    }

    public String getName(){
        return m_name;
    }

    public Image getIcon(){
        return m_icon;
    }

    public void setIcon(Image icon){
        m_icon = icon;
    }

    public JsonObject getJsonObject() {
        JsonObject networkObj = new JsonObject();
        networkObj.addProperty("name", getName());
        networkObj.addProperty("networkId", getNetworkId());
        return networkObj;

    }

    public NetworksData getNetworksData() {
        return m_networksData;
    }
    /* 
    public NoteInterface getTunnelNoteInterface(String networkId) {

        for (NoteInterface noteInterface : m_tunnelInterfaceList) {
            if (noteInterface.getNetworkId().equals(networkId)) {
                return noteInterface;
            }
        }
        return null;
    }

    public ArrayList<NoteInterface> getTunnelNoteInterfaces() {
        return m_tunnelInterfaceList;
    }

    public void addTunnelNoteInterface(NoteInterface noteInterface) {
        if (getTunnelNoteInterface(noteInterface.getNetworkId()) == null) {
            m_tunnelInterfaceList.add(noteInterface);
        }
    }

    public void removeTunnelNoteInterface(String id) {
        for (int i = 0; i < m_tunnelInterfaceList.size(); i++) {
            NoteInterface tunnel = m_tunnelInterfaceList.get(i);

            if (tunnel.getNetworkId().equals(id)) {
                m_tunnelInterfaceList.remove(tunnel);
                break;
            }

        }
    }*/

    public SimpleObjectProperty<LocalDateTime> getLastUpdated() {
        return m_lastUpdated;
    }

    public void addUpdateListener(ChangeListener<LocalDateTime> changeListener) {
        m_changeListener = changeListener;
        if (m_changeListener != null) {
            m_lastUpdated.addListener(m_changeListener);

        }
        // m_lastUpdated.addListener();
    }

    public void removeUpdateListener() {
        if (m_changeListener != null) {
            m_lastUpdated.removeListener(m_changeListener);
            m_changeListener = null;
        }
    }

    public void remove() {
        removeUpdateListener();

    }

    public void shutdown() {
        
       

        shutdownNowProperty().set(LocalDateTime.now());
    }

    private SimpleObjectProperty<JsonObject> m_cmdProperty = new SimpleObjectProperty<JsonObject>(null);
    private ChangeListener<JsonObject> m_cmdListener;

    public SimpleObjectProperty<JsonObject> cmdProperty() {
        return m_cmdProperty;
    }

    public void addCmdListener(ChangeListener<JsonObject> cmdListener) {
        m_cmdListener = cmdListener;
        if (m_cmdListener != null) {
            m_cmdProperty.addListener(m_cmdListener);
        }
        // m_lastUpdated.addListener();
    }

    public void removeCmdListener() {
        if (m_cmdListener != null) {
            m_cmdProperty.removeListener(m_cmdListener);
            m_cmdListener = null;
        }
    }

    private ChangeListener<LocalDateTime> m_shutdownListener;

    public SimpleObjectProperty<LocalDateTime> shutdownNowProperty() {
        return m_shutdownNow;
    }

    public void addShutdownListener(ChangeListener<LocalDateTime> shutdownListener) {
        m_shutdownListener = shutdownListener;
        if (m_shutdownListener != null) {

            m_shutdownNow.addListener(shutdownListener);
        }
        // m_lastUpdated.addListener();
    }

    public void removeShutdownListener() {
        if (m_shutdownListener != null) {
            m_shutdownNow.removeListener(m_shutdownListener);
            m_shutdownListener = null;
        }
    }



}
