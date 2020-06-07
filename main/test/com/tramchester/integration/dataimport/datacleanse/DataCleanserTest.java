package com.tramchester.integration.dataimport.datacleanse;

import com.tramchester.dataimport.*;
import com.tramchester.dataimport.datacleanse.DataCleanser;
import com.tramchester.dataimport.datacleanse.TransportDataWriterFactory;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.integration.IntegrationTramTestConfig;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

class DataCleanserTest {

    private final IntegrationTramTestConfig config = new IntegrationTramTestConfig();
    private Path dataOutputFolder;
    private Path unpackedZipPath;
    private List<TransportDataReader.InputFiles> files;

    @BeforeEach
    void beforeAllTestRuns() throws IOException {
        files = Arrays.asList(TransportDataReader.InputFiles.values());
        dataOutputFolder = config.getDataFolder();
        unpackedZipPath = config.getDataFolder().resolve("gtdf-out");
        tidyFiles();
    }

    @AfterEach
    void afterAllTestsRun() throws IOException {
        tidyFiles();
    }

    private void tidyFiles() throws IOException {
        if (unpackedZipPath.toFile().exists()) {
            FileUtils.cleanDirectory(unpackedZipPath.toFile());
            FileUtils.deleteDirectory(unpackedZipPath.toFile());
        }
        files.forEach(file-> {
            Path pathToOutputFile = dataOutputFolder.resolve(file.name()+".txt");
            if (pathToOutputFile.toFile().exists()) {
                pathToOutputFile.toFile().delete();
            }
        });
    }

    @Test
    void cleanseCurrentDataWithNoErrors() throws IOException {

        TransportDataReaderFactory readerFactory = new TransportDataReaderFactory(config);
        TransportDataWriterFactory writerFactory = new TransportDataWriterFactory(config);
        ProvidesNow providesNow = new ProvidesLocalNow();
        DataCleanser dataCleanser = new DataCleanser(readerFactory, writerFactory, providesNow, config);

        URLDownloader downloader = new URLDownloader();
        FetchDataFromUrl fetcher = new FetchDataFromUrl(downloader, config);
        Unzipper unzipper = new Unzipper();
        fetcher.fetchData(unzipper);

        dataCleanser.run();

        Assertions.assertTrue(dataOutputFolder.toFile().exists());
        Assertions.assertTrue(unpackedZipPath.toFile().exists());

        files.forEach(file -> {
            Assertions.assertTrue(unpackedZipPath.resolve(file.name()+".txt").toFile().exists(), file.name());
            Assertions.assertTrue(dataOutputFolder.resolve(file.name()+".txt").toFile().exists(), file.name());
        });

    }

}
