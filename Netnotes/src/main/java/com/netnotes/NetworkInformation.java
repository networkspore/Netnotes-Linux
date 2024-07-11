package com.netnotes;

public class NetworkInformation {
    private String m_networkId;
    private String m_networkName;
    private String m_iconString;
    private String m_smallIconString;
    private String m_description;

    public NetworkInformation(String networkId, String networkName, String iconString){
        m_networkId = networkId;
        m_networkName = networkName;
        m_iconString = iconString;
        m_smallIconString = iconString;
    }

    public NetworkInformation(String networkId, String networkName, String iconString, String smallIconString, String description){
        m_networkId = networkId;
        m_networkName = networkName;
        m_iconString = iconString;
        m_smallIconString = iconString;
        m_description = description;
    }

    public String getDescription(){
        return m_description;
    }

    public String getNetworkId(){
        return m_networkId;
    }

    public String getNetworkName(){
        return m_networkName;
    }

    public String iconString(){
        return m_iconString;
    }

    public String getSmallIconString(){
        return m_smallIconString;
    }
}