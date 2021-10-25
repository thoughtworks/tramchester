package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.livedata.domain.DTO.TramsPositionsDTO;
import com.tramchester.livedata.domain.DTO.TramPositionDTO;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.resources.TramPositionsResource;
import com.tramchester.testSupport.testTags.LiveDataTestCategory;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class TramPositionsResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new ResourceTramTestConfig<>(TramPositionsResource.class,true));

    @LiveDataTestCategory
    @Test
    void shouldGetSomePositionsFilteredByDefault() {
        String endPoint = "positions";
        Response responce = APIClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, responce.getStatus());

        TramsPositionsDTO filtered = responce.readEntity(TramsPositionsDTO.class);

        //assertFalse(filtered.getBuses());

        // should have some positions
        List<TramPositionDTO> positions = filtered.getPositionsList();
        assertFalse(positions.isEmpty(), "no tram positions available");

        // ALL of those positions should have trams
        long positionsWithTrams = positions.stream().filter(position -> !position.getTrams().isEmpty()).count();
        // MUST be same as total number of positions for filtered
        assertEquals(positions.size(), positionsWithTrams);

        Set<String> uniquePairs = positions.stream().
                map(position -> position.getFirst().getId() + position.getSecond().getId()).collect(Collectors.toSet());
        assertEquals(positions.size(), uniquePairs.size());

        long hasCost = positions.stream().filter(position -> position.getCost()>0).count();
        assertEquals(positions.size(), hasCost);

        long departingTrams = positions.stream().map(TramPositionDTO::getTrams).
                flatMap(Collection::stream).filter(dueTram -> "Departing".equals(dueTram.getStatus())).count();
        assertEquals(0, departingTrams);
    }

    @LiveDataTestCategory
    @Test
    void shouldGetSomePositionsUnfiltered() {
        String endPoint = "positions?unfiltered=true";
        Response responce = APIClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, responce.getStatus());

        TramsPositionsDTO unfiltered = responce.readEntity(TramsPositionsDTO.class);

        // should have some positions
        List<TramPositionDTO> positions = unfiltered.getPositionsList();
        assertFalse(positions.isEmpty());
        long positionsWithTrams = positions.stream().filter(position -> !position.getTrams().isEmpty()).count();
        assertTrue(positionsWithTrams>0, "no tram positions available");
        // for unfiltered should have more positions than ones with trams
        assertTrue(positions.size() > positionsWithTrams);

        long departingTrams = positions.stream().map(TramPositionDTO::getTrams).
                flatMap(Collection::stream).filter(dueTram -> "Departing".equals(dueTram.getStatus())).count();
        assertEquals(0, departingTrams);
    }
}
