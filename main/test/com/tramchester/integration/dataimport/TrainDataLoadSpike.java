package com.tramchester.integration.dataimport;

import com.tramchester.dataimport.*;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.StationLocations;
import com.tramchester.repository.TransportDataFromFiles;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class TrainDataLoadSpike {

    private final Path dataPath = Path.of("data", "gb-rail-latest");

    @Disabled("SPIKE")
    @Test
    void testShouldLoadTheData() {
        TransportDataLoader provider = () -> {
            DataLoaderFactory factory = new DataLoaderFactory(dataPath, ".txt");
            return new TransportDataReader(factory, false);
        };
        ProvidesLocalNow providesNow = new ProvidesLocalNow();
        StationLocations stationLocations = new StationLocations(new CoordinateTransforms());
        TransportDataFromFileFactory fileFactory = new TransportDataFromFileFactory(provider, providesNow, stationLocations);

        TransportDataFromFiles transportDataFromFiles = fileFactory.create(false);
        transportDataFromFiles.start();
    }
}
