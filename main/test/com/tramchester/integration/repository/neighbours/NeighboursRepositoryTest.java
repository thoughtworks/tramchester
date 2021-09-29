package com.tramchester.integration.repository.neighbours;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.NeighboursTestConfig;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.NeighboursRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BusTest
public class NeighboursRepositoryTest {

    private NeighboursRepository neighboursRepository;

    private CompositeStation shudehillCompositeBus;
    private Station shudehillTram;

    private static ComponentContainer componentContainer;
    private CompositeStationRepository compositeStationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new NeighboursTestConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        neighboursRepository = componentContainer.get(NeighboursRepository.class);
        compositeStationRepository = componentContainer.get(CompositeStationRepository.class);

        shudehillCompositeBus = compositeStationRepository.findByName("Shudehill Interchange");
        shudehillTram = compositeStationRepository.getStationById(Shudehill.getId());
    }

    @Test
    void shouldHaveCorrectNeighboursForAltrinchamTram() {
        CompositeStation altrinchamComposite = compositeStationRepository.findByName(BusStations.Composites.AltrinchamInterchange.getName());

        IdSet<Station> neighbours = neighboursRepository.getNeighboursFor(TramStations.Altrincham.getId())
                .stream().collect(IdSet.collector());
        IdSet<Station> ids = altrinchamComposite.getContained().stream().collect(IdSet.collector());

        assertTrue(neighbours.containsAll(ids));
    }

    @Test
    void shouldHaveCorrectNeighboursForTramAtShudehill() {
        IdSet<Station> neighbours = neighboursRepository.getNeighboursFor(Shudehill.getId()).stream().collect(IdSet.collector());
        IdSet<Station> busStops = shudehillCompositeBus.getContained().stream().collect(IdSet.collector());
        assertTrue(neighbours.containsAll(busStops));
    }

    @Test
    void shouldHaveCorrectNeighboursForBusAtShudehill() {
        shudehillCompositeBus.getContained().forEach(station -> {
            IdSet<Station> neighbours = neighboursRepository.getNeighboursFor(station.getId()).stream().collect(IdSet.collector());
            assertTrue(neighbours.contains(shudehillTram.getId()));
        });
    }

}
