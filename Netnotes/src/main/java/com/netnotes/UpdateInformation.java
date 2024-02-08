package com.netnotes;

import com.google.gson.JsonObject;
import com.utils.GitHubAPI.GitHubAsset;


public class UpdateInformation {
    private String m_jarUrl = null;
    private String m_tagName = null;
    private String m_jarName = null;
    private HashData m_jarHashData = null;
    private String m_releaseUrl;
    private JsonObject m_releaseInfoJson = null;
    private GitHubAsset[] m_assets = new GitHubAsset[0];

    public UpdateInformation() {
        
    }

    public UpdateInformation(String appUrl, String tagName, String appName, HashData hashData, String releaseUrl) {
        m_jarUrl = appUrl;
        m_tagName = tagName;
        m_jarName = appName;
        m_jarHashData = hashData;
        m_releaseUrl = releaseUrl;
    }

    public void setReleaseInfoJson(JsonObject releaseInfo) {
        m_releaseInfoJson = releaseInfo;

        m_jarHashData = new HashData(
                m_releaseInfoJson.get("application").getAsJsonObject().get("hashData").getAsJsonObject());
    }

    public GitHubAsset[] getAssets() {
        return m_assets;
    }

    public void setAssets(GitHubAsset[] assets) {
        m_assets = assets;
        for (GitHubAsset asset : assets) {
            if (asset.getName().equals("releaseInfo.json")) {
                setReleaseUrl(asset.getUrl());

            } else {
                if (asset.getName().endsWith(".jar")) {

                    setJarName(asset.getName());
                    setTagName(asset.getTagName());
                    setJarUrl(asset.getUrl());

                }
            }
        }
    }

    public JsonObject getReleaseInfoJson() {
        return m_releaseInfoJson;
    }

    public String getReleaseUrl() {
        return m_releaseUrl;
    }

    public void setReleaseUrl(String releaseUrl) {
        m_releaseUrl = releaseUrl;
    }

    public String getJarUrl() {
        return m_jarUrl;
    }

    public String getTagName() {
        return m_tagName;
    }

    public String getJarName() {
        return m_jarName;
    }

    public HashData getJarHashData() {
        return m_jarHashData;
    }

    public void setJarUrl(String url) {
        m_jarUrl = url;
    }

    public void setTagName(String tagName) {
        m_tagName = tagName;
    }

    public void setJarName(String name) {
        m_jarName = name;
    }

    public void setJarHashData(HashData hashData) {
        m_jarHashData = hashData;
    }
}