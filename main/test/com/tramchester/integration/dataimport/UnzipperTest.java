package com.tramchester.integration.dataimport;

import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.Unzipper;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.tramchester.Dependencies.TFGM_UNZIP_DIR;
import static junit.framework.TestCase.assertTrue;

public class UnzipperTest {

    private Path zipFilename;
    private Path targetDirectory;
    private Path unpackedDir;

    @Before
    public void beforeEachTestRuns() throws IOException {
        zipFilename = Paths.get("testData", "data.zip");
        targetDirectory = Paths.get(FileUtils.getTempDirectoryPath(),"unpackTarget");
        unpackedDir = targetDirectory.resolve(TFGM_UNZIP_DIR);
        cleanOutputFiles();
    }

    @After
    public void afterEachTestRuns() throws IOException {
        cleanOutputFiles();
        return;
    }

    private void cleanOutputFiles() throws IOException {
        if (Files.exists(unpackedDir)) {
            FileUtils.cleanDirectory(unpackedDir.toFile());
        }
        FileUtils.deleteDirectory(unpackedDir.toFile());
        FileUtils.deleteDirectory(targetDirectory.toFile());
    }

    @Test
    public void shouldUnzipFileToExpectedPlaced() {
        Unzipper unzipper = new Unzipper();

        unzipper.unpack(zipFilename, targetDirectory);
        assertTrue(Files.isDirectory(targetDirectory));
        assertTrue(Files.isDirectory(unpackedDir));

        assertTrue(Files.isRegularFile(formFilename(TransportDataReader.CALENDAR)));
        assertTrue(Files.isRegularFile(formFilename(TransportDataReader.FEED_INFO)));
        assertTrue(Files.isRegularFile(formFilename(TransportDataReader.ROUTES)));
        assertTrue(Files.isRegularFile(formFilename(TransportDataReader.STOP_TIMES)));
        assertTrue(Files.isRegularFile(formFilename(TransportDataReader.STOPS)));
        assertTrue(Files.isRegularFile(formFilename(TransportDataReader.TRIPS)));
    }

    private Path formFilename(String dataFile) {
        return unpackedDir.resolve(dataFile +".txt");
    }

}
