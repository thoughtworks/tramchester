package com.tramchester.dataimport.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AgencyData {

    @JsonProperty("agency_id")
    private String id;

    @JsonProperty("agency_name")
    private String name;

    public AgencyData() {
        // deserialisation
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
}
