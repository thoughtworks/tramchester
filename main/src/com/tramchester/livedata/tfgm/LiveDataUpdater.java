package com.tramchester.livedata.tfgm;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.repository.LiveDataCache;
import com.tramchester.livedata.repository.LiveDataObserver;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;

@LazySingleton
public class LiveDataUpdater {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataUpdater.class);

    private static final int TIME_LIMIT = 20; // only enrich if data is within this many minutes

    private final List<LiveDataObserver> observers;
    private final PlatformMessageRepository platformMessageRepository;
    private final LiveDataCache dueTramsRepository;

    private final LiveDataFetcher fetcher;
    private final LiveDataParser parser;
    private final ProvidesNow providesNow;

    @Inject
    public LiveDataUpdater(PlatformMessageRepository platformMessageRepository, TramDepartureRepository tramDepartureRepository,
                           LiveDataFetcher fetcher, LiveDataParser parser, ProvidesNow providesNow) {
        this.platformMessageRepository = platformMessageRepository;
        this.dueTramsRepository = tramDepartureRepository;
        this.fetcher = fetcher;
        this.parser = parser;
        this.providesNow = providesNow;

        observers = new LinkedList<>();
    }

    @PreDestroy
    public void dispose() {
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

        List<StationDepartureInfo> fresh = filterForFreshness(receivedInfos);
        int freshCount = fresh.size();
        String msg = freshCount + " of received " + received + " are fresh";
        if (freshCount > 0) {
            logger.info(msg);
        } else {
            logger.error(msg);
        }

        dueTramsRepository.updateCache(fresh);
        platformMessageRepository.updateCache(fresh);
        if (!fresh.isEmpty()) {
            invokeObservers(fresh);
        }

        fresh.clear();
        receivedInfos.clear();
    }

    @NotNull
    private List<StationDepartureInfo> filterForFreshness(List<StationDepartureInfo> receivedInfos) {
        TramTime now = providesNow.getNowHourMins();
        LocalDate date = providesNow.getDate();
        int stale = 0;

        List<StationDepartureInfo> fresh = new ArrayList<>();
        for (StationDepartureInfo departureInfo : receivedInfos) {
            if (isTimely(departureInfo, date, now)) {
                fresh.add(departureInfo);
            } else {
                stale = stale + 1;
                logger.warn("Received stale departure info " + departureInfo);
            }
        }
        if (stale >0) {
            logger.warn("Received " + stale + " stale messages out of " + receivedInfos.size());
        }
        if (fresh.isEmpty()) {
            logger.warn("Got zero fresh messages");
        }
        return fresh;
    }

    private boolean isTimely(StationDepartureInfo newDepartureInfo, LocalDate date, TramTime now) {
        LocalDate updateDate = newDepartureInfo.getLastUpdate().toLocalDate();
        if (!updateDate.equals(date)) {
            logger.info("Received invalid update, date was " + updateDate);
            return false;
        }
        TramTime updateTime = TramTime.ofHourMins(newDepartureInfo.getLastUpdate().toLocalTime());
        if (TramTime.diffenceAsMinutes(now, updateTime) > TIME_LIMIT) {
            logger.info(format("Received out of date update. Local Now: %s Update: %s ", providesNow.getNowHourMins(), updateTime));
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

    public void observeUpdates(LiveDataObserver observer) {
        observers.add(observer);
    }

}
