package com.netnotes;

import java.time.LocalDateTime;
import java.util.ArrayList;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AmountBoxes extends VBox {

    public final static String ADD_TO_LAST_ROW = "ADD_TO_LAST_ROW";
    public final static String ADD_AS_LAST_ROW = "ADD_AS_LAST_ROW";
    

    public final static int IMAGE_WIDTH = 40;

    private ObservableList<AmountBox> m_amountsList = FXCollections.observableArrayList();


    private VBox m_listVBox = new VBox();
    private final SimpleObjectProperty<Insets> m_paddingInsets =new SimpleObjectProperty<>( new Insets(0,0,10,10));
    private Node m_lastRowItem = null;
    private String m_lastRowItemStyle = ADD_AS_LAST_ROW;
    private boolean m_lastRowItemDisabled = false;

    private SimpleObjectProperty<PriceQuote> m_priceQuoteProperty = new SimpleObjectProperty<>(null);
    

    public AmountBoxes(AmountBox... boxes) {
        super();
       //m_addressData = addressData;
      //  HBox.setHgrow(m_listVBox, Priority.ALWAYS);
        
        
        getChildren().add(m_listVBox);

        m_amountsList.addListener((ListChangeListener.Change<? extends AmountBox> c) -> update());
        m_paddingInsets.addListener((obs, oldval, newval)->update());
        if(boxes != null && boxes.length > 0){
            for(int i = 0; i < boxes.length; i++){
                add(boxes[i]);
            }
        }else{
            update();
        }

    }

    public ObservableList<AmountBox> amountsList(){
        return m_amountsList;
    } 


    public void clear(){
        m_amountsList.clear();
    }

    public void setLastRowItem(Node item, String itemStyle){
        m_lastRowItem = item;
        m_lastRowItemStyle = itemStyle;
        update();
    }

    public Node getLastRowItem(){
        return m_lastRowItem;
    }

    public void setLastRowItemStyle(String itemStyle){
        m_lastRowItemStyle = itemStyle;
        update();
    }

    public String getLastRowItemStyle(){
        return m_lastRowItemStyle;
    }

    public void setLastRowItemDisabled(boolean disabled){
        m_lastRowItemDisabled = disabled;
        update();
    }

    public boolean getLastRowItemDisabled(){
        return m_lastRowItemDisabled;
    }

    public SimpleObjectProperty<Insets> amountBoxPaddingProperty(){
        return m_paddingInsets;
    }

    public ObservableList<AmountBox> getAmountBoxList() {
        return m_amountsList;
    }

    public VBox getListVBox(){
        return m_listVBox;
    }

    public AmountBox[] getAmountBoxArray(){
        int size = m_amountsList.size();
        if(size == 0){
            return null;
        }
        AmountBox[] amountBoxes = new AmountBox[size];
        amountBoxes = m_amountsList.toArray(amountBoxes);
        return amountBoxes;
    }


    public void add(AmountBox amountBox){
        if(amountBox != null){
            AmountBox existingBox = getAmountBox(amountBox.getTokenId());
            if(existingBox == null){
                m_amountsList.add(amountBox);
             
            }else{ 
                PriceAmount newPriceAmount = amountBox.priceAmountProperty().get();
                existingBox.priceAmountProperty().set(newPriceAmount);
            }
        }
    }

    public void removeOld(long timeStamp){
        ArrayList<String> removeList = new ArrayList<>();

        for(AmountBox amountBox : m_amountsList){
            if(amountBox.getTimeStamp() < timeStamp){
                removeList.add(amountBox.getTokenId());        
            }
        }

        for(String tokenId : removeList){
            removeAmountBox(tokenId);
        }
    }

    public void removeAmountBox(String tokenId){
        if(tokenId != null){
            int size = m_amountsList.size();
            AmountBox[] amountBoxes = new AmountBox[size];
            amountBoxes = m_amountsList.toArray(amountBoxes);

            for(int i = 0; i < size; i++){
                AmountBox amountBox = amountBoxes[i];
                String amountBoxTokenId = amountBox.getTokenId();

                if(amountBoxTokenId != null && amountBox.getTokenId().equals(tokenId)){
                    m_amountsList.remove(amountBox);
                    break;
                }
            }
        }
    }

    public AmountBox getAmountBox(String tokenId){
        if(tokenId != null){
            int size = m_amountsList.size();
            AmountBox[] amountBoxes = new AmountBox[size];
            amountBoxes = m_amountsList.toArray(amountBoxes);

            for(int i = 0; i < size; i++){
                AmountBox amountBox = amountBoxes[i];
                String amountBoxTokenId = amountBox.getTokenId();

                if(amountBoxTokenId != null && amountBoxTokenId.equals(tokenId)){
                    return amountBox;
                }
            }
        }

        return null;
    }

    public void update(){
        m_listVBox.getChildren().clear();

        for(int i = 0; i < m_amountsList.size(); i++){
            AmountBox amountBox = m_amountsList.get(i);
            AmountBox.setHgrow(amountBox, Priority.ALWAYS);
            
            Insets padding = m_paddingInsets.get();

            HBox paddingBox = new HBox(amountBox);
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
