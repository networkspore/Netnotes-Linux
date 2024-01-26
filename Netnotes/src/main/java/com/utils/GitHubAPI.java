package com.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;


public class GitHubAPI {

    public class GitHubAsset{
        private String m_name;
        private String m_url;
        private String m_contentType;
        private long m_size;
        private String m_tagName;

        public GitHubAsset(String name, String url, String contentType, long size){
            m_name = name;
            m_url = url;
            m_contentType = contentType;
            m_size = size;
        }
        
        public GitHubAsset(String name, String url, String contentType, long size, String tagName){
            m_name = name;
            m_url = url;
            m_contentType = contentType;
            m_size = size;
            m_tagName = tagName;
        }

        public String getTagName(){
            return m_tagName;
        }

        public String getName(){
            return m_name;
        }

        public String getUrl(){
            return m_url;
        }

        public String getContentType(){
            return m_contentType;
        }

        public long getSize(){
            return m_size;
        }
    }

    public final static String GITHUB_API_URL = "https://api.github.com";

    private String m_username;
    private String m_project;


    public GitHubAPI(String username, String project){
        m_username = username;
        m_project = project;
        
    }

    public String getUrlLatest(){
        return GITHUB_API_URL + "/repos/" + m_username + "/" + m_project + "/releases/latest";
    }
    
    public String getUrlAll(){
        return GITHUB_API_URL + "/repos/" + m_username + "/" + m_project + "/releases";
    }


    public void getAssetsAllLatest(EventHandler<WorkerStateEvent> onFinished, EventHandler<WorkerStateEvent> onFailed){
        Utils.getUrlJsonArray(getUrlAll(), (onSucceeded)->{
            Object sourceObject = onSucceeded.getSource().getValue();
             if (sourceObject != null && sourceObject instanceof JsonArray) {
                JsonArray allReleases = (JsonArray) sourceObject;
                JsonElement elementObject = allReleases.get(0);

                if (elementObject != null && elementObject.isJsonObject()) {
                     JsonObject gitHubApiJson = elementObject.getAsJsonObject();
                    String tagName = gitHubApiJson.get("tag_name").getAsString();
                    JsonElement assetsElement = gitHubApiJson.get("assets");
                    if (assetsElement != null && assetsElement.isJsonArray()) {
                        
                        JsonArray assetsArray = assetsElement.getAsJsonArray();
                        if (assetsArray.size() > 0) {
                            int assetArraySize = assetsArray.size();
                            GitHubAsset[] assetArray = new GitHubAsset[assetArraySize]; 

                            for(int i = 0; i < assetArraySize ; i++){
                                JsonElement assetElement = assetsArray.get(i);

                                if (assetElement != null && assetElement.isJsonObject()) {
                                    JsonObject assetObject = assetElement.getAsJsonObject();

                                    JsonElement downloadUrlElement = assetObject.get("browser_download_url");
                                    JsonElement nameElement = assetObject.get("name");
                                    JsonElement contentTypeElement = assetObject.get("content_type");
                                    JsonElement sizeElement = assetObject.get("size");

                                    if (downloadUrlElement != null && downloadUrlElement.isJsonPrimitive()) {
                                        
                                        String url = downloadUrlElement.getAsString();
                                        String name = nameElement.getAsString();
                                        long contentSize = sizeElement.getAsLong();
                                        String contentTypeString = contentTypeElement.getAsString();

                                        assetArray[i] = new GitHubAsset(name, url, contentTypeString, contentSize, tagName);

                                    }
                                }
                            }
                            Utils.returnObject((Object) assetArray, onFinished, null); 
                        }else{
                            Utils.returnObject(null, onFinished, null);
                        }
                    }else{
                        Utils.returnObject(null, onFinished, null);
                    }
                }else{
                    Utils.returnObject(null, onFinished, null);
                }
            }else{
                Utils.returnObject(null, onFinished, null);
            }
        }, onFailed, null);
    }


    public void getAssetsLatest(EventHandler<WorkerStateEvent> onFinished, EventHandler<WorkerStateEvent> onFailed){
         Utils.getUrlJson(getUrlLatest(), (onSucceeded) -> {
                Object sourceObject = onSucceeded.getSource().getValue();
                if (sourceObject != null && sourceObject instanceof JsonObject) {
                    JsonObject gitHubApiJson = (JsonObject) sourceObject;
                 
                    String tagName = gitHubApiJson.get("tag_name").getAsString();
                    JsonElement assetsElement = gitHubApiJson.get("assets");
                    if (assetsElement != null && assetsElement.isJsonArray()) {
                        
                        JsonArray assetsArray = assetsElement.getAsJsonArray();
                        if (assetsArray.size() > 0) {
                            int assetArraySize = assetsArray.size();
                            GitHubAsset[] assetArray = new GitHubAsset[assetArraySize]; 

                            for(int i = 0; i < assetArraySize ; i++){
                                JsonElement assetElement = assetsArray.get(i);

                                if (assetElement != null && assetElement.isJsonObject()) {
                                    JsonObject assetObject = assetElement.getAsJsonObject();

                                    JsonElement downloadUrlElement = assetObject.get("browser_download_url");
                                    JsonElement nameElement = assetObject.get("name");
                                    JsonElement contentTypeElement = assetObject.get("content_type");
                                    JsonElement sizeElement = assetObject.get("size");

                                    if (downloadUrlElement != null && downloadUrlElement.isJsonPrimitive()) {
                                        
                                        String url = downloadUrlElement.getAsString();
                                        String name = nameElement.getAsString();
                                        long contentSize = sizeElement.getAsLong();
                                        String contentTypeString = contentTypeElement.getAsString();

                                        assetArray[i] = new GitHubAsset(name, url, contentTypeString, contentSize, tagName);

                                    }
                                }
                            }
                            Utils.returnObject((Object)assetArray, onFinished, null);
                        }else{
                            Utils.returnObject(null, onFinished, null);
                        }
                    }else{
                        Utils.returnObject(null, onFinished, null);
                    }

                }else{
                    Utils.returnObject(null, onFinished, null);
                }
            }, onFailed, null);
        }


    }


