package com.tramchester.unit.domain;

import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.TransportMode;
import com.tramchester.testSupport.TestEnv;
import org.junit.Test;

import java.util.Set;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class RouteTest {

    @Test
    public void shouldHaveTramRoute() {
        Route route = new Route("id","code","name", TestEnv.MetAgency(), TransportMode.Tram);
        assertTrue(route.isTram());

        route = new Route("id","code","name", new Agency("GMS"), TransportMode.Bus);
        assertFalse(route.isTram());
    }

    @Test
    public void shouldAddService() {
        Route  route = new Route("id","code","name", TestEnv.MetAgency(), TransportMode.Tram);

        route.addService(new Service("serviceId", TestEnv.getTestRoute()));
        route.addService(new Service("serviceId", TestEnv.getTestRoute()));
        route.addService(new Service("serviceId2", TestEnv.getTestRoute()));

        Set<Service> services = route.getServices();

        assertEquals(2, services.size());
    }

    @Test
    public void shouldAddHeadsign() {
        Route  route = new Route("id","code","name", TestEnv.MetAgency(), TransportMode.Tram);

        route.addHeadsign("hs1");
        route.addHeadsign("hs2");
        route.addHeadsign("hs1");

        Set<String> results = route.getHeadsigns();

        assertEquals(2, results.size());
    }
}
