package com.tramchester.integration.dataimport;

import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.Unzipper;
import com.tramchester.repository.TransportData;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertTrue;

public class UnzipperTest {

    private Path zipFilename;
    private Path targetDirectory;
    private Path unpackedDir;

    @Before
    public void beforeEachTestRuns() throws IOException {
        zipFilename = Paths.get("testData", "data.zip");
        targetDirectory = Paths.get(FileUtils.getTempDirectoryPath(),"unpackTarget");
        unpackedDir = targetDirectory.resolve("gtdf-out");
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

        List<TransportDataReader.InputFiles> files = Arrays.asList(TransportDataReader.InputFiles.values());
        files.forEach(file -> {
            assertTrue(file.name(), Files.isRegularFile(formFilename(file)));
        });

    }

    private Path formFilename(TransportDataReader.InputFiles dataFile) {
        return unpackedDir.resolve(dataFile.name() +".txt");
    }

}
