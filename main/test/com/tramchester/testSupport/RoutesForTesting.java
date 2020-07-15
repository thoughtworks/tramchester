package com.tramchester.testSupport;


import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.TransportMode;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;

public class RoutesForTesting {
    // TRAM

    // TODO Lockdown route 1 Gone during lockdown changes
    public static final Route ALTY_TO_BURY = createTramRoute("1", "I", "Alty to Bury");
    public static final Route BURY_TO_ALTY = createTramRoute("1", "O", "Bury to Alty");

    public static final Route ALTY_TO_PICC = createTramRoute("2", "I", "Altrincham - Piccadilly");
    public static final Route PICC_TO_ALTY = createTramRoute("2", "O", "Piccadilly - Altrincham");

    public static final Route ASH_TO_ECCLES = createTramRoute("3", "I", "Ashton-under-Lyne - Manchester - Eccles");
    public static final Route ECCLES_TO_ASH = createTramRoute("3", "O", "Eccles - Manchester - Ashton-under-Lyne");

    // TODO Lockdown this route might be temp?
    public static final Route BURY_TO_PICC = createTramRoute("4", "I", "Picc to Bury");
    public static final Route PICC_TO_BURY = createTramRoute("4", "O", "Bury to Picc");

    public static final Route ROCH_TO_DIDS = createTramRoute("5", "O", "Rochdale - Manchester - E Didsbury");
    public static final Route DIDS_TO_ROCH = createTramRoute("5", "I", "E Didsbury - Manchester - Rochdale");

    public static final Route VIC_TO_AIR = createTramRoute("6", "I", "Victoria - Manchester Airport");
    public static final Route AIR_TO_VIC = createTramRoute("6", "O", "Manchester Airport - Victoria");

    public static final Route CORN_TO_INTU = createTramRoute("7", "O", "Cornbrook - intu Trafford Centre");
    public static final Route INTU_TO_CORN = createTramRoute("7", "I", "intu Trafford Centre - Cornbrook");

    // BUS
    public static final Route ALTY_TO_STOCKPORT = new Route("GMS: 11A:I:", "11A", "Alty to Stockport",
        new Agency("GMS", "agencyName"), TransportMode.Bus);

    @NotNull
    private static Route createTramRoute(String shortName, String direction, String longName) {
        return new Route(format("MET:   %s:%s:", shortName, direction), shortName, longName,
                TestEnv.MetAgency(), TransportMode.Tram);
    }


}
