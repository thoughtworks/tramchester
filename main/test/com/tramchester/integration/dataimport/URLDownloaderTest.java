package com.tramchester.integration.dataimport;

import com.tramchester.dataimport.URLDownloadAndModTime;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.TFGMTestDataSourceConfig;
import com.tramchester.testSupport.TestEnv;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class URLDownloaderTest {

    private Path temporaryFile;
    private URLDownloadAndModTime urlDownloader;

    @BeforeEach
    void beforeEachTestRuns() {
        urlDownloader = new URLDownloadAndModTime();

        temporaryFile = Paths.get(FileUtils.getTempDirectoryPath(), "downloadAFile");
        tidyFile();
    }

    @AfterEach
    void afterEachTestRuns() {
        tidyFile();
    }

    private void tidyFile() {
        if (temporaryFile.toFile().exists()) {
            temporaryFile.toFile().delete();
        }
    }

    @Test
    void shouldDownloadSomething() throws IOException {
        String url = "https://github.com/fluidicon.png";


        LocalDateTime modTime = urlDownloader.getModTime(url);
        assertTrue(modTime.isBefore(TestEnv.LocalNow()));
        assertTrue(modTime.isAfter(LocalDateTime.of(2000,1,1,12,59,22)));

        urlDownloader.downloadTo(temporaryFile, url);

        assertTrue(temporaryFile.toFile().exists());
        assertTrue(temporaryFile.toFile().length()>0);
    }

    @Test
    void shouldHaveValidModTimeForTimetableData() throws IOException {
        TFGMTestDataSourceConfig dataSourceConfig = new TFGMTestDataSourceConfig("folder", GTFSTransportationType.tram, TransportMode.Tram);

        String url = dataSourceConfig.getTramDataUrl();
        LocalDateTime modTime = urlDownloader.getModTime(url);

        assertTrue(modTime.getYear()>1970);
    }
}
