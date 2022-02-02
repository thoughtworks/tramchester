package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.AreaBoundaryDTO;
import com.tramchester.domain.presentation.DTO.BoxDTO;
import com.tramchester.domain.presentation.DTO.StationLinkDTO;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.naptan.ResourceTramTestConfigWithNaptan;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.StationGeographyResource;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class StationGeographyResourceTest {
    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new ResourceTramTestConfigWithNaptan<>(StationGeographyResource.class));

    private static GuiceContainerDependencies dependencies;
    private DTOFactory DTOFactory;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        App app = appExtension.getTestSupport().getApplication();
        dependencies = app.getDependencies();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        DTOFactory = dependencies.get(DTOFactory.class);
    }

    @Test
    void shouldGetStationLinks() {
        String endPoint = "geo/all";

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

        String endPoint = "geo/quadrants";
        Response response = APIClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, response.getStatus(), "status");

        List<BoxDTO> results = response.readEntity(new GenericType<>() {});
        assertEquals(quadrants.size(), results.size());

        Set<BoundingBox> received = results.stream().
                map(dto -> new BoundingBox(CoordinateTransforms.getGridPosition(dto.getBottomLeft()),
                        CoordinateTransforms.getGridPosition(dto.getTopRight()))).collect(Collectors.toSet());

        assertTrue(quadrants.containsAll(received));
    }

    @Test
    void shouldGetBounds() {
        TramchesterConfig config = dependencies.get(TramchesterConfig.class);

        String endPoint = "geo/bounds";
        Response response = APIClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, response.getStatus(), "status");

        BoxDTO result = response.readEntity(BoxDTO.class);

        BoundingBox expected = config.getBounds();

        assertEquals(expected.getTopRight(), CoordinateTransforms.getGridPosition(result.getTopRight()));
        assertEquals(expected.getBottomLeft(), CoordinateTransforms.getGridPosition(result.getBottomLeft()));
    }

    @Test
    void shouldGetAreas() {
        StationRepository stationRepository = dependencies.get(StationRepository.class);
        Station bury = Bury.from(stationRepository);

        String endPoint = "geo/areas";

        Response response = APIClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, response.getStatus(), "status");

        List<AreaBoundaryDTO> areas = response.readEntity(new GenericType<>() {});

        assertFalse(areas.isEmpty());

        boolean found = areas.stream().anyMatch(area -> area.getAreaId().equals(bury.getAreaId().forDTO()));
        assertTrue(found);
    }

    private StationLinkDTO createLink(TramStations begin, TramStations end) {
        return new StationLinkDTO(DTOFactory.createLocationRefWithPosition(begin.fake()),
                DTOFactory.createLocationRefWithPosition(end.fake()),
                Collections.singleton(Tram));
    }
}
