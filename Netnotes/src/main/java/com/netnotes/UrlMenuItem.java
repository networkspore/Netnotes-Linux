package com.netnotes;

import javafx.scene.control.MenuItem;

public class UrlMenuItem extends MenuItem {

    private String m_market;
    private String m_urlString;
    private String m_priceBaseCurrency;
    private String m_priceTargetCurrency;
    private String m_websiteName;
    private int m_index;

    public UrlMenuItem(String websiteName, String market, String urlString, String baseCurrency, String targetCurrency, int index) {
        super(websiteName + ": " + market);
        m_market = market;
        m_websiteName = websiteName;
        m_urlString = urlString;
        m_priceBaseCurrency = baseCurrency;
        m_priceTargetCurrency = targetCurrency;
        m_index = index;
    }

    public int getIndex() {
        return m_index;
    }

    public String getMarket() {
        return m_market;
    }

    public String getWebsiteName() {
        return m_websiteName;
    }

    public void setUrlString(String urlString) {
        m_urlString = urlString;
    }

    public String getUrlString() {
        return m_urlString;
    }

    public String getBaseCurrency() {
        return m_priceBaseCurrency;
    }

    public String getTargetCurrency() {
        return m_priceTargetCurrency;
    }
}
