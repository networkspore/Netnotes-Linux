package com.netnotes;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.devskiller.friendly_id.FriendlyId;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AmountSendBox extends HBox implements AmountBoxInterface {

    private long m_timestamp = 0;

    private PriceAmount m_balanceAmount;
    private BigDecimal m_sendAmount = BigDecimal.ZERO;
    private BufferedImage m_unitImage = null;

    private ChangeListener<BigDecimal> m_currentBalanceListener = null;

    private SimpleObjectProperty<PriceQuote> m_priceQuote = new SimpleObjectProperty<>(null);

    private int m_minImgWidth = 250;
    
    private String m_boxId;

    private SimpleBooleanProperty m_isFee = new SimpleBooleanProperty();
    private AmountFeeBox m_feeBox = null;
    private BigDecimal m_maxAmount = null;

    public AmountSendBox(PriceAmount balance, long timeStamp, Scene scene, boolean editable) {
        super();
        m_boxId = FriendlyId.createFriendlyId();
        m_timestamp = timeStamp;
        
        m_balanceAmount = balance;

        setId("darkBox");
        setMinHeight(40);
       
        setAlignment(Pos.CENTER_LEFT);

        m_feeBox = new AmountFeeBox(balance.getCurrency(), scene);
        VBox.setVgrow(m_feeBox, Priority.ALWAYS);
    
        ImageView amountImageView = new ImageView();

        HBox amountBtn = new HBox(amountImageView); 
       // amountBtn.textProperty().bind(m_currentAmount.asString());

        amountBtn.setId("amountBtn");
        amountBtn.setPadding(new Insets(2, 5, 2, 10));
        
        
        HBox amountsBox = new HBox(amountBtn);
        amountsBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(amountsBox,Priority.ALWAYS);

 

        Button maxAmountBtn = new Button();
        maxAmountBtn.setId("rowBox");
        maxAmountBtn.setContentDisplay(ContentDisplay.RIGHT);
        maxAmountBtn.setAlignment(Pos.CENTER_RIGHT);
        maxAmountBtn.setPadding(new Insets(0, 0, 0, 0));

        ImageView maxAmountBtnImageView = new ImageView();
        maxAmountBtnImageView.setPreserveRatio(true);
        
        maxAmountBtn.setGraphic(maxAmountBtnImageView);

       


        String textFieldId = m_boxId +"TextField";



     

        TextField amountField = new TextField();
        //  amountField.setMaxHeight(20);
        amountField.setId("amountField");
        amountField.setAlignment(Pos.CENTER_LEFT);
        amountField.setEditable(editable);
        amountField.setPadding(new Insets(3, 10, 3, 10));
        amountField.setUserData(textFieldId);
        amountField.textProperty().addListener((obs, oldval, newval)->{
            PriceCurrency currency = balance.getCurrency();
            String number = newval.replaceAll("[^0-9.]", "");
            int index = number.indexOf(".");
            String leftSide = index != -1 ? number.substring(0, index + 1) : number;
            String rightSide = index != -1 ?  number.substring(index + 1) : "";
            rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
            rightSide = rightSide.length() > currency.getDecimals() ? rightSide.substring(0, currency.getDecimals()) : rightSide;
        
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

        ImageView textViewImage = IconButton.getIconView(balance.getCurrency().getIcon(),35);

        VBox imgPaddingBox = new VBox(textViewImage);
        imgPaddingBox.setPadding(new Insets(0,15,0,10)); 
        imgPaddingBox.setMinHeight(40);
        imgPaddingBox.setAlignment(Pos.CENTER_LEFT);

        amountImageView.addEventFilter(MouseEvent.MOUSE_CLICKED, actionEvent -> {
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

        Runnable enterAction = ()->{
            
            if(editable){

                String text = amountField.getText();
                m_sendAmount = Utils.textZero(text) ?BigDecimal.ZERO : new BigDecimal(text);

            }
            setNotFocused.run();
        
        };

        enterButton.setOnAction(e->enterAction.run());

        amountField.setOnAction(e->enterAction.run());

        maxAmountBtn.setOnAction(e->{

            BigDecimal maxAmount = m_maxAmount;
            amountField.textProperty().set(maxAmount + "");    
            enterAction.run();
            
        });
        updateUnitImage();
    
        Runnable updateMaxAmount = ()->{
            
           // PriceCurrency currency = balanceAmount.getCurrency();

            boolean isFeeToken = m_isFee.get();

            if(isFeeToken){
                BigDecimal fee = m_feeBox.feeAmountProperty().get();
                BigDecimal b = m_balanceAmount.amountProperty().get();

                BigDecimal maxBalance = b.subtract(fee);

                maxBalance  = maxBalance.max(BigDecimal.ZERO);

                m_maxAmount = maxBalance;
            }else{
                m_maxAmount = m_balanceAmount.amountProperty().get();
            }
            updateAmountImage(amountImageView);
        };

        updateMaxAmount.run();

  
       

        isFeeProperty().addListener((obs,oldval,newval)->{
            if(newval){
                PriceCurrency priceCurrency = m_balanceAmount.getCurrency();
                if(priceCurrency.getTokenId().equals(ErgoCurrency.TOKEN_ID)){
                    m_feeBox.feeAmountProperty().set(AmountFeeBox.MIN_FEE);
                }
                if(!(getChildren().contains(m_feeBox))){
               
                   getChildren().add(1, m_feeBox);
                
                }
            }else{
                if(getChildren().contains(m_feeBox)){
               
                  getChildren().remove(m_feeBox);
                
                }
            }
            updateMaxAmount.run();
        });

        addBalanceListeners(updateMaxAmount,()->{
            updateUnitImage();
            updateAmountImage(amountImageView);
        });

      
        updateBufferedImage(amountImageView);
        
    }

    protected void addBalanceListeners(Runnable balanceChanged, Runnable currencyChanged){
        
        m_currentBalanceListener = (obs,oldval,newval)->{
            balanceChanged.run();
        };


        m_balanceAmount.amountProperty().addListener(m_currentBalanceListener);

      
    
    }

    public long getTimeStamp(){
        return m_timestamp;
    }

    public void setTimeStamp(long timeStamp){
        m_timestamp = timeStamp;
    }

    public BigDecimal getSendAmount(){
        return m_sendAmount;
    }



    public void updateUnitImage(){
        m_unitImage = SwingFXUtils.fromFXImage(m_balanceAmount.getCurrency().getIcon(), null);
        Drawing.setImageAlpha(m_unitImage, 0x40);
    }

    public boolean isNotAvailable(){
        BigDecimal maxAmount = m_maxAmount;
        BigDecimal sendAmount = m_sendAmount;

        if(maxAmount != null ){
            if( sendAmount.compareTo(maxAmount) < 1){
                return true;
            }
        }
        return false;
    }

    public SimpleObjectProperty<BigDecimal> feeAmountProperty(){
        return m_feeBox.feeAmountProperty();
    }
   
    private BufferedImage m_amountImg = null;
    private Graphics2D m_amountG2d = null;
    private BufferedImage m_img = null;
    private Graphics2D m_g2d = null;
    private WritableImage m_wImg = null;
    
    public void updateAmountImage(ImageView amountImgView) {
        final int padding = 5;
        
        BigDecimal max = m_maxAmount;
       
        if(max == null){
            return;
        }

        PriceCurrency currency = m_balanceAmount.getCurrency();


        
        BigInteger integers = max != null ? max.toBigInteger() : BigInteger.ZERO;
        BigDecimal decimals = max != null ? max.subtract(new BigDecimal(integers)) : BigDecimal.ZERO;
        int decimalPlaces = max != null ?  currency.getFractionalPrecision() : 0;
        String currencyName = currency.getSymbol();
        int space = currencyName.indexOf(" ");
        currencyName = space != -1 ? currencyName.substring(0, space) : currencyName;

     

        java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 30);
        java.awt.Font smallFont = new java.awt.Font("SANS-SERIF", java.awt.Font.PLAIN, 12);

        //   Image ergoBlack25 = new Image("/assets/ergo-black-25.png");
        //   SwingFXUtils.fromFXImage(ergoBlack25, null);
        
        String amountString = String.format("%d", integers);
        String decs = String.format("%." + decimalPlaces + "f", decimals);
        String maxString = "MAX";

        int maxLength = Math.min(decs.length(), decimalPlaces + 1);

        decs =  decs.substring(1, maxLength);
   
    
        m_amountImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        m_amountG2d = m_amountImg.createGraphics();
        
        m_amountG2d.setFont(font);
        FontMetrics fm = m_amountG2d.getFontMetrics();
 
        int stringWidth = fm.stringWidth(amountString);

      

        m_amountG2d.setFont(smallFont);

        fm = m_amountG2d.getFontMetrics();
        int maxWidth = fm.stringWidth(maxString);


        //  int priceAscent = fm.getAscent();
        int integersX = padding;
      
        int decimalsX = integersX + stringWidth + 1;

       // int currencyNameStringWidth = fm.stringWidth(currencyName);
        int decsWidth = decs.equals("") ? 0 : fm.stringWidth(decs);
        int currencyNameWidth = fm.stringWidth(currencyName);

        int width = decimalsX + stringWidth + (decsWidth < currencyNameWidth ? currencyNameWidth : decsWidth) + (padding * 2) + padding + maxWidth;
  

        int currencyNameStringX = decimalsX + 2;

        m_amountG2d.dispose();
        int height = 45;
        
        //  adrBuchImg.getScaledInstance(width, height, java.awt.Image.SCALE_AREA_AVERAGING);
        m_amountImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        m_amountG2d = m_amountImg.createGraphics();
        m_amountG2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        m_amountG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        m_amountG2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        m_amountG2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        m_amountG2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        m_amountG2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        m_amountG2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        m_amountG2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        //   m_amountG2d.setComposite(AlphaComposite.Clear);

        /* for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = new Color(adrBuchImg.getRGB(x, y), true);

                Color c2 = new Color(c.getRed(), c.getGreen(), c.getBlue(), 35);

                m_amountImg.setRGB(x, y, c2.getRGB());
            }
        }
         #ffffff05, #66666680, #ffffff05*/
         Drawing.fillArea(m_amountImg, 0xff000000, 0, 0, width,height);
        Drawing.drawBar(1, 0x30ffffff, 0x60666666,m_amountImg, 0, 0, width, height/2);

  

        m_amountG2d.drawImage(m_unitImage, width - m_unitImage.getWidth() - (maxWidth /2) , (height / 2) - (m_unitImage.getHeight() / 2), m_unitImage.getWidth(), m_unitImage.getHeight(), null);

       



        m_amountG2d.setFont(font);
        fm = m_amountG2d.getFontMetrics();


        m_amountG2d.setColor(java.awt.Color.WHITE);

        m_amountG2d.drawString(amountString, integersX, fm.getAscent() + 5);

        m_amountG2d.setFont(smallFont);
        fm = m_amountG2d.getFontMetrics();
        m_amountG2d.setColor(new java.awt.Color(.9f, .9f, .9f, .9f));

       
        if(decimalPlaces > 0){
            //decimalsX = widthIncrease > 0 ? decimalsX + widthIncrease : decimalsX;
            m_amountG2d.drawString(decs, decimalsX , fm.getHeight() + 2);
        }

        
        m_amountG2d.drawString(currencyName, currencyNameStringX, height - 10);
        // ((height - fm.getHeight()) / 2) + fm.getAscent())
        m_amountG2d.setColor(Color.WHITE);
        m_amountG2d.drawString(maxString, width - ((padding*2) + maxWidth),  ((height - fm.getHeight()) / 2) + fm.getAscent());
       // m_amountG2d.setFont(smallFont);
   
     //   fm = m_amountG2d.getFontMetrics();


        
        /*try {
            Files.writeString(logFile.toPath(), amountString + decs);
        } catch (IOException e) {

        }*/

       /* try {
            ImageIO.write(m_amountImg, "png", new File("outputImage.png"));
        } catch (IOException e) {

        }*/

        amountImgView.setImage(SwingFXUtils.toFXImage(m_amountImg, null));
        
        m_amountG2d.dispose();
        m_amountG2d = null;
        m_amountImg = null; 
    }

    public SimpleBooleanProperty isFeeProperty(){
        return m_isFee;
    }

    public int getMinImageWidth(){
        return m_minImgWidth;
    }

    public void setMinImageWidth(int width){
        m_minImgWidth = width;
    }
   
 
    public void updateBufferedImage(ImageView imageView) {
   /*     BigDecimal sendAmount = m_sendAmount;
        
        PriceCurrency currency = m_balanceAmount.getCurrency();

        boolean priceValid = priceQuote != null && priceQuote.getTimeStamp() != 0 && priceQuote.howOldMillis() < AddressesData.QUOTE_TIMEOUT;
        BigDecimal priceQuoteBigDecimal = priceValid  && priceQuote != null ? priceQuote.getBigDecimalAmount() : BigDecimal.valueOf(0);
        
        String totalPrice = priceValid && priceQuote != null ? Utils.formatCryptoString( sendAmount.multiply(priceQuoteBigDecimal), priceQuote.getQuoteCurrency(), priceQuote.getFractionalPrecision(),  priceValid) : " -.--";
        BigInteger integers = sendAmount.toBigInteger() ;
        BigDecimal decimals =sendAmount.subtract(new BigDecimal(integers));

        int decimalPlaces =  currency.getFractionalPrecision();
        String currencyName =  currency.getSymbol();
        int space = currencyName.indexOf(" ");
        currencyName = space != -1 ? currencyName.substring(0, space) : currencyName;

        String currencyPrice = priceValid && priceQuote != null ? priceQuote.toString() : "-.--";

        java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 30);
        java.awt.Font smallFont = new java.awt.Font("SANS-SERIF", java.awt.Font.PLAIN, 12);

        //   Image ergoBlack25 = new Image("/assets/ergo-black-25.png");
        //   SwingFXUtils.fromFXImage(ergoBlack25, null);
        
        String amountString = String.format("%d", integers);
        String decs = String.format("%." + decimalPlaces + "f", decimals);

        decs = decs.substring(1, decs.length());
        totalPrice = totalPrice + "   ";
        currencyPrice = "(" + currencyPrice + ")   ";
    
        m_img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        m_g2d = m_img.createGraphics();
        
        m_g2d.setFont(font);
        FontMetrics fm = m_g2d.getFontMetrics();
        int padding = 5;
        int stringWidth = fm.stringWidth(amountString);
       
        int height = fm.getHeight() + 10;

        m_g2d.setFont(smallFont);

        fm = m_g2d.getFontMetrics();
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

        m_g2d.dispose();
        
         //  adrBuchImg.getScaledInstance(width, height, java.awt.Image.SCALE_AREA_AVERAGING);
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


        m_g2d.drawImage(m_unitImage, 75 , (height / 2) - (m_unitImage.getHeight() / 2), m_unitImage.getWidth(), m_unitImage.getHeight(), null);

       



        m_g2d.setFont(font);
        fm = m_g2d.getFontMetrics();
       
        java.awt.Color priceColor = isNotAvailable() ? java.awt.Color.RED : java.awt.Color.WHITE;
        m_g2d.setColor( priceColor);
        
        int integersY = fm.getAscent() + 5;
     

        m_g2d.drawString(amountString, integersX, integersY);

        m_g2d.setFont(smallFont);
        fm = m_g2d.getFontMetrics();
      

       
        if(decimalPlaces > 0){
            //decimalsX = widthIncrease > 0 ? decimalsX + widthIncrease : decimalsX;
            m_g2d.drawString(decs, decimalsX , fm.getHeight() + 2);
        }
     

        m_g2d.setColor(new java.awt.Color(.9f, .9f, .9f, .9f));
        m_g2d.drawString(currencyName, currencyNameStringX, height - 10);

        m_g2d.setFont(smallFont);
        m_g2d.setColor(java.awt.Color.WHITE);
        fm = m_g2d.getFontMetrics();
        m_g2d.drawString(totalPrice, padding, fm.getHeight() + 2);

        m_g2d.setColor(new java.awt.Color(.6f, .6f, .6f, .9f));
        m_g2d.drawString(currencyPrice, padding, height - 10);

    
        m_g2d.dispose();



        imageView.setImage(SwingFXUtils.toFXImage(m_img, m_wImg));

        m_g2d = null;
        m_img = null;
       */ 
    }


    public void updateToken(){
        
    }
    public PriceAmount getPriceAmount(){
        return m_balanceAmount;
    }
    public PriceAmount getBalanceAmount(){
        return m_balanceAmount;
    }

    protected void removeListeners(){
        if(m_balanceAmount != null){
            if( m_currentBalanceListener != null){
                m_balanceAmount.amountProperty().removeListener(m_currentBalanceListener);
            }
           
        }
    }

    public PriceQuote getPriceQuote() {
        return m_priceQuote.get();
    }

    public void setPriceQuote(PriceQuote priceQuote) {
        m_priceQuote.set(priceQuote);
    }

    public void shutdown(){
        removeListeners();
        m_balanceAmount = null;
        m_sendAmount = null;
    }

    public String getTokenId(){
        return m_balanceAmount.getTokenId();
    }

}
