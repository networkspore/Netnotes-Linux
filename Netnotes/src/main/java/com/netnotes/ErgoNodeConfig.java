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
import org.ergoplatform.appkit.NetworkType;

public class ErgoNodeConfig {

    private File logFile = new File("netnotes-log.txt");


    public static class LogLevel {
        public final static String ERROR = "ERROR";
        public final static String TRACE = "TRACE";
    }

    private String m_configFileName = "ergo.conf";
    private HashData m_configFileHashData = null;


    public final static String DEFAULT_LOG_LEVEL = LogLevel.ERROR;
    public final static String DEFAULT_ADDRESS = "0.0.0.0";
    public final static int DEFAULT_PORT = ErgoNodes.MAINNET_PORT;
    public final static NetworkType DEFAULT_NETWORK_TYPE =  NetworkType.MAINNET;

    private String m_logLevel = DEFAULT_LOG_LEVEL;
    private String m_address = DEFAULT_ADDRESS;
    private int m_port = DEFAULT_PORT;
   // private NetworkType m_networkType = DEFAULT_NETWORK_TYPE;
    private ErgoNodeLocalData m_ergoNodeLocalData;   
    private String m_configText;



    public ErgoNodeConfig(JsonObject jsonObject, ErgoNodeLocalData nodeLocalData) throws Exception {
        m_ergoNodeLocalData = nodeLocalData;
   
        openJson(jsonObject);

        addListeners();
    }

    public ErgoNodeConfig( String configText, String fileName, ErgoNodeLocalData nodeLocalData) throws FileNotFoundException, IOException, Exception {
        m_ergoNodeLocalData = nodeLocalData;
        m_configFileName = fileName;
        
        updateConfigFile();
        addListeners();
    }

    private void addListeners(){
        m_ergoNodeLocalData.namedNodeUrlProperty().addListener((obs,oldval,newval)->{
            try {
                updateConfigFile();
            } catch (Exception e) {
                try {
                    Files.writeString(logFile.toPath(), "\nCannot update config file", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
       
                }
            }
        });
    }


    public void openJson(JsonObject json) throws Exception {

        if (json != null) {
 
            JsonElement configTextElement = json.get("configText");
            JsonElement configFileNameElement = json.get("configFileName");
            JsonElement configFileHashDataElement = json.get("configFileHashData");

            if (configFileNameElement != null && configFileNameElement.isJsonPrimitive()) {

                String configFileName = configFileNameElement.getAsString();
                File configFile = new File(m_ergoNodeLocalData.getAppDir() + "/" + configFileName);
                HashData configHashData = new HashData(configFile);
                HashData configFileHashData = new HashData(configFileHashDataElement.getAsJsonObject());

                m_configText = configTextElement != null && configTextElement.isJsonPrimitive() ? configTextElement.getAsString() : null;
                m_configFileName = configFileName;
                m_configFileHashData = configFileHashData;
                    
                if (!configFileHashData.getHashStringHex().equals(configHashData.getHashStringHex())) {
                    Files.writeString(logFile.toPath(), "\nErgoNodeConfig: hash data mismatch", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }

            }

        }
   

    }

    public String getConfigString(){
  
        return m_configText == null ? getBasicConfig() : m_configText;
  
    }

    public String getConfigText() {
        return m_configText == null ? getBasicConfig() : m_configText;
    }

    public void setConfigText(String configText) {
        this.m_configText = configText;
        getLastUpdated().set(LocalDateTime.now());
    }


    public SimpleObjectProperty<LocalDateTime> getLastUpdated() {
        return m_ergoNodeLocalData.getLastUpdated();
    } 
    public static String getApiKeyHash(String apiKey){
        if(!apiKey.equals("")){
        
            final byte[] apiHashbytes = Utils.digestBytesToBytes(apiKey.getBytes());
            return Hex.encodeHexString(apiHashbytes);
        }else{
            return "";
        }
    }




    public HashData getConfigFileHashData() {
        return m_configFileHashData;
    }

    public File getAppDir(){
       return m_ergoNodeLocalData.getAppDir();
    }

    public JsonObject getJsonObject() {
        JsonObject json = new JsonObject();
        if (m_configText != null) {
            json.addProperty("configText", m_configText);
        }

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

    public void setConfigFileName(String name, boolean update) throws FileNotFoundException, IOException, Exception {
        m_configFileName = name;
        if(update){
            File configFile = getConfigFile();
            if(!configFile.getName().equals(name)){
                File newName = new File(configFile.getParentFile().getAbsolutePath() + "/" + name );
                configFile.renameTo(newName);
            }
            getLastUpdated().set(LocalDateTime.now());
        }    
    }

    public File getConfigFile() {
        if (getAppDir() != null && getAppDir().isDirectory() && getConfigFileName() != null) {
            return new File(getAppDir().getAbsolutePath() + "/" + getConfigFileName());

        } else {
            return null;
        }
    }

    public void updateConfigFile() throws FileNotFoundException, IOException, Exception {

     
        File configFile = getConfigFile();

        if (configFile == null) {
            throw new Exception("Config file not found.");
        }

        
        Files.writeString(configFile.toPath(), getConfigString());


        m_configFileHashData = new HashData(configFile);

    }

    public NamedNodeUrl getNamedNodeUrl(){
        return m_ergoNodeLocalData.getNamedNodeUrl();
    }
    public String getBasicConfig(){
        return getBasicConfig(m_logLevel, m_address, m_port, getNamedNodeUrl().getApiKey());
    }



    public static String getBasicConfig(String logLevel, String address, int port, String apiKey){
        String apiKeyHash = getApiKeyHash(apiKey);

        String configFileString = "ergo {";
        //   configFileString += "\n  directory = ${ergo.directory}\"/.ergo\"";
        configFileString += "\n  node {\n";
    //  configFileString += "\n    stateType = \"" + (m_stateMode.equals(DigestAccess.LOCAL) ? "digest" : "utxo") + "\"";
        configFileString += "\n    mining = false";

       /*switch (m_blockchainMode) {
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

        }*/
        configFileString += "\n  }";
        configFileString += "\n}";
        configFileString += "\n";
        configFileString += "\nscorex {\n";
        if(logLevel != null){
            configFileString += "\n  logging {";
            configFileString += "\n    level = \""+logLevel+"\"";
            configFileString += "\n  }";
        }

        boolean isAddress = !address.equals("0.0.0.0") || port !=  ErgoNodes.MAINNET_PORT;
        boolean isApiKey = apiKeyHash != null && !apiKeyHash.equals("");
        if(isAddress || isApiKey){
            configFileString += "\n  restApi {";
            if(isAddress){
                configFileString += "\n    bindAddress = \"" + address + ":" + port + "\"";
            }
            if (isApiKey) {
                configFileString += "\n    apiKeyHash = \"" + apiKeyHash + "\"";
            }
            configFileString += "\n  }";
        }
    
        configFileString += "\n}";

        return configFileString;
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
