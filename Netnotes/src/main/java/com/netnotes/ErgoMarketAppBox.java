package com.netnotes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
    
    private Stage m_appStage;
    private SimpleObjectProperty<AppBox> m_currentBox = new SimpleObjectProperty<>(null);
    private NoteInterface m_ergoNetwork;
    private VBox m_mainBox;
    private SimpleObjectProperty<NoteInterface> m_ergoMarketInterface = null;

    private String m_locationId = null;
    private SimpleBooleanProperty m_showMarkets = new SimpleBooleanProperty(false);

    public ErgoMarketAppBox(Stage appStage, String locationId, String marketId, NoteInterface ergoNetwork,  SimpleObjectProperty<NoteInterface> ergoMarketObject){
        super();
        m_locationId = null;
        m_appStage = appStage;
        m_ergoMarketInterface = ergoMarketObject;
        m_ergoNetwork = ergoNetwork;

        setMarket(marketId);


        final String selectString = "[select]";

        ImageView marketIconView = new ImageView(new Image(ErgoMarkets.getAppIconString()));
        marketIconView.setPreserveRatio(true);
        marketIconView.setFitHeight(18);

        TextField marketField = new TextField();
        HBox.setHgrow(marketField, Priority.ALWAYS);

        marketField.setAlignment(Pos.CENTER_LEFT);
        marketField.setText(selectString);

        marketField.setMinWidth(90);
        marketField.setEditable(false);
        marketField.setId("hand");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ImageView closeImage = App.highlightedImageView(App.closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        Label toggleShowMarkets = new Label(m_showMarkets.get() ? "⏷ " : "⏵ ");
        toggleShowMarkets.setId("caretBtn");
        toggleShowMarkets.setMinWidth(25);


        Label openmarketBtn = new Label("⏷");
        openmarketBtn.setId("lblBtn");




        MenuButton marketMenuBtn = new MenuButton("⋮");
        marketMenuBtn.setId("lblBtn");
        marketMenuBtn.setFocusTraversable(true);

        Text marketTopLabel = new Text(String.format("%-8s","Market"));
        marketTopLabel.setFont(App.txtFont);
        marketTopLabel.setFill(App.txtColor);


        Label openMenuBtn = new Label("⏷");
        openMenuBtn.setId("lblBtn");


        HBox marketFieldBox = new HBox(marketField, openMenuBtn);
        HBox.setHgrow(marketFieldBox, Priority.ALWAYS);
        marketFieldBox.setAlignment(Pos.CENTER_LEFT);
        marketFieldBox.setId("bodyBox");
        marketFieldBox.setPadding(new Insets(0, 1, 0, 0));
        marketFieldBox.setMaxHeight(18);

        HBox marketMenuBtnPadding = new HBox(marketMenuBtn);
        marketMenuBtnPadding.setPadding(new Insets(0, 0, 0, 5));



        HBox marketBtnBox = new HBox(marketFieldBox, marketMenuBtnPadding);
        marketBtnBox.setPadding(new Insets(2, 2, 0, 5));
        HBox.setHgrow(marketBtnBox, Priority.ALWAYS);

        VBox marketBodyPaddingBox = new VBox();
        HBox.setHgrow(marketBodyPaddingBox, Priority.ALWAYS);
        marketBodyPaddingBox.setPadding(new Insets(0,10,0,10));

        Runnable updateMarketMenu = () -> {
            JsonObject note = Utils.getCmdObject("getMarkets");
            note.addProperty("networkId", App.MARKET_NETWORK);
            note.addProperty("locationId", m_locationId);

            

            JsonArray marketsArray = (JsonArray) m_ergoNetwork.sendNote(note);

            marketMenuBtn.getItems().clear();

    
            if(marketsArray != null){

                for (JsonElement element : marketsArray) {
                    if (element != null && element instanceof JsonObject) {
                        JsonObject json = element.getAsJsonObject();

                        String name = json.get("name").getAsString();
                        String id = json.get("id").getAsString();

                        MenuItem marketItem = new MenuItem(String.format("%-50s", "📊  " + name));

                        marketItem.setOnAction(action -> {
                            JsonObject getMarketObject = Utils.getCmdObject("getMarketById");
                            getMarketObject.addProperty("networkId", App.MARKET_NETWORK);
                            getMarketObject.addProperty("locationId", m_locationId);
                            getMarketObject.addProperty("id", id);
                            Object obj = ergoNetwork.sendNote(getMarketObject);
                            if(obj != null && obj instanceof NoteInterface){
                                NoteInterface marketInterface = (NoteInterface) obj; 
                                
                                m_ergoMarketInterface.set(marketInterface);
                                m_showMarkets.set(true);
                            }
                        });

                        marketMenuBtn.getItems().add(marketItem);
                    }

                }
                
            }

            
            

        };

        Binding<String> marketNameBinding = Bindings.createObjectBinding(()->m_ergoMarketInterface.get() != null ? m_ergoMarketInterface.get().getName() : selectString, m_ergoMarketInterface);

        marketField.textProperty().bind(marketNameBinding);


        

        
        Label marketNameIcon = new Label("  ");
        marketNameIcon.setId("logoBtn");

        Label marketNameText = new Label("Name"); 
        marketNameText.setFont(App.txtFont);
        marketNameText.setPadding(new Insets(0,5,0,5));
        marketNameText.setMinWidth(100);

        TextField marketNameField = new TextField();
        marketNameField.setEditable(false);
        HBox.setHgrow(marketNameField,Priority.ALWAYS);

        HBox marketNameFieldBox = new HBox(marketNameField);
        HBox.setHgrow(marketNameFieldBox,Priority.ALWAYS);
        marketNameFieldBox.setId("bodyBox");
        marketNameFieldBox.setAlignment(Pos.CENTER_LEFT);
        marketNameFieldBox.setMaxHeight(18);

        HBox marketNameBox = new HBox(marketNameIcon, marketNameText, marketNameFieldBox);
        marketNameBox.setAlignment(Pos.CENTER_LEFT);
        marketNameBox.setPadding(new Insets(2,0,0,0));

        Label marketUrlIcon = new Label("  ");
        marketUrlIcon.setId("logoBtn");
  

        Label marketUrlText = new Label("Url"); 
        marketUrlText.setFont(App.txtFont);
        marketUrlText.setPadding(new Insets(0,5,0,5));
        marketUrlText.setMinWidth(100);

        TextField marketUrlField = new TextField();
        marketUrlField.setEditable(false);
        HBox.setHgrow(marketUrlField,Priority.ALWAYS);

        HBox marketUrlFieldBox = new HBox(marketUrlField);
        HBox.setHgrow(marketUrlFieldBox,Priority.ALWAYS);
        marketUrlFieldBox.setId("bodyBox");
        marketUrlFieldBox.setAlignment(Pos.CENTER_LEFT);
        marketUrlFieldBox.setMaxHeight(18);
     
        HBox marketUrlBox = new HBox(marketUrlIcon, marketUrlText, marketUrlFieldBox);
        marketUrlBox.setAlignment(Pos.CENTER_LEFT);
        marketUrlBox.setPadding(new Insets(2,0,0,0));


        HBox marketLabelBox = new HBox(marketTopLabel);
        marketLabelBox.setPadding(new Insets(0, 5, 0, 5));
        marketLabelBox.setAlignment(Pos.CENTER_LEFT);


        HBox marketsTopBar = new HBox(toggleShowMarkets, marketIconView, marketLabelBox, marketBtnBox);
        marketsTopBar.setAlignment(Pos.CENTER_LEFT);
        marketsTopBar.setPadding(new Insets(2));

        VBox marketLayoutBox = new VBox(marketsTopBar, marketBodyPaddingBox);
        HBox.setHgrow(marketLayoutBox, Priority.ALWAYS);



        Runnable setMarketInfo = ()->{
            NoteInterface noteInterface =  m_ergoMarketInterface.get();

            if(noteInterface != null){

                JsonObject json = noteInterface.getJsonObject();

                JsonElement networkUrlElement = json.get("namedMarket");

                ErgoNetworkUrl ergoNetworkUrl = null;

                try{
                    ergoNetworkUrl = networkUrlElement != null && networkUrlElement.isJsonObject() ? new ErgoNetworkUrl(networkUrlElement.getAsJsonObject()) : null;
                }catch(Exception e){

                }
             
                final String noAccess = "No access";
            
                marketNameField.setText(ergoNetworkUrl != null ? ergoNetworkUrl.getName() : noAccess);
                marketUrlField.setText(ergoNetworkUrl != null ?ergoNetworkUrl.getUrlString() : noAccess);
            }else{
                marketNameField.setText("");
                marketUrlField.setText("");
            }
        };

      

        VBox marketBodyBox = new VBox(marketNameBox,  marketUrlBox);

        marketBodyBox.setId("networkBox");
       
        toggleShowMarkets.setOnMouseClicked(e->{
            m_showMarkets.set(!m_showMarkets.get());
        });
        
        m_showMarkets.addListener((obs, oldval, newval) -> {

            toggleShowMarkets.setText(newval ? "⏷ " : "⏵ ");

            if (newval) {
                if (!marketBodyPaddingBox.getChildren().contains(marketBodyBox)) {
                    marketBodyPaddingBox.getChildren().add(marketBodyBox);
                }
            } else {
                if (marketBodyPaddingBox.getChildren().contains(marketBodyBox)) {
                    marketBodyPaddingBox.getChildren().remove(marketBodyBox);
                }
            }
        });

        m_ergoMarketInterface.addListener((obs,oldval,newval)->{
            setMarketInfo.run();
        });

        setMarketInfo.run();

        m_mainBox = new VBox(marketLayoutBox);
        m_mainBox.setPadding(new Insets(0));
        HBox.setHgrow(m_mainBox, Priority.ALWAYS);

        m_currentBox.addListener((obs, oldval, newval) -> {
            m_mainBox.getChildren().clear();
            if (newval != null) {
                m_mainBox.getChildren().add(newval);
            } else {
                m_mainBox.getChildren().add(marketLayoutBox);
            }

        });


        getChildren().addAll(m_mainBox);
        setPadding(new Insets(0,0,5,0));
    }


    public void setMarket(String marketId){
        if(marketId != null){
            JsonObject getMarketObject = Utils.getCmdObject("getMarketById");
            getMarketObject.addProperty("locationId", m_locationId);
            getMarketObject.addProperty("networkId", App.MARKET_NETWORK);
            getMarketObject.addProperty("id", marketId);

            NoteInterface marketData = (NoteInterface) m_ergoNetwork.sendNote(getMarketObject);
            if (marketData != null) {

                m_ergoMarketInterface.set(marketData);
                m_showMarkets.set(true);
            }else{
                m_ergoMarketInterface.set(null);
            }
        }else{
            m_ergoMarketInterface.set(null);
        }
    }

}
