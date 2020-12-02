package com.tramchester.unit.dataimport.datacleanse;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.URLDownloadAndModTime;
import com.tramchester.dataimport.Unzipper;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.integration.TFGMTestDataSourceConfig;
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

class FetchDataFromUrlTest extends EasyMockSupport {

    private final Path path = Paths.get(Files.temporaryFolderPath());
    private URLDownloadAndModTime downloader;
    private FetchDataFromUrl fetchDataFromUrl;
    private Path zipFilename;
    private Unzipper unzipper;
    private final String expectedDownloadURL = "http://odata.tfgm.com/opendata/downloads/TfGMgtfs.zip";

    @BeforeEach
    void beforeEachTestRuns() {
        downloader = createMock(URLDownloadAndModTime.class);
        final String targetZipFilename = "downloadTarget.zip";
        zipFilename = path.resolve(targetZipFilename);
        unzipper = createMock(Unzipper.class);

        DataSourceConfig dataSourceConfig = new SourceConfig(path.toString(), targetZipFilename);

        TramchesterConfig config = new TestConfig() {
            @Override
            protected List<DataSourceConfig> getDataSourceFORTESTING() {
                return Collections.singletonList(dataSourceConfig);
            }
        };

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
        EasyMock.expect(unzipper.unpack(zipFilename, path)).andReturn(true);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData(unzipper));
        verifyAll();
    }

    @Test
    void shouldFetchIfLocalFileNotPresent() throws IOException {
        downloader.downloadTo(zipFilename, expectedDownloadURL);
        EasyMock.expectLastCall();
        EasyMock.expect(unzipper.unpack(zipFilename, path)).andReturn(true);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData(unzipper));
        verifyAll();
    }

    @Test
    void shouldNotFetchIfModTimeIsNotNewer() throws IOException {
        Files.newFile(zipFilename.toAbsolutePath().toString());
        LocalDateTime time = TestEnv.LocalNow();
        EasyMock.expect(downloader.getModTime(expectedDownloadURL)).andReturn(time.minusDays(1));
        EasyMock.expect(unzipper.unpack(zipFilename, path)).andReturn(true);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData(unzipper));
        verifyAll();
    }

    private static class SourceConfig extends TFGMTestDataSourceConfig {

        private final String targetZipFilename;

        public SourceConfig(String dataFolder, String targetZipFilename) {
            super(dataFolder, Collections.singleton(GTFSTransportationType.tram));
            this.targetZipFilename = targetZipFilename;
        }

        @Override
        public String getZipFilename() {
            return targetZipFilename;
        }

    }


}
