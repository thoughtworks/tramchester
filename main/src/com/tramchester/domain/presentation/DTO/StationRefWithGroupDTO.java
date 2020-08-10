package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.ProximityGroup;

public class StationRefWithGroupDTO extends StationRefDTO {
    private ProximityGroup proximityGroup;

    public StationRefWithGroupDTO(Location station, ProximityGroup proximityGroup) {
        super(station);
        this.proximityGroup = proximityGroup;
    }

    @SuppressWarnings("unused")
    public StationRefWithGroupDTO() {
        // deserialisation
    }

    public ProximityGroup getProximityGroup() {
        return proximityGroup;
    }
}
