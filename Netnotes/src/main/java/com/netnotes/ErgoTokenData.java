package com.netnotes;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netnotes.IconButton.IconStyle;
import com.utils.Utils;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
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

public class ErgoTokenData extends PriceCurrency    {

    private SimpleBooleanProperty m_explorerVerified = new SimpleBooleanProperty(false);

    private SimpleDoubleProperty m_sceneWidth = new SimpleDoubleProperty(450);
    private SimpleDoubleProperty m_sceneHeight = new SimpleDoubleProperty(575);

    private Stage m_ergoTokenStage = null;
    private ErgoTokensList m_tokensList = null;


    public ErgoTokenData(String name, String tokenId, int decimals, NetworkType networkType, JsonObject jsonObject, ErgoTokensList tokensList) {
        super(tokenId, name, name, name, decimals,  "/assets/unknown-unit.png", networkType.toString(),0, 0);
        m_tokensList = tokensList;

        JsonElement imageStringElement = jsonObject.get("imageString");
        JsonElement urlElement = jsonObject.get("url");
        //JsonElement sceneWidthElement = jsonObject.get("sceneWidth");
        //JsonElement sceneHeightElement = jsonObject.get("sceneHeight");
        JsonElement tokenDataElement = jsonObject.get("tokenData");
        JsonElement hashDataElement = jsonObject.get("hashData");
        JsonElement explorerVerifiedElement = jsonObject.get("explorerVerified");
        JsonElement timeStampElement = jsonObject.get("timeStamp");
        JsonElement emissionAmountElement = jsonObject.get("emissionAmount");
        JsonElement descriptionElement = jsonObject.get("description");
        
        m_explorerVerified.set(explorerVerifiedElement != null && explorerVerifiedElement.isJsonPrimitive() ? explorerVerifiedElement.getAsBoolean() : false);
    
        if(timeStampElement != null && timeStampElement.isJsonPrimitive()){
            setTimeStamp(timeStampElement.getAsLong());
        }

        if(emissionAmountElement != null){
            setEmissionAmount(emissionAmountElement.getAsLong());
        }

        if(descriptionElement != null){
            setDescription(descriptionElement.getAsString());
        }

        /*if (sceneWidthElement != null) {
            m_sceneWidth.set(sceneWidthElement.getAsDouble());
        }

        if (sceneHeightElement != null) {
            m_sceneHeight.set(sceneHeightElement.getAsDouble());
        }*/

        if (hashDataElement != null && hashDataElement.isJsonObject()) {
            setImgHashData(new HashData(hashDataElement.getAsJsonObject()));
        }

        if (urlElement != null) {
            urlProperty().set(urlElement.getAsString());
        }

        if (imageStringElement != null && getImgHashData() != null) {
            setImageString(imageStringElement.getAsString());
        }

        setNetworkType(networkType.toString());

        if (tokenDataElement != null && tokenDataElement.isJsonObject()) {
            JsonObject sourceJson = tokenDataElement.getAsJsonObject();
           
            setTokenData(sourceJson);
        }
 
     
    }

    public ErgoTokenData(String name, String url,int decimals, long emissionAmount, String description, String tokenId, String fileString,  HashData hashData, NetworkType networkType, ErgoTokensList tokensList) {
        super(tokenId, name, name, description, decimals,   fileString, networkType.toString(),emissionAmount, 0);
        m_tokensList = tokensList;
        urlProperty().set(url);
        setNetworkType(networkType.toString());
    }


    //tokenId, decimals, name, tokenType
    public ErgoTokenData(String tokenId, String name, int decimals, NetworkType networkType, ErgoTokensList tokensList) {
       
        super(tokenId, name, name, name, decimals,  "", networkType.toString(),0, 0);
        m_tokensList = tokensList;
        urlProperty().set("");
     
       setNetworkType(networkType.toString());

   }
   
    public ErgoTokenData(ErgoTokensList tokensList,long emissionAmount, String description, String tokenId, String name, String symbol, int fractionalPrecision,String networkType, String imageString, String tokenType, String fontSymbol){
        super(tokenId, name, symbol, fractionalPrecision,  networkType, imageString, tokenType, fontSymbol);
        m_tokensList = tokensList;

        setEmissionAmount(emissionAmount);
        setDescription(description);


    }


    
    

