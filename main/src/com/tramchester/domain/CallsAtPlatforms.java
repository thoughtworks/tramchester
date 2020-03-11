package com.tramchester.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tramchester.domain.time.TramTime;

import java.util.List;

public interface CallsAtPlatforms {
    @JsonIgnore
    List<HasId> getCallingPlatformIds();

    TramTime getQueryTime();
}
