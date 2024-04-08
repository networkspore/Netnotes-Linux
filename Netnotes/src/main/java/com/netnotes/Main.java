package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import com.utils.Utils;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import javafx.application.Application;

public class Main {

    public static String NOTES_ID = "main";

   // appFile = Utils.urlToFile(classLocation);
    public static void main(String[] args) {

        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run() {
                File resourceFile = new File(App.RESOURCES_FILENAME);
                if(resourceFile.isFile()){
                    try {
                        String resourceFileString = resourceFile.isFile() ? Files.readString(resourceFile.toPath()) : null;
                        JsonObject json = Utils.getResourcesObject(resourceFileString);
                        JsonArray appsArray = json.get("apps").getAsJsonArray();
                        
                        for(int i = 0; i < appsArray.size() ; i++){
                            JsonElement stringElement = appsArray.get(i);
                            if(stringElement != null){
                                Utils.sendTermSig(stringElement.getAsString());
                            }
                        }
                        Files.deleteIfExists(resourceFile.toPath());
          
                    } catch (IOException e) {
                        try {
                            Files.writeString(App.logFile.toPath(),"\nshutdown hook ioerr: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e1) {

                        }
                    }
                }
                //if(resourceFil)
                //Files.readString()
            }
        });
        Application.launch(App.class, args);

    }



}
