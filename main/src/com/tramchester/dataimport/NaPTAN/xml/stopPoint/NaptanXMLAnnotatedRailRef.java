package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("AnnotatedRailRef")
public class NaptanXMLAnnotatedRailRef {

    private final String tiplocRef;

    public NaptanXMLAnnotatedRailRef(@JsonProperty("TiplocRef") String tiplocRef) {
        this.tiplocRef = tiplocRef;
    }

    public String getTiplocRef() {
        return tiplocRef;
    }
}
