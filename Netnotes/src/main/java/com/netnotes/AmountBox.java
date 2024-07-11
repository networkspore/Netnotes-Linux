package com.netnotes;


import java.math.BigDecimal;

import com.devskiller.friendly_id.FriendlyId;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;


public class AmountBox extends HBox implements AmountBoxInterface {

    private long m_quoteTimeout = AddressesData.QUOTE_TIMEOUT;
    private PriceAmount m_priceAmount = null;
    private String m_id = null;

    private int m_minImgWidth = 250;
    private long m_timestamp = 0;


    private ChangeListener<PriceQuote> m_priceQuoteListener = null;
    private ChangeListener<BigDecimal> m_amountListener = null;


    public AmountBox(){
        super();
        m_id = FriendlyId.createFriendlyId();
    }

    private AddressesData m_addressesData;

    

    public AmountBox(PriceAmount priceAmount, Scene scene, AddressesData addressesData) {
        super();
        m_id = FriendlyId.createFriendlyId();
        m_priceAmount = priceAmount;
        setId("darkRowBox");
        setMinHeight(45);
        setMaxHeight(45);

        
        m_addressesData = addressesData;
        
        HBox.setHgrow(this,Priority.ALWAYS);
       
       // amountImageView.textProperty().bind(m_currentAmount.asString());
       
       // amountImageView.setGraphicTextGap(25);
        

        setAlignment(Pos.CENTER_LEFT);

        String textFieldId = m_id +"TextField";




        TextField amountField = new TextField(priceAmount.amountProperty().get() + "");
   
        amountField.setUserData(textFieldId);
        HBox.setHgrow(amountField, Priority.ALWAYS);
        amountField.setId("formField");
        amountField.setEditable(false);
  
        
        BufferedMenuButton menuBtn = new BufferedMenuButton();
        menuBtn.getBufferedImageView().setFitWidth(20);
        menuBtn.setPadding(new Insets(0));
        
        

        ImageView currencyImageView = new ImageView();
        currencyImageView.setFocusTraversable(true);
        currencyImageView.setPreserveRatio(true);
        currencyImageView.setImage(m_priceAmount.getCurrency().getBackgroundIcon(38));
        
     

        SimpleStringProperty userdata = new SimpleStringProperty(null);
     
    


        final String enableString = "enable";

        MenuItem enableErgoTokens = new MenuItem("Enable Ergo Tokens");
        enableErgoTokens.setOnAction(e->{
            //m_addressesData.ergoTokensProperty().set((ErgoTokens)m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getErgoTokens());
        });

    
        final String viewString = "viewToken";
      //  MenuItem addItem = new MenuItem("Add to Ergo Tokens");
        MenuItem viewItem = new MenuItem("Open");

        MenuItem currencyUrlItem = new MenuItem("Visit Website");


        StackPane menuStack = new StackPane(currencyImageView, menuBtn);
        menuStack.setAlignment(Pos.CENTER_RIGHT);
        getChildren().addAll(menuStack, amountField);
        setAlignment(Pos.CENTER_LEFT);

 
        
        Runnable updates = () ->{
           
            ErgoTokens ergoTokens = getErgoTokens();

            boolean isErgoTokens = ergoTokens != null;

            
            String tokenOptionsBtnUserData = userdata.get() != null ?  userdata.get() : "null";

            PriceAmount currentAmount = m_priceAmount;

          
            
        };

        Runnable quoteUpdate = () ->{
           // updateBufferedImage(amountImageView);
        };
        
        
        //addCurrentAmountListeners(priceAmount, amountField, updates, imageUpdates);
        
        Runnable addCurrentAmountListeners = ()->{
            PriceAmount currentAmount = m_priceAmount;

            m_amountListener = (obs,oldval,newval)->{
                if(!amountField.getText().equals(newval + "")){
                    amountField.setText(newval + "");
                }
                
            };

   
            m_priceQuoteListener = (obs, oldval, newval)-> quoteUpdate.run();
           
            currentAmount.amountProperty().addListener(m_amountListener);
           
            currentAmount.priceQuoteProperty().addListener(m_priceQuoteListener);

        };

        addCurrentAmountListeners.run();
        
    
        updates.run();
    }


  

    public long getTimeStamp(){
        return m_timestamp;
    }

    public void setTimeStamp(long timeStamp){
        m_timestamp = timeStamp;
    }



    public String getBoxId(){
        return m_id;
    }

    public void setBoxId(String id){
        m_id = id;
    }

    public long getQuoteTimeout(){
        return m_quoteTimeout;
    }

    public void setQuoteTimeout(long timeout){
        m_quoteTimeout = timeout;
    }

    public String getTokenId(){
        return m_priceAmount.getTokenId();
    }

   


    public int getMinImageWidth(){
        return m_minImgWidth;
    }

    public void setMinImageWidth(int width){
        m_minImgWidth = width;
    }


    private ErgoTokens getErgoTokens(){
        return null;//m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getErgoTokens();
    } 

    public PriceAmount getPriceAmount(){
        return m_priceAmount;
    }

    public void shutdown(){
    

        if(m_amountListener != null){
            m_priceAmount.amountProperty().removeListener(m_amountListener);
            m_amountListener = null;
        }
       
        if(m_priceQuoteListener != null){
            m_priceAmount.priceQuoteProperty().removeListener(m_priceQuoteListener);
            m_priceQuoteListener = null;
        }
        
    }

}
