package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.GroupedStations;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithNaptan;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.tramchester.integration.testSupport.Assertions.assertIdEquals;
import static org.junit.jupiter.api.Assertions.*;

class GroupedStationsRepositoryTest {

    private static GuiceContainerDependencies componentContainer;
    private StationGroupsRepository stationGroupsRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        final TramchesterConfig config = new TestConfigWithGroupsEnabledWithTram();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
    }

    @Test
    void shouldBeEnabled() {
        assertTrue(stationGroupsRepository.isEnabled());
    }

    @Test
    void shouldHaveExpectedTramStationGroup() {
        GroupedStations found = stationGroupsRepository.getStationGroup(StringIdFor.createId("940GZZMAALT"));

        assertNotNull(found, stationGroupsRepository.getAllGroups().toString());

        assertEquals("Altrincham (Manchester Metrolink)", found.getName());
    }

    @Test
    void shouldFindGroupByName() {
        GroupedStations found = stationGroupsRepository.findByName("Altrincham (Manchester Metrolink)");

        assertNotNull(found);

        assertIdEquals("940GZZMAALT", found.getAreaId());
    }

    public static class TestConfigWithGroupsEnabledWithTram extends IntegrationTramTestConfigWithNaptan {

        private final TFGMGTFSSourceTestConfig gtfsSourceConfig;

        private TestConfigWithGroupsEnabledWithTram() {
            List<StationClosure> closedStations = Collections.emptyList();
            final Set<TransportMode> groupStationModes = Collections.singleton(TransportMode.Tram);

            gtfsSourceConfig = new TFGMGTFSSourceTestConfig("data/tram", GTFSTransportationType.tram,
                    TransportMode.Tram, AdditionalTramInterchanges.stations(), groupStationModes, closedStations);
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return Collections.singletonList(gtfsSourceConfig);
        }

    }

}
