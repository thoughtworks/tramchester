package com.tramchester.livedata;

import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.repository.LiveDataRepository;

public class LiveDataEnricher implements EnrichPlatform {
    private LiveDataRepository repository;
    private TramServiceDate tramServiceDate;
    private TramTime queryTime;

    public LiveDataEnricher(LiveDataRepository repository, TramServiceDate tramServiceDate, TramTime queryMinutes) {
        this.repository = repository;
        this.tramServiceDate = tramServiceDate;
        this.queryTime = queryMinutes;
    }

    public void enrich(PlatformDTO platform) {
        repository.enrich(platform, tramServiceDate, queryTime);
    }
}
