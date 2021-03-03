package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.presentation.DTO.StationLinkDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.IntegrationClient;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(DropwizardExtensionsSupport.class)
class StationLinksResourceTest {
    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new IntegrationTramTestConfig());

    @Test
    void shouldGetStationLinks() {
        String endPoint = "links/all";

        Response response = IntegrationClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, response.getStatus(), "status");

        List<StationLinkDTO> results = response.readEntity(new GenericType<>() {});
        assertEquals(202, results.size(), "count");

        assertTrue(results.contains(createLink(StPetersSquare, PiccadillyGardens)));
        assertTrue(results.contains(createLink(StPetersSquare, MarketStreet)));
        assertTrue(results.contains(createLink(StPetersSquare, Deansgate)));

        assertTrue(results.contains(createLink(PiccadillyGardens, StPetersSquare)));
        assertTrue(results.contains(createLink(MarketStreet, StPetersSquare)));
        assertTrue(results.contains(createLink(Deansgate, StPetersSquare)));
    }

    private StationLinkDTO createLink(TramStations begin, TramStations end) {
        return new StationLinkDTO(new StationRefWithPosition(TramStations.of(begin)),
                new StationRefWithPosition(TramStations.of(end)), Collections.singleton(Tram));
    }
}
