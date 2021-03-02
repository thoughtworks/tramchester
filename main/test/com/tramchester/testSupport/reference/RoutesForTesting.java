package com.tramchester.testSupport.reference;


import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.reference.KnownTramRoute;
import com.tramchester.domain.reference.RouteDirection;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;

import static com.tramchester.domain.reference.RouteDirection.*;
import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.testSupport.TestEnv.StagecoachManchester;
import static com.tramchester.testSupport.TestEnv.WarringtonsOwnBuses;
import static java.lang.String.format;

public class RoutesForTesting {
    // TRAM

    // TODO Lockdown route 1 Gone during lockdown changes
    @Deprecated
    public static final Route ALTY_TO_BURY = createTramRoute("1", Inbound, "Alty to Bury");
    @Deprecated
    public static final Route BURY_TO_ALTY = createTramRoute("1", Outbound, "Bury to Alty");

    // BUS

    public static final Route ALTY_TO_STOCKPORT_WBT = new Route(StringIdFor.createId("WBT:5A:I:"), "5A",
            "Alty to Stockport", WarringtonsOwnBuses,
            Bus, Inbound);

    public static final Route AIR_TO_BUXTON = new Route(StringIdFor.createId("HGP:199:I:"), "199",
            "Manchester Airport - Stockport - Buxton Skyline",  new Agency(DataSourceID.TFGM(), "HGP", "High Peak Buses"),
            Bus, Circular);

    public static final Route StockportMarpleRomileyCircular = new Route(StringIdFor.createId("GMS:383:C:"), "383",
            "Stockport - Marple - Romiley Circular", StagecoachManchester,
            Bus, Circular);

    public static final Route ALTY_TO_STOCKPORT = new Route(StringIdFor.createId("GMS:11A:I:"), "11A",
            "Altrincham - Sharston - Cheadle - Stockport", new Agency(DataSourceID.TFGM(), "GMS", "agencyName"),
            Bus, Inbound);

    public static Route createTramRoute(KnownTramRoute knownRoute) {
        return new Route(knownRoute.getId(), knownRoute.number(), knownRoute.name(), TestEnv.MetAgency(),
                knownRoute.mode(), knownRoute.direction());
    }

    @Deprecated
    private static Route createTramRoute(String shortName, RouteDirection direction, String longName) {
        String idText = format("MET:%s%s", shortName, direction.getSuffix());
        return new Route(StringIdFor.createId(idText), shortName, longName,
                TestEnv.MetAgency(), TransportMode.Tram, direction);
    }


}
