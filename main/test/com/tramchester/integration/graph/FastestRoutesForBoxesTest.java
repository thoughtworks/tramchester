package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.BoundingBoxWithCost;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.search.FastestRoutesForBoxes;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastestRoutesForBoxesTest {

    private static ComponentContainer componentContainer;
    private FastestRoutesForBoxes calculator;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig config = new IntegrationTramTestConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        calculator = componentContainer.get(FastestRoutesForBoxes.class);
    }

    @Test
    void checkGroupStationsAsExpectedForRealData() {
        StationLocations stationLocations = componentContainer.get(StationLocations.class);
        StationRepository stationsRepo = componentContainer.get(StationRepository.class);

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
            if (group.getStations().contains(station)) {
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
    void shouldFindRoutesForAllExceptStart() {
        TramStations testStationWithInvalidPosition = TramStations.StPetersSquare;

        LatLong latLong = TestEnv.stPetersSquareLocation();
        GridPosition grid = CoordinateTransforms.getGridPosition(latLong);

        IdFor<NaptanArea> areaId = IdFor.invalid();
        Station destination = new MutableStation(testStationWithInvalidPosition.getId(), areaId,
                testStationWithInvalidPosition.getName(), latLong, grid, DataSourceID.tfgm);

        TramTime time = TramTime.of(9,15);
        JourneyRequest journeyRequest = new JourneyRequest(
                new TramServiceDate(TestEnv.testDay()), time, false, 2, 120, 3);

        Stream<BoundingBoxWithCost> results = calculator.findForGrid(destination, 2000, journeyRequest);

        List<BoundingBoxWithCost> found = results.filter(result -> result.getMinutes() <= 0).collect(Collectors.toList());

        assertEquals(1, found.size());
        assertTrue( found.get(0).contained(destination.getGridPosition()));
    }
}
