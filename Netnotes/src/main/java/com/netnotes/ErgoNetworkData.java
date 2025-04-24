package com.netnotes;

import java.util.ArrayList;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonObject;
import io.netnotes.engine.NetworksData;
import io.netnotes.engine.NoteMsgInterface;
import io.netnotes.friendly_id.FriendlyId;

import javafx.beans.property.SimpleLongProperty;

public class ErgoNetworkData {
    
    private SimpleLongProperty m_updated = new SimpleLongProperty();

    private double m_stageWidth = 750;
    private double m_stageHeight = 500;

    private ErgoNetwork m_ergoNetwork;

    private ErgoWallets m_ergoWallets = null ;
    private ErgoNodes m_ergoNodes = null;
    private ErgoExplorers m_ergoExplorers = null;
    private ErgoMarkets m_ergoMarkets = null;
           
    private ArrayList<String> m_authorizedLocations = new ArrayList<>();

    private String m_locationId;
    private String m_id;

    public ErgoNetworkData(ErgoNetwork ergoNetwork, String locationId) {
        m_id = FriendlyId.createFriendlyId();
        m_ergoNetwork = ergoNetwork;
        m_locationId = locationId;
        m_authorizedLocations.add(ErgoDex.NAME);
        installNetworks();
        
        
    }

    public NetworkType getNetworkType(){
        return m_ergoNetwork.getNetworkType();
    }


    public String getLocationId(){
        return m_locationId;
    }

    public boolean isLocationAuthorized(String locationString){
        if(locationString != null){
            return locationString.equals(ErgoNetwork.NAME) || m_authorizedLocations.contains(locationString);
        }else{
            return false;
        }
    }

    public SimpleLongProperty updatedProperty(){
        return m_updated;
    }

    public String getId(){
        return m_id;
    }
    
    public ErgoWallets getErgoWallets(){
        return m_ergoWallets;
    }

    public ErgoNodes getErgoNodes(){
        return m_ergoNodes;
    }



    public ErgoExplorers getErgoExplorers(){
        return m_ergoExplorers;
    }

    public ErgoMarkets getErgoMarkets(){
        return m_ergoMarkets;
    }

    private NoteMsgInterface m_networksDataMsgInterface = null;

    public void installNetworks() {
     
        m_ergoWallets = new ErgoWallets(this, m_ergoNetwork);
        m_ergoExplorers = new ErgoExplorers(this, m_ergoNetwork); 
        m_ergoNodes = new ErgoNodes(this, m_ergoNetwork);
        m_ergoMarkets = new ErgoMarkets(this, m_ergoNetwork);
        
        m_networksDataMsgInterface = new NoteMsgInterface() {

            @Override
            public String getId() {
                return getLocationId();
            }

            @Override
            public void sendMessage(int code, long timestamp, String networkId, String msg) {
                switch(networkId){
                    case NetworksData.APPS:
                        m_ergoMarkets.sendMessage(code, timestamp, networkId, msg);
                    break;
                }
                
            }

            @Override
            public void sendMessage(int code, long timestamp, String networkId, Number number) {
                        
            }
            
        };   

        getNetworksData().addMsgListener(m_networksDataMsgInterface);
    }

    public NetworksData getNetworksData(){
        return m_ergoNetwork.getNetworksData();
    }

    public JsonObject getStageObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("width", m_stageWidth);
        jsonObject.addProperty("height", m_stageHeight);
        return jsonObject;
    }

    public ErgoNetwork getErgoNetwork(){
        return m_ergoNetwork;
    }




    public void shutdown(){
        if(m_networksDataMsgInterface != null){
            getNetworksData().removeMsgListener(m_networksDataMsgInterface);
            m_networksDataMsgInterface = null;
        }
    }



}


