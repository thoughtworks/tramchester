package com.tramchester.integration.mappers.serialisation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.id.StringIdFor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.tramchester.integration.testSupport.rail.RailStationIds.LondonEuston;
import static com.tramchester.integration.testSupport.rail.RailStationIds.StokeOnTrent;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RouteIndexDataSerialisationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void onceBeforeEachTest() {
        mapper = new ObjectMapper();
    }

    @Test
    void shouldRoundTripRouteId() throws JsonProcessingException {
        IdFor<Route> routeId = Route.createId("routeB");

        RouteIndexData routeIndexData = new RouteIndexData(42, routeId);

        String asString = mapper.writeValueAsString(routeIndexData);

        RouteIndexData result = mapper.readValue(asString, RouteIndexData.class);

        assertEquals(42, result.getIndex());
        assertEquals(routeId, result.getRouteId());
    }

    @Disabled("WIP on serilisaiton handling for IDs")
    @Test
    void shouldRoundTripWithRailRouteId() throws JsonProcessingException {
        IdFor<Agency> agencyId = StringIdFor.createId("NT");
        RailRouteId railRouteId = new RailRouteId(LondonEuston.getId(), StokeOnTrent.getId(), agencyId, 1);

        RouteIndexData routeIndexData = new RouteIndexData(56, railRouteId);

        String asString = mapper.writeValueAsString(routeIndexData);

        RouteIndexData result = mapper.readValue(asString, RouteIndexData.class);

        assertEquals(56, result.getIndex());
        assertEquals(railRouteId, result.getRouteId());

    }


}
