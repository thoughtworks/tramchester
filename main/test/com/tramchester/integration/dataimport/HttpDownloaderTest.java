package com.tramchester.integration.dataimport;

import com.tramchester.dataimport.HttpDownloadAndModTime;
import com.tramchester.dataimport.URLStatus;
import com.tramchester.testSupport.TestEnv;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class HttpDownloaderTest {

    private Path temporaryFile;
    private HttpDownloadAndModTime urlDownloader;
    private LocalDateTime localModTime;

    @BeforeEach
    void beforeEachTestRuns() {
        localModTime = LocalDateTime.MIN;
        urlDownloader = new HttpDownloadAndModTime();

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
    void shouldDownloadSomething() throws IOException, InterruptedException {
        String url = "https://github.com/fluidicon.png";

        URLStatus status = urlDownloader.getStatusFor(url, localModTime);
        assertTrue(status.isOk());
        LocalDateTime modTime = status.getModTime();
        assertTrue(modTime.isBefore(TestEnv.LocalNow()));
        assertTrue(modTime.isAfter(LocalDateTime.of(2000,1,1,12,59,22)));

        urlDownloader.downloadTo(temporaryFile, url, modTime);

        assertTrue(temporaryFile.toFile().exists());
        assertTrue(temporaryFile.toFile().length()>0);
    }

    @Test
    void shouldHaveValidModTimeForTimetableData() throws IOException, InterruptedException {

        String url = TestEnv.TFGM_TIMETABLE_URL;
        URLStatus result = urlDownloader.getStatusFor(url, localModTime);

        assertTrue(result.getModTime().getYear()>1970);
    }

    @Test
    void shouldHave404StatusForMissingUrl() throws IOException, InterruptedException {
        String url = "http://www.google.com/nothere";

        URLStatus result = urlDownloader.getStatusFor(url, localModTime);

        assertFalse(result.isOk());
        assertFalse(result.isRedirect());

        assertEquals(404, result.getStatusCode());
    }

    @Test
    void shouldHaveRedirectStatusAndURL() throws IOException, InterruptedException {
        String url = "http://news.bbc.co.uk";

        URLStatus result = urlDownloader.getStatusFor(url, localModTime);

        assertFalse(result.isOk());
        assertTrue(result.isRedirect());

        assertEquals("https://www.bbc.co.uk/news", result.getActualURL());

    }
}
