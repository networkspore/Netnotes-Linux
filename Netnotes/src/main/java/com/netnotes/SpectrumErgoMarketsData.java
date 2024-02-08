package com.netnotes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import com.devskiller.friendly_id.FriendlyId;

import com.google.gson.JsonObject;

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

    
   
    public void updateMarkets(SpectrumMarketData[] marketData){
        //SpectrumFinance.getMarketDataBySymbols(marketData, "ERG", "SigUSD")
        //priceQuoteProperty().set();
        if(marketData != null){
            SpectrumMarketData data = SpectrumFinance.getMarketDataBySymbols(marketData, "ERG", "SigUSD");
            
            priceQuoteProperty().set(data);
            marketDataProperty().set(marketData);
        }else{
            priceQuoteProperty().set(null);            
            marketDataProperty().set(null);
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
    public void connectToExchange(SpectrumFinance spectrum){
        statusProperty().set(ErgoMarketsData.STARTED);
       
        
        m_msgListener = new SpectrumMarketInterface() {
            
            
          
            public String getId() {
                return m_exchangeId;
            }
        
            public void marketArrayChange(SpectrumMarketData[] dataArray) {
                
                if(dataArray != null){
                    updateMarkets(dataArray);
                }else{
                    priceQuoteProperty().set(null);
                }
            }


        };

        spectrum.addMsgListener(m_msgListener);
        
        setShutdownListener((obs, oldval, newVal) -> {
            try {
                Files.writeString(logFile.toPath(), "\nSpecErgoMarkets stopping", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
           
            }
            spectrum.removeMsgListener(m_msgListener);
            statusProperty().set(ErgoMarketsData.STOPPED);
            getShutdownNow().removeListener(getShutdownListener());
            priceQuoteProperty().set(null);
        });
       // spectrum.
        
    }


}