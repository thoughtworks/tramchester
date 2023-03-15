package com.tramchester.unit.domain.id;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class IdTest {

    private final IdFor<Station> idA = Station.createId("1234");
    private final IdFor<Station> idAA = Station.createId("1234");
    private final IdFor<Station> idB = Station.createId("0BCD");
    private final IdFor<Station> idC = Station.createId("5678");

    @Test
    void shouldHaveEquality() {

        assertEquals(idA, idA);
        assertEquals(idA, idAA);
        assertEquals(idAA, idA);

        assertNotEquals(idA, idC);
        assertNotEquals(idC, idA);
    }

    @Test
    void shouldNotBeEqualsIfDifferentDomains() {
        IdFor<Station> stationId = Station.createId("test123");
        IdFor<Route> routeId = Route.createId("test123");

        assertNotEquals(stationId, routeId);
    }


}
