package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LocationDTO {
    private String area;
    private String id;
    private String name;
    private LatLong latLong;
    private List<PlatformDTO> platforms;

    private List<RouteRefDTO> routes;
    private TransportMode transportMode;
    private LocationType locationType;

    public LocationDTO() {
        // deserialisation
    }

    public LocationDTO(PostcodeLocation source) {
        this(source, Collections.emptyList(), Collections.emptyList());
        platforms = Collections.emptyList();
        routes = Collections.emptyList();
    }

    public LocationDTO(Station source) {
        this(source,
                source.hasPlatforms() ? source.getPlatforms().stream().map(PlatformDTO::new).collect(Collectors.toList()) : Collections.emptyList(),
                source.getRoutes().stream().map(RouteRefDTO::new).collect(Collectors.toList()));
//        platforms = new LinkedList<>();
//        if (source.hasPlatforms()) {
//            Set<Platform> sourcePlatforms = source.getPlatforms();
//            sourcePlatforms.forEach(platform -> platforms.add(new PlatformDTO(platform)));
//        }
//        routes = new LinkedList<>();
//        source.getRoutes().forEach(route -> routes.add(new RouteRefDTO(route)));
    }

    private LocationDTO(Location<?> source, List<PlatformDTO> platforms, List<RouteRefDTO> routes) {
        this.id = source.getId().forDTO();
        this.name = source.getName();
        this.latLong = source.getLatLong();
        this.transportMode = source.getTransportMode();
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
        return transportMode.equals(TransportMode.Tram);
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

    public TransportMode getTransportMode() {
        return transportMode;
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
                ", transportMode=" + transportMode +
                '}';
    }


}
