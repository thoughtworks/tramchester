package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

import java.util.Arrays;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NaptanXMLStopAreaRef {

    @JsonIgnore
    private static final List<String> activeModificationStatus = Arrays.asList("new", "revise");

    @JacksonXmlProperty(isAttribute = true, localName = "Modification")
    private String modification;

    @JacksonXmlText
    private String id;

    public String getId() {
        return id;
    }

    public String getModification() {
        return modification;
    }

    @JsonIgnore
    public boolean isActive() {
        return activeModificationStatus.contains(modification);
    }

    @Override
    public String toString() {
        return "NaptanXMLStopAreaRef{" +
                "modification='" + modification + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
