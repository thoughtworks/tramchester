package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.Platform;
import com.tramchester.domain.presentation.LatLong;

public class PlatformDTO {

    private String id;
    private String name;
    private String platformNumber;
    private IdFor<Platform> platformId;
    private LatLong latLong;

    @SuppressWarnings("unused")
    public PlatformDTO() {
        // for deserialisation
    }

    @Override
    public String toString() {
        return "PlatformDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", platformNumber='" + platformNumber + '\'' +
                '}';
    }

    public PlatformDTO(Platform original) {
        platformId = original.getId();
        this.id = platformId.forDTO();
        this.name = original.getName();
        this.platformNumber = original.getPlatformNumber();
        this.latLong = original.getLatLong();
    }

    @JsonIgnore
    public IdFor<Platform> getPlatformId() {
        return platformId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPlatformNumber() {
        return platformNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlatformDTO that = (PlatformDTO) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public LatLong getLatLong() {
        return latLong;
    }
}
