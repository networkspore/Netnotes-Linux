package com.netnotes;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import com.utils.Utils;

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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AmountSendBox extends AmountBox {

  

    private SimpleObjectProperty<PriceAmount> m_balanceAmount;
    private SimpleObjectProperty<Image> m_maxAmountImage = new SimpleObjectProperty<>();
    private SimpleBooleanProperty m_isFee = new SimpleBooleanProperty();
    private AmountFeeBox m_feeBox = null;
    private SimpleObjectProperty<PriceAmount> m_maxAmount = new SimpleObjectProperty<>(null);

    public AmountSendBox(PriceAmount priceAmount, Scene scene, boolean editable) {
        super();
        m_balanceAmount =  new SimpleObjectProperty<>(new PriceAmount(0L, priceAmount.getCurrency()));
        setId("darkBox");
        setMinHeight(40);
        priceAmountProperty().set(priceAmount);
        setAlignment(Pos.CENTER_LEFT);

        m_feeBox = new AmountFeeBox(priceAmount.getCurrency(), scene);
        VBox.setVgrow(m_feeBox, Priority.ALWAYS);
    
        Button amountBtn = new Button();
        amountBtn.setId("amountBtn");
       // amountBtn.textProperty().bind(m_currentAmount.asString());
        amountBtn.setContentDisplay(ContentDisplay.LEFT);
        amountBtn.setAlignment(Pos.CENTER_LEFT);
        amountBtn.setPadding(new Insets(2, 5, 2, 10));
        amountBtn.setGraphicTextGap(25);
        imageBufferProperty().addListener((obs,oldval,newval)-> {
            if(newval != null){    
                amountBtn.setGraphic(IconButton.getIconView(newval, newval.getWidth()));
            }
        });
        
        HBox amountsBox = new HBox(amountBtn);
        amountsBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(amountsBox,Priority.ALWAYS);

 

        Button maxAmountBtn = new Button();
        maxAmountBtn.setId("rowBox");
        maxAmountBtn.setContentDisplay(ContentDisplay.RIGHT);
        maxAmountBtn.setAlignment(Pos.CENTER_RIGHT);
        maxAmountBtn.setPadding(new Insets(0, 0, 0, 0));

        Runnable updateImage = ()->{
        
            Image newImage = m_maxAmountImage.get();
            if(newImage != null){
                maxAmountBtn.setGraphic(IconButton.getIconView(newImage,newImage.getWidth()));
            }else{
                maxAmountBtn.setGraphic(null);
            }
        
        };
        updateImage.run();
        m_maxAmountImage.addListener((obs,oldval,newval)->updateImage.run());

        String textFieldId = getBoxId() +"TextField";

        int precision = priceAmount.getCurrency().getFractionalPrecision();
        DecimalFormat df = new DecimalFormat("0");
        df.setMaximumFractionDigits(precision);



     

        TextField amountField = new TextField(df.format(priceAmount.getDoubleAmount()));
        //  amountField.setMaxHeight(20);
        amountField.setId("amountField");
        amountField.setAlignment(Pos.CENTER_LEFT);
        amountField.setEditable(editable);
        amountField.setPadding(new Insets(3, 10, 3, 10));
        amountField.setUserData(textFieldId);
        amountField.textProperty().addListener((obs, oldval, newval)->{
           
            String number = newval.replaceAll("[^0-9.]", "");
            int index = number.indexOf(".");
            String leftSide = index != -1 ? number.substring(0, index + 1) : number;
            String rightSide = index != -1 ?  number.substring(index + 1) : "";
            rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
            rightSide = rightSide.length() > priceAmount.getCurrency().getDecimals() ? rightSide.substring(0, priceAmount.getCurrency().getDecimals()) : rightSide;
        
            amountField.setText(leftSide +  rightSide);
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

        HBox.setHgrow(amountField, Priority.ALWAYS);

        ImageView textViewImage = IconButton.getIconView( priceAmount.getCurrency().getIcon(),35);

        VBox imgPaddingBox = new VBox(textViewImage);
        imgPaddingBox.setPadding(new Insets(0,15,0,10)); 
        imgPaddingBox.setMinHeight(40);
        imgPaddingBox.setAlignment(Pos.CENTER_LEFT);

        amountBtn.setOnAction(actionEvent -> {
            amountsBox.getChildren().remove(amountBtn);
            if(editable){
                amountsBox.getChildren().add(0, imgPaddingBox);
                amountsBox.getChildren().add(1, amountField);
                amountsBox.getChildren().add(2, enterButton);
            }else{
                
                amountsBox.getChildren().add(0, imgPaddingBox);
                amountsBox.getChildren().add(1, amountField);
      
            }

            Platform.runLater(()-> {
                amountField.requestFocus();

            });
            
        });

       
       // HBox isFeeBox = new HBox();
     //   isFeeBox.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(amountsBox, maxAmountBtn);

        amountBtn.prefWidthProperty().bind(this.widthProperty());

        Runnable setNotFocused = () ->{
            if (amountsBox.getChildren().contains(enterButton)) {
                amountsBox.getChildren().remove(enterButton);
            }

            if (amountsBox.getChildren().contains(amountField)) {
                amountsBox.getChildren().remove(amountField);

            }

            if (amountsBox.getChildren().contains( imgPaddingBox)) {
                amountsBox.getChildren().remove( imgPaddingBox);
            }

            if (!(amountsBox.getChildren().contains(amountBtn))) {
                amountsBox.getChildren().add(amountBtn);
            }

         

            
        };
        enterButton.setOnAction(e->{
            if(editable){
                String text = amountField.getText();
                PriceAmount newAmount = new PriceAmount(Double.parseDouble(text.equals("") ? "0" : text), priceAmountProperty().get().getCurrency());
                priceAmountProperty().set(newAmount);
            }
            setNotFocused.run();
        });

        amountField.setOnAction(e->{
            enterButton.fire();
        });

        maxAmountBtn.setOnAction(e->{

            PriceAmount maxAmount = m_maxAmount.get();
            amountField.textProperty().set(maxAmount.getAmountString());    
            enterButton.fire();
            
        });

    
        Runnable updateMaxAmount = ()->{
            PriceAmount balanceAmount = m_balanceAmount.get();
            boolean isFeeToken = m_isFee.get();

            if(isFeeToken){
                long feeLong = m_feeBox.feeAmountProperty().get().getLongAmount();
                long balanceLong = balanceAmount.getLongAmount();
                long maxBalanceLong = balanceLong - feeLong;

                maxBalanceLong  = maxBalanceLong < 0 ? 0 : maxBalanceLong;

                PriceAmount maxAmount = new PriceAmount(maxBalanceLong, balanceAmount.getCurrency());

                m_maxAmount.set(maxAmount);
            }else{
                m_maxAmount.set(m_balanceAmount.get());
            }
        };

        m_feeBox.feeAmountProperty().addListener((obs, oldval, newval)->{
            updateMaxAmount.run();
            updateAmountImage();
            updateBufferedImage();
        });
        
        priceQuoteProperty().addListener((obs, oldval, newval)->updateBufferedImage());

        priceAmountProperty().addListener((obs,oldval, newval)-> updateBufferedImage());

        m_balanceAmount.addListener((obs, oldval, newVal)->{
            updateMaxAmount.run();
            updateAmountImage();
        });

       

        isFeeProperty().addListener((obs,oldval,newval)->{
            if(newval){
                PriceCurrency priceCurrency = priceAmountProperty().get().getCurrency();
                if(priceCurrency.getTokenId().equals(ErgoCurrency.TOKEN_ID)){
                    m_feeBox.feeAmountProperty().set(AmountFeeBox.MINIMUM_FEE);
                }
                if(!(getChildren().contains(m_feeBox))){
               
                   getChildren().add(1, m_feeBox);
                
                }
            }else{
                if(getChildren().contains(m_feeBox)){
               
                  getChildren().remove(m_feeBox);
                
                }
            }
           
        });

  
        updateBufferedImage();
        
    }

    public SimpleObjectProperty<PriceAmount> balanceAmountProperty(){
        return m_balanceAmount;
    }

    public boolean isNotAvailable(){
        PriceAmount maxAmount = m_maxAmount.get();
        PriceAmount priceAmount = priceAmountProperty().get();

        if(maxAmount != null && priceAmount != null){
            if( priceAmount.getLongAmount() > maxAmount.getLongAmount()){
                return true;
            }
        }
        return false;
    }

    public SimpleObjectProperty<PriceAmount> feeAmountProperty(){
        return m_feeBox.feeAmountProperty();
    }
   

    public void updateAmountImage() {
        final int padding = 5;
        

       
        PriceAmount maxAmount = m_maxAmount.get() != null ? m_maxAmount.get() : new PriceAmount(0, (priceAmountProperty().get() != null ? priceAmountProperty().get().getCurrency() : new PriceCurrency("", "unknonw", "unknown", 0, "","unknown", "/assets/unknown-unit.png", "unknown", "")));
        boolean quantityValid = maxAmount != null && maxAmount.getAmountValid();
        
    
        BigInteger integers = maxAmount != null ? maxAmount.getBigDecimalAmount().toBigInteger() : BigInteger.ZERO;
        BigDecimal decimals = maxAmount != null ? maxAmount.getBigDecimalAmount().subtract(new BigDecimal(integers)) : BigDecimal.ZERO;
        int decimalPlaces = maxAmount != null ? maxAmount.getCurrency().getFractionalPrecision() : 0;
        String currencyName = maxAmount != null ? maxAmount.getCurrency().getSymbol() : "UKNOWN";
        int space = currencyName.indexOf(" ");
        currencyName = space != -1 ? currencyName.substring(0, space) : currencyName;

     

        java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 30);
        java.awt.Font smallFont = new java.awt.Font("SANS-SERIF", java.awt.Font.PLAIN, 12);

        //   Image ergoBlack25 = new Image("/assets/ergo-black-25.png");
        //   SwingFXUtils.fromFXImage(ergoBlack25, null);
        
        String amountString = quantityValid ? String.format("%d", integers) : " -";
        String decs = String.format("%." + decimalPlaces + "f", decimals);
        String maxString = "MAX";

        decs = quantityValid ? decs.substring(1, decs.length()) : "";
   
    
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
 
        int stringWidth = fm.stringWidth(amountString);

      

        g2d.setFont(smallFont);

        fm = g2d.getFontMetrics();
        int maxWidth = fm.stringWidth(maxString);


        //  int priceAscent = fm.getAscent();
        int integersX = padding;
      
        int decimalsX = integersX + stringWidth + 1;

       // int currencyNameStringWidth = fm.stringWidth(currencyName);
        int decsWidth = decs.equals("") ? 0 : fm.stringWidth(decs);
        int currencyNameWidth = fm.stringWidth(currencyName);

        int width = decimalsX + stringWidth + (decsWidth < currencyNameWidth ? currencyNameWidth : decsWidth) + (padding * 2) + padding + maxWidth;
  

        int currencyNameStringX = decimalsX + 2;

        g2d.dispose();
        int height = 45;
        BufferedImage unitImage = SwingFXUtils.fromFXImage(maxAmount != null ? maxAmount.getCurrency().getIcon() : new Image("/assets/unknown-unit.png"), null);
        Drawing.setImageAlpha(unitImage, 0x40);
        //  adrBuchImg.getScaledInstance(width, height, java.awt.Image.SCALE_AREA_AVERAGING);
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
        //   g2d.setComposite(AlphaComposite.Clear);

        /* for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = new Color(adrBuchImg.getRGB(x, y), true);

                Color c2 = new Color(c.getRed(), c.getGreen(), c.getBlue(), 35);

                img.setRGB(x, y, c2.getRGB());
            }
        }
         #ffffff05, #66666680, #ffffff05*/
         Drawing.fillArea(img, 0xff000000, 0, 0, width,height);
        Drawing.drawBar(1, 0x30ffffff, 0x60666666,img, 0, 0, width, height/2);

  

        g2d.drawImage(unitImage, width - unitImage.getWidth() - (maxWidth /2) , (height / 2) - (unitImage.getHeight() / 2), unitImage.getWidth(), unitImage.getHeight(), null);

       



        g2d.setFont(font);
        fm = g2d.getFontMetrics();


        g2d.setColor(java.awt.Color.WHITE);

        g2d.drawString(amountString, integersX, fm.getAscent() + 5);

        g2d.setFont(smallFont);
        fm = g2d.getFontMetrics();
        g2d.setColor(new java.awt.Color(.9f, .9f, .9f, .9f));

       
        if(decimalPlaces > 0){
            //decimalsX = widthIncrease > 0 ? decimalsX + widthIncrease : decimalsX;
            g2d.drawString(decs, decimalsX , fm.getHeight() + 2);
        }

        
        g2d.drawString(currencyName, currencyNameStringX, height - 10);
        // ((height - fm.getHeight()) / 2) + fm.getAscent())
        g2d.setColor(Color.WHITE);
        g2d.drawString(maxString, width - ((padding*2) + maxWidth),  ((height - fm.getHeight()) / 2) + fm.getAscent());
       // g2d.setFont(smallFont);
   
     //   fm = g2d.getFontMetrics();


        
        /*try {
            Files.writeString(logFile.toPath(), amountString + decs);
        } catch (IOException e) {

        }*/
        g2d.dispose();

       /* try {
            ImageIO.write(img, "png", new File("outputImage.png"));
        } catch (IOException e) {

        }*/

        m_maxAmountImage.set(SwingFXUtils.toFXImage(img, null));
        
    }

    public SimpleBooleanProperty isFeeProperty(){
        return m_isFee;
    }
   
    @Override
    public void updateBufferedImage() {
        PriceAmount priceAmount = priceAmountProperty().get();
        boolean quantityValid = priceAmount != null && priceAmount.getAmountValid();
        BigDecimal priceAmountDecimal = priceAmount != null && quantityValid ? priceAmount.getBigDecimalAmount() : BigDecimal.valueOf(0);

        PriceQuote priceQuote = priceQuoteProperty().get();
        boolean priceValid = priceQuote != null && priceQuote.getTimeStamp() != 0 && priceQuote.howOldMillis() < getQuoteTimeout();
        BigDecimal priceQuoteBigDecimal = priceValid  && priceQuote != null ? priceQuote.getBigDecimalAmount() : BigDecimal.valueOf(0);
        
        String totalPrice = priceValid && priceQuote != null ? Utils.formatCryptoString( priceAmountDecimal.multiply(priceQuoteBigDecimal), priceQuote.getQuoteCurrency(), priceQuote.getFractionalPrecision(),  quantityValid && priceValid) : " -.--";
        BigInteger integers = priceAmount != null ? priceAmount.getBigDecimalAmount().toBigInteger() : BigInteger.ZERO;
        BigDecimal decimals = priceAmount != null ? priceAmount.getBigDecimalAmount().subtract(new BigDecimal(integers)) : BigDecimal.ZERO;
        int decimalPlaces = priceAmount != null ? priceAmount.getCurrency().getFractionalPrecision() : 0;
        String currencyName = priceAmount != null ? priceAmount.getCurrency().getSymbol() : "UKNOWN";
        int space = currencyName.indexOf(" ");
        currencyName = space != -1 ? currencyName.substring(0, space) : currencyName;

        String currencyPrice = priceValid && priceQuote != null ? priceQuote.toString() : "-.--";

        java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 30);
        java.awt.Font smallFont = new java.awt.Font("SANS-SERIF", java.awt.Font.PLAIN, 12);

        //   Image ergoBlack25 = new Image("/assets/ergo-black-25.png");
        //   SwingFXUtils.fromFXImage(ergoBlack25, null);
        
        String amountString = quantityValid ? String.format("%d", integers) : " -";
        String decs = String.format("%." + decimalPlaces + "f", decimals);

        decs = quantityValid ? decs.substring(1, decs.length()) : "";
        totalPrice = totalPrice + "   ";
        currencyPrice = "(" + currencyPrice + ")   ";
    
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int padding = 5;
        int stringWidth = fm.stringWidth(amountString);
       
        int height = fm.getHeight() + 10;

        g2d.setFont(smallFont);

        fm = g2d.getFontMetrics();
        int priceWidth = fm.stringWidth(totalPrice);
        int currencyWidth = fm.stringWidth(currencyPrice);
        int priceLength = (priceWidth > currencyWidth ? priceWidth : currencyWidth);

        //  int priceAscent = fm.getAscent();
        int integersX = priceLength + 10;
        integersX = integersX < 130 ? 130 : integersX;
        int decimalsX = integersX + stringWidth + 1;

       // int currencyNameStringWidth = fm.stringWidth(currencyName);
        int decsWidth = fm.stringWidth(decs);
        int currencyNameWidth = fm.stringWidth(currencyName);

        int width = decimalsX + stringWidth + (decsWidth < currencyNameWidth ? currencyNameWidth : decsWidth) + (padding * 2)+40;
       
        width = width < getMinImageWidth() ? getMinImageWidth() : width;

       
        int currencyNameStringX = decimalsX + 2;

        g2d.dispose();
        
        BufferedImage unitImage = SwingFXUtils.fromFXImage(priceAmount != null ? priceAmount.getCurrency().getIcon() : new Image("/assets/unknown-unit.png"), null);
        Drawing.setImageAlpha(unitImage, 0x40);
        //  adrBuchImg.getScaledInstance(width, height, java.awt.Image.SCALE_AREA_AVERAGING);
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
        //   g2d.setComposite(AlphaComposite.Clear);

        /* for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = new Color(adrBuchImg.getRGB(x, y), true);

                Color c2 = new Color(c.getRed(), c.getGreen(), c.getBlue(), 35);

                img.setRGB(x, y, c2.getRGB());
            }
        }
         */
        g2d.drawImage(unitImage, 75 , (height / 2) - (unitImage.getHeight() / 2), unitImage.getWidth(), unitImage.getHeight(), null);

       



        g2d.setFont(font);
        fm = g2d.getFontMetrics();
       
        java.awt.Color priceColor = isNotAvailable() ? java.awt.Color.RED : java.awt.Color.WHITE;
        g2d.setColor( priceColor);
        
        int integersY = fm.getAscent() + 5;
     

        g2d.drawString(amountString, integersX, integersY);

        g2d.setFont(smallFont);
        fm = g2d.getFontMetrics();
      

       
        if(decimalPlaces > 0){
            //decimalsX = widthIncrease > 0 ? decimalsX + widthIncrease : decimalsX;
            g2d.drawString(decs, decimalsX , fm.getHeight() + 2);
        }
     

        g2d.setColor(new java.awt.Color(.9f, .9f, .9f, .9f));
        g2d.drawString(currencyName, currencyNameStringX, height - 10);

        g2d.setFont(smallFont);
        g2d.setColor(java.awt.Color.WHITE);
        fm = g2d.getFontMetrics();
        g2d.drawString(totalPrice, padding, fm.getHeight() + 2);

        g2d.setColor(new java.awt.Color(.6f, .6f, .6f, .9f));
        g2d.drawString(currencyPrice, padding, height - 10);

        /*try {
            Files.writeString(logFile.toPath(), amountString + decs);
        } catch (IOException e) {

        }*/
        g2d.dispose();

       /* try {
            ImageIO.write(img, "png", new File("outputImage.png"));
        } catch (IOException e) {

        }*/

        imageBufferProperty().set(SwingFXUtils.toFXImage(img, null));
        
    }

}
