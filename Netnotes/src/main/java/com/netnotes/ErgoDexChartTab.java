package com.netnotes;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netnotes.NetworksData.ManageNetworksTab;
import com.utils.Utils;

import org.reactfx.util.Timer;

import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.io.IOException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.reactfx.util.FxTimer;

public class ErgoDexChartTab extends ContentTab {
    private final ErgoDexDataList m_dataList;
    private final ErgoDexMarketData m_marketData;
    private final ErgoDexMarketItem m_marketItem;

    public final static String MARKET_ORDER = "Market order";
    public final static String LIMIT_ORDER = "limit order";
    public final static double SWAP_BOX_MIN_WIDTH = 300;
    public final static String DEFAULT_ID = "DEFAULT_ID";

    private ScrollPane m_chartScroll;
    private int m_cellWidth = 20;
    private int m_cellPadding = 3;
   
    private ErgoDexChartView m_spectrumChartView;
    private java.awt.Font m_labelFont;
    private FontMetrics m_labelMetrics;
    private int m_amStringWidth;
    private int m_zeroStringWidth;
    private RangeBar m_chartRange;
    private ImageView m_chartImageView;
    private ChangeListener<Boolean> m_invertListener = null;
    private MenuButton m_timeSpanBtn;
   
  
    private BufferedImage m_img = null;
    private Graphics2D m_g2d = null;
    private WritableImage m_wImg = null;
    private ErgoDexNumbers m_numbers = null;
    private NoteMsgInterface m_chartMsgInterface;

    private final double chartScrollVvalue = 1;
    private final double chartScrollHvalue = 1;

    private SimpleObjectProperty<ControlInterface> m_currentControl = new SimpleObjectProperty<>(null);
    private SimpleStringProperty m_titleProperty;

    private ErgoDexSwapBox m_ergoDexSwapBox = null;
    private Button m_toggleSwapBtn;
    private HBox m_bodyBox;

    private SimpleBooleanProperty m_showSwap = new SimpleBooleanProperty(true);
    private SimpleBooleanProperty m_showWallet = new SimpleBooleanProperty(true);
    private SimpleBooleanProperty m_showPoolStats = new SimpleBooleanProperty(true);
        

    private TimeSpan m_timeSpan = null;
    private String m_defaultWalletId = DEFAULT_ID;

    public static final long DELAY_MILLIS = 200;
    private Timer m_delayTimer = null;
    

    public boolean isInvert(){
        return m_marketItem.isInvert();
    }

    public NetworksData getNetworksData(){
        return m_marketItem.getNetworksData();
    }

    public Scene getScene(){
        return m_marketItem.getScene();
    }

    public SimpleBooleanProperty isInvertProperty(){
        return m_marketItem.isInvertProperty();
    }



    public ErgoDexChartTab(String id, Image logo, String title,  Pane layoutBox,  ErgoDexDataList dataList, ErgoDexMarketData marketData, ErgoDexMarketItem marketItem){
        super(id, ErgoDex.NETWORK_ID, logo, title , layoutBox);
        m_dataList = dataList;
        m_marketData = marketData;
        m_marketItem = marketItem;


        getData();
      
        m_titleProperty = new SimpleStringProperty(title);

        m_labelFont = m_dataList.getLabelFont();
        m_labelMetrics = m_dataList.getLabelMetrics();
        m_amStringWidth = m_labelMetrics.stringWidth(" a.m. ");
        m_zeroStringWidth = m_labelMetrics.stringWidth("0");

        if(!m_marketData.isPool() || m_marketData.getSpectrumChartView().get() == null){
            Alert a = new Alert(AlertType.NONE, "Price history unavailable.", ButtonType.OK);
            a.setHeaderText("Notice");
            a.setTitle("Notice: Price history unavailable");
            a.showAndWait();
            return;
        }
        m_spectrumChartView = m_marketData.getSpectrumChartView().get();

        SimpleDoubleProperty rangeWidth = new SimpleDoubleProperty(12);
        SimpleDoubleProperty rangeHeight = new SimpleDoubleProperty(100);

       // ErgoDex exchange = m_dataList.getErgoDex();
        
        BufferedMenuButton menuButton = new BufferedMenuButton("/assets/menu-outline-30.png", App.MENU_BAR_IMAGE_WIDTH);
        BufferedButton invertBtn = new BufferedButton( isInvert() ? "/assets/targetSwapped.png" : "/assets/targetStandard.png", App.MENU_BAR_IMAGE_WIDTH);
        
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



        m_timeSpanBtn = new MenuButton(m_timeSpan.getName());
        m_timeSpanBtn.setFont(App.txtFont);
        m_timeSpanBtn.setContentDisplay(ContentDisplay.LEFT);
        m_timeSpanBtn.setAlignment(Pos.CENTER_LEFT);

        
        Region headingBoxSpacerR = new Region();
        HBox.setHgrow(headingBoxSpacerR,Priority.ALWAYS);

        HBox headingBox = new HBox(headingSpacerL, headingText, m_timeSpanBtn, headingBoxSpacerR);
        headingBox.prefHeight(40);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(5, 5, 5, 5));
        headingBox.setId("headingBox");
        
        m_chartRange = new RangeBar(rangeWidth, rangeHeight, getNetworksData().getExecService());
        m_chartImageView = new ImageView();
        m_chartImageView.setPreserveRatio(true);
        m_chartScroll = new ScrollPane(m_chartImageView);        
        
        headingSpacerL.prefWidthProperty().bind(headingBox.widthProperty().subtract(m_timeSpanBtn.widthProperty().divide(2)).subtract(headingText.layoutBoundsProperty().get().getWidth()).divide(2));
        
        Region headingPaddingRegion = new Region();
        headingPaddingRegion.setMinHeight(5);
        headingPaddingRegion.setPrefHeight(5);

        VBox paddingBox = new VBox(menuBar, headingPaddingRegion, headingBox);
        paddingBox.setPadding(new Insets(0, 5, 0, 5));

        VBox headerVBox = new VBox(paddingBox);
        m_chartScroll.setPadding(new Insets(0, 0, 0, 0));

