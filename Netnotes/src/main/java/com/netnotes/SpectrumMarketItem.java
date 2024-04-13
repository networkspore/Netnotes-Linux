package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.reactfx.util.FxTimer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.netnotes.IconButton.IconStyle;
import com.google.gson.JsonArray;

import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty; 
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;

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
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
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

    private final SimpleBooleanProperty m_isInvertChart = new SimpleBooleanProperty( );

    private int m_positionIndex = 0;
    private double m_prevWidth = -1;
    private double m_prevHeight = -1;
    private double m_prevX = -1;
    private double m_prevY = -1;
    private String m_symbol;

    

    public SpectrumMarketItem(boolean favorite, SpectrumMarketData marketData, SpectrumDataList dataList) {
        m_symbol = marketData.getSymbol();
        m_dataList = dataList;
        m_marketData = marketData;
        m_isInvertChart.set(isInvert());
        m_isFavorite.set(favorite);
       
    }  

    public SimpleBooleanProperty isFavoriteProperty() {
        return m_isFavorite;
    }

    public File getMarketFile() throws IOException{
    
        File marketFile = m_dataList.getSpectrumFinance().getIdDataFile(m_symbol);

        return marketFile;
    }



    public SpectrumMarketData getMarketData() {
        return m_marketData;
    }

    public HBox getRowBox() {



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
      

        ImageView rowImgView = new ImageView();
        rowImgView.setPreserveRatio(true);
       // rowImgView.setContentDisplay(ContentDisplay.LEFT);
       // rowImgView.setAlignment(Pos.CENTER_LEFT);
        //rowImgView.setId("rowBtn");

        HBox hasChart = new HBox();
        hasChart.setMinWidth(10);
        hasChart.setPrefWidth(10);
        hasChart.setMinHeight(15);
        hasChart.setMaxHeight(15);

        hasChart.setId(m_marketData.getPoolId() != null ? "onlineBtn" : "offlineBtn");
        hasChart.setOnMouseClicked(e->{
            if(m_marketData.getPoolId() != null){
                open();
            }
        });

        HBox rowChartBox = new HBox(hasChart);
        HBox.setHgrow(rowChartBox, Priority.ALWAYS);
        rowChartBox.setPadding(new Insets(0,5, 0, 0));
        rowChartBox.setAlignment(Pos.CENTER_RIGHT);

        HBox rowBox = new HBox(favoriteBtn, rowImgView, rowChartBox);
        rowBox.setFocusTraversable(true);
        rowBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e->{
            Platform.runLater(()->rowBox.requestFocus());
            if(e.getClickCount() == 2){
                open();
            }
        });
        //rowImgView.prefWidthProperty().bind(rowBox.widthProperty().subtract(favoriteBtn.widthProperty()));
        rowBox.setId("row");
        rowBox.setAlignment(Pos.CENTER_LEFT);

        Runnable update = ()->{
            Image img = m_dataList.getButtonImage(m_marketData);
            if(img != null){
                rowImgView.setFitWidth(img.getWidth());
                rowImgView.setImage(img);
            }
            hasChart.setId(m_marketData != null ? (m_marketData.getPoolId() != null ? "availableBtn" : "offlineBtn") : "offlineBtn");
        
        };

        m_marketData.getLastUpdated().addListener((obs, oldVal, newVal) -> update.run());

        update.run();
        
        m_dataList.getSortMethod().isTargetSwappedProperty().addListener((obs, oldVal, newVal) -> {
            Image img = m_dataList.getButtonImage(m_marketData);
            if(img != null){
                rowImgView.setImage(img);
                rowImgView.setFitWidth( img.getWidth());
            }
        });

        return rowBox;
    }

    public String returnGetId() {
        return getId();
    }

    public boolean isInvert(){
        SpectrumMarketData data = m_marketData;
        return data.getDefaultInvert() ? !m_dataList.getSortMethod().isTargetSwapped() : m_dataList.getSortMethod().isTargetSwapped();
    }

    public void open(){
        showStage();
    }
    
    public void showStage() {
        if (m_stage == null) {
            
            SimpleBooleanProperty shutdownSwap = new SimpleBooleanProperty(false);
     

            double sceneWidth = 900;
            double sceneHeight = 800;
            final double chartScrollVvalue = 1;
            final double chartScrollHvalue = 1;

            SimpleDoubleProperty chartWidth = new SimpleDoubleProperty(sceneWidth - 50);
            SimpleDoubleProperty chartHeight = new SimpleDoubleProperty(sceneHeight - 170);
            //SimpleDoubleProperty chartHeightOffset = new SimpleDoubleProperty(0);
            SimpleDoubleProperty rangeWidth = new SimpleDoubleProperty(12);
            SimpleDoubleProperty rangeHeight = new SimpleDoubleProperty(100);

          //  double chartSizeInterval = 25;

            SpectrumFinance exchange = m_dataList.getSpectrumFinance();

            m_stage = new Stage();
            m_stage.getIcons().add(SpectrumFinance.getSmallAppIcon());
            m_stage.initStyle(StageStyle.UNDECORATED);
            m_stage.setTitle(exchange.getName() + " - " + m_marketData.getCurrentSymbol(m_isInvertChart.get()) + (m_marketData != null ? " - " +(m_isInvertChart.get() ? m_marketData.getInvertedLastPrice().toString() : m_marketData.getLastPrice()) + "" : ""));

            Button maximizeBtn = new Button();
            Button closeBtn = new Button();
            Button fillRightBtn = new Button();

            HBox titleBox = App.createTopBar(SpectrumFinance.getSmallAppIcon(), fillRightBtn, maximizeBtn, closeBtn, m_stage);

            BufferedMenuButton menuButton = new BufferedMenuButton("/assets/menu-outline-30.png", App.MENU_BAR_IMAGE_WIDTH);
            BufferedButton invertBtn = new BufferedButton(m_dataList.getSortMethod().isTargetSwapped()? "/assets/targetSwapped.png" : "/assets/targetStandard.png", App.MENU_BAR_IMAGE_WIDTH);
            
            invertBtn.setOnAction(e->{
                m_isInvertChart.set(!m_isInvertChart.get());
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

            SpectrumChartView chartView = new SpectrumChartView(chartWidth, chartHeight, new TimeSpan("1day"));
      

  

            MenuButton timeSpanBtn = new MenuButton(chartView.getTimeSpan().getName());
            timeSpanBtn.setFont(App.txtFont);

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

            headingSpacerL.prefWidthProperty().bind(headingBox.widthProperty().subtract(timeSpanBtn.widthProperty().divide(2)).subtract(favoriteBtn.widthProperty()).subtract(headingText.layoutBoundsProperty().get().getWidth()).divide(2));

            ScrollPane chartScroll = new ScrollPane(chartView.getChartBox());

            Platform.runLater(()-> chartScroll.setVvalue(chartScrollVvalue));

            Region headingPaddingRegion = new Region();
            headingPaddingRegion.setMinHeight(5);
            headingPaddingRegion.setPrefHeight(5);

            VBox paddingBox = new VBox(menuBar, headingPaddingRegion, headingBox);
            paddingBox.setPadding(new Insets(0, 5, 0, 5));

            VBox headerVBox = new VBox(titleBox, paddingBox);
            chartScroll.setPadding(new Insets(0, 0, 0, 0));

            
     

            RangeBar chartRange = new RangeBar(rangeWidth, rangeHeight, getNetworksData().getExecService());

            setChartRangeItem.setOnAction((e)->chartRange.toggleSettingRange());

            BufferedButton setRangeBtn = new BufferedButton("/assets/checkmark-25.png");
            setRangeBtn.getBufferedImageView().setFitWidth(15);
            setRangeBtn.setOnAction(e->chartRange.start());
            setRangeBtn.setId("circleGoBtn");
            setRangeBtn.setMaxWidth(20);
            setRangeBtn.setMaxHeight(20);

            Region btnSpacer = new Region();
            btnSpacer.setMinWidth(5);

            BufferedButton cancelRangeBtn = new BufferedButton("/assets/close-outline-white.png");
            cancelRangeBtn.getBufferedImageView().setFitWidth(15);
            cancelRangeBtn.setId("menuBarCircleBtn");
            cancelRangeBtn.setOnAction(e->chartRange.stop());
            cancelRangeBtn.setMaxWidth(20);
            cancelRangeBtn.setMaxHeight(20);

            Text rangeText = new Text(String.format("%-14s", "Price range"));
            rangeText.setFont(App.titleFont);
            rangeText.setFill(App.txtColor);

            HBox chartRangeToolbox = new HBox(rangeText, setRangeBtn, btnSpacer, cancelRangeBtn);
            chartRangeToolbox.setId("bodyBox");
            chartRangeToolbox.setAlignment(Pos.CENTER_LEFT);
            chartRangeToolbox.setPadding(new Insets(0,5,0,5));

            chartView.rangeActiveProperty().bind(chartRange.activeProperty());
            chartView.rangeTopVvalueProperty().bind(chartRange.topVvalueProperty());
            chartView.rangeBottomVvalueProperty().bind(chartRange.bottomVvalueProperty());
        
            chartRange.settingRangeProperty().addListener((obs,oldval,newval)->{
                chartView.setIsSettingRange(newval);
                if(newval){
                    if(!menuAreaBox.getChildren().contains(chartRangeToolbox)){
                        menuAreaBox.getChildren().add(chartRangeToolbox);
                    }
                }else{
                    if(menuAreaBox.getChildren().contains(chartRangeToolbox)){
                        menuAreaBox.getChildren().remove(chartRangeToolbox);
                    }
                }
            });
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

            

   //         chartScroll.maxHeightProperty().bind(marketScene.heightProperty().subtract(headerVBox.heightProperty()).subtract(10));
            chartScroll.prefViewportWidthProperty().bind(marketScene.widthProperty().subtract(45));
            chartScroll.prefViewportHeightProperty().bind(marketScene.heightProperty().subtract(headerVBox.heightProperty()).subtract(10));

            chartHeight.bind(Bindings.createObjectBinding(() ->chartScroll.viewportBoundsProperty().get().getHeight(), chartScroll.viewportBoundsProperty()));
            rangeHeight.bind(marketScene.heightProperty().subtract(headerVBox.heightProperty()).subtract(65));

            chartWidth.bind(marketScene.widthProperty().subtract(50));

           /* chartHeightOffset.addListener((obs, oldVal, newVal) -> {
                chartHeight.set(newVal.doubleValue() - headerVBox.heightProperty().get() - 30 + marketScene.getHeight());
            });*/

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
            
            Runnable setChartScrollRight = () ->{
                Platform.runLater(()->chartScroll.setVvalue(chartScrollVvalue));
                Platform.runLater(()->chartScroll.setHvalue(chartScrollHvalue));
            };
   
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

            // chartHeight.bind(Bindings.add(marketScene.heightProperty(), chartHeightOffset));
            Runnable updateMarketData = () -> {
                SpectrumMarketData marketData = m_marketData;
               
                if (marketData != null) {
                   // m_isInvertChart.get() ? newVal.getInvertedLastPrice() : newVal.getLastPrice();
                    m_stage.setTitle(exchange.getName() + " - " + m_marketData.getCurrentSymbol(m_isInvertChart.get())+ (marketData != null ? " - " + (m_isInvertChart.get() ? marketData.getInvertedLastPrice() : marketData.getLastPrice()) + "" : ""));
                    
                    BigDecimal price =  m_isInvertChart.get() ? marketData.getInvertedLastPrice() : marketData.getLastPrice();;
                    marketData.getTimeStamp();
             
                    chartView.updateMarketData(marketData.getTimeStamp(), price);
                    
                
                  
                   //Utils.formatDateTimeString(Utils.milliToLocalTime(timeStamp);
                   
                    //chartStatusField.setText(timeStamp);
                } else {

                }
            };

            ChangeListener<LocalDateTime> marketDataListener = (obs,oldval,newval)->{
                updateMarketData.run();
     
            };

            m_marketData.getLastUpdated().addListener(marketDataListener);

   

            closeBtn.setOnAction(e -> {
                m_marketData.getLastUpdated().removeListener(marketDataListener);
                shutdownSwap.set(true);
                chartView.shutdown();
                m_stage.close();
                m_stage = null;
            });

            m_stage.setOnCloseRequest(e -> closeBtn.fire());

           // chartBox.requestFocus();

   
            Runnable resetChartHeightOffset = () -> {
                chartScroll.setVvalue(chartScrollVvalue);
            };
            /*
            Runnable increaseChartHeight = () -> {

                chartHeightOffset.set((chartHeightOffset.get() + chartSizeInterval));

            };

            Runnable decreaseChartHeight = () -> {

                if (chartHeightOffset.get() - chartSizeInterval < 0) {
                    chartHeightOffset.set(0);
                } else {
                    chartHeightOffset.set((chartHeightOffset.get() - chartSizeInterval));

                }

            };

            EventHandler<javafx.scene.input.KeyEvent> keyEventHandler = (keyEvent) -> {
                
                switch (keyEvent.getCode()) {
                    case ADD:
                    case PLUS:
                        increaseChartHeight.run();

                        break;
                    case SUBTRACT:
                        decreaseChartHeight.run();

                        break;
                    case BACK_SPACE:
                        resetChartHeightOffset.run();

                        break;
                    case R:
                        chartRange.toggleSettingRange();
                        break;
                    default:
                        break;
                }
            };
            
            zoomOut.setOnAction((action) -> {
                decreaseChartHeight.run();
            });
            zoomIn.setOnAction((action) -> {
                increaseChartHeight.run();
            });*/

           // chartBox.setOnKeyPressed(keyEventHandler);

     
            chartView.setLastTimeStamp(System.currentTimeMillis());

            Runnable setCandles = () ->{
                JsonObject existingObj = null;
                try {
                    existingObj = getMarketFile().isFile() ? Utils.readJsonFile(getAppKey(), getMarketFile()) : null;
                } catch (IOException | JsonParseException | InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                    try {
                        Files.writeString(App.logFile.toPath(), "\nSpectrum Market Data (setCandles.run):" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException  e1) {

                    }
                }
               
                JsonElement priceDataElement = existingObj != null ? existingObj.get("priceData") : null;
                JsonElement lastTimeStampElement = existingObj != null && priceDataElement != null ? existingObj.get("lastTimeStamp") : null; 

                final JsonArray priceDataArray = lastTimeStampElement != null && lastTimeStampElement.isJsonPrimitive() && priceDataElement != null && priceDataElement.isJsonArray() ? priceDataElement.getAsJsonArray() : null;
              //  final long lastTimeStamp = lastTimeStampElement != null && priceDataArray != null  ? lastTimeStampElement.getAsLong() : 0;
                final long currentTime = System.currentTimeMillis();

                m_dataList.getSpectrumFinance().getPoolChart(m_marketData.getPoolId(), (onSuccess)->{
                    Object sourceObject = onSuccess.getSource().getValue();
                
                    if (sourceObject != null && sourceObject instanceof JsonArray) {
                        JsonArray newChartArray = (JsonArray) sourceObject;
                        
                        JsonArray chartArray = priceDataArray != null ? priceDataArray : newChartArray;

                        if(priceDataArray != null){
                            for(int i = 0; i < newChartArray.size() ; i++){
                                JsonElement newDataElement = newChartArray.get(i);
                                JsonObject newDataJson = newDataElement != null && newDataElement.isJsonObject() ? newDataElement.getAsJsonObject() : null;
                                if(newDataJson != null){
                                    chartArray.add(newDataJson);
                                }
                            }
                        }

                       // saveNewDataJson(currentTime, chartArray);
                      
                       
                   
                       
                        chartView.setPriceDataList(m_isInvertChart.get() ?  chartArray : invertPrices(chartArray), currentTime);
                        
                        //Platform.runLater(()->);
               
                        FxTimer.runLater(Duration.ofMillis(200), setChartScrollRight);
                    }else{
                        
                        closeBtn.fire();
                    }
                    
                }, onFailed->{
                 
                    closeBtn.fire();
                });
            };

            
            if(m_marketData != null){
                setCandles.run();  
            }

          
       

            m_isInvertChart.addListener((obs,oldval,newval)->{
                chartView.reset();
                chartRange.reset();
                setCandles.run();
                invertBtn.setImage( new Image(newval? "/assets/targetSwapped.png" : "/assets/targetStandard.png"));
                m_stage.setTitle(exchange.getName() + " - " +  m_marketData.getCurrentSymbol(newval) + (m_marketData != null ? " - " +(newval ? m_marketData.getInvertedLastPrice().toString() : m_marketData.getLastPrice()) + "" : ""));
                headingText.setText(m_marketData.getCurrentSymbol(newval));
            });

            String[] spans = TimeSpan.AVAILABLE_TIMESPANS;

            for (int i = 0; i < spans.length; i++) {

                String span = spans[i];
                TimeSpan timeSpan = new TimeSpan(span);
                MenuItem menuItm = new MenuItem(timeSpan.getName());
                menuItm.setId("urlMenuItem");
                menuItm.setUserData(timeSpan);

                menuItm.setOnAction(action -> {
                  
                    resetChartHeightOffset.run();

                    Object item = menuItm.getUserData();

                    if (item != null && item instanceof TimeSpan) {

                        timeSpanBtn.setUserData(item);
                        timeSpanBtn.setText(((TimeSpan) item).getName());
                        
                    }

                });

                timeSpanBtn.getItems().add(menuItm);

            }

            timeSpanBtn.textProperty().addListener((obs, oldVal, newVal) -> {
                Object objData = timeSpanBtn.getUserData();

                if (newVal != null && !newVal.equals(chartView.getTimeSpan().getName()) && objData != null && objData instanceof TimeSpan) {

                    chartView.setTimeSpan((TimeSpan) objData);

                
                
                    chartRange.reset();
                    chartView.reset();
                    setCandles.run();
      
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
                orderPriceObject.set(m_isInvertChart.get() ? m_marketData.getInvertedLastPrice() : m_marketData.getLastPrice());
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

        ImageView orderPriceImageView = new ImageView(PriceCurrency.getBlankBgIcon(38, m_marketData.getCurrentSymbol(m_isInvertChart.get())));
        

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
      //  swapBox.setPadding(new Insets(5,0,5,0));

     //   SimpleObjectProperty<ChangeListener<LocalDateTime>> orderPriceListener = new SimpleObjectProperty<>(null);
       
       


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
            boolean isInvert = m_isInvertChart.get();
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
            
            boolean invert = m_isInvertChart.get();
           
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

        m_isInvertChart.addListener((obs,oldval, newval)->{
            updateBuySellBtns.run();
            updateBalances.run();
            orderPriceImageView.setImage(PriceCurrency.getBlankBgIcon(38, m_marketData.getCurrentSymbol(m_isInvertChart.get())));
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

    public void saveNewDataJson(long lastTimeStamp, JsonArray jsonArray){
      
     
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
    
        
    }

    public static JsonArray invertPrices(JsonArray jsonArray){
        JsonArray invertedJson = new JsonArray();
        for(int i = 0; i < jsonArray.size() ; i++){
            try{
                SpectrumPrice price = new SpectrumPrice(jsonArray.get(i).getAsJsonObject());
                invertedJson.add(price.getInvertedJson());
            }catch(Exception e){
            
                try {
                    Files.writeString(App.logFile.toPath(), "\nSpectrumMarketItem invertedPrice #" + i + " error: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
         
                }
            }
        }
        return invertedJson;
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
