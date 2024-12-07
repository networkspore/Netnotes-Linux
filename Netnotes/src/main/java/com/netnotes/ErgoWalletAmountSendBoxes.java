package com.netnotes;
import com.google.gson.JsonObject;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import com.google.gson.JsonElement;

import java.math.BigDecimal;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;

public class ErgoWalletAmountSendBoxes extends AmountBoxes {
    private Scene m_scene;
    private final NetworkType m_networkType;
    private SimpleObjectProperty<JsonObject> m_balanceObject;
    private ChangeListener<JsonObject> m_balanceChangeListener;
    private SimpleObjectProperty<PriceAmount> m_feeAmount = new SimpleObjectProperty<>(null);
    private SimpleObjectProperty<BigDecimal> m_minimumFee = new SimpleObjectProperty<>(BigDecimal.valueOf(0.001));
    private final TextArea m_warningTextArea;
    private final HBox m_warningBox;

    public ErgoWalletAmountSendBoxes(Scene scene, NetworkType networktype, SimpleObjectProperty<JsonObject> balanceObject){
        super();

        m_networkType = networktype;
      
        m_balanceObject = balanceObject;
        
        m_scene = scene;

        m_balanceChangeListener = (obs,oldval, newval) ->{
            update(newval);
        };

        m_balanceObject.addListener(m_balanceChangeListener);

        JsonObject balanceJson = balanceObject.get();
       
        

        update(balanceJson);

        m_warningTextArea = new TextArea();
        m_warningTextArea.setMinHeight(53);
        m_warningTextArea.setMaxHeight(53);
        HBox.setHgrow(m_warningTextArea, Priority.ALWAYS);
        m_warningTextArea.setEditable(false);
        m_warningTextArea.setWrapText(true);
        m_warningTextArea.setId("textAreaInput");
       


        m_warningBox = new HBox(m_warningTextArea);
        HBox.setHgrow(m_warningBox, Priority.ALWAYS);

        m_warningTextArea.textProperty().addListener((obs,oldval,newval)->{
            if(newval.length() > 0){
                if(!lastRowHBox().getChildren().contains(m_warningBox)){
                    lastRowHBox().getChildren().add(m_warningBox);
                }
            }else{
                if(lastRowHBox().getChildren().contains(m_warningBox)){
                    lastRowHBox().getChildren().remove(m_warningBox);
                }
            }
        });

      
    }



    public TextArea warningField(){
        return m_warningTextArea;
    }

    private boolean m_warningBoolean;

    public void updateWarnings(){
        AmountBoxInterface ergoAmountBoxInterface = getAmountBox(ErgoCurrency.TOKEN_ID);
        ErgoWalletAmountSendBox ergoAmountSendBox = ergoAmountBoxInterface != null && ergoAmountBoxInterface instanceof ErgoWalletAmountSendBox ? (ErgoWalletAmountSendBox) ergoAmountBoxInterface : null;
    
        int boxesSize = getAmountListSize();

        if(ergoAmountSendBox != null && boxesSize > 1){
            BigDecimal minFee = m_minimumFee.get();
            BigDecimal ergoAmountRemaining = ergoAmountSendBox.getBalanceRemaining();
            boolean isTokenWarning = ergoAmountRemaining.compareTo(minFee) < 0;
            if(isTokenWarning){
                AmountBoxInterface[] boxesArray = getAmountBoxArray();
                m_warningBoolean = false;
                for(AmountBoxInterface boxInterface : boxesArray){
                    if(boxInterface instanceof ErgoWalletAmountSendBox){
                        ErgoWalletAmountSendBox sendBox = (ErgoWalletAmountSendBox) boxInterface;
                        if(sendBox.getTokenId() != ErgoCurrency.TOKEN_ID && sendBox.getBalanceRemaining().compareTo(BigDecimal.ZERO) > 0){
                            m_warningBoolean = true;
                            break;
                        }
                    }
                }
                if(m_warningBoolean){
                
                    String warningText = "Notice: Addresses must maintain a minimum ~" + minFee + " ERG when containing tokens.";
                    if(!m_warningTextArea.getText().equals(warningText)){
                        m_warningTextArea.setText(warningText);
                    }
                        
                    
                }else{
                    m_warningTextArea.setText("");
                }
            }else{
                m_warningTextArea.setText("");
            }
        }else{
            m_warningTextArea.setText("");
        }

    }

