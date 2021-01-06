package com.tramchester.integration.livedata;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
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
    private static ComponentContainer componentContainer;

    private PlatformMessageRepository messageRepo;

    public static final TramStations StationWithNotes = TramStations.ShawAndCrompton;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder<>().create(new IntegrationTramTestConfig(true), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        // don't want to fetch every time
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        messageRepo = componentContainer.get(PlatformMessageRepository.class);
        LiveDataUpdater liveDataUpdater = componentContainer.get(LiveDataUpdater.class);
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
