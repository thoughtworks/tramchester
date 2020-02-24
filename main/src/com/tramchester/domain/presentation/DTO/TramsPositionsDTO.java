package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.presentation.TramPositionDTO;

import java.util.List;

public class TramsPositionsDTO {
    private List<TramPositionDTO> positionsList;
    private boolean buses;

    public TramsPositionsDTO() {
        // deserialisation
    }

    public TramsPositionsDTO(List<TramPositionDTO> positionsList, boolean buses) {
        this.positionsList = positionsList;
        this.buses = buses;
    }

    public List<TramPositionDTO> getPositionsList() {
        return positionsList;
    }

    public boolean getBuses() {
        return buses;
    }
}
