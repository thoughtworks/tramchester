package com.tramchester.integration.dataimport;

import com.tramchester.dataimport.URLDownloadAndModTime;
import com.tramchester.testSupport.TestEnv;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

class URLDownloaderTest {

    private Path temporaryFile;

    @BeforeEach
    void beforeEachTestRuns() {
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

        URLDownloadAndModTime urlDownloader = new URLDownloadAndModTime();

        LocalDateTime modTime = urlDownloader.getModTime(url);
        Assertions.assertTrue(modTime.isBefore(TestEnv.LocalNow()));
        Assertions.assertTrue(modTime.isAfter(LocalDateTime.of(2000,1,1,12,59,22)));

        urlDownloader.downloadTo(temporaryFile, url);

        Assertions.assertTrue(temporaryFile.toFile().exists());
        Assertions.assertTrue(temporaryFile.toFile().length()>0);

    }
}
