package com.netnotes;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netnotes.IconButton.IconStyle;
import com.utils.Utils;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ErgoNetworkToken extends PriceCurrency {

    private File logFile = new File("netnotes-log.txt");

    private SimpleBooleanProperty m_explorerVerified = new SimpleBooleanProperty(false);

    private SimpleDoubleProperty m_sceneWidth = new SimpleDoubleProperty(450);
    private SimpleDoubleProperty m_sceneHeight = new SimpleDoubleProperty(575);
    private String m_urlString;
    private Stage m_ergoTokenStage = null;
    private ErgoTokensList m_tokensList = null;


  
    public ErgoNetworkToken(String name, String tokenId, NetworkType networkType, JsonObject jsonObject, ErgoTokensList tokensList) {
        super(tokenId, name, name, name, 0,  ErgoNetwork.NETWORK_ID, "/assets/unknown-unit.png", networkType.toString(),0, 0);
        m_tokensList = tokensList;

        JsonElement imageStringElement = jsonObject.get("imageString");
        JsonElement urlElement = jsonObject.get("url");
        JsonElement sceneWidthElement = jsonObject.get("sceneWidth");
        JsonElement sceneHeightElement = jsonObject.get("sceneHeight");
        JsonElement tokenDataElement = jsonObject.get("tokenData");
        JsonElement hashDataElement = jsonObject.get("hashData");

        if (sceneWidthElement != null) {
            m_sceneWidth.set(sceneWidthElement.getAsDouble());
        }

        if (sceneHeightElement != null) {
            m_sceneHeight.set(sceneHeightElement.getAsDouble());
        }

        if (hashDataElement != null && hashDataElement.isJsonObject()) {
            setImgHashData(new HashData(hashDataElement.getAsJsonObject()));
        }

        if (urlElement != null) {
            m_urlString = urlElement.getAsString();
        }

        if (imageStringElement != null && getImgHashData() != null) {
            setImageString(imageStringElement.getAsString());
        }

        setNetworkType(networkType.toString());

        if (tokenDataElement != null && tokenDataElement.isJsonObject()) {
            JsonObject sourceJson = tokenDataElement.getAsJsonObject();
           
            setTokenData(sourceJson);
        }
        if(getTimeStamp() == 0){
            updateTokenInfo();
        }
     
    }

    public ErgoNetworkToken(String name, String url, String tokenId, String fileString, HashData hashData, NetworkType networkType, ErgoTokensList tokensList) {
         super(tokenId, name, name, name, 0,  ErgoNetwork.NETWORK_ID, fileString, networkType.toString(),0, 0);
        m_tokensList = tokensList;
        m_urlString = url;
      
        setNetworkType(networkType.toString());

        updateTokenInfo();
   
    }
    //tokenId, decimals, name, tokenType
    public ErgoNetworkToken(String tokenId, String name, int decimals, NetworkType networkType, ErgoTokensList tokensList) {
       
        super(tokenId, name, name, name, decimals,  ErgoNetwork.NETWORK_ID, "", networkType.toString(),0, 0);
        m_tokensList = tokensList;
        m_urlString = "";
     
       setNetworkType(networkType.toString());

       updateTokenInfo();
  
   }


    public void open() {
        
        showTokenStage();
    }

    public void setTokenData(JsonObject sourceJson){

        JsonElement emissionAmountElement = sourceJson.get("emissionAmount");
        JsonElement nameElement = sourceJson.get("name");
        JsonElement descriptionElement = sourceJson.get("description");
        JsonElement decimalsElement = sourceJson.get("decimals");
 

        long emissionAmount = emissionAmountElement == null ? 0 : emissionAmountElement.getAsLong();
        String tokenName = nameElement == null ? null : nameElement.getAsString();
        String description = descriptionElement == null ? null : descriptionElement.getAsString();
        int decimals = decimalsElement == null ? 0 : decimalsElement.getAsInt();
        
        setEmissionAmount(emissionAmount);
        setSymbol(tokenName);
        setDescription(description);
        setDecimals(decimals);
        setTimeStamp(System.currentTimeMillis());
        getLastUpdated().set(LocalDateTime.now());
    }

    


    public SimpleBooleanProperty explorerVerifiedProperty() {
        return m_explorerVerified;
    }

    public NetworkType getNetworkType() {
        String networkTypeString = getNetworkTypeString();
        return networkTypeString != null ? (networkTypeString.equals(NetworkType.MAINNET.toString()) ? NetworkType.MAINNET : NetworkType.TESTNET) : NetworkType.MAINNET;
    }



    public void updateTokenInfo() {
        ErgoExplorerData ergoExplorerData =  m_tokensList.selectedExplorerData().get();


        if(ergoExplorerData != null){

            ergoExplorerData.getTokenInfo(getTokenId(), succeededEvent -> {
                WorkerStateEvent workerEvent = succeededEvent;
                Object sourceObject = workerEvent.getSource().getValue();

                if (sourceObject != null && sourceObject instanceof JsonObject) {
                    JsonObject sourceJson = (JsonObject) sourceObject;
         
                
                    setTokenData(sourceJson);
                    m_explorerVerified.set(true);
                    getLastUpdated().set(LocalDateTime.now());
                    
                    
                }
            }, failedEvent -> {
                m_explorerVerified.set(false);
                try {
                    Files.writeString(logFile.toPath(), "\nFailed: " + getName() + ": " + failedEvent.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }
            });
    
        }

        
      

    }

    @Override
    public PriceQuote getPriceQuote(){
 
        return m_tokensList.findPriceQuoteById(getTokenId(), SpectrumMarketItem.ERG_ID);            
        
    }

    public void visitUrl(){
         m_tokensList.getErgoTokens().getNetworksData().getHostServices().showDocument(m_urlString);
    }


    public void showTokenStage() {
        if (m_ergoTokenStage == null) {
            String title = getName() + " - Ergo Tokens";
            Image icon = getIcon();
            
            m_ergoTokenStage = new Stage();
            m_ergoTokenStage.getIcons().add(icon);
            m_ergoTokenStage.initStyle(StageStyle.UNDECORATED);
            m_ergoTokenStage.setTitle(title);
            if(getTimeStamp() == 0){
                updateTokenInfo();
            }

            Text promptText = new Text("");
            TextArea descriptionTextArea = new TextArea();

            Label emissionLbl = new Label();
            TextField emissionAmountField = new TextField();
            Button closeBtn = new Button();
          //  Button maximizeBtn = new Button();
            if (getTimeStamp() != 0) {

       
                promptText.setText(getName());
                descriptionTextArea.setText(getDescription());
                long emissionAmount = getEmissionAmount();

                if (emissionAmount != 0) {
                    emissionLbl.setText("Total Supply:");
                    emissionAmountField.setText(emissionAmount + "");
                }
            } else {
                promptText.setText("");
                descriptionTextArea.setText("No informaiton available.");
                emissionLbl.setText("");
                emissionAmountField.setText("");
            }
            
            Button maximizeBtn = new Button();
            
            HBox titleBox = App.createTopBar(icon, maximizeBtn, closeBtn, m_ergoTokenStage);

            maximizeBtn.setOnAction(e->{
                m_ergoTokenStage.setMaximized(!m_ergoTokenStage.isMaximized());
            });

            Tooltip explorerTip = new Tooltip();
            explorerTip.setShowDelay(new javafx.util.Duration(100));
            explorerTip.setFont(App.txtFont);

            
            BufferedMenuButton explorerBtn = new BufferedMenuButton("/assets/ergo-explorer-30.png", App.MENU_BAR_IMAGE_WIDTH);
            explorerBtn.setPadding(new Insets(2, 0, 0, 2));
            explorerBtn.setTooltip(explorerTip);

            

           

            Runnable updateExplorerBtn = () ->{
                ErgoExplorers ergoExplorers = (ErgoExplorers) m_tokensList.getErgoTokens().getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);
            
                ErgoExplorerData explorerData = m_tokensList.selectedExplorerData().get();            
            
                if(explorerData != null && ergoExplorers != null){
                
                    explorerTip.setText("Ergo Explorer: " + explorerData.getName());
                    

                }else{
                    
                    if(ergoExplorers == null){
                        explorerTip.setText("(install 'Ergo Explorer')");
                    }else{
                        explorerTip.setText("Select Explorer...");
                    }
                }
                
            };

            Runnable getAvailableExplorerMenu = () ->{
            
                ErgoExplorers ergoExplorers = (ErgoExplorers) m_tokensList.getErgoTokens().getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);
                if(ergoExplorers != null){
                    explorerBtn.setId("menuBtn");
                    m_tokensList.ergoExplorerListProperty().get().getMenu(explorerBtn, m_tokensList.selectedExplorerData());
                }else{
                    explorerBtn.getItems().clear();
                    explorerBtn.setId("menuBtnDisabled");
                
                }
                updateExplorerBtn.run();
            };

            HBox rightSideMenu = new HBox(explorerBtn);
            rightSideMenu.setId("rightSideMenuBar");
            rightSideMenu.setPadding(new Insets(0, 10, 0, 20));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Tooltip editTip = new Tooltip("Edit");
            editTip.setShowDelay(new javafx.util.Duration(100));
            editTip.setFont(App.txtFont);

            Button editButton = new Button();
            editButton.setGraphic(IconButton.getIconView(new Image("/assets/options-outline-white-30.png"), App.MENU_BAR_IMAGE_WIDTH));
            editButton.setId("menuBtn");
            editButton.setTooltip(editTip);
            editButton.setOnAction(e -> {
              
                Scene editScene = m_tokensList.getEditTokenScene(this, getNetworkType(), m_ergoTokenStage, closeBtn);
                m_ergoTokenStage.setScene(editScene);

            });

            HBox menuBar = new HBox(editButton, spacer, rightSideMenu);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 5));

            Button imageButton = App.createImageButton(icon, getName());
            imageButton.setGraphicTextGap(25);

            HBox imageBox = new HBox(imageButton);
            imageBox.setAlignment(Pos.CENTER);
            imageBox.setPadding(new Insets(25, 0, 25, 0));

            promptText.setFont(App.txtFont);
            promptText.setFill(Color.WHITE);

            HBox promptBox = new HBox(promptText);

            promptBox.prefHeight(40);
            promptBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(promptBox, Priority.ALWAYS);
            promptBox.setPadding(new Insets(10, 15, 10, 30));

            descriptionTextArea.setFont(App.txtFont);
            descriptionTextArea.setId("bodyBox");
            descriptionTextArea.setEditable(false);
            descriptionTextArea.setWrapText(true);

            emissionLbl.setFont(App.txtFont);
            emissionLbl.setTextFill(App.altColor);

            emissionAmountField.setFont(App.txtFont);
            emissionAmountField.setEditable(false);
            emissionAmountField.setId("formField");

            Button urlLink = new Button("visit: " + m_urlString);
            urlLink.setFont(App.txtFont);
            urlLink.setId("addressBtn");
            urlLink.setOnAction(e -> {
                visitUrl();
            });

            HBox urlBox = new HBox(urlLink);
            urlBox.setPadding(new Insets(25, 30, 25, 30));
  
            urlBox.setAlignment(Pos.CENTER);
            HBox.setHgrow(urlBox, Priority.ALWAYS);

            String tokenId = "Token ID: " + getTokenId();

            int tokenIdWidth = Utils.measureString(tokenId, new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 14));

            TextField tokenIdField = new TextField(tokenId);
            tokenIdField.setEditable(false);
            tokenIdField.setId("formFieldSmall");
            tokenIdField.setPrefWidth(tokenIdWidth +20);
          

            HBox tokenIdBox = new HBox(tokenIdField);
            tokenIdBox.setId("bodyBox");
            tokenIdBox.prefWidthProperty().bind(descriptionTextArea.widthProperty());
            tokenIdBox.setAlignment(Pos.CENTER);

            VBox scrollPaneVBox = new VBox(descriptionTextArea,tokenIdBox,  urlBox);
            scrollPaneVBox.setPadding(new Insets(0, 40, 0, 40));
            HBox.setHgrow(scrollPaneVBox, Priority.ALWAYS);

            HBox emissionBox = new HBox(emissionLbl, emissionAmountField);
            emissionBox.setAlignment(Pos.CENTER_LEFT);
            // emissionBox.setPadding(new Insets(0, 0, 0, 15));

            VBox footerVBox = new VBox(emissionBox);
            HBox.setHgrow(footerVBox, Priority.ALWAYS);

            HBox footerHBox = new HBox(footerVBox);
            footerHBox.setPadding(new Insets(25, 30, 25, 30));
            footerHBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(footerHBox, Priority.ALWAYS);

            VBox bodyVBox = new VBox(menuBar, imageBox, promptBox, scrollPaneVBox);

            HBox bodyPaddingBox = new HBox(bodyVBox);
            bodyPaddingBox.setPadding(new Insets(5,5,5,5));

            VBox layoutVBox = new VBox(titleBox, bodyPaddingBox, footerHBox);
            VBox.setVgrow(layoutVBox, Priority.ALWAYS);
    
            Scene tokenScene = new Scene(layoutVBox, m_sceneWidth.get(), m_sceneHeight.get());
            tokenScene.setFill(null);
            tokenScene.getStylesheets().add("/css/startWindow.css");
              
   
            m_ergoTokenStage.setScene(tokenScene);
            NetworksData netData = m_tokensList.getErgoTokens().getNetworksData();
  

            Rectangle rect = netData.getMaximumWindowBounds();
            
 
            ResizeHelper.addResizeListener(m_ergoTokenStage, 300, 600, rect.getWidth(), rect.getHeight());

           
   
            m_ergoTokenStage.show();

      
            urlLink.maxWidthProperty().bind(tokenScene.widthProperty().multiply(0.75));

            descriptionTextArea.prefWidthProperty().bind(m_ergoTokenStage.widthProperty().subtract(40));
            descriptionTextArea.prefHeightProperty().bind(tokenScene.heightProperty().subtract(titleBox.heightProperty()).subtract(imageBox.heightProperty()).subtract(promptBox.heightProperty()).subtract(footerHBox.heightProperty()));

             m_tokensList.getErgoTokens().getErgoNetworkData().addNetworkListener((ListChangeListener.Change<? extends NoteInterface> c) -> {
    
                getAvailableExplorerMenu.run();
        
            });

            getAvailableExplorerMenu.run();

           getLastUpdated().addListener((obs,oldval, newval)->{
              
                    
                    promptText.setText(getName());
                    descriptionTextArea.setText(getDescription());
                    long emissionAmount = getEmissionAmount();

                    if (emissionAmount != 0) {
                        emissionLbl.setText("Total Supply:");
                        emissionAmountField.setText(emissionAmount + "");
                    }
                
           });
    

            /* addShutdownListener((obs, oldVal, newVal) -> {

                Platform.runLater(() -> closeBtn.fire());
            });*/

            m_ergoTokenStage.setOnCloseRequest(e -> {
               // removeShutdownListener();
              //  m_priceCurrency.removeListener(tokenDataListener);
              //  close();
                
                closeBtn.fire();

            });

            closeBtn.setOnAction(event -> {

                m_ergoTokenStage.close();
                m_ergoTokenStage = null;
            });

        } else {
            if (m_ergoTokenStage.isIconified()) {
                m_ergoTokenStage.setIconified(false);
            }
            m_ergoTokenStage.show();
        }
    }


    @Override
    public JsonObject getJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", getName());
        jsonObject.addProperty("tokenId", getTokenId());
        jsonObject.addProperty("imageString", getImageString());
        jsonObject.addProperty("url", m_urlString);
        jsonObject.addProperty("sceneWidth", m_sceneWidth.get());
        jsonObject.addProperty("sceneHeight", m_sceneHeight.get());
        jsonObject.addProperty("networkType", getNetworkTypeString());
        jsonObject.addProperty("emissionAmount", getEmissionAmount());

        if(getDescription() != null){
            jsonObject.addProperty("description", getDescription());
        }

        jsonObject.addProperty("decimals", getDecimals());
    
        jsonObject.addProperty("timeStamp", getTimeStamp());
        if(getNetworkTypeString() != null){
            jsonObject.addProperty("networkId", getNetworkTypeString());
        }
        jsonObject.addProperty("symbol", getSymbol());
   
        if(getFontSymbol() != null){
            jsonObject.addProperty("fontSymbol", getFontSymbol());
        }
        if (getImgHashData() != null) {
            jsonObject.add("hashData", getImgHashData().getJsonObject());
        }
    
        return jsonObject;
    }


    public String getUrlString() {
        return m_urlString;
    }

    public void setUrlString(String url){
        m_urlString = url;
    }


    @Override
    public String toString() {
        return getName();
    }




    public String getNetworkId(){
        return getTokenId();
    }

    public static String getButtonText(String name, java.awt.Font font, double imageWidth){
        name = name.replace("\n", " ");
   
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        
        int stringWidth = fm.stringWidth(name);
  
        if (stringWidth > imageWidth) {
            int indexOfSpace = name.indexOf(" ");

            if (indexOfSpace > 0) {
                String firstWord = name.substring(0, indexOfSpace);

                if (Utils.measureString(firstWord, font) > imageWidth) {
                    return Utils.truncateText(name,fm, stringWidth);
                } else {

                    String text = firstWord + "\n";
                    String secondWord = name.substring(indexOfSpace + 1, name.length());

                    if (fm.stringWidth(secondWord) > imageWidth) {
                        secondWord = Utils.truncateText(secondWord, fm, imageWidth);
                    }
                    text = text + secondWord;
                    return text;
                }
            } else {

                return Utils.truncateText(name, fm, imageWidth);
            }

        } else {
            return name;
        }
    }
    public void openButton(){
        open();
    }
    public IconButton getButton(String iconStyle){
        java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 12);
        IconButton iconButton = new IconButton(iconStyle.equals(IconStyle.ROW) ? getIcon() : getIcon(), iconStyle.equals(IconStyle.ROW) ? getName() : getButtonText(getName(),font , 75), iconStyle) {
            @Override
            public void open() {
                openButton();
            }
        };

        iconButton.setUserData(this);

        if (iconStyle.equals(IconStyle.ROW)) {
            iconButton.setContentDisplay(ContentDisplay.LEFT);
            iconButton.setImageWidth(30);
        } else {
            iconButton.setContentDisplay(ContentDisplay.TOP);
            iconButton.setTextAlignment(TextAlignment.CENTER);
        }

        return iconButton;
    }
}
