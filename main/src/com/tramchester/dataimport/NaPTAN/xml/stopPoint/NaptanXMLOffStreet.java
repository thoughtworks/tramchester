package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("OffStreet")
public class NaptanXMLOffStreet {

    @JsonProperty("Rail")
    private NaptanXMLRailInfo railInfo;

    public boolean hasRailInfo() {
        return railInfo!=null && railInfo.hasRailInfo();
    }

    public NaptanXMLRailInfo getRailInfo() {
        return railInfo;
    }
}
