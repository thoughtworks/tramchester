package com.tramchester.dataimport.NaPTAN.xml;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("Rail")
public class NaptanXMLRailInfo {

    @JsonProperty("AnnotatedRailRef")
    NaptanXMLAnnotatedRailRef annotatedRailRef;

    public String getTiploc() {
        return annotatedRailRef.getTiplocRef();
    }
}
