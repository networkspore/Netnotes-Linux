package com.netnotes;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.BadPaddingException;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.imageio.ImageIO;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import com.satergo.extra.AESEncryption;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ove.crypto.digest.Blake2b;

public class ErgoTokensList  {

    public final static String NETWORK_ID = "ERGO_TOKENS_LIST";

    private File logFile = new File("netnotes-log.txt");



    //private final ArrayList<ErgoTokenData> m_networkTokenList = new ArrayList<>();
   // private VBox m_buttonGrid = null;
    private double m_sceneWidth = 600;
    private double  m_sceneHeight = 630;
    private NetworkType m_networkType;
    private ErgoTokens m_ergoTokens = null;
    private final SimpleObjectProperty<ErgoTokenData> m_selectedNetworkToken = new SimpleObjectProperty<>(null);

    private final SimpleObjectProperty<ErgoExplorerList> m_ergoExplorersList = new SimpleObjectProperty<>(null);

    private ArrayList<ErgoTokenData> m_dataList = new ArrayList<>();
    private ErgoToken m_ergoToken = null; 

    private File m_tokensDir;
    public ErgoTokensList(NetworkType networkType, ErgoTokens ergoTokens) {
       
        m_networkType = networkType;
        m_ergoTokens = ergoTokens;
        setup();
    }

