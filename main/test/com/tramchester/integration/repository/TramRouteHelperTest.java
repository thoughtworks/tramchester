package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.Route;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.TransportDataFactory;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TramRouteHelperTest {

    private static GuiceContainerDependencies<TransportDataFactory> componentContainer;
    private TramRouteHelper helper;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder<>().create(new IntegrationTramTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        helper = new TramRouteHelper(componentContainer);
    }

    @Test
    void shouldFindAllKnownRoutes() {
        KnownTramRoute[] knownRoutes = KnownTramRoute.values();
        for(KnownTramRoute knownRoute : knownRoutes) {
            Route found = helper.get(knownRoute);
            assertEquals(TestEnv.MetAgency(), found.getAgency());
            assertEquals(knownRoute.shortName(), found.getShortName());
            assertTrue(found.getId().forDTO().contains(knownRoute.direction().getSuffix()));
        }
    }
}
