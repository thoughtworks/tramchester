package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.ProximityGroup;

public class StationDTO extends LocationDTO {

    private ProximityGroup proximityGroup;

    public StationDTO(Station other, ProximityGroup proximityGroup) {
       super(other);
       this.proximityGroup = proximityGroup;
    }

    public StationDTO() {
        // deserialisation
    }

    public ProximityGroup getProximityGroup() {
        return proximityGroup;
    }

}
