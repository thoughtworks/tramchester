package com.tramchester.domain.liveUpdates;

import java.time.LocalDateTime;

public interface HasPlatformMessage {
    String getMessage();
    String getLocation();
    LocalDateTime getLastUpdate();
}
