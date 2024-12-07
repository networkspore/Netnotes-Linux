package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.ergoplatform.appkit.Mnemonic;
import org.ergoplatform.appkit.MnemonicValidationException;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.SecretString;
import org.reactfx.util.FxTimer;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.satergo.Wallet;
import com.utils.Utils;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Duration;

public class ErgoWalletsAppBox extends AppBox {

    private boolean m_current = false;

    private NoteInterface m_ergoNetworkInterface = null;


    
    private SimpleBooleanProperty showWallet = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty showBalance = new SimpleBooleanProperty(false);

    private SimpleObjectProperty<AppBox> m_currentBox = new SimpleObjectProperty<>(null);

    private SimpleObjectProperty<ConfigBox> m_configBox = new SimpleObjectProperty<>(null);
    private VBox m_mainBox;
    private final String walletBtnDefaultString = "[select]";


    private String m_locationId;

    private Stage m_appStage;

    private SimpleObjectProperty<NoteInterface> m_selectedWallet = new SimpleObjectProperty<>(null);
    private SimpleObjectProperty<NoteInterface> m_selectedMarket = new SimpleObjectProperty<>(null);
    private SimpleObjectProperty<NoteInterface> m_selectedTokensMarket = new SimpleObjectProperty<>(null);



    private NoteMsgInterface m_walletMsgInterface = null;


    private SimpleObjectProperty<PriceQuote> m_ergoPriceQuoteProperty = new SimpleObjectProperty<>();
    private SimpleObjectProperty<JsonObject> m_balanceObject = new SimpleObjectProperty<>();

    private String m_currentQuoteMarketId = null;
    private String m_stableSymbol = "USD";

    private TextField m_walletField;    
    private HBox m_walletFieldBox;
    private VBox m_walletBodyBox;
    private Label m_disableWalletBtn;
    private VBox m_selectedAddressBox;
    private Label m_configBtn;


    private JsonParser m_jsonParser = new JsonParser();

    private NetworkType m_networkType = NetworkType.MAINNET;


    public void setDefault(String walletId){
        m_balanceObject.set(null);
        JsonObject getWalletObject = Utils.getCmdObject(  walletId != null ?"setDefault" : "clearDefault");
        getWalletObject.addProperty("locationId", m_locationId);
        getWalletObject.addProperty("networkId", App.WALLET_NETWORK);
        
        if(walletId != null){
            getWalletObject.addProperty("id", walletId);
        }

        m_ergoNetworkInterface.sendNote(getWalletObject);

    }

    public void clearDefault(){
        m_balanceObject.set(null);
        JsonObject setDefaultObject = Utils.getCmdObject("clearDefault");
        setDefaultObject.addProperty("networkId", App.WALLET_NETWORK);
        setDefaultObject.addProperty("locationId", m_locationId);
        m_ergoNetworkInterface.sendNote(setDefaultObject);
    }

    public void getDefaultInteface(){
        m_balanceObject.set(null);
        JsonObject note = Utils.getCmdObject("getDefaultInterface");
        note.addProperty("networkId", App.WALLET_NETWORK);
        note.addProperty("locationId", m_locationId);
        NoteInterface noteInterface = (NoteInterface) m_ergoNetworkInterface.sendNote(note);
      
        m_selectedWallet.set(noteInterface);
        
    }




    public void getErgoQuote(){
        NoteInterface ergoMarketInterface = m_selectedMarket.get();
        
        if(ergoMarketInterface != null){
            
            if(m_ergoPriceQuoteProperty.get() == null || m_currentQuoteMarketId == null || (m_currentQuoteMarketId != null && !m_currentQuoteMarketId.equals(ergoMarketInterface.getNetworkId()))){
                
                Object succededObject = ergoMarketInterface.sendNote(getErgoQuoteWithSymbol(m_stableSymbol));

                if(succededObject != null && succededObject instanceof PriceQuote){
                    PriceQuote newQuote = (PriceQuote) succededObject;
                    m_currentQuoteMarketId = ergoMarketInterface.getNetworkId();

                    m_ergoPriceQuoteProperty.set(newQuote);
                }
                
            }
        }

    }

    @Override
    public void sendMessage(int code, long timeStamp,String networkId, String msg){
        if(networkId != null && networkId.equals(App.WALLET_NETWORK)){
            
            switch(code){
                case App.LIST_DEFAULT_CHANGED:
                    getDefaultInteface();
                break;
            }

            AppBox appBox  = m_currentBox.get();
            if(appBox != null){
                JsonElement msgElement = msg != null ? m_jsonParser.parse(msg) : null;
                if(msgElement != null && msgElement.isJsonObject()){
                    appBox.sendMessage(code, timeStamp, networkId, msg);
                }
            }
        }
    }



    
    public static JsonObject getTokenQuoteJson(String tokenId){
        JsonObject json = Utils.getCmdObject("getQuote");
        json.addProperty("baseType", "id");
        json.addProperty("base", ErgoCurrency.TOKEN_ID);
        json.addProperty("quoteType","firstId");
        json.addProperty("quote", tokenId);

        return json;
    }

    public static JsonObject getErgoQuoteWithSymbol(String firstQuoteSymbolContains){
        JsonObject json = Utils.getCmdObject("getQuote");
        json.addProperty("baseType", "symbol");
        json.addProperty("base", ErgoCurrency.SYMBOL);
        json.addProperty("quoteType", "firstSymbolContains");
        json.addProperty("quote", firstQuoteSymbolContains);

        return json;
    }

    
    public void updateTokenQuote(PriceAmount tokenAmount, String marketInterfaceId){
        

        NoteInterface tokenMarketInterface = m_selectedTokensMarket.get();
        
        if(tokenMarketInterface != null){
            
            String currentQuoteMarketId = tokenAmount.getMarketId();
     

            if(tokenAmount.priceQuoteProperty().get() == null || currentQuoteMarketId == null || (currentQuoteMarketId != null && !currentQuoteMarketId.equals(marketInterfaceId))){
                String tokenId = tokenAmount.getTokenId();
              
                
                Object succededObject = tokenMarketInterface.sendNote(getTokenQuoteJson(tokenId));
                  
                if(succededObject != null && succededObject instanceof PriceQuote){
                    PriceQuote newQuote = (PriceQuote) succededObject;
                    tokenAmount.setMarketId(marketInterfaceId);

                    tokenAmount.priceQuoteProperty().set(newQuote);
                    
                }
                
            }
        }else{

        }
    }


    private void updateWallet(){

        NoteInterface noteInterface = m_selectedWallet.get();

        if (noteInterface == null) {
            m_walletField.setText(walletBtnDefaultString);
  
            if (m_walletBodyBox.getChildren().contains(m_selectedAddressBox)) {
                m_walletBodyBox.getChildren().remove(m_selectedAddressBox);
            }

            if (m_walletFieldBox.getChildren().contains(m_disableWalletBtn)) {
                m_walletFieldBox.getChildren().remove(m_disableWalletBtn);
            }
            if (m_walletFieldBox.getChildren().contains(m_configBtn)) {
                m_walletFieldBox.getChildren().remove(m_configBtn);
            }

        } else {
          
            m_walletField.setText(noteInterface.getName());

            if (!m_walletBodyBox.getChildren().contains(m_selectedAddressBox)) {
                m_walletBodyBox.getChildren().add(m_selectedAddressBox);
            }
            if (!m_walletFieldBox.getChildren().contains(m_configBtn)) {
                m_walletFieldBox.getChildren().add(m_configBtn);
            }

            if (!m_walletFieldBox.getChildren().contains(m_disableWalletBtn)) {
                m_walletFieldBox.getChildren().add(m_disableWalletBtn);
            }
            showWallet.set(true);
        }

        
    }

