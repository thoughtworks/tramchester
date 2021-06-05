package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.search.NumberHopsForDestination;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Map;

import static com.tramchester.testSupport.reference.BusStations.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class BusNumberHopsForDestinationTest {

    private static ComponentContainer componentContainer;

    private NumberHopsForDestination numberHops;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationBusTestConfig();
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

    //@Disabled("takes far far too long")
    @Test
    void shouldFindStockportToAllDestinations() {
        Map<IdFor<Station>, Long> hops = numberHops.calculateFor(BusStations.of(StopAtStockportBusStation));
        assertEquals(stationRepository.getNumberOfStations(), hops.size());
        
        assertEquals(0, hops.get(Bury.getId()));
        assertEquals(-1, hops.get(PiccadilyStationStopA.getId()));
        assertEquals(-1, hops.get(KnutsfordStationStand3.getId()));
        assertEquals(-1, hops.get(StopAtAltrinchamInterchange.getId()));

    }


}
