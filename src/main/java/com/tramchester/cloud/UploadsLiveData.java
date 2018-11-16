package com.tramchester.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.repository.LiveDataObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

public class UploadsLiveData implements LiveDataObserver {
    private static final Logger logger = LoggerFactory.getLogger(UploadsLiveData.class);

    private final ObjectMapper objectMapper;
    private final ClientForS3 s3;

    public UploadsLiveData(ClientForS3 s3) {
        this.s3 = s3;
        objectMapper = new ObjectMapper();
    }

    public boolean seenUpdate(Collection<StationDepartureInfo> liveData) {
        try {
            LocalDateTime timeStamp = extractMostRecent(liveData);

            String environment = System.getenv("PLACE");

            if (environment==null) {
                logger.warn("PLACE is not set");
                environment = "test";
            }

            String date = timeStamp.toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE);
            String time = timeStamp.toLocalTime().format(DateTimeFormatter.ISO_TIME);
            String key = String.format("%s/%s/%s", environment.toLowerCase(), date,time);

            if (s3. keyExists(date, key)) {
                return true;
            }

            logger.info("Upload live data to S3");
            String json = objectMapper.writeValueAsString(liveData);
            logger.debug("JSON to update is " + json);

            return s3.upload(key, json);
        } catch (JsonProcessingException e) {
            logger.warn("Unable to upload live data to S3",e);
        }
        return false;
    }

    // can't just use local now as won't be able to detect duplicate entries on S3
    private LocalDateTime extractMostRecent(Collection<StationDepartureInfo> liveData) {
        LocalDateTime latest = LocalDateTime.MIN;
        for (StationDepartureInfo info: liveData) {
            if (info.getLastUpdate().isAfter(latest)) {
                latest = info.getLastUpdate();
            }
        }
        return latest;
    }
}
