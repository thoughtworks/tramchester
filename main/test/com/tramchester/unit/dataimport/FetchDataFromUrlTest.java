package com.tramchester.unit.dataimport;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.*;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.testSupport.tfgm.TFGMRemoteDataSourceConfig;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.cypher.internal.expressions.functions.E;

import java.io.IOException;
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
    private final String expectedDownloadURL = TestEnv.TFGM_TIMETABLE_URL;
    private RemoteDataSourceConfig remoteDataSourceConfig;
    private ProvidesNow providesLocalNow;
    private DownloadedRemotedDataRepository downloadedDataRepository;
    private LocalDateTime startTime;
    private LocalDateTime expiredFileTime;

    @BeforeEach
    void beforeEachTestRuns() throws IOException {

        TramchesterConfig config = new LocalTestConfig(Path.of("dataFolder"));

        providesLocalNow = createMock(ProvidesNow.class);
        httpDownloader = createMock(HttpDownloadAndModTime.class);
        fetchFileModTime = createMock(FetchFileModTime.class);
        S3DownloadAndModTime s3Downloader = createMock(S3DownloadAndModTime.class);

        remoteDataSourceConfig = config.getDataRemoteSourceConfig(DataSourceID.tfgm);

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
        httpDownloader.downloadTo(destinationFile, expectedDownloadURL, expiredFileTime);
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
        httpDownloader.downloadTo(destinationFile, expectedDownloadURL, startTime);
        EasyMock.expectLastCall();

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

        httpDownloader.downloadTo(destinationFile, expectedDownloadURL, LocalDateTime.MIN);
        EasyMock.expectLastCall();

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
    void shouldCopeWithRedirects() throws IOException, InterruptedException {

        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(startTime);

        LocalDateTime modTime = startTime.plusMinutes(1);

        EasyMock.expect(fetchFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(fetchFileModTime.getFor(destinationFile)).andReturn(modTime);

        String redirectUrl1 = "https://resource.is.always.now.com/resource";
        String redirectUrl2 = "https://resource.is.temp.now.com/resource";

        LocalDateTime time = TestEnv.LocalNow().plusMinutes(1);

        URLStatus status1 = new URLStatus(redirectUrl1, 301);
        URLStatus status2 = new URLStatus(redirectUrl2, 302);
        URLStatus status3 = new URLStatus(redirectUrl2, 200, time);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, modTime)).andReturn(status1);
        EasyMock.expect(httpDownloader.getStatusFor(redirectUrl1, modTime)).andReturn(status2);
        EasyMock.expect(httpDownloader.getStatusFor(redirectUrl2, modTime)).andReturn(status3);

        httpDownloader.downloadTo(destinationFile, redirectUrl2, modTime);
        EasyMock.expectLastCall();

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
        httpDownloader.downloadTo(destinationFile, expectedDownloadURL, expiredFileTime);
        EasyMock.expectLastCall();

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
        httpDownloader.downloadTo(destinationFile, expectedDownloadURL, LocalDateTime.MIN);
        EasyMock.expectLastCall();

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
        httpDownloader.downloadTo(destinationFile, expectedDownloadURL, LocalDateTime.MIN);
        EasyMock.expectLastCall().andThrow(new IOException("could not download"));

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
