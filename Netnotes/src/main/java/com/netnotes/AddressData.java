package com.netnotes;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import java.util.ArrayList;
import java.util.Collections;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.ErgoClient;
import org.ergoplatform.appkit.ErgoToken;
import org.ergoplatform.appkit.InputBoxesSelectionException;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.RestApiErgoClient;
import org.ergoplatform.appkit.UnsignedTransaction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netnotes.ErgoTransaction.TransactionStatus;
import com.netnotes.ErgoTransaction.TransactionType;
import com.satergo.Wallet;
import com.satergo.WalletKey;
import com.satergo.ergo.ErgoInterface;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.awt.Graphics2D;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;


public class AddressData extends Network {  
    
    public final static int UPDATE_LIMIT = 10;

    private int m_index;
    private Address m_address;
   
    private final ErgoAmount m_ergoAmount;
    private final PriceAmount m_unconfirmedAmount;

    private final ObservableList<PriceAmount> m_confirmedTokensList = FXCollections.observableArrayList();

    private final ObservableList<ErgoTransaction> m_watchedTransactions = FXCollections.observableArrayList();
    private final SimpleObjectProperty<ErgoTransaction> m_selectedTransaction = new SimpleObjectProperty<>(null);
    
    private ArrayList<PriceAmount> m_unconfirmedTokensList = new ArrayList<>();
    private AddressesData m_addressesData;
    private int m_apiIndex = 0;
    
    private SimpleObjectProperty<WritableImage> m_imgBuffer = new SimpleObjectProperty<WritableImage>(null);
    private final String m_addressString;
    private Wallet m_wallet = null;

  // private BufferedImage m_img = null;
    private Graphics2D m_g2d = null;

   // private java.awt.Font m_imgFont = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 30);
 //   private java.awt.Font m_imgSmallFont = new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 12);
    
 //   private BufferedImage m_unitImage = null;

   
    
    public AddressData(String name, int index, Address address, Wallet wallet, NetworkType networktype, AddressesData addressesData) {
        super(null, name, address.toString(), addressesData.getWalletData());
        m_wallet = wallet;
     
        m_addressesData = addressesData;
        m_index = index;
        m_address = address;
        m_addressString = address.toString();

        m_ergoAmount = new ErgoAmount(getErgoNetworkData().ergoPriceQuoteProperty());
        m_unconfirmedAmount= new ErgoAmount(getErgoNetworkData().ergoPriceQuoteProperty());

        getAddressInfo();
        
    }

    public JsonObject getAddressJson(){
        JsonObject json = new JsonObject();
        json.addProperty("index", m_index);
        json.addProperty("address", m_addressString);

        return json;
    }

    public AddressData getThis(){
        return this;
    }

    /*
    public void update(){
      //  updateBufferedImage();
      
    }
    
    

     public void updateBufferedImage() {
        
        long timeStamp = System.currentTimeMillis();

        ErgoAmount priceAmount = getErgoAmount();
        boolean quantityValid = priceAmount.amountProperty().get() != null &&  priceAmount.getTimeStamp() > 0 && (timeStamp - priceAmount.getTimeStamp()) < AddressesData.QUOTE_TIMEOUT; 
        double priceAmountDouble = quantityValid ? priceAmount.amountProperty().get().doubleValue() : 0;

        PriceQuote priceQuote = quantityValid ? priceAmount.priceQuoteProperty().get() : null;
        boolean priceValid = priceQuote != null && priceQuote.getTimeStamp() > 0 && (timeStamp - priceQuote.getTimeStamp()) <  AddressesData.QUOTE_TIMEOUT;
        double priceQuoteDouble = priceValid  && priceQuote != null ? priceQuote.getDoubleAmount() : 0;
        
        String totalPrice = priceValid && priceQuote != null ? Utils.formatCryptoString( priceQuoteDouble * priceAmountDouble, priceQuote.getQuoteCurrency(), priceQuote.getFractionalPrecision(),  quantityValid && priceValid) : " -.--";
        int integers = priceAmount != null ? (int) priceAmount.getDoubleAmount() : 0;
        double decimals = priceAmount != null ? priceAmount.getDoubleAmount() - integers : 0;
        
        PriceCurrency priceCurrency = priceAmount.getCurrency();

        int decimalPlaces = priceAmount != null ? priceCurrency.getFractionalPrecision() : 0;
        String cryptoName = priceAmount != null ? priceCurrency.getSymbol() : "UKNOWN";
        int space = cryptoName.indexOf(" ");
        cryptoName = space != -1 ? cryptoName.substring(0, space) : cryptoName;

        String currencyPrice = priceValid && priceQuote != null ? priceQuote.toString() : "-.--";

        

        //   Image ergoBlack25 = new Image("/assets/ergo-black-25.png");
        //   SwingFXUtils.fromFXImage(ergoBlack25, null);
        
        String amountString = quantityValid ? String.format("%d", integers) : " -";
        String decs = String.format("%." + decimalPlaces + "f", decimals);

        decs = quantityValid ? decs.substring(1, decs.length()) : "";
        totalPrice = totalPrice + "   ";
        currencyPrice = "(" + currencyPrice + ")   ";
    
       
        
        
        
        m_unitImage = SwingFXUtils.fromFXImage(priceAmount != null ? priceCurrency.getIcon() : new Image("/assets/unknown-unit.png"), m_unitImage);
        Drawing.setImageAlpha(m_unitImage, 0x40);
        //  adrBuchImg.getScaledInstance(width, height, java.awt.Image.SCALE_AREA_AVERAGING);
       // int width = Math.max( 200 ,(int) m_addressesData.widthProperty().get() - 150);

        if(m_img == null){
            m_img = new BufferedImage(AddressesData.ADDRESS_IMG_WIDTH, AddressesData.ADDRESS_IMG_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            m_g2d = m_img.createGraphics();
            m_g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            m_g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            m_g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            m_g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
            m_g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            m_g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            m_g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            m_g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }else{
            Drawing.fillArea(m_img, App.DEFAULT_RGBA, 0, 0, m_img.getWidth(), m_img.getHeight(), false);
        }
        
        m_g2d.setFont(m_imgFont);
        FontMetrics fm = m_g2d.getFontMetrics();
        int padding = 5;
        int stringWidth = fm.stringWidth(amountString);
       
        int height = fm.getHeight() + 10;

        m_g2d.setFont(m_imgSmallFont);

        fm = m_g2d.getFontMetrics();
        
        int priceWidth = fm.stringWidth(totalPrice);
        int currencyWidth = fm.stringWidth(currencyPrice);
        //int decsWidth = fm.stringWidth(decs);


        int priceLength = (priceWidth > currencyWidth ? priceWidth : currencyWidth);
        
        //  int priceAscent = fm.getAscent();
        int integersX = priceLength + 10;
        integersX = integersX < 130 ? 130 : integersX;
        int decimalsX = integersX + stringWidth ;

       // int cryptoNameStringWidth = fm.stringWidth(cryptoName);
       

        //int width = decimalsX + decsWidth + (padding * 2);
   
        //width = width < m_minImgWidth ? m_minImgWidth : width;

        int cryptoNameStringX = decimalsX + 2;

        //   g2d.setComposite(AlphaComposite.Clear);


        m_g2d.drawImage(m_unitImage,75, (height / 2) - (m_unitImage.getHeight() / 2), m_unitImage.getWidth(), m_unitImage.getHeight(), null);

       



        m_g2d.setFont(m_imgFont);
        fm = m_g2d.getFontMetrics();
        m_g2d.setColor(java.awt.Color.WHITE);

        

        m_g2d.drawString(amountString, integersX, fm.getAscent() + 5);

        m_g2d.setFont(m_imgSmallFont);
        fm = m_g2d.getFontMetrics();
        m_g2d.setColor(new java.awt.Color(.9f, .9f, .9f, .9f));

       
        if(decimalPlaces > 0){
            //decimalsX = widthIncrease > 0 ? decimalsX + widthIncrease : decimalsX;
            m_g2d.drawString(decs, decimalsX, fm.getHeight() + 4);
        }

        
        m_g2d.drawString(cryptoName, cryptoNameStringX, height - 10);

        m_g2d.setFont(m_imgSmallFont);
        m_g2d.setColor(java.awt.Color.WHITE);
        fm = m_g2d.getFontMetrics();
        m_g2d.drawString(totalPrice, padding, fm.getHeight() + 2);

        m_g2d.setColor(new java.awt.Color(.6f, .6f, .6f, .9f));
        m_g2d.drawString(currencyPrice, padding, height - 10);

     
        getImage().set(SwingFXUtils.toFXImage(m_img, getImage().get()));

  
    }*/