        setChartRangeItem.setOnAction((e)->m_chartRange.toggleSettingRange());

     
                    //⯈🗘
        m_toggleSwapBtn = new Button("⯈");
        m_toggleSwapBtn.setTextFill(App.txtColor);
        m_toggleSwapBtn.setFont( Font.font("OCR A Extended", 10));
        m_toggleSwapBtn.setPadding(new Insets(2,2,2,1));
        m_toggleSwapBtn.setId("barBtn");
        //toggleSwapBtn.setMinWidth(20);
        m_toggleSwapBtn.setOnAction(e->{
            setShowSwap(!getShowSwap());
        });

        Region topRegion = new Region();
        VBox.setVgrow(topRegion, Priority.ALWAYS);

        
        HBox swapButtonPaddingBox = new HBox(m_toggleSwapBtn);
        swapButtonPaddingBox.setId("darkBox");
        swapButtonPaddingBox.setPadding(new Insets(1,0,1,2));

        VBox swapButtonBox = new VBox(swapButtonPaddingBox);
        VBox.setVgrow(swapButtonBox,Priority.ALWAYS);
        swapButtonBox.setAlignment(Pos.CENTER);
        swapButtonBox.setId("darkBox");

        m_bodyBox = new HBox(m_chartRange, m_chartScroll, swapButtonBox);
        m_bodyBox.setId("bodyBox");
        m_bodyBox.setAlignment(Pos.TOP_LEFT);
        HBox bodyPaddingBox = new HBox(m_bodyBox);
        bodyPaddingBox.setPadding(new Insets(0, 5, 5 ,5));

        //Binding<Double> bodyHeightBinding = Bindings.createObjectBinding(()->bodyBox.layoutBoundsProperty().get().getHeight(), bodyBox.layoutBoundsProperty());
        
        m_toggleSwapBtn.prefHeightProperty().bind(m_bodyBox.heightProperty());

        layoutBox.getChildren().addAll(headerVBox, bodyPaddingBox);


        m_showSwap.addListener((obs,oldVal,newVal)->updateShowSwap());

        updateShowSwap();

        m_chartScroll.prefViewportWidthProperty().bind(layoutBox.widthProperty().subtract(45));
        m_chartScroll.prefViewportHeightProperty().bind(layoutBox.heightProperty().subtract(headerVBox.heightProperty()).subtract(10));


        //layoutBox.setPrefWidth(Double.MAX_VALUE);

        rangeHeight.bind(layoutBox.heightProperty().subtract(headerVBox.heightProperty()).subtract(65));

      
        
        m_chartRange.bottomVvalueProperty().addListener((obs,oldval,newval)->{
            if(m_chartRange.settingRangeProperty().get()){
                updateChart();
                
            }
        });

        m_chartRange.topVvalueProperty().addListener((obs,oldval,newval)->{
            if(m_chartRange.settingRangeProperty().get()){
                updateChart();
            }
        });


        m_currentControl.addListener((obs,oldval,newval)->{
            if(oldval != null){
                oldval.cancel();
            }
            if(newval != null){
                menuAreaBox.getChildren().add(newval.getControlBox());
            }else{
                menuAreaBox.getChildren().clear();
            }

        });


        m_chartRange.settingRangeProperty().addListener((obs,oldval,newval)->{
            if(newval){
                m_currentControl.set(m_chartRange);
            }else{
                if(m_currentControl.get() != null && m_currentControl.get().equals(m_chartRange)){
                    m_currentControl.set(null);
                }
            }
            createChart();
        });

        m_chartScroll.viewportBoundsProperty().addListener((obs,oldval,newval)->createChart());
        
        addErgoDexListener();

        
        String[] spans = TimeSpan.AVAILABLE_TIMESPANS;

        for (int i = 0; i < spans.length; i++) {

            String span = spans[i];
            TimeSpan timeSpan = new TimeSpan(span);
            String timeSpanName = timeSpan.getName();
            MenuItem menuItm = new MenuItem(timeSpanName);
            menuItm.setId("urlMenuItem");

            menuItm.setOnAction(action -> {
                setTimeSpan(timeSpan);
            });

            m_timeSpanBtn.getItems().add(menuItm);

        }




        m_invertListener = (obs,oldval,newval)->{
            
            invertBtn.setImage( new Image(newval ? "/assets/targetSwapped.png" : "/assets/targetStandard.png"));
            headingText.setText(m_marketData.getCurrentSymbol(newval));
            m_chartRange.reset();
            createChart();
        };

        isInvertProperty().addListener(m_invertListener);

        Binding<String> titleBinding = Bindings.createObjectBinding(()->{
            boolean isInvert = isInvert();
            String currentSymbol = m_marketData.getCurrentSymbol(isInvert);
            String number = isInvert() ? m_marketData.getInvertedLastPrice().toString() : m_marketData.getLastPrice().toString();
            int index = number.indexOf(".");
            String leftSide = index != -1 ? number.substring(0, index + 1) : number;
            String rightSide = index != -1 ?  number.substring(index + 1) : "";
            rightSide = leftSide.length() > 2 ? (rightSide.length() > 2 ? rightSide.substring(0,2) + "…" : rightSide)  : rightSide.length() > 5 ? rightSide.substring(0,5) + "…" :rightSide;
            return currentSymbol + " - " + leftSide + rightSide;
        }, isInvertProperty(), m_marketData.getLastUpdated());

        m_titleProperty.bind(titleBinding);

