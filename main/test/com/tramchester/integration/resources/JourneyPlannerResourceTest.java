package com.tramchester.integration.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.Sets;
import com.tramchester.App;
import com.tramchester.LiveDataTestCategory;
import com.tramchester.TestConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
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
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.tramchester.TestConfig.dateFormatDashes;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Fail.fail;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIn.oneOf;
import static org.joda.time.DateTimeConstants.SATURDAY;
import static org.joda.time.DateTimeConstants.SUNDAY;
import static org.junit.Assert.assertEquals;

public class JourneyPlannerResourceTest extends JourneyPlannerHelper {

    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    private ObjectMapper mapper = new ObjectMapper();
    private LocalDate when;
    private DateTimeFormatter timeFormatter;

    @Before
    public void beforeEachTestRuns() {
        when = TestConfig.nextTuesday(0);
        mapper.registerModule(new JodaModule());
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm:00");
    }

    @Test
    public void shouldPlanSimpleJourneyFromAltyToCornbrook() {

        JourneyPlanRepresentation plan = getJourneyPlan(Stations.Altrincham, Stations.Cornbrook, LocalTime.of(8,15),
                new TramServiceDate(when));

        SortedSet<JourneyDTO> journeys = plan.getJourneys();
        assertTrue(journeys.size()>0);
        JourneyDTO journey = journeys.first();
        StageDTO firstStage = journey.getStages().get(0);
        PlatformDTO platform = firstStage.getPlatform();

        assertEquals("1", platform.getPlatformNumber());
        assertEquals( "Altrincham platform 1", platform.getName());
        assertEquals( Stations.Altrincham.getId()+"1", platform.getId());
    }

    @Test
    @Category(LiveDataTestCategory.class)
    public void shouldPlanSimpleJourneyFromAltyToCornbrookLiveDepartureInfo() {

        LocalTime timeForQuery = ZonedDateTime.now(TramchesterConfig.TimeZone).toLocalTime();

        JourneyPlanRepresentation plan = getJourneyPlan(Stations.Altrincham, Stations.Cornbrook, timeForQuery,
                new TramServiceDate(LocalDate.now()));

        SortedSet<JourneyDTO> journeys = plan.getJourneys();
        assertTrue(journeys.size()>0);
        JourneyDTO journey = journeys.first();
        StageDTO firstStage = journey.getStages().get(0);
        PlatformDTO platform = firstStage.getPlatform();

        // depends on up to date departure info and current query time
        StationDepartureInfo departInfo = platform.getStationDepartureInfo();
        assertNotNull("departInfo was null",departInfo);
        String expected = Stations.Altrincham.getId() + "1";
        assertEquals("got "+departInfo.getStationPlatform(), expected,departInfo.getStationPlatform());
    }

    @Test
    public void shouldReproLateNightIssueShudehillToAltrincham() {
        LocalTime timeForQuery = LocalTime.of(23,11);
        JourneyPlanRepresentation plan = getJourneyPlan(Stations.Shudehill, Stations.Altrincham, timeForQuery,
                new TramServiceDate(LocalDate.now()));

        SortedSet<JourneyDTO> journeys = plan.getJourneys();
        assertTrue(journeys.size()>0);
        journeys.forEach(journeyDTO -> {
            assertTrue(journeyDTO.getExpectedArrivalTime().isAfter(journeyDTO.getFirstDepartureTime()));
        });
    }

    @Test
    public void shouldPlanSimpleJourneyFromAltyToAshton() {

        JourneyPlanRepresentation plan = getJourneyPlan(Stations.Altrincham, Stations.Ashton, LocalTime.of(17,45),
                new TramServiceDate(when));

        SortedSet<JourneyDTO> journeys = plan.getJourneys();
        assertTrue(journeys.size()>0);
        JourneyDTO journey = journeys.first();

        StageDTO firstStage = journey.getStages().get(0);
        PlatformDTO platform1 = firstStage.getPlatform();

        assertEquals("1", platform1.getPlatformNumber());
        assertEquals( "Altrincham platform 1", platform1.getName());
        assertEquals( Stations.Altrincham.getId()+"1", platform1.getId());

        StageDTO secondStage = journey.getStages().get(1);
        PlatformDTO platform2 = secondStage.getPlatform();

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
                LocalTime.of(11,00), nextSunday);

