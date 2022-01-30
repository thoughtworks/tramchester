package com.tramchester.integration.testSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.App;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.ParseStream;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.FakeStation;
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
    private final StationRepository stationRepository;

    public JourneyResourceTestFacade(IntegrationAppExtension appExtension) {
        this.appExtension = appExtension;
        App app =  appExtension.getApplication();
        stationRepository = app.getDependencies().get(StationRepository.class);

        ObjectMapper mapper = new ObjectMapper();
        parseStream = new ParseStream<>(mapper);
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

        Response response = fetchJourneyResponceGET(queryDate, time.asLocalTime(), start.from(stationRepository),
                dest.from(stationRepository), arriveBy, maxChanges, true);

        Assertions.assertEquals(200, response.getStatus());

        InputStream inputStream = response.readEntity(InputStream.class);

        return parseStream.receive(response, inputStream, JourneyDTO.class);
    }

    private JourneyPlanRepresentation getPlan(LocalDate when, TramTime time, Location<?> start, Location<?> dest,
                                              boolean arriveBy, int maxChanges) {

        Response response = fetchJourneyResponceGET(when, time.asLocalTime(), start, dest, arriveBy, maxChanges, false);

        Assertions.assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    private Response fetchJourneyResponceGET(LocalDate date, LocalTime time, Location<?> start,  Location<?> dest,
                                             boolean arriveBy, int maxChanges, boolean streamed) {

        String startAsString = getStringId(start);
        String destAsString = getStringId(dest);

        LatLong latLong = getPositionFor(start, dest);

        String timeString = time.format(TestEnv.timeFormatter);
        String dateString = date.format(dateFormatDashes);

        String prefix = streamed ? "journey/streamed" : "journey";

        String queryString = String.format("%s?start=%s&end=%s&departureTime=%s&departureDate=%s&arriveby=%s&maxChanges=%s",
                prefix, startAsString, destAsString, timeString, dateString, arriveBy, maxChanges);

        if (MyLocation.MY_LOCATION_PLACEHOLDER_ID.equals(startAsString) || MyLocation.MY_LOCATION_PLACEHOLDER_ID.equals(destAsString)) {
            // todo remove the null check once all callers pass invalid instead
            if (latLong==null || !latLong.isValid()) {
                fail("must provide latlong");
            } else {
                queryString = String.format("%s&lat=%f&lon=%f", queryString, latLong.getLat(), latLong.getLon());
            }
        }
        return APIClient.getApiResponse(appExtension, queryString);
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
