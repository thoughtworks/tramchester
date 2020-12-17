package com.tramchester.dataimport.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.geo.HasGridPosition;

import java.util.Objects;

public class PostcodeData implements HasGridPosition {

    public static final String CVS_HEADER = "Postcode,Positional_quality_indicator,Eastings,Northings,Country_code,NHS_regional_HA_code," +
            "NHS_HA_code,Admin_county_code,Admin_district_code,Admin_ward_code";

    private String postcode;

    @JsonProperty("Eastings")
    private int eastings;
    @JsonProperty("Northings")
    private int northings;

    public PostcodeData(String postcode, int eastings, int northings) {
        this.postcode = postcode;
        this.eastings = eastings;
        this.northings = northings;
    }

    public PostcodeData() {
        // deserialization
    }

    @JsonProperty("Postcode")
    private void setPostcode(String text) {
        this.postcode = text.replaceAll(" ","");
    }

    public String getId() {
        return postcode;
    }

    public long getEastings() {
        return eastings;
    }

    public long getNorthings() {
        return northings;
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
}
