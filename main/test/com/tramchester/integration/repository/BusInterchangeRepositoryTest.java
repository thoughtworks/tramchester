package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.IntegrationBusTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusInterchangeRepositoryTest {
    private static ComponentContainer componentContainer;
    private InterchangeRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationBusTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        repository = componentContainer.get(InterchangeRepository.class);
    }

    @Test
    void shouldFindTramInterchanges() {
        for (IdFor<Station> interchange : TramInterchanges.stations()) {
            assertTrue(repository.isInterchange(interchange));
        }
    }

    @BusTest
    @Test
    void shouldFindBusInterchanges() {

        Collection<Station> interchanges = repository.getBusInterchanges();

        for (Station interchange : interchanges) {
            assertFalse(TransportMode.isTram(interchange));
        }

        assertFalse(interchanges.isEmpty());
        IdSet<Station> interchangeIds = interchanges.stream().collect(IdSet.collector());
        assertTrue(interchangeIds.contains(BusStations.AltrinchamInterchange.getId()));
    }

//    @BusTest
//    @Test
//    void shouldFindSharedStationsUsedByMultipleAgencies() {
//
//        Set<Station> shared = repository.getBusMultiAgencyStations();
//
//        assertFalse(shared.isEmpty());
//
//        assertTrue(shared.contains(BusStations.AltrinchamInterchange));
//        assertTrue(shared.contains(BusStations.StockportBusStation));
//        assertTrue(shared.contains(BusStations.ShudehillInterchange));
//
//        StationRepository stationRepos = dependencies.get(StationRepository.class);
//        assertNotEquals(stationRepos.getStations().size(), shared.size());
//    }

}
