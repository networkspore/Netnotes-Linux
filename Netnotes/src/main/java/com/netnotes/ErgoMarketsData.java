package com.netnotes;


import java.io.File;
import java.time.LocalDateTime;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class ErgoMarketsData {

    public static File logFile = new File("netnotes-log.txt");;

    public final static String SCHEDULED = "Scheduled";
    public final static String POLLING = "Polling";
    public final static String STARTED = "Started";
    public final static String STARTING = "Starting";
    public final static String STOPPED = "Stopped";
    public final static String ERROR = "Error";
    public final static String STOP = "STOP";
    public final static String POLLED = "POLLED";
    public final static String REALTIME = "REALTIME";


    private final static double CURRENCY_COL_WIDTH = 80;

    public final static String TICKER = "ticker";

    private ErgoMarketsList m_marketsList;

    private Font m_priceFont = Font.font("OCR A Extended", FontWeight.NORMAL, 25);
    private Font m_font = Font.font("OCR A Extended", FontWeight.BOLD, 13);

    private Font m_smallFont =Font.font("OCR A Extended", FontWeight.NORMAL, 10);


    private Color m_baseCurrencyColor = new Color(.4, .4, .4, .9);
    private Color m_quoteCurrencyColor = new Color(.7, .7, .7, .9);

    private String m_radioOffUrl = "/assets/radio-button-off-30.png";
    private String m_radioOnUrl = "/assets/radio-button-on-30.png";

    private String m_startImgUrl = "/assets/play-30.png";
    private String m_stopImgUrl = "/assets/stop-30.png";

    private final String m_id;
    private String m_name;
    private String m_baseSymbol = "ERG";
    private String m_quoteSymbol = "USDT";
    private String m_updateType = TICKER;
    private String m_value = null;
    private SimpleObjectProperty<LocalDateTime> m_shutdownNow = new SimpleObjectProperty<>(LocalDateTime.now());
    private ChangeListener<LocalDateTime> m_shutdownListener = null;
    private SimpleObjectProperty<LocalDateTime> m_updated = new SimpleObjectProperty<>(LocalDateTime.now());
    private SimpleObjectProperty<PriceQuote> m_priceQuoteProperty = new SimpleObjectProperty<PriceQuote>(null);
    private SimpleStringProperty m_statusProperty = new SimpleStringProperty(STOPPED);

  

    public ErgoMarketsData(ErgoMarketsList marketsList, JsonObject json) throws NullPointerException {
        m_marketsList = marketsList;
        if (json != null) {

            JsonElement idElement = json.get("id");
            JsonElement typeElement = json.get("type");
            JsonElement valueElement = json.get("value");
            JsonElement symbolElement = json.get("symbol");
            JsonElement nameElement = json.get("name");

            m_id = idElement == null ? null : idElement.getAsString();

            if(m_id == null){
                throw new NullPointerException("ErgoMarketData null Id");
            }

            m_updateType = typeElement == null ? null : typeElement.getAsString();
            m_value = valueElement == null ? null : valueElement.getAsString();
            m_name = nameElement == null ?  m_id : nameElement.getAsString();

            JsonObject symbolObject = symbolElement != null && symbolElement.isJsonObject() ? symbolElement.getAsJsonObject() : null;

            if (symbolObject != null) {

                m_baseSymbol = symbolObject.get("base").getAsString();
                m_quoteSymbol = symbolObject.get("quote").getAsString();

            } else {
                m_baseSymbol = "ERG";
                m_quoteSymbol = "";
            }
        } else {
            throw new NullPointerException("ErgoMarketData null Id");
        }


    }

    public ErgoMarketsData(String name, String marketId, String baseSymbol, String quoteSymbol, String updateType, String updateValue, ErgoMarketsList marketsList) {
    
        m_name = name;
        m_baseSymbol = baseSymbol;
        m_quoteSymbol = quoteSymbol;
        m_id = marketId;
        m_updateType = updateType;
        m_value = updateValue;
        m_marketsList = marketsList;

    }

    public void setShutdownListener(ChangeListener<LocalDateTime> lisener){
        m_shutdownListener = lisener;
        m_shutdownNow.addListener(m_shutdownListener);
    }

    public ChangeListener<LocalDateTime> getShutdownListener(){
        return m_shutdownListener;
    }

    public SimpleStringProperty statusProperty(){
        return m_statusProperty;
    }

    public SimpleObjectProperty<LocalDateTime> updatedProperty(){
        return m_updated;
    }

    public String getName(){
        return m_name;
    }
    public void setName(String name){
        m_name = name;
    }

    public static String getFriendlyUpdateTypeName(String type) {
        switch (type) {
            case REALTIME:
                return "Real-time";
            case POLLED:
                return "Timer";
            default:
                return type;
        }
    }

    public String getId() {
        return m_id;
    }

    public String getMarketId() {
        return m_id;
    }



    public String getUpdateType() {
        return m_updateType;
    }

    public void setUpdateType(String updateType) {
        m_updateType = updateType;
    }

    public String getUpdateValue() {
        return m_value;
    }

    public Font getPriceFont() {
        return m_priceFont;
    }

    public void setPriceFont(Font font){
        m_priceFont = font;
    }

   public Font getFont(){
        return m_font;
   }
   public void setFont(Font font){
    m_font = font;
   }

   public Font getSmallFont(){
    return m_smallFont;
   }
   public void setSmallFont(Font font){
    m_font = font;
   }

   public Color getBaseCurrencyColor(){
    return m_baseCurrencyColor;
   }
   public Color getQuoteCurrencyColor(){
    return m_quoteCurrencyColor;
   }
   public String getRadioOffUrl(){
    return m_radioOffUrl;
   }
   public String getRadioOnUrl(){
    return m_radioOnUrl;
   }

   public String getStartImgUrl(){
    return m_startImgUrl;
   }

   public String getStopImgUrl(){
    return m_stopImgUrl;
   }


    public JsonObject getSymbolJson() {
        JsonObject json = new JsonObject();
        json.addProperty("base", m_baseSymbol);
        json.addProperty("quote", m_quoteSymbol);
        return json;
    }

    public JsonObject getJsonObject() {
        JsonObject json = new JsonObject();
        json.addProperty("id", m_id);
        json.addProperty("type", m_updateType);
        json.addProperty("value", m_value);
        json.addProperty("name", m_name);
        json.add("symbol", getSymbolJson());
        return json;
    }


    public ErgoMarketsList getMarketsList(){
        return m_marketsList;
    }

    public PriceQuote getPriceQuoteById(String baseId, String quoteId){
        return null;
    }

      
    public void start(){

    }

    public String getValue(){
        return m_value;
    }

    public String getQuoteSymbol(){
        return m_quoteSymbol;
    }

    public String getBaseSymbol(){
        return m_baseSymbol;
    }

    public SimpleObjectProperty<PriceQuote> priceQuoteProperty() {
        return m_priceQuoteProperty;
    }


    public void shutdown() {
        m_shutdownNow.set(LocalDateTime.now());

    }

    public SimpleObjectProperty<LocalDateTime> getShutdownNow(){
        return m_shutdownNow;
    }

    public HBox getRowItem() {
        String defaultId = getMarketsList().defaultIdProperty().get();

        BufferedButton defaultBtn = new BufferedButton(defaultId != null && getId().equals(defaultId) ?getRadioOnUrl() : getRadioOffUrl(), 15);

        getMarketsList().defaultIdProperty().addListener((obs, oldval, newVal) -> {
            defaultBtn.getBufferedImageView().setDefaultImage(new Image(newVal != null && getId().equals(newVal) ? getRadioOnUrl() : getRadioOffUrl()));
        });

        String valueString = getValue() == null ? "" : getValue();
        String amountString = priceQuoteProperty().get() == null ? "-" : priceQuoteProperty().get().getAmountString();
        String baseCurrencyString = priceQuoteProperty().get() == null ? getBaseSymbol() : priceQuoteProperty().get().getTransactionCurrency();
        String quoteCurrencyString = priceQuoteProperty().get() == null ? getQuoteSymbol() : priceQuoteProperty().get().getQuoteCurrency();

        Text topValueText = new Text(valueString);
        topValueText.setFont(getFont());
        topValueText.setFill(getBaseCurrencyColor());

        Text topInfoStringText = new Text(getName());
        topInfoStringText.setFont(getFont());
        topInfoStringText.setFill(getBaseCurrencyColor());

        Text topRightText = new Text("");
        topRightText.setFont(getFont());
        topRightText.setFill(getBaseCurrencyColor());


        Text botTimeText = new Text();
        botTimeText.setFont(getSmallFont());
        botTimeText.setFill(getBaseCurrencyColor());

        TextField amountField = new TextField(amountString);
        amountField.setFont(getPriceFont());
        amountField.setId("textField");
        amountField.setEditable(false);
        amountField.setAlignment(Pos.CENTER_RIGHT);
        amountField.setPadding(new Insets(0, 10, 0, 0));

        Text baseCurrencyText = new Text(baseCurrencyString);
        baseCurrencyText.setFont(getFont());
        baseCurrencyText.setFill(getBaseCurrencyColor());

        Text quoteCurrencyText = new Text(quoteCurrencyString);
        quoteCurrencyText.setFont(getFont());
        quoteCurrencyText.setFill(getQuoteCurrencyColor());

        VBox currencyBox = new VBox(baseCurrencyText, quoteCurrencyText);
        currencyBox.setAlignment(Pos.CENTER_RIGHT);
        currencyBox.setMinWidth(CURRENCY_COL_WIDTH);

        VBox.setVgrow(currencyBox, Priority.ALWAYS);

        BufferedButton statusBtn = new BufferedButton(statusProperty().get().equals(STOPPED) ? getStartImgUrl() : getStopImgUrl(), 15);
        statusBtn.setId("statusBtn");
        statusBtn.setPadding(new Insets(0, 10, 0, 10));
        statusBtn.setOnAction(action -> {
            if (statusProperty().get().equals(STOPPED)) {
                start();
            } else {
                shutdown();
            }
        });

        statusProperty().addListener((obs, oldVal, newVal) -> {
            switch (statusProperty().get()) {
                case STOPPED:
                    statusBtn.getBufferedImageView().setDefaultImage(new Image(getStartImgUrl()), 15);
                    break;
                default:
                    statusBtn.getBufferedImageView().setDefaultImage(new Image(getStopImgUrl()), 15);
                    break;
            }
        });

        priceQuoteProperty().addListener((obs, oldVal, newVal) -> {
            String updateAmountString = priceQuoteProperty().get() == null ? "-" : priceQuoteProperty().get().getAmountString();
            String updateBaseCurrencyString = priceQuoteProperty().get() == null ? getBaseSymbol() : priceQuoteProperty().get().getTransactionCurrency();
            String updateQuoteCurrencyString = priceQuoteProperty().get() == null ? getQuoteSymbol() : priceQuoteProperty().get().getQuoteCurrency();
            String updateTimeString = priceQuoteProperty().get() == null ? "" : Utils.formatTimeString(Utils.milliToLocalTime(priceQuoteProperty().get().getTimeStamp()));

            amountField.setText(updateAmountString);
            baseCurrencyText.setText(updateBaseCurrencyString);
            quoteCurrencyText.setText(updateQuoteCurrencyString);

            topValueText.setText(getValue() == null ? "" : getValue());
            botTimeText.setText(updateTimeString);
        });

        HBox leftBox = new HBox(defaultBtn);
        HBox rightBox = new HBox(statusBtn);

        leftBox.setAlignment(Pos.CENTER_LEFT);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        leftBox.setId("bodyBox");
        rightBox.setId("bodyBox");

        Region currencySpacer = new Region();
        currencySpacer.setMinWidth(10);

        HBox centerBox = new HBox(amountField, currencyBox);
        centerBox.setPadding(new Insets(0, 5, 0, 5));
        centerBox.setAlignment(Pos.CENTER_RIGHT);
        centerBox.setId("darkBox");

        HBox topSpacer = new HBox();
        HBox bottomSpacer = new HBox();

        topSpacer.setMinHeight(2);
        bottomSpacer.setMinHeight(2);

        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
        topSpacer.setId("bodyBox");
        bottomSpacer.setId("bodyBox");

        Region topMiddleRegion = new Region();
        HBox.setHgrow(topMiddleRegion, Priority.ALWAYS);

        HBox topBox = new HBox(topInfoStringText, topMiddleRegion, topRightText);
        topBox.setId("darkBox");

        HBox bottomBox = new HBox(botTimeText);
        bottomBox.setId("darkBox");
        bottomBox.setAlignment(Pos.CENTER_RIGHT);

        HBox.setHgrow(bottomBox, Priority.ALWAYS);

        VBox bodyBox = new VBox(topSpacer, topBox, centerBox, bottomBox, bottomSpacer);
        HBox.setHgrow(bodyBox, Priority.ALWAYS);

        HBox rowBox = new HBox(leftBox, bodyBox, rightBox);
        rowBox.setAlignment(Pos.CENTER_LEFT);
        rowBox.setId("rowBox");
        
        rowBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if(e.getClickCount() == 2){
                Network market = (Network) m_marketsList.getErgoMarkets().getNetworksData().getNoteInterface(m_id);
                if(market != null){
                    market.open();
                }
                e.consume();
            }else{
              
                m_marketsList.selectedIdProperty().set(getId());
                    
                
                e.consume();
            }
            
        });

        Runnable updateSelected = () -> {
            String selectedId = m_marketsList.selectedIdProperty().get();
            boolean isSelected = selectedId != null && getId().equals(selectedId);

            amountField.setId(isSelected ? "textField" : "priceField");

            rowBox.setId(isSelected ? "selected" : "unSelected");
        };

        m_marketsList.selectedIdProperty().addListener((obs, oldval, newVal) -> updateSelected.run());
        updateSelected.run();
        // centerBox.prefWidthProperty().bind(rowBox.widthProperty().subtract(leftBox.widthProperty()).subtract(rightBox.widthProperty()));
        start();
        return rowBox;
    }

}