    private ChangeListener<BigDecimal> m_ergoAmountListener = null;

    public void setErgoAmountListener(ChangeListener<BigDecimal> ergoAmountListener){
        m_ergoAmountListener = ergoAmountListener;
        m_ergoAmount.amountProperty().addListener(m_ergoAmountListener);
    }

    public void removeErgoAmountListener(){
        if(m_ergoAmountListener != null){
            m_ergoAmount.amountProperty().removeListener(m_ergoAmountListener);
        }
    }

    
    


    public void getAddressInfo(){
        
        
            
        JsonObject json = getNetworksData().getData(m_addressString, m_addressesData.getWalletData().getNetworkId(), ErgoWallets.NETWORK_ID, ErgoNetwork.NETWORK_ID);
        
        if(json != null){
            
            openAddressJson(json);
            
        }
        
    }


 
  

    public ErgoTransaction[] getTxArray(JsonObject json){
        
        if(json != null){
            
            JsonElement itemsElement = json.get("items");

            if(itemsElement != null && itemsElement.isJsonArray()){
                JsonArray itemsArray = itemsElement.getAsJsonArray();
                int size = itemsArray.size();
                ErgoTransaction[] ergTxs = new ErgoTransaction[size];

                for(int i = 0; i < size ; i ++){
                    JsonElement txElement = itemsArray.get(i);
                    if(txElement != null && txElement.isJsonObject()){
                        JsonObject txObject = txElement.getAsJsonObject();

                        JsonElement txIdElement = txObject.get("id");
                        if(txIdElement != null && txIdElement.isJsonPrimitive()){
                            String txId = txIdElement.getAsString();
            
                            ergTxs[i] = new ErgoTransaction(txId, this, txObject);
                        }
                    }
                }
                return ergTxs;
            }

        }

        return new ErgoTransaction[0];
    }

    public void getErgoQuote(String interfaceId){
        
    }

  
    public void openAddressJson(JsonObject json){
        if(json != null){
         
            JsonElement txsElement = json.get("txs");
            
            if(txsElement != null && txsElement.isJsonArray()){
                openWatchedTxs(txsElement.getAsJsonArray());
            }
           
         
        }
    }

