package com.tramchester.domain.presentation.DTO;

import java.util.List;

public class TramsPositionsDTO {
    private List<TramPositionDTO> positionsList;

    // TODO Remove this
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

    @Deprecated
    public boolean getBuses() {
        return buses;
    }
}
