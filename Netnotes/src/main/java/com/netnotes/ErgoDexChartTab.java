package com.netnotes;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.netnotes.NetworksData.ManageNetworksTab;
import com.utils.Utils;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
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
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
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
import java.net.MalformedURLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.Future;

import org.reactfx.util.FxTimer;

public class ErgoDexChartTab extends ContentTab {
    private final ErgoDexDataList m_dataList;
    private final ErgoDexMarketData m_marketData;
    private final ErgoDexMarketItem m_marketItem;

    public final static String MARKET_ORDER = "Market order";
    public final static String LIMIT_ORDER = "limit order";
    public final static double SWAP_BOX_MIN_WIDTH = 300;
    public final static String DEFAULT_ID = "DEFAULT_ID";
    public final static double BG_ICON_HEIGHT = 38;


    private ScrollPane m_chartScroll = null;
    private int m_cellWidth = 20;
    private int m_cellPadding = 3;
   
    private ErgoDexChartView m_chartView = null;
    private java.awt.Font m_labelFont = null;
    private FontMetrics m_labelMetrics = null;
    private int m_amStringWidth = 10;
    private int m_zeroStringWidth = 25;
    private RangeBar m_chartRange = null;
    private ImageView m_chartImageView = null;
    private ChangeListener<Boolean> m_invertListener = null;
    private MenuButton m_timeSpanBtn = null;

    private SimpleDoubleProperty m_rangeWidth = null;
    private SimpleDoubleProperty m_rangeHeight = null;
    private SimpleDoubleProperty m_chartScrollWidth = null;
    private SimpleDoubleProperty m_chartScrollHeight = null;
   
  
    private BufferedImage m_img = null;
    private Graphics2D m_g2d = null;
    private WritableImage m_wImg = null;
    private ErgoDexNumbers m_numbers = null;
    private NoteMsgInterface m_chartMsgInterface = null;
    private ChangeListener<Boolean> m_maximizeListener = null;

    private final double chartScrollVvalue = 1;
    private final double chartScrollHvalue = 1;

    private SimpleObjectProperty<ControlInterface> m_currentControl = new SimpleObjectProperty<>(null);
    private SimpleStringProperty m_titleProperty;

    private ErgoDexSwapBox m_ergoDexSwapBox = null;
    private Button m_toggleSwapBtn = null;
    private HBox m_bodyBox = null;
    private VBox m_layoutBox = null;
    private VBox m_chartTabBox = null;
    private HBox m_chartBox = null;

    private boolean m_showSwap = true;
    private SimpleBooleanProperty m_showWallet = new SimpleBooleanProperty(true);
    private SimpleBooleanProperty m_showPoolStats = new SimpleBooleanProperty(true);
    private SimpleBooleanProperty m_showSwapFeesBox = new SimpleBooleanProperty(false);

    private TimeSpan m_timeSpan = new TimeSpan("30min");
    private SimpleObjectProperty<BigDecimal> m_slippageTolerance = new SimpleObjectProperty<>( BigDecimal.valueOf(0.03));
    private SimpleObjectProperty<PriceAmount> m_maxExFee = new SimpleObjectProperty<>( ErgoDex.SWAP_MIN_MAX_EXEC_FEE);
    

  
    private SimpleObjectProperty<PriceAmount> m_networkFee = new SimpleObjectProperty<>(ErgoDex.NETWORK_MIN_FEE);
    private SimpleObjectProperty<PriceAmount> m_minExFee = new SimpleObjectProperty<>(ErgoDex.SWAP_MIN_EXECUTION_FEE);

    private SimpleStringProperty m_lastPriceProperty = new SimpleStringProperty("");

    private SimpleObjectProperty<LocalDateTime> m_priceLastChecked = new SimpleObjectProperty<>(LocalDateTime.now());
    private String m_defaultWalletId = DEFAULT_ID;
   
    public boolean isInvert(){
        return m_marketItem.isInvert();
    }

    public NetworksData getNetworksData(){
        return m_marketItem.getNetworksData();
    }

    public Scene getScene(){
        return m_marketItem.getScene();
    }

    public ReadOnlyBooleanProperty isInvertProperty(){
        return m_marketItem.isInvertProperty();
    }

   

    public String getNFT(){
        return m_marketData.getPoolId();
    }



    public ErgoDexChartTab(String id, Image logo, String title,  VBox layoutBox,  ErgoDexDataList dataList, ErgoDexMarketData marketData, ErgoDexMarketItem marketItem){
        super(id, ErgoDex.NETWORK_ID, logo, title , layoutBox);
        m_dataList = dataList;
        m_marketData = marketData;
        m_marketItem = marketItem;
        m_titleProperty = new SimpleStringProperty(title);
        m_labelFont = m_dataList.getLabelFont();
        m_labelMetrics = m_dataList.getLabelMetrics();
        m_amStringWidth = m_labelMetrics.stringWidth(" a.m. ");
        m_zeroStringWidth = m_labelMetrics.stringWidth("0");
        m_chartView = m_marketData.getChartView();

        m_rangeWidth = new SimpleDoubleProperty(12);
        m_rangeHeight = new SimpleDoubleProperty(100);
       
        m_layoutBox = layoutBox;

        m_chartTabBox = new VBox();
        m_chartTabBox.setAlignment(Pos.CENTER);
        
        m_chartBox = new HBox();
        m_chartBox.setAlignment(Pos.CENTER);
        m_chartBox.setId("darkBox");
        setLoadingBox();
        
        getData((onSucceeded)->{
            Object obj = onSucceeded.getSource().getValue();
            JsonObject json = obj != null && obj instanceof JsonObject ? (JsonObject) obj : null;
            
            openJson(json); 
            
            initLayout();
        });

        
    }


    public void getData(EventHandler<WorkerStateEvent> onSucceeded){
        getNetworksData().getData("chartData", m_marketData.getId(), ErgoDexDataList.NETWORK_ID, ErgoDex.NETWORK_ID, onSucceeded);
    }


    private void setLoadingBox(){
        ImageView imgView = new ImageView(m_dataList.getErgoDex().getAppIcon());
        imgView.setPreserveRatio(true);
        imgView.setFitWidth(150);

        Button loadingBtn = new Button(ErgoDex.NAME);
        loadingBtn.setTextFill(Color.WHITE);
        loadingBtn.setGraphicTextGap(30);
        loadingBtn.setId("startImageBtn");
        loadingBtn.setGraphicTextGap(15);
        loadingBtn.setGraphic(imgView);
        loadingBtn.setContentDisplay(ContentDisplay.TOP);
        loadingBtn.setPadding(new Insets(0,0,30,0));

        ProgressBar progressBar = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.setPrefWidth(200);

        HBox progressBarBox = new HBox(progressBar);
        HBox.setHgrow(progressBarBox,Priority.ALWAYS);
        progressBarBox.setAlignment(Pos.CENTER);

        VBox imageBox = new VBox(loadingBtn, progressBarBox);
        imageBox.setId("transparentColor");
        imageBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(imageBox, Priority.ALWAYS);
        

        Label statusLabel = new Label("Starting...");
        statusLabel.setId("italicFont");
        
        progressBarBox.setPadding(new Insets(0,0,0,0));

        VBox statusLabelBox = new VBox( statusLabel);
        HBox.setHgrow(statusLabelBox, Priority.ALWAYS);
        statusLabelBox.setAlignment(Pos.CENTER);

        imageBox.prefHeightProperty().bind(m_layoutBox.heightProperty().subtract(statusLabelBox.heightProperty()).subtract(5));

        VBox loadingBox = new VBox(imageBox, statusLabelBox);
        loadingBox.setAlignment(Pos.CENTER);

        m_layoutBox.getChildren().clear();
        m_layoutBox.getChildren().add(loadingBox);
    }

    private void initLayout(){

        initChart( m_timeSpan);

        if(!m_marketData.isPool() || m_marketData.getChartView() == null){
            Alert a = new Alert(AlertType.NONE, "Price history unavailable.", ButtonType.OK);
            a.setHeaderText("Notice");
            a.setTitle("Notice: Price history unavailable");
            a.showAndWait();
            return;
        }
  

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

       
        Text headingText = new Text(m_marketData.getCurrentSymbol(isInvert()));
        headingText.setFont(App.txtFont);
        headingText.setFill(App.txtColor);


        m_timeSpanBtn = new MenuButton(m_timeSpan.getName() + " ");
        m_timeSpanBtn.setId("arrowMenuButton");
        
      
      
        
        Region headingBoxSpacerR = new Region();
        HBox.setHgrow(headingBoxSpacerR,Priority.ALWAYS);
        Region headingSpacerL = new Region();
        HBox.setHgrow(headingSpacerL,Priority.ALWAYS);

        Text dashText = new Text("  - ");
        dashText.setFont(App.txtFont);
        dashText.setFill(App.txtColor);

        HBox headingCenterBox = new HBox( headingText, dashText, m_timeSpanBtn);
        headingCenterBox.setAlignment(Pos.CENTER);
        headingCenterBox.setPadding(new Insets(5, 0, 5, 0));

        HBox headingBox = new HBox(headingSpacerL, headingCenterBox, headingBoxSpacerR);
        headingBox.prefHeight(40);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);

        headingBox.setId("headingBox");
        
        m_chartRange = new RangeBar(m_rangeWidth, m_rangeHeight, getNetworksData().getExecService());
        m_chartRange.setId("hand");

        m_chartImageView = new ImageView();
        m_chartImageView.setPreserveRatio(true);

        m_chartScroll = new ScrollPane(m_chartBox);        
        
  //      headingSpacerL.prefWidthProperty().bind(headingBox.widthProperty().subtract(m_timeSpanBtn.widthProperty().divide(2)).subtract(headingText.widthProperty()).divide(2));
        
        Region headingPaddingRegion = new Region();
        headingPaddingRegion.setMinHeight(5);
        headingPaddingRegion.setPrefHeight(5);

        VBox paddingBox = new VBox(menuBar, headingPaddingRegion, headingBox);

        VBox headerVBox = new VBox(paddingBox);

        m_chartScroll.setPadding(new Insets(0, 0, 0, 0));

        setChartRangeItem.setOnAction((e)->m_chartRange.toggleSettingRange());

                    //⯈🗘
        m_toggleSwapBtn = new Button("⯈");
        m_toggleSwapBtn.setTextFill(App.txtColor);
        m_toggleSwapBtn.setFont( Font.font("OCR A Extended", 10));
        m_toggleSwapBtn.setPadding(new Insets(2,2,2,1));
        m_toggleSwapBtn.setId("barBtn");
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

