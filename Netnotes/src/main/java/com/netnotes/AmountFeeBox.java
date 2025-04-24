package com.netnotes;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;

import io.netnotes.engine.Drawing;
import io.netnotes.engine.PriceCurrency;
import io.netnotes.engine.PriceQuote;
import io.netnotes.engine.Utils;
import io.netnotes.friendly_id.FriendlyId;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

public class AmountFeeBox extends HBox {
    private java.awt.Font m_font = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 12);
    private Graphics2D m_g2d = null;
    private FontMetrics m_fm = null;
    private BufferedImage m_img = null;
    private WritableImage m_wImg = null;
    private BufferedImage m_feeImage = null;

    public final static BigDecimal MIN_FEE =  BigDecimal.valueOf(0.0011);


    private SimpleObjectProperty<BigDecimal> m_feeAmount = new SimpleObjectProperty<>(null);
    private SimpleObjectProperty<PriceQuote> m_priceQuote = new SimpleObjectProperty<>();
    private SimpleObjectProperty<BigDecimal> m_minFee = new SimpleObjectProperty<>(MIN_FEE);

    private ChangeListener<BigDecimal> m_feeAmountListener = null;
    private PriceCurrency m_currency;

    public AmountFeeBox(PriceCurrency c, Scene scene){
        super();
        m_currency = c;
        m_feeImage = SwingFXUtils.fromFXImage( new Image("/assets/pricetag-40.png"), null);

        String textFieldId = FriendlyId.createFriendlyId() +"TextField";

        Tooltip feeTooltip = new Tooltip("Sending fee");
        feeTooltip.setShowDelay(new Duration(100));

        ImageView feeImgView = new ImageView();
        feeImgView.setId("rowBox");
        
        Tooltip.install(feeImgView, feeTooltip);
        //feeBtn.setFocusTraversable(true);
        //feeBtn.setAlignment(Pos.CENTER_RIGHT);
      
    
        setAlignment(Pos.CENTER_RIGHT);

        getChildren().add(feeImgView);

        TextField amountField = new TextField();
        amountField.setId("amountField");
        amountField.setAlignment(Pos.CENTER_LEFT);
        amountField.setPadding(new Insets(3, 0, 3, 0));
        amountField.setUserData(textFieldId);
        amountField.setOnKeyPressed(e->{
            if (Utils.keyCombCtrZ.match(e) ) { 
                e.consume();
            }
        });
        amountField.textProperty().addListener((obs, oldval, newval)->{
           
            String number = newval.replaceAll("[^0-9.]", "");
            int index = number.indexOf(".");
            String leftSide = index != -1 ? number.substring(0, index + 1) : number;
            String rightSide = index != -1 ?  number.substring(index + 1) : "";
            rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
            rightSide = rightSide.length() >  m_currency.getDecimals() ? rightSide.substring(0, m_currency.getDecimals()) : rightSide;
        
            amountField.setText(leftSide +  rightSide);
        });
       
        HBox.setHgrow(amountField,Priority.ALWAYS);


        Runnable setNotFocused = () ->{
            if (getChildren().contains(amountField)) {
                getChildren().remove( amountField);
            }

            if (!(getChildren().contains(feeImgView))) {
               getChildren().add(feeImgView);
            }
        };

        Runnable enterAmount = ()->{
         
            String text = amountField.getText();

            BigDecimal amount = Utils.isTextZero(text) ? BigDecimal.ZERO : new BigDecimal(text);
            
   

            if(amount.compareTo(m_minFee.get()) == -1){
             
                
                m_feeAmount.set(m_minFee.get());
            
            }else{
               
                m_feeAmount.set(amount);
                
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

        feeImgView.addEventFilter(MouseEvent.MOUSE_CLICKED , e->{
            
            getChildren().remove(feeImgView);
            getChildren().add(amountField);
            Platform.runLater(()->amountField.requestFocus());
        });
        
        
        m_feeAmountListener = (obs,oldVal, newVal)->{
            if(newVal != null){
                String newValString = newVal + "";
                String amountFieldText = amountField.getText();
                if(!(newValString.equals(amountFieldText))){
                    amountField.setText(newValString);
                }
            }
            updateAmountImage(newVal, feeImgView);
        };
        
        m_feeAmount.addListener(m_feeAmountListener);

      
        updateAmountImage(m_feeAmount.get(), feeImgView);
    }

    public SimpleObjectProperty<BigDecimal> minFeeProperty(){
        return m_minFee;
    }

    public SimpleObjectProperty<BigDecimal> feeAmountProperty(){
        return m_feeAmount;
    }
    public SimpleObjectProperty<PriceQuote> priceQuoteProperty(){
        return m_priceQuote;
    }

    public PriceCurrency currencyProperty(){
        return m_currency;
    }
    

    public void updateAmountImage(BigDecimal feeAmount, ImageView imgView){

        final int padding = 5;

       
        //PriceQuote priceQuote = priceQuoteProperty().get();

        //boolean priceValid = priceQuote != null && priceQuote.getTimeStamp() != 0 && priceQuote.howOldMillis() < AddressesData.QUOTE_TIMEOUT;
        //BigDecimal priceQuoteBigDecimal = priceValid  && priceQuote != null ? priceQuote.getBigDecimalAmount() : BigDecimal.valueOf(0);

        

       // BufferedImage downImage = SwingFXUtils.fromFXImage( new Image("/assets/caret-down-15.png"), null);
      //  Drawing.setImageAlpha(downImage, 0x40);

        
        Drawing.setImageAlpha(m_feeImage, 0x40);

        m_img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        m_g2d = m_img.createGraphics();

        m_g2d.setFont(m_font);
    
        m_fm = m_g2d.getFontMetrics();
        int fontHeight = m_fm.getHeight();
        int fontAscent = m_fm.getAscent();

        final String feeAmountString = feeAmount == null ? "0" : feeAmount + "";
      //  final String feeString = "Fee";
        int feeAmountStringWidth = m_fm.stringWidth(feeAmountString) + (padding*2);
      //  int feeStringWidth = fm.stringWidth(feeString);

        int width = feeAmountStringWidth < 50 ? 50 : feeAmountStringWidth;
        int height = 45;
        m_g2d.dispose();

        m_img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        m_g2d = m_img.createGraphics();
        m_g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        m_g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        m_g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        m_g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        m_g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        m_g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        m_g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        m_g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Drawing.fillArea(m_img, 0xff000000, 0, 0, width,height);
        Drawing.drawBar(1, 0x30ffffff, 0x60666666,m_img, 0, 0, width, height/2);
  

      //  g2d.drawImage(downImage, (width/2) - (downImage.getWidth() / 2) , height - downImage.getHeight() - 3, downImage.getWidth(), downImage.getHeight(),  null);

        m_g2d.drawImage(m_feeImage, width - m_feeImage.getWidth() - 3 , 3 , null);

      
        m_g2d.setFont(m_font);
        m_g2d.setColor(new java.awt.Color(0xcdd4da, false));

        m_g2d.drawString(feeAmountString, padding, ((height - fontHeight) / 2) + fontAscent);
       
       // g2d.drawString(feeString, (width /2) - (feeStringWidth/2), 13);
        m_g2d.dispose();
        m_wImg = SwingFXUtils.toFXImage(m_img, m_wImg);
        imgView.setImage(m_wImg);

        m_g2d = null;
        m_img = null;
        
    }

    public void shutdown(){
        m_feeImage = null;

    }
}
