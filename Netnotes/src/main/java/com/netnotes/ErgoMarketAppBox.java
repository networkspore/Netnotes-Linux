package com.netnotes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class ErgoMarketAppBox extends AppBox {
    private final String selectString = "[disabled]";

    private Stage m_appStage;
    private SimpleObjectProperty<AppBox> m_currentBox = new SimpleObjectProperty<>(null);
    private NoteInterface m_ergoNetworkInterface;
    private VBox m_mainBox;
    
    private SimpleBooleanProperty m_showBody = new SimpleBooleanProperty(false);
    private SimpleObjectProperty<NoteInterface> m_selectedMarket = new SimpleObjectProperty<>(null);

    private String m_locationId = null;

    private HBox m_ergoMarketsFieldBox;
    private MenuButton m_ergoMarketsMenuBtn;
    private Button m_disableBtn;

    private TextField m_marketPriceField;
    private Label m_marketPriceCurrency;
    private JsonParametersBox m_priceParametersBox = null;
    private NoteMsgInterface m_marketMsgInterface;
    private SimpleObjectProperty<PriceQuote> m_tickerPriceQuote = new SimpleObjectProperty<>(null);
    private SimpleIntegerProperty m_exchangeStatus = new SimpleIntegerProperty(App.STOPPED);
    private String m_statusMsg = "Unavailable";
    private SimpleBooleanProperty m_showTickerBody = new SimpleBooleanProperty(false);

    public ErgoMarketAppBox(Stage appStage, String locationId, NoteInterface ergoNetworkInterface){
        super();
        m_ergoNetworkInterface = ergoNetworkInterface;
        m_appStage = appStage;
        m_locationId = locationId;

    
        ImageView logoIconView = new ImageView(new Image(ErgoMarkets.getSmallAppIconString()));
        logoIconView.setPreserveRatio(true);
        logoIconView.setFitHeight(18);

        
        HBox topIconBox = new HBox(logoIconView);
        topIconBox.setAlignment(Pos.CENTER_LEFT);
        topIconBox.setMinWidth(30);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ImageView closeImage = App.highlightedImageView(App.closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);
      
        Button toggleShowOptions = new Button(m_showBody.get() ? "⏷" : "⏵");
        toggleShowOptions.setId("caretBtn");
        toggleShowOptions.setOnAction(e->{
            m_showBody.set(!m_showBody.get());
        });

        MenuButton marketMenuBtn = new MenuButton("⋮");


        Text topLogoText = new Text(String.format("%-13s", "Ergo Market"));
        topLogoText.setFont(App.txtFont);
        topLogoText.setFill(App.txtColor);


        m_ergoMarketsMenuBtn = new MenuButton();
        m_ergoMarketsMenuBtn.setId("arrowMenuButton");
        m_ergoMarketsMenuBtn.showingProperty().addListener((obs,oldval,newval)->{
            if(newval){
                updateMarkets();
            }
        });

        m_ergoMarketsFieldBox = new HBox(m_ergoMarketsMenuBtn);
        HBox.setHgrow(m_ergoMarketsFieldBox, Priority.ALWAYS);
        m_ergoMarketsFieldBox.setAlignment(Pos.CENTER_LEFT);
        m_ergoMarketsFieldBox.setId("bodyBox");
        m_ergoMarketsFieldBox.setPadding(new Insets(0, 1, 0, 0));
        m_ergoMarketsFieldBox.setMaxHeight(18);

        m_ergoMarketsMenuBtn.prefWidthProperty().bind(m_ergoMarketsFieldBox.widthProperty().subtract(1));

        HBox marketMenuBtnPadding = new HBox(marketMenuBtn);
        marketMenuBtnPadding.setPadding(new Insets(0, 0, 0, 5));



        HBox ergoMarketsBtnBox = new HBox(m_ergoMarketsFieldBox, marketMenuBtnPadding);
        ergoMarketsBtnBox.setPadding(new Insets(2, 2, 0, 5));
        HBox.setHgrow(ergoMarketsBtnBox, Priority.ALWAYS);

        VBox marketsBodyPaddingBox = new VBox();
        HBox.setHgrow(marketsBodyPaddingBox, Priority.ALWAYS);
        marketsBodyPaddingBox.setPadding(new Insets(0,10,0,0));





        HBox topBar = new HBox(toggleShowOptions, topIconBox, topLogoText, ergoMarketsBtnBox);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(2));

        VBox layoutBox = new VBox(topBar, marketsBodyPaddingBox);
        HBox.setHgrow(layoutBox, Priority.ALWAYS);

       

        Button toggleShowTickerInfo = new Button(m_showTickerBody.get() ? "⏷" : "⏵");
        toggleShowTickerInfo.setId("caretBtn");
        toggleShowTickerInfo.setOnAction(e->{
            if(m_tickerPriceQuote.get() != null){
                m_showTickerBody.set(!m_showTickerBody.get());
            }else{
                if(m_showTickerBody.get() == true){
                    m_showTickerBody.set(!m_showTickerBody.get());
                }
            }
            
        });

 

        Label logoLbl = new Label("📟");
        logoLbl.setId("logoBtn");

        Label marketPriceText = new Label("Ticker");
        marketPriceText.setMinWidth(145);

        m_marketPriceField = new TextField();
        HBox.setHgrow(m_marketPriceField, Priority.ALWAYS);
        m_marketPriceField.setEditable(false);
        m_marketPriceField.setAlignment(Pos.CENTER);

       

 



        Binding<String> quoteBinding = Bindings.createObjectBinding(()->{
            PriceQuote quote = m_tickerPriceQuote.get();
            int code = m_exchangeStatus.get();
            switch(code){
                case App.LIST_CHANGED:
                case App.LIST_UPDATED:
                    if(quote != null){
                        return quote.getAmountString();
                    }
                break;
            }
            return m_statusMsg;
        }, m_tickerPriceQuote, m_exchangeStatus);

        m_marketPriceField.textProperty().bind(quoteBinding);

        m_marketPriceCurrency = new Label("");

        Binding<String> quoteCurrencyBinding = Bindings.createObjectBinding(()->{
            PriceQuote quote = m_tickerPriceQuote.get();
            int code = m_exchangeStatus.get();
            switch(code){
                case App.LIST_CHANGED:
                case App.LIST_UPDATED:
                    if(quote != null){
                        return quote.getBaseSymbol() + "/" + quote.getQuoteSymbol();
                    }
                break;
            }
            return "";
        }, m_tickerPriceQuote, m_exchangeStatus);


        m_marketPriceCurrency.textProperty().bind(quoteCurrencyBinding);

        HBox marketPriceFieldBox = new HBox(m_marketPriceField);
        HBox.setHgrow(marketPriceFieldBox, Priority.ALWAYS);
        marketPriceFieldBox.setAlignment(Pos.CENTER_LEFT);
        marketPriceFieldBox.setId("bodyBox");

        HBox marketPriceCurrencyBox = new HBox(m_marketPriceCurrency);
        marketPriceCurrencyBox.setAlignment(Pos.CENTER_LEFT);

        HBox marketPriceBox = new HBox(toggleShowTickerInfo, logoLbl, marketPriceText, marketPriceFieldBox, marketPriceCurrencyBox);
        marketPriceBox.setAlignment(Pos.CENTER_LEFT);
        marketPriceBox.setPadding(new Insets(5,0,2,0));

        VBox marketPriceBodyBox = new VBox(marketPriceBox);
        marketPriceBodyBox.setPadding(new Insets(2,0,2,5));


        m_showTickerBody.addListener((obs,oldval,newval)->{
           
            toggleShowTickerInfo.setText(newval ? "⏷" : "⏵");
            if(newval){
                
                if(m_priceParametersBox == null){
                    PriceQuote pricequote = m_tickerPriceQuote.get();
                    JsonObject quoteJson = pricequote != null ? pricequote.getJsonObject() : Utils.getJsonObject("", "*(no information)");
                    m_priceParametersBox = new JsonParametersBox(quoteJson,150);
                    m_priceParametersBox.setPadding(new Insets(5, 10,2,25));
                    marketPriceBodyBox.getChildren().add(m_priceParametersBox);
                }
            }else{
                if(m_priceParametersBox != null){
                    marketPriceBodyBox.getChildren().remove(m_priceParametersBox);
                    m_priceParametersBox = null;
                }
            }
        });


    

        SimpleBooleanProperty showMarketInfo = new SimpleBooleanProperty(false);

        JsonParametersBox marketInfoParamBox = new JsonParametersBox(Utils.getJsonObject("marketInformation", "disabled"), 160);
        marketInfoParamBox.setPadding(new Insets(2,0,2,15));

        Button toggleShowMarketInfo = new Button(showMarketInfo.get() ? "⏷" : "⏵");
        toggleShowMarketInfo.setId("caretBtn");
        toggleShowMarketInfo.setOnAction(e -> showMarketInfo.set(!showMarketInfo.get()));
      
        Label infoLbl = new Label("🛈");
        infoLbl.setId("logoBtn");

        Text marketInfoText = new Text("Info");
        marketInfoText.setFont(App.txtFont);
        marketInfoText.setFill(App.txtColor);

        HBox toggleMarketInfoBox = new HBox(toggleShowMarketInfo, infoLbl, marketInfoText);
        HBox.setHgrow(toggleMarketInfoBox, Priority.ALWAYS);
        toggleMarketInfoBox.setAlignment(Pos.CENTER_LEFT);
        toggleMarketInfoBox.setPadding(new Insets(2,0,2,0));

        VBox marketInfoVBox = new VBox(toggleMarketInfoBox);
        marketInfoVBox.setPadding(new Insets(2));



        showMarketInfo.addListener((obs,oldval,newval)->{
            toggleShowMarketInfo.setText(newval ? "⏷" : "⏵");
            if(newval){
                if( !marketInfoVBox.getChildren().contains(marketInfoParamBox)){
                    marketInfoVBox.getChildren().addAll(marketInfoParamBox);
                }
              
            }else{
                if(marketInfoVBox.getChildren().contains(marketInfoParamBox)){
                    marketInfoVBox.getChildren().remove(marketInfoParamBox);
                }
            }
        });

        VBox bodyBox = new VBox(marketPriceBodyBox, marketInfoVBox);
        marketInfoVBox.setPadding(new Insets(5,0,0,5));

        Runnable setMarketInfo = ()->{
            NoteInterface marketInterface = m_selectedMarket.get();
            JsonObject marketJson = m_selectedMarket.get() != null ? marketInterface.getJsonObject() : null;
            
            if(marketJson != null){
                marketJson.remove("name");
                marketJson.remove("networkId");
                marketInfoParamBox.updateParameters(  marketJson);
                if (!m_ergoMarketsFieldBox.getChildren().contains(m_disableBtn)) {
                    m_ergoMarketsFieldBox.getChildren().add(m_disableBtn);
                }
            }else{
                marketInfoParamBox.updateParameters(Utils.getJsonObject("marketInformation", "disabled"));
                if (m_ergoMarketsFieldBox.getChildren().contains(m_disableBtn)) {
                    m_ergoMarketsFieldBox.getChildren().remove(m_disableBtn);
                }
            }
            m_ergoMarketsMenuBtn.setText(marketInterface != null ?marketInterface.getName() : selectString);
        };
        setMarketInfo.run();
      
        
        m_showBody.addListener((obs, oldval, newval) -> {

            toggleShowOptions.setText(newval ? "⏷" : "⏵");

            if (newval) {
                if (!marketsBodyPaddingBox.getChildren().contains(bodyBox)) {
                    marketsBodyPaddingBox.getChildren().add(bodyBox);
                }
               
            } else {
                if (marketsBodyPaddingBox.getChildren().contains(bodyBox)) {
                    marketsBodyPaddingBox.getChildren().remove(bodyBox);
                }
            }
            connectToExchange(newval, m_selectedMarket.get());
        });

        m_selectedMarket.addListener((obs,oldval,newval)->{
            setMarketInfo.run();
            if(oldval != null){
         
                connectToExchange(false, oldval);
              
            }
            if(newval != null){
                connectToExchange(m_showBody.get(), newval);
            }
        });

        m_tickerPriceQuote.addListener((obs,oldval,newval)->{
            JsonObject quoteJson = newval != null ? newval.getJsonObject() : Utils.getJsonObject("", "*(no information)");
            if(m_priceParametersBox != null){
                m_priceParametersBox.updateParameters(quoteJson);
            }
        });


        m_disableBtn = new Button("☓");
        m_disableBtn.setId("lblBtn");

        m_disableBtn.setOnMouseClicked(e -> {
            m_showBody.set(false);
            clearDefault();
        });

        m_mainBox = new VBox(layoutBox);
        m_mainBox.setPadding(new Insets(0));
        HBox.setHgrow(m_mainBox, Priority.ALWAYS);

        m_currentBox.addListener((obs, oldval, newval) -> {
            m_mainBox.getChildren().clear();
            if (newval != null) {
                m_mainBox.getChildren().add(newval);
            } else {
                m_mainBox.getChildren().add(layoutBox);
            }

        });

        getDefaultMarket();


        getChildren().addAll(m_mainBox);
        setPadding(new Insets(0,0,5,0));
    }

    public void setDefaultMarket(String id){
        JsonObject note = Utils.getCmdObject("setDefault");
        note.addProperty("networkId", ErgoNetwork.MARKET_NETWORK);
        note.addProperty("locationId", m_locationId);
        note.addProperty("id", id);
        
        m_ergoNetworkInterface.sendNote(note);
    }

    public void clearDefault(){
        JsonObject note = Utils.getCmdObject("clearDefault");
        note.addProperty("networkId", ErgoNetwork.MARKET_NETWORK);
        note.addProperty("locationId", m_locationId);
        m_ergoNetworkInterface.sendNote(note);
    }

    public void getDefaultMarket(){
        
        JsonObject note = Utils.getCmdObject("getDefaultMarketInterface");
        note.addProperty("networkId", ErgoNetwork.MARKET_NETWORK);
        note.addProperty("locationId", m_locationId);
        Object obj = m_ergoNetworkInterface.sendNote(note);;
        NoteInterface noteInterface =obj != null && obj instanceof NoteInterface ? (NoteInterface) obj : null;
        m_selectedMarket.set(noteInterface);
        
    }


    public void updateMarkets(){
       
        JsonObject note = Utils.getCmdObject("getMarkets");
        note.addProperty("networkId", ErgoNetwork.MARKET_NETWORK);
        note.addProperty("locationId", m_locationId);

        Object objResult = m_ergoNetworkInterface.sendNote(note);

        m_ergoMarketsMenuBtn.getItems().clear();

        if (objResult != null && objResult instanceof JsonArray) {

            JsonArray explorersArray = (JsonArray) objResult;

            for (JsonElement element : explorersArray) {
                
                JsonObject json = element.getAsJsonObject();

                String name = json.get("name").getAsString();
                String id = json.get("networkId").getAsString();

                MenuItem menuItems = new MenuItem(String.format("%-20s", " " + name));
                menuItems.setOnAction(action -> {
                    m_ergoMarketsMenuBtn.hide();
                    setDefaultMarket(id);
                });
                m_ergoMarketsMenuBtn.getItems().add(menuItems);
                
            }
       
       
        }else{
            MenuItem explorerItem = new MenuItem(String.format("%-50s", " Unable to find available markets."));
            m_ergoMarketsMenuBtn.getItems().add(explorerItem);
        }

    }

    @Override
    public void sendMessage(int code, long timestamp,String networkId, String msg){
        
        if(networkId != null && networkId.equals(ErgoNetwork.MARKET_NETWORK)){

            switch(code){
                
                case ErgoMarkets.MARKET_LIST_DEFAULT_CHANGED:
                    getDefaultMarket(); 
                break;
              
            }

            AppBox appBox  = m_currentBox.get();
            if(appBox != null){
       
                appBox.sendMessage(code, timestamp,networkId, msg);
                
            }

        }
    
    }
    private String m_msgId;

    public void connectToExchange(boolean connect, NoteInterface exchangeInterface){
     
        if(connect && exchangeInterface != null){
            m_msgId = FriendlyId.createFriendlyId();
            m_marketMsgInterface = new NoteMsgInterface() {
               
                public String getId(){
                    return m_msgId;
                }

                public void sendMessage(int code, long timeStamp, String poolId, Number num){

                    switch(code){
                        case App.LIST_CHANGED:
                        case App.LIST_UPDATED:
                            //updated

                                getQuote();
                            
                            m_exchangeStatus.set(code);
                            break;
                        case App.STOPPED:
                            m_exchangeStatus.set(code);
                        break;
                        case App.STARTED:
                            m_exchangeStatus.set(code);
                        break;
                        case App.STARTING:
                            m_exchangeStatus.set(code);
                        break;
                        case App.ERROR:
                        
                        break;
                    } 
                    
                }
            
                public void sendMessage(int code, long timestamp, String networkId, String msg){
                    if(code == App.ERROR){
                
                        m_statusMsg = msg;
                        m_exchangeStatus.set(code);
                    }
                }

        

            };
    
            if(exchangeInterface.getConnectionStatus() == App.STARTED){
                getQuote();
            }

            exchangeInterface.addMsgListener(m_marketMsgInterface);

            

        }else{
            if(m_marketMsgInterface != null && exchangeInterface != null){
                exchangeInterface.removeMsgListener(m_marketMsgInterface);  
            }
            m_marketMsgInterface = null;
            m_tickerPriceQuote.set(null);
        }
    }

    public void getQuote(){
        NoteInterface exchangeInterface = m_selectedMarket.get();
     
        if(exchangeInterface != null){
            JsonObject note = Utils.getCmdObject("getErgoUSDQuote");
            note.addProperty("locationId", m_locationId);

            Object result =  exchangeInterface.sendNote(note);
            if(result != null && result instanceof PriceQuote){
                PriceQuote priceQuote = (PriceQuote) result;
                
                m_tickerPriceQuote.set(priceQuote);
            }
        }
    }

    @Override
    public void shutdown(){
        NoteInterface selectedMarket = m_selectedMarket.get();
        if(selectedMarket != null){
            connectToExchange(false, selectedMarket);
        }
    }
}
