package com.tramchester.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.Platform;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.PlatformDueTrams;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.DeparturesMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.picocontainer.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class DueTramsRepository implements DueTramsSource, Disposable, ReportsCacheStats {
    private static final Logger logger = LoggerFactory.getLogger(DueTramsRepository.class);

    private static final int TIME_LIMIT = 20; // only enrich if data is within this many minutes
    private static final long STATION_INFO_CACHE_SIZE = 250; // currently 202, see healthcheck for current numbers

    // platformId -> StationDepartureInfo
    private final Cache<IdFor<Platform>, PlatformDueTrams> dueTramsCache;
    private final ProvidesNow providesNow;

    private LocalDateTime lastRefresh;

    public DueTramsRepository(ProvidesNow providesNow) {
        this.providesNow = providesNow;

        dueTramsCache = Caffeine.newBuilder().maximumSize(STATION_INFO_CACHE_SIZE).
                expireAfterWrite(TIME_LIMIT, TimeUnit.MINUTES).recordStats().build();
    }

    @Override
    public void dispose() {
        dueTramsCache.invalidateAll();
    }

    @Override
    public int updateCache(List<StationDepartureInfo> departureInfos) {
        int consumed = consumeDepartInfo(departureInfos);
        dueTramsCache.cleanUp();
        lastRefresh = providesNow.getDateTime();
        return consumed;
    }

    private int consumeDepartInfo(List<StationDepartureInfo> receivedUpdate) {
        IdSet<Platform> platformsSeen = new IdSet<>();

        for (StationDepartureInfo newDepartureInfo : receivedUpdate) {
            updateCacheFor(newDepartureInfo, platformsSeen);
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
    public List<DueTram> dueTramsFor(Station station, TramServiceDate when, TramTime queryTime) {
        Set<PlatformDueTrams> platformDueTrams = station.getPlatforms().stream().
                map(platform -> dueTramsFor(platform.getId(), when, queryTime)).
                filter(Optional::isPresent).
                map(Optional::get).
                collect(Collectors.toSet());

        return platformDueTrams.stream().
                map(PlatformDueTrams::getDueTrams).
                flatMap(Collection::stream).
                filter(dueTram -> DeparturesMapper.DUE.equals(dueTram.getStatus())).
                collect(Collectors.toList());
    }

    @Override
    public Optional<PlatformDueTrams> dueTramsFor(IdFor<Platform> platform, TramServiceDate queryDate, TramTime queryTime) {
        if (lastRefresh==null) {
            logger.warn("No refresh has happened");
            return Optional.empty();
        }
        if (!queryDate.getDate().equals(lastRefresh.toLocalDate())) {
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
            logger.info("last update of departure info (" + infoLastUpdate +") not within query time " + queryTime);
            return Optional.empty();
        }
        return Optional.of(departureInfo);
    }

    private boolean withinTime(TramTime queryTime, LocalTime updateTime) {
        TramTime limitBefore = TramTime.of(updateTime.minusMinutes(TIME_LIMIT));
        TramTime limitAfter = TramTime.of(updateTime.plusMinutes(TIME_LIMIT));
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

    public int upToDateEntries() {
        dueTramsCache.cleanUp();
        return (int) dueTramsCache.estimatedSize();
    }
}
