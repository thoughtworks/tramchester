package com.tramchester.integration.resources.journeyPlanning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.tramchester.App;
import com.tramchester.domain.Timestamped;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.JourneyResourceTestFacade;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.resources.JourneyPlannerResource;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(DropwizardExtensionsSupport.class)
public class JourneyPlannerCookieTest {
    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new ResourceTramTestConfig<>(JourneyPlannerResource.class));

    private final ObjectMapper mapper = new ObjectMapper();
    private LocalDateTime now;
    private JourneyResourceTestFacade journeyPlanner;

    @BeforeEach
    void beforeEachTestRuns() {
        journeyPlanner = new JourneyResourceTestFacade(appExtension, true);
        now = TestEnv.LocalNow();
    }

    @Test
    void shouldSetCookieForRecentJourney() throws IOException {
        Station start = TramStations.Bury.fake();
        Station end = TramStations.ManAirport.fake();

        Response result = getResponseForJourney(start, end, now.toLocalTime(), now.toLocalDate(), Collections.emptyList());

        Assertions.assertEquals(200, result.getStatus());

        RecentJourneys recentJourneys = getRecentJourneysFromCookie(result);

        Assertions.assertEquals(2,recentJourneys.getRecentIds().size());
        assertTrue(recentJourneys.getRecentIds().contains(new Timestamped(start.getId(), now)));
        assertTrue(recentJourneys.getRecentIds().contains(new Timestamped(end.getId(), now)));
    }

    @Test
    void shouldUdateCookieForRecentJourney() throws IOException {
        Station start = TramStations.Bury.fake();
        Station end = TramStations.ManAirport.fake();

        // cookie with ashton
        RecentJourneys recentJourneys = new RecentJourneys();
        Timestamped ashton = new Timestamped(TramStations.Ashton.getId(), now);
        recentJourneys.setRecentIds(Sets.newHashSet(ashton));
        Cookie cookie = new Cookie("tramchesterRecent", RecentJourneys.encodeCookie(mapper,recentJourneys));

        // journey to bury
        Response response = getResponseForJourney(start, end, now.toLocalTime(), now.toLocalDate(), List.of(cookie));

        Assertions.assertEquals(200, response.getStatus());

        RecentJourneys result = getRecentJourneysFromCookie(response);

        // ashton, bury and man airport now in cookie
        Set<Timestamped> recents = result.getRecentIds();
        Assertions.assertEquals(3, recents.size());
        assertTrue(recents.contains(new Timestamped(start.getId(), now)));
        assertTrue(recents.contains(ashton));
        assertTrue(recents.contains(new Timestamped(end.getId(), now)));
    }

    @Test
    void shouldOnlyCookiesForDestinationIfLocationSent() throws IOException {
        LatLong latlong = new LatLong(53.3949553,-2.3580997999999997 );
        MyLocation start = new MyLocation(latlong);
        Station end = TramStations.ManAirport.fake();

        Response response = getResponseForJourney(start, end, now.toLocalTime(),  now.toLocalDate(), Collections.emptyList());
        Assertions.assertEquals(200, response.getStatus());

        RecentJourneys result = getRecentJourneysFromCookie(response);
        Set<Timestamped> recents = result.getRecentIds();
        Assertions.assertEquals(1, recents.size());
        // checks ID only
        assertTrue(recents.contains(new Timestamped(end.getId(), now)));
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

    private Response getResponseForJourney(Location<?> start, Location<?> end, LocalTime time, LocalDate date, List<Cookie> cookies) {

        return journeyPlanner.getFromAPI(date, time, start, end, false, 3, false, cookies);
    }
}
