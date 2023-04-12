package com.tramchester.integration.repository.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TrainTest
class InterchangesRailTest {
    private static ComponentContainer componentContainer;
    private static IntegrationRailTestConfig config;
    private InterchangeRepository interchangeRepository;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationRailTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }


    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        interchangeRepository = componentContainer.get(InterchangeRepository.class);
    }

    @Test
    void shouldHaveExpectedNumberOfInterchanges() {
        assertEquals(993, interchangeRepository.size());
    }

    @Test
    void shouldHaveExpectedInterchanges() {
        assertTrue(interchangeRepository.isInterchange(getStation(RailStationIds.ManchesterPiccadilly)));
        assertTrue(interchangeRepository.isInterchange(getStation(RailStationIds.Stockport)));
        assertTrue(interchangeRepository.isInterchange(getStation(RailStationIds.LondonEuston)));

        assertFalse(interchangeRepository.isInterchange(getStation(RailStationIds.Hale)));
        assertFalse(interchangeRepository.isInterchange(getStation(RailStationIds.Knutsford)));
        assertFalse(interchangeRepository.isInterchange(getStation(RailStationIds.Mobberley)));
    }

    @Test
    void shouldHaveExpectedConfig() {
        assertTrue(config.getRailConfig().getOnlyMarkedInterchanges());
        Station station = getStation(RailStationIds.ManchesterPiccadilly);
        assertTrue(config.onlyMarkedInterchange(station));
    }

    @Test
    void shouldNotAddAnyInterchangeNotAlreadyMarked() {
        Set<Station> interchangeButNotMarked = stationRepository.getStations().stream().
                filter(station -> interchangeRepository.isInterchange(station)).
                filter(station -> station.getTransportModes().size()==1).
                filter(found -> !found.isMarkedInterchange()).collect(Collectors.toSet());

        assertTrue(interchangeButNotMarked.isEmpty(), interchangeButNotMarked.toString());
    }

    private Station getStation(RailStationIds railStationIds) {
        return stationRepository.getStationById(railStationIds.getId());
    }


}
