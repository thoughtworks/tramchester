package com.tramchester.unit.liveData;

import com.tramchester.cloud.data.DownloadsLiveData;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.presentation.DTO.StationDepartureInfoDTO;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.livedata.CountsUploadedLiveData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestLiveDataConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

class CountsUploadedLiveDataTest extends EasyMockSupport {

    private LocalDateTime checkTime;
    private ProvidesLocalNow providesLocalNow;
    private DownloadsLiveData downloadsLiveData;
    private CountsUploadedLiveData countsUploadedLiveData;
    private Duration expectedDuration;

    @BeforeEach
    void beforeEachTest() {
        checkTime = TestEnv.LocalNow();
        providesLocalNow = createMock(ProvidesLocalNow.class);
        downloadsLiveData = createMock(DownloadsLiveData.class);

        countsUploadedLiveData = new CountsUploadedLiveData(downloadsLiveData, providesLocalNow);
        expectedDuration = Duration.of(5, SECONDS);
    }

    @Test
    void shouldCountUpToDateDataIsInS3() {

        StationDepartureInfoDTO item = new StationDepartureInfoDTO();
        List<StationDepartureInfoDTO> liveData = Collections.singletonList(item);
        Stream<StationDepartureInfoDTO> liveDataSteam = liveData.stream();

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

        StationDepartureInfoDTO item = new StationDepartureInfoDTO();
        List<StationDepartureInfoDTO> liveData = Collections.singletonList(item);
        Stream<StationDepartureInfoDTO> liveDataSteam = liveData.stream();

        EasyMock.expect(providesLocalNow.getDateTime()).andStubReturn(checkTime);
        EasyMock.expect(downloadsLiveData.downloadFor(checkTime, Duration.of(1, SECONDS)))
                .andReturn(liveDataSteam);

        replayAll();
        long result = countsUploadedLiveData.countNow();
        verifyAll();

        assertEquals(1, result);

    }
}
