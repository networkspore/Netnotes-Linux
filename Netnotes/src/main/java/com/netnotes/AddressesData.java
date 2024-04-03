package com.netnotes;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
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

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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
 
    public final static int ADDRESS_IMG_HEIGHT = 40;
    public final static int ADDRESS_IMG_WIDTH = 350;
    public final static long QUOTE_TIMEOUT = 1000*60;

    private BufferedImage m_img = null;
    private Graphics2D m_g2d = null;

    private java.awt.Font m_imgFont = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 30);
    private java.awt.Font m_imgSmallFont = new java.awt.Font("SANS-SERIF", java.awt.Font.PLAIN, 12);
    private static volatile boolean m_shuttingDown = false;

    private Stage m_promptStage = null;

    public AddressesData(String id, Wallet wallet, ErgoWalletData walletData, NetworkType networkType) {
        m_shuttingDown = false;
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


     private ScheduledExecutorService m_schedualedExecutor = null;
    private ScheduledFuture<?> m_scheduledFuture = null;

    public void start(){
        if(m_schedualedExecutor == null){
            m_schedualedExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });
        }

        m_scheduledFuture = m_schedualedExecutor.scheduleAtFixedRate(()->{
            try {
            for(int i = 0; i < m_addressDataList.size(); i++){
                m_addressDataList.get(i).update();
          
                    Thread.sleep(100);
                
            }
        } catch (InterruptedException e) {
            stop();
        }

        },0, 7000, TimeUnit.MILLISECONDS);
       
    }

    public void stop(){
        if(m_scheduledFuture != null && !m_scheduledFuture.isDone()){
            m_scheduledFuture.cancel(false);
        }
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
                if(!m_promptStage.isShowing()){
                    m_promptStage.show();
                }else{
                    Platform.runLater(() -> m_promptStage.toBack());
                    Platform.runLater(() -> m_promptStage.toFront());
                }
                
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

    public void getMenu(MenuButton menuBtn){
        
      

        Runnable updateMenu = () ->{
            for (int i = 0; i < m_addressDataList.size(); i++) {
                final AddressData addressData = m_addressDataList.get(i);
         
                MenuItem menuItem = new MenuItem(addressData.getAddressString());
                menuItem.setOnAction(e->{
                    selectedAddressDataProperty().set(addressData);
                });
                
                if(selectedAddressDataProperty().get() != null && selectedAddressDataProperty().get().getAddressString().equals(addressData.getAddressString())){
                    menuItem.setId("selectedMenuItem");    
                }

                Runnable updateMenuItemImage = ()->{
                    Image img = addressData.getImageProperty().get();
                    if(img != null){
                        menuItem.setGraphic(IconButton.getIconView(img, img.getWidth()));
                    }
                };
                updateMenuItemImage.run();

                addressData.getImageProperty().addListener((obs,oldval,newval)->{
                    updateMenuItemImage.run();
                });
                AddressData selectedAdrData = selectedAddressDataProperty().get();
                
                if( selectedAdrData != null && selectedAdrData.getAddressString().equals(addressData.getAddressString())){
                    menuBtn.setText( addressData.getAddressMinimal(7) );
                }

                menuBtn.getItems().add(menuItem);
            }
        };

        selectedAddressDataProperty().addListener((obs,oldval,newval)->updateMenu.run());
        m_addressDataList.addListener((ListChangeListener.Change<? extends AddressData> c) ->updateMenu.run());
        updateMenu.run();
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

        stop();
        m_shuttingDown = true;
        
        if(m_g2d != null){
            m_g2d.dispose();
            m_g2d = null;
        }
        m_img = null;
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


    


     public Image updateBufferedImage(AddressData addressData) {
        if(m_shuttingDown){
            return null;
        }
        ErgoAmount priceAmount = addressData.ergoAmountProperty().get();
        boolean quantityValid = priceAmount != null && priceAmount.getAmountValid();
        double priceAmountDouble = priceAmount != null && quantityValid ? priceAmount.getDoubleAmount() : 0;

        PriceQuote priceQuote = addressData.getValid() ? selectedMarketData().get().priceQuoteProperty().get() : null;
        boolean priceValid = priceQuote != null && priceQuote.getTimeStamp() != 0 && priceQuote.howOldMillis() <  QUOTE_TIMEOUT;
        double priceQuoteDouble = priceValid  && priceQuote != null ? priceQuote.getDoubleAmount() : 0;
        
        String totalPrice = priceValid && priceQuote != null ? Utils.formatCryptoString( priceQuoteDouble * priceAmountDouble, priceQuote.getQuoteCurrency(), priceQuote.getFractionalPrecision(),  quantityValid && priceValid) : " -.--";
        int integers = priceAmount != null ? (int) priceAmount.getDoubleAmount() : 0;
        double decimals = priceAmount != null ? priceAmount.getDoubleAmount() - integers : 0;
        int decimalPlaces = priceAmount != null ? priceAmount.getCurrency().getFractionalPrecision() : 0;
        String cryptoName = priceAmount != null ? priceAmount.getCurrency().getSymbol() : "UKNOWN";
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
    
       

        
        
        BufferedImage unitImage = SwingFXUtils.fromFXImage(priceAmount != null ? priceAmount.getCurrency().getIcon() : new Image("/assets/unknown-unit.png"), null);
        Drawing.setImageAlpha(unitImage, 0x40);
        //  adrBuchImg.getScaledInstance(width, height, java.awt.Image.SCALE_AREA_AVERAGING);
        
        if(m_img == null){
            m_img = new BufferedImage(ADDRESS_IMG_WIDTH, ADDRESS_IMG_HEIGHT, BufferedImage.TYPE_INT_ARGB);
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
        int decimalsX = integersX + stringWidth + 1;

       // int cryptoNameStringWidth = fm.stringWidth(cryptoName);
       

        //int width = decimalsX + decsWidth + (padding * 2);
   
        //width = width < m_minImgWidth ? m_minImgWidth : width;

        int cryptoNameStringX = decimalsX + 2;

        //   g2d.setComposite(AlphaComposite.Clear);

        /* for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = new Color(adrBuchImg.getRGB(x, y), true);

                Color c2 = new Color(c.getRed(), c.getGreen(), c.getBlue(), 35);

                m_img.setRGB(x, y, c2.getRGB());
            }
        }
         */
        m_g2d.drawImage(unitImage,75, (height / 2) - (unitImage.getHeight() / 2), unitImage.getWidth(), unitImage.getHeight(), null);

       



        m_g2d.setFont(m_imgFont);
        fm = m_g2d.getFontMetrics();
        m_g2d.setColor(java.awt.Color.WHITE);

        

        m_g2d.drawString(amountString, integersX, fm.getAscent() + 5);

        m_g2d.setFont(m_imgSmallFont);
        fm = m_g2d.getFontMetrics();
        m_g2d.setColor(new java.awt.Color(.9f, .9f, .9f, .9f));

       
        if(decimalPlaces > 0){
            //decimalsX = widthIncrease > 0 ? decimalsX + widthIncrease : decimalsX;
            m_g2d.drawString(decs, decimalsX , fm.getHeight() + 2);
        }

        
        m_g2d.drawString(cryptoName, cryptoNameStringX, height - 10);

        m_g2d.setFont(m_imgSmallFont);
        m_g2d.setColor(java.awt.Color.WHITE);
        fm = m_g2d.getFontMetrics();
        m_g2d.drawString(totalPrice, padding, fm.getHeight() + 2);

        m_g2d.setColor(new java.awt.Color(.6f, .6f, .6f, .9f));
        m_g2d.drawString(currencyPrice, padding, height - 10);

        /*try {
            Files.writeString(logFile.toPath(), amountString + decs);
        } catch (IOException e) {

        }*/

        return SwingFXUtils.toFXImage(m_img, null);

  
    }


}
