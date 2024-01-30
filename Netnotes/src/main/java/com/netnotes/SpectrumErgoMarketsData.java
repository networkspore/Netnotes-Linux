package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.beans.property.SimpleObjectProperty;
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

                switch (getUpdateType()) {
                    case POLLED:
                        startPolling(exchange);
                        break;       
                }
            }
                
        }
    }

    private ChangeListener<LocalDateTime> m_timeCycleListener = null;
    

    public void startPolling(SpectrumFinance spectrum){
        statusProperty().set(STARTED);
        Runnable poll = () ->{    

            spectrum.getCMCMarkets(success -> {
                SimpleObjectProperty<SpectrumMarketData> newPriceQuoteProperty = new SimpleObjectProperty<>();
                Object sourceObject = success.getSource().getValue();
                if (sourceObject != null && sourceObject instanceof JsonArray) {
                
                    JsonArray jsonArray = (JsonArray) sourceObject;
                    ArrayList<SpectrumMarketData> marketDataList = new ArrayList<>();
                    
                    for(int i = 0; i < jsonArray.size();i++) {
                        
                        JsonElement marketObjectElement = jsonArray.get(i);
                        if (marketObjectElement != null && marketObjectElement.isJsonObject()) {

                            JsonObject marketDataJson = marketObjectElement.getAsJsonObject();
                            
                            try{
                                
                                SpectrumMarketData marketData = new SpectrumMarketData(marketDataJson);
                                                
                                
                            
                                if(marketData.getQuoteCurrency().equals("SigUSD") && marketData.getTransactionCurrency().equals("ERG")){
                                    
                                    newPriceQuoteProperty.set(marketData);
                                }else{
                                    marketDataList.add(marketData);
                                }
                
                            }catch(Exception e){
                                try {
                                    Files.writeString(logFile.toPath(), "\nSpectrumFinance(updateMarkets): " + e.toString() + " " + marketDataJson.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                } catch (IOException e1) {
                                
                                }
                            }
                            
                        }

                    }          
                    
                    if(newPriceQuoteProperty.get() !=null){
                        SpectrumMarketData smD = newPriceQuoteProperty.get();
                        SpectrumMarketData[] txMarketArray = new SpectrumMarketData[marketDataList.size()];
                        txMarketArray = marketDataList.toArray(txMarketArray);
                        smD.setPriceQuotes(txMarketArray);
                        priceQuoteProperty().set(smD);
                    }   
                            }
            },(onFailed)->{
                try {
                    Files.writeString(logFile.toPath(), "\nSpectrumErgoMarketsData Polling Failed: " + onFailed.getSource().getException().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    
                }
            });
                
        };
        m_timeCycleListener = (obs, oldval, newval) -> poll.run();
        poll.run();
        getMarketsList().getErgoMarkets().getNetworksData().timeCycleProperty().addListener(m_timeCycleListener);       
    }

    @Override
    public void shutdown(){
        if(m_timeCycleListener != null){
            getMarketsList().getErgoMarkets().getNetworksData().timeCycleProperty().removeListener(m_timeCycleListener); 
            m_timeCycleListener = null;
        }
        super.shutdown();
    }
}