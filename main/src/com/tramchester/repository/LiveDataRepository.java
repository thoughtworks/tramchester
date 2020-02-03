package com.tramchester.repository;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.livedata.LiveDataFetcher;
import com.tramchester.mappers.DeparturesMapper;
import com.tramchester.mappers.LiveDataParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.time.temporal.ChronoField.MINUTE_OF_DAY;

public class LiveDataRepository implements LiveDataSource {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataRepository.class);

    private static final int TIME_LIMIT = 15; // only enrich if data is within this many minutes
    private static final String NO_MESSAGE = "<no message>";

    // platformId -> StationDepartureInfo
    private Map<String, StationDepartureInfo> stationInformation;

    private LiveDataFetcher fetcher;
    private LiveDataParser parser;
    private LocalDateTime lastRefresh;
    private int messageCount;

    private List<LiveDataObserver> observers;

    // TODO inject provider of current time
    public LiveDataRepository(LiveDataFetcher fetcher, LiveDataParser parser) {
        this.fetcher = fetcher;
        this.parser = parser;
        stationInformation = new HashMap<>();
        lastRefresh = LocalDateTime.now();
        observers = new LinkedList<>();
        messageCount = 0;
    }

    public void refreshRespository()  {
        Map<String, StationDepartureInfo> newMap = Collections.emptyMap();

        logger.info("Refresh repository");
        String payload  = fetcher.fetch();
        if (payload.length()>0) {
            messageCount = 0;
            try {
                List<StationDepartureInfo> infos = parser.parse(payload);
                newMap = consumeDepartInfo(infos);
            } catch (ParseException exception) {
                logger.error("Unable to parse received JSON: " + payload, exception);
            }
        }
        if (newMap.isEmpty()) {
            logger.error("Unable to refresh live data from payload: " + payload);
        } else {
            logger.info("Refreshed live data, count is: " + newMap.size());
        }
        stationInformation = newMap;
        lastRefresh = LocalDateTime.now();
        invokeObservers();
    }

    private HashMap<String, StationDepartureInfo> consumeDepartInfo(List<StationDepartureInfo> infos) {
        HashMap<String,StationDepartureInfo> newMap = new HashMap<>();

        infos.forEach(newDepartureInfo -> {
            String platformId = newDepartureInfo.getStationPlatform();
            if (!newMap.containsKey(platformId)) {
                String message = newDepartureInfo.getMessage();
                if (message.equals(NO_MESSAGE)) {
                    newDepartureInfo.clearMessage();
                } else {
                    messageCount = messageCount + 1;
                }
                if (scrollingDisplay(message)) {
                    newDepartureInfo.clearMessage();
                }
                newMap.put(platformId, newDepartureInfo);
            } else {
                StationDepartureInfo existingDepartureInfo = newMap.get(platformId);
                newDepartureInfo.getDueTrams().forEach(dueTram -> {
                    if (!existingDepartureInfo.hasDueTram(dueTram)) {
                        // WARNING because: right now this hardly seems to happen, need some examples to figure
                        // out the right approach, in part because the live api is not really documented
                        logger.warn(format("Additional due tram '%s' seen for platform id '%s'", dueTram, platformId));
                        existingDepartureInfo.addDueTram(dueTram);
                    }
                });
            }
        });
        return newMap;
    }

    private boolean scrollingDisplay(String message) {
        return message.startsWith("^F0Next");
    }

    private void invokeObservers() {
        try {
            observers.forEach(observer -> observer.seenUpdate(stationInformation.values()));
        }
        catch (RuntimeException runtimeException) {
            logger.error("Error invoking observer", runtimeException);
        }
    }

    @Override
    public Optional<StationDepartureInfo> departuresFor(Platform platform, TramServiceDate tramServiceDate, TramTime queryTime) {
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
        station.getPlatforms().forEach(platform -> departuresFor(platform, when, queryTime).ifPresent(results::add));
        return results;
    }

    @Override
    public List<DueTram> dueTramsFor(Station station, TramServiceDate when, TramTime queryTime) {
        return departuresFor(station, when, queryTime).stream().
                map(info -> info.getDueTrams()).
                flatMap(Collection::stream).
                filter(dueTram -> DeparturesMapper.DUE.equals(dueTram.getStatus())).
                collect(Collectors.toList());
    }

    private boolean withinTime(TramTime queryTime, LocalTime updateTime) {
        return Math.abs(queryTime.minutesOfDay()-updateTime.get(MINUTE_OF_DAY)) < TIME_LIMIT;
    }

    public long upToDateEntries(TramTime queryTime) {
        Collection<StationDepartureInfo> infos = stationInformation.values();

        return infos.stream().
                filter(info -> withinTime(queryTime, info.getLastUpdate().toLocalTime())).
                count();
    }

    public int countEntries() {
        return stationInformation.size();
    }

    public int countMessages() {
        return messageCount;
    }

    public long staleDataCount() {
        Collection<StationDepartureInfo> infos = stationInformation.values();
        int total = infos.size();

        LocalDateTime cutoff = lastRefresh.minusMinutes(TIME_LIMIT);
        long withinCutof = infos.stream().filter(info -> info.getLastUpdate().isAfter(cutoff)).count();
        if (withinCutof<total) {
            logger.error(format("%s out of %s records are within of cuttoff time %s", withinCutof, total, cutoff));
        }
        return total-withinCutof;
    }

    @Deprecated
    @Override
    public List<StationDepartureInfo> departuresFor(Station station) {
        List<StationDepartureInfo> result = new LinkedList<>();

        station.getPlatforms().forEach(platform -> {
            String platformId = platform.getId();
            if (stationInformation.containsKey(platformId)) {
                result.add(stationInformation.get(platformId));
            } else {
                logger.error("Unable to find stationInformation for platform " + platform);
            }
        });

        return result;
    }

    @Deprecated
    @Override
    public Optional<StationDepartureInfo> departuresFor(Platform platform) {
        String platformId = platform.getId();

        if (!stationInformation.containsKey(platformId)) {
            logger.warn("Could find departure info for " + platform);
            return Optional.empty();
        }
        return Optional.of(stationInformation.get(platformId));
    }

    @Deprecated
    @Override
    public List<DueTram> dueTramsFor(Station station) {
        return departuresFor(station).stream().
                map(info -> info.getDueTrams()).
                flatMap(Collection::stream).
                filter(dueTram -> DeparturesMapper.DUE.equals(dueTram.getStatus())).
                collect(Collectors.toList());
    }

    public void observeUpdates(LiveDataObserver observer) {
        observers.add(observer);
    }

    public Collection<StationDepartureInfo> allDepartures() {
        return stationInformation.values();
    }

}
