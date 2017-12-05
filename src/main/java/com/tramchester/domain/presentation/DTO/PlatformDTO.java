package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.Platform;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;

public class PlatformDTO {

    private String id;
    private String name;
    private String platformNumber;
    private StationDepartureInfo stationDepartureInfo;

    public PlatformDTO() {
        // for deserialisation
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

    @Override
    public String toString() {
        return "PlatformDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", platformNumber='" + platformNumber + '\'' +
                ", stationDepartureInfo=" + stationDepartureInfo +
                '}';
    }

    public PlatformDTO(Platform original) {
        this.id = original.getId();
        this.name = original.getName();
        this.platformNumber = original.getPlatformNumber();
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

    public StationDepartureInfo getStationDepartureInfo() {
        return stationDepartureInfo;
    }

    public void setDepartureInfo(StationDepartureInfo stationDepartureInfo) {
        this.stationDepartureInfo = stationDepartureInfo;
    }
}
