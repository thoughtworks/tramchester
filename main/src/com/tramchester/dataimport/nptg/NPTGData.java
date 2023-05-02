package com.tramchester.dataimport.nptg;

import com.fasterxml.jackson.annotation.JsonProperty;

/***
 * National Public Transport Gazetteer
 *
 * https://data.gov.uk/dataset/3b1766bf-04a3-44f5-bea9-5c74cf002e1d/national-public-transport-gazetteer-nptg
 *
 *  Cross referenced by naptan data via the nptgLocalityCode
 */
public class NPTGData {

    @JsonProperty("ATCOCode")
    private String actoCode;

    @JsonProperty("LocalityName")
    private String localityName;

    @JsonProperty("ParentLocalityName")
    private String parentLocalityName;

    public NPTGData() {
        // deserialisation
    }

    public String getActoCode() {
        return actoCode;
    }

    public String getLocalityName() {
        return localityName;
    }

    @Override
    public String toString() {
        return "NPTGData{" +
                "actoCode='" + actoCode + '\'' +
                ", localityName='" + localityName + '\'' +
                ", parentLocalityName='" + parentLocalityName + '\'' +
                '}';
    }

    public String getParentLocalityName() {
        return parentLocalityName;
    }
}
