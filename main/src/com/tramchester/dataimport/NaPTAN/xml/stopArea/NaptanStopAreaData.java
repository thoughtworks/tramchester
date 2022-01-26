package com.tramchester.dataimport.NaPTAN.xml.stopArea;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.tramchester.dataimport.NaPTAN.NaptanXMLData;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanXMLLocation;
import com.tramchester.geo.GridPosition;

@JsonRootName("StopAreas")
@JsonTypeName("StopArea")
public class NaptanStopAreaData implements NaptanXMLData {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("StopAreaCode")
    private String stopAreaCode;

    @JsonProperty("Location")
    private NaptanXMLLocation location;

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
}
