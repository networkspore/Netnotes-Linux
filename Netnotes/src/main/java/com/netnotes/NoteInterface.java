package com.netnotes;

import java.time.LocalDateTime;

import com.google.gson.JsonObject;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public interface NoteInterface {

    String getName();

    String getNetworkId();

    Image getAppIcon();

    Image getSmallAppIcon();

    SimpleObjectProperty<LocalDateTime> getLastUpdated();

    boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator);

    Object sendNote(JsonObject note);

    JsonObject getJsonObject();

    

    TabInterface getTab(Stage appStage, SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject,  Button networkBtn);

    String getType();


    NetworksData getNetworksData();

    NoteInterface getParentInterface();

    void addUpdateListener(ChangeListener<LocalDateTime> changeListener);

    void removeUpdateListener();

    void remove();

    void shutdown();

    SimpleObjectProperty<LocalDateTime> shutdownNowProperty();


    void addMsgListener(NoteMsgInterface listener);

    boolean removeMsgListener(NoteMsgInterface listener);

    int getConnectionStatus();

    void setConnectionStatus(int status);


    String getDescription();
}
