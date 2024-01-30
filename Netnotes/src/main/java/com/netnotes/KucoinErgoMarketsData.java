package com.netnotes;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import javafx.scene.text.Text;

public class KucoinErgoMarketsData extends ErgoMarketsData {

    private MessageInterface m_msgListener;
    

    public KucoinErgoMarketsData(ErgoMarketsList marketsList, JsonObject json) throws NullPointerException{
        super(marketsList, json);

    }

    public KucoinErgoMarketsData(ErgoMarketsList marketsList) {
        super("KuCoin Ticker (Live)", KucoinExchange.NETWORK_ID, "ERG", "USDT", ErgoMarketsData.REALTIME, ErgoMarketsData.TICKER, marketsList);

    }




    @Override
    public JsonObject getJsonObject() {
        
        return super.getJsonObject();
    }


    @Override
    public void start() {
      
        if (getMarketId() != null &&  !statusProperty().get().equals(STARTED)) {
            NoteInterface marketInterface = getMarketsList().getErgoMarkets().getNetworksData().getNoteInterface(getMarketId());
           
            if (marketInterface instanceof KucoinExchange) {
                KucoinExchange exchange = (KucoinExchange) marketInterface;

                switch (getUpdateType()) {
                    case POLLED:
                        startPollingKuCoin(exchange);
                        break;
                    case REALTIME:
                        startKucoinListener(exchange);
                }
            }
                
        }
    }

    private void startKucoinListener(KucoinExchange exchange) {
        String symbol = getBaseSymbol() + "-" + getQuoteSymbol();
        switch (getValue()) {
            case TICKER:
                startTicker(symbol, exchange);
                break;
        }

    }

  

    private void stopTicker(KucoinExchange exchange) {
    
        boolean removed = exchange.removeMsgListener(m_msgListener);
        if (removed) {

            exchange.unsubscribeToTicker(getId(), getBaseSymbol() + "-" + getQuoteSymbol());
            statusProperty().set(STOPPED);

            getShutdownNow().removeListener(getShutdownListener());
        }
    }

    private void startTicker(String symbol, KucoinExchange exchange) {
        statusProperty().set(STARTING);

        String subjectString = "trade.ticker";
        String topicString = "/market/ticker:" + symbol;

        exchange.getTicker(symbol, (onSucceeded) -> {
            Object sourceObject = onSucceeded.getSource().getValue();
            if (sourceObject != null && sourceObject instanceof JsonObject) {
                onTickerMsg((JsonObject) sourceObject);
            }
        }, (onFailed) -> {
            
            Platform.runLater(()->statusProperty().set(ERROR));
        });

        m_msgListener = new MessageInterface() {
            
            public String getSubject() {
                return subjectString;
            }

            public String getTopic() {
                return topicString;
            }

            public String getTunnelId() {
                return null;
            }

            public String getId() {
                return getMarketId();
            }

            public void onMsgChanged(JsonObject json) {
                /*try {
                    Files.writeString(logFile.toPath(), json.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }*/

                JsonElement subjectElement = json.get("subject");
                JsonElement topicElement = json.get("topic");

                String subject = subjectElement != null && subjectElement.isJsonPrimitive() ? subjectElement.getAsString() : null;
                String topic = topicElement != null && topicElement.isJsonPrimitive() ? topicElement.getAsString() : null;
                if (subject != null && topic != null && subject.equals(subjectString)) {

                    if (topic.substring(topic.length() - symbol.length(), topic.length()).equals(symbol)) {
                        onTickerMsg(json);
                    }
                }
            }

            public void onReady() {
                /*try {
                    Files.writeString(logFile.toPath(), "\nready", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }*/

                exchange.subscribeToTicker(getId(), getBaseSymbol() + "-" + getQuoteSymbol());
            }
        };

        exchange.addMsgListener(m_msgListener);

        if (exchange.isClientReady()) {

            exchange.subscribeToTicker(getId(), getBaseSymbol() + "-" + getQuoteSymbol());

        }

        setShutdownListener((obs, oldval, newVal) -> {
            stopTicker(exchange);
        });

        

    }

    private void onTickerMsg(JsonObject json) {
      
        if (json != null) {

            JsonElement dataElement = json.get("data");

            if (dataElement != null && dataElement.isJsonObject()) {
                JsonObject dataObject = dataElement.getAsJsonObject();

                JsonElement priceElement = dataObject.get("price");
               // JsonElement timeElement = dataObject.get("time");
               String priceString = priceElement != null && priceElement.isJsonPrimitive() ? priceElement.getAsString() : null;



                if(priceString!=null){
                    priceQuoteProperty().set(new PriceQuote(priceString, getBaseSymbol(), getQuoteSymbol(), System.currentTimeMillis()));
                }
            }
        }
    }

    private void startPollingKuCoin(KucoinExchange exchange) {

        statusProperty().set(STARTED);

      
        getMarketsList().getErgoMarkets().getNetworksData().timeCycleProperty().addListener((obs,oldval,newval)->{
            statusProperty().set(POLLING);
            exchange.getTicker(getBaseSymbol() + "-" + getQuoteSymbol(), (success) -> {
                Object sourceObject = success.getSource().getValue();
                if (sourceObject != null && sourceObject instanceof PriceQuote) {
                    PriceQuote priceQuote = (PriceQuote) sourceObject;
                    priceQuoteProperty().set(priceQuote);
                }
            }, (failed) -> {
                statusProperty().set(ERROR);
            });
        });
                
   

        

    }

    
    
    

 
  
}
