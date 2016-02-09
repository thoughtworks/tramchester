package com.tramchester.mappers;


import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.ServiceTime;
import com.tramchester.domain.presentation.StageWithTiming;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalTime;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;

public class JourneyResponseMapperForTramTest extends JourneyResponseMapperTest {
    private LocalTime sevenAM = LocalTime.of(7, 0);
    private LocalTime eightAM = LocalTime.of(8, 0);

    private static Dependencies dependencies;
    private TramJourneyResponseMapper mapper;
    private Set<RawJourney> journeys;
    private List<TransportStage> stages;

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
        String svcId = findServiceId(Stations.Victoria.getId(), Stations.Rochdale.getId(), 930);
        int elapsedTime = 931;
        RawVehicleStage vicToRoch = new RawVehicleStage(Stations.Victoria, "route text", TransportMode.Tram, "cssClass", elapsedTime);
        vicToRoch.setServiceId(svcId);
        vicToRoch.setLastStation(Stations.Rochdale);
        stages.add(vicToRoch);
        journeys.add(new RawJourney(stages));
        JourneyPlanRepresentation result = mapper.map(journeys, new TimeWindow(930, 30));

        Journey journey = result.getJourneys().stream().findFirst().get();
        StageWithTiming stage = journey.getStages().get(0);
        // for this service trips later in the list actually depart earlier, so this would fail
        assertTrue(stage.getFirstDepartureTime().isBefore(LocalTime.of(16,00)));
    }

    @Test
    public void shouldMapSimpleJourney() throws TramchesterException {
        String svcId = findServiceId(Stations.Altrincham.getId(), Stations.Cornbrook.getId(), 7*60);

        int elapsedTime = (7*60)+1;
        RawVehicleStage altToCorn = new RawVehicleStage(Stations.Altrincham, "route text", TransportMode.Tram, "cssClass", elapsedTime);
        altToCorn.setServiceId(svcId);
        altToCorn.setLastStation(Stations.Cornbrook);

        stages.add(altToCorn);
        journeys.add(new RawJourney(stages));
        JourneyPlanRepresentation result = mapper.map(journeys, new TimeWindow(7*60, 30));

        assertEquals(1,result.getJourneys().size());
        Journey journey = result.getJourneys().stream().findFirst().get();
        assertEquals(1, journey.getStages().size());
        StageWithTiming stage = journey.getStages().get(0);
        assertEquals(Stations.Altrincham,stage.getFirstStation());
        assertEquals(Stations.Cornbrook,stage.getLastStation());
        assertTrue(stage.getDuration()>0);
        assertTrue(stage.getFirstDepartureTime().isAfter(sevenAM));
        assertTrue(stage.getFirstDepartureTime().isBefore(eightAM));
        assertTrue(stage.getExpectedArrivalTime().isAfter(sevenAM));
        assertTrue(stage.getExpectedArrivalTime().isBefore(eightAM));

        SortedSet<ServiceTime> serviceTimes = stage.getServiceTimes();
        assertEquals(2, serviceTimes.size());
    }

    @Test
    public void shouldMapTwoStageJourney() throws TramchesterException {
        int pm10 = 22 * 60;
        String svcId = findServiceId(Stations.Altrincham.getId(), Stations.Deansgate.getId(), pm10);

        int elapsedTime = pm10 + 1;
        RawVehicleStage altToDeansgate = new RawVehicleStage(Stations.Altrincham, "route text", TransportMode.Tram, "cssClass", elapsedTime);
        altToDeansgate.setLastStation(Stations.Deansgate);
        altToDeansgate.setServiceId(svcId);

        svcId = findServiceId(Stations.Deansgate.getId(), Stations.Victoria.getId(), pm10);

        RawVehicleStage deansgateToVic = new RawVehicleStage(Stations.Deansgate, "route2 text", TransportMode.Tram, "cssClass", elapsedTime);
        deansgateToVic.setLastStation(Stations.Victoria);
        deansgateToVic.setServiceId(svcId);

        stages.add(altToDeansgate);
        stages.add(deansgateToVic);
        journeys.add(new RawJourney(stages));

        JourneyPlanRepresentation result = mapper.map(journeys, new TimeWindow(pm10, 30));
        assertEquals(1,result.getJourneys().size());
        Journey journey = result.getJourneys().stream().findFirst().get();
        assertEquals(2, journey.getStages().size());

        StageWithTiming stage2 = journey.getStages().get(1);
        assertEquals(Stations.Deansgate,stage2.getFirstStation());
        assertEquals(Stations.Victoria,stage2.getLastStation());

        SortedSet<ServiceTime> serviceTimes = stage2.getServiceTimes();
        assertEquals(2, serviceTimes.size());

        assertEquals("Change tram at",stage2.getPrompt());
    }

    @Test
    public void shouldMapEndOfDayJourneyCorrectly() throws TramchesterException {
        int pm23 = 23 * 60;
        String svcId = findServiceId(Stations.PiccadilyGardens.getId(), Stations.Cornbrook.getId(), pm23);

        int elapsedTime = pm23 +1;
        RawVehicleStage picToCorn = new RawVehicleStage(Stations.PiccadilyGardens, "routeText", TransportMode.Tram, "cssClass", elapsedTime);
        picToCorn.setLastStation(Stations.Cornbrook);
        // use test TramJourneyPlannerTest.shouldFindRoutePiccadilyGardensToCornbrook
        picToCorn.setServiceId(svcId);

        svcId = findServiceId(Stations.Cornbrook.getId(), Stations.ManAirport.getId(), pm23);
        RawVehicleStage cornToAir = new RawVehicleStage(Stations.Cornbrook, "routeText", TransportMode.Tram, "cssClass", elapsedTime);
        cornToAir.setLastStation(Stations.ManAirport);
        // user test TramJourneyPlannerTest.shouldFindRouteCornbrookToManAirport
        cornToAir.setServiceId(svcId);

        stages.add(picToCorn);
        stages.add(cornToAir);
        journeys.add(new RawJourney(stages));

        JourneyPlanRepresentation result = mapper.map(journeys, new TimeWindow(pm23, 30));

        Journey journey = result.getJourneys().stream().findFirst().get();
        assertTrue(journey.getNumberOfTimes()>0);
    }

}
