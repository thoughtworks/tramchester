package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.presentation.ProximityGroup;

import java.util.LinkedList;
import java.util.List;

public class StationListDTO {

    private List<StationDTO> stations;
    private List<Note> notes;
    private List<ProximityGroup> proximityGroups;

    public StationListDTO() {
        // deserialization
    }

    public StationListDTO(List<StationDTO> stations, List<ProximityGroup> proximityGroups) {
        this(stations, new LinkedList<>(), proximityGroups);
    }

    public StationListDTO(List<StationDTO> stations, List<Note> notes, List<ProximityGroup> proximityGroups) {
        this.stations = stations;
        this.notes = notes;
        this.proximityGroups = proximityGroups;
    }

    public List<StationDTO> getStations() {
        return stations;
    }

    public List<Note> getNotes() {
        return notes;
    }

    public List<ProximityGroup> getProximityGroups() {
        return proximityGroups;
    }
}
