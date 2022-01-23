package com.tramchester.dataimport.NaPTAN.xml;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("Place")
public class NaptanXMLPlace {

    @JsonProperty("Location")
    private NaptanXMLLocation location;

    @JsonProperty("Suburb")
    private String suburb;

    @JsonProperty("Town")
    private String town;

    public NaptanXMLLocation getLocation() {
        return location;
    }

    public String getSuburb() {
        return suburb==null ? "" : suburb;
    }

    public String getTown() {
        return town==null ? "" : town;

    }
}
