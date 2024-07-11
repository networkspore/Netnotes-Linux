package com.netnotes;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class NetworkTabInterface extends VBox implements TabInterface {
    
  
    private final Button m_menuBtn;

    private SimpleBooleanProperty m_current = new SimpleBooleanProperty(true);
    private final String m_name;
    private final String m_tabId;
   
    public NetworkTabInterface(String tabId, String name, SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject, Button menuBtn){
        super();
        m_menuBtn = menuBtn;
        prefWidthProperty().bind(widthObject);
        prefHeightProperty().bind(heightObject);
       
        

        m_name = name;
        m_tabId = tabId;

        init();
    }

    public void init(){

    }


    

    public String getTabId(){
        return m_tabId;
    }
    public String getName(){
        return m_name;
    }
    public void shutdown(){
        prefWidthProperty().unbind();
    }
    public void setCurrent(boolean value){
        m_menuBtn.setId(value ? "activeMenuBtn" : "menuTabBtn");
        m_current.set(value);
    }

    
    public boolean getCurrent(){
        return m_current.get();
    } 

    public String getType(){
        return App.STATIC_TYPE;
    }

    public boolean isStatic(){
        return getType().equals(App.STATIC_TYPE);
    }

    private SimpleStringProperty m_titleProperty = new SimpleStringProperty("");

    public SimpleStringProperty titleProperty(){
        return m_titleProperty;
    }
}

    

