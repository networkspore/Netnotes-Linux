package com.netnotes;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonObject;
import com.netnotes.ErgoTransactionPartner.PartnerType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class ErgoSimpleSendTx extends ErgoTransaction  {


    

    private AddressInformation m_receipientAddress;

   
    private String m_explorerUrl = "";
    private String m_nodeUrl;
   

  

        


    public ErgoSimpleSendTx(String txId, AddressData parentAddress, AddressInformation receipientAddress, long ergoAmount, PriceAmount feeAmount, PriceAmount[] tokens, String nodeUrl,  String explorerUrl, String status, long created){
        super(txId, parentAddress);

        partnerTypeProperty().set(PartnerType.SENDER);
    
        setErgoAmount(new ErgoAmount(ergoAmount, getNetworkType()));
        m_receipientAddress = receipientAddress;
        setFeeAmount(feeAmount);
        setTokens(tokens == null ? new PriceAmount[0] : tokens); 
        m_nodeUrl = nodeUrl;
        m_explorerUrl = explorerUrl == null ? "" : explorerUrl;
        statusProperty().set(status);
        setTimeStamp(created);
        setTxType(TransactionType.SEND);
     
    }



    public ErgoSimpleSendTx(String txId,AddressData parentAddress, JsonObject json) throws Exception{
        super(txId, parentAddress);
        setTxType(TransactionType.SEND);
        partnerTypeProperty().set(PartnerType.SENDER);

      
        
   
        JsonElement nanoErgsElement = json.get("nanoErgs");
        JsonElement feeAmountElement = json.get("feeAmount");
        JsonElement tokensElement = json.get("tokens");
        JsonElement numConfirmationsElement = json.get("numConfirmations");
        JsonElement explorerUrlElement = json.get("explorerUrl");
        JsonElement nodeUrlElement = json.get("nodeUrl");
        JsonElement statusElement = json.get("status");
        JsonElement timeStampElement = json.get("timeStamp");
        JsonElement recipientAddressElement = json.get("recipientAddress");

        if(timeStampElement != null && timeStampElement.isJsonPrimitive()){
            setTimeStamp(timeStampElement.getAsLong());
        }else{
            setTimeStamp(System.currentTimeMillis());
        }

        ErgoAmount parentAmount = new ErgoAmount(nanoErgsElement.getAsLong(), getParentAddress().getNetworkType());



        setErgoAmount(parentAmount);
        if(feeAmountElement != null && feeAmountElement.isJsonObject()){
                 
            setFeeAmount(new ErgoAmount(feeAmountElement.getAsJsonObject().get("amount").getAsLong(), getParentAddress().getNetworkType()));
            
        }else{
            setFeeAmount(new ErgoAmount(0, getParentAddress().getNetworkType()));
        }


      

        m_explorerUrl = explorerUrlElement != null && explorerUrlElement.isJsonPrimitive() ? explorerUrlElement.getAsString() : "";
        m_nodeUrl = nodeUrlElement != null && nodeUrlElement.isJsonPrimitive() ? nodeUrlElement.getAsString() : "";
        JsonArray tokensArray = tokensElement != null && tokensElement.isJsonArray() ? tokensElement.getAsJsonArray() : new JsonArray();
        int tokensArrayLength = tokensArray.size();

        PriceAmount[] tokenAmounts = new PriceAmount[tokensArrayLength];
        for(int i = 0; i < tokensArrayLength ; i ++ ){
            JsonElement tokenElement = tokensArray.get(i);
            
            PriceAmount tokenAmount = tokenElement != null && tokenElement.isJsonObject() ? new PriceAmount(tokenElement.getAsJsonObject()) : UNKNOWN_PRICE_AMOUNT;
          
            tokenAmounts[i] = tokenAmount;
        }
        String status = statusElement != null && statusElement.isJsonPrimitive() ? statusElement.getAsString() : TransactionStatus.PENDING;
        statusProperty().set(status);

        setTokens(tokenAmounts);

        if(recipientAddressElement != null && recipientAddressElement.isJsonObject()){
            m_receipientAddress = new AddressInformation(recipientAddressElement.getAsJsonObject());
            setTxPartnerArray( new ErgoTransactionPartner[]{new ErgoTransactionPartner(getParentAddress().getAddressString(), PartnerType.SENDER, parentAmount, tokenAmounts),  new ErgoTransactionPartner(m_receipientAddress.getAddressString(), PartnerType.RECEIVER, parentAmount, tokenAmounts)});
        }else{
            m_receipientAddress = new AddressInformation("Unknown");
            setTxPartnerArray( new ErgoTransactionPartner[]{new ErgoTransactionPartner(getParentAddress().getAddressString(), PartnerType.SENDER, parentAmount, tokenAmounts), new ErgoTransactionPartner("Unknown", PartnerType.RECEIVER, new ErgoAmount(0, getNetworkType()))});
        }
        
        if(numConfirmationsElement != null && numConfirmationsElement.isJsonPrimitive()){
            numConfirmationsProperty().set(numConfirmationsElement.getAsLong());
        }


    }

    

 
    public void open(){
       
    }
    /*
    public void showErgoTxStage(){
        if(getStage() == null){
           

    
  
            VBox layoutVBox = new VBox();
            Scene txScene = new Scene(layoutVBox, getStageWidth(), getStageHeight());
            txScene.getStylesheets().add("/css/startWindow.css");

            String titleString = "Send - " + statusProperty().get() +": " + getErgoAmount().toString() + " - " + getTxId();

            Stage stage = new Stage();
            stage.getIcons().add(ErgoWallets.getAppIcon());
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setTitle(titleString);

            Button closeBtn = new Button();

            HBox titleBox = App.createTopBar(ErgoWallets.getSmallAppIcon(), titleString, closeBtn, stage);

            setStage(stage);

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


            BufferedButton deleteBtn = new BufferedButton("/assets/trash-outline-white-30.png", App.MENU_BAR_IMAGE_WIDTH);
            deleteBtn.setTooltip(deleteTooltip);
            deleteBtn.setOnAction(e->{
                getParentAddress().removeTransaction(getTxId());
                closeBtn.fire();
            });

            HBox menuBar = new HBox(deleteBtn, spacer, rightSideMenu);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 5));


            Text txText = new Text();
            txText.setFont(App.txtFont);
            txText.setFill(App.txtColor);
            txText.textProperty().bind(Bindings.concat("Send - ", statusProperty(), Bindings.when(numConfirmationsProperty().greaterThan(1)).then( Bindings.concat(" (",numConfirmationsProperty()," confirmations)")).otherwise("") ));

            
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

            TextField fromField = new TextField( getParentAddress().getAddressString());
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

            TextField toField = new TextField( m_receipientAddress.getAddressString());
            toField.setEditable(false);
            toField.setId("formFieldSmall");
            HBox.setHgrow(toField, Priority.ALWAYS);

            HBox toBox = new HBox(toText, toField);
            HBox.setHgrow(toBox, Priority.ALWAYS);
            toBox.setAlignment(Pos.CENTER_LEFT);
            toBox.setPadding(new Insets(0,15,0,10));
            toBox.setMinHeight(30);

            AmountConfirmBox ergoAmountBox = new AmountConfirmBox(getErgoAmount(), getFeeAmount(), txScene);
            HBox.setHgrow(ergoAmountBox, Priority.ALWAYS);
            ergoAmountBox.priceQuoteProperty().bind(getParentAddress().getAddressesData().currentPriceQuoteProperty());


            HBox amountBoxPadding = new HBox(ergoAmountBox);
            amountBoxPadding.setPadding(new Insets(10, 10, 0, 10));

            AmountBoxes amountBoxes = new AmountBoxes();
            amountBoxes.setPadding(new Insets(5, 10, 5, 0));
            amountBoxes.setAlignment(Pos.TOP_LEFT);

    
            if (getTokens() != null && getTokens().length > 0) {
                PriceAmount[] tokens = getTokens();
                int numTokens = tokens.length;
                for (int i = 0; i < numTokens; i++) {
                    PriceAmount tokenAmount = tokens[i];

                    AmountConfirmBox confirmBox = new AmountConfirmBox(tokenAmount,null, txScene);
                    amountBoxes.add(confirmBox);
                    
                }
            }
            
  
            
            VBox boxesVBox = new VBox(amountBoxPadding, amountBoxes);
            HBox.setHgrow(boxesVBox, Priority.ALWAYS);

            ScrollPane scrollPane = new ScrollPane(boxesVBox);
            scrollPane.setPadding(new Insets(10, 0, 5, 0));
     

            VBox detailsBox = new VBox(fromBox, toBox, scrollPane);
            HBox.setHgrow(detailsBox, Priority.ALWAYS);
            detailsBox.setId("darkBox");
            detailsBox.setPadding(new Insets(10,10,0,10));

            VBox bodyDetailsBox = new VBox(txBox, txidBox, detailsBox);
            HBox.setHgrow(bodyDetailsBox, Priority.ALWAYS);
            bodyDetailsBox.setId("bodyBox");
            bodyDetailsBox.setPadding(new Insets(0,10,10,10));

            VBox bodyBox = new VBox(bodyDetailsBox);
            bodyBox.setPadding(new Insets(4));
            bodyBox.setId("bodyBox");

            Region menuBarRegion = new Region();
            menuBarRegion.setMinHeight(4);

            VBox bodyPaddingBox = new VBox(menuBar, menuBarRegion, bodyBox);

            
            bodyPaddingBox.setPadding(new Insets(0,4,4,4));
            layoutVBox.getChildren().addAll(titleBox,bodyPaddingBox);

            stage.setScene(txScene);
            
            getParentAddress().getAddressesData().getWalletData().getErgoWallets().getErgoNetworkData().addNetworkListener((ListChangeListener.Change<? extends NoteInterface> c) -> {
               
                getAvailableExplorerMenu.run();
            });

            getAvailableExplorerMenu.run();

            
            scrollPane.prefViewportWidthProperty().bind(txScene.widthProperty().subtract(60));
            scrollPane.prefViewportHeightProperty().bind(txScene.heightProperty().subtract(270));
            amountBoxes.minHeightProperty().bind(scrollPane.prefViewportHeightProperty().subtract(60));
            amountBoxes.prefWidthProperty().bind(txScene.widthProperty().subtract(60));

            java.awt.Rectangle rect = getParentAddress().getAddressesData().getWalletData().getNetworksData().getDefaultWindowBounds();

            ResizeHelper.addResizeListener(stage, 200, 250, rect.getWidth(), rect.getHeight());
            
            stage.show();
            closeBtn.setOnAction(e->{
              
                stage.close();
                setStage(null);
                
            });

            stage.setOnCloseRequest(e->closeBtn.fire());
        }else{
            Stage stage = getStage();
            if(stage.isIconified()){
                stage.setIconified(false);
                stage.show();
                stage.toFront();
            }else{
                Platform.runLater(()-> stage.requestFocus());
            }
        }
    }*/

  
   


    public AddressInformation getReceipientAddressInfo(){
        return m_receipientAddress;
    }

    public NetworkType getNetworkType(){
        return getParentAddress().getNetworkType();
    }

    public String getExplorerUrl(){
        return m_explorerUrl;
    }

    public String getNodeUrl(){
        return m_nodeUrl;
    }


    public boolean isSent(){
        return getTxId() != null;
    }
    /*
   @Override
   public void update(JsonObject json){
        if(json != null){   
            JsonElement numConfirmationsElement = json.get("numConfirmations");
            JsonElement timeStampElement = json.get("timestamp");
            if(numConfirmationsElement != null && numConfirmationsElement.isJsonPrimitive()){
                long numConfirmations = numConfirmationsElement.getAsLong();

                if(numConfirmations > 0){
                    statusProperty().set(TransactionStatus.CONFIRMED);
                    numConfirmationsProperty().set(numConfirmations);
                
                }
            }

            setTimeStamp( timeStampElement != null && timeStampElement.isJsonPrimitive() ? timeStampElement.getAsLong() : getTimeStamp());
            getLastUpdated().set(LocalDateTime.now());
        }
   } */

    @Override
    public JsonObject getJsonObject(){
      
        JsonObject json = new JsonObject();
        json.addProperty("txId", getTxId());
        json.addProperty("parentAddress",getParentAddress().getAddressString());
        json.addProperty("timeStamp", getTimeStamp());
        json.addProperty("txType",  getTxType());
        json.addProperty("status", statusProperty().get());
        json.addProperty("numConfirmations", numConfirmationsProperty().getValue());

        json.addProperty("explorerUrl", m_explorerUrl);
        json.addProperty("nodeUrl", m_nodeUrl);
        json.add("feeAmount", getFeeAmount().getJsonObject());
        json.addProperty("nanoErgs", getErgoAmount().getLongAmount());
        json.add("tokens", getTokenJsonArray());
        json.add("recipientAddress", m_receipientAddress.getJsonObject());
        
        
        return json;

    }
    
    
}
