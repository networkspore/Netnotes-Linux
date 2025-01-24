package com.netnotes;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
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

public class ErgoWalletAmountSendTokenBox extends HBox implements AmountBoxInterface {

    private long m_quoteTimeout = AddressesData.QUOTE_TIMEOUT;
    private PriceAmount m_balanceAmount = null;

    private String m_id = null;

    private int m_minImgWidth = 250;
    private SimpleBooleanProperty m_showSubMenuProperty = new SimpleBooleanProperty(false);
    private SimpleObjectProperty<PriceQuote> m_tokenQuote = new SimpleObjectProperty<>(null);
    private SimpleObjectProperty<PriceQuote> m_ergoQuote = new SimpleObjectProperty<>(null);
    private ChangeListener<BigDecimal> m_amountListener = null;
    private SimpleObjectProperty<BigDecimal> m_ergQuoteBalanceAmount = new SimpleObjectProperty<>(null);
    private SimpleObjectProperty<BigDecimal> m_stableQuoteBalanceAmount = new SimpleObjectProperty<>(null);
    private TextField m_sendAmountField;
    private int m_leftColWidth = 140;
    private int m_botRowPadding = 3;
    private SimpleObjectProperty<BigDecimal> m_sendAmountProperty = new SimpleObjectProperty<>(null);
    private Binding<String> m_amountBinding = null;
    private ErgoWalletAmountSendBoxes m_sendBoxes;
    private SimpleBooleanProperty m_isFieldFocused = new SimpleBooleanProperty(false);
    private ChangeListener<String> m_sendAmountFieldTextChangeListener;

    private SimpleBooleanProperty m_toggleShowPriceQuote = new SimpleBooleanProperty(false);

    
    private SimpleDoubleProperty m_colWidth = new SimpleDoubleProperty(160);
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
    private Binding<String> m_priceQuoteInfoHeadingBinding = null;
    private Binding<Double> m_quoteAmountFieldBinding = null;
    
    private Text m_quoteAmountSymbolText = null;
    private TextField m_quoteAmountField = null;
    private VBox m_quoteAmountBox = null;
    private HBox m_fieldBox = null;
    private HBox m_balanceFieldBox = null;
    private VBox m_bodyBox = null;
    private double m_charWidth = 10;
    private JsonParametersBox m_currencyParamsBox = null;


    public void reset() {
        m_sendAmountProperty.set(null);
    }

    public boolean isSendAmount() {
        BigDecimal sendAmount = m_sendAmountProperty.get();
        return sendAmount != null && sendAmount.compareTo(BigDecimal.ZERO) == 1; 
    }

   

    

