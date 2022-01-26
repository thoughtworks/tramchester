package com.tramchester.integration.repository.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.GroupedStations;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.testTags.BusTest;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.testSupport.reference.BusStations.ManchesterAirportStation;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
class GroupedStationsRepositoryTest {
    private CompositeStationRepository repository;
    private StationRepository fullRepository;

    private static ComponentContainer componentContainer;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationBusTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        repository = componentContainer.get(CompositeStationRepository.class);
        fullRepository = componentContainer.get(StationRepository.class);
    }

    @Test
    void shouldNotHaveDuplicateNamesForStationsWithAreas() {
        Set<String> uniqueNames = new HashSet<>();
        Set<Station> dups = new HashSet<>();

        repository.getStationsServing(Bus).forEach(station -> {
            String name = station.getName() + " " + station.getArea();
            if (uniqueNames.contains(name)) {
                dups.add(station);
            }
            uniqueNames.add(name);
        });

        assertTrue(dups.isEmpty());
    }

    @Test
    void shouldFindExpectedCompositeStations() {
        assertNotNull(repository.findByName("Shudehill Interchange"));
        final GroupedStations groupedStations = repository.findByName(BusStations.Composites.AltrinchamInterchange.getName());
        assertNotNull(groupedStations);

        assertTrue(groupedStations.isComposite());
        assertEquals("Altrincham", groupedStations.getArea());
        assertEquals(2, groupedStations.getMinimumChangeCost());
    }

    @Test
    void shouldHaveCorrectNumberOfComposites() {
        assertFalse(repository.getStationsServing(Bus).isEmpty());

        long duplicateNamesFromFullRepository = fullRepository.getStations().stream().map(Station::getName).
                collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).
                values().stream().
                filter(count -> count > 1).count();

        // at least this many, will be more due to additional area grouping
        assertTrue(repository.getNumberOfComposites() >= duplicateNamesFromFullRepository);
        assertTrue(repository.getCompositesServing(Bus).size() >= duplicateNamesFromFullRepository);
    }

    @Test
    void shouldHaveUniqueIds() {
        IdSet<Station> uniqueIds = new IdSet<>();

        repository.getStationsServing(Bus).forEach(station -> {
            IdFor<Station> id = station.getId();
            assertFalse(uniqueIds.contains(id), "Not unique " + id);
            uniqueIds.add(id);
        });
    }

    @Test
    void shouldHaveAndFindCorrectlyForComposites() {
        Set<GroupedStations> compositesFor = repository.getCompositesServing(Bus);
        assertFalse(compositesFor.isEmpty());

        compositesFor.forEach(station -> {
            IdFor<Station> id = station.getId();

            assertTrue(repository.hasStationId(id), "could not find " + id);
            assertNotNull(repository.getStationById(id), "could not get " + id);

            assertFalse(fullRepository.hasStationId(id), "comp id should not be in full repos");

        });
    }

    @Test
    void shouldFindCorrectlyForNonComposites() {
        IdSet<Station> compIds = repository.getCompositesServing(Bus).stream().map(Station::getId).collect(IdSet.idCollector());
        assertFalse(compIds.isEmpty());

        fullRepository.getStations().stream().filter(station -> !compIds.contains(station.getId())).forEach(station -> {
            IdFor<Station> id = station.getId();

            assertTrue(repository.hasStationId(id));
            Station foundViaRepos = repository.getStationById(id);
            assertNotNull(foundViaRepos);
            assertEquals(station, foundViaRepos);
        });
    }

    @Test
    void shouldGetCorrectForNonCompositeStation() {
        Station resultA = repository.getStationById(ManchesterAirportStation.getId());
        Station resultFull = fullRepository.getStationById(ManchesterAirportStation.getId());

        assertEquals(resultFull, resultA);

    }

}