    public SimpleObjectProperty<PriceAmount> feeAmountProperty(){
        return m_feeAmount;
    }

    public SimpleObjectProperty<BigDecimal> minimumFeeProperty(){
        return m_minimumFee;
    }
    
    public void update(JsonObject json){

        JsonElement timeStampElement = json != null ? json.get("timeStamp") : null;
        JsonElement objElement = json != null ? json.get( "confirmed") : null;

        long timeStamp = timeStampElement != null ? timeStampElement.getAsLong() : -1;
        if (objElement != null && timeStamp != -1) {

            JsonObject objObject = objElement.getAsJsonObject();
            JsonElement nanoErgElement = objObject.get("nanoErgs");
            
            long nanoErg = nanoErgElement != null && nanoErgElement.isJsonPrimitive() ? nanoErgElement.getAsLong() : 0;

            AmountBoxInterface amountBoxInterface = getAmountBox(ErgoCurrency.TOKEN_ID);
            if(amountBoxInterface == null){
                ErgoAmount ergoAmount = new ErgoAmount(nanoErg, m_networkType);
                ErgoWalletAmountSendBox box = new ErgoWalletAmountSendBox(this, ergoAmount, m_scene);
                box.setTimeStamp(timeStamp);
                add(box);
            }else{
                ErgoWalletAmountSendBox ergoAmountBox = (ErgoWalletAmountSendBox) amountBoxInterface;
                ergoAmountBox.getBalanceAmount().setLongAmount(nanoErg, timeStamp);
             
            }
          
     
            
            JsonElement confirmedArrayElement = objObject.get("tokens");
        

        
            if(confirmedArrayElement != null && confirmedArrayElement.isJsonArray()){
                JsonArray confirmedTokenArray = confirmedArrayElement.getAsJsonArray();
            
                
            
                for (JsonElement tokenElement : confirmedTokenArray) {
                    JsonObject tokenObject = tokenElement.getAsJsonObject();

                    JsonElement tokenIdElement = tokenObject.get("tokenId");
                    JsonElement amountElement = tokenObject.get("amount");
                    JsonElement decimalsElement = tokenObject.get("decimals");
                    JsonElement nameElement = tokenObject.get("name");
                    JsonElement tokenTypeElement = tokenObject.get("tokenType");
                    
                    String tokenId = tokenIdElement.getAsString();
                    long amount = amountElement.getAsLong();
                    int decimals = decimalsElement.getAsInt();
                    String name = nameElement.getAsString();
                    String tokenType = tokenTypeElement.getAsString();
                    
                    PriceAmount tokenAmount = new PriceAmount(amount, new PriceCurrency(tokenId, name, decimals, tokenType, m_networkType.toString()));    
              
                    ErgoWalletAmountSendBox amountBox = (ErgoWalletAmountSendBox) getAmountBox(tokenId);
                    if(amountBox == null){
                        ErgoWalletAmountSendBox box = new ErgoWalletAmountSendBox(this, tokenAmount, m_scene);
                        box.setTimeStamp(timeStamp);
                        add(box);
                          
                 
                    }else{
                        amountBox.getBalanceAmount().setLongAmount( amount, timeStamp);
                    //    amountBox.setTimeStamp(timeStamp);
                    //    amountBox.getPriceAmount().setLongAmount(amount);
                       
                    }
                    
                }
 
                removeOld(timeStamp);

             
            }else{
               clear();
            }
     
             
        }else{
            clear();
        }

       
    }

    public void reset(){
        AmountBoxInterface[] amountBoxAray =  getAmountBoxArray();
                    
        for(int i = 0; i < amountBoxAray.length ;i++ ){
            AmountBoxInterface amountBox = amountBoxAray[i];

            if(amountBox instanceof ErgoWalletAmountSendBox){   
                ErgoWalletAmountSendBox sendBox = (ErgoWalletAmountSendBox) amountBox;
                
                sendBox.reset();
            }
        }
        
    }


  
}