    public ErgoWalletsAppBox(Stage appStage, String locationId, NoteInterface ergoNetwork) {
        super();


        m_walletField = new TextField();
        HBox.setHgrow(m_walletField, Priority.ALWAYS);

        m_walletField.setAlignment(Pos.CENTER_LEFT);
        m_walletField.setText(walletBtnDefaultString);

        m_walletField.setMinWidth(90);
        m_walletField.setEditable(false);
        m_walletField.setId("hand");
        

        m_ergoNetworkInterface = ergoNetwork;

        m_locationId = locationId;
  
        m_appStage = appStage;
        

        ImageView walletIconView = new ImageView(new Image("/assets/ergo-wallet-30.png"));

        walletIconView.setPreserveRatio(true);
        walletIconView.setFitWidth(18);

        LockField lockBox = new LockField("Locked:", "≬", " Unlock ");
        lockBox.setPadding(new Insets(2, 5, 2, 0));
        lockBox.setAlignment(Pos.CENTER_LEFT);
        lockBox.setMaxHeight(15);
        HBox.setHgrow(lockBox, Priority.ALWAYS);

        
       

        Label toggleShowBalance = new Label(showBalance.get() ? "⏷ " : "⏵ ");
        toggleShowBalance.setId("caretBtn");
        

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ImageView closeImage = App.highlightedImageView(App.closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        Label toggleShowWallets = new Label(showWallet.get()? "⏷ "  : "⏵ ");
        toggleShowWallets.setId("caretBtn");
        ContextMenu walletMenu = new ContextMenu();

        Label openWalletBtn = new Label("⏷");
        openWalletBtn.setId("lblBtn");



        MenuButton walletMenuBtn = new MenuButton("⋮");

        Text walletTopLabel = new Text(" Wallet");
        walletTopLabel.setFont(App.txtFont);
        walletTopLabel.setFill(App.txtColor);

        MenuItem openWalletMenuItem = new MenuItem("⇲   Open…");

        MenuItem newWalletMenuItem = new MenuItem("⇱   New…");

        MenuItem restoreWalletMenuItem = new MenuItem("⟲   Restore…");

        MenuItem removeWalletMenuItem = new MenuItem("🗑   Remove…");

       
  

        walletMenuBtn.getItems().addAll(newWalletMenuItem, openWalletMenuItem, restoreWalletMenuItem,
                removeWalletMenuItem);


        MenuButton adrMenuBtn = new MenuButton("⋮");

    
        HBox adrBtnsBox = new HBox(adrMenuBtn);
        adrBtnsBox.setAlignment(Pos.CENTER_LEFT);


    
        SeparatorMenuItem seperatorAdrMenuItem = new SeparatorMenuItem();

        MenuItem copyAdrMenuItem = new MenuItem("⧉  Copy address to clipbord");
        copyAdrMenuItem.setOnAction(e->lockBox.copyAddressToClipboard());

       // MenuItem addAdrMenuItem = new MenuItem("➕  Add address to wallet");
        MenuItem sendMenuItem = new MenuItem("⮩  Send");

        adrMenuBtn.getItems().addAll(copyAdrMenuItem, sendMenuItem, seperatorAdrMenuItem);
        


        Runnable hideMenus = () ->{
            walletMenuBtn.hide();
            adrMenuBtn.hide();
            walletMenu.hide();            
        };
        
      

        sendMenuItem.setOnAction(e->{
            hideMenus.run();
            m_currentBox.set(new SendAppBox(lockBox));
        });

         

        Runnable openWallet = () -> {
            
            FileChooser openFileChooser = new FileChooser();
            openFileChooser.setTitle("Select wallet (*.erg)");
            openFileChooser.setInitialDirectory(AppData.HOME_DIRECTORY);
            openFileChooser.getExtensionFilters().add(ErgoWallets.ergExt);
            openFileChooser.setSelectedExtensionFilter(ErgoWallets.ergExt);

            File walletFile = openFileChooser.showOpenDialog(appStage);

            if (walletFile != null) {
                String fileName = walletFile.getName();
                fileName = fileName.endsWith(".erg") ? fileName.substring(0, fileName.length() - 4) : fileName;

                JsonObject note = Utils.getCmdObject("openWallet");
                note.addProperty("networkId", App.WALLET_NETWORK);
                note.addProperty("locationId", m_locationId);

                JsonObject walletData = new JsonObject();
                walletData.addProperty("networkType", m_networkType.toString());
                walletData.addProperty("path", walletFile.getAbsolutePath());

                note.add("data", walletData);
                Object result = m_ergoNetworkInterface.sendNote(note);

                if (result != null && result instanceof JsonObject) {
                    JsonObject walletJson = (JsonObject) result;
                    JsonElement idElement = walletJson.get("id");
                    if (idElement != null && idElement.isJsonPrimitive()) {
      
                        setDefault(idElement.getAsString());
                        
                    }
                        
                }
            }
        };

        Runnable newWallet = () -> {
            hideMenus.run();
            NewWalletBox newWalletBox = new NewWalletBox(true);
            m_currentBox.set(newWalletBox);
        };

        Runnable restoreWallet = () -> {
            hideMenus.run();
            NewWalletBox newWalletBox = new NewWalletBox(false);
            m_currentBox.set(newWalletBox);
        };
       
        Runnable removeWallet = () ->{
            hideMenus.run();
            m_currentBox.set(new RemoveWalletBox());
        };
 

        openWalletMenuItem.setOnAction(e -> openWallet.run());
        newWalletMenuItem.setOnAction(e -> newWallet.run());
        restoreWalletMenuItem.setOnAction(e -> restoreWallet.run());
        removeWalletMenuItem.setOnAction(e -> removeWallet.run());

        m_configBtn = new Label("⚙");
        m_configBtn.setId("lblBtn");

        m_walletFieldBox = new HBox(m_walletField, openWalletBtn);
        HBox.setHgrow(m_walletFieldBox, Priority.ALWAYS);
        m_walletFieldBox.setAlignment(Pos.CENTER_LEFT);
        m_walletFieldBox.setId("bodyBox");
        m_walletFieldBox.setPadding(new Insets(0, 1, 0, 0));
        m_walletFieldBox.setMaxHeight(18);

        HBox walletMenuBtnPadding = new HBox(walletMenuBtn);
        walletMenuBtnPadding.setPadding(new Insets(0, 0, 0, 5));

        

        HBox walletBtnBox = new HBox(m_walletFieldBox, walletMenuBtnPadding);
        walletBtnBox.setPadding(new Insets(2, 2, 0, 5));
        HBox.setHgrow(walletBtnBox, Priority.ALWAYS);

        walletBtnBox.setAlignment(Pos.CENTER_LEFT);

        Runnable showWalletMenu = () -> {

            Point2D p = walletBtnBox.localToScene(0.0, 0.0);
            walletMenu.setPrefWidth(walletBtnBox.getLayoutBounds().getWidth());

            walletMenu.show(walletBtnBox,
                    5 + p.getX() + walletBtnBox.getScene().getX() + walletBtnBox.getScene().getWindow().getX(),
                    (p.getY() + walletBtnBox.getScene().getY() + walletBtnBox.getScene().getWindow().getY())
                            + walletBtnBox.getLayoutBounds().getHeight() - 1);
        };
  

        MenuItem openWalletItem = new MenuItem("[Open]");
        openWalletItem.setOnAction(e -> openWallet.run());

        MenuItem newWalletItem = new MenuItem("[New]");
        newWalletItem.setOnAction(e -> newWallet.run());

        MenuItem restoreWalletItem = new MenuItem("[Restore]                ");
        restoreWalletItem.setOnAction(e -> restoreWallet.run());

        MenuItem removeWalletItem = new MenuItem("[Remove]");
        removeWalletItem.setOnAction(e -> removeWallet.run());

        Runnable showOpenWalletMenu = () -> {

            JsonObject note = Utils.getCmdObject("getWallets");
            note.addProperty("networkId", App.WALLET_NETWORK);
            note.addProperty("locationId", m_locationId);

            JsonArray walletIds = (JsonArray) m_ergoNetworkInterface.sendNote(note);

            walletMenu.getItems().clear();
            if (walletIds != null) {

                for (JsonElement element : walletIds) {
                    if (element != null && element instanceof JsonObject) {
                        JsonObject json = element.getAsJsonObject();

                        String name = json.get("name").getAsString();
                        String id = json.get("id").getAsString();

                        MenuItem walletItem = new MenuItem(String.format("%-50s", " " + name));

                        walletItem.setOnAction(action -> {
                            m_balanceObject.set(null);
                            JsonObject getWalletObject = Utils.getCmdObject("setDefault");
                            getWalletObject.addProperty("networkId", App.WALLET_NETWORK);
                            getWalletObject.addProperty("locationId", m_locationId);

                            getWalletObject.addProperty("id", id);

                            m_ergoNetworkInterface.sendNote(getWalletObject);
                       
                        });

                        walletMenu.getItems().add(walletItem);
                    }
                }

            }

            walletMenu.getItems().addAll(newWalletItem, openWalletItem, restoreWalletItem, removeWalletItem);
            showWalletMenu.run();

        };

        openWalletBtn.setOnMouseClicked(e -> {
            showOpenWalletMenu.run();
        });

        m_walletField.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            showOpenWalletMenu.run();
        });

        HBox walletLabelBox = new HBox(walletTopLabel);
        walletLabelBox.setPadding(new Insets(0, 5, 0, 5));
        walletLabelBox.setAlignment(Pos.CENTER_LEFT);

        HBox walletsTopBar = new HBox(toggleShowWallets, walletIconView, walletLabelBox, walletBtnBox);
        walletsTopBar.setAlignment(Pos.CENTER_LEFT);
        walletsTopBar.setPadding(new Insets(2));

        m_walletBodyBox = new VBox();
        m_walletBodyBox.setPadding(new Insets(0, 0, 0, 5));
        m_walletBodyBox.setId("networkBox");

        VBox walletBodyPaddingBox = new VBox();
        HBox.setHgrow(walletBodyPaddingBox, Priority.ALWAYS);

        VBox walletLayoutBox = new VBox(walletsTopBar, walletBodyPaddingBox);
        HBox.setHgrow(walletLayoutBox, Priority.ALWAYS);

        m_mainBox = new VBox(walletLayoutBox);
        m_mainBox.setPadding(new Insets(0));

        m_disableWalletBtn = new Label("☓");
        m_disableWalletBtn.setId("lblBtn");

        m_disableWalletBtn.setOnMouseClicked(e -> {
            showWallet.set(false);
            lockBox.setLocked();
            m_balanceObject.set(null);
            JsonObject note = Utils.getCmdObject("clearDefault");
            note.addProperty("networkId", App.WALLET_NETWORK);
            note.addProperty("locationId", m_locationId);

            
            m_ergoNetworkInterface.sendNote(note);
        });

       

     

        m_currentBox.addListener((obs, oldval, newval) -> {
            m_mainBox.getChildren().clear();
            if (newval != null) {
                m_mainBox.getChildren().add(newval);
            } else {
                m_mainBox.getChildren().add(walletLayoutBox);
            }

        });

     

        Runnable updateShowWallets = () -> {
            boolean isShowWallet = showWallet.get();
            toggleShowWallets.setText(isShowWallet ? "⏷ " : "⏵ ");

            if (isShowWallet) {
                if (!walletBodyPaddingBox.getChildren().contains(m_walletBodyBox)) {
                    walletBodyPaddingBox.getChildren().add(m_walletBodyBox);
                }
            } else {
                if (walletBodyPaddingBox.getChildren().contains(m_walletBodyBox)) {
                    walletBodyPaddingBox.getChildren().remove(m_walletBodyBox);
                }
            }
        };
        updateShowWallets.run();

        showWallet.addListener((obs, oldval, newval) -> updateShowWallets.run());
        
        ContextMenu addressesMenu = new ContextMenu();

        ImageView addressIconView = new ImageView(new Image(ErgoWallets.getSmallAppIconString()));
        addressIconView.setFitWidth(20);
        addressIconView.setPreserveRatio(true);

