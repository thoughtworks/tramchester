package com.tramchester.dataimport.postcodes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.HasGridPosition;

import java.util.Objects;

public class PostcodeData implements HasGridPosition {

    // Note: required as no header line in the supplied csv files
    public static final String CVS_HEADER = "Postcode,Positional_quality_indicator,Eastings,Northings,Country_code,NHS_regional_HA_code," +
            "NHS_HA_code,Admin_county_code,Admin_district_code,Admin_ward_code";

    private String postcode;

    @JsonProperty("Eastings")
    private int eastings;
    @JsonProperty("Northings")
    private int northings;

    public PostcodeData(String postcode, int eastings, int northings) {
        // for test
        this.postcode = postcode;
        this.eastings = eastings;
        this.northings = northings;
    }

    @SuppressWarnings("unused")
    public PostcodeData() {
        // deserialization
    }

    @SuppressWarnings("unused")
    @JsonProperty("Postcode")
    private void setPostcode(String text) {
        this.postcode = text.replaceAll(" ","");
    }

    public String getId() {
        return postcode;
    }

    @Override
    public String toString() {
        return "PostcodeData{" +
                "postcode='" + postcode + '\'' +
                ", eastings=" + eastings +
                ", northings=" + northings +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostcodeData that = (PostcodeData) o;
        return postcode.equals(that.postcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postcode);
    }

    @Override
    public GridPosition getGridPosition() {
        // strictly speaking 0 is a valid position, but many postcodes seem to be to 0,0 in the input file
        if (eastings==0 || northings==0) {
            return GridPosition.Invalid;
        }
        return new GridPosition(eastings, northings);
    }
}
