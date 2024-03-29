package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("Rail")
public class NaptanXMLRailInfo {

    private final NaptanXMLAnnotatedRailRef annotatedRailRef;

    @JsonCreator
    public NaptanXMLRailInfo(@JsonProperty("AnnotatedRailRef")
                                     NaptanXMLAnnotatedRailRef annotatedRailRef) {
        this.annotatedRailRef = annotatedRailRef;
    }

    public String getTiploc() {
        return annotatedRailRef.getTiplocRef();
    }

    public boolean hasRailInfo() {
        return annotatedRailRef!=null;
    }
}
