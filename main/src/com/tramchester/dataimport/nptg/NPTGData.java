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

    @JsonProperty("NptgLocalityCode")
    private String nptgLocalityCode;

    @JsonProperty("LocalityName")
    private String localityName;

    @JsonProperty("AdministrativeAreaCode")
    private String administrativeAreaCode;

    @JsonProperty("QualifierName")
    private String qualifierName;

    public NPTGData() {
        // deserialisation
    }

    public String getNptgLocalityCode() {
        return nptgLocalityCode;
    }

    public String getLocalityName() {
        return localityName;
    }

    public String getAdministrativeAreaCode() {
        return administrativeAreaCode;
    }

    public String getQualifierName() { return qualifierName; }
}
