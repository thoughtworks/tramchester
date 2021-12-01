package com.tramchester.integration.repository.trains;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TrainTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class InterchangesTrainTest {
    private static ComponentContainer componentContainer;
    private InterchangeRepository interchangeRepository;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationRailTestConfig(), TestEnv.NoopRegisterMetrics());
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
    void shouldHaveExpectedInterchanges() {

        assertTrue(interchangeRepository.isInterchange(getStation(RailStationIds.ManchesterPiccadilly)));
        assertTrue(interchangeRepository.isInterchange(getStation(RailStationIds.Stockport)));
        assertTrue(interchangeRepository.isInterchange(getStation(RailStationIds.LondonEuston)));

        assertFalse(interchangeRepository.isInterchange(getStation(RailStationIds.Hale)));
        assertFalse(interchangeRepository.isInterchange(getStation(RailStationIds.Knutsford)));
        assertFalse(interchangeRepository.isInterchange(getStation(RailStationIds.Mobberley)));
    }

    private Station getStation(RailStationIds railStationIds) {
        return stationRepository.getStationById(railStationIds.getId());
    }


}
