package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("AnnotatedRailRef")
public class NaptanXMLAnnotatedRailRef {

    @JsonProperty("TiplocRef")
    private String tiplocRef;

    public String getTiplocRef() {
        return tiplocRef;
    }
}
