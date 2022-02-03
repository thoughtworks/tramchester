package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;

import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class LocationDTO {
    private String id;
    private String name;
    private LatLong latLong;
    private Set<TransportMode> transportModes;
    private LocationType locationType;
    private List<PlatformDTO> platforms;
    private List<RouteRefDTO> routes;
    private boolean markedInterchange;

    public LocationDTO(Location<?> source, List<PlatformDTO> platforms, List<RouteRefDTO> routes) {
        this.id = source.getId().forDTO();
        this.name = source.getName();
        this.latLong = source.getLatLong();
        this.transportModes = source.getTransportModes();
        this.locationType = source.getLocationType();
        this.platforms = platforms;
        this.routes = routes;
        this.markedInterchange = source.isMarkedInterchange();
    }

    public LocationDTO() {
        // deserialisation
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

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public boolean isTram() {
        return transportModes.contains(TransportMode.Tram);
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public boolean hasPlatforms() {
        return !platforms.isEmpty();
    }

    public List<RouteRefDTO> getRoutes() {
        return routes;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public List<PlatformDTO> getPlatforms() {
        return platforms;
    }

    public Set<TransportMode> getTransportModes() {
        return transportModes;
    }

    public LocationType getLocationType() { return locationType; }

    public boolean getIsMarkedInterchange() {
        return markedInterchange;
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

    @Override
    public String toString() {
        return "LocationDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", latLong=" + latLong +
                ", transportModes=" + transportModes +
                ", locationType=" + locationType +
                ", platforms=" + platforms +
                ", routes=" + routes +
                ", markedInterchange=" + markedInterchange +
                '}';
    }
}
