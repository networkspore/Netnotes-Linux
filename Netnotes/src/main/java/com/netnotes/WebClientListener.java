package com.netnotes;

import java.util.ArrayList;

import com.google.gson.JsonObject;

interface WebClientListener {

    void urlPriceData(String tunnelID, ArrayList<PriceData> priceDataList, NumberClass numberClass);

    void urlPriceDataFailed(String tunnelID);

    void updatePriceData(String tunnelId, String topicString, PriceData priceData);

    void ready();

    void message(String tunnelID, JsonObject obj);

    void close(int i, String s, boolean b);

    void error(Exception e);
}
