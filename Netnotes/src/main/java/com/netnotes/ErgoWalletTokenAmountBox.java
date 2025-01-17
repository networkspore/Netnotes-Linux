package com.netnotes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonObject;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.text.Text;


public class ErgoWalletTokenAmountBox extends HBox implements AmountBoxInterface {

    private long m_quoteTimeout = AddressesData.QUOTE_TIMEOUT;
    private PriceAmount m_priceAmount = null;
    private String m_id = null;

    private int m_minImgWidth = 250;
    private SimpleBooleanProperty m_showSubMenuProperty = new SimpleBooleanProperty(false);

    private ChangeListener<BigDecimal> m_amountListener = null;
    private SimpleDoubleProperty m_colWidth = new SimpleDoubleProperty(160);

    private SimpleObjectProperty<PriceQuote> m_priceQuote = new SimpleObjectProperty<>(null);
    private SimpleObjectProperty<BigDecimal> m_ergQuoteAmount = new SimpleObjectProperty<>(null);
    private SimpleObjectProperty<BigDecimal> m_stableQuoteAmount = new SimpleObjectProperty<>(null);

    private Text m_quoteAmountSymbolText = null;
    private TextField m_quoteAmountField = null;
    private VBox m_quoteAmountBox = null;
    private HBox m_fieldBox = null;
    private HBox m_topBox;
    private HBox m_balanceFieldBox;

    private SimpleBooleanProperty m_toggleShowPriceQuote = new SimpleBooleanProperty(false);

    private Runnable m_toggleShowPriceQuoteRunnable = null;
    private Button m_togglePriceQuoteBtn = null;
    private TextField m_priceQuoteField = null;
    private HBox m_priceQuoteFieldBox = null;
    private Label m_priceQuoteLabel = null;
    private HBox m_priceQuoteHeadingBox = null;
    private JsonParametersBox m_priceQuoteParametersBox = null;
    private VBox m_priceQuoteVBox = null;
    private ChangeListener<Boolean> m_togglePriceQuoteListener = null;
    private ChangeListener<PriceQuote> m_priceQuoteChangeListener = null;
    private ChangeListener<PriceQuote> m_priceQuoteHeadingChangeListener = null;

    private JsonParametersBox m_currencyParamsBox = null;


    private VBox m_bodyBox;

    public ErgoWalletTokenAmountBox(){
        super();
        m_id = FriendlyId.createFriendlyId();
    }

