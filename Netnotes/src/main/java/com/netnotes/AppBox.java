package com.netnotes;

import com.google.gson.JsonObject;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.VBox;

public class AppBox extends VBox implements SimpleNoteInterface{


    public AppBox(){
        super();
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
