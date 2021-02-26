package com.tramchester.domain.input;

import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;

import java.util.Arrays;

public class TramInterchanges {

    // TODO Into config?

    private enum Interchanges {
        // official
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

    public static IdSet<Station> stations() {
        return ids;
    }
}
