package com.tramchester.livedata.domain.liveUpdates;

import com.tramchester.domain.Platform;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

public class PlatformMessage {
    private final IdFor<Platform> stationPlatform;
    private final String message;
    private final LocalDateTime lastUpdate;
    private final Station station;
    private final String displayId;

    public PlatformMessage(IdFor<Platform> stationPlatform, String message, LocalDateTime lastUpdate, Station station, String displayId) {
        this.stationPlatform = stationPlatform;
        this.message = message;
        this.lastUpdate = lastUpdate;
        this.station = station;
        this.displayId = displayId;
    }

    public PlatformMessage(TramStationDepartureInfo departureInfo) {
        this(departureInfo.getStationPlatform(), departureInfo.getMessage(), departureInfo.getLastUpdate(),
                departureInfo.getStation(), departureInfo.getDisplayId());
    }

    @NotNull
    public String getMessage() {
        return message;
    }

    public Station getStation() {
        return station;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public String toString() {
        return "PlatformMessage{" +
                "stationPlatform=" + stationPlatform +
                ", message='" + message + '\'' +
                ", lastUpdate=" + lastUpdate +
                ", station=" + HasId.asId(station) +
                ", displayId='" + displayId + '\'' +
                '}';
    }

    public String getDisplayId() {
        return displayId;
    }
}
