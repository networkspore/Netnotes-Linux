package com.netnotes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import com.utils.Utils;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import ove.crypto.digest.Blake2b;

public class ErgoTokensList  {

    public final static String NETWORK_ID = "ERGO_TOKENS_LIST";


    private ArrayList<ErgoTokenData> m_dataList = new ArrayList<>();

    private ErgoTokens m_ergoTokens = null;
    private final SimpleObjectProperty<ErgoTokenData> m_selectedNetworkToken = new SimpleObjectProperty<>(null);

    private final SimpleObjectProperty<ErgoExplorerList> m_ergoExplorersList = new SimpleObjectProperty<>(null);

    private ErgoToken m_ergoToken = null; 

    private File m_tokensDir;
    public ErgoTokensList(ErgoTokens ergoTokens) {
       
        m_ergoTokens = ergoTokens;
        setup();
    }

    private void setup(){
        m_tokensDir = new File(m_ergoTokens.getErgoNetworkData().getErgoNetwork().getAppDir().getAbsolutePath() + "/tokens");
        if(!m_tokensDir.isDirectory()){
            try {
                Files.createDirectories(m_tokensDir.toPath());
            
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }
        }

        JsonObject json = m_ergoTokens.getNetworksData().getData("tokenList", m_ergoTokens.getNetworkType().toString(), ErgoTokens.NETWORK_ID, ErgoNetwork.NETWORK_ID);      
        JsonElement jsonArrayElement = json != null ? json.get("data") : null;

        JsonArray jsonArray = jsonArrayElement != null && jsonArrayElement.isJsonArray() ? jsonArrayElement.getAsJsonArray() : null;

        if (!m_tokensDir.isDirectory() || jsonArray == null) {
            

         
         
            try(
                InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("assets/ergoTokenIcons.zip");
                ZipInputStream zipStream = new ZipInputStream(is);
            ) {

                
                final Blake2b digest = Blake2b.Digest.newInstance(32);
            
                String tokensPathString = m_tokensDir.getAbsolutePath();

                if (zipStream != null) {
                    // Enumeration<? extends ZipEntry> entries = zipFile.entries();
                  
                    ZipEntry entry;
                    while ((entry = zipStream.getNextEntry()) != null) {

                        String entryName = entry.getName();

                        int indexOfDir = entryName.lastIndexOf("/");

                        if (indexOfDir != entryName.length() - 1) {

                            int indexOfExt = entryName.lastIndexOf(".");

                            String fileName = entryName.substring(0, indexOfExt);

                            File newDirFile = new File(tokensPathString + "/" + fileName);

                            String fileString = tokensPathString + "/" + fileName + "/" + entryName;
                            File entryFile = new File(fileString);

                            boolean isDirectory = newDirFile.isDirectory();
                          
                            if (!isDirectory || (isDirectory && !entryFile.isFile())) {
                                Files.createDirectory(newDirFile.toPath());
                          

                                
                                OutputStream outStream = null;
                                try {
                                    
                                
                                    outStream = new FileOutputStream(entryFile);
                                    //outStream.write(buffer);

                                    byte[] buffer = new byte[8 * 1024];
                                    int bytesRead;
                                    while ((bytesRead = zipStream.read(buffer)) != -1) {

                                        outStream.write(buffer, 0, bytesRead);
                                        digest.update(buffer, 0, bytesRead);
                                    }
                                    byte[] hashbytes = digest.digest();

                                    HashData hashData = new HashData(hashbytes);
                            
                                
                            //      ErgoTokenData token = createToken();
                                
                                //   if (token != null) {
                                       
                                    addToken(fileName, fileString, hashData);
                                // }

                                } catch (IOException ex) {
                                    try {
                                        Files.writeString(App.logFile.toPath(), "\nErgoTokens:" + ex.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                    } catch (IOException e1) {

                                    }
                                } finally {
                                    if (outStream != null) {
                                        outStream.close();
                                    }
                                }
                            
                            }else{
                                HashData hashData = new HashData(entryFile);
                                addToken(fileName, fileString, hashData);
                            }
                        }
                    }
                    save();
                    m_ergoTokens.sendMessage(App.UPDATED, System.currentTimeMillis());
       
                }
            } catch (IOException e) {
                try {
                    Files.writeString(App.logFile.toPath(), "\nErgoTokens:" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
            } 


        }else{
            openJsonArray(jsonArray);
        }
    }
  
    private void openJsonArray(JsonArray jsonArray){
        for(int i = 0; i < jsonArray.size() ; i ++){
            JsonObject json = jsonArray.get(i).getAsJsonObject();

            JsonElement nameElement = json.get("name");
            JsonElement idElement = json.get("tokenId");
            JsonElement decimalsElement = json.get("decimals");
            JsonElement networkTypeElement = json.get("networkType");

            if(networkTypeElement.getAsString().equals(getNetworkType().toString())){


                ErgoTokenData ergoTokenData = new ErgoTokenData(nameElement.getAsString(),idElement.getAsString(), decimalsElement.getAsInt(), getNetworkType(), json, this);
             
                m_dataList.add(ergoTokenData);
            }

        }
        m_ergoTokens.sendMessage(App.UPDATED, System.currentTimeMillis());
    }
    
    /*public ErgoTokensList(ArrayList<ErgoTokenData> networkTokenList, NetworkType networkType, ErgoTokens ergoTokens) {
      
        m_networkType = networkType;
        m_ergoTokens = ergoTokens;
        
        for (ErgoTokenData networkToken : networkTokenList) {

            addToken(networkToken, false);

        }
    }*/





    public SimpleObjectProperty<ErgoExplorerList> ergoExplorerListProperty(){
        return m_ergoExplorersList;
    }
  

 //   private SimpleObjectProperty<LocalDateTime> m_marketUpdated = new SimpleObjectProperty<>(LocalDateTime.now());

    /*public SimpleObjectProperty<LocalDateTime> marketUpdated(){
        return m_marketUpdated;
    }*/

 
    public SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>();
    public SimpleObjectProperty<LocalDateTime> getLastUpdated(){
        return m_lastUpdated;
    }
 
    public void shutdown(){

     

    }


    /*
    public PriceQuote findPriceQuote(String baseSymbol, String quoteSymbol){
        ErgoMarketsData marketsData = m_selectedMarketData.get();
        if(marketsData != null){
            
        }
        /*PriceQuote[] quotes = priceQuotesProperty().get();
        if(quotes != null && baseSymbol != null && quoteSymbol != null){
            for(int i = 0; i < quotes.length ; i++){
                
                PriceQuote quote = quotes[i];
                if(quote.getTransactionCurrency().equals(baseSymbol) && quote.getQuoteCurrency().equals(quoteSymbol)){
                    return quote;
                }
            }
        }
        return null;
    }*/


    public SimpleObjectProperty<ErgoTokenData> selectedTokenProperty(){
        return m_selectedNetworkToken;
    }

   
 
    /* public SimpleObjectProperty<ErgoTokenData> getTokenProperty() {
        return m_ergoNetworkToken;
    }*/


    public ErgoTokens getErgoTokens(){
        return m_ergoTokens;
    }
   
  

    /*public void setNetworkType(SecretKey secretKey, NetworkType networkType) {

        
        m_networkTokenList.clear();
        m_networkType = networkType;
        setName("Ergo Tokens - List (" + networkType.toString() + ")");
        openFile(secretKey);
     

    }*/

    /*public void openTestnetFile(Path filePath) {
        m_networkTokenList.clear();

        if (filePath != null) {
            try {
                JsonElement jsonElement = new JsonParser().parse(Files.readString(filePath));

                if (jsonElement != null && jsonElement.isJsonObject()) {
                    openJson(jsonElement.getAsJsonObject(), NetworkType.TESTNET);
                }
            } catch (JsonParseException | IOException e) {
                try {
                    Files.writeString(logFile.toPath(), "\nInvalid testnet file: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
            }
        }
    }*/

   

    public void getErgoToken(String tokenid, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        
        Utils.returnObject(getErgoToken(tokenid), m_ergoTokens.getNetworksData().getExecService(), onSucceeded, onFailed);
        
    }

    public NetworksData getNetworksData(){
        return m_ergoTokens.getNetworksData();
    }

    private JsonArray getJsonArray(){
        JsonArray jsonArray = new JsonArray();
        for(int i = 0; i < m_dataList.size() ; i++){
            jsonArray.add(m_dataList.get(i).getJsonObject());
        }
        return jsonArray;
    }

    public void save(){
        JsonObject json = new JsonObject();
        JsonArray jsonArray = getJsonArray();
        json.add("data", jsonArray);
        getNetworksData().save("tokenList", m_ergoTokens.getNetworkType().toString(), ErgoTokens.NETWORK_ID, ErgoNetwork.NETWORK_ID, json);

        
    }

    public JsonObject getAddToken(String tokenId, String name, int decimals){

        ErgoTokenData ergoTokenData = getErgoToken(tokenId);
        if(ergoTokenData != null){
            if(ergoTokenData.getTimeStamp() == 0){
                ergoTokenData.update();
            }

            return ergoTokenData.getJsonObject();
        }else{
            ErgoTokenData newErgoTokenData = new ErgoTokenData(tokenId, name, decimals, getNetworkType(), this);
            addToken(newErgoTokenData, true);
            return newErgoTokenData.getJsonObject();
   
        }
    }


    public JsonObject getTokenJson( String tokenId) {
        if(tokenId != null){
            for (int i = 0; i < m_dataList.size(); i++) {
                ErgoTokenData tokenData = m_dataList.get(i);
                
                if(tokenId.equals(tokenData.getTokenId())){
                    return tokenData.getJsonObject();
                }
            }
        }
        return null;
    }

    public ErgoTokenData getErgoToken(String tokenId){
        if(tokenId != null){
            if(tokenId.equals(ErgoCurrency.TOKEN_ID)){
               
                m_ergoToken = m_ergoToken == null || (m_ergoToken != null) ? new ErgoToken(this) : m_ergoToken;
                   
                return m_ergoToken;
            }else{

                JsonObject json = getTokenJson(tokenId);
                
                if(json!= null){
                    JsonElement nameElement = json.get("name");
                    String name = nameElement != null && nameElement.isJsonPrimitive() ? nameElement.getAsString() : null;

                    return name != null ? new ErgoTokenData(tokenId, name, 0, m_ergoTokens.getNetworkType(), json, this) : null;
                }
                
            }
        }
        return null;
    }

    public ErgoTokenData getAddErgoToken(String tokenId, String name, int decimals){
        ErgoTokenData token = getErgoToken(tokenId);

        if(token == null){
            ErgoTokenData newToken = new ErgoTokenData(tokenId, name, decimals, getNetworkType(), this);
            addToken(newToken);
            return newToken;
        }
        return token;
    }



    public JsonObject getTokenJsonByName(String name) {
        if(name != null){
            for (int i = 0; i < m_dataList.size(); i++) {
                ErgoTokenData tokenData = m_dataList.get(i);
                
                if(name.equals(tokenData.getTokenId())){
                    return tokenData.getJsonObject();
                }
            }
        }
        return null;
    }

    public ErgoTokenData getTokenByName(String name){
        JsonObject json = getTokenJsonByName(name);
        if(json != null){
            JsonElement nameElement = json.get("name");
            JsonElement idElement = json.get("tokenId");
            JsonElement decimalsElement = json.get("decimals");
            return new ErgoTokenData(nameElement.getAsString(), idElement.getAsString(), decimalsElement.getAsInt(), getNetworkType(), json, this);
        }
        return null;
    }

    public JsonObject getTokensStageObject() {
        JsonObject tokenStageObject = new JsonObject();
        tokenStageObject.addProperty("subject", "GET_ERGO_TOKENS_STAGE");
        return tokenStageObject;
    }



    public NetworkType getNetworkType(){
        return m_ergoTokens.getNetworkType();
    }
    public void addToken(ErgoTokenData tokenData){
        addToken(tokenData, true);
    }

    public void addToken(ErgoTokenData tokenData, boolean save) {
       
        if (tokenData != null && tokenData.getTokenId() != null && tokenData.getName() != null) {
            if (getTokenJson(tokenData.getTokenId()) == null) {     
                m_dataList.add( tokenData);
                if(save){
                    save();
                    m_ergoTokens.sendMessage(App.LIST_ITEM_ADDED, System.currentTimeMillis(), tokenData.getTokenId());
                   
                }
            }else{
                try {
                    Files.writeString(App.logFile.toPath(), "\ncannot add" , StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }
            }
        }

    }

    public void removeToken(String tokenId) {

        removeToken(tokenId,true);
    }

    public void removeToken(String tokenId, boolean save) {
        if(tokenId != null){
            for (int i = 0; i < m_dataList.size(); i++) {
                ErgoTokenData tokenData = m_dataList.get(i);
                
                if(tokenId.equals(tokenData.getTokenId())){
                    m_dataList.remove(i);
                  
                    if(save){
                        save();
                        m_ergoTokens.sendMessage(App.LIST_ITEM_REMOVED, System.currentTimeMillis(), tokenId);
                    }
                    break;
                }
                
            }
            
        }
    }

    public void replaceToken(ErgoTokenData tokenData){
        if(tokenData != null){
        
            removeToken(tokenData.getTokenId(), false);
            addToken(tokenData, false);
            save();

            m_ergoTokens.sendMessage(App.LIST_UPDATED, System.currentTimeMillis(), tokenData.getTokenId());
        }
    }


    public void importJson(Stage callingStage, File file) {
        SimpleBooleanProperty updated = new SimpleBooleanProperty(false);
        if (file != null && file.isFile()) {
            String contentType = null;
            try {
                contentType = Files.probeContentType(file.toPath());

            } catch (IOException e) {

            }

            if (contentType != null && (contentType.equals("application/json") || contentType.substring(0, 4).equals("text"))) {

                JsonObject fileJson = null;
                try {
                    String fileString = Files.readString(file.toPath());
                    JsonElement jsonElement = new JsonParser().parse(fileString);
                    if (jsonElement != null && jsonElement.isJsonObject()) {
                        fileJson = jsonElement.getAsJsonObject();
                    }
                } catch (JsonParseException | IOException e) {
                    Alert noFile = new Alert(AlertType.NONE, e.toString(), ButtonType.OK);
                    noFile.setHeaderText("Load Error");
                    noFile.initOwner(callingStage);
                    noFile.setTitle("Import JSON - Load Error");
                    noFile.setGraphic(IconButton.getIconView(new Image("/assets/load-30.png"), App.MENU_BAR_IMAGE_WIDTH));
                    noFile.show();
                }
                if (fileJson != null) {
                    JsonElement networkTypeElement = fileJson.get("networkType");
                    JsonElement dataElement = fileJson.get("data");
                    NetworkType networkType =networkTypeElement != null && networkTypeElement.isJsonPrimitive() && networkTypeElement.getAsString().equals(NetworkType.TESTNET.toString()) ? NetworkType.TESTNET : NetworkType.MAINNET;

       
                    if (networkType != getNetworkType()) {
                        Alert a = new Alert(AlertType.NONE, "JSON network type is: " + networkType.toString() + ". Import into " + getNetworkType().toString() + " canceled.", ButtonType.OK);
                        a.initOwner(callingStage);
                        a.setHeaderText("Network Type Mismatch");
                        a.setGraphic(IconButton.getIconView(m_ergoTokens.getAppIcon(), 40));
                        a.showAndWait();
                        return;
                    }
                    
                    if (dataElement != null && dataElement.isJsonArray()) {
                        JsonArray dataArray = dataElement.getAsJsonArray();

                        for (JsonElement tokenObjectElement : dataArray) {
                            if (tokenObjectElement.isJsonObject()) {
                                JsonObject tokenJson = tokenObjectElement.getAsJsonObject();

                                JsonElement nameElement = tokenJson.get("name");
                                JsonElement tokenIdElement = tokenJson.get("tokenId");
                                JsonElement decimalsElement = tokenJson.get("decimals");

                                if (nameElement != null && tokenIdElement != null && decimalsElement != null) {
                                    String tokenId = tokenIdElement.getAsString();
                                    String name = nameElement.getAsString();
                                    int decimals = decimalsElement.getAsInt();
                                    ErgoTokenData oldToken = getErgoToken(tokenId);
                                    if (oldToken == null) {
                                        ErgoTokenData nameToken = getTokenByName(name);
                                        if (nameToken == null) {
                                            updated.set(true);
                                            addToken(new ErgoTokenData(name, tokenId, decimals, networkType, tokenJson, this));
                                        } else {
                                            Alert nameAlert = new Alert(AlertType.NONE, "Token:\n\n'" + tokenJson.toString() + "'\n\nName is used by another tokenId. Token will not be loaded.", ButtonType.OK);
                                            nameAlert.setHeaderText("Token Conflict");
                                            nameAlert.setTitle("Import JSON - Token Conflict");
                                            nameAlert.initOwner(callingStage);
                                            nameAlert.showAndWait();
                                        }
                                    } else {
                                        ErgoTokenData newToken = new ErgoTokenData(name, tokenId, decimals, networkType, tokenJson, this);

                                        Alert nameAlert = new Alert(AlertType.NONE, "Existing Token:\n\n'" + oldToken.getName() + "' exists, overwrite token with '" + newToken.getName() + "'?", ButtonType.YES, ButtonType.NO);
                                        nameAlert.setHeaderText("Resolve Conflict");
                                        nameAlert.initOwner(callingStage);
                                        nameAlert.setTitle("Import JSON - Resolve Conflict");
                                        Optional<ButtonType> result = nameAlert.showAndWait();

                                        if (result.isPresent() && result.get() == ButtonType.YES) {
                                            replaceToken(newToken);
                                        }
                                    }
                                } else {
                                    Alert noFile = new Alert(AlertType.NONE, "Token:\n" + tokenJson.toString() + "\n\nMissing name and/or tokenId properties and cannote be loaded.\n\nContinue?", ButtonType.YES, ButtonType.NO);
                                    noFile.setHeaderText("Encoding Error");
                                    noFile.initOwner(callingStage);
                                    noFile.setTitle("Import JSON - Encoding Error");
                                    Optional<ButtonType> result = noFile.showAndWait();

                                    if (result.isPresent() && result.get() == ButtonType.NO) {
                                        break;
                                    }
                                }
                            }
                        }

                    }
                }
            } else {
                Alert tAlert = new Alert(AlertType.NONE, "File content type: " + contentType + " not supported.\n\nContent type: text/plain or application/json required.", ButtonType.OK);
                tAlert.setHeaderText("Content Type Mismatch");
                tAlert.initOwner(callingStage);
                tAlert.setTitle("Import JSON - Content Type Mismatch");
                tAlert.setGraphic(IconButton.getIconView(new Image("/assets/load-30.png"), App.MENU_BAR_IMAGE_WIDTH));
                tAlert.show();

            }
        }
        

    }

   

    public JsonObject getJsonObject() {
        JsonObject tokensListJson = new JsonObject();
        tokensListJson.addProperty("networkType", getNetworkType().toString());


        tokensListJson.add("data", getJsonArray());

        return tokensListJson;
    }

     public void addToken( String key, String imageString, HashData hashData) {
        ErgoTokenData ergoToken = null;

        NetworkType networkType = m_ergoTokens.getNetworkType();
        switch (key) {
            case "aht":
                ergoToken = new ErgoTokenData("Ergo Auction House", "https://ergoauctions.org/",4,5000000000000L,"Official token for Auction House", "18c938e1924fc3eadc266e75ec02d81fe73b56e4e9f4e268dffffcb30387c42d", imageString, hashData, networkType, this);
                break;
            case "comet":
                ergoToken = new ErgoTokenData("Comet", "https://thecomettoken.com/",6,21000000000L,"The meme token on the Ergo blockchain. To infinity and beyond!", "0cd8c9f416e5b1ca9f986a7f10a84191dfb85941619e49e53c0dc30ebf83324b", imageString, hashData, networkType, this);
                break;
            case "cypx":
                ergoToken = new ErgoTokenData("CyberVerse", "https://cybercitizens.io/dist/pages/cyberverse.html", 4,5700000000000L,"CyberPixels, the official token of CyberVerse", "01dce8a5632d19799950ff90bca3b5d0ca3ebfa8aaafd06f0cc6dd1e97150e7f", imageString, hashData, networkType, this);
                break;
            case "egio":
                ergoToken = new ErgoTokenData("ErgoGames.io", "https://www.ergogames.io/",4,8500000000000L,"Official token of ErgoGames.io", "00b1e236b60b95c2c6f8007a9d89bc460fc9e78f98b09faec9449007b40bccf3", imageString, hashData, networkType, this);
                break;
            case "epos":
                ergoToken = new ErgoTokenData("ErgoPOS", "https://www.tabbylab.io/",4,5000000000000L,"Official token of ErgoPOS", "00bd762484086cf560d3127eb53f0769d76244d9737636b2699d55c56cd470bf", imageString, hashData, networkType, this);
                break;
            case "erdoge":
                ergoToken = new ErgoTokenData("Erdoge", "https://erdoge.biz/",0,500000L,"Like Doge but Erg.", "36aba4b4a97b65be491cf9f5ca57b5408b0da8d0194f30ec8330d1e8946161c1", imageString, hashData, networkType, this);
                break;
            case "ergold":
                ergoToken = new ErgoTokenData("Ergold", "https://github.com/supERGeometry/Ergold",0,55000000L, "Gold has been a store of wealth for thousands of years and has outlasted the currencies of various empires and nation-states that have come and gone. It is estimated that we have less than 55,000 tons of gold left to discover. Gold mining is one of the most destructive industries in the world. It can displace communities, contaminate drinking water, hurt workers, and destroy pristine environments. It pollutes water and land with mercury and cyanide, endangering the health of people and ecosystems.\nOur Understanding of a Traditional Asset Has Changed.\nAn asset is anything of value or a resource of value that can be converted into cash. So an asset’s value will depend on the collective belief and trust of the people dealing with it. \nThe growth in the number of cryptocurrencies is changing all of this, and the faith placed in them by investors is driving confidence in them as an asset class. If investors continue to believe in the value of gold because others believe in it, it will remain an asset. The difference between cryptocurrencies today and gold in the past is therefore minimal.\nIt is not a secret that Bitcoin is the most valued and thereby attractive cryptocurrency on the market. Experts have largely credited this to its scarcity. \nScarcity increases the value of an asset, therefore the Ergold quantity issued will be capped at 55 million. The number of kilograms of gold left to be discovered. \nLet's reduce the environmental impact and make its practices more sustainable with the blockchain technology.", "e91cbc48016eb390f8f872aa2962772863e2e840708517d1ab85e57451f91bed", imageString, hashData, networkType, this);
                break;
            case "ergone":
                ergoToken = new ErgoTokenData("ErgOne NFT", "http://ergone.io/",8,100000000000000L, "Limited supply / Reserve of value.  \nBuy and swap assets , acquire NFTs, currency in the Sigmaverse.\n\nAll in One","fcfca7654fb0da57ecf9a3f489bcbeb1d43b56dce7e73b352f7bc6f2561d2a1b", imageString, hashData, networkType, this);
                break;
            case "ergopad":
                ergoToken = new ErgoTokenData("ErgoPad", "https://www.ergopad.io/",2,40000000000L,"The official ErgoPad token", "d71693c49a84fbbecd4908c94813b46514b18b67a99952dc1e6e4791556de413", imageString, hashData, networkType, this);
                break;
            case "ermoon":
                ergoToken = new ErgoTokenData("ErMoon", "",2, 1000000000L ,"ErMoon is a token that created from Ergo Token Minter with focus as meme and give a solution as a website consultant and who use our service.  ", "9dbc8dd9d7ea75e38ef43cf3c0ffde2c55fd74d58ac7fc0489ec8ffee082991b", imageString, hashData, networkType, this);
                break;
            case "exle":
                ergoToken = new ErgoTokenData("Ergo-Lend", "https://exle.io/",4, 750000000000L,"Official token of Ergo-Lend","007fd64d1ee54d78dd269c8930a38286caa28d3f29d27cadcb796418ab15c283", imageString, hashData, networkType, this);
                break;
            case "flux":
                ergoToken = new ErgoTokenData("Flux", "https://runonflux.io/",8,44000000000000000L, "Flux - Decentralized Cloud Infrastructure - https://runonflux.io","e8b20745ee9d18817305f32eb21015831a48f02d40980de6e849f886dca7f807", imageString, hashData, networkType, this);
                break;
            case "getblock":
                ergoToken = new ErgoTokenData("GetBlok.io", "https://www.getblok.io/", 9,500000000000000L,"GetBlok.io Soft-Fork Governance Token", "4f5c05967a2a68d5fe0cdd7a688289f5b1a8aef7d24cab71c20ab8896068e0a8", imageString, hashData, networkType, this);
                break;
            case "kushti":
                ergoToken = new ErgoTokenData("Kushti", "https://github.com/kushti",0,1000000000L,"A token to support and memorialize nanoergs.", "fbbaac7337d051c10fc3da0ccb864f4d32d40027551e1c3ea3ce361f39b91e40", imageString, hashData, networkType, this);
                break;
            case "love":
                ergoToken = new ErgoTokenData("Love", "https://explorer.ergoplatform.com/en/issued-tokens?searchQuery=3405d8f709a19479839597f9a22a7553bdfc1a590a427572787d7c44a88b6386", 0,1000000,"People may not remember exactly what you did, or what you said, but they will always remember how you made them feel.", "3405d8f709a19479839597f9a22a7553bdfc1a590a427572787d7c44a88b6386", imageString, hashData, networkType, this);
                break;
            case "lunadog":
                ergoToken = new ErgoTokenData("LunaDog", "https://explorer.ergoplatform.com/en/issued-tokens?searchQuery=5a34d53ca483924b9a6aa0c771f11888881b516a8d1a9cdc535d063fe26d065e",0,2000000000000000L ,"Community created and backed token on the Ergo Platform. ", "5a34d53ca483924b9a6aa0c771f11888881b516a8d1a9cdc535d063fe26d065e", imageString, hashData, networkType, this);
                break;
            case "migoreng":
                ergoToken = new ErgoTokenData("Mi Goreng", "https://docs.google.com/spreadsheets/d/148c1iHNMNfyjscCcPznepkEnMp2Ycj3HuLvpcLsnWrM/edit#gid=205730070", 0,15000000000L, "The most delicious, delectable, palatable feast that one can ever enjoy.", "0779ec04f2fae64e87418a1ad917639d4668f78484f45df962b0dec14a2591d2", imageString, hashData, networkType, this);
                break;
            case "neta":
                ergoToken = new ErgoTokenData("anetaBTC", "https://anetabtc.io/",6, 1000000000000000L,"", "472c3d4ecaa08fb7392ff041ee2e6af75f4a558810a74b28600549d5392810e8", imageString, hashData, networkType, this);
                break;
            case "obsidian":
                ergoToken = new ErgoTokenData("Adventurers DAO", "https://adventurersdao.xyz/",0,25000000L,"Governance token for the AdventurersDAO.", "2a51396e09ad9eca60b1bdafd365416beae155efce64fc3deb0d1b3580127b8f", imageString, hashData, networkType, this);
                break;
            case "ogre":
                ergoToken = new ErgoTokenData("Ogre", "https://ogre-token.web.app",0, 1000000000000000000L,	"Ergo's #1 Meme Coin","6de6f46e5c3eca524d938d822e444b924dbffbe02e5d34bd9dcd4bbfe9e85940", imageString, hashData, networkType, this);
                break;
            case "paideia":
                ergoToken = new ErgoTokenData("Paideia", "https://www.paideia.im/",4,2000000000000L,	"Official Paideia DAO token", "1fd6e032e8476c4aa54c18c1a308dce83940e8f4a28f576440513ed7326ad489", imageString, hashData, networkType, this);
                break;
            case "proxie":
                ergoToken = new ErgoTokenData("Proxies NFT", "https://proxiesnft.io/",6, 21000000000000L,	"Description:\n                 The multichain utility and governance token for the ProxiesNFT Project\n                 Ticker:\n                 PROXIE\n                 Url:\n                 www.proxiesnft.io","01ddcc3d0205c2da8a067ffe047a2ccfc3e8241bc3fcc6f6ebc96b7f7363bb36", imageString, hashData, networkType, this);
                break;
            case "quacks":
                ergoToken = new ErgoTokenData("duckpools.io", "https://www.duckpools.io/",6,100000000000000L,	"The official token of the duckpools platform", "089990451bb430f05a85f4ef3bcb6ebf852b3d6ee68d86d78658b9ccef20074f", imageString, hashData, networkType, this);
                break;
            case "sigrsv":
                ergoToken = new ErgoTokenData("Sigma Reserve", "https://sigmausd.io/",6, 10000000000001L, "SigRSV - V2 - Reserve token for SigmaUSD Bank","003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0", imageString, hashData, networkType, this);
                break;
            case "sigusd":
                ergoToken = new ErgoTokenData("Sigma USD", "https://sigmausd.io/",2, 10000000000001L	,"SigmaUSD - V2","03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04", imageString, hashData, networkType, this);
                break;
            case "spf":
                ergoToken = new ErgoTokenData("Spectrum Finanace", "https://spectrum.fi/",6, 1000000000000000L 	,"Official utility and governance token of the Spectrum Finance protocol","9a06d9e545a41fd51eeffc5e20d818073bf820c635e2a9d922269913e0de369d", imageString, hashData, networkType, this);
                break;
            case "terahertz":
                ergoToken = new ErgoTokenData("swamp.audio", "https://www.thz.fm/",4,400000000000L,	"The official SwampAudio token", "02f31739e2e4937bb9afb552943753d1e3e9cdd1a5e5661949cb0cef93f907ea", imageString, hashData, networkType, this);
                break;
            case "walrus":
                ergoToken = new ErgoTokenData("Walrus Dao", "https://www.walrusdao.io/",0,10000000,"Governance token for Walrus DAO.", "59ee24951ce668f0ed32bdb2e2e5731b6c36128748a3b23c28407c5f8ccbf0f6", imageString, hashData, networkType, this);
                break;
            case "woodennickels":
                ergoToken = new ErgoTokenData("Wooden Nickles", "https://brianrxm.com/comimg/cnsmovtv_perrymason_woodennickels_12.jpg",6,97739924000000L, "\t`Tip token inspired by the American idiom: \"Don't take any wooden nickels.\" Wooden nickels were a form of fake promotional coinage that were sometimes used as community currency but more often were a scam.`", "4c8ac00a28b198219042af9c03937eecb422b34490d55537366dc9245e85d4e1", imageString, hashData, networkType, this);
                break;
        }
        if(ergoToken != null && getErgoToken(ergoToken.getTokenId()) == null){
           
            m_dataList.add( ergoToken);
              
            
                   
                
            

        }
    }


    
}
