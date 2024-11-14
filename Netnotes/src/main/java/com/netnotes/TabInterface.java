package com.netnotes;

import com.google.gson.JsonObject;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public interface TabInterface {
    String getAppId();
    String getName();
    void shutdown();
    void setCurrent(boolean value);
    boolean getCurrent();

    SimpleStringProperty titleProperty();
    void sendMessage(int code, long timestamp,String networkId, Number number);
    void sendMessage(int code, long timeStamp,String networkId, String str);
    Object sendNote(JsonObject note);
    boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed);
}
