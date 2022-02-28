package com.tramchester.livedata.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.data.LiveDataClientForS3;
import com.tramchester.cloud.data.S3Keys;
import com.tramchester.cloud.data.StationDepartureMapper;
import com.tramchester.livedata.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.livedata.domain.DTO.StationDepartureInfoDTO;
import com.tramchester.livedata.repository.LiveDataObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

@LazySingleton
public class UploadsLiveData implements LiveDataObserver {
    private static final Logger logger = LoggerFactory.getLogger(UploadsLiveData.class);

    private final StationDepartureMapper mapper;
    private final LiveDataClientForS3 s3;
    private final S3Keys s3Keys;

    @Inject
    public UploadsLiveData(LiveDataClientForS3 s3, StationDepartureMapper mapper, S3Keys s3Keys) {
        this.s3 = s3;
        this.mapper = mapper;
        this.s3Keys = s3Keys;
    }

    public boolean seenUpdate(Collection<StationDepartureInfo> stationDepartureInfos) {
        if (!s3.isStarted()) {
            logger.warn("S3 client not started, no live data will be archived");
            return false;
        }
        if (stationDepartureInfos.isEmpty()) {
            logger.error("Invoked with zero departures");
            return false;
        }

        List<StationDepartureInfoDTO> dtoToUpload = stationDepartureInfos.stream().
                map(StationDepartureInfoDTO::new).collect(Collectors.toList());
        LocalDateTime timeStamp = extractMostRecent(dtoToUpload);

        try {

            String prefix = s3Keys.createPrefix(timeStamp.toLocalDate());
            String key = s3Keys.create(timeStamp);

            // already uploaded by another instance
            if (s3.itemExists(prefix, key)) {
                return true;
            }

            logger.info("Upload live data to S3");
            String json = mapper.map(dtoToUpload);

            final boolean flag = s3.upload(key, json);
            if (flag) {
                logger.info("Upload done");
            } else {
                logger.warn("Upload failed");
            }
            return flag;

        } catch (JsonProcessingException e) {
            logger.warn("Unable to upload live data to S3",e);
        } catch (DateTimeException dateException) {
            logger.warn(format("Unable to upload live data to S3, timestamp '%s'", timeStamp),dateException);
        }
        return false;
    }

    // can't just use local now as won't be able to detect duplicate entries on S3
    private LocalDateTime extractMostRecent(Collection<StationDepartureInfoDTO> liveData) {
        LocalDateTime latest = LocalDateTime.MIN;
        for (StationDepartureInfoDTO info: liveData) {
            if (info.getLastUpdate().isAfter(latest)) {
                latest = info.getLastUpdate();
            }
        }
        return latest;
    }

}
