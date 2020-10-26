package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.livedata.LiveDataHTTPFetcher;
import com.tramchester.mappers.LiveDataParser;
import com.tramchester.repository.LiveDataRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class LiveDataRepositoryTest {
    private static Dependencies dependencies;

    private LiveDataRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
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
        LiveDataParser parser = dependencies.get(LiveDataParser.class);
        LiveDataHTTPFetcher fetcher = dependencies.get(LiveDataHTTPFetcher.class);
        repository = new LiveDataRepository(fetcher, parser, new ProvidesLocalNow());
        repository.refreshRespository();
    }

    @Test
    void findAtleastOneStationWithNotes() {
        assertNotEquals(repository.countEntriesWithMessages(), 0);

        Set<StationDepartureInfo> haveMessages = repository.getEntriesWithMessages().collect(Collectors.toSet());
        assertFalse(haveMessages.isEmpty());

        Set<Station> stations = haveMessages.stream().map(StationDepartureInfo::getStation).collect(Collectors.toSet());
        assertTrue(stations.size()>1);
    }

    @Test
    void spikeOnDuplicatePlatformDisplayHandling() {


        //Collection<StationDepartureInfo> departs = repository.allDepartures();
        //repository.countEntries();
        //Set<String> lineNames = departs.stream().map(depart -> depart.getLineName()).collect(Collectors.toSet());

        //assertFalse(lineNames.isEmpty());
    }
}
