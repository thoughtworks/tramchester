package com.tramchester.integration.dataimport.NaPTAN;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.RemoteDataRefreshed;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.domain.DataSourceID;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithNaptan;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NaptanDatadownloadTest {
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
    void shouldHaveExpectedDownloadFilePresentOriginalURL() {
        final DataSourceID sourceID = DataSourceID.naptanxml;

        UnzipFetchedData unzipFetchedData = componentContainer.get(UnzipFetchedData.class);
        unzipFetchedData.getReady();

        RemoteDataRefreshed dataRefreshed = componentContainer.get(RemoteDataRefreshed.class);

        assertTrue(dataRefreshed.hasFileFor(sourceID));

        Path result = dataRefreshed.fileFor(sourceID);

        assertTrue(Files.exists(result));

        RemoteDataSourceConfig config = testConfig.getDataRemoteSourceConfig(sourceID);

        Path expected = config.getDataPath().resolve("NaPTAN.xml");

        assertEquals(expected, result);
    }


}
