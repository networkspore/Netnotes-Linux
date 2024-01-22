package com.netnotes;


import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.devskiller.friendly_id.FriendlyId;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class ErgoMarketsData {

  //  private File logFile = new File("netnotes-log.txt");;

    public final static String SCHEDULED = "Scheduled";
    public final static String POLLING = "Polling";
    public final static String STARTED = "Started";
    public final static String STARTING = "Starting";
    public final static String STOPPED = "Stopped";
    public final static String ERROR = "Error";
    public final static String STOP = "STOP";
    public final static String POLLED = "POLLED";
    public final static String REALTIME = "REALTIME";

    public final static String TICKER = "ticker";

    private ErgoMarketsList m_marketsList;

    private Font m_priceFont = Font.font("OCR A Extended", FontWeight.BOLD, 30);
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
    private String m_marketId = KucoinExchange.NETWORK_ID;
    private String m_updateType = TICKER;
    private String m_value = null;
    private SimpleObjectProperty<LocalDateTime> m_shutdownNow = new SimpleObjectProperty<>(LocalDateTime.now());
   

    private ScheduledExecutorService m_polledExecutor = null;

    private SimpleObjectProperty<PriceQuote> m_priceQuoteProperty = new SimpleObjectProperty<PriceQuote>(null);
    private SimpleStringProperty m_statusProperty = new SimpleStringProperty(STOPPED);
    private MessageInterface m_msgListener;

    public ErgoMarketsData(ErgoMarketsList marketsList, JsonObject json) {
        m_marketsList = marketsList;
        if (json != null) {

            JsonElement idElement = json.get("id");
            JsonElement typeElement = json.get("type");
            JsonElement valueElement = json.get("value");
            JsonElement marketIdElement = json.get("marketId");
            JsonElement symbolElement = json.get("symbol");
            JsonElement nameElement = json.get("name");

            m_id = idElement == null ? FriendlyId.createFriendlyId() : idElement.getAsString();
            m_marketId = marketIdElement == null ? null : marketIdElement.getAsString();
            m_updateType = typeElement == null ? null : typeElement.getAsString();
            m_value = valueElement == null ? null : valueElement.getAsString();
            m_name = nameElement == null ? "market #" + m_id : nameElement.getAsString();

            JsonObject symbolObject = symbolElement != null && symbolElement.isJsonObject() ? symbolElement.getAsJsonObject() : null;

            if (symbolObject != null) {

                m_baseSymbol = symbolObject.get("base").getAsString();
                m_quoteSymbol = symbolObject.get("quote").getAsString();

            } else {
                m_baseSymbol = "ERG";
                m_quoteSymbol = "USDT";
            }
        } else {
            m_id = FriendlyId.createFriendlyId();
        }


    }

    public ErgoMarketsData(String id, String name, String marketId, String baseSymbol, String quoteSymbol, String updateType, String updateValue, ErgoMarketsList marketsList) {
        m_id = id;
        m_name = name;
        m_baseSymbol = baseSymbol;
        m_quoteSymbol = quoteSymbol;
        m_marketId = marketId;
        m_updateType = updateType;
        m_value = updateValue;
        m_marketsList = marketsList;

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
        return m_marketId;
    }



    public void setMarketid(String marketId) {
        m_marketId = marketId;
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
        json.addProperty("marketId", m_marketId);
        json.addProperty("name", m_name);
        json.add("symbol", getSymbolJson());
        return json;
    }

    
    private NoteInterface m_marketInterface = null;

    public void start() {
        if (m_marketId != null &&  !m_statusProperty.get().equals(STARTED)) {
            m_marketInterface = m_marketsList.getErgoMarkets().getNetworksData().getNoteInterface(m_marketId);
            if (m_marketInterface != null) {

                if (m_marketInterface instanceof KucoinExchange) {
                    KucoinExchange exchange = (KucoinExchange) m_marketInterface;

                    switch (m_updateType) {
                        case POLLED:
                            startPollingKuCoin(exchange);
                            break;
                        case REALTIME:
                            startKucoinListener(exchange);
                    }
                }

            }
        }
    }

    private void startKucoinListener(KucoinExchange exchange) {
        String symbol = m_baseSymbol + "-" + m_quoteSymbol;
        switch (m_value) {
            case TICKER:
                startTicker(symbol, exchange);
                break;
        }

    }

    private ChangeListener<LocalDateTime> m_shutdownListener = null;

    private void stopTicker(KucoinExchange exchange) {

        boolean removed = exchange.removeMsgListener(m_msgListener);
        if (removed) {

            exchange.unsubscribeToTicker(m_id, m_baseSymbol + "-" + m_quoteSymbol);
            m_statusProperty.set(STOPPED);

            m_shutdownNow.removeListener(m_shutdownListener);
        }
    }

    private void startTicker(String symbol, KucoinExchange exchange) {
        m_statusProperty.set(STARTING);

        String subjectString = "trade.ticker";
        String topicString = "/market/ticker:" + symbol;

        exchange.getTicker(symbol, (onSucceeded) -> {
            Object sourceObject = onSucceeded.getSource().getValue();
            if (sourceObject != null && sourceObject instanceof JsonObject) {
                onTickerMsg((JsonObject) sourceObject);
            }
        }, (onFailed) -> {
            m_statusProperty.set(ERROR);
        });

        m_msgListener = new MessageInterface() {

            public String getSubject() {
                return subjectString;
            }

            public String getTopic() {
                return topicString;
            }

            public String getTunnelId() {
                return null;
            }

            public String getId() {
                return m_id;
            }

            public void onMsgChanged(JsonObject json) {
                /*try {
                    Files.writeString(logFile.toPath(), json.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }*/

                JsonElement subjectElement = json.get("subject");
                JsonElement topicElement = json.get("topic");

                String subject = subjectElement != null && subjectElement.isJsonPrimitive() ? subjectElement.getAsString() : null;
                String topic = topicElement != null && topicElement.isJsonPrimitive() ? topicElement.getAsString() : null;
                if (subject != null && topic != null && subject.equals(subjectString)) {

                    if (topic.substring(topic.length() - symbol.length(), topic.length()).equals(symbol)) {
                        onTickerMsg(json);
                    }
                }
            }

            public void onReady() {
                /*try {
                    Files.writeString(logFile.toPath(), "\nready", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }*/

                exchange.subscribeToTicker(m_id, m_baseSymbol + "-" + m_quoteSymbol);
            }
        };

        exchange.addMsgListener(m_msgListener);

        if (exchange.isClientReady()) {

            exchange.subscribeToTicker(m_id, m_baseSymbol + "-" + m_quoteSymbol);

        }

        m_shutdownListener = (obs, oldval, newVal) -> {
            stopTicker(exchange);
        };

        m_shutdownNow.addListener(m_shutdownListener);

    }

    private void onTickerMsg(JsonObject json) {
      
        if (json != null) {

            JsonElement dataElement = json.get("data");

            if (dataElement != null && dataElement.isJsonObject()) {
                JsonObject dataObject = dataElement.getAsJsonObject();

                JsonElement priceElement = dataObject.get("price");
               // JsonElement timeElement = dataObject.get("time");
               String priceString = priceElement != null && priceElement.isJsonPrimitive() ? priceElement.getAsString() : null;



                if(priceString!=null){
                    priceQuoteProperty().set(new PriceQuote(priceString, m_baseSymbol, m_quoteSymbol, System.currentTimeMillis()));
                }
            }
        }
    }

    private void startPollingKuCoin(KucoinExchange exchange) {

        m_statusProperty.set(STARTED);

        long seconds = Long.parseLong(m_value);
        if (seconds > 0) {
            m_polledExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });

            m_polledExecutor.schedule(() -> {
                m_statusProperty.set(POLLING);
                exchange.getTicker(m_baseSymbol + "-" + m_quoteSymbol, (success) -> {
                    Object sourceObject = success.getSource().getValue();
                    if (sourceObject != null && sourceObject instanceof PriceQuote) {
                        PriceQuote priceQuote = (PriceQuote) sourceObject;
                        m_priceQuoteProperty.set(priceQuote);
                    }
                }, (failed) -> {
                    m_statusProperty.set(ERROR);
                });
            }, seconds, TimeUnit.SECONDS);

            m_statusProperty.set(SCHEDULED);

        }

    }

    public SimpleObjectProperty<PriceQuote> priceQuoteProperty() {
        return m_priceQuoteProperty;
    }
    
    public HBox getRowItem() {
        String defaultId = m_marketsList.defaultIdProperty().get();

        BufferedButton defaultBtn = new BufferedButton(defaultId != null && m_id.equals(defaultId) ? m_radioOnUrl : m_radioOffUrl, 15);

        m_marketsList.defaultIdProperty().addListener((obs, oldval, newVal) -> {
            defaultBtn.getBufferedImageView().setDefaultImage(new Image(newVal != null && m_id.equals(newVal) ? m_radioOnUrl : m_radioOffUrl));
        });

        String valueString = m_value == null ? "" : m_value;
        String amountString = m_priceQuoteProperty.get() == null ? "-" : m_priceQuoteProperty.get().getAmountString();
        String baseCurrencyString = m_priceQuoteProperty.get() == null ? m_baseSymbol : m_priceQuoteProperty.get().getTransactionCurrency();
        String quoteCurrencyString = m_priceQuoteProperty.get() == null ? m_quoteSymbol : m_priceQuoteProperty.get().getQuoteCurrency();

        Text topValueText = new Text(valueString);
        topValueText.setFont(m_font);
        topValueText.setFill(m_baseCurrencyColor);

        Text topInfoStringText = new Text(getName());
        topInfoStringText.setFont(m_font);
        topInfoStringText.setFill(m_baseCurrencyColor);

        Text topRightText = new Text("");
        topRightText.setFont(m_font);
        topRightText.setFill(m_baseCurrencyColor);


        Text botTimeText = new Text();
        botTimeText.setFont(m_smallFont);
        botTimeText.setFill(m_baseCurrencyColor);

        TextField amountField = new TextField(amountString);
        amountField.setFont(getPriceFont());
        amountField.setId("textField");
        amountField.setEditable(false);
        amountField.setAlignment(Pos.CENTER_RIGHT);
        amountField.setPadding(new Insets(0, 10, 0, 0));

        Text baseCurrencyText = new Text(baseCurrencyString);
        baseCurrencyText.setFont(m_font);
        baseCurrencyText.setFill(m_baseCurrencyColor);

        Text quoteCurrencyText = new Text(quoteCurrencyString);
        quoteCurrencyText.setFont(m_font);
        quoteCurrencyText.setFill(m_quoteCurrencyColor);

        VBox currencyBox = new VBox(baseCurrencyText, quoteCurrencyText);
        currencyBox.setAlignment(Pos.CENTER_RIGHT);

        VBox.setVgrow(currencyBox, Priority.ALWAYS);

        BufferedButton statusBtn = new BufferedButton(m_statusProperty.get().equals(STOPPED) ? m_startImgUrl : m_stopImgUrl, 15);
        statusBtn.setId("statusBtn");
        statusBtn.setPadding(new Insets(0, 10, 0, 10));
        statusBtn.setOnAction(action -> {
            if (m_statusProperty.get().equals(STOPPED)) {
                start();
            } else {
                m_shutdownNow.set(LocalDateTime.now());
            }
        });

        m_statusProperty.addListener((obs, oldVal, newVal) -> {
            switch (m_statusProperty.get()) {
                case STOPPED:
                    statusBtn.getBufferedImageView().setDefaultImage(new Image(m_startImgUrl), 15);
                    break;
                default:
                    statusBtn.getBufferedImageView().setDefaultImage(new Image(m_stopImgUrl), 15);
                    break;
            }
        });

        m_priceQuoteProperty.addListener((obs, oldVal, newVal) -> {
            String updateAmountString = m_priceQuoteProperty.get() == null ? "-" : m_priceQuoteProperty.get().getAmountString();
            String updateBaseCurrencyString = m_priceQuoteProperty.get() == null ? m_baseSymbol : m_priceQuoteProperty.get().getTransactionCurrency();
            String updateQuoteCurrencyString = m_priceQuoteProperty.get() == null ? m_quoteSymbol : m_priceQuoteProperty.get().getQuoteCurrency();
            String updateTimeString = m_priceQuoteProperty.get() == null ? "" : Utils.formatTimeString(Utils.milliToLocalTime(m_priceQuoteProperty.get().getTimeStamp()));

            amountField.setText(updateAmountString);
            baseCurrencyText.setText(updateBaseCurrencyString);
            quoteCurrencyText.setText(updateQuoteCurrencyString);

            topValueText.setText(m_value == null ? "" : m_value);
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

        // centerBox.prefWidthProperty().bind(rowBox.widthProperty().subtract(leftBox.widthProperty()).subtract(rightBox.widthProperty()));
        start();
        return rowBox;
    }

    public void shutdown() {
        m_shutdownNow.set(LocalDateTime.now());

        if (m_polledExecutor != null) {
            m_polledExecutor.shutdownNow();
            m_polledExecutor = null;
        }
    }

  
}
