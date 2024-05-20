package com.netnotes;


import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;

import javax.crypto.SecretKey;

import org.reactfx.util.FxTimer;

import com.netnotes.IconButton.IconStyle;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
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
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.control.Alert.AlertType;
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
    private SimpleBooleanProperty m_isFavorite = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty m_showSwap = new SimpleBooleanProperty(false);
    private SimpleLongProperty m_shutdown = new SimpleLongProperty(0);

    private int m_positionIndex = 0;
    private double m_prevWidth = -1;
    private double m_prevHeight = -1;
    private double m_prevX = -1;
    private double m_prevY = -1;


    private SimpleBooleanProperty m_isInvert = new SimpleBooleanProperty();


    public SpectrumMarketItem(boolean favorite, SpectrumMarketData marketData, SpectrumDataList dataList) {
        m_dataList = dataList;
        m_marketData = marketData;
        m_isFavorite.set(favorite);
        

        m_dataList.isInvertProperty().addListener((obs,oldval,newval)->{
            boolean invert = newval;
            
            m_isInvert.set(m_marketData.getDefaultInvert() ? !invert : invert);
        });

        m_isInvert.set(m_marketData.getDefaultInvert() ? ! m_dataList.isInvertProperty().get():  m_dataList.isInvertProperty().get());
    }  
    

    public SimpleBooleanProperty isFavoriteProperty() {
        return m_isFavorite;
    }




    public SpectrumMarketData getMarketData() {
        return m_marketData;
    }

    private BufferedImage m_rowImg = null;

    public HBox getRowBox(SimpleDoubleProperty widthObject, SimpleObjectProperty<TimeSpan> timeSpanObject, SimpleObjectProperty<HBox> currentBox) {

        
        double regularHeight = 32;
        double focusedHeight = 140;
        int  chartWidthOffset = 240;

        SimpleDoubleProperty chartHeightObject = new SimpleDoubleProperty(regularHeight);

        Button favoriteBtn = new Button();
        favoriteBtn.setId("menuBtn");
        favoriteBtn.setGraphic(IconButton.getIconView(new Image(m_isFavorite.get() ? "/assets/star-30.png" : "/assets/star-outline-30.png"), 30));
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
        });
      


        HBox hasChart = new HBox();
        hasChart.setMinWidth(10);
        hasChart.setPrefWidth(10);
        hasChart.setMinHeight(15);
        hasChart.setMaxHeight(15);

        hasChart.setId(m_marketData.getPoolId() != null ? "onlineBtn" : "offlineBtn");
        hasChart.setOnMouseClicked(e->{
            if(m_marketData.getPoolId() == null){
                
            }else{
                open();
            }
        }); 
       

        HBox rowChartBox = new HBox(hasChart);
        rowChartBox.setAlignment(Pos.CENTER_LEFT);
        rowChartBox.setPadding(new Insets(0,5, 0, 0));

       ImageView rowChartImgView = new ImageView();
       rowChartImgView.setPreserveRatio(true);
     
        HBox chartBox = new HBox(rowChartImgView);
        
    
        chartBox.setPadding(new Insets(0));
        chartBox.minWidthProperty().bind(widthObject.subtract(chartWidthOffset));
        
        Text priceText = new Text();
        priceText.setFont(Font.font("OCR A Extended", FontWeight.NORMAL, 14));
        priceText.setFill(App.txtColor);
        

        Text symbolText = new Text(m_marketData.getCurrentSymbol(m_isInvert.get()));
        symbolText.setFont(Font.font("Deja Vu Sans", FontWeight.NORMAL, 14));
        symbolText.setFill(App.txtColor);
      
        DropShadow shadow = new DropShadow();
        symbolText.setEffect(shadow);

        HBox symbolTextBox = new HBox(symbolText);
        symbolTextBox.setMaxHeight(  regularHeight);
        symbolTextBox.setMinHeight(regularHeight);
        symbolTextBox.setAlignment(Pos.CENTER_LEFT);

        HBox symbolTextPaddingBox = new HBox(symbolTextBox);
      
        VBox.setVgrow(symbolTextPaddingBox, Priority.ALWAYS);
        HBox.setHgrow(symbolTextPaddingBox, Priority.ALWAYS);
        symbolTextPaddingBox.setAlignment(Pos.CENTER_LEFT);

  
        SimpleObjectProperty<Image> baseImg = new SimpleObjectProperty<>();
        SimpleObjectProperty<Image> quoteImg = new SimpleObjectProperty<>();

        WritableImage logo = new WritableImage(150,32);
        ImageView logoView = new ImageView(logo);

        HBox imagesBox = new HBox(logoView);
        imagesBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(imagesBox,Priority.ALWAYS);
        VBox.setVgrow(imagesBox,Priority.ALWAYS);

        Runnable updateImages = ()->{
            
            if(m_dataList.tokensListNetwork().get() != null && m_dataList.tokensListNetwork().get() instanceof ErgoTokensList){
               
                ErgoTokensList tokensList = (ErgoTokensList)m_dataList.tokensListNetwork().get();

                String baseTokenId =  m_marketData.getBaseId();
                String baseTokenName = m_marketData.getBaseSymbol();
                int baseDecimals = m_marketData.getBaseDecimals();
                ErgoNetworkToken baseToken = tokensList.getAddErgoToken(baseTokenId, baseTokenName , baseDecimals);

                String quoteTokenId = m_marketData.getQuoteId();
                String quoteTokenName = m_marketData.getQuoteSymbol();
                int quoteDecimals = m_marketData.getQuoteDecimals();
                ErgoNetworkToken quoteToken = tokensList.getAddErgoToken(quoteTokenId, quoteTokenName, quoteDecimals);


                baseImg.set(baseToken != null ? baseToken.getIcon() : null);
                quoteImg.set(quoteToken != null ? quoteToken.getIcon() : null);
          
            }
        };
       
        
        StackPane rowImgBox = new StackPane(chartBox, imagesBox, symbolTextPaddingBox);
        HBox.setHgrow(rowImgBox,Priority.ALWAYS);
        rowImgBox.minWidthProperty().bind(widthObject.subtract(chartWidthOffset));
        rowImgBox.setAlignment(Pos.CENTER_LEFT);
        rowImgBox.setPadding(new Insets(0,30,0,0));

        SimpleObjectProperty<SpectrumNumbers> numbersObject = new SimpleObjectProperty<>(null);
        
        HBox statsBox = new HBox();
        statsBox.setId("transparentColor");
        HBox.setHgrow(statsBox, Priority.ALWAYS);
        VBox.setVgrow(statsBox, Priority.ALWAYS);
        statsBox.setPadding(new Insets(0,10,0,0));

        Text lblPercentChangeText = new Text(String.format("%-6s", ""));
        lblPercentChangeText.setFont(App.txtFont);
        lblPercentChangeText.setFill(Color.web("#777777"));

        Text lblOpenText = new Text(String.format("%-6s", "Open"));
        lblOpenText.setFont(App.txtFont);
        lblOpenText.setFill(Color.web("#777777"));

        Text lblHighText = new Text(String.format("%-6s", "High"));
        lblHighText.setFont(App.txtFont);
        lblHighText.setFill(Color.web("#777777"));

        Text lblLowText = new Text(String.format("%-6s", "Low"));
        lblLowText.setFont(App.txtFont);
        lblLowText.setFill(Color.web("#777777"));

        Text percentChangeText = new Text();

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

        VBox statsVbox = new VBox( highHBox, lowHBox,openHbox, changeHBox);
        VBox.setVgrow(statsVbox, Priority.ALWAYS);
        statsVbox.setId("transparentColor");
        
        numbersObject.addListener((obs,oldval,newval)->{
            if(newval != null){
                if(!statsBox.getChildren().contains(statsVbox)){
                    statsBox.getChildren().add(statsVbox);
                }
                openText.setText(String.format("%-12s",newval.getOpen()+ "").substring(0,12));
                highText.setText(String.format("%-12s",  newval.getHigh()+ "").substring(0,12) );
                lowText.setText(String.format("%-12s",newval.getLow()+ "").substring(0,12) );
                BigDecimal increase = newval.getPercentIncrease();
                increase = increase == null ? BigDecimal.ZERO : increase;

                int increaseDirection = BigDecimal.ZERO.compareTo(increase);
                NumberFormat percentFormat = NumberFormat.getPercentInstance();
                percentFormat.setMaximumFractionDigits(2);
                
                percentChangeText.setText(increaseDirection == 0 ? "" : (increaseDirection == -1 ? "+" :"") + percentFormat.format(increase));
                percentChangeText.setFill(increaseDirection == 0 ? Color.WHITE  : (increaseDirection == -1 ? Color.web("#028A0F") : Color.web("#feb9e9")) );
            }else{
                statsBox.getChildren().clear();
            }
        });

  
        Runnable updateRowImg = () ->{
            SpectrumChartView chartView =  m_marketData.getSpectrumChartView().get();
            if(chartView != null){
                int height = (int) chartHeightObject.get();
                boolean isCurrent = height > (int) regularHeight;
                int w = (int) widthObject.get() - (isCurrent ? chartWidthOffset : (chartWidthOffset-30));
       
                int width = w < chartWidthOffset ? chartWidthOffset : w  ;
                int cellWidth = 3;
                int maxBars = width / cellWidth;
                boolean invert = m_isInvert.get();
                TimeSpan durationSpan = timeSpanObject.get();
                long durationSeconds = durationSpan.getSeconds();
                long colSpanSeconds = (durationSeconds / maxBars);
                TimeSpan colSpan = new TimeSpan("custom","id1", colSpanSeconds);
              
             
                long currentTime =  m_marketData.getTimeStamp();

                long startTimeStamp = currentTime - durationSpan.getMillis();

                
                chartView.processData(invert, startTimeStamp, colSpan, currentTime, m_dataList.getSpectrumFinance().getExecService(), (onSucceeded)->{
                    Object sourceValue = onSucceeded.getSource().getValue();
                    if(sourceValue != null && sourceValue instanceof SpectrumNumbers){
                        SpectrumNumbers numbers =(SpectrumNumbers) sourceValue;
                        
                        numbersObject.set( isCurrent ? numbers : null);
                        
                        boolean isNewImage = (m_rowImg == null) ||  (m_rowImg != null &&(m_rowImg.getWidth() != width || m_rowImg.getHeight() != height));
                        m_rowImg = isNewImage ? new BufferedImage(width , height, BufferedImage.TYPE_INT_ARGB) : m_rowImg; 
                        
                        try {

                            chartView.updateRowChart(numbers, colSpan, cellWidth, m_rowImg);
                            rowChartImgView.setImage(SwingFXUtils.toFXImage(m_rowImg, null));
                            rowChartImgView.setFitWidth(m_rowImg.getWidth());

                        } catch ( ArithmeticException e1) {
                        
                        }
                    }

                }, (onFailed)->{

                });
               


            }
        };

        updateRowImg.run();
        ChangeListener<Number> widthListener = (obs,oldval,newval) ->{
            updateRowImg.run();
        };
        ChangeListener<Number> heightListener = (obs,oldval,newval) ->{
            updateRowImg.run();
        };
        chartHeightObject.addListener(heightListener);
        widthObject.addListener(widthListener);

        ChangeListener<TimeSpan> timeSpanListener = (obs,oldval,newval) ->{
            updateRowImg.run();
        };

        timeSpanObject.addListener(timeSpanListener);
       // cellWidth.addListener((obs,oldval,newval)->updateRowImg.run());
      //  widthProperty.addListener((obs,oldval,newval)->updateRowImg.run());
       // heightProperty.addListener((obs,oldval,newval)->updateRowImg.run());

        SimpleObjectProperty<ChangeListener<Number>> listListenerObject = new SimpleObjectProperty<>(null);

        Runnable addListListener = ()->{
            SpectrumChartView chartView = m_marketData.getSpectrumChartView().get();
            if(chartView != null && listListenerObject.get() == null){
                ChangeListener<Number> dataListListener = (obs,oldval,newval)->updateRowImg.run();
                chartView.dataListChangedProperty().addListener(dataListListener);
                listListenerObject.set(dataListListener);
            }
        };

        addListListener.run();

        ChangeListener<SpectrumChartView> chartViewChangeListener = (obs,oldval,newval)->{
            if(oldval != null && listListenerObject.get() != null){
                oldval.dataListChangedProperty().removeListener(listListenerObject.get());
                listListenerObject.set(null);
            }
            updateRowImg.run();
            
            if(newval != null){
                addListListener.run();
            }
            
        };

        m_marketData.getSpectrumChartView().addListener(chartViewChangeListener);

        HBox priceHBox = new HBox(priceText);
        HBox.setHgrow(priceHBox, Priority.ALWAYS);
        priceHBox.setAlignment(Pos.BOTTOM_RIGHT);
        priceHBox.setPadding(new Insets(0,20,5,0));
        priceHBox.setMinHeight(32);

        VBox priceVBox = new VBox(priceHBox, statsBox);
        HBox.setHgrow(priceVBox,Priority.ALWAYS);
        priceVBox.setAlignment(Pos.CENTER_RIGHT);
        

        HBox rowBox = new HBox(favoriteBtn, rowImgBox,  priceVBox, rowChartBox );
        rowBox.setId("rowBox");
        rowBox.setAlignment(Pos.TOP_LEFT);
        rowBox.maxWidthProperty().bind(widthObject);
        rowBox.setFocusTraversable(true);
        rowBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e->{
            
            currentBox.set(rowBox);
            
            if(e.getClickCount() == 2){
                open();
            }
            
        });

        rowBox.setAlignment(Pos.CENTER_LEFT);
     


        
        Runnable update = ()->{
   
            boolean isChart = m_marketData.getPoolId() != null;
            hasChart.setId(m_marketData != null ? (isChart ? "availableBtn" : "offlineBtn") : "offlineBtn");
            

            priceText.setText( m_isInvert.get() ?String.format("%-12s", m_marketData.getInvertedLastPrice()+ "").substring(0,12) : String.format("%-12s", m_marketData.getLastPrice()+ "").substring(0,12) );
            symbolText.setText(m_marketData.getCurrentSymbol(m_isInvert.get()));

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
            symbolTextPaddingBox.setAlignment(isCurrent ? Pos.TOP_LEFT: Pos.CENTER_LEFT);
            imagesBox.setAlignment(isCurrent ? Pos.TOP_LEFT : Pos.CENTER);
      
        };

  
        currentBox.addListener(currentBoxChangeListener);
        

        ChangeListener<LocalDateTime> marketDataUpdated = (obs, oldVal, newVal) -> update.run();
        ChangeListener<Boolean> invertChanged = (obs,oldval,newval)->{
            update.run();
            updateLogo.run();
            updateRowImg.run();
        };
        ChangeListener<Network> dataListNetworklistener = (obs,oldval,newval)->updateImages.run();
        m_isInvert.addListener(invertChanged);

        m_marketData.getLastUpdated().addListener(marketDataUpdated);

        update.run();

        updateImages.run();
        
        m_dataList.tokensListNetwork().addListener(dataListNetworklistener);
        
        Runnable removeListListener = () ->{
            SpectrumChartView chartView = m_marketData.getSpectrumChartView().get();
            if(chartView != null && listListenerObject.get() != null){
                
                chartView.dataListChangedProperty().removeListener(listListenerObject.get());
                listListenerObject.set(null);
            }
        };

        
        m_shutdown.addListener((obs,oldval,newval)->{

            if(m_marketData.getSpectrumChartView().get() != null){ 
                m_marketData.getSpectrumChartView().addListener(chartViewChangeListener);
            }
            widthObject.removeListener(widthListener);
            timeSpanObject.removeListener(timeSpanListener);
            m_isInvert.removeListener(invertChanged);
            m_marketData.getLastUpdated().removeListener(marketDataUpdated);
            m_dataList.tokensListNetwork().removeListener(dataListNetworklistener);
            currentBox.removeListener(currentBoxChangeListener);
            removeListListener.run();
        
        });
        return rowBox;
    }

    public void init(String id){
        if(m_marketData.isPool()){
            boolean isSet = m_marketData.getSpectrumChartView().get() == null;            
            SpectrumChartView chartView = isSet ? new SpectrumChartView(m_marketData, m_dataList.getSpectrumFinance()) : m_marketData.getSpectrumChartView().get();

            if(isSet){
                m_marketData.getSpectrumChartView().set(chartView);
            }
            chartView.addDataListener(id);
            
        }
        
    }

    public void sendMessage(int msg){
        switch(msg){
            case SpectrumFinance.STOPPED:
            //SpectrumFinance Stopped
            break;
        }
    }

 

    public void shutdown(String id){
        m_shutdown.set(System.currentTimeMillis());
        SpectrumChartView chartView =  m_marketData.getSpectrumChartView().get();
        if(m_marketData.isPool() && m_marketData.getSpectrumChartView().get() != null){
             
            chartView.removeListener(id);
        }
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
    private SpectrumNumbers m_numbers = null;

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
            m_stage.getIcons().add(SpectrumFinance.getSmallAppIcon());
            m_stage.initStyle(StageStyle.UNDECORATED);
            m_stage.setTitle(exchange.getName() + " - " + m_marketData.getCurrentSymbol(isInvert()) + (m_marketData != null ? " - " +(isInvert() ? m_marketData.getInvertedLastPrice().toString() : m_marketData.getLastPrice()) + "" : ""));

            Button maximizeBtn = new Button();
            Button closeBtn = new Button();
            Button fillRightBtn = new Button();

            HBox titleBox = App.createTopBar(SpectrumFinance.getSmallAppIcon(), fillRightBtn, maximizeBtn, closeBtn, m_stage);

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

            Button favoriteBtn = new Button();
            favoriteBtn.setId("menuBtn");
            favoriteBtn.setContentDisplay(ContentDisplay.LEFT);
            favoriteBtn.setGraphic(IconButton.getIconView(new Image(m_isFavorite.get() ? "/assets/star-30.png" : "/assets/star-outline-30.png"), 30));
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
            });

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

  

            HBox headingBox = new HBox(favoriteBtn, headingSpacerL, headingText, timeSpanBtn, headingBoxSpacerR);
            headingBox.prefHeight(40);
            headingBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headingBox, Priority.ALWAYS);
            headingBox.setPadding(new Insets(5, 5, 5, 5));
            headingBox.setId("headingBox");
            
   

            RangeBar chartRange = new RangeBar(rangeWidth, rangeHeight, getNetworksData().getExecService());
            ImageView chartImageView = new ImageView();
            
            ScrollPane chartScroll = new ScrollPane(chartImageView);
            
            headingSpacerL.prefWidthProperty().bind(headingBox.widthProperty().subtract(timeSpanBtn.widthProperty().divide(2)).subtract(favoriteBtn.widthProperty()).subtract(headingText.layoutBoundsProperty().get().getWidth()).divide(2));
         

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
       
            Runnable setChartScrollRight = () ->{
                Platform.runLater(()->chartScroll.setVvalue(chartScrollVvalue));
                Platform.runLater(()->chartScroll.setHvalue(chartScrollHvalue));
            };

            Runnable createChart = () ->{
                spectrumChartView.processData(
                    m_isInvert.get(), 
                    SpectrumChartView.MAX_BARS, 
                    timeSpanObject.get(),
                    Utils.getNowEpochMillis(m_marketData.getLastUpdated().get() == null ? LocalDateTime.now() : m_marketData.getLastUpdated().get()),
                     m_dataList.getSpectrumFinance().getExecService(), 
                     (onSucceeded)->{
                        Object sourceValue = onSucceeded.getSource().getValue();
                        if(sourceValue != null && sourceValue instanceof SpectrumNumbers){
                            SpectrumNumbers numbers = (SpectrumNumbers) sourceValue;
                            int size = numbers.dataLength() > SpectrumChartView.MAX_BARS ? SpectrumChartView.MAX_BARS : numbers.dataLength();
                            if(size > 0){
                           
                                int totalCellWidth = cellWidth + cellPadding;
                                
                                int itemsTotalCellWidth = size * (totalCellWidth + cellPadding);
                         
                                int scaleColWidth =  labelMetrics.stringWidth(numbers.getClose() +"")+ 30;
                
                                int width = (itemsTotalCellWidth + scaleColWidth) < 300 ? 300 : itemsTotalCellWidth + scaleColWidth;
                                int height = (int) chartScroll.viewportBoundsProperty().get().getHeight();
                                
                                
                                boolean isNewImg = m_img == null || (m_img != null && (m_img.getWidth() != width || m_img.getHeight() != height));
                            
                                m_img = isNewImg ? new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB) : m_img;
                                m_g2d = isNewImg ? m_img.createGraphics() : m_g2d;
                                boolean settingRange = chartRange.settingRangeProperty().get();
                                m_numbers = numbers;
                                double botVvalue = chartRange.bottomVvalueProperty().get(); 
                                double topVvalue = chartRange.topVvalueProperty().get();
                                boolean active = chartRange.activeProperty().get();
                                TimeSpan timeSpan = timeSpanObject.get();
                                double[] topBotRange = SpectrumChartView.updateBufferedImage(
                                    m_img, 
                                    m_g2d, 
                                    labelFont, 
                                    labelMetrics, 
                                    numbers,
                                    chartRange.getTopBotRange(), 
                                    cellWidth, 
                                    cellPadding, 
                                    scaleColWidth, 
                                    amStringWidth,
                                    timeSpan, 
                                    botVvalue,
                                    topVvalue,
                                    active,
                                    settingRange
                                );
                                if(settingRange){
                                    chartRange.setTopBotRange(topBotRange == null ? new double[]{0,0} : topBotRange);
                                }

                                chartImageView.setImage(SwingFXUtils.toFXImage(m_img, null));
                                if(isNewImg){
                                    setChartScrollRight.run();
                                }
                            }
                        }
                        
                     }, 
                     (onFailed)->{

                     });
               // spectrumChartView.updateChartImage(cellWidth, cellPadding, priceData, numbersObject, chartRange, chartWidth, chartHeight, timeSpanObject.get(), m_isInvert.get());
            };
            
            Runnable updateChart = () ->{
                if(m_numbers != null){
                    SpectrumNumbers numbers = m_numbers;
                    int size = numbers.dataLength() > SpectrumChartView.MAX_BARS ? SpectrumChartView.MAX_BARS : numbers.dataLength();
                    if(size > 0){
                   
                        int totalCellWidth = cellWidth + cellPadding;
                        
                        int itemsTotalCellWidth = size * (totalCellWidth + cellPadding);
                 
                        int scaleColWidth =  labelMetrics.stringWidth(numbers.getClose() +"")+ 30;
        
                        int width = (itemsTotalCellWidth + scaleColWidth) < 300 ? 300 : itemsTotalCellWidth + scaleColWidth;
                        int height = (int) chartScroll.viewportBoundsProperty().get().getHeight();
                        height = height < 300 ? 300 : height;
                        
                        boolean isNewImg = m_img == null || (m_img != null && (m_img.getWidth() != width || m_img.getHeight() != height));
                    
                        m_img = isNewImg ? new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB) : m_img;
                        m_g2d = isNewImg ? m_img.createGraphics() : m_g2d;
                        boolean settingRange = chartRange.settingRangeProperty().get();
                        double botVvalue = chartRange.bottomVvalueProperty().get(); 
                        double topVvalue = chartRange.topVvalueProperty().get();
                        boolean active = chartRange.activeProperty().get();
                        TimeSpan timeSpan = timeSpanObject.get();

                        double[] topBotRange = SpectrumChartView.updateBufferedImage(
                            m_img, 
                            m_g2d, 
                            labelFont, 
                            labelMetrics, 
                            numbers,
                            chartRange.getTopBotRange(), 
                            cellWidth, 
                            cellPadding, 
                            scaleColWidth, 
                            amStringWidth,
                            timeSpan, 
                            botVvalue, 
                            topVvalue, 
                            active , 
                            settingRange
                        );
                        if(settingRange){
                            chartRange.setTopBotRange(topBotRange == null ? new double[]{0,0} : topBotRange);
                        }

                        chartImageView.setImage(SwingFXUtils.toFXImage(m_img, null));
                        if(isNewImg){
                            setChartScrollRight.run();
                        }
                    }
                }
            };

            
            ChangeListener<Bounds> boundsChangeListener = (obs,oldval,newval)->{
           
                    updateChart.run();
              
              

            };
            ChangeListener<Number> dataChangeListener = (obs,oldval,newval)->{
                createChart.run();
            };
            
            chartRange.bottomVvalueProperty().addListener((obs,oldval,newval)->{
                updateChart.run();
            });

            chartRange.topVvalueProperty().addListener((obs,oldval,newval)->{
                updateChart.run();
            });

            chartRange.activeProperty().addListener((obs,oldval,newval)->{
                updateChart.run();
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
                updateChart.run();
            });
    

            Runnable addListeners = () ->{
                spectrumChartView.dataListChangedProperty().addListener(dataChangeListener);
                chartScroll.viewportBoundsProperty().addListener(boundsChangeListener);
       
            };
            createChart.run();

            FxTimer.runLater(Duration.ofMillis(200),()->{
                
               
                addListeners.run();
            });

            
            


            ResizeHelper.addResizeListener(m_stage, 200, 200, Double.MAX_VALUE, Double.MAX_VALUE);
            m_stage.show();

            /*EventHandler<MouseEvent> toggleHeader = (mouseEvent) -> {
                double mouseY = mouseEvent.getY();
                if (mouseY < (titleBox.getLayoutBounds().getHeight() + 10)) {
                    if (!headerVBox.getChildren().contains(paddingBox)) {
                        headerVBox.getChildren().addAll( paddingBox);
                       // chartHeightOffset.set(chartHeightOffset.get() == headerVBox.getHeight() ? 0 : chartHeightOffset.get());
                    }
                } else {
                    if (headerVBox.getChildren().contains(titleBox)) {
                        if (mouseY > headerVBox.heightProperty().get()) {
                            headerVBox.getChildren().remove(paddingBox);
                         //   chartHeightOffset.set(chartHeightOffset.get() == 0 ? headerVBox.getHeight() : chartHeightOffset.get());
                        }
                    }
                }
            };*/
           
   
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
                    m_g2d.dispose();
                    m_g2d = null;
                    m_img = null;
                }
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
                spectrumChartView.dataListChangedProperty().removeListener(dataChangeListener);

                if(m_stage != null){
                    m_stage.close();
                    m_stage = null;
                    m_g2d.dispose();
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

    public ErgoNetwork getErgoNetwork(){
        return (ErgoNetwork) getNetworksData().getNoteInterface(ErgoNetwork.NETWORK_ID);
    }

    public VBox getSwapBox(Scene scene, SimpleBooleanProperty shutdownSwap){

        ErgoNetwork ergoNetwork = getErgoNetwork();
        ErgoWallets ergoWallets = ergoNetwork != null ? (ErgoWallets) ergoNetwork.getNetwork(ErgoWallets.NETWORK_ID) : null;
        ErgoNodes ergoNodes = ergoNetwork != null ? (ErgoNodes) ergoNetwork.getNetwork(ErgoNodes.NETWORK_ID) : null;

        SimpleObjectProperty<ErgoWalletDataList> walletsListObject = new SimpleObjectProperty<>();
        SimpleObjectProperty<ErgoWalletData> ergoWalletObject = new SimpleObjectProperty<>(null);
        SimpleObjectProperty<AddressesData> addressesDataObject = new SimpleObjectProperty<>(null);

        SimpleObjectProperty<ErgoAmount> ergoPriceAmountObject = new SimpleObjectProperty<ErgoAmount>(null);
        SimpleObjectProperty<PriceAmount> spfPriceAmountObject = new SimpleObjectProperty<>(null);
        SimpleObjectProperty<PriceAmount> basePriceAmountObject = new SimpleObjectProperty<>(null); 
        SimpleObjectProperty<PriceAmount> quotePriceAmountObject = new SimpleObjectProperty<>(null);

        SimpleObjectProperty<BigDecimal> orderPriceObject = new SimpleObjectProperty< BigDecimal>();
        
        SimpleBooleanProperty nodeAvailableObject = new SimpleBooleanProperty(false);
        SimpleStringProperty nodeStatusObject = new SimpleStringProperty(null);
        SimpleBooleanProperty isBuyObject = new SimpleBooleanProperty(true);
        SimpleBooleanProperty isSpfFeesObject = new SimpleBooleanProperty(false);
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
        MenuButton walletBtn = new MenuButton();

        

        if(ergoNetwork != null){
            if(ergoWallets != null && ergoNodes != null){
                ErgoWalletDataList ergoWalletDataList = new ErgoWalletDataList(160,IconStyle.ROW,ergoWallets.getDataFile(), ergoWallets);
                walletsListObject.set(ergoWalletDataList);
                ergoWalletDataList.getMenu(walletBtn, ergoWalletObject);
                
                if(ergoWalletObject.get() == null){
                    walletBtn.show();
                }

            }else{
                String missingString = (ergoNodes == null ? " 'Ergo Nodes" : "" +  (ergoWallets == null ? (ergoNodes == null ? "," : "") + " 'Ergo wallets'" : ""));

                Alert a = new Alert(AlertType.NONE, "Please install" + missingString + " in 'Ergo Network' to use this feature.", ButtonType.OK);
                a.setHeaderText("Required: " + missingString);
                a.setTitle("Required: " + missingString);
                a.showAndWait();
                return null;
            }
        }else{
            
            Alert a = new Alert(AlertType.NONE, "Please install 'Ergo Network' to use this feature.", ButtonType.OK);
            a.setHeaderText("Required: Ergo Network");
            a.setTitle("Required: Ergo Network");
            a.showAndWait();

            return null;
        }

    

        HBox nodeAvailableBox = new HBox();
        nodeAvailableBox.setMaxHeight(15);
        nodeAvailableBox.setMinWidth(10);
        nodeAvailableBox.setId("offlineBtn");
        nodeAvailableBox.setFocusTraversable(true);
        nodeAvailableBox.setOnMouseClicked(e->{
            ergoNodes.open();
        });

        Binding<String> nodeAvailableStringBinding = Bindings.createObjectBinding(()->(nodeAvailableObject.get()  ? "Available" : "Unavailable"), nodeAvailableObject);

        Tooltip nodeAvailableTooltip = new Tooltip("Offline");
        nodeAvailableTooltip.setShowDelay(new javafx.util.Duration(100));

        Binding<String> nodeStatusStringBinding = Bindings.createObjectBinding( ()->nodeStatusObject.get() == null ? "Offline" : nodeStatusObject.get(),  nodeStatusObject);

        nodeAvailableTooltip.textProperty().bind(Bindings.concat(nodeAvailableStringBinding, " - ", nodeStatusStringBinding));
        Tooltip.install(nodeAvailableBox, nodeAvailableTooltip);
        

        nodeAvailableObject.addListener((obs,oldVal,newVal)->{
            nodeAvailableBox.setId(newVal ? "onlineBtn" : "offlineBtn");
        });


        

        final String walletBtnDefaultString = "[Select]           ";


        
        SimpleBooleanProperty showWallet = new SimpleBooleanProperty(true);
        
        walletBtn.setMaxHeight(40);
        walletBtn.setContentDisplay(ContentDisplay.LEFT);
        walletBtn.setAlignment(Pos.CENTER_LEFT);
        walletBtn.setText(walletBtnDefaultString);
        walletBtn.setMinWidth(90);
        walletBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, e->{
            walletBtn.show();
        });
        Text walletText = new Text(String.format("%-9s","Wallet"));
        walletText.setFont(App.txtFont);
        walletText.setFill(App.txtColor);

        HBox walletBtnBox = new HBox(walletBtn);
        HBox.setHgrow(walletBtnBox, Priority.ALWAYS);
        walletBtnBox.setId("darkBox");
        walletBtnBox.setAlignment(Pos.CENTER_LEFT);

        walletBtn.prefWidthProperty().bind(walletBtnBox.widthProperty());

        HBox walletBox = new HBox(walletText, walletBtnBox);
        walletBox.setPadding(new Insets(3,3,3,5));
        HBox.setHgrow(walletBox, Priority.ALWAYS);
        walletBox.setAlignment(Pos.CENTER_LEFT);

        
        final String nodeBtnDefaultString = "[select]";
        MenuButton nodeBtn = new MenuButton(nodeBtnDefaultString);
        nodeBtn.setMaxHeight(40);
        nodeBtn.setContentDisplay(ContentDisplay.LEFT);
        nodeBtn.setAlignment(Pos.CENTER_LEFT);
        nodeBtn.setText(nodeBtnDefaultString);
        nodeBtn.setMinWidth(90);
        nodeBtn.setTooltip(nodeAvailableTooltip);
        nodeBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, e->{
            nodeBtn.show();
        });

        Text nodeText = new Text("Node ");
        nodeText.setFont(App.txtFont);
        nodeText.setFill(App.txtColor);

     

        HBox nodeBtnBox = new HBox(nodeBtn, nodeAvailableBox);
        HBox.setHgrow(nodeBtnBox, Priority.ALWAYS);
        nodeBtnBox.setId("darkBox");
        nodeBtnBox.setAlignment(Pos.CENTER_LEFT);
        nodeBtnBox.setPadding(new Insets(0,3,0,0));

        nodeBtn.prefWidthProperty().bind(nodeBtnBox.widthProperty());

        HBox nodeBox = new HBox(nodeText, nodeBtnBox);
        nodeBox.setPadding(new Insets(3,3,3,5));
        HBox.setHgrow(nodeBox, Priority.ALWAYS);
        nodeBox.setAlignment(Pos.CENTER_LEFT);

        final String adrBtnDefaultString = "[none]";
        MenuButton adrBtn = new MenuButton();
        adrBtn.setContentDisplay(ContentDisplay.LEFT);
        adrBtn.setAlignment(Pos.CENTER_LEFT);
        adrBtn.setText(adrBtnDefaultString);
        adrBtn.setMinWidth(100);
        adrBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, e->{
            adrBtn.show();
        });

        Text addressText = new Text(String.format("%-9s", "Address"));
        addressText.setFont(App.txtFont);
        addressText.setFill(App.txtColor);

        HBox adrBtndarkBox = new HBox(adrBtn);
        adrBtndarkBox.setId("darkBox");
        HBox.setHgrow(adrBtndarkBox, Priority.ALWAYS);

        adrBtn.prefWidthProperty().bind(adrBtndarkBox.widthProperty());

        HBox adrBtnBox = new HBox(addressText, adrBtndarkBox);
        adrBtnBox.setPadding(new Insets(3,3,3,5));
        adrBtnBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(adrBtnBox, Priority.ALWAYS);


      

        ///   

        TextField ergoQuantityField = new TextField();
        HBox.setHgrow(ergoQuantityField, Priority.ALWAYS);
        ergoQuantityField.setId("formField");
        ergoQuantityField.setEditable(false);
        ergoQuantityField.setAlignment(Pos.CENTER_RIGHT);

        ImageView ergoImgView = new ImageView();
        ergoImgView.setPreserveRatio(true);
        

        StackPane ergoQuantityFieldBox = new StackPane(ergoImgView, ergoQuantityField);
        ergoQuantityFieldBox.setPadding(new Insets(0,3,0,0));
        ergoQuantityFieldBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(ergoQuantityFieldBox, Priority.ALWAYS);
        ergoQuantityFieldBox.setId("darkBox");


        HBox ergoQuantityBox = new HBox(ergoQuantityFieldBox);
        ergoQuantityBox.setPadding(new Insets(0,0,0,0));
        ergoQuantityBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(ergoQuantityBox, Priority.ALWAYS);

        ///

        TextField spfQuantityField = new TextField();
        HBox.setHgrow(spfQuantityField, Priority.ALWAYS);
        spfQuantityField.setId("formField");
        spfQuantityField.setEditable(false);
        spfQuantityField.setAlignment(Pos.CENTER_RIGHT);

        ImageView spfImgView = new ImageView();
        spfImgView.setPreserveRatio(true);
        

        StackPane spfQuantityFieldBox = new StackPane(spfImgView, spfQuantityField);
        HBox.setHgrow(spfQuantityFieldBox, Priority.ALWAYS);
        spfQuantityFieldBox.setPadding(new Insets(0,3,0,0));
        spfQuantityFieldBox.setAlignment(Pos.CENTER_LEFT);
        spfQuantityFieldBox.setId("darkBox");

        HBox spfQuantityBox = new HBox(spfQuantityFieldBox);
        spfQuantityBox.setPadding(new Insets(0,5,0,5));
        spfQuantityBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(spfQuantityBox, Priority.ALWAYS);


        TextField quoteQuantityField = new TextField();
        HBox.setHgrow(quoteQuantityField, Priority.ALWAYS);
        quoteQuantityField.setId("formField");
        quoteQuantityField.setEditable(false);
        quoteQuantityField.setAlignment(Pos.CENTER_RIGHT);

        ImageView quoteImgView = new ImageView();
        quoteImgView.setPreserveRatio(true);
        


        StackPane quoteQuantityFieldBox = new StackPane(quoteImgView, quoteQuantityField);
        quoteQuantityFieldBox.setPadding(new Insets(0,3,0,0));
        quoteQuantityFieldBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(quoteQuantityFieldBox, Priority.ALWAYS);
        quoteQuantityFieldBox.setId("darkBox");

        HBox quoteQuantityBox = new HBox( quoteQuantityFieldBox);
        quoteQuantityBox.setPadding(new Insets(0,5,0,3));
        quoteQuantityBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(quoteQuantityBox, Priority.ALWAYS);

        ///

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

        Region addressBoxSeperator = new Region();
        HBox.setHgrow(addressBoxSeperator, Priority.ALWAYS);
        addressBoxSeperator.setMinHeight(2);
        addressBoxSeperator.setId("hGradient");
        

        HBox addressBoxSepBox = new HBox(addressBoxSeperator);
        HBox.setHgrow(addressBoxSepBox, Priority.ALWAYS);
        addressBoxSepBox.setPadding(new Insets(1,0,5,0));

        Region botAddressBoxSeperator = new Region();
        HBox.setHgrow(botAddressBoxSeperator, Priority.ALWAYS);
        botAddressBoxSeperator.setMinHeight(2);
        botAddressBoxSeperator.setId("hGradient");

        HBox bottomGradientBox = new HBox(botAddressBoxSeperator);
        bottomGradientBox.setPadding(new Insets(5, 0,0,0));

        HBox addressFeesBox = new HBox(ergoQuantityBox, spfQuantityBox);
        HBox.setHgrow(addressFeesBox, Priority.ALWAYS);

        VBox selectedAddressBox = new VBox(adrBtnBox);
        HBox.setHgrow(selectedAddressBox,Priority.ALWAYS);

        ImageView walletIconView = new ImageView(ErgoWallets.getSmallAppIcon());
        walletIconView.setFitWidth(20);
        walletIconView.setPreserveRatio(true);

        Label ergoWalletsLbl = new Label("Ergo Wallets");
        ergoWalletsLbl.setFont(App.titleFont);
        ergoWalletsLbl.setTextFill(App.txtColor);
        ergoWalletsLbl.setPadding(new Insets(0, 0, 0, 10));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ImageView closeImage = App.highlightedImageView(App.closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        

        
        BufferedButton toggleShowWallets = new BufferedButton("/assets/caret-up-15.png", 15);
        toggleShowWallets.setId("toolBtn");
        toggleShowWallets.setPadding(new Insets(0, 5, 0, 3));
        toggleShowWallets.setOnAction(e->{
            showWallet.set(!showWallet.get());
           
        });
        

        
        
        BufferedButton openErgoWalletsBtn = new BufferedButton("/assets/open-outline-white-20.png", App.MENU_BAR_IMAGE_WIDTH);
        openErgoWalletsBtn.setId("toolBtn");
        openErgoWalletsBtn.setPadding(new Insets(0, 5, 0, 3));
        openErgoWalletsBtn.setOnAction(e->{
            ergoWallets.open();
        });   

        HBox walletsTopBar = new HBox(walletIconView, ergoWalletsLbl, spacer, openErgoWalletsBtn,toggleShowWallets);
        walletsTopBar.setAlignment(Pos.CENTER_LEFT);
        walletsTopBar.setPadding(new Insets(5,1, 5, 5));
        walletsTopBar.setId("networkTopBar");



        VBox selectWalletBox = new VBox( walletBox);
        selectWalletBox.setPadding(new Insets(0,0,0,5));
        selectWalletBox.setId("networkBox");

        VBox selectWalletPaddingBox = new VBox(walletsTopBar, selectWalletBox);
        selectWalletPaddingBox.setPadding(new Insets(0));
        


        ImageView poolStatsIconView = new ImageView(SpectrumFinance.getSmallAppIcon());
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
                if(!poolStatsPaddingBox.getChildren().contains(selectWalletBox)){
                    poolStatsPaddingBox.getChildren().add(selectWalletBox);
                }
            }else{
                if(poolStatsPaddingBox.getChildren().contains(selectWalletBox)){
                    poolStatsPaddingBox.getChildren().remove(selectWalletBox);
                }
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
                if(amountAvailable.getBigDecimalAmount().compareTo(bigAmount) == -1){
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

        Button disableWalletBtn = new Button();
        disableWalletBtn.setId("toolBtn");
        disableWalletBtn.setGraphic(closeImage);
        disableWalletBtn.setPadding(new Insets(0, 1, 0, 3));
        disableWalletBtn.setOnAction(e->{
            ergoWalletObject.set(null);
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

        ScrollPane walletBoxScroll = new ScrollPane(selectWalletPaddingBox);
        selectWalletPaddingBox.prefWidthProperty().bind(Bindings.createObjectBinding(()->walletBoxScroll.viewportBoundsProperty().get().getWidth() - 2, walletBoxScroll.viewportBoundsProperty()));

        showWallet.addListener((obs,oldval,newval)->{
            
            toggleShowWallets.setImage( newval ? new Image("/assets/caret-up-15.png") : new Image("/assets/caret-down-15.png"));   
        
            if(newval){
                if(!selectWalletPaddingBox.getChildren().contains(selectWalletBox)){
                    selectWalletPaddingBox.getChildren().add(selectWalletBox);
                }
            }else{
                if(selectWalletPaddingBox.getChildren().contains(selectWalletBox)){
                    selectWalletPaddingBox.getChildren().remove(selectWalletBox);
                }
            }
        });
    

        VBox marketPaddingBox = new VBox(orderTypeBox, marketBox);

        VBox swapBox = new VBox(walletBoxScroll, marketPaddingBox);
        
        walletBoxScroll.prefViewportHeightProperty().bind(swapBox.heightProperty().subtract(marketPaddingBox.heightProperty()));
        walletBoxScroll.prefViewportWidthProperty().bind(swapBox.widthProperty());
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

        Runnable addOpenItem = ()->{
            adrBtn.setText("[Locked]");
            adrBtn.getItems().clear();

            MenuItem openItem = new MenuItem(" (open)                     ");
            openItem.setOnAction(e->{
                ErgoWalletData walletData = ergoWalletObject.get();
                if(walletData != null){
                    walletData.getWallet((onSucceeded)->{
                        Object successObject = onSucceeded.getSource().getValue();
                        if(successObject != null){
                            addressesDataObject.set((AddressesData) successObject);
                        }else{
                            addressesDataObject.set(null);
                        }
    
                    }, (onFailed)->{
                        addressesDataObject.set(null);
                    });
                }
            });
            
            adrBtn.getItems().add(openItem);
        };

        Runnable updateErgoWallet = ()->{
            ErgoWalletData walletData = ergoWalletObject.get();
            addressesDataObject.set(null);

            
            if(walletData == null){
                walletBtn.setText(walletBtnDefaultString);
                if(walletBtn.getText().equals(walletBtnDefaultString)){
                    if(selectWalletBox.getChildren().contains(selectedAddressBox)){
                        selectWalletBox.getChildren().remove(selectedAddressBox);
                    }
                }
                if(walletBtnBox.getChildren().contains(disableWalletBtn)){
                    walletBtnBox.getChildren().remove(disableWalletBtn);
                }
            }else{
                if(!selectWalletBox.getChildren().contains(selectedAddressBox)){
                    selectWalletBox.getChildren().add(selectedAddressBox);
                }
                if(!walletBtnBox.getChildren().contains(disableWalletBtn)){
                    walletBtnBox.getChildren().add(disableWalletBtn);
                }
                walletBtn.setText(walletData.getName());
                addOpenItem.run();
                walletData.getWallet((onSucceeded)->{
                    Object successObject = onSucceeded.getSource().getValue();

                    if(successObject != null){
                       
                        addressesDataObject.set((AddressesData) successObject);
                    }else{
                        addressesDataObject.set(null);
                    }

                }, (onFailed)->{
                    addressesDataObject.set(null);
                });
            }
          
        };
   

        ergoWalletObject.addListener((obs, oldVal, newVal)->updateErgoWallet.run());

      

        Runnable updateBalances = () ->{
            boolean isInvert = isInvert();
            boolean isBuy = isBuyObject.get();

            String baseId = isInvert ? m_marketData.getQuoteId() :  m_marketData.getBaseId();
            String quoteId = isInvert ? m_marketData.getBaseId() :  m_marketData.getQuoteId();

            String baseSymbol = isInvert ? m_marketData.getQuoteSymbol() : m_marketData.getBaseSymbol();
            String quoteSymbol = isInvert ? m_marketData.getBaseSymbol() : m_marketData.getQuoteSymbol();

            AddressData adrData = addressesDataObject.get() != null ? addressesDataObject.get().selectedAddressDataProperty().get() : null;

            ErgoAmount ergoPriceAmount = adrData != null ? adrData.ergoAmountProperty().get() : null;
            PriceAmount spfPriceAmount = adrData != null ? adrData.getConfirmedTokenAmount(SPF_ID) : null;
            PriceAmount basePriceAmount = adrData != null ? (!baseId.equals(ERG_ID) ?  adrData.getConfirmedTokenAmount(baseId) : adrData.ergoAmountProperty().get()) : null; 
            PriceAmount quotePriceAmount = adrData != null ? (!quoteId.equals(ERG_ID) ? adrData.getConfirmedTokenAmount(quoteId) : adrData.ergoAmountProperty().get()) : null;

            ergoPriceAmountObject.set(ergoPriceAmount);
            spfPriceAmountObject.set(spfPriceAmount);
            basePriceAmountObject.set(basePriceAmount);
            quotePriceAmountObject.set(quotePriceAmount);

            updateOrderPriceObject.run();
            updateVolumeFromAmount.run();
           /*ergoAmount*/
            Image ergoCurrencyImage = ergoPriceAmount != null ? ergoPriceAmount.getCurrency().getBackgroundIcon(38) : PriceCurrency.getBlankBgIcon(38, "ERG");   
            ergoQuantityField.setText(ergoPriceAmount != null ? ergoPriceAmount.getAmountString() : "0.0");
            ergoImgView.setImage(ergoCurrencyImage);
            ergoImgView.setFitWidth(ergoCurrencyImage.getWidth());
         
            /*spf amount*/
            Image spfCurrencyImage = spfPriceAmount != null ? spfPriceAmount.getCurrency().getBackgroundIcon(38) : PriceCurrency.getBlankBgIcon(38, "SPF");   
            spfQuantityField.setText(spfPriceAmount != null ? spfPriceAmount.getAmountString() : "0.0");
            spfImgView.setImage(spfCurrencyImage);
            spfImgView.setFitWidth(spfCurrencyImage.getWidth());
        
        
            /*base Amount*/
            PriceCurrency baseCurrency = basePriceAmount != null ? basePriceAmount.getCurrency() : null;
            Image baseCurrencyImage = baseCurrency != null ?  baseCurrency.getBackgroundIcon(38) : PriceCurrency.getBlankBgIcon(38, baseSymbol);

            baseQuantityField.setText(basePriceAmount != null ? basePriceAmount.getAmountString() : "0.0");
            baseImgView.setImage(baseCurrencyImage);
            baseImgView.setFitWidth(baseCurrencyImage.getWidth());

            if(isBuy){
                double basePriceAmountDouble = basePriceAmount != null ? basePriceAmount.getDoubleAmount() : 0;
                amountFieldImage.setImage(baseCurrencyImage);
                amountFieldImage.setFitWidth(baseCurrencyImage.getWidth());
                amountSlider.setMax(basePriceAmountDouble);

                amountSlider.setMajorTickUnit(basePriceAmountDouble > 0 ? basePriceAmountDouble /4 : 1);

            }else{
                volumeFieldImage.setImage(baseCurrencyImage);
                volumeFieldImage.setFitWidth(baseCurrencyImage.getWidth());
            }
            
            /*quote Amount*/
            PriceCurrency quoteCurrency = quotePriceAmount != null ? quotePriceAmount.getCurrency() : null;
            Image quoteCurrencyImage = quoteCurrency != null ?  quoteCurrency.getBackgroundIcon(38) : PriceCurrency.getBlankBgIcon(38, quoteSymbol);

            quoteQuantityField.setText(quotePriceAmount != null ? quotePriceAmount.getAmountString() : "0.0");
            quoteImgView.setImage(quoteCurrencyImage);
            quoteImgView.setFitWidth(quoteCurrencyImage.getWidth());

            if(!isBuy){
                double quotePriceAmountDouble = quotePriceAmount != null ? quotePriceAmount.getDoubleAmount() : 0;
                amountFieldImage.setImage(quoteCurrencyImage);
                amountFieldImage.setFitWidth(quoteCurrencyImage.getWidth());
                amountSlider.setMax(quotePriceAmountDouble);
                amountSlider.setMajorTickUnit(quotePriceAmountDouble > 0 ? quotePriceAmountDouble /4 : 1);
            
            }else{
                volumeFieldImage.setImage(quoteCurrencyImage);
                volumeFieldImage.setFitWidth(quoteCurrencyImage.getWidth());
            }

      
        };
        updateBalances.run();

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


        Runnable updateFeesBtn = () ->{
            
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
        });

   

        isBuyObject.addListener((obs,oldval,newval)->{
            updateBuySellBtns.run();
            updateBalances.run();
         
        });

        m_isInvert.addListener((obs,oldval, newval)->{
            updateBuySellBtns.run();
            updateBalances.run();
            orderPriceImageView.setImage(PriceCurrency.getBlankBgIcon(38, m_marketData.getCurrentSymbol(newval)));
        });

        
        orderPriceObject.addListener((obs,oldval,newval)->{
         
            if(newval != null){
                
                orderPriceTextField.setText(newval.toString());
              
            }else{
                orderPriceTextField.setText("");
            }
        });



        isSpfFeesObject.addListener((obs,oldval,newval)->updateFeesBtn.run());
       //AddressData update
            
        ChangeListener<LocalDateTime> addressUpdateListener = (obs,oldval,newval)->{
            
            updateBalances.run();
   
        };

        ChangeListener<AddressData> selectedAddressListener = (obs,oldval,newval)->{
            if(oldval != null){
                oldval.getLastUpdated().removeListener(addressUpdateListener);
            }
            if(newval != null){
                newval.getLastUpdated().addListener(addressUpdateListener);
            }
            updateBalances.run();
        
        };
        
        m_marketData.getLastUpdated().addListener((obs,oldVal,newVal)->{
            String orderType = orderTypeStringObject.get() != null ? orderTypeStringObject.get() : "";
            
            switch(orderType){
                case MARKET_ORDER:
                    updateOrderPriceObject.run();
                    updateVolumeFromAmount.run();
                    orderPriceStatusField.setText(Utils.formatTimeString(newVal));
                break;
            }

            
            
        });

        Runnable updateNodeBtn = () ->{
            
            AddressesData addressesData = addressesDataObject.get();
            if(addressesData != null){
                ErgoNodeData nodeData = addressesData != null ? addressesData.selectedNodeData().get() : null;

                
                if(nodeData != null && ergoNodes != null){

                    nodeBtn.setText(nodeData.getName());
                
                }else{
                    
                    if(ergoNodes == null){
                        nodeBtn.setText("[Install]");
                    }else{
                        nodeBtn.setText("[Select]");
                    }
                }
            }
          
            
        };
        ChangeListener<Boolean> isAvailableListener = (obs,oldval,newval)->nodeAvailableObject.set(newval);
        ChangeListener<String> statusListener = (obs,oldVal, newVal)->{
            ErgoNodeData ergoNodeData = addressesDataObject.get() != null && addressesDataObject.get().selectedNodeData().get() != null ? addressesDataObject.get().selectedNodeData().get() : null;
            
            nodeStatusObject.set(ergoNodeData == null ? null : ergoNodeData.statusString().get());
            
        };

        ChangeListener<ErgoNodeData> selectedNodeListener = (obs,oldval,newval) -> {
           // nodeAvailableObject.unbind();
           // nodeStatusObject.unbind();
            ErgoWalletData ergoWalletData =  ergoWalletObject.get();
            if(oldval != null){
                oldval.isAvailableProperty().removeListener(isAvailableListener);
                oldval.statusString().removeListener(statusListener);
            }
            if(newval != null){
                if(newval.isStopped()){ 
                    newval.start();
                }
                //nodeAvailableListenerstatusListener
                newval.isAvailableProperty().addListener(isAvailableListener);
                newval.statusString().addListener(statusListener);
                
                nodeAvailableObject.set(newval.isAvailable());
              //  nodeStatusObject.bind(newval.nodeStatusInfo());
            }
            if(ergoWalletData != null){
                ergoWalletData.setNodesId(newval == null ? null : newval.getId());
            }
            updateNodeBtn.run();
            
            
        };
        
      
        
        addressesDataObject.addListener((obs,oldval,newval)->{
            adrBtn.getItems().clear();
            nodeBtn.getItems().clear();

            if(oldval != null){
                if(oldval.selectedAddressDataProperty().get() != null){
                    oldval.selectedAddressDataProperty().get().getLastUpdated().removeListener(addressUpdateListener);
                
                }
                oldval.selectedAddressDataProperty().removeListener(selectedAddressListener);
                oldval.selectedNodeData().removeListener(selectedNodeListener);

                if(oldval.selectedNodeData().get() != null){
                    ErgoNodeData selectedErgoNodeData = oldval.selectedNodeData().get();
                    selectedErgoNodeData.isAvailableProperty().removeListener(isAvailableListener);
                    selectedErgoNodeData.statusString().removeListener(statusListener);

                
                }
                oldval.shutdown();
            }

            HBox marketItemsBox = new HBox(baseQuantityBox, quoteQuantityBox);
            marketItemsBox.setPadding(new Insets(5,0,5,0));
            HBox.setHgrow(marketItemsBox, Priority.ALWAYS);

            VBox balancesBox = new VBox(addressBoxSepBox, addressFeesBox, marketItemsBox , nodeBox, bottomGradientBox);
            balancesBox.setId("bodyBox");

            Region nodeBoxSpacer = new Region();
            nodeBoxSpacer.setMinHeight(5);

            Region addressPaddingBoxRegion = new Region();
            addressPaddingBoxRegion.setId("hGradient");
            addressPaddingBoxRegion.setMinHeight(2);

            HBox.setHgrow(addressPaddingBoxRegion, Priority.ALWAYS);

            HBox addressPaddingBoxRegionBox = new HBox(addressPaddingBoxRegion);
            addressPaddingBoxRegionBox.setPadding(new Insets(1,0,4,0));

            VBox addressPaddingBox = new VBox(balancesBox );
           

            if(newval == null){
             

                if(selectedAddressBox.getChildren().contains(addressPaddingBox)){
                    selectedAddressBox.getChildren().remove(addressPaddingBox);
                }
                
                if(ergoWalletObject.get() == null){
                    adrBtn.setText(adrBtnDefaultString);
                    
                    if(selectWalletBox.getChildren().contains(selectedAddressBox)){
                        selectWalletBox.getChildren().remove(selectedAddressBox);
                    }
                    
                }else{
                    if(!selectWalletBox.getChildren().contains(selectedAddressBox)){
                        selectWalletBox.getChildren().add(selectedAddressBox);
                    }
                    addOpenItem.run();
                }
                
            }else{
               
                if(!selectedAddressBox.getChildren().contains(addressPaddingBox)){
                    selectedAddressBox.getChildren().add(addressPaddingBox);
                   
                }
                if(!selectWalletBox.getChildren().contains(selectedAddressBox)){
                    selectWalletBox.getChildren().add(selectedAddressBox);
                }
                if(newval.selectedNodeData().get() != null){
                    ErgoNodeData selectedErgoNodeData = newval.selectedNodeData().get();

                    if(!selectedErgoNodeData.statusProperty().get().equals(ErgoMarketsData.STARTED)){
                        selectedErgoNodeData.start();
                    }

                    selectedErgoNodeData.isAvailableProperty().addListener(isAvailableListener);
                    selectedErgoNodeData.statusString().addListener(statusListener);
                    nodeAvailableObject.set(selectedErgoNodeData.isAvailableProperty().get());
                    nodeStatusObject.set(selectedErgoNodeData.statusString().get());
                }

                if(newval.selectedAddressDataProperty().get() != null){
                    newval.selectedAddressDataProperty().get().getLastUpdated().addListener(addressUpdateListener);
                }
     
                newval.getMenu(adrBtn);
                ergoNodes.getErgoNodesList().getMenu(nodeBtn, newval.selectedNodeData());
                newval.selectedNodeData().addListener(selectedNodeListener);
                updateNodeBtn.run();

                AddressData selectedAddressData = newval.selectedAddressDataProperty().get();
                
                if( selectedAddressData == null && adrBtn.getItems().size() > 0){
                    adrBtn.getItems().get(0).fire();
                }
                
                if(selectedAddressData == null){
                    adrBtn.setText("[Select address]");
                }

                newval.selectedAddressDataProperty().addListener(selectedAddressListener);
                
            }
            updateBalances.run();
        });

        walletsListObject.addListener((obs,oldVal,newVal)->{});

        

        

        shutdownSwap.addListener((obs,oldval,newval)->{
            if(newval){
                AddressesData addressesData = addressesDataObject.get();
                if(addressesData != null){
                    addressesData.shutdown();
                }
            }
        });

        return swapBox;
    }



    public SecretKey getAppKey(){
        return m_dataList.getSpectrumFinance().getAppKey();
    }



    public String getSymbol() {
        return m_marketData != null ? m_marketData.getSymbol() : "Unknown";
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
