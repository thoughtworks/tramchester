package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.rail.repository.RailRouteIdRepository;
import com.tramchester.dataimport.rail.repository.RailStationRecordsRepository;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.search.routes.RouteCostMatrix;
import com.tramchester.graph.search.routes.RouteIndex;
import com.tramchester.integration.testSupport.RailAndTramGreaterManchesterConfig;
import com.tramchester.repository.NumberOfRoutes;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import org.apache.commons.collections4.SetUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.dataimport.rail.reference.TrainOperatingCompanies.TP;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
public class RailAndTramRouteCostMatrixConsisencyTest {

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

        TestEnv.clearDataCache(componentContainer);

        RouteCostMatrix.RouteDateAndDayOverlap overlapsA = getOverlapsFor(componentContainer);
        overlapsA.populateFor();
        final int previousOverlaps = overlapsA.numberBitsSet();
        final TransportDataContainer transportDataContainer = (TransportDataContainer) componentContainer.get(TransportData.class);
        final TransportData previousTransportData = TransportDataContainer.createUnmanagedCopy(transportDataContainer);

        for (int i = 0; i < 10; i++) {
            TestEnv.clearDataCache(componentContainer);
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

            assertSetEquals(previousTransportData.getRouteStations(), transportData.getRouteStations());

            RouteCostMatrix.RouteDateAndDayOverlap overlapsB = getOverlapsFor(componentContainer);
            overlapsB.populateFor();
            final int result = overlapsB.numberBitsSet();

            assertEquals(previousOverlaps, result);
        }

    }

    @Test
    void shouldHaveRouteIdsConsistently() {
        GuiceContainerDependencies componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        for (int i = 0; i < 10; i++) {
            TestEnv.clearDataCache(componentContainer);
            componentContainer.close();

            componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
            componentContainer.initialise();

            final RailRouteIdRepository railRouteIdRepository = componentContainer.get(RailRouteIdRepository.class);
            final RailStationRecordsRepository stationRepository = componentContainer.get(RailStationRecordsRepository.class);

            IdFor<Agency> agencyId = TP.getAgencyId();
            RailRouteId idA = railRouteIdRepository.getRouteIdFor(agencyId, getStatonsFor(idsA, stationRepository));
            RailRouteId idB = railRouteIdRepository.getRouteIdFor(agencyId, getStatonsFor(idsB, stationRepository));

            assertNotEquals(idA, idB);
            assertEquals(Route.createId("MNCRIAP:SBRN=>TP:2"), idA);
            assertEquals(Route.createId("MNCRIAP:SBRN=>TP:1"), idB);
        }
    }

    private List<Station> getStatonsFor(List<IdFor<Station>> stationIds, RailStationRecordsRepository stationRepository) {
        return stationIds.stream().map(stationRepository::getMutableStationForTiploc).collect(Collectors.toList());
    }

    private <X> void assertSetEquals(Set<X> itemsA, Set<X> itemsB) {
        SetUtils.SetView<X> difference = SetUtils.disjunction(itemsA, itemsB);
        Set<X> inBnotA = new HashSet<>(itemsA);
        inBnotA.removeAll(itemsB);
        Set<X> inAnotB = new HashSet<>(itemsB);
        inAnotB.removeAll(itemsA);
        String message = "Different A:" + itemsA.size() + " B:" + itemsB.size() + " " + difference +  " in A but not B " +
                inAnotB + " in B but not A " + inBnotA;
        assertTrue(difference.isEmpty(), message);
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
