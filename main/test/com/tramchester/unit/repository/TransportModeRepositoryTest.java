package com.tramchester.unit.repository;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.TFGMTestDataSourceConfig;
import com.tramchester.repository.TransportModeRepository;
import com.tramchester.testSupport.TestConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportModeRepositoryTest {

    @Test
    void shouldHaveCorrectTransportModesSingle() {
        Set<GTFSTransportationType> configModes = new HashSet<>();
        configModes.add(GTFSTransportationType.tram);

        TramchesterConfig config = createConfig(configModes);

        TransportModeRepository repository = new TransportModeRepository(config);
        Set<TransportMode> modes = repository.getModes();

        assertEquals(1, modes.size());
        assertTrue(modes.contains(TransportMode.Tram));
    }

    @Test
    void shouldHaveCorrectTransportModesMultipleForSameSource() {
        Set<GTFSTransportationType> configModes = new HashSet<>();
        configModes.add(GTFSTransportationType.tram);
        configModes.add(GTFSTransportationType.bus);

        TramchesterConfig config = createConfig(configModes);

        TransportModeRepository repository = new TransportModeRepository(config);
        Set<TransportMode> modes = repository.getModes();

        assertEquals(2, modes.size());
        assertTrue(modes.contains(TransportMode.Tram));
        assertTrue(modes.contains(TransportMode.Bus));
    }

    @Test
    void shouldHaveCorrectTransportModesMultipleForDiffSource() {
        Set<GTFSTransportationType> configModesSourceA = new HashSet<>();
        configModesSourceA.add(GTFSTransportationType.tram);
        configModesSourceA.add(GTFSTransportationType.train);

        List<DataSourceConfig> dataSources = new LinkedList<>();
        DataSourceConfig sourceA = new TFGMTestDataSourceConfig("folder/some/pathA", configModesSourceA);
        dataSources.add(sourceA);

        Set<GTFSTransportationType> configModesSourceB = new HashSet<>();
        configModesSourceB.add(GTFSTransportationType.bus);
        configModesSourceB.add(GTFSTransportationType.train);
        DataSourceConfig sourceB = new TFGMTestDataSourceConfig("folder/some/pathB", configModesSourceB);
        dataSources.add(sourceB);

        TramchesterConfig config = new ModeConfig(dataSources);

        TransportModeRepository repository = new TransportModeRepository(config);
        Set<TransportMode> modes = repository.getModes();

        assertEquals(3, modes.size());
        assertTrue(modes.contains(TransportMode.Tram));
        assertTrue(modes.contains(TransportMode.Bus));
        assertTrue(modes.contains(TransportMode.Train));
    }

    @NotNull
    private TramchesterConfig createConfig(Set<GTFSTransportationType> configModes) {
        List<DataSourceConfig> dataSources = new LinkedList<>();
        DataSourceConfig tramConfig = new TFGMTestDataSourceConfig("folder/some/path", configModes);
        dataSources.add(tramConfig);
        return new ModeConfig(dataSources);
    }

    private static class ModeConfig extends TestConfig {

        private final List<DataSourceConfig> dataSources;

        private ModeConfig(List<DataSourceConfig> dataSources) {
            this.dataSources = dataSources;
        }

        @Override
        protected List<DataSourceConfig> getDataSourceFORTESTING() {
            return dataSources;
        }
    }
}
