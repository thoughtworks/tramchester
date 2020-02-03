package com.tramchester.integration.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.tramchester.App;
import com.tramchester.TestConfig;
import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.Location;
import com.tramchester.domain.MyLocationFactory;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

import static com.tramchester.TestConfig.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MyLocationJourneyPlannerTest {

    public static final String TIME_PATTERN = "HH:mm:00";
    private static AppConfiguration config = new IntegrationTramTestConfig();

    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, config);

    private ObjectMapper mapper = new ObjectMapper();
    private LocalDate when;

    @Before
    public void beforeEachTestRuns() {
        when = TestConfig.nextTuesday(0);
        mapper.registerModule(new JodaModule());
    }

    @Test
    public void shouldFindStationsNearPiccGardensToExchangeSquare() {
        validateJourneyFromLocation(nearPiccGardens, Stations.ExchangeSquare.getId(), LocalTime.of(9,0));
    }

    @Test
    public void planRouteAllowingForWalkingTime() {
        SortedSet<JourneyDTO> journeys = validateJourneyFromLocation(nearAltrincham, Stations.Deansgate.getId(),  LocalTime.of(20,9));
        assertTrue(journeys.size()>0);
        JourneyDTO firstJourney = journeys.first();

        List<StageDTO> stages = firstJourney.getStages();
        assertEquals(2, stages.size());
        StageDTO walkingStage = stages.get(0);
        TramTime departureTime = walkingStage.getFirstDepartureTime();
        // two walks result in same arrival time
        List<TramTime> possibleTimes = Arrays.asList(TramTime.of(20, 19), TramTime.of(20, 12));
        assertTrue(departureTime.toString(), possibleTimes.contains(departureTime));

        assertEquals(firstJourney.toString(), TramTime.of(20,19), firstJourney.getFirstDepartureTime());
        assertEquals(firstJourney.toString(), TramTime.of(20,48), firstJourney.getExpectedArrivalTime());
    }

    @Test
    public void shouldPlanRouteEndingInAWalk() {
        SortedSet<JourneyDTO> journeys = validateJourneyToLocation( Stations.Deansgate.getId(), nearAltrincham, LocalTime.of(20,9));
        JourneyDTO firstJourney = journeys.first();
        List<StageDTO> stages = firstJourney.getStages();
        assertEquals(2, stages.size());
        StageDTO walkingStage = stages.get(1);

        assertEquals(Stations.NavigationRoad.getId(), walkingStage.getFirstStation().getId());
        assertEquals(nearAltrincham, walkingStage.getLastStation().getLatLong());
        assertEquals(14, walkingStage.getDuration());
    }

    @Test
    public void shouldGiveWalkingRouteFromMyLocationToNearbyStop() {
        SortedSet<JourneyDTO> journeys = validateJourneyFromLocation(nearAltrincham, Stations.Altrincham.getId(),
                LocalTime.of(22, 9));
        assertTrue(journeys.size()>0);
        JourneyDTO first = journeys.first();

        List<StageDTO> stages = first.getStages();
        assertEquals(1, stages.size());
        StageDTO walkingStage = stages.get(0);
        assertEquals(TramTime.of(22,9), walkingStage.getFirstDepartureTime());
    }

    @Test
    public void shouldGiveWalkingRouteFromStationToMyLocation() {
        SortedSet<JourneyDTO> journeys = validateJourneyToLocation(Stations.Altrincham.getId(), nearAltrincham,
                LocalTime.of(22, 9));
        assertTrue(journeys.size()>0);
        JourneyDTO first = journeys.first();

        List<StageDTO> stages = first.getStages();
        assertEquals(1, stages.size());
        StageDTO walkingStage = stages.get(0);
        assertEquals(TramTime.of(22,9), walkingStage.getFirstDepartureTime());
    }


    @Test
    public void shouldFindStationsNearPiccGardensWalkingOnly() {
        SortedSet<JourneyDTO> journeys = validateJourneyFromLocation(nearPiccGardens, Stations.PiccadillyGardens.getId(),
                LocalTime.of(9,0));

        assertTrue(journeys.size()>0);
        JourneyDTO first = journeys.first();
        List<StageDTO> stages = first.getStages();
        assertEquals(journeys.toString(), TramTime.of(9,0), first.getFirstDepartureTime());
        assertEquals(journeys.toString(), TramTime.of(9,3), first.getExpectedArrivalTime());
        assertEquals(Stations.PiccadillyGardens.getId(), first.getEnd().getId());

        assertEquals(1, stages.size());
        StageDTO stage = stages.get(0);
        assertEquals(TramTime.of(9,0), stage.getFirstDepartureTime());
        assertEquals(TramTime.of(9,3), stage.getExpectedArrivalTime());
    }

    @Test
    public void reproduceIssueNearAltyToAshton()  {
        SortedSet<JourneyDTO> journeys = validateJourneyFromLocation(nearAltrincham,
                Stations.Ashton.getId(), LocalTime.of(19,47));

        journeys.forEach(journey -> {
            assertEquals(Stations.Ashton.getId(), journey.getEnd().getId());
            assertEquals(3, journey.getStages().size());
        });
    }

    @Test
    public void shouldFindRouteNearEndOfServiceTimes() {
        Location destination = Stations.Deansgate;

        LocalTime queryTime = LocalTime.of(23,00);
        int walkingTime = 13;
        JourneyPlanRepresentation directFromStationNoWalking = getPlanFor(Stations.NavigationRoad, destination,
                queryTime.plusMinutes(walkingTime));
        assertTrue(directFromStationNoWalking.getJourneys().size()>0);
        // now check walking
        validateJourneyFromLocation(nearAltrincham, destination.getId(), queryTime);
    }

    private JourneyPlanRepresentation getPlanFor(Location start, Location end, LocalTime time) {
        String date = when.format(dateFormatDashes);
        String timeString = time.format(DateTimeFormatter.ofPattern(TIME_PATTERN));
        Response response = JourneyPlannerResourceTest.getResponseForJourney(testRule, start.getId(), end.getId(), timeString, date, null);
        Assert.assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    private SortedSet<JourneyDTO> validateJourneyFromLocation(LatLong location, String destination, LocalTime queryTime) {

        String date = when.format(dateFormatDashes);
        String time = queryTime.format(DateTimeFormatter.ofPattern(TIME_PATTERN));

        Response response = JourneyPlannerResourceTest.getResponseForJourney(testRule,
                MyLocationFactory.MY_LOCATION_PLACEHOLDER_ID, destination, time, date, location);
        Assert.assertEquals(200, response.getStatus());

        return validateJourneyPresent(response);
    }

    private SortedSet<JourneyDTO> validateJourneyToLocation(String startId, LatLong location, LocalTime queryTime) {
        String date = when.format(dateFormatDashes);
        String time = queryTime.format(DateTimeFormatter.ofPattern(TIME_PATTERN));

        Response response = JourneyPlannerResourceTest.getResponseForJourney(testRule, startId,
                MyLocationFactory.MY_LOCATION_PLACEHOLDER_ID, time, date, location);
        Assert.assertEquals(200, response.getStatus());

        return validateJourneyPresent(response);

    }

    private SortedSet<JourneyDTO> validateJourneyPresent(Response response) {
        JourneyPlanRepresentation plan = response.readEntity(JourneyPlanRepresentation.class);
        SortedSet<JourneyDTO> journeys = plan.getJourneys();
        assertTrue(journeys.size()>=1);
        List<StageDTO> stages = journeys.first().getStages();
        assertTrue(stages.size()>0);
        stages.forEach(stage -> assertTrue(stage.toString(),stage.getDuration()>0));
        return journeys;
    }

}
