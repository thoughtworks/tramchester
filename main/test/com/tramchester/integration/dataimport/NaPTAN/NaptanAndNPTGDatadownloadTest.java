package com.tramchester.integration.dataimport.NaPTAN;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.RemoteDataRefreshed;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.dataimport.nptg.NPTGDataLoader;
import com.tramchester.domain.DataSourceID;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithNaptan;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class NaptanAndNPTGDatadownloadTest {
    private static GuiceContainerDependencies componentContainer;
    private static TramchesterConfig testConfig;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationTramTestConfigWithNaptan();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        componentContainer.close();
    }

    @Test
    void shouldHaveExpectedDownloadFilePresentForNaptan() {
        final DataSourceID sourceID = DataSourceID.naptanxml;

        Path result = getDownloadedFileFor(sourceID);

        assertTrue(Files.exists(result));

        RemoteDataSourceConfig config = testConfig.getDataRemoteSourceConfig(sourceID);

        Path expected = config.getDataPath().resolve("NaPTAN.xml");
        assertEquals(expected, result);

    }

    @Test
    void shouldHaveExpectedDownloadFilePresentForNPTG() {
        final DataSourceID sourceID = DataSourceID.nptg;

        RemoteDataSourceConfig config = testConfig.getDataRemoteSourceConfig(sourceID);
        final Path configDataPath = config.getDataPath();

        Path result = getDownloadedFileFor(sourceID);

        assertTrue(Files.exists(result));

        Path expected = configDataPath.resolve("Localities.csv");
        assertEquals(expected, result);

        FileFilter filter = pathname -> pathname.getName().toLowerCase().endsWith(".csv");
        final File[] fileArray = configDataPath.toFile().listFiles(filter);
        assertNotNull(fileArray);

        Set<String> names = Arrays.stream(fileArray).map(File::getName).collect(Collectors.toSet());

        assertEquals(1, names.size(), "Unexpected number of files " + names);
        assertTrue(names.contains(NPTGDataLoader.LOCALITIES_CSV), NPTGDataLoader.LOCALITIES_CSV + " is missing");

    }

    private Path getDownloadedFileFor(DataSourceID sourceID) {
        UnzipFetchedData unzipFetchedData = componentContainer.get(UnzipFetchedData.class);
        unzipFetchedData.getReady();

        RemoteDataRefreshed dataRefreshed = componentContainer.get(RemoteDataRefreshed.class);

        assertTrue(dataRefreshed.hasFileFor(sourceID));

        return dataRefreshed.fileFor(sourceID);
    }


}
