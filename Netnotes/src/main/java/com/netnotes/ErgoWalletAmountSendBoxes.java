package com.netnotes;
import com.google.gson.JsonObject;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Scene;

import com.google.gson.JsonElement;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;

public class ErgoWalletAmountSendBoxes extends AmountBoxes {
    private Scene m_scene;
    private final NetworkType m_networkType;
    private SimpleObjectProperty<JsonObject> m_balanceObject;
    private ChangeListener<JsonObject> m_balanceChangeListener;

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
    }

  
    @Override
    public void updateGrid(){
        super.updateGrid();

            
        
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
                ErgoWalletAmountSendBox box = new ErgoWalletAmountSendBox(ergoAmount, m_scene);
                box.setTimeStamp(timeStamp);
                add(box);
            }else{
                ErgoWalletAmountSendBox ergoAmountBox = (ErgoWalletAmountSendBox) amountBoxInterface;

                ergoAmountBox.setBalance(timeStamp, nanoErg);
           
                
                
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
                        ErgoWalletAmountSendBox box = new ErgoWalletAmountSendBox(tokenAmount, getScene());
                        box.setTimeStamp(timeStamp);
                        add(box);
                          
                 
                    }else{
                        amountBox.setBalance(timeStamp, amount);
                    //    amountBox.setTimeStamp(timeStamp);
                    //    amountBox.getPriceAmount().setLongAmount(amount);
                       
                    }
                    
                }
 
                removeOld(timeStamp);

             
            }else{
               clear();
            }
     
             
        } 

       
    }

    public void reset(){
       
        for(int i = 0; i < getAmountBoxList().size();i++ ){
            AmountBoxInterface amountBox = getAmountBoxList().get(i);
            if(amountBox instanceof ErgoWalletAmountSendBox){   
                ErgoWalletAmountSendBox sendBox = (ErgoWalletAmountSendBox) amountBox;
                
                sendBox.reset();
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
