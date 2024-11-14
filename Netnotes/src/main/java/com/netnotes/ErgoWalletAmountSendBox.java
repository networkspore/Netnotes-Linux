package com.netnotes;


import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import com.devskiller.friendly_id.FriendlyId;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import com.google.gson.JsonObject;

public class ErgoWalletAmountSendBox extends HBox implements AmountBoxInterface {

    private long m_quoteTimeout = AddressesData.QUOTE_TIMEOUT;
    private PriceAmount m_balanceAmount = null;
    private String m_id = null;

    private int m_minImgWidth = 250;
    private long m_timestamp = 0;
    private SimpleBooleanProperty m_showSubMenuProperty = new SimpleBooleanProperty(false);

    private ChangeListener<PriceQuote> m_priceQuoteListener = null;
    private ChangeListener<BigDecimal> m_amountListener = null;
    
    private Button m_enterBtn;
    private TextField m_amountField;
    private HBox m_amountFieldBox;
    private int m_leftColWidth = 140;

    private JsonParametersBox m_paramsBox;

    public ErgoWalletAmountSendBox(){
        super();
        m_id = FriendlyId.createFriendlyId();
    }

    public void reset(){
        m_amountField.setText("");
    }

    public boolean isSendAmount(){
        if(m_amountField.getLength() > 0 ){
            return !(m_amountField.getLength() == 1 && m_amountField.getText().equals("0"));
        }
        return false;
    }

    public PriceAmount getSendAmount(){
        return isSendAmount() ? new PriceAmount( new BigDecimal(m_amountField.getText()), getPriceAmount().getCurrency())  : null;
    }

