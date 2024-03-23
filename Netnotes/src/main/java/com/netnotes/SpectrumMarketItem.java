package com.netnotes;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
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

import javafx.event.EventHandler;
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

    private static File logFile = new File("netnotes-log.txt");

    public final static double SWAP_BOX_MIN_WIDTH = 300;
    public final static String ERG_ID = "0000000000000000000000000000000000000000000000000000000000000000";

    public static int FILL_COLOR = 0xffffffff;
    public static java.awt.Color WHITE_COLOR = new java.awt.Color(FILL_COLOR, true);

    private SpectrumDataList m_dataList = null;
    private SpectrumMarketData m_marketData = null;
    private Stage m_stage = null;
    private SimpleBooleanProperty m_isFavorite = new SimpleBooleanProperty(false);

    private SimpleBooleanProperty m_showSwap = new SimpleBooleanProperty(false);

    private double m_prevWidth = -1;
    private double m_prevHeight = -1;
    private double m_prevX = -1;
    private double m_prevY = -1;
    private String m_symbol;

    public SpectrumMarketItem(boolean favorite, SpectrumMarketData marketData, SpectrumDataList dataList) {
        m_symbol = marketData.getSymbol();
        m_dataList = dataList;
        m_marketData = marketData;
        
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
            hasChart.setId(m_marketData != null ? (m_marketData.getPoolId() != null ? "onlineBtn" : "offlineBtn") : "offlineBtn");
        
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
            SimpleBooleanProperty isInvertChart = new SimpleBooleanProperty(isInvert() );

            double sceneWidth = 900;
            double sceneHeight = 800;
            final double chartScrollVvalue = 1;
            final double chartScrollHvalue = 1;

            SimpleDoubleProperty chartWidth = new SimpleDoubleProperty(sceneWidth - 50);
            SimpleDoubleProperty chartHeight = new SimpleDoubleProperty(sceneHeight - 170);
            SimpleDoubleProperty chartHeightOffset = new SimpleDoubleProperty(0);
            SimpleDoubleProperty rangeWidth = new SimpleDoubleProperty(35);
            SimpleDoubleProperty rangeHeight = new SimpleDoubleProperty(100);

            double chartSizeInterval = 25;

            SpectrumFinance exchange = m_dataList.getSpectrumFinance();

            m_stage = new Stage();
            m_stage.getIcons().add(SpectrumFinance.getSmallAppIcon());
            m_stage.initStyle(StageStyle.UNDECORATED);
            m_stage.setTitle(exchange.getName() + " - " + m_marketData.getCurrentSymbol(isInvertChart.get()) + (m_marketData != null ? " - " +(isInvertChart.get() ? m_marketData.getInvertedLastPrice().toString() : m_marketData.getLastPrice()) + "" : ""));

            Button maximizeBtn = new Button();
            Button closeBtn = new Button();
            Button fillRightBtn = new Button();

            HBox titleBox = App.createTopBar(SpectrumFinance.getSmallAppIcon(), fillRightBtn, maximizeBtn, closeBtn, m_stage);

            BufferedMenuButton menuButton = new BufferedMenuButton("/assets/menu-outline-30.png", App.MENU_BAR_IMAGE_WIDTH);
            BufferedButton invertBtn = new BufferedButton(m_dataList.getSortMethod().isTargetSwapped()? "/assets/targetSwapped.png" : "/assets/targetStandard.png", App.MENU_BAR_IMAGE_WIDTH);
            
            invertBtn.setOnAction(e->{
                isInvertChart.set(!isInvertChart.get());
            });
            Region menuSpacer = new Region();
            HBox.setHgrow(menuSpacer, Priority.ALWAYS);

            HBox menuBar = new HBox(menuButton, menuSpacer,  invertBtn);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 5));

            MenuItem setChartRangeItem = new MenuItem("Set price range     [ R ]");
            setChartRangeItem.setId("urlMenuItem");
            MenuItem zoomIn = new MenuItem("Zoom in             [ + ]");
            zoomIn.setId("urlMenuItem");
            MenuItem zoomOut = new MenuItem("Zoom out            [ - ]");
            zoomOut.setId("urlMenuItem");
            MenuItem resetZoom = new MenuItem("Reset zoom  [ Backspace ]");
            resetZoom.setId("urlMenuItem");
            menuButton.getItems().addAll(setChartRangeItem, zoomIn, zoomOut, resetZoom);

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
            HBox chartBox = chartView.getChartBox();

  

            MenuButton timeSpanBtn = new MenuButton(chartView.getTimeSpan().getName());
            timeSpanBtn.setFont(App.txtFont);

            timeSpanBtn.setContentDisplay(ContentDisplay.LEFT);
            timeSpanBtn.setAlignment(Pos.CENTER_LEFT);
            
            Region headingBoxSpacerR = new Region();
            HBox.setHgrow(headingBoxSpacerR,Priority.ALWAYS);
      
            int statusFieldsWidth = 180;

            TextField chartStatusFieldTop = new TextField();
            chartStatusFieldTop.setPrefWidth(statusFieldsWidth);
            chartStatusFieldTop.setId("smallPrimaryColor");
            chartStatusFieldTop.setAlignment(Pos.CENTER_RIGHT);
            chartStatusFieldTop.setEditable(false);
            
            TextField chartStatusFieldTopLine2 = new TextField();
            chartStatusFieldTopLine2.setPrefWidth(statusFieldsWidth);
            chartStatusFieldTopLine2.setId("smallSecondaryColor");
            chartStatusFieldTopLine2.setEditable(false);
            chartStatusFieldTopLine2.setAlignment(Pos.CENTER_RIGHT);

            Runnable setStatusText = () ->{
                SpectrumMarketData marketData = m_marketData;
                if(marketData != null){
                    long timestamp = marketData.getTimeStamp();

                    chartStatusFieldTop.setText(isInvertChart.get() ? marketData.getInvertedLastPrice().toString() : marketData.getLastPrice().toString());
                    chartStatusFieldTopLine2.setText("(" + timestamp + ") " + Utils.formatDateTimeString(marketData.getLastUpdated().get()));
                }
            };
            setStatusText.run();

            VBox chartStatusBox = new VBox(chartStatusFieldTop, chartStatusFieldTopLine2);
            chartStatusBox.setId("bodyBox");
            chartStatusBox.addEventFilter(MouseEvent.MOUSE_CLICKED, (e)->{
                int clickCount = e.getClickCount();
                if(clickCount == 2){
                    
                }
            });

            HBox headingBox = new HBox(favoriteBtn, headingSpacerL, headingText, timeSpanBtn, headingBoxSpacerR, chartStatusBox);
            headingBox.prefHeight(40);
            headingBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headingBox, Priority.ALWAYS);
            headingBox.setPadding(new Insets(5, 5, 5, 5));
            headingBox.setId("headingBox");

            headingSpacerL.prefWidthProperty().bind(headingBox.widthProperty().subtract(timeSpanBtn.widthProperty().divide(2)).subtract(favoriteBtn.widthProperty()).subtract(headingText.layoutBoundsProperty().get().getWidth()).divide(2));

            

            

            ScrollPane chartScroll = new ScrollPane(chartBox);
            Platform.runLater(()-> chartScroll.setVvalue(chartScrollVvalue));

            Region headingPaddingRegion = new Region();
            headingPaddingRegion.setMinHeight(5);
            headingPaddingRegion.setPrefHeight(5);

            VBox paddingBox = new VBox(menuBar, headingPaddingRegion, headingBox);
            paddingBox.setPadding(new Insets(0, 5, 0, 5));

            VBox headerVBox = new VBox(titleBox, paddingBox);
            chartScroll.setPadding(new Insets(0, 0, 0, 0));

            RangeBar chartRange = new RangeBar(rangeWidth, rangeHeight);
           // chartRange.setId("menuBtn");
            chartRange.setVisible(false);

            chartView.rangeActiveProperty().bind(chartRange.activeProperty());
            chartView.rangeTopVvalueProperty().bind(chartRange.topVvalueProperty());
            chartView.rangeBottomVvalueProperty().bind(chartRange.bottomVvalueProperty());
        
            chartRange.settingRangeProperty().addListener((obs,oldval,newval)->{
                chartView.setIsSettingRange(newval);
            });
                        //⯈🗘
            Button toggleSwapBtn = new Button("⯈");
            toggleSwapBtn.setTextFill(App.txtColor);
            toggleSwapBtn.setFont( Font.font("OCR A Extended", 10));
            toggleSwapBtn.setPadding(new Insets(5,5,5,5));
            toggleSwapBtn.setId("barBtn");
            //toggleSwapBtn.setMinWidth(20);
            toggleSwapBtn.setOnAction(e->{
                m_showSwap.set(!m_showSwap.get());
            });

            Region topRegion = new Region();
            VBox.setVgrow(topRegion, Priority.ALWAYS);

          
            HBox swapButtonPaddingBox = new HBox(toggleSwapBtn);
            swapButtonPaddingBox.setId("darkBox");
            swapButtonPaddingBox.setPadding(new Insets(2,0,2,2));

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
   
            Runnable updateShowSwap = ()->{
    
                boolean showSwap = m_showSwap.get();
    
                if( showSwap){
                    toggleSwapBtn.setText("⯈");
                    
                    if(shutdownSwap.get()){
                        shutdownSwap.set(false);
                        swapBoxObject.set( getSwapBox(shutdownSwap));
                    }else{
                        if(swapBoxObject.get() == null){
                            swapBoxObject.set( getSwapBox(shutdownSwap));    
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


            VBox layoutBox = new VBox(headerVBox, bodyPaddingBox);

            Rectangle rect = m_dataList.getNetworksData().getMaximumWindowBounds();

            Scene marketScene = new Scene(layoutBox, sceneWidth, sceneHeight);
            marketScene.setFill(null);
            marketScene.setFill(null);
            marketScene.getStylesheets().add("/css/startWindow.css");
            m_stage.setScene(marketScene);

            chartScroll.maxHeightProperty().bind(marketScene.heightProperty().subtract(headerVBox.heightProperty()).subtract(10));
            chartScroll.prefViewportWidthProperty().bind(marketScene.widthProperty().subtract(45));
            chartScroll.prefViewportHeightProperty().bind(marketScene.heightProperty().subtract(headerVBox.heightProperty()).subtract(10));

            rangeHeight.bind(marketScene.heightProperty().subtract(headerVBox.heightProperty()).subtract(50));

            marketScene.heightProperty().addListener((obs, oldVal, newVal) -> {
                chartHeight.set(newVal.doubleValue() - headerVBox.heightProperty().get() - 30 + chartHeightOffset.get());

            });
            chartWidth.bind(marketScene.widthProperty().subtract(50));

            chartHeightOffset.addListener((obs, oldVal, newVal) -> {
                chartHeight.set(newVal.doubleValue() - headerVBox.heightProperty().get() - 30 + marketScene.getHeight());
            });

            ResizeHelper.addResizeListener(m_stage, 200, 200, rect.getWidth(), rect.getHeight());
            m_stage.show();

            EventHandler<MouseEvent> toggleHeader = (mouseEvent) -> {
                double mouseY = mouseEvent.getY();
                if (mouseY < 10) {
                    if (!headerVBox.getChildren().contains(titleBox)) {
                        headerVBox.getChildren().addAll(titleBox, paddingBox);
                        chartHeightOffset.set(chartHeightOffset.get() == headerVBox.getHeight() ? 0 : chartHeightOffset.get());
                    }
                } else {
                    if (headerVBox.getChildren().contains(titleBox)) {
                        if (mouseY > headerVBox.heightProperty().get()) {
                            headerVBox.getChildren().clear();
                            chartHeightOffset.set(chartHeightOffset.get() == 0 ? headerVBox.getHeight() : chartHeightOffset.get());
                        }
                    }
                }
            };
            
            Runnable setChartScrollRight = () ->{
                Platform.runLater(()->chartScroll.setVvalue(chartScrollVvalue));
                Platform.runLater(()->chartScroll.setHvalue(chartScrollHvalue));
            };

            maximizeBtn.setOnAction((e) -> {
                if (m_prevHeight == -1) {

                    headerVBox.getChildren().clear();
                    chartHeightOffset.set(chartHeightOffset.get() == 0 ? headerVBox.getHeight() : chartHeightOffset.get());
                    marketScene.setOnMouseMoved(toggleHeader);
                    m_prevHeight = m_stage.getHeight();
                    m_prevWidth = m_stage.getWidth();
                    m_prevX = m_stage.getX();
                    m_prevY = m_stage.getY();

                    m_stage.setWidth(rect.getWidth());
                    m_stage.setHeight(rect.getHeight());
                    m_stage.setX(0);
                    m_stage.setY(0);

                } else {
                    m_stage.setWidth(m_prevWidth);
                    m_stage.setHeight(m_prevHeight);
                    m_stage.setX(m_prevX);
                    m_stage.setY(m_prevY);

                    m_prevHeight = -1;
                    m_prevWidth = -1;
                    m_prevX = -1;
                    m_prevY = -1;
                    if (!headerVBox.getChildren().contains(titleBox)) {
                        headerVBox.getChildren().addAll(titleBox, paddingBox);
                        chartHeightOffset.set(chartHeightOffset.get() == headerVBox.getHeight() ? 0 : chartHeightOffset.get());
                    }
                    marketScene.setOnMouseMoved(null);
                }
                 setChartScrollRight.run();
            });

         

            fillRightBtn.setOnAction(e -> {
                if(m_stage.isMaximized()){
                    maximizeBtn.fire();
                }
                if (exchange.getAppStage() != null) {
                    Stage exStage = exchange.getAppStage();
                    if (exStage.getWidth() != m_stage.getX()) {
                        exchange.cmdObjectProperty().set(Utils.getCmdObject("MAXIMIZE_STAGE_LEFT"));

                        m_prevHeight = m_stage.getHeight();
                        m_prevWidth = m_stage.getWidth();
                        m_prevX = m_stage.getX();
                        m_prevY = m_stage.getY();
 
                        m_stage.setWidth(rect.getWidth() - exStage.getWidth());
                        m_stage.setHeight(rect.getHeight());
                        
                        m_stage.setY(0);
                        m_stage.setX(exStage.getWidth());
                      
    

                        setChartScrollRight.run();
                    } 
                } 

            });

            // chartHeight.bind(Bindings.add(marketScene.heightProperty(), chartHeightOffset));
            Runnable updateMarketData = () -> {
                SpectrumMarketData marketData = m_marketData;
               
                if (marketData != null) {
                   // isInvertChart.get() ? newVal.getInvertedLastPrice() : newVal.getLastPrice();
                    m_stage.setTitle(exchange.getName() + " - " + m_marketData.getCurrentSymbol(isInvertChart.get())+ (marketData != null ? " - " + (isInvertChart.get() ? marketData.getInvertedLastPrice() : marketData.getLastPrice()) + "" : ""));
                    long timeStamp = marketData .getTimeStamp();
                    BigDecimal price =  isInvertChart.get() ? marketData.getInvertedLastPrice() : marketData.getLastPrice();;
                    chartView.updateMarketData(timeStamp, price);
                    setStatusText.run();
                   //Utils.formatDateTimeString(Utils.milliToLocalTime(timeStamp);
                   
                    //chartStatusField.setText(timeStamp);
                } else {

                }
            };

            ChangeListener<LocalDateTime> marketDataListener = (obs,oldval,newval)->{
                updateMarketData.run();
     
            };

            m_marketData.getLastUpdated().addListener(marketDataListener);

           
            updateMarketData.run();

            closeBtn.setOnAction(e -> {
                m_marketData.getLastUpdated().removeListener(marketDataListener);
                shutdownSwap.set(true);
                chartView.shutdown();
                m_stage.close();
                m_stage = null;
            });

            m_stage.setOnCloseRequest(e -> closeBtn.fire());

            chartBox.requestFocus();

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

            Runnable resetChartHeightOffset = () -> {
                chartHeightOffset.set(0);
                chartScroll.setVvalue(chartScrollVvalue);
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
            setChartRangeItem.setOnAction((action) -> chartRange.toggleSettingRange());
            zoomOut.setOnAction((action) -> {
                decreaseChartHeight.run();
            });
            zoomIn.setOnAction((action) -> {
                increaseChartHeight.run();
            });

            chartBox.setOnKeyPressed(keyEventHandler);

     
            chartView.setLastTimeStamp(System.currentTimeMillis());

            Runnable setCandles = () ->{
                JsonObject existingObj = null;
                try {
                    existingObj = getMarketFile().isFile() ? Utils.readJsonFile(getAppKey(), getMarketFile()) : null;
                } catch (IOException | JsonParseException | InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                    try {
                        Files.writeString(logFile.toPath(), "\nSpectrum Market Data (setCandles.run):" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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
                      
                       
                   
                       
                        chartView.setPriceDataList(isInvertChart.get() ?  chartArray : invertPrices(chartArray), currentTime);
                        
                        Platform.runLater(()->chartRange.setVisible(true));

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

          
       

            isInvertChart.addListener((obs,oldval,newval)->{
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
                    chartBox.requestFocus();
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

                    chartRange.setVisible(false);
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

    private SimpleBooleanProperty m_isBuyProperty = new SimpleBooleanProperty(true);

    public NetworksData getNetworksData(){
        return m_dataList.getSpectrumFinance().getNetworksData();
    }

    public ErgoNetwork getErgoNetwork(){
        return (ErgoNetwork) getNetworksData().getNoteInterface(ErgoNetwork.NETWORK_ID);
    }

    public VBox getSwapBox(SimpleBooleanProperty shutdownSwap){
        ErgoNetwork ergoNetwork = getErgoNetwork();
        ErgoWallets ergoWallets = ergoNetwork != null ? (ErgoWallets) ergoNetwork.getNetwork(ErgoWallets.NETWORK_ID) : null;
        ErgoNodes ergoNodes = ergoNetwork != null ? (ErgoNodes) ergoNetwork.getNetwork(ErgoNodes.NETWORK_ID) : null;

        SimpleObjectProperty<ErgoWalletDataList> walletsListObject = new SimpleObjectProperty<>();
        SimpleObjectProperty<ErgoWalletData> ergoWalletObject = new SimpleObjectProperty<>(null);
        SimpleObjectProperty<AddressesData> addressesDataObject = new SimpleObjectProperty<>(null);

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
    
        SpectrumMarketData specMarketData = m_marketData;
        SimpleBooleanProperty nodeAvailableObject = new SimpleBooleanProperty(false);
        SimpleStringProperty nodeStatusObject = new SimpleStringProperty(null);


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


        
        final String baseId = specMarketData.getBaseId();
        final String quoteId = specMarketData.getQuoteId();
        final String walletBtnDefaultString = "[Select]           ";

        final String showUrl = "/assets/eye-30.png";
        final String hideUrl = "/assets/eye-off-30.png";
        
        SimpleBooleanProperty showWallet = new SimpleBooleanProperty(true);

        walletBtn.setMaxHeight(40);
        walletBtn.setContentDisplay(ContentDisplay.LEFT);
        walletBtn.setAlignment(Pos.CENTER_LEFT);
        walletBtn.setText(walletBtnDefaultString);
        walletBtn.setMinWidth(90);
        walletBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, e->{
            walletBtn.show();
        });
        Text walletText = new Text(String.format("%-8s","Wallet"));
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

        Text nodeText = new Text(String.format("%-8s","Node"));
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

        Text addressText = new Text(String.format("%-8s", "Address"));
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
        

        Text baseSymbolText = new Text(" " + String.format("%-8s", specMarketData.getBaseSymbol()));
        baseSymbolText.setFont(App.txtFont);
        baseSymbolText.setFill(App.txtColor);

        TextField baseAvailableField = new TextField("");
        baseAvailableField.setFont(App.txtFont);
        baseAvailableField.setEditable(false);


        HBox.setHgrow(baseAvailableField,Priority.ALWAYS);
        //baseAvailableField.setAlignment(Pos.CENTER_RIGHT);
        HBox baseAvailableBox = new HBox(baseSymbolText, baseAvailableField);
        baseAvailableBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(baseAvailableBox, Priority.ALWAYS);

        Text quoteSymbolText = new Text(" " + String.format("%-8s", specMarketData.getQuoteSymbol()));
        quoteSymbolText.setFont(App.txtFont);
        quoteSymbolText.setFill(App.txtColor);

        TextField quoteAvailableField = new TextField("");
        HBox.setHgrow(quoteAvailableField, Priority.ALWAYS);
        quoteAvailableField.setFont(App.txtFont);
        quoteAvailableField.setEditable(false);

        HBox quoteAvailableBox = new HBox(quoteSymbolText, quoteAvailableField);
        quoteAvailableBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(quoteAvailableBox, Priority.ALWAYS);

        VBox selectedAddressBox = new VBox(adrBtnBox);
        HBox.setHgrow(selectedAddressBox,Priority.ALWAYS);

        ImageView walletIconView = new ImageView(ErgoWallets.getSmallAppIcon());
        walletIconView.setFitWidth(20);
        walletIconView.setPreserveRatio(true);

        Label ergoWalletsLbl = new Label("Ergo Walelts");
        ergoWalletsLbl.setFont(App.titleFont);
        ergoWalletsLbl.setTextFill(App.txtColor);
        ergoWalletsLbl.setPadding(new Insets(0, 0, 0, 10));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ImageView closeImage = App.highlightedImageView(App.closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        Button disableWalletBtn = new Button();
        disableWalletBtn.setId("toolBtn");
        disableWalletBtn.setGraphic(closeImage);
        disableWalletBtn.setPadding(new Insets(0, 1, 0, 3));
        disableWalletBtn.setOnAction(e->{
            ergoWalletObject.set(null);
        });
        

        
        BufferedButton toggleShowWallets = new BufferedButton(hideUrl, App.MENU_BAR_IMAGE_WIDTH);
        toggleShowWallets.setId("toolBtn");
        toggleShowWallets.setPadding(new Insets(0, 5, 0, 3));
        toggleShowWallets.setOnAction(e->{
            boolean isShowWallet = !showWallet.get();
            showWallet.set(isShowWallet);
            toggleShowWallets.setImage(isShowWallet ? new Image(hideUrl) : new Image(showUrl));            
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
        selectWalletBox.setPadding(new Insets(0));
        selectWalletBox.setId("networkBox");

        VBox selectWalletPaddingBox = new VBox(walletsTopBar, selectWalletBox);
        selectWalletPaddingBox.setPadding(new Insets(0));
        
        showWallet.addListener((obs,oldVal,newVal)->{
            if(newVal){
                if(!selectWalletPaddingBox.getChildren().contains(selectWalletBox)){
                    selectWalletPaddingBox.getChildren().add(selectWalletBox);
                }
            }else{
                if(selectWalletPaddingBox.getChildren().contains(selectWalletBox)){
                    selectWalletPaddingBox.getChildren().remove(selectWalletBox);
                }
            }
        });


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

        SimpleObjectProperty<PriceAmount> quoteAmountObject = new SimpleObjectProperty<>(null);
        SimpleObjectProperty<PriceAmount> baseAmountObject = new SimpleObjectProperty<>(null);

        Runnable removeAmounts = () ->{
            if(selectedAddressBox.getChildren().contains(quoteAvailableBox)){
                selectedAddressBox.getChildren().remove(quoteAvailableBox);
            }
            if(selectedAddressBox.getChildren().contains(baseAvailableBox)){
                selectedAddressBox.getChildren().remove(baseAvailableBox);
            }
            
            baseAvailableField.setText("");
            quoteAvailableField.setText("");
            quoteAmountObject.set(null);
            baseAmountObject.set(null);
        };

        Runnable updateBalances = () ->{
            if(addressesDataObject.get() != null){
                AddressData adrData = addressesDataObject.get().selectedAddressDataProperty().get();
                if(adrData != null){
                    if(!baseId.equals(ERG_ID)){
                        PriceAmount tokenAmount = adrData.getConfirmedTokenAmount(baseId);
                        baseAmountObject.set(tokenAmount);
                        baseAvailableField.setText(tokenAmount != null ? tokenAmount.getAmountString() : "0");
                    }else{
                        ErgoAmount ergAmount = adrData.ergoAmountProperty().get();
                        baseAmountObject.set(ergAmount);
                        baseAvailableField.setText(ergAmount.getAmountString());
                    }
                    if(!quoteId.equals(ERG_ID)){
                        PriceAmount tokenAmount = adrData.getConfirmedTokenAmount(quoteId);
                        quoteAmountObject.set(tokenAmount);
                        quoteAvailableField.setText(tokenAmount != null ? tokenAmount.toString() : "0");
                    }else{
                        ErgoAmount ergAmount = adrData.ergoAmountProperty().get();
                        quoteAmountObject.set(ergAmount);
                        quoteAvailableField.setText(ergAmount.getAmountString());
                  
                    }
                    if(!selectedAddressBox.getChildren().contains(quoteAvailableBox)){
                        selectedAddressBox.getChildren().add(quoteAvailableBox);
                    }
                    if(!selectedAddressBox.getChildren().contains(baseAvailableBox)){
                        selectedAddressBox.getChildren().add(baseAvailableBox);
                    }
                
                }else{
                    removeAmounts.run();
                }
            }else{
                removeAmounts.run();
            }
        };

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

        Runnable updateNodeBtn = () ->{
            
            AddressesData addressesData = addressesDataObject.get();
            if(addressesData != null){
                ErgoNodeData nodeData = addressesData.selectedNodeData().get();

                
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
                    ErgoNodeData selectedErgoNodeData = newval.selectedNodeData().get();
                    selectedErgoNodeData.isAvailableProperty().removeListener(isAvailableListener);
                    selectedErgoNodeData.statusString().removeListener(statusListener);

                
                }
                oldval.shutdown();
            }

            if(newval == null){
                
                if(selectedAddressBox.getChildren().contains(nodeBox)){
                    selectedAddressBox.getChildren().remove(nodeBox);
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
                removeAmounts.run();
            }else{
                if(!selectedAddressBox.getChildren().contains(nodeBox)){
                    selectedAddressBox.getChildren().add(nodeBox);
                   
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
                updateBalances.run();
            }
        });

        walletsListObject.addListener((obs,oldVal,newVal)->{});

        

        
        Slider amountSlider = new Slider(0, 100, 0);
        amountSlider.setShowTickLabels(true);
        amountSlider.setShowTickMarks(true);
    
        TextField amountField = new TextField();
        amountField.setId("darkField");
        amountField.setAlignment(Pos.CENTER_RIGHT);
        amountField.setPromptText("(none)");

        Region amountSpacerRegion = new Region();
        amountSpacerRegion.setMinWidth(8);

        HBox amountBox = new HBox(amountSlider,amountSpacerRegion, amountField);
        HBox.setHgrow(amountBox, Priority.ALWAYS);
        amountSlider.prefWidthProperty().bind(amountBox.widthProperty().subtract(60));

        Button passwordBtn = new Button("[Authorization]");
        passwordBtn.setId("");

       // Button okBtn = new Button("Ok");

        Button buyBtn = new Button("Buy");
        buyBtn.setOnAction(e->{
            m_isBuyProperty.set(true);
        });

        Button sellBtn = new Button("Sell");
        sellBtn.setOnAction(e->{
            m_isBuyProperty.set(false);
        });

        Region buySellSpacerRegion = new Region();
        VBox.setVgrow(buySellSpacerRegion, Priority.ALWAYS);

        HBox buySellBox = new HBox(buyBtn, sellBtn);
        HBox.setHgrow(buySellBox,Priority.ALWAYS);

        buyBtn.prefWidthProperty().bind(buySellBox.widthProperty().divide(2));
        sellBtn.prefWidthProperty().bind(buySellBox.widthProperty().divide(2));

        Runnable updateBuySellBtns = ()->{
            boolean isBuy = m_isBuyProperty.get();
            buyBtn.setId(isBuy ? "iconBtnSelected" : "iconBtn");
            sellBtn.setId(isBuy ? "iconBtn" : "iconBtnSelected");
        };

        updateBuySellBtns.run();

        m_isBuyProperty.addListener((obs,oldval,newval)->updateBuySellBtns.run());

        shutdownSwap.addListener((obs,oldval,newval)->{
            if(newval){
                AddressesData addressesData = addressesDataObject.get();
                if(addressesData != null){
                    addressesData.shutdown();
                }
            }
        });


  
        VBox swapBox = new VBox(selectWalletPaddingBox, buySellSpacerRegion, amountBox, buySellBox);
        VBox.setVgrow(swapBox, Priority.ALWAYS);
        swapBox.setMinWidth(SWAP_BOX_MIN_WIDTH);
        swapBox.setPadding(new Insets(5));
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
                    Files.writeString(logFile.toPath(), "\nSpectrumMarketItem (saveNewDataJson): " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND );
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
                    Files.writeString(logFile.toPath(), "\nSpectrumMarketItem invertedPrice #" + i + " error: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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
