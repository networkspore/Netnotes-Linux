package com.netnotes;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;

import com.devskiller.friendly_id.FriendlyId;


import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;


public class AddressBox extends HBox {

    public final static String EMPTY_TEXT = "Enter Address";

    private NetworkType m_networkType = NetworkType.MAINNET;
    // private boolean m_valid = false;

    private String m_id = FriendlyId.createFriendlyId();
    private int m_minHeight = 75;

    private final SimpleObjectProperty<LocalDateTime> m_LastUpdated = new SimpleObjectProperty<LocalDateTime>(LocalDateTime.now());

    private SimpleObjectProperty<AddressInformation> m_addressInformation = new SimpleObjectProperty<>(null);
    private StackPane m_stackPane;
    private ImageView m_backgroundImgView;
    private Text m_headingText;
    private TextArea m_addressTextArea;

    private ArrayList<String> m_invalidAddressList = new ArrayList<>();

//String explorerId, NetworksData networksData
    public AddressBox(AddressInformation addressInformation, Scene scene, NetworkType networkType) {
        super();
        setFocusTraversable(true);
        setMinHeight(m_minHeight);
        setPrefHeight(m_minHeight);
        setId("bodyBox2");

        m_networkType = networkType;
        m_addressInformation.set(addressInformation);
        
        m_headingText = new Text();
        m_headingText.setMouseTransparent(true);
        m_headingText.setFont(App.txtFont);
        m_headingText.setFill(App.txtColor);
        
        m_backgroundImgView = new ImageView();
        m_backgroundImgView.setFitHeight(m_minHeight);
        m_backgroundImgView.setMouseTransparent(true);
        m_backgroundImgView.setPreserveRatio(true);
        m_backgroundImgView.setId("backgroundImage");

        HBox headingTextBox = new HBox(m_headingText);
        HBox.setHgrow(headingTextBox, Priority.ALWAYS);
        headingTextBox.setAlignment(Pos.CENTER);
        headingTextBox.setMouseTransparent(true);

        m_addressTextArea = new TextArea();
        HBox.setHgrow(m_addressTextArea, Priority.ALWAYS);
        m_addressTextArea.setId("priceFieldRegular");
        m_addressTextArea.setEditable(false);
        m_addressTextArea.setWrapText(true);
        m_addressTextArea.setMouseTransparent(true);
        m_addressTextArea.setMinHeight(48);
        

        HBox addressTextAreaBox = new HBox(m_addressTextArea);
        HBox.setHgrow(addressTextAreaBox, Priority.ALWAYS);
        VBox.setVgrow(addressTextAreaBox, Priority.ALWAYS);
        addressTextAreaBox.setPadding(new Insets(0,10,0,10));
        addressTextAreaBox.setMouseTransparent(true);

        VBox stackPaneVBox = new VBox(headingTextBox);
        HBox.setHgrow(stackPaneVBox, Priority.ALWAYS);
        VBox.setVgrow(stackPaneVBox, Priority.ALWAYS);
        stackPaneVBox.setAlignment(Pos.CENTER_LEFT);
        stackPaneVBox.setPadding(new Insets(0,0,0,20));
        stackPaneVBox.setMouseTransparent(true);

        HBox imgViewBox = new HBox(m_backgroundImgView);
        HBox.setHgrow(imgViewBox, Priority.ALWAYS);
        VBox.setVgrow(imgViewBox, Priority.ALWAYS);
        imgViewBox.setAlignment(Pos.CENTER);
        
        m_addressTextArea.textProperty().addListener((obs,oldval,newval)->{
            if(newval.length() > 0){
                if(!stackPaneVBox.getChildren().contains(addressTextAreaBox)){
                    stackPaneVBox.getChildren().add(addressTextAreaBox);
                    headingTextBox.setAlignment(Pos.CENTER_LEFT);
                    imgViewBox.setAlignment(Pos.CENTER_LEFT);
                }

            }else{
                if(stackPaneVBox.getChildren().contains(addressTextAreaBox)){
                    stackPaneVBox.getChildren().remove(addressTextAreaBox);
                    headingTextBox.setAlignment(Pos.CENTER);
                    imgViewBox.setAlignment(Pos.CENTER);
                }
              
            }
        });


        m_stackPane = new StackPane(imgViewBox,stackPaneVBox);
        m_stackPane.setAlignment(Pos.CENTER_LEFT);
        m_stackPane.setId("hand");
        HBox.setHgrow(m_stackPane, Priority.ALWAYS);
        
        getChildren().add(m_stackPane);



        setAlignment(Pos.CENTER_LEFT);

        String textFieldId = m_id +"TextField";

     

        TextArea addressField = new TextArea(addressInformation.getAddressString());
        //  addressField.setMaxHeight(20);
        HBox.setHgrow(addressField, Priority.ALWAYS);
        VBox.setVgrow(addressField, Priority.ALWAYS);
        addressField.setWrapText(true);
        addressField.setUserData(textFieldId);
        addressField.setPromptText("            Enter Address");
        addressField.setId("textAreaInputEmpty");
        addressField.textProperty().addListener((obs,oldval,newval)->{
            String b58 = newval.replaceAll("[^A-HJ-NP-Za-km-z1-9]", "");
           
            if(newval.length() == 0){
                addressField.setId("textAreaInputEmpty");
            }else{
                addressField.setId("textAreaInput");
            }
            addressField.setText(b58);
        });

        m_addressTextArea.textProperty().bind(addressField.textProperty());

        Button enterButton = new Button("[ ENTER ]");
        enterButton.setFont(App.txtFont);
        enterButton.setId("toolBtn");
        enterButton.setMinWidth(90);
        enterButton.setPrefHeight(60);

        HBox addressFieldBox = new HBox(addressField, enterButton);
        HBox.setHgrow(addressFieldBox, Priority.ALWAYS);
        addressFieldBox.setAlignment(Pos.CENTER_LEFT);
       // addressFieldBox.setId("bodyBox");

       
        SimpleBooleanProperty isFieldFocused = new SimpleBooleanProperty(false);
        
        scene.focusOwnerProperty().addListener((obs, old, newPropertyValue) -> {
            if (newPropertyValue != null && newPropertyValue instanceof TextArea) {
                TextArea focusedField = (TextArea) newPropertyValue;
                Object userData = focusedField.getUserData();
                if(userData != null && userData instanceof String){
                    String userDataString = (String) userData;
                    if(userDataString.equals(textFieldId)){
                        isFieldFocused.set(true);
                    }else{
                        if(isFieldFocused.get()){
                            isFieldFocused.set(false);
                            enterButton.fire();
                        }
                    }
                }else{
                    if(isFieldFocused.get()){
                        isFieldFocused.set(false);
                        enterButton.fire();
                    }
                }
            }else{
                if(isFieldFocused.get()){
                    isFieldFocused.set(false);
                    enterButton.fire();
                }
            }
        });

        m_stackPane.addEventFilter(MouseEvent.MOUSE_CLICKED, actionEvent -> {
            if(getChildren().contains(m_stackPane))
            {
                getChildren().remove(m_stackPane);
            } 
          
            if(!(getChildren().contains(addressFieldBox))){
                getChildren().add( addressFieldBox);
            }
     
            Platform.runLater(()-> {
                addressField.requestFocus();

            });
        
        });

 
        

       

        Runnable setNotFocused = () ->{
            if (getChildren().contains(enterButton)) {
                getChildren().remove(enterButton);
            }

            if (getChildren().contains(addressFieldBox)) {
                getChildren().remove(addressFieldBox);

            }
   

            if (!(getChildren().contains(m_stackPane))) {
                getChildren().add(m_stackPane);
            }

            
        };
        enterButton.setOnAction(e->{
            
            String text = addressField.getText();
            AddressInformation newAddressInformation = new AddressInformation(text);
            m_addressInformation.set(newAddressInformation);
            setNotFocused.run();
        });

        addressField.setOnKeyPressed(e->{
            if(e.getCode() == KeyCode.ENTER){
                enterButton.fire();
            }
        });
 

        m_addressInformation.addListener((obs,oldval, newval)->{

            update();
     
            /*if(!newval.toString().equals(addressField.getText())){
                addressField.setText(newval.toString());
            }*/
        });
      
        update();


       
    }

