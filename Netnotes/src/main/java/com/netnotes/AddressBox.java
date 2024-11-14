package com.netnotes;

import java.time.LocalDateTime;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;

import com.devskiller.friendly_id.FriendlyId;


import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import javafx.embed.swing.SwingFXUtils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;


public class AddressBox extends HBox {


    private NetworkType m_networkType = NetworkType.MAINNET;
    // private boolean m_valid = false;

    private String m_id = FriendlyId.createFriendlyId();
    private int m_minHeight = 40;

    private final SimpleObjectProperty<LocalDateTime> m_LastUpdated = new SimpleObjectProperty<LocalDateTime>(LocalDateTime.now());

    private SimpleObjectProperty<AddressInformation> m_addressInformation = new SimpleObjectProperty<>(null);

//String explorerId, NetworksData networksData
    public AddressBox(AddressInformation addressInformation, Scene scene, NetworkType networkType) {
        super();
        setFocusTraversable(true);
        setMinHeight(m_minHeight);
        m_networkType = networkType;
        m_addressInformation.set(addressInformation);
        
  

        Button addressBtn = new Button();
        addressBtn.setId("transparentColor");
        addressBtn.setContentDisplay(ContentDisplay.LEFT);
        addressBtn.setAlignment(Pos.CENTER_LEFT);
        addressBtn.setPadding(new Insets(0));
        addressBtn.setMouseTransparent(true);
        
        getChildren().add(addressBtn);

        addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if(getChildren().contains(addressBtn)){
                addressBtn.fire();
            }
        });
        //addressBtn.setGraphicTextGap(25);

      
       
          

        setAlignment(Pos.CENTER_LEFT);

        String textFieldId = m_id +"TextField";

     

        TextField addressField = new TextField(addressInformation.getAddressString());
        //  addressField.setMaxHeight(20);
        HBox.setHgrow(addressField, Priority.ALWAYS);
        addressField.setAlignment(Pos.CENTER_LEFT);
        addressField.setPadding(new Insets(1, 10, 1, 10));
        addressField.setUserData(textFieldId);
        
        addressField.setPromptText("Enter Address");
        addressField.setId("formField");
        addressField.textProperty().addListener((obs,oldval,newval)->{
            String b58 = newval.replaceAll("[^A-HJ-NP-Za-km-z1-9]", "");
           
            if(newval.length() == 0){
                addressField.setId("formField");
            }else{
                addressField.setId(null);
            }
            addressField.setText(b58);
        });

      

        Button enterButton = new Button("[ ENTER ]");
        enterButton.setFont(App.txtFont);
        enterButton.setId("toolBtn");
   

       
        SimpleBooleanProperty isFieldFocused = new SimpleBooleanProperty(false);
        
        scene.focusOwnerProperty().addListener((obs, old, newPropertyValue) -> {
            if (newPropertyValue != null && newPropertyValue instanceof TextField) {
                TextField focusedField = (TextField) newPropertyValue;
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

        addressBtn.setOnAction(actionEvent -> {
            if(getChildren().contains(addressBtn))
            {
                getChildren().remove(addressBtn);
            } 
          
            if(!(getChildren().contains(addressField))){
                getChildren().addAll( addressField, enterButton);
            }
     
                Platform.runLater(()-> {
                    addressField.requestFocus();

                });
            
        });

 
        

       

        Runnable setNotFocused = () ->{
            if (getChildren().contains(enterButton)) {
                getChildren().remove(enterButton);
            }

            if (getChildren().contains(addressField)) {
                getChildren().remove(addressField);

            }
   

            if (!(getChildren().contains(addressBtn))) {
                getChildren().add(addressBtn);
            }

            
        };
        enterButton.setOnAction(e->{
            
            String text = addressField.getText();
            AddressInformation newAddressInformation = new AddressInformation(text);
            m_addressInformation.set(newAddressInformation);
            setNotFocused.run();
        });

         addressField.setOnAction(e->{
            enterButton.fire();
        });

        ImageView addressImgView = new ImageView(); //addressBtn.setGraphic(IconButton.getIconView(newval, newval.getWidth()));
        addressBtn.setGraphic(addressImgView);
       
        m_addressInformation.addListener((obs,oldval, newval)->{

            updateBufferedImage(addressImgView);
     
            /*if(!newval.toString().equals(addressField.getText())){
                addressField.setText(newval.toString());
            }*/
        });
      
        updateBufferedImage(addressImgView);
       
        setId("hand");
    }

    public SimpleObjectProperty<AddressInformation> addressInformationProperty(){
        return m_addressInformation;
    }

    @Override
    public String toString(){
        return m_addressInformation.get().toString();
    }

    /*
        TextField toTextField = new TextField();

        toTextField.setMaxHeight(40);
        toTextField.setId("formField");
        toTextField.setPadding(new Insets(3, 10, 0, 0));
        HBox.setHgrow(toTextField, Priority.ALWAYS);

        toAddressBtn.setOnAction(e -> {

            toAddressBox.getChildren().remove(toAddressBtn);
            toAddressBox.getChildren().add(toTextField);

            Platform.runLater(() -> toTextField.requestFocus());

        });

        Region toMinHeightRegion = new Region();
        toMinHeightRegion.setMinHeight(40);


        toAddressBox.getChildren().addAll(toMinHeightRegion, toText, toAddressBtn);

        toTextField.textProperty().addListener((obs, old, newVal) -> {
            String text = newVal.trim();
            if (text.length() > 5) {
                if (!toAddressBox.getChildren().contains(toEnterButton)) {
                    toAddressBox.getChildren().add(toEnterButton);
                }

                toAddressBtn.setAddressByString(text, onVerified -> {

                    Object object = onVerified.getSource().getValue();

                    if (object != null && (Boolean) object) {

                        toAddressBox.getChildren().remove(toTextField);
                        if (toAddressBox.getChildren().contains(toEnterButton)) {
                            toAddressBox.getChildren().remove(toEnterButton);
                        }
                        toAddressBox.getChildren().add(toAddressBtn);
                    }
                });
            }

        });

        toTextField.setOnKeyPressed((keyEvent) -> {
            KeyCode keyCode = keyEvent.getCode();
            if (keyCode == KeyCode.ENTER) {
              //  String text = toTextField.getText();

                toAddressBox.getChildren().remove(toTextField);

                if (toAddressBox.getChildren().contains(toEnterButton)) {
                    toAddressBox.getChildren().remove(toEnterButton);
                }
                toAddressBox.getChildren().add(toAddressBtn);
            }
        });

        toTextField.focusedProperty().addListener((obs, old, newPropertyValue) -> {

            if (newPropertyValue) {

            } else {

                toAddressBox.getChildren().remove(toTextField);
                if (toAddressBox.getChildren().contains(toEnterButton)) {
                    toAddressBox.getChildren().remove(toEnterButton);
                }
                toAddressBox.getChildren().add(toAddressBtn);

            }
        });*/






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

    private BufferedImage m_img = null;
    private WritableImage m_wImg = null;
    private Graphics2D m_imgG2d = null;

    private BufferedImage m_txtImg = null;
    private Graphics2D m_txtG2d = null;
    private FontMetrics m_txtFm = null;

    private int m_imgHeight = 40;

    private java.awt.Font m_font = new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 14);
    private BufferedImage m_unitImage = null;

    public void updateBufferedImage(ImageView imgView) {

        final int padding = 10;
        final int bottomIndent = 15;

        AddressInformation adrInfo = m_addressInformation.get() != null ? m_addressInformation.get() : new AddressInformation("");
        Address adr = adrInfo.getAddress();
        NetworkType adrNetworkType = adrInfo.getNetworkType();
        String adrType = adrInfo.getAddressType();
        String adrString = adrInfo.getAddressString();
    
        final String promptString =(adr != null && adrNetworkType != null && adrNetworkType == m_networkType && adrType != null) ? adrType + " (" + adrNetworkType.toString() + ")" : "Enter address";
       
        String textNetType = adrNetworkType == null ? " [Address Invalid]" : (adrNetworkType != m_networkType ? " [Invalid network type: "+ adrNetworkType.toString() + "]" : "");
        String textAdrType = adrType == null || adr == null ? " [Address Invalid]" : "";
        final String errortext = adrString.length() > 0 ? ((textAdrType.equals("") ? textNetType : textAdrType)) : "" ;

       
        m_txtImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        m_txtG2d = m_txtImg.createGraphics();
        m_txtG2d.setFont(m_font);
        m_txtFm = m_txtG2d.getFontMetrics();    
        
        int errorSize = errortext.length() > 0 ? m_txtFm.stringWidth(errortext) : 0;
        int promptSize = m_txtFm.stringWidth(promptString);
        int topTextSize = promptSize + 10 + errorSize;
        int adrSize = 25 + adrString.length() > 0 ? m_txtFm.stringWidth(adrString) : 0;

        int stringSize = topTextSize >  bottomIndent + adrSize ? padding + topTextSize : padding + bottomIndent + adrSize;
        int width = stringSize + 40;
       
        m_unitImage = adr != null ? SwingFXUtils.fromFXImage(getUnitImage(), null) : SwingFXUtils.fromFXImage(getUnknownUnitImage(), null);
        
        if(adrInfo.getNetworkType() != null && adrInfo.getNetworkType() == NetworkType.TESTNET){
            InvertEffect.invertRGB(m_unitImage, 1);
        }
        
        Drawing.setImageAlpha(m_unitImage, 0x20);

        m_img = new BufferedImage(width, m_imgHeight, BufferedImage.TYPE_INT_ARGB);
        m_imgG2d = m_img.createGraphics();
        m_imgG2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        m_imgG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        m_imgG2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        m_imgG2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        m_imgG2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        m_imgG2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        m_imgG2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        m_imgG2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        m_imgG2d.drawImage(m_unitImage, 75, (m_imgHeight / 2) - (m_unitImage.getHeight() / 2), m_unitImage.getWidth(), m_unitImage.getHeight(), null);

        m_imgG2d.setFont(m_font);
    
        
        if(errortext.length() > 0){
            m_imgG2d.setColor(java.awt.Color.WHITE);
            m_imgG2d.drawString(errortext, padding, m_txtFm.getHeight() + 2);
        }else{
            m_imgG2d.setColor(new java.awt.Color(.8f, .8f, .8f, .7f));
            m_imgG2d.drawString(promptString, padding, adrString.length() > 0 ? m_txtFm.getHeight() + 2 : ((m_imgHeight - m_txtFm.getHeight()) / 2) + m_txtFm.getAscent());
        }
        if(adrString.length() > 0){ 
            m_imgG2d.setColor(new java.awt.Color(.8f, .8f, .8f, .8f));
            m_imgG2d.drawString(adrString, padding + bottomIndent , m_imgHeight - 8);
        }


        /*
       try {
            ImageIO.write(m_img, "png", new File("outputImage.png"));
        } catch (IOException e) {

        } */


        imgView.setImage(SwingFXUtils.toFXImage(m_img, m_wImg));
        
        m_txtG2d.dispose();
        m_txtG2d = null;
        m_txtImg = null;
        m_txtFm = null;
     
        m_imgG2d.dispose();
        m_imgG2d = null;
        m_img = null;

        m_unitImage = null;
    }



}
