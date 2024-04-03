package com.netnotes;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;

import com.devskiller.friendly_id.FriendlyId;
import com.utils.Utils;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class ErgoAmountBox extends HBox {

    
    private final SimpleObjectProperty<PriceAmount> m_currentAmount = new SimpleObjectProperty<PriceAmount>(null);
    private String m_id = null;
    private final SimpleObjectProperty<Image> m_imgBuffer = new SimpleObjectProperty<Image>(null);
    private final SimpleObjectProperty<PriceQuote> m_priceQuoteProperty = new SimpleObjectProperty<>(null);
  // private final SimpleObjectProperty<ErgoNetworkData> m_ergoNetworkData = new SimpleObjectProperty<>(null);
    private int m_minImgWidth = 250;
    private long m_timestamp = 0;

    

 //   private Color m_secondaryColor = new Color(.4, .4, .4, .9);
 //   private Color m_primaryColor = new Color(.7, .7, .7, .9); 
  //  private Font m_smallFont = Font.font("OCR A Extended", FontWeight.NORMAL, 10);

    public ErgoAmountBox(){
        super();
        m_id = FriendlyId.createFriendlyId();
    }



    public ErgoAmountBox(PriceAmount priceAmount, Scene scene, HostServices hostServices) {
        super();
        m_id = FriendlyId.createFriendlyId();
        setId("darkRowBox");
        setMinHeight(45);
        setMaxHeight(45);

        m_currentAmount.set(priceAmount);
  

       
        
        Button amountBtn = new Button();
        amountBtn.setId("tokenBtn");
       // amountBtn.textProperty().bind(m_currentAmount.asString());
        amountBtn.setContentDisplay(ContentDisplay.LEFT);
        amountBtn.setAlignment(Pos.CENTER_LEFT);
       
       // amountBtn.setGraphicTextGap(25);
    
        m_imgBuffer.addListener((obs,oldval,newval)-> {
            if(newval != null){
                ImageView imageView = IconButton.getIconView(newval, newval.getWidth());    
                amountBtn.setGraphic(imageView);
                amountBtn.setPrefWidth( newval.getWidth() + 15 );
            }else{
                amountBtn.setPrefWidth(75);
            }

        });

        setAlignment(Pos.CENTER_LEFT);

        String textFieldId = m_id +"TextField";

        int precision = priceAmount.getCurrency().getFractionalPrecision();
        DecimalFormat df = new DecimalFormat("0");
        df.setMaximumFractionDigits(precision);

       ImageView textViewImage = IconButton.getIconView( priceAmount.getCurrency().getIcon(),35);

        VBox imgPaddingBox = new VBox(textViewImage);
        imgPaddingBox.setPadding(new Insets(0,15,0,10)); 
        imgPaddingBox.setMinHeight(40);
        imgPaddingBox.setMaxHeight(40);
        imgPaddingBox.setAlignment(Pos.CENTER_LEFT);

        TextField amountField = new TextField(df.format(priceAmount.getDoubleAmount()));
        //  amountField.setMaxHeight(20);
        amountField.setId("amountField");
        amountField.setAlignment(Pos.CENTER_LEFT);
        amountField.setEditable(false);
        amountField.setPadding(new Insets(3, 10, 3, 10));
        amountField.setUserData(textFieldId);
        amountField.setMinWidth(200);
        HBox.setHgrow(amountField, Priority.ALWAYS);
       // amountField.prefWidthProperty().bind(amountBtn.widthProperty().subtract(imgPaddingBox.widthProperty()));
       /* amountField.textProperty().addListener((obs, oldval, newval)->{
           
            String number = newval.replaceAll("[^0-9.]", "");
            int index = number.indexOf(".");
            String leftSide = index != -1 ? number.substring(0, index + 1) : number;
            String rightSide = index != -1 ?  number.substring(index + 1) : "";
            rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
            rightSide = rightSide.length() > 9 ? rightSide.substring(0, 9) : rightSide;
        
            amountField.setText(leftSide +  rightSide);
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

      

      

        amountBtn.setOnAction(actionEvent -> {
            getChildren().remove(amountBtn);
            getChildren().add(1, imgPaddingBox);
            getChildren().add( 2, amountField);

            Platform.runLater(()-> amountField.requestFocus());
        });

 
        
      //  amountBtn.prefWidthProperty().bind(this.widthProperty());

        Runnable setNotFocused = () ->{
            if (getChildren().contains(enterButton)) {
                getChildren().remove(enterButton);
            }

            if (getChildren().contains(amountField)) {
                getChildren().remove(amountField);

            }
            if (getChildren().contains( imgPaddingBox)) {
                getChildren().remove( imgPaddingBox);
            }

            if (!(getChildren().contains(amountBtn))) {
                getChildren().add(1, amountBtn);
            }

            
        };
        enterButton.setOnAction(e->{
            setNotFocused.run();
        });

        amountField.setOnAction(e->{
             setNotFocused.run();
        });

        
        m_priceQuoteProperty.addListener((obs, oldval, newval)->updateBufferedImage());
        updateBufferedImage();

        





    //    BufferedButton ergoTokensBtn = new BufferedButton();

        
        Tooltip menuBtnBtnTip = new Tooltip("Options");

        BufferedMenuButton menuBtn = new BufferedMenuButton("/assets/menu-outline-30.png", 30);
        menuBtn.setTooltip(menuBtnBtnTip);
        menuBtn.setTextAlignment(TextAlignment.LEFT);

        final String urlString = "https://ergoplatform.org";

        MenuItem urlItem = new MenuItem("Visit Website");
        urlItem.setOnAction(e->{
            hostServices.showDocument(urlString);
        });



        menuBtn.getItems().addAll( urlItem);

        getChildren().addAll(menuBtn, amountBtn);

        Runnable updates = () ->{
              
            PriceAmount currentAmount = m_currentAmount.get();
            if(currentAmount != null){
                String newAmountText = df.format(currentAmount.getDoubleAmount());
                if(!newAmountText.equals(amountField.getText())){
                    amountField.setText(newAmountText);
                }
            }else{
                amountField.setText("0");
        
            }
        
        };

        m_currentAmount.addListener((obs,oldval, newval)->{
            updates.run();
            updateBufferedImage();
        });
        updates.run();
        updateBufferedImage();
     
    }

    public  SimpleObjectProperty<Image> imageBufferProperty(){
        return m_imgBuffer;
    }

    public long getTimeStamp(){
        return m_timestamp;
    }

    public void setTimeStamp(long timeStamp){
        m_timestamp = timeStamp;
    }



    public String getBoxId(){
        return m_id;
    }

    public void setBoxId(String id){
        m_id = id;
    }


    public String getTokenId(){
        return m_currentAmount.get().getCurrency().getTokenId();
    }

    public SimpleObjectProperty<PriceAmount> priceAmountProperty(){
        return m_currentAmount;
    }

    public SimpleObjectProperty<PriceQuote> priceQuoteProperty(){
        return m_priceQuoteProperty;
    }

    public int getMinImageWidth(){
        return m_minImgWidth;
    }

    public void setMinImageWidth(int width){
        m_minImgWidth = width;
    }
   

   
    public void updateBufferedImage() {
        
        PriceAmount priceAmount = m_currentAmount.get();
        boolean quantityValid = priceAmount != null && priceAmount.getAmountValid();
        double priceAmountDouble = priceAmount != null && quantityValid ? priceAmount.getDoubleAmount() : 0;

        PriceQuote priceQuote = m_priceQuoteProperty.get();
        boolean priceValid = priceQuote != null;
        double priceQuoteDouble = priceValid  && priceQuote != null ? priceQuote.getDoubleAmount() : 0;
        
        String totalPrice = priceValid && priceQuote != null ? Utils.formatCryptoString( priceQuoteDouble * priceAmountDouble, priceQuote.getQuoteCurrency(), priceQuote.getFractionalPrecision(),  quantityValid && priceValid) : " -.--";
        int integers = priceAmount != null ? (int) priceAmount.getDoubleAmount() : 0;
        double decimals = priceAmount != null ? priceAmount.getDoubleAmount() - integers : 0;
        int decimalPlaces = priceAmount != null ? priceAmount.getCurrency().getFractionalPrecision() : 0;
        String cryptoName = priceAmount != null ? priceAmount.getCurrency().getSymbol() : "UKNOWN";
        int space = cryptoName.indexOf(" ");
        cryptoName = space != -1 ? cryptoName.substring(0, space) : cryptoName;

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
        priceLength = priceLength < 400 ? 400 : priceLength;

        //  int priceAscent = fm.getAscent();
        int integersX = priceLength + 10;
        integersX = integersX < 130 ? 130 : integersX;
        int decimalsX = integersX + stringWidth + 1;

       // int cryptoNameStringWidth = fm.stringWidth(cryptoName);
        int decsWidth = fm.stringWidth(decs);

        int width = decimalsX + stringWidth + decsWidth + (padding * 2);
        int widthIncrease = width;
        width = width < m_minImgWidth ? m_minImgWidth : width;

        widthIncrease = width - widthIncrease;

        int cryptoNameStringX = decimalsX + 2;

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
        g2d.drawImage(unitImage, 200 , (height / 2) - (unitImage.getHeight() / 2), unitImage.getWidth(), unitImage.getHeight(), null);

       



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

        
        g2d.drawString(cryptoName, cryptoNameStringX, height - 10);

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

        m_imgBuffer.set(SwingFXUtils.toFXImage(img, null));
        
    }
}
