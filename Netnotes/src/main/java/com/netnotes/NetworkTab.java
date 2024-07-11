package com.netnotes;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class NetworkTab extends VBox implements TabInterface {
    
    public static final String NAME = "Network";

    private Button m_menuBtn;

    
    private boolean m_current = false;
    private SimpleStringProperty m_selectedNetworkId = new SimpleStringProperty();
   // private ChangeListener<String> m_selectedStringListener = null;

   
    public NetworkTab(Stage appStage, NetworksData networksData, SimpleDoubleProperty heightObject, SimpleDoubleProperty widthObject, Button menuBtn){
        super();

        m_menuBtn = menuBtn;      
        prefWidthProperty().bind(widthObject);
        prefHeightProperty().bind(heightObject);
        setAlignment(Pos.CENTER);
        
        m_selectedNetworkId.set(networksData.currentNetworkProperty().get() == null ? NetworksData.NO_NETWORK.getNetworkId() : networksData.currentNetworkProperty().get().getNetworkId());

        ImageView btnImageView = new ImageView(App.globeImg);
      
        btnImageView.setPreserveRatio(true);
        btnImageView.fitHeightProperty().bind(heightObject.multiply(.2));
        
        HBox imgBox = new HBox(btnImageView);
     
        imgBox.setPadding(new Insets(5));

        Text networkName = new Text("Network");
        networkName.setFont(App.mainFont);
        networkName.setFill(Color.WHITE);

   

        HBox imgcenteringBox = new HBox(imgBox);
        imgcenteringBox.setAlignment(Pos.CENTER);

        HBox networkNameBox = new HBox(networkName);
        networkNameBox.setAlignment(Pos.CENTER);
        networkNameBox.setPadding(new Insets(8,0,10,0));

        Region hTitleRule = new Region();
        hTitleRule.setMinHeight(2);
        hTitleRule.setMaxWidth(100);
        hTitleRule.setMinWidth(100);
        hTitleRule.setId("hGradient");

        VBox infoBox = new VBox(imgcenteringBox, networkNameBox, hTitleRule);
        infoBox.setPadding(new Insets(10));
        infoBox.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
    

        HBox networkOptionsBox = new HBox();
        networkOptionsBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(networkOptionsBox, Priority.ALWAYS);
        networkOptionsBox.setPadding(new Insets(0,0,0,0));
        
        Region btnTopSpaceRegion = new Region();
        btnTopSpaceRegion.prefHeightProperty().bind(heightObject.multiply(.18).subtract(20));
        btnTopSpaceRegion.setMinHeight(10);
        Button selectBtn = new Button("Select");
        selectBtn.setId("roundBox"); 
        HBox selectBtnBox = new HBox(selectBtn);
        selectBtnBox.setAlignment(Pos.CENTER);
       

        Region btnSpaceRegion = new Region();
        btnSpaceRegion.prefHeightProperty().bind(heightObject.multiply(.25).subtract(20));
        btnSpaceRegion.setMinHeight(10);

        VBox bodyBox = new VBox(btnTopSpaceRegion, networkOptionsBox,btnSpaceRegion, selectBtnBox);
        HBox.setHgrow(bodyBox, Priority.ALWAYS);

        Region topRegion = new Region();
        Region botRegion = new Region();

        VBox.setVgrow(topRegion, Priority.ALWAYS);
        VBox.setVgrow(botRegion, Priority.ALWAYS);
        
        getChildren().addAll(topRegion, infoBox, bodyBox, botRegion);

    
        networkOptionsBox.getChildren().clear();
       
        String selectedId = m_selectedNetworkId.get() == null ? NetworksData.NO_NETWORK.getNetworkId() : m_selectedNetworkId.get();

        ImageView noImgView = new ImageView();
        noImgView.setImage(new Image( NetworksData.NO_NETWORK.iconString()));
        noImgView.setPreserveRatio(true);
        noImgView.setFitWidth(50);
        
        Button noBtn = new Button( NetworksData.NO_NETWORK.getNetworkName(), noImgView);
        noBtn.setGraphic(noImgView);
        noBtn.setContentDisplay(ContentDisplay.TOP);
        noBtn.setTextAlignment(TextAlignment.CENTER);
        
        noBtn.setId( NetworksData.NO_NETWORK.getNetworkId().equals(selectedId) ? "iconBtnSelected" :  "iconBtn");
    
        noBtn.setOnAction(e->{
            
            networkOptionsBox.getChildren().forEach(item->{
            
                if(!item.getId().equals("iconBtn")){ 
                    item.setId("iconBtn");
                    
                }
            });
            noBtn.setId("iconBtnSelected");
            m_selectedNetworkId.set( NetworksData.NO_NETWORK.getNetworkId());
            
        
        });

        networkOptionsBox.getChildren().add(noBtn);


        

        for(int i = 0; i < NetworksData.SUPPORTED_NETWORKS.length ; i++){
            
            NetworkInformation supportedNetwork = NetworksData.SUPPORTED_NETWORKS[i];
            ImageView imgView = new ImageView();
            imgView.setImage(new Image(supportedNetwork.iconString()));
            imgView.setPreserveRatio(true);
            imgView.setFitWidth(50);
            
            Button btn = new Button(supportedNetwork.getNetworkName(), imgView);
            btn.setGraphic(imgView);
            btn.setContentDisplay(ContentDisplay.TOP);
            btn.setTextAlignment(TextAlignment.CENTER);
            
            btn.setId(supportedNetwork.getNetworkId().equals(selectedId) ? "iconBtnSelected" :  "iconBtn");
        
            btn.setOnAction(e->{
                
                networkOptionsBox.getChildren().forEach(item->{
                
                    if(!item.getId().equals("iconBtn")){ 
                        item.setId("iconBtn");
                        
                    }
                });
                btn.setId("iconBtnSelected");
                m_selectedNetworkId.set(supportedNetwork.getNetworkId());
                
            
            });
            
            networkOptionsBox.getChildren().add(btn);

        }
    

      //  m_selectedNetworkId.addListener(m_selectedStringListener);

        selectBtn.setOnAction(e->{
            String selectedNetworkId = m_selectedNetworkId.get();
            networksData.installNetwork(selectedNetworkId);

            networksData.setCurrentNetwork(selectedNetworkId);
            
            networksData.open(selectedNetworkId, App.NETWORK_TYPE);
        });
    }

 

    public String getTabId(){
        return NAME;
    }
    public String getName(){
        return NAME;
    }
    
    public void setCurrent(boolean value){
        m_current = value;
        m_menuBtn.setId(value ? "activeMenuBtn" : "menuTabBtn");
        
      //  m_networkImgView.setImage(newval == null ? m_globeImage : newval.getSmallAppIcon());
     //   m_networkToolTip.setText(newval == null ? "Network: Select": newval.getName());
    }

    
    public boolean getCurrent(){
        
        return m_current;
    } 
    
    public String getType(){
        return App.STATIC_TYPE;
    }

    public boolean isStatic(){
        return getType().equals(App.STATIC_TYPE);
    }
    private SimpleStringProperty m_titleProperty = new SimpleStringProperty(NAME);

    public SimpleStringProperty titleProperty(){
        return m_titleProperty;
    }

    public void shutdown(){
        /*if(m_selectedStringListener != null){
            m_selectedNetworkId.addListener(m_selectedStringListener);
            m_selectedStringListener = null;
        }*/
        this.prefHeightProperty().unbind();
        this.prefWidthProperty().unbind();
    }
}

    

