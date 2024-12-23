package com.netnotes;

import com.devskiller.friendly_id.FriendlyId;
import com.utils.Utils;

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

import java.time.LocalDateTime;

public class ErgoDexChartTab extends ContentTab {
    private final ErgoDexDataList m_dataList;
    private final ErgoDexMarketData m_marketData;
    private final ErgoDexMarketItem m_marketItem;

    public final static String MARKET_ORDER = "Market order";
    public final static String LIMIT_ORDER = "limit order";
    public final static double SWAP_BOX_MIN_WIDTH = 300;

    private ScrollPane m_chartScroll;
    private int m_cellWidth = 20;
    private int m_cellPadding = 3;
    private SimpleObjectProperty<TimeSpan> m_chartTimeSpanObject = new SimpleObjectProperty<>(new TimeSpan("30min"));
    private ErgoDexChartView m_spectrumChartView;
    private java.awt.Font m_labelFont;
    private FontMetrics m_labelMetrics;
    private int m_amStringWidth;
    private int m_zeroStringWidth;
    private RangeBar m_chartRange;
    private ImageView m_chartImageView;
    private ChangeListener<Boolean> m_invertListener = null;

    private SimpleBooleanProperty m_showSwap = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty shutdownSwap;
  
    private BufferedImage m_img = null;
    private Graphics2D m_g2d = null;
    private WritableImage m_wImg = null;
    private ErgoDexNumbers m_numbers = null;
    private NoteMsgInterface m_chartMsgInterface;

    private final double chartScrollVvalue = 1;
    private final double chartScrollHvalue = 1;

    private SimpleObjectProperty<ControlInterface> m_currentControl = new SimpleObjectProperty<>(null);
    private SimpleStringProperty m_titleProperty;

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
        shutdownSwap = new SimpleBooleanProperty(false);
        



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
    




        MenuButton timeSpanBtn = new MenuButton(m_chartTimeSpanObject.get().getName());
        timeSpanBtn.setFont(App.txtFont);

        m_chartTimeSpanObject.addListener((obs,oldval,newval)->{
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
        


        m_chartRange = new RangeBar(rangeWidth, rangeHeight, getNetworksData().getExecService());
        m_chartImageView = new ImageView();
        m_chartImageView.setPreserveRatio(true);
        m_chartScroll = new ScrollPane(m_chartImageView);
        
        headingSpacerL.prefWidthProperty().bind(headingBox.widthProperty().subtract(timeSpanBtn.widthProperty().divide(2)).subtract(headingText.layoutBoundsProperty().get().getWidth()).divide(2));
        

        Region headingPaddingRegion = new Region();
        headingPaddingRegion.setMinHeight(5);
        headingPaddingRegion.setPrefHeight(5);

        VBox paddingBox = new VBox(menuBar, headingPaddingRegion, headingBox);
        paddingBox.setPadding(new Insets(0, 5, 0, 5));

        VBox headerVBox = new VBox(paddingBox);
        m_chartScroll.setPadding(new Insets(0, 0, 0, 0));

        setChartRangeItem.setOnAction((e)->m_chartRange.toggleSettingRange());



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

        
        HBox bodyBox = new HBox(m_chartRange, m_chartScroll, swapButtonBox);
        bodyBox.setId("bodyBox");
        bodyBox.setAlignment(Pos.TOP_LEFT);
        HBox bodyPaddingBox = new HBox(bodyBox);
        bodyPaddingBox.setPadding(new Insets(0, 5, 5 ,5));

        //Binding<Double> bodyHeightBinding = Bindings.createObjectBinding(()->bodyBox.layoutBoundsProperty().get().getHeight(), bodyBox.layoutBoundsProperty());
        SimpleObjectProperty<VBox> swapBoxObject = new SimpleObjectProperty<>(null);


        toggleSwapBtn.prefHeightProperty().bind(bodyBox.heightProperty());




        layoutBox.getChildren().addAll(headerVBox, bodyPaddingBox);


        
        
        
        Runnable updateShowSwap = ()->{

            boolean showSwap = m_showSwap.get();

            if( showSwap){
                toggleSwapBtn.setText("⯈");
                
                if(shutdownSwap.get()){
                    shutdownSwap.set(false);
                    swapBoxObject.set( getSwapBox(getScene()));
                }else{
                    if(swapBoxObject.get() == null){
                        swapBoxObject.set( getSwapBox(getScene()));    
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

        m_chartScroll.prefViewportWidthProperty().bind(layoutBox.widthProperty().subtract(45));
        m_chartScroll.prefViewportHeightProperty().bind(layoutBox.heightProperty().subtract(headerVBox.heightProperty()).subtract(10));

        rangeHeight.bind(layoutBox.heightProperty().subtract(headerVBox.heightProperty()).subtract(65));

        
        

        
        ChangeListener<Bounds> boundsChangeListener = (obs,oldval,newval)->{
        
            createChart();

        };
    
        
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

        m_chartTimeSpanObject.addListener((obs,oldval,newval)->createChart());

        
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
            m_chartScroll.viewportBoundsProperty().addListener(boundsChangeListener);
        }
        
    
    

    
    
        
        
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

                    m_chartTimeSpanObject.set((TimeSpan)item);
            
                    
                }

            });

            timeSpanBtn.getItems().add(menuItm);

        }

        timeSpanBtn.textProperty().addListener((obs, oldVal, newVal) -> {
            Object objData = timeSpanBtn.getUserData();

            if (newVal != null && !newVal.equals(m_chartTimeSpanObject.get().getName()) && objData != null && objData instanceof TimeSpan) {

                m_chartTimeSpanObject.set((TimeSpan) objData);
                
                setChartScrollRight();
                // chartView.reset();
                // setCandles.run();
    
            }
        });
        



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

    
    public void createChart(){
        Bounds bounds = m_chartScroll.viewportBoundsProperty().get();
                          
        int viewPortHeight = (int) bounds.getHeight();
        int viewPortWidth = (int) bounds.getWidth();
        int maxBars =  (ErgoDexChartView.MAX_CHART_WIDTH / (m_cellWidth + m_cellPadding));

        TimeSpan timeSpan = m_chartTimeSpanObject.get();
        long timestamp = System.currentTimeMillis();

        m_spectrumChartView.processData(
            isInvertProperty().get(), 
            maxBars, 
            m_chartTimeSpanObject.get(),
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
        TimeSpan timeSpan = m_chartTimeSpanObject.get();
            
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

    
    public VBox getSwapBox(Scene scene){

      

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

    @Override
    public void close(){
        shutdownSwap.set(true);
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
        
        try {
            Files.writeString(App.logFile.toPath(), "tabCLose\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
    }

}
