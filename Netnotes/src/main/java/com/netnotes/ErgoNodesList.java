package com.netnotes;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.utils.Utils;

public class ErgoNodesList {

    //options
    public final static String DEFAULT = "DEFAULT";
    public final static String PUBLIC = "PUBLIC";
    public final static String CUSTOM = "CUSTOM";


    private ErgoNodes m_ergoNodes;

    private HashMap<String, ErgoNodeData> m_dataList = new HashMap<>();


    private String m_downloadImgUrl = "/assets/cloud-download-30.png";
    private String m_defaultNodeId = null;
    private ErgoNetworkData m_ergoNetworkData;

    public ErgoNodesList( ErgoNodes ergoNodes, ErgoNetworkData ergoNetworkData) {
        m_ergoNodes = ergoNodes;
        m_ergoNetworkData = ergoNetworkData;
        getData();
    }

    private void getData() {
        
   
        JsonObject json = m_ergoNodes.getNetworksData().getData("data",".", ErgoNetwork.NODE_NETWORK, ErgoNetwork.NETWORK_ID);
        

        openJson(json);
            
        

    }

    public final static String PUBLIC_NODES_LIST_URL = "https://raw.githubusercontent.com/networkspore/Netnotes/main/publicNodes.json";
    
    private void getPublicNodesList(){
        
  
        Utils.getUrlJson(PUBLIC_NODES_LIST_URL, getErgoNetwork().getNetworksData().getExecService(), (onSucceeded) -> {
            Object sourceObject = onSucceeded.getSource().getValue();
            if (sourceObject != null && sourceObject instanceof JsonObject) {
                //openNodeJson((JsonObject) sourceObject);
            }
        }, (onFailed) -> {
            //  setDefaultList();
        });
    
        
    }

    public String getDefaultNodeId(){
        return m_defaultNodeId;
    }

    public void setDefaultNodeId(String id){
        setDefaultNodeId(id, true);
    }

    public void setDefaultNodeId(String id, boolean isSave){
        m_defaultNodeId = id;

        if(isSave){
          
            save();
            long timeStamp = System.currentTimeMillis();
            JsonObject note = Utils.getMsgObject(App.LIST_DEFAULT_CHANGED, timeStamp, m_ergoNodes.getNetworkId());
            note.addProperty("code", App.LIST_DEFAULT_CHANGED);
            note.addProperty("timeStamp", timeStamp);
            if(id != null){
                note.addProperty("id",  id);
            }
            
            getErgoNetwork().sendMessage(App.LIST_DEFAULT_CHANGED, timeStamp, m_ergoNodes.getNetworkId(), note.toString());
        }
    
    }

    public JsonObject setDefault(JsonObject note){
        JsonElement idElement = note != null ? note.get("id") : null;
  
        if(idElement != null){
            String defaultId = idElement.getAsString();
            ErgoNodeData nodeData = getNodeById(defaultId);
            if(nodeData != null){
                setDefaultNodeId(defaultId);
                return nodeData.getJsonObject();
            }
        }
        return null;
    }

    public Boolean clearDefault(){

        m_defaultNodeId = null;
        long timeStamp = System.currentTimeMillis();
        
        JsonObject note = Utils.getJsonObject("networkId", m_ergoNodes.getNetworkId());
        note.addProperty("code", App.LIST_DEFAULT_CHANGED);
        note.addProperty("timeStamp", timeStamp);
        getErgoNetwork().sendMessage(App.LIST_DEFAULT_CHANGED, timeStamp, m_ergoNodes.getNetworkId(), note.toString());
        

        return true;
    }

    public JsonObject getDefault(){
        ErgoNodeData nodeData = getNodeById(m_defaultNodeId);

        return nodeData != null ? nodeData.getJsonObject() : null;
        
    }

    public NetworksData getNetworksData(){
        return m_ergoNodes.getNetworksData();
    }

    public ErgoNetworkData getErgoNetworkData(){
        return m_ergoNetworkData;
    }


