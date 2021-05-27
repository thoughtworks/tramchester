package com.tramchester.unit.repository;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.repository.TransportModeRepository;
import com.tramchester.testSupport.TestConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportModeRepositoryTest {

    private final Set<TransportMode> modesWithPlatforms = Collections.singleton(TransportMode.Tram);
    private final Set<String> additionalInterchanges = Collections.emptySet();

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

        List<GTFSSourceConfig> dataSources = new LinkedList<>();
        GTFSSourceConfig sourceA = new TFGMGTFSSourceTestConfig("folder/some/pathA", configModesSourceA, modesWithPlatforms, additionalInterchanges);
        dataSources.add(sourceA);

        Set<GTFSTransportationType> configModesSourceB = new HashSet<>();
        configModesSourceB.add(GTFSTransportationType.bus);
        configModesSourceB.add(GTFSTransportationType.train);
        GTFSSourceConfig sourceB = new TFGMGTFSSourceTestConfig("folder/some/pathB", configModesSourceB, modesWithPlatforms, additionalInterchanges);
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
        List<GTFSSourceConfig> dataSources = new LinkedList<>();
        GTFSSourceConfig tramConfig = new TFGMGTFSSourceTestConfig("folder/some/path", configModes, modesWithPlatforms, additionalInterchanges);
        dataSources.add(tramConfig);
        return new ModeConfig(dataSources);
    }

    private static class ModeConfig extends TestConfig {

        private final List<GTFSSourceConfig> dataSources;

        private ModeConfig(List<GTFSSourceConfig> dataSources) {
            this.dataSources = dataSources;
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return dataSources;
        }
    }
}
