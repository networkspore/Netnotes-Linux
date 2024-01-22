package com.netnotes;

import com.google.gson.JsonObject;

public class KucoinSymbolData {

    private String m_symbol;

    public KucoinSymbolData(JsonObject jsonObject) {

    }

    public String getSymbol() {
        return m_symbol;
    }
}
