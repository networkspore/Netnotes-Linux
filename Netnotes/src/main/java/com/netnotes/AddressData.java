package com.netnotes;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.utils.Utils;
import java.io.IOException;
import java.math.BigDecimal;
import org.ergoplatform.appkit.Address;

public class AddressData extends Network {  
    
    private int m_index;

    //private final ArrayList<ErgoTransaction> m_watchedTransactions = new ArrayList<>();

    private String m_balanceString = null;
    private JsonParser m_jsonParser = new JsonParser();
  /* 
    private final SimpleObjectProperty<ErgoTransaction> m_selectedTransaction = new SimpleObjectProperty<>(null);
    
 */
    private ErgoWalletData m_walletData;
    private int m_apiIndex = 0;
    
    private final String m_addressString;
    private final NetworkType m_networkType;
    private AddressesData m_addressesData = null;

   
    
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
       
        JsonObject note = Utils.getCmdObject("getBalance");
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
                Files.writeString(App.logFile.toPath(), "AddressData, Explorer failed update: " + failed.getSource().getException().toString() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                
                
            }
        });
    }


 
  

    public ErgoTransaction[] getTxArray(JsonObject json){
        
        if(json != null){
            
            JsonElement itemsElement = json.get("items");

            if(itemsElement != null && itemsElement.isJsonArray()){
                JsonArray itemsArray = itemsElement.getAsJsonArray();
                int size = itemsArray.size();
                ErgoTransaction[] ergTxs = new ErgoTransaction[size];

                for(int i = 0; i < size ; i ++){
                    JsonElement txElement = itemsArray.get(i);
                    if(txElement != null && txElement.isJsonObject()){
                        JsonObject txObject = txElement.getAsJsonObject();

                        JsonElement txIdElement = txObject.get("id");
                        if(txIdElement != null && txIdElement.isJsonPrimitive()){
                            String txId = txIdElement.getAsString();
            
                            ergTxs[i] = new ErgoTransaction(txId, this, txObject);
                        }
                    }
                }
                return ergTxs;
            }

        }

        return new ErgoTransaction[0];
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
                                                Files.writeString(App.logFile.toPath(), "\nCould not read tx json: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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
            Files.writeString(App.logFile.toPath(), gson.toJson(balanceJson) +"\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/
        
        m_walletData.sendMessage(App.UPDATED,timeStamp ,m_addressString,(String) null); 


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

   




   /* public void updateWatchedTxs(ErgoExplorerData explorerData, JsonObject json){
        
        if(json != null){
            JsonElement itemsElement = json.get("items");

            if(itemsElement != null && itemsElement.isJsonArray()){
                JsonArray itemsArray = itemsElement.getAsJsonArray();
                int size = itemsArray.size();

                SimpleBooleanProperty found = new SimpleBooleanProperty(false);

                for(int i = 0; i < size ; i ++){
                    JsonElement txElement = itemsArray.get(i);
                    if(txElement != null && txElement.isJsonObject()){
                        JsonObject txObject = txElement.getAsJsonObject();

                        JsonElement txIdElement = txObject.get("id");
                        if(txIdElement != null && txIdElement.isJsonPrimitive()){
                           
                            String txId = txIdElement.getAsString();
                            ErgoTransaction ergTx = getWatchedTx(txId);
                            if(ergTx != null){
                                if(!found.get()){
                                    found.set(true);
                                }
                                ergTx.update(txObject);
                            }
                        }
                    }
                }

           
               
            }
        
        }
    }*/

    

    public int getApiIndex() {
        return m_apiIndex;
    }


    @Override
    public String toString() {
  
        return getName();
    }

}
