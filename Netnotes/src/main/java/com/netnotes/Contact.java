package com.netnotes;

import com.google.gson.JsonObject;
import io.netnotes.engine.IconButton;

public class Contact extends IconButton {

    private String m_contactId;

    private JsonObject m_contactDataJson;

    public Contact(String name, String id, JsonObject contactDataJson) {
        super(name);
        m_contactId = id;
        m_contactDataJson = contactDataJson;
    }

    public String getContactId() {
        return m_contactId;
    }

    public JsonObject getContactData() {
        return m_contactDataJson;
    }

    public void setContactData(JsonObject contactData) {
        m_contactDataJson = contactData;
    }
}
