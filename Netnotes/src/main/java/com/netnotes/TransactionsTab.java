package com.netnotes;

import com.google.gson.JsonObject;
import com.netnotes.ErgoTransaction.TransactionType;
import com.utils.Utils;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class TransactionsTab extends VBox implements TabInterface {
    
    public final static String NAME = "Transactions";

    private SimpleBooleanProperty m_current = new SimpleBooleanProperty(true);

    public boolean getCurrent(){
        return m_current.get();
    } 

    public void setCurrent(boolean value){
        m_current.set(value);
    }

    public String getType(){
        return App.STATIC_TYPE;
    }

    public boolean isStatic(){
        return getType().equals(App.STATIC_TYPE);
    }

    private SimpleStringProperty m_titleProperty = new SimpleStringProperty(NAME);

    public SimpleStringProperty titleProperty(){
        return m_titleProperty;
    }

    private ListChangeListener<ErgoTransaction> m_watchedTxListChanged;

    private final AddressData m_addressData;
    public TransactionsTab(Scene scene, AddressData addressData, SimpleDoubleProperty widthObject, VBox summaryBox){
        super();
        m_addressData = addressData;
        summaryBox.getChildren().clear();

        ImageView watchedIcon = IconButton.getIconView(new Image("/assets/star-30.png"), 30);
        
        Text watchedText = new Text("Watched");
        watchedText.setFont(App.txtFont);
        watchedText.setFill(App.txtColor);

        TextField newTxIdField = new TextField();
        newTxIdField.setPromptText("Add Transaction Id");
        newTxIdField.setPrefWidth(200);
        newTxIdField.setId("numField");
       
        Button addTxId = new Button("Add");
        addTxId.setOnAction(e->{
            String newTxId = newTxIdField.getText();
            String hexString =  newTxId.replaceAll("[^0-9a-fA-F]", "");

            if(newTxId.length() == 64 && hexString.equals(newTxId)){
               
                ErgoTransaction userTx = new ErgoTransaction(newTxIdField.getText(), addressData, TransactionType.USER);
            
                userTx.doUpdate( true);
                
                addressData.addWatchedTransaction(userTx);
                
            }else{
                Alert a = new Alert(AlertType.NONE, "Notice: Invalid transaction Id.", ButtonType.OK);

                a.setHeaderText("Invalid Transaction Id");
                a.setTitle("Invalid Transaction Id");
                a.show();
            }
            newTxIdField.setText("");
        });
        newTxIdField.setOnAction(e->addTxId.fire());
      

        Region watchedSpacerRegion = new Region();
        HBox.setHgrow(watchedSpacerRegion, Priority.ALWAYS);

        HBox watchedHeadingBox = new HBox(watchedIcon, watchedText,watchedSpacerRegion, newTxIdField, addTxId);
        HBox.setHgrow(watchedHeadingBox, Priority.ALWAYS);
        watchedHeadingBox.setId("headingBox");
        watchedHeadingBox.setMinHeight(40);
        watchedHeadingBox.setAlignment(Pos.CENTER_LEFT);
        watchedHeadingBox.setPadding(new Insets(0,15,0,5));

        VBox watchedTxsBox = new VBox();
        HBox.setHgrow(watchedTxsBox, Priority.ALWAYS);

        Text allText = new Text("All");
        allText.setFont(App.txtFont);
        allText.setFill(App.txtColor);

        Region spacerRegion = new Region();
        HBox.setHgrow(spacerRegion, Priority.ALWAYS);

        Text offsetText = new Text("Start at: ");
        offsetText.setFont(App.titleFont);
        offsetText.setFill(App.altColor);
        
        TextField offsetField = new TextField("0");
        offsetField.setPromptText("Offset");
        offsetField.setPrefWidth(60);
        offsetField.setId("numField");
        offsetField.textProperty().addListener((obs,oldval,newval)->{
            String numStr = newval.replaceAll("[^0-9]", "");
            numStr = numStr.equals("") ? "0" : numStr;
            long longNum = Long.parseLong(numStr);
            int num = longNum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) longNum;
            offsetField.setText(num + "");

        });

        Text limitText = new Text("Max: ");
        limitText.setFont(App.titleFont);
        limitText.setFill(App.altColor);

        TextField limitField = new TextField("50");
        limitField.setPrefWidth(60);
        limitField.setPromptText("Limit");
        limitField.setId("numField");
        limitField.textProperty().addListener((obs,oldval,newval)->{
            String numStr = newval.replaceAll("[^0-9]", "");
            numStr = numStr.equals("") ? "0" : numStr;
            int maxInt = Integer.parseInt(numStr);
            maxInt = maxInt > 500 ? 500 : maxInt;
            limitField.setText(maxInt + "");
        });

        Button getTxsBtn = new Button("Get");

        Region fieldSpacer = new Region();
        fieldSpacer.setMinWidth(5);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);

        Text progressText = new Text("Getting transactions...");
        progressText.setFill(App.txtColor);
        progressText.setFont(App.titleFont);

        HBox progressTextBox = new HBox(progressText);
        HBox.setHgrow(progressTextBox, Priority.ALWAYS);
        progressTextBox.setMinHeight(30);
        progressTextBox.setAlignment(Pos.CENTER);


        progressBar.progressProperty().addListener((obs,oldval,newval)->{
            double value = newval.doubleValue();
            if(value > 0){
                progressText.setText("(" + String.format("%.1f", value * 100) + "%)");
            }else{
                progressText.setText("Getting Transactions...");
            }
        });
    
        VBox progressBarBox = new VBox(progressBar, progressTextBox);
        HBox.setHgrow(progressBarBox, Priority.ALWAYS);
        progressBarBox.setAlignment(Pos.CENTER);
        progressBarBox.setMinHeight(150);

        HBox allHeadingBox = new HBox(allText, spacerRegion,offsetText, offsetField,limitText, limitField,fieldSpacer, getTxsBtn);
        HBox.setHgrow(allHeadingBox, Priority.ALWAYS);
        allHeadingBox.setId("headingBox");
        allHeadingBox.setMinHeight(40);
        allHeadingBox.setAlignment(Pos.CENTER_LEFT);
        allHeadingBox.setPadding(new Insets(0,15,0,15));

        VBox allTxsBox = new VBox();
        HBox.setHgrow(allTxsBox, Priority.ALWAYS);

        Runnable updateWatchedTxs = ()->{
            
            ErgoTransaction[] watchedTxs = addressData.getWatchedTxArray();
            for(int i = 0; i < watchedTxs.length ; i++){
                watchedTxs[i].doUpdate( false);
            }
            
        
        };

        //getNetworksData().timeCycleProperty().addListener((obs,oldval,newval)->updateWatchedTxs.run());

        updateWatchedTxs.run();

        Runnable updateTxList = () ->{
            
            ErgoTransaction[] txArray = addressData.getReverseTxArray();
        
            watchedTxsBox.getChildren().clear();
       
            for(int i = 0; i < txArray.length ; i++){
                ErgoTransaction ergTx = txArray[i];

                watchedTxsBox.getChildren().add(ergTx.getTxBox());
                
                
            }
            
            if(watchedTxsBox.getChildren().size() == 0){
                Text noSavedTxs = new Text("No saved transactions");
                noSavedTxs.setFill(App.altColor);
                noSavedTxs.setFont(App.txtFont);

                HBox emptywatchedBox = new HBox(noSavedTxs);
                HBox.setHgrow(emptywatchedBox, Priority.ALWAYS);
                emptywatchedBox.setMinHeight(40);
                emptywatchedBox.setAlignment(Pos.CENTER);

                watchedTxsBox.getChildren().add(emptywatchedBox);
            }

            
        };

        updateTxList.run();     
        /*getNetworksData().timeCycleProperty().addListener((obs,oldval,newVal)->{
            int watchedTxSize = m_watchedTransactions.size();
            if(watchedTxSize > 0){
                ErgoExplorerData explorerData = m_addressesData.selectedExplorerData().get();
                for(int i = 0; i < watchedTxSize ; i++){

                }
            }
        }); */
        m_watchedTxListChanged = (ListChangeListener.Change<? extends ErgoTransaction> c)->updateTxList.run();

        addressData.watchedTxList().addListener(m_watchedTxListChanged);
        SimpleObjectProperty<ErgoTransaction[]> txsProperty = new SimpleObjectProperty<>(new ErgoTransaction[0]);

        Runnable updateAllTxList = ()->{
            ErgoTransaction[] txArray = txsProperty.get();
            
            allTxsBox.getChildren().clear();

            for(int i = 0; i < txArray.length ; i++){
               
                allTxsBox.getChildren().add(txArray[i].getTxBox());
                
            }
            
            if(allTxsBox.getChildren().size() == 0){
                Text noSavedTxs = new Text("No transactions available");
                noSavedTxs.setFill(App.altColor);
                noSavedTxs.setFont(App.txtFont);

                HBox emptywatchedBox = new HBox(noSavedTxs);
                HBox.setHgrow(emptywatchedBox, Priority.ALWAYS);
                emptywatchedBox.setMinHeight(40);
                emptywatchedBox.setAlignment(Pos.CENTER);

                allTxsBox.getChildren().add(emptywatchedBox);
            }
            if(allTxsBox.getChildren().contains(progressBarBox)){
                allTxsBox.getChildren().remove(progressBarBox);
            }
        };

       
       

    
     
        Region allSpacer = new Region();
        allSpacer.setMinHeight(10);

     
       

        getTxsBtn.setOnAction(e->{
            NoteInterface explorerInterface = addressData.getErgoNetworkData().selectedExplorerData().get();
            if(explorerInterface != null){
                int offset = Integer.parseInt(offsetField.getText());
                int limit = Integer.parseInt(limitField.getText());
                allTxsBox.getChildren().clear();
                
                allTxsBox.getChildren().add(progressBarBox);

                JsonObject note = Utils.getCmdObject("getAddressTransactions");
                note.addProperty("address", addressData.getAddressString());
                note.addProperty("offset", offset);
                note.addProperty("limit", limit);

                explorerInterface.sendNote(note ,(onSucceeded)->{
                    Object sourceObject = onSucceeded.getSource().getValue();
                    if(sourceObject != null && sourceObject instanceof JsonObject){
                        JsonObject txsJson = (JsonObject) sourceObject;

                        ErgoTransaction[] txArray = addressData.getTxArray(txsJson);
                      
                        txsProperty.set(txArray);
                        updateAllTxList.run();
                        
                    }else{
                
                        txsProperty.set(new ErgoTransaction[0]);
                        updateAllTxList.run();
                        
                    }
                }, (onFailed)->{
                    
                    txsProperty.set(new ErgoTransaction[0]);
                    updateAllTxList.run();
                  
                });
            }else{
                Alert a = new Alert(AlertType.NONE, "Select an Ergo explorer", ButtonType.OK);
                
                a.setTitle("Required: Ergo Explorer");
                a.setHeaderText("Required: Ergo Explorer");
                a.show();
            }
        });
        
        getTxsBtn.fire();
        prefWidthProperty().bind(widthObject);
        getChildren().addAll(watchedHeadingBox, watchedTxsBox, allSpacer, allHeadingBox, allTxsBox);
        setPadding(new Insets(10));
    }
    
    public String getTabId(){
        return NAME + ":" + m_addressData.getAddress().toString();
    }

    public String getName(){
        return NAME;
    }

    public static String createTabId(AddressData addressData){
        return NAME + ":" + addressData.getAddress().toString();
    }


    public void shutdown(){
        prefWidthProperty().unbind();
        if(m_watchedTxListChanged != null){
            m_addressData.watchedTxList().removeListener(m_watchedTxListChanged);
            m_watchedTxListChanged = null;
        }
    }
}
