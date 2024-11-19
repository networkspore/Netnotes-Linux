package com.netnotes;


import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;

import org.reactfx.util.FxTimer;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SpectrumMarketItem {
    public class StageKeyPress{
        private final long m_timeStamp;
        private KeyCode m_keyCode;
        public StageKeyPress(KeyCode keyCode){
            m_keyCode = keyCode;
            m_timeStamp = System.currentTimeMillis();
        }
        
        public KeyCode getKeyCode(){
    
            return m_keyCode;
        }

        public void setKeyCode(KeyCode keyCode){
            m_keyCode = keyCode;
        }
        public long getTimeStamp(){
            return m_timeStamp;
        }
    }
    public final static int LINE_HEIGHT = 32;
    public final static int CHART_WIDTH = 96*3;

    public final static double SWAP_BOX_MIN_WIDTH = 300;
    public final static String ERG_ID = "0000000000000000000000000000000000000000000000000000000000000000";
    public final static String SPF_ID = "9a06d9e545a41fd51eeffc5e20d818073bf820c635e2a9d922269913e0de369d";

    public final static String MARKET_ORDER = "Market order";
    public final static String LIMIT_ORDER = "limit order";

    public static int FILL_COLOR = 0xffffffff;
    public static java.awt.Color WHITE_COLOR = new java.awt.Color(FILL_COLOR, true);

    private SpectrumDataList m_dataList = null;
    private final SpectrumMarketData m_marketData;
    private Stage m_stage = null;

    private SimpleBooleanProperty m_showSwap = new SimpleBooleanProperty(false);
    private SimpleLongProperty m_shutdown = new SimpleLongProperty(0);

    private int m_positionIndex = 0;
    private double m_prevWidth = -1;
    private double m_prevHeight = -1;
    private double m_prevX = -1;
    private double m_prevY = -1;

    private SimpleStringProperty m_statusProperty = new SimpleStringProperty();
    private SimpleLongProperty m_ergoTokensUpdated = new SimpleLongProperty();
    private SimpleBooleanProperty m_isInvert = new SimpleBooleanProperty();
    private BufferedImage m_rowImg = null;
    private HBox m_rowBox;

    // private ArrayList<SpectrumMarketInterface> m_msgListeners = new ArrayList<>();


    public SpectrumMarketItem(SpectrumMarketData marketData, SpectrumDataList dataList) {
        m_dataList = dataList;
        m_marketData = marketData;

       

        m_dataList.isInvertProperty().addListener((obs,oldval,newval)->{
            boolean invert = newval;
            
            m_isInvert.set(m_marketData.getDefaultInvert() ? !invert : invert);
        });

        m_isInvert.set(m_marketData.getDefaultInvert() ? ! m_dataList.isInvertProperty().get():  m_dataList.isInvertProperty().get());
    }  
    

    public SimpleLongProperty ergoTokensUpdatedProeprty(){
        return m_ergoTokensUpdated;
    }

    public SimpleStringProperty statusProperty(){
        return m_statusProperty;
    }


    public SpectrumMarketData getMarketData() {
        return m_marketData;
    }


   
    HBox getRowBox(){
        return m_rowBox;
    }

    private JsonObject m_tmpObj = null;
    private JsonObject m_tmpDataObj = null; 

    public void setupRowBox() {

        String friendlyId = FriendlyId.createFriendlyId();
        double regularHeight = 50;
        double focusedHeight = 150;
        int  chartWidthOffset = 200;

        SimpleDoubleProperty chartHeightObject = new SimpleDoubleProperty(regularHeight);

        /*BufferedMenuButton menuBtn = new BufferedMenuButton();
        menuBtn.setId("menuBtn");
        menuBtn.getBufferedImageView().setFitWidth(15);*/

        /*menuBtn.setGraphic(IconButton.getIconView(new Image(m_isFavorite.get() ? "/assets/star-30.png" : "/assets/star-outline-30.png"), 30));
        favoriteBtn.setOnAction(e -> {
            boolean newVal = !m_isFavorite.get();
            m_isFavorite.set(newVal);
            if (newVal) {
                m_dataList.addFavorite(getId(), true);
            } else {
                m_dataList.removeFavorite(getId(), true);
            }
        });

        m_isFavorite.addListener((obs, oldVal, newVal) -> {
            favoriteBtn.setGraphic(IconButton.getIconView(new Image(m_isFavorite.get() ? "/assets/star-30.png" : "/assets/star-outline-30.png"), 30));
        });*/
      


        HBox openChartBtn = new HBox();
        openChartBtn.setMinWidth(10);
        openChartBtn.setPrefWidth(10);
        openChartBtn.setMinHeight(15);
        openChartBtn.setMaxHeight(15);

        openChartBtn.setId(m_marketData.getPoolId() != null ? "onlineBtn" : "offlineBtn");
        openChartBtn.setOnMouseClicked(e->{
            if(m_marketData.getPoolId() == null){
                
            }else{
                open();
            }
        });



       ImageView rowChartImgView = new ImageView();
       rowChartImgView.setPreserveRatio(false);
     
        HBox chartBox = new HBox(rowChartImgView);
        chartBox.setPadding(new Insets(0));
        chartBox.minWidthProperty().bind(m_dataList.gridWidthProperty().subtract(chartWidthOffset));


        String initialValue = m_isInvert.get() ? m_marketData.getInvertedLastPrice().doubleValue() + "" :  m_marketData.getLastPrice().doubleValue() + "";
        initialValue = initialValue.substring(0,Math.min(12, initialValue.length()));
        
       

        TextField priceText = new TextField(initialValue);
        priceText.setEditable(false);
        priceText.setId("priceText");
        priceText.setPrefWidth(100);
        
        Text symbolText = new Text(m_marketData.getCurrentSymbol(m_isInvert.get()));
        symbolText.setFont(Font.font("DejaVu Sans Mono, Book", FontWeight.NORMAL, 14));
        symbolText.setFill(App.txtColor);
        
        DropShadow shadow = new DropShadow();
        symbolText.setEffect(shadow);
        
        HBox openChartBtnBox = new HBox(openChartBtn);
        openChartBtnBox.setPadding(new Insets(5, 0,0,10));
        HBox priceHBox = new HBox(priceText, openChartBtnBox);
        HBox.setHgrow(priceHBox, Priority.ALWAYS);
        priceHBox.setAlignment(Pos.TOP_RIGHT);
        priceHBox.setPadding(new Insets(5,0,5,0));
 
        priceHBox.setMinWidth(90);

        /*HBox symbolTextBox = new HBox();
        symbolTextBox.setMaxHeight(  regularHeight);
        symbolTextBox.setMinHeight(regularHeight);
        symbolTextBox.setAlignment(Pos.CENTER_LEFT);*/

        HBox symbolTextPaddingBox = new HBox(symbolText, priceHBox);
        VBox.setVgrow(symbolTextPaddingBox, Priority.ALWAYS);
        symbolTextPaddingBox.prefWidthProperty().bind(m_dataList.gridWidthProperty());
        symbolTextPaddingBox.setAlignment(Pos.TOP_LEFT);

  
        SimpleObjectProperty<Image> baseImg = new SimpleObjectProperty<>();
        SimpleObjectProperty<Image> quoteImg = new SimpleObjectProperty<>();

        WritableImage logo = new WritableImage(150,32);
        ImageView logoView = new ImageView(logo);

        HBox imagesBox = new HBox(logoView);
        imagesBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(imagesBox,Priority.ALWAYS);
        VBox.setVgrow(imagesBox,Priority.ALWAYS);

      


        Runnable updateImages = ()->{
            NoteInterface networkInterface = m_dataList.networkInterfaceProperty().get();
            if(networkInterface == null){
                return;
            }

                    
            String baseTokenId =  m_marketData.getBaseId();
            String baseTokenName = m_marketData.getBaseSymbol();
            int baseDecimals = m_marketData.getBaseDecimals();

        
            m_tmpObj = Utils.getCmdObject("getAddToken");
            m_tmpObj.addProperty("networkId", App.TOKEN_NETWORK);

            m_tmpDataObj = new JsonObject();
            m_tmpDataObj.addProperty("tokenId", baseTokenId);
            m_tmpDataObj.addProperty("name",baseTokenName);
            m_tmpDataObj.addProperty("decimals", baseDecimals);

            m_tmpObj.add("data", m_tmpDataObj);

    
            Object getAddBaseResult = networkInterface.sendNote(m_tmpObj);

            PriceCurrency baseToken = getAddBaseResult != null && getAddBaseResult instanceof JsonObject ? new PriceCurrency((JsonObject) getAddBaseResult): null;
            if(baseToken != null){
                baseImg.set(baseToken.getIcon());
            }else{
                baseImg.set(null);
            }

            String quoteTokenId = m_marketData.getQuoteId();
            String quoteTokenName = m_marketData.getQuoteSymbol();
            int quoteDecimals = m_marketData.getQuoteDecimals();

            m_tmpObj = Utils.getCmdObject("getAddToken");
            m_tmpObj.addProperty("networkId", App.TOKEN_NETWORK);
            m_tmpDataObj = new JsonObject();
            m_tmpObj.addProperty("tokenId", quoteTokenId);
            m_tmpObj.addProperty("name", quoteTokenName);
            m_tmpObj.addProperty("decimals", quoteDecimals);
            m_tmpObj.add("data", m_tmpDataObj);

            Object getAddQuoteResult = networkInterface.sendNote(m_tmpObj);

            PriceCurrency quoteToken = getAddQuoteResult != null && getAddQuoteResult instanceof JsonObject ? new PriceCurrency((JsonObject) getAddQuoteResult): null;

            if(quoteToken != null){
                quoteImg.set(quoteToken.getIcon());
            }else{
                quoteImg.set(null);
            }

                
            m_tmpObj = null;
        };
       
        
        StackPane rowImgBox = new StackPane(chartBox, imagesBox, symbolTextPaddingBox);
        HBox.setHgrow(rowImgBox,Priority.ALWAYS);
        rowImgBox.minWidthProperty().bind(m_dataList.gridWidthProperty().subtract(chartWidthOffset));
        rowImgBox.setAlignment(Pos.CENTER_LEFT);
        rowImgBox.setPadding(new Insets(0,0,0,0));

        SimpleObjectProperty<SpectrumNumbers> numbersObject = new SimpleObjectProperty<>(null);
        
        int posColor = 0xff028A0F;
        int negColor = 0xff9A2A2A;

                       

        HBox statsBox = new HBox();
        statsBox.setId("transparentColor");
        HBox.setHgrow(statsBox, Priority.ALWAYS);
        VBox.setVgrow(statsBox, Priority.ALWAYS);
        statsBox.setPadding(new Insets(0,10,0,0));

        Text lblPercentChangeText = new Text(String.format("%-7s", "Change"));
        lblPercentChangeText.setFont(App.txtFont);
        lblPercentChangeText.setFill(Color.web("#777777"));

        Text lblOpenText = new Text(String.format("%-7s", "Open"));
        lblOpenText.setFont(App.txtFont);
        lblOpenText.setFill(Color.web("#777777"));

        Text lblHighText = new Text(String.format("%-7s", "High"));
        lblHighText.setFont(App.txtFont);
        lblHighText.setFill(Color.web("#777777"));

        Text lblLowText = new Text(String.format("%-7s", "Low"));
        lblLowText.setFont(App.txtFont);
        lblLowText.setFill(Color.web("#777777"));

        Text percentChangeText = new Text("0.00%");
        percentChangeText.setFont(App.txtFont);

        Text openText = new Text();
        openText.setFont(App.txtFont);
        openText.setFill(App.formFieldColor);
        
        Text highText = new Text();
        highText.setFont(App.txtFont);
        highText.setFill(App.formFieldColor);
        
        Text lowText = new Text();
        lowText.setFont(App.txtFont);
        lowText.setFill(App.formFieldColor);

        HBox openHbox = new HBox(lblOpenText, openText);
        openHbox.setPadding(new Insets(5));
        HBox changeHBox = new HBox(lblPercentChangeText, percentChangeText);
        changeHBox.setPadding(new Insets(5));
        HBox highHBox = new HBox(lblHighText, highText);
        highHBox.setPadding(new Insets(5));
        HBox lowHBox = new HBox(lblLowText, lowText);
        lowHBox.setPadding(new Insets(5));

        VBox statsVbox = new VBox( openHbox,changeHBox, highHBox, lowHBox);
        VBox.setVgrow(statsVbox, Priority.ALWAYS);
        statsVbox.setId("transparentColor");
        NumberFormat percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMaximumFractionDigits(2);
        

  
        Runnable updateRowImg = () ->{
            SpectrumChartView chartView =  m_marketData.getSpectrumChartView().get();
            int height = (int) chartHeightObject.get();
            boolean isCurrent = height > (int) regularHeight;
           // int w = ;
    
            int width = (int) m_dataList.gridWidthProperty().get() - (isCurrent ? chartWidthOffset : 0); //w < chartWidthOffset ? chartWidthOffset : w  ;

            if(chartView != null ){
                if(chartView.getConnectionStatus() != App.ERROR ){
            
                int cellWidth = 1;
                int maxBars = width / cellWidth;
                boolean invert = m_isInvert.get();
                TimeSpan durationSpan = m_dataList.timeSpanObjectProperty().get();
                long durationMillis = durationSpan.getMillis();
                long colSpanMillis = (durationMillis / maxBars);
                TimeSpan colSpan = new TimeSpan(colSpanMillis, "custom","id1");
                
       
                long currentTime = (long) (Math.ceil(System.currentTimeMillis() / colSpanMillis)* colSpanMillis) + colSpanMillis;

                long startTimeStamp = currentTime - durationSpan.getMillis();

                
                chartView.processData(invert, startTimeStamp, colSpan, currentTime, m_dataList.getSpectrumFinance().getExecService(), (onSucceeded)->{
                    Object sourceValue = onSucceeded.getSource().getValue();
                    if(sourceValue != null && sourceValue instanceof SpectrumNumbers){
                        SpectrumNumbers numbers =(SpectrumNumbers) sourceValue;
                        
               
                       

                        numbersObject.set( isCurrent ? numbers : null);
                        
                        boolean isNewImage = (m_rowImg == null) ||  (m_rowImg != null &&(m_rowImg.getWidth() != width || m_rowImg.getHeight() != height));
                        m_rowImg = isNewImage ? new BufferedImage(width , height, BufferedImage.TYPE_INT_ARGB) : m_rowImg; 
                        
                        
                        chartView.updateRowChart(numbers, colSpan, cellWidth, m_rowImg, posColor, negColor);
                        
                        rowChartImgView.setImage(SwingFXUtils.toFXImage(m_rowImg, m_rowWImg));
                        rowChartImgView.setFitWidth(m_rowImg.getWidth());
                        rowChartImgView.setFitHeight(m_rowImg.getHeight());

                        String priceString = String.format("%-12s", numbers.getClose().doubleValue()).substring(0,12).trim();
                        
                        priceText.setPrefWidth(isCurrent ? 130 : 100);
                        priceText.setText(priceString);
               
                        
             
                    }

                }, (onFailed)->{
                   
                    rowChartImgView.setFitWidth(width);
                    rowChartImgView.setFitHeight(height);
                });
                }else{
                     
                rowChartImgView.setFitWidth(width);
                rowChartImgView.setFitHeight(height);
                }

                
            }else{
               
            }
        };
        

        ChangeListener<Number> widthListener = (obs,oldval,newval) ->{
            updateRowImg.run();
        };
        ChangeListener<Number> heightListener = (obs,oldval,newval) ->{
            updateRowImg.run();
        };
        


        ChangeListener<TimeSpan> timeSpanListener = (obs,oldval,newval) ->{
            updateRowImg.run();
        };
      
  

        SimpleObjectProperty<NoteMsgInterface> listListenerObject = new SimpleObjectProperty<>(null);

        Runnable addChartViewListener = ()->{
            SpectrumChartView chartView = m_marketData.getSpectrumChartView().get();
            
            if(chartView != null && listListenerObject.get() == null){
                NoteMsgInterface msgInterface = new NoteMsgInterface() {
                    public String getId() {
                        return friendlyId;
                    }
                    
                    public void sendMessage(int code, long timeStamp, String networkId, Number num){
                      
                        switch(code){
                            case App.STARTED:
                            case App.LIST_UPDATED:
                                updateRowImg.run();
                                
                            break;
                
                            case App.STOPPED:

                            break;
                        }   
                        
                    }

                    public void sendMessage(int code, long timestamp, String networkId, String msg){
                        switch(code){
                            case App.ERROR:
                            updateRowImg.run();
                            break;
                        }   
                    }

                  
                 
                };
                
                listListenerObject.set(msgInterface);

                chartView.addMsgListener(msgInterface);
       
            }
        };

                 
        chartHeightObject.addListener(heightListener);
        m_dataList.gridWidthProperty().addListener(widthListener);
        m_dataList.timeSpanObjectProperty().addListener(timeSpanListener);
   

     

        Region phb0 = new Region();
        phb0.setMinWidth(15);
        HBox.setHgrow(phb0,Priority.ALWAYS);

        Region phb1 = new Region();
        HBox.setHgrow(phb1,Priority.ALWAYS);

      

        VBox priceVBox = new VBox( statsBox);
        HBox.setHgrow(priceVBox,Priority.ALWAYS);
        priceVBox.setAlignment(Pos.CENTER_LEFT);

        /*HBox menuBtnBox = new HBox(menuBtn);
        menuBtnBox.setMaxHeight(32);
        menuBtnBox.setAlignment(Pos.CENTER_LEFT);

        VBox leftMarginVBox = new VBox(menuBtnBox);
        leftMarginVBox.setAlignment(Pos.TOP_CENTER);
        leftMarginVBox.setId("darkBox");*/

        HBox rowBox = new HBox( rowImgBox,  priceVBox );
        rowBox.setId("rowBox");
        rowBox.setAlignment(Pos.TOP_LEFT);
        rowBox.maxWidthProperty().bind(m_dataList.gridWidthProperty());
        rowBox.setFocusTraversable(true);

        rowBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e->{
            
            m_dataList.currentBoxProperty().set(rowBox);
            
            if(e.getClickCount() == 2){
                open();
            }
            
        });

        rowBox.setAlignment(Pos.CENTER_LEFT);
     


        
        Runnable update = ()->{
            boolean isInvert = m_isInvert.get();
            boolean isChart = m_marketData.getPoolId() != null;
            openChartBtn.setId(m_marketData != null ? (isChart ? "availableBtn" : "offlineBtn") : "offlineBtn");
            symbolText.setText(m_marketData.getCurrentSymbol(isInvert));
            if(!isChart){
                priceText.setText(isInvert ? m_marketData.getInvertedLastPrice().doubleValue() + "" : m_marketData.getLastPrice().doubleValue() + "");
            }

        };

        Runnable updateLogo = ()->{
            
            Drawing.clearImage(logo);
            Image qImg = quoteImg.get();
          

            Image bImg = baseImg.get();
            int limitAlpha = 0x40;
            
            if(m_isInvert.get()){
                if(qImg != null){
                    double halfQWidth = (qImg.getWidth()/2);
                    int qX = (int)((logo.getWidth()/2) - halfQWidth);
                    int qY = (int)( (logo.getHeight()/2) - (qImg.getHeight() /2));
                    
                    Drawing.drawImageLimit(logo, logo.getPixelReader(), logo.getPixelWriter(), qImg ,  qX -(int)(halfQWidth/2), qY, limitAlpha);
                }
                if(bImg != null){
                    double halfBWidth = (bImg.getWidth() / 2);
                    int bX = (int)((logo.getWidth()/2) - halfBWidth);
                    int bY = (int)( (logo.getHeight()/2) - (bImg.getHeight() / 2));
                    
                    Drawing.drawImageLimit(logo, logo.getPixelReader(), logo.getPixelWriter(), bImg, bX + (int)(halfBWidth/2), bY, limitAlpha);
                }
            }else{
                if(bImg != null){
                    double halfBWidth = (bImg.getWidth() / 2);
                    int bX = (int)((logo.getWidth()/2) - halfBWidth);
                    int bY = (int)( (logo.getHeight()/2) - (bImg.getHeight() / 2));

                    Drawing.drawImageLimit(logo, logo.getPixelReader(), logo.getPixelWriter(), bImg, bX - (int) (halfBWidth/2), bY, limitAlpha);
                }
                if(qImg != null){
                    double halfQWidth = (qImg.getWidth()/2);
                    int qX = (int)((logo.getWidth()/2) - halfQWidth);
                    int qY = (int)( (logo.getHeight()/2) - (qImg.getHeight() /2));
                    
                    Drawing.drawImageLimit(logo, logo.getPixelReader(), logo.getPixelWriter(), qImg, qX + (int) (halfQWidth/2), qY, limitAlpha);
                }
            }
        };

        baseImg.addListener((obs,oldval,newval)->updateLogo.run());
        quoteImg.addListener((obs,oldval,newval)->updateLogo.run());


        ChangeListener<HBox> currentBoxChangeListener = (obs,oldval,newval)->{
            boolean isCurrent = newval != null && newval.equals(rowBox);
            rowBox.setId(isCurrent ? "headingBox" : "rowBox");
            chartHeightObject.set(isCurrent ? focusedHeight : regularHeight);
        
            imagesBox.setAlignment(isCurrent ? Pos.TOP_LEFT : Pos.CENTER);
        
        };

  
        m_dataList.currentBoxProperty().addListener(currentBoxChangeListener);

        
        numbersObject.addListener((obs,oldval,newval)->{
            if(newval != null){
                symbolTextPaddingBox.getChildren().remove(priceHBox);
                if(!statsBox.getChildren().contains(statsVbox)){
                    statsBox.getChildren().add(statsVbox);
                  
                    statsVbox.getChildren().add(0,priceHBox);
                    
                }
              
                openText.setText(String.format("%-12s",newval.getOpen().doubleValue() + "").substring(0,12));
                highText.setText(String.format("%-12s",  newval.getHigh().doubleValue() + "").substring(0,12) );
                lowText.setText(String.format("%-12s",newval.getLow().doubleValue() + "").substring(0,12) );

                BigDecimal increase = newval.getPercentIncrease();
                increase = increase == null ? BigDecimal.ZERO : increase;

                int increaseDirection = BigDecimal.ZERO.compareTo(increase);
               
                
                percentChangeText.setText(increaseDirection == 0 ? "0.00%" : (increaseDirection == -1 ? "+" :"") + percentFormat.format(increase));
                percentChangeText.setFill(increaseDirection == 0 ? App.txtColor  : (increaseDirection == -1 ? Color.web("#028A0F") : Color.web("#ffb8e8")) );
            
            }else{
                statsVbox.getChildren().remove(priceHBox);
                symbolTextPaddingBox.getChildren().add(priceHBox);
                statsBox.getChildren().clear();
            
            }
        });

        

        ChangeListener<LocalDateTime> marketDataUpdated = (obs, oldVal, newVal) -> update.run();
        ChangeListener<Boolean> invertChanged = (obs,oldval,newval)->{
            update.run();
            updateLogo.run();
            updateRowImg.run();
        };
      
        m_isInvert.addListener(invertChanged);

        m_marketData.getLastUpdated().addListener(marketDataUpdated);

        update.run();

        updateImages.run();

        if(m_marketData.isPool()){
            addChartViewListener.run();
        }

        ChangeListener<SpectrumChartView> chartViewChangeListener = (obs,oldval,newval)->{
            if(oldval != null && listListenerObject.get() != null){
                oldval.removeMsgListener(listListenerObject.get());
                listListenerObject.set(null);
            }
            updateRowImg.run();
            
            if(newval != null){
               addChartViewListener.run();
            }
            
        };
        
        m_marketData.getSpectrumChartView().addListener(chartViewChangeListener);
        


        ChangeListener<String> statusChangeListener = (obs,oldval,newval)->{
            updateRowImg.run();
        };

        m_dataList.statusMsgProperty().addListener(statusChangeListener);
        
        Button removeShutdown = new Button();

        Runnable shutdown = () ->
        {
            SpectrumChartView chartView = m_marketData.getSpectrumChartView().get();
            
            m_marketData.getSpectrumChartView().removeListener(chartViewChangeListener);

            if(chartView != null && listListenerObject.get() != null){ 

                chartView.removeMsgListener(listListenerObject.get());
                listListenerObject.set(null);
            }

            chartHeightObject.removeListener(heightListener);
            m_dataList.gridWidthProperty().removeListener(widthListener);
            m_dataList.timeSpanObjectProperty().removeListener(timeSpanListener);
            m_dataList.currentBoxProperty().removeListener(currentBoxChangeListener);

            m_marketData.getLastUpdated().removeListener(marketDataUpdated);
       
            m_dataList.statusMsgProperty().removeListener(statusChangeListener);

           
            removeShutdown.fire();

        };
        
        ChangeListener<Number> shutdownListener = (obs,oldval,newval)->shutdown.run();

        m_shutdown.addListener(shutdownListener);

        removeShutdown.setOnAction(e->{
            m_shutdown.removeListener(shutdownListener);
        });
        m_rowBox = rowBox;
    }

    public void init(){
        if(m_marketData.isPool()){
            boolean isSet = m_marketData.getSpectrumChartView().get() == null;            
            SpectrumChartView chartView = isSet ? new SpectrumChartView(m_marketData, m_dataList.getSpectrumFinance()) : m_marketData.getSpectrumChartView().get();

            if(isSet){
                m_marketData.getSpectrumChartView().set(chartView);
                
            }

        }
        setupRowBox();
    }



 

    public void shutdown(){
        m_shutdown.set(System.currentTimeMillis());
    
    }

    public SimpleBooleanProperty isItemInvertProperty(){

        return m_isInvert;
    }


    /*
    public HBox addPoolInfo( SpectrumChartView chartView, SimpleDoubleProperty widthObject){
         
        if(chartView == null){
            
            
            return new HBox();
        }

        TextField oneDayField = new TextField();
        oneDayField.setEditable(false);
        oneDayField.setPrefWidth(80);


        HBox oneDayBox = new HBox(oneDayField);
        oneDayBox.setAlignment(Pos.CENTER_LEFT);
        oneDayBox.setPadding(new Insets(0,5, 0, 0));

        TextField sevenDayField = new TextField();
        sevenDayField.setEditable(false);
        sevenDayField.setPrefWidth(80);

        HBox sevenDayBox = new HBox(sevenDayField);
        sevenDayBox.setAlignment(Pos.CENTER_LEFT);
        sevenDayBox.setPadding(new Insets(0,5, 0, 0));

        TextField oneMonthField = new TextField();
        oneMonthField.setEditable(false);
        oneMonthField.setPrefWidth(80);


        HBox oneMonthBox = new HBox(oneMonthField);
        oneMonthBox.setAlignment(Pos.CENTER_LEFT);
        oneMonthBox.setPadding(new Insets(0,5, 0, 0));

        TextField sixMonthField = new TextField();
        sixMonthField.setEditable(false);
        sixMonthField.setPrefWidth(80);

        

        HBox sixMonthBox = new HBox(sixMonthField);
        sixMonthBox.setAlignment(Pos.CENTER_LEFT);
        sixMonthBox.setPadding(new Insets(0,5, 0, 0));

        HBox daysBox = new HBox();
        daysBox.setPadding(new Insets(0));

        
    
        Runnable updateOneDay = () ->{
           
            if(chartView != null){
                SpectrumNumbers oneDay = chartView.oneDayProperty().get();
                if(oneDay != null){
                    BigDecimal increase = oneDay.getPercentIncrease(isItemInvertProperty().get());
                    oneDayField.setText(increase.equals(BigDecimal.ZERO) ? "0%" : increase.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP) + "%");
                    oneDayField.setId(increase.equals(BigDecimal.ZERO) ? "rowFieldGreen" : BigDecimal.ZERO.compareTo(increase) <= 0 ? "rowFieldGreen" : "rowFieldRed");
                }else{
                    oneDayField.setText("");
                }
            }
        };

        chartView.oneDayProperty().addListener((obs,oldval,newval)->updateOneDay.run());

        Runnable updateSevenDay = () ->{
     
            if(chartView != null){
                SpectrumNumbers sevenDay = chartView.sevenDayProperty().get();
                if(sevenDay != null){
                    BigDecimal increase = sevenDay.getPercentIncrease(isItemInvertProperty().get());
                    sevenDayField.setText(increase.equals(BigDecimal.ZERO) ? "0%" : increase.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP) + "%");
                    sevenDayField.setId(increase.equals(BigDecimal.ZERO) ? "rowFieldGreen" : BigDecimal.ZERO.compareTo(increase) <= 0? "rowFieldGreen" : "rowFieldRed");
            
                }else{
                    sevenDayField.setText("");
                }
            }
        };

        chartView.sevenDayProperty().addListener((obs,oldval,newval)->updateSevenDay.run());
        
        Runnable updateOneMonth =()->{

            if(chartView != null){
                SpectrumNumbers oneMonth = chartView.oneMonthProperty().get();
                if(oneMonth != null){
                    
                    BigDecimal increase = oneMonth.getPercentIncrease(isItemInvertProperty().get());
                    oneMonthField.setText(increase.equals(BigDecimal.ZERO) ? "0%" : increase.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP) + "%");
                    oneMonthField.setId(increase.equals(BigDecimal.ZERO) ? "rowFieldGreen" : BigDecimal.ZERO.compareTo(increase) <=0 ? "rowFieldGreen" : "rowFieldRed");
                }else{
                    oneMonthField.setText("");
                }
            }
        };

        chartView.oneMonthProperty().addListener((obs,oldval,newval)->updateOneMonth.run());

        Runnable updateSixMonth = ()->{
          
            if(chartView != null){
                SpectrumNumbers newval = chartView.sixMonthProperty().get();
                if(newval != null){
                    BigDecimal increase = newval.getPercentIncrease(isItemInvertProperty().get());
                    sixMonthField.setText(increase.equals(BigDecimal.ZERO) ? "0%" : increase.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP) + "%");
                    sixMonthField.setId(increase.equals(BigDecimal.ZERO) ? "rowFieldGreen" : BigDecimal.ZERO.compareTo(increase) <=0 ? "rowFieldGreen" : "rowFieldRed");
                }else{
                    sixMonthField.setText("");
                }
            }
        };

        chartView.sixMonthProperty().addListener((obs,oldval,newval)->updateSixMonth.run());

        updateOneDay.run();
        updateSevenDay.run();
        updateOneMonth.run();
        updateSixMonth.run();

        m_isInvert.addListener((obs,oldval,newval)->{
            updateOneDay.run();
            updateSevenDay.run();
            updateOneMonth.run();
            updateSixMonth.run();
        });

      

        Runnable updateWidth = ()->{
        
            if(m_marketData.getPoolId() != null ){
                if(!daysBox.getChildren().contains(oneDayBox)){
                    daysBox.getChildren().add(0, oneDayBox);
                }
            }else{
                if(daysBox.getChildren().contains(oneDayBox)){
                    daysBox.getChildren().remove(oneDayBox);
                }
            }
    

            
            if(m_marketData.getPoolId() != null  ){
                if(!daysBox.getChildren().contains(sevenDayBox)){
                    daysBox.getChildren().add( sevenDayBox);
                }
            }else{
                if(daysBox.getChildren().contains(sevenDayBox)){
                    daysBox.getChildren().remove(sevenDayBox);
                }
            }

            if(m_marketData.getPoolId() != null ){
                if(!daysBox.getChildren().contains(oneMonthBox)){
                    daysBox.getChildren().add( oneMonthBox);
                }
            }else{
                if(daysBox.getChildren().contains(oneMonthBox)){
                    daysBox.getChildren().remove(oneMonthBox);
                }
            }
            if(m_marketData.getPoolId() != null ){
                if(!daysBox.getChildren().contains(sixMonthBox)){
                    daysBox.getChildren().add( sixMonthBox);
                }
            }else{
                if(daysBox.getChildren().contains(sixMonthBox)){
                    daysBox.getChildren().remove(sixMonthBox);
                }
            }

        };
        updateWidth.run();

  
        return daysBox;
    }
    */

    public String returnGetId() {
        return getId();
    }


    public void open(){
        showStage();
    }
    
    public boolean isInvert(){
        return m_isInvert.get();
    }
  
    private BufferedImage m_img = null;
    private Graphics2D m_g2d = null;
    private WritableImage m_wImg = null;

    private WritableImage m_rowWImg = null;
    
    private SpectrumNumbers m_numbers = null;
    private NoteMsgInterface m_chartMsgInterface;

    public void showStage() {
        if (m_stage == null) {
            if(!m_marketData.isPool() || m_marketData.getSpectrumChartView().get() == null){
                Alert a = new Alert(AlertType.NONE, "Price history unavailable.", ButtonType.OK);
                a.setHeaderText("Notice");
                a.setTitle("Notice: Price history unavailable");
                a.showAndWait();
                return;
            }
            SpectrumChartView spectrumChartView = m_marketData.getSpectrumChartView().get();
            SimpleBooleanProperty shutdownSwap = new SimpleBooleanProperty(false);
            SimpleObjectProperty<TimeSpan> timeSpanObject = new SimpleObjectProperty<>(new TimeSpan("30min"));
       
            double sceneWidth = 900;
            double sceneHeight = 800;

            final double chartScrollVvalue = 1;
            final double chartScrollHvalue = 1;

            SimpleDoubleProperty rangeWidth = new SimpleDoubleProperty(12);
            SimpleDoubleProperty rangeHeight = new SimpleDoubleProperty(100);

            SpectrumFinance exchange = m_dataList.getSpectrumFinance();
           

            m_stage = new Stage();
            m_stage.getIcons().add(m_dataList.getSpectrumFinance().getSmallAppIcon());
            m_stage.initStyle(StageStyle.UNDECORATED);
            m_stage.setTitle(exchange.getName() + " - " + m_marketData.getCurrentSymbol(isInvert()) + (m_marketData != null ? " - " +(isInvert() ? m_marketData.getInvertedLastPrice().toString() : m_marketData.getLastPrice()) + "" : ""));

            Button maximizeBtn = new Button();
            Button closeBtn = new Button();
            Button fillRightBtn = new Button();

            HBox titleBox = App.createTopBar(m_dataList.getSpectrumFinance().getSmallAppIcon(), fillRightBtn, maximizeBtn, closeBtn, m_stage);

            BufferedMenuButton menuButton = new BufferedMenuButton("/assets/menu-outline-30.png", App.MENU_BAR_IMAGE_WIDTH);
            BufferedButton invertBtn = new BufferedButton( m_isInvert.get() ? "/assets/targetSwapped.png" : "/assets/targetStandard.png", App.MENU_BAR_IMAGE_WIDTH);
            
            invertBtn.setOnAction(e->{
                m_dataList.isInvertProperty().set(!m_dataList.isInvertProperty().get());
            });



            HBox menuAreaBox = new HBox();
            HBox.setHgrow(menuAreaBox, Priority.ALWAYS);
            menuAreaBox.setAlignment(Pos.CENTER_LEFT);

            HBox menuBar = new HBox(menuButton, menuAreaBox,  invertBtn);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 5));

            MenuItem setChartRangeItem = new MenuItem("Set price range");

            setChartRangeItem.setId("urlMenuItem");
            /*MenuItem zoomIn = new MenuItem("Zoom in             [ + ]");
            zoomIn.setId("urlMenuItem");
            MenuItem zoomOut = new MenuItem("Zoom out            [ - ]");
            zoomOut.setId("urlMenuItem");
            MenuItem resetZoom = new MenuItem("Reset zoom  [ Backspace ]");
            resetZoom.setId("urlMenuItem");
             */
            menuButton.getItems().addAll(setChartRangeItem);

    
            Text headingText = new Text(m_marketData.getCurrentSymbol(isInvert()) + "  - ");
            headingText.setFont(App.txtFont);
            headingText.setFill(Color.WHITE);

            Region headingSpacerL = new Region();
      


  

            MenuButton timeSpanBtn = new MenuButton(timeSpanObject.get().getName());
            timeSpanBtn.setFont(App.txtFont);

            timeSpanObject.addListener((obs,oldval,newval)->{
                timeSpanBtn.setText(newval.getName());
            });

            timeSpanBtn.setContentDisplay(ContentDisplay.LEFT);
            timeSpanBtn.setAlignment(Pos.CENTER_LEFT);
            
            Region headingBoxSpacerR = new Region();
            HBox.setHgrow(headingBoxSpacerR,Priority.ALWAYS);

  

            HBox headingBox = new HBox(headingSpacerL, headingText, timeSpanBtn, headingBoxSpacerR);
            headingBox.prefHeight(40);
            headingBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headingBox, Priority.ALWAYS);
            headingBox.setPadding(new Insets(5, 5, 5, 5));
            headingBox.setId("headingBox");
            
   

            RangeBar chartRange = new RangeBar(rangeWidth, rangeHeight, getNetworksData().getExecService());
            ImageView chartImageView = new ImageView();
            chartImageView.setPreserveRatio(true);
            ScrollPane chartScroll = new ScrollPane(chartImageView);
            
            headingSpacerL.prefWidthProperty().bind(headingBox.widthProperty().subtract(timeSpanBtn.widthProperty().divide(2)).subtract(headingText.layoutBoundsProperty().get().getWidth()).divide(2));
         

            Region headingPaddingRegion = new Region();
            headingPaddingRegion.setMinHeight(5);
            headingPaddingRegion.setPrefHeight(5);

            VBox paddingBox = new VBox(menuBar, headingPaddingRegion, headingBox);
            paddingBox.setPadding(new Insets(0, 5, 0, 5));

            VBox headerVBox = new VBox(titleBox, paddingBox);
            chartScroll.setPadding(new Insets(0, 0, 0, 0));

            setChartRangeItem.setOnAction((e)->chartRange.toggleSettingRange());


    
                        //⯈🗘
            Button toggleSwapBtn = new Button("⯈");
            toggleSwapBtn.setTextFill(App.txtColor);
            toggleSwapBtn.setFont( Font.font("OCR A Extended", 10));
            toggleSwapBtn.setPadding(new Insets(2,2,2,1));
            toggleSwapBtn.setId("barBtn");
            //toggleSwapBtn.setMinWidth(20);
            toggleSwapBtn.setOnAction(e->{
                m_showSwap.set(!m_showSwap.get());
            });

            Region topRegion = new Region();
            VBox.setVgrow(topRegion, Priority.ALWAYS);

          
            HBox swapButtonPaddingBox = new HBox(toggleSwapBtn);
            swapButtonPaddingBox.setId("darkBox");
            swapButtonPaddingBox.setPadding(new Insets(1,0,1,2));

            VBox swapButtonBox = new VBox(swapButtonPaddingBox);
            VBox.setVgrow(swapButtonBox,Priority.ALWAYS);
            swapButtonBox.setAlignment(Pos.CENTER);
            swapButtonBox.setId("darkBox");

            
            HBox bodyBox = new HBox(chartRange, chartScroll, swapButtonBox);
            bodyBox.setId("bodyBox");
            bodyBox.setAlignment(Pos.TOP_LEFT);
            HBox bodyPaddingBox = new HBox(bodyBox);
            bodyPaddingBox.setPadding(new Insets(0, 5, 5 ,5));

            //Binding<Double> bodyHeightBinding = Bindings.createObjectBinding(()->bodyBox.layoutBoundsProperty().get().getHeight(), bodyBox.layoutBoundsProperty());
            SimpleObjectProperty<VBox> swapBoxObject = new SimpleObjectProperty<>(null);


            toggleSwapBtn.prefHeightProperty().bind(bodyBox.heightProperty());
   



            VBox layoutBox = new VBox(headerVBox, bodyPaddingBox);


            Scene marketScene = new Scene(layoutBox, sceneWidth, sceneHeight);
            marketScene.setFill(null);
            marketScene.getStylesheets().add("/css/startWindow.css");
            m_stage.setScene(marketScene);

            
            Runnable updateShowSwap = ()->{
    
                boolean showSwap = m_showSwap.get();
    
                if( showSwap){
                    toggleSwapBtn.setText("⯈");
                    
                    if(shutdownSwap.get()){
                        shutdownSwap.set(false);
                        swapBoxObject.set( getSwapBox(marketScene, shutdownSwap));
                    }else{
                        if(swapBoxObject.get() == null){
                            swapBoxObject.set( getSwapBox(marketScene, shutdownSwap));    
                        }
                    }
                    
                    bodyBox.getChildren().add(swapBoxObject.get());
                }else{
                    toggleSwapBtn.setText("⯇");
                    
                    if(swapBoxObject.get() != null){
                        VBox swapBox = swapBoxObject.get();
                        if(bodyBox.getChildren().contains(swapBox)){
                            bodyBox.getChildren().remove(swapBox);
                        }
                      //  swapBoxObject.set(null);
                    }
                }
    
            };
    
            m_showSwap.addListener((obs,oldVal,newVal)->updateShowSwap.run());
    
            updateShowSwap.run();

            chartScroll.prefViewportWidthProperty().bind(marketScene.widthProperty().subtract(45));
            chartScroll.prefViewportHeightProperty().bind(marketScene.heightProperty().subtract(headerVBox.heightProperty()).subtract(10));

            rangeHeight.bind(marketScene.heightProperty().subtract(headerVBox.heightProperty()).subtract(65));

            int cellWidth = 20;
            int cellPadding = 3;
            java.awt.Font labelFont = m_dataList.getLabelFont();
            FontMetrics labelMetrics = m_dataList.getLabelMetrics();
            int amStringWidth = labelMetrics.stringWidth(" a.m. ");
            int zeroStringWidth = labelMetrics.stringWidth("0");

            Runnable setChartScrollRight = () ->{
                Platform.runLater(()->chartScroll.setVvalue(chartScrollVvalue));
                Platform.runLater(()->chartScroll.setHvalue(chartScrollHvalue));
            };
            

            Runnable createChart = () ->{
                Bounds bounds = chartScroll.viewportBoundsProperty().get();
                          
                int viewPortHeight = (int) bounds.getHeight();
                int viewPortWidth = (int) bounds.getWidth();
                int maxBars =  (SpectrumChartView.MAX_CHART_WIDTH / (cellWidth + cellPadding));

                TimeSpan timeSpan = timeSpanObject.get();
                long timestamp = System.currentTimeMillis();

                spectrumChartView.processData(
                    m_isInvert.get(), 
                    maxBars, 
                    timeSpanObject.get(),
                    timestamp,
                     m_dataList.getSpectrumFinance().getExecService(), 
                     (onSucceeded)->{
                        Object sourceValue = onSucceeded.getSource().getValue();
                        if(sourceValue != null && sourceValue instanceof SpectrumNumbers){
                            SpectrumNumbers numbers = (SpectrumNumbers) sourceValue;
                            
                            int size = numbers.dataLength();

                        
                            if(size > 0){
                           
                                int totalCellWidth = cellWidth + cellPadding;
                                
                                int itemsTotalCellWidth = size * (totalCellWidth);

                                int scaleLabelLength = (numbers.getClose() +"").length();

                                int scaleColWidth =  (scaleLabelLength * zeroStringWidth )+ SpectrumChartView.SCALE_COL_PADDING;
                                
                                

                                int width =Math.max(viewPortWidth, Math.max(itemsTotalCellWidth + scaleColWidth, SpectrumChartView.MIN_CHART_WIDTH));
                                
                                int height = Math.min(SpectrumChartView.MAX_CHART_HEIGHT, Math.max(viewPortHeight, SpectrumChartView.MIN_CHART_HEIGHT));

                                boolean isNewImg = m_img == null || (m_img != null && (m_img.getWidth() != width || m_img.getHeight() != height));
                            
                                m_img = isNewImg ? new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB) : m_img;
                                m_g2d = isNewImg ? m_img.createGraphics() : m_g2d;
                               
                                m_numbers = numbers;
            
                                
                                SpectrumChartView.updateBufferedImage(
                                    m_img, 
                                    m_g2d, 
                                    labelFont, 
                                    labelMetrics, 
                                    numbers,
                                    cellWidth, 
                                    cellPadding, 
                                    scaleColWidth,
                                    amStringWidth,
                                    timeSpan, 
                                    chartRange
                                );

                                /*File saveFile = new File(m_marketData.getCurrentSymbol(false) + ".json");
                                SpectrumPrice[] data = spectrumChartView.getSpectrumData();
                                String lastPrices = "";
                                for(int i = data.length-1 ; i > data.length -6 ; i --){
                                    lastPrices += "\n " + data[i].getPrice().doubleValue() + " ";
                                }
                                try {
                                    Files.writeString(saveFile.toPath(), lastPrices + " isPositive: " + numbers.getLastCloseDirection(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                    
                                } catch (IOException e1) {
                            
                                }*/
                               
                               
                                chartImageView.setImage(SwingFXUtils.toFXImage(m_img, m_wImg));
                                
                                if(isNewImg){
                                    setChartScrollRight.run();
                                    if(viewPortWidth > SpectrumChartView.MAX_CHART_WIDTH){
                                        chartImageView.setFitWidth(viewPortWidth);
                                    }else{
                                        
                                        chartImageView.setFitWidth(m_img.getWidth());
                                        
                                    }
                                    
                                }
                            }
                        }
                        
                     }, 
                     (onFailed)->{

                     });
            };
            
            Runnable updateChart = () ->{
                
                TimeSpan timeSpan = timeSpanObject.get();
               
                if(m_numbers != null){
                    SpectrumNumbers numbers = m_numbers;
                    
                    int size = numbers.dataLength();

                
                    if(size > 0){
                    
                        int totalCellWidth = cellWidth + cellPadding;
                        
                        int itemsTotalCellWidth = size * (totalCellWidth);

                        int scaleLabelLength = (numbers.getClose() +"").length();

                        int scaleColWidth =  (scaleLabelLength * zeroStringWidth )+ SpectrumChartView.SCALE_COL_PADDING;
                        
                        Bounds bounds = chartScroll.viewportBoundsProperty().get();
                        int viewPortHeight = (int) bounds.getHeight();
                        int viewPortWidth = (int) bounds.getWidth();
                        
                        int width = (itemsTotalCellWidth + scaleColWidth) < 300 ? 300 : itemsTotalCellWidth + scaleColWidth;
                        int height = Math.min(SpectrumChartView.MAX_CHART_HEIGHT, Math.max(viewPortHeight, SpectrumChartView.MIN_CHART_HEIGHT));

                        

                        boolean isNewImg = m_img == null || (m_img != null && (m_img.getWidth() != width || m_img.getHeight() != height));
                    
                        m_img = isNewImg ? new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB) : m_img;
                        m_g2d = isNewImg ? m_img.createGraphics() : m_g2d;
                        
                        
                        SpectrumChartView.updateBufferedImage(
                            m_img, 
                            m_g2d, 
                            labelFont, 
                            labelMetrics, 
                            numbers,
                            cellWidth, 
                            cellPadding, 
                            scaleColWidth,
                            amStringWidth,
                            timeSpan, 
                            chartRange
                        );
                        
                        chartImageView.setImage(SwingFXUtils.toFXImage(m_img, m_wImg));
                        if(isNewImg){
                            setChartScrollRight.run();
                            double w = Math.max(viewPortWidth, SpectrumChartView.MAX_CHART_WIDTH);
                            if(w > viewPortHeight){
                                chartImageView.setFitWidth(w);
                            }else{
                                chartImageView.setFitHeight(viewPortHeight);
                            }
                        }
                    }
                }
                        
                     
            };

            
            ChangeListener<Bounds> boundsChangeListener = (obs,oldval,newval)->{
           
                    createChart.run();
              
              

            };
     
            
            chartRange.bottomVvalueProperty().addListener((obs,oldval,newval)->{
                if(chartRange.settingRangeProperty().get()){
                    updateChart.run();
                    
                }
            });

            chartRange.topVvalueProperty().addListener((obs,oldval,newval)->{
                if(chartRange.settingRangeProperty().get()){
                    updateChart.run();
                }
            });

        

            SimpleObjectProperty<ControlInterface> currentControl = new SimpleObjectProperty<>(null);

            currentControl.addListener((obs,oldval,newval)->{
                if(oldval != null){
                    oldval.cancel();
                }
                if(newval != null){
                    menuAreaBox.getChildren().add(newval.getControlBox());
                }else{
                    menuAreaBox.getChildren().clear();
                }

            });
    

            chartRange.settingRangeProperty().addListener((obs,oldval,newval)->{
                if(newval){
                    currentControl.set(chartRange);
                }else{
                    if(currentControl.get() != null && currentControl.get().equals(chartRange)){
                        currentControl.set(null);
                    }
                }
                createChart.run();
            });
    
            timeSpanObject.addListener((obs,oldval,newval)->createChart.run());

            
        String friendlyId = FriendlyId.createFriendlyId();

 

     
       

        FxTimer.runLater(Duration.ofMillis(150),()->{
            SpectrumChartView chartView = m_marketData.getSpectrumChartView().get();
            
            if(chartView != null && m_chartMsgInterface == null){
              
                m_chartMsgInterface = new NoteMsgInterface() {
                    public String getId() {
                        return friendlyId;
                    }

                    public void sendMessage(int code, long timestamp,String networkId, Number num){
                     
                        switch(code){
                            case App.STARTED:
                            case App.LIST_UPDATED:
                            case App.LIST_CHECKED:
                            case App.LIST_CHANGED:
                                createChart.run();  
                         
                            break;
                            case App.STOPPED:
    
                            break;
                        }  
                        
                    }

                    public void sendMessage(int code, long timestamp, String networkId, String msg){
                       
                    }
        
                  
                };
                
                
                chartView.addMsgListener(m_chartMsgInterface);
                chartScroll.viewportBoundsProperty().addListener(boundsChangeListener);
            }
            
            createChart.run();
        });

            
            


            ResizeHelper.addResizeListener(m_stage, 200, 200, Double.MAX_VALUE, Double.MAX_VALUE);
            m_stage.show();

    
   
            Runnable resetPosition = () ->{
            
                if(m_prevX != -1){
                    m_stage.setY(m_prevY);
                    m_stage.setX(m_prevX);
                    m_stage.setWidth(m_prevWidth);
                    m_stage.setHeight(m_prevHeight);
                    m_prevX = -1;
                    m_prevY = -1;
                    m_prevWidth = -1;
                    m_prevHeight = -1;
                    m_positionIndex = 0;
                }
               
                
            };

            maximizeBtn.setOnAction((e) -> {
               
                if(m_positionIndex == 0){
                    
                    m_prevHeight = m_stage.getScene().getHeight();
                    m_prevWidth = m_stage.getScene().getWidth();
                    m_prevX = m_stage.getX();
                    m_prevY = m_stage.getY();
                    m_stage.setMaximized(true);
                    setChartScrollRight.run();
                    m_positionIndex = 1;
                }else{
                    if(m_positionIndex == 1){
                        m_stage.setMaximized(false);
                        resetPosition.run();
                        setChartScrollRight.run();
                    }else{
                        resetPosition.run();
                        FxTimer.runLater(Duration.ofMillis(100), ()->{
                            m_stage.setMaximized(true);
                            setChartScrollRight.run();
                            m_positionIndex = 1;
                        });
                        
                    }
                     
                }
               
            });

            Runnable fillRight = () ->{
                Stage exStage = exchange.getAppStage();
                m_prevHeight = m_stage.getScene().getHeight();
                m_prevWidth = m_stage.getScene().getWidth();
                m_prevX = m_stage.getX();
                m_prevY = m_stage.getY();
                m_positionIndex = 2;
                m_stage.setMaximized(true);

                FxTimer.runLater(Duration.ofMillis(100), ()->{
                    double topY = m_stage.getY();
                    double leftX = m_stage.getX();
                    double width = m_stage.getScene().getWidth();
                    double height = m_stage.getScene().getHeight();
                    m_stage.setMaximized(false);
                    
                    FxTimer.runLater(Duration.ofMillis(100), ()->{
                        
                        m_stage.setWidth(width - (exStage != null ?  exStage.getWidth() : 0));
                        m_stage.setHeight(height);
                        m_stage.setY(topY);
                        m_stage.setX(leftX + (exStage != null ? exStage.getWidth() : 0));
                        
                        if(exStage != null){
                            exStage.setX(leftX);
                            exStage.setY(topY);
                            exStage.setHeight(height);
                        }                        
                        setChartScrollRight.run();
                        
                    });
    
                
                });
            };

            fillRightBtn.setOnAction(e -> {
                
                if(m_positionIndex == 2){
                    resetPosition.run();
                }else{
                    if(m_positionIndex == 1){
                        m_stage.setMaximized(false);
                        FxTimer.runLater(Duration.ofMillis(100), ()->{
                            resetPosition.run();
                            fillRight.run();
                        });
                    }else{
                        resetPosition.run();
                        fillRight.run();
                    }
                }
            });

     

            m_stage.setOnCloseRequest(e ->{
                shutdownSwap.set(true);
                if(m_stage != null){
                    m_stage.close();
                    m_stage = null;
                  
                }
                if(m_g2d != null){
                    m_g2d.dispose();
                }
                m_g2d = null;
                m_img = null;
            });

     
        
            
          
            String[] spans = TimeSpan.AVAILABLE_TIMESPANS;

            for (int i = 0; i < spans.length; i++) {

                String span = spans[i];
                TimeSpan timeSpan = new TimeSpan(span);
                MenuItem menuItm = new MenuItem(timeSpan.getName());
                menuItm.setId("urlMenuItem");
                menuItm.setUserData(timeSpan);

                menuItm.setOnAction(action -> {
                  
      

                    Object item = menuItm.getUserData();

                    if (item != null && item instanceof TimeSpan) {

                        timeSpanObject.set((TimeSpan)item);
               
                        
                    }

                });

                timeSpanBtn.getItems().add(menuItm);

            }

            timeSpanBtn.textProperty().addListener((obs, oldVal, newVal) -> {
                Object objData = timeSpanBtn.getUserData();

                if (newVal != null && !newVal.equals(timeSpanObject.get().getName()) && objData != null && objData instanceof TimeSpan) {

                    timeSpanObject.set((TimeSpan) objData);
                  
                    setChartScrollRight.run();
                   // chartView.reset();
                   // setCandles.run();
      
                }
            });

            ChangeListener<Boolean> invertListener = (obs,oldval,newval)->{
               
                invertBtn.setImage( new Image(newval ? "/assets/targetSwapped.png" : "/assets/targetStandard.png"));
                m_stage.setTitle(exchange.getName() + " - " +  m_marketData.getCurrentSymbol(newval) + " - " + (newval ? m_marketData.getInvertedLastPrice() + "" : m_marketData.getLastPrice() + "") + ""  );
                headingText.setText(m_marketData.getCurrentSymbol(newval));
                chartRange.reset();
                createChart.run();
            };

            m_isInvert.addListener(invertListener);

            ChangeListener<Number> shutdownListener = (obs,oldval,newval)->{
               
                closeBtn.fire();
            };

            m_shutdown.addListener(shutdownListener);

            closeBtn.setOnAction(e -> {
                shutdownSwap.set(true);
                m_shutdown.removeListener(shutdownListener);
                m_isInvert.removeListener(invertListener);

                if(m_chartMsgInterface != null){
                    spectrumChartView.removeMsgListener(m_chartMsgInterface);
                }
                if(m_stage != null){
                    m_stage.close();
                    m_stage = null;
                    if(m_g2d != null){
                        m_g2d.dispose();
                    }
                    m_g2d = null;
                    m_img = null;
                }
            });

        } else {
            m_stage.show();
            m_stage.requestFocus();
        }
    }



    public NetworksData getNetworksData(){
        return m_dataList.getSpectrumFinance().getNetworksData();
    }

    public NoteInterface getCurrentNetwork(){
        return  getNetworksData().getNetwork(ErgoNetwork.NETWORK_ID);
    }

    public VBox getSwapBox(Scene scene, SimpleBooleanProperty shutdownSwap){

      

       // SimpleObjectProperty<PriceAmount> spfPriceAmountObject = new SimpleObjectProperty<>(null);
        SimpleObjectProperty<PriceAmount> basePriceAmountObject = new SimpleObjectProperty<>(null); 
        SimpleObjectProperty<PriceAmount> quotePriceAmountObject = new SimpleObjectProperty<>(null);

        SimpleObjectProperty<BigDecimal> orderPriceObject = new SimpleObjectProperty< BigDecimal>();
 
        SimpleBooleanProperty isBuyObject = new SimpleBooleanProperty(true);
     //   SimpleBooleanProperty isSpfFeesObject = new SimpleBooleanProperty(false);
        SimpleObjectProperty<BigDecimal> currentAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
        SimpleObjectProperty<BigDecimal> currentVolume = new SimpleObjectProperty<>(BigDecimal.ZERO);
        
        SimpleStringProperty orderTypeStringObject = new SimpleStringProperty(MARKET_ORDER);

        SimpleBooleanProperty showPoolStats = new SimpleBooleanProperty(true);

        Runnable updateOrderPriceObject = () ->{
            if(orderTypeStringObject.get() != null && orderTypeStringObject.get().equals(MARKET_ORDER)){
                orderPriceObject.set(isInvert() ? m_marketData.getInvertedLastPrice() : m_marketData.getLastPrice());
            }
            
        };

        updateOrderPriceObject.run();


        TextField baseQuantityField = new TextField();
        HBox.setHgrow(baseQuantityField, Priority.ALWAYS);
        baseQuantityField.setId("formField");
        baseQuantityField.setEditable(false);
        baseQuantityField.setAlignment(Pos.CENTER_RIGHT);

        ImageView baseImgView = new ImageView();
        baseImgView.setPreserveRatio(true);



        StackPane baseQuantityFieldBox = new StackPane(baseImgView, baseQuantityField);
        baseQuantityFieldBox.setId("darkBox");
        baseQuantityFieldBox.setPadding(new Insets(0,3,0,0));
        baseQuantityFieldBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(baseQuantityFieldBox, Priority.ALWAYS);

        HBox baseQuantityBox = new HBox(baseQuantityFieldBox);
        baseQuantityBox.setPadding(new Insets(0,5,0,3));
        baseQuantityBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(baseQuantityBox, Priority.ALWAYS);

   
        


        ImageView poolStatsIconView = new ImageView(m_dataList.getSpectrumFinance().getSmallAppIcon());
        poolStatsIconView.setFitWidth(20);
        poolStatsIconView.setPreserveRatio(true);

        Label poolStatsLbl = new Label("Profile");
        poolStatsLbl.setFont(App.titleFont);
        poolStatsLbl.setTextFill(App.txtColor);
        poolStatsLbl.setPadding(new Insets(0, 0, 0, 10));

        Region poolStatsSpacer = new Region();
        HBox.setHgrow(poolStatsSpacer, Priority.ALWAYS);

        
        
        BufferedButton poolStatsToggleShowBtn = new BufferedButton("/assets/caret-up-15.png", 15);
        poolStatsToggleShowBtn.setId("toolBtn");
        poolStatsToggleShowBtn.setPadding(new Insets(0, 5, 0, 3));
        poolStatsToggleShowBtn.setOnAction(e->{
            showPoolStats.set(!showPoolStats.get());
        });
        

        
        HBox poolStatsTopBar = new HBox(poolStatsIconView, poolStatsLbl, poolStatsSpacer, poolStatsToggleShowBtn);
        poolStatsTopBar.setAlignment(Pos.CENTER_LEFT);
        poolStatsTopBar.setPadding(new Insets(5,1, 5, 5));
        poolStatsTopBar.setId("networkTopBar");

        

        VBox poolStatsBox = new VBox();

        VBox poolStatsPaddingBox = new VBox(poolStatsTopBar, poolStatsBox);
        
        
        showPoolStats.addListener((obs,oldval,newval)->{
            
            poolStatsToggleShowBtn .setImage( newval ? new Image("/assets/caret-up-15.png") : new Image("/assets/caret-down-15.png"));   
        
            if(newval){
                
            }else{
               
            }
        });

        Button buyBtn = new Button("Buy");
        buyBtn.setOnAction(e->{
            isBuyObject.set(true);
        });

        Button sellBtn = new Button("Sell");
        sellBtn.setOnAction(e->{
            isBuyObject.set(false);
        });

        Region buySellSpacerRegion = new Region();
        VBox.setVgrow(buySellSpacerRegion, Priority.ALWAYS);

        HBox buySellBox = new HBox(buyBtn, sellBtn);
        HBox.setHgrow(buySellBox,Priority.ALWAYS);

        buyBtn.prefWidthProperty().bind(buySellBox.widthProperty().divide(2));
        sellBtn.prefWidthProperty().bind(buySellBox.widthProperty().divide(2));

 
        
        


        
        Button marketOrderBtn = new Button("Market");
        marketOrderBtn.setId("selectedBtn");

        HBox orderTypeBtnBox = new HBox(marketOrderBtn);
        HBox.setHgrow(orderTypeBtnBox, Priority.ALWAYS);
    
        orderTypeBtnBox.setAlignment(Pos.CENTER_LEFT);

        marketOrderBtn.prefWidthProperty().bind(orderTypeBtnBox.widthProperty().divide(3));

        HBox orderTypeBox = new HBox(orderTypeBtnBox);
        HBox.setHgrow(orderTypeBox, Priority.ALWAYS);
        orderTypeBox.setAlignment(Pos.CENTER_LEFT);
    //    orderTypeBox.setPadding(new Insets(5));

        Text orderPriceText = new Text(String.format("%-9s", "Price"));
        orderPriceText.setFill(App.txtColor);
        orderPriceText.setFont(App.txtFont);

        ImageView orderPriceImageView = new ImageView(PriceCurrency.getBlankBgIcon(38, m_marketData.getCurrentSymbol(isInvert())));
        

        TextField orderPriceTextField = new TextField(orderPriceObject.get() != null ? orderPriceObject.get().toString() : "");
        HBox.setHgrow(orderPriceTextField, Priority.ALWAYS);
        orderPriceTextField.setAlignment(Pos.CENTER_RIGHT);


        TextField orderPriceStatusField = new TextField(m_marketData.getLastUpdated().get() != null ? Utils.formatTimeString(m_marketData.getLastUpdated().get()) : "");
        orderPriceStatusField.setPrefWidth(80);
        orderPriceStatusField.setId("smallSecondaryColor");
        orderPriceStatusField.setPadding(new Insets(1,0,0,0));
        orderPriceStatusField.setAlignment(Pos.BOTTOM_RIGHT);

        HBox orderPriceStatusBox = new HBox(orderPriceStatusField);
        VBox.setVgrow(orderPriceStatusBox, Priority.ALWAYS);
        HBox.setHgrow(orderPriceStatusBox, Priority.ALWAYS);
        orderPriceStatusBox.setAlignment(Pos.BOTTOM_RIGHT);

        StackPane orderPriceStackBox = new StackPane(orderPriceStatusBox, orderPriceImageView, orderPriceTextField);
        HBox.setHgrow(orderPriceStackBox, Priority.ALWAYS);
        orderPriceStackBox.setAlignment(Pos.CENTER_LEFT);
        orderPriceStackBox.setId("darkBox");

        HBox orderPriceBox = new HBox(orderPriceText, orderPriceStackBox);
        HBox.setHgrow(orderPriceBox, Priority.ALWAYS);
        orderPriceBox.setAlignment(Pos.CENTER_LEFT);
        orderPriceBox.setPadding(new Insets(5,0,5,0));

        HBox pricePaddingBox = new HBox(orderPriceBox);
        HBox.setHgrow(pricePaddingBox, Priority.ALWAYS);
        pricePaddingBox.setPadding(new Insets(5));

        Text amountText = new Text(String.format("%-9s", "Amount"));
        amountText.setFill(App.txtColor);
        amountText.setFont(App.txtFont);

        ImageView amountFieldImage = new ImageView();
        amountFieldImage.setPreserveRatio(true);


        TextField amountField = new TextField("0.0");
        HBox.setHgrow(amountField, Priority.ALWAYS);
        amountField.setId("swapBtn");
        amountField.setPadding(new Insets(2,10,2,10));
        amountField.setAlignment(Pos.CENTER_RIGHT);
        
       

        StackPane amountStack = new StackPane(amountFieldImage, amountField);
        HBox.setHgrow(amountStack, Priority.ALWAYS);
        amountStack.setAlignment(Pos.CENTER_LEFT);
        amountStack.setId("darkBox");
        amountStack.setPadding(new Insets(2));
        amountStack.setMinHeight(40);
        Slider amountSlider = new Slider();
        HBox.setHgrow(amountSlider, Priority.ALWAYS);
        amountSlider.setShowTickMarks(true);
        amountSlider.setMinorTickCount(4);
        amountSlider.setSnapToTicks(true);
        amountSlider.setMax(0);

       

        amountSlider.valueProperty().addListener((obs,oldval,newval)->{

            if(newval.doubleValue() == 0){
                amountField.setText("0.0");
            }else{
                amountField.setText(newval.doubleValue() + "");
            }
            
        });
        VBox amountVBox = new VBox( amountStack, amountSlider);
        HBox.setHgrow(amountVBox, Priority.ALWAYS);

        HBox amountPaddingBox = new HBox(amountText, amountVBox);
        amountPaddingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(amountPaddingBox,Priority.ALWAYS);
        amountPaddingBox.setPadding(new Insets(5));

        HBox feeAmountPaddingBox = new HBox();
        HBox.setHgrow(feeAmountPaddingBox,Priority.ALWAYS);

        Text volumeText = new Text(String.format("%-9s", "Volume"));
        volumeText.setFill(App.txtColor);
        volumeText.setFont(App.txtFont);

        ImageView volumeFieldImage = new ImageView();
        volumeFieldImage.setPreserveRatio(true);



        TextField volumeField = new TextField("0.0");
        HBox.setHgrow(volumeField, Priority.ALWAYS);
        volumeField.setId("swapBtnStatic");
        volumeField.setEditable(false);
        volumeField.setPadding(new Insets(2,10,2,10));
        volumeField.setAlignment(Pos.CENTER_RIGHT);
    
        currentVolume.addListener((obs,oldVal, newVal)->{
            if(newVal.equals(BigDecimal.ZERO)){
                volumeField.setText("0.0");
            }else{
                volumeField.setText(String.format("%.6f", newVal));
            }
        });

        Runnable updateVolumeFromAmount = ()->{
            

            BigDecimal bigPrice =  orderPriceObject.get() != null ? orderPriceObject.get() : BigDecimal.ZERO;
            
            BigDecimal bigAmount = currentAmount.get() != null ? currentAmount.get() : BigDecimal.ZERO;

            BigDecimal volume = BigDecimal.ZERO;
            if(!bigAmount.equals(BigDecimal.ZERO)){
                try{
                    volume = isBuyObject.get() ? bigAmount.multiply(bigPrice) : bigAmount.divide(bigPrice, m_marketData.getFractionalPrecision(), RoundingMode.HALF_UP);
                }catch(Exception e){
                    volume = BigDecimal.ZERO;
                }
            }
        
            currentVolume.set(volume);
            PriceAmount amountAvailable = isBuyObject.get() ? basePriceAmountObject.get() : quotePriceAmountObject.get();
            if(amountAvailable != null){
                if(amountAvailable.amountProperty().get().compareTo(bigAmount) == -1){
                    amountField.setId("swapBtnUnavailable");
                    volumeField.setId("swapBtnUnavailable");
                }else{
                    amountField.setId("swapBtnAvailable");
                    volumeField.setId("swapBtnAvailable");
                }
            }else{
                amountField.setId("swapBtn");
                volumeField.setId("swapBtn");
            }
            
            //if(bigAmount.compareTo(newval))
        
        };
        
        currentAmount.addListener((obs,oldval, newval)->updateVolumeFromAmount.run());
        
        
        amountField.textProperty().addListener((obs, oldval, newval)->{
            
            int decimals = m_marketData.getFractionalPrecision();
            String number = newval.replaceAll("[^0-9.]", "");
            int index = number.indexOf(".");
            String leftSide = index != -1 ? number.substring(0, index + 1) : number;
            String rightSide = index != -1 ?  number.substring(index + 1) : "";

            rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
            rightSide = rightSide.length() > decimals ? rightSide.substring(0, decimals) : rightSide;

            String str = leftSide +  rightSide;
            amountField.setText(str);
            
            try{
                if(!Utils.onlyZero(str)){
                    BigDecimal bigAmount = new BigDecimal(str);
                    currentAmount.set(bigAmount);
                    Files.writeString(App.logFile.toPath(), "\ncurrentAmount: " + bigAmount.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }else{
                    currentAmount.set(BigDecimal.ZERO);
                }
            }catch(Exception e){
                currentAmount.set(BigDecimal.ZERO);
             
            }   
   

            
        });



        amountField.focusedProperty().addListener((obs,oldval,newval)->{
            if(!newval){
                String str = amountField.getText();
                if(str.equals(("0.")) && str.equals(("0")) && str.equals("") && str.equals(".")){
                    amountField.setText("0.0");
                }
            }
        });

        StackPane volumeStack = new StackPane(volumeFieldImage, volumeField);
        HBox.setHgrow(volumeStack, Priority.ALWAYS);
        volumeStack.setAlignment(Pos.CENTER_LEFT);
        volumeStack.setId("darkBox");
        //volumeStack.setPadding(new Insets(2));
        volumeStack.setMinHeight(40);

       

        HBox quoteVolumHbox = new HBox(volumeText, volumeStack); 
        HBox.setHgrow(quoteVolumHbox, Priority.ALWAYS);
        quoteVolumHbox.setAlignment(Pos.CENTER_LEFT);
        

        HBox quoteAmountPaddingBox = new HBox(quoteVolumHbox);
        HBox.setHgrow(quoteAmountPaddingBox,Priority.ALWAYS);
        quoteAmountPaddingBox.setPadding(new Insets(5));
       
        HBox isBuyPaddingBox = new HBox(buySellBox);
        HBox.setHgrow(isBuyPaddingBox, Priority.ALWAYS);
        isBuyPaddingBox.setPadding(new Insets(15));

        Button executeBtn = new Button("");
        executeBtn.setOnAction(e->{
            boolean isBuy = isBuyObject.get();
            boolean invert = isBuy ? isInvert() : !isInvert();
           
            String quoteId = invert ?  m_marketData.getBaseId() : m_marketData.getQuoteId();
            String baseId = invert ? m_marketData.getQuoteId() : m_marketData.getBaseId();
            BigDecimal baseAmount = currentAmount.get();

           // executeBtn.setText(isBuy ? "Buy " + quoteSymbol : "Sell " + quoteSymbol);
            //swap();
        });

        HBox executeBox = new HBox(executeBtn);
        HBox.setHgrow(executeBox,Priority.ALWAYS);
        executeBox.setAlignment(Pos.CENTER);
        executeBox.setMinHeight(40);
        

        HBox executePaddingBox = new HBox(executeBox);
        HBox.setHgrow(executePaddingBox, Priority.ALWAYS);
        executePaddingBox.setPadding(new Insets(5));

        VBox marketBox = new VBox(isBuyPaddingBox, pricePaddingBox, amountPaddingBox, quoteAmountPaddingBox, executePaddingBox );
        marketBox.setPadding(new Insets(5));
        marketBox.setId("bodyBox");

     

        VBox marketPaddingBox = new VBox(orderTypeBox, marketBox);

        VBox swapBox = new VBox( marketPaddingBox);
        
        
        VBox.setVgrow(swapBox, Priority.ALWAYS);
        swapBox.setMinWidth(SWAP_BOX_MIN_WIDTH);


        Runnable updateOrderType = () ->{
        
            String orderType = orderTypeStringObject.get() != null ? orderTypeStringObject.get() : "";
            
            switch(orderType){
                
                case LIMIT_ORDER:
                    orderPriceTextField.setId("swapBtn");
                    orderPriceTextField.setEditable(true);
                
                    break;
                case MARKET_ORDER:        
                default:
                    orderPriceTextField.setId("swapBtnStatic");
                    orderPriceTextField.setEditable(false);
                 
                    
            }
        
        };
        

        orderTypeStringObject.addListener((obs,oldVal,newVal)->updateOrderType.run());

        updateOrderType.run();

      

        

        Runnable updateBuySellBtns = ()->{
            boolean isBuy = isBuyObject.get();
            buyBtn.setId(isBuy ? "iconBtnSelected" : "iconBtn");
            sellBtn.setId(isBuy ? "iconBtn" : "iconBtnSelected");
            
            boolean invert = isInvert();
           
            String quoteSymbol = invert ?  m_marketData.getBaseSymbol() : m_marketData.getQuoteSymbol();

            executeBtn.setText(isBuy ? "Buy " + quoteSymbol : "Sell " + quoteSymbol);

            
        };

        updateBuySellBtns.run();

        Text feesText = new Text("Fees");
        feesText.setFont(App.titleFont);
        feesText.setFill(Color.WHITE);

        HBox feesTextBox = new HBox(feesText);
        HBox.setHgrow(feesTextBox,Priority.ALWAYS);
        VBox.setVgrow(feesTextBox,Priority.ALWAYS);
        feesTextBox.setAlignment(Pos.TOP_RIGHT);


      /*  Runnable updateFeesBtn = () ->{
            
            if(isSpfFeesObject.get()){
                //ergoRemoveFees
                if(ergoQuantityFieldBox.getChildren().contains(feesTextBox)){
                    ergoQuantityFieldBox.getChildren().remove(feesTextBox);
                }
                //spfAddFees
                if(!spfQuantityFieldBox.getChildren().contains(feesTextBox)){
                    spfQuantityFieldBox.getChildren().add(feesTextBox);
                }
            }else{
                //spfAddFees
                if(spfQuantityFieldBox.getChildren().contains(feesTextBox)){
                    spfQuantityFieldBox.getChildren().remove(feesTextBox);
                }
                //ergoRemoveFees
                if(!ergoQuantityFieldBox.getChildren().contains(feesTextBox)){
                    ergoQuantityFieldBox.getChildren().add(feesTextBox);
                }
            }
        };

        updateFeesBtn.run();

        ergoQuantityFieldBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e->{
            isSpfFeesObject.set(false);
        });
        spfQuantityFieldBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e->{
            isSpfFeesObject.set(true);
        });*/

   

        isBuyObject.addListener((obs,oldval,newval)->{
            updateBuySellBtns.run();
 
         
        });

        m_isInvert.addListener((obs,oldval, newval)->{
            updateBuySellBtns.run();

            orderPriceImageView.setImage(PriceCurrency.getBlankBgIcon(38, m_marketData.getCurrentSymbol(newval)));
        });

        
        orderPriceObject.addListener((obs,oldval,newval)->{
         
            if(newval != null){
                
                orderPriceTextField.setText(newval.toString());
              
            }else{
                orderPriceTextField.setText("");
            }
        });



     //   isSpfFeesObject.addListener((obs,oldval,newval)->updateFeesBtn.run());
       //AddressData update
            
   

        ChangeListener<LocalDateTime> marketDataUpdateListener = (obs,oldVal,newVal)->{
            String orderType = orderTypeStringObject.get() != null ? orderTypeStringObject.get() : "";
            
            switch(orderType){
                case MARKET_ORDER:
                    updateOrderPriceObject.run();
                    updateVolumeFromAmount.run();
                    orderPriceStatusField.setText(Utils.formatTimeString(newVal));
                break;
            }

            
            
        };
        
        m_marketData.getLastUpdated().addListener(marketDataUpdateListener);

      
       


        Button shutdownMenu = new Button();
        

        

        

        shutdownSwap.addListener((obs,oldval,newval)->{
            if(newval){
                shutdownMenu.fire();
             
                
                m_marketData.getLastUpdated().removeListener(marketDataUpdateListener);
              
            }
        });

        return swapBox;
    }







    public String getSymbol() {
        return m_marketData != null ? m_marketData.getCurrentSymbol(m_isInvert.get()) : "Unknown";
    }

    public String getId(){
        return m_marketData.getId();
    }

    public String getPoolId(){
        return m_marketData.getPoolId();
    }

    public BigDecimal getLiquidityUSD(){
        return m_marketData.getLiquidityUSD();
    }

    public BigDecimal getLastPrice(){
        return m_marketData.getLastPrice();
    }
    public BigDecimal getBaseVolume(){
        return m_marketData.getBaseVolume();
    }
    public BigDecimal getQuoteVolume(){
        return m_marketData.getQuoteVolume();
    }

  

}
