package com.tramchester.unit.domain;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.TransportRelationshipTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransportRelationshipTypesTest {

    private List<TransportRelationshipTypes> all;

    @BeforeEach
    void beforeEachTestRuns() {
        all = Arrays.asList(TransportRelationshipTypes.values());
    }

    @Test
    void shouldRememberToUpdateTestsBelowIfNewTypesAdd() {
        assertEquals(20, all.size(),"New types, update the tests");
    }

    @Test
    void shouldHaveCorrectTypesWithTripId()  {
        List<TransportRelationshipTypes> expectedTripId = Arrays.asList(TransportRelationshipTypes.TO_MINUTE,
                TransportRelationshipTypes.BUS_GOES_TO,
                TransportRelationshipTypes.TRAM_GOES_TO,
                TransportRelationshipTypes.TRAIN_GOES_TO,
                TransportRelationshipTypes.FERRY_GOES_TO,
                TransportRelationshipTypes.SUBWAY_GOES_TO);

        expectedTripId.forEach(expected -> assertTrue(TransportRelationshipTypes.hasTripId(expected), expected.name()));

        List<TransportRelationshipTypes> expectedNoTripId = new ArrayList<>(all);
        expectedNoTripId.removeAll(expectedTripId);
        expectedNoTripId.forEach(expected -> assertFalse(TransportRelationshipTypes.hasTripId(expected), expected.name()));
    }

    @Test
    void shouldHaveCorrectTypesWithCost()  {
        List<TransportRelationshipTypes> expectedNoCost = Arrays.asList(
                TransportRelationshipTypes.TO_MINUTE,
                TransportRelationshipTypes.TO_HOUR,
                TransportRelationshipTypes.TO_SERVICE);

        expectedNoCost.forEach(expected -> assertFalse(TransportRelationshipTypes.hasCost(expected), expected.name()));

        List<TransportRelationshipTypes> expectedToHaveCost = new ArrayList<>(all);
        expectedToHaveCost.removeAll(expectedNoCost);

        expectedToHaveCost.forEach(expected -> assertTrue(TransportRelationshipTypes.hasCost(expected), expected.name()));

    }

    @Test
    void shouldHaveCorrectRelationshipForTransportMode() {
        assertEquals(TransportRelationshipTypes.BUS_GOES_TO, TransportRelationshipTypes.forMode(TransportMode.Bus));
        assertEquals(TransportRelationshipTypes.BUS_GOES_TO, TransportRelationshipTypes.forMode(TransportMode.RailReplacementBus));

        assertEquals(TransportRelationshipTypes.TRAM_GOES_TO, TransportRelationshipTypes.forMode(TransportMode.Tram));
        assertEquals(TransportRelationshipTypes.TRAIN_GOES_TO, TransportRelationshipTypes.forMode(TransportMode.Train));
        assertEquals(TransportRelationshipTypes.FERRY_GOES_TO, TransportRelationshipTypes.forMode(TransportMode.Ferry));
        assertEquals(TransportRelationshipTypes.SUBWAY_GOES_TO, TransportRelationshipTypes.forMode(TransportMode.Subway));
    }

}
