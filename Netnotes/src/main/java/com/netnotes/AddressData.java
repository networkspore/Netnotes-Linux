package com.netnotes;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.RestApiErgoClient;
import org.ergoplatform.appkit.UnsignedTransaction;
import org.ergoplatform.sdk.ErgoToken;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.netnotes.engine.networks.ergo.AddressInformation;
import io.netnotes.engine.networks.ergo.ErgoAmount;
import io.netnotes.engine.networks.ergo.ErgoBoxInfo;
import io.netnotes.engine.networks.ergo.ErgoTransactionView;
import io.netnotes.engine.networks.ergo.ErgoTxInfo;
import io.netnotes.engine.networks.ergo.ErgoTransactionView.TransactionStatus;

import io.netnotes.engine.Network;
import io.netnotes.engine.NoteConstants;
import io.netnotes.engine.PriceAmount;
import io.netnotes.engine.PriceCurrency;
import io.netnotes.engine.PriceQuote;
import io.netnotes.engine.Utils;
import com.satergo.Wallet;
import com.satergo.WalletKey;
import com.satergo.WalletKey.Failure;
import com.satergo.ergo.ErgoInterface;

import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.math.BigDecimal;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.BlockHeader;
import org.ergoplatform.appkit.BoxOperations;
import org.ergoplatform.appkit.ConstantsBuilder;
import org.ergoplatform.appkit.ErgoClient;
import org.ergoplatform.appkit.InputBox;
import org.ergoplatform.appkit.BlockchainContext;
import org.ergoplatform.appkit.BlockchainDataSource;
import org.ergoplatform.appkit.UnsignedTransactionBuilder;
import org.ergoplatform.appkit.OutBoxBuilder;
import org.ergoplatform.appkit.OutBox;

public class AddressData extends Network {  
    
    private int m_index;

    private String m_balanceString = null;
    private JsonParser m_jsonParser = new JsonParser();
 
    private ErgoWalletData m_walletData;
    private int m_apiIndex = 0;
    
    private final String m_addressString;
    private final NetworkType m_networkType;
    private AddressesData m_addressesData = null;
    private AtomicBoolean m_isAquired = new AtomicBoolean(false);
    
    public AddressData(String name, int index, String addressString, NetworkType networktype, ErgoWalletData walletData) {
        super(null, name, addressString, walletData);
        //m_wallet = wallet;
        m_walletData = walletData;
        
        m_index = index;
        m_networkType = networktype;
        m_addressString = addressString;
   
    }

    public JsonObject getAddressJson(){
        JsonObject json = new JsonObject();
        json.addProperty("address", m_addressString);
        json.addProperty("name", getName());
        json.addProperty("networkType", m_networkType.toString());
        JsonObject balanceObject = getBalance();
        if(balanceObject != null){
            json.add("balance", balanceObject);
        }
        return json;
    }

    public Address createAddress() throws RuntimeException{
        return Address.create(m_addressString);
    }

    public void setAddressesData(AddressesData addressesData){
        m_addressesData = addressesData;
    }

    public BigDecimal totalQuote(){
        return m_totalQuote;
    }

    public BigDecimal totalErg(){
        return m_totalErg;
    }

