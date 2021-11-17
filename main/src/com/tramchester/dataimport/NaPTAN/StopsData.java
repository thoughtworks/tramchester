package com.tramchester.dataimport.NaPTAN;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.geo.GridPosition;


public class StopsData {

    // Matches ID for TFGM gtfs data
    @JsonProperty("ATCOCode")
    private String atcoCode;

    @JsonProperty("NaptanCode")
    private String naptanCode;

    // e.g. Manchester City Centre
    @JsonProperty("LocalityName")
    private String localityName;

    // e.g. Manchester
    @JsonProperty("ParentLocalityName")
    private String parentLocalityName;

    // e.g. Stand A, Stand 2, etc
    @JsonProperty("Indicator")
    private String indicator;

    @JsonProperty("Easting")
    private Integer easting;

    @JsonProperty("Northing")
    private Integer northing;

    public StopsData() {
        // deserialisation
    }

    public String getAtcoCode() {
        return atcoCode;
    }

    public String getNaptanCode() {
        return naptanCode;
    }

    public String getLocalityName() {
        return localityName;
    }

    public String getParentLocalityName() {
        return parentLocalityName;
    }

    public String getIndicator() {
        return indicator;
    }

    public GridPosition getGridPosition() {
        if (easting==0 || northing==0) {
            return GridPosition.Invalid;
        }
        return new GridPosition(easting, northing);
    }
}
