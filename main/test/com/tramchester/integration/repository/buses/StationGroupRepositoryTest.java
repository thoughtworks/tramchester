package com.tramchester.integration.repository.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.integration.testSupport.Assertions.assertIdEquals;
import static com.tramchester.testSupport.reference.BusStations.ManchesterAirportStation;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
class StationGroupRepositoryTest {
    private StationGroupsRepository stationGroupsRepository;
    private StationRepository stationRepository;

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
        stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
    }

    @Test
    void shouldFindExpectedCompositeStations() {
        assertNotNull(stationGroupsRepository.findByName("Shudehill Interchange"));
        final StationGroup groupedStations = stationGroupsRepository.findByName(BusStations.Composites.AltrinchamInterchange.getName());
        assertNotNull(groupedStations);

        assertEquals(6, groupedStations.getContained().size());

        assertEquals(LocationType.StationGroup, groupedStations.getLocationType());
        assertIdEquals("180GAMIC", groupedStations.getAreaId());
        assertEquals(2, groupedStations.getMinimumChangeCost());
    }

    @Test
    void shouldHaveUniqueIds() {
        IdSet<Station> uniqueIds = new IdSet<>();

        stationRepository.getStationsServing(Bus).forEach(station -> {
            IdFor<Station> id = station.getId();
            assertFalse(uniqueIds.contains(id), "Not unique " + id);
            uniqueIds.add(id);
        });
    }

    @Test
    void shouldHaveValidStationsInGroupedStation() {
        Set<StationGroup> compositesFor = stationGroupsRepository.getStationGroupsFor(Bus);
        assertFalse(compositesFor.isEmpty());

        compositesFor.forEach(group -> {

            group.getContained().forEach(station -> {
                IdFor<Station> id = station.getId();
                assertTrue(stationRepository.hasStationId(id), "could not find " + id + " for group " + group.getId());
            });
        });
    }

    // now grouped station have their own ID this is no longer needed
//    @Test
//    void shouldFindCorrectlyForNonComposites() {
//        IdSet<GroupedStations> compIds = stationGroupsRepository.getStationGroupsFor(Bus).stream().
//                map(GroupedStations::getId).
//                collect(IdSet.idCollector());
//        assertFalse(compIds.isEmpty());
//
//        stationRepository.getStations().stream().filter(station -> !compIds.contains(station.getId())).forEach(station -> {
//            IdFor<Station> id = station.getId();
//
//            assertTrue(stationRepository.hasStationId(id));
//            Station foundViaRepos = stationRepository.getStationById(id);
//            assertNotNull(foundViaRepos);
//            assertEquals(station, foundViaRepos);
//        });
//    }

    @Test
    void shouldGetCorrectForNonCompositeStation() {
        Station resultA = stationRepository.getStationById(ManchesterAirportStation.getId());
        Station resultFull = stationRepository.getStationById(ManchesterAirportStation.getId());

        assertEquals(resultFull, resultA);

    }

}
