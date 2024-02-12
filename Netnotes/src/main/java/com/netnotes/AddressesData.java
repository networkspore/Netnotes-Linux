package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;


import org.ergoplatform.appkit.ErgoToken;
import org.ergoplatform.appkit.InputBoxesSelectionException;
import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.ErgoClient;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.RestApiErgoClient;
import org.ergoplatform.appkit.UnsignedTransaction;

import com.netnotes.ErgoTransaction.TransactionStatus;
import com.satergo.Wallet;
import com.satergo.WalletKey;
import com.satergo.WalletKey.Failure;
import com.satergo.ergo.ErgoInterface;

import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;

import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
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

public class AddressesData {

    private File logFile = new File("netnotes-log.txt");
    private final NetworkType m_networkType;

    private final Wallet m_wallet;
    private ErgoWalletData m_walletData;

    private SimpleObjectProperty<AddressData> m_selectedAddressData = new SimpleObjectProperty<AddressData>(null);

    private SimpleObjectProperty<ErgoAmount> m_totalErgoAmount = new SimpleObjectProperty<>(null);

    private SimpleObjectProperty<ErgoTokensList> m_tokensListProperty = new SimpleObjectProperty<>(null);
    private SimpleObjectProperty<ErgoExplorerList> m_ergoExplorerList = new SimpleObjectProperty<>(null);

    private ObservableList<AddressData> m_addressDataList = FXCollections.observableArrayList();

    private SimpleObjectProperty<ErgoMarketsData> m_selectedMarketData = new SimpleObjectProperty<ErgoMarketsData>(null);
    private SimpleObjectProperty<ErgoNodeData> m_selectedNodeData = new SimpleObjectProperty<ErgoNodeData>(null);
    private SimpleObjectProperty<ErgoExplorerData> m_selectedExplorerData = new SimpleObjectProperty<ErgoExplorerData>(null);
    private SimpleBooleanProperty m_isErgoTokens = new SimpleBooleanProperty();

    private SimpleObjectProperty<PriceQuote> m_currentQuote = new SimpleObjectProperty<>(null);
 


    private Stage m_promptStage = null;