    public void updateBalance() {
       
        JsonObject note = NoteConstants.getCmdObject("getBalance");
        note.addProperty("address", m_addressString);
    
        getErgoNetworkData().getErgoExplorers().sendNote(note, success -> {
            Object sourceObject = success.getSource().getValue();

            if (sourceObject != null) {
                JsonObject jsonObject = (JsonObject) sourceObject;
                JsonObject balance = parseBalance(jsonObject);
              
                setBalance(balance);
              
            }},
        failed -> {

            try {
                Files.writeString(NoteConstants.logFile.toPath(), "AddressData, Explorer failed update: " + failed.getSource().getException().toString() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                
                
            }
        });
    }

     public Future<?> getTransactions(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed ){
        JsonObject txNote = NoteConstants.getCmdObject("getTransactionsByAddress");
        txNote.addProperty("address", m_addressString);

        JsonElement startIndexElement = note.get("startIndex");
        JsonElement limitElement = note.get("limit");
        JsonElement conciseElement = note.get("concise");
        JsonElement fromHeightElement = note.get("fromHeight");
        JsonElement toHeightElement = note.get("toHeight");

        if(startIndexElement != null){
            txNote.add("startIndex", startIndexElement);
        }
        if(limitElement != null){
            txNote.add("limit",limitElement);
        }
        if(conciseElement != null){
            txNote.add("concise", conciseElement);
        }
        if(fromHeightElement != null){
            txNote.add("fromHeight", fromHeightElement);
        }
        if(toHeightElement != null){
            txNote.add("toHeight", toHeightElement);
        }

        return getErgoNetworkData().getErgoExplorers().sendNote(txNote, onSucceeded, onFailed);

    }




    public Future<?> getUnspentBoxes(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed ){
        JsonObject note = NoteConstants.getCmdObject("getUnspentByAddress");
        note.addProperty("value", m_addressString);
    
        return getErgoNetworkData().getErgoExplorers().sendNote(note, onSucceeded,onFailed);
    }
    


  
    /*public void openAddressJson(JsonObject json){
        if(json != null){
         
            JsonElement txsElement = json.get("txs");
            
            if(txsElement != null && txsElement.isJsonArray()){
                openWatchedTxs(txsElement.getAsJsonArray());
            }
           
         
        }
    }*/

    public ErgoNetworkData getErgoNetworkData(){
        
        return m_walletData.getErgoWallets().getErgoNetworkData();
    }
   


    /*public void openWatchedTxs(JsonArray txsJsonArray){
        if(txsJsonArray != null){
       
            int size = txsJsonArray.size();

            for(int i = 0; i<size ; i ++){
                JsonElement txElement = txsJsonArray.get(i);

                if(txElement != null && txElement.isJsonObject()){
                    JsonObject txJson = txElement.getAsJsonObject();
                                                
                        JsonElement txIdElement = txJson.get("txId");
                        JsonElement parentAdrElement = txJson.get("parentAddress");
                        JsonElement timeStampElement = txJson.get("timeStamp");
                        JsonElement txTypeElement = txJson.get("txType");
                       // JsonElement nodeUrlElement = txJson.get("nodeUrl");

                        if(txIdElement != null && txIdElement.isJsonPrimitive() 
                            && parentAdrElement != null && parentAdrElement.isJsonPrimitive() 
                            && timeStampElement != null && timeStampElement.isJsonPrimitive() 
                            && txTypeElement != null && txTypeElement.isJsonPrimitive()){

                            String txId = txIdElement.getAsString();
                            String txType = txTypeElement.getAsString();
                            String parentAdr = parentAdrElement.getAsString();

                            

                            if(parentAdr.equals(getAddressString())){
                                switch(txType){
                                    case TransactionType.SEND:
                                        
                                        try {
                                            ErgoSimpleSendTx simpleSendTx = new ErgoSimpleSendTx(txId, this, txJson);
                                            addWatchedTransaction(simpleSendTx, false);
                                        } catch (Exception e) {
                                            try {
                                                Files.writeString(NoteConstants.logFile.toPath(), "\nCould not read tx json: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                            } catch (IOException e1) {
                                                
                                            }
                                        }
                                      
                                        break;
                                    default:
                                        ErgoTransaction ergTx = new ErgoTransaction(txId, this, txType);
                                        
                                        addWatchedTransaction(ergTx, false);
                                }
                            }
                        }
                    }
                }
            
        }
    }*/

    

    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("address", m_addressString);
        JsonObject balanceObject = getBalance();
        if(balanceObject != null){
            json.add("balance", balanceObject);
        }
        
        return json;
    }




   /*  public void addWatchedTransaction(ErgoTransaction transaction){
        addWatchedTransaction(transaction, true);
    }

    public void addWatchedTransaction(ErgoTransaction transaction, boolean save){
        if(getWatchedTx(transaction.getTxId()) == null){
            m_watchedTransactions.add(transaction);
            transaction.addUpdateListener((obs,oldval,newval)->{
            
                saveAddresInfo();
            });
            if(save){
                saveAddresInfo();
            }
        }
    }*/

   

   /* public JsonArray getWatchedTxJsonArray(){
        ErgoTransaction[] ergoTransactions =  getWatchedTxArray();

        JsonArray jsonArray = new JsonArray();
   
        for(int i = 0; i < ergoTransactions.length; i++){
            JsonObject tokenJson = ergoTransactions[i].getJsonObject();
            jsonArray.add(tokenJson);
        }
 
        return jsonArray; 
    }

    public ErgoTransaction getWatchedTx(String txId){
        if(txId != null){
            ErgoTransaction[] txs =  getWatchedTxArray();
            
            for(int i = 0; i < txs.length; i++){
                ErgoTransaction tx = txs[i];
                if(txId.equals(tx.getTxId())){
                    return tx;
                }
            }
        }
        return null;
    }*/




    public String getButtonText() {
        return "  " + getName() + "\n   " + getAddressString();
    }

    /*public boolean donate(){
        BigDecimal amountFullErg = dialog.showForResult().orElse(null);
		if (amountFullErg == null) return;
		try {
			Wallet wallet = Main.get().getWallet();
			UnsignedTransaction unsignedTx = ErgoInterface.createUnsignedTransaction(Utils.createErgoClient(),
					wallet.addressStream().toList(),
					DONATION_ADDRESS, ErgoInterface.toNanoErg(amountFullErg), Parameters.MinFee, Main.get().getWallet().publicAddress(0));
			String txId = wallet.transact(Utils.createErgoClient().execute(ctx -> {
				try {
					return wallet.key().sign(ctx, unsignedTx, wallet.myAddresses.keySet());
				} catch (WalletKey.Failure ex) {
					return null;
				}
			}));
			if (txId != null) Utils.textDialogWithCopy(Main.lang("transactionId"), txId);
		} catch (WalletKey.Failure ignored) {
			// user already informed
		}
    }*/
 /* return ;
        }); */
    public String getNodesId() {
        return "";
    }

 

     
    
  
   /* public ErgoTransaction[] getReverseTxArray(){
        ArrayList<ErgoTransaction> list = new ArrayList<>(m_watchedTransactions);
        Collections.reverse(list);

        int size = list.size();
        ErgoTransaction[] txArray = new ErgoTransaction[size];
        txArray = list.toArray(txArray);
        return txArray;
    }

    public ErgoTransaction[] getWatchedTxArray(){
        int size = m_watchedTransactions.size();
        ErgoTransaction[] txArray = new ErgoTransaction[size];
        txArray = m_watchedTransactions.toArray(txArray);
        return txArray;
    }

    public void removeTransaction(String txId){
        removeTransaction(txId, true);
    }

     public void removeTransaction(String txId, boolean save){
        
        ErgoTransaction ergTx = getWatchedTx(txId);
        if(ergTx != null){
            m_watchedTransactions.remove(ergTx);
        }
        if(save){
            saveAddresInfo();
        }
    }*/

    



   

 

