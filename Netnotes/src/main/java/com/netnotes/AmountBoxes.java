package com.netnotes;

import java.util.ArrayList;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AmountBoxes extends VBox {

    public final static String ADD_TO_LAST_ROW = "ADD_TO_LAST_ROW";
    public final static String ADD_AS_LAST_ROW = "ADD_AS_LAST_ROW";
    

    public final static int IMAGE_WIDTH = 40;

    private ObservableList<AmountBoxInterface> m_amountsList = FXCollections.observableArrayList();


    private VBox m_listVBox = new VBox();
    private final SimpleObjectProperty<Insets> m_paddingInsets =new SimpleObjectProperty<>( new Insets(0,0,0,0));
    private Node m_lastRowItem = null;
    private String m_lastRowItemStyle = ADD_AS_LAST_ROW;
    private boolean m_lastRowItemDisabled = false;
    public AmountBoxes(){
        super();
    }

    public AmountBoxes( AmountBoxInterface... boxes) {
        super();
       //m_addressData = addressData;
      //  HBox.setHgrow(m_listVBox, Priority.ALWAYS);
       
        init(boxes);

    }

    public void init(AmountBoxInterface... boxes){
            
        getChildren().add(m_listVBox);

        m_amountsList.addListener((ListChangeListener.Change<? extends AmountBoxInterface> c) -> updateGrid());
        m_paddingInsets.addListener((obs, oldval, newval)->updateGrid());
        if(boxes != null && boxes.length > 0){
            for(int i = 0; i < boxes.length; i++){
                add(boxes[i]);
            }
        }else{
            updateGrid();
        }
    }


    public ObservableList<AmountBoxInterface> amountsList(){
        return m_amountsList;
    } 

    public void shutdown(){
        m_amountsList.forEach(box ->{
            box.shutdown();
        });
        m_amountsList.clear();
        m_listVBox.getChildren().clear();
    }


    public void clear(){
        shutdown();
        m_amountsList.clear();
    }

    public void setLastRowItem(Node item, String itemStyle){
        m_lastRowItem = item;
        m_lastRowItemStyle = itemStyle;
        updateGrid();
    }

    public Node getLastRowItem(){
        return m_lastRowItem;
    }

    public void setLastRowItemStyle(String itemStyle){
        m_lastRowItemStyle = itemStyle;
        updateGrid();
    }

    public String getLastRowItemStyle(){
        return m_lastRowItemStyle;
    }

    public void setLastRowItemDisabled(boolean disabled){
        m_lastRowItemDisabled = disabled;
        updateGrid();
    }

    public boolean getLastRowItemDisabled(){
        return m_lastRowItemDisabled;
    }

    public SimpleObjectProperty<Insets> amountBoxPaddingProperty(){
        return m_paddingInsets;
    }

  

    public VBox getListVBox(){
        return m_listVBox;
    }

    public ObservableList<AmountBoxInterface> getAmountBoxList(){
        return m_amountsList;
    }

    public AmountBoxInterface[] getAmountBoxArray(){
        int size = m_amountsList.size();
        if(size == 0){
            return null;
        }
        AmountBoxInterface[] amountBoxes = new AmountBoxInterface[size];
        amountBoxes = m_amountsList.toArray(amountBoxes);
        return amountBoxes;
    }


    public void add(AmountBoxInterface amountBox){
        if(amountBox != null && amountBox instanceof HBox){
            HBox existingBox = (HBox) getAmountBox(amountBox.getTokenId());
            if(existingBox == null){
                m_amountsList.add(amountBox);    
            }
        }
    }
    private ArrayList<String> m_removeList  = new ArrayList<>();
    public void removeOld(long timeStamp){
        
        m_removeList.clear();
        for(AmountBoxInterface amountBox : m_amountsList){
            if(amountBox.getTimeStamp() < timeStamp){
                m_removeList.add(amountBox.getTokenId());        
            }
        }

        for(String tokenId : m_removeList){
            removeAmountBox(tokenId);
        }
        m_removeList.clear();
    }

    public void removeAmountBox(String tokenId){
        if(tokenId != null){
            int size = m_amountsList.size();
        

            for(int i = 0; i < size; i++){
                AmountBoxInterface amountBox = m_amountsList.get(i);
                String amountBoxTokenId = amountBox.getTokenId();

                if(amountBoxTokenId != null && amountBoxTokenId.equals(tokenId)){
                    amountBox.shutdown();
                    m_amountsList.remove(amountBox);
                    break;
                }
            }
        }
    }

    public AmountBoxInterface getAmountBox(String tokenId){
        if(tokenId != null){
            int size = m_amountsList.size();
        

            for(int i = 0; i < size; i++){
                AmountBoxInterface amountBox = m_amountsList.get(i);
                String amountBoxTokenId = amountBox.getTokenId();

                if(amountBoxTokenId != null && amountBoxTokenId.equals(tokenId)){
                    return amountBox;
                }
            }
        }

        return null;
    }

    public void updateGrid(){
        m_listVBox.getChildren().clear();

        for(int i = 0; i < m_amountsList.size(); i++){

            HBox amountBox = (HBox) m_amountsList.get(i);
            HBox.setHgrow(amountBox, Priority.ALWAYS);
            
            Insets padding = m_paddingInsets.get();

            HBox paddingBox = new HBox(amountBox);
            paddingBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(paddingBox, Priority.ALWAYS);
            paddingBox.setPadding(padding);

            
            m_listVBox.getChildren().add(paddingBox);

            if(m_lastRowItem != null && !m_lastRowItemDisabled && m_lastRowItemStyle != null){
                switch(m_lastRowItemStyle){
                    case ADD_TO_LAST_ROW:
                        if(i == m_amountsList.size() - 1){
                            paddingBox.getChildren().add(m_lastRowItem);
                        }
                    break;
                }
            }

        }
        if(m_lastRowItem != null && !m_lastRowItemDisabled && m_lastRowItemStyle != null){
                switch(m_lastRowItemStyle){
                    case ADD_AS_LAST_ROW:
                        
                            HBox lastRowPaddingBox = new HBox(m_lastRowItem);
                            HBox.setHgrow(lastRowPaddingBox, Priority.ALWAYS);
                            Insets lastRowPadding = m_paddingInsets.get();
                            
                            lastRowPaddingBox.setPadding(lastRowPadding);
                            m_listVBox.getChildren().add(lastRowPaddingBox);
                        
                    break;
                }
            }
       
    }
}
