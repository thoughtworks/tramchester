package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.TramAndTrainGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.Collections;

import static com.tramchester.integration.testSupport.rail.RailStationIds.ManchesterPiccadilly;
import static com.tramchester.integration.testSupport.rail.RailStationIds.Stockport;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TrainTest
public class RailAndTramRouteToRouteCostsTest {
    private static StationRepository stationRepository;


    private static ComponentContainer componentContainer;

    private LocalDate date;
    private RouteToRouteCosts routeToRouteCosts;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig testConfig = new TramAndTrainGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        stationRepository = componentContainer.get(StationRepository.class);
    }

    @AfterEach
    void afterAllEachTestsHasRun() {

    }

    @AfterAll
    static void afterAllTestsRun() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        date = TestEnv.testDay();
        routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);
    }

    @Test
    void shouldValidHopsBetweenTramAndRailLongRange() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8, 15), TramTime.of(22, 35));

        NumberOfChanges result = routeToRouteCosts.getNumberOfChanges(tram(TramStations.Bury), rail(Stockport),
                Collections.emptySet(), date, timeRange);

        assertEquals(0, result.getMin());
        assertEquals(2, result.getMax());
    }

    @Test
    void shouldValidHopsBetweenTramAndRailNeighbours() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8, 15), TramTime.of(22, 35));

        NumberOfChanges result = routeToRouteCosts.getNumberOfChanges(tram(Altrincham), rail(RailStationIds.Altrincham),
                Collections.emptySet(), date, timeRange);

        assertEquals(1, result.getMin());
        assertEquals(1, result.getMax());
    }

    @Test
    void shouldValidHopsBetweenTramAndRailShortRange() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8, 15), TramTime.of(22, 35));

        NumberOfChanges result = routeToRouteCosts.getNumberOfChanges(tram(Cornbrook), rail(RailStationIds.Altrincham),
                Collections.emptySet(), date, timeRange);

        assertEquals(1, result.getMin());
        assertEquals(1, result.getMax());
    }

    @Test
    void shouldNotHaveHopsBetweenTramAndRailWhenTramOnly() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8, 15), TramTime.of(22, 35));

        NumberOfChanges result = routeToRouteCosts.getNumberOfChanges(tram(TramStations.Bury), rail(Stockport),
                Collections.emptySet(), date, timeRange);

        assertEquals(Integer.MAX_VALUE, result.getMin());
        assertEquals(Integer.MAX_VALUE, result.getMax());
    }

    @Test
    void shouldHaveCorrectHopsBetweenRailStationsOnly() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8, 15), TramTime.of(22, 35));

        NumberOfChanges result = routeToRouteCosts.getNumberOfChanges(rail(ManchesterPiccadilly), rail(Stockport),
                Collections.emptySet(), date, timeRange);

        assertEquals(0, result.getMin());
        assertEquals(2, result.getMax());
    }

    @Test
    void shouldHaveCorrectHopsBetweenTramStationsOnly() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8, 15), TramTime.of(22, 35));

        NumberOfChanges result = routeToRouteCosts.getNumberOfChanges(tram(Cornbrook), tram(StPetersSquare),
                Collections.emptySet(), date, timeRange);

        assertEquals(0, result.getMin());
        assertEquals(2, result.getMax());
    }

    private Station tram(TramStations tramStation) {
        return tramStation.from(stationRepository);
    }

    private Station rail(RailStationIds railStation) {
        return railStation.from(stationRepository);
    }
}
