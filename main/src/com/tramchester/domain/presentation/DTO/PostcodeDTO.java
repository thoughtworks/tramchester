package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.places.PostcodeLocation;

public class PostcodeDTO extends LocationDTO {
    public PostcodeDTO(PostcodeLocation postcodeLocation) {
        super(postcodeLocation);
    }

    public PostcodeDTO() {
        // deserialisation
    }
}
