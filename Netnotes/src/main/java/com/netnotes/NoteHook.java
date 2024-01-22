package com.netnotes;

import com.google.gson.JsonObject;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public class NoteHook {
    private JsonObject m_note;
    private EventHandler<WorkerStateEvent> m_onSucceeded;
    private EventHandler<WorkerStateEvent> m_onFailed;

    public NoteHook(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        m_note = note;
        m_onSucceeded = onSucceeded;
        m_onFailed = onFailed;
    }

    public JsonObject getNote(){
        return m_note;
    }

    public EventHandler<WorkerStateEvent> getOnSucceeded(){
        return m_onSucceeded;
    }
    public EventHandler<WorkerStateEvent> getOnFailed(){
        return m_onFailed;
    }
}
