package com.tramchester.integration.livedata;

import com.tramchester.Dependencies;
import com.tramchester.domain.liveUpdates.PlatformMessage;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.livedata.LiveDataUpdater;
import com.tramchester.repository.PlatformMessageRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class LiveDataUpdaterTest {
    private static Dependencies dependencies;

    private LiveDataUpdater liveDataUpdater;
    private PlatformMessageRepository messageRepo;

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
        messageRepo = dependencies.get(PlatformMessageRepository.class);
        liveDataUpdater = dependencies.get(LiveDataUpdater.class);
        liveDataUpdater.refreshRespository();
    }

    @Test
    void findAtleastOneStationWithNotes() {
        assertNotEquals(liveDataUpdater.countEntriesWithMessages(), 0);

        Set<PlatformMessage> haveMessages = messageRepo.getEntriesWithMessages().collect(Collectors.toSet());
        assertFalse(haveMessages.isEmpty());

        Set<Station> stations = haveMessages.stream().map(PlatformMessage::getStation).collect(Collectors.toSet());
        assertTrue(stations.size()>1);
    }

    // TODO add missing tests

    @Test
    void spikeOnDuplicatePlatformDisplayHandling() {


        //Collection<StationDepartureInfo> departs = repository.allDepartures();
        //repository.countEntries();
        //Set<String> lineNames = departs.stream().map(depart -> depart.getLineName()).collect(Collectors.toSet());

        //assertFalse(lineNames.isEmpty());
    }
}
