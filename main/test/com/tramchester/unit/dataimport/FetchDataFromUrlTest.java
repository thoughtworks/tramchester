package com.tramchester.unit.dataimport;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.URLDownloadAndModTime;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.integration.testSupport.tfgm.TFGMRemoteDataSourceConfig;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FetchDataFromUrlTest extends EasyMockSupport {

    private URLDownloadAndModTime downloader;
    private FetchDataFromUrl fetchDataFromUrl;
    private Path zipFilename;
    private final String expectedDownloadURL = TestEnv.TFGM_TIMETABLE_URL;
    private RemoteDataSourceConfig remoteDataSourceConfig;
    private ProvidesNow providesLocalNow;

    @BeforeEach
    void beforeEachTestRuns() throws IOException {

        TramchesterConfig config = new LocalTestConfig(java.nio.file.Files.createTempDirectory("FetchDataFromUrlTest"));

        providesLocalNow = createMock(ProvidesNow.class);
        downloader = createMock(URLDownloadAndModTime.class);
        remoteDataSourceConfig = config.getRemoteDataSourceConfig().get(0);
        final String targetZipFilename = remoteDataSourceConfig.getDownloadFilename();
        Path path = remoteDataSourceConfig.getDataPath();
        zipFilename = path.resolve(targetZipFilename);

        fetchDataFromUrl = new FetchDataFromUrl(downloader, config, providesLocalNow);

        removeTmpFile();
    }

    @AfterEach
    void removeTmpDir() throws IOException {
        removeTmpFile();
        Path tmpDir = remoteDataSourceConfig.getDataPath();
        if (java.nio.file.Files.exists(tmpDir)) {
            java.nio.file.Files.delete(tmpDir);
        }
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

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(fetchDataFromUrl.refreshed("intergationTestRemoteSource"));
    }

    @Test
    void shouldFetchIfLocalFileNotPresent() throws IOException {
        downloader.downloadTo(zipFilename, expectedDownloadURL);
        EasyMock.expectLastCall();

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(fetchDataFromUrl.refreshed("intergationTestRemoteSource"));
    }

    @Test
    void shouldNotFetchIfModTimeIsNotNewer() throws IOException {
        Files.newFile(zipFilename.toAbsolutePath().toString());
        LocalDateTime time = TestEnv.LocalNow();
        EasyMock.expect(downloader.getModTime(expectedDownloadURL)).andReturn(time.minusDays(1));

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(fetchDataFromUrl.refreshed("intergationTestRemoteSource"));
    }

    @Test
    void shouldHandleNoModTimeIsAvailableByDownloadingIfExpiryTimePast() throws IOException {
        Files.newFile(zipFilename.toAbsolutePath().toString());
        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(LocalDateTime.now().
                plusMinutes(FetchDataFromUrl.DEFAULT_EXPIRY_MINS).plusDays(1));

        LocalDateTime fileIsMissingTime = LocalDateTime.MIN;
        EasyMock.expect(downloader.getModTime(expectedDownloadURL)).andReturn(fileIsMissingTime);
        downloader.downloadTo(zipFilename, expectedDownloadURL);
        EasyMock.expectLastCall();

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(fetchDataFromUrl.refreshed("intergationTestRemoteSource"));
    }

    @Test
    void shouldHandleNoModTimeIsAvailableByNotDownloadingIfExpiryOK() throws IOException {
        Files.newFile(zipFilename.toAbsolutePath().toString());
        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(LocalDateTime.now());
        LocalDateTime fileIsMissingTime = LocalDateTime.MIN;
        EasyMock.expect(downloader.getModTime(expectedDownloadURL)).andReturn(fileIsMissingTime);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(fetchDataFromUrl.refreshed("intergationTestRemoteSource"));
    }

    @Test
    void shouldHandlerFileIsMissing() throws IOException {
        Files.newFile(zipFilename.toAbsolutePath().toString());
        LocalDateTime fileIsMissingTime = LocalDateTime.MAX;
        EasyMock.expect(downloader.getModTime(expectedDownloadURL)).andReturn(fileIsMissingTime);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
    }

    private static class LocalTestConfig extends TestConfig {
        private final Path dataPath;

        private LocalTestConfig(Path dataPath) {
            this.dataPath = dataPath;
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return null;
        }

        @Override
        public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
            return Collections.singletonList(new TFGMRemoteDataSourceConfig(dataPath));
        }
    }
}