    public ErgoWalletAmountSendBox(PriceAmount balanceAmount, Scene scene) {
        super();
        m_timestamp = System.currentTimeMillis();

        m_id = FriendlyId.createFriendlyId();
        m_balanceAmount = balanceAmount;
        
        Label currencyName = new Label(m_balanceAmount.getCurrency().getName());
        currencyName.setId("passField");
        currencyName.setMaxWidth(m_leftColWidth -40);
        currencyName.setMinWidth(m_leftColWidth -40);
        HBox.setHgrow(currencyName,Priority.ALWAYS);
        currencyName.setPadding(new Insets(0,0,0,10));

        ImageView currencyImageView = new ImageView();
        currencyImageView.setPreserveRatio(true);
        currencyImageView.setFitWidth(App.MENU_BAR_IMAGE_WIDTH);
        currencyImageView.setImage(m_balanceAmount.getCurrency().getIcon());

        m_amountField = new TextField();
        m_amountField.setPromptText("0");
        HBox.setHgrow(m_amountField, Priority.ALWAYS);
        m_amountField.setAlignment(Pos.CENTER_LEFT);
        m_amountField.textProperty().addListener((obs,oldval,newval)->{
            PriceCurrency currency = m_balanceAmount.getCurrency();
            String number = newval.replaceAll("[^0-9.]", "");
            int index = number.indexOf(".");
            String leftSide = index != -1 ? number.substring(0, index + 1) : number;
            String rightSide = index != -1 ?  number.substring(index + 1) : "";
            rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
            rightSide = rightSide.length() > currency.getDecimals() ? rightSide.substring(0, currency.getDecimals()) : rightSide;
            m_amountField.setText(leftSide +  rightSide);
        });

        m_enterBtn = new Button("[enter]"); 
        m_enterBtn.setMinHeight(15);
        m_enterBtn.setId("toolBtn");
        m_enterBtn.setPadding(new Insets(0,5,0,5));
    

        m_amountField.setOnAction(e->m_enterBtn.fire());

        m_amountFieldBox = new HBox(m_amountField);
        HBox.setHgrow(m_amountFieldBox,Priority.ALWAYS);
        m_amountFieldBox.setAlignment(Pos.CENTER_LEFT);
        m_amountFieldBox.setMaxHeight(18);
        m_amountFieldBox.setId("bodyBox");
        m_amountField.focusedProperty().addListener((obs,oldval,newval)->{
            if(newval){
                if(!m_amountFieldBox.getChildren().contains(m_enterBtn)){
                    m_amountFieldBox.getChildren().add(m_enterBtn);
                }
            }else{
                if(m_amountFieldBox.getChildren().contains(m_enterBtn)){
                    m_amountFieldBox.getChildren().remove(m_enterBtn);
                }
            }
        });

        m_enterBtn.setOnAction(e->m_enterBtn.requestFocus());

        Label toggleShowSubMenuBtn = new Label(m_showSubMenuProperty.get() ? "⏷ " : "⏵ ");
        toggleShowSubMenuBtn.setId("caretBtn");
        toggleShowSubMenuBtn.setMinWidth(25);



        m_paramsBox = new JsonParametersBox((JsonObject) null, m_leftColWidth);
        HBox.setHgrow(m_paramsBox, Priority.ALWAYS);
        m_paramsBox.setPadding(new Insets(2,10,0,10));
        m_paramsBox.updateParameters(m_balanceAmount.getJsonObject());
        
        HBox bodyPaddingBox = new HBox();
        HBox.setHgrow(m_paramsBox,Priority.ALWAYS);
        

        HBox topBox = new HBox(toggleShowSubMenuBtn, currencyImageView, currencyName, m_amountFieldBox);
        HBox.setHgrow(topBox, Priority.ALWAYS);
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setPadding(new Insets(0,10,1,0));

        VBox layoutBox = new VBox(topBox, bodyPaddingBox);
        HBox.setHgrow(layoutBox, Priority.ALWAYS);
        layoutBox.setPrefHeight(18);
        layoutBox.setPadding(new Insets(1,0,1,0));

 
        

        

        toggleShowSubMenuBtn.setOnMouseClicked(e->{
            m_showSubMenuProperty.set(!m_showSubMenuProperty.get());
        });

        m_showSubMenuProperty.addListener((obs,oldval,newval)->{
            toggleShowSubMenuBtn.setText(newval ? "⏷ " : "⏵ ");
            if(newval){
                if(!bodyPaddingBox.getChildren().contains(m_paramsBox)){
                    bodyPaddingBox.getChildren().add(m_paramsBox);
                }
            }else{
                if(bodyPaddingBox.getChildren().contains(m_paramsBox)){
                    bodyPaddingBox.getChildren().remove(m_paramsBox);
                }
            }
        });
       
        updateParamsBox();
        
        Runnable addCurrentAmountListeners = ()->{
            PriceAmount currentAmount = m_balanceAmount;

          //  m_amountListener = (obs,oldval,newval)->updateParamsBox();
            m_priceQuoteListener = (obs, oldval, newval)->updateQuote(newval);
            
           // currentAmount.amountProperty().addListener(m_amountListener);
           
            currentAmount.priceQuoteProperty().addListener(m_priceQuoteListener);

        };

        addCurrentAmountListeners.run();
        
    

        getChildren().add(layoutBox);
        setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(this,Priority.ALWAYS);
       
    }

    public void updateParamsBox(){
 
        JsonObject json = new JsonObject();
        json.addProperty("balance", m_balanceAmount.getAmountString());
        if(m_balanceAmount.priceQuoteProperty().get() != null){
            json.addProperty("marketPrice", m_balanceAmount.priceQuoteProperty().get().getAmountString());
        }
        json.addProperty("timeStamp", m_balanceAmount.getTimeStamp());
        json.add("details", m_balanceAmount.getCurrency().getJsonObject());

        m_paramsBox.updateParameters(json);
    }



    public void updateQuote(PriceQuote priceQuote){
        updateParamsBox();
    }

    public void setBalance(long timeStamp, long balance){
        m_timestamp = timeStamp;
        m_balanceAmount.setLongAmount(balance, timeStamp);
      
        updateParamsBox();
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
        return m_balanceAmount.getTokenId();
    }

   


    public int getMinImageWidth(){
        return m_minImgWidth;
    }

    public void setMinImageWidth(int width){
        m_minImgWidth = width;
    }


  
    public PriceAmount getPriceAmount(){
        return m_balanceAmount;
    }

    public void shutdown(){
    

        if(m_amountListener != null){
            m_balanceAmount.amountProperty().removeListener(m_amountListener);
            m_amountListener = null;
        }
       
        if(m_priceQuoteListener != null){
            m_balanceAmount.priceQuoteProperty().removeListener(m_priceQuoteListener);
            m_priceQuoteListener = null;
        }
        
    }

}
