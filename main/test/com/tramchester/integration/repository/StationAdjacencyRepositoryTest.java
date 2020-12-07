package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.TramStationAdjacenyRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TramStations;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class StationAdjacencyRepositoryTest {
    private static Dependencies dependencies;

    private TramStationAdjacenyRepository repository;
    private TransportData transportDataSource;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        transportDataSource = dependencies.get(TransportData.class);
        repository = dependencies.get(TramStationAdjacenyRepository.class);
    }

    @Test
    void shouldGiveCorrectCostForAdjaceny() {
        Assertions.assertEquals(3, getAdjacent(TramStations.Altrincham, TramStations.NavigationRoad));
        Assertions.assertEquals(3, getAdjacent(TramStations.Altrincham, TramStations.NavigationRoad));
        Assertions.assertEquals(3, getAdjacent(TramStations.Cornbrook, TramStations.Deansgate));
        Assertions.assertEquals(3, getAdjacent(TramStations.Deansgate, TramStations.Cornbrook));

        Assertions.assertEquals(-1, getAdjacent(TramStations.NavigationRoad, TramStations.Cornbrook));
    }

    @Test
    void shouldHavePairsOfStations() {
        Set<Pair<Station, Station>> pairs = repository.getTramStationParis();
        IdFor<Station> stationId = TramStations.NavigationRoad.getId();

        List<Pair<Station, Station>> results = pairs.stream().
                filter(pair -> pair.getLeft().getId().equals(stationId) ||
                        pair.getRight().getId().equals(stationId)).
                collect(Collectors.toList());
        Assertions.assertEquals(4, results.size(), pairs.toString());
    }

    private int getAdjacent(TramStations first, TramStations second) {
        return repository.getAdjacent(transportDataSource.getStationById(first.getId()),
                transportDataSource.getStationById(second.getId()));
    }
}
