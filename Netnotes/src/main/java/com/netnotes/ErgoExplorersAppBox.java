package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class ErgoExplorersAppBox extends AppBox {
    
    private Stage m_appStage;
    private SimpleObjectProperty<AppBox> m_currentBox = new SimpleObjectProperty<>(null);
    private NoteInterface m_ergoNetworkInterface;
    private VBox m_mainBox;
    private SimpleBooleanProperty m_showExplorers = new SimpleBooleanProperty(false);
    private SimpleObjectProperty<NoteInterface> m_defaultExplorer = new SimpleObjectProperty<>(null);
    private String m_locationId = null;

    private ContextMenu m_explorerMenu = new ContextMenu();
    private HBox m_explorerFieldBox;
    private Button m_toggleExplorersBtn;
    private VBox m_explorerBodyPaddingBox = null;
    private JsonParametersBox m_explorerParameterBox = null;

    public static final String SEARCH_TxId = "Tx (TxId)";
    public static final String SEARCH_AdrTxs = "Txs (Address)";
    public static final String SEARCH_TokenIdInfo = "Token (TokenId)";
    public static final String SEARCH_UnspentByTokenId = "Unspent (TokenId)";
    public static final String SEARCH_UnspentByErgoTree = "Unspent (ErgoTree)";
    public static final String SEARCH_UnspentByTemplateHash = "Unspent (Hash)";
    
    private Text m_searchText = null;
    private TextField m_searchTextField = null;
    private Binding<String> m_searchTextFieldIdBinding = null;
    private ChangeListener<String> m_searchFieldEnterBtnAddListener = null;
    private EventHandler<ActionEvent> m_searchBtnEnterAction;
    private EventHandler<ActionEvent> m_searchFieldEnterAction;
    private EventHandler<ActionEvent> m_clearBtnAction;
    private Button m_searchEnterBtn = null;
    private Button m_searchClearBtn = null;
    private MenuButton m_searchTypeMenuButton = null;
    private ChangeListener<String> m_searchTypeTextListener = null;
    private MenuItem m_txIdMenuItem = null;
    private MenuItem m_tokenIdInfoMenuItem = null;
    private MenuItem m_unspentByTokenIdMenuItem = null;
    private MenuItem m_unspentByErgoTreeMenuItem = null;
    private MenuItem m_unspentByHashMenuItem = null;
    private HBox m_searchFieldBox = null;
    private HBox m_searchHBox = null;
    private VBox m_searchVBox = null;
    private JsonParametersBox m_searchResultBox = null;
    private Button m_exportBtn = null;
    private ExtensionFilter m_exportSaveFilter = null;
    private HBox m_exportBtnBox = null;
    private Gson m_gson = null;

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

        m_toggleExplorersBtn = new Button(m_showExplorers.get() ? "⏷" : "⏵");
        m_toggleExplorersBtn.setId("caretBtn");
        m_toggleExplorersBtn.setOnAction(e->{
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

        m_explorerBodyPaddingBox = new VBox();
        HBox.setHgrow(m_explorerBodyPaddingBox, Priority.ALWAYS);
        m_explorerBodyPaddingBox.setPadding(new Insets(0,10,0,5));


        Binding<String> explorerNameBinding = Bindings.createObjectBinding(()->{
            NoteInterface defaultExplorer = m_defaultExplorer.get();
            return defaultExplorer != null ? defaultExplorer.getName() : selectString;
        }, m_defaultExplorer);

        openMenuBtn.textProperty().bind(explorerNameBinding);

        HBox explorerLabelBox = new HBox(explorerTopLabel);
        explorerLabelBox.setAlignment(Pos.CENTER_LEFT);


        HBox explorersTopBar = new HBox(m_toggleExplorersBtn, topIconBox, explorerLabelBox, explorerBtnBox);
        explorersTopBar.setAlignment(Pos.CENTER_LEFT);
        explorersTopBar.setPadding(new Insets(2));

        VBox explorerLayoutBox = new VBox(explorersTopBar, m_explorerBodyPaddingBox);
        HBox.setHgrow(explorerLayoutBox, Priority.ALWAYS);



      
    
        
        m_showExplorers.addListener((obs, oldval, newval) -> updateShowExplorers());

        m_defaultExplorer.addListener((obs,oldval,newval)->setExplorerInfo());

        setExplorerInfo();
        updateShowExplorers();

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

    public void updateShowExplorers(){

        boolean isShow = m_showExplorers.get();
        m_toggleExplorersBtn.setText(isShow ? "⏷" : "⏵");

        if (isShow) {
            addExplorerBoxes();
        } else {
            removeExplorerBoxes();
        }
    
    }
    

    public void addSearchBox(){
        if(m_searchText == null){
            

            m_searchText = new Text("Search ");
            m_searchText.setFill(App.txtColor);
            m_searchText.setFont(App.txtFont);
            
            m_searchTextField = new TextField();
            HBox.setHgrow(m_searchTextField, Priority.ALWAYS);
            m_searchTextField.setPromptText("");
            m_searchTextFieldIdBinding = Utils.createFormFieldIdBinding(m_searchTextField);
            m_searchTextField.idProperty().bind(m_searchTextFieldIdBinding);
             
            m_searchTypeMenuButton = new MenuButton(SEARCH_TxId);
            resizeMenuBtn();
            m_searchTypeTextListener = (obs, oldval, newval)->updateSearchType();

            m_searchTypeMenuButton.textProperty().addListener(m_searchTypeTextListener);

            m_txIdMenuItem = new MenuItem("Transaction by Tx Id");
            m_txIdMenuItem.setOnAction(e->{
                m_searchTypeMenuButton.setText(SEARCH_TxId);
            });

            m_tokenIdInfoMenuItem = new MenuItem("Token info by Token Id");
            m_tokenIdInfoMenuItem.setOnAction(e->{
                m_searchTypeMenuButton.setText(SEARCH_TokenIdInfo);
                resizeMenuBtn();
            });

            m_unspentByTokenIdMenuItem = new MenuItem("Unspent boxes by Token Id");
            m_unspentByTokenIdMenuItem.setOnAction(e->{
                m_searchTypeMenuButton.setText(SEARCH_UnspentByTokenId);
            });

            m_unspentByErgoTreeMenuItem = new MenuItem("Unspent boxes by Ergo Tree");
            m_unspentByErgoTreeMenuItem.setOnAction(e->{
                m_searchTypeMenuButton.setText(SEARCH_UnspentByErgoTree);
            });

            m_unspentByHashMenuItem = new MenuItem("Unspent boxes by Template Hash");
            m_unspentByHashMenuItem.setOnAction(e->{
                m_searchTypeMenuButton.setText(SEARCH_UnspentByTemplateHash);
            });

            m_searchTypeMenuButton.getItems().addAll(m_txIdMenuItem, m_tokenIdInfoMenuItem, m_unspentByTokenIdMenuItem, m_unspentByErgoTreeMenuItem, m_unspentByHashMenuItem);

            m_searchFieldBox = new HBox(m_searchTextField);
            m_searchFieldBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(m_searchFieldBox, Priority.ALWAYS);
            m_searchFieldBox.setId("bodyBox");

            m_searchEnterBtn = new Button("↵");
            m_searchEnterBtn.setId("toolBtn");
            m_searchBtnEnterAction = (e)->{
                if(m_searchTextField != null && m_searchTypeMenuButton != null){
                    String searchText = m_searchTextField.getText();
                    switch(m_searchTypeMenuButton.getText()){
                        case SEARCH_TxId:
                            searchForTxId(searchText);
                        break;
                    
                        case SEARCH_TokenIdInfo:
                            searchForTokenInfo(searchText);
                        break;
                        case SEARCH_UnspentByTokenId:
                            searchForUnspentByTokenId(searchText);
                        break;
                        case SEARCH_UnspentByErgoTree:
                            searchForUnspentByErgoTree(searchText);
                        break;
                        case SEARCH_UnspentByTemplateHash:
                            searchForUnspentByTemplateHash(searchText);
                        break;

                    }
                }
            };
            m_searchEnterBtn.setOnAction(m_searchBtnEnterAction);

            m_searchFieldEnterAction = (e)->m_searchEnterBtn.fire();
            m_searchTextField.setOnAction(m_searchFieldEnterAction);

         
           

            m_searchFieldEnterBtnAddListener = Utils.createFieldEnterBtnAddListener(m_searchTextField, m_searchFieldBox, m_searchEnterBtn);
            m_searchTextField.textProperty().addListener(m_searchFieldEnterBtnAddListener);

            m_searchHBox = new HBox( m_searchText, m_searchFieldBox, m_searchTypeMenuButton);
            HBox.setHgrow(m_searchHBox, Priority.ALWAYS);
            m_searchHBox.setAlignment(Pos.CENTER_LEFT);
            m_searchHBox.setPadding(new Insets(10,0,10,0));

            m_searchVBox = new VBox(m_searchHBox);


            m_explorerBodyPaddingBox.getChildren().addAll(m_searchVBox);

            updateSearchType();
        }
    }

    public void resizeMenuBtn(){
        double w = Utils.computeTextWidth(App.txtFont, m_searchTypeMenuButton.getText());
        m_searchTypeMenuButton.setPrefWidth(w);
    }

    public void updateSearchType(){
        
            if(m_searchTextField != null && m_searchTypeMenuButton != null){
                switch(m_searchTypeMenuButton.getText()){
                    case SEARCH_TxId:
                        m_searchTextField.setPromptText("TxId");
                    break;
                    case SEARCH_TokenIdInfo:
                        m_searchTextField.setPromptText("TokenId");
                    break;
                    case SEARCH_UnspentByTokenId:
                        m_searchTextField.setPromptText("TokenId");
                    break;
                    case SEARCH_UnspentByErgoTree:
                        m_searchTextField.setPromptText("ErgoTree Hex");
                    break;
                    case SEARCH_UnspentByTemplateHash:
                        m_searchTextField.setPromptText("Template Hash");
                    break;
                }
            }
        
    }

    public void removeSearchBox(){

        if(m_searchTextField != null){

            m_searchTextField.idProperty().unbind();
            m_searchEnterBtn.setOnAction(null);
            m_searchTextField.setOnAction(null);
            m_searchTextField.textProperty().removeListener(m_searchFieldEnterBtnAddListener);
            m_searchTypeMenuButton.textProperty().removeListener(m_searchTypeTextListener);          
            m_searchVBox.getChildren().clear();
            m_searchHBox.getChildren().clear();
            m_searchFieldBox.getChildren().clear();
            
            m_searchTextFieldIdBinding = null;
            m_searchFieldEnterBtnAddListener = null;
            m_searchBtnEnterAction = null;
            m_searchFieldEnterAction = null;
            m_clearBtnAction = null;
            m_searchTypeTextListener = null;
            m_searchText = null;
            m_searchTextField = null;
            m_searchEnterBtn = null;
            m_searchClearBtn = null;
            m_searchTypeMenuButton = null;
            m_txIdMenuItem = null;
            m_tokenIdInfoMenuItem = null;
            m_unspentByTokenIdMenuItem = null;
            m_unspentByErgoTreeMenuItem = null;
            m_unspentByHashMenuItem = null;
            m_searchFieldBox = null;
            m_searchHBox = null;
            m_searchVBox = null;
        }
        
    }

    public void searchForTxId(String id){
       
        NoteInterface explorerInterface = m_defaultExplorer.get();
        if(explorerInterface != null){
            JsonObject note = Utils.getCmdObject("getTransaction");
            note.addProperty("txId", id);
            updateSearchResults(null, Utils.getJsonObject("status", "Searching..."));
            explorerInterface.sendNote(note, (onSucceeded)->{
                Object sourceObject = onSucceeded.getSource().getValue();
                if(sourceObject != null){
                    updateSearchResults("tx_" +id, (JsonObject) sourceObject);
                }else{
                    updateSearchResults(null, Utils.getJsonObject("error", "Received invalid result"));
                }
            }, (onFailed)->{
                Throwable throwable = onFailed.getSource().getException();
                updateSearchResults(null, Utils.getJsonObject("error", throwable != null ? throwable.getMessage() : "Unknwon error"));
            });
        }else{
            updateSearchResults(null,Utils.getJsonObject("error", "Explorer disabled"));
        }
        
    }
    public void searchForTokenInfo(String id){
    
        NoteInterface explorerInterface = m_defaultExplorer.get();
        if(explorerInterface != null){
            JsonObject note = Utils.getCmdObject("getTokenInfo");
            note.addProperty("tokenId", id);
            updateSearchResults(null, Utils.getJsonObject("status", "Searching..."));
            explorerInterface.sendNote(note, (onSucceeded)->{
                Object sourceObject = onSucceeded.getSource().getValue();
                if(sourceObject != null){
                    updateSearchResults("tokenInfo_" +id, (JsonObject) sourceObject);
                }else{
                    updateSearchResults(null, Utils.getJsonObject("error", "Received invalid result"));
                }
            }, (onFailed)->{
                Throwable throwable = onFailed.getSource().getException();
                updateSearchResults(null, Utils.getJsonObject("error", throwable != null ? throwable.getMessage() : "Unknwon error"));
            });
        }else{
            updateSearchResults(null,Utils.getJsonObject("error", "Explorer disabled"));
        }
    
    }
                     
    public void searchForUnspentByTokenId(String id){
    
        NoteInterface explorerInterface = m_defaultExplorer.get();
        if(explorerInterface != null){
            JsonObject note = Utils.getCmdObject("getUnspentByTokenId");
            note.addProperty("tokenId", id);
            updateSearchResults(null, Utils.getJsonObject("status", "Searching..."));
            explorerInterface.sendNote(note, (onSucceeded)->{
                Object sourceObject = onSucceeded.getSource().getValue();
                if(sourceObject != null){
                    updateSearchResults("unspent_" + id, (JsonObject) sourceObject);
                }else{
                    updateSearchResults(null, Utils.getJsonObject("error", "Received invalid result"));
                }
            }, (onFailed)->{
                Throwable throwable = onFailed.getSource().getException();
                updateSearchResults(null, Utils.getJsonObject("error", throwable != null ? throwable.getMessage() : "Unknwon error"));
            });
        }else{
            updateSearchResults(null,Utils.getJsonObject("error", "Explorer disabled"));
        }
        
    }
                  
    public void searchForUnspentByErgoTree(String ergoTree){
    
        NoteInterface explorerInterface = m_defaultExplorer.get();
        if(explorerInterface != null){
            JsonObject note = Utils.getCmdObject("getUnspentByErgoTree");
            note.addProperty("ergoTree", ergoTree);
            updateSearchResults(null, Utils.getJsonObject("status", "Searching..."));
            explorerInterface.sendNote(note, (onSucceeded)->{
                Object sourceObject = onSucceeded.getSource().getValue();
                if(sourceObject != null){
                    updateSearchResults("unspentBoxes", (JsonObject) sourceObject);
                }else{
                    updateSearchResults(null, Utils.getJsonObject("error", "Received invalid result"));
                }
            }, (onFailed)->{
                Throwable throwable = onFailed.getSource().getException();
                updateSearchResults(null, Utils.getJsonObject("error", throwable != null ? throwable.getMessage() : "Unknwon error"));
            });
        }else{
            updateSearchResults(null,Utils.getJsonObject("error", "Explorer disabled"));
        }
    
    }
                       
                       
    public void searchForUnspentByTemplateHash(String hash){
   
        NoteInterface explorerInterface = m_defaultExplorer.get();
        if(explorerInterface != null){
            JsonObject note = Utils.getCmdObject("getUnspentByErgoTreeTemplateHash");
            note.addProperty("hash", hash);
            updateSearchResults(null, Utils.getJsonObject("status", "Searching..."));

            explorerInterface.sendNote(note, (onSucceeded)->{
                Object sourceObject = onSucceeded.getSource().getValue();
                if(sourceObject != null){
                    updateSearchResults("unspentBoxes", (JsonObject) sourceObject);
                }else{
                    updateSearchResults(null, Utils.getJsonObject("error", "Received invalid result"));
                }
            }, (onFailed)->{
                Throwable throwable = onFailed.getSource().getException();
                updateSearchResults(null, Utils.getJsonObject("error", throwable != null ? throwable.getMessage() : "Unknwon error"));
            });
        }else{
            updateSearchResults(null, Utils.getJsonObject("error", "Explorer disabled"));
        }
        
    }

    private JsonObject m_resultsJson = null;
    private String m_resultsName = null;

    public void updateSearchResults(String name, JsonObject resultsObject){
        m_resultsJson = resultsObject;
        m_resultsName = name;
        if(resultsObject != null){
            if(m_searchClearBtn == null){
                m_searchClearBtn = new Button("☓");
                m_clearBtnAction = (e)->{
                    updateSearchResults(null, null);
                };
                m_searchClearBtn.setOnAction(m_clearBtnAction);
                m_searchHBox.getChildren().add(m_searchClearBtn);                
            }
            if(m_searchResultBox == null){
                m_searchResultBox = new JsonParametersBox(resultsObject, ErgoNetwork.COL_WIDTH);
                m_searchResultBox.setPadding(new Insets(0,0,0,10));
                m_searchVBox.getChildren().add(m_searchResultBox);
            }else{
                m_searchResultBox.updateParameters(resultsObject);
            }
            if(name != null){
                if(m_exportBtn == null){
                    m_exportBtn = new Button("🖫 Export… (*.json)");
                    m_exportSaveFilter = new FileChooser.ExtensionFilter("JSON (application/json)", "*.json");
                    m_gson = new GsonBuilder().setPrettyPrinting().create();
                    m_exportBtn.setOnAction(onSave->{
                        if(m_resultsName != null && m_resultsJson != null && m_gson != null){
                            JsonObject results = m_resultsJson;
                            Gson gson = m_gson;

                            FileChooser saveChooser = new FileChooser();
                            saveChooser.setTitle("🖫 Export JSON");
                            saveChooser.getExtensionFilters().addAll(m_exportSaveFilter);
                            saveChooser.setSelectedExtensionFilter(m_exportSaveFilter);
                            saveChooser.setInitialFileName(m_resultsName + ".json");
                            File saveFile = saveChooser.showSaveDialog(m_appStage);
                            if(saveFile != null){
                                
                                
                                try {
                                    Files.writeString(saveFile.toPath(), gson.toJson(results));
                                } catch (IOException e1) {
                                    Alert alert = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                                    alert.setTitle("Error");
                                    alert.setHeaderText("Error");
                                    alert.initOwner(m_appStage);
                                    alert.show();
                                }
                            }
                        }
                    });

                    m_exportBtnBox = new HBox(m_exportBtn);
                    m_exportBtnBox.setAlignment(Pos.CENTER_RIGHT);
                    m_exportBtnBox.setPadding(new Insets(15,0,15,0));
                    m_searchVBox.getChildren().add(m_exportBtnBox);
                }
            }else{
                if(m_searchVBox.getChildren().contains(m_exportBtnBox)){
                    m_searchVBox.getChildren().remove(m_exportBtnBox);
                    m_exportBtnBox.getChildren().clear();
                    m_exportBtnBox = null;
                    m_exportBtn.setOnAction(null);
                    m_exportBtn = null;
                    m_gson = null;
                }
            }
        }else{
            if(m_searchClearBtn != null){
                m_searchClearBtn.setOnAction(null);
                m_clearBtnAction = null;
                m_searchHBox.getChildren().remove(m_searchClearBtn);
                m_searchClearBtn = null;
            }
            if(m_searchResultBox != null){
                m_searchVBox.getChildren().remove(m_searchResultBox);
                m_searchResultBox.shutdown();
                m_searchResultBox = null;
               
            }
            if(m_searchVBox.getChildren().contains(m_exportBtnBox)){
                m_searchVBox.getChildren().remove(m_exportBtnBox);
                m_exportBtnBox.getChildren().clear();
                m_exportBtnBox = null;
                m_exportBtn.setOnAction(null);
                m_exportBtn = null;
                m_gson = null;
            }
        }
        
    }


    public void addExplorerBoxes(){

        
        
        if(m_explorerParameterBox == null){
            m_explorerParameterBox = new JsonParametersBox(getExplorerInfo(), ErgoNetwork.COL_WIDTH);
            m_explorerBodyPaddingBox.getChildren().add(m_explorerParameterBox);
            
        }
        addSearchBox();
    }

    public void removeExplorerBoxes(){
        removeSearchBox();
        if(m_explorerParameterBox != null){
            if(m_explorerBodyPaddingBox.getChildren().contains(m_explorerParameterBox)) {
                m_explorerBodyPaddingBox.getChildren().remove(m_explorerParameterBox);
            }
            m_explorerParameterBox.shutdown();
            m_explorerParameterBox = null;
        }
    }

    public JsonObject getDefaultJsonObject(){
        NoteInterface noteInterface = m_defaultExplorer.get();
        return noteInterface != null ? noteInterface.getJsonObject() : null;
    }

    public void setExplorerInfo(){
        if(m_explorerParameterBox != null){        
            m_explorerParameterBox.updateParameters(getExplorerInfo());   
        }
    }

    public JsonObject getExplorerInfo(){
        JsonObject defaultJson = getDefaultJsonObject();
        if(defaultJson != null){
            JsonObject infoJson = new JsonObject();
            infoJson.add("info", getDefaultJsonObject());
            return infoJson;
        }else{
            return Utils.getJsonObject("info", "(disabled)");
        }
    }

    public void setDefaultExplorer(String id){
        JsonObject note = Utils.getCmdObject("setDefault");
        note.addProperty("networkId", ErgoNetwork.EXPLORER_NETWORK);
        note.addProperty("locationId", m_locationId);
        note.addProperty("id", id);
        
        m_ergoNetworkInterface.sendNote(note);
    }

    public void getDefaultExplorer(){
        JsonObject getDefaultObject = Utils.getCmdObject("getDefault");
        getDefaultObject.addProperty("networkId", ErgoNetwork.EXPLORER_NETWORK);
        getDefaultObject.addProperty("locationId", m_locationId);
        Object obj = m_ergoNetworkInterface.sendNote(getDefaultObject);
       
        if(obj != null && obj instanceof NoteInterface){
            m_defaultExplorer.set((NoteInterface) obj);
        }else{
            m_showExplorers.set(false);
        }
       
        
    }

    public void updateExplorerMenu(){
       
        JsonObject note = Utils.getCmdObject("getExplorers");
        note.addProperty("networkId", ErgoNetwork.EXPLORER_NETWORK);
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
        
        if(networkId != null && networkId.equals(ErgoNetwork.EXPLORER_NETWORK)){

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