    public ErgoNetworkData getErgoNetworkData(){
        return m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData();
    }
    /*
    public void showSendStage(){
        //String titleString = getName() + " - " + m_address.toString() + " - (" + getNetworkType().toString() + ")";
        Stage sendStage = new Stage();
        sendStage.getIcons().add(m_addressesData.getWalletData().getErgoWallets().getAppIcon());
        sendStage.setResizable(false);
        sendStage.initStyle(StageStyle.UNDECORATED);
        

        Button closeBtn = new Button();

        addShutdownListener((obs, oldVal, newVal) -> {
            closeBtn.fire();
        });

        VBox layoutBox = new VBox();

        Scene sendScene = new Scene(layoutBox, Network.DEFAULT_STAGE_WIDTH, Network.DEFAULT_STAGE_HEIGHT); 
        sendScene.setFill(null);
        sendScene.getStylesheets().add("/css/startWindow.css");
        
    
        SimpleStringProperty babbleTokenId = new SimpleStringProperty(null);


        String stageName = "Send - " + m_addressString + " - (" + m_address.getNetworkType().toString() + ")";
        sendStage.setTitle(stageName);

      

        Button maximizeBtn = new Button();

        HBox titleBox = App.createTopBar(m_addressesData.getWalletData().getSmallAppIcon(), stageName, maximizeBtn, closeBtn, sendStage);
        maximizeBtn.setOnAction(e -> {
            sendStage.setMaximized(!sendStage.isMaximized());
        });
        Tooltip backTip = new Tooltip("Back");
        backTip.setShowDelay(new javafx.util.Duration(100));
        backTip.setFont(App.txtFont);


        double imageWidth = App.MENU_BAR_IMAGE_WIDTH;

        Tooltip nodesTip = new Tooltip("Select node");
        nodesTip.setShowDelay(new javafx.util.Duration(50));
        nodesTip.setFont(App.txtFont);

        BufferedMenuButton nodesBtn = new BufferedMenuButton("/assets/ergoNodes-30.png", imageWidth);
        nodesBtn.setPadding(new Insets(2, 0, 0, 0));
        nodesBtn.setTooltip(nodesTip);

        Tooltip explorerTip = new Tooltip("Select explorer");
        explorerTip.setShowDelay(new javafx.util.Duration(50));
        explorerTip.setFont(App.txtFont);

        BufferedMenuButton explorerBtn = new BufferedMenuButton("/assets/ergo-explorer-30.png", imageWidth);
        explorerBtn.setPadding(new Insets(2, 0, 0, 2));
        explorerBtn.setTooltip(explorerTip);

        SimpleObjectProperty<ErgoExplorerData> selectedExplorer = new SimpleObjectProperty<>(getErgoNetworkData().selectedExplorerData().get());

        Runnable updateExplorerBtn = () -> {
            ErgoExplorers ergoExplorers = getErgoNetworkData().getErgoExplorers();

            ErgoExplorerData explorerData = selectedExplorer.get();

            if (explorerData != null && ergoExplorers != null) {

                explorerTip.setText("Ergo Explorer: " + explorerData.getName());

            } else {

                if (ergoExplorers == null) {
                    explorerTip.setText("(install 'Ergo Explorer')");
                } else {
                    explorerTip.setText("Select Explorer...");
                }
            }

        };
        Button shutdownExplorerBtn = new Button();

   

        selectedExplorer.addListener((obs, oldval, newval) -> {
            updateExplorerBtn.run();
        });

        Tooltip marketsTip = new Tooltip("Select market");
        marketsTip.setShowDelay(new javafx.util.Duration(50));
        marketsTip.setFont(App.txtFont);

        BufferedMenuButton marketsBtn = new BufferedMenuButton("/assets/ergoChart-30.png", imageWidth);
        marketsBtn.setPadding(new Insets(2, 0, 0, 0));
        marketsBtn.setTooltip(marketsTip);

       

        Tooltip tokensTip = new Tooltip("Ergo Tokens");
        tokensTip.setShowDelay(new javafx.util.Duration(50));
        tokensTip.setFont(App.mainFont);

        BufferedMenuButton tokensBtn = new BufferedMenuButton(ErgoTokens.getSmallAppIconString(), imageWidth);
        tokensBtn.setPadding(new Insets(2, 0, 0, 0));
        

        Runnable updateTokensMenu = () -> {
            tokensBtn.getItems().clear();
   
            tokensBtn.setId("menuBtn");
            MenuItem tokensEnabledItem = new MenuItem("Enabled");
            tokensEnabledItem.setOnAction(e -> {
               // m_addressesData.ergoTokensProperty().set(ergoTokens);
            });

            MenuItem tokensDisabledItem = new MenuItem("Disabled");
            tokensDisabledItem.setOnAction(e -> {
              //  m_addressesData.ergoTokensProperty().set(null);
            });

        
         
            tokensBtn.getItems().addAll(tokensEnabledItem, tokensDisabledItem);
        

        };

       // m_addressesData.ergoTokensProperty().addListener((obs, oldval, newval) -> {
            //m_walletData.setIsErgoTokens(newval);
        //    updateTokensMenu.run();
       // });

        Region seperator1 = new Region();
        seperator1.setMinWidth(1);
        seperator1.setId("vSeperatorGradient");
        VBox.setVgrow(seperator1, Priority.ALWAYS);

        Region seperator2 = new Region();
        seperator2.setMinWidth(1);
        seperator2.setId("vSeperatorGradient");
        VBox.setVgrow(seperator2, Priority.ALWAYS);

        Region seperator3 = new Region();
        seperator3.setMinWidth(1);
        seperator3.setId("vSeperatorGradient");
        VBox.setVgrow(seperator3, Priority.ALWAYS);

        HBox rightSideMenu = new HBox(nodesBtn, seperator1, explorerBtn, seperator2, marketsBtn, seperator3, tokensBtn);
        rightSideMenu.setId("rightSideMenuBar");
        rightSideMenu.setPadding(new Insets(0, 0, 0, 0));
        rightSideMenu.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox menuBar = new HBox(spacer, rightSideMenu);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(1, 0, 1, 5));

        Text headingText = new Text("Send");
        headingText.setFont(App.txtFont);
        headingText.setFill(Color.WHITE);

        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(40);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 15, 10, 15));
        headingBox.setId("headingBox");


        Text fromText = new Text("From   ");
        fromText.setFont(App.txtFont);
        fromText.setFill(App.txtColor);

      

        MenuButton fromAddressBtn = new MenuButton();
        fromAddressBtn.setMaxHeight(40);
        fromAddressBtn.setContentDisplay(ContentDisplay.LEFT);
        fromAddressBtn.setAlignment(Pos.CENTER_LEFT);
        fromAddressBtn.setText(getButtonText());

        ImageView fromAdrImgView = new ImageView();
        fromAddressBtn.setGraphic(fromAdrImgView);

        Runnable updateImage = ()->{
            WritableImage img = getImage().get();
            if(img != null){
                fromAdrImgView.setImage(img);
            }
        };
        getImage().addListener((obs, oldval, newval) -> updateImage.run());
        updateImage.run();

        Text toText = new Text("To     ");
        toText.setFont(App.txtFont);
        toText.setFill(App.txtColor);

        AddressBox toAddressEnterBox = new AddressBox(new AddressInformation(""), sendScene, m_address.getNetworkType());
        toAddressEnterBox.setId("bodyRowBox");
        toAddressEnterBox.setMinHeight(50);

        HBox.setHgrow(toAddressEnterBox, Priority.ALWAYS);

        HBox toAddressBox = new HBox(toText, toAddressEnterBox);
        toAddressBox.setPadding(new Insets(0, 15, 10, 30));
        toAddressBox.setAlignment(Pos.CENTER_LEFT);

        HBox fromRowBox = new HBox(fromAddressBtn);
        HBox.setHgrow(fromRowBox, Priority.ALWAYS);
        fromRowBox.setAlignment(Pos.CENTER_LEFT);
        fromRowBox.setId("bodyRowBox");
        fromRowBox.setPadding(new Insets(0));

        HBox fromAddressBox = new HBox(fromText, fromRowBox);
        fromAddressBox.setPadding(new Insets(3, 15, 8, 30));

        HBox.setHgrow(fromAddressBox, Priority.ALWAYS);
        fromAddressBox.setAlignment(Pos.CENTER_LEFT);

        Button statusBoxBtn = new Button();
        statusBoxBtn.setId("bodyRowBox");
        statusBoxBtn.setPrefHeight(50);
        statusBoxBtn.setFont(App.txtFont);
        statusBoxBtn.setAlignment(Pos.CENTER_LEFT);
        statusBoxBtn.setPadding(new Insets(0));
       

        HBox nodeStatusBox = new HBox();
        nodeStatusBox.setId("bodyRowBox");
        nodeStatusBox.setPadding(new Insets(0, 0, 0, 0));
        nodeStatusBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nodeStatusBox, Priority.ALWAYS);
     

        SimpleObjectProperty<ErgoNodeData> selectedNode = new SimpleObjectProperty<>(null); // m_addressesData.selectedNodeData().get());
   
        Runnable updateNodeBtn = () -> {
            ErgoNodes ergoNodes =  getErgoNetworkData().getErgoNodes();
            ErgoNodeData nodeData = selectedNode.get();

            nodeStatusBox.getChildren().clear();

            if (nodeData != null && ergoNodes != null) {
                nodesTip.setText(nodeData.getName());
                HBox statusBox = nodeData.getStatusBox();

                nodeStatusBox.getChildren().add(statusBox);

                nodeStatusBox.setId("tokenBtn");
            } else {
                nodeStatusBox.setId(null);
                nodeStatusBox.getChildren().add(statusBoxBtn);
                statusBoxBtn.prefWidthProperty().bind(fromAddressBtn.widthProperty());
                if (ergoNodes == null) {
                    String statusBtnText = "Install Ergo Nodes";
                    nodesTip.setText(statusBtnText);
                    statusBoxBtn.setGraphic(IconButton.getIconView(new Image("/assets/selectNode.png"), 164));
                } else {
                    String statusBtnText = "Select node";
                    nodesTip.setText(statusBtnText);
                    statusBoxBtn.setGraphic(IconButton.getIconView(new Image("/assets/selectNode.png"), 164));
                }
            }

        };

        Runnable getAvailableNodeMenu = () -> {
            ErgoNodes ergoNodes =  getErgoNetworkData().getErgoNodes();
            if (ergoNodes != null) {
                ergoNodes.getErgoNodesList().getMenu(nodesBtn, selectedNode);
                nodesBtn.setId("menuBtn");
            } else {
                nodesBtn.getItems().clear();
                nodesBtn.setId("menuBtnDisabled");

            }
            updateNodeBtn.run();
        };

        selectedNode.addListener((obs, oldval, newval) -> {
            updateNodeBtn.run();
           // m_addressesData.getWalletData().setNodesId(newval == null ? null : newval.getId());
        });

    

   
        getAvailableNodeMenu.run();
     //  getAvailableMarketsMenu.run();
        updateTokensMenu.run();

        Text nodeText = new Text("Node   ");
        nodeText.setFont(App.txtFont);
        nodeText.setFill(App.txtColor);

        HBox nodeRowBox = new HBox(nodeText, nodeStatusBox);
        nodeRowBox.setPadding(new Insets(0, 15, 10, 30));
        nodeRowBox.setMinHeight(60);
        nodeRowBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nodeRowBox, Priority.ALWAYS);

        Text amountText = new Text("Amount ");
        amountText.setFont(App.txtFont);
        amountText.setFill(App.txtColor);

        //   BufferedButton addTokenBtn = new BufferedButton("/assets/add-outline-white-40.png", 15);
        Tooltip addTokenBtnTip = new Tooltip("Add Token");
        addTokenBtnTip.setShowDelay(new Duration(100));

        BufferedMenuButton addTokenBtn = new BufferedMenuButton("/assets/add-30.png", 20);
        addTokenBtn.setTooltip(addTokenBtnTip);

        Tooltip addAllTokenBtnTip = new Tooltip("Add All Tokens");
        addAllTokenBtnTip.setShowDelay(new Duration(100));

        BufferedButton addAllTokenBtn = new BufferedButton("/assets/add-all-30.png", 20);
        addAllTokenBtn.setTooltip(addAllTokenBtnTip);

        Tooltip removeTokenBtnTip = new Tooltip("Remove Token");
        removeTokenBtnTip.setShowDelay(new Duration(100));
        
        BufferedMenuButton removeTokenBtn = new BufferedMenuButton("/assets/remove-30.png", 20);
        removeTokenBtn.setTooltip(removeTokenBtnTip);

        Tooltip removeAllTokenBtnTip = new Tooltip("Remove All Tokens");
        removeAllTokenBtnTip.setShowDelay(new Duration(100));

        BufferedButton removeAllTokenBtn = new BufferedButton("/assets/remove-all-30.png", 20);
        removeAllTokenBtn.setTooltip(removeAllTokenBtnTip);

        HBox amountBoxesButtons = new HBox(addTokenBtn, addAllTokenBtn, removeTokenBtn, removeAllTokenBtn);
        amountBoxesButtons.setId("bodyBoxMenu");
        amountBoxesButtons.setPadding(new Insets(0, 5, 0, 5));
        amountBoxesButtons.setAlignment(Pos.BOTTOM_CENTER);

        VBox amountRightSideBox = new VBox(amountBoxesButtons);
        amountRightSideBox.setPadding(new Insets(0, 3, 0, 0));
        amountRightSideBox.setAlignment(Pos.BOTTOM_RIGHT);
        VBox.setVgrow(amountRightSideBox, Priority.ALWAYS);

        //    HBox.setHgrow(amountRightSideBox,Priority.ALWAYS);
        HBox amountTextBox = new HBox(amountText);
        amountTextBox.setAlignment(Pos.CENTER_LEFT);
        amountTextBox.setMinHeight(40);
        HBox.setHgrow(amountTextBox, Priority.ALWAYS);

        HBox amountBoxRow = new HBox(amountTextBox, amountRightSideBox);
        amountBoxRow.setPadding(new Insets(10, 20, 0, 30));

        amountBoxRow.setAlignment(Pos.BOTTOM_LEFT);
        HBox.setHgrow(amountBoxRow, Priority.ALWAYS);

        AmountSendBox ergoAmountBox = new AmountSendBox(m_ergoAmount,0, sendScene, true);
        ergoAmountBox.isFeeProperty().set(true);
        

        HBox.setHgrow(ergoAmountBox, Priority.ALWAYS);

        // addTokenBtn.setOnAction(e->addTokenBtn.show());
        AmountBoxes amountBoxes = new AmountBoxes();
        amountBoxes.setPadding(new Insets(10, 10, 10, 0));

        amountBoxes.setAlignment(Pos.TOP_LEFT);
        //   amountBoxes.setLastRowItem(addTokenBtn, AmountBoxes.ADD_AS_LAST_ROW);
        amountBoxes.setId("bodyBox");
        //  addTokenBtn.setAmountBoxes(amountBoxes);

        Runnable removeTokenMenuItems = ()->{
            addTokenBtn.getItems().forEach(item->((AmountMenuItem)item).shutdown());
            addTokenBtn.getItems().clear();
        };

  

        addTokenBtn.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            removeTokenMenuItems.run();
           
            long balanceTimestamp = System.currentTimeMillis();
            int size = getConfirmedTokenList().size();
            PriceAmount[] tokenArray = size > 0 ? new PriceAmount[size] : null;
            tokenArray = tokenArray != null ? getConfirmedTokenList().toArray(tokenArray) : null;
            if (tokenArray != null) {
                for (int i = 0; i < size; i++) {
                    PriceAmount tokenAmount = tokenArray[i];
                    String tokenId = tokenAmount.getTokenId();

                    if (tokenId != null) {
                        AmountBox isBox = (AmountBox) amountBoxes.getAmountBox(tokenId);

                        if (isBox == null) {
                            AmountMenuItem menuItem = new AmountMenuItem(tokenAmount);
                            addTokenBtn.getItems().add(menuItem);
                            menuItem.setOnAction(e1 -> {
                                PriceAmount menuItemPriceAmount = menuItem.getPriceAmount();
                           
                                AmountSendBox newAmountSendBox = new AmountSendBox(menuItemPriceAmount, balanceTimestamp, sendScene, true);
                                
                                amountBoxes.add(newAmountSendBox);
                            });
                        }

                    }
                }
            }
            
            if (addTokenBtn.getItems().size() == 0) {
                addTokenBtn.getItems().add(new MenuItem("No tokens to add"));
            }

        });

        addAllTokenBtn.setOnAction(e -> {
            
            List<PriceAmount> tokenList = getConfirmedTokenList();
            long timeStamp = System.currentTimeMillis();

            for (int i = 0; i < tokenList.size(); i++) {
                PriceAmount tokenAmount = tokenList.get(i);
                String tokenId = tokenAmount.getTokenId();
                AmountSendBox existingTokenBox = (AmountSendBox) amountBoxes.getAmountBox(tokenId);
                if (existingTokenBox == null) {
                   
                    AmountSendBox tokenAmountBox = new AmountSendBox(tokenAmount,timeStamp, sendScene, true);
              
                   
                    amountBoxes.add(tokenAmountBox);
                } else {
                    existingTokenBox.setTimeStamp(timeStamp);
                    
                }
            }
            
        });

        removeTokenBtn.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {

            removeTokenBtn.getItems().clear();
            int size = amountBoxes.amountsList().size();
            if (size > 0) {
                AmountBox[] boxArray = new AmountBox[size];
                boxArray = amountBoxes.amountsList().toArray(boxArray);

                for (int i = 0; i < size; i++) {
                    AmountBox tokenBox = boxArray[i];
                    PriceAmount tokenAmount = tokenBox.getPriceAmount();
                    AmountMenuItem removeAmountItem = new AmountMenuItem(tokenAmount);
                    removeAmountItem.setOnAction(e1 -> {
                        String tokenId = removeAmountItem.getTokenId();
                        amountBoxes.removeAmountBox(tokenId);
                    });
                    removeTokenBtn.getItems().add(removeAmountItem);
                }
            }
            if (removeTokenBtn.getItems().size() == 0) {
                removeTokenBtn.getItems().add(new MenuItem("No tokens to remove"));
            }
        });

        removeAllTokenBtn.setOnAction(e -> {
            amountBoxes.clear();
        });

        Runnable updateBabbleFees = () -> {
            String babbleId = babbleTokenId.get();

            if (babbleId == null) {
                ergoAmountBox.isFeeProperty().set(true);
            }
        };

        updateBabbleFees.run();

        babbleTokenId.addListener((obs, oldval, newval) -> updateBabbleFees.run());


        //  addTokenBtn.prefWidthProperty().bind(amountBoxes.widthProperty());
        Region sendBoxSpacer = new Region();
        HBox.setHgrow(sendBoxSpacer, Priority.ALWAYS);

        Runnable checkAndSend = () -> {

            if (ergoAmountBox.isNotAvailable()) {
                String insufficentErrorString = "Balance: " + m_ergoAmount.toString() + "\nAmount: " + ergoAmountBox.getSendAmount() + (ergoAmountBox.isFeeProperty().get() ? ("\nSend fee: " + ergoAmountBox.feeAmountProperty().get().toString()) : "");
                String insufficentTitleString = "Insuficient " + m_ergoAmount.getCurrency().getDefaultName();
                Alert a = new Alert(AlertType.NONE, insufficentErrorString, ButtonType.CANCEL);
                a.setTitle(insufficentTitleString);
                a.initOwner(sendStage);
                a.setHeaderText(insufficentTitleString);
                a.show();
                return;
            }

            AddressInformation addressInformation = toAddressEnterBox.addressInformationProperty().get();

            if (addressInformation != null && addressInformation.getAddress() != null) {
                ErgoNodeData ergoNodeData = selectedNode.get();
                
                ErgoExplorerData ergoExplorerData = null;//m_addressesData.selectedExplorerData().get();
                AmountBoxInterface[] amountBoxArray = amountBoxes.getAmountBoxArray();

                int amountOfTokens = amountBoxArray != null && amountBoxArray.length > 0 ? amountBoxArray.length : 0;

                AmountSendBox[] tokenArray = amountOfTokens > 0 ? new AmountSendBox[amountOfTokens] : null;

                if (amountOfTokens > 0 && amountBoxArray != null && tokenArray != null) {
                    for (int i = 0; i < amountOfTokens; i++) {
                        AmountBoxInterface box = amountBoxArray[i];
                        if (box != null && box instanceof AmountSendBox) {
                            AmountSendBox sendBox = (AmountSendBox) box;
                            if (sendBox.isNotAvailable()) {

                                BigDecimal sendAmount = sendBox.getSendAmount();
                                PriceAmount balance = sendBox.getBalanceAmount();

                                String insufficentErrorString = "Balance: " + sendAmount.toString() + "\nAmount: " + sendAmount + (sendBox.isFeeProperty().get() ? ("\nSend fee: " + sendBox.feeAmountProperty().get().toString()) : "");
                                String insufficentTitleString = "Insuficient " + balance.getCurrency().getDefaultName();
               
                                Alert a = new Alert(AlertType.NONE, insufficentErrorString, ButtonType.CANCEL);
                                a.setTitle(insufficentTitleString);
                                a.initOwner(sendStage);
                                a.setHeaderText(insufficentTitleString);
                                a.show();
                                return;
                            } else {

                                tokenArray[i] = sendBox;
                                 
                            }
                        }
                    }
               

                }
               
                
                BigDecimal feeAmount = ergoAmountBox.feeAmountProperty().get();
               
                showTxConfirmScene(this, addressInformation, ergoNodeData, ergoExplorerData,ergoAmountBox, tokenArray,feeAmount,sendScene, sendStage, () -> closeBtn.fire(), () -> closeBtn.fire());

            } else {

                Alert a = new Alert(AlertType.NONE, "Enter a valid address.", ButtonType.CANCEL);
                a.setTitle("Invalid Receiver Address");
                a.initOwner(sendStage);
                a.setHeaderText("Invalid Receiver Address");
                a.show();

                return;

            }

        };

        BufferedButton sendBtn = new BufferedButton("Send", "/assets/arrow-send-white-30.png", 30);
        sendBtn.setFont(App.txtFont);
        sendBtn.setId("toolBtn");
        sendBtn.setUserData("sendButton");
        sendBtn.setContentDisplay(ContentDisplay.LEFT);
        sendBtn.setPadding(new Insets(5, 10, 3, 5));
        sendBtn.setOnAction(e -> {
            ErgoNodeData ergoNodeData = selectedNode.get();
            if (ergoNodeData != null) {
                if (ergoNodeData instanceof ErgoNodeLocalData) {
                    ErgoNodeLocalData localErgoNode = (ErgoNodeLocalData) ergoNodeData;
                    if (localErgoNode.isSetupProperty().get()) {
                        if (localErgoNode.syncedProperty().get()) {
                            checkAndSend.run();
                        } else {
                            long nodeBlockHeight = localErgoNode.nodeBlockHeightProperty().get();
                            long networkBlockHeight = localErgoNode.networkBlockHeightProperty().get();
                            double percentage = nodeBlockHeight != -1 && networkBlockHeight != -1 ? (nodeBlockHeight / networkBlockHeight) * 100 : -1;
                            String msgPercent = percentage != -1 ? String.format("%.2f", percentage) + "%" : "Starting";

                            Alert a = new Alert(AlertType.NONE, "Sync status: " + msgPercent, ButtonType.CANCEL);
                            a.setTitle("Node Sync Required");
                            a.initOwner(sendStage);
                            a.setHeaderText("Node Sync Required");
                            a.show();
                        }
                    } else {
                        localErgoNode.setup();
                    }
                } else {
                    checkAndSend.run();
                }
            } else {
                nodesBtn.show();
            }
            
        });

        HBox sendBox = new HBox(sendBtn);
        VBox.setVgrow(sendBox, Priority.ALWAYS);
        sendBox.setPadding(new Insets(0, 0, 8, 15));
        sendBox.setAlignment(Pos.CENTER_RIGHT);

        HBox ergoAmountPaddingBox = new HBox(ergoAmountBox);
        ergoAmountPaddingBox.setId("bodyBox");
        ergoAmountPaddingBox.setPadding(new Insets(10, 10, 0, 10));

        VBox scrollPaneContentVBox = new VBox(ergoAmountPaddingBox, amountBoxes);

        ScrollPane scrollPane = new ScrollPane(scrollPaneContentVBox);
        scrollPane.setPadding(new Insets(0, 0, 0, 20));

        VBox scrollPaddingBox = new VBox(scrollPane);
        HBox.setHgrow(scrollPaddingBox, Priority.ALWAYS);
        scrollPaddingBox.setPadding(new Insets(0, 5, 0, 5));

        VBox bodyBox = new VBox(fromAddressBox, toAddressBox, nodeRowBox, amountBoxRow, scrollPaddingBox);
        VBox.setVgrow(bodyBox, Priority.ALWAYS);
        bodyBox.setId("bodyBox");
        bodyBox.setPadding(new Insets(15, 0, 0, 0));

        VBox bodyLayoutBox = new VBox(headingBox, bodyBox);
        VBox.setVgrow(bodyLayoutBox, Priority.ALWAYS);
        bodyLayoutBox.setPadding(new Insets(0, 4, 4, 4));

        HBox footerBox = new HBox(sendBox);
        HBox.setHgrow(footerBox, Priority.ALWAYS);
        footerBox.setPadding(new Insets(5, 30, 0, 5));
        footerBox.setAlignment(Pos.CENTER_RIGHT);

        HBox paddingBox = new HBox(menuBar);
        HBox.setHgrow(paddingBox, Priority.ALWAYS);
        paddingBox.setPadding(new Insets(0, 4, 4, 4));

        layoutBox.getChildren().addAll(titleBox, paddingBox, bodyLayoutBox, footerBox);
        VBox.setVgrow(layoutBox, Priority.ALWAYS);
        layoutBox.setAlignment(Pos.TOP_LEFT);

        fromAddressBtn.prefWidthProperty().bind(fromAddressBox.widthProperty().subtract(fromText.layoutBoundsProperty().getValue().getWidth()).subtract(30));

        scrollPane.prefViewportHeightProperty().bind(layoutBox.heightProperty().subtract(20).subtract(titleBox.heightProperty()).subtract(paddingBox.heightProperty()).subtract(headingBox.heightProperty()).subtract(fromAddressBox.heightProperty()).subtract(toAddressBox.heightProperty()).subtract(nodeRowBox.heightProperty()).subtract(amountBoxRow.heightProperty()).subtract(footerBox.heightProperty()).subtract(15));
        amountBoxes.minHeightProperty().bind(scrollPane.prefViewportHeightProperty().subtract(20).subtract(ergoAmountPaddingBox.heightProperty()));
        scrollPane.prefViewportWidthProperty().bind(sendScene.widthProperty().subtract(60));
        amountBoxes.prefWidthProperty().bind(sendScene.widthProperty().subtract(60));
        ergoAmountPaddingBox.prefWidthProperty().bind(sendScene.widthProperty().subtract(60));

        sendStage.setScene(sendScene);
        sendStage.show();

        closeBtn.setOnAction(e->{
            ergoAmountBox.shutdown();
            amountBoxes.shutdown();
            scrollPane.prefHeightProperty().unbind();
            scrollPane.prefWidthProperty().unbind();
            amountBoxes.minHeightProperty().unbind();
            ergoAmountPaddingBox.prefWidthProperty().unbind();
            shutdownExplorerBtn.fire();
            sendStage.close();
           
        });

        maximizeBtn.setOnAction(e->{
            sendStage.setMaximized(!sendStage.isMaximized());
        });


        ResizeHelper.addResizeListener(sendStage, 200, 250, Double.MAX_VALUE, Double.MAX_VALUE);

    }*/


