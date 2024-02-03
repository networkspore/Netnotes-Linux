package com.netnotes;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.time.LocalDateTime;

import org.ergoplatform.appkit.NetworkType;

import com.devskiller.friendly_id.FriendlyId;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AmountBox extends HBox {

    private long m_quoteTimeout = AddressData.QUOTE_TIMEOUT;
    private final SimpleObjectProperty<PriceAmount> m_currentAmount = new SimpleObjectProperty<PriceAmount>(null);
    private String m_id = FriendlyId.createFriendlyId();
    private final SimpleObjectProperty<Image> m_imgBuffer = new SimpleObjectProperty<Image>(null);
    private final SimpleObjectProperty<PriceQuote> m_priceQuoteProperty = new SimpleObjectProperty<>(null);

    private int m_minImgWidth = 250;
    private long m_timestamp = 0;

    

  //  private Color m_secondaryColor = new Color(.4, .4, .4, .9);
    private Color m_primaryColor = new Color(.7, .7, .7, .9); 
    private Font m_smallFont = Font.font("OCR A Extended", FontWeight.NORMAL, 10);

    public AmountBox(){
        super();
        
    }



    public AmountBox(PriceAmount priceAmount, Scene scene, SimpleBooleanProperty isErgoTokensProperty, ErgoNetworkData ergoNetworkData) {
        super();
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
        //amountField.prefWidthProperty().bind(amountBtn.widthProperty().subtract(imgPaddingBox.widthProperty()));
        /*amountField.textProperty().addListener((obs, oldval, newval)->{
           
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
      

        


        Tooltip ergoTokensBtnTip = new Tooltip("Options");
        

        BufferedMenuButton ergoTokensBtn = new BufferedMenuButton("/assets/menu-outline-30.png", 30);
        ergoTokensBtn.setTooltip(ergoTokensBtnTip);
        ergoTokensBtn.setUserData("null");
        ergoTokensBtn.setTextAlignment(TextAlignment.LEFT);

        final String installString = "install";

        MenuItem installErgoTokensItem = new MenuItem("Install Ergo Tokens");
        installErgoTokensItem.setOnAction(e->{
            ergoNetworkData.showwManageStage();
        });

        final String enableString = "enable";

        MenuItem enableErgoTokens = new MenuItem("Enable Ergo Tokens");
        enableErgoTokens.setOnAction(e->{
            isErgoTokensProperty.set(true);
        });

        final String rememberString = "Remember";
        final String viewString = "viewToken";
        MenuItem addItem = new MenuItem("Add to Ergo Tokens");
        MenuItem viewItem = new MenuItem("Open");

        MenuItem currencyUrlItem = new MenuItem("Visit Website");



        getChildren().addAll( ergoTokensBtn, amountBtn);

        

        Runnable updates = () ->{
            
            boolean isErgoTokens = isErgoTokensProperty.get();
            ErgoTokens ergoTokens = (ErgoTokens) ergoNetworkData.getNetwork(ErgoTokens.NETWORK_ID) ;
            Object btnUserData =ergoTokensBtn.getUserData();
            String tokenOptionsBtnUserData = btnUserData != null && btnUserData instanceof String ?  (String) btnUserData : "null";

            PriceAmount currentAmount = m_currentAmount.get();
            if(currentAmount != null){

                String newAmountText = df.format(currentAmount.getDoubleAmount());
                if(!newAmountText.equals(amountField.getText())){
                    amountField.setText(newAmountText);
                }
                PriceCurrency currency = currentAmount.getCurrency();
        
               

                if(ergoTokens != null && isErgoTokens){
            
                    if(currency instanceof ErgoNetworkToken){
                        
                        if(!tokenOptionsBtnUserData.equals(viewString)){
                            ergoTokensBtn.setUserData(viewString);
                            ergoTokensBtn.getItems().clear();
                            
                            ErgoNetworkToken ergoNetworkToken = (ErgoNetworkToken) currency;
                            currencyUrlItem.setOnAction(e->{
                                ergoNetworkToken.visitUrl();
                            });
                            ergoTokensBtn.getItems().add(currencyUrlItem);

                            viewItem.setOnAction(e->{
                                ergoNetworkToken.open();
                            });
                            ergoTokensBtn.getItems().add(viewItem);
                        }   
                    }else{
                        if(!tokenOptionsBtnUserData.equals(rememberString)){
                            ergoTokensBtn.setUserData(rememberString);
                            ergoTokensBtn.getItems().clear();
                            ergoTokensBtn.getItems().add(addItem);
                            addItem.setOnAction(e->{
                                  
                               NetworkType tokenNetworkType = currency.getNetworkTypeString().equals(NetworkType.TESTNET.toString()) ? NetworkType.TESTNET : NetworkType.MAINNET;
                               ErgoTokensList tokensList = ergoTokens.getTokensList(tokenNetworkType);
                               //  Alert a = new Alert(AlertType.NONE, "ok", ButtonType.OK);
                             //   a.show();

                                 //String name, String url, String tokenId, String fileString, HashData hashData, NetworkType networkType, TokensList tokensList
                                ErgoNetworkToken newToken = new ErgoNetworkToken(currency.getName(), "https://spectrum.fi/", currency.getTokenId(), "", null, tokenNetworkType, tokensList);

                                Stage addEditTokenStage =  new Stage();
                                addEditTokenStage.getIcons().add(ErgoTokens.getAppIcon());
                                addEditTokenStage.initStyle(StageStyle.UNDECORATED);
                                
                                Button stageCloseBtn = new Button();

                             
                                Scene addTokenScene = tokensList.getEditTokenScene(newToken, tokenNetworkType, addEditTokenStage, stageCloseBtn);

                                addEditTokenStage.setScene(addTokenScene);
                                addEditTokenStage.show();
                                stageCloseBtn.setOnAction(e1->{
                                    addEditTokenStage.close();
                                });
                            });
                        }
                    }
                }else{
                    if(!isErgoTokens && ergoTokens != null){
                        
                        if(!tokenOptionsBtnUserData.equals(enableString)){
                            ergoTokensBtn.setUserData(enableString);
                            ergoTokensBtn.getItems().clear();
                            ergoTokensBtn.getItems().add(enableErgoTokens);
                        }
                    }else{
                        if(!tokenOptionsBtnUserData.equals(installString)){
                            ergoTokensBtn.setUserData(installString);
                            ergoTokensBtn.getItems().clear();
                            ergoTokensBtn.getItems().add(installErgoTokensItem);
                   
                        }
                    }
                }
            }else{

                amountField.setText("0");
                ergoTokensBtn.getItems().clear();
            }
            
            
        };

        m_currentAmount.addListener((obs,oldval, newval)->{
            updates.run();
            updateBufferedImage();
        });
        isErgoTokensProperty.addListener((obs,oldval,newval)->updates.run());
        ergoNetworkData.addNetworkListener((ListChangeListener.Change<? extends NoteInterface> c) -> updates.run());
        
        updateBufferedImage();
        updates.run();
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

    public long getQuoteTimeout(){
        return m_quoteTimeout;
    }

    public void setQuoteTimeout(long timeout){
        m_quoteTimeout = timeout;
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
        BigDecimal priceAmountDecimal = priceAmount != null && quantityValid ? priceAmount.getBigDecimalAmount() : BigDecimal.valueOf(0);

        PriceQuote priceQuote = m_priceQuoteProperty.get();
        //PriceQuote priceQuote = priceQuoteBase != null ? priceQuoteBase.getPriceQuote(quantityValid ? priceAmount.getTokenId() : null) : null;
     
        //String tokenId = priceAmount != null ? priceAmount.getTokenId() : null;

        //PriceQuote priceQuote =  (priceQuoteBase != null && tokenId != null ? priceQuoteBase.getPriceQuote(tokenId): null);

        boolean priceValid = priceQuote != null && priceQuote.getTimeStamp() != 0 && priceQuote.howOldMillis() < m_quoteTimeout;
        
        
        BigDecimal priceQuoteBigDecimal = priceValid  && priceQuote != null ? priceQuote.getBigDecimalAmount() : BigDecimal.valueOf(0);
        
        String totalPrice = priceValid && priceQuote != null ? Utils.formatCryptoString( priceAmountDecimal.multiply(priceQuoteBigDecimal), priceQuote.getQuoteCurrency(), priceQuote.getFractionalPrecision(),  quantityValid && priceValid) : " -.--";
        BigInteger integers = priceAmount != null ? priceAmount.getBigDecimalAmount().toBigInteger() : BigInteger.ZERO;
        BigDecimal decimals = priceAmount != null ? priceAmount.getBigDecimalAmount().subtract(new BigDecimal(integers)) : BigDecimal.ZERO;
        int decimalPlaces = priceAmount != null ? priceAmount.getCurrency().getFractionalPrecision() : 0;
        String currencySymbol = priceAmount != null ? priceAmount.getCurrency().getSymbol() : "UKNOWN";
       

        //String currencyName = priceAmount != null ? priceAmount.getCurrency().getSymbol() : "Token";

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
       
       // int smallAscent = fm.getAscent();

        //  int priceAscent = fm.getAscent();
        int integersX = priceLength + 10;
        integersX = integersX < 130 ? 130 : integersX;
        int decimalsX = integersX + stringWidth + 1;

       // int currencySymbolStringWidth = fm.stringWidth(currencySymbol);
        int decsWidth = fm.stringWidth(decs);
        int currencySymbolWidth = fm.stringWidth(currencySymbol);

        int width = decimalsX + stringWidth + (decsWidth < currencySymbolWidth ? currencySymbolWidth : decsWidth) + (padding * 2)+40;
       
        width = width < m_minImgWidth ? m_minImgWidth : width;

       
        int currencySymbolStringX = decimalsX + 2;

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

        //g2d.setFont(smallFont);
      //  g2d.setColor(new java.awt.Color(0x777777, false));
        
     //   g2d.drawString(currencyName,0, smallAscent);


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

        
        g2d.drawString(currencySymbol, currencySymbolStringX, height - 10);

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
