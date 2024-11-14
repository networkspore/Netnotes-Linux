package com.netnotes;
import com.google.gson.JsonObject;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;

import com.google.gson.JsonElement;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;

public class ErgoWalletAmountBoxes extends AmountBoxes {
    private final boolean m_isConfirmed;
    private final NetworkType m_networkType;
    private SimpleObjectProperty<JsonObject> m_balanceObject;
    private ChangeListener<JsonObject> m_balanceChangeListener;

    public ErgoWalletAmountBoxes(boolean isConfirmed, NetworkType networktype, SimpleObjectProperty<JsonObject> balanceObject){
        super();
        m_isConfirmed = isConfirmed;
        m_networkType = networktype;
      
        m_balanceObject = balanceObject;
        
        
        m_balanceChangeListener = (obs,oldval, newval) ->{
            update(newval);
        };

        m_balanceObject.addListener(m_balanceChangeListener);

       
       
    }

  
    @Override
    public void updateGrid(){
        super.updateGrid();

            
        
    }

    
    public void update(JsonObject json){

   

        JsonElement timeStampElement = json != null ? json.get("timeStamp") : null;
        JsonElement objElement = json != null ? json.get(m_isConfirmed ? "confirmed" : "unconfirmed") : null;

        long timeStamp = timeStampElement != null ? timeStampElement.getAsLong() : -1;
        if (objElement != null && timeStamp != -1) {

            JsonObject objObject = objElement.getAsJsonObject();
            JsonElement nanoErgElement = objObject.get("nanoErgs");
            
            long nanoErg = nanoErgElement != null && nanoErgElement.isJsonPrimitive() ? nanoErgElement.getAsLong() : 0;

            AmountBox ergoAmountBox = (AmountBox) getAmountBox(ErgoCurrency.TOKEN_ID);
            if(ergoAmountBox == null){
                ErgoAmount ergoAmount = new ErgoAmount(nanoErg, m_networkType);
                AmountBox box = new AmountBox(ergoAmount, getScene());
                box.setTimeStamp(timeStamp);
                add(box);
            }else{
                ergoAmountBox.getPriceAmount().setLongAmount(nanoErg);
                ergoAmountBox.setTimeStamp(timeStamp);
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
              
                    AmountBox amountBox = (AmountBox) getAmountBox(tokenId);
                    if(amountBox == null){
                        AmountBox box = new AmountBox(tokenAmount, getScene());
                        box.setTimeStamp(timeStamp);
                        add(box);
                          
                    
                    }else{

                        amountBox.getPriceAmount().setLongAmount(amount);
                        amountBox.setTimeStamp(timeStamp);
                    }
                    
                }
 
                removeOld(timeStamp);

             
            }else{
               clear();
            }
     
             
        } 

       
    }


    public void updateToken(String id){
        if(id != null){
            ObservableList <AmountBoxInterface> list = amountsList();
    
            int size = list.size();
        
            for(int i = 0; i < size; i++){
                AmountBoxInterface amountBox = list.get(i);

                if(id.equals(amountBox.getTokenId())){
                   
                    break;
                }
            }
        }
    
       
    }
}
