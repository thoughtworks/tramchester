package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.places.StationGroup;

import java.util.List;
import java.util.stream.Collectors;

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

    public static StationGroupDTO create(StationGroup stationGroup) {
        String areaId = stationGroup.getAreaId().forDTO();
        List<StationRefWithPosition> contained = stationGroup.getContained().stream().
                map(StationRefWithPosition::new).collect(Collectors.toList());
        return new StationGroupDTO(areaId, contained);
    }

    public String getAreaId() {
        return areaId;
    }
}
