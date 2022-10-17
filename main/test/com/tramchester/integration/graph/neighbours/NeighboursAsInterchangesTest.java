package com.tramchester.integration.graph.neighbours;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.places.InterchangeStation;
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

import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@BusTest
public class NeighboursAsInterchangesTest {

    private static GuiceContainerDependencies componentContainer;
    private InterchangeRepository interchangeRepository;
    private Station altrinchamTram;

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
        altrinchamTram = stationRepository.getStationById(Altrincham.getId());
    }

    /***
     * @see InterchangesTramTest#altrinchamNotAnInterchange()
     */
    @Test
    public void altrinchamBecomesInterchangeWhenNeighboursCreated() {
        assertTrue(interchangeRepository.isInterchange(altrinchamTram));
        fail("todo  - need to pick a different station, shudehill is an interchange");
    }

    @Test
    public void shouldHaveAltrinchamAsMultimode() {
        Optional<InterchangeStation> results = interchangeRepository.getAllInterchanges().stream().
                filter(interchangeStation -> interchangeStation.getStationId().equals(altrinchamTram.getId()))
                .findFirst();

        assertTrue(results.isPresent(), "missing");

        InterchangeStation interchange = results.get();
        assertTrue(interchange.isMultiMode(), "not multimode "  +interchange);

    }

}