    private void openJson(JsonObject json) {
      
        JsonElement nodesElement = json != null ? json.get("nodes") : null;
        JsonElement defaultNodeIdElement = json !=null ? json.get("defaultNodeId") : null;

     
        if (nodesElement != null && nodesElement.isJsonArray()) {

   
            JsonArray jsonArray = nodesElement.getAsJsonArray();
         
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonElement nodeElement = jsonArray.get(i);

                if (nodeElement != null && nodeElement.isJsonObject()) {
                    JsonObject nodeJson = nodeElement.getAsJsonObject();

                    JsonElement namedNodeElement = nodeElement == null ? null : nodeJson.get("namedNode");
                 
                        
                        JsonElement idElement = nodeJson == null ? null : nodeJson.get("id");
                        JsonElement nameElement = nodeJson == null ? null : nodeJson.get("name");
                        JsonElement iconElement = nodeJson == null ? null : nodeJson.get("iconString");
                        JsonElement clientTypeElement = nodeJson == null ? null : nodeJson.get("clientType");
                        

                        if(namedNodeElement != null && idElement != null && nameElement != null && iconElement != null){
                            String clientType = clientTypeElement != null ? clientTypeElement.getAsString() : ErgoNodeData.LIGHT_CLIENT;
                            String id = idElement.getAsString();
                            String name = nameElement.getAsString();
                            String imgString = iconElement.getAsString();

                            try{
                                switch(clientType){
                                    case ErgoNodeData.LOCAL_NODE:
                                        loadLocalNode(id, name, imgString, clientType, nodeJson);
                                    break;
                                    default:
                                        ErgoNodeData ergoNodeData = new ErgoNodeData(imgString, name, id, clientType, this, nodeJson);
                                        addRemoteNode(ergoNodeData , false);
                                    break;
                                }
                            }catch(Exception e){
                                try {
                                    Files.writeString(App.logFile.toPath(), e.toString() +"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                                } catch (IOException e1) {
                           
                                }
                            
                            }
                          
                        }   
                    
                }
            }
           
            setDefaultNodeId(defaultNodeIdElement != null ?  defaultNodeIdElement.getAsString() : null, false);
        }else{
            NamedNodeUrl defaultNode = new NamedNodeUrl();
            addRemoteNode(new ErgoNodeData(ErgoNodeData.PULIC_NODE_1,defaultNode.getName(), ErgoNodeData.LIGHT_CLIENT, FriendlyId.createFriendlyId(), this,  defaultNode), true);
        }



       
   
    }

    private void loadLocalNode(String id, String name, String imgString, String clientType, JsonObject nodeJson) throws Exception{
        ErgoNodeLocalData localData = new ErgoNodeLocalData(imgString, id, name, clientType, nodeJson, this);

        addNode(localData, false);
    }

    private void addNode(ErgoNodeData nodeData, boolean isSave){
        if(nodeData != null && nodeData.getId() != null){
            nodeData.addUpdateListener((obs,oldval,newval)->{
                JsonObject note = Utils.getJsonObject("networkId", ErgoNetwork.WALLET_NETWORK);
                note.addProperty("id", nodeData.getId());
                note.addProperty("code", App.UPDATED);

                long timeStamp = System.currentTimeMillis();
                note.addProperty("timeStamp", timeStamp);

                getErgoNetwork().sendMessage(App.LIST_ITEM_ADDED,timeStamp, ErgoNetwork.WALLET_NETWORK, note.toString());
                
                save();
            });
            String id = nodeData.getId();
            m_dataList.put(id, nodeData);

          

            if(isSave){
                JsonObject note = Utils.getJsonObject("networkId", ErgoNetwork.WALLET_NETWORK);
                note.addProperty("id", id);
                note.addProperty("code", App.UPDATED);

                long timeStamp = System.currentTimeMillis();
                note.addProperty("timeStamp", timeStamp);

                getErgoNetwork().sendMessage(App.LIST_ITEM_ADDED, timeStamp, ErgoNetwork.WALLET_NETWORK, note.toString());

               
            }
        }
    }

   


