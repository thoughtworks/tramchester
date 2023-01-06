package com.tramchester.unit.dataimport;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.*;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import org.assertj.core.util.Files;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("Now always require a target file")
class FetchDataFromUrlTestNoTargetFileInConfig extends EasyMockSupport {

    private HttpDownloadAndModTime httpDownloader;
    private FetchDataFromUrl fetchDataFromUrl;
    private Path zipFilename;
    private final String expectedDownloadURL = TestEnv.TFGM_TIMETABLE_URL;
    private RemoteDataSourceConfig remoteDataSourceConfig;
    private ProvidesNow providesLocalNow;
    private DownloadedRemotedDataRepository downloadedDataRepository;

    @BeforeEach
    void beforeEachTestRuns() throws IOException {

        final String targetZipFilename = "TfGMgtfsnew.zip";

        TramchesterConfig config = new LocalTestConfig(java.nio.file.Files.createTempDirectory("FetchDataFromUrlTest"), targetZipFilename);

        providesLocalNow = createMock(ProvidesNow.class);
        httpDownloader = createMock(HttpDownloadAndModTime.class);
        S3DownloadAndModTime s3Downloader = createMock(S3DownloadAndModTime.class);
        remoteDataSourceConfig = config.getDataRemoteSourceConfig(DataSourceID.tfgm);


        Path path = remoteDataSourceConfig.getDataPath();
        zipFilename = path.resolve(targetZipFilename);

        downloadedDataRepository = new DownloadedRemotedDataRepository();

        fetchDataFromUrl = new FetchDataFromUrl(httpDownloader, s3Downloader, config, providesLocalNow, downloadedDataRepository);

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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void removeTmpFile() {
        if (zipFilename.toFile().exists()) {
            zipFilename.toFile().delete();
        }
    }

    @Test
    void shouldFetchIfModTimeIsNewer() throws IOException, InterruptedException {
        Files.newFile(zipFilename.toAbsolutePath().toString());
        LocalDateTime time = TestEnv.LocalNow();

        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(LocalDateTime.now());

        URLStatus status = new URLStatus(expectedDownloadURL, 200, time.plusMinutes(30));
        //status.setFilename("TfGMgtfsnew.zip");
        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, time)).andReturn(status);

