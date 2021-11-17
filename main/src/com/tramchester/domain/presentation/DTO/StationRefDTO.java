package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.places.Location;

import java.util.Set;

@SuppressWarnings("unused")
public class StationRefDTO {
    private String id;
    private String name;
    private String area;
    private Set<TransportMode> transportModes;

    public StationRefDTO(Location<?> station) {
        this.id = station.forDTO();
        this.name = station.getName();
        this.transportModes = station.getTransportModes();
        this.area = station.getArea();
    }

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
                ", area='" + area + '\'' +
                ", transportModes=" + transportModes +
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

    public Set<TransportMode> getTransportModes() {
        return transportModes;
    }

    public String getArea() {
        return area;
    }
}
