package com.netnotes;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
//import java.io.File;
import java.time.LocalDateTime;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.utils.Utils;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ErgoExplorerData {
     public final static String TESTNET_STRING = NetworkType.TESTNET.toString();
     public final static String MAINNET_STRING = NetworkType.MAINNET.toString();
     
    
     
     public final SimpleObjectProperty<ErgoNetworkUrl> m_websiteUrlProperty = new SimpleObjectProperty<>();
     public final SimpleObjectProperty< ErgoNetworkUrl> m_ergoNetworkUrlProperty = new SimpleObjectProperty<>();
     private ErgoExplorerList m_explorerList = null;

    /* private String m_radioOffUrl = "/assets/radio-button-off-30.png";
     private String m_radioOnUrl = "/assets/radio-button-on-30.png";

    private String m_startImgUrl = "/assets/refresh-white-30.png";
    private String m_stopImgUrl = "/assets/stop-30.png";

     private Color m_secondaryColor = new Color(.4, .4, .4, .9);
     private Color m_primaryColor = new Color(.7, .7, .7, .9); 

     private Font m_largeFont = Font.font("OCR A Extended", FontWeight.BOLD, 25);
     private Font m_font = Font.font("OCR A Extended", FontWeight.BOLD, 13);
     private Font m_smallFont = Font.font("OCR A Extended", FontWeight.NORMAL, 10);*/
     

     public final SimpleStringProperty statusProperty = new SimpleStringProperty(App.STATUS_STOPPED);
     public final SimpleObjectProperty<LocalDateTime> lastUpdated = new SimpleObjectProperty<LocalDateTime>(null);
     public final SimpleObjectProperty<LocalDateTime> shutdownNow = new SimpleObjectProperty<>(LocalDateTime.now());
     public final SimpleStringProperty displayTextProperty = new SimpleStringProperty("");

     
     private ChangeListener<LocalDateTime> m_updateListener = null;
     private String m_id;
     private String m_imgUrl;

     public ErgoExplorerData(String id,String name, String imgUrl, String apiUrl,String websiteName, String webstiteUrl, ErgoExplorerList explorerList){
          m_id = id; 
          m_ergoNetworkUrlProperty.set(new ErgoNetworkUrl(id,name, "https", apiUrl, 443, NetworkType.MAINNET ));
          m_websiteUrlProperty.set( new ErgoNetworkUrl( id, websiteName, "https", webstiteUrl, 443, NetworkType.MAINNET));
          m_explorerList = explorerList;
          m_imgUrl = imgUrl;
     }

     public ErgoExplorerData(String id, JsonObject json, ErgoExplorerList explorerList) throws Exception{
          m_id = id;
          m_explorerList = explorerList;
          openJson(json);
     }

     public void openJson(JsonObject json) throws Exception{
          JsonElement ergoNetworkUrl = json.get("ergoNetworkUrl");
          JsonElement webUrlElement = json.get("webUrl");
          JsonElement imgUrlElement = json.get("imgUrl");

          if(ergoNetworkUrl != null && ergoNetworkUrl.isJsonObject()){
               m_ergoNetworkUrlProperty.set( new ErgoNetworkUrl(ergoNetworkUrl.getAsJsonObject()) );          
          }else{
               throw new Exception("Insuffient data");
          }
          
          m_imgUrl = imgUrlElement != null && imgUrlElement.isJsonPrimitive() ? imgUrlElement.getAsString() : ErgoExplorers.getSmallAppIconString();
          
          if(webUrlElement != null && webUrlElement.isJsonObject()){
               m_websiteUrlProperty.set(new ErgoNetworkUrl(webUrlElement.getAsJsonObject()));
          }else{
               String id = m_ergoNetworkUrlProperty.get().getId();
               m_websiteUrlProperty.set(new ErgoNetworkUrl(id, "Explorer Website", "https", "explorer.ergoplatform.com", 443, NetworkType.MAINNET));
          }
     }

     public String getImgUrl(){
          return m_imgUrl;
     }

     public NetworksData getNetworksData(){
          return m_explorerList.getErgoExplorer().getNetworksData();
     }
  
     public void getLatestBlocks(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        ErgoNetworkUrl namedUrl =  m_ergoNetworkUrlProperty.get();

        String urlString = namedUrl.getUrlString() + "/api/v1/blocks/";
        Utils.getUrlJson(urlString, getNetworksData().getExecService(), onSucceeded, onFailed, null);
    }

    
     public void getTokenInfo(JsonObject json, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
          JsonElement tokenIdElement = json != null ? json.get("tokenId") : null;
          String tokenId = tokenIdElement != null && tokenIdElement.isJsonPrimitive() ? tokenIdElement.getAsString() : null;
          if(tokenId != null){
               
               getTokenInfo(tokenId, onSucceeded, onFailed);
          }
     }

     public void getTokenInfo(String tokenId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
          ErgoNetworkUrl namedUrl =  m_ergoNetworkUrlProperty.get();

          String urlString = namedUrl.getUrlString() + "/api/v1/tokens/" + tokenId;

          

          Utils.getUrlJson(urlString, getNetworksData().getExecService(), onSucceeded, onFailed, null);
     }

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
          JsonElement subjectElement = note.get("subject");
          JsonElement networkIdElement = note.get("networkId");
     
          if (subjectElement != null && subjectElement.isJsonPrimitive() && networkIdElement != null && networkIdElement.isJsonPrimitive()) {
               String networkId = networkIdElement.getAsString();
          
               if(networkId.equals(m_explorerList.getErgoNetworkData().getId())){
                    String subject = subjectElement.getAsString();

                    switch(subject){
                         case "getTransaction":
                              getTransaction(note, onSucceeded, onFailed);
                              return true;
                         case "getBalance":
                              getBalance(note, onSucceeded, onFailed);
                              return true;
                         case "getTokenInfo":
                              getTokenInfo(note, onSucceeded, onFailed);
                         default:
                              Object obj = sendNote(note);
                              Utils.returnObject(obj,m_explorerList.getErgoExplorer().getNetworksData().getExecService(), onSucceeded, onFailed);
                              return obj != null;  
                    }
                    
               
               }
          }

          return false;
                
    }

     /*public Object sendNote(JsonObject note){
          JsonElement subjectElement = note.get("subject");
          JsonElement networkIdElement = note.get("networkId");
        
          if (subjectElement != null && subjectElement.isJsonPrimitive() && networkIdElement != null && networkIdElement.isJsonPrimitive()) {
               String networkId = networkIdElement.getAsString();
          
               if(networkId.equals(m_explorerList.getErgoNetworkData().getId())){
                    String subject = subjectElement.getAsString();

                    switch(subject){
                         case "getAddresses":
                         
                         
                         case "getBalance":
                         
                    }
                    
               
               }
          }
          return null;
     }*/

     public Object sendNote(JsonObject note){
          JsonElement subjectElement = note.get("subject");
          JsonElement networkIdElement = note.get("networkId");
        
          if (subjectElement != null && subjectElement.isJsonPrimitive() && networkIdElement != null && networkIdElement.isJsonPrimitive()) {
               String networkId = networkIdElement.getAsString();
          
               if(networkId.equals(m_explorerList.getErgoNetworkData().getId())){
                    String subject = subjectElement.getAsString();

                    switch(subject){
                         default:    
                    }
                    
               
               }
          }
          return null;
     }



    public NoteInterface getNoteInterface(){
     
          ErgoExplorerData thisExplorer = this;
          return new NoteInterface() {
               
               public String getName(){
                    return thisExplorer.getName();
               }

               public String getNetworkId(){
                    return thisExplorer.getId();
               }

               public Image getAppIcon(){
                    return thisExplorer.getImgUrl() != null ? new Image(thisExplorer.getImgUrl()) : null;
               }

               public Image getSmallAppIcon(){
                    return thisExplorer.getImgUrl() != null ? new Image(thisExplorer.getImgUrl()) : null;
               }

               public SimpleObjectProperty<LocalDateTime> getLastUpdated(){
                    return null;
               }

               public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator){
                   return thisExplorer.sendNote(note, onSucceeded, onFailed);
               }

               public Object sendNote(JsonObject note){
                    return thisExplorer.sendNote(note);
               }

               public JsonObject getJsonObject(){
                    return thisExplorer.getJsonObject();
               }

               

               public TabInterface getTab(Stage appStage, SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject,  Button networkBtn){
                    return null;
               }
               public String getType(){
                    return null;
               }


               public NetworksData getNetworksData(){
                    return null;
               }

               public NoteInterface getParentInterface(){
                    return null;
               }

               public void addUpdateListener(ChangeListener<LocalDateTime> changeListener){
               
               }

               public void removeUpdateListener(){
                  
               }

               public void remove(){
           
               }

               public void shutdown(){}

               public SimpleObjectProperty<LocalDateTime> shutdownNowProperty(){
                    return null;
               }


               public void addMsgListener(NoteMsgInterface listener){
                   
               }

               public boolean removeMsgListener(NoteMsgInterface listener){
                    return false;
               }

               public int getConnectionStatus(){
                    return App.STARTED;    
               }

               public void setConnectionStatus(int status){

               }


               public String getDescription(){
                    return m_explorerList.getErgoExplorer().getDescription();
               }
          };
    }
     private void getBalance(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
          JsonElement addressElement = note.get("address");

          if(addressElement != null && addressElement.isJsonPrimitive()){
               getBalance(addressElement.getAsString(), onSucceeded, onFailed);
          }
     }

     protected void getBalance(String address, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

          ErgoNetworkUrl namedUrl =  m_ergoNetworkUrlProperty.get();

          String urlString = namedUrl.getUrlString() + "/api/v1/addresses/" + address + "/balance/total";
   
          Utils.getUrlJson(urlString,getNetworksData().getExecService(), onSucceeded, onFailed, null);
     }

     private void getTransaction(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
          JsonElement txIdElement = note.get("txId");

          if(txIdElement != null && txIdElement.isJsonPrimitive()){
               getTransaction(txIdElement.getAsString(), onSucceeded, onFailed);
          }
     }

     protected void getTransaction(String txId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

          ErgoNetworkUrl namedUrl =  m_ergoNetworkUrlProperty.get();

          String urlString = namedUrl.getUrlString() + "/api/v1/transactions/" + txId;
   
          Utils.getUrlJson(urlString,getNetworksData().getExecService(), onSucceeded, onFailed, null);
     }

     private void getAddressTransactions(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator){
          JsonElement addressElement = note.get("address");
          JsonElement startIndexElement = note.get("startIndex");
          JsonElement limitElement = note.get("limit");

          int startIndex = startIndexElement != null && startIndexElement.isJsonPrimitive() ? startIndexElement.getAsInt() : 0;
          int limit = limitElement != null && limitElement.isJsonPrimitive() ? limitElement.getAsInt() : 0;

          if(addressElement != null && addressElement.isJsonPrimitive()){
               getAddressTransactions(addressElement.getAsString(), startIndex, limit, onSucceeded, onFailed, null);
          }
     }

      public void getAddressTransactions(String address,int startIndex, int limit,  EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator) {
         
          ErgoNetworkUrl namedUrl =  m_ergoNetworkUrlProperty.get();

          String urlString = namedUrl.getUrlString() + "/api/v1/addresses/" + address + "/transactions?offset=" + (startIndex < 0 ? 0 : startIndex ) + "&limit=" + (limit < 1 ? 500 : (limit > 500 ? 500 : limit));          
   
          Utils.getUrlJson(urlString,getNetworksData().getExecService(), onSucceeded, onFailed, progressIndicator);
     }

     public String getWebsiteTxLink(String txId) {

          ErgoNetworkUrl namedUrl = m_websiteUrlProperty.get();

          String urlString = namedUrl.getUrlString() + "/en/transactions/" + txId;
   
          return urlString;
     }

     public SimpleObjectProperty<ErgoNetworkUrl> websiteUrlProperty(){
          return m_websiteUrlProperty;
     }


     public SimpleObjectProperty<ErgoNetworkUrl> ergoNetworkUrlProperty(){
          return m_ergoNetworkUrlProperty;
     }

     public String getId(){
          return m_id;
     }

     public String getName() {
          return m_ergoNetworkUrlProperty.get() == null ? "INVALID" : m_ergoNetworkUrlProperty.get().getName();
     }

     public NetworkType getNetworkType() {
        return m_ergoNetworkUrlProperty.get() == null ? null : m_ergoNetworkUrlProperty.get().getNetworkType();
    }


     public String getNetworkTypeString() {
        return getNetworkType() != null ? getNetworkType().toString() : "NONE";
    }

    
    
    public JsonObject getJsonObject(){

          JsonObject json = new JsonObject();
          json.addProperty("explorerId", m_id);
          json.add("ergoNetworkUrl", m_ergoNetworkUrlProperty.get().getJsonObject());
          json.add("webUrl", m_websiteUrlProperty.get().getJsonObject());
          return json;
    }

    public void addUpdateListener(ChangeListener<LocalDateTime> changeListener) {
        m_updateListener = changeListener;
        if (m_updateListener != null) {
            lastUpdated.addListener(m_updateListener);

        }
        // lastUpdated.addListener();
    }

    public void removeUpdateListener() {
        if (m_updateListener != null) {
            lastUpdated.removeListener(m_updateListener);
            m_updateListener = null;
        }
    }



}

