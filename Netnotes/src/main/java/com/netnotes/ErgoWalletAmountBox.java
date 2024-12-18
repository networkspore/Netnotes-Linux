package com.netnotes;


import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import com.devskiller.friendly_id.FriendlyId;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;


public class ErgoWalletAmountBox extends HBox implements AmountBoxInterface {

    private long m_quoteTimeout = AddressesData.QUOTE_TIMEOUT;
    private PriceAmount m_priceAmount = null;
    private String m_id = null;

    private int m_minImgWidth = 250;
    private SimpleBooleanProperty m_showSubMenuProperty = new SimpleBooleanProperty(false);

    private ChangeListener<BigDecimal> m_amountListener = null;
    private ChangeListener<PriceQuote> m_quoteListener = null;
    private SimpleDoubleProperty m_colWidth = new SimpleDoubleProperty(180);
    
    private Text m_quoteAmountSymbolText = null;
    private TextField m_quoteAmountField = null;
    private VBox m_quoteAmountBox = null;
    private HBox m_topBox;
    private HBox m_fieldBox = null;
    private HBox m_balanceFieldBox;

    private SimpleObjectProperty<PriceQuote> m_priceQuote = new SimpleObjectProperty<>(null);
    private SimpleObjectProperty<BigDecimal> m_quoteAmount = new SimpleObjectProperty<>(null);
    public ErgoWalletAmountBox(){
        super();
        m_id = FriendlyId.createFriendlyId();
    }

 
    

