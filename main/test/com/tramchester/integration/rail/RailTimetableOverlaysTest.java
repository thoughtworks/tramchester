package com.tramchester.integration.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.RailConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Service;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("WIP")
public class RailTimetableOverlaysTest {
    private static TramchesterConfig config;

    private static ComponentContainer componentContainer;
    private TransportData transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new RailOverlayTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = componentContainer.get(TransportData.class);
    }

    @Test
    void shouldTestSomething() {
        Set<Service> services = transportData.getServices();
        assertEquals(7, services.size());
    }

    private static class RailOverlayTestConfig extends IntegrationRailTestConfig {

        @Override
        public Path getCacheFolder() {
            return TestEnv.CACHE_DIR.resolve("railOverlays");
        }


        @Override
        public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
            return Collections.emptyList();
            //return List.of(remoteNaptanXMLConfig, remoteNPTGconfig);
        }

        @Override
        public RailConfig getRailConfig() {
            return new RailConfig() {
                @Override
                public Path getStations() {
                    return Path.of("exampleStations.msn");
                }

                @Override
                public Path getTimetable() {
                    return Path.of("exampleOverlays.mca");
                }

                @Override
                public Path getDataPath() {
                    return Path.of("testData", "rail");
                }

                @Override
                public Set<TransportMode> getModes() {
                    return Collections.singleton(TransportMode.Train);
                }

                @Override
                public String getVersion() {
                    return "invalid";
                }


                @Override
                public boolean getOnlyMarkedInterchanges() {
                    return true;
                }

                @Override
                public DataSourceID getDataSourceId() {
                    return DataSourceID.rail;
                }
            };
        }
    }
}
