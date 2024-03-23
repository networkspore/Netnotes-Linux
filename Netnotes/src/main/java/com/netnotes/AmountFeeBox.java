package com.netnotes;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;

import org.ergoplatform.appkit.NetworkType;

import com.devskiller.friendly_id.FriendlyId;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

public class AmountFeeBox extends HBox {
    public final static PriceAmount MINIMUM_FEE = new PriceAmount(0.0011, new ErgoCurrency(NetworkType.MAINNET)); 

    private SimpleObjectProperty<PriceAmount> m_feeAmount = new SimpleObjectProperty<>(null);
    private SimpleObjectProperty<Image> m_feeAmountImage = new SimpleObjectProperty<>();
    private SimpleObjectProperty<PriceQuote> m_priceQuote = new SimpleObjectProperty<>();
    

    public AmountFeeBox(PriceCurrency currency, Scene scene){
        super();
        String textFieldId = FriendlyId.createFriendlyId() +"TextField";

        Tooltip feeTooltip = new Tooltip("Sending fee");
        feeTooltip.setShowDelay(new Duration(100));

        Button feeBtn = new Button();
        feeBtn.setId("rowBox");
        feeBtn.setContentDisplay(ContentDisplay.LEFT);
        feeBtn.setTooltip(feeTooltip);
        //feeBtn.setFocusTraversable(true);
        //feeBtn.setAlignment(Pos.CENTER_RIGHT);
        feeBtn.setPadding(new Insets(0, 0, 0, 0));
    
        setAlignment(Pos.CENTER_RIGHT);

        getChildren().add(feeBtn);

        TextField amountField = new TextField();
        amountField.setId("amountField");
        amountField.setAlignment(Pos.CENTER_LEFT);
        amountField.setPadding(new Insets(3, 0, 3, 0));
        amountField.setUserData(textFieldId);
        amountField.textProperty().addListener((obs, oldval, newval)->{
            PriceCurrency feeCurrency = m_feeAmount.get().getCurrency();
            String number = newval.replaceAll("[^0-9.]", "");
            int index = number.indexOf(".");
            String leftSide = index != -1 ? number.substring(0, index + 1) : number;
            String rightSide = index != -1 ?  number.substring(index + 1) : "";
            rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
            rightSide = rightSide.length() > feeCurrency.getDecimals() ? rightSide.substring(0, feeCurrency.getDecimals()) : rightSide;
        
            amountField.setText(leftSide +  rightSide);
        });
       
        HBox.setHgrow(amountField,Priority.ALWAYS);
        
        m_feeAmount.addListener((obs,oldVal, newVal)->{
            if(newVal != null){
                String newValString = newVal.getAmountString();
                String amountFieldText = amountField.getText();
                if(!(newValString.equals(amountFieldText))){
                    amountField.setText(newValString);
                }
            }
            updateAmountImage();
        });

        Runnable setNotFocused = () ->{
            if (getChildren().contains(amountField)) {
                getChildren().remove( amountField);
            }

            if (!(getChildren().contains(feeBtn))) {
               getChildren().add(feeBtn);
            }
        };

        Runnable enterAmount = ()->{
         
            String text = amountField.getText();
            
            BigDecimal amount = new BigDecimal(text.equals("") ? "0" : text);

            if(amount.compareTo(MINIMUM_FEE.getBigDecimalAmount()) == -1){
             
                PriceAmount newAmount = new PriceAmount(MINIMUM_FEE.getBigDecimalAmount(), currency);
                m_feeAmount.set(newAmount);
            
            }else{
                PriceAmount newAmount = new PriceAmount(amount, currency);
                m_feeAmount.set(newAmount);
                
            }
            setNotFocused.run();
        };

         amountField.setOnAction(e->{
            enterAmount.run();
         });
    

      // SimpleBooleanProperty isFieldFocused = new SimpleBooleanProperty(false);

        amountField.focusedProperty().addListener((obs,oldval,newval)->{
            if(!newval){
               enterAmount.run();
            }
        });
        /*
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
                           enterAmount.run();
                        }
                    }
                }else{
                    if(isFieldFocused.get()){
                        isFieldFocused.set(false);
                        enterAmount.run();
                    }
                }
            }else{
                if(isFieldFocused.get()){
                    isFieldFocused.set(false);
                    enterAmount.run();
                }
            }
        });*/

        feeBtn.setOnAction(e->{
            
            getChildren().remove(feeBtn);
            getChildren().add(amountField);
            Platform.runLater(()->amountField.requestFocus());
        });
        
   
        m_feeAmountImage.addListener((obs,oldval,newval)->{
            if(newval != null){
                feeBtn.setGraphic(IconButton.getIconView(newval,newval.getWidth()));
                setMinWidth(newval.getWidth());
            }else{
                feeBtn.setGraphic(null);
            }
        });

        updateAmountImage();
    }

    public SimpleObjectProperty<PriceAmount> feeAmountProperty(){
        return m_feeAmount;
    }
    public SimpleObjectProperty<PriceQuote> priceQuoteProperty(){
        return m_priceQuote;
    }

    public void updateAmountImage(){

        final int padding = 5;

        PriceAmount feeAmount = m_feeAmount.get();
        if(feeAmount == null){
            m_feeAmountImage.set(null);
            return;
        }
        //PriceQuote priceQuote = priceQuoteProperty().get();

        //boolean priceValid = priceQuote != null && priceQuote.getTimeStamp() != 0 && priceQuote.howOldMillis() < AddressesData.QUOTE_TIMEOUT;
        //BigDecimal priceQuoteBigDecimal = priceValid  && priceQuote != null ? priceQuote.getBigDecimalAmount() : BigDecimal.valueOf(0);

        java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 12);

       // BufferedImage downImage = SwingFXUtils.fromFXImage( new Image("/assets/caret-down-15.png"), null);
      //  Drawing.setImageAlpha(downImage, 0x40);

        BufferedImage feeImage = SwingFXUtils.fromFXImage( new Image("/assets/pricetag-40.png"), null);
        Drawing.setImageAlpha(feeImage, 0x40);

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        g2d.setFont(font);
    
        FontMetrics fm = g2d.getFontMetrics();
        int fontHeight = fm.getHeight();
        int fontAscent = fm.getAscent();

        final String feeAmountString = feeAmount.getAmountString();
      //  final String feeString = "Fee";
        int feeAmountStringWidth = fm.stringWidth(feeAmountString) + (padding*2);
      //  int feeStringWidth = fm.stringWidth(feeString);

        int width = feeAmountStringWidth < 50 ? 50 : feeAmountStringWidth;
        int height = 45;
        g2d.dispose();

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

        Drawing.fillArea(img, 0xff000000, 0, 0, width,height);
        Drawing.drawBar(1, 0x30ffffff, 0x60666666,img, 0, 0, width, height/2);
  

      //  g2d.drawImage(downImage, (width/2) - (downImage.getWidth() / 2) , height - downImage.getHeight() - 3, downImage.getWidth(), downImage.getHeight(),  null);

        g2d.drawImage(feeImage, width - feeImage.getWidth() - 3 , 3 , null);

      
        g2d.setFont(font);
         g2d.setColor(new java.awt.Color(0xcdd4da, false));

        g2d.drawString(feeAmountString, padding, ((height - fontHeight) / 2) + fontAscent);
       
       // g2d.drawString(feeString, (width /2) - (feeStringWidth/2), 13);
        g2d.dispose();
        m_feeAmountImage.set(SwingFXUtils.toFXImage(img, null));
    }
}
