package com.tramchester.livedata.cloud;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.data.LiveDataClientForS3;
import com.tramchester.cloud.data.S3Keys;
import com.tramchester.cloud.data.StationDepartureMapper;
import com.tramchester.livedata.domain.DTO.archived.ArchivedStationDepartureInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class DownloadsLiveDataFromS3 {
    private static final Logger logger = LoggerFactory.getLogger(DownloadsLiveDataFromS3.class);

    private final LiveDataClientForS3 s3Client;
    private final S3Keys s3Keys;
    private final StationDepartureMapper stationDepartureMapper;

    @Inject
    public DownloadsLiveDataFromS3(LiveDataClientForS3 s3Client, StationDepartureMapper stationDepartureMapper, S3Keys s3Keys) {
        this.s3Client = s3Client;
        this.stationDepartureMapper = stationDepartureMapper;
        this.s3Keys = s3Keys;
    }

    public Stream<ArchivedStationDepartureInfoDTO> downloadFor(LocalDateTime start, Duration duration) {
        logger.info("Download departure info from s3 for " + start + " and duration " + duration.getSeconds() + " seconds");
        final LocalDate end = start.plus(duration).toLocalDate();
        LocalDate current = start.toLocalDate();

        final Set<String> inscopeKeys = new HashSet<>();
        while (current.isBefore(end) || current.equals(end)) {
            String prefix = s3Keys.createPrefix(current);
            Set<String> keys = s3Client.getKeysFor(prefix);
            inscopeKeys.addAll(filteredKeys(start, duration, keys));
            current = current.plusDays(1);
        }

        if (inscopeKeys.isEmpty()) {
            logger.warn(format("Found zero keys for %s and %s seconds", start, duration.getSeconds()));
            return Stream.empty();
        }

        logger.info(format("Found %s keys for %s and %s", inscopeKeys.size(), start, duration));
        return downloadFor(inscopeKeys);

    }

    private Set<String> filteredKeys(LocalDateTime start, Duration duration, Set<String> keys) {
        Set<String> results = new HashSet<>();
        LocalDateTime end = start.plus(duration);

        for (String key : keys) {
            try {
                LocalDateTime dateTime = s3Keys.parse(key);
                if (matchesDate(start, end, dateTime)) {
                    results.add(key);
                }
            }
            catch (S3Keys.S3KeyException exception) {
                logger.warn("Unable to parse key: " + key);
            }
        }

        return results;
    }

    private boolean matchesDate(LocalDateTime start, LocalDateTime end, LocalDateTime query) {
        if (query.isEqual(start) || query.isEqual(end)) {
            return true;
        }
        return (query.isAfter(start) && query.isBefore(end));
    }

    private Stream<ArchivedStationDepartureInfoDTO> downloadFor(final Set<String> keys) {

        return s3Client.downloadAndMap(keys, bytes -> {
            final String text = new String(bytes, StandardCharsets.US_ASCII);
            return stationDepartureMapper.parse(text);
        });

    }
}
