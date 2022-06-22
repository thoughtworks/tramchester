package com.tramchester.dataimport.NaPTAN.xml.stopArea;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.tramchester.dataimport.NaPTAN.NaptanXMLData;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanXMLLocation;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanStopAreaType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName("StopAreas")
@JsonTypeName("StopArea")
public class NaptanStopAreaData implements NaptanXMLData {

    private final String name;
    private final String stopAreaCode;
    private final NaptanXMLLocation location;
    private final String stopAreaType;
    private final String status;

    @JsonCreator
    public NaptanStopAreaData(@JsonProperty("Name") String name,
                              @JsonProperty("StopAreaCode") String stopAreaCode,
                              @JsonProperty("Location") NaptanXMLLocation location,
                              @JsonProperty("StopAreaType") String stopAreaType,
                              @JacksonXmlProperty(isAttribute = true, localName = "Status") String status) {
        this.name = name;
        this.stopAreaCode = stopAreaCode;

        this.location = location;
        this.stopAreaType = stopAreaType;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public String getStopAreaCode() {
        if (stopAreaCode==null) {
            return "";
        }
        return stopAreaCode;
    }

    public GridPosition getGridPosition() {
        return location.getGridPosition();
    }

    public String getStatus() {
        if (status==null) {
            return "";
        }
        return status;
    }

    @JsonIgnore
    public boolean isActive() {
        return "active".equals(status);
    }

    public NaptanStopAreaType getAreaType() {
        return NaptanStopAreaType.parse(stopAreaType);
    }
}
