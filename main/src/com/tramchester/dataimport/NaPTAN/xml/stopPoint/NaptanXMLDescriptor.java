package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonIgnoreProperties(ignoreUnknown = true)
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
