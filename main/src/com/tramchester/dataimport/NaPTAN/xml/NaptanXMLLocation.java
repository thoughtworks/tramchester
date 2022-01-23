package com.tramchester.dataimport.NaPTAN.xml;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.tramchester.geo.GridPosition;

@JsonTypeName("Location")
public class NaptanXMLLocation {

    @JsonProperty("Translation")
    private NaptanXMLLocationTranslation translation;

    public GridPosition getGridPosition() {
        return translation.getGridPosition();
    }
}
