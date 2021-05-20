package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.graphdb.Transaction;

import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class JourneyPlannerLocationResourceTest {

    private static final AppConfiguration config = new IntegrationTramTestConfig();
    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, config);

    private LocalDate when;

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
    }

    @Test
    void shouldFindStationsNearPiccGardensToExchangeSquare() {
        validateJourneyFromLocation(TestEnv.nearPiccGardens, TramStations.ExchangeSquare.getId(), LocalTime.of(9,0),
                false);
    }

    @Test
    void planRouteAllowingForWalkingTime() {
        Set<JourneyDTO> journeys = validateJourneyFromLocation(TestEnv.nearAltrincham, Deansgate.getId(),
                LocalTime.of(19,1), false);
        assertTrue(journeys.size()>0);

        JourneyDTO firstJourney = getEarliestArrivingJourney(journeys);

        List<StageDTO> stages = firstJourney.getStages();
        assertEquals(2, stages.size());
        StageDTO walkingStage = stages.get(0);
        LocalDateTime departureTime = walkingStage.getFirstDepartureTime();

        // two walks result in same arrival time
        //List<TramTime> possibleTimes = Arrays.asList(TramTime.of(20, 19), TramTime.of(20, 12));

        // todo new lockdown timetable
        List<LocalDateTime> possibleTimes = Collections.singletonList(LocalDateTime.of(when, LocalTime.of(19, 5)));

        assertTrue(possibleTimes.contains(departureTime), "Expected time "+departureTime.toString());

        // assertEquals(firstJourney.toString(), TramTime.of(20,48), firstJourney.getExpectedArrivalTime());
        // todo new lockdown timetable
        assertEquals(LocalDateTime.of(when, LocalTime.of(19,34)), firstJourney.getExpectedArrivalTime(), firstJourney.toString());
    }

    private JourneyDTO getEarliestArrivingJourney(Set<JourneyDTO> journeys) {
        return journeys.stream().
                sorted(Comparator.comparing(JourneyDTO::getExpectedArrivalTime)).
                collect(Collectors.toList()).get(0);
    }

    @Test
    void reproCacheStalenessIssueWithNearAltyToDeansgate() {
        for (int i = 0; i <1000; i++) {
            LocalTime localTime = LocalTime.of(10, 15);
            Set<JourneyDTO> journeys = validateJourneyFromLocation(TestEnv.nearAltrincham, Deansgate.getId(),
                    localTime, false);
            assertTrue(journeys.size()>0);

            LocalDateTime planTime = localTime.atDate(when);
            for (JourneyDTO result : journeys) {
                LocalDateTime departTime = result.getFirstDepartureTime();
                assertTrue(departTime.isAfter(planTime), result.toString());

                LocalDateTime arriveTime = result.getExpectedArrivalTime();
                assertTrue(arriveTime.isAfter(departTime), result.toString());
            }
        }
    }

    @Test
    void planRouteAllowingForWalkingTimeArriveBy() {
        LocalTime queryTime = LocalTime.of(20, 9);
        Set<JourneyDTO> journeys = validateJourneyFromLocation(TestEnv.nearAltrincham, Deansgate.getId(), queryTime, true);
        assertTrue(journeys.size()>0);

        journeys.forEach(journeyDTO -> {
            assertTrue(journeyDTO.getFirstDepartureTime().isBefore(queryTime.atDate(when)));

            List<StageDTO> stages = journeyDTO.getStages();
            assertEquals(2, stages.size());
        });

    }

    @Test
    void shouldPlanRouteEndingInAWalk() {
        final LocalTime queryTime = LocalTime.of(20, 9);
        Set<JourneyDTO> journeys = validateJourneyToLocation(Deansgate.getId(), TestEnv.nearAltrincham,
                queryTime, false);

        journeys.forEach(journeyDTO -> {
            assertTrue(journeyDTO.getFirstDepartureTime().isAfter(queryTime.atDate(when)));

            List<StageDTO> stages = journeyDTO.getStages();
            assertEquals(2, stages.size());
            assertEquals(TransportMode.Tram, stages.get(0).getMode());
            assertEquals(TransportMode.Walk, stages.get(1).getMode());

            StageDTO walkingStage = stages.get(1);
            assertEquals(TestEnv.nearAltrincham, walkingStage.getLastStation().getLatLong());
        });
    }

    @Test
    void shouldPlanRouteEndingInAWalkArriveBy() {
        LocalTime queryTime = LocalTime.of(19, 9);
        Set<JourneyDTO> results = validateJourneyToLocation(Deansgate.getId(), TestEnv.nearAltrincham,
                queryTime, true);

        List<JourneyDTO> journeys = results.stream().
                filter(journeyDTO -> journeyDTO.getStages().size() == 2).collect(Collectors.toList());
        assertFalse(journeys.isEmpty());

        JourneyDTO firstJourney = journeys.get(0);
        assertTrue(firstJourney.getFirstDepartureTime().isBefore(queryTime.atDate(when)));

        List<StageDTO> stages = firstJourney.getStages();
        assertEquals(2, stages.size());
        assertEquals(TransportMode.Tram, stages.get(0).getMode());
        assertEquals(TransportMode.Walk, stages.get(1).getMode());
   }

    @Test
    void shouldGiveWalkingRouteFromMyLocationToNearbyStop() {
        Set<JourneyDTO> journeys = validateJourneyFromLocation(TestEnv.nearAltrincham, TramStations.Altrincham.getId(),
                LocalTime.of(22, 9), false);
        assertTrue(journeys.size()>0);
        JourneyDTO first = getEarliestArrivingJourney(journeys);

        List<StageDTO> stages = first.getStages();
        assertEquals(1, stages.size());
        StageDTO walkingStage = stages.get(0);
        assertEquals(LocalDateTime.of(when,LocalTime.of(22,9)), walkingStage.getFirstDepartureTime());
    }

    @Test
    void shouldGiveWalkingRouteFromStationToMyLocation() {
        Set<JourneyDTO> journeys = validateJourneyToLocation(TramStations.Altrincham.getId(), TestEnv.nearAltrincham,
                LocalTime.of(22, 9), false);
        assertTrue(journeys.size()>0);
        JourneyDTO first = getEarliestArrivingJourney(journeys);

        List<StageDTO> stages = first.getStages();
        assertEquals(1, stages.size());
        StageDTO walkingStage = stages.get(0);
        assertEquals(LocalDateTime.of(when,LocalTime.of(22,9)), walkingStage.getFirstDepartureTime());
    }

    @Test
    void shouldFindStationsNearPiccGardensWalkingOnly() {
        Set<JourneyDTO> journeys = validateJourneyFromLocation(TestEnv.nearPiccGardens, TramStations.PiccadillyGardens.getId(),
                LocalTime.of(9,0), false);
        checkAltyToPiccGardens(journeys);
    }

    @Test
    void shouldFindStationsNearPiccGardensWalkingOnlyArriveBy() {
        LocalTime queryTime = LocalTime.of(9, 0);
        Set<JourneyDTO> journeys = validateJourneyFromLocation(TestEnv.nearPiccGardens, TramStations.PiccadillyGardens.getId(),
                queryTime, true);
        journeys.forEach(journeyDTO -> assertTrue(journeyDTO.getFirstDepartureTime().isBefore(queryTime.atDate(when))));
    }

    private void checkAltyToPiccGardens(Set<JourneyDTO> journeys) {
        assertTrue(journeys.size()>0);
        JourneyDTO first = getEarliestArrivingJourney(journeys);
        List<StageDTO> stages = first.getStages();
        assertEquals(LocalDateTime.of(when, LocalTime.of(9,0)), first.getFirstDepartureTime(), journeys.toString());
        assertEquals(LocalDateTime.of(when, LocalTime.of(9,3)), first.getExpectedArrivalTime(), journeys.toString());
        assertEquals(TramStations.PiccadillyGardens.forDTO(), first.getEnd().getId());

        assertEquals(1, stages.size());
        StageDTO stage = stages.get(0);
        assertEquals(LocalDateTime.of(when,LocalTime.of(9,0)), stage.getFirstDepartureTime());
        assertEquals(LocalDateTime.of(when,LocalTime.of(9,3)), stage.getExpectedArrivalTime());
    }

    @Test
    void reproduceIssueNearAltyToAshton()  {
        Set<JourneyDTO> journeys = validateJourneyFromLocation(TestEnv.nearAltrincham, TramStations.Ashton.getId(),
                LocalTime.of(19,47), false);

        journeys.forEach(journey -> {
            assertEquals(TramStations.Ashton.forDTO(), journey.getEnd().getId());
            assertEquals(3, journey.getStages().size());
        });
    }

    @Test
    void shouldFindRouteNearEndOfServiceTimes() {
        Station destination = TramStations.of(Deansgate);

        LocalTime queryTime = LocalTime.of(23,0);
        int walkingTime = 13;
        JourneyPlanRepresentation directFromStationNoWalking = getPlanFor(TramStations.of(NavigationRoad), destination,
                queryTime.plusMinutes(walkingTime));
        assertTrue(directFromStationNoWalking.getJourneys().size()>0);
        // now check walking
        validateJourneyFromLocation(TestEnv.nearAltrincham, destination.getId(), queryTime, false);
        validateJourneyFromLocation(TestEnv.nearAltrincham, destination.getId(), queryTime, true);
    }

    private JourneyPlanRepresentation getPlanFor(Station start, Station end, LocalTime time) {
        Response response = JourneyPlannerResourceTest.getResponseForJourney(appExtension,
                start.forDTO(), end.forDTO(), time, when, null, false, 3);
        assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    private Set<JourneyDTO> validateJourneyFromLocation(LatLong location, IdFor<Station> destination, LocalTime queryTime, boolean arriveBy) {
        return validateJourneyFromLocation(location, destination.forDTO(), queryTime, arriveBy);
    }

    private Set<JourneyDTO> validateJourneyFromLocation(LatLong location, String destination, LocalTime queryTime, boolean arriveBy) {

        Response response = JourneyPlannerResourceTest.getResponseForJourney(appExtension,
                MyLocation.MY_LOCATION_PLACEHOLDER_ID, destination, queryTime, when, location, arriveBy, 3);
        assertEquals(200, response.getStatus());

        return validateJourneyPresent(response);
    }

    private Set<JourneyDTO> validateJourneyToLocation(IdFor<Station> start, LatLong destination, LocalTime queryTime, boolean arriveBy) {
        return validateJourneyToLocation(start.forDTO(), destination, queryTime, arriveBy);
    }

    private Set<JourneyDTO> validateJourneyToLocation(String startId, LatLong location, LocalTime queryTime, boolean arriveBy) {

        Response response = JourneyPlannerResourceTest.getResponseForJourney(appExtension, startId,
                MyLocation.MY_LOCATION_PLACEHOLDER_ID, queryTime, when, location, arriveBy, 3);
        assertEquals(200, response.getStatus());

        return validateJourneyPresent(response);

    }

    private Set<JourneyDTO> validateJourneyPresent(Response response) {
        JourneyPlanRepresentation plan = response.readEntity(JourneyPlanRepresentation.class);
        Set<JourneyDTO> journeys = plan.getJourneys();
        assertTrue(journeys.size()>=1, "no journeys");

        journeys.forEach(journeyDTO -> {
            List<StageDTO> stages = journeyDTO.getStages();
            assertTrue(stages.size()>0, "no stages");
            stages.forEach(stage -> assertTrue(stage.getDuration()>0, stage.toString()));
        });

        return journeys;
    }

}
