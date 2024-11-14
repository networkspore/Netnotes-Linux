package com.netnotes;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.utils.Utils;

import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class ErgoNodeLocalControl extends ErgoNodeClientControl {

    private BufferedButton m_updateBtn;
    private VBox m_topPropertiesBox = null;
    private Tooltip m_updateTooltip = new Tooltip("Update"); 
    private ProgressBar m_progressBar = null;

    public BufferedButton getUpdateBtn() {
        return m_updateBtn;
    }

    public VBox getTopPropertiesBox() {
        return m_topPropertiesBox;
    }

    public ProgressBar getProgressBar() {
        return m_progressBar;
    }

    public ErgoNodeLocalControl(NoteInterface noteInterface, NamedNodeUrl nodeUrl, String clientType, String accessId){
        super(noteInterface, nodeUrl,clientType, accessId);

        getConnectBtn().setText(App.PLAY);
        getConnectBtn().setOnMouseClicked(e->{
            if(getConnectBtn().getText().equals(App.STOP)){
                terminate();
            }else{
                run();
            }
        });

        m_topPropertiesBox = new VBox();
        HBox.setHgrow(m_topPropertiesBox, Priority.ALWAYS);
        m_updateTooltip.setShowDelay(Duration.millis(100));

        m_updateBtn = new BufferedButton("/assets/cloud-download-30.png", App.MENU_BAR_IMAGE_WIDTH);
        m_updateBtn.setOnAction(e->updateApp());
        m_updateBtn.setTooltip(m_updateTooltip);

        getStatusPaddingBox().getChildren().add(m_updateBtn);
    
        getChildren().add(1, m_topPropertiesBox);
    }

    public void updateApp(){
        JsonObject note = Utils.getCmdObject("updateApp");
        note.addProperty("accessId", getAccessId());

        getNodeInterface().sendNote(note);
    }


    @Override
    public void getStatus(){
        JsonObject note = Utils.getCmdObject("getStatus");
        note.addProperty("accessId", getAccessId());


        Object obj = getNodeInterface().sendNote(note);
        
     

        if(obj != null && obj instanceof  JsonObject ){
            JsonObject statusObject = (JsonObject) obj;
           
            updateStatus(statusObject);
            
          
        }else{

            getShowSubControl().set(false);

            getNodeControlIndicatorTooltip().setText("Unavailable");
            getNodeControlIndicator().setId("lblBlack");
            getNodeControlLabelField().setId("smallSecondaryColor");
            updateParamsBox(null);
        }


    }

    public void updateParamsBox(JsonObject json){
        if(getParamsBox() != null){

            getParamsBox().updateParameters(json);
        }
    }
    

    public void updateProgressBar(JsonObject statusObject){
     

        JsonElement networkBlockHeightElement = statusObject != null ? statusObject.get("maxPeerHeight") : null;
        JsonElement nodeBlockHeightElement = statusObject != null ? statusObject.get("fullHeight") : null;
        JsonElement headersBlockHeightElement = statusObject != null ? statusObject.get("headersHeight") : null;

        
    
        BigDecimal networkBlockHeight = networkBlockHeightElement != null && !networkBlockHeightElement.isJsonNull() ?  networkBlockHeightElement.getAsBigDecimal() : null;
        BigDecimal headersBlockHeight = headersBlockHeightElement != null && !headersBlockHeightElement.isJsonNull() ? headersBlockHeightElement.getAsBigDecimal() : null;
        BigDecimal nodeBlockHeight = nodeBlockHeightElement != null && !nodeBlockHeightElement.isJsonNull() ?  nodeBlockHeightElement.getAsBigDecimal() : null;
        


        networkBlockHeight = networkBlockHeight != null ? networkBlockHeight.multiply(BigDecimal.valueOf(2)) : null;
        
        boolean isNetworkBlockHeight = networkBlockHeight != null && networkBlockHeight.compareTo(BigDecimal.ZERO) > 0;
        boolean isHeadersBlockHeight = headersBlockHeight != null && headersBlockHeight.compareTo(BigDecimal.ZERO) > 0;
        boolean isNodeBlockHeight = nodeBlockHeight != null && nodeBlockHeight.compareTo(BigDecimal.ZERO) > 0;


        if(isNetworkBlockHeight && isHeadersBlockHeight){

            BigDecimal progressHeight = headersBlockHeight.add( isNodeBlockHeight? nodeBlockHeight : BigDecimal.ZERO);
            // double progressPercent = progressHeight.divide(networkBlockHeight) .doubleValue();
            BigDecimal progress = BigDecimal.ZERO;
            try {
                progress = progressHeight.divide(networkBlockHeight, 5, RoundingMode.HALF_UP);
               
                m_progressBar.setProgress(progress.doubleValue());
              
            } catch ( ArithmeticException e) {
                m_progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            }

          
        }else{
            
            m_progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        } 
        
    }

    public void updateStatus(JsonObject statusObject){
        if(m_topPropertiesBox == null){
            return;
        }
        JsonElement statusCodeElement = statusObject != null ? statusObject.get("statusCode") : null;

        if(statusCodeElement != null && statusCodeElement.isJsonPrimitive()){
            JsonElement syncedElement = statusObject.get("synced");
            JsonElement statusElement = statusObject.get("status");

            boolean synced = syncedElement != null ? syncedElement.getAsBoolean() : false;

            String lblColor = "smallSecondaryColor";
            String indicatorColor = "lblBlack";
            String indicatorString = "Unavailable";
           
            int statusCode = statusCodeElement.getAsInt();
            String msgString = statusElement != null ? statusElement.getAsString() : indicatorString;

         

            if(!synced && statusCode != App.STOPPED && statusCode != App.UPDATING){
                if(m_progressBar == null){
                    m_progressBar = new ProgressBar();
                
                    m_progressBar.prefWidthProperty().bind(this.m_topPropertiesBox.widthProperty().subtract(10));
                    m_topPropertiesBox.getChildren().add(m_progressBar);
                    
                }
                JsonElement infoElement = statusObject.get("information");
 
                if(infoElement != null && infoElement.isJsonObject()){
                    updateProgressBar(infoElement.getAsJsonObject());
                }
                
            }else{
                if(m_progressBar != null){
                    m_topPropertiesBox.getChildren().remove(m_progressBar);
                    m_progressBar = null;
               
                }

            }
           
            switch(statusCode){
                case App.STARTED:
                    lblColor = "smallPrimaryColor";
                    indicatorColor = synced ? "lblGreen" : "lblWhite";
                    indicatorString = "Running";
                    updateParamsBox(statusObject);
                   
                    if( !getConnectBtn().getText().equals(App.STOP)){
                        getConnectBtn().setText(App.STOP);
                        getConnectBtnTooltip().setText("Stop");
                    }
                break;
                case App.STARTING:
                    lblColor = "smallSecondaryColor";
                    indicatorColor = "lblGrey";
                    indicatorString = "Starting";
                    updateParamsBox(statusObject);
                    if( !getConnectBtn().getText().equals(App.STOP)){
                        getConnectBtn().setText(App.STOP);
                        getConnectBtnTooltip().setText("Stop");
                    }
                break;
                case App.UPDATING:
                    
                    lblColor = "smallSecondaryColor";
                    indicatorColor = "lblYellow";
                    indicatorString = "Updating";

                    if( !getConnectBtn().getText().equals(App.STOP)){
                        getConnectBtn().setText(App.STOP);
                        getConnectBtnTooltip().setText("Stop");
                    }
                    updateParamsBox(statusObject);
                break;
                case App.SUCCESS:
                case App.STOPPED:
                    lblColor = "smallSecondaryColor";
                    indicatorColor = "lblBlack";
                    indicatorString = "Stopped";
                    if(!getConnectBtn().getText().equals(App.PLAY)){
                        getConnectBtn().setText(App.PLAY);
                        getConnectBtnTooltip().setText("Start");
                    }
                    updateParamsBox(statusObject);
                break;
                default:
                    if( !getConnectBtn().getText().equals(App.STOP)){
                        getConnectBtn().setText(App.STOP);
                        getConnectBtnTooltip().setText("Stop");
                    }
                    updateParamsBox(null);
            }
            
            getNodeControlLabelField().setText(msgString);
            getNodeControlIndicatorTooltip().setText(indicatorString);
            getNodeControlIndicator().setId(indicatorColor);
            getNodeControlLabelField().setId(lblColor);
            
           
            
        }else{
            getShowSubControl().set(false);
            getNodeControlIndicatorTooltip().setText("Unavailable");
            getNodeControlIndicator().setId("lblBlack");
            getNodeControlLabelField().setId("smallSecondaryColor");
            updateParamsBox(null);
        }
    }

    public void run(){
        JsonObject note = Utils.getCmdObject("run");
        note.addProperty("accessId", getAccessId());
        getNodeInterface().sendNote(note);
      
    }

    public void terminate(){
        JsonObject note = Utils.getCmdObject("terminate");
        note.addProperty("accessId", getAccessId());
        getNodeInterface().sendNote(note);
    }

    


    @Override
    public void shutdown(){
        super.shutdown();
    }

    public void updateError(JsonObject json){
        JsonElement msgElement = json != null ? json.get("status") : null;
        String errorString = msgElement != null ? msgElement.getAsString() : "Code 0";
        getNodeControlLabelField().setText("Error: " + errorString);
        
    }
    JsonParser m_jsonParser = new JsonParser();

    @Override
    public void sendMessage(int code, long timestamp, String networkId, String msg) {
       
        JsonElement msgElement = msg != null ? m_jsonParser.parse(msg) : null;
        JsonObject dataObject = msgElement != null && msgElement.isJsonObject() ? msgElement.getAsJsonObject() : null;

        switch(code){
            case App.ERROR:
                updateError(dataObject != null  ? dataObject : null);       
            break;
            case App.STATUS:
            
                updateStatus(dataObject != null  ? dataObject : null);
            break;
        }
        
    }

    @Override
    public void sendMessage(int code, long timestamp,String networkId, Number number) {
        switch(code){
            case ErgoNodeLocalData.INSTALL_PROGRESS:
                if(!getNodeControlIndicatorTooltip().getText().equals("Updating")){
                    getNodeControlIndicatorTooltip().setText("Updating");
                    
                    getNodeControlIndicator().setId("lblYellow");
                    getNodeControlLabelField().setId("smallSecondaryColor");
                    getStatus();
                    
                }
                getNodeControlLabelField().setText("Updating: " + String.format("%.1f", number.doubleValue() * 100) + "%");
            break;
        }

    }
    
}
