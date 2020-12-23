package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;

import java.util.Set;

public class TramPositionDTO {
    private StationRefWithPosition first;
    private StationRefWithPosition second;
    private Set<DepartureDTO> trams;
    private int cost;

    public TramPositionDTO() {
        // deserialisation
    }

    public TramPositionDTO(StationRefWithPosition first, StationRefWithPosition second, Set<DepartureDTO> trams, int cost) {
        this.first = first;
        this.second = second;
        this.trams = trams;
        this.cost = cost;
    }

    public StationRefWithPosition getFirst() {
        return first;
    }

    public StationRefWithPosition getSecond() {
        return second;
    }

    public Set<DepartureDTO> getTrams() {
        return trams;
    }

    public int getCost() {
        return cost;
    }
}
