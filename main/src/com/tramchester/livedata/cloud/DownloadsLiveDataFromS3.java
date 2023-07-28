package com.tramchester.livedata.cloud;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.data.LiveDataClientForS3;
import com.tramchester.cloud.data.S3Keys;
import com.tramchester.cloud.data.StationDepartureMapper;
import com.tramchester.livedata.domain.DTO.archived.ArchivedStationDepartureInfoDTO;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

// Bucket->Environment->Date->Files
// Each file is snapshot of current state of live data

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

        final Set<LiveDataRecordKey> inscopeKeys = new HashSet<>();
        while (current.isBefore(end) || current.equals(end)) {
            String prefix = s3Keys.createPrefix(current);
            Stream<LiveDataRecordKey> keys = getKeysFor(prefix);
            inscopeKeys.addAll(selectByTimeWindow(start, duration, keys));
            current = current.plusDays(1);
        }

        return downloadSelectedKeys(start, duration, inscopeKeys);
    }

    private Stream<LiveDataRecordKey> getKeysFor(String prefix) {
        return s3Client.getKeysFor(prefix).map(this::parse).filter(LiveDataRecordKey::isValid);
    }

    private LiveDataRecordKey parse(String text) {
        try {
            LocalDateTime parsed = s3Keys.parse(text);
            return new LiveDataRecordKey(text, parsed);
        } catch (S3Keys.S3KeyException e) {
            return LiveDataRecordKey.Invalid();
        }
    }

    public Stream<ArchivedStationDepartureInfoDTO> downloadFor(LocalDateTime start, Duration duration, Duration samplePeriod) {
        logger.info("Download departure info from s3 for %s and duration %d seconds sample window %s seconds".
                formatted(start, duration.getSeconds(), samplePeriod.getSeconds()));
        final LocalDate end = start.plus(duration).toLocalDate();
        LocalDate current = start.toLocalDate();

        final Set<LiveDataRecordKey> inscopeKeys = new HashSet<>();
        while (current.isBefore(end) || current.equals(end)) {
            String prefix = s3Keys.createPrefix(current);
            Stream<LiveDataRecordKey> keys = getKeysFor(prefix);
            inscopeKeys.addAll(sampledKeys(start, duration, keys, samplePeriod));
            current = current.plusDays(1);
        }

        return downloadSelectedKeys(start, duration, inscopeKeys);
    }

    private Set<LiveDataRecordKey> sampledKeys(LocalDateTime start, Duration duration, Stream<LiveDataRecordKey> keys, final Duration samplePeriod) {
        final Set<LiveDataRecordKey> sampledKeys = new HashSet<>();

        final LinkedList<LiveDataRecordKey> inDateTimeOrder = selectByTimeWindow(start, duration, keys).stream().
                sorted().collect(Collectors.toCollection(LinkedList::new));

        if (inDateTimeOrder.isEmpty()) {
            logger.warn("No keys found for " + start + " and " + duration);
            return sampledKeys;
        }

        LiveDataRecordKey current = inDateTimeOrder.removeFirst();
        sampledKeys.add(current);
        LocalDateTime endOfSample = current.plus(samplePeriod);
        while (!inDateTimeOrder.isEmpty()) {
            final LocalDateTime beginPeriod = current.time;
            final LocalDateTime endPeriod = endOfSample;
            final Optional<LiveDataRecordKey> liveDataRecordKey = consumeWhile(inDateTimeOrder,
                    key -> key.isBetween(beginPeriod, endPeriod));
            if (liveDataRecordKey.isPresent()) {
                current = liveDataRecordKey.get();
                sampledKeys.add(current);
                endOfSample = current.plus(samplePeriod);
            } else {
                break;
            }
        }

        logger.info(String.format("Found %s that matched %s %s and sample window %s", sampledKeys.size(),
                start, duration, samplePeriod));

        return sampledKeys;
    }

    private Optional<LiveDataRecordKey> consumeWhile(LinkedList<LiveDataRecordKey> list, Function<LiveDataRecordKey, Boolean> predicate) {
        if (list.isEmpty()) {
            return Optional.empty();
        }

        LiveDataRecordKey next = list.removeFirst();
        while (predicate.apply(next)) {
            if (list.isEmpty()) {
                return Optional.empty();
            }
            next = list.removeFirst();
        }
        return Optional.of(next);
    }

    private Stream<ArchivedStationDepartureInfoDTO> downloadSelectedKeys(LocalDateTime start, Duration duration, Set<LiveDataRecordKey> inscopeKeys) {
        if (inscopeKeys.isEmpty()) {
            logger.warn(format("Found zero keys for %s and %s seconds", start, duration.getSeconds()));
            return Stream.empty();
        }

        logger.info(format("Downloading %s keys for %s and %s", inscopeKeys.size(), start, duration));

        Set<String> keysAsText = inscopeKeys.stream().map(key -> key.text).collect(Collectors.toSet());

        return s3Client.downloadAndMap(keysAsText, (key, bytes) -> {
            final String text = new String(bytes, StandardCharsets.US_ASCII);
            return stationDepartureMapper.parse(text);
        });
    }

    private Set<LiveDataRecordKey> selectByTimeWindow(final LocalDateTime start, final Duration duration, Stream<LiveDataRecordKey> keys) {
        final LocalDateTime end = start.plus(duration);
        Set<LiveDataRecordKey> found = keys.filter(key -> matchesRange(start, end, key.time)).collect(Collectors.toSet());
        logger.info("Found " + found.size() + " keys for window " + start + " and " + duration);
        return found;
    }

    private boolean matchesRange(LocalDateTime begin, LocalDateTime end, LocalDateTime date) {
        if (date.isEqual(begin) || date.isEqual(end)) {
            return true;
        }
        return (date.isAfter(begin) && date.isBefore(end));
    }

    private static class LiveDataRecordKey implements Comparable<LiveDataRecordKey> {
        private final String text;
        private final LocalDateTime time;

        private LiveDataRecordKey(String text, LocalDateTime time) {
            this.text = text;
            this.time = time;
        }

        public static LiveDataRecordKey Invalid() {
            return new LiveDataRecordKey("", LocalDateTime.MIN);
        }

        public boolean isValid() {
            return !time.equals(LocalDateTime.MIN);
        }

        @Override
        public int compareTo(@NotNull DownloadsLiveDataFromS3.LiveDataRecordKey other) {
            return time.compareTo(other.time);
        }

        public LocalDateTime plus(Duration samplePeriod) {
            return time.plus(samplePeriod);
        }

        boolean isBetween(final LocalDateTime begin, final LocalDateTime end) {
            return (time.equals(begin) || time.isAfter(begin))  && (time.isBefore(end));
                    //&& (time.equals(end) || time.isBefore(end));
        }
    }


}
