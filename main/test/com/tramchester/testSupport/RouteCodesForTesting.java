package com.tramchester.testSupport;


import java.util.Arrays;
import java.util.List;

public class RouteCodesForTesting {
    // TRAM
    public static final String ALTY_TO_BURY = "MET:   1:I:";
    public static final String BURY_TO_ALTY = "MET:   1:O:";

    public static final String ALTY_TO_PICC = "MET:   2:I:";

    public static final String ASH_TO_ECCLES = "MET:   3:I:";
    public static final String ECCLES_TO_ASH = "MET:   3:O:";

    public static final String ROCH_TO_DIDS = "MET:   5:O:";
    public static final String DIDS_TO_ROCH = "MET:   5:I:";

    public static final String VIC_TO_MAN_AIR = "MET:   6:I:";

    public static final String CORN_TO_INTU = "MET:   7:O:";
    public static final String INTU_TO_CORN = "MET:   7:I:";

    // BUS
    public static final String ALTY_TO_STOCKPORT = "GMS: 11A:I:";

    public static final List<String> RouteSeven = Arrays.asList(CORN_TO_INTU, INTU_TO_CORN);
}
