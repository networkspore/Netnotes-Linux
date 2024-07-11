package com.netnotes;

import static java.lang.System.currentTimeMillis;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.devskiller.friendly_id.FriendlyId;

import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

public class AmountMenuItem extends MenuItem {
    private String m_friendlyId = FriendlyId.createFriendlyId();
    private PriceAmount m_priceAmount;
    private long m_timeStamp = 0;
    

    private ChangeListener<BigDecimal> m_amountListener = null;

    private BufferedImage m_unitImage = null;
    private BufferedImage m_img = null;
    private Graphics2D m_g2d = null;
    private WritableImage m_wImg = null;
    

    public AmountMenuItem (PriceAmount priceAmount){
        super();
        m_priceAmount = priceAmount;
        ImageView imgView = new ImageView();
        setGraphic(imgView);

        m_amountListener = (obs,oldval,newval)->updateImage(imgView);

        m_priceAmount.amountProperty().addListener(m_amountListener);
     
        m_timeStamp = currentTimeMillis();
    }

   

    public void shutdown(){
        
        if(m_amountListener != null){
            m_priceAmount.amountProperty().removeListener(m_amountListener);
        }
    }


    public long getTimeStamp(){
        return m_timeStamp;
    }

    public void setTimeStamp(long timeStamp){
        m_timeStamp = timeStamp;
    }

    public PriceAmount getPriceAmount(){
        return m_priceAmount;
    }

    public String getFriendlyId(){
        return m_friendlyId;
    }

    public String getTokenId(){
        
        return m_priceAmount.getTokenId();
        
    }

    public void updateImage( ImageView imgView ) {
        PriceAmount priceAmount = m_priceAmount;
        BigDecimal amount = priceAmount.amountProperty().get();
        PriceCurrency currency = priceAmount.getCurrency();
        
       BigInteger integers = amount.toBigInteger();
        BigDecimal decimals = amount.subtract(new BigDecimal(integers));
        int decimalPlaces = currency.getFractionalPrecision();
        String currencyName =  currency.getSymbol();
        int space = currencyName.indexOf(" ");
        currencyName = space != -1 ? currencyName.substring(0, space) : currencyName;

     
        java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 30);
        java.awt.Font smallFont = new java.awt.Font("SANS-SERIF", java.awt.Font.PLAIN, 12);

        //   Image ergoBlack25 = new Image("/assets/ergo-black-25.png");
        //   SwingFXUtils.fromFXImage(ergoBlack25, null);
        
        String amountString = String.format("%d", integers) ;
        String decs = String.format("%." + decimalPlaces + "f", decimals);

        decs = decs.substring(1, decs.length());
        
    
    
        m_img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        m_g2d = m_img.createGraphics();
        
        m_g2d.setFont(font);
        FontMetrics fm = m_g2d.getFontMetrics();
        int padding = 5;
        int stringWidth = fm.stringWidth(amountString);
       
        int height = fm.getHeight() + 10;

        m_g2d.setFont(smallFont);

        fm = m_g2d.getFontMetrics();

        //  int priceAscent = fm.getAscent();
        int integersX = padding;
        int decimalsX = integersX + stringWidth + 1;

       // int currencyNameStringWidth = fm.stringWidth(currencyName);
        int decsWidth = fm.stringWidth(decs);
        int currencyNameWidth = fm.stringWidth(currencyName);

        int width = decimalsX + stringWidth + (decsWidth < currencyNameWidth ? currencyNameWidth : decsWidth) + 50;
        width = width < 100 ? 100 : width;
        int currencyNameStringX = decimalsX + 2;

        m_g2d.dispose();
        
        m_unitImage = SwingFXUtils.fromFXImage(currency.getIcon(), m_unitImage);
        Drawing.setImageAlpha(m_unitImage, 0x40);
        //  adrBuchImg.getScaledInstance(width, height, java.awt.Image.SCALE_AREA_AVERAGING);
        m_img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        m_g2d = m_img.createGraphics();
        m_g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        m_g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        m_g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        m_g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        m_g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        m_g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        m_g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        m_g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        //   m_g2d.setComposite(AlphaComposite.Clear);

        /* for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = new Color(adrBuchImg.getRGB(x, y), true);

                Color c2 = new Color(c.getRed(), c.getGreen(), c.getBlue(), 35);

                m_img.setRGB(x, y, c2.getRGB());
            }
        }
         */
        
        m_g2d.drawImage(m_unitImage, 20, (height / 2) - (m_unitImage.getHeight() / 2), m_unitImage.getWidth(), m_unitImage.getHeight(), null);

       



        m_g2d.setFont(font);
        fm = m_g2d.getFontMetrics();
        m_g2d.setColor(java.awt.Color.WHITE);

        

        m_g2d.drawString(amountString, integersX, fm.getAscent() + 5);

        m_g2d.setFont(smallFont);
        fm = m_g2d.getFontMetrics();
        m_g2d.setColor(new java.awt.Color(.9f, .9f, .9f, .9f));

       
        if(decimalPlaces > 0){
            //decimalsX = widthIncrease > 0 ? decimalsX + widthIncrease : decimalsX;
            m_g2d.drawString(decs, decimalsX , fm.getHeight() + 2);
        }

        
        m_g2d.drawString(currencyName, currencyNameStringX, height - 10);

        m_g2d.setFont(smallFont);
      
        fm = m_g2d.getFontMetrics();
     


        /*try {
            Files.writeString(logFile.toPath(), amountString + decs);
        } catch (IOException e) {

        }*/
        m_g2d.dispose();

       /* try {
            ImageIO.write(m_img, "png", new File("outputImage.png"));
        } catch (IOException e) {

        }*/

        imgView.setImage(SwingFXUtils.toFXImage(m_img, m_wImg));
        
    }
}
