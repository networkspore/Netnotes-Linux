package com.netnotes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

import javafx.beans.property.SimpleObjectProperty;

import com.google.gson.JsonObject;
import com.utils.Utils;
import com.google.gson.JsonElement;

import org.apache.commons.codec.binary.Hex;

public class ErgoNodeConfig {

    private File logFile = new File("netnotes-log.txt");

    public static class ConfigMode {

        public final static String ADVANCED = "Existing";
        public final static String BASIC = "New";
    }

    public static class DigestAccess {

        public final static String LOCAL = "Local Only";
        public final static String ALL = "All";
    }

    public static class BlockchainMode {

        public final static String PRUNED = "Pruned Node";
        public final static String RECENT_ONLY = "Recent-Only Node";
        public final static String FULL = "Full Node";
    }

    private File m_appDir;
    private String m_configFileName = "ergo.conf";
    private HashData m_configFileHashData = null;

    private String m_configMode = ConfigMode.BASIC;
    private String m_blockchainMode = BlockchainMode.FULL;
    private String m_stateMode = DigestAccess.ALL;
    private String m_apiKeyHash = "";

    public SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<LocalDateTime>(null);

    public double STAGE_MIN_WIDTH = 500;
    public double STAGE_MIN_HEIGHT = 850;

    /*private double m_StageWidth = STAGE_MIN_WIDTH;
    private double m_stageHeight = STAGE_MIN_HEIGHT;
    private double m_stagePrevWidth = STAGE_MIN_WIDTH;
    private double m_stagePrevHeight = STAGE_MIN_HEIGHT;
    private boolean m_stageMaximized = false;*/

    public ErgoNodeConfig(String apiKeyString, File appDir) throws FileNotFoundException, IOException, Exception {
        this(apiKeyString, ConfigMode.BASIC, DigestAccess.ALL, BlockchainMode.FULL, appDir);
    }

    public ErgoNodeConfig(String apiKeyString, String configMode, String digestAccess, String blockchainMode, String configFileName, File appDir) throws FileNotFoundException, IOException, Exception {
        m_configMode = configMode;
        m_stateMode = digestAccess;
        m_blockchainMode = blockchainMode;
        m_appDir = appDir;
        m_configFileName = configFileName;
        setApiKey(apiKeyString);

    }

    public ErgoNodeConfig(JsonObject jsonObject, File appDir) throws Exception {

        openJson(jsonObject, appDir);
    }

    public ErgoNodeConfig(String apiKeyString, String configMode, String digestAccess, String blockchainMode, File appDir) throws FileNotFoundException, IOException, Exception {
        m_configMode = configMode;
        m_stateMode = digestAccess;
        m_blockchainMode = blockchainMode;
        m_appDir = appDir;
        setApiKey(apiKeyString);

    }

    public ErgoNodeConfig(ErgoNodeConfig nodeConfig) {
        m_configMode = nodeConfig.getConfigMode();
        m_stateMode = nodeConfig.getStateMode();
        m_blockchainMode = nodeConfig.getBlockchainMode();
        m_appDir = nodeConfig.getAppDir();
        m_configFileName = nodeConfig.getConfigFileName();
        m_configFileHashData = nodeConfig.getConfigFileHashData();
        m_apiKeyHash = nodeConfig.getApiKeyHash();
    }

    public void setBasicConfig(JsonObject json) throws Exception {
        if (json != null) {
            JsonElement blockchainModeElement = json.get("blockchainMode");
            JsonElement digestModeElement = json.get("stateMode");

            if (blockchainModeElement != null && blockchainModeElement.isJsonPrimitive() && digestModeElement != null && digestModeElement.isJsonPrimitive()) {
                m_blockchainMode = blockchainModeElement.getAsString();
                m_stateMode = digestModeElement.getAsString();

            } else {
                throw new Exception("Config altered");
            }
        }

    }

