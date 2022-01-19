package com.tramchester.testSupport;

import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class AdditionalTramInterchanges {

    private enum Interchanges {
        // official interchange points not auto-detected by InterchangeRepository, see config for tram routing also
        ST_WS_ROAD("9400ZZMASTW"),
        POMONA("9400ZZMAPOM"), // needed otherwise some interconnects are not possible
        HARBOUR_CITY("9400ZZMAHCY"),
        BROADWAY("9400ZZMABWY");
        //ETIHAD("9400ZZMAECS");

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

    public static Set<String> get() {
        return Arrays.stream(Interchanges.values()).map(interchange -> interchange.stationId).collect(Collectors.toSet());
    }
}
