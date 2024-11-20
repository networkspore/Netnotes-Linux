package com.netnotes;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class ContentTab {

    private SimpleBooleanProperty m_isCurrent = new SimpleBooleanProperty(false);
    private SimpleStringProperty m_currentId = new SimpleStringProperty(null);

    private String m_id;
    private String m_parentId;
    private Text m_title;
    private Pane m_pane;
    private HBox m_tabBox;
    private HBox m_titleCloseBox;
    private BufferedButton m_closeBtn;

    public ContentTab(String id, String parentId, String title, Pane pane){
        m_id = id;
        m_parentId = parentId;
        
        m_pane = pane;
        
        m_title = new Text(title);
        m_title.setFont(App.txtFont);
        m_title.setFill(Color.web("#777777"));
        m_title.setMouseTransparent(true);
         
        m_titleCloseBox = new HBox();
        m_titleCloseBox.setMinWidth(App.MENU_BAR_IMAGE_WIDTH);
        
        m_tabBox = new HBox(m_title, m_titleCloseBox);
        m_tabBox.setPadding(new Insets(2,2,2,2));
        m_tabBox.setId("tabBtn");
        
        m_closeBtn = new BufferedButton("/assets/close-outline-white.png", 20);
        m_closeBtn.setId("closeBtn");
        m_closeBtn.setPadding(new Insets(0, 5, 0, 3));
    
        m_isCurrent.addListener((obs,oldval,newval)->{
            m_title.setFill(newval ? App.txtColor : Color.web("#777777"));
            m_tabBox.setId(newval ? "tabBtnSelected" : "tabBtn");
            if(newval){
                m_titleCloseBox.getChildren().add(m_closeBtn);
            }else{
                m_titleCloseBox.getChildren().remove(m_closeBtn);
            }
        });

        m_currentId.addListener((obs,oldval,newval)->{
            if(newval != null && newval.equals(m_id)){
                m_isCurrent.set(true);
            }else{
                m_isCurrent.set(false);
            }
        });

    }

    public SimpleStringProperty currentIdProperty(){
        return m_currentId;
    }

    public void onCloseBtn(EventHandler<ActionEvent> eventHandler){
        m_closeBtn.setOnAction(eventHandler);
    }
    public void onTabClicked(EventHandler<MouseEvent> mouseEventHandler){
        m_titleCloseBox.setOnMouseClicked(mouseEventHandler);
    }

    public void close(){
        m_closeBtn.fire();
    }

    public SimpleBooleanProperty isCurrent(){
        return m_isCurrent;
    }

    public Text getTitle(){
        return m_title;
    }

    public String getId() {
        return m_id;
    }

    public void setId(String id) {
        this.m_id = id;
    }

    public String getParentId() {
        return m_parentId;
    }

    public void setParentId(String parentId) {
        this.m_parentId = parentId;
    }

    public HBox getTabBox(){
        return m_tabBox;
    }

    public Pane getPane() {
        return m_pane;
    }

    public void setPane(Pane pane) {
        this.m_pane = pane;
    }


}
