package com.netnotes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.devskiller.friendly_id.FriendlyId;
import com.utils.Utils;

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
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

public class ErgoWalletAmountSendBox extends HBox implements AmountBoxInterface {

    private long m_quoteTimeout = AddressesData.QUOTE_TIMEOUT;
    private PriceAmount m_balanceAmount = null;

    private String m_id = null;

    private int m_minImgWidth = 250;
    private SimpleBooleanProperty m_showSubMenuProperty = new SimpleBooleanProperty(false);
    private SimpleObjectProperty<PriceQuote> m_priceQuote = new SimpleObjectProperty<>(null);
    private ChangeListener<PriceQuote> m_priceQuoteListener = null;
    private ChangeListener<BigDecimal> m_amountListener = null;

    private TextField m_amountField;
    private int m_leftColWidth = 140;
    private int m_botRowPadding = 3;

    private ErgoWalletAmountSendBoxes m_sendBoxes;

    public void reset() {
        m_amountField.setText("");
    }

    public boolean isSendAmount() {
        if (m_amountField.getLength() > 0) {
            return !(m_amountField.getLength() == 1 && m_amountField.getText().equals("0"));
        }
        return false;
    }

   

    private SimpleBooleanProperty m_isFieldFocused = new SimpleBooleanProperty(false);

