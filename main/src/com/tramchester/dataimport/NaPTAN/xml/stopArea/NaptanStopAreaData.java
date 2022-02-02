package com.tramchester.dataimport.NaPTAN.xml.stopArea;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.tramchester.dataimport.NaPTAN.NaptanXMLData;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanXMLLocation;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanStopAreaType;

@JsonRootName("StopAreas")
@JsonTypeName("StopArea")
public class NaptanStopAreaData implements NaptanXMLData {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("StopAreaCode")
    private String stopAreaCode;

    @JsonProperty("Location")
    private NaptanXMLLocation location;

    @JsonProperty("StopAreaType")
    private String stopAreaType;

    @JacksonXmlProperty(isAttribute = true, localName = "Status")
    private String status;

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
