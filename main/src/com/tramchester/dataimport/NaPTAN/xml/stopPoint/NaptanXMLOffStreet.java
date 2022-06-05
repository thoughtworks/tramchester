package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("OffStreet")
public class NaptanXMLOffStreet {

    private final NaptanXMLRailInfo railInfo;

    public NaptanXMLOffStreet(@JsonProperty("Rail") NaptanXMLRailInfo railInfo) {
        this.railInfo = railInfo;
    }

    public boolean hasRailInfo() {
        return railInfo!=null && railInfo.hasRailInfo();
    }

    public NaptanXMLRailInfo getRailInfo() {
        return railInfo;
    }
}
