package com.netnotes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.gson.JsonPrimitive;
import io.netnotes.engine.NoteConstants;
import io.netnotes.engine.apps.AppConstants;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ErgoNodeConfigFile{

    public static final String DEFAULT_NAME = "ergo.conf";
   
    private String m_currentHeading = "";

    private ArrayList<ConfigItem> m_configItems = new ArrayList<>();

    private File m_file;
    
    public ErgoNodeConfigFile(File file){
        m_file = file;
        try {
            parseConfigFile(file);
        } catch (IOException e) {
            try {
                Files.writeString(AppConstants.LOG_FILE.toPath(), "ConfigFile: " + file.getName() + ": " + e.toString() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
        }
    }

    public ErgoNodeConfigFile(){

    }
    


    public ConfigItem getConfigItem(String name, String heading){
        if(name != null && heading != null){
            for(int i = 0; i < m_configItems.size(); i++){
                ConfigItem item = m_configItems.get(i);
                if(item.getName().equals(name) && item.getHeading().equals(heading)){
                    return item;
                }
            }
        }
        return null;
    }

    private ArrayList<String> m_inputBuffer = new ArrayList<>();

    

    public void parseConfigFile(File file) throws IOException{
        m_inputBuffer.clear();
         try(
            
            InputStream stream = file == null ? App.class.getResource("/txt/DefaultNode.conf").openStream() : new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        ){
    
            String line = null;
   
            while( (line = reader.readLine()) != null) {
                
                line = line.trim();

                int indexOfHash = line.indexOf("#");
                

                String comment = indexOfHash != -1 ? line.substring(indexOfHash) : "";

                line = indexOfHash != -1 ? (indexOfHash == 0 ? "" : line.substring(0, indexOfHash)) : line;
                int indexOfComment = line.indexOf("//");
                comment += (indexOfComment !=  -1 ? line.substring(indexOfComment) : "") + comment;
                line = indexOfComment != -1 ? (indexOfComment == 0 ? "" : line.substring(0, indexOfComment)) : line;


                int indexOfOpenBrace = line.indexOf("{");
                int indexOfCloseBrace = line.indexOf("}");
                int indexOfEquals = line.indexOf("=");
                int indexOfOpenBraket = line.indexOf("[");

                if(indexOfEquals != -1){
                    String name = line.substring(0, indexOfEquals).trim();
                    String value = line.substring(indexOfEquals + 1).trim();

                    ConfigItem currentItem = new ConfigItem(m_currentHeading, name, null, value);
                    if( indexOfOpenBrace != -1 || indexOfOpenBraket != -1){

                        long numOpen = value.chars().filter(c -> c == (indexOfOpenBrace != -1 ? '{' : '[')).count();
                        long numClosed = value.chars().filter(c -> c == (indexOfOpenBrace != -1 ? '}' : ']')).count();
                     
                        if(numOpen > numClosed){
                            long difference = numOpen - numClosed;
                            String nLine = null;
                            while( difference > 0){
                
                                nLine = reader.readLine();

                                if(nLine == null){
                                   break;
                                }
                                nLine = nLine.trim();
                                currentItem.getValueLines().add(nLine);

                                int nIndexOfHash = nLine.indexOf("#");
                               

                                nLine = nIndexOfHash != -1 ? (nIndexOfHash == 0 ? "" : nLine.substring(0, nIndexOfHash)) : nLine;
                                int nIndexOfComment = nLine.indexOf("//");
                                nLine = nIndexOfComment != -1 ? (nIndexOfComment == 0 ? "" : nLine.substring(0, nIndexOfComment)) : nLine;

                                long nNumOpen = nLine.chars().filter(c -> c == (indexOfOpenBrace != -1 ? '{' : '[')).count();
                                long nNumClosed = nLine.chars().filter(c -> c == (indexOfOpenBrace != -1 ? '}' : ']')).count();
                                
                                difference += (nNumOpen - nNumClosed);
                            }
                            if(nLine == null){
                                break;
                            }
                            
                        }                                
                        
                    }
                    m_configItems.add(currentItem);
                  
                }else{
                    if(comment.length() > 0){
                        ConfigItem commentItem = new ConfigItem(m_currentHeading, null, comment);
                        m_configItems.add(commentItem);
                    }else{
                        if(indexOfOpenBrace != -1){

                            m_currentHeading += "." + line.substring(0, indexOfOpenBrace).trim();
                          
                        }
    
                        if(indexOfCloseBrace != -1){
                            int lastIndexOfDot = m_currentHeading.lastIndexOf(".");
                            if(lastIndexOfDot == -1){
                                Files.writeString(AppConstants.LOG_FILE.toPath(), "\nErr (no dot): " + line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            }else{
                                m_currentHeading = lastIndexOfDot == 0 ? "" : m_currentHeading.substring(0, lastIndexOfDot);
                            }
                        }
                    }
                    
                    
                   
                }

                
            }

        }
    }

    
   

    public class ConfigItem{
        private String m_heading = null;
        private String m_name = null;
        private ArrayList<String> m_valueLines = new ArrayList<>();
        private String m_comment = null;
        private int m_depth = 0;

        public ConfigItem(String heading, String name, String comment, String... value){
            setHeading(heading);
            m_name = name;
            m_comment = comment;
            m_valueLines = new ArrayList<>( Arrays.asList(value));
        }

        public String getComment(){
            return m_comment;
        }

        public void setComment(String comment){
            m_comment = comment;
        }

        public String getHeading() {
            return m_heading;
        }
        public void setHeading(String heading) {
            m_heading = heading;
            m_depth = (int) getDotCount(heading);
        }
        public String getName() {
            return m_name;
        }
        public void setName(String name) {
            this.m_name = name;
        }
        public ArrayList<String> getValueLines() {
            return m_valueLines;
        }

        public static long getDotCount(String str){
            return str != null ? (str.length() == 0 ? 0 : str.chars().filter(c->c == '.').count()) : 0;
        }

        public int getDepth(){
            return m_depth;
        }

        /*
              String spaces = "";
            long tabs = getIndentCount();

            for(int i=0; i< tabs; i++){
                spaces += "  ";
            }
            String lines = "";

            for(int i = 0; i < m_valueLines.size(); i++){
                String line = m_valueLines.get(i);
                
                lines += (i == 0 ? line : "") + "\n";
                
            }
        */

        public String getValueAsStringNoIndent(){
            return String.join("\n", m_valueLines);
        }

        public JsonArray getValueAsJsonArray(){
            JsonArray jsonArray = new JsonArray();
            for(String line: m_valueLines){
                jsonArray.add(new JsonPrimitive(line));
            }
            return jsonArray;
        }

        public JsonObject getJsonObject(){
            JsonObject json = new JsonObject();
            json.addProperty("heading", m_heading);
            if(m_name != null){
                json.addProperty("name", m_name);
            }
            if(m_comment != null){
                json.addProperty("comment", m_comment);
            } 
            json.add("value", getValueAsJsonArray());
            return json;
        }
        
        @Override
        public String toString(){

            return getJsonObject().toString();
        }
    }

}