    public void openWatchedTxs(JsonArray txsJsonArray){
        if(txsJsonArray != null){
       
            int size = txsJsonArray.size();

            for(int i = 0; i<size ; i ++){
                JsonElement txElement = txsJsonArray.get(i);

                if(txElement != null && txElement.isJsonObject()){
                    JsonObject txJson = txElement.getAsJsonObject();
                                                
                        JsonElement txIdElement = txJson.get("txId");
                        JsonElement parentAdrElement = txJson.get("parentAddress");
                        JsonElement timeStampElement = txJson.get("timeStamp");
                        JsonElement txTypeElement = txJson.get("txType");
                       // JsonElement nodeUrlElement = txJson.get("nodeUrl");

                        if(txIdElement != null && txIdElement.isJsonPrimitive() 
                            && parentAdrElement != null && parentAdrElement.isJsonPrimitive() 
                            && timeStampElement != null && timeStampElement.isJsonPrimitive() 
                            && txTypeElement != null && txTypeElement.isJsonPrimitive()){

                            String txId = txIdElement.getAsString();
                            String txType = txTypeElement.getAsString();
                            String parentAdr = parentAdrElement.getAsString();

                            

                            if(parentAdr.equals(getAddressString())){
                                switch(txType){
                                    case TransactionType.SEND:
                                        
                                        try {
                                            ErgoSimpleSendTx simpleSendTx = new ErgoSimpleSendTx(txId, this, txJson);
                                            addWatchedTransaction(simpleSendTx, false);
                                        } catch (Exception e) {
                                            try {
                                                Files.writeString(App.logFile.toPath(), "\nCould not read tx json: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                            } catch (IOException e1) {
                                                
                                            }
                                        }
                                      
                                        break;
                                    default:
                                        ErgoTransaction ergTx = new ErgoTransaction(txId, this, txType);
                                        
                                        addWatchedTransaction(ergTx, false);
                                }
                            }
                        }
                    }
                }
            
        }
    }

    

    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.add("txs", getWatchedTxJsonArray());

        return json;
    }




