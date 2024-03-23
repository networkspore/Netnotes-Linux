package com.netnotes;

import java.time.LocalDateTime;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.application.Platform;
//import javafx.beans.property.SimpleObjectProperty;


public class KucoinErgoMarketsData extends ErgoMarketsData {

    private MessageInterface m_msgListener;

    //private ScheduledExecutorService m_timeExecutor = null;
    //private ScheduledFuture<?> m_lastExecution = null;
    //private final SimpleObjectProperty<LocalDateTime> m_timeCycle = new SimpleObjectProperty<>(LocalDateTime.now());
    //private long m_cyclePeriod = 7;
   // private TimeUnit m_cycleTimeUnit = TimeUnit.SECONDS;
    

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

              //  switch (getUpdateType()) {
                   // case POLLED:
                     //   startPollingKuCoin(exchange);
                   //     break;
                   // case REALTIME:
                        startKucoinListener(exchange);
              //  }
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

    private String m_listenerId = FriendlyId.createFriendlyId();

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
                return m_listenerId;
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
    /*/
      public void setupTimer() {

        if (m_lastExecution != null) {
            m_lastExecution.cancel(false);
        }

        if (m_timeExecutor != null) {
            m_timeExecutor.shutdownNow();
            m_timeExecutor = null;
        }
        

        if (getCyclePeriod() > 0) {
            m_timeExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });
      
        
        }
    }*/

    /*

    private void startPollingKuCoin(KucoinExchange exchange) {

        statusProperty().set(STARTED);

      
        Runnable doUpdate = () -> {
                
            
            exchange.getTicker(getBaseSymbol() + "-" + getQuoteSymbol(), (success) -> {
                statusProperty().set(POLLING);
                Object sourceObject = success.getSource().getValue();
                if (sourceObject != null && sourceObject instanceof PriceQuote) {
                    PriceQuote priceQuote = (PriceQuote) sourceObject;
                    priceQuoteProperty().set(priceQuote);
                }
            }, (failed) -> {
                statusProperty().set(ERROR);
            });
        
        };

        m_lastExecution = m_timeExecutor.scheduleAtFixedRate(doUpdate, 0, getCyclePeriod(), getCycleTimeUnit());

    }
    */
    
    
    

 
  
}
