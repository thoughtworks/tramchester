package com.tramchester.livedata.cloud;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.livedata.domain.DTO.archived.ArchivedDepartureDTO;
import com.tramchester.livedata.domain.DTO.archived.ArchivedStationDepartureInfoDTO;

import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class FindUniqueDueTramStatus {
    private final DownloadsLiveDataFromS3 downloader;

    @Inject
    public FindUniqueDueTramStatus(DownloadsLiveDataFromS3 downloader) {
        this.downloader = downloader;
    }

    public Set<String> getUniqueDueTramStatus(LocalDateTime startDate, Duration duration) {
        Stream<String> all = getAllDueStatus(startDate, duration);
        return all.collect(Collectors.toSet());
    }

    private Stream<String> getAllDueStatus(LocalDateTime startDate, Duration duration) {
        Stream<ArchivedStationDepartureInfoDTO> records = downloader.downloadFor(startDate, duration);
        return records.flatMap(this::extractDueStatus);
    }

    private Stream<String> extractDueStatus(ArchivedStationDepartureInfoDTO record) {
        return record.getDueTrams().stream().map(ArchivedDepartureDTO::getStatus);
    }

}
