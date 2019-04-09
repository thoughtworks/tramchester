package com.tramchester.integration.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.tramchester.App;
import com.tramchester.TestConfig;
import com.tramchester.domain.Location;
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.SortedSet;

import static com.tramchester.TestConfig.dateFormatDashes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MyLocationJourneyPlannerTest {

    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    private ObjectMapper mapper = new ObjectMapper();
    private LocalDate when;

    private LatLong nearPiccGardens;
    private LatLong nearAltrincham;

    @Before
    public void beforeEachTestRuns() {
        when = TestConfig.nextTuesday(0);
        mapper.registerModule(new JodaModule());
        nearPiccGardens = new LatLong(53.4805248D, -2.2394929D);
        nearAltrincham = new LatLong(53.394982299999995D,-2.3581502D);
    }

    @Test
    public void shouldFindStationsNearPiccGardensToExchangeSquare() throws JsonProcessingException, UnsupportedEncodingException {
        validateJourneyFromLocation(nearPiccGardens, Stations.ExchangeSquare.getId(), 9 * 60);
    }

    @Test
    public void planRouteAllowingForWalkingTime() throws JsonProcessingException, UnsupportedEncodingException, TramchesterException {
        SortedSet<JourneyDTO> journeys = validateJourneyFromLocation(nearAltrincham, Stations.Deansgate.getId(), (20 * 60) + 9);
        assertTrue(journeys.size()>0);
        JourneyDTO first = journeys.first();

        List<StageDTO> stages = first.getStages();
        assertEquals(2, stages.size());
        StageDTO walkingStage = stages.get(0);
        assertEquals(TramTime.create(20,9), walkingStage.getFirstDepartureTime());

        TramTime walkArrives = TramTime.create(20, 22);
        assertEquals(walkArrives, walkingStage.getExpectedArrivalTime());
        assertTrue(first.getFirstDepartureTime().asLocalTime().isAfter(walkArrives.asLocalTime()));
    }

    @Test
    public void shouldGiveWalkingRouteFromMyLocationToNearbyStop() throws JsonProcessingException, UnsupportedEncodingException, TramchesterException {
        SortedSet<JourneyDTO> journeys = validateJourneyFromLocation(nearAltrincham, Stations.Altrincham.getId(), (22 * 60) + 9);
        assertTrue(journeys.size()>0);
        JourneyDTO first = journeys.first();

        List<StageDTO> stages = first.getStages();
        assertEquals(1, stages.size());
        StageDTO walkingStage = stages.get(0);
        assertEquals(TramTime.create(22,9), walkingStage.getFirstDepartureTime());
    }

    @Test
    public void shouldFindStationsNearPiccGardensWalkingOnly() throws JsonProcessingException, UnsupportedEncodingException, TramchesterException {
        SortedSet<JourneyDTO> journeys = validateJourneyFromLocation(nearPiccGardens, Stations.PiccadillyGardens.getId(), 9 * 60);

        assertTrue(journeys.size()>0);
        JourneyDTO first = journeys.first();
        List<StageDTO> stages = first.getStages();
        assertEquals(TramTime.create(9,00), first.getFirstDepartureTime());
        assertEquals(TramTime.create(9,03), first.getExpectedArrivalTime());
        assertEquals(Stations.PiccadillyGardens.getId(), first.getEnd().getId());

        assertEquals(1, stages.size());
        StageDTO stage = stages.get(0);
        assertEquals(TramTime.create(9,00), stage.getFirstDepartureTime());
        assertEquals(TramTime.create(9,03), stage.getExpectedArrivalTime());
    }

    @Test
    public void reproduceIssueNearAltyToAshton() throws JsonProcessingException, UnsupportedEncodingException {
        SortedSet<JourneyDTO> journeys = validateJourneyFromLocation(nearAltrincham,
                Stations.Ashton.getId(), (19 * 60) +47);

        journeys.forEach(journey -> {
            assertEquals(Stations.Ashton.getId(), journey.getEnd().getId());
            assertEquals(3, journey.getStages().size());
        });
    }

    @Test
    public void shouldFindRouteNearEndOfServiceTimes() throws JsonProcessingException, UnsupportedEncodingException {
        Location destination = Stations.Deansgate;

        int queryTime = 23 * 60;
        int walkingTime = 13;
        JourneyPlanRepresentation direct = getPlanFor(Stations.NavigationRoad, destination, queryTime+walkingTime);
        assertTrue(direct.getJourneys().size()>0);

        validateJourneyFromLocation(nearAltrincham, destination.getId(), queryTime);
    }

    private JourneyPlanRepresentation getPlanFor(Location start, Location end, int minsPastMid) {
        String date = when.format(dateFormatDashes);
        String time = LocalTime.MIDNIGHT.plusMinutes(minsPastMid).format(DateTimeFormatter.ofPattern("HH:mm:00"));
        Response response = JourneyPlannerResourceTest.getResponseForJourney(testRule, start.getId(), end.getId(), time, date);
        Assert.assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    private SortedSet<JourneyDTO> validateJourneyFromLocation(LatLong location, String destination, int queryTime)
            throws JsonProcessingException, UnsupportedEncodingException {
        String startId = URLEncoder.encode(JourneyPlannerTest.formId(location), "UTF-8");

        String date = when.format(dateFormatDashes);
        String time = LocalTime.MIDNIGHT.plusMinutes(queryTime).format(DateTimeFormatter.ofPattern("HH:mm:00"));
        Response response = JourneyPlannerResourceTest.getResponseForJourney(testRule, startId, destination, time, date);
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
