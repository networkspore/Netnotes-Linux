package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netnotes.ErgoTransactionPartner.PartnerType;
import com.utils.Utils;
import com.google.gson.JsonArray;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class ErgoTransaction {
    private File logFile = new File("netnotes-log.txt");

    public final static String FEE_ERGOTREE_START = "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d";
    public final long REQUIRED_CONFIRMATIONS = 100;
    public final static PriceAmount UNKNOWN_PRICE_AMOUNT = new PriceAmount(0, new PriceCurrency("unknown","unknown","unknown",0,"unknown","unknown",null,"unknown",""));
    
    public static class TransactionType{
        public final static String SEND = "Send";
        public final static String HISTORICAL = "Historical";
        public final static String USER = "User";
        public final static String LARGE = "Large";
        public final static String UNKNOWN = "Unknown";
    }

    public static class TransactionStatus{
        public final static String PENDING = "Pending";
        public final static String CONFIRMED = "Confirmed";
        public final static String UNKNOWN = "Unknown";
    }
    private long m_requiredConfirmations = REQUIRED_CONFIRMATIONS;
    private SimpleStringProperty m_txPartnerTypeProperty = new SimpleStringProperty(PartnerType.UNKNOWN); 
    private String m_txId;
    private AddressData m_parentAddress;
    private long m_timeStamp = 0;
    private SimpleStringProperty m_txTypeProperty = new SimpleStringProperty( TransactionType.UNKNOWN);
    private SimpleStringProperty m_status = new SimpleStringProperty(TransactionStatus.UNKNOWN);
    private SimpleLongProperty m_numConfirmations = new SimpleLongProperty(0);
    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>();
    private ChangeListener<LocalDateTime> m_changeListener = null;
    private SimpleObjectProperty<PriceAmount> m_feeAmountProperty = new SimpleObjectProperty<>();
    private SimpleObjectProperty<ErgoAmount> m_ergoAmountProperty = new SimpleObjectProperty<>();

    private PriceAmount[] m_tokens = new PriceAmount[0];

    private ErgoTransactionPartner[] m_txPartners = new ErgoTransactionPartner[0];

    private double m_stageWidth = 600;
    private double m_stageHeight = 600;
    private Stage m_stage;

    public ErgoTransaction(String txId, AddressData parentAddress){
        m_txId = txId;
        m_parentAddress = parentAddress;
        m_feeAmountProperty.set(new ErgoAmount(0, parentAddress.getNetworkType()));
        m_ergoAmountProperty.set(new ErgoAmount(0, parentAddress.getNetworkType()));
    }
    

    public ErgoTransaction(String txId, AddressData parentAddress, JsonObject json){
        m_txId = txId;
        m_parentAddress = parentAddress;
        m_feeAmountProperty.set(new ErgoAmount(0, parentAddress.getNetworkType()));
        m_ergoAmountProperty.set(new ErgoAmount(0, parentAddress.getNetworkType()));
        m_txTypeProperty.set(TransactionType.HISTORICAL);
        update(json);
    }

    public ErgoTransaction(String txId, AddressData parentAddress, String txType){
        m_txId = txId;
        m_parentAddress = parentAddress;
        m_feeAmountProperty.set(new ErgoAmount(0, parentAddress.getNetworkType()));
        m_ergoAmountProperty.set(new ErgoAmount(0, parentAddress.getNetworkType()));
        m_txTypeProperty.set(txType);
    }

    public ErgoTransactionPartner[] getTxPartnerArray(){
        return m_txPartners;
    }

    public void setTxPartnerArray(ErgoTransactionPartner[] partners){
        m_txPartners = partners;
     
    }

    public ErgoTransactionPartner getTxPartner(String addressString){
        if(addressString != null){
            ErgoTransactionPartner[] txPartners = getTxPartnerArray();
            for(int i = 0; i < txPartners.length ; i++){
                if(txPartners[i].getParnterAddressString().equals(addressString)){
                    return txPartners[i];
                }
            }
        }
        return null;
    }


  

    public void doUpdate(ErgoExplorerData explorerData, boolean force){
        if(explorerData != null){
            long numConfirmations = numConfirmationsProperty().get();
            if(numConfirmations < m_requiredConfirmations || force){
                explorerData.getTransaction(getTxId(), (onSucceeded)->{
                    Object sourceObject = onSucceeded.getSource().getValue();
                    if(sourceObject != null && sourceObject instanceof JsonObject){
                        Platform.runLater(()-> update((JsonObject) sourceObject));
                    }
                }, (onFailed)->{
                    
                    try {
                        Files.writeString(logFile.toPath(), "tx doUpdate failed: " + onFailed.getSource().getException().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {
        
                    }

                });
            }
        }
    }

    public Stage getStage(){
        return m_stage;
    }

    public void setStage(Stage stage){
        m_stage = stage;
    }

    public double getStageWidth(){
        return m_stageWidth;
    }

    public void setStageWidth(double width){
        m_stageWidth = width;
         getLastUpdated().set(LocalDateTime.now());
    }

    public double getStageHeight(){
        return m_stageHeight;
    }

    public void setStageHeight(double height){
        m_stageHeight = height;
        getLastUpdated().set(LocalDateTime.now());
    }


    public String getTxType(){
        return m_txTypeProperty.get();
    }

    public void setTxType(String type){
        m_txTypeProperty.set(type);;
    }

    public SimpleStringProperty txTypeProperty(){
        return m_txTypeProperty;
    }

    public String getTxId(){
        return m_txId;
    }
    
    public long getTimeStamp(){
        return m_timeStamp;
    }

    public void setTimeStamp(long timeStamp){
        m_timeStamp = timeStamp;
    }    

    public SimpleStringProperty statusProperty(){
        return m_status;
    }

    public SimpleLongProperty numConfirmationsProperty(){
        return m_numConfirmations;
    }

    public SimpleObjectProperty< ErgoAmount> ergoAmountProperty(){
        return m_ergoAmountProperty;
    }

    public void setErgoAmount(ErgoAmount ergoAmount){
        m_ergoAmountProperty.set( ergoAmount);
    }

    public ErgoAmount getErgoAmount(){
        return m_ergoAmountProperty.get();
    }

     public void setFeeAmount(PriceAmount feeAmount){
        m_feeAmountProperty.set( feeAmount);
    }

    public PriceAmount getFeeAmount(){
        return m_feeAmountProperty.get();
    }

     public SimpleObjectProperty<PriceAmount> getFeeAmountProperty(){
        return m_feeAmountProperty;
    }

    public void setTokens(PriceAmount[] tokens){
        m_tokens = tokens;
   
    }



    public PriceAmount[] getTokens(){
        return m_tokens;
    }

    public PriceAmount getToken(String tokenId){
        if(tokenId != null){
            PriceAmount[] tokens = getTokens();
            for(int i = 0; i < tokens.length ; i++){
                PriceAmount token = tokens[i];
                if(token.getTokenId().equals(tokenId)){
                    return token;
                }
            }
        }
        return null;
    }
    

    public JsonArray getTokenJsonArray(){
        JsonArray jsonArray = new JsonArray();
        PriceAmount[] tokens = getTokens();
        if(tokens != null){
            for(int i = 0; i < tokens.length ; i++){
                jsonArray.add(tokens[i].getJsonObject());
            }
        }
        return jsonArray;
    }
    

    public void open(){
        showErgoTxStage();
    }

     public void showErgoTxStage(){
        if(m_stage == null){
           
            VBox layoutVBox = new VBox();
            Scene txScene = new Scene(layoutVBox, m_stageWidth, m_stageHeight);
            txScene.setFill(null);
            txScene.getStylesheets().add("/css/startWindow.css");

            String titleString = partnerTypeProperty().get() +": " + getErgoAmount().toString() + " - " + statusProperty().get()  + " - " + getTxId();

            m_stage = new Stage();
            m_stage.getIcons().add(ErgoWallets.getAppIcon());
            m_stage.initStyle(StageStyle.UNDECORATED);
            m_stage.setTitle(titleString);

            Button closeBtn = new Button();

            HBox titleBox = App.createTopBar(ErgoWallets.getSmallAppIcon(), titleString, closeBtn, m_stage);

            Tooltip explorerTip = new Tooltip("Select explorer");
            explorerTip.setShowDelay(new javafx.util.Duration(50));
            explorerTip.setFont(App.txtFont);



            BufferedMenuButton explorerBtn = new BufferedMenuButton("/assets/ergo-explorer-30.png", App.MENU_BAR_IMAGE_WIDTH);
            explorerBtn.setPadding(new Insets(2, 0, 0, 2));
            explorerBtn.setTooltip(explorerTip);

            Runnable updateExplorerBtn = () ->{
                ErgoExplorers ergoExplorers = (ErgoExplorers) getParentAddress().getAddressesData().getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);

                ErgoExplorerData explorerData = getParentAddress().getAddressesData().selectedExplorerData().get();
            
            
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
            
                ErgoExplorers ergoExplorers = (ErgoExplorers) getParentAddress().getAddressesData().getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);
                if(ergoExplorers != null){
                    explorerBtn.setId("menuBtn");
                    ergoExplorers.getErgoExplorersList().getMenu(explorerBtn, getParentAddress().getAddressesData().selectedExplorerData());
                }else{
                    explorerBtn.getItems().clear();
                    explorerBtn.setId("menuBtnDisabled");
                
                }
                updateExplorerBtn.run();
            };    

            getParentAddress().getAddressesData().selectedExplorerData().addListener((obs, oldval, newval)->{
                getParentAddress().getAddressesData().getWalletData().setExplorer(newval == null ? null : newval.getId());
                updateExplorerBtn.run();
            });

            HBox rightSideMenu = new HBox( explorerBtn);
            rightSideMenu.setId("rightSideMenuBar");
            rightSideMenu.setPadding(new Insets(0, 0, 0, 0));
            rightSideMenu.setAlignment(Pos.CENTER_RIGHT);

             Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Tooltip deleteTooltip = new Tooltip("Remove from watch list");
            deleteTooltip.setShowDelay(new Duration(100));


            BufferedButton deleteBtn = new BufferedButton("/assets/star-30.png", App.MENU_BAR_IMAGE_WIDTH);
            deleteBtn.setTooltip(deleteTooltip);
            deleteBtn.setOnAction(e->{
                getParentAddress().removeTransaction(getTxId());
                closeBtn.fire();
            });

            BufferedButton refreshBtn = new BufferedButton("/assets/refresh-white-30.png", App.MENU_BAR_IMAGE_WIDTH);
            refreshBtn.setOnAction(e->{
                ErgoExplorerData explorerData = getParentAddress().getAddressesData().selectedExplorerData().get();
                if(explorerData != null){
                    doUpdate(explorerData, true);
                }
            });

            HBox menuBar = new HBox(refreshBtn, spacer, rightSideMenu);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 5));

            Tooltip watchTooltip = new Tooltip("Add to watch list");
            watchTooltip.setShowDelay(new Duration(100));

            BufferedButton watchTxBtn = new BufferedButton("/assets/star-outline-30.png", App.MENU_BAR_IMAGE_WIDTH);
            watchTxBtn.setTooltip(watchTooltip);

            watchTxBtn.setOnAction(e->{
                getParentAddress().addWatchedTransaction(this);
            });

            Runnable updateMenuBar = ()->{
                ErgoTransaction ergoTx = getParentAddress().getWatchedTx(getTxId());

                if(ergoTx != null){
                    if(menuBar.getChildren().contains(watchTxBtn)){
                        menuBar.getChildren().remove(watchTxBtn);     
                    }
                    if(!menuBar.getChildren().contains(deleteBtn)){
                        menuBar.getChildren().add(0, deleteBtn);     
                    }
                    
                }else{
                    if(menuBar.getChildren().contains(deleteBtn)){
                        menuBar.getChildren().remove(deleteBtn);     
                    }
                    if(!menuBar.getChildren().contains(watchTxBtn)){
                        menuBar.getChildren().add(0, watchTxBtn);     
                    }
                }
            };

            getParentAddress().watchedTxList().addListener((ListChangeListener.Change<? extends ErgoTransaction> c)->{
                updateMenuBar.run();
            });            
            updateMenuBar.run();

            Text txText = new Text();
            txText.setFont(App.txtFont);
            txText.setFill(App.txtColor);
            txText.textProperty().bind(Bindings.concat(partnerTypeProperty(), " - ", statusProperty(), Bindings.when(numConfirmationsProperty().greaterThan(1)).then( Bindings.concat(" (",numConfirmationsProperty()," confirmations)")).otherwise("") ));

            
            Text txIdText = new Text("Tx:");
            txIdText.setFont(App.txtFont);
            txIdText.setFill(App.txtColor);

            TextField txField = new TextField(getTxId());
            txField.setId("formFieldSmall");
            txField.setEditable(false);
            txField.setPrefWidth(Utils.measureString(getTxId(), new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 12)) + 30);

            Tooltip copiedTooltip = new Tooltip("copied");

            BufferedButton copyTxBtn = new BufferedButton("/assets/copy-30.png", App.MENU_BAR_IMAGE_WIDTH);
            copyTxBtn.setOnAction(e->{
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(getTxId());
                clipboard.setContent(content);

                Point2D p = copyTxBtn.localToScene(0.0, 0.0);

                copiedTooltip.show(
                    copyTxBtn,  
                    p.getX() + copyTxBtn.getScene().getX() + copyTxBtn.getScene().getWindow().getX(), 
                    (p.getY()+ copyTxBtn.getScene().getY() + copyTxBtn.getScene().getWindow().getY())-copyTxBtn.getLayoutBounds().getHeight()
                    );
                PauseTransition pt = new PauseTransition(Duration.millis(1600));
                pt.setOnFinished(ptE->{
                    copiedTooltip.hide();
                });
                pt.play();
            });

            Tooltip unknownExplorerTip = new Tooltip("Select Explorer");

        

            BufferedButton linkBtn = new BufferedButton("/assets/link-20.png", App.MENU_BAR_IMAGE_WIDTH);
            
            linkBtn.setOnAction(e->{
                if(isErgoExplorer()){
                    openLink();
                }else{
                    Point2D p = linkBtn.localToScene(0.0, 0.0);

                    unknownExplorerTip.show(
                        linkBtn,  
                        p.getX() + linkBtn.getScene().getX() + linkBtn.getScene().getWindow().getX(), 
                        (p.getY()+ linkBtn.getScene().getY() + linkBtn.getScene().getWindow().getY())-linkBtn.getLayoutBounds().getHeight()
                    );

                    PauseTransition pt = new PauseTransition(Duration.millis(1600));
                    pt.setOnFinished(ptE->{
                        unknownExplorerTip.hide();
                    });
                    pt.play();
                }
            });

            HBox txBox = new HBox(txText);
            txBox.setId("headingBox");
            HBox.setHgrow(txBox, Priority.ALWAYS);
            txBox.setAlignment(Pos.CENTER_LEFT);
            txBox.setPadding(new Insets(0,15,0,15));
            txBox.setMinHeight(40);

            HBox txidBox = new HBox( txIdText, txField, copyTxBtn, linkBtn);
            HBox.setHgrow(txidBox, Priority.ALWAYS);
            txidBox.setMinHeight(40);
            txidBox.setPadding(new Insets(0,15,0,10));
            txidBox.setAlignment(Pos.CENTER_LEFT);
        

            Text fromText = new Text("From:");
            fromText.setFill(App.txtColor);
            fromText.setFont(App.txtFont);

            TextField fromField = new TextField( );
            fromField.setEditable(false);
            fromField.setId("formFieldSmall");
            HBox.setHgrow(fromField, Priority.ALWAYS);

            HBox fromBox = new HBox(fromText, fromField);
            HBox.setHgrow(fromBox, Priority.ALWAYS);
            fromBox.setAlignment(Pos.CENTER_LEFT);
            fromBox.setPadding(new Insets(0,15,0,10));
            fromBox.setMinHeight(30);


            Text toText = new Text("To:  ");
            toText.setFill(App.txtColor);
            toText.setFont(App.txtFont);

           

            TextField toField = new TextField();
            toField.setEditable(false);
            toField.setId("formFieldSmall");
            HBox.setHgrow(toField, Priority.ALWAYS);

            Runnable updateReceivers = ()->{
                ErgoTransactionPartner partnerArray[] = getTxPartnerArray();
                int size = partnerArray.length;
                toField.setText("");
                fromField.setText("");
                for(int i = 0 ; i < size ; i++){
                    ErgoTransactionPartner partner = partnerArray[i];

                    if(partner.getPartnerType().equals(PartnerType.RECEIVER)){
                        if(toField.getText().length() > 0){
                            toField.setText(toField.getText() + ", " + partner.getParnterAddressString());
                        }else{
                            toField.setText(partner.getParnterAddressString());
                        }
                    }else{
                        if(fromField.getText().length() > 0){
                            fromField.setText(fromField.getText() + ", " + partner.getParnterAddressString());
                        }else{
                            fromField.setText(partner.getParnterAddressString());
                        }
                    }

                }
                if(partnerTypeProperty().get().equals(PartnerType.SENDER)){
                    fromField.setText(getParentAddress().getAddressString());
                }
                
            };

            updateReceivers.run();

          
            HBox toBox = new HBox(toText, toField);
            HBox.setHgrow(toBox, Priority.ALWAYS);
            toBox.setAlignment(Pos.CENTER_LEFT);
            toBox.setPadding(new Insets(0,15,0,10));
            toBox.setMinHeight(30);

            ErgoAmountBox ergoAmountBox = new ErgoAmountBox(getErgoAmount(), txScene, getParentAddress().getNetworksData().getHostServices());
            HBox.setHgrow(ergoAmountBox, Priority.ALWAYS);
            ergoAmountBox.priceQuoteProperty().bind(getParentAddress().getAddressesData().currentPriceQuoteProperty());
            ergoAmountBox.priceAmountProperty().bind(ergoAmountProperty());

            HBox amountBoxPadding = new HBox(ergoAmountBox);
            amountBoxPadding.setPadding(new Insets(10, 10, 0, 10));

            AmountBoxes amountBoxes = new AmountBoxes();
            amountBoxes.setPadding(new Insets(5, 10, 5, 0));
            amountBoxes.setAlignment(Pos.TOP_LEFT);

            Runnable updateTokens = ()->{
                amountBoxes.clear();
                if (getTokens() != null && getTokens().length > 0) {
                    PriceAmount[] tokens = getTokens();
                    int numTokens = tokens.length;
                    for (int i = 0; i < numTokens; i++) {
                        PriceAmount tokenAmount = tokens[i];

                        AmountBox amountBox = new AmountBox(tokenAmount,txScene, getParentAddress().getAddressesData().isErgoTokensProperty(), getParentAddress().getAddressesData().getWalletData().getErgoWallets().getErgoNetworkData());
                        amountBoxes.add(amountBox);
                        
                    }
                }
            };
            
            updateTokens.run();

            getLastUpdated().addListener((obs,oldval,newval)->{
                updateTokens.run();
            });
            
            VBox boxesVBox = new VBox(amountBoxPadding, amountBoxes);
            HBox.setHgrow(boxesVBox, Priority.ALWAYS);

            ScrollPane scrollPane = new ScrollPane(boxesVBox);
            scrollPane.setPadding(new Insets(10, 0, 5, 0));
            scrollPane.setId("bodyBox");

            VBox detailsBox = new VBox(fromBox, toBox, scrollPane);
            HBox.setHgrow(detailsBox, Priority.ALWAYS);
            detailsBox.setId("darkBox");
            detailsBox.setPadding(new Insets(10,10,0,10));

            VBox bodyDetailsBox = new VBox(txidBox, detailsBox);
            HBox.setHgrow(bodyDetailsBox, Priority.ALWAYS);
            bodyDetailsBox.setId("bodyBox");
            bodyDetailsBox.setPadding(new Insets(0,10,10,10));

            VBox bodyBox = new VBox(txBox, bodyDetailsBox);
            bodyBox.setPadding(new Insets(0));
            bodyBox.setId("bodyBox");

            Region menuBarRegion = new Region();
            menuBarRegion.setMinHeight(4);

            VBox bodyPaddingBox = new VBox(menuBar, menuBarRegion, bodyBox);

            
            bodyPaddingBox.setPadding(new Insets(0,4,4,4));
            layoutVBox.getChildren().addAll(titleBox,bodyPaddingBox);

            m_stage.setScene(txScene);
            
            getParentAddress().getAddressesData().getWalletData().getErgoWallets().getErgoNetworkData().addNetworkListener((ListChangeListener.Change<? extends NoteInterface> c) -> {
               
                getAvailableExplorerMenu.run();
            });

            getAvailableExplorerMenu.run();

            
            scrollPane.prefViewportWidthProperty().bind(txScene.widthProperty().subtract(60));
            scrollPane.prefViewportHeightProperty().bind(txScene.heightProperty().subtract(270));
            amountBoxes.minHeightProperty().bind(scrollPane.prefViewportHeightProperty().subtract(60));
            amountBoxes.prefWidthProperty().bind(txScene.widthProperty().subtract(60));

            java.awt.Rectangle rect = getParentAddress().getAddressesData().getWalletData().getNetworksData().getMaximumWindowBounds();

            ResizeHelper.addResizeListener(m_stage, 200, 250, rect.getWidth(), rect.getHeight());
            
            m_stage.show();
            closeBtn.setOnAction(e->{
              
                m_stage.close();
                m_stage = null;
                
            });

            m_stage.setOnCloseRequest(e->closeBtn.fire());
        }else{
            if(m_stage.isIconified()){
                m_stage.setIconified(false);
                m_stage.show();
                m_stage.toFront();
            }else{
                Platform.runLater(()-> m_stage.requestFocus());
            }
        }
    }

    public AddressData getParentAddress(){
        return m_parentAddress;
    }

    public boolean isErgoExplorer(){
        return getExplorerData() != null;
    }

    public ErgoExplorerData getExplorerData(){
        return getParentAddress().getAddressesData().selectedExplorerData().get();
    }

    public SimpleStringProperty partnerTypeProperty(){
        return m_txPartnerTypeProperty;
    }
    
    public void openLink(){
        if(getExplorerData() != null){
            String explorerUrlString = getExplorerData().getWebsiteTxLink(getTxId());
            getParentAddress().getNetworksData().getHostServices().showDocument(explorerUrlString);
        }
    }

    public HBox getTxBox(){
        ImageView txPartnerTypeText = new ImageView();
        txPartnerTypeText.setPreserveRatio(true);
    

        Runnable updateTextColor = () ->{
            String partnerType = partnerTypeProperty().get();
            if(partnerType == null){
               
               Image img = Drawing.getPosNegText(PartnerType.UNKNOWN, false, true);
               txPartnerTypeText.setImage(img);
               txPartnerTypeText.setFitWidth(img.getWidth());

            }else{
                switch(partnerType){
                    case PartnerType.RECEIVER:
                        Image img = Drawing.getPosNegText(partnerType, true, false);
                        txPartnerTypeText.setImage(img);
                        txPartnerTypeText.setFitWidth(img.getWidth());
                    break;
                    case PartnerType.SENDER:
                        Image img1 = Drawing.getPosNegText(partnerType, false, false);
                        txPartnerTypeText.setImage(img1);
                        txPartnerTypeText.setFitWidth(img1.getWidth());
                    break;
                    default:
                        Image img2 = Drawing.getPosNegText(partnerType, false, true);
                        txPartnerTypeText.setImage(img2);
                        txPartnerTypeText.setFitWidth(img2.getWidth());
                }
              
            }
        };
        updateTextColor.run();
        partnerTypeProperty().addListener((obs,oldval,newval)->updateTextColor.run());
       
        String ergAmountText = ergoAmountProperty().get().toString();

        String tokensString = "";
        PriceAmount[] tokens = getTokens();
        if(tokens != null && tokens.length > 0){
            for(int i = 0; i < tokens.length ; i ++){
                tokensString += ", " + tokens[i].toString();
            }
        }
        tokensString = !tokensString.equals("") && ergAmountText.equals("") ? tokensString.substring(1) : tokensString;

        TextField txAmountField = new TextField((ergAmountText.equals("0 ERG") ? "" : ergAmountText) +  tokensString);
        txAmountField.setFont(App.txtFont);
        txAmountField.setId("formField");
        HBox.setHgrow(txAmountField,Priority.ALWAYS);

        Text txStatus = new Text();
        txStatus.setFont(App.txtFont);
        txStatus.setFill(App.txtColor);
        txStatus.textProperty().bind( statusProperty());
        
        TextField txField = new TextField(getTxId());
        txField.setId("addressField");
        txField.setEditable(false);
        txField.setPrefWidth(Utils.measureString(getTxId(), new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 14)) + 30);
        HBox.setHgrow(txField,Priority.SOMETIMES);

        Tooltip copiedTooltip = new Tooltip("copied");

        Tooltip copyTooltip = new Tooltip("Copy Id");
        copyTooltip.setShowDelay(new Duration(100));

        BufferedButton copyTxBtn = new BufferedButton("/assets/copy-30.png", App.MENU_BAR_IMAGE_WIDTH);
        copyTxBtn.setTooltip(copyTooltip);
        copyTxBtn.setOnAction(e->{
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(getTxId());
            clipboard.setContent(content);

            Point2D p = copyTxBtn.localToScene(0.0, 0.0);

            copiedTooltip.show(
                copyTxBtn,  
                p.getX() + copyTxBtn.getScene().getX() + copyTxBtn.getScene().getWindow().getX(), 
                (p.getY()+ copyTxBtn.getScene().getY() + copyTxBtn.getScene().getWindow().getY())-copyTxBtn.getLayoutBounds().getHeight()
                );
            PauseTransition pt = new PauseTransition(Duration.millis(1600));
            pt.setOnFinished(ptE->{
                copiedTooltip.hide();
            });
            pt.play();
        });

        Tooltip openTooltip = new Tooltip("Open");
        openTooltip.setShowDelay(new Duration(100));

        BufferedButton openBtn = new BufferedButton("/assets/open-outline-white-20.png", 15);
        openBtn.setTooltip(openTooltip);
        openBtn.setOnAction(e->{
            open();
        });

        Tooltip unknownExplorerTip = new Tooltip("Select Explorer");

        Tooltip linkTooltip = new Tooltip("Open in browser");
        linkTooltip.setShowDelay(new Duration(100));

        BufferedButton linkBtn = new BufferedButton("/assets/link-20.png", App.MENU_BAR_IMAGE_WIDTH);
        linkBtn.setTooltip(linkTooltip);
        linkBtn.setOnAction(e->{
            if(isErgoExplorer()){
                    openLink();
                }else{
                    Point2D p = linkBtn.localToScene(0.0, 0.0);

                    unknownExplorerTip.show(
                        linkBtn,  
                        p.getX() + linkBtn.getScene().getX() + linkBtn.getScene().getWindow().getX(), 
                        (p.getY()+ linkBtn.getScene().getY() + linkBtn.getScene().getWindow().getY())-linkBtn.getLayoutBounds().getHeight()
                    );

                    PauseTransition pt = new PauseTransition(Duration.millis(1600));
                    pt.setOnFinished(ptE->{
                        unknownExplorerTip.hide();
                    });
                    pt.play();
                }
        });
 

        HBox menuBar = new HBox( copyTxBtn, linkBtn, openBtn);

        Tooltip deleteTooltip = new Tooltip("Remove from watch list");
        deleteTooltip.setShowDelay(new Duration(100));

        BufferedButton deleteBtn = new BufferedButton("/assets/star-30.png", App.MENU_BAR_IMAGE_WIDTH);
        deleteBtn.setTooltip(deleteTooltip);
        deleteBtn.setOnAction(e->{
            getParentAddress().removeTransaction(getTxId());
        });

         Tooltip watchTooltip = new Tooltip("Add to watch list");
            watchTooltip.setShowDelay(new Duration(100));

            BufferedButton watchTxBtn = new BufferedButton("/assets/star-outline-30.png", App.MENU_BAR_IMAGE_WIDTH);
            watchTxBtn.setTooltip(watchTooltip);

            watchTxBtn.setOnAction(e->{
                getParentAddress().addWatchedTransaction(this);
            });

            Runnable updateMenuBar = ()->{
                ErgoTransaction ergoTx = getParentAddress().getWatchedTx(getTxId());

                if(ergoTx != null){
                    if(menuBar.getChildren().contains(watchTxBtn)){
                        menuBar.getChildren().remove(watchTxBtn);     
                    }
                    if(!menuBar.getChildren().contains(deleteBtn)){
                        menuBar.getChildren().add(3, deleteBtn);     
                    }
                    
                }else{
                    if(menuBar.getChildren().contains(deleteBtn)){
                        menuBar.getChildren().remove(deleteBtn);     
                    }
                    if(!menuBar.getChildren().contains(watchTxBtn)){
                        menuBar.getChildren().add(3,watchTxBtn);     
                    }
                }
            };

        getParentAddress().watchedTxList().addListener((ListChangeListener.Change<? extends ErgoTransaction> c)->{
            updateMenuBar.run();
        });            
        updateMenuBar.run();

        HBox botRightBox = new HBox();
        botRightBox.setMinHeight(10);

        VBox rightBox = new VBox(menuBar, botRightBox);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.setPadding(new Insets(0,0,0,10));
 



        VBox leftVBox = new VBox(txPartnerTypeText, txAmountField);
        leftVBox.setMinWidth(100);
        leftVBox.setAlignment(Pos.CENTER_LEFT);

        VBox midBox = new VBox(txStatus, txField);
        midBox.setAlignment(Pos.CENTER_LEFT);
        midBox.setPadding(new Insets(0));
        HBox.setHgrow(midBox, Priority.ALWAYS);

        HBox txBox = new HBox(leftVBox, midBox, rightBox);
        HBox.setHgrow(txBox, Priority.ALWAYS);
        txBox.setAlignment(Pos.CENTER_LEFT);
        txBox.setPadding(new Insets(10,15,0,10));
        txBox.setMinHeight(40);
        txBox.setId("rowBox");

        txBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e->{
            getParentAddress().selectedTransaction().set(this);
            if(e.getClickCount() == 2){
                open();
            }
        }); 

        getParentAddress().selectedTransaction().addListener((obs,oldval,newval)->{
            if(newval != null && newval.getTxId().equals(getTxId())){
                txBox.setId("bodyRowBox");
            }else{
                txBox.setId("rowBox");
            }
        });
        
        return txBox;
    }

    public SimpleObjectProperty<LocalDateTime> getLastUpdated(){
        return m_lastUpdated;
    }

    public PriceAmount[] parseAssetsPriceAmount(JsonArray jsonArray){
        
        if(jsonArray != null && jsonArray.size() > 0){
            ErgoTokens ergoTokens = getParentAddress().getAddressesData().isErgoTokensProperty().get() ? (ErgoTokens) getParentAddress().getAddressesData().getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoTokens.NETWORK_ID) : null;
            ErgoTokensList tokensList = ergoTokens != null ? ergoTokens.getTokensList(getParentAddress().getNetworkType()) : null;

            int size = jsonArray.size();
            ArrayList<PriceAmount> tokenArrayList = new ArrayList<>();

            for(int i = 0; i < size; i++){
                JsonElement tokenElement = jsonArray.get(i);

                if(tokenElement != null && tokenElement.isJsonObject()){
                    JsonObject tokenObject = tokenElement.getAsJsonObject();

                    JsonElement tokenIdElement = tokenObject.get("tokenId");
                    JsonElement amountElement = tokenObject.get("amount");
                    JsonElement decimalsElement = tokenObject.get("decimals");
                    JsonElement nameElement = tokenObject.get("name");
                    JsonElement tokenTypeElement = tokenObject.get("tokenType");

                    if(tokenIdElement != null && tokenIdElement.isJsonPrimitive() && 
                        amountElement != null && amountElement.isJsonPrimitive() &&
                        decimalsElement != null && decimalsElement.isJsonPrimitive() &&
                        nameElement != null && nameElement.isJsonPrimitive()){
                    
                        String tokenId = tokenIdElement.getAsString();
                        long amount = amountElement.getAsLong();
                        int decimals = decimalsElement.getAsInt();
                        String name = nameElement.getAsString();
                        String tokenType = tokenTypeElement != null && tokenTypeElement.isJsonPrimitive() ? tokenTypeElement.getAsString() : "";

                        ErgoNetworkToken networkToken = tokensList != null ? tokensList.getErgoToken(tokenId) : null;

                        if(networkToken != null){
                            networkToken.setDecimals(decimals);
                            networkToken.setTokenType(tokenType);
                        }

                        PriceAmount tokenAmount = networkToken != null ? new PriceAmount(amount, networkToken) : new PriceAmount(amount, new PriceCurrency(tokenId, name, name, decimals, ErgoNetwork.NETWORK_ID, getParentAddress().getNetworkType().toString(), "/assets/unknown-unit.png",tokenType, ""));    
                        tokenArrayList.add(tokenAmount);
                    }
                }                
            }

            PriceAmount[] tokens = new PriceAmount[tokenArrayList.size()];
            return tokenArrayList.toArray(tokens);
        }
        return new PriceAmount[0];
    }


    public void update(JsonObject json){
        if(json != null){
            JsonElement numConfirmationsElement = json.get("numConfirmations");
            JsonElement timeStampElement = json.get("timestamp");
            JsonElement sizeElement = json.get("size");

            int txSize = sizeElement != null && sizeElement.isJsonPrimitive() ? sizeElement.getAsInt() : 0;
            

            

            if(numConfirmationsElement != null && numConfirmationsElement.isJsonPrimitive()){
                long numConfirmations = numConfirmationsElement.getAsLong();

                if(numConfirmations > 0){
                    statusProperty().set(TransactionStatus.CONFIRMED);
                    m_numConfirmations.set(numConfirmations);
                
                }
            }

            m_timeStamp = timeStampElement != null && timeStampElement.isJsonPrimitive() ? timeStampElement.getAsLong() : m_timeStamp;
            
         
                          
                JsonElement outputsElement = json.get("outputs");
                JsonElement inputsElement = json.get("inputs");

                SimpleStringProperty simpleSenderAddressString = new SimpleStringProperty(null);

                SimpleLongProperty simpleErgNanoFeeTotal = new SimpleLongProperty(0);
                ArrayList<ErgoTransactionPartner> outputTxPartner = new ArrayList<>();

                if(inputsElement != null && inputsElement.isJsonArray()){
                    JsonArray jsonArray = inputsElement.getAsJsonArray();
                    int size = jsonArray.size();
                    for(int i = 0; i < size ; i++){
                        JsonElement inputItemElement = jsonArray.get(i);
                        if(inputItemElement != null && inputItemElement.isJsonObject()){
                            JsonObject inputItemObject = inputItemElement.getAsJsonObject();
                        // JsonElement outputItemValue = inputItemObject.get("value");
                            JsonElement inputItemAddress = inputItemObject.get("address");
                            if(inputItemAddress != null && inputItemAddress.isJsonPrimitive()){
                                simpleSenderAddressString.set(inputItemAddress.getAsString());
                                break;
                            }
                        }
                    }
                }

                if(simpleSenderAddressString.get() == null){
                    setTxType(TransactionType.UNKNOWN);
                    return;
                }

                final String parentAddressString = getParentAddress().getAddressString();
                final String senderAddressString = simpleSenderAddressString.get();

                boolean isParentSender = parentAddressString.equals(senderAddressString);

               

    
                ErgoTransactionPartner parentPartner = new ErgoTransactionPartner(getParentAddress().getAddressString(), isParentSender ? PartnerType.SENDER : PartnerType.RECEIVER, new ErgoAmount(0, getParentAddress().getNetworkType()));
                outputTxPartner.add(parentPartner);
                ErgoTransactionPartner senderPartner = isParentSender ? parentPartner : new ErgoTransactionPartner(senderAddressString, PartnerType.SENDER,  new ErgoAmount(0, getParentAddress().getNetworkType()));
                if(!isParentSender){
                    outputTxPartner.add(senderPartner);
                }else{
                    m_txPartnerTypeProperty.set(PartnerType.SENDER);
                }
                

                if(outputsElement != null && outputsElement.isJsonArray()){
                    JsonArray jsonArray = outputsElement.getAsJsonArray();
                    int size =  jsonArray.size();

                    for(int i = 0; i < size ; i++){
                        JsonElement outputItemElement = jsonArray.get(i);

                        if(outputItemElement != null && outputItemElement.isJsonObject()){
                            JsonObject outputItemObject = outputItemElement.getAsJsonObject();

                            JsonElement outputItemValue = outputItemObject.get("value");
                            JsonElement outputItemAddress = outputItemObject.get("address");
                            JsonElement outputItemErgoTree = outputItemObject.get("ergoTree");
                            JsonElement outputItemAssets = outputItemObject.get("assets");
                            
                        
                            if(outputItemAddress != null && outputItemAddress.isJsonPrimitive() && outputItemErgoTree != null && outputItemErgoTree.isJsonPrimitive()){
                                long outputNanoErg = outputItemValue != null && outputItemValue.isJsonPrimitive() ? outputItemValue.getAsLong() : 0;
                                String outputAddressString = outputItemAddress.getAsString();
                                String outputErgoTree = outputItemErgoTree.getAsString();
                                PriceAmount[] outputTokens = parseAssetsPriceAmount(outputItemAssets.getAsJsonArray());

                                if(isParentSender){
                                    if(outputErgoTree.startsWith(FEE_ERGOTREE_START )){
                                        simpleErgNanoFeeTotal.set(simpleErgNanoFeeTotal.get() + outputNanoErg);
                                    }else{
                                        if(!outputAddressString.equals(parentAddressString)){
                                            parentPartner.addNanoErgs( outputNanoErg);
                                            parentPartner.addTokens(outputTokens);

                                            SimpleBooleanProperty foundPartner = new SimpleBooleanProperty(false);
                                            int txPartnersSize = outputTxPartner.size();
                                            for(int j = 0; j < txPartnersSize ; j++){
                                                ErgoTransactionPartner outputPartner = outputTxPartner.get(j);
                                                if(outputPartner.getParnterAddressString().equals(outputAddressString)){
                                                    foundPartner.set(true);       
                                                    outputPartner.addNanoErgs(outputNanoErg);
                                                    outputPartner.addTokens(outputTokens);
                                                    break;
                                                }
                                            }
                                            if(!foundPartner.get()){
                                                ErgoTransactionPartner newTxPartner = new ErgoTransactionPartner(outputAddressString,PartnerType.RECEIVER, new ErgoAmount(outputNanoErg, getParentAddress().getNetworkType()),outputTokens );
                                                outputTxPartner.add(newTxPartner);
                                            }
                                        }
                                    }
                                    
                                }else{
                              
                                    if(outputErgoTree.startsWith(FEE_ERGOTREE_START)){
                                        simpleErgNanoFeeTotal.set(simpleErgNanoFeeTotal.get() + outputNanoErg);
                                    }else{
                                        if(!outputAddressString.equals(senderAddressString)){
                                            senderPartner.addNanoErgs(outputNanoErg);
                                            senderPartner.addTokens(outputTokens);

                                            SimpleBooleanProperty foundPartner = new SimpleBooleanProperty(false);
                                            int txPartnersSize = outputTxPartner.size();
                                            for(int j = 0; j < txPartnersSize ; j++){
                                                ErgoTransactionPartner outputPartner = outputTxPartner.get(j);
                                                if(outputPartner.getParnterAddressString().equals(outputAddressString)){
                                                    foundPartner.set(true);       
                                                    outputPartner.addNanoErgs(outputNanoErg);
                                                    outputPartner.addTokens(outputTokens);
                                                    break;
                                                }
                                            }
                                            if(!foundPartner.get()){
                                                ErgoTransactionPartner newTxPartner = new ErgoTransactionPartner(outputAddressString,PartnerType.RECEIVER, new ErgoAmount(outputNanoErg, getParentAddress().getNetworkType()),outputTokens );
                                                outputTxPartner.add(newTxPartner);
                                            }
                                            if(outputAddressString.equals(parentAddressString)){
                                                m_txPartnerTypeProperty.set(PartnerType.RECEIVER);
                                            }
                                        }
                                    }
                                    
                                }
                                

                            }
                        }
                    }
                    if(parentPartner.ergoAmountProperty().get().getLongAmount() == 0 && parentPartner.getTokensArray().length == 0){
                        outputTxPartner.remove(parentPartner);
                    }

                    ErgoTransactionPartner[] partnerArray = new ErgoTransactionPartner[outputTxPartner.size()];
                    partnerArray = outputTxPartner.toArray(partnerArray);
                    setTxPartnerArray(partnerArray);
                 
                    setFeeAmount((PriceAmount) new ErgoAmount(simpleErgNanoFeeTotal.get(), getParentAddress().getNetworkType()));
                
                    setErgoAmount(parentPartner.ergoAmountProperty().get());
                    setTokens(parentPartner.getTokensArray());
                }
          
        }else{
        
        }
        m_lastUpdated.set(LocalDateTime.now());
    }

    
    public void addUpdateListener(ChangeListener<LocalDateTime> changeListener) {
        m_changeListener = changeListener;
        if (m_changeListener != null) {
            m_lastUpdated.addListener(m_changeListener);

        }
        // m_lastUpdated.addListener();
    }

    public void removeUpdateListener() {
        if (m_changeListener != null) {
            m_lastUpdated.removeListener(m_changeListener);
            m_changeListener = null;
        }
    }



    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
      
        json.addProperty("txId", m_txId);
        json.addProperty("parentAddress", m_parentAddress.getAddress().toString());
        json.addProperty("timeStamp", m_timeStamp);
        json.addProperty("txType", m_txTypeProperty.get());
        json.addProperty("status", m_status.get());
        json.addProperty("numConfirmations", m_numConfirmations.getValue());
        return json;
     }
}
