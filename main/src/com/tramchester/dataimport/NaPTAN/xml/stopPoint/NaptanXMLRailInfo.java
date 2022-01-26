package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("Rail")
public class NaptanXMLRailInfo {

    @JsonProperty("AnnotatedRailRef")
    NaptanXMLAnnotatedRailRef annotatedRailRef;

    public String getTiploc() {
        return annotatedRailRef.getTiplocRef();
    }

    public boolean hasRailInfo() {
        return annotatedRailRef!=null;
    }
}
