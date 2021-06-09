package com.tramchester.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.PlatformDueTrams;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.DeparturesMapper;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.metrics.HasMetrics;
import com.tramchester.metrics.RegistersMetrics;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class DueTramsRepository implements DueTramsSource, ReportsCacheStats, HasMetrics {
    private static final Logger logger = LoggerFactory.getLogger(DueTramsRepository.class);

    // TODO Correct limit here?
    private static final int TIME_LIMIT_MINS = 20; // only enrich if data is within this many minutes
    private static final long STATION_INFO_CACHE_SIZE = 250; // currently 202, see healthcheck for current numbers

    // platformId -> StationDepartureInfo
    private final Cache<IdFor<Platform>, PlatformDueTrams> dueTramsCache;
    private final ProvidesNow providesNow;

    private LocalDateTime lastRefresh;

    @Inject
    public DueTramsRepository(ProvidesNow providesNow, CacheMetrics registory) {
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
    public int updateCache(List<StationDepartureInfo> departureInfos) {
        int consumed = consumeDepartInfo(departureInfos);
        dueTramsCache.cleanUp();
        lastRefresh = providesNow.getDateTime();
        logger.info("Update cache. Up to date entries size: " + this.upToDateEntries());
        return consumed;
    }

    private int consumeDepartInfo(List<StationDepartureInfo> departureInfos) {
        IdSet<Platform> platformsSeen = new IdSet<>();

        for (StationDepartureInfo departureInfo : departureInfos) {
            updateCacheFor(departureInfo, platformsSeen);
        }

        return platformsSeen.size();
    }

    private void updateCacheFor(StationDepartureInfo newDepartureInfo, IdSet<Platform> platformsSeen) {
        IdFor<Platform> platformId = newDepartureInfo.getStationPlatform();
        if (!platformsSeen.contains(platformId)) {
            platformsSeen.add(platformId);
            dueTramsCache.put(platformId, new PlatformDueTrams(newDepartureInfo));
            return;
        }

        // many platforms have more than one display, no need to warn about it
        @Nullable PlatformDueTrams existingEntry = dueTramsCache.getIfPresent(platformId);
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
    public List<DueTram> dueTramsFor(Station station, LocalDate date, TramTime queryTime) {
        Set<PlatformDueTrams> allTrams = station.getPlatforms().stream().
                map(platform -> allTrams(platform.getId(), date, queryTime)).
                filter(Optional::isPresent).
                map(Optional::get).
                collect(Collectors.toSet());

        if (allTrams.isEmpty()) {
            logger.info("No trams found for " + HasId.asId(station) + " at " + date + " " + queryTime);
            return Collections.emptyList();
        }

        List<DueTram> dueTrams = allTrams.stream().
                map(PlatformDueTrams::getDueTrams).
                flatMap(Collection::stream).
                filter(dueTram -> DeparturesMapper.DUE.equals(dueTram.getStatus())).
                collect(Collectors.toList());

        if (dueTrams.isEmpty()) {
            logger.info("No DUE trams found for " + HasId.asId(station) + " at " + date + " " + queryTime);
        }

        return dueTrams;
    }

    @Override
    public Optional<PlatformDueTrams> allTrams(IdFor<Platform> platform, LocalDate queryDate, TramTime queryTime) {
        if (lastRefresh==null) {
            logger.warn("No refresh has happened");
            return Optional.empty();
        }
        if (!queryDate.equals(lastRefresh.toLocalDate())) {
            logger.warn("No data for date, not querying for departure info " + queryDate);
            return Optional.empty();
        }
        Optional<PlatformDueTrams> maybe = departuresFor(platform);
        if (maybe.isEmpty()) {
            logger.info("No due trams found for platform: " + platform);
            return Optional.empty();
        }
        PlatformDueTrams departureInfo = maybe.get();

        LocalDateTime infoLastUpdate = departureInfo.getLastUpdate();
        if (!withinTime(queryTime, infoLastUpdate.toLocalTime())) {
            logger.info("Last update of departure info (" + infoLastUpdate +") not within query time " + queryTime);
            return Optional.empty();
        }
        return Optional.of(departureInfo);
    }

    private boolean withinTime(TramTime queryTime, LocalTime updateTime) {
        TramTime limitBefore = TramTime.of(updateTime.minusMinutes(TIME_LIMIT_MINS));
        TramTime limitAfter = TramTime.of(updateTime.plusMinutes(TIME_LIMIT_MINS));
        return queryTime.between(limitBefore, limitAfter);
    }

    private Optional<PlatformDueTrams> departuresFor(IdFor<Platform> platformId) {
        @Nullable PlatformDueTrams ifPresent = dueTramsCache.getIfPresent(platformId);

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

        TramTime queryTime = TramTime.of(queryDateTime.toLocalTime());
        return getEntryStream(queryTime).
                map(PlatformDueTrams::getStation).collect(Collectors.toSet()).size();
    }

    public int getNumStationsWithTrams(LocalDateTime dateTime) {
        if (!dateTime.toLocalDate().equals(lastRefresh.toLocalDate())) {
            return 0;
        }

        TramTime queryTime = TramTime.of(dateTime.toLocalTime());
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
}
