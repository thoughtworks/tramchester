package com.tramchester.domain.presentation.DTO;

import java.util.List;

public class StationGroupDTO {
    private String areaId;
    private List<StationRefWithPosition> contained;

    public StationGroupDTO() {
        // deserialization
    }

    public StationGroupDTO(String areaId, List<StationRefWithPosition> contained) {
        this.areaId = areaId;
        this.contained = contained;
    }

    public List<StationRefWithPosition> getContained() {
        return contained;
    }



    public String getAreaId() {
        return areaId;
    }
}
