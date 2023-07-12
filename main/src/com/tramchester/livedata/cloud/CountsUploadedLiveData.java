package com.tramchester.livedata.cloud;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.livedata.domain.DTO.StationDepartureInfoDTO;
import com.tramchester.livedata.domain.DTO.archived.ArchivedStationDepartureInfoDTO;
import com.tramchester.metrics.HasMetrics;
import com.tramchester.metrics.RegistersMetrics;

import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.MINUTES;

@LazySingleton
public class CountsUploadedLiveData implements HasMetrics {
    private final DownloadsLiveDataFromS3 downloadsLiveData;
    private final ProvidesNow providesNow;

    @Inject
    public CountsUploadedLiveData(DownloadsLiveDataFromS3 downloadsLiveData, ProvidesNow providesNow) {
        this.downloadsLiveData = downloadsLiveData;
        this.providesNow = providesNow;
    }

    public long count(LocalDateTime checkTime, Duration checkDuration) {
        final Stream<ArchivedStationDepartureInfoDTO> results = downloadsLiveData.downloadFor(checkTime, checkDuration);
        final long count = results.count();
        results.close();
        return count;
    }

    public Integer countNow() {
        final long count =  count(providesNow.getDateTime(), Duration.of(1, MINUTES));
        return Math.toIntExact(count);
    }

    @Override
    public void registerMetrics(RegistersMetrics registersMetrics) {
        registersMetrics.add(this, "liveData", "uploadsLastMinute", this::countNow);
    }
}
