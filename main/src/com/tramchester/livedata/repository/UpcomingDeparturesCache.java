package com.tramchester.livedata.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.ReportsCacheStats;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UpcomingDeparturesCache  {
    private static final Logger logger = LoggerFactory.getLogger(UpcomingDeparturesCache.class);

    private final DeparturesCache cache;
    private final CacheMetrics cacheMetrics;

    public UpcomingDeparturesCache(long size, Duration duration, CacheMetrics cacheMetrics) {
        this.cacheMetrics = cacheMetrics;
        cache = new DeparturesCache(size, duration);
    }

    public List<UpcomingDeparture> getOrUpdate(Station station, CacheUpdateStrategy cacheUpdateStrategy) {
        logger.info("Get for " + station);
        return cache.getOrUpdate(station, cacheUpdateStrategy);
    }


    public void start() {
        logger.info("starting " + this);
        cacheMetrics.register(cache);
        logger.info("started " + this);
    }

    private static class DeparturesCache implements ReportsCacheStats {
        private final Cache<Station, List<UpcomingDeparture>> cache;

        private DeparturesCache(long size, Duration duration) {
            cache = Caffeine.newBuilder().maximumSize(size).
                    expireAfterWrite(duration.getSeconds(), TimeUnit.SECONDS).
                    initialCapacity((int) size).
                    recordStats().build();
        }

        public List<UpcomingDeparture> getOrUpdate(Station station, CacheUpdateStrategy cacheUpdateStrategy) {
            logger.info("get departures for " + station.getId());
            return cache.get(station, key -> cacheUpdateStrategy.updateFor(station));
        }

        @Override
        public List<Pair<String, CacheStats>> stats() {
            return Collections.singletonList(
                    Pair.of("UpcomingDeparturesCache:cache", cache.stats()));
        }
    }

    public interface CacheUpdateStrategy {
        List<UpcomingDeparture> updateFor(Station station);
    }
}
