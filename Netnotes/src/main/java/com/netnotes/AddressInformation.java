package com.netnotes;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;

import com.utils.Utils;

import scala.util.Try;

public class AddressInformation {
    private String m_addressType = null;
    private Address m_address = null;
    private NetworkType m_networkType = null;
    private String m_addressString = null;

    public AddressInformation(String addressString){
       setAddressString(addressString);

    }

    public AddressInformation(JsonObject json) throws Exception{
        if(json != null){
            JsonElement addressElement = json.get("address");

            if(addressElement != null && addressElement.isJsonPrimitive()){
                setAddressString(addressElement.getAsString());
            }else{
                throw new Exception("Invalid arguments");
            }
        }else{
            throw new Exception("Null Json");
        }
    }

    public JsonObject getJsonObject(){
        if(m_addressString == null){
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("address",m_addressString);
        if(m_addressType != null){
            json.addProperty("addressType", m_addressType);
        }
        if(m_networkType != null){
            json.addProperty("networkType",m_networkType.toString());
        }
        return json;
    }


    public String getAddressType(){
        return m_addressType;
    }

    public String getAddressString(){
        return m_addressString;
    }

    public Address getAddress(){
        return m_address;
    }

    public NetworkType getNetworkType(){
        return m_networkType;
    }

    public void setAddress(Address address){
        m_address = address;
        byte[] addressBytes = null;
        
        Try<byte[]> bytes = scorex.util.encode.Base58.decode(address.toString());

        addressBytes = bytes.get();
        
        setAddressType(addressBytes);
    }

    public void setAddressType(byte[] addressBytes){
        switch (addressBytes[0]) {
            case 0x01:
                m_addressType = "P2PK";
                m_networkType = NetworkType.MAINNET;
                break;
            case 0x02:
                m_addressType = "P2SH";
                m_networkType = NetworkType.MAINNET;
                break;
            case 0x03:
                m_addressType = "P2S";
                m_networkType = NetworkType.MAINNET;
                break;
            case 0x11:
                m_addressType = "P2PK";
                m_networkType = NetworkType.TESTNET;
                break;
            case 0x12:
                m_addressType = "P2SH";
                m_networkType = NetworkType.TESTNET;
                break;
            case 0x13:
                m_addressType = "P2S";
                m_networkType = NetworkType.TESTNET;
                break;
            default:
                m_addressType = null;
                m_networkType = null;
        }
    }

    public void setAddressString(String addressString){
        
        m_addressString = addressString != null ? addressString : "";

        Runnable setNull = () ->{
            m_address = null;
            m_addressType = null;
            m_networkType = null;
        };

        if(m_addressString.equals("")){
            setNull.run();
            return;
        }

       // String checkString = m_addressString.replaceAll("[^0-9a-fA-F]", "");
  
        /*if(!checkString.equals(m_addressString))
        {
            setNull.run();
            return;
        }*/

        byte[] addressBytes = null;
        

        Try<byte[]> bytes = scorex.util.encode.Base58.decode(addressString);

        addressBytes = bytes.get();
        
        setAddressType(addressBytes);

        if(m_addressType  == null){
            m_address = null;
            return;
        }
        
        byte[] checksumBytes = new byte[]{addressBytes[addressBytes.length - 4], addressBytes[addressBytes.length - 3], addressBytes[addressBytes.length - 2], addressBytes[addressBytes.length - 1]};

        byte[] testBytes = new byte[addressBytes.length - 4];

        for (int i = 0; i < addressBytes.length - 4; i++) {
            testBytes[i] = addressBytes[i];
        }

        byte[] hashBytes = Utils.digestBytesToBytes(testBytes);

        if (!(checksumBytes[0] == hashBytes[0]
                && checksumBytes[1] == hashBytes[1]
                && checksumBytes[2] == hashBytes[2]
                && checksumBytes[3] == hashBytes[3])) {
            m_address = null;
            return;
        }

        m_address = Address.create(scorex.util.encode.Base58.encode(addressBytes));
    }
    
    
    public String getAddressMinimal(int show) {
        

        String adr = m_addressString != null ?  m_addressString : "";
        int len = adr.length();

        return (show * 2) > len ? adr : adr.substring(0, show) + "..." + adr.substring(len - show, len);
    }

    @Override
    public String toString(){
        return m_addressString;
    }
  
    

    
  
}