    public AddressesData(String id, Wallet wallet, ErgoWalletData walletData, NetworkType networkType) {

        m_wallet = wallet;
        m_walletData = walletData;
        m_networkType = networkType;


        ErgoNetworkData ergNetData = walletData.getErgoWallets().getErgoNetworkData();

        ErgoNodes ergoNodes = (ErgoNodes) ergNetData.getNetwork(ErgoNodes.NETWORK_ID);
        ErgoExplorers ergoExplorer = (ErgoExplorers) ergNetData.getNetwork(ErgoExplorers.NETWORK_ID);
        ErgoTokens ergoTokens = (ErgoTokens) ergNetData.getNetwork(ErgoTokens.NETWORK_ID);
        ErgoMarkets ergoMarkets = (ErgoMarkets) ergNetData.getNetwork(ErgoMarkets.NETWORK_ID);

        if (ergoNodes != null && walletData.getNodesId() != null) {
            m_selectedNodeData.set(ergoNodes.getErgoNodesList().getErgoNodeData(walletData.getNodesId()));
        }
        if(ergoExplorer != null){
            ErgoExplorerList explorerList = ergoExplorer.getErgoExplorersList();
            m_ergoExplorerList.set(explorerList);
        
            if (walletData.getExplorerId() != null) {
                String explorerId = walletData.getExplorerId();
                if(explorerId != null){
                    ErgoExplorerData explorerData = explorerList.getErgoExplorerData(explorerId);
                    if(explorerData != null){
                        m_selectedExplorerData.set(explorerData);
                    }else{
                        ErgoExplorerData defaultExplorerData = explorerList.getErgoExplorerData(explorerList.defaultIdProperty().get());
                        m_selectedExplorerData.set(defaultExplorerData);
                    }
                }else{
                    m_selectedExplorerData.set(null);
                }
            }
        }
        boolean isTokens= ergoTokens != null && walletData.isErgoTokens();
        m_isErgoTokens.set(isTokens);

        if(isTokens){
            m_tokensListProperty.set(ergoTokens.getTokensList(networkType));
        }
        m_isErgoTokens.addListener((obs,oldval,newval)->{
            if(newval){
                m_tokensListProperty.set(ergoTokens.getTokensList(networkType));
            }else{
                m_tokensListProperty.set(null);
            }
        });

        if(ergoMarkets != null && walletData.getMarketsId()!= null){
            String marketId = walletData.getMarketsId();
            ErgoMarketsData marketData = ergoMarkets.getErgoMarketsList().getMarketsData(marketId);
         
            if(marketData != null){
                selectedMarketData().set(marketData);
                marketData.start();
                m_currentQuote.bind(marketData.priceQuoteProperty());
            }
        }

        m_wallet.myAddresses.forEach((index, name) -> {

            try {

                Address address = wallet.publicAddress(m_networkType, index);
                AddressData addressData = new AddressData(name, index, address, m_networkType, this);
                addAddressData(addressData);

            } catch (Failure e) {
                try {
                    Files.writeString(logFile.toPath(), "\nAddressesData - address failure: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
            }

        });
        selectedAddressDataProperty().set(m_addressDataList.get(0));
        calculateCurrentTotal();
     
    }

   
    public SimpleObjectProperty<ErgoTokensList> tokensListProperty(){
        return m_tokensListProperty;
    }





    public ErgoWalletData getWalletData() {
        return m_walletData;
    }


    public SimpleObjectProperty<ErgoNodeData> selectedNodeData() {
        return m_selectedNodeData;
    }

    public SimpleObjectProperty<ErgoMarketsData> selectedMarketData() {
        return m_selectedMarketData;
    }

    public SimpleObjectProperty<ErgoExplorerData> selectedExplorerData() {
        return m_selectedExplorerData;
    }

    public SimpleBooleanProperty isErgoTokensProperty() {
        return m_isErgoTokens;
    }

    public void closeAll() {
        for (int i = 0; i < m_addressDataList.size(); i++) {
            AddressData addressData = m_addressDataList.get(i);
            addressData.close();
        }
    }


    public SimpleObjectProperty<AddressData> selectedAddressDataProperty() {
        return m_selectedAddressData;
    }

    public void addAddress() {

        if (m_promptStage == null) {

            m_promptStage = new Stage();
            m_promptStage.initStyle(StageStyle.UNDECORATED);
            m_promptStage.getIcons().add(new Image("/assets/git-branch-outline-white-30.png"));
            m_promptStage.setTitle("Add Address - " + m_walletData.getName() + " - Ergo Wallets");

            TextField textField = new TextField();
            Button closeBtn = new Button();

            App.showGetTextInput("Address name", "Add Address", new Image("/assets/git-branch-outline-white-240.png"), textField, closeBtn, m_promptStage);
            closeBtn.setOnAction(e -> {
                m_promptStage.close();
                m_promptStage = null;
            });
            m_promptStage.setOnCloseRequest(e -> {
                closeBtn.fire();
            });
            textField.setOnKeyPressed(e -> {

                KeyCode keyCode = e.getCode();

                if (keyCode == KeyCode.ENTER) {
                    String addressName = textField.getText();
                    if (!addressName.equals("")) {
                        int nextAddressIndex = m_wallet.nextAddressIndex();
                        m_wallet.myAddresses.put(nextAddressIndex, addressName);

                        try {

                            Address address = m_wallet.publicAddress(m_networkType, nextAddressIndex);
                            AddressData addressData = new AddressData(addressName, nextAddressIndex, address, m_networkType, this);
                            addAddressData(addressData);
                          
                        } catch (Failure e1) {

                            Alert a = new Alert(AlertType.ERROR, e1.toString(), ButtonType.OK);
                            a.showAndWait();
                        }

                    }
                    closeBtn.fire();
                }
            });
        } else {
            if (m_promptStage.isIconified()) {
                m_promptStage.setIconified(false);
            } else {
                m_promptStage.show();
                Platform.runLater(() -> m_promptStage.requestFocus());
            }
        }

    }

    private void addAddressData(AddressData addressData) {
        m_addressDataList.add(addressData);
        addressData.ergoAmountProperty().addListener((obs, oldval, newval) -> {
            long oldNanoErgs = oldval == null ? 0 : oldval.getLongAmount();

            long newNanoErgs = newval == null ? 0 : newval.getLongAmount();

            if (oldNanoErgs != newNanoErgs) {
                calculateCurrentTotal();
            }
        });

    }

    public VBox getAddressesBox(Scene scene) {

        VBox addressBox = new VBox();

        Runnable updateAdressBox = () ->{
            addressBox.getChildren().clear();
            for (int i = 0; i < m_addressDataList.size(); i++) {
                AddressData addressData = m_addressDataList.get(i);

                //addressData.prefWidthProperty().bind(m_addressBox.widthProperty());
                
                addressBox.getChildren().add(addressData.getAddressBox());
            }
        };
        updateAdressBox.run();
        m_addressDataList.addListener((ListChangeListener.Change<? extends AddressData> c) ->updateAdressBox.run());

        addressBox.prefWidthProperty().bind(scene.widthProperty().subtract(30)); 

        return addressBox;
    }

    /*public void updateAddress() {
        for (int i = 0; i < m_addressDataList.size(); i++) {
            AddressData addressData = m_addressDataList.get(i);

            addressData.updateBalance();
        }
    }*/

    /*public void startBalanceUpdates() {
       
        try {
            if (m_balanceExecutor != null) {
                stopBalanceUpdates();
            }

            m_balanceExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });

            m_balanceExecutor.scheduleAtFixedRate(() -> {
                Platform.runLater(() -> updateBalance());
            }, 0, UPDATE_PERIOD, TimeUnit.SECONDS);
        } catch (Exception e) {
            Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
            a.show();
        }
   
    }*/
   

    public void shutdown() {
        m_currentQuote.unbind();
        ErgoTokensList tokensList = tokensListProperty().get();
        ErgoMarketsData marketsData = m_selectedMarketData.get();
    
        if(marketsData != null){
            marketsData.shutdown();
        }

        if(tokensList != null){
            tokensList.shutdown();
        }

    }


    public boolean updateSelectedExplorer(ErgoExplorerData ergoExplorerData) {
        ErgoExplorerData previousSelectedExplorerData = m_selectedExplorerData.get();

        if (ergoExplorerData == null && previousSelectedExplorerData == null) {
            return false;
        }

        m_selectedExplorerData.set(ergoExplorerData);

        /* if (previousSelectedExplorerData != null) {
        
           //update services if implemented

        }
        
        }*/
        return true;
    }

    public ErgoAmount getTotalTokenErgs(){
        BigDecimal totalTokenErgs = BigDecimal.ZERO;
        for (int i = 0; i < m_addressDataList.size(); i++) {
            AddressData addressData = m_addressDataList.get(i);
           
            BigDecimal tokenErgs = addressData.getTotalTokenErgBigDecimal();
            totalTokenErgs = totalTokenErgs.add(tokenErgs);
            
        
        }   
   
        
        return new ErgoAmount(totalTokenErgs, m_networkType);
    }

    public void calculateCurrentTotal() {
        SimpleLongProperty totalNanoErgs = new SimpleLongProperty();
       
        for (int i = 0; i < m_addressDataList.size(); i++) {
            AddressData addressData = m_addressDataList.get(i);
            ErgoAmount ergoAmount = addressData.ergoAmountProperty().get();
            
        
            totalNanoErgs.set(totalNanoErgs.get() + (ergoAmount == null ? 0 : ergoAmount.getLongAmount()));

        }

        m_totalErgoAmount.set(new ErgoAmount(totalNanoErgs.get(), m_networkType));
    }

    public SimpleObjectProperty<ErgoAmount> totalErgoTokenAmountProperty(){
        return m_totalErgoTokenAmountProperty;
    }
    private final SimpleObjectProperty<ErgoAmount> m_totalErgoTokenAmountProperty = new SimpleObjectProperty<>(null);

    public SimpleObjectProperty<ErgoAmount> totalErgoAmountProperty() {
        return m_totalErgoAmount;
    }

    public Scene getSendScene(Scene parentScene, Stage parentStage, Button closeBtn) {

        if (selectedAddressDataProperty().get() == null) {
            if (m_addressDataList.size() == 1) {
                selectedAddressDataProperty().set(m_addressDataList.get(0));
            }
        }

        Runnable requiredErgoNodes = () -> {
            if (m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID) == null) {
                Alert a = new Alert(AlertType.NONE, "Would you like to install Ergo Nodes?\n\n", ButtonType.YES, ButtonType.NO);
                a.setTitle("Required: Ergo Nodes");
                a.setHeaderText("Required: Ergo Nodes");
                a.initOwner(parentStage);
                Optional<ButtonType> result = a.showAndWait();
                if (result != null && result.isPresent() && result.get() == ButtonType.YES) {
                    m_walletData.getErgoWallets().getErgoNetworkData().installNetwork(ErgoNodes.NETWORK_ID);
                    if (selectedNodeData().get() == null) {
                        ErgoNodes ergoNodes = (ErgoNodes) m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID);
                        if (ergoNodes != null && ergoNodes.getErgoNodesList().defaultNodeIdProperty() != null) {
                            ErgoNodeData ergoNodeData = ergoNodes.getErgoNodesList().getErgoNodeData(ergoNodes.getErgoNodesList().defaultNodeIdProperty().get());
                            if (ergoNodeData != null) {
                                selectedNodeData().set(ergoNodeData);
                            }
                        }
                    }
                }

            }
        };
        requiredErgoNodes.run();

        SimpleStringProperty babbleTokenId = new SimpleStringProperty(null);

        String oldStageName = parentStage.getTitle();

        String stageName = "Send - " + m_walletData.getName() + " - (" + m_networkType + ")";

        parentStage.setTitle(stageName);

        VBox layoutBox = new VBox();
        Scene sendScene = new Scene(layoutBox, 800, 600);
        sendScene.setFill(null);
        sendScene.getStylesheets().add("/css/startWindow.css");

        Button maximizeBtn = new Button();

        HBox titleBox = App.createTopBar(ErgoWallets.getSmallAppIcon(), stageName, maximizeBtn, closeBtn, parentStage);
        maximizeBtn.setOnAction(e -> {
            parentStage.setMaximized(!parentStage.isMaximized());
        });
        Tooltip backTip = new Tooltip("Back");
        backTip.setShowDelay(new javafx.util.Duration(100));
        backTip.setFont(App.txtFont);

        BufferedButton backButton = new BufferedButton("/assets/return-back-up-30.png", App.MENU_BAR_IMAGE_WIDTH);
        backButton.setId("menuBtn");
        backButton.setTooltip(backTip);
        backButton.setOnAction(e -> {
            parentStage.setScene(parentScene);
            parentStage.setTitle(oldStageName);
            // ResizeHelper.addResizeListener(parentStage, WalletData.MIN_WIDTH, WalletData.MIN_HEIGHT, m_walletData.getMaxWidth(), m_walletData.getMaxHeight());
        });

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

        SimpleObjectProperty<ErgoExplorerData> ergoExplorerProperty = new SimpleObjectProperty<>(selectedExplorerData().get());

        Runnable updateExplorerBtn = () -> {
            ErgoExplorers ergoExplorers = (ErgoExplorers) m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);

            ErgoExplorerData explorerData = ergoExplorerProperty.get();

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
        Runnable getAvailableExplorerMenu = () -> {

            ErgoExplorers ergoExplorers = (ErgoExplorers) m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);
            if (ergoExplorers != null) {
                explorerBtn.setId("menuBtn");
                ergoExplorers.getErgoExplorersList().getMenu(explorerBtn, ergoExplorerProperty);
            } else {
                explorerBtn.getItems().clear();
                explorerBtn.setId("menuBtnDisabled");

            }
            updateExplorerBtn.run();
        };

