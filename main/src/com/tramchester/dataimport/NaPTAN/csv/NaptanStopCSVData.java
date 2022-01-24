package com.tramchester.dataimport.NaPTAN.csv;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.dataimport.NaPTAN.NaptanRailStationData;
import com.tramchester.dataimport.NaPTAN.xml.NaptanStopData;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanStopType;

@Deprecated
public class NaptanStopCSVData implements NaptanStopData {

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

    @JsonProperty("CommonName")
    private String commonName;

    @JsonProperty("StopType")
    private String stopType;

    @JsonProperty("Suburb")
    private String suburb;

    @JsonProperty("Town")
    private String town;

    @JsonProperty("NptgLocalityCode")
    private String nptgLocalityCode;

    public NaptanStopCSVData() {
        // deserialisation
    }

    public String getAtcoCode() {
        return atcoCode;
    }

    public String getNaptanCode() {
        return naptanCode;
    }

    public String getSuburb() {
        return suburb.isBlank() ? localityName : suburb;
    }

    public String getTown() {
        return town.isBlank() ? parentLocalityName : town;
    }

    public String getIndicator() {
        return indicator;
    }

    public String getNptgLocality() {
        return nptgLocalityCode;
    }

    @Override
    public boolean hasRailInfo() {
        return false;
    }

    @Override
    public NaptanRailStationData getRailInfo() {
        return null;
    }

    @Override
    public GridPosition getGridPosition() {
        if (easting==0 || northing==0) {
            return GridPosition.Invalid;
        }
        return new GridPosition(easting, northing);
    }

    public String getCommonName() {
        return commonName;
    }

    public NaptanStopType getStopType() {
        return NaptanStopType.parse(stopType);
    }
}
