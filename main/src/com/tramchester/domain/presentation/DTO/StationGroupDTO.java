package com.tramchester.domain.presentation.DTO;

import java.util.List;

public class StationGroupDTO {
    private String areaId;
    private List<LocationRefWithPosition> contained;

    public StationGroupDTO() {
        // deserialization
    }

    public StationGroupDTO(String areaId, List<LocationRefWithPosition> contained) {
        this.areaId = areaId;
        this.contained = contained;
    }

    public List<LocationRefWithPosition> getContained() {
        return contained;
    }



    public String getAreaId() {
        return areaId;
    }
}
