package com.netnotes;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netnotes.NetworksData.ManageNetworksTab;
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
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
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
import java.util.ArrayList;

import org.ergoplatform.appkit.NetworkType;
import org.reactfx.util.FxTimer;

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
        m_toggleSwapBtn = new Button("⯈");
        m_toggleSwapBtn.setTextFill(App.txtColor);
        m_toggleSwapBtn.setFont( Font.font("OCR A Extended", 10));
        m_toggleSwapBtn.setPadding(new Insets(2,2,2,1));
        m_toggleSwapBtn.setId("barBtn");
        //toggleSwapBtn.setMinWidth(20);
        m_toggleSwapBtn.setOnAction(e->{
            m_showSwap.set(!m_showSwap.get());
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

        m_chartTimeSpanObject.addListener((obs,oldval,newval)->createChart());
        m_chartScroll.viewportBoundsProperty().addListener((obs,oldval,newval)->createChart());
        
        addErgoDexListener();

        
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

    public void updateShowSwap(){

        boolean showSwap = m_showSwap.get();

        if( showSwap){
            m_toggleSwapBtn.setText("⯈");
            if(m_ergoDexSwapBox == null){
                m_ergoDexSwapBox = new ErgoDexSwapBox();
                m_bodyBox.getChildren().add(m_ergoDexSwapBox);
            }
        }else{
            m_toggleSwapBtn.setText("⯇");
            
            if(m_ergoDexSwapBox != null){
                m_ergoDexSwapBox.shutdown();
                if(m_bodyBox.getChildren().contains(m_ergoDexSwapBox)){
                    m_bodyBox.getChildren().remove(m_ergoDexSwapBox);
                }
                m_ergoDexSwapBox = null;
                //  swapBoxObject.set(null);
            }
        }
    }
    

    @Override
    public void close(){
        m_showSwap.set(false);
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
        private SimpleStringProperty m_currentSwapContentBox = new SimpleStringProperty(WALLET_BOX);
        private ScrollPane m_swapBoxScroll;
        private ErgoDexWalletBox m_dexWallet;

        private ChangeListener<LocalDateTime> m_marketDataUpdateListener = null;

        public ErgoDexSwapBox(){
            super();
            setMinWidth(SWAP_BOX_MIN_WIDTH);
            VBox.setVgrow(this, Priority.ALWAYS);
            m_dexWallet = new ErgoDexWalletBox();
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
           
            Button walletBtn = new Button(WALLET_BOX);
             walletBtn.setId("selectedBtn");

            HBox swapScrollHeadingBox = new HBox(walletBtn);
            HBox.setHgrow(swapScrollHeadingBox, Priority.ALWAYS);
            swapScrollHeadingBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e->{
                m_currentSwapContentBox.set(WALLET_BOX);
            });

            VBox walletVBox = new VBox();
            HBox.setHgrow(walletVBox, Priority.ALWAYS);

            m_currentSwapContentBox.addListener((obs,oldVal,newVal)->setSwapContent(newVal != null ? newVal : WALLET_BOX));
            
            
            m_swapScrollContentBox = new VBox(m_dexWallet);
           
            m_swapBoxScroll = new ScrollPane(m_swapScrollContentBox); 
            m_swapBoxScroll.prefViewportWidthProperty().bind(widthProperty()); 
            m_swapBoxScroll.prefViewportHeightProperty().bind(heightProperty().subtract(swapScrollHeadingBox.heightProperty()).subtract(marketPaddingBox.heightProperty()).subtract(1));
        
            m_swapScrollContentBox.setPrefWidth(m_swapBoxScroll.viewportBoundsProperty().get().getWidth());
            m_swapScrollContentBox.setMinHeight(m_swapBoxScroll.viewportBoundsProperty().get().getHeight());

            m_swapBoxScroll.viewportBoundsProperty().addListener((obs,oldval,newval)->{
                double width = newval.getWidth();
                double height = newval.getHeight();
                m_swapScrollContentBox.setPrefWidth(width);
                m_swapScrollContentBox.setMinHeight(height);
            });

            getChildren().addAll(swapScrollHeadingBox, m_swapBoxScroll, marketPaddingBox);
                
    
             
        }

        private void setSwapContent(String value){
     
            switch(value){
                case WALLET_BOX:
                    if(!m_swapScrollContentBox.getChildren().contains(m_dexWallet)){
                        m_swapScrollContentBox.getChildren().clear();
                        m_swapScrollContentBox.getChildren().add(m_dexWallet);
                    }
                    break;
            }
        }

        public void shutdown(){
            if(m_marketDataUpdateListener != null){
                m_marketData.getLastUpdated().removeListener(m_marketDataUpdateListener);
            }
        }

        public class ErgoDexWalletBox extends VBox {
            private final String emptyAddressString = "[unlock]";;
            private final String walletBtnDefaultString = "[select wallet]";
            private SimpleObjectProperty<PriceAmount> m_ergoAmountProperty = new SimpleObjectProperty<>(null);
            private SimpleObjectProperty<PriceAmount> m_baseAmountProperty = new SimpleObjectProperty<>(null);
            private SimpleObjectProperty<PriceAmount> m_quoteAmountProperty = new SimpleObjectProperty<>(null);
    
           
            private SimpleBooleanProperty m_showAssets = new SimpleBooleanProperty(true);

            private NoteMsgInterface m_ergoNetworkMsgInterface = null;
       
    
            private MenuButton m_openWalletBtn;
            private HBox m_walletFieldBox;
    
            private Button m_clearWalletBtn;
            private VBox m_selectedAddressBox;
    
 
            private MenuButton m_walletAdrMenu = null;
            private HBox m_walletAdrFieldBox;
            private Button m_walletCloseBtn;
            private HBox m_walletAdrBox;
            private VBox m_walletBodyVBox;
            private ErgoWalletControl m_walletControl;
    
            public NoteInterface getErgoNetworkInterface(){
                return m_dataList.ergoNetworkProperty().get();
            }
    
            public String getLocationId(){
                return m_dataList.getLocationId();
            }
    
            public ErgoDexWalletBox(){
                int colWidth = 120;
    
                m_walletControl = new ErgoWalletControl(getLocationId(), m_dataList.ergoNetworkProperty().get());
    
                Label walletLabel = new Label("Wallet");
                walletLabel.setMinWidth(colWidth);
               
                m_openWalletBtn = new MenuButton(walletBtnDefaultString);
                m_openWalletBtn.setId("arrowMenuButton");
                m_openWalletBtn.showingProperty().addListener((obs,oldval,newval)->{
                    if(newval){
                        updateWalletsMenu();
                    }
                });

    
                m_walletFieldBox = new HBox(m_openWalletBtn);
                HBox.setHgrow(m_walletFieldBox, Priority.ALWAYS);
                m_walletFieldBox.setAlignment(Pos.CENTER_LEFT);
                m_walletFieldBox.setId("bodyBox");
                m_walletFieldBox.setPadding(new Insets(0, 1, 0, 0));
                m_walletFieldBox.setMaxHeight(18);
                m_openWalletBtn.prefWidthProperty().bind(m_walletFieldBox.widthProperty().subtract(1));

             
    
                HBox selectWalletRowBox = new HBox(walletLabel, m_walletFieldBox);
                selectWalletRowBox.setAlignment(Pos.CENTER_LEFT);
    
                m_clearWalletBtn = new Button("☓");
                m_clearWalletBtn.setId("lblBtn");
                m_clearWalletBtn.setOnAction(e -> m_walletControl.clearWallet());
    
    
                Label addressLabel = new Label("Address");
                addressLabel.setMinWidth(colWidth);
    
                

                m_walletAdrMenu = new MenuButton(emptyAddressString);
            
                m_walletAdrMenu.showingProperty().addListener((obs,oldval,newval)->{
            
                    if(newval){
                        if(m_walletControl.currentAddress().get() != null){
                            updateAddressesMenu();
                        }else{
                            if(m_walletControl.walletNameProperty().get() != null){
                                m_walletControl.connectToWallet();
                            }
                        }
                    }
                });
    

                m_walletAdrFieldBox = new HBox(m_walletAdrMenu);
                HBox.setHgrow(m_walletAdrFieldBox, Priority.ALWAYS);
                m_walletAdrFieldBox.setId("bodyBox");
                m_walletAdrMenu.prefWidthProperty().bind(m_walletAdrFieldBox.widthProperty().subtract(1));
    
                m_walletAdrBox = new HBox(addressLabel, m_walletAdrFieldBox);
                HBox.setHgrow(m_walletAdrBox, Priority.ALWAYS);
                m_walletAdrBox.setAlignment(Pos.CENTER_LEFT);
    
               
                
                m_walletCloseBtn = new Button("☓");
                m_walletCloseBtn.setId("lblBtn");
                m_walletCloseBtn.setOnAction(e -> {
                    m_walletControl.disconnectWallet();
                });
    
                getChildren().addAll(selectWalletRowBox, m_walletAdrBox);

                m_walletBodyVBox = new VBox();
    
    
                m_walletControl.currentAddress().addListener((obs,oldval,newval)->{
                    if(newval != null){
                        if(!m_walletAdrFieldBox.getChildren().contains( m_walletCloseBtn)){
                            m_walletAdrFieldBox.getChildren().add( m_walletCloseBtn);
                        }
                        m_walletAdrMenu.setText(newval);
            
                    }else{
                        if(m_walletAdrFieldBox.getChildren().contains( m_walletCloseBtn)){
                            m_walletAdrFieldBox.getChildren().remove( m_walletCloseBtn);
                        }
                        m_walletAdrMenu.setText(emptyAddressString);
                        
                    }
                });

    
                m_showAssets.addListener((obs,oldval,newval)->{
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
                
                m_walletControl.walletNameProperty().addListener((obs, oldVal, newVal) -> {
    
                    m_openWalletBtn.setText(newVal != null ? newVal : "[select wallet]" );

                    if(newVal != null){
                      
                        if(!m_walletAdrFieldBox.getChildren().contains(m_clearWalletBtn)){
                            m_walletAdrFieldBox.getChildren().add(m_clearWalletBtn);
                        }
                    }else{
                     
                        if(m_walletAdrFieldBox.getChildren().contains(m_clearWalletBtn)){
                            m_walletAdrFieldBox.getChildren().remove(m_clearWalletBtn);
                        }
                    }
            
                });
    
                m_dataList.ergoNetworkProperty().addListener((obs,oldval,newval)->{
                    m_walletControl.setErgoNetworkInterface(newval);
                    if(oldval != null){
                        connectToErgoNetwork(false, oldval);
                    }
                    if(newval != null){
                        connectToErgoNetwork(true, newval);
                    }
                });
    
                connectToErgoNetwork(true, getErgoNetworkInterface());

                m_walletControl.getDefaultWallet();
            
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
                                    if(code == App.LIST_ITEM_REMOVED){
                                        m_walletControl.walletRemoved(msg);
                                    }
                        
                                break;
                            }
                        }
    
                        @Override
                        public void sendMessage(int code, long timestamp, String networkId, Number number) {
                            
                        }
                        
                    };
    
                    ergoNetworkInterface.addMsgListener(m_ergoNetworkMsgInterface);
                    m_walletControl.getDefaultWallet();
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
    
                                String name = json.get("name").getAsString();
                                String id = json.get("id").getAsString();
    
                                MenuItem walletItem = new MenuItem(String.format("%-50s", " " + name));
    
                                walletItem.setOnAction(action -> {
                                
                                    m_walletControl.setWalletInterface(id);
                                });
    
                                m_openWalletBtn.getItems().add(walletItem);
                            }
                        }
                        
                    }

                    MenuItem disableWallet = new MenuItem("[none]");
                    disableWallet.setOnAction(e->{
                        m_walletControl.clearWallet();
                    });

                    SeparatorMenuItem separatorItem = new SeparatorMenuItem();

                    MenuItem openErgoNetworkItem = new MenuItem("Open (Ergo Network)…");
                    openErgoNetworkItem.setOnAction(e->{
                        m_openWalletBtn.hide();
                        getNetworksData().openNetwork(ErgoNetwork.NETWORK_ID);
                    });

                    m_openWalletBtn.getItems().addAll(disableWallet, separatorItem, openErgoNetworkItem);

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
                    for(int i = 0; i < size ; i++){
                       
                        JsonElement addressJsonElement = addressesArray.get(i);
                        JsonObject addressJson = addressJsonElement != null && !addressJsonElement.isJsonNull() && addressJsonElement.isJsonObject() ? addressJsonElement.getAsJsonObject() : null;
                        JsonElement addressElement = addressJson != null ? addressJson.get("address") : null;
                        String address = addressElement != null ? addressElement.getAsString() : null;
                        
                        if(address != null){
                            MenuItem addressItem = new MenuItem(address);
                            addressItem.setOnAction(e->{
                                m_walletControl.currentAddress().set(address);
                                m_walletControl.updateBalance();
                            });
                            m_walletAdrMenu.getItems().add(addressItem);
                       }
                       
                    }
                    MenuItem closeWalletItem = new MenuItem("[lock wallet]");
                    closeWalletItem.setOnAction(e->{
                        m_walletControl.disconnectWallet();
                    });
                    m_walletAdrMenu.getItems().add(closeWalletItem);
                }
            }

            public void shutdown(){
                connectToErgoNetwork(false, getErgoNetworkInterface());
                m_walletControl.shutdown();
            }
    
            
        }
        
    }

   

}