        getTabLabel().textProperty().bind(m_titleProperty);

    
    
       
        createChart();
    }


    public void getData(){
        JsonObject json = getNetworksData().getData("chartData", m_marketData.getId(), ErgoDexDataList.NETWORK_ID, ErgoDex.NETWORK_ID);

        openJson(json);
    }

    public void setTimeSpan(TimeSpan timeSpan){
        m_timeSpan = timeSpan != null ? timeSpan : new TimeSpan("30min");
        m_timeSpanBtn.setText(m_timeSpan.getName());
        createChart();
        delaySave();
    }

    public boolean getShowSwap(){
        return m_showSwap.get();
    }

    public void delaySave(){
        if(m_delayTimer != null){
            m_delayTimer = FxTimer.runLater(Duration.ofMillis(DELAY_MILLIS), ()->{
                m_delayTimer = null;
                save();
              
            });
        }
    }

    public void setShowSwap(boolean showSwap){
        m_showSwap.set(showSwap);
        delaySave();
    }

    public boolean getShowWallet(){
        return m_showWallet.get();
    }

    public void setShowWallet(boolean showWallet){
        m_showWallet.set(showWallet);
        delaySave();
    }

    public String getChartWalletId(){
        return m_defaultWalletId;
    }

    public void setChartWalletId(String id){
        m_defaultWalletId = id;
        delaySave();
    }

    private void openJson(JsonObject json){
        if(json != null){
            JsonElement timeSpanElement = json.get("timeSpan");
            JsonElement showSwapElement = json.get("showSwap");
            JsonElement showWalletElement = json.get("showWallet");
            JsonElement defaeultWalletIdElement = json.get("defaultWalletId");

            JsonObject timeSpanJson = timeSpanElement != null && timeSpanElement.isJsonObject() ? timeSpanElement.getAsJsonObject() : null;
 
            TimeSpan timeSpan = timeSpanJson != null ? new TimeSpan(timeSpanJson) : new TimeSpan("30min");
            boolean showSwap = showSwapElement != null && !showSwapElement.isJsonNull() ? showSwapElement.getAsBoolean() : true;
            boolean showWallet = showWalletElement != null && !showWalletElement.isJsonNull() ? showWalletElement.getAsBoolean() : true;
            String defaultWalletId = defaeultWalletIdElement == null ?  DEFAULT_ID : (defaeultWalletIdElement.isJsonNull() ? null : defaeultWalletIdElement.getAsString());

            m_timeSpan = timeSpan;
            m_showSwap.set(showSwap);
            m_showWallet.set(showWallet);
            m_defaultWalletId = defaultWalletId;
        }
    }

    private JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("defaultWalletId", m_defaultWalletId);
        json.addProperty("showSwap", m_showSwap.get());
        json.addProperty("showWallet", m_showWallet.get());
        json.add("timeSpan", m_timeSpan.getJsonObject());
        return json;
    }

    public void save(){
        getNetworksData().save("chartData", m_marketData.getId(), ErgoDexDataList.NETWORK_ID, ErgoDex.NETWORK_ID, getJsonObject());
    }

    public void addErgoDexListener(){
        
        String friendlyId = FriendlyId.createFriendlyId();

        ErgoDexChartView chartView = m_marketData.getSpectrumChartView().get();
        
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
                            createChart();  
                        
                        break;
                        case App.STOPPED:

                        break;
                    }  
                    
                }

                public void sendMessage(int code, long timestamp, String networkId, String msg){
                    
                }
    
                
            };
            
            
            chartView.addMsgListener(m_chartMsgInterface);
          
        }
        
    }

    
    public void createChart(){
        Bounds bounds = m_chartScroll.viewportBoundsProperty().get();
                          
        int viewPortHeight = (int) bounds.getHeight();
        int viewPortWidth = (int) bounds.getWidth();
        int maxBars =  (ErgoDexChartView.MAX_CHART_WIDTH / (m_cellWidth + m_cellPadding));

        TimeSpan timeSpan = m_timeSpan;
        long timestamp = System.currentTimeMillis();

        m_spectrumChartView.processData(
            isInvertProperty().get(), 
            maxBars, 
            timeSpan,
            timestamp,
             m_dataList.getErgoDex().getExecService(), 
             (onSucceeded)->{
                Object sourceValue = onSucceeded.getSource().getValue();
                if(sourceValue != null && sourceValue instanceof ErgoDexNumbers){
                    ErgoDexNumbers numbers = (ErgoDexNumbers) sourceValue;
                    
                    int size = numbers.dataLength();

                
                    if(size > 0){
                   
                        int totalCellWidth = m_cellWidth + m_cellPadding;
                        
                        int itemsTotalCellWidth = size * (totalCellWidth);

                        int scaleLabelLength = (numbers.getClose() +"").length();

                        int scaleColWidth =  (scaleLabelLength * m_zeroStringWidth )+ ErgoDexChartView.SCALE_COL_PADDING;
                        
                        

                        int width =Math.max(viewPortWidth, Math.max(itemsTotalCellWidth + scaleColWidth, ErgoDexChartView.MIN_CHART_WIDTH));
                        
                        int height = Math.min(ErgoDexChartView.MAX_CHART_HEIGHT, Math.max(viewPortHeight, ErgoDexChartView.MIN_CHART_HEIGHT));

                        boolean isNewImg = m_img == null || (m_img != null && (m_img.getWidth() != width || m_img.getHeight() != height));
                    
                        m_img = isNewImg ? new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB) : m_img;
                        m_g2d = isNewImg ? m_img.createGraphics() : m_g2d;
                       
                        m_numbers = numbers;
    
                        
                        ErgoDexChartView.updateBufferedImage(
                            m_img, 
                            m_g2d, 
                            m_labelFont, 
                            m_labelMetrics, 
                            numbers,
                            m_cellWidth, 
                            m_cellPadding, 
                            scaleColWidth,
                            m_amStringWidth,
                            timeSpan, 
                            m_chartRange
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
                       
                       
                        m_chartImageView.setImage(SwingFXUtils.toFXImage(m_img, m_wImg));
                        
                        if(isNewImg){
                            setChartScrollRight();
                            if(viewPortWidth > ErgoDexChartView.MAX_CHART_WIDTH){
                                m_chartImageView.setFitWidth(viewPortWidth);
                            }else{
                                
                                m_chartImageView.setFitWidth(m_img.getWidth());
                                
                            }
                            
                        }
                    }
                }
                
             }, 
             (onFailed)->{

             });
    }

    public void updateChart(){
        TimeSpan timeSpan = m_timeSpan;
            
        if(m_numbers != null){
            ErgoDexNumbers numbers = m_numbers;
            
            int size = numbers.dataLength();

        
            if(size > 0){
            
                int totalCellWidth = m_cellWidth + m_cellPadding;
                
                int itemsTotalCellWidth = size * (totalCellWidth);

                int scaleLabelLength = (numbers.getClose() +"").length();

                int scaleColWidth =  (scaleLabelLength * m_zeroStringWidth )+ ErgoDexChartView.SCALE_COL_PADDING;
                
                Bounds bounds = m_chartScroll.viewportBoundsProperty().get();
                int viewPortHeight = (int) bounds.getHeight();
                int viewPortWidth = (int) bounds.getWidth();
                
                int width = (itemsTotalCellWidth + scaleColWidth) < 300 ? 300 : itemsTotalCellWidth + scaleColWidth;
                int height = Math.min(ErgoDexChartView.MAX_CHART_HEIGHT, Math.max(viewPortHeight, ErgoDexChartView.MIN_CHART_HEIGHT));

                

                boolean isNewImg = m_img == null || (m_img != null && (m_img.getWidth() != width || m_img.getHeight() != height));
            
                m_img = isNewImg ? new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB) : m_img;
                m_g2d = isNewImg ? m_img.createGraphics() : m_g2d;
                
                
                ErgoDexChartView.updateBufferedImage(
                    m_img, 
                    m_g2d, 
                    m_labelFont, 
                    m_labelMetrics, 
                    numbers,
                    m_cellWidth, 
                    m_cellPadding, 
                    scaleColWidth,
                    m_amStringWidth,
                    timeSpan, 
                    m_chartRange
                );
                
                m_chartImageView.setImage(SwingFXUtils.toFXImage(m_img, m_wImg));
                if(isNewImg){
                    setChartScrollRight();
                    double w = Math.max(viewPortWidth, ErgoDexChartView.MAX_CHART_WIDTH);
                    if(w > viewPortHeight){
                        m_chartImageView.setFitWidth(w);
                    }else{
                        m_chartImageView.setFitHeight(viewPortHeight);
                    }
                }
            }
        }
    }

    public void setChartScrollRight(){
        Platform.runLater(()->m_chartScroll.setVvalue(chartScrollVvalue));
        Platform.runLater(()->m_chartScroll.setHvalue(chartScrollHvalue));
    }

    public void updateShowSwap(){

        boolean showSwap = m_showSwap.get();

        if( showSwap){
            m_toggleSwapBtn.setText("⯈");
            if(! m_bodyBox.getChildren().contains(m_ergoDexSwapBox)){
                m_ergoDexSwapBox = new ErgoDexSwapBox();
                m_bodyBox.getChildren().add(m_ergoDexSwapBox);
            }
        }else{
            m_toggleSwapBtn.setText("⯇");
            
            if(m_ergoDexSwapBox != null){
                m_ergoDexSwapBox.shutdown();
                m_ergoDexSwapBox = null;
                if(m_bodyBox.getChildren().contains(m_ergoDexSwapBox)){
                    m_bodyBox.getChildren().remove(m_ergoDexSwapBox);
                }
         
            }
        }
    }
    

    @Override
    public void close(){
        m_ergoDexSwapBox.shutdown();

        if(m_invertListener != null){
            isInvertProperty().removeListener(m_invertListener);
        }
        if(m_chartMsgInterface != null){
            m_spectrumChartView.removeMsgListener(m_chartMsgInterface);
            m_chartMsgInterface = null;
        } 
        m_img = null;
        m_g2d = null;
        super.close();
        
    }

    public class ErgoDexSwapBox extends VBox{
        public final static String WALLET_BOX = "Wallet";

        private VBox m_swapScrollContentBox;
        private ScrollPane m_swapBoxScroll;
        private ErgoDexWalletBox m_dexWallet;

        private ChangeListener<LocalDateTime> m_marketDataUpdateListener = null;

        public ErgoDexSwapBox(){
            super();
            setMinWidth(SWAP_BOX_MIN_WIDTH);
            VBox.setVgrow(this, Priority.ALWAYS);
    
            // SimpleObjectProperty<PriceAmount> spfPriceAmountObject = new SimpleObjectProperty<>(null);
            SimpleObjectProperty<PriceAmount> basePriceAmountObject = new SimpleObjectProperty<>(null); 
            SimpleObjectProperty<PriceAmount> quotePriceAmountObject = new SimpleObjectProperty<>(null);
    
            SimpleObjectProperty<BigDecimal> orderPriceObject = new SimpleObjectProperty< BigDecimal>();
    
            SimpleBooleanProperty isBuyObject = new SimpleBooleanProperty(true);
        //   SimpleBooleanProperty isSpfFeesObject = new SimpleBooleanProperty(false);
            SimpleObjectProperty<BigDecimal> currentAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
            SimpleObjectProperty<BigDecimal> currentVolume = new SimpleObjectProperty<>(BigDecimal.ZERO);
            
            SimpleStringProperty orderTypeStringObject = new SimpleStringProperty(MARKET_ORDER);
    

    
            Runnable updateOrderPriceObject = () ->{
                if(orderTypeStringObject.get() != null && orderTypeStringObject.get().equals(MARKET_ORDER)){
                    orderPriceObject.set(isInvert() ? m_marketData.getInvertedLastPrice() : m_marketData.getLastPrice());
                }
                
            };
    
            updateOrderPriceObject.run();


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
    
    

    
    
            isBuyObject.addListener((obs,oldval,newval)->{
                updateBuySellBtns.run();
    
            
            });
    
            isInvertProperty().addListener((obs,oldval, newval)->{
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
                
    
    
            m_marketDataUpdateListener = (obs,oldVal,newVal)->{
                String orderType = orderTypeStringObject.get() != null ? orderTypeStringObject.get() : "";
                
                switch(orderType){
                    case MARKET_ORDER:
                        updateOrderPriceObject.run();
                        updateVolumeFromAmount.run();
                        orderPriceStatusField.setText(Utils.formatTimeString(newVal));
                    break;
                }
    
                
                
            };
            
            m_marketData.getLastUpdated().addListener(m_marketDataUpdateListener);
           
   
            m_dexWallet = new ErgoDexWalletBox();
            
            m_swapScrollContentBox = new VBox(m_dexWallet);
           
            m_swapBoxScroll = new ScrollPane(m_swapScrollContentBox); 
            m_swapBoxScroll.prefViewportWidthProperty().bind(widthProperty()); 
            m_swapBoxScroll.prefViewportHeightProperty().bind(heightProperty().subtract(marketPaddingBox.heightProperty()).subtract(1));
        
            m_swapScrollContentBox.setPrefWidth(m_swapBoxScroll.viewportBoundsProperty().get().getWidth());
            m_swapScrollContentBox.setMinHeight(m_swapBoxScroll.viewportBoundsProperty().get().getHeight());

            m_swapBoxScroll.viewportBoundsProperty().addListener((obs,oldval,newval)->{
                double width = newval.getWidth();
                double height = newval.getHeight();
                m_swapScrollContentBox.setPrefWidth(width);
                m_swapScrollContentBox.setMinHeight(height);
            });

            getChildren().addAll( m_swapBoxScroll, marketPaddingBox);
                
    
             
        }



        public void shutdown(){
            if(m_marketDataUpdateListener != null){
                m_marketData.getLastUpdated().removeListener(m_marketDataUpdateListener);
            }
        }

        public class ErgoDexWalletBox extends VBox {

           

            private final String emptyAddressString = "[ click to unlock ]";;
            private final String walletBtnDefaultString = "[ select wallet ]";
            private SimpleObjectProperty<PriceAmount> m_ergoAmountProperty = new SimpleObjectProperty<>(null);
            private SimpleObjectProperty<PriceAmount> m_baseAmountProperty = new SimpleObjectProperty<>(null);
            private SimpleObjectProperty<PriceAmount> m_quoteAmountProperty = new SimpleObjectProperty<>(null);
    
           
         //   private SimpleBooleanProperty m_showAssets = new SimpleBooleanProperty(true);

            private NoteMsgInterface m_ergoNetworkMsgInterface = null;
       
    
            private MenuButton m_openWalletBtn;
            private HBox m_walletFieldBox;
    
            private Button m_clearWalletBtn;
    
 
            private MenuButton m_walletAdrMenu = null;
            private Button m_walletAdrUnlockBtn;
            private HBox m_walletAdrFieldBox;
            private Button m_walletCloseBtn;
            private HBox m_walletAdrBox;
            private VBox m_walletBodyVBox;
            private HBox m_assetsVBox;
            
            private ErgoWalletControl m_walletControl;
    
            public NoteInterface getErgoNetworkInterface(){
                return m_dataList.ergoInterfaceProperty().get();
            }
    
            public String getLocationId(){
                return m_dataList.getLocationId();
            }
    
            public ErgoDexWalletBox(){
                int colWidth = 100;
                int padding = 7;
                Insets rowPadding = new Insets(5,0,5,0);
                Insets rowSpacing = new Insets(0,0,7,0);
                double btnWidth = 257;

                m_walletControl = new ErgoWalletControl(getLocationId(), getErgoNetworkInterface());

                Text headingText = new Text("Ergo Wallet ");
                headingText.setFont(App.txtFont);
                headingText.setFill(App.txtColor);

                headingText.setMouseTransparent(true);

                HBox headingTextBox = new HBox(headingText);
                HBox.setHgrow(headingTextBox, Priority.ALWAYS);
                headingTextBox.setAlignment(Pos.CENTER_LEFT);

                TextField ergoAmountHeadingField = new TextField();
                ergoAmountHeadingField.setMouseTransparent(true);
                ergoAmountHeadingField.textProperty().bind(Bindings.createObjectBinding(()->{
                    String walletName = m_walletControl.walletNameProperty().get();
                    PriceAmount ergoAmount =  m_ergoAmountProperty.get();
                    NoteInterface ergoInterface = m_dataList.ergoInterfaceProperty().get();
                    return ergoAmount != null ? ergoAmount.getAmountString() : walletName != null ? "🔒 Locked" : ergoInterface != null ? "  🚫  " : "  ⛔  " ;
                }, m_ergoAmountProperty, m_walletControl.walletNameProperty(), m_dataList.ergoInterfaceProperty()));
                ergoAmountHeadingField.prefWidthProperty().bind(Bindings.createObjectBinding(()->{
                    double w = Utils.computeTextWidth(App.txtFont, ergoAmountHeadingField.textProperty().get()) + 20;
                    return w < 50 ? 50 : (w > 200 ? 200 : w);
                }, ergoAmountHeadingField.textProperty()));


                Text ergText= new Text("");
                ergText.setFont(App.defjaVuTxt);
                ergText.setFill(App.txtColor);
                ergText.textProperty().bind(Bindings.createObjectBinding(()->{
                    PriceAmount ergoAmount =  m_ergoAmountProperty.get();
                    return ergoAmount != null ? ErgoCurrency.FONT_SYMBOL : "";
                }, m_ergoAmountProperty));

                HBox ergoAmountHeadingFieldBox = new HBox(ergoAmountHeadingField);
                ergoAmountHeadingFieldBox.setPadding(new Insets(2, 0,0,0));

                HBox ergoAmountLabelBox = new HBox(ergoAmountHeadingFieldBox, ergText);
                ergoAmountLabelBox.setAlignment(Pos.CENTER_LEFT);
                ergoAmountLabelBox.setId("darkBox");
                ergoAmountLabelBox.setPadding(new Insets(2,10,2,0));

                Button showWalletBtn = new Button();

                showWalletBtn.setPadding(new Insets(0,7,0,0));
                showWalletBtn.setMouseTransparent(true);
                showWalletBtn.textProperty().bind(Bindings.createObjectBinding(()->{
                    return m_showWallet.get() ? "⏷" : "⏵";
                },m_showWallet));



                HBox headingBox = new HBox(showWalletBtn, headingTextBox, ergoAmountLabelBox);
                headingBox.setId("headingBox");
                headingBox.setPadding(new Insets(7));
                headingBox.setOnMouseClicked(e->setShowWallet(!getShowWallet()));
                headingBox.setAlignment(Pos.CENTER_LEFT);

                getChildren().add(headingBox);
               
               
                m_openWalletBtn = new MenuButton(walletBtnDefaultString);
               
                m_openWalletBtn.setId("arrowMenuButton");
                m_openWalletBtn.showingProperty().addListener((obs,oldval,newval)->{
                    if(newval){
                        updateWalletsMenu();
                    }
                });
 
                m_clearWalletBtn = new Button("☓");
                m_clearWalletBtn.setId("lblBtn");
                m_clearWalletBtn.setOnAction(e -> m_walletControl.clearWallet());


                m_walletFieldBox = new HBox(m_openWalletBtn);
                HBox.setHgrow(m_walletFieldBox, Priority.ALWAYS);
                m_walletFieldBox.setAlignment(Pos.CENTER_LEFT);
                m_walletFieldBox.setMaxHeight(18);
                m_walletFieldBox.setId("darkBox");
                m_walletFieldBox.setPadding(rowPadding);

                m_openWalletBtn.setPrefWidth(btnWidth);

                HBox clearWalletBtnBox = new HBox();
                VBox.setVgrow(clearWalletBtnBox, Priority.ALWAYS);
                clearWalletBtnBox.setId("darkBox");
                clearWalletBtnBox.setAlignment(Pos.CENTER);
    
                HBox selectWalletRowBox = new HBox( m_walletFieldBox, clearWalletBtnBox);
                selectWalletRowBox.setAlignment(Pos.CENTER_LEFT);
                selectWalletRowBox.setPadding(new Insets(0,0,7,0));

                m_walletBodyVBox = new VBox(selectWalletRowBox);
                m_walletBodyVBox.setId("bodyBox");
                m_walletBodyVBox.setPadding(new Insets(padding));

    
               
    
                m_walletAdrUnlockBtn = new Button(emptyAddressString);
                m_walletAdrUnlockBtn.setId("rowBtn");
                m_walletAdrUnlockBtn.setPadding(new Insets(0, 0, 0, 10));
                m_walletAdrUnlockBtn.setAlignment(Pos.CENTER);
                m_walletAdrUnlockBtn.setOnAction(e->{
                    if(m_walletControl.walletNameProperty().get() != null){
                        m_walletControl.connectToWallet();
                    }
                });

                m_walletAdrMenu = new MenuButton("");
                m_walletAdrMenu.setId("arrowMenuButton");
                m_walletAdrMenu.setPadding(rowPadding);
                m_walletAdrMenu.getItems().add(new MenuItem("- disabled -"));
                m_walletAdrMenu.showingProperty().addListener((obs,oldval,newval)->{
            
                    if(newval){
                        if(m_walletControl.currentAddress().get() != null){
                            updateAddressesMenu();
                        }
                    }
                });
    

                m_walletAdrFieldBox = new HBox(m_walletAdrUnlockBtn);
                HBox.setHgrow(m_walletAdrFieldBox, Priority.ALWAYS);
                m_walletAdrFieldBox.setId("darkBox");
                m_walletAdrFieldBox.setPadding(rowPadding);
                m_walletAdrFieldBox.setAlignment(Pos.CENTER);
                m_walletAdrMenu.setPrefWidth(btnWidth);
                m_walletAdrUnlockBtn.setPrefWidth(btnWidth);


                HBox closeBtnBox = new HBox();
                VBox.setVgrow(closeBtnBox,Priority.ALWAYS);
                closeBtnBox.setId("darkBox");
                closeBtnBox.setAlignment(Pos.CENTER);

                m_walletAdrBox = new HBox( m_walletAdrFieldBox, closeBtnBox);
                HBox.setHgrow(m_walletAdrBox, Priority.ALWAYS);
                m_walletAdrBox.setAlignment(Pos.CENTER_LEFT);
                m_walletAdrBox.setPadding(rowSpacing);
    
               
                
                m_walletCloseBtn = new Button("☓");
                m_walletCloseBtn.setId("lblBtn");
                m_walletCloseBtn.setOnAction(e -> {
                    m_walletControl.disconnectWallet();
                });

    
                Label baseAmountLabel = new Label(isInvert() ? m_marketData.getQuoteSymbol() : m_marketData.getBaseSymbol());
                baseAmountLabel.setMinWidth(colWidth);
                baseAmountLabel.setMaxWidth(colWidth);

                TextField baseAmountField = new TextField();
                HBox.setHgrow(baseAmountField, Priority.ALWAYS);
                baseAmountField.setId("formField");
                baseAmountField.setEditable(false);
                baseAmountField.setAlignment(Pos.CENTER_RIGHT);
                baseAmountField.textProperty().bind(Bindings.createObjectBinding(()->{
                    PriceAmount baseAmount = m_baseAmountProperty.get();
                    return baseAmount != null ? baseAmount.amountProperty().get() + "" : "Unavailable";
                }, m_baseAmountProperty));
        
                ImageView baseImgView = new ImageView(PriceCurrency.getBlankBgIcon(38, m_marketData.getBaseSymbol()));
                baseImgView.setPreserveRatio(true);

        
                StackPane baseAmountFieldBox = new StackPane(baseImgView, baseAmountField);
                baseAmountFieldBox.setId("darkBox");
                baseAmountFieldBox.setPadding(new Insets(0,3,0,0));
                baseAmountFieldBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(baseAmountFieldBox, Priority.ALWAYS);
        
                HBox baseAmountBox = new HBox(baseAmountFieldBox);
                baseAmountBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(baseAmountBox, Priority.ALWAYS);


                TextField quoteAmountField = new TextField();
                HBox.setHgrow(quoteAmountField, Priority.ALWAYS);
                quoteAmountField.setId("formField");
                quoteAmountField.setEditable(false);
                quoteAmountField.setAlignment(Pos.CENTER_RIGHT);
                quoteAmountField.textProperty().bind(Bindings.createObjectBinding(()->{
                    PriceAmount quoteAmount = m_quoteAmountProperty.get();
                    return quoteAmount != null ? quoteAmount.amountProperty().get() + "" : "Unavailable";
                }, m_quoteAmountProperty));
                
        
                ImageView quoteImgView = new ImageView(PriceCurrency.getBlankBgIcon(38, m_marketData.getQuoteSymbol()));
                quoteImgView.setPreserveRatio(true);


        
                StackPane quoteAmountFieldBox = new StackPane(quoteImgView, quoteAmountField);
                quoteAmountFieldBox.setId("darkBox");
                quoteAmountFieldBox.setPadding(new Insets(0,3,0,0));
                quoteAmountFieldBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(quoteAmountFieldBox, Priority.ALWAYS);
        
                HBox quoteAmountBox = new HBox(quoteAmountFieldBox);
                quoteAmountBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(quoteAmountBox, Priority.ALWAYS);

            
                m_assetsVBox = new HBox();
                Region assetSpacer = new Region();
                assetSpacer.setMinWidth(5);
                
                if(isInvert()){
                    m_assetsVBox.getChildren().addAll(quoteAmountBox, assetSpacer, baseAmountBox);
                }else{
                    m_assetsVBox.getChildren().addAll(baseAmountBox, assetSpacer, quoteAmountBox);
                }

                isInvertProperty().addListener((obs,oldval,newVal)->{
                    m_assetsVBox.getChildren().clear();
                    if(newVal){
                        m_assetsVBox.getChildren().addAll(quoteAmountBox, assetSpacer, baseAmountBox);
                    }else{
                        m_assetsVBox.getChildren().addAll(baseAmountBox, assetSpacer, quoteAmountBox);
                    }
                });

                m_walletControl.walletNameProperty().addListener((obs, oldVal, newVal) -> {
    
                    m_openWalletBtn.setText(newVal != null ? newVal : walletBtnDefaultString );

                    if(newVal != null){
                      

              
                        if(!  clearWalletBtnBox.getChildren().contains(m_clearWalletBtn)){
                            clearWalletBtnBox.getChildren().add(m_clearWalletBtn);
                        }
                        if(!m_walletBodyVBox.getChildren().contains(m_walletAdrBox)){
                            m_walletBodyVBox.getChildren().add(1, m_walletAdrBox);
                        }
                    }else{
                     
                        if( clearWalletBtnBox.getChildren().contains(m_clearWalletBtn)){
                            clearWalletBtnBox.getChildren().remove(m_clearWalletBtn);
                        }
                        if(m_walletBodyVBox.getChildren().contains(m_walletAdrBox)){
                            m_walletBodyVBox.getChildren().remove(m_walletAdrBox);
                        }
                    }
            
                });

                m_walletControl.balanceProperty().addListener((obs,oldval,newval)->{
                    if(newval != null){
                        ArrayList<PriceAmount> priceAmountList = AddressesData.getBalanceList(newval,true, ErgoDex.NETWORK_TYPE);
                        
                        m_ergoAmountProperty.set(AddressesData.getPriceAmountFromList(priceAmountList, ErgoCurrency.TOKEN_ID));
                        m_baseAmountProperty.set(AddressesData.getPriceAmountFromList(priceAmountList, m_marketData.getBaseId()));
                        m_quoteAmountProperty.set(AddressesData.getPriceAmountFromList(priceAmountList, m_marketData.getQuoteId()));
                    }else{
                        m_ergoAmountProperty.set(null);
                        m_baseAmountProperty.set(null);
                        m_quoteAmountProperty.set(null);
                    
                    }
                });
    
                m_walletControl.currentAddress().addListener((obs,oldval,newval)->{
                    if(newval != null){
                      

                        if(m_walletAdrFieldBox.getChildren().contains(m_walletAdrUnlockBtn)){
                            m_walletAdrFieldBox.getChildren().remove(m_walletAdrUnlockBtn);
                        }
                        if(!m_walletAdrFieldBox.getChildren().contains(m_walletAdrMenu)){
                            
                            m_walletAdrFieldBox.getChildren().add(0, m_walletAdrMenu);
                        }
                        if(!m_walletBodyVBox.getChildren().contains(m_assetsVBox)){
                            m_walletBodyVBox.getChildren().add(m_assetsVBox);
                        }

                        if(!closeBtnBox.getChildren().contains(m_walletCloseBtn)){
                            closeBtnBox.getChildren().add(m_walletCloseBtn);
                        }

                        
                        m_walletAdrMenu.setText(Utils.formatAddressString(newval, btnWidth, m_dataList.defaultCharacterSize()));

                    }else{
                    
                        m_walletAdrMenu.setText("");

                        if(m_walletAdrFieldBox.getChildren().contains(m_walletAdrMenu)){
                            
                            m_walletAdrFieldBox.getChildren().remove(m_walletAdrMenu);
                        }

                        if(!m_walletAdrFieldBox.getChildren().contains(m_walletAdrUnlockBtn)){
                            m_walletAdrFieldBox.getChildren().add(m_walletAdrUnlockBtn);
                        }
                      

                        if(m_walletBodyVBox.getChildren().contains(m_assetsVBox)){
                            m_walletBodyVBox.getChildren().remove(m_assetsVBox);
                        }  

                        if(closeBtnBox.getChildren().contains(m_walletCloseBtn)){
                            closeBtnBox.getChildren().remove(m_walletCloseBtn);
                        }
                    }
                });

  
                m_dataList.ergoInterfaceProperty().addListener((obs,oldval,newval)->{
                    m_walletControl.setErgoNetworkInterface(newval);
                 
                    if(oldval != null){
                        connectToErgoNetwork(false, oldval);
                    }
                    if(newval != null){
                        connectToErgoNetwork(true, newval);
                    }
                });
    
                connectToErgoNetwork(true, getErgoNetworkInterface());
                
                String defaultWalletId = getChartWalletId();
                if(defaultWalletId != null){
                    if(defaultWalletId.equals(DEFAULT_ID)){
                        m_walletControl.getDefaultWallet();
                    }else{
                        m_walletControl.setWalletInterface(defaultWalletId);
                    }
                }
               
                if(getShowWallet()){
                    getChildren().add(m_walletBodyVBox);
                }

                m_showWallet.addListener((obs,oldval,newval)->{
                    if(newval){
                        if(!getChildren().contains(m_walletBodyVBox)){
                            getChildren().add(m_walletBodyVBox);
                        }
                    }else{
                        if(getChildren().contains(m_walletBodyVBox)){
                            getChildren().remove(m_walletBodyVBox);
                        }
                    }
                });
                
            }
    
            public void connectToErgoNetwork(boolean connect, NoteInterface ergoNetworkInterface){
                
                if(connect && ergoNetworkInterface != null){
                    m_ergoNetworkMsgInterface = new NoteMsgInterface() {
                        private String msgId = FriendlyId.createFriendlyId();
                        
                        @Override
                        public String getId() {
                            return msgId;
                        }
    
                        @Override
                        public void sendMessage(int code, long timestamp, String networkId, String msg) {
                            switch(networkId){
                                case ErgoNetwork.WALLET_NETWORK:
                                    switch(code){
                                        case App.LIST_ITEM_REMOVED:
                                            m_walletControl.walletRemoved(msg);
                                        break;
                                        case App.LIST_DEFAULT_CHANGED:
                                            if(getChartWalletId() != null && getChartWalletId().equals(DEFAULT_ID)){
                                                m_walletControl.getDefaultWallet();
                                            }
                                        break;
                                    }
                        
                                break;
                            }
                        }
    
                        @Override
                        public void sendMessage(int code, long timestamp, String networkId, Number number) {
                            
                        }
                        
                    };
    
                    ergoNetworkInterface.addMsgListener(m_ergoNetworkMsgInterface);
                  
                }else{
    
                    if(ergoNetworkInterface != null && m_ergoNetworkMsgInterface != null){
                        ergoNetworkInterface.removeMsgListener(m_ergoNetworkMsgInterface);
                    }
                    m_ergoNetworkMsgInterface = null;
                }
 
            }

            public void updateWalletsMenu(){
                m_openWalletBtn.getItems().clear();
                NoteInterface ergoNetworkInterface = getErgoNetworkInterface();
                if(ergoNetworkInterface != null){

                    JsonArray walletIds = m_walletControl.getWallets();

                    if (walletIds != null) {
                        for (JsonElement element : walletIds) {
                            if (element != null && element instanceof JsonObject) {
                                JsonObject json = element.getAsJsonObject();
                                JsonElement nameElement = json.get("name");
                                JsonElement idElement = json.get("id");
                      

                             
                                if(nameElement != null && idElement != null && !nameElement.isJsonNull() && !idElement.isJsonNull()){
                                    String name = nameElement.getAsString();
                                    String id = idElement.getAsString();

                                    JsonElement defaultElement = json.get("default");
                                    boolean isDefault = defaultElement != null && !defaultElement.isJsonNull() ? defaultElement.getAsBoolean() : false;

                                    MenuItem walletItem = new MenuItem( (isDefault ? "* " :"  ") + name);
        
                                    walletItem.setOnAction(action -> {
                                    
                                        m_walletControl.setWalletInterface(id);
                                        setChartWalletId(id);
                                    });
        
                                    m_openWalletBtn.getItems().add(walletItem);
                                }
                            }
                        }
                        
                    }

                    MenuItem defaultWallet = new MenuItem("[ default wallet ]");
                    defaultWallet.setOnAction(e->{
                        m_walletControl.getDefaultWallet();
                        setChartWalletId(DEFAULT_ID);
                    });

                    MenuItem disableWallet = new MenuItem("[ disable ]");
                    disableWallet.setOnAction(e->{
                        m_walletControl.clearWallet();
                        setChartWalletId(null);
                    });

                    SeparatorMenuItem separatorItem = new SeparatorMenuItem();

                    MenuItem openErgoNetworkItem = new MenuItem("Manage wallets…");
                    openErgoNetworkItem.setOnAction(e->{
                        m_openWalletBtn.hide();
                        getNetworksData().openNetwork(ErgoNetwork.NETWORK_ID);
                    });

                    m_openWalletBtn.getItems().addAll(defaultWallet, disableWallet, separatorItem, openErgoNetworkItem);

                }else{
 

                    MenuItem manageNetworkItem = new MenuItem("Manage networks…");
                    manageNetworkItem.setOnAction(e->{
                        m_openWalletBtn.hide();
                        getNetworksData().openStatic(ManageNetworksTab.NAME);
                    });
                    m_openWalletBtn.getItems().add(manageNetworkItem);
                }
            }
         

            public void updateAddressesMenu(){

                JsonArray addressesArray = m_walletControl.getAddresses();
                m_walletAdrMenu.getItems().clear();

                if(addressesArray != null){
                    int size = addressesArray.size();
                    if(size > 0){
                        for(int i = 0; i < size ; i++){
                        
                            JsonElement addressJsonElement = addressesArray.get(i);
                            JsonObject addressJson = addressJsonElement != null && !addressJsonElement.isJsonNull() && addressJsonElement.isJsonObject() ? addressJsonElement.getAsJsonObject() : null;
                            JsonElement addressElement = addressJson != null ? addressJson.get("address") : null;
                            JsonElement nameElement = addressJson != null ? addressJson.get("name") : null;



                            String address = addressElement != null ? addressElement.getAsString() : null;
                          
                            if(address != null){
                                MenuItem addressItem = new MenuItem((nameElement != null && !nameElement.isJsonNull() ? nameElement.getAsString() + ": "  : "" ) + address);
                                addressItem.setOnAction(e->{
                                    m_walletControl.currentAddress().set(address);
                                    m_walletControl.updateBalance();
                                });
                                m_walletAdrMenu.getItems().add(addressItem);
                        }
                        
                        }
                    }
               

                }
            }

            public void shutdown(){
                connectToErgoNetwork(false, getErgoNetworkInterface());
                m_walletControl.shutdown();
            }
    
            
        }
        
    }

    public class ErgoDexStatsBox extends VBox {
       
        public ErgoDexStatsBox(){

    
            ImageView poolStatsIconView = new ImageView(m_dataList.getErgoDex().getSmallAppIcon());
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
                m_showPoolStats.set(!m_showPoolStats.get());
            });
            
    
            
            HBox poolStatsTopBar = new HBox(poolStatsIconView, poolStatsLbl, poolStatsSpacer, poolStatsToggleShowBtn);
            poolStatsTopBar.setAlignment(Pos.CENTER_LEFT);
            poolStatsTopBar.setPadding(new Insets(5,1, 5, 5));
            poolStatsTopBar.setId("networkTopBar");
    
            
    
            VBox poolStatsBox = new VBox();
    
            VBox poolStatsPaddingBox = new VBox(poolStatsTopBar, poolStatsBox);
            
            
            m_showPoolStats.addListener((obs,oldval,newval)->{
                
                poolStatsToggleShowBtn .setImage( newval ? new Image("/assets/caret-up-15.png") : new Image("/assets/caret-down-15.png"));   
            
                if(newval){
                    
                }else{
                
                }
            });
        }
    }

}
