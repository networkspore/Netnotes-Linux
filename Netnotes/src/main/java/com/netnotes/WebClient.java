package com.netnotes;

import java.util.ArrayList;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public interface WebClient {

    void requestSocket(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed);

    //void subscribeToCandles(String symbol, String timespan);
    void subscribeToCandles(String tunnelId, String symbol, String timespan);

    void unsubscribeToCandles(String tunnelId, String symbol, String timespan);

    void subescribeToTicker(String tunnelId, String symbol);

    void unsubscribeToTicker(String tunnelId, String clientID, String symbol);

    Exception terminate();

    boolean isReady();

}
