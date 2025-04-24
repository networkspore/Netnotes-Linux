package com.netnotes;

import io.netnotes.engine.PriceQuote;

public interface QuoteListener {

    void onNewQuote(PriceQuote quote);
}