    public String getDownloadImgUrl() {
        return m_downloadImgUrl;
    }


    public ErgoNodeData getRemoteNode(String id) {
        if (id != null) {
       
            return m_dataList.get(id);
            
            
        }
        return null;
    }


    public void addRemoteNode(ErgoNodeData ergoNodeData, boolean isSave) {
   
        if (ergoNodeData != null && ergoNodeData.getId() != null) {
            if(getNodeById(ergoNodeData.getId()) == null){
                
                addNode(ergoNodeData, isSave);
                
            }
        }
    }

    public ErgoNetwork getErgoNetwork(){
        return m_ergoNodes.getErgoNetwork();
    }

    public boolean remove(String id, boolean isDelete, boolean isSave) {
        if(id != null ){
            if(id.equals(m_defaultNodeId)){
                clearDefault();
            }
             
            ErgoNodeData nodeData = m_dataList.remove(id);

            if(nodeData != null){
                if(isDelete){
                    if(nodeData != null && nodeData instanceof ErgoNodeLocalData){
                        File nodeAppDir = ((ErgoNodeLocalData) nodeData).getAppDir();
                        if(nodeAppDir != null){
                     
                            try {
                                FileUtils.deleteDirectory(nodeAppDir);
                            } catch (IOException e) {
                                try {
                                    Files.writeString(App.logFile.toPath(), "Error deleting node files: " + e.toString() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                } catch (IOException e1) {
                   
                                }
                            }

                        }
                    }
                }
                if(isSave){
                    save();
        
                    JsonObject note = Utils.getJsonObject("networkId", m_ergoNodes.getNetworkId());
                    
                    JsonArray jsonArray = new JsonArray();
                    jsonArray.add(nodeData.getJsonObject());

                    note.add("ids", jsonArray);
                    note.addProperty("code", App.LIST_ITEM_REMOVED);
                            
                    long timeStamp = System.currentTimeMillis();
                    note.addProperty("timeStamp", timeStamp);
                    
                    getErgoNetwork() .sendMessage(App.LIST_ITEM_REMOVED, timeStamp, m_ergoNodes.getNetworkId(), note.toString());
                }

                return true;
            }
        }
        return false;
        
    }

    public JsonObject removeNodes(JsonObject note){
        
        long timestamp = System.currentTimeMillis();

        JsonElement idsElement = note.get("ids");

        if(idsElement != null && idsElement.isJsonArray()){
       
            JsonArray idsArray = idsElement.getAsJsonArray();
            if(idsArray.size() > 0){
                
                JsonObject json = Utils.getMsgObject(App.LIST_ITEM_REMOVED, timestamp, m_ergoNodes.getNetworkId());
                JsonArray jsonArray = new JsonArray();

                for(JsonElement element : idsArray){
                    JsonObject idObj = element.getAsJsonObject();

                    JsonElement idElement = idObj.get("id");

                    if(idElement != null){
                        String id = idElement.getAsString();

                        JsonElement deleteElement = idObj.get("isDelete");
                        
                        boolean isDelete = deleteElement != null && deleteElement.isJsonPrimitive() ? deleteElement.getAsBoolean() : false;
             
                        
                        if(remove(id, isDelete, false)){
                            jsonArray.add(idObj);
                        }
                    }
                }
                
                json.add("ids", jsonArray);



                save();

                getErgoNetwork().sendMessage( App.LIST_ITEM_REMOVED, timestamp, m_ergoNodes.getNetworkId(), json.toString());

                return json;
            }

        }

        return null;
    }

    public ErgoNodeData getNodeById(String id){
 
        if(id != null){

            return m_dataList.get(id);
        
        }

        return null;
    }

    public NoteInterface getNoteInterface(JsonObject note){
   
        if(note != null){
            JsonElement idElement = note.get("id");
            String id = idElement != null && idElement.isJsonPrimitive() ? idElement.getAsString() : null;
            
            if(id != null){
        
                ErgoNodeData nodeData = getNodeById(id);
                if(nodeData != null){
                    return nodeData.getNoteInterface();
                }
            }
        }

        return null;
    }

    public NoteInterface getDefaultInterface(JsonObject note){
  
        ErgoNodeData nodeData = getNodeById(m_defaultNodeId);
        if(nodeData != null){
            return nodeData.getNoteInterface();
        }
        
        return null;
    }

    public JsonObject getLocalNodeByFile(File file){
        if (m_dataList.size() > 0 && file != null) {
            
            file = file.isFile() ? file.getParentFile() : (file.isDirectory() ? file : null);

            if ( file != null) {
              
                for (Map.Entry<String, ErgoNodeData> entry : m_dataList.entrySet()) {
                    ErgoNodeData nodeData = entry.getValue();
                    if(nodeData instanceof ErgoNodeLocalData){
                        ErgoNodeLocalData localData = (ErgoNodeLocalData) nodeData;
                        
                        if (localData.getAppDir().getAbsolutePath().equals(file.getAbsolutePath())){
                            return localData.getJsonObject();
                        }
            
                    }
                }
            }
        }
        return null;
    }

    public JsonObject getNodeByName(String name){
        if (m_dataList.size() > 0 && name != null) {
            
              
            for (Map.Entry<String, ErgoNodeData> entry : m_dataList.entrySet()) {
                ErgoNodeData nodeData = entry.getValue();
      
                
                if (nodeData.getName().equals(name) ){
                    return nodeData.getJsonObject();
                }
    
                
            }
            
        }
        return null;
    }

            
    public JsonArray getNodesJsonArray() {
        JsonArray jsonArray = new JsonArray();

        for (Map.Entry<String, ErgoNodeData> entry : m_dataList.entrySet()) {
            ErgoNodeData data = entry.getValue();
            JsonObject jsonObj = data.getJsonObject();
            jsonArray.add(jsonObj);
        }

        return jsonArray;
    }


    public JsonArray getRemoteNodes(JsonObject note) {

        JsonArray jsonArray = new JsonArray();

        for (Map.Entry<String, ErgoNodeData> entry : m_dataList.entrySet()) {
            ErgoNodeData ergNodeData = entry.getValue();
            if(!(ergNodeData instanceof ErgoNodeLocalData)){
                JsonObject jsonObj = ergNodeData.getJsonObject();
                jsonArray.add(jsonObj);
            }
        }
      
        return jsonArray;
    }

    public JsonArray getLocalNodes(JsonObject note) {
        JsonArray jsonArray = new JsonArray();

        for (Map.Entry<String, ErgoNodeData> entry : m_dataList.entrySet()) {
            ErgoNodeData ergNodeData = entry.getValue();

            if(ergNodeData instanceof ErgoNodeLocalData){
                JsonObject jsonObj = ergNodeData.getJsonObject();
                jsonArray.add(jsonObj);
            }

        }

        return jsonArray;
    }

    public JsonArray getNodes(JsonObject note){
        return getNodesJsonArray();
    }


     public JsonObject getDataJson() {
        JsonObject json = new JsonObject();

        json.add("nodes", getNodesJsonArray());
        if(m_defaultNodeId != null){
            json.addProperty("defaultNodeId", m_defaultNodeId);
        }
        return json;
    }



    public void save() {
        JsonObject saveJson = getDataJson();
        m_ergoNodes.getNetworksData().save("data",".", ErgoNetwork.NODE_NETWORK, ErgoNetwork.NETWORK_ID, saveJson);
        
 
    }

  
    /*
    public VBox getGridBox(SimpleDoubleProperty width, SimpleDoubleProperty scrollWidth) {
        VBox gridBox = new VBox();

        Runnable updateGrid = () -> {
            gridBox.getChildren().clear();
          
            if(m_localDataList != null){
                HBox fullNodeRowItem = m_localDataList.getRowItem();
                fullNodeRowItem.prefWidthProperty().bind(width.subtract(scrollWidth));
                gridBox.getChildren().add(fullNodeRowItem);
            }            
            
            int numCells = m_dataList.size();

            for (int i = 0; i < numCells; i++) {
                ErgoNodeData nodeData = m_dataList.get(i);
                HBox rowItem = nodeData.getRowItem();
                rowItem.prefWidthProperty().bind(width.subtract(scrollWidth));
                gridBox.getChildren().add(rowItem);
            }

        };

        updateGrid.run();

        m_doGridUpdate.addListener((obs, oldval, newval) -> updateGrid.run());

        return gridBox;
    } */

    /*
    public void getMenu(MenuButton menuBtn, SimpleObjectProperty<ErgoNodeData> selectedNode){

        Runnable updateMenu = () -> {
            menuBtn.getItems().clear();
            ErgoNodeData newSelectedNodedata = selectedNode.get();

           

            MenuItem noneMenuItem = new MenuItem("(disabled)");
            if(selectedNode.get() == null){
                noneMenuItem.setId("selectedMenuItem");
            }
            noneMenuItem.setOnAction(e->{
                selectedNode.set(null);
            });
            menuBtn.getItems().add(noneMenuItem);

            if(m_localDataList != null){
                MenuItem localNodeMenuItem = new MenuItem( m_localDataList.getName());
                if(m_localDataList != null && newSelectedNodedata != null && m_localDataList.getId().equals(newSelectedNodedata.getId())){
                    localNodeMenuItem.setId("selectedMenuItem");
                    localNodeMenuItem.setText(localNodeMenuItem.getText());
                }
                localNodeMenuItem.setOnAction(e->{
                    selectedNode.set(m_localDataList);
                });

                menuBtn.getItems().add( localNodeMenuItem );
            }            
            
            int numCells = m_dataList.size();

            for (int i = 0; i < numCells; i++) {
                
                ErgoNodeData nodeData = m_dataList.get(i);
                 MenuItem menuItem = new MenuItem(nodeData.getName());
                if(newSelectedNodedata != null && newSelectedNodedata.getId().equals(nodeData.getId())){
                    menuItem.setId("selectedMenuItem");
                    menuItem.setText(menuItem.getText());
                }
                menuItem.setOnAction(e->{
                    selectedNode.set(nodeData);
                });

                menuBtn.getItems().add(menuItem);
            }



        };

        updateMenu.run();

        selectedNode.addListener((obs,oldval, newval) -> updateMenu.run());

        m_doGridUpdate.addListener((obs, oldval, newval) -> updateMenu.run());

    }
 */
    
   

    

    public void shutdown() {
        for (Map.Entry<String, ErgoNodeData> entry : m_dataList.entrySet()) {
            ErgoNodeData ergNodeData = entry.getValue();
            ergNodeData.shutdown();
        }

    }

    /*private void setDefaultNodeOption(String option) {
        m_defaultAddType = option;
        save();
    }*/

    private String m_tmpId = null;

    private String getNewId(){
        m_tmpId = FriendlyId.createFriendlyId();

        while(getNodeById(m_tmpId) != null){
            m_tmpId = FriendlyId.createFriendlyId();
        }

        return m_tmpId;
    }



    public ErgoNodes getErgoNodes() {
        return m_ergoNodes;
    }

    public String getMemoryInfoString(FreeMemory freeMemory){
        int nodeMemRequired = ErgoNodeLocalData.DEFAULT_MEM_GB_REQUIRED;
        int memoryFootPrint = (int) ((freeMemory.getMemTotalGB() - freeMemory.getMemFreeGB()) + nodeMemRequired ) ;                

        double required = freeMemory.getMemFreeGB() - nodeMemRequired;
        required = required > 0 ? 0 : Math.abs(required);
        String availableMemoryString = String.format("%.1f", freeMemory.getMemAvailableGB()) + " GB";
        String memoryString = freeMemory == null ? " - " :  String.format("%.1f", freeMemory.getMemFreeGB()) + " GB /  " + String.format("%-8s",availableMemoryString) + " / " + String.format("%.1f",freeMemory.getMemTotalGB())+ " GB";
        String swapString = freeMemory == null ? " - " : String.format("%.1f",freeMemory.getSwapFreeGB());
        
//String.format("%.1f", required)

        String textAreaString = "Memory Requirements";
        textAreaString += "\n";
        textAreaString += "\nUsage:       " + nodeMemRequired + " GB / " + memoryFootPrint + " GB";
        textAreaString += "\n                   (Peak / Total)";
        textAreaString += "\nSystem:    " + String.format("%-50s",  memoryString) + "Swap: " + swapString + " GB";
        textAreaString += "\n" + String.format("%-70s","                   (Free / Available / Total)")+"             (Total)";
        textAreaString += "\n";
        if(required > 0){
            textAreaString += "\nWarning: " + String.format("%.1f", freeMemory.getMemFreeGB()) + " GB free memory, " + nodeMemRequired + " GB recommended.";
            textAreaString += "\nSwap usage: " + String.format("%.2f",required) + " GB";
            
            int swapSize = getSwapSize(freeMemory);

            if(swapSize > -1){
                textAreaString += "\n\n*Recommended: Increase swap file size to:" + swapSize + " GB*";
            }    
        }
        return textAreaString;
    }

    public int getMemoryFootPrint(FreeMemory freeMemory){
        int nodeMemRequired = ErgoNodeLocalData.DEFAULT_MEM_GB_REQUIRED;
        return (int) ((freeMemory.getMemTotalGB() - freeMemory.getMemFreeGB()) + nodeMemRequired ) ;                
    }

    public int getSwapSize(FreeMemory freeMemory){
        int memoryFootPrint = getMemoryFootPrint(freeMemory);
        
        if(freeMemory.getSwapTotalGB() < (freeMemory.getMemTotalGB() + 2) || (freeMemory.getSwapTotalGB() < memoryFootPrint + 2)){
            return (Math.ceil(memoryFootPrint + 2) > Math.ceil(freeMemory.getMemTotalGB() + 2) ? (int)Math.ceil(memoryFootPrint + 2) : (int) Math.ceil(freeMemory.getMemTotalGB() + 2)) ;
        }

        return -1; 
    }

    public ErgoNodeLocalData getLocalNode(String id) {

        ErgoNodeData nodeData = m_dataList.get(id);
            
        return nodeData instanceof ErgoNodeLocalData ? (ErgoNodeLocalData) nodeData : null;
        
    }



    public ErgoNodeData getRemoteNodeByUrl(String urlString){
        if (urlString != null) {
              
            for (Map.Entry<String, ErgoNodeData> entry : m_dataList.entrySet()) {
                ErgoNodeData nodeData = entry.getValue();

                if(nodeData.getNamedNodeUrl().getUrlString().equals(urlString)){
                    return nodeData;
                }
            }
            
        }
        return null;
    }

    public String addRemoteNode(JsonObject note){
        if(note != null){
            JsonElement dataElement = note.get("data");
            JsonObject dataJson = dataElement != null ? dataElement.getAsJsonObject() : null;
            
            
            if(dataJson != null){

                NamedNodeUrl nodeUrl = null;
                try{
                    nodeUrl = new NamedNodeUrl(dataJson);
                }catch(Exception e){
                    try {
                        Files.writeString(App.logFile.toPath(), "\naddRemoteNode failed: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e1) {

                    }
                }


                if(nodeUrl != null){
                    

                    ErgoNodeData existingNode = getRemoteNodeByUrl(nodeUrl.getUrlString());
        
                    if(existingNode != null){
                        return existingNode.getNetworkId();
                    }
                    
                   
                    String configId = FriendlyId.createFriendlyId();
                    String networkId = getNewId();
                    ErgoNodeData ergoNodeData = new ErgoNodeData(networkId, nodeUrl.getName(), ErgoNodeData.LIGHT_CLIENT, configId, this, nodeUrl);

                   
                    addRemoteNode(ergoNodeData, true);

                    return networkId;
                }
            }
        }

        return null;
    }

    public ExecutorService getExecService(){
        return getNetworksData().getExecService();
    }


    public JsonObject addLocalNode(JsonObject note){
       
        if(note != null){
            JsonElement dataElement = note.get("data");

            if(dataElement != null && dataElement.isJsonObject()){

        
                JsonObject dataJson = dataElement.getAsJsonObject();

                JsonElement namedNodeElement = dataJson.get("namedNode");
                

                JsonElement configFileNameElement = dataJson.get("configFileName");
                JsonElement configTextElement = dataJson.get("configText");

                JsonElement isAppElement = dataJson.get("isAppFile");
                JsonElement appFileElement = dataJson.get("appFile");

                JsonElement appDirElement = dataJson.get("appDir");

                if(namedNodeElement != null && namedNodeElement.isJsonObject() && appDirElement != null){
                    NamedNodeUrl namedNodeUrl = null;
                    try {
                        JsonObject namedNodeJson = namedNodeElement.getAsJsonObject();
                     
                        namedNodeUrl = new NamedNodeUrl(namedNodeJson);
                    } catch (Exception e1) {
                        return Utils.getMsgObject(App.ERROR, e1.toString());
                    }

                    if(configFileNameElement != null){
                        String configFileName = configFileNameElement.getAsString();
                        String configText = configTextElement != null ? configTextElement.getAsString() : null;

                        File roots[] = App.getRoots();
                        String appDirString = appDirElement != null && appDirElement.isJsonPrimitive() ? appDirElement.getAsString() : null;

                        File appDir = appDirElement != null && Utils.findPathPrefixInRoots(roots, appDirString) ? new File(appDirString) : null;

                        

                        if(appDir != null){
            

                            if(!appDir.isDirectory()){
                                try {
                                    boolean success = appDir.mkdirs();
                                    if (!success && !appDir.isDirectory()) {
                                    
                                        return Utils.getMsgObject(App.ERROR, "Unable to access folder location");
                                    }

                                } catch (SecurityException e1) {
                                    return Utils.getMsgObject(App.ERROR, e1.toString());
                                }
                            }

                            if(getLocalNodeByFile(appDir) != null){
                                return Utils.getMsgObject(App.ERROR, "Directory contains an existing node");
                            }

                            boolean isAppFile = isAppElement != null && isAppElement.isJsonPrimitive() ? isAppElement.getAsBoolean() : false;
                            String appFileString = isAppFile && appFileElement != null && appFileElement.isJsonPrimitive() ? appFileElement.getAsString() : null;

                            File appFile = appFileString != null && Utils.findPathPrefixInRoots(roots, appFileString) ? new File(appFileString) : null;
                        
            
                            if((isAppFile && appFile != null) || !isAppFile){
                                
                                
                                String id = getNewId();
                                String configId = FriendlyId.createFriendlyId();
                                
                                
                                try {
                                    ErgoNodeLocalData localNodeData = new ErgoNodeLocalData(id,configId, appDir, isAppFile, appFile, configFileName, configText, namedNodeUrl, this);
                                    addNode(localNodeData, true);
                                             
                                    JsonObject returnObject = Utils.getJsonObject("code", App.SUCCESS);
                                    returnObject.addProperty("id",id);
                                    returnObject.addProperty("configId", configId);

                                    return returnObject;
                                } catch (Exception e1) {
                                    return Utils.getMsgObject(App.ERROR, e1.toString());
                                }
                                
                            }else{
                                
                                return Utils.getMsgObject(App.ERROR, "App file missing");
                                
                            }
                        
                        }else{
                
                            return Utils.getMsgObject(App.ERROR, "Directory element missing");
                        }
                    }else{
                        return Utils.getMsgObject(App.ERROR, "Config element missing");
                    }
                
                }else{
                    
                    return Utils.getMsgObject(App.ERROR, "Named node json object missing");
                }


            }else{
                return Utils.getMsgObject(App.ERROR, "Note data element required");
            }
        }else{
            return Utils.getMsgObject(App.ERROR, "Note is null");
        }
                    
          

        

    }


}
