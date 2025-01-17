package com.netnotes;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.time.LocalDateTime;

import com.devskiller.friendly_id.FriendlyId;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
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

public class ErgoDexMarketItem {
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


    public static int FILL_COLOR = 0xffffffff;
    public static java.awt.Color WHITE_COLOR = new java.awt.Color(FILL_COLOR, true);

    private ErgoDexDataList m_dataList = null;
    private final ErgoDexMarketData m_marketData;
    
    private final double regularHeight = 50;
    private final double focusedHeight = 152;
    private final int  chartWidthOffset = 200;
    

   // private Stage m_stage = null;

    private double m_currentHeight =regularHeight;

    private int m_posColor = 0xff028A0F;
    private int m_negColor = 0xff9A2A2A;

    private SimpleStringProperty m_statusProperty = new SimpleStringProperty();
    private SimpleBooleanProperty m_isInvert;
    private BufferedImage m_rowImg = null;
    private SimpleObjectProperty<HBox> m_rowBox = new SimpleObjectProperty<>(null);

    private ErgoDexNumbers m_numbers;
    private ImageView m_rowChartImgView = new ImageView();
    private TextField m_priceText;
    

    private ChangeListener<Number> m_widthListener;
    private ChangeListener<TimeSpan> m_timeSpanListener;
    private ChangeListener<HBox> m_currentBoxChangeListener;
    private ChangeListener<LocalDateTime> m_marketDataUpdated;
    private ChangeListener<String> m_statusChangeListener;

    private NoteMsgInterface m_chartViewListener = null;
    // private ArrayList<SpectrumMarketInterface> m_msgListeners = new ArrayList<>();

    private WritableImage m_rowWImg = null;
    

        



    public ErgoDexMarketItem(ErgoDexMarketData marketData, ErgoDexDataList dataList) {
        m_dataList = dataList;
        m_marketData = marketData;

        m_isInvert = new SimpleBooleanProperty();
        if(marketData.getDefaultInvert()){
            m_isInvert.bind(m_dataList.isInvertProperty().not());
        }else{
            m_isInvert.bind(m_dataList.isInvertProperty());
        }
   
    }  
    



    public SimpleStringProperty statusProperty(){
        return m_statusProperty;
    }


    public ErgoDexMarketData getMarketData() {
        return m_marketData;
    }

    

   
    HBox getRowBox(){
        HBox rowBox = m_rowBox.get();
        if(rowBox == null){
            setupRowBox();
            return m_rowBox.get();
        }else{
            return rowBox;
        }

    }

    private SimpleBooleanProperty m_isShowing = new SimpleBooleanProperty(false);

    private int m_itemIndex = -1;

    public void setItemIndex(int index){
        m_itemIndex = index;
    }

    public int getItemIndex(){
        return m_itemIndex;
    }

    public SimpleBooleanProperty rowBoxisShowing(){
        return m_isShowing;
    }

    

    public double getCurrentHeight(){
        return m_currentHeight;
    }

    public void setCurrentHeight(double height){
        m_currentHeight = height;
          
        m_rowChartImgView.setFitWidth(calculateCurrentImgWidth());
        m_rowChartImgView.setFitHeight(height);
    }

    private HBox symbolTextPaddingBox;
    private HBox priceHBox ;
    private StackPane rowImgBox;
    private HBox statsBox;
    private VBox statsVbox;
    private TextField openText;
    private TextField highText;
    private TextField lowText;
    private Text percentChangeText;
    private Insets priceHBoxDefaultPadding;
    private NumberFormat percentFormat;

