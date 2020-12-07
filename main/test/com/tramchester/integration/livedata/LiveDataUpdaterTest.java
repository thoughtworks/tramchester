package com.tramchester.integration.livedata;

import com.tramchester.Dependencies;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.livedata.LiveDataUpdater;
import com.tramchester.repository.PlatformMessageRepository;
import com.tramchester.testSupport.LiveDataMessagesCategory;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class LiveDataUpdaterTest {
    private static Dependencies dependencies;

    private PlatformMessageRepository messageRepo;

    public static final TramStations StationWithNotes = TramStations.StPetersSquare;

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
        LiveDataUpdater liveDataUpdater = dependencies.get(LiveDataUpdater.class);
        liveDataUpdater.refreshRespository();
    }

    @Test
    @LiveDataMessagesCategory
    void findAtleastOneStationWithNotes() {
        assertNotEquals(messageRepo.numberOfEntries(), 0);

        int numStationsWithMessages = messageRepo.numberStationsWithMessages(TestEnv.LocalNow());

        assertTrue(numStationsWithMessages>1);
    }

    @Test
    @LiveDataMessagesCategory
    void shouldHaveMessagesForTestStation() {
        Set<Station> stations = messageRepo.getStationsWithMessages(TestEnv.LocalNow());

        assertTrue(stations.contains(TramStations.of(StationWithNotes)), stations.toString());
    }
}
