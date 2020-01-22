package com.tramchester.repository;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Station;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StationDepartureInfoDTO;
import com.tramchester.livedata.LiveDataFetcher;
import com.tramchester.mappers.LiveDataParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static java.lang.String.format;
import static java.time.temporal.ChronoField.MINUTE_OF_DAY;

public class LiveDataRepository implements LiveDataSource {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataRepository.class);

    public static final int TIME_LIMIT = 15; // only enrich if data is within this many minutes
    public static final String NO_MESSAGE = "<no message>";

    // platformId -> StationDepartureInfo
    private HashMap<String, StationDepartureInfo> stationInformation;

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
        logger.info("Refresh repository");
        HashMap<String,StationDepartureInfo> newMap = new HashMap<>();
        String payload  = fetcher.fetch();
        if (payload.length()>0) {
            messageCount = 0;
            try {
                List<StationDepartureInfo> infos = parser.parse(payload);
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

    private boolean scrollingDisplay(String message) {
        return message.startsWith("^F0Next");
    }

    public void invokeObservers() {
        try {
            observers.forEach(observer -> observer.seenUpdate(stationInformation.values()));
        }
        catch (RuntimeException runtimeException) {
            logger.error("Error invoking observer", runtimeException);
        }
    }

    public void enrich(PlatformDTO platform, TramServiceDate tramServiceDate, TramTime queryTime) {
        LocalDate queryDate = tramServiceDate.getDate();
        if (!lastRefresh.toLocalDate().equals(queryDate)) {
            logger.info("no data for date, not querying for departure info " + queryDate);
            return;
        }

        String platformId = platform.getId();
        if (stationInformation.containsKey(platformId)) {
            enrichPlatformIfTimeMatches(platform, queryTime);
        } else {
            logger.error("Unable to find live data for platform " + platform.getId());
        }
    }

    public void enrich(LocationDTO locationDTO, LocalDateTime current) {
        if (!locationDTO.hasPlatforms()) {
            return;
        }

        if (!current.toLocalDate().equals(lastRefresh.toLocalDate())) {
            logger.warn("no data for date, not querying for departure info " + current.toLocalDate());
            return;
        }

        TramTime queryTime = TramTime.of(current.toLocalTime());

        locationDTO.getPlatforms().forEach(platformDTO -> {
            String idToFind = platformDTO.getId();
            if (stationInformation.containsKey(idToFind)) {
                enrichPlatformIfTimeMatches(platformDTO, queryTime);
            } else {
                logger.error("Unable to find live data for platform " + platformDTO.getId());
            }
        });

    }

    private void enrichPlatformIfTimeMatches(PlatformDTO platform, TramTime queryTime) {
        String platformId = platform.getId();
        logger.info("Found live data for " + platformId);

        StationDepartureInfo info = stationInformation.get(platformId);
        LocalDateTime infoLastUpdate = info.getLastUpdate();

        if (withinTime(queryTime, infoLastUpdate.toLocalTime())) {
            logger.info(format("Adding departure info '%s' for platform %s",info, platform));

            StationDepartureInfoDTO infoDTO = new StationDepartureInfoDTO(info);
            platform.setDepartureInfo(infoDTO);
        } else {
            logger.warn(format("Not adding departure info as not within time range, query at %s, update at %s (%s)",
                    queryTime, infoLastUpdate, infoLastUpdate));
        }
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

    @Override
    public StationDepartureInfo departuresFor(Platform platform) {
        String platformId = platform.getId();

        if (!stationInformation.containsKey(platformId)) {
            logger.warn("Could find departure info for " + platform);
            return null;
        }
        return stationInformation.get(platformId);
    }

    public void observeUpdates(LiveDataObserver observer) {
        observers.add(observer);
    }

    public Collection<StationDepartureInfo> allDepartures() {
        return stationInformation.values();
    }
}
