package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

public class SpectrumErgoMarketsData extends ErgoMarketsData{
    
    private String m_id = FriendlyId.createFriendlyId();


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

    
   
    public void onTickerMsg(SpectrumMarketData[] marketData){
        //SpectrumFinance.getMarketDataBySymbols(marketData, "ERG", "SigUSD")
        //priceQuoteProperty().set();
        SpectrumMarketData data = SpectrumFinance.getMarketDataBySymbols(marketData, "ERG", "SigUSD");
        priceQuoteProperty().set(data);
        try {
            Files.writeString(logFile.toPath(), "ErgoMarkets Spectrum onTickerMsg: " + (data != null ? data.getBaseId() : "data null"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

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
    public void connectToExchange(SpectrumFinance spectrum){
        statusProperty().set(STARTED);
        
  

        m_msgListener = new SpectrumMarketInterface() {
            
            
          
            public String getId() {
                return m_id;
            }
        
            public void marketArrayChange(SpectrumMarketData[] dataArray) {
                onTickerMsg(dataArray);
            }


        };

        spectrum.addMsgListener(m_msgListener);

        setShutdownListener((obs, oldval, newVal) -> {
            spectrum.removeMsgListener(m_msgListener);
            statusProperty().set(STOPPED);
        });
       // spectrum.
        
    }

    @Override
    public void shutdown(){
        if (getMarketId() != null &&  !statusProperty().get().equals(STARTED)) {
            NoteInterface marketInterface = getMarketsList().getErgoMarkets().getNetworksData().getNoteInterface(getMarketId());
           
            if (marketInterface instanceof SpectrumFinance) {
            }
        }
        super.shutdown();
    }
}