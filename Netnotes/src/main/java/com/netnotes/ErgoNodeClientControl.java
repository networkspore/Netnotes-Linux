package com.netnotes;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

import com.google.gson.JsonObject;
import com.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import com.google.gson.JsonElement;

public class ErgoNodeClientControl extends AppBox{

    private String m_accessId;
    private NoteInterface m_nodeInterface;
    private Tooltip m_nodeControlIndicatorTooltip = new Tooltip();
    private Label m_nodeControlIndicator = new Label("⬤");
    private Text m_nodeControlClientTypeText = new Text("");
    private TextField m_nodeControlLabelField = new TextField();
    private Label m_nodeControlShowBtn = new Label("⏵ ");
    private SimpleBooleanProperty m_showSubControl = new SimpleBooleanProperty(false);
    private JsonParametersBox m_paramsBox = null;
    private Label m_connectBtn = new Label("🗘");
    private VBox m_propertiesBox;
    private HBox m_headingBtnBox;
    private HBox m_statusPaddingBox;
    private Tooltip m_connectBtnTooltip;

    public HBox getStatusPaddingBox() {
        return m_statusPaddingBox;
    } 
    
    public Tooltip getConnectBtnTooltip() {
        return m_connectBtnTooltip;
    }


    public HBox getHeadingBtnBox() {
        return m_headingBtnBox;
    }


    public String getAccessId() {
        return m_accessId;
    }


    public VBox getTopPropertiesBox() {
        return m_propertiesBox;
    }




    public NoteInterface getNodeInterface() {
        return m_nodeInterface;
    }




    public Tooltip getNodeControlIndicatorTooltip() {
        return m_nodeControlIndicatorTooltip;
    }




    public Label getNodeControlIndicator() {
        return m_nodeControlIndicator;
    }


    public Text getNodeControlClientTypeText() {
        return m_nodeControlClientTypeText;
    }


    public TextField getNodeControlLabelField() {
        return m_nodeControlLabelField;
    }


    public Label getNodeControlShowBtn() {
        return m_nodeControlShowBtn;
    }



    public SimpleBooleanProperty getShowSubControl() {
        return m_showSubControl;
    }



    public JsonParametersBox getParamsBox() {
        return m_paramsBox;
    }




    public Label getConnectBtn() {
        return m_connectBtn;
    }



    

    @Override
    public void shutdown() {
     
        if (m_paramsBox != null) {
        
            m_paramsBox.shutdown();
          
            m_propertiesBox.getChildren().remove(m_paramsBox);
            m_paramsBox = null;
          
        }
    }

    
 

    public void getStatus(){
        JsonObject json = m_nodeInterface.getJsonObject();
        JsonElement namedNodeElement = json.get("namedNode");

        NamedNodeUrl namedNode = null;
        try {
            namedNode = namedNodeElement != null && namedNodeElement.isJsonObject() ? (new NamedNodeUrl(namedNodeElement.getAsJsonObject())) : null;
        } catch (Exception e) {

        }
        
        m_nodeControlLabelField.setText(namedNode != null ? namedNode.getUrlString() : "Setup required");

        m_nodeControlIndicator.setId("lblGrey");
            
        JsonObject note = Utils.getCmdObject("getStatus");
        note.addProperty("accessId", m_accessId);

        //m_nodeInterface
        m_nodeInterface.sendNote(note, (onSucceeded)->{
            Object obj = onSucceeded.getSource().getValue();
 
            if(obj != null && obj instanceof  JsonObject ){
                JsonObject statusObject = (JsonObject) obj;
                JsonElement syncedElement = statusObject.get("synced");
                boolean synced = syncedElement != null ? syncedElement.getAsBoolean() : false;
                if(m_paramsBox != null){
                    m_paramsBox.updateParameters(statusObject);
                };
                
                
                m_nodeControlIndicatorTooltip.setText(synced ? "Synced" : "Unsynced");
                m_nodeControlIndicator.setId(synced ? "lblGreen" : "lblGrey");
                m_nodeControlLabelField.setId("smallPrimaryColor");
                
            }else{
                m_showSubControl.set(false);

                m_nodeControlIndicatorTooltip.setText( "Unavailable");
                m_nodeControlIndicator.setId("lblBlack");
                m_nodeControlLabelField.setId("smallSecondaryColor");
            }
        }, (onFailed)->{
            m_showSubControl.set(false);
            m_nodeControlIndicator.setId("lblBlack");
            m_nodeControlLabelField.setId("smallSecondaryColor");
            m_nodeControlIndicatorTooltip.setText("Error: " + onFailed.getSource().getException().toString());
        });

    }

    

