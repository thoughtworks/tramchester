package com.tramchester.testSupport.reference;


import com.tramchester.domain.MutableAgency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.id.StringIdFor;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.testSupport.TestEnv.WarringtonsOwnBuses;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class BusRoutesForTesting {

    // TODO Copy the KnownTramRoute pattern

    // BUS

    @Deprecated
    public static final MutableRoute ALTY_TO_WARRINGTON = new MutableRoute(StringIdFor.createId("WBTR05A:I:"), "5A",
            "Alty to Stockport", WarringtonsOwnBuses, Bus);

    @Deprecated
    public static final MutableAgency HIGH_PEAK_BUSES = new MutableAgency(DataSourceID.tfgm, StringIdFor.createId("HGP"),
            "High Peak Buses");

    @Deprecated
    public static final MutableRoute AIR_TO_BUXTON = new MutableRoute(StringIdFor.createId("HGP:199:I:"), "199",
            "Manchester Airport - Stockport - Buxton Skyline", HIGH_PEAK_BUSES, Bus);


}
