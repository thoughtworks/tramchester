package com.tramchester.domain.presentation.DTO;

import java.util.List;

public class TramsPositionsDTO {
    private List<TramPositionDTO> positionsList;

    public TramsPositionsDTO() {
        // deserialisation
    }

    public TramsPositionsDTO(List<TramPositionDTO> positionsList) {
        this.positionsList = positionsList;
    }

    public List<TramPositionDTO> getPositionsList() {
        return positionsList;
    }

}
