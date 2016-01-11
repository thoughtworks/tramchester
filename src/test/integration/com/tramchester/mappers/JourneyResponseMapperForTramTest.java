package com.tramchester.mappers;


import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.RawJourney;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.ServiceTime;
import com.tramchester.domain.presentation.Stage;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

public class JourneyResponseMapperForTramTest extends JourneyResponseMapperTest {
    private LocalTime sevenAM = LocalTime.of(7, 0);
    private LocalTime eightAM = LocalTime.of(8, 0);

    private static Dependencies dependencies;
    private TramJourneyResponseMapper mapper;
    private Set<RawJourney> journeys;
    private List<Stage> stages;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }


    @Before
    public void beforeEachTestRuns() {
        mapper = dependencies.get(TramJourneyResponseMapper.class);
        routeCalculator = dependencies.get(RouteCalculator.class);
        journeys = new HashSet<>();
        stages = new LinkedList<>();
    }

    @Test
    public void shouldEnsureTripsAreOrderByEarliestFirst() throws TramchesterException {
        String svcId = findServiceId(Stations.Victoria, Stations.Rochdale, 930);
        Stage vicToRoch = new Stage(Stations.Victoria, "route text", "tram", "cssClass");
        vicToRoch.setServiceId(svcId);
        vicToRoch.setLastStation(Stations.Rochdale);
        stages.add(vicToRoch);
        journeys.add(new RawJourney(stages,1));
        JourneyPlanRepresentation result = mapper.map(journeys, 930, 5);

        Journey journey = result.getJourneys().stream().findFirst().get();
        Stage stage = journey.getStages().get(0);
        // for this service trips later in the list actually depart earlier, so this would fail
        assertTrue(stage.getFirstDepartureTime().isBefore(LocalTime.of(16,00)));
    }

    @Test
    public void shouldMapSimpleJourney() throws TramchesterException {
        String svcId = findServiceId(Stations.Altrincham, Stations.Cornbrook, 7*60);

        Stage altToCorn = new Stage(Stations.Altrincham, "route text", "tram", "cssClass");
        altToCorn.setServiceId(svcId);
        altToCorn.setLastStation(Stations.Cornbrook);

        stages.add(altToCorn);
        journeys.add(new RawJourney(stages,1));
        JourneyPlanRepresentation result = mapper.map(journeys, 7*60, 1);

        assertEquals(1,result.getJourneys().size());
        Journey journey = result.getJourneys().stream().findFirst().get();
        assertEquals(1, journey.getStages().size());
        Stage stage = journey.getStages().get(0);
        assertEquals(Stations.Altrincham,stage.getFirstStation());
        assertEquals(Stations.Cornbrook,stage.getLastStation());
        assertTrue(stage.getDuration()>0);
        assertTrue(stage.getFirstDepartureTime().isAfter(sevenAM));
        assertTrue(stage.getFirstDepartureTime().isBefore(eightAM));
        assertTrue(stage.getExpectedArrivalTime().isAfter(sevenAM));
        assertTrue(stage.getExpectedArrivalTime().isBefore(eightAM));

        List<ServiceTime> serviceTimes = stage.getServiceTimes();
        assertEquals(1, serviceTimes.size());
    }

    @Test
    public void shouldMapTwoStageJourney() throws TramchesterException {
        int pm10 = 22 * 60;
        String svcId = findServiceId(Stations.Altrincham, Stations.Deansgate, pm10);

        Stage altToDeansgate = new Stage(Stations.Altrincham, "route text", "tram", "cssClass");
        altToDeansgate.setLastStation(Stations.Deansgate);
        altToDeansgate.setServiceId(svcId);

        svcId = findServiceId(Stations.Deansgate, Stations.Victoria, pm10);

        Stage deansgateToVic = new Stage(Stations.Deansgate, "route2 text", "tram", "cssClass");
        deansgateToVic.setLastStation(Stations.Victoria);
        deansgateToVic.setServiceId(svcId);

        stages.add(altToDeansgate);
        stages.add(deansgateToVic);
        journeys.add(new RawJourney(stages,1));

        JourneyPlanRepresentation result = mapper.map(journeys, pm10, 1);
        assertEquals(1,result.getJourneys().size());
        Journey journey = result.getJourneys().stream().findFirst().get();
        assertEquals(2, journey.getStages().size());

        Stage stage2 = journey.getStages().get(1);
        assertEquals(Stations.Deansgate,stage2.getFirstStation());
        assertEquals(Stations.Victoria,stage2.getLastStation());

        List<ServiceTime> serviceTimes = stage2.getServiceTimes();
        assertEquals(1, serviceTimes.size());
    }

    @Test
    public void shouldMapEndOfDayJourneyCorrectly() throws TramchesterException {
        int pm23 = 23 * 60;
        String svcId = findServiceId(Stations.PiccadilyGardens, Stations.Cornbrook, pm23);

        Stage picToCorn = new Stage(Stations.PiccadilyGardens, "routeText", "tram", "cssClass");
        picToCorn.setLastStation(Stations.Cornbrook);
        // use test TramJourneyPlannerTest.shouldFindRoutePiccadilyGardensToCornbrook
        picToCorn.setServiceId(svcId);

        svcId = findServiceId(Stations.Cornbrook, Stations.ManAirport, pm23);
        Stage cornToAir = new Stage(Stations.Cornbrook, "routeText", "tram", "cssClass");
        cornToAir.setLastStation(Stations.ManAirport);
        // user test TramJourneyPlannerTest.shouldFindRouteCornbrookToManAirport
        cornToAir.setServiceId(svcId);

        stages.add(picToCorn);
        stages.add(cornToAir);
        journeys.add(new RawJourney(stages,1));

        JourneyPlanRepresentation result = mapper.map(journeys, pm23, 3);

        Journey journey = result.getJourneys().stream().findFirst().get();
        assertTrue(journey.getNumberOfTimes()>0);
    }

}