        toggleShowWallets.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (m_selectedWallet.get() != null) {
                showWallet.set(!showWallet.get());
            } else {
                showOpenWalletMenu.run();
            }
        });

        lockBox.setOnMenu((e) -> {
            NoteInterface selectedWallet = m_selectedWallet.get();
            addressesMenu.getItems().clear();

            if (selectedWallet != null && lockBox.getLockId() != null) {
                JsonObject json = Utils.getCmdObject("getAddresses");
                json.addProperty("accessId", lockBox.getLockId());
                json.addProperty("networkId", m_locationId);

                Object successObject = selectedWallet.sendNote(json);

                if (successObject != null && successObject instanceof JsonArray) {

                    JsonArray jsonArray = (JsonArray) successObject;

                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonElement element = jsonArray.get(i);
                        if (element != null && element.isJsonObject()) {

                            JsonObject jsonObj = element.getAsJsonObject();
                            String address = jsonObj.get("address").getAsString();
                            String name = jsonObj.get("name").getAsString();

                            MenuItem addressMenuItem = new MenuItem(name + ": " + address);
                            addressMenuItem.setOnAction(e1 -> {
                                m_balanceObject.set(null);
                                lockBox.setAddress(address);
                                lockBox.setName(name);
                            });

                            addressesMenu.getItems().add(addressMenuItem);
                        }
                    }

                    Point2D p = lockBox.localToScene(0.0, 0.0);

                    addressesMenu.show(lockBox,
                            p.getX() + lockBox.getScene().getX() + lockBox.getScene().getWindow().getX() + 90,
                            (p.getY() + lockBox.getScene().getY() + lockBox.getScene().getWindow().getY())
                                    + lockBox.getLayoutBounds().getHeight() - 1);
                }
            }
        });

        showBalance.addListener((obs, oldval, newval) -> {
            toggleShowBalance.setText(newval ? "⏷ " : "⏵ ");
        });





        HBox adrBtnBoxes = new HBox();
        adrBtnBoxes.setAlignment(Pos.CENTER_LEFT);

        m_configBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            NoteInterface noteInterface = m_selectedWallet.get();

            if (m_configBox.get() == null && noteInterface != null) {

                JsonObject note = Utils.getCmdObject("getConfigId");
                note.addProperty("networkId", m_locationId);

                noteInterface.sendNote(note, (onSuccess) -> {
                    Object obj = onSuccess.getSource().getValue();
                    if (obj != null && obj instanceof JsonObject) {
                        JsonObject json = (JsonObject) obj;
                        hideMenus.run();
                        m_configBox.set(new ConfigBox(json.get("configId").getAsString()));
                    }
                }, (onFailed) -> {
                });

            } else {
                m_configBox.set(null);
            }
        });

        HBox addressBtnsBox = new HBox(toggleShowBalance, lockBox, adrBtnBoxes);
        addressBtnsBox.setPadding(new Insets(2, 0, 2, 0));
        addressBtnsBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(addressBtnsBox, Priority.ALWAYS);

        toggleShowBalance.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (lockBox.isUnlocked()) {
                showBalance.set(!showBalance.get());
            } else {
                lockBox.requestFocus();
            }
        });

        VBox walletBalanceBox = new VBox();
        walletBalanceBox.setPadding(new Insets(2, 0, 2, 5));
        walletBalanceBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(walletBalanceBox, Priority.ALWAYS);

        HBox actionBox = new HBox();
        HBox.setHgrow(actionBox, Priority.ALWAYS);

        VBox adrBox = new VBox(actionBox, addressBtnsBox, walletBalanceBox);
        adrBox.setPadding(new Insets(0, 2, 5, 5));
        HBox.setHgrow(adrBox, Priority.ALWAYS);
        adrBox.setAlignment(Pos.CENTER_LEFT);

        adrBox.setPadding(new Insets(2));
        ///
        m_configBox.addListener((obs, oldval, newval) -> {
            actionBox.getChildren().clear();
            if (oldval != null) {
                oldval.shutdown();
            }

            if (newval != null) {
                actionBox.getChildren().add((Pane) newval);
            }

        });

        m_selectedAddressBox = new VBox(adrBox);
        HBox.setHgrow(m_selectedAddressBox, Priority.ALWAYS);
        m_selectedAddressBox.setAlignment(Pos.TOP_LEFT);

        ErgoWalletAmountBoxes amountBoxes = new ErgoWalletAmountBoxes(true, m_networkType, m_balanceObject);
        VBox balancePaddingBox = new VBox(amountBoxes);
        balancePaddingBox.setPadding(new Insets(0, 0, 0, 0));
        HBox.setHgrow(balancePaddingBox, Priority.ALWAYS);
        
        Runnable getBalance = ()->{

            NoteInterface walletInterface = m_selectedWallet.get();

            if(lockBox.isUnlocked()){
                JsonObject note = Utils.getCmdObject("getBalance");
                note.addProperty("locationId", locationId);
                note.addProperty("accessId", lockBox.getLockId());
                
                Object obj = walletInterface.sendNote(note);
                
                if(obj != null && obj instanceof JsonObject){

                    m_balanceObject.set((JsonObject) obj);
                   
                
                }else{
                    m_balanceObject.set(null);
                    showBalance.set(false);
                }

            }else{
                m_balanceObject.set(null);
                showBalance.set(false);
            }
        };

        ChangeListener<String> lockListener = (obs, oldval, newval) -> {
            NoteInterface noteInterface = m_selectedWallet.get();

            if (oldval != null) {

            
                showBalance.set(false);
            }

            if (newval != null && noteInterface != null) {
                JsonObject json = Utils.getCmdObject("getAddresses");
                json.addProperty("accessId", newval);
                json.addProperty("networkId", m_locationId);

                Object successObject = noteInterface.sendNote(json);

                if (successObject != null && successObject instanceof JsonArray) {

                    JsonArray jsonArray = (JsonArray) successObject;
                    if (jsonArray.size() > 0) {
                        JsonObject adr0 = jsonArray.get(0).getAsJsonObject();
                        JsonElement addressElement = adr0.get("address");
                        JsonElement nameElement = adr0.get("name");

                        String address = addressElement.getAsString();
                        String name = nameElement.getAsString();

                        lockBox.setAddress(address);
                        lockBox.setName(name);
                        
                        getBalance.run();
                       
                    } else {
                        lockBox.setLocked();
                    }
                }
                if (!adrBtnBoxes.getChildren().contains(adrBtnsBox)) {
                    adrBtnBoxes.getChildren().add(adrBtnsBox);
                }
            } else {
                if (adrBtnBoxes.getChildren().contains(adrBtnsBox)) {
                    adrBtnBoxes.getChildren().remove(adrBtnsBox);
                }
                lockBox.setLocked();
            }
        };

        

        lockBox.lockId().addListener(lockListener);


        m_selectedWallet.addListener((obs, oldVal, newVal) -> {


            if (oldVal != null && m_walletMsgInterface != null) {
                oldVal.removeMsgListener(m_walletMsgInterface);
                m_walletMsgInterface = null;
            }
        

            lockBox.setLocked();
            m_configBox.set(null);
            updateWallet();
        });

      

        showBalance.addListener((obs, oldval, newval) -> {
            if (newval) {
                if (!walletBalanceBox.getChildren().contains(balancePaddingBox)) {
                    walletBalanceBox.getChildren().add(balancePaddingBox);
                }

            } else {
                if (walletBalanceBox.getChildren().contains(balancePaddingBox)) {
                    walletBalanceBox.getChildren().remove(balancePaddingBox);
                }
            }
        });

        
      



        lockBox.setOnSend(e->{
            hideMenus.run();
            m_currentBox.set(new SendAppBox(lockBox));
        });

        lockBox.setPasswordAction(e -> {
            
            NoteInterface noteInterface = m_selectedWallet.get();

            JsonObject getWalletObject = Utils.getCmdObject("getAccessId");
            getWalletObject.addProperty("locationId", m_locationId);

            noteInterface.sendNote(getWalletObject, onSucceeded->{
                Object successObject = onSucceeded.getSource().getValue();

                if (successObject != null) {
                    JsonObject json = (JsonObject) successObject;
                    // addressesDataObject.set(json);
                    JsonElement codeElement = json.get("code");
                    JsonElement accessIdElement = json.get("accessId");
                    if(accessIdElement != null && (codeElement == null || codeElement != null && codeElement.getAsString().equals(App.SUCCESS))){
                    
                        String accessId = accessIdElement.getAsString();

                        m_walletMsgInterface = new NoteMsgInterface() {
                            private final String m_accessId = accessId;

                            public String getId() {
                                return m_accessId;
                            }
                            @Override
                            public void sendMessage(int code, long timestamp,String networkId, Number num) {
                            }

                            public void sendMessage(int code, long timestamp,String networkId, String msg) {
                            

                                switch (code) {
                                    case App.UPDATED:
                                        if (networkId != null && networkId.equals(lockBox.getAddress())) {
                                            JsonElement jsonElement = m_jsonParser.parse(msg);

                                            if(jsonElement != null && jsonElement.isJsonObject()){

                                                m_balanceObject.set(jsonElement.getAsJsonObject());
                                                
                                            }
                                        }else{
                                            m_balanceObject.set(null);
                                        }
                                        break;
                            
                                }
                            }
                        };

                        noteInterface.addMsgListener(m_walletMsgInterface);

                        lockBox.setUnlocked(accessId);
                        
                        FxTimer.runLater(java.time.Duration.ofMillis(150), ()->{
                            showBalance.set(true);
                        });
                    }
                } 
            }, onFailed->{});
            

        });

        getDefaultInteface();




        getChildren().add(m_mainBox);
    }


    @Override 
    public Object sendNote(JsonObject note){

        return null;
    }

    public String getName() {
        return "Wallets";
    }

    public void shutdown() {
        NoteInterface noteInterface = m_selectedWallet.get();
        m_balanceObject.set(null);
        if(noteInterface != null){
            if(m_walletMsgInterface != null){
                noteInterface.removeMsgListener(m_walletMsgInterface);
            }
            m_selectedWallet.set(null);
        }  
    }

    public void setCurrent(boolean value) {
        m_current = value;
    }

    public boolean getCurrent() {
        return m_current;
    }

    public String getType() {
        return "RowArea";
    }

    public boolean isStatic() {
        return false;
    }



    private class ConfigBox extends AppBox {

        private SimpleStringProperty m_name = new SimpleStringProperty();
        private SimpleStringProperty m_fileName = new SimpleStringProperty();
        private String m_configId;

        public void shutdown() {

        }

        @Override
        public void sendMessage(int code, long timeStamp,String networkId, String msg) {
            
            update();

        }

        private void update() {
            NoteInterface noteInterface = m_selectedWallet.get();

            JsonObject note = Utils.getCmdObject("getFileData");
            note.addProperty("networkId", m_locationId);
            note.addProperty("configId", m_configId);

            Object obj = noteInterface.sendNote(note);
            if (obj != null && obj instanceof JsonObject) {
                JsonObject json = (JsonObject) obj;
                m_name.set(noteInterface.getName());
                m_fileName.set(json.get("name").getAsString());
            }

        }

        public ConfigBox(String configId) {
      
            m_configId = configId;
            Label toggleShowSettings = new Label("⏷ ");
            toggleShowSettings.setId("caretBtn");

            Label settingsLbl = new Label("⚙");
            settingsLbl.setId("logoBox");

            Text settingsText = new Text("Config");
            settingsText.setFont(App.txtFont);
            settingsText.setFill(App.txtColor);

            Tooltip walletInfoTooltip = new Tooltip("Wallet in use");

            HBox settingsBtnsBox = new HBox(toggleShowSettings, settingsLbl, settingsText);
            HBox.setHgrow(settingsBtnsBox, Priority.ALWAYS);
            settingsBtnsBox.setAlignment(Pos.CENTER_LEFT);

            Label walletNameText = new Label("Name ");
            // walletNameText.setFill(App.txtColor);
            walletNameText.setFont(App.txtFont);
            walletNameText.setMinWidth(70);

            TextField walletNameField = new TextField();
            HBox.setHgrow(walletNameField, Priority.ALWAYS);

            walletNameField.setEditable(false);

            Label editNameLabel = new Label("✎");
            editNameLabel.setId("lblBtn");

            Button walletNameEnterBtn = new Button("[enter]");
            walletNameEnterBtn.setMinHeight(15);
            walletNameEnterBtn.setText("[enter]");
            walletNameEnterBtn.setId("toolBtn");
            walletNameEnterBtn.setPadding(new Insets(0, 5, 0, 5));
            walletNameEnterBtn.setFocusTraversable(false);

            walletNameField.setOnAction(e -> walletNameEnterBtn.fire());

            Button walletFileEnterBtn = new Button("[enter]");
            walletFileEnterBtn.setText("[enter]");
            walletFileEnterBtn.setId("toolBtn");
            walletFileEnterBtn.setPadding(new Insets(0, 5, 0, 5));
            walletFileEnterBtn.setFocusTraversable(false);

            HBox walletNameFieldBox = new HBox(walletNameField, editNameLabel);
            HBox.setHgrow(walletNameFieldBox, Priority.ALWAYS);
            walletNameFieldBox.setId("bodyBox");
            walletNameFieldBox.setPadding(new Insets(0, 5, 0, 0));
            walletNameFieldBox.setMaxHeight(18);
            walletNameFieldBox.setAlignment(Pos.CENTER_LEFT);

            Label walletNameLbl = new Label("  ");
            walletNameLbl.setId("logoBtn");

            HBox walletNameBox = new HBox(walletNameLbl, walletNameText, walletNameFieldBox);
            walletNameBox.setAlignment(Pos.CENTER_LEFT);
            walletNameBox.setPadding(new Insets(2, 0, 2, 0));

            Label walletFileText = new Label("File");
            walletFileText.setFont(App.txtFont);
            walletFileText.setMinWidth(70);

            TextField walletFileField = new TextField();
            walletFileField.setEditable(false);
            HBox.setHgrow(walletFileField, Priority.ALWAYS);
            walletFileField.setOnAction(e -> walletFileEnterBtn.fire());

            Label walletFileOpenLbl = new Label("…");
            walletFileOpenLbl.setId("lblBtn");

            Label walletFileEditLbl = new Label("✎");
            walletFileEditLbl.setId("lblBtn");

            HBox walletFileFieldBox = new HBox(walletFileField, walletFileOpenLbl, walletFileEditLbl);
            HBox.setHgrow(walletFileFieldBox, Priority.ALWAYS);
            walletFileFieldBox.setId("bodyBox");
            walletFileFieldBox.setAlignment(Pos.CENTER_LEFT);
            walletFileFieldBox.setMaxHeight(18);
            walletFileFieldBox.setPadding(new Insets(0, 5, 0, 0));

            walletFileOpenLbl.setOnMouseClicked(e -> {
                NoteInterface noteInterface = m_selectedWallet.get();

                if (noteInterface != null) {
                    JsonObject note = Utils.getCmdObject("getFileData");
                    note.addProperty("configId", m_configId);
                    note.addProperty("networkId", m_locationId);

                    Object obj = noteInterface.sendNote(note);

                    JsonObject json = obj != null && obj instanceof JsonObject ? (JsonObject) obj : null;
                    boolean isFile = json != null ? json.get("isFile").getAsBoolean() : false;
                    String path = isFile ? json.get("path").getAsString() : null;

                    File currentFile = path != null ? new File(path) : null;
                    File currentDir = currentFile != null ? currentFile.getParentFile() : AppData.HOME_DIRECTORY;
                    // String fileName = currentFile != null ? currentFile.getName() :
                    // noteInteface.getNetworkId() + ".erg";

                    FileChooser openFileChooser = new FileChooser();
                    openFileChooser.setTitle("Select wallet (*.erg)");
                    openFileChooser.setInitialDirectory(currentDir);
                    openFileChooser.getExtensionFilters().add(ErgoWallets.ergExt);
                    openFileChooser.setSelectedExtensionFilter(ErgoWallets.ergExt);

                    File walletFile = openFileChooser.showOpenDialog(m_appStage);

                    if (walletFile != null) {
                        JsonObject updateFileObject = Utils.getCmdObject("updateFile");
                        updateFileObject.addProperty("file", walletFile.getAbsolutePath());
                        updateFileObject.addProperty("networkId", m_locationId);
                        updateFileObject.addProperty("configId", m_configId);
                        noteInterface.sendNote(updateFileObject);
                    }
                }
            });

            Label walletFileLbl = new Label("  ");
            walletFileLbl.setId("logoBtn");

            toggleShowSettings.setOnMouseClicked(e -> {
                m_configBox.set(null);
            });

            HBox walletFileBox = new HBox(walletFileLbl, walletFileText, walletFileFieldBox);
            walletFileBox.setAlignment(Pos.CENTER_LEFT);
            walletFileBox.setPadding(new Insets(2, 0, 2, 0));

            VBox settingsBodyBox = new VBox(walletNameBox, walletFileBox);
            settingsBodyBox.setPadding(new Insets(0, 5, 0, 30));
            HBox.setHgrow(settingsBodyBox, Priority.ALWAYS);

            getChildren().addAll(settingsBtnsBox, settingsBodyBox);
            HBox.setHgrow(this, Priority.ALWAYS);

            Runnable setWalletConfigInfo = () -> {
                NoteInterface noteInterface = m_selectedWallet.get();

                if (noteInterface == null) {
                    m_configBox.set(null);
                    return;
                }
                JsonObject note = Utils.getCmdObject("getFileData");
                note.addProperty("networkId", m_locationId);
                note.addProperty("configId", m_configId);

                Object obj = noteInterface.sendNote(note);
                if (obj != null && obj instanceof JsonObject) {
                    JsonObject json = (JsonObject) obj;

                    String filePath = json.get("path").getAsString();

                    if (json.get("isFile").getAsBoolean()) {
                        File walletFile = new File(filePath);
                        walletFileField.setText(walletFile.getName());
                    } else {
                        walletFileField.setText("(File not found) " + filePath);
                    }

                } else {
                    walletFileField.setText("(Unable to retreive wallet info) ");
                }
                walletNameField.setText(noteInterface.getName());
            };

            Runnable setWalletSettingsNonEditable = () -> {

                if (walletNameField.isEditable()) {
                    walletNameField.setEditable(false);
                    if (walletNameFieldBox.getChildren().contains(walletNameEnterBtn)) {
                        walletNameFieldBox.getChildren().remove(walletNameEnterBtn);
                    }
                }
                if (walletFileField.isEditable()) {
                    walletFileField.setEditable(false);
                    if (walletFileFieldBox.getChildren().contains(walletFileEnterBtn)) {
                        walletFileFieldBox.getChildren().remove(walletFileEnterBtn);
                    }
                }

                setWalletConfigInfo.run();
            };

            editNameLabel.setOnMouseClicked(e -> {
                NoteInterface noteInterface = m_selectedWallet.get();
                if (noteInterface == null) {
                    m_configBox.set(null);
                    return;
                }

                boolean isOpen = noteInterface != null && noteInterface.getConnectionStatus() != 0;

                if (isOpen) {
                    walletInfoTooltip.setText("Wallet in use");
                    walletInfoTooltip.show(editNameLabel, e.getScreenX(), e.getScreenY());
                    PauseTransition pt = new PauseTransition(Duration.millis(1600));
                    pt.setOnFinished(ptE -> {
                        walletInfoTooltip.hide();
                    });
                    pt.play();
                    setWalletSettingsNonEditable.run();
                } else {
                    if (walletNameField.isEditable()) {
                        setWalletSettingsNonEditable.run();
                    } else {
                        if (!walletNameFieldBox.getChildren().contains(walletNameEnterBtn)) {
                            walletNameFieldBox.getChildren().add(1, walletNameEnterBtn);
                        }
                        walletNameField.setEditable(true);
                        walletNameField.requestFocus();

                    }
                }
            });

            walletNameField.focusedProperty().addListener((obs, oldval, newval) -> {
                if (!newval) {
                    if (walletNameField.isEditable()) {
                        setWalletSettingsNonEditable.run();
                    }
                }
            });

            walletNameEnterBtn.setOnAction(e -> {
                NoteInterface noteInterface = m_selectedWallet.get();
                if (noteInterface == null) {
                    setWalletSettingsNonEditable.run();
                    m_configBox.set(null);
                    return;
                }

                String name = walletNameField.getText();

                JsonObject json = Utils.getCmdObject("updateName");
                json.addProperty("name", name);
                json.addProperty("networkId", m_locationId);
                json.addProperty("configId", m_configId);
                JsonObject updatedObj = (JsonObject) noteInterface.sendNote(json);
                if (updatedObj != null) {
                    JsonElement codeElement = updatedObj.get("code");
                    JsonElement msgElement = updatedObj.get("msg");

                    int code = codeElement != null && codeElement.isJsonPrimitive() ? codeElement.getAsInt() : -1;
                    String msg = msgElement != null && msgElement.isJsonPrimitive() ? msgElement.getAsString() : null;
                    if (code != App.WARNING) {
                        Point2D p = walletNameField.localToScene(0.0, 0.0);
                        walletInfoTooltip.setText(msg != null ? msg : "Error");
                        walletInfoTooltip.show(walletFileEditLbl,
                                p.getX() + walletFileField.getScene().getX()
                                        + walletFileField.getScene().getWindow().getX()
                                        + walletFileField.getLayoutBounds().getWidth(),
                                (p.getY() + walletFileField.getScene().getY()
                                        + walletFileField.getScene().getWindow().getY()) - 30);
                        PauseTransition pt = new PauseTransition(Duration.millis(1600));
                        pt.setOnFinished(ptE -> {
                            walletInfoTooltip.hide();
                        });
                        pt.play();
                    }
                    setWalletSettingsNonEditable.run();
                }
            });

            walletFileEditLbl.setOnMouseClicked(e -> {
                NoteInterface noteInterface = m_selectedWallet.get();
                if (noteInterface == null) {
                    m_configBox.set(null);
                    return;
                }

                boolean isOpen = noteInterface != null && noteInterface.getConnectionStatus() != 0;

                if (isOpen) {
                    walletInfoTooltip.setText("Wallet in use");
                    walletInfoTooltip.show(walletFileEditLbl, e.getScreenX(), e.getScreenY());
                    PauseTransition pt = new PauseTransition(Duration.millis(1600));
                    pt.setOnFinished(ptE -> {
                        walletInfoTooltip.hide();
                    });
                    pt.play();
                    setWalletSettingsNonEditable.run();
                } else {
                    if (walletFileField.isEditable()) {
                        setWalletSettingsNonEditable.run();
                    } else {
                        if (!walletFileFieldBox.getChildren().contains(walletFileEnterBtn)) {
                            walletFileFieldBox.getChildren().add(1, walletFileEnterBtn);
                        }
                        walletFileField.setEditable(true);
                        JsonObject note = Utils.getCmdObject("getFileData");
                        note.addProperty("configId", m_configId);
                        note.addProperty("networkId", m_locationId);

                        Object obj = noteInterface.sendNote(note);
                        if (obj != null && obj instanceof JsonObject) {
                            JsonObject json = (JsonObject) obj;
                            walletFileField.setText(json.get("path").getAsString());
                        }
                        walletFileField.requestFocus();

                    }
                }
            });

            walletFileField.focusedProperty().addListener((obs, oldval, newval) -> {
                if (walletFileField.isEditable() && !newval) {
                    setWalletSettingsNonEditable.run();
                }
            });

            walletFileEnterBtn.setOnAction(e -> {
                NoteInterface noteInterface = m_selectedWallet.get();
                if (noteInterface == null) {
                    setWalletSettingsNonEditable.run();
                    m_configBox.set(null);
                    return;
                }

                String fileString = walletFileField.getText();

                if (fileString.length() > 0 && Utils.findPathPrefixInRoots(fileString)) {

                    JsonObject note = Utils.getCmdObject("updateFile");
                    note.addProperty("file", fileString);
                    note.addProperty("networkId", m_locationId);
                    note.addProperty("configId", m_configId);
                    Object obj = noteInterface.sendNote(note);
                    if (obj != null && obj instanceof JsonObject) {
                        JsonObject resultObject = (JsonObject) obj;
                        JsonElement codeElement = resultObject.get("code");
                        JsonElement msgElement = resultObject.get("msg");
                        if (codeElement != null && msgElement != null) {
                            int code = codeElement.getAsInt();
                            String msg = codeElement.getAsString();
                            if (code != App.WARNING) {
                                Point2D p = walletNameField.localToScene(0.0, 0.0);
                                walletInfoTooltip.setText(msg != null ? msg : "Error");
                                walletInfoTooltip.show(walletFileEditLbl,
                                        p.getX() + walletFileField.getScene().getX()
                                                + walletFileField.getScene().getWindow().getX()
                                                + walletFileField.getLayoutBounds().getWidth(),
                                        (p.getY() + walletFileField.getScene().getY()
                                                + walletFileField.getScene().getWindow().getY()) - 30);
                                PauseTransition pt = new PauseTransition(Duration.millis(1600));
                                pt.setOnFinished(ptE -> {
                                    walletInfoTooltip.hide();
                                });
                                pt.play();
                            }
                            setWalletSettingsNonEditable.run();
                        } else {
                            setWalletSettingsNonEditable.run();
                            m_configBox.set(null);
                        }

                    } else {

                    }

                } else {
                    Point2D p = walletFileField.localToScene(0.0, 0.0);

                    walletInfoTooltip.setText("File not found");

                    walletInfoTooltip.show(walletFileField,
                            p.getX() + walletFileField.getScene().getX() + walletFileField.getScene().getWindow().getX()
                                    + walletFileField.getLayoutBounds().getWidth(),
                            (p.getY() + walletFileField.getScene().getY()
                                    + walletFileField.getScene().getWindow().getY()) - 30);
                    PauseTransition pt = new PauseTransition(Duration.millis(1600));
                    pt.setOnFinished(ptE -> {
                        walletInfoTooltip.hide();
                    });
                    pt.play();
                }
            });

            m_name.addListener((obs, oldval, newval) -> {
                if (newval != null) {
                    walletNameField.setText(newval);
                }
            });

            m_fileName.addListener((obs, oldval, newval) -> {
                if (newval != null) {
                    walletFileField.setText(newval);
                }
            });

            update();

        }

    }

    private class NewWalletBox extends AppBox {


        public NewWalletBox(boolean isNew) {
            super();


            Label backButton = new Label("🡄");
            backButton.setId("lblBtn");

            backButton.setOnMouseClicked(e -> {
                m_currentBox.set(null);
            });

            Label headingText = new Label((isNew ? "New" : "Restore") + " Wallet");
            headingText.setFont(App.txtFont);
            headingText.setPadding(new Insets(0,0,0,15));

            HBox headingBox = new HBox(backButton, headingText);
            headingBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headingBox, Priority.ALWAYS);
            headingBox.setPadding(new Insets(10, 15, 0, 15));
     
            VBox headerBox = new VBox(headingBox);
            headerBox.setPadding(new Insets(0, 5, 0, 0));

            TextArea mnemonicField = new TextArea(isNew ? Mnemonic.generateEnglishMnemonic() : "");
            mnemonicField.setFont(App.txtFont);
            mnemonicField.setId("textFieldCenter");
            mnemonicField.setEditable(!isNew);
            mnemonicField.setWrapText(true);
            mnemonicField.setPrefRowCount(4);
            mnemonicField.setPromptText("(enter mnemonic)");
            HBox.setHgrow(mnemonicField, Priority.ALWAYS);

            Platform.runLater(() -> mnemonicField.requestFocus());

            HBox mnemonicFieldBox = new HBox(mnemonicField);
            mnemonicFieldBox.setId("bodyBOx");
            mnemonicFieldBox.setPadding(new Insets(15, 0,0,0));

            VBox mnemonicBox = new VBox(mnemonicFieldBox);
            mnemonicBox.setAlignment(Pos.CENTER);
            mnemonicBox.setPadding(new Insets(20, 30, 20, 30));

            Region hBar = new Region();
            hBar.setPrefWidth(400);
            hBar.setPrefHeight(2);
            hBar.setMinHeight(2);
            hBar.setId("hGradient");

            HBox gBox = new HBox(hBar);
            gBox.setAlignment(Pos.CENTER);
            gBox.setPadding(new Insets(0, 0, 20, 0));

            Button nextBtn = new Button("Next");


            HBox nextBox = new HBox(nextBtn);
            nextBox.setAlignment(Pos.CENTER);
            nextBox.setPadding(new Insets(20, 0, 20, 0));

            HBox mnBodyBox = new HBox(mnemonicBox);
            HBox.setHgrow(mnBodyBox, Priority.ALWAYS);
        

            VBox bodyBox = new VBox(gBox, mnBodyBox, nextBox);
            VBox.setMargin(bodyBox, new Insets(10, 10, 0, 20));


            VBox layoutVBox = new VBox(headerBox, bodyBox);

            mnemonicFieldBox.setMaxWidth(900);
            HBox.setHgrow(mnemonicFieldBox, Priority.ALWAYS);

            getChildren().add(layoutVBox);


            Label backButton2 = new Label("🡄");
            backButton2.setId("lblBtn");

            backButton2.setOnMouseClicked(e -> {
                getChildren().clear();
                getChildren().add(layoutVBox);
            });

            Label headingText2 = new Label("Create password");
            headingText2.setFont(App.txtFont);
            headingText2.setPadding(new Insets(0,0,0,15));

            HBox headingBox2 = new HBox(backButton2, headingText2);
            headingBox2.prefHeight(40);
            headingBox2.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headingBox2, Priority.ALWAYS);
            headingBox2.setPadding(new Insets(10, 15, 0, 15));
           

            VBox headerBox2 = new VBox(headingBox2);
            headerBox2.setPadding(new Insets(0, 5, 2, 0));

            PasswordField passField = new PasswordField();
            passField.setPromptText("(enter password)");
            passField.setAlignment(Pos.CENTER);
            HBox.setHgrow(passField, Priority.ALWAYS);

            HBox passFieldBox = new HBox(passField);
            passFieldBox.setId("bodyBox");
            passFieldBox.setPrefHeight(20);
            passFieldBox.setMaxWidth(200);
            passFieldBox.setAlignment(Pos.CENTER);

            VBox passBox = new VBox(passFieldBox);
            passBox.setAlignment(Pos.CENTER);
            passBox.setPadding(new Insets(30, 30, 0, 30));

            PasswordField passField2 = new PasswordField();
            passField2.setPromptText("(re-enter password)");
            HBox.setHgrow(passField2, Priority.ALWAYS);
            passField2.setAlignment(Pos.CENTER);

            HBox passFieldBox2 = new HBox(passField2);
            passFieldBox2.setPrefHeight(20);
            passFieldBox2.setMaxWidth(200);
            passFieldBox2.setId("bodyBox");
            passFieldBox2.setAlignment(Pos.CENTER);

            VBox passBox2 = new VBox(passFieldBox2);
            
            passBox2.setAlignment(Pos.CENTER);
            passBox2.setPadding(new Insets(20, 30, 30, 30));

            VBox passwordsBox = new VBox(passBox, passBox2);
            passwordsBox.setAlignment(Pos.CENTER);
            HBox.setHgrow(passwordsBox,Priority.ALWAYS);

            Region hBar2 = new Region();
            hBar2.setPrefWidth(400);
            hBar2.setPrefHeight(2);
            hBar2.setMinHeight(2);
            hBar2.setId("hGradient");

            HBox gBox2 = new HBox(hBar2);
            gBox2.setAlignment(Pos.CENTER);
            gBox2.setPadding(new Insets(0, 0, 20, 0));

            Tooltip passwordsTip = new Tooltip();

            Button createBtn = new Button("Create");

            passField2.setOnAction(e->createBtn.fire());

            HBox createBox = new HBox(createBtn);
            createBox.setAlignment(Pos.CENTER);
            createBox.setPadding(new Insets(10, 0, 20, 0));

            HBox passwordBodyBox = new HBox(passwordsBox);
            HBox.setHgrow(passwordBodyBox,Priority.ALWAYS);
         

            VBox bodyBox2 = new VBox(gBox2,passwordBodyBox , createBox);
            VBox.setMargin(bodyBox2, new Insets(10, 10, 0, 20));
  


            VBox layoutVBox2 = new VBox(headingBox2, bodyBox2);

 
            Runnable createWallet = () -> {

                String password = passField.getText();

                if (password.length() > 3 && password.equals(passField2.getText())) {

                    FileChooser saveFileChooser = new FileChooser();
                    saveFileChooser.setTitle("Save wallet (*.erg)");
                    saveFileChooser.setInitialDirectory(AppData.HOME_DIRECTORY);
                    saveFileChooser.getExtensionFilters().add(ErgoWallets.ergExt);
                    saveFileChooser.setSelectedExtensionFilter(ErgoWallets.ergExt);

                    File walletFile = saveFileChooser.showSaveDialog(m_appStage);

                    if (walletFile != null) {

                        boolean isDotErg = walletFile != null && walletFile.getAbsolutePath().endsWith(".erg");
                        walletFile = isDotErg ? walletFile : new File(walletFile.getAbsolutePath() + ".erg"); 

                        Wallet.create(walletFile.toPath(), Mnemonic.create(SecretString.create(mnemonicField.getText()),
                                SecretString.create(password)), walletFile.getName(), password.toCharArray());

                        mnemonicField.setText("-");
                        passField.setText("");
                        passField2.setText("");

                        if (walletFile.isFile()) {
                            JsonObject note = Utils.getCmdObject("openWallet");
                            note.addProperty("networkId", App.WALLET_NETWORK);
                            note.addProperty("locationId", m_locationId);

                            JsonObject walletData = new JsonObject();
                            walletData.addProperty("networkType", m_networkType.toString());
                            walletData.addProperty("path", walletFile.getAbsolutePath());

                            note.add("data", walletData);
                            Object result = m_ergoNetworkInterface.sendNote(note);
                            if (result != null && result instanceof JsonObject) {
                                JsonObject walletJson = (JsonObject) result;
                                JsonElement idElement = walletJson.get("id");
                                if (idElement != null && idElement.isJsonPrimitive()) {
                                 
                                    setDefault(idElement.getAsString());
                                    
                                }
                                    
                            }
                            
                            m_currentBox.set(null); 
                        }   

                    }

                }else{
                    Point2D p = createBtn.localToScene(0.0, 0.0);
                    passwordsTip.setText(password.length() < 6  ? "Password too weak" :  "Passwords do not match" );
                    passwordsTip.show(createBtn,
                            p.getX() + createBtn.getScene().getX()
                                    + createBtn.getScene().getWindow().getX() - 60,
                            (p.getY() + createBtn.getScene().getY()
                                    + createBtn.getScene().getWindow().getY()) - 40);
                    PauseTransition pt = new PauseTransition(Duration.millis(1600));
                    pt.setOnFinished(ptE -> {
                        passwordsTip.hide();
                    });
                    pt.play();
                }
            };

            nextBtn.setOnAction(e -> {
                String mnemonic =  mnemonicField.getText().trim();
                String[] words = mnemonic.split("\\s+");
                List<String> items = Arrays.asList(words);
                int size = mnemonic.equals("") ? 0 : items.size();
                final int mnemonicSize = 15;
                if(size == mnemonicSize){
                    try{
                        Mnemonic.checkEnglishMnemonic(items);
                        getChildren().clear();
                        getChildren().add(layoutVBox2);
                    }catch(MnemonicValidationException validationEx){
                        Point2D p = nextBtn.localToScene(0.0, 0.0);
                        passwordsTip.setText(validationEx.toString());
                        passwordsTip.show(nextBtn,
                                p.getX() + nextBtn.getScene().getX()
                                        + nextBtn.getScene().getWindow().getX() - 60,
                                (p.getY() + nextBtn.getScene().getY()
                                        + nextBtn.getScene().getWindow().getY()) - 40);
                        PauseTransition pt = new PauseTransition(Duration.millis(1600));
                        pt.setOnFinished(ptE -> {
                            passwordsTip.hide();
                        });
                        pt.play();
                    }
                }else{
                    Point2D p = nextBtn.localToScene(0.0, 0.0);
                    passwordsTip.setText(items.size() + " of " + mnemonicSize + " words");
                    passwordsTip.show(nextBtn,
                            p.getX() + nextBtn.getScene().getX()
                                    + nextBtn.getScene().getWindow().getX() - 60,
                            (p.getY() + nextBtn.getScene().getY()
                                    + nextBtn.getScene().getWindow().getY()) - 40);
                    PauseTransition pt = new PauseTransition(Duration.millis(1600));
                    pt.setOnFinished(ptE -> {
                        passwordsTip.hide();
                    });
                    pt.play();
                }
                
            });


            createBtn.setOnAction(e -> createWallet.run());

        }
    }

    private class RemoveWalletBox extends AppBox {

        private Button nextBtn = new Button("Remove");
        private Tooltip tooltip = new Tooltip();

        private SimpleObjectProperty<JsonArray> m_walletIds = new SimpleObjectProperty<>(null);

        @Override
        public void sendMessage(int code, long timeStamp,String networkId, String str){
            switch(code){
                case App.LIST_ITEM_ADDED:
                case App.LIST_ITEM_REMOVED:
                case App.LIST_UPDATED:
                    updateWalletList();
                break;
                case App.ERROR:
                    showError(str);
                break;
            }
        }

        private void showError(String errText){
            Point2D p = nextBtn.localToScene(0.0, 0.0);
                    tooltip.setText(errText);
                    tooltip.show(nextBtn,
                            p.getX() + nextBtn.getScene().getX()
                                    + nextBtn.getScene().getWindow().getX() - 60,
                            (p.getY() + nextBtn.getScene().getY()
                                    + nextBtn.getScene().getWindow().getY()) - 40);
                    PauseTransition pt = new PauseTransition(Duration.millis(1600));
                    pt.setOnFinished(ptE -> {
                        tooltip.hide();
                    });
                    pt.play();
        }

        private void updateWalletList(){
            JsonObject note = Utils.getCmdObject("getWallets");
            note.addProperty("networkId", App.WALLET_NETWORK);
            note.addProperty("locationId", m_locationId);

            Object result =  m_ergoNetworkInterface.sendNote(note);

            if(result != null && result instanceof JsonArray){
                m_walletIds.set((JsonArray) result);
            }else{
                m_walletIds.set(null);
            }
           
        }

        public RemoveWalletBox() {



            Label backButton = new Label("🡄");
            backButton.setId("lblBtn");

            backButton.setOnMouseClicked(e -> {
                m_currentBox.set(null);
            });

            Label headingText = new Label("Remove Wallet");
            headingText.setFont(App.txtFont);
            headingText.setPadding(new Insets(0,0,0,15));

            HBox headingBox = new HBox(backButton, headingText);
            headingBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headingBox, Priority.ALWAYS);
            headingBox.setPadding(new Insets(10, 15, 0, 15));
     
            VBox headerBox = new VBox(headingBox);
            headerBox.setPadding(new Insets(0, 5, 0, 0));

            Region hBar = new Region();
            hBar.setPrefWidth(400);
            hBar.setPrefHeight(2);
            hBar.setMinHeight(2);
            hBar.setId("hGradient");

            HBox gBox = new HBox(hBar);
            gBox.setAlignment(Pos.CENTER);
            gBox.setPadding(new Insets(0, 0, 20, 0));

            VBox listBox = new VBox();
            listBox.setPadding(new Insets(10));
            listBox.setId("bodyBox");

            ScrollPane listScroll = new ScrollPane(listBox);
            listScroll.setPrefViewportHeight(120);

            HBox walletListBox = new HBox(listScroll);
            walletListBox.setPadding(new Insets(0,40,0, 40));
         
            HBox.setHgrow(walletListBox, Priority.ALWAYS);

           

          
            listScroll.prefViewportWidthProperty().bind(walletListBox.widthProperty().subtract(1));

            listScroll.viewportBoundsProperty().addListener((obs,oldval,newval)->{
                listBox.setMinWidth(newval.getWidth());
                listBox.setMinHeight(newval.getHeight());
            });
           
            HBox nextBox = new HBox(nextBtn);
            nextBox.setAlignment(Pos.CENTER);
            nextBox.setPadding(new Insets(20, 0, 0, 0));


            Label noticeText = new Label("Notice: ");
            noticeText.setId("smallPrimaryColor");
            noticeText.setMinWidth(58);

            TextArea noticeTxt = new TextArea("The associated (.erg) file will not be deleted.");
            noticeTxt.setId("smallSecondaryColor");
            noticeTxt.setWrapText(true);
            noticeTxt.setPrefHeight(40);
            noticeTxt.setEditable(false );

            HBox noticeBox = new HBox(noticeText, noticeTxt);
            HBox.setHgrow(noticeBox,Priority.ALWAYS);
            noticeBox.setAlignment(Pos.CENTER);
            noticeBox.setPadding(new Insets(10,20,0,20));

            VBox bodyBox = new VBox(gBox, walletListBox,noticeBox, nextBox);
            VBox.setMargin(bodyBox, new Insets(10, 10, 0, 10));


            VBox layoutVBox = new VBox(headerBox, bodyBox);

            JsonArray removeIds = new JsonArray();

            

            m_walletIds.addListener((obs,oldval, walletIds)->{
                listBox.getChildren().clear();

                if (walletIds != null) {
    
                    for (JsonElement element : walletIds) {
                        if (element != null && element.isJsonObject()) {
                            JsonObject json = element.getAsJsonObject();

                            String name = json.get("name").getAsString();
                         
                            Label nameText = new Label(name);
                            nameText.setFont(App.txtFont);
                            nameText.setPadding(new Insets(0,0,0,20));

                            Text checkBox = new Text(" ");
                            checkBox.setFill(Color.BLACK);
                            Runnable addItemToRemoveIds = ()->{
                               
                                if(!removeIds.contains(element)){
                                    removeIds.add(element);
                                }
                            };

                            Runnable removeItemFromRemoveIds = () ->{
                                removeIds.remove(element);
                            };
                            //toggleBox
                            //toggleBoxPressed
                            Runnable toggleCheck = ()->{
                                if(checkBox.getText().equals(" ")){
                                    checkBox.setText("🗶");
                                    addItemToRemoveIds.run();    
                                }else{
                                    checkBox.setText(" ");
                                    removeItemFromRemoveIds.run();
                                }
                         
                            };

                            HBox checkBoxBox = new HBox(checkBox);
                            checkBoxBox.setId("xBtn");
                            

                            HBox walletItem = new HBox(checkBoxBox, nameText);
                            walletItem.setAlignment(Pos.CENTER_LEFT);
                            walletItem.setMinHeight(25);
                            HBox.setHgrow(walletItem, Priority.ALWAYS);
                            walletItem.setId("rowBtn");
                            walletItem.setPadding(new Insets(2,5,2,5));
                            
                            walletItem.addEventFilter(MouseEvent.MOUSE_CLICKED, e->toggleCheck.run());

                            listBox.getChildren().add(walletItem);
                        }
                    }
                }
            });
          
            updateWalletList();

            nextBtn.setOnAction(e->{
                if(removeIds.size() == 0){
                    showError("No wallets selected");
                }else{
                    JsonObject note = Utils.getCmdObject("removeWallet");
                    note.addProperty("locationId", m_locationId);
                    note.addProperty("networkId", App.WALLET_NETWORK);
                    note.add("ids", removeIds);
                    
                    m_ergoNetworkInterface.sendNote(note);
            
                    
                   
                }
            });

            getChildren().add(layoutVBox);
        }
    
    }

    private class SendAppBox extends AppBox {
        
        private LockField m_sendLockBox;
        private ErgoWalletAmountSendBoxes m_amountBoxes;
        private Button m_sendBtn = new Button("Send");
        private Tooltip m_tooltip = new Tooltip();
      
   
        public SendAppBox(LockField lockBox){
            super();
            m_sendLockBox = lockBox;

            Label backButton = new Label("🡄");
            backButton.setId("lblBtn");

            backButton.setOnMouseClicked(e -> {
                m_currentBox.set(null);
            });

            Label headingText = new Label("Send assets");
            headingText.setFont(App.txtFont);
            headingText.setPadding(new Insets(0,0,0,15));

            HBox headingBox = new HBox(backButton, headingText);
            headingBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headingBox, Priority.ALWAYS);
            headingBox.setPadding(new Insets(5, 15, 5, 15));
        
            VBox headerBox = new VBox(headingBox);
            headerBox.setPadding(new Insets(0, 5, 0, 0));

            Region hBar = new Region();
            hBar.setPrefWidth(400);
            hBar.setPrefHeight(2);
            hBar.setMinHeight(2);
            hBar.setId("hGradient");

            HBox gBox = new HBox(hBar);
            gBox.setAlignment(Pos.CENTER);
            gBox.setPadding(new Insets(0, 0, 20, 0));

  
            Label addressText = new Label("Address ");
            addressText.setFont(App.txtFont);
            addressText.setMinWidth(90);

            HBox addressTextBox = new HBox(addressText);
            addressTextBox.setAlignment(Pos.CENTER_LEFT);
            addressTextBox.setPadding(new Insets(0,0,2,5));

            Region addressHBar = new Region();
            addressHBar.setPrefWidth(400);
            addressHBar.setPrefHeight(2);
            addressHBar.setMinHeight(2);
            addressHBar.setId("hGradient");

            HBox addressGBox = new HBox(addressHBar);
            addressGBox.setAlignment(Pos.CENTER);
            addressGBox.setPadding(new Insets(0,0,0,0));

            AddressBox toAddress = new AddressBox(new AddressInformation(""), m_appStage.getScene(), m_networkType);
            HBox.setHgrow(toAddress, Priority.ALWAYS);
            toAddress.getInvalidAddressList().add(m_sendLockBox.getAddress());
        
            HBox addressFieldBox = new HBox(toAddress);
            HBox.setHgrow(addressFieldBox, Priority.ALWAYS);
           // addressFieldBox.setId("bodyBox ");
        
            
            
            VBox addressPaddingBox = new VBox(addressTextBox, addressGBox, addressFieldBox);
            addressPaddingBox.setPadding(new Insets(0,10,10,10));
    
            Label amountText = new Label("Amount ");
            amountText.setFont(App.txtFont);
            
            HBox amountTextBox = new HBox(amountText);
            amountTextBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(amountTextBox, Priority.ALWAYS);
            amountTextBox.setPadding(new Insets(10, 0,2,5));

            Region amountHBar = new Region();
            amountHBar.setPrefWidth(400);
            amountHBar.setPrefHeight(2);
            amountHBar.setMinHeight(2);
            amountHBar.setId("hGradient");

            HBox amountGBox = new HBox(amountHBar);
            amountGBox.setAlignment(Pos.CENTER);
            amountGBox.setPadding(new Insets(0,0,10,0));

            m_amountBoxes = new ErgoWalletAmountSendBoxes(m_appStage.getScene(), NetworkType.MAINNET, m_balanceObject);
            HBox.setHgrow(m_amountBoxes, Priority.ALWAYS);
   
            VBox walletListBox = new VBox( m_amountBoxes);
            walletListBox.setPadding(new Insets(0, 0, 10, 5));
            walletListBox.minHeight(80);
            HBox.setHgrow(walletListBox, Priority.ALWAYS);

            Label feesLabel = new Label("Fee");
            feesLabel.setFont(App.txtFont);
            feesLabel.setMinWidth(50);

            String feesFieldId = FriendlyId.createFriendlyId();

            TextField feesField = new TextField("0.001");
            feesField.setPrefWidth(100);
            feesField.setUserData(feesFieldId);
            feesField.textProperty().addListener((obs,oldval,newval)->{
                String number = newval.replaceAll("[^0-9.]", "");
      
                int index = number.indexOf(".");
                String leftSide = index != -1 ? number.substring(0, index + 1) : number;
                String rightSide = index != -1 ?  number.substring(index + 1) : "";
                rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
                rightSide = rightSide.length() > ErgoCurrency.FRACTIONAL_PRECISION ? rightSide.substring(0, ErgoCurrency.FRACTIONAL_PRECISION) : rightSide;
                feesField.setText(leftSide +  rightSide);
            });
      
            

            MenuButton feeTypeBtn = new MenuButton();
            feeTypeBtn.setFont(App.txtFont);

            MenuItem ergFeeTypeItem = new MenuItem(String.format("%-20s", "Ergo")+ "(ERG)");
            ergFeeTypeItem.setOnAction(e->{
                PriceCurrency currency = new ErgoCurrency(m_networkType);
                feeTypeBtn.setUserData(currency);
                feeTypeBtn.setText(currency.getSymbol());
            });

            feeTypeBtn.getItems().add(ergFeeTypeItem);

            Runnable setFee = ()->{
                String feesString = feesField.getText().length() == 0 ? "0" : feesField.getText();
                BigDecimal fee = new BigDecimal(feesString);
                
                Object feeTypeUserData = feeTypeBtn.getUserData();
                if(feeTypeUserData != null && feeTypeUserData instanceof PriceCurrency){
                    PriceCurrency currency = (PriceCurrency) feeTypeUserData;
                    m_amountBoxes.feeAmountProperty().set(new PriceAmount(fee, currency));
                }
            };

            feeTypeBtn.textProperty().addListener((obs,oldval,newval)->{
                setFee.run();
            });
         
            SimpleBooleanProperty isFeesFocused = new SimpleBooleanProperty(false);

            m_appStage.getScene().focusOwnerProperty().addListener((obs, old, newPropertyValue) -> {
                if (newPropertyValue != null && newPropertyValue instanceof TextField) {
                    TextField focusedField = (TextField) newPropertyValue;
                    Object userData = focusedField.getUserData();
                    if(userData != null && userData instanceof String){
                        String userDataString = (String) userData;
                        if(userDataString.equals(feesFieldId)){
                            isFeesFocused.set(true);
                        }else{
                            if(isFeesFocused.get()){
                                isFeesFocused.set(false);
                                setFee.run();
                            }
                        }
                    }else{
                        if(isFeesFocused.get()){
                            isFeesFocused.set(false);
                            setFee.run();
                        }
                    }
                }else{
                    if(isFeesFocused.get()){
                        isFeesFocused.set(false);
                        setFee.run();
                    }
                }
            });


            ergFeeTypeItem.fire();
            HBox feeEnterBtnBox = new HBox();
            feeEnterBtnBox.setAlignment(Pos.CENTER_LEFT);

            HBox feesFieldBox = new HBox(feesField, feeEnterBtnBox, feeTypeBtn);
            feesFieldBox.setId("bodyBox");
            feesFieldBox.setAlignment(Pos.CENTER_LEFT);
            feesFieldBox.setPadding(new Insets(2));


            HBox feesBox = new HBox(feesLabel, feesFieldBox);
            HBox.setHgrow(feesBox, Priority.ALWAYS);
            feesBox.setAlignment(Pos.CENTER_RIGHT);
            feesBox.setPadding(new Insets(0,10,20,0));


            Region sendHBar = new Region();
            sendHBar.setPrefWidth(400);
            sendHBar.setPrefHeight(2);
            sendHBar.setMinHeight(2);
            sendHBar.setId("hGradient");

            HBox sendGBox = new HBox(sendHBar);
            sendGBox.setAlignment(Pos.CENTER);
            sendGBox.setPadding(new Insets(10,0,0,0));


            VBox amountPaddingBox = new VBox(amountTextBox, amountGBox, walletListBox, feesBox, sendGBox);
            amountPaddingBox.setPadding(new Insets(0,10,0,10));


        
            HBox nextBox = new HBox(m_sendBtn);
            nextBox.setAlignment(Pos.CENTER);
            nextBox.setPadding(new Insets(20, 0, 0, 0));


            VBox sendBodyBox = new VBox(addressPaddingBox, amountPaddingBox, nextBox);

            VBox bodyContentBox = new VBox(sendBodyBox);

            VBox bodyBox = new VBox(gBox, bodyContentBox);
            VBox.setMargin(bodyBox, new Insets(10, 0, 10, 0));



            VBox layoutVBox = new VBox(headerBox, bodyBox);

            Runnable resetSend = ()->{
                bodyContentBox.getChildren().clear();
                feesField.setText("0.001");
                toAddress.addressInformationProperty().set(new AddressInformation(""));
                m_amountBoxes.reset();
                bodyContentBox.getChildren().add(sendBodyBox);
            };

            Runnable addSendBox = ()->{
                bodyContentBox.getChildren().clear();
                bodyContentBox.getChildren().add(sendBodyBox);
            };

            m_sendBtn.setOnAction(e->{

                bodyContentBox.getChildren().clear();

                Text sendText = new Text("Sending...");
                sendText.setFont(App.txtFont);
                sendText.setFill(App.txtColor);

                HBox sendTextBox = new HBox(sendText);
                HBox.setHgrow(sendTextBox, Priority.ALWAYS);
                sendTextBox.setAlignment(Pos.CENTER);

                ProgressBar progressBar = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
                progressBar.setPrefWidth(400);
                
                VBox statusBox = new VBox(sendTextBox, progressBar);
                HBox.setHgrow(statusBox, Priority.ALWAYS);
                statusBox.setAlignment(Pos.CENTER);
                statusBox.setPrefHeight(200);

                bodyContentBox.getChildren().add(statusBox);
                

                JsonObject sendObject = new JsonObject();
               
                AddressInformation addressInformation = toAddress.addressInformationProperty().get();

                if (addressInformation == null  || (addressInformation != null && addressInformation.getAddress() == null))  {
                    
                    addSendBox.run();
                    showError("Error: Enter valid recipient address");
                    return;
                }

                if (!toAddress.isAddressValid().get())  {
                    
                    addSendBox.run();
                    showError("Error: Address is an invalid recipient");
                    return;
                }
               
                JsonObject getDefaultExplorerNote = Utils.getCmdObject("getDefault");
                getDefaultExplorerNote.addProperty("networkId", App.EXPLORER_NETWORK);
                getDefaultExplorerNote.addProperty("locationId", m_locationId);
                Object explorerObj = m_ergoNetworkInterface.sendNote(getDefaultExplorerNote);

                if(explorerObj == null){
                    
                    addSendBox.run();
                    showError("Error: No explorer selected");
                    return;
                }

                JsonObject explorerObject = (JsonObject) explorerObj;
                JsonElement namedExplorerElement = explorerObject.get("ergoNetworkUrl");

                JsonObject namedExplorerJson = namedExplorerElement.getAsJsonObject();
                JsonObject explorerJson = new JsonObject();

                try {
                    ErgoNetworkUrl explorerUrl = new ErgoNetworkUrl(namedExplorerJson);
                    explorerJson.addProperty("url", explorerUrl.getUrlString());
                } catch (Exception e1) {
                    
                    addSendBox.run();
                    showError("Error: " + e1.toString());
                    return;
                }

                JsonObject getDefaultNodeNote = Utils.getCmdObject("getDefaultInterface");
                getDefaultNodeNote.addProperty("networkId", App.NODE_NETWORK);
                getDefaultNodeNote.addProperty("locationId", m_locationId);
               
                Object nodeObj = m_ergoNetworkInterface.sendNote(getDefaultNodeNote);
              
                NoteInterface nodeInterface = nodeObj != null && nodeObj instanceof NoteInterface ? (NoteInterface) nodeObj : null;

                if(nodeInterface == null){
                    
                    addSendBox.run();
                    showError("Error: No node selected");
                    return;
                }
                
    
                JsonElement namedNodeElement = nodeInterface.getJsonObject().get("namedNode");

                if(namedNodeElement == null || (namedNodeElement != null && !namedNodeElement.isJsonObject())){
                    
                    addSendBox.run();
                    showError("Error: Node information not found");
                    return;
                }

                JsonObject namedNodeJson = namedNodeElement.getAsJsonObject();
                JsonObject nodeJson = new JsonObject();

                try {
                    NamedNodeUrl namedNode = new NamedNodeUrl(namedNodeJson);
                    nodeJson.addProperty("name", nodeInterface.getName());
                    nodeJson.addProperty("url", namedNode.getUrlString());
                    nodeJson.addProperty("apiKey", namedNode.getApiKey());
                } catch (Exception e1) {
                    
                    addSendBox.run();
                    showError("Error: " + e1.toString());
                    return;
                }

                sendObject.add("node", nodeJson);
                sendObject.add("explorer", explorerJson);

                NoteInterface walletInterface = m_selectedWallet.get();

                if(walletInterface == null){
                    
                    addSendBox.run();
                    showError("Error: No wallet selected");
                    return;
                }

                JsonObject walletObject = new JsonObject();
                walletObject.addProperty("name", walletInterface.getName());
                walletObject.addProperty("address", m_sendLockBox.getAddress());

                sendObject.add("wallet", walletObject);


                JsonObject recipientObject = new JsonObject();
                recipientObject.addProperty("address", addressInformation.getAddress().toString());
                recipientObject.addProperty("addressType", addressInformation.getAddressType());
                
                sendObject.add("recipient", recipientObject);

                BigDecimal minimumDecimal = m_amountBoxes.minimumFeeProperty().get();
                PriceAmount feePriceAmount = m_amountBoxes.feeAmountProperty().get();
                if(minimumDecimal == null || (feePriceAmount == null || (feePriceAmount != null && feePriceAmount.amountProperty().get().compareTo(minimumDecimal) == -1))){
                    addSendBox.run();
                    showError("Error: Minimum fee " +(minimumDecimal != null ? ("of " + minimumDecimal) : "unavailable") + " " +feeTypeBtn.getText() + " required");
                    return;
                }
        
                sendObject.add("fee", feePriceAmount.getAmountObject());                

                //sendObject.add("feeAmount", feeObject);
                JsonArray sendAssets = new JsonArray();


                AmountBoxInterface[] amountBoxAray =  m_amountBoxes.getAmountBoxArray();
                
                for(int i = 0; i < amountBoxAray.length ;i++ ){
                    AmountBoxInterface amountBox =amountBoxAray[i];
                
                    ErgoWalletAmountSendBox sendBox = (ErgoWalletAmountSendBox) amountBox;
                    
                    PriceAmount sendAmount = sendBox.getSendAmount();
                    if(sendAmount!= null){
        
                        sendAssets.add(sendAmount.getAmountObject());
                    
                    }
                    
                }
            
                if(sendObject.size() == 0){
                    addSendBox.run();
                    showError("Enter assets to send");
                    return;
                }
            
                sendObject.add("assets", sendAssets);

                JsonObject networkJson = Utils.getJsonObject("networkType", m_networkType.toString());

                sendObject.add("network", networkJson);

                JsonObject note = Utils.getCmdObject("sendAssets");
                note.addProperty("accessId", m_sendLockBox.getLockId());
                note.addProperty("locationId", m_locationId);
                note.add("data", sendObject);

                
                
                walletInterface.sendNote(note, (onComplete)->{
                    Object sourceObject = onComplete.getSource().getValue();
                    if(sourceObject != null && sourceObject instanceof JsonObject){

                        JsonObject receiptJson = (JsonObject) sourceObject;
                        
                        JsonElement codeElement = receiptJson.get("code");
                        JsonElement txIdElement = receiptJson.get("txId");;
                        JsonElement errTxIdElement = receiptJson.get("errTxId");
                        JsonElement resultElement = receiptJson.get("result");
                        int code = codeElement != null ? codeElement.getAsInt() : -1;

                        String resultString = resultElement != null ? resultElement.getAsString() : "Failed";

                        receiptJson.addProperty("result", resultString);
                        
                        Label sendReceiptText = new Label("Send Receipt");
                        sendReceiptText.setFont(App.txtFont);
                        
                        HBox sendReceiptTextBox = new HBox(sendReceiptText);
                        sendReceiptTextBox.setAlignment(Pos.CENTER_LEFT);
                        HBox.setHgrow(sendReceiptTextBox, Priority.ALWAYS);
                        sendReceiptTextBox.setPadding(new Insets(10, 0,2,5));
            
            
                        Region sendReceiptHBar = new Region();
                        sendReceiptHBar.setPrefWidth(400);
                        sendReceiptHBar.setPrefHeight(2);
                        sendReceiptHBar.setMinHeight(2);
                        sendReceiptHBar.setId("hGradient");
                    
                        HBox sendReceiptHBarGBox = new HBox(sendReceiptHBar);
                        sendReceiptHBarGBox.setAlignment(Pos.CENTER);
                        sendReceiptHBarGBox.setPadding(new Insets(0,0,10,0));

                        JsonParametersBox sendReceiptJsonBox = new JsonParametersBox((JsonObject) null, 120);
                        HBox.setHgrow(sendReceiptJsonBox, Priority.ALWAYS);
                        sendReceiptJsonBox.setPadding(new Insets(2,10,0,10));
                        sendReceiptJsonBox.updateParameters(receiptJson);



                        Button exportBtn = new Button("🖫 Export JSON…");
                        exportBtn.setOnAction(onSave->{
                            ExtensionFilter txtFilter = new FileChooser.ExtensionFilter("JSON (application/json)", "*.json");
                            FileChooser saveChooser = new FileChooser();
                            saveChooser.setTitle("🖫 Export JSON…");
                            saveChooser.getExtensionFilters().addAll(txtFilter);
                            saveChooser.setSelectedExtensionFilter(txtFilter);
                           
                            String id = txIdElement != null ? "tx_" + txIdElement.getAsString() : (errTxIdElement != null ? errTxIdElement.getAsString() : "TxErr");

                            saveChooser.setInitialFileName(id + ".json");
                            File saveFile = saveChooser.showSaveDialog(m_appStage);
                            if(saveFile != null){
                                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                                
                                try {
                                    Files.writeString(saveFile.toPath(), gson.toJson(receiptJson));
                                } catch (IOException e1) {
                                    Alert alert = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                                    alert.setTitle("Error");
                                    alert.setHeaderText("Error");
                                    alert.initOwner(m_appStage);
                                    alert.show();
                                }
                            }
                        });

                        HBox exportBtnBox = new HBox(exportBtn);
                        exportBtnBox.setAlignment(Pos.CENTER_RIGHT);
                        exportBtnBox.setPadding(new Insets(15,15,15,0));

                        Region receiptBottomHBar = new Region();
                        receiptBottomHBar.setPrefWidth(400);
                        receiptBottomHBar.setPrefHeight(2);
                        receiptBottomHBar.setMinHeight(2);
                        receiptBottomHBar.setId("hGradient");
            
                        HBox receiptBottomGBox = new HBox(receiptBottomHBar);
                        receiptBottomGBox.setAlignment(Pos.CENTER);
                        receiptBottomGBox.setPadding(new Insets(10,0,0,0));


                        Button receiptBottomOkBtn = new Button("Ok");
                        receiptBottomOkBtn.setOnAction(onOk->{
                            if(code == App.SUCCESS){
                                resetSend.run();
                                m_currentBox.set(null);
                            }else{
                                addSendBox.run();
                            }
                        });


                        HBox receiptBottomOkBox = new HBox(receiptBottomOkBtn);
                        receiptBottomOkBox.setAlignment(Pos.CENTER);
                        receiptBottomOkBox.setPadding(new Insets(20, 0, 0, 0));


                        VBox receiptContentBox = new VBox(sendReceiptTextBox,sendReceiptHBarGBox, sendReceiptJsonBox, exportBtnBox, receiptBottomGBox, receiptBottomOkBox);

                        bodyContentBox.getChildren().clear();
                        bodyContentBox.getChildren().add(receiptContentBox);
                        
                    }

                }, (onError)->{
                    addSendBox.run();
                    showError("Error: " + onError.getSource().getException().toString());

                });
                
                
                
                
            });
    
                getChildren().add(layoutVBox);
            }

            public void showError(String msg){

                int stringWidth = Utils.getStringWidth(msg);

                Point2D p = m_sendBtn.localToScene(0.0, 0.0);
                m_tooltip.setText(msg);
                m_tooltip.show(m_sendBtn,
                        p.getX() + m_sendBtn.getScene().getX()
                                + m_sendBtn.getScene().getWindow().getX() - (stringWidth/2),
                        (p.getY() + m_sendBtn.getScene().getY()
                                + m_sendBtn.getScene().getWindow().getY()) - 40);
                PauseTransition pt = new PauseTransition(Duration.millis(5000));
                pt.setOnFinished(ptE -> {
                    m_tooltip.hide();
                });
                pt.play();
            }

        }

       

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

