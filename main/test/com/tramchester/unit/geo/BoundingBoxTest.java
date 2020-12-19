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

}
