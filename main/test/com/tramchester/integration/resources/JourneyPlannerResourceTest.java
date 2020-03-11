package com.tramchester.integration.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.Sets;
import com.tramchester.App;
import com.tramchester.testSupport.LiveDataTestCategory;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.Stations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
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

import static com.tramchester.testSupport.TestConfig.dateFormatDashes;
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

    @Before
    public void beforeEachTestRuns() {
        when = TestConfig.nextTuesday(0);
        // todo NO longer needed?
        mapper.registerModule(new JodaModule());
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

    @Test
    public void shouldPlanSimpleJourneyArriveByHasAtLeastOneDepartByRequiredTime() {
        TramTime queryTime = TramTime.of(11,45);
        JourneyPlanRepresentation plan = getJourneyPlan(Stations.Altrincham, Stations.Cornbrook, queryTime,
                new TramServiceDate(when), true);

        List<JourneyDTO> found = new ArrayList<>();
        plan.getJourneys().forEach(journeyDTO -> {
            assertTrue(journeyDTO.getFirstDepartureTime().isBefore(queryTime));
            if (journeyDTO.getExpectedArrivalTime().isBefore(queryTime)) {
                found.add(journeyDTO);
            }
        });
        assertFalse(found.isEmpty());
    }

    private void checkAltyToCornbrook(TramTime queryTime, boolean arriveBy) {
        JourneyPlanRepresentation plan = getJourneyPlan(Stations.Altrincham, Stations.Cornbrook, queryTime,
                new TramServiceDate(when), arriveBy);

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
    @Category(LiveDataTestCategory.class)
    public void shouldPlanSimpleJourneyFromAltyToCornbrookLiveDepartureInfo() {

        // has to be NOW to get a hit on live data
        LocalTime currentLocalTime = ZonedDateTime.now(TramchesterConfig.TimeZone).toLocalTime();
        TramTime timeForQuery = TramTime.of(currentLocalTime);

        JourneyPlanRepresentation plan = getJourneyPlan(Stations.Altrincham, Stations.Cornbrook, timeForQuery,
                new TramServiceDate(LocalDate.now()), false);

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

    @Test
    public void shouldReproLateNightIssueShudehillToAltrincham() {
        TramTime timeForQuery = TramTime.of(23,11);
        JourneyPlanRepresentation plan = getJourneyPlan(Stations.Shudehill, Stations.Altrincham, timeForQuery,
                new TramServiceDate(LocalDate.now()), false);

        SortedSet<JourneyDTO> journeys = plan.getJourneys();
        assertTrue(journeys.size()>0);
        journeys.forEach(journeyDTO -> {
            assertTrue(journeyDTO.getExpectedArrivalTime().isAfter(journeyDTO.getFirstDepartureTime()));
        });
    }

    @Test
    public void shouldPlanSimpleJourneyFromAltyToAshton() {

        JourneyPlanRepresentation plan = getJourneyPlan(Stations.Altrincham, Stations.Ashton, TramTime.of(17,45),
                new TramServiceDate(when), false);

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

        List<String> notes = results.getNotes();
        assertEquals(2, notes.size()); // include station closure message
        String prefix = "At the weekend your journey may be affected by improvement works."+ProvidesNotes.website;
        assertThat(notes, hasItem(prefix));

        int offsetToSaturday = SATURDAY-when.getDayOfWeek().getValue();
        LocalDate nextSaturday = when.plusDays(offsetToSaturday);

        results = getJourneyPlan(Stations.Altrincham, Stations.ManAirport,
                TramTime.of(11,43), nextSaturday);

        notes = results.getNotes();
        assertEquals(2,notes.size());
        assertThat(notes, hasItem(prefix));

        JourneyPlanRepresentation notWeekendResult = getJourneyPlan(Stations.Altrincham, Stations.ManAirport,
                TramTime.of(11,43), nextSunday.plusDays(1));
        notes = notWeekendResult.getNotes();
        assertEquals(1,notes.size());
        assertThat(notes, not(hasItem(prefix)));
    }

    @Test
    public void shouldFindRouteVicToShawAndCrompton() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Victoria, Stations.ShawAndCrompton, TramTime.of(23,34), when);
    }

    @Test
    public void shouldFindRouteDeansgateToVictoria() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Deansgate, Stations.Victoria, TramTime.of(23,41), when);
    }

    @Test
    public void shouldFindEndOfDayTwoStageJourney() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Altrincham, Stations.ManAirport, TramTime.of(22,56), when);
    }

    @Test
    public void shouldFindEndOfDayThreeStageJourney() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Altrincham, Stations.ShawAndCrompton, TramTime.of(22,45), when);
    }

    @Test
    public void shouldOnlyReturnFullJourneysForEndOfDaysJourney() throws TramchesterException {
        JourneyPlanRepresentation results = validateAtLeastOneJourney(Stations.Deansgate,
                Stations.ManAirport, TramTime.of(23,5), when);

        Assert.assertTrue(results.getJourneys().size()>0);
    }

    @Test
    public void shouldSetCookieForRecentJourney() throws IOException {
        String start = Stations.Bury.getId();
        String end = Stations.ManAirport.getId();
        String time = LocalTime.now().format(TestConfig.timeFormatter);
        String date = LocalDate.now().format(dateFormatDashes);
        Response result = getResponseForJourney(testRule, start, end, time, date, null, false);

        assertEquals(200, result.getStatus());

        RecentJourneys recentJourneys = getRecentJourneysFromCookie(result);

        assertEquals(2,recentJourneys.getRecentIds().size());
        assertTrue(recentJourneys.getRecentIds().contains(new Timestamped(start, LocalDateTime.now())));
        assertTrue(recentJourneys.getRecentIds().contains(new Timestamped(end, LocalDateTime.now())));
    }

    @Test
    public void shouldUdateCookieForRecentJourney() throws IOException {
        String start = Stations.Bury.getId();
        String end = Stations.ManAirport.getId();
        String time = LocalTime.now().format(TestConfig.timeFormatter);
        String date = LocalDate.now().format(dateFormatDashes);

        // cookie with ashton
        RecentJourneys recentJourneys = new RecentJourneys();
        Timestamped ashton = new Timestamped(Stations.Ashton.getId(), LocalDateTime.now());
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
        assertTrue(recents.contains(new Timestamped(start, LocalDateTime.now())));
        assertTrue(recents.contains(ashton));
        assertTrue(recents.contains(new Timestamped(end, LocalDateTime.now())));
    }

    @Test
    public void shouldOnlyCookiesForDestinationIfLocationSent() throws IOException {
        LatLong latlong = new LatLong(53.3949553,-2.3580997999999997 );
        String start = MyLocationFactory.MY_LOCATION_PLACEHOLDER_ID;
        String end = Stations.ManAirport.getId();
        String time = LocalTime.now().format(TestConfig.timeFormatter);
        String date = LocalDate.now().format(dateFormatDashes);
        Response response = getResponseForJourney(testRule, start, end, time, date, latlong, false);

        assertEquals(200, response.getStatus());
        RecentJourneys result = getRecentJourneysFromCookie(response);
        Set<Timestamped> recents = result.getRecentIds();
        assertEquals(1, recents.size());
        // checks ID only
        assertTrue(recents.contains(new Timestamped(end, LocalDateTime.now())));
    }

    private RecentJourneys getRecentJourneysFromCookie(Response response) throws IOException {
        Map<String, NewCookie> cookies = response.getCookies();
        NewCookie recent = cookies.get("tramchesterRecent");
        assertNotNull(recent);
        String value = recent.toCookie().getValue();
        return RecentJourneys.decodeCookie(mapper,value);
    }

    protected JourneyPlanRepresentation getJourneyPlan(Location start, Location end, TramTime queryTime,
                                                       TramServiceDate queryDate, boolean arriveBy) {
        return getJourneyPlanRepresentation(testRule, start, end, queryTime, queryDate, arriveBy);
    }

    public static JourneyPlanRepresentation getJourneyPlanRepresentation(IntegrationTestRun rule, Location start, Location end,
                                                                         TramTime queryTime,
                                                                         TramServiceDate queryDate, boolean arriveBy) {
        String date = queryDate.getDate().format(dateFormatDashes);
        String time = queryTime.asLocalTime().format(TestConfig.timeFormatter);
        Response response = getResponseForJourney(rule, start.getId(), end.getId(), time, date,
                null, arriveBy);
        assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    public static Response getResponseForJourney(IntegrationTestRun rule, String start, String end, String time,
                                                 String date, LatLong latlong, boolean arriveBy) {
        String queryString = String.format("journey?start=%s&end=%s&departureTime=%s&departureDate=%s&arriveby=%s",
                start, end, time, date, arriveBy);

        if (MyLocationFactory.MY_LOCATION_PLACEHOLDER_ID.equals(start) || MyLocationFactory.MY_LOCATION_PLACEHOLDER_ID.equals(end)) {
            if (latlong==null) {
                fail("must provide latlong");
            }
            queryString = String.format("%s&lat=%f&lon=%f", queryString, latlong.getLat(), latlong.getLon());
        }
        return IntegrationClient.getResponse(rule, queryString, Optional.empty(), 200);
    }
}
