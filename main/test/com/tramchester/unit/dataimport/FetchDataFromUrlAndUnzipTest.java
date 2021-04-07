package com.tramchester.unit.dataimport;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchDataFromUrlAndUnzip;
import com.tramchester.dataimport.URLDownloadAndModTime;
import com.tramchester.dataimport.Unzipper;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.TFGMTestDataSourceConfig;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import org.assertj.core.util.Files;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

class FetchDataFromUrlAndUnzipTest extends EasyMockSupport {

    private final Path path = Paths.get(Files.temporaryFolderPath());
    private URLDownloadAndModTime downloader;
    private FetchDataFromUrlAndUnzip fetchDataFromUrlAndUnzip;
    private Path zipFilename;
    private Unzipper unzipper;
    private final String expectedDownloadURL = TestEnv.TFGM_TIMETABLE_URL;

    @BeforeEach
    void beforeEachTestRuns() {
        downloader = createMock(URLDownloadAndModTime.class);
        final String targetZipFilename = "downloadTarget.zip";
        zipFilename = path.resolve(targetZipFilename);
        unzipper = createMock(Unzipper.class);

        GTFSSourceConfig dataSourceConfig = new SourceConfig(path.toString(), targetZipFilename);

        TramchesterConfig config = new TestConfig() {
            @Override
            protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
                return Collections.singletonList(dataSourceConfig);
            }
        };

        fetchDataFromUrlAndUnzip = new FetchDataFromUrlAndUnzip(unzipper, downloader, config);
        removeTmpFile();
    }

    @AfterEach
    void afterEachTestRuns() {
        removeTmpFile();
    }

    private void removeTmpFile() {
        if (zipFilename.toFile().exists()) {
            zipFilename.toFile().delete();
        }
    }

    @Test
    void shouldFetchIfModTimeIsNewer() throws IOException {
        Files.newFile(zipFilename.toAbsolutePath().toString());
        LocalDateTime time = TestEnv.LocalNow();
        EasyMock.expect(downloader.getModTime(expectedDownloadURL)).andReturn(time.plusMinutes(30));
        downloader.downloadTo(zipFilename, expectedDownloadURL);
        EasyMock.expectLastCall();
        EasyMock.expect(unzipper.unpack(zipFilename, path)).andReturn(true);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrlAndUnzip.fetchData());
        verifyAll();
    }

    @Test
    void shouldFetchIfLocalFileNotPresent() throws IOException {
        downloader.downloadTo(zipFilename, expectedDownloadURL);
        EasyMock.expectLastCall();
        EasyMock.expect(unzipper.unpack(zipFilename, path)).andReturn(true);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrlAndUnzip.fetchData());
        verifyAll();
    }

    @Test
    void shouldNotFetchIfModTimeIsNotNewer() throws IOException {
        Files.newFile(zipFilename.toAbsolutePath().toString());
        LocalDateTime time = TestEnv.LocalNow();
        EasyMock.expect(downloader.getModTime(expectedDownloadURL)).andReturn(time.minusDays(1));
        EasyMock.expect(unzipper.unpack(zipFilename, path)).andReturn(true);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrlAndUnzip.fetchData());
        verifyAll();
    }

    @Test
    void shouldHandlerUnexpectedServerUpdateTime() throws IOException {
        Files.newFile(zipFilename.toAbsolutePath().toString());
        LocalDateTime fileIsMissingTime = LocalDateTime.MIN;
        EasyMock.expect(downloader.getModTime(expectedDownloadURL)).andReturn(fileIsMissingTime);

        EasyMock.expect(unzipper.unpack(zipFilename, path)).andReturn(true);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrlAndUnzip.fetchData());
        verifyAll();
    }

    private static class SourceConfig extends TFGMTestDataSourceConfig {

        private final String targetZipFilename;

        public SourceConfig(String dataFolder, String targetZipFilename) {
            super(dataFolder, Collections.singleton(GTFSTransportationType.tram), Collections.singleton(TransportMode.Tram));
            this.targetZipFilename = targetZipFilename;
        }

        @Override
        public String getZipFilename() {
            return targetZipFilename;
        }

    }


}
