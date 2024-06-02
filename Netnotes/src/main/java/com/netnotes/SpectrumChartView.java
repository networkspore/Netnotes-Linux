package com.netnotes;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.image.WritableImage;

public class SpectrumChartView {

    
    public final static int DECIMAL_PRECISION = 6;
   
    public final static int MIN_CHART_HEIGHT = 300;
    public final static int MIN_CHART_WIDTH = 200;
    
    public final static int DEFAULT_CELL_WIDTH = 20;
    public final static int DEFAULT_CELL_PADDING = 3;
    public final static int MAX_CHART_WIDTH = 3840;
    public final static int MAX_CHART_HEIGHT = 2160;

    public final static int CANDLE_GRAPH = 0;
    public final static int MINIMAL_LINE_GRAPH = 1;

    public static final long oneHourMillis = (60 * 60 * 1000);
    public static final long oneDayMillis = 60*60*24 * 1000;
    public static final long oneWeekMillis = 60L*60L*24L * 7L * 1000L;

    public static final long oneMonthMillis = 60L*60L*24L * 7L * 1000L * 4L;
    public static final long sixMonthMillis = 60L*60L*24L * 7L * 1000L * 4L * 6L;
    public static final long oneYearMillis = 60L*60L*24L * 365L * 1000L;



    private int m_valid = 0;
    private Color m_backgroundColor = new Color(1f, 1f, 1f, 0f);


    private int m_decimals = DECIMAL_PRECISION;

    private String m_msg = "Loading";


    // private BufferedImage m_img = null;

    
    private int m_connectionStatus = SpectrumFinance.STOPPED;
   

    private static int m_defaultColor = new Color(0f, 0f, 0f, 0.01f).getRGB();


    private long m_lastTimeStamp = 0;


    //private double m_lastClose = 0;
    private static int m_labelSpacingSize = 150;
    private static Color m_labelColor = new Color(0xc0ffffff);

    private static int m_greenHighlightRGB = 0x504bbd94;
    //  int m_greenHighlightRGB2 = 0x80028a0f;
    private static int m_redRGBhighlight = 0x50e96d71;
    //  int redRGBhighlight2 = 0x809a2a2a;
    private static Color m_highlightGreen = KucoinExchange.POSITIVE_HIGHLIGHT_COLOR;
    private static Color m_baseRed = KucoinExchange.NEGATIVE_COLOR;
    private static Color m_highlightRed = KucoinExchange.NEGATIVE_HIGHLIGHT_COLOR;

    private static Color m_overlayRed = new Color(0x709A2A2A, true);
    private static Color m_overlayHighlightRed = new Color(0x70e96d71, true);


    private SpectrumMarketData m_marketData;
    private SpectrumFinance m_spectrumFinance;

    private static Font m_arial = new Font("Arial", Font.PLAIN, 12);


    private SpectrumPrice[] m_data = null; 
    
    

   // private volatile int m_tries = 0;

    private ArrayList<SpectrumMarketInterface> m_msgListeners = new ArrayList<>();

    public SpectrumChartView(SpectrumMarketData marketData, SpectrumFinance spectrumFinance) {
        m_marketData = marketData;
        m_spectrumFinance = spectrumFinance;
    }
 
