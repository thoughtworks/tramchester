package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.HasId;
import com.tramchester.domain.Platform;

public class DTO implements HasId {

    private String id;
    private String name;
    private String platformNumber;
    private StationDepartureInfoDTO stationDepartureInfo;

    public DTO() {
        // for deserialisation
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

    public DTO(Platform original) {
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

    public StationDepartureInfoDTO getStationDepartureInfo() {
        return stationDepartureInfo;
    }

    public void setDepartureInfo(StationDepartureInfoDTO stationDepartureInfo) {
        this.stationDepartureInfo = stationDepartureInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DTO that = (DTO) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