        HBox chartRangeBox = new HBox(m_chartRange);

        m_bodyBox = new HBox(chartRangeBox, m_chartScroll, swapButtonBox);
        m_bodyBox.setId("bodyBox");
        m_bodyBox.setAlignment(Pos.TOP_LEFT);
        HBox bodyPaddingBox = new HBox(m_bodyBox);

        //Binding<Double> bodyHeightBinding = Bindings.createObjectBinding(()->bodyBox.layoutBoundsProperty().get().getHeight(), bodyBox.layoutBoundsProperty());
        VBox headerPaddingBox = new VBox(headerVBox);
        HBox.setHgrow(headerPaddingBox, Priority.ALWAYS);

        m_toggleSwapBtn.prefHeightProperty().bind(m_bodyBox.heightProperty());

        m_chartTabBox.getChildren().clear();
        m_chartTabBox.getChildren().addAll(headerPaddingBox, bodyPaddingBox);


        m_chartScrollWidth = new SimpleDoubleProperty();
        m_chartScrollWidth.bind(getNetworksData().getContentTabs().bodyWidthProperty().subtract(chartRangeBox.widthProperty()).subtract(swapButtonBox.widthProperty()).subtract(1));

        m_chartScrollHeight = new SimpleDoubleProperty();
        m_chartScrollHeight.bind(getNetworksData().getContentTabs().bodyHeightProperty().subtract(headerPaddingBox.heightProperty()).subtract(11));

        m_chartScroll.prefViewportWidthProperty().bind(m_chartScrollWidth);
        m_chartScroll.prefViewportHeightProperty().bind(m_chartScrollHeight);

        m_chartBox.minWidthProperty().bind(m_chartScrollWidth.subtract(1));
        m_chartBox.minHeightProperty().bind(m_chartScrollHeight.subtract(App.VIEWPORT_HEIGHT_OFFSET));

        m_rangeHeight.bind(getNetworksData().getContentTabs().bodyHeightProperty().subtract(headerPaddingBox.heightProperty()).subtract(65));
        
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
        
        addErgoDexListener();

        
        String[] spans = TimeSpan.AVAILABLE_TIMESPANS;

        for (int i = 0; i < spans.length; i++) {

            String span = spans[i];
            TimeSpan tS = new TimeSpan(span);
            String timeSpanName = tS.getName();
            MenuItem menuItm = new MenuItem(timeSpanName);
            menuItm.setId("urlMenuItem");

            menuItm.setOnAction(action -> {
                setTimeSpan(tS);
             
            });

            m_timeSpanBtn.getItems().add(menuItm);

        }

        m_invertListener = (obs,oldval,newval)->{
            
            invertBtn.setImage( new Image(newval ? "/assets/targetSwapped.png" : "/assets/targetStandard.png"));
            headingText.setText(m_marketData.getCurrentSymbol(newval));

            m_chartRange.reset();
           
            createChart();
            m_ergoDexSwapBox.updateInvert(newval);
        };

        isInvertProperty().addListener(m_invertListener);

        Binding<String> titleBinding = Bindings.createObjectBinding(()->{
            boolean isInvert = isInvert();
            String currentSymbol = m_marketData.getCurrentSymbol(isInvert);
            String number = m_lastPriceProperty.get();
            int index = number.indexOf(".");
            String leftSide = index != -1 ? number.substring(0, index + 1) : number;
            String rightSide = index != -1 ?  number.substring(index + 1) : "";
            rightSide = leftSide.length() > 2 ? (rightSide.length() > 2 ? rightSide.substring(0,2) + "…" : rightSide)  : rightSide.length() > 5 ? rightSide.substring(0,5) + "…" :rightSide;
            return currentSymbol + " - " + leftSide + rightSide;
        }, m_lastPriceProperty);

        m_titleProperty.bind(titleBinding);

        getTabLabel().textProperty().bind(m_titleProperty);

        
        updateShowSwap(m_showSwap);
   

        FxTimer.runLater(Duration.ofMillis(200), ()->{
            completeLoading();
            FxTimer.runLater(Duration.ofMillis(200), ()->{
                if(m_numbers != null){
                    
                    updateChart();
                }else{
                    
                    createChart();
                }

                m_chartScrollHeight.addListener((obs,oldval,newval)->{
                    if(m_numbers != null){
                        
                        updateChart();
                        setChartScrollRight();
                    }
                });
            });
        });