    public long addPrices(SpectrumPrice[] prices){
        
        
        if(m_data != null && m_data.length > 0 && prices != null && prices.length > 0){
            
          
            int size = m_data.length;

            SpectrumPrice lastItem = m_data[size -1];
            long lastTimeStamp = lastItem.getTimeStamp();

            int priceLength = prices.length;
            int startIndex = 0;                 
            
            for(int i = 0; i < priceLength ; i ++){
                SpectrumPrice p = prices[i];
                if(p.getTimeStamp() > lastTimeStamp){
                    break;
                }
                startIndex++;
            }
           
            int itemsLeft = priceLength - startIndex;

            if(size > 0 && itemsLeft > 0){
                int newSize = size + itemsLeft;
                
                SpectrumPrice[] data = Arrays.copyOf(m_data, newSize);
                System.arraycopy(prices, startIndex, data, size, itemsLeft);
              
                SpectrumPrice newLastItem = data[newSize -1];
                m_data = data;

                return newLastItem.getTimeStamp();
            }

        }
        
        return -1;
    }
    public void addMsgListener(SpectrumMarketInterface item) {
        if (!m_msgListeners.contains(item)) {

            if(m_connectionStatus == SpectrumFinance.STARTED){
                item.sendMessage(SpectrumFinance.STARTED, m_data[m_data.length-1].getTimeStamp());
            }else{
                start();
            }
            
            m_msgListeners.add(item);
            if(m_marketData.getQuoteSymbol().equals("SigUSD")){
                try {
                    Files.writeString(App.logFile.toPath(),"added item:" + item.getId(), StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                } catch (IOException e) {
            
                }
            }  
        }

    }

    public void start(){

 
 
        getPoolData();
       
    }



    public SpectrumMarketInterface getListener(String id) {
        
        for (int i = 0; i < m_msgListeners.size(); i++) {
            SpectrumMarketInterface listener = m_msgListeners.get(i);
            if (listener.getId().equals(id)) {
                return listener;
            }
        }
        return null;
    }

    public boolean removeMsgListener(SpectrumMarketInterface item) {
        
        if (item != null) {
            boolean removed = m_msgListeners.remove(item);
            
            
            if (m_msgListeners.size() == 1) {
                stop();
            }
            if(m_marketData.getQuoteSymbol().equals("SigUSD")){
                try {
                    Files.writeString(App.logFile.toPath(),"removed " + item.getId(), StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                } catch (IOException e) {
            
                }
            }  
            return removed;
        }

        return false;
    }

    public void stop(){
        m_connectionStatus = SpectrumFinance.STOPPED;
        m_data = null;
        m_lastTimeStamp = 0;
    }
    
    public void sendMessage(int msg){
        long timestamp = System.currentTimeMillis();
        for(int i = 0; i < m_msgListeners.size() ; i++){
            m_msgListeners.get(i).sendMessage(msg, timestamp);
        }
    }

    public void sendMessage(int msg, long timestamp){

        for(int i = 0; i < m_msgListeners.size() ; i++){
            m_msgListeners.get(i).sendMessage(msg, timestamp);
        }
    }
    public void sendMessage(int code, long timestamp, String msg){

        for(int i = 0; i < m_msgListeners.size() ; i++){
            m_msgListeners.get(i).sendMessage(code, timestamp, msg);
        }
    }


    public void getPoolData(){
        
        String poolId = m_marketData.getPoolId();
        long currentTime = Utils.getNowEpochMillis();

        m_spectrumFinance.getPoolChart(poolId, currentTime, (onSucceeded)->{
            Object succededObject = onSucceeded.getSource().getValue();
            if(succededObject != null && succededObject instanceof JsonArray){
                JsonArray json = (JsonArray) succededObject;
                getSpectrumPriceArray(m_spectrumFinance.getExecService(), json, (onSpectrumData)->{
                    Object sourceObject = onSpectrumData.getSource().getValue();
                    if(sourceObject != null && sourceObject instanceof SpectrumPrice[]){
                        m_data = (SpectrumPrice[]) sourceObject;
                        if(m_data.length > 2){
                            m_lastTimeStamp = m_data[m_data.length -1].getTimeStamp();
                            m_connectionStatus = SpectrumFinance.STARTED;
                            sendMessage(SpectrumFinance.STARTED, m_lastTimeStamp);
                        }
                    }
                }, (onFailed)->{
                    m_connectionStatus = SpectrumFinance.ERROR;
                    sendMessage(SpectrumFinance.ERROR, currentTime, onFailed.getSource().getException().toString());
                });

            
            }
        
        }, (onFailed)->{
            
            Throwable throwable = onFailed.getSource().getException();
            
            String msg= throwable instanceof java.net.SocketException ? "Connection unavailable" : (throwable instanceof java.net.UnknownHostException ? "Unknown host: Spectrum Finance unreachable" : throwable.toString());
            m_connectionStatus = SpectrumFinance.ERROR;
            sendMessage(SpectrumFinance.ERROR, currentTime, msg);

         });

        
    }

    

    public void update(){
        
        long currentTime = System.currentTimeMillis();
   
        
        if(m_connectionStatus != SpectrumFinance.STOPPED && m_lastTimeStamp > 0){

            String poolId = m_marketData.getPoolId();
            
    
            m_spectrumFinance.getPoolChart(poolId, m_lastTimeStamp + 1, currentTime, (onSucceeded)->{
                Object succededObject = onSucceeded.getSource().getValue();
                if(succededObject != null && succededObject instanceof JsonArray){
                    JsonArray json = (JsonArray) succededObject;
                   
                        
                        getSpectrumPriceArray(m_spectrumFinance.getExecService(), json, (onSpectrumData)->{
                            Object sourceObject = onSpectrumData.getSource().getValue();
                            if(sourceObject != null && sourceObject instanceof SpectrumPrice[]){
                                SpectrumPrice[] prices = (SpectrumPrice[]) sourceObject;
                                m_connectionStatus = SpectrumFinance.STARTED;
                                
                                if(prices.length > 0){
                                    long lastTimeStamp = addPrices(prices); 
                                    m_lastTimeStamp = lastTimeStamp;

                                    sendMessage(lastTimeStamp > 0 ? SpectrumFinance.LIST_UPDATED : SpectrumFinance.LIST_CHECKED, m_lastTimeStamp);
                                    
                                }else{

                                    sendMessage(SpectrumFinance.LIST_CHECKED, m_lastTimeStamp);
                                   
                                }
                             
                            }
                        }, (onFailed)->{
                            
                            Throwable throwable = onFailed.getSource().getException();
                            
            
                            sendMessage(SpectrumFinance.ERROR, currentTime, throwable.toString());
                        });
                    
    
                
                }
              
            }, (onFailed)->{
                
                Throwable throwable = onFailed.getSource().getException();
                
                String msg= throwable instanceof java.net.SocketException ? "Connection unavailable" : (throwable instanceof java.net.UnknownHostException ? "Unknown host: Spectrum Finance unreachable" : throwable.toString());
                
                sendMessage(SpectrumFinance.ERROR, currentTime, msg);

             });
        }
       // m_data.setPriceData(updateLastItem(m_data.getPriceData(), new SpectrumPrice(m_marketData.getLastPrice(), m_marketData.getTimeStamp())));
        /*if(m_data != null){
    
            int size = m_data.length;
                
            if(size > 2){
                SpectrumPrice secondLastPrice =m_data[size-2];
                SpectrumPrice lastPrice = m_data[size -1];
                
                if(lastPrice.getTimeStamp() < timeStamp){
                    if(secondLastPrice.getPrice().equals(lastPrice.getPrice()) && lastPrice.getPrice().equals(price)){
                        m_data[size-1] = new SpectrumPrice(price, timeStamp);
                        m_dataListChanged.set(timeStamp);
                    }else{
                    
                       addPrice(new SpectrumPrice(price, timeStamp), size);


                    }
                }
            }else{
                addPrice(new SpectrumPrice(price, timeStamp), size);
            }
        }*/
       
    }


  



    public void getSpectrumPriceArray(ExecutorService execService, JsonArray jsonArray, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){

        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() throws Exception{

                return getSpectrumPriceArray(jsonArray);
            }
        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        execService.submit(task);
    }


    public SpectrumPrice[] getSpectrumPriceArray(JsonArray jsonArray) throws Exception{
 

        int size = jsonArray.size();

        SpectrumPrice data[] = new SpectrumPrice[size];
        
        for(int i = 0; i < size ; i ++){
            SpectrumPrice spectrumPrice = new SpectrumPrice(jsonArray.get(i).getAsJsonObject());
            data[i] = spectrumPrice; 
        }

        return data; 
    }

    public SpectrumPrice[] getSpectrumData(){
        return m_data;
    }


    public long getLastTimestamp(){
        return m_lastTimeStamp;
    }

    public void setLastTimeStamp(long timestamp){
        m_lastTimeStamp = timestamp;
    }

    /*public TimeSpan getTimeSpan(){
        return m_timeSpan;
    }

    public void setTimeSpan(TimeSpan timeSpan){
        m_timeSpan = timeSpan;
    }*/

    public JsonObject getJsonObject() {
        JsonObject jsonObject = new JsonObject();
        return jsonObject;
    }

    public WritableImage getImageObject(int items, int cellWidth, int cellPadding, int scaleColWidth,int minWidth, int height, SimpleObjectProperty<WritableImage> wImgObj){
        int totalCellWidth = cellWidth + cellPadding;
        int itemsTotalCellWidth = items * totalCellWidth + cellPadding;

        
        
        int totalWidth = itemsTotalCellWidth + scaleColWidth < minWidth ? minWidth : itemsTotalCellWidth + scaleColWidth;


        boolean isNewImg = wImgObj.get() == null || (wImgObj.get() != null && ((int) wImgObj.get().getWidth() != totalWidth || (int) wImgObj.get().getHeight() != height));

        WritableImage img = isNewImg ? new WritableImage(totalWidth, height < MIN_CHART_HEIGHT ? MIN_CHART_HEIGHT : height) : wImgObj.get();
        
        if(isNewImg){
            wImgObj.set(img);
        }

        return img;
    }




   
    public void updatePriceListNumbers(ArrayList<SpectrumPriceData> priceList, SimpleObjectProperty<SpectrumNumbers> numbersObject){
        SpectrumNumbers numbers = new SpectrumNumbers();
        int size = priceList.size();

        for(int i = 0; i < size ; i++ ){
            SpectrumPriceData priceData = priceList.get(i);
            numbers.updateData(priceData);
        }

        numbersObject.set(numbers);
    }
    
    /*public void saveNewDataJson(long lastTimeStamp, JsonArray jsonArray){
      
     
        JsonObject json = new JsonObject();
        json.addProperty("lastTimeStamp", jsonArray.size() > 0 ? lastTimeStamp : 0);
        json.add("priceData", jsonArray);

        try {
            Utils.saveJson(getAppKey(), json, getMarketFile());
        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
            try {
                Files.writeString(App.logFile.toPath(), "\nSpectrumMarketItem (saveNewDataJson): " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND );
            } catch (IOException e1) {

            }
        }

    
    }*/



    public void reset() {
        reset(true);
    }
    
    public void reset(boolean update) {
        m_lastTimeStamp = 0;
        m_valid = 0;
        m_msg = "Loading";
     
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
    
    public int getConnectionStatus(){
        return m_connectionStatus;
    }


    

    public boolean updateRowChart(SpectrumNumbers spectrumNumbers, TimeSpan timeSpan, int cellWidth, BufferedImage bImg, int posColor, int negColor) throws ArithmeticException{
  

        int imgWidth = (int) bImg.getWidth();        
        int imgHeight = (int) bImg.getHeight();
        BigDecimal bigImageHeight = (BigDecimal.valueOf(imgHeight));

        SpectrumPriceData[] priceList = spectrumNumbers.getSpectrumPriceData();

        //long latestTime = System.currentTimeMillis();
  
        int priceListSize = priceList.length;
        int totalCellWidth = cellWidth;


        Drawing.clearImage(bImg);
        
      

        int numCells = (int) Math.floor(imgWidth / totalCellWidth);

        int i = numCells > priceListSize ? 0 : priceListSize - numCells;
        BigDecimal closeMiddle = BigDecimal.valueOf(0.6).multiply(bigImageHeight);

        BigDecimal openValue = spectrumNumbers.getOpen();
        BigDecimal closeValue = spectrumNumbers.getClose();

        BigDecimal highValue = spectrumNumbers.getHigh();
        BigDecimal lowValue = spectrumNumbers.getLow();
        BigDecimal spacing =  spectrumNumbers.getHigh().multiply(BigDecimal.valueOf(.05));
        
        BigDecimal scale = closeMiddle.divide(highValue, 20, RoundingMode.UP);
 
        
        BigDecimal topRangePrice = highValue.add(spacing) ;
        BigDecimal botRangePrice = lowValue.subtract(spacing);
        botRangePrice = botRangePrice.max(BigDecimal.ZERO);

        BigDecimal scale2 = bigImageHeight.divide((topRangePrice.subtract(botRangePrice)), 20, RoundingMode.UP);

        Drawing.fillArea(bImg, 0xff111111, 0, 0, imgWidth, imgHeight);

        

        //    Color green = KucoinExchange.POSITIVE_COLOR;

      //  0, 2 , 4 , 6
        
        int rowHeight = 7;

        int rows = (int) Math.floor(imgHeight / rowHeight);

        rows = rows == 0 ? 1 : rows;
  
  
        boolean isPositive = openValue.compareTo(closeValue)  <  1;


        while (i < priceListSize) {
            SpectrumPriceData priceData = priceList[i];

            int x = i * cellWidth;
       
            //open < low
            BigDecimal low =  priceData.getOpen().min(priceData.getLow());
            BigDecimal high = priceData.getHigh();
        
            low =  low.max(botRangePrice);
            low = low.min(topRangePrice);
            high = high.max(botRangePrice);
            high = high.min(topRangePrice);
        
         //   double nextOpen = (i < priceList.size() - 2 ? priceList.get(i + 1).getOpen().doubleValue() : priceData.getClose().doubleValue());
            BigDecimal close = priceData.getClose();
            BigDecimal open = priceData.getOpen();

            close = close.max(botRangePrice);
            close = close.min( topRangePrice);
            open = open.max(botRangePrice);
            open = open.min(topRangePrice);
            

            int lowY = low.multiply(scale).intValue();
            int highY = high.multiply(scale).intValue();
            int openY = open.multiply(scale).intValue();
            int closeY = close.multiply(scale).intValue();
        
            lowY = (low.subtract( botRangePrice)).multiply(scale2).intValue();
            highY =(high.subtract(botRangePrice)).multiply(scale2).intValue();
            openY = (open.subtract(botRangePrice)).multiply(scale2).intValue();
            closeY =  (close.subtract(botRangePrice)).multiply(scale2).intValue();

            lowY = Math.max(lowY, 0);
            lowY = Math.min(lowY, imgHeight);

            highY = Math.max(highY, 0);
            highY = Math.min(highY, imgHeight) ;

            openY = Math.max(openY,0);
            openY = Math.min(openY, imgHeight);

            closeY = Math.max(closeY, 0);
            closeY = Math.min(closeY,imgHeight);

          //  boolean positive = !((imgHeight - closeY) > (imgHeight - openY));
         //   boolean neutral = open == nextOpen && open == close;
            
            //switch(m_chartType){
            //    case MINIMAL_LINE_GRAPH:
         

            int pos2 = 0x40eeffee;//0x503dd9a4;
            int neg2 = negColor;//0x50e96d71;

          //  Drawing.drawLineRect(bImg,0xff028A0F , 1, x, imgHeight - lowY, x + cellWidth, imgHeight -highY);

            int barY = (imgHeight -highY);
            int barY2 = (imgHeight);
            
            int tmpbarY1 = Math.max(barY, barY2);

            barY = Math.min(barY, barY2);
            barY2 = tmpbarY1;

            int lineY1 = (imgHeight - openY);
            int lineY2 = imgHeight - closeY;

            int tmpY1 = Math.max(lineY1, lineY2);

            lineY1 = Math.min(lineY1, lineY2);
            lineY2 = tmpY1;

            Drawing.drawBar(1, isPositive ? 0x203dd944 : 0x30e96d71, 0x00111111, bImg, x, barY, x + cellWidth + 1, barY2);

            Drawing.drawFadeHLine(bImg, isPositive ? posColor : negColor ,isPositive ? pos2 : neg2, 1, x, lineY1, x + cellWidth+1, lineY2, true );

            i++;
        }

            
        
    
        return true;

        
    }

    
    public int getDecimals(){
        return m_decimals;
    }
    public void setDecimals(int decimals){
        m_decimals = decimals;
    }
    
    private static void getOpen(SimpleObjectProperty<SpectrumPrice> lastPriceObject, SimpleIntegerProperty index, long epochStart, SpectrumPrice[] dataList){
 
        while(index.get() +1 < dataList.length){
            SpectrumPrice indexPrice = dataList[index.get() +1];
            long timeStamp = indexPrice.getTimeStamp();
            if(timeStamp >= epochStart){
                break;
            }
            lastPriceObject.set(indexPrice);
            index.set(index.get() + 1);
        };
    }

    private static void addPrices(boolean isInvert, SpectrumPrice[] dataList, SimpleIntegerProperty index, SpectrumPriceData priceData){
        int size = dataList.length;

        long epochEnd = priceData.getEpochEnd();

        if(index.get() + 1 < size){         
            try{
                SimpleObjectProperty<SpectrumPrice> nextSpectrumPrice = new SimpleObjectProperty<>(isInvert ? dataList[index.get() + 1].getInverted() : dataList[index.get() + 1]);
          
                while(nextSpectrumPrice.get().getTimeStamp() <= epochEnd && nextSpectrumPrice.get().getTimeStamp() > priceData.getEpochStart()){
                    long timestamp = nextSpectrumPrice.get().getTimeStamp();
                    BigDecimal price = nextSpectrumPrice.get().getPrice();
                    priceData.addPrice(timestamp, price);
                    index.set(index.get() + 1);

                    if((index.get() +1) == size){
                        break;
                    }
                    try{
                        nextSpectrumPrice.set(isInvert ? dataList[index.get() + 1].getInverted() : dataList[index.get() + 1] );
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
    public SpectrumPrice getPrice(long startTimeStamp){
        SpectrumPrice[] data = m_data;
        if(data != null){
            int size = data.length;
            for(int i = 0; i < size ; i++){
                SpectrumPrice p = data[i];
                if(p.getTimeStamp() >= startTimeStamp){
                    return p;
                }
            }
        }
        return null;
    }
    
    public static SpectrumNumbers process(SpectrumPrice[] sDArray , boolean isInvert, long startTimeStamp, TimeSpan timeSpan,  long currentTime) throws ArithmeticException{
        SpectrumNumbers numbers = new SpectrumNumbers();

        long timeSpanMillis = timeSpan.getMillis();

        SimpleIntegerProperty index = new SimpleIntegerProperty(-1);


        int numItems = (int) Math.ceil((currentTime - startTimeStamp) / timeSpanMillis);

       

        SpectrumPriceData[] priceList = new SpectrumPriceData[numItems];
        SimpleObjectProperty<SpectrumPrice> lastPriceObject = new SimpleObjectProperty<>(null);
        
        for(int i = 0 ; i < numItems ; i++){
            long span = i * timeSpanMillis;
            long epochStart = startTimeStamp + span;
            long epochEnd = epochStart + timeSpanMillis;

            getOpen(lastPriceObject, index, epochStart, sDArray);

            SpectrumPrice lastPrice = isInvert && lastPriceObject.get() != null ? lastPriceObject.get().getInverted() : lastPriceObject.get() ;

            BigDecimal price = lastPrice != null ?  lastPrice.getPrice() : BigDecimal.ZERO;

            SpectrumPriceData lastPriceData = i == 0 ? null : priceList[i -1];

            SpectrumPriceData priceData = new SpectrumPriceData( epochStart, epochEnd, lastPriceData == null ? price : lastPriceData.getClose());

            addPrices(isInvert, sDArray, index, priceData);

            priceList[i] = priceData;

            numbers.updateData(priceData);
          
        }
        SpectrumPrice sDxm2 = sDArray[sDArray.length-2]; 
        numbers.setLastCloseDirection(sDxm2.getPrice().compareTo(numbers.getClose()) == -1);
        numbers.setSpectrumPriceData(priceList);
        numbers.setDataLength(index.get());
        return numbers;
    }
        /*    SpectrumNumbers numbers = new SpectrumNumbers();

        

        long timeSpanMillis = timeSpan.getMillis();
        SpectrumPrice firstItem = sDArray[0];
        long ts0 = firstItem.getTimeStamp();

       //SimpleIntegerProperty index = new SimpleIntegerProperty(0);
        
        long firstTimeStamp = ((((int) ((startTimeStamp - ts0) / timeSpanMillis))) * timeSpanMillis) + ts0;

       // int numItems = (int) Math.ceil((currentTime - firstTimeStamp) / timeSpanMillis);
        ArrayList<SpectrumPriceData> priceDataList = new ArrayList<>();
    
        SimpleIntegerProperty i = new SimpleIntegerProperty(0);
        SimpleLongProperty ts = new SimpleLongProperty(0);
        SimpleObjectProperty<SpectrumPrice> p = new SimpleObjectProperty<>();
        
        long arrayLength = sDArray.length;
        
        int j = 0;
        
        
        SimpleObjectProperty<SpectrumPrice> lastPrice = new SimpleObjectProperty<SpectrumPrice>(new SpectrumPrice(BigDecimal.ZERO, firstTimeStamp));

        while(i.get() < arrayLength){
            long span = j * timeSpanMillis;
            long epochStart = firstTimeStamp + span;
            long epochEnd = epochStart + timeSpanMillis;
           
            
            p.set(isInvert ? sDArray[i.get()].getInverted() : sDArray[i.get()]);

            ts.set(p.get().getTimeStamp());

            while(i.get() < arrayLength && ts.get() < epochStart){
                lastPrice.set(p.get());
                p.set(isInvert ? sDArray[i.get()].getInverted() : sDArray[i.get()]);
                ts.set(p.get().getTimeStamp());
                i.set(i.get() + 1);
            }
            
            SpectrumPriceData priceData = new SpectrumPriceData( epochStart, epochEnd, lastPrice.get().getPrice());


            while(ts.get() < epochEnd && i.get() < arrayLength){
                lastPrice.set(p.get());
                p.set(isInvert ? sDArray[i.get()].getInverted() : sDArray[i.get()]);
                

                priceData.addPrice(p.get().getTimeStamp(), p.get().getPrice());
                
                ts.set(p.get().getTimeStamp());
                i.set(i.get()+1);
                
            }
            
            priceDataList.add(priceData);

            numbers.updateData(priceData);
            j++;
        }

        SpectrumPriceData[] d = new SpectrumPriceData[priceDataList.size()];
        d = priceDataList.toArray(d);
        
        numbers.setSpectrumPriceData(d);

        return numbers;
    }*/

    public void processData(boolean isInvert, long startTimeStamp, TimeSpan timeSpan,  long currentTime, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        
        
        

        if(m_data != null ){
            int size = m_data.length;
            if(size > 2){
                Task<Object> task = new Task<Object>() {
                    @Override
                    public Object call() throws ArithmeticException {
                        

                        return process( m_data, isInvert, startTimeStamp,timeSpan, currentTime);
                    }
                };

                task.setOnFailed(onFailed);

                task.setOnSucceeded(onSucceeded);

                execService.submit(task);
            }
        }
                    
    }

    public void processData(boolean isInvert, int maxBars, TimeSpan timeSpan,  long currentTime, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        
       
        
        if(m_data != null){
             
            SpectrumPrice[] sPData = m_data;
            int size = sPData.length;
            if(size > 2){
            Task<Object> task = new Task<Object>() {
                @Override
                public Object call()throws ArithmeticException {

                    
                 //   ArrayList<SpectrumPriceData> priceList = new ArrayList<>();

        
                
                   // SpectrumNumbers numbers = new SpectrumNumbers();

                    SpectrumPrice oldestPrice = sPData[0];
                    long oldestTimeStamp = oldestPrice.getTimeStamp();

                    long timeSpanMillis = timeSpan.getMillis();

                    int maxElements = (int)((currentTime - oldestTimeStamp) / timeSpanMillis);

                    int startElement = maxElements > maxBars ? maxBars : maxElements;

                    final long startTimeStamp = currentTime - (startElement * timeSpanMillis);
                           
                    

                    return process( sPData, isInvert,startTimeStamp, timeSpan, currentTime);
                }
            };

            task.setOnFailed(onFailed);

            task.setOnSucceeded(onSucceeded);

            execService.submit(task);
        }
        }
                    
    }




 
    /*
    private void addPrices(ArrayList<SpectrumPrice> dataList, SimpleIntegerProperty index, SpectrumPriceData priceData, long epochEnd){
        int size = dataList.size();

        if(index.get() + 1 < size){         
            try{
                SimpleObjectProperty<SpectrumPrice> nextSpectrumPrice = new SimpleObjectProperty<>(dataList.get(index.get() + 1));
    
                while(nextSpectrumPrice.get().getTimeStamp() <= epochEnd){
                    long timestamp = nextSpectrumPrice.get().getTimeStamp();
                    BigDecimal price = nextSpectrumPrice.get().getPrice();
                    priceData.addPrice(timestamp, price);

            
     

                    index.set(index.get() + 1);

                    if((index.get() +1) == size){
                        break;
                    }
                    try{
                        nextSpectrumPrice.set(dataList.get(index.get() + 1));
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
    }*/






  

 /*   private int m_maxBarsOffset = 0;

    public void updateNumbers(){
        int size = m_priceList.size();
      //  

        double visualWidth = m_chartWidth.get();

        switch(m_chartType){
            case MINIMAL_LINE_GRAPH:
                int maxbars = (int) Math.floor(visualWidth / (m_cellWidth + m_cellPadding));
                m_maxBarsOffset = (size - maxbars) >= 0 ? size - maxbars : 0;
            break;
            default:
                m_maxBarsOffset = (size - MAX_BARS) >= 0 ? size - MAX_BARS : 0;
            break;
        }

   
    }*/

    public JsonObject getPriceDataJson(ArrayList<SpectrumPriceData> priceList){
        JsonObject json = new JsonObject();
        JsonArray jsonArray = new JsonArray();

        for(SpectrumPriceData dataItem : priceList){
            jsonArray.add(dataItem.getJsonObject());
        }

        json.add("priceData", jsonArray);

        return json;
    }
  
    public BigDecimal getCurrentPrice(boolean isInvert) {
        return isInvert ? m_marketData.getInvertedLastPrice() : m_marketData.getLastPrice();
    }

    public void setBackgroundColor(Color color) {
        m_backgroundColor = color;
    }

    public Color getBackgroundColor() {
        return m_backgroundColor;
    }


    public int getValid() {
        return m_valid;
    }


    /*1min, 3min, 15min, 30min, 1hour, 2hour, 4hour, 6hour, 8hour, 12hour, 1day, 1week */


    public int getChartTopY(double scale,SpectrumNumbers numberClass, BufferedImage img) {
        return img.getHeight() - (int) (scale * numberClass.getHigh(false).doubleValue());
    }

    public int getChartBottomY(double scale, SpectrumNumbers numberClass, BufferedImage img) {
        return img.getHeight() - (int) (scale * numberClass.getLow(false).doubleValue());
    }

   

    public void setMsg(String msg) {
        m_msg = msg;
        
    }

    public String getMsg() {
        return m_msg;
    }


    private static int m_rowPadding = 7;

    public static int SCALE_COL_PADDING = 40;
    
    
    public static void updateBufferedImage(BufferedImage img, Graphics2D g2d, Font labelFont, FontMetrics labelMetrics, SpectrumNumbers numbers, int cellWidth, int cellPadding, int scaleColWidth,  int amStringWidth, TimeSpan timeSpan,  RangeBar rangeBar) {
        
        if(img == null ){
            return;
        }
        SpectrumPriceData[] priceList = numbers.getSpectrumPriceData();

        if(priceList == null || (priceList != null && priceList.length == 0) ) {
            return;
        }
        RangeStyle rangeStyle = rangeBar.getRangeStyle();

        double bottomVvalue = rangeBar.bottomVvalueProperty().get();
        double topVvalue = rangeBar.topVvalueProperty().get(); 
      
        boolean isRangeSetting = rangeBar.settingRangeProperty().get(); 
        
        boolean rangeActive = rangeBar.isActive();
        boolean isAuto = rangeStyle.getId().equals(RangeStyle.AUTO) && !isRangeSetting;

        rangeActive = isAuto || (rangeActive && !isRangeSetting);
        
        LocalDateTime now = LocalDateTime.now();

        BigDecimal currentPrice = numbers.getClose();
        
        int priceListSize = priceList.length;
        int totalCellWidth = cellWidth + cellPadding;

        int width = img.getWidth(); 
        int height = img.getHeight();
        
        Drawing.fillArea(img, m_defaultColor, 0, 0, width, height, false);


        int labelHeight = labelMetrics.getHeight();

       

        int chartWidth = width - scaleColWidth;
        int chartHeight = height - (2 * (labelHeight + 5)) - 10;

        int numCells = (int) Math.floor(chartWidth / totalCellWidth);

           
        int rowHeight = labelHeight + m_rowPadding; 
            
    

        g2d.setFont(labelFont);
        g2d.setColor(m_labelColor);

        BigDecimal bigImageHeight = (BigDecimal.valueOf(height));
        BigDecimal closeMiddle = BigDecimal.valueOf(0.6).multiply(bigImageHeight);

       
        BigDecimal highValue = numbers.getHigh();
        BigDecimal lowValue = numbers.getLow();

        BigDecimal spacing =  highValue.multiply(BigDecimal.valueOf(.05));
        
        BigDecimal scale = closeMiddle.divide(highValue, 20, RoundingMode.UP);
        
        rangeStyle.setScale(scale);

        BigDecimal topRangePrice = isAuto ? highValue.add(spacing) : (isRangeSetting ? BigDecimal.valueOf(topVvalue * chartHeight).divide(scale, 15, RoundingMode.HALF_UP) : rangeStyle.getTopValue());
        BigDecimal botRangePrice = isAuto ? lowValue.subtract(spacing) : (isRangeSetting? BigDecimal.valueOf(bottomVvalue * chartHeight).divide(scale, 15, RoundingMode.HALF_UP) : rangeStyle.getBotValue());


        botRangePrice = botRangePrice.max(BigDecimal.ZERO);
        BigDecimal topChartValue = BigDecimal.valueOf(chartHeight).divide(scale, 13, RoundingMode.HALF_UP);
            
        
        BigDecimal scale2 = bigImageHeight.divide((topRangePrice.subtract(botRangePrice)), 20, RoundingMode.UP);
         
        if (isRangeSetting || isAuto ) {
            rangeStyle.setTopValue(topRangePrice);
            rangeStyle.setBottomValue(botRangePrice);
            rangeStyle.setChartHeight(chartHeight);
        }

        if(isAuto){

            double tVv = topRangePrice.divide(topChartValue, 12, RoundingMode.HALF_UP).doubleValue();
            double bVv = botRangePrice.divide(topChartValue, 12, RoundingMode.HALF_UP).doubleValue();
            
            if(topVvalue != tVv || bVv != bottomVvalue){

                rangeBar.setTopVvalue(tVv, false);
                rangeBar.setBotVvalue(bVv, true);  
                
            }

        }

        Drawing.fillArea(img, 0xff111111, 0, 0, chartWidth, chartHeight, true);

        int j = 0;

        //    Color green = KucoinExchange.POSITIVE_COLOR;

        int priceListWidth = priceListSize * totalCellWidth;
        int lblMheight = labelMetrics.getAscent() - labelMetrics.getDescent() - labelMetrics.getLeading();
        int halfHeight = labelHeight /2;
        int lblCenter = halfHeight + lblMheight/2 ;
        
        int halfLabelHeight = (labelHeight / 2);
        int halfCellWidth = cellWidth / 2;

        int startPostion = numCells > priceListSize ? 0 : priceListSize - numCells;

        int items = priceListSize - startPostion;
        int colLabelSpacing = (int)(items / (int)(priceListWidth / m_labelSpacingSize));

    

        int rows = (int) Math.floor(chartHeight / rowHeight);

        rows = rows < 1 ? 1 : rows;

        int rowsHeight = rows * rowHeight;
        
        double rowsHeightPerSpacingSize = (rowsHeight / m_labelSpacingSize);
        int rowLabelSpacing = (int) (rows / rowsHeightPerSpacingSize);

        int scaleLabelLength = (numbers.getClose() + "").length();

        for (j = 0; j < rows; j++) {

            int y = chartHeight - (j * rowHeight);
            if (j % rowLabelSpacing == 0) {
                Drawing.fillAreaDotted(2, img, 0x10ffffff, 0, y, chartWidth, y + 1);
                if (j != 0) {
                    Drawing.fillArea(img, 0xc0ffffff, chartWidth, y, chartWidth + 6, y + 2, true);
                }
            }
            Drawing.fillArea(img, 0xff000000, chartWidth, y, chartWidth + 6, y + 1, false);

           BigDecimal scaleLabelDec = rangeActive ? BigDecimal.valueOf(j * rowHeight).divide(scale2, 14, RoundingMode.HALF_UP).add(botRangePrice) : BigDecimal.valueOf(j * rowHeight).divide(scale, 14, RoundingMode.HALF_UP);

          //  String scaleAmount = String.format("%." + numbers.getDecimals() + "f", scaleLabeldbl);
            //int amountWidth = m_imageText.getStandarFontMetrics().stringWidth(scaleAmount);

            // amountWidth = amountWidth > scaleColWidth - 2 ? scaleColWidth -2 : amountWidth;

            int x1 = chartWidth + (SCALE_COL_PADDING/2);
            int y1 = (y - halfLabelHeight) + lblCenter;
            
            String scaleLabelString = scaleLabelDec + "";
            int scaleLabelStringLength = scaleLabelString.length();
            String label = scaleLabelDec.doubleValue() == 0 && j == 0 ? "0" : scaleLabelString.substring(0, Math.min(scaleLabelStringLength, scaleLabelLength));
            g2d.drawString( label, x1, y1);

            
        }
        j = 0;
        int i = startPostion;
        while (i < priceList.length) {
            SpectrumPriceData priceData = priceList[i];

            int x = ((priceListWidth < chartWidth) ? (chartWidth - priceListWidth) : 0) + (j * (cellWidth + cellPadding));
            j++;
           
           BigDecimal low =  priceData.getOpen().min(priceData.getLow());
           BigDecimal high = priceData.getHigh();


            BigDecimal nextOpen = (i < priceListSize - 2 ? priceList[i + 1].getOpen() : priceData.getClose());  
            BigDecimal close = priceData.getClose();
            BigDecimal open = priceData.getOpen();

            
            if(rangeActive){
                low =  low.max(botRangePrice);
                low = low.min(topRangePrice);
                high = high.max(botRangePrice);
                high = high.min(topRangePrice);
                close = close.max(botRangePrice);
                close = close.min( topRangePrice);
                open = open.max(botRangePrice);
                open = open.min(topRangePrice);
            }
                    
            int lowY = rangeActive ?  low.subtract(botRangePrice).multiply(scale2).intValue() : low.multiply(scale).intValue();
            int highY = rangeActive ? high.subtract(botRangePrice).multiply(scale2).intValue() : high.multiply(scale).intValue();
            int openY = rangeActive ? open.subtract(botRangePrice).multiply(scale2).intValue() : open.multiply(scale).intValue();
            int closeY = rangeActive ? close.subtract(botRangePrice).multiply(scale2).intValue(): close.multiply(scale).intValue();


            lowY = Math.max(lowY, 0);
            lowY = Math.min(lowY, chartHeight);

            highY = Math.max(highY, 0);
            highY = Math.min(highY, chartHeight) ;

            openY = Math.max(openY,0);
            openY = Math.min(openY, chartHeight);

            closeY = Math.max(closeY, 0);
            closeY = Math.min(closeY,chartHeight);
        

            boolean positive = !((height - closeY) > (height - openY));
            boolean neutral = open == nextOpen && open == close;
    
            
            drawBar(img, neutral, positive, x, highY, lowY, openY, closeY,  cellWidth, chartHeight);

            i++;
        }
        j--;
        
        int chartLabelOffset = 13;
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        DateTimeFormatter firstFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
        DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("MM/dd/YYYY");
        
        
        for(i = priceList.length-1; i >= startPostion; i--){
            SpectrumPriceData priceData = priceList[i];
            
            int x = ((priceListWidth < chartWidth) ? (chartWidth - priceListWidth) : 0) + (j * (cellWidth + cellPadding));
            j--;

            LocalDateTime localTimestamp = priceData.getLocalDateTime();
            
            if (localTimestamp != null) {
                if ((i-(priceList.length-1)) % colLabelSpacing == 0) {
                  
                    if(i != (priceList.length-1)){
                        Drawing.fillAreaDotted(2, img, 0x10ffffff, x + halfCellWidth - 1, 0, x + halfCellWidth, chartHeight);
                        Drawing.fillArea(img, 0x80000000, x + halfCellWidth - 1, chartHeight, x + halfCellWidth + 1, chartHeight + 4, true);
                    }
                   

                    String timeString = i == (priceList.length-1) ? firstFormatter.format(now) :formatter.format(localTimestamp);
                    
                    int timeStringWidth = labelMetrics.stringWidth(timeString);

                    int timeStringX = x - ((timeStringWidth - amStringWidth) / 2);


                    g2d.drawString(timeString, timeStringX, chartHeight + chartLabelOffset + lblCenter );
        

                    if (!(localTimestamp.getDayOfYear() == now.getDayOfYear() && localTimestamp.getYear() == now.getYear())) {

                        

                        timeString = yearFormatter.format(localTimestamp);

                        timeStringWidth = labelMetrics.stringWidth(timeString);

                        timeStringX = x - ((timeStringWidth - amStringWidth) / 2);

                        g2d.drawString(timeString, timeStringX, chartHeight + chartLabelOffset + labelHeight + 4 + lblCenter);
                        
                    }
                }

            }
        }


        int currentCloseY = rangeActive ? currentPrice.subtract(botRangePrice).multiply(scale2).intValue() : currentPrice.multiply(scale).intValue();

        int y = chartHeight - currentCloseY;
        int minY = (halfLabelHeight + 8);
        int maxY = chartHeight;

        boolean outOfBounds = y < minY || y > maxY;

        y = rangeActive ? (y < minY ? minY : (y>maxY ? maxY : y)) : y; 
        
        y = Math.max(0, y); 
        y = Math.min(y, chartHeight);
        
        // m_direction = m_isPositive.get() > -1;

    // m_lastClose = getCurrentPrice().doubleValue();
        int y1 = y - (halfLabelHeight + 7);
        int y2 = y + (halfLabelHeight + 5);
       // int halfScaleColWidth = (scaleColWidth / 2);

        int RGBhighlight;

        int x1 = chartWidth + 1;
        int x2 = chartWidth + scaleColWidth;

        boolean isLastPositive = numbers.getLastCloseDirection();

        if (isLastPositive) {
            RGBhighlight = m_greenHighlightRGB;
        } else {
            RGBhighlight = m_redRGBhighlight;
        }
//int direction, int RGB1, int RGB2, WritableImage img, PixelReader pR, PixelWriter pW, int x1, int y1, int x2, int y2
        Drawing.drawBar(1, 0xff000000, 0xff111111, img, x1, y1, width, y2);
        Drawing.drawBar(1, RGBhighlight, 0xff000000,img, x1, y1, width, y2);

        y2 = y2 + 1;

        Drawing.drawBar(1, 0x90000000, RGBhighlight, img, x1, y1 - 1, x2, y1 + 3);
        Drawing.drawBar(0x80555555, RGBhighlight, img, x1 - 2, y1 + 3, x1 + 1, y2 - 1);
        Drawing.drawBar(0x80555555, RGBhighlight, img, x2, y1 + 4, x2 - 2, y2);
        Drawing.drawBar(1, 0x50ffffff, RGBhighlight, img, x1, y2 - 1, x2, y2);

        Color stringColor = new java.awt.Color(0xffffffff, true);

        String closeString = String.format("%." + numbers.getDecimals() + "f", currentPrice);
    


        g2d.setColor(stringColor);
        // m_g2d.drawString(closeString, stringX, stringY);
        
        int stringX = chartWidth + 16;
        int stringY = (y - halfLabelHeight) + lblCenter;

        g2d.drawString(closeString, stringX, stringY);
  

        if (!outOfBounds) {
            g2d.setColor(isLastPositive ? KucoinExchange.POSITIVE_HIGHLIGHT_COLOR : KucoinExchange.NEGATIVE_HIGHLIGHT_COLOR);
            g2d.setFont(m_arial);
            g2d.drawString("◄", chartWidth - 9, stringY);

            g2d.setFont(labelFont);

        }

        stringX = chartWidth - 9;
        if (isLastPositive) {
            Drawing.drawBarFillColor(1, false, stringColor.getRGB(), 0xffffffff, 0xffffffff, img,  stringX, y - (labelHeight / 2) - 1, chartWidth + scaleColWidth, y + 4 - (labelHeight / 2));
            Drawing.drawBarFillColor(1, false, stringColor.getRGB(), 0xffffffff, 0xff4bbd94, img,  stringX, y + 4 - (labelHeight / 2), chartWidth + scaleColWidth, y + (labelHeight / 2));
        } else {
            Drawing.drawBarFillColor(1, false, stringColor.getRGB(), 0xffffffff, 0xffffffff, img,  stringX, y - (labelHeight / 2) - 1, chartWidth + scaleColWidth, y + 4 - (labelHeight / 2));
            Drawing.drawBarFillColor(1, false, stringColor.getRGB(), 0xffffffff, 0xffe96d71, img,  stringX, y + 4 - (labelHeight / 2), chartWidth + scaleColWidth, y + (labelHeight / 2));
        }

        int borderColor = 0xFF000000;

        Drawing.fillArea(img,  borderColor, 0, chartHeight, chartWidth, chartHeight + 1, true);

        Drawing.fillArea(img, borderColor, chartWidth, 0, chartWidth + 1, chartHeight, true);//(width - scaleColWidth, 0, width - 1, chartHeight - 1);
        if (!outOfBounds) {
            for (int x = 0; x < width - scaleColWidth - 7; x++) {
                int p =  img.getRGB(x, y);

                p = (0xFFFFFF - p) | 0xFF000000;

                img.setRGB(x, y, p);
            }
        }
        if (isRangeSetting) {

            x1 = x1 + 1;
            Color blackColor = new java.awt.Color(0xff000000, true);

//m_g2d.setFont(m_labelFont);
//int x1 = chartWidth + 17;
//int y1 = (y - halfLabelHeight) + lblCenter;


//g2d.drawString(String.format("%."+numbers.getClose().precision()+"f",scaleLabeldbl ), x1, y1);

            int topRangeY = (int) Math.ceil(chartHeight - (topVvalue * chartHeight));
            int botRangeY = (int) Math.ceil(chartHeight - (bottomVvalue * (chartHeight)));

            Drawing.fillArea(img, 0x40ffffff, 0, 0, width, topRangeY);
            Drawing.fillArea(img, 0x40ffffff, 0, botRangeY, width, chartHeight);

            String topRangeString = String.format("%." + numbers.getClose().precision() + "f", topRangePrice);
            int topRangeStringWidth = labelMetrics.stringWidth(topRangeString);

            int topRangeStringX = (chartWidth + (scaleColWidth / 2)) - (topRangeStringWidth / 2);
            int topRangeStringY = (topRangeY - halfLabelHeight) + lblCenter;
            int fillTopRangeY1 = topRangeY - (labelHeight / 2) - 3;
            int fillTopRangeY2 = topRangeY + (labelHeight / 2) + 3;

            fillTopRangeY1 = fillTopRangeY1 < 1 ? 1 : fillTopRangeY1;
            fillTopRangeY2 = fillTopRangeY2 >= height -1 ? height -1 : fillTopRangeY2;
            
            Drawing.fillArea(img, 0xffffffff, x1,fillTopRangeY1 , x2, fillTopRangeY2);
            g2d.setColor(blackColor);

            g2d.drawString(topRangeString,  topRangeStringX, topRangeStringY);
         

            String botRangeString = String.format("%." + numbers.getClose().precision() + "f", botRangePrice);
            int botRangeStringWidth = labelMetrics.stringWidth(botRangeString);

            int botRangeStringX = (chartWidth + (scaleColWidth / 2)) - (botRangeStringWidth / 2);
            int botRangeStringY = (botRangeY - halfLabelHeight) + lblCenter;

            Drawing.fillArea(img, 0xffffffff, x1, botRangeY - lblCenter - 3, x2, botRangeY + lblCenter + 3);
        
            g2d.drawString(botRangeString,  botRangeStringX, botRangeStringY);
       

        }



      
    }

    public static void drawBar(BufferedImage img, boolean neutral, boolean positive, int x, int highY, int lowY, int openY, int closeY,  int cellWidth,  int chartHeight){
        int halfCellWidth = cellWidth / 2;
        //Candle wick
        if (highY != lowY) {
            Drawing.fillArea(img, 0xffffffff, x + halfCellWidth - 1, chartHeight - highY, x + halfCellWidth, chartHeight - lowY);
         }

         if (neutral) {

             Drawing.drawBar(1, Color.lightGray.getRGB(), Color.gray.getRGB(), img,  x, chartHeight - openY - 1, x + cellWidth, chartHeight - closeY + 1);

         } else {
             if (positive) {

                 int y1 = chartHeight - closeY;
                 int y2 = (chartHeight - openY) + 1;

                 int x2 = x + cellWidth;
                 Drawing.drawBar(1, 0xff000000, 0xff111111, img,  x, y1, x2, y2);
                 Drawing.drawBar(1, m_greenHighlightRGB, 0xff000000, img,  x, y1, x2, y2);
                 Drawing.drawBar(0, 0x104bbd94, 0x004bbd94, img,x, y1, x2, y2);

                 int RGBhighlight = m_highlightGreen.getRGB();

                 Drawing.fillArea(img, 0x804bbd94, x + 1, y1, x2, y1 + 1);

                 Drawing.drawBar(0x80555555, RGBhighlight, img,  x2, y1 + 3, x2 - 2, y2);

                 Drawing.fillArea(img, 0x30000000, x, y2 - 1, x2, y2);

             } else {

                 int y1 = chartHeight - openY;
                 int y2 = chartHeight - closeY;

                 Drawing.drawBar(1, m_baseRed.getRGB(), m_highlightRed.getRGB(), img, x, y1, x + cellWidth, y2);
                 Drawing.drawBar(0, m_overlayRed.getRGB(), m_overlayHighlightRed.getRGB(), img, x, y1, x + cellWidth, y2);

                 int RGBhighlight = m_overlayHighlightRed.getRGB();

                 Drawing.fillArea(img, RGBhighlight, x, y1, x + 1, y2);
                 Drawing.fillArea(img, RGBhighlight, x, y1, x + cellWidth, y1 + 1);
                 Drawing.fillArea(img, m_baseRed.getRGB(), x + cellWidth - 1, y1, x + cellWidth, y2);

                 Drawing.fillArea(img,  m_baseRed.getRGB(), x + 1, y2 - 1, x + cellWidth - 1, y2);

             }
         }
    }

   
    /*
    public void drawBar(WritableImage img, PixelReader pR, PixelWriter pW, boolean neutral, boolean positive, int x, int highY, int lowY, int openY, int closeY,  int cellWidth,  int chartHeight){
        int halfCellWidth = cellWidth / 2;
        //Candle wick
        if (highY != lowY) {
            Drawing.fillArea(img,pR,pW, 0xffffffff, x + halfCellWidth - 1, chartHeight - highY, x + halfCellWidth, chartHeight - lowY);
         }

         if (neutral) {

             Drawing.drawBar(1, Color.lightGray.getRGB(), Color.gray.getRGB(), img, pR, pW, x, chartHeight - openY - 1, x + cellWidth, chartHeight - closeY + 1);

         } else {
             if (positive) {

                 int y1 = chartHeight - closeY;
                 int y2 = (chartHeight - openY) + 1;

                 int x2 = x + cellWidth;
                 Drawing.drawBar(1, 0xff000000, 0xff111111, img, pR, pW, x, y1, x2, y2);
                 Drawing.drawBar(1, m_greenHighlightRGB, 0xff000000, img, pR, pW, x, y1, x2, y2);
                 Drawing.drawBar(0, 0x104bbd94, 0x004bbd94, img, pR, pW, x, y1, x2, y2);

                 int RGBhighlight = m_highlightGreen.getRGB();

                 Drawing.fillArea(img, pR, pW, 0x804bbd94, x + 1, y1, x2, y1 + 1);

                 Drawing.drawBar(0x80555555, RGBhighlight, img, pR, pW, x2, y1 + 3, x2 - 2, y2);

                 Drawing.fillArea(img, pR, pW, 0x30000000, x, y2 - 1, x2, y2);

             } else {

                 int y1 = chartHeight - openY;
                 int y2 = chartHeight - closeY;

                 Drawing.drawBar(1, m_baseRed.getRGB(), m_highlightRed.getRGB(), img,pR,pW, x, y1, x + cellWidth, y2);
                 Drawing.drawBar(0, m_overlayRed.getRGB(), m_overlayHighlightRed.getRGB(), img,pR,pW, x, y1, x + cellWidth, y2);

                 int RGBhighlight = m_overlayHighlightRed.getRGB();

                 Drawing.fillArea(img, pR, pW, RGBhighlight, x, y1, x + 1, y2);
                 Drawing.fillArea(img, pR, pW, RGBhighlight, x, y1, x + cellWidth, y1 + 1);
                 Drawing.fillArea(img, pR, pW, m_baseRed.getRGB(), x + cellWidth - 1, y1, x + cellWidth, y2);

                 Drawing.fillArea(img, pR, pW, m_baseRed.getRGB(), x + 1, y2 - 1, x + cellWidth - 1, y2);

             }
         }
    } */

    



    
    public class SpectrumData {
        private SpectrumPrice[] m_dataArray;
        private SpectrumNumbers m_oneDay;
        private SpectrumNumbers m_sevenDay;
        private SpectrumNumbers m_sixMonth;
        private SpectrumNumbers m_oneMonth;
        private SpectrumNumbers m_oneYear;
        private SpectrumNumbers m_allTime; 
        
        public SpectrumData(SpectrumPrice[] data, SpectrumNumbers oneDay, SpectrumNumbers sevenDay, SpectrumNumbers sixMonth, SpectrumNumbers oneMonth, SpectrumNumbers oneYear, SpectrumNumbers allTime){
            m_dataArray = data;
            m_oneDay = oneDay;
            m_sevenDay = sevenDay;
            m_sixMonth = sixMonth;
            m_oneMonth = oneMonth;
            m_oneYear = oneYear;
            m_allTime = allTime;
        }

        public SpectrumPrice[] getPriceData(){
            return m_dataArray;
        }

        public void setPriceData(SpectrumPrice[] value){
            m_dataArray = value;
        }

        public SpectrumNumbers getOneDay(){
            return m_oneDay;
        }
        public SpectrumNumbers getSevenDay(){
            return m_sevenDay;
        }
        public SpectrumNumbers getOneMonth(){
            return m_oneMonth;
        }
        public SpectrumNumbers getSixMonth(){
            return m_sixMonth;
        }
        public SpectrumNumbers getOneYear(){
            return m_oneYear;
        }
        public SpectrumNumbers getAllTime(){
            return m_allTime;
        }

        public void setOneDay(SpectrumNumbers value){
            m_oneDay = value;
        }
        public void setSevenDay(SpectrumNumbers value){
            m_sevenDay = value;
        }
        public void setOneMonth(SpectrumNumbers value){
            m_oneMonth = value;
        }
        public void setSixMonth(SpectrumNumbers value){
            m_sixMonth = value;
        }
        public void setOneYear(SpectrumNumbers value){
            m_oneYear = value;
        }
        public void setAllTime(SpectrumNumbers value){
            m_allTime = value;
        }
    }
}

/*           for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
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
