package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("Place")
public class NaptanXMLPlace {

    private final NaptanXMLLocation location;
    private final String suburb;
    private final String town;
    private final String nptgLocalityRef;

    @JsonCreator
    public NaptanXMLPlace(@JsonProperty("Location") NaptanXMLLocation location,
                          @JsonProperty("Suburb") String suburb,
                          @JsonProperty("Town") String town,
                          @JsonProperty("NptgLocalityRef") String nptgLocalityRef) {
        this.location = location;
        this.suburb = suburb;
        this.town = town;
        this.nptgLocalityRef = nptgLocalityRef;

    }

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
