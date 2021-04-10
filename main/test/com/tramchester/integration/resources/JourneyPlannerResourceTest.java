package com.tramchester.integration.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.tramchester.App;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.Timestamped;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.IntegrationClient;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.ParseStream;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tramchester.testSupport.TestEnv.dateFormatDashes;
import static org.assertj.core.api.Fail.fail;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIn.oneOf;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
public class JourneyPlannerResourceTest extends JourneyPlannerHelper {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new IntegrationTramTestConfig());

    private final ObjectMapper mapper = new ObjectMapper();
    private LocalDate when;
    private LocalDateTime now;
    private ParseStream<JourneyDTO> parseStream;
    private TramServiceDate tramServiceDate;

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
        tramServiceDate = new TramServiceDate(when);
        now = TestEnv.LocalNow();
        parseStream = new ParseStream<>(mapper);
    }

    @Test
    void shouldPlanSimpleJourneyFromAltyToCornbrook() {
        TramTime departTime = TramTime.of(8, 15);
        checkAltyToCornbrook(departTime, false);
    }

    @Test
    void shouldPlanSimpleJourneyFromAltyToCornbrookArriveBy() {
        TramTime arriveByTime = TramTime.of(8, 15);
        checkAltyToCornbrook(arriveByTime, true);
    }

    private void checkAltyToCornbrook(TramTime queryTime, boolean arriveBy) {
        JourneyPlanRepresentation plan = getJourneyPlan(TramStations.Altrincham, TramStations.Cornbrook, tramServiceDate,
                queryTime.asLocalTime(), arriveBy, 0);

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

            journey.getStages().forEach(stage -> {
                assertEquals(when, stage.getQueryDate());
            });
        });
    }

    @Test
    void shouldPlanSimpleJourneyArriveByHasAtLeastOneDepartByRequiredTime() {
        TramTime queryTime = TramTime.of(11,45);
        JourneyPlanRepresentation plan = getJourneyPlan(TramStations.Altrincham, TramStations.Cornbrook, tramServiceDate,
                queryTime.asLocalTime(), true, 0);

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
        LocalTime queryTime = LocalTime.of(11,45);
        JourneyPlanRepresentation plan = getJourneyPlan(TramStations.Altrincham, TramStations.ManAirport, tramServiceDate,
                queryTime, true, 0);
        assertTrue(plan.getJourneys().isEmpty());
    }

    @Test
    void shouldReproLateNightIssueShudehillToAltrincham() {
        LocalTime timeForQuery = LocalTime.of(23,11);
        JourneyPlanRepresentation plan = getJourneyPlan(TramStations.Shudehill, TramStations.Altrincham,
                tramServiceDate, timeForQuery, false, 3);

        Set<JourneyDTO> journeys = plan.getJourneys();
        assertFalse(journeys.isEmpty(), "no journeys");
        journeys.forEach(journeyDTO -> assertTrue(journeyDTO.getExpectedArrivalTime().isAfter(journeyDTO.getFirstDepartureTime())));
    }

    @Test
    void shouldPlanSimpleJourneyFromAltyToAshton() {

        JourneyPlanRepresentation plan = getJourneyPlan(TramStations.Altrincham, TramStations.Ashton, tramServiceDate,
                LocalTime.of(17,45), false, 1);

        Set<JourneyDTO> journeys = plan.getJourneys();
        assertTrue(journeys.size()>0);

        journeys.forEach(journey -> {
            StageDTO firstStage = journey.getStages().get(0);
            PlatformDTO platform1 = firstStage.getPlatform();

            Assertions.assertEquals("1", platform1.getPlatformNumber());
            Assertions.assertEquals( "Altrincham platform 1", platform1.getName());
            Assertions.assertEquals( TramStations.Altrincham.forDTO()+"1", platform1.getId());

            StageDTO secondStage = journey.getStages().get(1);
            PlatformDTO platform2 = secondStage.getPlatform();

            Assertions.assertEquals("1", platform2.getPlatformNumber());
            // multiple possible places to change depending on timetable etc
            assertThat(platform2.getName(), is(oneOf("Piccadilly platform 1",
                    "Cornbrook platform 1",
                    "St Peter's Square platform 1", "Piccadilly Gardens platform 1")));
            assertThat( platform2.getId(), is(oneOf(TramStations.Piccadilly.forDTO()+"1",
                    TramStations.Cornbrook.forDTO()+"1",
                    TramStations.StPetersSquare.forDTO()+"1",
                    TramStations.PiccadillyGardens.forDTO()+"1")));
        });

    }

    @Test
    void testAltyToManAirportHasRealisticTranferAtCornbrook() {
        LocalDate nextSunday = TestEnv.nextSunday();

        JourneyPlanRepresentation results = getJourneyPlan(TramStations.Altrincham, TramStations.ManAirport,
                TramTime.of(11,0), nextSunday);

        Set<JourneyDTO> journeys = results.getJourneys();

        assertTrue(journeys.size()>0, "no journeys");
        checkDepartsAfterPreviousArrival("Altrincham to airport at 11:43 sunday", journeys);
    }

    @Test
    void shouldWarnOnSaturdayAndSundayJourney() {

        Note weekendNote = new Note("At the weekend your journey may be affected by improvement works."
                + ProvidesNotes.website, Note.NoteType.Weekend);

        JourneyPlanRepresentation results = getJourneyPlan(TramStations.Altrincham, TramStations.ManAirport,
                TramTime.of(11, 43), TestEnv.nextSunday());

        results.getJourneys().forEach(journeyDTO -> assertThat(journeyDTO.getNotes(), hasItem(weekendNote)));

        results = getJourneyPlan(TramStations.Altrincham, TramStations.ManAirport,
                TramTime.of(11, 43), TestEnv.nextSaturday());

        results.getJourneys().forEach(journeyDTO -> assertThat(journeyDTO.getNotes(), hasItem(weekendNote)));

        JourneyPlanRepresentation notWeekendResult = getJourneyPlan(TramStations.Altrincham, TramStations.ManAirport,
                TramTime.of(11, 43), TestEnv.nextMonday());

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
    void shouldSetCookieForRecentJourney() throws IOException {
        IdFor<Station> start = TramStations.Bury.getId();
        IdFor<Station> end = TramStations.ManAirport.getId();

        Response result = getResponseForJourney(appExtension, start, end, now.toLocalTime(), now.toLocalDate(), null,
                false, 3);

        Assertions.assertEquals(200, result.getStatus());

        RecentJourneys recentJourneys = getRecentJourneysFromCookie(result);

        Assertions.assertEquals(2,recentJourneys.getRecentIds().size());
        assertTrue(recentJourneys.getRecentIds().contains(new Timestamped(start, now)));
        assertTrue(recentJourneys.getRecentIds().contains(new Timestamped(end, now)));
    }

    @Test
    void shouldUdateCookieForRecentJourney() throws IOException {
        IdFor<Station> start = TramStations.Bury.getId();
        IdFor<Station> end = TramStations.ManAirport.getId();
        String time = now.toLocalTime().format(TestEnv.timeFormatter);
        String date = now.toLocalDate().format(dateFormatDashes);

        // cookie with ashton
        RecentJourneys recentJourneys = new RecentJourneys();
        Timestamped ashton = new Timestamped(TramStations.Ashton.getId(), now);
        recentJourneys.setRecentIds(Sets.newHashSet(ashton));
        Cookie cookie = new Cookie("tramchesterRecent", RecentJourneys.encodeCookie(mapper,recentJourneys));

        // journey to bury
        Response response = IntegrationClient.getApiResponse(appExtension,
                String.format("journey?start=%s&end=%s&departureTime=%s&departureDate=%s", start.forDTO(), end.forDTO(), time, date),cookie);

        Assertions.assertEquals(200, response.getStatus());

        RecentJourneys result = getRecentJourneysFromCookie(response);

        // ashton, bury and man airport now in cookie
        Set<Timestamped> recents = result.getRecentIds();
        Assertions.assertEquals(3, recents.size());
        assertTrue(recents.contains(new Timestamped(start, now)));
        assertTrue(recents.contains(ashton));
        assertTrue(recents.contains(new Timestamped(end, now)));
    }

    @Test
    void shouldOnlyCookiesForDestinationIfLocationSent() throws IOException {
        LatLong latlong = new LatLong(53.3949553,-2.3580997999999997 );
        String start = MyLocation.MY_LOCATION_PLACEHOLDER_ID;
        IdFor<Station> end = TramStations.ManAirport.getId();

        Response response = getResponseForJourney(appExtension, start, end.forDTO(), now.toLocalTime(),  now.toLocalDate(),
                latlong, false, 3);
        Assertions.assertEquals(200, response.getStatus());

        RecentJourneys result = getRecentJourneysFromCookie(response);
        Set<Timestamped> recents = result.getRecentIds();
        Assertions.assertEquals(1, recents.size());
        // checks ID only
        assertTrue(recents.contains(new Timestamped(end, now)));
    }

    @Test
    void shouldSpikeResultsAsStream() throws IOException {
        IdFor<Station> start = TramStations.Bury.getId();
        IdFor<Station> end = TramStations.ManAirport.getId();
        String time = TramTime.of(11,45).toPattern();
        String date = when.format(dateFormatDashes);

        String queryString = String.format("journey/streamed?start=%s&end=%s&departureTime=%s&departureDate=%s&arriveby=%s&maxChanges=%s",
                start.forDTO(), end.forDTO(), time, date, false, 3);

        Response response = IntegrationClient.getApiResponse(appExtension, queryString);
        Assertions.assertEquals(200, response.getStatus());

        InputStream inputStream = response.readEntity(InputStream.class);
        List<JourneyDTO> journeyDTOS = parseStream.receive(response, inputStream, JourneyDTO.class);

        Assertions.assertFalse(journeyDTOS.isEmpty());
        journeyDTOS.forEach(journeyDTO -> Assertions.assertFalse(journeyDTO.getStages().isEmpty()));
    }

    private RecentJourneys getRecentJourneysFromCookie(Response response) throws IOException {
        Map<String, NewCookie> cookies = response.getCookies();
        NewCookie recent = cookies.get("tramchesterRecent");
        assertNotNull(recent);
        Assertions.assertEquals("/api",recent.getPath());
        Assertions.assertEquals("localhost", recent.getDomain());
        String value = recent.toCookie().getValue();
        return RecentJourneys.decodeCookie(mapper,value);
    }

    protected JourneyPlanRepresentation getJourneyPlan(TramStations start, TramStations end, TramServiceDate queryDate, LocalTime queryTime,
                                                       boolean arriveBy, int maxChanges) {
        return getJourneyPlanRepresentation(appExtension, start, end, queryDate, queryTime, arriveBy, maxChanges);
    }

    static JourneyPlanRepresentation getJourneyPlanRepresentation(IntegrationAppExtension rule, TramStations start,
                                                                  TramStations end,
                                                                  TramServiceDate queryDate, LocalTime queryTime,
                                                                  boolean arriveBy, int maxChanges) {
        Response response = getResponseForJourney(rule, start.forDTO(), end.forDTO(), queryTime, queryDate.getDate(),
                null, arriveBy, maxChanges);
        Assertions.assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    static Response getResponseForJourney(IntegrationAppExtension rule, IdFor<Station> start, IdFor<Station> end,
                                          LocalTime time, LocalDate date, LatLong latlong, boolean arriveBy, int maxChanges) {
        return getResponseForJourney(rule, start.forDTO(), end.forDTO(), time, date, latlong, arriveBy, maxChanges);

    }

    public static Response getResponseForJourney(IntegrationAppExtension rule, String start, String end, LocalTime time,
                                                 LocalDate date, LatLong latlong, boolean arriveBy, int maxChanges) {
        String timeString = time.format(TestEnv.timeFormatter);
        String dateString = date.format(dateFormatDashes);

        String queryString = String.format("journey?start=%s&end=%s&departureTime=%s&departureDate=%s&arriveby=%s&maxChanges=%s",
                start, end, timeString, dateString, arriveBy, maxChanges);

        if (MyLocation.MY_LOCATION_PLACEHOLDER_ID.equals(start) || MyLocation.MY_LOCATION_PLACEHOLDER_ID.equals(end)) {
            if (latlong==null) {
                fail("must provide latlong");
            } else {
                queryString = String.format("%s&lat=%f&lon=%f", queryString, latlong.getLat(), latlong.getLon());
            }
        }
        return IntegrationClient.getApiResponse(rule, queryString);
    }
}
