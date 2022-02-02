package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.LatLong;

@SuppressWarnings("unused")
public class LocationRefWithPosition extends LocationRefDTO {

    private LatLong latLong;

    public LocationRefWithPosition(Location<?> location) {
        super(location);
        this.latLong = location.getLatLong();
    }

    public LocationRefWithPosition() {
        // deserialisation
    }

    public LatLong getLatLong() {
        return latLong;
    }

    @Override
    public String toString() {
        return "LocationRefWithPosition{" +
                "latLong=" + latLong +
                "} " + super.toString();
    }
}