    public SimpleObjectProperty<AddressInformation> addressInformationProperty(){
        return m_addressInformation;
    }

    @Override
    public String toString(){
        return m_addressInformation.get().toString();
    }

    public ArrayList<String> getInvalidAddressList(){
        return m_invalidAddressList;
    }

    public SimpleObjectProperty<LocalDateTime> getLastUpdatedObject() {
        return m_LastUpdated;
    }

    public void setLastUpdatedNow() {

        //   DateTimeFormatter formater = DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm:ss.SSSSS a");
        LocalDateTime now = LocalDateTime.now();

        m_LastUpdated.set(now);

    }

    public Image getUnknownUnitImage() {
        return new Image("/assets/unknown-unit.png");
    }

    public Image getUnitImage() {
        return new Image("/assets/unitErgo.png");
    }

    public NetworkType getNetworkType() {
        return m_networkType;
    }

    public boolean isInvalidAddress(String address){
        if(address != null){
            if(m_invalidAddressList.contains(address)){
                return true;
            }else{
                return false;
            }
        }
        return true;
    }

    private SimpleBooleanProperty m_addressValid = new SimpleBooleanProperty(false);

    public SimpleBooleanProperty isAddressValid(){
        return m_addressValid;
    }

    public void update() {


        AddressInformation adrInfo = m_addressInformation.get() != null ? m_addressInformation.get() : new AddressInformation("");
        Address adr = adrInfo.getAddress();

        NetworkType adrNetworkType = adrInfo.getNetworkType();
        String adrType = adrInfo.getAddressType();
        String adrString = adrInfo.getAddressString();

        boolean isInvalidAddress = adr != null ? isInvalidAddress(adrString) : true;
    
        String promptString =(adr != null && adrNetworkType != null && adrNetworkType == m_networkType && adrType != null) ? adrType + " (" + adrNetworkType.toString() + ")" : EMPTY_TEXT;
       
        String textNetType = adrNetworkType == null ? " [Address Invalid]" : (adrNetworkType != m_networkType ? " [Invalid network type: "+ adrNetworkType.toString() + "]" : "");
        String textAdrType = adrType == null || adr == null ? " [Address Invalid]" : (isInvalidAddress ? " [Invalid Recipient]" : "");

         

        String errortext = adrString.length() > 0 ? ((textAdrType.equals("") ? textNetType : textAdrType)) : "" ;

       
        m_backgroundImgView.setImage(adr != null ? getUnitImage(): getUnknownUnitImage());
        
        
        if(errortext.length() > 0){
            m_addressValid.set(false);
            m_headingText.setText(errortext);
        }else{
            m_addressValid.set(true);
            m_headingText.setText(promptString);
        }

        setLastUpdatedNow();
    }



}
