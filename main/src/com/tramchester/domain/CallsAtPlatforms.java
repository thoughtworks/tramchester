package com.tramchester.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.time.TramTime;

public interface CallsAtPlatforms {
    @JsonIgnore
    IdSet<Platform> getCallingPlatformIds();

    TramTime getQueryTime();
}
