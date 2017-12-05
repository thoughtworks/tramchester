package com.tramchester.repository;

import com.tramchester.domain.Platform;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.livedata.LiveDataFetcher;
import com.tramchester.mappers.LiveDataMapper;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

public class LiveDataRepository {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataRepository.class);

    private HashMap<String,StationDepartureInfo> map;

    private LiveDataFetcher fetcher;
    private LiveDataMapper mapper;

    public LiveDataRepository(LiveDataFetcher fetcher, LiveDataMapper mapper) {
        this.fetcher = fetcher;
        this.mapper = mapper;
        map = new HashMap<>();
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
    }

    public void enrich(Platform platform) {
        String idToFind = platform.getId();
        if (map.containsKey(idToFind)) {
            logger.info("Found live data for " + platform.getId());
            StationDepartureInfo info = map.get(idToFind);
            platform.setDepartureInfo(info);
        } else {
            logger.error("Unable to find live data for platform " + platform.getId());
        }

    }
}
