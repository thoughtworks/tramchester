package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.places.Location;

public class StationDTO extends LocationDTO {

    public StationDTO(Location other) {
       super(other);
    }

    public StationDTO() {
        // deserialisation
    }

}
