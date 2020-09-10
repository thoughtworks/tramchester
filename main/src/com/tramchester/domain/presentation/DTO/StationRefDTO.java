package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.Location;

public class StationRefDTO {
    private String id;
    private String name;
    private String area;
    private TransportMode transportMode;

    public StationRefDTO(Location<?> location) {
        this.id = location.forDTO();
        this.name = location.getName();
        this.transportMode = location.getTransportMode();
        this.area = location.getArea();
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
                ", transportMode=" + transportMode +
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

    public TransportMode getTransportMode() {
        return transportMode;
    }

    public String getArea() {
        return area;
    }
}
