package com.tramchester.integration.dataimport;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.DataLoaderFactory;
import com.tramchester.dataimport.TransportDataBuilderFactory;
import com.tramchester.dataimport.TransportDataLoader;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.IntegrationTrainTestConfig;
import com.tramchester.repository.TransportDataFromFilesBuilder;
import com.tramchester.repository.TransportDataSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collections;

@Disabled("spike")
class TrainDataLoadSpike {

    private final Path dataPath = Path.of("data", "gb-rail-latest");

    @Test
    void testShouldLoadTheData() {

        TransportDataLoader provider = () -> {
            DataLoaderFactory factory = new DataLoaderFactory(dataPath, ".txt");
            return Collections.singletonList(new TransportDataReader(factory, false));
        };
        ProvidesLocalNow providesNow = new ProvidesLocalNow();
        StationLocations stationLocations = new StationLocations(new CoordinateTransforms());

        TransportDataBuilderFactory fileFactory = new TransportDataBuilderFactory(provider, providesNow, stationLocations);

        TransportDataFromFilesBuilder builder = fileFactory.create();
        builder.load();

    }

    @Test
    void shouldInitDependencies() {
        Dependencies dependencies = new Dependencies();
        TramchesterConfig testConfig = new IntegrationTrainTestConfig();

        TransportDataLoader provider = () -> {
            DataLoaderFactory factory = new DataLoaderFactory(dataPath, ".txt");
            return Collections.singletonList(new TransportDataReader(factory, false));
        };
        ProvidesLocalNow providesNow = new ProvidesLocalNow();
        StationLocations stationLocations = dependencies.get(StationLocations.class);

        TransportDataBuilderFactory fileFactory = new TransportDataBuilderFactory(provider, providesNow, stationLocations);
        TransportDataFromFilesBuilder builder = fileFactory.create();
        builder.load();

        TransportDataSource transportDataFromFiles = builder.getData();
        dependencies.initialise(testConfig, transportDataFromFiles);

        dependencies.close();
    }
}
