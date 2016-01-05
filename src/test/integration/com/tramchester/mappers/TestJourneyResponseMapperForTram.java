package com.tramchester.mappers;


import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.Journey;
import com.tramchester.domain.ServiceTime;
import com.tramchester.domain.Stage;
import com.tramchester.domain.TramchesterException;
import com.tramchester.representations.JourneyPlanRepresentation;
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

public class TestJourneyResponseMapperForTram {
    private LocalTime sevenAM = LocalTime.of(7, 0);
    private LocalTime eightAM = LocalTime.of(8, 0);

    private static Dependencies dependencies;
    private JourneyResponseMapper mapper;
    private Set<Journey> journeys;
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
        mapper = dependencies.get(JourneyResponseMapper.class);
        journeys = new HashSet<>();
        stages = new LinkedList<>();
    }

    @Test
    public void shouldEnsureTripsAreOrderByEarliestFirst() throws TramchesterException {
        Stage vicToRoch = new Stage(Stations.Victoria, "route text", "route id", "tram");
        vicToRoch.setServiceId("Serv002271");
        vicToRoch.setLastStation(Stations.Rochdale);
        stages.add(vicToRoch);
        journeys.add(new Journey(stages));
        JourneyPlanRepresentation result = mapper.map(journeys, 930, 5);

        Journey journey = result.getJourneys().stream().findFirst().get();
        Stage stage = journey.getStages().get(0);
        // for this service trips later in the list actually depart earlier, so this would fail
        assertTrue(stage.getFirstDepartureTime().isBefore(LocalTime.of(16,00)));
    }

    @Test
    public void shouldMapSimpleJourney() throws TramchesterException {
        Stage altToCorn = new Stage(Stations.Altrincham, "route text", "route id", "tram");
        altToCorn.setServiceId("Serv002201");
        altToCorn.setLastStation(Stations.Cornbrook);

        stages.add(altToCorn);
        journeys.add(new Journey(stages));
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
        Stage altToDeansgate = new Stage(Stations.Altrincham, "route text", "route id", "tram");
        altToDeansgate.setLastStation(Stations.Deansgate);
        altToDeansgate.setServiceId("Serv002192");

        Stage deansgateToVic = new Stage(Stations.Deansgate, "route2 text", "route2 id", "tram");
        deansgateToVic.setLastStation(Stations.Victoria);
        deansgateToVic.setServiceId("Serv003979");

        stages.add(altToDeansgate);
        stages.add(deansgateToVic);
        journeys.add(new Journey(stages));

        JourneyPlanRepresentation result = mapper.map(journeys, (22*60), 1);
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
        Stage picToCorn = new Stage(Stations.PiccadilyGardens, "routeText", "routeId", "tram");
        picToCorn.setLastStation(Stations.Cornbrook);
        picToCorn.setServiceId("Serv002327");
        // yields 3 trips

        Stage cornToAir = new Stage(Stations.Cornbrook, "routeText", "routeId", "tram");
        cornToAir.setLastStation(Stations.ManAirport);
        cornToAir.setServiceId("Serv004826");
        // yields 1 trip

        stages.add(picToCorn);
        stages.add(cornToAir);
        journeys.add(new Journey(stages));

        JourneyPlanRepresentation result = mapper.map(journeys, (23*60)+10, 3);

        Journey journey = result.getJourneys().stream().findFirst().get();
        assertEquals(1, journey.getNumberOfTimes());
    }

}
