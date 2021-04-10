package com.tramchester.testSupport.reference;


import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import org.jetbrains.annotations.NotNull;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.testSupport.TestEnv.StagecoachManchester;
import static com.tramchester.testSupport.TestEnv.WarringtonsOwnBuses;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BusRoutesForTesting {

    // BUS

    @Deprecated
    public static final Route ALTY_TO_WARRINGTON = new Route(StringIdFor.createId("WBTR05A:I:"), "5A",
            "Alty to Stockport", WarringtonsOwnBuses, Bus);

    @Deprecated
    public static final Agency HIGH_PEAK_BUSES = new Agency(DataSourceID.TFGM(), "HGP", "High Peak Buses");

    @Deprecated
    public static final Route AIR_TO_BUXTON = new Route(StringIdFor.createId("HGP:199:I:"), "199",
            "Manchester Airport - Stockport - Buxton Skyline", HIGH_PEAK_BUSES, Bus);


    public static Route findAltyToStockport(RouteRepository routeRepository) {
        return getRouteAssertExists(routeRepository, StagecoachManchester.getId(), "Altrincham - Stockport");
    }

    public static Route findStockportMarpleRomileyCircular(RouteRepository routeRepository) {
        return getRouteAssertExists(routeRepository, StagecoachManchester.getId(), "Stockport - Marple/Romiley Circular");
    }

    @NotNull
    private static Route getRouteAssertExists(RouteRepository routeRepository, IdFor<Agency> agencyId, String longName) {
        Route result = routeRepository.findFirstRouteByName(agencyId, longName);
        assertNotNull(result);
        return result;
    }
}
