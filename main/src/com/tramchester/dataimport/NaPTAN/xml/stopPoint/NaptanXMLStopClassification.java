package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.tramchester.dataimport.NaPTAN.NaptanRailStationData;
import com.tramchester.repository.naptan.NaptanStopType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("StopClassification")
public class NaptanXMLStopClassification {

    final private String stopType;
    final private NaptanXMLOffStreet offStreet;

    public NaptanXMLStopClassification(@JsonProperty("StopType") String stopType,
                                        @JsonProperty("OffStreet") NaptanXMLOffStreet offStreet) {
        this.stopType = stopType;
        this.offStreet = offStreet;
    }

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
