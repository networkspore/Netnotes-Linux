package com.netnotes;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import java.util.Arrays;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;

import com.utils.Utils;

import scala.util.Try;
import scorex.util.encode.Base16;

public class AddressInformation {
    public final static int P2PK_INT_TYPE = 1;
    public final static int P2SH_INT_TYPE = 2;
    public final static int P2S_INT_TYPE = 3;
    public final static int TEST_P2PK_INT_TYPE = 11;
    public final static int TEST_P2SH_INT_TYPE = 12;
    public final static int TEST_P2S_INT_TYPE = 13;

    public final static int MAINNET_INT_TYPE = 0;
    public final static int TESTNET_INT_TYPE = 10;




    public final static String SIGMA_PROP_CONST_PREFIX_HEX = "08cd";
    public final static String ERGO_TREE_PREFIX_HEX = "00";

    private String m_addressType = null;
    private Address m_address = null;
    private NetworkType m_networkType = null;
    private String m_addressString = null;

    private int m_addressIntType = -1;
    private int m_addressIntNetworkType = -1;

    public AddressInformation(String addressString){

       setAddressString(addressString.replaceAll("[^A-HJ-NP-Za-km-z0-9]", ""));

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

    public int getAddressIntType(){
        return m_addressIntType;
    }

    public int getAddressIntNetworkType(){
        return m_addressIntNetworkType;
    }

    public String getAddressTypeString(){
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
        
        setAddressByBytes(addressBytes);
    }

    private void setAddressByBytes(byte[] addressBytes){
        if(addressBytes != null){
            int[] addressIntType = getAddressIntType(addressBytes[0]);


            if(addressIntType != null){
                m_addressIntType = addressIntType[0];
                m_addressIntNetworkType = addressIntType[1];

                m_addressType = getAddressTypeString(m_addressIntType);
                m_networkType = getAddressTypeNetworkType(m_addressIntNetworkType);
            }else{
                m_addressType = null;
                m_networkType = null;
            }
        }else{
            m_addressType = null;
            m_networkType = null;
        }
    }


    public static int[] getAddressIntType(byte addressByte){
        switch (addressByte) {
            case 0x01:
                return new int[] {P2PK_INT_TYPE, MAINNET_INT_TYPE}; 
            case 0x02:
                return new int[] {P2SH_INT_TYPE, MAINNET_INT_TYPE}; 
            case 0x03:
                return new int[] {P2S_INT_TYPE, MAINNET_INT_TYPE}; 
            case 0x11:
                return new int[] {TEST_P2PK_INT_TYPE, TESTNET_INT_TYPE}; 
            case 0x12:
                return new int[] {TEST_P2SH_INT_TYPE, TESTNET_INT_TYPE}; 
            case 0x13:
                return new int[] {TEST_P2S_INT_TYPE, TESTNET_INT_TYPE}; 
            default:
                return null;
        }
    }


    public static String getAddressTypeString(int intType){
        switch (intType) {
            case P2PK_INT_TYPE:
                return "P2PK";
            case P2SH_INT_TYPE:
                return "P2SH";
            case P2S_INT_TYPE:
                return "P2S";
            case TEST_P2PK_INT_TYPE:
                return "P2PK (TESTNET)";
            case TEST_P2SH_INT_TYPE:
                return "P2SH (TESTNET)";
            case TEST_P2S_INT_TYPE:
                return "P2S (TESTNET)";
            default:
                return null;
        }
    }
    public static NetworkType getAddressTypeNetworkType(int intNetworkType){
        return intNetworkType == MAINNET_INT_TYPE ? NetworkType.MAINNET : (intNetworkType == TESTNET_INT_TYPE ? NetworkType.TESTNET : null);
    }

    private void setNull(){
        m_address = null;
        m_addressType = null;
        m_networkType = null;
    }

    public void setAddressString(String addressString){
        
        m_addressString = addressString != null ? addressString : "";

        if(m_addressString.equals("")){
            setNull();
            return;
        }


        byte[] addressBytes = convertAddressToBytes(addressString);


        
        setAddressByBytes(addressBytes);

        if(m_addressType  == null){
            m_address = null;
            return;
        }
        
        

        m_address = Address.create(scorex.util.encode.Base58.encode(addressBytes));
    }

    
    public static byte[] convertAddressToBytes(String addressString){
        if(addressString == null){
            return null;
        }
        Try<byte[]> bytes = scorex.util.encode.Base58.decode(addressString);
        byte[] addressBytes = bytes.getOrElse(null);

        if(addressBytes == null){
            return null;
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
            return null;
        }

        return addressBytes;
    }

    
    public String getAddressMinimal(int show) {
        

        String adr = m_addressString != null ?  m_addressString : "";
        int len = adr.length();

        return (show * 2) > len ? adr : adr.substring(0, show) + "..." + adr.substring(len - show, len);
    }

    public static String cratePKHexFromAddress(String addressString){
        byte[] addressBytes = convertAddressToBytes(addressString);
        if(addressBytes != null){
            
            if(addressBytes != null && addressBytes.length > 32){
                int[] intType = AddressInformation.getAddressIntType(addressBytes[0]);

                if(intType != null){
                    if(intType[0] == AddressInformation.P2PK_INT_TYPE){
                        byte[] addressByteRange = Arrays.copyOfRange(addressBytes, 1, 34);
                        String pk = Base16.encode(addressByteRange);

                        return pk;
                    }
                }
            }
        }

        return null;
    }

    public static String createPKPropHexFromAddress(String address){
        String pk = cratePKHexFromAddress(address);
        return pk != null ? ERGO_TREE_PREFIX_HEX + SIGMA_PROP_CONST_PREFIX_HEX + pk : null;
    }

    public String getAddressPKPropHex(){
        return m_addressString != null ? createPKPropHexFromAddress(m_addressString) : null;
    }

    public String getAddressPKHex(){
        return m_addressString != null ? cratePKHexFromAddress(m_addressString) : null;
    }

    @Override
    public String toString(){
        return m_addressString;
    }
  
  
    
  
}