    public NamedNodeUrl getNamedNodeUrl() {
        JsonElement namedNodeElement = m_nodeInterface.getJsonObject().get("namedNode");
        
        try {
            return namedNodeElement != null ? new NamedNodeUrl(namedNodeElement.getAsJsonObject()) : null;
        } catch (Exception e) {
            return null;
        }
    }


    public ErgoNodeClientControl(NoteInterface noteInterface, NamedNodeUrl namedNodeUrl, String clientType, String accessId){
        super();
        m_connectBtnTooltip = new Tooltip("Refresh");
        m_connectBtnTooltip.setShowDelay(Duration.millis(100));

      

        m_accessId = accessId;
        m_nodeInterface = noteInterface;
        m_nodeControlShowBtn.setId("caretBtn");
        m_nodeControlShowBtn.setMinWidth(25);
        
        m_nodeControlShowBtn.setOnMouseClicked(e->{
          
            m_showSubControl.set(!m_showSubControl.get());
            
        });


        m_nodeControlIndicatorTooltip.setShowDelay(new Duration(100));
        m_nodeControlIndicatorTooltip.setText("Unavailable");

        m_nodeControlIndicator.setTooltip(m_nodeControlIndicatorTooltip);
        m_nodeControlIndicator.setId("lblBlack");
        m_nodeControlIndicator.setPadding(new Insets(0,0,3,0));


        m_nodeControlLabelField.setText(namedNodeUrl.getUrlString());
        m_nodeControlLabelField.setEditable(false);
        m_nodeControlLabelField.setId("smallSecondaryColor");
        m_nodeControlLabelField.setPadding(new Insets(0,10,0,0));
        HBox.setHgrow(m_nodeControlLabelField,Priority.ALWAYS);
    
        Region nodeControlGrowRegion = new Region();
        HBox.setHgrow(nodeControlGrowRegion, Priority.ALWAYS);

        m_nodeControlClientTypeText.setText(clientType);
        m_nodeControlClientTypeText.setFont(App.txtFont);
        m_nodeControlClientTypeText.setFill(App.txtColor);

        
        
        m_connectBtn.setId("lblBtn");
        m_connectBtn.setOnMouseClicked(e->getStatus());
        m_connectBtn.setTooltip(getConnectBtnTooltip());

        m_headingBtnBox = new HBox(m_connectBtn);
        m_headingBtnBox.setPadding(new Insets(0, 5, 0, 5));
        m_headingBtnBox.setAlignment(Pos.CENTER);

        m_statusPaddingBox = new HBox(m_nodeControlLabelField, m_nodeControlIndicator);
        m_statusPaddingBox.setAlignment(Pos.CENTER_LEFT);
        m_statusPaddingBox.setId("bodyBox");
        m_statusPaddingBox.setMaxHeight(App.MAX_ROW_HEIGHT);
        m_statusPaddingBox.setPadding(new Insets(2,5,2,0));

        HBox.setHgrow(m_statusPaddingBox, Priority.ALWAYS);

        Region clientTypeTextSpacer = new Region();
        clientTypeTextSpacer.setMinWidth(10);

        HBox topBox = new HBox(m_nodeControlShowBtn, m_nodeControlClientTypeText, clientTypeTextSpacer, m_statusPaddingBox, m_headingBtnBox);
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setMinHeight(App.ROW_HEIGHT);
        HBox.setHgrow(topBox, Priority.ALWAYS);



      

        m_propertiesBox = new VBox();
        getChildren().addAll(topBox, m_propertiesBox);


        m_showSubControl.addListener((obs,oldval,newval)->{
            m_nodeControlShowBtn.setText(newval ? "⏷ " : "⏵ ");
            
            if (newval) {
                if(m_paramsBox == null){
                    m_paramsBox = new JsonParametersBox(Utils.getJsonObject("Status", "Updating..."));
                    m_paramsBox.setPadding(new Insets(0,10,0,5));
       
                    m_propertiesBox.getChildren().add(m_paramsBox);
                    getStatus();
                }
            } else {
              
               shutdown();
        
            }
        });

    

        getStatus();
    }

    
    
}