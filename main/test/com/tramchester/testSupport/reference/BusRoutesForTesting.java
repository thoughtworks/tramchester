package com.tramchester.testSupport.reference;


import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.StringIdFor;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.testSupport.TestEnv.StagecoachManchester;
import static com.tramchester.testSupport.TestEnv.WarringtonsOwnBuses;

public class BusRoutesForTesting {

    // BUS

    public static final Route ALTY_TO_WARRINGTON = new Route(StringIdFor.createId("WBTR05A:I:"), "5A",
            "Alty to Stockport", WarringtonsOwnBuses, Bus);

    public static final Agency HIGH_PEAK_BUSES = new Agency(DataSourceID.TFGM(), "HGP", "High Peak Buses");

    public static final Route AIR_TO_BUXTON = new Route(StringIdFor.createId("HGP:199:I:"), "199",
            "Manchester Airport - Stockport - Buxton Skyline", HIGH_PEAK_BUSES, Bus);

    public static final Route StockportMarpleRomileyCircular = new Route(StringIdFor.createId("SCMN383:O:"), "383",
            "Stockport - Marple/Romiley Circular", StagecoachManchester, Bus);

    public static final Route ALTY_TO_STOCKPORT = new Route(StringIdFor.createId("SCMN11A:I:"), "11A",
            "Altrincham - Stockport", StagecoachManchester, Bus);

}
