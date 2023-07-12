package com.tramchester.integration.cloud.data;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.cloud.data.LiveDataClientForS3;
import com.tramchester.livedata.cloud.DownloadsLiveDataFromS3;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.livedata.domain.DTO.archived.ArchivedStationDepartureInfoDTO;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestTramLiveDataConfig;
import com.tramchester.testSupport.testTags.S3Test;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@S3Test
class DownloadsLiveDataFromS3Test {
    private static final int NUM_OF_DISPLAYS = 189;
    private static final String PREFIX = "uat/20200227/";
    private static final int NUM_KEYS_FOR_PREFIX = 7844; // from s3 console, historical so should not change

    private static ComponentContainer componentContainer;
    private DownloadsLiveDataFromS3 downloader;
    private LiveDataClientForS3 clientForS3;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig configuration = new RealBucketConfig(new RealLiveConfig("tramchesterlivedata","uat"));
        componentContainer = new ComponentsBuilder().create(configuration, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTest() {
        downloader = componentContainer.get(DownloadsLiveDataFromS3.class);
        clientForS3 = componentContainer.get(LiveDataClientForS3.class);
    }

    @Test
    void shouldDownloadHistoricalDataMin() {
        LocalDateTime start = LocalDateTime.of(2020, 2, 27, 0, 1);
        Duration duration = Duration.of(1, ChronoUnit.MINUTES);

        List<ArchivedStationDepartureInfoDTO> results = downloader.downloadFor(start, duration).collect(Collectors.toList());

        assertFalse(results.isEmpty());
        int assumedLen = NUM_OF_DISPLAYS * 6;
        assertEquals(assumedLen, results.size());
    }

    @Test
    void shouldDownloadHistoricalDataMinutes() {
        LocalDateTime start = LocalDateTime.of(2020, 2, 27, 10, 0);
        Duration duration = Duration.of(59, ChronoUnit.MINUTES).plusSeconds(59);

        List<ArchivedStationDepartureInfoDTO> results = downloader.downloadFor(start, duration).collect(Collectors.toList());
        assertFalse(results.isEmpty());

        int expectedRecords = NUM_OF_DISPLAYS * 325; // from S3 console prefix search
        assertEquals(expectedRecords, results.size());

        results.forEach(result -> {
            assertTrue(result.getLastUpdate().isAfter(start), result.toString());
        });
    }

    @Test
    void shouldHaveKeysForOneDay() {
        Set<String> keys = clientForS3.getKeysFor(PREFIX);
        assertEquals(NUM_KEYS_FOR_PREFIX, keys.size());
    }

    @Disabled("Too slow, need to change to bulk download")
    @Test
    void shouldDownloadHistoricalData1Day() {
        Set<String> keys = clientForS3.getKeysFor(PREFIX);
        assertEquals(NUM_KEYS_FOR_PREFIX, keys.size());

        LocalDateTime start = LocalDateTime.of(2020, 2, 27, 0, 1);
        Duration duration = Duration.of(1, ChronoUnit.DAYS);

        List<ArchivedStationDepartureInfoDTO> results = downloader.downloadFor(start, duration).collect(Collectors.toList());

        assertFalse(results.isEmpty());
        assertEquals(NUM_OF_DISPLAYS * keys.size(), results.size());
    }

    static class RealBucketConfig extends IntegrationTramTestConfig {
        private final TfgmTramLiveDataConfig liveDataConfig;

        RealBucketConfig(TfgmTramLiveDataConfig liveDataConfig) {
            this.liveDataConfig = liveDataConfig;
        }

        @Override
        public TfgmTramLiveDataConfig getLiveDataConfig() {
            return liveDataConfig;
        }
    }

    static class RealLiveConfig extends TestTramLiveDataConfig {
        private final String bucketName;
        private final String prefix;

        public RealLiveConfig(String bucketName, String prefix) {
            this.bucketName = bucketName;
            this.prefix = prefix;
        }

        @Override
        public String getS3Bucket() {
            return bucketName;
        }

        @Override
        public String getS3Prefix() {
            return prefix;
        }
    }

}