        ergoExplorerProperty.addListener((obs, oldval, newval) -> {
            m_walletData.setExplorer(newval == null ? null : newval.getId());
            updateSelectedExplorer(newval);
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

        BufferedMenuButton tokensBtn = new BufferedMenuButton("/assets/diamond-30.png", imageWidth);
        tokensBtn.setPadding(new Insets(2, 0, 0, 0));

        Runnable updateTokensMenu = () -> {
            tokensBtn.getItems().clear();
            ErgoTokens ergoTokens = (ErgoTokens) m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoTokens.NETWORK_ID);
            boolean isEnabled = isErgoTokensProperty().get();

            if (ergoTokens != null) {
                tokensBtn.setId("menuBtn");
                MenuItem tokensEnabledItem = new MenuItem("Enabled" + (isEnabled ? " (selected)" : ""));
                tokensEnabledItem.setOnAction(e -> {
                    isErgoTokensProperty().set(true);
                });

                MenuItem tokensDisabledItem = new MenuItem("Disabled" + (isEnabled ? "" : " (selected)"));
                tokensDisabledItem.setOnAction(e -> {
                    isErgoTokensProperty().set(false);
                });

                if (isEnabled) {
                    tokensTip.setText("Ergo Tokens: Enabled");
                    tokensEnabledItem.setId("selectedMenuItem");
                } else {
                    tokensTip.setText("Ergo Tokens: Disabled");
                    tokensDisabledItem.setId("selectedMenuItem");
                }
                tokensBtn.getItems().addAll(tokensEnabledItem, tokensDisabledItem);
            } else {
                tokensBtn.setId("menuBtnDisabled");
                MenuItem tokensInstallItem = new MenuItem("(install 'Ergo Tokens')");
                tokensInstallItem.setOnAction(e -> {
                    m_walletData.getErgoWallets().getErgoNetworkData().showwManageStage();
                });
                tokensTip.setText("(install 'Ergo Tokens')");
            }

        };

        isErgoTokensProperty().addListener((obs, oldval, newval) -> {
            m_walletData.setIsErgoTokens(newval);
            updateTokensMenu.run();
        });

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

        HBox menuBar = new HBox(backButton, spacer, rightSideMenu);
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

        String nullAddressImageString = "/assets/selectAddress.png";
        Image nullAddressImg = new Image(nullAddressImageString);

        MenuButton fromAddressBtn = new MenuButton();
        fromAddressBtn.setMaxHeight(40);
        fromAddressBtn.setContentDisplay(ContentDisplay.LEFT);
        fromAddressBtn.setAlignment(Pos.CENTER_LEFT);

        Runnable updateAvaliableAddresses = () -> {
            fromAddressBtn.getItems().clear();
            for (AddressData addressItem : m_addressDataList) {

                MenuItem addressMenuItem = new MenuItem(addressItem.getAddressString());
                addressMenuItem.textProperty().bind(addressItem.textProperty());
                Image addressImage = addressItem.getImageProperty().get();
                addressMenuItem.setGraphic(IconButton.getIconView(addressImage, addressImage.getWidth()));

                addressItem.getImageProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        addressMenuItem.setGraphic(IconButton.getIconView(newVal, newVal.getWidth()));
                    }
                });

