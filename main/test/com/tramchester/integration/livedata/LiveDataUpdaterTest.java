package com.tramchester.integration.livedata;

import com.tramchester.Dependencies;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.livedata.LiveDataUpdater;
import com.tramchester.repository.PlatformMessageRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveDataUpdaterTest {
    private static Dependencies dependencies;

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
        LiveDataUpdater liveDataUpdater = dependencies.get(LiveDataUpdater.class);
        liveDataUpdater.refreshRespository();
    }

    @Test
    void findAtleastOneStationWithNotes() {
        assertNotEquals(messageRepo.numberOfEntries(), 0);

        int numStationsWithMessages = messageRepo.numberStationsWithMessages(TestEnv.LocalNow());

        assertTrue(numStationsWithMessages>1);
    }
}
