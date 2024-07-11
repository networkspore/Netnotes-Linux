package com.netnotes;

import java.time.LocalDateTime;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class BalanceTab extends VBox implements TabInterface {

    
    private SimpleBooleanProperty m_current = new SimpleBooleanProperty(true);

    
    public final static String NAME = "Balance";

    private final AddressData m_addressData;
    private final AddressesData m_addressesData;
    private ListChangeListener<PriceAmount> m_tokensListChangeListener = null;
    private AmountBoxes m_amountBoxes;
    private ChangeListener<LocalDateTime> m_balanceChanged = null;
    private ChangeListener<LocalDateTime> m_priceChanged = null;
    private ChangeListener<PriceQuote> m_priceQuoteListener = null;
    
    public BalanceTab(Scene scene, AddressData addressData, SimpleDoubleProperty widthObject, VBox summaryBox){
        super();
        m_addressData = addressData;
        m_addressesData = addressData.getAddressesData();

        TextField totalField = new TextField();
        totalField.setId("formFieldSmall");
        totalField.setEditable(false);
        HBox.setHgrow(totalField, Priority.ALWAYS);

        VBox totalBox = new VBox(totalField);
        HBox.setHgrow(totalBox, Priority.ALWAYS);

        summaryBox.getChildren().clear();
        summaryBox.getChildren().addAll(totalBox);




        //////

        AmountBox ergoAmountBox = new AmountBox(addressData.getErgoAmount(), scene, addressData.getAddressesData());
        HBox.setHgrow(ergoAmountBox,Priority.ALWAYS);
      
      

        HBox amountBoxPadding = new HBox(ergoAmountBox);
        amountBoxPadding.setPadding(new Insets(10,10,0,10));
        HBox.setHgrow(amountBoxPadding, Priority.ALWAYS);

        m_amountBoxes = new AmountBoxes();
     
        m_amountBoxes.setPadding(new Insets(10,10,10,0));
        m_amountBoxes.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(m_amountBoxes, Priority.ALWAYS);
    
 
        Runnable updateAmountBoxes = ()->{
            long timestamp = System.currentTimeMillis();
            for(int i = 0; i < m_addressData.getConfirmedTokenList().size() ; i ++){
                PriceAmount tokenAmount = m_addressData.getConfirmedTokenList().get(i);
                AmountBoxInterface amountBoxInterface = m_amountBoxes.getAmountBox(tokenAmount.getCurrency().getTokenId());
                
                if(amountBoxInterface == null){
                    AmountBox newAmountBox = new AmountBox(tokenAmount, scene, m_addressData.getAddressesData());
                    newAmountBox.setTimeStamp(timestamp);
        
                    m_amountBoxes.add(newAmountBox);
                }else{
                    
                 //   amountBox.priceAmountProperty().set(tokenAmount);
                    amountBoxInterface.setTimeStamp(timestamp);
                }
            }

            m_amountBoxes.removeOld(timestamp);

         //   m_fieldsUpdated.set(LocalDateTime.now());
        };

        updateAmountBoxes.run();
        m_tokensListChangeListener = (ListChangeListener.Change<? extends PriceAmount> c) ->updateAmountBoxes.run();

        m_addressData.getConfirmedTokenList().addListener(m_tokensListChangeListener);

        prefWidthProperty().bind(widthObject);
        
       getChildren().addAll(amountBoxPadding, m_amountBoxes);
        
    }

    private SimpleStringProperty m_titleProperty = new SimpleStringProperty(NAME);

    public SimpleStringProperty titleProperty(){
        return m_titleProperty;
    }

    public String getType(){
        return App.STATIC_TYPE;
    }

    public boolean isStatic(){
        return getType().equals(App.STATIC_TYPE);
    }

    public ErgoNetworkData getErgoNetworkData(){
        return m_addressData.getErgoNetworkData();
    }

    public void shutdown(){
          
        if(m_balanceChanged != null){
            m_addressesData.balanceUpdatedProperty().removeListener(m_balanceChanged);
        }
        
        if(getErgoNetworkData().ergoPriceQuoteProperty().get() != null && m_priceChanged != null){
            getErgoNetworkData().ergoPriceQuoteProperty().get().getLastUpdated().removeListener(m_priceChanged);
        }
        
        if(getErgoNetworkData().ergoPriceQuoteProperty().get() != null && m_priceChanged != null && m_priceQuoteListener != null){
            getErgoNetworkData().ergoPriceQuoteProperty().removeListener(m_priceQuoteListener);
        }

        prefWidthProperty().unbind();
        if(m_tokensListChangeListener != null){
            m_addressData.getConfirmedTokenList().removeListener(m_tokensListChangeListener);
            m_tokensListChangeListener = null;
        }
        if(m_amountBoxes != null){
            m_amountBoxes.shutdown();
            m_amountBoxes = null;
      
        }
        getChildren().clear();
    }

    public String getTabId(){
        return NAME + ":" + m_addressData.getAddress().toString();
    }

    public String getName(){
        return NAME;
    }

    public static String createTabId(AddressData addressData){
        return BalanceTab.NAME + ":" + addressData.getAddress().toString();
    }

    
    public boolean getCurrent(){
        return m_current.get();
    } 

    public void setCurrent(boolean value){
        m_current.set(value);
    }

}
