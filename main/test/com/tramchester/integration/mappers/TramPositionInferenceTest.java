package com.tramchester.integration.mappers;

import com.tramchester.Dependencies;
import com.tramchester.LiveDataTestCategory;
import com.tramchester.domain.Station;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.graph.TramRouteReachable;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import com.tramchester.mappers.TramPositionInference;
import com.tramchester.repository.LiveDataRepository;
import com.tramchester.repository.StationAdjacenyRepository;
import com.tramchester.repository.StationRepository;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TramPositionInferenceTest {

    private static Dependencies dependencies;

    private TramPositionInference mapper;
    private StationRepository stationRepository;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
    }

    @Before
    public void onceBeforeEachTestRuns() {
        LiveDataRepository liveDataSource = dependencies.get(LiveDataRepository.class);
        liveDataSource.refreshRespository();
        TramRouteReachable routeReachable = dependencies.get(TramRouteReachable.class);
        StationAdjacenyRepository adjacenyMatrix = dependencies.get(StationAdjacenyRepository.class);
        mapper = new TramPositionInference(liveDataSource, adjacenyMatrix, routeReachable);
        stationRepository = dependencies.get(StationRepository.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    @Category(LiveDataTestCategory.class)
    public void shouldInferTramPosition() {
        // NOTE: costs are not symmetric between two stations, i.e. one direction might cost more than the other
        // Guess this is down to signalling, track, etc.
        int cost = 3; // cost between the stations, no due trams outside this limit should appear

        Station first = stationRepository.getStation(Stations.Deansgate.getId()).get();
        Station second = stationRepository.getStation(Stations.StPetersSquare.getId()).get();

        Set<DueTram> between = mapper.findBetween(first, second);
        assertTrue(between.size()>=1);
        between.forEach(dueTram -> assertFalse(Integer.toString(dueTram.getWait()), (dueTram.getWait())> cost));

        Set<DueTram> otherDirection = mapper.findBetween(second, first);
        assertTrue(otherDirection.size()>=1);
        otherDirection.forEach(dueTram -> assertFalse(Integer.toString(dueTram.getWait()), (dueTram.getWait())> cost));
    }
}
