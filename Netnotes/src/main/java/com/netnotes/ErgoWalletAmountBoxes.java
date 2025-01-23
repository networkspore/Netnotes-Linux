package com.netnotes;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

import com.google.gson.JsonElement;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.ergoplatform.appkit.NetworkType;


public class ErgoWalletAmountBoxes extends AmountBoxes {

    private final NetworkType m_networkType;
    private SimpleObjectProperty<JsonObject> m_balanceObject;
    private ChangeListener<JsonObject> m_balanceChangeListener;
   // private SimpleObjectProperty<PriceQuote> m_ergoQuoteProperty = new SimpleObjectProperty<>(null);
    

    public ErgoWalletAmountBoxes( boolean isConfirmed, NetworkType networktype, SimpleObjectProperty<JsonObject> balanceObject){
        super();

        m_networkType = networktype;
        m_balanceObject = balanceObject;

        m_balanceChangeListener = (obs,oldval, newval) ->{
            update(newval);
        };

        m_balanceObject.addListener(m_balanceChangeListener);

    }

    public void update(JsonObject balanceJson){
        update(balanceJson, false);
    }

    /**
     * 
     * @param balanceJson - the balance json object from the explorer, which may have quote information added by the wallet
     * @param getUnconfirmed - not currently supported
     */
    public void update(JsonObject balanceJson, boolean getUnconfirmed){
        

        JsonElement timeStampElement = balanceJson != null ? balanceJson.get("timeStamp") : null;
        JsonElement objElement = balanceJson != null ? balanceJson.get("confirmed") : null;

        long timeStamp = timeStampElement != null ? timeStampElement.getAsLong() : -1;
        if (objElement != null && timeStamp != -1) {

            JsonObject confirmedObject = objElement.getAsJsonObject();
            JsonElement nanoErgElement = confirmedObject.get("nanoErgs");
            JsonElement ergoQuoteElement = confirmedObject.get("ergoQuote");
            JsonElement ergoQuoteAmountElement = confirmedObject.get("ergoQuoteAmount");

            long nanoErg = nanoErgElement != null && nanoErgElement.isJsonPrimitive() ? nanoErgElement.getAsLong() : 0;

            PriceQuote ergoQuote = ergoQuoteElement != null && !ergoQuoteElement.isJsonNull() && ergoQuoteElement.isJsonObject() ? new PriceQuote(ergoQuoteElement.getAsJsonObject()) : null;
            BigDecimal ergoQuoteAmount = ergoQuoteAmountElement != null && !ergoQuoteAmountElement.isJsonNull() ? ergoQuoteAmountElement.getAsBigDecimal() : null;

            //m_ergoQuoteProperty.set(ergoQuote);

            AmountBoxInterface ergAmountBoxInterface = getAmountBox(ErgoCurrency.TOKEN_ID);
            if(ergAmountBoxInterface == null){
                ErgoAmount ergoAmount = new ErgoAmount(nanoErg, m_networkType);
                ErgoWalletAmountBox box = new ErgoWalletAmountBox(ergoAmount, getScene());
                box.setTimeStamp(timeStamp);
                box.setQuote(ergoQuote, ergoQuoteAmount);
                add(box);
            }else if(ergAmountBoxInterface instanceof ErgoWalletAmountBox){
                ErgoWalletAmountBox ergoAmountBox = (ErgoWalletAmountBox) ergAmountBoxInterface;
                if(ergoAmountBox.getPriceAmount().getLongAmount() != nanoErg){
                    ergoAmountBox.getPriceAmount().setLongAmount(nanoErg);
                  
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
                    int decimals = decimalsElement.getAsInt();
                    String name = nameElement.getAsString();
                    String tokenType = tokenTypeElement.getAsString();
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
                        ErgoWalletTokenAmountBox box = new ErgoWalletTokenAmountBox(tokenAmount, getScene());
                        box.setTimeStamp(timeStamp);
                        add(box);
                        box.setQuote(tokenQuote, tokenQuoteErgAmount, tokenQuoteAmount, ergoQuote != null ? ergoQuote.getQuoteSymbol() : null);
                    }else if(tokenBoxInterface instanceof ErgoWalletTokenAmountBox){
                        ErgoWalletTokenAmountBox tokenAmountBox = (ErgoWalletTokenAmountBox) tokenBoxInterface;
                        if(tokenAmountBox.getPriceAmount().getLongAmount() != amount){
                            tokenAmountBox.getPriceAmount().setLongAmount(amount);
                        }
                        tokenAmountBox.setTimeStamp(timeStamp);
                        tokenAmountBox.setQuote(tokenQuote, tokenQuoteErgAmount, tokenQuoteAmount, ergoQuote != null ? ergoQuote.getQuoteSymbol() : null);
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


}
