package com.tramchester.integration.mappers;

import com.tramchester.Dependencies;
import com.tramchester.domain.Station;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.graph.TramRouteReachable;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import com.tramchester.mappers.TramPositionInference;
import com.tramchester.repository.LiveDataRepository;
import com.tramchester.repository.LiveDataSource;
import com.tramchester.repository.StationRepository;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
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
        mapper = new TramPositionInference(liveDataSource, routeReachable);
        stationRepository = dependencies.get(StationRepository.class);
    }

    @Test
    public void shouldInferTramPosition() {
        Station first = stationRepository.getStation(Stations.Altrincham.getId()).get();
        Station second = stationRepository.getStation(Stations.NavigationRoad.getId()).get();

        Set<DueTram> between = mapper.findBetween(first, second);

        assertTrue(between.size()>=1);
    }
}
