package com.netnotes;
import com.google.gson.JsonObject;

import javafx.collections.ObservableList;

import com.google.gson.JsonElement;

import java.math.BigDecimal;
import java.util.ArrayList;

import com.google.gson.JsonArray;

public class ErgoWalletAmountBoxes extends AmountBoxes {
    private final ErgoTokens m_ergoTokens;
    private final boolean m_isConfirmed;

    private ErgoAmount m_ergoAmount;
    private AmountBox m_ergoAmountBox;


    public ErgoWalletAmountBoxes(ErgoTokens ergoTokens, boolean isConfirmed, AmountBoxInterface... boxes){
        super();
        m_ergoTokens = ergoTokens;
        m_isConfirmed = isConfirmed;
        m_ergoAmount = new ErgoAmount(BigDecimal.ZERO, m_ergoTokens.getNetworkType());
        m_ergoAmountBox = new AmountBox(ergoTokens, m_ergoAmount, getScene());
        getChildren().add(m_ergoAmountBox);
        init(boxes);
       
    }

  
    @Override
    public void updateGrid(){
        super.updateGrid();

            
        
    }
    
    
    public void update(JsonObject json, long timeStamp){
        
        JsonElement objElement = json != null ? json.get(m_isConfirmed ? "confirmed" : "unconfirmed") : null;


        if (objElement != null) {

            JsonObject objObject = objElement.getAsJsonObject();
       
            
            JsonElement confirmedArrayElement = objObject.get("tokens");
            JsonElement nanoErgElement = objObject.get("nanoErgs");

        
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
                    
                    PriceAmount tokenAmount = new PriceAmount(amount, new PriceCurrency(m_ergoTokens, tokenId, name, decimals, tokenType, m_ergoTokens.getErgoNetworkData().getNetworkType().toString()));    
              
                    AmountBox amountBox = (AmountBox) getAmountBox(tokenId);
                    if(amountBox == null){
                        AmountBox box = new AmountBox(m_ergoTokens, tokenAmount, getScene());
                        box.setTimeStamp(timeStamp);
                        add(box);

                    }else{

                        amountBox.getPriceAmount().setLongAmount(amount);
                        amountBox.setTimeStamp(timeStamp);
                    }
                    
                }

                removeOld(timeStamp);

            }
        
            long nanoErg = nanoErgElement != null && nanoErgElement.isJsonPrimitive() ? nanoErgElement.getAsLong() : 0;
            
            m_ergoAmount.setLongAmount(nanoErg);
            
        } 

       
    }
    public void updateTokens(){
        ObservableList <AmountBoxInterface> list = amountsList();
  
        int size = list.size();
    
        for(int i = 0; i < size; i++){
            AmountBoxInterface amountBox = list.get(i);
            amountBox.updateToken();
        }
    
       
    }

    public void updateToken(String id){
        if(id != null){
            ObservableList <AmountBoxInterface> list = amountsList();
    
            int size = list.size();
        
            for(int i = 0; i < size; i++){
                AmountBoxInterface amountBox = list.get(i);

                if(id.equals(amountBox.getTokenId())){
                    amountBox.updateToken();
                    break;
                }
            }
        }
    
       
    }
}
