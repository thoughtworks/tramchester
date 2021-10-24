package com.tramchester.integration.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.App;
import com.tramchester.domain.BoxWithServiceFrequency;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.BoxWithFrequencyDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.ParseStream;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.dateFormatDashes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(DropwizardExtensionsSupport.class)
public class FrequencyResourceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private ParseStream<BoxWithFrequencyDTO> parseStream;

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new IntegrationTramTestConfig(true));

    @BeforeEach
    public void beforeEachTest() {
        parseStream = new ParseStream<>(mapper);
    }

    @Test
    void shouldHaveJourneysForWholeGrid() throws IOException {

        int gridSizeMeters = 1000;
        LocalDate date = TestEnv.testDay();

        LocalTime start= LocalTime.of(7,59);
        LocalTime end = LocalTime.of(9,15);
        String queryString = String.format("frequency?gridSize=%s&date=%s&startTime=%s&endTime=%s",
                gridSizeMeters, date.format(dateFormatDashes),
                start.format(TestEnv.timeFormatter), end.format(TestEnv.timeFormatter));

        Response response = APIClient.getApiResponse(appExtension, queryString);
        assertEquals(200, response.getStatus());

        InputStream inputStream = response.readEntity(InputStream.class);
        List<BoxWithFrequencyDTO> results = parseStream.receive(response, inputStream, BoxWithFrequencyDTO.class);

        assertFalse(results.isEmpty());

        TramStations station =TramStations.ManAirport;
        GridPosition stationPosition = CoordinateTransforms.getGridPosition(station.getLatLong());

        List<BoxWithFrequencyDTO> containsAirport = results.stream().
                filter(item -> containsStation(stationPosition, item)).collect(Collectors.toList());
        assertEquals(1, containsAirport.size());
        BoxWithFrequencyDTO airport = containsAirport.get(0);

        assertEquals(6, airport.getNumberOfStopcalls());
    }

    private boolean containsStation(GridPosition position, BoxWithFrequencyDTO box) {

        BoundingBox boundingBox = new BoundingBox(CoordinateTransforms.getGridPosition(box.getBottomLeft()),
                CoordinateTransforms.getGridPosition(box.getTopRight()));
        return boundingBox.contained(position);
    }

}
