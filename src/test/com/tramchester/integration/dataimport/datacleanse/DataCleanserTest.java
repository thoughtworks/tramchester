package com.tramchester.integration.dataimport.datacleanse;

import com.tramchester.Dependencies;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.dataimport.ErrorCount;
import com.tramchester.integration.dataimport.FetchDataFromUrl;
import com.tramchester.integration.dataimport.TransportDataFetcher;
import com.tramchester.integration.dataimport.TransportDataReader;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class DataCleanserTest implements TransportDataFetcher {

    private static String path = "testData";
    public static final Path INPUT = Paths.get(path, Dependencies.TFGM_UNZIP_DIR);
    public static final Path OUTPUT = Paths.get(path, "output");
    private static Path dataCleansePath = Paths.get("data","testCleanse");

    @BeforeClass
    public static void beforeAllTestRuns() throws IOException {
        tidyFiles();
        File outputDir = OUTPUT.toFile();
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
    }

    @AfterClass
    public static void afterAllTestsRun() throws IOException {
        tidyFiles();
    }

    private static void tidyFiles() throws IOException {
        FileUtils.deleteDirectory(INPUT.toFile());
        File directory = OUTPUT.toFile();
        if (directory.exists()) {
            FileUtils.cleanDirectory(directory);
        }
        File dataCleanse = dataCleansePath.toFile();
        if (dataCleanse.exists()) {
            FileUtils.deleteDirectory(dataCleanse);
        }
    }

    @Test
    @Ignore("Primarily for performance testing")
    public void shouldCleanseTramData() throws IOException {
        DataCleanser dataCleanser = getDataCleanser();
        IntegrationTramTestConfig integrationTramTestConfig = new IntegrationTramTestConfig();
        dataCleanser.run(integrationTramTestConfig.getAgencies());
    }

    @Test
    public void cleanseCurrentDataWithNoErrors() throws IOException {

        FetchDataFromUrl fetcher = new FetchDataFromUrl(dataCleansePath, "http://odata.tfgm.com/opendata/downloads/TfGMgtfs.zip");
        fetcher.fetchData();
        Dependencies dependencies = new Dependencies();
        Set<String> agencies = new HashSet<>();
        agencies.add("MET");
        dependencies.cleanseData(agencies, dataCleansePath, dataCleansePath);
    }

    private DataCleanser getDataCleanser() throws IOException {
        fetchData();
        TransportDataReader reader = new TransportDataReader(INPUT);
        TransportDataWriterFactory writeFactory = new TransportDataWriterFactory(OUTPUT);
        return new DataCleanser(reader, writeFactory, new ErrorCount());
    }

    @Override
    public void fetchData() throws IOException {
        Path filename = Paths.get(path, "data.zip");
        try {
            ZipFile zipFile = new ZipFile(filename.toFile());
            zipFile.extractAll(path);
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }
}
