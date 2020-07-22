package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;

public class StationDTO extends LocationDTO {

    public StationDTO(Station other) {
       super(other);
    }

    public StationDTO() {
        // deserialisation
    }

}
