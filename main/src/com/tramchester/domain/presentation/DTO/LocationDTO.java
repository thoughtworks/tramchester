package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tramchester.domain.places.*;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.presentation.LatLong;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class LocationDTO {
    private String area;
    private String id;
    private String name;
    private LatLong latLong;
    private List<PlatformDTO> platforms;

    private List<RouteRefDTO> routes;
    private Set<TransportMode> transportModes;
    private LocationType locationType;

    public LocationDTO() {
        // deserialisation
    }

    public LocationDTO(PostcodeLocation source) {
        this(source, Collections.emptyList(), Collections.emptyList());
    }

    public LocationDTO(Station source) {
        this(source,
                source.hasPlatforms() ? source.getPlatforms().stream().map(PlatformDTO::new).collect(Collectors.toList()) : Collections.emptyList(),
                Stream.concat(source.getDropoffRoutes().stream(), source.getPickupRoutes().stream()).map(RouteRefDTO::new).collect(Collectors.toList()));
    }

    private LocationDTO(Location<?> source, List<PlatformDTO> platforms, List<RouteRefDTO> routes) {
        this.id = source.getId().forDTO();
        this.name = source.getName();
        this.latLong = source.getLatLong();
        this.transportModes = source.getTransportModes();
        this.area = source.getArea();
        this.locationType = source.getLocationType();
        this.platforms = platforms;
        this.routes = routes;
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

    public String getArea() {
        return area;
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
                "area='" + area + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", latLong=" + latLong +
                ", platforms=" + platforms +
                ", routes=" + routes +
                ", transportModes=" + transportModes +
                '}';
    }


}
