package com.netnotes;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class SimpleTab extends AppBox implements TabInterface {
    
    public static final String NAME = "Network";

    private final Button m_menuBtn;

    private SimpleBooleanProperty m_current = new SimpleBooleanProperty(true);
    
   
    public SimpleTab(Stage appStage, NetworksData networksData, SimpleDoubleProperty widthObject, Button menuBtn){
        super(NAME);
        m_menuBtn = menuBtn;
        prefWidthProperty().bind(widthObject);
      
    }

 
    public String getName(){
        return NAME;
    }
    public void shutdown(){

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

    private SimpleStringProperty m_titleProperty = new SimpleStringProperty(NAME);

    public SimpleStringProperty titleProperty(){
        return m_titleProperty;
    }
}

    

