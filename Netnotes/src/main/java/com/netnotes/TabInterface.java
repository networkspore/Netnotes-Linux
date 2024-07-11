package com.netnotes;

import javafx.beans.property.SimpleStringProperty;

public interface TabInterface {
    String getTabId();
    String getName();
    void shutdown();
    void setCurrent(boolean value);
    boolean getCurrent();
    String getType();
    boolean isStatic();
    SimpleStringProperty titleProperty();
}
