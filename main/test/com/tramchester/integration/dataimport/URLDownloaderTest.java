package com.tramchester.integration.dataimport;

import com.tramchester.config.DownloadConfig;
import com.tramchester.dataimport.URLDownloader;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static junit.framework.TestCase.assertTrue;

public class URLDownloaderTest {

    private Path temporaryFile;

    @Before
    public void beforeEachTestRuns() {
        temporaryFile = Paths.get(FileUtils.getTempDirectoryPath(), "downloadAFile");
        tidyFile();
    }

    @After
    public void afterEachTestRuns() {
        tidyFile();
    }

    private void tidyFile() {
        if (temporaryFile.toFile().exists()) {
            temporaryFile.toFile().delete();
        }
    }

    @Test
    public void shouldDownloadSomething() throws IOException {
        DownloadConfig downloadConfig = new DownloadConfig() {
            @Override
            public String getTramDataUrl() {
                return "https://github.com/fluidicon.png";
            }

            @Override
            public Path getDataPath() {
                return null;
            }

            @Override
            public Path getUnzipPath() {
                return null;
            }
        };

        URLDownloader urlDownloader = new URLDownloader(downloadConfig);

        LocalDateTime modTime = urlDownloader.getModTime();
        assertTrue(modTime.isBefore(LocalDateTime.now()));
        assertTrue(modTime.isAfter(LocalDateTime.of(2000,1,1,12,59,22)));

        urlDownloader.downloadTo(temporaryFile);

        assertTrue(temporaryFile.toFile().exists());
        assertTrue(temporaryFile.toFile().length()>0);

    }
}
