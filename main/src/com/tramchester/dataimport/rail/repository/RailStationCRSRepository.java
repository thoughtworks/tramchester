package com.tramchester.dataimport.rail.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;

@LazySingleton
public class RailStationCRSRepository implements CRSRepository {
    private static final Logger logger = LoggerFactory.getLogger(RailStationCRSRepository.class);

    private final Map<IdFor<Station>, String> toCrs;
    private final Map<String, Station> toStation;

    public RailStationCRSRepository() {
        toStation = new HashMap<>();
        toCrs = new HashMap<>();
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        toCrs.clear();
        toStation.clear();
        logger.info("Stopped");
    }

    public void putCRS(Station station, String crs) {
        final IdFor<Station> stationId = station.getId();
        if (crs.isBlank()) {
            logger.error("Attempt to insert blank CRS for station " + station.getId());
            return;
        }
        toCrs.put(stationId, crs);
        toStation.put(crs, station);
    }

    public String getCRSFor(Station station) {
        return toCrs.get(station.getId());
    }

    public boolean hasStation(Station station) {
        return toCrs.containsKey(station.getId());
    }

    @Override
    public Station getFor(String crs) {
        return toStation.get(crs);
    }

    @Override
    public boolean hasCrs(String crs) {
        return toStation.containsKey(crs);
    }

}
