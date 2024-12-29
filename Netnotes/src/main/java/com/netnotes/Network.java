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
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Network  {

    private int m_connectionStatus = App.STOPPED;
    private String m_networkId;
    private NetworksData m_networksData;
    private NoteInterface m_parentInterface = null;
    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<LocalDateTime>(LocalDateTime.now());
    private ChangeListener<LocalDateTime> m_changeListener = null;
    private SimpleObjectProperty<LocalDateTime> m_shutdownNow = new SimpleObjectProperty<>(null);
    public final static long EXECUTION_TIME = 500;

    public final static double SMALL_STAGE_WIDTH = 500;
    public final static double DEFAULT_STAGE_WIDTH = 700;
    public final static double DEFAULT_STAGE_HEIGHT = 500;

    private double m_stagePrevWidth = DEFAULT_STAGE_WIDTH;
    private double m_stagePrevHeight = DEFAULT_STAGE_HEIGHT;
    private boolean m_stageMaximized = false;
    private double m_stageWidth = DEFAULT_STAGE_WIDTH;
    private double m_stageHeight = DEFAULT_STAGE_HEIGHT;

    private String[] m_keyWords = null;

    private ArrayList<NoteMsgInterface> m_msgListeners = new ArrayList<>();

    private String m_name;
    private Image m_icon;
    private Button m_appBtn;

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

    public Button getButton(double size){
        if(m_appBtn != null){
            return m_appBtn;
        }else{
            Tooltip tooltip = new Tooltip(getName());
            tooltip.setShowDelay(javafx.util.Duration.millis(100));
            ImageView imgView = new ImageView(getAppIcon());
            imgView.setPreserveRatio(true);
            imgView.setFitWidth(size);

            m_appBtn = new Button();
            m_appBtn.setGraphic(imgView);
            m_appBtn.setId("menuTabBtn");
            m_appBtn.setTooltip(tooltip);
            return m_appBtn; 
        }
    }

    
    public void addMsgListener(NoteMsgInterface item) {
        if (item != null && !m_msgListeners.contains(item)) {
            if(m_connectionStatus != App.STARTED){
                start();
            }
            m_msgListeners.add(item);
        }
    }



    private String m_description = null;

    public void setDescpription(String value){
        m_description = value;
    }

    public String getDescription(){
        return m_description;
    }

    public NoteInterface getNoteInterface(){
       
        return new NoteInterface() {
            
            public String getName(){
                return Network.this.getName();
            }

            public String getNetworkId(){
                return Network.this.getNetworkId();
            }

            public Image getAppIcon(){
                return Network.this.getAppIcon();
            }


            public SimpleObjectProperty<LocalDateTime> getLastUpdated(){
                return null;
            }

            public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
                return Network.this.sendNote(note, onSucceeded, onFailed);
            }

            public Object sendNote(JsonObject note){
                return Network.this.sendNote(note);
            }

            public JsonObject getJsonObject(){
                return Network.this.getJsonObject();
            }

            public TabInterface getTab(Stage appStage, SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject,  Button networkBtn){
                return Network.this.getTab(appStage, heightObject, widthObject, networkBtn);
            }




            public NetworksData getNetworksData(){
                return Network.this.getNetworksData();
            }

            public NoteInterface getParentInterface(){
                return null;
            }

            public void addUpdateListener(ChangeListener<LocalDateTime> changeListener){}

            public void removeUpdateListener(){}

            public void shutdown(){}

            public SimpleObjectProperty<LocalDateTime> shutdownNowProperty(){
                return null;
            }

            public void addMsgListener(NoteMsgInterface listener){
                if(listener != null && listener.getId() != null){
                    Network.this.addMsgListener(listener);
                }
            }
            public boolean removeMsgListener(NoteMsgInterface listener){
                
                return Network.this.removeMsgListener(listener);
            }

            public int getConnectionStatus(){
                return Network.this.getConnectionStatus();
            }

            public void setConnectionStatus(int status){}


            public String getDescription(){
                return Network.this.getDescription();
            }
        };
    }


    protected void setName(String name){
        m_name = name;
        
    }

    public Image getAppIcon(){
        return m_icon;
    }


    public TabInterface getTab(Stage appStage, SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject, Button networkBtn){
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



    protected void sendMessage(int code, long timeStamp,String networkId, String msg){
        for(int i = 0; i < m_msgListeners.size() ; i++){
            m_msgListeners.get(i).sendMessage(code, timeStamp, networkId, msg);
        }
    }

    protected void sendMessage(int code, long timeStamp, String networkId, Number num){
        for(int i = 0; i < m_msgListeners.size() ; i++){
            m_msgListeners.get(i).sendMessage(code, timeStamp, networkId, num);
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

    public String[] getKeyWords() {
        return m_keyWords;
    }

    public void setKeyWords(String[] value){
        m_keyWords = value;
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

    public void shutdown() {
        shutdownNowProperty().set(LocalDateTime.now());
        
        removeUpdateListener();
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
