package com.tramchester.livedata;

import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.repository.LiveDataRepository;

public class LiveDataEnricher implements EnrichPlatform {
    private LiveDataRepository repository;
    private TramServiceDate tramServiceDate;
    private int queryMins;

    public LiveDataEnricher(LiveDataRepository repository, TramServiceDate tramServiceDate, int queryMins) {
        this.repository = repository;
        this.tramServiceDate = tramServiceDate;
        this.queryMins = queryMins;
    }

    public void enrich(PlatformDTO platform) {
        repository.enrich(platform, tramServiceDate, queryMins);
    }
}
