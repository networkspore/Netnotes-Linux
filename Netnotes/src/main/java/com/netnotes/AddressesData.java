package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;

import org.ergoplatform.appkit.ErgoToken;
import org.ergoplatform.appkit.InputBoxesSelectionException;
import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.ErgoClient;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.RestApiErgoClient;
import org.ergoplatform.appkit.SignedTransaction;
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

        if(isTokens && ergoTokens != null){
            m_tokensListProperty.set(ergoTokens.getTokensList(networkType));
        }
        m_isErgoTokens.addListener((obs,oldval,newval)->{
            if(newval){
                ErgoTokens ergTokens = (ErgoTokens) ergNetData.getNetwork(ErgoTokens.NETWORK_ID);
                if(ergTokens != null){
                    m_tokensListProperty.set(ergTokens.getTokensList(networkType));
                }
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
                AddressData addressData = new AddressData(name, index, address,m_wallet, m_networkType, this);
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
                            AddressData addressData = new AddressData(addressName, nextAddressIndex, address,m_wallet, m_networkType, this);
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




}
