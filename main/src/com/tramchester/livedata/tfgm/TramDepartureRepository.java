package com.tramchester.livedata.tfgm;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.mappers.DeparturesMapper;
import com.tramchester.livedata.repository.TramLiveDataCache;
import com.tramchester.livedata.repository.UpcomingDeparturesSource;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.metrics.HasMetrics;
import com.tramchester.metrics.RegistersMetrics;
import com.tramchester.repository.ReportsCacheStats;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class TramDepartureRepository implements UpcomingDeparturesSource, TramLiveDataCache, ReportsCacheStats, HasMetrics {
    private static final Logger logger = LoggerFactory.getLogger(TramDepartureRepository.class);

    // TODO Correct limit here?
    private static final int TIME_LIMIT_MINS = 20; // only enrich if data is within this many minutes
    private static final long STATION_INFO_CACHE_SIZE = 250; // currently 202, see healthcheck for current numbers

    // platformId -> StationDepartureInfo
    private final Cache<IdFor<Platform>, PlatformDueTrams> dueTramsCache;
    private final ProvidesNow providesNow;

    private LocalDateTime lastRefresh;

    @Inject
    public TramDepartureRepository(ProvidesNow providesNow, CacheMetrics registory) {
        this.providesNow = providesNow;

        dueTramsCache = Caffeine.newBuilder().maximumSize(STATION_INFO_CACHE_SIZE).
                expireAfterWrite(TIME_LIMIT_MINS, TimeUnit.MINUTES).recordStats().build();

        registory.register(this);
    }

    @PreDestroy
    public void dispose() {
        dueTramsCache.invalidateAll();
    }

    @Override
    public int updateCache(List<TramStationDepartureInfo> departureInfos) {
        int consumed = consumeDepartInfo(departureInfos);
        dueTramsCache.cleanUp();
        lastRefresh = providesNow.getDateTime();
        logger.info("Update cache. Up to date entries size: " + this.upToDateEntries());
        return consumed;
    }

    private int consumeDepartInfo(List<TramStationDepartureInfo> departureInfos) {
        IdSet<Platform> platformsSeen = new IdSet<>();

        // TODO See platforms in live data not present in the timetable data
        for (TramStationDepartureInfo departureInfo : departureInfos) {
            if (departureInfo.hasStationPlatform()) {
                updateCacheFor(departureInfo, platformsSeen);
            } else {
                logger.warn("No platform, skipping " + departureInfo);
            }
        }

        return platformsSeen.size();
    }

    private void updateCacheFor(TramStationDepartureInfo newDepartureInfo, IdSet<Platform> platformsSeen) {

        Platform platform = newDepartureInfo.getStationPlatform();
        IdFor<Platform> platformId = platform.getId();
        if (!platformsSeen.contains(platformId)) {
            platformsSeen.add(platformId);
            dueTramsCache.put(platformId, new PlatformDueTrams(newDepartureInfo));
            return;
        }

        // many platforms have more than one display, no need to warn about it
        PlatformDueTrams existingEntry = dueTramsCache.getIfPresent(platformId);
        if (existingEntry!=null) {
            newDepartureInfo.getDueTrams().forEach(dueTram -> {
                if (!existingEntry.hasDueTram(dueTram)) {
                    logger.info(format("Additional due tram '%s' seen for platform id '%s'", dueTram, platformId));
                    existingEntry.addDueTram(dueTram);
                }
            });
        }
    }

    @Override
    public List<UpcomingDeparture> dueTramsForStation(Station station) {
        Set<UpcomingDeparture> allTrams = station.getPlatforms().stream().
                flatMap(platform -> dueTramsForPlatform(platform.getId()).stream()).
                collect(Collectors.toSet());

        if (allTrams.isEmpty()) {
            logger.debug("No trams found for " + HasId.asId(station));
            return Collections.emptyList();
        }

        List<UpcomingDeparture> dueTrams = allTrams.stream().
                filter(dueTram -> DeparturesMapper.DUE.equals(dueTram.getStatus())).
                collect(Collectors.toList());

        if (dueTrams.isEmpty()) {
            logger.info("No DUE trams found for " + HasId.asId(station));
        }

        return dueTrams;
    }

    private List<UpcomingDeparture> dueTramsForPlatform(IdFor<Platform> platform) {
        if (lastRefresh==null) {
            logger.warn("No refresh has happened");
            return Collections.emptyList();
        }
        Optional<PlatformDueTrams> maybe = departuresFor(platform);
        if (maybe.isEmpty()) {
            logger.info("No due trams found for platform: " + platform);
            return Collections.emptyList();
        }

        PlatformDueTrams departureInfo = maybe.get();
        return departureInfo.getDueTrams();
    }

    private boolean withinTime(TramTime queryTime, LocalTime updateTime) {
        TramTime limitBefore = TramTime.ofHourMins(updateTime.minusMinutes(TIME_LIMIT_MINS));
        TramTime limitAfter = TramTime.ofHourMins(updateTime.plusMinutes(TIME_LIMIT_MINS));
        return queryTime.between(limitBefore, limitAfter);
    }

    private Optional<PlatformDueTrams> departuresFor(IdFor<Platform> platformId) {
        PlatformDueTrams ifPresent = dueTramsCache.getIfPresent(platformId);

        if (ifPresent==null) {
            logger.warn("Could not find departure info for " + platformId);
            return Optional.empty();
        }
        return Optional.of(ifPresent);
    }


    @Override
    public List<Pair<String, CacheStats>> stats() {
        return Collections.singletonList(
                Pair.of("PlatformMessageRepository:messageCache", dueTramsCache.stats()));
    }

    // for healthcheck
    public int upToDateEntries() {
        dueTramsCache.cleanUp();
        return (int) dueTramsCache.estimatedSize();
    }

    // for healthcheck
    public int getNumStationsWithData(LocalDateTime queryDateTime) {
        if (!queryDateTime.toLocalDate().equals(lastRefresh.toLocalDate())) {
            return 0;
        }

        TramTime queryTime = TramTime.ofHourMins(queryDateTime.toLocalTime());
        return getEntryStream(queryTime).
                map(PlatformDueTrams::getStation).collect(Collectors.toSet()).size();
    }

    public int getNumStationsWithTrams(LocalDateTime dateTime) {
        if (!dateTime.toLocalDate().equals(lastRefresh.toLocalDate())) {
            return 0;
        }

        TramTime queryTime = TramTime.ofHourMins(dateTime.toLocalTime());
        return getEntryStream(queryTime).filter(entry -> !entry.getDueTrams().isEmpty())
                .map(PlatformDueTrams::getStation).collect(Collectors.toSet()).size();
    }

    private Stream<PlatformDueTrams> getEntryStream(TramTime queryTime) {
        return dueTramsCache.asMap().values().stream().
                filter(entry -> withinTime(queryTime, entry.getLastUpdate().toLocalTime()));
    }

    @Override
    public void registerMetrics(RegistersMetrics registersMetrics) {
        registersMetrics.add(this, "liveData", "number", this::upToDateEntries);
        registersMetrics.add(this, "liveData", "stationsWithData", this::getNumStationsWithDataNow);
        registersMetrics.add(this,"liveData", "stationsWithTrams", this::getNumStationsWithTramsNow);
    }

    private Integer getNumStationsWithDataNow() {
        return getNumStationsWithData(providesNow.getDateTime());
    }

    private Integer getNumStationsWithTramsNow() {
        return getNumStationsWithTrams(providesNow.getDateTime());
    }

    private static class PlatformDueTrams {
        private final Platform stationPlatform;
        private final List<UpcomingDeparture> dueTrams;
        private final LocalDateTime lastUpdate;
        private final IdFor<Station> stationId;

        private PlatformDueTrams(Platform stationPlatform, List<UpcomingDeparture> dueTrams, LocalDateTime lastUpdate,
                                 IdFor<Station> stationId) {
            this.stationPlatform = stationPlatform;
            this.dueTrams = dueTrams;
            this.lastUpdate = lastUpdate;
            this.stationId = stationId;
        }

        public PlatformDueTrams(TramStationDepartureInfo departureInfo) {
            this(departureInfo.getStationPlatform(), departureInfo.getDueTrams(), departureInfo.getLastUpdate(),
                    departureInfo.getStation().getId());
        }

        public boolean hasDueTram(UpcomingDeparture dueTram) {
            return dueTrams.contains(dueTram);
        }

        public void addDueTram(UpcomingDeparture dueTram) {
            dueTrams.add(dueTram);
        }

        public LocalDateTime getLastUpdate() {
            return lastUpdate;
        }

        public List<UpcomingDeparture> getDueTrams() {
            return dueTrams;
        }

        public IdFor<Station> getStation() {
            return stationId;
        }

        @Override
        public String toString() {
            return "PlatformDueTrams{" +
                    "stationPlatform=" + stationPlatform.getId() +
                    ", stationId=" + stationId +
                    ", dueTrams=" + dueTrams +
                    ", lastUpdate=" + lastUpdate +
                    '}';
        }
    }
}
