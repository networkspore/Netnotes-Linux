package com.netnotes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import org.apache.commons.codec.binary.Hex;

public class HashDataFile extends HashData {
    private File m_file = null;
    public HashDataFile(File file) throws IOException{
        super(file);
        m_file = file;
    }

    public HashDataFile(JsonObject json) throws NullPointerException, IOException{
        super(json);

        JsonElement fileElement = json.get("file");
        
        File file = fileElement != null && fileElement.isJsonPrimitive() ? new File(fileElement.getAsString()) : null;
        if(file != null && m_file.isFile()){
            byte[] fileBytes = Utils.digestFile(file);
            String fileHex = Hex.encodeHexString(fileBytes);
            if(fileHex.equals(getHashStringHex())){
                m_file = file;
            }else{
                throw new FileNotFoundException("File with matching hash data not found.");
            }
        }else{
            throw new NullPointerException("File does not exist");
        }
    }

    public File getFile(){
        return m_file;
    }

    public void setFile(File file){
        m_file = file;
    }

    public boolean isFileValid(){
        if(m_file != null && m_file.isFile()){
          
           
            try {
                byte[]fileBytes = Utils.digestFile(m_file);
                String fileHex = Hex.encodeHexString(fileBytes);
                return fileHex.equals(getHashStringHex());
            } catch (IOException e) {
                String exString = e.toString();

                return exString == null;
            }
      
                
        }else{
            return false;
        }
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject json = new JsonObject();
        json.addProperty("id", getId());
        json.addProperty("name", getHashName());
        if (getHashBytes() != null) {
            json.addProperty("hash", getHashStringHex());
        }
        json.addProperty("file",m_file.getAbsolutePath());
        return json;
    }
}
