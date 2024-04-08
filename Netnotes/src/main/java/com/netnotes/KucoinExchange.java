package com.netnotes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.reactfx.util.FxTimer;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class KucoinExchange extends Network implements NoteInterface {

    public static String DESCRIPTION = "KuCoin Exchange provides access to real time crypto market information for trading pairs.";
    public static String SUMMARY = "";
    public static String NAME = "KuCoin Exchange";
    public final static String NETWORK_ID = "KUCOIN_EXCHANGE";
    private String m_clientId = null;

    public static String[] AVAILABLE_TIMESPANS = new String[]{
        "1min", "3min", "15min", "30min", "1hour", "2hour", "4hour", "6hour", "8hour", "12hour", "1day", "1week"
    };

    public static String API_URL = "https://api.kucoin.com";

    private File logFile = new File("netnotes-log.txt");

    public static java.awt.Color POSITIVE_HIGHLIGHT_COLOR = new java.awt.Color(0xff3dd9a4, true);
    public static java.awt.Color POSITIVE_COLOR = new java.awt.Color(0xff028A0F, true);

    public static java.awt.Color NEGATIVE_COLOR = new java.awt.Color(0xff9A2A2A, true);
    public static java.awt.Color NEGATIVE_HIGHLIGHT_COLOR = new java.awt.Color(0xffe96d71, true);
    public static java.awt.Color NEUTRAL_COLOR = new java.awt.Color(0x111111);

    public final static int CONNECTED = 4;
    private WebSocketClient m_websocketClient = null;

    private File m_appDir = null;
    private File m_dataFile = null;

    private Stage m_appStage = null;
    private boolean m_isMax = false;
    private double m_prevHeight = -1;
    private double m_prevX = -1;
    private double m_prevY = -1;
    //private static long MIN_QUOTE_MILLIS = 5000;

    private SimpleObjectProperty<JsonObject> m_cmdObjectProperty = new SimpleObjectProperty<>(null);

    private ArrayList<MessageInterface> m_msgListeners = new ArrayList<>();
    private SimpleObjectProperty<JsonObject> m_socketMsg = new SimpleObjectProperty<>(null);

   //private AtomicInteger m_reconnectTries = new AtomicInteger(0);

    public KucoinExchange(NetworksData networksData) {
        this(null, networksData);
        setup(null);
        addListeners();
    }

    public KucoinExchange(JsonObject jsonObject, NetworksData networksData) {
        super(getAppIcon(), NAME, NETWORK_ID, networksData);

        setup(jsonObject);
        addListeners();
    }

    public void addListeners(){
        getNetworksData().getAppData().appKeyProperty().addListener((obs,oldval,newval)->{
            new KuCoinDataList(this, oldval, newval);
        });
    }

    public SimpleObjectProperty<JsonObject> cmdObjectProperty() {

        return m_cmdObjectProperty;
    }

    public void addMsgListener(MessageInterface item) {
        if (!m_msgListeners.contains(item)) {

            if (m_connectionStatus.get() == 0) {
                connectToExchange();
            }

            m_msgListeners.add(item);

            if (isClientReady()) {
                if (item.getTunnelId() != null) {
                    openTunnel(item.getTunnelId());
                }
            }

        }

    }

    public MessageInterface getListener(String id) {
        for (int i = 0; i < m_msgListeners.size(); i++) {
            MessageInterface listener = m_msgListeners.get(i);
            if (listener.getId().equals(id)) {
                return listener;
            }
        }
        return null;
    }

    public boolean removeMsgListener(MessageInterface item) {
        /*try {
            Files.writeString(logFile.toPath(), "removing listener:" + item.getId(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/
        MessageInterface listener = getListener(item.getId());
        if (listener != null) {
            boolean removed = m_msgListeners.remove(listener);

            /*try {
                Files.writeString(logFile.toPath(), "removed listener:" + item.getId() + " " + removed, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {

            }*/

            if (m_msgListeners.size() == 0) {
                m_websocketClient.close();

            }
            return removed;
        }
        return false;
    }

    public File getAppDir() {
        return m_appDir;
    }

    public static Image getAppIcon() {
        return App.kucoinImg;
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/kucoin-30.png");
    }

    public File getDataFile() {
        return m_dataFile;
    }


    private void setup(JsonObject jsonObject) {


        String fileString = null;
        String appDirFileString = null;
        if (jsonObject != null) {
            JsonElement appDirElement = jsonObject.get("appDir");
      
            JsonElement dataFileElement = jsonObject.get("dataFile");

            fileString = dataFileElement == null ? null : dataFileElement.toString();

            appDirFileString = appDirElement == null ? null : appDirElement.getAsString();

        }

        m_appDir = appDirFileString == null ? new File(getNetworksData().getAppDir().getAbsolutePath() + "/" + NAME) : new File(appDirFileString);

        if (!m_appDir.isDirectory()) {

            try {
                Files.createDirectories(m_appDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }

        }

   
        m_dataFile = new File(fileString == null ? m_appDir.getAbsolutePath() + "/" + NAME + ".dat" : fileString);

    }

    public void open() {
        super.open();
        showAppStage();
    }

    public Stage getAppStage() {
        return m_appStage;
    }

    public void subscribeLevel2Depth5(String tunnelId, String symbol) {

        m_websocketClient.send("{\"id\": \"" + m_clientId + "\", \"tunnelId\": \"" + tunnelId + "\", \"type\": \"subscribe\", \"/spotMarket/level2Depth5:" + symbol + "\", \"response\": true}");
    }

    public void unsubscribeLevel2Depth5(String tunnelId, String symbol) {
        m_websocketClient.send(createMessageString(tunnelId, "unsubscribe", "/spotMarket/level2Depth5:" + symbol, true));
    }

    private ArrayList<String> m_openTunnels = new ArrayList<String>();

    public boolean openTunnel(String tunnelId) {
        if (m_openTunnels.size() == 5) {
            return false;
        }
        /*try {
            Files.writeString(logFile.toPath(), "opening tunnel " + tunnelId, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/
        if (!m_openTunnels.contains(tunnelId)) {
            m_openTunnels.add(tunnelId);
            m_websocketClient.send("{\"id\": \"" + m_clientId + "\", \"type\": \"openTunnel\", \"newTunnelId\": \"" + tunnelId + "\", \"response\": true}");
        }
        return true;
    }

    public boolean closeTunnel(String tunnelId) {

        m_openTunnels.remove(tunnelId);

        /*try {
            Files.writeString(logFile.toPath(), "closing tunnel" + tunnelId, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/
        m_websocketClient.send("{\"id\": \"" + m_clientId + "\", \"type\": \"closeTunnel\", \"tunnelId\": \"" + tunnelId + "\", \"response\": true}");

        return true;

    }

    private void showAppStage() {
        if (m_appStage == null) {

            KuCoinDataList kucoinData = new KuCoinDataList(this);

            if (isClientReady()) {
                openTunnel(getNetworkId());
            }

            m_connectionStatus.addListener((obs, oldVal, newVal) -> {
                if (newVal.intValue() == 4) {
                    openTunnel(getNetworkId());
                }
            });

            MessageInterface msgInterface = new MessageInterface() {
                public String getTunnelId() {
                    return KucoinExchange.NETWORK_ID;
                }

                public String getId() {
                    return getNetworkId();
                }

                public void onMsgChanged(JsonObject jsonObject) {

                }

                public void onReady() {

                }

                public String getSubject() {
                    return null;
                }

                public String getTopic() {
                    return null;
                }

            };
            addMsgListener(msgInterface);

            double appStageWidth = 450;
            double appStageHeight = 600;

            m_appStage = new Stage();
            m_appStage.getIcons().add(KucoinExchange.getSmallAppIcon());
            m_appStage.initStyle(StageStyle.UNDECORATED);
            m_appStage.setTitle(NAME);

            Button closeBtn = new Button();

            Runnable runClose = () -> {

                if (isClientReady()) {
                    closeTunnel(getNetworkId());
                }
                removeMsgListener(msgInterface);
                kucoinData.closeAll();
                kucoinData.removeUpdateListener();

                m_appStage = null;

            };

            closeBtn.setOnAction(closeEvent -> {
                m_appStage.close();
                runClose.run();
            });

            Button maxBtn = new Button();

            HBox titleBox = App.createTopBar(getSmallAppIcon(), maxBtn, closeBtn, m_appStage);
            titleBox.setPadding(new Insets(7, 8, 5, 10));

            m_appStage.titleProperty().bind(Bindings.concat(NAME, " - ", kucoinData.statusProperty()));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Tooltip refreshTip = new Tooltip("Refresh");
            refreshTip.setShowDelay(new javafx.util.Duration(100));
            refreshTip.setFont(App.txtFont);

            BufferedButton refreshBtn = new BufferedButton("/assets/refresh-white-30.png",App.MENU_BAR_IMAGE_WIDTH);
     
            refreshBtn.setId("menuBtn");
            refreshBtn.setOnAction(refreshAction -> {
                refreshBtn.setDisable(true);
                refreshBtn.setImage(new Image("/assets/sync-30.png"));
                kucoinData.updateTickers();
            });

            TextField searchField = new TextField();
            searchField.setPromptText("Search");
            searchField.setId("urlField");
            searchField.setPrefWidth(200);
            searchField.setPadding(new Insets(2, 10, 3, 10));
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                kucoinData.setSearchText(searchField.getText());
            });

            Region menuBarRegion = new Region();
            HBox.setHgrow(menuBarRegion, Priority.ALWAYS);

            HBox menuBar = new HBox(menuBarRegion, searchField, refreshBtn);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 10, 1, 5));

            VBox favoritesVBox = kucoinData.getFavoriteGridBox();

            ScrollPane favoriteScroll = new ScrollPane(favoritesVBox);
            favoriteScroll.setPadding(new Insets(5, 0, 5, 5));
            favoriteScroll.setId("bodyBox");

            VBox chartList = kucoinData.getGridBox();

            ScrollPane scrollPane = new ScrollPane(chartList);
            scrollPane.setPadding(SMALL_INSETS);
            scrollPane.setId("bodyBox");

            VBox bodyPaddingBox = new VBox(scrollPane);
            bodyPaddingBox.setPadding(SMALL_INSETS);

            Font smallerFont = Font.font("OCR A Extended", 10);

            Text lastUpdatedTxt = new Text("Updated ");
            lastUpdatedTxt.setFill(App.formFieldColor);
            lastUpdatedTxt.setFont(smallerFont);

            TextField lastUpdatedField = new TextField();
            lastUpdatedField.setEditable(false);
            lastUpdatedField.setId("formField");
            lastUpdatedField.setFont(smallerFont);
            lastUpdatedField.setPrefWidth(165);

            HBox lastUpdatedBox = new HBox(lastUpdatedTxt, lastUpdatedField);
            lastUpdatedBox.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(lastUpdatedBox, Priority.ALWAYS);

            VBox footerVBox = new VBox(lastUpdatedBox);
            HBox.setHgrow(footerVBox, Priority.ALWAYS);
            footerVBox.setPadding(SMALL_INSETS);

            VBox headerVBox = new VBox(titleBox);
            headerVBox.setPadding(new Insets(0, 5, 0, 5));
            headerVBox.setAlignment(Pos.TOP_CENTER);

            VBox layoutBox = new VBox(headerVBox, bodyPaddingBox, footerVBox);

            HBox menuPaddingBox = new HBox(menuBar);
            menuPaddingBox.setPadding(new Insets(0, 0, 0, 0));

            kucoinData.statusProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !(newVal.equals("Loading..."))) {
                    if (!headerVBox.getChildren().contains(menuPaddingBox)) {
                        headerVBox.getChildren().add(1, menuPaddingBox);

                    }

                } else {
                    if (headerVBox.getChildren().contains(menuPaddingBox)) {
                        headerVBox.getChildren().remove(menuPaddingBox);

                    }

                }
            });

            favoritesVBox.getChildren().addListener((Change<? extends Node> changeEvent) -> {
                int numFavorites = favoritesVBox.getChildren().size();
                if (numFavorites > 0) {
                    if (!headerVBox.getChildren().contains(favoriteScroll)) {

                        headerVBox.getChildren().add(favoriteScroll);
                        menuPaddingBox.setPadding(new Insets(0, 0, 5, 0));
                    }
                    int favoritesHeight = numFavorites * 40 + 21;
                    favoriteScroll.setPrefViewportHeight(favoritesHeight > 175 ? 175 : favoritesHeight);
                } else {
                    if (headerVBox.getChildren().contains(favoriteScroll)) {

                        headerVBox.getChildren().remove(favoriteScroll);
                        menuPaddingBox.setPadding(new Insets(0, 0, 0, 0));
                    }
                }
            });

 
            
            Scene appScene = new Scene(layoutBox, appStageWidth, appStageHeight);
            appScene.setFill(null);
            appScene.getStylesheets().add("/css/startWindow.css");
            m_appStage.setScene(appScene);
            m_appStage.show();

            bodyPaddingBox.prefWidthProperty().bind(m_appStage.widthProperty());
            scrollPane.prefViewportWidthProperty().bind(m_appStage.widthProperty());

            favoriteScroll.prefViewportWidthProperty().bind(m_appStage.widthProperty());

            scrollPane.prefViewportHeightProperty().bind(m_appStage.heightProperty().subtract(headerVBox.heightProperty()).subtract(footerVBox.heightProperty()));

            chartList.prefWidthProperty().bind(scrollPane.prefViewportWidthProperty().subtract(40));
            favoritesVBox.prefWidthProperty().bind(favoriteScroll.prefViewportWidthProperty().subtract(40));

            kucoinData.getLastUpdated().addListener((obs, oldVal, newVal) -> {
                refreshBtn.setDisable(false);
                refreshBtn.setImage(new Image("/assets/refresh-white-30.png"));
                String dateString = Utils.formatDateTimeString(newVal);

                lastUpdatedField.setText(dateString);
            });

        

            ResizeHelper.addResizeListener(m_appStage, 250, 300, 500, Double.MAX_VALUE);

            maxBtn.setOnAction(e -> {
                 if(m_isMax){
                    
                    m_appStage.setX(m_prevX);
                    m_appStage.setHeight(m_prevHeight);
                    m_appStage.setY(m_prevY);
                    m_prevX = -1;
                    m_prevY = -1;
                    m_prevHeight = -1;
                    m_isMax = false;
                }else{
                    m_isMax = true;
                    m_prevY = m_appStage.getY();
                    m_prevX = m_appStage.getX();
                    m_prevHeight = m_appStage.getScene().getHeight();
                    m_appStage.setMaximized(true);
                    FxTimer.runLater(Duration.ofMillis(100), ()->{
                        double height = m_appStage.getScene().getHeight();
                       // double width = m_appStage.getScene().getWidth();
                        double x = m_appStage.getX();
                        double y = m_appStage.getY();
                        m_appStage.setMaximized(false);
                        FxTimer.runLater(Duration.ofMillis(100), ()->{
                            m_appStage.setX(x);
                            m_appStage.setY(y);
                            m_appStage.setHeight(height);
                        });
                    });
                }

            });

           

            m_appStage.setOnCloseRequest(e -> runClose.run());

        } else {
            m_appStage.show();
            Platform.runLater(()->m_appStage.requestFocus());
        }
    }
    private SimpleIntegerProperty m_connectionStatus = new SimpleIntegerProperty(0);

    public static String PUBLIC_TOKEN_URL = "https://api.kucoin.com/api/v1/bullet-public";

    public static String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";

    public void requestSocket(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        /*try {
            Files.writeString(logFile.toPath(), "Requesting socket: " + PUBLIC_TOKEN_URL, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e1) {

        }*/
        Task<JsonObject> task = new Task<JsonObject>() {
            @Override
            public JsonObject call() {
                InputStream inputStream = null;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try {
                    URL url = new URL(PUBLIC_TOKEN_URL);

                    //  String urlParameters = "param1=a&param2=b&param3=c";
                    //  byte[] postData = new byte[]{};
                    //  int postDataLength = postData.length;
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

                    //  conn.setDoOutput(false);
                    //   conn.setInstanceFollowRedirects(false);
                    conn.setRequestMethod("POST");
                    //  conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    //   conn.setRequestProperty("charset", "utf-8");
                    //  conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                    //   conn.setUseCaches(false);

                    conn.setRequestProperty("Content-Type", "application/json");

                    conn.setRequestProperty("User-Agent", USER_AGENT);

                    // long contentLength = con.getContentLengthLong();
                    inputStream = conn.getInputStream();

                    byte[] buffer = new byte[2048];

                    int length;
                    // long downloaded = 0;

                    while ((length = inputStream.read(buffer)) != -1) {

                        outputStream.write(buffer, 0, length);
                        //   downloaded += (long) length;

                    }
                    String jsonString = outputStream.toString();

                    /*try {
                        Files.writeString(logFile.toPath(), "\nKucoin post results:\n" + jsonString + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e1) {

                    }*/

                    JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
                    return jsonObject;
                } catch (JsonParseException | IOException e) {
                    return null;
                }

            }

        };
        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void connectToExchange() {
        if (m_connectionStatus.get() == 0) {
            m_connectionStatus.set(1);

            requestSocket(e -> {

                JsonObject jsonObject = (JsonObject) e.getSource().getValue();

                /*try {
                    Files.writeString(logFile.toPath(), "\nKucoinExchange - connectToExchange:\n" + jsonObject.toString() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }*/

                if (jsonObject != null) {
                    JsonElement dataElement = jsonObject.get("data");
                    if (dataElement != null) {
                        JsonObject dataObject = dataElement.getAsJsonObject();
                        String tokenString = dataObject.get("token").getAsString();
                        JsonArray serversArray = dataObject.get("instanceServers").getAsJsonArray();
                        int i = 0;
                        int serverArraySize = serversArray.size();
                        String endpoint = "";
                        int pingInterval = 18000;

                        while (endpoint.equals("") && i < serverArraySize) {
                            JsonElement arrayElement = serversArray.get(i);
                            if (arrayElement != null) {

                                JsonObject serverObj = arrayElement.getAsJsonObject();
                                endpoint = serverObj.get("endpoint").getAsString();
                                pingInterval = serverObj.get("pingInterval").getAsInt();

                            }
                            i++;
                        }

                        openKucoinSocket(tokenString, endpoint, pingInterval);
                    }
                }
            }, onFailed -> {
                //m_webClient.setReady(false);
            });
        }
    }

    public JsonObject getReadyJson() {
        JsonObject json = new JsonObject();
        json.addProperty("subject", "READY");
        json.addProperty("networkId", getNetworkId());
        return json;
    }

    public void relayMessage(JsonObject messageObject) {
        for (MessageInterface msgInterface : m_msgListeners) {

            msgInterface.onMsgChanged(messageObject);
        }
    }

    public void relayOnReady() {
        for (MessageInterface msgInterface : m_msgListeners) {
            msgInterface.onReady();
        }
    }

    private ScheduledFuture<?> m_future = null;

    private ScheduledExecutorService m_pingTimer = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        }
    });

    public JsonObject getPingObject(String clientId){
        JsonObject pingMessageObj = new JsonObject();
        pingMessageObj.addProperty("id", clientId);
        pingMessageObj.addProperty("type", "ping");
        return pingMessageObj;
    }

      public void startPinging(String clientId, long pingInterval) {


        JsonObject pingMessageObj =    getPingObject(clientId);

          final  String pingString = pingMessageObj.toString();
          //  m_websocketClient.send(pingString);
            m_future = m_pingTimer.scheduleAtFixedRate (()->{

                Platform.runLater(()->{
                    if(m_websocketClient.isOpen()){
                        m_websocketClient.send(pingString);
                    }
                });

            },0, pingInterval, TimeUnit.MILLISECONDS); //(()-> send(pingString), 0, pingInterval);
            
    }

    private void openKucoinSocket(String tokenString, String endpointURL, int pingInterval) {
        m_connectionStatus.set(2);
        m_clientId = FriendlyId.createFriendlyId();
        
        URI uri;
        try {
            uri = new URI(endpointURL + "?token=" + tokenString + "&[connectId=" + m_clientId + "]");
        } catch (URISyntaxException e) {
            m_connectionStatus.set(0);
            return;
        }

        m_websocketClient = new WebSocketClient(uri) {


            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                m_connectionStatus.set(3);
            }

          

            @Override
            public void close() {
                /*try {
                    Files.writeString(logFile.toPath(), "\nclosing", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }*/
           
                super.close();
            }

            @Override
            public void onMessage(String s) {

                /*try {
                    Files.writeString(logFile.toPath(), "\nwebsocket message:\n" + s + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }*/
                if (s != null) {
                    JsonElement messageElement = new JsonParser().parse(s);
                    if (messageElement != null && messageElement.isJsonObject()) {
                        JsonObject messageObject = messageElement.getAsJsonObject();
                        if (messageObject != null) {

                            JsonElement typeElement = messageObject.get("type");

                            if (typeElement != null) {
                                String type = typeElement.getAsString();

                                switch (type) {
                                    case "welcome":
                                        m_connectionStatus.set(4);
                                        JsonElement idElement = messageObject.get("id");
                                        m_clientId = idElement.getAsString();
                                        startPinging(m_clientId, pingInterval);
                                        relayOnReady();
                                        break;
                                    case "pong":
                                        
                                        // m_webClient.setPong(System.currentTimeMillis());
                                        break;
                                    case "message":
                                        /*JsonElement tunnelIdElement = messageObject.get("tunnelId");

                                        if (tunnelIdElement != null) {

                                            String tunnelId = tunnelIdElement.getAsString();

                                        }*/
                                        Platform.runLater(()->relayMessage(messageObject));
                                        break;

                                }
                            }

                        }
                    }

                }

            }

            @Override
            public void onClose(int i, String s, boolean b) {
                if(m_future != null){
                    m_future.cancel(false);
                }
                m_openTunnels.clear();

                m_socketMsg.set(Utils.getCmdObject("close"));
                m_connectionStatus.set(0);
            
                /*ArrayList<WebClientListener> listeners = getMessageListeners();
                for (WebClientListener messagelistener : listeners) {

                    messagelistener.close(i, s, b);
                }*/
            }

            @Override
            public void onError(Exception e) {
                
                try {
                    Files.writeString(logFile.toPath(), "\nKucoinExcehange - websocket error:" + e.toString() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }

            }
        };

        m_websocketClient.connect();

    }

    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        JsonElement subjecElement = note.get("subject");
        JsonElement transactionCurrencyElement = note.get("transactionCurrency");
        JsonElement quoteCurrencyElement = note.get("quoteCurrency");

        if (subjecElement != null) {
            String subject = subjecElement.getAsString();
            switch (subject) {
                case "GET_QUOTE":
                    if (transactionCurrencyElement != null && quoteCurrencyElement != null) {
                      //  String transactionCurrency = transactionCurrencyElement.getAsString();
                      //  String quoteCurrency = quoteCurrencyElement.getAsString();

                        return true;
                    } else {
                        return false;
                    }

            }
        } else {
            return false;
        }
        return true;
    }

    public boolean isClientReady() {
        /*try {
            Files.writeString(logFile.toPath(), "isClientReady():" + (m_websocketClient != null && m_websocketClient.isOpen() && m_connectionStatus.get() == 4), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/
        return m_websocketClient != null && m_websocketClient.isOpen() && m_connectionStatus.get() == 4;
    }

    public void getCandlesDataset(String symbol, String timespan, EventHandler<WorkerStateEvent> onSuccess, EventHandler<WorkerStateEvent> onFailed) {

        String urlString = API_URL + "/api/v1/market/candles?type=" + timespan + "&symbol=" + symbol;
        /*try {
            Files.writeString(logFile.toPath(), "\ngetting url: " + urlString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/
        Utils.getUrlJson(urlString,getNetworksData().getExecService(), onSuccess, onFailed, null);

    }

    public boolean getAllTickers(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/api/v1/market/allTickers";
        /*try {
            Files.writeString(logFile.toPath(), "\ngetting url: " + urlString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/
        Utils.getUrlJson(urlString,getNetworksData().getExecService(), onSucceeded, onFailed, null);

        return false;
    }

    public boolean getTicker(String symbol, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/api/v1/market/orderbook/level1?symbol=" + symbol;
        /*try {
            Files.writeString(logFile.toPath(), "\ngetting ticker: " + symbol, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/
        Utils.getUrlJson(urlString,getNetworksData().getExecService(), onSucceeded, onFailed, null);

        return false;
    }

    public boolean getAllTickers(int page, int pageSize, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/api/v1/market/allTickers?currentPage=" + page + "&pageSize=" + pageSize;
        /*try {
            Files.writeString(logFile.toPath(), "\ngetting url: " + urlString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/
        Utils.getUrlJson(urlString,getNetworksData().getExecService(), onSucceeded, onFailed, null);

        return false;
    }

    public boolean getSymbols(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/api/v2/symbols";
        /*try {
            Files.writeString(logFile.toPath(), "\ngetting url: " + urlString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/
        Utils.getUrlJson(urlString,getNetworksData().getExecService(), onSucceeded, onFailed, null);

        return false;
    }

    public String createMessageString(String type, String topic, boolean response, String id) {
        JsonObject messageObj = new JsonObject();
        messageObj.addProperty("id", id);
        messageObj.addProperty("type", type);
        messageObj.addProperty("topic", topic);
        messageObj.addProperty("response", response);

        return messageObj.toString();
    }

    public String createMessageString(String type, String topic, boolean response) {

        JsonObject messageObj = new JsonObject();
        messageObj.addProperty("id", m_clientId);
        messageObj.addProperty("type", type);
        messageObj.addProperty("topic", topic);
        messageObj.addProperty("response", response);

        return messageObj.toString();
    }

    public String createMessageString(String tunnelId, String type, String topic, boolean response, String id) {

        JsonObject messageObj = new JsonObject();
        messageObj.addProperty("id", id);
        messageObj.addProperty("type", type);
        messageObj.addProperty("topic", topic);
        messageObj.addProperty("tunnelId", tunnelId);
        messageObj.addProperty("response", response);

        return messageObj.toString();
    }

    public String createMessageString(String tunnelId, String type, String topic, boolean response) {

        JsonObject messageObj = new JsonObject();
        messageObj.addProperty("id", m_clientId);
        messageObj.addProperty("type", type);
        messageObj.addProperty("topic", topic);
        messageObj.addProperty("tunnelId", tunnelId);
        messageObj.addProperty("response", response);

        return messageObj.toString();
    }

    public int getConnectionStatus() {
        return m_connectionStatus.get();
    }

    public void subscribeToCandles(String symbol, String timespan) {

        /*try {
            Files.writeString(logFile.toPath(), "\nSubscribing to " + symbol + " - " + timespan, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/

        String topic = "/market/candles:" + symbol + "_" + timespan;

        m_websocketClient.send(createMessageString("subscribe", topic, true));
    }

    public void unsubscribeToCandles(String symbol, String timespan) {

        m_websocketClient.send(createMessageString("unsubscribe", "/market/candles:" + symbol + "_" + timespan, true));
    }

    public void subscribeToCandles(String tunnelId, String symbol, String timespan) {

        /*try {
            Files.writeString(logFile.toPath(), "\nSubscribing to " + symbol + " - " + timespan, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/

        String topic = "/market/candles:" + symbol + "_" + timespan;

        m_websocketClient.send(createMessageString(tunnelId, "subscribe", topic, true));
    }

    public void unsubscribeToCandles(String tunnelId, String symbol, String timespan) {

        m_websocketClient.send(createMessageString(tunnelId, "unsubscribe", "/market/candles:" + symbol + "_" + timespan, true));
    }

    public void subscribeToTicker(String id, String symbol) {
        /*try {
            Files.writeString(logFile.toPath(), "subscribing to ticker " + symbol, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/
        m_websocketClient.send(createMessageString("subscribe", "/market/ticker:" + symbol, true, id));
    }

    public boolean tickerNeeded(String symbol) {
        for (int i = 0; i < m_msgListeners.size(); i++) {
            MessageInterface listener = m_msgListeners.get(i);
            String subject = listener.getSubject();
            if (subject != null && subject.equals("trade.ticker")) {
                String topic = listener.getTopic();

                if (topic != null && topic.substring(topic.length() - symbol.length(), topic.length()).equals(symbol)) {
                    return true;
                }
            }

        }
        return false;
    }

    public void unsubscribeToTicker(String id, String symbol) {
        if (m_msgListeners.size() != 0) {
            /*try {
                Files.writeString(logFile.toPath(), "\nunsubscribing from ticker " + symbol, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {

            }*/
            if (!tickerNeeded(symbol)) {

                m_websocketClient.send(createMessageString("unsubscribe", "/market/ticker:" + symbol, true, id));
            } else {
                try {
                    Files.writeString(logFile.toPath(), "\nKuCoinExchange - ticker needed: not unsubscribing", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }
            }
        } else {
            /*try {
                Files.writeString(logFile.toPath(), "\nunsubscribe ticker: " + symbol + " not needed (no listeners, auto-shutdown expected.)" + symbol, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {

            }*/
        }
    }

    @Override
    public IconButton getButton(String iconStyle) {

        IconButton iconButton = new IconButton(iconStyle.equals(IconStyle.ROW) ? getSmallAppIcon() : getAppIcon(), getName(), iconStyle) {
            @Override
            public void open() {
                getOpen();
            }
        };

        return iconButton;
    }
}
