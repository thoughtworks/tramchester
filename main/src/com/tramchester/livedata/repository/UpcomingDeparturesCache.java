package com.tramchester.livedata.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.tfgm.TramDepartureRepository;
import com.tramchester.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@LazySingleton
public class UpcomingDeparturesCache implements  UpcomingDeparturesSource {
    private static final Logger logger = LoggerFactory.getLogger(UpcomingDeparturesCache.class);

    private Map<DataSourceID, DeparturesCache> caches;
    private final StationRepository stationRepository;
    private final TramDepartureRepository tramDepartureRepository;

    @Inject
    public UpcomingDeparturesCache(StationRepository stationRepository, TramDepartureRepository tramDepartureRepository) {
        this.stationRepository = stationRepository;
        this.tramDepartureRepository = tramDepartureRepository;
        caches = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        // TODO from config, which sources? Or get implementations of the i/f??
        long tfgmTramStationCount = stationRepository.getNumberOfStations(DataSourceID.tfgm, TransportMode.Tram);
        caches.put(DataSourceID.tfgm, new DeparturesCache(tfgmTramStationCount, Duration.ofMinutes(20)));
        logger.info("started");
    }

    @Override
    public List<UpcomingDeparture> forStation(Station station) {
        DataSourceID dataSourceID = station.getDataSourceID();
        if (!caches.containsKey(dataSourceID)) {
            final String msg = "No live data available for " + dataSourceID;
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        DeparturesCache cache = caches.get(dataSourceID);
        throw new RuntimeException("WIP");
    }

    private class DeparturesCache {
        private final Cache<IdFor<Platform>, Set<UpcomingDeparture>> cache;

        private DeparturesCache(long size, Duration duration) {
            cache = Caffeine.newBuilder().maximumSize(size).
                    expireAfterAccess(duration.getSeconds(), TimeUnit.SECONDS).
                    initialCapacity((int) size).
                    recordStats().build();
        }

//        public void updateForStation(Station station, LocalDate date, TramTime queryTime) {
//            result = station.getPlatforms().stream().
//                    flatMap(platform -> cache.get(platform.getId(), id ->
//                            tramDepartureRepository.dueTramsForPlatform(id, date, queryTime).stream())).
//                    collect(Collectors.toSet());
//        }
    }
}
