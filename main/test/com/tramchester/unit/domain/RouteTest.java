package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class RouteTest {

    @Test
    void shouldHaveTramRoute() {
        Route route = MutableRoute.getRoute(StringIdFor.createId("idA"),"code","name", TestEnv.MetAgency(),
                TransportMode.Tram);
        Assertions.assertTrue(TransportMode.isTram(route));

        route = MutableRoute.getRoute(StringIdFor.createId("idB"),"code","name",
                new Agency(DataSourceID.tfgm, StringIdFor.createId("GMS"), "agencyName"),
                TransportMode.Bus);
        Assertions.assertFalse(TransportMode.isTram(route));
    }

    @Test
    void shouldAddService() {
        MutableRoute route = new MutableRoute(StringIdFor.createId("routeId"),"code","name", TestEnv.MetAgency(), TransportMode.Tram);

        route.addService(new MutableService("serviceId"));
        route.addService(new MutableService("serviceId"));
        route.addService(new MutableService("serviceId2"));

        Set<Service> services = route.getServices();

        Assertions.assertEquals(2, services.size());
    }

}
