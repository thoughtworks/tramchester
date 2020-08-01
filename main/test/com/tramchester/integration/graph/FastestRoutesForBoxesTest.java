package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.domain.BoundingBoxWithCost;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.search.FastestRoutesForBoxes;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.operation.TransformException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastestRoutesForBoxesTest {

    private static Dependencies dependencies;
    private FastestRoutesForBoxes calculator;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        IntegrationTramTestConfig config = new IntegrationTramTestConfig();
        dependencies.initialise(config);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        calculator = dependencies.get(FastestRoutesForBoxes.class);
    }

    @Test
    void checkGroupStationsAsExpectedForRealData() {
        StationLocations stationLocations = dependencies.get(StationLocations.class);
        StationRepository stationsRepo = dependencies.get(StationRepository.class);

        List<BoundingBoxWithStations> grouped = stationLocations.getGroupedStations(2000).collect(Collectors.toList());
        List<BoundingBoxWithStations> emptyGroup = grouped.stream().filter(group -> !group.hasStations()).collect(Collectors.toList());
        assertEquals(Collections.emptyList(),emptyGroup);

        List<String> notInAGroupById =stationsRepo.getStations().stream().
                filter(station -> !isPresentIn(grouped, station)).
                map(Station::getName).
                collect(Collectors.toList());
        assertEquals(Collections.emptyList(), notInAGroupById);

        List<String> notInAGroupByPosition = stationsRepo.getStations().stream().
                filter(station -> !isPresentInByPos(grouped, station)).
                map(Station::getName).
                collect(Collectors.toList());
        assertEquals(Collections.emptyList(), notInAGroupByPosition);
    }

    private boolean isPresentIn(List<BoundingBoxWithStations> grouped, Station station) {
        for (BoundingBoxWithStations group:grouped) {
            if (group.getStaions().contains(station)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPresentInByPos(List<BoundingBoxWithStations> grouped, Station station) {
        for (BoundingBoxWithStations group:grouped) {
            if (group.contained(station.getGridPosition())) {
                return true;
            }
        }
        return false;
    }

    @Test
    void shouldFindRoutesForAllExceptStart() throws TransformException {
        Station testStationWithInvalidPosition = Stations.StPetersSquare;

        LatLong latLong = TestEnv.stPetersSquareLocation();
        GridPosition grid = CoordinateTransforms.getGridPosition(latLong);
        Station destination = new Station(testStationWithInvalidPosition.getId(), testStationWithInvalidPosition.getArea(),
                testStationWithInvalidPosition.getName(), latLong, grid);
        TramTime time = TramTime.of(9,15);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.testDay()), time, false, 3, 120);
        long numberToFind = 3;

        Stream<BoundingBoxWithCost> results = calculator.findForGrid(destination, 2000, journeyRequest, numberToFind);

        List<BoundingBoxWithCost> noRoute = results.filter(result -> result.getMinutes() <= 0).collect(Collectors.toList());

        assertEquals(1, noRoute.size());
        assertTrue( noRoute.get(0).contained(destination.getGridPosition()));
    }
}
