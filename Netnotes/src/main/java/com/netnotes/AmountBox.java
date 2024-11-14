package com.netnotes;


import java.math.BigDecimal;

import com.devskiller.friendly_id.FriendlyId;

import javafx.beans.property.SimpleBooleanProperty;
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


public class AmountBox extends HBox implements AmountBoxInterface {

    private long m_quoteTimeout = AddressesData.QUOTE_TIMEOUT;
    private PriceAmount m_priceAmount = null;
    private String m_id = null;

    private int m_minImgWidth = 250;
    private long m_timestamp = 0;
    private SimpleBooleanProperty m_showSubMenuProperty = new SimpleBooleanProperty(false);

    private ChangeListener<PriceQuote> m_priceQuoteListener = null;
    private ChangeListener<BigDecimal> m_amountListener = null;


    public AmountBox(){
        super();
        m_id = FriendlyId.createFriendlyId();
    }

 
    

    public AmountBox(PriceAmount priceAmount, Scene scene) {
        super();
        int rowPadding = 2;

        m_id = FriendlyId.createFriendlyId();
        m_priceAmount = priceAmount;
        
        TextField currencyName = new TextField(priceAmount.getCurrency().getName());
        HBox.setHgrow(currencyName,Priority.ALWAYS);
        currencyName.setMaxWidth(100);


        TextField amountField = new TextField();
        HBox.setHgrow(amountField, Priority.ALWAYS);
        amountField.setEditable(false);
        amountField.setAlignment(Pos.CENTER_LEFT);
        amountField.textProperty().bind(priceAmount.amountProperty().asString());
        

        ImageView currencyImageView = new ImageView();
        currencyImageView.setPreserveRatio(true);
        currencyImageView.setFitWidth(App.MENU_BAR_IMAGE_WIDTH);
        currencyImageView.setImage(m_priceAmount.getCurrency().getIcon());


        

        HBox balanceFieldBox = new HBox(amountField);
        HBox.setHgrow(balanceFieldBox,Priority.ALWAYS);
        balanceFieldBox.setId("bodyBox");
        balanceFieldBox.setAlignment(Pos.CENTER_LEFT);
        balanceFieldBox.setMaxHeight(18);

        //tokenId
        Label tokenIdIcon = new Label("  ");
        tokenIdIcon.setId("logoBtn");

        Label tokenIdText = new Label("Token Id"); 
        tokenIdText.setFont(App.txtFont);
        tokenIdText.setPadding(new Insets(0,5,0,5));
        tokenIdText.setMinWidth(100);

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
        descriptionIcon.setId("logoBtn");

        Label descriptionText = new Label("Description"); 
        descriptionText.setFont(App.titleFont);
        descriptionText.setPadding(new Insets(0,5,0,5));
        descriptionText.setMinWidth(100);

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
        emissionAmountIcon.setId("logoBtn");

        Label emissionAmountText = new Label("Emission"); 
        emissionAmountText.setFont(App.txtFont);
        emissionAmountText.setPadding(new Insets(0,5,0,5));
        emissionAmountText.setMinWidth(100);

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
        decimalsIcon.setId("logoBtn");

        Label decimalsText = new Label("Decimals"); 
        decimalsText.setFont(App.txtFont);
        decimalsText.setPadding(new Insets(0,5,0,5));
        decimalsText.setMinWidth(100);

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
        urlIcon.setId("logoBtn");

        Label urlText = new Label("Url"); 
        urlText.setFont(App.txtFont);
        urlText.setPadding(new Insets(0,5,0,5));
        urlText.setMinWidth(100);

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
        imageIcon.setId("logoBtn");

        Label imageText = new Label("Image"); 
        imageText.setFont(App.txtFont);
        imageText.setPadding(new Insets(0,5,0,5));
        imageText.setMinWidth(100);

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

        Label toggleShowSubMenuBtn = new Label(m_showSubMenuProperty.get() ? "⏷ " : "⏵ ");
        toggleShowSubMenuBtn.setId("caretBtn");
        toggleShowSubMenuBtn.setMinWidth(25);

        VBox bodyBox = new VBox( imageBox, tokenIdBox, urlBox, descriptionBox, emissionAmountBox, decimalsBox);
        HBox.setHgrow(bodyBox,Priority.ALWAYS);
        bodyBox.setAlignment(Pos.CENTER_LEFT);

        HBox bodyPaddingBox = new HBox();
        HBox.setHgrow(bodyBox,Priority.ALWAYS);
        bodyBox.setAlignment(Pos.CENTER_LEFT);
        

        HBox topBox = new HBox(toggleShowSubMenuBtn, currencyImageView, currencyName, balanceFieldBox);
        HBox.setHgrow(topBox, Priority.ALWAYS);
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setPadding(new Insets(0,10,1,0));

        VBox layoutBox = new VBox(topBox, bodyPaddingBox);
        HBox.setHgrow(layoutBox, Priority.ALWAYS);
        layoutBox.setPrefHeight(18);
        layoutBox.setPadding(new Insets(1,0,1,0));

 
        
      
        Runnable quoteUpdate = () ->{
           // updateBufferedImage(amountImageView);
        };
        

        toggleShowSubMenuBtn.setOnMouseClicked(e->{
            m_showSubMenuProperty.set(!m_showSubMenuProperty.get());
        });

        m_showSubMenuProperty.addListener((obs,oldval,newval)->{
            toggleShowSubMenuBtn.setText(newval ? "⏷ " : "⏵ ");
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
       
        
        Runnable addCurrentAmountListeners = ()->{
            PriceAmount currentAmount = m_priceAmount;

            m_amountListener = (obs,oldval,newval)->{
                if(!amountField.getText().equals(newval + "")){
                    amountField.setText(newval + "");
                }
                
            };

   
            m_priceQuoteListener = (obs, oldval, newval)-> quoteUpdate.run();
           
            currentAmount.amountProperty().addListener(m_amountListener);
           
            currentAmount.priceQuoteProperty().addListener(m_priceQuoteListener);

        };

        addCurrentAmountListeners.run();
        
    

        getChildren().add(layoutBox);
        setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(this,Priority.ALWAYS);
       
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

    public void shutdown(){
    

        if(m_amountListener != null){
            m_priceAmount.amountProperty().removeListener(m_amountListener);
            m_amountListener = null;
        }
       
        if(m_priceQuoteListener != null){
            m_priceAmount.priceQuoteProperty().removeListener(m_priceQuoteListener);
            m_priceQuoteListener = null;
        }
        
    }

}
