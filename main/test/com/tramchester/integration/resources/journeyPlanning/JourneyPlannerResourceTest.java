package com.tramchester.integration.resources.journeyPlanning;

import com.tramchester.App;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.JourneyResourceTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIn.oneOf;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
public class JourneyPlannerResourceTest {

    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new IntegrationTramTestConfig());

    private LocalDate when;
    private TramServiceDate tramServiceDate;
    private JourneyResourceTestFacade journeyPlanner;

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
        tramServiceDate = new TramServiceDate(when);
        journeyPlanner = new JourneyResourceTestFacade(appExtension);
    }

    @Test
    void shouldPlanSimpleJourneyFromAltyToCornbrook() {
        checkAltyToCornbrook(TramTime.of(8, 15), false);
    }

    @Test
    void shouldPlanSimpleJourneyFromAltyToCornbrookArriveBy() {
        TramTime arriveByTime = TramTime.of(8, 15);
        checkAltyToCornbrook(arriveByTime, true);
    }

    private void checkAltyToCornbrook(TramTime queryTime, boolean arriveBy) {
        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(tramServiceDate.getDate(),
                queryTime, TramStations.Altrincham, TramStations.Cornbrook, arriveBy, 0);

        Set<JourneyDTO> journeys = plan.getJourneys();
        assertTrue(journeys.size() > 0);

        journeys.forEach(journey -> {
            StageDTO firstStage = journey.getStages().get(0);
            PlatformDTO platform = firstStage.getPlatform();
            if (arriveBy) {
                assertTrue(journey.getFirstDepartureTime().isBefore(queryTime.toDate(when)));
            } else {
                assertTrue(journey.getFirstDepartureTime().isAfter(queryTime.toDate(when)));
            }
            assertEquals(when, journey.getQueryDate());

            assertEquals("1", platform.getPlatformNumber());
            assertEquals("Altrincham platform 1", platform.getName());
            assertEquals(TramStations.Altrincham.forDTO() + "1", platform.getId());

            journey.getStages().forEach(stage -> assertEquals(when, stage.getQueryDate()));
        });
    }

    @Test
    void shouldPlanSimpleJourneyArriveByHasAtLeastOneDepartByRequiredTime() {
        TramTime queryTime = TramTime.of(11,45);
        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(tramServiceDate.getDate(), queryTime,
                TramStations.Altrincham, TramStations.Cornbrook, true, 0);

        List<JourneyDTO> found = new ArrayList<>();
        plan.getJourneys().forEach(journeyDTO -> {
            assertTrue(journeyDTO.getFirstDepartureTime().isBefore(queryTime.toDate(when)));
            // TODO lockdown less frequent services during lockdown mean threshhold here increased to 12
            Duration duration = Duration.between(journeyDTO.getExpectedArrivalTime(), queryTime.toDate(when));
            if (duration.getSeconds()<=(12*60)) {
                found.add(journeyDTO);
            }
            assertEquals(when, journeyDTO.getQueryDate());
        });
        Assertions.assertFalse(found.isEmpty(), "no journeys found");
    }

    @Test
    void shouldGetNoResultsToAirportWhenLimitOnChanges() {
        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(tramServiceDate.getDate(), TramTime.of(11,45),
                TramStations.Altrincham, TramStations.ManAirport, true, 0);
        assertTrue(plan.getJourneys().isEmpty());
    }

    @Test
    void shouldReproLateNightIssueShudehillToAltrincham() {
        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(tramServiceDate.getDate(), TramTime.of(23,11),
                TramStations.Shudehill, TramStations.Altrincham, false, 3);

        Set<JourneyDTO> journeys = plan.getJourneys();
        assertFalse(journeys.isEmpty(), "no journeys");
        journeys.forEach(journeyDTO -> assertTrue(journeyDTO.getExpectedArrivalTime().isAfter(journeyDTO.getFirstDepartureTime())));
    }

    @Test
    void shouldPlanSimpleJourneyFromAltyToAshton() {

        // note: Cornbrook, StPetersSquare, Deansgate all valid but have same cost

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(tramServiceDate.getDate(),
                TramTime.of(17, 45), TramStations.Altrincham, TramStations.Ashton, false, 1);

        Set<JourneyDTO> journeys = plan.getJourneys();
        assertTrue(journeys.size()>0);

        journeys.forEach(journey -> {
            StageDTO firstStage = journey.getStages().get(0);
            PlatformDTO platform1 = firstStage.getPlatform();

            assertEquals("1", platform1.getPlatformNumber());
            assertEquals( "Altrincham platform 1", platform1.getName());
            assertEquals( TramStations.Altrincham.forDTO()+"1", platform1.getId());

            StageDTO secondStage = journey.getStages().get(1);
            PlatformDTO platform2 = secondStage.getPlatform();

            // seems can be either 1 or 2
            String platformNumber = platform2.getPlatformNumber();
            assertTrue("12".contains(platformNumber));
            // multiple possible places to change depending on timetable etc
            assertThat(platform2.getName(), is(oneOf(
                    "Cornbrook platform 1",
                    "Deansgate-Castlefield platform 1",
                    "Piccadilly platform 1", // summer 2021 only
                    "St Peter's Square platform 2")));
            assertThat( platform2.getId(), is(oneOf(Cornbrook.forDTO()+platformNumber,
                    Deansgate.forDTO()+platformNumber,
                    StPetersSquare.forDTO()+platformNumber,
                    Piccadilly.forDTO()+platformNumber // <- summer 2021 only
            )));
        });

    }

    @Test
    void testAltyToManAirportHasRealisticTranferAtCornbrook() {
        LocalDate nextSunday = TestEnv.nextSunday();

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(new TramServiceDate(nextSunday).getDate(), TramTime.of(TramTime.of(11, 0).asLocalTime()), TramStations.Altrincham, TramStations.ManAirport, false, 3);

        Set<JourneyDTO> journeys = results.getJourneys();

        assertTrue(journeys.size()>0, "no journeys");
        checkDepartsAfterPreviousArrival("Altrincham to airport at 11:43 sunday", journeys);
    }

    @Test
    void shouldWarnOnSaturdayAndSundayJourney() {

        Note weekendNote = new Note("At the weekend your journey may be affected by improvement works."
                + ProvidesNotes.website, Note.NoteType.Weekend);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(new TramServiceDate(TestEnv.nextSunday()).getDate(), TramTime.of(TramTime.of(11, 43).asLocalTime()), TramStations.Altrincham, TramStations.ManAirport, false, 3);

        results.getJourneys().forEach(journeyDTO -> assertThat(journeyDTO.getNotes(), hasItem(weekendNote)));

        results = journeyPlanner.getJourneyPlan(new TramServiceDate(TestEnv.nextSaturday()).getDate(), TramTime.of(TramTime.of(11, 43).asLocalTime()), TramStations.Altrincham, TramStations.ManAirport, false, 3);

        results.getJourneys().forEach(journeyDTO -> assertThat(journeyDTO.getNotes(), hasItem(weekendNote)));

        JourneyPlanRepresentation notWeekendResult = journeyPlanner.getJourneyPlan(new TramServiceDate(TestEnv.nextMonday()).getDate(), TramTime.of(TramTime.of(11, 43).asLocalTime()), TramStations.Altrincham, TramStations.ManAirport, false, 3);

        notWeekendResult.getJourneys().forEach(journeyDTO -> assertThat(journeyDTO.getNotes(), not(hasItem(weekendNote))));

    }

    @Test
    void shouldFindRouteVicToShawAndCrompton() {
        validateAtLeastOneJourney(TramStations.Victoria, TramStations.ShawAndCrompton, when, TramTime.of(23,15));
    }

    @Test
    void shouldFindRouteDeansgateToVictoria() {
        validateAtLeastOneJourney(TramStations.Deansgate, TramStations.Victoria, when, TramTime.of(23,41));
    }

    @Test
    void shouldFindEndOfDayTwoStageJourney() {
        validateAtLeastOneJourney(TramStations.Intu, TramStations.TraffordBar, when, TramTime.of(23,30));
    }

    @Test
    void shouldFindEndOfDayThreeStageJourney() {
        validateAtLeastOneJourney(TramStations.Altrincham, TramStations.ShawAndCrompton, when, TramTime.of(22,45));
    }

    @Test
    void shouldOnlyReturnFullJourneysForEndOfDaysJourney() {
        JourneyPlanRepresentation results = validateAtLeastOneJourney(TramStations.Deansgate,
                TramStations.ManAirport, when, TramTime.of(23,5));

        assertTrue(results.getJourneys().size()>0);
    }

    @Test
    void shouldHaveFirstResultWithinReasonableTimeOfQuery() {

        TramTime queryTime = TramTime.of(17,45);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(new TramServiceDate(TestEnv.testDay()).getDate(), TramTime.of(queryTime.asLocalTime()), Altrincham, Ashton, false, 3);

        Set<JourneyDTO> journeys = results.getJourneys();

        Optional<TramTime> earliest = journeys.stream().map(JourneyDTO::getFirstDepartureTime).
                map(LocalDateTime::toLocalTime).
                map(TramTime::of).
                min(TramTime.comparing(time->time));

        assertTrue(earliest.isPresent());

        final TramTime firstDepartTime = earliest.get();
        int elapsed = TramTime.diffenceAsMinutes(queryTime, firstDepartTime);
        assertTrue(elapsed<15, "first result too far in future " + firstDepartTime);
    }

    private void checkDepartsAfterPreviousArrival(String message, Set<JourneyDTO> journeys) {
        for(JourneyDTO journey: journeys) {
            LocalDateTime previousArrive = null;
            for(StageDTO stage : journey.getStages()) {
                if (previousArrive!=null) {
                    LocalDateTime firstDepartureTime = stage.getFirstDepartureTime();
                    String prefix  = String.format("Check first departure time %s is after arrival time %s for %s" ,
                            firstDepartureTime, previousArrive, stage);
                    if (stage.getMode()!= TransportMode.Walk) {
                        assertTrue(firstDepartureTime.isAfter(previousArrive), prefix + message);
                    }
                }
                previousArrive = stage.getExpectedArrivalTime();
            }
        }
    }

    private JourneyPlanRepresentation validateAtLeastOneJourney(TramStations start, TramStations end, LocalDate date, TramTime queryTime)  {
        TramServiceDate queryDate = new TramServiceDate(date);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(queryDate.getDate(), TramTime.of(queryTime.asLocalTime()),
                start, end, false, 3);
        Set<JourneyDTO> journeys = results.getJourneys();

        String message = String.format("from %s to %s at %s on %s", start, end, queryTime, queryDate);
        assertTrue(journeys.size() > 0, "Unable to find journey " + message);
        checkDepartsAfterPreviousArrival(message, journeys);
        journeys.forEach(journey -> assertFalse(journey.getStages().isEmpty(), "Missing stages for journey"+journey));
        return results;
    }

}
