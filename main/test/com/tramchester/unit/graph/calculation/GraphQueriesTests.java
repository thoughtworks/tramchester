package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.search.FindStationLinks;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestProvider;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphQueriesTests {

    private static ComponentContainer componentContainer;
    private static SimpleGraphConfig config;
    private TramTransportDataForTestProvider.TestTransportData transportData;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new SimpleGraphConfig();

        FileUtils.deleteDirectory(config.getDBPath().toFile());

        componentContainer = new ComponentsBuilder<TramTransportDataForTestProvider>().
                overrideProvider(TramTransportDataForTestProvider.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initContainer();
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        componentContainer.close();
        FileUtils.deleteDirectory(config.getDBPath().toFile());
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = (TramTransportDataForTestProvider.TestTransportData) componentContainer.get(TransportData.class);
    }

    @Test
    void shouldHaveCorrectLinksBetweenStations() {
        FindStationLinks findStationLinks = componentContainer.get(FindStationLinks.class);

        Set<StationLink> links = findStationLinks.findFor(Tram);

        assertEquals(5, links.size());

        Set<TransportMode> modes = Collections.singleton(Tram);
        assertTrue(links.contains(new StationLink(transportData.getFirst(), transportData.getSecond(), modes)));
        assertTrue(links.contains(new StationLink(transportData.getSecond(), transportData.getInterchange(), modes)));
        assertTrue(links.contains(new StationLink(transportData.getInterchange(), transportData.getFourthStation(), modes)));
        assertTrue(links.contains(new StationLink(transportData.getInterchange(), transportData.getFifthStation(), modes)));
        assertTrue(links.contains(new StationLink(transportData.getInterchange(), transportData.getLast(), modes)));

    }
}
