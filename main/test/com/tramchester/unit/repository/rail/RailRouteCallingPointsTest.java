package com.tramchester.unit.repository.rail;

import com.tramchester.dataimport.rail.repository.RailRouteCallingPoints;
import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RailRouteCallingPointsTest {

    private IdFor<Agency> agencyId;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        agencyId = Agency.createId("NT");
    }

    @Test
    void shouldSortBySizeCallingPoints() {
        // shorter first
        RailRouteCallingPoints routeA = formRailRoute(agencyId, ManchesterPiccadilly, Stockport);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, ManchesterPiccadilly, Stockport, StokeOnTrent);

        int result = routeA.compareTo(routeB);
        assertTrue(result>0, Integer.toString(result));
    }

    @Test
    void shouldSortBySizeCallingPointsReversed() {
        RailRouteCallingPoints routeA = formRailRoute(agencyId, ManchesterPiccadilly, Stockport, Altrincham);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, ManchesterPiccadilly, Macclesfield);

        int result = routeA.compareTo(routeB);
        assertTrue(result<0, Integer.toString(result));
    }

    @Test
    void shouldSortBySizeCallingPointsThenNames() {
        RailRouteCallingPoints routeA = formRailRoute(agencyId, ManchesterPiccadilly, Stockport);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, ManchesterPiccadilly, Macclesfield);

        int result = routeA.compareTo(routeB);
        assertTrue(result>0, Integer.toString(result));
    }

    @Test
    void shouldSortBySizeCallingPointsThenNamesReversed() {
        RailRouteCallingPoints routeA = formRailRoute(agencyId, ManchesterPiccadilly, Macclesfield);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, ManchesterPiccadilly, Stockport);

        int result = routeA.compareTo(routeB);
        assertTrue(result<0, Integer.toString(result));
    }

    @Test
    void shouldSortBySizeCallingPointsThenNamesSame() {
        RailRouteCallingPoints routeA = formRailRoute(agencyId, ManchesterPiccadilly, Stockport);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, ManchesterPiccadilly, Stockport);

        int result = routeA.compareTo(routeB);
        assertEquals(0,result, Integer.toString(result));
    }

    @Test
    void shouldHaveExpectedOrderLongestFirst() {
        RailRouteCallingPoints routeA = formRailRoute(agencyId, ManchesterPiccadilly, Stockport, StokeOnTrent);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, ManchesterPiccadilly, Stockport, StokeOnTrent, LondonEuston);
        RailRouteCallingPoints routeC = formRailRoute(agencyId, ManchesterPiccadilly, StokeOnTrent);

        List<RailRouteCallingPoints> list = Arrays.asList(routeA, routeB, routeC);

        List<RailRouteCallingPoints> result = list.stream().sorted(RailRouteCallingPoints::compareTo).collect(Collectors.toList());

        assertEquals(routeB, result.get(0));
        assertEquals(routeA, result.get(1));
        assertEquals(routeC, result.get(2));

    }

    @Test
    void shouldHaveExpectedOrderSameLength() {
        RailRouteCallingPoints routeA = formRailRoute(agencyId, ManchesterPiccadilly, Stockport);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, ManchesterPiccadilly, Macclesfield);
        RailRouteCallingPoints routeC = formRailRoute(agencyId, ManchesterPiccadilly, LondonEuston);

        List<RailRouteCallingPoints> list = Arrays.asList(routeA, routeB, routeC);

        List<RailRouteCallingPoints> result = list.stream().sorted(RailRouteCallingPoints::compareTo).collect(Collectors.toList());

        assertEquals(routeC, result.get(0));
        assertEquals(routeB, result.get(1));
        assertEquals(routeA, result.get(2));

    }

    private RailRouteCallingPoints formRailRoute(IdFor<Agency> agencyId, RailStationIds... stationIds) {
        return new RailRouteCallingPoints(agencyId, idsFor(stationIds));
    }

    private List<IdFor<Station>> idsFor(RailStationIds... stationIds) {
        return Arrays.stream(stationIds).map(RailStationIds::getId).collect(Collectors.toList());
    }
}
