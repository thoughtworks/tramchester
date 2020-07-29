package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.presentation.ProximityGroup;

import java.util.List;

public class StationListDTO {

    private List<StationRefWithGroupDTO> stations;
    private List<ProximityGroup> proximityGroups;

    @SuppressWarnings("unused")
    public StationListDTO() {
        // deserialization
    }

    public StationListDTO(List<StationRefWithGroupDTO> stations, List<ProximityGroup> proximityGroups) {
        this.stations = stations;
        this.proximityGroups = proximityGroups;
    }

    public List<StationRefWithGroupDTO> getStations() {
        return stations;
    }
    
    public List<ProximityGroup> getProximityGroups() {
        return proximityGroups;
    }
}
