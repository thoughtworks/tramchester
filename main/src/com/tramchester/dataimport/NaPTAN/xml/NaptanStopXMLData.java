package com.tramchester.dataimport.NaPTAN.xml;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.tramchester.dataimport.NaPTAN.NaptanRailStationData;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanStopType;

import java.util.Collections;
import java.util.List;


@JsonRootName("StopPoint")
public class NaptanStopXMLData {

    // aliaises are for the xml version of the data

    // Matches ID for TFGM gtfs data
    @JsonProperty(value = "AtcoCode")
    private String atcoCode;

    @JsonProperty("NaptanCode")
    private String naptanCode;

    @JsonProperty("Place")
    private NaptanXMLPlace place;

    @JsonProperty("Descriptor")
    private NaptanXMLDescriptor descriptor;

    @JsonProperty("StopClassification")
    private NaptanXMLStopClassification stopClassification;

    @JsonProperty("StopAreas")
    private List<String> stopAreas;

    public NaptanStopXMLData() {
        // deserialisation
    }

    public String getAtcoCode() {
        return atcoCode;
    }

    public String getNaptanCode() {
        return naptanCode;
    }

    public String getIndicator() {
        return descriptor.getIndicator();
    }

    public boolean hasRailInfo() {
        return stopClassification.hasRailInfo();
    }

    public String getNptgLocality() {
        return place.getNptgLocalityRef();
    }

    public GridPosition getGridPosition() {
        return place.getLocation().getGridPosition();
    }

    public NaptanStopType getStopType() {
        return stopClassification.getStopType();
    }

    public String getCommonName() {
        return descriptor.getCommonName();
    }

    public String getSuburb() {
        return place.getSuburb();
    }

    public String getTown() {
        return place.getTown();
    }

    public List<String> getStopAreaCode() {
        if (stopAreas==null) {
            return Collections.emptyList();
        }
        return stopAreas;
    }

    public NaptanRailStationData getRailInfo() {
        return stopClassification.getRailInfo();
    }

    @Override
    public String toString() {
        return "NaptanStopXMLData{" +
                "atcoCode='" + atcoCode + '\'' +
                ", naptanCode='" + naptanCode + '\'' +
                ", place=" + place +
                ", descriptor=" + descriptor +
                ", stopClassification=" + stopClassification +
                ", stopAreas=" + stopAreas +
                '}';
    }

}
