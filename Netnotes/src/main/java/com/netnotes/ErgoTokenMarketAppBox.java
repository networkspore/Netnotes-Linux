package com.netnotes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class ErgoTokenMarketAppBox extends AppBox {
    private final String selectString = "[disabled]";

    private Stage m_appStage;
    private SimpleObjectProperty<AppBox> m_currentBox = new SimpleObjectProperty<>(null);
    private NoteInterface m_ergoNetworkInterface;
    private VBox m_mainBox;
    
    private SimpleBooleanProperty m_showInformation = new SimpleBooleanProperty(false);
    private SimpleObjectProperty<NoteInterface> m_selectedMarket = new SimpleObjectProperty<>(null);

    private String m_locationId = null;

    private HBox m_tokenMarketsFieldBox;
    private MenuButton m_tokenMarketsMenuBtn;
    private Button m_disableBtn;

    public ErgoTokenMarketAppBox(Stage appStage, String locationId, NoteInterface ergoNetworkInterface){
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
      
        Button toggleShowOptions = new Button(m_showInformation.get() ? "⏷" : "⏵");
        toggleShowOptions.setId("caretBtn");
        toggleShowOptions.setOnAction(e->{
            m_showInformation.set(!m_showInformation.get());
        });

        MenuButton marketMenuBtn = new MenuButton("⋮");


        Text topLogoText = new Text(String.format("%-13s", "Token Market"));
        topLogoText.setFont(App.txtFont);
        topLogoText.setFill(App.txtColor);


        m_tokenMarketsMenuBtn = new MenuButton();
        m_tokenMarketsMenuBtn.setId("arrowMenuButton");
        m_tokenMarketsMenuBtn.showingProperty().addListener((obs,oldval,newval)->{
            if(newval){
                updateMarkets();
            }
        });

        m_tokenMarketsFieldBox = new HBox(m_tokenMarketsMenuBtn);
        HBox.setHgrow(m_tokenMarketsFieldBox, Priority.ALWAYS);
        m_tokenMarketsFieldBox.setAlignment(Pos.CENTER_LEFT);
        m_tokenMarketsFieldBox.setId("bodyBox");
        m_tokenMarketsFieldBox.setPadding(new Insets(0, 1, 0, 0));
        m_tokenMarketsFieldBox.setMaxHeight(18);

        m_tokenMarketsMenuBtn.prefWidthProperty().bind(m_tokenMarketsFieldBox.widthProperty().subtract(1));

        HBox marketMenuBtnPadding = new HBox(marketMenuBtn);
        marketMenuBtnPadding.setPadding(new Insets(0, 0, 0, 5));



        HBox tokenMarketsBtnBox = new HBox(m_tokenMarketsFieldBox, marketMenuBtnPadding);
        tokenMarketsBtnBox.setPadding(new Insets(2, 2, 0, 5));
        HBox.setHgrow(tokenMarketsBtnBox, Priority.ALWAYS);

        VBox marketsBodyPaddingBox = new VBox();
        HBox.setHgrow(marketsBodyPaddingBox, Priority.ALWAYS);
        marketsBodyPaddingBox.setPadding(new Insets(0,10,0,0));





        HBox topBar = new HBox(toggleShowOptions, topIconBox, topLogoText, tokenMarketsBtnBox);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(2));

        VBox layoutBox = new VBox(topBar, marketsBodyPaddingBox);
        HBox.setHgrow(layoutBox, Priority.ALWAYS);

         SimpleBooleanProperty showMarketInfo = new SimpleBooleanProperty(false);

        JsonParametersBox marketInfoParamBox = new JsonParametersBox(Utils.getJsonObject("marketInformation", "disabled"), 150);
        marketInfoParamBox.setPadding(new Insets(2,0,0,5));

        Button toggleShowMarketInfo = new Button(showMarketInfo.get() ? "⏷" : "⏵");
        toggleShowMarketInfo.setId("caretBtn");
        toggleShowMarketInfo.setOnAction(e -> showMarketInfo.set(!showMarketInfo.get()));
      
        Label infoLbl = new Label("🛈");
        infoLbl.setId("logoBtn");

        Text marketInfoText = new Text("Market Information");
        marketInfoText.setFont(App.txtFont);
        marketInfoText.setFill(App.txtColor);

        HBox toggleMarketInfoBox = new HBox(toggleShowMarketInfo, infoLbl, marketInfoText);
        HBox.setHgrow(toggleMarketInfoBox, Priority.ALWAYS);
        toggleMarketInfoBox.setAlignment(Pos.CENTER_LEFT);
        toggleMarketInfoBox.setPadding(new Insets(0,0,2,0));

        VBox marketInfoVBox = new VBox(toggleMarketInfoBox);
        marketInfoVBox.setPadding(new Insets(2));

        showMarketInfo.addListener((obs,oldval,newval)->{
            toggleShowMarketInfo.setText(newval ? "⏷" : "⏵");
            marketInfoVBox.getChildren().add(marketInfoParamBox);
        });

        VBox bodyBox = new VBox(marketInfoVBox);
        marketInfoVBox.setPadding(new Insets(5,0,0,5));

        Runnable setMarketInfo = ()->{
            NoteInterface marketInterface = m_selectedMarket.get();
            JsonObject marketJson = m_selectedMarket.get() != null ? marketInterface.getJsonObject() : null;
            
            if(marketJson != null){
                marketJson.remove("name");
                marketJson.remove("networkId");
                marketInfoParamBox.updateParameters(  marketJson);
                if (!m_tokenMarketsFieldBox.getChildren().contains(m_disableBtn)) {
                    m_tokenMarketsFieldBox.getChildren().add(m_disableBtn);
                }
            }else{
                marketInfoParamBox.updateParameters(Utils.getJsonObject("marketInformation", "disabled"));
                if (m_tokenMarketsFieldBox.getChildren().contains(m_disableBtn)) {
                    m_tokenMarketsFieldBox.getChildren().remove(m_disableBtn);
                }
            }
            m_tokenMarketsMenuBtn.setText(marketInterface != null ?marketInterface.getName() : selectString);
        };
        setMarketInfo.run();
      
        
        m_showInformation.addListener((obs, oldval, newval) -> {

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
        });

        m_selectedMarket.addListener((obs,oldval,newval)->{
            setMarketInfo.run();
        });


        m_disableBtn = new Button("☓");
        m_disableBtn.setId("lblBtn");

        m_disableBtn.setOnMouseClicked(e -> {
            m_showInformation.set(false);
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

        getDefaultTokenMarket();


        getChildren().addAll(m_mainBox);
        setPadding(new Insets(0,0,5,0));
    }

    public void setDefaultMarket(String id){
        JsonObject note = Utils.getCmdObject("setDefaultTokenMarket");
        note.addProperty("networkId", ErgoNetwork.MARKET_NETWORK);
        note.addProperty("locationId", m_locationId);
        note.addProperty("id", id);
        
        m_ergoNetworkInterface.sendNote(note);
    }

    public void clearDefault(){
        JsonObject note = Utils.getCmdObject("clearDefaultTokenMarket");
        note.addProperty("networkId", ErgoNetwork.MARKET_NETWORK);
        note.addProperty("locationId", m_locationId);
        m_ergoNetworkInterface.sendNote(note);
    }

    public void getDefaultTokenMarket(){
        
        JsonObject note = Utils.getCmdObject("getDefaultTokenInterface");
        note.addProperty("networkId", ErgoNetwork.MARKET_NETWORK);
        note.addProperty("locationId", m_locationId);
        Object obj = m_ergoNetworkInterface.sendNote(note);;
        NoteInterface noteInterface =obj != null && obj instanceof NoteInterface ? (NoteInterface) obj : null;
        m_selectedMarket.set(noteInterface);
        
    }


    public void updateMarkets(){
       
        JsonObject note = Utils.getCmdObject("getTokenMarkets");
        note.addProperty("networkId", ErgoNetwork.MARKET_NETWORK);
        note.addProperty("locationId", m_locationId);

        Object objResult = m_ergoNetworkInterface.sendNote(note);

        m_tokenMarketsMenuBtn.getItems().clear();

        if (objResult != null && objResult instanceof JsonArray) {

            JsonArray explorersArray = (JsonArray) objResult;

            for (JsonElement element : explorersArray) {
                
                JsonObject json = element.getAsJsonObject();

                String name = json.get("name").getAsString();
                String id = json.get("networkId").getAsString();

                MenuItem menuItems = new MenuItem(String.format("%-20s", " " + name));
                menuItems.setOnAction(action -> {
                    m_tokenMarketsMenuBtn.hide();
                    setDefaultMarket(id);
                });
                m_tokenMarketsMenuBtn.getItems().add(menuItems);
                
            }
       
       
        }else{
            MenuItem explorerItem = new MenuItem(String.format("%-50s", " Unable to find available markets."));
            m_tokenMarketsMenuBtn.getItems().add(explorerItem);
        }

    }

    @Override
    public void sendMessage(int code, long timestamp,String networkId, String msg){
        
        if(networkId != null && networkId.equals(ErgoNetwork.MARKET_NETWORK)){

            switch(code){
                
                case ErgoMarkets.TOKEN_LIST_DEFAULT_CHANGED:
                    getDefaultTokenMarket(); 
                break;
              
            }

            AppBox appBox  = m_currentBox.get();
            if(appBox != null){
       
                appBox.sendMessage(code, timestamp,networkId, msg);
                
            }

        }
    
    }


}
