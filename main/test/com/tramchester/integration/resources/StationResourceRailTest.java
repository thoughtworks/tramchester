package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.RouteRefDTO;
import com.tramchester.domain.presentation.DTO.StationRefDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.integration.testSupport.rail.ResourceRailTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.StationResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TrainTest
@ExtendWith(DropwizardExtensionsSupport.class)
class StationResourceRailTest {

    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new ResourceRailTestConfig<>(StationResource.class));

    private StationRepository stationRepo;

    @BeforeEach
    void beforeEachTestRuns() {
        App app =  appExtension.getApplication();
        stationRepo = app.getDependencies().get(StationRepository.class);
    }


    @Test
    void shouldGetSingleStationWithPlatforms() {
        String stationId = RailStationIds.ManchesterPiccadilly.getId().forDTO();
        String endPoint = "stations/" + stationId;
        Response response = APIClient.getApiResponse(appExtension, endPoint);
        
        assertEquals(200,response.getStatus());
        LocationDTO result = response.readEntity(LocationDTO.class);

        assertEquals(stationId, result.getId());
        assertEquals("Manchester Piccadilly Rail Station", result.getName());

        List<PlatformDTO> platforms = result.getPlatforms();
        assertEquals(16, platforms.size(), platforms.toString());

        List<String> platformIds = platforms.stream().map(PlatformDTO::getId).collect(Collectors.toList());
        assertTrue(platformIds.contains(stationId+":1"), platformIds.toString());
        assertTrue(platformIds.contains(stationId+":13B"), platformIds.toString());

        List<RouteRefDTO> routeRefDTOS = result.getRoutes();

        assertFalse(routeRefDTOS.isEmpty());

        Station station = stationRepo.getStationById(RailStationIds.ManchesterPiccadilly.getId());
        int stationRoutesNumber = station.getPickupRoutes().size() + station.getDropoffRoutes().size();

        assertEquals(routeRefDTOS.size(), stationRoutesNumber);

    }

    @Test
    void shouldGetTrainStations() {
        Response result = APIClient.getApiResponse(appExtension, "stations/mode/Train");

        assertEquals(200, result.getStatus());

        List<StationRefDTO> results = result.readEntity(new GenericType<>() {});

        Set<String> expectedIds = stationRepo.getStations().stream().
                filter(Location::isActive).
                map(station -> station.getId().forDTO()).collect(Collectors.toSet());

        assertEquals(expectedIds.size(), results.size());

        List<String> resultIds = results.stream().map(StationRefDTO::getId).collect(Collectors.toList());
        assertTrue(expectedIds.containsAll(resultIds));

        ArrayList<StationRefDTO> sortedResults = new ArrayList<>(results);
        sortedResults.sort(Comparator.comparing(item -> item.getName().toLowerCase()));

        for (int i = 0; i < sortedResults.size(); i++) {
            assertEquals(results.get(i), sortedResults.get(i), "not sorted");
        }
    }

    @Test
    void shouldGetNearestTrainStations() {

        LatLong nearPiccGardens = TestEnv.nearPiccGardens;
        Response result = APIClient.getApiResponse(appExtension, String.format("stations/near?lat=%s&lon=%s",
                nearPiccGardens.getLat(), nearPiccGardens.getLon()));
        assertEquals(200, result.getStatus());

        List<StationRefDTO> stationList = result.readEntity(new GenericType<>() {});

        assertEquals(5, stationList.size(), stationList.toString());
        Set<String> ids = stationList.stream().map(StationRefDTO::getId).collect(Collectors.toSet());
        assertTrue(ids.contains(RailStationIds.ManchesterPiccadilly.getId().forDTO()));
        assertTrue(ids.contains(RailStationIds.ManchesterVictoria.getId().forDTO()));
        assertTrue(ids.contains(RailStationIds.ManchesterDeansgate.getId().forDTO()));
        assertTrue(ids.contains(RailStationIds.SalfordCentral.getId().forDTO()));
        assertTrue(ids.contains(RailStationIds.ManchesterOxfordRoad.getId().forDTO()));
    }


}
