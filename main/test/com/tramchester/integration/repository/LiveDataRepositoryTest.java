package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.livedata.LiveDataHTTPFetcher;
import com.tramchester.mappers.LiveDataParser;
import com.tramchester.repository.LiveDataRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertFalse;

class LiveDataRepositoryTest {
    private static Dependencies dependencies;

    private LiveDataHTTPFetcher fetcher;
    private LiveDataParser parser;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
        // don't want to fetch every time
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        parser = dependencies.get(LiveDataParser.class);
        fetcher = dependencies.get(LiveDataHTTPFetcher.class);
    }

    @Test
    void spikeOnDuplicatePlatformDisplayHandling() {
        LiveDataRepository repository = new LiveDataRepository(fetcher, parser, new ProvidesLocalNow());
        repository.refreshRespository();

        //Collection<StationDepartureInfo> departs = repository.allDepartures();
        //repository.countEntries();
        //Set<String> lineNames = departs.stream().map(depart -> depart.getLineName()).collect(Collectors.toSet());

        //assertFalse(lineNames.isEmpty());
    }
}
