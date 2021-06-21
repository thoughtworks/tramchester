package com.tramchester.testSupport.reference;


import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.repository.RouteRepository;

import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.testSupport.TestEnv.StagecoachManchester;
import static com.tramchester.testSupport.TestEnv.WarringtonsOwnBuses;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class BusRoutesForTesting {

    // BUS

    @Deprecated
    public static final Route ALTY_TO_WARRINGTON = new Route(StringIdFor.createId("WBTR05A:I:"), "5A",
            "Alty to Stockport", WarringtonsOwnBuses, Bus);

    @Deprecated
    public static final Agency HIGH_PEAK_BUSES = new Agency(DataSourceID.tfgm, StringIdFor.createId("HGP"),
            "High Peak Buses");

    @Deprecated
    public static final Route AIR_TO_BUXTON = new Route(StringIdFor.createId("HGP:199:I:"), "199",
            "Manchester Airport - Stockport - Buxton Skyline", HIGH_PEAK_BUSES, Bus);


    public static Set<Route> findAltyToWarrington(RouteRepository routeRepository) {
        return getRouteAssertExists(routeRepository, WarringtonsOwnBuses.getId(), "Altrincham - Partington - Thelwall - Warrington");
    }

    public static Set<Route> findAltyToStockport(RouteRepository routeRepository) {
        return getRouteAssertExists(routeRepository, StagecoachManchester.getId(), "Altrincham - Stockport");
    }

    public static Set<Route> findStockportMarpleRomileyCircular(RouteRepository routeRepository) {
        return getRouteAssertExists(routeRepository, StagecoachManchester.getId(), "Stockport - Marple/Romiley Circular");
    }

    private static Set<Route> getRouteAssertExists(RouteRepository routeRepository, IdFor<Agency> agencyId, String longName) {
        Set<Route> result = routeRepository.findRoutesByName(agencyId, longName);
        assertFalse(result.isEmpty());
        return result;
    }
}
