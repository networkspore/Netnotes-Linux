package com.netnotes;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Ping;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
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
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class ErgoNodeData {

    File logFile = new File("netnotes-log.txt");

    public final static String PUBLIC = "PUBLIC";
    public final static String PRIVATE = "PRIVATE";

    public final static String LIGHT_CLIENT = "Remote Node";
    public final static String NODE_INSTALLER = "Node Installer";
    public final static String LOCAL_NODE = "Local Node";
    
    public double SETUP_STAGE_WIDTH = 700;
  

    private final SimpleObjectProperty< NamedNodeUrl> m_namedNodeUrlProperty = new SimpleObjectProperty<>();

    private String m_imgUrl = "/assets/ergoNodes-30.png";

    private String m_radioOffUrl = "/assets/radio-button-off-30.png";
    private String m_radioOnUrl = "/assets/radio-button-on-30.png";

    private Font m_largeFont = Font.font("OCR A Extended", FontWeight.BOLD, 25);
    private Font m_font = Font.font("OCR A Extended", FontWeight.BOLD, 13);
    private Font m_smallFont = Font.font("OCR A Extended", FontWeight.NORMAL, 10);

    private Color m_secondaryColor = new Color(.4, .4, .4, .9);
    private Color m_primaryColor = new Color(.7, .7, .7, .9); 

    private String m_startImgUrl = "/assets/play-30.png";
    private String m_stopImgUrl = "/assets/stop-30.png";
   
    private final SimpleStringProperty m_statusProperty = new SimpleStringProperty(App.STATUS_STOPPED);
    private final SimpleStringProperty m_statusString = new SimpleStringProperty("");
    private final SimpleObjectProperty<LocalDateTime> m_shutdownNow = new SimpleObjectProperty<>(LocalDateTime.now());
    private final SimpleStringProperty m_cmdProperty = new SimpleStringProperty("");
    private final SimpleStringProperty m_cmdStatusUpdated = new SimpleStringProperty(String.format("%29s", Utils.formatDateTimeString(LocalDateTime.now())));
    private final SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<LocalDateTime>(null);

    private ChangeListener<LocalDateTime> m_updateListener = null;

    private Stage m_settingsStage = null;


    //  public SimpleStringProperty nodeApiAddress;
    private ErgoNodesList m_ergoNodesList;

    private String m_clientType = LIGHT_CLIENT;

    private final SimpleBooleanProperty m_availableProperty = new SimpleBooleanProperty(false);
    private final SimpleObjectProperty<Ping> m_pingProperty = new SimpleObjectProperty<>(null);

    public ErgoNodeData(ErgoNodesList nodesList, JsonObject jsonObj) {
        m_ergoNodesList = nodesList;

        openJson(jsonObj);
        m_pingProperty.addListener(m_pingListener);
    }

    public ErgoNodeData(ErgoNodesList ergoNodesList, String clientType, NamedNodeUrl namedNodeUrl) {
        m_ergoNodesList = ergoNodesList;
        m_clientType = clientType;

        m_namedNodeUrlProperty.set(namedNodeUrl == null ? new NamedNodeUrl() : namedNodeUrl);
        m_pingProperty.addListener(m_pingListener);
    }

    public SimpleObjectProperty< NamedNodeUrl> namedNodeUrlProperty(){
      return m_namedNodeUrlProperty;
    }

    public SimpleBooleanProperty isAvailableProperty(){
        return m_availableProperty;
    }

   public NamedNodeUrl getNamedNodeUrl(){
        return m_namedNodeUrlProperty.get();
   }

   public void setNamedNodeUrl(NamedNodeUrl nodeUrl){
        m_namedNodeUrlProperty.set(nodeUrl);
        m_lastUpdated.set(LocalDateTime.now());
   }

    public SimpleStringProperty statusProperty(){
        return m_statusProperty;
    }

    public SimpleStringProperty statusString(){
        return m_statusString;
    }

    public SimpleObjectProperty<LocalDateTime> shutdownNow(){
        return m_shutdownNow;
    }

    public SimpleStringProperty cmdProperty(){
        return m_cmdProperty;
    }

    public SimpleStringProperty cmdStatusUpdated(){
        return m_cmdStatusUpdated;
    }

    public SimpleObjectProperty<LocalDateTime> lastUpdated(){
        return m_lastUpdated;
    }

    public String getId() {
        return m_namedNodeUrlProperty.get().getId();
    }

    public String getClientType() {
        return m_clientType;
    }

    public String getName() {
        return m_namedNodeUrlProperty.get() == null ? "INVALID NODE" : m_namedNodeUrlProperty.get().getName();
    }

    public NetworkType getNetworkType() {
        return m_namedNodeUrlProperty.get() == null ? null : m_namedNodeUrlProperty.get().getNetworkType();
    }

    public void openJson(JsonObject jsonObj) {

        JsonElement namedNodeElement = jsonObj == null ? null : jsonObj.get("namedNode");

        m_namedNodeUrlProperty.set(namedNodeElement != null && namedNodeElement.isJsonObject() ? new NamedNodeUrl(namedNodeElement.getAsJsonObject()) : new NamedNodeUrl());

    }

    public Image getIcon() {
        return new Image(m_imgUrl == null ? "/assets/ergoNodes-30.png" : m_imgUrl);
    }

    public JsonObject getJsonObject() {
        NamedNodeUrl namedNodeUrl = m_namedNodeUrlProperty.get();

        JsonObject json = new JsonObject();

        if (namedNodeUrl != null) {
            json.add("namedNode", namedNodeUrl.getJsonObject());
        }

        return json;

    }

    public String getNetworkTypeString() {
        return getNetworkType() != null ? getNetworkType().toString() : "NONE";
    }

    public ErgoNodesList getErgoNodesList() {
        return m_ergoNodesList;
    }

    public String getRadioOnUrl() {
        return m_radioOnUrl;
    }

    public String getRadioOffUrl() {
        return m_radioOffUrl;
    }


    public String getStopImgUrl() {
        return m_stopImgUrl;
    }

    public String getStartImgUrl() {
        return m_startImgUrl;
    }

    public Font getFont() {
        return m_font;
    }

    public Font getSmallFont() {
        return m_smallFont;
    }

    public Font getLargeFont() {
        return m_largeFont;
    }

    public Color getPrimaryColor() {
        return m_primaryColor;
    }

    public Color getSecondaryColor() {
        return m_secondaryColor;
    }

    public void stop() {

    }

    public HBox getStatusBox(){
    
        String centerString = "";

        Text topInfoStringText = new Text((m_namedNodeUrlProperty.get() != null ? (getName() == null ? "INVALID" : getName()) : "INVALID"));
        topInfoStringText.setFont(m_font);
        topInfoStringText.setFill(m_primaryColor);

        Text topRightText = new Text(getClientType());
        topRightText.setFont(m_smallFont);
        topRightText.setFill(m_secondaryColor);

        Text botTimeText = new Text();
        botTimeText.setFont(m_smallFont);
        botTimeText.setFill(m_secondaryColor);
        botTimeText.textProperty().bind(m_cmdStatusUpdated);
        
        Text centerField = new Text(centerString);
        centerField.setFont(App.txtFont);
        centerField.setFill(App.txtColor);
       // centerField.setEditable(false);

       // centerField.setPadding(new Insets(0, 10, 0, 0));

        centerField.textProperty().bind(m_statusString);

        Text middleTopRightText = new Text();
        middleTopRightText.setFont(m_font);
        middleTopRightText.setFill(m_secondaryColor);

        middleTopRightText.textProperty().bind(m_cmdProperty);

        Text middleBottomRightText = new Text(getNetworkTypeString());
        middleBottomRightText.setFont(m_font);
        middleBottomRightText.setFill(m_primaryColor);

        VBox centerRightBox = new VBox(middleTopRightText, middleBottomRightText);
        centerRightBox.setAlignment(Pos.CENTER_RIGHT);

        VBox.setVgrow(centerRightBox, Priority.ALWAYS);

        Region currencySpacer = new Region();
        currencySpacer.setMinWidth(10);

        HBox centerFieldBox = new HBox(centerField);
        centerFieldBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(centerFieldBox, Priority.ALWAYS);

        HBox centerBox = new HBox(centerFieldBox, centerRightBox);
        centerBox.setPadding(new Insets(0, 5, 0, 5));
        centerBox.setAlignment(Pos.CENTER_LEFT);
      //  centerBox.setId("darkBox");

       // centerField.prefWidthProperty().bind(centerBox.widthProperty().subtract(centerRightBox.widthProperty()).subtract(20));

        HBox topSpacer = new HBox();
        HBox bottomSpacer = new HBox();

        topSpacer.setMinHeight(2);
        bottomSpacer.setMinHeight(2);

        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
      //  topSpacer.setId("bodyBox");
      //  bottomSpacer.setId("bodyBox");

        Region topMiddleRegion = new Region();
        HBox.setHgrow(topMiddleRegion, Priority.ALWAYS);

        HBox topBox = new HBox(topInfoStringText, topMiddleRegion, topRightText);
        topBox.setPadding(new Insets(0,5,0,5));
     //   topBox.setId("darkBox");

     


        Text ipText = new Text(m_namedNodeUrlProperty.get() != null ? (m_namedNodeUrlProperty.get().getUrlString() == null ? "IP INVALID" : m_namedNodeUrlProperty.get().getUrlString()) : "Configure node");
        ipText.setFill(m_primaryColor);
        ipText.setFont(m_smallFont);

        Region bottomMiddleRegion = new Region();
        HBox.setHgrow(bottomMiddleRegion, Priority.ALWAYS);

        HBox bottomBox = new HBox(ipText, bottomMiddleRegion, botTimeText);
      //  bottomBox.setId("darkBox");
        bottomBox.setAlignment(Pos.CENTER_LEFT);
        bottomBox.setPadding(new Insets(0,5,0,5));
        HBox.setHgrow(bottomBox, Priority.ALWAYS);

        VBox bodyBox = new VBox(topSpacer, topBox, centerBox, bottomBox, bottomSpacer);
        HBox.setHgrow(bodyBox, Priority.ALWAYS);

        HBox contentsBox = new HBox( bodyBox);
        contentsBox.setPadding(new Insets(0,5,0,5));
       // contentsBox.setId("rowBox");
        HBox.setHgrow(contentsBox, Priority.ALWAYS);

        m_lastUpdated.addListener((obs,oldval,newval)->{
            NamedNodeUrl namedNodeUrl = m_namedNodeUrlProperty.get();
            if(namedNodeUrl != null){
                topInfoStringText.setText(namedNodeUrl.getName());
                ipText.setText(namedNodeUrl.getUrlString());
            }
        });
       
        start();
        contentsBox.setId("bodyRowBox");
        return contentsBox;
    }

    public void remove(){
        stop();
        NamedNodeUrl namedNode = m_namedNodeUrlProperty.get();
        if(namedNode != null){
        
            Alert a = new Alert(AlertType.NONE, "Would you like to remove:\n\n" + namedNode.getName() + "\nhttp://" + namedNode.getUrlString(), ButtonType.NO, ButtonType.YES);
            Optional<ButtonType> btnType = a.showAndWait();
            
            if(btnType.isPresent() && btnType.get() == ButtonType.YES){
                m_ergoNodesList.remove(getId());
            }

        }else{
            m_ergoNodesList.remove(getId());
        }
    }

    



    public HBox getRowItem() {

       
       

        BufferedMenuButton itemMenuBtn = new BufferedMenuButton();
        itemMenuBtn.setPadding(new Insets(0));

        MenuItem openItem = new MenuItem("Open");
        openItem.setOnAction(e->{
            open();
        });

        MenuItem removeItem = new MenuItem("Remove");
        removeItem.setOnAction(e->{
            NamedNodeUrl namedNode = m_namedNodeUrlProperty.get();
            if(namedNode != null){
                Alert a = new Alert(AlertType.NONE, "Would you like to remove: \n\n" + namedNode.getName() + "\nhttp://" + namedNode.getUrlString(), ButtonType.NO, ButtonType.YES);
                Optional<ButtonType> btnType = a.showAndWait();
                if(btnType.isPresent() && btnType.get() == ButtonType.YES){
                    remove();
                }
            }else{
                remove();
            }
        });
        
        itemMenuBtn.getItems().addAll(openItem, removeItem);

    

        String centerString = "";

        Text topInfoStringText = new Text((m_namedNodeUrlProperty.get() != null ? (getName() == null ? "INVALID" : getName()) : "INVALID"));
        topInfoStringText.setFont(m_font);
        topInfoStringText.setFill(m_primaryColor);

        Text topRightText = new Text(getClientType());
        topRightText.setFont(m_smallFont);
        topRightText.setFill(m_secondaryColor);

        Text botTimeText = new Text();
        botTimeText.setFont(m_smallFont);
        botTimeText.setFill(m_secondaryColor);
        botTimeText.textProperty().bind(m_cmdStatusUpdated);
        
        TextField centerField = new TextField(centerString);
        centerField.setFont(m_largeFont);
        centerField.setId("formField");
        centerField.setEditable(false);
        centerField.setAlignment(Pos.CENTER);
        centerField.setPadding(new Insets(0, 10, 0, 0));

        centerField.textProperty().bind(m_statusString);

        Text middleTopRightText = new Text();
        middleTopRightText.setFont(m_font);
        middleTopRightText.setFill(m_secondaryColor);

        middleTopRightText.textProperty().bind(m_cmdProperty);

        Text middleBottomRightText = new Text(getNetworkTypeString());
        middleBottomRightText.setFont(m_font);
        middleBottomRightText.setFill(m_primaryColor);

        VBox centerRightBox = new VBox(middleTopRightText, middleBottomRightText);
        centerRightBox.setAlignment(Pos.CENTER_RIGHT);

        VBox.setVgrow(centerRightBox, Priority.ALWAYS);

        Tooltip statusBtnTip = new Tooltip(m_statusProperty.get().equals(App.STATUS_STOPPED) ? "Ping" : "Stop");
        statusBtnTip.setShowDelay(new Duration(100));
        BufferedButton statusBtn = new BufferedButton(m_statusProperty.get().equals(App.STATUS_STOPPED) ? m_startImgUrl : m_stopImgUrl, 15);
        statusBtn.setId("statusBtn");
        statusBtn.setPadding(new Insets(0, 10, 0, 10));
        statusBtn.setTooltip(statusBtnTip);
        statusBtn.setOnAction(action -> {
            if (m_statusProperty.get().equals(App.STATUS_STOPPED)) {
                start();
            } else {
                m_shutdownNow.set(LocalDateTime.now());
            }
        });

        m_statusProperty.addListener((obs, oldVal, newVal) -> {
            switch (m_statusProperty.get()) {
                case App.STATUS_STOPPED:

                    statusBtnTip.setText("Ping");
                    statusBtn.getBufferedImageView().setDefaultImage(new Image(m_startImgUrl), 15);
                    /*if (!availableProperty.get()) {
                        defaultIdBtn.setGraphic(IconButton.getIconView(new Image(getPowerOffUrl()), 15));
                    } else {
                        defaultIdBtn.setGraphic(IconButton.getIconView(new Image(getPowerOnUrl()), 15));
                    }*/

                    break;
                default:
                    if (!statusBtnTip.getText().equals("Stop")) {
                        statusBtnTip.setText("Stop");
                        statusBtn.getBufferedImageView().setDefaultImage(new Image(m_stopImgUrl), 15);
                        /*if (!availableProperty.get()) {
                            defaultIdBtn.setGraphic(IconButton.getIconView(new Image(getPowerInitUrl()), 15));
                        }*/
                    }
                    break;
            }
        });

        HBox leftBox = new HBox();
        leftBox.setPadding(new Insets(5));
        HBox rightBox = new HBox(statusBtn);

        leftBox.setAlignment(Pos.CENTER_LEFT);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        leftBox.setId("bodyBox");
        rightBox.setId("bodyBox");

        Region currencySpacer = new Region();
        currencySpacer.setMinWidth(10);

        HBox centerBox = new HBox(itemMenuBtn, centerField, centerRightBox);
        centerBox.setPadding(new Insets(0, 5, 0, 5));
        centerBox.setAlignment(Pos.CENTER_RIGHT);
        centerBox.setId("darkBox");

        centerField.prefWidthProperty().bind(centerBox.widthProperty().subtract(centerRightBox.widthProperty()).subtract(20));

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

        Region topRightMiddleRegion = new Region();
        HBox.setHgrow(topRightMiddleRegion, Priority.ALWAYS);

        TextField topMiddleField = new TextField();
        topMiddleField.setFont(App.titleFont);
        topMiddleField.setId("smallPrimaryColor");
        topMiddleField.setAlignment(Pos.CENTER);
        HBox.setHgrow(topMiddleField, Priority.ALWAYS);


        HBox topBox = new HBox(topInfoStringText, topMiddleField,  topRightText);
        topBox.setId("darkBox");

        Text ipText = new Text(m_namedNodeUrlProperty.get() != null ? (m_namedNodeUrlProperty.get().getUrlString() == null ? "IP INVALID" : m_namedNodeUrlProperty.get().getUrlString()) : "Configure node");
        ipText.setFill(m_primaryColor);
        ipText.setFont(m_smallFont);

        TextField bottomMiddleField = new TextField();
        bottomMiddleField.setFont(App.titleFont);
        bottomMiddleField.setId("formFieldSmall");
        bottomMiddleField.setAlignment(Pos.CENTER);
        bottomMiddleField.setPadding(new Insets(0));
        HBox.setHgrow(bottomMiddleField, Priority.ALWAYS);

        Binding<String> lastPingBinding = Bindings.createObjectBinding(()->m_pingProperty.get() == null ? "" : "    Ping: " + (m_pingProperty.get().getAvailable() ? m_pingProperty.get().getPing() + " ms" : m_pingProperty.get().getError()), m_pingProperty);

        bottomMiddleField.textProperty().bind(Bindings.concat(lastPingBinding, "       "));
        
        HBox bottomMiddleBox = new HBox(bottomMiddleField);
        HBox.setHgrow(bottomMiddleBox, Priority.ALWAYS);

        HBox bottomBox = new HBox(ipText, bottomMiddleBox, botTimeText);
        bottomBox.setId("darkBox");
        bottomBox.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(bottomBox, Priority.ALWAYS);

        VBox bodyBox = new VBox(topSpacer, topBox, centerBox, bottomBox, bottomSpacer);
        HBox.setHgrow(bodyBox, Priority.ALWAYS);

       

        HBox contentsBox = new HBox(leftBox,  bodyBox, rightBox);
     
        HBox.setHgrow(contentsBox, Priority.ALWAYS);

        HBox rowBox = new HBox(contentsBox);
        rowBox.setPadding(new Insets(0, 0, 5, 0));
        rowBox.setAlignment(Pos.CENTER_RIGHT);
       
        HBox.setHgrow(rowBox, Priority.ALWAYS);

        rowBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
           
                getErgoNodesList().selectedIdProperty().set(getId());
                e.consume();
           
        });

        Runnable updateSelected = () -> {
            String selectedId = m_ergoNodesList.selectedIdProperty().get();
            boolean isSelected = selectedId != null && getId().equals(selectedId);

            centerField.setId(isSelected ? "selectedField" : "formField");

            rowBox.setId(isSelected ? "selected" : "unSelected");
        };

        m_ergoNodesList.selectedIdProperty().addListener((obs, oldval, newVal) -> updateSelected.run());
        updateSelected.run();

        m_lastUpdated.addListener((obs,oldval,newval)->{
            NamedNodeUrl namedNodeUrl = m_namedNodeUrlProperty.get();
            if(namedNodeUrl != null){
                topInfoStringText.setText(namedNodeUrl.getName());
                ipText.setText(namedNodeUrl.getUrlString());
            }
        });
       

        start();
        return rowBox;
    }
    public boolean isStopped(){
        return statusProperty().get().equals(App.STATUS_STOPPED);
    }
   
    private ChangeListener<Ping> m_pingListener = (obs,oldval,newval) ->{
        
        Runnable setOffline = ()->{
            m_statusString.set(newval != null ? newval.getError() : "Ping unavailable");
            m_cmdStatusUpdated.set(Utils.formatDateTimeString(LocalDateTime.now()));
            m_cmdProperty.set("");
            m_availableProperty.set(false);
        };

        if (newval == null) {
            setOffline.run();
        } else {
            if(newval.getAvailable()){
                m_cmdProperty.set("");
                if(m_clientType != null && m_clientType.equals( LIGHT_CLIENT)){
                    m_availableProperty.set(true);
                }
                m_cmdStatusUpdated.set(Utils.formatDateTimeString(LocalDateTime.now()));
                m_statusString.set("Online");
            
            }else{
                setOffline.run();
            }
           
        }
        m_statusProperty.set(App.STATUS_STOPPED);

    };

    public NetworksData getNetworksData(){
        return getErgoNodesList().getErgoNodes().getNetworksData();
    }
    
    public void start() {
        NamedNodeUrl namedNodeUrl = m_namedNodeUrlProperty.get();
        if (namedNodeUrl != null && namedNodeUrl.getIP() != null &&  m_statusProperty.get().equals(App.STATUS_STOPPED)) {
            


            m_statusProperty.set(App.STATUS_STARTED);
            m_statusString.set("Pinging...");
                
            m_cmdProperty.set("PING");
                    
            Utils.pingIP(namedNodeUrl.getIP(), m_pingProperty, m_ergoNodesList.getErgoNodes().getNetworksData().getExecService());

                    
            

            
            //Thread t = new Thread(r);
           // t.setDaemon(true);
           // t.start();
        }

    }

    public String getCurrentStatus(){
        return statusString().get() != null  && statusString().get() != "" ? statusString().get() : (isAvailable() ? "Online" : "Offline");
    }

    public void addUpdateListener(ChangeListener<LocalDateTime> changeListener) {
        m_updateListener = changeListener;
        if (m_updateListener != null) {
            m_lastUpdated.addListener(m_updateListener);

        }
        // m_lastUpdated.addListener();
    }

    public void removeUpdateListener() {
        if (m_updateListener != null) {
            m_lastUpdated.removeListener(m_updateListener);
            m_updateListener = null;
        }
    }

    
    public Stage getSettingsStage(){
        return m_settingsStage;
    }
    
    public void setSettingsStage(Stage stage){
        m_settingsStage = stage;
    }
    public void open(){
        openSettings();
    }

    public void openSettings() {
        
            if (m_settingsStage == null) {

                m_settingsStage = new Stage();
                Runnable close = () -> {
                    if(m_settingsStage != null){
                        m_settingsStage.close();
                        m_settingsStage = null;
                    }
                };
                m_settingsStage.getIcons().add(getIcon());
                m_settingsStage.setResizable(false);
                m_settingsStage.initStyle(StageStyle.UNDECORATED);

                
                SimpleDoubleProperty rowHeight = new SimpleDoubleProperty(40);

                NamedNodeUrl namedNode = m_namedNodeUrlProperty.get();
                SimpleObjectProperty<NetworkType> networkTypeOption = new SimpleObjectProperty<NetworkType>(namedNode.getNetworkType());
                Button closeBtn = new Button();



                HBox titleBox = App.createTopBar(m_ergoNodesList.getErgoNodes().getSmallAppIcon(), "Edit - Remote Node Config - Ergo Nodes", closeBtn, m_settingsStage);
                
                Text headingText = new Text("Node Config");
                headingText.setFont(App.txtFont);
                headingText.setFill(Color.WHITE);
    
                HBox headingBox = new HBox(headingText);
                headingBox.prefHeight(40);
                headingBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(headingBox, Priority.ALWAYS);
                headingBox.setPadding(new Insets(10, 10, 10, 10));
                headingBox.setId("headingBox");
    
                HBox headingPaddingBox = new HBox(headingBox);
    
                headingPaddingBox.setPadding(new Insets(5, 0, 2, 0));
    
                VBox headerBox = new VBox(titleBox, headingPaddingBox);
    
                headerBox.setPadding(new Insets(0, 5, 0, 5));


                Text nodeName = new Text(String.format("%-13s", "Name"));
                nodeName.setFill(App.txtColor);
                nodeName.setFont(App.txtFont);

                TextField nodeNameField = new TextField(namedNode.getName());
                nodeNameField.setFont(App.txtFont);
                nodeNameField.setId("formField");
                HBox.setHgrow(nodeNameField, Priority.ALWAYS);

                HBox nodeNameBox = new HBox(nodeName, nodeNameField);
                nodeNameBox.setAlignment(Pos.CENTER_LEFT);
                nodeNameBox.minHeightProperty().bind(rowHeight);

                Text networkTypeText = new Text(String.format("%-13s", "Network Type"));
                networkTypeText.setFill(App.txtColor);
                networkTypeText.setFont(App.txtFont);

                MenuButton networkTypeBtn = new MenuButton(namedNode.getNetworkType().toString());
                networkTypeBtn.setFont(App.txtFont);
                networkTypeBtn.setId("formField");
                networkTypeBtn.setUserData(namedNode.getNetworkType());
                HBox.setHgrow(networkTypeBtn, Priority.ALWAYS);

                MenuItem mainnetItem = new MenuItem(NetworkType.MAINNET.toString());
                mainnetItem.setId("rowBtn");
        
                MenuItem testnetItem = new MenuItem(NetworkType.TESTNET.toString());
                testnetItem.setId("rowBtn");
        
                networkTypeBtn.getItems().addAll(mainnetItem, testnetItem);

                HBox networkTypeBox = new HBox(networkTypeText, networkTypeBtn);
                networkTypeBox.setAlignment(Pos.CENTER_LEFT);
                networkTypeBox.minHeightProperty().bind(rowHeight);

                    
                Text apiKeyText = new Text(String.format("%-14s", "API Key"));
                apiKeyText.setFill(getPrimaryColor());
                apiKeyText.setFont((App.txtFont));

                TextField apiKeyField = new TextField(namedNode.getApiKey());
                apiKeyField.setPromptText("Enter key");
                apiKeyField.setId("formField");
                HBox.setHgrow(apiKeyField, Priority.ALWAYS);

                Button showKeyBtn = new Button("(Click to view)");
                showKeyBtn.setId("rowBtn");
                showKeyBtn.setPrefWidth(250);
                showKeyBtn.setPrefHeight(30);
                showKeyBtn.setAlignment(Pos.CENTER_LEFT);


                Runnable updateKey = ()->{
                    String keyString = apiKeyField.getText();

                        try {

                            NamedNodeUrl newNamedNodeUrl = getNamedNodeUrl();
                            newNamedNodeUrl.setApiKey(keyString);
                            setNamedNodeUrl(newNamedNodeUrl);
                            
                        } catch (Exception e1) {
                    
                        }
                };

                Tooltip randomApiKeyTip = new Tooltip("Random API Key");

                BufferedButton hideKeyBtn = new BufferedButton("/assets/eye-off-30.png", App.MENU_BAR_IMAGE_WIDTH);
                BufferedButton saveKeyBtn = new BufferedButton("/assets/save-30.png", App.MENU_BAR_IMAGE_WIDTH);
                BufferedButton randomApiKeyBtn = new BufferedButton("/assets/d6-30.png", App.MENU_BAR_IMAGE_WIDTH);

            

                randomApiKeyBtn.setTooltip(randomApiKeyTip);
                randomApiKeyBtn.setOnAction(e -> {
                    try {
                        int length = Utils.getRandomInt(12, 20);
                        char key[] = new char[length];
                        for (int i = 0; i < length; i++) {
                            key[i] = (char) Utils.getRandomInt(33, 126);
                        }
                        String keyString = new String(key);
                        apiKeyField.setText(keyString);
                    
                    } catch (NoSuchAlgorithmException e1) {
                        Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.CANCEL);
                        a.initOwner(m_settingsStage);
                        a.setHeaderText("Error");
                        a.setTitle("Error");
                        a.show();
                    }
                });

                HBox apiKeyBox = new HBox(apiKeyText, showKeyBtn);
                apiKeyBox.setPadding(new Insets(0));;
                apiKeyBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(apiKeyBox, Priority.ALWAYS);

            
                Runnable hideKey = ()->{
                
                    apiKeyBox.getChildren().removeAll(apiKeyField, hideKeyBtn, randomApiKeyBtn, saveKeyBtn);
            
                    apiKeyBox.getChildren().add(showKeyBtn);
                
                };

                Runnable showKey = ()->{
                    apiKeyField.setText(namedNodeUrlProperty().get().getApiKey());
                    apiKeyBox.getChildren().remove(showKeyBtn);
                    apiKeyBox.getChildren().addAll(apiKeyField, hideKeyBtn, randomApiKeyBtn, saveKeyBtn);
                };

                hideKeyBtn.setOnAction(e->{
                    hideKey.run();
                });
            
                saveKeyBtn.setOnAction(e->{
                    updateKey.run();
                    hideKey.run();
                });

                showKeyBtn.setOnAction(e ->{
                    getNetworksData().verifyAppKey(()->{
                        showKey.run();
                    });
                    
                });

                Text nodePortText = new Text(String.format("%-13s", "Port"));
                nodePortText.setFill(App.txtColor);
                nodePortText.setFont(App.txtFont);

                TextField nodePortField = new TextField("9053");
                nodePortField.setId("formField");
                HBox.setHgrow(nodePortField, Priority.ALWAYS);

                nodePortField.textProperty().addListener((obs, oldval, newVal) -> {

                    if (!newVal.matches("\\d*")) {
                        newVal = newVal.replaceAll("[^\\d]", "");

                    }
                    int intVal = Integer.parseInt(newVal);

                    if (intVal > 65535) {
                        intVal = 65535;
                    }

                    nodePortField.setText(intVal + "");

                });

                nodePortField.focusedProperty().addListener((obs, oldval, newVal) -> {
                    if (!newVal) {
                        String portString = nodePortField.getText();
                        int intVal = Integer.parseInt(portString);

                        if (intVal < 1025) {
                            if (networkTypeOption.get().equals(NetworkType.TESTNET)) {
                                nodePortField.setText(ErgoNodes.TESTNET_PORT + "");
                            } else {
                                nodePortField.setText(ErgoNodes.MAINNET_PORT + "");
                            }

                            Alert portSmallAlert = new Alert(AlertType.NONE, "The minimum port value which may be assigned is: 1025\n\n(Default value used.)", ButtonType.CLOSE);
                            portSmallAlert.initOwner(m_settingsStage);
                            portSmallAlert.setHeaderText("Invalid Port");
                            portSmallAlert.setTitle("Invalid Port");
                            portSmallAlert.show();
                        }

                    }
                });

                HBox nodePortBox = new HBox(nodePortText, nodePortField);
                nodePortBox.setAlignment(Pos.CENTER_LEFT);
                nodePortBox.minHeightProperty().bind(rowHeight);

                testnetItem.setOnAction((e) -> {
                    networkTypeBtn.setText(testnetItem.getText());
                    networkTypeOption.set(NetworkType.TESTNET);
                    int portValue = Integer.parseInt(nodePortField.getText());
                    if (portValue == ErgoNodes.MAINNET_PORT) {
                        nodePortField.setText(ErgoNodes.TESTNET_PORT + "");
                    }
                });

                mainnetItem.setOnAction((e) -> {
                    networkTypeBtn.setText(mainnetItem.getText());
                    networkTypeOption.set(NetworkType.MAINNET);

                    int portValue = Integer.parseInt(nodePortField.getText());
                    if (portValue == ErgoNodes.TESTNET_PORT) {
                        nodePortField.setText(ErgoNodes.MAINNET_PORT + "");
                    }

                });

                Text nodeUrlText = new Text(String.format("%-13s", "IP"));
                nodeUrlText.setFill(App.txtColor);
                nodeUrlText.setFont(App.txtFont);

                TextField nodeUrlField = new TextField(namedNode.getIP());
                nodeUrlField.setFont(App.txtFont);
                nodeUrlField.setId("formField");
                HBox.setHgrow(nodeUrlField, Priority.ALWAYS);

                HBox nodeUrlBox = new HBox(nodeUrlText, nodeUrlField);
                nodeUrlBox.setAlignment(Pos.CENTER_LEFT);
                nodeUrlBox.minHeightProperty().bind(rowHeight);

                Region urlSpaceRegion = new Region();
                urlSpaceRegion.setMinHeight(40);

                Button okButton = new Button("Save");
                okButton.setPrefWidth(100);

                HBox okBox = new HBox(okButton);
                okBox.setAlignment(Pos.CENTER_RIGHT);
                HBox.setHgrow(okBox,Priority.ALWAYS);
                okBox.setPadding(new Insets(10));

                VBox customClientOptionsBox = new VBox(nodeNameBox, networkTypeBox, nodeUrlBox, nodePortBox, apiKeyBox);
                customClientOptionsBox.setPadding(new Insets(15));
                customClientOptionsBox.setId("bodyBox");


                VBox bodyBox = new VBox(customClientOptionsBox, okBox);
                bodyBox.setPadding(new Insets(5));
                bodyBox.setId("bodyBox");
                HBox.setHgrow(bodyBox, Priority.ALWAYS);

                VBox bodyPaddingBox = new VBox(bodyBox);
                bodyPaddingBox.setPadding(new Insets(0,5,5,5));

                Runnable onClose = () ->{
                    if(m_settingsStage != null){
                        m_settingsStage.close();
                        m_settingsStage = null;
                    }
                };

                okButton.setOnAction(e->{
                    try {

                        NamedNodeUrl newNamedNodeUrl = getNamedNodeUrl();
                        newNamedNodeUrl.setName( nodeNameField.getText());
                        newNamedNodeUrl.setIp(nodeUrlField.getText());
                        newNamedNodeUrl.setPort(Integer.parseInt(nodePortField.getText()));
                        newNamedNodeUrl.setApiKey(apiKeyField.getText());
                        newNamedNodeUrl.setNetworkType(networkTypeOption.get());
                        setNamedNodeUrl(newNamedNodeUrl);
                        
                    } catch (Exception e1) {
                
                    }


                    m_ergoNodesList.save();
                    onClose.run();
                });

                closeBtn.setOnAction(e->{
                    onClose.run();
                });

                m_settingsStage.setOnCloseRequest(e->onClose.run());

                VBox layoutBox = new VBox(headerBox, bodyPaddingBox);

                Scene scene = new Scene(layoutBox, SETUP_STAGE_WIDTH, 350);
                scene.setFill(null);
                scene.getStylesheets().add("/css/startWindow.css");

                m_settingsStage.setScene(scene);
                m_settingsStage.setOnCloseRequest(e -> close.run());
                m_settingsStage.show();
          
           
            } else {
                if (m_settingsStage.isIconified()) {
                    m_settingsStage.setIconified(false);
                }
                if(!m_settingsStage.isShowing()){
                    m_settingsStage.show();
                }else{
                    Platform.runLater(()->m_settingsStage.toBack());
                    Platform.runLater(()->m_settingsStage.toFront());
                }
                
            }
     

    }

    public HBox getMenuBar() {
        Tooltip settingsTip = new Tooltip("Settings");
        settingsTip.setShowDelay(new Duration(100));
        BufferedButton settingsBtn = new BufferedButton("/assets/settings-outline-white-120.png", 20);
        settingsBtn.setTooltip(settingsTip);

        Region menuSpacer = new Region();
        HBox.setHgrow(menuSpacer, Priority.ALWAYS);

        HBox menuBar = new HBox(menuSpacer, settingsBtn);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);

        settingsBtn.setOnAction(e -> openSettings());
        return menuBar;
    }



    public boolean isAvailable(){
        return m_availableProperty.get();
    }



  


}
