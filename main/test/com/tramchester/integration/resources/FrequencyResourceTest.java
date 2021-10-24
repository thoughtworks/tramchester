package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(DropwizardExtensionsSupport.class)
public class FrequencyResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new IntegrationTramTestConfig(true));

    @Test
    void shouldHaveJourneysForWholeGrid() {

        int gridSizeMeters = 1000;
        LocalDateTime startDateTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endDateTime = LocalDateTime.now().plusDays(1);

        String queryString = String.format("frequency?gridSize=%s&startDateTime=%s&endDateTime=%s",
                gridSizeMeters, startDateTime, endDateTime);

        Response response = APIClient.getApiResponse(appExtension, queryString);
        assertEquals(200, response.getStatus());
    }

}