    public ErgoWalletAmountBox(PriceAmount priceAmount, Scene scene) {
        super();
          int rowPadding = 2;

        m_id = FriendlyId.createFriendlyId();
        m_priceAmount = priceAmount;

        Label toggleShowSubMenuBtn = new Label(m_showSubMenuProperty.get() ? "⏷" : "⏵");
        toggleShowSubMenuBtn.setId("caretBtn");
        toggleShowSubMenuBtn.setMinWidth(25);
        
        TextField currencyName = new TextField(priceAmount.getCurrency().getName());
        HBox.setHgrow(currencyName,Priority.ALWAYS);
        currencyName.maxWidthProperty().bind(m_colWidth);
        currencyName.setEditable(false);

        TextField amountField = new TextField();
        HBox.setHgrow(amountField, Priority.ALWAYS);
        amountField.setEditable(false);
        amountField.setAlignment(Pos.CENTER_LEFT);
        amountField.textProperty().bind(priceAmount.amountProperty().asString());
        

        ImageView currencyImageView = new ImageView();
        currencyImageView.setPreserveRatio(true);
        currencyImageView.setFitWidth(App.MENU_BAR_IMAGE_WIDTH);
        currencyImageView.setImage(m_priceAmount.getCurrency().getIcon());

        HBox amaountFieldBox = new HBox(amountField);
        HBox.setHgrow(amaountFieldBox, Priority.ALWAYS);
        amaountFieldBox.setAlignment(Pos.CENTER_LEFT);
        amaountFieldBox.setMinHeight(30);

        m_balanceFieldBox = new HBox(amaountFieldBox);
        HBox.setHgrow(m_balanceFieldBox,Priority.ALWAYS);
        m_balanceFieldBox.setId("bodyBox");
        m_balanceFieldBox.setAlignment(Pos.CENTER_LEFT);

        m_topBox = new HBox(toggleShowSubMenuBtn, currencyImageView, currencyName, m_balanceFieldBox);
        HBox.setHgrow(m_topBox, Priority.ALWAYS);
        m_topBox.setAlignment(Pos.CENTER_LEFT);
        m_topBox.setPadding(new Insets(0,10,1,0));


     
        //////Body
        /// 
        //tokenId
        Label tokenIdIcon = new Label("  ");
        tokenIdIcon.minWidth(40);

        Label tokenIdText = new Label("Token Id");
        HBox.setHgrow(tokenIdText, Priority.ALWAYS);
        tokenIdText.setFont(App.txtFont);
        tokenIdText.setPadding(new Insets(0,5,0,5));
        tokenIdText.maxWidthProperty().bind(m_colWidth);

        TextField tokenIdField = new TextField( getTokenId());
        HBox.setHgrow(tokenIdField,Priority.ALWAYS);
        tokenIdField.setEditable(false);

        HBox tokenIdFieldBox = new HBox(tokenIdField);
        HBox.setHgrow(tokenIdFieldBox,Priority.ALWAYS);
        tokenIdFieldBox.setId("bodyBox");
        tokenIdFieldBox.setAlignment(Pos.CENTER_LEFT);
        tokenIdFieldBox.setMaxHeight(18);
  
        HBox tokenIdBox = new HBox(tokenIdIcon, tokenIdText, tokenIdFieldBox);
        HBox.setHgrow(tokenIdBox,Priority.ALWAYS);
        tokenIdBox.setAlignment(Pos.CENTER_LEFT);
        tokenIdBox.setPadding(new Insets(0,0,rowPadding,0));

        //Description
        Label descriptionIcon = new Label("  ");
        descriptionIcon.minWidth(40);

        Label descriptionText = new Label("Description");
        HBox.setHgrow(descriptionText,Priority.ALWAYS);
        descriptionText.setFont(App.titleFont);
        descriptionText.setPadding(new Insets(0,5,0,5));
        descriptionText.maxWidthProperty().bind(m_colWidth);

        TextField descriptionField = new TextField( );
        HBox.setHgrow(descriptionField,Priority.ALWAYS);
        descriptionField.setEditable(false);
        descriptionField.textProperty().bind(m_priceAmount.getCurrency().descriptionProperty());

        HBox descriptionFieldBox = new HBox(descriptionField);
        HBox.setHgrow(descriptionFieldBox,Priority.ALWAYS);
        descriptionFieldBox.setId("bodyBox");
        descriptionFieldBox.setAlignment(Pos.CENTER_LEFT);
        descriptionFieldBox.setMaxHeight(18);
  
        HBox descriptionBox = new HBox(descriptionIcon, descriptionText, descriptionFieldBox);
        HBox.setHgrow(descriptionBox,Priority.ALWAYS);
        descriptionBox.setAlignment(Pos.CENTER_LEFT);
        descriptionBox.setPadding(new Insets(0, 0,rowPadding,0));
        //emissionAmount

        Label emissionAmountIcon = new Label("  ");
        emissionAmountIcon.minWidth(40);

        Label emissionAmountText = new Label("Emission"); 
        HBox.setHgrow(emissionAmountText,Priority.ALWAYS);
        emissionAmountText.setFont(App.txtFont);
        emissionAmountText.setPadding(new Insets(0,5,0,5));
        emissionAmountText.maxWidthProperty().bind(m_colWidth);

        TextField emissionAmountField = new TextField();
        emissionAmountField.setEditable(false);
        emissionAmountField.textProperty().bind(m_priceAmount.getCurrency().emissionAmountProperty().asString());
        HBox.setHgrow(emissionAmountField,Priority.ALWAYS);

        HBox emissionAmountFieldBox = new HBox(emissionAmountField);
        HBox.setHgrow(emissionAmountFieldBox,Priority.ALWAYS);
        emissionAmountFieldBox.setId("bodyBox");
        emissionAmountFieldBox.setAlignment(Pos.CENTER_LEFT);
        emissionAmountFieldBox.setMaxHeight(18);
  
        HBox emissionAmountBox = new HBox(emissionAmountIcon, emissionAmountText, emissionAmountFieldBox);
        HBox.setHgrow(emissionAmountBox,Priority.ALWAYS);
        emissionAmountBox.setAlignment(Pos.CENTER_LEFT);
        emissionAmountBox.setPadding(new Insets(0,0,rowPadding,0));
        //decimals

        Label decimalsIcon = new Label("  ");
        decimalsIcon.minWidth(40);

        Label decimalsText = new Label("Decimals"); 
        HBox.setHgrow(decimalsText,Priority.ALWAYS);
        decimalsText.setFont(App.txtFont);
        decimalsText.setPadding(new Insets(0,5,0,5));
        decimalsText.maxWidthProperty().bind(m_colWidth);

        TextField decimalsField = new TextField(m_priceAmount.getCurrency().getDecimals() + "");
        decimalsField.setEditable(false);
        HBox.setHgrow(decimalsField,Priority.ALWAYS);

        HBox decimalsFieldBox = new HBox(decimalsField);
        HBox.setHgrow(decimalsFieldBox,Priority.ALWAYS);
        decimalsFieldBox.setId("bodyBox");
        decimalsFieldBox.setAlignment(Pos.CENTER_LEFT);
        decimalsFieldBox.setMaxHeight(18);
  
        HBox decimalsBox = new HBox(decimalsIcon, decimalsText, decimalsFieldBox);
        HBox.setHgrow(decimalsBox,Priority.ALWAYS);
        decimalsBox.setAlignment(Pos.CENTER_LEFT);
        decimalsBox.setPadding(new Insets(0,0,rowPadding,0));

        //Url

        Label urlIcon = new Label("  ");
        urlIcon.minWidth(40);

        Label urlText = new Label("Url");
        HBox.setHgrow(urlText,Priority.ALWAYS);
        urlText.setFont(App.txtFont);
        urlText.setPadding(new Insets(0,5,0,5));
        urlText.maxWidthProperty().bind(m_colWidth);

        TextField urlField = new TextField();
        HBox.setHgrow(urlField,Priority.ALWAYS);
        urlField.textProperty().bind(m_priceAmount.getCurrency().urlProperty());




        HBox urlFieldBox = new HBox(urlField);
        HBox.setHgrow(urlFieldBox,Priority.ALWAYS);
        urlFieldBox.setId("bodyBox");
        urlFieldBox.setAlignment(Pos.CENTER_LEFT);
        urlFieldBox.setMaxHeight(18);
  
        HBox urlBox = new HBox(urlIcon, urlText, urlFieldBox);
        HBox.setHgrow(urlBox,Priority.ALWAYS);
        urlBox.setAlignment(Pos.CENTER_LEFT);
        urlBox.setPadding(new Insets(0,0,rowPadding,0));
        //Image

        Label imageIcon = new Label("  ");
        imageIcon.minWidth(40);

        Label imageText = new Label("Image"); 
        HBox.setHgrow(imageText,Priority.ALWAYS);
        imageText.setFont(App.txtFont);
        imageText.setPadding(new Insets(0,5,0,5));
        imageText.maxWidthProperty().bind(m_colWidth);

        String defaultImgString = m_priceAmount.getCurrency().getImageString();

        TextField imageField = new TextField(defaultImgString.equals("/assets/unknown-unit.png") || defaultImgString.equals("/assets/unitErgo.png") ? "(default)" : defaultImgString);
        HBox.setHgrow(imageField,Priority.ALWAYS);
        imageField.setEditable(false);


        HBox imageFieldBox = new HBox(imageField);
        HBox.setHgrow(imageFieldBox,Priority.ALWAYS);
        imageFieldBox.setId("bodyBox");
        imageFieldBox.setAlignment(Pos.CENTER_LEFT);
        imageFieldBox.setMaxHeight(18);
  
        HBox imageBox = new HBox(imageIcon, imageText, imageFieldBox);
        HBox.setHgrow(imageBox,Priority.ALWAYS);
        imageBox.setAlignment(Pos.CENTER_LEFT);
        imageBox.setPadding(new Insets(0,0,rowPadding,0));

   

        VBox bodyBox = new VBox( imageBox, tokenIdBox, urlBox, descriptionBox, emissionAmountBox, decimalsBox);
        HBox.setHgrow(bodyBox,Priority.ALWAYS);
        bodyBox.setAlignment(Pos.CENTER_LEFT);

        HBox bodyPaddingBox = new HBox();
        HBox.setHgrow(bodyBox,Priority.ALWAYS);
        bodyBox.setAlignment(Pos.CENTER_LEFT);
        



        VBox layoutBox = new VBox(m_topBox, bodyPaddingBox);
        HBox.setHgrow(layoutBox, Priority.ALWAYS);
        layoutBox.setPrefHeight(18);
        layoutBox.setPadding(new Insets(1,0,1,0));

 
        

        

        toggleShowSubMenuBtn.setOnMouseClicked(e->{
            m_showSubMenuProperty.set(!m_showSubMenuProperty.get());
        });

        m_showSubMenuProperty.addListener((obs,oldval,newval)->{
            toggleShowSubMenuBtn.setText(newval ? "⏷" : "⏵");
            if(newval){
                if(!bodyPaddingBox.getChildren().contains(bodyBox)){
                    bodyPaddingBox.getChildren().add(bodyBox);
                }
            }else{
                if(bodyPaddingBox.getChildren().contains(bodyBox)){
                    bodyPaddingBox.getChildren().remove(bodyBox);
                }
            }
        });


       

          

        m_amountListener = (obs,oldval,newval)->{
            String amountString = newval + "";
            if(!amountField.getText().equals(amountString)){
                amountField.setText(amountString);
            }
            
        };


        
        m_priceAmount.amountProperty().addListener(m_amountListener);

        m_priceQuote.addListener((obs,oldval,newval)->{
            if(newval != null){
                if(m_quoteAmountBox == null){
                    addQuoteAmountBox();
                }
            }else{
                if(m_quoteAmountBox != null){
                    removeQuoteAmountBox();
                }
            }
        });
    
        m_quoteAmount.addListener((obs,oldval,newval)->{
            PriceQuote priceQuote = m_priceQuote.get();
            if(newval != null){
                if(m_quoteAmountBox == null){
                    addQuoteAmountBox();
                }
                m_quoteAmountField.setText(newval + "");
                m_quoteAmountSymbolText.setText(priceQuote != null ? priceQuote.getQuoteSymbol() : "");
            }else{
                if(m_quoteAmountBox != null){
                    removeQuoteAmountBox();
                }
            }
        });

        getChildren().add(layoutBox);
        setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(this,Priority.ALWAYS);
       
    }

   
    public void addQuoteAmountBox(){
        m_quoteAmountField = new TextField();
        m_quoteAmountField.setPrefWidth(100);
        m_quoteAmountField.setPadding(new Insets(0,5,0,5));
        m_quoteAmountField.setId("itemLbl");

        m_quoteAmountSymbolText = new Text();
        m_quoteAmountSymbolText.setFill(App.txtColor);
        m_quoteAmountSymbolText.setFont(App.smallFont);
        
        m_fieldBox = new HBox(m_quoteAmountField);
        m_fieldBox.setAlignment(Pos.CENTER_LEFT);
        

        m_quoteAmountBox = new VBox(m_fieldBox, m_quoteAmountSymbolText);
        m_quoteAmountBox.setAlignment(Pos.CENTER_RIGHT);
        m_quoteAmountBox.setId("bodyBox");
        m_quoteAmountBox.setPadding( new Insets(0,5,0,0));
        
        m_balanceFieldBox.getChildren().add(m_quoteAmountBox);
    }

