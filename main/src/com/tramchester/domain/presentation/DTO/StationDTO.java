package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.Location;
import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.ProximityGroup;

import java.util.List;

public class StationDTO extends LocationDTO {

    private ProximityGroup proximityGroup;

    public StationDTO(Location other, ProximityGroup proximityGroup) {
       super(other);
       this.proximityGroup = proximityGroup;
    }

    public StationDTO() {
        // deserialisation
    }

    public StationDTO(Station station, List<DTO> platformDTOS, ProximityGroup proximityGroup) {
        super(station, platformDTOS);
        this.proximityGroup = proximityGroup;
    }

    public ProximityGroup getProximityGroup() {
        return proximityGroup;
    }

}
