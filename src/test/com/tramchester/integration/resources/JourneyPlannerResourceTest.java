package com.tramchester.integration.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.Sets;
import com.tramchester.App;
import com.tramchester.domain.Location;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.Timestamped;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.joda.time.DateTimeConstants.*;
import static org.junit.Assert.assertEquals;

public class JourneyPlannerResourceTest extends JourneyPlannerHelper {

    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    private ObjectMapper mapper = new ObjectMapper();
    private LocalDate when;

    @Before
    public void beforeEachTestRuns() {
        when = nextMonday(0);

        mapper.registerModule(new JodaModule());
    }

    @Test
    public void shouldPlanSimpleJourneyFromAltyToCornbrook() throws TramchesterException {

        JourneyPlanRepresentation plan = getJourneyPlan(Stations.Altrincham, Stations.Cornbrook, (8 * 60) + 15,
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
    public void testAltyToManAirportHasRealisticTranferAtCornbrook() throws TramchesterException {
        int offsetToSunday = SUNDAY- when.getDayOfWeek();
        LocalDate nextSunday = when.plusDays(offsetToSunday);

        JourneyPlanRepresentation results = getJourneyPlan(Stations.Altrincham, Stations.ManAirport,(11*60)+43, nextSunday);

        Set<JourneyDTO> journeys = results.getJourneys();

        assertEquals(1, journeys.size());
        checkDepartsAfterPreviousArrival("Altrincham to airport at 11:43 sunday", journeys);
    }

    @Test
    public void shouldWarnOnSaturdayAndSundayJourney() throws TramchesterException {
        int offsetToSunday = SUNDAY-when.getDayOfWeek();
        LocalDate nextSunday = when.plusDays(offsetToSunday);

        JourneyPlanRepresentation results = getJourneyPlan(Stations.Altrincham, Stations.ManAirport,
                (11*60)+43, nextSunday);

        List<String> notes = results.getNotes();
        assertEquals(1, notes.size());
        String prefix = "At the weekend your journey may be affected by improvement works";
        assertTrue(notes.get(0).startsWith(prefix));

        int offsetToSaturday = SATURDAY-when.getDayOfWeek();
        LocalDate nextSaturday = when.plusDays(offsetToSaturday);

        results = getJourneyPlan(Stations.Altrincham, Stations.ManAirport,
                (11*60)+43, nextSaturday);

        notes = results.getNotes();
        assertEquals(1,notes.size());
        assertTrue(notes.get(0).startsWith(prefix));

        JourneyPlanRepresentation notWeekendResult = getJourneyPlan(Stations.Altrincham, Stations.ManAirport,
                (11*60)+43, nextSunday.plusDays(1));
        notes = notWeekendResult.getNotes();
        assertEquals(0,notes.size());
    }

    @Test
    public void shouldFindRouteVeloToEtihad() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Etihad, 8*60, when);
    }

