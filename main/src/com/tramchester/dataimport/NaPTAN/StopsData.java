package com.tramchester.dataimport.NaPTAN;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StopsData {
    @JsonProperty("ATCOCode")
    private String atcoCode;

    @JsonProperty("NaptanCode")
    private String naptanCode;

    @JsonProperty("LocalityName")
    private String localityName;

    @JsonProperty("ParentLocalityName")
    private String parentLocalityName;

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
}