    public void open() {
            
        showTokenStage();
    }

    public void setTokenData(JsonObject sourceJson){
        JsonElement timeStampElement = sourceJson.get("timeStamp");
        long timeStamp =  timeStampElement != null && timeStampElement.isJsonPrimitive() ? timeStampElement.getAsLong() : System.currentTimeMillis();

        JsonElement emissionAmountElement = sourceJson.get("emissionAmount");
        JsonElement nameElement = sourceJson.get("name");
        JsonElement descriptionElement = sourceJson.get("description");
        JsonElement decimalsElement = sourceJson.get("decimals");
        JsonElement typeElement = sourceJson.get("type");

        long emissionAmount = emissionAmountElement == null ? 0 : emissionAmountElement.getAsLong();
        String tokenName = nameElement == null ? null : nameElement.getAsString();
        String description = descriptionElement == null ? null : descriptionElement.getAsString();
        int decimals = decimalsElement == null ? 0 : decimalsElement.getAsInt();
        
        String type = typeElement != null && typeElement.isJsonPrimitive() ? typeElement.getAsString() : "";
        setEmissionAmount(emissionAmount);
        setDescription(description);
        setDecimals(decimals);
        setName(tokenName);
        setTimeStamp(timeStamp);
        setTokenType(type);


    }



    public SimpleBooleanProperty explorerVerifiedProperty() {
        return m_explorerVerified;
    }

    public NetworkType getNetworkType() {
        String networkTypeString = getNetworkTypeString();
        return networkTypeString != null ? (networkTypeString.equals(NetworkType.MAINNET.toString()) ? NetworkType.MAINNET : NetworkType.TESTNET) : NetworkType.MAINNET;
    }

    private NoteInterface getExplorer(){
        return m_tokensList.getErgoTokens().getErgoNetworkData().selectedExplorerData().get();
    }


