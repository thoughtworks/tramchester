package com.tramchester.integration.testSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.ParseStream;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocations;
import org.junit.jupiter.api.Assertions;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static com.tramchester.testSupport.TestEnv.dateFormatDashes;
import static org.assertj.core.api.Fail.fail;

public class JourneyResourceTestFacade {

    private final IntegrationAppExtension appExtension;
    private final ParseStream<JourneyDTO> parseStream;

    public JourneyResourceTestFacade(IntegrationAppExtension appExtension) {
        this.appExtension = appExtension;
        ObjectMapper mapper = new ObjectMapper();
        parseStream = new ParseStream<>(mapper);
    }

    public JourneyPlanRepresentation getJourneyPlan(LocalDate when, TramTime time, PostcodeLocation start, HasId<?> end,
                                                    boolean arriveBy, int maxChanges) {
        return getApiResponse(when, time, prefix(start), end.getId(), null, arriveBy, maxChanges);
    }

    public JourneyPlanRepresentation getJourneyPlan(LocalDate when, TramTime time, KnownLocations start, IdFor<?> end,
                                                    boolean arriveBy, int maxChanges) {
        return getApiResponse(when, time, MyLocation.MY_LOCATION_PLACEHOLDER_ID, end, start.latLong(), arriveBy, maxChanges);
    }

    public JourneyPlanRepresentation getJourneyPlan(LocalDate when, TramTime time, IdFor<Station> startId, KnownLocations end,
                                                    boolean arriveBy, int maxChanges) {
        return getApiResponse(when, time, startId, MyLocation.MY_LOCATION_PLACEHOLDER_ID, end.latLong(), arriveBy, maxChanges);
    }

    public JourneyPlanRepresentation getJourneyPlan(LocalDate when, TramTime time, PostcodeLocation start, PostcodeLocation end,
                                                    boolean arriveBy, int maxChanges) {
        return getApiResponse(when, time, prefix(start), prefix(end), null, arriveBy, maxChanges);
    }

    public JourneyPlanRepresentation getJourneyPlan(LocalDate when, TramTime time, HasId<?> start, HasId<?> end,
                                                    boolean arriveBy, int maxChanges) {
        return getApiResponse(when, time, start.getId(), end.getId(), null, arriveBy, maxChanges);
    }

    private JourneyPlanRepresentation getApiResponse(LocalDate when, TramTime time, IdFor<?> start, IdFor<?> end,
                                                     LatLong position, boolean arriveBy, int maxChanges) {
        return getApiResponse(when, time, start.forDTO(), end.forDTO(), position, arriveBy, maxChanges);
    }

    private JourneyPlanRepresentation getApiResponse(LocalDate when, TramTime time, IdFor<?> start, String endAsString,
                                                     LatLong position, boolean arriveBy, int maxChanges) {
        return getApiResponse(when, time, start.forDTO(), endAsString, position, arriveBy, maxChanges);
    }

    private JourneyPlanRepresentation getApiResponse(LocalDate when, TramTime time, String startAsString, IdFor<?> end,
                                                     LatLong position, boolean arriveBy, int maxChanges) {
        return getApiResponse(when, time, startAsString, end.forDTO(), position, arriveBy, maxChanges);
    }

    private JourneyPlanRepresentation getApiResponse(LocalDate when, TramTime time, String startAsString, String endAsString,
                                                     LatLong position, boolean arriveBy, int maxChanges) {
        Response response = getResponseForJourney(when, time.asLocalTime(), startAsString,
                endAsString, position, arriveBy, maxChanges, false);

        Assertions.assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    public List<JourneyDTO> getJourneyPlanStreamed(LocalDate when, TramTime time, HasId<Station> start, HasId<Station> end,
                                                   boolean arriveBy, int maxChanges) throws IOException {

        Response response = getResponseForJourney(when, time.asLocalTime(), start.getId().forDTO(), end.getId().forDTO(),
                null, arriveBy, maxChanges, true);
        Assertions.assertEquals(200, response.getStatus());

        InputStream inputStream = response.readEntity(InputStream.class);

        return parseStream.receive(response, inputStream, JourneyDTO.class);
    }

    private Response getResponseForJourney(LocalDate date, LocalTime time, String start, String end,
                                           LatLong latlong, boolean arriveBy, int maxChanges, boolean streamed) {
        String timeString = time.format(TestEnv.timeFormatter);
        String dateString = date.format(dateFormatDashes);

        String prefix = streamed ? "journey/streamed" : "journey";

        String queryString = String.format("%s?start=%s&end=%s&departureTime=%s&departureDate=%s&arriveby=%s&maxChanges=%s",
                prefix, start, end, timeString, dateString, arriveBy, maxChanges);

        if (MyLocation.MY_LOCATION_PLACEHOLDER_ID.equals(start) || MyLocation.MY_LOCATION_PLACEHOLDER_ID.equals(end)) {
            if (latlong==null) {
                fail("must provide latlong");
            } else {
                queryString = String.format("%s&lat=%f&lon=%f", queryString, latlong.getLat(), latlong.getLon());
            }
        }
        return APIClient.getApiResponse(appExtension, queryString);
    }

    private String prefix(PostcodeLocation postcode) {
        return  "POSTCODE_"+postcode.forDTO();
    }


}