    @Test
    public void shouldFindRouteVicToShawAndCrompton() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Victoria, Stations.ShawAndCrompton, (23*60)+34, when);
    }

    @Test
    public void shouldFindRoutePiccadilyGardensToCornbrook() throws TramchesterException {
        validateAtLeastOneJourney(Stations.PiccadillyGardens, Stations.Cornbrook, 23*60, when);
    }

    @Test
    public void shouldFindRouteCornbrookToManAirport() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.ManAirport, (23*60)+20, when);
    }

    @Test
    public void shouldFindRouteDeansgateToVictoria() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Deansgate, Stations.Victoria, (23*60)+41, when);
    }

    @Test
    public void shouldFindRouteVeloToHoltTownAt8AM() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.HoltTown, 8*60, when);
    }

    @Test
    public void shouldFindRouteVeloToEndOfLines() throws TramchesterException {
        int offsetToMonday = MONDAY- when.getDayOfWeek();
        LocalDate nextMonday = when.plusDays(offsetToMonday);

        for (Location dest : Stations.EndOfTheLine) {
            validateAtLeastOneJourney(Stations.VeloPark, dest, 8*60, nextMonday);
        }
    }

    @Test
    public void shouldFindRouteVeloInterchanges() throws TramchesterException {
        for (Location dest : Stations.getInterchanges()) {
            validateAtLeastOneJourney(Stations.VeloPark, dest, 8*60, when);
        }
    }

    @Test
    public void shouldFindRouteVeloToDeansgate() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Deansgate, 8*60, when);
    }

    @Test
    public void shouldFindRouteVeloToBroadway() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Broadway, 8*60, when);
    }

    @Test
    public void shouldFindRouteVeloToPomona() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Pomona, 8*60, when);
    }

    @Test
    public void shouldFindEndOfDayTwoStageJourney() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Altrincham, Stations.ManAirport, (22*60)+56, when);
    }

    @Test
    public void shouldFindEndOfDayThreeStageJourney() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Altrincham, Stations.ShawAndCrompton, (22*60)+45, when);
    }

    @Test
    public void shouldOnlyReturnFullJourneysForEndOfDaysJourney() throws TramchesterException {
        JourneyPlanRepresentation results = validateAtLeastOneJourney(Stations.Deansgate,
                Stations.ManAirport, (23*60)+5, when);

        Assert.assertTrue(results.getJourneys().size()>0);
    }

    @Test
    public void shouldSetCookieForRecentJourney() throws IOException {
        String start = Stations.Bury.getId();
        String end = Stations.ManAirport.getId();
        String time = LocalTime.now().toString("HH:mm:00");
        String date = LocalDate.now().toString("YYYY-MM-dd");
        Response result = getResponseForJourney(testRule, start, end, time, date);

        assertEquals(200, result.getStatus());

        RecentJourneys recentJourneys = getRecentJourneysFromCookie(result);

        assertEquals(2,recentJourneys.getRecentIds().size());
        assertTrue(recentJourneys.getRecentIds().contains(new Timestamped(start, DateTime.now())));
        assertTrue(recentJourneys.getRecentIds().contains(new Timestamped(end, DateTime.now())));
    }

    @Test
    public void shouldUdateCookieForRecentJourney() throws IOException {
        String start = Stations.Bury.getId();
        String end = Stations.ManAirport.getId();
        String time = LocalTime.now().toString("HH:mm:00");
        String date = LocalDate.now().toString("YYYY-MM-dd");

        // cookie with ashton
        RecentJourneys recentJourneys = new RecentJourneys();
        Timestamped ashton = new Timestamped(Stations.Ashton.getId(), DateTime.now());
        recentJourneys.setRecentIds(Sets.newHashSet(ashton));
        Cookie cookie = new Cookie("tramchesterRecent", RecentJourneys.encodeCookie(mapper,recentJourneys));

        // journey to bury
        Response response = IntegrationClient.getResponse(testRule,
                String.format("journey?start=%s&end=%s&departureTime=%s&departureDate=%s", start, end, time, date),
                Optional.of(cookie));

        assertEquals(200, response.getStatus());

        RecentJourneys result = getRecentJourneysFromCookie(response);

        // ashton, bury and man airport now in cookie
        Set<Timestamped> recents = result.getRecentIds();
        assertEquals(3, recents.size());
        assertTrue(recents.contains(new Timestamped(start, DateTime.now())));
        assertTrue(recents.contains(ashton));
        assertTrue(recents.contains(new Timestamped(end, DateTime.now())));
    }

    @Test
    public void shouldOnlyCookiesForDestinationIfLocationSent() throws IOException {
        String start = "%7B%22lat%22:53.3949553,%22lon%22:-2.3580997999999997%7D";
        String end = Stations.ManAirport.getId();
        String time = LocalTime.now().toString("HH:mm:00");
        String date = LocalDate.now().toString("YYYY-MM-dd");
        Response response = getResponseForJourney(testRule, start, end, time, date);

        assertEquals(200, response.getStatus()
        );
        RecentJourneys result = getRecentJourneysFromCookie(response);
        Set<Timestamped> recents = result.getRecentIds();
        assertEquals(1, recents.size());
        assertTrue(recents.contains(new Timestamped(end, DateTime.now())));
    }

    @Test
    public void shouldReproduceIssueWithMissingRoutes() throws TramchesterException {
        validateAtLeastOneJourney(Stations.TraffordBar, Stations.ExchangeSquare, 10*60, when);
    }

    private RecentJourneys getRecentJourneysFromCookie(Response response) throws IOException {
        Map<String, NewCookie> cookies = response.getCookies();
        NewCookie recent = cookies.get("tramchesterRecent");
        assertNotNull(recent);
        String value = recent.toCookie().getValue();
        RecentJourneys result = RecentJourneys.decodeCookie(mapper,value);
        return result;
    }

    protected JourneyPlanRepresentation getJourneyPlan(Location start, Location end, int minsPastMid,
                                                       TramServiceDate queryDate) throws TramchesterException {
        String date = queryDate.getDate().toString("YYYY-MM-dd");
        String time = LocalTime.MIDNIGHT.plusMinutes(minsPastMid).toString("HH:mm:00");
        Response response = getResponseForJourney(testRule, start.getId(), end.getId(), time, date);
        assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    public static Response getResponseForJourney(IntegrationTestRun rule, String start, String end, String time, String date) {
        return IntegrationClient.getResponse(rule,
                String.format("journey?start=%s&end=%s&departureTime=%s&departureDate=%s", start, end, time, date),
                Optional.empty());
    }
}
