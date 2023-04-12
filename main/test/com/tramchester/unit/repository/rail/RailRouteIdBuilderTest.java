package com.tramchester.unit.repository.rail;

import com.tramchester.dataimport.rail.RailRouteIDBuilder;
import com.tramchester.dataimport.rail.repository.RailRouteCallingPoints;
import com.tramchester.dataimport.rail.repository.RailRouteIdRepository;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RailRouteIdBuilderTest {

    private RailRouteIDBuilder builder;
    private IdFor<Agency> agencyId;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        builder = new RailRouteIDBuilder();
        agencyId = Agency.createId("VT");
    }

    @Test
    void shouldCreateExpectedIdForSingleRoute() {

        Set<RailRouteCallingPoints> agencyCallingPoints = new HashSet<>();

        RailRouteCallingPoints railRouteA = formRailRoute(agencyId, ManchesterPiccadilly, Stockport, StokeOnTrent, Crewe, LondonEuston);
        agencyCallingPoints.add(railRouteA);

        Set<RailRouteIdRepository.RailRouteCallingPointsWithRouteId> results = builder.getRouteIdsFor(agencyId, agencyCallingPoints);

        assertEquals(1, results.size());

        RailRouteIdRepository.RailRouteCallingPointsWithRouteId result = getFor(results, railRouteA);

        assertEquals(Route.createId("MNCRPIC:EUSTON=>VT:1"), result.getRouteId());
    }

    @Test
    void shouldCreateExpectedIdForThreeRoutesWithSubset() {

        Set<RailRouteCallingPoints> agencyCallingPoints = new HashSet<>();

        // no sub-routes here
        RailRouteCallingPoints routeA = formRailRoute(agencyId, ManchesterPiccadilly, Stockport, LondonEuston);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, ManchesterPiccadilly, Stockport, StokeOnTrent, Crewe, LondonEuston);
        RailRouteCallingPoints routeC = formRailRoute(agencyId, ManchesterPiccadilly, Stockport, MiltonKeynesCentral, LondonEuston);

        agencyCallingPoints.add(routeA);
        agencyCallingPoints.add(routeB);
        agencyCallingPoints.add(routeC);

        Set<RailRouteIdRepository.RailRouteCallingPointsWithRouteId> results = builder.getRouteIdsFor(agencyId, agencyCallingPoints);

        assertEquals(2, results.size());

        RailRouteIdRepository.RailRouteCallingPointsWithRouteId resultB = getFor(results, routeB);
        assertEquals(Route.createId("MNCRPIC:EUSTON=>VT:1"), resultB.getRouteId());

        RailRouteIdRepository.RailRouteCallingPointsWithRouteId resultC = getFor(results, routeC);
        assertEquals(Route.createId("MNCRPIC:EUSTON=>VT:2"), resultC.getRouteId());

    }

    @Test
    void shouldCreateExpectedIdForThreeRoutes() {

        Set<RailRouteCallingPoints> agencyCallingPoints = new HashSet<>();

        // no sub-routes here
        RailRouteCallingPoints routeA = formRailRoute(agencyId, ManchesterPiccadilly, Macclesfield, LondonEuston);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, ManchesterPiccadilly, Stockport, StokeOnTrent, Crewe, LondonEuston);
        RailRouteCallingPoints routeC = formRailRoute(agencyId, ManchesterPiccadilly, Stockport, MiltonKeynesCentral, LondonEuston);

        agencyCallingPoints.add(routeA);
        agencyCallingPoints.add(routeB);
        agencyCallingPoints.add(routeC);

        Set<RailRouteIdRepository.RailRouteCallingPointsWithRouteId> results = builder.getRouteIdsFor(agencyId, agencyCallingPoints);

        assertEquals(3, results.size());

        RailRouteIdRepository.RailRouteCallingPointsWithRouteId resultA = getFor(results, routeA);
        assertEquals(Route.createId("MNCRPIC:EUSTON=>VT:3"), resultA.getRouteId());

        RailRouteIdRepository.RailRouteCallingPointsWithRouteId resultB = getFor(results, routeB);
        assertEquals(Route.createId("MNCRPIC:EUSTON=>VT:1"), resultB.getRouteId());

        RailRouteIdRepository.RailRouteCallingPointsWithRouteId resultC = getFor(results, routeC);
        assertEquals(Route.createId("MNCRPIC:EUSTON=>VT:2"), resultC.getRouteId());

    }

    @Test
    void shouldCreateExpectedIdForThreeRoutesSameSize() {

        Set<RailRouteCallingPoints> agencyCallingPoints = new HashSet<>();

        // no sub-routes here
        RailRouteCallingPoints routeA = formRailRoute(agencyId, ManchesterPiccadilly, Macclesfield, LondonEuston);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, ManchesterPiccadilly, StokeOnTrent, LondonEuston);
        RailRouteCallingPoints routeC = formRailRoute(agencyId, ManchesterPiccadilly, Stockport, LondonEuston);

        agencyCallingPoints.add(routeA);
        agencyCallingPoints.add(routeB);
        agencyCallingPoints.add(routeC);

        Set<RailRouteIdRepository.RailRouteCallingPointsWithRouteId> results = builder.getRouteIdsFor(agencyId, agencyCallingPoints);

        assertEquals(3, results.size());

        RailRouteIdRepository.RailRouteCallingPointsWithRouteId resultA = getFor(results, routeA);
        assertEquals(Route.createId("MNCRPIC:EUSTON=>VT:1"), resultA.getRouteId());

        RailRouteIdRepository.RailRouteCallingPointsWithRouteId resultB = getFor(results, routeB);
        assertEquals(Route.createId("MNCRPIC:EUSTON=>VT:3"), resultB.getRouteId());

        RailRouteIdRepository.RailRouteCallingPointsWithRouteId resultC = getFor(results, routeC);
        assertEquals(Route.createId("MNCRPIC:EUSTON=>VT:2"), resultC.getRouteId());

    }

    private RailRouteIdRepository.RailRouteCallingPointsWithRouteId getFor(Set<RailRouteIdRepository.RailRouteCallingPointsWithRouteId> items, RailRouteCallingPoints railRoute) {
        Optional<RailRouteIdRepository.RailRouteCallingPointsWithRouteId> search = items.stream().filter(item -> item.getCallingPoints().equals(railRoute.getCallingPoints())).findAny();
        assertTrue(search.isPresent(), "Could not find " + railRoute + " in " + items);
        return search.get();
    }

    @NotNull
    private RailRouteCallingPoints formRailRoute(IdFor<Agency> agencyId, RailStationIds... stationIds) {
        return new RailRouteCallingPoints(agencyId, idsFor(stationIds));
    }

    private List<IdFor<Station>> idsFor(RailStationIds... stationIds) {
        return Arrays.stream(stationIds).map(RailStationIds::getId).collect(Collectors.toList());
    }
}
