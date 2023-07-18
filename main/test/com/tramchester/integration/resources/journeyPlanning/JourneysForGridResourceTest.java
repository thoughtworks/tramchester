package com.tramchester.integration.resources.journeyPlanning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.BoxWithCostDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.JourneysForGridResource;
import com.tramchester.testSupport.ParseStream;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocations;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
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
    private StationLocations stationLocations;
    private StationRepository stationRepository;
    private ClosedStationsRepository closedStationsRepository;
    private TramDate when;

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
        parseStream = new ParseStream<>(mapper);

        time = TramTime.of(9,15).toPattern();
        date = when.format(dateFormatDashes);
        maxChanges = 3;
        gridSize = 2000;
        maxDuration = 40;

        App app =  appExtension.getApplication();
        GuiceContainerDependencies dependencies = app.getDependencies();
        stationLocations = dependencies.get(StationLocations.class);
        closedStationsRepository = dependencies.get(ClosedStationsRepository.class);
        stationRepository = dependencies.get(StationRepository.class);
    }

    @Test
    void shouldHaveJourneysForWholeGrid() throws IOException {
        LatLong destPos = KnownLocations.nearStPetersSquare.latLong(); //TestEnv.stPetersSquareLocation();
        Station destination = TramStations.StPetersSquare.from(stationRepository);

        String actualId = new IdForDTO(destination.getId()).getActualId();
        String queryString = String.format("grid?gridSize=%s&destination=%s&departureTime=%s&departureDate=%s&maxChanges=%s&maxDuration=%s",
                gridSize, actualId, time, date, maxChanges, maxDuration);

        final int outOfRangeForDuration = 3;

        Response response = APIClient.getApiResponse(appExtension, queryString);
        assertEquals(200, response.getStatus());

        InputStream inputStream = response.readEntity(InputStream.class);
        List<BoxWithCostDTO> results = parseStream.receive(response, inputStream, BoxWithCostDTO.class);
        assertFalse(results.isEmpty());

        List<BoxWithCostDTO> containsDest = results.stream().filter(result -> result.getMinutes() == 0).collect(Collectors.toList());
        assertEquals(1, containsDest.size());
        BoxWithCostDTO boxWithDest = containsDest.get(0);
        assertTrue(boxWithDest.getBottomLeft().getLat() <= destPos.getLat());
        assertTrue(boxWithDest.getBottomLeft().getLon() <= destPos.getLon());
        assertTrue(boxWithDest.getTopRight().getLat() >= destPos.getLat());
        assertTrue(boxWithDest.getTopRight().getLon() >= destPos.getLon());

        List<BoxWithCostDTO> notDest = results.stream().filter(result -> result.getMinutes() > 0).collect(Collectors.toList());
        notDest.forEach(boundingBoxWithCost -> assertTrue(boundingBoxWithCost.getMinutes()<=maxDuration));

        List<BoxWithCostDTO> noResult = results.stream().filter(result -> result.getMinutes() < 0).collect(Collectors.toList());
        assertEquals(outOfRangeForDuration, noResult.size());

        Set<BoundingBoxWithStations> expectedBoxes = getExpectedBoxesInSearchGrid(destination);

        assertEquals(expectedBoxes.size() - outOfRangeForDuration, notDest.size(), "Expected " + expectedBoxes + " but got " + notDest);
    }

    private Set<BoundingBoxWithStations> getExpectedBoxesInSearchGrid(Station destination) {
        return stationLocations.getStationsInGrids(gridSize).
                filter(boxWithStations -> !boxWithStations.getStations().contains(destination)).
                filter(boxWithStations -> anyOpen(boxWithStations.getStations())).collect(Collectors.toSet());
    }

    private boolean anyOpen(Set<Station> stations) {
        return stations.stream().anyMatch(station -> !closedStationsRepository.isClosed(station, when));
    }


}
