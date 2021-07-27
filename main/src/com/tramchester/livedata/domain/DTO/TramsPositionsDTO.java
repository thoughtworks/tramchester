package com.tramchester.livedata.domain.DTO;

import com.tramchester.livedata.domain.DTO.TramPositionDTO;

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
