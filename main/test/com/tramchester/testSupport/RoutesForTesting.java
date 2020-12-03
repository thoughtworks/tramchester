package com.tramchester.testSupport;


import com.tramchester.domain.Agency;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.reference.RouteDirection;
import com.tramchester.domain.reference.TransportMode;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;

public class RoutesForTesting {
    // TRAM

    // TODO Lockdown route 1 Gone during lockdown changes
    @Deprecated
    public static final Route ALTY_TO_BURY = createTramRoute("1", RouteDirection.Inbound, "Alty to Bury");
    @Deprecated
    public static final Route BURY_TO_ALTY = createTramRoute("1", RouteDirection.Outbound, "Bury to Alty");

    public static final Route ALTY_TO_PICC = createTramRoute("2", RouteDirection.Inbound, "Altrincham - Piccadilly");
    public static final Route PICC_TO_ALTY = createTramRoute("2", RouteDirection.Outbound, "Piccadilly - Altrincham");

    public static final Route ASH_TO_ECCLES = createTramRoute("3", RouteDirection.Inbound, "Ashton-under-Lyne - Manchester - Eccles");
    public static final Route ECCLES_TO_ASH = createTramRoute("3", RouteDirection.Outbound, "Eccles - Manchester - Ashton-under-Lyne");

    // TODO Lockdown this route might be temp?
    public static final Route BURY_TO_PICC = createTramRoute("4", RouteDirection.Inbound, "Picc to Bury");
    public static final Route PICC_TO_BURY = createTramRoute("4", RouteDirection.Outbound, "Bury to Picc");

    public static final Route ROCH_TO_DIDS = createTramRoute("5", RouteDirection.Outbound, "Rochdale - Manchester - E Didsbury");
    public static final Route DIDS_TO_ROCH = createTramRoute("5", RouteDirection.Inbound, "E Didsbury - Manchester - Rochdale");

    public static final Route VIC_TO_AIR = createTramRoute("6", RouteDirection.Inbound, "Victoria - Manchester Airport");
    public static final Route AIR_TO_VIC = createTramRoute("6", RouteDirection.Outbound, "Manchester Airport - Victoria");

    public static final Route CORN_TO_INTU = createTramRoute("7", RouteDirection.Outbound, "Cornbrook - intu Trafford Centre");
    public static final Route INTU_TO_CORN = createTramRoute("7", RouteDirection.Inbound, "intu Trafford Centre - Cornbrook");

    // BUS
    public static final Route ALTY_TO_STOCKPORT = new Route(IdFor.createId("GMS: 11A:I:"), "11A", "Alty to Stockport",
        new Agency("GMS", "agencyName"), TransportMode.Bus, RouteDirection.Inbound);
    public static final Route ALTY_TO_STOCKPORT_WBT = new Route(IdFor.createId("WBT:5A:I:"), "5A", "Alty to Stockport",
            new Agency("WBT", "Warrington's Own Buses"), TransportMode.Bus, RouteDirection.Inbound);

    @NotNull
    private static Route createTramRoute(String shortName, RouteDirection direction, String longName) {
        String idText = format("MET:%s%s", shortName, direction.getSuffix());
        return new Route(IdFor.createId(idText), shortName, longName,
                TestEnv.MetAgency(), TransportMode.Tram, direction);
    }


}
