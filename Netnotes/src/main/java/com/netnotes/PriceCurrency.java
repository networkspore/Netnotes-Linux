package com.netnotes;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import com.utils.Utils;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class PriceCurrency {

    public final static int VALID_TIMEOUT = 1000*60;
    public final static String DEFAULT_TOKEN_URL = "https://explorer.ergoplatform.com/en/token/";

    private String m_defaultCurrencyName = ""; 
    private String m_tokenId = null;
    private String m_symbol = null;
    private String m_name = null;
    private String m_imageString = "/assets/unknown-unit.png";
    private int m_fractionalPrecision = 2;
    private long m_timestamp = 0;
    private String m_networkType = null;
    
    private SimpleStringProperty m_fontSymbol = new SimpleStringProperty("");
    private SimpleStringProperty m_description = new SimpleStringProperty("");
    private SimpleLongProperty m_emissionAmount = new SimpleLongProperty(0);
    private SimpleStringProperty m_tokenType = new SimpleStringProperty("");
    private SimpleStringProperty m_url = new SimpleStringProperty();

    private HashData m_hashData = null;

    public final SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>(null); 

    public PriceCurrency(String tokenId, String name, int decimals, String tokenType, String networkType){
        m_tokenId = tokenId;
        m_name = name;
        m_symbol = name;
        m_tokenType.set(tokenType);
        m_imageString = "/assets/unknown-unit.png";
        m_fractionalPrecision = decimals;
        m_networkType = networkType;
        m_url.set(DEFAULT_TOKEN_URL + m_tokenId);
    }


    public PriceCurrency(JsonObject json){
        openJson(json);
    }

   /* public void setErgoTokens(ErgoTokens ergoTokens){
        m_ergoTokens = ergoTokens;
        update();
    } */


    public PriceCurrency(String token_id, String name, String symbol, String description, int fractionalPrecision, String unitImageString, String networkType, long emissionAmount, long timestamp) {
        this(token_id, name, symbol,  fractionalPrecision, networkType, unitImageString, "","");
        m_timestamp = timestamp;
        m_description.set(description);
        m_emissionAmount.set(emissionAmount);
    }

    public PriceCurrency(String token_id, String name, String symbol, int fractionalPrecision,  String networkType, String unitImageString, String tokenType, String fontSymbol) {
        m_tokenId = token_id;
        m_name = name;
        m_symbol = symbol;
        m_imageString = unitImageString;
        m_fractionalPrecision = fractionalPrecision;
        m_fontSymbol.set(fontSymbol);
        m_tokenType.set(tokenType);
        m_networkType = networkType;

        if( m_imageString != null && !m_imageString.startsWith("/assets")){
            File file = new File(m_imageString);
            if(file.isFile()){
                try {

                    m_hashData = new HashData(file);
                } catch (IOException e) {

                    try {
                        Files.writeString(new File("netnotes-log.txt").toPath(), "\nPrice currency " + m_name + " hashdata failed.", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e1) {

                    }
                    m_hashData = null;
                }

            }else{
                m_hashData = null;
            }        
        }
    }

    public void openJson(JsonObject json) {

        JsonElement emissionElement = json.get("emissionAmount");
        JsonElement descriptionElement = json.get("description");
        JsonElement decimalsElement = json.get("decimals");
        JsonElement timeStampElement = json.get("timeStamp");
        JsonElement symbolElement = json.get("symbol");
        JsonElement fontSymbolElement = json.get("fontSymbol");
        JsonElement imageStringElement = json.get("imageString");
        JsonElement hashDataElement = json.get("hashData");

        
  
        m_fractionalPrecision = decimalsElement.getAsInt();
        m_symbol = symbolElement.getAsString();
        m_emissionAmount.set(emissionElement != null && emissionElement.isJsonPrimitive() ? emissionElement.getAsLong() : 0);
        m_description.set(descriptionElement != null && descriptionElement.isJsonPrimitive() ? descriptionElement.getAsString(): "");
        m_timestamp = timeStampElement != null && timeStampElement.isJsonPrimitive() ? timeStampElement.getAsLong() : 0;

        m_fontSymbol.set(fontSymbolElement != null && fontSymbolElement.isJsonPrimitive() ? fontSymbolElement.getAsString() : "");

        if (hashDataElement != null && hashDataElement.isJsonObject()) {
            m_hashData = new HashData(hashDataElement.getAsJsonObject());
        }

        if (imageStringElement != null && getImgHashData() != null) {
            setImageString(imageStringElement.getAsString());
        }
    }
        
    public SimpleStringProperty fontSymbolProperty(){
        return m_fontSymbol;
    } 
    public SimpleStringProperty descriptionProperty(){
        return m_description;
    }
    public SimpleLongProperty emissionAmountProperty(){
        return m_emissionAmount;
    } 
    public SimpleStringProperty tokenTypeProperty(){
        return m_tokenType;
    } 
    public SimpleStringProperty urlProperty(){
        return m_url;
    }
  


    public String getDefaultName(){
        return m_defaultCurrencyName;
    }

    public void setDefaultName(String name){
        m_defaultCurrencyName = name;
    }

    public HashData getImgHashData(){
        return m_hashData;
    }

    public void setImgHashData(HashData hashData){
        m_hashData = hashData;
    }

    public SimpleObjectProperty<LocalDateTime> getLastUpdated(){
        return m_lastUpdated;
    }

    public String getTokenType(){
        return m_tokenType.get();
    }

    public void setTokenType(String tokenType){
        m_tokenType.set(tokenType);
    }
   
    public void setDecimals(int decimals){
        m_fractionalPrecision = decimals;
    }

    public int getDecimals(){
        return m_fractionalPrecision;
    }


    public void setEmissionAmount(long amount){
        m_emissionAmount.set(amount);
    }
    public long getEmissionAmount(){
        return m_emissionAmount.get();
    }

    public void setDescription(String description){
        m_description.set(description);
    }

    public String getDescription(){
        return m_description.get();
    }

    public String getNetworkTypeString(){
        return m_networkType;
    }
    
    public void setTimeStamp(long timestamp){
        m_timestamp = timestamp;
    }

    public long getTimeStamp(){
        return m_timestamp;
    }

    public LocalDateTime getLocalTimeStamp(){
        return m_timestamp == 0 ? null : Utils.milliToLocalTime(m_timestamp);
    }

    public String getFontSymbol(){
        return m_fontSymbol.get();
    }
  
    public void setUrl(String url){
        m_url.set(url);
    }
    public String getUrl(){
        
        return m_url.get();
    }
 

    public String getTokenId() {
        return m_tokenId;
    }

    public Image getIcon() {
        if (m_symbol != null && m_name != null && m_imageString != null) {
            if(m_imageString.startsWith("/assets")){
                return new Image(m_imageString);
            }else{
                return Utils.checkAndLoadImage(getImageString(), getImgHashData());
            }

        } else {
            return getUnknownUnitImage();
        }
    }

    private static BufferedImage m_bgImg = null;
    private static Graphics2D m_g2d = null;

    public static Image getBlankBgIcon(double height, String symbol){
        java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 10);

        
        m_bgImg = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
        m_g2d = m_bgImg.createGraphics();
        m_g2d.setFont(font);
        FontMetrics fontMetrics = m_g2d.getFontMetrics();
        //int fontHeight = fontMetrics.getHeight();
        //int fontAscent = fontMetrics.getAscent();
        int symbolStringWidth = fontMetrics.stringWidth(symbol) + 10;

        m_bgImg = new BufferedImage(symbolStringWidth,(int) height, BufferedImage.TYPE_INT_ARGB);

        m_g2d = m_bgImg.createGraphics();
        m_g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        m_g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        m_g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        m_g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        m_g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        m_g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        m_g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        m_g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        m_g2d.setColor(new Color(0xcdd4da));
        m_g2d.drawString(symbol, 5, (int)height - fontMetrics.getDescent());

        Image img =  SwingFXUtils.toFXImage(m_bgImg, null);
        m_g2d.dispose();
        m_bgImg = null;
        return img;
    }

    public Image getBackgroundIcon(double height){
        String symbol = getSymbol();
        java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 10);

        
        BufferedImage unitImage = SwingFXUtils.fromFXImage( getIcon(), null);
        Drawing.setImageAlpha(unitImage, 0x30);

        int imgWidth = unitImage.getWidth();
        int imgHeight = unitImage.getHeight();
        
        Dimension scaledDimension = Drawing.getScaledDimension(new Dimension(imgWidth, imgHeight),new Dimension((int)(height + (height*.5)), (int)(height + (height*.5))));
        
        m_bgImg = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
        m_g2d = m_bgImg.createGraphics();
        m_g2d.setFont(font);
        FontMetrics fontMetrics = m_g2d.getFontMetrics();
        //int fontHeight = fontMetrics.getHeight();
        //int fontAscent = fontMetrics.getAscent();
        int symbolStringWidth = fontMetrics.stringWidth(symbol) + 10;

        m_bgImg = new BufferedImage(imgWidth < symbolStringWidth ? symbolStringWidth : imgWidth,(int) height, BufferedImage.TYPE_INT_ARGB);

        m_g2d = m_bgImg.createGraphics();
        m_g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        m_g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        m_g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        m_g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        m_g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        m_g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        m_g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        m_g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        m_g2d.drawImage(unitImage, (int)-Math.ceil(imgWidth *.2)  , (int) ((height / 2) - (scaledDimension.getHeight() / 2)), (int) scaledDimension.getWidth(), (int) scaledDimension.getHeight(), null);

        m_g2d.setColor(new Color(0xcdd4da));
        m_g2d.drawString(symbol, 5, (int)height - fontMetrics.getDescent());

        Image img =  SwingFXUtils.toFXImage(m_bgImg, null);
        m_g2d.dispose();
        m_bgImg = null;
        return img;
    }

    public static Image getUnknownUnitImage() {
        return new Image("/assets/unknown-unit.png");
    }

    public String getImageString(){
        return m_imageString;
    }
    

    public void setImageString(String imgString){
        m_imageString = imgString;
    }

    public void setNetworkType(String networkType){
        m_networkType = networkType;
    }

    public String getName() {
        return m_name;
    }

    public void setName(String name){
        m_name = name;
    }

    public String getSymbol() {
        int space = m_symbol.indexOf(" ");
        return  space != -1 ? m_symbol.substring(0, space) : m_symbol;
    }

    public void setSymbol(String symbol){
        m_symbol = symbol;
    }


    public int getFractionalPrecision() {
        return m_fractionalPrecision;
    }

    @Override
    public String toString() {
        return m_symbol;
    }

    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        if(m_tokenId != null){
            json.addProperty("id", m_tokenId);
        }
  
        json.addProperty("emissionAmount", m_emissionAmount.get());
        
        json.addProperty("name", m_name);
        if(m_description.get() != null){
            json.addProperty("description", m_description.get());
        }
        json.addProperty("decimals", m_fractionalPrecision);
        if(m_networkType != null){
            json.addProperty("networkType", m_networkType);
        }
        json.addProperty("timeStamp", m_timestamp);
       
        json.addProperty("symbol", m_symbol);
        if(m_imageString != null){
            json.addProperty("imageString",m_imageString);
        }
        if(m_fontSymbol.get() != null){
            json.addProperty("fontSymbol", m_fontSymbol.get());
        }
        if(m_tokenType.get() != null){
            json.addProperty("tokenType",m_tokenType.get());
        }
        if (getImgHashData() != null) {
            json.add("hashData", getImgHashData().getJsonObject());
        }
    

        return json;
    }
}
