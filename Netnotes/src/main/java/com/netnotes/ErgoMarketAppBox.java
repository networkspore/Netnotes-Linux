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

public class ErgoMarketAppBox extends AppBox {
    private final String selectString = "[disabled]";

    private Stage m_appStage;
    private SimpleObjectProperty<AppBox> m_currentBox = new SimpleObjectProperty<>(null);
    private NoteInterface m_ergoNetworkInterface;
    private VBox m_mainBox;
    
    private SimpleBooleanProperty m_showInformation = new SimpleBooleanProperty(false);
    private SimpleObjectProperty<NoteInterface> m_selectedMarket = new SimpleObjectProperty<>(null);

    private String m_locationId = null;

    private HBox m_ergoMarketsFieldBox;
    private MenuButton m_ergoMarketsMenuBtn;
    private Button m_disableBtn;

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
      
        Button toggleShowOptions = new Button(m_showInformation.get() ? "⏷" : "⏵");
        toggleShowOptions.setId("caretBtn");
        toggleShowOptions.setOnAction(e->{
            m_showInformation.set(!m_showInformation.get());
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

        

        JsonParametersBox marketInfoBox = new JsonParametersBox(Utils.getJsonObject("marketInformation", "disabled"), 160);
        marketInfoBox.setPadding(new Insets(2,0,2,30));

        Runnable setMarketInfo = ()->{
            NoteInterface marketInterface = m_selectedMarket.get();
            JsonObject marketJson = m_selectedMarket.get() != null ? marketInterface.getJsonObject() : null;
            
            if(marketJson != null){
                marketJson.remove("name");
                marketJson.remove("networkId");
                marketInfoBox.updateParameters(  marketJson);
                if (!m_ergoMarketsFieldBox.getChildren().contains(m_disableBtn)) {
                    m_ergoMarketsFieldBox.getChildren().add(m_disableBtn);
                }
            }else{
                marketInfoBox.updateParameters(Utils.getJsonObject("marketInformation", "disabled"));
                if (m_ergoMarketsFieldBox.getChildren().contains(m_disableBtn)) {
                    m_ergoMarketsFieldBox.getChildren().remove(m_disableBtn);
                }
            }
            m_ergoMarketsMenuBtn.setText(marketInterface != null ?marketInterface.getName() : selectString);
        };
        setMarketInfo.run();
      
        
        m_showInformation.addListener((obs, oldval, newval) -> {

            toggleShowOptions.setText(newval ? "⏷" : "⏵");

            if (newval) {
                if (!marketsBodyPaddingBox.getChildren().contains(marketInfoBox)) {
                    marketsBodyPaddingBox.getChildren().add(marketInfoBox);
                }
            } else {
                if (marketsBodyPaddingBox.getChildren().contains(marketInfoBox)) {
                    marketsBodyPaddingBox.getChildren().remove(marketInfoBox);
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


}
