package com.tramchester.integration.graph.neighbours;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.InterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.repository.InterchangesTramTest;
import com.tramchester.integration.testSupport.NeighboursTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BusTest
public class NeighboursAsInterchangesTest {

    private static GuiceContainerDependencies componentContainer;
    private InterchangeRepository interchangeRepository;
    private Station shudehillTram;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        NeighboursTestConfig config = new NeighboursTestConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        interchangeRepository = componentContainer.get(InterchangeRepository.class);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        shudehillTram = stationRepository.getStationById(Shudehill.getId());
    }

    /***
     * @see InterchangesTramTest#shudehillNotAnInterchange()
     */
    @Test
    public void shudehillBecomesInterchangeWhenNeighboursCreated() {
        assertTrue(interchangeRepository.isInterchange(shudehillTram));
    }

    @Test
    public void shouldHaveShudehillAsMultimode() {
        Optional<InterchangeStation> results = interchangeRepository.getAllInterchanges().stream().
                filter(interchangeStation -> interchangeStation.getStationId().equals(shudehillTram.getId()))
                .findFirst();

        assertTrue(results.isPresent(), "missing");

        InterchangeStation interchange = results.get();
        assertTrue(interchange.isMultiMode(), "not multimode "  +interchange);

    }

}
