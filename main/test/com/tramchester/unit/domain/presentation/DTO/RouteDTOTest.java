package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.TransportMode;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.Stations;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteDTOTest {

    @Test
    void shouldUseRouteNameForEquality() {

        List<StationRefWithPosition> stations = new ArrayList<>();
        stations.add(new StationRefWithPosition(Stations.Intu));
        RouteDTO routeDTO = new RouteDTO(RoutesForTesting.INTU_TO_CORN, stations);

        assertEquals("MET:   7:I:", routeDTO.getId());
        assertEquals("intu Trafford Centre - Cornbrook", routeDTO.getRouteName());
        assertEquals("7", routeDTO.getShortName());
        assertEquals(TransportMode.Tram, routeDTO.getTransportMode());

        List<StationRefWithPosition> stationsDTO = routeDTO.getStations();
        assertEquals(1, stationsDTO.size());
        assertEquals(Stations.Intu.forDTO(), stations.get(0).getId());

    }
}
