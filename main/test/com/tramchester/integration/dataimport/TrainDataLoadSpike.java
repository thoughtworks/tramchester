package com.tramchester.integration.dataimport;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.DataLoaderFactory;
import com.tramchester.dataimport.TransportDataFromFileFactory;
import com.tramchester.dataimport.TransportDataLoader;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.IntegrationTrainTestConfig;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.repository.TransportDataSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

@Disabled("spike")
class TrainDataLoadSpike {

    private final Path dataPath = Path.of("data", "gb-rail-latest");

    @Test
    void testShouldLoadTheData() {

        TransportDataLoader provider = () -> {
            DataLoaderFactory factory = new DataLoaderFactory(dataPath, ".txt");
            return new TransportDataReader(factory);
        };
        ProvidesLocalNow providesNow = new ProvidesLocalNow();
        StationLocations stationLocations = new StationLocations(new CoordinateTransforms());

        TransportDataFromFileFactory fileFactory = new TransportDataFromFileFactory(provider, providesNow, stationLocations);

        TransportDataFromFiles transportDataFromFiles = fileFactory.create(false);
        transportDataFromFiles.start();

    }

    @Test
    void shouldInitDependencies() {
        Dependencies dependencies = new Dependencies();
        TramchesterConfig testConfig = new IntegrationTrainTestConfig();

        TransportDataLoader provider = () -> {
            DataLoaderFactory factory = new DataLoaderFactory(dataPath, ".txt");
            return new TransportDataReader(factory);
        };
        ProvidesLocalNow providesNow = new ProvidesLocalNow();
        StationLocations stationLocations = dependencies.get(StationLocations.class);

        TransportDataFromFileFactory fileFactory = new TransportDataFromFileFactory(provider, providesNow, stationLocations);
        TransportDataSource transportDataFromFiles = fileFactory.create(false);

        dependencies.initialise(testConfig, transportDataFromFiles);

        dependencies.close();
    }
}
