package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;

@JsonTypeName("Translation")
public class NaptanXMLLocationTranslation {

    @JsonProperty("Easting")
    private Integer easting;

    @JsonProperty("Northing")
    private Integer northing;

    @JsonProperty("Latitude")
    private Double latitude;

    @JsonProperty("Longitude")
    private Double longitude;

    public GridPosition getGridPosition() {
        if (easting==0 || northing==0) {
            return GridPosition.Invalid;
        }
        return new GridPosition(easting, northing);
    }

    public LatLong getLatLong() {
        if (latitude==0 || longitude==0) {
            return LatLong.Invalid;
        }
        return new LatLong(latitude, longitude);
    }

    @Override
    public String toString() {
        return "NaptanXMLLocationTranslation{" +
                "easting=" + easting +
                ", northing=" + northing +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

}