    public ErgoWalletAmountSendTokenBox(ErgoWalletAmountSendBoxes sendBoxes, PriceAmount balanceAmount, Scene scene) {
        super();
        m_sendBoxes = sendBoxes;
        m_id = FriendlyId.createFriendlyId();
        m_balanceAmount = balanceAmount;
        final PriceCurrency currency = m_balanceAmount.getCurrency();
        final int decimals = currency.getDecimals();

        m_charWidth = Utils.computeTextWidth(App.txtFont, " ");

        TextField currencyName = new TextField(currency.getName());
        HBox.setHgrow(currencyName, Priority.ALWAYS);
        currencyName.setMaxWidth(m_leftColWidth);
        currencyName.setMinWidth(m_leftColWidth);
        currencyName.setEditable(false);

        String textFieldId = balanceAmount.getCurrency().getName() + "sendBox";

        m_sendAmountField = new TextField();
        HBox.setHgrow(m_sendAmountField, Priority.ALWAYS);
        m_sendAmountField.setAlignment(Pos.CENTER_LEFT);
        m_sendAmountField.setUserData(textFieldId);

        m_amountBinding = Bindings.createObjectBinding(()->{
            BigDecimal amount = m_sendAmountProperty.get();
            return amount == null ? "" : amount.toPlainString();
        },m_sendAmountProperty);

        m_sendAmountField.textProperty().bind(m_amountBinding);
    
        m_sendAmountFieldTextChangeListener = (obs,oldval,newval)->{
            if(m_sendAmountField != null){
                
                String number = newval.replaceAll("[^0-9.]", "");
                int index = number.indexOf(".");
                String leftSide = index != -1 ? number.substring(0, index + 1) : number;
                String rightSide = index != -1 ?  number.substring(index + 1) : "";
                rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
                rightSide = rightSide.length() > decimals ? rightSide.substring(0, decimals) : rightSide;
            
                String amountString = leftSide +  rightSide;
                m_sendAmountField.setText(amountString);
                boolean isTextZero = Utils.isTextZero(amountString);
                m_sendAmountProperty.set(isTextZero ? null : new BigDecimal(Utils.formatStringToNumber(amountString, decimals)));
            }
        };



        ImageView currencyImageView = new ImageView();
        currencyImageView.setPreserveRatio(true);
        currencyImageView.setFitWidth(App.MENU_BAR_IMAGE_WIDTH);
        currencyImageView.setImage(currency.getIcon());

        Button maxBtn = new Button("MAX");
        maxBtn.setFocusTraversable(true);

        HBox amountFieldBox = new HBox(m_sendAmountField);
        HBox.setHgrow(amountFieldBox, Priority.ALWAYS);
        amountFieldBox.setId("bodyBox");
        amountFieldBox.setAlignment(Pos.CENTER_LEFT);
        amountFieldBox.setMaxHeight(18);

        m_balanceFieldBox = new HBox(amountFieldBox, maxBtn);
        HBox.setHgrow(m_balanceFieldBox,Priority.ALWAYS);
        m_balanceFieldBox.setId("bodyBox");
        m_balanceFieldBox.setAlignment(Pos.CENTER_LEFT);

       
        Button toggleShowSubMenuBtn = new Button(m_showSubMenuProperty.get() ? "⏷" : "⏵");
        toggleShowSubMenuBtn.setId("caretBtn");
        toggleShowSubMenuBtn.setMinWidth(25);
        toggleShowSubMenuBtn.setOnAction(e->{
            m_showSubMenuProperty.set(!m_showSubMenuProperty.get());
        });

        maxBtn.setOnAction(e -> {
            BigDecimal balance = m_balanceAmount.amountProperty().get();
            BigDecimal fee = getTokenFeeBigDecimal();
            BigDecimal availableBalance = balance.subtract(fee);
            m_sendAmountProperty.set(availableBalance.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : availableBalance);
        });

        m_sendAmountProperty.addListener((obs,oldval,newval)->{
            updateSendAmountQuote();
            m_sendBoxes.updateWarnings();
        });


     

        HBox topBox = new HBox(toggleShowSubMenuBtn, currencyImageView, currencyName, m_balanceFieldBox);
        HBox.setHgrow(topBox, Priority.ALWAYS);
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setPadding(new Insets(0, 10, m_botRowPadding, 0));

        m_bodyBox = new VBox( );
        HBox.setHgrow(m_bodyBox,Priority.ALWAYS);
        m_bodyBox.setAlignment(Pos.CENTER_LEFT);
        m_bodyBox.setPadding(new Insets(0,10,5,0));

        HBox bodyPaddingBox = new HBox();
        HBox.setHgrow(m_bodyBox,Priority.ALWAYS);
        m_bodyBox.setAlignment(Pos.CENTER_LEFT);
        

        VBox layoutBox = new VBox(topBox, bodyPaddingBox);
        HBox.setHgrow(layoutBox, Priority.ALWAYS);
        layoutBox.setPrefHeight(18);
        layoutBox.setPadding(new Insets(1, 0, 1, 0));



        getChildren().add(layoutBox);
        setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(this, Priority.ALWAYS);

        scene.focusOwnerProperty().addListener((obs, old, newPropertyValue) -> {
            if (newPropertyValue != null && newPropertyValue instanceof TextField) {
                TextField focusedField = (TextField) newPropertyValue;
                Object userData = focusedField.getUserData();
                if (userData != null && userData instanceof String) {
                    String userDataString = (String) userData;
                    if (userDataString.equals(textFieldId)) {
                        m_isFieldFocused.set(true);
                        
                    } else {
                        if (m_isFieldFocused.get()) {
                            m_isFieldFocused.set(false);
                        }

                    }
                } else {
                    if (m_isFieldFocused.get()) {
                        m_isFieldFocused.set(false);
                    }

                }
            } else {
                if (m_isFieldFocused.get()) {
                    m_isFieldFocused.set(false);
                }

            }
        });

        m_isFieldFocused.addListener((obs,oldval,newval)->{
            if(newval){
                m_sendAmountField.textProperty().unbind();
                if(m_sendAmountField.getText().equals("0")){
                    m_sendAmountField.setText("");
                }
                m_sendAmountField.textProperty().addListener(m_sendAmountFieldTextChangeListener);
            }else{
                m_sendAmountField.textProperty().removeListener(m_sendAmountFieldTextChangeListener);
                m_sendAmountProperty.set(Utils.isTextZero(m_sendAmountField.getText()) ? null : new BigDecimal(Utils.formatStringToNumber(m_sendAmountField.getText(), decimals)));
                m_sendAmountField.textProperty().bind(m_amountBinding);
            }
        });

        
        m_tokenQuote.addListener((obs,oldval,newval)->{
            BigDecimal sendAmount = m_sendAmountProperty.get();
           
            if(newval != null  && sendAmount != null && sendAmount.compareTo(BigDecimal.ZERO) > 0){
               
                addQuoteAmountBox();
                
    
            }else{
                
                removeQuoteAmountBox();
           
            }
            if(newval != null){
                addPriceQuoteBox();
    
            }else{
                removePriceQuoteBox();
            }
        });
    


        m_showSubMenuProperty.addListener((obs,oldval,newval)->{
            toggleShowSubMenuBtn.setText(newval ? "⏷" : "⏵");
            if(newval){
                addCurrencyBox();
                if(!bodyPaddingBox.getChildren().contains(m_bodyBox)){
                    bodyPaddingBox.getChildren().add(m_bodyBox);
                }
            }else{
                if(bodyPaddingBox.getChildren().contains(m_bodyBox)){
                    bodyPaddingBox.getChildren().remove(m_bodyBox);
                }
                removeCurrencyBox();
            }
        });
    }

    
    
      
    public void addCurrencyBox(){
        if(m_currencyParamsBox == null){
            m_currencyParamsBox = new JsonParametersBox((JsonObject) null, (int) m_colWidth.get() + 20);
            m_currencyParamsBox.setPadding(new Insets(0,0,0,5));
        
                PriceCurrency currency = m_balanceAmount.getCurrency();
                
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


    public void removeQuoteAmountBox(){
        if(m_quoteAmountBox != null){
            m_balanceFieldBox.getChildren().remove(m_quoteAmountBox);
            m_quoteAmountBox.getChildren().clear();
            
            
            m_quoteAmountField.prefWidthProperty().unbind();
            m_quoteAmountFieldBinding = null;
            m_quoteAmountBox = null;
            m_quoteAmountField = null;
            m_quoteAmountSymbolText = null;
            m_fieldBox = null;
        }
    }
    private final int quoteDecimalScale = ErgoNetwork.MIN_NETWORK_FEE.scale();
    public void addQuoteAmountBox(){

        if(m_quoteAmountBox == null){
            m_quoteAmountField = new TextField();
            m_quoteAmountField.setEditable(false);
           
            m_quoteAmountField.setPadding(new Insets(0,5,0,5));
            m_quoteAmountField.setId("itemLbl");
            
            m_quoteAmountFieldBinding = Bindings.createObjectBinding(()->{
                String number = m_quoteAmountField.textProperty().get();
                int index = number.indexOf(".");
               
                String leftSide = index != -1 ? number.substring(0, index + 1) : number;
                String rightSide = index != -1 ?  number.substring(index + 1) : "";
                rightSide = rightSide.length() > quoteDecimalScale ? rightSide.substring(0, quoteDecimalScale) : rightSide;

                double width = (leftSide + rightSide).length() * m_charWidth;
                return width < 30 ? 30 : width + 10;
            }, m_quoteAmountField.textProperty());
            
            m_quoteAmountField.prefWidthProperty().bind(m_quoteAmountFieldBinding);
            m_quoteAmountSymbolText = new Text();
            m_quoteAmountSymbolText.setFill(App.txtColor);
            m_quoteAmountSymbolText.setFont(App.smallFont);
            
            m_fieldBox = new HBox(m_quoteAmountField);
            m_fieldBox.setAlignment(Pos.CENTER_LEFT);
            

            m_quoteAmountBox = new VBox(m_fieldBox, m_quoteAmountSymbolText);
            m_quoteAmountBox.setAlignment(Pos.CENTER_RIGHT);
            m_quoteAmountBox.setId("bodyBox");
            m_quoteAmountBox.setPadding( new Insets(0,5,0,0));

            m_balanceFieldBox.getChildren().add(1, m_quoteAmountBox);
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

            m_priceQuoteField = new TextField(m_tokenQuote.get() != null ? m_tokenQuote.get().getAmountString() + " " + m_tokenQuote.get().getQuoteSymbol() : "");
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
            m_priceQuoteParametersBox.setPadding(new Insets(5,10,0,15));

            m_priceQuoteInfoHeadingBinding = Bindings.createObjectBinding(()->{
                PriceQuote tokenQuote = m_tokenQuote.get();
                PriceQuote ergoQuote = m_ergoQuote.get();

                return ergoQuote != null && tokenQuote != null ? ergoQuote.getAmountString() + " " + ergoQuote.getQuoteSymbol() : (tokenQuote != null ?  tokenQuote.getAmountString() + " " + tokenQuote.getQuoteSymbol(): "");
            }, m_tokenQuote, m_ergoQuote); 

            m_priceQuoteField.textProperty().bind(m_priceQuoteInfoHeadingBinding);
        
            m_toggleShowPriceQuoteRunnable = ()->{
                boolean isShow = m_toggleShowPriceQuote.get();
            
                m_togglePriceQuoteBtn.setText(isShow? "⏷" : "⏵");

                if(isShow){
          
                    if(!m_priceQuoteVBox.getChildren().contains(m_priceQuoteParametersBox)){
                        m_priceQuoteVBox.getChildren().add(m_priceQuoteParametersBox);
                    }
                    if(m_priceQuoteChangeListener == null){
                        PriceQuote priceQuote = m_tokenQuote.get();
                       
                        m_priceQuoteParametersBox.updateParameters(priceQuote != null ? priceQuote.getJsonObject() : null);
                        
                        m_priceQuoteChangeListener = (obs,oldval,newval) ->{
                            m_priceQuoteParametersBox.updateParameters(newval != null ? newval.getJsonObject() : null);
                        };
                        m_tokenQuote.addListener(m_priceQuoteChangeListener);

                    }
                }else{

                    if(m_priceQuoteVBox.getChildren().contains(m_priceQuoteParametersBox)){
                        m_priceQuoteVBox.getChildren().remove(m_priceQuoteParametersBox);
                    }
                    if(m_priceQuoteChangeListener != null){
                        m_tokenQuote.removeListener(m_priceQuoteChangeListener);
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
            
            m_priceQuoteField.textProperty().unbind();
            m_priceQuoteInfoHeadingBinding = null;
            if(m_priceQuoteChangeListener != null){
                m_tokenQuote.removeListener(m_priceQuoteChangeListener);
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

    public SimpleBooleanProperty isFieldFocused() {
        return m_isFieldFocused;
    }

    public BigDecimal getTokenFeeBigDecimal(){
        PriceAmount feePriceAmount = m_sendBoxes.feeAmountProperty().get(); 
        return feePriceAmount != null && feePriceAmount.getTokenId().equals(getTokenId()) ? feePriceAmount.getBigDecimalAmount() : BigDecimal.ZERO;
    }

    public PriceAmount getSendAmount() {
        BigDecimal sendAmount = m_sendAmountProperty.get();
        return sendAmount != null && sendAmount.compareTo(BigDecimal.ZERO) > 0 ? new PriceAmount(sendAmount, getCurrency()) : null;
    }

    public boolean isSufficientBalance(){
        BigDecimal remaining = getBalanceRemaining();

        return remaining.compareTo(BigDecimal.ZERO) > -1;
    }

    public ReadOnlyObjectProperty<BigDecimal> sendAmountProperty(){
        return m_sendAmountProperty;
    }

    public PriceCurrency getCurrency(){
        return getBalanceAmount().getCurrency();
    }

    public BigDecimal getBalanceRemaining(){
        PriceAmount sendAmount = getSendAmount();
        BigDecimal sendDecimal = sendAmount == null ? BigDecimal.ZERO : sendAmount.getBigDecimalAmount();
        BigDecimal feeDecimal = getTokenFeeBigDecimal();
        BigDecimal balanceDecimal = getBalanceAmount().getBigDecimalAmount();

        return balanceDecimal.subtract(sendDecimal.add(feeDecimal));
    }


    public void setQuote(PriceQuote tokenQuote, PriceQuote ergoQuote, BigDecimal ergQuoteAmount, BigDecimal stableQuoteAmount, String stableQuoteSymbol){
       
        m_ergoQuote.set(ergoQuote);
        m_ergQuoteBalanceAmount.set(tokenQuote != null && ergQuoteAmount != null ?  ergQuoteAmount : null);
        m_stableQuoteBalanceAmount.set(ergQuoteAmount != null && stableQuoteAmount != null ? stableQuoteAmount : null);
        m_tokenQuote.set(tokenQuote);
        updateSendAmountQuote();
    }

    private void updateSendAmountQuote(){
        BigDecimal sendAmount =  m_sendAmountProperty.get();
        PriceQuote tokenQuote = m_tokenQuote.get();
        PriceQuote ergoQuote = m_ergoQuote.get();
       
        if(tokenQuote != null && sendAmount != null && sendAmount.compareTo(BigDecimal.ZERO) > 0){
          //  String quoteAmountString = stableQuoteAmount != null ? (stableQuoteAmount + "") : (ergQuoteAmount != null ? ergQuoteAmount + "" : "");
            boolean isErgoQuote = ergoQuote != null;
            String quoteTextString = isErgoQuote ? ergoQuote.getQuoteSymbol() : tokenQuote.getSymbol();
            

            BigDecimal tokenQuoteBigDecimal = tokenQuote.getBigDecimalQuote();
            BigDecimal ergoQuoteBigDecimal = isErgoQuote ? ergoQuote.getBigDecimalQuote() : null;

            BigDecimal amountInErg = sendAmount.multiply(tokenQuoteBigDecimal);
     
            BigDecimal amountInErgQuote = isErgoQuote ? amountInErg.multiply(ergoQuoteBigDecimal) : null;

            BigDecimal quoteAmount = isErgoQuote ? amountInErgQuote : amountInErg;

            String quoteSymbolString = isErgoQuote ? ergoQuote.getQuoteSymbol() : tokenQuote.getQuoteSymbol();
            String quoteAmountString = quoteSymbolString.indexOf("USD") > -1 ? quoteAmount.setScale(2, RoundingMode.HALF_UP).toPlainString() : quoteAmount.toPlainString();

            if(m_quoteAmountField == null && sendAmount != null && sendAmount.compareTo(BigDecimal.ZERO) > 0){
                addQuoteAmountBox();
            }
            if(m_quoteAmountField != null && !m_quoteAmountField.getText().equals(quoteAmountString)){
                m_quoteAmountField.setText(quoteAmountString);
            }
            if( m_quoteAmountSymbolText != null && !quoteTextString.equals(m_quoteAmountSymbolText.getText())){
                m_quoteAmountSymbolText.setText(quoteTextString);
            }
        }else{
            if(m_quoteAmountBox != null){
                removeQuoteAmountBox();
            }
        }
    }


    public PriceAmount getBalanceAmount() {
        return m_balanceAmount;
    }

    public long getTimeStamp() {
        return m_balanceAmount.getTimeStamp();
    }

    public void setTimeStamp(long timeStamp) {
        m_balanceAmount.setTimeStamp(timeStamp);
    }

    public SimpleLongProperty timeStampProperty() {
        return m_balanceAmount.timeStampProperty();
    }

    public String getBoxId() {
        return m_id;
    }

    public void setBoxId(String id) {
        m_id = id;
    }

    public long getQuoteTimeout() {
        return m_quoteTimeout;
    }

    public void setQuoteTimeout(long timeout) {
        m_quoteTimeout = timeout;
    }

    public String getTokenId() {
        return m_balanceAmount.getTokenId();
    }

    public int getMinImageWidth() {
        return m_minImgWidth;
    }

    public void setMinImageWidth(int width) {
        m_minImgWidth = width;
    }

    public PriceAmount getPriceAmount(){
        return getSendAmount();
    }


    public void shutdown() {

        if (m_amountListener != null) {
            m_balanceAmount.amountProperty().removeListener(m_amountListener);
            m_amountListener = null;
        }

        

    }

}
