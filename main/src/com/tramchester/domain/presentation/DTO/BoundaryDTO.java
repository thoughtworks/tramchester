package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.presentation.LatLong;

import java.util.List;

public class BoundaryDTO {
    protected List<LatLong> points;

    public BoundaryDTO(List<LatLong> points) {
        this.points = points;
    }

    public BoundaryDTO() {
        // deserialisation
    }

    public List<LatLong> getPoints() {
        return points;
    }
}
