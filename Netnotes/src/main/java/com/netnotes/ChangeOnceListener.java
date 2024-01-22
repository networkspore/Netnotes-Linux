package com.netnotes;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class ChangeOnceListener<T> implements ChangeListener<T> {

    private ChangeListener<T> wrapped;

    public ChangeOnceListener(ChangeListener<T> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
        observable.removeListener(this);
        wrapped.changed(observable, oldValue, newValue);
    }

}
