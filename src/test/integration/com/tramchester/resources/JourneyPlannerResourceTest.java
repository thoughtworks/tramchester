package com.tramchester.resources;

import com.tramchester.*;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import junit.framework.TestCase;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import java.util.Map;
import java.util.Optional;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class JourneyPlannerResourceTest {
    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    // TODO need to make elements of JourneyPlanRepresentation deserialable before making all tests use this
    // for now just check cookie behaviours

    @Test
    public void shouldSetCookieForRecentJourney() {
        String start = Stations.Bury.getId();
        String end = Stations.ManAirport.getId();
        String time = LocalTime.now().toString("HH:mm:00");
        String date = LocalDate.now().toString("YYYY-MM-dd");
        Response result = IntegrationClient.getResponse(testRule,
                String.format("journey?start=%s&end=%s&departureTime=%s&departureDate=%s", start, end, time, date),
                Optional.empty());

        assertEquals(200, result.getStatus());
        Map<String, NewCookie> cookies = result.getCookies();
        NewCookie recent = cookies.get("tramchesterRecent");
        assertNotNull(recent);
        assertEquals(start,recent.toCookie().getValue());

    }

    @Test
    public void shouldNotSetCookieForRecentJourneyFromMyLocation() {
        String start = "%7B%22lat%22:53.3949553,%22lon%22:-2.3580997999999997%7D";
        String end = Stations.ManAirport.getId();
        String time = LocalTime.now().toString("HH:mm:00");
        String date = LocalDate.now().toString("YYYY-MM-dd");
        Response result = IntegrationClient.getResponse(testRule,
                String.format("journey?start=%s&end=%s&departureTime=%s&departureDate=%s", start, end, time, date),
                Optional.empty());

        assertEquals(200, result.getStatus());
        Map<String, NewCookie> cookies = result.getCookies();
        NewCookie recent = cookies.get("tramchesterRecent");
        assertTrue(recent==null);
    }
}