    public void openJson(JsonObject json, File appDir) throws Exception {

        if (json != null && appDir != null && appDir.isDirectory()) {
            m_appDir = appDir;
            JsonElement configModeElement = json.get("configMode");
            JsonElement apiKeyHashElement = json.get("apiKeyHash");
            JsonElement configFileNameElement = json.get("configFileName");
            JsonElement configFileHashDataElement = json.get("configFileHashData");

            if (configFileNameElement != null && configFileNameElement.isJsonPrimitive()
                    && configFileHashDataElement != null && configFileHashDataElement.isJsonObject()
                    && apiKeyHashElement != null && apiKeyHashElement.isJsonPrimitive()
                    && configModeElement != null && configModeElement.isJsonPrimitive()) {

                String configFileName = configFileNameElement.getAsString();
                File configFile = new File(m_appDir.getAbsolutePath() + "/" + configFileName);
                HashData configHashData = new HashData(configFile);
                HashData configFileHashData = new HashData(configFileHashDataElement.getAsJsonObject());

                if (configFileHashData.getHashStringHex().equals(configHashData.getHashStringHex())) {

                    m_apiKeyHash = apiKeyHashElement.getAsString();
                    m_configMode = configModeElement.getAsString();
                    m_configFileName = configFileName;
                    m_configFileHashData = configFileHashData;

                    switch (m_configMode) {
                        case ConfigMode.BASIC:
                            JsonElement basicConfigElement = json.get("basicConfig");
                            if (basicConfigElement != null && basicConfigElement.isJsonObject()) {
                                setBasicConfig(basicConfigElement.getAsJsonObject());
                                return;
                            }
                            break;
                        case ConfigMode.ADVANCED:

                            return;

                    }

                } else {
                    Files.writeString(logFile.toPath(), "\nErgoNodeConfig: hash data mismatch", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }

            }

        }

        throw new Exception("Application config have been altered.");

    }

    public SimpleObjectProperty<LocalDateTime> getLastUpdated() {
        return m_lastUpdated;
    }

    public File getAppDir() {
        return m_appDir;
    }

    public void setAppDir(File appDir) {
        m_appDir = appDir;
    }

    public String getConfigMode() {
        return m_configMode;
    }

    public void setConfigMode(String configMode) {
        m_configMode = configMode;
        getLastUpdated().set(LocalDateTime.now());
    }

    public String getStateMode() {
        return m_stateMode;
    }

    public void seStateMode(String stateMode) {
        m_stateMode = stateMode;
        getLastUpdated().set(LocalDateTime.now());
    }

    public void blockchainMode(String blockchainMode) {
        m_blockchainMode = blockchainMode;
        getLastUpdated().set(LocalDateTime.now());
    }

    public String getBlockchainMode() {
        return m_blockchainMode;
    }

    public void setApiKey(String apiKeyString) throws FileNotFoundException, IOException, Exception {

        if (apiKeyString != null && apiKeyString != "") {
            final byte[] apiHashbytes = Utils.digestBytesToBytes(apiKeyString.getBytes());

            m_apiKeyHash = Hex.encodeHexString(apiHashbytes);
        } else {
            m_apiKeyHash = "";
        }

        updateConfigFile();
        getLastUpdated().set(LocalDateTime.now());
    }

    public String getApiKeyHash() {
        return m_apiKeyHash;
    }

    public HashData getConfigFileHashData() {
        return m_configFileHashData;
    }

    public JsonObject getBasicConfigJson() {
        JsonObject json = new JsonObject();
        json.addProperty("blockchainMode", m_blockchainMode);
        json.addProperty("stateMode", m_stateMode);
        return json;
    }

    public JsonObject getJsonObject() {
        JsonObject json = new JsonObject();
        if (m_configMode != null) {
            json.addProperty("configMode", m_configMode);
        }
        if (m_configMode.equals(ConfigMode.BASIC)) {
            json.add("basicConfig", getBasicConfigJson());
        }

        json.addProperty("apiKeyHash", m_apiKeyHash);

        if (m_configFileName != null) {
            json.addProperty("configFileName", m_configFileName);
        }
        if (m_configFileHashData != null) {
            json.add("configFileHashData", m_configFileHashData.getJsonObject());
        }
        return json;
    }

    public String getConfigFileName() {
        return m_configFileName;
    }

    public void setConfigFileName(String name) throws FileNotFoundException, IOException, Exception {
        m_configFileName = name;
        updateConfigFile();
        getLastUpdated().set(LocalDateTime.now());
    }

    public File getConfigFile() {
        if (m_appDir != null && m_appDir.exists() && m_appDir.isDirectory() && getConfigFileName() != null) {
            return new File(m_appDir.getAbsolutePath() + "/" + getConfigFileName());

        } else {
            return null;
        }
    }

    public void updateConfigFile() throws FileNotFoundException, IOException, Exception {

        if (m_configMode == null) {
            throw new Exception("Null config mode.");
        }
        File configFile = getConfigFile();

        if (configFile == null) {
            throw new Exception("Config file not found.");
        }

        if (m_configMode.equals(ConfigMode.BASIC)) {

            String configFileString = "ergo {";
            //   configFileString += "\n  directory = ${ergo.directory}\"/.ergo\"";
            configFileString += "\n  node {\n";
            configFileString += "\n    stateType = \"" + (m_stateMode.equals(DigestAccess.LOCAL) ? "digest" : "utxo") + "\"";
            configFileString += "\n    mining = false";

            switch (m_blockchainMode) {
                case BlockchainMode.RECENT_ONLY:
                    configFileString += "\n    blocksToKeep = 1440";
                case BlockchainMode.PRUNED:
                    configFileString += "\n    utxo {";
                    configFileString += "\n        utxoBootstrap = true";
                    configFileString += "\n        storingUtxoSnapshots = 0";
                    configFileString += "\n        p2pUtxoSnapshots = 2";
                    configFileString += "\n    }";
                    configFileString += "\n";
                    configFileString += "\n    nipopow {";
                    configFileString += "\n        nipopowBootstrap = true";
                    configFileString += "\n        p2pNipopows = 2";
                    configFileString += "\n    }\n";

                    break;

            }
            configFileString += "\n  }";
            configFileString += "\n}";
            configFileString += "\n";
            configFileString += "\nscorex {\n";
            configFileString += "\n  restApi {";
            configFileString += "\n    bindAddress = \"0.0.0.0:" + ErgoNodes.MAINNET_PORT + "\"";
            if (m_apiKeyHash != null && !m_apiKeyHash.equals("")) {
                configFileString += "\n    apiKeyHash = \"" + m_apiKeyHash + "\"";
            }
            configFileString += "\n  }";
            configFileString += "\n}";

            Files.writeString(configFile.toPath(), configFileString);
        }

        if (!configFile.isFile()) {
            throw new Exception("Config file not found.");
        }

        m_configFileHashData = new HashData(configFile);

    }


    /*
      public Scene getConfigScene(SimpleObjectProperty<NamedNodeUrl> namedNode, Button okBtn, Stage stage) {
        final String headingString = "Config";
        final String nodeId = namedNode.get().getId();

        SimpleStringProperty apiKeySting = new SimpleStringProperty(namedNode.get().getApiKey());

        stage.titleProperty().bind(Bindings.concat(headingString, " - ", namedNode.asString(), " - ", ErgoNodes.NAME));

        Image icon = ErgoNodes.getSmallAppIcon();
        double defaultRowHeight = 40;
        Button closeBtn = new Button();
        Button maximizeBtn = new Button();

        okBtn.setText("Ok");
        okBtn.setPadding(new Insets(5, 15, 5, 15));

        HBox titleBox = App.createTopBar(icon, maximizeBtn, closeBtn, stage);
        Text headingText = new Text(headingString);
        headingText.setFont(App.txtFont);
        headingText.setFill(Color.WHITE);

        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(defaultRowHeight);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 10, 10, 10));
        headingBox.setId("headingBox");

        HBox headingPaddingBox = new HBox(headingBox);

        headingPaddingBox.setPadding(new Insets(5, 0, 2, 0));

        VBox headerBox = new VBox(titleBox, headingPaddingBox);

        headerBox.setPadding(new Insets(0, 5, 0, 5));

        SimpleDoubleProperty rowHeight = new SimpleDoubleProperty(defaultRowHeight);

        Text configText = new Text(headingString);
        configText.setFill(App.txtColor);
        configText.setFont(App.txtFont);

        HBox configBox = new HBox(configText);
        configBox.setAlignment(Pos.CENTER_LEFT);
        configBox.setMinHeight(40);;
        configBox.setId("headingBox");
        configBox.setPadding(new Insets(0, 0, 0, 15));

        HBox okBox = new HBox(okBtn);
        okBox.setMinHeight(35);
        okBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(okBox, Priority.ALWAYS);

        Text apiKeyText = new Text(String.format("%-13s", "API Key"));
        apiKeyText.setFill(App.txtColor);
        apiKeyText.setFont(App.txtFont);

        Button apiKeyBtn = new Button(apiKeySting.get() != null && apiKeySting.get() != "" ? "(Click to update)" : "(Click to set)");
        apiKeyBtn.setId("rowBtn");
        apiKeyBtn.setOnAction(e -> {

        });

        HBox apiKeyBox = new HBox(apiKeyText, apiKeyBtn);
        apiKeyBox.setAlignment(Pos.CENTER_LEFT);
        apiKeyBox.setPadding(new Insets(0, 0, 0, 15));
        HBox.setHgrow(apiKeyBox, Priority.ALWAYS);

        apiKeyBtn.prefWidthProperty().addListener((obs, oldval, newVal) -> {

        });

        VBox configPaddingBox = new VBox(apiKeyBox);

        VBox advPaddingBox = new VBox();

        VBox bodyBox = new VBox(configPaddingBox, advPaddingBox, okBox);
        bodyBox.setId("bodyBox");
        bodyBox.setPadding(new Insets(15));

        VBox bodyPaddingBox = new VBox(bodyBox);
        bodyPaddingBox.setPadding(new Insets(5, 5, 5, 5));

        Region footerSpacer = new Region();
        footerSpacer.setMinHeight(5);

        VBox footerBox = new VBox(footerSpacer);

        VBox layoutBox = new VBox(headerBox, bodyPaddingBox, footerBox);
        Scene configScene = new Scene(layoutBox, m_configStageWidth, m_configStageHeight);
        configScene.getStylesheets().add("/css/startWindow.css");

        Runnable closeStage = () -> {
            stage.close();
            m_configStage = null;
        };

        closeBtn.setOnAction(e -> closeStage.run());
        stage.setOnCloseRequest(e -> closeStage.run());

        Runnable updateNamedNode = () -> {

            //String id, String name, String ip, int port, String apiKey, NetworkType networkType
            namedNode.set(new NamedNodeUrl(nodeId, nameField.getText(), "127.0.0.1", Integer.parseInt(portField.getText()), apiKeySting.get(), NetworkType.MAINNET));
        };

        apiKeySting.addListener((obs, oldVal, newval) -> {
            updateNamedNode.run();
        });

        namedNode.addListener((obs, oldVal, newVal) -> {
            apiKeyBtn.setText(namedNode.get().getApiKey() != null && namedNode.get().getApiKey() != "" ? "(Click to update)" : "(Click to set)");
        });

        return configScene;
    }
     */
}
