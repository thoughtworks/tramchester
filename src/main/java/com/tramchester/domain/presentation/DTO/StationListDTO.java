package com.tramchester.domain.presentation.DTO;

import java.util.LinkedList;
import java.util.List;

public class StationListDTO {

    private List<StationDTO> stations;
    private List<String> notes;

    public StationListDTO() {
        // deserialization
    }

    public StationListDTO(List<StationDTO> stations) {
        this(stations, new LinkedList<>());
    }

    public StationListDTO(List<StationDTO> stations, List<String> notes) {
        this.stations = stations;
        this.notes = notes;
    }

    public List<StationDTO> getStations() {
        return stations;
    }

    public List<String> getNotes() {
        return notes;
    }
}