    public void setupRowBox() {

        String friendlyId = FriendlyId.createFriendlyId();
      
        
    
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
      


        Button openChartBtn = new Button();
        openChartBtn.setMinWidth(10);
        openChartBtn.setPrefWidth(10);
        openChartBtn.setMinHeight(15);
        openChartBtn.setMaxHeight(15);

        openChartBtn.setId(getPoolId() != null ? "onlineBtn" : "offlineBtn");
        openChartBtn.setOnAction(e->{
            if(getPoolId() != null){
                open();
            }
        });
      
        m_rowChartImgView.setPreserveRatio(false);
     
        HBox chartBox = new HBox(m_rowChartImgView);
        chartBox.setPadding(new Insets(0));
        chartBox.minWidthProperty().bind(m_dataList.gridWidthProperty().subtract(chartWidthOffset));


        String initialValue = isInvert() ? m_marketData.getInvertedLastPrice() + "" :  m_marketData.getLastPrice() + "";
        initialValue = initialValue.substring(0,Math.min(12, initialValue.length()));
        
       

        m_priceText = new TextField(initialValue);
        m_priceText.setEditable(false);
        m_priceText.setId("priceText");
        m_priceText.setPrefWidth(100);
        
        Text symbolText = new Text(m_marketData.getCurrentSymbol(isInvert()));
        symbolText.setFont(Font.font("DejaVu Sans Mono, Book", FontWeight.NORMAL, 14));
        symbolText.setFill(Color.WHITE);
        
        DropShadow shadow = new DropShadow();
        symbolText.setEffect(shadow);
        
        HBox openChartBtnBox = new HBox(openChartBtn);
        openChartBtnBox.setPadding(new Insets(3, 0,0,10));

        priceHBoxDefaultPadding = new Insets(0, 10,10,0);

        priceHBox = new HBox(m_priceText, openChartBtnBox);
        HBox.setHgrow(priceHBox, Priority.ALWAYS);
        priceHBox.setAlignment(Pos.TOP_RIGHT);
        priceHBox.setPadding(priceHBoxDefaultPadding);
 
        priceHBox.setMinWidth(90);

        /*HBox symbolTextBox = new HBox();
        symbolTextBox.setMaxHeight(  regularHeight);
        symbolTextBox.setMinHeight(regularHeight);
        symbolTextBox.setAlignment(Pos.CENTER_LEFT);*/

        symbolTextPaddingBox = new HBox(symbolText, priceHBox);
        symbolTextPaddingBox.setPadding(new Insets(10,0,0,10));
        VBox.setVgrow(symbolTextPaddingBox, Priority.ALWAYS);
        HBox.setHgrow(symbolTextPaddingBox,Priority.ALWAYS);
        //symbolTextPaddingBox.setPrefWidth(App.DEFAULT_STATIC_WIDTH);
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
          /*  NoteInterface networkInterface = m_dataList.networkInterfaceProperty().get();
            if(networkInterface == null){
                return;
            }

                    
            String baseTokenId =  m_marketData.getBaseId();
            String baseTokenName = m_marketData.getBaseSymbol();
            int baseDecimals = m_marketData.getBaseDecimals();

        
            m_tmpObj = Utils.getCmdObject("getAddToken");
            m_tmpObj.addProperty("networkId", ErgoNetwork.TOKEN_NETWORK);

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
            m_tmpObj.addProperty("networkId", ErgoNetwork.TOKEN_NETWORK);
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

                
            m_tmpObj = null;*/
        };
       
        
        rowImgBox = new StackPane(chartBox, imagesBox, symbolTextPaddingBox);
       
        HBox.setHgrow(rowImgBox,Priority.ALWAYS);
        rowImgBox.setMinWidth(NetworksData.DEFAULT_STATIC_WIDTH-25);
        rowImgBox.setAlignment(Pos.CENTER_LEFT);
        rowImgBox.setPadding(new Insets(0,0,0,0));




                       

        statsBox = new HBox();
        statsBox.setId("transparentColor");
        HBox.setHgrow(statsBox, Priority.ALWAYS);
        VBox.setVgrow(statsBox, Priority.ALWAYS);
        statsBox.setAlignment(Pos.CENTER_RIGHT);
        statsBox.setPadding(new Insets(0,0,0,0));

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

        percentChangeText = new Text("0.00%");
        percentChangeText.setFont(App.txtFont);

        double textWidth = 110;


        openText = new TextField();
        openText.setPrefWidth(textWidth);
        highText = new TextField(); 
        highText.setPrefWidth(textWidth);
        lowText = new TextField();
        lowText.setPrefWidth(textWidth);

        Insets boxPadding = new Insets(5);

        HBox openHbox = new HBox(lblOpenText, openText);
        openHbox.setPadding(boxPadding);
        HBox changeHBox = new HBox(lblPercentChangeText, percentChangeText);
        changeHBox.setPadding(boxPadding);
        HBox highHBox = new HBox(lblHighText, highText);
        highHBox.setPadding(boxPadding);
        HBox lowHBox = new HBox(lblLowText, lowText);
        lowHBox.setPadding(boxPadding);

        statsVbox = new VBox( openHbox,changeHBox, highHBox, lowHBox);
        statsVbox.setPadding(new Insets(10,0,0,0));
        VBox.setVgrow(statsVbox, Priority.ALWAYS);
        statsVbox.setId("transparentColor");
        percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMaximumFractionDigits(2);
        

  


