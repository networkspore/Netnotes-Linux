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

    private final SimpleObjectProperty<Image> m_imgBuffer = new SimpleObjectProperty<Image>(null);
    private final SimpleObjectProperty<LocalDateTime> m_LastUpdated = new SimpleObjectProperty<LocalDateTime>(LocalDateTime.now());
;
    private SimpleObjectProperty<AddressInformation> m_addressInformation = new SimpleObjectProperty<>(null);

//String explorerId, NetworksData networksData
    public AddressBox(AddressInformation addressInformation, Scene scene, NetworkType networkType) {
        super();
        setFocusTraversable(true);
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

        m_imgBuffer.addListener((obs,oldval,newval)-> {
            if(newval != null){    
                addressBtn.setGraphic(IconButton.getIconView(newval, newval.getWidth()));
            }
        });

        setAlignment(Pos.CENTER_LEFT);

        String textFieldId = m_id +"TextField";

     

        TextField addressField = new TextField(addressInformation.getAddressString());
        //  addressField.setMaxHeight(20);
        addressField.setId("amountField");
        addressField.setAlignment(Pos.CENTER_LEFT);
        addressField.setPadding(new Insets(3, 10, 3, 10));
        addressField.setUserData(textFieldId);
        /*addressField.textProperty().addListener((obs, oldval, newval)->{
           
            String hex = newval.replaceAll("[^0-9a-fA-F]", "");
            
        
            addressField.setText(hex);
        });*/
      

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

        HBox.setHgrow(addressField, Priority.ALWAYS);

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

        m_addressInformation.addListener((obs,oldval, newval)->{

            updateBufferedImage();
     
            /*if(!newval.toString().equals(addressField.getText())){
                addressField.setText(newval.toString());
            }*/
        });
      
        updateBufferedImage();
       

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

    public void updateBufferedImage() {

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


        int height = 40;
        

        //    java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 30);
        java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 14);

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();

        
        int errorSize = errortext.length() > 0 ? fm.stringWidth(errortext) : 0;
        int promptSize = fm.stringWidth(promptString);
        int topTextSize = promptSize + 10 + errorSize;
        int adrSize = 25 + adrString.length() > 0 ? fm.stringWidth(adrString) : 0;

        g2d.dispose();

        int stringSize = topTextSize >  bottomIndent + adrSize ? padding + topTextSize : padding + bottomIndent + adrSize;
        int width = stringSize + 40;
       

        // BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        BufferedImage unitImage = adr != null ? SwingFXUtils.fromFXImage(getUnitImage(), null) : SwingFXUtils.fromFXImage(getUnknownUnitImage(), null);
        
        if(adrInfo.getNetworkType() != null && adrInfo.getNetworkType() == NetworkType.TESTNET){
            InvertEffect.invertRGB(unitImage, 1);
        }
        
        Drawing.setImageAlpha(unitImage, 0x20);

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2d.drawImage(unitImage, 75, (height / 2) - (unitImage.getHeight() / 2), unitImage.getWidth(), unitImage.getHeight(), null);

        g2d.setFont(font);
        g2d.setColor(java.awt.Color.WHITE);
        
        // rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
        
        
        if(errortext.length() > 0){
        
            g2d.drawString(errortext, padding, fm.getHeight() + 2);
        }else{
            g2d.drawString(promptString, padding, adrString.length() > 0 ? fm.getHeight() + 2 : ((height - fm.getHeight()) / 2) + fm.getAscent());
        }
        if(adrString.length() > 0){ 
            g2d.setColor(new java.awt.Color(.8f, .8f, .8f, .8f));
            g2d.drawString(adrString, padding + bottomIndent , height - 8);
        }
        g2d.dispose();
        
        /*
       try {
            ImageIO.write(img, "png", new File("outputImage.png"));
        } catch (IOException e) {

        } */


        m_imgBuffer.set(SwingFXUtils.toFXImage(img, null));
    }



}
