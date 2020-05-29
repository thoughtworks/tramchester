package com.tramchester.unit.resource;


import com.tramchester.resources.RouteCodeToClassMapper;
import com.tramchester.testSupport.RoutesForTesting;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RouteCodeToClassMapperTest {

    @Test
    public void testShouldMapToCorrectClassNewRouteStyle() {
        RouteCodeToClassMapper mapper = new RouteCodeToClassMapper();

        assertEquals("RouteClass1", mapper.map(RoutesForTesting.ALTY_TO_BURY));
        assertEquals("RouteClass1", mapper.map(RoutesForTesting.BURY_TO_ALTY));
        assertEquals("RouteClass2", mapper.map(RoutesForTesting.ALTY_TO_PICC));
        assertEquals("RouteClass2", mapper.map(RoutesForTesting.PICC_TO_ALTY));
        assertEquals("RouteClass3", mapper.map(RoutesForTesting.ASH_TO_ECCLES));
        assertEquals("RouteClass3", mapper.map(RoutesForTesting.ECCLES_TO_ASH));
        assertEquals("RouteClass4", mapper.map(RoutesForTesting.BURY_TO_PICC));
        assertEquals("RouteClass4", mapper.map(RoutesForTesting.BURY_TO_PICC));
        assertEquals("RouteClass5", mapper.map(RoutesForTesting.DIDS_TO_ROCH));
        assertEquals("RouteClass5", mapper.map(RoutesForTesting.ROCH_TO_DIDS));
        assertEquals("RouteClass6", mapper.map(RoutesForTesting.AIR_TO_VIC));
        assertEquals("RouteClass6", mapper.map(RoutesForTesting.VIC_TO_AIR));
        assertEquals("RouteClass7", mapper.map(RoutesForTesting.CORN_TO_INTU));
        assertEquals("RouteClass7", mapper.map(RoutesForTesting.INTU_TO_CORN));

        assertEquals("RouteClassBus", mapper.map(RoutesForTesting.ALTY_TO_STOCKPORT));
    }
}
