package com.netnotes;

import java.awt.FontMetrics;
import java.awt.Graphics2D;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.reactfx.util.FxTimer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

import com.utils.Utils;

import javafx.application.Platform;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;

import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SpectrumMarketItem {

    private static File logFile = new File("netnotes-log.txt");

    private SpectrumDataList m_dataList = null;
    private SimpleObjectProperty<SpectrumMarketData> m_marketDataProperty = new SimpleObjectProperty<>(null);
    private Stage m_stage = null;
    private SimpleBooleanProperty m_isFavorite = new SimpleBooleanProperty(false);

    private double m_prevWidth = -1;
    private double m_prevHeight = -1;
    private double m_prevX = -1;
    private double m_prevY = -1;
    private String m_symbol;

    public SpectrumMarketItem(boolean favorite, SpectrumMarketData marketData, SpectrumDataList dataList) {
        m_symbol = marketData.getSymbol();
        m_dataList = dataList;
        m_marketDataProperty.set(marketData);
        m_isFavorite.set(favorite);
       
    }  

    public SimpleBooleanProperty isFavoriteProperty() {
        return m_isFavorite;
    }

    public File getMarketFile() throws IOException{
    
        File marketFile = m_dataList.getSpectrumFinance().getIdDataFile(m_symbol);

        return marketFile;
    }



    public SimpleObjectProperty<SpectrumMarketData> marketDataProperty() {
        return m_marketDataProperty;
    }

    public HBox getRowBox() {

        SpectrumMarketData data = m_marketDataProperty.get();

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
        Image buttonImage = data == null ? null : getButtonImage(data);

        Button rowButton = new IconButton() {
            @Override
            public void open() {
                if(m_marketDataProperty.get().getPoolId() != null){
                    showStage();
                }else{
                    Alert a = new Alert(AlertType.NONE, "Chart currently unavailable.", ButtonType.OK);
                    a.setTitle("Chart Unavailable - " + SpectrumFinance.NAME);
                    a.initOwner(m_stage);
                    a.show();
                }

            }
        };
        rowButton.setContentDisplay(ContentDisplay.LEFT);
        rowButton.setAlignment(Pos.CENTER_LEFT);
        rowButton.setGraphic(buttonImage == null ? null : IconButton.getIconView(buttonImage, buttonImage.getWidth()));
        rowButton.setId("rowBtn");

        Text hasChart = new Text(m_marketDataProperty.get().getPoolId() != null ? " Chart Available" : "");

        HBox rowBox = new HBox(favoriteBtn, rowButton, hasChart);

        rowButton.prefWidthProperty().bind(rowBox.widthProperty().subtract(favoriteBtn.widthProperty()));

        rowBox.setAlignment(Pos.CENTER_LEFT);

        m_marketDataProperty.addListener((obs, oldVal, newVal) -> {
            Image img = newVal == null ? null : getButtonImage(newVal);
            rowButton.setGraphic(img == null ? null : IconButton.getIconView(img, img.getWidth()));
        });

        
        m_dataList.getSortMethod().isTargetSwappedProperty().addListener((obs, oldVal, newVal) -> {
            Image img = getButtonImage(m_marketDataProperty.get());
            rowButton.setGraphic(img == null ? null : IconButton.getIconView(img, img.getWidth()));
        });

        return rowBox;
    }

    public static int FILL_COLOR = 0xffffffff;
    public static java.awt.Color WHITE_COLOR = new java.awt.Color(FILL_COLOR, true);

    public String getCurrentSymbol(boolean swapped){
        SpectrumMarketData data = m_marketDataProperty.get();
        return swapped ? (data.getQuoteSymbol() + "-" + data.getBaseSymbol()) : data.getSymbol();
    }

    private Image getButtonImage(SpectrumMarketData data) {
        if (data == null) {
            return null;
        }
        int height = 30;
        int symbolColWidth = 160;
        String symbolString = String.format("%-18s", getCurrentSymbol(isInvert()) );
        String priceString = isInvert() ? data.getInvertedLastPrice().toString() : data.getLastPrice().toString();

        boolean positive = false;
        boolean neutral = true;

        java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 15);
        java.awt.Font txtFont = new java.awt.Font("Deja Vu Sans", java.awt.Font.PLAIN, 15);
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);

        FontMetrics fm = g2d.getFontMetrics();

        //int symbolWidth = fm.stringWidth(symbolString);
        int priceWidth = fm.stringWidth(priceString);
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
        g2d.setFont(txtFont);
        g2d.setColor(WHITE_COLOR);
        g2d.drawString(symbolString, 0, stringY);
    

        img = new BufferedImage(symbolColWidth + colPadding + priceWidth, height, BufferedImage.TYPE_INT_ARGB);
        g2d = img.createGraphics();

        //g2d.drawImage(symbolImage, 0, 0, symbolColWidth, height, null);
        Drawing.drawImageExact(img, symbolImage, 0, 0, false);
       // if (neutral) {
        g2d.setFont(font);
        g2d.drawString(priceString, symbolColWidth + colPadding, stringY);

       /* } else {

            g2d.drawString(lastString, symbolWidth + colPadding, stringY);

            int x1 = symbolWidth + colPadding;
            int y1 = (height / 2) - (fontHeight / 2);
            int x2 = x1 + lastWidth;
            int y2 = y1 + fontHeight;
            java.awt.Color color1 = positive ? KucoinExchange.POSITIVE_COLOR : KucoinExchange.NEGATIVE_HIGHLIGHT_COLOR;
            java.awt.Color color2 = positive ? KucoinExchange.POSITIVE_HIGHLIGHT_COLOR : KucoinExchange.NEGATIVE_COLOR;

            Drawing.drawBarFillColor(positive ? 0 : 1, false, FILL_COLOR, color1.getRGB(), color2.getRGB(), img, x1, y1, x2, y2);

        }*/

        g2d.dispose();

        return SwingFXUtils.toFXImage(img, null);
    }

    public String returnGetId() {
        return getId();
    }

    public boolean isInvert(){
        return m_marketDataProperty.get().getDefaultInvert() ? !m_dataList.getSortMethod().isTargetSwapped() : m_dataList.getSortMethod().isTargetSwapped();
    }

    public void showStage() {
        if (m_stage == null) {
         
            SimpleBooleanProperty isInvertChart = new SimpleBooleanProperty(isInvert());

            double sceneWidth = 750;
            double sceneHeight = 800;
            final double chartScrollVvalue = 1;
            final double chartScrollHvalue = 1;

            SimpleDoubleProperty chartWidth = new SimpleDoubleProperty(sceneWidth - 50);
            SimpleDoubleProperty chartHeight = new SimpleDoubleProperty(sceneHeight - 170);
            SimpleDoubleProperty chartHeightOffset = new SimpleDoubleProperty(0);
            SimpleDoubleProperty rangeWidth = new SimpleDoubleProperty(30);
            SimpleDoubleProperty rangeHeight = new SimpleDoubleProperty(100);

            double chartSizeInterval = 25;

            SpectrumFinance exchange = m_dataList.getSpectrumFinance();

            m_stage = new Stage();
            m_stage.getIcons().add(SpectrumFinance.getSmallAppIcon());
            m_stage.initStyle(StageStyle.UNDECORATED);
            m_stage.setTitle(exchange.getName() + " - " + getCurrentSymbol(isInvertChart.get()) + (m_marketDataProperty.get() != null ? " - " +(isInvertChart.get() ? m_marketDataProperty.get().getInvertedLastPrice().toString() : m_marketDataProperty.get().getLastPrice()) + "" : ""));

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

            Text headingText = new Text(getCurrentSymbol(isInvertChart.get()) + "  - ");
            headingText.setFont(App.txtFont);
            headingText.setFill(Color.WHITE);

            Region headingSpacerL = new Region();

            SpectrumChartView chartView = new SpectrumChartView(chartWidth, chartHeight, new TimeSpan("1day"));
            HBox chartBox = chartView.getChartBox();

  

            MenuButton timeSpanBtn = new MenuButton(chartView.getTimeSpan().getName());
            timeSpanBtn.setFont(App.txtFont);

            timeSpanBtn.setContentDisplay(ContentDisplay.LEFT);
            timeSpanBtn.setAlignment(Pos.CENTER_LEFT);


            HBox headingBox = new HBox(favoriteBtn, headingSpacerL, headingText, timeSpanBtn);
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
                Platform.runLater(()->chartView.setIsSettingRange(newval));
            });
            HBox bodyBox = new HBox(chartRange, chartScroll);
            bodyBox.setId("bodyBox");
            bodyBox.setAlignment(Pos.TOP_LEFT);
            HBox bodyPaddingBox = new HBox(bodyBox);
            bodyPaddingBox.setPadding(new Insets(0, 5, 5 ,5));

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
                FxTimer.runLater(Duration.ofMillis(200), setChartScrollRight);
            });

         

            fillRightBtn.setOnAction(e -> {

                if (exchange.getAppStage() != null) {
                    Stage exStage = exchange.getAppStage();
                    if (exStage.getWidth() != m_stage.getX()) {
                        exchange.cmdObjectProperty().set(Utils.getCmdObject("MAXIMIZE_STAGE_LEFT"));

                        m_prevHeight = m_stage.getHeight();
                        m_prevWidth = m_stage.getWidth();
                        m_prevX = m_stage.getX();
                        m_prevY = m_stage.getY();

                        m_stage.setX(exStage.getWidth());
                        m_stage.setY(0);
                        m_stage.setWidth(rect.getWidth() - exStage.getWidth());
                        m_stage.setHeight(rect.getHeight());
                        FxTimer.runLater(Duration.ofMillis(200), setChartScrollRight);
                    } else {
                        maximizeBtn.fire();
                    }
                } else {
                    maximizeBtn.fire();
                }

            });

            // chartHeight.bind(Bindings.add(marketScene.heightProperty(), chartHeightOffset));
            ChangeListener<SpectrumMarketData> tickerListener = (obs, oldVal, newVal) -> {
                if (newVal != null) {

                    m_stage.setTitle(exchange.getName() + " - " + getCurrentSymbol(isInvertChart.get())+ (newVal != null ? " - " + newVal.getLastPrice() + "" : ""));

                } else {

                }
            };

            m_marketDataProperty.addListener(tickerListener);

        

           

            closeBtn.setOnAction(e -> {
       
   
                m_marketDataProperty.removeListener(tickerListener);
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

            marketScene.focusOwnerProperty().addListener((e) -> {
                Object focusOwnerObject = marketScene.focusOwnerProperty().get();
                if (!(focusOwnerObject instanceof MenuButton)) {
                   chartBox.requestFocus();
                }
            });
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
                final long lastTimeStamp = lastTimeStampElement != null && priceDataArray != null  ? lastTimeStampElement.getAsLong() : 0;
                final long currentTime = System.currentTimeMillis();

               
                m_dataList.getSpectrumFinance().getPoolChart(m_marketDataProperty.get().getPoolId(), lastTimeStamp, currentTime,  (onSuccess)->{
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

                        saveNewDataJson(currentTime, chartArray);
                      
                       
                   
                       
                        Platform.runLater(()->chartView.setPriceDataList(isInvertChart.get() ? invertPrices(chartArray) : chartArray, currentTime));
                        
                        Platform.runLater(()->chartRange.setVisible(true));

                        FxTimer.runLater(Duration.ofMillis(200), setChartScrollRight);
                    }else{
                        closeBtn.fire();
                    }
                    
                }, onFailed->{
                    closeBtn.fire();
                });
            };

            setCandles.run();

            Runnable updateCandles = () ->{
                JsonObject existingObj = null;
                try {
                    File marketFile = getMarketFile();
                    existingObj = marketFile.isFile() ? Utils.readJsonFile(getAppKey(), marketFile) : null;
                } catch (IOException | JsonParseException | InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                    try {
                        Files.writeString(logFile.toPath(), "\nSpectrum Market Data (setCandles.run):" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException  e1) {

                    }
                }
               
                JsonElement priceDataElement = existingObj != null ? existingObj.get("priceData") : null;
                JsonElement lastTimeStampElement = existingObj != null && priceDataElement != null ? existingObj.get("lastTimeStamp") : null; 

                final JsonArray priceDataArray = lastTimeStampElement != null && lastTimeStampElement.isJsonPrimitive() && priceDataElement != null && priceDataElement.isJsonArray() ? priceDataElement.getAsJsonArray() : null;
                final long lastTimeStamp = lastTimeStampElement != null && priceDataArray != null && priceDataArray.size() > 0  ? lastTimeStampElement.getAsLong() : 0;
                final long currentTime = System.currentTimeMillis();

               

                m_dataList.getSpectrumFinance().getPoolChart(m_marketDataProperty.get().getPoolId(), lastTimeStamp, currentTime,  (onSuccess)->{
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
                       
                        saveNewDataJson(currentTime, chartArray);
                   
                        Platform.runLater(()->chartView.updatePriceData(isInvertChart.get() ? invertPrices(newChartArray) : newChartArray, currentTime));                        
                    }
                    
                }, onFailed->{
                    try {
                        Files.writeString(logFile.toPath(), "SpectrumMarketItem updateCandles.run failed: " + onFailed.getSource().getException().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e1) {
                    
                    }
                });
            };

            m_dataList.getNetworksData().timeCycleProperty().addListener((obs,oldval,newval)->{
                if(chartView.getLastTimestamp() != 0){
                    updateCandles.run();
                }
            });            

            isInvertChart.addListener((obs,oldval,newval)->{
                chartView.reset();
                chartRange.reset();
                setCandles.run();
                invertBtn.setImage( new Image(newval? "/assets/targetSwapped.png" : "/assets/targetStandard.png"));
                m_stage.setTitle(exchange.getName() + " - " +  getCurrentSymbol(newval) + (m_marketDataProperty.get() != null ? " - " +(newval ? m_marketDataProperty.get().getInvertedLastPrice().toString() : m_marketDataProperty.get().getLastPrice()) + "" : ""));
                headingText.setText(getCurrentSymbol(newval));
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
        return m_marketDataProperty.get() != null ? m_marketDataProperty.get().getSymbol() : "Unknown";
    }

    public String getId(){
        return m_marketDataProperty.get().getId();
    }

    public String getPoolId(){
        return m_marketDataProperty.get().getPoolId();
    }

    public BigDecimal getLiquidityUSD(){
        return m_marketDataProperty.get().getLiquidityUSD();
    }

    public BigDecimal getLastPrice(){
        return m_marketDataProperty.get().getLastPrice();
    }
    public BigDecimal getBaseVolume(){
        return m_marketDataProperty.get().getBaseVolume();
    }
    public BigDecimal getQuoteVolume(){
        return m_marketDataProperty.get().getQuoteVolume();
    }

}
