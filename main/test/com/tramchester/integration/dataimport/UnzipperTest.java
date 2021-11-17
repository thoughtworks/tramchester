package com.tramchester.integration.dataimport;

import com.tramchester.dataimport.loader.TransportDataReader;
import com.tramchester.dataimport.Unzipper;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

class UnzipperTest {

    private Path zipFilename;
    private Path targetDirectory;
    private Path unpackedDir;

    @BeforeEach
    void beforeEachTestRuns() throws IOException {
        zipFilename = Paths.get("testData", "data.zip");
        targetDirectory = Paths.get(FileUtils.getTempDirectoryPath(),"unpackTarget");
        unpackedDir = targetDirectory.resolve("gtdf-out");
        cleanOutputFiles();
    }

    @AfterEach
    void afterEachTestRuns() throws IOException {
        cleanOutputFiles();
    }

    private void cleanOutputFiles() throws IOException {
        if (Files.exists(unpackedDir)) {
            FileUtils.cleanDirectory(unpackedDir.toFile());
        }
        FileUtils.deleteDirectory(unpackedDir.toFile());
        FileUtils.deleteDirectory(targetDirectory.toFile());
    }

    @Test
    void shouldUnzipFileToExpectedPlaced() {
        Unzipper unzipper = new Unzipper();

        unzipper.unpack(zipFilename, targetDirectory);
        Assertions.assertTrue(Files.isDirectory(targetDirectory));
        Assertions.assertTrue(Files.isDirectory(unpackedDir));

        List<TransportDataReader.InputFiles> files = Arrays.asList(TransportDataReader.InputFiles.values());
        files.forEach(file -> Assertions.assertTrue(Files.isRegularFile(formFilename(file)), file.name()));

    }

    private Path formFilename(TransportDataReader.InputFiles dataFile) {
        return unpackedDir.resolve(dataFile.name() +".txt");
    }

}
