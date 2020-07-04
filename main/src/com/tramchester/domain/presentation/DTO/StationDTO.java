package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
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

    public StationDTO(Station station, List<PlatformDTO> platformDTOS, ProximityGroup proximityGroup) {
        super(station, platformDTOS);
        this.proximityGroup = proximityGroup;
    }

    public ProximityGroup getProximityGroup() {
        return proximityGroup;
    }
}