    public void addWatchedTransaction(ErgoTransaction transaction){
        addWatchedTransaction(transaction, true);
    }

    public void addWatchedTransaction(ErgoTransaction transaction, boolean save){
        if(getWatchedTx(transaction.getTxId()) == null){
            m_watchedTransactions.add(transaction);
            transaction.addUpdateListener((obs,oldval,newval)->{
            
                saveAddresInfo();
            });
            if(save){
                saveAddresInfo();
            }
        }
    }

   

    public JsonArray getWatchedTxJsonArray(){
        ErgoTransaction[] ergoTransactions =  getWatchedTxArray();

        JsonArray jsonArray = new JsonArray();
   
        for(int i = 0; i < ergoTransactions.length; i++){
            JsonObject tokenJson = ergoTransactions[i].getJsonObject();
            jsonArray.add(tokenJson);
        }
 
        return jsonArray; 
    }

    public ErgoTransaction getWatchedTx(String txId){
        if(txId != null){
            ErgoTransaction[] txs =  getWatchedTxArray();
            
            for(int i = 0; i < txs.length; i++){
                ErgoTransaction tx = txs[i];
                if(txId.equals(tx.getTxId())){
                    return tx;
                }
            }
        }
        return null;
    }



    public ObservableList<ErgoTransaction> watchedTxList(){
        return m_watchedTransactions;
    }

