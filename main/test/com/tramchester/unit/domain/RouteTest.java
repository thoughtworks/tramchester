package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.tramchester.domain.id.StringIdFor.createId;

class RouteTest {

    @Test
    void shouldHaveTramRoute() {
        Route route = MutableRoute.getRoute(createId("idA"),"code","name", TestEnv.MetAgency(),
                TransportMode.Tram);
        Assertions.assertTrue(TransportMode.isTram(route));

        route = MutableRoute.getRoute(createId("idB"),"code","name",
                new MutableAgency(DataSourceID.tfgm, createId("GMS"), "agencyName"),
                TransportMode.Bus);
        Assertions.assertFalse(TransportMode.isTram(route));
    }

    @Test
    void shouldAddService() {
        MutableRoute route = new MutableRoute(createId("routeId"),"code","name", TestEnv.MetAgency(), TransportMode.Tram);

        route.addService(MutableService.build(createId("serviceId")));
        route.addService(MutableService.build(createId("serviceId")));
        route.addService(MutableService.build(createId("serviceId2")));

        Set<Service> services = route.getServices();

        Assertions.assertEquals(2, services.size());
    }

}