    private void setup(){
        m_tokensDir = new File(m_ergoTokens.getErgoNetworkData().getErgoNetwork().getAppDir().getAbsolutePath() + "/tokens");
        if(!m_tokensDir.isDirectory()){
            try {
                Files.createDirectories(m_tokensDir.toPath());
            
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }
        }

        JsonObject json = m_ergoTokens.getNetworksData().getData("data", NetworkType.MAINNET.toString(), ErgoExplorers.NETWORK_ID, ErgoNetwork.NETWORK_ID);      
      
        if (!m_tokensDir.isDirectory() || json == null) {
            
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("assets/ergoTokenIcons.zip");

            ZipInputStream zipStream = null;
         
            try {

                zipStream = new ZipInputStream(is);
                final Blake2b digest = Blake2b.Digest.newInstance(32);
            
                String tokensPathString = m_tokensDir.getAbsolutePath();

                if (zipStream != null) {
                    // Enumeration<? extends ZipEntry> entries = zipFile.entries();
                  
                    ZipEntry entry;
                    while ((entry = zipStream.getNextEntry()) != null) {

                        String entryName = entry.getName();

                        int indexOfDir = entryName.lastIndexOf("/");

                        if (indexOfDir != entryName.length() - 1) {

                            int indexOfExt = entryName.lastIndexOf(".");

                            String fileName = entryName.substring(0, indexOfExt);

                            File newDirFile = new File(tokensPathString + "/" + fileName);
                            if (!newDirFile.isDirectory()) {
                                Files.createDirectory(newDirFile.toPath());
                            
                                String fileString = tokensPathString + "/" + fileName + "/" + entryName;
                                File entryFile = new File(fileString);
                                OutputStream outStream = null;
                                try {
                                    
                                
                                    outStream = new FileOutputStream(entryFile);
                                    //outStream.write(buffer);

                                    byte[] buffer = new byte[8 * 1024];
                                    int bytesRead;
                                    while ((bytesRead = zipStream.read(buffer)) != -1) {

                                        outStream.write(buffer, 0, bytesRead);
                                        digest.update(buffer, 0, bytesRead);
                                    }
                                    byte[] hashbytes = digest.digest();

                                    HashData hashData = new HashData(hashbytes);
                            
                                    
                            //      ErgoTokenData token = createToken();
                                
                                //   if (token != null) {
                                       
                                        addToken(fileName, fileString, hashData);
                                // }

                                } catch (IOException ex) {
                                    try {
                                        Files.writeString(App.logFile.toPath(), "\nErgoTokens:" + ex.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                    } catch (IOException e1) {

                                    }
                                } finally {
                                    if (outStream != null) {
                                        outStream.close();
                                    }
                                }
                            }
                        }

                    }
                }
            } catch (IOException e) {
                try {
                    Files.writeString(App.logFile.toPath(), "\nErgoTokens:" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
            } finally {
                if (zipStream != null) {

                    try {
                        zipStream.close();
                    } catch (IOException e2) {
                        try {
                            Files.writeString(App.logFile.toPath(), "\nErgoTokens: " + e2.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e1) {

                        }
                    }

                }
            }


            save();
        }
    }
  
    
    /*public ErgoTokensList(ArrayList<ErgoTokenData> networkTokenList, NetworkType networkType, ErgoTokens ergoTokens) {
      
        m_networkType = networkType;
        m_ergoTokens = ergoTokens;
        
        for (ErgoTokenData networkToken : networkTokenList) {

            addToken(networkToken, false);

        }
    }*/





    public SimpleObjectProperty<ErgoExplorerList> ergoExplorerListProperty(){
        return m_ergoExplorersList;
    }
  

 //   private SimpleObjectProperty<LocalDateTime> m_marketUpdated = new SimpleObjectProperty<>(LocalDateTime.now());

    /*public SimpleObjectProperty<LocalDateTime> marketUpdated(){
        return m_marketUpdated;
    }*/

 
    public SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>();
    public SimpleObjectProperty<LocalDateTime> getLastUpdated(){
        return m_lastUpdated;
    }
 
    public void shutdown(){

     

    }


    /*
    public PriceQuote findPriceQuote(String baseSymbol, String quoteSymbol){
        ErgoMarketsData marketsData = m_selectedMarketData.get();
        if(marketsData != null){
            
        }
        /*PriceQuote[] quotes = priceQuotesProperty().get();
        if(quotes != null && baseSymbol != null && quoteSymbol != null){
            for(int i = 0; i < quotes.length ; i++){
                
                PriceQuote quote = quotes[i];
                if(quote.getTransactionCurrency().equals(baseSymbol) && quote.getQuoteCurrency().equals(quoteSymbol)){
                    return quote;
                }
            }
        }
        return null;
    }*/


    public SimpleObjectProperty<ErgoTokenData> selectedTokenProperty(){
        return m_selectedNetworkToken;
    }

   
 
    /* public SimpleObjectProperty<ErgoTokenData> getTokenProperty() {
        return m_ergoNetworkToken;
    }*/


    public ErgoTokens getErgoTokens(){
        return m_ergoTokens;
    }
   
  

    /*public void setNetworkType(SecretKey secretKey, NetworkType networkType) {

        
        m_networkTokenList.clear();
        m_networkType = networkType;
        setName("Ergo Tokens - List (" + networkType.toString() + ")");
        openFile(secretKey);
     

    }*/

    /*public void openTestnetFile(Path filePath) {
        m_networkTokenList.clear();

        if (filePath != null) {
            try {
                JsonElement jsonElement = new JsonParser().parse(Files.readString(filePath));

                if (jsonElement != null && jsonElement.isJsonObject()) {
                    openJson(jsonElement.getAsJsonObject(), NetworkType.TESTNET);
                }
            } catch (JsonParseException | IOException e) {
                try {
                    Files.writeString(logFile.toPath(), "\nInvalid testnet file: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
            }
        }
    }*/

    private ChangeListener<LocalDateTime> m_lastUpdatedlistener = null;
    private VBox m_buttonGrid = new VBox();

    public VBox getButtonGrid() {
        m_buttonGrid = new VBox();
        
        

        Runnable updateGrid = ()->{
            
            
            int numCells = m_dataList.size();

            m_buttonGrid.getChildren().clear();
        
               
            for (int i = 0; i < numCells; i++) {
               
                ErgoTokenData networkToken = m_dataList.get(i);
                
                IconButton rowButton = networkToken.getButton(IconButton.IconStyle.ROW);
                HBox rowBox = new HBox(rowButton);
                rowBox.setPadding(new Insets(1, 0,1,0));
                m_buttonGrid.getChildren().add(rowBox);
                rowButton.prefWidthProperty().bind(m_buttonGrid.widthProperty());
                
            }
            
        };

        updateGrid.run();
        m_lastUpdatedlistener = (obs,oldval,newval)->updateGrid.run();
     
        getLastUpdated().addListener(m_lastUpdatedlistener);

    

        return m_buttonGrid;
    }

    

    public void getErgoToken(String tokenid, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        
        Utils.returnObject(getErgoToken(tokenid), m_ergoTokens.getNetworksData().getExecService(), onSucceeded, onFailed);
        
    }

    public ErgoTokenData getErgoToken(String tokenId){
        if(tokenId != null){
            if(tokenId.equals(ErgoCurrency.TOKEN_ID)){
               
                m_ergoToken = m_ergoToken == null || (m_ergoToken != null) ? new ErgoToken(this) : m_ergoToken;
                   
                return m_ergoToken;
            }else{
           
                for (int i = 0; i < m_dataList.size(); i++) {
                    ErgoTokenData networkToken = m_dataList.get(i);
                    
                    if (networkToken.getTokenId().equals(tokenId)) {
                        return networkToken;
                    }
                    
                }
            }
        }
        return null;
    }

    public ErgoTokenData getAddErgoToken(String tokenId, String name, int decimals){
        ErgoTokenData token = getErgoToken(tokenId);

        if(token == null){
            ErgoTokenData newToken = new ErgoTokenData(tokenId, name, decimals, m_networkType, this);
            addToken(newToken, true);
            return newToken;
        }
        return token;
    }



    public ErgoTokenData getTokenByName(String name) {
        for (int i = 0; i < m_dataList.size(); i++) {
            ErgoTokenData networkToken = m_dataList.get(i);
            
            if (networkToken.getName().equals(name)) {
                return networkToken;
            }
            
        }
        return null;
    }

    public JsonObject getTokensStageObject() {
        JsonObject tokenStageObject = new JsonObject();
        tokenStageObject.addProperty("subject", "GET_ERGO_TOKENS_STAGE");
        return tokenStageObject;
    }

    public void addToken(ErgoTokenData networkToken) {
        addToken(networkToken, true);
    }

    public NetworkType getNetworkType(){
        return m_networkType;
    }

    public void addToken(ErgoTokenData networkToken, boolean update) {

        if (networkToken != null) {
            if (getErgoToken(networkToken.getNetworkId()) == null) {
                m_dataList.add(networkToken);
               
            }
            if (update) {
                save();
            }
        }

    }

    public boolean removeToken(String networkId, boolean update) {
        if (networkId != null) {
            ErgoTokenData networkToken = getErgoToken(networkId);
            if (networkToken != null) {

                boolean removed = m_dataList.remove(networkToken);
                if (update) {
                
                    save();
                    
                }
                return removed;
            }
        }
        return false;
    }

    public void removeToken(String networkId) {
        removeToken(networkId, true);
    }



    public void importJson(Stage callingStage, File file) {
        SimpleBooleanProperty updated = new SimpleBooleanProperty(false);
        if (file != null && file.isFile()) {
            String contentType = null;
            try {
                contentType = Files.probeContentType(file.toPath());

            } catch (IOException e) {

            }

            if (contentType != null && (contentType.equals("application/json") || contentType.substring(0, 4).equals("text"))) {

                JsonObject fileJson = null;
                try {
                    String fileString = Files.readString(file.toPath());
                    JsonElement jsonElement = new JsonParser().parse(fileString);
                    if (jsonElement != null && jsonElement.isJsonObject()) {
                        fileJson = jsonElement.getAsJsonObject();
                    }
                } catch (JsonParseException | IOException e) {
                    Alert noFile = new Alert(AlertType.NONE, e.toString(), ButtonType.OK);
                    noFile.setHeaderText("Load Error");
                    noFile.initOwner(callingStage);
                    noFile.setTitle("Import JSON - Load Error");
                    noFile.setGraphic(IconButton.getIconView(new Image("/assets/load-30.png"), App.MENU_BAR_IMAGE_WIDTH));
                    noFile.show();
                }
                if (fileJson != null) {
                    JsonElement networkTypeElement = fileJson.get("networkType");
                    JsonElement dataElement = fileJson.get("data");
                    NetworkType networkType = null;

                    if (networkTypeElement != null && networkTypeElement.isJsonPrimitive()) {
                        String networkTypeString = networkTypeElement.getAsString();
                        networkType = networkTypeString.equals(NetworkType.MAINNET.toString()) ? NetworkType.MAINNET : networkTypeString.equals(NetworkType.TESTNET.toString()) ? NetworkType.TESTNET : null;
                        if (networkType != m_networkType) {
                            Alert a = new Alert(AlertType.NONE, "JSON network type is: " + networkType.toString() + ". Import into " + m_networkType.toString() + " canceled.", ButtonType.OK);
                            a.initOwner(callingStage);
                            a.setHeaderText("Network Type Mismatch");
                            a.setGraphic(IconButton.getIconView(m_ergoTokens.getAppIcon(), 40));
                            a.showAndWait();
                            return;
                        }
                    }

                    if (networkType == null) {
                       
                    } else {
                       
                    }

                    if (dataElement != null && dataElement.isJsonArray()) {
                        JsonArray dataArray = dataElement.getAsJsonArray();

                        for (JsonElement tokenObjectElement : dataArray) {
                            if (tokenObjectElement.isJsonObject()) {
                                JsonObject tokenJson = tokenObjectElement.getAsJsonObject();

                                JsonElement nameElement = tokenJson.get("name");
                                JsonElement tokenIdElement = tokenJson.get("tokenId");

                                if (nameElement != null && tokenIdElement != null) {
                                    String tokenId = tokenIdElement.getAsString();
                                    String name = nameElement.getAsString();
                                    ErgoTokenData oldToken = getErgoToken(tokenId);
                                    if (oldToken == null) {
                                        ErgoTokenData nameToken = getTokenByName(name);
                                        if (nameToken == null) {
                                            updated.set(true);
                                            addToken(new ErgoTokenData(name, tokenId, networkType, tokenJson, this), false);
                                        } else {
                                            Alert nameAlert = new Alert(AlertType.NONE, "Token:\n\n'" + tokenJson.toString() + "'\n\nName is used by another tokenId. Token will not be loaded.", ButtonType.OK);
                                            nameAlert.setHeaderText("Token Conflict");
                                            nameAlert.setTitle("Import JSON - Token Conflict");
                                            nameAlert.initOwner(callingStage);
                                            nameAlert.showAndWait();
                                        }
                                    } else {
                                        ErgoTokenData newToken = new ErgoTokenData(name, tokenId, networkType, tokenJson, this);

                                        Alert nameAlert = new Alert(AlertType.NONE, "Existing Token:\n\n'" + oldToken.getName() + "' exists, overwrite token with '" + newToken.getName() + "'?", ButtonType.YES, ButtonType.NO);
                                        nameAlert.setHeaderText("Resolve Conflict");
                                        nameAlert.initOwner(callingStage);
                                        nameAlert.setTitle("Import JSON - Resolve Conflict");
                                        Optional<ButtonType> result = nameAlert.showAndWait();

                                        if (result.isPresent() && result.get() == ButtonType.YES) {
                                            removeToken(oldToken.getNetworkId(), false);
                                           
                                            addToken(newToken, true);
                                            updated.set(true);
                                        }
                                    }
                                } else {
                                    Alert noFile = new Alert(AlertType.NONE, "Token:\n" + tokenJson.toString() + "\n\nMissing name and/or tokenId properties and cannote be loaded.\n\nContinue?", ButtonType.YES, ButtonType.NO);
                                    noFile.setHeaderText("Encoding Error");
                                    noFile.initOwner(callingStage);
                                    noFile.setTitle("Import JSON - Encoding Error");
                                    Optional<ButtonType> result = noFile.showAndWait();

                                    if (result.isPresent() && result.get() == ButtonType.NO) {
                                        break;
                                    }
                                }
                            }
                        }

                    }
                }
            } else {
                Alert tAlert = new Alert(AlertType.NONE, "File content type: " + contentType + " not supported.\n\nContent type: text/plain or application/json required.", ButtonType.OK);
                tAlert.setHeaderText("Content Type Mismatch");
                tAlert.initOwner(callingStage);
                tAlert.setTitle("Import JSON - Content Type Mismatch");
                tAlert.setGraphic(IconButton.getIconView(new Image("/assets/load-30.png"), App.MENU_BAR_IMAGE_WIDTH));
                tAlert.show();

            }
        }
        if(updated.get()){
            save();
            
        }

    }

    private void openJson(JsonObject json, NetworkType networkType) {
   
        JsonElement networkTypeElement = json.get("networkType");

        JsonElement dataElement = json.get("data");

        if (dataElement != null && dataElement.isJsonArray() && networkTypeElement != null) {

            JsonArray dataArray = dataElement.getAsJsonArray();

            //  if (m_ergoTokens.getNetworkType().toString().equals(networkType)) {
            for (JsonElement objElement : dataArray) {
                if (objElement.isJsonObject()) {
                    JsonObject objJson = objElement.getAsJsonObject();
                    JsonElement nameElement = objJson.get("name");
                    JsonElement tokenIdElement = objJson.get("tokenId");

                    if (nameElement != null && nameElement.isJsonPrimitive() && tokenIdElement != null && tokenIdElement.isJsonPrimitive()) {
                   
                        addToken(new ErgoTokenData(nameElement.getAsString(), tokenIdElement.getAsString(), networkType, objJson, this), false);
                    }

                }
            }
            //   }
        }
    }

    public Scene getEditTokenScene(ErgoTokenData token, NetworkType networkType, Stage parentStage, Button closeBtn) {
        String title = "Ergo Tokens - Editor " + (networkType == NetworkType.MAINNET ? "(MAINNET)" : "(TESTNET)");

        SimpleDoubleProperty rowHeight = new SimpleDoubleProperty(40);
       
      
        parentStage.setTitle(title);
        //String type = "Existing token";
        boolean tokenNull = token == null;
        final String tokenImgString = token != null ? token.getImageString() : null;
        final HashData tokenHashData = token != null ? token.getImgHashData() : null;
        
        token = token == null ? new ErgoTokenData("", "", "", "", null, networkType, this) : token;
        
        final Image defaultImage = token.getIcon();
       
        HBox titleBox = App.createTopBar( defaultImage, title, closeBtn, parentStage);
       
        HBox.setHgrow(titleBox, Priority.ALWAYS);

       
        SimpleObjectProperty<File> selectedImageFile = new SimpleObjectProperty<File>(null);

        ImageView imageView = IconButton.getIconView(token == null ? m_ergoTokens.getAppIcon() : defaultImage, 135);

        Button imageBtn = new Button(token.getName().equals("") ? "New Token" : token.getName());
        imageBtn.setContentDisplay(ContentDisplay.TOP);
        imageBtn.setGraphicTextGap(20);
        imageBtn.setFont(App.mainFont);
        imageBtn.prefHeight(135);
        imageBtn.prefWidth(135);
        imageBtn.setId("menuBtn");
        imageBtn.setGraphic(IconButton.getIconView(defaultImage, 135));

        imageBtn.setGraphic(imageView);

    

        Tooltip explorerTip = new Tooltip();
        explorerTip.setShowDelay(new javafx.util.Duration(100));
        explorerTip.setFont(App.txtFont);

        MenuButton explorerBtn = new MenuButton();

        explorerBtn.setPadding(new Insets(2, 0, 0, 0));
        explorerBtn.setTooltip(explorerTip);

        HBox rightSideMenu = new HBox(explorerBtn);
        rightSideMenu.setId("rightSideMenuBar");
        rightSideMenu.setPadding(new Insets(0, 10, 0, 20));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);



 
        HBox menuBar = new HBox( spacer, rightSideMenu);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(1, 0, 1, 5));

        HBox imageBox = new HBox(imageBtn);
        HBox.setHgrow(imageBox, Priority.ALWAYS);
        imageBox.setAlignment(Pos.CENTER);
        imageBox.setPadding(new Insets(20, 0, 10, 0));

        Text promptText = new Text("Token");
        promptText.setFont(App.txtFont);
        promptText.setFill(Color.WHITE);

        HBox promptBox = new HBox(promptText);
        promptBox.prefHeight(40);
        promptBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(promptBox, Priority.ALWAYS);
        promptBox.setPadding(new Insets(10, 15, 10, 15));
        promptBox.setId("headingBox");

        Button nameSpacerBtn = new Button();
        nameSpacerBtn.setDisable(true);
        nameSpacerBtn.setId("transparentColor");
        nameSpacerBtn.setPrefWidth(5);
        nameSpacerBtn.setPrefHeight(60);;

        Button tokenIdSpacerBtn = new Button();
        tokenIdSpacerBtn.setDisable(true);
        tokenIdSpacerBtn.setId("transparentColor");
        tokenIdSpacerBtn.setPrefHeight(60);;
        tokenIdSpacerBtn.setPrefWidth(5);

        Button urlSpacerBtn = new Button();
        urlSpacerBtn.setDisable(true);
        urlSpacerBtn.setId("transparentColor");
        urlSpacerBtn.setPrefWidth(5);
        urlSpacerBtn.setPrefHeight(60);;

        Button imgFileSpacerBtn = new Button();
        imgFileSpacerBtn.setDisable(true);
        imgFileSpacerBtn.setId("transparentColor");
        imgFileSpacerBtn.setPrefWidth(5);
        imgFileSpacerBtn.setPrefHeight(60);;

        TextField tokenIdField = new TextField(token.getTokenId());
        tokenIdField.setPadding(new Insets(9, 0, 10, 0));
        tokenIdField.setFont(App.txtFont);
        tokenIdField.setId("formField");
        HBox.setHgrow(tokenIdField, Priority.ALWAYS);

        Button tokenIdBtn = new Button(tokenIdField.getText().equals("") ? "Enter Token Id" : tokenIdField.getText());
        tokenIdBtn.setId("rowBtn");
        tokenIdBtn.setFont(App.txtFont);
        tokenIdBtn.setContentDisplay(ContentDisplay.LEFT);
        tokenIdBtn.setAlignment(Pos.CENTER_LEFT);
        tokenIdBtn.setPadding(new Insets(10, 10, 10, 10));

        TextField nameField = new TextField(token.getName());
        nameField.setPadding(new Insets(9, 0, 10, 0));
        nameField.setFont(App.txtFont);
        nameField.setId("formField");
   
        HBox.setHgrow(nameField, Priority.ALWAYS);


        Text nameCaret = new Text("Name       ");
        nameCaret.setFont(App.txtFont);
        nameCaret.setFill(Color.WHITE);

        Button nameButton = new Button(nameField.getText().equals("") ? "Enter name" : nameField.getText());

        nameButton.setId("rowBtn");
        nameButton.setContentDisplay(ContentDisplay.LEFT);
        nameButton.setAlignment(Pos.CENTER_LEFT);
        nameButton.setPadding(new Insets(10, 10, 10, 10));

        nameButton.setFont(App.txtFont);

        HBox nameBox = new HBox(nameCaret, nameSpacerBtn, nameButton);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        nameBox.setPadding(new Insets(5, 0, 5, 0));
     

        Button nameEnterBtn = new Button("[ Enter ]");
        nameEnterBtn.setFont(App.txtFont);
        nameEnterBtn.setId("toolBtn");
        nameEnterBtn.setPadding(new Insets(5, 5, 5, 5));
        nameEnterBtn.prefWidth(75);

        nameButton.setOnAction(event -> {
            if(nameBox.getChildren().contains(nameButton)){
                nameBox.getChildren().remove(nameButton);
            }
            if(!nameBox.getChildren().contains(nameField)){
                nameBox.getChildren().add(nameField);
            }
            Platform.runLater(() -> nameField.requestFocus());

        });

        nameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {

            } else {
                if(nameBox.getChildren().contains(nameField)){
                    nameBox.getChildren().remove(nameField);
                }
                if(!nameBox.getChildren().contains(nameButton)){
                    nameBox.getChildren().add(nameButton);
                }
                nameSpacerBtn.setId("transparentColor");
                String text = nameField.getText();

                nameButton.setText(text.equals("") ? "Enter name" : text);
                imageBtn.setText(nameField.getText().equals("") ? "New Token" : nameField.getText());
                if (nameBox.getChildren().contains(nameEnterBtn)) {
                    nameBox.getChildren().remove(nameEnterBtn);
                }
            }
        });

        nameField.setOnKeyPressed(keyEvent -> {
            KeyCode keyCode = keyEvent.getCode();

            if (keyCode == KeyCode.ENTER) {
                nameBox.getChildren().remove(nameField);
                nameBox.getChildren().add(nameButton);
                nameSpacerBtn.setId("transparentColor");
                String text = nameField.getText();

                nameButton.setText(text.equals("") ? "Enter name" : text);
                imageBtn.setText(nameField.getText().equals("") ? "New Token" : nameField.getText());
                if (nameBox.getChildren().contains(nameEnterBtn)) {
                    nameBox.getChildren().remove(nameEnterBtn);
                }
            }
        });

        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!nameBox.getChildren().contains(nameEnterBtn)) {
                nameBox.getChildren().add(nameEnterBtn);
            }
        });

        nameEnterBtn.setOnAction(action -> {
            //nameBox.getChildren().remove(nameField);
           // nameBox.getChildren().add(nameButton);
            nameSpacerBtn.setId("transparentColor");
            String text = nameField.getText();

            nameButton.setText(text.equals("") ? "Enter name" : text);
            imageBtn.setText(nameField.getText().equals("") ? "New Token" : nameField.getText());
            if (nameBox.getChildren().contains(nameEnterBtn)) {
                nameBox.getChildren().remove(nameEnterBtn);
            }
        });

        HBox.setHgrow(tokenIdField, Priority.ALWAYS);

        Text tokenIdCaret = new Text("Token Id   ");
        tokenIdCaret.setFont(App.txtFont);
        tokenIdCaret.setFill(Color.WHITE);

        Button tokenIdEnterBtn = new Button("[ Enter ]");
        tokenIdEnterBtn.setFont(App.txtFont);
        tokenIdEnterBtn.setId("toolBtn");
        tokenIdEnterBtn.setPadding(new Insets(5, 5, 5, 5));
        tokenIdEnterBtn.prefWidth(75);

        HBox tokenIdBox = new HBox(tokenIdCaret, tokenIdSpacerBtn, tokenIdBtn);
        HBox.setHgrow(tokenIdBox, Priority.ALWAYS);
        tokenIdBox.setAlignment(Pos.CENTER_LEFT);
        tokenIdBox.setPadding(new Insets(5, 0, 5, 0));
        
        // tokenIdBox.setPrefHeight(60);;
        tokenIdBtn.setOnAction(action -> {
            tokenIdBox.getChildren().remove(tokenIdBtn);
            tokenIdBox.getChildren().add(tokenIdField);
            Platform.runLater(() -> tokenIdField.requestFocus());

        });

        tokenIdField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {

            } else {
                String tokenIdFieldText = tokenIdField.getText();
                tokenIdSpacerBtn.setId("transparentColor");
                if (tokenIdFieldText.equals("")) {
                    tokenIdBtn.setText("Enter token id");

                } else {

                    tokenIdBtn.setText(tokenIdFieldText);
                }
                if (tokenIdBox.getChildren().contains(tokenIdEnterBtn)) {
                    tokenIdBox.getChildren().remove(tokenIdEnterBtn);
                }
                tokenIdBox.getChildren().remove(tokenIdField);
                tokenIdBox.getChildren().add(tokenIdBtn);
            }
        });

        tokenIdEnterBtn.setOnAction(action -> {
            String tokenIdFieldText = tokenIdField.getText();
            tokenIdSpacerBtn.setId("transparentColor");
            if (tokenIdFieldText.equals("")) {
                tokenIdBtn.setText("Enter token id");

            } else {

                tokenIdBtn.setText(tokenIdFieldText);
            }
            if (tokenIdBox.getChildren().contains(tokenIdEnterBtn)) {
                tokenIdBox.getChildren().remove(tokenIdEnterBtn);
            }
            tokenIdBox.getChildren().remove(tokenIdField);
            tokenIdBox.getChildren().add(tokenIdBtn);
        });

        tokenIdField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!tokenIdBox.getChildren().contains(tokenIdEnterBtn)) {
                tokenIdBox.getChildren().add(tokenIdEnterBtn);
            }
        });

        tokenIdField.setOnKeyPressed(keyEvent -> {
            KeyCode keyCode = keyEvent.getCode();
            if (keyCode == KeyCode.ENTER) {
                String tokenIdFieldText = tokenIdField.getText();
                tokenIdSpacerBtn.setId("transparentColor");
                if (tokenIdFieldText.equals("")) {
                    tokenIdBtn.setText("Enter token id");

                } else {

                    tokenIdBtn.setText(tokenIdFieldText);
                }
                if (tokenIdBox.getChildren().contains(tokenIdEnterBtn)) {
                    tokenIdBox.getChildren().remove(tokenIdEnterBtn);
                }
                tokenIdBox.getChildren().remove(tokenIdField);
                tokenIdBox.getChildren().add(tokenIdBtn);
            }
        });

        TextField urlLinkField = new TextField(token.getUrlString());
        urlLinkField.setPadding(new Insets(9, 0, 10, 0));
        urlLinkField.setFont(App.txtFont);
        urlLinkField.setId("formField");
        HBox.setHgrow(urlLinkField, Priority.ALWAYS);

        Text urlLinkCaret = new Text("URL        ");
        urlLinkCaret.setFont(App.txtFont);
        urlLinkCaret.setFill(Color.WHITE);

        Button urlLinkBtn = new Button(urlLinkField.getText().equals("") ? "Enter URL" : urlLinkField.getText());
        urlLinkBtn.setId("rowBtn");
        urlLinkBtn.setFont(App.txtFont);
        urlLinkBtn.setContentDisplay(ContentDisplay.LEFT);
        urlLinkBtn.setAlignment(Pos.CENTER_LEFT);
        urlLinkBtn.setPadding(new Insets(10, 10, 10, 10));

        Button urlEnterBtn = new Button("[ Enter ]");
        urlEnterBtn.setFont(App.txtFont);
        urlEnterBtn.setId("toolBtn");
        urlEnterBtn.setPadding(new Insets(5, 5, 5, 5));
        urlEnterBtn.prefWidth(75);

        HBox urlBox = new HBox(urlLinkCaret, urlSpacerBtn, urlLinkBtn);
          
        HBox.setHgrow(urlBox, Priority.ALWAYS);
        urlBox.setAlignment(Pos.CENTER_LEFT);
        urlBox.setPadding(new Insets(5, 0, 5, 0));
        urlBox.setPrefHeight(60);;

        urlLinkBtn.setOnAction(action -> {
            urlBox.getChildren().remove(urlLinkBtn);
            urlBox.getChildren().add(urlLinkField);
            Platform.runLater(() -> urlLinkField.requestFocus());

        });

        urlLinkField.textProperty().addListener((obs, oldVal, newVal) -> {

            if (!urlBox.getChildren().contains(urlEnterBtn)) {
                urlBox.getChildren().add(urlEnterBtn);
            }

        });

        urlLinkField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {

            } else {
                String text = urlLinkField.getText();

                urlLinkBtn.setText(text.equals("") ? "Enter url" : text);

                urlBox.getChildren().remove(urlLinkField);
                urlBox.getChildren().add(urlLinkBtn);
                if (urlBox.getChildren().contains(urlEnterBtn)) {
                    urlBox.getChildren().remove(urlEnterBtn);
                }

            }
        });

        urlLinkField.setOnKeyPressed((keyEvent) -> {

            KeyCode keyCode = keyEvent.getCode();
            if (keyCode == KeyCode.ENTER) {
                String text = urlLinkField.getText();

                urlLinkBtn.setText(text.equals("") ? "Enter url" : text);

                urlBox.getChildren().remove(urlLinkField);

                if (urlBox.getChildren().contains(urlEnterBtn)) {
                    urlBox.getChildren().remove(urlEnterBtn);
                }
                urlBox.getChildren().add(urlLinkBtn);
            }
        });

        urlEnterBtn.setOnAction((keyEvent) -> {

            String text = urlLinkField.getText();

            urlLinkBtn.setText(text.equals("") ? "Enter url" : text);

            urlBox.getChildren().remove(urlLinkField);

            if (urlBox.getChildren().contains(urlEnterBtn)) {
                urlBox.getChildren().remove(urlEnterBtn);
            }
            urlBox.getChildren().add(urlLinkBtn);

        });

        Text imageFileCaret = new Text("Image File ");
        imageFileCaret.setFont(App.txtFont);
        imageFileCaret.setFill(Color.WHITE);

        
        Button imageFileBtn = new Button("(select image)");
        imageFileBtn.setId("rowBtn");
        imageFileBtn.setFont(App.txtFont);

        imageFileBtn.setContentDisplay(ContentDisplay.LEFT);
        imageFileBtn.setAlignment(Pos.CENTER_LEFT);
        imageFileBtn.setPadding(new Insets(10, 10, 10, 10));
        imageFileBtn.textProperty().addListener((obs,oldval,newval)->{
            if(newval.startsWith("/assets")){
                imageFileBtn.setText("(select image)");
            }
        });
  

        selectedImageFile.addListener((obs,oldval,newval)->{
            if(newval != null){
                try{
                    Image img = Utils.getImageByFile(newval);
                    imageBtn.setGraphic(IconButton.getIconView(img, 135));
                    imageFileBtn.setText(newval.getAbsolutePath());
                }catch(IOException e){
                    Alert a = new Alert(AlertType.NONE, "Unable to load image", ButtonType.OK);
                    a.setTitle("Error");
                    a.setHeaderText("Error");
                    a.initOwner(parentStage);
                    a.show();
                }
            }else{
                imageBtn.setGraphic(IconButton.getIconView(defaultImage, 135));
                imageBtn.setText(defaultImage.getUrl());
            }
        });
        
        HBox imageFileBox = new HBox(imageFileCaret, imgFileSpacerBtn, imageFileBtn);
        HBox.setHgrow(imageFileBox, Priority.ALWAYS);
        imageFileBox.setAlignment(Pos.CENTER_LEFT);
        imageFileBox.setPadding(new Insets(5, 0, 5, 0));
       
        VBox scrollPaneVBox = new VBox(nameBox, tokenIdBox, urlBox, imageFileBox);
        scrollPaneVBox.setPadding(new Insets(0, 20, 20, 40));
        HBox.setHgrow(scrollPaneVBox, Priority.ALWAYS);
        scrollPaneVBox.setId("bodyBox");




        VBox bodyVBox = new VBox( imageBox, promptBox, scrollPaneVBox);
        bodyVBox.setId("bodyBox");
        HBox.setHgrow(bodyVBox,Priority.ALWAYS);
        Button okButton = new Button("Ok");
        okButton.setFont(App.txtFont);
        okButton.setPrefWidth(100);
        okButton.setPrefHeight(30);

        SimpleObjectProperty<Runnable> shutdownRunnable = new SimpleObjectProperty<>(null);

        okButton.setOnAction(e -> {

            if (nameField.getText().length() < 1) {
                Alert nameAlert = new Alert(AlertType.NONE, "Name must be at least 1 character long.", ButtonType.OK);
                nameAlert.initOwner(parentStage);
                nameAlert.setGraphic(IconButton.getIconView(m_ergoTokens.getAppIcon(), 75));
                nameAlert.show();
            } else {
                

                if (tokenIdField.getText().length() < 3) {
                    Alert tokenAlert = new Alert(AlertType.NONE, "Token Id must be at least 3 characters long.", ButtonType.OK);
                    tokenAlert.initOwner(parentStage);
                    tokenAlert.setGraphic(IconButton.getIconView(m_ergoTokens.getAppIcon(), 75));
                    tokenAlert.show();
                } else {
                    
                    
                    if(selectedImageFile.get() != null){
                        try {
                            
                            
                            if(!m_tokensDir.isDirectory()){
                                Files.createDirectory(m_tokensDir.toPath());
                            }
                            
                            String tokenDirName = Utils.removeInvalidChars(nameField.getText());

                            File tokenDir = new File(m_tokensDir.getAbsolutePath() + "/" + tokenDirName);  
  
                            if(!tokenDir.exists()){
                                Files.createDirectory(tokenDir.toPath());
                            }
                   
                            BufferedImage bufImg = SwingFXUtils.fromFXImage(Utils.getImageByFile(selectedImageFile.get()), null);
                            BufferedImage newImage = bufImg.getWidth() > 75 || bufImg.getHeight() > 75 ? Drawing.resizeImage(bufImg, 75, 75,true) : bufImg;
                                      
                            
                            SimpleObjectProperty<File> newImgFile = new SimpleObjectProperty<>(null);
                            int i = 0;
                            while(newImgFile.get() == null){
                                File newFile = new File(tokenDir +"/" + tokenDirName + i + ".png");

                                if(!newFile.isFile()){
                                    newImgFile.set(newFile);
                                }else{
                                    i++;
                                }
                            }
                            ImageIO.write(newImage,"png", newImgFile.get());

                            HashData hashData = new HashData(newImgFile.get());

                            ErgoTokenData newToken = new ErgoTokenData(nameField.getText(), urlLinkField.getText(), tokenIdField.getText(),newImgFile.get().getAbsolutePath() , hashData, networkType, this);
                            ErgoTokenData oldToken = getErgoToken(newToken.getTokenId());
                            
                            oldToken.setImageString(newImgFile.get().getAbsolutePath());
                            oldToken.setImgHashData(hashData);
                            oldToken.setName(nameField.getText());
                            oldToken.setUrlString(urlLinkField.getText()); 

                            if (oldToken != null) {
                                removeToken(oldToken.getNetworkId(), false);
                            } 
                          
                            addToken(newToken, false);
                            save();
                            if(shutdownRunnable.get() != null){
                                shutdownRunnable.get().run();
                            }
                            closeBtn.fire();
                                
                        } catch (Exception e1) {
                            Alert tokenAlert = new Alert(AlertType.NONE, "Unable to open image file.\n" + e1.toString(), ButtonType.OK);
                            tokenAlert.initOwner(parentStage);
                            tokenAlert.setGraphic(IconButton.getIconView(m_ergoTokens.getAppIcon(), 75));
                            tokenAlert.setTitle("Error");
                            tokenAlert.setHeaderText("Error");
                            tokenAlert.show();
                        }
                        
                    }else{
                  
                        ErgoTokenData newToken = new ErgoTokenData(nameField.getText(), urlLinkField.getText(), tokenIdField.getText(),tokenNull ? "" : tokenImgString , tokenNull ? null : tokenHashData, m_networkType, this);
                        ErgoTokenData oldToken = getErgoToken(newToken.getTokenId());

                        if (oldToken != null) {
                    
                            removeToken(oldToken.getNetworkId(), false);
                
                        } 
                        addToken(newToken);
                        
                        if(shutdownRunnable.get() != null){
                            shutdownRunnable.get().run();
                        }
                        closeBtn.fire();
                        
                    }
                    
                }
            }
        });

        HBox okButtonBox = new HBox(okButton);
        HBox.setHgrow(okButtonBox, Priority.ALWAYS);
        okButtonBox.setAlignment(Pos.CENTER_RIGHT);
        okButtonBox.setPadding(new Insets(10, 10, 10, 10));
        okButtonBox.setPrefHeight(60);;

        VBox footerVBox = new VBox(okButtonBox);
        HBox.setHgrow(footerVBox, Priority.ALWAYS);

        HBox footerHBox = new HBox(footerVBox);
        footerHBox.setPadding(new Insets(10, 10, 10, 5));
        footerHBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(footerHBox, Priority.ALWAYS);

        HBox bodyPaddingBox = new HBox(bodyVBox);
        bodyPaddingBox.setPadding(new Insets(5, 5, 5, 5));
        HBox.setHgrow(bodyPaddingBox,Priority.ALWAYS);

        VBox layoutVBox = new VBox(titleBox, bodyPaddingBox, footerHBox);
        VBox.setVgrow(layoutVBox, Priority.ALWAYS);
       

        Scene tokenEditorScene = new Scene(layoutVBox, m_sceneWidth, m_sceneHeight);
        tokenEditorScene.setFill(null);
        tokenEditorScene.getStylesheets().add("/css/startWindow.css");

        layoutVBox.prefWidthProperty().bind(tokenEditorScene.widthProperty());
        menuBar.prefWidthProperty().bind(tokenEditorScene.widthProperty());
        nameButton.prefWidthProperty().bind(nameBox.widthProperty().subtract(nameCaret.layoutBoundsProperty().getValue().getWidth()));
        imageFileBtn.prefWidthProperty().bind(imageFileBox.widthProperty().subtract(imageFileCaret.layoutBoundsProperty().getValue().getWidth()));
        tokenIdBtn.prefWidthProperty().bind(tokenIdBox.widthProperty().subtract(tokenIdCaret.layoutBoundsProperty().getValue().getWidth()));
        urlLinkBtn.prefWidthProperty().bind(urlBox.widthProperty().subtract(urlLinkCaret.layoutBoundsProperty().getValue().getWidth()));

        nameBox.prefHeightProperty().bind(rowHeight);
        tokenIdBox.prefHeightProperty().bind(rowHeight);
        urlBox.prefHeightProperty().bind(rowHeight);
        imageFileBox.prefHeightProperty().bind(rowHeight);

        rowHeight.bind(tokenEditorScene.heightProperty().subtract(footerHBox.heightProperty()).subtract(imageBox.heightProperty()).subtract(promptBox.heightProperty()).divide(4));


   

        EventHandler<ActionEvent> imageClickEvent = (event) -> {

            FileChooser imageChooser = new FileChooser();
            imageChooser.setTitle("Token Editor - Select Image file");
            imageChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image (*.jpg, *.png)", "*.png", "*.jpg", "*.jpeg"));

            File chosenFile = imageChooser.showOpenDialog(parentStage);

            if (chosenFile != null && chosenFile.isFile()) {

                String mimeTypeString = "";
                try {
                    String probeContent = Files.probeContentType(chosenFile.toPath());
                    mimeTypeString = probeContent.split("/")[0];

                } catch (IOException e) {

                }
      
                if (mimeTypeString.equals("image")) {
                    selectedImageFile.set(chosenFile);
                }
            }

        };

        imageBtn.setOnAction(imageClickEvent);

        imageFileBtn.setOnAction(imageClickEvent);

        return tokenEditorScene;
    }
   
    public void save(){
        m_ergoTokens.getNetworksData().save("data",NetworkType.MAINNET.toString(), ErgoTokens.NETWORK_ID, ErgoNetwork.NETWORK_ID, getJsonObject());
        
        m_ergoTokens.sendMessage(App.LIST_CHANGED);
    }

 
    public JsonObject getJsonObject() {
        JsonObject tokensListJson = new JsonObject();
        tokensListJson.addProperty("networkType", m_networkType.toString());
        JsonArray jsonArray = new JsonArray();

        for (int i = 0; i < m_dataList.size(); i++) {
    
            ErgoTokenData ergoNetworkToken = m_dataList.get(i);
            jsonArray.add(ergoNetworkToken.getJsonObject());
        
        }

        tokensListJson.add("data", jsonArray);

        return tokensListJson;
    }

     public void addToken(String key, String imageString, HashData hashData) {
        ErgoTokenData ergoToken = null;
       
        NetworkType networkType = NetworkType.MAINNET;
        switch (key) {
            case "aht":
                ergoToken = new ErgoTokenData("Ergo Auction House", "https://ergoauctions.org/", "18c938e1924fc3eadc266e75ec02d81fe73b56e4e9f4e268dffffcb30387c42d", imageString, hashData, networkType, this);
                break;
            case "comet":
                ergoToken = new ErgoTokenData("Comet", "https://thecomettoken.com/", "0cd8c9f416e5b1ca9f986a7f10a84191dfb85941619e49e53c0dc30ebf83324b", imageString, hashData, networkType, this);
                break;
            case "cypx":
                ergoToken = new ErgoTokenData("CyberVerse", "https://cybercitizens.io/dist/pages/cyberverse.html", "01dce8a5632d19799950ff90bca3b5d0ca3ebfa8aaafd06f0cc6dd1e97150e7f", imageString, hashData, networkType, this);
                break;
            case "egio":
                ergoToken = new ErgoTokenData("ErgoGames.io", "https://www.ergogames.io/", "00b1e236b60b95c2c6f8007a9d89bc460fc9e78f98b09faec9449007b40bccf3", imageString, hashData, networkType, this);
                break;
            case "epos":
                ergoToken = new ErgoTokenData("ErgoPOS", "https://www.tabbylab.io/", "00bd762484086cf560d3127eb53f0769d76244d9737636b2699d55c56cd470bf", imageString, hashData, networkType, this);
                break;
            case "erdoge":
                ergoToken = new ErgoTokenData("Erdoge", "https://erdoge.biz/", "36aba4b4a97b65be491cf9f5ca57b5408b0da8d0194f30ec8330d1e8946161c1", imageString, hashData, networkType, this);
                break;
            case "ergold":
                ergoToken = new ErgoTokenData("Ergold", "https://github.com/supERGeometry/Ergold", "e91cbc48016eb390f8f872aa2962772863e2e840708517d1ab85e57451f91bed", imageString, hashData, networkType, this);
                break;
            case "ergone":
                ergoToken = new ErgoTokenData("ErgOne NFT", "http://ergone.io/", "fcfca7654fb0da57ecf9a3f489bcbeb1d43b56dce7e73b352f7bc6f2561d2a1b", imageString, hashData, networkType, this);
                break;
            case "ergopad":
                ergoToken = new ErgoTokenData("ErgoPad", "https://www.ergopad.io/", "d71693c49a84fbbecd4908c94813b46514b18b67a99952dc1e6e4791556de413", imageString, hashData, networkType, this);
                break;
            case "ermoon":
                ergoToken = new ErgoTokenData("ErMoon", "", "9dbc8dd9d7ea75e38ef43cf3c0ffde2c55fd74d58ac7fc0489ec8ffee082991b", imageString, hashData, networkType, this);
                break;
            case "exle":
                ergoToken = new ErgoTokenData("Ergo-Lend", "https://exle.io/", "007fd64d1ee54d78dd269c8930a38286caa28d3f29d27cadcb796418ab15c283", imageString, hashData, networkType, this);
                break;
            case "flux":
                ergoToken = new ErgoTokenData("Flux", "https://runonflux.io/", "e8b20745ee9d18817305f32eb21015831a48f02d40980de6e849f886dca7f807", imageString, hashData, networkType, this);
                break;
            case "getblock":
                ergoToken = new ErgoTokenData("GetBlok.io", "https://www.getblok.io/", "4f5c05967a2a68d5fe0cdd7a688289f5b1a8aef7d24cab71c20ab8896068e0a8", imageString, hashData, networkType, this);
                break;
            case "kushti":
                ergoToken = new ErgoTokenData("Kushti", "https://github.com/kushti", "fbbaac7337d051c10fc3da0ccb864f4d32d40027551e1c3ea3ce361f39b91e40", imageString, hashData, networkType, this);
                break;
            case "love":
                ergoToken = new ErgoTokenData("Love", "https://explorer.ergoplatform.com/en/issued-tokens?searchQuery=3405d8f709a19479839597f9a22a7553bdfc1a590a427572787d7c44a88b6386", "3405d8f709a19479839597f9a22a7553bdfc1a590a427572787d7c44a88b6386", imageString, hashData, networkType, this);
                break;
            case "lunadog":
                ergoToken = new ErgoTokenData("LunaDog", "https://explorer.ergoplatform.com/en/issued-tokens?searchQuery=5a34d53ca483924b9a6aa0c771f11888881b516a8d1a9cdc535d063fe26d065e", "5a34d53ca483924b9a6aa0c771f11888881b516a8d1a9cdc535d063fe26d065e", imageString, hashData, networkType, this);
                break;
            case "migoreng":
                ergoToken = new ErgoTokenData("Mi Goreng", "https://docs.google.com/spreadsheets/d/148c1iHNMNfyjscCcPznepkEnMp2Ycj3HuLvpcLsnWrM/edit#gid=205730070", "0779ec04f2fae64e87418a1ad917639d4668f78484f45df962b0dec14a2591d2", imageString, hashData, networkType, this);
                break;
            case "neta":
                ergoToken = new ErgoTokenData("anetaBTC", "https://anetabtc.io/", "472c3d4ecaa08fb7392ff041ee2e6af75f4a558810a74b28600549d5392810e8", imageString, hashData, networkType, this);
                break;
            case "obsidian":
                ergoToken = new ErgoTokenData("Adventurers DAO", "https://adventurersdao.xyz/", "2a51396e09ad9eca60b1bdafd365416beae155efce64fc3deb0d1b3580127b8f", imageString, hashData, networkType, this);
                break;
            case "ogre":
                ergoToken = new ErgoTokenData("Ogre", "https://ogre-token.web.app", "6de6f46e5c3eca524d938d822e444b924dbffbe02e5d34bd9dcd4bbfe9e85940", imageString, hashData, networkType, this);
                break;
            case "paideia":
                ergoToken = new ErgoTokenData("Paideia", "https://www.paideia.im/", "1fd6e032e8476c4aa54c18c1a308dce83940e8f4a28f576440513ed7326ad489", imageString, hashData, networkType, this);
                break;
            case "proxie":
                ergoToken = new ErgoTokenData("Proxies NFT", "https://proxiesnft.io/", "01ddcc3d0205c2da8a067ffe047a2ccfc3e8241bc3fcc6f6ebc96b7f7363bb36", imageString, hashData, networkType, this);
                break;
            case "quacks":
                ergoToken = new ErgoTokenData("duckpools.io", "https://www.duckpools.io/", "089990451bb430f05a85f4ef3bcb6ebf852b3d6ee68d86d78658b9ccef20074f", imageString, hashData, networkType, this);
                break;
            case "sigrsv":
                ergoToken = new ErgoTokenData("Sigma Reserve", "https://sigmausd.io/", "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0", imageString, hashData, networkType, this);
                break;
            case "sigusd":
                ergoToken = new ErgoTokenData("Sigma USD", "https://sigmausd.io/", "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04", imageString, hashData, networkType, this);
                break;
            case "spf":
                ergoToken = new ErgoTokenData("Spectrum Finanace", "https://spectrum.fi/", "9a06d9e545a41fd51eeffc5e20d818073bf820c635e2a9d922269913e0de369d", imageString, hashData, networkType, this);
                break;
            case "terahertz":
                ergoToken = new ErgoTokenData("swamp.audio", "https://www.thz.fm/", "02f31739e2e4937bb9afb552943753d1e3e9cdd1a5e5661949cb0cef93f907ea", imageString, hashData, networkType, this);
                break;
            case "walrus":
                ergoToken = new ErgoTokenData("Walrus Dao", "https://www.walrusdao.io/", "59ee24951ce668f0ed32bdb2e2e5731b6c36128748a3b23c28407c5f8ccbf0f6", imageString, hashData, networkType, this);
                break;
            case "woodennickels":
                ergoToken = new ErgoTokenData("Wooden Nickles", "https://brianrxm.com/comimg/cnsmovtv_perrymason_woodennickels_12.jpg", "4c8ac00a28b198219042af9c03937eecb422b34490d55537366dc9245e85d4e1", imageString, hashData, networkType, this);
                break;
        }
        if(ergoToken != null){
            
            addToken(ergoToken, false);
            
        
        }
    }


    
}
