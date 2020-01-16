package com.tramchester.integration.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.tramchester.App;
import com.tramchester.TestConfig;
import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.Location;
import com.tramchester.domain.MyLocationFactory;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
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
import java.util.List;
import java.util.SortedSet;

import static com.tramchester.TestConfig.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MyLocationJourneyPlannerTest {

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
        JourneyDTO first = journeys.first();

        List<StageDTO> stages = first.getStages();
        assertEquals(2, stages.size());
        StageDTO walkingStage = stages.get(0);
        assertTrue(walkingStage.getFirstDepartureTime().between(TramTime.of(20,9), TramTime.of(20,15)));

        assertTrue(walkingStage.toString(),walkingStage.getExpectedArrivalTime().between(TramTime.of(20,13), TramTime.of(20,18)));
        assertTrue(first.getFirstDepartureTime().toString(), first.getFirstDepartureTime().isAfter(TramTime.of(20,22)));
    }

    @Test
    public void shouldGiveWalkingRouteFromMyLocationToNearbyStop() throws TramchesterException {
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
//        String time = LocalTime.MIDNIGHT.plusMinutes(minsPastMid).format(DateTimeFormatter.ofPattern("HH:mm:00"));
        String timeString = time.format(DateTimeFormatter.ofPattern("HH:mm:00"));
        Response response = JourneyPlannerResourceTest.getResponseForJourney(testRule, start.getId(), end.getId(), timeString, date, null);
        Assert.assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    private SortedSet<JourneyDTO> validateJourneyFromLocation(LatLong location, String destination, LocalTime queryTime) {

        String date = when.format(dateFormatDashes);
        String time = queryTime.format(DateTimeFormatter.ofPattern("HH:mm:00"));

        Response response = JourneyPlannerResourceTest.getResponseForJourney(testRule,
                MyLocationFactory.MY_LOCATION_PLACEHOLDER_ID, destination, time, date, location);
        Assert.assertEquals(200, response.getStatus());

        JourneyPlanRepresentation plan = response.readEntity(JourneyPlanRepresentation.class);
        SortedSet<JourneyDTO> journeys = plan.getJourneys();
        assertTrue(journeys.size()>=1);
        List<StageDTO> stages = journeys.first().getStages();
        assertTrue(stages.size()>0);
        stages.forEach(stage -> assertTrue(stage.toString(),stage.getDuration()>0));

        return journeys;
    }

}
