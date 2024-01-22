package com.netnotes;

public class ImageItem {

    private boolean m_enabled = false;
    private double m_price = 0;
    private int m_x1 = 0;
    private int m_x2 = 0;
    private int m_y1 = 0;
    private int m_y2 = 0;

    public ImageItem(double price, boolean enabled, int x1, int y1, int x2, int y2) {
        m_price = price;
        m_enabled = enabled;
        m_x1 = x1;
        m_y1 = y1;
        m_x2 = x2;
        m_y2 = y2;
    }

    public double getPrice() {
        return m_price;
    }

    public boolean enabled() {
        return m_enabled;
    }

    public int getX1() {
        return m_x1;
    }

    public int getX2() {
        return m_x2;
    }

    public int getY1() {
        return m_y1;
    }

    public int getY2() {
        return m_y2;
    }

}
