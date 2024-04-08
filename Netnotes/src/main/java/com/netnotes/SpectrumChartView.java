package com.netnotes;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class SpectrumChartView {

    
   public final static int DECIMAL_PRECISION = 6;
   public final static int MAX_BARS = 150;
   public final static int MIN_CHART_HEIGHT = 300;


   private TimeSpan m_timeSpan = new TimeSpan("30min");

    private SpectrumNumbers m_numberClass = new SpectrumNumbers();

    private ArrayList<SpectrumPriceData> m_priceList = new ArrayList<>();


    private SimpleDoubleProperty m_topVvalue = new SimpleDoubleProperty(1);
    private SimpleDoubleProperty m_bottomVvalue = new SimpleDoubleProperty(0);
    private boolean m_settingRange = false;
    private SimpleBooleanProperty m_active = new SimpleBooleanProperty(false);


    private int m_valid = 0;
    private Font m_headingFont = new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 18);
    private Font m_labelFont = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 12);
    private Color m_backgroundColor = new Color(1f, 1f, 1f, 0f);


    private double m_scale = 0;
    private int m_defaultCellWidth = 20;
    private int m_cellWidth = m_defaultCellWidth;
    private SimpleDoubleProperty m_chartHeight;
    private SimpleDoubleProperty m_chartWidth;



    private String m_msg = "Loading";

    private BigDecimal m_currentPrice = BigDecimal.ZERO;
    // private BufferedImage m_img = null;
    private int m_cellPadding = 3;

    private int m_scaleColWidth = 0;
    private int m_labelHeight = 0;
    private int m_amStringWidth = 0;
    private int m_labelAscent = 0;
    private FontMetrics m_fm = null;

    private BufferedImage m_img = null;
    private Graphics2D m_g2d = null;
    private int m_imgWidth = -1;
    private int m_imgHeight = -1;
    private Color m_defaultColor = new Color(0f, 0f, 0f, 0.01f);

    private long m_lastTimeStamp = 0;

    public final SimpleLongProperty m_doUpdate = new SimpleLongProperty(0);

    //private double m_lastClose = 0;
    private int m_labelSpacingSize = 150;
    private Color m_labelColor = new Color(0xc0ffffff);

    private double m_topRangePrice = 0;
    private double m_botRangePrice = 0;

    private boolean m_isPositive = true;

    public SpectrumChartView(SimpleDoubleProperty width, SimpleDoubleProperty height, TimeSpan timeSpan) {

        m_chartWidth = width;
        m_chartHeight = height;
        m_timeSpan = timeSpan;
        updateLabelFont();
        //updateBufferedImage();
    }


    public long getLastTimestamp(){
        return m_lastTimeStamp;
    }

    public void setLastTimeStamp(long timestamp){
        m_lastTimeStamp = timestamp;
    }

    public TimeSpan getTimeSpan(){
        return m_timeSpan;
    }

    public void setTimeSpan(TimeSpan timeSpan){
        m_timeSpan = timeSpan;
    }

    public JsonObject getJsonObject() {
        JsonObject jsonObject = new JsonObject();
        return jsonObject;
    }

    public SimpleBooleanProperty rangeActiveProperty() {
        return m_active;
    }

    public SimpleDoubleProperty rangeTopVvalueProperty() {
        return m_topVvalue;
    }

    public SimpleDoubleProperty rangeBottomVvalueProperty() {
        return m_bottomVvalue;
    }

    public boolean isSettingRange(){
        return m_settingRange;
    }


    public void setIsSettingRange(boolean isRangeSetting) {
        m_settingRange = isRangeSetting;
        m_doUpdate.set(System.currentTimeMillis());

        //updateBufferedImage();
    }

    private BufferedImage m_labelImg = null;
    private Graphics2D m_labelG2d = null;

    public void updateLabelFont() {

        m_labelImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        m_labelG2d = m_labelImg.createGraphics();
        m_labelG2d.setFont(m_labelFont);
        m_labelG2d.setColor(m_labelColor);
        m_fm = m_labelG2d.getFontMetrics();
        String measureString = "0.00000000";
        int stringWidth = m_fm.stringWidth(measureString);
        m_amStringWidth = m_fm.stringWidth(" a.m. ");
        m_labelAscent = m_fm.getAscent();
        m_labelHeight = m_fm.getHeight();
        m_scaleColWidth = stringWidth + 25;
        m_labelG2d.dispose();
        m_labelG2d = null;
        m_labelImg = null;
    }

    
    

    public HBox getChartBox() {


        ImageView imgView = new ImageView();
        imgView.setPreserveRatio(true);
        

        Runnable updateImg = ()->{
            Image img = updateBufferedImage();
            if(img != null){
                imgView.setFitWidth(img.getWidth());
                imgView.setImage(img);

            }
        };

        updateImg.run();

        m_doUpdate.addListener((obs,oldval,newval)->updateImg.run());


        m_chartHeight.addListener((obs, oldVal, newVal) -> updateImg.run());

        //  m_chartWidth.addListener((obs, oldVal, newVal) -> updateImage.run());


        rangeBottomVvalueProperty().addListener((obs, oldVal, newVal) -> updateImg.run());
        rangeTopVvalueProperty().addListener((obs, oldVal, newVal) -> updateImg.run());


        rangeActiveProperty().addListener((obs, oldVal, newVal) -> updateImg.run());

        
        HBox chartBox = new HBox(imgView);
    
        return chartBox;
    }

    public void reset() {
        m_lastTimeStamp = 0;
        m_valid = 0;
        m_priceList.clear();
        m_msg = "Loading";
        m_doUpdate.set(System.currentTimeMillis());
    }

    /*
    private void resizeChart() {
        int fullChartSize = (m_cellWidth + (m_cellPadding * 2)) * m_priceList.size();
        if (fullChartSize < m_chartWidth.get()) {
            while (fullChartSize < m_chartWidth.get()) {
                m_cellWidth += 1;
                fullChartSize = m_cellWidth + (m_cellPadding * 2);
            }
        } else {
            if ((m_chartWidth.get() / (m_cellWidth + (m_cellPadding * 2))) < 30) {
                while (m_cellWidth > 2 && (m_chartWidth.get() / (m_cellWidth + (m_cellPadding * 2))) < 30) {
                    m_cellWidth -= 1;
                }
            }
        }

    } */
    public static int getZeros(String string) {
         int indexOfDecimal = string.indexOf(".");

        int numZeros = 0;
        String strZero = "0";
        char zero = strZero.charAt(0);

        while(indexOfDecimal != -1 && indexOfDecimal < string.length() && string.charAt(indexOfDecimal + 1) == zero){
            indexOfDecimal ++;
            numZeros++;
        }
                    
                        
        return numZeros;
    }
    /*
    private static SpectrumPriceData getEpochElement(JsonArray array, long epochStart, long longEpochEnd, BigDecimal prevClose) {
        SimpleObjectProperty<SpectrumPriceData> priceData = new SimpleObjectProperty<>(null);
       // SimpleObjectProperty<BigDecimal> prevPrice = new SimpleObjectProperty<>(BigDecimal.ZERO);

        for (JsonElement jsonElement : array) {
            if (jsonElement != null && jsonElement.isJsonObject()) {

                JsonObject priceObject = jsonElement.getAsJsonObject();
                JsonElement timeStampElement = priceObject.get("timestamp");
                JsonElement priceElement = priceObject.get("price");

                if(timeStampElement != null && timeStampElement.isJsonPrimitive() &&
                    priceElement != null && priceElement.isJsonPrimitive()
                ){
                    long timestamp = timeStampElement.getAsLong();
                    BigDecimal price = priceElement.getAsBigDecimal();

                    if (timestamp > epochStart && timestamp <= longEpochEnd) {
                        if(priceData.get() == null){
                            priceData.set(new SpectrumPriceData(longEpochEnd, price));
                        }
                        priceData.get().addPrice(timestamp, price);
                    }else{
                        if(timestamp > longEpochEnd){
                            if(priceData.get() != null){
                                return priceData.get();
                            }else{
                                if(prevClose != null){
                                    return new SpectrumPriceData(longEpochEnd, prevClose );
                                }else{
                                    return null;
                                }
                            }
                        }
                    }
                }
            }
        }
        if(prevClose != null){
            return new SpectrumPriceData(longEpochEnd, prevClose );
        }else{
            return null;
        }
    }*/
    
    private void updateDirection(BigDecimal lastPrice, BigDecimal newPrice){
       
        int direction = newPrice.compareTo(lastPrice);
        if(direction != 0){
            m_isPositive = direction == 1;
        }
        
    }

    
    public void setPriceDataList(JsonArray jsonArray, long latestTime) {
        m_priceList.clear();

        if (jsonArray != null && jsonArray.size() > 0) {
            long timeSpanMillis = m_timeSpan.getMillis();
            setLastTimeStamp(latestTime);
           
            m_valid = 1;
            m_msg = "Loading";
         
            JsonElement oldestElement = jsonArray.get(0);
            JsonElement newestElement = jsonArray.get(jsonArray.size() - 1);

        
          

            if (oldestElement != null && oldestElement.isJsonObject() && newestElement != null && newestElement.isJsonObject()) {
              
                JsonObject oldestObject = oldestElement.getAsJsonObject();
                JsonObject newestObject = newestElement.getAsJsonObject();
   
                long oldestTimeStamp = oldestObject.get("timestamp").getAsLong();
                long newestTimeStamp = newestObject.get("timestamp").getAsLong();

        

                int maxElements = (int) Math.ceil(((newestTimeStamp + timeSpanMillis) - oldestTimeStamp) / timeSpanMillis);

                int startElement = maxElements > MAX_BARS ? maxElements - MAX_BARS : 0;

                final long startTimeStamp = ((startElement * timeSpanMillis) + oldestTimeStamp ) + timeSpanMillis;

                SimpleIntegerProperty index = new SimpleIntegerProperty(0);
                int size = jsonArray.size();
       
                BigDecimal firstOpen = oldestObject.get("price").getAsBigDecimal();
                SimpleObjectProperty<BigDecimal> lastClose = new SimpleObjectProperty<>(firstOpen);
   
                SimpleLongProperty epochEnd = new SimpleLongProperty(startTimeStamp);

                lastClose.addListener((obs,oldval,newval)->{
                    updateDirection(oldval, newval);
                });
                
                while (index.get() < size) {
                    
                    try{
                        SimpleObjectProperty<SpectrumPrice> spectrumPrice = new SimpleObjectProperty<>( new SpectrumPrice( jsonArray.get(index.get()).getAsJsonObject()));
                        
                    
                       
                     

                        if(spectrumPrice.get().getTimeStamp() > startTimeStamp - timeSpanMillis  ){

                            while(spectrumPrice.get().getTimeStamp() > epochEnd.get()){
                                long currentTime = System.currentTimeMillis();
                                long newestEpochTime = epochEnd.get() > currentTime ? currentTime : epochEnd.get();
                                SpectrumPriceData data = new SpectrumPriceData(newestEpochTime, epochEnd.get(), lastClose.get());
                                m_priceList.add(data);
                                epochEnd.set(epochEnd.get() + timeSpanMillis);
                            }

                                SpectrumPriceData priceData = new SpectrumPriceData(spectrumPrice.get(), epochEnd.get());
                                priceData.setOpen(lastClose.get());
                            
                                addPrices(index, priceData, epochEnd.get(), jsonArray);

                                

                                lastClose.set(priceData.getClose());

                            
                                m_priceList.add(priceData);
                                setCurrentPrice(priceData.getClose());
                                
                                
                                epochEnd.set(epochEnd.get() + timeSpanMillis);
                               
                            
                        }else{
                           
                            lastClose.set(spectrumPrice.get().getPrice());
                        }
                    }catch(Exception priceException){
                        
                        try {
                            Files.writeString(App.logFile.toPath(), "\nSpectrum setPriceData: " + priceException.toString() , StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e) {
             
                        }
                    
                    }
                 
                    index.set(index.get()+1);
                    
                }
                
                if(m_priceList.size() > 0){
                    SpectrumPriceData lastPriceData = m_priceList.get(m_priceList.size() -1);
                    updateMarketData(System.currentTimeMillis(), lastPriceData.getClose());
                }else{
                    m_doUpdate.set(System.currentTimeMillis());
                }
        
            } else {
                m_valid = 2;
                m_msg = "Received no data.";
                m_doUpdate.set(System.currentTimeMillis());
            }
        
          
   
           
        } else {
            m_valid = 2;
            m_msg = "Received no data.";
            m_doUpdate.set(System.currentTimeMillis());
        }
        
    }

    private void setCurrentPrice(BigDecimal newPrice){
      
        m_currentPrice = newPrice;
    }

    private void addPrices(SimpleIntegerProperty index, SpectrumPriceData priceData, long epochEnd, JsonArray jsonArray){
        int size = jsonArray.size();

        if(index.get() + 1 < size){         
            try{
                SimpleObjectProperty<SpectrumPrice> nextSpectrumPrice = new SimpleObjectProperty<>(new SpectrumPrice(jsonArray.get(index.get() + 1).getAsJsonObject()));
    
                while(nextSpectrumPrice.get().getTimeStamp() <= epochEnd){
                    long timestamp = nextSpectrumPrice.get().getTimeStamp();
                    BigDecimal price = nextSpectrumPrice.get().getPrice();
                    priceData.addPrice(timestamp, price);

                    updateDirection(m_currentPrice, price);
                    m_currentPrice = price;

                    index.set(index.get() + 1);

                    if((index.get() +1) == size){
                        break;
                    }
                    try{
                        nextSpectrumPrice.set(new SpectrumPrice(jsonArray.get(index.get() + 1).getAsJsonObject()));
                    }catch(Exception whileNextException){
                        try{
                            Files.writeString(App.logFile.toPath(), "Spectrum nextPriceException: " + whileNextException.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        }catch(IOException npioe){

                        }
                        if((index.get() +1) == size){
                            index.set(index.get() + 1);
                        }
                    
                    }
                }
            }catch(Exception nextPriceException){
                try{
                    Files.writeString(App.logFile.toPath(), "Spectrum nextPriceException: " + nextPriceException.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }catch(IOException npioe){

                } 
                if((index.get() +1) != size){        
                    index.set(index.get() + 1);
                }
            }
        }
    }

    public boolean updateMarketData(long timeStamp, BigDecimal price){
        int size = m_priceList.size();
        if(size == 0){
            return false;
        }
        updateDirection(m_currentPrice, price);

        long timeSpanMillis = m_timeSpan.getMillis();
        

        SpectrumPriceData lastPriceData = m_priceList.get(size-1);

        long currentEpochEnd = lastPriceData.getEpochEnd();

       
        
        long nextEpochStart = currentEpochEnd + 1;
        long nextEpochEnd = currentEpochEnd + timeSpanMillis;

        if(currentEpochEnd >= timeStamp){
            if(timeStamp > (currentEpochEnd - timeSpanMillis)){
                
                lastPriceData.addPrice(timeStamp, price);
                
        
            }
        }else{


            while(timeStamp > nextEpochEnd){
                SpectrumPriceData emptyPriceData = new SpectrumPriceData(nextEpochStart, m_currentPrice, m_currentPrice, m_currentPrice, m_currentPrice, nextEpochEnd);
                m_priceList.add(emptyPriceData);
                nextEpochStart += timeSpanMillis;
                nextEpochEnd += timeSpanMillis;
                
            }
            
            SpectrumPriceData nextPriceData = new SpectrumPriceData(timeStamp, nextEpochEnd, price);
            m_priceList.add(nextPriceData);
       

        }
        //setIsPositive(price.compareTo(m_currentPrice));

        setCurrentPrice(price);

        updateNumbers();
        m_doUpdate.set(timeStamp);
        return true;
    }

    public SimpleLongProperty updatedTimeStampProperty(){
        return m_doUpdate;
    }

    public void updateNumbers(){
        int size = m_priceList.size();
        while(size > MAX_BARS){
            m_priceList.remove(0);
            size = m_priceList.size();
        }
        
        SpectrumNumbers numberClass = new SpectrumNumbers();

        for(int i = 0; i < size ; i++){
            SpectrumPriceData priceData = m_priceList.get(i);
            
            if (numberClass.getLow().equals(BigDecimal.ZERO)) {
                numberClass.setLow(priceData.getLow());
            }

            numberClass.setSum(numberClass.getSum().add(priceData.getClose()));
        
            numberClass.setHigh(priceData.getHigh().max(numberClass.getHigh()));

            numberClass.setLow(priceData.getLow().equals(BigDecimal.ZERO) ? numberClass.getLow() : priceData.getLow().min(numberClass.getLow()));
            
                       
        }
        numberClass.setCount(size);
        BigDecimal min =  numberClass.getLow();
            
        int decimals = min.doubleValue() > 0 ? (min.doubleValue() > 100 ? 2 : (min.scale() > DECIMAL_PRECISION ? DECIMAL_PRECISION : min.scale())) : (getZeros(min.toString()) + DECIMAL_PRECISION);
        
        numberClass.setDecimals(decimals);
        

        m_numberClass = numberClass;
    }

    public JsonObject getPriceDataJson(){
        JsonObject json = new JsonObject();
        JsonArray jsonArray = new JsonArray();

        for(SpectrumPriceData dataItem : m_priceList){
            jsonArray.add(dataItem.getJsonObject());
        }

        json.add("priceData", jsonArray);

        return json;
    }
    /*
    public void updatePriceData(JsonArray jsonArray, long lastestTimeStamp) {
         int size = jsonArray.size();

        if(size == 0){
            return;
        }

       
        if(m_priceList.size() > 0 ){
          
      
            long timeSpanMillis = m_timeSpan.getMillis();
            setLastTimeStamp(lastestTimeStamp);
            
            SpectrumPriceData newestData = m_priceList.get(m_priceList.size() -1);

            

            SimpleIntegerProperty index = new SimpleIntegerProperty(0);
          

            SimpleLongProperty epochEnd = new SimpleLongProperty(newestData.getEpochEnd());
     
            SimpleObjectProperty<SpectrumPrice> spectrumPrice = new SimpleObjectProperty<>(null);

            while(spectrumPrice.get() == null && index.get() < jsonArray.size()){
                try{
                    spectrumPrice.set(new SpectrumPrice (jsonArray.get(index.get()).getAsJsonObject()));
                }catch(Exception e){
                    index.set(index.get()+1);
                }
            }

          
            


            while(spectrumPrice.get().getTimeStamp() > epochEnd.get() + timeSpanMillis){
                epochEnd.set( epochEnd.get() + timeSpanMillis);
                long currentTime = System.currentTimeMillis();
                long newestEpochTime = epochEnd.get() > currentTime ? currentTime : epochEnd.get();
                SpectrumPriceData data = new SpectrumPriceData(newestEpochTime, epochEnd.get(), m_currentPrice);
                m_priceList.add(data);
                m_numberClass.setCount(m_numberClass.getCount() + 1);
        
            }



            if(spectrumPrice.get().getTimeStamp() <= epochEnd.get()){
                double lastCurrentPrice = m_currentPrice.doubleValue();

                while(spectrumPrice.get().getTimeStamp() <= epochEnd.get()){
                   
                    newestData.addPrice(spectrumPrice.get().getTimeStamp(), spectrumPrice.get().getPrice());
               
                    m_currentPrice = spectrumPrice.get().getPrice();
                    index.set(index.get() + 1);
                    if(index.get() >= size){
                        break;
                    }
                    try{
                        spectrumPrice.set(new SpectrumPrice(jsonArray.get(index.get()).getAsJsonObject()));
                    }catch(Exception whileNextException){
                        try{
                            Files.writeString(App.logFile.toPath(), "Spectrum updatePriceData nextPriceException: " + whileNextException.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        }catch(IOException npioe){

                        }
                        index.set(index.get() + 1);
                    }
                }
                if (m_numberClass.getLow().equals(BigDecimal.ZERO)) {
                    m_numberClass.setLow(newestData.getLow());
                }

                double currentPriceDiff = lastCurrentPrice - m_currentPrice.doubleValue();

                m_numberClass.setSum(m_numberClass.getSum().subtract(new BigDecimal(currentPriceDiff)));
                m_numberClass.setHigh(newestData.getHigh().max(m_numberClass.getHigh()));
                m_numberClass.setLow(newestData.getLow().equals(BigDecimal.ZERO) ? m_numberClass.getLow() : newestData.getLow().min(m_numberClass.getLow()));
                
                epochEnd.set(epochEnd.get() + timeSpanMillis);
            }

             try{
                SpectrumPrice lastPrice =  new SpectrumPrice(jsonArray.get(size -1).getAsJsonObject());
                BigDecimal secondLastPrice = size > 1 ? new SpectrumPrice(jsonArray.get(size -2).getAsJsonObject()).getPrice() : m_currentPrice;
               
                setIsPositive(lastPrice.getPrice().compareTo(secondLastPrice));
            }catch(Exception jsonException){
                setIsPositive(0);
            }
          

            while (index.get() < size) {
                
                try{
                    spectrumPrice.set( new SpectrumPrice( jsonArray.get(index.get()).getAsJsonObject()));
                                    
                    while((epochEnd.get()) < spectrumPrice.get().getTimeStamp() ){
                        long currentTime = System.currentTimeMillis();
                        long newestEpochTime = epochEnd.get() > currentTime ? currentTime : epochEnd.get();
                        SpectrumPriceData data = new SpectrumPriceData(newestEpochTime, epochEnd.get(), m_currentPrice);
                        m_priceList.add(data);
                        m_numberClass.setCount(m_numberClass.getCount() + 1);
                        epochEnd.set(epochEnd.get() + timeSpanMillis);
                    }

                    SpectrumPriceData priceData = new SpectrumPriceData(spectrumPrice.get(), epochEnd.get());
                    priceData.setOpen(m_currentPrice);
                    m_currentPrice = spectrumPrice.get().getPrice();

                    if(index.get() + 1 < size){
                        
                        try{
                            SimpleObjectProperty<SpectrumPrice> nextSpectrumPrice = new SimpleObjectProperty<>(new SpectrumPrice(jsonArray.get(index.get() + 1).getAsJsonObject()));
                
                            while(nextSpectrumPrice.get().getTimeStamp() <= epochEnd.get()){
                                priceData.addPrice(nextSpectrumPrice.get().getTimeStamp(), nextSpectrumPrice.get().getPrice());
                                m_currentPrice = nextSpectrumPrice.get().getPrice();
                                index.set(index.get() + 1);
                                if(index.get() +1 >= size){
                                    break;
                                }
                                try{
                                    nextSpectrumPrice.set(new SpectrumPrice(jsonArray.get(index.get() + 1).getAsJsonObject()));
                                }catch(Exception whileNextException){
                                    try{
                                        Files.writeString(App.logFile.toPath(), "Spectrum nextPriceException: " + whileNextException.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                    }catch(IOException npioe){

                                    }
                                        index.set(index.get() + 1);
                                
                                }
                            }
                        }catch(Exception nextPriceException){
                            try{
                                Files.writeString(App.logFile.toPath(), "Spectrum nextPriceException: " + nextPriceException.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            }catch(IOException npioe){

                            }
                            index.set(index.get() + 1);
                        }
                    }

                    if (m_numberClass.getLow().equals(BigDecimal.ZERO)) {
                        m_numberClass.setLow(priceData.getLow());
                    }

                    m_numberClass.setSum(m_numberClass.getSum().add(priceData.getClose()));
                

                    m_numberClass.setHigh(priceData.getHigh().max(m_numberClass.getHigh()));

                    m_numberClass.setLow(priceData.getLow().equals(BigDecimal.ZERO) ? m_numberClass.getLow() : priceData.getLow().min(m_numberClass.getLow()));
                    
                    m_numberClass.setCount(m_numberClass.getCount() + 1);
                    m_priceList.add(priceData);
                    m_currentPrice = priceData.getClose();
                    
                    
                    epochEnd.set(epochEnd.get() + timeSpanMillis);
            
                        
                   
                }catch(Exception priceException){
                    
                    try {
                        Files.writeString(App.logFile.toPath(), "\nSpectrum invalid price.", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {
            
                    }
                
                }
                
                index.set(index.get()+1);
                
            }
                
                
        

           
            
   
            
            m_lastUpdated.set(LocalDateTime.now());
        }else{
            
            setPriceDataList(jsonArray, lastestTimeStamp);
        }
    }*/

   

    public BigDecimal getCurrentPrice() {
        return m_currentPrice;
    }

    public SimpleDoubleProperty chartHeightProperty() {
        return m_chartHeight;
    }

    public double getChartHeight() {
        return m_chartHeight.get();
    }

    public void setChartHeight(int chartHeight) {
        m_chartHeight.set(chartHeight);
    }

    public int getCellWidth() {
        return m_cellWidth;
    }

    public void setCellWidth(int cellWidth) {
        m_cellWidth = cellWidth;
    }

    public double getScale() {
        return m_scale;
    }

    public void setScale(double scale) {
        m_scale = scale;
    }

    public void setBackgroundColor(Color color) {
        m_backgroundColor = color;
    }

    public Color getBackgroundColor() {
        return m_backgroundColor;
    }

    public void setHeadingFont(Font font) {
        m_headingFont = font;
    }

    public Font getHeadingFont() {
        return m_headingFont;
    }

    public int getValid() {
        return m_valid;
    }

    public SpectrumNumbers getNumbers() {
        return m_numberClass;
    }

    /*1min, 3min, 15min, 30min, 1hour, 2hour, 4hour, 6hour, 8hour, 12hour, 1day, 1week */
    public int getPriceListSize() {
        return m_priceList.size();
    }

    public int getChartTopY(BufferedImage img) {
        return img.getHeight() - (int) (m_scale * m_numberClass.getHigh().doubleValue());
    }

    public int getChartBottomY(BufferedImage img) {
        return img.getHeight() - (int) (m_scale * m_numberClass.getLow().doubleValue());
    }

    public String getTimeStampString() {
        long lastUpdated = m_doUpdate.get();
        return Utils.formatTimeString(Utils.milliToLocalTime(lastUpdated));
    }

    public void setMsg(String msg) {
        m_msg = msg;
        m_doUpdate.set(System.currentTimeMillis());
    }

    public String getMsg() {
        return m_msg;
    }

    public int getTotalCellWidth() {
        return m_cellPadding + m_cellWidth;
    }



    private int getRowHeight() {
        return m_labelHeight + 7;
    }



    public Image updateBufferedImage() {
        LocalDateTime now = LocalDateTime.now();
     
       
        int greenHighlightRGB = 0x504bbd94;
      //  int greenHighlightRGB2 = 0x80028a0f;
        int redRGBhighlight = 0x50e96d71;
      //  int redRGBhighlight2 = 0x809a2a2a;

        int priceListSize = getPriceListSize();
        int totalCellWidth = getTotalCellWidth();

        double bottomVvalue = rangeBottomVvalueProperty().get();
        double topVvalue = rangeTopVvalueProperty().get();
        boolean isRangeSetting = m_settingRange;

        boolean rangeActive = rangeActiveProperty().get() && !isRangeSetting;

        int cellWidth = m_cellWidth;
        
        int currentWidth = m_priceList.size() == 0 ? (int) m_chartWidth.get() : m_scaleColWidth + (m_priceList.size() * totalCellWidth);
        currentWidth = currentWidth < m_chartWidth.get() ? (int) m_chartWidth.get() : currentWidth;
        currentWidth = currentWidth < 100 ? 100 : currentWidth;
        boolean init = m_chartHeight.get() < 1;

        int currentHeight = m_chartHeight.get() < MIN_CHART_HEIGHT ? MIN_CHART_HEIGHT : (int) Math.ceil(m_chartHeight.get()) ;

       
        if(m_img == null || (currentWidth != m_imgWidth || currentHeight != m_imgHeight)){
            if(m_g2d != null){
                m_g2d.dispose();
            }
            m_imgWidth = currentWidth;
            m_imgHeight = currentHeight;

            m_img = new BufferedImage(m_imgWidth, m_imgHeight, BufferedImage.TYPE_INT_ARGB);
            m_g2d = m_img.createGraphics();
            m_g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            m_g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            m_g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        }

        Drawing.fillArea(m_img, m_defaultColor.getRGB(), 0, 0, m_img.getWidth(), m_img.getHeight(), false);
     

        m_g2d.setFont(m_labelFont);
        m_g2d.setColor(m_labelColor);

        if (m_valid == 1 && m_priceList.size() > 0 &&  (m_imgHeight - (2 * (m_labelHeight + 5)) - 10) > 100) {

            int chartWidth = m_imgWidth - m_scaleColWidth;
            int chartHeight = m_imgHeight - (2 * (m_labelHeight + 5)) - 10;

            int numCells = (int) Math.floor((m_imgWidth - m_scaleColWidth) / totalCellWidth);

            int i = numCells > priceListSize ? 0 : priceListSize - numCells;
            /*
            SpectrumNumbers nc = new SpectrumNumbers();

            for (int j = i; j < priceListSize; j++) {
                SpectrumPriceData priceData = m_priceList.get(j);

                if (nc.getLow().equals(BigDecimal.ZERO)) {
                    nc.setLow(priceData.getLow());
                }

                nc.setSum(nc.getSum().add(priceData.getClose()));
                nc.setCount(nc.getCount() + 1);
                nc.setHigh(priceData.getHigh().max(nc.getHigh()));
                
            
                nc.setLow(priceData.getLow().min(nc.getLow()));
                
                int decimals = getDecimals(priceData.getCloseString());

                if (decimals > nc.getDecimals()) {
                    nc.setDecimals(decimals);
                }
            } */

            double highValue = m_numberClass.getHigh().doubleValue();

        

            double scale = (.6d * ((double) chartHeight)) / highValue;

    
            double topRangePrice = (topVvalue * (Math.floor(chartHeight / getRowHeight()) * getRowHeight())) / scale;
            double botRangePrice = (bottomVvalue * (Math.floor(chartHeight / getRowHeight()) * getRowHeight())) / scale;

    

            if (isRangeSetting) {
                m_topRangePrice = topRangePrice;
                m_botRangePrice = botRangePrice;
            } else {
                topRangePrice = m_topRangePrice;
                botRangePrice = m_botRangePrice;
            }

            double scale2 = (double) chartHeight / (topRangePrice - botRangePrice);

            Drawing.fillArea(m_img, 0xff111111, 0, 0, chartWidth, chartHeight);

            int j = 0;

            //    Color green = KucoinExchange.POSITIVE_COLOR;
            Color highlightGreen = KucoinExchange.POSITIVE_HIGHLIGHT_COLOR;
            Color garnetRed = KucoinExchange.NEGATIVE_COLOR;
            Color highlightRed = KucoinExchange.NEGATIVE_HIGHLIGHT_COLOR;

            Color overlayRed = new Color(garnetRed.getRed(), garnetRed.getGreen(), garnetRed.getBlue(), 0x70);
            Color overlayRedHighlight = new Color(highlightRed.getRed(), highlightRed.getGreen(), highlightRed.getBlue(), 0x70);

            int priceListWidth = priceListSize * totalCellWidth;

            int halfCellWidth = cellWidth / 2;

            int items = m_priceList.size() - i;
            int colLabelSpacing = items == 0 ? m_labelSpacingSize : (int) Math.floor(items / ((items * cellWidth) / m_labelSpacingSize));

            int rowHeight = getRowHeight();

            int rows = (int) Math.floor(chartHeight / rowHeight);
            
            ////////////////////////////TODO: handle chart too small

            rows = rows == 0 ? 1 : rows;

            int rowsHeight = rows * rowHeight;
            

            int rowLabelSpacing = (int) (rows / (rowsHeight / m_labelSpacingSize));

            for (j = 0; j < rows; j++) {

                int y = chartHeight - (j * rowHeight);
                if (j % rowLabelSpacing == 0) {
                    Drawing.fillAreaDotted(2, m_img, 0x10ffffff, 0, y, chartWidth, y + 1);
                    if (j != 0) {
                        Drawing.fillArea(m_img, 0xc0ffffff, chartWidth, y, chartWidth + 6, y + 2);
                    }
                }
                Drawing.fillArea(m_img, 0xff000000, chartWidth, y, chartWidth + 6, y + 1);

                double scaleLabeldbl = rangeActive ? (double) ((j * rowHeight) / scale2) + botRangePrice : (double) (j * rowHeight) / scale;

                String scaleAmount = String.format("%." + m_numberClass.getDecimals() + "f", scaleLabeldbl);
                int amountWidth = m_fm.stringWidth(scaleAmount);

                int x1 = (chartWidth + (m_scaleColWidth / 2)) - (amountWidth / 2);
                int y1 = (y - (m_labelHeight / 2)) + m_labelAscent;

                m_g2d.drawString(scaleAmount, x1, y1);

            }
            j = 0;

            while (i < m_priceList.size()) {
                SpectrumPriceData priceData = m_priceList.get(i);

                int x = ((priceListWidth < chartWidth) ? (chartWidth - priceListWidth) : 0) + (j * (cellWidth + m_cellPadding));
                j++;

                double low = priceData.getOpen().doubleValue() < priceData.getLow().doubleValue() ? priceData.getOpen().doubleValue() : priceData.getLow().doubleValue();
                double high = priceData.getHigh().doubleValue();
                if(rangeActive){
                    low = low < botRangePrice ? botRangePrice : low;
                    low = low > topRangePrice ? topRangePrice : low;
                    high = high < botRangePrice ? botRangePrice : high;
                    high = high > topRangePrice ? topRangePrice : high;
                }
                double nextOpen = (i < m_priceList.size() - 2 ? m_priceList.get(i + 1).getOpen().doubleValue() : priceData.getClose().doubleValue());
                double close = priceData.getClose().doubleValue();
                double open = priceData.getOpen().doubleValue();

                
                if(rangeActive){
                    close = close < botRangePrice ? botRangePrice : close;
                    close = close > topRangePrice ? topRangePrice : close;
                    open = open < botRangePrice ? botRangePrice : open;
                    open = open > topRangePrice ? topRangePrice : open;
                }

                int lowY = (int) (low * scale);
                int highY = (int) (high * scale);
                int openY = (int) (open * scale);
                int closeY = (int) (close * scale);

                if (rangeActive) {

                    lowY = (int) ((low - botRangePrice) * scale2);
                    highY = (int) ((high - botRangePrice) * scale2);
                    openY = (int) ((open - botRangePrice) * scale2);
                    closeY = (int) ((close - botRangePrice) * scale2);

                    lowY = lowY < 1 ? 1 : lowY;
                    lowY = lowY > chartHeight ? chartHeight : lowY;

                    highY = highY < 1 ? 1 : highY;
                    highY = highY > chartHeight ? chartHeight : highY;

                    openY = openY < 1 ? 1 : openY;
                    openY = openY > chartHeight ? chartHeight : openY;

                    closeY = closeY < 1 ? 1 : closeY;
                    closeY = closeY > chartHeight ? chartHeight : closeY;

                }

                boolean positive = !((m_imgHeight - closeY) > (m_imgHeight - openY));
                boolean neutral = open == nextOpen && open == close;

                LocalDateTime localTimestamp = priceData.getLocalDateTime();

                if (localTimestamp != null) {
                    if (i % colLabelSpacing == 0) {

                        Drawing.fillAreaDotted(2, m_img, 0x10ffffff, x + halfCellWidth - 1, 0, x + halfCellWidth, chartHeight);
                        Drawing.fillArea(m_img, 0x80000000, x + halfCellWidth - 1, chartHeight, x + halfCellWidth + 1, chartHeight + 4);

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

                        String timeString = formatter.format(localTimestamp);
                        int timeStringWidth = m_fm.stringWidth(timeString);

                        int timeStringX = x - ((timeStringWidth - m_amStringWidth) / 2);
                        m_g2d.drawString(timeString, timeStringX, chartHeight + 4 + (m_labelHeight / 2) + m_labelAscent);

                        if (!(localTimestamp.getDayOfYear() == now.getDayOfYear() && localTimestamp.getYear() == now.getYear())) {

                            formatter = DateTimeFormatter.ofPattern("MM/dd/YYYY");

                            timeString = formatter.format(localTimestamp);

                            timeStringWidth = m_fm.stringWidth(timeString);

                            timeStringX = x - ((timeStringWidth - m_amStringWidth) / 2);

                            m_g2d.drawString(timeString, timeStringX, chartHeight + 4 + m_labelHeight + 4 + (m_labelHeight / 2) + m_labelAscent);

                        }
                    }

                }

                if (highY != lowY) {
                    Drawing.fillArea(m_img, 0xffffffff, x + halfCellWidth - 1, chartHeight - highY, x + halfCellWidth, chartHeight - lowY);
                }

                if (neutral) {

                    Drawing.drawBar(1, Color.lightGray, Color.gray, m_img, x, chartHeight - openY - 1, x + cellWidth, chartHeight - closeY + 1);

                } else {
                    if (positive) {

                        int y1 = chartHeight - closeY;
                        int y2 = (chartHeight - openY) + 1;

                        int x2 = x + cellWidth;
                        Drawing.drawBar(1, 0xff000000, 0xff111111, m_img, x, y1, x2, y2);
                        Drawing.drawBar(1, greenHighlightRGB, 0xff000000, m_img, x, y1, x2, y2);
                        Drawing.drawBar(0, 0x104bbd94, 0x004bbd94, m_img, x, y1, x2, y2);

                        int RGBhighlight = highlightGreen.getRGB();

                        Drawing.fillArea(m_img, 0x804bbd94, x + 1, y1, x2, y1 + 1);

                        Drawing.drawBar(0x80555555, RGBhighlight, m_img, x2, y1 + 3, x2 - 2, y2);

                        Drawing.fillArea(m_img, 0x30000000, x, y2 - 1, x2, y2);

                    } else {

                        int y1 = chartHeight - openY;
                        int y2 = chartHeight - closeY;

                        Drawing.drawBar(1, garnetRed, highlightRed, m_img, x, y1, x + cellWidth, y2);
                        Drawing.drawBar(0, overlayRed, overlayRedHighlight, m_img, x, y1, x + cellWidth, y2);

                        int RGBhighlight = overlayRedHighlight.getRGB();

                        Drawing.fillArea(m_img, RGBhighlight, x, y1, x + 1, y2);
                        Drawing.fillArea(m_img, RGBhighlight, x, y1, x + cellWidth, y1 + 1);
                        Drawing.fillArea(m_img, garnetRed.getRGB(), x + cellWidth - 1, y1, x + cellWidth, y2);

                        Drawing.fillArea(m_img, garnetRed.getRGB(), x + 1, y2 - 1, x + cellWidth - 1, y2);
                    }
                }

                i++;
            }

            int halfLabelHeight = (m_labelHeight / 2);
            int currentCloseY = (int) (getCurrentPrice().doubleValue() * scale);

            int y = chartHeight - currentCloseY;
            int minY = (halfLabelHeight + 8);
            int maxY = chartHeight;

            boolean outOfBounds = false;

            if (rangeActive) {
                y = (int) (chartHeight - ((getCurrentPrice().doubleValue() - botRangePrice) * scale2));

                if (y < minY) {
                    outOfBounds = true;
                    y = minY;
                } else {
                    if (y > maxY) {
                        outOfBounds = true;
                        y = maxY;
                    }
                }

            }

            y = y < 0 ? 0 : y > chartHeight ? chartHeight : y;

           // m_direction = m_isPositive.get() > -1;

        // m_lastClose = getCurrentPrice().doubleValue();
            int y1 = y - (halfLabelHeight + 7);
            int y2 = y + (halfLabelHeight + 5);
            int halfScaleColWidth = (m_scaleColWidth / 2);

            int RGBhighlight;

            int x1 = chartWidth + 1;
            int x2 = chartWidth + m_scaleColWidth;

            boolean isLastPositive = m_isPositive;

            if (isLastPositive) {
                RGBhighlight = greenHighlightRGB;
            } else {
                RGBhighlight = redRGBhighlight;
            }

            Drawing.drawBar(1, 0xff000000, 0xff111111, m_img, x1, y1, m_imgWidth, y2);
            Drawing.drawBar(1, RGBhighlight, 0xff000000, m_img, x1, y1, m_imgWidth, y2);

            y2 = y2 + 1;

            Drawing.drawBar(1, 0x90000000, RGBhighlight, m_img, x1, y1 - 1, x2, y1 + 3);
            Drawing.drawBar(0x80555555, RGBhighlight, m_img, x1 - 2, y1 + 3, x1 + 1, y2 - 1);
            Drawing.drawBar(0x80555555, RGBhighlight, m_img, x2, y1 + 4, x2 - 2, y2);
            Drawing.drawBar(1, 0x50ffffff, RGBhighlight, m_img, x1, y2 - 1, x2, y2);

            Color stringColor = new java.awt.Color(0xffffffff, true);

            String closeString = String.format("%." + m_numberClass.getDecimals() + "f", getCurrentPrice());
            int stringWidth = m_fm.stringWidth(closeString);

            int stringY = (y - halfLabelHeight) + m_labelAscent;
            int stringX = (x1 + halfScaleColWidth) - (stringWidth / 2);

            m_g2d.setColor(stringColor);
            m_g2d.drawString(closeString, stringX, stringY);
            if (!outOfBounds) {
                m_g2d.setColor(isLastPositive ? KucoinExchange.POSITIVE_HIGHLIGHT_COLOR : KucoinExchange.NEGATIVE_HIGHLIGHT_COLOR);
                m_g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                m_g2d.drawString("◄", chartWidth - 9, stringY);
            }
            stringX = chartWidth - 9;
            if (isLastPositive) {
                Drawing.drawBarFillColor(1, false, stringColor.getRGB(), 0xffffffff, 0xffffffff, m_img, stringX, y - (m_labelHeight / 2) - 1, chartWidth + m_scaleColWidth, y + 4 - (m_labelHeight / 2));
                Drawing.drawBarFillColor(1, false, stringColor.getRGB(), 0xffffffff, 0xff4bbd94, m_img, stringX, y + 4 - (m_labelHeight / 2), chartWidth + m_scaleColWidth, y + (m_labelHeight / 2));
            } else {
                Drawing.drawBarFillColor(1, false, stringColor.getRGB(), 0xffffffff, 0xffffffff, m_img, stringX, y - (m_labelHeight / 2) - 1, chartWidth + m_scaleColWidth, y + 4 - (m_labelHeight / 2));
                Drawing.drawBarFillColor(1, false, stringColor.getRGB(), 0xffffffff, 0xffe96d71, m_img, stringX, y + 4 - (m_labelHeight / 2), chartWidth + m_scaleColWidth, y + (m_labelHeight / 2));
            }
            int borderColor = 0xFF000000;

            Drawing.fillArea(m_img, borderColor, 0, chartHeight, chartWidth, chartHeight + 1);

            Drawing.fillArea(m_img, borderColor, chartWidth, 0, chartWidth + 1, chartHeight);//(m_imgWidth - scaleColWidth, 0, m_imgWidth - 1, chartHeight - 1);
            if (!outOfBounds) {
                for (int x = 0; x < m_imgWidth - m_scaleColWidth - 7; x++) {
                    int p = m_img.getRGB(x, y);

                    p = (0xFFFFFF - p) | 0xFF000000;

                    m_img.setRGB(x, y, p);
                }
            }
            if (isRangeSetting) {

                x1 = x1 + 1;
                Color blackColor = new java.awt.Color(0xff000000, true);

                m_g2d.setFont(m_labelFont);

                int topRangeY = (int) Math.ceil(chartHeight - (topVvalue * (rows * rowHeight)));
                int botRangeY = (int) Math.ceil(chartHeight - (bottomVvalue * ((rows * rowHeight))));

                Drawing.fillArea(m_img, 0x40ffffff, 0, 0, m_imgWidth, topRangeY);
                Drawing.fillArea(m_img, 0x40ffffff, 0, botRangeY, m_imgWidth, chartHeight);

                String topRangeString = String.format("%." + m_numberClass.getDecimals() + "f", topRangePrice);
                int topRangeStringWidth = m_fm.stringWidth(topRangeString);

                int topRangeStringX = (chartWidth + (m_scaleColWidth / 2)) - (topRangeStringWidth / 2);
                int topRangeStringY = (topRangeY - (m_labelHeight / 2)) + m_labelAscent;
                int fillTopRangeY1 = topRangeY - (m_labelHeight / 2) - 3;
                int fillTopRangeY2 = topRangeY + (m_labelHeight / 2) + 3;

                fillTopRangeY1 = fillTopRangeY1 < 1 ? 1 : fillTopRangeY1;
                fillTopRangeY2 = fillTopRangeY2 >= m_img.getHeight() -1 ? m_img.getHeight() -1 : fillTopRangeY2;
                
                Drawing.fillArea(m_img, 0xffffffff, x1,fillTopRangeY1 , x2, fillTopRangeY2);
                m_g2d.setColor(blackColor);
                m_g2d.drawString(topRangeString, topRangeStringX, topRangeStringY);

                String botRangeString = String.format("%." + m_numberClass.getDecimals() + "f", botRangePrice);
                int botRangeStringWidth = m_fm.stringWidth(botRangeString);

                int botRangeStringX = (chartWidth + (m_scaleColWidth / 2)) - (botRangeStringWidth / 2);
                int botRangeStringY = (botRangeY - (m_labelHeight / 2)) + m_labelAscent;

                Drawing.fillArea(m_img, 0xffffffff, x1, botRangeY - (m_labelHeight / 2) - 3, x2, botRangeY + (m_labelHeight / 2) + 3);
                m_g2d.drawString(botRangeString, botRangeStringX, botRangeStringY);

            }

     

        } else {

            m_g2d.setFont(m_headingFont);
            FontMetrics fm = m_g2d.getFontMetrics();
            String text = init ? "Initializing..." : m_msg;

            int stringWidth = fm.stringWidth(text);
            int fmAscent = fm.getAscent();
            int x = (m_imgWidth / 2) - (stringWidth/2);
            int y = ((m_imgHeight - fm.getHeight()) / 2) + fmAscent;
            m_g2d.setColor(Color.WHITE);
            m_g2d.drawString(text, x, y);
  
        }
        m_g2d.setColor(new Color(0xffcdd4da, true));
        m_g2d.setFont(m_labelFont);
        m_g2d.drawString(Utils.formatTimeString(Utils.milliToLocalTime(m_doUpdate.get())), m_imgWidth-85, m_img.getHeight());
        /*   File outputfile = new File("image.png");
        try {
            ImageIO.write(img, "png", outputfile);
        } catch (IOException e) {

        } */
        return SwingFXUtils.toFXImage(m_img,null);
      
    }

    public void shutdown(){
        if(m_g2d != null){
            m_g2d.dispose();
            m_g2d = null;
        }
        m_img = null;
    }




    public boolean isPositive(){
        return m_isPositive;
    }

    
}

/*           for (int y = 0; y < height; y++) {
                for (int x = 0; x < m_imgWidth; x++) {
                    int p = img.getRGB(x, y);

                      int a = (p >> 24) & 0xff;
                    int r = (p >> 16) & 0xff;
                    int g = (p >> 8) & 0xff;
                    int b = p & 0xff;
                    if ((y >= topY && y < bottomY) && (x >= newX)) {
                        newImg.setRGB(x - newX, y - topY, p);
                    }

                }
            }
 */
