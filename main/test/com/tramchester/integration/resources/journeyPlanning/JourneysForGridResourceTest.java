package com.tramchester.integration.resources.journeyPlanning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.tramchester.App;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.BoxWithCostDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.resources.JourneysForGridResource;
import com.tramchester.testSupport.ParseStream;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.dateFormatDashes;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class JourneysForGridResourceTest {
    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new ResourceTramTestConfig<>(JourneysForGridResource.class));

    private final ObjectMapper mapper = JsonMapper.builder().addModule(new AfterburnerModule()).build();
    private ParseStream<BoxWithCostDTO> parseStream;
    private String time;
    private String date;
    private int maxChanges;
    private int gridSize;
    private int maxDuration;

    @BeforeEach
    void beforeEachTestRuns() {
        LocalDate when = TestEnv.testDay();
        parseStream = new ParseStream<>(mapper);

        time = TramTime.of(9,15).toPattern();
        date = when.format(dateFormatDashes);
        maxChanges = 3;
        gridSize = 2000;
        maxDuration = 40;
    }

    @Test
    void shouldHaveJourneysForWholeGrid() throws IOException {
        LatLong destPos = TestEnv.stPetersSquareLocation();
        IdFor<Station> destination = TramStations.StPetersSquare.getId();

        String queryString = String.format("grid?gridSize=%s&destination=%s&departureTime=%s&departureDate=%s&maxChanges=%s&maxDuration=%s",
                gridSize, destination.forDTO(), time, date, maxChanges, maxDuration);

        Response response = APIClient.getApiResponse(appExtension, queryString);
        assertEquals(200, response.getStatus());

        InputStream inputStream = response.readEntity(InputStream.class);
        List<BoxWithCostDTO> results = parseStream.receive(response, inputStream, BoxWithCostDTO.class);

        List<BoxWithCostDTO> containsDest = results.stream().filter(result -> result.getMinutes() == 0).collect(Collectors.toList());
        assertEquals(1, containsDest.size());
        BoxWithCostDTO boxWithDest = containsDest.get(0);
        assertTrue(boxWithDest.getBottomLeft().getLat() <= destPos.getLat());
        assertTrue(boxWithDest.getBottomLeft().getLon() <= destPos.getLon());
        assertTrue(boxWithDest.getTopRight().getLat() >= destPos.getLat());
        assertTrue(boxWithDest.getTopRight().getLon() >= destPos.getLon());

        List<BoxWithCostDTO> notDest = results.stream().filter(result -> result.getMinutes() > 0).collect(Collectors.toList());

        // 40->37 summer 2021
        assertEquals(42, notDest.size());
        assertFalse(results.isEmpty());
        notDest.forEach(boundingBoxWithCost -> assertTrue(boundingBoxWithCost.getMinutes()<=maxDuration));

        List<BoxWithCostDTO> noResult = results.stream().filter(result -> result.getMinutes() < 0).collect(Collectors.toList());
        assertEquals(2, noResult.size());
    }


}