        m_maximizeListener = (obs,oldval,newval)->FxTimer.runLater(Duration.ofMillis(200), ()->{
           
            updateChart();
            setChartScrollRight();
        });

    
        getNetworksData().getStage().maximizedProperty().addListener(m_maximizeListener);
    
    }
   

    private void completeLoading(){
        if(!m_layoutBox.getChildren().contains(m_chartTabBox)){
            m_layoutBox.getChildren().clear();
            m_layoutBox.getChildren().add(m_chartTabBox);

            m_chartBox.getChildren().clear();
            m_chartBox.setAlignment(Pos.CENTER);

            ProgressBar chartProgressBar = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
            chartProgressBar.setPrefWidth(200);
            m_chartBox.getChildren().add(chartProgressBar);
            setChartScrollRight();
        }
    }

    public void setTimeSpan(TimeSpan timeSpan){
        if(timeSpan != null){
            m_timeSpan = timeSpan;
            m_timeSpanBtn.setText(m_timeSpan.getName() + " ");
           
            createChart();
            save();
        }
    }

    public boolean getShowSwap(){
        return m_showSwap;
    }


 
    public void setShowSwap(boolean showSwap){
        m_showSwap = showSwap;
        updateShowSwap(m_showSwap);
        save();
    }

    public boolean getShowWallet(){
        return m_showWallet.get();
    }

    public void setShowWallet(boolean showWallet){
        m_showWallet.set(showWallet);
        save();
    }

    public String getChartWalletId(){
        return m_defaultWalletId;
    }

    public void setChartWalletId(String id){
        m_defaultWalletId = id;
        save();
    }

    public String getPoolId(){
        return m_marketData.getPoolId();
    }

    public String getNetworkTypeString(){
        return ErgoDex.NETWORK_TYPE.toString();
    }



   

    private void openJson(JsonObject json){
        if(json != null){
            JsonElement timeSpanElement = json.get("timeSpan");
            JsonElement slippageToleranceElement = json.get("slippageTolerance");
            JsonElement maxExFeeElement = json.get("maxExFee");
            JsonElement showSwapElement = json.get("showSwap");
            JsonElement showWalletElement = json.get("showWallet");
            JsonElement defaeultWalletIdElement = json.get("defaultWalletId");
            JsonElement showSwapSettingsElement = json.get("showSwapSettings");
            JsonElement networkfeeElement = json.get("networkfee");

            JsonObject timeSpanJson = timeSpanElement != null && timeSpanElement.isJsonObject() ? timeSpanElement.getAsJsonObject() : null;
 
            TimeSpan timeSpan = timeSpanJson != null ? new TimeSpan(timeSpanJson) : new TimeSpan("30min");
            boolean showSwap = showSwapElement != null && !showSwapElement.isJsonNull() ? showSwapElement.getAsBoolean() : true;
            boolean showWallet = showWalletElement != null && !showWalletElement.isJsonNull() ? showWalletElement.getAsBoolean() : true;
            String defaultWalletId = defaeultWalletIdElement == null ?  DEFAULT_ID : (defaeultWalletIdElement.isJsonNull() ? null : defaeultWalletIdElement.getAsString());
            BigDecimal slippageTolerance = slippageToleranceElement != null ? slippageToleranceElement.getAsBigDecimal() : ErgoDex.MIN_SLIPPAGE_TOLERANCE;
            boolean showSwapSettings = showSwapSettingsElement != null ? showSwapSettingsElement.getAsBoolean() : false;
            ErgoAmount maxExFee = maxExFeeElement != null ? new ErgoAmount(maxExFeeElement.getAsBigDecimal(), ErgoDex.NETWORK_TYPE) :  ErgoDex.SWAP_MIN_MAX_EXEC_FEE;
            ErgoAmount networkfee = networkfeeElement != null ? new ErgoAmount(networkfeeElement.getAsBigDecimal(), ErgoDex.NETWORK_TYPE) : ErgoDex.NETWORK_MIN_FEE;

            m_slippageTolerance.set(slippageTolerance);
            m_maxExFee.set(maxExFee.getBigDecimalAmount().compareTo(ErgoDex.SWAP_MIN_MAX_EXEC_FEE.getBigDecimalAmount()) == -1 ? ErgoDex.SWAP_MIN_MAX_EXEC_FEE : maxExFee);
            m_networkFee.set(networkfee);
            m_showSwapFeesBox.set(showSwapSettings);
            m_timeSpan = timeSpan;
            m_showSwap = showSwap;
            m_showWallet.set(showWallet);
            m_defaultWalletId = defaultWalletId;
        }
    }

    private JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("defaultWalletId", m_defaultWalletId);
        json.addProperty("showSwap", m_showSwap);
        json.addProperty("showSwapSettings", m_showSwapFeesBox.get());
        json.addProperty("showWallet", m_showWallet.get());
        json.addProperty("slippageTolerance", m_slippageTolerance.get());
        json.addProperty("maxExFee", m_maxExFee.get().getBigDecimalAmount());
        json.addProperty("networkfee", m_networkFee.get().getBigDecimalAmount());
        json.add("timeSpan", m_timeSpan.getJsonObject());
        return json;
    }

    public void save(){
        getNetworksData().save("chartData", m_marketData.getId(), ErgoDexDataList.NETWORK_ID, ErgoDex.NETWORK_ID, getJsonObject());
    }

    public PriceAmount getLastPriceAmount(){
        boolean isInvert = isInvert();
        ErgoDexPrice dexPrice = m_chartView.getLastDexPrice();
        
        PriceCurrency quoteCurrency = isInvert ? m_marketData.getBaseCurrency() : m_marketData.getQuoteCurrency();
        
        BigDecimal quotePrice = dexPrice != null ?  (isInvert ? dexPrice.getInvertedPrice(): dexPrice.getPrice() ) : (isInvert ? m_marketData.getInvertedLastPrice() : m_marketData.getLastPrice());

        return new PriceAmount( quotePrice, quoteCurrency);
    }

    public void chartUpdated(boolean createChart){
        
        m_priceLastChecked.set(LocalDateTime.now());
        if(createChart){
            createChart();
        }else{
            updateChart();
        }
    }

    public void addErgoDexListener(){
        
        String friendlyId = FriendlyId.createFriendlyId();

        ErgoDexChartView chartView = m_marketData.getChartView();
        chartView.enableCHainData(true);
        
        if(chartView != null && m_chartMsgInterface == null){
            
            m_chartMsgInterface = new NoteMsgInterface() {
                public String getId() {
                    return friendlyId;
                }

                public void sendMessage(int code, long timestamp,String networkId, Number num){
                    
                    switch(code){
                        case App.STARTED:
                        case App.LIST_UPDATED:
                        case App.LIST_CHANGED:
                            chartUpdated(true);
                            
                        break;
                        case App.LIST_CHECKED:
                            chartUpdated(false);
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



    public void initChart(TimeSpan timeSpan){
        int maxBars =  (ErgoDexChartView.MAX_CHART_WIDTH / (m_cellWidth + m_cellPadding));
        long timestamp = System.currentTimeMillis();

        m_chartView.processData(
            isInvert(), 
            maxBars, 
            timeSpan,
            timestamp,
            m_dataList.getErgoDex().getExecService(), 
            (onSucceeded)->{
            Object sourceValue = onSucceeded.getSource().getValue();
            if(sourceValue != null && sourceValue instanceof ErgoDexNumbers){
                ErgoDexNumbers numbers = (ErgoDexNumbers) sourceValue;
                m_numbers = numbers;
                m_lastPriceProperty.set(m_numbers.getClose().toPlainString());
            }
        
        }, 
        (onFailed)->{

        });
    }
    
    public void createChart(){

        LocalDateTime now = m_priceLastChecked.get();
        int viewPortHeight = (int) m_chartScrollHeight.get() - App.VIEWPORT_HEIGHT_OFFSET;
        int viewPortWidth = (int) m_chartScrollWidth.get();
        int maxBars =  (ErgoDexChartView.MAX_CHART_WIDTH / (m_cellWidth + m_cellPadding));

        TimeSpan timeSpan = m_timeSpan;
        long timestamp = System.currentTimeMillis();
       
        m_chartView.processData(
            isInvert(), 
            maxBars, 
            timeSpan,
            timestamp,
            m_dataList.getErgoDex().getExecService(), 
            (onSucceeded)->{
            Object sourceValue = onSucceeded.getSource().getValue();
            if(sourceValue != null && sourceValue instanceof ErgoDexNumbers){
                ErgoDexNumbers numbers = (ErgoDexNumbers) sourceValue;
                m_numbers = numbers;
                m_lastPriceProperty.set(m_numbers.getClose().toPlainString());
                drawChart(viewPortWidth, viewPortHeight, timeSpan, now != null ? now : LocalDateTime.now());
                setChartScrollRight();
            }
        
        }, 
        (onFailed)->{

        });
    }

    private void drawChart(int viewPortWidth, int viewPortHeight, TimeSpan timeSpan, LocalDateTime now){
        int size = m_numbers.dataLength();
        
        if(size > 0){
        
            int totalCellWidth = m_cellWidth + m_cellPadding;
            
            int itemsTotalCellWidth = size * (totalCellWidth);

            int scaleLabelLength = (m_numbers.getClose() +"").length();

            int scaleColWidth =  (scaleLabelLength * m_zeroStringWidth )+ ErgoDexChartView.SCALE_COL_PADDING;
            
            
            int width =Math.max(viewPortWidth, Math.max(itemsTotalCellWidth + scaleColWidth, ErgoDexChartView.MIN_CHART_WIDTH));
            
            int height = Math.min(ErgoDexChartView.MAX_CHART_HEIGHT, Math.max(viewPortHeight, ErgoDexChartView.MIN_CHART_HEIGHT));

            boolean isNewImg = m_img == null || (m_img != null && (m_img.getWidth() != width || m_img.getHeight() != height));

            m_img = isNewImg ? new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB) : m_img;
            m_g2d = isNewImg ? m_img.createGraphics() : m_g2d;

            ErgoDexChartView.updateBufferedImage(
                m_img, 
                m_g2d, 
                m_labelFont, 
                m_labelMetrics, 
                m_numbers,
                m_cellWidth, 
                m_cellPadding, 
                scaleColWidth,
                m_amStringWidth,
                timeSpan, 
                m_chartRange,
                now
            );

            m_chartImageView.setImage(SwingFXUtils.toFXImage(m_img, m_wImg));
        
            if(isNewImg){ 
                m_chartImageView.setFitWidth(m_img.getWidth());   
            }
    
            if(!m_chartBox.getChildren().contains(m_chartImageView)){
                m_chartBox.getChildren().clear();
                m_chartBox.setAlignment(Pos.BOTTOM_RIGHT);
                m_chartBox.getChildren().add(m_chartImageView);
                FxTimer.runLater(Duration.ofMillis(100), ()->setChartScrollRight());
        
            }

      


        }
    }

    public void updateChart(){
        LocalDateTime now = m_priceLastChecked.get();
        drawChart((int) m_chartScrollWidth.get(), (int) m_chartScrollHeight.get() - App.VIEWPORT_HEIGHT_OFFSET, m_timeSpan, now != null ? now : LocalDateTime.now());
    }

    public void setChartScrollRight(){
        
            Platform.runLater(()->m_chartScroll.setVvalue(chartScrollVvalue));
            Platform.runLater(()->m_chartScroll.setHvalue(chartScrollHvalue));

    }

    public void updateShowSwap(boolean showSwap){

        if( showSwap){
            m_toggleSwapBtn.setText("⯈");

            m_ergoDexSwapBox = m_ergoDexSwapBox == null ?  new ErgoDexSwapBox() : m_ergoDexSwapBox;

            if(! m_bodyBox.getChildren().contains(m_ergoDexSwapBox)){  
                m_bodyBox.getChildren().add(m_ergoDexSwapBox);
            }
        }else{
            m_toggleSwapBtn.setText("⯇");
            
            if(m_ergoDexSwapBox != null){
       
                if(m_bodyBox.getChildren().contains(m_ergoDexSwapBox)){
                    m_bodyBox.getChildren().remove(m_ergoDexSwapBox);
                }
                m_ergoDexSwapBox.shutdown();
                m_ergoDexSwapBox = null;
            }
        }
    }
    

    @Override
    public void close(){
        if(m_maximizeListener != null){
            getNetworksData().getStage().maximizedProperty().removeListener(m_maximizeListener);
        }
        m_ergoDexSwapBox.shutdown();

        if(m_invertListener != null){
            isInvertProperty().removeListener(m_invertListener);
        }
        if(m_chartMsgInterface != null){
            m_chartView.enableCHainData(false);
            m_chartView.removeMsgListener(m_chartMsgInterface);
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
        private ErgoDexWalletBox m_dexWallet = null;
       
        //   SimpleBooleanProperty isSpfFeesObject = new SimpleBooleanProperty(false);
        private SimpleObjectProperty<BigDecimal> m_inputAmount = new SimpleObjectProperty<>(null);
        private SimpleStringProperty m_orderTypeProperty = new SimpleStringProperty(MARKET_ORDER);

        private MenuButton m_slippageMenu;

        private ErgoDexSwapDeductionsBox m_swapFeesBox;

        private Tooltip m_errTip = new Tooltip();

        private SimpleBooleanProperty m_isSellProperty = new SimpleBooleanProperty(true);

        private ErgoDexChainData m_chainData;
  
        public ReadOnlyBooleanProperty isSellProperty(){
            return m_isSellProperty;
        }
    
        //private SimpleObjectProperty<PriceCurrency> m_currentAmountCurrency = new SimpleObjectProperty<>(null);
      //  private SimpleObjectProperty<PriceCurrency> m_currentVolumeCurrency = new SimpleObjectProperty<>(null);
    
    
            
        private Button m_executeBtn = new Button("");
        private DropShadow textShadow = new DropShadow();

        private PriceCurrency m_amountCurrency;
        private PriceCurrency m_volumeCurrency;

        private Image m_baseCurrencyImage = m_marketData.getBaseCurrency().getBackgroundIcon(BG_ICON_HEIGHT);
        private Image m_quoteCurrencyImage =  m_marketData.getQuoteCurrency().getBackgroundIcon(BG_ICON_HEIGHT);

        private TextField volumeField;
        private ImageView volumeFieldImage;
        private ImageView amountFieldImage;
        private Button sellBtn;
        private Button buyBtn;

        public ErgoDexSwapBox(){
            super();
            setMinWidth(SWAP_BOX_MIN_WIDTH);
            VBox.setVgrow(this, Priority.ALWAYS);

            
            m_dexWallet = new ErgoDexWalletBox();


            m_amountCurrency = getAmountCurrency(isSellProperty().get(), isInvertProperty().get());
            m_volumeCurrency = getVolumeCurrency(isSellProperty().get(), isInvertProperty().get());
           
            sellBtn = new Button("Sell");
            sellBtn.setOnAction(e->{
                m_isSellProperty.set(true);
            });
           
           
            buyBtn = new Button("Buy");
            buyBtn.setOnAction(e->{
                m_isSellProperty.set(false);
            });
       

            Region buySellSpacerRegion = new Region();
            VBox.setVgrow(buySellSpacerRegion, Priority.ALWAYS);
    
            HBox buySellBox = new HBox(sellBtn, buyBtn);
            HBox.setHgrow(buySellBox,Priority.ALWAYS);
    
            sellBtn.prefWidthProperty().bind(buySellBox.widthProperty().divide(2));
            buyBtn.prefWidthProperty().bind(buySellBox.widthProperty().divide(2));
    
            
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
    
            ImageView orderPriceImageView = new ImageView(PriceCurrency.getBlankBgIcon(BG_ICON_HEIGHT, m_marketData.getCurrentSymbol(isInvert())));
            orderPriceImageView.imageProperty().bind(Bindings.createObjectBinding(()->PriceCurrency.getBlankBgIcon(BG_ICON_HEIGHT, m_marketData.getCurrentSymbol(isInvertProperty().get())), isInvertProperty()));
    
            TextField orderPriceTextField = new TextField( (isInvert() ? m_marketData.getInvertedLastPrice() : m_marketData.getLastPrice()).toPlainString());
            HBox.setHgrow(orderPriceTextField, Priority.ALWAYS);
            orderPriceTextField.setAlignment(Pos.CENTER_RIGHT);
            orderPriceTextField.setEffect(textShadow);
            orderPriceTextField.textProperty().bind(m_lastPriceProperty);
    
            HBox orderPriceTextFieldBox = new HBox(orderPriceTextField);
            HBox.setHgrow(orderPriceTextFieldBox, Priority.ALWAYS);
            orderPriceTextFieldBox.setPadding(new Insets(0,0,3,0));
            orderPriceTextFieldBox.setAlignment(Pos.CENTER_LEFT);

            Label orderPriceCheckedField = new Label();
            orderPriceCheckedField.setPrefWidth(80);
            orderPriceCheckedField.setId("smallSecondaryColor");
            orderPriceCheckedField.textProperty().bind(Bindings.createObjectBinding(()->m_priceLastChecked.get() != null ? Utils.formatTimeString(m_priceLastChecked.get()) : "" , m_priceLastChecked));
    
            HBox orderPriceStatusBox = new HBox(orderPriceCheckedField);
            VBox.setVgrow(orderPriceStatusBox, Priority.ALWAYS);
            HBox.setHgrow(orderPriceStatusBox, Priority.ALWAYS);
            orderPriceStatusBox.setAlignment(Pos.BOTTOM_RIGHT);
    
            StackPane orderPriceStackBox = new StackPane(orderPriceImageView, orderPriceStatusBox, orderPriceTextFieldBox);
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
    
            amountFieldImage = new ImageView();
           
    
            TextField amountField = new TextField("0");
            HBox.setHgrow(amountField, Priority.ALWAYS);
            amountField.setId("swapBtn");
            amountField.setAlignment(Pos.CENTER_RIGHT);
            amountField.setOnAction(e -> m_executeBtn.fire());
            amountField.setEffect(textShadow);

            amountField.textProperty().addListener((obs, oldval, newval)->{
                String number = newval.replaceAll("[^0-9.]", "");
                if(!Utils.onlyZero(number)){
                    BigDecimal bigAmount = BigDecimal.ZERO;
                    PriceCurrency amountCurrency = m_amountCurrency;
                    
                    bigAmount = new BigDecimal(number);
                    bigAmount.setScale(amountCurrency.getDecimals(), RoundingMode.FLOOR);
                   
                    amountField.setText(bigAmount.toPlainString());

                    m_inputAmount.set(bigAmount);
                   
                }else{
                    amountField.setText(number);
                    m_inputAmount.set(BigDecimal.ZERO);
                }
                
            });
    

            amountField.focusedProperty().addListener((obs,oldval,newval)->{
                if(!newval){
                    String str = amountField.getText();
                    if(Utils.isTextZero(str)){
                        amountField.setText("0");
                    }
                }
            });
    
            StackPane amountStack = new StackPane(amountFieldImage, amountField);
            HBox.setHgrow(amountStack, Priority.ALWAYS);
            amountStack.setAlignment(Pos.CENTER_LEFT);
            amountStack.setId("darkBox");
            amountStack.setPadding(new Insets(2));
            amountStack.setMinHeight(40);

            /*Slider amountSlider = new Slider();
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
                
            });*/
            VBox amountVBox = new VBox( amountStack);
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
    
            volumeFieldImage = new ImageView();
        
            volumeField = new TextField("0");
            HBox.setHgrow(volumeField, Priority.ALWAYS);
            volumeField.setId("swapBtnStatic");
            volumeField.setEditable(false);
            volumeField.setAlignment(Pos.CENTER_RIGHT);
            volumeField.setEffect(textShadow);

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

            Text slippageText = new Text("Slippage Tolerance ");
            slippageText.setFont(App.smallFont);
            slippageText.setFill(App.txtColor);
            
            Tooltip slippageToleranceTip = new Tooltip("Slippage tolerance: Transaction will revert if\nprice changes unfavorably by this percentage.");
            slippageToleranceTip.setShowDelay(javafx.util.Duration.millis(100));
            slippageToleranceTip.setShowDuration(javafx.util.Duration.seconds(20));

            m_slippageMenu = new MenuButton();
            m_slippageMenu.setId("arrowMenuButtonSmall");
            m_slippageMenu.setTooltip(slippageToleranceTip);
            m_slippageMenu.textProperty().bind(Bindings.createObjectBinding(()->{
                BigDecimal slippage = m_slippageTolerance.get();
                return slippage.multiply(BigDecimal.valueOf(100)).intValue() + "% ";
            }, m_slippageTolerance));

            MenuItem onePercentItem = new MenuItem(" 1%");
            onePercentItem.setOnAction(e->setSlippageTolerance(BigDecimal.valueOf(0.01)));
            MenuItem threePercentItem = new MenuItem(" 3%");
            threePercentItem.setOnAction(e->m_slippageTolerance.set(BigDecimal.valueOf(0.03)));
            MenuItem sevenPercentItem = new MenuItem( " 7%");
            sevenPercentItem.setOnAction(e->setSlippageTolerance(BigDecimal.valueOf(0.07)));
            m_slippageMenu.getItems().addAll(onePercentItem, threePercentItem, sevenPercentItem);


            HBox slippageMenuBox = new HBox(m_slippageMenu);
            slippageMenuBox.setId("darkBox");

            HBox slippageBox = new HBox(slippageText, slippageMenuBox);
            HBox.setHgrow(slippageBox, Priority.ALWAYS);
            slippageBox.setPadding(new Insets(0,10,0,10));
            slippageBox.setAlignment(Pos.CENTER_RIGHT);
        
            HBox isBuyPaddingBox = new HBox(buySellBox);
            HBox.setHgrow(isBuyPaddingBox, Priority.ALWAYS);
            isBuyPaddingBox.setPadding(new Insets(15));
    
            m_executeBtn.setPadding(new Insets(5));
            m_executeBtn.setOnAction(e->executeSwap());
        
            

            HBox executeBox = new HBox(m_executeBtn);
            HBox.setHgrow(executeBox,Priority.ALWAYS);
            executeBox.setAlignment(Pos.CENTER);
            executeBox.setMinHeight(40);

    
            HBox executePaddingBox = new HBox(executeBox);
            HBox.setHgrow(executePaddingBox, Priority.ALWAYS);
            executePaddingBox.setPadding(new Insets(10,0, 10, 0));

            m_swapFeesBox = new ErgoDexSwapDeductionsBox();
    
            VBox marketBox = new VBox( isBuyPaddingBox, pricePaddingBox, amountPaddingBox, quoteAmountPaddingBox, slippageBox, executePaddingBox, m_swapFeesBox);
            marketBox.setPadding(new Insets(5));
            marketBox.setId("bodyBox");
    
        
    
            VBox marketPaddingBox = new VBox(orderTypeBox, marketBox);
    
            
    
    
            Runnable updateOrderType = () ->{
            
                String orderType = m_orderTypeProperty.get() != null ? m_orderTypeProperty.get() : "";
                
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
            
    
            m_orderTypeProperty.addListener((obs,oldVal,newVal)->updateOrderType.run());
    
            updateOrderType.run();
    

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
                
            m_inputAmount.addListener((obs,oldval,newval)-> volumeField.setText(getSubTotalVolume().toPlainString()));

            m_priceLastChecked.addListener((obs,oldval,newval)->{});

            isSellProperty().addListener((obs,oldval,newval)->{
                sellBtn.setId(newval ? "iconBtnSelected" : "iconBtn");
                buyBtn.setId(newval ? "iconBtn" : "iconBtnSelected");
                updateSell(newval);
            });
     
     
    
        }

        public void updateSell(boolean isSell){
           
            boolean isInvert = isInvertProperty().get();
            PriceCurrency baseCurrency = isInvert ? m_marketData.getQuoteCurrency() : m_marketData.getBaseCurrency();
            //PriceCurrency quoteCurrency = isInvert ? m_marketData.getBaseCurrency() : m_marketData.getQuoteCurrency();
            
            String baseSymbol = baseCurrency.getName();
    
            m_executeBtn.setText(isSell ? "Sell " + baseSymbol : "Buy " + baseSymbol);
            Image amountImage = getAmountImage(isSell, isInvert);
            Image volumeImage = getVolumeImage(isSell, isInvert);
        
            amountFieldImage.setImage(amountImage);
            volumeFieldImage.setImage(volumeImage);

            m_amountCurrency = getAmountCurrency(isSell, isInvert);
            m_volumeCurrency = getVolumeCurrency(isSell, isInvert);

            volumeField.setText(getSubTotalVolume().toPlainString());
        }

        public void updateInvert(boolean invert){
            if(m_dexWallet != null){
                m_dexWallet.updateBalance();
            }
           
            updateSell(isSellProperty().get());
        }
        
        
        public PriceCurrency getAmountCurrency(boolean isSell,  boolean invert){
            PriceCurrency baseCurrency  = invert ? m_marketData.getQuoteCurrency() : m_marketData.getBaseCurrency();
            PriceCurrency quoteCurrency = invert ? m_marketData.getBaseCurrency()  : m_marketData.getQuoteCurrency();
            return isSell ? baseCurrency : quoteCurrency;
        }

        public PriceCurrency getVolumeCurrency(boolean isSell,  boolean invert){
            PriceCurrency baseCurrency  = invert ? m_marketData.getQuoteCurrency() : m_marketData.getBaseCurrency();
            PriceCurrency quoteCurrency = invert ? m_marketData.getBaseCurrency()  : m_marketData.getQuoteCurrency();
            return isSell ? quoteCurrency :  baseCurrency;
        }

        public Image getAmountImage(boolean isSell, boolean isInvert){
            Image baseImage = isInvert ? m_quoteCurrencyImage : m_baseCurrencyImage;
            Image quoteImage = isInvert ? m_baseCurrencyImage : m_quoteCurrencyImage;
            return isSell ? baseImage : quoteImage;
        }

    
        public Image getVolumeImage(boolean isSell, boolean isInvert){
            Image baseImage = isInvert ? m_quoteCurrencyImage : m_baseCurrencyImage;
            Image quoteImage = isInvert ? m_baseCurrencyImage : m_quoteCurrencyImage;
            return isSell ? quoteImage : baseImage;
        }

      

        public void showError(String msg){
               
            double stringWidth =  Utils.computeTextWidth(App.txtFont, msg);;

            Point2D p = m_executeBtn.localToScene(0.0, 0.0);
            m_errTip.setText(msg);
            m_errTip.show(m_executeBtn,
                    p.getX() + m_executeBtn.getScene().getX()
                            + m_executeBtn.getScene().getWindow().getX() - (stringWidth/2),
                    (p.getY() + m_executeBtn.getScene().getY()
                            + m_executeBtn.getScene().getWindow().getY()) - 40);
            PauseTransition pt = new PauseTransition(javafx.util.Duration.millis(5000));
            pt.setOnFinished(ptE -> {
                m_errTip.hide();
            });
            pt.play();
        }

        public String getLocationId(){
            return m_dataList.getLocationId();
        }


        public void executeSwap(){
            boolean isSell =  isSellProperty().get();
            boolean isInvert = isInvertProperty().get();
            boolean isFeeSPF = false;
            PriceAmount baseBalance = m_dexWallet == null ? null : isSell ? m_dexWallet.baseAmountProperty().get() : m_dexWallet.quoteAmountProperty().get();
          
            if( m_dexWallet.ergoAmountProperty().get() != null && baseBalance != null){
               
                boolean isErg = baseBalance.getTokenId().equals(ErgoCurrency.TOKEN_ID);
                
          
                BigDecimal totalFees = m_swapFeesBox.totalFeesProperty().get();
                BigDecimal minErg = totalFees.add(ErgoNetwork.MIN_NETWORK_FEE);

                BigDecimal bigAmount = m_inputAmount.get();
              
                BigDecimal totalAmountRequired = isErg ? bigAmount.add(minErg) : bigAmount;

                if(m_dexWallet.ergoAmountProperty().get().getBigDecimalAmount().compareTo(minErg) > -1){
                    if(totalAmountRequired.compareTo(baseBalance.getBigDecimalAmount()) < 1){
                  
                        NoteInterface ergoInterface = m_dataList.ergoInterfaceProperty().get();

                        if(ergoInterface != null){
                                long networkFee = m_networkFee.get().getLongAmount();
                                long minExFee = m_minExFee.get().getLongAmount();
                                long maxExFee = m_maxExFee.get().getLongAmount();
                                PriceCurrency amountCurrency = getAmountCurrency(isSell, isInvert);
                                PriceAmount input = new PriceAmount(bigAmount, amountCurrency);
                                
                                if(m_marketData.isNative2Token()){
                                    String contractString  = null;
                                    try{
                                        contractString = Utils.getStringFromResource("/ergoDex/contracts/SwapSellV3.sc");
                                    }catch(IOException e1){
                                        showError("Error loading contract: " + e1.toString());
                                        return;
                                    }
                                    
                                    executeN2TContract(ergoInterface, contractString, isSell, isFeeSPF, input.getLongAmount(), networkFee, minExFee, maxExFee);
                                }
                                
                          
                        }else{
                            showError("Ergo Network is not available");
                        }
                      
                    }else{
                        showError("Insufficient " + baseBalance.getCurrency().getSymbol()+ " balance: " + totalAmountRequired + " required)");
                    }
                }else{
                    showError("insufficient Ergo balamce for fees: " + minErg + "ERG required");
                }
            }else{
                showError("Wallet unavailable");
            }
        }
        
        public static Future<?> executeN2TContract(NoteInterface ergoInterface, String contractStr, boolean isSell, boolean isFeeSPF, long baseAmount, long networkFee, long minExFee, long maxExFee){


            long maxOutput = 0;

           /* BigDecimal feePerToken = getFeePerToken(minExFee, minOutput);
            long adjustedMinExFee = feePerToken.multiply(BigDecimal.valueOf(minOutput)).longValue();
            
            try {
                Files.writeString(App.logFile.toPath(), "feePerToken: " + feePerToken + "\n" + adjustedMinExFee + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {

            }*/

            

            //minExFee / minOutput(long)
            JsonObject exFeePerTokenDenom = new JsonObject();
           // exFeePerTokenDenom.addProperty("value", getFeePerDenom(ergoAmount.getLongAmount(), quotePriceAmount.getLongAmount()));
            exFeePerTokenDenom.addProperty("type", "long");
            exFeePerTokenDenom.addProperty("name", "ExFeePerTokenDenom");

            JsonObject swapObject = new JsonObject();
                 
            /*if(!m_dexWallet.getErgoWalletControl().sendNote(
                "executeContract", 
                swapObject, 
                onSucceeded->{

                }, onFailed->{

                })){
                    showError("Ergo Network - Ergo Wallet is inaccessible.");
            }*/
            return null;
        }
        
        public long getMinQuoteAmount(long slippage){


            return 0;
        }

        public BigDecimal getFeePerToken(long minExFee, long minOutput){
            
            return BigDecimal.valueOf(minExFee).divide(BigDecimal.valueOf(minOutput), 18, RoundingMode.HALF_UP);
        }

        public void setSlippageTolerance(BigDecimal decimal){
            m_slippageTolerance.set(decimal);
            save();
        }
        
        public void setMaxExFee(BigDecimal decimal){
            m_maxExFee.set(new ErgoAmount(decimal, ErgoDex.NETWORK_TYPE));
            save();
        }

        public void setNetworkFee(BigDecimal decimal){
            m_networkFee.set(new ErgoAmount(decimal, ErgoDex.NETWORK_TYPE));
            save();
        }
  
        public void setShowSwapSettings(boolean showSettings){
            m_showSwapFeesBox.set(showSettings);
            save();
        }
    

      
        /*
         if (input.asset.id === this.assetX.id)
      return this.y.withAmount(
        BigInt(
          math
            .evaluate!(`(${this.y.amount} * ${input.amount} * ${this.feeNum}) / ((${this.x.amount} + (${this.x.amount} * ${slippage}) / ${10000n}) * ${this.feeDenom} + ${input.amount} * ${this.feeNum})`)
            .toFixed(0)
        )
      )
    else
      return this.x.withAmount(
        BigInt(
          math
            .evaluate!(`(${this.x.amount} * ${input.amount} * ${this.feeNum}) / ((${this.y.amount} + (${this.y.amount} * ${slippage}) / ${10000n}) * ${this.feeDenom} + ${input.amount} * ${this.feeNum})`)
            .toFixed(0)
        )
      )
         */
        public PriceAmount calculateMinOutputVolume(PriceAmount input, PriceCurrency outputCurrency){

            PriceAmount x = null; // m_marketData.xVolumeProperty().get();
            PriceAmount y = null; // m_marketData.yVolumeProperty().get();
            
            long feeNum = 0;// m_marketData.feeNumProperty().get();
       
            if(input != null && x != null && y != null & feeNum > 0){

                BigDecimal maxSlippage = m_slippageTolerance.get();
               
                //BigDecimal bigPrice =  m_orderPriceProperty.get() != null ? m_orderPriceProperty.get() : BigDecimal.ZERO;
          
                long xAmount = x.getLongAmount();
                long yAmount = y.getLongAmount();

                long slippage = maxSlippage.multiply(BigDecimal.valueOf(100)).longValue();
                long feeDenom = ErgoDex.SWAP_FEE_DENOM;

                if(input.getTokenId().equals(x.getTokenId())){
                    long amount = (yAmount * input.getLongAmount() * feeNum) / ((yAmount + (yAmount * slippage) / 10000L) * feeDenom + input.getLongAmount() * feeNum);
                    return new PriceAmount(amount, outputCurrency);
                }else{
                    long amount = (xAmount * input.getLongAmount() * feeNum) / ((yAmount + (yAmount * slippage) / 10000L) * feeDenom + input.getLongAmount() * feeNum);
                    return new PriceAmount(amount, outputCurrency);
                }
            }
            
            return outputCurrency != null ? new PriceAmount(BigDecimal.ZERO, outputCurrency) : null;
            

        }

        public BigDecimal getSubTotalVolume(){
            BigDecimal amount = m_inputAmount.get();
            boolean isSell = isSellProperty().get();
            
            BigDecimal priceQuote = m_numbers != null ? m_numbers.getClose() : BigDecimal.ZERO;
            PriceCurrency volumeCurrency = m_volumeCurrency;

            BigDecimal volume = priceQuote.equals(BigDecimal.ZERO) || amount.equals(BigDecimal.ZERO) ? BigDecimal.ZERO : (isSell ? amount.multiply(priceQuote) : amount.divide(priceQuote, volumeCurrency.getDecimals(), RoundingMode.FLOOR));
            return volume.setScale(volumeCurrency.getDecimals(), RoundingMode.FLOOR);
        }

        public void shutdown(){
            m_swapFeesBox.shutdown();
    
           
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

            public ErgoWalletControl getErgoWalletControl(){
                return m_walletControl;
            }
    
            private NoteInterface getErgoNetworkInterface(){
                return m_dataList.ergoInterfaceProperty().get();
            }

            public ReadOnlyObjectProperty<PriceAmount> ergoAmountProperty(){
                return m_ergoAmountProperty;
            }

            public ReadOnlyObjectProperty<PriceAmount> baseAmountProperty(){
                return m_baseAmountProperty;
            }

            public ReadOnlyObjectProperty<PriceAmount> quoteAmountProperty(){
                return m_quoteAmountProperty;
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
                
                ergoAmountHeadingField.textProperty().bind(Bindings.createObjectBinding(()->{
                    String walletName = m_walletControl.walletNameProperty().get();
                    PriceAmount ergoAmount =  m_ergoAmountProperty.get();
                    NoteInterface ergoInterface = m_dataList.ergoInterfaceProperty().get();
                    return ergoAmount != null ? ergoAmount.getAmountString() : walletName != null ? "🔒 Locked" : ergoInterface != null ? "🚫" : "⛔" ;
                }, m_ergoAmountProperty, m_walletControl.walletNameProperty(), m_dataList.ergoInterfaceProperty()));
                
                ergoAmountHeadingField.prefWidthProperty().bind(Bindings.createObjectBinding(()->{
                    double w = Utils.computeTextWidth(App.txtFont, ergoAmountHeadingField.textProperty().get()) + 20;
                    return w < 30 ? 30 : (w > 200 ? 200 : w);
                }, ergoAmountHeadingField.textProperty()));


                Text ergText= new Text("");
                ergText.setFont(App.defjaVuTxt);
                ergText.setFill(App.txtColor);
                ergText.textProperty().bind(Bindings.createObjectBinding(()->{
                    PriceAmount ergoAmount =  m_ergoAmountProperty.get();
                    return ergoAmount != null ? ErgoCurrency.FONT_SYMBOL + " " : "";
                }, m_ergoAmountProperty));

                HBox ergoAmountHeadingFieldBox = new HBox(ergoAmountHeadingField);
                ergoAmountHeadingFieldBox.setPadding(new Insets(2, 0,0,0));

                HBox ergoAmountLabelBox = new HBox(ergoAmountHeadingFieldBox, ergText);
                ergoAmountLabelBox.setAlignment(Pos.CENTER_LEFT);
                ergoAmountLabelBox.setId("darkBox");
                ergoAmountLabelBox.setPadding(new Insets(2,0,2,0));

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
                selectWalletRowBox.setAlignment(Pos.CENTER);
                selectWalletRowBox.setPadding(new Insets(7,0,7,0));

                m_walletBodyVBox = new VBox(selectWalletRowBox);
                m_walletBodyVBox.setId("bodyBox");
                m_walletBodyVBox.setPadding(new Insets(padding));

    
               
    
                m_walletAdrUnlockBtn = new Button(emptyAddressString);
                m_walletAdrUnlockBtn.setId("rowBtn");
                m_walletAdrUnlockBtn.setPadding(new Insets(0, 0, 0, 10));
                m_walletAdrUnlockBtn.setAlignment(Pos.CENTER);
                m_walletAdrUnlockBtn.setPrefWidth(btnWidth + 20);
                m_walletAdrUnlockBtn.setOnAction(e->{
                    if(m_walletControl.walletNameProperty().get() != null){
                        m_walletControl.connectToWallet();
                    }
                });

                m_walletAdrMenu = new MenuButton("");
                m_walletAdrMenu.setId("arrowMenuButton");
                m_walletAdrMenu.setPadding(rowPadding);
                m_walletAdrMenu.setPrefWidth(btnWidth);
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


                TextField baseAmountField = new TextField();
                HBox.setHgrow(baseAmountField, Priority.ALWAYS);
                baseAmountField.setId("itemLbl");
                baseAmountField.setEditable(false);
                baseAmountField.setAlignment(Pos.CENTER_RIGHT);
                baseAmountField.textProperty().bind(Bindings.createObjectBinding(()->{
                    PriceAmount baseAmount = m_baseAmountProperty.get();
                    return baseAmount != null ? baseAmount.amountProperty().get() + "" : "⸻";
                }, m_baseAmountProperty));
            
                ImageView baseImgView = new ImageView();
                baseImgView.imageProperty().bind(Bindings.createObjectBinding(()->getAmountImage(isSellProperty().get(), isInvertProperty().get()),isInvertProperty(), isSellProperty()));
        
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
                quoteAmountField.setId("itemLbl");
                quoteAmountField.setEditable(false);
                quoteAmountField.setAlignment(Pos.CENTER_RIGHT);
                quoteAmountField.textProperty().bind(Bindings.createObjectBinding(()->{
                    PriceAmount quoteAmount = m_quoteAmountProperty.get();
                    return quoteAmount != null ? quoteAmount.amountProperty().get() + "" : "⸻";
                }, m_quoteAmountProperty));
                
        
                ImageView quoteImgView = new ImageView();
                quoteImgView.setPreserveRatio(true);
                quoteImgView.imageProperty().bind(Bindings.createObjectBinding(()->{
                    PriceAmount quoteAmount =  m_quoteAmountProperty.get();
                    String quoteSymbol =quoteAmount != null ? quoteAmount.getCurrency().getSymbol() : "-";

                    return PriceCurrency.getBlankBgIcon(38, quoteSymbol);
                }, m_quoteAmountProperty));



        
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
                
               
                m_assetsVBox.getChildren().addAll(baseAmountBox, assetSpacer, quoteAmountBox);
                

                m_walletControl.walletNameProperty().addListener((obs, oldVal, newVal) -> {

                    if(newVal != null){
                        m_openWalletBtn.setText(newVal);
                        m_openWalletBtn.setPrefWidth(btnWidth);
              
                        if(!  clearWalletBtnBox.getChildren().contains(m_clearWalletBtn)){
                            clearWalletBtnBox.getChildren().add(m_clearWalletBtn);
                        }
                        if(!m_walletBodyVBox.getChildren().contains(m_walletAdrBox)){
                            m_walletBodyVBox.getChildren().add(1, m_walletAdrBox);
                        }
                    }else{
                        m_openWalletBtn.setText(walletBtnDefaultString);
                        m_openWalletBtn.setPrefWidth(btnWidth + 30);
                        if( clearWalletBtnBox.getChildren().contains(m_clearWalletBtn)){
                            clearWalletBtnBox.getChildren().remove(m_clearWalletBtn);
                        }
                        if(m_walletBodyVBox.getChildren().contains(m_walletAdrBox)){
                            m_walletBodyVBox.getChildren().remove(m_walletAdrBox);
                        }
                    }
            
                });

                m_walletControl.balanceProperty().addListener((obs,oldval,newval)->updateBalance());
    
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

            public void updateBalance(){
                JsonObject balanceObject = m_walletControl.balanceProperty().get();
                boolean isInverted = isInvert();
                if(balanceObject != null){
                    ArrayList<PriceAmount> priceAmountList = AddressesData.getBalanceList(balanceObject,true, ErgoDex.NETWORK_TYPE);
                    
                    m_ergoAmountProperty.set(AddressesData.getPriceAmountFromList(priceAmountList, ErgoCurrency.TOKEN_ID));
                    
                    PriceAmount baseAmount = AddressesData.getPriceAmountFromList(priceAmountList, isInverted ?  m_marketData.getQuoteId() :  m_marketData.getBaseId());
                    baseAmount = baseAmount == null ?  new PriceAmount(0, isInverted ? m_marketData.getQuoteCurrency() : m_marketData.getBaseCurrency())  : baseAmount;

                    m_baseAmountProperty.set(baseAmount);
                    
                    PriceAmount quoteAmount = AddressesData.getPriceAmountFromList(priceAmountList, isInverted ? m_marketData.getBaseId() : m_marketData.getQuoteId());
            
                    quoteAmount = quoteAmount == null ?  new PriceAmount(0, isInverted ?  m_marketData.getBaseCurrency() : m_marketData.getQuoteCurrency())  : quoteAmount;

                    m_quoteAmountProperty.set(quoteAmount);
                }else{
                    m_ergoAmountProperty.set(null);
                    m_baseAmountProperty.set(null);
                    m_quoteAmountProperty.set(null);
                
                }
               // updateVolumeFromAmount();
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
        
        public class ErgoDexSwapDeductionsBox extends VBox{
     
 
            private VBox m_settingsBodyPaddingBox;
            
            private Text m_networkFeeText = null;
            private TextField m_networkFeeField = null;
            private HBox m_networkFeeBox = null;
            private Tooltip m_networkFeeTooltip = null;
            private Label m_networkFeeInfoLbl = null;
            private HBox m_networkFeeFieldBox = null;
            private Button m_networkFeeEnterBtn = null;
            private EventHandler<ActionEvent> m_networkFeeFieldAction = null;
            private ChangeListener<String> m_networkFeeTextListener = null;
            private ChangeListener<Boolean> m_networkFeeFocusListener = null;

            private Text m_maxSwapFeeText = null;
            private TextField m_maxSwapFeeField = null;
            private Tooltip m_maxSwapTooltip;
            private Label m_maxSwapInfoLbl;
            private HBox m_maxSwapFeeBox;
            private HBox m_maxSwapFeeFieldBox;
            private Button m_maxSwapFeeEnterBtn = null;
            private EventHandler<ActionEvent> m_maxSwapFieldAction;
            private ChangeListener<String> m_maxSwapFeeTextListener = null;
            private ChangeListener<Boolean> m_maxSwapFeeFocusListener = null;


            private Tooltip m_slippageAmountToolTip = null;
            private Label m_slippageAmountInfoLbl = null;
            private Text m_slippageAmountText = null;
            private Label m_slippageAmountLbl = null;
            private HBox m_slippageAmountFieldBox = null;
            private HBox m_slippageAmountBox = null;

            private Tooltip m_minErgoToolTip = null;
            private Label m_minErgoInfoLbl = null;
            private Text m_minErgoText = null;
            private Text m_minErgoAmountText = null;
            private HBox m_minErgoFieldBox = null;
            private HBox m_minErgoBox = null;

            private VBox m_settingsExtendedBox = null;
            private SimpleObjectProperty<BigDecimal> m_totalFees = new SimpleObjectProperty<>(BigDecimal.ZERO);

            public ErgoDexSwapDeductionsBox(){
                super();
                setId("bodyBox");

                HBox bodyHGradient = new HBox();
                HBox.setHgrow(bodyHGradient, Priority.ALWAYS);
                bodyHGradient.setId("hGradient");
                bodyHGradient.setMinHeight(2);

            
                Label showFeesBtn = new Label();
                showFeesBtn.setId("caretBtn");
                showFeesBtn.textProperty().bind(Bindings.createObjectBinding(()->m_showSwapFeesBox.get() ? "⏷" : "⏵", m_showSwapFeesBox));
              
                Text feesText = new Text("Fees ");
                feesText.setFont(App.smallFont);
                feesText.setFill(App.txtColor);
                feesText.setMouseTransparent(true);
        
                Label totalFeesLbl = new Label();
                totalFeesLbl.setMouseTransparent(true);
                totalFeesLbl.setId("addressField");
                totalFeesLbl.setAlignment(Pos.CENTER_RIGHT);
          
                HBox feesFieldBox = new HBox(totalFeesLbl);
                HBox.setHgrow(feesFieldBox, Priority.ALWAYS);
                feesFieldBox.setMouseTransparent(true);
                feesFieldBox.setAlignment(Pos.CENTER_RIGHT);
                feesFieldBox.setPadding(new Insets(5,5,5,10));

                Button minDeductionsBtn = new Button("MIN");
                minDeductionsBtn.setPadding(new Insets(2, 5, 2, 5));
                minDeductionsBtn.setId("smallIconBtnSelected");
                minDeductionsBtn.setOnAction(e->{
                    setMinimalFees();
                });

                HBox deductionsBox = new HBox(showFeesBtn, feesText, feesFieldBox);
                deductionsBox.setPadding(new Insets(5,5,0,5));
                deductionsBox.setAlignment(Pos.CENTER_LEFT);
                deductionsBox.setId("hand");
                deductionsBox.setFocusTraversable(true);
                deductionsBox.setOnMouseClicked(e-> setShowSwapSettings(!m_showSwapFeesBox.get()));

                m_totalFees.addListener((obs,oldval,newval)->{
                    if(newval.compareTo(ErgoDex.SWAP_MIN_TOTAL_FEES.getBigDecimalAmount()) != 0){
                        if(!deductionsBox.getChildren().contains(minDeductionsBtn)){
                            deductionsBox.getChildren().add(minDeductionsBtn);
                        }
                    }else{
                        if(deductionsBox.getChildren().contains(minDeductionsBtn)){
                            deductionsBox.getChildren().remove(minDeductionsBtn);
                        }
                    }
                });

                m_totalFees.bind(Bindings.createObjectBinding(()->{
                    BigDecimal maxFee = m_maxExFee.get().getBigDecimalAmount();
                    BigDecimal networkFee = m_networkFee.get().getBigDecimalAmount();
                    return maxFee.add(networkFee);
                }, m_maxExFee, m_networkFee));
                
                totalFeesLbl.textProperty().bind(m_totalFees.asString().concat(" ERG"));

                totalFeesLbl.minWidthProperty().bind(Bindings.createObjectBinding(()->{
                    String str = totalFeesLbl.textProperty().get();
                    return str.length() == 0 ? 20 : Utils.computeTextWidth(App.txtFont, str);
                }, totalFeesLbl.textProperty()));


                HBox minVolumeHGradient = new HBox();
                minVolumeHGradient.setId("hGradient");
                minVolumeHGradient.setMinHeight(2);
                minVolumeHGradient.setMinWidth(150);

                Tooltip minVolumeToolTip = new Tooltip("The minimum amount you will receive.");
                minVolumeToolTip.setShowDelay(javafx.util.Duration.millis(100));
                minVolumeToolTip.setShowDuration(javafx.util.Duration.seconds(20));

                Label minVolumeInfoLbl = new Label("ⓘ");
                minVolumeInfoLbl.setTooltip(minVolumeToolTip);
                minVolumeInfoLbl.setId("logoBtn");
 
                Text minVolumeText = new Text("Minimum Volume");
                minVolumeText.setFont(App.smallFont);
                minVolumeText.setFill(App.txtColor);
                minVolumeText.setMouseTransparent(true);
        
                TextField minVolumeField = new TextField("1000");
                minVolumeField.setId("addressField");
                minVolumeField.setAlignment(Pos.CENTER_RIGHT);
                HBox.setHgrow(minVolumeField, Priority.ALWAYS);
            
                HBox minVolumeFieldBox = new HBox(minVolumeField);
                HBox.setHgrow(minVolumeFieldBox, Priority.ALWAYS);
                minVolumeFieldBox.setAlignment(Pos.CENTER_LEFT);
          
                HBox minVolumeBox = new HBox(minVolumeInfoLbl, minVolumeText, minVolumeFieldBox);
                HBox.setHgrow(minVolumeBox, Priority.ALWAYS);
                minVolumeBox.setMouseTransparent(true);
                minVolumeBox.setAlignment(Pos.CENTER_LEFT);
                minVolumeBox.setPadding(new Insets(5,5,1,10));
           
                m_settingsBodyPaddingBox = new VBox();
                m_settingsBodyPaddingBox.setPadding(new Insets(2, 5,5,5));

                getChildren().addAll(bodyHGradient, deductionsBox, m_settingsBodyPaddingBox, minVolumeHGradient, minVolumeBox);

                updateShowDeductions(m_showSwapFeesBox.get());

                m_showSwapFeesBox.addListener((obs,oldval,newval)->updateShowDeductions(newval));

            }

            public void setMinimalFees(){
                BigDecimal maxExFee = m_maxExFee.get().getBigDecimalAmount();
                if(maxExFee.compareTo(ErgoDex.SWAP_MIN_MAX_EXEC_FEE.getBigDecimalAmount()) != 0){
                    setMaxExFee(ErgoDex.SWAP_MIN_MAX_EXEC_FEE.getBigDecimalAmount());
                    if(m_maxSwapFeeField != null){
                        m_maxSwapFeeField.setText(ErgoDex.SWAP_MIN_MAX_EXEC_FEE + "");
                    }
                }

                BigDecimal networkFee = m_networkFee.get().getBigDecimalAmount();
                if(networkFee.compareTo(ErgoDex.NETWORK_MIN_FEE.getBigDecimalAmount()) != 0){
                    setNetworkFee(ErgoDex.NETWORK_MIN_FEE.getBigDecimalAmount());
                    if(m_networkFeeField != null){
                        m_networkFeeField.setText(ErgoDex.NETWORK_MIN_FEE + "");
                    }
                }

                /*BigDecimal slippage = m_slippageTolerance.get();
                if(slippage.compareTo(ErgoDex.MIN_SLIPPAGE_TOLERANCE) != 0){
                    setSlippageTolerance(ErgoDex.MIN_SLIPPAGE_TOLERANCE);
                }*/
            }

            public ReadOnlyObjectProperty<BigDecimal> totalFeesProperty(){
                return m_totalFees;
            }
            
            public void updateShowDeductions(boolean value){
                
                if(value){
                    if(m_settingsExtendedBox == null){
                        m_networkFeeText = new Text(String.format("%-15s", "Network Fee" ));
                        m_networkFeeText.setFont(App.smallFont);
                        m_networkFeeText.setFill(App.txtColor);

                        m_networkFeeEnterBtn = new Button("↵");
                        m_networkFeeEnterBtn.setFocusTraversable(true);
                        m_networkFeeEnterBtn.setPadding(Insets.EMPTY);
                        m_networkFeeEnterBtn.setMinWidth(25);
                
                        m_networkFeeField = new TextField(m_networkFee.get().getBigDecimalAmount() + "");
                        HBox.setHgrow(m_networkFeeField, Priority.ALWAYS);
                        m_networkFeeField.setAlignment(Pos.CENTER_RIGHT);
                        m_networkFeeField.setId("smallPrimaryColor");

                        m_networkFeeFieldAction = e->{
                            if(m_networkFeeEnterBtn != null){
                                Platform.runLater(()-> m_networkFeeEnterBtn.requestFocus());
                            }
                        };

                        m_networkFeeField.setOnAction(m_networkFeeFieldAction);

                        m_networkFeeTextListener = (obs,oldval,newval)->{
                            if(m_networkFeeField != null){
                                String number = newval.replaceAll("[^0-9.]", "");
                                int index = number.indexOf(".");
                                String leftSide = index != -1 ? number.substring(0, index + 1) : number;
                                String rightSide = index != -1 ?  number.substring(index + 1) : "";
                                rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
                                rightSide = rightSide.length() > ErgoDex.POOL_FEE_MAX_DECIMALS ? rightSide.substring(0, ErgoDex.POOL_FEE_MAX_DECIMALS) : rightSide;
                            
                                m_networkFeeField.setText(leftSide +  rightSide);

                            }
                        };

                        m_networkFeeField.textProperty().addListener(m_networkFeeTextListener);

                        m_networkFeeFocusListener = (obs,oldval,newval)->{
                            if(m_networkFeeField != null){
                                if(!newval){
                                    String str = m_networkFeeField.getText();
                                    
                                    if(Utils.isTextZero(str)){
                                        setNetworkFee(ErgoDex.NETWORK_MIN_FEE.getBigDecimalAmount());
                                    }else{
                                        BigDecimal newFee = new BigDecimal(str);
                                        if(newFee.compareTo(ErgoDex.NETWORK_MIN_FEE.getBigDecimalAmount()) == -1){
                                            setNetworkFee(ErgoDex.NETWORK_MIN_FEE.getBigDecimalAmount());
                                        }else{
                                            setNetworkFee(newFee);
                                        }
                                    }
                                    m_networkFeeField.setText(m_networkFee.get() + "");
                                    if(m_networkFeeFieldBox.getChildren().contains(m_networkFeeEnterBtn)){
                                        m_networkFeeFieldBox.getChildren().remove(m_networkFeeEnterBtn);
                                    }
                                }else{
                                    if(!m_networkFeeFieldBox.getChildren().contains(m_networkFeeEnterBtn)){
                                        m_networkFeeFieldBox.getChildren().add(m_networkFeeEnterBtn);
                                    }
                                }
                            }
                        };
                     

                        m_networkFeeField.focusedProperty().addListener(m_networkFeeFocusListener);

                        
                        m_networkFeeTooltip = new Tooltip("The fee paid to miners to insentivize them to include the transaction in a block. (Minimum: "+ErgoDex.NETWORK_MIN_FEE+" ERG)");
                        m_networkFeeTooltip.setShowDelay(javafx.util.Duration.millis(100));
                        m_networkFeeTooltip.setShowDuration(javafx.util.Duration.seconds(20));

                        m_networkFeeInfoLbl = new Label("ⓘ");
                        m_networkFeeInfoLbl.setId("logoBtn");
                        m_networkFeeInfoLbl.setTooltip(m_networkFeeTooltip);

                        m_networkFeeFieldBox = new HBox(m_networkFeeField);
                        HBox.setHgrow(m_networkFeeFieldBox, Priority.ALWAYS);
                        m_networkFeeFieldBox.setId("darkBox");
                        m_networkFeeFieldBox.setAlignment(Pos.CENTER_LEFT);

                        m_networkFeeBox = new HBox(m_networkFeeInfoLbl, m_networkFeeText, m_networkFeeFieldBox);
                        HBox.setHgrow(m_networkFeeBox, Priority.ALWAYS);
                        m_networkFeeBox.setAlignment(Pos.CENTER_LEFT);
                        m_networkFeeBox.setPadding(new Insets(0,0, 5,0));
                        
                        m_maxSwapFeeText = new Text(String.format("%-15s", "DEX Fee"));
                        m_maxSwapFeeText.setFont(App.smallFont);
                        m_maxSwapFeeText.setFill(App.txtColor);

                        m_maxSwapFeeEnterBtn = new Button("↵");
                        m_maxSwapFeeEnterBtn.setFocusTraversable(true);
                        m_maxSwapFeeEnterBtn.setPadding(Insets.EMPTY);
                        m_maxSwapFeeEnterBtn.setMinWidth(25);

                        m_maxSwapFeeField = new TextField(m_maxExFee.get().getBigDecimalAmount() + "");
                        HBox.setHgrow(m_maxSwapFeeField, Priority.ALWAYS);
                        m_maxSwapFeeField.setAlignment(Pos.CENTER_RIGHT);
                        m_maxSwapFeeField.setId("smallPrimaryColor");

                        m_maxSwapFieldAction = e->{
                            if(m_maxSwapFeeEnterBtn != null){
                                Platform.runLater(()-> m_maxSwapFeeEnterBtn.requestFocus());
                            }
                        };

                        m_maxSwapFeeField.setOnAction(m_maxSwapFieldAction);
                        m_maxSwapFeeTextListener = (obs,oldval,newval)->{
                            if(m_maxSwapFeeField != null){
                                String number = newval.replaceAll("[^0-9.]", "");
                                int index = number.indexOf(".");
                                String leftSide = index != -1 ? number.substring(0, index + 1) : number;
                                String rightSide = index != -1 ?  number.substring(index + 1) : "";
                                rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
                                rightSide = rightSide.length() > ErgoDex.POOL_FEE_MAX_DECIMALS ? rightSide.substring(0, ErgoDex.POOL_FEE_MAX_DECIMALS) : rightSide;
                            
                                m_maxSwapFeeField.setText(leftSide +  rightSide);
                            }
                        };

                        m_maxSwapFeeField.textProperty().addListener(m_maxSwapFeeTextListener);
                        m_maxSwapFeeFocusListener = (obs,oldval,newval)->{
                            if(m_maxSwapFeeField != null){
                                if(!newval){
                                    String str = m_maxSwapFeeField.getText();
                                    
                                    if(!Utils.isTextZero(str)){
                                        BigDecimal newFee = new BigDecimal(str);
                                        if(newFee.compareTo(ErgoDex.SWAP_MIN_MAX_EXEC_FEE.getBigDecimalAmount()) == -1){
                                            if(m_maxExFee.get().getBigDecimalAmount().compareTo(newFee) != 0){
                                                setMaxExFee(ErgoDex.SWAP_MIN_MAX_EXEC_FEE.getBigDecimalAmount());
                                            }
                                        }else{
                                            setMaxExFee(newFee);
                                        }
                                    }
                                    m_maxSwapFeeField.setText(m_maxExFee.get().getBigDecimalAmount() + "");
                                    if(m_maxSwapFeeFieldBox.getChildren().contains(m_maxSwapFeeEnterBtn)){
                                        m_maxSwapFeeFieldBox.getChildren().remove(m_maxSwapFeeEnterBtn);
                                    }
                                }else{
                                    if(!m_maxSwapFeeFieldBox.getChildren().contains(m_maxSwapFeeEnterBtn)){
                                        m_maxSwapFeeFieldBox.getChildren().add(m_maxSwapFeeEnterBtn);
                                    }
                                }
                            }
                        };
                        m_maxSwapFeeField.focusedProperty().addListener(m_maxSwapFeeFocusListener);

                        m_maxSwapTooltip = new Tooltip("The fee paid to execute the swap. Increasing\nthis value will increase the priority and\nmay decrease slippage. (Minimum: " + ErgoDex.SWAP_MIN_EXECUTION_FEE.getBigDecimalAmount().multiply(ErgoDex.DEFAULT_NITRO) +" ERG)");
                        m_maxSwapTooltip.setShowDelay(javafx.util.Duration.millis(100));
                        m_maxSwapTooltip.setShowDuration(javafx.util.Duration.seconds(20));

                        m_maxSwapInfoLbl = new Label("ⓘ");
                        m_maxSwapInfoLbl.setTooltip(m_maxSwapTooltip);
                        m_maxSwapInfoLbl.setId("logoBtn");

                        m_maxSwapFeeFieldBox = new HBox(m_maxSwapFeeField);
                        HBox.setHgrow(m_maxSwapFeeFieldBox, Priority.ALWAYS);
                        m_maxSwapFeeFieldBox.setId("darkBox");
                        m_maxSwapFeeFieldBox.setAlignment(Pos.CENTER_LEFT);

                        m_maxSwapFeeBox = new HBox(m_maxSwapInfoLbl, m_maxSwapFeeText, m_maxSwapFeeFieldBox);
                        HBox.setHgrow(m_maxSwapFeeBox, Priority.ALWAYS);
                        m_maxSwapFeeBox.setAlignment(Pos.CENTER_LEFT);

                        //
                        m_slippageAmountToolTip = new Tooltip("The maximum amount difference resulting from current price and executed price slippage");
                        m_slippageAmountToolTip.setShowDelay(javafx.util.Duration.millis(100));
                        m_slippageAmountToolTip.setShowDuration(javafx.util.Duration.seconds(20));

                        m_slippageAmountInfoLbl = new Label("ⓘ");
                        m_slippageAmountInfoLbl.setTooltip(m_slippageAmountToolTip);
                        m_slippageAmountInfoLbl.setId("logoBtn");

                        m_slippageAmountText = new Text(String.format("%-15s", "Max Slippage"));
                        m_slippageAmountText.setFont(App.smallFont);
                        m_slippageAmountText.setFill(App.txtColor);

                        m_slippageAmountLbl = new Label();
                        m_slippageAmountLbl.setId("smallPrimaryColor");
                        m_slippageAmountLbl.textProperty().bind(Bindings.createObjectBinding(()->{
                            BigDecimal amount = m_inputAmount.get();
                            if(amount != null){
                                BigDecimal slippageAmount = amount.multiply(m_slippageTolerance.get());
                                slippageAmount.setScale(amount.scale(), RoundingMode.FLOOR);
                                return slippageAmount.toPlainString();
                            }
                            return "0";
                        }, m_inputAmount, m_slippageTolerance));

                        m_slippageAmountFieldBox = new HBox(m_slippageAmountLbl);
                        HBox.setHgrow(m_slippageAmountFieldBox, Priority.ALWAYS);
                        m_slippageAmountFieldBox.setAlignment(Pos.CENTER_RIGHT);
                        m_slippageAmountFieldBox.setPadding(new Insets(0,10, 0, 0));

                        m_slippageAmountBox = new HBox(m_slippageAmountInfoLbl, m_slippageAmountText, m_slippageAmountFieldBox);
                        m_slippageAmountBox.setAlignment(Pos.CENTER_LEFT);
                        HBox.setHgrow(m_slippageAmountBox, Priority.ALWAYS);

                        //Min ergo balance

                        m_minErgoToolTip = new Tooltip("The minimum balance required to house tokens. (Warning:\nInactive addresses can become subject to storage/rent.\nBalances going below the minimum threshold can result\nin loss of tokens.)");
                        m_minErgoToolTip.setShowDelay(javafx.util.Duration.millis(100));
                        m_minErgoToolTip.setShowDuration(javafx.util.Duration.seconds(20));

                        m_minErgoInfoLbl = new Label("ⓘ");
                        m_minErgoInfoLbl.setTooltip(m_minErgoToolTip);
                        m_minErgoInfoLbl.setId("logoBtn");

                        m_minErgoText = new Text(String.format("%-15s", "Token Housing"));
                        m_minErgoText.setFont(App.smallFont);
                        m_minErgoText.setFill(App.txtColor);

                        m_minErgoAmountText = new Text(ErgoNetwork.MIN_NETWORK_FEE + " ERG");
                        m_minErgoAmountText.setFont(App.smallFont);
                        m_minErgoAmountText.setFill(App.txtColor);

                        m_minErgoFieldBox = new HBox(m_minErgoAmountText);
                        HBox.setHgrow(m_minErgoFieldBox, Priority.ALWAYS);
                        m_minErgoFieldBox.setAlignment(Pos.CENTER_RIGHT);

                        m_minErgoBox = new HBox(m_minErgoInfoLbl, m_minErgoText, m_minErgoFieldBox);
                        m_minErgoBox.setId("tabsBox");
                        m_minErgoBox.setAlignment(Pos.CENTER_LEFT);
                        HBox.setHgrow(m_minErgoBox, Priority.ALWAYS);

                        m_settingsExtendedBox = new VBox(m_networkFeeBox, m_maxSwapFeeBox, m_slippageAmountBox, m_minErgoBox);
                        m_settingsExtendedBox.setPadding(new Insets(0,5,0,20));
                        
                    }

                    if(!m_settingsBodyPaddingBox.getChildren().contains(m_settingsExtendedBox)){
                        m_settingsBodyPaddingBox.getChildren().add(m_settingsExtendedBox);
                    }
                }else{
                    if(m_settingsExtendedBox != null){

                        if(m_settingsBodyPaddingBox.getChildren().contains(m_settingsExtendedBox)){
                            m_settingsBodyPaddingBox.getChildren().remove(m_settingsExtendedBox);
                        }

                        m_settingsExtendedBox.getChildren().clear();
                        m_networkFeeFieldBox.getChildren().clear();
                        m_networkFeeBox.getChildren().clear();
                        m_networkFeeField.textProperty().removeListener(m_networkFeeTextListener);
                        m_networkFeeField.focusedProperty().removeListener(m_networkFeeFocusListener);
                        m_networkFeeInfoLbl.setTooltip(null);
                        m_networkFeeField.setOnAction(null);

                        m_networkFeeFieldAction = null;
                        m_networkFeeTooltip = null;
                        m_networkFeeInfoLbl = null;
                        m_networkFeeFieldBox = null;
                        m_networkFeeText = null;
                        m_networkFeeField = null;
                        m_networkFeeBox = null;
                        m_networkFeeTextListener = null;
                        m_networkFeeFocusListener = null;

                        m_maxSwapFeeFieldBox.getChildren().clear();
                        m_maxSwapFeeBox.getChildren().clear();
                        m_maxSwapFeeField.textProperty().removeListener(m_maxSwapFeeTextListener);
                        m_maxSwapFeeField.focusedProperty().removeListener(m_maxSwapFeeFocusListener);
                        m_maxSwapInfoLbl.setTooltip(null);
                        m_maxSwapFeeField.setOnAction(null);

                        m_maxSwapFieldAction = null;
                        m_maxSwapFeeText = null;
                        m_maxSwapFeeField = null;
                        m_maxSwapTooltip = null;
                        m_maxSwapInfoLbl = null;
                        m_maxSwapFeeBox = null;
                        m_maxSwapFeeTextListener = null;
                        m_maxSwapFeeFocusListener = null;
                        m_maxSwapFeeFieldBox = null;

                        m_slippageAmountFieldBox.getChildren().clear();
                        m_slippageAmountBox.getChildren().clear();
                        m_slippageAmountLbl.textProperty().unbind();
                        m_slippageAmountInfoLbl.setTooltip(null);
                        m_slippageAmountToolTip = null;
                        m_slippageAmountInfoLbl = null;
                        m_slippageAmountText = null;
                        m_slippageAmountLbl = null;
                        m_slippageAmountFieldBox = null;
                        m_slippageAmountBox = null;

                        m_minErgoFieldBox.getChildren().clear();
                        m_minErgoBox.getChildren().clear();
                        m_minErgoInfoLbl.setTooltip(null);

                        m_minErgoToolTip = null;
                        m_minErgoInfoLbl = null;
                        m_minErgoText = null;
                        m_minErgoAmountText = null;
                        m_minErgoFieldBox = null;
                        m_minErgoBox = null;

                        m_settingsExtendedBox = null;
                    }
                }
            }
            public void shutdown(){
                updateShowDeductions(false);
            }
        }
        
        public class ErgoDexChainData extends VBox{

          
    
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
