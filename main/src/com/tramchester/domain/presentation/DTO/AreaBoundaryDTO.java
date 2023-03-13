package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.repository.naptan.NaptanStopAreaType;

import java.util.List;

public class AreaBoundaryDTO extends BoundaryDTO {
    private IdForDTO areaId;
    private String areaName;
    private NaptanStopAreaType type;

    public AreaBoundaryDTO(List<LatLong> points, NaptanArea area) {
        super(points);
        this.areaId = IdForDTO.createFor(area);
        this.areaName = area.getName();
        this.type = area.getType();
    }

    public AreaBoundaryDTO() {
        // for deserialisation
    }

    public IdForDTO getAreaId() {
        return areaId;
    }

    public String getAreaName() {
        return areaName;
    }

    public NaptanStopAreaType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "AreaBoundaryDTO{" +
                "areaId=" + areaId +
                ", areaName='" + areaName + '\'' +
                ", type=" + type +
                "} " + super.toString();
    }
}
