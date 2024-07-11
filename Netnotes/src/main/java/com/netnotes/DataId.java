package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

public class DataId {
    private File m_dataDir = null;

    public File getDataDir(){
        if(m_dataDir != null && !m_dataDir.isDirectory()){
            File parentDir = m_dataDir.getParentFile();
            if(parentDir.isDirectory()){
                try {
                   Files.createDirectory(m_dataDir.toPath());
                } catch (IOException e) {
                    try {
                        Files.writeString(App.logFile.toPath(), "\nNetwork could not create directory: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e1) {
                 
                    }
                }
            }
        }
        return m_dataDir;
    }

  

    public File getIdIndexFile(){
        return new File(getDataDir().getAbsolutePath() + "/index.dat");
    }
    
    public File addNewIdFile(String id, JsonArray jsonArray){
        String friendlyId = FriendlyId.createFriendlyId();
        String filePath = getDataDir().getAbsolutePath() + "/" + friendlyId + ".dat";
        File newFile = new File(filePath);
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("file", filePath);

        jsonArray.add(json);
        return newFile;
    }
    public SecretKey getAppKey(){
        return null;
    }
    public void saveIndexFile(JsonArray jsonArray){
        JsonObject json = new JsonObject();
        json.add("fileArray", jsonArray);
        
        try {
            Utils.saveJson(getAppKey(), json, getIdIndexFile());
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                | IOException e) {
            try {
                Files.writeString(App.logFile.toPath(), "SpectrumFinance (saveIndexFile): " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
        }
    }

    public JsonArray getIndexFileArray(SecretKey key){
        File indexFile = getIdIndexFile();
        try {
            JsonObject indexFileJson = indexFile.isFile() ? Utils.readJsonFile(key, indexFile) : null;
            if(indexFileJson != null){
                JsonElement fileArrayElement = indexFileJson.get("fileArray");
                if(fileArrayElement != null && fileArrayElement.isJsonArray()){
                    return fileArrayElement.getAsJsonArray();
                }
            }
        } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                | IOException e) {
            try {
                Files.writeString(App.logFile.toPath(), "SpectrumFinance (getIndexFileArray): " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
            
        }
        return null;
    }



    public JsonArray getIndexFileArray(SecretKey key, File indexFile){
        try {
            JsonObject indexFileJson = indexFile.isFile() ? Utils.readJsonFile(key, indexFile) : null;
            if(indexFileJson != null){
                JsonElement fileArrayElement = indexFileJson.get("fileArray");
                if(fileArrayElement != null && fileArrayElement.isJsonArray()){
                    return fileArrayElement.getAsJsonArray();
                }
            }
        } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                | IOException e) {
            try {
                Files.writeString(App.logFile.toPath(), "SpectrumFinance (getIndexFileArray): " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
            
        }
        return null;
    }

    public File getIdDataFile(String id){
        File indexFile = getIdIndexFile();
        JsonArray indexFileArray = indexFile.isFile() ? getIndexFileArray(getAppKey(), indexFile) : null;
        
        if(indexFileArray != null){
            File existingFile = getFileById(id, indexFileArray);
            if(existingFile != null){
                return existingFile;
            }else{
                File newFile = addNewIdFile(id, indexFileArray);
                saveIndexFile(indexFileArray);
                return newFile;
            }
        }else{
            JsonArray newIndexFileArray = new JsonArray();
            File newFile = addNewIdFile(id, newIndexFileArray);
            
            saveIndexFile(newIndexFileArray);

            return newFile;
        }

    }

    private File getFileById(String id, JsonArray jsonArray){
        int size = jsonArray.size();
        for(int i = 0; i < size; i++){
            JsonElement jsonElement = jsonArray.get(i);
            
            JsonObject obj = jsonElement.getAsJsonObject();

            String idString = obj.get("id").getAsString();
            if(idString.equals(id)){
                return new File(obj.get("file").getAsString());
            }
        }
        return null;
    }
}
