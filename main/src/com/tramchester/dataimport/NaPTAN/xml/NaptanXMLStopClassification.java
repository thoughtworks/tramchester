package com.tramchester.dataimport.NaPTAN.xml;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.tramchester.repository.naptan.NaptanStopType;

@JsonTypeName("StopClassification")
public class NaptanXMLStopClassification {
    @JsonProperty("StopType")
    private String stopType;

    public NaptanStopType getStopType() {
        return NaptanStopType.parse(stopType);
    }

}
