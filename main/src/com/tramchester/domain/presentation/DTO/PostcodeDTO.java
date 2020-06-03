package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.ProximityGroup;

public class PostcodeDTO extends LocationDTO {

    private ProximityGroup proximityGroup;

    public PostcodeDTO(PostcodeLocation postcodeLocation) {
        super(postcodeLocation);
        this.proximityGroup = ProximityGroup.POSTCODES;
    }

    public PostcodeDTO() {
        // deserialisation
    }

    public ProximityGroup getProximityGroup() {
        return proximityGroup;
    }

}
