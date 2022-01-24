package com.tramchester.unit.dataimport;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.HttpDownloadAndModTime;
import com.tramchester.dataimport.S3DownloadAndModTime;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.integration.testSupport.tfgm.TFGMRemoteDataSourceConfig;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import org.assertj.core.util.Files;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FetchDataFromUrlTest extends EasyMockSupport {

    private HttpDownloadAndModTime httpDownloader;
    private FetchDataFromUrl fetchDataFromUrl;
    private Path zipFilename;
    private final String expectedDownloadURL = TestEnv.TFGM_TIMETABLE_URL;
    private RemoteDataSourceConfig remoteDataSourceConfig;
    private ProvidesNow providesLocalNow;

    @BeforeEach
    void beforeEachTestRuns() throws IOException {

        TramchesterConfig config = new LocalTestConfig(java.nio.file.Files.createTempDirectory("FetchDataFromUrlTest"));

        providesLocalNow = createMock(ProvidesNow.class);
        httpDownloader = createMock(HttpDownloadAndModTime.class);
        S3DownloadAndModTime s3Downloader = createMock(S3DownloadAndModTime.class);
        remoteDataSourceConfig = config.getRemoteDataSourceConfig().get(0);
        final String targetZipFilename = remoteDataSourceConfig.getDownloadFilename();
        Path path = remoteDataSourceConfig.getDataPath();
        zipFilename = path.resolve(targetZipFilename);

        fetchDataFromUrl = new FetchDataFromUrl(httpDownloader, s3Downloader, config, providesLocalNow);

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

        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(LocalDateTime.now());

        HttpDownloadAndModTime.URLStatus status = new HttpDownloadAndModTime.URLStatus(expectedDownloadURL, 200, time.plusMinutes(30));
        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL)).andReturn(status);
        httpDownloader.downloadTo(zipFilename, expectedDownloadURL);
        EasyMock.expectLastCall();

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(fetchDataFromUrl.refreshed("intergationTestRemoteSource"));
    }

    @Test
    void shouldFetchIfLocalFileNotPresent() throws IOException {

        LocalDateTime time = TestEnv.LocalNow();
        HttpDownloadAndModTime.URLStatus status = new HttpDownloadAndModTime.URLStatus(expectedDownloadURL, 200, time);
        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL)).andReturn(status);

        httpDownloader.downloadTo(zipFilename, expectedDownloadURL);
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
        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(LocalDateTime.now());
        HttpDownloadAndModTime.URLStatus status = new HttpDownloadAndModTime.URLStatus(expectedDownloadURL, 200, time.minusDays(1));
        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(fetchDataFromUrl.refreshed("intergationTestRemoteSource"));
    }

    @Test
    void shouldCopeWithRedirects() throws IOException {
        Files.newFile(zipFilename.toAbsolutePath().toString());

        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(LocalDateTime.now());

        String redirectUrl1 = "https://resource.is.always.now.com/resource";
        String redirectUrl2 = "https://resource.is.temp.now.com/resource";

        LocalDateTime time = TestEnv.LocalNow().plusMinutes(1);

        HttpDownloadAndModTime.URLStatus status1 = new HttpDownloadAndModTime.URLStatus(redirectUrl1, 301);
        HttpDownloadAndModTime.URLStatus status2 = new HttpDownloadAndModTime.URLStatus(redirectUrl2, 302);
        HttpDownloadAndModTime.URLStatus status3 = new HttpDownloadAndModTime.URLStatus(redirectUrl2, 200, time);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL)).andReturn(status1);
        EasyMock.expect(httpDownloader.getStatusFor(redirectUrl1)).andReturn(status2);
        EasyMock.expect(httpDownloader.getStatusFor(redirectUrl2)).andReturn(status3);

        httpDownloader.downloadTo(zipFilename, redirectUrl2);
        EasyMock.expectLastCall();

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(fetchDataFromUrl.refreshed("intergationTestRemoteSource"));
    }

    @Test
    void shouldHandleNoModTimeIsAvailableByDownloadingIfExpiryTimePast() throws IOException {
        Files.newFile(zipFilename.toAbsolutePath().toString());
        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(LocalDateTime.now().
                plusMinutes(FetchDataFromUrl.DEFAULT_EXPIRY_MINS).plusDays(1));

        //LocalDateTime fileIsMissingTime = LocalDateTime.MIN;
        HttpDownloadAndModTime.URLStatus status = new HttpDownloadAndModTime.URLStatus(expectedDownloadURL, 200);
        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL)).andReturn(status);
        httpDownloader.downloadTo(zipFilename, expectedDownloadURL);
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

        //LocalDateTime fileIsMissingTime = LocalDateTime.MIN;
        HttpDownloadAndModTime.URLStatus status = new HttpDownloadAndModTime.URLStatus(expectedDownloadURL, 200);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(fetchDataFromUrl.refreshed("intergationTestRemoteSource"));
    }

    @Test
    void shouldHandlerFileIsMissing() throws IOException {
        Files.newFile(zipFilename.toAbsolutePath().toString());

        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(LocalDateTime.now());
        HttpDownloadAndModTime.URLStatus status = new HttpDownloadAndModTime.URLStatus(expectedDownloadURL, 404);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL)).andReturn(status);

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
