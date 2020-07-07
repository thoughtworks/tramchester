package com.tramchester.integration.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.App;
import com.tramchester.domain.BoundingBoxWithCost;
import com.tramchester.domain.presentation.DTO.BoxWithCostDTO;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.IntegrationAppExtension;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.ParseStream;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import static com.tramchester.testSupport.TestEnv.dateFormatDashes;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(DropwizardExtensionsSupport.class)
class JourneysForGridResourceTest {
    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new IntegrationTramTestConfig());

    private final ObjectMapper mapper = new ObjectMapper();
    private LocalDate when;
    private ParseStream<BoxWithCostDTO> parseStream;

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
        parseStream = new ParseStream<>(mapper);
    }

    @Test
    void shouldCreateGrid() throws IOException {

        String time = TramTime.of(9,15).toPattern();
        String date = when.format(dateFormatDashes);
        int maxChanges = 3;
        long gridSize = 2000;
        int maxDuration = 120;

        String destination = Stations.StPetersSquare.getId();

        String queryString = String.format("grid?gridSize=%s&destination=%s&departureTime=%s&departureDate=%s&maxChanges=%s&maxDuration=%s",
                gridSize, destination, time, date, maxChanges, maxDuration);

        Response response = IntegrationClient.getApiResponse(appExtension, queryString);
        Assertions.assertEquals(200, response.getStatus());

        InputStream inputStream = response.readEntity(InputStream.class);
        List<BoxWithCostDTO> results = parseStream.receive(response, inputStream, BoxWithCostDTO.class);

        assertFalse(results.isEmpty());
        results.forEach(boundingBoxWithCost -> assertTrue(boundingBoxWithCost.getMinutes()>0));
        results.forEach(boundingBoxWithCost -> assertTrue(boundingBoxWithCost.getMinutes()<=maxDuration));

    }

}
