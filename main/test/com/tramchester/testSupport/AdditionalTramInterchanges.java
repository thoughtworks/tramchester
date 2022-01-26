package com.tramchester.testSupport;

import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;

import java.util.Arrays;

public class AdditionalTramInterchanges {

    private enum Interchanges {
        // official interchange points not auto-detected by InterchangeRepository, see config for tram routing also
        Deansgate("9400ZZMAGMX"),
        Piccadilly("9400ZZMAPIC");


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

//    public static IdSet<Station> get() {
//        return Arrays.stream(Interchanges.values()).
//                map(interchange -> Station.createId(interchange.stationId)).
//                collect(IdSet.idCollector());
//    }
}
