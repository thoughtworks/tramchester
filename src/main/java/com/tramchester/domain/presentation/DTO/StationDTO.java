package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProximityGroup;

public class StationDTO {

    private final String id;
    private final String name;
    private final LatLong latLong;
    private final boolean tram;
    private final ProximityGroup proximityGroup;

    public StationDTO(Station other, ProximityGroup proximityGroup) {
        this.id = other.getId();
        this.name = other.getName();
        this.latLong = other.getLatLong();
        this.tram = other.isTram();
        this.proximityGroup = proximityGroup;

    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LatLong getLatLong() {
        return latLong;
    }

    public boolean isTram() {
        return tram;
    }

    public ProximityGroup getProximityGroup() {
        return proximityGroup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StationDTO station = (StationDTO) o;

        return id.equals(station.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
