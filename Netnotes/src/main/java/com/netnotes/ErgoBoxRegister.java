package com.netnotes;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;




public class ErgoBoxRegister {
 

        private String m_serializedValue;
        private String m_sigmaType;
        private JsonElement m_renderedValue;

        public String getSerializedValue() {
            return m_serializedValue;
        }

        public String getSigmaType() {
            return m_sigmaType;
        }

        public JsonElement getRenderedValue() {
            return m_renderedValue;
        }

        public ErgoBoxRegister(JsonObject json){
            JsonElement serializedValueElement = json.get("serializedValue");
            JsonElement sigmaTypeElement = json.get("sigmaType");
            JsonElement renderedValueElement = json.get("renderedValue");

            m_serializedValue = serializedValueElement != null ? serializedValueElement.getAsString() : "";
            m_sigmaType = sigmaTypeElement != null ? sigmaTypeElement.getAsString() : "";
            m_renderedValue = renderedValueElement;

        }
    
}
