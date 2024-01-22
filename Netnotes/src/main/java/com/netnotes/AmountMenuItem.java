package com.netnotes;

import static java.lang.System.currentTimeMillis;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.devskiller.friendly_id.FriendlyId;


import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;

public class AmountMenuItem extends MenuItem {
    private String m_friendlyId = FriendlyId.createFriendlyId();
    private SimpleObjectProperty<PriceAmount> m_priceAmount = new SimpleObjectProperty<>(null);
    private SimpleObjectProperty<Image> m_bufferedImage = new SimpleObjectProperty<>(null);
    private long m_timeStamp = 0;

    public AmountMenuItem (PriceAmount priceAmount){
        super();
        

        m_bufferedImage.addListener((obs,oldval,newval)->{
            setText(m_priceAmount.get().getCurrency().getName());
            setGraphic(newval != null ? IconButton.getIconView(newval, newval.getWidth() ): null);
        });
        m_priceAmount.addListener((obs,oldval,newval)->updateImage());

        m_priceAmount.set(priceAmount);
        m_timeStamp = currentTimeMillis();
    }

    public long getTimeStamp(){
        return m_timeStamp;
    }

    public void setTimeStamp(long timeStamp){
        m_timeStamp = timeStamp;
    }

    public SimpleObjectProperty<PriceAmount> priceAmountProperty(){
        return m_priceAmount;
    }

    public SimpleObjectProperty<Image> bufferedImageProperty(){
        return m_bufferedImage;
    }
    public String getFriendlyId(){
        return m_friendlyId;
    }

    public String getTokenId(){
        PriceAmount priceAmount = m_priceAmount.get();
        if(priceAmount != null && priceAmount.getCurrency() != null){
           
            return priceAmount.getCurrency().getTokenId();
        }
            
        return null;
        
    }

    public void updateImage( ) {
        PriceAmount priceAmount = m_priceAmount.get();
        boolean quantityValid = priceAmount != null && priceAmount.getAmountValid();
       // BigDecimal priceAmountDecimal = priceAmount != null && quantityValid ? priceAmount.getBigDecimalAmount() : BigDecimal.valueOf(0);

  
        
       BigInteger integers = priceAmount != null ? priceAmount.getBigDecimalAmount().toBigInteger() : BigInteger.ZERO;
        BigDecimal decimals = priceAmount != null ? priceAmount.getBigDecimalAmount().subtract(new BigDecimal(integers)) : BigDecimal.ZERO;
        int decimalPlaces = priceAmount != null ? priceAmount.getCurrency().getFractionalPrecision() : 0;
        String currencyName = priceAmount != null ? priceAmount.getCurrency().getSymbol() : "UKNOWN";
        int space = currencyName.indexOf(" ");
        currencyName = space != -1 ? currencyName.substring(0, space) : currencyName;

     
        java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 30);
        java.awt.Font smallFont = new java.awt.Font("SANS-SERIF", java.awt.Font.PLAIN, 12);

        //   Image ergoBlack25 = new Image("/assets/ergo-black-25.png");
        //   SwingFXUtils.fromFXImage(ergoBlack25, null);
        
        String amountString = quantityValid ? String.format("%d", integers) : " -";
        String decs = String.format("%." + decimalPlaces + "f", decimals);

        decs = quantityValid ? decs.substring(1, decs.length()) : "";
        
    
    
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int padding = 5;
        int stringWidth = fm.stringWidth(amountString);
       
        int height = fm.getHeight() + 10;

        g2d.setFont(smallFont);

        fm = g2d.getFontMetrics();

        //  int priceAscent = fm.getAscent();
        int integersX = padding;
        int decimalsX = integersX + stringWidth + 1;

       // int currencyNameStringWidth = fm.stringWidth(currencyName);
        int decsWidth = fm.stringWidth(decs);
        int currencyNameWidth = fm.stringWidth(currencyName);

        int width = decimalsX + stringWidth + (decsWidth < currencyNameWidth ? currencyNameWidth : decsWidth) + 50;
        width = width < 100 ? 100 : width;
        int currencyNameStringX = decimalsX + 2;

        g2d.dispose();
        
        BufferedImage unitImage = SwingFXUtils.fromFXImage(priceAmount != null ? priceAmount.getCurrency().getIcon() : new Image("/assets/unknown-unit.png"), null);
        Drawing.setImageAlpha(unitImage, 0x40);
        //  adrBuchImg.getScaledInstance(width, height, java.awt.Image.SCALE_AREA_AVERAGING);
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        //   g2d.setComposite(AlphaComposite.Clear);

        /* for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = new Color(adrBuchImg.getRGB(x, y), true);

                Color c2 = new Color(c.getRed(), c.getGreen(), c.getBlue(), 35);

                img.setRGB(x, y, c2.getRGB());
            }
        }
         */
        
        g2d.drawImage(unitImage, 20, (height / 2) - (unitImage.getHeight() / 2), unitImage.getWidth(), unitImage.getHeight(), null);

       



        g2d.setFont(font);
        fm = g2d.getFontMetrics();
        g2d.setColor(java.awt.Color.WHITE);

        

        g2d.drawString(amountString, integersX, fm.getAscent() + 5);

        g2d.setFont(smallFont);
        fm = g2d.getFontMetrics();
        g2d.setColor(new java.awt.Color(.9f, .9f, .9f, .9f));

       
        if(decimalPlaces > 0){
            //decimalsX = widthIncrease > 0 ? decimalsX + widthIncrease : decimalsX;
            g2d.drawString(decs, decimalsX , fm.getHeight() + 2);
        }

        
        g2d.drawString(currencyName, currencyNameStringX, height - 10);

        g2d.setFont(smallFont);
      
        fm = g2d.getFontMetrics();
     


        /*try {
            Files.writeString(logFile.toPath(), amountString + decs);
        } catch (IOException e) {

        }*/
        g2d.dispose();

       /* try {
            ImageIO.write(img, "png", new File("outputImage.png"));
        } catch (IOException e) {

        }*/

        m_bufferedImage.set(SwingFXUtils.toFXImage(img, null));
        
    }
}