    public SimpleObjectProperty<ErgoTransaction> selectedTransaction(){
        return m_selectedTransaction;
    }

    public String getButtonText() {
        return "  " + getName() + "\n   " + getAddressString();
    }

    /*public boolean donate(){
        BigDecimal amountFullErg = dialog.showForResult().orElse(null);
		if (amountFullErg == null) return;
		try {
			Wallet wallet = Main.get().getWallet();
			UnsignedTransaction unsignedTx = ErgoInterface.createUnsignedTransaction(Utils.createErgoClient(),
					wallet.addressStream().toList(),
					DONATION_ADDRESS, ErgoInterface.toNanoErg(amountFullErg), Parameters.MinFee, Main.get().getWallet().publicAddress(0));
			String txId = wallet.transact(Utils.createErgoClient().execute(ctx -> {
				try {
					return wallet.key().sign(ctx, unsignedTx, wallet.myAddresses.keySet());
				} catch (WalletKey.Failure ex) {
					return null;
				}
			}));
			if (txId != null) Utils.textDialogWithCopy(Main.lang("transactionId"), txId);
		} catch (WalletKey.Failure ignored) {
			// user already informed
		}
    }*/
 /* return ;
        }); */
    public String getNodesId() {
        return "";
    }

 

     
    
  
    public ErgoTransaction[] getReverseTxArray(){
        ArrayList<ErgoTransaction> list = new ArrayList<>(m_watchedTransactions);
        Collections.reverse(list);

        int size = list.size();
        ErgoTransaction[] txArray = new ErgoTransaction[size];
        txArray = list.toArray(txArray);
        return txArray;
    }

    public ErgoTransaction[] getWatchedTxArray(){
        int size = m_watchedTransactions.size();
        ErgoTransaction[] txArray = new ErgoTransaction[size];
        txArray = m_watchedTransactions.toArray(txArray);
        return txArray;
    }

    public void removeTransaction(String txId){
        removeTransaction(txId, true);
    }

     public void removeTransaction(String txId, boolean save){
        
        ErgoTransaction ergTx = getWatchedTx(txId);
        if(ergTx != null){
            m_watchedTransactions.remove(ergTx);
        }
        if(save){
            saveAddresInfo();
        }
    }

    

    public AddressesData getAddressesData(){
        return m_addressesData;
    }


   

    public void showTxConfirmScene(AddressData addressData, AddressInformation receiverAddressInformation, ErgoNodeData nodeData, ErgoExplorerData explorerData, AmountSendBox ergoSendBox, AmountSendBox[] tokenAmounts, BigDecimal feeAmount, Scene parentScene, Stage parentStage, Runnable parentClose, Runnable complete) {
        if (nodeData == null) {
            Alert a = new Alert(AlertType.NONE, "Please select a node in order to continue.", ButtonType.CANCEL);
            a.setTitle("Invalid Node");
            a.initOwner(parentStage);
            a.setHeaderText("Invalid Node");
            a.show();
            return;
        }

        final String explorerUrl = explorerData != null ? explorerData.ergoNetworkUrlProperty().get().getUrlString() : null;

        

        String oldStageName = parentStage.getTitle();
        String title = "Confirmation - Send - " + m_addressString + "(" + m_address.getNetworkType().toString() + ")";
        Button maximizeBtn = new Button();
        Button closeBtn = new Button();

        closeBtn.setOnAction(e -> {
            parentClose.run();
        });

        HBox titleBox = App.createTopBar(m_addressesData.getWalletData().getSmallAppIcon(), maximizeBtn, closeBtn, parentStage);
        maximizeBtn.setOnAction(e -> {
            parentStage.setMaximized(!parentStage.isMaximized());
        });

        Tooltip backTip = new Tooltip("Back");
        backTip.setShowDelay(new javafx.util.Duration(100));
        backTip.setFont(App.txtFont);

        BufferedButton backButton = new BufferedButton("/assets/return-back-up-30.png", App.MENU_BAR_IMAGE_WIDTH);
        backButton.setId("menuBtn");
        backButton.setTooltip(backTip);
        

        HBox menuBar = new HBox(backButton);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(1, 0, 1, 5));

        VBox menuPaddingBox = new VBox(menuBar);
        menuPaddingBox.setPadding(new Insets(0, 5, 0, 5));

        final Address address = addressData.getAddress();
        final String addressName = addressData.getName();

        Text addressText = new Text("From: ");
        addressText.setFill(App.txtColor);
        addressText.setFont(App.txtFont);

        TextField addressField = new TextField(address.toString() + " (" + addressName + ")");
        addressField.setId("addressField");
        addressField.setEditable(false);
        HBox.setHgrow(addressField, Priority.ALWAYS);

        HBox addressBox = new HBox(addressText, addressField);
        HBox.setHgrow(addressBox, Priority.ALWAYS);
        addressBox.setAlignment(Pos.CENTER_LEFT);
        addressBox.setPrefHeight(30);

        Text receiverText = new Text("To:   ");
        receiverText.setFill(App.txtColor);
        receiverText.setFont(App.txtFont);

        final Address receiverAddress = receiverAddressInformation.getAddress();
        TextField receiverField = new TextField(receiverAddress.toString());
        receiverField.setId("addressField");
        receiverField.setEditable(false);
        HBox.setHgrow(receiverField, Priority.ALWAYS);

        HBox receiverBox = new HBox(receiverText, receiverField);
        HBox.setHgrow(receiverBox, Priority.ALWAYS);
        receiverBox.setAlignment(Pos.CENTER_LEFT);
        receiverBox.setPrefHeight(30);

        Text nodeText = new Text("Node: ");
        nodeText.setFill(App.txtColor);
        nodeText.setFont(App.txtFont);

        final NamedNodeUrl namedNodeUrl = nodeData.namedNodeUrlProperty().get();
        final String nodeUrl = namedNodeUrl.getUrlString();
   
        TextField nodeField = new TextField(namedNodeUrl.getName() + " (" + nodeUrl + ")");
        nodeField.setId("addressField");
        nodeField.setEditable(false);
        HBox.setHgrow(nodeField, Priority.ALWAYS);

        HBox nodeBox = new HBox(nodeText, nodeField);
        HBox.setHgrow(nodeBox, Priority.ALWAYS);
        nodeBox.setAlignment(Pos.CENTER_LEFT);
        nodeBox.setPrefHeight(30);

        VBox layoutBox = new VBox();
        Scene confirmTxScene = new Scene(layoutBox, 600, 500);
        confirmTxScene.setFill(null);
        confirmTxScene.getStylesheets().add("/css/startWindow.css");
        parentStage.setScene(confirmTxScene);
        parentStage.setTitle(title);
        
        AmountConfirmBox ergoAmountBox = new AmountConfirmBox(ergoSendBox.getSendAmount(), feeAmount, new ErgoCurrency(NetworkType.MAINNET), confirmTxScene);
        HBox.setHgrow(ergoAmountBox, Priority.ALWAYS);
        
        PriceAmount ergoPriceAmount = ergoAmountBox.getConfirmPriceAmount();
        PriceAmount feePriceAmount = ergoAmountBox.getFeePriceAmount();

        final long ergoAmountLong = ergoPriceAmount.getLongAmount();
        final long feeAmountLong = feePriceAmount.getLongAmount();
        

        HBox amountBoxPadding = new HBox(ergoAmountBox);
        amountBoxPadding.setPadding(new Insets(10, 10, 0, 10));

        AmountBoxes amountBoxes = new AmountBoxes();
        amountBoxes.setPadding(new Insets(5, 10, 5, 0));
        amountBoxes.setAlignment(Pos.TOP_LEFT);


