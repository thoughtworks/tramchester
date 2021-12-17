package com.tramchester.integration.graph.rail;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteInterchanges;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TrainTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class RailInterchangeTest {
    private static GuiceContainerDependencies componentContainer;
    private InterchangeRepository interchangeRepository;
    private StationRepository stationRepository;
    private RouteInterchanges routeInterchanges;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig testConfig = new IntegrationRailTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        interchangeRepository = componentContainer.get(InterchangeRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
        routeInterchanges = componentContainer.get(RouteInterchanges.class);
    }

    @Test
    void shouldHaveExpectedInterchange() {
        Station manPicc = stationRepository.getStationById(RailStationIds.ManchesterPiccadilly.getId());
        assertTrue(interchangeRepository.isInterchange(manPicc));

        Station euston = stationRepository.getStationById(RailStationIds.LondonEuston.getId());
        assertTrue(interchangeRepository.isInterchange(euston));
    }

    @Test
    void shouldNotHaveInterchangeIfRoutesAreGrouped() {
        Station hale = stationRepository.getStationById(RailStationIds.Hale.getId());
        assertFalse(interchangeRepository.isInterchange(hale));
    }
    
    @Test
    void shouldHaveConsistencyOnZeroCostToInterchangeAndInterchanges() {
        Set<RouteStation> zeroCostToInterchange = stationRepository.getRouteStations().stream().
                filter(routeStation -> routeInterchanges.costToInterchange(routeStation) == 0).
                collect(Collectors.toSet());

        Set<RouteStation> zeroCostButNotInterchange = zeroCostToInterchange.stream().
                filter(zeroCost -> !interchangeRepository.isInterchange(zeroCost.getStation())).
                collect(Collectors.toSet());

        assertTrue(zeroCostButNotInterchange.isEmpty(), zeroCostButNotInterchange.toString());
    }



}
