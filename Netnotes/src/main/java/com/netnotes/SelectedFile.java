package com.netnotes;

import java.io.File;

public class SelectedFile {

    private File m_file;

    public SelectedFile(File file) {
        m_file = file;
    }

    public File getFile() {
        return m_file;
    }

    public void setFile(File file) {
        m_file = file;
    }
}