    public ErgoWalletTokenAmountBox(PriceAmount priceAmount, Scene scene) {
        super();
      //  int rowPadding = 2;

        m_id = FriendlyId.createFriendlyId();
        m_priceAmount = priceAmount;

        Label toggleShowSubMenuBtn = new Label(m_showSubMenuProperty.get() ? "⏷" : "⏵");
        toggleShowSubMenuBtn.setId("caretBtn");
        toggleShowSubMenuBtn.setMinWidth(25);
        
        TextField currencyName = new TextField(priceAmount.getCurrency().getName());
        HBox.setHgrow(currencyName,Priority.ALWAYS);
        currencyName.minWidthProperty().bind(m_colWidth);
        currencyName.maxWidthProperty().bind(m_colWidth);
        currencyName.setEditable(false);

        TextField amountField = new TextField();
        HBox.setHgrow(amountField, Priority.ALWAYS);
        amountField.setEditable(false);
        amountField.setAlignment(Pos.CENTER_LEFT);
        amountField.textProperty().bind(priceAmount.amountProperty().asString());
        amountField.setPadding(new Insets(8,0,8,5));

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


   

        m_bodyBox = new VBox();
        HBox.setHgrow(m_bodyBox,Priority.ALWAYS);
        m_bodyBox.setAlignment(Pos.CENTER_LEFT);

        HBox bodyPaddingBox = new HBox();
        HBox.setHgrow(m_bodyBox,Priority.ALWAYS);
        m_bodyBox.setAlignment(Pos.CENTER_LEFT);
        



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
                addCurrencyBox();
                if(!bodyPaddingBox.getChildren().contains(m_bodyBox)){
                    bodyPaddingBox.getChildren().add(m_bodyBox);
                }
            }else{
                removeCurrencyBox();
                if(bodyPaddingBox.getChildren().contains(m_bodyBox)){
                    bodyPaddingBox.getChildren().remove(m_bodyBox);
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
                    
                    addPriceQuoteBox();
                }
            }else{
                if(m_quoteAmountBox != null){
                    removeQuoteAmountBox();
                
                    removePriceQuoteBox();
                }
            }
        });
        getChildren().add(layoutBox);
        setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(this,Priority.ALWAYS);
       
    }

    
    public void addCurrencyBox(){
        if(m_currencyParamsBox == null){
            m_currencyParamsBox = new JsonParametersBox((JsonObject) null, (int) m_colWidth.get() + 20);
            m_currencyParamsBox.setPadding(new Insets(0,0,0,5));
        
                PriceCurrency currency = m_priceAmount.getCurrency();
                
                JsonObject infoJson = new JsonObject();
                JsonObject currencyJson = currency.getJsonObject();
                currencyJson.remove("imageString");
                infoJson.add("info", currencyJson);
                m_currencyParamsBox.updateParameters(infoJson);
            



            m_bodyBox.getChildren().add(0, m_currencyParamsBox);
        }
    }

    public void removeCurrencyBox(){
        if(m_currencyParamsBox != null){
           

            if(m_bodyBox.getChildren().contains(m_currencyParamsBox)){
                m_bodyBox.getChildren().remove(m_currencyParamsBox);
            }
            m_currencyParamsBox.shutdown();
            m_currencyParamsBox = null;
        }
    }


    public void addPriceQuoteBox(){
        if(m_priceQuoteVBox == null){
            m_togglePriceQuoteBtn = new Button(m_toggleShowPriceQuote.get() ? "⏷" : "⏵");
            m_togglePriceQuoteBtn.setId("caretBtn");
            m_togglePriceQuoteBtn.setOnAction(e->{
                m_toggleShowPriceQuote.set(!m_toggleShowPriceQuote.get());
            });

            m_priceQuoteLabel = new Label("Price Quote");
            m_priceQuoteLabel.maxWidthProperty().bind(m_colWidth.add(10));
            m_priceQuoteLabel.minWidthProperty().bind(m_colWidth.add(10));

            m_priceQuoteField = new TextField(m_priceQuote.get() != null ? m_priceQuote.get().getAmountString() + " " + m_priceQuote.get().getQuoteSymbol() : "");
            HBox.setHgrow(m_priceQuoteField, Priority.ALWAYS);
            m_priceQuoteField.setEditable(false);
            
            m_priceQuoteFieldBox = new HBox(m_priceQuoteField);
            m_priceQuoteFieldBox.setId("bodyBox");
            HBox.setHgrow(m_priceQuoteFieldBox, Priority.ALWAYS);

            m_priceQuoteHeadingBox = new HBox(m_togglePriceQuoteBtn, m_priceQuoteLabel, m_priceQuoteFieldBox);
            m_priceQuoteHeadingBox.setPadding(new Insets(0,0,0,5));
            HBox.setHgrow(m_priceQuoteHeadingBox, Priority.ALWAYS);

            m_priceQuoteVBox = new VBox(m_priceQuoteHeadingBox);
            m_priceQuoteVBox.setPadding(new Insets(2,0,2,0));
        
            m_priceQuoteParametersBox = new JsonParametersBox((JsonObject) null, (int) m_colWidth.get() + 20);
            m_priceQuoteParametersBox.setPadding(new Insets(5,0,0,15));

            m_priceQuoteHeadingChangeListener = (obs,oldval,newval)->{
                if(m_priceQuoteField != null){
                    m_priceQuoteField.setText(newval != null ? newval.getAmountString() + " " + newval.getQuoteSymbol() : "");
                }
            };

            m_priceQuote.addListener(m_priceQuoteHeadingChangeListener);

            m_toggleShowPriceQuoteRunnable = ()->{
                boolean isShow = m_toggleShowPriceQuote.get();
            
                m_togglePriceQuoteBtn.setText(isShow? "⏷" : "⏵");

                if(isShow){
          
                    if(!m_priceQuoteVBox.getChildren().contains(m_priceQuoteParametersBox)){
                        m_priceQuoteVBox.getChildren().add(m_priceQuoteParametersBox);
                    }
                    if(m_priceQuoteChangeListener == null){
                        PriceQuote priceQuote = m_priceQuote.get();
                       
                        m_priceQuoteParametersBox.updateParameters(priceQuote != null ? priceQuote.getJsonObject() : null);
                        
                        m_priceQuoteChangeListener = (obs,oldval,newval) ->{
                            m_priceQuoteParametersBox.updateParameters(newval != null ? newval.getJsonObject() : null);
                        };
                        m_priceQuote.addListener(m_priceQuoteChangeListener);

                    }
                }else{

                    if(m_priceQuoteVBox.getChildren().contains(m_priceQuoteParametersBox)){
                        m_priceQuoteVBox.getChildren().remove(m_priceQuoteParametersBox);
                    }
                    if(m_priceQuoteChangeListener != null){
                        m_priceQuote.removeListener(m_priceQuoteChangeListener);
                        m_priceQuoteParametersBox.shutdown();
                        m_priceQuoteChangeListener = null;
                    }
                }
            };

            m_toggleShowPriceQuoteRunnable.run();

            m_togglePriceQuoteListener = (obs,oldval,newval)->m_toggleShowPriceQuoteRunnable.run();
            m_toggleShowPriceQuote.addListener(m_togglePriceQuoteListener);
           
            m_bodyBox.getChildren().add(m_priceQuoteVBox);
            
        }
    }

    public void removePriceQuoteBox(){
        if(m_priceQuoteVBox != null){
            
            m_priceQuote.removeListener(m_priceQuoteHeadingChangeListener);
            m_priceQuoteHeadingChangeListener = null;
            if(m_priceQuoteChangeListener != null){
                m_priceQuote.removeListener(m_priceQuoteChangeListener);
                m_priceQuoteChangeListener = null;
            }
            m_bodyBox.getChildren().remove(m_priceQuoteVBox);
            m_priceQuoteVBox.getChildren().clear();
            m_priceQuoteFieldBox.getChildren().clear();
            m_priceQuoteHeadingBox.getChildren().clear();
            m_toggleShowPriceQuote.removeListener(m_togglePriceQuoteListener);
            m_priceQuoteParametersBox.shutdown();


            m_toggleShowPriceQuoteRunnable = null;
            m_togglePriceQuoteBtn = null;
            m_priceQuoteField = null;
            m_priceQuoteFieldBox = null;
            m_priceQuoteLabel = null;
            m_priceQuoteHeadingBox = null;
            m_priceQuoteParametersBox = null;
            m_priceQuoteVBox = null;
            m_togglePriceQuoteListener = null;
            
        }
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
        m_balanceFieldBox.getChildren().add(m_quoteAmountBox);
        m_quoteAmountBox.setPadding( new Insets(0,5,0,0));
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

    public PriceQuote getPriceQuote() {
        return m_priceQuote.get();
    }


    public void setErgQuoteAmount(BigDecimal amount){
        m_ergQuoteAmount.set(amount);
    }

    public BigDecimal getErgQuoteAmount(){
        return m_ergQuoteAmount.get();
    }

    

    public void setStableQuoteAmount(BigDecimal stableQuoteAmount){
        m_stableQuoteAmount.set(stableQuoteAmount);
    }


    
    public void setQuote(PriceQuote priceQuote, BigDecimal ergQuoteAmount, BigDecimal stableQuoteAmount, String stableQuoteSymbol){
        m_priceQuote.set(ergQuoteAmount != null ? priceQuote : null);
        m_ergQuoteAmount.set(priceQuote != null && ergQuoteAmount != null ?  ergQuoteAmount : null);
        m_stableQuoteAmount.set(ergQuoteAmount != null && stableQuoteAmount != null ? stableQuoteAmount : null);

        if(priceQuote != null && m_quoteAmountSymbolText != null && m_quoteAmountField != null){
            String quoteAmountString = stableQuoteAmount != null ? (stableQuoteAmount + "") : (ergQuoteAmount != null ? ergQuoteAmount + "" : "");
            String quoteTextString = stableQuoteAmount != null ? (stableQuoteSymbol != null ? stableQuoteSymbol : "") : (ergQuoteAmount != null ? (priceQuote != null ? priceQuote.getQuoteSymbol() : "") : "");
            if(!m_quoteAmountField.getText().equals(quoteAmountString)){
                m_quoteAmountField.setText(quoteAmountString);
            }
            if(!quoteTextString.equals(m_quoteAmountSymbolText.getText())){
                m_quoteAmountSymbolText.setText(quoteTextString);
            }
        }
    }


    public void shutdown(){
        
    }

}
