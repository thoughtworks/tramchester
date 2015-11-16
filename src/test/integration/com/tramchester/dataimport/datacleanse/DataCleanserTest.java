package com.tramchester.dataimport.datacleanse;

import com.tramchester.Dependencies;
import com.tramchester.IntegrationTestConfig;
import com.tramchester.dataimport.TransportDataFetcher;
import com.tramchester.dataimport.TransportDataReader;
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

public class DataCleanserTest implements TransportDataFetcher {

    private static String path = "testData";
    public static final Path INPUT = Paths.get(path, Dependencies.TFGM_UNZIP_DIR);
    public static final Path OUTPUT = Paths.get(path, "output");

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
    }

    @Test
    @Ignore("Primarily for performance testing")
    public void shouldCleanseData() throws IOException {
        fetchData();
        TransportDataReader reader = new TransportDataReader(INPUT);
        TransportDataWriterFactory writeFactory = new TransportDataWriterFactory(OUTPUT);
        DataCleanser dataCleanser = new DataCleanser(reader, writeFactory);

        dataCleanser.run(new IntegrationTestConfig());

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