        Set<JourneyDTO> journeys = results.getJourneys();

        assertTrue(journeys.size()>0);
        checkDepartsAfterPreviousArrival("Altrincham to airport at 11:43 sunday", journeys);
    }

    @Test
    public void shouldWarnOnSaturdayAndSundayJourney() throws TramchesterException {
        int offsetToSunday = SUNDAY-when.getDayOfWeek().getValue();
        LocalDate nextSunday = when.plusDays(offsetToSunday);

        JourneyPlanRepresentation results = getJourneyPlan(Stations.Altrincham, Stations.ManAirport,
                LocalTime.of(11,43), nextSunday);

        List<String> notes = results.getNotes();
        assertEquals(2, notes.size()); // include station closure message
        String prefix = "At the weekend your journey may be affected by improvement works."+ProvidesNotes.website;
        assertThat(notes, hasItem(prefix));

        int offsetToSaturday = SATURDAY-when.getDayOfWeek().getValue();
        LocalDate nextSaturday = when.plusDays(offsetToSaturday);

        results = getJourneyPlan(Stations.Altrincham, Stations.ManAirport,
                LocalTime.of(11,43), nextSaturday);

        notes = results.getNotes();
        assertEquals(2,notes.size());
        assertThat(notes, hasItem(prefix));

        JourneyPlanRepresentation notWeekendResult = getJourneyPlan(Stations.Altrincham, Stations.ManAirport,
                LocalTime.of(11,43), nextSunday.plusDays(1));
        notes = notWeekendResult.getNotes();
        assertEquals(1,notes.size());
        assertThat(notes, not(hasItem(prefix)));
    }

    @Test
    public void shouldFindRouteVeloToEtihad() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Etihad, LocalTime.of(8,0), when);
    }

    @Test
    public void shouldFindRouteVicToShawAndCrompton() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Victoria, Stations.ShawAndCrompton, LocalTime.of(23,34), when);
    }

    @Test
    public void shouldFindRoutePiccadilyGardensToCornbrook() throws TramchesterException {
        validateAtLeastOneJourney(Stations.PiccadillyGardens, Stations.Cornbrook, LocalTime.of(23,0), when);
    }

    @Test
    public void shouldFindRouteCornbrookToManAirport() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.ManAirport, LocalTime.of(23,20), when);
    }

    @Test
    public void shouldFindRouteDeansgateToVictoria() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Deansgate, Stations.Victoria, LocalTime.of(23,41), when);
    }

    @Test
    public void shouldFindRouteVeloToHoltTownAt8AM() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.HoltTown, LocalTime.of(8,0), when);
    }

    @Test
    public void shouldFindRouteVeloToEndOfLines() throws TramchesterException {
        LocalDate weekOnTuesday = when.plusWeeks(1);

        for (Location dest : Stations.EndOfTheLine) {
            validateAtLeastOneJourney(Stations.VeloPark, dest, LocalTime.of(8,0), weekOnTuesday);
        }
    }

    @Test
    public void shouldFindRouteVeloInterchanges() throws TramchesterException {
        for (Location dest : Stations.Interchanges) {
            validateAtLeastOneJourney(Stations.VeloPark, dest, LocalTime.of(8,0), when);
        }
    }

    @Test
    public void shouldReproIssueWithChangesVeloToRoch() throws TramchesterException {
        LocalDate weekOnTuesday = when.plusWeeks(1);
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Rochdale, LocalTime.of(8,0), weekOnTuesday);
    }

    @Test
    public void shouldReproIssueWithChangesVeloToTraffordBar() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.TraffordBar, LocalTime.of(8,0), when);
    }

    @Test
    public void shouldFindRouteVeloToDeansgate() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Deansgate, LocalTime.of(8,0), when);
    }

    @Test
    public void shouldFindRouteVeloToBroadway() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Broadway, LocalTime.of(8,0), when);
    }

    @Test
    public void shouldFindRouteVeloToPomona() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Pomona, LocalTime.of(8,0), when);
    }

    @Test
    public void shouldFindEndOfDayTwoStageJourney() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Altrincham, Stations.ManAirport, LocalTime.of(22,56), when);
    }

    @Test
    public void shouldFindEndOfDayThreeStageJourney() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Altrincham, Stations.ShawAndCrompton, LocalTime.of(22,45), when);
    }

    @Test
    public void shouldOnlyReturnFullJourneysForEndOfDaysJourney() throws TramchesterException {
        JourneyPlanRepresentation results = validateAtLeastOneJourney(Stations.Deansgate,
                Stations.ManAirport, LocalTime.of(23,5), when);

        Assert.assertTrue(results.getJourneys().size()>0);
    }

    @Test
    public void shouldSetCookieForRecentJourney() throws IOException {
        String start = Stations.Bury.getId();
        String end = Stations.ManAirport.getId();
        String time = LocalTime.now().format(timeFormatter);
        String date = LocalDate.now().format(dateFormatDashes);
        Response result = getResponseForJourney(testRule, start, end, time, date, null);

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
        String time = LocalTime.now().format(timeFormatter);
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
        //String latlong = "%7B%22lat%22:53.3949553,%22lon%22:-2.3580997999999997%7D";
        LatLong latlong = new LatLong(53.3949553,-2.3580997999999997 );
        String start = MyLocationFactory.MY_LOCATION_PLACEHOLDER_ID;
        String end = Stations.ManAirport.getId();
        String time = LocalTime.now().format(timeFormatter);
        String date = LocalDate.now().format(dateFormatDashes);
        Response response = getResponseForJourney(testRule, start, end, time, date, latlong);

        assertEquals(200, response.getStatus());
        RecentJourneys result = getRecentJourneysFromCookie(response);
        Set<Timestamped> recents = result.getRecentIds();
        assertEquals(1, recents.size());
        // checks ID only
        assertTrue(recents.contains(new Timestamped(end, LocalDateTime.now())));
    }

    @Test
    public void shouldReproduceIssueWithMissingRoutes() throws TramchesterException {
        validateAtLeastOneJourney(Stations.TraffordBar, Stations.ExchangeSquare, LocalTime.of(10,0), when);
    }

    private RecentJourneys getRecentJourneysFromCookie(Response response) throws IOException {
        Map<String, NewCookie> cookies = response.getCookies();
        NewCookie recent = cookies.get("tramchesterRecent");
        assertNotNull(recent);
        String value = recent.toCookie().getValue();
        RecentJourneys result = RecentJourneys.decodeCookie(mapper,value);
        return result;
    }

    protected JourneyPlanRepresentation getJourneyPlan(Location start, Location end, LocalTime queryTime,
                                                       TramServiceDate queryDate) {
        String date = queryDate.getDate().format(dateFormatDashes);
        String time = queryTime.format(timeFormatter);
        Response response = getResponseForJourney(testRule, start.getId(), end.getId(), time, date, null);
        assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    public static Response getResponseForJourney(IntegrationTestRun rule, String start, String end, String time,
                                                 String date, LatLong latlong) {
        String queryString = String.format("journey?start=%s&end=%s&departureTime=%s&departureDate=%s", start, end, time, date);
        if (MyLocationFactory.MY_LOCATION_PLACEHOLDER_ID.equals(start)) {
            if (latlong==null) {
                fail("must provide latlong");
            }
            queryString = String.format("%s&lat=%f&lon=%f", queryString, latlong.getLat(), latlong.getLon());
        }
        return IntegrationClient.getResponse(rule, queryString, Optional.empty(), 200);
    }
}
