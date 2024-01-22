package com.netnotes;

import java.time.LocalDateTime;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonObject;
import com.netnotes.IconButton.IconStyle;

import javafx.beans.property.SimpleObjectProperty;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonElement;

public class ErgoNetworkUrl {

    //networkType
    public final static String TESTNET_STRING = NetworkType.TESTNET.toString();
    public final static String MAINNET_STRING = NetworkType.MAINNET.toString();

    //type

    private String m_id;
    private String m_name = "localhost";
    private String m_protocol = "https";
    private String m_url = "127.0.0.1";
    private int m_port = 443;
    private NetworkType m_networkType = NetworkType.MAINNET;



    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>(null);

    public ErgoNetworkUrl(String id, String name, String protocol, String url, int port, NetworkType networkType) {
        m_id = id;
        m_name = name;
        m_protocol = protocol;
        m_url = url;
        m_port = port;
        m_networkType = networkType;
    }

    public ErgoNetworkUrl(JsonObject json) {
        if (json != null) {

            JsonElement idElement = json.get("id");
            JsonElement nameElement = json.get("name");
            JsonElement urlElement = json.get("url");
            JsonElement portElement = json.get("port");
            JsonElement networkTypeElement = json.get("networkType");
            JsonElement protocolElement = json.get("protocol");


            m_id = idElement != null ? idElement.getAsString() : FriendlyId.createFriendlyId();

            if (networkTypeElement != null && networkTypeElement.isJsonPrimitive()) {
                String networkTypeString = networkTypeElement.getAsString();
                m_networkType = networkTypeString.equals(TESTNET_STRING) ? NetworkType.TESTNET : NetworkType.MAINNET;
            }

            m_name = nameElement != null && idElement != null ? nameElement.getAsString() : m_networkType.toString() + " #" + m_id;
            m_url = urlElement != null ? urlElement.getAsString() : m_url;
            m_port = portElement != null ? portElement.getAsInt() : m_port;
            m_protocol = protocolElement != null ? protocolElement.getAsString() : m_protocol;
        }
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

    public String getURL() {
        return m_url;
    }

    public void setURL(String url) {
        m_url = url;
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



    public JsonObject getJsonObject() {
        JsonObject json = new JsonObject();
        json.addProperty("id", m_id);
        json.addProperty("name", m_name);
        json.addProperty("protocol", m_protocol);
        json.addProperty("url", m_url);
        json.addProperty("port", m_port);
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
        return m_protocol + "://" + m_url + (m_port != 80 && m_port != 443 ? ":" + m_port : "" );
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
