package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.Location;
import com.tramchester.domain.presentation.LatLong;

public class LocationDTO implements Location {
    private String id;
    private String name;
    private LatLong latLong;
    private boolean tram;

    public LocationDTO() {
        // deserialisation
    }

    public LocationDTO(Location other) {
        this.id = other.getId();
        this.name = other.getName();
        this.latLong = other.getLatLong();
        this.tram = other.isTram();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LatLong getLatLong() {
        return latLong;
    }

    @Override
    public boolean isTram() {
        return tram;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocationDTO location = (LocationDTO) o;

        return id.equals(location.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
