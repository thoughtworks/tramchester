package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;

public class StationRefWithPosition extends StationRefDTO {

    private LatLong latLong;

    public StationRefWithPosition(Location station) {
        super(station);
        this.latLong = station.getLatLong();
    }

    @SuppressWarnings("unused")
    public StationRefWithPosition() {
        // deserialisation
    }

    public LatLong getLatLong() {
        return latLong;
    }
}
