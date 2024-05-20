package com.netnotes;

import javafx.scene.layout.HBox;

public interface ControlInterface {
    void reset();
    void start();
    void stop();
    void toggle();
    boolean isActive();
    HBox getControlBox();
    void cancel();
    boolean isCancelled();
}
