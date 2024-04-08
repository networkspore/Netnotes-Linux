package com.netnotes;


import com.devskiller.friendly_id.FriendlyId;

import javafx.scene.image.WritableImage;

public class Effects {

    private String m_name;
    private String m_id;

    public Effects(String name) {
        m_id = FriendlyId.createFriendlyId();
        m_name = name;
    }

    public Effects(String id, String name) {
        m_id = id;
        m_name = name;
    }

    public void applyEffect(WritableImage img) {

    }

    public String getName() {
        return m_name;
    }

    public String getId() {
        return m_id;
    }
}
