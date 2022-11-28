package com.tramchester.integration.resources.journeyPlanning;

import com.tramchester.App;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.JourneyResourceTestFacade;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.livedata.tfgm.ProvidesTramNotes;
import com.tramchester.resources.JourneyPlannerResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIn.oneOf;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
public class JourneyPlannerResourceTest {

    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new ResourceTramTestConfig<>(JourneyPlannerResource.class));

    private TramDate when;
    private JourneyResourceTestFacade journeyPlanner;

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
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

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, queryTime, Altrincham, Cornbrook, arriveBy, 0);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = plan.getJourneys();
        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            StageDTO firstStage = journey.getStages().get(0);
            PlatformDTO platform = firstStage.getPlatform();
            if (arriveBy) {
                assertTrue(journey.getFirstDepartureTime().isBefore(queryTime.toDate(when)));
            } else {
                assertTrue(journey.getFirstDepartureTime().isAfter(queryTime.toDate(when)));
            }
            assertEquals(when.toLocalDate(), journey.getQueryDate());

            assertEquals("1", platform.getPlatformNumber());
            assertEquals("Altrincham platform 1", platform.getName());
            assertEquals(Altrincham.getRawId() + "1", platform.getId());

            journey.getStages().forEach(stage -> assertEquals(when.toLocalDate(), stage.getQueryDate()));
        });
    }

    @Test
    void shouldNotFindAnyResultsIfNoneTramModeIsRequested() {

        TramTime queryTime = TramTime.of(8,15);

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, queryTime, TramStations.Altrincham,
                TramStations.Cornbrook, false, 0);
        query.setModes(Collections.singleton(TransportMode.Train));

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);

        assertTrue(plan.getJourneys().isEmpty());

    }

    @Test
    void shouldNotFindAnyResultsIfNoneTramModeIsRequestedArriveBy() {

        TramTime queryTime = TramTime.of(8,15);

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, queryTime, TramStations.Altrincham,
                TramStations.Cornbrook, true, 0);
        query.setModes(Collections.singleton(TransportMode.Train));

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);

        assertTrue(plan.getJourneys().isEmpty());

    }

    @Test
    void shouldPlanSimpleJourneyArriveByHasAtLeastOneDepartByRequiredTime() {
        TramTime queryTime = TramTime.of(11,45);

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, queryTime, TramStations.Altrincham, TramStations.Cornbrook, true, 0);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);

        List<JourneyDTO> found = new ArrayList<>();
        plan.getJourneys().forEach(journeyDTO -> {
            assertTrue(journeyDTO.getFirstDepartureTime().isBefore(queryTime.toDate(when)));
            // TODO lockdown less frequent services during lockdown mean threshhold here increased to 12
            Duration duration = Duration.between(journeyDTO.getExpectedArrivalTime(), queryTime.toDate(when));
            if (duration.getSeconds()<=(12*60)) {
                found.add(journeyDTO);
            }
            assertEquals(when.toLocalDate(), journeyDTO.getQueryDate());
        });
        Assertions.assertFalse(found.isEmpty(), "no journeys found");
    }

    @Test
    void shouldGetNoResultsToAirportWhenLimitOnChanges() {

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, TramTime.of(11,45), Altrincham, ManAirport, true, 0);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);
        assertTrue(plan.getJourneys().isEmpty());
    }

    @Test
    void shouldReproLateNightIssueShudehillToAltrincham() {

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, TramTime.of(23,11), Shudehill, Altrincham, false, 3);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = plan.getJourneys();
        assertFalse(journeys.isEmpty(), "no journeys");
        journeys.forEach(journeyDTO ->
                assertTrue(journeyDTO.getExpectedArrivalTime().isAfter(journeyDTO.getFirstDepartureTime())));
    }

    @Test
    void shouldPlanSimpleJourneyFromAltyToAshton() {

        // note: Cornbrook, StPetersSquare, Deansgate all valid but have same cost

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, TramTime.of(17, 45), Altrincham, Ashton, false, 1);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = plan.getJourneys();
        assertTrue(journeys.size()>0);

        journeys.forEach(journey -> {
            StageDTO firstStage = journey.getStages().get(0);
            PlatformDTO stategOnePlatform = firstStage.getPlatform();

            assertEquals("1", stategOnePlatform.getPlatformNumber());
            assertEquals( "Altrincham platform 1", stategOnePlatform.getName());
            assertEquals( TramStations.Altrincham.getRawId()+"1", stategOnePlatform.getId());

            StageDTO secondStage = journey.getStages().get(1);
            PlatformDTO secondStagePlatform = secondStage.getPlatform();

            // seems can be either 1 or 2
            String platformNumber = secondStagePlatform.getPlatformNumber();
            assertTrue("12".contains(platformNumber));
            // multiple possible places to change depending on timetable etc
            assertThat(secondStagePlatform.getName(), is(oneOf(
                    "Cornbrook platform 1",
                    "Deansgate-Castlefield platform 1",
                    "Piccadilly platform 1", // summer 2021 only?
                    "St Peter's Square platform 2")));
            assertThat( secondStagePlatform.getId(), is(oneOf(Cornbrook.getRawId()+platformNumber,
                    Deansgate.getRawId()+platformNumber,
                    StPetersSquare.getRawId()+platformNumber,
                    Piccadilly.getRawId()+platformNumber // <- summer 2021 only?
            )));
        });

    }

    @Test
    void testAltyToManAirportHasRealisticTranferAtCornbrook() {
        TramDate nextSunday = TestEnv.nextSunday();

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(nextSunday, TramTime.of(11, 0),
                Altrincham, ManAirport, false, 3);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = results.getJourneys();

        assertTrue(journeys.size()>0, "no journeys");
        checkDepartsAfterPreviousArrival("Altrincham to airport at 11:43 sunday", journeys);
    }

    @Test
    void shouldWarnOnSaturdayAndSundayJourney() {

        Note weekendNote = new Note("At the weekend your journey may be affected by improvement works."
                + ProvidesTramNotes.website, Note.NoteType.Weekend);

        JourneyQueryDTO query2 = journeyPlanner.getQueryDTO(TestEnv.nextSunday(), TramTime.of(11, 43), Altrincham, ManAirport, false, 3);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(query2);

        results.getJourneys().forEach(journeyDTO -> assertThat(journeyDTO.getNotes(), hasItem(weekendNote)));

        JourneyQueryDTO query1 = journeyPlanner.getQueryDTO(TestEnv.nextSaturday(), TramTime.of(11, 43), Altrincham, ManAirport, false, 3);

        results = journeyPlanner.getJourneyPlan(query1);

        results.getJourneys().forEach(journeyDTO -> assertThat(journeyDTO.getNotes(), hasItem(weekendNote)));

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(TestEnv.nextMonday(), TramTime.of(11, 43), Altrincham, ManAirport, false, 3);

        JourneyPlanRepresentation notWeekendResult = journeyPlanner.getJourneyPlan(query);

        notWeekendResult.getJourneys().forEach(journeyDTO -> assertThat(journeyDTO.getNotes(), not(hasItem(weekendNote))));

    }

    @Test
    void shouldFindRouteVicToShawAndCrompton() {
        validateAtLeastOneJourney(Victoria, ShawAndCrompton, when, TramTime.of(23,15));
    }

    @Test
    void shouldFindRouteDeansgateToVictoria() {
        validateAtLeastOneJourney(Deansgate, Victoria, when, TramTime.of(23,41));
    }

    @Test
    void shouldFindEndOfDayTwoStageJourney() {
        validateAtLeastOneJourney(TraffordCentre, TraffordBar, when, TramTime.of(23,30));
    }

    @Test
    void shouldFindEndOfDayThreeStageJourney() {
        validateAtLeastOneJourney(Altrincham, ShawAndCrompton, when, TramTime.of(22,45));
    }

    @Test
    void shouldOnlyReturnFullJourneysForEndOfDaysJourney() {
        JourneyPlanRepresentation results = validateAtLeastOneJourney(Deansgate,
                ManAirport, when, TramTime.of(23,5));

        assertTrue(results.getJourneys().size()>0);
    }

    @Test
    void shouldFilterOutJourneysWithSameDepartArrivePathButDiffChanges() {

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(TestEnv.testDay(), TramTime.of(10, 43), Altrincham, Ashton, false, 1);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = plan.getJourneys();
        Set<Triple<LocalDateTime, LocalDateTime, List<LocationRefWithPosition>>> filtered = journeys.stream().
                map(dto -> Triple.of(dto.getFirstDepartureTime(), dto.getExpectedArrivalTime(), dto.getPath())).
                collect(Collectors.toSet());

        assertEquals(filtered.size(), journeys.size(), "Not same " + journeys + " filtered " + filtered);
    }

    @Test
    void shouldHaveFirstResultWithinReasonableTimeOfQuery() {

        TramTime queryTime = TramTime.of(17,45);

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(TestEnv.testDay(), TramTime.ofHourMins(queryTime.asLocalTime()), Altrincham, Ashton, false, 3);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = results.getJourneys();

        Optional<TramTime> earliest = journeys.stream().map(JourneyDTO::getFirstDepartureTime).
                map(LocalDateTime::toLocalTime).
                map(TramTime::ofHourMins).
                min(TramTime.comparing(time->time));

        assertTrue(earliest.isPresent());

        final TramTime firstDepartTime = earliest.get();
        Duration elapsed = TramTime.difference(queryTime, firstDepartTime);
        assertTrue(Durations.lessThan(elapsed,Duration.ofMinutes(15)), "first result too far in future " + firstDepartTime);
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

    private JourneyPlanRepresentation validateAtLeastOneJourney(TramStations start, TramStations end,
                                                                TramDate date, TramTime queryTime)  {

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(date, queryTime, start, end, false, 3);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = results.getJourneys();

        String message = String.format("from %s to %s at %s on %s", start, end, queryTime, date);
        assertTrue(journeys.size() > 0, "Unable to find journey " + message);
        checkDepartsAfterPreviousArrival(message, journeys);
        journeys.forEach(journey -> assertFalse(journey.getStages().isEmpty(), "Missing stages for journey"+journey));
        return results;
    }

}
