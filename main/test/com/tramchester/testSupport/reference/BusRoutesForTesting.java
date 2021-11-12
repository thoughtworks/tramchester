package com.tramchester.testSupport.reference;


import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutableRoute;
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

    // TODO Copy the KnownTramRoute pattern

    // BUS

    @Deprecated
    public static final MutableRoute ALTY_TO_WARRINGTON = new MutableRoute(StringIdFor.createId("WBTR05A:I:"), "5A",
            "Alty to Stockport", WarringtonsOwnBuses, Bus);

    @Deprecated
    public static final Agency HIGH_PEAK_BUSES = new Agency(DataSourceID.tfgm, StringIdFor.createId("HGP"),
            "High Peak Buses");

    @Deprecated
    public static final MutableRoute AIR_TO_BUXTON = new MutableRoute(StringIdFor.createId("HGP:199:I:"), "199",
            "Manchester Airport - Stockport - Buxton Skyline", HIGH_PEAK_BUSES, Bus);


}
