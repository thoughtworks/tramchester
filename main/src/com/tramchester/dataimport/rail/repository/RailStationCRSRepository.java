package com.tramchester.dataimport.rail.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Set;

import static java.lang.String.format;

@LazySingleton
public class RailStationCRSRepository implements CRSRepository {
    private static final Logger logger = LoggerFactory.getLogger(RailStationCRSRepository.class);

    private final HashMap<IdFor<Station>, String> map;

    public RailStationCRSRepository() {
        map = new HashMap<>();
    }

    public void putCRS(Station station, String crs) {
        final IdFor<Station> stationId = station.getId();
        if (crs.isBlank()) {
            logger.error("Attempt to insert blank CRS for station " + station.getId());
            return;
        }
        map.put(stationId, crs);
    }

    public void keepOnly(Set<Station> stations) {
        long sizeBefore = map.size();
        IdSet<Station> idsToKeep = stations.stream().collect(IdSet.collector());

        IdSet<Station> toRemove = map.keySet().stream().
                filter(id -> !idsToKeep.contains(id)).collect(IdSet.idCollector());

        toRemove.forEach(map::remove);

        logger.info(format("Removed unwanted CRS entries, was %s and now %s", sizeBefore, map.size()));
    }

    public String getCRSFor(Station station) {
        return map.get(station.getId());
    }

    public boolean hasStation(Station station) {
        return map.containsKey(station.getId());
    }

}
