package com.netnotes;

import java.awt.image.BufferedImage;

import com.devskiller.friendly_id.FriendlyId;

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

    public void applyEffect(BufferedImage img) {

    }

    public String getName() {
        return m_name;
    }

    public String getId() {
        return m_id;
    }
}