        m_widthListener = (obs,oldval,newval) ->{
            updateRowImg();
        };


        m_timeSpanListener = (obs,oldval,newval) ->{
            updateRowImg();
        };
      
  

       

        Runnable addChartViewListener = ()->{
            ErgoDexChartView chartView = m_marketData.getChartView();
            
            if(chartView != null && m_chartViewListener  == null){
                m_chartViewListener = new NoteMsgInterface() {
                    public String getId() {
                        return friendlyId;
                    }
                    
                    public void sendMessage(int code, long timeStamp, String networkId, Number num){
                      
                        switch(code){
                            case App.STARTED:
                            case App.LIST_UPDATED:
                                updateRowImg();
                                
                            break;
                
                            case App.STOPPED:

                            break;
                        }   
                        
                    }

                    public void sendMessage(int code, long timestamp, String networkId, String msg){
                        switch(code){
                            case App.ERROR:
                            updateRowImg();
                            break;
                        }   
                    }

                  
                 
                };
                
              

                chartView.addMsgListener(m_chartViewListener);
       
            }
        };

                 
        m_dataList.gridWidthProperty().addListener(m_widthListener);
        m_dataList.timeSpanObjectProperty().addListener(m_timeSpanListener);
   

     

        Region phb0 = new Region();
        phb0.setMinWidth(15);
        HBox.setHgrow(phb0,Priority.ALWAYS);

        Region phb1 = new Region();
        HBox.setHgrow(phb1,Priority.ALWAYS);

      



        /*HBox menuBtnBox = new HBox(menuBtn);
        menuBtnBox.setMaxHeight(32);
        menuBtnBox.setAlignment(Pos.CENTER_LEFT);

        VBox leftMarginVBox = new VBox(menuBtnBox);
        leftMarginVBox.setAlignment(Pos.TOP_CENTER);
        leftMarginVBox.setId("darkBox");*/

        HBox rowBox = new HBox( rowImgBox );
        rowBox.setId("rowBox");
        rowBox.setAlignment(Pos.TOP_LEFT);
      //  rowBox.maxWidthProperty().bind(m_dataList.gridWidthProperty());
        rowBox.setFocusTraversable(true);

        rowBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e->{
            

            if(e.getClickCount() == 2){
                open();
            }
      
            
            m_dataList.currentBoxProperty().set(rowBox);
            
            
        });

        rowBox.setAlignment(Pos.CENTER_LEFT);
     


        
        Runnable update = ()->{
            boolean isInvert = isInvert();
            boolean isChart = getPoolId() != null;
            openChartBtn.setId(m_marketData != null ? (isChart ? "availableBtn" : "offlineBtn") : "offlineBtn");
            symbolText.setText(m_marketData.getCurrentSymbol(isInvert));
            if(!isChart){
                m_priceText.setText(isInvert ? m_marketData.getInvertedLastPrice() + "" : m_marketData.getLastPrice() + "");
            }

        };

        Runnable updateLogo = ()->{
            
            Drawing.clearImage(logo);
            Image qImg = quoteImg.get();
          

            Image bImg = baseImg.get();
            int limitAlpha = 0x40;
            
            if(isInvert()){
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


        m_currentBoxChangeListener = (obs,oldval,newval)->{

            boolean isCurrent = newval != null && newval.equals(rowBox);
            rowBox.setId(isCurrent ? "headingBox" : "rowBox");
            
            setCurrent(isCurrent);

            imagesBox.setAlignment(isCurrent ? Pos.TOP_LEFT : Pos.CENTER);

            
        };

  
        m_dataList.currentBoxProperty().addListener(m_currentBoxChangeListener);

       

        

        m_marketDataUpdated = (obs, oldVal, newVal) -> update.run();
        ChangeListener<Boolean> invertChanged = (obs,oldval,newval)->{
            update.run();
            updateLogo.run();
            updateRowImg();
        };
      
        m_isInvert.addListener(invertChanged);

        m_marketData.getLastUpdated().addListener(m_marketDataUpdated);

        update.run();

        updateImages.run();

        if(m_marketData.isPool()){
            addChartViewListener.run();
        }





        m_statusChangeListener = (obs,oldval,newval)->{
            updateRowImg();
        };

        m_dataList.statusMsgProperty().addListener(m_statusChangeListener);
        



        m_rowBox.set(rowBox);
    }

  

    public void init(){
        if(m_marketData.isPool()){
            boolean isNotSet = m_marketData.getChartView() == null;            
            ErgoDexChartView chartView = isNotSet ? new ErgoDexChartView(m_marketData, m_dataList.getErgoDex(), m_dataList) : m_marketData.getChartView();

            if(isNotSet){
                m_marketData.setChartView(chartView);
               
                updateRowImg();

            }

        }
        setupRowBox();
    }

    private boolean m_isCurrent = false;

    public boolean isCurrent(){
        return m_isCurrent;
    }

    public int calculateCurrentImgWidth(){
        return (int) m_dataList.gridWidthProperty().get() - (isCurrent() ? chartWidthOffset : 0);
    }

    public void setCurrent(boolean isCurrent){
        m_isCurrent = isCurrent;

        setCurrentHeight(isCurrent ? focusedHeight : regularHeight);

        Insets selectedInsets = new Insets(0,10,0,0);        
       

        ErgoDexNumbers numbers = m_numbers;



        if(isCurrent){
            symbolTextPaddingBox.getChildren().remove(priceHBox);
            priceHBox.setPadding(selectedInsets);
            if(!rowImgBox.getChildren().contains(statsBox)){
                rowImgBox.getChildren().add(statsBox);
            }
            if(!statsBox.getChildren().contains(statsVbox)){
                statsBox.getChildren().add(statsVbox);
                
                statsVbox.getChildren().add(0,priceHBox);
                
            }

 


            String openString = numbers != null ? numbers.getOpen().toPlainString() : "0";
            String highString = numbers != null ? numbers.getHigh().toPlainString() : "0";
            String lowString =  numbers != null ? numbers.getLow().toPlainString() : "0";

            openText.setText(String.format("%-12s", openString).substring(0,12));
            highText.setText(String.format("%-12s", highString).substring(0,12) );
            lowText.setText(String.format("%-12s", lowString).substring(0,12) );

            BigDecimal increase = numbers != null ? numbers.getPercentIncrease() : BigDecimal.ZERO;

            increase = increase == null ? BigDecimal.ZERO : increase;

            int increaseDirection = BigDecimal.ZERO.compareTo(increase);
            
            
            percentChangeText.setText(increaseDirection == 0 ? " 0.00%" : (increaseDirection == -1 ? "+" :"") + percentFormat.format(increase));
            percentChangeText.setFill(increaseDirection == 0 ? App.txtColor  : (increaseDirection == -1 ? Color.web("#028A0F") : Color.web("#ffb8e8")) );
        
        }else{
            priceHBox.setPadding(priceHBoxDefaultPadding);
            rowImgBox.getChildren().remove(statsBox);
            statsVbox.getChildren().remove(priceHBox);

            if(!symbolTextPaddingBox.getChildren().contains(priceHBox)){
                symbolTextPaddingBox.getChildren().add(priceHBox);
            }
            statsBox.getChildren().clear();
            
        }
        
         
    }


    public void updateRowImg(){
        ErgoDexChartView chartView =  m_marketData.getChartView();


        if(chartView != null ){
            if(chartView.getConnectionStatus() != App.ERROR ){

                int width = calculateCurrentImgWidth();
                int height = (int) m_currentHeight;

                int imgCellWidth = 1;
                int maxBars = width / imgCellWidth;

                boolean invert = isInvert();
                TimeSpan durationSpan = m_dataList.timeSpanObjectProperty().get();
                long durationMillis = durationSpan.getMillis();
                long colSpanMillis = (durationMillis / maxBars);
                TimeSpan colSpan = new TimeSpan(colSpanMillis, "custom","id1");
                
        
                long currentTime = (long) (Math.ceil(System.currentTimeMillis() / colSpanMillis)* colSpanMillis) + colSpanMillis;

                long startTimeStamp = currentTime - durationSpan.getMillis();

                
                chartView.processData(invert, startTimeStamp, colSpan, currentTime, m_dataList.getErgoDex().getExecService(), (onSucceeded)->{
                    Object sourceValue = onSucceeded.getSource().getValue();
                    if(sourceValue != null && sourceValue instanceof ErgoDexNumbers){
                        ErgoDexNumbers numbers =(ErgoDexNumbers) sourceValue;
                        
                
                        
                        setNumbers(numbers);
                        
                        boolean isNewImage = (m_rowImg == null) ||  (m_rowImg != null &&(m_rowImg.getWidth() != width || m_rowImg.getHeight() != height));
                        m_rowImg = isNewImage ? new BufferedImage(width , height, BufferedImage.TYPE_INT_ARGB) : m_rowImg; 
                        
                        
                        chartView.updateRowChart(numbers, colSpan, imgCellWidth, m_rowImg, m_posColor, m_negColor);
                        
                        m_rowChartImgView.setImage(SwingFXUtils.toFXImage(m_rowImg, m_rowWImg));
                        m_rowChartImgView.setFitWidth(m_rowImg.getWidth());
                        m_rowChartImgView.setFitHeight(m_rowImg.getHeight());
                     
                        String priceString = String.format("%-12s", numbers.getClose() + "").substring(0,12).trim();
                        
                        m_priceText.setPrefWidth(isCurrent() ? 130 : 100);
                        m_priceText.setText(priceString);
                
                        
                
                    }

                }, (onFailed)->{
                    try {
                        Files.writeString(App.logFile.toPath(), "(ErgoDexMarketItem) updateRowImg onFailed: " + onFailed.getSource().getException().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {
               
                    }
                    m_rowChartImgView.setFitWidth(width);
                    m_rowChartImgView.setFitHeight(height);
                });
            }else{
                try {
                    Files.writeString(App.logFile.toPath(), "(ErgoDexMarketItem) updateRowImg error: " + chartView.getErrorString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
              
                }
            }

            
        }else{
            
        }
    }

    public void setNumbers(ErgoDexNumbers numbers){
        m_numbers = numbers;
        setCurrent(m_isCurrent);
    }

 

    public void shutdown(){
        ErgoDexChartView chartView = m_marketData.getChartView();
        if(m_currentBoxChangeListener != null){
            m_dataList.currentBoxProperty().removeListener(m_currentBoxChangeListener);
        }
        if(m_chartTab != null){
            m_chartTab.close();
            m_chartTab = null;
        }

        if(chartView != null){ 
            if(m_chartViewListener != null){
                chartView.removeMsgListener(m_chartViewListener);
                m_chartViewListener = null;
            }
           
        }

        m_dataList.gridWidthProperty().removeListener(m_widthListener);
        m_dataList.timeSpanObjectProperty().removeListener(m_timeSpanListener);
        m_dataList.currentBoxProperty().removeListener(m_currentBoxChangeListener);

        m_marketData.getLastUpdated().removeListener(m_marketDataUpdated);
   
        m_dataList.statusMsgProperty().removeListener(m_statusChangeListener);

    }

    public ReadOnlyBooleanProperty isInvertProperty(){

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

    private ErgoDexChartTab m_chartTab = null;

    public void open(){
        if(m_chartTab == null){

            Image logo = m_dataList.getErgoDex().getSmallAppIcon();
            


            VBox layoutBox = new VBox();
            layoutBox.setPrefWidth(getNetworksData().getContentTabs().bodyWidthProperty().get());
            layoutBox.setPrefHeight(getNetworksData().getContentTabs().bodyHeightProperty().get());

            m_chartTab = new ErgoDexChartTab(FriendlyId.createFriendlyId(), 
                logo, 
                m_marketData.getCurrentSymbol(isInvert()) + (m_marketData != null ? " - " +(isInvert() ? m_marketData.getInvertedLastPrice().toString() : m_marketData.getLastPrice()) + "" : ""), 
                layoutBox, 
                m_dataList, 
                m_marketData, 
                this);
           
            m_chartTab.shutdownMilliesProperty().addListener((obs,oldval,newval)->{
                if(newval.longValue() > 0){
                    
                    m_chartTab = null;
                }
                
            });
            getNetworksData().getContentTabs().addContentTab(m_chartTab);


        }else{
            if(!getNetworksData().getContentTabs().containsId(m_chartTab.getId())){
                getNetworksData().getContentTabs().addContentTab(m_chartTab);
            }
        }
    }
    
    public boolean isInvert(){
        return m_isInvert.get();
    }
  

    public Stage getStage(){
        return m_dataList.appStage();
    }


    public Scene getScene(){
        return m_dataList.appStage().getScene();
    }

  

    public NetworksData getNetworksData(){
        return m_dataList.getErgoDex().getNetworksData();
    }

    public NoteInterface getCurrentNetwork(){
        return  getNetworksData().getNetwork(ErgoNetwork.NETWORK_ID);
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
        return m_marketData.getBaseVolume() != null ? m_marketData.getBaseVolume().getBigDecimalAmount() : BigDecimal.ZERO;
    }
    public BigDecimal getQuoteVolume(){
        return m_marketData.getQuoteVolume() != null ? m_marketData.getQuoteVolume().getBigDecimalAmount() : BigDecimal.ZERO;
    }

  

}
