package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("AnnotatedRailRef")
public class NaptanXMLAnnotatedRailRef {

    @JsonProperty("TiplocRef")
    private String tiplocRef;

    public String getTiplocRef() {
        return tiplocRef;
    }
}
