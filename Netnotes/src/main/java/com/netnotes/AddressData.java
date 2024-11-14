package com.netnotes;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import java.util.ArrayList;
import java.util.Collections;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netnotes.ErgoTransaction.TransactionType;
import com.utils.Utils;

import javafx.beans.property.SimpleBooleanProperty;
import java.io.IOException;


public class AddressData extends Network {  
    
   // public final static int UPDATE_LIMIT = 10;

    private int m_index;
    //private String m_addressString;

    //private final ArrayList<ErgoTransaction> m_watchedTransactions = new ArrayList<>();

    private JsonObject m_balanceJson = null;
   
  /*  private final ErgoAmount m_ergoAmount;
    private final PriceAmount m_unconfirmedAmount;

    private final ObservableList<PriceAmount> m_confirmedTokensList = FXCollections.observableArrayList();

    
    private final SimpleObjectProperty<ErgoTransaction> m_selectedTransaction = new SimpleObjectProperty<>(null);
    
    private ArrayList<PriceAmount> m_unconfirmedTokensList = new ArrayList<>(); */
    private ErgoWalletData m_walletData;
    private int m_apiIndex = 0;
    
   // private SimpleObjectProperty<WritableImage> m_imgBuffer = new SimpleObjectProperty<WritableImage>(null);
    private final String m_addressString;
    private final NetworkType m_networkType;
  //  private Wallet m_wallet = null;

  // private BufferedImage m_img = null;
   // private Graphics2D m_g2d = null;

   // private java.awt.Font m_imgFont = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 30);
 //   private java.awt.Font m_imgSmallFont = new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 12);
    
 //   private BufferedImage m_unitImage = null;

   
    
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
        json.addProperty("index", m_index);
        json.addProperty("address", m_addressString);

