package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.presentation.DTO.TramsPositionsDTO;
import com.tramchester.domain.presentation.TramPositionDTO;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TramPositionsResourceTest {
    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    @Test
    public void shouldGetSomePositionsFilteredByDefault() {
        String endPoint = "positions";
        Response responce = IntegrationClient.getResponse(testRule, endPoint, Optional.empty(), 200);

        TramsPositionsDTO filtered = responce.readEntity(TramsPositionsDTO.class);

        assertFalse(filtered.getBuses());

        // should have some positions
        List<TramPositionDTO> positions = filtered.getPositionsList();
        assertFalse(positions.isEmpty());

        // ALL of those positions should have trams
        long positionsWithTrams = positions.stream().filter(position -> !position.getTrams().isEmpty()).count();
        // MUST be same as total number of positions for filtered
        assertEquals(positions.size(), positionsWithTrams);

        Set<String> uniquePairs = positions.stream().
                map(position -> position.getFirst().getId() + position.getSecond().getId()).collect(Collectors.toSet());
        assertEquals(positions.size(), uniquePairs.size());

        long hasCost = positions.stream().filter(position -> position.getCost()>0).count();
        assertEquals(positions.size(), hasCost);

        long departingTrams = positions.stream().map(position -> position.getTrams()).
                flatMap(dueTrams -> dueTrams.stream()).filter(dueTram -> "Departing".equals(dueTram.getStatus())).count();
        assertEquals(0, departingTrams);
    }

    @Test
    public void shouldGetSomePositionsUnfiltered() {
        String endPoint = "positions?unfiltered=true";
        Response responce = IntegrationClient.getResponse(testRule, endPoint, Optional.empty(), 200);
        TramsPositionsDTO unfiltered = responce.readEntity(TramsPositionsDTO.class);

        // should have some positions
        List<TramPositionDTO> positions = unfiltered.getPositionsList();
        assertFalse(positions.isEmpty());
        long positionsWithTrams = positions.stream().filter(position -> !position.getTrams().isEmpty()).count();
        assertTrue(positionsWithTrams>0);
        // for unfiltered should have more positions than ones with trams
        assertTrue(positions.size() > positionsWithTrams);

        long departingTrams = positions.stream().map(position -> position.getTrams()).
                flatMap(dueTrams -> dueTrams.stream()).filter(dueTram -> "Departing".equals(dueTram.getStatus())).count();
        assertEquals(0, departingTrams);
    }
}
