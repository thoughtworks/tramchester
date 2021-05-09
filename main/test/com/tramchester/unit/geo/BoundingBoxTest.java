package com.tramchester.unit.geo;

import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundingBoxTest {

    @Test
    void shouldHaveContained() {
        BoundingBox box = TestEnv.getTFGMBusBounds();

        List<LatLong> withinBox = Arrays.asList(TestEnv.nearPiccGardens, TestEnv.nearShudehill,
                TramStations.ManAirport.getLatLong(), TestEnv.nearAltrincham, TestEnv.nearStockportBus);

        withinBox.forEach(latLong -> {
            @NotNull GridPosition grid = CoordinateTransforms.getGridPosition(latLong);
            assertTrue(box.contained(grid), grid.toString());
        });

        LatLong outsideBox = TestEnv.nearGreenwich;
        assertFalse(box.contained(CoordinateTransforms.getGridPosition(outsideBox)));
    }

    @Test
    void shouldHaveOverlapsWithin() {
        BoundingBox boxA = new BoundingBox(1,2, 10,12);
        BoundingBox boxB = new BoundingBox(4,5, 7,9);

        assertTrue(boxA.overlapsWith(boxB));
        assertTrue(boxB.overlapsWith(boxA));
    }

    @Test
    void shouldHaveOverlapsTopRightOrBottomLeft() {
        BoundingBox boxA = new BoundingBox(1,2, 10,12);
        BoundingBox boxB = new BoundingBox(4,5, 13,14);

        assertTrue(boxA.overlapsWith(boxB));
        assertTrue(boxB.overlapsWith(boxA));
    }

    @Test
    void shouldHaveOverlapsBottomRightOrTopLeft() {
        BoundingBox boxA = new BoundingBox(5,6, 10,12);
        BoundingBox boxB = new BoundingBox(7,4, 13,8);

        assertTrue(boxA.overlapsWith(boxB));
        assertTrue(boxB.overlapsWith(boxA));
    }

    @Test
    void shouldHaveOverlapVertStrip() {
        BoundingBox boxA = new BoundingBox(5,6, 10,12);
        BoundingBox boxB = new BoundingBox(7,4, 8,14);

        assertTrue(boxA.overlapsWith(boxB));
        assertTrue(boxB.overlapsWith(boxA));
    }

    @Test
    void shouldHaveOverlapHorzStrip() {
        BoundingBox boxA = new BoundingBox(5,6, 10,12);
        BoundingBox boxB = new BoundingBox(4,7, 12,10);

        assertTrue(boxA.overlapsWith(boxB));
        assertTrue(boxB.overlapsWith(boxA));
    }

    @Test
    void shouldHaveOverlapPartial() {
        BoundingBox boxA = new BoundingBox(5,6, 10,12);
        BoundingBox boxB = new BoundingBox(7,8, 8,14);

        assertTrue(boxA.overlapsWith(boxB));
        assertTrue(boxB.overlapsWith(boxA));
    }


    @Test
    void shouldHaveNotOverlaps() {
        BoundingBox boxA = new BoundingBox(1,2, 10,12);
        BoundingBox boxB = new BoundingBox(20,25, 28,34);

        assertFalse(boxA.overlapsWith(boxB));
        assertFalse(boxB.overlapsWith(boxA));
    }

}