        if (tokenAmounts != null && tokenAmounts.length > 0) {
            int numTokens = tokenAmounts.length;
            for (int i = 0; i < numTokens; i++) {
                
                AmountSendBox sendBox = tokenAmounts[i];
                BigDecimal sendAmount = sendBox.getSendAmount();
                PriceAmount balance = sendBox.getBalanceAmount();
                PriceCurrency priceCurrency = balance.getCurrency();

                if (sendAmount.compareTo(BigDecimal.ZERO) == -1 ) {


                    AmountConfirmBox confirmBox = new AmountConfirmBox(sendAmount,sendBox.isFeeProperty().get() ? sendBox.feeAmountProperty().get() : null,priceCurrency, confirmTxScene );
                    amountBoxes.add(confirmBox);
                }
            }
        }

        backButton.setOnAction(e -> {
            amountBoxes.shutdown();
            parentStage.setScene(parentScene);
            parentStage.setTitle(oldStageName);
        });
     

        VBox infoBox = new VBox(nodeBox, addressBox, receiverBox);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        infoBox.setPadding(new Insets(10, 15, 10, 15));

        VBox paddingAmountBox = new VBox(amountBoxPadding);
        paddingAmountBox.setPadding(new Insets(5, 16, 0, 0));

        VBox boxesVBox = new VBox(amountBoxPadding, amountBoxes);
        HBox.setHgrow(boxesVBox, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(boxesVBox);
        scrollPane.setPadding(new Insets(0, 0, 5, 0));

        HBox infoBoxPadding = new HBox(infoBox);
        HBox.setHgrow(infoBoxPadding, Priority.ALWAYS);
        infoBoxPadding.setPadding(new Insets(0, 4, 0, 4));

        VBox bodyBox = new VBox(infoBoxPadding, paddingAmountBox, scrollPane);

        bodyBox.setPadding(new Insets(0, 0, 0, 15));
        HBox.setHgrow(bodyBox, Priority.ALWAYS);

        VBox bodyPaddingBox = new VBox(bodyBox);
        bodyPaddingBox.setPadding(new Insets(4, 4, 0, 4));
        HBox.setHgrow(bodyPaddingBox, Priority.ALWAYS);

        Text confirmText = new Text("Notice");
        confirmText.setFill(App.txtColor);
        confirmText.setFont(App.txtFont);

        HBox confirmTextBox = new HBox(confirmText);

        TextArea confirmNotice = new TextArea("All transactions are considered final and cannot be reversed. Please verify the transaction and the receiving party.");
        HBox.setHgrow(confirmNotice, Priority.ALWAYS);
        confirmNotice.setWrapText(true);
        confirmNotice.setPrefRowCount(2);

        HBox confirmNoticeBox = new HBox(confirmNotice);
        HBox.setHgrow(confirmNoticeBox, Priority.ALWAYS);
        confirmNoticeBox.setPadding(new Insets(5, 15, 0, 15));

        VBox confirmTextVBox = new VBox(confirmTextBox, confirmNoticeBox);
        confirmTextVBox.setPadding(new Insets(0, 15, 0, 15));

        Text passwordTxt = new Text("> Enter wallet password:");
        passwordTxt.setFill(App.txtColor);
        passwordTxt.setFont(App.txtFont);

        PasswordField passwordField = new PasswordField();
        passwordField.setFont(App.txtFont);
        passwordField.setId("passField");
        HBox.setHgrow(passwordField, Priority.ALWAYS);
        passwordField.setOnAction(e -> {
            Stage statusStage = App.getStatusStage("Verifying - Netnotes", "Verifying...");
            if (passwordField.getText().length() < 6) {
                passwordField.setText("");
            } else {

                statusStage.show();
                passwordField.setEditable(false);

                AmountBoxInterface[] amountBoxArray = amountBoxes.getAmountBoxArray();

                int amountOfTokens = amountBoxArray != null && amountBoxArray.length > 0 ? amountBoxArray.length : 0;
                final ErgoToken[] tokenArray = new ErgoToken[amountOfTokens];

                if (amountOfTokens > 0 && amountBoxArray != null && tokenArray != null) {
                    for (int i = 0; i < amountOfTokens; i++) {
                        AmountBoxInterface box = amountBoxArray[i];
                        if (box != null && box instanceof AmountConfirmBox) {
                            AmountConfirmBox confirmBox = (AmountConfirmBox) box;
                            PriceAmount confirmedPriceAmount = confirmBox.getConfirmPriceAmount();
                            tokenArray[i] = new ErgoToken(confirmedPriceAmount.getCurrency().getTokenId(), confirmedPriceAmount.getLongAmount());
                        }
                    }

                }

                Task<String> task = new Task<String>() {
                    @Override
                    public String call() throws Exception {

                        ErgoClient ergoClient = RestApiErgoClient.create(nodeUrl, m_address.getNetworkType(),  namedNodeUrl.getApiKey(), explorerUrl);
                    
                        UnsignedTransaction unsignedTx = ErgoInterface.createUnsignedTransaction(
                            ergoClient,
                            m_wallet.addressStream(m_address.getNetworkType()).toList(),
                            receiverAddress,
                            ergoAmountLong,
                            feeAmountLong,
                            address,
                            tokenArray
                        );
                       
                        String txId = m_wallet.transact(ergoClient, ergoClient.execute(ctx -> {
                            try {
                                return m_wallet.key().signWithPassword(passwordField.getText(), ctx, unsignedTx, m_wallet.myAddresses.keySet());
                            } catch (WalletKey.Failure ex) {

                                return null;
                            }
                        }));

                        return txId;
                    }
                };
                task.setOnFailed((onFailed) -> {
                    statusStage.close();
                    passwordField.setEditable(true);
                    passwordField.setText("");

                    Throwable throwable = onFailed.getSource().getException();

                    if (throwable instanceof InputBoxesSelectionException) {
                        Alert a = new Alert(AlertType.NONE, "Insuficient Funds", ButtonType.CANCEL);
                        a.setTitle("Transaction cancelled.");
                        a.initOwner(parentStage);
                        a.setHeaderText("Insuficient Funds");
                        a.show();
                    } else {
                        Alert a = new Alert(AlertType.NONE, "Error: " + throwable.toString(), ButtonType.CANCEL);
                        a.setTitle("Error - Transaction Cancelled");
                        a.initOwner(parentStage);
                        a.setHeaderText("Transaction Cancelled");
                        a.show();
                    }
                    backButton.fire();
                });

                task.setOnSucceeded((onSucceded) -> {
                    statusStage.close();
                    passwordField.setEditable(true);
                    passwordField.setText("");

                    Object sourceValue = onSucceded.getSource().getValue();

                    if (sourceValue != null && sourceValue instanceof String) {
                        String txId = (String) sourceValue;

                        PriceAmount[] tokens = new PriceAmount[amountOfTokens];
                        if(amountBoxArray != null){
                            for(int i = 0; i< amountOfTokens ; i++){
                                AmountBoxInterface box = amountBoxArray[i];
                                
                                if (box != null && box instanceof AmountConfirmBox) {
                                
                                    AmountConfirmBox confirmBox = (AmountConfirmBox) box;

                                    tokens[i] = confirmBox.getConfirmPriceAmount();
                                }
                            }  
                        }

                        try{

                            ErgoSimpleSendTx ergTx = new ErgoSimpleSendTx(txId, addressData, receiverAddressInformation, ergoAmountLong, new PriceAmount( feeAmount, new ErgoCurrency(NetworkType.MAINNET)), tokens, nodeUrl, explorerUrl,TransactionStatus.PENDING, System.currentTimeMillis());
                            addressData.addWatchedTransaction(ergTx);
                          

                        }catch(Exception txCreateEx){
                       
                           
                        }
                        
                        complete.run();

                    }else{
                        Alert a = new Alert(AlertType.NONE, "Could not complete transaction.", ButtonType.CANCEL);
                        a.setTitle("Error - Transaction Cancelled");
                        a.initOwner(parentStage);
                        a.setHeaderText("Transaction Cancelled");
                        a.show();

                        backButton.fire();
                    }
                });

                Thread t = new Thread(task);
                t.setDaemon(true);
                t.start();

            }

        });

        Platform.runLater(() -> passwordField.requestFocus());

        HBox passwordBox = new HBox(passwordTxt, passwordField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        passwordBox.setPadding(new Insets(20, 0, 15, 15));

        VBox footerBox = new VBox(confirmTextVBox, passwordBox);
        HBox.setHgrow(footerBox, Priority.ALWAYS);
        footerBox.setPadding(new Insets(5, 15, 0, 4));
        footerBox.setAlignment(Pos.CENTER_RIGHT);

        layoutBox.getChildren().addAll(titleBox, menuPaddingBox, infoBoxPadding, bodyPaddingBox, footerBox);

        scrollPane.prefViewportWidthProperty().bind(confirmTxScene.widthProperty().subtract(60));
        scrollPane.prefViewportHeightProperty().bind(parentStage.heightProperty().subtract(titleBox.heightProperty()).subtract(menuPaddingBox.heightProperty()).subtract(infoBox.heightProperty()).subtract(footerBox.heightProperty()).subtract(10));
        amountBoxes.minHeightProperty().bind(scrollPane.prefViewportHeightProperty().subtract(60));
        amountBoxes.prefWidthProperty().bind(confirmTxScene.widthProperty().subtract(60));
        

        

        ResizeHelper.addResizeListener(parentStage, 200, 250, Double.MAX_VALUE, Double.MAX_VALUE);

    }
 

    public int getIndex() {
        return m_index;
    }


    public Address getAddress() {
        return m_address;
    }

    public String getAddressString() {
        return m_addressString;
    }

    public String getAddressMinimal(int show) {
        String adr = m_address.toString();
        int len = adr.length();

        return (show * 2) > len ? adr : adr.substring(0, show) + "..." + adr.substring(len - show, len);
    }

    public BigDecimal getConfirmedAmount() {
        return ErgoInterface.toFullErg(getConfirmedNanoErgs());
    }

    public NetworkType getNetworkType() {
        return m_address.getNetworkType();
    }

    public ErgoAmount getErgoAmount(){
        return m_ergoAmount;
    }

    public long getConfirmedNanoErgs() {
        return m_ergoAmount.getLongAmount();
    }

    
    public PriceAmount getUnconfirmedErgoAmount() {
        return m_unconfirmedAmount;
    }

    public ObservableList<PriceAmount> getConfirmedTokenList() {
        return m_confirmedTokensList;
    }


    
    public BigDecimal getTotalTokenErgBigDecimal(){
        int tokenListSize = m_confirmedTokensList.size();
       
        SimpleObjectProperty<BigDecimal> total = new SimpleObjectProperty<>(BigDecimal.ZERO);

        PriceAmount[] tokenAmounts = new PriceAmount[tokenListSize];
        tokenAmounts = m_confirmedTokensList.toArray(tokenAmounts);

        for(int i = 0; i < tokenListSize ; i++){
            PriceAmount priceAmount = tokenAmounts[i];
          
            PriceQuote priceQuote =  priceAmount.priceQuoteProperty().get();
            
            BigDecimal priceBigDecimal = priceQuote != null ? priceQuote.getInvertedAmount() : null;

            BigDecimal amountBigDecimal = priceQuote != null ? priceAmount.amountProperty().get() : null;
            BigDecimal tokenErgs = priceBigDecimal != null && amountBigDecimal != null ? priceBigDecimal.multiply(amountBigDecimal) : BigDecimal.ZERO;
        
        
            total.set(total.get().add(tokenErgs));
            
        }
 
        return total.get();
    }

    public ArrayList<PriceAmount> getUnconfirmedTokenList() {
        return m_unconfirmedTokensList;
    }

  

    public BigDecimal getFullAmountUnconfirmed() {
        return m_unconfirmedAmount.amountProperty().get();
    }

    

    public BigDecimal getTotalAmountPrice() {
        return getErgoAmount().amountProperty().get().multiply( m_addressesData.getPrice());
    }



   

    public BigInteger getAmountInt() {
        return  getErgoAmount().amountProperty().get().toBigInteger();
    }

    public BigDecimal getAmountDecimalPosition() {
        return getErgoAmount().amountProperty().get().subtract(new BigDecimal(getAmountInt()));
    }

    public Image getUnitImage() {
   
        if (m_ergoAmount == null) {
            return new Image("/assets/unknown-unit.png");
        } else {
            return m_ergoAmount.getCurrency().getIcon();
        }
    }

 
 

   

    public SimpleObjectProperty<WritableImage> getImage() {
        return m_imgBuffer;
    }



    public PriceAmount getConfirmedTokenAmount(String tokenId){
        
        for(int i = 0; i < m_confirmedTokensList.size(); i++)
        {
            PriceAmount priceAmount = m_confirmedTokensList.get(i);

            if(priceAmount.getTokenId().equals(tokenId)){
                return priceAmount;
            }
        }
        return null;
    }

    public void updateBalance() {
        

        NoteInterface explorerInterface = getErgoNetworkData().selectedExplorerData().get();
        if(explorerInterface != null){
            
            JsonObject note = Utils.getCmdObject("getBalance");
            note.addProperty("networkId", m_addressesData.getErgoNetworkdata().getId());
            note.addProperty("address", m_addressString);

            explorerInterface.sendNote(note,
                success -> {
                    Object sourceObject = success.getSource().getValue();

                    if (sourceObject != null) {
                        JsonObject jsonObject = (JsonObject) sourceObject;
                
                        
                        setBalance(jsonObject); 
                        
                    }},
                    failed -> {
                            try {
                                Files.writeString(App.logFile.toPath(), "\nAddressData, Explorer failed update: " + failed.getSource().getException().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e) {
                                
                                
                            }
            
                    }
                , null);
                
        

        }
        
    }
    /*
    public void updateTransactions(ErgoExplorerData explorerData){
        
        try {
            Files.writeString(logFile.toPath(), "updateTxOffset: " + m_updateTxOffset, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
    
        }

        explorerData.getAddressTransactions(m_addressString, (m_updateTxOffset * UPDATE_LIMIT), UPDATE_LIMIT,
            success -> {
                Object sourceObject = success.getSource().getValue();

                if (sourceObject != null && sourceObject instanceof JsonObject) {
                    JsonObject jsonObject = (JsonObject) sourceObject;
                    
                    Platform.runLater(() ->{
                        updateWatchedTxs(explorerData, jsonObject);
                        //  saveAllTxJson(jsonObject);
                    });  
                    
                    
                }
            },
            failed -> {
                    try {
                        Files.writeString(logFile.toPath(), "\nAddressData, Explorer failed transaction update: " + failed.getSource().getException().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        
                        
                        }
                
                    //update();
                }
        );
        
        
    }*/
    private JsonObject m_balanceJson = null;

    private void setBalance(JsonObject jsonObject){
        m_balanceJson = jsonObject;
       
        m_addressesData.getWalletData().sendMessage(App.UPDATED, System.currentTimeMillis(), m_addressString); 


    }
    
    public JsonObject getBalance(){
        return m_balanceJson;
    }

   




    public void updateWatchedTxs(ErgoExplorerData explorerData, JsonObject json){
        
        if(json != null){
            JsonElement itemsElement = json.get("items");

            if(itemsElement != null && itemsElement.isJsonArray()){
                JsonArray itemsArray = itemsElement.getAsJsonArray();
                int size = itemsArray.size();

                SimpleBooleanProperty found = new SimpleBooleanProperty(false);

                for(int i = 0; i < size ; i ++){
                    JsonElement txElement = itemsArray.get(i);
                    if(txElement != null && txElement.isJsonObject()){
                        JsonObject txObject = txElement.getAsJsonObject();

                        JsonElement txIdElement = txObject.get("id");
                        if(txIdElement != null && txIdElement.isJsonPrimitive()){
                           
                            String txId = txIdElement.getAsString();
                            ErgoTransaction ergTx = getWatchedTx(txId);
                            if(ergTx != null){
                                if(!found.get()){
                                    found.set(true);
                                }
                                ergTx.update(txObject);
                            }
                        }
                    }
                }

                /*if(size >= UPDATE_LIMIT && !found.get()){
                    m_updateTxOffset++;
                    
                    updateTransactions(explorerData);
                }*/
               
            }
        
        }
    }

    /* Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String prettyString = gson.toJson(json); */

    public void saveAddresInfo(){
       

        JsonObject json = getJsonObject();    
        
        getNetworksData().save(m_addressString, m_addressesData.getWalletData().getNetworkId(), ErgoWallets.NETWORK_ID, ErgoNetwork.NETWORK_ID, json);
        
        
    }
     

    public int getApiIndex() {
        return m_apiIndex;
    }


    @Override
    public String toString() {
  
        return getName();
    }

    @Override
    public void shutdown(){
        removeErgoAmountListener();
        if(m_g2d != null){
            m_g2d.dispose();
            m_g2d = null;
        }
      //  m_img = null;
        super.shutdown();
    }
}
