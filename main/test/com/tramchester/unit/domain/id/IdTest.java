package com.tramchester.unit.domain.id;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.CompositeId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.MixedCompositeId;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class IdTest {

    private final IdFor<Station> idA = StringIdFor.createId("1234");
    private final IdFor<Station> idAA = StringIdFor.createId("1234");
    private final IdFor<Station> idB = StringIdFor.createId("0BCD");
    private final IdFor<Station> idC = StringIdFor.createId("5678");

    @Test
    void shouldHaveEquality() {

        assertEquals(idA, idA);
        assertEquals(idA, idAA);
        assertEquals(idAA, idA);

        assertNotEquals(idA, idC);
        assertNotEquals(idC, idA);
    }

    @Test
    void shouldHaveCompositeIdEquality() {

        CompositeId<Station> compositeIdA = new CompositeId<>(idA, idB);
        CompositeId<Station> compositeIdB = new CompositeId<>(idB, idA);
        CompositeId<Station> compositeIdC = new CompositeId<>(idA, idC);

        assertEquals(compositeIdA, compositeIdA);
        assertEquals(compositeIdA, compositeIdB);
        assertEquals(compositeIdB, compositeIdA);

        assertNotEquals(compositeIdA, compositeIdC);
        assertNotEquals(compositeIdC, compositeIdA);

        assertEquals("0BCD_1234", compositeIdA.forDTO());
        assertEquals("0BCD_1234", compositeIdA.getGraphId());

        assertEquals(compositeIdA.forDTO(), compositeIdB.forDTO());
        assertEquals(compositeIdA.getGraphId(), compositeIdB.getGraphId());
    }

    @Test
    void shouldHaveMixedCompositeEquality() {
        IdFor<Route> routeA = StringIdFor.createId("routeA");
        IdFor<Route> routeB = StringIdFor.createId("routeB");

        IdFor<RouteStation> compositeIdA = MixedCompositeId.createId(routeA, idA);
        IdFor<RouteStation> compositeIdB = MixedCompositeId.createId(routeA, idA);
        IdFor<RouteStation> compositeIdC = MixedCompositeId.createId(routeB, idA);

        assertEquals(compositeIdA, compositeIdA);
        assertEquals(compositeIdA, compositeIdB);
        assertEquals(compositeIdB, compositeIdA);

        assertNotEquals(compositeIdA, compositeIdC);
        assertNotEquals(compositeIdC, compositeIdA);
    }

    @Test
    void shouldRoundTripParseComposite() {
        CompositeId<Station> compositeIdA = new CompositeId<>(idA, idB, idC);

        String forDto = compositeIdA.forDTO();
        CompositeId<Station> result = CompositeId.parse(forDto);

        assertEquals(compositeIdA, result);
    }

    @Test
    void shouldRoundTripParseMixedComposite() {
        IdFor<Route> routeA = StringIdFor.createId("routeA");
        IdFor<RouteStation> compositeIdA = MixedCompositeId.createId(routeA, idA);

        String forDto = compositeIdA.forDTO();
        IdFor<RouteStation> result = MixedCompositeId.parse(forDto);

        assertEquals(compositeIdA, result);
    }

}
