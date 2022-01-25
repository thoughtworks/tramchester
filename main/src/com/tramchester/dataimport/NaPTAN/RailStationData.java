package com.tramchester.dataimport.NaPTAN;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.HasGridPosition;

@Deprecated
public class RailStationData implements HasGridPosition {
    // Matches ID in main naptan Stops.csv file
    @JsonProperty("AtcoCode")
    private String atcoCode;

    // Matches tiploc ID in the rail data
    @JsonProperty("TiplocCode")
    private String tiplocCode;

    // need positional data so only load data required for current geo bounds
    @JsonProperty("Easting")
    private Integer easting;

    @JsonProperty("Northing")
    private Integer northing;

    @Override
    public GridPosition getGridPosition() {
        if (easting==0 || northing==0) {
            return GridPosition.Invalid;
        }
        return new GridPosition(easting, northing);
    }

    public String getTiploc() {
        return tiplocCode;
    }

    public String getActo() {
        return atcoCode;
    }
}
