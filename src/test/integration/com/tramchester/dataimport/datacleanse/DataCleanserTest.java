package com.tramchester.dataimport.datacleanse;

import com.tramchester.dataimport.TransportDataFetcher;
import com.tramchester.dataimport.TransportDataReader;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class DataCleanserTest implements TransportDataFetcher {

    private static String path = "testData";

    @BeforeClass
    public static void beforeAllTestRuns() throws IOException {
        tidyFiles();
    }

    @AfterClass
    public static void afterAllTestsRun() throws IOException {
        tidyFiles();
    }

    private static void tidyFiles() throws IOException {
        FileUtils.deleteDirectory(new File(path + "/gtdf-out/"));

    }

    @Test
    public void shouldCleanserData() throws IOException {
        TransportDataFetcher fetcher = this;
        TransportDataReader reader = new TransportDataReader(path+ "/gtdf-out/");
        TransportDataWriterFactory writeFactory = new TransportDataWriterFactory(path+"/output");
        DataCleanser dataCleanser = new DataCleanser(fetcher, reader, writeFactory);

        dataCleanser.run(Arrays.asList("MET"));
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
