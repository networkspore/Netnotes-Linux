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
import com.utils.GitHubAPI;
import com.utils.GitHubAPI.GitHubAsset;
import com.utils.Utils;
import com.utils.Version;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;

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
        JsonElement updatesElement = dataObject.get("updates");

        if (appkeyElement != null && appkeyElement.isJsonPrimitive()) {

            m_appKey = appkeyElement.getAsString();
            m_updates = updatesElement != null && updatesElement.isJsonPrimitive() ? updatesElement.getAsBoolean() : false;
           
        } else {
            throw new JsonParseException("Null appKey");
        }
     
    }

    public boolean getUpdates(){
        return m_updates;
    }

    public void setUpdates(boolean updates) throws IOException{
        m_updates = updates;
        save();
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

    


    public void checkForUpdates( SimpleObjectProperty<UpdateInformation> updateInformation){
        GitHubAPI gitHubAPI = new GitHubAPI(App.GITHUB_USER, App.GITHUB_PROJECT);
         gitHubAPI.getAssetsLatest((onFinished)->{
            UpdateInformation tmpInfo = new UpdateInformation();

                Object finishedObject = onFinished.getSource().getValue();
                if(finishedObject != null && finishedObject instanceof GitHubAsset[] && ((GitHubAsset[]) finishedObject).length > 0){
            
                    GitHubAsset[] assets = (GitHubAsset[]) finishedObject;
              
                    for(GitHubAsset asset : assets){
                        if(asset.getName().equals("releaseInfo.json")){
                            tmpInfo.setReleaseUrl(asset.getUrl());
                            
                        }else{
                            if(asset.getContentType().equals("application/x-java-archive")){
                                if(asset.getName().startsWith("netnotes-")){
                                   
                                    tmpInfo.setAppName(asset.getName());
                                    tmpInfo.setTagName(asset.getTagName());
                                    tmpInfo.setAppUrl(asset.getUrl());
                                                                
                                }
                            }
                        }
                    }

                    Utils.getUrlJson(tmpInfo.getReleaseUrl(), (onReleaseInfo)->{
                        Object sourceObject = onReleaseInfo.getSource().getValue();
                        if(sourceObject != null && sourceObject instanceof com.google.gson.JsonObject){
                            com.google.gson.JsonObject releaseInfoJson = (com.google.gson.JsonObject) sourceObject;
                            UpdateInformation upInfo = new UpdateInformation(tmpInfo.getAppUrl(),tmpInfo.getTagName(),tmpInfo.getAppName(),null,tmpInfo.getReleaseUrl());
                            upInfo.setReleaseInfoJson(releaseInfoJson);
             
                            Platform.runLater(()->updateInformation.set(upInfo));
                        }
                    }, (releaseInfoFailed)->{

                    }, null);
                    
                 

                }
            },(onFailed)->{

            });

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
        dataObject.addProperty("updates", m_updates);
        return dataObject;
    }

    public void save() throws IOException {
        String jsonString = getJson().toString();
        Files.writeString(m_settingsFile.toPath(), jsonString);
    }

    protected class UpdateInformation{
        private String m_appUrl = null;
        private String m_tagName = null;
        private String m_appName = null; 
        private HashData m_appHashData = null;
        private String m_releaseUrl;
        private JsonObject m_releaseInfoJson = null;

        public UpdateInformation(){
        }

        public UpdateInformation(String appUrl, String tagName, String appName, HashData hashData, String releaseUrl){
            m_appUrl = appUrl;
            m_tagName = tagName;
            m_appName = appName;
            m_appHashData = hashData;
            m_releaseUrl = releaseUrl;
        }

        public void setReleaseInfoJson(JsonObject releaseInfo){
            m_releaseInfoJson = releaseInfo;
          
            m_appHashData = new HashData(m_releaseInfoJson.get("application").getAsJsonObject().get("hashData").getAsJsonObject());
        }

        public JsonObject getReleaseInfoJson(){
            return m_releaseInfoJson;
        }

        public String getReleaseUrl(){
            return m_releaseUrl;
        }

        public void setReleaseUrl(String releaseUrl){
            m_releaseUrl = releaseUrl;
        }

        public String getAppUrl(){
            return m_appUrl;
        }

        public String getTagName(){
            return m_tagName;
        }

        public String getAppName(){
            return m_appName;
        } 
        public HashData getAppHashData(){
            return m_appHashData;
        }

        public void setAppUrl(String url){
            m_appUrl = url;
        }

        public void setTagName(String tagName){
            m_tagName = tagName;
        }
        public void setAppName(String name){
            m_appName = name;
        } 
        public void setAppHashData(HashData hashData){
            m_appHashData = hashData;
        } 
    }
}
