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
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
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

public class ErgoExplorersAppBox extends AppBox {
    
    private Stage m_appStage;
    private SimpleObjectProperty<AppBox> m_currentBox = new SimpleObjectProperty<>(null);
    private NoteInterface m_ergoNetworkInterface;
    private VBox m_mainBox;
    private SimpleBooleanProperty m_showExplorers = new SimpleBooleanProperty(false);
    private SimpleObjectProperty<JsonObject> m_defaultExplorer = new SimpleObjectProperty<>(null);
    private String m_locationId = null;

    private ContextMenu m_explorerMenu = new ContextMenu();
    private HBox m_explorerFieldBox;

    public ErgoExplorersAppBox(Stage appStage, String locationId, NoteInterface ergoNetworkInterface){
        super();
        m_ergoNetworkInterface = ergoNetworkInterface;
        m_appStage = appStage;
        m_locationId = locationId;

        final String selectString = "[select]";
    
        ImageView explorerIconView = new ImageView(new Image(ErgoExplorers.getSmallAppIconString()));
        explorerIconView.setPreserveRatio(true);
        explorerIconView.setFitHeight(18);

        HBox topIconBox = new HBox(explorerIconView);
        topIconBox.setAlignment(Pos.CENTER_LEFT);
        topIconBox.setMinWidth(30);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ImageView closeImage = App.highlightedImageView(App.closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        Button toggleShowExplorers = new Button(m_showExplorers.get() ? "⏷" : "⏵");
        toggleShowExplorers.setId("caretBtn");
        toggleShowExplorers.setOnAction(e->{
            m_showExplorers.set(!m_showExplorers.get());
        });

        MenuButton explorerMenuBtn = new MenuButton("⋮");


        Text explorerTopLabel = new Text(String.format("%-13s","Explorer "));
        explorerTopLabel.setFont(App.txtFont);
        explorerTopLabel.setFill(App.txtColor);


        MenuButton openMenuBtn = new MenuButton("⏷");
        openMenuBtn.setId("arrowMenuButton");


        m_explorerFieldBox = new HBox(openMenuBtn);
        HBox.setHgrow(m_explorerFieldBox, Priority.ALWAYS);
        m_explorerFieldBox.setAlignment(Pos.CENTER_LEFT);
        m_explorerFieldBox.setId("bodyBox");
        m_explorerFieldBox.setPadding(new Insets(0, 1, 0, 0));
        m_explorerFieldBox.setMaxHeight(18);

        openMenuBtn.prefWidthProperty().bind(m_explorerFieldBox.widthProperty().subtract(1));

        HBox explorerMenuBtnPadding = new HBox(explorerMenuBtn);
        explorerMenuBtnPadding.setPadding(new Insets(0, 0, 0, 5));



        HBox explorerBtnBox = new HBox(m_explorerFieldBox, explorerMenuBtnPadding);
        explorerBtnBox.setPadding(new Insets(2, 2, 0, 5));
        HBox.setHgrow(explorerBtnBox, Priority.ALWAYS);

        VBox explorerBodyPaddingBox = new VBox();
        HBox.setHgrow(explorerBodyPaddingBox, Priority.ALWAYS);
        explorerBodyPaddingBox.setPadding(new Insets(0,10,0,0));


        Binding<String> explorerNameBinding = Bindings.createObjectBinding(()->{
            JsonObject explorer = m_defaultExplorer.get();
            if(explorer != null){
                JsonElement name = explorer.get("name");
                if(name != null && name.isJsonPrimitive()){
                    return name.getAsString();
                }
            }
            return selectString;
        }, m_defaultExplorer);

        openMenuBtn.textProperty().bind(explorerNameBinding);


       

  

   

        
        Label explorerNameIcon = new Label("  ");
        explorerNameIcon.setId("logoBtn");

        Label explorerNameText = new Label("Name"); 
        explorerNameText.setFont(App.txtFont);
        explorerNameText.setPadding(new Insets(0,5,0,5));
        explorerNameText.setMinWidth(100);

        TextField explorerNameField = new TextField();
        explorerNameField.setEditable(false);
        HBox.setHgrow(explorerNameField,Priority.ALWAYS);

        HBox explorerNameFieldBox = new HBox(explorerNameField);
        HBox.setHgrow(explorerNameFieldBox,Priority.ALWAYS);
        explorerNameFieldBox.setId("bodyBox");
        explorerNameFieldBox.setAlignment(Pos.CENTER_LEFT);
        explorerNameFieldBox.setMaxHeight(18);

        HBox explorerNameBox = new HBox(explorerNameIcon, explorerNameText, explorerNameFieldBox);
        explorerNameBox.setAlignment(Pos.CENTER_LEFT);
        explorerNameBox.setPadding(new Insets(2,0,0,0));

        Label explorerUrlIcon = new Label("  ");
        explorerUrlIcon.setId("logoBtn");
  

        Label explorerUrlText = new Label("Api Url"); 
        explorerUrlText.setFont(App.txtFont);
        explorerUrlText.setPadding(new Insets(0,5,0,5));
        explorerUrlText.setMinWidth(100);

        TextField explorerUrlField = new TextField();
        explorerUrlField.setEditable(false);
        HBox.setHgrow(explorerUrlField,Priority.ALWAYS);

        HBox explorerUrlFieldBox = new HBox(explorerUrlField);
        HBox.setHgrow(explorerUrlFieldBox,Priority.ALWAYS);
        explorerUrlFieldBox.setId("bodyBox");
        explorerUrlFieldBox.setAlignment(Pos.CENTER_LEFT);
        explorerUrlFieldBox.setMaxHeight(18);
     
        HBox explorerUrlBox = new HBox(explorerUrlIcon, explorerUrlText, explorerUrlFieldBox);
        explorerUrlBox.setAlignment(Pos.CENTER_LEFT);
        explorerUrlBox.setPadding(new Insets(2,0,0,0));

        Label explorerWebsiteIcon = new Label("  ");
        explorerWebsiteIcon.setId("logoBtn");

        Label explorerWebsiteText = new Label("Website"); 
        explorerWebsiteText.setFont(App.txtFont);
        explorerWebsiteText.setPadding(new Insets(0,5,0,5));
        explorerWebsiteText.setMinWidth(100);

        TextField explorerWebsiteField = new TextField();
        explorerWebsiteField.setEditable(false);
        HBox.setHgrow(explorerWebsiteField,Priority.ALWAYS);

        HBox explorerWebsiteFieldBox = new HBox(explorerWebsiteField);
        HBox.setHgrow(explorerWebsiteFieldBox,Priority.ALWAYS);
        explorerWebsiteFieldBox.setId("bodyBox");
        explorerWebsiteFieldBox.setAlignment(Pos.CENTER_LEFT);
        explorerWebsiteFieldBox.setMaxHeight(18);

        HBox explorerWebsiteBox = new HBox(explorerWebsiteIcon, explorerWebsiteText, explorerWebsiteFieldBox);
        explorerWebsiteBox.setAlignment(Pos.CENTER_LEFT);
        explorerWebsiteBox.setPadding(new Insets(2,0,0,0));

        HBox explorerLabelBox = new HBox(explorerTopLabel);
        explorerLabelBox.setAlignment(Pos.CENTER_LEFT);


        HBox explorersTopBar = new HBox(toggleShowExplorers, topIconBox, explorerLabelBox, explorerBtnBox);
        explorersTopBar.setAlignment(Pos.CENTER_LEFT);
        explorersTopBar.setPadding(new Insets(2));

        VBox explorerLayoutBox = new VBox(explorersTopBar, explorerBodyPaddingBox);
        HBox.setHgrow(explorerLayoutBox, Priority.ALWAYS);



        Runnable setExplorerInfo = ()->{
            JsonObject json =  m_defaultExplorer.get();

            if(json != null){

                JsonElement networkUrlElement = json.get("ergoNetworkUrl");
                JsonElement webUrlElement = json.get("webUrl");

                ErgoNetworkUrl ergoNetworkUrl = null;
                
                ErgoNetworkUrl webUrl = null; 
                try{
                    ergoNetworkUrl = networkUrlElement != null && networkUrlElement.isJsonObject() ? new ErgoNetworkUrl(networkUrlElement.getAsJsonObject()) : null;
                    webUrl = webUrlElement != null && webUrlElement.isJsonObject() ? new ErgoNetworkUrl(webUrlElement.getAsJsonObject()) : null;
                }catch(Exception e){

                }

                final String noAccess = "No access";
            
                explorerNameField.setText(ergoNetworkUrl != null ? ergoNetworkUrl.getName() : noAccess);
                explorerUrlField.setText(ergoNetworkUrl != null ?ergoNetworkUrl.getUrlString() : noAccess);
                explorerWebsiteField.setText(webUrl != null ? webUrl.getUrlString() : noAccess);     
            }else{
                explorerNameField.setText("");
                explorerUrlField.setText("");
                explorerWebsiteField.setText("");
            }
        };

      

        VBox explorerBodyBox = new VBox(explorerNameBox,  explorerUrlBox, explorerWebsiteBox);

   
        
        m_showExplorers.addListener((obs, oldval, newval) -> {

            toggleShowExplorers.setText(newval ? "⏷" : "⏵");

            if (newval) {
                if (!explorerBodyPaddingBox.getChildren().contains(explorerBodyBox)) {
                    explorerBodyPaddingBox.getChildren().add(explorerBodyBox);
                }
            } else {
                if (explorerBodyPaddingBox.getChildren().contains(explorerBodyBox)) {
                    explorerBodyPaddingBox.getChildren().remove(explorerBodyBox);
                }
            }
        });

        m_defaultExplorer.addListener((obs,oldval,newval)->{
            setExplorerInfo.run();
        });

        setExplorerInfo.run();

        m_mainBox = new VBox(explorerLayoutBox);
        m_mainBox.setPadding(new Insets(0));
        HBox.setHgrow(m_mainBox, Priority.ALWAYS);

        m_currentBox.addListener((obs, oldval, newval) -> {
            m_mainBox.getChildren().clear();
            if (newval != null) {
                m_mainBox.getChildren().add(newval);
            } else {
                m_mainBox.getChildren().add(explorerLayoutBox);
            }

        });

        getDefaultExplorer();


        getChildren().addAll(m_mainBox);
        setPadding(new Insets(0,0,5,0));
    }

    public void setDefaultExplorer(String id){
        JsonObject note = Utils.getCmdObject("setDefault");
        note.addProperty("networkId", App.EXPLORER_NETWORK);
        note.addProperty("locationId", m_locationId);
        note.addProperty("id", id);
        
        m_ergoNetworkInterface.sendNote(note);
    }

    public void getDefaultExplorer(){
        JsonObject getDefaultObject = Utils.getCmdObject("getDefault");
        getDefaultObject.addProperty("networkId", App.EXPLORER_NETWORK);
        getDefaultObject.addProperty("locationId", m_locationId);
        JsonObject obj = (JsonObject) m_ergoNetworkInterface.sendNote(getDefaultObject);
       
        m_defaultExplorer.set(obj);
        if(obj == null){
            m_showExplorers.set(false);
        }
    }

    public void updateExplorerMenu(){
       
        JsonObject note = Utils.getCmdObject("getExplorers");
        note.addProperty("networkId", App.EXPLORER_NETWORK);
        note.addProperty("locationId", m_locationId);

        Object objResult = m_ergoNetworkInterface.sendNote(note);

        m_explorerMenu.getItems().clear();

        if (objResult != null && objResult instanceof JsonArray) {

            JsonArray explorersArray = (JsonArray) objResult;

            for (JsonElement element : explorersArray) {
                
                JsonObject json = element.getAsJsonObject();

                String name = json.get("name").getAsString();
                String id = json.get("id").getAsString();

                MenuItem explorerItem = new MenuItem(String.format("%-50s", " " + name));

                explorerItem.setOnAction(action -> {
                    setDefaultExplorer(id);
                });

                m_explorerMenu.getItems().add(explorerItem);
                
            }
       
       
        }else{
            MenuItem explorerItem = new MenuItem(String.format("%-50s", " Error: Unable to get available explorers."));
            m_explorerMenu.getItems().add(explorerItem);
        }
        Point2D p = m_explorerFieldBox.localToScene(0.0, 0.0);
        m_explorerMenu.setPrefWidth(m_explorerFieldBox.getLayoutBounds().getWidth());

        m_explorerMenu.show(m_explorerFieldBox,
                5 + p.getX() + m_explorerFieldBox.getScene().getX() + m_explorerFieldBox.getScene().getWindow().getX(),
                (p.getY() + m_explorerFieldBox.getScene().getY() + m_explorerFieldBox.getScene().getWindow().getY())
                        + m_explorerFieldBox.getLayoutBounds().getHeight() - 1);
    }

    @Override
    public void sendMessage(int code, long timestamp,String networkId, String msg){
        
        if(networkId != null && networkId.equals(App.EXPLORER_NETWORK)){

            switch(code){
                
                case App.LIST_DEFAULT_CHANGED:
                    getDefaultExplorer();
                break;
              
            }

            AppBox appBox  = m_currentBox.get();
            if(appBox != null){
       
                appBox.sendMessage(code, timestamp,networkId, msg);
                
            }

        }
    
    }


}
