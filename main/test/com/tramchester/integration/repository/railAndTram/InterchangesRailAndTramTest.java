package com.tramchester.integration.repository.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.InterchangeType;
import com.tramchester.domain.places.SimpleInterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.TramAndTrainGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@GMTest
class InterchangesRailAndTramTest {
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;
    private InterchangeRepository interchangeRepository;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new TramAndTrainGreaterManchesterConfig();
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
    void shouldHaveExpectedInterchangesRail() {
        assertTrue(interchangeRepository.isInterchange(getStation(RailStationIds.ManchesterPiccadilly)));
        assertTrue(interchangeRepository.isInterchange(getStation(RailStationIds.Stockport)));
        assertFalse(interchangeRepository.isInterchange(getStation(RailStationIds.Hale)));
        assertFalse(interchangeRepository.isInterchange(getStation(RailStationIds.Knutsford)));
        assertFalse(interchangeRepository.isInterchange(getStation(RailStationIds.Mobberley)));
    }

    @Test
    void shouldHaveAdditionalTramInterchanges() {
        for (IdFor<Station> interchangeId : AdditionalTramInterchanges.stations()) {
            Station interchange = stationRepository.getStationById(interchangeId);
            assertTrue(interchangeRepository.isInterchange(interchange), interchange.toString());
        }
    }

    @Test
    void shouldHaveExpectedConfig() {
        assertTrue(config.getRailConfig().getOnlyMarkedInterchanges());
        Station station = getStation(RailStationIds.ManchesterPiccadilly);
        assertTrue(config.onlyMarkedInterchange(station));
    }

    @Test
    void shouldHaveExpectedModesForNeighbourInterchangeTram() {
        Station tram = TramStations.Altrincham.from(stationRepository);
        Station train = RailStationIds.Altrincham.from(stationRepository);

        // check not being added as interchange via config or source data
        assertFalse(tram.isMarkedInterchange());

        assertTrue(interchangeRepository.isInterchange(tram));

        InterchangeStation tramInterchange = interchangeRepository.getInterchange(tram);

        assertTrue(tramInterchange.isMultiMode());
        assertEquals(InterchangeType.NeighbourLinks, tramInterchange.getType());

        IdSet<Route> tramPickups = getIds(tram.getPickupRoutes());
        IdSet<Route> trainPickups = getIds(train.getPickupRoutes());
        IdSet<Route> interchangePickups = getIds(tramInterchange.getPickupRoutes());

        assertEquals(tramPickups, interchangePickups);


        assertEquals(train.getDropoffRoutes(), tramInterchange.getDropoffRoutes());
    }

    private IdSet<Route> getIds(Set<Route> pickupRoutes) {
        return pickupRoutes.stream().collect(IdSet.collector());
    }

    @Test
    void shouldHaveExpectedModesForNeighbourInterchangeTrain() {
        Station tram = TramStations.Altrincham.from(stationRepository);
        Station train = RailStationIds.Altrincham.from(stationRepository);

        // for train source dataset indicates if an interchange or not, not via config
        //assertFalse(tram.isMarkedInterchange());

        assertTrue(interchangeRepository.isInterchange(train));

        InterchangeStation trainInterchange = interchangeRepository.getInterchange(tram);

        assertTrue(trainInterchange.isMultiMode());

        assertEquals(train.getPickupRoutes(), trainInterchange.getPickupRoutes());
        assertEquals(tram.getDropoffRoutes(), trainInterchange.getDropoffRoutes());

    }

    @Disabled("todo for mixed transport modes")
    @Test
    void shouldNotAddAnyInterchangeNotAlreadyMarked() {
        IdSet<Station> interchangeButNotMarked = stationRepository.getStations().stream().
                filter(station -> interchangeRepository.isInterchange(station)).
                filter(station -> station.getTransportModes().size()==1).
                filter(found -> !found.isMarkedInterchange()).
                collect(IdSet.collector());

        assertTrue(interchangeButNotMarked.isEmpty(), interchangeButNotMarked.toString());
    }

    private Station getStation(RailStationIds railStationIds) {
        return stationRepository.getStationById(railStationIds.getId());
    }


}
