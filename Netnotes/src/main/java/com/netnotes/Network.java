package com.netnotes;

import java.io.File;

import java.time.LocalDateTime;
import java.util.ArrayList;

import com.google.gson.JsonObject;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.text.TextAlignment;

public class Network extends IconButton {

    private File logFile = new File("Network-log");
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

    private String m_stageIconStyle = IconStyle.ICON;

    public Network(Image icon, String name, String id, NetworksData networksData) {
        super(icon);
        setName(name);
        setIconStyle(IconStyle.ICON);
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

    public String getStageIconStyle() {
        return m_stageIconStyle;
    }

    public void setStageIconStyle(String iconStyle) {
        m_stageIconStyle = iconStyle;

    }

    public JsonObject getStageJson() {
        JsonObject json = new JsonObject();
        json.addProperty("maximized", getStageMaximized());
        json.addProperty("width", getStageWidth());
        json.addProperty("height", getStageHeight());
        json.addProperty("prevWidth", getStagePrevWidth());
        json.addProperty("prevHeight", getStagePrevHeight());
        json.addProperty("iconStyle", getStageIconStyle());
        return json;
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

    @Override
    public void close() {

        super.close();

        shutdownNowProperty().set(LocalDateTime.now());
    }

    public void getOpen() {
        open();
    }

    public IconButton getButton(String iconStyle) {

        IconButton iconButton = new IconButton(getIcon(), getName()) {
            @Override
            public void open() {
                getOpen();
            }
        };

        if (iconStyle.equals(IconStyle.ROW)) {
            iconButton.setContentDisplay(ContentDisplay.LEFT);
        } else {
            iconButton.setContentDisplay(ContentDisplay.TOP);
            iconButton.setTextAlignment(TextAlignment.CENTER);
        }

        return iconButton;
    }
}
