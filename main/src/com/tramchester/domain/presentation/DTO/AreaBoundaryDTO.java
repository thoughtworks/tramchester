package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.presentation.LatLong;

import java.util.List;

public class AreaBoundaryDTO {
    private List<LatLong> points;
    private String areaId;
    private String areaName;

    public AreaBoundaryDTO(List<LatLong> points, IdFor<NaptanArea> areaId, String areaName) {
        this.points = points;
        this.areaId = areaId.forDTO();
        this.areaName = areaName;
    }

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
}