        httpDownloader.downloadTo(zipFilename, expectedDownloadURL, time);
        EasyMock.expectLastCall();

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(zipFilename, downloadedDataRepository.fileFor(DataSourceID.tfgm));
    }

    @Test
    void shouldFetchIfLocalFileNotPresent() throws IOException, InterruptedException {

        LocalDateTime time = TestEnv.LocalNow();
        URLStatus status = new URLStatus(expectedDownloadURL, 200, time);
        //status.setFilename("TfGMgtfsnew.zip");
        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, time)).andReturn(status);

        httpDownloader.downloadTo(zipFilename, expectedDownloadURL, time);
        EasyMock.expectLastCall();

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(zipFilename, downloadedDataRepository.fileFor(DataSourceID.tfgm));
    }

    @Test
    void shouldNotFetchIfModTimeIsNotNewer() throws IOException, InterruptedException {
        Files.newFile(zipFilename.toAbsolutePath().toString());
        LocalDateTime time = TestEnv.LocalNow();
        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(LocalDateTime.now());

        URLStatus status = new URLStatus(expectedDownloadURL, 200, time.minusDays(1));
        //status.setFilename("TfGMgtfsnew.zip");
        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, time)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(zipFilename, downloadedDataRepository.fileFor(DataSourceID.tfgm));

    }

    @Test
    void shouldCopeWithRedirects() throws IOException, InterruptedException {
        Files.newFile(zipFilename.toAbsolutePath().toString());

        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(LocalDateTime.now());

        String redirectUrl1 = "https://resource.is.always.now.com/resource";
        String redirectUrl2 = "https://resource.is.temp.now.com/resource/actualFilename.txt";

        Path filename = remoteDataSourceConfig.getDataPath().resolve(zipFilename);

        LocalDateTime time = TestEnv.LocalNow().plusMinutes(1);

        URLStatus status1 = new URLStatus(redirectUrl1, 301);
        URLStatus status2 = new URLStatus(redirectUrl2, 302);
        URLStatus status3 = new URLStatus(redirectUrl2, 200, time);
        //status3.setFilename("actualFilename.txt");

        LocalDateTime localModTime = time;
        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, localModTime)).andReturn(status1);
        EasyMock.expect(httpDownloader.getStatusFor(redirectUrl1, localModTime)).andReturn(status2);
        EasyMock.expect(httpDownloader.getStatusFor(redirectUrl2, localModTime)).andReturn(status3);

        httpDownloader.downloadTo(filename, redirectUrl2, localModTime);
        EasyMock.expectLastCall();

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(filename, downloadedDataRepository.fileFor(DataSourceID.tfgm));

    }

    @Test
    void shouldHandleNoModTimeIsAvailableByDownloadingIfExpiryTimePast() throws IOException, InterruptedException {
        Files.newFile(zipFilename.toAbsolutePath().toString());
        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(LocalDateTime.now().
                plus(remoteDataSourceConfig.getDefaultExpiry()).plusDays(1));

        URLStatus status = new URLStatus(expectedDownloadURL, 200);
        //status.setFilename("TfGMgtfsnew.zip");
        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, LocalDateTime.MIN)).andReturn(status);
        httpDownloader.downloadTo(zipFilename, expectedDownloadURL, LocalDateTime.MIN);
        EasyMock.expectLastCall();

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(zipFilename, downloadedDataRepository.fileFor(DataSourceID.tfgm));
    }

    @Test
    void shouldHandleNoModTimeIsAvailableByNotDownloadingIfExpiryOK() throws IOException, InterruptedException {
        Files.newFile(zipFilename.toAbsolutePath().toString());
        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(LocalDateTime.now());

        //LocalDateTime fileIsMissingTime = LocalDateTime.MIN;
        URLStatus status = new URLStatus(expectedDownloadURL, 200);
        //status.setFilename("TfGMgtfsnew.zip");

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, LocalDateTime.MIN)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertFalse(downloadedDataRepository.hasFileFor(DataSourceID.tfgm));
    }

    @Test
    void shouldHandlerFileIsMissing() throws IOException, InterruptedException {
        //Files.newFile(zipFilename.toAbsolutePath().toString());

        //EasyMock.expect(providesLocalNow.getDateTime()).andReturn(LocalDateTime.now());
        URLStatus status = new URLStatus(expectedDownloadURL, 404);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, LocalDateTime.MIN)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(downloadedDataRepository.hasFileFor(DataSourceID.tfgm));

    }

    private static class LocalTestConfig extends TestConfig {
        private final Path dataPath;
        private final String targetFilename;

        private LocalTestConfig(Path dataPath, String targetFilename) {
            this.dataPath = dataPath;
            this.targetFilename = targetFilename;
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return null;
        }

        @Override
        public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
            return Collections.singletonList(new RemoteDataSourceConfigNoTargetFile(dataPath, targetFilename));
        }

        private static class RemoteDataSourceConfigNoTargetFile extends RemoteDataSourceConfig {
            private final Path dataPath;
            private final String targetFilename;

            public RemoteDataSourceConfigNoTargetFile(Path dataPath, String targetFilename) {
                this.dataPath = dataPath;
                this.targetFilename = targetFilename;
            }

            @Override
            public Path getDataPath() {
                return dataPath;
            }

            @Override
            public String getDataCheckUrl() {
                return TestEnv.TFGM_TIMETABLE_URL;
            }

            @Override
            public String getDataUrl() {
                return TestEnv.TFGM_TIMETABLE_URL;
            }

            @Override
            public Duration getDefaultExpiry() {
                return Duration.ofDays(2);
            }

            @Override
            public String getDownloadFilename() {
                return targetFilename;
            }

            @Override
            public String getName() {
                return "tfgm";
            }

            @Override
            public boolean getIsS3() {
                return false;
            }
        }
    }
}
