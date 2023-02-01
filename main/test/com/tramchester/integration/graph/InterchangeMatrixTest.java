package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.routes.InterchangeMatrix;
import com.tramchester.integration.testSupport.ConfigParameterResolver;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.testTags.DualTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.EnumSet;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
public class InterchangeMatrixTest {
    private static ComponentContainer componentContainer;

    private InterchangeMatrix matrix;
    private TramDate date;
    private Set<TransportMode> modes;
    private TimeRange timeRange;
    private TramRouteHelper tramRouteHelper;
    private RouteRepository routeRepository;

    // NOTE: this test does not cause a full db rebuild, so might see VERSION node missing messages

    @BeforeAll
    static void onceBeforeAnyTestRuns(TramchesterConfig tramchesterConfig) {

        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        TestEnv.clearDataCache(componentContainer);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        matrix = componentContainer.get(InterchangeMatrix.class);
        routeRepository = componentContainer.get(RouteRepository.class);

        date = TestEnv.testDay();
        modes = EnumSet.of(Tram);
        timeRange = TimeRange.of(TramTime.of(8,0), TramTime.of(12,0));

        tramRouteHelper = new TramRouteHelper(routeRepository);
    }

    @Test
    void shouldHaveDirectlyLinked() {
        assertEquals(0, matrix.getDegree(StPetersSquare.getId(), Cornbrook.getId(), date, timeRange, modes));
        assertEquals(0, matrix.getDegree(Cornbrook.getId(), StPetersSquare.getId(), date, timeRange, modes));
    }

    @Test
    void shouldHaveIndirectlyLinkedStWerburghsRoadPomona() {
        assertEquals(1, matrix.getDegree(StWerburghsRoad.getId(), Pomona.getId(), date, timeRange, modes));
        assertEquals(1, matrix.getDegree(Pomona.getId(), StWerburghsRoad.getId(), date, timeRange, modes));
    }

    @Test
    void shouldHaveIndirectlyLinkedStWerburghsRoadPiccadilly() {
        assertEquals(1, matrix.getDegree(StWerburghsRoad.getId(), Piccadilly.getId(), date, timeRange, modes));
        assertEquals(1, matrix.getDegree(Piccadilly.getId(), StWerburghsRoad.getId(), date, timeRange, modes));
    }

    @Disabled("WIP")
    @Test
    void shouldGetInterchangesForRoutesAtDepth() {
        Route altyToPicc = tramRouteHelper.getOneRoute(KnownTramRoute.AltrinchamPiccadilly, date);
        Route cornbrookToTraffordCentre = tramRouteHelper.getOneRoute(KnownTramRoute.CornbrookTheTraffordCentre, date);

        //Set<InterchangeStation> results = matrix.getInterchangesFor(RoutePair.of(altyToPicc, cornbrookToTraffordCentre), 1, date, timeRange, modes);
    }
}
