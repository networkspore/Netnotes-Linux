package com.netnotes;

import java.io.File;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;


import javafx.stage.FileChooser.ExtensionFilter;
import javafx.beans.property.SimpleLongProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public class ErgoWallets extends Network implements NoteInterface {

    public final static String DESCRIPTION = "Ergo Wallet allows you to create and manage wallets on the Ergo Blockchain.";
    public final static String SUMMARY = "Access can be controlled with the Ergo Wallet, in order to keep the wallet isolated, or access can be given to the Ergo Network in order to make transactions, or the Ergo Explorer to get your ERG ballance and to the KuCoin Exchange to get your ERG value real time.";
    public final static String NAME = "Ergo Wallet";
    public final static ExtensionFilter ergExt = new ExtensionFilter("Ergo Wallet (Satergo compatible)", "*.erg");
    public final static String NETWORK_ID = "ERGO_WALLETS";
    public final static String DONATION_ADDRESS_STRING = "9h123xUZMi26FZrHuzsFfsTpfD3mMuTxQTNEhAjTpD83EPchePU";

    private ErgoNetworkData m_ergNetData;
    private final SimpleLongProperty m_timeStampProperty = new SimpleLongProperty(0);

    private ErgoWalletDataList m_ergoWalletDataList = null;


    public ErgoWallets( ErgoNetworkData ergoNetworkData, ErgoNetwork ergoNetwork) {
        super(new Image(getAppIconString()), NAME, NETWORK_ID, ergoNetwork);
        m_ergNetData = ergoNetworkData;

    }


    public ErgoNetworkData getErgoNetworkData() {
        return m_ergNetData;
    }

    public SimpleLongProperty timeStampProperty(){
        return m_timeStampProperty;
    }



   

    public static String getAppIconString(){
        return "/assets/ergo-wallet.png";
    }

    public static String getSmallAppIconString(){
        return "/assets/ergo-wallet-30.png";
    }

    
    private Image m_smallAppIcon = new Image(getSmallAppIconString());
    public Image getSmallAppIcon() {
        return m_smallAppIcon;
    }

    

    @Override
    public Object sendNote(JsonObject note){
        if(note != null && m_ergoWalletDataList != null){
            JsonElement subjecElement = note.get("subject");
            JsonElement networkIdElement = note.get("networkId");

            if( subjecElement != null && subjecElement.isJsonPrimitive() && networkIdElement != null && networkIdElement.isJsonPrimitive()){
                String networkId = networkIdElement.getAsString();
            
                if(networkId.equals(m_ergNetData.getId())){
                    switch (subjecElement.getAsString()) {
                        case "getWallets":
                            return m_ergoWalletDataList.getWallets();
                        case "getWalletById":
                            return m_ergoWalletDataList.getWalletById(note);
                        case "getWalletByName":
                            return m_ergoWalletDataList.getWalletByName(note);    
                        case "openWallet":
                            return m_ergoWalletDataList.openWallet(note);
                    }
                }
                
            }
        }
        return null;
    }

    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator) {
        JsonElement subjecElement = note.get("subject");
        JsonElement networkIdElement = note.get("networkId");
        

        if (subjecElement != null  && networkIdElement != null) {
            Utils.returnObject(sendNote(note), getNetworksData().getExecService(), onSucceeded, onFailed);
        }
        return false;
    }

    
   


 

    @Override
    public void start(){
        if(getConnectionStatus() == App.STOPPED){
            super.start();
            m_ergoWalletDataList = new ErgoWalletDataList(this);
            
        }
    }

    @Override
    public void stop(){
        if(getConnectionStatus() != App.STOPPED){
            super.stop();
            
            if(m_ergoWalletDataList != null){
                m_ergoWalletDataList.shutdown();
                m_ergoWalletDataList = null;
            }
            
        }
    }



    public String getDescription(){
        return DESCRIPTION;
    }
    
    public String getType(){
        return App.WALLET_TYPE;
    }




  
    public NetworkInformation getNetworkInformation(){
        return new NetworkInformation(NETWORK_ID, NAME, getAppIconString(), getSmallAppIconString(), DESCRIPTION);
    }

}
