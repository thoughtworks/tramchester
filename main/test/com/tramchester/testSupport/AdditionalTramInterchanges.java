package com.tramchester.testSupport;

import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class AdditionalTramInterchanges {

    private enum Interchanges {
        // official interchange points not autodetected by InterchangeRepository
        CORNBROOK("9400ZZMACRN"),
        ST_PETERS_SQUARE("9400ZZMASTP"),
        PIC_GARDENS("9400ZZMAPGD"),
        TRAF_BAR("9400ZZMATRA"),
        ST_WS_ROAD("9400ZZMASTW"),
        VICTORIA("9400ZZMAVIC"),
        DEANSGATE("9400ZZMAGMX"),
        PICCADILLY("9400ZZMAPIC");

        private final String stationId;

        Interchanges(String stationId) {
            this.stationId = stationId;
        }
    }

    private static final IdSet<Station> ids;

    static {
        ids = new IdSet<>();
        Arrays.asList(Interchanges.values()).forEach(interchange -> ids.add(StringIdFor.createId(interchange.stationId)));
    }

    @Deprecated
    public static IdSet<Station> stations() {
        return ids;
    }

    public static Set<String> get() {
        return Arrays.stream(Interchanges.values()).map(interchange -> interchange.stationId).collect(Collectors.toSet());
    }
}
