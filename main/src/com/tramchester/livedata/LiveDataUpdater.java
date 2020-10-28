package com.tramchester.livedata;

import com.tramchester.domain.IdSet;
import com.tramchester.domain.Platform;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.LiveDataParser;
import com.tramchester.repository.DueTramsRepository;
import com.tramchester.repository.LiveDataObserver;
import com.tramchester.repository.PlatformMessageRepository;
import org.picocontainer.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;

public class LiveDataUpdater implements Disposable {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataUpdater.class);

    private static final int TIME_LIMIT = 20; // only enrich if data is within this many minutes

    private final IdSet<Platform> uniquePlatformsSeen;
    private final List<LiveDataObserver> observers;
    private final PlatformMessageRepository platformMessageRepository;
    private final DueTramsRepository dueTramsRepository;

    private final LiveDataFetcher fetcher;
    private final LiveDataParser parser;
    private final ProvidesNow providesNow;

    public LiveDataUpdater(PlatformMessageRepository platformMessageRepository, DueTramsRepository dueTramsRepository,
                           LiveDataFetcher fetcher, LiveDataParser parser, ProvidesNow providesNow) {
        this.platformMessageRepository = platformMessageRepository;
        this.dueTramsRepository = dueTramsRepository;
        this.fetcher = fetcher;
        this.parser = parser;
        this.providesNow = providesNow;

        uniquePlatformsSeen = new IdSet<>();
        observers = new LinkedList<>();
    }

    @Override
    public void dispose() {
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

        TramTime now = providesNow.getNow();
        LocalDate date = providesNow.getDate();
        int stale = 0;

        List<StationDepartureInfo> fresh = new ArrayList<>();
        for (StationDepartureInfo departureInfo : receivedInfos) {
            uniquePlatformsSeen.add(departureInfo.getStationPlatform());
            if (isTimely(departureInfo, date, now)) {
                fresh.add(departureInfo);
            } else {
                stale = stale + 1;
                logger.warn("Received stale departure info " + departureInfo);
            }
        }

        dueTramsRepository.updateCache(fresh);
        platformMessageRepository.updateCache(fresh);

        invokeObservers(fresh);

        fresh.clear();
        receivedInfos.clear();

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

    private void invokeObservers(List<StationDepartureInfo> receivedInfos) {
        try {
            observers.forEach(observer -> observer.seenUpdate(receivedInfos));
        }
        catch (RuntimeException runtimeException) {
            logger.error("Error invoking observer", runtimeException);
        }
    }

    public int upToDateEntries() {
        return dueTramsRepository.upToDateEntries();
    }

    public int countEntriesWithMessages() {
        return platformMessageRepository.countEntriesWithMessages();
    }

    public long missingDataCount() {
        long upToDateEntries = upToDateEntries();
        long totalSeen = uniquePlatformsSeen.size();

        if (upToDateEntries < totalSeen) {
            logger.error(format("%s out of %s records are within of cuttoff time %s minutes", upToDateEntries, totalSeen, TIME_LIMIT));
        }
        return totalSeen-upToDateEntries;
    }

    public void observeUpdates(LiveDataObserver observer) {
        observers.add(observer);
    }

}
