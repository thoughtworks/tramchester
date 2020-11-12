package com.tramchester.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.domain.HasId;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.Platform;
import com.tramchester.domain.liveUpdates.PlatformMessage;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.picocontainer.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlatformMessageRepository implements PlatformMessageSource, Disposable, ReportsCacheStats {
    private static final Logger logger = LoggerFactory.getLogger(PlatformMessageRepository.class);

    private static final int TIME_LIMIT = 20; // only enrich if data is within this many minutes
    private static final long STATION_INFO_CACHE_SIZE = 250; // currently 202, see healthcheck for current numbers

    private final Cache<IdFor<Platform>, PlatformMessage> messageCache;
    private final ProvidesNow providesNow;
    private LocalDate lastRefresh;

    public PlatformMessageRepository(ProvidesNow providesNow) {
        this.providesNow = providesNow;
        messageCache = Caffeine.newBuilder().maximumSize(STATION_INFO_CACHE_SIZE).
                expireAfterWrite(TIME_LIMIT, TimeUnit.MINUTES).recordStats().build();
    }

    @Override
    public void dispose() {
        messageCache.invalidateAll();
    }

    @Override
    public int updateCache(List<StationDepartureInfo> departureInfos) {
        logger.info("Updating cache");
        consumeDepartInfo(departureInfos);
        messageCache.cleanUp();
        lastRefresh = providesNow.getDate();
        int entries = numberOfEntries();
        logger.info("Cache now has " + entries + " entries");
        return entries;
    }

    private void consumeDepartInfo(List<StationDepartureInfo> departureInfos) {
        IdSet<Platform> platformsSeen = new IdSet<>();
        int emptyMessages = 0;

        for (StationDepartureInfo departureInfo : departureInfos) {
            if (!updateCacheFor(departureInfo, platformsSeen)) {
                emptyMessages = emptyMessages + 1;
            }
        }

        if (emptyMessages>0) {
            logger.info("Received "+emptyMessages+" empty messages");
        }
    }

    private boolean updateCacheFor(StationDepartureInfo departureInfo, IdSet<Platform> platformsSeenForUpdate) {
        IdFor<Platform> platformId = departureInfo.getStationPlatform();
        if (platformsSeenForUpdate.contains(platformId)) {
            if (!departureInfo.getMessage().isEmpty()) {
                PlatformMessage current = messageCache.getIfPresent(platformId);
                String currentMessage = current.getMessage();
                if (!departureInfo.getMessage().equals(currentMessage)) {
                    logger.warn("Mutiple messages for " + platformId + " displayId: " + departureInfo.getDisplayId() +
                            " Received: '" + departureInfo.getMessage() + "' was '" + currentMessage + "' displayId: "
                            + current.getDisplayId());
                }
            } else {
                logger.info("Multiple (but empty) message for " + platformId + " displayId: " + departureInfo.getDisplayId());
            }
            return false;
        }

        if (departureInfo.getMessage().isEmpty()) {
            logger.info("Skipping empty message for " +platformId+ " displayId: " + departureInfo.getDisplayId());
            return false;
        }

        platformsSeenForUpdate.add(platformId);
        messageCache.put(platformId, new PlatformMessage(departureInfo));
        return true;
    }

    @Override
    public List<PlatformMessage> messagesFor(Station station, LocalDate when, TramTime queryTime) {
        // TODO this uses the timetable station/platform association, but seems live data includes extra platforms
        // not provided in stops.txt feed
        logger.info("Get messages for " + HasId.asId(station));
        List<PlatformMessage> results = new ArrayList<>();
        station.getPlatforms().forEach(platform -> messagesFor(platform.getId(), when, queryTime).ifPresent(results::add));
        if (results.isEmpty()) {
            logger.warn("No platform messages found for " + HasId.asId(station));
        }
        return results;
    }

    @Override
    public Optional<PlatformMessage> messagesFor(IdFor<Platform> platformId, LocalDate queryDate, TramTime queryTime) {
        if (lastRefresh==null) {
            logger.warn("No refresh has happened");
            return Optional.empty();
        }
        if (!queryDate.equals(lastRefresh)) {
            logger.warn("No data for date, not querying for departure info " + queryDate);
            return Optional.empty();
        }
        Optional<PlatformMessage> maybe = messagesFor(platformId);
        if (maybe.isEmpty()) {
            logger.info("No message found for platform: " + platformId);
            return Optional.empty();
        }
        PlatformMessage departureInfo = maybe.get();

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

    public int numberOfEntries() {
        messageCache.cleanUp();
        return (int) messageCache.estimatedSize();
    }

    @Deprecated
    @NotNull
    public Stream<PlatformMessage> getEntriesWithMessages() {
        return messageCache.asMap().values().stream();
    }

    private Optional<PlatformMessage> messagesFor(IdFor<Platform> platformId) {
        @Nullable PlatformMessage ifPresent = messageCache.getIfPresent(platformId);

        if (ifPresent==null) {
            return Optional.empty();
        }
        return Optional.of(ifPresent);
    }

    @Override
    public List<Pair<String, CacheStats>> stats() {
        return Collections.singletonList(
                Pair.of("PlatformMessageRepository:messageCache", messageCache.stats()));
    }

    // for healthcheck
    public int numberStationsWithMessages(LocalDateTime queryDateTime) {
        if (!queryDateTime.toLocalDate().equals(lastRefresh)) {
            return 0;
        }

        TramTime queryTime = TramTime.of(queryDateTime);
        Set<Station> haveMessages = messageCache.asMap().values().stream().
                filter(entry -> withinTime(queryTime, entry.getLastUpdate().toLocalTime())).
                map(PlatformMessage::getStation).collect(Collectors.toSet());
        logger.debug("Stations with messages " + haveMessages);
        return haveMessages.size();
    }

    // for metrics
    public Integer numberStationsWithMessagesNow() {
        return numberStationsWithMessages(providesNow.getDateTime());
    }
}
