package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.places.Location;

import java.util.Set;

@SuppressWarnings("unused")
public class LocationRefDTO {
    private LocationType locationType;
    private boolean pickUp;
    private boolean dropOff;
    private IdForDTO id;
    private String name;
    private Set<TransportMode> transportModes;

    public LocationRefDTO(Location<?> location) {
        this.id = IdForDTO.createFor(location);
        this.name = location.getName();
        this.transportModes = location.getTransportModes();
        this.locationType = location.getLocationType();
        this.pickUp = location.hasPickup();
        this.dropOff = location.hasDropoff();
    }

    public LocationRefDTO() {
        // deserialization
    }

    public IdForDTO getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<TransportMode> getTransportModes() {
        return transportModes;
    }

    public boolean getPickUp() {
        return pickUp;
    }

    public boolean getDropOff() {
        return dropOff;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    @Override
    public String toString() {
        return "LocationRefDTO{" +
                "locationType=" + locationType +
                ", pickUp=" + pickUp +
                ", dropOff=" + dropOff +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", transportModes=" + transportModes +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocationRefDTO that = (LocationRefDTO) o;

        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
