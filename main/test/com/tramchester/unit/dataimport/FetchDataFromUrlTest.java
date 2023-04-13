package com.tramchester.unit.dataimport;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.*;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.tfgm.TFGMRemoteDataSourceConfig;
import org.apache.http.HttpStatus;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FetchDataFromUrlTest extends EasyMockSupport {

    private HttpDownloadAndModTime httpDownloader;
    private FetchDataFromUrl fetchDataFromUrl;
    private FetchFileModTime fetchFileModTime;
    private Path destinationFile;
    private final URI expectedDownloadURL = URI.create(TestEnv.TFGM_TIMETABLE_URL);
    private ProvidesNow providesLocalNow;
    private DownloadedRemotedDataRepository downloadedDataRepository;
    private LocalDateTime startTime;
    private LocalDateTime expiredFileTime;

    @BeforeEach
    void beforeEachTestRuns() {

        TramchesterConfig config = new LocalTestConfig(Path.of("dataFolder"));

        providesLocalNow = createMock(ProvidesNow.class);
        httpDownloader = createMock(HttpDownloadAndModTime.class);
        fetchFileModTime = createMock(FetchFileModTime.class);
        S3DownloadAndModTime s3Downloader = createMock(S3DownloadAndModTime.class);

        RemoteDataSourceConfig remoteDataSourceConfig = config.getDataRemoteSourceConfig(DataSourceID.tfgm);

        final String targetZipFilename = remoteDataSourceConfig.getDownloadFilename();
        Path path = remoteDataSourceConfig.getDataPath();

        destinationFile = path.resolve(targetZipFilename);

        downloadedDataRepository = new DownloadedRemotedDataRepository();
        fetchDataFromUrl = new FetchDataFromUrl(httpDownloader, s3Downloader, config, providesLocalNow, downloadedDataRepository, fetchFileModTime);


        startTime = LocalDateTime.now();
        expiredFileTime = startTime.minus(remoteDataSourceConfig.getDefaultExpiry()).minusDays(1);

    }


    @Test
    void shouldHandleNoModTimeIsAvailableByDownloadingIfExpiryTimePast() throws IOException, InterruptedException {
        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(startTime);

        EasyMock.expect(fetchFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(fetchFileModTime.getFor(destinationFile)).andReturn(expiredFileTime);

        URLStatus status = new URLStatus(expectedDownloadURL, 200);
        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, expiredFileTime)).andReturn(status);
        EasyMock.expect(httpDownloader.downloadTo(destinationFile, expectedDownloadURL, expiredFileTime)).andReturn(status);
        EasyMock.expectLastCall();

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(destinationFile, downloadedDataRepository.fileFor(DataSourceID.tfgm));

    }


    @Test
    void shouldFetchIfModTimeIsNewer() throws IOException, InterruptedException {

        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(startTime);

        EasyMock.expect(fetchFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(fetchFileModTime.getFor(destinationFile)).andReturn(startTime);

        URLStatus status = new URLStatus(expectedDownloadURL, 200, startTime.plusMinutes(30));
        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, startTime)).andReturn(status);
        EasyMock.expect(httpDownloader.downloadTo(destinationFile, expectedDownloadURL, startTime)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(destinationFile, downloadedDataRepository.fileFor(DataSourceID.tfgm));
    }

    @Test
    void shouldFetchIfLocalFileNotPresent() throws IOException, InterruptedException {

        EasyMock.expect(fetchFileModTime.exists(destinationFile)).andReturn(false);

        URLStatus status = new URLStatus(expectedDownloadURL, 200, startTime);
        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, LocalDateTime.MIN)).andReturn(status);

        EasyMock.expect(httpDownloader.downloadTo(destinationFile, expectedDownloadURL, LocalDateTime.MIN)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(destinationFile, downloadedDataRepository.fileFor(DataSourceID.tfgm));
    }

    @Test
    void shouldNotFetchIfModTimeIsNotNewer() throws IOException, InterruptedException {

        EasyMock.expect(fetchFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(fetchFileModTime.getFor(destinationFile)).andReturn(startTime);

        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(LocalDateTime.now());
        URLStatus status = new URLStatus(expectedDownloadURL, 200, startTime.minusDays(1));
        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, startTime)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(destinationFile, downloadedDataRepository.fileFor(DataSourceID.tfgm));

    }

    @Test
    void shouldCopeWithRedirectsPermAndTemp() throws IOException, InterruptedException {

        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(startTime);

        LocalDateTime modTime = startTime.plusMinutes(1);

        EasyMock.expect(fetchFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(fetchFileModTime.getFor(destinationFile)).andReturn(modTime);

        URI redirectUrl1 = URI.create("https://resource.is.always.now.com/resource");
        URI redirectUrl2 = URI.create("https://resource.is.temp.now.com/resource");

        LocalDateTime time = TestEnv.LocalNow().plusMinutes(1);

        URLStatus status1 = new URLStatus(redirectUrl1, HttpStatus.SC_MOVED_PERMANENTLY);
        URLStatus status2 = new URLStatus(redirectUrl2, HttpStatus.SC_MOVED_TEMPORARILY);
        URLStatus status3 = new URLStatus(redirectUrl2, 200, time);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, modTime)).andReturn(status1);
        EasyMock.expect(httpDownloader.getStatusFor(redirectUrl1, modTime)).andReturn(status2);
        EasyMock.expect(httpDownloader.getStatusFor(redirectUrl2, modTime)).andReturn(status3);

        EasyMock.expect(httpDownloader.downloadTo(destinationFile, redirectUrl2, modTime)).andReturn(status3);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(destinationFile, downloadedDataRepository.fileFor(DataSourceID.tfgm));

    }

    @Test
    void shouldCopeWithRedirectsOnDownloadPermAndTemp() throws IOException, InterruptedException {

        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(startTime);

        LocalDateTime fileModTime = expiredFileTime;

        EasyMock.expect(fetchFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(fetchFileModTime.getFor(destinationFile)).andReturn(fileModTime);

        URI fromConfigURL = URI.create("https://resource.confifured.com/resource");
        URI redirectedURL = URI.create("https://resource.is.temp.moved.com/resource");

        LocalDateTime remoteModTime = startTime.plusMinutes(2);

        // sometimes see 200 from a server for HEAD but then a redirect for the GET (!)
        URLStatus initialHeadStatus = new URLStatus(fromConfigURL, HttpStatus.SC_OK);
        URLStatus firstGetStatus = new URLStatus(redirectedURL, HttpStatus.SC_MOVED_TEMPORARILY);
        URLStatus secondHeadStatus = new URLStatus(redirectedURL, 200, remoteModTime);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, fileModTime)).andReturn(initialHeadStatus);

        EasyMock.expect(httpDownloader.downloadTo(destinationFile, fromConfigURL, fileModTime)).andReturn(firstGetStatus);
        EasyMock.expect(httpDownloader.downloadTo(destinationFile, redirectedURL, fileModTime)).andReturn(secondHeadStatus);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();

        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(destinationFile, downloadedDataRepository.fileFor(DataSourceID.tfgm));

    }

    @Test
    void shouldHandleTooManyDirectsByThrowing() throws IOException, InterruptedException {
        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(startTime);

        LocalDateTime fileModTime = expiredFileTime;

        EasyMock.expect(fetchFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(fetchFileModTime.getFor(destinationFile)).andReturn(fileModTime);

        URI redirectedURL = URI.create("https://resource.is.temp.moved.com/resource");

        // sometimes see 200 from a server for HEAD but then a redirect for the GET (!)
        URLStatus redirect = new URLStatus(redirectedURL, HttpStatus.SC_MOVED_TEMPORARILY);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, fileModTime)).andReturn(redirect);
        EasyMock.expect(httpDownloader.getStatusFor(redirectedURL, fileModTime)).andStubReturn(redirect);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();

    }

    @Test
    void shouldCopeWithRedirects307() throws IOException, InterruptedException {

        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(startTime);

        LocalDateTime modTime = startTime.plusMinutes(1);

        EasyMock.expect(fetchFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(fetchFileModTime.getFor(destinationFile)).andReturn(modTime);

        URI redirectUrl = URI.create("https://resource.is.always.now.com/resource");

        LocalDateTime time = TestEnv.LocalNow().plusMinutes(1);

        URLStatus status1 = new URLStatus(redirectUrl, HttpStatus.SC_TEMPORARY_REDIRECT);
        URLStatus status2 = new URLStatus(redirectUrl, 200, time);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, modTime)).andReturn(status1);
        EasyMock.expect(httpDownloader.getStatusFor(redirectUrl, modTime)).andReturn(status2);

        EasyMock.expect(httpDownloader.downloadTo(destinationFile, redirectUrl, modTime)).andReturn(status2);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(destinationFile, downloadedDataRepository.fileFor(DataSourceID.tfgm));

    }

    @Test
    void shouldHandleNoModTimeIsAvailableByNotDownloadingIfExpiryOK() throws IOException, InterruptedException {

        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(startTime);

        LocalDateTime modTime = startTime.plusMinutes(1);

        EasyMock.expect(fetchFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(fetchFileModTime.getFor(destinationFile)).andReturn(modTime);

        URLStatus status = new URLStatus(expectedDownloadURL, 200);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, modTime)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertTrue(downloadedDataRepository.hasFileFor(DataSourceID.tfgm));
    }

    @Test
    void shouldHandleNoModTimeIsAvailableByDownloadingWhenExpired() throws IOException, InterruptedException {

        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(startTime);

        EasyMock.expect(fetchFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(fetchFileModTime.getFor(destinationFile)).andReturn(expiredFileTime);

        URLStatus status = new URLStatus(expectedDownloadURL, 200);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, expiredFileTime)).andReturn(status);
        EasyMock.expect(httpDownloader.downloadTo(destinationFile, expectedDownloadURL, expiredFileTime)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertTrue(downloadedDataRepository.hasFileFor(DataSourceID.tfgm));
    }

    @Test
    void shouldHandleNoModTimeIsAvailableByStillTryingDownloadWhenNoLocalFile() throws IOException, InterruptedException {

        EasyMock.expect(fetchFileModTime.exists(destinationFile)).andReturn(false);

        URLStatus statusWithoutValidModTime = new URLStatus(expectedDownloadURL, 200);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, LocalDateTime.MIN)).andReturn(statusWithoutValidModTime);
        EasyMock.expect(httpDownloader.downloadTo(destinationFile, expectedDownloadURL, LocalDateTime.MIN)).andReturn(statusWithoutValidModTime);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertTrue(downloadedDataRepository.hasFileFor(DataSourceID.tfgm));
    }

    @Test
    void shouldHandleNoModTimeIsAvailableByStillTryingDownloadWhenNoLocalFileDownloadFails() throws IOException, InterruptedException {

        EasyMock.expect(fetchFileModTime.exists(destinationFile)).andReturn(false);

        URLStatus statusWithoutValidModTime = new URLStatus(expectedDownloadURL, 200);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, LocalDateTime.MIN)).andReturn(statusWithoutValidModTime);
        EasyMock.expect(httpDownloader.downloadTo(destinationFile, expectedDownloadURL, LocalDateTime.MIN)).
                andReturn(new URLStatus(expectedDownloadURL, HttpStatus.SC_NOT_FOUND));

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertFalse(downloadedDataRepository.hasFileFor(DataSourceID.tfgm));
    }

    @Test
    void shouldHandleRemoteIs404NoLocalFile() throws IOException, InterruptedException {

        EasyMock.expect(fetchFileModTime.exists(destinationFile)).andReturn(false);

        URLStatus status = new URLStatus(expectedDownloadURL, 404);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, LocalDateTime.MIN)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(downloadedDataRepository.hasFileFor(DataSourceID.tfgm));

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
