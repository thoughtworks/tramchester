package com.tramchester.livedata;

import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.repository.LiveDataSource;

// TODO refactoring has rendered this empty, remove?
public class LiveDataEnricher implements EnrichPlatform {
    private LiveDataSource repository;

    public LiveDataEnricher(LiveDataSource repository) {
        this.repository = repository;
    }

    public void enrich(PlatformDTO platform, TramServiceDate tramServiceDate, TramTime queryTime) {
        repository.enrich(platform, tramServiceDate, queryTime);
    }
}
