package com.netnotes;

import java.time.LocalDateTime;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonObject;
import com.netnotes.IconButton.IconStyle;

import javafx.beans.property.SimpleObjectProperty;

import com.google.gson.JsonElement;

public class NamedNodeUrl {

    //networkType
    public final static String TESTNET_STRING = NetworkType.TESTNET.toString();
    public final static String MAINNET_STRING = NetworkType.MAINNET.toString();

    //type
    private int m_port = ErgoNodeData.DEFAULT_MAINNET_PORT;
    private String m_id = null;
    private String m_name = null;
    private String m_protocol = "http";
    private String m_ip = ErgoNodeData.DEFAULT_NODE_IP;
    private NetworkType m_networkType = NetworkType.MAINNET;
    private String m_apiKey = "";
   // private boolean m_rememberKey = true;

    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>(null);

    public NamedNodeUrl(){
        this(ErgoNodeData.PULIC_NODE_1, "Public Node #1");
    }

    public NamedNodeUrl(String id, String name) {
        m_id = id;
        m_name = name;
    }

    public NamedNodeUrl(JsonObject json) throws Exception {
        if (json != null) {

            JsonElement idElement = json.get("id");
            JsonElement nameElement = json.get("name");
            JsonElement ipElement = json.get("ip");
            JsonElement portElement = json.get("port");
            JsonElement networkTypeElement = json.get("networkType");
          //  JsonElement nodeTypeElement = json.get("nodeType");
            JsonElement apiKeyElement = json.get("apiKey");

            if(!(idElement != null && idElement.isJsonPrimitive() && networkTypeElement != null&& networkTypeElement.isJsonPrimitive() &&  nameElement != null && nameElement.isJsonPrimitive())){
                throw new Exception("Null data");
            }
            m_id =  idElement.getAsString();

            String networkTypeString = networkTypeElement.getAsString();
            m_networkType = networkTypeString.equals(TESTNET_STRING) ? NetworkType.TESTNET : NetworkType.MAINNET;
        

            m_name = nameElement.getAsString();
            m_ip = ipElement.getAsString();
            m_port = portElement.getAsInt();
            m_apiKey = apiKeyElement != null ? apiKeyElement.getAsString() : "";
                
            

        }else{
            throw new Exception("Null data");
        }
    }

    public NamedNodeUrl(String id, String name, String ip, int port, String apiKey, NetworkType networkType) {
        m_id = id;
        m_name = name;
        m_ip = ip;
        m_port = port;
        m_networkType = networkType;
        m_apiKey = apiKey;
    }

    public String getId() {
        return m_id;
    }

    public String getName() {
        return m_name;
    }

    public void setName(String value) {
        m_name = value;
    }

    public String getIP() {
        return m_ip;
    }

    public void setIp(String ip) {
        m_ip = ip;
    }

    public NetworkType getNetworkType() {
        return m_networkType;
    }

    public void setNetworkType(NetworkType networkType) {
        m_networkType = networkType;
    }

    public int getPort() {
        return m_port;
    }

    public void setPort(int port) {
        m_port = port;
    }

    public String getProtocol() {
        return m_protocol;
    }

    public void setProtocol(String value) {
        m_protocol = value;
    }

    public String getApiKey() {

        return m_apiKey;
    }

    public void setApiKey(String apiKey) {
        m_apiKey = apiKey;
    }

    public JsonObject getJsonObject() {
        JsonObject json = new JsonObject();
        json.addProperty("id", m_id);
        json.addProperty("name", m_name);
        json.addProperty("protocol", m_protocol);
        json.addProperty("ip", m_ip);
        json.addProperty("port", m_port);
        if (m_apiKey != null) {
            json.addProperty("apiKey", m_apiKey);
        }
        json.addProperty("networkType", m_networkType == null ? MAINNET_STRING : m_networkType.toString());
        return json;
    }

    public IconButton getButton() {

        IconButton btn = new IconButton(null, getRowString(), IconStyle.ROW);
        btn.setButtonId(m_id);
        return btn;

    }

    public SimpleObjectProperty<LocalDateTime> lastUpdatedProperty() {
        return m_lastUpdated;
    }

    public String getUrlString() {
        return m_protocol + "://" + m_ip + ":" + m_port ;
    }

    public String getRowString() {
        String formattedName = String.format("%-28s", m_name);
        String formattedUrl = String.format("%-30s", "(" + getUrlString() + ")");

        return formattedName + " " + formattedUrl;
    }

    @Override
    public String toString() {
        return m_name;
    }
}
