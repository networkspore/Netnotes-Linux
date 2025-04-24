package com.netnotes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.netnotes.engine.BufferedButton;
import io.netnotes.engine.BufferedMenuButton;
import io.netnotes.engine.ContentTab;
import io.netnotes.engine.networks.ergo.ErgoCurrency;
import io.netnotes.engine.networks.ergo.ErgoWalletControl;
import io.netnotes.engine.NetworksData;
import io.netnotes.engine.NoteConstants;
import io.netnotes.engine.NoteInterface;
import io.netnotes.engine.NoteMsgInterface;
import io.netnotes.engine.PriceAmount;
import io.netnotes.engine.PriceCurrency;
import io.netnotes.engine.Stages;
import io.netnotes.engine.Utils;
import io.netnotes.engine.NetworksData.ManageNetworksTab;
import io.netnotes.friendly_id.FriendlyId;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.ergoplatform.appkit.NetworkType;
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
    private SimpleBooleanProperty m_showSwapFeesBox = new SimpleBooleanProperty(false);

    private TimeSpan m_timeSpan = new TimeSpan("30min");


    private SimpleObjectProperty<LocalDateTime> m_priceLastChecked = new SimpleObjectProperty<>(LocalDateTime.now());
    private String m_defaultWalletId = DEFAULT_ID;

    private SimpleBooleanProperty m_isSellProperty = new SimpleBooleanProperty(true);
    private SimpleObjectProperty<BigDecimal> m_lastPriceProperty = new SimpleObjectProperty<>(null);

   // private ErgoDexFees m_dexFees = null;
   
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

    public ReadOnlyBooleanProperty isSellProperty(){
        return m_isSellProperty;
    }

    public void setIsSell(boolean isSell){
        m_isSellProperty.set(isSell);
        save();
    }

    public ReadOnlyObjectProperty<BigDecimal> lastPriceProperty(){
        return m_lastPriceProperty;
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
        
        BufferedMenuButton menuButton = new BufferedMenuButton("/assets/menu-outline-30.png", Stages.MENU_BAR_IMAGE_WIDTH);
        BufferedButton invertBtn = new BufferedButton( isInvert() ? "/assets/targetSwapped.png" : "/assets/targetStandard.png", Stages.MENU_BAR_IMAGE_WIDTH);
        
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
        headingText.setFont(Stages.txtFont);
        headingText.setFill(Stages.txtColor);


        m_timeSpanBtn = new MenuButton(m_timeSpan.getName() + " ");
        m_timeSpanBtn.setId("arrowMenuButton");
        
      
      
        
        Region headingBoxSpacerR = new Region();
        HBox.setHgrow(headingBoxSpacerR,Priority.ALWAYS);
        Region headingSpacerL = new Region();
        HBox.setHgrow(headingSpacerL,Priority.ALWAYS);

        Text dashText = new Text("  - ");
        dashText.setFont(Stages.txtFont);
        dashText.setFill(Stages.txtColor);

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
        m_toggleSwapBtn.setTextFill(Stages.txtColor);
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
        m_chartBox.minHeightProperty().bind(m_chartScrollHeight.subtract(Stages.VIEWPORT_HEIGHT_OFFSET));

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
        
        addChartViewListener();

        
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
            
        };

        isInvertProperty().addListener(m_invertListener);

        Binding<String> titleBinding = Bindings.createObjectBinding(()->{
            BigDecimal lastPrice = m_lastPriceProperty.get() == null ? BigDecimal.ZERO : m_lastPriceProperty.get();
            boolean isInvert = isInvert();
            String currentSymbol = m_marketData.getCurrentSymbol(isInvert);
            String number =  lastPrice.toPlainString() + "";
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
            JsonElement showSwapElement = json.get("showSwap");
            JsonElement showWalletElement = json.get("showWallet");
            JsonElement defaeultWalletIdElement = json.get("defaultWalletId");
            JsonElement showSwapSettingsElement = json.get("showSwapSettings");
            JsonElement isSellElement = json.get("isSell");
            
            JsonObject timeSpanJson = timeSpanElement != null && timeSpanElement.isJsonObject() ? timeSpanElement.getAsJsonObject() : null;
 
            TimeSpan timeSpan = timeSpanJson != null ? new TimeSpan(timeSpanJson) : new TimeSpan("30min");
            boolean showSwap = showSwapElement != null && !showSwapElement.isJsonNull() ? showSwapElement.getAsBoolean() : true;
            boolean showWallet = showWalletElement != null && !showWalletElement.isJsonNull() ? showWalletElement.getAsBoolean() : true;
            String defaultWalletId = defaeultWalletIdElement == null ?  DEFAULT_ID : (defaeultWalletIdElement.isJsonNull() ? null : defaeultWalletIdElement.getAsString());
            boolean showSwapSettings = showSwapSettingsElement != null ? showSwapSettingsElement.getAsBoolean() : false;
            boolean isSell = isSellElement != null ? isSellElement.getAsBoolean() : true;

           
            m_showSwapFeesBox.set(showSwapSettings);
            m_timeSpan = timeSpan;
            m_showSwap = showSwap;
            m_showWallet.set(showWallet);
            m_defaultWalletId = defaultWalletId;
            m_isSellProperty.set(isSell);
        }
    }

    private JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("defaultWalletId", m_defaultWalletId);
        json.addProperty("showSwap", m_showSwap);
        json.addProperty("showSwapSettings", m_showSwapFeesBox.get());
        json.addProperty("showWallet", m_showWallet.get());
        json.addProperty("isSell", m_isSellProperty.get());
        json.add("timeSpan", m_timeSpan.getJsonObject());
        
        return json;
    }

    public void save(){
        getNetworksData().save("chartData", m_marketData.getId(), ErgoDexDataList.NETWORK_ID, ErgoDex.NETWORK_ID, getJsonObject());
    }


    public void chartUpdated(boolean createChart){
        
        m_priceLastChecked.set(LocalDateTime.now());
        if(createChart){
            createChart();
        }else{
            updateChart();
        }
    }


    public void addChartViewListener(){
        
        String friendlyId = FriendlyId.createFriendlyId();

        ErgoDexChartView chartView = m_marketData.getChartView();

        
        if(chartView != null && m_chartMsgInterface == null){
            
            m_chartMsgInterface = new NoteMsgInterface() {
                public String getId() {
                    return friendlyId;
                }

                public void sendMessage(int code, long timestamp,String networkId, Number num){
                    
                    switch(code){
                        case NoteConstants.STARTED:
                        case NoteConstants.LIST_UPDATED:
                        case NoteConstants.LIST_CHANGED:
                            chartUpdated(true);
                            
                        break;
                        case NoteConstants.LIST_CHECKED:
                            chartUpdated(false);
                        break;
                        case NoteConstants.STOPPED:
                            
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
                m_lastPriceProperty.set(m_numbers.getClose());
            }
        
        }, 
        (onFailed)->{

        });
    }
    
    public void createChart(){

        LocalDateTime now = m_priceLastChecked.get();
        int viewPortHeight = (int) m_chartScrollHeight.get() - Stages.VIEWPORT_HEIGHT_OFFSET;
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
                m_lastPriceProperty.set(m_numbers.getClose());
                drawChart(viewPortWidth, viewPortHeight, timeSpan, now != null ? now : LocalDateTime.now());
                setChartScrollRight();
            }
        
        }, 
        (onFailed)->{

        });
    }

    private void drawChart(int viewPortWidth, int viewPortHeight, TimeSpan timeSpan, LocalDateTime now){
        if(m_numbers == null){
            return;
        }

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
        drawChart((int) m_chartScrollWidth.get(), (int) m_chartScrollHeight.get() - Stages.VIEWPORT_HEIGHT_OFFSET, m_timeSpan, now != null ? now : LocalDateTime.now());
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
        private DexOrderBook m_dexOrderBox = null;
     

        private DexSwapInfoBox m_txInfoBox;
        private TextField orderPriceTextField;

        private Tooltip m_errTip;
    
        private Button m_executeBtn;
        private DropShadow textShadow;

        private Image m_baseCurrencyImage = null;
        private Image m_quoteCurrencyImage = null;


        private ImageView volumeFieldImage = null;
        private ImageView amountFieldImage = null;
        private Button m_sellBtn = null;
        private Button m_buyBtn = null;
        private TextField m_amountField = null;
        private TextField m_volumeField = null;





        public ErgoDexSwapBox(){
            super();
            setMinWidth(SWAP_BOX_MIN_WIDTH);
            VBox.setVgrow(this, Priority.ALWAYS);

            m_errTip = new Tooltip();
            
            textShadow = new DropShadow();
           
            m_baseCurrencyImage = m_marketData.getBaseCurrency().getBackgroundIcon(BG_ICON_HEIGHT);
            m_quoteCurrencyImage = m_marketData.getQuoteCurrency().getBackgroundIcon(BG_ICON_HEIGHT);
            
            m_dexWallet = new ErgoDexWalletBox();
            m_txInfoBox = new DexSwapInfoBox();
 

            m_sellBtn = new Button("Sell");
            m_sellBtn.setId(isSellProperty().get() ? "iconBtnSelected" : "iconBtn");
            m_sellBtn.setOnAction(e->{
                setIsSell(true);
            });
           
            m_buyBtn = new Button("Buy");
            m_buyBtn.setId(isSellProperty().get() ? "iconBtn" : "iconBtnSelected");
            m_buyBtn.setOnAction(e->{
                setIsSell(false);
            });
     

            Region buySellSpacerRegion = new Region();
            VBox.setVgrow(buySellSpacerRegion, Priority.ALWAYS);
    
            HBox buySellBox = new HBox(m_sellBtn, m_buyBtn);
            HBox.setHgrow(buySellBox,Priority.ALWAYS);
    
            m_sellBtn.prefWidthProperty().bind(buySellBox.widthProperty().divide(2));
            m_buyBtn.prefWidthProperty().bind(buySellBox.widthProperty().divide(2));
    
            
            Button orderTypeBtn = new Button("Limit");
            orderTypeBtn.setId("selectedBtn");
    
            HBox orderTypeBtnBox = new HBox(orderTypeBtn);
            HBox.setHgrow(orderTypeBtnBox, Priority.ALWAYS);
        
            orderTypeBtnBox.setAlignment(Pos.CENTER_LEFT);
    
            orderTypeBtn.prefWidthProperty().bind(orderTypeBtnBox.widthProperty().divide(3));
    
            HBox orderTypeBox = new HBox(orderTypeBtnBox);
            HBox.setHgrow(orderTypeBox, Priority.ALWAYS);
            orderTypeBox.setAlignment(Pos.CENTER_LEFT);
        //    orderTypeBox.setPadding(new Insets(5));

            
    
            Text orderPriceText = new Text(String.format("%-9s", "Price"));
            orderPriceText.setFill(Stages.txtColor);
            orderPriceText.setFont(Stages.txtFont);
    
            ImageView orderPriceImageView = new ImageView(PriceCurrency.getBlankBgIcon(BG_ICON_HEIGHT, m_marketData.getCurrentSymbol(isInvert())));
            orderPriceImageView.imageProperty().bind(Bindings.createObjectBinding(()->PriceCurrency.getBlankBgIcon(BG_ICON_HEIGHT, m_marketData.getCurrentSymbol(isInvertProperty().get())), isInvertProperty()));
    
            orderPriceTextField = new TextField();
            HBox.setHgrow(orderPriceTextField, Priority.ALWAYS);
            orderPriceTextField.setAlignment(Pos.CENTER_RIGHT);
            orderPriceTextField.setEffect(textShadow);
            orderPriceTextField.setId("swapFieldStatic");


            orderPriceTextField.focusedProperty().addListener((obs,oldval,newval)->{
                boolean isZero = Utils.isTextZero(orderPriceTextField.getText());
                if(!newval){
                    
                    if(isZero){
                        orderPriceTextField.setText("0");
                    }
                }else{
                    if(isZero){
                        orderPriceTextField.setText("");
                    }
                }
            });
            orderPriceTextField.setOnKeyPressed(e->{
                if (Utils.keyCombCtrZ.match(e) ) { 
                    e.consume();
                }
            });
            orderPriceTextField.textProperty().addListener((obs, oldval, newval)->{
         
                int decimals = m_lastPriceProperty.get() != null ?  m_lastPriceProperty.get().scale() : m_marketData.getQuoteDecimals();
         
                String number = newval.replaceAll("[^0-9.]", "");
                int index = number.indexOf(".");
                String leftSide = index != -1 ? number.substring(0, index + 1) : number;
                String rightSide = index != -1 ?  number.substring(index + 1) : "";
                rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
                rightSide = rightSide.length() > decimals ? rightSide.substring(0, decimals) : rightSide;
                number = (leftSide + rightSide);
                orderPriceTextField.setText(number);

                m_txInfoBox.setOrderPrice(Utils.isTextZero(number) ? BigDecimal.ZERO : new BigDecimal(Utils.formatStringToNumber(number, decimals)));
                
            });
    
            HBox orderPriceTextFieldBox = new HBox(orderPriceTextField);
            HBox.setHgrow(orderPriceTextFieldBox, Priority.ALWAYS);
            orderPriceTextFieldBox.setPadding(new Insets(0,0,3,0));
            orderPriceTextFieldBox.setAlignment(Pos.CENTER_LEFT);

            Label orderPriceCheckedField = new Label();
            orderPriceCheckedField.setPrefWidth(80);
            orderPriceCheckedField.setId("smallSecondaryColor");
            orderPriceCheckedField.textProperty().bind(Bindings.createObjectBinding(()->m_priceLastChecked.get() != null ? Utils.formatTimeString(m_priceLastChecked.get()) : "" , m_priceLastChecked));
    

    
            StackPane orderPriceStackBox = new StackPane(orderPriceImageView, orderPriceTextFieldBox);
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
            amountText.setFill(Stages.txtColor);
            amountText.setFont(Stages.txtFont);
    
            amountFieldImage = new ImageView();
            amountFieldImage.setImage(getAmountImage(isSellProperty().get(), isInvert()));
            
            m_amountField = new TextField();
            HBox.setHgrow(m_amountField, Priority.ALWAYS);
            m_amountField.setId("swapField");
            m_amountField.setAlignment(Pos.CENTER_RIGHT);
            m_amountField.setOnAction(e -> m_executeBtn.fire());
            m_amountField.setEffect(textShadow);
            m_amountField.focusedProperty().addListener((obs,oldval,newval)->{
                boolean isZero = Utils.isTextZero(m_amountField.getText());
                if(!newval){
                    
                    if(isZero){
                        m_amountField.setText("0");
                    }
                }else{
                    if(isZero){
                        m_amountField.setText("");
                    }
                }
            });
            m_amountField.setOnKeyPressed(e->{
                if (Utils.keyCombCtrZ.match(e) ) { 
                    e.consume();
                }
            });
    
            StackPane amountStack = new StackPane(amountFieldImage, m_amountField);
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
            volumeText.setFill(Stages.txtColor);
            volumeText.setFont(Stages.txtFont);
    
            volumeFieldImage = new ImageView();
            volumeFieldImage.setImage(getVolumeImage(isSellProperty().get(), isInvert()));
        
            m_volumeField = new TextField("0");
            HBox.setHgrow(m_volumeField, Priority.ALWAYS);
            m_volumeField.setId("swapField");
            m_volumeField.setEditable(false);
            m_volumeField.setAlignment(Pos.CENTER_RIGHT);
            m_volumeField.setEffect(textShadow);
            m_volumeField.textProperty().bind(Bindings.createObjectBinding(()->m_txInfoBox.volumeProperty().get() + "", m_txInfoBox.volumeProperty()));

            StackPane volumeStack = new StackPane(volumeFieldImage, m_volumeField);
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

          
            
            m_executeBtn = new Button("Loading...");
            m_executeBtn.setPadding(new Insets(5, 15, 5,15));
            m_executeBtn.setOnAction(e->{
                 
                BigDecimal amount = m_txInfoBox.getAmount();
                if(amount == null || amount.equals(BigDecimal.ZERO)){
                    showError("Enter an amount");
                    return;
                }
       
                PriceCurrency amountCurrency = getAmountCurrency();
                PriceCurrency volumeCurrency = getVolumeCurrency();

                BigDecimal dexFeeBigDecimal = m_txInfoBox.getDexFee();
                BigDecimal networkFeeBigDecimal = m_txInfoBox.getNetworkFee();

                boolean isAmountErg = m_txInfoBox.isAmountErg();
            
                int amountDecimals = amountCurrency.getDecimals();  
                        
                long dexFeeLong = PriceAmount.calculateBigDecimalToLong(dexFeeBigDecimal, ErgoCurrency.DECIMALS);
                long baseAmount = PriceAmount.calculateBigDecimalToLong(amount, amountDecimals);
                
                long nanoErgs = (isAmountErg ? baseAmount : 0L) + dexFeeLong;
                long networkFeeNanoErgs = ErgoCurrency.getNanoErgsFromErgs(networkFeeBigDecimal);
    
                PriceAmount[] outputTokens = isAmountErg ? new PriceAmount[0] : new PriceAmount[]{new PriceAmount(amount, amountCurrency) };
    
 
                long dexFee = ErgoCurrency.getNanoErgsFromErgs(dexFeeBigDecimal);
            
                String tokenId = isAmountErg ? volumeCurrency.getTokenId() : amountCurrency.getTokenId();

                NoteInterface ergoInterface = getErgoInterface();

           
            
                String errorString = m_txInfoBox.getExecuteErrorString(ergoInterface, amount, amountCurrency, volumeCurrency, networkFeeBigDecimal, dexFeeBigDecimal);
                if(errorString != null){
                    showError(errorString);
                    return;
                }

                long pricePerToken = m_txInfoBox.getPricePerToken();
                long feePerToken = m_txInfoBox.getFeePerToken();

        

                executeSwap(ergoInterface, isAmountErg, tokenId, outputTokens, baseAmount, pricePerToken, dexFee, feePerToken, nanoErgs, networkFeeNanoErgs, (onComplete)->{
                    Object sourceValue = onComplete.getSource().getValue();
                    if(sourceValue != null && sourceValue instanceof JsonObject){
                        Utils.logJson("**Executed Swap**", (JsonObject)sourceValue );
                    } 
                }, onError->{
                    Throwable exception = onError.getSource().getException();
                    String msg =  exception.getMessage();
                    int length = exception.getStackTrace().length;
                    String stackTrace = "";
                    for(int i = 0; i < length ; i ++){
                        StackTraceElement element = exception.getStackTrace()[i];
                        stackTrace += element.toString() +  "\n";
                    }

                    try {
                        Files.writeString(NoteConstants.logFile.toPath(), exception + "\n" + stackTrace,StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e1) {

                    }



                    showError(exception != null ? msg : "Swap failed");
                });

               

            });
            m_executeBtn.setId("iconBtnSelected");
            

            HBox executeBox = new HBox(m_executeBtn);
            HBox.setHgrow(executeBox,Priority.ALWAYS);
            executeBox.setAlignment(Pos.CENTER);
            executeBox.setMinHeight(40);

    
            HBox executePaddingBox = new HBox(executeBox);
            HBox.setHgrow(executePaddingBox, Priority.ALWAYS);
            executePaddingBox.setPadding(new Insets(10,0, 10, 0));

           
    
            VBox marketBox = new VBox( isBuyPaddingBox, pricePaddingBox, amountPaddingBox, quoteAmountPaddingBox, executePaddingBox, m_txInfoBox);
            marketBox.setPadding(new Insets(5));
            marketBox.setId("bodyBox");
    
        
    
            VBox marketPaddingBox = new VBox(orderTypeBox, marketBox);
    

            double barWidth = 3;
            double barHeight = 3;

            

            m_swapScrollContentBox = new VBox();
            m_swapScrollContentBox.setAlignment(Pos.TOP_CENTER);
            m_swapBoxScroll = new ScrollPane(m_swapScrollContentBox); 


            m_swapBoxScroll.prefViewportWidthProperty().bind(widthProperty().subtract(1)); 
            m_dexOrderBox = new DexOrderBook();
            m_swapScrollContentBox.getChildren().addAll( m_dexWallet, m_dexOrderBox);
   
            heightProperty().addListener((obs,oldval,newval)->{
                double height = newval.doubleValue();
                double boxHeight = marketPaddingBox.heightProperty().get();
                double scrollHeight = height - boxHeight - 1;
                m_swapBoxScroll.setPrefViewportHeight(scrollHeight);

            });
            marketPaddingBox.heightProperty().addListener((obs,oldval,newval)->{
                double height =  heightProperty().get();
                double boxHeight = newval.doubleValue();
                double scrollHeight = height - boxHeight - 1;
                m_swapBoxScroll.setPrefViewportHeight(scrollHeight);   
            });



            m_swapScrollContentBox.setPrefWidth(m_swapBoxScroll.prefViewportWidthProperty().get()-barWidth);
            m_swapScrollContentBox.setMinHeight(m_swapBoxScroll.prefViewportHeightProperty().get()-barHeight);

            m_swapBoxScroll.prefViewportWidthProperty().addListener((obs,oldval,newval)->{
                m_swapScrollContentBox.setPrefWidth(newval.doubleValue() - barWidth);
            });

            m_swapBoxScroll.prefViewportHeightProperty().addListener((obs,oldval,newval)->{
                m_swapScrollContentBox.setMinHeight(newval.doubleValue() - barHeight);
            });

            getChildren().addAll( m_swapBoxScroll, marketPaddingBox);
                
            

            m_priceLastChecked.addListener((obs,oldval,newval)->{});

            isSellProperty().addListener((obs,oldval,newval)->{
                m_sellBtn.setId(isSellProperty().get() ? "iconBtnSelected" : "iconBtn");
                m_buyBtn.setId(isSellProperty().get() ? "iconBtn" : "iconBtnSelected");

               
                update();
            });
     
            isInvertProperty().addListener((obs, oldval, newval)->{
               
                update();
                if(m_dexWallet != null){
                    m_dexWallet.updateBalance();
                }
            });
           
    
            m_amountField.textProperty().addListener((obs, oldval, newval)->{
                PriceCurrency amountCurrency = getAmountCurrency();
                int decimals = amountCurrency.getDecimals();
                String number = newval.replaceAll("[^0-9.]", "");
                int index = number.indexOf(".");
                String leftSide = index != -1 ? number.substring(0, index + 1) : number;
                String rightSide = index != -1 ?  number.substring(index + 1) : "";
                rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
                rightSide = rightSide.length() > decimals ? rightSide.substring(0, decimals) : rightSide;
                number = (leftSide + rightSide);
                m_amountField.setText(number);
                m_txInfoBox.setAmount(Utils.isTextZero(number) ? BigDecimal.ZERO : new BigDecimal(Utils.formatStringToNumber(number, decimals)));
            });

            m_amountField.setText(m_txInfoBox.getAmount().toPlainString());
            m_dexWallet.walletUpdatedProperty().addListener((obs,oldVal,newval)->m_txInfoBox.checkSwap());
            update();
        }

    


        public void update(){
       
            boolean isInvert = isInvertProperty().get();
            boolean isSell = isSellProperty().get();
            
            amountFieldImage.setImage(getAmountImage(isSell, isInvert));
            volumeFieldImage.setImage(getVolumeImage(isSell, isInvert));
            PriceCurrency amountCurrency = getAmountCurrency();
            PriceCurrency volumeCurrency = getVolumeCurrency();
            
            m_executeBtn.setText(isSell ? "Sell " + amountCurrency.getSymbol() : "Buy " + volumeCurrency.getSymbol());

            m_txInfoBox.updateVolume();

        }

      
      
        

        public boolean isNetworkEnabled(){
            return m_dataList.isNetworkEnabled();
        }
        
        public PriceCurrency getAmountCurrency(){
            boolean isInvert = isInvertProperty().get();
            boolean isSell = isSellProperty().get();

            PriceCurrency baseCurrency  = isInvert ? m_marketData.getQuoteCurrency() : m_marketData.getBaseCurrency();
            PriceCurrency quoteCurrency = isInvert ? m_marketData.getBaseCurrency()  : m_marketData.getQuoteCurrency();
            return isSell ? baseCurrency : quoteCurrency;
        }

        public PriceCurrency getVolumeCurrency(){
            boolean isInvert = isInvertProperty().get();
            boolean isSell = isSellProperty().get();
            PriceCurrency baseCurrency  = isInvert ? m_marketData.getQuoteCurrency() : m_marketData.getBaseCurrency();
            PriceCurrency quoteCurrency = isInvert ? m_marketData.getBaseCurrency()  : m_marketData.getQuoteCurrency();
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

        private boolean m_logErrors = true;

        public void showError(String msg){

            if(m_logErrors){
                try {
                    Files.writeString(NoteConstants.logFile.toPath(), msg +"\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }
            }

            int maxLength = 35;
            msg = Utils.formatStringLineLength(msg, maxLength);

            
            double stringWidth =  Utils.computeTextWidth(Stages.txtFont, msg.length() >= maxLength ? String.format("%" +maxLength+"s", "") : msg );
            Point2D p = m_executeBtn.localToScene(0.0, 0.0);
            double x =  p.getX() + m_executeBtn.getScene().getX() + m_executeBtn.getScene().getWindow().getX() + (m_executeBtn.widthProperty().get() / 2) - (stringWidth/2);

            
            m_errTip.setText(msg);
            m_errTip.show(m_executeBtn,
                    x,
                    (p.getY() + m_executeBtn.getScene().getY()
                            + m_executeBtn.getScene().getWindow().getY()) - 40);
            PauseTransition pt = new PauseTransition(javafx.util.Duration.millis(8000));
            pt.setOnFinished(ptE -> {
                m_errTip.hide();
            });
            pt.play();
        }

        public String getLocationId(){
            return m_dataList.getLocationId();
        }

        public ExecutorService getExecService(){
            return getNetworksData().getExecService();
        }

        public NoteInterface getErgoInterface(){
            return m_dataList.ergoInterfaceProperty().get();
        }

        public Future<?> executeSwap(NoteInterface ergoInterface, boolean isAmountErg, String tokenId,  PriceAmount[] outputTokens, long amount, long pricePerToken, long dexFee, long feePerToken, long nanoErgs, long networkFee, EventHandler<WorkerStateEvent> onComplete, EventHandler<WorkerStateEvent> onError){
           

            String walletAddress = m_dexWallet.getErgoWalletControl().getCurrentAddress();

            if(walletAddress == null){
                return Utils.returnException("Wallet is locked", getExecService(), onError);
            }
       
            try {
                Files.writeString(NoteConstants.logFile.toPath(),"walletAddress: " + walletAddress + "\ntokenId: " + tokenId+ "\npricePerToken: " + pricePerToken + "\nfeePerToken: " + feePerToken + "\nnanoErgs: " + nanoErgs,StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }

            JsonObject outputData = null;
            try{
     
                    
                outputData = isAmountErg ? ErgoDexContracts.getBuyTokenOutputData(
                    walletAddress, 
                    tokenId, 
                    pricePerToken, 
                    feePerToken, 
                    nanoErgs
                ) : ErgoDexContracts.getSellTokenOutputData(
                    walletAddress, 
                    tokenId, 
                    pricePerToken, 
                    feePerToken, 
                    outputTokens, 
                    nanoErgs
                );
                
              
            }catch(Exception e){
                return Utils.returnException(e, getExecService(), onError);
            }
            if(outputData != null){ 

                return m_dexWallet.getErgoWalletControl().executeSimpleTransaction(nanoErgs, networkFee, outputTokens, outputData,  onComplete, onError);
            }else{
                return Utils.returnException("This swap has not been enabled yet", getExecService(), onError);
            }
           
        }
       
       



        public void setShowSwapSettings(boolean showSettings){
            m_showSwapFeesBox.set(showSettings);
            save();
        }
    

      
     

        public void shutdown(){
            m_txInfoBox.shutdown();
    
           
        }


        public class ErgoDexWalletBox extends VBox {

           

            private final String emptyAddressString = "[ click to unlock ]";;
            private final String walletBtnDefaultString = "[ select wallet ]";
            private SimpleObjectProperty<PriceAmount> m_ergoAmountProperty = new SimpleObjectProperty<>(null);
            private PriceAmount m_baseAmount = null;
            private PriceAmount m_quoteAmount = null;
    
           
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
            private ImageView m_quoteImgView;
            private ImageView m_baseImgView;
            private TextField baseAmountField;
            private TextField quoteAmountField;
            
            private ErgoWalletControl m_walletControl;
            private SimpleObjectProperty<LocalDateTime> m_walletUpdated = new SimpleObjectProperty<>(null);

            public ErgoWalletControl getErgoWalletControl(){
                return m_walletControl;
            }
    
            private NoteInterface getErgoNetworkInterface(){
                return m_dataList.ergoInterfaceProperty().get();
            }

            public ReadOnlyObjectProperty<PriceAmount> ergoAmountProperty(){
                return m_ergoAmountProperty;
            }

            public PriceAmount getBaseAmount(){
                return m_baseAmount;
            }

            public PriceAmount getQuoteAmount(){
                return m_quoteAmount;
            }

    
            public String getLocationId(){
                return m_dataList.getLocationId();
            }
    
            public ErgoDexWalletBox(){
                int padding = 7;
                Insets rowPadding = new Insets(5,0,5,0);
                Insets rowSpacing = new Insets(0,0,7,0);
                double btnWidth = 257;

                m_walletControl = new ErgoWalletControl(getLocationId(), getErgoNetworkInterface());

                Text headingText = new Text("Ergo Wallet ");
                headingText.setFont(Stages.txtFont);
                headingText.setFill(Stages.txtColor);

                headingText.setMouseTransparent(true);

                HBox headingTextBox = new HBox(headingText);
                HBox.setHgrow(headingTextBox, Priority.ALWAYS);
                headingTextBox.setAlignment(Pos.CENTER_LEFT);

                TextField ergoAmountHeadingField = new TextField();
                
                ergoAmountHeadingField.textProperty().bind(Bindings.createObjectBinding(()->{
                    String walletName = m_walletControl.walletNameProperty().get();
                    PriceAmount ergoAmount =  m_ergoAmountProperty.get();
                    boolean isNetwork = isNetworkEnabled();
                    return ergoAmount != null ? ergoAmount.getAmountString() : walletName != null ? "🔒 Locked" : isNetwork ? "🚫" : "⛔" ;
                }, m_ergoAmountProperty, m_walletControl.walletNameProperty(), m_dataList.isNetworkEnabledProperty()));
                
                ergoAmountHeadingField.prefWidthProperty().bind(Bindings.createObjectBinding(()->{
                    double w = Utils.computeTextWidth(Stages.txtFont, ergoAmountHeadingField.textProperty().get()) + 20;
                    return w < 30 ? 30 : (w > 200 ? 200 : w);
                }, ergoAmountHeadingField.textProperty()));
                

                Text ergText= new Text("");
                ergText.setFont(Stages.openSansTxt);
                ergText.setFill(Stages.txtColor);
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
                        if(m_walletControl.currentAddressProperty().get() != null){
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


                baseAmountField = new TextField();
                HBox.setHgrow(baseAmountField, Priority.ALWAYS);
                baseAmountField.setId("itemLbl");
                baseAmountField.setEditable(false);
                baseAmountField.setAlignment(Pos.CENTER_RIGHT);
            
            
                m_baseImgView = new ImageView();

            
        
                StackPane baseAmountFieldBox = new StackPane(m_baseImgView, baseAmountField);
                baseAmountFieldBox.setId("darkBox");
                baseAmountFieldBox.setPadding(new Insets(0,3,0,0));
                baseAmountFieldBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(baseAmountFieldBox, Priority.ALWAYS);
        
                HBox baseAmountBox = new HBox(baseAmountFieldBox);
                baseAmountBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(baseAmountBox, Priority.ALWAYS);


                quoteAmountField = new TextField();
                HBox.setHgrow(quoteAmountField, Priority.ALWAYS);
                quoteAmountField.setId("itemLbl");
                quoteAmountField.setEditable(false);
                quoteAmountField.setAlignment(Pos.CENTER_RIGHT);

      
                
        
                m_quoteImgView = new ImageView();
                m_quoteImgView.setPreserveRatio(true);
            
        
                StackPane quoteAmountFieldBox = new StackPane(m_quoteImgView, quoteAmountField);
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
    
                m_walletControl.currentAddressProperty().addListener((obs,oldval,newval)->{
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

                update();
                
            }

            public ReadOnlyObjectProperty<LocalDateTime> walletUpdatedProperty(){
                return m_walletUpdated;
            }

            public void update(){

                m_baseImgView.setImage(isInvert() ? m_quoteCurrencyImage : m_baseCurrencyImage);
                m_quoteImgView.setImage(isInvert() ? m_baseCurrencyImage : m_quoteCurrencyImage);

                PriceAmount baseAmount = m_baseAmount;
                baseAmountField.setText(baseAmount != null ? baseAmount.amountProperty().get() + "" : "⸻");

                PriceAmount quoteAmount = m_quoteAmount;
                quoteAmountField.setText(quoteAmount != null ? quoteAmount.amountProperty().get() + "" : "⸻");
                m_walletUpdated.set(LocalDateTime.now());
            }

            public void updateBalance(){
                JsonObject balanceObject = m_walletControl.balanceProperty().get();
                boolean isInverted = isInvert();
                if(balanceObject != null){
                   
                    
                    PriceAmount baseAmount = getBalancePriceAmountByTokenId(isInverted ?  m_marketData.getQuoteId() :  m_marketData.getBaseId());
                    baseAmount = baseAmount == null ?  new PriceAmount(0, isInverted ? m_marketData.getQuoteCurrency() : m_marketData.getBaseCurrency())  : baseAmount;

                    m_baseAmount = baseAmount;
                    
                    PriceAmount quoteAmount = getBalancePriceAmountByTokenId(isInverted ? m_marketData.getBaseId() : m_marketData.getQuoteId());
            
                    quoteAmount = quoteAmount == null ?  new PriceAmount(0, isInverted ?  m_marketData.getBaseCurrency() : m_marketData.getQuoteCurrency())  : quoteAmount;

                    m_quoteAmount = quoteAmount;

                    m_ergoAmountProperty.set(getBalancePriceAmountByTokenId(ErgoCurrency.TOKEN_ID));
                 
                }else{
                   
                    m_baseAmount = null;
                    m_quoteAmount = null;

                    m_ergoAmountProperty.set(null);
                }
               
                update();
            }

            public PriceAmount getAvailablePriceAmountByTokenId(String tokenId){
                if(m_baseAmount != null && m_baseAmount.getTokenId().equals(tokenId)){
                    return m_baseAmount;
                }else if(m_quoteAmount != null && m_quoteAmount.getTokenId().equals(tokenId)){
                    return m_quoteAmount;
                }
                return null;
            }
            
            public PriceAmount getBalancePriceAmountByTokenId(String tokenId){
                JsonObject balanceObject = m_walletControl.balanceProperty().get();

                ArrayList<PriceAmount> priceAmountList = NoteConstants.getBalanceList(balanceObject,true, ErgoDex.NETWORK_TYPE);
                    
                    
                return NoteConstants.getPriceAmountFromList(priceAmountList, tokenId);
              
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
                                case NoteConstants.WALLET_NETWORK:
                                    switch(code){
                                        case NoteConstants.LIST_ITEM_REMOVED:
                                            m_walletControl.walletRemoved(msg);
                                        break;
                                        case NoteConstants.LIST_DEFAULT_CHANGED:
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
                        getNetworksData().openNetwork(NoteConstants.ERGO_NETWORK_ID);
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
                                    m_walletControl.setCurrentAddress(address);
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
        
        public class DexSwapInfoBox extends VBox{
            public final static BigDecimal DEFAULT_MIN_DEX_FEE = NoteConstants.MIN_NETWORK_FEE.multiply(BigDecimal.valueOf(3));
            

            private SimpleObjectProperty<BigDecimal> m_orderPriceProperty = new SimpleObjectProperty<>(BigDecimal.ZERO);
            private SimpleObjectProperty<BigDecimal> m_amountProperty = new SimpleObjectProperty<>(BigDecimal.ZERO);
            private SimpleObjectProperty<BigDecimal> m_volumeProperty = new SimpleObjectProperty<>(BigDecimal.ZERO);
            private SimpleObjectProperty<BigDecimal> m_dexFeeProperty = new SimpleObjectProperty<>(DEFAULT_MIN_DEX_FEE);
            private SimpleObjectProperty<BigDecimal> m_networkFeeProperty = new SimpleObjectProperty<>(NoteConstants.MIN_NETWORK_FEE);

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

            private Text m_swapFeeText = null;
            private TextField m_swapFeeField = null;
            private Tooltip m_swapTooltip;
            private Label m_swapInfoLbl;
            private HBox m_swapFeeBox;
            private HBox m_swapFeeFieldBox;
            private Button m_swapFeeEnterBtn = null;
            private EventHandler<ActionEvent> m_swapFieldAction;
            private ChangeListener<String> m_swapFeeTextListener = null;
            private ChangeListener<Boolean> m_swapFeeFocusListener = null;
            private Binding<String> m_dexFeeBinding;




            private VBox m_settingsExtendedBox = null;
            private SimpleBooleanProperty m_showResetBtn = new SimpleBooleanProperty(false);

            public DexSwapInfoBox(){
                super();
               // m_minErgReq = new SimpleObjectProperty<>(BigDecimal.ZERO);
               

                HBox bodyHGradient = new HBox();
                HBox.setHgrow(bodyHGradient, Priority.ALWAYS);
                bodyHGradient.setId("hGradient");
                bodyHGradient.setMinHeight(2);

            
                Label showFeesBtn = new Label();
                showFeesBtn.setId("caretBtn");
                showFeesBtn.textProperty().bind(Bindings.createObjectBinding(()->m_showSwapFeesBox.get() ? "⏷" : "⏵", m_showSwapFeesBox));
              
                Text feesText = new Text("Fees ");
                feesText.setFont(Stages.smallFont);
                feesText.setFill(Stages.txtColor);
                feesText.setMouseTransparent(true);
        
                TextField totalFeesField = new TextField();
                HBox.setHgrow(totalFeesField, Priority.ALWAYS);
                totalFeesField.setMouseTransparent(true);
                totalFeesField.setId("addressField");
                totalFeesField.setAlignment(Pos.CENTER_RIGHT);
          
                HBox feesFieldBox = new HBox(totalFeesField);
                HBox.setHgrow(feesFieldBox, Priority.ALWAYS);
                feesFieldBox.setMouseTransparent(true);
                feesFieldBox.setAlignment(Pos.CENTER_RIGHT);
                feesFieldBox.setPadding(new Insets(5,5,5,10));

                Tooltip defaultBtnTooltip = new Tooltip("Reset fees");
                defaultBtnTooltip.setShowDelay(javafx.util.Duration.millis(100));

                BufferedButton resetFeesBtn = new BufferedButton("/assets/refresh-white-30.png", Stages.MENU_BAR_IMAGE_WIDTH);
                resetFeesBtn.setOnAction(e->{
                    setDefaultFees();
                });

                HBox deductionsBox = new HBox(showFeesBtn, feesText, feesFieldBox);
                deductionsBox.setPadding(new Insets(5,5,0,5));
                deductionsBox.setAlignment(Pos.CENTER_LEFT);
                deductionsBox.setId("hand");
                deductionsBox.setFocusTraversable(true);
                deductionsBox.setOnMouseClicked(e-> setShowSwapSettings(!m_showSwapFeesBox.get()));

                Binding<Boolean> showResetBtnBinding = Bindings.createObjectBinding(()->{
                    
                    BigDecimal dexFee = m_dexFeeProperty.get();
                    if(dexFee == null){
                        return true;
                    }
                    BigDecimal networkFee = m_networkFeeProperty.get();
                    if(networkFee == null){
                        return true;
                    }
                    if(!networkFee.equals(NoteConstants.MIN_NETWORK_FEE)){
                        return true;
                    }
                    return !dexFee.equals(NoteConstants.MIN_NETWORK_FEE);
                }, m_dexFeeProperty);

                m_showResetBtn.bind(showResetBtnBinding);

                m_showResetBtn.addListener((obs,oldval,newval)->{
                    if(newval){
                        if(!deductionsBox.getChildren().contains(resetFeesBtn)){
                            deductionsBox.getChildren().add(resetFeesBtn);
                        }
                    }else{
                        if(deductionsBox.getChildren().contains(resetFeesBtn)){
                            deductionsBox.getChildren().remove(resetFeesBtn);
                        }
                    }
                });
        
                
                totalFeesField.textProperty().bind(Bindings.createObjectBinding(()->{
      
                    BigDecimal dexFee = m_dexFeeProperty.get() == null ? BigDecimal.ZERO : m_dexFeeProperty.get().stripTrailingZeros();
                    BigDecimal networkFee =  m_networkFeeProperty.get() == null ? BigDecimal.ZERO :  m_networkFeeProperty.get().stripTrailingZeros();
                    return dexFee.add(networkFee).toPlainString() + " ERG";
                }, m_dexFeeProperty, m_networkFeeProperty));

   
                m_settingsBodyPaddingBox = new VBox();
                m_settingsBodyPaddingBox.setPadding(new Insets(2, 5,5,5));

                VBox deductionsPaddingBox = new VBox(bodyHGradient, deductionsBox, m_settingsBodyPaddingBox);
                deductionsPaddingBox.setId("bodyBox");

                getChildren().addAll( deductionsPaddingBox);

                updateShowDeductions(m_showSwapFeesBox.get());

                m_showSwapFeesBox.addListener((obs,oldval,newval)->updateShowDeductions(newval));

            }

            public boolean isAmountErg(){
                PriceCurrency amountCurrency = getAmountCurrency();
                return amountCurrency.getTokenId().equals(ErgoCurrency.TOKEN_ID);
            }

            public PriceCurrency getTokenCurrency(){
                return m_txInfoBox.isAmountErg() ? getVolumeCurrency() : getAmountCurrency();
            }

            public long getPricePerToken(){
                if(isSwapWithoutErg()){
                    return 0;
                }
                
                BigDecimal amount = getAmount();
                BigDecimal volume = getVolume();

                if(amount.equals(BigDecimal.ZERO) || volume.equals(BigDecimal.ZERO)){
                    return 0;
                }

                boolean isAmountErg = isAmountErg();

                BigDecimal tokenBigDecimal = isAmountErg ? volume : amount;
                BigDecimal ergs = isAmountErg ? amount : volume;
                PriceCurrency tokenCurrency = isAmountErg ? getVolumeCurrency() : getAmountCurrency();

                return ErgoDexContracts.getPricePerToken(ergs, tokenBigDecimal, tokenCurrency);
            }

            public long getFeePerToken(){
                boolean isAmountErg = m_txInfoBox.isAmountErg();

                BigDecimal dexFeeBigDecimal = getDexFee();
                BigDecimal tokenBigDecimal = isAmountErg ? getVolume() : getAmount();
                PriceCurrency tokenCurrency = isAmountErg ? getVolumeCurrency() : getAmountCurrency();
                
                return ErgoDexContracts.getFeePerToken(dexFeeBigDecimal, tokenBigDecimal, tokenCurrency);
            }

            public ReadOnlyObjectProperty<BigDecimal> amountProperty(){
                return m_amountProperty;
            }
     
            public ReadOnlyObjectProperty<BigDecimal> volumeProperty(){
                return m_volumeProperty;
            }

            public ReadOnlyObjectProperty<BigDecimal> dexFeeProperty(){
                return m_dexFeeProperty;
            }

            public ReadOnlyObjectProperty<BigDecimal> networkFeeProperty(){
                return m_networkFeeProperty;
            }

            public ReadOnlyObjectProperty<BigDecimal> priceProperty(){
                return m_orderPriceProperty;
            }

            public void setDefaultFees(){
                m_dexFeeProperty.set(NoteConstants.MIN_NETWORK_FEE);
                m_networkFeeProperty.set(NoteConstants.MIN_NETWORK_FEE);
                save();
            }

            public void setNetworkFee(BigDecimal feeBigDecimal){
                m_networkFeeProperty.set(feeBigDecimal);
                save();
            }

            public BigDecimal getNetworkFee(){
                return m_networkFeeProperty.get();
            }

            public void setDexFee(BigDecimal dexFeeBigDecimal){
                m_dexFeeProperty.set(dexFeeBigDecimal);
                save();
            }

            public BigDecimal getDexFee(){
                return m_dexFeeProperty.get();
            }

            public void setVolume(BigDecimal volumeBigDecimal){
                m_volumeProperty.set(volumeBigDecimal);
            }

            public BigDecimal getVolume(){
                return m_volumeProperty.get();
            }

            public void setAmount(BigDecimal amountBigDecimal){
                m_amountProperty.set(amountBigDecimal);
                updateVolume();
            }


            public BigDecimal getAmount(){
                return m_amountProperty.get();
            }

            public BigDecimal getOrderPrice(){
                return m_orderPriceProperty.get();
            }

            public void setOrderPrice(BigDecimal priceBigDecimal){
                m_orderPriceProperty.set(priceBigDecimal);
                updateVolume();
            }

            public void updateVolume(){
            
                BigDecimal volume = calculateVolume();

                setVolume(volume);
            
                checkSwap();
            }

            public BigDecimal calculateVolume(){
                BigDecimal amount = getAmount();
                BigDecimal priceQuote = getOrderPrice();
                if(amount == null || priceQuote == null || priceQuote.equals(BigDecimal.ZERO) || amount.equals(BigDecimal.ZERO)){
                    return BigDecimal.ZERO;
                }
    
                boolean isSell = isSellProperty().get();
                PriceCurrency volumeCurrency = getVolumeCurrency();
                int scale = Math.min(volumeCurrency.getDecimals(), priceQuote.scale());
    
                BigDecimal volume = isSell ? amount.multiply(priceQuote).setScale(scale, RoundingMode.FLOOR) : amount.divide(priceQuote, scale, RoundingMode.FLOOR);
               
                return volume;
            }
    
            public void checkSwap(){
                NoteInterface ergoInterface = getErgoInterface();
                PriceCurrency amountCurrency = getAmountCurrency();
                PriceCurrency volumeCurrency = getVolumeCurrency();
      
                BigDecimal amount = getAmount();
                BigDecimal dexFee = getDexFee();
                BigDecimal networkFee = getNetworkFee();
                boolean isUnlocked = m_dexWallet.getErgoWalletControl().isUnlocked();
    
                if(isUnlocked){
                    String err = getExecuteErrorString(ergoInterface, amount, amountCurrency, volumeCurrency, networkFee, dexFee);
                    if(err != null){
                        m_amountField.setId("swapFieldUnavailable");
                        m_volumeField.setId("swapFieldUnavailable");
                    }else{
                        m_amountField.setId("swapFieldAvailable");
                        m_volumeField.setId("swapFieldAvailable");
                    }
                }else{
                    m_amountField.setId("swapField");
                    m_volumeField.setId("swapField");
                }
                
                
            }

            public boolean isSwapWithoutErg(){
                PriceCurrency amountCurrency = getAmountCurrency();
                PriceCurrency volumeCurrency = getVolumeCurrency();
                String ergoCurrencyId = ErgoCurrency.TOKEN_ID;

                return !amountCurrency.getTokenId().equals(ergoCurrencyId) && !volumeCurrency.getTokenId().equals(ergoCurrencyId);
            }

            public String getExecuteErrorString(NoteInterface ergoInterface, BigDecimal amount, PriceCurrency amountCurrency, PriceCurrency volumeCurrency, BigDecimal networkFee, BigDecimal dexFee){
                String amountTokenId = amountCurrency.getTokenId();
                String amountSymbol = amountCurrency.getSymbol();
    
    
                if(isSwapWithoutErg()){
                    return "Token to token swaps are not currently enabled.";
                }

                NetworkType networkType = m_dexWallet.getErgoWalletControl().getNetworkType();
    
                PriceAmount ergoAmountBalance = m_dexWallet.ergoAmountProperty().get();
                boolean isUnlocked = m_dexWallet.getErgoWalletControl().isUnlocked();
                boolean isNetworkType = networkType != null && networkType == ErgoDex.NETWORK_TYPE;
                boolean isNetwork = ergoInterface != null;
    
                if(!isUnlocked || !isNetworkType || ergoAmountBalance == null){
    
                    return !isNetwork ? "Ergo network disabled" :  (!isUnlocked ? "Wallet is locked" : (ergoAmountBalance == null ? "Wallet balance unavailable": "Wallet network type is " + (networkType == null ? "unavailable" : networkType.toString()) + ": " + ErgoDex.NETWORK_TYPE.toString() + " required"));
                }
    
                if( dexFee == null){
                    return "Fee is invalid";
                }
    
                if(m_txInfoBox.getFeePerToken() < 2){
                    return "Dex fee must be more than 1 nanoERG per token";
                }
    
                PriceAmount amountBalance = m_dexWallet.getAvailablePriceAmountByTokenId(amountTokenId);
    
                if(amountBalance == null){
                    return "Wallet does not contain " + amountCurrency.getSymbol();
                }
    
                BigDecimal walletAmountBalanceBigDecimal = amountBalance.getBigDecimalAmount();
    
    
                BigDecimal ergoFees = networkFee.add(dexFee);
                BigDecimal fees = ergoFees;
    
                String feeCurrencyId = ErgoCurrency.TOKEN_ID;
                String feeCurrencySymbol = ErgoCurrency.SYMBOL;
                
                PriceAmount feeCurrencyBalance = m_dexWallet.getBalancePriceAmountByTokenId(feeCurrencyId);
                
                BigDecimal minHouseingAmount = NoteConstants.MIN_NETWORK_FEE;
    
                BigDecimal feeCurrencyBalanceBigDecimal = feeCurrencyBalance.getBigDecimalAmount();
                BigDecimal ergoBalanceBigDecimal = ergoAmountBalance.getBigDecimalAmount();
    
                boolean isAmountFeeType = amountTokenId.equals(feeCurrencyId);
        
                if(isAmountFeeType && amount.add(ergoFees).add(minHouseingAmount).compareTo(ergoBalanceBigDecimal) == 1){
                    return "Insufficent ERG balance for swap, with fees and token housing.\n(Required: " + amount.add(ergoFees).add(minHouseingAmount) + " ERG)";
                }
    
                if(ergoFees.add(minHouseingAmount).compareTo(ergoBalanceBigDecimal) == 1){
                    return "Insufficent ERG balance for fees and token housing.\n(Required: " + ergoFees.add(minHouseingAmount) + " ERG)";
                }
     
                if(fees.compareTo(feeCurrencyBalanceBigDecimal) == 1){
                    return "Insufficent balance for swap fee\n(Required: " + fees + " " + feeCurrencySymbol + ")";
                }
                if(amount.compareTo(walletAmountBalanceBigDecimal) == 1){
                    return "Insufficent " + amountSymbol + " balance";
                }

                if(getPricePerToken() < 2){
                    return "Order price is invalid";
                }

                if(getFeePerToken() < 2){
                    return "Dex fee is insufficient";
                }

                return null;
            }

            
            public void updateShowDeductions(boolean value){
                
                if(value){
                    if(m_settingsExtendedBox == null){
                        m_networkFeeText = new Text(String.format("%-15s", "Network Fee" ));
                        m_networkFeeText.setFont(Stages.smallFont);
                        m_networkFeeText.setFill(Stages.txtColor);

                        m_networkFeeEnterBtn = new Button("↵");
                        m_networkFeeEnterBtn.setFocusTraversable(true);
                        m_networkFeeEnterBtn.setPadding(Insets.EMPTY);
                        m_networkFeeEnterBtn.setMinWidth(25);
                
                        m_networkFeeField = new TextField(m_networkFeeProperty.get() + "");
                        HBox.setHgrow(m_networkFeeField, Priority.ALWAYS);
                        m_networkFeeField.setAlignment(Pos.CENTER_RIGHT);
                        m_networkFeeField.setId("smallPrimaryColor");

                        m_networkFeeFieldAction = e->{
                            if(m_networkFeeEnterBtn != null){
                                Platform.runLater(()-> m_networkFeeEnterBtn.requestFocus());
                            }
                        };

                        m_networkFeeField.setOnAction(m_networkFeeFieldAction);
                        m_networkFeeField.setOnKeyPressed(e->{
                            if (Utils.keyCombCtrZ.match(e) ) { 
                                e.consume();
                            }
                        });

                        m_networkFeeTextListener = (obs,oldval,newval)->{
                            if(m_networkFeeField != null){
                                String number = newval.replaceAll("[^0-9.]", "");
                                int index = number.indexOf(".");
                                String leftSide = index != -1 ? number.substring(0, index + 1) : number;
                                String rightSide = index != -1 ?  number.substring(index + 1) : "";
                                rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
                                rightSide = rightSide.length() > ErgoCurrency.DECIMALS ? rightSide.substring(0, ErgoCurrency.DECIMALS) : rightSide;
                            
                                m_networkFeeField.setText(leftSide +  rightSide);

                            }
                        };

                        m_networkFeeField.textProperty().addListener(m_networkFeeTextListener);

                        m_networkFeeFocusListener = (obs,oldval,newval)->{
                            if(m_networkFeeField != null){
                                if(!newval){
                                    String str = m_networkFeeField.getText();
                                    BigDecimal defaultFee = NoteConstants.MIN_NETWORK_FEE;
                                    if(Utils.isTextZero(str)){
                                        setNetworkFee(defaultFee);
                                    }else{
                                        
                                        BigDecimal newFee = new BigDecimal(Utils.formatStringToNumber(str, ErgoCurrency.DECIMALS));
                                        if(newFee.compareTo(defaultFee) == -1){
                                            setNetworkFee(defaultFee);
                                        }else{
                                            setNetworkFee(newFee);
                                        }
                                    }
                                    
                                    m_networkFeeField.setText(m_networkFeeProperty.get() + "");
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

                        
                        m_networkFeeTooltip = new Tooltip("The fee paid to miners to insentivize them to include the transaction in a block. (Minimum: "+NoteConstants.MIN_NETWORK_FEE+" ERG)");
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
                        
                        m_swapFeeText = new Text(String.format("%-15s", "DEX Fee"));
                        m_swapFeeText.setFont(Stages.smallFont);
                        m_swapFeeText.setFill(Stages.txtColor);

                        m_swapFeeEnterBtn = new Button("↵");
                        m_swapFeeEnterBtn.setFocusTraversable(true);
                        m_swapFeeEnterBtn.setPadding(Insets.EMPTY);
                        m_swapFeeEnterBtn.setMinWidth(25);

                        m_swapFeeField = new TextField();
                        HBox.setHgrow(m_swapFeeField, Priority.ALWAYS);
                        m_swapFeeField.setAlignment(Pos.CENTER_RIGHT);
                        m_swapFeeField.setId("smallPrimaryColor");

                        m_dexFeeBinding = Bindings.createObjectBinding(()->{
                            BigDecimal dexFee = m_dexFeeProperty.get();
                            return dexFee != null ? dexFee + "" : "";
                        }, m_dexFeeProperty);

                        m_swapFeeField.textProperty().bind(m_dexFeeBinding);

                        m_swapFieldAction = e->{
                            if(m_swapFeeEnterBtn != null){
                                Platform.runLater(()-> m_swapFeeEnterBtn.requestFocus());
                            }
                        };

                        m_swapFeeField.setOnAction(m_swapFieldAction);
                        m_swapFeeField.setOnKeyPressed(e->{
                            if (Utils.keyCombCtrZ.match(e) ) { 
                                e.consume();
                            }
                        });
                        m_swapFeeTextListener = (obs,oldval,newval)->{
                            if(m_swapFeeField != null){
                                int scale =ErgoCurrency.DECIMALS;
                                String number = newval.replaceAll("[^0-9.]", "");
                                int index = number.indexOf(".");
                                String leftSide = index != -1 ? number.substring(0, index + 1) : number;
                                String rightSide = index != -1 ?  number.substring(index + 1) : "";
                                rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
                                rightSide = rightSide.length() > scale ? rightSide.substring(0, scale) : rightSide;
                            
                                m_swapFeeField.setText(leftSide +  rightSide);
                            }
                        };

                        m_swapFeeField.textProperty().bind(m_dexFeeBinding);
                        m_swapFeeFocusListener = (obs,oldval,newval)->{
                            if(m_swapFeeField != null){
                                if(!newval){
                                  

                                    String str = m_swapFeeField.getText();
                                    
                                    if(!Utils.isTextZero(str)){
                                        int scale = ErgoCurrency.DECIMALS;
                                        BigDecimal newFee = new BigDecimal(Utils.formatStringToNumber(str, scale));
                                        
                                        
                                        setDexFee(newFee);
                                        
                                    }else{
                                        setDexFee(BigDecimal.ZERO);
                                    }
                     
                                    if(m_swapFeeFieldBox.getChildren().contains(m_swapFeeEnterBtn)){
                                        m_swapFeeFieldBox.getChildren().remove(m_swapFeeEnterBtn);
                                    }
                                    m_swapFeeField.textProperty().removeListener(m_swapFeeTextListener);
                                    m_swapFeeField.textProperty().bind(m_dexFeeBinding);
                                }else{
                                    m_swapFeeField.textProperty().unbind();
                                    m_swapFeeField.textProperty().addListener(m_swapFeeTextListener);
                                    if(!m_swapFeeFieldBox.getChildren().contains(m_swapFeeEnterBtn)){
                                        m_swapFeeFieldBox.getChildren().add(m_swapFeeEnterBtn);
                                    }
                                }
                            }
                        };
                        m_swapFeeField.focusedProperty().addListener(m_swapFeeFocusListener);

                        m_swapTooltip = new Tooltip("The maximum execution fee to pay in order to give your transaction\npriority. Increasing this value may decrease slippage and ensure execution.");
                        m_swapTooltip.setShowDelay(javafx.util.Duration.millis(100));
                        m_swapTooltip.setShowDuration(javafx.util.Duration.seconds(20));

                        m_swapInfoLbl = new Label("ⓘ");
                        m_swapInfoLbl.setTooltip(m_swapTooltip);
                        m_swapInfoLbl.setId("logoBtn");

                        m_swapFeeFieldBox = new HBox(m_swapFeeField);
                        HBox.setHgrow(m_swapFeeFieldBox, Priority.ALWAYS);
                        m_swapFeeFieldBox.setId("darkBox");
                        m_swapFeeFieldBox.setAlignment(Pos.CENTER_LEFT);

                        m_swapFeeBox = new HBox(m_swapInfoLbl, m_swapFeeText, m_swapFeeFieldBox);
                        HBox.setHgrow(m_swapFeeBox, Priority.ALWAYS);
                        m_swapFeeBox.setAlignment(Pos.CENTER_LEFT);
                        m_swapFeeBox.setPadding(new Insets(0,0,5,0));
                      

                        m_settingsExtendedBox = new VBox(m_networkFeeBox, m_swapFeeBox);
                        m_settingsExtendedBox.setPadding(new Insets(0,5,0,10));
                        
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
                        m_networkFeeEnterBtn.setOnAction(null);
                        m_networkFeeFieldAction = null;
                        m_networkFeeTooltip = null;
                        m_networkFeeInfoLbl = null;
                        m_networkFeeFieldBox = null;
                        m_networkFeeText = null;
                        m_networkFeeField = null;
                        m_networkFeeBox = null;
                        m_networkFeeTextListener = null;
                        m_networkFeeFocusListener = null;
                        m_networkFeeEnterBtn = null;

                        m_swapFeeFieldBox.getChildren().clear();
                        m_swapFeeBox.getChildren().clear();
                        m_swapFeeField.textProperty().unbind();
                        m_swapFeeField.textProperty().removeListener(m_swapFeeTextListener);
                        m_swapFeeField.focusedProperty().removeListener(m_swapFeeFocusListener);
                        m_swapInfoLbl.setTooltip(null);
                        m_swapFeeField.setOnAction(null);
                        m_swapFeeEnterBtn.setOnAction(null);
                        m_dexFeeBinding = null;
                        m_swapFieldAction = null;
                        m_swapFeeText = null;
                        m_swapFeeField = null;
                        m_swapTooltip = null;
                        m_swapInfoLbl = null;
                        m_swapFeeBox = null;
                        m_swapFeeTextListener = null;
                        m_swapFeeFocusListener = null;
                        m_swapFeeFieldBox = null;
                        m_swapFeeEnterBtn = null;

                        m_settingsExtendedBox = null;
                    }
                }
            }
            public void shutdown(){
                updateShowDeductions(false);
            }
        }
        
        public class DexOrderBook extends VBox {
            private ScrollPane m_buyScroll = null;
            private VBox m_buyListBox = null;

            private ScrollPane m_sellScroll = null;
            private VBox m_sellListBox = null;

            private SimpleDoubleProperty m_remainingSpace = new SimpleDoubleProperty(80);

            private String m_buyTemplateHash = null;
            private String m_sellTemplateHash = null;
            
            public DexOrderBook(){
                super();
              

                HBox.setHgrow(DexOrderBook.this, Priority.ALWAYS);

                m_buyListBox = new VBox();
                m_buyListBox.setId("blackBox");

                m_buyScroll = new ScrollPane(m_buyListBox);

                m_buyScroll.setMinViewportHeight(40);


                TextField lastPriceField = new TextField();
                HBox.setHgrow(lastPriceField, Priority.ALWAYS);
                lastPriceField.setId("hand");
                lastPriceField.setOnMouseClicked(e->{
                    orderPriceTextField.textProperty().set(lastPriceField.getText());
                    orderPriceTextField.positionCaret(orderPriceTextField.getText().length()-1);
                    orderPriceTextField.positionCaret(0);
                });

                HBox topPriceHGradient = new HBox();
                HBox.setHgrow(topPriceHGradient, Priority.ALWAYS);
                topPriceHGradient.setId("hGradient");
                topPriceHGradient.setMinHeight(2);

                HBox botPriceHGradient = new HBox();
                HBox.setHgrow(botPriceHGradient, Priority.ALWAYS);
                botPriceHGradient.setId("hGradient");
                botPriceHGradient.setMinHeight(2);

                HBox lastPricePaddingBox = new HBox(lastPriceField);
                HBox.setHgrow(lastPricePaddingBox, Priority.ALWAYS);
                lastPricePaddingBox.setPadding(new Insets(5));
          
                VBox lastPriceFieldBox = new VBox(topPriceHGradient, lastPricePaddingBox, botPriceHGradient);
                HBox.setHgrow(lastPriceFieldBox, Priority.ALWAYS);
                lastPriceFieldBox.setAlignment(Pos.CENTER_RIGHT);

                m_sellListBox = new VBox();
                m_sellListBox.setId("blackBox");


                m_sellScroll = new ScrollPane(m_sellListBox);
    
                m_sellScroll.setMinViewportHeight(40);

                DexOrderBook.this.widthProperty().addListener((obs,oldval,newval)->{
                    double w = newval.doubleValue();
                    m_sellScroll.setPrefViewportWidth(w > 100 ? w-1 : 100);
                    m_buyScroll.setPrefViewportWidth(w > 100 ? w-1 : 100);
      
                });

                m_remainingSpace.addListener((obs,oldval,newval)->{
                    double height = newval.doubleValue() -2;
              
                    double scrollHeight = height / 2;
                    scrollHeight = scrollHeight < 30 ? 30 : scrollHeight;
                    m_sellScroll.setPrefViewportHeight(scrollHeight);
                    m_buyScroll.setPrefViewportHeight(scrollHeight);

                    m_buyListBox.setMinHeight(scrollHeight - 10);
                    m_sellListBox.setMinHeight(scrollHeight - 10);
                });

                Binding<String> lastPriceFieldTextBinding = Bindings.createObjectBinding(()->{
                    BigDecimal lastPrice = m_lastPriceProperty.get();
                    if(lastPrice != null){
                        return lastPrice.toPlainString();
                    }
                    return "";
                }, m_lastPriceProperty);

                lastPriceField.textProperty().bind(lastPriceFieldTextBinding);
                m_remainingSpace.bind(m_swapBoxScroll.prefViewportHeightProperty().subtract(lastPriceFieldBox.heightProperty()).subtract(m_dexWallet.heightProperty()).subtract(30));

                DexOrderBook.this.getChildren().addAll(m_buyScroll, lastPriceFieldBox, m_sellScroll);

            }


           
        }
    
        
    }

   
}
