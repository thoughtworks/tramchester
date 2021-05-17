package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.presentation.DTO.StationLinkDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.IntegrationClient;
import com.tramchester.integration.testSupport.NeighboursTestConfig;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
@ExtendWith(DropwizardExtensionsSupport.class)
class StationLinksResourceNeighboursTest {
    private static final AppConfiguration configuration = new NeighboursTestConfig();
    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, configuration);

    @Test
    void shouldGetStationNeighbours() {
        String endPoint = "links/neighbours";

        Response response = IntegrationClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, response.getStatus(), "status");

        List<StationLinkDTO> results = response.readEntity(new GenericType<>() {});
        assertEquals(710, results.size(), "count");

        //assertTrue(results.contains(createLink(Shudehill, TestEnv.)));

    }

    private StationLinkDTO createLink(TramStations begin, TramStations end) {
        return new StationLinkDTO(new StationRefWithPosition(TramStations.of(begin)),
                new StationRefWithPosition(TramStations.of(end)), Collections.singleton(Tram));
    }
}
