package com.netnotes;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.math.BigDecimal;

import javafx.scene.layout.VBox;

public class AmountConfirmBox extends AmountBox {

    private final SimpleDoubleProperty m_rowHeight = new SimpleDoubleProperty(30); 
    private final BigDecimal m_confirmAmount;
    private final BigDecimal m_feeAmount;

    private final String m_defaultName;
    private final PriceCurrency m_currency;

    public AmountConfirmBox(BigDecimal amount, BigDecimal feeAmount, PriceCurrency currency, Scene scene) {
    
        super();
        m_currency = currency;
        m_confirmAmount = amount;
  
        m_feeAmount = feeAmount;
        
        m_defaultName = m_currency.getDefaultName();
        
        layoutBox(feeAmount != null , scene);
    }

    private void layoutBox(boolean isFeeAmount, Scene scene ){
        PriceAmount totalAmount = m_feeAmount != null && isFeeAmount ? new PriceAmount(m_confirmAmount.add(m_feeAmount), m_currency) : null;


        //priceAmountProperty().set(m_confirmAmount);
        setAlignment(Pos.CENTER_LEFT);
        

        String textFieldId = getBoxId() +"TextField";

        final String amountString = m_confirmAmount + ( isFeeAmount && totalAmount != null ? " + (" + m_feeAmount + " Fee) = " + totalAmount.getAmountString() : "");
        
        TextField amountField = new TextField(amountString);
        amountField.setId("amountField");
        amountField.setAlignment(Pos.CENTER_LEFT);
        amountField.setEditable(false);
        amountField.setPrefWidth(200);
        amountField.setPadding(new Insets(3, 10, 3, 10));
        amountField.setUserData(textFieldId);
        
        amountField.prefWidthProperty().bind(scene.widthProperty().multiply(0.6));
   
        ImageView textViewImage = new ImageView(m_currency.getIcon());
        textViewImage.setPreserveRatio(true);
        textViewImage.fitHeightProperty().bind(m_rowHeight.subtract(2));

        VBox imgPaddingBox = new VBox(textViewImage);
        imgPaddingBox.setPadding(new Insets(0,15,0,10)); 
        imgPaddingBox.setAlignment(Pos.CENTER_LEFT);
        imgPaddingBox.minHeightProperty().bind(m_rowHeight);

        TextField currencyName = new TextField(m_defaultName);
        currencyName.setFont(App.txtFont);
        currencyName.setPadding(new Insets(3, 10, 3, 10));
        currencyName.setPrefWidth(60);

        

        getChildren().addAll(imgPaddingBox, amountField, currencyName);

        getPriceAmount().amountProperty().addListener((obs,oldval, newval)-> {
            if(newval == null){
                amountField.setText("-");
            }else{
                amountField.setText(newval + "");
            }            
        });
    }

    public PriceAmount getConfirmPriceAmount(){
        return new PriceAmount(m_confirmAmount, m_currency);
    }

    public PriceAmount getFeePriceAmount(){
        return m_feeAmount != null ? new PriceAmount(m_feeAmount, m_currency) : null;
    }

    public BigDecimal getConfirmAmount(){
        return m_confirmAmount;
    }

    public BigDecimal getFeeAmount(){
        return m_feeAmount;
    }

    public PriceCurrency getCurrency(){
        return m_currency;
    }

    /*public ErgoToken getErgoToken(){
        
        return new ErgoToken(getTokenId(), m_confirmAmountLong);
    }*/

    public SimpleDoubleProperty rowHeightProperty(){
        return m_rowHeight;
    } 
    

    public String getAmountString(){
        return m_confirmAmount + "";
    }

    public String getAmountCurrencyName(){
        return m_currency.getName();
    }

    public Image getAmountIcon(){
        return m_currency.getIcon();
    }



    public String getTokenId(){
        return m_currency.getTokenId();
    }

 
    


}
