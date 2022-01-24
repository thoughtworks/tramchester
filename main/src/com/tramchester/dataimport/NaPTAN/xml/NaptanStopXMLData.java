package com.tramchester.dataimport.NaPTAN.xml;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.tramchester.dataimport.NaPTAN.NaptanRailStationData;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanStopType;


@JsonRootName("StopPoint")
public class NaptanStopXMLData implements NaptanStopData {

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

    public NaptanStopXMLData() {
        // deserialisation
    }

    @Override
    public String getAtcoCode() {
        return atcoCode;
    }

    @Override
    public String getNaptanCode() {
        return naptanCode;
    }

    public String getIndicator() {
        return descriptor.getIndicator();
    }

    @Override
    public boolean hasRailInfo() {
        return stopClassification.hasRailInfo();
    }

    @Override
    public String getNptgLocality() {
        return place.getNptgLocalityRef();
    }

    @Override
    public GridPosition getGridPosition() {
        return place.getLocation().getGridPosition();
    }

    @Override
    public NaptanStopType getStopType() {
        return stopClassification.getStopType();
    }

    @Override
    public String getCommonName() {
        return descriptor.getCommonName();
    }

    @Override
    public String getSuburb() {
        return place.getSuburb();
    }

    @Override
    public String getTown() {
        return place.getTown();
    }

    @Override
    public String toString() {
        return "NaptanStopXMLData{" +
                "atcoCode='" + atcoCode + '\'' +
                ", naptanCode='" + naptanCode + '\'' +
                ", place=" + place +
                ", descriptor=" + descriptor +
                ", stopClassification=" + stopClassification +
                '}';
    }

    @Override
    public NaptanRailStationData getRailInfo() {
        return stopClassification.getRailInfo();
    }
}
