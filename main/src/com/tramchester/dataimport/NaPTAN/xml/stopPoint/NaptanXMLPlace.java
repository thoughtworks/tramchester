package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

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

    @JsonProperty("NptgLocalityRef")
    private String nptgLocalityRef;

    public NaptanXMLLocation getLocation() {
        return location;
    }

    public String getSuburb() {
        return suburb==null ? "" : suburb;
    }

    public String getTown() {
        return town==null ? "" : town;
    }

    public String getNptgLocalityRef() {
        return nptgLocalityRef==null? "" : nptgLocalityRef;
    }

    @Override
    public String toString() {
        return "NaptanXMLPlace{" +
                "location=" + location +
                ", suburb='" + suburb + '\'' +
                ", town='" + town + '\'' +
                ", nptgLocalityRef='" + nptgLocalityRef + '\'' +
                '}';
    }
}
