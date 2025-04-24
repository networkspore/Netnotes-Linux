package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netnotes.engine.NoteConstants;
import io.netnotes.engine.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import javafx.application.Application;


public class Main {

    public static String NOTES_ID = "main";
    public final static String RESOURCES_FILENAME = "resources.dat";

   // appFile = Utils.urlToFile(classLocation);
    public static void main(String[] args) {

        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run() {
                File resourceFile = new File(RESOURCES_FILENAME);
                if(resourceFile.isFile()){
                    try {
                        String resourceFileString = resourceFile.isFile() ? Files.readString(resourceFile.toPath()) : null;
                        JsonObject json = getResourcesObject(resourceFileString);
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
                            Files.writeString(new File("error.log").toPath(),"\nshutdown hook ioerr: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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


    public static void addAppResource(String appName) {
        File resourceFile = new File(RESOURCES_FILENAME);
        if(appName == null){
            return;
        }
        
        String resourceFileString = null;
        try{
            resourceFileString = resourceFile.isFile() ?  Files.readString(resourceFile.toPath()) : null ;
        }catch(IOException e){
            
        }
            
        JsonObject resourcesObject = getResourcesObject(resourceFileString);


        JsonArray appsArray = resourcesObject.get("apps").getAsJsonArray();
        
        resourcesObject.remove("apps");

        boolean isAdd = findJsonArrayStringIndex(appsArray, appName) == -1;

        if(isAdd){
            appsArray.add(appName);
            resourcesObject.add("apps", appsArray);

            try {
                Files.writeString(resourceFile.toPath(), resourcesObject.toString());
            } catch (IOException e) {
                try {
                    Files.writeString(NoteConstants.logFile.toPath(), "\naddAppResource: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
                }
            }
        }


    }

    public static void removeAppResource(String appName) throws IOException {
   
    
        File resourceFile = new File(RESOURCES_FILENAME);
        if(resourceFile.isFile() && appName != null)
        {
            String resourceFileString = null;
            try{
                resourceFileString = Files.readString(resourceFile.toPath());
            }catch(IOException e){
                
            }
            JsonObject resourcesObject = getResourcesObject(resourceFileString);
             
            JsonArray appsArray = resourcesObject.get("apps").getAsJsonArray();
            
            resourcesObject.remove("apps");

            int index = findJsonArrayStringIndex(appsArray, appName);

            if(index != -1){
                appsArray.remove(index);
                resourcesObject.add("apps", appsArray);
                Files.writeString(resourceFile.toPath(), resourcesObject.toString());
            
            }
        }
    }

    public static int findJsonArrayStringIndex(JsonArray stringJson, String str){
        
        int size = stringJson.size();

        for(int i = 0; i < size ; i ++){
            if(stringJson.get(i).getAsString().equals(str)){
                return i;
            }
        }

        return -1;
    }



    public static JsonObject getResourcesObject(String resourceString){

        if(resourceString != null){
         
            JsonElement resourceFileElement = new JsonParser().parse(resourceString);
            
            if(resourceFileElement != null && resourceFileElement.isJsonObject()){
                
                JsonObject json = resourceFileElement.getAsJsonObject();
                if(json.has("apps")){
                    if(!json.get("apps").isJsonArray()){
                        json.remove("apps");
                        json.add("apps", new JsonArray());
                    }
                }else{
                    json.add("apps", new JsonArray());
                }
                return json;
            }
            
        }
        JsonObject resourceObject = new JsonObject();
        resourceObject.add("apps", new JsonArray());
        
        return resourceObject;
    }

}
