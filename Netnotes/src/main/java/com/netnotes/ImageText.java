package com.netnotes;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;

import io.netnotes.engine.Drawing;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ImageText {
    public final static int STANDARD_CHAR_START = 32;
    public final static int STANDARD_CHAR_END = 126; 

    public final static int EMOJI_START = 127183;

    public final static int DEFAULT_LETTER_SPACING_OFFSET = 0;

    public final static String EMOJI_32_DIR = "/emoji32";
    public final static String EMOJI_FILE_PREFIX = "emoji_u";
    public final static String EMOJI_FILE_EXT = ".png";
   
    public final static String[] EMOJI_MODIFIERS = new String[]{"1f3fb", "1f3fc", "1f3fd", "1f3fe", "1f3ff"};
    public final static String EMOJI_JOINER = "200d";

    public final static Color RENDER_COLOR = new Color(0xffffffff, true);
    public final static Color SHADOW_COLOR = new Color(0xff000000, true);

    private BufferedImage m_imgTxtExtBuffImg;
    private Graphics2D m_imgTxtExtG2d;
    private java.awt.Font m_imgTxtExtFont;
    private FontMetrics m_imgTxtExtFontMetrics;
    
   // private HashMap<Integer, BufferedImage> m_standardImgfontMap = null;
    private String[] m_emojiCodes = new String[]{};



    private BufferedImage m_standardFontImg = null;
    private Graphics2D m_standardFontG2d = null;
    private FontMetrics m_standardFontMetrics = null;
    private Font m_standardFont = null;


    private int m_letterSpacing = DEFAULT_LETTER_SPACING_OFFSET; 

    public ImageText(){
        this("OCR A Extended",java.awt.Font.BOLD, 15,"Deja Vu Sans", java.awt.Font.PLAIN,15);
    }

    public ImageText(String fontString,int weight, int fontSize, String extFontString, int extWeight, int extFontSize){
        
        setupStandardFont(fontString, weight, fontSize);
        setupExtendedFont(extFontString, extWeight, extFontSize);
    }

   

    private void setupStandardFont(String fontName, int fontWeight, int fontSize){
        
        m_standardFontImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        m_standardFontG2d = m_standardFontImg.createGraphics();
        m_standardFont = new Font(fontName, fontWeight, fontSize);
        m_standardFontG2d.setFont(m_standardFont);
        m_standardFontMetrics = m_standardFontG2d.getFontMetrics();

        int height = m_standardFontMetrics.getHeight();
        int width = m_standardFontMetrics.getMaxAdvance();
        m_standardFontImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        m_standardFontG2d = m_standardFontImg.createGraphics();
        m_standardFontG2d.setFont(m_standardFont);
        m_standardFontMetrics = m_standardFontG2d.getFontMetrics();
        
        setDefaultRendinghints(m_standardFontG2d);





    }
   


    private void setupExtendedFont(String fontName, int fontWeight, int fontSize){
        
        m_imgTxtExtBuffImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

        m_imgTxtExtG2d = m_imgTxtExtBuffImg.createGraphics();
        m_imgTxtExtFont = new Font(fontName, fontWeight, fontSize);
        m_imgTxtExtG2d.setFont(m_imgTxtExtFont);
        m_imgTxtExtFontMetrics = m_imgTxtExtG2d.getFontMetrics();

        int height = m_imgTxtExtFontMetrics.getHeight();
        int width = m_imgTxtExtFontMetrics.getMaxAdvance();
        m_imgTxtExtBuffImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        m_imgTxtExtG2d = m_imgTxtExtBuffImg.createGraphics();
        m_imgTxtExtG2d.setFont(m_imgTxtExtFont);
        m_imgTxtExtFontMetrics = m_imgTxtExtG2d.getFontMetrics();
        
        setDefaultRendinghints(m_imgTxtExtG2d);




    }

    public void setDefaultRendinghints(Graphics2D g2d){
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
     //   g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
     //   g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
     //   g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      //  g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }


    public BufferedImage updateLineImage(int x, int textY, BufferedImage lineImage, String str){

        SimpleObjectProperty<BufferedImage> emojiLayer = new SimpleObjectProperty<>(null);
        SimpleIntegerProperty xP = new SimpleIntegerProperty(x);

        try {
          
            int prevInt = -1;
            int i = 0;
            while(i < str.length()){
                int chrInt = (int) str.charAt(i);
                if(chrInt > STANDARD_CHAR_END){
                    if(prevInt == -1 && (chrInt < 55296 || chrInt > 57343)){

                        drawExtendedGlyph(chrInt, lineImage, xP, textY);

                    }else{
                        if(prevInt == -1){

                            prevInt = chrInt;

                        }else{
                            int codePoint = prevInt != -1 ? Character.toCodePoint((char)prevInt, (char)chrInt) : chrInt;
                            
                            String cpHexString = Integer.toHexString(codePoint);
                         
                            prevInt = -1;
                            if(codePoint < EMOJI_START){
                                drawExtendedGlyph(chrInt, lineImage, xP, textY);
                            }else{
                                drawEmoji(cpHexString, lineImage, emojiLayer, xP, 0);
                            }
                        }
                    }
                    
                }else{
                    drawStandardGlyph(chrInt, lineImage, xP, textY);
                }
                
                i++;
                  
            }
        } catch (IOException e) {

        }

        return emojiLayer.get();
    }


    public boolean isEmojiCode(String emojiCode){
        if(emojiCode != null){
            for(String f : m_emojiCodes){
                if(emojiCode.equals(f)){
                    
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isModifierCode(String cpHex){
        for(String s : EMOJI_MODIFIERS){
            if(s.equals(cpHex) || s.equals(EMOJI_JOINER)){
                return true;
            }
        }
        return false;
    }

    public boolean drawEmoji(String emojiCode, BufferedImage lineImage, SimpleObjectProperty<BufferedImage> emojiLayer, SimpleIntegerProperty x, int y) throws IOException{
        String fileString = EMOJI_32_DIR + "/" + EMOJI_FILE_PREFIX + emojiCode + EMOJI_FILE_EXT;
     
        //File emojiFile = new File(fileString);

        URL url = App.class.getResource(fileString);
        if(url != null){
            if(emojiLayer.get() == null){
                emojiLayer.set( new BufferedImage(lineImage.getWidth(), lineImage.getHeight(), BufferedImage.TYPE_INT_ARGB));
            }
            
            InputStream inputStream = url.openStream();

            BufferedImage emojiImg = ImageIO.read(inputStream);
            inputStream.close();
            
            Drawing.drawImageExact(emojiLayer.get(), emojiImg, x.get() , y, false);
            x.set(x.get()+emojiImg.getWidth() + 1);
            return true;       
        }
        return false;
    }

   
    public void drawStandardGlyph(int codePoint, BufferedImage lineImage, SimpleIntegerProperty x, int y){
        int ascent = m_standardFontMetrics.getAscent();
        int leading = m_standardFontMetrics.getLeading();
        

        String charStr = Character.toString((char) codePoint);
        Drawing.clearImage(m_standardFontImg);
        m_standardFontG2d.setColor(SHADOW_COLOR);
        m_standardFontG2d.drawString(charStr, 2, ascent + leading+2);
        m_standardFontG2d.setColor(RENDER_COLOR);
        m_standardFontG2d.drawString(charStr, 0, ascent + leading);
        int w = m_standardFontMetrics.charWidth(codePoint);
        
        Drawing.drawImageExact(lineImage, m_standardFontImg, x.get(), y, w, m_standardFontImg.getHeight(), true);
        x.set(x.get()+ m_letterSpacing + w);

    }
   
    public void drawExtendedGlyph(int codePoint, BufferedImage lineImage, SimpleIntegerProperty x, int y){
        int ascent = m_imgTxtExtFontMetrics.getAscent();
        int leading = m_imgTxtExtFontMetrics.getLeading();
        

        String charStr = Character.toString((char) codePoint);
        Drawing.clearImage(m_imgTxtExtBuffImg);
        m_imgTxtExtG2d.setColor(SHADOW_COLOR);
        m_imgTxtExtG2d.drawString(charStr, 2, ascent + leading+2);
        m_imgTxtExtG2d.setColor(RENDER_COLOR);
        m_imgTxtExtG2d.drawString(charStr, 0, ascent + leading);

        int w = m_imgTxtExtFontMetrics.charWidth(codePoint);
        
        Drawing.drawImageExact(lineImage, m_imgTxtExtBuffImg, x.get(), y, w, m_imgTxtExtBuffImg.getHeight(), true);
        x.set(x.get()+ m_letterSpacing + w);

    }

    public FontMetrics getStandarFontMetrics(){
        return m_standardFontMetrics;
    }

    public FontMetrics getExtendedFontMetrics(){
        return m_imgTxtExtFontMetrics;
    }
}
