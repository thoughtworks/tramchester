package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.TransportMode;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

class RouteDTOTest {

    @Test
    void shouldUseRouteNameForEquality() {

        List<StationRefWithPosition> stations = new LinkedList<>();

        RouteDTO routeA = new RouteDTO("nameA", "A", stations, "displayClass", TransportMode.Tram);
        RouteDTO routeB = new RouteDTO("nameB", "B", stations, "displayClass", TransportMode.Tram);
        RouteDTO routeC = new RouteDTO("nameA", "C", stations, "displayClass", TransportMode.Tram);

        Assertions.assertEquals(routeA, routeC);
        Assertions.assertEquals(routeC, routeA);
        Assertions.assertNotEquals(routeA, routeB);

    }
}