/*
public HBox getRowItem() {
     String defaultId = m_explorerList.defaultIdProperty().get();

     BufferedButton defaultBtn = new BufferedButton(defaultId != null && getId().equals(defaultId) ? m_radioOnUrl : m_radioOffUrl, 15);

     m_explorerList.defaultIdProperty().addListener((obs, oldval, newVal) -> {
          defaultBtn.setImage(new Image(newVal != null && getId().equals(newVal) ? m_radioOnUrl : m_radioOffUrl));
     });

     defaultBtn.setOnAction(e->{
          String explorerDefaultId = m_explorerList.defaultIdProperty().get();

          if(explorerDefaultId != null && explorerDefaultId.equals(getId())){
               m_explorerList.defaultIdProperty().set(null);
          }else{           
               m_explorerList.defaultIdProperty().set(getId());
          }
     });

     Text topInfoStringText = new Text((m_ergoNetworkUrlProperty.get() != null ? (getName() == null ? "INVALID" : getName()) : "INVALID"));
     topInfoStringText.setFont(m_font);
     topInfoStringText.setFill(m_primaryColor);

     Text topRightText = new Text("");
     topRightText.setFont(m_smallFont);
     topRightText.setFill(m_secondaryColor);

     Text botTimeText = new Text();
     botTimeText.setFont(m_smallFont);
     botTimeText.setFill(m_secondaryColor);
     botTimeText.textProperty().bind(lastUpdated.asString());

     TextField centerField = new TextField("test test ...testtset");
     centerField.setFont(m_largeFont);
     centerField.setId("formField");
     centerField.setEditable(false);
     centerField.setAlignment(Pos.CENTER);
     centerField.setPadding(new Insets(0, 10, 0, 0));
     HBox.setHgrow(centerField, Priority.ALWAYS);
     centerField.textProperty().bind(displayTextProperty);

     Text middleTopRightText = new Text();
     middleTopRightText.setFont(m_font);
     middleTopRightText.setFill(m_secondaryColor);

  //   middleTopRightText.textProperty().bind(cmdProperty);

     Text middleBottomRightText = new Text(getNetworkTypeString());
     middleBottomRightText.setFont(m_font);
     middleBottomRightText.setFill(m_primaryColor);

     VBox centerRightBox = new VBox(middleTopRightText, middleBottomRightText);
     centerRightBox.setAlignment(Pos.CENTER_RIGHT);

     VBox.setVgrow(centerRightBox, Priority.ALWAYS);

     BufferedButton statusBtn = new BufferedButton(statusProperty.get().equals(App.STATUS_STOPPED) ? m_startImgUrl : m_stopImgUrl, 20);
     statusBtn.setId("statusBtn");
     statusBtn.setPadding(new Insets(0, 10, 0, 10));
     

     statusProperty.addListener((obs, oldVal, newVal) -> {
          switch (newVal) {
               case App.STATUS_STOPPED:
                    statusBtn.setImage(new Image(m_startImgUrl));
               break;
               default:
                    statusBtn.setImage(new Image(m_stopImgUrl));
               break;
          }
     });

    

     HBox leftBox = new HBox(defaultBtn);
     HBox rightBox = new HBox(statusBtn);

     leftBox.setAlignment(Pos.CENTER_LEFT);
     rightBox.setAlignment(Pos.CENTER_RIGHT);
     leftBox.setId("bodyBox");
     rightBox.setId("bodyBox");

     Region currencySpacer = new Region();
     currencySpacer.setMinWidth(10);

     HBox centerBox = new HBox(centerField, centerRightBox);
     centerBox.setPadding(new Insets(0, 5, 0, 5));
     centerBox.setAlignment(Pos.CENTER_RIGHT);
     centerBox.setId("darkBox");

     HBox topSpacer = new HBox();
     HBox bottomSpacer = new HBox();

     topSpacer.setMinHeight(2);
     bottomSpacer.setMinHeight(2);

     HBox.setHgrow(topSpacer, Priority.ALWAYS);
     HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
     topSpacer.setId("bodyBox");
     bottomSpacer.setId("bodyBox");

     Region topMiddleRegion = new Region();
     HBox.setHgrow(topMiddleRegion, Priority.ALWAYS);

     HBox topBox = new HBox(topInfoStringText, topMiddleRegion, topRightText);
     topBox.setId("darkBox");

     Text urlText = new Text(m_ergoNetworkUrlProperty.get() != null ? (m_ergoNetworkUrlProperty.get().getUrlString() == null ? "INVALID" : m_ergoNetworkUrlProperty.get().getUrlString()) : "Configure url");
     urlText.setFill(m_primaryColor);
     urlText.setFont(m_smallFont);

     Region bottomMiddleRegion = new Region();
     HBox.setHgrow(bottomMiddleRegion, Priority.ALWAYS);

     HBox bottomBox = new HBox(urlText, bottomMiddleRegion, botTimeText);
     bottomBox.setId("darkBox");
     bottomBox.setAlignment(Pos.CENTER_LEFT);

     HBox.setHgrow(bottomBox, Priority.ALWAYS);

     VBox bodyBox = new VBox(topSpacer, topBox, centerBox, bottomBox, bottomSpacer);
     HBox.setHgrow(bodyBox, Priority.ALWAYS);

     HBox rowBox = new HBox(leftBox, bodyBox, rightBox);
     rowBox.setAlignment(Pos.CENTER_LEFT);
     rowBox.setId("rowBox");

     rowBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
       
        //  m_explorerList.selectedIdProperty().set(getId());
          e.consume();
      
   });

     // centerBox.prefWidthProperty().bind(rowBox.widthProperty().subtract(leftBox.widthProperty()).subtract(rightBox.widthProperty()));
     Runnable updateBlockInfo = () ->{
          statusProperty.set(App.STATUS_STARTED);
          getLatestBlocks((onSucceeded)->{
               Object sourceValue = onSucceeded.getSource().getValue();
               if(sourceValue != null && sourceValue instanceof JsonObject){
                    JsonObject latestBlocksObject = (JsonObject) sourceValue;
                  
                    
                    JsonElement itemsElement = latestBlocksObject.get("items");
                    if(itemsElement != null && itemsElement.isJsonArray()){
                         JsonArray itemsArray = itemsElement.getAsJsonArray();
                         if(itemsArray.size() > 0){

                              JsonElement latestItemElement = itemsArray.get(0);
                              if(latestItemElement.isJsonObject()){
                                   JsonObject latestItemObject = latestItemElement.getAsJsonObject();
                                   
                                   JsonElement heightElement = latestItemObject.get("height");
                                   JsonElement timestampElement = latestItemObject.get("timestamp");
                                   JsonElement minerRewardElement = latestItemObject.get("minerReward");
                                   String displayText = "";
                                   if(heightElement != null && heightElement.isJsonPrimitive()){
                                        long blockHeight = heightElement.getAsLong();
                                        displayText += "Latest block: " + blockHeight;
                                       
                                   }

                                   if(minerRewardElement != null && minerRewardElement.isJsonPrimitive()){
                                        long minerReward = minerRewardElement.getAsLong();
                                        ErgoAmount ergoAmount = new ErgoAmount(minerReward, m_ergoNetworkUrlProperty.get().getNetworkType());

                                        displayText += (displayText.equals("") ? "" : " - ") + "Reward: " + ergoAmount;
                                   }
                                   if(timestampElement != null && timestampElement.isJsonPrimitive()){
                                        LocalDateTime latestItemTimestamp = Utils.milliToLocalTime(timestampElement.getAsLong());
                                        lastUpdated.set(latestItemTimestamp);
                                   }else{
                                        lastUpdated.set(LocalDateTime.now());
                                   }
                                   
                                   displayTextProperty.set(displayText);
                                
                              }

                         }
                    }
               }else{
                   
               }
               statusProperty.set(App.STATUS_STOPPED);
          }, (onFailed)->{
               displayTextProperty.set("Error: " + onFailed.getSource().getException().toString());
               statusProperty.set(App.STATUS_STOPPED);
          });
     };
     updateBlockInfo.run();

     Runnable updateSelected = () -> {
       String selectedId =  null;// m_explorerList.selectedIdProperty().get();
       boolean isSelected = selectedId != null && getId().equals(selectedId);

       centerField.setId(isSelected ? "selectedField" : "formField");

       rowBox.setId(isSelected ? "selected" : "unSelected");
   };

   //  m_explorerList.selectedIdProperty().addListener((obs, oldval, newVal) -> updateSelected.run());

     statusBtn.setOnAction(action -> {
          if (statusProperty.get().equals(App.STATUS_STOPPED)) {
              
          } else {
               shutdownNow.set(LocalDateTime.now());
          }
     });

     return rowBox;
}

 
 */
