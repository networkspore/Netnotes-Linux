package com.netnotes;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import io.netnotes.engine.networks.ergo.ErgoNetworkUrl;
import io.netnotes.engine.networks.ergo.ErgoTransactionView;
import io.netnotes.engine.NetworksData;
import io.netnotes.engine.NoteConstants;
import io.netnotes.engine.NoteInterface;
import io.netnotes.engine.NoteMsgInterface;
import io.netnotes.engine.TabInterface;
import io.netnotes.engine.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ErgoExplorerData {
     public final static String TESTNET_STRING = NetworkType.TESTNET.toString();
     public final static String MAINNET_STRING = NetworkType.MAINNET.toString();
     
     public ErgoNetworkUrl m_websiteUrlProperty = null;
     public ErgoNetworkUrl m_ergoNetworkUrlProperty = null;
     private ErgoExplorerList m_explorerList = null;
    
     public final SimpleStringProperty statusProperty = new SimpleStringProperty(NoteConstants.STATUS_STOPPED);
     public final SimpleObjectProperty<LocalDateTime> lastUpdated = new SimpleObjectProperty<LocalDateTime>(null);
     public final SimpleObjectProperty<LocalDateTime> shutdownNow = new SimpleObjectProperty<>(LocalDateTime.now());
     public final SimpleStringProperty displayTextProperty = new SimpleStringProperty("");

     private ChangeListener<LocalDateTime> m_updateListener = null;
     private String m_id;
     private String m_imgUrl;
     
    

     public ErgoExplorerData(String id,String name, String imgUrl, String apiUrl,String websiteName, String webstiteUrl, ErgoExplorerList explorerList){
          m_id = id; 
          m_ergoNetworkUrlProperty = new ErgoNetworkUrl(id,name, "https", apiUrl, 443, NetworkType.MAINNET );
          m_websiteUrlProperty = new ErgoNetworkUrl( id, websiteName, "https", webstiteUrl, 443, NetworkType.MAINNET);
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
               m_ergoNetworkUrlProperty =  new ErgoNetworkUrl(ergoNetworkUrl.getAsJsonObject());          
          }else{
               throw new Exception("Insuffient data");
          }

          m_imgUrl = imgUrlElement != null && imgUrlElement.isJsonPrimitive() ? imgUrlElement.getAsString() : ErgoExplorers.getSmallAppIconString();
          
          m_websiteUrlProperty = webUrlElement != null && webUrlElement.isJsonObject() ? new ErgoNetworkUrl(webUrlElement.getAsJsonObject()) : null;
          
     }

     public String getImgUrl(){
          return m_imgUrl;
     }

     public NetworksData getNetworksData(){
          return m_explorerList.getErgoExplorer().getNetworksData();
     }

    
     public Future<?> getTokenInfo(JsonObject json, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
          JsonElement tokenIdElement = json != null ? json.get("tokenId") : null;
          String tokenId = tokenIdElement != null && tokenIdElement.isJsonPrimitive() ? tokenIdElement.getAsString() : null;
          if(tokenId != null){
               
               return getTokenInfo(tokenId, onSucceeded, onFailed);
          }
          return null;
     }

     /*
     {
          "id": "string",
          "boxId": "string",
          "emissionAmount": 0,
          "name": "string",
          "description": "string",
          "type": "string",
          "decimals": 0
     }
     */
     public Future<?> getTokenInfo(String tokenId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
          
          ErgoNetworkUrl namedUrl =  m_ergoNetworkUrlProperty;
          
          String urlString = namedUrl.getUrlString() + "/api/v1/tokens/" + tokenId;

          return Utils.getUrlJson(urlString, getExecService(), onSucceeded, onFailed);
     
     }


     public class TokenInfoResult {
          private JsonObject m_balanceObject;
          private JsonObject m_tokenInfoObject;

          public TokenInfoResult(JsonObject balanceObject, JsonObject tokenInfoObject){
               m_balanceObject = balanceObject;
               m_tokenInfoObject = tokenInfoObject;
          }

          public JsonObject getBalanceObject(){
               return m_balanceObject;
          }

          public JsonObject getTokenInfObject(){
               return m_tokenInfoObject;
          }
     }

     

     public static JsonObject getTokenInfoFromJsonArray(JsonArray tokenInfoArray, String tokenId){
          if(tokenInfoArray != null && tokenId != null){
               int size = tokenInfoArray.size();
               for(int i = 0; i < size ; i++){
                    JsonElement tokenInfoElement = tokenInfoArray.get(i);
                    JsonObject tokenInfoObject = tokenInfoElement.getAsJsonObject();
                    JsonElement idElement = tokenInfoObject.get("id");
                    if(idElement != null && !idElement.isJsonNull() && idElement.getAsString().equals(tokenId)){
                         return tokenInfoObject;
                    }
               }
          }
          return null;
     }

     

     public ExecutorService getExecService(){
          return getNetworksData().getExecService();
     }

     public Future<?> getNetworkState(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

          ErgoNetworkUrl namedUrl =  m_ergoNetworkUrlProperty;
          if(namedUrl != null && namedUrl.getUrlString().length() > 0){
               String urlString = namedUrl.getUrlString() + "/api/v1/networkState";

               return Utils.getUrlJson(urlString, getExecService(), onSucceeded, onFailed);
          }
     
          
          return null;
     }



    public Future<?> sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
          JsonElement cmdElement = note.get(NoteConstants.CMD);

          if (cmdElement != null && !cmdElement.isJsonNull()) {
               String cmd = cmdElement.getAsString();
              
               switch(cmd){
                    case "getBalance":
                         return getBalance(note, onSucceeded, onFailed);
                    case "getTokenInfo":
                         return getTokenInfo(note, onSucceeded, onFailed);
                    case "getNetworkState":
                         return getNetworkState(note, onSucceeded, onFailed);
                    case "getTransactionById":
                         return getTransactionById(note, onSucceeded, onFailed);
                    case "getTransactionsByAddress":
                         return getTransactionsByAddress(note, onSucceeded, onFailed);
                    case "getTransactionViewsByAddress":
                         return getTransactionViewsByAddress(note, onSucceeded, onFailed);
                    case "getBox":
                    case "getTransactionsByInputsScriptTemplateHash":
                    case "getUnspentByTokenId":
                    case "getUnspentByErgoTree":
                    case "getUnspentByErgoTreeTemplateHash":
                    case "getUnspentByAddress":
                    case "getBoxesByErgoTreeTemplateHash":
                    case "getBoxesByErgoTree":
                    case "getBoxesByTokenId":
                    case "getBlocks":
                         return getBy(note, cmd, onSucceeded, onFailed);
                    
               }
                     
               return null;
          }

          return null;
                
    }

    
    public Future<?> getBy(JsonObject json, String cmd, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
     JsonElement byElement = json != null ? json.get("value") : null;
 
     String value = byElement != null && byElement.isJsonPrimitive() ? byElement.getAsString() : null;
     if(value != null){
          JsonElement offsetElement = json.get("offset");
          JsonElement limitElement = json.get("limit");
          JsonElement sortDirectionElement = json.get("sortDirection");
          int offset = offsetElement != null && !offsetElement.isJsonNull() ? offsetElement.getAsInt() : -1;
          int limit = limitElement != null && !limitElement.isJsonNull() ? limitElement.getAsInt() : -1;
          String sortDirection = sortDirectionElement != null && !sortDirectionElement.isJsonNull() ? sortDirectionElement.getAsString() : null;
          
          switch(cmd){
               case "getBlocks":
                    return getByUrlPostFix("/api/v1/blocks/", value, offset, limit,sortDirection, onSucceeded, onFailed);
            
               case "getTransactionsByInputsScriptTemplateHash":
                    return getByUrlPostFix("/api/v1/transactions/byInputsScriptTemplateHash/", value, offset, limit,sortDirection, onSucceeded, onFailed);
               
               case "getUnspentByErgoTreeTemplateHash":
                    return getByUrlPostFix("/api/v1/boxes/unspent/byErgoTreeTemplateHash/", value, offset, limit,sortDirection, onSucceeded, onFailed);
               case "getUnspentByErgoTree":
                    return getByUrlPostFix("/api/v1/boxes/unspent/byErgoTree/", value, offset, limit,sortDirection, onSucceeded, onFailed);
                case "getUnspentByTokenId":
                    return getByUrlPostFix("/api/v1/boxes/unspent/byTokenId/", value, offset, limit,sortDirection, onSucceeded, onFailed);
               case "getUnspentUnconfirmedByAddress":
                    return getByUrlPostFix("/api/v1/boxes/unspent/unconfirmed/byAddress/", value, offset, limit, sortDirection, onSucceeded, onFailed);
               case "getUnspentByAddress":
                    return getByUrlPostFix("/api/v1/boxes/unspent/byAddress/", value, offset, limit,sortDirection, onSucceeded, onFailed);
              
               case "getBoxesByErgoTreeTemplateHash":
                    return getByUrlPostFix("/api/v1/boxes/byErgoTreeTemplateHash/", value, offset, limit, sortDirection, onSucceeded, onFailed);
               case "getBoxesByErgoTree":
                    return getByUrlPostFix("/api/v1/boxes/byErgoTree/", value, offset, limit,sortDirection, onSucceeded, onFailed);
               case "getBoxesByAddress":
                    return getByUrlPostFix("/api/v1/boxes/byAddress/", value, offset, limit,sortDirection, onSucceeded, onFailed);
               case "getBoxesByTokenId":
                    return getByUrlPostFix("/api/v1/boxes/byTokenId/", value, offset, limit,sortDirection, onSucceeded, onFailed);
               case "getBox":
                    return getByUrlPostFix("/api/v1/boxes/", value, -1, -1, null, onSucceeded, onFailed);
               
          }
     }
     return null;
}

