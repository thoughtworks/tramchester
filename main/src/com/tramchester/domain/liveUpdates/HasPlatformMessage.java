package com.tramchester.domain.liveUpdates;

import com.tramchester.domain.places.Station;

import java.time.LocalDateTime;

public interface HasPlatformMessage {
    String getMessage();
    Station getStation();
    LocalDateTime getLastUpdate();
}
