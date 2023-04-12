package com.tramchester.unit.domain.id;

import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import org.junit.jupiter.api.Test;

import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class RailRouteIdTest {

    @Test
    void shouldHaveEqualityForTwoRailRouteIds() {
        RailRouteId idA  = new RailRouteId(LondonEuston.getId(), StokeOnTrent.getId(), Agency.createId("NT"), 1);
        RailRouteId idB = new RailRouteId(LondonEuston.getId(), StokeOnTrent.getId(), Agency.createId("NT"), 1);

        assertEquals(idA,idA);
        assertEquals(idB,idA);
        assertEquals(idA,idB);
    }

    @Test
    void shouldHaveInEquality() {
        RailRouteId idA  = new RailRouteId(LondonEuston.getId(), StokeOnTrent.getId(), Agency.createId("NT"), 1);

        RailRouteId idB = new RailRouteId(ManchesterPiccadilly.getId(), StokeOnTrent.getId(), Agency.createId("NT"), 1);
        RailRouteId idC  = new RailRouteId(LondonEuston.getId(), StokeOnTrent.getId(), Agency.createId("XX"), 1);
        RailRouteId idD  = new RailRouteId(LondonEuston.getId(), ManchesterPiccadilly.getId(), Agency.createId("NT"), 1);
        RailRouteId idE  = new RailRouteId(LondonEuston.getId(), StokeOnTrent.getId(), Agency.createId("NT"), 2);

        assertNotEquals(idA, idB);
        assertNotEquals(idA, idC);
        assertNotEquals(idA, idD);
        assertNotEquals(idA, idE);

    }

    @Test
    void shouldHaveEqualityAndSameHashWithValidStringRouteId() {
        IdFor<Route> idA  = new RailRouteId(LondonEuston.getId(), StokeOnTrent.getId(), Agency.createId("NT"), 1);

        IdFor<Route> idB = Route.createId("EUSTON:STOKEOT=>NT:1");

        assertEquals(idA, idB);
        assertEquals(idB, idA);

        // need same hash otherwise issue with collections with mixed sources
        assertEquals(idA.hashCode(), idB.hashCode());
    }

    @Test
    void shouldHaveInEqualityWithValidStringRouteId() {
        IdFor<Route> idA  = new RailRouteId(LondonEuston.getId(), StokeOnTrent.getId(), Agency.createId("NT"), 1);

        IdFor<Route> idB = Route.createId("EUSTON:STOKEOT=>NT:X");

        assertNotEquals(idA, idB);
        assertNotEquals(idB, idA);
    }
}
