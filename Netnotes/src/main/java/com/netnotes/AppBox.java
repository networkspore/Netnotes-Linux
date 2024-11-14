package com.netnotes;

import com.google.gson.JsonObject;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.VBox;

public class AppBox extends VBox implements SimpleNoteInterface{
    
    private String m_appId;

    public AppBox(){
        super();
    }

    public AppBox(String appId){
        m_appId = appId;
    }

    public void setAppId(String appId){
        m_appId = appId;
    }

    public String getAppId(){
        return m_appId;
    }

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        return false;
    }

    public Object sendNote(JsonObject note){
        return null;
    }


    public void sendMessage(int code, long timestamp,String networkId, Number number){

    }

    public void sendMessage(int code, long timeStamp,String networkId, String str){
        
    }

    public void shutdown(){
      
    }

    
}
