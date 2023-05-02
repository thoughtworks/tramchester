package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

import com.fasterxml.jackson.annotation.*;
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


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName("StopPoints")
@JsonTypeName("StopPoint")
public class NaptanStopData implements NaptanXMLData {

    // Matches ID for TFGM gtfs data
    final private String atcoCode;

    final private String naptanCode;
    final private NaptanXMLPlace place;
    final private NaptanXMLDescriptor descriptor;
    final private NaptanXMLStopClassification stopClassification;
    final private List<NaptanXMLStopAreaRef> stopAreas;

    @JsonCreator
    public NaptanStopData(@JsonProperty("AtcoCode") String atcoCode,
                          @JsonProperty("NaptanCode") String naptanCode,
                          @JsonProperty("Place") NaptanXMLPlace place,
                          @JsonProperty("Descriptor") NaptanXMLDescriptor descriptor,
                          @JsonProperty("StopClassification") NaptanXMLStopClassification stopClassification,
                          @JacksonXmlProperty(localName = "StopAreas") List<NaptanXMLStopAreaRef> stopAreas) {
        this.atcoCode = atcoCode;
        this.naptanCode = naptanCode;
        this.place = place;
        this.descriptor = descriptor;
        this.stopClassification = stopClassification;
        this.stopAreas = stopAreas;
    }

    public IdFor<NaptanRecord> getAtcoCode() {
        return NaptanRecord.createId(atcoCode);
    }

    public boolean hasValidAtcoCode() {
        if (atcoCode==null) {
            return false;
        }
        return !atcoCode.isBlank();
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
//
//    public String getNptgLocality() {
//        return place.getNptgLocalityRef();
//    }

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
