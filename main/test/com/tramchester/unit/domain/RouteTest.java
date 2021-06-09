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
        Route route = new Route(StringIdFor.createId("idA"),"code","name", TestEnv.MetAgency(),
                TransportMode.Tram);
        Assertions.assertTrue(TransportMode.isTram(route));

        route = new Route(StringIdFor.createId("idB"),"code","name",
                new Agency(DataSourceID.TFGM(), StringIdFor.createId("GMS"), "agencyName"),
                TransportMode.Bus);
        Assertions.assertFalse(TransportMode.isTram(route));
    }

    @Test
    void shouldAddService() {
        Route  route = new Route(StringIdFor.createId("routeId"),"code","name", TestEnv.MetAgency(), TransportMode.Tram);

        route.addService(new Service("serviceId"));
        route.addService(new Service("serviceId"));
        route.addService(new Service("serviceId2"));

        Set<Service> services = route.getServices();

        Assertions.assertEquals(2, services.size());
    }

}
