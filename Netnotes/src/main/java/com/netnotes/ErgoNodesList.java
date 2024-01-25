package com.netnotes;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;

import com.utils.Utils;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class ErgoNodesList {

    //options
    public final static String DEFAULT = "DEFAULT";
    public final static String PUBLIC = "PUBLIC";
    public final static String CUSTOM = "CUSTOM";

    private File logFile = new File("netnotes-log.txt");
    private SimpleStringProperty m_selectedId = new SimpleStringProperty(null);
    private SimpleStringProperty m_defaultId = new SimpleStringProperty(null);

    private ArrayList<ErgoNodeData> m_dataList = new ArrayList<>();
    private ErgoNodes m_ergoNodes;
    private ErgoNodeLocalData m_ergoLocalNode = null;

    private double DEFAULT_ADD_STAGE_WIDTH = 600;
    private double DEFAULT_ADD_STAGE_HEIGHT = 500;

    private final static long EXECUTION_TIME = 500;
    private double m_addStageWidth = DEFAULT_ADD_STAGE_WIDTH;
    private double m_addStageHeight = DEFAULT_ADD_STAGE_HEIGHT;
    private double m_prevAddStageWidth = DEFAULT_ADD_STAGE_WIDTH;
    private double m_prevAddStageHeight = DEFAULT_ADD_STAGE_HEIGHT;
    private boolean m_addStageMaximized = false;
    private ScheduledFuture<?> m_lastExecution = null;

    private SimpleObjectProperty<LocalDateTime> m_doGridUpdate = new SimpleObjectProperty<LocalDateTime>(null);

    private Stage m_addStage = null;
    private String m_defaultAddType = PUBLIC;

    private String m_downloadImgUrl = "/assets/cloud-download-30.png";
    private ChangeListener<LocalDateTime> m_nodeUpdateListener = (obs, oldval, newVal) -> save();

    public ErgoNodesList( ErgoNodes ergoNodes) {
        m_ergoNodes = ergoNodes;
        getFile();
        m_ergoNodes.getNetworksData().getAppData().appKeyProperty().addListener((obs,oldval,newval)->save());
    }

    private void getFile() {
        File dataFile = m_ergoNodes.getDataFile();
        File appDir = m_ergoNodes.getAppDir();

        if (appDir != null && appDir.isDirectory() && dataFile != null && dataFile.isFile()) {
          
            try {
   
                JsonObject json = Utils.readJsonFile(m_ergoNodes.getNetworksData().getAppData().appKeyProperty().get(), m_ergoNodes.getDataFile());
                openJson(json);
            } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                    | IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.setHeaderText("Loading Error");
                a.setTitle("Ergo Nodes: Loading Error");
                a.show();
            }
        } else {
     
            String localNodeId = FriendlyId.createFriendlyId();
            m_ergoLocalNode = new ErgoNodeLocalData(localNodeId, this);
            m_defaultId.set(localNodeId);

            m_ergoLocalNode.lastUpdated().addListener(m_nodeUpdateListener);
            add(new ErgoNodeData(this, ErgoNodeData.LIGHT_CLIENT, new NamedNodeUrl()), true);
        
        }

    }

    private void openJson(JsonObject json) {

        JsonElement nodesElement = json.get("nodes");
        JsonElement defaultIdElement = json.get("defaultId");
        JsonElement defaultAddTypeElement = json.get("defaultAddType");
        JsonElement addStageElement = json.get("addStage");

        m_defaultAddType = defaultAddTypeElement != null && defaultAddTypeElement.isJsonPrimitive() ? defaultAddTypeElement.getAsString() : PUBLIC;

        JsonElement localNodeElement = json == null ? null : json.get("localNode");

        if (localNodeElement != null && localNodeElement.isJsonObject()) {
            JsonObject localNodeObject = localNodeElement.getAsJsonObject();


            JsonElement namedNodeElement = localNodeObject.get("namedNode");

            NamedNodeUrl namedNodeUrl = namedNodeElement != null && namedNodeElement.isJsonObject() ? new NamedNodeUrl(namedNodeElement.getAsJsonObject()) : null;

            if (namedNodeUrl != null) {
                m_ergoLocalNode = new ErgoNodeLocalData(namedNodeUrl, localNodeObject, this);

                m_ergoLocalNode.lastUpdated().addListener(m_nodeUpdateListener);
            } else {
                m_ergoLocalNode = new ErgoNodeLocalData(FriendlyId.createFriendlyId(), this);
                m_ergoLocalNode.lastUpdated().addListener(m_nodeUpdateListener);
            }

        } else {
            m_ergoLocalNode = new ErgoNodeLocalData(FriendlyId.createFriendlyId(), this);
            m_ergoLocalNode.lastUpdated().addListener(m_nodeUpdateListener);
        }

        if (nodesElement != null && nodesElement.isJsonArray()) {
            JsonArray jsonArray = nodesElement.getAsJsonArray();

            for (int i = 0; i < jsonArray.size(); i++) {
                JsonElement nodeElement = jsonArray.get(i);

                if (nodeElement != null && nodeElement.isJsonObject()) {
                    add(new ErgoNodeData(this, nodeElement.getAsJsonObject()), false);
                }
            }
        }

        if (addStageElement != null && addStageElement.isJsonObject()) {
            JsonObject addStageObject = addStageElement.getAsJsonObject();

            JsonElement widthElement = addStageObject.get("width");
            JsonElement heightElement = addStageObject.get("height");
            JsonElement stagePrevWidthElement = addStageObject.get("prevWidth");
            JsonElement stagePrevHeightElement = addStageObject.get("prevHeight");
            JsonElement stageMaximizedElement = addStageObject.get("maximized");

            boolean maximized = stageMaximizedElement != null && stageMaximizedElement.isJsonPrimitive() ? stageMaximizedElement.getAsBoolean() : false;

            if (!maximized) {
                m_addStageWidth = widthElement != null && widthElement.isJsonPrimitive() ? widthElement.getAsDouble() : DEFAULT_ADD_STAGE_WIDTH;
                m_addStageHeight = heightElement != null && heightElement.isJsonPrimitive() ? heightElement.getAsDouble() : DEFAULT_ADD_STAGE_HEIGHT;
            } else {
                double prevWidth = stagePrevWidthElement != null && stagePrevWidthElement.isJsonPrimitive() ? stagePrevWidthElement.getAsDouble() : DEFAULT_ADD_STAGE_WIDTH;
                double prevHeight = stagePrevHeightElement != null && stagePrevHeightElement.isJsonPrimitive() ? stagePrevHeightElement.getAsDouble() : DEFAULT_ADD_STAGE_HEIGHT;

                m_prevAddStageWidth = prevWidth;
                m_prevAddStageHeight = prevHeight;

                m_addStageWidth = prevWidth;
                m_addStageHeight = prevHeight;

            }
        }

        if(defaultIdElement != null && defaultIdElement.isJsonPrimitive()){
            String defaultId = defaultIdElement.getAsString();
            m_defaultId.set(defaultId);
        }

    }

    public String getDownloadImgUrl() {
        return m_downloadImgUrl;
    }

    public void add(ErgoNodeData ergoNodeData, boolean doSave) {
        if (ergoNodeData != null) {
            m_dataList.add(ergoNodeData);
       
            ergoNodeData.addUpdateListener(m_nodeUpdateListener);
            if (doSave) {
                save();
            }
        }
    }

    public void remove(String id) {
        if(id != null ){
            String selectedId = selectedIdProperty().get();
            if(selectedId != null && selectedId.equals(id)){
                selectedIdProperty().set(null);
            }
            boolean updated = false;
            if(m_ergoLocalNode.getId().equals(id)){
                m_ergoLocalNode.remove();
                m_ergoLocalNode = new ErgoNodeLocalData(FriendlyId.createFriendlyId(), this);
                m_ergoLocalNode.addUpdateListener(m_nodeUpdateListener);
                updated = true;
            }else{
                if (m_dataList.size() > 0) {
                    for (int i = 0; i < m_dataList.size(); i++) {
                        ErgoNodeData ergoNodeData = m_dataList.get(i);
                        if (ergoNodeData.getId().equals(id)) {
                            m_dataList.remove(i);
                            ergoNodeData.removeUpdateListener();
                            updated = true;
                        }
                    }
                }
            }
            if(updated){
                save();
                m_doGridUpdate.set(LocalDateTime.now());
            }
        }
        
    }

    public ErgoNodeData getErgoNodeData(String id) {
        if (id != null) {
            if (m_ergoLocalNode != null && m_ergoLocalNode.getId().equals(id)) {
                return m_ergoLocalNode;
            } else {
                for (int i = 0; i < m_dataList.size(); i++) {
                    ErgoNodeData ergoNodeData = m_dataList.get(i);
                    if (ergoNodeData.getId().equals(id)) {
                        return ergoNodeData;
                    }
                }
            }
        }
        return null;
    }

    public JsonArray getNodesJsonArray() {
        JsonArray jsonArray = new JsonArray();

        for (int i = 0; i < m_dataList.size(); i++) {
            ErgoNodeData ergNodeData = m_dataList.get(i);

            jsonArray.add(ergNodeData.getJsonObject());
        }

        return jsonArray;
    }

   

    public JsonObject getAddStageJson() {
        JsonObject json = new JsonObject();
        json.addProperty("width", m_addStageWidth);
        json.addProperty("height", m_addStageHeight);
        json.addProperty("prevWidth", m_prevAddStageWidth);
        json.addProperty("prevHeight", m_prevAddStageHeight);
        json.addProperty("maximized", m_addStageMaximized);
        return json;
    }

     public JsonObject getJsonObject() {
        JsonObject json = new JsonObject();

        json.add("nodes", getNodesJsonArray());
        json.addProperty("defaultAddType", m_defaultAddType);
        json.add("addStage", getAddStageJson());
        if(m_defaultId.get() != null){
            json.addProperty("defaultId", m_defaultId.get());
        }
        json.add("localNode", m_ergoLocalNode.getJsonObject());

        return json;
    }

    public void save() {
        

      
        //  byte[] bytes = jsonString.getBytes(StandardCharsets.UTF_8);
        // String fileHexString = Hex.encodeHexString(bytes);
        File appDir = m_ergoNodes.getAppDir();
        try {
            if (!appDir.isDirectory()) {
                Files.createDirectory(appDir.toPath());
            }
            JsonObject fileJson = getJsonObject();
            Utils.saveJson(m_ergoNodes.getNetworksData().getAppData().appKeyProperty().get(), fileJson, m_ergoNodes.getDataFile());
         
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
                | IOException e) {
            try {
                Files.writeString(logFile.toPath(), "\nErgoNodesList: " + e.toString());
            } catch (IOException e1) {

            }
        }
    }

  

    public VBox getGridBox(SimpleDoubleProperty width, SimpleDoubleProperty scrollWidth) {
        VBox gridBox = new VBox();

        Runnable updateGrid = () -> {
            gridBox.getChildren().clear();

            HBox fullNodeRowItem = m_ergoLocalNode.getRowItem();
            fullNodeRowItem.prefWidthProperty().bind(width.subtract(scrollWidth));
            gridBox.getChildren().add(fullNodeRowItem);
            int numCells = m_dataList.size();

            for (int i = 0; i < numCells; i++) {
                ErgoNodeData nodeData = m_dataList.get(i);
                HBox rowItem = nodeData.getRowItem();
                rowItem.prefWidthProperty().bind(width.subtract(scrollWidth));
                gridBox.getChildren().add(rowItem);
            }

        };

        updateGrid.run();

        m_doGridUpdate.addListener((obs, oldval, newval) -> updateGrid.run());

        return gridBox;
    }

    public void getMenu(MenuButton menuBtn, SimpleObjectProperty<ErgoNodeData> selectedNode){

        Runnable updateMenu = () -> {
            menuBtn.getItems().clear();
            ErgoNodeData newSelectedNodedata = selectedNode.get();

            MenuItem noneMenuItem = new MenuItem("(disabled)");
            if(selectedNode.get() == null){
                noneMenuItem.setId("selectedMenuItem");
            }
            noneMenuItem.setOnAction(e->{
                selectedNode.set(null);
            });
            menuBtn.getItems().add(noneMenuItem);

            MenuItem localNodeMenuItem = new MenuItem( m_ergoLocalNode.getName());
            if(m_ergoLocalNode != null && newSelectedNodedata != null && m_ergoLocalNode.getId().equals(newSelectedNodedata.getId())){
                localNodeMenuItem.setId("selectedMenuItem");
                localNodeMenuItem.setText(localNodeMenuItem.getText() + " (selected)");
            }
            localNodeMenuItem.setOnAction(e->{
                selectedNode.set(m_ergoLocalNode);
            });

            menuBtn.getItems().add( localNodeMenuItem );
            int numCells = m_dataList.size();

            for (int i = 0; i < numCells; i++) {
                
                ErgoNodeData nodeData = m_dataList.get(i);
                 MenuItem menuItem = new MenuItem(nodeData.getName());
                if(newSelectedNodedata != null && newSelectedNodedata.getId().equals(nodeData.getId())){
                    menuItem.setId("selectedMenuItem");
                    menuItem.setText(menuItem.getText() + " (selected)");
                }
                menuItem.setOnAction(e->{
                    selectedNode.set(nodeData);
                });

                menuBtn.getItems().add(menuItem);
            }



        };

        updateMenu.run();

        selectedNode.addListener((obs,oldval, newval) -> updateMenu.run());

        m_doGridUpdate.addListener((obs, oldval, newval) -> updateMenu.run());

    }

    public SimpleStringProperty selectedIdProperty() {
        return m_selectedId;
    }

    public void setselectedId(String id) {
        m_selectedId.set(id);
        save();
    }

    public SimpleStringProperty defaultNodeIdProperty(){
        return m_defaultId;
    }

    public void shutdown() {

    }

    private void setDefaultNodeOption(String option) {
        m_defaultAddType = option;
        save();
    }

    public ErgoNodes getErgoNodes() {
        return m_ergoNodes;
    }

    public void showAddNodeStage() {
        if (m_addStage == null) {
            String friendlyId = FriendlyId.createFriendlyId();

            SimpleStringProperty nodeOption = new SimpleStringProperty(m_defaultAddType);

            // Alert a = new Alert(AlertType.NONE, "updates: " + updatesEnabled, ButtonType.CLOSE);
            //a.show();
            NamedNodesList nodesList = new NamedNodesList(m_ergoNodes.getNetworksData().getAppData().getUpdates());

            //private
            SimpleObjectProperty<NetworkType> networkTypeOption = new SimpleObjectProperty<NetworkType>(NetworkType.MAINNET);
            SimpleStringProperty clientTypeOption = new SimpleStringProperty(ErgoNodeData.LIGHT_CLIENT);

            Image icon = ErgoNodes.getSmallAppIcon();
            String name = m_ergoNodes.getName();

            VBox layoutBox = new VBox();

            m_addStage = new Stage();
            m_addStage.getIcons().add(icon);
            m_addStage.setResizable(false);
            m_addStage.initStyle(StageStyle.UNDECORATED);

            double minWidth = 600;
            double minHeight = 500;

            Scene addNodeScene = new Scene(layoutBox, m_addStageWidth, m_addStageHeight);

            String heading = "Add Node";
            Button closeBtn = new Button();

            String titleString = heading + " - " + name;
            m_addStage.setTitle(titleString);

            Button maximizeBtn = new Button();

            HBox titleBox = App.createTopBar(icon, maximizeBtn, closeBtn, m_addStage);
            Text headingText = new Text(heading);
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

            SimpleDoubleProperty rowHeight = new SimpleDoubleProperty(40);

            Text nodeTypeText = new Text("Type ");
            nodeTypeText.setFill(App.txtColor);
            nodeTypeText.setFont(App.txtFont);

            MenuButton typeBtn = new MenuButton();
            typeBtn.setAlignment(Pos.CENTER_LEFT);

            MenuItem defaultClientItem = new MenuItem("Public node (Remote client)");
            defaultClientItem.setOnAction((e) -> {

                nodeOption.set(PUBLIC);

            });
            defaultClientItem.setId("rowBtn");

            MenuItem configureItem = new MenuItem("Custom (Remote client)");
            configureItem.setOnAction((e) -> {

                nodeOption.set(CUSTOM);

            });

            configureItem.setId("rowBtn");

            typeBtn.getItems().addAll(defaultClientItem, configureItem);

            Text publicNodesText = new Text("Public Nodes");
            publicNodesText.setFill(App.txtColor);
            publicNodesText.setFont(App.txtFont);

            Tooltip enableUpdatesTip = new Tooltip("Update");
            enableUpdatesTip.setShowDelay(new javafx.util.Duration(100));

            BufferedButton getNodesListBtn = new BufferedButton(getDownloadImgUrl(), 30);
            getNodesListBtn.setTooltip(enableUpdatesTip);
            final String updateEffectId = "UPDATE_DISABLED";
            Runnable updateEnableEffect = () -> {
                boolean updatesEnabled = m_ergoNodes.getNetworksData().getAppData().getUpdates();

                enableUpdatesTip.setText("Updates settings: " + (updatesEnabled ? "Enabled" : "Disabled"));
                if (!updatesEnabled) {
                    if (getNodesListBtn.getBufferedImageView().getEffect(updateEffectId) == null) {
                        getNodesListBtn.getBufferedImageView().applyEffect(new InvertEffect(updateEffectId, 0.7));
                    }
                } else {
                    getNodesListBtn.getBufferedImageView().removeEffect(updateEffectId);
                }
            };

            getNodesListBtn.setOnAction((e) -> {
                nodesList.getGitHubList();
            });

        

            updateEnableEffect.run();
            Region btnSpacerRegion = new Region();
            HBox.setHgrow(btnSpacerRegion, Priority.ALWAYS);

            HBox publicNodesBox = new HBox(publicNodesText, btnSpacerRegion, getNodesListBtn);
            publicNodesBox.setAlignment(Pos.CENTER_LEFT);
            publicNodesBox.setMinHeight(40);

            Tooltip defaultTypeBtnTip = new Tooltip("Set default");
            defaultTypeBtnTip.setShowDelay(new Duration(100));
            
            BufferedButton defaultTypeBtn = new BufferedButton(m_defaultAddType.equals(nodeOption.get()) ? "/assets/star-30.png" : "/assets/star-outline-30.png", 20);
            defaultTypeBtn.setTooltip(defaultTypeBtnTip);

            defaultTypeBtn.setOnAction(e -> {
                String currentNodeOption = nodeOption.get();
                if (!m_defaultAddType.equals(currentNodeOption)) {
                    setDefaultNodeOption(currentNodeOption);
                    defaultTypeBtn.setImage(new Image("/assets/star-30.png"));

                }
            });

            HBox nodeTypeBox = new HBox(nodeTypeText, typeBtn, defaultTypeBtn);
            nodeTypeBox.setAlignment(Pos.CENTER_LEFT);
            nodeTypeBox.setPadding(new Insets(0));
            nodeTypeBox.minHeightProperty().bind(rowHeight);

            VBox namedNodesGridBox = nodesList.getGridBox();

            ScrollPane namedNodesScroll = new ScrollPane(namedNodesGridBox);
            namedNodesScroll.setId("darkBox");
            namedNodesScroll.setPadding(new Insets(10));

            HBox nodeScrollBox = new HBox(namedNodesScroll);
            nodeScrollBox.setPadding(new Insets(0, 15, 0, 0));

            Text namedNodeText = new Text("Node ");
            namedNodeText.setFill(App.altColor);
            namedNodeText.setFont(App.txtFont);

            Button namedNodeBtn = new Button();
            namedNodeBtn.setId("darkBox");
            namedNodeBtn.setAlignment(Pos.CENTER_LEFT);
            namedNodeBtn.setPadding(new Insets(5, 5, 5, 10));

            Runnable updateSelectedBtn = () -> {
                String selectedId = nodesList.selectedNamedNodeIdProperty().get();
                if (selectedId == null) {
                    namedNodeBtn.setText("(select node)");
                } else {
                    NamedNodeUrl namedNodeUrl = nodesList.getNamedNodeUrl(selectedId);
                    if (namedNodeUrl != null) {
                        namedNodeBtn.setText(namedNodeUrl.getName());
                    } else {
                        namedNodeBtn.setText("(select node)");
                    }
                }
            };

            updateSelectedBtn.run();

            nodesList.selectedNamedNodeIdProperty().addListener((obs, oldval, newVal) -> updateSelectedBtn.run());

            HBox nodesBox = new HBox(namedNodeText, namedNodeBtn);
            nodesBox.setAlignment(Pos.CENTER_LEFT);
            nodesBox.setMinHeight(40);
            nodesBox.setPadding(new Insets(10, 0, 0, 0));

            namedNodeBtn.prefWidthProperty().bind(nodesBox.widthProperty().subtract(namedNodeText.layoutBoundsProperty().get().getWidth()).subtract(15));
            namedNodesScroll.prefViewportWidthProperty().bind(nodesBox.widthProperty());

            SimpleDoubleProperty scrollWidth = new SimpleDoubleProperty(0);

            namedNodesGridBox.heightProperty().addListener((obs, oldVal, newVal) -> {
                double scrollViewPortHeight = namedNodesScroll.prefViewportHeightProperty().doubleValue();
                double gridBoxHeight = newVal.doubleValue();

                if (gridBoxHeight > scrollViewPortHeight) {
                    scrollWidth.set(40);
                }

            });

            nodesList.gridWidthProperty().bind(nodesBox.widthProperty().subtract(40).subtract(scrollWidth));

            VBox lightClientOptions = new VBox(publicNodesBox, nodeScrollBox, nodesBox);

            /* 
            *
            ***********Custom properties 
            *
             */
            Text nodeName = new Text(String.format("%-13s", "Name"));
            nodeName.setFill(App.txtColor);
            nodeName.setFont(App.txtFont);

            TextField nodeNameField = new TextField("Node #" + friendlyId);
            nodeNameField.setFont(App.txtFont);
            nodeNameField.setId("formField");
            HBox.setHgrow(nodeNameField, Priority.ALWAYS);

            HBox nodeNameBox = new HBox(nodeName, nodeNameField);
            nodeNameBox.setAlignment(Pos.CENTER_LEFT);
            nodeNameBox.minHeightProperty().bind(rowHeight);

            Text networkTypeText = new Text(String.format("%-13s", "Network Type"));
            networkTypeText.setFill(App.txtColor);
            networkTypeText.setFont(App.txtFont);

            MenuButton networkTypeBtn = new MenuButton("MAINNET");
            networkTypeBtn.setFont(App.txtFont);
            networkTypeBtn.setId("formField");
            HBox.setHgrow(networkTypeBtn, Priority.ALWAYS);

            MenuItem mainnetItem = new MenuItem("MAINNET");
            mainnetItem.setId("rowBtn");

            MenuItem testnetItem = new MenuItem("TESTNET");
            testnetItem.setId("rowBtn");

            networkTypeBtn.getItems().addAll(mainnetItem, testnetItem);

            HBox networkTypeBox = new HBox(networkTypeText, networkTypeBtn);
            networkTypeBox.setAlignment(Pos.CENTER_LEFT);
            networkTypeBox.minHeightProperty().bind(rowHeight);

            Text apiKeyText = new Text(String.format("%-13s", "API Key"));
            apiKeyText.setFill(App.txtColor);
            apiKeyText.setFont(App.txtFont);

            TextField apiKeyField = new TextField("");
            apiKeyField.setFont(App.txtFont);
            apiKeyField.setId("formField");
            apiKeyField.setPromptText("(enter API Key)");
            HBox.setHgrow(apiKeyField, Priority.ALWAYS);

            HBox apiKeyBox = new HBox(apiKeyText, apiKeyField);
            apiKeyBox.setAlignment(Pos.CENTER_LEFT);
            apiKeyBox.minHeightProperty().bind(rowHeight);

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
                        portSmallAlert.initOwner(m_addStage);
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

            TextField nodeUrlField = new TextField("127.0.0.1");
            nodeUrlField.setFont(App.txtFont);
            nodeUrlField.setId("formField");
            HBox.setHgrow(nodeUrlField, Priority.ALWAYS);

            HBox nodeUrlBox = new HBox(nodeUrlText, nodeUrlField);
            nodeUrlBox.setAlignment(Pos.CENTER_LEFT);
            nodeUrlBox.minHeightProperty().bind(rowHeight);

            Region urlSpaceRegion = new Region();
            urlSpaceRegion.setMinHeight(40);

            VBox customClientOptionsBox = new VBox(nodeNameBox, networkTypeBox, nodeUrlBox, nodePortBox, apiKeyBox);
            customClientOptionsBox.setPadding(new Insets(15, 0, 0, 15));

            VBox bodyOptionBox = new VBox(lightClientOptions);
            bodyOptionBox.setId("bodyBox");
            bodyOptionBox.setPadding(new Insets(0, 0, 15, 15));

            Button nextBtn = new Button("Add");
            nextBtn.setPadding(new Insets(5, 15, 5, 15));

            HBox nextBox = new HBox(nextBtn);
            nextBox.setPadding(new Insets(0, 0, 0, 0));
            nextBox.setMinHeight(50);
            nextBox.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(nextBox, Priority.ALWAYS);

            VBox bodyBox = new VBox(nodeTypeBox, bodyOptionBox, nextBox);
            bodyBox.setId("bodyBox");
            bodyBox.setPadding(new Insets(0, 10, 0, 10));

            VBox bodyPaddingBox = new VBox(bodyBox);
            bodyPaddingBox.setPadding(new Insets(5, 5, 5, 5));

            Region footerSpacer = new Region();
            footerSpacer.setMinHeight(5);

            VBox footerBox = new VBox(footerSpacer);

            layoutBox.getChildren().addAll(headerBox, bodyPaddingBox, footerBox);

            namedNodesScroll.prefViewportHeightProperty().bind(m_addStage.heightProperty().subtract(headerBox.heightProperty()).subtract(footerBox.heightProperty()).subtract(publicNodesBox.heightProperty()).subtract(nodesBox.heightProperty()));

            rowHeight.bind(m_addStage.heightProperty().subtract(headerBox.heightProperty()).subtract(nodeTypeBox.heightProperty()).subtract(footerBox.heightProperty()).subtract(95).divide(5));

            addNodeScene.getStylesheets().add("/css/startWindow.css");
            m_addStage.setScene(addNodeScene);
            m_addStage.show();

            ChangeListener<? super Node> listFocusListener = (obs, oldval, newVal) -> {
                if (newVal != null && newVal instanceof IconButton) {
                    IconButton iconButton = (IconButton) newVal;
                    String btnId = iconButton.getButtonId();
                    if (btnId != null) {
                        nodesList.selectedNamedNodeIdProperty().set(btnId);
                    }
                }
            };

            Runnable setPublic = () -> {

                bodyOptionBox.getChildren().clear();

                bodyOptionBox.getChildren().add(lightClientOptions);

                addNodeScene.focusOwnerProperty().addListener(listFocusListener);
                typeBtn.setText("Public node (Remote client)");
                typeBtn.setPrefWidth(150);
            };

            Runnable setCuston = () -> {
                bodyOptionBox.getChildren().clear();

                bodyOptionBox.getChildren().add(customClientOptionsBox);

                addNodeScene.focusOwnerProperty().removeListener(listFocusListener);
                typeBtn.setText("Custom");
                typeBtn.setPrefWidth(58);
            };

            Runnable switchPublic = () -> {
                switch (nodeOption.get()) {
                    case CUSTOM:
                        setCuston.run();
                        break;
                    default:
                        setPublic.run();
                        break;
                }
            };

            nodeOption.addListener((obs, oldVal, newVal) -> {
                switchPublic.run();
                defaultTypeBtn.getBufferedImageView().setImage(new Image(m_defaultAddType.equals(nodeOption.get()) ? "/assets/star-30.png" : "/assets/star-outline-30.png"));
            });

            switchPublic.run();

            Rectangle maxRect = m_ergoNodes.getNetworksData().getMaximumWindowBounds();

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });

            Runnable setUpdated = () -> {
                save();
            };

            addNodeScene.widthProperty().addListener((obs, oldVal, newVal) -> {
                m_addStageWidth = newVal.doubleValue();

                if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                    m_lastExecution.cancel(false);
                }

                m_lastExecution = executor.schedule(setUpdated, EXECUTION_TIME, TimeUnit.MILLISECONDS);
            });

            addNodeScene.heightProperty().addListener((obs, oldVal, newVal) -> {
                m_addStageHeight = newVal.doubleValue();

                if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                    m_lastExecution.cancel(false);
                }

                m_lastExecution = executor.schedule(setUpdated, EXECUTION_TIME, TimeUnit.MILLISECONDS);
            });

            ResizeHelper.addResizeListener(m_addStage, minWidth, minHeight, maxRect.getWidth(), maxRect.getHeight());

            maximizeBtn.setOnAction(maxEvent -> {
                boolean maximized = m_addStage.isMaximized();

                m_addStageMaximized = !maximized;

                if (!maximized) {
                    m_prevAddStageWidth = m_addStage.getWidth();
                    m_prevAddStageHeight = m_addStage.getHeight();
                }
                save();
                m_addStage.setMaximized(m_addStageMaximized);
            });

            Runnable doClose = () -> {
                m_addStage.close();
                m_addStage = null;
            };
            Runnable showNoneSelect = () -> {
                Alert a = new Alert(AlertType.NONE, "Select a node.", ButtonType.OK);
                a.setTitle("Select a node");
                a.initOwner(m_addStage);
                a.show();
            };
            nextBtn.setOnAction((e) -> {
                switch (nodeOption.get()) {
                    case CUSTOM:
                        add(new ErgoNodeData(this, clientTypeOption.get(), new NamedNodeUrl(friendlyId, nodeNameField.getText(), nodeUrlField.getText(), Integer.parseInt(nodePortField.getText()), apiKeyField.getText(), networkTypeOption.get())), true);
                        m_doGridUpdate.set(LocalDateTime.now());
                        doClose.run();
                        break;
                    default:
                        String nodeId = nodesList.selectedNamedNodeIdProperty().get();
                        if (nodeId != null) {
                            NamedNodeUrl namedNodeUrl = nodesList.getNamedNodeUrl(nodeId);

                            add(new ErgoNodeData(this, ErgoNodeData.LIGHT_CLIENT, namedNodeUrl), true);
                            m_doGridUpdate.set(LocalDateTime.now());
                            doClose.run();
                        } else {
                            showNoneSelect.run();
                        }
                        break;
                }
            });

            m_ergoNodes.shutdownNowProperty().addListener((obs, oldVal, newVal) -> {
                doClose.run();
            });

            m_addStage.setOnCloseRequest((e) -> doClose.run());

            closeBtn.setOnAction((e) -> doClose.run());
        } else {
            if (m_addStage.isIconified()) {
                m_addStage.setIconified(false);
            }
            m_addStage.show();
            m_addStage.toFront();
        }
    }

    public ErgoNodeLocalData getErgoLocalNode() {
        return m_ergoLocalNode;
    }

}
