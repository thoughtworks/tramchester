package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tramchester.domain.Platform;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LocationDTO {
    private String area;
    private String id;
    private String name;
    private LatLong latLong;
    private List<PlatformDTO> platforms;
    private TransportMode transportMode;

    public LocationDTO() {
        // deserialisation
    }


    public LocationDTO(PostcodeLocation source) {
        this.id = source.getId().forDTO();
        this.name = source.getName();
        this.latLong = source.getLatLong();
        this.transportMode = source.getTransportMode();
        this.area = source.getArea();
        platforms = Collections.emptyList();
    }

    public LocationDTO(Station source) {
        this.id = source.getId().forDTO();
        this.name = source.getName();
        this.latLong = source.getLatLong();
        this.transportMode = source.getTransportMode();
        this.area = source.getArea();
        platforms = new LinkedList<>();
        if (source.hasPlatforms()) {
            List<Platform> sourcePlatforms = source.getPlatforms();
            sourcePlatforms.forEach(platform -> platforms.add(new PlatformDTO(platform)));
        }
    }

//    private LocationDTO(Location source) {
//        this.id = source.getId().forDTO();
//        this.name = source.getName();
//        this.latLong = source.getLatLong();
//        this.transportMode = source.getTransportMode();
//        this.area = source.getArea();
//        platforms = new LinkedList<>();
//        if (source.hasPlatforms()) {
//            List<Platform> sourcePlatforms = source.getPlatforms();
//            sourcePlatforms.forEach(platform -> platforms.add(new PlatformDTO(platform)));
//        }
//    }

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

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public List<PlatformDTO> getPlatforms() {
        return platforms;
    }

    @Override
    public String toString() {
        return "LocationDTO{" +
                "area='" + area + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", latLong=" + latLong +
                ", platforms=" + platforms +
                ", transportMode=" + transportMode +
                '}';
    }

    public TransportMode getTransportMode() {
        return transportMode;
    }
}
