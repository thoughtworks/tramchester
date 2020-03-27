package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.livedata.LiveDataHTTPFetcher;
import com.tramchester.mappers.LiveDataParser;
import com.tramchester.repository.LiveDataRepository;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;

public class LiveDataRepositoryTest {
    private static Dependencies dependencies;

    private LiveDataHTTPFetcher fetcher;
    private LiveDataParser parser;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
        // don't want to fetch every time
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void beforeEachTestRuns() {
        parser = dependencies.get(LiveDataParser.class);
        fetcher = dependencies.get(LiveDataHTTPFetcher.class);
    }

    @Test
    public void spikeOnDuplicatePlatformDisplayHandling() {
        LiveDataRepository repository = new LiveDataRepository(fetcher, parser, new ProvidesLocalNow());
        repository.refreshRespository();

        //Collection<StationDepartureInfo> departs = repository.allDepartures();
        //repository.countEntries();
        //Set<String> lineNames = departs.stream().map(depart -> depart.getLineName()).collect(Collectors.toSet());

        //assertFalse(lineNames.isEmpty());
    }
}
