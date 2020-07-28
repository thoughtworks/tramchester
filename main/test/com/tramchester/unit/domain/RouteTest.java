package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class RouteTest {

    @Test
    void shouldHaveTramRoute() {
        Route route = new Route(IdFor.createId("idA"),"code","name", TestEnv.MetAgency(), TransportMode.Tram);
        Assertions.assertTrue(TransportMode.isTram(route));

        route = new Route(IdFor.createId("idB"),"code","name", new Agency("GMS", "agencyName"), TransportMode.Bus);
        Assertions.assertFalse(TransportMode.isTram(route));
    }

    @Test
    void shouldAddService() {
        Route  route = new Route(IdFor.createId("routeId"),"code","name", TestEnv.MetAgency(), TransportMode.Tram);

        route.addService(new Service("serviceId", TestEnv.getTestRoute()));
        route.addService(new Service("serviceId", TestEnv.getTestRoute()));
        route.addService(new Service("serviceId2", TestEnv.getTestRoute()));

        Set<Service> services = route.getServices();

        Assertions.assertEquals(2, services.size());
    }

    @Test
    void shouldAddHeadsign() {
        Route  route = new Route(IdFor.createId("id"),"code","name", TestEnv.MetAgency(), TransportMode.Tram);

        route.addHeadsign("hs1");
        route.addHeadsign("hs2");
        route.addHeadsign("hs1");

        Set<String> results = route.getHeadsigns();

        Assertions.assertEquals(2, results.size());
    }
}