/*
 * //New
 * // Scene mnemonicScene = createMnemonicScene(friendlyId,
 * walletNameField.getText(), nodeId, explorerId,
 * marketsId,tokenMarketIdProperty.get(), tokensEnabled, networkType, stage);
 * 
 * //Restore
 * 
 * restoreWalletBtn.setOnAction(clickEvent -> {
 * String seedPhrase = restoreMnemonicStage();
 * if (!seedPhrase.equals("")) {
 * Button passBtn = new Button();
 * Stage passwordStage = App.createPassword(m_ergoWallet.getName() +
 * " - Restore wallet: Password", m_ergoWallet.getIcon(),
 * m_ergoWallet.getAppIcon(), passBtn,
 * m_ergoWallet.getNetworksData().getExecService(), onSuccess -> {
 * Object sourceObject = onSuccess.getSource().getValue();
 * 
 * if (sourceObject != null && sourceObject instanceof String) {
 * 
 * String passwordString = (String) sourceObject;
 * if (!passwordString.equals("")) {
 * Mnemonic mnemonic = Mnemonic.create(SecretString.create(seedPhrase),
 * SecretString.create(passwordString));
 * 
 * FileChooser saveFileChooser = new FileChooser();
 * // saveFileChooser.setInitialDirectory(getWalletsDirectory());
 * saveFileChooser.setTitle("Save: Wallet file");
 * saveFileChooser.getExtensionFilters().add(ErgoWallets.ergExt);
 * saveFileChooser.setSelectedExtensionFilter(ErgoWallets.ergExt);
 * 
 * File walletFile = saveFileChooser.showSaveDialog(appStage);
 * 
 * if (walletFile != null) {
 * 
 * try {
 * 
 * 
 * Wallet.create(walletFile.toPath(), mnemonic, seedPhrase,
 * passwordString.toCharArray());
 * 
 * NetworkType networkType = selectedNetworkType.get();
 * String nodeId = selectedNodeData.get() == null ? null :
 * selectedNodeData.get().getId();
 * String explorerId = selectedExplorerData.get() == null ? null :
 * selectedExplorerData.get().getId();
 * String marketsId = marketIdProperty.get();
 * boolean tokensEnabled =ergoTokensEnabledProperty.get();
 * 
 * ErgoWalletData walletData = new ErgoWalletData(friendlyId,
 * walletNameField.getText(), walletFile, networkType, m_ergoWallet);
 * add(walletData);
 * 
 * 
 * 
 * } catch (Exception e1) {
 * Alert a = new Alert(AlertType.NONE, "Wallet creation: Cannot be saved.\n\n" +
 * e1.toString(), ButtonType.OK);
 * a.initOwner(appStage);
 * a.show();
 * }
 * }
 * 
 * }
 * 
 * passBtn.fire();
 * }
 * });
 * passwordStage.show();
 * }
 * 
 * });
 */