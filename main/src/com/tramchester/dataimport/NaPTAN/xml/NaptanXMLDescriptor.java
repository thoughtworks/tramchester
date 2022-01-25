package com.tramchester.dataimport.NaPTAN.xml;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("Descriptor")
public class NaptanXMLDescriptor {

    @JsonProperty("Indicator")
    private String indicator;

    @JsonProperty("CommonName")
    private String commonName;

    public String getIndicator() {
        return indicator;
    }

    public String getCommonName() {
        return commonName;
    }

    @Override
    public String toString() {
        return "NaptanXMLDescriptor{" +
                "indicator='" + indicator + '\'' +
                ", commonName='" + commonName + '\'' +
                '}';
    }
}
