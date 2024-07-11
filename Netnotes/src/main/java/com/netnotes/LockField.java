package com.netnotes;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class LockField extends HBox {
    private final TextField m_textField;
    private final PasswordField m_passField;
    private final Text m_text;
    private String m_lockString;
    private String m_unlockString;
    private String m_prompt;
    private final HBox m_fieldBox;
    private final Button m_enterBtn;
    private SimpleStringProperty m_lockId = new SimpleStringProperty(null);
    private final Label m_openBtn;
    private  EventHandler<MouseEvent> m_onMouseClicked;
    private final BufferedButton m_copyBtn;
    private final Label m_label;
    private String m_unlockLabelString;
    private String m_lockLabelString = "🔒 ";
    private final BufferedButton m_lockBtn;

    public LockField(String lockString,String unlockLabelString, String unlockString, String prompt){
        super();
        setAlignment(Pos.CENTER_LEFT);
        m_lockString = lockString;
        m_unlockString = unlockString;
        m_label = new Label(m_lockLabelString);
        m_label.setId("logoBox");

        m_unlockLabelString = unlockLabelString;

        m_lockBtn = new BufferedButton();

        m_lockBtn.setId("toolBtn");
        m_lockBtn.setImage(App.closeImg);
        m_lockBtn.getBufferedImageView().setFitWidth(20);
        m_lockBtn.setPadding(new Insets(0, 1, 0, 1));

        m_text = new Text(lockString);
        m_text.setFont(App.txtFont);
        m_text.setFill(App.txtColor);

        m_enterBtn = new Button(); 
        m_enterBtn.setMinHeight(15);
        m_enterBtn.setText("[enter]");
        m_enterBtn.setId("toolBtn");
        m_enterBtn.setPadding(new Insets(0,5,0,5));
       
        m_textField = new TextField(lockString);
    
        m_textField.setPadding(new Insets(0,0,0,10));
        HBox.setHgrow(m_textField, Priority.ALWAYS);
   
        m_passField = new PasswordField();
        m_passField.setPromptText(prompt);
        HBox.setHgrow(m_passField, Priority.ALWAYS);
        m_passField.setPadding(new Insets(0,5,0,5));


        HBox textBox = new HBox(m_text);
        textBox.setAlignment(Pos.CENTER_LEFT);
        textBox.setPadding(new Insets(0,0,0,0));

        m_fieldBox = new HBox(m_passField);
        HBox.setHgrow(m_fieldBox, Priority.ALWAYS);
        m_fieldBox.setAlignment(Pos.CENTER_LEFT);
        m_fieldBox.setId("bodyBox");
        m_fieldBox.setMinHeight(18);
        getChildren().addAll(m_label, textBox, m_fieldBox);

        m_passField.focusedProperty().addListener((obs,oldval,newval)->{
            if(newval){
                if(!m_fieldBox.getChildren().contains(m_enterBtn)){
                    m_fieldBox.getChildren().add(m_enterBtn);
                }
            }else{
                if(m_fieldBox.getChildren().contains(m_enterBtn)){
                    m_fieldBox.getChildren().remove(m_enterBtn);
                }
            }
        });

        m_passField.setOnAction(e->m_enterBtn.fire());
   
        m_openBtn = new Label("⏷");
        m_openBtn.setId("lblBtn");
      

        Tooltip copiedTooltip = new Tooltip("copied");

        Tooltip copyToolTip = new Tooltip("Copy");
        copyToolTip.setShowDelay(Duration.millis(100));
        m_copyBtn = new BufferedButton("/assets/copy-30.png", App.MENU_BAR_IMAGE_WIDTH);
        m_copyBtn.setTooltip(copyToolTip);

        m_copyBtn.setOnAction(e->{
            e.consume();
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(m_textField.getText());
            clipboard.setContent(content);

            Point2D p = m_copyBtn.localToScene(0.0, 0.0);
            copyToolTip.hide();

            copiedTooltip.show(
                m_copyBtn,  
                p.getX() + m_copyBtn.getScene().getX() + m_copyBtn.getScene().getWindow().getX(), 
                (p.getY()+ m_copyBtn.getScene().getY() + m_copyBtn.getScene().getWindow().getY())-m_copyBtn.getLayoutBounds().getHeight()
            );
            PauseTransition pt = new PauseTransition(Duration.millis(1600));
            pt.setOnFinished(ptE->{
                copiedTooltip.hide();
            });
            pt.play();
        });
           
        m_lockBtn.setOnAction(e->{
            setLocked();
        });
       
    
      /*  */
    }

    @Override
    public void requestFocus(){
        if(!getUnLocked()){
            m_passField.requestFocus();
        }else{
            super.requestFocus();
        }
    }

    public void setLocked(){
        if(m_lockId.get() != null){
            m_lockId.set(null);
            m_label.setText(m_lockLabelString);

            removeEventFilter(MouseEvent.MOUSE_CLICKED, m_onMouseClicked);
            m_passField.setText("");
           

            if(m_fieldBox.getChildren().contains(m_textField)){
                m_fieldBox.getChildren().removeAll(m_textField, m_openBtn, m_copyBtn, m_lockBtn);
                m_fieldBox.getChildren().addAll(m_passField);
            }

            m_text.setText(m_lockString);
            m_textField.setText(m_lockString);
            

            Platform.runLater(()->m_passField.requestFocus());
        }
    }

    public void setUnlocked(String id){

        if( m_lockId.get() == null ){
            m_label.setText(m_unlockLabelString);
          
            m_text.setText(m_unlockString);
            if(m_fieldBox.getChildren().contains(m_passField)){
                m_fieldBox.getChildren().remove(m_passField);
                m_fieldBox.getChildren().addAll(m_textField, m_openBtn, m_copyBtn, m_lockBtn);
                m_passField.setText("");
            }
            
            if(m_fieldBox.getChildren().contains(m_enterBtn)){
                m_fieldBox.getChildren().remove(m_enterBtn);
            }
            
            
            m_lockId.set(id);
        }
    }

    public SimpleStringProperty lockId(){
        return m_lockId;
    }



    public void setOnAction( EventHandler<ActionEvent> onEnter){
        m_enterBtn.setOnAction(onEnter);
    }

    public void setOnMenu(EventHandler<MouseEvent> onMouseClicked){
        m_onMouseClicked = onMouseClicked;
        m_textField.addEventFilter(MouseEvent.MOUSE_CLICKED, m_onMouseClicked);
        m_openBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, m_onMouseClicked);
    }

    public String getLockString(){
        return m_lockString;
    }

    public StringProperty textProperty(){
        return m_textField.textProperty();
    }

    public String getText(){
        return m_textField.getText();
    }

    public void setText(String text){
        m_textField.setText(text);
    }

    public boolean getUnLocked(){
        return m_lockId.get() != null;
    }

    public String getPassString(){
        return m_passField.getText();
    }

    public void resetPass(){
        m_passField.setText("");
    }

    public void setPromptText(String prompt){
        m_passField.setPromptText(prompt);
    }

    public String getPromptText(){
        return m_passField.getPromptText();
    }

    public String getLockId(){
        return m_lockId.get();
    }


}
