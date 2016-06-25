package com.tramchester.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.Location;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.PresentableStage;
import org.joda.time.LocalDate;
import org.junit.*;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.time.LocalTime;
import java.util.List;
import java.util.SortedSet;

import static org.junit.Assert.*;

public class MyLocationJourneyPlannerTest extends JourneyPlannerHelper {
    private static Dependencies dependencies;
    private LocalDate today;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10*60);
    private LatLong nearPiccGardens;
    private LatLong nearAltrincham;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        nearPiccGardens = new LatLong(53.4805248D, -2.2394929D);
        nearAltrincham = new LatLong(53.394948299999996D,-2.3581502D);
        today = LocalDate.now();
        planner = dependencies.get(JourneyPlannerResource.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldFindStationsNearPiccGardensToExchangeSquare() throws JsonProcessingException, TramchesterException {
        validateJourneyFromLocation(nearPiccGardens, Stations.ExchangeSquare.getId(), 9 * 60);
    }

    @Test
    public void planRouteAllowingForWalkingTime() throws JsonProcessingException, TramchesterException {
        SortedSet<Journey> journeys = validateJourneyFromLocation(nearAltrincham, Stations.Piccadilly.getId(), (22 * 60) + 9);
        assertEquals(1, journeys.size());
        Journey first = journeys.first();

        List<PresentableStage> stages = first.getStages();
        assertEquals(2, stages.size());
        PresentableStage walkingStage = stages.get(0);
        assertEquals(LocalTime.of(22,9), walkingStage.getFirstDepartureTime());
        assertEquals(LocalTime.of(22,22), walkingStage.getExpectedArrivalTime());

        assertEquals(LocalTime.of(22,34), first.getFirstDepartureTime());
        assertEquals(LocalTime.of(23,5), first.getExpectedArrivalTime());
    }

    @Test
    public void shouldFindStationsNearPiccGardensWalkingOnly() throws JsonProcessingException, TramchesterException {
        SortedSet<Journey> journeys = validateJourneyFromLocation(nearPiccGardens, Stations.PiccadillyGardens.getId(), 9 * 60);
        assertEquals(1, journeys.size());
        Journey first = journeys.first();
        List<PresentableStage> stages = first.getStages();
        assertEquals(LocalTime.of(9,00), first.getFirstDepartureTime());
        assertEquals(LocalTime.of(9,03), first.getExpectedArrivalTime());
        assertEquals(Stations.PiccadillyGardens, first.getEnd());

        assertEquals(1, stages.size());
        PresentableStage stage = stages.get(0);
        assertEquals(LocalTime.of(9,00), stage.getFirstDepartureTime());
        assertEquals(LocalTime.of(9,03), stage.getExpectedArrivalTime());
    }

    @Test
    public void shouldFindRouteNearEndOfServiceTimes() throws JsonProcessingException, TramchesterException {
        Location destination = Stations.PiccadillyGardens;

        int queryTime = 23 * 60;
        int walkingTime = 13;
        JourneyPlanRepresentation direct = validateAtLeastOneJourney(Stations.NavigationRoad,
                destination, queryTime+walkingTime, today);
        assertTrue(direct.getJourneys().size()>0);

        validateJourneyFromLocation(nearAltrincham, destination.getId(), queryTime);

    }

    private SortedSet<Journey> validateJourneyFromLocation(LatLong location, String destination, int queryTime)
            throws JsonProcessingException, TramchesterException {
        String startId = JourneyPlannerTest.formId(location);
        JourneyPlanRepresentation plan = planner.createJourneyPlan(startId, destination, new TramServiceDate(today), queryTime);
        SortedSet<Journey> journeys = plan.getJourneys();
        assertTrue(journeys.size()>=1);
        List<PresentableStage> stages = journeys.first().getStages();
        assertTrue(stages.size()>0);
        stages.forEach(stage -> assertTrue(stage.toString(),stage.getDuration()>0));

        return journeys;
    }

}
