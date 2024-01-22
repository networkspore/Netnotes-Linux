package com.netnotes;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ErgoTransactionPartner {
    public static class PartnerType{
        public final static String RECEIVER = "Received";
        public final static String SENDER = "Sent";
        public final static String UNKNOWN = "Unknown";

    }


    private String m_partnerAddressString;
    private String m_partnerType;
    private SimpleObjectProperty<ErgoAmount> m_ergoAmountProperty = new SimpleObjectProperty<>(null);
    private ObservableList< PriceAmount> m_tokensList = FXCollections.observableArrayList();

    public ErgoTransactionPartner(String partnerAddressString, String partnerType, ErgoAmount ergoAmount, PriceAmount[] tokens){
        m_partnerAddressString = partnerAddressString;
        m_partnerType = partnerType;
        m_ergoAmountProperty = new SimpleObjectProperty<>(ergoAmount);
        
        if(tokens != null){
            for(int i = 0; i< tokens.length; i++){
                m_tokensList.add(tokens[i]);
            }
        }
    }

    public ErgoTransactionPartner(String partnerAddressString, String partnerType, ErgoAmount ergoAmount){
        m_partnerAddressString = partnerAddressString;
        m_partnerType = partnerType;
        m_ergoAmountProperty = new SimpleObjectProperty<>(ergoAmount);
    }


    public void addNanoErgs(long nanoErgs){
        ErgoAmount ergoAmount = m_ergoAmountProperty.get();
        ergoAmount.addLongAmount(nanoErgs);
        m_ergoAmountProperty.set(ergoAmount);
    }

    public void addTokens(PriceAmount[] tokens){
        if(tokens != null){
            for(int i = 0; i < tokens.length ; i++){
                PriceAmount token = tokens[i];
                if(token != null){
                    PriceAmount currentToken = getToken(token.getTokenId());

                    if(currentToken != null){
                        currentToken.addBigDecimalAmount(token.getBigDecimalAmount());
                        int index = m_tokensList.indexOf(currentToken);
                        m_tokensList.set(index, currentToken);
                    }else{
                        m_tokensList.add(token);
                    }
                }
            }
        }
    }



    public String getParnterAddressString(){
        return m_partnerAddressString;
    }

    public String getPartnerType(){
        return m_partnerType;
    }

    public SimpleObjectProperty< ErgoAmount> ergoAmountProperty(){
        return m_ergoAmountProperty;
    }


    public PriceAmount[] getTokensArray(){
        int size = m_tokensList.size();
        PriceAmount[] tokens = new PriceAmount[size];

        return m_tokensList.toArray(tokens);
    }

    public PriceAmount getToken(String tokenId){
        if(tokenId != null){
            PriceAmount[] tokens = getTokensArray();
            for(int i = 0; i < tokens.length ; i++){
                PriceAmount token = tokens[i];
                if(token.getTokenId().equals(tokenId)){
                    return token;
                }
            }
        }
        return null;
    }

    public ObservableList<PriceAmount> tokensList(){
        return m_tokensList;
    }
}
