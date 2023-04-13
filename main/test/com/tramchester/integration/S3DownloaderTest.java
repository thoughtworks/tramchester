package com.tramchester.integration;

import com.tramchester.cloud.data.ClientForS3;
import com.tramchester.dataimport.S3DownloadAndModTime;
import com.tramchester.dataimport.URLStatus;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.S3Test;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

@S3Test
public class S3DownloaderTest {
    private static ClientForS3 clientForS3;

    private Path temporaryFile;
    private S3DownloadAndModTime downloadAndModTime;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        clientForS3 = new ClientForS3();
        clientForS3.start();
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        clientForS3.stop();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        downloadAndModTime = new S3DownloadAndModTime(clientForS3);

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
        URI url = URI.create("s3://tramchester2dist/testing/ForTestSupport.txt");

        LocalDateTime localModTime = LocalDateTime.MIN;
        URLStatus result = downloadAndModTime.getStatusFor(url, localModTime);
        assertTrue(result.isOk());

        LocalDateTime modTime = result.getModTime();
        assertTrue(modTime.isBefore(TestEnv.LocalNow()));
        assertTrue(modTime.isAfter(LocalDateTime.of(2000,1,1,12,59,22)));

        downloadAndModTime.downloadTo(temporaryFile, url, localModTime);

        assertTrue(temporaryFile.toFile().exists());
        assertTrue(temporaryFile.toFile().length()>0);
    }

}
