package com.netnotes;

import com.google.gson.JsonObject;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public interface SimpleNoteInterface {

    
    boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed);

    Object sendNote(JsonObject note);

    void sendMessage(String id, int code, long timeStamp, String str);

    void shutdown();

}
