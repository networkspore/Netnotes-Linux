package com.netnotes;

import java.math.BigDecimal;

public class PriceDirection{
    private boolean m_direction;
    private BigDecimal m_price;

    public PriceDirection(BigDecimal price, boolean direction){
        m_direction = direction;
        m_price = price;
    }

    public boolean getDirection(){
        return m_direction;
    }
    public BigDecimal getPrice(){
        return m_price;
    }

    public void setDirection(boolean direction){
        m_direction = direction;
    }

    public void setPrice(BigDecimal price){
        m_price = price;
    }
}