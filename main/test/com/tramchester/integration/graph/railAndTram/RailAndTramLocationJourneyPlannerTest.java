package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.WalkingFromStationStage;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.integration.testSupport.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tramchester.integration.testSupport.rail.RailStationIds.ManchesterPiccadilly;
import static com.tramchester.testSupport.TestEnv.Modes.TrainAndTram;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.KnownLocations.nearPiccGardens;
import static com.tramchester.testSupport.reference.TramStations.PiccadillyGardens;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@GMTest
class RailAndTramLocationJourneyPlannerTest {
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static IntegrationTramTestConfig testConfig;

    private final TramDate when = TestEnv.testDay();
    private Transaction txn;
    private LocationJourneyPlannerTestFacade testFacade;
    private Duration maxJourneyDuration;
    private long maxNumberOfJourneys;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        maxJourneyDuration = Duration.ofMinutes(testConfig.getMaxJourneyDuration());
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        stationRepository = componentContainer.get(StationRepository.class);
        testFacade = new LocationJourneyPlannerTestFacade(componentContainer.get(LocationJourneyPlanner.class), stationRepository, txn);
        maxNumberOfJourneys = 3;
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldWalkFromCurrentLocationToStationThenTrain() {
        // repro bug
        // Finding shortest path for Id{'MyLocation:53.39501165965501,-2.358059432782345'} --> Id{'Station:MNCRPIC'} (Manchester Piccadilly Rail Station)
        // for JourneyRequest{date=TramDate{epochDays=19494, dayOfWeek=WEDNESDAY, date=2023-05-17}, originalQueryTime=TramTime{h=10, m=17},
        // arriveBy=false, maxChanges=3, uid=1566ae6b-97d5-49fe-a013-f84f2775518f, maxJourneyDuration=PT2H5M, maxNumberOfJourneys=5,
        // allowedModes=[Tram, Train, RailReplacementBus]}

        TramTime time = TramTime.of(10,17);
        TramDate date = TramDate.of(2023, 5, 17);

        JourneyRequest request = new JourneyRequest(date, time, false, 3,
                maxJourneyDuration, 5, TrainAndTram);

        Location<?> location = new MyLocation(new LatLong(53.39501165965501,-2.358059432782345));

        List<Journey> journeys = new ArrayList<>(testFacade.quickestRouteForLocation(location,
                rail(ManchesterPiccadilly), request, 4));

        assertFalse(journeys.isEmpty(), "no journeys");
    }

    @Test
    void shouldHaveDirectWalkFromPiccadily() {

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(9, 0),
                false, 1, maxJourneyDuration, maxNumberOfJourneys, TramsOnly);

        Set<Journey> unsortedResults = testFacade.quickestRouteForLocation(PiccadillyGardens, nearPiccGardens, journeyRequest, 2);

        assertFalse(unsortedResults.isEmpty());
        unsortedResults.forEach(journey -> {
            List<TransportStage<?,?>> stages = journey.getStages();
            WalkingFromStationStage first = (WalkingFromStationStage) stages.get(0);
            assertEquals(PiccadillyGardens.getId(), first.getFirstStation().getId());
            assertEquals(nearPiccGardens.latLong(), first.getLastStation().getLatLong());
        });
    }

    private Station rail(RailStationIds railStation) {
        return railStation.from(stationRepository);
    }


}
