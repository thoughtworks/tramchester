package com.tramchester.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.Sets;
import com.tramchester.*;
import com.tramchester.domain.RecentJourneys;
import com.tramchester.domain.Timestamped;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.fail;

public class JourneyPlannerResourceTest {
    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    ObjectMapper mapper = new ObjectMapper();

    @Before
    public void beforeEachTestRuns() {
        mapper.registerModule(new JodaModule());
    }


    // TODO need to make elements of JourneyPlanRepresentation deserialable before making all tests use this
    // for now just check cookie behaviours

    @Test
    public void shouldSetCookieForRecentJourney() throws IOException {
        String start = Stations.Bury.getId();
        String end = Stations.ManAirport.getId();
        String time = LocalTime.now().toString("HH:mm:00");
        String date = LocalDate.now().toString("YYYY-MM-dd");
        Response result = IntegrationClient.getResponse(testRule,
                String.format("journey?start=%s&end=%s&departureTime=%s&departureDate=%s", start, end, time, date),
                Optional.empty());

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
        Response response = IntegrationClient.getResponse(testRule,
                String.format("journey?start=%s&end=%s&departureTime=%s&departureDate=%s", start, end, time, date),
                Optional.empty());

        assertEquals(200, response.getStatus()
        );
        RecentJourneys result = getRecentJourneysFromCookie(response);
        Set<Timestamped> recents = result.getRecentIds();
        assertEquals(1, recents.size());
        assertTrue(recents.contains(new Timestamped(end, DateTime.now())));
    }

    private RecentJourneys getRecentJourneysFromCookie(Response response) throws IOException {
        Map<String, NewCookie> cookies = response.getCookies();
        NewCookie recent = cookies.get("tramchesterRecent");
        assertNotNull(recent);
        String value = recent.toCookie().getValue();
        RecentJourneys result = RecentJourneys.decodeCookie(mapper,value);
        return result;
    }
}
