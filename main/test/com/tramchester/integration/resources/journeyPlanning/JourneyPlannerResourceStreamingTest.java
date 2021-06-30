package com.tramchester.integration.resources.journeyPlanning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.tramchester.App;
import com.tramchester.domain.Timestamped;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.APIClient;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tramchester.testSupport.TestEnv.dateFormatDashes;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(DropwizardExtensionsSupport.class)
public class JourneyPlannerResourceStreamingTest {

    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new IntegrationTramTestConfig());

    private final ObjectMapper mapper = new ObjectMapper();
    private LocalDate when;
    private LocalDateTime now;
    private ParseStream<JourneyDTO> parseStream;

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
        now = TestEnv.LocalNow();
        parseStream = new ParseStream<>(mapper);
    }

    @Test
    void shouldGetResultsAsStream() throws IOException {
        IdFor<Station> start = TramStations.Bury.getId();
        IdFor<Station> end = TramStations.ManAirport.getId();
        String time = TramTime.of(11,45).toPattern();
        String date = when.format(dateFormatDashes);
        final int maxChanges = 3;
        final boolean arriveBy = false;

        String queryString = String.format("journey/streamed?start=%s&end=%s&departureTime=%s&departureDate=%s&arriveby=%s&maxChanges=%s",
                start.forDTO(), end.forDTO(), time, date, arriveBy, maxChanges);

        Response response = APIClient.getApiResponse(appExtension, queryString);
        Assertions.assertEquals(200, response.getStatus());

        InputStream inputStream = response.readEntity(InputStream.class);
        List<JourneyDTO> journeyDTOS = parseStream.receive(response, inputStream, JourneyDTO.class);

        Assertions.assertFalse(journeyDTOS.isEmpty());
        journeyDTOS.forEach(journeyDTO -> Assertions.assertFalse(journeyDTO.getStages().isEmpty()));
    }

    @Test
    void shouldSetCookieForRecentJourney() throws IOException {
        IdFor<Station> start = TramStations.Bury.getId();
        IdFor<Station> end = TramStations.ManAirport.getId();

        Response result = getResponseForJourney(appExtension, start.forDTO(), end.forDTO(), now.toLocalTime(),
                now.toLocalDate(), null, false, 3);

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
        Response response = APIClient.getApiResponse(appExtension,
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

    private RecentJourneys getRecentJourneysFromCookie(Response response) throws IOException {
        Map<String, NewCookie> cookies = response.getCookies();
        NewCookie recent = cookies.get("tramchesterRecent");
        assertNotNull(recent);
        Assertions.assertEquals("/api",recent.getPath());
        Assertions.assertEquals("localhost", recent.getDomain());
        String value = recent.toCookie().getValue();
        return RecentJourneys.decodeCookie(mapper,value);
    }

    private static Response getResponseForJourney(IntegrationAppExtension rule, String start, String end, LocalTime time,
                                                 LocalDate date, LatLong latlong, boolean arriveBy, int maxChanges) {
        String timeString = time.format(TestEnv.timeFormatter);
        String dateString = date.format(dateFormatDashes);

        String queryString = String.format("journey/streamed?start=%s&end=%s&departureTime=%s&departureDate=%s&arriveby=%s&maxChanges=%s",
                start, end, timeString, dateString, arriveBy, maxChanges);

        if (MyLocation.MY_LOCATION_PLACEHOLDER_ID.equals(start) || MyLocation.MY_LOCATION_PLACEHOLDER_ID.equals(end)) {
            if (latlong==null) {
                fail("must provide latlong");
            } else {
                queryString = String.format("%s&lat=%f&lon=%f", queryString, latlong.getLat(), latlong.getLon());
            }
        }
        return APIClient.getApiResponse(rule, queryString);
    }
}
