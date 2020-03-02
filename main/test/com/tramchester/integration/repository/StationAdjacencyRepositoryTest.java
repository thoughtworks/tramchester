package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.Station;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.Stations;
import com.tramchester.repository.StationAdjacenyRepository;
import com.tramchester.repository.TransportDataSource;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class StationAdjacencyRepositoryTest {
    private static Dependencies dependencies;
    private static IntegrationTramTestConfig testConfig;

    private StationAdjacenyRepository repository;
    private TransportDataSource transportDataSource;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }


    @Before
    public void onceBeforeEachTestRuns() {
        transportDataSource = dependencies.get(TransportDataSource.class);
        repository = new StationAdjacenyRepository(transportDataSource);
    }

    @Test
    public void shouldGiveCorrectCostForAdjaceny() {
        assertEquals(3, getAdjacent(Stations.Altrincham, Stations.NavigationRoad));
        assertEquals(3, getAdjacent(Stations.Altrincham, Stations.NavigationRoad));
        assertEquals(3, getAdjacent(Stations.Cornbrook, Stations.Deansgate));
        assertEquals(3, getAdjacent(Stations.Deansgate, Stations.Cornbrook));
        assertEquals(-1, getAdjacent(Stations.NavigationRoad, Stations.Cornbrook));
    }

    @Test
    public void shouldHavePairsOfStations() {
        Set<Pair<Station, Station>> pairs = repository.getStationParis();
        String stationId = Stations.NavigationRoad.getId();

        List<Pair<Station, Station>> results = pairs.stream().
                filter(pair -> pair.getLeft().getId().equals(stationId) ||
                        pair.getRight().getId().equals(stationId)).
                collect(Collectors.toList());
        assertEquals(pairs.toString(), 4, results.size());
    }

    private int getAdjacent(Station first, Station second) {
        return repository.getAdjacent(transportDataSource.getStation(first.getId()),
                transportDataSource.getStation(second.getId()));
    }
}
