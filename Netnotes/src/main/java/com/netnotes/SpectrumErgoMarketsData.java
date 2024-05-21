package com.netnotes;

import java.time.LocalDateTime;

import com.devskiller.friendly_id.FriendlyId;

import com.google.gson.JsonObject;

import javafx.beans.value.ChangeListener;

public class SpectrumErgoMarketsData extends ErgoMarketsData{
    

    public SpectrumErgoMarketsData(ErgoMarketsList marketsList){
        super("Spectrum Finance", SpectrumFinance.NETWORK_ID, "ERG", "sigUSD", ErgoMarketsData.POLLED, ErgoMarketsData.TICKER, marketsList);
    }
    public SpectrumErgoMarketsData(ErgoMarketsList marketsList, JsonObject json) throws NullPointerException {
        super(marketsList, json);
    }

    @Override
    public void start() {
        if (getMarketId() != null &&  !statusProperty().get().equals(STARTED)) {
            NoteInterface marketInterface = getMarketsList().getErgoMarkets().getNetworksData().getNoteInterface(getMarketId());
           
            if (marketInterface instanceof SpectrumFinance) {
                SpectrumFinance exchange = (SpectrumFinance) marketInterface;
                connectToExchange(exchange);
            }
                
        }
    }

    
   

    SpectrumMarketInterface m_msgListener;

    /*public void getMarketArray(SpectrumFinance exchange){
        SimpleObjectProperty<SpectrumMarketData[]> data = new SimpleObjectProperty<>();
        
        data.addListener((obs,oldval,newval)->{
            onTickerMsg(newval);
        });
        exchange.getMarketArray(data);


    }*/
    private String m_exchangeId = FriendlyId.createFriendlyId();
    private SpectrumMarketData m_marketData = null;
    
    public void connectToExchange(SpectrumFinance spectrum){
        statusProperty().set(ErgoMarketsData.STARTED);

        Runnable getQuote = ()->{
            if(m_marketData != null){
                PriceQuote priceQuote = m_marketData.getPriceQuote();
                priceQuoteProperty().set(priceQuote);
               
            }else{
                priceQuoteProperty().set(null);            
                
            }
            updatedProperty().set(LocalDateTime.now());
        };


        Runnable setMarketData = ()->{

            m_marketData = spectrum.getMarketDataBySymbols("SigUSD", "ERG");

            getQuote.run();

        };

        if(spectrum.getConnectionStatus() == SpectrumFinance.STARTED){
            setMarketData.run();
        }


        m_msgListener = new SpectrumMarketInterface() {
            
            public String getId() {
                return m_exchangeId;
            }
            public void sendMessage(int msg, long timestamp){
                switch(msg){
                    case SpectrumFinance.STARTED:
                        setMarketData.run();
                    break;
                    case SpectrumFinance.STOPPED:
                        m_marketData = null;
                        getQuote.run();
                    break;
                    case SpectrumFinance.LIST_CHANGED:
                    case SpectrumFinance.LIST_UPDATED:
                        getQuote.run();
                    break;
                }
            }
            public void sendMessage(int code, long timestamp, String msg){
                
            }
        };

        spectrum.addMsgListener(m_msgListener);

       
        setShutdownListener((obs, oldval, newVal) -> {
            
            spectrum.removeMsgListener(m_msgListener);
            statusProperty().set(ErgoMarketsData.STOPPED);
            getShutdownNow().removeListener(getShutdownListener());
            m_marketData = null;
            priceQuoteProperty().set(null);

        });
       // spectrum.
        
    }

    public SpectrumFinance getSpectrumFinance(){
        if (getMarketId() != null) {
            NoteInterface marketInterface = getMarketsList().getErgoMarkets().getNetworksData().getNoteInterface(getMarketId());
        
            if (marketInterface instanceof SpectrumFinance) {
                return (SpectrumFinance) marketInterface;
               
            }
                
        }
        return null;
    }

    @Override
    public PriceQuote getPriceQuoteById(String baseId, String quoteId){
        SpectrumFinance exchange = getSpectrumFinance();
 
        if(exchange != null){

            return exchange.getPriceQuoteById(baseId, quoteId);
        }
        return null;
    }
}