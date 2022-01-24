package com.tramchester.integration.dataimport;

import com.tramchester.dataimport.HttpDownloadAndModTime;
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

    @BeforeEach
    void beforeEachTestRuns() {
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
    void shouldDownloadSomething() throws IOException {
        String url = "https://github.com/fluidicon.png";

        HttpDownloadAndModTime.URLStatus status = urlDownloader.getStatusFor(url);
        assertTrue(status.isOk());
        LocalDateTime modTime = status.getModTime();
        assertTrue(modTime.isBefore(TestEnv.LocalNow()));
        assertTrue(modTime.isAfter(LocalDateTime.of(2000,1,1,12,59,22)));

        urlDownloader.downloadTo(temporaryFile, url);

        assertTrue(temporaryFile.toFile().exists());
        assertTrue(temporaryFile.toFile().length()>0);
    }

    @Test
    void shouldHaveValidModTimeForTimetableData() throws IOException {

        String url = TestEnv.TFGM_TIMETABLE_URL;
        HttpDownloadAndModTime.URLStatus result = urlDownloader.getStatusFor(url);

        assertTrue(result.getModTime().getYear()>1970);
    }

    @Test
    void shouldHave404StatusForMissingUrl() throws IOException {
        String url = "http://www.google.com/nothere";

        HttpDownloadAndModTime.URLStatus result = urlDownloader.getStatusFor(url);

        assertFalse(result.isOk());
        assertFalse(result.isRedirect());

        assertEquals(404, result.getStatusCode());
    }

    @Test
    void shouldHaveRedirectStatusAndURL() throws IOException {
        String url = "http://news.bbc.co.uk";

        HttpDownloadAndModTime.URLStatus result = urlDownloader.getStatusFor(url);

        assertFalse(result.isOk());
        assertTrue(result.isRedirect());

        assertEquals("https://www.bbc.co.uk/news", result.getActualURL());

    }
}
