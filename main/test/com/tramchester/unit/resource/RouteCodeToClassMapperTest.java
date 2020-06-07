package com.tramchester.unit.resource;


import com.tramchester.resources.RouteCodeToClassMapper;
import com.tramchester.testSupport.RoutesForTesting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RouteCodeToClassMapperTest {

    @Test
    void testShouldMapToCorrectClassNewRouteStyle() {
        RouteCodeToClassMapper mapper = new RouteCodeToClassMapper();

        Assertions.assertEquals("RouteClass1", mapper.map(RoutesForTesting.ALTY_TO_BURY));
        Assertions.assertEquals("RouteClass1", mapper.map(RoutesForTesting.BURY_TO_ALTY));
        Assertions.assertEquals("RouteClass2", mapper.map(RoutesForTesting.ALTY_TO_PICC));
        Assertions.assertEquals("RouteClass2", mapper.map(RoutesForTesting.PICC_TO_ALTY));
        Assertions.assertEquals("RouteClass3", mapper.map(RoutesForTesting.ASH_TO_ECCLES));
        Assertions.assertEquals("RouteClass3", mapper.map(RoutesForTesting.ECCLES_TO_ASH));
        Assertions.assertEquals("RouteClass4", mapper.map(RoutesForTesting.BURY_TO_PICC));
        Assertions.assertEquals("RouteClass4", mapper.map(RoutesForTesting.BURY_TO_PICC));
        Assertions.assertEquals("RouteClass5", mapper.map(RoutesForTesting.DIDS_TO_ROCH));
        Assertions.assertEquals("RouteClass5", mapper.map(RoutesForTesting.ROCH_TO_DIDS));
        Assertions.assertEquals("RouteClass6", mapper.map(RoutesForTesting.AIR_TO_VIC));
        Assertions.assertEquals("RouteClass6", mapper.map(RoutesForTesting.VIC_TO_AIR));
        Assertions.assertEquals("RouteClass7", mapper.map(RoutesForTesting.CORN_TO_INTU));
        Assertions.assertEquals("RouteClass7", mapper.map(RoutesForTesting.INTU_TO_CORN));

        Assertions.assertEquals("RouteClassBus", mapper.map(RoutesForTesting.ALTY_TO_STOCKPORT));
    }
}
