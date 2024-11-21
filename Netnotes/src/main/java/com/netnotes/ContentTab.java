package com.netnotes;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

public class ContentTab {

    private SimpleBooleanProperty m_isCurrent = new SimpleBooleanProperty(false);
    private SimpleStringProperty m_currentId = new SimpleStringProperty(null);
    private SimpleLongProperty m_shutdownMilliesProperty = new SimpleLongProperty(0);

    private String m_id;
    private String m_parentId;
    private Label m_tabLabel;
    private Pane m_pane;
    private HBox m_tabBox;
    private HBox m_tabCloseBox;
    private BufferedButton m_closeBtn;
    private ImageView m_tabImageView;
    private Image m_logo;
    private Tooltip m_tabLabelTooltip;

    public ContentTab(String id, String parentId, Image logo, String title, Pane pane){
        m_id = id;
        m_parentId = parentId;
     
        m_pane = pane;

        m_logo = logo;

        m_tabImageView = new ImageView(m_logo);
        m_tabImageView.setPreserveRatio(true);
        m_tabImageView.setFitWidth(App.MENU_BAR_IMAGE_WIDTH);
        m_tabImageView.setMouseTransparent(true);


        m_tabLabel = new Label(title);
        m_tabLabel.setId("tabLabel");
        m_tabLabel.setMouseTransparent(true);
        m_tabLabel.setPadding(new Insets(0,2,0,5));
         
        m_tabCloseBox = new HBox();
        m_tabCloseBox.setMinWidth(App.MENU_BAR_IMAGE_WIDTH);

        m_tabLabelTooltip = new Tooltip(title);
        m_tabLabelTooltip.setShowDelay(javafx.util.Duration.millis(100));
        m_tabLabelTooltip.textProperty().bind(m_tabLabel.textProperty());
        
        m_tabBox = new HBox(m_tabImageView, m_tabLabel, m_tabCloseBox);
        m_tabBox.setPadding(new Insets(2,3,2,2));
        m_tabBox.setId("tabBtn");
        m_tabBox.setFocusTraversable(true);
        m_tabBox.setAlignment(Pos.CENTER_LEFT);
        
        m_closeBtn = new BufferedButton("/assets/close-outline-white.png", 20);
        m_closeBtn.setId("closeBtn");
        m_closeBtn.setPadding(new Insets(0, 5, 0, 3));
    
        m_isCurrent.addListener((obs,oldval,newval)->{
            m_tabLabel.setId(newval ? "tabLabelSelected" : "tabLabel");
            m_tabBox.setId(newval ? "tabBtnSelected" : "tabBtn");
            if(newval){
                m_tabCloseBox.getChildren().add(m_closeBtn);
            }else{
                m_tabCloseBox.getChildren().remove(m_closeBtn);
            }
        });

        m_tabBox.onMouseEnteredProperty().addListener((mouseEvent)->{
            if(!m_tabCloseBox.getChildren().contains(m_closeBtn)){
                m_tabCloseBox.getChildren().add(m_closeBtn);
            }
        }); 
        m_tabBox.onMouseExitedProperty().addListener(mouseEvent->{
            if(!isCurrent().get() && m_tabCloseBox.getChildren().contains(m_closeBtn)){
                m_tabCloseBox.getChildren().remove(m_closeBtn);
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

    public ImageView getTabImageView(){
        return m_tabImageView;
    }

    public Image geTabImage(){
        return m_logo;
    }

    public void setTabImage(Image image){
        m_logo = image;
        m_tabImageView.setImage(image);
    }

    public SimpleStringProperty currentIdProperty(){
        return m_currentId;
    }

    public void onCloseBtn(EventHandler<ActionEvent> eventHandler){
        m_closeBtn.setOnAction(eventHandler);
    }
    public void onTabClicked(EventHandler<MouseEvent> mouseEventHandler){
        m_tabBox.setOnMouseClicked(mouseEventHandler);
    }

    public void close(){
        m_closeBtn.fire();
    }

    public SimpleBooleanProperty isCurrent(){
        return m_isCurrent;
    }

    public Label getTabLabel(){
        return m_tabLabel;
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

    public SimpleLongProperty shutdownMilliesProperty(){
        return m_shutdownMilliesProperty;
    }



}
