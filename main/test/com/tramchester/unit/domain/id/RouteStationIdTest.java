package com.tramchester.unit.domain.id;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class RouteStationIdTest {

    private final IdFor<Station> stationId = StringIdFor.createId("1234");
    private final IdFor<Route> routeA = StringIdFor.createId("routeA");

    @Test
    void shouldHaveMixedCompositeEquality() {
        IdFor<Route> routeB = StringIdFor.createId("routeB");

        IdFor<RouteStation> compositeIdA = RouteStationId.createId(routeA, stationId);
        IdFor<RouteStation> compositeIdB = RouteStationId.createId(routeA, stationId);
        IdFor<RouteStation> compositeIdC = RouteStationId.createId(routeB, stationId);

        assertEquals(compositeIdA, compositeIdA);
        assertEquals(compositeIdA, compositeIdB);
        assertEquals(compositeIdB, compositeIdA);

        assertNotEquals(compositeIdA, compositeIdC);
        assertNotEquals(compositeIdC, compositeIdA);
    }

    @Test
    void shouldOutputGraphIdAsExpected() {

        IdFor<RouteStation> compositeIdA = RouteStationId.createId(routeA, stationId);

        assertEquals("routeA_1234", compositeIdA.getGraphId());
    }

    @Test
    void shouldOutputParseGraphIdAsExpected() {

        IdFor<RouteStation> expected = RouteStationId.createId(routeA, stationId);

        IdFor<RouteStation> id = RouteStationId.parse("routeA_1234");
        assertEquals(id, expected);
    }

    @Test
    void shouldRoundTripParseMixedComposite() {
        IdFor<RouteStation> compositeIdA = RouteStationId.createId(routeA, stationId);

        String forDto = compositeIdA.getGraphId();
        IdFor<RouteStation> result = RouteStationId.parse(forDto);

        assertEquals(compositeIdA, result);
    }


}
