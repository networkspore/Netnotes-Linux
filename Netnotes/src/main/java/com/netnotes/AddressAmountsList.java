package com.netnotes;

import io.netnotes.engine.Network;
import io.netnotes.engine.NoteInterface;

import javafx.scene.layout.VBox;

public class AddressAmountsList extends Network {

    private VBox m_gridBox = new VBox();

    public AddressAmountsList(NoteInterface parentInterface) {
        super(null, "Address Acounts", "AMOUNTS_LIST", parentInterface);

    }

    public VBox getGridBox() {
        return m_gridBox;
    }
}
