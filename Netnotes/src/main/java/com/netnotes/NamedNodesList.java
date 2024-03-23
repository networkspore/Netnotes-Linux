package com.netnotes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import com.google.gson.JsonObject;
import com.utils.Utils;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.VBox;

public class NamedNodesList {

    public final static String NODES_LIST_URL = "https://raw.githubusercontent.com/networkspore/Netnotes/main/publicNodes.json";

    private SimpleStringProperty m_selectedNamedNodeId = new SimpleStringProperty(null);

    private ArrayList<NamedNodeUrl> m_dataList = new ArrayList<>();

    private boolean m_updates;

    private SimpleDoubleProperty m_gridWidthProperty = new SimpleDoubleProperty(200);

    private SimpleObjectProperty<LocalDateTime> m_doGridUpdate = new SimpleObjectProperty<LocalDateTime>(null);

    private SimpleObjectProperty<LocalDateTime> m_optionsUpdated = new SimpleObjectProperty<LocalDateTime>(null);

    private long m_updateTimeStamp = -1;

    public NamedNodesList(boolean updates, ExecutorService execService) {

        m_updates = updates;

        if (updates) {
            getGitHubList(execService);
        } else {
            setDefaultList();
        }

     
    }

    public SimpleStringProperty selectedNamedNodeIdProperty() {
        return m_selectedNamedNodeId;
    }

    public SimpleDoubleProperty gridWidthProperty() {
        return m_gridWidthProperty;
    }

    public SimpleObjectProperty<LocalDateTime> doUpdateProperty() {
        return m_doGridUpdate;
    }

    public SimpleObjectProperty<LocalDateTime> optionsUpdatedProperty() {
        return m_optionsUpdated;
    }

    public void getGitHubList(ExecutorService execService) {
        Utils.getUrlJson(NODES_LIST_URL, execService, (onSucceeded) -> {
            Object sourceObject = onSucceeded.getSource().getValue();
            if (sourceObject != null && sourceObject instanceof JsonObject) {
                openNodeJson((JsonObject) sourceObject);
            }
        }, (onFailed) -> {
            //  setDefaultList();
        }, null);

    }

    public void openNodeJson(JsonObject json) {
        JsonElement timeStampElement = json.get("timeStamp");

        long timeStamp = timeStampElement != null && timeStampElement.isJsonPrimitive() ? timeStampElement.getAsLong() : -1;

        if (timeStamp > m_updateTimeStamp) {
            m_updateTimeStamp = timeStamp;

            JsonElement nodesElement = json.get("nodes");

            if (nodesElement != null && nodesElement.isJsonArray()) {
                JsonArray nodesArray = nodesElement.getAsJsonArray();
                m_dataList.clear();
                for (int i = 0; i < nodesArray.size(); i++) {
                    JsonElement nodeJsonElement = nodesArray.get(i);
                    if (nodeJsonElement != null && nodeJsonElement.isJsonObject()) {
                        JsonObject nodeObject = nodeJsonElement.getAsJsonObject();
                        addNamedNodeUrl(new NamedNodeUrl(nodeObject), false);

                    }
                }
                doUpdateProperty().set(LocalDateTime.now());
            }
        }
    }

    public void addNamedNodeUrl(NamedNodeUrl nodeUrl) {
        addNamedNodeUrl(nodeUrl, true);
    }

    public void addNamedNodeUrl(NamedNodeUrl nodeUrl, boolean update) {
        if (nodeUrl != null) {
            m_dataList.add(nodeUrl);
            nodeUrl.lastUpdatedProperty().addListener((obs, oldVal, newVal) -> doUpdateProperty().set(newVal));
            doUpdateProperty().set(LocalDateTime.now());
        }
    }

    public void setDefaultList() {
        if (m_dataList.size() == 0) {
            NamedNodeUrl defaultNodeUrl = new NamedNodeUrl();
            m_dataList.add(defaultNodeUrl);
            m_selectedNamedNodeId.set(defaultNodeUrl.getId());
            m_doGridUpdate.set(LocalDateTime.now());
        }
    }

    public VBox getGridBox() {
        VBox gridBox = new VBox();

        updateGrid(gridBox);

        m_doGridUpdate.addListener((obs, oldVal, newVal) -> updateGrid(gridBox));

        return gridBox;
    }

    public void updateGrid(VBox gridBox) {
        gridBox.getChildren().clear();

        for (int i = 0; i < m_dataList.size(); i++) {
            NamedNodeUrl namedNode = m_dataList.get(i);
            IconButton namedButton = namedNode.getButton();
            namedButton.prefWidthProperty().bind(m_gridWidthProperty);

            gridBox.getChildren().add(namedButton);
        }
    }

    public NamedNodeUrl getNamedNodeUrl(String id) {
        if (id != null && m_dataList.size() > 0) {
            for (int i = 0; i < m_dataList.size(); i++) {
                NamedNodeUrl namedNodeUrl = m_dataList.get(i);

                if (namedNodeUrl.getId().equals(id)) {
                    return namedNodeUrl;
                }
            }
        }

        return null;
    }

}
