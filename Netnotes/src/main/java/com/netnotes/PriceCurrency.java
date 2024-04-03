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

import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class PriceCurrency {

    public final static int VALID_TIMEOUT = 1000*60;
    
    private boolean m_priceValid = true;
    private String m_defaultCurrencyName = ""; 
    private String m_tokenId = null;
    private String m_symbol = null;
    private String m_name = null;
    private String m_networkId = null;
    private String m_imageString = "/assets/unknown-unit.png";
    private int m_fractionalPrecision = 2;
    private String m_fontSymbol = "";
    private long m_timestamp = 0;
    private String m_networkType = null;
    private String m_description = "";
    private long m_emissionAmount = 0;
    private String m_tokenType = null;

    private HashData m_hashData = null;

    public final SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>(null); 

    public PriceCurrency(String token_id, String name, String symbol, String description, int fractionalPrecision, String networkId, String unitImageString, String networkType, long emissionAmount, long timestamp) {
        this(token_id, name, symbol,  fractionalPrecision, networkId,networkType, unitImageString, "","");
        m_timestamp = timestamp;
        m_description = description;
        m_emissionAmount = emissionAmount;
    }

    public PriceCurrency(String token_id, String name, String symbol, int fractionalPrecision, String networkId, String networkType, String unitImageString, String tokenType, String fontSymbol) {
        m_tokenId = token_id;
        m_name = name;
        m_symbol = symbol;
        m_networkId = networkId;
        m_imageString = unitImageString;
        m_fractionalPrecision = fractionalPrecision;
        m_fontSymbol = fontSymbol;
        m_tokenType = tokenType;
        m_networkType = networkType;

        if( m_imageString != null && !m_imageString.startsWith("/assets")){
            try {
                m_hashData = new HashData(new File(m_imageString));
            } catch (IOException e) {
                try {
                    Files.writeString(new File("netnotes-log.txt").toPath(), "\nPrice currency " + m_name + " hashdata failed.", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
                m_hashData = null;
            }
        }
    }

    public PriceCurrency(JsonObject json) throws Exception{
        JsonElement idElement = json.get("id");
        JsonElement emissionElement = json.get("emissionAmount");
        JsonElement nameElement = json.get("name");
        JsonElement descriptionElement = json.get("description");
        JsonElement decimalsElement = json.get("decimals");
        JsonElement networkTypeElement = json.get("networkType");
        JsonElement timeStampElement = json.get("timeStamp");
        JsonElement networkIdElement = json.get("networkId");
        JsonElement symbolElement = json.get("symbol");
        JsonElement fontSymbolElement = json.get("fontSymbol");
        JsonElement imageStringElement = json.get("imageString");
        JsonElement hashDataElement = json.get("hashData");

        if(idElement == null || decimalsElement == null || symbolElement == null || nameElement == null){
            throw new Exception("Invalid arguments");
        }
        
        m_tokenId = idElement.getAsString();
        m_fractionalPrecision = decimalsElement.getAsInt();
        m_symbol = symbolElement.getAsString();
        m_name = nameElement.getAsString();

        m_emissionAmount = emissionElement != null && emissionElement.isJsonPrimitive() ? emissionElement.getAsLong() : 0;
        m_description = descriptionElement != null && descriptionElement.isJsonPrimitive() ? descriptionElement.getAsString(): "";
        m_networkType = networkTypeElement != null && networkTypeElement.isJsonPrimitive() ? networkTypeElement.getAsString() : "";
        m_timestamp = timeStampElement != null && timeStampElement.isJsonPrimitive() ? timeStampElement.getAsLong() : 0;
        m_networkId = networkIdElement != null && networkIdElement.isJsonPrimitive() ? networkIdElement.getAsString() : "";
        m_fontSymbol = fontSymbolElement != null && fontSymbolElement.isJsonPrimitive() ? fontSymbolElement.getAsString() : "";

        if (hashDataElement != null && hashDataElement.isJsonObject()) {
            m_hashData = new HashData(hashDataElement.getAsJsonObject());
        }

        if (imageStringElement != null && getImgHashData() != null) {
            setImageString(imageStringElement.getAsString());
        }
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
        return m_tokenType;
    }

    public void setTokenType(String tokenType){
        m_tokenType = tokenType;
    }
   
    public void setDecimals(int decimals){
        m_fractionalPrecision = decimals;
    }

    public int getDecimals(){
        return m_fractionalPrecision;
    }


    public void setEmissionAmount(long amount){
        m_emissionAmount = amount;
    }
    public long getEmissionAmount(){
        return m_emissionAmount;
    }

    public void setDescription(String description){
        m_description = description;
    }

    public String getDescription(){
        return m_description;
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

    public void setPriceValid(boolean priceValid) {
        m_priceValid = priceValid;
    }

    public String getFontSymbol(){
        return m_fontSymbol;
    }
   public PriceQuote getPriceQuote() {
        return null;
    }

    public boolean getPriceValid() {
        return getPriceQuote() != null ? getPriceQuote().howOldMillis() < VALID_TIMEOUT  : m_priceValid;
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


    public String networkId() {
        return m_networkId;
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
        json.addProperty("emissionAmount", m_emissionAmount);
        json.addProperty("name", m_name);
        if(m_description != null){
            json.addProperty("description", m_description);
        }
        json.addProperty("decimals", m_fractionalPrecision);
        if(m_networkType != null){
            json.addProperty("networkType", m_networkType);
        }
        json.addProperty("timeStamp", m_timestamp);
        if(m_networkId != null){
            json.addProperty("networkId", m_networkId);
        }
        json.addProperty("symbol", m_symbol);
        if(m_imageString != null){
            json.addProperty("imageString",m_imageString);
        }
        if(m_fontSymbol != null){
            json.addProperty("fontSymbol", m_fontSymbol);
        }
        if (getImgHashData() != null) {
            json.add("hashData", getImgHashData().getJsonObject());
        }
    

        return json;
    }
}