    public ErgoWalletAmountSendBox(ErgoWalletAmountSendBoxes sendBoxes, PriceAmount balanceAmount, Scene scene) {
        super();
        m_sendBoxes = sendBoxes;
        m_id = FriendlyId.createFriendlyId();
        m_balanceAmount = balanceAmount;

        TextField currencyName = new TextField(m_balanceAmount.getCurrency().getName());
        HBox.setHgrow(currencyName, Priority.ALWAYS);
        currencyName.setMaxWidth(m_leftColWidth);
        currencyName.setEditable(false);

        String textFieldId = balanceAmount.getCurrency().getName() + "sendBox";

        m_amountField = new TextField();
        HBox.setHgrow(m_amountField, Priority.ALWAYS);
        m_amountField.setAlignment(Pos.CENTER_LEFT);
        m_amountField.setUserData(textFieldId);

        ImageView currencyImageView = new ImageView();
        currencyImageView.setPreserveRatio(true);
        currencyImageView.setFitWidth(App.MENU_BAR_IMAGE_WIDTH);
        currencyImageView.setImage(m_balanceAmount.getCurrency().getIcon());

        Button maxBtn = new Button("MAX");

        HBox amountFieldBox = new HBox(m_amountField, maxBtn);
        HBox.setHgrow(amountFieldBox, Priority.ALWAYS);
        amountFieldBox.setId("bodyBox");
        amountFieldBox.setAlignment(Pos.CENTER_LEFT);
        amountFieldBox.setMaxHeight(18);



        // tokenId
        Label tokenIdIcon = new Label("  ");
        tokenIdIcon.setId("logoBtn");

        Label tokenIdText = new Label("Token Id");
        tokenIdText.setFont(App.txtFont);
        tokenIdText.setPadding(new Insets(0, 5, 0, 5));
        tokenIdText.setMinWidth(m_leftColWidth);

        TextField tokenIdField = new TextField(getTokenId());
        HBox.setHgrow(tokenIdField, Priority.ALWAYS);
        tokenIdField.setEditable(false);

        HBox tokenIdFieldBox = new HBox(tokenIdField);
        HBox.setHgrow(tokenIdFieldBox, Priority.ALWAYS);
        tokenIdFieldBox.setId("bodyBox");
        tokenIdFieldBox.setAlignment(Pos.CENTER_LEFT);
        tokenIdFieldBox.setMaxHeight(18);

        HBox tokenIdBox = new HBox(tokenIdIcon, tokenIdText, tokenIdFieldBox);
        HBox.setHgrow(tokenIdBox, Priority.ALWAYS);
        tokenIdBox.setAlignment(Pos.CENTER_LEFT);
        tokenIdBox.setPadding(new Insets(0, 0, m_botRowPadding, 0));

        // Description
        Label descriptionIcon = new Label("  ");
        descriptionIcon.setId("logoBtn");

        Label descriptionText = new Label("Description");
        descriptionText.setFont(App.titleFont);
        descriptionText.setPadding(new Insets(0, 5, 0, 5));
        descriptionText.setMinWidth(m_leftColWidth);

        TextField descriptionField = new TextField();
        HBox.setHgrow(descriptionField, Priority.ALWAYS);
        descriptionField.setEditable(false);
        descriptionField.textProperty().bind(m_balanceAmount.getCurrency().descriptionProperty());

        HBox descriptionFieldBox = new HBox(descriptionField);
        HBox.setHgrow(descriptionFieldBox, Priority.ALWAYS);
        descriptionFieldBox.setId("bodyBox");
        descriptionFieldBox.setAlignment(Pos.CENTER_LEFT);
        descriptionFieldBox.setMaxHeight(18);

        HBox descriptionBox = new HBox(descriptionIcon, descriptionText, descriptionFieldBox);
        HBox.setHgrow(descriptionBox, Priority.ALWAYS);
        descriptionBox.setAlignment(Pos.CENTER_LEFT);
        descriptionBox.setPadding(new Insets(0, 0, m_botRowPadding, 0));
        // emissionAmount

        Label emissionAmountIcon = new Label("  ");
        emissionAmountIcon.setId("logoBtn");

        Label emissionAmountText = new Label("Emission");
        emissionAmountText.setFont(App.txtFont);
        emissionAmountText.setPadding(new Insets(0, 5, 0, 5));
        emissionAmountText.setMinWidth(m_leftColWidth);

        TextField emissionAmountField = new TextField();
        emissionAmountField.setEditable(false);
        emissionAmountField.textProperty().bind(m_balanceAmount.getCurrency().emissionAmountProperty().asString());
        HBox.setHgrow(emissionAmountField, Priority.ALWAYS);

        HBox emissionAmountFieldBox = new HBox(emissionAmountField);
        HBox.setHgrow(emissionAmountFieldBox, Priority.ALWAYS);
        emissionAmountFieldBox.setId("bodyBox");
        emissionAmountFieldBox.setAlignment(Pos.CENTER_LEFT);
        emissionAmountFieldBox.setMaxHeight(18);

        HBox emissionAmountBox = new HBox(emissionAmountIcon, emissionAmountText, emissionAmountFieldBox);
        HBox.setHgrow(emissionAmountBox, Priority.ALWAYS);
        emissionAmountBox.setAlignment(Pos.CENTER_LEFT);
        emissionAmountBox.setPadding(new Insets(0, 0, m_botRowPadding, 0));
        // decimals

        Label decimalsIcon = new Label("  ");
        decimalsIcon.setId("logoBtn");

        Label decimalsText = new Label("Decimals");
        decimalsText.setFont(App.txtFont);
        decimalsText.setPadding(new Insets(0, 5, 0, 5));
        decimalsText.setMinWidth(m_leftColWidth);

        TextField decimalsField = new TextField(m_balanceAmount.getCurrency().getDecimals() + "");
        decimalsField.setEditable(false);
        HBox.setHgrow(decimalsField, Priority.ALWAYS);

        HBox decimalsFieldBox = new HBox(decimalsField);
        HBox.setHgrow(decimalsFieldBox, Priority.ALWAYS);
        decimalsFieldBox.setId("bodyBox");
        decimalsFieldBox.setAlignment(Pos.CENTER_LEFT);
        decimalsFieldBox.setMaxHeight(18);

        HBox decimalsBox = new HBox(decimalsIcon, decimalsText, decimalsFieldBox);
        HBox.setHgrow(decimalsBox, Priority.ALWAYS);
        decimalsBox.setAlignment(Pos.CENTER_LEFT);
        decimalsBox.setPadding(new Insets(0, 0, m_botRowPadding, 0));

        // Image

        Label imageIcon = new Label("  ");
        imageIcon.setId("logoBtn");

        Label imageText = new Label("Image");
        imageText.setFont(App.txtFont);
        imageText.setPadding(new Insets(0, 5, 0, 5));
        imageText.setMinWidth(m_leftColWidth);

        String defaultImgString = m_balanceAmount.getCurrency().getImageString();

        TextField imageField = new TextField(
                defaultImgString.equals("/assets/unknown-unit.png") || defaultImgString.equals("/assets/unitErgo.png")
                        ? "(default)"
                        : defaultImgString);
        HBox.setHgrow(imageField, Priority.ALWAYS);
        imageField.setEditable(false);

        HBox imageFieldBox = new HBox(imageField);
        HBox.setHgrow(imageFieldBox, Priority.ALWAYS);
        imageFieldBox.setId("bodyBox");
        imageFieldBox.setAlignment(Pos.CENTER_LEFT);
        imageFieldBox.setMaxHeight(18);

        Label toggleShowSubMenuBtn = new Label(m_showSubMenuProperty.get() ? "⏷" : "⏵");
        toggleShowSubMenuBtn.setId("caretBtn");
        toggleShowSubMenuBtn.setMinWidth(25);

        Label balanceIcon = new Label("  ");
        balanceIcon.setId("logoBtn");

        Label balanceLabel = new Label("Balance");
        balanceLabel.setFont(App.txtFont);
        balanceLabel.setPadding(new Insets(0, 5, 0, 5));
        balanceLabel.setMinWidth(m_leftColWidth);

        TextField balanceField = new TextField(m_balanceAmount.amountProperty().get().toString());
        HBox.setHgrow(balanceField, Priority.ALWAYS);
        balanceField.setAlignment(Pos.CENTER_LEFT);
        balanceField.setEditable(false);

        m_balanceAmount.amountProperty().addListener((obs, oldval, newval) -> {
            if (!balanceField.getText().equals(newval.toString())) {
                balanceField.setText(newval.toString());
            }
        });

        maxBtn.setOnAction(e -> {
            BigDecimal balance = m_balanceAmount.amountProperty().get();
            BigDecimal fee = getTokenFeeBigDecimal();
            BigDecimal totalFee = balance.subtract(fee);
            totalFee = totalFee.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : totalFee;
            m_amountField.setText(totalFee.toString());
            m_sendBoxes.updateWarnings();
        });



        HBox balanceFieldBox = new HBox(balanceField);
        HBox.setHgrow(balanceFieldBox, Priority.ALWAYS);
        balanceFieldBox.setId("bodyBox");
        balanceFieldBox.setAlignment(Pos.CENTER_LEFT);
        balanceFieldBox.setMaxHeight(18);

        HBox balanceBox = new HBox(balanceIcon, balanceLabel, balanceFieldBox);
        HBox.setHgrow(balanceBox, Priority.ALWAYS);
        balanceBox.setAlignment(Pos.CENTER_LEFT);
        balanceBox.setPadding(new Insets(0, 0, m_botRowPadding, 0));

        // tokenId
        Label timeStampIcon = new Label("  ");
        timeStampIcon.setId("logoBtn");

        Label timeStampText = new Label("Time Stamp");
        timeStampText.setFont(App.txtFont);
        timeStampText.setPadding(new Insets(0, 5, 0, 5));
        timeStampText.setMinWidth(m_leftColWidth);

        TextField timeStampField = new TextField();
        HBox.setHgrow(timeStampField, Priority.ALWAYS);
        timeStampField.setEditable(false);
        Binding<String> timeStampBinding = Bindings
                .createObjectBinding(
                        () -> m_balanceAmount.timeStampProperty().get() == 0
                                ? Utils.formatDateTimeString(LocalDateTime.now())
                                : Utils.formatDateTimeString(
                                        Utils.milliToLocalTime(m_balanceAmount.timeStampProperty().get())),
                        m_balanceAmount.timeStampProperty());
        timeStampField.textProperty().bind(timeStampBinding);

        HBox timeStampFieldBox = new HBox(timeStampField);
        HBox.setHgrow(timeStampFieldBox, Priority.ALWAYS);
        timeStampFieldBox.setId("bodyBox");
        timeStampFieldBox.setAlignment(Pos.CENTER_LEFT);
        timeStampFieldBox.setMaxHeight(18);

        HBox timeStampBox = new HBox(timeStampIcon, timeStampText, timeStampFieldBox);
        HBox.setHgrow(timeStampBox, Priority.ALWAYS);
        timeStampBox.setAlignment(Pos.CENTER_LEFT);
        timeStampBox.setPadding(new Insets(0, 0, m_botRowPadding, 0));

        VBox bodyBox = new VBox(balanceBox, tokenIdBox, timeStampBox);
        HBox.setHgrow(bodyBox, Priority.ALWAYS);
        bodyBox.setAlignment(Pos.CENTER_LEFT);

        HBox bodyPaddingBox = new HBox();
        HBox.setHgrow(bodyBox, Priority.ALWAYS);
        bodyBox.setAlignment(Pos.CENTER_LEFT);

        HBox topBox = new HBox(toggleShowSubMenuBtn, currencyImageView, currencyName, amountFieldBox);
        HBox.setHgrow(topBox, Priority.ALWAYS);
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setPadding(new Insets(0, 10, m_botRowPadding, 0));

        VBox layoutBox = new VBox(topBox, bodyPaddingBox);
        HBox.setHgrow(layoutBox, Priority.ALWAYS);
        layoutBox.setPrefHeight(18);
        layoutBox.setPadding(new Insets(1, 0, 1, 0));

        Runnable quoteUpdate = () -> {
            // updateBufferedImage(amountImageView);
        };

        toggleShowSubMenuBtn.setOnMouseClicked(e -> {
            m_showSubMenuProperty.set(!m_showSubMenuProperty.get());
        });

        m_showSubMenuProperty.addListener((obs, oldval, newval) -> {
            toggleShowSubMenuBtn.setText(newval ? "⏷" : "⏵");
            if (newval) {
                if (!bodyPaddingBox.getChildren().contains(bodyBox)) {
                    bodyPaddingBox.getChildren().add(bodyBox);
                }
            } else {
                if (bodyPaddingBox.getChildren().contains(bodyBox)) {
                    bodyPaddingBox.getChildren().remove(bodyBox);
                }
            }
        });

        Runnable addCurrentAmountListeners = () -> {
            PriceAmount currentAmount = m_balanceAmount;

            m_amountListener = (obs, oldval, newval) -> {
                if (!m_amountField.getText().equals(newval + "")) {
                    m_amountField.setText(newval + "");
                }

            };

         

            currentAmount.amountProperty().addListener(m_amountListener);


        };

        addCurrentAmountListeners.run();

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
            if(!newval){
                m_sendBoxes.updateWarnings();
            }
        });
    }

    public SimpleBooleanProperty isFieldFocused() {
        return m_isFieldFocused;
    }

    public BigDecimal getTokenFeeBigDecimal(){
        PriceAmount feePriceAmount = m_sendBoxes.feeAmountProperty().get(); 
        return feePriceAmount != null && feePriceAmount.getTokenId().equals(getTokenId()) ? feePriceAmount.getBigDecimalAmount() : BigDecimal.ZERO;
    }

    public PriceAmount getSendAmount() {
        return isSendAmount() ? new PriceAmount(new BigDecimal(m_amountField.getText()), getPriceAmount().getCurrency())
                : null;
    }

    public BigDecimal getBalanceRemaining(){
        PriceAmount sendAmount = getSendAmount();
        BigDecimal sendDecimal = sendAmount == null ? BigDecimal.ZERO : sendAmount.getBigDecimalAmount();
        BigDecimal feeDecimal = getTokenFeeBigDecimal();
        BigDecimal balanceDecimal = getBalanceAmount().getBigDecimalAmount();

        return balanceDecimal.subtract(sendDecimal.add(feeDecimal));
    }

    public void updateQuote(PriceQuote priceQuote) {

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

    public PriceAmount getPriceAmount() {
        return m_balanceAmount;
    }

    public PriceQuote getPriceQuote() {
        return m_priceQuote.get();
    }

    public void setPriceQuote(PriceQuote priceQuote) {
        m_priceQuote.set(priceQuote);
    }

    public void shutdown() {

        if (m_amountListener != null) {
            m_balanceAmount.amountProperty().removeListener(m_amountListener);
            m_amountListener = null;
        }

        

    }

}
