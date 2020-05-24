package com.tramchester.integration.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.tramchester.App;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocationFactory;
import com.tramchester.domain.Timestamped;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.LiveDataTestCategory;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.*;
import org.junit.experimental.categories.Category;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;

import static com.tramchester.testSupport.TestEnv.dateFormatDashes;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Fail.fail;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIn.oneOf;
import static org.joda.time.DateTimeConstants.SATURDAY;
import static org.joda.time.DateTimeConstants.SUNDAY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class JourneyPlannerResourceTest extends JourneyPlannerHelper {

    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    private ObjectMapper mapper = new ObjectMapper();
    private LocalDate when;
    private LocalDateTime now;

    @Before
    public void beforeEachTestRuns() {
        when = TestEnv.nextTuesday(0);
        now = TestEnv.LocalNow();
    }

    @Test
    public void shouldPlanSimpleJourneyFromAltyToCornbrook() {
        TramTime departTime = TramTime.of(8, 15);
        checkAltyToCornbrook(departTime, false);
    }

    @Test
    public void shouldPlanSimpleJourneyFromAltyToCornbrookArriveBy() {
        TramTime arriveByTime = TramTime.of(8, 15);
        checkAltyToCornbrook(arriveByTime, true);
    }

    private void checkAltyToCornbrook(TramTime queryTime, boolean arriveBy) {
        JourneyPlanRepresentation plan = getJourneyPlan(Stations.Altrincham, Stations.Cornbrook, new TramServiceDate(when), queryTime,
                arriveBy, 3);

        SortedSet<JourneyDTO> journeys = plan.getJourneys();
        assertTrue(journeys.size() > 0);
        JourneyDTO journey = journeys.first();
        StageDTO firstStage = journey.getStages().get(0);
        DTO platform = firstStage.getPlatform();
        if (arriveBy) {
            assertTrue(journey.getFirstDepartureTime().isBefore(queryTime));
        } else {
            assertTrue(journey.getFirstDepartureTime().isAfter(queryTime));
        }

        assertEquals("1", platform.getPlatformNumber());
        assertEquals("Altrincham platform 1", platform.getName());
        assertEquals(Stations.Altrincham.getId() + "1", platform.getId());
    }

    @Test
    public void shouldPlanSimpleJourneyArriveByHasAtLeastOneDepartByRequiredTime() {
        TramTime queryTime = TramTime.of(11,45);
        JourneyPlanRepresentation plan = getJourneyPlan(Stations.Altrincham, Stations.Cornbrook, new TramServiceDate(when), queryTime,
                true, 3);

        List<JourneyDTO> found = new ArrayList<>();
        plan.getJourneys().forEach(journeyDTO -> {
            assertTrue(journeyDTO.getFirstDepartureTime().isBefore(queryTime));
            // TODO lockdown less frequent services during lockdown mean threshhold here increased to 12
            if (TramTime.diffenceAsMinutes(journeyDTO.getExpectedArrivalTime(),queryTime)<=12) {
                found.add(journeyDTO);
            }
        });
        assertFalse("no journeys found", found.isEmpty());
    }

    @Test
    public void shouldGetNoResultsToAirportWhenLimitOnChanges() {
        TramTime queryTime = TramTime.of(11,45);
        JourneyPlanRepresentation plan = getJourneyPlan(Stations.Altrincham, Stations.ManAirport, new TramServiceDate(when), queryTime,
                true, 0);
        assertTrue(plan.getJourneys().isEmpty());
    }

    @Test
    @Category(LiveDataTestCategory.class)
    public void shouldPlanSimpleJourneyFromAltyToCornbrookLiveDepartureInfo() {

        // has to be NOW to get a hit on live data
        LocalTime currentLocalTime = ZonedDateTime.now(TramchesterConfig.TimeZone).toLocalTime();
        TramTime timeForQuery = TramTime.of(currentLocalTime);

        JourneyPlanRepresentation plan = getJourneyPlan(Stations.Altrincham, Stations.Cornbrook, new TramServiceDate(now.toLocalDate()), timeForQuery,
                false, 3);

        SortedSet<JourneyDTO> journeys = plan.getJourneys();
        assertTrue(journeys.size()>0);
        JourneyDTO journey = journeys.first();
        StageDTO firstStage = journey.getStages().get(0);
        DTO platform = firstStage.getPlatform();

        // depends on up to date departure info and current query time
        StationDepartureInfoDTO departInfo = platform.getStationDepartureInfo();
        assertNotNull("departInfo was null",departInfo);
        String expected = Stations.Altrincham.getId() + "1";
        assertEquals("got "+departInfo.getStationPlatform(), expected,departInfo.getStationPlatform());
    }

    @Ignore("Temporary: trams finish at 2300")
    @Test
    public void shouldReproLateNightIssueShudehillToAltrincham() {
        TramTime timeForQuery = TramTime.of(23,11);
        JourneyPlanRepresentation plan = getJourneyPlan(Stations.Shudehill, Stations.Altrincham, new TramServiceDate(now.toLocalDate()), timeForQuery,
                false, 3);

        SortedSet<JourneyDTO> journeys = plan.getJourneys();
        assertTrue(journeys.size()>0);
        journeys.forEach(journeyDTO -> {
            assertTrue(journeyDTO.getExpectedArrivalTime().isAfter(journeyDTO.getFirstDepartureTime()));
        });
    }

    @Test
    public void shouldPlanSimpleJourneyFromAltyToAshton() {

        JourneyPlanRepresentation plan = getJourneyPlan(Stations.Altrincham, Stations.Ashton, new TramServiceDate(when), TramTime.of(17,45),
                false, 3);

        SortedSet<JourneyDTO> journeys = plan.getJourneys();
        assertTrue(journeys.size()>0);
        JourneyDTO journey = journeys.first();

        StageDTO firstStage = journey.getStages().get(0);
        DTO platform1 = firstStage.getPlatform();

        assertEquals("1", platform1.getPlatformNumber());
        assertEquals( "Altrincham platform 1", platform1.getName());
        assertEquals( Stations.Altrincham.getId()+"1", platform1.getId());

        StageDTO secondStage = journey.getStages().get(1);
        DTO platform2 = secondStage.getPlatform();

        assertEquals("1", platform2.getPlatformNumber());
        // multiple possible places to change depending on timetable etc
        assertThat(platform2.getName(), is(oneOf("Piccadilly platform 1", "Cornbrook platform 1", "St Peter's Square platform 1")));
        assertThat( platform2.getId(), is(oneOf(Stations.Piccadilly.getId()+"1",
                Stations.Cornbrook.getId()+"1",
                Stations.StPetersSquare.getId()+"1")));
    }

    @Test
    public void testAltyToManAirportHasRealisticTranferAtCornbrook() throws TramchesterException {
        int offsetToSunday = SUNDAY - when.getDayOfWeek().getValue();
        LocalDate nextSunday = when.plusDays(offsetToSunday).plusWeeks(1);

        JourneyPlanRepresentation results = getJourneyPlan(Stations.Altrincham, Stations.ManAirport,
                TramTime.of(11,00), nextSunday);

        Set<JourneyDTO> journeys = results.getJourneys();

        assertTrue(journeys.size()>0);
        checkDepartsAfterPreviousArrival("Altrincham to airport at 11:43 sunday", journeys);
    }

    @Test
    public void shouldWarnOnSaturdayAndSundayJourney() throws TramchesterException {
        int offsetToSunday = SUNDAY-when.getDayOfWeek().getValue();
        LocalDate nextSunday = when.plusDays(offsetToSunday);

        JourneyPlanRepresentation results = getJourneyPlan(Stations.Altrincham, Stations.ManAirport,
                TramTime.of(11,43), nextSunday);

        List<Note> notes = results.getNotes();
        assertEquals(2, notes.size()); // include station closure message
        Note weekendNote = new Note(Note.NoteType.Weekend,"At the weekend your journey may be affected by improvement works."+ProvidesNotes.website);
        assertThat(notes, hasItem(weekendNote));

        int offsetToSaturday = SATURDAY-when.getDayOfWeek().getValue();
        LocalDate nextSaturday = when.plusDays(offsetToSaturday);

        results = getJourneyPlan(Stations.Altrincham, Stations.ManAirport,
                TramTime.of(11,43), nextSaturday);

        notes = results.getNotes();
        assertEquals(2,notes.size());
        assertThat(notes, hasItem(weekendNote));

        JourneyPlanRepresentation notWeekendResult = getJourneyPlan(Stations.Altrincham, Stations.ManAirport,
                TramTime.of(11,43), nextSunday.plusDays(1));
        notes = notWeekendResult.getNotes();
        assertEquals(1,notes.size());
        assertThat(notes, not(hasItem(weekendNote)));
    }

    @Ignore("Temporary: trams finish at 2300")
    @Test
    public void shouldFindRouteVicToShawAndCrompton() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Victoria, Stations.ShawAndCrompton, when, TramTime.of(23,34));
    }

    @Ignore("Temporary: trams finish at 2300")
    @Test
    public void shouldFindRouteDeansgateToVictoria() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Deansgate, Stations.Victoria, when, TramTime.of(23,41));
    }

    @Ignore("Temporary: trams finish at 2300")
    @Test
    public void shouldFindEndOfDayTwoStageJourney() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Altrincham, Stations.ManAirport, when, TramTime.of(22,56));
    }

    @Ignore("Temporary: trams finish at 2300")
    @Test
    public void shouldFindEndOfDayThreeStageJourney() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Altrincham, Stations.ShawAndCrompton, when, TramTime.of(22,45));
    }

    @Ignore("Temporary: trams finish at 2300")
    @Test
    public void shouldOnlyReturnFullJourneysForEndOfDaysJourney() throws TramchesterException {
        JourneyPlanRepresentation results = validateAtLeastOneJourney(Stations.Deansgate,
                Stations.ManAirport, when, TramTime.of(23,5));

        Assert.assertTrue(results.getJourneys().size()>0);
    }

    @Test
    public void shouldSetCookieForRecentJourney() throws IOException {
        String start = Stations.Bury.getId();
        String end = Stations.ManAirport.getId();
        String time = now.toLocalTime().format(TestEnv.timeFormatter);
        String date = now.toLocalDate().format(dateFormatDashes);
        Response result = getResponseForJourney(testRule, start, end, time, date, null, false, 3);

        assertEquals(200, result.getStatus());

        RecentJourneys recentJourneys = getRecentJourneysFromCookie(result);

        assertEquals(2,recentJourneys.getRecentIds().size());
        assertTrue(recentJourneys.getRecentIds().contains(new Timestamped(start, now)));
        assertTrue(recentJourneys.getRecentIds().contains(new Timestamped(end, now)));
    }

    @Test
    public void shouldUdateCookieForRecentJourney() throws IOException {
        String start = Stations.Bury.getId();
        String end = Stations.ManAirport.getId();
        String time = now.toLocalTime().format(TestEnv.timeFormatter);
        String date = now.toLocalDate().format(dateFormatDashes);

        // cookie with ashton
        RecentJourneys recentJourneys = new RecentJourneys();
        Timestamped ashton = new Timestamped(Stations.Ashton.getId(), now);
        recentJourneys.setRecentIds(Sets.newHashSet(ashton));
        Cookie cookie = new Cookie("tramchesterRecent", RecentJourneys.encodeCookie(mapper,recentJourneys));

        // journey to bury
        Response response = IntegrationClient.getResponse(testRule,
                String.format("journey?start=%s&end=%s&departureTime=%s&departureDate=%s", start, end, time, date),
                Optional.of(cookie), 200);

        assertEquals(200, response.getStatus());

        RecentJourneys result = getRecentJourneysFromCookie(response);

        // ashton, bury and man airport now in cookie
        Set<Timestamped> recents = result.getRecentIds();
        assertEquals(3, recents.size());
        assertTrue(recents.contains(new Timestamped(start, now)));
        assertTrue(recents.contains(ashton));
        assertTrue(recents.contains(new Timestamped(end, now)));
    }

    @Test
    public void shouldOnlyCookiesForDestinationIfLocationSent() throws IOException {
        LatLong latlong = new LatLong(53.3949553,-2.3580997999999997 );
        String start = MyLocationFactory.MY_LOCATION_PLACEHOLDER_ID;
        String end = Stations.ManAirport.getId();
        String time = now.toLocalTime().format(TestEnv.timeFormatter);
        String date = now.toLocalDate().format(dateFormatDashes);
        Response response = getResponseForJourney(testRule, start, end, time, date, latlong, false, 3);

        assertEquals(200, response.getStatus());
        RecentJourneys result = getRecentJourneysFromCookie(response);
        Set<Timestamped> recents = result.getRecentIds();
        assertEquals(1, recents.size());
        // checks ID only
        assertTrue(recents.contains(new Timestamped(end, now)));
    }

    private RecentJourneys getRecentJourneysFromCookie(Response response) throws IOException {
        Map<String, NewCookie> cookies = response.getCookies();
        NewCookie recent = cookies.get("tramchesterRecent");
        assertNotNull(recent);
        String value = recent.toCookie().getValue();
        return RecentJourneys.decodeCookie(mapper,value);
    }

    protected JourneyPlanRepresentation getJourneyPlan(Location start, Location end, TramServiceDate queryDate, TramTime queryTime,
                                                       boolean arriveBy, int maxChanges) {
        return getJourneyPlanRepresentation(testRule, start, end, queryDate, queryTime, arriveBy, maxChanges);
    }

    public static JourneyPlanRepresentation getJourneyPlanRepresentation(IntegrationTestRun rule, Location start, Location end,
                                                                         TramServiceDate queryDate, TramTime queryTime,
                                                                         boolean arriveBy, int maxChanges) {
        String date = queryDate.getDate().format(dateFormatDashes);
        String time = queryTime.asLocalTime().format(TestEnv.timeFormatter);
        Response response = getResponseForJourney(rule, start.getId(), end.getId(), time, date,
                null, arriveBy, maxChanges);
        assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    public static Response getResponseForJourney(IntegrationTestRun rule, String start, String end, String time,
                                                 String date, LatLong latlong, boolean arriveBy, int maxChanges) {
        String queryString = String.format("journey?start=%s&end=%s&departureTime=%s&departureDate=%s&arriveby=%s&maxChanges=%s",
                start, end, time, date, arriveBy, maxChanges);

        if (MyLocationFactory.MY_LOCATION_PLACEHOLDER_ID.equals(start) || MyLocationFactory.MY_LOCATION_PLACEHOLDER_ID.equals(end)) {
            if (latlong==null) {
                fail("must provide latlong");
            }
            queryString = String.format("%s&lat=%f&lon=%f", queryString, latlong.getLat(), latlong.getLon());
        }
        return IntegrationClient.getResponse(rule, queryString, Optional.empty(), 200);
    }
}
