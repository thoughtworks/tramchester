package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.places.Location;

import java.util.Set;

@SuppressWarnings("unused")
public class StationRefDTO {
    private boolean pickUp;
    private boolean dropOff;
    private String id;
    private String name;
    private String area;
    private Set<TransportMode> transportModes;

    public StationRefDTO(Location<?> location) {
        this.id = location.forDTO();
        this.name = location.getName();
        this.transportModes = location.getTransportModes();
        this.area = location.getArea();
        this.pickUp = location.hasPickup();
        this.dropOff = location.hasDropoff();
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

    public boolean getPickUp() {
        return pickUp;
    }

    public boolean getDropOff() {
        return dropOff;
    }
}
