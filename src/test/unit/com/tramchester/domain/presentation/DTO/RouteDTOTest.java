package com.tramchester.domain.presentation.DTO;

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RouteDTOTest {

    @Test
    public void shouldUseRouteNameForEquality() {

        List<StationDTO> stations = new LinkedList<>();

        RouteDTO routeA = new RouteDTO("nameA", stations);
        RouteDTO routeB = new RouteDTO("nameB", stations);
        RouteDTO routeC = new RouteDTO("nameA", stations);

        assertTrue(routeA.equals(routeC));
        assertTrue(routeC.equals(routeA));

        assertFalse(routeA.equals(routeB));

    }
}
