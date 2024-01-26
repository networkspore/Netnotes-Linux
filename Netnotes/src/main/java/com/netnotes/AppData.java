package com.netnotes;


import java.io.File;
import java.io.FileNotFoundException;

import java.io.IOException;
import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;

import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;


import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;

import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;



import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;

import com.utils.Utils;
import com.utils.Version;


import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class AppData {
   // private static File logFile = new File("netnotes-log.txt");
    public static final String SETTINGS_FILE_NAME = "settings.conf";
    public static final String HOME_DIRECTORY = System.getProperty("user.home");
    public static final File DESKTOP_DIRECTORY = new File(HOME_DIRECTORY + "/Desktop");

    private File m_appDir = null;
    private File m_settingsFile = null;

    private String m_appKey;
    
    private File m_appFile = null;
    private HashData m_appHashData = null;
    private Version m_javaVersion = null;
    private SimpleObjectProperty<SecretKey> m_secretKey = new SimpleObjectProperty<SecretKey>(null);
    private boolean m_updates = false;
   // private Stage m_persistenceStage = null;


    public AppData() throws JsonParseException, IOException{

        URL classLocation = Utils.getLocation(App.class);
        m_appFile = Utils.urlToFile(classLocation);
        m_appHashData = new HashData(m_appFile);
        m_appDir = m_appFile.getParentFile();
        m_settingsFile = new File(m_appDir.getAbsolutePath() + "/" + SETTINGS_FILE_NAME);

        readFile();

    }

    public AppData(String password)throws NoSuchAlgorithmException, InvalidKeySpecException, IOException{
      
        URL classLocation = Utils.getLocation(App.class);
        m_appFile = Utils.urlToFile(classLocation);
        m_appHashData = new HashData(m_appFile);
        m_appDir = m_appFile.getParentFile();
        m_settingsFile = new File(m_appDir.getAbsolutePath() + "/" + SETTINGS_FILE_NAME);

        m_appKey = Utils.getBcryptHashString(password);
        
        save();
        createKey(password);
   
    }

  

    private void readFile()throws JsonParseException, IOException{
        
        if(m_settingsFile.isFile()){
        
            openJson(new JsonParser().parse(Files.readString(m_settingsFile.toPath())).getAsJsonObject());
        }else{
            throw new FileNotFoundException("Settings file not found.");
        }

    
    }

    private void openJson(JsonObject dataObject) throws JsonParseException{
        
        JsonElement appkeyElement = dataObject.get("appKey");

        if (appkeyElement != null && appkeyElement.isJsonPrimitive()) {

            m_appKey = appkeyElement.getAsString();
           
        } else {
            throw new JsonParseException("Null appKey");
        }
     
    }

    public boolean getUpdates(){
        return m_updates;
    }
   

    public File getAppDir(){
        return m_appDir;
    }

    public File getAppFile(){
        return m_appFile;
    }

  
     public void createKey(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {


        m_secretKey.set( new SecretKeySpec(createKeyBytes(password), "AES"));

    }

    public byte[] createKeyBytes(String password) throws NoSuchAlgorithmException, InvalidKeySpecException  {

        byte[] bytes = password.getBytes(StandardCharsets.UTF_8);

    

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        KeySpec spec = new PBEKeySpec(password.toCharArray(), bytes, 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        return tmp.getEncoded();

    }



    
    public String getAppKey() {
        return m_appKey;
    }

    public byte[] getAppKeyBytes() {
        return m_appKey.getBytes();
    }

    public void setAppKey(String keyHash) throws IOException {
        m_appKey = keyHash;
        save();
    }



    public Version getJavaVersion(){
        return m_javaVersion;
    }

    public HashData appHashData(){
        return m_appHashData;
    }

    public File appFile(){
        return m_appFile;
    }

    public SimpleObjectProperty<SecretKey> appKeyProperty() {
        return m_secretKey;
    }

    public void setAppKey(SecretKey secretKey) {
        m_secretKey.set(secretKey);
    }

    public JsonObject getJson() {
        JsonObject dataObject = new JsonObject();
        dataObject.addProperty("appKey", m_appKey);
        return dataObject;
    }

    public void save() throws IOException {
        String jsonString = getJson().toString();
        Files.writeString(m_settingsFile.toPath(), jsonString);
    }
}
