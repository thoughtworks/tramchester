package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.BusStations;
import com.tramchester.testSupport.BusTest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


@Disabled("Experimental")
class BusInterchangeRepositoryTest {
    private static Dependencies dependencies;
    private InterchangeRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationBusTestConfig());
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }
    @BeforeEach
    void onceBeforeEachTestRuns() {
        repository = dependencies.get(InterchangeRepository.class);
    }

    @Test
    void shouldFindTramInterchanges() {
        for (String interchange : TramInterchanges.stations()) {
            assertTrue(repository.isInterchange(interchange));
        }
    }

    @BusTest
    @Test
    void shouldFindBusInterchanges() {

        Collection<Station> interchanges = repository.getBusInterchanges();

        for (Station interchange : interchanges) {
            assertFalse(interchange.isTram());
        }

        assertFalse(interchanges.isEmpty());
        Set<String> interchangeIds = interchanges.stream().map(Station::getId).collect(Collectors.toSet());
        assertTrue(interchangeIds.contains(BusStations.AltrinchamInterchange.getId()));
    }

    @BusTest
    @Test
    void shouldFindSharedStationsUsedByMultipleAgencies() {

        Set<Station> shared = repository.getMultiAgencyStations();

        assertFalse(shared.isEmpty());

        assertTrue(shared.contains(BusStations.AltrinchamInterchange));
        assertTrue(shared.contains(BusStations.StockportBusStation));
        assertTrue(shared.contains(BusStations.ShudehillInterchange));

        StationRepository stationRepos = dependencies.get(StationRepository.class);
        assertNotEquals(stationRepos.getStations().size(), shared.size());
    }

}