    public int getIndex() {
        return m_index;
    }



    public String getAddressString() {
        return m_addressString;
    }

    public String getAddressMinimal(int show) {
        String adr = m_addressString;
        int len = adr.length();

        return (show * 2) > len ? adr : adr.substring(0, show) + "..." + adr.substring(len - show, len);
    }



    public NetworkType getNetworkType() {
        return m_networkType;
    }



    
    /*




    public void updateTransactions(ErgoExplorerData explorerData){
        
        try {
            Files.writeString(logFile.toPath(), "updateTxOffset: " + m_updateTxOffset, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
    
        }

        explorerData.getAddressTransactions(m_addressString, (m_updateTxOffset * UPDATE_LIMIT), UPDATE_LIMIT,
            success -> {
                Object sourceObject = success.getSource().getValue();

                if (sourceObject != null && sourceObject instanceof JsonObject) {
                    JsonObject jsonObject = (JsonObject) sourceObject;
                    
                    Platform.runLater(() ->{
                        updateWatchedTxs(explorerData, jsonObject);
                        //  saveAllTxJson(jsonObject);
                    });  
                    
                    
                }
            },
            failed -> {
                    try {
                        Files.writeString(logFile.toPath(), "\nAddressData, Explorer failed transaction update: " + failed.getSource().getException().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        
                        
                        }
                
                    //update();
                }
        );
        
        
    }*/

    private BigDecimal m_totalQuote = BigDecimal.ZERO;
    private BigDecimal m_totalErg = BigDecimal.ZERO;

    private JsonObject parseBalance(JsonObject balanceJson ){
        m_totalQuote = BigDecimal.ZERO;
        m_totalErg = BigDecimal.ZERO;
        if(m_addressesData != null && balanceJson != null){
            JsonElement confirmedElement = balanceJson.get("confirmed");
            
            boolean isErgoMarket = m_addressesData.isErgoMarket();
            boolean isTokenMarket = m_addressesData.isTokenMarket();

            if(confirmedElement != null && (isErgoMarket || isTokenMarket)){
            

                JsonObject confirmedObject = confirmedElement != null && confirmedElement.isJsonObject() ? confirmedElement.getAsJsonObject() : null;
                
                if(confirmedObject != null){
                    balanceJson.remove("confirmed");

                    PriceQuote ergoQuote = isErgoMarket ? m_addressesData.getErgoQuote() : null;
                    if(ergoQuote != null){
                        confirmedObject.add("ergoQuote", ergoQuote.getJsonObject());
                        
                        JsonElement nanoErgElement = confirmedObject.get("nanoErgs");
                        long nanoErg = nanoErgElement != null && nanoErgElement.isJsonPrimitive() ? nanoErgElement.getAsLong() : 0;
                        ErgoAmount ergoAmount =  new ErgoAmount(nanoErg, m_networkType);
                    
                        BigDecimal ergoQuoteAmount = ergoQuote.getQuote().multiply(ergoAmount.getBigDecimalAmount());    
                        
                        confirmedObject.addProperty("ergoQuoteAmount", ergoQuoteAmount);
                        m_totalErg = m_totalErg.add(ergoAmount.getBigDecimalAmount());    
                        m_totalQuote = m_totalQuote.add(ergoQuoteAmount);
                        
                    }


                    JsonElement tokensElement = confirmedObject != null ? confirmedObject.get("tokens") : null;

                    JsonArray confirmedTokenArray = tokensElement != null && tokensElement.isJsonArray() ? tokensElement.getAsJsonArray() : null;
                    int tokenSize = confirmedTokenArray != null ? confirmedTokenArray.size() : 0;
                    if(confirmedTokenArray != null && tokenSize > 0 && isTokenMarket){
                        confirmedObject.remove("tokens");
                        
                        JsonArray udpatedTokenArray = new JsonArray();

                        for (int i = 0; i < tokenSize ; i++) {
                            JsonElement tokenElement = confirmedTokenArray.get(i);

                            JsonObject tokenObject = tokenElement.getAsJsonObject();

                            JsonElement tokenIdElement = tokenObject.get("tokenId");
                            JsonElement amountElement = tokenObject.get("amount");
                            JsonElement decimalsElement = tokenObject.get("decimals");
                            JsonElement nameElement = tokenObject.get("name");
                            JsonElement tokenTypeElement = tokenObject.get("tokenType");

                            String tokenId = tokenIdElement.getAsString();
                            long amount = amountElement.getAsLong();
                            int decimals = decimalsElement.getAsInt();
                            String name = nameElement.getAsString();
                            String tokenType = tokenTypeElement.getAsString();
                            
                            PriceAmount tokenAmount = new PriceAmount(amount, new PriceCurrency(tokenId, name, decimals, tokenType, m_networkType.toString()));
                            PriceQuote tokenQuote = m_addressesData.getTokenQuoteInErg(tokenId);
                            
                            if(tokenQuote != null){
                                
                                tokenObject.add("tokenQuote", tokenQuote.getJsonObject());
                                BigDecimal tokenQuoteErgAmount =  tokenQuote.getQuote().multiply(tokenAmount.getBigDecimalAmount());

                                m_totalErg = m_totalErg.add(tokenQuoteErgAmount);    
                                tokenObject.addProperty("tokenQuoteErgAmount", tokenQuoteErgAmount);
                            
                                if(ergoQuote != null){
                                    
                                    BigDecimal tokenQuoteAmount = ergoQuote.getQuote().multiply(tokenQuoteErgAmount);
                                    m_totalQuote = m_totalQuote.add(tokenQuoteAmount);

                                    tokenObject.addProperty("tokenQuoteAmount", tokenQuoteAmount);
                                    
                                }

                            }

                            udpatedTokenArray.add(tokenObject);

                        }

                        confirmedObject.add("tokens", udpatedTokenArray);
    
                    }
                    confirmedObject.addProperty("totalQuote", m_totalQuote);
                    confirmedObject.addProperty("totalErg", m_totalErg);

                    balanceJson.add("confirmed", confirmedObject);
                }
                
            }     
        }   
        return balanceJson;
    }


