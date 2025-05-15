package com.netnotes;

import io.netnotes.engine.Network;
import io.netnotes.engine.NetworksData;

import javafx.scene.layout.VBox;

public class AddressAmountsList extends Network {

    private VBox m_gridBox = new VBox();

    public AddressAmountsList(NetworksData networksData) {
        super(null, "Address Acounts", "AMOUNTS_LIST", networksData);

    }

    public VBox getGridBox() {
        return m_gridBox;
    }
}
