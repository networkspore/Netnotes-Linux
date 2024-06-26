package com.netnotes;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.time.LocalDateTime;


import com.devskiller.friendly_id.FriendlyId;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
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
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;


public class AmountBox extends HBox {

    private long m_quoteTimeout = AddressesData.QUOTE_TIMEOUT;
    private final SimpleObjectProperty<PriceAmount> m_currentAmount = new SimpleObjectProperty<PriceAmount>(null);
    private String m_id = null;
    private final SimpleObjectProperty<Image> m_imgBuffer = new SimpleObjectProperty<Image>(null);
    private final SimpleObjectProperty<PriceQuote> m_priceQuoteProperty = new SimpleObjectProperty<>(null);

    private int m_minImgWidth = 250;
    private long m_timestamp = 0;

    

  //  private Color m_secondaryColor = new Color(.4, .4, .4, .9);
    //private Color m_primaryColor = new Color(.7, .7, .7, .9); 
    //private Font m_smallFont = Font.font("OCR A Extended", FontWeight.NORMAL, 10);

    public AmountBox(){
        super();
        m_id = FriendlyId.createFriendlyId();
    }

    private AddressesData m_addressesData;

    public AmountBox(PriceAmount priceAmount, Scene scene, AddressesData addressesData) {
        super();
        m_id = FriendlyId.createFriendlyId();
        setId("darkRowBox");
        setMinHeight(45);
        setMaxHeight(45);

        m_currentAmount.set(priceAmount);
        m_addressesData = addressesData;
        

       
        
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
            m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().showwManageStage();
          
        });

        final String enableString = "enable";

        MenuItem enableErgoTokens = new MenuItem("Enable Ergo Tokens");
        enableErgoTokens.setOnAction(e->{
            m_addressesData.isErgoTokensProperty().set(true);
        });

    
        final String viewString = "viewToken";
      //  MenuItem addItem = new MenuItem("Add to Ergo Tokens");
        MenuItem viewItem = new MenuItem("Open");

        MenuItem currencyUrlItem = new MenuItem("Visit Website");



        getChildren().addAll( ergoTokensBtn, amountBtn);

        

        Runnable updates = () ->{
            
           
            ErgoTokensList tokensList = m_addressesData.tokensListProperty().get();

            boolean isErgoTokens = tokensList != null;

            Object btnUserData =ergoTokensBtn.getUserData();
            String tokenOptionsBtnUserData = btnUserData != null && btnUserData instanceof String ?  (String) btnUserData : "null";

            PriceAmount currentAmount = m_currentAmount.get();

            if(currentAmount != null){

                String newAmountText = df.format(currentAmount.getDoubleAmount());
                if(!newAmountText.equals(amountField.getText())){
                    amountField.setText(newAmountText);
                }
                PriceCurrency currency = currentAmount.getCurrency();
                
               
                if(tokensList != null){
                   
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
                     /*else{
                        if(!tokenOptionsBtnUserData.equals(rememberString)){
                            ergoTokensBtn.setUserData(rememberString);
                            ergoTokensBtn.getItems().clear();
                            ergoTokensBtn.getItems().add(addItem);
                            addItem.setOnAction(e->{
                                  
                               NetworkType tokenNetworkType = currency.getNetworkTypeString().equals(NetworkType.TESTNET.toString()) ? NetworkType.TESTNET : NetworkType.MAINNET;
                               
                               //  Alert a = new Alert(AlertType.NONE, "ok", ButtonType.OK);
                             //   a.show();

                                 //String name, String url, String tokenId, String fileString, HashData hashData, NetworkType networkType, TokensList tokensList
                                ErgoNetworkToken newToken = new ErgoNetworkToken(currency.getName(), "https://spectrum.fi/", currency.getTokenId(), "", null, tokenNetworkType, tokensList);

                                Stage addEditTokenStage =  new Stage();
                                addEditTokenStage.getIcons().add(ErgoTokens.getAppIcon());
                                addEditTokenStage.initStyle(StageStyle.UNDECORATED);
                                Button addEditTokenStageCloseBtn = new Button();
                                Scene addTokenScene = tokensList.getEditTokenScene(newToken, tokenNetworkType, addEditTokenStage, addEditTokenStageCloseBtn);
                                
                                addEditTokenStage.setScene(addTokenScene);
                                addEditTokenStage.show();
                                addEditTokenStageCloseBtn.setOnAction(e1->{
                                    addEditTokenStage.close();
                                });
                            });
                        }*/
                    }
                }else{
                    if(!isErgoTokens && m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoTokens.NETWORK_ID) != null){
                        
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

        Runnable updatePriceQuote = ()->{
            
            PriceAmount currentAmount = m_currentAmount.get();
            if(currentAmount != null){
                PriceCurrency priceCurrency = currentAmount.getCurrency();
                if(priceCurrency != null && priceCurrency instanceof ErgoNetworkToken){
                    
                    ErgoNetworkToken networkToken = (ErgoNetworkToken) priceCurrency;
                    PriceQuote quote = networkToken.getPriceQuote();
                    
                    
                    m_priceQuoteProperty.set( quote);    
                }else{
                    updateBufferedImage();
                }
            }else{
                updateBufferedImage();
            }
        
        };

        ChangeListener<LocalDateTime> listPriceListener = (obs,oldval,newval)->updatePriceQuote.run();
        ChangeListener<LocalDateTime> updateListener = (obs,oldval,newval)->{
            PriceAmount currentAmount = m_currentAmount.get();
            if(currentAmount != null){
                textViewImage.setImage(currentAmount.getCurrency().getIcon());
            }
        };
        m_addressesData.tokensListProperty().addListener((obs,oldval,newval)->{
            updates.run();
            if(oldval != null){
                oldval.marketUpdated().removeListener(listPriceListener);
                oldval.getLastUpdated().removeListener(updateListener);
            }
            if(newval != null){
                newval.marketUpdated().addListener(listPriceListener);
                newval.getLastUpdated().removeListener(updateListener);
            }
            updateBufferedImage();
        });
        if(m_addressesData.tokensListProperty().get() != null){
            m_addressesData.tokensListProperty().get().marketUpdated().addListener(listPriceListener);
            m_addressesData.tokensListProperty().get().getLastUpdated().addListener(updateListener);

        }
        m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().addNetworkListener((ListChangeListener.Change<? extends NoteInterface> c) -> updates.run());
        updateBufferedImage();
        updatePriceQuote.run();
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
   
    private BufferedImage m_img = null;
    private Graphics2D m_g2d = null;
    
    private java.awt.Font m_font = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 30);
    private java.awt.Font m_smallFont = new java.awt.Font("SANS-SERIF", java.awt.Font.PLAIN, 12);
    private java.awt.Color m_txtColor = new java.awt.Color(.9f, .9f, .9f, .9f);
   
    public void updateBufferedImage() {
        PriceAmount priceAmount = m_currentAmount.get();
        boolean quantityValid = priceAmount != null && priceAmount.getAmountValid();
        BigDecimal priceAmountDecimal = priceAmount != null && quantityValid ? priceAmount.getBigDecimalAmount() : BigDecimal.valueOf(0);

        PriceCurrency priceCurrency = priceAmount != null ? priceAmount.getCurrency() : null;

        int decimalPlaces = priceCurrency != null ? priceCurrency.getFractionalPrecision() : 0;
        String currencySymbol =  priceCurrency != null ? priceCurrency.getSymbol() : "UKNOWN";

        PriceQuote priceQuote = m_priceQuoteProperty.get();
        //PriceQuote priceQuote = priceQuoteBase != null ? priceQuoteBase.getPriceQuote(quantityValid ? priceAmount.getTokenId() : null) : null;
     
        //String tokenId = priceAmount != null ? priceAmount.getTokenId() : null;

        //PriceQuote priceQuote =  (priceQuoteBase != null && tokenId != null ? priceQuoteBase.getPriceQuote(tokenId): null);

        boolean priceValid = priceQuote != null; //&& priceQuote.getTimeStamp() != 0 && priceQuote.howOldMillis() < m_quoteTimeout;
    
        
        BigDecimal priceQuoteBigDecimal = priceValid  && priceQuote != null ? priceQuote.getBigDecimalAmount() : BigDecimal.valueOf(0);
        
        String totalPrice = priceValid && priceQuote != null ? Utils.formatCryptoString( priceAmountDecimal.multiply(priceQuoteBigDecimal), priceQuote.getQuoteCurrency(), priceQuote.getFractionalPrecision(),  quantityValid && priceValid) : " -.--";
        BigInteger integers = priceAmount != null ? priceAmount.getBigDecimalAmount().toBigInteger() : BigInteger.ZERO;
        BigDecimal decimals = priceAmount != null ? priceAmount.getBigDecimalAmount().subtract(new BigDecimal(integers)) : BigDecimal.ZERO;
      
       

        //String currencyName = priceAmount != null ? priceAmount.getCurrency().getSymbol() : "Token";

        String currencyPrice = priceValid && priceQuote != null ? priceQuote.toString() : "-.--";



        //   Image ergoBlack25 = new Image("/assets/ergo-black-25.png");
        //   SwingFXUtils.fromFXImage(ergoBlack25, null);
        
        String amountString = quantityValid ? String.format("%d", integers) : " -";
        String decs = String.format("%." + decimalPlaces + "f", decimals);

        decs = quantityValid ? decs.substring(1, decs.length()) : "";
        totalPrice = totalPrice + "   ";
        currencyPrice = "(" + currencyPrice + ")   ";
    
        m_img  = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        m_g2d = m_img.createGraphics();
        
        m_g2d.setFont(m_font);
        FontMetrics fm = m_g2d.getFontMetrics();
        int padding = 5;
        int stringWidth = fm.stringWidth(amountString);
       
        int height = fm.getHeight() + 10;

        m_g2d.setFont(m_smallFont);

        fm = m_g2d.getFontMetrics();
        int priceWidth = fm.stringWidth(totalPrice);
        int currencyWidth = fm.stringWidth(currencyPrice);
        int priceLength = (priceWidth > currencyWidth ? priceWidth : currencyWidth);
       
        priceLength = priceLength < 400 ? 400 : priceLength;
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

        m_g2d.dispose();
        
        BufferedImage unitImage = SwingFXUtils.fromFXImage(priceAmount != null ? priceAmount.getCurrency().getIcon() : new Image("/assets/unknown-unit.png"), null);
        Drawing.setImageAlpha(unitImage, 0x40);
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
        //   m_g2d.setComposite(AlphaComposite.Clear);

        /* for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = new Color(adrBuchImg.getRGB(x, y), true);

                Color c2 = new Color(c.getRed(), c.getGreen(), c.getBlue(), 35);

                m_img.setRGB(x, y, c2.getRGB());
            }
        }
         */
        m_g2d.drawImage(unitImage, 200 , (height / 2) - (unitImage.getHeight() / 2), unitImage.getWidth(), unitImage.getHeight(), null);

        //m_g2d.setFont(m_smallFont);
      //  m_g2d.setColor(new java.awt.Color(0x777777, false));
        
     //   m_g2d.drawString(currencyName,0, smallAscent);


        m_g2d.setFont(m_font);
        fm = m_g2d.getFontMetrics();
        m_g2d.setColor(java.awt.Color.WHITE);

        

        m_g2d.drawString(amountString, integersX, fm.getAscent() + 5);

        m_g2d.setFont(m_smallFont);
        fm = m_g2d.getFontMetrics();
        m_g2d.setColor(m_txtColor);

       
        if(decimalPlaces > 0){
            //decimalsX = widthIncrease > 0 ? decimalsX + widthIncrease : decimalsX;
            m_g2d.drawString(decs, decimalsX , fm.getHeight() + 2);
        }

        
        m_g2d.drawString(currencySymbol, currencySymbolStringX, height - 10);

        m_g2d.setFont(m_smallFont);
        m_g2d.setColor(java.awt.Color.WHITE);
        fm = m_g2d.getFontMetrics();
        m_g2d.drawString(totalPrice, padding, fm.getHeight() + 2);

        m_g2d.setColor(new java.awt.Color(.6f, .6f, .6f, .9f));
        m_g2d.drawString(currencyPrice, padding, height - 10);

        /*try {
            Files.writeString(logFile.toPath(), amountString + decs);
        } catch (IOException e) {

        }*/
        

       /* try {
            ImageIO.write(m_img, "png", new File("outputImage.png"));
        } catch (IOException e) {

        }*/

        m_imgBuffer.set(SwingFXUtils.toFXImage(m_img, null));
        m_g2d.dispose();
        m_g2d = null;
        m_img = null;

    }


}
