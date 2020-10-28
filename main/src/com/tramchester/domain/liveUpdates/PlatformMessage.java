package com.tramchester.domain.liveUpdates;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

public class PlatformMessage implements HasPlatformMessage {
    private final IdFor<Platform> stationPlatform;
    private final String message;
    private final LocalDateTime lastUpdate;
    private final Station station; // TODO Just use ID?

    public PlatformMessage(IdFor<Platform> stationPlatform, String message, LocalDateTime lastUpdate, Station station) {
        this.stationPlatform = stationPlatform;
        this.message = message;
        this.lastUpdate = lastUpdate;
        this.station = station;
    }

    public PlatformMessage(StationDepartureInfo departureInfo) {
        this(departureInfo.getStationPlatform(), departureInfo.getMessage(), departureInfo.getLastUpdate(), departureInfo.getStation());
    }

    @NotNull
    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Station getStation() {
        return station;
    }

    @Override
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public String toString() {
        return "PlatformMessage{" +
                "stationPlatform=" + stationPlatform +
                ", message='" + message + '\'' +
                ", lastUpdate=" + lastUpdate +
                ", station=" + station +
                '}';
    }
}
