package com.tramchester.cloud.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.tramchester.domain.presentation.DTO.StationDepartureInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class DownloadsLiveData {
    private static final Logger logger = LoggerFactory.getLogger(DownloadsLiveData.class);

    private final ClientForS3 s3Client;
    private final S3Keys s3Keys;
    private final StationDepartureMapper mapper;

    public DownloadsLiveData(ClientForS3 s3Client, StationDepartureMapper mapper, S3Keys s3Keys) {
        this.s3Client = s3Client;
        this.mapper = mapper;
        this.s3Keys = s3Keys;
    }

    public List<StationDepartureInfoDTO> downloadFor(LocalDateTime start, Duration duration) {
        logger.info("Download departure info for " + start + " and duration " + duration);
        LocalDate end = start.plus(duration).toLocalDate();
        LocalDate current = start.toLocalDate();

        Set<String> inscopeKeys = new HashSet<>();
        while (current.isBefore(end) || current.equals(end)) {
            String prefix = s3Keys.createPrefix(current);
            Set<String> keys = s3Client.getKeysFor(prefix);
            inscopeKeys.addAll(filtered(start, duration, keys));
            current = current.plusDays(1);
        }

        logger.info(format("Found %s keys for %s and %s", inscopeKeys.size(), start, duration));

        return downloadFor(inscopeKeys);

    }

    private Set<String> filtered(LocalDateTime start, Duration duration, Set<String> keys) {
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

    private List<StationDepartureInfoDTO> downloadFor(Set<String> keys) {
        List<StationDepartureInfoDTO> results = new ArrayList<>();
        Set<LocalDateTime> updatesSeen = new HashSet<>();

        for (String key: keys) {
            String text = s3Client.download(key);
            List<StationDepartureInfoDTO> received = mapper.parse(text);
            logger.info("Read " + received.size() + "records for key: " + key);

            Set<StationDepartureInfoDTO> unique = received.stream().filter(item -> !updatesSeen.contains(item.getLastUpdate())).collect(Collectors.toSet());
            updatesSeen.addAll(unique.stream().map(StationDepartureInfoDTO::getLastUpdate).collect(Collectors.toSet()));
            results.addAll(unique);
        }

        return results;
    }
}
