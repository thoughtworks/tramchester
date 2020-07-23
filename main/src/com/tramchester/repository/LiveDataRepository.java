package com.tramchester.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.Platform;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.LiveDataFetcher;
import com.tramchester.mappers.DeparturesMapper;
import com.tramchester.mappers.LiveDataParser;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.picocontainer.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class LiveDataRepository implements LiveDataSource, ReportsCacheStats, Disposable {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataRepository.class);

    private static final int TIME_LIMIT = 20; // only enrich if data is within this many minutes
    private static final long STATION_INFO_CACHE_SIZE = 250; // currently 202, see healthcheck for current numbers

    // platformId -> StationDepartureInfo
    private final Cache<IdFor<Platform>, StationDepartureInfo> departureInfoCache;
    private final IdSet<Platform> uniquePlatformsSeen;
    private final List<LiveDataObserver> observers;
    private final LiveDataFetcher fetcher;
    private final LiveDataParser parser;
    private final ProvidesNow providesNow;

    private LocalDateTime lastRefresh;

    public LiveDataRepository(LiveDataFetcher fetcher, LiveDataParser parser, ProvidesNow providesNow) {
        this.fetcher = fetcher;
        this.parser = parser;
        this.providesNow = providesNow;

        departureInfoCache = Caffeine.newBuilder().maximumSize(STATION_INFO_CACHE_SIZE).
                expireAfterWrite(TIME_LIMIT, TimeUnit.MINUTES).recordStats().build();
        uniquePlatformsSeen = new IdSet<>();
        observers = new LinkedList<>();
    }

    @Override
    public void dispose() {
        departureInfoCache.invalidateAll();
        uniquePlatformsSeen.clear();
        observers.clear();
    }

    public void refreshRespository()  {

        logger.info("Refresh repository");
        String payload  = fetcher.fetch();
        List<StationDepartureInfo> receivedInfos = Collections.emptyList();
        if (payload.length()>0) {
            receivedInfos = parser.parse(payload);
        }

        int received = receivedInfos.size();
        logger.info(format("Received %s updates", received));
        int updatesConsumed = consumeDepartInfo(receivedInfos);
        departureInfoCache.cleanUp();

        long currentSize = departureInfoCache.estimatedSize();
        if (currentSize != updatesConsumed) {
            logger.warn(format("Unable to fully refresh (current: %s consumed: %s) live data", currentSize, updatesConsumed));
        } else {
            logger.info("Refreshed live data, count is: " + departureInfoCache.estimatedSize());
        }
        lastRefresh = providesNow.getDateTime();
        invokeObservers();
    }

    private int consumeDepartInfo(List<StationDepartureInfo> receivedUpdate) {
        IdSet<Platform> platformsSeen = new IdSet<>();
        TramTime now = providesNow.getNow();
        LocalDate date = providesNow.getDate();
        int stale = 0;

        for (StationDepartureInfo newDepartureInfo : receivedUpdate) {
            uniquePlatformsSeen.add(newDepartureInfo.getStationPlatform());
            if (isTimely(newDepartureInfo, date, now)) {
                updateCacheFor(newDepartureInfo, platformsSeen);
            } else {
                stale = stale + 1;
                logger.warn("Received stale departure info " + newDepartureInfo);
            }
        }
        return platformsSeen.size();
    }

    private boolean isTimely(StationDepartureInfo newDepartureInfo, LocalDate date, TramTime now) {
        LocalDate updateDate = newDepartureInfo.getLastUpdate().toLocalDate();
        if (!updateDate.equals(date)) {
            logger.warn("Received invalid update, date was " + updateDate);
            return false;
        }
        TramTime updateTime = TramTime.of(newDepartureInfo.getLastUpdate());
        if (TramTime.diffenceAsMinutes(now, updateTime) > TIME_LIMIT) {
            logger.warn(format("Received invalid update. Local Now: %s Update: %s ", providesNow.getNow(), updateDate));
            return false;
        }
        return true;
    }

    private void updateCacheFor(StationDepartureInfo newDepartureInfo, IdSet<Platform> platformsSeen) {
        IdFor<Platform> platformId = newDepartureInfo.getStationPlatform();
        if (!platformsSeen.contains(platformId)) {
            platformsSeen.add(platformId);
            departureInfoCache.put(platformId, newDepartureInfo);
            return;
        }

        // many platforms have more than one display, no need to warn about it
        @Nullable StationDepartureInfo existingEntry = departureInfoCache.getIfPresent(platformId);
        if (existingEntry!=null) {
            newDepartureInfo.getDueTrams().forEach(dueTram -> {
                if (!existingEntry.hasDueTram(dueTram)) {
                    logger.info(format("Additional due tram '%s' seen for platform id '%s'", dueTram, platformId));
                    existingEntry.addDueTram(dueTram);
                }
            });
        }
    }

    private void invokeObservers() {
        try {
            observers.forEach(observer -> observer.seenUpdate(departureInfoCache.asMap().values()));
        }
        catch (RuntimeException runtimeException) {
            logger.error("Error invoking observer", runtimeException);
        }
    }

    @Override
    public Optional<StationDepartureInfo> departuresFor(IdFor<Platform> platform, TramServiceDate tramServiceDate, TramTime queryTime) {
        if (lastRefresh==null) {
            logger.warn("No refresh has happened");
            return Optional.empty();
        }
        if (!tramServiceDate.getDate().equals(lastRefresh.toLocalDate())) {
            logger.warn("No data for date, not querying for departure info " + tramServiceDate);
            return Optional.empty();
        }
        Optional<StationDepartureInfo> maybe = departuresFor(platform);
        if (maybe.isEmpty()) {
            return Optional.empty();
        }
        StationDepartureInfo departureInfo = maybe.get();

        LocalDateTime infoLastUpdate = departureInfo.getLastUpdate();
        if (!withinTime(queryTime, infoLastUpdate.toLocalTime())) {
           return Optional.empty();
        }
        return Optional.of(departureInfo);
    }

    @Override
    public List<StationDepartureInfo> departuresFor(Station station, TramServiceDate when, TramTime queryTime) {
        List<StationDepartureInfo> results = new ArrayList<>();
        station.getPlatforms().forEach(platform -> departuresFor(platform.getId(), when, queryTime).ifPresent(results::add));
        return results;
    }

    @Override
    public List<DueTram> dueTramsFor(Station station, TramServiceDate when, TramTime queryTime) {
        return departuresFor(station, when, queryTime).stream().
                map(StationDepartureInfo::getDueTrams).
                flatMap(Collection::stream).
                filter(dueTram -> DeparturesMapper.DUE.equals(dueTram.getStatus())).
                collect(Collectors.toList());
    }

    private boolean withinTime(TramTime queryTime, LocalTime updateTime) {
        TramTime limitBefore = TramTime.of(updateTime.minusMinutes(TIME_LIMIT));
        TramTime limitAfter = TramTime.of(updateTime.plusMinutes(TIME_LIMIT));
        return queryTime.between(limitBefore, limitAfter);
    }

    public int upToDateEntries() {
        departureInfoCache.cleanUp();
        return (int) departureInfoCache.estimatedSize();
    }

    public int entriesWithMessages() {
        return (int) departureInfoCache.asMap().values().stream().filter(info -> !info.getMessage().isEmpty()).count();
    }

    public long missingDataCount() {
        long upToDateEntries = upToDateEntries();
        long totalSeen = uniquePlatformsSeen.size();

        if (upToDateEntries<totalSeen) {
            logger.error(format("%s out of %s records are within of cuttoff time %s minutes", upToDateEntries, totalSeen, TIME_LIMIT));
        }
        return totalSeen-upToDateEntries;
    }

    private Optional<StationDepartureInfo> departuresFor(IdFor<Platform> platformId) {
        //IdFor<Platform> platformId = platform.getId();

        @Nullable StationDepartureInfo ifPresent = departureInfoCache.getIfPresent(platformId);

        if (ifPresent==null) {
            logger.warn("Could not find departure info for " + platformId);
            return Optional.empty();
        }
        return Optional.of(ifPresent);
    }

    public void observeUpdates(LiveDataObserver observer) {
        observers.add(observer);
    }

    @Override
    public List<Pair<String, CacheStats>> stats() {
        return Collections.singletonList(
                Pair.of("LiveDataRepository:departureInfoCache", departureInfoCache.stats()));
    }

}
