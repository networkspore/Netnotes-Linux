package com.netnotes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import com.utils.Utils;

import javafx.animation.PauseTransition;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

public class LockField extends HBox {
    private final Label m_nameLabel;
    private String m_walletName = "";
    private final HBox m_addressFieldBox;
    private final HBox m_addressBox;
    private final Button m_unlockBtn;
    private String m_lockString;
    private final HBox m_unlockBtnBox;
    private SimpleStringProperty m_lockId = new SimpleStringProperty(null);
    private final MenuButton m_openBtn;
    private final BufferedButton m_copyBtn;
    private final BufferedButton m_sendBtn;
    private final Button m_magnifyBtn;
    private final Label m_label;
    private String m_unlockLabelString;
    private String m_lockLabelString = "⚿ ";
    private final BufferedButton m_lockBtn;
    private SimpleStringProperty m_addressProperty = new SimpleStringProperty("");

    private final HBox m_topBox;

    public LockField(String lockString,String unlockLabelString, String prompt){
        super();
        setAlignment(Pos.CENTER_LEFT);
        m_lockString = String.format("%-8s",lockString);
        m_label = new Label(m_lockLabelString);
        m_label.setId("logoBox");
        
        m_unlockLabelString = unlockLabelString;

        m_lockBtn = new BufferedButton();
        m_lockBtn.setId("toolBtn");
        m_lockBtn.setImage(App.closeImg);
        m_lockBtn.getBufferedImageView().setFitWidth(20);
        m_lockBtn.setPadding(new Insets(0, 1, 0, 1));


        m_nameLabel = new Label(lockString);
        HBox.setHgrow(m_nameLabel, Priority.ALWAYS);
        m_nameLabel.setMaxWidth(90);

        m_unlockBtn = new Button(prompt);
        m_unlockBtn.setPadding(new Insets(2,15,2,15));
        m_unlockBtn.setAlignment(Pos.CENTER_LEFT);

        HBox textBox = new HBox(m_nameLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);
        textBox.setPadding(new Insets(0,0,0,0));

        m_unlockBtnBox = new HBox(m_unlockBtn);
        HBox.setHgrow(m_unlockBtnBox, Priority.ALWAYS);
        m_unlockBtnBox.setAlignment(Pos.CENTER_LEFT);
      
        m_unlockBtn.prefWidthProperty().bind(m_unlockBtnBox.widthProperty().subtract(1));

        m_openBtn = new MenuButton();
        m_openBtn.setId("arrowMenuButton");

        int fontSize = 16;
        int w = Utils.getCharacterSize(fontSize);

        Binding<String> addressStringBinding = Bindings.createObjectBinding(()->{
            String addressString = m_addressProperty.get();
            if(addressString != null && !addressString.equals(m_lockString)){
                double btnWidth = m_openBtn.widthProperty().get() - 30;
                int elipsisSize = 3;
                int adrStrLen = addressString.length();

                if(adrStrLen > 5){
                    int characters = ((int) (btnWidth / w)) -elipsisSize;
                    if(characters > 6 && characters < adrStrLen){
                        
                        int len = (int) (characters / 2);         
                        String returnString = addressString.substring(0, len ) + "…" + addressString.substring(adrStrLen- len, adrStrLen) ;
                    
                        return returnString;
                    }else{
                        return addressString;
                    }
                }else{
                    return addressString;
                }
            }else{
                return addressString;
            }
        }
        , m_addressProperty, m_openBtn.widthProperty());

        m_openBtn.textProperty().bind(addressStringBinding);

        Tooltip magnifyTip = new Tooltip("🔍 Magnify");
        magnifyTip.setShowDelay(Duration.millis(100));

        m_magnifyBtn =new Button("🔍");
        m_magnifyBtn.setId("logoBtn");
        m_magnifyBtn.setTooltip(magnifyTip);
        m_magnifyBtn.setOnAction(e->{
            String adrText = getAddress();
            App.showMagnifyingStage("Wallet: " +m_walletName+" - Address: " + m_nameLabel.getText(), adrText);
        });
        

        Tooltip copiedTooltip = new Tooltip("copied");

        Tooltip copyToolTip = new Tooltip("Copy");
        copyToolTip.setShowDelay(Duration.millis(100));
        
        m_copyBtn = new BufferedButton("/assets/copy-30.png", App.MENU_BAR_IMAGE_WIDTH);
        m_copyBtn.setTooltip(copyToolTip);

        Tooltip sendToolTip = new Tooltip("Send");
        sendToolTip.setShowDelay(Duration.millis(100));

        m_sendBtn = new BufferedButton("/assets/arrow-send-white-30.png", App.MENU_BAR_IMAGE_WIDTH);
        m_sendBtn.setTooltip(sendToolTip);

        m_copyBtn.setOnAction(e->{
            e.consume();
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(getAddress());
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



        m_addressFieldBox = new HBox(m_openBtn, m_magnifyBtn, m_copyBtn, m_lockBtn);
        HBox.setHgrow(m_addressFieldBox, Priority.ALWAYS);
        m_addressFieldBox.setId("bodyBox");

        m_openBtn.prefWidthProperty().bind(m_addressFieldBox.widthProperty().subtract(1).subtract(m_magnifyBtn.widthProperty()).subtract(m_copyBtn.widthProperty()).subtract(m_lockBtn.widthProperty()));

        m_addressBox = new HBox( m_addressFieldBox, m_sendBtn);
        HBox.setHgrow(m_addressBox, Priority.ALWAYS);
        m_addressBox.setPadding(new Insets(0,0,0,10));
   
           
        m_lockBtn.setOnAction(e->{
            setLocked();
        });
       
        m_topBox = new HBox();
        HBox.setHgrow(m_topBox, Priority.ALWAYS);
 
        
        getChildren().addAll(m_label, textBox, m_unlockBtnBox);

      /*  */
    }

    public void copyAddressToClipboard(){
        m_copyBtn.fire();
    }



    public void setLocked(){
        if(m_lockId.get() != null){
            m_lockId.set(null);
            m_label.setText(m_lockLabelString);

 
           

            if(!getChildren().contains(m_unlockBtnBox)){
                getChildren().add(m_unlockBtnBox);
            }

            if(getChildren().contains(m_addressBox)){
                getChildren().remove(m_addressBox);
            }

            m_nameLabel.setText(m_lockString);
            setAddress(m_lockString);
        }
    }

    public void setUnlocked(String id){

        if( m_lockId.get() == null ){
            m_label.setText(m_unlockLabelString);
          
            if(getChildren().contains(m_unlockBtnBox)){
                getChildren().remove(m_unlockBtnBox);
            }
            
            if(!getChildren().contains(m_addressBox)){
                getChildren().add(m_addressBox);
            }
            
            m_lockId.set(id);
        }
    }

    public SimpleStringProperty lockId(){
        return m_lockId;
    }

    public void setOnSend(EventHandler<ActionEvent> onSend){
        m_sendBtn.setOnAction(onSend);
    }

    public void setPasswordAction( EventHandler<ActionEvent> onAction){
        m_unlockBtn.setOnAction(onAction);
    }

    public void setOnMenuShowing(ChangeListener<Boolean> onShowing){
  
        m_openBtn.showingProperty().addListener(onShowing);
 
    }

    public ObservableList<MenuItem> getItems(){
        return m_openBtn.getItems();
    }

    public String getLockString(){
        return m_lockString;
    }

    public StringProperty textProperty(){
        return m_nameLabel.textProperty();
    }

 

    public String getName(){
        return m_nameLabel.getText();
    }

    public void setName(String name){

        m_nameLabel.setText(name);
    }

    public void setAddress(String address){

        m_addressProperty.set(address);
     
    }

    public String getAddress(){
        return m_addressProperty.get();
    }

    public String getWalletName(){
        return m_walletName;
    }

    public void setWalletName(String walletName){
        m_walletName = walletName;
    }

    public boolean isUnlocked(){
        return m_lockId.get() != null;
    }

    public String getLockId(){
        return m_lockId.get();
    }


}
