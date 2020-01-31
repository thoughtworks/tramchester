package com.tramchester.domain.presentation;

import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.LocationDTO;

import java.util.Set;

public class TramPositionDTO {
    private LocationDTO first;
    private LocationDTO second;
    private Set<DepartureDTO> trams;
    private int cost;

    public TramPositionDTO() {
        // deserialisation
    }

    public TramPositionDTO(LocationDTO first, LocationDTO second, Set<DepartureDTO> trams, int cost) {
        this.first = first;
        this.second = second;
        this.trams = trams;
        this.cost = cost;
    }

    public LocationDTO getFirst() {
        return first;
    }

    public LocationDTO getSecond() {
        return second;
    }

    public Set<DepartureDTO> getTrams() {
        return trams;
    }

    public int getCost() {
        return cost;
    }
}
