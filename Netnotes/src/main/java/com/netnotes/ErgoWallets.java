package com.netnotes;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.scene.image.Image;


import javafx.stage.FileChooser.ExtensionFilter;
import javafx.beans.property.SimpleLongProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public class ErgoWallets  {

    public final static String DESCRIPTION = "Ergo Wallet allows you to create and manage wallets on the Ergo Blockchain.";
    public final static String SUMMARY = "Access can be controlled with the Ergo Wallet, in order to keep the wallet isolated, or access can be given to the Ergo Network in order to make transactions, or the Ergo Explorer to get your ERG ballance and to the KuCoin Exchange to get your ERG value real time.";
    public final static String NAME = "Ergo Wallet";
    public final static ExtensionFilter ergExt = new ExtensionFilter("Ergo Wallet (Satergo compatible)", "*.erg");

    public final static String DONATION_ADDRESS_STRING = "9h123xUZMi26FZrHuzsFfsTpfD3mMuTxQTNEhAjTpD83EPchePU";

    private ErgoNetworkData m_ergNetData;
    private final SimpleLongProperty m_timeStampProperty = new SimpleLongProperty(0);

    private ErgoWalletDataList m_ergoWalletDataList = null;


    public ErgoWallets( ErgoNetworkData ergoNetworkData, ErgoNetwork ergoNetwork) {
      //  super(new Image(getAppIconString()), NAME, ErgoNetwork.WALLET_NETWORK, ergoNetwork);
        m_ergNetData = ergoNetworkData;
        m_ergoWalletDataList = new ErgoWalletDataList(this);

    }

    public String getName(){
        return NAME;
    }

    public Image getIcon(){
        return getSmallAppIcon();
    }


    public ErgoNetworkData getErgoNetworkData() {
        return m_ergNetData;
    }

    public SimpleLongProperty timeStampProperty(){
        return m_timeStampProperty;
    }

    private Image m_smallAppIcon = new Image(getSmallAppIconString());
    public Image getSmallAppIcon() {
        return m_smallAppIcon;
    }


   

    public static String getAppIconString(){
        return "/assets/ergo-wallet.png";
    }

    public static String getSmallAppIconString(){
        return "/assets/ergo-wallet-30.png";
    }

    

    


    public Object sendNote(JsonObject note){
     
        if(note != null && m_ergoWalletDataList != null){
            JsonElement cmdElement = note.get(App.CMD);

            if( cmdElement != null && cmdElement.isJsonPrimitive()){
                switch (cmdElement.getAsString()) {
                    case "getWallets":
                        return m_ergoWalletDataList.getWallets();
                    case "setDefault":
                        return m_ergoWalletDataList.setDefault(note);
                    case "clearDefault":
                        return m_ergoWalletDataList.clearDefault();
                    case "getDefault":
                        return m_ergoWalletDataList.getDefault();
                    case "getDefaultInterface":
                        return m_ergoWalletDataList.getDefaultInterface();
                    case "getWalletInterface":
                        return m_ergoWalletDataList.getWalletInterface(note);
                    case "getWalletById":
                        return m_ergoWalletDataList.getWalletById(note);
                    case "getWalletByName":
                        return m_ergoWalletDataList.getWalletByName(note);    
                    case "openWallet":
                        return m_ergoWalletDataList.openWallet(note);
                    case "removeWallet":
                        return m_ergoWalletDataList.removeWallet(note);
                }
            }
        }
        return null;
    }


    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        JsonElement cmdElement = note.get(App.CMD);

        if (cmdElement != null) {
            String cmd  = cmdElement.getAsString();
            switch(cmd){
                default:
            }
            
        }
       

        return false;
    }

    

    public String getDescription(){
        return DESCRIPTION;
    }



}
