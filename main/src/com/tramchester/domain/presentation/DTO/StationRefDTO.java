package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tramchester.domain.places.Location;

public class StationRefDTO {
    private String id;
    private String name;
    private Boolean tram;

    public StationRefDTO(Location station) {
        this.id = station.getId();
        this.name = station.getName();
        this.tram = station.isTram();
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

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public Boolean isTram() { return tram; }

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