public Future<?> getByUrlPostFix(String urlPostfix, String value, int offset, int limit,String sortDirection, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
     ErgoNetworkUrl namedUrl =  m_ergoNetworkUrlProperty;
     
     String urlString = namedUrl.getUrlString() + urlPostfix + value;

     if(limit != -1 || offset != -1 || sortDirection != null){
          urlString += "?offset=" + (offset != -1 ? offset : 0) + ( limit != -1 ? "&limit=" + limit : "")+ "&sortDirection=" + (sortDirection != null ? (sortDirection.equals("asc") ? "asc" : "dsc") : "asc");
     }

     return Utils.getUrlJson(urlString, getExecService(), onSucceeded, onFailed);
}


     /*public Object sendNote(JsonObject note){
          JsonElement subjectElement = note.get(NoteConstants.CMD);
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
          JsonElement subjectElement = note.get(NoteConstants.CMD);
        
          if (subjectElement != null && subjectElement.isJsonPrimitive()) {
               
          
               String subject = subjectElement.getAsString();

               switch(subject){
                    case "getWebsiteTxLink":
                         return getWebsiteTxLink(note);
                    default:    
               }
                    
               
               
          }
          return null;
     }



    public NoteInterface getNoteInterface(){
     
          return new NoteInterface() {
               
               public String getName(){
                    return ErgoExplorerData.this.getName();
               }

               public String getNetworkId(){
                    return ErgoExplorerData.this.getId();
               }

               public Image getAppIcon(){
                    return ErgoExplorerData.this.getImgUrl() != null ? new Image(ErgoExplorerData.this.getImgUrl()) : null;
               }

           

               public Future<?> sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
                   return ErgoExplorerData.this.sendNote(note, onSucceeded, onFailed);
               }

               public Object sendNote(JsonObject note){
                    return ErgoExplorerData.this.sendNote(note);
               }

               public JsonObject getJsonObject(){
                    return ErgoExplorerData.this.getJsonObject();
               }

               

               public TabInterface getTab(Stage appStage, SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject,  Button networkBtn){
                    return null;
               }
         


               public NetworksData getNetworksData(){
                    return null;
               }

               public NoteInterface getParentInterface(){
                    return null;
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
                    return NoteConstants.STARTED;    
               }

               public void setConnectionStatus(int status){

               }


               public String getDescription(){
                    return m_explorerList.getErgoExplorer().getDescription();
               }
          };
    }
     private Future<?> getBalance(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
          JsonElement addressElement = note.get("address");

          if(addressElement != null && addressElement.isJsonPrimitive()){
               return getBalance(addressElement.getAsString(), onSucceeded, onFailed);
          }
          return null;
     }

     protected Future<?> getBalance(String address, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

          ErgoNetworkUrl namedUrl =  m_ergoNetworkUrlProperty;

          if(namedUrl != null && namedUrl.getUrlString().length() > 0){
               
                 Task<JsonObject> task = new Task<JsonObject>() {
                    @Override
                    public JsonObject call() throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, InterruptedException {
                         String urlString = namedUrl.getUrlString() + "/api/v1/addresses/" + address + "/balance/total";

                         JsonObject json = Utils.getJsonFromUrlSync(urlString);

                         return json != null ? getBalanceTokenInfoBlocking(json) : null;
                    }

               };

               task.setOnFailed(onFailed);

               task.setOnSucceeded(onSucceeded);

               return getNetworksData().getExecService().submit(task);
          
          }
          return null;
     }

   
     

     public JsonObject getBalanceTokenInfoBlocking(JsonObject balance) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, InterruptedException, IOException{
          if(balance != null){
               JsonObject cachedTokenInfo = getNetworksData().getDataBlocking("array", "tokenInfo", NoteConstants.EXPLORER_NETWORK , ErgoNetwork.NETWORK_ID);
     
               JsonObject infoObject = cachedTokenInfo;
               JsonObject balanceJson = balance;

               JsonElement tokenInfoArrayElement = infoObject != null ? infoObject.get("array") : null;
               JsonArray tokenInfoArray = tokenInfoArrayElement != null ? tokenInfoArrayElement.getAsJsonArray() : new JsonArray();
               int tokenInfoArraySize = tokenInfoArray.size();

          
               JsonElement confirmedElement = balanceJson.get("confirmed");
               JsonObject confirmedObject = confirmedElement != null && confirmedElement.isJsonObject() ? confirmedElement.getAsJsonObject() : null;
               if(confirmedObject != null){
               
                    JsonElement tokensElement = confirmedObject != null ? confirmedObject.get("tokens") : null;
                    JsonArray confirmedTokenArray = tokensElement != null && tokensElement.isJsonArray() ? tokensElement.getAsJsonArray() : null;     
                    int tokenSize = confirmedTokenArray != null ? confirmedTokenArray.size() : 0;
                    
                    if(confirmedTokenArray != null && tokenSize > 0){
                         balanceJson.remove("confirmed");
                         confirmedObject.remove("tokens");
                         JsonArray udpatedTokenArray = new JsonArray();

                         String urlString = m_ergoNetworkUrlProperty.getUrlString() + "/api/v1/tokens/";

                         for (int i = 0; i < tokenSize ; i++) {
                              JsonElement tokenElement = confirmedTokenArray.get(i);

                              JsonObject tokenObject = tokenElement.getAsJsonObject();

                              JsonElement tokenIdElement = tokenObject.get("tokenId");
                              String tokenId = tokenIdElement.getAsString();

                              JsonObject infoArrayResult = getTokenInfoFromJsonArray(tokenInfoArray, tokenId);

                              if(infoArrayResult != null){
                                   tokenObject.add("tokenInfo", infoArrayResult);
                              }else{
                                   JsonObject urlInfoResult = Utils.getJsonFromUrlSync(urlString + tokenId);
                                   
                                   if(urlInfoResult != null){
                                        tokenObject.add("tokenInfo", urlInfoResult);
                                        tokenInfoArray.add(urlInfoResult);
                                   }
                              }
                              udpatedTokenArray.add(tokenObject);
                         }
                         confirmedObject.add("tokens", udpatedTokenArray);
                         balanceJson.add("confirmed", confirmedObject);
                         if(tokenInfoArray.size() > tokenInfoArraySize){
                              JsonObject tokenInfoObject = new JsonObject();
                              tokenInfoObject.add("array", tokenInfoArray);
                              getNetworksData().save("array", "tokenInfo", NoteConstants.EXPLORER_NETWORK , ErgoNetwork.NETWORK_ID, tokenInfoObject);
                           
                         }
                            
                    }

               
               }

               return balanceJson;
          }           
          
          return null;

     }


     private Future<?> getTransactionById(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
          JsonElement txIdElement = note!=null ? note.get("txId") : null;

          if(txIdElement != null && txIdElement.isJsonPrimitive()){
              return getTransaction(txIdElement.getAsString(), onSucceeded, onFailed);
          }
          return null;
     }

     protected Future<?> getTransaction(String txId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

          ErgoNetworkUrl namedUrl =  m_ergoNetworkUrlProperty;
          
          if(namedUrl != null && namedUrl.getUrlString().length() > 0){

               String urlString = namedUrl.getUrlString() + "/api/v1/transactions/" + txId;
     
               return Utils.getUrlJson(urlString,getExecService(), onSucceeded, onFailed);
          }
          return null;
     }

     public int MAX_TRANSACTIONS = 100;

     private Future<?> getTransactionsByAddress(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
          JsonElement addressElement = note.get("address");
     
          if(addressElement != null && !addressElement.isJsonNull()){

               JsonElement startIndexElement = note.get("startIndex");
               JsonElement limitElement = note.get("limit");
               JsonElement conciseElement = note.get("concise");
               JsonElement fromHeightElement = note.get("fromHeight");
               JsonElement toHeightElement = note.get("toHeight");
               
               String address = addressElement.getAsString();

               if(startIndexElement != null && limitElement != null && conciseElement != null && fromHeightElement != null && toHeightElement != null){
                    int startIndex = startIndexElement != null && startIndexElement.isJsonPrimitive() ? startIndexElement.getAsInt() : 0;
                    int limit = limitElement != null && limitElement.isJsonPrimitive() ? limitElement.getAsInt() : 0;
                    boolean concise = conciseElement != null && !conciseElement.isJsonNull() ? conciseElement.getAsBoolean() : false;
                    int fromHeight = fromHeightElement != null && !fromHeightElement.isJsonNull() ? fromHeightElement.getAsInt() : -1;
                    int toHeight = toHeightElement != null && !toHeightElement.isJsonNull() ? toHeightElement.getAsInt() : -1;

                    return getAddressTransactions(address, startIndex, limit, concise, fromHeight, toHeight, onSucceeded, onFailed);
               }else{
                    return getAddressTransactions(address, MAX_TRANSACTIONS, onSucceeded, onFailed);
               }
          }
          return null;
     }

     public Future<?> getAddressTransactions(String address, int maxSize, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
         
          ErgoNetworkUrl namedUrl =  m_ergoNetworkUrlProperty;
          if(namedUrl != null && namedUrl.getUrlString().length() > 0){

               String urlString = namedUrl.getUrlString() + "/api/v1/addresses/" + address + "/transactions?limit=" + maxSize;          

               return Utils.getUrlJson(urlString, getExecService(), onSucceeded, onFailed);
          }
          return null;
     }


   
     private Future<?> getTransactionViewsByAddress(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
          JsonElement addressElement = note.get("address");
          JsonElement locationIdElement = note.get("locationId");
         

          if(addressElement != null && !addressElement.isJsonNull()){
               String locationId = locationIdElement != null && !locationIdElement.isJsonNull() ? locationIdElement.getAsString() : null;
               JsonElement startIndexElement = note.get("startIndex");
               JsonElement limitElement = note.get("limit");
               JsonElement conciseElement = note.get("concise");
               JsonElement fromHeightElement = note.get("fromHeight");
               JsonElement toHeightElement = note.get("toHeight");
               
               String address = addressElement.getAsString();

               if(startIndexElement != null && limitElement != null && conciseElement != null && fromHeightElement != null && toHeightElement != null){
                    int startIndex = startIndexElement != null && startIndexElement.isJsonPrimitive() ? startIndexElement.getAsInt() : 0;
                    int limit = limitElement != null && limitElement.isJsonPrimitive() ? limitElement.getAsInt() : 0;
                    boolean concise = conciseElement != null && !conciseElement.isJsonNull() ? conciseElement.getAsBoolean() : false;
                    int fromHeight = fromHeightElement != null && !fromHeightElement.isJsonNull() ? fromHeightElement.getAsInt() : -1;
                    int toHeight = toHeightElement != null && !toHeightElement.isJsonNull() ? toHeightElement.getAsInt() : -1;

                    return getAddressTransactionViews(locationId, address, startIndex, limit, concise, fromHeight, toHeight, onSucceeded, onFailed);
               }else{
                    return getAddressTransactionViews(locationId, address, MAX_TRANSACTIONS, onSucceeded, onFailed);
               }
          }
          return null;
     }

     public Future<?> getAddressTransactionViews(String locationId, String address, int maxSize, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
         
          ErgoNetworkUrl namedUrl =  m_ergoNetworkUrlProperty;
          if(namedUrl != null && namedUrl.getUrlString().length() > 0){

               String urlString = namedUrl.getUrlString() + "/api/v1/addresses/" + address + "/transactions?limit=" + maxSize;          

               return getAddressTxViews(locationId, address, urlString, maxSize, onSucceeded, onFailed);
          }
          return null;
     }
     



     public Future<?> getAddressTxViews(String locationId, String address, String urlString, int maxSize, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
         
          Task<Object> task = new Task<Object>() {
               @Override
               public Object call() throws IOException {
                    
                    URL url = new URL(urlString);
                    URLConnection con = url.openConnection();
                    con.setRequestProperty("User-Agent", Utils.USER_AGENT);
                   // JsonArray txViewsArray = new JsonArray();
                    
                   JsonArray txViewsArray = new JsonArray();

                    try(
                         InputStreamReader streamReader = new InputStreamReader(con.getInputStream());
                         JsonReader reader = new JsonReader(streamReader);
                    ){
                         reader.beginObject();
                         while(reader.hasNext()){
                              switch(reader.nextName()){
                                   case "items":
                                        reader.beginArray();
                                        while(reader.hasNext()){
                                             if(txViewsArray.size() > maxSize){
                                                  reader.skipValue();
                                             }else{
                                                  ErgoTransactionView txView = new ErgoTransactionView(address, reader, true);
                                                  txView.setTxLink(getWebsiteTxLink(txView.getId()));
                                                  txView.setApiLink(getApiTxLink(txView.getId()));
                                                  txViewsArray.add(txView.getJsonObject());  
                                             }
                                        }
                                        reader.endArray();
                                   break;
                                   default:
                                        reader.skipValue();
                              }
                         }
                         reader.endObject();
                    }
                    
                    JsonObject json = new JsonObject();
                    json.add("txs", txViewsArray);
                    return json;
               }

          };

     
          task.setOnFailed(onFailed);

          task.setOnSucceeded(onSucceeded);

          return getExecService().submit(task);
     }


     public Future<?> getAddressTransactions(String address,int startIndex, int limit, boolean concise, int fromHeight, int toHeight,  EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
          
          ErgoNetworkUrl namedUrl =  m_ergoNetworkUrlProperty;
          if(namedUrl != null && namedUrl.getUrlString().length() > 0){

               String urlString = namedUrl.getUrlString() + "/api/v1/addresses/" + address + "/transactions?offset=" + (startIndex < 0 ? 0 : startIndex ) + "&limit=" + (limit < 1 ? 500 : (limit > 500 ? 500 : limit)) + "&concise=" + concise + "&fromHeight=" + (fromHeight > -1 ? fromHeight : 0) + (toHeight > 0 ? "&toHeight=" + toHeight : ""  );          

               return Utils.getUrlJson(urlString,getExecService(), onSucceeded, onFailed);
          }
          return null;
     }

     public Future<?> getAddressTransactionViews(String locationId, String address,int startIndex, int limit, boolean concise, int fromHeight, int toHeight,  EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
          limit =  (limit < 1 ? 500 : (limit > 500 ? 500 : limit));
          ErgoNetworkUrl namedUrl =  m_ergoNetworkUrlProperty;
          if(namedUrl != null && namedUrl.getUrlString().length() > 0){
             
               String urlString = namedUrl.getUrlString() + "/api/v1/addresses/" + address + "/transactions?offset=" + (startIndex < 0 ? 0 : startIndex ) + "&limit=" + limit+ "&concise=" + concise + "&fromHeight=" + (fromHeight > -1 ? fromHeight : 0) + (toHeight > 0 ? "&toHeight=" + toHeight : ""  );          

               return getAddressTxViews(locationId, address,urlString, limit, onSucceeded, onFailed);
          }
          return null;
     }

     public String getWebsiteTxLink(String txId) {

          ErgoNetworkUrl websiteUrl =  m_websiteUrlProperty;

          if(websiteUrl != null && websiteUrl.getUrlString().length() > 0){

               String urlString = websiteUrl.getUrlString() + "/en/transactions/" + txId;
     
               return urlString;
          }

          return null;
     }

     public String getApiTxLink(String txId){
          ErgoNetworkUrl apiUrl =  m_ergoNetworkUrlProperty;

          if(apiUrl != null && apiUrl.getUrlString().length() > 0){
               
               String urlString = apiUrl.getUrlString() + "/api/v1/transactions/" + txId;
     
               return urlString;
          }

          return null;
     }

     public JsonObject getWebsiteTxLink(JsonObject note) {
     
          JsonElement txIdElement = note != null ? note.get("txId") : null;
          String txId = txIdElement != null ? txIdElement.getAsString() : null;
          
          String txLink = getWebsiteTxLink(txId);

          if(txId != null && txLink != null){
               JsonObject json = NoteConstants.getJsonObject("txId", txId);
               json.addProperty("websiteTxLink", txLink);
               json.addProperty("id", getId());
               return json;
          }

          return null;
     }

     public ErgoNetworkUrl websiteNetworkUrl(){
          return m_websiteUrlProperty;
     }


     public ErgoNetworkUrl ergoNetworkUrl(){
          return m_ergoNetworkUrlProperty;
     }

     public String getId(){
          return m_id;
     }

     public String getName() {
          return m_ergoNetworkUrlProperty == null ? "INVALID" : m_ergoNetworkUrlProperty.getName();
     }

     public NetworkType getNetworkType() {
        return m_ergoNetworkUrlProperty == null ? null : m_ergoNetworkUrlProperty.getNetworkType();
    }


     public String getNetworkTypeString() {
        return getNetworkType() != null ? getNetworkType().toString() : "NONE";
    }

    
    
    public JsonObject getJsonObject(){

          JsonObject json = new JsonObject();

          json.addProperty("name", getName());
          json.addProperty("id", m_id);
          
          if(m_ergoNetworkUrlProperty != null){
               json.add("ergoNetworkUrl", m_ergoNetworkUrlProperty.getJsonObject());
          }

          if(m_websiteUrlProperty != null){
               json.add("webUrl", m_websiteUrlProperty.getJsonObject());
          }
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
