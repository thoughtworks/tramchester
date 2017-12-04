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

    public PlatformDTO(Platform original) {
        this.id = original.getId();
        this.name = original.getName();
        this.platformNumber = original.getPlatformNumber();
        this.stationDepartureInfo = original.getDepartureInfo();
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
}
