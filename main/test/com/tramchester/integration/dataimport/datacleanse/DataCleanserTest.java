package com.tramchester.integration.dataimport.datacleanse;

import com.tramchester.dataimport.*;
import com.tramchester.dataimport.datacleanse.DataCleanser;
import com.tramchester.dataimport.datacleanse.TransportDataWriterFactory;
import com.tramchester.integration.IntegrationTramTestConfig;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertTrue;

public class DataCleanserTest {

    private IntegrationTramTestConfig config = new IntegrationTramTestConfig();
    private Path dataOutputFolder;
    private Path unpackedZipPath;
    private List<TransportDataReader.InputFiles> files;

    @Before
    public void beforeAllTestRuns() throws IOException {
        files = Arrays.asList(TransportDataReader.InputFiles.values());
        dataOutputFolder = config.getDataFolder();
        unpackedZipPath = config.getDataFolder().resolve("gtdf-out");
        tidyFiles();
    }

    @After
    public void afterAllTestsRun() throws IOException {
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
    public void cleanseCurrentDataWithNoErrors() throws IOException {

        TransportDataReaderFactory readerFactory = new TransportDataReaderFactory(config);
        TransportDataWriterFactory writerFactory = new TransportDataWriterFactory(config);
        DataCleanser dataCleanser = new DataCleanser(readerFactory, writerFactory, config);

        URLDownloader downloader = new URLDownloader();
        FetchDataFromUrl fetcher = new FetchDataFromUrl(downloader, config);
        Unzipper unzipper = new Unzipper();
        fetcher.fetchData(unzipper);

        dataCleanser.run();

        assertTrue(dataOutputFolder.toFile().exists());
        assertTrue(unpackedZipPath.toFile().exists());

        files.forEach(file -> {
            assertTrue(file.name(), unpackedZipPath.resolve(file.name()+".txt").toFile().exists());
            assertTrue(file.name(), dataOutputFolder.resolve(file.name()+".txt").toFile().exists());
        });

    }

}
