package com.tramchester.domain;

import org.apache.commons.collections4.ListUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Interchanges {

    public static final String CORNBROOK = "9400ZZMACRN";
    public static final String ST_PETERS_SQUARE = "9400ZZMASTP";
    public static final String PIC_GARDENS = "9400ZZMAPGD";
    public static final String TRAF_BAR = "9400ZZMATRA";
    public static final String ST_WS_ROAD = "9400ZZMASTW";
    public static final String VICTORIA = "9400ZZMAVIC";
    public static final String DEANSGATE = "9400ZZMAGMX";
    public static final String PICCADILLY = "9400ZZMAPIC";
    public static final String HARBOURCITY = "9400ZZMAHCY";
    public static final String SHAW_AND_CROMPTON = "9400ZZMASHA";


    public static List<String> eastInterchanges = Arrays.asList(PIC_GARDENS, VICTORIA, SHAW_AND_CROMPTON, PICCADILLY);

    public static List<String> westInterchanges = Arrays.asList(CORNBROOK, TRAF_BAR, ST_WS_ROAD, HARBOURCITY,
            DEANSGATE, ST_PETERS_SQUARE);

    // the split into east and west was to make the st peters square closure tests easier to maintain
    private static final Set<String> interchanges = new HashSet<>(ListUtils.union(eastInterchanges,westInterchanges));

    public static boolean has(Location station) {
        // TODO changes for Buses
        return interchanges.contains(station.getId());
    }

    public static Set<String> stations() {
        // TODO changes for Buses
        return interchanges;
    }
}
