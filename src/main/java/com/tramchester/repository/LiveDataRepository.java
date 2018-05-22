package com.tramchester.repository;

import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.livedata.LiveDataFetcher;
import com.tramchester.mappers.LiveDataParser;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static java.lang.String.format;

public class LiveDataRepository {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataRepository.class);

    public static final int TIME_LIMIT = 15; // only enrich if data is within this many minutes

    // some displays don't show normal messages in MessageBoard but instead a list of due trams, so exclude these
    List<String> displaysToExclude = Arrays.asList("303","304","461");

    // platformId -> StationDepartureInfo
    private HashMap<String,StationDepartureInfo> stationInformation;

    private LiveDataFetcher fetcher;
    private LiveDataParser parser;
    private DateTime lastRefresh;

    public LiveDataRepository(LiveDataFetcher fetcher, LiveDataParser parser) {
        this.fetcher = fetcher;
        this.parser = parser;
        stationInformation = new HashMap<>();
        lastRefresh = DateTime.now();
    }

    public void refreshRespository()  {
        logger.info("Refresh repository");
        HashMap<String,StationDepartureInfo> newMap = new HashMap<>();
        String payload  = fetcher.fetch();
        if (payload.length()>0) {
            try {
                List<StationDepartureInfo> infos = parser.parse(payload);
                infos.forEach(info -> {
                    String platformId = info.getStationPlatform();
                    if (!newMap.containsKey(platformId)) {
                        if (displaysToExclude.contains(info.getDisplayId())) {
                            info.clearMessage();
                        }
                        newMap.put(platformId, info);
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
        lastRefresh = DateTime.now();
        checkForDataFresh(newMap.values());
    }

    // log errors is data we are receiving from TFGM contains no up to date data
    private void checkForDataFresh(Collection<StationDepartureInfo> infos) {
        int total = infos.size();
        DateTime cutoff = lastRefresh.minusMinutes(TIME_LIMIT);
        long withinCutof = infos.stream().filter(info -> info.getLastUpdate().isAfter(cutoff)).count();
        if (withinCutof<total) {
            logger.error(format("%s out of %s records are within of cuttoff time %s", withinCutof, total, cutoff));
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

    public void enrich(LocationDTO locationDTO, DateTime current) {
        if (!locationDTO.hasPlatforms()) {
            return;
        }

        if (!current.toLocalDate().equals(lastRefresh.toLocalDate())) {
            logger.warn("no data for date, not querying for departure info " + current.toLocalDate());
            return;
        }

        TramTime queryTime = TramTime.create(current.toLocalTime());

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

        DateTime infoLastUpdate = info.getLastUpdate();

        TramTime updateTime = TramTime.create(infoLastUpdate.toLocalTime());

        if (Math.abs(queryTime.minutesOfDay()-updateTime.minutesOfDay()) < TIME_LIMIT) {
            logger.info(format("Adding departure info '%s' for platform %s",info, platform));
            platform.setDepartureInfo(info);
        } else {
            logger.warn(format("Not adding departure info as not within time range, query at %s, update at %s (%s)",
                    queryTime, updateTime, infoLastUpdate));
        }
    }

    public int count() {
        return stationInformation.size();
    }
}
