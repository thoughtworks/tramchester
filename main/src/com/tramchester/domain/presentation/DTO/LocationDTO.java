package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.Location;
import com.tramchester.domain.presentation.LatLong;

import java.util.LinkedList;
import java.util.List;

public class LocationDTO {
    private String area;
    private String id;
    private String name;
    private LatLong latLong;
    private boolean tram;
    private List<PlatformDTO> platforms;

    public LocationDTO() {
        // deserialisation
    }

    public LocationDTO(Location source) {
        this.id = source.getId();
        this.name = source.getName();
        this.latLong = source.getLatLong();
        this.tram = source.isTram();
        this.area = source.getArea();
        platforms = new LinkedList<>();
        if (source.hasPlatforms()) {
            source.getPlatforms().forEach(platform -> platforms.add(new PlatformDTO(platform)));
        }
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

    public boolean isTram() {
        return tram;
    }

    public String getArea() {
        return area;
    }

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
                ", tram=" + tram +
                ", platforms=" + platforms +
                '}';
    }
}
