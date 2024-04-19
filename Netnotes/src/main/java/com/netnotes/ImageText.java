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
import java.util.HashMap;

import javax.imageio.ImageIO;

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

    public final static String EXTEN_32_DIR = "/extended32";
    public final static String EXTEN_FILE_EXT = ".png";

    private BufferedImage m_imgTxtExtBuffImg;
    private Graphics2D m_imgTxtExtG2d;
    private java.awt.Font m_imgTxtExtFont;
    private FontMetrics m_imgTxtExtFontMetrics;
    
    private HashMap<Integer, BufferedImage> m_standardImgfontMap = null;
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
        
        updateStandardImageFontMap(fontString, weight, fontSize);
        setupExtendedFont(extFontString, extWeight, extFontSize);
    }

   

    private void updateStandardImageFontMap(String fontString, int weight, int fontSize){
        m_standardFontImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        m_standardFontG2d = m_standardFontImg.createGraphics();
        m_standardFont = new java.awt.Font(fontString, weight, fontSize);
        m_standardFontG2d.setFont(m_standardFont);
        m_standardFontMetrics = m_standardFontG2d.getFontMetrics();
        
        int height = m_standardFontMetrics.getHeight();
        int width = m_standardFontMetrics.getMaxAdvance();
        int ascent = m_standardFontMetrics.getMaxAscent();
        int leading = m_standardFontMetrics.getLeading();
        int[] widths = m_standardFontMetrics.getWidths();

        m_standardFontImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        m_standardFontG2d = m_standardFontImg.createGraphics();
        m_standardFontG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        m_standardFontG2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
     //   m_standardFontG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        m_standardFontG2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
     //   m_standardFontG2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
     //   m_standardFontG2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        m_standardFontG2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      //  m_standardFontG2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        m_standardFontG2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        m_standardFontG2d.setColor(new Color(0xffffffff,true));
        m_standardFontG2d.setFont(m_standardFont);

        m_standardImgfontMap = new HashMap<Integer, BufferedImage>();



        for(int i = STANDARD_CHAR_START; i <= STANDARD_CHAR_END ; i++){
            String charStr = Character.toString((char) i);
            
            Drawing.clearImage(m_standardFontImg);
            m_standardFontG2d.drawString(charStr, 0, ascent + leading);
            int w = widths[i];
            BufferedImage img = new BufferedImage(w, height, BufferedImage.TYPE_INT_ARGB);
            Drawing.drawImageExact(img, m_standardFontImg, 0, 0, false);
            m_standardImgfontMap.put(i, img);

        }
     
        m_standardFontG2d = null;
        m_standardFontImg = null;
        m_standardFont = null;
    }
  
   


    private void setupExtendedFont(String fontName, int fontWeight, int fontSize){
        
        m_imgTxtExtBuffImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

        m_imgTxtExtG2d = m_imgTxtExtBuffImg.createGraphics();
        m_imgTxtExtFont = new java.awt.Font(fontName, fontWeight, fontSize);
        m_imgTxtExtG2d.setFont(m_imgTxtExtFont);
        m_imgTxtExtFontMetrics = m_imgTxtExtG2d.getFontMetrics();

        int height = m_imgTxtExtFontMetrics.getHeight();
        int width = m_imgTxtExtFontMetrics.getMaxAdvance();
        m_imgTxtExtBuffImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        m_imgTxtExtG2d = m_imgTxtExtBuffImg.createGraphics();
        m_imgTxtExtG2d.setFont(m_imgTxtExtFont);
        m_imgTxtExtFontMetrics = m_imgTxtExtG2d.getFontMetrics();
        
        m_imgTxtExtG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        m_imgTxtExtG2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
     //   m_imgTxtExtG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        m_imgTxtExtG2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
     //   m_imgTxtExtG2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
     //   m_imgTxtExtG2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        m_imgTxtExtG2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      //  m_imgTxtExtG2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        m_imgTxtExtG2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        m_imgTxtExtG2d.setColor(new java.awt.Color(0xffffffff,true));


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

    public void drawStandardGlyph(int cpInt, BufferedImage lineImage, SimpleIntegerProperty x, int y){
        if(cpInt < STANDARD_CHAR_START || cpInt > STANDARD_CHAR_END){
            cpInt = 32;
        }
        BufferedImage chrImg = m_standardImgfontMap.get(cpInt);

        Drawing.drawImageExact(lineImage, chrImg, x.get(), y, true);
      
        x.set(x.get() + m_letterSpacing + chrImg.getWidth());
    }
   
    public void drawExtendedGlyph(int codePoint, BufferedImage lineImage, SimpleIntegerProperty x, int y){
        int ascent = m_imgTxtExtFontMetrics.getAscent();
        int leading = m_imgTxtExtFontMetrics.getLeading();
        

        String charStr = Character.toString((char) codePoint);
        Drawing.clearImage(m_imgTxtExtBuffImg);
        m_imgTxtExtG2d.drawString(charStr, 0, ascent + leading);
        int w = m_imgTxtExtFontMetrics.charWidth(codePoint);
        
        Drawing.drawImageExact(lineImage, m_imgTxtExtBuffImg, x.get(), y, w, m_imgTxtExtBuffImg.getHeight(), true);
        x.set(x.get()+ m_letterSpacing + w);

    }

    public HashMap<Integer, BufferedImage> getStandardImgfontMap(){
        return m_standardImgfontMap;
    }

    public FontMetrics getStandarFontMetrics(){
        return m_standardFontMetrics;
    }
}
