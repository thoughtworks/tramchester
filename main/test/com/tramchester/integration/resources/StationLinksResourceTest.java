package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.presentation.DTO.BoxDTO;
import com.tramchester.domain.presentation.DTO.StationLinkDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(DropwizardExtensionsSupport.class)
class StationLinksResourceTest {
    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new IntegrationTramTestConfig());
    private static GuiceContainerDependencies dependencies;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        App app = appExtension.getTestSupport().getApplication();
        dependencies = app.getDependencies();
    }

    @Test
    void shouldGetStationLinks() {
        String endPoint = "links/all";

        Response response = APIClient.getApiResponse(appExtension, endPoint);
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

    @Test
    void shouldGetQuadrants() {
        StationLocations stationLocations = dependencies.get(StationLocations.class);
        Set<BoundingBox> quadrants = stationLocations.getQuadrants();

        String endPoint = "links/quadrants";
        Response response = APIClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, response.getStatus(), "status");

        List<BoxDTO> results = response.readEntity(new GenericType<>() {});
        assertEquals(quadrants.size(), results.size());

        Set<BoundingBox> received = results.stream().
                map(dto -> new BoundingBox(CoordinateTransforms.getGridPosition(dto.getBottomLeft()),
                        CoordinateTransforms.getGridPosition(dto.getTopRight()))).collect(Collectors.toSet());

        assertTrue(quadrants.containsAll(received));

    }

    private StationLinkDTO createLink(TramStations begin, TramStations end) {
        return new StationLinkDTO(new StationRefWithPosition(TramStations.of(begin)),
                new StationRefWithPosition(TramStations.of(end)), Collections.singleton(Tram));
    }
}
