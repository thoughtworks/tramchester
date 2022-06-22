package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("Descriptor")
public class NaptanXMLDescriptor {

    private final String indicator;
    private final String commonName;

    @JsonCreator
    public NaptanXMLDescriptor(@JsonProperty("Indicator") String indicator,
                               @JsonProperty("CommonName") String commonName) {
        this.indicator = indicator;
        this.commonName = commonName;
    }

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
