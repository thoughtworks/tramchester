package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.places.Location;

public class StationRefDTO {
    private String id;
    private String name;

    public StationRefDTO(Location station) {
        this.id = station.getId();
        this.name = station.getName();
    }

    public StationRefDTO(LocationDTO locationDTO) {
        this.id = locationDTO.getId();
        this.name = locationDTO.getName();
    }

    @SuppressWarnings("unused")
    public StationRefDTO() {
        // deserialization
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "StationRefDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StationRefDTO that = (StationRefDTO) o;

        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
