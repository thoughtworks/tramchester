package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithNaptan;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("for tram stations none of the groups have more that one station")
class StationGroupRepositoryTest {

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
        StationGroup found = stationGroupsRepository.getStationGroup(StringIdFor.createId("940GZZMAALT"));

        // only load groups with more than one stop, for tram stops this is none at all
        assertNull(found, stationGroupsRepository.getAllGroups().toString());

//        assertEquals("Altrincham (Manchester Metrolink)", found.getName());
    }

    @Test
    void shouldFindGroupByName() {
        StationGroup found = stationGroupsRepository.findByName("Altrincham (Manchester Metrolink)");

        // only load groups with more than one stop, for tram stops this is none at all
        assertNull(found);

        //assertIdEquals("940GZZMAALT", found.getAreaId());
    }

    public static class TestConfigWithGroupsEnabledWithTram extends IntegrationTramTestConfigWithNaptan {

        private final TFGMGTFSSourceTestConfig gtfsSourceConfig;

        private TestConfigWithGroupsEnabledWithTram() {
            List<StationClosures> closedStations = Collections.emptyList();
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
