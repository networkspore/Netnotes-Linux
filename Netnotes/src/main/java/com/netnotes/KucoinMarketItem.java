package com.netnotes;

import java.awt.FontMetrics;
import java.awt.Graphics2D;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.Duration;

import org.reactfx.util.FxTimer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.netnotes.engine.BufferedButton;
import io.netnotes.engine.BufferedMenuButton;
import io.netnotes.engine.Drawing;
import io.netnotes.engine.IconButton;
import io.netnotes.engine.NoteConstants;
import io.netnotes.engine.NoteInterface;
import io.netnotes.engine.ResizeHelper;
import io.netnotes.engine.Stages;
import io.netnotes.engine.apps.RangeBar;
import io.netnotes.engine.apps.TimeSpan;

import com.google.gson.JsonArray;


import javafx.application.Platform;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.Scene;

import javafx.scene.control.Button;

import javafx.scene.control.ContentDisplay;

import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class KucoinMarketItem {


    private String m_id;
    private String m_symbol;
    private String m_name;

    private KuCoinDataList m_dataList = null;
    private SimpleObjectProperty<KucoinTickerData> m_tickerDataProperty = new SimpleObjectProperty<>(null);
    private Stage m_stage = null;
    private SimpleBooleanProperty m_isFavorite = new SimpleBooleanProperty(false);

    private double m_prevWidth = -1;
    private double m_prevHeight = -1;
    private double m_prevX = -1;
    private double m_prevY = -1;
    private int m_positionIndex = 0;

    private TimeSpan m_timeSpan = new TimeSpan("30min");

    private NoteInterface m_parentInterface;

    public KucoinMarketItem(NoteInterface parentInterface, String id, String symbol, String name, boolean favorite, KucoinTickerData tickerData, KuCoinDataList dataList) {
        m_parentInterface = parentInterface;
        m_id = id;
        m_symbol = symbol;
        m_name = name;
        m_dataList = dataList;
        m_tickerDataProperty.set(tickerData);
        m_isFavorite.set(favorite);

    }

    public SimpleBooleanProperty isFavoriteProperty() {
        return m_isFavorite;
    }

    public String getId() {
        return m_id;
    }

    public String getName() {
        return m_name;
    }

    public TimeSpan getTimeSpan() {
        return m_timeSpan;
    }

    public SimpleObjectProperty<KucoinTickerData> tickerDataProperty() {
        return m_tickerDataProperty;
    }

    public HBox getRowBox() {

        KucoinTickerData data = m_tickerDataProperty.get();

        Button favoriteBtn = new Button();
        favoriteBtn.setId("menuBtn");
        favoriteBtn.setGraphic(IconButton.getIconView(new Image(m_isFavorite.get() ? "/assets/star-30.png" : "/assets/star-outline-30.png"), 30));
        favoriteBtn.setOnAction(e -> {
            boolean newVal = !m_isFavorite.get();
            m_isFavorite.set(newVal);
            if (newVal) {
                m_dataList.addFavorite(m_symbol, true);
            } else {
                m_dataList.removeFavorite(m_symbol, true);
            }
        });

        m_isFavorite.addListener((obs, oldVal, newVal) -> {
            favoriteBtn.setGraphic(IconButton.getIconView(new Image(m_isFavorite.get() ? "/assets/star-30.png" : "/assets/star-outline-30.png"), 30));
        });
        Image buttonImage = data == null ? null : getButtonImage(data);

        Button rowButton = new IconButton() {
            @Override
            public void open() {
                showStage();
            }
        };
        rowButton.setContentDisplay(ContentDisplay.LEFT);
        rowButton.setAlignment(Pos.CENTER_LEFT);
        rowButton.setGraphic(buttonImage == null ? null : IconButton.getIconView(buttonImage, buttonImage.getWidth()));
        rowButton.setId("rowBtn");

        HBox rowBox = new HBox(favoriteBtn, rowButton);

        rowButton.prefWidthProperty().bind(rowBox.widthProperty().subtract(favoriteBtn.widthProperty()));

        rowBox.setAlignment(Pos.CENTER_LEFT);

        m_tickerDataProperty.addListener((obs, oldVal, newVal) -> {
            Image img = newVal == null ? null : getButtonImage(newVal);
            rowButton.setGraphic(img == null ? null : IconButton.getIconView(img, img.getWidth()));
        });

        return rowBox;
    }

    public static int FILL_COLOR = 0xffffffff;
    public static java.awt.Color WHITE_COLOR = new java.awt.Color(FILL_COLOR, true);

    private Image getButtonImage(KucoinTickerData data) {
        if (data == null) {
            return null;
        }
        int height = 30;
        int symbolColWidth = 160;

        String symbolString = String.format("%-18s", data.getSymbol());
        String lastString = data.getLastString();

        boolean positive = data.getChangeRate() > 0;
        boolean neutral = data.getChangeRate() == 0;

        java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 15);

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);

        FontMetrics fm = g2d.getFontMetrics();

       // int symbolWidth = fm.stringWidth(symbolString);
        int lastWidth = fm.stringWidth(lastString);
        int fontAscent = fm.getAscent();
        int fontHeight = fm.getHeight();
        int stringY = ((height - fontHeight) / 2) + fontAscent;
        int colPadding = 5;

        BufferedImage symbolImage = new BufferedImage(symbolColWidth, height, BufferedImage.TYPE_INT_ARGB);
        g2d = symbolImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setFont(font);
        g2d.setColor(WHITE_COLOR);
        g2d.drawString(symbolString, 0, stringY);
    
        img = new BufferedImage(symbolColWidth + colPadding + lastWidth, height, BufferedImage.TYPE_INT_ARGB);
        g2d = img.createGraphics();
  
        Drawing.drawImageExact(img, symbolImage, 0, 0, false);

        g2d.setFont(font);
        g2d.setColor(WHITE_COLOR);
        

        if (neutral) {

            g2d.drawString(lastString, symbolColWidth + colPadding, stringY);

        } else {

            g2d.drawString(lastString, symbolColWidth + colPadding, stringY);

            int x1 = symbolColWidth + colPadding;
            int y1 = (height / 2) - (fontHeight / 2);
            int x2 = x1 + lastWidth;
            int y2 = y1 + fontHeight;
            java.awt.Color color1 = positive ? Drawing.POSITIVE_COLOR : Drawing.NEGATIVE_HIGHLIGHT_COLOR;
            java.awt.Color color2 = positive ? Drawing.POSITIVE_HIGHLIGHT_COLOR : Drawing.NEGATIVE_COLOR;

            Drawing.drawBarFillColor(positive ? 0 : 1, false, FILL_COLOR, color1.getRGB(), color2.getRGB(), img, x1, y1, x2, y2);

        }

        g2d.dispose();

        return SwingFXUtils.toFXImage(img, null);
    }

    public String returnGetId() {
        return getId();
    }

    public void showStage() {
        if (m_stage == null) {
          
            double sceneWidth = 750;
            double sceneHeight = 800;
            final double chartScrollVvalue = 1;
            final double chartScrollHvalue = 1;

            SimpleDoubleProperty chartWidth = new SimpleDoubleProperty(sceneWidth - 50);
            SimpleDoubleProperty chartHeight = new SimpleDoubleProperty(sceneHeight - 170);
            SimpleDoubleProperty chartHeightOffset = new SimpleDoubleProperty(0);
            SimpleDoubleProperty rangeWidth = new SimpleDoubleProperty(12);
            SimpleDoubleProperty rangeHeight = new SimpleDoubleProperty(100);

            double chartSizeInterval = 25;

            KucoinExchange exchange = m_dataList.getKucoinExchange();

            m_stage = new Stage();
            m_stage.getIcons().add(m_dataList.getKucoinExchange().getSmallAppIcon());
            m_stage.initStyle(StageStyle.UNDECORATED);
            m_stage.setTitle(exchange.getName() + " - " + m_name + (m_tickerDataProperty.get() != null ? " - " + m_tickerDataProperty.get().getLastString() + "" : ""));

            Button maximizeBtn = new Button();
            Button closeBtn = new Button();
            Button fillRightBtn = new Button();

            HBox titleBox = Stages.createTopBar(m_dataList.getKucoinExchange().getSmallAppIcon(), fillRightBtn, maximizeBtn, closeBtn, m_stage);

            BufferedMenuButton menuButton = new BufferedMenuButton("/assets/menu-outline-30.png", Stages.MENU_BAR_IMAGE_WIDTH);

            HBox menuAreaBox = new HBox();
            HBox.setHgrow(menuAreaBox, Priority.ALWAYS);
            menuAreaBox.setAlignment(Pos.CENTER_LEFT);

            HBox menuBar = new HBox(menuButton, menuAreaBox);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 5));

            MenuItem setChartRangeItem = new MenuItem("Set price range   ");
            setChartRangeItem.setId("urlMenuItem");
  
            menuButton.getItems().addAll(setChartRangeItem);

            Button favoriteBtn = new Button();
            favoriteBtn.setId("menuBtn");
            favoriteBtn.setContentDisplay(ContentDisplay.LEFT);
            favoriteBtn.setGraphic(IconButton.getIconView(new Image(m_isFavorite.get() ? "/assets/star-30.png" : "/assets/star-outline-30.png"), 30));
            favoriteBtn.setOnAction(e -> {
                boolean newVal = !m_isFavorite.get();
                m_isFavorite.set(newVal);
                if (newVal) {
                    m_dataList.addFavorite(m_symbol, true);
                } else {
                    m_dataList.removeFavorite(m_symbol, true);
                }
            });

            m_isFavorite.addListener((obs, oldVal, newVal) -> {
                favoriteBtn.setGraphic(IconButton.getIconView(new Image(m_isFavorite.get() ? "/assets/star-30.png" : "/assets/star-outline-30.png"), 30));
            });

            Text headingText = new Text(m_name + "  - ");
            headingText.setFont(Stages.txtFont);
            headingText.setFill(Color.WHITE);

            Region headingSpacerL = new Region();

            MenuButton timeSpanBtn = new MenuButton(m_timeSpan.getName());
            timeSpanBtn.setFont(Stages.txtFont);

            timeSpanBtn.setContentDisplay(ContentDisplay.LEFT);
            timeSpanBtn.setAlignment(Pos.CENTER_LEFT);

            HBox headingBox = new HBox(favoriteBtn, headingSpacerL, headingText, timeSpanBtn);
            headingBox.prefHeight(40);
            headingBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headingBox, Priority.ALWAYS);
            headingBox.setPadding(new Insets(5, 5, 5, 5));
            headingBox.setId("headingBox");

            headingSpacerL.prefWidthProperty().bind(headingBox.widthProperty().subtract(timeSpanBtn.widthProperty().divide(2)).subtract(favoriteBtn.widthProperty()).subtract(headingText.layoutBoundsProperty().get().getWidth()).divide(2));
            /*
            TextArea informationTextArea = new TextArea();

            Label emissionLbl = new Label();
            TextField emissionAmountField = new TextField();*/

            ChartView chartView = new ChartView(chartWidth, chartHeight);

            HBox chartBox = chartView.getChartBox();

            ScrollPane chartScroll = new ScrollPane(chartBox);
            Platform.runLater(()-> chartScroll.setVvalue(chartScrollVvalue));

            int symbolLength = getSymbol().length();

            MessageInterface msgInterface = new MessageInterface() {
                public String getSubject() {
                    return null;
                }

                public String getTopic() {
                    return null;
                }

                @Override
                public String getTunnelId() {
                    return KucoinExchange.NETWORK_ID;
                }

                @Override
                public String getId() {
                    return m_id;
                }

                @Override
                public void onMsgChanged(JsonObject newVal) {
                    if (newVal != null) {

                        JsonElement subjectElement = newVal.get(NoteConstants.CMD);
                        JsonElement topicElement = newVal.get("topic");
                        JsonElement dataElement = newVal.get("data");

                        String subject = subjectElement != null && subjectElement.isJsonPrimitive() ? subjectElement.getAsString() : null;
                        String topic = topicElement != null && topicElement.isJsonPrimitive() ? topicElement.getAsString() : null;

                        if (subject != null && topic != null) {
                            switch (subject) {
                                case "trade.candles.update":
                                    String topicHeader = "/market/candles:";
                                    String topicBody = topic.substring(topicHeader.length());
                                    int indexOfunderscore = topicBody.indexOf("_");

                                    if (topicHeader.equals(topic.substring(0, topicHeader.length())) && symbolLength == indexOfunderscore && getSymbol().equals(topicBody.substring(0, indexOfunderscore))) {

                                        String timeSpanId = topicBody.substring(indexOfunderscore + 1);

                                        if (m_timeSpan.getId().equals(timeSpanId)) {
                                            JsonObject dataObject = dataElement != null && dataElement.isJsonObject() ? dataElement.getAsJsonObject() : null;
                                            if (dataObject != null) {
                                                JsonElement candlesElement = dataObject.get("candles");

                                                JsonArray dataArray = candlesElement != null && candlesElement.isJsonArray() ? candlesElement.getAsJsonArray() : null;

                                                if (dataArray != null) {

                                                    PriceData priceData = new PriceData(dataArray);

                                                    chartView.updateCandleData(priceData, m_timeSpan.getSeconds());

                                                    m_stage.setTitle(exchange.getName() + " - " + m_name + (newVal != null ? " - " + priceData.getCloseString() + "" : ""));
                                                }

                                            }
                                        }

                                    }
                                    break;
                            }
                        }
                    }
                }

                @Override
                public void onReady() {
                    exchange.subscribeToCandles(m_parentInterface.getNetworkId(), m_symbol, m_timeSpan.getId());
                }

            };

            exchange.addListener(msgInterface);

            Region headingPaddingRegion = new Region();
            headingPaddingRegion.setMinHeight(5);
            headingPaddingRegion.setPrefHeight(5);

            VBox paddingBox = new VBox(menuBar, headingPaddingRegion, headingBox);
            paddingBox.setPadding(new Insets(0, 5, 0, 5));

            VBox headerVBox = new VBox(titleBox, paddingBox);
            chartScroll.setPadding(new Insets(0));

            

            RangeBar chartRange = new RangeBar(rangeWidth, rangeHeight, m_dataList.getNetworksData().getExecService());
            chartRange.setId("menuBtn");
            chartRange.setVisible(false);

            BufferedButton setRangeBtn = new BufferedButton("/assets/checkmark-25.png");
            setRangeBtn.getBufferedImageView().setFitWidth(15);
            setRangeBtn.setOnAction(e->chartRange.start());
            setRangeBtn.setId("menuBarCircleBtn");
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
            rangeText.setFont(Stages.titleFont);
            rangeText.setFill(Stages.txtColor);

            HBox chartRangeToolbox = new HBox(rangeText, setRangeBtn, btnSpacer, cancelRangeBtn);
            chartRangeToolbox.setId("bodyBox");
            chartRangeToolbox.setAlignment(Pos.CENTER_LEFT);
            chartRangeToolbox.setPadding(new Insets(0,5,0,5));

            chartView.rangeActiveProperty().bind(chartRange.activeProperty());
            chartView.rangeTopVvalueProperty().bind(chartRange.topVvalueProperty());
            chartView.rangeBottomVvalueProperty().bind(chartRange.bottomVvalueProperty());
            
            chartRange.settingRangeProperty().addListener((obs,oldval,newval)->{
                chartView.isSettingRangeProperty().set(newval);
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

            HBox bodyBox = new HBox(chartRange, chartScroll);
            bodyBox.setAlignment(Pos.TOP_LEFT);
            bodyBox.setId("bodyBox");
            HBox bodyPaddingBox = new HBox(bodyBox);
            bodyPaddingBox.setPadding(new Insets(0,5,5,5));

            VBox layoutBox = new VBox(headerVBox, bodyPaddingBox);

            Scene marketScene = new Scene(layoutBox, sceneWidth, sceneHeight);
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

            ResizeHelper.addResizeListener(m_stage, 200, 200, Double.MAX_VALUE, Double.MAX_VALUE);
            m_stage.show();


            Runnable resetScroll = () ->{
                
                Platform.runLater(()-> chartScroll.setVvalue(chartScrollVvalue));
                Platform.runLater(()-> chartScroll.setHvalue(chartScrollHvalue));
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
                    resetScroll.run();
                    m_positionIndex = 1;
                }else{
                    if(m_positionIndex == 1){
                        m_stage.setMaximized(false);
                        resetPosition.run();
                        resetScroll.run();
                    }else{
                        resetPosition.run();
                        FxTimer.runLater(Duration.ofMillis(100), ()->{
                            m_stage.setMaximized(true);
                            resetScroll.run();
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
                        resetScroll.run();
                        
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
            ChangeListener<KucoinTickerData> tickerListener = (obs, oldVal, newVal) -> {
                if (newVal != null) {

                    m_stage.setTitle(exchange.getName() + " - " + m_name + (newVal != null ? " - " + newVal.getLastString() + "" : ""));

                } else {

                }
            };

            m_tickerDataProperty.addListener(tickerListener);

            Runnable closable = () -> {

                exchange.unsubscribeToCandles(m_parentInterface.getNetworkId(), m_symbol, m_timeSpan.getId());
                m_tickerDataProperty.removeListener(tickerListener);
                exchange.removeListener(msgInterface);

                m_stage = null;
            };

            m_stage.setOnCloseRequest(e -> closable.run());

            closeBtn.setOnAction(e -> {
                m_stage.close();
                closable.run();
            });
     

            Runnable startCandles = () -> {
                TimeSpan tSpan = m_timeSpan;
                Platform.runLater(()->chartScroll.setHvalue(0));
                exchange.getCandlesDataset(m_symbol, tSpan.getId(), onSuccess -> {
                    WorkerStateEvent worker = onSuccess;
                    Object sourceObject = worker.getSource().getValue();

                    if (sourceObject != null && sourceObject instanceof JsonObject) {

                        JsonObject sourceJson = (JsonObject) sourceObject;

                        JsonElement msgElement = sourceJson.get("msg");
                        JsonElement dataElement = sourceJson.get("data");

                        if (msgElement != null && msgElement.isJsonPrimitive()) {
                            Platform.runLater(()->chartView.setMsg(msgElement.toString()));
                        } else {
                            if (dataElement != null && dataElement.isJsonArray()) {
                                JsonArray dataElementArray = dataElement.getAsJsonArray();

                                Platform.runLater(()->chartView.setPriceDataList(dataElementArray, tSpan.getSeconds()));

                                

                                Platform.runLater(()->chartRange.setVisible(true));;

                                if (exchange.isClientReady()) {
                                    Platform.runLater(()->exchange.subscribeToCandles(m_parentInterface.getNetworkId(), m_symbol, m_timeSpan.getId()));
                                }
                                FxTimer.runLater(Duration.ofMillis(100), resetScroll);
                            } else {
                                
                            }

                        }

                    }
                }, onFailed -> {
                    FxTimer.runLater(Duration.ofMillis(100), resetScroll);
                });
            };

            timeSpanBtn.textProperty().addListener((obs, oldVal, newVal) -> {
                Object objData = timeSpanBtn.getUserData();

                if (newVal != null && !newVal.equals(m_timeSpan.getName()) && objData != null && objData instanceof TimeSpan) {

                    exchange.unsubscribeToCandles(m_parentInterface.getNetworkId(), m_symbol, m_timeSpan.getId());

                    TimeSpan tSpan = (TimeSpan) objData;

                    m_timeSpan = tSpan;

                    chartRange.setVisible(false);
                    chartRange.reset();
                    chartView.reset();
                    startCandles.run();
                }
            });

            startCandles.run();
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
                Platform.runLater(()->chartScroll.setVvalue(chartScrollVvalue));
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
  
            chartBox.setOnKeyPressed(keyEventHandler);

            marketScene.focusOwnerProperty().addListener((e) -> {
                Object focusOwnerObject = marketScene.focusOwnerProperty().get();
                if (!(focusOwnerObject instanceof MenuButton)) {
                   chartBox.requestFocus();
                }
            });

            String[] spans = KucoinExchange.AVAILABLE_TIMESPANS;

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
            // chartScroll.setHvalue(1);

        } else {
            m_stage.show();
            m_stage.requestFocus();
        }
    }

    public String getSymbol() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getSymbol() : m_symbol;
    }

    public String getSymbolName() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getSymbolName() : m_name;
    }

    public double getBuy() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getBuy() : Double.NaN;
    }

    public double getSell() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getSell() : Double.NaN;
    }

    public double getChangeRate() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getChangeRate() : Double.NaN;
    }

    public double getChangePrice() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getChangePrice() : Double.NaN;
    }

    public double getHigh() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getHigh() : Double.NaN;
    }

    public double getLow() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getLow() : Double.NaN;
    }

    public double getVol() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getVol() : Double.NaN;
    }

    public double getVolValue() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getVolValue() : Double.NaN;
    }

    public double getLast() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getLast() : Double.NaN;
    }

    public double getAveragePrice() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getAveragePrice() : Double.NaN;
    }

    public double getTakerFeeRate() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getTakerFeeRate() : Double.NaN;
    }

    public double getMakerFeeRate() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getMakerFeeRate() : Double.NaN;
    }

    public double getTakerCoefficient() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getTakerCoefficient() : Double.NaN;
    }

    public double getMakerCoefficient() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getMakerCoefficient() : Double.NaN;
    }
}
