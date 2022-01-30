package com.tramchester.integration.testSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.App;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.JourneyQueryDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.ParseStream;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.FakeStation;
import com.tramchester.testSupport.reference.KnownLocations;
import org.junit.jupiter.api.Assertions;

import  javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static com.tramchester.testSupport.TestEnv.dateFormatDashes;
import static org.assertj.core.api.Fail.fail;

public class JourneyResourceTestFacade {

    private final IntegrationAppExtension appExtension;
    private final ParseStream<JourneyDTO> parseStream;
    private final StationRepository stationRepository;
    private final boolean usePost;

    public JourneyResourceTestFacade(IntegrationAppExtension appExtension, boolean post) {
        this.appExtension = appExtension;
        App app =  appExtension.getApplication();
        stationRepository = app.getDependencies().get(StationRepository.class);

        ObjectMapper mapper = new ObjectMapper();
        parseStream = new ParseStream<>(mapper);

       this.usePost = post;
    }

    public JourneyPlanRepresentation getJourneyPlan(LocalDate when, TramTime time, Location<?> start, Location<?> end,
                                                    boolean arriveBy, int maxChanges) {
        return getPlan(when, time, start, end, arriveBy, maxChanges);
    }

    public JourneyPlanRepresentation getJourneyPlan(LocalDate when, TramTime time, KnownLocations start, Location<?> end,
                                                    boolean arriveBy, int maxChanges) {
        return getPlan(when, time, start.location(), end,  arriveBy, maxChanges);
    }

    public JourneyPlanRepresentation getJourneyPlan(LocalDate date, TramTime time, Location<?> start,
                                                    FakeStation dest, boolean arriveBy, int maxChanges) {
        return getPlan(date, time, start, dest.from(stationRepository), arriveBy, maxChanges);
    }

    public JourneyPlanRepresentation getJourneyPlan(LocalDate date, TramTime time, FakeStation start, Location<?> dest,
                                                    boolean arriveBy, int maxChanges) {
        return getPlan(date, time, start.from(stationRepository), dest, arriveBy, maxChanges);
    }

    public JourneyPlanRepresentation getJourneyPlan(LocalDate date, TramTime queryTime, FakeStation start,
                                                    FakeStation dest, boolean arriveBy, int maxChanges) {
        return getPlan(date, queryTime, start.from(stationRepository), dest.from(stationRepository), arriveBy, maxChanges);
    }


    public List<JourneyDTO> getJourneyPlanStreamed(LocalDate queryDate, TramTime time, FakeStation start,
                                                   FakeStation dest, boolean arriveBy, int maxChanges) throws IOException {

        Response response = getFromAPI(queryDate, time.asLocalTime(), start.from(stationRepository),
                dest.from(stationRepository), arriveBy, maxChanges, true, Collections.emptyList());

        Assertions.assertEquals(200, response.getStatus());

        InputStream inputStream = response.readEntity(InputStream.class);

        return parseStream.receive(response, inputStream, JourneyDTO.class);
    }

    private JourneyPlanRepresentation getPlan(LocalDate when, TramTime time, Location<?> start, Location<?> dest,
                                              boolean arriveBy, int maxChanges) {

        Response response = getFromAPI(when, time.asLocalTime(), start, dest, arriveBy, maxChanges, false, Collections.emptyList());

        Assertions.assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    public Response getFromAPI(LocalDate date, LocalTime time, Location<?> start, Location<?> dest,
                                boolean arriveBy, int maxChanges, boolean streamed, List<Cookie> cookies) {

        if (usePost) {
            return getJourneyFromAPIPOST(date, time, start, dest, arriveBy, maxChanges, streamed, cookies);
        } else {
            return getJourneyFromAPIGET(date, time, start, dest, arriveBy, maxChanges, streamed, cookies);
        }
    }

    private Response getJourneyFromAPIPOST(LocalDate date, LocalTime time, Location<?> start, Location<?> dest,
                                           boolean arriveBy, int maxChanges, boolean streamed, List<Cookie> cookies) {
        String startId = start.getId().forDTO();
        String destId = dest.getId().forDTO();
        LocationType startType = start.getLocationType();
        LocationType destType = dest.getLocationType();

        JourneyQueryDTO query = new JourneyQueryDTO(date, time, startType, startId, destType, destId, arriveBy, maxChanges);
        String prefix = streamed ? "journey/streamed" : "journey";
        return APIClient.postAPIRequest(appExtension, prefix, query, cookies);
    }

    private Response getJourneyFromAPIGET(LocalDate date, LocalTime time, Location<?> start, Location<?> dest,
                                          boolean arriveBy, int maxChanges, boolean streamed, List<Cookie> cookies) {
        String startAsString = getStringId(start);
        String destAsString = getStringId(dest);

        LatLong latLong = getPositionFor(start, dest);

        String timeString = time.format(TestEnv.timeFormatter);
        String dateString = date.format(dateFormatDashes);

        String prefix = streamed ? "journey/streamed" : "journey";

        String queryString = String.format("%s?start=%s&end=%s&departureTime=%s&departureDate=%s&arriveby=%s&maxChanges=%s",
                prefix, startAsString, destAsString, timeString, dateString, arriveBy, maxChanges);

        if (start.getLocationType()==LocationType.MyLocation || dest.getLocationType()==LocationType.MyLocation) {
            // todo remove the null check once all callers pass invalid instead
            if (latLong == null || !latLong.isValid()) {
                fail("must provide latlong");
            } else {
                queryString = String.format("%s&lat=%f&lon=%f", queryString, latLong.getLat(), latLong.getLon());
            }
        }
        return APIClient.getApiResponse(appExtension, queryString, cookies);
    }


    private LatLong getPositionFor(Location<?> start, Location<?> dest) {
        boolean startIsLocation = start.getLocationType() == LocationType.MyLocation;
        boolean destIsLocation = dest.getLocationType() == LocationType.MyLocation;

        if (startIsLocation && destIsLocation) {
            throw new RuntimeException("todo");
        }
        if (startIsLocation) {
            return start.getLatLong();
        }
        if (destIsLocation) {
            return dest.getLatLong();
        }
        return LatLong.Invalid;
    }

    private String getStringId(Location<?> location) {
        return switch (location.getLocationType()) {
            case Station -> location.getId().forDTO();
            case MyLocation -> MyLocation.MY_LOCATION_PLACEHOLDER_ID;
            case Postcode -> prefixPostcode(location);
            case StationGroup -> throw new RuntimeException("todo");
        };
    }

    private String prefixPostcode(Location<?> postcode) {
        return  "POSTCODE_"+postcode.forDTO();
    }

}
