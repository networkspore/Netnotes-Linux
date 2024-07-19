package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;

import com.satergo.Wallet;
import com.satergo.WalletKey.Failure;
import com.utils.Utils;
import com.google.gson.JsonArray;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AddressesData {
    

    
    public final static NetworkInformation NOMARKET = new NetworkInformation("null", "(disabled)","/assets/bar-chart-150.png", "/assets/bar-chart-30.png", "No market selected" );
    public final static NetworkInformation[] ERGO_MARKETS = new NetworkInformation[]{
        NOMARKET,
        SpectrumFinance.getNetworkInformation(), 
        KucoinExchange.getNetworkInformation()
    };
    public final static NetworkInformation[] ERGO_TOKEN_MARKETS= new NetworkInformation[]{ 
        NOMARKET,
        SpectrumFinance.getNetworkInformation() 
    }; 

   

    private File logFile = new File("netnotes-log.txt");
    private final NetworkType m_networkType;

    private final Wallet m_wallet;
    private ErgoWalletData m_walletData;

    private SimpleObjectProperty<AddressData> m_selectedAddressData = new SimpleObjectProperty<AddressData>(null);




    private ObservableList<AddressData> m_addressDataList = FXCollections.observableArrayList();


    private ArrayList<PriceAmount> m_tokenAmounts = new ArrayList<>();
    




   // private SimpleObjectProperty<PriceQuote> m_currentQuote = new SimpleObjectProperty<>(null);
    public final static long POLLING_TIME = 3000;
    public final static int ADDRESS_IMG_HEIGHT = 40;
    public final static int ADDRESS_IMG_WIDTH = 350;
    public final static long QUOTE_TIMEOUT = POLLING_TIME*2;


    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>(LocalDateTime.now());

    

    private Stage m_promptStage = null;

    

    private ScheduledExecutorService m_schedualedExecutor = null;
    private ScheduledFuture<?> m_scheduledFuture = null;


    private long m_balanceTimestamp = 0;
    

    public AddressesData(String id, Wallet wallet, ErgoWalletData walletData, NetworkType networkType) {
  
        m_wallet = wallet;
        m_walletData = walletData;
        m_networkType = networkType;
      

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

        start();
    }
   

    public ErgoNetworkData getErgoNetworkdata(){
        return m_walletData.getErgoWallets().getErgoNetworkData();
    }

    public JsonArray getAddressesJson(){
        JsonArray json = new JsonArray();
        
        for(AddressData addressData : m_addressDataList){
            json.add(addressData.getAddressJson());
        }
        return json;
    }

 
    
    private EventHandler<WorkerStateEvent> m_updateEvent;

    private void start(){
        if(m_schedualedExecutor == null){
            m_schedualedExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });
        }


        
        m_updateEvent = (onSucceded)->{

            for(int i = 0; i < m_addressDataList.size(); i++){
                AddressData addressData = m_addressDataList.get(i);

                addressData.updateBalance();
            }
            
        };
        
        m_scheduledFuture = m_schedualedExecutor.scheduleAtFixedRate(()->{
            Utils.returnObject(System.currentTimeMillis(),m_updateEvent, null);
        },0, POLLING_TIME, TimeUnit.MILLISECONDS);
       
    }

    public void stop(){
        
        if(m_scheduledFuture != null && !m_scheduledFuture.isDone()){
            m_scheduledFuture.cancel(false);
        }
    }

    public SimpleObjectProperty<LocalDateTime> balanceUpdatedProperty(){
        return m_lastUpdated;
    }

    public ErgoWalletData getWalletData() {
        return m_walletData;
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
                if(!m_promptStage.isShowing()){
                    m_promptStage.show();
                }else{
                    Platform.runLater(() -> m_promptStage.toBack());
                    Platform.runLater(() -> m_promptStage.toFront());
                }
                
            }
        }

    }
    public BigDecimal getPrice() {

        return BigDecimal.ZERO;//getValid() ? m_addressesData.selectedMarketData().get().priceQuoteProperty().get().getDoubleAmount()  : 0.0;
    }


    private void addAddressData(AddressData addressData) {
        m_addressDataList.add(addressData);
        
    }

    /*public VBox getAddressesBox(Button shutdown, Button itemShutdown) {

        VBox addressBox = new VBox();
        
        Runnable updateAdressBox = () ->{
            addressBox.getChildren().clear();
            for (int i = 0; i < m_addressDataList.size(); i++) {
                AddressData addressData = m_addressDataList.get(i);

                //addressData.prefWidthProperty().bind(m_addressBox.widthProperty());
                
                addressBox.getChildren().add(addressData.getAddressBox(itemShutdown));
            }
        };
        updateAdressBox.run();
        ListChangeListener<AddressData> addressDataListListener = (ListChangeListener.Change<? extends AddressData> c) ->updateAdressBox.run();
        m_addressDataList.addListener(addressDataListListener);

        shutdown.setOnAction(e->{
            m_addressDataList.removeListener(addressDataListListener);
        });
 
        return addressBox;
    }*/

  
    public PriceAmount getTokenAmount(String tokenId){
        for(int i =0 ; i< m_tokenAmounts.size(); i++){
            PriceAmount priceAmount = m_tokenAmounts.get(i);
            if(priceAmount.getTokenId().equals(tokenId)){
                return priceAmount;
            }
        }
        return null;
    }


    public PriceAmount getTokenAmount(PriceCurrency currency){
        PriceAmount priceAmount = getTokenAmount(currency.getTokenId());
        
        if(priceAmount != null){
            
            return priceAmount;
        }else{
            PriceAmount newTokenAmount = new PriceAmount(BigDecimal.ZERO, currency);
            m_tokenAmounts.add(newTokenAmount);
            return newTokenAmount;
        }
        
    }
   

    public void getMenu(ContextMenu menuBtn){
        menuBtn.getItems().clear();
        for (int i = 0; i < m_addressDataList.size(); i++) {
            AddressData addressData = m_addressDataList.get(i);
            ImageView iconImgView = new ImageView();

            MenuItem menuItem = new MenuItem(addressData.getAddressString());
            menuItem.setGraphic(iconImgView);
            menuItem.setOnAction(e->{
                selectedAddressDataProperty().set(addressData);
            });
            
            if(selectedAddressDataProperty().get() != null && selectedAddressDataProperty().get().getAddressString().equals(addressData.getAddressString())){
                menuItem.setId("selectedMenuItem");    
            }

            Runnable updateMenuItemImage = ()->{
                Image img = addressData.getImage().get();
                if(img != null){
                    iconImgView.setImage(img);
                }
            };

            updateMenuItemImage.run();

    
            menuBtn.getItems().add(menuItem);
        }
        
    }



    public void getMenu(MenuButton menuBtn){
        menuBtn.getItems().clear();
        for (int i = 0; i < m_addressDataList.size(); i++) {
            AddressData addressData = m_addressDataList.get(i);
            ImageView iconImgView = new ImageView();

            MenuItem menuItem = new MenuItem(addressData.getAddressString());
            menuItem.setGraphic(iconImgView);
            menuItem.setOnAction(e->{
                selectedAddressDataProperty().set(addressData);
            });
            
            if(selectedAddressDataProperty().get() != null && selectedAddressDataProperty().get().getAddressString().equals(addressData.getAddressString())){
                menuItem.setId("selectedMenuItem");    
            }

            Runnable updateMenuItemImage = ()->{
                Image img = addressData.getImage().get();
                if(img != null){
                    iconImgView.setImage(img);
                }
            };

            updateMenuItemImage.run();

    
            menuBtn.getItems().add(menuItem);
        }
        
    }

    public BigDecimal getTotalErgo(){
        SimpleObjectProperty<BigDecimal> total = new SimpleObjectProperty<>(BigDecimal.ZERO);

        for(int i =0; i < m_addressDataList.size() ; i++){
            AddressData addressData = m_addressDataList.get(i);
            BigDecimal erg = addressData.getErgoAmount().getBigDecimalAmount();
            total.set(total.get().add(erg));
        }

        return total.get();
    }

    public BigDecimal getTotalTokenErgo(){
        SimpleObjectProperty<BigDecimal> total = new SimpleObjectProperty<>(BigDecimal.ZERO);

        for(int i =0; i < m_addressDataList.size() ; i++){
            AddressData addressData = m_addressDataList.get(i);
            BigDecimal amount = addressData.getTotalTokenErgBigDecimal();
            total.set(total.get().add(amount));
        }

        return total.get();
    }
    

  
   
    public AddressData getAddress(String address){
        for(AddressData addressData :  m_addressDataList){
            if(addressData.getAddressString().equals(address)){
                return addressData;
            }
        }
        return null;
    }



    public long getBalanceTimeStamp(){
        return m_balanceTimestamp;
    }



    public void shutdown() {
      
        m_addressDataList.forEach(item->{
            item.shutdown();
        });
       

        stop();
         
     
    }

}
