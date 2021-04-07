package com.tramchester.unit.dataimport;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.URLDownloadAndModTime;
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

class FetchDataFromUrlTest extends EasyMockSupport {

    private URLDownloadAndModTime downloader;
    private FetchDataFromUrl fetchDataFromUrl;
    private Path zipFilename;
    private final String expectedDownloadURL = TestEnv.TFGM_TIMETABLE_URL;

    @BeforeEach
    void beforeEachTestRuns() {
        TramchesterConfig config = TestEnv.GET();

        downloader = createMock(URLDownloadAndModTime.class);
        RemoteDataSourceConfig remoteDataSourceConfig = config.getRemoteDataSourceConfig().get(0);
        final String targetZipFilename = remoteDataSourceConfig.getDownloadFilename();
        Path path = remoteDataSourceConfig.getDataPath();
        zipFilename = path.resolve(targetZipFilename);

        fetchDataFromUrl = new FetchDataFromUrl(downloader, config);

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

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
    }

    @Test
    void shouldFetchIfLocalFileNotPresent() throws IOException {
        downloader.downloadTo(zipFilename, expectedDownloadURL);
        EasyMock.expectLastCall();

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
    }

    @Test
    void shouldNotFetchIfModTimeIsNotNewer() throws IOException {
        Files.newFile(zipFilename.toAbsolutePath().toString());
        LocalDateTime time = TestEnv.LocalNow();
        EasyMock.expect(downloader.getModTime(expectedDownloadURL)).andReturn(time.minusDays(1));

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
    }

    @Test
    void shouldHandlerUnexpectedServerUpdateTime() throws IOException {
        Files.newFile(zipFilename.toAbsolutePath().toString());
        LocalDateTime fileIsMissingTime = LocalDateTime.MIN;
        EasyMock.expect(downloader.getModTime(expectedDownloadURL)).andReturn(fileIsMissingTime);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
    }


}
