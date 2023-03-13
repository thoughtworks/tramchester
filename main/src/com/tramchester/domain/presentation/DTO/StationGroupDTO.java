package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.NaptanArea;

import java.util.List;

public class StationGroupDTO {
    private IdForDTO areaId;
    private List<LocationRefWithPosition> contained;

    public StationGroupDTO() {
        // deserialization
    }

    public StationGroupDTO(IdFor<NaptanArea> areaId, List<LocationRefWithPosition> contained) {
        this.areaId = new IdForDTO(areaId);
        this.contained = contained;
    }

    public List<LocationRefWithPosition> getContained() {
        return contained;
    }

    public IdForDTO getAreaId() {
        return areaId;
    }
}
