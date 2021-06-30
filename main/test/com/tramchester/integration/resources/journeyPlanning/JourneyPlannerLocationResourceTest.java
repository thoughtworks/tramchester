package com.tramchester.integration.resources.journeyPlanning;

import com.tramchester.App;
import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.JourneyResourceTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStations;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.TramStations.Deansgate;
import static com.tramchester.testSupport.reference.TramStations.NavigationRoad;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class JourneyPlannerLocationResourceTest {

    private static final AppConfiguration config = new IntegrationTramTestConfig();
    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, config);

    private LocalDate when;
    private JourneyResourceTestFacade journeyPlanner;

    @BeforeEach
    void beforeEachTestRuns() {
        journeyPlanner = new JourneyResourceTestFacade(appExtension);
        when = TestEnv.testDay();
    }

    @Test
    void shouldFindStationsNearPiccGardensToExchangeSquare() {
        validateJourneyFromLocation(TestEnv.nearPiccGardens, TramStations.ExchangeSquare.getId(), TramTime.of(9,0),
                false);
    }

    @Test
    void planRouteAllowingForWalkingTime() {
        Set<JourneyDTO> journeys = validateJourneyFromLocation(TestEnv.nearAltrincham, Deansgate.getId(),
                TramTime.of(19,1), false);
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
        final int count = 20; // 2000
        for (int i = 0; i < count; i++) {
            TramTime localTime = TramTime.of(10, 15);
            Set<JourneyDTO> journeys = validateJourneyFromLocation(TestEnv.nearAltrincham, Deansgate.getId(),
                    localTime, false);
            assertTrue(journeys.size()>0);

            LocalDateTime planTime = localTime.toDate(when);
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
        TramTime queryTime = TramTime.of(20, 9);
        Set<JourneyDTO> journeys = validateJourneyFromLocation(TestEnv.nearAltrincham, Deansgate.getId(), queryTime, true);
        assertTrue(journeys.size()>0);

        journeys.forEach(journeyDTO -> {
            assertTrue(journeyDTO.getFirstDepartureTime().isBefore(queryTime.toDate(when)));

            List<StageDTO> stages = journeyDTO.getStages();
            assertEquals(2, stages.size());
        });

    }

    @Test
    void shouldPlanRouteEndingInAWalk() {
        final TramTime queryTime = TramTime.of(20, 9);
        Set<JourneyDTO> journeys = validateJourneyToLocation(Deansgate, TestEnv.nearAltrincham,
                queryTime, false);

        journeys.forEach(journeyDTO -> {
            assertTrue(journeyDTO.getFirstDepartureTime().isAfter(queryTime.toDate(when)));

            List<StageDTO> stages = journeyDTO.getStages();
            assertEquals(2, stages.size(), stages.toString());
            assertEquals(TransportMode.Tram, stages.get(0).getMode());
            assertEquals(TransportMode.Walk, stages.get(1).getMode());

            StageDTO walkingStage = stages.get(1);
            assertEquals(TestEnv.nearAltrincham, walkingStage.getLastStation().getLatLong());
        });
    }

    @Test
    void shouldPlanRouteEndingInAWalkArriveBy() {
        TramTime queryTime = TramTime.of(19, 9);
        Set<JourneyDTO> results = validateJourneyToLocation(Deansgate, TestEnv.nearAltrincham,
                queryTime, true);

        List<JourneyDTO> journeys = results.stream().
                filter(journeyDTO -> journeyDTO.getStages().size() == 2).collect(Collectors.toList());
        assertFalse(journeys.isEmpty());

        JourneyDTO firstJourney = journeys.get(0);
        assertTrue(firstJourney.getFirstDepartureTime().isBefore(queryTime.toDate(when)));

        List<StageDTO> stages = firstJourney.getStages();
        assertEquals(2, stages.size());
        assertEquals(TransportMode.Tram, stages.get(0).getMode());
        assertEquals(TransportMode.Walk, stages.get(1).getMode());
   }

    @Test
    void shouldGiveWalkingRouteFromMyLocationToNearbyStop() {
        Set<JourneyDTO> journeys = validateJourneyFromLocation(TestEnv.nearAltrincham, TramStations.Altrincham.getId(),
                TramTime.of(22, 9), false);
        assertTrue(journeys.size()>0);
        JourneyDTO first = getEarliestArrivingJourney(journeys);

        List<StageDTO> stages = first.getStages();
        assertEquals(1, stages.size());
        StageDTO walkingStage = stages.get(0);
        assertEquals(LocalDateTime.of(when,LocalTime.of(22,9)), walkingStage.getFirstDepartureTime());
    }

    @Test
    void shouldGiveWalkingRouteFromStationToMyLocation() {
        Set<JourneyDTO> journeys = validateJourneyToLocation(TramStations.Altrincham, TestEnv.nearAltrincham,
                TramTime.of(22, 9), false);
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
                TramTime.of(9,0), false);
        checkAltyToPiccGardens(journeys);
    }

    @Test
    void shouldFindStationsNearPiccGardensWalkingOnlyArriveBy() {
        TramTime queryTime = TramTime.of(9, 0);
        Set<JourneyDTO> journeys = validateJourneyFromLocation(TestEnv.nearPiccGardens, TramStations.PiccadillyGardens.getId(),
                queryTime, true);
        journeys.forEach(journeyDTO -> assertTrue(journeyDTO.getFirstDepartureTime().isBefore(queryTime.toDate(when))));
    }

    private void checkAltyToPiccGardens(Set<JourneyDTO> journeys) {
        assertTrue(journeys.size()>0);
        JourneyDTO first = getEarliestArrivingJourney(journeys);
        List<StageDTO> stages = first.getStages();
        assertEquals(LocalDateTime.of(when, LocalTime.of(9,0)), first.getFirstDepartureTime(), journeys.toString());
        assertEquals(LocalDateTime.of(when, LocalTime.of(9,3)), first.getExpectedArrivalTime(), journeys.toString());

        assertEquals(1, stages.size());
        StageDTO stage = stages.get(0);
        assertEquals(LocalDateTime.of(when,LocalTime.of(9,0)), stage.getFirstDepartureTime());
        assertEquals(LocalDateTime.of(when,LocalTime.of(9,3)), stage.getExpectedArrivalTime());
    }

    @Test
    void reproduceIssueNearAltyToAshton()  {
        Set<JourneyDTO> journeys = validateJourneyFromLocation(TestEnv.nearAltrincham, TramStations.Ashton.getId(),
                TramTime.of(19,47), false);

        journeys.forEach(journey -> assertEquals(3, journey.getStages().size()));
    }

    @Test
    void shouldFindRouteNearEndOfServiceTimes() {
        TramStations destination = Deansgate;

        TramTime queryTime = TramTime.of(23,0);
        int walkingTime = 13;

        JourneyPlanRepresentation directFromStationNoWalking =  journeyPlanner.getJourneyPlan(when, queryTime.plusMinutes(walkingTime),
                NavigationRoad, destination, false, 3);

        assertTrue(directFromStationNoWalking.getJourneys().size()>0);
        // now check walking
        validateJourneyFromLocation(TestEnv.nearAltrincham, destination.getId(), queryTime, false);
        validateJourneyFromLocation(TestEnv.nearAltrincham, destination.getId(), queryTime, true);
    }

    private Set<JourneyDTO> validateJourneyFromLocation(LatLong location, IdFor<Station> destination, TramTime queryTime, boolean arriveBy) {
        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(when, queryTime, location, destination, arriveBy, 3);
        return validateJourneyPresent(plan);
    }

    private Set<JourneyDTO> validateJourneyToLocation(TestStations start, LatLong destination, TramTime queryTime, boolean arriveBy) {
        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(when, queryTime, start, destination, arriveBy, 3);
        return validateJourneyPresent(plan);
    }

    @NotNull
    private Set<JourneyDTO> validateJourneyPresent(JourneyPlanRepresentation plan) {
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
