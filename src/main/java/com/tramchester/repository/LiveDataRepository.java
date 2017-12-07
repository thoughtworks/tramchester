package com.tramchester.repository;

import com.tramchester.domain.TimeAsMinutes;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.livedata.LiveDataFetcher;
import com.tramchester.mappers.LiveDataMapper;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

import static java.lang.String.format;

public class LiveDataRepository {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataRepository.class);

    public static final int TIME_LIMIT = 5; // only enrich if data is within this many minutes

    private HashMap<String,StationDepartureInfo> map;

    private LiveDataFetcher fetcher;
    private LiveDataMapper mapper;
    private DateTime lastRefresh;

    public LiveDataRepository(LiveDataFetcher fetcher, LiveDataMapper mapper) {
        this.fetcher = fetcher;
        this.mapper = mapper;
        map = new HashMap<>();
        lastRefresh = DateTime.now();
    }

    public void refreshRespository()  {
        logger.info("Refresh repository");
        HashMap<String,StationDepartureInfo> newMap = new HashMap<>();
        String payload  = fetcher.fetch();
        if (payload.length()>0) {
            try {
                List<StationDepartureInfo> infos = mapper.map(payload);
                infos.forEach(info -> {
                    String platformId = info.getStationPlatform();
                    if (!newMap.containsKey(platformId)) {
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
        map = newMap;
        lastRefresh = DateTime.now();
    }

    public void enrich(PlatformDTO platform, TramServiceDate tramServiceDate, int queryMins) {
        LocalDate queryDate = tramServiceDate.getDate();
        if (!lastRefresh.toLocalDate().equals(queryDate)) {
            logger.info("no data for date, not querying for departure info " + queryDate);
            return;
        }

        String idToFind = platform.getId();
        if (map.containsKey(idToFind)) {
            enrichPlatformIfTimeMatches(platform, queryMins);
        } else {
            logger.error("Unable to find live data for platform " + platform.getId());
        }
    }

    private void enrichPlatformIfTimeMatches(PlatformDTO platform, int queryMins) {
        String platformId = platform.getId();
        logger.info("Found live data for " + platformId);
        StationDepartureInfo info = map.get(platformId);

        DateTime infoLastUpdate = info.getLastUpdate();

        int updateMins = TimeAsMinutes.getMinutes(infoLastUpdate.toLocalTime());

        if (Math.abs(queryMins-updateMins) < TIME_LIMIT) {
            logger.info(format("Adding departure info '%s' for platform %s",info, platform));
            platform.setDepartureInfo(info);
        } else {
            logger.info("Not adding departure into as not within time range");
        }
    }

    public void enrich(LocationDTO locationDTO, DateTime now) {
        if (!locationDTO.hasPlatforms()) {
            return;
        }

        if (!now.toLocalDate().equals(lastRefresh.toLocalDate())) {
            logger.info("no data for date, not querying for departure info " + now.toLocalDate());
            return;
        }

        int minutes = TimeAsMinutes.getMinutes(now.toLocalTime());

        locationDTO.getPlatforms().forEach(platformDTO -> {
            String idToFind = platformDTO.getId();
            if (map.containsKey(idToFind)) {
                enrichPlatformIfTimeMatches(platformDTO, minutes);
            } else {
                logger.error("Unable to find live data for platform " + platformDTO.getId());
            }
        });

    }
}
