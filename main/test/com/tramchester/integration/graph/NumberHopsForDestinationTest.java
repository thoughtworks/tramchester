package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.search.NumberHopsForDestination;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NumberHopsForDestinationTest {

    private static ComponentContainer componentContainer;

    private NumberHopsForDestination numberHops;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        numberHops = componentContainer.get(NumberHopsForDestination.class);
        stationRepository = componentContainer.get(StationRepository.class);
    }

    @Test
    void shouldComputeNumberOfHopsToDestation() {
        Map<IdFor<Station>, Long> hops = numberHops.calculateFor(TramStations.of(Bury));
        assertEquals(stationRepository.getNumberOfStations(), hops.size());

        assertEquals(0, hops.get(Bury.getId()));
        assertEquals(23, hops.get(Altrincham.getId()));
        assertEquals(33, hops.get(ManAirport.getId()));
    }
}
