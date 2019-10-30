package com.tramchester.integration.dataimport.datacleanse;

import com.tramchester.App;
import com.tramchester.Dependencies;
import com.tramchester.dataimport.*;
import com.tramchester.dataimport.datacleanse.DataCleanser;
import com.tramchester.dataimport.datacleanse.TransportDataWriterFactory;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import org.apache.commons.io.FileUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertTrue;

public class DataCleanserTest {

    private IntegrationTramTestConfig integrationTramTestConfig = new IntegrationTramTestConfig();
    private Dependencies dependencies;
    private Path dataOutputFolder;
    private Path unpackedZipPath;
    private List<TransportDataReader.InputFiles> files;

    @Before
    public void beforeAllTestRuns() throws IOException {
        files = Arrays.asList(TransportDataReader.InputFiles.values());
        dataOutputFolder = integrationTramTestConfig.getDataFolder();
        unpackedZipPath = integrationTramTestConfig.getDataFolder().resolve(Dependencies.TFGM_UNZIP_DIR);
        dependencies = new Dependencies();
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

        dependencies.initialise(integrationTramTestConfig); // this calls cleanse

        assertTrue(dataOutputFolder.toFile().exists());
        assertTrue(unpackedZipPath.toFile().exists());

        files.forEach(file -> {
            assertTrue(file.name(), unpackedZipPath.resolve(file.name()+".txt").toFile().exists());
            assertTrue(file.name(), dataOutputFolder.resolve(file.name()+".txt").toFile().exists());
        });


    }

}
