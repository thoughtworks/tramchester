package com.tramchester.integration.testSupport;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStations;
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

    public JourneyPlanRepresentation getJourneyPlan(LocalDate when, TramTime time, PostcodeLocation start, HasId<Station> end,
                                                    boolean arriveBy, int maxChanges) {
        return getApiResponse(when, time, prefix(start), end.getId().forDTO(), null, arriveBy, maxChanges);
    }

    public JourneyPlanRepresentation getJourneyPlan(LocalDate when, TramTime time, LatLong start, IdFor<Station> end,
                                                    boolean arriveBy, int maxChanges) {
        return getApiResponse(when, time, MyLocation.MY_LOCATION_PLACEHOLDER_ID, end.forDTO(), start, arriveBy, maxChanges);
    }

    public JourneyPlanRepresentation getJourneyPlan(LocalDate when, TramTime time, TestStations start, LatLong end,
                                                    boolean arriveBy, int maxChanges) {
        return getApiResponse(when, time, start.getId().forDTO(), MyLocation.MY_LOCATION_PLACEHOLDER_ID, end, arriveBy, maxChanges);
    }


    public JourneyPlanRepresentation getJourneyPlan(LocalDate when, TramTime time, TestStations start, PostcodeLocation end,
                                                    boolean arriveBy, int maxChanges) {
        return getApiResponse(when, time, start.getId().forDTO(), prefix(end), null, arriveBy, maxChanges);
    }

    public JourneyPlanRepresentation getJourneyPlan(LocalDate when, TramTime time, PostcodeLocation start, PostcodeLocation end,
                                                    boolean arriveBy, int maxChanges) {
        return getApiResponse(when, time, prefix(start), prefix(end), null, arriveBy, maxChanges);
    }

    public JourneyPlanRepresentation getJourneyPlan(LocalDate when, TramTime time, HasId<Station> start, HasId<Station> end,
                                                    boolean arriveBy, int maxChanges) {
        return getApiResponse(when, time, start.getId().forDTO(), end.getId().forDTO(), null, arriveBy, maxChanges);
    }


    private JourneyPlanRepresentation getApiResponse(LocalDate when, TramTime time, String startAsString, String endAsString,
                                                     LatLong position, boolean arriveBy, int maxChanges) {
        Response response = getResponseForJourney(when, time.asLocalTime(), startAsString,
                endAsString, position, arriveBy, maxChanges);

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
