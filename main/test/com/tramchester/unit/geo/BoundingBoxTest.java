package com.tramchester.unit.geo;

import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.HasGridPosition;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.operation.TransformException;

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
            @NotNull HasGridPosition grid = getGridPosition(latLong);
            assertTrue(box.contained(grid), grid.toString());
        });

        LatLong outsideBox = TestEnv.nearGreenwich;
        assertFalse(box.contained(getGridPosition(outsideBox)));
    }

    @NotNull
    private HasGridPosition getGridPosition(LatLong place)  {
        try {
            return CoordinateTransforms.getGridPosition(place);
        } catch (TransformException exception) {
            return new HasGridPosition() {
                @Override
                public long getEastings() {
                    return -1;
                }

                @Override
                public long getNorthings() {
                    return -1;
                }
            };
        }
    }
}