                fromAddressBtn.getItems().add(addressMenuItem);

                addressMenuItem.setOnAction(actionEvent -> {
                    m_selectedAddressData.set(addressItem);
                });
            }
        };
        updateAvaliableAddresses.run();

        SimpleObjectProperty<Image> addressImageProperty = new SimpleObjectProperty<>(new Image(nullAddressImageString));

        // fromAddressBtn.setPadding(new Insets(2, 5, 2, 0));
        Runnable updateAddressBtn = () -> {
            AddressData addressData = selectedAddressDataProperty().get();

            if (addressData != null) {
                addressImageProperty.bind(addressData.getImageProperty());
                fromAddressBtn.setText(addressData.getButtonText());
            } else {
                addressImageProperty.unbind();
                addressImageProperty.set(nullAddressImg);

            }

        };

        addressImageProperty.addListener((obs, oldval, newval) -> {
            ImageView imgView = newval != null ? IconButton.getIconView(newval, newval.getWidth()) : IconButton.getIconView(nullAddressImg, nullAddressImg.getWidth());
            fromAddressBtn.setGraphic(imgView);
        });

        updateAddressBtn.run();

        Text toText = new Text("To     ");
        toText.setFont(App.txtFont);
        toText.setFill(App.txtColor);

        AddressBox toAddressEnterBox = new AddressBox(new AddressInformation(""), sendScene, m_networkType);
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
        statusBoxBtn.setOnAction(e -> {
            nodesBtn.show();
        });

        HBox nodeStatusBox = new HBox();
        nodeStatusBox.setId("bodyRowBox");
        nodeStatusBox.setPadding(new Insets(0, 0, 0, 0));
        nodeStatusBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nodeStatusBox, Priority.ALWAYS);
        nodeStatusBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            nodesBtn.show();
        });

        Runnable updateNodeBtn = () -> {
            ErgoNodes ergoNodes = (ErgoNodes) m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID);
            ErgoNodeData nodeData = selectedNodeData().get();
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
            ErgoNodes ergoNodes = (ErgoNodes) m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID);
            if (ergoNodes != null) {
                ergoNodes.getErgoNodesList().getMenu(nodesBtn, selectedNodeData());
                nodesBtn.setId("menuBtn");
            } else {
                nodesBtn.getItems().clear();
                nodesBtn.setId("menuBtnDisabled");

            }
            updateNodeBtn.run();
        };

        selectedNodeData().addListener((obs, oldval, newval) -> {
            updateNodeBtn.run();

            m_walletData.setNodesId(newval == null ? null : newval.getId());

        });
        m_walletData.getErgoWallets().getErgoNetworkData().addNetworkListener((ListChangeListener.Change<? extends NoteInterface> c) -> {
            getAvailableNodeMenu.run();
            getAvailableExplorerMenu.run();
           // getAvailableMarketsMenu.run();
            updateTokensMenu.run();
        });

        getAvailableExplorerMenu.run();
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

        AmountSendBox ergoAmountBox = new AmountSendBox(new ErgoAmount(0, m_networkType), sendScene, true);
        ergoAmountBox.priceQuoteProperty().bind(m_currentQuote);
        ergoAmountBox.isFeeProperty().set(true);

        ergoAmountBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (selectedAddressDataProperty().get() == null) {

                fromAddressBtn.show();
            }
        });

        HBox.setHgrow(ergoAmountBox, Priority.ALWAYS);

        // addTokenBtn.setOnAction(e->addTokenBtn.show());
        AmountBoxes amountBoxes = new AmountBoxes();
        amountBoxes.setPadding(new Insets(10, 10, 10, 0));

        amountBoxes.setAlignment(Pos.TOP_LEFT);
        //   amountBoxes.setLastRowItem(addTokenBtn, AmountBoxes.ADD_AS_LAST_ROW);
        amountBoxes.setId("bodyBox");
        //  addTokenBtn.setAmountBoxes(amountBoxes);

        addTokenBtn.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            addTokenBtn.getItems().clear();
            //addTokenBtn.getItems().add(new MenuItem("bob"));

            AddressData addressData = m_selectedAddressData.get();
            if (addressData != null) {

                long balanceTimestamp = System.currentTimeMillis();
                int size = addressData.getConfirmedTokenList().size();
                PriceAmount[] tokenArray = size > 0 ? new PriceAmount[size] : null;
                tokenArray = tokenArray != null ? addressData.getConfirmedTokenList().toArray(tokenArray) : null;
                if (tokenArray != null) {
                    for (int i = 0; i < size; i++) {
                        PriceAmount tokenAmount = tokenArray[i];
                        String tokenId = tokenAmount.getTokenId();

                        if (tokenId != null) {
                            AmountBox isBox = amountBoxes.getAmountBox(tokenId);

                            if (isBox == null) {
                                AmountMenuItem menuItem = new AmountMenuItem(tokenAmount);
                                addTokenBtn.getItems().add(menuItem);
                                menuItem.setOnAction(e1 -> {
                                    PriceAmount menuItemPriceAmount = menuItem.priceAmountProperty().get();
                                    PriceCurrency menuItemPriceCurrency = menuItemPriceAmount.getCurrency();
                                    AmountSendBox newAmountSendBox = new AmountSendBox(new PriceAmount(0, menuItemPriceCurrency), sendScene, true);
                                    newAmountSendBox.balanceAmountProperty().set(menuItemPriceAmount);
                                    newAmountSendBox.setTimeStamp(balanceTimestamp);
                                    amountBoxes.add(newAmountSendBox);
                                });
                            }

                        }
                    }
                }
            }
            if (addTokenBtn.getItems().size() == 0) {
                addTokenBtn.getItems().add(new MenuItem("No tokens to add"));
            }

        });

        addAllTokenBtn.setOnAction(e -> {
            AddressData addressData = m_selectedAddressData.get();
            if (addressData != null) {

                ArrayList<PriceAmount> tokenList = addressData.getConfirmedTokenList();
                long timeStamp = System.currentTimeMillis();

                for (int i = 0; i < tokenList.size(); i++) {
                    PriceAmount tokenAmount = tokenList.get(i);
                    String tokenId = tokenAmount.getTokenId();
                    AmountSendBox existingTokenBox = (AmountSendBox) amountBoxes.getAmountBox(tokenId);
                    if (existingTokenBox == null) {
                        PriceCurrency tokenCurrency = tokenAmount.getCurrency();
                        AmountSendBox tokenAmountBox = new AmountSendBox(new PriceAmount(0, tokenCurrency), sendScene, true);
                        tokenAmountBox.balanceAmountProperty().set(tokenAmount);
                        tokenAmountBox.setTimeStamp(timeStamp);
                        amountBoxes.add(tokenAmountBox);
                    } else {
                        existingTokenBox.setTimeStamp(timeStamp);
                        existingTokenBox.balanceAmountProperty().set(tokenAmount);
                    }
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
                    PriceAmount tokenAmount = tokenBox.priceAmountProperty().get();
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

        Runnable updateErgoMaxBalance = () -> {
            AddressData addressData = m_selectedAddressData.get();

            if (addressData != null) {

                ergoAmountBox.balanceAmountProperty().bind(addressData.ergoAmountProperty());

            } else {
                ergoAmountBox.balanceAmountProperty().unbind();
                ergoAmountBox.balanceAmountProperty().set(null);
            }
        };
        updateErgoMaxBalance.run();

        Runnable updateTokensMaxBalance = () -> {
            AddressData addressData = m_selectedAddressData.get();
            if (addressData != null) {

                for (int i = 0; i < amountBoxes.amountsList().size(); i++) {
                    AmountBox amountBox = amountBoxes.amountsList().get(i);
                    if (amountBox != null && amountBox instanceof AmountSendBox) {
                        AmountSendBox amountSendBox = (AmountSendBox) amountBox;

                        PriceAmount tokenAmount = addressData.getConfirmedTokenAmount(amountSendBox.getTokenId());

                        amountSendBox.balanceAmountProperty().set(tokenAmount);

                    }
                }
            } else {

                for (AmountBox amountBox : amountBoxes.amountsList()) {
                    if (amountBox != null && amountBox instanceof AmountSendBox) {
                        AmountSendBox amountSendBox = (AmountSendBox) amountBox;
                        amountSendBox.balanceAmountProperty().set(null);
                    }
                }
            }
        };
        updateTokensMaxBalance.run();

        Runnable updateBabbleFees = () -> {
            String babbleId = babbleTokenId.get();

            if (babbleId == null) {
                ergoAmountBox.isFeeProperty().set(true);
            }
        };

        updateBabbleFees.run();

        babbleTokenId.addListener((obs, oldval, newval) -> updateBabbleFees.run());

        ChangeListener<? super LocalDateTime> balanceChangeListener = (obs, oldVal, newVal) -> {

            updateTokensMaxBalance.run();
        };

        selectedAddressDataProperty().addListener((obs, oldval, newval) -> {
            updateAddressBtn.run();
            updateErgoMaxBalance.run();

            if (oldval != null) {
                oldval.getLastUpdated().removeListener(balanceChangeListener);
            }
            if (newval != null) {

                newval.getLastUpdated().addListener(balanceChangeListener);
            }
        });

        //  addTokenBtn.prefWidthProperty().bind(amountBoxes.widthProperty());
        Region sendBoxSpacer = new Region();
        HBox.setHgrow(sendBoxSpacer, Priority.ALWAYS);

        Runnable checkAndSend = () -> {

            AddressData addressData = m_selectedAddressData.get();

            if (ergoAmountBox.isNotAvailable()) {
                String insufficentErrorString = "Address does not contain: " + ergoAmountBox.priceAmountProperty().get().toString() + (ergoAmountBox.isFeeProperty().get() ? ("\nAfter paying the send fee of: " + ergoAmountBox.feeAmountProperty().get().toString()) : "");
                Alert a = new Alert(AlertType.NONE, insufficentErrorString, ButtonType.CANCEL);
                a.setTitle("Insuficient Funds");
                a.initOwner(parentStage);
                a.setHeaderText("Insuficient Funds");
                a.show();
                return;
            }

            AddressInformation addressInformation = toAddressEnterBox.addressInformationProperty().get();

            if (addressInformation != null && addressInformation.getAddress() != null) {
                ErgoNodeData ergoNodeData = selectedNodeData().get();
                ErgoExplorerData ergoExplorerData = selectedExplorerData().get();
                AmountBox[] amountBoxArray = amountBoxes.getAmountBoxArray();
                int amountOfTokens = amountBoxArray != null && amountBoxArray.length > 0 ? amountBoxArray.length : 0;
                AmountSendBox[] tokenArray = amountOfTokens > 0 ? new AmountSendBox[amountOfTokens] : null;

                if (amountOfTokens > 0 && amountBoxArray != null && tokenArray != null) {
                    for (int i = 0; i < amountOfTokens; i++) {
                        AmountBox box = amountBoxArray[i];
                        if (box != null && box instanceof AmountSendBox) {
                            AmountSendBox sendBox = (AmountSendBox) box;
                            if (sendBox.isNotAvailable()) {
                                Alert a = new Alert(AlertType.NONE, "Address does not contain: " + sendBox.priceAmountProperty().get().toString(), ButtonType.CANCEL);
                                a.setTitle("Insuficient Funds");
                                a.initOwner(parentStage);
                                a.setHeaderText("Insuficient Funds");
                                a.show();
                                return;
                            } else {

                                tokenArray[i] = sendBox;
                            }
                        }
                    }

                }
                //  String babbleId = babbleTokenId.get();

                //  AmountSendBox feeSendBox = ergoAmountBox;
                PriceAmount feeAmount = ergoAmountBox.feeAmountProperty().get();

                showTxConfirmScene(addressData, addressInformation, ergoNodeData, ergoExplorerData, ergoAmountBox, tokenArray, feeAmount, sendScene, parentStage, () -> closeBtn.fire(), () -> {backButton.fire();});
            } else {
                Alert a = new Alert(AlertType.NONE, "Enter a valid address.", ButtonType.CANCEL);
                a.setTitle("Invalid Receiver Address");
                a.initOwner(parentStage);
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
            requiredErgoNodes.run();
            if (m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID) != null) {
                ErgoNodeData ergoNodeData = selectedNodeData().get();
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
                                a.initOwner(parentStage);
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

        return sendScene;
    }

    public void showTxConfirmScene(AddressData addressData, AddressInformation receiverAddressInformation, ErgoNodeData nodeData, ErgoExplorerData explorerData, AmountSendBox ergoSendBox, AmountSendBox[] tokenAmounts, PriceAmount feeAmount, Scene parentScene, Stage parentStage, Runnable parentClose, Runnable complete) {
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
        String title = "Confirmation - Send - " + getWalletData().getName() + "(" + m_networkType.toString() + ")";
        Button maximizeBtn = new Button();
        Button closeBtn = new Button();

        closeBtn.setOnAction(e -> {
            parentClose.run();
        });

        HBox titleBox = App.createTopBar(ErgoWallets.getSmallAppIcon(), maximizeBtn, closeBtn, parentStage);
        maximizeBtn.setOnAction(e -> {
            parentStage.setMaximized(!parentStage.isMaximized());
        });

        Tooltip backTip = new Tooltip("Back");
        backTip.setShowDelay(new javafx.util.Duration(100));
        backTip.setFont(App.txtFont);

        BufferedButton backButton = new BufferedButton("/assets/return-back-up-30.png", App.MENU_BAR_IMAGE_WIDTH);
        backButton.setId("menuBtn");
        backButton.setTooltip(backTip);
        backButton.setOnAction(e -> {
            parentStage.setScene(parentScene);
            parentStage.setTitle(oldStageName);
        });

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

        AmountConfirmBox ergoAmountBox = new AmountConfirmBox(ergoSendBox.priceAmountProperty().get(),ergoSendBox.isFeeProperty().get() ? feeAmount : null, confirmTxScene);
        HBox.setHgrow(ergoAmountBox, Priority.ALWAYS);
  

        final long feeAmountLong = feeAmount.getLongAmount();
        final long ergoAmountLong = ergoAmountBox.getLongAmount();


        HBox amountBoxPadding = new HBox(ergoAmountBox);
        amountBoxPadding.setPadding(new Insets(10, 10, 0, 10));

        AmountBoxes amountBoxes = new AmountBoxes();
        amountBoxes.setPadding(new Insets(5, 10, 5, 0));
        amountBoxes.setAlignment(Pos.TOP_LEFT);


        if (tokenAmounts != null && tokenAmounts.length > 0) {
            int numTokens = tokenAmounts.length;
            for (int i = 0; i < numTokens; i++) {
                AmountSendBox sendBox = tokenAmounts[i];
                if (sendBox.priceAmountProperty().get().getLongAmount() > 0) {
                    PriceAmount sendAmount = sendBox.priceAmountProperty().get();
                    PriceAmount babbleFeeAmount = feeAmount.getTokenId().equals(sendAmount.getTokenId()) ? feeAmount : null;
                    AmountConfirmBox confirmBox = new AmountConfirmBox(sendAmount, babbleFeeAmount,  confirmTxScene);
                    amountBoxes.add(confirmBox);
                }
            }
        }
     

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

                AmountBox[] amountBoxArray = amountBoxes.getAmountBoxArray();

                int amountOfTokens = amountBoxArray != null && amountBoxArray.length > 0 ? amountBoxArray.length : 0;
                final ErgoToken[] tokenArray = new ErgoToken[amountOfTokens];

                if (amountOfTokens > 0 && amountBoxArray != null && tokenArray != null) {
                    for (int i = 0; i < amountOfTokens; i++) {
                        AmountBox box = amountBoxArray[i];
                        if (box != null && box instanceof AmountConfirmBox) {
                            AmountConfirmBox confirmBox = (AmountConfirmBox) box;

                            tokenArray[i] = confirmBox.getErgoToken();

                        }
                    }

                }

                Task<String> task = new Task<String>() {
                    @Override
                    public String call() throws Exception {

                        ErgoClient ergoClient = RestApiErgoClient.create(nodeUrl, m_networkType,  namedNodeUrl.getApiKey(), explorerUrl);
                    
                        UnsignedTransaction unsignedTx = ErgoInterface.createUnsignedTransaction(
                            ergoClient,
                            m_wallet.addressStream(m_networkType).toList(),
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

                        /* ErgoTransaction ergoTransaction = new ErgoTransaction(addressData, m_wallet, ergoAmountBox.getLongAmount(), receiverAddress, nodeData, explorerData, feeAmount, amountBoxArray);
                         */
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
                                tokens[i] = amountBoxArray[i].priceAmountProperty().get();
                            }  
                        }

                        try{
                            ErgoSimpleSendTx ergTx = new ErgoSimpleSendTx(txId, addressData, receiverAddressInformation, ergoAmountLong, feeAmount, tokens, nodeUrl, explorerUrl,TransactionStatus.PENDING, System.currentTimeMillis());
                            addressData.addWatchedTransaction(ergTx);
                            addressData.selectedTabProperty().set(AddressData.AddressTabs.TRANSACTIONS);
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

        java.awt.Rectangle rect = getWalletData().getNetworksData().getMaximumWindowBounds();

        ResizeHelper.addResizeListener(parentStage, 200, 250, rect.getWidth(), rect.getHeight());

    }

}