    private void setBalance(JsonObject balanceJson){
        long timeStamp = System.currentTimeMillis();
        
        balanceJson.addProperty("address", m_addressString);
        balanceJson.addProperty("timeStamp", timeStamp);
       // String balanceString = balanceJson.toString();
        
        m_balanceString = balanceJson.toString();
        /*try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(NoteConstants.logFile.toPath(), gson.toJson(balanceJson) +"\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/
        
        m_walletData.sendMessage(NoteConstants.UPDATED,timeStamp ,m_addressString,(String) null); 


    }
    
    public JsonObject getBalance(){
        if(m_balanceString != null){
            JsonElement balanceElement = m_jsonParser.parse(m_balanceString);
            if(balanceElement != null && balanceElement.isJsonObject()){
                return balanceElement.getAsJsonObject();
            }
        }
        return null;
    }
    
    public int getApiIndex() {
        return m_apiIndex;
    }


    public Future<?> getAddressTxInfoFile( EventHandler<WorkerStateEvent> onSucceeded){
        return getNetworksData().getIdDataFile(getAddressString(), "txInfo" , NoteConstants.WALLET_NETWORK, ErgoNetwork.NETWORK_ID, onSucceeded);
    }

    public File getAddressTxViewFile() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, InterruptedException, IOException{
        return getNetworksData().getIdDataFileBlocking(getAddressString(), "txView" , NoteConstants.WALLET_NETWORK, ErgoNetwork.NETWORK_ID);
     }
 

    private ExecutorService getExecService(){
        return getErgoNetworkData().getNetworksData().getExecService();
    }

    
    public Future<?> getTransactionViews(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed ){
        JsonObject txNote = NoteConstants.getCmdObject("getTransactionViewsByAddress");
        txNote.addProperty("address", m_addressString);

        JsonElement startIndexElement = note.get("startIndex");
        JsonElement limitElement = note.get("limit");
        JsonElement conciseElement = note.get("concise");
        JsonElement fromHeightElement = note.get("fromHeight");
        JsonElement toHeightElement = note.get("toHeight");

        if(startIndexElement != null){
            txNote.add("startIndex", startIndexElement);
        }
        if(limitElement != null){
            txNote.add("limit",limitElement);
        }
        if(conciseElement != null){
            txNote.add("concise", conciseElement);
        }
        if(fromHeightElement != null){
            txNote.add("fromHeight", fromHeightElement);
        }
        if(toHeightElement != null){
            txNote.add("toHeight", toHeightElement);
        }

        return getErgoNetworkData().getErgoExplorers().sendNote(txNote, onTxViews->{
            Object obj = onTxViews.getSource().getValue();
            if(obj != null && obj instanceof JsonObject){
                parseTxViews((JsonObject) obj, onSucceeded, onFailed);
            }else{
                Utils.returnException("Returned null", getExecService(), onFailed);
            }
        }, onFailed);

    }

    private void parseTxViews(JsonObject json, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed ){

        getAddressTxInfoFile(onTxFile->{
            Object obj = onTxFile.getSource().getValue();
            if(obj != null && obj instanceof File){
                File readFile = (File) obj;
                File writeFile =  new File(readFile.getAbsolutePath() + "tmp");
     
                if(readFile != null && readFile.isFile() && readFile.length() > 12 ){
                    
                    JsonElement txsElement = json != null ? json.remove("txs") : null;

                    if(txsElement != null && txsElement.isJsonArray()){
                
                        JsonArray txsArray = txsElement.getAsJsonArray();
                        ArrayList<ErgoTransactionView> txViews = new ArrayList<>();
                    
                        while( txsArray.size() > 0){
                            JsonElement txViewElement = txsArray.remove(0);
                            txViews.add(new ErgoTransactionView(getAddressString(), txViewElement.getAsJsonObject(),  true));
                        }                            
                        
                        PipedOutputStream pipedReaderOutput = new PipedOutputStream();            
                        getNetworksData().readEncryptedFile(readFile, m_isAquired, pipedReaderOutput, onRead->{
                            try {
                                pipedReaderOutput.close();
                            } catch (IOException e) {

                            }
                        }, onReadFailed->{
                            try {
                                pipedReaderOutput.close();
                            } catch (IOException e) {

                            }
                        });
                            
                        try{
                
                            PipedInputStream pipedReaderInput = new PipedInputStream(pipedReaderOutput, Utils.DEFAULT_BUFFER_SIZE);
                            JsonReader jsonReader = new JsonReader(new InputStreamReader(pipedReaderInput));
                            
                            PipedOutputStream pipedWriterOutput = new PipedOutputStream();
                            JsonWriter writer = new JsonWriter(new OutputStreamWriter(pipedWriterOutput));
                            
                            updateTxViews(jsonReader, writer, txViews, onSucceeded, updateTxViewsFailed->{
                                try{
                                    writer.close();
                                }catch(IOException e){
                                    try {
                                        Files.writeString(NoteConstants.logFile.toPath(), e.toString() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                    } catch (IOException e1) {
          
                                    }
                                }
                                Throwable throwable = updateTxViewsFailed.getSource().getException();
                                Exception ex = throwable != null && throwable instanceof Exception ? (Exception) throwable : null;
                                String msg = ex != null ? ex.toString() : "readUpdateTxInfo unknown error";
                                try {
                                    Files.writeString(NoteConstants.logFile.toPath(), msg + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                } catch (IOException e) {
    
                                }

                            });

                            PipedInputStream pipedWriterInput = new PipedInputStream(pipedWriterOutput, Utils.DEFAULT_BUFFER_SIZE);
                            getNetworksData().writeEncryptedFile( writeFile, readFile, m_isAquired, pipedWriterInput);

                        }catch(IOException e){
                            if(m_isAquired.get()){
                                m_isAquired.set(false);
                                getNetworksData().releaseDataSemaphore();   
                                Utils.returnException(e, getExecService(), onFailed);
                            }
                        }


                    }else{
                        Utils.returnObject(json, getExecService(), onSucceeded);
                    }

        
                }else{
                    Utils.returnObject(json, getExecService(), onSucceeded);
                }
            }else{
                Utils.returnObject(json, getExecService(), onSucceeded);
            }
        });
            
     
    }

    private Future<?> updateTxViews(JsonReader jsonReader, JsonWriter writer, ArrayList<ErgoTransactionView> txViews, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onfailed){
        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() throws IOException {
                jsonReader.beginObject();
                writer.beginObject();
                while(jsonReader.hasNext()){
                        switch(jsonReader.nextName()){
                            case "data":
                                writer.name("data");  
                                writer.beginArray();
                                jsonReader.beginArray();
                                while(jsonReader.hasNext()){
                                        ErgoTxInfo txInfo = new ErgoTxInfo(jsonReader);
                                        if(txInfo.getBoxSize() > 0){
                                            ErgoTransactionView txView = findTxViewInfo(txInfo.getTxId(), txViews);
                                            ErgoBoxInfo[] boxInfoArray = txInfo.getBoxInfoArray();
                                            for(int i = 0; i < boxInfoArray.length ; i++){
                                                ErgoBoxInfo boxInfo = boxInfoArray[i];
                                                ErgoTransactionView boxTxView = findTxViewInfo( boxInfo.getTxId(), txViews);
                                                if(boxTxView != null){
                                                    boxInfo.setStatus(boxTxView.getStatus());
                                                }
                                            }
                                            txInfo.setBoxInfoArray(boxInfoArray);
                                            if(txView != null){
                                                txView.setTxInfo(txInfo);
                                            }
                                            txInfo.writeJson(writer);
                                        }
                                }
                                jsonReader.endArray();
                                writer.endArray();
                            break;
                            default:
                                jsonReader.skipValue();
                        }
                }
                jsonReader.endObject();
                writer.endObject();
                writer.flush();
                writer.close();

                JsonObject json = new JsonObject();
                JsonArray jsonArray = new JsonArray();
                while(txViews.size() > 0){
                    jsonArray.add(txViews.remove(0).getJsonObject());
                }
                json.add("txs", jsonArray);
                return json;
            }
        };

        task.setOnSucceeded(onSucceeded);
        task.setOnFailed(onfailed);

        return getExecService().submit(task);
    }

    

     public static ErgoTransactionView findTxViewInfo(String txId, ArrayList<ErgoTransactionView> txViews){
          for(int i = 0; i < txViews.size(); i++){
               ErgoTransactionView txView = txViews.get(i);
               if(txView.getId().equals(txId)){
                    return txView;
               }
          }

          return null;
     }



     public Future<?> updateBoxInfo(JsonObject dataObject, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){

        JsonElement txIdElement = dataObject.get("txId");
        JsonElement boxInfoElement = dataObject.get("boxInfo");
        if(boxInfoElement != null && boxInfoElement.isJsonObject() && txIdElement != null && !txIdElement.isJsonNull()){
            ErgoBoxInfo boxInfo = new ErgoBoxInfo(boxInfoElement.getAsJsonObject());

            return updateBoxInfo(txIdElement.getAsString(), boxInfo, onSucceeded, onFailed) ; 
        }
        return null;
     }


    private Future<?> updateBoxInfo(String txId, ErgoBoxInfo boxInfo,  EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
     
        return getAddressTxInfoFile(onTxFile->{
            Object obj = onTxFile.getSource().getValue();
            if(obj != null && obj instanceof File){
                File readFile = (File) obj;
                File writeFile =  new File(readFile.getAbsolutePath() + "tmp");

                if(readFile.length() < 12){
                    PipedOutputStream pipedReaderOutput = new PipedOutputStream();            

                    getNetworksData().readEncryptedFile(readFile, m_isAquired, pipedReaderOutput, onRead->{
                        try {
                            pipedReaderOutput.close();
                        } catch (IOException e) {

                        }
                    }, onReadFailed->{
                        try {
                            pipedReaderOutput.close();
                        } catch (IOException e) {

                        }
                    });

                    try{
            
                        PipedInputStream pipedReaderInput = new PipedInputStream(pipedReaderOutput, Utils.DEFAULT_BUFFER_SIZE);
                        JsonReader jsonReader = new JsonReader(new InputStreamReader(pipedReaderInput));
                        
                        PipedOutputStream pipedWriterOutput = new PipedOutputStream();
                        JsonWriter writer = new JsonWriter(new OutputStreamWriter(pipedWriterOutput));
                        
                        readUpdateTxInfo(jsonReader, writer, txId, boxInfo, onSucceeded, onReadUpdateFailed->{
                            if(m_isAquired.get()){
                                m_isAquired.set(false);
                                getNetworksData().releaseDataSemaphore();
                            }
                            try {
                                writer.close();
                            } catch (IOException e) {
                                try {
                                    Files.writeString(NoteConstants.logFile.toPath(), e.toString() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                } catch (IOException e1) {
      
                                }
                            }
                            Throwable throwable = onReadUpdateFailed.getSource().getException();
                            Exception ex = throwable != null && throwable instanceof Exception ? (Exception) throwable : null;
                            String msg = ex != null ? ex.toString() : "readUpdateTxInfo unknown error";
                            try {
                                Files.writeString(NoteConstants.logFile.toPath(), msg + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e) {
  
                            }

                        });

                        PipedInputStream pipedWriterInput = new PipedInputStream(pipedWriterOutput, Utils.DEFAULT_BUFFER_SIZE);
                        getNetworksData().writeEncryptedFile( writeFile, readFile, m_isAquired, pipedWriterInput);

                    }catch(IOException e){
                        if(m_isAquired.get()){
                            m_isAquired.set(false);
                            getNetworksData().releaseDataSemaphore();
                            Utils.returnException(e, getExecService(), onFailed);
                        }
                    }
                }else{
                    ErgoTxInfo newTxInfo = new ErgoTxInfo(txId, System.currentTimeMillis(), new ErgoBoxInfo[] {boxInfo});
                    JsonObject json = new JsonObject();
                    JsonArray jsonArray = new JsonArray();
                    jsonArray.add(newTxInfo.getJsonObject());
                    json.add("data", jsonArray);
                    saveTxInfoData(json);
                }
            }else{
                Utils.returnException("Unable to aquire data file", getExecService(), onFailed);
            }
        }); 
        

    }

    private void saveTxInfoData(JsonObject txInfoData){
        getNetworksData().save(getAddressString(), "txInfo" , NoteConstants.WALLET_NETWORK, ErgoNetwork.NETWORK_ID, txInfoData);
    }

    private Future<?> readUpdateTxInfo(JsonReader reader, JsonWriter writer,String txId, ErgoBoxInfo boxInfo, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() throws IOException {
                SimpleObjectProperty<ErgoTxInfo> foundTxInfo = new SimpleObjectProperty<>(null);
                reader.beginObject();
                writer.beginObject();
                while(reader.hasNext()){
                    switch(reader.nextName()){
                        case "data":
                            writer.name("data");
                            writer.beginArray();
                            reader.beginArray();
                        
                            while(reader.hasNext()){
                                ErgoTxInfo txInfo = new ErgoTxInfo(reader);
                                if(txInfo.getTxId().equals(txId)){
                                    txInfo.updateBoxInfo(boxInfo);
                                    foundTxInfo.set(txInfo);
                                    txInfo.writeJson(writer);                
                                }else{
                                    txInfo.writeJson(writer);
                                }
                            }
                            if(foundTxInfo.get() == null){
                                ErgoTxInfo newTxInfo = new ErgoTxInfo(txId, System.currentTimeMillis(), new ErgoBoxInfo[] {boxInfo});
                                foundTxInfo.set(newTxInfo);
                                newTxInfo.writeJson(writer);
                            }
                            reader.endArray();
                            writer.endArray();
                        break;
                        default:
                            reader.skipValue();
                    }
                }
                reader.endObject();
                writer.endObject();
                writer.flush();

                return foundTxInfo.get().getJsonObject();
            }
        };

        task.setOnSucceeded(onSucceeded);
        task.setOnFailed(onFailed);

        return getExecService().submit(task);
    }

    @Override
    public String toString() {
  
        return getName();
    }

    private InputBox[] getRefundBoxesToSpend(Wallet wallet, InputBox refundBox, NetworkType networkType, BlockchainContext ctx){
        if(refundBox.getValue() < NoteConstants.MIN_NANO_ERGS){
           
            List<InputBox> boxesToSpend = BoxOperations.createForSenders(wallet.addressStream(networkType).toList(), ctx)
                    .withFeeAmount(NoteConstants.MIN_NANO_ERGS - refundBox.getValue())
                    .loadTop();
                    boxesToSpend.add(refundBox);
            return boxesToSpend.toArray(new InputBox[boxesToSpend.size()]);
        }else{
            return new InputBox[]{refundBox};
        }

    }


    public static byte[] getWalletAddressPropBytes(Wallet wallet, String addressString, NetworkType networkType)throws Exception{
        Address address = getWalletAddress(wallet, addressString, networkType);
        if(address != null){
            return address.toPropositionBytes();
        }
        throw new Exception("Wallet does not contain address: " + address );
    }

    public static byte[] getAddressPropBytes(String addressString){
        
        return Address.create(addressString).toPropositionBytes();
        
    }


    public static Address getWalletAddress(Wallet wallet, String addressString, NetworkType networkType){
        SimpleObjectProperty<Address> resultAddress = new SimpleObjectProperty<>(null);
        wallet.myAddresses.forEach((index, name) -> {                    
            try {
                Address address = wallet.publicAddress(networkType, index);
                if(address.toString().equals(addressString)){
                   resultAddress.set(address);
                }
            } catch (Failure e) {
       
            }
        });

        return resultAddress.get();
    }


    public Future<?> reclaimBox( 
        Wallet wallet, 
        String nodeUrl, 
        String nodeApiKey, 
        String explorerUrl, 
        NetworkType networkType,
        String parentTxId,
        String boxId,
        String pass,
        String locationId,
        EventHandler<WorkerStateEvent> onSucceeded,
        EventHandler<WorkerStateEvent> onFailed)
    {
        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() throws Exception {
                Address address = getWalletAddress(wallet, getAddressString(), networkType);
                ErgoClient ergoClient = RestApiErgoClient.create(nodeUrl, networkType, nodeApiKey, explorerUrl);
                UnsignedTransaction unsignedTx = ergoClient.execute(ctx -> {
                    InputBox boxToCancel = ctx.getBoxesById(boxId)[0];
                    InputBox[] inputBoxes =  getRefundBoxesToSpend(wallet, boxToCancel, networkType, ctx);
                    UnsignedTransactionBuilder txBuilder = ctx.newTxBuilder();
                    OutBoxBuilder newBoxBuilder = txBuilder.outBoxBuilder();
                    newBoxBuilder.value(boxToCancel.getValue() - NoteConstants.MIN_NANO_ERGS);
                    int tokenSize = boxToCancel.getTokens().size();
                    if (tokenSize > 0) {
                        newBoxBuilder.tokens(boxToCancel.getTokens().toArray(new ErgoToken[tokenSize]));
                    }
                    newBoxBuilder.contract(ctx.compileContract(ConstantsBuilder.create()
                            .item("refundPk", address.getPublicKey())
                            .build(), "{ refundPk }")).build();
                    OutBox outBox = newBoxBuilder.build();
                    return txBuilder
                    .addInputs(inputBoxes).addOutputs(outBox)
                    .fee(NoteConstants.MIN_NANO_ERGS)
                    .sendChangeTo(address)
                    .build();
                });
          
                String txId = wallet.transact(ergoClient, ergoClient.execute(ctx -> {
                    try {
                        return wallet.key().signWithPassword(pass, ctx, unsignedTx, wallet.myAddresses.keySet());
                    } catch (WalletKey.Failure ex) {

                        return null;
                    }
                }));

                if(txId != null){
                    return new ErgoBoxInfo(boxId, TransactionStatus.PENDING, txId, System.currentTimeMillis());
                    
                }else{
                    throw new Exception("Transaction signing failed");
                }
                
            }
        };

        task.setOnSucceeded((txComplete)->{
            Object obj = txComplete.getSource().getValue();
            if(obj != null && obj instanceof ErgoBoxInfo){
                updateBoxInfo(parentTxId, (ErgoBoxInfo) obj, onSucceeded, onFailed);
            }else{
                Utils.returnException("Returned null", getExecService(), onFailed);
            }
        });
        task.setOnFailed(onFailed);

        return getExecService().submit(task);
    }

    
    
    public Future<?> sendAssets(ExecutorService execService, 
        Wallet wallet, 
        String nodeUrl, 
        String nodeApiKey, 
        String explorerUrl, 
        NetworkType networkType, 
        AddressInformation recipientAddressInfo, 
        long amountToSpendNanoErgs,
        long feeAmountNanoErgs,
        PriceAmount[] priceAmounts,
        String pass,
        EventHandler<WorkerStateEvent> onSucceeded,
        EventHandler<WorkerStateEvent> onFailed)
    {
        Task<JsonObject> task = new Task<JsonObject>() {
            @Override
            public JsonObject call() throws Exception {

           
                Address address = getWalletAddress(wallet, getAddressString(), networkType);
        
                ErgoClient ergoClient = RestApiErgoClient.create(nodeUrl, networkType, nodeApiKey, explorerUrl);

                ErgoToken[] tokenArray = getTokenArrayFromPriceAmounts(priceAmounts);

                UnsignedTransaction unsignedTx = ErgoInterface.createUnsignedTransaction(
                    ergoClient,
                    wallet.addressStream(networkType).toList(),
                    recipientAddressInfo.getAddress(),
                    amountToSpendNanoErgs,
                    feeAmountNanoErgs,
                    address,
                    tokenArray
                );
          
                String txId = wallet.transact(ergoClient, ergoClient.execute(ctx -> {
                    try {
                        return wallet.key().signWithPassword(pass, ctx, unsignedTx, wallet.myAddresses.keySet());
                    } catch (WalletKey.Failure ex) {

                        return null;
                    }
                }));


                if(txId != null){

                    JsonObject resultObject = new JsonObject();
                    resultObject.addProperty("result","Sent");
                    resultObject.addProperty("txId", txId);
                    resultObject.addProperty("timeStamp", System.currentTimeMillis());

                    try{
                    
                        BlockchainDataSource dataSource = ergoClient != null ? ergoClient.getDataSource() : null;
                        List<BlockHeader> blockHeaderList =  dataSource != null ? dataSource.getLastBlockHeaders(1, true) : null;
                        BlockHeader blockHeader = blockHeaderList != null && blockHeaderList.size() > 0 ? blockHeaderList.get(0)  : null;
                        
                        int blockHeight = blockHeader != null ? blockHeader.getHeight() : -1;
                        long timeStamp = blockHeader != null ? blockHeader.getTimestamp() : -1;

                        JsonObject networkInfoObject = new JsonObject();
                        networkInfoObject.addProperty("networkHeight", blockHeight);
                        networkInfoObject.addProperty("timeStamp", timeStamp);
                        resultObject.add("networkInfo", networkInfoObject);
                    }catch(Exception dataSourcException){
         
                    }
                    /*
                    ErgoTransactionPartner senderPartner = new ErgoTransactionPartner(getAddressString(), PartnerType.SENDER, amountToSpendNanoErgs, priceAmounts);
                    ErgoTransactionPartner receiverPartner = new ErgoTransactionPartner(recipientAddressInfo.getAddressString(), PartnerType.RECEIVER, amountToSpendNanoErgs, priceAmounts);
                    ErgoTransactionPartner minerPartner = new ErgoTransactionPartner(ErgoTransactionPartner.PENDING_ADDRESS, PartnerType.MINER, feeAmountNanoErgs);
                    ErgoTransactionView txView = new ErgoTransactionView(txId, getAddressString(),new ErgoTransactionPartner[]{senderPartner, receiverPartner, minerPartner});
                     */
                    return resultObject;
                }else{
                    throw new Exception("Transaction signing failed");
                }
                
            }
        };

        task.setOnSucceeded(onSucceeded);
        task.setOnFailed(onFailed);

        return execService.submit(task);
    }

    public static ErgoToken[] getTokenArrayFromPriceAmounts(PriceAmount[] amounts){
        if(amounts == null){
            return new ErgoToken[0];
        }
        ErgoToken[] tokens = new ErgoToken[amounts.length];
        for(int i = 0; i < amounts.length ; i++){
            tokens[i] = amounts[i].getErgoToken();
        }
        return tokens;
    }


    /*
    public Future<?> executeExchange( 
        Wallet wallet, 
        String nodeUrl, 
        String nodeApiKey, 
        String explorerUrl, 
        NetworkType networkType,
        String parentTxId,
        String sellerBoxId,
        Address sellerAddress,
        String buyerBoxId,
        Address buyerAddress,
        String pass,
        String locationId,
        EventHandler<WorkerStateEvent> onSucceeded,
        EventHandler<WorkerStateEvent> onFailed)
    {
        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() throws Exception {
                Address address = NoteConstants.getWalletAddress(wallet, getAddressString(), networkType);
                ErgoClient ergoClient = RestApiErgoClient.create(nodeUrl, networkType, nodeApiKey, explorerUrl);
                UnsignedTransaction unsignedTx = ergoClient.execute(ctx -> {
                    InputBox sellerBox = ctx.getBoxesById(sellerBoxId)[0];
                    ErgoToken sellerTokens = sellerBox.getTokens().get(0);
                    InputBox buyerBox = ctx.getBoxesById(buyerBoxId)[0];
                    long buyerNanoErgs = buyerBox.getValue();
                    
          

                    InputBox[] inputBoxes =  getRefundBoxesToSpend(wallet, boxToCancel, networkType, ctx);
                    UnsignedTransactionBuilder txBuilder = ctx.newTxBuilder();
                    OutBoxBuilder newBoxBuilder = txBuilder.outBoxBuilder();
                    newBoxBuilder.value(boxToCancel.getValue() - ErgoNetwork.MIN_NANO_ERGS);
                    int tokenSize = boxToCancel.getTokens().size();
                    if (tokenSize > 0) {
                        newBoxBuilder.tokens(boxToCancel.getTokens().toArray(new ErgoToken[tokenSize]));
                    }
                    newBoxBuilder.contract(ctx.compileContract(ConstantsBuilder.create()
                            .item("refundPk", address.getPublicKey())
                            .build(), "{ refundPk }")).build();
                    OutBox outBox = newBoxBuilder.build();
                    return txBuilder
                    .addInputs(inputBoxes).addOutputs(outBox)
                    .fee(ErgoNetwork.MIN_NANO_ERGS)
                    .sendChangeTo(address)
                    .build();
                });
          
                String txId = wallet.transact(ergoClient, ergoClient.execute(ctx -> {
                    try {
                        return wallet.key().signWithPassword(pass, ctx, unsignedTx, wallet.myAddresses.keySet());
                    } catch (WalletKey.Failure ex) {

                        return null;
                    }
                }));

                if(txId != null){
                    return new ErgoBoxInfo(boxId, TransactionStatus.PENDING, txId, System.currentTimeMillis());
                    
                }else{
                    throw new Exception("Transaction signing failed");
                }
                
            }
        };

        task.setOnSucceeded((txComplete)->{
            Object obj = txComplete.getSource().getValue();
            if(obj != null && obj instanceof ErgoBoxInfo){
                updateBoxInfo(parentTxId, (ErgoBoxInfo) obj, onSucceeded, onFailed);
            }else{
                Utils.returnException("Returned null", getExecService(), onFailed);
            }
        });
        task.setOnFailed(onFailed);

        return getExecService().submit(task);
    }*/


}
