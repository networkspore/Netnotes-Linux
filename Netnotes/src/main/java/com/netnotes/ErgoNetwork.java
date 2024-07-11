package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonObject;
import com.devskiller.friendly_id.FriendlyId;
import com.utils.Utils;

import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;

import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class ErgoNetwork extends Network implements NoteInterface {

    public final static String NAME = "Ergo Network";
    public final static String DESCRIPTION = "A layer 0, smart contract enabled P2P blockchain network.";
    public final static String SUMMARY = "";
    public final static String NETWORK_ID = "ERGO_NETWORK";


    public final static String[] INTALLABLE_NETWORK_IDS = new String[]{
        ErgoExplorers.NETWORK_ID,
        ErgoTokens.NETWORK_ID,
        ErgoNodes.NETWORK_ID,
        ErgoWallets.NETWORK_ID
    };

    public String getType(){
        return App.NETWORK_TYPE;
    }

    private File m_appDir = null;

    private NetworkType m_networkType = NetworkType.MAINNET;

   // private File logFile = new File("netnotes-log.txt");
    private ErgoNetworkData m_ergNetData = null;

    
  //  private Image m_balanceImage = new Image("/assets/balance-list-30.png");
 //   private Image m_txImage = new Image("/assets/transaction-list-30.png");
//    private Image m_sendImage = new Image("/assets/arrow-send-white-30.png");

    
    private File m_networksDir;

    //private SimpleBooleanProperty m_shuttingdown = new SimpleBooleanProperty(false);
    public ErgoNetwork(NetworksData networksData) {
        super(new Image(getAppIconString()), NAME, NETWORK_ID, networksData);
        m_networksDir = new File (getNetworksData().getAppDir().getAbsolutePath() +"/networks");
        if(!m_networksDir.isDirectory()){
            try {
                Files.createDirectory(m_networksDir.toPath());
            } catch (IOException e) {
                try {
                    Files.writeString(App.logFile.toPath(), "Cannot create network directory: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
                    
                }
            }
        }
        m_appDir = new File( m_networksDir.getAbsolutePath() + "/Ergo");

        if(!m_appDir.isDirectory()){
            try {
                Files.createDirectory(m_appDir.toPath());
            } catch (IOException e) {
                try {
                    Files.writeString(App.logFile.toPath(), "Cannot create ergo directory: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
                    
                }
            }
        }
    }
  


    public String getDescription(){
        return DESCRIPTION;
    }

    public File getAppDir(){
        if(m_appDir == null){
            m_appDir = new File(getNetworksData().getAppDir().getAbsolutePath() + "/" + "Ergo Network");
        }

        if(!m_appDir.isDirectory()){
            try {
                Files.createDirectory(m_appDir.toPath());
            } catch (IOException e) {
                // TODO Auto-generated catch block
               
            }
        }

        return m_appDir;
    }

    private Image m_smallAppIcon = new Image(getSmallAppIconString());

    public Image getSmallAppIcon() {
        return m_smallAppIcon;
    }

    public static String getAppIconString(){
        return "/assets/ergo-network.png";
    }

    public static String getSmallAppIconString(){
        return "/assets/ergo-network-30.png";
    }
    
    @Override
    public JsonObject getJsonObject() {

        JsonObject networkObj = super.getJsonObject();
        networkObj.addProperty("networkType", m_networkType.toString());
        networkObj.add("stage", getStageJson());
        return networkObj;

    }

    public NetworkType getNetworkType(){
        return m_networkType;
    }

    private ChangeListener<Number> m_networkUpdateListener = null;

    @Override
    protected void start(){
        if(getConnectionStatus() == App.STOPPED){
            super.start();
            m_ergNetData = new ErgoNetworkData(this);
        }
    }

    @Override
    protected void stop(){
        super.stop();
        if(m_ergNetData != null){
            m_ergNetData.shutdown();
            m_ergNetData = null;
        }
    }


    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {


        Utils.returnObject(sendNote(note), getNetworksData().getExecService(), onSucceeded, onFailed);
                  

        return false;
    }

    @Override
    public Object sendNote(JsonObject note){
        //JsonElement subjecElement = note.get("subject");

        return null;
    }






    public static NetworkInformation getNetworkInformation(){
        return new NetworkInformation(NETWORK_ID, NAME, getAppIconString(), getSmallAppIconString(), DESCRIPTION);
    }


    public final String ADDRESS_LOCKED =  "[ Locked ]";


    @Override
    public TabInterface getTab(Stage appStage,  SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject, Button networkBtn){

        return new NetworkTabInterface(getNetworkId(), getName(), heightObject, widthObject, networkBtn){
           

            private ScrollPane m_walletScroll;
            private ChangeListener<Bounds> m_boundsChange;
            private ErgoWalletsRowArea m_ergoWalletsRowArea = null;
            

            @Override
            public void init(){

                double maxWidth = 400;
                
                setMaxWidth(maxWidth);
              
                m_ergoWalletsRowArea = new ErgoWalletsRowArea(appStage, m_ergNetData);
                
                getChildren().addAll(m_ergoWalletsRowArea);
                

                SimpleBooleanProperty nodeAvailableObject = new SimpleBooleanProperty(false);
                SimpleStringProperty nodeStatusObject = new SimpleStringProperty(null);

                /////////


                HBox nodeAvailableBox = new HBox();
                nodeAvailableBox.setMaxHeight(15);
                nodeAvailableBox.setMinWidth(10);
                nodeAvailableBox.setId("offlineBtn");
                nodeAvailableBox.setFocusTraversable(true);
                nodeAvailableBox.setOnMouseClicked(e->{
                  
                });

                Binding<String> nodeAvailableStringBinding = Bindings.createObjectBinding(()->(nodeAvailableObject.get()  ? "Available" : "Unavailable"), nodeAvailableObject);

                Tooltip nodeAvailableTooltip = new Tooltip("Offline");
                nodeAvailableTooltip.setShowDelay(new javafx.util.Duration(100));

                Binding<String> nodeStatusStringBinding = Bindings.createObjectBinding( ()->nodeStatusObject.get() == null ? "Offline" : nodeStatusObject.get(),  nodeStatusObject);

                nodeAvailableTooltip.textProperty().bind(Bindings.concat(nodeAvailableStringBinding, " - ", nodeStatusStringBinding));
                Tooltip.install(nodeAvailableBox, nodeAvailableTooltip);
                

                nodeAvailableObject.addListener((obs,oldVal,newVal)->{
                    nodeAvailableBox.setId(newVal ? "onlineBtn" : "offlineBtn");
                });
                

                
                final String nodeBtnDefaultString = "[ select ]";
                MenuButton nodeBtn = new MenuButton(nodeBtnDefaultString);
                nodeBtn.setMaxHeight(40);
                nodeBtn.setContentDisplay(ContentDisplay.LEFT);
                nodeBtn.setAlignment(Pos.CENTER_LEFT);
                nodeBtn.setText(nodeBtnDefaultString);
                nodeBtn.setMinWidth(90);
                nodeBtn.setTooltip(nodeAvailableTooltip);
                nodeBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, e->{
                    nodeBtn.show();
                });

                Text nodeText = new Text("Node ");
                nodeText.setFont(App.txtFont);
                nodeText.setFill(App.txtColor);

            

                HBox nodeBtnBox = new HBox(nodeBtn, nodeAvailableBox);
                HBox.setHgrow(nodeBtnBox, Priority.ALWAYS);
                nodeBtnBox.setId("darkBox");
                nodeBtnBox.setAlignment(Pos.CENTER_LEFT);
                nodeBtnBox.setPadding(new Insets(0,3,0,0));

                nodeBtn.prefWidthProperty().bind(nodeBtnBox.widthProperty());

                HBox nodeBox = new HBox(nodeText, nodeBtnBox);
                nodeBox.setPadding(new Insets(3,3,3,5));
                HBox.setHgrow(nodeBox, Priority.ALWAYS);
                nodeBox.setAlignment(Pos.CENTER_LEFT);

                



                Runnable updateNodeBtn = () ->{
                    
                   // ErgoNodeData nodeData =  m_ergNetData.selectedNodeData().get();
                  //  nodeBtn.setText(nodeData != null ? nodeData.getName() : "...");
                    
                };
                ChangeListener<Boolean> isAvailableListener = (obs,oldval,newval)->nodeAvailableObject.set(newval);
                ChangeListener<String> statusListener = (obs,oldVal, newVal)->{
                  //  ErgoNodeData ergoNodeData =  m_ergNetData.selectedNodeData().get();;
                    
                   // nodeStatusObject.set(ergoNodeData == null ? null : ergoNodeData.statusString().get());
                    
                };

                ChangeListener<ErgoNodeData> selectedNodeListener = (obs,oldval,newval) -> {
                // nodeAvailableObject.unbind();
                // nodeStatusObject.unbind();
                
                    if(oldval != null){
                        oldval.isAvailableProperty().removeListener(isAvailableListener);
                        oldval.statusString().removeListener(statusListener);
                    }
                    if(newval != null){
                        if(newval.isStopped()){ 
                            newval.start();
                        }
                        //nodeAvailableListenerstatusListener
                        newval.isAvailableProperty().addListener(isAvailableListener);
                        newval.statusString().addListener(statusListener);
                        
                        nodeAvailableObject.set(newval.isAvailable());
                    //  nodeStatusObject.bind(newval.nodeStatusInfo());
                    }
                   
                     //   ergoWalletData.setNodesId(newval == null ? null : newval.getId());
                
                    updateNodeBtn.run();
                    
                    
                };

               

      

                Region nodeBoxSpacer = new Region();
                nodeBoxSpacer.setMinHeight(5);




        

        
            }
            
            @Override
            public void shutdown(){
                super.shutdown();
                if(m_ergoWalletsRowArea != null){
                    getChildren().remove(m_ergoWalletsRowArea);
                    m_ergoWalletsRowArea.shutdown();
                    m_ergoWalletsRowArea = null;
                }
               
                /*
                    shutdownMenu.fire();
                    AddressesData addressesData = m_addressesDataObject.get();
                    if(addressesData != null){
                        addressesData.shutdown();
                    }
                
                    m_addressesDataObject.removeListener(addressesDataObjChangeListener);
                 */
                if(m_boundsChange != null && m_walletScroll != null){
                    m_walletScroll.layoutBoundsProperty().removeListener(m_boundsChange);
                    m_boundsChange = null;
                }
                
            }

            @Override
            public void setCurrent(boolean value){
                super.setCurrent(value);

            }
        };
    }

      


}