        return json;
    }


    /*public void getAddressInfo(){
        
        
            
        JsonObject json = getNetworksData().getData(m_addressString, m_addressesData.getWalletData().getNetworkId(), ErgoWallets.NETWORK_ID, ErgoNetwork.NETWORK_ID);
        
        if(json != null){
            
            openAddressJson(json);
            
        }
        
    }*/

    public void updateBalance() {
       
        JsonObject note = Utils.getCmdObject("getBalance");
        note.addProperty("address", m_addressString);

        getErgoNetworkData().getErgoExplorers().sendNote(note, success -> {
            Object sourceObject = success.getSource().getValue();

            if (sourceObject != null) {
                JsonObject jsonObject = (JsonObject) sourceObject;

                setBalance(jsonObject); 
                
            }},
        failed -> {

            try {
                Files.writeString(App.logFile.toPath(), "\nAddressData, Explorer failed update: " + failed.getSource().getException().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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

    public void getErgoQuote(String interfaceId){
        
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
        json.add("balance", m_balanceJson);
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

    /*public BigDecimal getConfirmedAmount() {
        return ErgoInterface.toFullErg(getConfirmedNanoErgs());
    }*/

    public NetworkType getNetworkType() {
        return m_networkType;
    }


    
    /*public BigDecimal getTotalTokenErgBigDecimal(){
        int tokenListSize = m_confirmedTokensList.size();
       
        SimpleObjectProperty<BigDecimal> total = new SimpleObjectProperty<>(BigDecimal.ZERO);

        PriceAmount[] tokenAmounts = new PriceAmount[tokenListSize];
        tokenAmounts = m_confirmedTokensList.toArray(tokenAmounts);

        for(int i = 0; i < tokenListSize ; i++){
            PriceAmount priceAmount = tokenAmounts[i];
          
            PriceQuote priceQuote =  priceAmount.priceQuoteProperty().get();
            
            BigDecimal priceBigDecimal = priceQuote != null ? priceQuote.getInvertedAmount() : null;

            BigDecimal amountBigDecimal = priceQuote != null ? priceAmount.amountProperty().get() : null;
            BigDecimal tokenErgs = priceBigDecimal != null && amountBigDecimal != null ? priceBigDecimal.multiply(amountBigDecimal) : BigDecimal.ZERO;
        
        
            total.set(total.get().add(tokenErgs));
            
        }
 
        return total.get();
    }*/
    
    /*
    public ArrayList<PriceAmount> getUnconfirmedTokenList() {
        return m_unconfirmedTokensList;
    }

  

    public BigDecimal getFullAmountUnconfirmed() {
        return m_unconfirmedAmount.amountProperty().get();
    }

    

    public BigDecimal getTotalAmountPrice() {
        return getErgoAmount().amountProperty().get().multiply( m_addressesData.getPrice());
    }



   

    public BigInteger getAmountInt() {
        return  getErgoAmount().amountProperty().get().toBigInteger();
    }

    public BigDecimal getAmountDecimalPosition() {
        return getErgoAmount().amountProperty().get().subtract(new BigDecimal(getAmountInt()));
    }

    public Image getUnitImage() {
   
        if (m_ergoAmount == null) {
            return new Image("/assets/unknown-unit.png");
        } else {
            return m_ergoAmount.getCurrency().getIcon();
        }
    }
 
 

   

    public SimpleObjectProperty<WritableImage> getImage() {
        return m_imgBuffer;
    }*?


    public PriceAmount getConfirmedTokenAmount(String tokenId){
        
        for(int i = 0; i < m_confirmedTokensList.size(); i++)
        {
            PriceAmount priceAmount = m_confirmedTokensList.get(i);

            if(priceAmount.getTokenId().equals(tokenId)){
                return priceAmount;
            }
        }
        return null;
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


    private void setBalance(JsonObject jsonObject){
        long timeStamp = System.currentTimeMillis();
        m_balanceJson = jsonObject;
    
        m_balanceJson.addProperty("networkId", m_addressString);
        m_balanceJson.addProperty("timeStamp", timeStamp);

        String balanceString = m_balanceJson.toString();
    

        m_walletData.sendMessage(App.UPDATED,timeStamp ,m_addressString, balanceString); 


    }
    
    public JsonObject getBalance(){
        return m_balanceJson;
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

    /* Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String prettyString = gson.toJson(json); */

    /*public void saveAddresInfo(){
       

        JsonObject json = getJsonObject();    
        
        getNetworksData().save(m_addressString, m_addressesData.getWalletData().getNetworkId(), ErgoWallets.NETWORK_ID, ErgoNetwork.NETWORK_ID, json);
        
        
    }*/
     

    public int getApiIndex() {
        return m_apiIndex;
    }


    @Override
    public String toString() {
  
        return getName();
    }

    @Override
    public void shutdown(){
       
      //  m_img = null;
        super.shutdown();
    }
}

/*
    public void update(){
      //  updateBufferedImage();
      
    }
    
    

     public void updateBufferedImage() {
        
        long timeStamp = System.currentTimeMillis();

        ErgoAmount priceAmount = getErgoAmount();
        boolean quantityValid = priceAmount.amountProperty().get() != null &&  priceAmount.getTimeStamp() > 0 && (timeStamp - priceAmount.getTimeStamp()) < AddressesData.QUOTE_TIMEOUT; 
        double priceAmountDouble = quantityValid ? priceAmount.amountProperty().get().doubleValue() : 0;

        PriceQuote priceQuote = quantityValid ? priceAmount.priceQuoteProperty().get() : null;
        boolean priceValid = priceQuote != null && priceQuote.getTimeStamp() > 0 && (timeStamp - priceQuote.getTimeStamp()) <  AddressesData.QUOTE_TIMEOUT;
        double priceQuoteDouble = priceValid  && priceQuote != null ? priceQuote.getDoubleAmount() : 0;
        
        String totalPrice = priceValid && priceQuote != null ? Utils.formatCryptoString( priceQuoteDouble * priceAmountDouble, priceQuote.getQuoteCurrency(), priceQuote.getFractionalPrecision(),  quantityValid && priceValid) : " -.--";
        int integers = priceAmount != null ? (int) priceAmount.getDoubleAmount() : 0;
        double decimals = priceAmount != null ? priceAmount.getDoubleAmount() - integers : 0;
        
        PriceCurrency priceCurrency = priceAmount.getCurrency();

        int decimalPlaces = priceAmount != null ? priceCurrency.getFractionalPrecision() : 0;
        String cryptoName = priceAmount != null ? priceCurrency.getSymbol() : "UKNOWN";
        int space = cryptoName.indexOf(" ");
        cryptoName = space != -1 ? cryptoName.substring(0, space) : cryptoName;

        String currencyPrice = priceValid && priceQuote != null ? priceQuote.toString() : "-.--";

        

        //   Image ergoBlack25 = new Image("/assets/ergo-black-25.png");
        //   SwingFXUtils.fromFXImage(ergoBlack25, null);
        
        String amountString = quantityValid ? String.format("%d", integers) : " -";
        String decs = String.format("%." + decimalPlaces + "f", decimals);

        decs = quantityValid ? decs.substring(1, decs.length()) : "";
        totalPrice = totalPrice + "   ";
        currencyPrice = "(" + currencyPrice + ")   ";
    
       
        
        
        
        m_unitImage = SwingFXUtils.fromFXImage(priceAmount != null ? priceCurrency.getIcon() : new Image("/assets/unknown-unit.png"), m_unitImage);
        Drawing.setImageAlpha(m_unitImage, 0x40);
        //  adrBuchImg.getScaledInstance(width, height, java.awt.Image.SCALE_AREA_AVERAGING);
       // int width = Math.max( 200 ,(int) m_addressesData.widthProperty().get() - 150);

        if(m_img == null){
            m_img = new BufferedImage(AddressesData.ADDRESS_IMG_WIDTH, AddressesData.ADDRESS_IMG_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            m_g2d = m_img.createGraphics();
            m_g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            m_g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            m_g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            m_g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
            m_g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            m_g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            m_g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            m_g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }else{
            Drawing.fillArea(m_img, App.DEFAULT_RGBA, 0, 0, m_img.getWidth(), m_img.getHeight(), false);
        }
        
        m_g2d.setFont(m_imgFont);
        FontMetrics fm = m_g2d.getFontMetrics();
        int padding = 5;
        int stringWidth = fm.stringWidth(amountString);
       
        int height = fm.getHeight() + 10;

        m_g2d.setFont(m_imgSmallFont);

        fm = m_g2d.getFontMetrics();
        
        int priceWidth = fm.stringWidth(totalPrice);
        int currencyWidth = fm.stringWidth(currencyPrice);
        //int decsWidth = fm.stringWidth(decs);


        int priceLength = (priceWidth > currencyWidth ? priceWidth : currencyWidth);
        
        //  int priceAscent = fm.getAscent();
        int integersX = priceLength + 10;
        integersX = integersX < 130 ? 130 : integersX;
        int decimalsX = integersX + stringWidth ;

       // int cryptoNameStringWidth = fm.stringWidth(cryptoName);
       

        //int width = decimalsX + decsWidth + (padding * 2);
   
        //width = width < m_minImgWidth ? m_minImgWidth : width;

        int cryptoNameStringX = decimalsX + 2;

        //   g2d.setComposite(AlphaComposite.Clear);


        m_g2d.drawImage(m_unitImage,75, (height / 2) - (m_unitImage.getHeight() / 2), m_unitImage.getWidth(), m_unitImage.getHeight(), null);

       



        m_g2d.setFont(m_imgFont);
        fm = m_g2d.getFontMetrics();
        m_g2d.setColor(java.awt.Color.WHITE);

        

        m_g2d.drawString(amountString, integersX, fm.getAscent() + 5);

        m_g2d.setFont(m_imgSmallFont);
        fm = m_g2d.getFontMetrics();
        m_g2d.setColor(new java.awt.Color(.9f, .9f, .9f, .9f));

       
        if(decimalPlaces > 0){
            //decimalsX = widthIncrease > 0 ? decimalsX + widthIncrease : decimalsX;
            m_g2d.drawString(decs, decimalsX, fm.getHeight() + 4);
        }

        
        m_g2d.drawString(cryptoName, cryptoNameStringX, height - 10);

        m_g2d.setFont(m_imgSmallFont);
        m_g2d.setColor(java.awt.Color.WHITE);
        fm = m_g2d.getFontMetrics();
        m_g2d.drawString(totalPrice, padding, fm.getHeight() + 2);

        m_g2d.setColor(new java.awt.Color(.6f, .6f, .6f, .9f));
        m_g2d.drawString(currencyPrice, padding, height - 10);

     
        getImage().set(SwingFXUtils.toFXImage(m_img, getImage().get()));

  
    }*/