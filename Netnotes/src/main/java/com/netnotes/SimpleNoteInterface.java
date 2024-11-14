package com.netnotes;

import com.google.gson.JsonObject;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public interface SimpleNoteInterface {

    
    boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed);

    Object sendNote(JsonObject note);

    void sendMessage(int code, long timestamp,String networkId, Number number);

    void sendMessage(int code, long timeStamp,String networkId, String str);


    void shutdown();

}
