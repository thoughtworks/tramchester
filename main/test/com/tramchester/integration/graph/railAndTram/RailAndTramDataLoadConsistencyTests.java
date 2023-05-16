package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.rail.repository.RailRouteIds;
import com.tramchester.dataimport.rail.repository.RailStationRecordsRepository;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.search.routes.RouteCostMatrix;
import com.tramchester.graph.search.routes.RouteIndex;
import com.tramchester.integration.testSupport.RailAndTramGreaterManchesterConfig;
import com.tramchester.repository.NumberOfRoutes;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.dataimport.rail.reference.TrainOperatingCompanies.TP;
import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.TestEnv.assertSetEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


@Disabled("Case caught and repdudced in the RailRouteTest class")
@GMTest
public class RailAndTramDataLoadConsistencyTests {

    private static RailAndTramGreaterManchesterConfig config;
    private GuiceContainerDependencies componentContainer;

    private final List<IdFor<Station>> idsA = asStationIds(Arrays.asList("MNCRIAP", "MNCRPIC", "MNCROXR", "MNCRVIC", "HDRSFLD",
            "DWBY", "LEEDS", "GARFRTH",
            "YORK", "THIRSK", "NLRTN", "YAAM", "TABY", "MDLSBRO", "REDCARC", "SBRN"));

    private final List<IdFor<Station>> idsB = asStationIds(Arrays.asList("MNCRIAP","GATLEY","MNCRPIC","MNCROXR","MNCRVIC","HDRSFLD",
            "DWBY","LEEDS",
            "YORK","THIRSK","NLRTN","YAAM","TABY","MDLSBRO","REDCARC","SBRN"));

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        config = new RailAndTramGreaterManchesterConfig();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        componentContainer = null;
    }

    @AfterEach
    void onceAfterEachTest() {
        if (componentContainer!=null) {
            TestEnv.clearDataCache(componentContainer);
            componentContainer.close();
            componentContainer = null;
        }
    }

    @Test
    void shouldHaveDateOverlapsConsistently() {
        GuiceContainerDependencies componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        //TestEnv.clearDataCache(componentContainer);

        RouteCostMatrix.RouteDateAndDayOverlap overlapsA = getOverlapsFor(componentContainer);
        overlapsA.populateFor();
        final int previousOverlaps = overlapsA.numberBitsSet();
        final TransportDataContainer transportDataContainer = (TransportDataContainer) componentContainer.get(TransportData.class);
        final TransportData previousTransportData = TransportDataContainer.createUnmanagedCopy(transportDataContainer);
        final Set<RouteStation> previousRouteStations = previousTransportData.getRouteStations();

        for (int i = 0; i < 10; i++) {
            //TestEnv.clearDataCache(componentContainer);

            componentContainer.close();

            componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
            componentContainer.initialise();

            final TransportData transportData = componentContainer.get(TransportData.class);

            assertEquals(previousTransportData.getSourceName(), transportData.getSourceName());
            assertSetEquals(previousTransportData.getRoutes(), transportData.getRoutes());
            assertSetEquals(previousTransportData.getServices(), transportData.getServices());
            assertSetEquals(previousTransportData.getStations(), transportData.getStations());
            assertSetEquals(previousTransportData.getAgencies(), transportData.getAgencies());
            assertSetEquals(previousTransportData.getPlaformStream().collect(Collectors.toSet()),
                    transportData.getPlaformStream().collect(Collectors.toSet()));
            assertSetEquals(previousTransportData.getTrips(), transportData.getTrips());

            Set<RouteStation> routeStations = transportData.getRouteStations();

            assertSetEquals(byMode(previousRouteStations, Tram), byMode(routeStations, Tram));
            assertSetEquals(byMode(previousRouteStations, Train), byMode(routeStations, Train));

            assertSetEquals(previousRouteStations, routeStations);

            RouteCostMatrix.RouteDateAndDayOverlap overlapsB = getOverlapsFor(componentContainer);
            overlapsB.populateFor();
            final int result = overlapsB.numberBitsSet();

            assertEquals(previousOverlaps, result);
        }

    }

    @NotNull
    private Set<RouteStation> byMode(Set<RouteStation> previousRouteStations, TransportMode mode) {
        return previousRouteStations.stream().
                filter(routeStation -> routeStation.getTransportModes().contains(mode)).
                collect(Collectors.toSet());
    }

    @Test
    void shouldHaveRouteIdsConsistently() {
        GuiceContainerDependencies componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        final IdFor<Agency> agencyId = TP.getAgencyId();

        for (int i = 0; i < 10; i++) {
            TestEnv.clearDataCache(componentContainer);
            componentContainer.close();

            componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
            componentContainer.initialise();

            final RailRouteIds railRouteIdRepository = componentContainer.get(RailRouteIds.class);
            final RailStationRecordsRepository stationRepository = componentContainer.get(RailStationRecordsRepository.class);

            RailRouteId idB = railRouteIdRepository.getRouteIdFor(agencyId, getStatonsFor(idsB, stationRepository));
            RailRouteId idA = railRouteIdRepository.getRouteIdFor(agencyId, getStatonsFor(idsA, stationRepository));

            assertNotEquals(idA, idB);
            assertEquals(Route.createId("MNCRIAP:SBRN=>TP:2"), idA);
            assertEquals(Route.createId("MNCRIAP:SBRN=>TP:1"), idB);
        }
    }

    private List<Station> getStatonsFor(List<IdFor<Station>> stationIds, RailStationRecordsRepository stationRepository) {
        return stationIds.stream().map(stationRepository::getMutableStationForTiploc).collect(Collectors.toList());
    }



    private List<IdFor<Station>> asStationIds(List<String> asList) {
        return asList.stream().map(Station::createId).collect(Collectors.toList());
    }

    @NotNull
    private RouteCostMatrix.RouteDateAndDayOverlap getOverlapsFor(GuiceContainerDependencies componentContainer) {
        RouteIndex routeIndex = componentContainer.get(RouteIndex.class);
        NumberOfRoutes numberOfRoutes = componentContainer.get(NumberOfRoutes.class);
        return new RouteCostMatrix.RouteDateAndDayOverlap(routeIndex, numberOfRoutes.numberOfRoutes());
    }

}
