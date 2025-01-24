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

    public Scene getAppScene(){
        return m_scene;
    }


    public TextArea warningField(){
        return m_warningTextArea;
    }

    private boolean m_warningBoolean;

    public void updateWarnings(){
        AmountBoxInterface ergoAmountBoxInterface = getAmountBox(ErgoCurrency.TOKEN_ID);
        ErgoWalletAmountSendBox ergoAmountSendBox = ergoAmountBoxInterface != null && ergoAmountBoxInterface instanceof ErgoWalletAmountSendBox ? (ErgoWalletAmountSendBox) ergoAmountBoxInterface : null;
    
        int boxesSize = size();

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
                
                    String warningText = "Notice: Addresses must maintain a minimum " + minFee + " ERG when containing tokens.";
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

    public void update(JsonObject balanceJson){

        update(balanceJson, false);
    
    }


    public void update(JsonObject json, boolean confirmed){
        

        JsonElement timeStampElement = json != null ? json.get("timeStamp") : null;
        JsonElement objElement = json != null ? json.get("confirmed") : null;

        long timeStamp = timeStampElement != null ? timeStampElement.getAsLong() : -1;
        if (objElement != null && timeStamp != -1) {

            JsonObject confirmedObject = objElement.getAsJsonObject();
            JsonElement nanoErgElement = confirmedObject.get("nanoErgs");
            JsonElement ergoQuoteElement = confirmedObject.get("ergoQuote");
            JsonElement ergoQuoteAmountElement = confirmedObject.get("ergoQuoteAmount");

            long nanoErg = nanoErgElement != null && nanoErgElement.isJsonPrimitive() ? nanoErgElement.getAsLong() : 0;

            PriceQuote ergoQuote = ergoQuoteElement != null && !ergoQuoteElement.isJsonNull() && ergoQuoteElement.isJsonObject() ? new PriceQuote(ergoQuoteElement.getAsJsonObject()) : null;
            BigDecimal ergoQuoteAmount = ergoQuoteAmountElement != null && !ergoQuoteAmountElement.isJsonNull() ? ergoQuoteAmountElement.getAsBigDecimal() : null;

        

            AmountBoxInterface ergAmountBoxInterface = getAmountBox(ErgoCurrency.TOKEN_ID);
            if(ergAmountBoxInterface == null){
                ErgoAmount ergoAmount = new ErgoAmount(nanoErg, m_networkType);
                ErgoWalletAmountSendBox box = new ErgoWalletAmountSendBox(this, ergoAmount, m_scene);
                box.setTimeStamp(timeStamp);
                box.setQuote(ergoQuote, ergoQuoteAmount);
                add(box, false);
            }else if(ergAmountBoxInterface instanceof ErgoWalletAmountSendBox){
                ErgoWalletAmountSendBox ergoAmountBox = (ErgoWalletAmountSendBox) ergAmountBoxInterface;
                if(ergoAmountBox.getBalanceAmount().getLongAmount() != nanoErg){
                    ergoAmountBox.getBalanceAmount().setLongAmount(nanoErg);
                  
                }
                ergoAmountBox.setQuote(ergoQuote, ergoQuoteAmount);

                ergoAmountBox.setTimeStamp(timeStamp);
            }
          
           
            
            JsonElement confirmedArrayElement = confirmedObject.get("tokens");
        

        
            if(confirmedArrayElement != null && confirmedArrayElement.isJsonArray()){
                JsonArray confirmedTokenArray = confirmedArrayElement.getAsJsonArray();
            
                
            
                for (JsonElement tokenElement : confirmedTokenArray) {
                    JsonObject tokenObject = tokenElement.getAsJsonObject();

                    JsonElement tokenIdElement = tokenObject.get("tokenId");
                    JsonElement amountElement = tokenObject.get("amount");
                    JsonElement decimalsElement = tokenObject.get("decimals");
                    JsonElement nameElement = tokenObject.get("name");
                    JsonElement tokenTypeElement = tokenObject.get("tokenType");
                    JsonElement tokenQuoteElement = tokenObject.get("tokenQuote");
                    JsonElement tokenInfoElement = tokenObject.get("tokenInfo");
                    JsonElement tokenQuoteErgAmountElement = tokenObject.get("tokenQuoteErgAmount");
                    JsonElement tokenQuoteAmountElement = tokenObject.get("tokenQuoteAmount");

                    String tokenId = tokenIdElement.getAsString();
                    long amount = amountElement.getAsLong();
                    int decimals = decimalsElement != null && !decimalsElement.isJsonNull() ? decimalsElement.getAsInt() : 0;
                    String name = nameElement != null && !nameElement.isJsonNull() ? nameElement.getAsString() : tokenId;
                    String tokenType = tokenTypeElement != null && !tokenTypeElement.isJsonNull() ? tokenTypeElement.getAsString() : "";
                    JsonObject tokenInfoJsonObject = tokenInfoElement != null && !tokenInfoElement.isJsonNull() && tokenInfoElement.isJsonObject() ? tokenInfoElement.getAsJsonObject() : null;
                    
                    PriceCurrency priceCurrency = new PriceCurrency(tokenId, name, decimals, tokenType, m_networkType.toString());
                    if(tokenInfoJsonObject != null){
                        priceCurrency.setTokenInfo(tokenInfoJsonObject);
                    }

                    PriceAmount tokenAmount = new PriceAmount(amount, priceCurrency);    
                    PriceQuote tokenQuote = tokenQuoteElement != null && !tokenQuoteElement.isJsonNull() && tokenQuoteElement.isJsonObject() ? new PriceQuote(tokenQuoteElement.getAsJsonObject()) : null;
                    BigDecimal tokenQuoteErgAmount = tokenQuoteErgAmountElement != null && !tokenQuoteErgAmountElement.isJsonNull() ? tokenQuoteErgAmountElement.getAsBigDecimal() : null;
                    BigDecimal tokenQuoteAmount = tokenQuoteAmountElement != null && !tokenQuoteAmountElement.isJsonNull() ? tokenQuoteAmountElement.getAsBigDecimal() : null;

                    AmountBoxInterface tokenBoxInterface = getAmountBox(tokenId);
                    if(tokenBoxInterface == null){
                        ErgoWalletAmountSendTokenBox box = new ErgoWalletAmountSendTokenBox(this, tokenAmount, m_scene);
                        box.setTimeStamp(timeStamp);
                        add(box,false);
                        box.setQuote(tokenQuote, ergoQuote, tokenQuoteErgAmount, tokenQuoteAmount, ergoQuote != null ? ergoQuote.getQuoteSymbol() : null);
                    }else if(tokenBoxInterface instanceof ErgoWalletAmountSendTokenBox){
                        ErgoWalletAmountSendTokenBox tokenAmountBox = (ErgoWalletAmountSendTokenBox) tokenBoxInterface;
                        if(tokenAmountBox.getBalanceAmount().getLongAmount() != amount){
                            tokenAmountBox.getBalanceAmount().setLongAmount(amount);
                        }
                        tokenAmountBox.setTimeStamp(timeStamp);
                        tokenAmountBox.setQuote(tokenQuote, ergoQuote, tokenQuoteErgAmount, tokenQuoteAmount, ergoQuote != null ? ergoQuote.getQuoteSymbol() : null);
                    }
                    
                }
 
                removeOld(timeStamp);

                updateGrid();
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