    public void removeQuoteAmountBox(){
        m_balanceFieldBox.getChildren().remove(m_quoteAmountBox);
        m_quoteAmountBox.getChildren().clear();
        m_quoteAmountBox = null;
        m_quoteAmountField = null;
        m_quoteAmountSymbolText = null;
        m_fieldBox = null;
    }
   
    public long getTimeStamp(){
        return m_priceAmount.getTimeStamp();
    }

    public void setTimeStamp(long timeStamp){
        m_priceAmount.setTimeStamp(timeStamp);;
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
        return m_priceAmount.getTokenId();
    }

   


    public int getMinImageWidth(){
        return m_minImgWidth;
    }

    public void setMinImageWidth(int width){
        m_minImgWidth = width;
    }


  
    public PriceAmount getPriceAmount(){
        return m_priceAmount;
    }


    public void setQuoteAmount(BigDecimal amount){
        m_quoteAmount.set(amount);
    }

    public void setQuote(PriceQuote priceQuote, BigDecimal quoteAmount){
        m_priceQuote.set(quoteAmount != null ? priceQuote : null);
        m_quoteAmount.set(priceQuote != null ? quoteAmount : null);
    }

    public BigDecimal getQuoteAmount(){
        return m_quoteAmount.get();
    }

    public void shutdown(){
        
    }

}
