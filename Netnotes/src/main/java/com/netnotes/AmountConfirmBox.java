package com.netnotes;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.ergoplatform.appkit.ErgoToken;

import com.utils.Utils;

import javafx.scene.layout.VBox;

public class AmountConfirmBox extends AmountBox {

    private final SimpleDoubleProperty m_rowHeight = new SimpleDoubleProperty(30); 
    private final PriceAmount m_confirmAmount;
    private final long m_confirmAmountLong;
    private final PriceAmount m_feeAmount;
    private final long m_feeAmountLong;



    public AmountConfirmBox(PriceAmount priceAmount, PriceAmount feeAmount, Scene scene) {
        
        super();
        m_confirmAmount = priceAmount;
        m_confirmAmountLong = m_confirmAmount.getLongAmount();
        m_feeAmount = feeAmount;
        m_feeAmountLong = feeAmount == null ? 0 : m_feeAmount.getLongAmount();

        layoutBox(feeAmount != null , scene);
    }

    private void layoutBox(boolean isFeeAmount, Scene scene ){
        PriceAmount totalAmount = m_feeAmount != null && isFeeAmount ? new PriceAmount(m_confirmAmountLong + m_feeAmountLong, m_confirmAmount.getCurrency()) : null;


        priceAmountProperty().set(m_confirmAmount);
        setAlignment(Pos.CENTER_LEFT);
        

        String textFieldId = getBoxId() +"TextField";

        final String amountString = m_confirmAmount.getAmountString() + ( isFeeAmount && totalAmount != null ? " + (" + m_feeAmount.getAmountString() + " Fee) = " + totalAmount.getAmountString() : "");
        
        TextField amountField = new TextField(amountString);
        amountField.setId("amountField");
        amountField.setAlignment(Pos.CENTER_LEFT);
        amountField.setEditable(false);
        amountField.setPrefWidth(200);
        amountField.setPadding(new Insets(3, 10, 3, 10));
        amountField.setUserData(textFieldId);
        
        amountField.prefWidthProperty().bind(scene.widthProperty().multiply(0.6));
   
        ImageView textViewImage = new ImageView(m_confirmAmount.getCurrency().getIcon());
        textViewImage.setPreserveRatio(true);
        textViewImage.fitHeightProperty().bind(m_rowHeight.subtract(2));

        VBox imgPaddingBox = new VBox(textViewImage);
        imgPaddingBox.setPadding(new Insets(0,15,0,10)); 
        imgPaddingBox.setAlignment(Pos.CENTER_LEFT);
        imgPaddingBox.minHeightProperty().bind(m_rowHeight);

        TextField currencyName = new TextField(m_confirmAmount.getCurrency().getName());
        currencyName.setFont(App.txtFont);
        currencyName.setPadding(new Insets(3, 10, 3, 10));
        currencyName.setPrefWidth(Utils.measureString(currencyName.getText(), new java.awt.Font("OCR A Extended",java.awt.Font.PLAIN, 14))+ 30);;


        

        getChildren().addAll(imgPaddingBox, amountField, currencyName);

        priceAmountProperty().addListener((obs,oldval, newval)-> {
            if(newval == null){
                amountField.setText("-");
            }else{
                amountField.setText(newval.getAmountString());
            }            
        });
    }


    public ErgoToken getErgoToken(){
        return new ErgoToken(getTokenId(), m_confirmAmountLong);
    }

    public SimpleDoubleProperty rowHeightProperty(){
        return m_rowHeight;
    } 
    
    public PriceAmount feeAmount(){
        return m_feeAmount;
    }

    public long feeAmountLong(){
        return m_feeAmountLong;
    }

    public String getAmountString(){
        return m_confirmAmount.toString();
    }

    public String getAmountCurrencyName(){
        return m_confirmAmount.getCurrency().getName();
    }

    public Image getAmountIcon(){
        return m_confirmAmount.getCurrency().getIcon();
    }

    public long getLongAmount(){
        return m_confirmAmountLong;
    }

    public String getTokenId(){
        return m_confirmAmount.getTokenId();
    }

 
    
    @Override
    public void updateBufferedImage() {
    }

}
