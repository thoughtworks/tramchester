package com.tramchester.unit.liveData;

import com.tramchester.livedata.cloud.DownloadsLiveDataFromS3;
import com.tramchester.livedata.domain.DTO.StationDepartureInfoDTO;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.livedata.cloud.CountsUploadedLiveData;
import com.tramchester.livedata.domain.DTO.archived.ArchivedStationDepartureInfoDTO;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

class CountsUploadedLiveDataTest extends EasyMockSupport {

    private LocalDateTime checkTime;
    private ProvidesLocalNow providesLocalNow;
    private DownloadsLiveDataFromS3 downloadsLiveData;
    private CountsUploadedLiveData countsUploadedLiveData;
    private Duration expectedDuration;

    @BeforeEach
    void beforeEachTest() {
        checkTime = TestEnv.LocalNow();
        providesLocalNow = createMock(ProvidesLocalNow.class);
        downloadsLiveData = createMock(DownloadsLiveDataFromS3.class);

        countsUploadedLiveData = new CountsUploadedLiveData(downloadsLiveData, providesLocalNow);
        expectedDuration = Duration.of(5, SECONDS);
    }

    @Test
    void shouldCountUpToDateDataIsInS3() {

        ArchivedStationDepartureInfoDTO item = new ArchivedStationDepartureInfoDTO();
        List<ArchivedStationDepartureInfoDTO> liveData = Collections.singletonList(item);
        Stream<ArchivedStationDepartureInfoDTO> liveDataSteam = liveData.stream();

        EasyMock.expect(downloadsLiveData.downloadFor(checkTime, expectedDuration))
                .andReturn(liveDataSteam);

        replayAll();
        long result = countsUploadedLiveData.count(checkTime, expectedDuration);
        verifyAll();

        assertEquals(1, result);

    }

    @Test
    void shouldCountZeroIfNoDataFound() {
        EasyMock.expect(downloadsLiveData.downloadFor(checkTime, expectedDuration))
                .andReturn(Stream.empty());

        replayAll();
        long result = countsUploadedLiveData.count(checkTime, expectedDuration);
        verifyAll();

        assertEquals(0, result);
    }

    @Test
    void shouldCountNow() {

        ArchivedStationDepartureInfoDTO item = new ArchivedStationDepartureInfoDTO();
        List<ArchivedStationDepartureInfoDTO> liveData = Collections.singletonList(item);
        Stream<ArchivedStationDepartureInfoDTO> liveDataSteam = liveData.stream();

        EasyMock.expect(providesLocalNow.getDateTime()).andStubReturn(checkTime);
        EasyMock.expect(downloadsLiveData.downloadFor(checkTime, Duration.of(1, MINUTES)))
                .andReturn(liveDataSteam);

        replayAll();
        long result = countsUploadedLiveData.countNow();
        verifyAll();

        assertEquals(1, result);

    }
}