    public void update() {
        NoteInterface noteInterface = getExplorer();
        if(noteInterface != null && !m_explorerVerified.get()){
            JsonObject note = Utils.getCmdObject("getTokenInfo");
            note.addProperty("tokenId",getTokenId());
            note.addProperty("networkId", m_tokensList.getErgoTokens().getErgoNetworkData().getId());
            
            noteInterface.sendNote(note,succeededEvent -> {
                
                Object sourceObject = succeededEvent.getSource().getValue();

                if (sourceObject != null && sourceObject instanceof JsonObject) {
                    JsonObject sourceJson = (JsonObject) sourceObject;
                   
                    
                    setTokenData(sourceJson);
                    m_explorerVerified.set(true);
                    m_tokensList.save();
                    m_tokensList.getErgoTokens().sendMessage(App.LIST_UPDATED, System.currentTimeMillis(), getTokenId());
                }
            }, failedEvent -> {
                m_explorerVerified.set(false);
                try {
                    Files.writeString(App.logFile.toPath(), "\nFailed: " + getName() + ": " + failedEvent.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }
            }, null);
        
        }
    }

    private ArrayList<NoteMsgInterface> m_msgInterfaces = new ArrayList<>();

    public void addMsgListener(NoteMsgInterface msgInterface){
        m_msgInterfaces.add(msgInterface);
    }

    public void removeMsgListener(NoteMsgInterface msgInterface){
        m_msgInterfaces.remove(msgInterface);
    }

    public String getId(){
        return getNetworkId();
    }

    public void sendMessage(String networkId, int code, long timestamp){
        for(int i = 0; i < m_msgInterfaces.size(); i ++){
            NoteMsgInterface noteMsgInterface = m_msgInterfaces.get(i);
            noteMsgInterface.sendMessage(networkId, code, timestamp);
        }
    }
    public void sendMessage(int code, long timestamp){
        for(int i = 0; i < m_msgInterfaces.size(); i ++){
            NoteMsgInterface noteMsgInterface = m_msgInterfaces.get(i);
            noteMsgInterface.sendMessage(code, timestamp);
        }
    }
    public void sendMessage(int code, long timestamp, String msg){
        for(int i = 0; i < m_msgInterfaces.size(); i ++){
            NoteMsgInterface noteMsgInterface = m_msgInterfaces.get(i);
            noteMsgInterface.sendMessage(code, timestamp, msg);
        }
    }

    public void sendMessage(String networkId, int code, long timestamp, String msg){
        for(int i = 0; i < m_msgInterfaces.size(); i ++){
            NoteMsgInterface noteMsgInterface = m_msgInterfaces.get(i);
            noteMsgInterface.sendMessage(networkId, code, timestamp);
        }
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
                update();
            }

            Text promptText = new Text("");
            TextArea descriptionTextArea = new TextArea();

            Label emissionLbl = new Label();
            TextField emissionAmountField = new TextField();
            Button closeBtn = new Button();
          //  Button maximizeBtn = new Button();
           /* if (getTimeStamp() != 0) {

       
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
            }*/
            
            Button maximizeBtn = new Button();
            
            HBox titleBox = App.createTopBar(icon, maximizeBtn, closeBtn, m_ergoTokenStage);

            maximizeBtn.setOnAction(e->{
                m_ergoTokenStage.setMaximized(!m_ergoTokenStage.isMaximized());
            });

       
           

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
              
                //Scene editScene = getEditTokenScene(this, getNetworkType(), m_ergoTokenStage, closeBtn);
                //m_ergoTokenStage.setScene(editScene);

            });

            HBox menuBar = new HBox(editButton, spacer);
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

            Button urlLink = new Button("visit: " + getUrlString());
            urlLink.setFont(App.txtFont);
            urlLink.setId("addressBtn");
            urlLink.setOnAction(e -> {
                m_tokensList.getErgoTokens().getNetworksData().getHostServices().showDocument(getUrlString());
            
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

 
            ResizeHelper.addResizeListener(m_ergoTokenStage, 300, 600, Double.MAX_VALUE, Double.MAX_VALUE);

           
   
            m_ergoTokenStage.show();

      
            urlLink.maxWidthProperty().bind(tokenScene.widthProperty().multiply(0.75));

            descriptionTextArea.prefWidthProperty().bind(m_ergoTokenStage.widthProperty().subtract(40));
            descriptionTextArea.prefHeightProperty().bind(tokenScene.heightProperty().subtract(titleBox.heightProperty()).subtract(imageBox.heightProperty()).subtract(promptBox.heightProperty()).subtract(footerHBox.heightProperty()));

          


            Runnable updateData = ()->{
              
                promptText.setText(getName());
                descriptionTextArea.setText(getDescription());
                

                int decimals = getDecimals();

                long longAmount = getEmissionAmount();

                if (longAmount != 0) {
                    BigDecimal emissionAmount = BigDecimal.valueOf(longAmount);
                    BigDecimal bigAmount = decimals != 0 ? emissionAmount.divide(BigDecimal.valueOf(10).pow(decimals), decimals, RoundingMode.UNNECESSARY) : emissionAmount;
                    emissionLbl.setText("Total Supply:");
                    emissionAmountField.setText(bigAmount.toString());
                }

            };

            getLastUpdated().addListener((obs,oldval, newval)->updateData.run());
            
            updateData.run();

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
        jsonObject.addProperty("url", urlProperty().get());
       // jsonObject.addProperty("sceneWidth", m_sceneWidth.get());
        //jsonObject.addProperty("sceneHeight", m_sceneHeight.get());
        jsonObject.addProperty("networkType", getNetworkTypeString());
        jsonObject.addProperty("emissionAmount", getEmissionAmount());
        jsonObject.addProperty("explorerVerified", m_explorerVerified.get());
        if(getDescription() != null){
            jsonObject.addProperty("description", getDescription());
        }

        jsonObject.addProperty("decimals", getDecimals());
    
        jsonObject.addProperty("timeStamp", getTimeStamp());
        if(getNetworkTypeString() != null){
            jsonObject.addProperty("networkType", getNetworkTypeString());
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
        return urlProperty().get();
    }



    @Override
    public String toString() {
        return getName();
    }

    public void sendMessage(String networkId, int code, long timestamp, JsonObject json){
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


/*
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
                            oldToken.urlProperty().set(urlLinkField.getText()); 

                            replaceToken(newToken);
                         

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
                  
                        ErgoTokenData newToken = new ErgoTokenData(nameField.getText(), urlLinkField.getText(), tokenIdField.getText(),tokenNull ? "" : tokenImgString , tokenNull ? null : tokenHashData, getNetworkType(), this);
                      
                      
                        replaceToken(newToken);
                        
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
 */