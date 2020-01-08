package com.tramchester.domain.input;

import com.tramchester.domain.Location;
import org.apache.commons.collections4.ListUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TramInterchanges {

    private enum Interchanges {
        // official
        CORNBROOK("9400ZZMACRN"),
        ST_PETERS_SQUARE("9400ZZMASTP"),
        PIC_GARDENS("9400ZZMAPGD"),
        TRAF_BAR("9400ZZMATRA"),
        ST_WS_ROAD("9400ZZMASTW"),
        VICTORIA("9400ZZMAVIC"),
        DEANSGATE("9400ZZMAGMX"),
        PICCADILLY("9400ZZMAPIC"),
        // additional route swap points, needed for journeys when restrict change over points
        SHAW_AND_CROMPTON("9400ZZMASHA"),
        HARBOUR_CITY("9400ZZMAHCY");

        private final String stationId;

        Interchanges(String stationId) {
            this.stationId = stationId;
        }
    }

    private static Set<String> ids;

    static {
        ids = new HashSet<>();
        Arrays.asList(Interchanges.values()).forEach(interchange -> ids.add(interchange.stationId));
    }

    public static boolean has(String stationId) {
        return ids.contains(stationId);
    }

    public static boolean has(Location station) {
        return has(station.getId());
    }

    public static Set<String> stations() {
        return ids;
    }
}
