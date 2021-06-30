package com.tramchester.integration.testSupport;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStations;
import com.tramchester.testSupport.reference.BusStations;
import org.junit.jupiter.api.Assertions;

import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalTime;

import static com.tramchester.testSupport.TestEnv.dateFormatDashes;
import static org.assertj.core.api.Fail.fail;

public class JourneyResourceTestFacade {

    private final IntegrationAppExtension appExt;

    public JourneyResourceTestFacade(IntegrationAppExtension appExt) {
        this.appExt = appExt;
    }


    public JourneyPlanRepresentation getJourneyPlan(LocalDate day, TramTime time, PostcodeLocation start, HasId<Station> end,
                                                    boolean arriveBy, int maxChanges) {
        Response response = getResponseForJourney(day, time.asLocalTime(), prefix(start), end.getId().forDTO(),
                null, arriveBy, maxChanges);

        Assertions.assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    public JourneyPlanRepresentation getJourneyPlan(LocalDate day, TramTime time, TestStations start, PostcodeLocation end,
                                                    boolean arriveBy, int maxChanges) {
        Response response = getResponseForJourney(day, time.asLocalTime(), start.getId().forDTO(), prefix(end),
                null, arriveBy, maxChanges);

        Assertions.assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    public JourneyPlanRepresentation getJourneyPlan(LocalDate queryDate, TramTime queryTime, LatLong start, IdFor<Station> end,
                                                    boolean arriveBy, int maxChanges) {

        // TODO??? start and end swapped??
        Response response = getResponseForJourney(queryDate, queryTime.asLocalTime(), MyLocation.MY_LOCATION_PLACEHOLDER_ID,
                end.forDTO(), start, arriveBy, maxChanges);

        Assertions.assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    public JourneyPlanRepresentation getJourneyPlan(LocalDate queryDate, TramTime queryTime, PostcodeLocation start, PostcodeLocation end,
                                                    boolean arriveBy, int maxChanges) {

        Response response = getResponseForJourney(queryDate, queryTime.asLocalTime(), prefix(start), prefix(end),
            null, arriveBy, maxChanges);

        Assertions.assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    public JourneyPlanRepresentation getJourneyResults(LocalDate when, TramTime queryTime, HasId<Station> start, HasId<Station> end,
                                                       boolean arriveBy, int maxChanges) {

        Response response = getResponseForJourney(when, queryTime.asLocalTime(), start.getId().forDTO(),
                end.getId().forDTO(), null, arriveBy, maxChanges);

        Assertions.assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    private Response getResponseForJourney(LocalDate date, LocalTime time, String start, String end,
                                           LatLong latlong, boolean arriveBy, int maxChanges) {
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
        return APIClient.getApiResponse(appExt, queryString);
    }

    private String prefix(PostcodeLocation postcode) {
        return  "POSTCODE_"+postcode.forDTO();
    }

}
