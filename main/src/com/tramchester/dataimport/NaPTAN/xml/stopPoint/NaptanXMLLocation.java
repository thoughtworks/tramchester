package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;

@JsonTypeName("Location")
public class NaptanXMLLocation {

    @JsonProperty("Translation")
    private NaptanXMLLocationTranslation translation;

    public GridPosition getGridPosition() {
        if (translation==null) {
            return GridPosition.Invalid;
        }
        return translation.getGridPosition();
    }

    @Override
    public String toString() {
        return "NaptanXMLLocation{" +
                "translation=" + translation +
                '}';
    }

    public LatLong getLatLong() {
        if (translation==null) {
            return LatLong.Invalid;
        }
        return translation.getLatLong();
    }
}
