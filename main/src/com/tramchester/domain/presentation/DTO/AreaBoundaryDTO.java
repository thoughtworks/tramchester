package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.repository.naptan.NaptanStopAreaType;

import java.util.List;

public class AreaBoundaryDTO {
    private List<LatLong> points;
    private String areaId;
    private String areaName;
    private NaptanStopAreaType type;

    public AreaBoundaryDTO(List<LatLong> points, NaptanArea area) {
        this.points = points;
        this.areaId = area.getId().forDTO();
        this.areaName = area.getName();
        this.type = area.getType();
    }

//    public AreaBoundaryDTO(List<LatLong> points, IdFor<NaptanArea> areaId, String areaName, NaptanStopAreaType type) {
//        this.points = points;
//        this.areaId = areaId.forDTO();
//        this.areaName = areaName;
//        this.type = type;
//    }

    public AreaBoundaryDTO() {
        // for deserialisation
    }

    public List<LatLong> getPoints() {
        return points;
    }

    public String getAreaId() {
        return areaId;
    }

    public String getAreaName() {
        return areaName;
    }

    public NaptanStopAreaType getType() {
        return type;
    }
}
