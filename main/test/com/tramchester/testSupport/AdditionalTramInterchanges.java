package com.tramchester.testSupport;

import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;

import java.util.Arrays;

public class AdditionalTramInterchanges {

    private enum Interchanges {

        // official interchange points not auto-detected by InterchangeRepository, see config for tram routing also
        Deansgate("9400ZZMAGMX");
        //MediacityUK("9400ZZMAMCU"),
        //Piccadilly("9400ZZMAPIC");

        private final String stationId;

        Interchanges(String stationId) {
            this.stationId = stationId;
        }
    }

    public static IdSet<Station> stations() {
        return Arrays.stream(Interchanges.values()).map(id -> Station.createId(id.stationId)).collect(IdSet.idCollector());
    }

}
