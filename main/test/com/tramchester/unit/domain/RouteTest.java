package com.tramchester.unit.domain;

import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.TransportMode;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class RouteTest {

    @Test
    void shouldHaveTramRoute() {
        Route route = new Route("id","code","name", TestEnv.MetAgency(), TransportMode.Tram);
        Assertions.assertTrue(route.isTram());

        route = new Route("id","code","name", new Agency("GMS", "agencyName"), TransportMode.Bus);
        Assertions.assertFalse(route.isTram());
    }

    @Test
    void shouldAddService() {
        Route  route = new Route("id","code","name", TestEnv.MetAgency(), TransportMode.Tram);

        route.addService(new Service("serviceId", TestEnv.getTestRoute()));
        route.addService(new Service("serviceId", TestEnv.getTestRoute()));
        route.addService(new Service("serviceId2", TestEnv.getTestRoute()));

        Set<Service> services = route.getServices();

        Assertions.assertEquals(2, services.size());
    }

    @Test
    void shouldAddHeadsign() {
        Route  route = new Route("id","code","name", TestEnv.MetAgency(), TransportMode.Tram);

        route.addHeadsign("hs1");
        route.addHeadsign("hs2");
        route.addHeadsign("hs1");

        Set<String> results = route.getHeadsigns();

        Assertions.assertEquals(2, results.size());
    }
}
