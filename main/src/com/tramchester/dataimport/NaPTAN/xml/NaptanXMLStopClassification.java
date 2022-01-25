package com.tramchester.dataimport.NaPTAN.xml;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.tramchester.dataimport.NaPTAN.NaptanRailStationData;
import com.tramchester.repository.naptan.NaptanStopType;

@JsonTypeName("StopClassification")
public class NaptanXMLStopClassification {

    @JsonProperty("StopType")
    private String stopType;

    @JsonProperty("OffStreet")
    private NaptanXMLOffStreet offStreet;

    public NaptanStopType getStopType() {
        return NaptanStopType.parse(stopType);
    }

    public boolean hasRailInfo() {
        if (offStreet==null) {
            return false;
        }
        return offStreet.hasRailInfo();
    }

    public NaptanRailStationData getRailInfo() {
        return new NaptanRailStationData(offStreet.getRailInfo().getTiploc());
    }

    @Override
    public String toString() {
        return "NaptanXMLStopClassification{" +
                "stopType='" + stopType + '\'' +
                ", offStreet=" + offStreet +
                '}';
    }
}
