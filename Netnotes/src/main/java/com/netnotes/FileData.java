package com.netnotes;

import java.io.File;
import java.nio.file.Files;

import com.devskiller.friendly_id.FriendlyId;

import com.utils.Utils;

public class FileData {

    private boolean m_valid = false;
    private String m_id = null;
    private File m_file = null;
    private String m_name = null;
    private HashData m_hashData = null;
    private String m_subId = null;
    private String m_contentType = null;
    private String m_baseContentType = null;

    public FileData(String name, File file) {
        this(FriendlyId.createFriendlyId(), file, name, null);

    }

    public FileData(String id, File file, String name, String subId) {
        m_id = id;
        m_file = file;
        m_name = name;
        m_subId = subId;

        setup();
    }

    public void setup() {
        if (m_file != null && m_file.isFile()) {
            try {
                m_contentType = Files.probeContentType(m_file.toPath());
                m_baseContentType = m_contentType.split("/")[0];
                m_hashData = new HashData(Utils.digestFile(m_file));
                m_valid = true;
            } catch (Exception e) {

            }
        }
    }

    public String getId() {
        return m_id;
    }

    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        m_name = name;
    }

    public String getSubId() {
        return m_subId;
    }

    public void setSubId(String subId) {
        m_subId = subId;
    }

    public String getContentType() {
        return m_contentType;
    }

    public String getBaseContentType() {
        return m_baseContentType;
    }

    public HashData getHashData() {
        return m_hashData;
    }

    public boolean getValid() {
        return m_valid;
    }

}
