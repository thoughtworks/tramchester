package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationLink;
import com.tramchester.graph.search.FindStationLinks;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static org.junit.jupiter.api.Assertions.*;

class FindStationLinksTestBus {

    private static ComponentContainer componentContainer;
    private FindStationLinks findStationLinks;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachOfTheTestsRun() {
        findStationLinks = componentContainer.get(FindStationLinks.class);
    }

    // TODO
//    @Test
//    void shouldFindExpectedLinksBetweenStations() {
//
//    }

    @Test
    void shouldHaveCorrectTransportMode() {
        Set<StationLink> forBus = findStationLinks.findLinkedFor(Bus);
        long notBus = forBus.stream().filter(link -> !link.getModes().contains(Bus)).count();
        assertEquals(0, notBus);

        long isBus = forBus.stream().filter(link -> link.getModes().contains(Bus)).count();
        assertEquals(forBus.size(), isBus);
    }

}
