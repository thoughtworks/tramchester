package com.tramchester.testSupport;


import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.TransportMode;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

public class RoutesForTesting {
    // TRAM
    public static final Route ALTY_TO_BURY = createTramRoute("1", "I", "Alty to Bury");
    public static final Route BURY_TO_ALTY = createTramRoute("1", "O", "Bury to Alty");

    public static final Route ALTY_TO_PICC = createTramRoute("2", "I", "Alty to Picc");
    public static final Route PICC_TO_ALTY = createTramRoute("2", "O", "Picc to Alty");


    public static final Route ASH_TO_ECCLES = createTramRoute("3", "I", "Ash to Eccles");
    public static final Route ECCLES_TO_ASH = createTramRoute("3", "O", "Eccles to Ash");

    public static final Route ROCH_TO_DIDS = createTramRoute("5", "O", "Roch to Dids");
    public static final Route DIDS_TO_ROCH = createTramRoute("5", "I", "Dids to Ruch");

    public static final Route VIC_TO_AIR = createTramRoute("6", "I", "Vic To Air");
    public static final Route AIR_TO_VIC = createTramRoute("6", "O", "Vic To Air");

    public static final Route CORN_TO_INTU = createTramRoute("7", "O", "Corn to Intu");
    public static final Route INTU_TO_CORN = createTramRoute("7", "I", "Intu to Corn");

    // BUS
    public static final Route ALTY_TO_STOCKPORT = new Route("GMS: 11A:I:", "11A", "Alty to Stockport",
        new Agency("GMS"), TransportMode.Bus);

    @NotNull
    private static Route createTramRoute(String shortName, String direction, String longName) {
        return new Route(format("MET:   %s:%s:", shortName, direction), shortName, longName,
                TestEnv.MetAgency(), TransportMode.Tram);
    }


}
