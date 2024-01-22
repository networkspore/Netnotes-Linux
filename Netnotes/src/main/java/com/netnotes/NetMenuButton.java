package com.netnotes;

import javafx.scene.control.MenuButton;

public class NetMenuButton extends MenuButton {

    private String m_networkId;
    private String m_name;

    public NetMenuButton(String networkId, String name, String text, Object userData) {
        super(text);
        m_name = name;
        m_networkId = networkId;
        setUserData(userData);
    }

    public String getNetworkId() {
        return m_networkId;
    }

    public String getName() {
        return m_name;
    }
}
