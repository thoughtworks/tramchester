package com.tramchester.unit.domain.presentation.DTO;

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

        RouteDTO routeA = new RouteDTO("nameA", "A", stations, "displayClass", true);
        RouteDTO routeB = new RouteDTO("nameB", "B", stations, "displayClass", true);
        RouteDTO routeC = new RouteDTO("nameA", "C", stations, "displayClass", true);

        Assertions.assertEquals(routeA, routeC);
        Assertions.assertEquals(routeC, routeA);
        Assertions.assertNotEquals(routeA, routeB);

    }
}
