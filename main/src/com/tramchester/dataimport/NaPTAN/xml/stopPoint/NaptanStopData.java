package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.tramchester.dataimport.NaPTAN.NaptanRailStationData;
import com.tramchester.dataimport.NaPTAN.NaptanXMLData;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanStopType;

import java.util.Collections;
import java.util.List;


@JsonRootName("StopPoints")
@JsonTypeName("StopPoint")
public class NaptanStopData implements NaptanXMLData {

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

    @JacksonXmlProperty(localName = "StopAreas")
    private List<NaptanXMLStopAreaRef> stopAreas;

    public NaptanStopData() {
        // deserialisation
    }

    public IdFor<NaptanRecord> getAtcoCode() {
        return StringIdFor.createId(atcoCode);
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

    public LatLong getLatLong() {
        return place.getLocation().getLatLong();
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

    public List<NaptanXMLStopAreaRef> stopAreasRefs() {
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
