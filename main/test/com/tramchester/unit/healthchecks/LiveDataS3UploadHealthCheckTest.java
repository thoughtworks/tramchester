package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.cloud.data.DownloadsLiveData;
import com.tramchester.domain.presentation.DTO.StationDepartureInfoDTO;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.healthchecks.LiveDataS3UploadHealthCheck;
import com.tramchester.testSupport.TestEnv;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveDataS3UploadHealthCheckTest extends EasyMockSupport {

    private LocalDateTime localNow;
    private ProvidesLocalNow providesLocalNow;
    private DownloadsLiveData downloadsLiveData;
    private LiveDataS3UploadHealthCheck healthCheck;

    @BeforeEach
    void beforeEachTest() {
        localNow = TestEnv.LocalNow();
        providesLocalNow = createMock(ProvidesLocalNow.class);
        downloadsLiveData = createMock(DownloadsLiveData.class);
        healthCheck = new LiveDataS3UploadHealthCheck(providesLocalNow, downloadsLiveData);
    }

    @Test
    void shouldReportHealthIfUpToDateDataIsInS3() throws Exception {

        StationDepartureInfoDTO item = new StationDepartureInfoDTO();
        List<StationDepartureInfoDTO> liveData = Collections.singletonList(item);
        Stream<StationDepartureInfoDTO> liveDataSteam = liveData.stream();

        EasyMock.expect(providesLocalNow.getDateTime()).andStubReturn(localNow);
        EasyMock.expect(downloadsLiveData.downloadFor(localNow.minusMinutes(10), Duration.of(10, ChronoUnit.MINUTES)))
                .andReturn(liveDataSteam);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertTrue(result.isHealthy());

    }

    @Test
    void shouldReportUnhealthIfNoDataFound() throws Exception {
        EasyMock.expect(providesLocalNow.getDateTime()).andStubReturn(localNow);
        EasyMock.expect(downloadsLiveData.downloadFor(localNow.minusMinutes(10), Duration.of(10, ChronoUnit.MINUTES)))
                .andReturn(Stream.empty());

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertFalse(result.isHealthy());
    }
}
