package com.tramchester.livedata;

import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.presentation.DTO.PlatformDTO;

public interface EnrichPlatform {
    void enrich(PlatformDTO platform, TramServiceDate tramServiceDate, TramTime queryTime);
}
