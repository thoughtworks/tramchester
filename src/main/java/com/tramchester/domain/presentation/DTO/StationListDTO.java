package com.tramchester.domain.presentation.DTO;

import java.util.List;

public class StationListDTO {

    private List<StationDTO> stations;

    public StationListDTO() {
        // deserialization
    }

    public StationListDTO(List<StationDTO> stations) {
        this.stations = stations;
    }

    public List<StationDTO> getStations() {
        return stations;
    }
}
